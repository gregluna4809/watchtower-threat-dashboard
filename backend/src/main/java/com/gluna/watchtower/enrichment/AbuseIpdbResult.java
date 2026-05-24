package com.gluna.watchtower.enrichment;

public record AbuseIpdbResult(
        int abuseConfidenceScore,
        int totalReports,
        String countryCode,
        String usageType,
        String isp,
        String domain
) {
}

