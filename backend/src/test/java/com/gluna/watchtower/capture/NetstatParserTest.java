package com.gluna.watchtower.capture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NetstatParserTest {

    private final NetstatParser parser = new NetstatParser();

    @Test
    void parsesTcpUdpIpv4AndIpv6Rows() {
        Instant observedAt = Instant.parse("2026-05-24T12:00:00Z");

        List<ConnectionSnapshot> snapshots = parser.parse(loadFixture(), observedAt);

        assertThat(snapshots).hasSize(7);
        assertThat(snapshots)
                .extracting(ConnectionSnapshot::protocol)
                .containsExactly("TCP", "TCP", "TCP", "TCP", "TCP", "UDP", "UDP");
        assertThat(snapshots)
                .extracting(ConnectionSnapshot::observedAt)
                .containsOnly(observedAt);
    }

    @Test
    void parsesTcpStateAndPid() {
        List<ConnectionSnapshot> snapshots = parser.parse(
                loadFixture(),
                Instant.parse("2026-05-24T12:00:00Z")
        );

        ConnectionSnapshot established = snapshots.get(1);

        assertThat(established.protocol()).isEqualTo("TCP");
        assertThat(established.localIp()).isEqualTo("192.168.1.25");
        assertThat(established.localPort()).isEqualTo(53142);
        assertThat(established.remoteIp()).isEqualTo("93.184.216.34");
        assertThat(established.remotePort()).isEqualTo(443);
        assertThat(established.state()).isEqualTo("ESTABLISHED");
        assertThat(established.pid()).isEqualTo(4321);
    }

    @Test
    void stripsIpv6BracketsAndZoneIdentifiers() {
        List<ConnectionSnapshot> snapshots = parser.parse(
                loadFixture(),
                Instant.parse("2026-05-24T12:00:00Z")
        );

        ConnectionSnapshot ipv6 = snapshots.get(4);

        assertThat(ipv6.localIp()).isEqualTo("fe80::1");
        assertThat(ipv6.localPort()).isEqualTo(53144);
        assertThat(ipv6.remoteIp()).isEqualTo("2606:4700:4700::1111");
        assertThat(ipv6.remotePort()).isEqualTo(443);
    }

    @Test
    void parsesUdpWildcardRemoteEndpointAsNull() {
        List<ConnectionSnapshot> snapshots = parser.parse(
                loadFixture(),
                Instant.parse("2026-05-24T12:00:00Z")
        );

        ConnectionSnapshot udp = snapshots.get(5);

        assertThat(udp.protocol()).isEqualTo("UDP");
        assertThat(udp.localIp()).isEqualTo("0.0.0.0");
        assertThat(udp.localPort()).isEqualTo(5353);
        assertThat(udp.remoteIp()).isNull();
        assertThat(udp.remotePort()).isNull();
        assertThat(udp.state()).isNull();
        assertThat(udp.pid()).isEqualTo(2222);
    }

    private List<String> loadFixture() {
        try (var input = getClass().getResourceAsStream("/netstat-sample.txt")) {
            if (input == null) {
                throw new IllegalStateException("Missing netstat-sample.txt fixture");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}

