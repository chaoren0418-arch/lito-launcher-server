# Issue #419 — T143：F1–F5 環境與 fixture 唯讀取證

**結論：部分通過（✅ 6／❌ 0／⚠️ 2）。** 五份指定證據已交付；未修改程式、工具、fixture 或資料，未重編 fixture、未重跑戰鬥回歸。P24 已正常關閉。本表評估的是取證工作，**不表示 #341 的 F1–F5 已通過**。

2026-11-14（Asia/Taipei），在指定 worktree／`task/419` 執行。已讀根與伺服器 `AGENTS.md`、維護規範、`docs/REPORT.md`、#284 啟停入口及 #341 指定失敗證據；`TASKS.md` 尚無 T143 條目，範圍依本張派工。未 checkout／另建分支、未改 `docs/REPORT.md`、未刪 worktree／junction／目錄。原始專案僅讀取。

## 逐項結果

`E`＝本 worktree 的 `scratch/issue-419/evidence/`；`R`＝本報告旁的 `issue-419/`。JSON 的 `checked_files`／`evidence` 保存完整原始路徑、SHA-256 與必要行號片段。

| 編號 | 結果 | 實際觀察 | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| 01 | ✅ | lease holder 為 #419、有效至 2026-11-13 22:56:18.661822Z；SELECT 在有效期間完成 | 讀 `/api/state` 的 `leases`，核對 holder／exp | `R/historical-env.json` → `collection_audit.lease` |
| 02 | ⚠️ | `characters WHERE char_name='SignedHp1'` 為零列；連帶材料 join 零列，無法由 SQL 取得 ephemeral 角色／背包。已補 fixture 靜態初值，未冒充現場記憶體 | 以 #83 帳號查角色及對應材料；讀既有 init/setup | `R/profile.tsv`；`E/characters.tsv`、`materials.tsv` |
| 03 | ✅ | 技能 33／44／165／170 完整四列、每列 32 欄已取得 | SELECT 四個 skill_id，保留全部欄位 | `R/skills.tsv` |
| 04 | ✅ | 22:13:56Z 延後補查：`character_lito=2`、`lito_battle_settlement=0`，與 #341 測前筆數一致 | SELECT 兩表 COUNT(*) | `R/counts.tsv` |
| 05 | ⚠️ | 找到歷史來源名與設定路徑，但指定資格屬 `8d26c9d7…`；所查 `50635f7e…` 收據沒有同套完整通過證明，歷史 DB/schema 快照找不到 | 讀 build、qualification、stage receipt／observations／log 及後續 50635 manifest | `R/historical-env.json`（54 個已查檔案） |
| 06 | ✅ | 解析 #341 classpath，列出目錄／檔／ZIP entry hash、時間及六種指定 probe 的來源 commit；缺 git Java 者明列 | 讀 log argv／manifest，逐檔 SHA-256；`git log -1 --format=%H\|%cI -- <java>` | `R/fixtures.tsv`（386 列） |
| 07 | ✅ | F1–F5 各自的差異、反證及待查點如下，沒有據共同失敗斷定環境就是根因 | 對照歷史首案與 #341 首案，不執行 probe | 本報告下表；`R/historical-env.json` |
| 08 | ✅ | 自啟 MySQL 父 PID15224／子 PID18832 正常退出；SHUTDOWN exit 0，43324／42061／42083／42084 無監聽 | 既有 stop 腳本核對 PID、啟動時間、UUID、datadir、其他連線後關閉 | `R/historical-env.json` → `collection_audit.shutdown`；`E/shutdown.json` |

## 已驗證：P24、角色與技能

實例為 `127.0.0.1:43324`、UUID `e3443e40-d090-11f1-8040-4cedfb679789`、datadir `D:/BinEngine/environments/p24/db-data/`。資料查詢使用 `lito83_exp@127.0.0.1`；accounts／static／lito schema 分別為 `p24_accounts_lito_exp`、`p24_world_lito_exp`、`p24_lito_lito_exp`。只送出 SELECT 查詢，JDBC 設為 read-only；沒有 DML／DDL／GRANT，也沒有啟動遊戲或完整伺服器。正常停機沿用管理腳本的既有憑證，只用於身分／其他連線查核與 SHUTDOWN。未連正式 3306／2000，未讀私密 `config/server.properties`。

啟停腳本僅複製到本 worktree 的 scratch，將 holder／收據位置指向本案。既有 start 腳本只比對父 PID，誤報 timeout；實際 listener 屬其子 PID18832，核實後繼續，沒有重複啟動。首次身分 SELECT 的 `current_user` 別名觸發 1064，改為 `db_user` 後成功；無擴權或修改資料。早期 API 空 leases 未當作授權，啟動入口取得有效 lease 才執行。

