# LiTo 0.6.60

**基準：0.6.60。** 聯合備份、保留規則與還原方式見 [基準說明](docs/BASELINE-0.6.60.md)；本次清理收據為 `docs/baseline-0.6.60-cleanup-result.json`。

啟動入口：`release/current/LiTo381Launcher.exe`。預設正式 standard 流程、200% 視窗；遊戲目標維持 `D:/lineage開發/0-客戶端/LiTo.bin`。

| 目錄 | 用途 |
|---|---|
| `src/` | 現行 C# 登入器及配套 Python 啟動 helper |
| `native/` | 現行相容 DLL 原始碼與測試；shared-resources 為共同資源載入程式 |
| `app/` | 有雜湊驗證的啟動模組、共同動畫目錄、相容 DLL、固定伺服器 JAR/lib |
| `release/current/` | 唯一目前發佈版本，所有捷徑指向此處 |
| `server-patches/` | 已發佈伺服器修補原始碼及回歸案例；不是另一份啟動版本 |
| `server-profiles/` | 登入器設定範本 |
| `tools/` | 單一建置、驗證、備份及清理工具 |
| `docs/` | 容量盤點、清理清單、驗證結果 |
| `backups/` | 0.6.60 聯合基準、0.6.26 離線回復資料及集中歷史封存 |

## 建置與驗證

PowerShell 執行 `./Build.ps1`。直接由 src 建置並更新 release/current，不再依賴舊版目錄範本。結束會清除 src/bin、src/obj 等可再生快取。

`tools/verify_current.py` 驗證啟動雜湊、固定伺服器及已移除的說明頁碼。加上 `--native` 會實際啟動正式測試場次，等待原生共同資源就緒後關閉自己的場次；執行前請關閉本專案遊戲。

`app/server/test_formal.py` 在 port 32020 使用 SQL 測試副本檢查正式伺服器連線及清理。原生說明測試為 `native/build-new-tests.cmd`。

伺服器固定 JAR 的 SHA256：`ebf5ee66f051700ac4b8f524847d7372af8065c4dbb5d91b2f74e03dadff4e7b`。0.6.36 已加入共用資料庫與來源登入隔離；0.6.38 保留該核心，不改戰鬥／技能規則。歷史診斷 CLI 舊路徑僅供查考，現行 GUI 正式啟動以 DevelopmentStartup 為準。

## 備份與回復

- `tools/backup_current.py` 更新唯一 `backups/current-0.6.35.zip` 並逐檔驗證，不包入快取、場次或其他備份。
- `tools/restore_current.py` 預設只驗證；關閉登入器及遊戲後加 `--apply` 才回復。
- `tools/restore_rollback26.py` 預設驗證 0.6.26；加 `--apply` 才重建舊版目錄與客戶端副本。原始資源來自 `D:/380/天堂(Lineage 3.81C) (1)`，差異檔及每個檔案 SHA256 保存在 .26 備份中，不再常駐第二套客戶端。
- 備份不含 SQL 快照或完整遊戲資源。Java、Python、0-客戶端、伺服器專案 formal 設定及 SQL 副本為外部相依項。

## 容量規則

1. 只使用 release/current，不新增 publish-版本／launcher-src-版本整套副本。
2. 大型遊戲資源只使用共同來源，不再複製到登入器。
3. `tools/Clean-Generated.ps1 -Apply` 清除可再生快取。
4. 關閉登入器後執行 `tools/Clean-CompletedRuns.ps1 -Apply`，保留最近 5 個已確認清理完成的場次；未完成或異常紀錄保留。
5. 目前備份覆寫單一槽，0.6.26 是唯一另行保留的歷史基準。歷史證據壓縮檔不再納入每次備份。


## 0.6.38 同一視窗選服與登出

桌面「LiTo 0.6.60 正式啟動器」指向 release/current。原生伺服器選單 → 空白帳密頁 → 角色選單，全程保留同一遊戲程序與視窗。帳密只交給已選來源的登入入口；登入成功後驗證單筆来源目錄，接續原生 C8 交接。

