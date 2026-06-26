package com.example.app.dto;

import java.sql.Timestamp;

/** CPS-06 · DTO 層：純資料，不依賴任何上層。 */
public final class Payout {

    private final long id;
    private final long customerId;
    private final long amount;
    private final int statusValue;
    private final boolean deleted;
    private final Timestamp updateDate;

    public Payout(long id, long customerId, long amount, int statusValue,
                  boolean deleted, Timestamp updateDate) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.statusValue = statusValue;
        this.deleted = deleted;
        this.updateDate = updateDate;
    }

    public long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public long getAmount() { return amount; }
    public int getStatusValue() { return statusValue; }
    public boolean isDeleted() { return deleted; }
    public Timestamp getUpdateDate() { return updateDate; }
}
