-- =========================================================
-- OPTRABIDZ DATABASE SCHEMA
-- =========================================================
--
-- This file is organized by bounded context and contains the table shape,
-- constraints, indexes, triggers, and seed/reference data in one place.


-- =========================================================
-- 1. ENUM TYPES
-- =========================================================

CREATE TYPE account_state_enum AS ENUM (
    'CREATED',
    'ACTIVE',
    'SUSPENDED',
    'DEACTIVATED'
);

CREATE TYPE role_type_enum AS ENUM (
    'STARTUP',
    'INVESTOR',
    'ADMIN'
);

CREATE TYPE profile_status_enum AS ENUM (
    'INCOMPLETE',
    'COMPLETE'
);

CREATE TYPE credential_status_enum AS ENUM (
    'CREATED',
    'ACTIVE',
    'LOCKED',
    'DISABLED'
);

CREATE TYPE session_status_enum AS ENUM (
    'CREATED',
    'ACTIVE',
    'EXPIRED',
    'TERMINATED'
);

CREATE TYPE admin_state_enum AS ENUM (
    'ACTIVE',
    'REVOKED'
);

CREATE TYPE listing_state_enum AS ENUM (
    'DRAFT',
    'OPEN',
    'AGREEMENT_REACHED',
    'FUNDED',
    'CLOSED'
);

CREATE TYPE funding_model_enum AS ENUM (
    'DEBT',
    'EQUITY',
    'HYBRID'
);

CREATE TYPE bid_state_enum AS ENUM (
    'SUBMITTED',
    'ACCEPTED',
    'PENDING_SETTLEMENT',
    'FUNDED',
    'WITHDRAWN',
    'REJECTED'
);

CREATE TYPE repayment_plan_type_enum AS ENUM (
    'INSTALLMENT_MONTHLY',
    'INSTALLMENT_QUARTERLY',
    'ONE_TIME'
);

CREATE TYPE settlement_state_enum AS ENUM (
    'SETTLEMENT_PENDING',
    'SETTLEMENT_CONFIRMED',
    'SETTLEMENT_FAILED',
    'SETTLEMENT_EXPIRED',
    'SETTLEMENT_CANCELLED'
);

CREATE TYPE repayment_status_enum AS ENUM (
    'NOT_STARTED',
    'IN_PROGRESS',
    'PAYMENT_ISSUE',
    'OVERDUE',
    'COMPLETED',
    'CANCELLED'
);

CREATE TYPE repayment_installment_status_enum AS ENUM (
    'NOT_STARTED',
    'PAYMENT_IN_PROGRESS',
    'PAYMENT_FAILED',
    'OVERDUE',
    'PAID',
    'CANCELLED'
);

CREATE TYPE payment_purpose_enum AS ENUM (
    'SETTLEMENT',
    'REPAYMENT'
);

CREATE TYPE payment_state_enum AS ENUM (
    'CREATED',
    'PAYMENT_PENDING',
    'PAYMENT_CONFIRMED',
    'PAYMENT_FAILED',
    'PAYMENT_EXPIRED',
    'PAYMENT_CANCELLED'
);

CREATE TYPE payment_method_type_enum AS ENUM (
    'UPI',
    'CARD',
    'NET_BANKING',
    'WALLET',
    'BANK_TRANSFER',
    'OTHER'
);

CREATE TYPE payment_attempt_state_enum AS ENUM (
    'CREATED',
    'INITIATED',
    'REQUIRES_ACTION',
    'CONFIRMED',
    'FAILED',
    'EXPIRED',
    'CANCELLED'
);

CREATE TYPE payment_webhook_processing_state_enum AS ENUM (
    'RECEIVED',
    'PROCESSED',
    'FAILED',
    'IGNORED'
);

CREATE TYPE recipient_delivery_status_enum AS ENUM (
    'PENDING',
    'DELIVERED',
    'PARTIALLY_DELIVERED',
    'FAILED'
);

CREATE TYPE read_status_enum AS ENUM (
    'UNREAD',
    'READ'
);

CREATE TYPE channel_delivery_status_enum AS ENUM (
    'PENDING',
    'ATTEMPTING',
    'DELIVERED',
    'FAILED'
);

CREATE TYPE channel_type_enum AS ENUM (
    'IN_APP',
    'EMAIL',
    'PUSH'
);


-- =========================================================
-- 2. IDENTITY MODULE
-- =========================================================

CREATE TABLE account (
    account_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_state account_state_enum NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    deactivated_at TIMESTAMPTZ NULL,

    CONSTRAINT chk_account_deactivation
    CHECK (
        (account_state = 'DEACTIVATED' AND deactivated_at IS NOT NULL)
        OR
        (account_state <> 'DEACTIVATED' AND deactivated_at IS NULL)
    )
);

CREATE TABLE role (
    role_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL UNIQUE,
    role_type role_type_enum NOT NULL,

    CONSTRAINT fk_role_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT
);

CREATE TABLE profile (
    profile_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL UNIQUE,
    profile_status profile_status_enum NOT NULL,

    CONSTRAINT fk_profile_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT
);


-- =========================================================
-- 3. SECURITY MODULE
-- =========================================================

CREATE TABLE credential (
    credential_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    credential_status credential_status_enum NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    password_updated_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_credential_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT
);

CREATE TABLE session (
    session_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    session_status session_status_enum NOT NULL,

    CONSTRAINT fk_session_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_session_time
    CHECK (expires_at > created_at)
);

CREATE TABLE login_attempt (
    login_attempt_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    email VARCHAR(255) NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason TEXT NULL,
    source_ip TEXT NULL
);


-- =========================================================
-- 4. PARTICIPATION MODULE
-- =========================================================

CREATE TABLE startup (
    startup_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL UNIQUE,
    legal_entity_name VARCHAR(255) NOT NULL,
    incorporation_country_code VARCHAR(10) NOT NULL,
    public_display_name VARCHAR(255) NOT NULL,
    business_description TEXT NULL,

    CONSTRAINT fk_startup_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT
);

CREATE TABLE startup_legal_registration (
    registration_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    startup_id BIGINT NOT NULL,
    registration_type TEXT NOT NULL,
    registration_value TEXT NOT NULL,

    CONSTRAINT fk_startup_registration
    FOREIGN KEY (startup_id) REFERENCES startup(startup_id)
    ON DELETE CASCADE
);

