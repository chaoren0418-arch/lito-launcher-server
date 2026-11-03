# 0.6.66 碰撞後動作鎖定

## 現場證據

場次 development-f022810e10294764bc09a4f7f2686fdc：地圖 564，角色 3333 於來源座標 (32735,32801) 嘗試向 (32736,32800) 移動，伺服器回覆 rejected-closed-door，位置修正後送 S_BLINK。客戶端回 C_BLINK，伺服器只清除 movementLockPending，沒有回覆原生解鎖。倒數與心跳持續。讀取 PID 22764 的 byte_63DF4C=1，確認客戶端動作鎖仍保留。

原生 sub_4AFBA0 送 C_BLINK 並設 byte_63DF4C=1、動作鎖 bit 8。sub_4A6A80 的模式 17 經 loc_4A6DA6 清 byte_63DF4C，並呼叫 sub_4C3E10 清除 bit 8；其他 bit 保留。一般傳送模式 7 不會清除 bit 8。原生證據及現場旗標保存於 tools/resource-database/build/movement-lock。

## 修正

有效、且有待處理移動鎖的 C_BLINK，先送 S_PARALYSE 模式 17（25 11），成功後才清除 movementLockPending。重複／未請求／格式錯誤的確認不產生解鎖。所有既有 issueMovementLock 碰撞修正入口共用此握手，保持門、牆、地圖與伺服器權威座標判定。

僅修改 Protocol 與 ClientSession；不修改客戶端 DLL、SQL、戰鬥狀態或暈眩／麻痺邏輯。ASM 補丁在 movementUnlock 中嚴格限定唯一旗標寫入點；相同變更同步於原始碼。

## 驗證

MovementLockRegression 透過真實 ClientSession 與加密封包測試撞門、過期來源座標、超出地圖範圍三條移動拒絕路徑；每次回覆精確 25 11，後續合法移動成功。另測重複鎖合併、重複確認、錯誤格式確認及同時存在暈眩／麻痺時保留限制。測試無 SQL 寫入。

既有 ResourceRegression、RoomRulesRegression、FlowRegression、NativeDirectoryRegression、MovementRegression 全部通過。建置 tools/resource-database/build.py；整套回歸 tools/resource-database/test.py。尚待遊戲內手動碰撞驗收。

回復備份：tools/resource-database/build/deployment-before-066.zip（0.6.65 正式 JAR 與發布設定）。

發布：0.6.66 已部署，啟動器雜湊、正式 SQL READY、端點及程序清理通過（movement-lock-066-smoke）。
