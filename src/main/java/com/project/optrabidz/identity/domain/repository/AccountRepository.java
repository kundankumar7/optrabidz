package com.project.optrabidz.identity.domain.repository;

import com.project.optrabidz.identity.domain.model.Account;

import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(Long accountId);
    Optional<Account> findWithRoleByAccountId(Long accountId);
}
