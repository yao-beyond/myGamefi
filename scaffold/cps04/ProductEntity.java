package com.example.scaffold.cps04;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CPS-04 · 中間層 Entity（對應 MarketEntity）。
 *
 * 不變式：
 *  - 每層 map key 型別固定：Map<Long, SkuEntity>。
 *  - 更新子節點必須透過父節點方法維護一致性（不繞過父層散落儲存）。
 */
public final class ProductEntity {

    private final long productId;
    private final Map<Long, SkuEntity> skus = new ConcurrentHashMap<>();

    public ProductEntity(long productId) {
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }

    public void putSku(SkuEntity sku) {
        skus.put(sku.getSkuId(), sku);
    }

    public SkuEntity getSku(long skuId) {
        return skus.get(skuId);
    }

    public void removeSku(long skuId) {
        skus.remove(skuId);
    }

    public Map<Long, SkuEntity> getSkus() {
        return skus;
    }
}
