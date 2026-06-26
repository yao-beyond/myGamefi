# ADAF — AI-Driven Architecture Framework

> 把一套正式交易後端的架構經驗，變成 **AI 能確定性重現、機器能驗證**的治理系統。
>
> 這個 repo 不是要教你寫程式，而是要證明一件事：當 AI 參與架構生成時，產出可以**被規範、被檢查、被追責**，而不是靠工程師的個人手感。

---

## What this proves — 這個 repo 證明了什麼

多數工程師證明的是「我能寫出來」。ADAF 證明的是另一個層級的能力：

- **我能定義什麼叫「寫對」，並讓機器檢查它。** 架構慣例不是口頭品味，而是 11 條可機器驗證的規則。
- **我能約束 AI，而不只是使喚 AI。** 同一份規格餵給 AI，能確定性重現同一套架構；走樣會被測試擋下。
- **我能把企業級系統經驗，產品化成可重複套用的方法。** 不是一次性的高手交付，而是下次、別人、別的領域都能重現的流程。

對需要導入 AI 的團隊來說，真正的問題不是「AI 能不能做」，而是「AI 做出來的東西**能不能控**」。ADAF 是回答這個問題的證據。

---

## Governance Loop — 治理閉環

ADAF 的核心是一條閉環，而不是一堆技術零件：

![ADAF 治理閉環：正式系統經驗 → 架構規格 → AI 產生骨架 → 機器驗證 → 可審查交付](system_design/adaf-governance-loop.png)

