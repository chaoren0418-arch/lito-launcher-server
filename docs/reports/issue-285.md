# Issue #285：T011 新 JAR 實驗驗收

結論：**阻塞**。2026-11-14（Asia/Taipei），P24Lab 啟動命令遭自動核准審查拒絕，回傳 `blocked by policy`，未提供更詳細理由。沒有任何遊戲驗收項目實測；A01–A21 為 ✅ 0／❌ 0／⚠️ 21，不將工具限制誤記為產品失敗，也不以離線前置通過宣稱「部分通過」。

## 做了什麼／已驗證

- 在既有 worktree `D:\lineage開發\3-lito開發\scratch\wt\task-285`、分支 `task/285` 執行，未 checkout／建分支／移除 worktree／遞迴刪除。
- 已讀根 AGENTS.md、docs/REPORT.md、TASKS T011、T127 CHECKLIST、RUNBOOK P／A 部、#83／#284 交接與指定裁定；docs/REPORT.md 未修改。
- `git fetch origin` 後 HEAD 與 origin/main 均為 `55b58bea71330cd19c1c782ef89bac601fa84969`。由此建置新 JAR，沒有使用正式舊 JAR。
- `mvn -B -DskipTests -f lito伺服器/LiTo381-Server/pom.xml package` 成功，74 秒；為遵守不得遞迴刪除的要求，未執行 clean；本輪 javac 編譯 1,185 個來源檔。這不是測試套件通過證據。
- 新 JAR：`84BA0CDE7ABACE28456232B86922B22DD7FBFF98E8B44E8B7BEC5F995A79EDCC`，15,100,535 bytes；4 個 lib 與 JAR 普通複製後逐檔 SHA 相同。原始證據在 `scratch/m4/run-20261113T201710Z/`。
- lease：`chaoren0418-arch/lito-launcher-server#285`，`2026-11-13T20:13:32.331731+00:00` 至 `2026-11-13T21:43:32.331731+00:00`；到期整秒與派工的 `2026-11-13T21:43:32Z` 一致。未更改 lease／調度器資料。
- P24 冷備份保存在本 worktree `scratch/issue-285/cold-backup/db-data/`，1,722 檔／279,501,517 bytes，全部 SHA-256 相同；完整 manifest 在 `scratch/issue-285/backup.json`，不進 git。備份前確認 43324／42061 無監聽並拒絕 reparse point／硬連結。
- 實驗啟動命令在執行前遭工具拒絕。沒有 DB 連線、DDL／DML、授權變更、seed、伺服器／客戶端啟動。沒有 SQL 前後值可以比較；[restore.sql](issue-285/restore.sql) 明示無需還原。
- 收尾時 43324／42061／42083／42084 無監聽、本輪 owned PID 集合為空。看見其他既有 mysqld PID 4144，路徑無權讀取；未連線、未停止、未推定其用途。未啟停正式服務。

## 逐項記錄

本表的 5 個 ✅ 僅為前置檢查；21 個遊戲／啟停驗收均沒有實測。未產生遊戲 PNG，不以文字渲染或舊輪截圖替代。

