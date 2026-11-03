# 0.6.65 裝備穿脫回復修正

狀態：0.6.65 已部署，啟動器雜湊與正式 SQL READY／端點／程序清理通過（equipment-toggle-065-smoke）；遊戲內快捷鍵與滑鼠操作的視覺驗收待使用者測試。

## 已證實原因

0.6.63 場次 development-38362931220e4c018907d89df8f9d62a 的武器卸裝先送出 staged-equipped=false，約 51 毫秒後送 rollback-unequip-evaluation=true。角色重新選取時清空 sourceEquipmentObjects 等來源映射，但 loadPersistentState 因 inventoryCharacter 未變而略過載入。記憶體裝備覆寫因此為空，SQL 評估又讀到來源庫原本的穿戴旗標。

獨立測試使用正式唯讀來源、真實 ClientSession 及加密封包，舊版重現映射 8→0、一次卸裝仍為穿戴；修正版保持 8 筆並成功卸裝。

新 0.6.64 場次 development-06cf14ab148a441b9473b9d115a76835 部分穿脫只有一次請求、一次狀態更新，沒有回復；該部分的畫面感受仍需新版實測，不能全部歸因於同一條失敗路徑。

## 變更

- 同一來源、同一角色的背包保留時，來源裝備／消耗／箭矢／魔法娃娃映射及已計算裝備素質一併保留。
- 切換角色或來源時，照原流程清空並重新載入，沒有額外的靜態玩家快取。
- 武器與防具完成來源評估及記憶體提交後，才傳送穿戴描述與武器外型；拒絕的候選狀態不送到客戶端。
- 客戶端 DLL、資料表與來源人物資料均未修改。

## 驗證與建置

`tools/resource-database/build.py` 仍使用固定 0.6.61 底版。EquipmentPatch 嚴格驗證 8 個保留點、2 個延後描述點、1 個延後外型點與 2 個提交點，避免默默套用錯誤版本；ClientSession.java 保留相同語義供後續整體建置。

`run_equipment_test.py`：實際 C_USE_ITEM 武器／防具連續穿脫，每次只有一份最終描述；同角重選不恢復舊素質；換角及再選回不混用物品；注入 SQL 評估失敗時不提交背包、不送出候選穿戴；來源 inventory 前後完全一致。測試直接建立唯讀來源，不啟動正式結算日誌或補登工作。

`test.py`：資源表消費、測試房假人人數、跨來源房間列表及斷線結算測試通過。

候選版相對 0.6.64 僅變更 ClientSession.class，增加 EquipmentTransitions.class。證據與雜湊見 `tools/resource-database/equipment-evidence.json`。

回復備份：tools/resource-database/build/deployment-before-065.zip（0.6.64 正式 JAR 與發布設定）；0.6.60 基準保留。
