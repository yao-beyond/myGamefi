package com.example.app.util;

import com.example.app.code.ServerRole;

/** CPS-08 · 持有本機 server 角色 bitmask 的單例（啟動時設定一次，之後唯讀）。 */
public final class SystemInfo {

    private static final SystemInfo INSTANCE = new SystemInfo();

    public static SystemInfo getInstance() {
        return INSTANCE;
    }

    private volatile int serverType = ServerRole.MAINTAIN.unique();

    private SystemInfo() {
    }

    public void configure(int serverType) {
        this.serverType = serverType;
    }

    public int getServerType() {
        return serverType;
    }
}
