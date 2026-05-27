package com.project.optrabidz.participation.application.profile;

import com.project.optrabidz.identity.application.command.UpdateProfileStatusCommand;
import com.project.optrabidz.identity.application.port.IdentityCommandPort;
import com.project.optrabidz.identity.domain.model.ProfileStatus;
import com.project.optrabidz.participation.application.event.ParticipationProfileChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ParticipationProfileStatusSyncHandler {
    private final ParticipationProfileCompletenessEvaluator evaluator;
    private final IdentityCommandPort identityCommandPort;

    public ParticipationProfileStatusSyncHandler(ParticipationProfileCompletenessEvaluator evaluator,
                                                 IdentityCommandPort identityCommandPort) {
        this.evaluator = evaluator;
        this.identityCommandPort = identityCommandPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ParticipationProfileChangedEvent event) {
        ProfileStatus profileStatus = evaluator.evaluate(event.accountId(), event.roleType());
        identityCommandPort.updateProfileStatus(new UpdateProfileStatusCommand(
                event.accountId(),
                profileStatus
        ));
    }
}
