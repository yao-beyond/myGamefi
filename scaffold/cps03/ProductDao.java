package com.example.scaffold.cps03;

import java.sql.Timestamp;
import java.util.List;

/**
 * 資料來源介面（測試可換 fake DAO 驗證增量更新邏輯）。
 * 實作見 CPS-07 的 DAO 樣板。
 */
public interface ProductDao {

    /** 查 updatedate > since 的列（含 deleted 標記，供 tombstone）。 */
    List<ProductRow> findUpdatedSince(Timestamp since);
}
