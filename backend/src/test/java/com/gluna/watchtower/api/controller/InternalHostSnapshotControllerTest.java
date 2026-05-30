package com.gluna.watchtower.api.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gluna.watchtower.capture.ConnectionSnapshot;
import com.gluna.watchtower.service.IngestService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InternalHostSnapshotControllerTest {

    private IngestService ingestService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ingestService = Mockito.mock(IngestService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalHostSnapshotController(ingestService, "observer-secret"))
                .build();
    }

    @Test
    void rejectsMissingBearerToken() throws Exception {
        mockMvc.perform(post("/internal/v1/host-snapshots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("unauthorized"))
                .andExpect(jsonPath("$.accepted").value(0));

        verify(ingestService, never()).ingestSnapshots(anyList());
    }

    @Test
    void acceptsAuthorizedSnapshotsAndDelegatesToIngestService() throws Exception {
        mockMvc.perform(post("/internal/v1/host-snapshots")
                        .header("Authorization", "Bearer observer-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.accepted").value(1));

        ArgumentCaptor<List<ConnectionSnapshot>> captor = ArgumentCaptor.captor();
        verify(ingestService).ingestSnapshots(captor.capture());
        ConnectionSnapshot snapshot = captor.getValue().getFirst();
        org.assertj.core.api.Assertions.assertThat(snapshot.protocol()).isEqualTo("TCP");
        org.assertj.core.api.Assertions.assertThat(snapshot.localIp()).isEqualTo("24.199.87.150");
        org.assertj.core.api.Assertions.assertThat(snapshot.localPort()).isEqualTo(22);
        org.assertj.core.api.Assertions.assertThat(snapshot.remoteIp()).isEqualTo("71.251.0.115");
        org.assertj.core.api.Assertions.assertThat(snapshot.remotePort()).isEqualTo(64373);
        org.assertj.core.api.Assertions.assertThat(snapshot.state()).isEqualTo("ESTABLISHED");
        org.assertj.core.api.Assertions.assertThat(snapshot.pid()).isEqualTo(944);
    }

    private String validPayload() {
        return """
                {
                  "observedAt": "2026-05-30T21:30:00Z",
                  "snapshots": [
                    {
                      "protocol": "tcp",
                      "localIp": "24.199.87.150",
                      "localPort": 22,
                      "remoteIp": "71.251.0.115",
                      "remotePort": 64373,
                      "state": "established",
                      "pid": 944
                    }
                  ]
                }
                """;
    }
}
