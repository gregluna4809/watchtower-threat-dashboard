package com.gluna.watchtower.enrichment;

import com.gluna.watchtower.model.IntelCache;
import com.gluna.watchtower.model.IntelSource;
import com.gluna.watchtower.repo.IntelCacheRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ThreatIntelService {

    private static final Logger log = LoggerFactory.getLogger(ThreatIntelService.class);
    private static final int CLEAN_TTL_SECONDS = 24 * 60 * 60;
    private static final int SUSPICIOUS_TTL_SECONDS = 6 * 60 * 60;

    private final IntelCacheRepository intelCacheRepository;
    private final GeoIpService geoIpService;
    private final RestClient restClient;
    private final boolean enabled;
    private final String apiKey;
    private final int perHourLimit;
    private final int perDayLimit;
    private final AtomicInteger hourlyLookups = new AtomicInteger();
    private final AtomicInteger dailyLookups = new AtomicInteger();

    public ThreatIntelService(
            IntelCacheRepository intelCacheRepository,
            GeoIpService geoIpService,
            @Value("${watchtower.intel.abuseipdb.enabled:true}") boolean enabled,
            @Value("${watchtower.intel.abuseipdb.api-key:}") String apiKey,
            @Value("${watchtower.intel.abuseipdb.base-url}") String baseUrl,
            @Value("${watchtower.intel.rate-limit.per-hour:30}") int perHourLimit,
            @Value("${watchtower.intel.rate-limit.per-day:500}") int perDayLimit
    ) {
        this.intelCacheRepository = intelCacheRepository;
        this.geoIpService = geoIpService;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.perHourLimit = perHourLimit;
        this.perDayLimit = perDayLimit;
    }

    @PostConstruct
    public void warnIfDisabled() {
        if (enabled && apiKey.isBlank()) {
            log.warn("AbuseIPDB API key is not configured. Set ABUSEIPDB_API_KEY to enable AbuseIPDB enrichment.");
        }
    }

    @Transactional
    public Optional<AbuseIpdbResult> checkIp(String ip) {
        if (!enabled || apiKey.isBlank() || !geoIpService.isPublicAddress(ip)) {
            return Optional.empty();
        }

        Optional<IntelCache> cached = intelCacheRepository.findByIpAndSource(ip, IntelSource.ABUSEIPDB);
        if (cached.isPresent() && isFresh(cached.get())) {
            return Optional.of(toResult(cached.get().getPayload()));
        }

        if (!consumeToken()) {
            log.debug("AbuseIPDB lookup skipped for {} because the in-memory rate limit is exhausted", ip);
            return Optional.empty();
        }

        Optional<Map<String, Object>> payload = fetchAbuseIpdb(ip);
        if (payload.isEmpty()) {
            return Optional.empty();
        }

        AbuseIpdbResult result = toResult(payload.get());
        IntelCache cache = cached.orElseGet(IntelCache::new);
        cache.setIp(ip);
        cache.setSource(IntelSource.ABUSEIPDB);
        cache.setPayload(payload.get());
        cache.setFetchedAt(OffsetDateTime.now(ZoneOffset.UTC));
        cache.setTtlSeconds(result.abuseConfidenceScore() >= 25 ? SUSPICIOUS_TTL_SECONDS : CLEAN_TTL_SECONDS);
        intelCacheRepository.save(cache);
        return Optional.of(result);
    }

    @Scheduled(fixedRate = 60 * 60 * 1000L)
    public void resetHourlyLimit() {
        hourlyLookups.set(0);
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000L)
    public void resetDailyLimit() {
        dailyLookups.set(0);
    }

    private boolean isFresh(IntelCache cache) {
        OffsetDateTime expiresAt = cache.getFetchedAt().plusSeconds(cache.getTtlSeconds());
        return expiresAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private boolean consumeToken() {
        while (true) {
            int currentHour = hourlyLookups.get();
            int currentDay = dailyLookups.get();
            if (currentHour >= perHourLimit || currentDay >= perDayLimit) {
                return false;
            }
            if (hourlyLookups.compareAndSet(currentHour, currentHour + 1)) {
                dailyLookups.incrementAndGet();
                return true;
            }
        }
    }

    private Optional<Map<String, Object>> fetchAbuseIpdb(String ip) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/check")
                            .queryParam("ipAddress", ip)
                            .queryParam("maxAgeInDays", 90)
                            .build())
                    .header("Key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            Object data = response == null ? null : response.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                return Optional.of(copyStringKeyedMap(dataMap));
            }
            log.warn("AbuseIPDB response for {} did not contain a data object", ip);
            return Optional.empty();
        } catch (RestClientResponseException ex) {
            log.warn("AbuseIPDB lookup failed for {} with HTTP {}", ip, ex.getStatusCode().value());
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("AbuseIPDB lookup failed for {}: {}", ip, ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> copyStringKeyedMap(Map<?, ?> input) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (entry.getKey() != null) {
                output.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return output;
    }

    private AbuseIpdbResult toResult(Map<String, Object> payload) {
        return new AbuseIpdbResult(
                intValue(payload.get("abuseConfidenceScore")),
                intValue(payload.get("totalReports")),
                stringValue(payload.get("countryCode")),
                stringValue(payload.get("usageType")),
                stringValue(payload.get("isp")),
                stringValue(payload.get("domain"))
        );
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        return 0;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}

