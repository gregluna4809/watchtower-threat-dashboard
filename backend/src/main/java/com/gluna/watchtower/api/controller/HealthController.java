package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final String version;

    public HealthController(@Value("${watchtower.version:dev}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", version);
    }
}

