package com.cabinetai.design.controller;

import com.cabinetai.design.model.DesignRequest;
import com.cabinetai.design.model.DesignResponse;
import com.cabinetai.design.service.DesignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/designs")
public class DesignController {
    private final DesignService designService;

    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @PostMapping("/generate")
    public ResponseEntity<DesignResponse> generate(@Valid @RequestBody DesignRequest request) {
        return ResponseEntity.ok(designService.generate(request));
    }
}
