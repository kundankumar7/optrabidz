package com.project.optrabidz.participation.infrastructure.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "startup")
public class StartupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "startup_id", nullable = false, updatable = false)
    private Long startupId;

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "legal_entity_name", nullable = false)
    private String legalEntityName;

    @Column(name = "incorporation_country_code", nullable = false)
    private String incorporationCountryCode;

    @Column(name = "public_display_name", nullable = false)
    private String publicDisplayName;

    @Column(name = "business_description")
    private String businessDescription;

    @OneToMany(mappedBy = "startup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StartupWebPresenceEntity> webPresences = new ArrayList<>();

    @OneToMany(mappedBy = "startup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StartupLegalRegistrationEntity> legalRegistrations = new ArrayList<>();

    public StartupEntity() {
    }

    public Long getStartupId() {
        return startupId;
    }

    public void setStartupId(Long startupId) {
        this.startupId = startupId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public String getIncorporationCountryCode() {
        return incorporationCountryCode;
    }

    public void setIncorporationCountryCode(String incorporationCountryCode) {
        this.incorporationCountryCode = incorporationCountryCode;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public void setPublicDisplayName(String publicDisplayName) {
        this.publicDisplayName = publicDisplayName;
    }

    public String getBusinessDescription() {
        return businessDescription;
    }

    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }

    public List<StartupWebPresenceEntity> getWebPresences() {
        return webPresences;
    }

    public void setWebPresences(List<StartupWebPresenceEntity> webPresences) {
        this.webPresences = webPresences;
    }

    public List<StartupLegalRegistrationEntity> getLegalRegistrations() {
        return legalRegistrations;
    }

    public void setLegalRegistrations(List<StartupLegalRegistrationEntity> legalRegistrations) {
        this.legalRegistrations = legalRegistrations;
    }
}
