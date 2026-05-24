package com.gluna.watchtower.api.dto;

import java.util.List;

public record SummaryDto(
        Long totalConnections,
        Long distinctProcesses,
        Long distinctEndpoints,
        Double meanScore,
        List<TopProcessDto> topProcesses
) {
}

