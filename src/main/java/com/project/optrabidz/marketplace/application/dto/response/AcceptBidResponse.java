package com.project.optrabidz.marketplace.application.dto.response;

public record AcceptBidResponse(
        BidActionResponse bid,
        CloseListingResponse listing,
        AgreementResponse agreement
) {
}
