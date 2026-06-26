# Contributing to ADAF (myGameFi)

歡迎貢獻！這個 repo 是一套**給 AI 用的架構生成框架**（規格 → 樣板 → 測試）。
先讀 [README.md](README.md) 與 [QUICKSTART.md](QUICKSTART.md) 了解全貌，名詞看 [GLOSSARY.md](GLOSSARY.md)。

---

## 黃金規則

> **任何改動都必須讓測試保持全綠，且 scaffold 能編譯。** 規範本身要以身作則。

```bash
cd examples/payout-service
mvn test          # 11 條 conformance + 4 條行為測試，必須全綠
```
改了 `scaffold/` 的 Java 樣板，請確認它仍可編譯（範例專案沿用相同模式即可驗證）。

---

## 開發前置

- JDK 8（樣板與 conformance 以 Java 8 為基準）
- Maven 3.6+

---

## 你可以貢獻什麼

| 類型 | 怎麼做 |
|---|---|
| 📖 文件 / 白話化 | 直接改 `*.md`；保持繁體中文、術語首次出現附英文 |
| 🐛 修 bug | 樣板編不過、conformance 規則誤判/漏判等 → 開 Bug issue 或 PR |
| 🧩 新增 / 改進模式（CPS） | **先開 New Pattern issue 討論**，再依下方流程 |
| 🛡️ 強化 conformance | 在 `scaffold/conformance/ArchitectureConformanceTest.java` 加規則 + 在合規/違規 fixture 上驗證 |

---

## 新增一個模式（CPS）的流程

1. 先開 [New Pattern issue](../../issues/new?template=new_pattern.md) 說明它**擋掉哪個會失控的問題**（這是收錄門檻）。
2. 在 [`ARCH_BLUEPRINT.md`](ARCH_BLUEPRINT.md) 第 3 章新增一張 Pattern Card（Intent / Trigger / Invariants / Naming / Anti-Pattern / Acceptance）。
3. 在 `scaffold/cpsNN/` 加**可編譯**的參考實作，檔頭標註不變式。
4. 可機器檢查的不變式 → 在 conformance 測試補規則。
5. 更新編號與索引：`README.md`、`scaffold/README.md`、決策樹。

---

## 風格約定

- 程式碼：**遵守 ADAF 命名契約與依賴方向**（見 [AI_CONTEXT.md](AI_CONTEXT.md)）——這個 repo 自己也要守規範。
- 文件：繁體中文為主，可掃讀（表格、小標、TL;DR）。
- 不引入 Spring / DI / ORM（框架刻意保持無框架風格，見 [FAQ.md](FAQ.md)）。

---

## 送出 PR

1. Fork → 開 feature branch（例如 `feat/cps09-xxx`、`docs/quickstart-typo`）。
2. 確認 `mvn test` 全綠、scaffold 可編譯。
3. Commit 訊息用祈使句、說清楚「改了什麼、為什麼」。
4. 開 PR，描述對應的 issue 與驗證結果（貼 `mvn test` 輸出）。

---

## 行為準則

請友善、就事論事。對人對事都以「能不能被檢查、能不能重現」為討論基準——這正是這個專案的精神。
