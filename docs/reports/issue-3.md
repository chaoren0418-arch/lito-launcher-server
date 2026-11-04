# Issue #3 / T005 — Legacy MySQL assume 守門 + CI `mvn test`

## 做了什麼
在每個需要 MySQL 的 `Legacy*IntegrationTest` 類加上與 `LitoMySql*IntegrationTest` 相同的系統屬性守門：

```java
@BeforeAll
static void requireMySqlIntegration() {
    Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
}
```

未設 `-Dlito.mysql.integration=true` 時不連正式／本機 MySQL。既有 `@EnabledIfSystemProperty(named = "lito.legacy.integration")` 保留。

`.github/workflows/build.yml` 新增 `server-test` job（Java 17、無 DB、`mvn -B test`）。既有 `server` compile job 不變。

為通過 `testCompile`（否則 `mvn test` 進不了 surefire），另修兩個範圍外編譯錯誤：
- `GameHubChannelTest`：補 `assertNotEquals` static import
- `NativeExternalControlV2Fixture`：`NativePlayerLoadoutAdapter` 現為 3 參數建構子，拿掉多餘的 `this::observeEntry`

## 改了哪些測試類（36）
`lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/`：

- LegacyAdditionalFireIntegrationTest
- LegacyArmorBreakIntegrationTest
- LegacyBodyToMindIntegrationTest
- LegacyBraveAuraIntegrationTest
- LegacyBringStoneIntegrationTest
- LegacyBurningSpiritIntegrationTest
- LegacyCallClanIntegrationTest
- LegacyCallOfNatureIntegrationTest
- LegacyDarkBlindIntegrationTest
- LegacyDoubleBrakeIntegrationTest
- LegacyDressDexterityIntegrationTest
- LegacyDressEvasionIntegrationTest
- LegacyDressMightyIntegrationTest
- LegacyEnchantVenomIntegrationTest
- LegacyEquipmentPhysicalIntegrationTest
- LegacyFinalBurnIntegrationTest
- LegacyGlowingAuraIntegrationTest
- LegacyMovingAccelerationIntegrationTest
- LegacyNaturesBlessingIntegrationTest
- LegacyPolluteWaterIntegrationTest
- LegacyReservedSkillGapIntegrationTest
- LegacyResistMagicIntegrationTest
- LegacyRunClanIntegrationTest
- LegacyShadowArmorIntegrationTest
- LegacyShadowFangIntegrationTest
- LegacyShiningAuraIntegrationTest
- LegacySoulOfFlameIntegrationTest
- LegacySourceInventoryIntegrationTest
- LegacyStormShotIntegrationTest
- LegacyStrikerGaleIntegrationTest
- LegacyTripleArrowIntegrationTest
- LegacyTrueTargetIntegrationTest
- LegacyUncannyDodgeIntegrationTest
- LegacyVenomResistIntegrationTest
- LegacyWaterDragonAfflictionIntegrationTest
- LegacyWorldTreeCallIntegrationTest

## 怎麼驗證（已驗證）
- 已讀 `AGENTS.md`／`CLAUDE.md`、`TASKS.md` T005、`PLAN.md`。未碰 `config/server.properties`、正式 DB、`LiTo.bin`、pak/map/sprite、封存。
- 無 DB、未設 `lito.mysql.integration`：36 個 Legacy 類 surefire 皆 `tests=0 failures=0 errors=0`（方法上還有 `lito.legacy.integration` 的 `@EnabledIfSystemProperty`，JUnit 計為 disabled 而非 skipped）。
- 既有 `LitoMySql*IntegrationTest` 12 類共 17 案全部 skipped（assume 守門仍有效）。
- 本機 `mvn test` 能完成 testCompile + surefire（Java 21／Maven 3.9.11；CI 用 Java 17）。

## 未驗證什麼
- 未設 `-Dlito.mysql.integration=true`，也未連 MySQL，因此 **未驗證** assume 打開後 Legacy 案是否通過。
- **全套 `mvn test` 未綠**：7002 tests、232 failures、590 errors、105 skipped、332 個失敗類。失敗主因在範圍外：
  - 硬讀 `LiTo381-Server/config/server.properties`（git 不追蹤）
  - 硬讀本機 `lito伺服器/1-伺服器/db/380_db.sql` 與 Lineage 來源 `.java`
  - 硬讀本機 `D:/380/.../LiTo.bin`、`ida-client` 反組譯
- 因此 CI `server-test` **預期會紅**，直到另開任務給那些測試加 assume／路徑守門。這不是 Legacy MySQL 守門沒生效。
- 未跑登入器、未啟動遊戲／正式伺服器。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/Legacy*IntegrationTest.java`（36）
- `.github/workflows/build.yml`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/GameHubChannelTest.java`（編譯）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NativeExternalControlV2Fixture.java`（編譯）
- `docs/reports/issue-3.md`（本檔）
- `docs/REPORT.md`
- `TASKS.md`（T005 → review）

## 給下一棒的提示
1. 審查這 36 個類的 `@BeforeAll` 是否與 `LitoMySql*` 的 `Boolean.getBoolean("lito.mysql.integration")` 一致。
2. 不要為了把 CI 洗綠去提交 `config/server.properties` 或複製 pak/map/sprite／`1-伺服器`。
3. 若要 T005 驗收字面「`mvn test` 全綠」，需要新任務：替 `Native*SourceAuditTest`、讀 `server.properties`／`LiTo.bin` 的測試加 assume 或 CI exclude。
4. 本機打開 Legacy 活庫驗證：`-Dlito.mysql.integration=true -Dlito.legacy.integration=true`（僅本機、勿連正式玩家庫）。
