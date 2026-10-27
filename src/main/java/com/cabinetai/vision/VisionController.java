package com.cabinetai.vision;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/vision")
public class VisionController {
    private final VisionAnalysisService visionService;
    private final DxfStructureService dxfService;

    public VisionController(VisionAnalysisService visionService, DxfStructureService dxfService) {
        this.visionService = visionService;
        this.dxfService = dxfService;
    }

    @PostMapping(value = "/floorplan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> analyzeFloorPlan(@RequestParam MultipartFile file) throws Exception {
        requireImage(file);
        return visionService.analyzeFloorPlan(file.getBytes());
    }

    @PostMapping(value = "/cad-structure", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> analyzeCad(@RequestParam MultipartFile file) throws Exception {
        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".dxf")) {
            throw new IllegalArgumentException("请上传 ASCII DXF 文件");
        }
        return dxfService.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/match", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> match(@RequestParam MultipartFile reference,
            @RequestParam MultipartFile actual) throws Exception {
        requireImage(reference);
        requireImage(actual);
        return visionService.compare(reference.getBytes(), actual.getBytes());
    }

    private void requireImage(MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("请上传 PNG 或 JPG 图片");
        }
    }
}
