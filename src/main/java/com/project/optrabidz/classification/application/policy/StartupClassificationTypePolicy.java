package com.project.optrabidz.classification.application.policy;

public interface StartupClassificationTypePolicy {
    boolean supports(String classificationType);

    void validateValue(String classificationValue);

    int maxAllowedPerType();
}
