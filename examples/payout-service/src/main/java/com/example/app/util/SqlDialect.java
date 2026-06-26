package com.example.app.util;

/** CPS-07 · 方言合約：DAO 透過此取得方言化片段，不在 DAO 硬編 vendor 判斷。 */
public interface SqlDialect {

    String limitOffset(int limit, int offset);

    String nowFunction();
}
