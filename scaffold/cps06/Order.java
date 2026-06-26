package com.example.scaffold.cps06;

/**
 * CPS-06 · DTO 層（最底層）。純資料，不依賴 BO/Cache/DAO/Controller。
 * 注意：domain DTO 在 MyXchange 慣例中不帶後綴（如 Order, Market），
 *      但仍視為 *DTO 角色。
 */
public final class Order {

    private final long id;
    private final long customerId;
    private final long amount;
    private final int statusValue;

    public Order(long id, long customerId, long amount, int statusValue) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.statusValue = statusValue;
    }

    public long getId() {
        return id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public long getAmount() {
        return amount;
    }

    public int getStatusValue() {
        return statusValue;
    }
}
