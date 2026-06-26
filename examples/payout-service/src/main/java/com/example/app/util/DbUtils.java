package com.example.app.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** CPS-07 · 通用 JDBC 工具；與 vendor 無關的共用行為集中於此。 */
public final class DbUtils {

    private DbUtils() {
    }

    public static void closeAll(Connection conn, Statement st, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignore) { /* no-op */ }
        }
        if (st != null) {
            try { st.close(); } catch (SQLException ignore) { /* no-op */ }
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignore) { /* no-op */ }
        }
    }
}
