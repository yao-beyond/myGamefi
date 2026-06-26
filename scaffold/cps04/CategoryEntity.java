package com.example.scaffold.cps04;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CPS-04 · 階層最上層 Entity（對應 MarketGroupEntity）。
 * 階層明示：CategoryEntity → ProductEntity → SkuEntity。
 */
public final class CategoryEntity {

    private final long categoryId;
    private final Map<Long, ProductEntity> products = new ConcurrentHashMap<>();

    public CategoryEntity(long categoryId) {
        this.categoryId = categoryId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void putProduct(ProductEntity product) {
        products.put(product.getProductId(), product);
    }

    public ProductEntity getProduct(long productId) {
        return products.get(productId);
    }

    public void removeProduct(long productId) {
        products.remove(productId);
    }

    public Map<Long, ProductEntity> getProducts() {
        return products;
    }
}
