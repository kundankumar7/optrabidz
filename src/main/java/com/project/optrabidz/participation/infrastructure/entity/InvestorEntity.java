package com.project.optrabidz.participation.infrastructure.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "investor")
public class InvestorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investor_id", nullable = false, updatable = false)
    private Long investorId;

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "public_display_name", nullable = false)
    private String publicDisplayName;

    @Column(name = "investor_description")
    private String investorDescription;

    @Column(name = "legal_entity_name")
    private String legalEntityName;

    @OneToMany(mappedBy = "investor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvestorWebPresenceEntity> webPresences = new ArrayList<>();

    public InvestorEntity() {
    }

    public Long getInvestorId() {
        return investorId;
    }

    public void setInvestorId(Long investorId) {
        this.investorId = investorId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public void setPublicDisplayName(String publicDisplayName) {
        this.publicDisplayName = publicDisplayName;
    }

    public String getInvestorDescription() {
        return investorDescription;
    }

    public void setInvestorDescription(String investorDescription) {
        this.investorDescription = investorDescription;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public List<InvestorWebPresenceEntity> getWebPresences() {
        return webPresences;
    }

    public void setWebPresences(List<InvestorWebPresenceEntity> webPresences) {
        this.webPresences = webPresences;
    }
}