角色選單按「伺服器」會關閉該帳號連線，取消舊連線通知與重連計時器，清除原生帳密、自動登入及交接快取，再於同一視窗返回三服選單。重新選服必須再次輸入帳密。0.6.37 的關閉／重啟遊戲流程已移除。

接線：native/source_login_order.inc、native/character_source_return.inc、app/lito_runtime_probe_shared.py、src/development_startup.py。LiToSourceSession 為程序內連線入口與世代資訊，不含帳密；資料庫來源限制仍由伺服器驗證。每次新帳號階段重設角色頁建立標記，避免第二次登入停在登入頁。

原生路由測試：native/build-source-tests.cmd。完整原生回呼測試：native/build-source-ui-test.cmd 及 tools/native-flow-build/verify_same_window.py，使用隔離 52001–52003 連接埠與記憶體測試帳號，不存取 SQL；測試 DLL 不發佈。結果見 docs/same-window-source-verification.json。正式 DLL 另外測試選服後空白登入頁、程序數及資源啟動。實際操作手感仍待使用者驗收。

變更前精簡備份：backups/before-native-source-order-0.6.36.zip；0.6.35 基準仍保留。

## 0.6.39 角色頁返回帳密

角色頁 slot 6「返回」（資源 0x183D、原回呼 0x494D50）改接帳號登出，保留已選來源，重新建立空白帳密頁及該來源登入連線。slot 4「伺服器」仍登出至來源選單；slot 5「確定」與 slot 7「離開」不改。共用清理會釋放舊帳號、清除交接帳密／自動登入及重設角色頁建立標記，保持同一 PID／HWND。

verify_same_window.py 增加原生「返回」控制回呼 → 空白帳密 → 無自動 C_LOGIN → 同服重登入，再測「伺服器」→ 換服重登入。測試使用隔離記憶體帳號，不存取 SQL。

## 0.6.42 即時戰績與排行榜

結算返回大廳／刷新房間清單時，伺服器送出來源及角色限定的 LITOSTAT1 訊息；相容層只更新原生戰績、勝率及排名快取，不重新送選角目錄，不切換畫面。保留原生配色及排版，原生大廳 WORD 計數以 65535 飽和顯示，角色目錄與 SQL 完整值不截斷。

共用資料庫模式改為戰績或積分變動後，下次排行／大廳查詢即時計算，不等待每日 05:00。月榜保留原月期邊界；20 場上榜門檻、積分排序、各服／頻道／職業篩選保留。排行榜查詢不回寫 SQL；保存結果的版本與勝敗比對仍有效，不因即時排名欄位改變而拒絕下一場結算。

此改動不新增資料庫寫入範圍：只保存 character_lito 勝敗。積分、月榜與排名變動仍是程序記憶體資料，伺服器重新啟動後依既定策略重新建立。

現行核心變更來源：server-patches/repair-040/LiveBattleStats.java 與 Patch.java；以 build.py 對固定的 build/base.jar 增量建置，保留其他既有修補，不使用根目錄舊 JAR 覆蓋。舊核心 SHA256 與回復配套保存在 backups/before-live-stats-0.6.39.zip 的清單。原生來源為 native/live_stats.h、live_stats.inc。

驗證：repair-040/Regression.java（不連 SQL）、native/build-live-stats-tests.cmd（使用伺服器實際封包）、tools/native-flow-build/verify_live_stats.py（獨立假帳號的原生收包及大廳欄位讀回）；結果見 docs/live-stats-native-verification.json。實際正常戰鬥後的畫面仍需使用者驗收。

0.6.41 新增結算專用戰績寫入閘門與暫存清理，詳見伺服器 README 的記憶體生命週期。修補入口：`server-patches/repair-041/build.py`。

0.6.42：伺服器單一 server.properties、SQL lito_source_server 來源表、統一負重及 GUI/BAT JVM 設定。現行修補入口 `server-patches/repair-042/build.py`；不再讀取來源檔及資料庫分檔。

## 2026-10-12 啟動驗證修正

