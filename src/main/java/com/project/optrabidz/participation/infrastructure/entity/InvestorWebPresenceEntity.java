package com.project.optrabidz.participation.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "investor_web_presence")
public class InvestorWebPresenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "web_presence_id", nullable = false, updatable = false)
    private Long webPresenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id", nullable = false)
    private InvestorEntity investor;

    @Column(name = "url", nullable = false)
    private String url;

    public InvestorWebPresenceEntity() {
    }

    public Long getWebPresenceId() {
        return webPresenceId;
    }

    public void setWebPresenceId(Long webPresenceId) {
        this.webPresenceId = webPresenceId;
    }

    public InvestorEntity getInvestor() {
        return investor;
    }

    public void setInvestor(InvestorEntity investor) {
        this.investor = investor;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
