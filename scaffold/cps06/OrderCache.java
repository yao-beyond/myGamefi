package com.example.scaffold.cps06;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CPS-06 · Cache 層（簡化版，完整增量更新見 CPS-03）。
 * 只被 BO 呼叫；不依賴 Controller。
 */
public final class OrderCache {

    private static final OrderCache INSTANCE = new OrderCache();

    public static OrderCache getInstance() {
        return INSTANCE;
    }

    private final Map<Long, Order> hot = new ConcurrentHashMap<>();

    private OrderCache() {
    }

    public Order get(long id) {
        return hot.get(id);
    }

    public void put(Order order) {
        hot.put(order.getId(), order);
    }

    public void evict(long id) {
        hot.remove(id);
    }
}
