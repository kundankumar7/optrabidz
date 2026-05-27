package com.project.optrabidz.classification.application.port.in;

import com.project.optrabidz.classification.application.command.AddInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.RemoveInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.ReplaceInvestorPreferencesCommand;
import com.project.optrabidz.common.api.response.MessageData;

public interface InvestorPreferenceCommandPort {
    MessageData addPreference(AddInvestorPreferenceCommand command);

    MessageData replacePreferences(ReplaceInvestorPreferencesCommand command);

    MessageData removePreference(RemoveInvestorPreferenceCommand command);
}
