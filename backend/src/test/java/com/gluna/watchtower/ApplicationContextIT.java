package com.gluna.watchtower;

import static org.assertj.core.api.Assertions.assertThat;

import com.gluna.watchtower.capture.NetstatPoller;
import com.gluna.watchtower.enrichment.EnrichmentService;
import com.gluna.watchtower.enrichment.ThreatIntelService;
import com.gluna.watchtower.scoring.ScoringService;
import com.gluna.watchtower.service.IngestService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.task.scheduling.enabled=false"
)
class ApplicationContextIT {

    @LocalServerPort
    private int port;

    @MockitoBean
    private NetstatPoller netstatPoller;

    @MockitoBean
    private IngestService ingestService;

    @MockitoBean
    private EnrichmentService enrichmentService;

    @MockitoBean
    private ThreatIntelService threatIntelService;

    @MockitoBean
    private ScoringService scoringService;

    @Test
    void contextLoadsOnRandomPort() {
        assertThat(port).isPositive();
    }
}
