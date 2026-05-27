package com.project.optrabidz.classification.application.port.in;

import com.project.optrabidz.classification.application.dto.response.StartupClassificationResponse;

public interface StartupClassificationQueryPort {
    StartupClassificationResponse getMyClassifications(Long accountId);

    StartupClassificationResponse getStartupClassifications(Long startupId);
}
