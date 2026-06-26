package com.example.scaffold.cps04;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CPS-04 · 持有階層樹的快取，示範「維護父層一致性」與「固定讀路徑」。
 *
 * 不變式：
 *  - 讀路徑固定順序：categoryId → productId → skuId。
 *  - 更新 sku 時，沿父鏈維護一致性（缺父節點則建立），不散落全域儲存。
 *  - 可選 secondary index（skuId → 直達）必須與主樹同步維護。
 */
public final class CatalogCache {

    private final Map<Long, CategoryEntity> categories = new ConcurrentHashMap<>();

    /** secondary index：skuId 直達，與主樹同步維護（manifest 宣告才可有）。 */
    private final Map<Long, SkuEntity> skuIndex = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    /** 沿父鏈維護一致性。 */
    public void upsertSku(long categoryId, long productId, SkuEntity sku) {
        lock.lock();
        try {
            CategoryEntity category = categories.computeIfAbsent(categoryId, CategoryEntity::new);
            ProductEntity product = category.getProduct(productId);
            if (product == null) {
                product = new ProductEntity(productId);
                category.putProduct(product);
            }
            product.putSku(sku);
            skuIndex.put(sku.getSkuId(), sku);   // 同步 secondary index
        } finally {
            lock.unlock();
        }
    }

    /** 固定讀路徑：category → product → sku。 */
    public SkuEntity getSku(long categoryId, long productId, long skuId) {
        CategoryEntity category = categories.get(categoryId);
        if (category == null) {
            return null;
        }
        ProductEntity product = category.getProduct(productId);
        if (product == null) {
            return null;
        }
        return product.getSku(skuId);
    }

    /** 經 secondary index 直達（已宣告才用）。 */
    public SkuEntity getSkuByIndex(long skuId) {
        return skuIndex.get(skuId);
    }
}
