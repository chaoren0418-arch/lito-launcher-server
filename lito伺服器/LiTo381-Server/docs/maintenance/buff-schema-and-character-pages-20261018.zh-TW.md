# BUFF 來源相容及八角色選單驗證

2026-10-18，已將使用者驗收的 BUFF 讀取及角色分頁發布為 0.6.69。以原正式 0.6.68 JAR 為基底，只替換兩個 repository 方法及新增讀取類別；沒有合併其他戰鬥技能施工候選。

## 已完成

- `SourceBuffRead` 偵測新版四欄 `character_buff` 單表及舊版完成快照。舊版只載入 `completed_version`，不把未完成版本混入；新版不要求已移除的快照表。所有來源讀取均為 SELECT。
- 實際 repository 方法回歸：原版 9 案、候選 16 案。候選只改 healthCheck、loadBuffSnapshot 兩個方法，另 275 個方法保持相同；來源寫入方法未改。
- 公牛、天使目前均已改為單表。唯讀實庫比對各 7 個角色、9 筆 BUFF 通過，沒有修改資料庫。舊版／新舊混用由 JDBC 契約回歸驗證。
- 使用者指定的 `2-登入器/打包建置/編碼器/TW13081901.txt` 與原發布 pin 完全相符，已恢復到共用客戶端。SHA-256：`7ebe2b464c8b1ea9c58ac985dd8ff4c823784d8231894262eddcd7691ca94008`。
- 候選以正式 SQL 設定啟動成功；啟動器所擁有的程序及端點關閉清理通過。這不等同 JVM 自然關閉／所有遊戲介面驗收通過。

## 第一場原生介面失敗及原因

場次 `buff-schema-native-20261018-a`：帳號驗證成功，伺服器送出 8 筆角色，客戶端也收完 8 筆；使用者回報角色選單空白、無反應。不是 BUFF 缺表／帳號未載入。

原生角色區起點 `0x62FAA0`、每筆 `0xB4` bytes，實際四格；第 5 筆起點 `0x62FD70` 正是職業動畫表。第 5～8 筆覆蓋該表。原 `Protocol.java` 註解把原生容量誤寫為八筆，已更正為「原生四格、相容層分頁八筆」。沒有透過刪除角色或修改來源 SQL 解決。

此場原生 probe 記錄 exit code 1，啟動器外層 exit code 0 表示監督及清理完成，不能當遊戲介面成功。`terminal.json` 的 cleanup_verified=true、cleanup_errors=[]。畫面擷取工具兩次逾時；沒有把畫面擷取列為通過。

## 候選分頁修正

登入器 `native/character_pages.h`、`.inc` 在固定容量八筆的程序內緩衝區保留目錄，收齊後只讓原生接收當頁最多四筆。每次新目錄重置緩衝；超出容量／格式錯誤拒絕繼續。選角進場的 S_CB 仍走原有角色切換分支。

原生每頁四格不變，以 PgUp／PgDn 或滑鼠滾輪翻頁，標題顯示頁次；離開角色頁停止處理翻頁並恢復標題。伺服器仍保留完整八角色清單，以選定角色名稱進場。

通過的本機測試：0～8 角色、兩頁與反向翻頁、重新載入、四格後的動畫表保護區不變、進場封包分流、場景限制、最大字串長度、截短／錯誤資料；既有大廳→頻道→角色、角色→來源、來源→帳號流程測試通過。候選 DLL 建置通過；既有 DEF 匯出 PRIVATE 警告仍存在。

## 目前驗收門檻

場次 `buff-schema-native-20261018-b` 選角崩潰，原生退出碼 `0x0019FFCC`；啟動器及程序清理完成不代表遊戲成功。原分頁候選 `7d4b7bd0…` 未發布。

已用候選 DLL 的實際機器碼、原生 `sub_4A7550`／`sub_4A2A50` 及計數觀測 trampoline 重現原因：原生變參解析器最後一個 checksum 寫入目的地取自 receiver 保存的呼叫端 ESI。原生 dispatcher 使 ESI=封包位址，C++ 分頁迴圈的 ESI 則是索引；直接 fastcall 不足以滿足該隱含 ABI。第一次測試只用了替身 receiver，未覆蓋這個契約。

`InvokeNativeCharacterRow` 現在明確以封包位址設定 ESI、呼叫後還原；原生 checksum 會修改封包首 byte，因此顯示使用當筆副本，保留分頁快取。另將血盟名稱上限對齊原生 17-byte 容器（16 bytes 文字＋NUL）。

