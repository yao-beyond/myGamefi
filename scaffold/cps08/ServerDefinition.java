package com.example.scaffold.cps08;

/**
 * CPS-08 · 角色專屬設定/行為的合約。
 * 由需要額外設定的 ServerRole 常數透過 getDefinition() 提供（其餘回 Optional.empty()）。
 */
public interface ServerDefinition {

    /** 該角色啟動時要拉起的背景工作（排程/同步迴圈）識別名。 */
    String[] startupJobs();

    /** 該角色對外暴露的 endpoint 路徑前綴。 */
    String[] exposedPaths();
}
