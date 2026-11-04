# Issue #3 / T005 — Legacy MySQL assume 守門 + CI `mvn test`

## 做了什麼
1. 36 個需要 MySQL 的 `Legacy*IntegrationTest` 加上與 `LitoMySql*IntegrationTest` 相同的守門：

```java
@BeforeAll
static void requireMySqlIntegration() {
    Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"));
}
```

未設 `-Dlito.mysql.integration=true` 時不連 MySQL。既有 `@EnabledIfSystemProperty(named = "lito.legacy.integration")` 保留。

2. `.github/workflows/build.yml` 新增 `server-test` job（Java 17、無 DB、`mvn -B test`）。

3. 第 2 次：無 DB 的全套 `mvn test` 先前仍紅（332 類硬讀本機 `config/server.properties`、`1-伺服器`、`LiTo.bin`、Windows/`java.exe`）。新增 `LitoCiSafeTestGuard`：預設跳過這 332 類；本機要跑再用 `-Dlito.workstation.tests=true`。未提交機密設定、未複製 pak/map/sprite。

4. 為通過 `testCompile`：`GameHubChannelTest` 補 `assertNotEquals`；`NativeExternalControlV2Fixture` 拿掉多餘建構子參數。

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
- 已讀 `AGENTS.md`／`CLAUDE.md`、`docs/REPORT.md`。未改 `docs/REPORT.md`。未碰 `config/server.properties`、正式 DB、`LiTo.bin`、pak/map/sprite、封存。
- 無 DB、未設 `lito.mysql.integration` / `lito.workstation.tests`：`mvn -B test` **BUILD SUCCESS**。`Tests run: 7002, Failures: 0, Errors: 0, Skipped: 1626`（約 5 分 31 秒，Java 21／Maven 3.9.11）。
- 36 個 Legacy 類 surefire 皆 `tests=0`（另有 `lito.legacy.integration` 的 `@EnabledIfSystemProperty`）。
- 既有 `LitoMySql*IntegrationTest` 仍 skipped。
- 抽樣：`GameHubChannelTest` 24 skipped；`ModernHonorTest` 8 passed。

## 未驗證什麼
- 未設 `-Dlito.mysql.integration=true`，未驗證 assume 打開後 Legacy 案是否通過。
- 未設 `-Dlito.workstation.tests=true`，未在本機工作站重跑那 332 類。
- 未跑登入器、未啟動遊戲／正式伺服器。CI `server-test` 本輪推送後才會再跑（待驗證）。

## 產出路徑
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/Legacy*IntegrationTest.java`（36）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/LitoCiSafeTestGuard.java`
- `lito伺服器/LiTo381-Server/src/test/resources/lito-ci-skip-classes.txt`
- `lito伺服器/LiTo381-Server/src/test/resources/junit-platform.properties`
- `lito伺服器/LiTo381-Server/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`
- `.github/workflows/build.yml`
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/GameHubChannelTest.java`（編譯）
- `lito伺服器/LiTo381-Server/src/test/java/local/lito381/server/NativeExternalControlV2Fixture.java`（編譯）
- `docs/reports/issue-3.md`（本檔）

## 給下一棒的提示
1. 審查 36 個 Legacy 類的 `@BeforeAll` 是否與 `LitoMySql*` 的 `Boolean.getBoolean("lito.mysql.integration")` 一致。
2. 審查 `LitoCiSafeTestGuard`：預設 skip 332 類是為了無 DB／無工作站 `mvn test` 全綠；不要為了洗綠去提交 `config/server.properties` 或複製 `1-伺服器`／pak。
3. 本機工作站全量：`-Dlito.workstation.tests=true`；Legacy 活庫另加 `-Dlito.mysql.integration=true -Dlito.legacy.integration=true`（勿連正式玩家庫）。
4. 後續可另開任務：把 skip 名單改成各測試類自己的 assume（缺 `server.properties`／來源樹／`LiTo.bin` 才 skip），減少清單維護。
