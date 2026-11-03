# Source 與材料技能完成工作清單

更新：2026-10-27。狀態：**進行中，完整補齊尚未完成；先更新程式，後集中驗收，不部署。**

新工作目標：以固定 Source 與正式0.6.71功能為依據，完整補齊競技場材料技能、Source外控、傷害生產端與跨角色生命週期。保留正常／Ctrl、競技關係、目標型別與免疫、原扣料／機率及有界記憶體生命週期；禁止 Source／玩家 SQL／schema／WAL 寫入及正式檔覆寫。LITO產品只有BATTLE；既有WORLD後備保留。

2026-10-25使用者再次表示已刪除舊目標並要求重設；本次create_goal仍被系統以unfinished goal拒絕，get_goal仍為active。保留進度繼續完整範圍，未誤標完成，也未宣稱重設成功。[本次目標工具收據](D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\tools\combat-continuation\integration-lifecycle\build\source-goal-reset-attempts\20261025T035609172256Z\receipt.json)。

## 盤點依據

- 現行候選：`50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729`；歷史測試案數不移算到此SHA。
- 現行JAR：36,551方法、125,466本地呼叫點、55個特定HP消費端直接呼叫點；這些是定位數量，不能當作缺口或通過案數。
- 歷史靜態資料：807技能列、180原生模型、185個Source符號列、234解析路徑；實際材料技能為58列，舊「34個」不是完整分母。新候選尚無完整語意資格化。
- 八個相關Source設定類別：252個明確鍵引用，52列有完整字串定位。動態組鍵／別名需另核對；不得把其餘200列直接判為未實作。SQL外控與其他Source路徑不包含在此鍵數內。
- 逐技能：[807列清單](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-inventory/20261027T061317708687Z/skills.tsv)；[外控清單](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-inventory/20261027T061317708687Z/controls.tsv)；[HP生產端](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-inventory/20261027T061317708687Z/hp-producers.tsv)；[盤點收據](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-completion-inventory-current.json)。

下一批生產端已凍結133個Source方法：[Source參考收據](D:/lineage開發/3-lito開發/lito伺服器/LiTo381-Server/tools/combat-continuation/integration-lifecycle/build/source-next-producer-contracts-current.json)。立方體傷害來源是實際效果NPC。娃娃SKILL保留接受後魔法數字與PC true／false；UnifiedProcExecutor則把加傷放在技能之後，獨立扣HP、扣HP前魔法數字及PC false／false。減傷沿用防禦計算；不能把加傷合入物理傷害。PhysicalHitCommitPlan在物理接受後、物理數字前執行proc副作用。

## 本次已更新的程式

29／152／153受控NPC已將首次來源／主人／NPC／接收者不可變值接入既有完整PC／Motion sender副本，在原cipher監視器重驗status容器、observer、Hub lifetime及application。修正舊29／152觀察者封包與153自接收的退役漏洞；已提交自身MP／HP／方向更新不被後續目標退役誤丟，非自身呈現仍保留來源／NPC位置與placement檢查。normal／DRAINING／僅主人移動保留，Main／STOPPED及替換身分拒絕；不改Source成本、免疫、RNG或競技／Ctrl。

完整原producer lambda副本保留所有呼叫／分支／handler；scoped BATTLE不再將Aim強參照放入finish callback，原finally關閉Aim、清空首次接收者表並釋放全部主人receipt後才等待IO。queued record不含Aim／Context／Frame／receipt／Member／Scope／Task／collection；無新global root／TLS／queue／worker。34完整原方法inverse、3既有owner、7接點＋1producer handle、1完整副本、1暫存Aim欄位、3新runtime class；原完整PC／Motion sender及20唯讀SQL reader位元不變。

300次最終等待通過：29／152一般／Ctrl、玩家／觀察者240次／2280斷言，153原扣款後自接收一般／Ctrl60次／480斷言；3組清理故障17斷言保留原primary、suppressed及全部pending／receipt釋放。獨立工具／plan閉包與固定輸入同位元重建、16103普通繼承檢查、150次首次等待1878斷言及原NPC／PC矩陣均在同SHA重跑。

全部58材料完整效果、PC／自然NPC／其他範圍與Source路徑、正式隔離合成、後端、中文MySQL與實機仍未完成；保留12工作群。不部署、不寫Source／玩家SQL／schema／WAL。詳見[source-negative-final-frame.zh-TW.md](source-negative-final-frame.zh-TW.md)。

## 尚未完成與完成門檻

| 順序 | 工作 | 尚需完成 |
| --- | --- | --- |
| P0 | 其餘HP生產端 | 魔杖／反射／反饋／毒傷與各原始來源、裝備／魔石／武器proc（含68非補血）、 魔法娃娃proc（技能／加傷／減傷分開核對）、fatality單體／範圍、cube ignition／harmony玩家與非玩家回呼（Source以效果NPC為HP來源）、ground pulse、隨從／bot／自然NPC物理魔法及範圍AI、群補與不死反轉。逐一核對Source raw／普通命中語意及實際生命週期，不能全部套一般魔法數字。 |
| P0 | 58材料技能與其餘適用模型 | 每列核對Source型別／權限、自己／友敵、正常／Ctrl、主次／直線／範圍／LOS、免疫與屍體、原材料／HP／MP／機率／回滾；先前專項只保留各SHA範圍。 |
| P0 | 完整外控 | 從讀取器追到實際消費端，補動態鍵／別名與SQL控制；NPC工廠／AI、受傷動作抑制已補啟動清單，其餘封包／同步消費端仍待稽核。無產品適用性的欄位需具體分類，不新增WORLD產品。 |
| P0 | 生命週期 | 優先稽核共用入口外的receipt保留／排空與其他自訂鎖路徑； 所有更新路徑之來源／帳號／角色／實際actor、成對鎖、接受與完成、退役／死復活／場景／同ID替換、最終接收端IO、DRAINING／STOPPED／Main、失敗重試與有界記憶體，仍待完整同SHA矩陣。 |
| P0 | 顯示與NPC語意 | 完整HPBAR／傷害數字／奇古獸最終接收與transport生命、NPC工廠／AI／hate／family、owner birth／manual Ref／tower／door／bot、免疫／有效數值／狀態時序、受傷動作／RO同步／多資源路徑。 |
| P1 | 正式功能隔離合成 | 保全0.6.71資源／圖示／特殊載入DLL／GM7200；實驗祖先不能整顆覆蓋正式JAR。 |
| P1 | 同SHA後端驗證 | 全部程式補齊後集中執行，舊SHA案數不相加。 |
| P1 | 中文MySQL真裝備角色 | 依使用者指示延後；舊b85只有靜態裝備與空背包實際唯讀證據，真角色重載未完成，禁止遷移與寫入。 |
| P1 | 實機游標 | 程式全部完成後集中驗正常／Ctrl、自施／跨角色／範圍、多視窗／前景／大廳。Source26不按Ctrl無法自施仍未修復驗收，合成自施封包不能代替游標。 |

完成條件：所有適用程式／Source語意逐項有證據、正式功能隔離合成完成、同SHA全回歸及集中實機驗收通過。**以上尚未全部達成；未部署、未建立新基準。**
