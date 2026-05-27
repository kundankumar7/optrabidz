package com.project.optrabidz.classification.domain.model;

import org.springframework.util.Assert;

import java.util.Locale;

public class StartupClassification {
    private Long startupClassificationId;
    private String classificationType;
    private String classificationValue;

    protected StartupClassification() {
    }

    public StartupClassification(Long startupClassificationId,
                                 String classificationType,
                                 String classificationValue) {
        this.startupClassificationId = startupClassificationId;
        this.classificationType = normalizeType(classificationType);
        this.classificationValue = normalizeValue(classificationValue);
    }

    public static StartupClassification create(String classificationType, String classificationValue) {
        return new StartupClassification(null, classificationType, classificationValue);
    }

    private String normalizeType(String classificationType) {
        Assert.hasText(classificationType, "classificationType must not be blank");
        return classificationType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeValue(String classificationValue) {
        Assert.hasText(classificationValue, "classificationValue must not be blank");
        return classificationValue.trim();
    }

    public Long getStartupClassificationId() {
        return startupClassificationId;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public String getClassificationValue() {
        return classificationValue;
    }
}
