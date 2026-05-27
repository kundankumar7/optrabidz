package com.project.optrabidz.marketplace.application.policy;

import com.project.optrabidz.marketplace.application.exception.UnsupportedFundingModelException;
import com.project.optrabidz.marketplace.domain.model.FundingModel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class FundingModelPolicyResolver {
    private final Map<FundingModel, FundingModelPolicy> policies = new EnumMap<>(FundingModel.class);

    public FundingModelPolicyResolver(List<FundingModelPolicy> policies) {
        for (FundingModelPolicy policy : policies) {
            this.policies.put(policy.supports(), policy);
        }
    }

    public FundingModelPolicy resolve(FundingModel fundingModel) {
        FundingModelPolicy policy = policies.get(fundingModel);
        if (policy == null) {
            throw new UnsupportedFundingModelException("Only DEBT funding model is currently supported");
        }
        return policy;
    }
}