> 原始可編輯檔：[`system_design/adaf-governance-loop.drawio`](system_design/adaf-governance-loop.drawio)（用 [draw.io](https://app.diagrams.net) 開啟）

每一步都白話可懂：

1. **正式系統經驗** — 規則的來源是一個真實的高並發交易後端，不是教科書理論。
2. **架構規格（Manifest）** — AI 動手前先用一份宣告把領域講清楚，杜絕自由發揮；未知項標 `TODO_DECISION`，不准亂猜。
3. **AI 產生骨架（Scaffold）** — AI 依規格與參考樣板重現結構、命名與不變式。
4. **機器驗證（Conformance）** — ArchUnit 測試檢查架構有沒有走樣；沒過就退回，**不准只改報告**。
5. **可審查交付** — 最終交付的不只是一包程式碼，而是一套可被檢查的工作方式。

---

## Manifest → Scaffold → Conformance — 三段怎麼運作

| 階段 | 角色 | 落在哪 |
|---|---|---|
| **Manifest** | 生成前的領域宣告（YAML DSL），強制先講清楚 provider／type／cache／DB | [`ARCH_BLUEPRINT.md`](ARCH_BLUEPRINT.md) 第 4 章 |
| **Scaffold** | 8 個模式各一份可編譯參考實作，AI 依樣套用結構與不變式 | [`scaffold/`](scaffold/) |
| **Conformance** | 11 條 ArchUnit fitness functions，把自審清單變成 CI 可擋的測試 | [`scaffold/conformance/`](scaffold/conformance/) |

> 核心信條：自然語言只描述「意圖」；真正讓 AI 確定性重現的是 **Manifest → Scaffold → Conformance**。**風格不是品味，是規則。**

---

## Patterns covered — 規範了哪些架構決策

8 個從正式環境淬煉的模式（**CPS** = Canonical Pattern Spec，編號 CPS-01 ~ CPS-08）。每個模式重點不在它的名字，而在它解決哪個**會失控的管理問題**：

| CPS | 模式 | 解決什麼管理問題 |
|---|---|---|
| 01 | Provider-Enum-Registry + Singleton Manager | 換供應商／通道時，能不能**不停機、不出半初始化狀態** |
| 02 | Domain Type Enum | 型別分支邏輯散落各處，**改一個漏一個** |
| 03 | AbstractCache + 時間戳增量更新 | 大量熱資料的記憶體快取，**併發更新會不會錯亂** |
| 04 | Hierarchical Entity Nesting | 樹狀領域資料，**父子層級會不會不一致** |
| 05 | Single-Thread WebSocket Broadcast | 實時推送，**慢連線會不會拖垮全體、訊息會不會亂序** |
| 06 | Layered + Naming Convention | 分層依賴**會不會回呼、命名會不會漂移** |
| 07 | DAO + Dialect Utility Separation | 多資料庫，**方言差異會不會散落、會不會 SQL injection** |
| 08 | Bitmask Server-Role Partitioning | 單一 codebase 部署多角色，**啟用什麼、級聯通知誰，會不會做白工或漏通知**（見[系統結構範例](system_design/system-structure.md)） |

![ADAF 分層架構：Controller/WebSocketService → BO → Cache → DAO → DTO/Entity → DB，橫切 Manager／Type enum／Utils，標出 CPS-01~07 落點](system_design/adaf-architecture.png)

> 原始可編輯檔：[`system_design/adaf-architecture.drawio`](system_design/adaf-architecture.drawio) — 分層依賴架構與 7 個 CPS 落點。

---

## Example verification result — 驗證證據

不是「我說有照規範」，而是有機器跑出來的結果：

```
$ mvn test -Dadaf.basePackage=com.example.app
OK (11 tests)
```

- **合規 fixture：** 11 條規則全數通過（`OK (11 tests)`）。
- **故意注入違規：** 在 BO 內 `new XxxProvider()`、Controller 直呼 DAO → 測試**擋下 2 failures**。
- 環境：ArchUnit 1.3.0 / JUnit 4.12 / `--release 8`。

也就是說，這套規則**過得了合規碼、擋得住違規碼**——fail-closed，不是裝飾用的綠燈。語句層級反模式（如 `switch(typeId)`）ArchUnit 看不到的，另用 Checkstyle / PMD regex 補上，細節見 [`scaffold/conformance/README.md`](scaffold/conformance/README.md)。

---

## Why this matters for AI adoption — 為什麼這對 AI 落地重要

把 AI 接進開發流程，企業真正怕的不是「AI 不夠快」，而是：

- AI 生成的程式碼**沒人能保證符合架構規範**，半年後變技術債。
- 每次品質靠當下那個人的手感，**換人或換 AI 就走樣**。
- 出事的時候，**講不清楚哪一步沒守規矩、誰該負責**。

ADAF 把這三件事都收進閉環：規範寫成規格、AI 依規格產出、機器驗證有沒有走樣、交付的是可審查的證據。這就是「**AI 被流程與規範治理**」，而不是「AI 取代工程師」。

---

## 怎麼用（給 AI 與 reviewer）

1. 讓 AI 讀 [`ARCH_BLUEPRINT.md`](ARCH_BLUEPRINT.md)：原則 → 命名契約 → 決策樹 → 對應 Pattern Card。
2. 接到新需求時，嚴格依序執行第 6 章「The Golden Flow」九步演算法（讀需求 → 寫 Manifest → 生 domain 詞彙 → 生分層骨架 → 生 provider/cache/DAO/endpoint → 跑 conformance → 出報告）。
3. 對照 [`scaffold/`](scaffold/) 套用結構與不變式。
4. 用第 7 章自審清單 +  [`scaffold/conformance/`](scaffold/conformance/) 驗證，未過則回到對應步驟修正。

> 給人類 reviewer：把本 README + `ARCH_BLUEPRINT.md` 放進 agent 的 context（或專案的 `CLAUDE.md` / `AGENTS.md`），即可讓 AI 持續遵循。

### Conformance 快速開始

```bash
# 1. 目標專案加入 ArchUnit（test scope）：com.tngtech.archunit:archunit-junit4:1.3.0
# 2. 放入 scaffold/conformance/ArchitectureConformanceTest.java
# 3. 指定 base package 後執行
mvn test -Dadaf.basePackage=com.yourcompany.yourapp
```

---

## Repo 構成

| 路徑 | 內容 |
|---|---|
| [`ARCH_BLUEPRINT.md`](ARCH_BLUEPRINT.md) | 主文件：架構原則、命名契約、8 張 Pattern Card、Manifest DSL、決策樹、9 步生成演算法、自審清單、護欄 |
| [`scaffold/`](scaffold/) | 8 個 CPS 的可編譯參考實作 |
| [`scaffold/conformance/`](scaffold/conformance/) | 11 條 ArchUnit fitness functions |
| [`system_design/`](system_design/) | 架構圖（`.drawio`）、[系統結構範例](system_design/system-structure.md)、Provider/Manager 設計說明 |

> 範例領域用通用電商（Payment / Order / Product / Catalog / PriceFeed）示範，與任何特定產業無關——替換成你的領域即可。

---

## License

[MIT](LICENSE)。`LICENSE` 的 copyright holder 請改成你的實際發佈名義。

---

由 **曾敬堯・堯策 YAO/CE** 維護。堯策的定位是「先理順流程，再談 AI 落地」；ADAF 是這套方法可落地的技術佐證之一，不是一個待售產品。（顧問門面待補）
