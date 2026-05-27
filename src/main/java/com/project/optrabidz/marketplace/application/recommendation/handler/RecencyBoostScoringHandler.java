package com.project.optrabidz.marketplace.application.recommendation.handler;

import com.project.optrabidz.marketplace.application.recommendation.RecommendationContext;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Order(50)
public class RecencyBoostScoringHandler implements RecommendationHandler {
    @Override
    public void handle(RecommendationContext context) {
        Instant now = Instant.now();
        context.candidates().forEach(candidate -> {
            Instant publishedAt = candidate.listing().getPublishedAt();
            if (publishedAt != null && Duration.between(publishedAt, now).toDays() <= 7) {
                candidate.addScore(10, "Recently published");
            }
        });
    }
}
