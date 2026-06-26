package com.example.app.provider;

import com.example.app.dto.Payout;

/** CPS-01 · 第二個供應商實作。 */
public final class EWalletProvider extends PayoutProvider {

    private volatile boolean ready;

    @Override
    public void init() {
        ready = true;
    }

    @Override
    public void shutdown() {
        ready = false;
    }

    @Override
    public String send(Payout payout) {
        return "ewallet-" + payout.getId();
    }
}
