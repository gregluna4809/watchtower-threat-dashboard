package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.RemoteEndpoint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(
            value = """
                    SELECT *
                    FROM remote_endpoints
                    WHERE reverse_dns IS NULL
                      AND NOT (
                          ip <<= inet '0.0.0.0/8'
                          OR ip <<= inet '10.0.0.0/8'
                          OR ip <<= inet '100.64.0.0/10'
                          OR ip <<= inet '127.0.0.0/8'
                          OR ip <<= inet '169.254.0.0/16'
                          OR ip <<= inet '172.16.0.0/12'
                          OR ip <<= inet '192.168.0.0/16'
                          OR ip <<= inet '224.0.0.0/4'
                          OR ip <<= inet '::/128'
                          OR ip <<= inet '::1/128'
                          OR ip <<= inet 'fc00::/7'
                          OR ip <<= inet 'fe80::/10'
                          OR ip <<= inet 'ff00::/8'
                      )
                    ORDER BY last_seen DESC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<RemoteEndpoint> findTopNeedingReverseDns(@Param("limit") int limit);
}
