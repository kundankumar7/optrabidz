package com.project.optrabidz.marketplace.application;

import com.project.optrabidz.common.api.pagination.PageResponse;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.dto.response.AgreementResponse;
import com.project.optrabidz.marketplace.application.exception.AgreementNotFoundException;
import com.project.optrabidz.marketplace.application.specification.AgreementVisibleToActorSpec;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import com.project.optrabidz.marketplace.domain.repository.AgreementRepository;
import com.project.optrabidz.participation.application.exception.InvalidRoleException;
import com.project.optrabidz.participation.application.exception.ParticipationNotFoundException;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgreementService {
    private final AgreementRepository agreementRepository;
    private final StartupRepository startupRepository;
    private final InvestorRepository investorRepository;
    private final MarketplaceResponseMapper responseMapper;
    private final AgreementVisibleToActorSpec agreementVisibleToActorSpec;

    public AgreementService(AgreementRepository agreementRepository,
                            StartupRepository startupRepository,
                            InvestorRepository investorRepository,
                            MarketplaceResponseMapper responseMapper,
                            AgreementVisibleToActorSpec agreementVisibleToActorSpec) {
        this.agreementRepository = agreementRepository;
        this.startupRepository = startupRepository;
        this.investorRepository = investorRepository;
        this.responseMapper = responseMapper;
        this.agreementVisibleToActorSpec = agreementVisibleToActorSpec;
    }

    @Transactional(readOnly = true)
    public AgreementResponse getAgreementById(Long accountId, RoleType roleType, Long agreementId) {
        Agreement agreement = getAgreement(agreementId);
        ensureAgreementVisible(accountId, roleType, agreement);
        return toResponse(agreement);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgreementResponse> getMyStartupAgreements(Long accountId,
                                                                  RoleType roleType,
                                                                  int page,
                                                                  int size) {
        ensureRole(roleType, RoleType.STARTUP);
        Startup startup = getStartupByAccount(accountId);
        Pageable pageable = PageRequest.of(toPageIndex(page), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AgreementResponse> agreements = agreementRepository.findByStartupId(startup.getStartupId(), pageable)
                .map(this::toResponse);
        return toPageResponse(agreements, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgreementResponse> getMyInvestorAgreements(Long accountId,
                                                                   RoleType roleType,
                                                                   int page,
                                                                   int size) {
        ensureRole(roleType, RoleType.INVESTOR);
        Investor investor = getInvestorByAccount(accountId);
        Pageable pageable = PageRequest.of(toPageIndex(page), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AgreementResponse> agreements = agreementRepository.findByInvestorId(investor.getInvestorId(), pageable)
                .map(this::toResponse);
        return toPageResponse(agreements, page, size);
    }

    private Agreement getAgreement(Long agreementId) {
        return agreementRepository.findById(agreementId)
                .orElseThrow(() -> new AgreementNotFoundException("Agreement not found"));
    }

    private void ensureAgreementVisible(Long accountId, RoleType roleType, Agreement agreement) {
        if (roleType == RoleType.STARTUP) {
            Startup startup = getStartupByAccount(accountId);
            agreementVisibleToActorSpec.assertSatisfiedBy(roleType, agreement, startup.getStartupId(), null);
            return;
        }
        if (roleType == RoleType.INVESTOR) {
            Investor investor = getInvestorByAccount(accountId);
            agreementVisibleToActorSpec.assertSatisfiedBy(roleType, agreement, null, investor.getInvestorId());
            return;
        }
        agreementVisibleToActorSpec.assertSatisfiedBy(roleType, agreement, null, null);
    }

    private AgreementResponse toResponse(Agreement agreement) {
        Startup startup = startupRepository.findById(agreement.getStartupId())
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found"));
        Investor investor = investorRepository.findById(agreement.getInvestorId())
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found"));
        return responseMapper.toAgreementResponse(
                agreement,
                startup.getPublicDisplayName(),
                investor.getPublicDisplayName()
        );
    }

    private Startup getStartupByAccount(Long accountId) {
        return startupRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found for this account"));
    }

    private Investor getInvestorByAccount(Long accountId) {
        return investorRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found for this account"));
    }

    private void ensureRole(RoleType actualRole, RoleType expectedRole) {
        if (actualRole != expectedRole) {
            throw new InvalidRoleException("Role is not allowed to perform this operation");
        }
    }

    private int toPageIndex(int page) {
        return Math.max(page, 1) - 1;
    }

    private PageResponse<AgreementResponse> toPageResponse(Page<AgreementResponse> pageData, int page, int size) {
        return new PageResponse<>(
                pageData.getContent(),
                Math.max(page, 1),
                size,
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
