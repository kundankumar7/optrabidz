# Settlement

[Back to focused views](README.md)

<a href="../assets/settlement-schema.svg"><img src="../assets/settlement-schema.svg" alt="Settlement relational schema"></a>

[High-resolution PNG fallback](../assets/settlement-schema.png)

This slice shows how a settlement belongs to an accepted agreement and its
participants. Repayment scheduling is documented separately.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `agreement` | `1 -> 0..1` | `settlement` | `settlement.agreement_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_settlement_agreement`; `ON DELETE RESTRICT` |
| `R2` | `startup` | `1 -> 0..N` | `settlement` | `settlement.startup_id` is `FK`, `NOT NULL`; `fk_settlement_startup`; `ON DELETE RESTRICT` |
| `R3` | `investor` | `1 -> 0..N` | `settlement` | `settlement.investor_id` is `FK`, `NOT NULL`; `fk_settlement_investor`; `ON DELETE RESTRICT` |
