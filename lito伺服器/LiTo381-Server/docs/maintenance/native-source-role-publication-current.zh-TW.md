# native 角色完成輸出與選角退役邊界

2026-10-18 隔離候選：原 vital completion 保留原 callback、數值與 receipt 釋放，在送出每個原生 frame 前檢查既有 canonical role owner。選角退役與原 assignment 共用實際 PC action lock，避免等待選角期間建立的 owner 跨過同名重選。**完整 Source API、全部職業技能與整體目標仍未完成，未部署。**

候選 SHA-256：`3518be18bdb56dfe8333e2a2c637f024f864505ed2d978245b87c0d0b4f857c4`。父版為 native 外層 action scope；canonical、四個固定基準及玩家 SQL／WAL 未變；目前來源的明確 migration-only 差異如下記錄。

原 frameless MP callback 可在新角色初始化後送出舊角色 snapshot。候選於原 enqueueLocked 的實際 mutation lock 內，擷取原 PC、應用與既有 NativeDeItemApplication.Owner；只外包原 action，原 callback 即使已退役仍執行，以完成 receipt／HP handoff。wire guard 在原加密前檢查 owner active、角色 tuple、PC owner field 與應用 identity，保留既有 accepted-state、frame 與 item-publication ticket 檢查。ThreadLocal 以 finally 恢復，沒有新增 PC／HP／MP 權威、worker 或 timer。

未安裝 item application 時，角色 lease 使用既有 PC owner field 與原 Owner(null,pc)，沒有另一套 owner。之後真實 application 安裝時，原 owner 方法額外核對 application identity，退役 fallback 並建立該應用自己的 owner／inventory generation。一般 scene 改變保留角色 owner；同名重選、disconnect 或應用退役使舊角色 lease 失效。

只改五個既有方法／五個 entry（含 GameHub.close 的前兩個呼叫次序），新增一個 canonical owner capture bridge 與兩個 role helper entry。原 mutation、receipt lambda、PC 的 2263 個方法及其餘父版方法／宣告／資源經 inverse 核對保留。兩次凍結輸入建置得到同一 SHA，與測試 runtime 一致。

驗證：

- 同編譯實際 native 對照各 104 案，涵蓋兩個 memory-only 旗標、BATTLE／WORLD、有／無 item application、一般輸出與主例外。父版 120 個 frame、候選 64 個 frame；56 個已退役角色封包被抑制，正常 scene 與獨立 peer 輸出保留，原數值與全部 receipt 完成。
- 選角實際鎖競跑各 16 案、185 checks。父版在 selector 等待 action lock 時先退役，期間新建 owner 可跨過同名 assignment；候選把退役與 assignment 放在同一 action lock，舊 owner 到真正切換時才退役，下一次 admission 建立新 generation。
- native STOPPED 對照各 16 案。初版錯把 processes() 當成已受理輸出的有效條件，只有 8 個 frame；修正版保留停止前已排隊與已保留 receipt 的工作，共 24 個 frame，新工作仍由原 reserve 拒絕。初版完整 20 組回歸雖通過，仍被此專門控制否證，保留該 runtime 與證據；修正版另跑自己的完整回歸。
- 新增 20 個真實 native 邊界案例、24 個 frame：fallback owner 到真實 application 的 adoption；第一個 HP frame 已通過檢查並停在 socket 時同名重選，第二個 MP frame 被逐 frame 檢查抑制；回呼輸出故意拋出 AssertionError 後，原 recovery 數值不回滾或重播，receipt 與 role context 清理，下一個獨立 peer MP 正常送出。沒有 production overlay，frame 獨立解密核對。
- 修正版原十二組回歸與八組 authority／item／Main 重播全部通過。另依序通過 scope pair 16、worker gate 68、精確 draining-flag 故障對照 8、ready-prefix 8、accepted-state 50、Proc healing 514、外層效果 12，以及 HP／MP FIFO、handoff、Main normal／interrupt、Joy context 與 context-acquisition-failure。原斷言與期限不變，原 damage worker 上限為 2／256／64／64。

前一個修正版雖通過完整二十組與角色邊界控制，仍在原 HP Main normal 測試被否證：數值與尾端均完成，最後 HP snapshot 缺失。診斷顯示實際 GameHub.close 先退役 item application，再等待 native accepted lanes，第二個已受理 HP callback 捕捉不到有效 owner。最終版僅交換原 close 前兩個呼叫，先等待原 NativePlayerLifetime.closeHub 完成，之後才退役 item owner；其餘 GameHub 方法、宣告與整個 close body 完整 inverse 保留。新增同編譯實際 Hub 關閉對照各四案、兩旗標／兩模式：前版在 native drain 等待期間 owner 已退役，僅四個 MP frame；修正版保留 owner 至所有原工作完成，二十個原 MP／HP／WORLD hit-ratio／criminal-status frame 順序完整。原 HP Main normal／interrupt 也各恢復三個 frame，資源只關閉一次且無關閉後 repository 呼叫。前版完整回歸、失敗生命周期與診斷保留，最終版重新完成自己的完整回歸與生命周期重播。

新增 shutdown fixture 初輪未列入 WORLD 的原 hit-ratio frame，精確封包斷言拒絕該觀察結果；補上原 Protocol.hitRatio 與 criminalStatus 的預期 frame 後再跑同編譯對照。保留四次工具失敗，沒有更改 production 數值、原有回歸斷言或期限。

最後 audit 發現目前 Source L1PcInstance.java 已從父階段的 66c8abce… 變為 9b425830…，因此原先封存 gate 正確拒絕通過。唯一定義明確的差異是 MigrationDeparture 欄位／accessor／migration logout、logoutInternal 的 migration no-save overload、setNetConnection 防護與五個存檔路徑 guard。對這些固定 SHA 的明確區塊做完整 source inverse 後，其餘整份程式碼與歷史快照完全一致；PlayerActionLock.java／class、L1Character.java／class 與已編譯 L1PcInstance.class 五項輸入原 SHA 均未變。保留新舊來源、diff 與可重驗 inverse，新收據綁定目前來源 SHA；父階段歷史收據不改寫。來源 .java 比 compiled PC class 新，MigrationDeparture 功能與完整來源生命週期尚未移植／驗證。原完整回歸與本階段角色契約不因此改寫為 migration 資格。

這是 callback 所屬實際 PC 的角色邊界，不是每位 viewer 的通用 admission，也不會因 producer A 退役而抑制獨立 B callback。已通過 wire guard 並開始 socket write 的 frame 不回收；後續 frame 重新檢查。一般 scene 的舊 combat Binding 失效與角色 owner 存活是不同契約。本階段沒有全面啟用 Source API 或解決任意倒序巢狀 physical lock。

仍須分類 243 個 literal PC vital write 的實際可達性與既有 guard，完成 frame-null sleep／reveal、施法資源／reuse／動作封包順序、全部 ingress／item／currency／transfer／rollback、live Item／status／ability／executor／NPC、全部職業技能／Ctrl／範圍與 canonical SQL／啟動器／UI／關服。先前 actor-idle 與 normal Main 握手逾時根因仍開放。保留 competition／team／TA 與記憶體角色／道具／技能／XP，僅 character_lito／settlement 可持久化。

依精確路徑、連結與 SHA 核對，封存逐內容驗證後清理 5348 個重複生成檔，保留 46 個凍結輸入與必要對照。收據：`tools/resource-database/build/native-source-role-publication-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-source-role-publication.py`。
