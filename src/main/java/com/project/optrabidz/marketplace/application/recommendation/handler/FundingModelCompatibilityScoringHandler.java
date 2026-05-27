package com.project.optrabidz.marketplace.application.recommendation.handler;

import com.project.optrabidz.marketplace.application.recommendation.RecommendationContext;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationHandler;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class FundingModelCompatibilityScoringHandler implements RecommendationHandler {
    @Override
    public void handle(RecommendationContext context) {
        context.candidates().forEach(candidate -> {
            if (candidate.listing().getFundingModel() == FundingModel.DEBT) {
                candidate.addScore(20, "Matches currently supported funding model");
            }
        });
    }
}
