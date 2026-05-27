package com.project.optrabidz.marketplace.application.specification;

import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.marketplace.application.exception.MarketplaceAccessException;
import com.project.optrabidz.marketplace.domain.model.Agreement;
import org.springframework.stereotype.Component;

@Component
public class AgreementVisibleToActorSpec {
    public void assertSatisfiedBy(RoleType roleType,
                                  Agreement agreement,
                                  Long requesterStartupId,
                                  Long requesterInvestorId) {
        if (roleType == RoleType.STARTUP
                && requesterStartupId != null
                && requesterStartupId.equals(agreement.getStartupId())) {
            return;
        }
        if (roleType == RoleType.INVESTOR
                && requesterInvestorId != null
                && requesterInvestorId.equals(agreement.getInvestorId())) {
            return;
        }
        if (roleType == RoleType.ADMIN) {
            return;
        }
        throw new MarketplaceAccessException("You are not authorized to view this agreement");
    }
}
