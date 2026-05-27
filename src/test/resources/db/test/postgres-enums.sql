DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_state_enum') THEN
        CREATE TYPE account_state_enum AS ENUM ('CREATED', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'role_type_enum') THEN
        CREATE TYPE role_type_enum AS ENUM ('STARTUP', 'INVESTOR', 'ADMIN');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'profile_status_enum') THEN
        CREATE TYPE profile_status_enum AS ENUM ('INCOMPLETE', 'COMPLETE');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'credential_status_enum') THEN
        CREATE TYPE credential_status_enum AS ENUM ('CREATED', 'ACTIVE', 'LOCKED', 'DISABLED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'session_status_enum') THEN
        CREATE TYPE session_status_enum AS ENUM ('CREATED', 'ACTIVE', 'EXPIRED', 'TERMINATED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'listing_state_enum') THEN
        CREATE TYPE listing_state_enum AS ENUM ('DRAFT', 'OPEN', 'AGREEMENT_REACHED', 'FUNDED', 'CLOSED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'bid_state_enum') THEN
        CREATE TYPE bid_state_enum AS ENUM ('SUBMITTED', 'ACCEPTED', 'PENDING_SETTLEMENT', 'FUNDED', 'WITHDRAWN', 'REJECTED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'funding_model_enum') THEN
        CREATE TYPE funding_model_enum AS ENUM ('DEBT', 'EQUITY', 'HYBRID');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'repayment_plan_type_enum') THEN
        CREATE TYPE repayment_plan_type_enum AS ENUM ('INSTALLMENT_MONTHLY', 'INSTALLMENT_QUARTERLY', 'ONE_TIME');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'settlement_state_enum') THEN
        CREATE TYPE settlement_state_enum AS ENUM (
            'SETTLEMENT_PENDING',
            'SETTLEMENT_CONFIRMED',
            'SETTLEMENT_FAILED',
            'SETTLEMENT_EXPIRED',
            'SETTLEMENT_CANCELLED'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'repayment_status_enum') THEN
        CREATE TYPE repayment_status_enum AS ENUM (
            'NOT_STARTED',
            'IN_PROGRESS',
            'PAYMENT_ISSUE',
            'OVERDUE',
            'COMPLETED',
            'CANCELLED'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'repayment_installment_status_enum') THEN
        CREATE TYPE repayment_installment_status_enum AS ENUM (
            'NOT_STARTED',
            'PAYMENT_IN_PROGRESS',
            'PAYMENT_FAILED',
            'OVERDUE',
            'PAID',
            'CANCELLED'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_purpose_enum') THEN
        CREATE TYPE payment_purpose_enum AS ENUM ('SETTLEMENT', 'REPAYMENT');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_state_enum') THEN
        CREATE TYPE payment_state_enum AS ENUM (
            'CREATED',
            'PAYMENT_PENDING',
            'PAYMENT_CONFIRMED',
            'PAYMENT_FAILED',
            'PAYMENT_EXPIRED',
            'PAYMENT_CANCELLED'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_type_enum') THEN
        CREATE TYPE payment_method_type_enum AS ENUM ('UPI', 'CARD', 'NET_BANKING', 'WALLET', 'BANK_TRANSFER', 'OTHER');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_attempt_state_enum') THEN
        CREATE TYPE payment_attempt_state_enum AS ENUM (
            'CREATED',
            'INITIATED',
            'REQUIRES_ACTION',
            'CONFIRMED',
            'FAILED',
            'EXPIRED',
            'CANCELLED'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_webhook_processing_state_enum') THEN
        CREATE TYPE payment_webhook_processing_state_enum AS ENUM ('RECEIVED', 'PROCESSED', 'FAILED', 'IGNORED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'admin_state_enum') THEN
        CREATE TYPE admin_state_enum AS ENUM ('ACTIVE', 'REVOKED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'recipient_delivery_status_enum') THEN
        CREATE TYPE recipient_delivery_status_enum AS ENUM ('PENDING', 'DELIVERED', 'PARTIALLY_DELIVERED', 'FAILED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'read_status_enum') THEN
        CREATE TYPE read_status_enum AS ENUM ('UNREAD', 'READ');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'channel_delivery_status_enum') THEN
        CREATE TYPE channel_delivery_status_enum AS ENUM ('PENDING', 'ATTEMPTING', 'DELIVERED', 'FAILED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'channel_type_enum') THEN
        CREATE TYPE channel_type_enum AS ENUM ('IN_APP', 'EMAIL', 'PUSH');
    END IF;
END
$$@@