正式 JAR 雜湊比對統一大小寫；驗證工具直接執行正式啟動模組的輸入檢查，測試大小寫及錯誤雜湊拒絕。另修正 JVM 結束期間的映像查詢競態，使原 SQL 錯誤可見且保留程序 handle 正確清理。

依使用者指示將 `380_lito.lito_source_server` 的水蛇（ID 3／380_ac_3）設為 enabled=0，保留資料列；目前啟用公牛、天使。原生啟動驗證通過 resources_ready、exit=0 及 cleanup_verified，收據見 `docs/startup-smoke.json`。

## 0.6.43 七職業選角相容（2026-10-12）

來源角色 2266 的 Type=5、Sex=0、Class=6658 原本正確；舊 LiTo 的選角名稱表把第六種職業指向「技師」，圖片查詢僅有五種職業分支。第七種職業還會越界讀取名稱索引。

相容 DLL 的 `native/class_compat.h`、`class_compat.inc` 補齊七職業及男女對應，龍騎士／幻術士名稱使用共用文字表 1302–1305。四組新職業選角描述保留 TW13081901 的等待、指向、選取與循環動畫及播放速度，使用共用 Sprite PAK 的 841–1127.png。擴充原生 PNG 物件表至 1128 筆，同時更新繪圖、配置及釋放上限；使用原生配置器與解構流程，不累積額外副本。新增 PNX 暫存解碼，PAK、LiTo.bin、角色資料與正式伺服器 JAR 不改寫。

驗證：`native/build-class-tests.cmd` 檢查 14 組名稱、無效輸入、動畫範圍與 PNX 解碼；`tools/class_compat/verify_resources.py` 比對保留的 TW 記憶體證據，驗證全部 287 張影格的 PNG CRC 與尺寸（收據 `docs/class-compat-resources.json`）。`tools/verify_current.py --native` 以自行啟動的測試程序檢查實際補丁、1128 個原生圖片物件及四個中文名稱，並通過 resources_ready 與完整清理（`docs/startup-smoke.json`）。選服／返回原生回呼回歸測試通過。200% 下的 2266 選角圖片、動畫、確定與返回操作仍待使用者驗收。

範圍：此次完成選角顯示相容，未宣稱新職業全部戰鬥技能已驗證。伺服器世界外觀保留真正職業及男女 SPR（龍騎士 6658/6661、幻術士 6671/6650）；戰鬥室頭像封包仍有既有 0..4 限制及王族替代，原生房間職業限制亦只有五種選项。這兩條介面需另行擴充及驗證，不能只放寬封包值。

修正前精簡回復檔：`backups/before-seven-class-0.6.42.zip`（約 300 KB，僅相關 DLL、來源及啟動雜湊設定）；發佈仍只保留 `release/current`，桌面捷徑更新為 0.6.43。

## 0.6.44 新職業進入大廳崩潰（2026-10-12）

使用者確認 0.6.43 龍騎士選角成功後，13:40:20 在進大廳時崩潰。原始錯誤已保存在 `app/runs/development-06e4ae4448a84527b4173efab40a7385/LiToError.txt`。堆疊 0x45A11F 對應大廳／房間共用角色資訊圖示：原程式在 0x459E84–0x45A118 僅分支處理職業 0..4；type 5/6 會沿用未建立的物件並呼叫虛擬函式。

相容層改接有界的七職業繪圖函式：舊五職業沿用各自原圖，新兩職業依男女使用共用選角素材（884/941/1013/1102.png）縮圖與原生 61×73 金框。四個固定大小的記憶體快取合計約 35 KB，不新增 PAK／BIN 副本。無效 type/sex 或缺少資源時安全略過圖示，不再讀取未初始化指標；角色職業、來源 SQL、技能判斷均不改成其他職業。

