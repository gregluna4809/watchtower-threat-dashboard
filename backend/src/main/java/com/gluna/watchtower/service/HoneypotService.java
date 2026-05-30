package com.gluna.watchtower.service;

import com.gluna.watchtower.api.dto.HoneypotRequestDto;
import com.gluna.watchtower.api.dto.HoneypotSummaryDto;
import com.gluna.watchtower.api.dto.HoneypotUserAgentDto;
import com.gluna.watchtower.model.HoneypotHit;
import com.gluna.watchtower.repo.HoneypotHitRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HoneypotService {

    private static final Logger log = LoggerFactory.getLogger(HoneypotService.class);
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");
    private static final Pattern IPV6_PATTERN = Pattern.compile("[0-9a-fA-F:.%]+");
    private static final int MAX_METHOD_LENGTH = 16;
    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final String UNKNOWN_SOURCE_IP = "0.0.0.0";

    private final HoneypotHitRepository honeypotHitRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public HoneypotService(
            HoneypotHitRepository honeypotHitRepository,
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.honeypotHitRepository = honeypotHitRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(HttpServletRequest request) {
        try {
            HoneypotHit hit = new HoneypotHit();
            hit.setSourceIp(resolveSourceIp(request));
            hit.setObservedAt(OffsetDateTime.now(ZoneOffset.UTC));
            hit.setRequestPath(sanitizePath(request.getRequestURI()));
            hit.setUserAgent(truncate(blankToNull(request.getHeader("User-Agent")), MAX_USER_AGENT_LENGTH));
            hit.setMethod(truncate(request.getMethod(), MAX_METHOD_LENGTH));
            honeypotHitRepository.save(hit);
        } catch (RuntimeException ex) {
            log.warn("Failed to record honeypot hit: {}", ex.getMessage());
        }
    }

    public HoneypotSummaryDto summary() {
        Long totalHits = longValue(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM honeypot_hits",
                Map.of(),
                Long.class
        ));
        Long uniqueIps = longValue(jdbcTemplate.queryForObject(
                "SELECT count(DISTINCT source_ip) FROM honeypot_hits",
                Map.of(),
                Long.class
        ));
        List<HoneypotUserAgentDto> topUserAgents = jdbcTemplate.query(
                """
                SELECT coalesce(nullif(user_agent, ''), 'Unknown') AS user_agent, count(*) AS hit_count
                FROM honeypot_hits
                GROUP BY coalesce(nullif(user_agent, ''), 'Unknown')
                ORDER BY hit_count DESC, user_agent ASC
                LIMIT 5
                """,
                Map.of(),
                (rs, rowNum) -> new HoneypotUserAgentDto(
                        rs.getString("user_agent"),
                        rs.getObject("hit_count", Long.class)
                )
        );
        List<HoneypotRequestDto> recentRequests = jdbcTemplate.query(
                """
                SELECT id, host(source_ip) AS source_ip, observed_at, request_path, user_agent, method
                FROM honeypot_hits
                ORDER BY observed_at DESC
                LIMIT 10
                """,
                Map.of(),
                (rs, rowNum) -> honeypotRequestDto(rs)
        );
        return new HoneypotSummaryDto(totalHits, uniqueIps, topUserAgents, recentRequests);
    }

    private HoneypotRequestDto honeypotRequestDto(ResultSet rs) throws SQLException {
        return new HoneypotRequestDto(
                rs.getObject("id", Long.class),
                rs.getString("source_ip"),
                rs.getObject("observed_at", OffsetDateTime.class),
                rs.getString("request_path"),
                rs.getString("user_agent"),
                rs.getString("method")
        );
    }

    private String resolveSourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",", 2)[0].trim();
            String normalized = normalizeIp(first);
            if (normalized != null) {
                return normalized;
            }
        }

        String realIp = normalizeIp(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String remoteAddress = normalizeIp(request.getRemoteAddr());
        return remoteAddress == null ? UNKNOWN_SOURCE_IP : remoteAddress;
    }

    private String normalizeIp(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }

        String value = candidate.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        int zoneIndex = value.indexOf('%');
        if (zoneIndex >= 0) {
            value = value.substring(0, zoneIndex);
        }

        if (IPV4_PATTERN.matcher(value).matches() && !validIpv4(value)) {
            return null;
        }
        boolean ipv4 = IPV4_PATTERN.matcher(value).matches();
        boolean ipv6 = value.contains(":") && IPV6_PATTERN.matcher(value).matches();
        if (!ipv4 && !ipv6) {
            return null;
        }

        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private boolean validIpv4(String value) {
        String[] parts = value.split("\\.");
        for (String part : parts) {
            int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                return false;
            }
            if (octet < 0 || octet > 255) {
                return false;
            }
        }
        return true;
    }

    private String sanitizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/honeypot";
        }
        return path.length() > 2048 ? path.substring(0, 2048) : path;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private Long longValue(Long value) {
        return value == null ? 0L : value;
    }
}
