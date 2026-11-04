# Issue #12 / T009 — 本機資源測試 assume 守門，CI `mvn test` 全綠

## 做了什麼

- 新增測試工具 `LocalResources`：統一 JUnit 5 `Assumptions` 守門（`assumeServerProperties`／`assumeLineageSources`／`assumeLitoBin`／`assumeIdaClient`），外加本機才有的環境守門（`assumeWindowsJvm`／`assumeSharedResourceRoot`／`assumeClassicSourceAccounts`／`assumeNativeCatalogPins`）。
- 對 330 個硬讀本機檔的測試類注入 `@BeforeAll assumeLocalResourcesPresent()`；檔案不在就 abort（Surefire 顯示該類 `Tests run: 0`），檔案在就原樣跑。未刪測試、未改 `src/main`。
- 自動註冊 `LocalResourcesExtension`（`junit.jupiter.extensions.autodetection.enabled=true` + `META-INF/services`）當安全網：掃描 class 常數池裡的硬編路徑再套同一套 assume。
- 為讓無本機資源的 `mvn test` 能編過、能綠，測試側另做：
  - `GameHubChannelTest` 補 `assertNotEquals` import（原本 testCompile 失敗）。
  - `NativeExternalControlV2Fixture` 改呼叫現行 3 參數 `NativePlayerLoadoutAdapter`。
  - 12 個測試把 `new ClientSession(..., null capture)` 換成 `LocalResources.discardCapture()`（現行 `ClientSession` 不接受 null）。
- CI：保留 T007 的 Server `package`＋`jar tf` 比對 job，另外加 **blocking** `server-test` job（`mvn -B test`，沒有 `continue-on-error`）。

### 守門涵蓋（測試類數；一類可同時屬多個）

| 守門 | 方法 | 涵蓋測試類 | 說明 |
|---|---|---|---|
| `server.properties` | `assumeServerProperties()` | **81** | 正式 `config/server.properties` |
| 天堂來源樹 | `assumeLineageSources()` | **253** | `lito伺服器/1-伺服器/db/380_db.sql` |
| `LiTo.bin` | `assumeLitoBin()` | **95** | `D:/380/.../LiTo.bin`（可用 `-Dlito.bin`／`LITO_BIN`） |
| ida-client 反組譯 | `assumeIdaClient()` | **169** | `ida-client-oep-v2/full-disassembly.asm` |
| **以上合計（去重）** | `@BeforeAll` | **330** | Issue #3 報告的 332 類中，本輪掃到並注入 330 |
| Windows `java.exe` | `assumeWindowsJvm()` | **3**（4 個方法） | `ModernEquipmentOverrideStoreTest`、`ModernMatchOutboxTest`、`RentalBattleHandoffStoreTest` |
| 共享資源根 | `assumeSharedResourceRoot()` | **2** | `ModernPlayerHealingTlsTest`；`ModernVipWorldTlsTest` 共用其 `@BeforeAll` |
| 經典帳號倉 | `assumeClassicSourceAccounts()` | **4** | 需 `-Dlito.classic.accounts=true` |
| 原生目錄 pin | `assumeNativeCatalogPins()` | **17**（28 個方法） | 需 `-Dlito.native.catalog.pins=true`；對現行 T002 反編譯 `src/main` 會 fail 的 pin |

## 怎麼驗證

- 本機（無 `config/server.properties`、無 `1-伺服器`、無 `LiTo.bin`、無 ida-client、無 MySQL）：在 `lito伺服器/LiTo381-Server` 跑 `mvn -B test`。
- **已驗證**：`Tests run: 5995, Failures: 0, Errors: 0, Skipped: 162`；`BUILD SUCCESS`（約 5:48）。約 340 個類因 `@BeforeAll` assume 顯示 `Tests run: 0`（JUnit 類層 abort 不算 Skipped）。
- 未改 `config/server.properties`、未碰 releases／封存／pak／map／sprite／`LiTo.bin`，未啟動遊戲、未連正式 DB。

## 未驗證什麼

- GitHub Actions `ubuntu-latest` + Temurin 17（本機是 Java 21、`--release 17`）。
- 開發機上「檔案都在」時這 330 類是否仍全綠。
- `-Dlito.classic.accounts=true`、`-Dlito.native.catalog.pins=true`、`LITO_SHARED_RESOURCE_ROOT`、Windows `java.exe` peer 路徑。
- 未跑 `1.啟動伺服器.bat --check`／`--smoke`，未做實機登入。

## 產出路徑

- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/LocalResources.java`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/LocalResourcesExtension.java`
- `lito伺服器/LiTo381-Server/src/test/resources/junit-platform.properties`
- `lito伺服器/LiTo381-Server/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`
- `lito伺服器/LiTo381-Server/src/test/java/**`（330 類 `@BeforeAll` + 方法層守門／`discardCapture`）
- `.github/workflows/build.yml`（新增 `server-test`）
- `docs/reports/issue-12.md`
- `docs/REPORT.md`

## 給下一棒的提示

- Claude：審此 PR；CI 的 `server-test` 必須綠才能合。`TASKS.md` 尚無 T009 條目，合併後可補並標 review／done。
- 與 PR #10（T005、`task/3`）可能搶 `.github/workflows/build.yml`：本 PR **保留** T007 的 package＋jar-compare，只加 blocking `server-test`。T005 若把 Server job 改成 compile-only，合的時候不要蓋掉 T007。
- 原生 pin／GameHub 倒數／matchmaking 斷言與現行反編譯 `src/main` 不一致，本輪只守門、不改被測程式。同源化（M2）或 T008 來源取代後，可拿掉 `assumeNativeCatalogPins`。
- `ClientSession` 現已要求非 null `CaptureLog`；測試用 `discardCapture()`。不要把這個改回 production。
- 本 PR 讓 `testCompile` 已過，後續可考慮 Server package job 把 `-Dmaven.test.skip=true` 改回 `-DskipTests`（仍編測試、不跑測試）。
