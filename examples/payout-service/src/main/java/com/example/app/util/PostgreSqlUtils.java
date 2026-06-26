package com.example.app.util;

/** CPS-07 · PostgreSQL 方言；所有 PostgreSQL 專屬語法只能出現在這裡。 */
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
}
