package com.project.optrabidz.security.domain.repository;

import com.project.optrabidz.security.domain.model.LoginAttempt;

import java.util.List;

public interface LoginAttemptRepository {
    LoginAttempt save(LoginAttempt loginAttempt);

    List<LoginAttempt> findRecentByEmail(String email, int limit);
}
