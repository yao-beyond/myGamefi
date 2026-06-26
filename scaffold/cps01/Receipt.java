package com.example.scaffold.cps01;

/** 純資料 DTO。 */
public final class Receipt {

    private final String id;
    private final long amount;
    private final boolean success;

    public Receipt(String id, long amount, boolean success) {
        this.id = id;
        this.amount = amount;
        this.success = success;
    }

    public String getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }

    public boolean isSuccess() {
        return success;
    }
}
