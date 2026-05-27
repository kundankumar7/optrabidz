package com.project.optrabidz.marketplace.application.recommendation.handler;

import com.project.optrabidz.classification.application.dto.response.StartupClassificationResponse;
import com.project.optrabidz.classification.application.port.in.StartupClassificationQueryPort;
import com.project.optrabidz.marketplace.application.recommendation.ClassificationKey;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationContext;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class ClassificationMatchScoringHandler implements RecommendationHandler {
    private final StartupClassificationQueryPort startupClassificationQueryPort;

    public ClassificationMatchScoringHandler(StartupClassificationQueryPort startupClassificationQueryPort) {
        this.startupClassificationQueryPort = startupClassificationQueryPort;
    }

    @Override
    public void handle(RecommendationContext context) {
        if (context.investorPreferenceKeys().isEmpty()) {
            return;
        }

        context.candidates().forEach(candidate -> {
            StartupClassificationResponse classifications =
                    startupClassificationQueryPort.getStartupClassifications(candidate.listing().getStartupId());
            if (classifications == null || classifications.classifications() == null) {
                return;
            }
            long matchCount = classifications.classifications()
                    .stream()
                    .map(ClassificationKey::from)
                    .filter(context.investorPreferenceKeys()::contains)
                    .count();
            if (matchCount > 0) {
                candidate.addScore((int) Math.min(50, matchCount * 25),
                        "Matches investor classification preferences");
            }
        });
    }
}
