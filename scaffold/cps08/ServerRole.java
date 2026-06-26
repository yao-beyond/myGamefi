package com.example.scaffold.cps08;

import java.util.Optional;

/**
 * CPS-08 · Bitmask Server-Role Partitioning（對應 myGameFi 的 ServerInfoType）。
 *
 * 核心：單一 codebase（一份 WAR）部署到多台機器；每台用一個 int bitmask 標記
 *      自己扮演哪些角色，啟動時據此 gate「要開哪些 Cache / Job / endpoint」。
 *
 * 不變式（沿用 CPS-02 的 Type Enum 骨架，再加 bitmask 語意）：
 *  - 每個角色一個 enum 常數；value 必須是 2 的次方（1,2,4,8,...），保留 -1 給 MAINTAIN。
 *    → 才能用位元 OR 組合「複合角色」，用 (mask & role) > 0 測試成員資格。
 *  - value 全域唯一、不可變、對齊 DB / 設定檔。
 *  - getInstanceOf(int) 為「單一角色」精確反查；複合 mask 用 ServerRoleUtils.has()。
 *  - 角色專屬 metadata 用 constant-specific method body 內聚（描述、逾時、定義）。
 *  - enum 不得持有 runtime 可變狀態（當前 server 的 mask 放在 SystemInfo）。
 */
public enum ServerRole {

    /** 維護模式：不提供服務。value = -1（不參與 bitmask 組合）。 */
    MAINTAIN(-1) {
        @Override public String getDescription() { return "Maintenance Server"; }
    },

    /** 面向客戶的 API + WebSocket。 */
    CUSTOMER(1) {
        @Override public String getDescription() { return "Customer-facing Server"; }
        @Override public long getHeartbeatAbandonSeconds() { return 30; }
        @Override public Optional<ServerDefinition> getDefinition() {
            return Optional.of(new ServerDefinition() {
                @Override public String[] startupJobs() { return new String[]{"PriceBroadcast"}; }
                @Override public String[] exposedPaths() { return new String[]{"/customer/", "/feed/"}; }
            });
        }
    },

    /** 後台 / 營運管理。 */
    ADMIN(2) {
        @Override public String getDescription() { return "Back-office Admin Server"; }
        @Override public Optional<ServerDefinition> getDefinition() {
            return Optional.of(new ServerDefinition() {
                @Override public String[] startupJobs() { return new String[0]; }
                @Override public String[] exposedPaths() { return new String[]{"/admin/"}; }
            });
        }
    },

    /** 交易 / 結算處理。 */
    TRANSACTION(4) {
        @Override public String getDescription() { return "Transaction/Settlement Server"; }
        @Override public long getHeartbeatAbandonSeconds() { return 10; }
    },

    /** 外部供應商「報價」資料的匯入（outbound integration）。 */
    PRICE_INGEST(8) {
        @Override public String getDescription() { return "Price-ingest Server"; }
        @Override public Optional<ServerDefinition> getDefinition() {
            return Optional.of(new ServerDefinition() {
                @Override public String[] startupJobs() { return new String[]{"SyncPrice", "SyncInstrument"}; }
                @Override public String[] exposedPaths() { return new String[]{"/internal/price/"}; }
            });
        }
    },

    /** 外部供應商「結果 / 結算」資料的匯入。 */
    RESULT_INGEST(16) {
        @Override public String getDescription() { return "Result-ingest Server"; }
        @Override public Optional<ServerDefinition> getDefinition() {
            return Optional.of(new ServerDefinition() {
                @Override public String[] startupJobs() { return new String[]{"SyncResult"}; }
                @Override public String[] exposedPaths() { return new String[]{"/internal/result/"}; }
            });
        }
    },

    /** 共享記憶體快取節點（CPS-03 的 Cache 在此 server 為權威來源）。 */
    CACHE(32) {
        @Override public String getDescription() { return "Memory Cache Server"; }
    },

    /** 批次 / 排程工作。 */
    SCHEDULER(64) {
        @Override public String getDescription() { return "Scheduler Server"; }
    };

    public static final ServerRole[] VALUES = values();

    private final int value;

    ServerRole(int value) {
        this.value = value;
    }

    /** bitmask 值（2 的次方）。 */
    public int unique() {
        return value;
    }

    // ---- 角色專屬 metadata（預設值在此，個別角色覆寫）----
    public long getHeartbeatAbandonSeconds() {
        return 60;
    }

    public Optional<ServerDefinition> getDefinition() {
        return Optional.empty();
    }

    public abstract String getDescription();

    /** 單一角色精確反查（複合 mask 請用 ServerRoleUtils.has()）。 */
    public static Optional<ServerRole> getInstanceOf(int value) {
        for (ServerRole e : VALUES) {
            if (e.value == value) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /** 把多個角色組合成一個 bitmask（複合 server 用）。 */
    public static int mask(ServerRole... roles) {
        int m = 0;
        for (ServerRole r : roles) {
            if (r.value > 0) {       // MAINTAIN(-1) 不參與組合
                m |= r.value;
            }
        }
        return m;
    }
}
