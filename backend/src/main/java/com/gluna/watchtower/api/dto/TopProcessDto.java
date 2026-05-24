package com.gluna.watchtower.api.dto;

public record TopProcessDto(
        Long id,
        Integer pid,
        String name,
        Integer maxScore
) {
}

