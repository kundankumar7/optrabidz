package com.project.optrabidz.financial.infrastructure.repository;

import com.project.optrabidz.financial.domain.model.PaymentAccountBinding;
import com.project.optrabidz.financial.domain.model.PaymentAccountBindingStatus;
import com.project.optrabidz.financial.domain.repository.PaymentAccountBindingRepository;
import com.project.optrabidz.financial.infrastructure.mapper.PaymentAccountBindingPersistenceMapper;
import com.project.optrabidz.testsupport.PostgresJpaIntegrationTestSupport;
import com.project.optrabidz.testsupport.PostgresTestDataFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        PaymentAccountBindingPersistenceMapper.class,
        PaymentAccountBindingRepositoryAdapter.class
})
class PaymentAccountBindingRepositoryIT extends PostgresJpaIntegrationTestSupport {
    private static final Instant NOW = Instant.parse("2026-10-01T09:00:00Z");

    @Autowired
    private PaymentAccountBindingRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long accountId;

    @BeforeEach
    void setUp() {
        accountId = new PostgresTestDataFixture(jdbcTemplate, NOW)
                .createStartup("binding repository")
                .accountId();
    }

    @Test
    void findsActiveReplayAndVerifiedBindingThroughDistinctQueries() {
        PaymentAccountBinding saved = repository.save(pending("binding-command-1", "recipient-1"));

        assertThat(repository.findByAccountIdAndProviderCodeAndActive(accountId, "DEMO"))
                .isPresent()
                .get()
                .extracting(PaymentAccountBinding::getPaymentAccountBindingId)
                .isEqualTo(saved.getPaymentAccountBindingId());
        assertThat(repository.findByAccountIdAndProviderCodeAndCommandIdempotencyKey(
                accountId, "DEMO", "binding-command-1"))
                .isPresent();
        assertThat(repository.findVerifiedByAccountIdAndProviderCode(accountId, "DEMO"))
                .isEmpty();

        saved.verify(NOW.plusSeconds(30));
        repository.save(saved);

        assertThat(repository.findVerifiedByAccountIdAndProviderCode(accountId, "DEMO"))
                .isPresent()
                .get()
                .satisfies(binding -> {
                    assertThat(binding.getBindingStatus()).isEqualTo(PaymentAccountBindingStatus.VERIFIED);
                    assertThat(binding.isReady()).isTrue();
                });
    }

    @Test
    void databaseRejectsASecondNonDeactivatedBindingForTheSameAccountAndProvider() {
        repository.save(pending("binding-command-1", "recipient-1"));

        assertThatThrownBy(() -> repository.save(pending("binding-command-2", "recipient-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void staleLifecycleUpdateCannotOverwriteAConcurrentChange() {
        PaymentAccountBinding saved = repository.save(pending("binding-command-1", "recipient-1"));
        PaymentAccountBinding firstReader = repository.findById(saved.getPaymentAccountBindingId()).orElseThrow();
        PaymentAccountBinding staleReader = repository.findById(saved.getPaymentAccountBindingId()).orElseThrow();

        firstReader.verify(NOW.plusSeconds(30));
        repository.save(firstReader);
        staleReader.deactivate(NOW.plusSeconds(40));

        assertThatThrownBy(() -> repository.save(staleReader))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private PaymentAccountBinding pending(String commandKey, String recipientReference) {
        return PaymentAccountBinding.createPending(
                accountId,
                "DEMO",
                commandKey,
                null,
                recipientReference,
                "1234",
                "Demonstration Bank",
                NOW
        );
    }
}