CREATE TABLE startup_web_presence (
    web_presence_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    startup_id BIGINT NOT NULL,
    url TEXT NOT NULL,

    CONSTRAINT fk_startup_web_presence
    FOREIGN KEY (startup_id) REFERENCES startup(startup_id)
    ON DELETE CASCADE
);

CREATE TABLE investor (
    investor_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL UNIQUE,
    public_display_name VARCHAR(255) NOT NULL,
    investor_description TEXT NULL,
    legal_entity_name VARCHAR(255) NULL,

    CONSTRAINT fk_investor_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT
);

CREATE TABLE investor_web_presence (
    web_presence_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    investor_id BIGINT NOT NULL,
    url TEXT NOT NULL,

    CONSTRAINT fk_investor_web_presence
    FOREIGN KEY (investor_id) REFERENCES investor(investor_id)
    ON DELETE CASCADE
);


-- =========================================================
-- 5. GOVERNANCE MODULE - ADMIN AUTHORITY
-- =========================================================

CREATE TABLE admin (
    admin_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL UNIQUE,
    public_display_name VARCHAR(255) NOT NULL,
    organization_label TEXT NULL,
    admin_state admin_state_enum NOT NULL DEFAULT 'ACTIVE',
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ NULL,
    revoked_by_account_id BIGINT NULL,
    revoked_reason TEXT NULL,

    CONSTRAINT fk_admin_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_admin_revoked_by_account
    FOREIGN KEY (revoked_by_account_id) REFERENCES account(account_id)
    ON DELETE SET NULL,

    CONSTRAINT chk_admin_revocation
    CHECK (
        (
            admin_state = 'ACTIVE'
            AND revoked_at IS NULL
            AND revoked_reason IS NULL
        )
        OR
        (
            admin_state = 'REVOKED'
            AND revoked_at IS NOT NULL
        )
    )
);


-- =========================================================
-- 6. CLASSIFICATION MODULE
-- =========================================================

CREATE TABLE startup_classification (
    startup_classification_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    startup_id BIGINT NOT NULL,
    classification_type TEXT NOT NULL,
    classification_value TEXT NOT NULL,

    CONSTRAINT fk_startup_classification
    FOREIGN KEY (startup_id) REFERENCES startup(startup_id)
    ON DELETE CASCADE,

    CONSTRAINT uq_startup_classification
    UNIQUE (startup_id, classification_type, classification_value)
);

CREATE TABLE investor_preference (
    investor_preference_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    investor_id BIGINT NOT NULL,
    preference_type TEXT NOT NULL,
    preference_value TEXT NOT NULL,

    CONSTRAINT fk_investor_preference
    FOREIGN KEY (investor_id) REFERENCES investor(investor_id)
    ON DELETE CASCADE,

    CONSTRAINT uq_investor_preference
    UNIQUE (investor_id, preference_type, preference_value)
);


-- =========================================================
-- 7. MARKETPLACE MODULE
-- =========================================================

