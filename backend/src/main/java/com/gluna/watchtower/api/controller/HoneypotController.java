package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.dto.HoneypotSummaryDto;
import com.gluna.watchtower.service.HoneypotService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HoneypotController {

    private final HoneypotService honeypotService;

    public HoneypotController(HoneypotService honeypotService) {
        this.honeypotService = honeypotService;
    }

    @RequestMapping(
            path = {"/honeypot", "/honeypot/**"},
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST,
                    RequestMethod.PUT,
                    RequestMethod.PATCH,
                    RequestMethod.DELETE,
                    RequestMethod.OPTIONS,
                    RequestMethod.HEAD
            }
    )
    public ResponseEntity<Map<String, String>> capture(HttpServletRequest request) {
        honeypotService.record(request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/api/v1/honeypot/summary")
    public HoneypotSummaryDto summary() {
        return honeypotService.summary();
    }
}
