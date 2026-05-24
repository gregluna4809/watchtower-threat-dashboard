package com.gluna.watchtower.api.dto;

public record RuleDto(
        String code,
        String displayName,
        String description,
        Integer defaultPoints,
        Boolean enabled
) {
}

