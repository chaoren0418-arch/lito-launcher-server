# 材料技能 69 退出發布接線（隔離候選）

2026-10-27。候選 SHA-256：`c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6`。程式、共同建置／重建、同SHA盤點、ABI及武器稽核已接入共同候選進度。沒有部署或建立新正式基準。

原 `NativeAreaMaterialAction.massTeleport` 在正常及例外 finally 中，可能於原角色變更鎖已釋放後呼叫 `NativeRolePublication.completion`，以「role publication capture requires actual mutation lock」遮蔽原施法例外。修正保留完整原方法、兩條 finally、原 massUnlock／portal／例外清理，僅接入首次角色證據及受控發布。原解除鎖定 lambda 保留，另有完整原方法副本攜帶明確 proof 到原最終加密監視器。

發布憑證在原角色鎖內建立，鎖後重查首次角色／status／actor life／Hub／lifetime／application。Main、STOPPED、切角或生命替換拒絕；DRAINING 保留。持有第三角色鎖時只嘗試取得來源鎖；來源忙碌便退出。需等待外層解鎖的補送沿用原 Batch／root drain，不新增 registry、ThreadLocal、queue 或 worker。缺失的來源 status、actor token 或 lifetime 不會在等待後重建。

完成的離線檢查：

- 193 項新增檢查：13 次原來源鎖等待、13 次原最終接收者監視器等待、5 種第三角色／預持鎖／退役情境。
- 獨立逆向差分檢查 116 個完整原方法；只變更兩個原類別，新增兩個 runtime 類別，沒有新增原類別欄位。七個原方法接點、兩個完整 lambda 副本接點，原材料／portal／成本／例外流程保留。
- 124 個既有 runtime plan 在這個 SHA 重跑通過；未把歷史契約案數重新加總為新證據。既有 no-init loader 加四個新增明確 owner 通過。
- 58 列材料技能、232 次設定完整的原入口呼叫無未預期例外或清理失敗。116 次 Main／STOPPED 均沒有扣款或送包；其餘為 60 次觀察到成本、46 次拒絕或無輸出、10 次無成本回應。這不是全部技能效果或目標語意通過。
- 技能 69 正常 peer／self 均保留 54 MP／1 材料成本及 138 bytes 記憶體加密輸出。刻意使原施法 body 失敗時，原 privateShops 例外保留，補送一次 4-byte 解除鎖定封包；舊候選則以發布鎖例外遮蔽原例外。
- 使用凍結輸入重建得到相同 SHA。20 個原唯讀 SQL reader 類別逐位元保留，正式保護檔與共同進度索引保持原雜湊。

測試補正包含正確 map／x／y 封包順序、完整技能機率欄位、角色各自的嚴格記憶體 repository，以及原建構子應有的 inventory／story map／商店／交易集合。NPC CON／WIS、召喚資料與 DragonKnightAwake 設定仍含明示的合成值；沒有讀取真實 MySQL，不能當作真實 Source 資料驗收。

證據：

- [候選與全部測試](../../tools/combat-continuation/integration-lifecycle/build/source-mass-teleport-exit-probes/20261027T030258864316Z/manifest.json)
- [凍結輸入同位元重建](../../tools/combat-continuation/integration-lifecycle/build/source-mass-teleport-exit-probes/20261027T030522040304Z/manifest.json)
- [差分、no-init 與資料保護核對](../../tools/combat-continuation/integration-lifecycle/build/source-mass-teleport-exit-validation/20261027T030939203683Z/manifest.json)
- [後續接入項目](../../tools/combat-continuation/integration-lifecycle/build/source-mass-teleport-exit-pending-integration-current.json)

共同 builder／checkpoint／inventory、ABI 與武器稽核已接入此確切 SHA，六個既有 alias 差異仍保留；接續繼續材料技能的正常／Ctrl／友方／NPC／範圍目標效果與其他 Source 缺口。全部 12 組目標仍未關閉；後端、中文 MySQL 真裝備角色唯讀重載及實機游標驗收維持集中後驗，禁止部署及 Source／玩家 SQL／schema／WAL 寫入。

共同進度證據：[本次checkpoint](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-progress/20261027T032152772802Z/manifest.json)；普通同SHA檢查16103、forced-tie另列7，direct-lock census為58方法。
