package com.example.scaffold.cps01;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;

/**
 * CPS-01 · Singleton 協調者（對應 MyXchange 的 ApiDataProviderManager）。
 *
 * 不變式：
 *  - Singleton；對外只暴露委派方法，不暴露 mutable provider 欄位。
 *  - active provider id 只能從外部設定來源（DB SystemSetting）取得。
 *  - 熱切換 = 「新 provider init() 成功後才 swap」；swap 在 lock 內完成。
 *  - 失敗不得污染現有 provider（init 丟例外 → 保留舊 provider）。
 *  - 多把 lock 取得順序固定：payLock → refundLock（避免死鎖）。
 *  - 提供 reset()/test hook，避免單例在測試間互相污染。
 */
public final class PayProviderManager {

    private static final PayProviderManager INSTANCE = new PayProviderManager();

    public static PayProviderManager getInstance() {
        return INSTANCE;
    }

    /** 設定來源：實務上注入「讀 DB SystemSetting」的 supplier。 */
    private volatile IntSupplier activeIdSupplier = () -> PayProviderType.STRIPE.getValue();

    private PayProvider payProvider;
    private PayProvider refundProvider;

    private final ReentrantLock payLock = new ReentrantLock();
    private final ReentrantLock refundLock = new ReentrantLock();

    private PayProviderManager() {
    }

    public void configureSettingSource(IntSupplier supplier) {
        this.activeIdSupplier = supplier;
    }

    /** 啟動時初始化兩個 provider。 */
    public void init() {
        reloadPayProvider();
        reloadRefundProvider();
    }

    /** 無停機熱切換：讀設定 → enum 反查 → init 新 provider → 成功才 swap → 關舊的。 */
    public void reloadPayProvider() {
        payLock.lock();
        try {
            PayProvider next = resolveProvider();
            next.init();                 // 先 init，失敗會丟出，下一行不會執行
            PayProvider old = this.payProvider;
            this.payProvider = next;     // 原子 swap（lock 保護下）
            if (old != null && old != next) {
                old.shutdown();          // 釋放舊資源
            }
        } finally {
            payLock.unlock();
        }
    }

    public void reloadRefundProvider() {
        refundLock.lock();
        try {
            PayProvider next = resolveProvider();
            next.init();
            this.refundProvider = next;
        } finally {
            refundLock.unlock();
        }
    }

    private PayProvider resolveProvider() {
        int id = activeIdSupplier.getAsInt();
        PayProviderType type = PayProviderType.getInstanceOf(id)
                .orElseThrow(() -> new IllegalStateException("Unknown pay provider id: " + id));
        if (type.isDisabled()) {
            throw new IllegalStateException("Pay provider disabled: " + type);
        }
        return type.getProvider();
    }

    // ---- 對外只暴露委派方法 ----
    public Receipt charge(Order order) {
        return payProvider.charge(order);
    }

    public Receipt refund(String receiptId) {
        return refundProvider.refund(receiptId);
    }

    /** test hook：避免單例污染後續測試。 */
    public void resetForTest() {
        payLock.lock();
        refundLock.lock();
        try {
            payProvider = null;
            refundProvider = null;
        } finally {
            refundLock.unlock();
            payLock.unlock();
        }
    }
}
