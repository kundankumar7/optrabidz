package com.project.optrabidz.security.application;

import com.project.optrabidz.security.application.command.ProvisionCredentialCommand;
import com.project.optrabidz.security.application.exception.CredentialNotFoundException;
import com.project.optrabidz.security.application.exception.EmailAlreadyRegisteredException;
import com.project.optrabidz.security.application.exception.PasswordPolicyViolationException;
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
            throw new EmailAlreadyRegisteredException(email);
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
                .orElseThrow(() -> new CredentialNotFoundException(accountId));

        credential.disable();
        credentialRepository.save(credential);
    }

    private void validatePasswordPolicy(String password) {
        Assert.hasText(password, "password must not be blank");

        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasLetter || !hasDigit) {
            throw new PasswordPolicyViolationException();
        }
    }

    private String normalizeEmail(String email) {
        Assert.hasText(email, "email must not be blank");
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
