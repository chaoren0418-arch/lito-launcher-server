# M1 實驗環境實機核對表（正式 0.6.71＋P24 實驗 schema）

操作者：**dot 執行並截圖，使用者觀看驗收**。本表是待執行範本，不代表實機通過。以正式 0.6.71 建立「登入 → 大廳 → 技能 → 進房／戰鬥 → 結算 → 正常退出」的可重跑基線；依 Issue #283 指定交付基準，不以 Maven 產物或戰鬥候選替換。

## 前置

原始專案根目錄為 `D:\lineage開發\3-lito開發`，只讀取指定正式成品。以下 `scratch/t014/`、證據及結果文件路徑皆相對於**本次執行 worktree**，不可寫回原始專案目錄。不得連接正式服務／正式資料庫、啟停其程序，或修改正式成品、遊戲資源、硬連結與玩家實表。

| 項目 | 操作／要求 | 預期 | 證據 | 結果 |
|---|---|---|---|---|
| 交接與 lease | 讀取 [#83 報告](../reports/issue-83.md)；啟動 P24Lab 前先向調度器取得 `p24lab` lease，核對持有人、有效期間與無其他工作衝突 | 本次執行者在整個 DB 使用期間持有有效 lease；未取得不得啟動或連線 | lease 識別、持有人、取得／到期時間（不含秘密） | 待執行 |
| 正式 JAR | 原始專案的 `lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar` 只讀；用 `Get-FileHash -Algorithm SHA256` 記錄，普通複製到 `scratch/t014/LiTo381-SharedResources-candidate.jar` 後再次雜湊，不建立硬連結、不覆蓋原檔 | 來源、副本與下列基準 SHA-256 相同；若副本已存在先核對，不盲目覆寫 | 來源／副本完整路徑、大小、SHA-256 | 待執行 |
| 正式登入器 | 使用原始專案 `lito登入器/LiTo381-Launcher/release/current/LiTo381Launcher.exe` 的正式 0.6.71 成品；由 dot 準備保留必要配套的實驗副本，啟動前核對實際連線目標 | EXE 雜湊與下列基準一致；登入目標為本次 loopback 實驗端點，日誌／可寫設定留在 worktree 的 `scratch/t014/` | 來源與副本雜湊、版本畫面、去機密的目標位址 | 待執行 |
| 實驗設定 | dot 依 #83 私密交接建立 `scratch/t014/settings/experiment.properties`，使用下列統一格式；DB 帳密取自 #83 的 `isolated-test.properties`，不可印出或提交 | 設定位於本次 worktree、受 Git 忽略；所有必要欄位齊備，無舊環境設定混入 | 只列允許公開的端點／schema／路徑；`git check-ignore` 結果 | 待執行 |
| P24Lab | 持 lease 後依既有流程啟動／交接 P24Lab，核對 `127.0.0.1:43324` 的身分與 #83 收據；確認三個 `_lito_exp` schema 及受限帳號仍有效 | 實例 UUID、datadir、bind 與 #83 相符；來源路由僅指向 `p24_accounts_lito_exp` | P24Lab 身分核對摘要及下方來源路由 SELECT | 待執行 |
| 測試帳號 | 只使用 #83 建立的兩組普通測試登入／角色，本文稱 A、B；憑證由 dot 依 `test-accounts-private.properties` 私密交接取得；執行下方基線 SELECT | A、B 各一角色，保存當次 `wins`、`losses`、`version`、`last_result_id`，不假設仍為零 | 去機密的角色對照及戰績／收據前值，禁止截取登入密碼 | 待執行 |
| 客戶端與相容層 | 資源引用 `D:\lineage開發\0-客戶端`；只讀核對 `LiTo.bin` SHA 前綴 `CE6EE68A…` 與客戶端目錄 `ddraw.dll`／`litoddraw.dll` 的實際入口檔名、存在狀態、SHA-256，對照登入器指定／實際載入名稱 | 客戶端版本相符；相容層入口與引用相符；不預設兩個 DLL 都應存在，不改名、替換或複製遊戲資源 | 完整路徑、檔名／雜湊清單、目錄截圖；載入證據在步驟 06 補齊 | 待執行 |
| 實驗輸出與 WAL | Java 工作目錄固定 `scratch/t014/`；設定放在其 `settings/` 子目錄，讓相對 runtime／結算 WAL 留在 `scratch/t014/runtime/`；設定 `capture.directory=../captures`、`logs.directory=../logs` | 本輪沒有舊待補 WAL；既有待補資料先記錄並交接，不刪除、不混用；不讀取正式 runtime | 實際工作目錄、輸出路徑與啟動前 `runtime/battle-settlements/` 清單（不存在亦記錄） | 待執行 |
| 額外案例前置 | 核對八角色翻頁、技能／變身、GM 指令／魔杖所需角色、權限與道具 | #83 僅提供兩組普通帳號，不能當作 GM 或八角色資料已備妥；不足時記錄阻塞，由調度器另派實驗資料準備，不自行改權限／加資料 | 各案例可用前置與缺口清單 | 待執行 |
| 採證 | 原始 console、SQL、日誌存 `scratch/t014/evidence/`；公開截圖經去機密後存 `docs/acceptance/0.6.71/NN-<步驟>.png` | 每步可追溯實際時間、結果及證據；機密設定、帳密、原始資料不進 Git | 證據索引與去機密核對 | 待執行 |

2026-11-06 在原始專案唯讀取得的成品 SHA-256（執行當次仍須重算；不符時先交調度器確認基準，不自行換版本）：

| 成品 | SHA-256 |
|---|---|
| 正式伺服器 JAR | `E1498871C7BE6B287821C57EF5D95933DB1740D9B4D4A80F6C3465F21D99D498` |
| 正式登入器 EXE | `0BF5DDF2E479EF0B38F3416FF47CFF981AF5A6F4B5095719E24DC3BA9F624280` |

實驗設定的固定欄位如下；這是**必要覆核片段，不是完整可啟動設定**。其餘統一格式必要欄位由 dot 依 #83 私密設定補齊；不要加入舊格式唯讀旗標或把帳密填進本文件。#83 兩份私密交接檔位於原始專案的 `scratch/wt/task-83/scratch/t020c/`，本次文件編修不讀取其內容。

```properties
bind=127.0.0.1
game.bind=127.0.0.2
port=42083
database.jdbc.base.url=jdbc:mysql://127.0.0.1:43324/
database.static.database=p24_world_lito_exp
database.lito.database=p24_lito_lito_exp
capture.directory=../captures
logs.directory=../logs
```

## 步驟

結果逐項填「通過／失敗／阻塞／未執行」，不能沿用舊版通過紀錄。每個失敗或異常均須新增一列「現象／步驟／證據」；**失敗不中斷其餘可執行步驟，除非無法繼續**。例如缺 GM 前置只阻塞 GM 案例；lease、隔離端點、成品雜湊不符時停止依賴該前置的啟動／連線，仍可完成其他唯讀核對。

| # | 操作 | 預期 | 證據 | 結果 |
|---|---|---|---|---|
| 01 | dot 完成前置、核對實驗埠無衝突；在 `scratch/t014/` 以 `java -jar <正式 JAR 副本> <實驗設定>` 啟動，從命令開始保存 stdout／stderr 到啟動完成，之後持續收集 | Java 17 使用指定正式 JAR 與實驗設定，初始化完成、沒有啟動例外；不省略設定參數以免使用預設環境 | 啟動命令／工作目錄／時間／PID、`server-console.log`、啟動完成截圖 | 待執行 |
| 02 | 核對 Java PID 的監聽位址及來源路由；來源埠依 SQL 的實際 `source_id` 計算 | 登入監聽 `127.0.0.1:42083`；遊戲來源監聽 `127.0.0.2:(42083 + source_id)`，不得把主埠當來源埠；只有實驗端點 | console＋`Get-NetTCPConnection -State Listen` 的相關 LocalAddress／LocalPort／OwningProcess、路由 SELECT | 待執行 |
| 03 | 啟動正式 0.6.71 登入器實驗副本，檢查伺服器選擇 | 主視窗與版本正確；來源清單對應 `p24_lito_lito_exp.lito_source_server`，連線到實驗端點 | 登入器畫面、去機密的連線證據 | 待執行 |
| 04 | 帳號 A 登入、選角色進大廳 | 登入成功；角色及職業圖示正確，進入大廳 | 登入後角色清單與大廳截圖 | 待執行 |
| 05 | 執行八角色翻頁；若 #83 資料不足則記錄阻塞 | 有足夠實驗角色時可正確翻頁；不足不算通過、不臨時新增角色 | 翻頁前後截圖，或角色數不足的證據／缺口 | 待執行 |
| 06 | 唯讀核對本輪客戶端實際載入的相容層入口，對照前置的 `ddraw.dll`／`litoddraw.dll` 檔名 | 實際載入的檔案路徑／名稱與登入器引用一致；缺少載入證據不可僅憑檔案存在判通過 | 模組路徑或載入日誌＋目錄截圖；不改名或覆寫 DLL | 待執行 |
| 07 | 帳號 B 於第二視窗登入同一實驗來源 | A、B 都在大廳，角色與來源正確 | 雙視窗大廳截圖 | 待執行 |
| 08 | A 建房、B 進房，選擇會計入戰績的房間模式 | 房間狀態同步；`p24_lito_lito_exp.lito_room_policy` 規則生效；不以不計分測試房核對勝敗回存 | 房間／模式／兩人狀態截圖 | 待執行 |
| 09 | 開始戰鬥，記錄雙方角色與當場識別 | 兩人進入同一戰鬥場景、可操作；若地圖資產缺失則記錄實際錯誤 | 戰鬥場景截圖、對應時間與日誌 | 待執行 |
| 10 | 執行一次近戰技能，記錄技能名稱／ID 與目標 | 技能能施放、命中／效果與傷害表現符合規則 | **近戰技能獨立截圖一張**＋施放資訊／日誌 | 待執行 |
| 11 | 執行一次魔法技能，記錄技能名稱／ID 與目標 | 魔法施放與效果符合規則 | **魔法技能獨立截圖一張**＋施放資訊／日誌 | 待執行 |
| 12 | 執行一次變身（`.polyme`），記錄使用參數 | 變身指令與外觀生效；權限或前置不足則記錄阻塞，不自行提升普通帳號權限 | **變身獨立截圖一張**＋指令／回應 | 待執行 |
| 13 | 完成這一場戰鬥，分出勝負 | 正常進入結算，勝負與實際戰況一致 | 戰鬥結束截圖與時間 | 待執行 |
| 14 | 核對結算畫面及職業圖示 | 勝負、雙方角色與職業圖示正確 | 結算畫面截圖 | 待執行 |
| 15 | 執行下方戰績／收據 SELECT，與前值比對 | 勝者 `wins + 1`、敗者 `losses + 1`；雙方 `last_result_id` 更新並對上各自 `applied=1` 收據；收據依角色產生，不假設全場只有一列 | SELECT 前後結果、`battle_id`／`result_id` 對照及截圖 | 待執行 |
| 16 | 再打一場前保存戰績／收據；戰鬥中關閉 B 的客戶端，等待結算 | A 判勝；B 收據 `disconnected=1`；勝敗各增一次，收據均 `applied=1`，實驗 WAL 無殘留 `.pending`；無法開第二場則記錄阻塞 | A 畫面、第二場 SQL 前後對照、`scratch/t014/runtime/battle-settlements/` 清單、日誌 | 待執行 |
| 17 | 僅在調度器已安排有效的實驗 GM 前置後，執行一個 GM 指令與變身 7200 | 指令與變身生效；#83 普通帳號不能直接視為具備 GM 資格 | 指令／變身畫面，或 GM 前置不足證據 | 待執行 |
| 18 | 在相同實驗 GM 前置下，裝備／使用 GM 魔杖並以滑鼠點選目標，記錄點選對象與預期動作 | 點選目標與魔杖實際作用對應，沒有點選失效、錯誤目標或崩潰 | 滑鼠點選前後截圖＋對應日誌；前置不足記錄阻塞 | 待執行 |
| 19 | B 重新登入後，以雙視窗驗證 Ctrl 強制攻擊 | 按鍵與目標反應符合規則，兩視窗觀察一致 | 雙視窗操作／目標反應截圖 | 待執行 |
| 20 | 兩個客戶端正常登出、關閉登入器，對本次實驗 Java 主控台按 Ctrl+C | 正常關閉，無新增 `hs_err_pid*.log`；Java PID 與實驗監聽埠釋放、WAL 無待補；`owner.lock` 可留檔，應確認鎖已釋放，不以檔案存在判失敗 | 退出畫面、console、PID／監聽核對與實驗 runtime 清單 | 待執行 |
| 21 | 依 lease 交接約定正常關閉本次啟動的 P24Lab 或交回其管理者，再向調度器釋放 `p24lab` lease | 無本次遺留連線／程序；不停止他人程序、不刪 schema、帳號或憑證 | 關閉或交接紀錄、lease 釋放時間 | 待執行 |

步驟 01 的 PowerShell 範例（**僅由 dot 在實機任務完成前置後執行**；先確認 `evidence/` 存在，重跑時保存舊證據，勿覆蓋）：

```powershell
# 目前目錄：本次 worktree 的 scratch/t014/
java -jar .\LiTo381-SharedResources-candidate.jar .\settings\experiment.properties 2>&1 |
    Tee-Object -FilePath .\evidence\server-console.log
```

主控台需保存從啟動到初始化完成的完整輸出，並持續收集至結束；步驟 02 用另一個主控台唯讀核對。啟動失敗時仍保存退出碼與錯誤，不改用其他成品或正式端點。

## SQL 前後對照（僅 P24 實驗庫）

由 dot 使用 #83 受限 DB 帳號、明確指定 TCP `127.0.0.1:43324` 執行。以下查詢只讀 `p24_accounts_lito_exp`／`p24_lito_lito_exp`，不做 DDL、DML、擴權或建資料。先把 `<角色A>`、`<角色B>` 替換為私密交接中兩個實驗角色名稱，採參數綁定或正確 SQL 字串跳脫；不得執行未替換的示意值。公開結果以 A／B 去識別。

```sql
SELECT source_id, display_name, character_database, enabled
FROM p24_lito_lito_exp.lito_source_server
ORDER BY source_id;

SELECT char_obj_id, char_name, wins, losses, version, last_result_id
FROM p24_accounts_lito_exp.character_lito
WHERE char_name IN ('<角色A>', '<角色B>')
ORDER BY char_obj_id;

SELECT result_id, battle_id, source_database, char_obj_id, char_name,
       won, disconnected, applied
FROM p24_lito_lito_exp.lito_battle_settlement
WHERE source_database = 'p24_accounts_lito_exp'
  AND char_name IN ('<角色A>', '<角色B>')
ORDER BY battle_id, char_obj_id, result_id;
```

來源清單應僅有 #83 建立的實驗路由，啟用列的 `character_database` 必須為 `p24_accounts_lito_exp`。記錄實際 `source_id`；例如值為 1，來源遊戲埠就是 42084，不預設 ID 一定為 1。以每場新增的 `battle_id`／`result_id` 與戰績前後值核對，不能拿既有收據冒充本次成功。

## 失敗／異常記錄（M2 盤點來源）

每個失敗、異常或阻塞步驟各寫一列；即使後續重試通過仍保留原始現象及重試證據。無法繼續的步驟標「阻塞」並引用前置失敗編號；其餘步驟照常執行。GM、翻頁、技能等資料不足不得填「不適用」來當作驗收通過。

| 現象 | 步驟 | 證據 |
|---|---|---|
| 待執行後填寫：實際／預期差異、發生時間、能否繼續及影響 | 前置項目或步驟編號 | 截圖路徑、去機密日誌行號／時間段、SQL 前後對照；阻塞時附前置失敗編號 |

收集該時段 `scratch/t014/evidence/server-console.log`、實驗 logs／captures、登入器實驗副本日誌；需要時唯讀收集 Windows 應用程式事件中的 `LiTo381Launcher`／`java` 錯誤及本次 `hs_err_pid*.log`。原始資料保留本機；公開證據先遮蔽帳密與非本次資料。缺少證據則明列缺少什麼，不補造。

## 結果回寫與交接

dot 將本表複製為 `docs/acceptance/0.6.71.md`，填入執行日期、操作者、worktree、成品雜湊、lease 紀錄、每步結果／證據及失敗清單。報告分「已驗證／推測／待驗證」，由使用者觀看畫面驗收；未跑、阻塞與待確認項目不得宣稱通過。

調度器／Claude 依「現象／步驟／證據」逐項盤點 M2 缺口與前置任務，保留這份正式 0.6.71 基線作為後續候選回歸依據。58 列材料技能逐一驗證、NPC/PET 目標語意、HP 生產端、斷線長時間壓力與災難復原不在本次範圍。
