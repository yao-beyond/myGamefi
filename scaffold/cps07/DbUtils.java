package com.example.scaffold.cps07;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * CPS-07 · 通用 JDBC 工具（對應 MyXchange 的 DbUtils）。
 * 與 vendor 無關的共用行為集中於此；方言差異交給 SqlDialect 實作。
 */
public final class DbUtils {

    private DbUtils() {
    }

    /** 安全關閉，不拋例外。 */
    public static void closeAll(Connection conn, Statement st, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignore) {
                // no-op
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignore) {
                // no-op
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignore) {
                // no-op
            }
        }
    }

    /** long[] → "1,2,3"，供 IN (...) 用（值來自系統，非 user input）。 */
    public static String getCommaString(long[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }
}
