package com.project.optrabidz.classification.domain.model;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class StartupClassificationProfile {
    private final Long startupId;
    private List<StartupClassification> classifications;

    private StartupClassificationProfile(Long startupId, List<StartupClassification> classifications) {
        Assert.notNull(startupId, "startupId must not be null");
        this.startupId = startupId;
        this.classifications = new ArrayList<>(classifications == null ? List.of() : classifications);
    }

    public static StartupClassificationProfile establish(Long startupId, List<StartupClassification> classifications) {
        return new StartupClassificationProfile(startupId, classifications);
    }

    public void declare(String classificationType, String classificationValue) {
        if (contains(classificationType, classificationValue)) {
            throw new IllegalStateException("Classification already exists");
        }
        classifications.add(StartupClassification.create(classificationType, classificationValue));
    }

    public void revoke(String classificationType, String classificationValue) {
        boolean removed = classifications.removeIf(classification ->
                sameEntry(classification, classificationType, classificationValue));
        if (!removed) {
            throw new IllegalStateException("Classification not found");
        }
    }

    public void replaceAll(List<StartupClassification> classifications) {
        this.classifications = new ArrayList<>(classifications == null ? List.of() : classifications);
    }

    public boolean contains(String classificationType, String classificationValue) {
        return classifications.stream()
                .anyMatch(classification -> sameEntry(classification, classificationType, classificationValue));
    }

    private boolean sameEntry(StartupClassification classification,
                              String classificationType,
                              String classificationValue) {
        StartupClassification candidate = StartupClassification.create(classificationType, classificationValue);
        return classification.getClassificationType().equals(candidate.getClassificationType())
                && classification.getClassificationValue().equals(candidate.getClassificationValue());
    }

    public Long getStartupId() {
        return startupId;
    }

    public List<StartupClassification> getClassifications() {
        return List.copyOf(classifications);
    }
}
