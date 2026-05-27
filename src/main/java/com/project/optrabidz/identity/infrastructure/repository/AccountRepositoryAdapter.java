package com.project.optrabidz.identity.infrastructure.repository;

import com.project.optrabidz.identity.domain.model.Account;
import com.project.optrabidz.identity.domain.repository.AccountRepository;
import com.project.optrabidz.identity.infrastructure.entity.AccountEntity;
import com.project.optrabidz.identity.infrastructure.mapper.AccountPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountRepositoryAdapter implements AccountRepository {
    private final JpaAccountRepository jpaAccountRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;

    public AccountRepositoryAdapter(JpaAccountRepository jpaAccountRepository,
                                    AccountPersistenceMapper accountPersistenceMapper) {
        this.jpaAccountRepository = jpaAccountRepository;
        this.accountPersistenceMapper = accountPersistenceMapper;
    }

    @Override
    public Account save(Account account) {
        AccountEntity persisted = jpaAccountRepository.save(accountPersistenceMapper.toEntity(account));
        return accountPersistenceMapper.toDomain(persisted);
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        return jpaAccountRepository.findWithRoleAndProfileByAccountId(accountId)
                .map(accountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findWithRoleByAccountId(Long accountId) {
        return jpaAccountRepository
                .findWithRoleByAccountId(accountId)
                .map(accountPersistenceMapper::toDomain);
    }
}
