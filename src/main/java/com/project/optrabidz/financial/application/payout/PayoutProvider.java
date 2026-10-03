package com.project.optrabidz.financial.application.payout;

public interface PayoutProvider {
    PayoutResult execute(PayoutInstruction instruction);
}
