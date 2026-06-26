package com.example.scaffold.cps08;

/**
 * CPS-08 · 角色 gate 啟動範例：同一份 codebase，依本機 serverType 決定要拉起什麼。
 *
 * 示範三種 gating：
 *  1. 啟動時依角色啟用 Cache / Job / endpoint（讀各角色的 ServerDefinition）。
 *  2. 跨 Cache 級聯時用 is(ROLE) 判斷（呼應 CPS-03：只在相關角色的 server 上通知）。
 *  3. 複合 server（CUSTOMER|CACHE）會同時滿足多個 gate。
 */
public final class RoleGatedBootstrap {

    private RoleGatedBootstrap() {
    }

    /** 應用啟動時呼叫。 */
    public static void boot(int serverType) {
        SystemInfo.getInstance().configure(serverType);

        // 1. 依「本機扮演的每個角色」啟用其 startupJobs 與 endpoints
        for (ServerRole role : ServerRoleUtils.currentRoles()) {
            role.getDefinition().ifPresent(def -> {
                for (String job : def.startupJobs()) {
                    startJob(job);
                }
                for (String path : def.exposedPaths()) {
                    exposeEndpoint(path);
                }
            });
        }

        // 2. 只有 CACHE server 載入權威記憶體快取（其餘 server 走遠端/唯讀）
        if (ServerRoleUtils.isCacheServer()) {
            warmAuthoritativeCaches();
        }
    }

    /**
     * 角色 gate 的級聯通知（對應 CPS-03 的 EventCache → 下游 cache）。
     * 結算事件只通知「實際扮演該角色」的本機元件，避免在不相關的 server 上做白工。
     */
    public static void onSettlement(long entityId) {
        if (ServerRoleUtils.isCustomerServer()) {
            // CustomerCache.getInstance().invalidate(entityId);
        }
        if (ServerRoleUtils.isTransactionServer()) {
            // TransactionCache.getInstance().markSettled(entityId);
        }
    }

    // ---- 下列為示意 stub ----
    private static void startJob(String name) { /* scheduler.register(name) */ }
    private static void exposeEndpoint(String path) { /* router.mount(path) */ }
    private static void warmAuthoritativeCaches() { /* ProductCache.getInstance().init() */ }
}
