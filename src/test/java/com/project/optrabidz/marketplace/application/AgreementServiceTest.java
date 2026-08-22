package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.error.ApplicationException;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.error.MarketplaceErrors;
import com.project.optrabidz.marketplace.application.exception.AgreementNotFoundException;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.application.specification.AgreementVisibleToActorSpec;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgreementServiceTest {
    private static final Long ACCOUNT_ID = 101L;
    private static final Long AGREEMENT_ID = 701L;

    @Mock
    private AgreementRepository agreementRepository;
    @Mock
    private StartupRepository startupRepository;
    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private MarketplaceResponseMapper responseMapper;

    private AgreementService service;

    @BeforeEach
    void setUp() {
        service = new AgreementService(
                agreementRepository,
                startupRepository,
                investorRepository,
                responseMapper,
                new AgreementVisibleToActorSpec()
        );
    }

    @Test
    void missingAgreementUsesApprovedNotFoundDescriptor() {
        when(agreementRepository.findById(AGREEMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAgreementById(
                ACCOUNT_ID,
                RoleType.ADMIN,
                AGREEMENT_ID
        ))
                .isInstanceOf(AgreementNotFoundException.class)
                .satisfies(failure -> assertThat(
                                ((ApplicationException) failure).descriptor())
                        .isSameAs(MarketplaceErrors.AGREEMENT_NOT_FOUND));
    }

    @Test
    void startupAgreementQueryRejectsWrongRoleWithApprovedDescriptor() {
        assertThatThrownBy(() -> service.getMyStartupAgreements(
                ACCOUNT_ID,
                RoleType.INVESTOR,
                1,
                20
        ))
                .isInstanceOf(MarketplaceAccessException.class)
                .satisfies(failure -> assertThat(
                                ((ApplicationException) failure).descriptor())
                        .isSameAs(MarketplaceErrors.MARKETPLACE_ACCESS_DENIED));
    }
}
