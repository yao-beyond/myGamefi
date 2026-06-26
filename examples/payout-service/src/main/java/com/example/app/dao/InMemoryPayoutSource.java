package com.example.app.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.example.app.dto.Payout;

/**
 * 記憶體版 PayoutSource：供範例/測試離線驗證 CPS-03 增量更新（正式環境用 DAO + Connection）。
 * 置於 dao 層：只依賴 dto。
 */
public final class InMemoryPayoutSource implements PayoutSource {

    private final Map<Long, Payout> rows = new ConcurrentHashMap<>();

    /** 模擬 DB 寫入（含 updatedate）。 */
    public void upsert(Payout payout) {
        rows.put(payout.getId(), payout);
    }

    @Override
    public List<Payout> findUpdatedSince(Timestamp since) {
        List<Payout> out = new ArrayList<>();
        for (Payout p : rows.values()) {
            if (p.getUpdateDate().after(since)) {
                out.add(p);
            }
        }
        return out;
    }
}
