package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.HoneypotHit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoneypotHitRepository extends JpaRepository<HoneypotHit, Long> {
}
