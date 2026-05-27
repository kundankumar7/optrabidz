package com.project.optrabidz.classification.application.dto.response;

import java.util.List;

public record InvestorPreferenceResponse(
        Long investorId,
        List<ClassificationEntryResponse> preferences
) {
}
