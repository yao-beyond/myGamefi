# 系統結構範例 — Bitmask Server-Role Partitioning（CPS-08）

> 對應 scaffold：[`../scaffold/cps08/`](../scaffold/cps08/)
> 一份 codebase（單一 WAR），依「角色 bitmask」部署成多種 server，水平擴展。

---

## 1. 核心概念

整個系統只有**一份可部署檔**。每台機器啟動時被指派一個 **角色 bitmask（`serverType`）**，
決定它要拉起哪些 **Cache / Job / endpoint**。角色用 `ServerRole` enum 定義，
value 為 **2 的次方**，因此可用位元 OR 組合成「複合 server」。

```
mask & role.unique() > 0   ⇒  本機扮演該角色
```

判斷成員資格、啟用元件、跨 Cache 級聯通知，全部以此為準（見 `ServerRoleUtils`、`RoleGatedBootstrap`）。

---

## 2. 角色定義（ServerRole）

| 角色 | bitmask | 職責 | 啟用的 Job / endpoint |
|---|--:|---|---|
| `MAINTAIN` | -1 | 維護模式，不服務 | —（不參與組合） |
| `CUSTOMER` | 1 | 面向客戶的 API + WebSocket | `PriceBroadcast` / `/customer/` `/feed/` |
| `ADMIN` | 2 | 後台 / 營運 | `/admin/` |
| `TRANSACTION` | 4 | 交易 / 結算處理 | （內部） |
| `PRICE_INGEST` | 8 | 匯入外部供應商報價 | `SyncPrice` `SyncInstrument` / `/internal/price/` |
| `RESULT_INGEST` | 16 | 匯入外部供應商結果/結算 | `SyncResult` / `/internal/result/` |
| `CACHE` | 32 | 權威記憶體快取節點 | （載入 CPS-03 快取） |
| `SCHEDULER` | 64 | 批次 / 排程 | （cron jobs） |

> 真實系統可有更多角色（每接一個外部供應商就加一個 `*_INGEST` 常數，bitmask 繼續往上 ×2）。

---

## 3. 部署拓樸範例

```
                    ┌──────────────────────────────┐
                    │   單一 codebase（one WAR）     │
                    └──────────────┬───────────────┘
            部署到多台，每台帶不同 serverType bitmask
   ┌───────────────┬───────────────┬───────────────┬───────────────┐
   ▼               ▼               ▼               ▼               ▼
┌─────────┐   ┌─────────┐   ┌──────────────┐  ┌──────────────┐ ┌──────────┐
│CUSTOMER │   │CUSTOMER │   │ TRANSACTION  │  │ PRICE_INGEST │ │  CACHE   │
│  (1)    │   │  (1)    │   │     (4)      │  │     (8)      │ │  (32)    │
│ ×N 水平 │   │ ×N 水平 │   │              │  │              │ │ 權威快取 │
└────┬────┘   └────┬────┘   └──────┬───────┘  └──────┬───────┘ └────┬─────┘
     │             │               │                 │              │
     └─────────────┴───────────────┴────────┬────────┴──────────────┘
                                             ▼
                              ┌────────────────────────────┐
                              │  共享 DB（CPS-07 雙庫）       │
                              └────────────────────────────┘

複合 server 範例：小規模部署時用一台機器同時扮演多角色
   serverType = ServerRole.mask(CUSTOMER, CACHE, SCHEDULER) = 1|32|64 = 97
```

---

## 4. 角色如何 gate 行為

### (a) 啟動時依角色啟用元件
`RoleGatedBootstrap.boot(serverType)` 走訪 `currentRoles()`，
讀每個角色的 `ServerDefinition` 啟用其 `startupJobs()` 與 `exposedPaths()`。
→ 同一份 code，`PRICE_INGEST` server 只跑 `SyncPrice`，`CUSTOMER` server 只開 `/feed/`。

### (b) 跨 Cache 級聯只通知相關角色（呼應 CPS-03）
結算事件發生時，只對「本機實際扮演的角色」做後續處理：
```java
if (ServerRoleUtils.isCustomerServer())    { /* 失效 CustomerCache */ }
if (ServerRoleUtils.isTransactionServer()) { /* 更新 TransactionCache */ }
```
→ 不相關角色的 server 不做白工；級聯方向仍須維持 DAG（CPS-03 不變式）。

### (c) 權威 vs 唯讀
只有 `CACHE` server 載入權威記憶體快取；其餘 server 走遠端查詢或唯讀副本。

---

## 5. 部署矩陣（哪個角色跑什麼）

| 元件 | CUSTOMER | ADMIN | TRANSACTION | PRICE_INGEST | RESULT_INGEST | CACHE | SCHEDULER |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| 客戶 API / WebSocket（CPS-05） | ✅ | | | | | | |
| 後台 endpoint | | ✅ | | | | | |
| 交易/結算邏輯（CPS-06 BO） | | | ✅ | | | | |
| 報價同步 Job（CPS-01 Provider） | | | | ✅ | | | |
| 結果同步 Job | | | | | ✅ | | |
| 權威記憶體快取（CPS-03） | | | | | | ✅ | |
| 批次 cron | | | | | | | ✅ |

---

## 6. 不變式與驗收（conformance 重點）

- `ServerRole` value 必須 2 的次方（除 `MAINTAIN=-1`）；id 全域唯一 → 測試 `mask()` 組合可逆。
- 成員資格一律用 `(mask & role) > 0`，**禁止**用 `==` 比 bitmask（複合 server 會漏判）。
- `resolvePrimary()` 明確標示「不適用複合 server」；複合請用 `currentRoles()`。
- 角色 gate 不得改變 CPS-03 級聯的 DAG 性質（只是縮小通知對象）。
- 角色專屬狀態放 `ServerDefinition` / metadata 方法，**enum 不得持有 runtime 可變狀態**（沿用 CPS-02）。

---

## 7. 何時用 / 何時不要用

- **用**：單體或模組化單體（modular monolith）想用「同一份 code、多角色部署」做水平擴展與職責隔離，又不想拆成多個獨立服務與 repo。
- **不要用**：已是真正微服務（各服務獨立 codebase/部署管線）時，角色由部署單元天然隔離，不需要 bitmask。
- 角色數量成長到數十個、或角色間需獨立發版時，考慮拆分為獨立服務。
