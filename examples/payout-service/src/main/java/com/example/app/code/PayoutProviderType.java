package com.example.app.code;

import java.util.Optional;

import com.example.app.provider.BankWireProvider;
import com.example.app.provider.EWalletProvider;
import com.example.app.provider.PayoutProvider;

/**
 * CPS-01 · Enum 註冊表：每個出款供應商一個常數，內嵌 lazy-init 實例 + 元資料。
 * 這是唯一允許 new XxxProvider() 的地方（conformance 會檢查）。
 */
public enum PayoutProviderType {

    BANK_WIRE(1, "USD") {
        private PayoutProvider provider;
        @Override public synchronized PayoutProvider getProvider() {
            if (provider == null) {
                provider = new BankWireProvider();
            }
            return provider;
        }
    },
    EWALLET(2, "USD") {
        private PayoutProvider provider;
        @Override public synchronized PayoutProvider getProvider() {
            if (provider == null) {
                provider = new EWalletProvider();
            }
            return provider;
        }
    };

    private static final PayoutProviderType[] VALUES = values();

    private final int value;
    private final String defaultCurrency;

    PayoutProviderType(int value, String defaultCurrency) {
        this.value = value;
        this.defaultCurrency = defaultCurrency;
    }

    public int getValue() { return value; }

    public String getDefaultCurrency() { return defaultCurrency; }

    public boolean isDisabled() { return false; }

    public abstract PayoutProvider getProvider();

    public static Optional<PayoutProviderType> getInstanceOf(int value) {
        for (PayoutProviderType e : VALUES) {
            if (e.value == value) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }
}
