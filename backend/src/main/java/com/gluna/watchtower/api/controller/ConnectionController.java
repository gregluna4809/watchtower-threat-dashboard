package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.ConnectionDto;
import com.gluna.watchtower.api.dto.ObservationBucket;
import com.gluna.watchtower.api.dto.Page;
import com.gluna.watchtower.service.ObservationBucketingService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {

    private final ApiReadService apiReadService;
    private final ObservationBucketingService observationBucketingService;

    public ConnectionController(ApiReadService apiReadService, ObservationBucketingService observationBucketingService) {
        this.apiReadService = apiReadService;
        this.observationBucketingService = observationBucketingService;
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

    @GetMapping("/{id}/observations")
    public List<ObservationBucket> connectionObservations(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "24h") String window
    ) {
        apiReadService.connection(id);
        ObservationWindows.ObservationWindow observationWindow = ObservationWindows.parse(window);
        return observationBucketingService.bucket("CONNECTION", id, observationWindow.window(), observationWindow.bucketSize());
    }
}
