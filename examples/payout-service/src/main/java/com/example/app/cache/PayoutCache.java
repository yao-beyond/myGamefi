package com.example.app.cache;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.example.app.dao.PayoutSource;
import com.example.app.dto.Payout;

/**
 * CPS-03 · 時間戳增量更新快取：ConcurrentHashMap + ReentrantLock + lastUpdateDate(volatile)。
 * 查詢用容差 (ERROR_VALUE)；update idempotent（覆寫）；delete 用 tombstone。
 */
public final class PayoutCache extends AbstractCache {

    private static final long ERROR_VALUE = 1000L;

    private static final PayoutCache INSTANCE = new PayoutCache();

    public static PayoutCache getInstance() {
        return INSTANCE;
    }

    private final Map<Long, Payout> payouts = new ConcurrentHashMap<>(256);
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Timestamp lastUpdateDate = new Timestamp(0L);
    private PayoutSource source;

    private PayoutCache() {
    }

    public void wire(PayoutSource source) {
        this.source = source;
    }

    @Override
    public void update() {
        lock.lock();
        try {
            Timestamp since = new Timestamp(lastUpdateDate.getTime() - ERROR_VALUE);
            List<Payout> rows = source.findUpdatedSince(since);
            long maxMillis = lastUpdateDate.getTime();
            for (Payout row : rows) {
                if (row.isDeleted()) {
                    payouts.remove(row.getId());        // tombstone
                } else {
                    payouts.put(row.getId(), row);      // 覆寫 = idempotent
                }
                maxMillis = Math.max(maxMillis, row.getUpdateDate().getTime());
            }
            this.lastUpdateDate = new Timestamp(maxMillis);
        } finally {
            lock.unlock();
        }
    }

    public Payout get(long id) {
        return payouts.get(id);
    }

    public void put(Payout payout) {
        payouts.put(payout.getId(), payout);
    }

    public void evict(long id) {
        payouts.remove(id);
    }

    public int size() {
        return payouts.size();
    }

    public void resetForTest() {
        lock.lock();
        try {
            payouts.clear();
            lastUpdateDate = new Timestamp(0L);
        } finally {
            lock.unlock();
        }
    }
}
