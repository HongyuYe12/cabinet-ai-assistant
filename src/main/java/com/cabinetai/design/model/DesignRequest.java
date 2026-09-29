package com.cabinetai.design.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DesignRequest(
        @NotNull LayoutType layoutType,
        @Min(1800) @Max(10000) int wallAmm,
        @Min(0) @Max(10000) int wallBmm,
        @Min(0) @Max(10000) int wallCmm,
        @Min(5000) @Max(500000) int budget,
        String style,
        List<String> needs) {
}