新增 `tools/class_compat/NativeClassFixture.java` 與 `verify_lobby.py`，用現行正式協定／GameHub 加隔離記憶體帳號，測試男女龍騎士、男女幻術士依序登入 → 選角 → 頻道 → 大廳 → 頻道 → 角色選單 → 帳密頁，四條流程皆通過且保持同一 PID／視窗；另在原生 UI 執行緒逐一繪製全部 14 組圖示及拒絕無效輸入。來源目錄設定可唯讀查詢 SQL，玩家帳號／角色使用記憶體測試庫，不寫入玩家資料。收據：`docs/class-lobby-native-verification.json`。測試專用訊息僅編入 `ddraw-source-test.dll` 且限 52001–52008 測試連接埠，不包含在正式 DLL。

0.6.44 正式版另執行 `verify_current.py --native`，核對新繪圖跳轉、圖片表及啟動／清理。這次補齊資訊面板崩潰路徑；伺服器的房間參與者封包仍保留舊版職業替代與五職業限制，尚未宣稱新職業房間規則／全部專屬技能已完成。


### 0.6.45：龍騎士／幻術師技能欄

- 現場 2266 封包已包含 181–195 共 15 個技能。缺口在 LiTo 的 176 筆 LTSpellDesc 與僅五職業的技能排列，非角色庫未讀取。
- `native/class_skills.inc` 為兩職業接上原生技能物件、圖示索引與排列；保留真正職業／技能編號。未學技能不會因排列存在而被授予。
- 保留原先 176 筆 LiTo 技能說明，新增部分由共用 `0-客戶端/Text.pak` 的 `spelldesc-c.tbl` 匯出成小型 `class_skill_names.inc`；圖示對照取自 TW13081901 原始表。未複製或修改共用 PAK。更新資源後執行 `python -B -X utf8 tools/class_compat/verify_skills.py --extract` 驗證來源摘要與內容。
- 字串及指標表使用原生配置／釋放器；固定 256 格圖示索引與有界新職業排列。舊職業的四段初始排列迴圈保留原始起訖位址。
- `tools/class_compat/verify_lobby.py` 以隔離記憶體帳號驗證男女龍騎士／幻術師、法師再切回龍騎士；檢查技能編號、名稱、圖示、返回選角及重新登入。驗收見 `docs/class-skills-native-verification.json`、`docs/class-skills-resources.json`。此項驗收不代表所有職業技能戰鬥效果已逐招實測。
- 0.6.44 DLL 與啟動驗證資料保留於 `backups/before-class-skills-0.6.44.zip`。此修正不改伺服器 JAR 或玩家資料庫。

目前 0.6.50：幻術師九種 BUFF 的原生狀態图示、空地攻擊的基本動作與方向，以及自身名字的游標判斷。伺服器版本與驗證請見相鄰 LiTo381-Server 的 README.zh-TW.md 與 docs/maintenance/illusion-presentation-0.6.50.json。

目前 0.6.51：幻術師 14 個指向技能使用原生選取與明確目標封包；原生結算返回再進戰鬥已通過兩輪隔離測試。詳見 LiTo381-Server/docs/maintenance/illusion-targets-0.6.51.json。

目前 0.6.52：右上角自身位置依原生頁面狀態顯示大廳或實際戰鬥室房號，保留其他玩家位置及原生文字樣式；不改寫原生房間通訊狀態。測試與回復檔案見 docs/profile-location-0.6.52.json。0.6.51 幻術師指向施法已由玩家確認成功。


## 0.6.53 同一啟動器多開遊戲

先按「啟動 LiTo.bin」，共同資源載入完成後，「新增遊戲視窗」會啟用。每按一次增加一個獨立遊戲視窗，共用本次正式伺服器；使用不同帳號，在相同來源、頻道及戰鬥室測試玩家互動。每個視窗自行選服及輸入帳密。

關閉其中一個遊戲不會中止其他視窗；最後一個遊戲關閉後自動停止本次伺服器。關閉啟動器或按「結束全部遊戲與本次伺服器」會結束本次所有視窗與伺服器。多開由同一啟動器管理，無須啟動第二個伺服器。

新增視窗使用各自的原生執行鎖，並以預期位元組驗證的記憶體修補略過互斥鎖及重複視窗的單例退出分支。磁碟 LiTo.bin、相容 DLL 與伺服器 JAR 不變。各視窗有獨立程序識別、資源就緒紀錄及清理收據，失敗時只清理該次新增程序。

