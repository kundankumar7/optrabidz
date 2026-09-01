package com.project.optrabidz.security.application;

import com.project.optrabidz.security.application.command.ProvisionCredentialCommand;
import com.project.optrabidz.security.application.exception.CredentialNotFoundException;
import com.project.optrabidz.security.application.exception.EmailAlreadyRegisteredException;
import com.project.optrabidz.security.application.exception.PasswordPolicyViolationException;
import com.project.optrabidz.security.domain.model.Credential;
import com.project.optrabidz.security.domain.model.CredentialStatus;
import com.project.optrabidz.security.domain.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialProvisioningServiceTest {

    private static final Long ACCOUNT_ID = 41L;

    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private CredentialProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new CredentialProvisioningService(credentialRepository, passwordEncoder);
    }

    @Test
    void creationUsesTypedFailuresAndPreservesNormalizedCredential() {
        assertThatThrownBy(() -> service.createCredential(
                new ProvisionCredentialCommand(ACCOUNT_ID, "member@example.com", "onlyletters")))
                .isInstanceOf(PasswordPolicyViolationException.class);

        when(credentialRepository.existsByEmail("member@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.createCredential(
                new ProvisionCredentialCommand(ACCOUNT_ID, "member@example.com", "Password01")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        when(credentialRepository.existsByEmail("member@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password01")).thenReturn("encoded-password");

        service.createCredential(new ProvisionCredentialCommand(
                ACCOUNT_ID, " MEMBER@EXAMPLE.COM ", "Password01"));

        ArgumentCaptor<Credential> credentialCaptor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getEmail()).isEqualTo("member@example.com");
        assertThat(credentialCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void disableUsesTypedMissingCredentialFailureAndPreservesSuccess() {
        when(credentialRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.disableCredentialForAccount(ACCOUNT_ID))
                .isInstanceOf(CredentialNotFoundException.class);

        Credential credential = new Credential(
                11L, ACCOUNT_ID, "member@example.com", "encoded-password",
                CredentialStatus.ACTIVE, Instant.now(), null);
        when(credentialRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(credential));

        service.disableCredentialForAccount(ACCOUNT_ID);

        assertThat(credential.getCredentialStatus()).isEqualTo(CredentialStatus.DISABLED);
        verify(credentialRepository).save(credential);
    }
}
