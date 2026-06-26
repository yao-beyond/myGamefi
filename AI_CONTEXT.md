# AI_CONTEXT — 給 AI agent 的最小入口

> 把這份貼進你的 context（或專案的 `CLAUDE.md` / `AGENTS.md`）。
> 這是「精簡版」；完整規則在 [ARCH_BLUEPRINT.md](ARCH_BLUEPRINT.md)。

---

You are generating backend code that MUST follow the ADAF framework.

## 你的工作流程（接到需求時，嚴格依序）

1. **先寫 manifest**：把需求轉成一份宣告（provider / type / cache / endpoint / DB），未知項標 `TODO_DECISION`，**不准亂猜**。（規格見 ARCH_BLUEPRINT.md 第 4 章）
2. **選模式**：用決策樹挑出要用的 CPS（ARCH_BLUEPRINT.md 第 5 章）。
3. **依樣板生成**：對照 [`scaffold/`](scaffold/) 對應的 `cpsNN/` 複製結構、命名、不變式。
4. **跑 conformance**：`mvn test -Dadaf.basePackage=<base>`。
5. **沒過就修程式碼**，不准只改報告；直到全綠。

## 鐵則（The Nevers，違反一定錯）

- ❌ 不准引入 Spring / DI container / ORM / reactive（除非 manifest 明示）。
- ❌ 不准在 BO / DAO / Controller 用 `switch(typeId)` / `if(typeId==…)`；改用 `*Type` enum 多型。
- ❌ Controller 不准直接 import / 呼叫 DAO 或 Cache，必須經 BO。
- ❌ DAO 不准回呼 BO / Cache / Controller。
- ❌ 不准在 `*Type` enum 以外 `new XxxProvider()`。
- ❌ `*BO` 必須 private 建構子 + 全靜態。

## 命名契約（後綴 = 層級身分）

`*Controller`（HTTP 入口）→ `*BO`（業務，靜態）→ `*Cache`（記憶體狀態）→ `*DAO`（SQL）→ `*DTO`（純資料）；
橫切：`*Type`（策略 enum，放 `code`）、`*Utils`、`*Manager`。
依賴方向只能往下：`Controller → BO → Cache → DAO → DTO`。

## 成功標準

不是「我說完成」，而是 **conformance 測試通過**。

## 進一步閱讀

- 完整規格與 8 張 Pattern Card：[ARCH_BLUEPRINT.md](ARCH_BLUEPRINT.md)
- 可複製的人類指令：[PROMPTS.md](PROMPTS.md)
- 參考實作：[`scaffold/`](scaffold/)
