package com.gluna.watchtower.api.dto;

public record EndpointSummary(
        Long id,
        String ip,
        String countryIso,
        String countryName,
        String asnOrg
) {
}

