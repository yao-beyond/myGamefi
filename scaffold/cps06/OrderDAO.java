package com.example.scaffold.cps06;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CPS-06 · DAO 層。只做 SQL + row mapping。
 *
 * 不變式：
 *  - 不得 import / 呼叫 BO / Cache / Controller（依賴只能更往下到 DB）。
 *  - 全部用 PreparedStatement（禁止字串拼接 user input）。
 *  - 連線由呼叫端（BO）傳入，DAO 不管理交易邊界。
 */
public final class OrderDAO {

    private OrderDAO() {
    }

    public static Order findById(Connection conn, long id) {
        String sql = "SELECT id, customer_id, amount, status FROM orders WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + id, e);
        }
    }

    public static List<Order> findByCustomer(Connection conn, long customerId) {
        String sql = "SELECT id, customer_id, amount, status FROM orders WHERE customer_id = ?";
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByCustomer failed: " + customerId, e);
        }
        return list;
    }

    public static int updateStatus(Connection conn, long id, int statusValue) {
        String sql = "UPDATE orders SET status = ?, updatedate = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, statusValue);
            ps.setLong(2, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateStatus failed: " + id, e);
        }
    }

    private static Order map(ResultSet rs) throws SQLException {
        return new Order(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getLong("amount"),
                rs.getInt("status"));
    }
}
