# USAGE — 怎麼用 ADAF 框架（含完整 worked example）

> 先讀 [`README.md`](README.md) 了解全貌，本檔給你**可照抄的實戰流程**。
> 心智模型：這不是你 `import` 的程式庫，而是**交給 AI 的架構生成規格**——
> Blueprint（憲法）→ Scaffold（樣板）→ Conformance（機器驗證）。

---

## 0. 三種用法速覽

| 用法 | 做什麼 | 看哪裡 |
|---|---|---|
| **A. 讓 AI 依框架產生程式** | 把 `ARCH_BLUEPRINT.md` 放進 AI context，下需求要它走 Golden Flow | 本檔 §2 worked example |
| **B. 人工照樣板寫** | 複製 `scaffold/cpsNN/` 當範本 | [`scaffold/README.md`](scaffold/README.md) |
| **C. CI 防漂移** | 加 ArchUnit 測試擋架構走樣 | [`scaffold/conformance/README.md`](scaffold/conformance/README.md) |

把框架接進你的專案（用法 A）——在目標專案放一份 `CLAUDE.md` / `AGENTS.md`：

```markdown
本專案架構一律遵循 ADAF 框架（見 ARCH_BLUEPRINT.md）：
- 命名契約見第 2 章，模式見第 3 章 Pattern Cards。
- 接到需求時，嚴格依第 6 章 The Golden Flow 九步執行；未過 conformance 不得宣告完成。
- base package：com.yourcompany.yourapp
```

---

## 1. 決策樹（先選模式）

```
新需求
├─ 同行為、多個可切換供應商/通道?  → CPS-01 Provider-Enum-Registry
├─ 固定型別/狀態集 + 對應行為?      → CPS-02 Domain Type Enum
├─ 高頻讀的領域狀態（增量同步）?     → CPS-03 AbstractCache
├─ 明確父子樹狀結構?               → CPS-04 Hierarchical Entity
├─ server 實時推送?               → CPS-05 WebSocket Broadcast
├─ 需存取 DB?                     → CPS-07 DAO + Dialect Utils
├─ 單一 codebase 多角色部署?       → CPS-08 Bitmask Server-Role
└─ 其他                          → CPS-06 標準分層 + 命名
```

---

## 2. Worked Example — 從零加一個「Payout（出款）」功能

**情境**：客戶可申請出款；出款透過**可切換的外部出款供應商**送出（CPS-01）；
出款有**狀態機**（CPS-02）；近期出款**快取**供查詢（CPS-03）；走**標準分層**（CPS-06）；
**雙庫方言**（CPS-07）；只在 **TRANSACTION 角色** server 處理（CPS-08）。

> 下面嚴格照 `ARCH_BLUEPRINT.md` 第 6 章 The Golden Flow 九步，每步附「✅ 自我驗證點」。
> base package 以 `com.example.app` 示意，命名全部遵守第 2 章契約。

### Step 1 — 讀需求 → 寫 Manifest
先把需求宣告成 YAML，杜絕自由發揮；未知項標 `TODO_DECISION`。

```yaml
domainTypes:                       # → CPS-02
  - name: PayoutStatusType
    values:
      - { name: REQUESTED, id: 1 }
      - { name: SENT,      id: 2 }
      - { name: CONFIRMED, id: 3 }
      - { name: REJECTED,  id: 4 }
providers:                         # → CPS-01
  - { name: BankWire, id: 1, defaultCurrency: USD }
  - { name: EWallet,  id: 2, defaultCurrency: USD }
caches:                            # → CPS-03
  - name: PayoutCache
    key: payoutId
    updateColumn: updatedate
    errorValueMs: 1000
    cascadeTo: []                  # 無下游；若有須維持 DAG
databases:                         # → CPS-07
  primary: postgresql
  secondary: mysql
  crossDbTx: false
serverRoles:                       # → CPS-08
  - { name: TRANSACTION, mask: 4 }
```
✅ 所有 id 已宣告且唯一；跨庫交易明示為 false；未知項無遺漏。

### Step 2 — domain 詞彙（DTO / Type）
DTO 是純資料，狀態機收進 `*Type` enum（取代 if/switch）。

