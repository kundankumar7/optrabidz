package com.project.optrabidz.classification.application.port.in;

import com.project.optrabidz.classification.application.command.AddStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.RemoveStartupClassificationCommand;
import com.project.optrabidz.classification.application.command.ReplaceStartupClassificationsCommand;
public interface StartupClassificationCommandPort {
    void addClassification(AddStartupClassificationCommand command);

    void replaceClassifications(ReplaceStartupClassificationsCommand command);

    void removeClassification(RemoveStartupClassificationCommand command);
}
