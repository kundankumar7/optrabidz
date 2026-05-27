package com.project.optrabidz.marketplace.application.recommendation.handler;

import com.project.optrabidz.marketplace.application.recommendation.RecommendationCandidate;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationContext;
import com.project.optrabidz.marketplace.application.recommendation.RecommendationHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@Order(70)
public class RecommendationRankingHandler implements RecommendationHandler {
    @Override
    public void handle(RecommendationContext context) {
        context.candidates(context.candidates()
                .stream()
                .sorted(Comparator
                        .comparing(RecommendationCandidate::score)
                        .reversed()
                        .thenComparing(candidate -> candidate.listing().getPublishedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
    }
}
