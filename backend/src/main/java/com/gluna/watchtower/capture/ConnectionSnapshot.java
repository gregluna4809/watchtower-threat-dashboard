package com.gluna.watchtower.capture;

import java.time.Instant;

public record ConnectionSnapshot(
        String protocol,
        String localIp,
        int localPort,
        String remoteIp,
        Integer remotePort,
        String state,
        Integer pid,
        Instant observedAt
) {
}

