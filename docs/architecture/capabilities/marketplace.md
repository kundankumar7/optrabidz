# Marketplace

[Back to capability views](README.md)

This capability coordinates eligibility and reference classifications with the
listing, bidding, and agreement lifecycle.

| Module | Owns |
|---|---|
| [`classification`](../modules/classification.md) | Startup classifications and investor preferences |
| [`marketplace`](../modules/marketplace.md) | Listings, recommendations, bids, agreements, and guarded transitions |
| [`governance`](../modules/governance.md) | Authority, eligibility, neutrality, and lifecycle constraints |

Marketplace application services query classification and governance rules
before changing marketplace state. Existing dependencies on identity,
participation, security, and common contracts remain visible in the
[dependency view](../module-dependencies.md).
