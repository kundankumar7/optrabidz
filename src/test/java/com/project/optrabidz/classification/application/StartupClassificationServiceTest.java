package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.command.AddStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.RemoveStartupClassificationCommand;
import com.project.optrabidz.classification.application.event.StartupClassificationChangedEvent;
import com.project.optrabidz.classification.application.exception.StartupClassificationAlreadyExistsException;
import com.project.optrabidz.classification.application.exception.StartupClassificationNotFoundException;
import com.project.optrabidz.classification.application.exception.StartupClassificationProfileRequiredException;
import com.project.optrabidz.classification.application.port.out.ParticipationActorQueryPort;
import com.project.optrabidz.classification.domain.model.StartupClassification;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import com.project.optrabidz.classification.domain.repository.StartupClassificationRepository;
import com.project.optrabidz.common.event.EventPublisher;
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
class StartupClassificationServiceTest {

    private static final Long ACCOUNT_ID = 41L;
    private static final Long STARTUP_ID = 7L;

    @Mock
    private StartupClassificationRepository startupClassificationRepository;

    @Mock
    private ParticipationActorQueryPort participationActorQueryPort;

    @Mock
    private StartupClassificationRuleEngine startupClassificationRuleEngine;

    @Mock
    private EventPublisher eventPublisher;

    private StartupClassificationService service;

    @BeforeEach
    void setUp() {
        service = new StartupClassificationService(
                startupClassificationRepository,
                participationActorQueryPort,
                startupClassificationRuleEngine,
                eventPublisher
        );
    }

    @Test
    void missingStartupProfileStopsBeforeClassificationWork() {
        when(participationActorQueryPort.findStartupIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addClassification(addCommand()))
                .isInstanceOf(StartupClassificationProfileRequiredException.class);

        verifyNoInteractions(startupClassificationRepository, startupClassificationRuleEngine);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void duplicateClassificationStopsBeforeRuleValidationAndSideEffects() {
        when(participationActorQueryPort.findStartupIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(STARTUP_ID));
        when(startupClassificationRepository.findByStartupId(STARTUP_ID))
                .thenReturn(Optional.of(profileWith("SECTOR", "FINTECH")));

        assertThatThrownBy(() -> service.addClassification(addCommand()))
                .isInstanceOf(StartupClassificationAlreadyExistsException.class);

        verifyNoInteractions(startupClassificationRuleEngine);
        verify(startupClassificationRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void missingClassificationStopsBeforeRuleValidationAndSideEffects() {
        when(participationActorQueryPort.findStartupIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(STARTUP_ID));
        when(startupClassificationRepository.findByStartupId(STARTUP_ID))
                .thenReturn(Optional.of(profileWith("GEOGRAPHY", "INDIA")));

        assertThatThrownBy(() -> service.removeClassification(
                new RemoveStartupClassificationCommand(ACCOUNT_ID, "SECTOR", "FINTECH")
        )).isInstanceOf(StartupClassificationNotFoundException.class);

        verifyNoInteractions(startupClassificationRuleEngine);
        verify(startupClassificationRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void successfulAddPreservesPersistenceEventAndResponse() {
        when(participationActorQueryPort.findStartupIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(STARTUP_ID));
        when(startupClassificationRepository.findByStartupId(STARTUP_ID))
                .thenReturn(Optional.empty());

        var result = service.addClassification(addCommand());

        assertThat(result.message()).isEqualTo("Startup classification added successfully");
        ArgumentCaptor<StartupClassificationProfile> profileCaptor =
                ArgumentCaptor.forClass(StartupClassificationProfile.class);
        verify(startupClassificationRepository).saveAll(profileCaptor.capture());
        assertThat(profileCaptor.getValue().contains("SECTOR", "FINTECH")).isTrue();
        verify(startupClassificationRuleEngine).validateBeforeAdd(
                any(StartupClassificationProfile.class),
                any(StartupClassification.class)
        );
        ArgumentCaptor<StartupClassificationChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StartupClassificationChangedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().startupId()).isEqualTo(STARTUP_ID);
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(ACCOUNT_ID);
    }

    private static AddStartupClassificationCommand addCommand() {
        return new AddStartupClassificationCommand(ACCOUNT_ID, "SECTOR", "FINTECH");
    }

    private static StartupClassificationProfile profileWith(String type, String value) {
        return StartupClassificationProfile.establish(
                STARTUP_ID,
                List.of(StartupClassification.create(type, value))
        );
    }
}
