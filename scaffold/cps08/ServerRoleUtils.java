package com.example.scaffold.cps08;

import java.util.ArrayList;
import java.util.List;

/**
 * CPS-08 · 角色判斷工具（對應 MyXchange 的 ServerInfoUtils）。
 *
 * 不變式：
 *  - 成員資格一律用 bitwise：has(mask, role) = (mask & role.unique()) > 0。
 *  - isXxxServer() 無參數版讀 SystemInfo.getInstance().getServerType()。
 *  - resolvePrimary() 用固定優先序解析「單一主要角色」，明確標示不適用複合 server。
 */
public final class ServerRoleUtils {

    private ServerRoleUtils() {
    }

    /** 任意 mask 是否包含某角色。 */
    public static boolean has(int mask, ServerRole role) {
        return (mask & role.unique()) > 0;
    }

    /** 本機是否扮演某角色。 */
    public static boolean is(ServerRole role) {
        return has(SystemInfo.getInstance().getServerType(), role);
    }

    // ---- 語意化便捷方法（對應 isCustomerServer() 等）----
    public static boolean isCustomerServer() { return is(ServerRole.CUSTOMER); }
    public static boolean isAdminServer()    { return is(ServerRole.ADMIN); }
    public static boolean isTransactionServer() { return is(ServerRole.TRANSACTION); }
    public static boolean isPriceIngestServer() { return is(ServerRole.PRICE_INGEST); }
    public static boolean isResultIngestServer() { return is(ServerRole.RESULT_INGEST); }
    public static boolean isCacheServer()    { return is(ServerRole.CACHE); }
    public static boolean isSchedulerServer() { return is(ServerRole.SCHEDULER); }

    /** 列出本機扮演的所有角色（複合 server 會有多個）。 */
    public static List<ServerRole> currentRoles() {
        int mask = SystemInfo.getInstance().getServerType();
        List<ServerRole> roles = new ArrayList<>();
        for (ServerRole r : ServerRole.VALUES) {
            if (r.unique() > 0 && has(mask, r)) {
                roles.add(r);
            }
        }
        return roles;
    }

    /**
     * 解析「單一主要角色」（固定優先序）。
     * 注意：只考慮單一 Server，不適用於複合式 Server——複合請用 currentRoles()。
     */
    public static ServerRole resolvePrimary() {
        ServerRole[] priority = {
                ServerRole.CUSTOMER, ServerRole.ADMIN, ServerRole.TRANSACTION,
                ServerRole.PRICE_INGEST, ServerRole.RESULT_INGEST,
                ServerRole.CACHE, ServerRole.SCHEDULER
        };
        for (ServerRole r : priority) {
            if (is(r)) {
                return r;
            }
        }
        return ServerRole.MAINTAIN;
    }
}
