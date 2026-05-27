package com.project.optrabidz.participation.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "startup_legal_registration")
public class StartupLegalRegistrationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id", nullable = false, updatable = false)
    private Long registrationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private StartupEntity startup;

    @Column(name = "registration_type", nullable = false)
    private String registrationType;

    @Column(name = "registration_value", nullable = false)
    private String registrationValue;

    public StartupLegalRegistrationEntity() {
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public StartupEntity getStartup() {
        return startup;
    }

    public void setStartup(StartupEntity startup) {
        this.startup = startup;
    }

    public String getRegistrationType() {
        return registrationType;
    }

    public void setRegistrationType(String registrationType) {
        this.registrationType = registrationType;
    }

    public String getRegistrationValue() {
        return registrationValue;
    }

    public void setRegistrationValue(String registrationValue) {
        this.registrationValue = registrationValue;
    }
}
