package com.example.scaffold.cps04;

/**
 * CPS-04 · 階層最底層 Entity（對應 InstrumentEntity）。
 * Entity 持有領域資料 + 維護子層 map，但無業務運算（運算進 BO / Type enum）。
 */
public final class SkuEntity {

    private final long skuId;
    private volatile long stock;

    public SkuEntity(long skuId, long stock) {
        this.skuId = skuId;
        this.stock = stock;
    }

    public long getSkuId() {
        return skuId;
    }

    public long getStock() {
        return stock;
    }

    public void setStock(long stock) {
        this.stock = stock;
    }
}
