package com.project.optrabidz.classification.application.port.in;

import com.project.optrabidz.classification.application.command.AddStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.RemoveStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.ReplaceStartupClassificationsCommand;
import com.project.optrabidz.common.api.response.MessageData;

public interface StartupClassificationCommandPort {
    MessageData addClassification(AddStartupClassificationCommand command);

    MessageData replaceClassifications(ReplaceStartupClassificationsCommand command);

    MessageData removeClassification(RemoveStartupClassificationCommand command);
}
