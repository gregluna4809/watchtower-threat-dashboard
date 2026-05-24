package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.ConnectionDto;
import com.gluna.watchtower.api.dto.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {

    private final ApiReadService apiReadService;

    public ConnectionController(ApiReadService apiReadService) {
        this.apiReadService = apiReadService;
    }

    @GetMapping
    public Page<ConnectionDto> connections(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return apiReadService.connections(state, minScore, processName, country, limit, offset);
    }

    @GetMapping("/{id}")
    public ConnectionDto connection(@PathVariable Long id) {
        return apiReadService.connection(id);
    }
}

