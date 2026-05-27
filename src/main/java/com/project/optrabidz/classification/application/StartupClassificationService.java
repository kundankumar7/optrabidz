package com.project.optrabidz.classification.application;

import com.project.optrabidz.classification.application.command.AddStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.ClassificationEntryCommand;
import com.project.optrabidz.classification.application.command.RemoveStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.ReplaceStartupClassificationsCommand;
import com.project.optrabidz.classification.application.dto.response.ClassificationEntryResponse;
import com.project.optrabidz.classification.application.dto.response.StartupClassificationResponse;
import com.project.optrabidz.classification.application.event.StartupClassificationChangedEvent;
import com.project.optrabidz.classification.application.exception.ClassificationAlreadyExistsException;
import com.project.optrabidz.classification.application.port.in.StartupClassificationCommandPort;
import com.project.optrabidz.classification.application.port.in.StartupClassificationQueryPort;
import com.project.optrabidz.classification.application.port.out.ParticipationActorQueryPort;
import com.project.optrabidz.classification.domain.model.StartupClassification;
import com.project.optrabidz.classification.domain.model.StartupClassificationProfile;
import com.project.optrabidz.classification.domain.repository.StartupClassificationRepository;
import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.event.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class StartupClassificationService implements StartupClassificationCommandPort, StartupClassificationQueryPort {
    private final StartupClassificationRepository startupClassificationRepository;
    private final ParticipationActorQueryPort participationActorQueryPort;
    private final StartupClassificationRuleEngine startupClassificationRuleEngine;
    private final EventPublisher eventPublisher;

    public StartupClassificationService(StartupClassificationRepository startupClassificationRepository,
                                        ParticipationActorQueryPort participationActorQueryPort,
                                        StartupClassificationRuleEngine startupClassificationRuleEngine,
                                        EventPublisher eventPublisher) {
        this.startupClassificationRepository = startupClassificationRepository;
        this.participationActorQueryPort = participationActorQueryPort;
        this.startupClassificationRuleEngine = startupClassificationRuleEngine;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MessageData addClassification(AddStartupClassificationCommand command) {
        Long startupId = resolveStartupId(command.accountId());
        StartupClassificationProfile profile = loadProfile(startupId);

        if (profile.contains(command.classificationType(), command.classificationValue())) {
            throw new ClassificationAlreadyExistsException("Startup classification already exists");
        }

        StartupClassification entry = StartupClassification.create(
                command.classificationType(),
                command.classificationValue()
        );
        startupClassificationRuleEngine.validateBeforeAdd(profile, entry);

        profile.declare(command.classificationType(), command.classificationValue());
        startupClassificationRepository.saveAll(profile);
        publishChanged(startupId, command.accountId());
        return new MessageData("Startup classification added successfully");
    }

    @Override
    @Transactional
    public MessageData replaceClassifications(ReplaceStartupClassificationsCommand command) {
        Long startupId = resolveStartupId(command.accountId());
        StartupClassificationProfile currentProfile = loadProfile(startupId);
        List<StartupClassification> entries = command.entries().stream()
                .map(this::toStartupClassification)
                .toList();

        startupClassificationRuleEngine.validateBeforeReplace(currentProfile, entries);
        currentProfile.replaceAll(entries);
        startupClassificationRepository.saveAll(currentProfile);
        publishChanged(startupId, command.accountId());
        return new MessageData("Startup classifications replaced successfully");
    }

    @Override
    @Transactional
    public MessageData removeClassification(RemoveStartupClassificationCommand command) {
        Long startupId = resolveStartupId(command.accountId());
        StartupClassificationProfile profile = loadProfile(startupId);
        ensureClassificationExists(profile, command.classificationType(), command.classificationValue());

        startupClassificationRuleEngine.validateBeforeRemove(
                profile,
                command.classificationType(),
                command.classificationValue()
        );
        profile.revoke(command.classificationType(), command.classificationValue());
        startupClassificationRepository.saveAll(profile);
        publishChanged(startupId, command.accountId());
        return new MessageData("Startup classification removed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public StartupClassificationResponse getMyClassifications(Long accountId) {
        return getStartupClassifications(resolveStartupId(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public StartupClassificationResponse getStartupClassifications(Long startupId) {
        StartupClassificationProfile profile = loadProfile(startupId);
        return new StartupClassificationResponse(
                startupId,
                profile.getClassifications().stream()
                        .map(classification -> new ClassificationEntryResponse(
                                classification.getClassificationType(),
                                classification.getClassificationValue()
                        ))
                        .toList()
        );
    }

    private StartupClassification toStartupClassification(ClassificationEntryCommand entry) {
        return StartupClassification.create(entry.type(), entry.value());
    }

    private StartupClassificationProfile loadProfile(Long startupId) {
        return startupClassificationRepository.findByStartupId(startupId)
                .orElse(StartupClassificationProfile.establish(startupId, List.of()));
    }

    private Long resolveStartupId(Long accountId) {
        return participationActorQueryPort.findStartupIdByAccountId(accountId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Startup actor not found for account"
                ));
    }

    private void ensureClassificationExists(StartupClassificationProfile profile,
                                            String classificationType,
                                            String classificationValue) {
        if (!profile.contains(classificationType, classificationValue)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Startup classification not found");
        }
    }

    private void publishChanged(Long startupId, Long accountId) {
        eventPublisher.publish(new StartupClassificationChangedEvent(startupId, accountId, Instant.now()));
    }
}
