package com.gluna.watchtower.api.dto;

import java.time.OffsetDateTime;

public record EndpointDto(
        Long id,
        String ip,
        Integer asn,
        String asnOrg,
        String countryIso,
        String countryName,
        String city,
        Double latitude,
        Double longitude,
        String reverseDns,
        Long connectionCount,
        Integer latestScore,
        OffsetDateTime firstSeen,
        OffsetDateTime lastSeen
) {
}
