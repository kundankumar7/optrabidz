package com.project.optrabidz.marketplace.application.recommendation;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MarketplaceRecommendationPipeline {
    private final List<RecommendationHandler> handlers;

    public MarketplaceRecommendationPipeline(List<RecommendationHandler> handlers) {
        List<RecommendationHandler> orderedHandlers = new ArrayList<>(handlers);
        AnnotationAwareOrderComparator.sort(orderedHandlers);
        this.handlers = List.copyOf(orderedHandlers);
    }

    public List<RecommendationCandidate> recommend(RecommendationRequest request) {
        RecommendationContext context = new RecommendationContext(request);
        handlers.forEach(handler -> handler.handle(context));
        return context.candidates();
    }
}
