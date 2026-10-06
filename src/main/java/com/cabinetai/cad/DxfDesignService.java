package com.cabinetai.cad;
import java.nio.charset.StandardCharsets;import java.util.*;import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;import com.fasterxml.jackson.databind.ObjectMapper;
@Service public class DxfDesignService {
 private final ObjectMapper mapper=new ObjectMapper();
 record L(double x1,double y1,double x2,double y2){}
 public byte[] design(byte[] source,double depth){return design(source,depth,null);}
 public byte[] design(byte[] source,double depth,String plan){String d=new String(source,StandardCharsets.UTF_8).replace("\r\n","\n").replace('\r','\n');if(!d.contains("ENTITIES"))throw new IllegalArgumentException("DXF缺少ENTITIES段");List<L> ls=parse(d);StringBuilder o=new StringBuilder(d.trim());int end=o.lastIndexOf("0\nENDSEC");if(end<0)throw new IllegalArgumentException("DXF格式不完整：未找到ENDSEC");StringBuilder a=new StringBuilder();int id=1;for(L l:ls){double len=Math.hypot(l.x2-l.x1,l.y2-l.y1);if(len<900)continue;double ux=(l.x2-l.x1)/len,uy=(l.y2-l.y1)/len,nx=-uy*depth,ny=ux*depth,x=l.x1,y=l.y1;while(len>=300){double w=Math.min(600,len),x2=x+ux*w,y2=y+uy*w;a.append(rect(id++,x,y,x2,y2,nx,ny));x=x2;y=y2;len-=w;}}try{if(plan!=null){for(JsonNode p:mapper.readTree(plan).path("placements"))a.append(rectNamed(p.path("name").asText("APPLIANCE"),p.path("xMm").asDouble(),p.path("yMm").asDouble(),p.path("widthMm").asDouble(600),p.path("depthMm").asDouble(600)));}}catch(Exception ignored){}o.insert(end,a);return o.toString().getBytes(StandardCharsets.UTF_8);}
 private List<L> parse(String d){String[]t=d.replace("\r","").split("\n");List<L>r=new ArrayList<>();for(int i=0;i<t.length;i++)if(t[i].trim().equals("LINE"))r.add(new L(v(t,i,"10"),v(t,i,"20"),v(t,i,"11"),v(t,i,"21")));return r;}
 private double v(String[]t,int s,String c){for(int i=s;i<Math.min(t.length,s+30)-1;i++)if(t[i].trim().equals(c))return Double.parseDouble(t[i+1].trim());return 0;}
 private String rect(int id,double x1,double y1,double x2,double y2,double nx,double ny){double[][]p={{x1,y1},{x2,y2},{x2+nx,y2+ny},{x1+nx,y1+ny}};StringBuilder b=new StringBuilder();for(int i=0;i<4;i++)b.append(line(p[i][0],p[i][1],p[(i+1)%4][0],p[(i+1)%4][1]));return b.append("0\nTEXT\n8\nNEW_CABINET\n10\n").append(x1).append("\n20\n").append(y1).append("\n40\n80\n1\nCABINET-").append(id).append("\n").toString();}
 private String line(double x1,double y1,double x2,double y2){return "0\nLINE\n8\nNEW_CABINET\n10\n"+x1+"\n20\n"+y1+"\n11\n"+x2+"\n21\n"+y2+"\n";}
 private String rectNamed(String n,double x,double y,double w,double d){return line(x,y,x+w,y)+line(x+w,y,x+w,y+d)+line(x+w,y+d,x,y+d)+line(x,y+d,x,y)+"0\nTEXT\n8\nNEW_APPLIANCE\n10\n"+x+"\n20\n"+y+"\n40\n80\n1\n"+n+"\n";}
}
