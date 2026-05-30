package com.gluna.watchtower.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record HostSnapshotRequest(
        @NotNull
        Instant observedAt,

        @NotEmpty
        @Size(max = 5000)
        List<@Valid HostSocketSnapshot> snapshots
) {
}
