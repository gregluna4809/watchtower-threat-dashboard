package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.RuleDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleDefinitionRepository extends JpaRepository<RuleDefinition, Long> {

    Optional<RuleDefinition> findByCode(String code);

    List<RuleDefinition> findAllByEnabledTrue();
}

