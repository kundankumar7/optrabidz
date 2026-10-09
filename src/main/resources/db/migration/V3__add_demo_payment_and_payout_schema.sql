CREATE TYPE payment_account_binding_status_enum AS ENUM (
    'PENDING_VERIFICATION',
    'VERIFIED',
    'DEACTIVATED'
);

CREATE TYPE payout_transfer_status_enum AS ENUM (
    'PENDING',
    'PROCESSING',
    'CONFIRMED',
    'FAILED'
);

CREATE TABLE payment_account_binding (
    payment_account_binding_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL,
    provider_code VARCHAR(50) NOT NULL,
    command_idempotency_key VARCHAR(120) NOT NULL,
    replaces_binding_id BIGINT NULL,
    external_recipient_reference VARCHAR(120) NOT NULL,
    binding_status payment_account_binding_status_enum NOT NULL,
    masked_account_suffix VARCHAR(8) NOT NULL,
    demo_bank_label VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ NULL,
    deactivated_at TIMESTAMPTZ NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_payment_account_binding_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_account_binding_provider
    FOREIGN KEY (provider_code) REFERENCES payment_provider(provider_code)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_account_binding_replaced
    FOREIGN KEY (replaces_binding_id) REFERENCES payment_account_binding(payment_account_binding_id)
    ON DELETE RESTRICT,

    CONSTRAINT uq_payment_account_binding_command
    UNIQUE (account_id, provider_code, command_idempotency_key),

    CONSTRAINT uq_payment_account_binding_external_reference
    UNIQUE (provider_code, external_recipient_reference),

    CONSTRAINT uq_payment_account_binding_identity_provider
    UNIQUE (payment_account_binding_id, provider_code),

    CONSTRAINT chk_payment_account_binding_text
    CHECK (
        length(trim(provider_code)) > 0
        AND length(trim(command_idempotency_key)) > 0
        AND length(trim(external_recipient_reference)) > 0
        AND length(trim(masked_account_suffix)) > 0
        AND length(trim(demo_bank_label)) > 0
    ),

    CONSTRAINT chk_payment_account_binding_time
    CHECK (
        (verified_at IS NULL OR verified_at >= created_at)
        AND (deactivated_at IS NULL OR deactivated_at >= created_at)
        AND (verified_at IS NULL OR deactivated_at IS NULL OR deactivated_at >= verified_at)
    ),

    CONSTRAINT chk_payment_account_binding_status_timestamp
    CHECK (
        (
            binding_status = 'PENDING_VERIFICATION'
            AND verified_at IS NULL
            AND deactivated_at IS NULL
        )
        OR
        (
            binding_status = 'VERIFIED'
            AND verified_at IS NOT NULL
            AND deactivated_at IS NULL
        )
        OR
        (
            binding_status = 'DEACTIVATED'
            AND deactivated_at IS NOT NULL
        )
    )
);

CREATE UNIQUE INDEX uq_payment_account_binding_active
ON payment_account_binding(account_id, provider_code)
WHERE binding_status <> 'DEACTIVATED';

CREATE INDEX idx_payment_account_binding_account_status
ON payment_account_binding(account_id, binding_status);

