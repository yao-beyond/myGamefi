# ADAF Scaffold — 8 個 CPS 的 Reference Implementation

> CPS = Canonical Pattern Spec（架構模式規格編號 CPS-01 ~ CPS-08）。

這些是 [`../ARCH_BLUEPRINT.md`](../ARCH_BLUEPRINT.md) 第 3 章各模式的**參考樣板**。
它們是「依樣畫葫蘆」的範本，**不接進 Maven build**（package 用 `com.example.scaffold.*` 為教學用）。
AI 生成新程式碼時，應對照對應 CPS 樣板，套用相同結構、命名與不變式。

CPS-01~07 用一致的電商領域串成連貫範例：`Payment`(供應商) / `Order`(分層+方言) / `Product`(快取) / `Catalog`(階層) / `PriceFeed`(推送)；
CPS-08 為部署結構，對應系統結構文件 [`../system_design/system-structure.md`](../system_design/system-structure.md)。

| CPS | 模式 | 資料夾 | 關鍵檔案 |
|---|---|---|---|
| 01 | Provider-Enum-Registry + Singleton Manager | `cps01/` | `PayProvider`, `PayProviderType`, `PayProviderManager` |
| 02 | Domain Type Enum | `cps02/` | `OrderStatusType` |
| 03 | AbstractCache + 時間戳增量更新 | `cps03/` | `AbstractCache`, `ProductCache` |
| 04 | Hierarchical Entity Nesting | `cps04/` | `CategoryEntity` → `ProductEntity` → `SkuEntity`, `CatalogCache` |
| 05 | Single-Thread WebSocket Broadcast | `cps05/` | `PriceFeedService` |
| 06 | Layered + Naming（垂直切片） | `cps06/` | `OrderController` → `OrderBO` → `OrderCache` → `OrderDAO` → `Order` |
| 07 | DAO + Dialect Utility 分離 | `cps07/` | `DbUtils`, `MySqlUtils`, `PostgreSqlUtils`, `OrderDialectDAO` |
| 08 | Bitmask Server-Role Partitioning | `cps08/` | `ServerRole`, `ServerRoleUtils`, `SystemInfo`, `RoleGatedBootstrap` |

每份樣板開頭都標註它示範的**不變式（Invariants）**，對照 blueprint 的 Acceptance 條件即可寫 conformance 測試。

## conformance/ — ArchUnit 測試
`conformance/ArchitectureConformanceTest.java` 把 blueprint 第 7 章 checklist 變成 11 條可機器檢查的 fitness functions（依賴方向、命名↔package、`*BO` private 建構子、`*Type` enum、Manager 欄位封裝、禁 `new Provider`、禁 Spring）。
已用合規 fixture 實跑通過、並以注入違規驗證會擋。用法與 pom 依賴見 [`conformance/README.md`](conformance/README.md)。
