package com.gluna.watchtower.capture;

import com.gluna.watchtower.exception.CaptureException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NetstatPoller {

    private static final Logger log = LoggerFactory.getLogger(NetstatPoller.class);

    private final WindowsCommandRunner commandRunner;
    private final NetstatParser netstatParser;
    private final Duration commandTimeout;
    private final AtomicReference<List<ConnectionSnapshot>> latest = new AtomicReference<>(List.of());

    public NetstatPoller(
            WindowsCommandRunner commandRunner,
            NetstatParser netstatParser,
            @Value("${watchtower.capture.command-timeout-ms:5000}") long commandTimeoutMs
    ) {
        this.commandRunner = commandRunner;
        this.netstatParser = netstatParser;
        this.commandTimeout = Duration.ofMillis(commandTimeoutMs);
    }

    @Scheduled(fixedDelayString = "${watchtower.capture.poll-interval-ms:3000}")
    public void poll() {
        try {
            List<String> lines = commandRunner.runLines(List.of("netstat", "-ano"), commandTimeout);
            List<ConnectionSnapshot> snapshots = netstatParser.parse(lines, Instant.now());
            latest.set(snapshots);
            logSummary(snapshots);
        } catch (CaptureException ex) {
            log.warn("Netstat poll failed: {}", ex.getMessage());
        }
    }

    public List<ConnectionSnapshot> getLatest() {
        return latest.get();
    }

    private void logSummary(List<ConnectionSnapshot> snapshots) {
        long tcpCount = snapshots.stream()
                .filter(snapshot -> "TCP".equals(snapshot.protocol()))
                .count();
        long udpCount = snapshots.stream()
                .filter(snapshot -> "UDP".equals(snapshot.protocol()))
                .count();
        long establishedCount = snapshots.stream()
                .filter(snapshot -> "ESTABLISHED".equals(snapshot.state()))
                .count();

        log.info(
                "Netstat poll: {} rows ({} TCP, {} UDP, {} established)",
                snapshots.size(),
                tcpCount,
                udpCount,
                establishedCount
        );
    }
}

