package com.project.optrabidz.security.infrastructure.mapper;

import org.springframework.stereotype.Component;

@Component
public class SecurityPersistenceMapper {
    public com.project.optrabidz.security.infrastructure.entity.Credential toEntity(
            com.project.optrabidz.security.domain.model.Credential credential) {
        com.project.optrabidz.security.infrastructure.entity.Credential entity =
                new com.project.optrabidz.security.infrastructure.entity.Credential();
        entity.setCredentialId(credential.getCredentialId());
        entity.setAccountId(credential.getAccountId());
        entity.setEmail(credential.getEmail());
        entity.setPasswordHash(credential.getPasswordHash());
        entity.setCredentialStatus(credential.getCredentialStatus());
        entity.setCreatedAt(credential.getCreatedAt());
        entity.setPasswordUpdatedAt(credential.getPasswordUpdatedAt());
        return entity;
    }

    public com.project.optrabidz.security.domain.model.Credential toDomain(
            com.project.optrabidz.security.infrastructure.entity.Credential entity) {
        return new com.project.optrabidz.security.domain.model.Credential(
                entity.getCredentialId(),
                entity.getAccountId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCredentialStatus(),
                entity.getCreatedAt(),
                entity.getPasswordUpdatedAt()
        );
    }

    public com.project.optrabidz.security.infrastructure.entity.Session toEntity(
            com.project.optrabidz.security.domain.model.Session session) {
        com.project.optrabidz.security.infrastructure.entity.Session entity =
                new com.project.optrabidz.security.infrastructure.entity.Session();
        entity.setSessionId(session.getSessionId());
        entity.setAccountId(session.getAccountId());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setExpiresAt(session.getExpiresAt());
        entity.setSessionStatus(session.getSessionStatus());
        return entity;
    }

    public com.project.optrabidz.security.domain.model.Session toDomain(
            com.project.optrabidz.security.infrastructure.entity.Session entity) {
        return new com.project.optrabidz.security.domain.model.Session(
                entity.getSessionId(),
                entity.getAccountId(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getSessionStatus()
        );
    }

    public com.project.optrabidz.security.infrastructure.entity.LoginAttempt toEntity(
            com.project.optrabidz.security.domain.model.LoginAttempt loginAttempt) {
        com.project.optrabidz.security.infrastructure.entity.LoginAttempt entity =
                new com.project.optrabidz.security.infrastructure.entity.LoginAttempt();
        entity.setLoginAttemptId(loginAttempt.getLoginAttemptId());
        entity.setEmail(loginAttempt.getEmail());
        entity.setAttemptedAt(loginAttempt.getAttemptedAt());
        entity.setSuccess(loginAttempt.isSuccess());
        entity.setFailureReason(loginAttempt.getFailureReason());
        entity.setSourceIp(loginAttempt.getSourceIp());
        return entity;
    }

    public com.project.optrabidz.security.domain.model.LoginAttempt toDomain(
            com.project.optrabidz.security.infrastructure.entity.LoginAttempt entity) {
        return new com.project.optrabidz.security.domain.model.LoginAttempt(
                entity.getLoginAttemptId(),
                entity.getEmail(),
                entity.getAttemptedAt(),
                entity.isSuccess(),
                entity.getFailureReason(),
                entity.getSourceIp()
        );
    }
}
