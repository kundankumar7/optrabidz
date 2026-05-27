package com.project.optrabidz.participation.domain.model;

import org.springframework.util.Assert;

import java.util.List;

public class Startup {
    private Long startupId;
    private Long accountId;
    private String legalEntityName;
    private String incorporationCountryCode;
    private String publicDisplayName;
    private String businessDescription;
    private List<String> webPresences;
    private List<StartupLegalRegistration> legalRegistrations;

    protected Startup() {
    }

    public Startup(Long startupId,
                   Long accountId,
                   String legalEntityName,
                   String incorporationCountryCode,
                   String publicDisplayName,
                   String businessDescription,
                   List<String> webPresences,
                   List<StartupLegalRegistration> legalRegistrations) {
        this.startupId = startupId;
        this.accountId = accountId;
        updateRepresentation(
                legalEntityName,
                incorporationCountryCode,
                publicDisplayName,
                businessDescription,
                webPresences,
                legalRegistrations
        );
    }

    public static Startup establish(Long accountId,
                                    String legalEntityName,
                                    String incorporationCountryCode,
                                    String publicDisplayName,
                                    String businessDescription,
                                    List<String> webPresences,
                                    List<StartupLegalRegistration> legalRegistrations) {
        Assert.notNull(accountId, "accountId must not be null");
        return new Startup(
                null,
                accountId,
                legalEntityName,
                incorporationCountryCode,
                publicDisplayName,
                businessDescription,
                webPresences,
                legalRegistrations
        );
    }

    public void updateRepresentation(String legalEntityName,
                                     String incorporationCountryCode,
                                     String publicDisplayName,
                                     String businessDescription,
                                     List<String> webPresences,
                                     List<StartupLegalRegistration> legalRegistrations) {
        Assert.hasText(legalEntityName, "legalEntityName must not be blank");
        Assert.hasText(incorporationCountryCode, "incorporationCountryCode must not be blank");
        Assert.hasText(publicDisplayName, "publicDisplayName must not be blank");
        Assert.hasText(businessDescription, "businessDescription must not be blank");

        this.legalEntityName = legalEntityName;
        this.incorporationCountryCode = incorporationCountryCode;
        this.publicDisplayName = publicDisplayName;
        this.businessDescription = businessDescription;
        this.webPresences = List.copyOf(webPresences == null ? List.of() : webPresences);
        this.legalRegistrations = List.copyOf(legalRegistrations == null ? List.of() : legalRegistrations);
    }

    public Long getStartupId() {
        return startupId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public String getIncorporationCountryCode() {
        return incorporationCountryCode;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public String getBusinessDescription() {
        return businessDescription;
    }

    public List<String> getWebPresences() {
        return webPresences;
    }

    public List<StartupLegalRegistration> getLegalRegistrations() {
        return legalRegistrations;
    }
}
