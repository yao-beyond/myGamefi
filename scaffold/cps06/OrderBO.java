package com.example.scaffold.cps06;

import java.sql.Connection;
import java.util.List;

/**
 * CPS-06 · BO 業務聚合層（對應 MyXchange 的 EventBO/CustomerBO）。
 *
 * 不變式：
 *  - private 建構子 + 全靜態方法（無狀態 service facade）。
 *  - 依賴方向：BO → Cache → DAO → DTO。BO 不被 DAO/Cache 回呼。
 *  - 複雜邏輯須可用 fake DAO/Cache 測試，不得把狀態塞進 static global。
 *  - 交易邊界（取得/關閉連線）由 BO 持有，DAO 只收 Connection。
 */
public final class OrderBO {

    private OrderBO() {
        throw new AssertionError("no instance");
    }

    /** 讀路徑：先查 cache，miss 再回 DAO 並回填。 */
    public static Order getOrder(long id) {
        Order cached = OrderCache.getInstance().get(id);
        if (cached != null) {
            return cached;
        }
        try (Connection conn = ConnectionFactory.readConnection()) {
            Order order = OrderDAO.findById(conn, id);
            if (order != null) {
                OrderCache.getInstance().put(order);
            }
            return order;
        } catch (Exception e) {
            throw new RuntimeException("getOrder failed: " + id, e);
        }
    }

    public static List<Order> getOrdersByCustomer(long customerId) {
        try (Connection conn = ConnectionFactory.readConnection()) {
            return OrderDAO.findByCustomer(conn, customerId);
        } catch (Exception e) {
            throw new RuntimeException("getOrdersByCustomer failed: " + customerId, e);
        }
    }

    /** 寫路徑：DB 寫成功後 evict cache。 */
    public static void advanceStatus(long id, int statusValue) {
        try (Connection conn = ConnectionFactory.writeConnection()) {
            OrderDAO.updateStatus(conn, id, statusValue);
            OrderCache.getInstance().evict(id);
        } catch (Exception e) {
            throw new RuntimeException("advanceStatus failed: " + id, e);
        }
    }

    /** 範例用連線工廠（實務上對應 DBPool.MAIN）。 */
    interface ConnectionFactoryStub { }

    static final class ConnectionFactory {
        static Connection readConnection() {
            throw new UnsupportedOperationException("wire to real pool, e.g. DBPool.MAIN.getReadConnection()");
        }
        static Connection writeConnection() {
            throw new UnsupportedOperationException("wire to real pool, e.g. DBPool.MAIN.getWriteConnection()");
        }
    }
}
