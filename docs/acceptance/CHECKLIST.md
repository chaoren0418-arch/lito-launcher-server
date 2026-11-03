# M1 正式版實機核對表（0.6.71）

操作者：使用者。整理者：Claude。目的：把「published-and-verified」變成**有紀錄、可重跑**的基線，之後每次合併戰鬥候選都用同一張表回歸。

## 前置
| 項目 | 要求 | 核對 |
|---|---|---|
| 伺服器 JAR | 用 T002 合併後 `mvn -DskipTests package` 的產物；記下 SHA-256（`Get-FileHash`） | ☐ |
| 設定 | `config/server.properties` 從 `.example` 複製並填入新密碼；`database.read.only=true`、`persistence.policy=source-readonly-stats-only` | ☐ |
| 資料庫 | MySQL 啟動；`380_db`、`380_lito`、來源庫（`380_ac` 等）可連 | ☐ |
| 登入器 | `release/current/LiTo381Launcher.exe`（T003 後由 `Build.ps1` 產出）；記下版本（視窗標題或 `app/runtime-pins.json`） | ☐ |
| 客戶端 | `D:\lineage開發\0-客戶端` 的 `LiTo.bin` SHA 為 `CE6EE68A…` | ☐ |
| 測試帳號 | 兩個帳號各一角色（戰鬥需要兩人）；記下 `character_lito` 的勝敗與 `last_result_id` 起始值：`SELECT char_name, win, lose, last_result_id FROM character_lito WHERE char_name IN ('A','B');` | ☐ |
| 記錄 | 截圖存 `docs/acceptance/0.6.71/NN-<步驟>.png`；伺服器 console 另存 `server.log` | ☐ |

## 步驟
| # | 操作 | 預期 | 證據 | 結果 |
|---|---|---|---|---|
| 01 | `1.啟動伺服器.bat --check` | 預檢通過，列出 DB 模式與唯讀策略 | console | ☐ |
| 02 | `1.啟動伺服器.bat --smoke` | 短啟動後自行結束，無例外 | console | ☐ |
| 03 | `1.啟動伺服器.bat`（正式） | 監聽 port 32000；`netstat -ano \| findstr 32000` 有 LISTENING | console + netstat | ☐ |
| 04 | 啟動登入器 | 主視窗出現，顯示版本；伺服器來源清單讀自 `lito_source_server` | 登入器畫面 | ☐ |
| 05 | 帳號 A 登入 | 進大廳；角色清單含職業圖示（龍騎士／幻術師 0.6.70 驗過） | 大廳 | ☐ |
| 06 | 八角色翻頁 | 翻頁正常（0.6.69 驗過） | 畫面 | ☐ |
| 07 | 帳號 B 於第二台（或第二視窗）登入 | 兩人都在大廳 | 畫面 | ☐ |
| 08 | A 建房、B 進房 | 房間狀態同步；`lito_room_policy` 規則生效 | 畫面 | ☐ |
| 09 | 開始戰鬥，打到分出勝負 | 傷害／技能正常；觀察 1 個近戰、1 個魔法、1 個變身（`.polyme`）各一次 | 戰鬥畫面 ×3 | ☐ |
| 10 | 結算畫面 | 勝負顯示；職業圖示（**此項歷來無人工回報**） | 結算畫面 | ☐ |
| 11 | DB 核對 | `character_lito` 勝者 win+1、敗者 lose+1，`last_result_id` 更新；`380_lito.lito_battle_settlement` 多一筆 `applied=1` | SQL 查詢截圖 | ☐ |
| 12 | 斷線結算：再打一場，B 在戰鬥中直接關閉客戶端 | A 判勝；`disconnected=1` 收據；`runtime/battle-settlements/` 無殘留待補檔 | SQL + 目錄 | ☐ |
| 13 | GM：以 `accounts.access_level` 為 GM 的帳號使用一個 GM 指令與變身 7200 | 指令生效 | 畫面 | ☐ |
| 14 | Ctrl 強制攻擊（雙視窗） | 行為正確（**0.6.67 起未人工驗**） | 畫面 | ☐ |
| 15 | 正常退出：兩個客戶端登出、伺服器 Ctrl+C | 伺服器正常關閉，無 `hs_err_pid*.log` 新檔；`runtime/` 無鎖檔 | console + 目錄 | ☐ |

## 失敗時收集
`server.log` 對應時段、`LiTo381-Server/loginfo/`、登入器 `app/` 下的 log、Windows 事件檢視器（應用程式）中 `LiTo381Launcher` 或 `java` 的錯誤、`hs_err_pid*.log`。

## 結果回寫
複製本檔為 `docs/acceptance/0.6.71.md` 填寫；Claude 讀取後把失敗項開成 bug 任務（T1xx），並把這張表設為之後每個戰鬥候選 PR 的回歸要求。

## 不在本次範圍
58 列材料技能逐一驗證、NPC/PET 目標語意、HP 生產端（M2）。
