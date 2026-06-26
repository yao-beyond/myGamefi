package com.example.app.util;

import com.example.app.code.ServerRole;

/** CPS-08 · 角色判斷：成員資格一律用 bitwise (mask & role) > 0，禁止用 == 比 bitmask。 */
public final class ServerRoleUtils {

    private ServerRoleUtils() {
    }

    public static boolean has(int mask, ServerRole role) {
        return (mask & role.unique()) > 0;
    }

    public static boolean is(ServerRole role) {
        return has(SystemInfo.getInstance().getServerType(), role);
    }

    public static boolean isTransactionServer() {
        return is(ServerRole.TRANSACTION);
    }

    public static boolean isCustomerServer() {
        return is(ServerRole.CUSTOMER);
    }

    public static boolean isCacheServer() {
        return is(ServerRole.CACHE);
    }
}
