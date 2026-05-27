package com.project.optrabidz.participation.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "startup_web_presence")
public class StartupWebPresenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "web_presence_id", nullable = false, updatable = false)
    private Long webPresenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    private StartupEntity startup;

    @Column(name = "url", nullable = false)
    private String url;

    public StartupWebPresenceEntity() {
    }

    public Long getWebPresenceId() {
        return webPresenceId;
    }

    public void setWebPresenceId(Long webPresenceId) {
        this.webPresenceId = webPresenceId;
    }

    public StartupEntity getStartup() {
        return startup;
    }

    public void setStartup(StartupEntity startup) {
        this.startup = startup;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
