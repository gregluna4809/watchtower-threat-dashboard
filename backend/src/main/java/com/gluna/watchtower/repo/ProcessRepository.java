package com.gluna.watchtower.repo;

import com.gluna.watchtower.model.ProcessEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessRepository extends JpaRepository<ProcessEntity, Long> {

    Optional<ProcessEntity> findFirstByPidAndNameOrderByFirstSeenDesc(int pid, String name);

    List<ProcessEntity> findTop25ByPathIsNullOrderByLastSeenDesc();
}
