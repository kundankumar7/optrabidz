CREATE UNIQUE INDEX IF NOT EXISTS uq_active_payment_intent_per_settlement
ON payment_intent(settlement_id)
WHERE payment_purpose = 'SETTLEMENT'
  AND payment_state IN ('CREATED', 'PAYMENT_PENDING');

CREATE UNIQUE INDEX IF NOT EXISTS uq_active_payment_intent_per_repayment_installment
ON payment_intent(repayment_installment_id)
WHERE payment_purpose = 'REPAYMENT'
  AND payment_state IN ('CREATED', 'PAYMENT_PENDING');
