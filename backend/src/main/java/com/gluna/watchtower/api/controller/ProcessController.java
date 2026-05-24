package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.Page;
import com.gluna.watchtower.api.dto.ProcessDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessController {

    private final ApiReadService apiReadService;

    public ProcessController(ApiReadService apiReadService) {
        this.apiReadService = apiReadService;
    }

    @GetMapping
    public Page<ProcessDto> processes(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return apiReadService.processes(limit, offset);
    }

    @GetMapping("/{id}")
    public ProcessDto process(@PathVariable Long id) {
        return apiReadService.process(id);
    }
}

