# FAQ — 常見問題

**Q：這是一個 library / 框架（要 import）嗎？**
不是。它是一份**規格 + 樣板 + 測試**，給 AI 讀、照著生成程式碼。你不會 `import adaf`，而是把它放進 AI 的 context。

**Q：一定要用 Java 嗎？**
參考實作與 conformance（ArchUnit）目前是 Java。但**模式與規則本身是語言無關的**——分層、命名契約、Type enum、Provider 註冊表、增量快取等概念可移植到其他語言；只是現成的 ArchUnit 測試要換成該語言的等價工具。

**Q：為什麼不用 Spring / DI / ORM？**
這套架構刻意保持「無框架」風格（手動單例、靜態 BO、自寫 DAO），是它淬煉來源系統的特性。框架明令禁止 AI 為了「現代化」偷渡 Spring/DI/ORM，因為那會破壞架構一致性。**你的專案要用 Spring 當然可以**——那就調整命名契約與 conformance 規則去對齊你的選擇。

**Q：ArchUnit 測得到什麼、測不到什麼？**
測得到：依賴方向、命名↔package、`*BO` 建構子、`*Type` 是否 enum、provider 在哪被 new、有沒有偷渡 Spring。
測不到：語句層級反模式（如 `switch(typeId)`）——這類用 Checkstyle / PMD regex 補，見 [`scaffold/conformance/README.md`](scaffold/conformance/README.md)。

**Q：我可以只用 conformance（架構守門）嗎？**
可以。最小用法就是把 `scaffold/conformance/ArchitectureConformanceTest.java` 放進你的專案、指定 base package、跑 `mvn test`。不一定要讓 AI 生成。

**Q：CPS-01~08 必須全部使用嗎？**
不用。按需求用決策樹（[ARCH_BLUEPRINT.md](ARCH_BLUEPRINT.md) 第 5 章）挑用得到的。多數功能只會用到其中 2~4 個。

**Q：它跟「用 AI 寫 code」有什麼不同？**
差別在**可控**：一般用 AI 寫 code，品質靠當下提示與人肉 review；ADAF 把規則寫成機器可驗證的測試，AI 走樣會被擋下、交付的是可審查的證據。

**Q：名詞太多看不懂？**
看 [GLOSSARY.md](GLOSSARY.md)，每個術語一句白話。
