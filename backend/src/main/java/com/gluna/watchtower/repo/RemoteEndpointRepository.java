package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.RemoteEndpoint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RemoteEndpointRepository extends JpaRepository<RemoteEndpoint, Long> {

    @Query(value = "SELECT * FROM remote_endpoints WHERE ip = CAST(:ip AS inet)", nativeQuery = true)
    Optional<RemoteEndpoint> findByIp(String ip);

    List<RemoteEndpoint> findTop50ByCountryIsoIsNullOrderByLastSeenDesc();

    @Query(
            value = """
                    SELECT *
                    FROM remote_endpoints re
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM intel_cache ic
                        WHERE ic.ip = re.ip
                          AND ic.source = 'ABUSEIPDB'
                          AND ic.fetched_at + (ic.ttl_seconds * INTERVAL '1 second') > now()
                    )
                    ORDER BY re.last_seen DESC
                    LIMIT 10
                    """,
            nativeQuery = true
    )
    List<RemoteEndpoint> findTop10NeedingAbuseIpdbCheck();
}