`emulate_native_pages.py`（Unicorn 2.1.4）通過：兩個負對照分別重現 ESI 寫入錯誤及八筆越界；候選 0～8 筆、此場真實八角色封包、兩頁三次來回、ESP/EBX/EBP/ESI/EDI 保留、動畫表逐 byte 相同、原生角色進場分支通過。計時器、UI 繪圖、衍生說明及 OS 呼叫以替身隔離，不能取代實際畫面驗收。收據 `build/native-emulation.json` 保存 DLL／原生輸入 SHA。

場次 `buff-schema-native-20261018-d`：使用者確認角色正常顯示、翻頁、進入大廳及返回角色選單均正常，並已關閉視窗。相容層記錄完整八筆、第一頁／第二頁各四筆；伺服器記錄第二頁角色進場與最後 `C_QUIT graceful disconnect`。原生／伺服器監督程序清理通過，無所屬端點殘留。

原生退出值 `0x0019FFCC` 在失敗 b 與使用者正常關閉的 d 都出現，因此該值不能單獨判定崩潰。介面通過以使用者實測及流程紀錄為準；`game_window_verified=false` 表示沒有自動畫面驗收，不代表使用者未測。使用者關閉前未取得登入後動畫表快照，動畫表保護證據來自實際機器碼重播，並非本場關閉後的記憶體讀取。

BUFF 新／舊來源讀取已通過實庫與契約測試；使用者這次明確回報的範圍是選角與往返流程，沒有另外逐項回報 BUFF 圖示，故不宣稱全部 BUFF 圖示或技能效果已驗收。

正式 JAR、相容 DLL、啟動器 pins 與版本已更新至 0.6.69；共用客戶端載入同一份已驗收 DLL。來源資料庫沒有變更。發布收據 `tools/buff-schema/build/publication.json` 記錄精確雜湊。

正式發布後 `tools/verify_current.py --native` 通過全部固定輸入、JAR／catalog 大小寫與拒絕錯誤雜湊測試，正式 SQL 及原生資源啟動、七職業／技能接線、退出碼 0 與所屬程序清理均通過。場次 `cleanup-smoke-1792330077323434800`，結果見登入器 `docs/startup-smoke.json`。這是啟動與關閉冒煙測試，不代替前述使用者登入驗收，也不是 JVM 自然關閉測試。桌面已更新為「LiTo 0.6.69 正式啟動器」。

## 回復

登入器 `backups/pre-0.6.69-buff-pages-20261018.zip` 僅保留本輪替換前的 18 個發布／設定／來源檔，約 6.5 MB；內附原路徑與逐檔 SHA-256。要回復時先關閉 LiTo，驗證 manifest 後按原路徑還原整組檔案，再以 `tools/verify_current.py` 驗證。下次啟動會把已回復的 `app/compat/ddraw.dll` 複製到共用客戶端；桌面名稱亦須對應 0.6.68。現行 0.6.66 聯合基準、來源 SQL、結算 WAL 及未完成候選均保留。

## 檔案與證據

- 窄範圍建置／測試：`tools/buff-schema/`。固定原版與三個原生回復輸入放 `inputs/`，生成物及測試收據放 `build/`。
- JAR 候選 SHA-256：`75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371`。
- DLL 候選 SHA-256：`0135e6e76594eb98695b85e9b451669adaedccfa6a13fafc49bf934d9c761de4`。
- `build/build.json`、`live-read.log`、`preservation.log`、`native-ui-failure.json`、`native-tests.json`。
- `verify_native_pages.py` 只以讀取權限開啟本場程序，核對 PID 出生時間、路徑與 BIN 雜湊，再比對動畫表；不寫入遊戲記憶體。
- SQL 啟動／清理：`build/staging/runs/buff-schema-startup-20261018-a/`；原生伺服器兩場 `buff-schema-native-20261018-a`、`-b` 同目錄。
- 原生程序、關閉及接收紀錄：登入器 `app/runs/buff-schema-native-20261018-a/`、`-b/`。
- c 場已關閉；最終驗收為同位置 `buff-schema-native-20261018-d/`，兩份 terminal/result 均確認關閉清理。
- 一度嘗試在獨立 x86 測試程式映射原生固定地址，因位址已占用未執行解析測試，已改用隔離模擬器。清理其 `build/native/native-image.bin`、`native-test.exe`、`native-test.obj` 及 `test-native-parser.cmd` 的合併命令遭自動審查拒絕，未執行刪除，暫存保留；不把該嘗試列為通過。

先前缺表／缺 TXT 的失敗歷史保留於 [原驗證紀錄](release-verification-20261018.zh-TW.md)，以本文件的後續狀態為準。