CREATE TABLE funding_listing (
    listing_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    startup_id BIGINT NOT NULL,
    listing_state listing_state_enum NOT NULL,
    funding_model funding_model_enum NOT NULL DEFAULT 'DEBT',
    funding_purpose_description TEXT NOT NULL,
    title TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    closed_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_listing_startup
    FOREIGN KEY (startup_id) REFERENCES startup(startup_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_listing_time
    CHECK (
        (published_at IS NULL OR published_at >= created_at)
        AND
        (expires_at IS NULL OR expires_at > created_at)
        AND
        (closed_at IS NULL OR closed_at >= created_at)
        AND
        (published_at IS NULL OR expires_at IS NULL OR expires_at > published_at)
        AND
        (published_at IS NULL OR closed_at IS NULL OR closed_at >= published_at)
    ),

    CONSTRAINT chk_listing_state_timestamp
    CHECK (
        (
            listing_state = 'DRAFT'
            AND published_at IS NULL
            AND expires_at IS NULL
            AND closed_at IS NULL
        )
        OR
        (
            listing_state = 'OPEN'
            AND published_at IS NOT NULL
            AND expires_at IS NOT NULL
            AND closed_at IS NULL
        )
        OR
        (
            listing_state IN ('AGREEMENT_REACHED', 'FUNDED', 'CLOSED')
            AND published_at IS NOT NULL
            AND closed_at IS NOT NULL
        )
    )
);

CREATE TABLE listing_debt_terms (
    listing_debt_terms_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    listing_id BIGINT NOT NULL UNIQUE,
    requested_amount NUMERIC(18,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    minimum_interest_rate NUMERIC(5,2) NULL,
    maximum_interest_rate NUMERIC(5,2) NULL,
    requested_tenure_months INTEGER NULL,
    repayment_plan_type repayment_plan_type_enum NOT NULL DEFAULT 'INSTALLMENT_MONTHLY',
    one_time_repayment_due_after_months INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_listing_debt_terms_listing
    FOREIGN KEY (listing_id) REFERENCES funding_listing(listing_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_listing_debt_amount
    CHECK (requested_amount > 0),

    CONSTRAINT chk_listing_debt_currency
    CHECK (length(trim(currency_code)) > 0),

    CONSTRAINT chk_listing_debt_interest_range
    CHECK (
        minimum_interest_rate IS NULL
        OR maximum_interest_rate IS NULL
        OR minimum_interest_rate <= maximum_interest_rate
    ),

    CONSTRAINT chk_listing_debt_interest_non_negative
    CHECK (
        (minimum_interest_rate IS NULL OR minimum_interest_rate >= 0)
        AND
        (maximum_interest_rate IS NULL OR maximum_interest_rate >= 0)
    ),

    CONSTRAINT chk_listing_debt_tenure
    CHECK (
        requested_tenure_months IS NULL
        OR requested_tenure_months > 0
    ),

    CONSTRAINT chk_listing_debt_repayment_plan
    CHECK (
        (
            repayment_plan_type IN ('INSTALLMENT_MONTHLY', 'INSTALLMENT_QUARTERLY')
            AND one_time_repayment_due_after_months IS NULL
        )
        OR
        (
            repayment_plan_type = 'ONE_TIME'
            AND requested_tenure_months IS NOT NULL
            AND one_time_repayment_due_after_months IS NOT NULL
            AND one_time_repayment_due_after_months > 0
            AND one_time_repayment_due_after_months <= requested_tenure_months
        )
    )
);

CREATE TABLE bid (
    bid_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    listing_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    bid_state bid_state_enum NOT NULL,
    proposal_message TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    withdrawn_at TIMESTAMPTZ NULL,
    rejected_at TIMESTAMPTZ NULL,
    accepted_at TIMESTAMPTZ NULL,
    funded_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_bid_listing
    FOREIGN KEY (listing_id) REFERENCES funding_listing(listing_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_bid_investor
    FOREIGN KEY (investor_id) REFERENCES investor(investor_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_bid_terminal_marker
    CHECK (
        (withdrawn_at IS NOT NULL)::int
        + (rejected_at IS NOT NULL)::int
        + (accepted_at IS NOT NULL)::int
        <= 1
    ),

    CONSTRAINT chk_bid_state_marker
    CHECK (
        (
            bid_state = 'SUBMITTED'
            AND withdrawn_at IS NULL
            AND rejected_at IS NULL
            AND accepted_at IS NULL
            AND funded_at IS NULL
        )
        OR
        (
            bid_state = 'WITHDRAWN'
            AND withdrawn_at IS NOT NULL
            AND rejected_at IS NULL
            AND accepted_at IS NULL
            AND funded_at IS NULL
        )
        OR
        (
            bid_state = 'REJECTED'
            AND rejected_at IS NOT NULL
            AND withdrawn_at IS NULL
            AND accepted_at IS NULL
            AND funded_at IS NULL
        )
        OR
        (
            bid_state IN ('ACCEPTED', 'PENDING_SETTLEMENT')
            AND accepted_at IS NOT NULL
            AND withdrawn_at IS NULL
            AND rejected_at IS NULL
            AND funded_at IS NULL
        )
        OR
        (
            bid_state = 'FUNDED'
            AND accepted_at IS NOT NULL
            AND funded_at IS NOT NULL
            AND withdrawn_at IS NULL
            AND rejected_at IS NULL
        )
    ),

    CONSTRAINT chk_bid_time
    CHECK (
        (withdrawn_at IS NULL OR withdrawn_at >= created_at)
        AND
        (rejected_at IS NULL OR rejected_at >= created_at)
        AND
        (accepted_at IS NULL OR accepted_at >= created_at)
        AND
        (funded_at IS NULL OR funded_at >= accepted_at)
    )
);

CREATE TABLE bid_debt_terms (
    bid_debt_terms_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    bid_id BIGINT NOT NULL UNIQUE,
    proposed_amount NUMERIC(18,2) NOT NULL,
    proposed_interest_rate NUMERIC(5,2) NOT NULL,
    proposed_tenure_months INTEGER NOT NULL,
    repayment_plan_type repayment_plan_type_enum NOT NULL DEFAULT 'INSTALLMENT_MONTHLY',
    one_time_repayment_due_after_months INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_bid_debt_terms_bid
    FOREIGN KEY (bid_id) REFERENCES bid(bid_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_bid_terms_amount
    CHECK (proposed_amount > 0),

    CONSTRAINT chk_bid_terms_interest
    CHECK (proposed_interest_rate >= 0),

    CONSTRAINT chk_bid_terms_tenure
    CHECK (proposed_tenure_months > 0),

    CONSTRAINT chk_bid_terms_repayment_plan
    CHECK (
        (
            repayment_plan_type IN ('INSTALLMENT_MONTHLY', 'INSTALLMENT_QUARTERLY')
            AND one_time_repayment_due_after_months IS NULL
        )
        OR
        (
            repayment_plan_type = 'ONE_TIME'
            AND one_time_repayment_due_after_months IS NOT NULL
            AND one_time_repayment_due_after_months > 0
            AND one_time_repayment_due_after_months <= proposed_tenure_months
        )
    )
);

CREATE TABLE agreement (
    agreement_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    listing_id BIGINT NOT NULL,
    bid_id BIGINT NOT NULL UNIQUE,
    startup_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_agreement_listing
    FOREIGN KEY (listing_id) REFERENCES funding_listing(listing_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_agreement_bid
    FOREIGN KEY (bid_id) REFERENCES bid(bid_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_agreement_startup
    FOREIGN KEY (startup_id) REFERENCES startup(startup_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_agreement_investor
    FOREIGN KEY (investor_id) REFERENCES investor(investor_id)
    ON DELETE RESTRICT
);

CREATE TABLE agreement_debt_terms (
    agreement_debt_terms_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    agreement_id BIGINT NOT NULL UNIQUE,
    principal_amount NUMERIC(18,2) NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    repayment_plan_type repayment_plan_type_enum NOT NULL DEFAULT 'INSTALLMENT_MONTHLY',
    one_time_repayment_due_after_months INTEGER NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_agreement_debt_terms_agreement
    FOREIGN KEY (agreement_id) REFERENCES agreement(agreement_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_agreement_debt_principal
    CHECK (principal_amount > 0),

    CONSTRAINT chk_agreement_debt_interest
    CHECK (interest_rate >= 0),

    CONSTRAINT chk_agreement_debt_tenure
    CHECK (tenure_months > 0),

    CONSTRAINT chk_agreement_debt_repayment_plan
    CHECK (
        (
            repayment_plan_type IN ('INSTALLMENT_MONTHLY', 'INSTALLMENT_QUARTERLY')
            AND one_time_repayment_due_after_months IS NULL
        )
        OR
        (
            repayment_plan_type = 'ONE_TIME'
            AND one_time_repayment_due_after_months IS NOT NULL
            AND one_time_repayment_due_after_months > 0
            AND one_time_repayment_due_after_months <= tenure_months
        )
    )
);


-- =========================================================
-- 8. FINANCE MODULE
-- =========================================================

CREATE TABLE settlement (
    settlement_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    agreement_id BIGINT NOT NULL UNIQUE,
    settlement_state settlement_state_enum NOT NULL,
    startup_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ NULL,
    failed_at TIMESTAMPTZ NULL,
    expired_at TIMESTAMPTZ NULL,
    cancelled_at TIMESTAMPTZ NULL,
    failure_reason TEXT NULL,
    confirmed_payment_intent_id BIGINT NULL,
    psp_reference_id TEXT NULL,

    CONSTRAINT fk_settlement_agreement
    FOREIGN KEY (agreement_id) REFERENCES agreement(agreement_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_settlement_startup
    FOREIGN KEY (startup_id) REFERENCES startup(startup_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_settlement_investor
    FOREIGN KEY (investor_id) REFERENCES investor(investor_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_settlement_amount
    CHECK (amount > 0),

    CONSTRAINT chk_settlement_currency
    CHECK (length(trim(currency_code)) > 0),

    CONSTRAINT chk_settlement_time
    CHECK (
        expires_at > created_at
        AND (confirmed_at IS NULL OR confirmed_at >= created_at)
        AND (failed_at IS NULL OR failed_at >= created_at)
        AND (expired_at IS NULL OR expired_at >= created_at)
        AND (cancelled_at IS NULL OR cancelled_at >= created_at)
    ),

    CONSTRAINT chk_settlement_state_timestamp
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
    )
);

CREATE TABLE repayment (
    repayment_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    agreement_id BIGINT NOT NULL UNIQUE,
    startup_id BIGINT NOT NULL,
    investor_id BIGINT NOT NULL,
    total_repayable_amount NUMERIC(18,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    total_installments INTEGER NOT NULL,
    repayment_plan_type repayment_plan_type_enum NOT NULL,
    repayment_status repayment_status_enum NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    final_due_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NULL,
    cancelled_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_repayment_agreement
    FOREIGN KEY (agreement_id) REFERENCES agreement(agreement_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_repayment_startup
    FOREIGN KEY (startup_id) REFERENCES startup(startup_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_repayment_investor
    FOREIGN KEY (investor_id) REFERENCES investor(investor_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_repayment_total_amount
    CHECK (total_repayable_amount > 0),

    CONSTRAINT chk_repayment_currency
    CHECK (length(trim(currency_code)) > 0),

    CONSTRAINT chk_repayment_total_installments
    CHECK (total_installments > 0),

    CONSTRAINT chk_repayment_time
    CHECK (
        started_at >= created_at
        AND final_due_at >= started_at
        AND updated_at >= created_at
        AND (completed_at IS NULL OR completed_at >= created_at)
        AND (cancelled_at IS NULL OR cancelled_at >= created_at)
    ),

    CONSTRAINT chk_repayment_status_timestamp
    CHECK (
        (
            repayment_status IN ('NOT_STARTED', 'IN_PROGRESS', 'PAYMENT_ISSUE', 'OVERDUE')
            AND completed_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            repayment_status = 'COMPLETED'
            AND completed_at IS NOT NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            repayment_status = 'CANCELLED'
            AND cancelled_at IS NOT NULL
            AND completed_at IS NULL
        )
    )
);

CREATE TABLE repayment_installment (
    repayment_installment_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    repayment_id BIGINT NOT NULL,
    installment_number INTEGER NOT NULL,
    installment_status repayment_installment_status_enum NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    payment_started_at TIMESTAMPTZ NULL,
    paid_at TIMESTAMPTZ NULL,
    failed_at TIMESTAMPTZ NULL,
    overdue_at TIMESTAMPTZ NULL,
    cancelled_at TIMESTAMPTZ NULL,
    failure_reason TEXT NULL,
    confirmed_payment_intent_id BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_repayment_installment_repayment
    FOREIGN KEY (repayment_id) REFERENCES repayment(repayment_id)
    ON DELETE RESTRICT,

    CONSTRAINT uq_repayment_installment_number
    UNIQUE (repayment_id, installment_number),

    CONSTRAINT chk_repayment_installment_number
    CHECK (installment_number > 0),

    CONSTRAINT chk_repayment_installment_amount
    CHECK (amount > 0),

    CONSTRAINT chk_repayment_installment_currency
    CHECK (length(trim(currency_code)) > 0),

    CONSTRAINT chk_repayment_installment_time
    CHECK (
        due_at >= created_at
        AND updated_at >= created_at
        AND (payment_started_at IS NULL OR payment_started_at >= created_at)
        AND (paid_at IS NULL OR paid_at >= created_at)
        AND (failed_at IS NULL OR failed_at >= created_at)
        AND (overdue_at IS NULL OR overdue_at >= created_at)
        AND (cancelled_at IS NULL OR cancelled_at >= created_at)
    ),

    CONSTRAINT chk_repayment_installment_status_timestamp
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
    )
);

CREATE TABLE payment_provider (
    provider_code VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NULL
);

CREATE TABLE payment_provider_method (
    provider_code VARCHAR(50) NOT NULL,
    method_type payment_method_type_enum NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    min_amount NUMERIC(18,2) NULL,
    max_amount NUMERIC(18,2) NULL,

    PRIMARY KEY (provider_code, method_type, currency_code),

    CONSTRAINT fk_payment_provider_method_provider
    FOREIGN KEY (provider_code) REFERENCES payment_provider(provider_code)
    ON DELETE RESTRICT,

    CONSTRAINT chk_payment_provider_method_amount
    CHECK (
        (min_amount IS NULL OR min_amount > 0)
        AND
        (max_amount IS NULL OR max_amount > 0)
        AND
        (min_amount IS NULL OR max_amount IS NULL OR max_amount >= min_amount)
    )
);

CREATE TABLE payment_intent (
    payment_intent_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_purpose payment_purpose_enum NOT NULL,
    settlement_id BIGINT NULL,
    repayment_installment_id BIGINT NULL,
    payer_account_id BIGINT NOT NULL,
    payee_account_id BIGINT NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    payment_state payment_state_enum NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ NULL,
    failed_at TIMESTAMPTZ NULL,
    expired_at TIMESTAMPTZ NULL,
    cancelled_at TIMESTAMPTZ NULL,
    failure_code VARCHAR(100) NULL,
    failure_message TEXT NULL,

    CONSTRAINT fk_payment_intent_settlement
    FOREIGN KEY (settlement_id) REFERENCES settlement(settlement_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_intent_repayment_installment
    FOREIGN KEY (repayment_installment_id) REFERENCES repayment_installment(repayment_installment_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_intent_payer_account
    FOREIGN KEY (payer_account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_intent_payee_account
    FOREIGN KEY (payee_account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_payment_intent_amount
    CHECK (amount > 0),

    CONSTRAINT chk_payment_intent_currency
    CHECK (length(trim(currency_code)) > 0),

    CONSTRAINT chk_payment_intent_participant
    CHECK (payer_account_id <> payee_account_id),

    CONSTRAINT chk_payment_intent_purpose_reference
    CHECK (
        (
            payment_purpose = 'SETTLEMENT'
            AND settlement_id IS NOT NULL
            AND repayment_installment_id IS NULL
        )
        OR
        (
            payment_purpose = 'REPAYMENT'
            AND repayment_installment_id IS NOT NULL
            AND settlement_id IS NULL
        )
    ),

    CONSTRAINT chk_payment_intent_time
    CHECK (
        expires_at > created_at
        AND (confirmed_at IS NULL OR confirmed_at >= created_at)
        AND (failed_at IS NULL OR failed_at >= created_at)
        AND (expired_at IS NULL OR expired_at >= created_at)
        AND (cancelled_at IS NULL OR cancelled_at >= created_at)
    ),

    CONSTRAINT chk_payment_intent_state_timestamp
    CHECK (
        (
            payment_state IN ('CREATED', 'PAYMENT_PENDING')
            AND confirmed_at IS NULL
            AND failed_at IS NULL
            AND expired_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            payment_state = 'PAYMENT_CONFIRMED'
            AND confirmed_at IS NOT NULL
            AND failed_at IS NULL
            AND expired_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            payment_state = 'PAYMENT_FAILED'
            AND failed_at IS NOT NULL
            AND confirmed_at IS NULL
            AND expired_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            payment_state = 'PAYMENT_EXPIRED'
            AND expired_at IS NOT NULL
            AND confirmed_at IS NULL
            AND failed_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            payment_state = 'PAYMENT_CANCELLED'
            AND cancelled_at IS NOT NULL
            AND confirmed_at IS NULL
            AND failed_at IS NULL
            AND expired_at IS NULL
        )
    )
);

CREATE TABLE payment_attempt (
    payment_attempt_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    payment_intent_id BIGINT NOT NULL,
    provider_code VARCHAR(50) NOT NULL,
    method_type payment_method_type_enum NOT NULL,
    provider_order_id TEXT NULL,
    provider_payment_id TEXT NULL,
    provider_reference_id TEXT NULL,
    attempt_state payment_attempt_state_enum NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    initiated_at TIMESTAMPTZ NULL,
    confirmed_at TIMESTAMPTZ NULL,
    failed_at TIMESTAMPTZ NULL,
    expired_at TIMESTAMPTZ NULL,
    cancelled_at TIMESTAMPTZ NULL,
    failure_code VARCHAR(100) NULL,
    failure_message TEXT NULL,
    provider_payload JSONB NULL,

    CONSTRAINT fk_payment_attempt_intent
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intent(payment_intent_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_attempt_provider
    FOREIGN KEY (provider_code) REFERENCES payment_provider(provider_code)
    ON DELETE RESTRICT,

    CONSTRAINT chk_payment_attempt_time
    CHECK (
        (initiated_at IS NULL OR initiated_at >= created_at)
        AND (confirmed_at IS NULL OR confirmed_at >= created_at)
        AND (failed_at IS NULL OR failed_at >= created_at)
        AND (expired_at IS NULL OR expired_at >= created_at)
        AND (cancelled_at IS NULL OR cancelled_at >= created_at)
    ),

    CONSTRAINT chk_payment_attempt_state_timestamp
    CHECK (
        (
            attempt_state IN ('CREATED', 'INITIATED', 'REQUIRES_ACTION')
            AND confirmed_at IS NULL
            AND failed_at IS NULL
            AND expired_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            attempt_state = 'CONFIRMED'
            AND confirmed_at IS NOT NULL
            AND failed_at IS NULL
            AND expired_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            attempt_state = 'FAILED'
            AND failed_at IS NOT NULL
            AND confirmed_at IS NULL
            AND expired_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            attempt_state = 'EXPIRED'
            AND expired_at IS NOT NULL
            AND confirmed_at IS NULL
            AND failed_at IS NULL
            AND cancelled_at IS NULL
        )
        OR
        (
            attempt_state = 'CANCELLED'
            AND cancelled_at IS NOT NULL
            AND confirmed_at IS NULL
            AND failed_at IS NULL
            AND expired_at IS NULL
        )
    )
);

CREATE TABLE payment_webhook_event (
    payment_webhook_event_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    provider_code VARCHAR(50) NOT NULL,
    provider_event_id TEXT NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payment_intent_id BIGINT NULL,
    payment_attempt_id BIGINT NULL,
    processing_state payment_webhook_processing_state_enum NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ NULL,
    payload_hash VARCHAR(128) NULL,
    payload JSONB NOT NULL,
    failure_message TEXT NULL,

    CONSTRAINT fk_payment_webhook_provider
    FOREIGN KEY (provider_code) REFERENCES payment_provider(provider_code)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_webhook_intent
    FOREIGN KEY (payment_intent_id) REFERENCES payment_intent(payment_intent_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_payment_webhook_attempt
    FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt(payment_attempt_id)
    ON DELETE RESTRICT,

    CONSTRAINT uq_payment_webhook_provider_event
    UNIQUE (provider_code, provider_event_id),

    CONSTRAINT chk_payment_webhook_processed_at
    CHECK (
        (processing_state IN ('RECEIVED', 'FAILED', 'IGNORED') AND processed_at IS NULL)
        OR
        (processing_state = 'PROCESSED' AND processed_at IS NOT NULL)
    )
);

-- =========================================================
-- 9. COMMON OUTBOX
-- =========================================================

CREATE TABLE event_outbox (
    outbox_event_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    event_id TEXT NOT NULL UNIQUE,
    event_type TEXT NOT NULL,
    source_module TEXT NOT NULL,
    aggregate_type TEXT NULL,
    aggregate_id TEXT NULL,
    payload JSONB NOT NULL,
    event_status TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT NULL,
    locked_at TIMESTAMPTZ NULL,
    locked_by TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_event_outbox_status
    CHECK (event_status IN ('PENDING', 'PROCESSED', 'FAILED')),

    CONSTRAINT chk_event_outbox_retry_count
    CHECK (retry_count >= 0),

    CONSTRAINT chk_event_outbox_processed_at
    CHECK (
        (event_status <> 'PROCESSED' AND processed_at IS NULL)
        OR
        (event_status = 'PROCESSED' AND processed_at IS NOT NULL)
    )
);


-- =========================================================
-- 10. NOTIFICATION MODULE
-- =========================================================

CREATE TABLE notification (
    notification_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    event_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    notification_name TEXT NOT NULL,
    notification_type TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_notification_idempotency
    UNIQUE (event_id, notification_name)
);

CREATE TABLE notification_recipient (
    recipient_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    notification_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    recipient_delivery_status recipient_delivery_status_enum NOT NULL,
    read_status read_status_enum NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ NULL,
    read_at TIMESTAMPTZ NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_recipient_notification
    FOREIGN KEY (notification_id) REFERENCES notification(notification_id)
    ON DELETE CASCADE,

    CONSTRAINT fk_recipient_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_recipient_delete_consistency
    CHECK (
        (is_deleted = TRUE AND deleted_at IS NOT NULL)
        OR
        (is_deleted = FALSE AND deleted_at IS NULL)
    ),

    CONSTRAINT uq_notification_recipient_account
    UNIQUE (notification_id, account_id)
);

CREATE TABLE notification_delivery (
    delivery_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    recipient_id BIGINT NOT NULL,
    channel_type channel_type_enum NOT NULL,
    channel_delivery_status channel_delivery_status_enum NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NULL,
    last_attempt_at TIMESTAMPTZ NULL,
    delivered_at TIMESTAMPTZ NULL,
    failed_at TIMESTAMPTZ NULL,
    provider_message_id TEXT NULL,
    failure_reason TEXT NULL,
    locked_at TIMESTAMPTZ NULL,
    locked_by TEXT NULL,

    CONSTRAINT fk_delivery_recipient
    FOREIGN KEY (recipient_id) REFERENCES notification_recipient(recipient_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_delivery_attempt_count
    CHECK (attempt_count >= 0),

    CONSTRAINT uq_notification_delivery_channel
    UNIQUE (recipient_id, channel_type)
);

CREATE TABLE notification_delivery_attempt (
    delivery_attempt_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    delivery_id BIGINT NOT NULL,
    attempt_number INTEGER NOT NULL,
    attempt_status TEXT NOT NULL,
    provider_message_id TEXT NULL,
    error_code TEXT NULL,
    error_message TEXT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,
    duration_ms BIGINT NULL,

    CONSTRAINT fk_notification_delivery_attempt_delivery
    FOREIGN KEY (delivery_id) REFERENCES notification_delivery(delivery_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_notification_delivery_attempt_number
    CHECK (attempt_number > 0),

    CONSTRAINT chk_notification_delivery_attempt_status
    CHECK (attempt_status IN ('ATTEMPTING', 'DELIVERED', 'FAILED')),

    CONSTRAINT uq_notification_delivery_attempt_number
    UNIQUE (delivery_id, attempt_number)
);

CREATE TABLE notification_subscription (
    subscription_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id BIGINT NOT NULL,
    channel_type channel_type_enum NOT NULL,
    endpoint TEXT NOT NULL,
    public_key TEXT NULL,
    auth_secret TEXT NULL,
    subscription_state TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,

    CONSTRAINT fk_notification_subscription_account
    FOREIGN KEY (account_id) REFERENCES account(account_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_notification_subscription_state
    CHECK (subscription_state IN ('ACTIVE', 'REVOKED')),

    CONSTRAINT chk_notification_subscription_revoked_at
    CHECK (
        (subscription_state = 'ACTIVE' AND revoked_at IS NULL)
        OR
        (subscription_state = 'REVOKED' AND revoked_at IS NOT NULL)
    ),

    CONSTRAINT uq_notification_subscription_endpoint
    UNIQUE (account_id, channel_type, endpoint)
);


-- =========================================================
-- 11. COMMON AUDIT
-- =========================================================

CREATE TABLE audit_record (
    audit_record_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    event_id TEXT NULL,
    event_type TEXT NOT NULL,
    source_module TEXT NOT NULL,
    action TEXT NOT NULL,
    object_type TEXT NOT NULL,
    object_id TEXT NOT NULL,
    actor_account_id BIGINT NULL,
    actor_role TEXT NULL,
    outcome TEXT NOT NULL,
    request_id TEXT NULL,
    ip_address TEXT NULL,
    user_agent TEXT NULL,
    details JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_audit_account
    FOREIGN KEY (actor_account_id) REFERENCES account(account_id)
    ON DELETE SET NULL,

    CONSTRAINT chk_audit_outcome
    CHECK (outcome IN ('SUCCESS', 'FAILED', 'DENIED', 'SYSTEM')),

    CONSTRAINT chk_audit_time
    CHECK (recorded_at >= occurred_at),

    CONSTRAINT uq_audit_event_action
    UNIQUE (event_id, action)
);


-- =========================================================
-- 11. DATA INTEGRITY FUNCTIONS AND TRIGGERS
-- =========================================================

CREATE OR REPLACE FUNCTION enforce_actor_consistency()
RETURNS TRIGGER AS $$
DECLARE
    account_role role_type_enum;
BEGIN
    SELECT role_type
    INTO account_role
    FROM role
    WHERE account_id = NEW.account_id;

    IF TG_TABLE_NAME = 'startup' AND account_role <> 'STARTUP' THEN
        RAISE EXCEPTION 'Role mismatch: account is not Startup';
    END IF;

    IF TG_TABLE_NAME = 'investor' AND account_role <> 'INVESTOR' THEN
        RAISE EXCEPTION 'Role mismatch: account is not Investor';
    END IF;

    IF TG_TABLE_NAME = 'admin' AND account_role <> 'ADMIN' THEN
        RAISE EXCEPTION 'Role mismatch: account is not Admin';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_startup_role_check
BEFORE INSERT ON startup
FOR EACH ROW
EXECUTE FUNCTION enforce_actor_consistency();

CREATE TRIGGER trg_investor_role_check
BEFORE INSERT ON investor
FOR EACH ROW
EXECUTE FUNCTION enforce_actor_consistency();

CREATE TRIGGER trg_admin_role_check
BEFORE INSERT ON admin
FOR EACH ROW
EXECUTE FUNCTION enforce_actor_consistency();

CREATE OR REPLACE FUNCTION prevent_credential_email_update()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.email <> OLD.email THEN
        RAISE EXCEPTION 'Email cannot be modified';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_credential_email_immutable
BEFORE UPDATE ON credential
FOR EACH ROW
EXECUTE FUNCTION prevent_credential_email_update();

CREATE OR REPLACE FUNCTION prevent_login_attempt_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Login attempt records are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_login_attempt_no_update
BEFORE UPDATE ON login_attempt
FOR EACH ROW
EXECUTE FUNCTION prevent_login_attempt_modification();

CREATE TRIGGER trg_login_attempt_no_delete
BEFORE DELETE ON login_attempt
FOR EACH ROW
EXECUTE FUNCTION prevent_login_attempt_modification();

CREATE OR REPLACE FUNCTION enforce_agreement_from_accepted_bid()
RETURNS TRIGGER AS $$
DECLARE
    selected_bid_state bid_state_enum;
BEGIN
    SELECT bid_state
    INTO selected_bid_state
    FROM bid
    WHERE bid_id = NEW.bid_id;

    IF selected_bid_state <> 'ACCEPTED' THEN
        RAISE EXCEPTION 'Agreement can only be created from an accepted bid';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_agreement_bid_validation
BEFORE INSERT ON agreement
FOR EACH ROW
EXECUTE FUNCTION enforce_agreement_from_accepted_bid();

CREATE OR REPLACE FUNCTION enforce_agreement_participants()
RETURNS TRIGGER AS $$
DECLARE
    selected_listing_id BIGINT;
    selected_investor_id BIGINT;
    selected_startup_id BIGINT;
BEGIN
    SELECT listing_id, investor_id
    INTO selected_listing_id, selected_investor_id
    FROM bid
    WHERE bid_id = NEW.bid_id;

    SELECT startup_id
    INTO selected_startup_id
    FROM funding_listing
    WHERE listing_id = selected_listing_id;

    IF NEW.listing_id <> selected_listing_id THEN
        RAISE EXCEPTION 'Listing mismatch with bid';
    END IF;

    IF NEW.investor_id <> selected_investor_id THEN
        RAISE EXCEPTION 'Investor mismatch with bid';
    END IF;

    IF NEW.startup_id <> selected_startup_id THEN
        RAISE EXCEPTION 'Startup mismatch with listing';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_agreement_participant_validation
BEFORE INSERT ON agreement
FOR EACH ROW
EXECUTE FUNCTION enforce_agreement_participants();

CREATE OR REPLACE FUNCTION enforce_settlement_participants()
RETURNS TRIGGER AS $$
DECLARE
    selected_startup_id BIGINT;
    selected_investor_id BIGINT;
BEGIN
    SELECT startup_id, investor_id
    INTO selected_startup_id, selected_investor_id
    FROM agreement
    WHERE agreement_id = NEW.agreement_id;

    IF NEW.startup_id <> selected_startup_id THEN
        RAISE EXCEPTION 'Startup mismatch with agreement';
    END IF;

    IF NEW.investor_id <> selected_investor_id THEN
        RAISE EXCEPTION 'Investor mismatch with agreement';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_settlement_participant_validation
BEFORE INSERT ON settlement
FOR EACH ROW
EXECUTE FUNCTION enforce_settlement_participants();

CREATE OR REPLACE FUNCTION enforce_repayment_participants()
RETURNS TRIGGER AS $$
DECLARE
    selected_startup_id BIGINT;
    selected_investor_id BIGINT;
BEGIN
    SELECT startup_id, investor_id
    INTO selected_startup_id, selected_investor_id
    FROM agreement
    WHERE agreement_id = NEW.agreement_id;

    IF NEW.startup_id <> selected_startup_id THEN
        RAISE EXCEPTION 'Startup mismatch with agreement';
    END IF;

    IF NEW.investor_id <> selected_investor_id THEN
        RAISE EXCEPTION 'Investor mismatch with agreement';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_repayment_participant_validation
BEFORE INSERT ON repayment
FOR EACH ROW
EXECUTE FUNCTION enforce_repayment_participants();

CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit records are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_no_update
BEFORE UPDATE ON audit_record
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER trg_audit_no_delete
BEFORE DELETE ON audit_record
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_modification();


-- =========================================================
-- 12. INDEXES
-- =========================================================

CREATE INDEX idx_session_account_status
ON session(account_id, session_status, expires_at);

CREATE INDEX idx_login_attempt_email_time
ON login_attempt(lower(email), attempted_at DESC);

CREATE UNIQUE INDEX uq_single_active_admin
ON admin(admin_state)
WHERE admin_state = 'ACTIVE';

CREATE INDEX idx_startup_account
ON startup(account_id);

CREATE INDEX idx_investor_account
ON investor(account_id);

CREATE INDEX idx_startup_classification_lookup
ON startup_classification(classification_type, classification_value, startup_id);

CREATE INDEX idx_investor_preference_lookup
ON investor_preference(preference_type, preference_value, investor_id);

CREATE INDEX idx_listing_startup_state_created
ON funding_listing(startup_id, listing_state, created_at DESC);

CREATE INDEX idx_funding_listing_model_state
ON funding_listing(funding_model, listing_state);

CREATE INDEX idx_listing_open_expiry
ON funding_listing(expires_at, listing_id)
WHERE listing_state = 'OPEN';

CREATE INDEX idx_listing_open_published
ON funding_listing(published_at DESC)
WHERE listing_state = 'OPEN';

CREATE INDEX idx_listing_model_open_published
ON funding_listing(funding_model, published_at DESC)
WHERE listing_state = 'OPEN';

CREATE INDEX idx_listing_debt_terms_listing
ON listing_debt_terms(listing_id);

CREATE INDEX idx_listing_debt_terms_currency_amount
ON listing_debt_terms(currency_code, requested_amount, listing_id);

CREATE INDEX idx_bid_listing_state_created
ON bid(listing_id, bid_state, created_at DESC);

CREATE INDEX idx_bid_investor_created
ON bid(investor_id, created_at DESC);

CREATE UNIQUE INDEX uq_one_accepted_bid_per_listing
ON bid(listing_id)
WHERE bid_state = 'ACCEPTED';

CREATE INDEX idx_bid_debt_terms_bid
ON bid_debt_terms(bid_id);

CREATE INDEX idx_agreement_listing
ON agreement(listing_id);

CREATE INDEX idx_agreement_startup_created
ON agreement(startup_id, created_at DESC);

CREATE INDEX idx_agreement_investor_created
ON agreement(investor_id, created_at DESC);

CREATE INDEX idx_agreement_debt_terms_agreement
ON agreement_debt_terms(agreement_id);

CREATE INDEX idx_settlement_startup_created
ON settlement(startup_id, created_at DESC);

CREATE INDEX idx_settlement_investor_created
ON settlement(investor_id, created_at DESC);

CREATE INDEX idx_settlement_pending_expiry_id
ON settlement(expires_at, settlement_id)
WHERE settlement_state = 'SETTLEMENT_PENDING';

CREATE INDEX idx_repayment_startup_created
ON repayment(startup_id, created_at DESC);

CREATE INDEX idx_repayment_investor_created
ON repayment(investor_id, created_at DESC);

CREATE INDEX idx_repayment_agreement
ON repayment(agreement_id);

CREATE INDEX idx_repayment_installment_repayment_due
ON repayment_installment(repayment_id, due_at, installment_number);

CREATE INDEX idx_repayment_installment_status_due
ON repayment_installment(installment_status, due_at, repayment_installment_id);

CREATE INDEX idx_repayment_installment_overdue_scan
ON repayment_installment(due_at, repayment_installment_id)
WHERE installment_status IN ('NOT_STARTED', 'PAYMENT_IN_PROGRESS', 'PAYMENT_FAILED');

CREATE UNIQUE INDEX uq_active_payment_intent_per_settlement
ON payment_intent(settlement_id)
WHERE payment_purpose = 'SETTLEMENT'
  AND payment_state IN ('CREATED', 'PAYMENT_PENDING');

CREATE UNIQUE INDEX uq_active_payment_intent_per_repayment_installment
ON payment_intent(repayment_installment_id)
WHERE payment_purpose = 'REPAYMENT'
  AND payment_state IN ('CREATED', 'PAYMENT_PENDING');

CREATE UNIQUE INDEX uq_confirmed_payment_intent_per_settlement
ON payment_intent(settlement_id)
WHERE payment_purpose = 'SETTLEMENT'
  AND payment_state = 'PAYMENT_CONFIRMED';

CREATE UNIQUE INDEX uq_confirmed_payment_intent_per_repayment_installment
ON payment_intent(repayment_installment_id)
WHERE payment_purpose = 'REPAYMENT'
  AND payment_state = 'PAYMENT_CONFIRMED';

CREATE INDEX idx_payment_intent_active_expiry_id
ON payment_intent(expires_at, payment_intent_id)
WHERE payment_state IN ('CREATED', 'PAYMENT_PENDING');

CREATE INDEX idx_payment_attempt_intent
ON payment_attempt(payment_intent_id);

CREATE INDEX idx_payment_attempt_provider_method
ON payment_attempt(provider_code, method_type);

CREATE UNIQUE INDEX uq_payment_attempt_provider_order
ON payment_attempt(provider_code, provider_order_id)
WHERE provider_order_id IS NOT NULL;

CREATE UNIQUE INDEX uq_payment_attempt_provider_payment
ON payment_attempt(provider_code, provider_payment_id)
WHERE provider_payment_id IS NOT NULL;

CREATE UNIQUE INDEX uq_payment_attempt_provider_reference
ON payment_attempt(provider_code, provider_reference_id)
WHERE provider_reference_id IS NOT NULL;

CREATE INDEX idx_payment_webhook_processing_state
ON payment_webhook_event(processing_state, received_at);

CREATE INDEX idx_event_outbox_dispatch
ON event_outbox(event_status, available_at, outbox_event_id)
WHERE event_status = 'PENDING';

CREATE INDEX idx_event_outbox_aggregate
ON event_outbox(source_module, aggregate_type, aggregate_id, occurred_at DESC);

CREATE INDEX idx_notification_feed
ON notification_recipient(account_id, occurred_at DESC)
WHERE is_deleted = FALSE;

CREATE INDEX idx_notification_delivery_recipient
ON notification_delivery(recipient_id);

CREATE INDEX idx_notification_delivery_dispatch
ON notification_delivery(channel_delivery_status, next_attempt_at, delivery_id)
WHERE channel_delivery_status IN ('PENDING', 'FAILED');

CREATE INDEX idx_notification_delivery_attempt_delivery
ON notification_delivery_attempt(delivery_id, attempted_at DESC);

CREATE INDEX idx_notification_subscription_account
ON notification_subscription(account_id, channel_type, subscription_state);

CREATE INDEX idx_audit_object
ON audit_record(object_type, object_id, recorded_at DESC);

CREATE INDEX idx_audit_actor_time
ON audit_record(actor_account_id, recorded_at DESC);

CREATE INDEX idx_audit_module_action_time
ON audit_record(source_module, action, recorded_at DESC);

CREATE INDEX idx_audit_request_id
ON audit_record(request_id)
WHERE request_id IS NOT NULL;


-- =========================================================
-- 13. REFERENCE DATA
-- =========================================================

INSERT INTO payment_provider(provider_code, display_name, enabled)
VALUES
    ('LOCAL', 'Local Test Payment', true),
    ('RAZORPAY', 'Razorpay', true),
    ('CASHFREE', 'Cashfree', true),
    ('STRIPE', 'Stripe', false);

INSERT INTO payment_provider_method(provider_code, method_type, currency_code, enabled)
VALUES
    ('LOCAL', 'OTHER', 'INR', true),
    ('RAZORPAY', 'UPI', 'INR', true),
    ('RAZORPAY', 'CARD', 'INR', true),
    ('CASHFREE', 'UPI', 'INR', true),
    ('CASHFREE', 'CARD', 'INR', true),
    ('STRIPE', 'CARD', 'INR', false);


-- =========================================================
-- 14. DATABASE COMMENTS
-- =========================================================

COMMENT ON COLUMN settlement.confirmed_payment_intent_id IS
'Audit/reference pointer to the confirmed payment intent. payment_intent owns the relationship to settlement.';

COMMENT ON TABLE repayment IS
'Overall repayment obligation for one accepted agreement. Installment-level payment tracking lives in repayment_installment.';

COMMENT ON TABLE repayment_installment IS
'Individual scheduled repayment installment. Overdue installments remain payable until paid or cancelled.';

COMMENT ON COLUMN repayment_installment.confirmed_payment_intent_id IS
'Audit/reference pointer to the confirmed payment intent. payment_intent owns the relationship to repayment_installment.';

COMMENT ON TABLE payment_webhook_event IS
'Stores external payment service provider webhook events with provider-level idempotency.';
