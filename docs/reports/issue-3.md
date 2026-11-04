# Issue #3 / T005 — MySQL 守門與無 DB CI（第三次嘗試）

## 做了什麼

- 將既有 PR #10 的提交 `91d70d6` 接回從 main `46b6a9f` 建立的本機 `task/3`，保留最新 main 的 package／reference JAR 比對工作；不重建或切換分支，不覆寫遠端歷史。
- 保留 36 個需要 MySQL 的 `Legacy*IntegrationTest` 的 `@BeforeAll` 守門：`Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"))`。既有方法層級 `lito.legacy.integration` 條件保留。
- Java 17 的 testCompile 已通過，因此 package job 恢復 `mvn -B -DskipTests package`，移除暫設的 `maven.test.skip=true`。
- CI `server-test` 執行完整 `mvn -B -Dlito.mysql.integration=false test`，不配置 DB；成功或失敗皆上傳 Surefire 報告。未使用測試排除清單、忽略失敗或 `continue-on-error`。
- 合併保留 main 的 `docs/REPORT.md` 與 `TASKS.md`，兩者相對本輪起點沒有內容變動。
- 既有 PR 的兩個 testCompile 修正仍保留：`GameHubChannelTest` 補 `assertNotEquals` import；`NativeExternalControlV2Fixture` 改用現行三參數 `NativePlayerLoadoutAdapter` 建構子。這兩處是前次提交內容，並非本輪新增範圍。

## 怎麼驗證（已驗證）

- 已讀根目錄 `AGENTS.md`、`CLAUDE.md`、`docs/REPORT.md`、`TASKS.md` T005、`PLAN.md`，及伺服器子目錄規範與指定維護文件。
- 本機 JDK 17.0.17、Maven 3.9.16，離線使用既有 Maven 快取；測試暫存與日誌限定此 worktree 的 `scratch/`，編譯輸出在 `target/`。
- 守門驗證命令（於伺服器模組執行）：

```powershell
mvn -o -B '-Dtest=Legacy*IntegrationTest,LitoMySql*IntegrationTest' '-Dlito.legacy.integration=true' '-Dlito.mysql.integration=false' test
```

實際執行另外將 `java.io.tmpdir` 指向 worktree `scratch/issue-3-tmp`。
結果：**BUILD SUCCESS**；48 個測試類被發現，36 個 Legacy 類在 `@BeforeAll` 中止（Surefire 記為 tests=0），12 個 LitoMySql 類共 17 案 skipped，failures=0、errors=0。刻意開啟 legacy 舊開關，避免只驗證到舊條件停用。

- 完整測試：`mvn -o -B -Dlito.mysql.integration=false test`（另指定 worktree 暫存），2026-11-04 14:58:40 +08:00 結束，耗時 9 分 46 秒，**BUILD FAILURE / exit 1**。1265 個類報告合計 **7002 tests、284 failures、528 errors、106 skipped、328 個失敗類**。錯誤分類：460 個 NoSuchFileException、63 個 NullPointerException、2 個 SocketTimeoutException、2 個 IllegalStateException、1 個 StringIndexOutOfBoundsException；另有 284 個 AssertionFailedError。
- package 驗證：`mvn -o -B -DskipTests package`，**BUILD SUCCESS / exit 0**，包含 1411 個測試來源檔的 testCompile；產物僅放 `target/`。
- `git diff --check` 以 `core.whitespace=cr-at-eol` 檢查通過（沿用舊 fixture 的 CRLF）；`docs/REPORT.md`、`TASKS.md` 相對 main 無差異。
- 舊 CI 已確認：run `41780294277`、job `126410046756`，7002 tests、232 failures、590 errors、105 skipped。除缺少本機資源外，也存在 `CaptureLog` null 與 Linux 執行 `java.exe` 等獨立問題。

### 已定位的範圍外阻礙

共同目錄仍是伺服器 `src/test/java/local/lito381/server/`：

| 類型 | 具體例子／接手位置 | 處理方向 |
|---|---|---|
| 缺本機設定 | `ClassicWorldMonsterAiConfigTest`、`OriginSourceConfigTest`、`ServerConfigChannelPortsTest` | 明確區分本機配置驗收與可攜式單元測試，不讀取或提交正式設定 |
| 缺原始來源／反組譯／客戶端 | 多個 `Native*SourceAuditTest` 讀 `../1-伺服器`、`../LiTo381-開發/ida-client-oep-v2`、固定路徑 `LiTo.bin` | 依具體前置資源設守門，不能複製禁止資源到 CI |
| null fixture | `ProtocolRoomControlTest:84`、`RegistryOnlyOpcodeBoundaryTest:21`、`TournamentPresenceTest:274` 建立 `ClientSession` 時傳入 null capture | 補正測試 fixture，保留原本斷言 |
| 真正的斷言差異 | 本輪 `BattleCompanionTest.createdZombieUsesLevelSelectedTemplateAtFullVitals`：expected 81186 / actual 45065；`BattleFoeSlayerCatalogTest.pinsSelectedPrimaryRowAndRetainedThreeStrikeContract`：expected 12119 / actual 6509 | 由熟悉來源契約的人判定程式／測試哪邊失準，不能加 assume 掩蓋 |
| 平台路徑 | `RentalBattleHandoffStoreTest:137` 寫死 `java.exe` | 依作業系統選正確 Java executable；Windows 本機通過仍不能證明 Linux CI 通過 |

