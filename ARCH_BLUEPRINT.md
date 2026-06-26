# ADAF — AI-Driven Architecture Framework

> 本文件是一套「架構生成憲法」。把它交給一個 AI（Claude / GPT），AI 依此即可在新專案中
> 重現 MyXchange 的架構風格與實作模式。
>
> **核心信條**：自然語言只描述「意圖」；真正能讓 AI *確定性重現* 的是
> **Manifest（領域宣告）→ Scaffold（樣板）→ Conformance（可機器檢查的規則）**。
> 風格不是品味，是規則。

---

## 0. 使用方式（AI 的進入點）

當 AI 接到一個新需求時，**嚴格依序**執行第 6 章「The Golden Flow」九步演算法。
每一步都有「輸入 / 產出 / 自我驗證點」，任何驗證未過必須回到該步修正，**不得只改報告**。

文件閱讀順序：本章 → 第 1 章原則 → 第 2 章命名契約 → 第 5 章決策樹 → 對應的第 3 章 Pattern Card → 第 6 章流程 → 第 7 章自審清單。

---

## 1. Architectural Philosophy（架構魂）

1. **State over Behavior**：狀態（Cache / Entity）是系統核心，行為（BO）環繞狀態。
2. **Enum is Logic**：型別分支邏輯一律收進 enum 多型，不散落 if-else / switch / 設定字串。
3. **Unidirectional Dependency**：依賴只能單向往下流，永不回呼。
4. **Pluggable via Registry**：同行為、多供應商 → enum 註冊 + Singleton Manager 熱切換。
5. **Convention is Contract**：類別後綴 = 層級身分，命名即契約，不可漂移。
6. **Verify by Machine**：每條規則都要能被 lint / ArchUnit / 測試檢查，否則不算規則。

### The "Nevers"（絕對禁令）
- ❌ 禁止為了「現代化」引入 Spring / DI container / ORM / reactive，除非 Manifest 明確允許。
- ❌ 禁止在 BO / DAO / Controller 用 `if (typeId == ...)` 或 `switch(typeId)` 判斷 domain state。
- ❌ 禁止 Controller 直接 import / 操作 DAO 或 Cache 內部結構。
- ❌ 禁止 DAO 回呼 BO / Cache / Controller。
- ❌ 禁止 Cache 級聯通知形成循環（必須是 DAG）。
- ❌ 禁止 Entity / DTO 內含業務運算（邏輯進 Type enum 或 BO）。
- ❌ 禁止在 enum 以外 `new XxxProvider()`。
- ❌ 禁止任何「無 owner、無 lock、無 reset policy」的全域 mutable 狀態。

---

## 2. The Lexicon & Taxonomy（命名契約與分類學）

| 後綴 | 角色 | 所在 package | 狀態 | 關鍵約束 |
|---|---|---|---|---|
| `*Controller` | HTTP Servlet 入口 | `controller` | 無狀態 | `@WebServlet`；只呼叫 BO |
| `*Service` | WebSocket / 對外服務 | `controller`/`service` | 連線狀態 | `@ServerEndpoint`；單執行緒廣播 |
| `*BO` | 業務邏輯聚合層 | `*/model` | 無狀態 | private 建構子 + 全靜態方法 |
| `*Cache` | 記憶體狀態持有者 | `*/model/cache` | 有狀態 | `extends AbstractCache` |
| `*Entity` | 記憶體領域物件 | `*/model` | 有狀態 | 巢狀組合，無業務運算 |
| `*DAO` | 資料訪問 | `commons/dao` | 無狀態 | 只做 SQL + row mapping |
| `*DTO`(=domain) | 資料轉移物件 | `commons/dto` | 純資料 | 不依賴任何上層 |
| `*Type` | 策略 enum | `commons/code` | 不可變 | 統一 enum 骨架 |
| `*Utils` | 靜態工具 | `commons/util`,`util` | 無狀態 | 純函式，方言隔離 |
| `*Manager` | 全域協調者 | `*/provider` | 單例有狀態 | Singleton + lock + 熱切換 |

**依賴方向（唯一合法流向）**：
```
Controller / WebSocketService → BO → Cache → DAO → DTO/Entity → DB
                                  ↘ Type enum / Utils（橫切，任何層可讀）
```
DTO/Entity 不得依賴 BO/Cache/DAO；DAO 不得依賴 BO/Cache/Controller。

