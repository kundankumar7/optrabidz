package com.project.optrabidz.participation.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.dto.request.CreateStartupRequest;
import com.project.optrabidz.participation.application.event.ParticipationProfileChangedEvent;
import com.project.optrabidz.participation.application.exception.ParticipationAuthorizationException;
import com.project.optrabidz.participation.application.exception.StartupAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.StartupNotFoundException;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartupServiceTest {

    private static final Long ACCOUNT_ID = 41L;

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private EventPublisher eventPublisher;

    private StartupService service;

    @BeforeEach
    void setUp() {
        service = new StartupService(startupRepository, eventPublisher);
    }

    @Test
    void rejectsWrongRoleBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.getMyStartup(ACCOUNT_ID, RoleType.INVESTOR))
                .isInstanceOf(ParticipationAuthorizationException.class);

        verifyNoInteractions(startupRepository, eventPublisher);
    }

    @Test
    void duplicateStartupUsesStartupSpecificConflict() {
        when(startupRepository.existsByAccountId(ACCOUNT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createStartup(
                ACCOUNT_ID, RoleType.STARTUP, request()))
                .isInstanceOf(StartupAlreadyExistsException.class);

        verify(startupRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void missingStartupUsesStartupSpecificNotFound() {
        when(startupRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyStartup(ACCOUNT_ID, RoleType.STARTUP))
                .isInstanceOf(StartupNotFoundException.class);
    }

    @Test
    void successfulCreationPreservesPersistenceAndProfileEvent() {
        when(startupRepository.existsByAccountId(ACCOUNT_ID)).thenReturn(false);

        var response = service.createStartup(ACCOUNT_ID, RoleType.STARTUP, request());

        assertThat(response.message()).isEqualTo("Startup created successfully");
        ArgumentCaptor<Startup> startupCaptor = ArgumentCaptor.forClass(Startup.class);
        verify(startupRepository).save(startupCaptor.capture());
        assertThat(startupCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT_ID);
        ArgumentCaptor<ParticipationProfileChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationProfileChangedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(eventCaptor.getValue().roleType()).isEqualTo(RoleType.STARTUP);
    }

    private static CreateStartupRequest request() {
        return new CreateStartupRequest(
                "Example Private Limited",
                "IN",
                "Example Startup",
                "Builds useful software",
                List.of("https://startup.example.com"),
                List.of(new CreateStartupRequest.LegalRegistrationRequest("CIN", "U12345"))
        );
    }
}
