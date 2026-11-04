# TASKS — lito-launcher-server

格式：`ID | 狀態 | 負責 | 範圍 | 驗收條件`。狀態：todo / doing / review / done / blocked。
Cursor 接任務時只改「範圍」列出的路徑；PR 標題用 `ID: 摘要`。

## M0 可重現建置

### T001 | done | Claude | `.github/workflows/build.yml`
CI：Maven compile（先把 `tools/shared-database/SharedDatabase.java` 複製進 `src/main`）+ 登入器 `dotnet build`。
驗收：main 分支兩個 job 綠燈。

### T002 | done | Claude+Cursor | `lito伺服器/LiTo381-Server/pom.xml`, `src/main/java/**`, `tools/shared-database/*.java`, `lito登入器/LiTo381-Launcher/server-patches/**`（唯讀來源）
**現況**：`src/main` 只有 610 個檔，不是完整伺服器來源。正式 JAR = 二進位 `base-0.6.35/0.6.70.jar` + `tools/*/build.py` 逐層以 ASM patch 疊上去；缺的類別來源散在：
- `tools/shared-database/{SharedDatabase,UnifiedConfiguration,SourceLogin}.java`
- `lito登入器/LiTo381-Launcher/server-patches/repair-034/{LiToGmCommands,LiToGmNpcs,EnableCommands}.java` 等
- 各 `tools/<feature>/` 下的 `*.java`
目標：讓 `mvn -DskipTests package` 直接產出等價 JAR。步驟：
1. 用 `jar tf` 列出正式 JAR（`lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar`）全部 class；對照 `src/main` 找出缺的類別清單，寫進 PR。
2. 每個缺的類別：在 repo 內找到 `.java` 來源就搬進 `src/main/java/local/lito381/server/`；找不到來源的，用 CFR／Fernflower 反編譯 base JAR 取得並標注 `// decompiled from base-0.6.70.jar, TODO review`。
3. `tools/*/build.py` 改成只負責「驗證 mvn 產物 SHA 與正式 JAR 類別清單一致」，不再 patch。
4. `pom.xml` 版本改 `0.6.71`，設 Main-Class；CI 移除 `continue-on-error`。
驗收：CI Server job 綠；`jar tf` 類別清單與正式 JAR 一致（差異列在 PR）；`1.啟動伺服器.bat --check` 用 mvn 產物通過。
每個步驟一個 commit；反編譯的檔案另開 commit 以便審查。

### T003 | todo | Cursor | `lito登入器/LiTo381-Launcher/Build.ps1`, `tools/build_current.py`, `tools/verify_current.py`
去硬編路徑：Python 改用 `py -3.12` 或 PATH 中的 `python`；dotnet 改用 PATH；伺服器 `deployment/release-inputs.json` 路徑改相對 repo 根。
驗收：在 `D:\tmp\litotest` clone 後 `Build.ps1` 能跑到 publish 完成。

### T004 | todo | Cursor | `lito登入器/README.md`, `LiTo381-Launcher/README.zh-TW.md`, `lito伺服器/README.md`, `LiTo381-Server/README.zh-TW.md`, `VALIDATION.md`, `docs/HISTORY.md`
統一版本號為 0.6.71；刪除「正式 0.6.69」殘句；`VALIDATION.md` 只留最新一節，舊段落移到 `docs/HISTORY.md`。
驗收：`grep -rn "0\.6\.69\|0\.6\.70" --include=*.md` 只剩歷史章節。

### T005 | todo | Cursor | `lito伺服器/LiTo381-Server/src/test/java/local/**/Legacy*IntegrationTest.java`
DB 依賴測試加 `Assumptions.assumeTrue(Boolean.getBoolean("lito.mysql.integration"))` 守門，與 `LitoMySql*IntegrationTest` 一致。
驗收：無 DB 環境 `mvn test` 全綠（skipped 可）；CI 加 `mvn test` job。

### T006 | done | Claude | `.gitignore`, `config/server.properties.example`
真實 `server.properties` 不進 git。**使用者須更換 DB 密碼。**

