package com.cabinetai.design.service;

import com.cabinetai.design.model.CabinetItem;
import com.cabinetai.design.model.DesignRequest;
import com.cabinetai.design.model.DesignResponse;
import com.cabinetai.design.model.LayoutType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DesignService {
    private static final int MIN_WALKWAY_MM = 900;
    private final CabinetCatalog catalog;

    public DesignService(CabinetCatalog catalog) {
        this.catalog = catalog;
    }

    public DesignResponse generate(DesignRequest request) {
        List<CabinetItem> cabinets = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        addRun(cabinets, request.wallAmm(), "A墙");
        if (request.layoutType() != LayoutType.STRAIGHT) {
            addRun(cabinets, request.wallBmm(), "B墙");
        }
        if (request.layoutType() == LayoutType.U_SHAPED) {
            addRun(cabinets, request.wallCmm(), "C墙");
            warnings.add("U型厨房请现场确认中间通道不小于" + MIN_WALKWAY_MM + "mm。");
        }
        int total = cabinets.stream().mapToInt(CabinetItem::price).sum();
        int availableWall = request.wallAmm()
                + (request.layoutType() == LayoutType.STRAIGHT ? 0 : request.wallBmm())
                + (request.layoutType() == LayoutType.U_SHAPED ? request.wallCmm() : 0);
        int usedWall = cabinets.stream().mapToInt(CabinetItem::widthMm).sum();
        if (total > request.budget()) {
            warnings.add("当前组合超出预算 " + (total - request.budget()) + " 元，可替换材质或减少增配。");
        }
        warnings.add("方案为初步估算，尺寸、报价及水电燃气位置均需复尺确认。");
        return new DesignResponse(request.layoutType().getDisplayName(), request.style(), cabinets, total,
                Math.max(0, availableWall - usedWall), warnings);
    }

    private void addRun(List<CabinetItem> cabinets, int wallLength, String position) {
        int baseWidth = catalog.standardRun(position).stream().mapToInt(CabinetItem::widthMm).sum();
        if (wallLength < baseWidth) {
            cabinets.add(catalog.filler(Math.max(300, wallLength), position));
            return;
        }
        cabinets.addAll(catalog.standardRun(position));
        int remaining = wallLength - baseWidth;
        while (remaining >= 300) {
            int width = Math.min(600, remaining);
            cabinets.add(catalog.filler(width, position));
            remaining -= width;
        }
    }
}
