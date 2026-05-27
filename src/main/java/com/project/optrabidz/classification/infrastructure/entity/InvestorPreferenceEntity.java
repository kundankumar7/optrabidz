package com.project.optrabidz.classification.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "investor_preference",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_investor_preference",
                columnNames = {"investor_id", "preference_type", "preference_value"}
        ))
public class InvestorPreferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investor_preference_id", nullable = false, updatable = false)
    private Long investorPreferenceId;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Column(name = "preference_type", nullable = false)
    private String preferenceType;

    @Column(name = "preference_value", nullable = false)
    private String preferenceValue;

    public Long getInvestorPreferenceId() {
        return investorPreferenceId;
    }

    public void setInvestorPreferenceId(Long investorPreferenceId) {
        this.investorPreferenceId = investorPreferenceId;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public void setInvestorId(Long investorId) {
        this.investorId = investorId;
    }

    public String getPreferenceType() {
        return preferenceType;
    }

    public void setPreferenceType(String preferenceType) {
        this.preferenceType = preferenceType;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }

    public void setPreferenceValue(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }
}
