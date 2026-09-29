package com.cabinetai.design.model;

import java.util.List;

public record DesignResponse(
        String layoutType,
        String style,
        List<CabinetItem> cabinets,
        int estimatedPrice,
        int remainingWallSpaceMm,
        List<String> warnings) {
}