```java
// dto/Payout.java — 純資料，不依賴任何上層
public final class Payout {
    private final long id; private final long customerId;
    private final long amount; private final int statusValue;
    public Payout(long id, long customerId, long amount, int statusValue) {
        this.id = id; this.customerId = customerId; this.amount = amount; this.statusValue = statusValue;
    }
    public long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public long getAmount() { return amount; }
    public int getStatusValue() { return statusValue; }
}
```
```java
// code/PayoutStatusType.java — CPS-02 統一骨架
public enum PayoutStatusType {
    REQUESTED(1){ public boolean isTerminal(){return false;} public boolean canCancel(){return true;} },
    SENT(2)     { public boolean isTerminal(){return false;} public boolean canCancel(){return false;} },
    CONFIRMED(3){ public boolean isTerminal(){return true;}  public boolean canCancel(){return false;} },
    REJECTED(4) { public boolean isTerminal(){return true;}  public boolean canCancel(){return false;} };
    private static final PayoutStatusType[] VALUES = values();
    private final int value; PayoutStatusType(int v){ this.value = v; }
    public int getValue(){ return value; }
    public abstract boolean isTerminal();
    public abstract boolean canCancel();
    public static java.util.Optional<PayoutStatusType> getInstanceOf(int v){
        for (PayoutStatusType e : VALUES) if (e.value==v) return java.util.Optional.of(e);
        return java.util.Optional.empty();
    }
}
```
✅ 每個 `*Type` 有 value/getValue/getInstanceOf/述詞/abstract；id 唯一；無 runtime 可變狀態。

### Step 3 — 分層骨架
依賴方向 Controller→BO→Cache→DAO→DTO，後綴↔package 對齊（建在 §3 之後逐一填）。
✅ 依賴只往下；命名 suffix 合規；`*BO` private 建構子。

### Step 4 — provider registry（CPS-01）
```java
// provider/PayoutProvider.java
public abstract class PayoutProvider {
    public abstract void init();
    public abstract void shutdown();
    public abstract String send(Payout p);   // 回傳供應商交易號
}
// provider/BankWireProvider.java / EWalletProvider.java — 各自 extends，唯一允許在 enum 內 new
```
```java
// code/PayoutProviderType.java — 註冊表（enum 常數內嵌 lazy 實例 + 元資料）
public enum PayoutProviderType {
    BANK_WIRE(1){ private PayoutProvider p; public synchronized PayoutProvider getProvider(){ if(p==null) p=new BankWireProvider(); return p; } },
    EWALLET(2)  { private PayoutProvider p; public synchronized PayoutProvider getProvider(){ if(p==null) p=new EWalletProvider();  return p; } };
    private static final PayoutProviderType[] VALUES = values();
    private final int value; PayoutProviderType(int v){ this.value = v; }
    public int getValue(){ return value; }
    public abstract PayoutProvider getProvider();
    public static java.util.Optional<PayoutProviderType> getInstanceOf(int v){
        for (PayoutProviderType e : VALUES) if (e.value==v) return java.util.Optional.of(e);
        return java.util.Optional.empty();
    }
}
// provider/PayoutProviderManager.java — singleton，從 DB 設定讀 active id → init 成功後才 swap（在 lock 內）
```
✅ provider 只在 enum `new`；Manager init-success-then-swap；失敗保留舊 provider；Manager 欄位 private。

### Step 5 — cache（CPS-03）
```java
// cache/PayoutCache.java — extends AbstractCache
//   ConcurrentHashMap + ReentrantLock + lastUpdateDate(volatile)
//   query: updatedate > lastUpdateDate - ERROR_VALUE(1000ms)；update idempotent；delete 用 tombstone
```
✅ 容差查詢；update 冪等；無下游級聯（若有須 DAG）；有 test reset hook。

