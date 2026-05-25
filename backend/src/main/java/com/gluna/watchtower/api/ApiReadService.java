package com.gluna.watchtower.api;

import com.gluna.watchtower.api.dto.ConnectionDto;
import com.gluna.watchtower.api.dto.EndpointDto;
import com.gluna.watchtower.api.dto.EndpointScoresDto;
import com.gluna.watchtower.api.dto.EndpointSummary;
import com.gluna.watchtower.api.dto.Page;
import com.gluna.watchtower.api.dto.ProcessDto;
import com.gluna.watchtower.api.dto.ProcessSummary;
import com.gluna.watchtower.api.dto.ScoreTimelinePoint;
import com.gluna.watchtower.api.dto.SummaryDto;
import com.gluna.watchtower.api.dto.ThreatScoreDto;
import com.gluna.watchtower.api.dto.TopProcessDto;
import com.gluna.watchtower.exception.NotFoundException;
import com.gluna.watchtower.model.TargetType;
import com.gluna.watchtower.model.ThreatScore;
import com.gluna.watchtower.repo.ThreatScoreRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApiReadService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ThreatScoreRepository threatScoreRepository;

    public ApiReadService(NamedParameterJdbcTemplate jdbcTemplate, ThreatScoreRepository threatScoreRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.threatScoreRepository = threatScoreRepository;
    }

    public Page<ConnectionDto> connections(
            String state,
            Integer minScore,
            String processName,
            String country,
            Integer requestedLimit,
            Integer requestedOffset
    ) {
        Pagination pagination = pagination(requestedLimit, requestedOffset);
        QueryParts queryParts = connectionFilters(state, minScore, processName, country);
        queryParts.params().put("limit", pagination.limit());
        queryParts.params().put("offset", pagination.offset());

        String from = """
                FROM connections c
                LEFT JOIN processes p ON p.id = c.process_id
                LEFT JOIN remote_endpoints e ON e.id = c.endpoint_id
                LEFT JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                """;
        String select = """
                SELECT c.id, c.protocol, c.state, host(c.local_ip) AS local_ip, c.local_port,
                       host(e.ip) AS remote_ip, c.remote_port,
                       p.id AS process_id, p.pid, p.name AS process_name, p.path, p.signed, p.signer,
                       e.id AS endpoint_id, e.country_iso, e.country_name, e.asn_org,
                       ls.score AS latest_score,
                       c.first_seen, c.last_seen, c.observation_count
                """;

        List<ConnectionDto> items = jdbcTemplate.query(
                select + from + queryParts.where() + " ORDER BY c.last_seen DESC LIMIT :limit OFFSET :offset",
                queryParts.params(),
                (rs, rowNum) -> connectionDto(rs)
        );
        int total = count("SELECT count(*) " + from + queryParts.where(), queryParts.params());
        return new Page<>(items, total, pagination.limit(), pagination.offset());
    }

    public ConnectionDto connection(Long id) {
        Map<String, Object> params = Map.of("id", id);
        String sql = """
                SELECT c.id, c.protocol, c.state, host(c.local_ip) AS local_ip, c.local_port,
                       host(e.ip) AS remote_ip, c.remote_port,
                       p.id AS process_id, p.pid, p.name AS process_name, p.path, p.signed, p.signer,
                       e.id AS endpoint_id, e.country_iso, e.country_name, e.asn_org,
                       ls.score AS latest_score,
                       c.first_seen, c.last_seen, c.observation_count
                FROM connections c
                LEFT JOIN processes p ON p.id = c.process_id
                LEFT JOIN remote_endpoints e ON e.id = c.endpoint_id
                LEFT JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                WHERE c.id = :id
                """;
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> connectionDto(rs))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Connection not found: " + id));
    }

    public Page<ConnectionDto> endpointConnections(Long endpointId, Integer requestedLimit, Integer requestedOffset) {
        endpoint(endpointId);
        return connectionsByFilter("c.endpoint_id = :endpointId", Map.of("endpointId", endpointId), requestedLimit, requestedOffset);
    }

    public Page<ConnectionDto> processConnections(Long processId, Integer requestedLimit, Integer requestedOffset) {
        process(processId);
        return connectionsByFilter("c.process_id = :processId", Map.of("processId", processId), requestedLimit, requestedOffset);
    }

    public Page<ProcessDto> processes(Integer requestedLimit, Integer requestedOffset) {
        Pagination pagination = pagination(requestedLimit, requestedOffset);
        Map<String, Object> params = new HashMap<>();
        params.put("limit", pagination.limit());
        params.put("offset", pagination.offset());
        String from = """
                FROM processes p
                LEFT JOIN connections c ON c.process_id = p.id
                LEFT JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                """;
        String group = " GROUP BY p.id, p.pid, p.name, p.path, p.signed, p.signer, p.first_seen, p.last_seen ";
        List<ProcessDto> items = jdbcTemplate.query(
                """
                SELECT p.id, p.pid, p.name, p.path, p.signed, p.signer,
                       count(c.id) AS connection_count, max(ls.score) AS max_score,
                       p.first_seen, p.last_seen
                """
                        + from
                        + group
                        + " ORDER BY p.last_seen DESC LIMIT :limit OFFSET :offset",
                params,
                (rs, rowNum) -> processDto(rs)
        );
        int total = count("SELECT count(*) FROM processes", Map.of());
        return new Page<>(items, total, pagination.limit(), pagination.offset());
    }

    public ProcessDto process(Long id) {
        Map<String, Object> params = Map.of("id", id);
        String sql = """
                SELECT p.id, p.pid, p.name, p.path, p.signed, p.signer,
                       count(c.id) AS connection_count, max(ls.score) AS max_score,
                       p.first_seen, p.last_seen
                FROM processes p
                LEFT JOIN connections c ON c.process_id = p.id
                LEFT JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                WHERE p.id = :id
                GROUP BY p.id, p.pid, p.name, p.path, p.signed, p.signer, p.first_seen, p.last_seen
                """;
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> processDto(rs))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Process not found: " + id));
    }

    public Page<EndpointDto> endpoints(Integer requestedLimit, Integer requestedOffset) {
        Pagination pagination = pagination(requestedLimit, requestedOffset);
        Map<String, Object> params = new HashMap<>();
        params.put("limit", pagination.limit());
        params.put("offset", pagination.offset());
        String from = """
                FROM remote_endpoints e
                LEFT JOIN connections c ON c.endpoint_id = e.id
                LEFT JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                """;
        String group = """
                 GROUP BY e.id, e.ip, e.asn, e.asn_org, e.country_iso, e.country_name,
                          e.city, e.latitude, e.longitude, e.reverse_dns, e.first_seen, e.last_seen
                """;
        List<EndpointDto> items = jdbcTemplate.query(
                """
                SELECT e.id, host(e.ip) AS ip, e.asn, e.asn_org, e.country_iso, e.country_name,
                       e.city, e.latitude, e.longitude,
                       e.reverse_dns, count(c.id) AS connection_count, max(ls.score) AS latest_score,
                       e.first_seen, e.last_seen
                """
                        + from
                        + group
                        + " ORDER BY e.last_seen DESC LIMIT :limit OFFSET :offset",
                params,
                (rs, rowNum) -> endpointDto(rs)
        );
        int total = count("SELECT count(*) FROM remote_endpoints", Map.of());
        return new Page<>(items, total, pagination.limit(), pagination.offset());
    }

    public EndpointDto endpoint(Long id) {
        Map<String, Object> params = Map.of("id", id);
        String sql = """
                SELECT e.id, host(e.ip) AS ip, e.asn, e.asn_org, e.country_iso, e.country_name,
                       e.city, e.latitude, e.longitude,
                       e.reverse_dns, count(c.id) AS connection_count, max(ls.score) AS latest_score,
                       e.first_seen, e.last_seen
                FROM remote_endpoints e
                LEFT JOIN connections c ON c.endpoint_id = e.id
                LEFT JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                WHERE e.id = :id
                GROUP BY e.id, e.ip, e.asn, e.asn_org, e.country_iso, e.country_name,
                         e.city, e.latitude, e.longitude, e.reverse_dns, e.first_seen, e.last_seen
                """;
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> endpointDto(rs))
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Endpoint not found: " + id));
    }

    public EndpointScoresDto endpointScores(Long endpointId, Integer requestedLimit) {
        endpoint(endpointId);
        int limit = requestedLimit == null ? 20 : requestedLimit;
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be less than or equal to 500");
        }

        List<ThreatScoreDto> history = threatScoreRepository
                .findByTargetTypeAndTargetIdOrderByComputedAtDesc(TargetType.ENDPOINT, endpointId, PageRequest.of(0, limit))
                .stream()
                .map(this::threatScoreDto)
                .toList();
        ThreatScoreDto latest = history.isEmpty() ? null : history.getFirst();
        return new EndpointScoresDto(latest, history);
    }

    public SummaryDto summary() {
        Long totalConnections = longValue(jdbcTemplate.queryForObject("SELECT count(*) FROM connections", Map.of(), Long.class));
        Long distinctProcesses = longValue(jdbcTemplate.queryForObject("SELECT count(*) FROM processes", Map.of(), Long.class));
        Long distinctEndpoints = longValue(jdbcTemplate.queryForObject("SELECT count(*) FROM remote_endpoints", Map.of(), Long.class));
        Double meanScore = jdbcTemplate.queryForObject(
                """
                SELECT avg(score)
                FROM (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) latest
                """,
                Map.of(),
                Double.class
        );
        List<TopProcessDto> topProcesses = jdbcTemplate.query(
                """
                SELECT p.id, p.pid, p.name, max(ls.score) AS max_score
                FROM processes p
                JOIN connections c ON c.process_id = p.id
                JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                GROUP BY p.id, p.pid, p.name
                ORDER BY max(ls.score) DESC, p.name ASC
                LIMIT 5
                """,
                Map.of(),
                (rs, rowNum) -> new TopProcessDto(
                        rs.getObject("id", Long.class),
                        rs.getObject("pid", Integer.class),
                        rs.getString("name"),
                        rs.getObject("max_score", Integer.class)
                )
        );
        return new SummaryDto(totalConnections, distinctProcesses, distinctEndpoints, meanScore, topProcesses);
    }

    public List<ScoreTimelinePoint> scoreTimeline(String window) {
        int hours = parseWindowHours(window);
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(hours);
        return jdbcTemplate.query(
                """
                SELECT date_trunc('minute', computed_at) AS minute, max(score) AS max_score
                FROM threat_scores
                WHERE target_type = 'CONNECTION'
                  AND computed_at >= :since
                GROUP BY date_trunc('minute', computed_at)
                ORDER BY minute ASC
                """,
                Map.of("since", since),
                (rs, rowNum) -> new ScoreTimelinePoint(
                        rs.getObject("minute", OffsetDateTime.class),
                        rs.getObject("max_score", Integer.class)
                )
        );
    }

    private QueryParts connectionFilters(String state, Integer minScore, String processName, String country) {
        List<String> filters = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (state != null && !state.isBlank()) {
            filters.add("c.state = :state");
            params.put("state", state);
        }
        if (minScore != null) {
            filters.add("coalesce(ls.score, 0) >= :minScore");
            params.put("minScore", minScore);
        }
        if (processName != null && !processName.isBlank()) {
            filters.add("lower(p.name) = lower(:processName)");
            params.put("processName", processName);
        }
        if (country != null && !country.isBlank()) {
            filters.add("upper(e.country_iso) = upper(:country)");
            params.put("country", country);
        }
        String where = filters.isEmpty() ? "" : " WHERE " + String.join(" AND ", filters);
        return new QueryParts(where, params);
    }

    private Page<ConnectionDto> connectionsByFilter(
            String filter,
            Map<String, Object> filterParams,
            Integer requestedLimit,
            Integer requestedOffset
    ) {
        Pagination pagination = pagination(requestedLimit, requestedOffset);
        Map<String, Object> params = new HashMap<>(filterParams);
        params.put("limit", pagination.limit());
        params.put("offset", pagination.offset());

        String from = """
                FROM connections c
                LEFT JOIN processes p ON p.id = c.process_id
                LEFT JOIN remote_endpoints e ON e.id = c.endpoint_id
                LEFT JOIN (
                    SELECT DISTINCT ON (target_id) target_id, score
                    FROM threat_scores
                    WHERE target_type = 'CONNECTION'
                    ORDER BY target_id, computed_at DESC
                ) ls ON ls.target_id = c.id
                WHERE %s
                """.formatted(filter);
        String select = """
                SELECT c.id, c.protocol, c.state, host(c.local_ip) AS local_ip, c.local_port,
                       host(e.ip) AS remote_ip, c.remote_port,
                       p.id AS process_id, p.pid, p.name AS process_name, p.path, p.signed, p.signer,
                       e.id AS endpoint_id, e.country_iso, e.country_name, e.asn_org,
                       ls.score AS latest_score,
                       c.first_seen, c.last_seen, c.observation_count
                """;

        List<ConnectionDto> items = jdbcTemplate.query(
                select + from + " ORDER BY c.last_seen DESC LIMIT :limit OFFSET :offset",
                params,
                (rs, rowNum) -> connectionDto(rs)
        );
        int total = count("SELECT count(*) " + from, params);
        return new Page<>(items, total, pagination.limit(), pagination.offset());
    }

    private Pagination pagination(Integer requestedLimit, Integer requestedOffset) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        int offset = requestedOffset == null ? 0 : requestedOffset;
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be less than or equal to 500");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be at least 0");
        }
        return new Pagination(limit, offset);
    }

    private int count(String sql, Map<String, Object> params) {
        Number count = jdbcTemplate.queryForObject(sql, params, Number.class);
        return count == null ? 0 : count.intValue();
    }

    private ConnectionDto connectionDto(ResultSet rs) throws SQLException {
        ProcessSummary process = null;
        Long processId = rs.getObject("process_id", Long.class);
        if (processId != null) {
            process = new ProcessSummary(
                    processId,
                    rs.getObject("pid", Integer.class),
                    rs.getString("process_name"),
                    rs.getString("path"),
                    rs.getObject("signed", Boolean.class),
                    rs.getString("signer")
            );
        }

        EndpointSummary endpoint = null;
        Long endpointId = rs.getObject("endpoint_id", Long.class);
        if (endpointId != null) {
            endpoint = new EndpointSummary(
                    endpointId,
                    rs.getString("remote_ip"),
                    trimToNull(rs.getString("country_iso")),
                    rs.getString("country_name"),
                    rs.getString("asn_org")
            );
        }

        return new ConnectionDto(
                rs.getObject("id", Long.class),
                rs.getString("protocol"),
                rs.getString("state"),
                rs.getString("local_ip"),
                rs.getObject("local_port", Integer.class),
                rs.getString("remote_ip"),
                rs.getObject("remote_port", Integer.class),
                process,
                endpoint,
                rs.getObject("latest_score", Integer.class),
                rs.getObject("first_seen", OffsetDateTime.class),
                rs.getObject("last_seen", OffsetDateTime.class),
                rs.getObject("observation_count", Integer.class)
        );
    }

    private ProcessDto processDto(ResultSet rs) throws SQLException {
        return new ProcessDto(
                rs.getObject("id", Long.class),
                rs.getObject("pid", Integer.class),
                rs.getString("name"),
                rs.getString("path"),
                rs.getObject("signed", Boolean.class),
                rs.getString("signer"),
                rs.getObject("connection_count", Long.class),
                rs.getObject("max_score", Integer.class),
                rs.getObject("first_seen", OffsetDateTime.class),
                rs.getObject("last_seen", OffsetDateTime.class)
        );
    }

    private EndpointDto endpointDto(ResultSet rs) throws SQLException {
        return new EndpointDto(
                rs.getObject("id", Long.class),
                rs.getString("ip"),
                rs.getObject("asn", Integer.class),
                rs.getString("asn_org"),
                trimToNull(rs.getString("country_iso")),
                rs.getString("country_name"),
                rs.getString("city"),
                rs.getObject("latitude", Double.class),
                rs.getObject("longitude", Double.class),
                rs.getString("reverse_dns"),
                rs.getObject("connection_count", Long.class),
                rs.getObject("latest_score", Integer.class),
                rs.getObject("first_seen", OffsetDateTime.class),
                rs.getObject("last_seen", OffsetDateTime.class)
        );
    }

    private ThreatScoreDto threatScoreDto(ThreatScore threatScore) {
        return new ThreatScoreDto(
                threatScore.getScore(),
                threatScore.getReasons(),
                threatScore.getComputedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private Long longValue(Long value) {
        return value == null ? 0L : value;
    }

    private int parseWindowHours(String window) {
        if (window == null || window.isBlank()) {
            return 1;
        }
        String normalized = window.trim().toLowerCase();
        if (!normalized.endsWith("h")) {
            throw new IllegalArgumentException("window must be expressed in hours, for example 1h");
        }
        int hours;
        try {
            hours = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("window must be expressed in hours, for example 1h");
        }
        if (hours < 1 || hours > 24) {
            throw new IllegalArgumentException("window must be between 1h and 24h");
        }
        return hours;
    }

    private record QueryParts(String where, Map<String, Object> params) {
    }

    private record Pagination(int limit, int offset) {
    }
}
