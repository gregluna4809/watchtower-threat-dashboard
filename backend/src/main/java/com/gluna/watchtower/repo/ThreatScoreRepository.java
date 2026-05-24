package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.TargetType;
import com.gluna.watchtower.model.ThreatScore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ThreatScoreRepository extends JpaRepository<ThreatScore, Long> {

    Optional<ThreatScore> findFirstByTargetTypeAndTargetIdOrderByComputedAtDesc(TargetType type, Long id);

    @Query("""
            SELECT latest.connectionProcessId, MAX(latest.score)
            FROM (
                SELECT c.process.id AS connectionProcessId, ts.score AS score
                FROM Connection c
                JOIN ThreatScore ts
                  ON ts.targetType = com.gluna.watchtower.model.TargetType.CONNECTION
                 AND ts.targetId = c.id
                 AND ts.computedAt = (
                    SELECT MAX(ts2.computedAt)
                    FROM ThreatScore ts2
                    WHERE ts2.targetType = com.gluna.watchtower.model.TargetType.CONNECTION
                      AND ts2.targetId = c.id
                 )
                WHERE c.process IS NOT NULL
            ) latest
            GROUP BY latest.connectionProcessId
            """)
    List<Object[]> findLatestConnectionScoreMaxByProcess();
}
