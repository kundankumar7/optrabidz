package com.project.optrabidz.identity.application.service;

import com.project.optrabidz.identity.application.port.IdentityQueryPort;
import com.project.optrabidz.identity.application.query.AccountSnapshot;
import com.project.optrabidz.identity.domain.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AccountQueryService implements IdentityQueryPort {
    private final AccountRepository accountRepository;

    public AccountQueryService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<AccountSnapshot> findAccountById(Long accountId) {
        Assert.notNull(accountId, "accountId must not be null");
        return accountRepository.findById(accountId)
                .map(AccountSnapshot::from);
    }
}
