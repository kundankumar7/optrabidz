package com.project.optrabidz.marketplace.application.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class ListingExpiryPolicy {
    private final int defaultExpiryDays;

    public ListingExpiryPolicy(
            @Value("${optrabidz.marketplace.listing.default-expiry-days:14}") int defaultExpiryDays) {
        this.defaultExpiryDays = Math.max(1, defaultExpiryDays);
    }

    public Instant expiresAtFor(Instant publishedAt) {
        return publishedAt.plus(defaultExpiryDays, ChronoUnit.DAYS);
    }

    public int defaultExpiryDays() {
        return defaultExpiryDays;
    }
}