驗證：`tools/test_multi_window.py` 實際啟動兩個原生客戶端，驗證共同資源與單一 JVM，先關第一個確認另一個仍執行，再關最後一個確認自動清理。`--release --stop-all` 驗證發佈版結束全部流程。結果見 `docs/multi-window-test.json`、`docs/multi-window-stop-test.json`。玩家登入後互動待人工驗收。變更前備份為 `backups/before-multi-window-0.6.52.zip`。


### 0.6.54 跨來源大廳房間同步

建立、編輯、加入、離開戰鬥室後，向同頻道、仍停在大廳的其他玩家重送原生房間清單。來源資料庫不影響同頻道通知；不把清單送進其他頻道或房內畫面。沿用原有刪房及預設房間重設封包，某一接收者已斷線時繼續通知其他人，不中斷目前玩家離房。

已用四個記憶體測試玩家驗證跨來源同步、房間名稱、人數、刪除、頻道與畫面階段隔離；原版 JAR 可重現漏送通知。正式資料库及客戶端 DLL 不變，需重啟伺服器。伺服器測試入口：`tools/room-directory/test.py`，證據：伺服器 `docs/maintenance/room-directory-0.6.54.json`。原生雙玩家畫面待實測。


## 0.6.55 多視窗拖曳隔離

修正相容 DLL 的標題列拖曳備援：必須是滑鼠實際命中的前景視窗，且左鍵剛按下，才取得拖曳控制。放開左鍵或切換前景即停止；按住左鍵越過另一視窗的標題列不會同時帶動它。保留原生標題列移動流程。

`native/build-caption-test.cmd` 通過重疊、按住跨越、前景切換與釋放測試。正式雙開與共用單一伺服器測試通過：關閉第一視窗不影響第二視窗，全部關閉後完成程序清理。桌面擷取工具重試仍逾時，實際拖曳手感待使用者驗收，詳見 `docs/caption-isolation-0.6.55.json`。

此次不修改伺服器 JAR、資料庫或原生 LiTo.bin；啟動探針已恢復修改前版本，未發佈診斷攔截。精簡回復檔為 `backups/before-caption-isolation-0.6.54.zip`。


## 0.6.56 第二視窗啟動時序

修正新增遊戲視窗偶發 `Additional native exited before resources loaded`。舊流程輪詢解密後位元組才寫入多開修補，原生單一視窗檢查可能先行執行而退出。現在僅針對新增視窗，在初始主執行緒恢復前設定一次性的啟動入口攔截；到達入口後清除硬體中斷點，等待多開及啟動修補全部寫入、驗證完成，才放行原生初始化。等待有期限，失敗時清理該新視窗所屬程序，不影響既有視窗與共用伺服器。

本次沿用 0.6.55 的相容 DLL，不改顯示架構。驗證結果見 `docs/startup-gate-0.6.56.json`；重現快速双開：`python -B tools/test_multi_window.py --release`；200% 連續新增與整場清理：加上 `--repeat-add --scale-200 --stop-all`。執行完整測試前需關閉本專案的正式場次，以免共用伺服器連接埠衝突。

回復備份：`backups/before-startup-gate-0.6.55.zip`。


## 0.6.57 雙玩家進場與無勝方返回

修正原生視窗未啟用時停止戰鬥畫面與排程更新的問題。只在戰鬥及結算階段允許原生主執行緒繼續更新，保留原生暫停判斷與各視窗的真實輸入焦點，不模擬前景啟用。

無勝方的原生 F0 分支略過 WIN/LOSE 結算頁，原先因此缺少按下「確定」才會送出的 C_D0，伺服器仍停在結算階段。現在只針對已確認略過結算頁的分支補送原生離房封包，完成回大廳與暫存清理；有勝敗結果時仍由原生「確定」處理。

