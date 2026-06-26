package com.example.app.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.app.dto.Payout;

/**
 * CPS-07 · DAO：只做 SQL + row mapping，全 PreparedStatement，連線由 BO 傳入。
 * 不得 import bo/cache/controller。
 */
public final class PayoutDAO {

    private PayoutDAO() {
    }

    public static long insertRequested(Connection conn, long customerId, long amount, int statusValue) {
        String sql = "INSERT INTO payout (customer_id, amount, status, updatedate) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.setLong(2, amount);
            ps.setInt(3, statusValue);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("insertRequested failed", e);
        }
    }

    public static Payout findById(Connection conn, long id) {
        String sql = "SELECT id, customer_id, amount, status, deleted, updatedate FROM payout WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + id, e);
        }
    }

    public static int updateStatus(Connection conn, long id, int statusValue) {
        String sql = "UPDATE payout SET status = ?, updatedate = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, statusValue);
            ps.setLong(2, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateStatus failed: " + id, e);
        }
    }

    private static Payout map(ResultSet rs) throws SQLException {
        return new Payout(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getLong("amount"),
                rs.getInt("status"),
                rs.getBoolean("deleted"),
                rs.getTimestamp("updatedate"));
    }
}
