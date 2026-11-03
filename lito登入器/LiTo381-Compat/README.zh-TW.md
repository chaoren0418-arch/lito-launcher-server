# LiTo 3.81C DirectDraw 視窗相容層

## 原生按鈕、角色回程與對話輸入修正（Launcher 0.5.55／Server 0.7.863 配對版）

- `WM_ACTIVATEAPP(FALSE)`、`WM_ACTIVATE(WA_INACTIVE)` 與 `WM_KILLFOCUS` 仍交給 Windows 完成真正的視窗失焦，但不再轉送給 LiTo 2003 主迴圈設為停止；DirectDraw proxy 的背景 present 因此可持續更新。
- 重新追蹤 `sub_47CF00 -> sub_49E570 -> sub_49E110 -> sub_49CD70` 後確認，599／620 是控制的左上角，不是中心。設定命中固定為 `x=[599,620), y=[460,480)`，離開為 `x=[620,641), y=[460,480)`（640×480 畫布可見到 x=639），兩者不重疊。
- 原生 `sub_4438F0` 只有 20 個道具說明行尾槽；入口 `0x00443750` 只在前五 bytes 精確吻合時交易式改接安全排版，最多保留 20 行並保證 512-byte decode buffer 結尾。Server 也把狀態列預留在 16 行內，避免後續固定 itemdesc／材質超出容量。
- 現行 `ddraw.dll` 為 190,976 bytes／SHA-256 `FAB11934D6377BB253525330A8B1E490906CFACD641394F13906167D9A593D7B`。DirectDraw create、cooperative level、display mode、surface、caption、視窗移動、背景像素呈現與正常關閉 smoke 全部通過。

## Tournament 大廳按鈕與原生設定／退出流程（Launcher 0.5.52 配對版）

- 大廳 phase 1 與已進入戰鬥室的 phase 2 都在原生「商店」精確按鈕區域補送 `C_DA`；不再以可能殘留的 ordinary-world scene 指標判斷是否可用。伺服器 `S_DF` 仍負責填入資料庫外控清單，交易維持原生 `C_DB`。
- 右下「設定」呼叫 LiTo.bin 在 `sub_47CF00` 註冊的原生 `sub_47B8F0`，開啟 `LToptiong/LToptiong2`，不建立替代設定介面；觸發區直接使用原生左上角 `(599,460)` 與 21×20 大小。
- 右下「離開」若遇到 Tournament bootstrap 漏建物件，會以 LiTo 自己的 allocator 與 constructor 補建缺少的原生對話框，再交回 `sub_4786F0`。最新原版 phase 1 實機與 wire capture 證明 mode 2 對話框只有「離開／取消」兩列：第一列送 `C_QUIT`、播放結尾並關閉整個遊戲，第二列留在大廳；原生沒有大廳直接回頻道或角色選單的第三列。相容層不再把測試探針點到對話框外誤判為返回流程。
- Tournament phase 1～4 會隱藏舊引擎額外顯示的 `LUnicodeEdit` 子視窗表面，鍵盤與原生 DirectDraw 對話輸入流程仍保留；用來消除大廳對話列上重複且錯位的不透明輸入框。
- 原版 `LiTo.bin` 不會被修改；所有橋接都要求指定映像指紋、Tournament UI 旗標、合法 phase 與精確 640×480 虛擬座標。
- 現行 `ddraw.dll` 為 188,416 bytes／SHA-256 `CEB3B1483FEB6AA95B11869F0254CA87B456720AB50FD22FD3426D1ACD91467C`；DirectDraw、座標映射、非前景背景呈現、視窗移動後定位及正常關閉 smoke 全部通過。

## 32-bit 數值突破 Schema 4（現行 Launcher 0.5.52／Server 0.7.860 配對版）

- Schema 4 將 `S_STATUS` 與 `S_AC` 的火／水／風／地抗性擴為 little-endian int32，完整值保存於 wire 順序 `fire, water, wind, earth` 的 32-bit sidecar；原生 byte shadow 只保留 `-128..127` 飽和值。
- 已復原 `S_AC` 後四欄的真實語意是四屬性抗性，不是一般 modifier。`S_STATUS` 四個 destination、`S_AC` decoder 與八個直接 consumer 共 14 sites 都先精確驗證原位元再交易式提交；任一處不符就不啟用整組 patch。
- Schema 0～3 保持舊 wire 相容；Schema 4 必須與 Launcher 0.5.52、Server 0.7.860 同時啟用，避免寬封包被窄 decoder 誤讀而異常退出。
- 現行 `ddraw.dll` 為 190,976 bytes／SHA-256 `FAB11934D6377BB253525330A8B1E490906CFACD641394F13906167D9A593D7B`；2026-09-08 重新執行的 DirectDraw create、cooperative level、display mode、surface、caption、position、background present 與關閉 smoke 全數通過。完整對照見 [Schema 4 契約](<D:/lineage開發/LiTo381-開發/analysis/NATIVE-NUMERIC-SCHEMA4-CONTRACT.zh-TW.md>) 與 [14-site 稽核](<D:/lineage開發/LiTo381-開發/analysis/NATIVE-NUMERIC-SCHEMA4-PATCH-AUDIT.json>)。

## 32-bit 數值突破 Schema 3（現行 0.5.29 配對版）

