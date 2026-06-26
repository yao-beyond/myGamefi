# QUICKSTART — 5 分鐘上手 ADAF

> 目標：5 分鐘內，你會（1）親眼看到框架運作、（2）讓 AI 依框架幫你加一個功能。
> 還不懂名詞？看 [GLOSSARY.md](GLOSSARY.md)。

---

## 第 1 步（1 分鐘）— 跑綠燈範例

```bash
git clone https://github.com/yao-beyond/myGamefi.git
cd myGamefi/examples/payout-service
mvn test
```

成功會看到：
```
Tests run: 15, Failures: 0
```
- 11 條 **架構合規測試**通過 → 程式碼沒違反架構規則
- 4 條 **行為測試**通過 → 功能正確

👉 這證明：**AI 生成的程式碼可以被測試自動驗收**，不靠人肉 review 手感。

---

## 第 2 步（1 分鐘）— 看懂這個範例在幹嘛

打開 [`examples/payout-service/`](examples/payout-service/)，重點看三條線：

1. **分層**：`PayoutController → PayoutBO → PayoutDAO`（依賴只往下，不回呼）
2. **可切換供應商**：`PayoutProviderType` enum 如何限制「只能在這裡 new 供應商」
3. **守門測試**：`ArchitectureConformanceTest` 如何擋下違規寫法

---

## 第 3 步（3 分鐘）— 讓 AI 依框架幫你加功能

把框架接給 AI（Claude / GPT），三步：

**① 給 AI context** — 把 [AI_CONTEXT.md](AI_CONTEXT.md) 貼進對話，或放進專案的 `CLAUDE.md` / `AGENTS.md`。

**② 下指令**（從 [PROMPTS.md](PROMPTS.md) 複製）：
```
依 ADAF 框架，幫我加一個「Refund（退款）」功能。
先產出 manifest（需求清單），選好要用的模式，再生成程式碼，最後跑 conformance 測試。
```

**③ 驗收**：AI 產完程式後，跑
```bash
mvn test -Dadaf.basePackage=com.yourcompany.yourapp
```
沒過 → 要 AI 修程式碼（**不准只改報告**），直到全綠。

---

## 下一步

| 想做什麼 | 去哪 |
|---|---|
| 照著做一遍完整教學 | [USAGE.md](USAGE.md) |
| 把架構守門加進自己的專案 CI | [`scaffold/conformance/README.md`](scaffold/conformance/README.md) |
| 看完整規格（給 AI / 維護者） | [ARCH_BLUEPRINT.md](ARCH_BLUEPRINT.md) |
| 名詞 / 常見問題 | [GLOSSARY.md](GLOSSARY.md)、[FAQ.md](FAQ.md) |