`SignedHp1` 是 fixture 由 `NativeSignedHpProbe.init` 建立的角色名。`NativeDirectoryRegression.init`／`Regression.character` 的凍結來源在 `baseline-PC-ingress-efd4-v2/source-inputs.zip`；基底 INT=18、WIS=12、原始 INT=18、原始 WIS=12、CharacterSummary 的 HP/MP 上限=100。init 再設等級=99；各首案 setup 將原始 INT=10，F1／F2／F4 的 type=3、F3 的 type=2，session HP=4000/5000、MP=1000/1000。**這些是已讀到的靜態設定，不是 SELECT 得到的角色值，也未證明執行期 source snapshot 跟隨每次覆寫。**

| skill | mpConsume | hpConsume | 材料 | reuseDelay | probability_value / probability_dice |
|---|---:|---:|---|---:|---|
| 33 | 150 | 0 | 40318 × 2 | 5000 | 0 / 4 |
| 44 | 40 | 0 | 40318 × 2 | 500 | 0 / 3 |
| 165 | 50 | 0 | 40319 × 1 | 0 | 0 / 0 |
| 170 | 1 | 0 | 40319 × 1 | 100 | 0 / 0 |

`profile.tsv` 分開記錄「SQL 零列」、「靜態 fixture 設定」與「推導材料數量」：首案程式 grant(count+3)，按目前技能列可推導 33／44 各授予 5、165／170 各授予 4；**不是當時背包的實際快照**。F2／F4 還會在記憶體覆寫機率資料，因此不能只看 skills 表的 probability 欄位解釋是否擲骰。

`counts.tsv` 是 **#419 延後補查**，不是 #341 結束當下的即時測後快照。兩個時間點筆數相同，不能證明中間無寫入、所有欄位未變或沒有其他任務曾使用實驗庫。此次只有 SELECT，無資料修改與還原 SQL。

## 已驗證：歷史資格與 fixture

指定 `cancellation-battle-runtime/build/build.json`、`qualification-current.json` 及各 stage 的 passed 收據，runtime 都是 **`8d26c9d791e094ab3d3cd6c8dbe4218d99f96fcbe73858be5af534106c81cfb8`**。歷史 driver 指向原始 Server 的 `config/server.properties`；log 中 ephemeral source 為 **`公牛/SignedHp1`**，而 #341 為 **`P24LiToExp/SignedHp1`**。歷史 DB/schema 未保存在本次已查公開證據中，不能從現行秘密設定反推。

`50635f7e…` 的路徑是 `integration-lifecycle/build/source-negative-final-frame-builds/20261027T060934904682Z/`。其 current／manifest 可證明該 SHA 的建置、byte-identical reproduction 及有限負向流程驗證；狀態仍含 `unqualified`、`staged...not-promoted`、`common-integration-open`。**在本次已查檔案中找不到 50635 同 SHA、同 F1–F5 套件的歷史完整通過收據**，不可把 8d26 的 passed 身分移用。

`fixtures.tsv` 包含 30 個 classpath 目錄、133 個其中檔案、1 個獨立 ZIP、200 個 ZIP entry、13 個 #341 指定 probe class、5 個歷史 probe class、4 個補充來源／向量。目錄 SHA 定義為依相對 POSIX 路徑排序的 `相對路徑<TAB>檔案SHA256<LF>` UTF-8 清單之 SHA-256；檔案時間為 UTC，ZIP entry 時間無時區，明確另標。目錄時間不等於所有內容的編譯時間。

| probe | 同版取證結果 |
|---|---|
| CancellationProbabilityProbe | #341 候選／Maven class 與歷史 class 同 SHA `08e9eb4f…`；原始 Java 與 git 工作檔逐位元組相同 |
| CallNatureMaterialProbe | #341 候選 class 與歷史 class 同 SHA `860e040c…`；原始 Java 與 git 工作檔相同 |
| WaterLifeMaterialProbe | #341 composed 直接使用原始 build class，SHA `4b7a40d6…`，亦與歷史 composed manifest 固定值相同；Java 相同 |
| CurseClockBoundaryProbe | #341 雙邊及歷史 class 同 SHA `0d8a9c6c…`；獨立 Java 是產生檔，git 對應 `test_curse_clock_boundary.py`，不可捏造 Java commit |
| NativeLightCastProbe | 多個原始 classpath 副本同 SHA `9f842b9a…`，2026-10-18 生成；Java 只在未入 git 的 `resource-database/build/`，找不到 git Java commit |
| WeaponBuffRegression | class 在 `fixture-support.zip`，SHA `21ff814b…`；ZIP 內時間 2026-10-16 09:33:26（無時區），Java 與 git 工作檔相同 |

