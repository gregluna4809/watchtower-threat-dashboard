package com.gluna.watchtower.api.dto;

public record HostSnapshotResponse(
        String status,
        int accepted
) {
}
