package com.cabinetai.ai;

import com.cabinetai.design.model.DesignRequest;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiDesignController {
    private final AiDesignService service;

    public AiDesignController(AiDesignService service) { this.service = service; }

    @PostMapping("/design")
    public JsonNode design(@Valid @RequestBody DesignRequest request) { return service.recommend(request); }
}
