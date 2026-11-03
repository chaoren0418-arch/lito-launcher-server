# 材料技能 decoded request／Ctrl 與成對生命週期

2026-10-27。共同隔離候選：`c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6`。沒有新增部署、正式基準或資料庫寫入。

原材料入口測試直接呼叫 `NativeLightAction.dispatch`，未涵蓋外層 decoded-request 與選定物件 Ctrl 憑證。本次另建測試，從原 `ForcedCombat.dispatch` 進入 `ClientSession.handleBeforeForcedCombat`，使用現有八位元組 LFC1 擴充封包，不直接注入 ThreadLocal。每個請求結束皆檢查選定物件 Ctrl 權限已退出。

58 列材料技能共執行 464 次：普通敵方、自施、Ctrl 敵方、Ctrl 自施、同隊、Ctrl 同隊、Main 關閉及 STOPPED。友方使用原生 Honor Match、地圖 510、兩角色 force 1，並在 Ctrl 範圍外確認 `canAttack=false`；沒有更動競技程式規則。

- 無未預期例外或清理失敗；116 次 Main／STOPPED 均沒有扣款或送包。
- 147 次觀察到成本、99 次普通拒絕或無輸出、15 次無成本回應。另有 87 次 Ctrl 沒有可表達的物件目標，明確標為 `ctrl-not-object-layout`，不當成 Ctrl 通過。
- 技能 27 在普通敵方請求不扣款；Ctrl 敵方保留 29 MP／1 材料成本與原武器損傷路徑。這是目前原選定目標規則的觀察，不代表各技能與完整 Source 意圖都已核對完成。
- 技能 44 的 Ctrl 路徑曾遇到裸測試 Hub 缺少 `classicPartyMembership`，現已依原建構子補空 LinkedHashMap；未在生產程式新增 null bypass。

另以技能 27、173、174 的真實 Ctrl 封包，執行 42 次原成對鎖等待、312 項斷言。normal／DRAINING 保留原成本；Main、STOPPED、來源或目標切角、status／life 替換或缺失、Hub lifetime 缺失及 application 替換，均於原等待後拒絕，沒有扣款或送包。缺失的 status／life／lifetime 不會重建，舊角色 owner 不會輪替；原 Ctrl、Context、Batch、root 在實際呼叫執行緒恢復，所有測試執行緒終止。

證據：

- [464 次原 decoded-request 入口](../../tools/combat-continuation/integration-lifecycle/build/source-material-control-ingress/20261027T034814798625Z/manifest.json)
- [42 次原 Ctrl 成對鎖等待](../../tools/combat-continuation/integration-lifecycle/build/source-material-control-lifecycle/20261027T034821099722Z/manifest.json)
- [獨立測試 SHA 的原 gate 觀察](../../tools/combat-continuation/integration-lifecycle/build/source-material-target-gate/20261027T032908405087Z/manifest.json)

測試仍含明示的合成 NPC 資料與設定覆寫，尚未完整驗證每個技能的效果、Source 機率／免疫／目標型別、NPC／範圍副目標、屍體與裝備特殊輸入。Source 26 不按 Ctrl 的實機自施問題仍未驗收；這批不是實機游標、後端或中文 MySQL 真角色驗收。原本 12 組工作均維持未完成，後續集中實機驗收、不部署及資料安全限制不變。