### Step 6 — DAO + dialect（CPS-07）
```java
// dao/PayoutDAO.java — 只做 SQL + row mapping，全 PreparedStatement，連線由 BO 傳入
public final class PayoutDAO {
    private PayoutDAO(){}
    public static Payout findById(java.sql.Connection conn, long id){ /* SELECT ... WHERE id=? */ return null; }
    public static int updateStatus(java.sql.Connection conn, long id, int status){ /* UPDATE ... */ return 0; }
}
// 方言差異（分頁/upsert）只進 MySqlUtils / PostgreSqlUtils（見 scaffold/cps07）
```
✅ 全 prepared statement；vendor 差異只在 dialect util；DAO 不 import BO/Cache/Controller。

### Step 7 — BO + Controller（CPS-06）
```java
// bo/PayoutBO.java — private 建構子 + 全靜態；持有交易邊界
public final class PayoutBO {
    private PayoutBO(){ throw new AssertionError(); }
    public static long requestPayout(long customerId, long amount){
        // 1) 建立 REQUESTED 紀錄(DAO) 2) 委派 PayoutProviderManager.send() 3) 更新狀態 4) evict cache
        return 0L;
    }
    public static Payout getPayout(long id){
        Payout c = PayoutCache.getInstance().get(id);
        if (c != null) return c;
        // miss → DAO 查 → 回填 cache
        return c;
    }
}
```
```java
// controller/PayoutController.java — @WebServlet，只呼叫 BO，禁碰 DAO/Cache
@WebServlet(urlPatterns = "/customer/payoutController/*")
public final class PayoutController extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (req.getPathInfo() == null ? "" : req.getPathInfo()) {
            case "/request": PayoutBO.requestPayout(
                Long.parseLong(req.getParameter("customerId")),
                Long.parseLong(req.getParameter("amount"))); break;
            case "/get": /* PayoutBO.getPayout(...) → 組 JSON */ break;
            default: resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
```
✅ Controller 只委派 BO；無業務邏輯；無直接 DAO/Cache 呼叫。

### Step 8 — 跑 conformance（CPS-06/01/02…）
```bash
mvn test -Dadaf.basePackage=com.example.app
```
應全綠（11 條 ArchUnit fitness functions）。若紅燈，回到對應 Step 修正——**不准只改報告**。

### Step 9 — 出架構報告 `ARCHITECTURE_CONFORMANCE.md`
逐一列出每個不變式 pass/fail，例如：

| 不變式 | CPS | 結果 |
|---|---|:--:|
| 分層單向、無回呼 | 06 | ✅ |
| `PayoutStatusType` enum + getInstanceOf + id 唯一 | 02 | ✅ |
| provider 只在 enum `new`、Manager init-then-swap | 01 | ✅ |
| cache 容差查詢 + 冪等 | 03 | ✅ |
| DAO 全 prepared statement、方言隔離 | 07 | ✅ |
| 部署於 TRANSACTION 角色 | 08 | ✅ |

---

## 3. 這個範例用到哪些 CPS

| 元件 | CPS | scaffold 對照 |
|---|---|---|
| `PayoutProvider*`（可切換出款商） | 01 | `scaffold/cps01/` |
| `PayoutStatusType`（狀態機） | 02 | `scaffold/cps02/` |
| `PayoutCache`（增量快取） | 03 | `scaffold/cps03/` |
| 分層 + 命名 | 06 | `scaffold/cps06/` |
| `PayoutDAO` + 方言 | 07 | `scaffold/cps07/` |
| 部署在 TRANSACTION 角色 | 08 | `scaffold/cps08/` |

---

## 4. 常見誤用（護欄提醒）

- ❌ 在 `PayoutBO`/Controller 寫 `switch(statusId)` → 改用 `PayoutStatusType` 多型（CPS-02）。
- ❌ 在 BO 裡 `new BankWireProvider()` → 只能在 `PayoutProviderType` enum 內（CPS-01）。
- ❌ Controller 直接呼叫 `PayoutDAO` → 必須經 `PayoutBO`（CPS-06）。
- ❌ cache 假設 DB clock 準確而省略容差 → 保留 `ERROR_VALUE`（CPS-03）。
- ❌ 為了「現代化」偷渡 Spring/DI/ORM → 框架明令禁止（The Nevers）。

> 完整不變式與驗收條件見 `ARCH_BLUEPRINT.md` 第 3 章各 Pattern Card 與第 7、8 章。
