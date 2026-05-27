package com.project.optrabidz.participation.application;

import com.project.optrabidz.common.api.response.MessageData;
import com.project.optrabidz.common.event.EventPublisher;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.application.event.ParticipationProfileChangedEvent;
import com.project.optrabidz.participation.application.dto.request.CreateStartupRequest;
import com.project.optrabidz.participation.application.dto.response.StartupResponse;
import com.project.optrabidz.participation.application.exception.InvalidRoleException;
import com.project.optrabidz.participation.application.exception.ParticipationAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.ParticipationNotFoundException;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.model.StartupLegalRegistration;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class StartupService {
    private final StartupRepository startupRepository;
    private final EventPublisher eventPublisher;

    public StartupService(StartupRepository startupRepository,
                          EventPublisher eventPublisher) {
        this.startupRepository = startupRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MessageData createStartup(Long accountId, RoleType roleType, CreateStartupRequest request) {
        ensureRole(roleType, RoleType.STARTUP);

        if (startupRepository.existsByAccountId(accountId)) {
            throw new ParticipationAlreadyExistsException("Startup already exists for this account");
        }

        Startup startup = Startup.establish(
                accountId,
                request.legalEntityName(),
                request.incorporationCountryCode(),
                request.publicDisplayName(),
                request.businessDescription(),
                request.webPresences(),
                request.legalRegistrations() == null ? java.util.List.of() :
                        request.legalRegistrations().stream()
                                .map(registration -> new StartupLegalRegistration(
                                        registration.type(),
                                        registration.value()
                                ))
                                .toList()
        );

        startupRepository.save(startup);
        publishProfileChanged(accountId, roleType);

        return new MessageData("Startup created successfully");
    }

    @Transactional(readOnly = true)
    public StartupResponse getMyStartup(Long accountId, RoleType roleType) {
        ensureRole(roleType, RoleType.STARTUP);

        Startup startup = startupRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found for this account"));

        return toResponse(startup);
    }

    @Transactional
    public MessageData updateStartup(Long accountId, RoleType roleType, CreateStartupRequest request) {
        ensureRole(roleType, RoleType.STARTUP);

        Startup startup = startupRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ParticipationNotFoundException("Startup not found for this account"));

        startup.updateRepresentation(
                request.legalEntityName(),
                request.incorporationCountryCode(),
                request.publicDisplayName(),
                request.businessDescription(),
                request.webPresences(),
                request.legalRegistrations() == null ? java.util.List.of() :
                        request.legalRegistrations().stream()
                                .map(registration -> new StartupLegalRegistration(
                                        registration.type(),
                                        registration.value()
                                ))
                                .toList()
        );

        startupRepository.save(startup);
        publishProfileChanged(accountId, roleType);
        return new MessageData("Startup updated successfully");
    }

    private void publishProfileChanged(Long accountId, RoleType roleType) {
        eventPublisher.publish(new ParticipationProfileChangedEvent(
                accountId,
                roleType,
                Instant.now()
        ));
    }

    private StartupResponse toResponse(Startup startup) {
        return new StartupResponse(
                startup.getStartupId(),
                startup.getLegalEntityName(),
                startup.getIncorporationCountryCode(),
                startup.getPublicDisplayName(),
                startup.getBusinessDescription(),
                startup.getWebPresences(),
                startup.getLegalRegistrations().stream()
                        .map(registration -> new StartupResponse.LegalRegistrationResponse(
                                registration.type(),
                                registration.value()
                        ))
                        .toList()
        );
    }

    private void ensureRole(RoleType actualRole, RoleType expectedRole) {
        if (actualRole != expectedRole) {
            throw new InvalidRoleException("Role is not allowed to perform this operation");
        }
    }
}
