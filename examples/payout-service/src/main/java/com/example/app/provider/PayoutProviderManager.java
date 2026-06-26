package com.example.app.provider;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;

import com.example.app.code.PayoutProviderType;
import com.example.app.dto.Payout;

/**
 * CPS-01 · Singleton 協調者：從設定來源讀 active id → init 成功後才 swap（lock 內）。
 * 失敗保留舊 provider；對外只暴露委派方法；所有欄位 private（conformance 檢查）。
 */
public final class PayoutProviderManager {

    private static final PayoutProviderManager INSTANCE = new PayoutProviderManager();

    public static PayoutProviderManager getInstance() {
        return INSTANCE;
    }

    private volatile IntSupplier activeIdSupplier = () -> PayoutProviderType.BANK_WIRE.getValue();
    private PayoutProvider provider;
    private final ReentrantLock lock = new ReentrantLock();

    private PayoutProviderManager() {
    }

    public void configureSettingSource(IntSupplier supplier) {
        this.activeIdSupplier = supplier;
    }

    /** 無停機熱切換：init 新 provider 成功後才原子 swap，失敗保留舊的。 */
    public void reload() {
        lock.lock();
        try {
            int id = activeIdSupplier.getAsInt();
            PayoutProviderType type = PayoutProviderType.getInstanceOf(id)
                    .orElseThrow(() -> new IllegalStateException("Unknown payout provider id: " + id));
            if (type.isDisabled()) {
                throw new IllegalStateException("Payout provider disabled: " + type);
            }
            PayoutProvider next = type.getProvider();
            next.init();                       // 失敗會丟出，下一行不執行
            PayoutProvider old = this.provider;
            this.provider = next;              // 原子 swap（lock 保護）
            if (old != null && old != next) {
                old.shutdown();
            }
        } finally {
            lock.unlock();
        }
    }

    public String send(Payout payout) {
        return provider.send(payout);
    }

    /** test hook。 */
    public void resetForTest() {
        lock.lock();
        try {
            provider = null;
        } finally {
            lock.unlock();
        }
    }
}