四個有獨立受追蹤 Java 的檔案，最後 commit 為 `6bb4a27bb6cfbef1639d155700b09c6e1bfbf34f`（2026-11-04 00:36:18 +08:00）；Curse 產生器為 `44a223f1…`（2026-11-05 00:29:38 +08:00）。生成 class 早於 Java 入庫時間，**不等於過期**；沒有以時間順序代替編譯來源證明。Curse 原始目錄與 git 產生器 hash 不同，但已產生的歷史／#341 class 相同。

F5 新舊 manifest 的 **2 份向量、1 份 support、58 個 fixture dependency、10 個測試 class** 固定雜湊全部相同，並逐一重算兩邊所指現存檔案，142/142 次比對吻合（兩邊可能指向同一檔案，並非 142 個獨立檔案）。兩份向量仍為題示的 2026-10-16 輸入；`resourceProfile=tw13081901-v1` 也相同。比對細節在 JSON 的 `F5_pinned_input_comparison`。

## F1–F5：已驗證／推測／待驗證

| 失敗 | 已驗證 | 可疑差異與推測 | 待驗證（交 T144） |
|---|---|---|---|
| F1：Call Nature | 歷史 8d26 首案 `mp=950`，#341 候選為 1000、expectedCost=50；兩邊起始 MP 都是 1000，probe class 相同 | 來源由公牛換成 P24，runtime 也由 8d26 換成 50635；不是「歷史 MP 起始不同」的證據 | ephemeral source 的職業／角色 snapshot 是否跟隨 setup；技能165是否進入材料／目標 admission；不能先判為 DB 材料不足 |
| F2：Cancellation | 歷史有 `Source Cancellation probability...`，#341 雙邊缺此行；probe class 相同，首案程式覆寫 chance=50 | 來源路由／runtime 差異仍可疑；SQL 機率欄位不是 probe 的最終機率 | 先確認材料 admission 與來源 snapshot，找出擲骰前的拒絕點；不先改機率表 |
| F3：Water Life | 現行 SQL mpConsume=1；歷史通過及 #341 都印 `mp-cost=1 mp=999/1000`；class 與歷史固定 hash 相同 | 單憑消耗1或舊 class 無法解釋；斷言同時檢查預期 MP 與 HP，而 log 只提供部分狀態 | 檢查 reference cost、實際 HP、source profile 與 CharacterSummary／session 狀態是否一致；根因未證明 |
| F4：Curse Clock | 歷史首案確有施法、mp-cost=146／854/1000；#341 在第一個 socket delay 斷言就失敗，尚未到 target lock；class 相同 | 首次 socket 輸出前的 admission／發布路徑可疑，沒有證據支持只調 timing | 查是否真的施法／產生第一筆輸出，核對 source stats 與材料，再處理 deadline；不要先放寬延遲斷言 |
| F5：Actor State | 歷史 existing 套件屬8d26；#341 用50635；兩份向量、resourceProfile、固定 fixture bytes 均相同，WeaponBuff 只是共同 check 入口 | 目前沒有向量／support 過期證據；runtime 與環境變更仍需分開看，不能套用 F1–F4 的 source 假說為定論 | 保留事件排序斷言，追到 `NativeDeActorStateRegression` 第138行的 child value／HP enqueue；勿直接歸因 WeaponBuffRegression |

## 產出、驗證與給下一棒的提示

交付：[profile.tsv](issue-419/profile.tsv)、[skills.tsv](issue-419/skills.tsv)、[counts.tsv](issue-419/counts.tsv)、[historical-env.json](issue-419/historical-env.json)、[fixtures.tsv](issue-419/fixtures.tsv)。秘密設定不提交；SELECT 輔助程式、指令清單、原始查詢、啟停收據與靜態反組譯輸出留於本 worktree 的 `scratch/issue-419/`。沒有將本機 fixture 或 JAR 複製入庫。

驗證採 JSON／TSV 解析、指定四列技能／兩列筆數、六種 class 涵蓋、實體檔 SHA、公開輸出敏感字串檢查及 `git diff --check`；純取證文件沒有產品變更，未執行 Maven 建置／測試。`git fetch origin` 後分支與 main 相同基底，無需 merge。

**T144／戰鬥與 fixture 維護者接手**：先確認 50635 的真正歷史資格範圍，再比對 ephemeral source snapshot 與 probe 覆寫的 CharacterSummary／session 資料。保留已通過項與本張證據，不整套重跑；只在另張核准修正／重測任務定位 F1–F5。需要歷史 DB/schema 時，找當時可公開的快照或收據，不讀現行正式秘密設定來替代。調度器保全 #341 與本張 scratch 證據後再依既有流程清理；本執行器不移除 worktree。