---

## 3. Pattern Catalog（Pattern Cards）

> **CPS = Canonical Pattern Spec**：每個架構模式的規格編號（`CPS-01` ~ `CPS-08`）。
> blueprint、scaffold（`cpsNN/`）、conformance 測試三者以同一套編號互相對應。

每張卡片格式統一：**Intent / Trigger / Invariants / Naming / Structure / Min Example / Anti-Pattern / Acceptance**。

### CPS-01 · Provider-Enum-Registry + Singleton Manager
- **Intent**：同一組行為、多個可切換供應商，需運行時無停機熱切換。
- **Trigger**：出現 `if(provider==A)...else if(B)` 超過 2 處，或需求含「可換供應商 / 通道 / 廠商」。
- **Invariants**：
  1. 抽象基底為 **abstract class**，方法簽名固定（如 `init/syncX...`）。
  2. 供應商**只能**由 `*Type` enum 註冊；enum id 全域唯一且不可變。
  3. provider instance **lazy-init**；`getInstanceOf(int)` 對未知 id 行為固定（丟指定例外或回 fallback）。
  4. Manager 為 **Singleton**；active id **只能**從 DB `SystemSetting` 讀取。
  5. 熱切換 = **「新 provider init 成功後才 swap」**，swap 必須在 lock 內；失敗保留舊 provider。
  6. 多把 lock 取得**順序固定**（如 `apiDataProviderLock` → `apiPriceDataProviderLock`）。
  7. 對外只暴露委派方法，不暴露 mutable provider 欄位。
- **Naming**：`[Domain]Provider`(abstract) / `[Domain]ProviderType`(enum) / `[Domain]ProviderManager`(singleton)。
- **Structure**：enum 常數內嵌實例+元資料；Manager 持有當前實例 + 對應 lock。
- **Min Example**：
  ```java
  public abstract class PayProvider { public abstract void init(); public abstract Receipt charge(Order o); }
  public enum PayProviderType {
      STRIPE(1){ private final PayProvider p=new StripeProvider(); public PayProvider get(){return p;} },
      ALIPAY(2){ private final PayProvider p=new AlipayProvider(); public PayProvider get(){return p;} };
      private final int value; PayProviderType(int v){this.value=v;}
      public abstract PayProvider get();
      public static Optional<PayProviderType> getInstanceOf(int v){ for(var e:values()) if(e.value==v) return Optional.of(e); return Optional.empty(); }
  }
  ```
- **Anti-Pattern**：❌ Controller 內 `switch(type)`；❌ enum 外另寫 `ProviderUtils` 大量邏輯；❌ swap 在 init 之前。
- **Acceptance**：id uniqueness test；`getInstanceOf` roundtrip；hot-swap 並發測試（切換期間不 NPE、無半初始化）；init 失敗後舊 provider 仍可用；static check 禁止 enum 外 `new`。

### CPS-02 · Domain Type Enum Hierarchy
- **Intent**：用編譯期型別安全的 enum 多型取代 domain 分支邏輯與設定字串。
- **Trigger**：任何「固定狀態集 / 類型集 + 對應行為或述詞」的需求。
- **Invariants**：固定骨架 `private final int value` + `getValue()` + `static getInstanceOf(int)` + 必要 boolean 述詞 + 必要 abstract 業務方法；未知值處理策略統一；**enum 不得持有 DAO/Cache/Session/request 等 runtime 可變狀態**。
- **Naming**：`*Type`，置於 `commons/code`。
- **Anti-Pattern**：❌ 業務層用 raw int/string 判斷；❌ enum 持有可變狀態。
- **Acceptance**：每個常數 `getInstanceOf(getValue())==self`；id 不重複；lint 禁止 BO/DAO/Controller 出現 `switch(typeId)`；DB code table 與 enum 對齊測試。

