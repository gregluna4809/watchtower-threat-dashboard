package com.gluna.watchtower.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gluna.watchtower.service.HoneypotService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HoneypotControllerTest {

    private MockMvc mockMvc;

    private final HoneypotService honeypotService = org.mockito.Mockito.mock(HoneypotService.class);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HoneypotController(honeypotService))
                .build();
    }

    @Test
    void recordsHoneypotGetAndReturnsHarmlessOk() throws Exception {
        mockMvc.perform(get("/honeypot/wp-admin").header("User-Agent", "scanner"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));

        verify(honeypotService).record(any(HttpServletRequest.class));
    }

    @Test
    void recordsHoneypotPostAndDoesNotEchoRequestBody() throws Exception {
        mockMvc.perform(post("/honeypot/login").content("password=secret"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));

        verify(honeypotService).record(any(HttpServletRequest.class));
    }
}
