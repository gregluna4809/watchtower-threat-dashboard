package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.Connection;
import com.gluna.watchtower.model.Protocol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    @Query("""
            SELECT c
            FROM Connection c
            WHERE ((:processId IS NULL AND c.process IS NULL) OR c.process.id = :processId)
              AND c.localPort = :localPort
              AND ((:endpointId IS NULL AND c.endpoint IS NULL) OR c.endpoint.id = :endpointId)
              AND ((:remotePort IS NULL AND c.remotePort IS NULL) OR c.remotePort = :remotePort)
              AND c.protocol = :protocol
            """)
    Optional<Connection> findByUniqueTuple(
            @Param("processId") Long processId,
            @Param("localPort") int localPort,
            @Param("endpointId") Long endpointId,
            @Param("remotePort") Integer remotePort,
            @Param("protocol") Protocol protocol
    );

    List<Connection> findTop500ByOrderByLastSeenDesc();

    List<Connection> findAllByProcessIdAndEndpointIdOrderByLastSeenAsc(Long processId, Long endpointId);

    @Query("""
            SELECT c
            FROM Connection c
            LEFT JOIN ThreatScore ts
              ON ts.targetType = com.gluna.watchtower.model.TargetType.CONNECTION
             AND ts.targetId = c.id
             AND ts.computedAt = (
                SELECT MAX(ts2.computedAt)
                FROM ThreatScore ts2
                WHERE ts2.targetType = com.gluna.watchtower.model.TargetType.CONNECTION
                  AND ts2.targetId = c.id
             )
            WHERE ts.id IS NULL OR ts.computedAt < c.lastSeen
            ORDER BY c.lastSeen DESC
            LIMIT 100
            """)
    List<Connection> findTop100NeedingScore();
}
