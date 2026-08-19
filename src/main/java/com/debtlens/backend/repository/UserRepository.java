package com.debtlens.backend.repository;

import com.debtlens.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByAuth0UserId(String auth0UserId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByAuth0UserId(String auth0UserId);
}