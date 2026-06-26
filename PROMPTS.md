# PROMPTS — 可直接複製的 AI 指令

把 [AI_CONTEXT.md](AI_CONTEXT.md) 先貼進 AI 對話（或放進 `CLAUDE.md` / `AGENTS.md`），再複製下面的指令使用。

---

## 1. 讓 AI 依框架生成一個新功能

```
依 ADAF 框架，幫我加一個「<功能名稱，例如 Refund 退款>」功能。

請嚴格依 Golden Flow：
1. 先產出 manifest（需求清單：用到的 type / provider / cache / endpoint / DB），未知項標 TODO_DECISION，不要亂猜。
2. 用決策樹選出要套用的 CPS。
3. 對照 scaffold/ 的對應樣板生成程式碼（遵守命名契約與依賴方向）。
4. 列出你預期 conformance 會檢查的點。
完成後告訴我跑哪個指令驗證。
```

## 2. 讓 AI 檢查既有專案是否符合 ADAF

```
依 ADAF 框架（見 ARCH_BLUEPRINT.md 命名契約與護欄），檢查這個專案/這段程式碼有沒有違反：
- 分層依賴回呼（Controller 直呼 DAO、DAO 回呼 BO 等）
- switch(typeId) / if(typeId==…) 這類該用 *Type enum 的分支
- *BO 非 private 建構子、在 enum 外 new Provider
- 偷渡 Spring/DI/ORM
逐條列出違規位置與修法，依嚴重度排序。
```

## 3. 讓 AI 修 conformance 失敗

```
我跑 `mvn test -Dadaf.basePackage=<base>`，以下 conformance 測試 fail：
<貼上失敗訊息>

請依 ADAF 規則修「程式碼」（不是改測試、不是改報告），讓架構回到合規，並說明每個改動對應哪條不變式。
```

## 4. 把框架接進專案（一次性設定）

```
把以下內容寫進本專案的 CLAUDE.md（或 AGENTS.md）：
本專案架構一律遵循 ADAF（見 ARCH_BLUEPRINT.md）。接到需求時依 Golden Flow 九步執行；
未通過 scaffold/conformance 的 ArchUnit 測試前，不得宣告完成。base package：<your base>
```

---

> 小技巧：AI 第一次容易跳過「先寫 manifest」直接寫 code。若發生，回它一句：
> 「先只給我 manifest，不要寫 code」，強制它走第一步。
