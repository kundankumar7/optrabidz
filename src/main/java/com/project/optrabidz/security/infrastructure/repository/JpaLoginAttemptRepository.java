package com.project.optrabidz.security.infrastructure.repository;

import com.project.optrabidz.security.infrastructure.entity.LoginAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaLoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    List<LoginAttempt> findByEmailIgnoreCaseOrderByAttemptedAtDesc(String email, Pageable pageable);
}
