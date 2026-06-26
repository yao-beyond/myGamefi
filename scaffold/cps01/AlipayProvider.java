package com.example.scaffold.cps01;

/** CPS-01 · 第二個供應商實作。展示同合約、不同實作。 */
public final class AlipayProvider extends PayProvider {

    private volatile boolean ready;

    @Override
    public void init() {
        if (ready) {
            return;
        }
        ready = true;
    }

    @Override
    public void shutdown() {
        ready = false;
    }

    @Override
    public Receipt charge(Order order) {
        return new Receipt("alipay-" + order.getId(), order.getAmount(), true);
    }

    @Override
    public Receipt refund(String receiptId) {
        return new Receipt(receiptId, 0, true);
    }
}
