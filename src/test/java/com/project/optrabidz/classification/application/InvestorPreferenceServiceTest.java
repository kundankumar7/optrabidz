package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.command.AddInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.RemoveInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.event.InvestorPreferenceChangedEvent;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceAlreadyExistsException;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceNotFoundException;
import com.project.optrabidz.classification.application.exception.InvestorPreferenceProfileRequiredException;
import com.project.optrabidz.classification.application.port.out.ParticipationActorQueryPort;
import com.project.optrabidz.classification.domain.model.InvestorPreference;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import com.project.optrabidz.classification.domain.repository.InvestorPreferenceRepository;
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
class InvestorPreferenceServiceTest {

    private static final Long ACCOUNT_ID = 41L;
    private static final Long INVESTOR_ID = 8L;

    @Mock
    private InvestorPreferenceRepository investorPreferenceRepository;

    @Mock
    private ParticipationActorQueryPort participationActorQueryPort;

    @Mock
    private InvestorPreferenceRuleEngine investorPreferenceRuleEngine;

    @Mock
    private EventPublisher eventPublisher;

    private InvestorPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new InvestorPreferenceService(
                investorPreferenceRepository,
                participationActorQueryPort,
                investorPreferenceRuleEngine,
                eventPublisher
        );
    }

    @Test
    void missingInvestorProfileStopsBeforePreferenceWork() {
        when(participationActorQueryPort.findInvestorIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addPreference(addCommand()))
                .isInstanceOf(InvestorPreferenceProfileRequiredException.class);

        verifyNoInteractions(investorPreferenceRepository, investorPreferenceRuleEngine);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void duplicatePreferenceStopsBeforeRuleValidationAndSideEffects() {
        when(participationActorQueryPort.findInvestorIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(INVESTOR_ID));
        when(investorPreferenceRepository.findByInvestorId(INVESTOR_ID))
                .thenReturn(Optional.of(profileWith("SECTOR", "FINTECH")));

        assertThatThrownBy(() -> service.addPreference(addCommand()))
                .isInstanceOf(InvestorPreferenceAlreadyExistsException.class);

        verifyNoInteractions(investorPreferenceRuleEngine);
        verify(investorPreferenceRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void missingPreferenceStopsBeforeRuleValidationAndSideEffects() {
        when(participationActorQueryPort.findInvestorIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(INVESTOR_ID));
        when(investorPreferenceRepository.findByInvestorId(INVESTOR_ID))
                .thenReturn(Optional.of(profileWith("GEOGRAPHY", "INDIA")));

        assertThatThrownBy(() -> service.removePreference(
                new RemoveInvestorPreferenceCommand(ACCOUNT_ID, "SECTOR", "FINTECH")
        )).isInstanceOf(InvestorPreferenceNotFoundException.class);

        verifyNoInteractions(investorPreferenceRuleEngine);
        verify(investorPreferenceRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void successfulAddPreservesPersistenceEventAndResponse() {
        when(participationActorQueryPort.findInvestorIdByAccountId(ACCOUNT_ID))
                .thenReturn(Optional.of(INVESTOR_ID));
        when(investorPreferenceRepository.findByInvestorId(INVESTOR_ID))
                .thenReturn(Optional.empty());

        var result = service.addPreference(addCommand());

        assertThat(result.message()).isEqualTo("Investor preference added successfully");
        ArgumentCaptor<InvestorPreferenceProfile> profileCaptor =
                ArgumentCaptor.forClass(InvestorPreferenceProfile.class);
        verify(investorPreferenceRepository).saveAll(profileCaptor.capture());
        assertThat(profileCaptor.getValue().contains("SECTOR", "FINTECH")).isTrue();
        verify(investorPreferenceRuleEngine).validateBeforeAdd(
                any(InvestorPreferenceProfile.class),
                any(InvestorPreference.class)
        );
        ArgumentCaptor<InvestorPreferenceChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(InvestorPreferenceChangedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().investorId()).isEqualTo(INVESTOR_ID);
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(ACCOUNT_ID);
    }

    private static AddInvestorPreferenceCommand addCommand() {
        return new AddInvestorPreferenceCommand(ACCOUNT_ID, "SECTOR", "FINTECH");
    }

    private static InvestorPreferenceProfile profileWith(String type, String value) {
        return InvestorPreferenceProfile.establish(
                INVESTOR_ID,
                List.of(InvestorPreference.create(type, value))
        );
    }
}
