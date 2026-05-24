package com.gluna.watchtower.api.dto;

public record ProcessSummary(
        Long id,
        Integer pid,
        String name,
        String path,
        Boolean signed,
        String signer
) {
}

