package com.cabinetai.design.service;

import com.cabinetai.design.model.DesignRequest;
import com.cabinetai.design.model.DesignResponse;
import com.cabinetai.design.model.LayoutType;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DesignServiceTest {
    private final DesignService service = new DesignService(new CabinetCatalog());

    @Test
    void generatesStandardStraightRun() {
        DesignResponse response = service.generate(new DesignRequest(LayoutType.STRAIGHT, 3200, 0, 0, 20000, "现代", List.of()));
        assertEquals("一字型", response.layoutType());
        assertEquals(5, response.cabinets().size());
        assertEquals(0, response.remainingWallSpaceMm());
        assertFalse(response.warnings().isEmpty());
    }
}
