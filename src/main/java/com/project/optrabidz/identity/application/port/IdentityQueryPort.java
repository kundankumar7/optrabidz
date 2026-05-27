package com.project.optrabidz.identity.application.port;

import com.project.optrabidz.identity.application.query.AccountSnapshot;

import java.util.Optional;

public interface IdentityQueryPort {
    Optional<AccountSnapshot> findAccountById(Long accountId);
}
