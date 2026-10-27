package com.cabinetai.vision;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class DxfStructureService {
    private static final List<String> SUPPORTED = List.of(
            "LINE", "LWPOLYLINE", "POLYLINE", "ARC", "CIRCLE", "INSERT", "TEXT", "MTEXT", "HATCH");

    public Map<String, Object> parse(String raw) {
        String[] rows = raw.replace("\r", "").split("\n");
        if (rows.length < 4 || !raw.toUpperCase().contains("SECTION")) {
            throw new IllegalArgumentException("DXF 内容无效或不是 ASCII DXF");
        }
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < rows.length; i += 2) {
            try { pairs.add(new Pair(Integer.parseInt(rows[i].trim()), rows[i + 1].trim())); }
            catch (NumberFormatException ignored) { }
        }

        Map<String, Integer> entityCounts = new TreeMap<>();
        Map<String, Integer> layerCounts = new TreeMap<>();
        Bounds bounds = new Bounds();
        boolean inEntities = false;
        String currentType = null, currentLayer = "0";
        List<Double> xs = new ArrayList<>(), ys = new ArrayList<>();

        for (Pair pair : pairs) {
            if (pair.code == 2 && "ENTITIES".equalsIgnoreCase(pair.value)) { inEntities = true; continue; }
            if (!inEntities) continue;
            if (pair.code == 0 && "ENDSEC".equalsIgnoreCase(pair.value)) {
                finish(currentType, currentLayer, xs, ys, entityCounts, layerCounts, bounds); break;
            }
            if (pair.code == 0) {
                finish(currentType, currentLayer, xs, ys, entityCounts, layerCounts, bounds);
                currentType = pair.value.toUpperCase(); currentLayer = "0"; xs = new ArrayList<>(); ys = new ArrayList<>();
            } else if (currentType != null) {
                if (pair.code == 8) currentLayer = pair.value;
                if (pair.code >= 10 && pair.code <= 18) addNumber(xs, pair.value);
                if (pair.code >= 20 && pair.code <= 28) addNumber(ys, pair.value);
            }
        }

        int total = entityCounts.values().stream().mapToInt(Integer::intValue).sum();
        int recognized = SUPPORTED.stream().mapToInt(type -> entityCounts.getOrDefault(type, 0)).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entityCount", total);
        result.put("recognizedEntityCount", recognized);
        result.put("layers", layerCounts);
        result.put("entities", entityCounts);
        result.put("bounds", bounds.asMap());
        result.put("unitsNote", "坐标单位沿用 DXF 原始单位；橱柜项目通常应确认是否为毫米。");
        result.put("warnings", total == 0 ? List.of("ENTITIES 段没有解析到图元") : List.of());
        return result;
    }

    private void finish(String type, String layer, List<Double> xs, List<Double> ys,
            Map<String, Integer> entities, Map<String, Integer> layers, Bounds bounds) {
        if (type == null || "SEQEND".equals(type)) return;
        entities.merge(type, 1, Integer::sum); layers.merge(layer, 1, Integer::sum);
        xs.forEach(bounds::x); ys.forEach(bounds::y);
    }

    private void addNumber(List<Double> values, String value) {
        try { values.add(Double.parseDouble(value)); } catch (NumberFormatException ignored) { }
    }

    private record Pair(int code, String value) { }

    private static class Bounds {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        void x(double value) { minX = Math.min(minX, value); maxX = Math.max(maxX, value); }
        void y(double value) { minY = Math.min(minY, value); maxY = Math.max(maxY, value); }
        Map<String, Object> asMap() {
            if (!Double.isFinite(minX) || !Double.isFinite(minY)) return Map.of();
            return Map.of("minX", round(minX), "minY", round(minY), "maxX", round(maxX),
                    "maxY", round(maxY), "width", round(maxX - minX), "height", round(maxY - minY));
        }
        double round(double value) { return Math.round(value * 100.0) / 100.0; }
    }
}
