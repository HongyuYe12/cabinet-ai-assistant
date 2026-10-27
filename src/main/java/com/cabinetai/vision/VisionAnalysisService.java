package com.cabinetai.vision;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class VisionAnalysisService {
    private static final int MAX_SIDE = 1000;

    public Map<String, Object> analyzeFloorPlan(byte[] bytes) throws Exception {
        BufferedImage image = scaleDown(read(bytes), MAX_SIDE);
        int width = image.getWidth(), height = image.getHeight();
        boolean[][] dark = new boolean[height][width], walls = new boolean[height][width], thin = new boolean[height][width];
        int wallPixels = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) dark[y][x] = luminance(image.getRGB(x, y)) < 175;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (!dark[y][x]) continue;
            walls[y][x] = countNearby(dark, x, y, 2) >= 8; thin[y][x] = !walls[y][x];
            if (walls[y][x]) wallPixels++;
        }

        List<Component> rooms = components(invert(dilate(walls, 2)), Math.max(250, width * height / 350)).stream()
                .filter(c -> !c.touchesBorder).limit(20).toList();
        List<Component> openings = components(thin, 8).stream()
                .filter(c -> !c.touchesBorder && c.area <= Math.max(600, width * height / 120)).limit(80).toList();

        BufferedImage annotated = copy(image);
        Graphics2D g = annotated.createGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(0.22f));
        Color[] roomColors = {new Color(37, 99, 235), new Color(22, 163, 74), new Color(147, 51, 234), new Color(245, 158, 11)};
        int index = 0;
        for (Component room : rooms) {
            g.setColor(roomColors[index++ % roomColors.length]);
            g.fillRect(room.minX, room.minY, room.maxX - room.minX + 1, room.maxY - room.minY + 1);
        }
        g.dispose();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (walls[y][x]) annotated.setRGB(x, y, blend(annotated.getRGB(x, y), 0xFFE11D48, 0.62));
            else if (thin[y][x]) annotated.setRGB(x, y, blend(annotated.getRGB(x, y), 0xFF06B6D4, 0.70));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imageWidth", width); result.put("imageHeight", height);
        result.put("wallCoveragePercent", round(wallPixels * 100.0 / (width * height)));
        result.put("roomRegionCount", rooms.size()); result.put("doorWindowCandidateCount", openings.size());
        result.put("roomRegions", rooms.stream().map(Component::asMap).toList());
        result.put("annotatedImage", pngDataUrl(annotated));
        result.put("legend", Map.of("red", "墙体候选", "cyan", "门窗/细线候选", "colorBlocks", "封闭房间区域"));
        result.put("method", "本地启发式分割 MVP（灰度阈值、形态学加粗、连通区域分析）");
        result.put("warnings", List.of("复杂彩色图纸、低清扫描和非标准符号需要人工复核或训练分割模型。"));
        return result;
    }

    public Map<String, Object> compare(byte[] referenceBytes, byte[] actualBytes) throws Exception {
        BufferedImage reference = fit(read(referenceBytes), 720, 540), actual = fit(read(actualBytes), 720, 540);
        int bestDx = 0, bestDy = 0; double bestError = Double.POSITIVE_INFINITY;
        for (int dy = -24; dy <= 24; dy += 4) for (int dx = -24; dx <= 24; dx += 4) {
            double error = sampledError(reference, actual, dx, dy, 5);
            if (error < bestError) { bestError = error; bestDx = dx; bestDy = dy; }
        }
        for (int dy = bestDy - 3; dy <= bestDy + 3; dy++) for (int dx = bestDx - 3; dx <= bestDx + 3; dx++) {
            double error = sampledError(reference, actual, dx, dy, 3);
            if (error < bestError) { bestError = error; bestDx = dx; bestDy = dy; }
        }
        BufferedImage heatmap = copy(actual); long totalDiff = 0, changed = 0, compared = 0;
        for (int y = 0; y < actual.getHeight(); y++) for (int x = 0; x < actual.getWidth(); x++) {
            int rx = x - bestDx, ry = y - bestDy;
            if (rx < 0 || ry < 0 || rx >= reference.getWidth() || ry >= reference.getHeight()) continue;
            int diff = Math.abs(luminance(reference.getRGB(rx, ry)) - luminance(actual.getRGB(x, y)));
            totalDiff += diff; compared++; if (diff > 38) changed++;
            if (diff > 18) heatmap.setRGB(x, y, blend(heatmap.getRGB(x, y), 0xFFEF4444, Math.min(0.84, 0.28 + diff / 330.0)));
        }
        double meanDiff = compared == 0 ? 255 : totalDiff / (double) compared;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("similarityPercent", round(Math.max(0, 100.0 * (1.0 - meanDiff / 255.0))));
        result.put("changedAreaPercent", round(compared == 0 ? 100 : changed * 100.0 / compared));
        result.put("alignment", Map.of("offsetX", bestDx, "offsetY", bestDy));
        result.put("differenceHeatmap", pngDataUrl(heatmap));
        result.put("method", "亮度归一化、平移对齐与像素差异热力图");
        result.put("warnings", List.of("拍摄角度差异较大时应先透视校正；当前分数用于初筛，不替代工程验收。"));
        return result;
    }

    private BufferedImage read(byte[] bytes) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IllegalArgumentException("无法读取图片，请使用 PNG 或 JPG"); return image;
    }
    private BufferedImage scaleDown(BufferedImage image, int maxSide) {
        double scale = Math.min(1.0, maxSide / (double) Math.max(image.getWidth(), image.getHeight()));
        return scale == 1.0 ? copy(image) : resize(image, Math.max(1, (int)(image.getWidth()*scale)), Math.max(1, (int)(image.getHeight()*scale)));
    }
    private BufferedImage fit(BufferedImage image, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); Graphics2D g = output.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
        double scale = Math.min(width/(double)image.getWidth(), height/(double)image.getHeight()); int w=(int)(image.getWidth()*scale), h=(int)(image.getHeight()*scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, (width-w)/2, (height-h)/2, w, h, null); g.dispose(); return output;
    }
    private BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); Graphics2D g = output.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null); g.dispose(); return output;
    }
    private BufferedImage copy(BufferedImage source) {
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics(); g.drawImage(source, 0, 0, null); g.dispose(); return output;
    }
    private int luminance(int rgb) { return (int)(0.2126*((rgb>>16)&255)+0.7152*((rgb>>8)&255)+0.0722*(rgb&255)); }
    private int countNearby(boolean[][] mask, int x, int y, int radius) {
        int count=0; for(int yy=Math.max(0,y-radius);yy<=Math.min(mask.length-1,y+radius);yy++)
            for(int xx=Math.max(0,x-radius);xx<=Math.min(mask[0].length-1,x+radius);xx++) if(mask[yy][xx]) count++; return count;
    }
    private boolean[][] dilate(boolean[][] input,int radius){int h=input.length,w=input[0].length;boolean[][] out=new boolean[h][w];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(input[y][x])for(int yy=Math.max(0,y-radius);yy<=Math.min(h-1,y+radius);yy++)
            for(int xx=Math.max(0,x-radius);xx<=Math.min(w-1,x+radius);xx++)out[yy][xx]=true;return out;}
    private boolean[][] invert(boolean[][] input){boolean[][] out=new boolean[input.length][input[0].length];
        for(int y=0;y<input.length;y++)for(int x=0;x<input[0].length;x++)out[y][x]=!input[y][x];return out;}
    private List<Component> components(boolean[][] mask,int minArea){int h=mask.length,w=mask[0].length;boolean[][] seen=new boolean[h][w];List<Component> result=new ArrayList<>();int[] dx={1,-1,0,0},dy={0,0,1,-1};
        for(int sy=0;sy<h;sy++)for(int sx=0;sx<w;sx++){if(!mask[sy][sx]||seen[sy][sx])continue;Component c=new Component(sx,sy);ArrayDeque<int[]> q=new ArrayDeque<>();q.add(new int[]{sx,sy});seen[sy][sx]=true;
            while(!q.isEmpty()){int[]p=q.removeFirst();int x=p[0],y=p[1];c.add(x,y,w,h);for(int i=0;i<4;i++){int nx=x+dx[i],ny=y+dy[i];if(nx>=0&&ny>=0&&nx<w&&ny<h&&mask[ny][nx]&&!seen[ny][nx]){seen[ny][nx]=true;q.addLast(new int[]{nx,ny});}}}
            if(c.area>=minArea)result.add(c);}return result;}
    private double sampledError(BufferedImage reference,BufferedImage actual,int dx,int dy,int step){long error=0,count=0;
        for(int y=0;y<actual.getHeight();y+=step)for(int x=0;x<actual.getWidth();x+=step){int rx=x-dx,ry=y-dy;if(rx>=0&&ry>=0&&rx<reference.getWidth()&&ry<reference.getHeight()){error+=Math.abs(luminance(reference.getRGB(rx,ry))-luminance(actual.getRGB(x,y)));count++;}}
        return count==0?Double.POSITIVE_INFINITY:error/(double)count;}
    private int blend(int base,int overlay,double alpha){int br=(base>>16)&255,bg=(base>>8)&255,bb=base&255,or=(overlay>>16)&255,og=(overlay>>8)&255,ob=overlay&255;
        int r=(int)(br*(1-alpha)+or*alpha),g=(int)(bg*(1-alpha)+og*alpha),b=(int)(bb*(1-alpha)+ob*alpha);return 0xFF000000|(r<<16)|(g<<8)|b;}
    private String pngDataUrl(BufferedImage image)throws Exception{ByteArrayOutputStream output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);return "data:image/png;base64,"+Base64.getEncoder().encodeToString(output.toByteArray());}
    private double round(double value){return Math.round(value*10.0)/10.0;}

    private static class Component{int area=0,minX,minY,maxX,maxY;boolean touchesBorder;Component(int x,int y){minX=maxX=x;minY=maxY=y;}
        void add(int x,int y,int width,int height){area++;minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);touchesBorder|=x==0||y==0||x==width-1||y==height-1;}
        Map<String,Object> asMap(){return Map.of("areaPixels",area,"x",minX,"y",minY,"width",maxX-minX+1,"height",maxY-minY+1);}}
}
