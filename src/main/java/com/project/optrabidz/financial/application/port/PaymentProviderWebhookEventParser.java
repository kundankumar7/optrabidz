package com.project.optrabidz.financial.application.port;

import com.project.optrabidz.financial.application.command.PaymentProviderWebhookCommand;

public interface PaymentProviderWebhookEventParser {
    PaymentProviderWebhookCommand parse(String providerCode, byte[] rawBody);
}
