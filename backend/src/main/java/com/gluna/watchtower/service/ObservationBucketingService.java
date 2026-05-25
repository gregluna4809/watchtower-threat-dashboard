package com.gluna.watchtower.service;

import com.gluna.watchtower.api.dto.ObservationBucket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ObservationBucketingService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ObservationBucketingService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ObservationBucket> bucket(String targetType, Long targetId, Duration window, Duration bucketSize) {
        String normalizedTargetType = targetType == null ? "" : targetType.trim().toUpperCase();
        String targetFilter = switch (normalizedTargetType) {
            case "ENDPOINT" -> "c.endpoint_id = :targetId";
            case "PROCESS" -> "c.process_id = :targetId";
            case "CONNECTION" -> "c.id = :targetId";
            default -> throw new IllegalArgumentException("targetType must be ENDPOINT, PROCESS, or CONNECTION");
        };

        long bucketSeconds = bucketSize.toSeconds();
        if (bucketSeconds < 1) {
            throw new IllegalArgumentException("bucketSize must be at least one second");
        }

        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minus(window);
        Map<String, Object> params = new HashMap<>();
        params.put("targetId", targetId);
        params.put("since", since);
        params.put("bucketSeconds", bucketSeconds);

        String bucketExpression = bucketExpression(bucketSeconds);
        String sql = """
                SELECT bucket_start,
                       bucket_start + (:bucketSeconds * INTERVAL '1 second') AS bucket_end,
                       count(*) AS observation_count
                FROM (
                    SELECT %s AS bucket_start
                    FROM connections c
                    WHERE %s
                      AND c.last_seen >= :since
                ) bucketed
                GROUP BY bucket_start
                ORDER BY bucket_start ASC
                """.formatted(bucketExpression, targetFilter);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> observationBucket(rs));
    }

    private String bucketExpression(long bucketSeconds) {
        if (bucketSeconds == Duration.ofHours(1).toSeconds()) {
            return "date_trunc('hour', c.last_seen)";
        }
        if (bucketSeconds == Duration.ofDays(1).toSeconds()) {
            return "date_trunc('day', c.last_seen)";
        }
        return "to_timestamp(floor(extract(epoch from c.last_seen) / :bucketSeconds) * :bucketSeconds)";
    }

    private ObservationBucket observationBucket(ResultSet rs) throws SQLException {
        return new ObservationBucket(
                rs.getObject("bucket_start", OffsetDateTime.class),
                rs.getObject("bucket_end", OffsetDateTime.class),
                rs.getLong("observation_count")
        );
    }
}
