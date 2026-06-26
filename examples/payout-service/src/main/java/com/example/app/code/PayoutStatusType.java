package com.example.app.code;

import java.util.Optional;

/** CPS-02 · Domain Type Enum：狀態機收進 enum 多型，取代 if/switch。 */
public enum PayoutStatusType {

    REQUESTED(1) {
        @Override public boolean isTerminal() { return false; }
        @Override public boolean canCancel() { return true; }
    },
    SENT(2) {
        @Override public boolean isTerminal() { return false; }
        @Override public boolean canCancel() { return false; }
    },
    CONFIRMED(3) {
        @Override public boolean isTerminal() { return true; }
        @Override public boolean canCancel() { return false; }
    },
    REJECTED(4) {
        @Override public boolean isTerminal() { return true; }
        @Override public boolean canCancel() { return false; }
    };

    private static final PayoutStatusType[] VALUES = values();

    private final int value;

    PayoutStatusType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public abstract boolean isTerminal();

    public abstract boolean canCancel();

    public static Optional<PayoutStatusType> getInstanceOf(int value) {
        for (PayoutStatusType e : VALUES) {
            if (e.value == value) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }
}
