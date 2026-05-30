package com.gluna.watchtower.api.controller;

import com.gluna.watchtower.api.dto.HostSnapshotRequest;
import com.gluna.watchtower.api.dto.HostSnapshotResponse;
import com.gluna.watchtower.api.dto.HostSocketSnapshot;
import com.gluna.watchtower.capture.ConnectionSnapshot;
import com.gluna.watchtower.service.IngestService;
import jakarta.validation.Valid;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalHostSnapshotController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final IngestService ingestService;
    private final String observerToken;

    public InternalHostSnapshotController(
            IngestService ingestService,
            @Value("${watchtower.observer.token:${WATCHTOWER_OBSERVER_TOKEN:}}") String observerToken
    ) {
        this.ingestService = ingestService;
        this.observerToken = observerToken == null ? "" : observerToken;
    }

    @PostMapping("/host-snapshots")
    public ResponseEntity<HostSnapshotResponse> ingestHostSnapshots(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody HostSnapshotRequest request
    ) {
        if (!authorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HostSnapshotResponse("unauthorized", 0));
        }

        List<ConnectionSnapshot> snapshots = request.snapshots().stream()
                .map(snapshot -> toConnectionSnapshot(snapshot, request))
                .toList();
        ingestService.ingestSnapshots(snapshots);
        return ResponseEntity.ok(new HostSnapshotResponse("ok", snapshots.size()));
    }

    private boolean authorized(String authorization) {
        if (observerToken.isBlank() || authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        String provided = authorization.substring(BEARER_PREFIX.length());
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                observerToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private ConnectionSnapshot toConnectionSnapshot(HostSocketSnapshot snapshot, HostSnapshotRequest request) {
        String protocol = normalizeProtocol(snapshot.protocol());
        String state = normalizeState(snapshot.state());
        String localIp = validateIp(snapshot.localIp(), "localIp");
        String remoteIp = snapshot.remoteIp() == null || snapshot.remoteIp().isBlank()
                ? null
                : validateIp(snapshot.remoteIp(), "remoteIp");

        return new ConnectionSnapshot(
                protocol,
                localIp,
                snapshot.localPort(),
                remoteIp,
                snapshot.remotePort(),
                state,
                snapshot.pid(),
                request.observedAt()
        );
    }

    private String normalizeProtocol(String protocol) {
        String normalized = protocol.toUpperCase(Locale.ROOT);
        if (!"TCP".equals(normalized) && !"UDP".equals(normalized)) {
            throw new IllegalArgumentException("protocol must be TCP or UDP");
        }
        return normalized;
    }

    private String normalizeState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        return state.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String validateIp(String ip, String field) {
        try {
            return InetAddress.getByName(ip).getHostAddress();
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException(field + " must be a valid IP address");
        }
    }
}
