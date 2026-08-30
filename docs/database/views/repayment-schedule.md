# Repayment Schedule

[Back to focused views](README.md)

<a href="../assets/repayment-schedule-schema.svg"><img src="../assets/repayment-schedule-schema.svg" alt="Repayment schedule relational schema"></a>

[High-resolution PNG fallback](../assets/repayment-schedule-schema.png)

This slice shows the repayment schedule created for an accepted agreement.
Payment execution is documented in the payment views.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `agreement` | `1 -> 0..1` | `repayment` | `repayment.agreement_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_repayment_agreement`; `ON DELETE RESTRICT` |
| `R2` | `startup` | `1 -> 0..N` | `repayment` | `repayment.startup_id` is `FK`, `NOT NULL`; `fk_repayment_startup`; `ON DELETE RESTRICT` |
| `R3` | `investor` | `1 -> 0..N` | `repayment` | `repayment.investor_id` is `FK`, `NOT NULL`; `fk_repayment_investor`; `ON DELETE RESTRICT` |
| `R4` | `repayment` | `1 -> 0..N` | `repayment_installment` | `repayment_installment.repayment_id` is `FK`, `NOT NULL`; `fk_repayment_installment_repayment`; `ON DELETE RESTRICT` |
