package com.example.scaffold.cps07;

/**
 * CPS-07 · MySQL 方言實作（對應 myGameFi 的 MySqlUtils）。
 * 所有 MySQL 專屬語法只能出現在這裡。
 */
public final class MySqlUtils implements SqlDialect {

    public static final MySqlUtils INSTANCE = new MySqlUtils();

    private MySqlUtils() {
    }

    @Override
    public String limitOffset(int limit, int offset) {
        return " LIMIT " + offset + ", " + limit;
    }

    @Override
    public String nowFunction() {
        return "NOW()";
    }

    @Override
    public String upsert(String table, String[] columns, String[] conflictKeys) {
        String cols = String.join(", ", columns);
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            placeholders.append(i == 0 ? "?" : ", ?");
        }
        StringBuilder updates = new StringBuilder();
        for (String c : columns) {
            updates.append(updates.length() == 0 ? "" : ", ").append(c).append(" = VALUES(").append(c).append(")");
        }
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                + " ON DUPLICATE KEY UPDATE " + updates;
    }
}
