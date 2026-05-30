package com.gluna.watchtower.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HostSocketSnapshot(
        @NotBlank
        String protocol,

        @NotBlank
        String localIp,

        @NotNull
        @Min(1)
        @Max(65535)
        Integer localPort,

        String remoteIp,

        @Min(1)
        @Max(65535)
        Integer remotePort,

        String state,

        @Min(0)
        Integer pid
) {
}
