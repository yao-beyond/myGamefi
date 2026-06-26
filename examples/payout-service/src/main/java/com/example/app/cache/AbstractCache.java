package com.example.app.cache;

/** CPS-03 · 快取生命週期抽象基底。 */
public abstract class AbstractCache {

    /** 增量更新：只載入 updatedate > lastUpdateDate - ERROR_VALUE 的資料；必須 idempotent。 */
    public abstract void update();

    public void remove(String key) {
        // 預設 no-op
    }

    public void init() {
        update();
    }
}
