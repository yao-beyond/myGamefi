package com.example.scaffold.cps01;

/** 純資料 DTO（CPS-06）。無業務運算。此處供 CPS-01 範例使用。 */
public final class Order {

    private final long id;
    private final long amount;

    public Order(long id, long amount) {
        this.id = id;
        this.amount = amount;
    }

    public long getId() {
        return id;
    }

    public long getAmount() {
        return amount;
    }
}
