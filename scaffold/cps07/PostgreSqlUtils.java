package com.example.scaffold.cps07;

/**
 * CPS-07 · PostgreSQL 方言實作（對應 MyXchange 的 PostgreSqlUtils）。
 * 所有 PostgreSQL 專屬語法只能出現在這裡。
 */
public final class PostgreSqlUtils implements SqlDialect {

    public static final PostgreSqlUtils INSTANCE = new PostgreSqlUtils();

    private PostgreSqlUtils() {
    }

    @Override
    public String limitOffset(int limit, int offset) {
        return " LIMIT " + limit + " OFFSET " + offset;
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
            updates.append(updates.length() == 0 ? "" : ", ").append(c).append(" = EXCLUDED.").append(c);
        }
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                + " ON CONFLICT (" + String.join(", ", conflictKeys) + ") DO UPDATE SET " + updates;
    }
}
