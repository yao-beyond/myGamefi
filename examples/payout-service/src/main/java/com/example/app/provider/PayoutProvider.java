package com.example.app.provider;

import com.example.app.dto.Payout;

/** CPS-01 · 出款供應商抽象基底（abstract class，方法簽名固定）。 */
public abstract class PayoutProvider {

    /** 取得資源；必須 idempotent。 */
    public abstract void init();

    /** 釋放資源；熱切換換下舊 provider 後呼叫。 */
    public abstract void shutdown();

    /** 送出出款，回傳供應商交易號。 */
    public abstract String send(Payout payout);
}