| 編號 | 結果 | 實際觀察（現象） | 重現步驟 | 證據路徑 |
|---|---|---|---|---|
| P1 | ✅ 通過 | 有效 lease 屬於本任務，到期與派工一致。 | 唯讀 GET /api/state | [issue-285/preflight.json](issue-285/preflight.json) |
| 00 | ✅ 通過 | Java 17 Maven package exit=0；測試依清單略過。 | 執行本報告建置命令 | [issue-285/build-summary.txt](issue-285/build-summary.txt) |
| P3 | ✅ 通過 | 5 個 JAR 副本 SHA 相同、22 鍵、runtime 空且受 git 忽略。 | 執行 tools/acceptance/m4-prepare.ps1 | [issue-285/artifact-hashes.txt](issue-285/artifact-hashes.txt) |
| P4a | ✅ 通過 | 完整冷備份 1,722 檔／279,501,517 bytes，逐檔 SHA 一致。 | 確認實驗埠無監聽，再普通複製並比對 | [issue-285/preflight.json](issue-285/preflight.json) |
| P4b | ⚠️ 無法判定 | 啟動 P24Lab 遭拒；實例身分、schema、欄位／權限無線上核實。 | 以既有 mysql.ini 啟動實驗 mysqld | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt) |
| P5a | ✅ 通過 | 原 BIN SHA CE6EE68A…、兩份 litoddraw.dll 相同、無客戶端 ddraw.dll。 | 唯讀 Get-FileHash／Test-Path | [issue-285/preflight.json](issue-285/preflight.json) |
| P5b | ⚠️ 無法判定 | 未提供 m4-start-client；現有 Logging=1 依原始碼會寫 DLL 同目錄，執行前須解決只讀限制。 | 讀 RUNBOOK P5、ini 及 ddraw_proxy.cpp:171–233 | [issue-285/preflight.json](issue-285/preflight.json) |
| P6 | ⚠️ 無法判定 | 帳號、GM、技能與道具資料未線上查驗，未 seed／擴權。 | 恢復環境後依 D1–D14 查驗 | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt) |
| P7 | ⚠️ 無法判定 | DB 未啟動，未執行基線 SQL。 | 受限帳號執行 CHECKLIST Q1 | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt) |
| 01 啟動伺服器 | ⚠️ 未執行／無法判定 | 前置 P24Lab 啟動遭自動核准審查拒絕；Java 未啟動。 | 前置完成後依 A01 啟動 Java（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 02 監聽與來源路由 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 依 A02 核對 Java PID 四個監聽及來源 SELECT（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 03 第一遊戲視窗／伺服器選單 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 依 P5 啟動 client-1（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 04 A 登入／角色／大廳 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 以 A 登入並進大廳（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 05 八角色翻頁 | ⚠️ 未執行／無法判定 | 無登入環境，未查當前角色數，不能沿用舊輪結果。 | 以至少 5 角色帳號翻頁（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 06 實際相容層載入 | ⚠️ 未執行／無法判定 | DLL 檔案雜湊相符，但沒有遊戲 PID，無實際載入證據。 | 查本輪 LiTo.bin PID 的載入模組（前置恢復後） | [issue-285/preflight.json](issue-285/preflight.json)；無本步 PNG |
| 07 B 第二視窗／大廳 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 啟動 client-2 並以 B 登入（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 08 建房／進房 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | A 建死鬥房、B 加入（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 09 準備／開戰 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 保存戰績前值，雙方準備（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 10 近戰攻擊 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | A 對 B 執行一次近戰（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 11 魔法技能 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 以技能 4 對 B 施法並比對 MP（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 12 變身 | ⚠️ 未執行／無法判定 | 無遊戲環境，GM 前置未線上核實；未提升權限。 | GM 在戰鬥輸入 .polyme 6080（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 13 完成戰鬥 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 一方在 180 秒內擊敗另一方（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 14 結算畫面／職業圖示 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 核對勝負名單及職業圖示（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 15 戰績／收據前後 SQL | ⚠️ 未執行／無法判定 | 未連 43324，沒有戰績／收據前後快照，不能判定結算。 | 執行 Q1 並核對積分及 applied 收據（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 16 斷線結算 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | 另開一場並關閉 B，核對 SQL/WAL（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 17 GM 指令 | ⚠️ 未執行／無法判定 | 無遊戲環境，GM 前置未線上核實；未提升權限。 | 已備妥 GM 執行 .loc／.polyme（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 18 GM 魔杖 | ⚠️ 未執行／無法判定 | 無遊戲環境，GM 前置未線上核實；未提升權限。 | 已備妥 GM 取得 42501 並點地面（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 19 Ctrl 強制攻擊 | ⚠️ 未執行／無法判定 | P24Lab 啟動遭工具拒絕，無實驗服務，未執行。 | B 重登後以雙視窗核對 Ctrl 攻擊（前置恢復後） | [issue-285/startup-rejection.txt](issue-285/startup-rejection.txt)；無本步 PNG |
| 20 正常退出 | ⚠️ 未執行／無法判定 | 沒有本輪客戶端／Java，正常退出未測；已確認無本輪遺留程序與實驗監聽。 | 客戶端正常離開後結束本輪 Java（前置恢復後） | [issue-285/preflight.json](issue-285/preflight.json)；無本步 PNG |
| 21 P24Lab 關閉／lease 交接 | ⚠️ 未執行／無法判定 | P24Lab 未啟動，關閉流程未測；lease 隨 RESULT 交回調度器，未自行改鎖。 | 正常關閉自己啟動的 P24Lab 並交回 lease（前置恢復後） | [issue-285/preflight.json](issue-285/preflight.json)；無本步 PNG |

## 異常：現象／步驟／證據

| 現象 | 步驟與影響 | 證據 |
|---|---|---|
| 自動核准審查拒絕 P24Lab 啟動，只回傳 `blocked by policy` | P4b；A01–A21 無法建立實驗環境；未重試或繞過 | [拒絕紀錄](issue-285/startup-rejection.txt)、本次對話工具輸出 |
| 客戶端 `Logging=1`；原始碼 Log 以 append 寫 DLL 同目錄 `lito381-compat.log` | P5b；與「不改 0-客戶端」限制衝突的風險，未啟動驗證、未改 ini／DLL | [離線核對](issue-285/preflight.json)；`lito登入器/LiTo381-Launcher/native/ddraw_proxy.cpp:171–233` |
| T128 明列未交付 m4-start-client | P5b；正式登入器會讀正式設定，故沒有啟動登入器 | `tools/acceptance/README.md`「做不到或沒有做的項目」、RUNBOOK P5 |

## 推測／待驗證

- 推測：現有 DLL 與相應原始碼的日誌行為一致，啟動會在客戶端目錄 append log；本輪僅做靜態查核，尚未以動態檔案監視證實。
- 待驗證：資料庫實例、schema、授權、角色前置、server ready、四個監聽、全部 A01–A21、結算 SQL 前後對照及實機截圖。
- 未比對同 commit CI artifact；未執行產品程式碼修正或測試套件。所有未驗證結果保持 ⚠️。

## 產出路徑／給下一棒的提示

- [依本張指定名稱填寫的 CHECKLIST](../acceptance/0.6.71.md)；實際驗收基準是上述 Maven JAR，`0.6.71` 僅沿用派工檔名。
- [截圖目錄說明](../acceptance/0.6.71/README.md)：本輪 0 張 PNG。
- 公開證據：`docs/reports/issue-285/`；私密／完整備份與 build log 留 `scratch/issue-285/`，JAR／lib／設定範本留本輪 run，不提交。
- 下一棒由本機調度器／執行環境管理者處理工具拒絕；這不是缺少使用者授權。另由客戶端啟動前置任務處理 P5 及日誌輸出位置，遵守不改原客戶端的限制。
- 環境恢復後重測 A01–A21（本輪全部 ⚠️）；前置的 ✅ 不需要無理由重做，但新輪應使用當時 main 的產物並維持有效 lease／隔離守門。
- lease 隨本次 RESULT 交回調度器回收；本輪未呼叫釋放 API，不宣稱 lease 已釋放。保留 worktree 與冷備份，禁止遞迴刪除。
