package com.example.scaffold.cps08;

/**
 * CPS-08 · 持有「本機 server 的角色 bitmask」的單例（對應 myGameFi 的 SystemInfo）。
 *
 * serverType 在啟動時從設定檔/環境變數/DB 載入一次（例如 CUSTOMER|CACHE = 33），
 * 之後唯讀。所有 ServerRoleUtils.isXxxServer() 都讀這裡。
 */
public final class SystemInfo {

    private static final SystemInfo INSTANCE = new SystemInfo();

    public static SystemInfo getInstance() {
        return INSTANCE;
    }

    /** 本機角色 bitmask；預設 MAINTAIN，啟動流程須呼叫 configure() 設定。 */
    private volatile int serverType = ServerRole.MAINTAIN.unique();

    private SystemInfo() {
    }

    /** 啟動時設定一次（來源：設定檔/環境變數/DB）。 */
    public void configure(int serverType) {
        this.serverType = serverType;
    }

    public int getServerType() {
        return serverType;
    }
}