CREATE TABLE payout_transfer (
    payout_transfer_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_intent_id BIGINT NOT NULL,
    payment_account_binding_id BIGINT NOT NULL,
    provider_code VARCHAR(50) NOT NULL,
    provider_transfer_reference VARCHAR(120) NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    transfer_status payout_transfer_status_enum NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    last_attempt_at TIMESTAMPTZ NULL,
    confirmed_at TIMESTAMPTZ NULL,
    latest_failure_at TIMESTAMPTZ NULL,
    latest_failure_code VARCHAR(80) NULL,
    latest_failure_message VARCHAR(500) NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_payout_transfer_payment_intent
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intent(payment_intent_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payout_transfer_binding_provider
    FOREIGN KEY (payment_account_binding_id, provider_code)
    REFERENCES payment_account_binding(payment_account_binding_id, provider_code)
    ON DELETE RESTRICT,

    CONSTRAINT uq_payout_transfer_payment_intent
    UNIQUE (payment_intent_id),

    CONSTRAINT uq_payout_transfer_idempotency_key
    UNIQUE (idempotency_key),

    CONSTRAINT chk_payout_transfer_amount
    CHECK (amount > 0),

    CONSTRAINT chk_payout_transfer_currency
    CHECK (length(trim(currency_code)) > 0),

    CONSTRAINT chk_payout_transfer_text
    CHECK (
        length(trim(provider_code)) > 0
        AND length(trim(idempotency_key)) > 0
        AND (provider_transfer_reference IS NULL OR length(trim(provider_transfer_reference)) > 0)
        AND (latest_failure_code IS NULL OR length(trim(latest_failure_code)) > 0)
    ),

    CONSTRAINT chk_payout_transfer_attempt_count
    CHECK (attempt_count >= 0),

    CONSTRAINT chk_payout_transfer_time
    CHECK (
        updated_at >= created_at
        AND (last_attempt_at IS NULL OR last_attempt_at >= created_at)
        AND (confirmed_at IS NULL OR confirmed_at >= created_at)
        AND (latest_failure_at IS NULL OR latest_failure_at >= created_at)
        AND (last_attempt_at IS NULL OR updated_at >= last_attempt_at)
        AND (confirmed_at IS NULL OR updated_at >= confirmed_at)
        AND (latest_failure_at IS NULL OR updated_at >= latest_failure_at)
        AND (last_attempt_at IS NULL OR confirmed_at IS NULL OR confirmed_at >= last_attempt_at)
        AND (transfer_status <> 'FAILED' OR latest_failure_at >= last_attempt_at)
    ),

    CONSTRAINT chk_payout_transfer_failure_detail
    CHECK (
        (
            latest_failure_at IS NULL
            AND latest_failure_code IS NULL
            AND latest_failure_message IS NULL
        )
        OR
        (
            latest_failure_at IS NOT NULL
            AND latest_failure_code IS NOT NULL
        )
    ),

    CONSTRAINT chk_payout_transfer_status_timestamp
    CHECK (
        (
            transfer_status = 'PENDING'
            AND attempt_count = 0
            AND provider_transfer_reference IS NULL
            AND last_attempt_at IS NULL
            AND confirmed_at IS NULL
            AND latest_failure_at IS NULL
        )
        OR
        (
            transfer_status = 'PROCESSING'
            AND attempt_count >= 1
            AND last_attempt_at IS NOT NULL
            AND confirmed_at IS NULL
        )
        OR
        (
            transfer_status = 'FAILED'
            AND attempt_count >= 1
            AND last_attempt_at IS NOT NULL
            AND confirmed_at IS NULL
            AND latest_failure_at IS NOT NULL
            AND latest_failure_code IS NOT NULL
        )
        OR
        (
            transfer_status = 'CONFIRMED'
            AND attempt_count >= 1
            AND last_attempt_at IS NOT NULL
            AND confirmed_at IS NOT NULL
            AND provider_transfer_reference IS NOT NULL
        )
    )
);

CREATE UNIQUE INDEX uq_payout_transfer_provider_reference
ON payout_transfer(provider_code, provider_transfer_reference)
WHERE provider_transfer_reference IS NOT NULL;

CREATE INDEX idx_payout_transfer_status_created
ON payout_transfer(transfer_status, created_at);

ALTER TABLE settlement
DROP CONSTRAINT chk_settlement_state_timestamp;

ALTER TABLE settlement
ADD CONSTRAINT chk_settlement_state_timestamp
CHECK (
    (
        settlement_state = 'SETTLEMENT_PENDING'
        AND confirmed_at IS NULL
        AND failed_at IS NULL
        AND expired_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        settlement_state = 'SETTLEMENT_PAYOUT_PENDING'
        AND confirmed_payment_intent_id IS NOT NULL
        AND confirmed_at IS NULL
        AND failed_at IS NULL
        AND expired_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        settlement_state = 'SETTLEMENT_CONFIRMED'
        AND confirmed_at IS NOT NULL
        AND failed_at IS NULL
        AND expired_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        settlement_state = 'SETTLEMENT_FAILED'
        AND failed_at IS NOT NULL
        AND confirmed_at IS NULL
        AND expired_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        settlement_state = 'SETTLEMENT_EXPIRED'
        AND expired_at IS NOT NULL
        AND confirmed_at IS NULL
        AND failed_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        settlement_state = 'SETTLEMENT_CANCELLED'
        AND cancelled_at IS NOT NULL
        AND confirmed_at IS NULL
        AND failed_at IS NULL
        AND expired_at IS NULL
    )
);

ALTER TABLE repayment_installment
DROP CONSTRAINT chk_repayment_installment_status_timestamp;

ALTER TABLE repayment_installment
ADD CONSTRAINT chk_repayment_installment_status_timestamp
CHECK (
    (
        installment_status = 'NOT_STARTED'
        AND payment_started_at IS NULL
        AND paid_at IS NULL
        AND failed_at IS NULL
        AND overdue_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        installment_status = 'PAYMENT_IN_PROGRESS'
        AND payment_started_at IS NOT NULL
        AND paid_at IS NULL
        AND failed_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        installment_status = 'PAYOUT_PENDING'
        AND confirmed_payment_intent_id IS NOT NULL
        AND payment_started_at IS NOT NULL
        AND paid_at IS NULL
        AND failed_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        installment_status = 'PAYMENT_FAILED'
        AND failed_at IS NOT NULL
        AND paid_at IS NULL
        AND overdue_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        installment_status = 'OVERDUE'
        AND overdue_at IS NOT NULL
        AND paid_at IS NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        installment_status = 'PAID'
        AND paid_at IS NOT NULL
        AND cancelled_at IS NULL
    )
    OR
    (
        installment_status = 'CANCELLED'
        AND cancelled_at IS NOT NULL
        AND paid_at IS NULL
    )
);

INSERT INTO payment_provider(provider_code, display_name, enabled)
VALUES ('DEMO', 'Demonstration Payment Simulator', true);

INSERT INTO payment_provider_method(provider_code, method_type, currency_code, enabled)
VALUES
    ('DEMO', 'UPI', 'INR', true),
    ('DEMO', 'CARD', 'INR', true);

COMMENT ON TABLE payment_account_binding IS
'Generated demonstration receiving-account binding. No real financial credentials are stored.';

COMMENT ON TABLE payout_transfer IS
'Provider-neutral payout delivery record created after confirmed collection.';
