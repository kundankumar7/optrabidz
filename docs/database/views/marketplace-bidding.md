# Marketplace and Bidding

[Back to focused views](README.md)

<a href="../assets/marketplace-bidding-schema.svg"><img src="../assets/marketplace-bidding-schema.svg" alt="Marketplace and bidding relational schema"></a>

[High-resolution PNG fallback](../assets/marketplace-bidding-schema.png)

This slice focuses on listings and bids. Agreement acceptance is documented
separately so the bidding model stays readable.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `startup` | `1 -> 0..N` | `funding_listing` | `funding_listing.startup_id` is `FK`, `NOT NULL`; `fk_listing_startup`; `ON DELETE RESTRICT` |
| `R2` | `funding_listing` | `1 -> 0..1` | `listing_debt_terms` | `listing_debt_terms.listing_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_listing_debt_terms_listing`; `ON DELETE CASCADE` |
| `R3` | `funding_listing` | `1 -> 0..N` | `bid` | `bid.listing_id` is `FK`, `NOT NULL`; `fk_bid_listing`; `ON DELETE RESTRICT` |
| `R4` | `investor` | `1 -> 0..N` | `bid` | `bid.investor_id` is `FK`, `NOT NULL`; `fk_bid_investor`; `ON DELETE RESTRICT` |
| `R5` | `bid` | `1 -> 0..1` | `bid_debt_terms` | `bid_debt_terms.bid_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_bid_debt_terms_bid`; `ON DELETE CASCADE` |

`uq_one_accepted_bid_per_listing` limits accepted bids per listing, but the base
`funding_listing -> bid` relationship remains `1 -> 0..N`.
