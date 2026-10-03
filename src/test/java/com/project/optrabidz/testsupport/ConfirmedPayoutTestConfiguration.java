package com.project.optrabidz.testsupport;

import com.project.optrabidz.financial.application.payout.PayoutProvider;
import com.project.optrabidz.financial.application.payout.PayoutResult;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class ConfirmedPayoutTestConfiguration {

    @Bean
    PayoutProvider payoutProvider() {
        return instruction -> PayoutResult.confirmed(
                "TEST-PAYOUT-" + instruction.payoutTransferId());
    }
}
