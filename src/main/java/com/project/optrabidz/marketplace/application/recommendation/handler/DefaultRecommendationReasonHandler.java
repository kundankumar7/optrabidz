package com.project.optrabidz.marketplace.application.recommendation.handler;

import com.project.optrabidz.marketplace.application.recommendation.RecommendationContext;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(60)
public class DefaultRecommendationReasonHandler implements RecommendationHandler {
    @Override
    public void handle(RecommendationContext context) {
        context.candidates().forEach(candidate -> {
            if (!candidate.hasReasons()) {
                candidate.addReason("Open listing available for bidding");
            }
        });
    }
}
