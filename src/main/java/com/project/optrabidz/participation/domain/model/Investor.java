package com.project.optrabidz.participation.domain.model;

import org.springframework.util.Assert;

import java.util.List;

public class Investor {
    private Long investorId;
    private Long accountId;
    private String publicDisplayName;
    private String investorDescription;
    private String legalEntityName;
    private List<String> webPresences;

    protected Investor() {
    }

    public Investor(Long investorId,
                    Long accountId,
                    String publicDisplayName,
                    String investorDescription,
                    String legalEntityName,
                    List<String> webPresences) {
        this.investorId = investorId;
        this.accountId = accountId;
        updateRepresentation(publicDisplayName, investorDescription, legalEntityName, webPresences);
    }

    public static Investor establish(Long accountId,
                                     String publicDisplayName,
                                     String investorDescription,
                                     String legalEntityName,
                                     List<String> webPresences) {
        Assert.notNull(accountId, "accountId must not be null");
        return new Investor(null, accountId, publicDisplayName, investorDescription, legalEntityName, webPresences);
    }

    public void updateRepresentation(String publicDisplayName,
                                     String investorDescription,
                                     String legalEntityName,
                                     List<String> webPresences) {
        Assert.hasText(publicDisplayName, "publicDisplayName must not be blank");
        Assert.hasText(investorDescription, "investorDescription must not be blank");
        Assert.hasText(legalEntityName, "legalEntityName must not be blank");
        this.publicDisplayName = publicDisplayName;
        this.investorDescription = investorDescription;
        this.legalEntityName = legalEntityName;
        this.webPresences = List.copyOf(webPresences == null ? List.of() : webPresences);
    }

    public Long getInvestorId() {
        return investorId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public String getInvestorDescription() {
        return investorDescription;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public List<String> getWebPresences() {
        return webPresences;
    }
}
