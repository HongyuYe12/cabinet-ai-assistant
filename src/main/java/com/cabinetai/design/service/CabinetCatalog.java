package com.cabinetai.design.service;

import com.cabinetai.design.model.CabinetItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CabinetCatalog {
    public List<CabinetItem> standardRun(String position) {
        return List.of(
                new CabinetItem("BASE-SINK-800", "800水槽地柜", 800, 2200, position),
                new CabinetItem("BASE-DRAWER-600", "600抽屉地柜", 600, 1800, position),
                new CabinetItem("BASE-HOB-900", "900灶台地柜", 900, 2600, position));
    }

    public CabinetItem filler(int widthMm, String position) {
        return new CabinetItem("BASE-FILLER-" + widthMm, widthMm + "补充地柜", widthMm, widthMm * 2, position);
    }
}
