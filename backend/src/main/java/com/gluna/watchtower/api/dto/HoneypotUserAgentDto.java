package com.gluna.watchtower.api.dto;

public record HoneypotUserAgentDto(
        String userAgent,
        Long count
) {
}
