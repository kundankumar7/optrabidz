package com.project.optrabidz.classification.application.dto.response;

import java.util.List;

public record StartupClassificationResponse(
        Long startupId,
        List<ClassificationEntryResponse> classifications
) {
}
