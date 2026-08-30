# Agreement Acceptance

[Back to focused views](README.md)

<a href="../assets/agreement-acceptance-schema.svg"><img src="../assets/agreement-acceptance-schema.svg" alt="Agreement acceptance relational schema"></a>

[High-resolution PNG fallback](../assets/agreement-acceptance-schema.png)

This slice shows the accepted agreement and final debt terms. Triggers require
the bid to be accepted and enforce listing and participant consistency.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `funding_listing` | `1 -> 0..N` | `agreement` | `agreement.listing_id` is `FK`, `NOT NULL`; `fk_agreement_listing`; `ON DELETE RESTRICT` |
| `R2` | `bid` | `1 -> 0..1` | `agreement` | `agreement.bid_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_agreement_bid`; `ON DELETE RESTRICT` |
| `R3` | `startup` | `1 -> 0..N` | `agreement` | `agreement.startup_id` is `FK`, `NOT NULL`; `fk_agreement_startup`; `ON DELETE RESTRICT` |
| `R4` | `investor` | `1 -> 0..N` | `agreement` | `agreement.investor_id` is `FK`, `NOT NULL`; `fk_agreement_investor`; `ON DELETE RESTRICT` |
| `R5` | `agreement` | `1 -> 0..1` | `agreement_debt_terms` | `agreement_debt_terms.agreement_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_agreement_debt_terms_agreement`; `ON DELETE CASCADE` |
