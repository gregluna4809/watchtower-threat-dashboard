package com.gluna.watchtower.enrichment;

public record GeoIpResult(
        Integer asn,
        String asnOrg,
        String countryIso,
        String countryName,
        String city
) {
}

