package com.example.app.util;

/** CPS-07 · MySQL 方言；所有 MySQL 專屬語法只能出現在這裡。 */
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
}
