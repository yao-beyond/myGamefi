package com.example.scaffold.cps01;

/** CPS-01 · 一個具體供應商實作。只能由 PayProviderType enum 透過 new 註冊（其他地方禁止 new）。 */
public final class StripeProvider extends PayProvider {

    private volatile boolean ready;

    @Override
    public void init() {
        // idempotent：重複呼叫不重建資源
        if (ready) {
            return;
        }
        // openConnectionPool(); loadApiKey();
        ready = true;
    }

    @Override
    public void shutdown() {
        if (!ready) {
            return;
        }
        // closeConnectionPool();
        ready = false;
    }

    @Override
    public Receipt charge(Order order) {
        return new Receipt("stripe-" + order.getId(), order.getAmount(), true);
    }

    @Override
    public Receipt refund(String receiptId) {
        return new Receipt(receiptId, 0, true);
    }
}
