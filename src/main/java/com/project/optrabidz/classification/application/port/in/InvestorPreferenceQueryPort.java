package com.project.optrabidz.classification.application.port.in;

import com.project.optrabidz.classification.application.dto.response.InvestorPreferenceResponse;

public interface InvestorPreferenceQueryPort {
    InvestorPreferenceResponse getMyPreferences(Long accountId);

    InvestorPreferenceResponse getInvestorPreferences(Long investorId);
}
