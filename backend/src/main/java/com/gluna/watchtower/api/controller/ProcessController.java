package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.ConnectionDto;
import com.gluna.watchtower.api.dto.ObservationBucket;
import com.gluna.watchtower.api.dto.Page;
import com.gluna.watchtower.api.dto.ProcessDto;
import com.gluna.watchtower.service.ObservationBucketingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessController {

    private final ApiReadService apiReadService;
    private final ObservationBucketingService observationBucketingService;

    public ProcessController(ApiReadService apiReadService, ObservationBucketingService observationBucketingService) {
        this.apiReadService = apiReadService;
        this.observationBucketingService = observationBucketingService;
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

    @GetMapping("/{id}/connections")
    public Page<ConnectionDto> processConnections(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return apiReadService.processConnections(id, limit, offset);
    }

    @GetMapping("/{id}/observations")
    public List<ObservationBucket> processObservations(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "24h") String window
    ) {
        apiReadService.process(id);
        ObservationWindows.ObservationWindow observationWindow = ObservationWindows.parse(window);
        return observationBucketingService.bucket("PROCESS", id, observationWindow.window(), observationWindow.bucketSize());
    }
}
