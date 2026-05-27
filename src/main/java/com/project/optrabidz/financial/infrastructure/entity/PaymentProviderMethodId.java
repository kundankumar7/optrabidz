package com.project.optrabidz.financial.infrastructure.entity;

import com.project.optrabidz.financial.domain.model.PaymentMethodType;

import java.io.Serializable;
import java.util.Objects;

public class PaymentProviderMethodId implements Serializable {
    private String providerCode;
    private PaymentMethodType methodType;
    private String currencyCode;

    public PaymentProviderMethodId() {
    }

    public PaymentProviderMethodId(String providerCode, PaymentMethodType methodType, String currencyCode) {
        this.providerCode = providerCode;
        this.methodType = methodType;
        this.currencyCode = currencyCode;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public PaymentMethodType getMethodType() {
        return methodType;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PaymentProviderMethodId that)) {
            return false;
        }
        return Objects.equals(providerCode, that.providerCode)
                && methodType == that.methodType
                && Objects.equals(currencyCode, that.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerCode, methodType, currencyCode);
    }
}
