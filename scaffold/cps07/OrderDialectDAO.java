package com.example.scaffold.cps07;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CPS-07 · 使用方言工具的 DAO（雙庫支援）。
 *
 * 不變式：
 *  - DAO 只做 SQL + row mapping；不硬編 vendor 判斷，差異全走 SqlDialect。
 *  - 全部 PreparedStatement，user input 一律參數化（禁止字串拼接）。
 *  - 不支援自動跨庫交易：本 DAO 綁定單一 Connection 的 vendor。
 *  - 不得 import BO / Cache / Controller / Servlet。
 */
public final class OrderDialectDAO {

    private final SqlDialect dialect;

    /** vendor 由建構時注入（呼叫端依連線來源選 MySqlUtils / PostgreSqlUtils）。 */
    public OrderDialectDAO(SqlDialect dialect) {
        this.dialect = dialect;
    }

    public List<long[]> pageOrderIds(Connection conn, long customerId, int limit, int offset) {
        String sql = "SELECT id FROM orders WHERE customer_id = ?" + dialect.limitOffset(limit, offset);
        List<long[]> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);                    // user input 參數化
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(new long[]{rs.getLong("id")});
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("pageOrderIds failed", e);
        }
        return ids;
    }

    public void upsertOrder(Connection conn, long id, long customerId, long amount) {
        String sql = dialect.upsert(
                "orders",
                new String[]{"id", "customer_id", "amount"},
                new String[]{"id"});
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, customerId);
            ps.setLong(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("upsertOrder failed: " + id, e);
        }
    }
}
