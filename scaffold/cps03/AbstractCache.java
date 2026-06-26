package com.example.scaffold.cps03;

/**
 * CPS-03 · 快取生命週期抽象基底（對應 myGameFi 的 AbstractCache）。
 *
 * 不變式：
 *  - 定義 init() / update() 生命週期；init() 預設等於首次 update()。
 *  - update() 必須 idempotent：重複跑同一批資料不得產生副作用。
 *  - 子類負責 ConcurrentHashMap + per-domain ReentrantLock + lastUpdateDate。
 */
public abstract class AbstractCache {

    /** 增量更新：只載入 updatedate > lastUpdateDate - ERROR_VALUE 的資料。 */
    public abstract void update();

    /** 選擇性移除（tombstone / 失效）。 */
    public void remove(String key) {
        // 預設 no-op；需要刪除策略的子類覆寫
    }

    /** 初始化 = 首次更新。 */
    public void init() {
        update();
    }
}