測試入口：`native/build-battle-flow-test.cmd`、`native/build-battle-idle-test.cmd`、`tools/battle-flow/verify_pair.py`。兩個原生客戶端使用隔離連接埠與記憶體帳號，驗證準備／取消／開始、同時進場、無勝方返回大廳及再次進場；測試不寫入玩家 SQL。原生畫面擷取確認世界與倒數可見。正式 DLL 不含測試登入入口。完整手動雙玩家互動及使用者描述的十字架動畫仍待核對。

精簡回復檔：`backups/before-battle-flow-0.6.56.zip`；驗證摘要：`docs/battle-flow-0.6.57.json`。本次不變更正式伺服器 JAR、資料庫及磁碟 LiTo.bin。


## 0.6.58 房間目錄、人物標記與移動同步

人物建立封包補齊原生 PC 尾端 5 bytes，避免誤讀頭頂標記。原生 actor+0x13A 同時由 S_FA 血盟旗標控制，繪製 7550–7556 圖像並參與投降／停戰命令判斷；本次沒有屏蔽模式目標或修改圖像資源。

S12 玩家移動改送起點與方向，與原生解析一致；NPC／寵物原本已使用起點，維持原流程。補齊競技場雙方進出視野的人物移除／重建，進場先建立自身再建立其他玩家，避免第二場自身初始化清掉先收到的人物。

玩家開房使用獨立 ID，最後成員離開後清除；房間目錄明確更新名稱、模式、人數與上限，不再只重送原生會忽略的新增列。

隔離雙視窗測試：tools/battle-flow/verify_room_state.py，記錄 docs/room-state-native.json。已驗證原生八方向座標、離開／重返視野、標記清除、兩來源房間同步、結算回大廳及第二場進場。測試以記憶體帳號及伺服器命令驅動，移動測試另給自身校正以取代滑鼠預測，實際雙人連續操作仍需使用者驗收。

本次固定 JAR 由伺服器 tools/native-room-state/build.py 對已驗證基底進行限定修補；同時保留對應 Java 原始碼。未替換伺服器專案根目錄正在開發的 JAR。回復檔為 backups/before-room-state-0.6.57.zip。


## 0.6.59 正式房間名單與開戰隱藏

正式流程不再自動補入 ArenaBot：等待室、準備狀態、世界物件、AI 及單人假人勝方分支同步停用。GM 明確建立的 NPC／虛擬測試角色保持原有流程。沒有新增自動假人外控開關。

房間在等待入場時列於一般大廳；開戰立即通知同頻道等候玩家移除該列。戰鬥及結算期間重新請求列表也只會收到刪除，不會重新加入。A 先回大廳、B 仍在結算時，房間保持隱藏；房間資料與戰績結算仍保留到最後成員離開。獨立 /argo 觀戰目錄保留原規則。

驗證：伺服器 tools/native-room-admission/test.py 覆蓋三個連線、開戰即時移除、結算列表、最後成員清理、單人無隱藏假人；tools/battle-flow/verify_room_state.py --admission 覆蓋原生雙視窗先後返回與第二次進場，並保留八方向／視野往返驗證。結果：docs/room-admission-native-run.log，伺服器 tools/native-room-admission/evidence.json。回復：backups/before-room-admission-0.6.58.zip。


## 0.6.60 結算保存與隊伍斷線

先固定並保存全體勝負，再通知結算。SQL 失敗可從伺服器 runtime/battle-settlements 重試，重啟可補存已確定結果；場次收據與來源 character_lito.last_result_id 防止重複計算。結算後先返回或斷線不改判。

2 對 2 中一名成員斷線先記該人敗，其餘雙方仍有人則繼續；一方全離線後才由剩餘隊伍勝利。同一斷線判定窗口內雙方全離線視為無效，不新增勝敗。特殊玩法沿用自己的結算條件，沒有自動加入假人。

執行期只新增 LiTo 收據寫入，不保存天堂背包或角色屬性；資料表、回復限制與測試見伺服器 SQL-PERSISTENCE-GATES.zh-TW.md、tools/durable-settlement/evidence.json。回復輸入：backups/before-durable-settlement-0.6.59.zip；降版前不得刪除待補存檔。
