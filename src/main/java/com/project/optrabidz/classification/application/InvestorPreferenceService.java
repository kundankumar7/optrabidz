package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.command.AddInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.ClassificationEntryCommand;
import com.project.optrabidz.classification.application.command.RemoveInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.ReplaceInvestorPreferencesCommand;
import com.project.optrabidz.classification.application.dto.response.ClassificationEntryResponse;
import com.project.optrabidz.classification.application.dto.response.InvestorPreferenceResponse;
import com.project.optrabidz.classification.application.event.InvestorPreferenceChangedEvent;
import com.project.optrabidz.classification.application.exception.ClassificationAlreadyExistsException;
import com.project.optrabidz.classification.application.port.in.InvestorPreferenceCommandPort;
import com.project.optrabidz.classification.application.port.in.InvestorPreferenceQueryPort;
import com.project.optrabidz.classification.application.port.out.ParticipationActorQueryPort;
import com.project.optrabidz.classification.domain.model.InvestorPreference;
import com.project.optrabidz.classification.domain.model.InvestorPreferenceProfile;
import com.project.optrabidz.classification.domain.repository.InvestorPreferenceRepository;
import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.event.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class InvestorPreferenceService implements InvestorPreferenceCommandPort, InvestorPreferenceQueryPort {
    private final InvestorPreferenceRepository investorPreferenceRepository;
    private final ParticipationActorQueryPort participationActorQueryPort;
    private final InvestorPreferenceRuleEngine investorPreferenceRuleEngine;
    private final EventPublisher eventPublisher;

    public InvestorPreferenceService(InvestorPreferenceRepository investorPreferenceRepository,
                                     ParticipationActorQueryPort participationActorQueryPort,
                                     InvestorPreferenceRuleEngine investorPreferenceRuleEngine,
                                     EventPublisher eventPublisher) {
        this.investorPreferenceRepository = investorPreferenceRepository;
        this.participationActorQueryPort = participationActorQueryPort;
        this.investorPreferenceRuleEngine = investorPreferenceRuleEngine;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MessageData addPreference(AddInvestorPreferenceCommand command) {
        Long investorId = resolveInvestorId(command.accountId());
        InvestorPreferenceProfile profile = loadProfile(investorId);

        if (profile.contains(command.preferenceType(), command.preferenceValue())) {
            throw new ClassificationAlreadyExistsException("Investor preference already exists");
        }

        InvestorPreference entry = InvestorPreference.create(command.preferenceType(), command.preferenceValue());
        investorPreferenceRuleEngine.validateBeforeAdd(profile, entry);

        profile.declare(command.preferenceType(), command.preferenceValue());
        investorPreferenceRepository.saveAll(profile);
        publishChanged(investorId, command.accountId());
        return new MessageData("Investor preference added successfully");
    }

    @Override
    @Transactional
    public MessageData replacePreferences(ReplaceInvestorPreferencesCommand command) {
        Long investorId = resolveInvestorId(command.accountId());
        InvestorPreferenceProfile currentProfile = loadProfile(investorId);
        List<InvestorPreference> entries = command.entries().stream()
                .map(this::toInvestorPreference)
                .toList();

        investorPreferenceRuleEngine.validateBeforeReplace(currentProfile, entries);
        currentProfile.replaceAll(entries);
        investorPreferenceRepository.saveAll(currentProfile);
        publishChanged(investorId, command.accountId());
        return new MessageData("Investor preferences replaced successfully");
    }

    @Override
    @Transactional
    public MessageData removePreference(RemoveInvestorPreferenceCommand command) {
        Long investorId = resolveInvestorId(command.accountId());
        InvestorPreferenceProfile profile = loadProfile(investorId);
        ensurePreferenceExists(profile, command.preferenceType(), command.preferenceValue());

        investorPreferenceRuleEngine.validateBeforeRemove(
                profile,
                command.preferenceType(),
                command.preferenceValue()
        );
        profile.revoke(command.preferenceType(), command.preferenceValue());
        investorPreferenceRepository.saveAll(profile);
        publishChanged(investorId, command.accountId());
        return new MessageData("Investor preference removed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public InvestorPreferenceResponse getMyPreferences(Long accountId) {
        return getInvestorPreferences(resolveInvestorId(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public InvestorPreferenceResponse getInvestorPreferences(Long investorId) {
        InvestorPreferenceProfile profile = loadProfile(investorId);
        return new InvestorPreferenceResponse(
                investorId,
                profile.getPreferences().stream()
                        .map(preference -> new ClassificationEntryResponse(
                                preference.getPreferenceType(),
                                preference.getPreferenceValue()
                        ))
                        .toList()
        );
    }

    private InvestorPreference toInvestorPreference(ClassificationEntryCommand entry) {
        return InvestorPreference.create(entry.type(), entry.value());
    }

    private InvestorPreferenceProfile loadProfile(Long investorId) {
        return investorPreferenceRepository.findByInvestorId(investorId)
                .orElse(InvestorPreferenceProfile.establish(investorId, List.of()));
    }

    private Long resolveInvestorId(Long accountId) {
        return participationActorQueryPort.findInvestorIdByAccountId(accountId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Investor actor not found for account"
                ));
    }

    private void ensurePreferenceExists(InvestorPreferenceProfile profile,
                                        String preferenceType,
                                        String preferenceValue) {
        if (!profile.contains(preferenceType, preferenceValue)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Investor preference not found");
        }
    }

    private void publishChanged(Long investorId, Long accountId) {
        eventPublisher.publish(new InvestorPreferenceChangedEvent(investorId, accountId, Instant.now()));
    }
}
