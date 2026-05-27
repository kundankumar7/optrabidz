package com.project.optrabidz.classification.application.policy;

import com.project.optrabidz.classification.application.exception.InvalidClassificationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(Integer.MAX_VALUE)
public class DefaultStartupClassificationTypePolicy implements StartupClassificationTypePolicy {
    @Override
    public boolean supports(String classificationType) {
        return StringUtils.hasText(classificationType);
    }

    @Override
    public void validateValue(String classificationValue) {
        if (!StringUtils.hasText(classificationValue)) {
            throw new InvalidClassificationException("Classification value must not be blank");
        }
    }

    @Override
    public int maxAllowedPerType() {
        return Integer.MAX_VALUE;
    }
}