### T007 | todo | Cursor | `lito伺服器/LiTo381-Server/pom.xml`, `reference/`
T002 收尾：pom 版本改 0.6.71、`maven-jar-plugin` 設 Main-Class（`local.lito381.server.SharedResourceMain`，見 reference JAR 的 MANIFEST）；CI 加 `mvn -DskipTests package` 並用 `jar tf` 與 `reference/base-0.6.71.jar` 比對類別清單，差異寫進 `docs/server-source-gap.md`。
驗收：CI 綠；差異清單存在且只含 Modern* 類別與刻意排除項。

### T008 | todo | 本機代理 | `src/main/java/**`（266 個 DECOMPILED 檔）
用 `backups/history/server-development-evidence.zip`（本機，164 MB）搜尋這 266 個類別的原始 .java；找到的以原始碼取代反編譯版並去掉 DECOMPILED 標頭。每批 ≤ 50 檔一個 PR。
驗收：每批 CI 綠；`grep -l "^// DECOMPILED"` 數量下降；PR 描述列出取代／未找到清單。

## M1 正式版實機回歸

### T010 | done | Claude | `docs/acceptance/CHECKLIST.md`（新建）
實機核對表：伺服器 `--check` / `--smoke` 輸出預期、登入器登入→大廳→進房→戰鬥→結算→退出 每步截圖與 DB 核對 SQL（`SELECT ... FROM character_lito WHERE ...`）。

### T011 | blocked(T002,T003,T010) | 使用者 | `docs/acceptance/0.6.71.md`
照 T010 跑一次；失敗項各開 T1xx bug。

## M2 戰鬥技能同源化（每項一個 PR；先做 T020）

### T020 | todo | Claude+Cursor | `lito伺服器/LiTo381-Server/src/main/**`, `tools/combat-continuation/**`
決定並執行合併策略：把候選 `50635f7e…` 相對正式來源的 diff 併進 `src/main`，候選工具鏈停用。Claude 先產出 diff 摘要與風險清單，Cursor 執行。
驗收：合併後 CI 綠；`tools/combat-continuation` 既有 2,400 案可在合併後來源上重跑並通過。

### T021 | todo | — | 技能目標／Ctrl 政策（807 列技能 × 正常／Ctrl，既有 3,814 案未驗此政策）
### T022 | todo | — | 26／42 自施：Source26 不按 Ctrl 無法自施（需真實游標與送包證據）
### T023 | todo | — | 受控 NPC（PET／SUMMON）Source 語意：1,472 組目標失敗 + 1,472 正常順序
### T024 | todo | — | HP 生產端：魔杖／反射／毒傷／裝備／魔石／武器 proc、魔法娃娃 proc、fatality、cube、ground pulse、AI
### T025 | todo | — | 58 列材料技能逐列（剩 27、36、41、44；靜態掃描剩 3 個舊呼叫）
### T026 | todo | — | 外控完整鏈：讀取器→消費端、動態鍵／別名、SQL 控制
### T027 | todo | — | 生命週期：receipt 保留／排空、成對鎖、退役／死復活／同 ID 替換、DRAINING／STOPPED／Main
### T028 | todo | — | 顯示與 NPC 語意：HPBAR／傷害數字／奇古獸、NPC 工廠／AI／hate、免疫時序
### T029 | todo | — | 正式功能隔離合成：0.6.71 資源／圖示／litoddraw.dll／GM7200 併入合併後來源
### T030 | blocked(T021–T029) | — | 同 SHA 後端全回歸
### T031 | blocked(T030) | 使用者 | 實機：游標／前景／多視窗／大廳集中驗收
### T032 | todo | 使用者 | 中文 MySQL 真裝備角色唯讀重載（使用者延後）

## M3 交付
### T040 | todo | — | `docs/INSTALL.md`, `packaging/`
安裝手冊 + 打包腳本（JAR、`db/*.sql`、`server.properties.example`、登入器 release）。

## 登入器已知未驗（排進 M1 一起看）
- 結算畫面職業圖示無人工回報
- 0.6.67 Ctrl 強制攻擊雙視窗手動驗收未做
- GM 魔杖滑鼠點選未人工驗
- 斷線結算無長時間壓力／災難復原驗證
- 舊正式入口仍用通用 `ddraw.dll` 名稱（候選用 `litoddraw.dll`）
