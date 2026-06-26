package com.example.scaffold.cps02;

import java.util.Optional;

/**
 * CPS-02 · Domain Type Enum（對應 myGameFi 的 228 個 *Type）。
 *
 * 不變式（統一骨架，所有 *Type 都長這樣）：
 *  - private final int value + getValue()
 *  - static getInstanceOf(int)：id 反查，未知回 Optional.empty()
 *  - 必要的 boolean 述詞（把判斷邏輯收進 enum）
 *  - 必要的 abstract 業務方法（用多型取代 switch/if）
 *  - id 全域唯一、不可變、對齊 DB code table
 *  - 禁止持有 DAO / Cache / Session / request 等 runtime 可變狀態
 *
 * 取代：if (status == 1) ... else if (status == 2)；switch(status)；設定字串比對。
 */
public enum OrderStatusType {

    CREATED(1) {
        @Override public boolean isTerminal() { return false; }
        @Override public boolean canCancel() { return true; }
        @Override public OrderStatusType next() { return PAID; }
    },
    PAID(2) {
        @Override public boolean isTerminal() { return false; }
        @Override public boolean canCancel() { return true; }
        @Override public OrderStatusType next() { return SHIPPED; }
    },
    SHIPPED(3) {
        @Override public boolean isTerminal() { return false; }
        @Override public boolean canCancel() { return false; }
        @Override public OrderStatusType next() { return DONE; }
    },
    DONE(4) {
        @Override public boolean isTerminal() { return true; }
        @Override public boolean canCancel() { return false; }
        @Override public OrderStatusType next() { return DONE; }
    },
    CANCELLED(5) {
        @Override public boolean isTerminal() { return true; }
        @Override public boolean canCancel() { return false; }
        @Override public OrderStatusType next() { return CANCELLED; }
    };

    private static final OrderStatusType[] VALUES = values();

    private final int value;

    OrderStatusType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // ---- 述詞 + 業務方法（多型，取代分支）----
    public abstract boolean isTerminal();

    public abstract boolean canCancel();

    public abstract OrderStatusType next();

    public static Optional<OrderStatusType> getInstanceOf(int value) {
        for (OrderStatusType e : VALUES) {
            if (e.value == value) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }
}
