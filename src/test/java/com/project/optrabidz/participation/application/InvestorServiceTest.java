package com.project.optrabidz.participation.application;

import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.dto.request.CreateInvestorRequest;
import com.project.optrabidz.participation.application.event.ParticipationProfileChangedEvent;
import com.project.optrabidz.participation.application.exception.InvestorAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.InvestorNotFoundException;
import com.project.optrabidz.participation.application.exception.ParticipationAuthorizationException;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
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
class InvestorServiceTest {

    private static final Long ACCOUNT_ID = 42L;

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private EventPublisher eventPublisher;

    private InvestorService service;

    @BeforeEach
    void setUp() {
        service = new InvestorService(investorRepository, eventPublisher);
    }

    @Test
    void rejectsWrongRoleBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.getMyInvestor(ACCOUNT_ID, RoleType.STARTUP))
                .isInstanceOf(ParticipationAuthorizationException.class);

        verifyNoInteractions(investorRepository, eventPublisher);
    }

    @Test
    void duplicateInvestorUsesInvestorSpecificConflict() {
        when(investorRepository.existsByAccountId(ACCOUNT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createInvestor(
                ACCOUNT_ID, RoleType.INVESTOR, request()))
                .isInstanceOf(InvestorAlreadyExistsException.class);

        verify(investorRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void missingInvestorUsesInvestorSpecificNotFound() {
        when(investorRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyInvestor(ACCOUNT_ID, RoleType.INVESTOR))
                .isInstanceOf(InvestorNotFoundException.class);
    }

    @Test
    void successfulCreationPreservesPersistenceAndProfileEvent() {
        when(investorRepository.existsByAccountId(ACCOUNT_ID)).thenReturn(false);

        var response = service.createInvestor(ACCOUNT_ID, RoleType.INVESTOR, request());

        assertThat(response.message()).isEqualTo("Investor created successfully");
        ArgumentCaptor<Investor> investorCaptor = ArgumentCaptor.forClass(Investor.class);
        verify(investorRepository).save(investorCaptor.capture());
        assertThat(investorCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT_ID);
        ArgumentCaptor<ParticipationProfileChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(ParticipationProfileChangedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(eventCaptor.getValue().roleType()).isEqualTo(RoleType.INVESTOR);
    }

    private static CreateInvestorRequest request() {
        return new CreateInvestorRequest(
                "Example Investor",
                "Invests in early-stage companies",
                "Example Capital LLP",
                List.of("https://investor.example.com")
        );
    }
}