### CPS-03 · AbstractCache + Timestamp Incremental Update
- **Intent**：大規模熱資料的記憶體快取，靠時間戳做增量同步。
- **Trigger**：高頻讀、資料量大、可接受最終一致性的領域狀態。
- **Invariants**：`AbstractCache` 定義 `init()/update()` 生命週期；`ConcurrentHashMap` 儲存；每 domain 一把 `ReentrantLock`；每類資料一個 `lastUpdateDate`（`volatile`/atomic）；查詢條件固定 `updatedate > lastUpdateDate - ERROR_VALUE`（ERROR_VALUE 預設 1000ms，或 Manifest 明示）；**update 必須 idempotent**；delete/tombstone 策略明確；級聯通知**只在本 cache 成功套用後**觸發，方向為 **DAG**；跨 cache 取 lock 須定義順序。
- **Naming**：`*Cache`，置於 `*/model/cache`。
- **Anti-Pattern**：❌ Cache 直接呼叫 Controller；❌ 假設 DB clock 與 app clock 一致而省略容差；❌ 循環級聯。
- **Acceptance**：fake DAO 測新增/修改/重複修改/時鐘回退 999ms/超容差；級聯順序測試；並發 update 不丟 `ConcurrentModificationException`。

### CPS-04 · Hierarchical Entity Nesting
- **Intent**：樹狀領域資料的記憶體模型，最佳化查詢路徑。
- **Trigger**：明確父子層級的領域（Category>Product>SKU、Org>Dept>Team>Member）。
- **Invariants**：層級明示（如 `MarketGroupEntity→EventEntity→MarketEntity→InstrumentEntity`）；每層 map key 型別固定（`Map<Long, EventEntity>`）；更新子節點必須維護父 map 一致性；不得繞過父層散落儲存，除非 Manifest 宣告 secondary index；讀路徑用固定 id 順序。
- **Naming**：`*Entity`。
- **Acceptance**：新增/刪除子節點後父層 map 同步測試；序列化無循環引用；Controller 不得直接操作 nested map。

### CPS-05 · Single-Thread WebSocket Broadcast
- **Intent**：實時多客戶端推送，無鎖、有序、固定頻率。
- **Trigger**：需要 server push 且可接受固定延遲與有限連線數。
- **Invariants**：`@ServerEndpoint`；連線集合 `ConcurrentHashMap<id, Session>`；廣播用**單一** `Executors.newSingleThreadExecutor()`；所有 outbound 經同一 queue；推送間隔固定（預設 250ms 或 Manifest 明示）；**WebSocket thread 不得跑 DAO/blocking sync**；send 失敗即移除連線。
- **Naming**：`*Service`。
- **Anti-Pattern**：❌ `newFixedThreadPool` 或每連線一 thread；❌ send path 直接打 DAO。
- **Acceptance**：同 client message order 測試；慢 client 移除測試；連線數壓測 + backlog/延遲指標。

### CPS-06 · Layered + Naming Convention（見第 2 章）
- **Acceptance**：ArchUnit 依賴方向測試；`*BO` private 建構子檢查；suffix↔package 對齊 lint；禁止 Controller import DAO、DAO import BO/Cache/Controller。

### CPS-07 · DAO + Dialect Utility Separation
- **Intent**：雙庫（PostgreSQL + MySQL）支援，方言差異集中隔離。
- **Invariants**：DAO 只做 SQL + row mapping；通用 JDBC 行為在 `DbUtils`；方言差異**只允許**進 `MySqlUtils`/`PostgreSqlUtils`；DAO 不得硬編 vendor 判斷；所有參數用 **prepared statement**；**不支援自動跨庫交易**（須明示 eventual consistency / 補償 / 禁跨庫寫）。
- **Acceptance**：lint 禁止 DAO 散落 vendor string；SQL injection 檢查（禁字串拼接 user input）；雙庫 integration test；DAO 不 import Servlet/BO。

