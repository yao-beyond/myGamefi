package com.example.app.provider;

import com.example.app.dto.Payout;

/** CPS-01 · 具體供應商；只能由 PayoutProviderType enum 實例化。 */
public final class BankWireProvider extends PayoutProvider {

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
    public String send(Payout payout) {
        return "bankwire-" + payout.getId();
    }
}
