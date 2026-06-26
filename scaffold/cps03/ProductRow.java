package com.example.scaffold.cps03;

import java.sql.Timestamp;

/** DAO 回傳的列資料（DTO）。含 updatedate 供增量更新與容差判斷。 */
public final class ProductRow {

    private final long id;
    private final String name;
    private final boolean deleted;
    private final Timestamp updateDate;

    public ProductRow(long id, String name, boolean deleted, Timestamp updateDate) {
        this.id = id;
        this.name = name;
        this.deleted = deleted;
        this.updateDate = updateDate;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Timestamp getUpdateDate() {
        return updateDate;
    }
}