- `[NumericBreakthrough] Enabled=1` 僅對已驗證的 LiTo 3.81C 映像啟用；磁碟上的 `LiTo.bin` 不會被修改。
- `Schema=1` 以交易方式擴充 HP／MP／AC；`Schema=2` 再擴充等級與六素質；`Schema=3` 把 `S_MAGIC_STATUS` 的 SP／MR 修正值擴為 int32，完整值都存入 proxy 的 32-bit sidecar。
- 所有原始機器碼與 consumer 數量都須精確吻合才會提交；任何驗證或寫回失敗均回滾，避免半套 patch 造成遊戲異常退出。
- 角色目錄 `S_CB` 保留原生安全飽和值；完整等級／六素質由登入世界後的 Schema 2 `S_EXP`、`S_ABILITY_SCORES` 與 `S_STATUS` dword 欄位更新，記憶體順序固定為 `STR, INT, WIS, DEX, CON, CHA`。
- Schema 3 由 `0x004AF320` 解碼 9-byte `S_MAGIC_STATUS`，SP 的一個、MR 的四個直接 consumer 與 MR `+20` mutation 全部改讀寫 dword sidecar；原生 byte 全域僅保留 `-128..127` 飽和影子。七個位點的任何一處不符都會停用整組突破。
- exact unpacked image 的 70 個直接關聯點與目前共 109 個交易式 patch site 由 `tools/native_numeric_schema2_patch_audit.py` 固定稽核；原版 `LiTo.bin`／map 564 已實收 HP 100,000、MP 80,000、AC -500、level 345、六素質 301–306，沒有異常退出。角色目錄仍使用原生安全飽和值，完整值只在世界內 Schema 2 封包啟用後發布。
- 現行 `ddraw.dll` 為 180,224 bytes／SHA-256 `EAAE4ACD35B15DA98D80B93F9E3644D94F2A9331E065D35F8E5B6962DEDC40FB`；Launcher 0.5.29 預檢與啟動計畫鎖定同一指紋。完整對照見 [Schema 3 契約](<D:/lineage開發/LiTo381-開發/analysis/NATIVE-NUMERIC-SCHEMA3-CONTRACT.zh-TW.md>) 與 [380 Rust 安全修改審核](<D:/lineage開發/LiTo381-開發/analysis/LITO-380-RUST-LAUNCHER-REFERENCE-AUDIT.zh-TW.md>)。

專案分工、整體流程與接手方式請先讀 `D:\lineage開發\LiTo381-總開發報告.md`。

這是一個只針對 LiTo 3.81C 新案建立的 x86 `ddraw.dll` proxy，沒有 TW13081901 的固定地址或 LHZ 功能。

功能：

- 將 exclusive/fullscreen cooperative level 虛擬成 `DDSCL_NORMAL`
- 攔截顯示模式切換，不改變現代桌面解析度或色深
- 將 640×480×16 primary surface 改成 system-memory offscreen surface
- 以 GDI `StretchBlt` 顯示到一般視窗，保留 4:3 比例
- 視窗縮放時映射滑鼠座標回 640×480
- 保留原生標題列的 `HTCAPTION`，可拖曳且顯示模式重設後不會回彈到螢幕中央
- 視窗移動時會將最後完整幀重新定位到新的主畫布原點，避免角色選單的 DirectDraw 背景與 GDI 角色／資料圖層分離而產生偏移、局部舊畫面或黑塊
- 標題列 `X` 與 `Alt+F4` 在所有階段交回 LiTo 原生 `WM_CLOSE(lParam=1)` 完成 socket、視窗與程序清理；房間內原生返回按鈕及 `Alt+X` 仍由遊戲自己的 `sub_46B1A0` 送出 `C_D0` 返回大廳。相容層不顯示自製訊息框、不代送退出封包，也不強制 `ExitProcess`
- Tournament 右下角只在已驗證映像、UI／phase 與互不重疊的原生左上角區域內橋接「設定／離開」callback；其餘輸入仍交回 LiTo 原生 UI controller
- 戰鬥結果由 phase 5 進入原生勝敗摘要 phase 2 時，只在使用者按下摘要「確定」後，透過 LiTo 已建立的原生加密連線送出 opcode-only `C_D0`，讓伺服器以單筆具名 `S_D0` 正確返回大廳；不以固定秒數強迫離房
- RGB565 軟體工作表面可正確顯示 Loading、登入與 Tournament 大廳，避免黑屏或綠色半屏
- 744×552 與 640×480 工作表面只供舊引擎內部合成，不直接發布到視窗，避免與完成的 primary 畫面交替
- 同一幀內大量 Lock/Blt/BltFast 以 timer callback 合併並限制為約 60 FPS；即使 LiTo 在換畫面時替換 WindowProc，仍能避免黑幀、半張介面、閃爍或畫面凍結
- 不繪製替代背景、取消按鈕或好友／血盟提示；遊戲內容完全來自原始 LiTo renderer，相容層只負責呈現與輸入座標轉換
- 唯讀驗證到選定 `LiTo.bin` 的 skill118 case117 已存在，但名稱回呼誤把 column 固定為 skill116 的 3；相容層只在精確原始機器碼吻合時，於程序記憶體內把 case115／117 分別導向 column3／5，讓「呼喚盟友」與「援護盟友」各自送出正確封包，磁碟上的 `LiTo.bin` 保持不變
- 選定 `LiTo.bin` 的 skill131 case130 被導向立即返回的停用分支；相容層只在 byte-table 原值精確為 3 時，於程序記憶體把該一位元組改導向既有 selector-only producer（entry 1），恢復 LiTo 原生 `14 10 02`，驗證失敗會回滾且不修改磁碟檔

`Start-LiTo381.ps1` 只在 Windowed 模式暫時把 proxy 與 ini 放入客戶端目錄，遊戲結束後會復原原檔。建置使用 `Build-Compat.cmd`。
