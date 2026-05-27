package com.project.optrabidz.classification.application.port.out;

import java.util.Optional;

public interface ParticipationActorQueryPort {
    Optional<Long> findStartupIdByAccountId(Long accountId);

    Optional<Long> findInvestorIdByAccountId(Long accountId);
}
