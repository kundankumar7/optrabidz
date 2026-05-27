package com.project.optrabidz.classification.infrastructure.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "startup_classification",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_startup_classification",
                columnNames = {"startup_id", "classification_type", "classification_value"}
        ))
public class StartupClassificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "startup_classification_id", nullable = false, updatable = false)
    private Long startupClassificationId;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(name = "classification_type", nullable = false)
    private String classificationType;

    @Column(name = "classification_value", nullable = false)
    private String classificationValue;

    public Long getStartupClassificationId() {
        return startupClassificationId;
    }

    public void setStartupClassificationId(Long startupClassificationId) {
        this.startupClassificationId = startupClassificationId;
    }

    public Long getStartupId() {
        return startupId;
    }

    public void setStartupId(Long startupId) {
        this.startupId = startupId;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public void setClassificationType(String classificationType) {
        this.classificationType = classificationType;
    }

    public String getClassificationValue() {
        return classificationValue;
    }

    public void setClassificationValue(String classificationValue) {
        this.classificationValue = classificationValue;
    }
}