舊 CI 的完整證據：[run 41780294277 / server-test](https://github.com/chaoren0418-arch/lito-launcher-server/actions/runs/41780294277/job/126410046756)。以上不是完整失敗清單；完整個案見 Surefire XML，真正的斷言失敗需逐項修正。

## 未驗證什麼／驗收狀態

- **尚未達成「無 DB 的完整 mvn test 全綠」**；不得將守門子集通過當成全套通過。
- 未啟用 MySQL、未讀機密設定、未連正式 DB、未啟動正式伺服器／登入器／遊戲，未修改或複製客戶端資源、封存、發布檔及硬連結。
- 未驗證 MySQL opt-in 後的測試結果；未驗證既有 `NativeExternalControlV2Fixture` 移除 callback 後的外控觀察行為。
- 全套無 DB 測試的其他失敗超出本任務列出的 Legacy 測試與 workflow 範圍；已向使用者請求必要範圍擴充，未收到授權前不擅改這些測試或 `pom.xml`。

## 改了哪些 Legacy 測試類（36）

共同目錄：`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/`。
- `LegacyAdditionalFireIntegrationTest`
- `LegacyArmorBreakIntegrationTest`
- `LegacyBodyToMindIntegrationTest`
- `LegacyBraveAuraIntegrationTest`
- `LegacyBringStoneIntegrationTest`
- `LegacyBurningSpiritIntegrationTest`
- `LegacyCallClanIntegrationTest`
- `LegacyCallOfNatureIntegrationTest`
- `LegacyDarkBlindIntegrationTest`
- `LegacyDoubleBrakeIntegrationTest`
- `LegacyDressDexterityIntegrationTest`
- `LegacyDressEvasionIntegrationTest`
- `LegacyDressMightyIntegrationTest`
- `LegacyEnchantVenomIntegrationTest`
- `LegacyEquipmentPhysicalIntegrationTest`
- `LegacyFinalBurnIntegrationTest`
- `LegacyGlowingAuraIntegrationTest`
- `LegacyMovingAccelerationIntegrationTest`
- `LegacyNaturesBlessingIntegrationTest`
- `LegacyPolluteWaterIntegrationTest`
- `LegacyReservedSkillGapIntegrationTest`
- `LegacyResistMagicIntegrationTest`
- `LegacyRunClanIntegrationTest`
- `LegacyShadowArmorIntegrationTest`
- `LegacyShadowFangIntegrationTest`
- `LegacyShiningAuraIntegrationTest`
- `LegacySoulOfFlameIntegrationTest`
- `LegacySourceInventoryIntegrationTest`
- `LegacyStormShotIntegrationTest`
- `LegacyStrikerGaleIntegrationTest`
- `LegacyTripleArrowIntegrationTest`
- `LegacyTrueTargetIntegrationTest`
- `LegacyUncannyDodgeIntegrationTest`
- `LegacyVenomResistIntegrationTest`
- `LegacyWaterDragonAfflictionIntegrationTest`
- `LegacyWorldTreeCallIntegrationTest`

## 產出路徑

- `.github/workflows/build.yml`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/Legacy*IntegrationTest.java`（上列 36 類）
- 既有 PR 編譯修正：同目錄 `GameHubChannelTest.java`、`NativeExternalControlV2Fixture.java`
- `docs/reports/issue-3.md`（本報告）
- 未入 git 的本機證據：`scratch/issue-3-gates.log`、`scratch/issue-3-full-test.log`、`scratch/issue-3-package.log`，及伺服器 `target/surefire-reports/`
- GitHub CI 證據：`server-test-reports` artifact。

## 給下一棒的提示

1. Claude／調度器先確認範圍擴充：編譯輔助類、依賴本機來源／客戶端／設定的測試，以及必要的 `pom.xml` 測試配置。勿把 CI 紅燈視為已驗收。
2. 對環境依賴採明確 opt-in 或具體資源條件，保留無資源依賴的單元斷言；不得整批跳過失敗測試或讓 Maven 忽略失敗。
3. 獨立修正真正的行為／fixture 錯誤（例如 null CaptureLog）與跨平台程序路徑；不能用 assume 掩蓋。
4. 不把真實 `config/server.properties`、來源封存或 `LiTo.bin` 放入 CI。DB opt-in 與實機回歸由使用者在適當隔離環境執行。
5. `docs/REPORT.md` 保持 main 內容，合併後由調度器加索引。