### CPS-08 · Bitmask Server-Role Partitioning
- **Intent**：單一 codebase（一份部署檔）依「角色 bitmask」部署成多種 server，水平擴展且職責隔離。
- **Trigger**：模組化單體想用「同一份 code、多角色部署」擴展，又不想拆成多個獨立服務/repo。
- **Invariants**：角色用 `*Role` enum 定義（沿用 CPS-02 骨架），value 為 **2 的次方**（保留負值給維護模式）；本機角色為 int **bitmask**，可位元 OR 組合複合角色；成員資格一律 `(mask & role) > 0`，**禁止用 `==` 比 bitmask**；當前 mask 放單例（`SystemInfo`），**enum 不持有 runtime 可變狀態**；啟動依角色 gate 啟用 Cache/Job/endpoint；角色 gate 級聯**不得破壞 CPS-03 的 DAG**（只縮小通知對象）。
- **Naming**：`*Role`(enum) / `*RoleUtils`(判斷) / `SystemInfo`(持有 mask)。
- **Anti-Pattern**：❌ 用 `serverType == ROLE` 判斷（複合 server 漏判）；❌ 把角色狀態塞進 enum；❌ 已是真微服務還疊 bitmask。
- **Acceptance**：`mask()` 組合可逆測試；value 皆 2 的次方且唯一；`has()` 對複合 mask 正確；角色 gate 後級聯仍為 DAG。
- **完整範例**：[`system_design/system-structure.md`](system_design/system-structure.md) + scaffold `cps08/`。

---

## 4. Manifest / DSL（生成前置宣告）

AI 在寫任何 code 前，**先**把需求轉成此 YAML，杜絕自由發揮。未知項標 `TODO_DECISION`，不得自行猜測。

```yaml
providers:                        # → CPS-01
  - { name: Provider1, id: 1, priceProvider: true, defaultCurrency: GBP }
domainTypes:                      # → CPS-02
  - name: OrderStatusType
    values: [ { name: OPEN, id: 1 }, { name: MATCHED, id: 2 } ]
caches:                           # → CPS-03 / CPS-04
  - name: EventCache
    key: eventId
    updateColumn: updatedate
    errorValueMs: 1000
    hierarchy: [MarketGroup, Event, Market, Instrument]
    cascadeTo: [CustomerCache, TransactionCache]   # 必須 DAG
endpoints:                        # → CPS-05 / CPS-06
  - { name: MarketPriceService, kind: websocket, pushIntervalMs: 250 }
databases:                        # → CPS-07
  primary: postgresql
  secondary: mysql
  crossDbTx: false
serverRoles:                      # → CPS-08（bitmask 必須 2 的次方）
  - { name: CUSTOMER, mask: 1, jobs: [PriceBroadcast], paths: [/customer/, /feed/] }
  - { name: TRANSACTION, mask: 4 }
  - { name: PRICE_INGEST, mask: 8, jobs: [SyncPrice] }
  - { name: CACHE, mask: 32 }
```

---

## 5. Reasoning Decision Tree（選型導航）

```
新需求
├─ 同行為、多個可切換供應商/通道?  ── 是 → CPS-01 Provider-Enum-Registry
├─ 固定型別/狀態集 + 對應行為?      ── 是 → CPS-02 Domain Type Enum
├─ 需高頻讀的領域狀態?
│    ├─ 增量同步即可               → CPS-03 AbstractCache
│    └─ 明確父子樹狀結構           → CPS-04 Hierarchical Entity
├─ 需 server 實時推送?             ── 是 → CPS-05 WebSocket Broadcast
├─ 需存取 DB?                      ── 是 → CPS-07 DAO + Dialect Utils
├─ 單一 codebase 要多角色分區部署?  ── 是 → CPS-08 Bitmask Server-Role Partitioning
└─ 其他 → 標準 Layered CRUD（CPS-06 分層 + 命名）
```

---

## 6. The Golden Flow（AI 生成演算法，九步）

