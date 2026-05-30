package com.gluna.watchtower.api.dto;

import java.util.List;

public record HoneypotSummaryDto(
        Long totalHits,
        Long uniqueIps,
        List<HoneypotUserAgentDto> topUserAgents,
        List<HoneypotRequestDto> recentRequests
) {
}
