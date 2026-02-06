package com.creatoros.auth.repository;

import java.util.Optional;

import com.creatoros.auth.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(String id);
}
