# Issue #2 / T007 — pom 0.6.71 + Main-Class + CI 類別清單比對

## 做了什麼

- 確認 `lito伺服器/LiTo381-Server/reference/base-0.6.71.jar` 的 `META-INF/MANIFEST.MF`：`Main-Class: local.lito381.server.SharedResourceMain`（`Implementation-Version` 仍為歷史值 `0.7.864`）。
- `pom.xml` 版本改 `0.6.71`；預設 `maven-jar-plugin` Main-Class 改為 `local.lito381.server.SharedResourceMain`。Modern classifier JAR 仍走 `ModernStandaloneMain`。
- CI Server job 改為 `mvn -B -DskipTests -Dmaven.test.skip=true package`（見下方「未驗證／限制」），再用 `jar tf` 比對 target JAR 與 reference JAR 的 `.class` 清單，寫入 `docs/server-source-gap.md`，並以 artifact `server-source-gap` 上傳。
- 每個「只在 reference」的類別已標 Modern*／刻意排除／待補。

## 怎麼驗證

- 本機：`unzip -p …/reference/base-0.6.71.jar META-INF/MANIFEST.MF` 確認 Main-Class。
- 本機：`mvn -B -DskipTests -Dmaven.test.skip=true package` 成功；產物 MANIFEST 為 `Main-Class: local.lito381.server.SharedResourceMain`、`Implementation-Version: 0.6.71`。
- 本機：`jar tf` 集合差 — reference 2554、target 3907、交集 2538、只在 reference 16、只在 target 1369。
- 只在 reference：待補 1（`NativeDeVisibilitySwitch`）、刻意排除 15（inner／匿名類形狀不同）、Modern* 0。
- 只在 target：Modern* 1320 + 其他非 Modern 49（EquipmentRules、TournamentPresence 等）。
- GitHub Actions PR #7 全綠：Server（Java 17／Maven）+ Launcher（.NET 10）兩個 job 成功（run 41779799344）。
- 未改 `config/server.properties`、未碰 releases／封存／pak／map／sprite，未啟動遊戲、未連正式 DB。

## 未驗證什麼

- 純 `-DskipTests package`（不含 `-Dmaven.test.skip=true`）會在 `testCompile` 失敗：`NativeExternalControlV2Fixture` 建構子參數不符、`GameHubChannelTest` 找不到 `assertNotEquals`。此屬 T005 範圍，本任務未修測試。
- 類別清單一致 ≠ 位元組碼／行為一致。
- 未跑 `1.啟動伺服器.bat --check`／`--smoke`，未做實機登入。

## 產出路徑

- `lito伺服器/LiTo381-Server/pom.xml`
- `.github/workflows/build.yml`
- `docs/server-source-gap.md`
- `docs/reports/issue-2.md`
- `docs/REPORT.md`

## 給下一棒的提示

- Claude：PR #7 CI 已綠，審合併後把 `TASKS.md` T007 標 review／done。
- 下一棒（T008／來源補齊）：只缺頂層 `NativeDeVisibilitySwitch`（見 `reference/missing-classes.txt`）。15 個 inner／匿名差異不必為對齊 `$1` 去改現行原始碼，除非要位元組碼等同。
- T005：先修 `testCompile`，CI 才能拿掉 `-Dmaven.test.skip=true` 並加 `mvn test`。
- 使用者：實機 `--check`／`--smoke` 仍須等 M0 其餘項與本機代理。
