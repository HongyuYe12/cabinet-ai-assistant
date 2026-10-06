package com.cabinetai.design.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record DesignRequest(
        @Min(5000) @Max(500000) int budget,
        String style,
        List<String> needs) {
}
