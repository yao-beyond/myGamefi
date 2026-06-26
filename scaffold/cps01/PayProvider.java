package com.example.scaffold.cps01;

/**
 * CPS-01 · Provider-Enum-Registry 的抽象基底（對應 myGameFi 的 ApiDataProvider）。
 *
 * 不變式：
 *  - 必須是 abstract class（非 interface），方法簽名固定。
 *  - 定義「同行為、多供應商」的標準合約，不涉及實例管理。
 *  - init() 必須 idempotent；若持有連線池/thread/socket，子類須實作 shutdown()。
 */
public abstract class PayProvider {

    /** 取得外部資源（連線池、token...）。必須可重複呼叫而不產生副作用。 */
    public abstract void init();

    /** 釋放資源。熱切換換下舊 provider 後會被呼叫，避免資源洩漏。 */
    public abstract void shutdown();

    /** 核心業務行為。 */
    public abstract Receipt charge(Order order);

    public abstract Receipt refund(String receiptId);
}
