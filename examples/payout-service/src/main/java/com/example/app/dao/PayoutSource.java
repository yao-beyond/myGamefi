package com.example.app.dao;

import java.sql.Timestamp;
import java.util.List;

import com.example.app.dto.Payout;

/**
 * CPS-03 的資料來源介面（讓 PayoutCache 可注入；測試用 fake，正式用 DAO+Connection）。
 * 置於 dao 層：只依賴 dto，不依賴 bo/cache/controller。
 */
public interface PayoutSource {

    /** 查 updatedate > since 的列（含 deleted 標記供 tombstone）。 */
    List<Payout> findUpdatedSince(Timestamp since);
}
