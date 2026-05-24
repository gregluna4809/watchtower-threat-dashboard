package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.EndpointDto;
import com.gluna.watchtower.api.dto.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/endpoints")
public class EndpointController {

    private final ApiReadService apiReadService;

    public EndpointController(ApiReadService apiReadService) {
        this.apiReadService = apiReadService;
    }

    @GetMapping
    public Page<EndpointDto> endpoints(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return apiReadService.endpoints(limit, offset);
    }

    @GetMapping("/{id}")
    public EndpointDto endpoint(@PathVariable Long id) {
        return apiReadService.endpoint(id);
    }
}

