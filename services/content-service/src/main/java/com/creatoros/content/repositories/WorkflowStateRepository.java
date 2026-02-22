package com.creatoros.content.repositories;

import com.creatoros.content.entities.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, UUID> {

    Optional<WorkflowState> findByName(String name);
}
