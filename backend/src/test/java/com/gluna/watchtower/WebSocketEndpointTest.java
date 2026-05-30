package com.gluna.watchtower;

import static org.assertj.core.api.Assertions.assertThat;

import com.gluna.watchtower.ws.WebSocketConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = WebSocketEndpointTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.task.scheduling.enabled=false",
                "watchtower.websocket.allowed-origin-patterns=http://127.0.0.1:5173,http://localhost:5173,https://watchtower.pulse-forge.com"
        }
)
class WebSocketEndpointTest {

    @LocalServerPort
    private int port;

    @Test
    void sockJsInfoEndpointIsRegistered() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://127.0.0.1:" + port + "/ws/info",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"websocket\"");
        assertThat(response.getBody()).contains("\"cookie_needed\":false");
    }

    @Test
    void sockJsInfoEndpointAllowsConfiguredProductionOrigin() {
        TestRestTemplate restTemplate = new TestRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://watchtower.pulse-forge.com");

        ResponseEntity<String> response = restTemplate.exchange(
                "http://127.0.0.1:" + port + "/ws/info",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                new Object[0]
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"websocket\"");
        assertThat(response.getBody()).contains("\"cookie_needed\":false");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import(WebSocketConfig.class)
    static class TestApplication {
    }
}
