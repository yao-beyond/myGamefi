# Example — Payout Service（依 ADAF 框架生成）

一個**可編譯、可測試**的迷你完整專案，照 [`../../ARCH_BLUEPRINT.md`](../../ARCH_BLUEPRINT.md) 的 Golden Flow 產生，
實作 [`../../USAGE.md`](../../USAGE.md) 的「Payout（出款）」worked example。

## 跑起來

```bash
cd examples/payout-service
mvn test
```
會編譯全部分層，並執行：
- **`ArchitectureConformanceTest`** — 11 條 ArchUnit fitness functions（指向 `com.example.app`），全綠代表架構沒走樣。
- **`PayoutDomainTest`** — CPS-01/02/03/08 的行為驗收（熱切換、enum roundtrip、增量快取 + tombstone、bitmask 複合角色）。

## 它示範了哪些 CPS

| 層 / 元件 | package | CPS |
|---|---|---|
| `PayoutController`（`@WebServlet`） | `controller` | 06 |
| `PayoutBO`（private 建構子 + 靜態） | `bo` | 06 |
| `PayoutCache` + `AbstractCache`（時間戳增量、tombstone） | `cache` | 03 |
| `PayoutDAO` / `PayoutSource`（PreparedStatement） | `dao` | 06/07 |
| `Payout`（純資料 DTO） | `dto` | 06 |
| `PayoutStatusType`（狀態機 enum） | `code` | 02 |
| `PayoutProviderType` + `PayoutProvider*` + `PayoutProviderManager`（註冊表 + 熱切換） | `code` / `provider` | 01 |
| `SqlDialect` / `MySqlUtils` / `PostgreSqlUtils`（方言隔離） | `util` | 07 |
| `ServerRole` / `ServerRoleUtils` / `SystemInfo`（角色 bitmask） | `code` / `util` | 08 |

## 約定的分層 package
```
com.example.app.controller / .bo / .cache / .dao / .dto / .code / .provider / .util
```
依賴方向：`Controller → BO → Cache → DAO → DTO`（`code`/`provider`/`util` 為橫切）。

> 這個範例本身就是 conformance 的「綠燈樣本」——把它當你新專案的起點，照著加功能即可。
