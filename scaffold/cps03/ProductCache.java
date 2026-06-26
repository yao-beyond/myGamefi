package com.example.scaffold.cps03;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongConsumer;

/**
 * CPS-03 · 時間戳增量更新的具體快取（對應 MyXchange 的 EventCache）。
 *
 * 不變式：
 *  - ConcurrentHashMap 儲存熱資料；per-domain ReentrantLock 保護 update。
 *  - 每類資料一個 lastUpdateDate（volatile）。
 *  - 查詢條件固定：updatedate > lastUpdateDate - ERROR_VALUE（容差，避免時鐘/精度漏更新）。
 *  - update() idempotent：重複載入同列不產生副作用（直接覆寫 + 重算 watermark）。
 *  - delete 用 tombstone 標記處理。
 *  - 級聯通知只在「本 cache 成功套用後」觸發；級聯方向必須 DAG（不可循環）。
 */
public final class ProductCache extends AbstractCache {

    /** 時間容差（毫秒）：對應 blueprint 的 ERROR_VALUE。 */
    private static final long ERROR_VALUE = 1000L;

    private static final ProductCache INSTANCE = new ProductCache();

    public static ProductCache getInstance() {
        return INSTANCE;
    }

    private final Map<Long, ProductRow> products = new ConcurrentHashMap<>(1024);
    private final ReentrantLock lock = new ReentrantLock();

    private volatile Timestamp lastUpdateDate = new Timestamp(0L);

    /** 級聯下游：成功套用後才通知（DAG，注入避免硬耦合與循環）。 */
    private volatile LongConsumer cascade = id -> { };

    private ProductDao dao;

    private ProductCache() {
    }

    public void wire(ProductDao dao, LongConsumer cascade) {
        this.dao = dao;
        if (cascade != null) {
            this.cascade = cascade;
        }
    }

    @Override
    public void update() {
        lock.lock();
        try {
            // 容差查詢：往回退 ERROR_VALUE，寧可重載也不漏更新（靠 idempotent 抵銷重複）
            Timestamp since = new Timestamp(lastUpdateDate.getTime() - ERROR_VALUE);
            List<ProductRow> rows = dao.findUpdatedSince(since);

            long maxMillis = lastUpdateDate.getTime();
            for (ProductRow row : rows) {
                if (row.isDeleted()) {
                    products.remove(row.getId());          // tombstone
                } else {
                    products.put(row.getId(), row);         // 覆寫 = idempotent
                }
                maxMillis = Math.max(maxMillis, row.getUpdateDate().getTime());
            }
            // watermark 推進必須在「成功套用全部變更後」
            this.lastUpdateDate = new Timestamp(maxMillis);

            // 級聯通知：本 cache 成功後才觸發
            for (ProductRow row : rows) {
                cascade.accept(row.getId());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String key) {
        products.remove(Long.parseLong(key));
    }

    public ProductRow get(long id) {
        return products.get(id);
    }

    public int size() {
        return products.size();
    }

    /** test hook。 */
    public void resetForTest() {
        lock.lock();
        try {
            products.clear();
            lastUpdateDate = new Timestamp(0L);
        } finally {
            lock.unlock();
        }
    }
}
