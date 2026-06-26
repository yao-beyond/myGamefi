package com.example.scaffold.cps07;

/**
 * CPS-07 · 方言合約。DAO 透過此介面取得方言化 SQL 片段，
 * 不在 DAO 內硬編 vendor 判斷。
 */
public interface SqlDialect {

    /** 分頁片段（PostgreSQL 與 MySQL 寫法一致，但保留隔離點）。 */
    String limitOffset(int limit, int offset);

    /** 取得目前時間戳函式名。 */
    String nowFunction();

    /** upsert 片段（兩庫語法不同，差異集中於此）。 */
    String upsert(String table, String[] columns, String[] conflictKeys);
}
