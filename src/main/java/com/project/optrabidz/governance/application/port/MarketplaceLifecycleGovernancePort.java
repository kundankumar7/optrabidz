package com.project.optrabidz.governance.application.port;

import java.time.Instant;

public interface MarketplaceLifecycleGovernancePort {
    int expireOpenListings(Instant now, int batchSize);
}
