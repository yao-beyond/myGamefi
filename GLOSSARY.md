# GLOSSARY — 術語白話表

ADAF 的文件有些術語。這裡用一句白話解釋每個，看不懂時回來查。

| 術語 | 白話一句 | 正式 / 補充 |
|---|---|---|
| **ADAF** | 給 AI 用的「後端架構生成規格」 | AI-Driven Architecture Framework |
| **Manifest** | AI 動手前的「需求清單」（YAML） | 生成前置宣告；未知項標 `TODO_DECISION` |
| **Scaffold** | 8 份「標準參考程式碼」，照著複製 | 樣板 / reference implementation |
| **Conformance** | 「架構驗收測試」，檢查有沒有照規範寫 | ArchUnit fitness functions（11 條） |
| **CPS** | 「架構模式編號」（CPS-01~08） | Canonical Pattern Spec |
| **Pattern Card** | 一個模式的「規格卡」（何時用、不變式、範例） | 在 ARCH_BLUEPRINT.md 第 3 章 |
| **Golden Flow** | AI 的「生成步驟」（需求→manifest→生成→測試） | 九步演算法，ARCH_BLUEPRINT.md 第 6 章 |
| **不變式 (Invariant)** | 「一定要成立的規則」，違反就算錯 | 每張 Pattern Card 都列 |
| **護欄 (Guardrails)** | 「不准做的事」清單，防 AI 抄壞習慣 | The Nevers，ARCH_BLUEPRINT.md 第 8 章 |
| **fail-closed** | 沒驗證過就當作不通過 | 測試擋得住違規碼，不是裝飾綠燈 |

## 分層角色（後綴 = 身分）

| 後綴 | 白話 | 角色 |
|---|---|---|
| `*Controller` | 對外 HTTP 入口 | 只呼叫 BO |
| `*Service` | WebSocket / 對外服務 | 實時推送 |
| `*BO` | 業務邏輯（Business Object） | 全靜態、private 建構子 |
| `*Cache` | 記憶體狀態 | 熱資料 |
| `*DAO` | 資料庫存取（Data Access Object） | 只做 SQL |
| `*DTO` | 純資料物件（Data Transfer Object） | 不依賴任何上層 |
| `*Type` | 策略 enum | 取代 if/switch |
| `*Manager` | 全域協調者 | 單例 |

依賴方向只能往下：`Controller → BO → Cache → DAO → DTO`。
