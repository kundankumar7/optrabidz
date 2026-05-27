package com.project.optrabidz.marketplace.application.recommendation.handler;

import com.project.optrabidz.classification.application.dto.response.ClassificationEntryResponse;
import com.project.optrabidz.classification.application.dto.response.InvestorPreferenceResponse;
import com.project.optrabidz.classification.application.port.in.InvestorPreferenceQueryPort;
import com.project.optrabidz.marketplace.application.recommendation.ClassificationKey;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationContext;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(10)
public class InvestorPreferenceLoadHandler implements RecommendationHandler {
    private final InvestorPreferenceQueryPort investorPreferenceQueryPort;

    public InvestorPreferenceLoadHandler(InvestorPreferenceQueryPort investorPreferenceQueryPort) {
        this.investorPreferenceQueryPort = investorPreferenceQueryPort;
    }

    @Override
    public void handle(RecommendationContext context) {
        InvestorPreferenceResponse preferences =
                investorPreferenceQueryPort.getMyPreferences(context.request().accountId());
        Set<String> preferenceKeys = (preferences == null || preferences.preferences() == null
                ? List.<ClassificationEntryResponse>of()
                : preferences.preferences())
                .stream()
                .map(ClassificationKey::from)
                .collect(Collectors.toSet());
        context.investorPreferenceKeys(preferenceKeys);
    }
}
