package com.project.optrabidz.participation.infrastructure.mapper;

import com.project.optrabidz.participation.domain.model.Admin;
import com.project.optrabidz.participation.domain.model.Investor;
import com.project.optrabidz.participation.domain.model.Startup;
import com.project.optrabidz.participation.domain.model.StartupLegalRegistration;
import com.project.optrabidz.participation.infrastructure.entity.AdminEntity;
import com.project.optrabidz.participation.infrastructure.entity.InvestorEntity;
import com.project.optrabidz.participation.infrastructure.entity.InvestorWebPresenceEntity;
import com.project.optrabidz.participation.infrastructure.entity.StartupEntity;
import com.project.optrabidz.participation.infrastructure.entity.StartupLegalRegistrationEntity;
import com.project.optrabidz.participation.infrastructure.entity.StartupWebPresenceEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ParticipationPersistenceMapper {
    public StartupEntity toEntity(Startup startup) {
        StartupEntity entity = new StartupEntity();
        entity.setStartupId(startup.getStartupId());
        entity.setAccountId(startup.getAccountId());
        entity.setLegalEntityName(startup.getLegalEntityName());
        entity.setIncorporationCountryCode(startup.getIncorporationCountryCode());
        entity.setPublicDisplayName(startup.getPublicDisplayName());
        entity.setBusinessDescription(startup.getBusinessDescription());

        List<StartupWebPresenceEntity> webPresences = new ArrayList<>();
        for (String url : startup.getWebPresences()) {
            StartupWebPresenceEntity webPresenceEntity = new StartupWebPresenceEntity();
            webPresenceEntity.setStartup(entity);
            webPresenceEntity.setUrl(url);
            webPresences.add(webPresenceEntity);
        }
        entity.setWebPresences(webPresences);

        List<StartupLegalRegistrationEntity> legalRegistrations = new ArrayList<>();
        for (StartupLegalRegistration registration : startup.getLegalRegistrations()) {
            StartupLegalRegistrationEntity registrationEntity = new StartupLegalRegistrationEntity();
            registrationEntity.setStartup(entity);
            registrationEntity.setRegistrationType(registration.type());
            registrationEntity.setRegistrationValue(registration.value());
            legalRegistrations.add(registrationEntity);
        }
        entity.setLegalRegistrations(legalRegistrations);

        return entity;
    }

    public Startup toDomain(StartupEntity entity) {
        return new Startup(
                entity.getStartupId(),
                entity.getAccountId(),
                entity.getLegalEntityName(),
                entity.getIncorporationCountryCode(),
                entity.getPublicDisplayName(),
                entity.getBusinessDescription(),
                entity.getWebPresences().stream().map(StartupWebPresenceEntity::getUrl).toList(),
                entity.getLegalRegistrations().stream()
                        .map(registration -> new StartupLegalRegistration(
                                registration.getRegistrationType(),
                                registration.getRegistrationValue()
                        ))
                        .toList()
        );
    }

    public InvestorEntity toEntity(Investor investor) {
        InvestorEntity entity = new InvestorEntity();
        entity.setInvestorId(investor.getInvestorId());
        entity.setAccountId(investor.getAccountId());
        entity.setPublicDisplayName(investor.getPublicDisplayName());
        entity.setInvestorDescription(investor.getInvestorDescription());
        entity.setLegalEntityName(investor.getLegalEntityName());

        List<InvestorWebPresenceEntity> webPresences = new ArrayList<>();
        for (String url : investor.getWebPresences()) {
            InvestorWebPresenceEntity webPresenceEntity = new InvestorWebPresenceEntity();
            webPresenceEntity.setInvestor(entity);
            webPresenceEntity.setUrl(url);
            webPresences.add(webPresenceEntity);
        }
        entity.setWebPresences(webPresences);

        return entity;
    }

    public Investor toDomain(InvestorEntity entity) {
        return new Investor(
                entity.getInvestorId(),
                entity.getAccountId(),
                entity.getPublicDisplayName(),
                entity.getInvestorDescription(),
                entity.getLegalEntityName(),
                entity.getWebPresences().stream().map(InvestorWebPresenceEntity::getUrl).toList()
        );
    }

    public AdminEntity toEntity(Admin admin) {
        AdminEntity entity = new AdminEntity();
        entity.setAdminId(admin.getAdminId());
        entity.setAccountId(admin.getAccountId());
        entity.setPublicDisplayName(admin.getPublicDisplayName());
        entity.setOrganizationLabel(admin.getOrganizationLabel());
        entity.setAdminState(admin.getAdminState());
        entity.setGrantedAt(admin.getGrantedAt());
        entity.setRevokedAt(admin.getRevokedAt());
        entity.setRevokedByAccountId(admin.getRevokedByAccountId());
        entity.setRevokedReason(admin.getRevokedReason());
        return entity;
    }

    public Admin toDomain(AdminEntity entity) {
        return new Admin(
                entity.getAdminId(),
                entity.getAccountId(),
                entity.getPublicDisplayName(),
                entity.getOrganizationLabel(),
                entity.getAdminState(),
                entity.getGrantedAt(),
                entity.getRevokedAt(),
                entity.getRevokedByAccountId(),
                entity.getRevokedReason()
        );
    }
}
