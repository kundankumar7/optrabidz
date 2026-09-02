package com.project.optrabidz.governance.application.admin;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AdminPrivilegedConfigurationPolicy implements SmartInitializingSingleton {
    private static final int MAX_IDENTITY_LENGTH = 255;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int MIN_RECOVERY_TOKEN_BYTES = 32;
    private static final int MAX_RECOVERY_TOKEN_BYTES = 512;

    private final AdminBootstrapProperties bootstrap;
    private final AdminRecoveryProperties recovery;

    public AdminPrivilegedConfigurationPolicy(AdminBootstrapProperties bootstrap,
                                              AdminRecoveryProperties recovery) {
        this.bootstrap = bootstrap;
        this.recovery = recovery;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (bootstrap.isEnabled() && recovery.isEnabled()) {
            throw invalid("Admin bootstrap and recovery cannot be enabled together");
        }
        if (bootstrap.isEnabled()) {
            validateBootstrap();
        }
        if (recovery.isEnabled()) {
            validateRecovery();
        }
    }

    private void validateBootstrap() {
        if (!validEmail(bootstrap.getEmail())) {
            throw invalid("Admin bootstrap email configuration is invalid");
        }
        if (!validPassword(bootstrap.getPassword())) {
            throw invalid("Admin bootstrap password configuration is invalid");
        }
        if (!validIdentity(bootstrap.getPublicDisplayName())) {
            throw invalid("Admin bootstrap display-name configuration is invalid");
        }
        if (!validIdentity(bootstrap.getOrganizationLabel())) {
            throw invalid("Admin bootstrap organization configuration is invalid");
        }
    }

    private void validateRecovery() {
        String token = recovery.getToken();
        int byteLength = token == null ? 0 : token.getBytes(StandardCharsets.UTF_8).length;
        if (token == null || token.isBlank()
                || byteLength < MIN_RECOVERY_TOKEN_BYTES
                || byteLength > MAX_RECOVERY_TOKEN_BYTES) {
            throw invalid("Admin recovery token configuration is invalid");
        }
    }

    private static boolean validEmail(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > MAX_IDENTITY_LENGTH) {
            return false;
        }
        try {
            InternetAddress address = new InternetAddress(candidate, true);
            address.validate();
            return candidate.equals(address.getAddress());
        } catch (AddressException ignored) {
            return false;
        }
    }

    private static boolean validPassword(String candidate) {
        if (candidate == null
                || candidate.length() < MIN_PASSWORD_LENGTH
                || candidate.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLetter = candidate.chars().anyMatch(Character::isLetter);
        boolean hasDigit = candidate.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }

    private static boolean validIdentity(String candidate) {
        return candidate != null && !candidate.isBlank() && candidate.length() <= MAX_IDENTITY_LENGTH;
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException(message);
    }
}
