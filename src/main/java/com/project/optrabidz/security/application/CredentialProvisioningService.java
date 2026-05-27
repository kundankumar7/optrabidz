package com.project.optrabidz.security.application;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.security.application.command.ProvisionCredentialCommand;
import com.project.optrabidz.security.application.exception.EmailAlreadyExistsException;
import com.project.optrabidz.security.application.port.SecurityCredentialProvisioningPort;
import com.project.optrabidz.security.domain.model.Credential;
import com.project.optrabidz.security.domain.repository.CredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Locale;

@Service
public class CredentialProvisioningService implements SecurityCredentialProvisioningPort {
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    public CredentialProvisioningService(CredentialRepository credentialRepository,
                                         PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void createCredential(ProvisionCredentialCommand command) {
        Assert.notNull(command, "ProvisionCredentialCommand must not be null");
        Assert.notNull(command.accountId(), "accountId must not be null");

        String email = normalizeEmail(command.email());
        validatePasswordPolicy(command.rawPassword());

        if (credentialRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        credentialRepository.save(Credential.register(
                command.accountId(),
                email,
                passwordEncoder.encode(command.rawPassword())
        ));
    }

    @Override
    @Transactional
    public void disableCredentialForAccount(Long accountId) {
        Assert.notNull(accountId, "accountId must not be null");

        Credential credential = credentialRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Credential not found for account"
                ));

        credential.disable();
        credentialRepository.save(credential);
    }

    private void validatePasswordPolicy(String password) {
        Assert.hasText(password, "password must not be blank");

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Password must contain at least one letter and one digit"
            );
        }
    }

    private String normalizeEmail(String email) {
        Assert.hasText(email, "email must not be blank");
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
