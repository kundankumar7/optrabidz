package com.project.optrabidz.classification.application.port.in;

import com.project.optrabidz.classification.application.command.AddInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.RemoveInvestorPreferenceCommand;
import com.project.optrabidz.classification.application.command.ReplaceInvestorPreferencesCommand;
public interface InvestorPreferenceCommandPort {
    void addPreference(AddInvestorPreferenceCommand command);

    void replacePreferences(ReplaceInvestorPreferencesCommand command);

    void removePreference(RemoveInvestorPreferenceCommand command);
}
