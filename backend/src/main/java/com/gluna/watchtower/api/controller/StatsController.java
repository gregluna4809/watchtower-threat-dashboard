package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.ApiReadService;
import com.gluna.watchtower.api.dto.ScoreTimelinePoint;
import com.gluna.watchtower.api.dto.SummaryDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final ApiReadService apiReadService;

    public StatsController(ApiReadService apiReadService) {
        this.apiReadService = apiReadService;
    }

    @GetMapping("/summary")
    public SummaryDto summary() {
        return apiReadService.summary();
    }

    @GetMapping("/score-timeline")
    public List<ScoreTimelinePoint> scoreTimeline(@RequestParam(defaultValue = "1h") String window) {
        return apiReadService.scoreTimeline(window);
    }
}
