package com.cabinetai.ai;

import com.cabinetai.design.model.DesignRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiDesignController {
    private final AiDesignService service;

    public AiDesignController(AiDesignService service) { this.service = service; }

    @PostMapping(value = "/design", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JsonNode design(@RequestParam int budget, @RequestParam(required = false) String style,
            @RequestParam(required = false) String needs, @RequestParam MultipartFile cadFile) throws Exception {
        if (cadFile.isEmpty() || cadFile.getOriginalFilename() == null
                || !cadFile.getOriginalFilename().toLowerCase().endsWith(".dxf")) {
            throw new IllegalArgumentException("请上传有效的 DXF 墙体图纸");
        }
        List<String> needList = needs == null || needs.isBlank() ? List.of()
                : Arrays.stream(needs.split(";")).filter(s -> !s.isBlank()).toList();
        DesignRequest request = new DesignRequest(budget, style, needList);
        return service.recommend(request, new String(cadFile.getBytes(), StandardCharsets.UTF_8));
    }
}
