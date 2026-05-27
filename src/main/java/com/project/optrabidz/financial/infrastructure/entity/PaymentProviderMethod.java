package com.project.optrabidz.financial.infrastructure.entity;

import com.project.optrabidz.financial.domain.model.PaymentMethodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_provider_method")
@IdClass(PaymentProviderMethodId.class)
public class PaymentProviderMethod {
    @Id
    @Column(name = "provider_code", nullable = false)
    private String providerCode;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "method_type", nullable = false, columnDefinition = "payment_method_type_enum")
    private PaymentMethodType methodType;

    @Id
    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "min_amount")
    private BigDecimal minAmount;

    @Column(name = "max_amount")
    private BigDecimal maxAmount;

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public PaymentMethodType getMethodType() {
        return methodType;
    }

    public void setMethodType(PaymentMethodType methodType) {
        this.methodType = methodType;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
}