| # | 步驟 | 輸入 | 產出 | 自我驗證點 |
|---|---|---|---|---|
| 1 | 讀需求 → 寫 Manifest | 需求描述 | `manifest.yaml` 草案 | 所有 id/名稱/DB/cache/provider/type 已宣告；未知標 `TODO_DECISION` |
| 2 | 建 domain vocabulary | manifest | `*DTO`/`*Entity`/`*Type` 骨架 | 每個 `*Type` 有 value/getValue/getInstanceOf/述詞/abstract；id 無重複 |
| 3 | 生分層骨架 | vocabulary | `Controller→BO→Cache→DAO→DTO` 類別 | 依賴只往下；suffix 合規；`*BO` private 建構子 |
| 4 | 生 provider registry | manifest.providers | abstract base + `*Type` enum + `*Manager` | provider 只在 enum 註冊；init-success-then-swap；lock 順序固定 |
| 5 | 生 cache | manifest.caches + hierarchy | `AbstractCache` 子類 + nested map + cascade | query 用容差條件；update idempotent；cascade DAG；有 delete 策略 |
| 6 | 生 DAO + dialect utils | manifest.databases | `*DAO`/`DbUtils`/`MySqlUtils`/`PostgreSqlUtils` | prepared statement；vendor 差異只在 dialect util；DAO 不 import 上層 |
| 7 | 生 WebSocket/Controller | manifest.endpoints | `@ServerEndpoint` / Servlet | outbound 經單執行緒 executor；interval 明示；send 失敗移除；Controller 不打 DAO |
| 8 | 跑 conformance suite | 全部原始碼 | ArchUnit/lint/unit/it 結果 | 依賴/命名/enum 簽名/hot-swap/增量更新/方言隔離全過 |
| 9 | 產架構報告 | manifest + 測試 | `ARCHITECTURE_CONFORMANCE.md` | 列每個 invariant pass/fail；fail 回對應步驟修正 |

---

## 7. Verification Checklist（生成後 AI 必須逐項自答 Yes/No）

- [ ] 依賴方向只往下，無回呼？（CPS-06）
- [ ] 所有類別後綴與 package 對齊？（第 2 章）
- [ ] 所有 `*Type` 符合統一 enum 骨架、id 唯一？（CPS-02）
- [ ] 沒有任何 `switch(typeId)` / raw int 分支落在 BO/DAO/Controller？
- [ ] Provider 只在 enum 註冊、Manager 是 init-then-swap + 固定 lock 順序？（CPS-01）
- [ ] Cache update idempotent、有容差、級聯為 DAG、有 delete 策略？（CPS-03）
- [ ] WebSocket 單執行緒廣播、不在推送 thread 打 DAO？（CPS-05）
- [ ] DAO 全 prepared statement、方言只在 dialect util？（CPS-07）
- [ ] 沒有偷偷引入 Spring/DI/ORM/reactive？
- [ ] 每個全域 mutable 狀態都有 owner/lock/reset policy？

---

## 8. Guardrails — 避免 AI 複製歷史包袱（反模式區）

> 這套架構有歷史權衡，AI **不可把缺點當最佳實踐放大**。下列為強制護欄：

- `*Type` enum 只封裝穩定 domain code，**禁持有** DAO/Cache/Session/request/可變狀態。
- provider lazy instance **必須 idempotent init**；若有連線池/thread/socket，**必須定義 shutdown/reload**，否則熱切換洩漏資源。
- static BO 僅作 service facade；複雜邏輯須可用 fake DAO/Cache 測試，**不得把狀態塞進 static global**。
- Singleton Manager / Cache **必須有 test reset hook**，否則測試互相污染。
- 時間戳增量更新**不得假設 DB clock == app clock**；高一致性場景改用 monotonic version / sequence / CDC。
- 單執行緒 WebSocket 廣播只適合**固定延遲可接受 + 有限連線數**；必須有 backlog/queue size/慢 client 指標。
- 雙庫**不得暗示自動跨庫交易一致性**；明示 eventual consistency / 補償 / 禁跨庫寫。

---

## 9. 推薦包裝（如何把本框架落地給 AI）

雙重包裝，優先序由高到低：

1. **`scaffold/` + reference implementation（最高優先）** — 每個 CPS 一份完整可編譯樣板（AI 最擅長依樣畫葫蘆）。
2. **`manifest.yaml`（生成前置）** — 強制 AI 先宣告領域。
3. **conformance suite（防漂移關鍵）** — ArchUnit / Checkstyle / PMD / regex lint / unit / integration。
4. **`.claude/` 指令濃縮 + 本 `ARCH_BLUEPRINT.md`（解釋意圖與反模式）** — 自然語言只補充「為什麼」，不作唯一規格。
5. **`few-shot/`（Before/After 對比）** — AI 對「改錯」學習效果優於「讀定義」。

> 一句話總結：**把「品味」翻譯成「規則」**。不要告訴 AI「這是亮點」，要告訴它
> 「看到供應商切換，就寫一個繼承 abstract base 的 enum 註冊表，Manager 必須 init-success-then-swap 且 lock 順序固定」——它就能 100% 重現架構精髓。
