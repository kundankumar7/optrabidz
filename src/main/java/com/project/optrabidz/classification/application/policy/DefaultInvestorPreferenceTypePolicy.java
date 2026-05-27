package com.project.optrabidz.classification.application.policy;

import com.project.optrabidz.classification.application.exception.InvalidClassificationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(Integer.MAX_VALUE)
public class DefaultInvestorPreferenceTypePolicy implements InvestorPreferenceTypePolicy {
    @Override
    public boolean supports(String preferenceType) {
        return StringUtils.hasText(preferenceType);
    }

    @Override
    public void validateValue(String preferenceValue) {
        if (!StringUtils.hasText(preferenceValue)) {
            throw new InvalidClassificationException("Preference value must not be blank");
        }
    }

    @Override
    public int maxAllowedPerType() {
        return Integer.MAX_VALUE;
    }
}
