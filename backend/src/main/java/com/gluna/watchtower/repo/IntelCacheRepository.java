package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.IntelCache;
import com.gluna.watchtower.model.IntelSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntelCacheRepository extends JpaRepository<IntelCache, Long> {

    @Query(
            value = "SELECT * FROM intel_cache WHERE ip = CAST(:ip AS inet) AND source = CAST(:source AS varchar)",
            nativeQuery = true
    )
    Optional<IntelCache> findByIpAndSource(@Param("ip") String ip, @Param("source") IntelSource source);
}
