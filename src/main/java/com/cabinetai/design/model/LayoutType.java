package com.cabinetai.design.model;

public enum LayoutType {
    STRAIGHT("一字型"),
    L_SHAPED("L型"),
    U_SHAPED("U型");

    private final String displayName;

    LayoutType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
