package com.example.scaffold.cps01;

import java.util.Optional;

/**
 * CPS-01 · Enum 註冊表（對應 myGameFi 的 ApiDataProviderType）。
 *
 * 不變式：
 *  - 每個供應商 = 一個 enum 常數，內嵌 lazy-init 的 provider 實例 + 元資料。
 *  - id 全域唯一且不可變（綁定 DB SystemSetting 的值）。
 *  - provider 實例 lazy-init：常數欄位只在第一次 getProvider() 時建立。
 *  - getInstanceOf(int) 對未知 id 行為固定：回傳 Optional.empty()（呼叫端決定 fail/fallback）。
 *  - 這是唯一允許 new XxxProvider() 的地方。
 */
public enum PayProviderType {

    STRIPE(1, "https://api.stripe.com", Currency.USD) {
        private PayProvider provider;
        @Override
        public synchronized PayProvider getProvider() {
            if (provider == null) {
                provider = new StripeProvider();
            }
            return provider;
        }
    },
    ALIPAY(2, "https://openapi.alipay.com", Currency.CNY) {
        private PayProvider provider;
        @Override
        public synchronized PayProvider getProvider() {
            if (provider == null) {
                provider = new AlipayProvider();
            }
            return provider;
        }
    };

    private static final PayProviderType[] VALUES = values();

    private final int value;
    private final String url;
    private final Currency defaultCurrency;

    PayProviderType(int value, String url, Currency defaultCurrency) {
        this.value = value;
        this.url = url;
        this.defaultCurrency = defaultCurrency;
    }

    /** 內嵌業務實例（lazy）。 */
    public abstract PayProvider getProvider();

    // ---- 元資料方法（集中內聚，取代散落的 if/switch）----
    public int getValue() {
        return value;
    }

    public String getUrl() {
        return url;
    }

    public Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    public boolean isDisabled() {
        return false;
    }

    /** id 反查；未知 id 回 empty，由呼叫端決定 fail-fast 或 fallback。 */
    public static Optional<PayProviderType> getInstanceOf(int value) {
        for (PayProviderType e : VALUES) {
            if (e.value == value) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }
}
