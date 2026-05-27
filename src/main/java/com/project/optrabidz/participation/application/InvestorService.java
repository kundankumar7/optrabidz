package com.project.optrabidz.participation.application;

import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.event.ParticipationProfileChangedEvent;
import com.project.optrabidz.participation.application.dto.request.CreateInvestorRequest;
import com.project.optrabidz.participation.application.dto.response.InvestorResponse;
import com.project.optrabidz.participation.application.exception.InvalidRoleException;
import com.project.optrabidz.participation.application.exception.ParticipationAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.ParticipationNotFoundException;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InvestorService {
    private final InvestorRepository investorRepository;
    private final EventPublisher eventPublisher;

    public InvestorService(InvestorRepository investorRepository,
                           EventPublisher eventPublisher) {
        this.investorRepository = investorRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MessageData createInvestor(Long accountId, RoleType roleType, CreateInvestorRequest request) {
        ensureRole(roleType, RoleType.INVESTOR);

        if (investorRepository.existsByAccountId(accountId)) {
            throw new ParticipationAlreadyExistsException("Investor already exists for this account");
        }

        Investor investor = Investor.establish(
                accountId,
                request.publicDisplayName(),
                request.investorDescription(),
                request.legalEntityName(),
                request.webPresences()
        );

        investorRepository.save(investor);
        publishProfileChanged(accountId, roleType);

        return new MessageData("Investor created successfully");
    }

    @Transactional(readOnly = true)
    public InvestorResponse getMyInvestor(Long accountId, RoleType roleType) {
        ensureRole(roleType, RoleType.INVESTOR);

        Investor investor = investorRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found for this account"));

        return toResponse(investor);
    }

    @Transactional
    public MessageData updateInvestor(Long accountId, RoleType roleType, CreateInvestorRequest request) {
        ensureRole(roleType, RoleType.INVESTOR);

        Investor investor = investorRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Investor not found for this account"));

        investor.updateRepresentation(
                request.publicDisplayName(),
                request.investorDescription(),
                request.legalEntityName(),
                request.webPresences()
        );

        investorRepository.save(investor);
        publishProfileChanged(accountId, roleType);
        return new MessageData("Investor updated successfully");
    }

    private void publishProfileChanged(Long accountId, RoleType roleType) {
        eventPublisher.publish(new ParticipationProfileChangedEvent(
                accountId,
                roleType,
                Instant.now()
        ));
    }

    private InvestorResponse toResponse(Investor investor) {
        return new InvestorResponse(
                investor.getInvestorId(),
                investor.getPublicDisplayName(),
                investor.getInvestorDescription(),
                investor.getLegalEntityName(),
                investor.getWebPresences()
        );
    }

    private void ensureRole(RoleType actualRole, RoleType expectedRole) {
        if (actualRole != expectedRole) {
            throw new InvalidRoleException("Role is not allowed to perform this operation");
        }
    }
}
