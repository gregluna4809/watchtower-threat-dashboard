package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.ConnectionDto;
import com.gluna.watchtower.api.dto.EndpointDto;
import com.gluna.watchtower.api.dto.EndpointScoresDto;
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
@RequestMapping("/api/v1/endpoints")
public class EndpointController {

    private final ApiReadService apiReadService;
    private final ObservationBucketingService observationBucketingService;

    public EndpointController(ApiReadService apiReadService, ObservationBucketingService observationBucketingService) {
        this.apiReadService = apiReadService;
        this.observationBucketingService = observationBucketingService;
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

    @GetMapping("/{id}/connections")
    public Page<ConnectionDto> endpointConnections(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        return apiReadService.endpointConnections(id, limit, offset);
    }

    @GetMapping("/{id}/observations")
    public List<ObservationBucket> endpointObservations(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "24h") String window
    ) {
        apiReadService.endpoint(id);
        ObservationWindows.ObservationWindow observationWindow = ObservationWindows.parse(window);
        return observationBucketingService.bucket("ENDPOINT", id, observationWindow.window(), observationWindow.bucketSize());
    }

    @GetMapping("/{id}/scores")
    public EndpointScoresDto endpointScores(
            @PathVariable Long id,
            @RequestParam(required = false) Integer limit
    ) {
        return apiReadService.endpointScores(id, limit);
    }
}
