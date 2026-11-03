# 跨角色材料技能與生命週期驗證

2026-10-20 隔離候選 `175022207c3c5b7954eb0f86edc01ec879f849314e978d2eee35d176961ded71`，父候選0269195c。技能68聖結界改用既有Source成對實體鎖；施法者、目標與各輸出接收者都有角色／場景epoch驗證。目標receipt保留到整個原始輸出批次結束，GM查詢後與材料安裝前重驗目標，避免同objectId切角、場景轉移或退場後提交。保留原生競技、自我／血盟、存活／觀察者／隱身、座標與螢幕範圍、MP、效果及封包尾段；材料走現有canonical記憶體交易，移除效果之後才保存的舊入口。

本SHA實測216案：兩persistence旗標、WORLD／BATTLE、有無item application、自我與跨角色、材料／MP不足、Source共用窗口、兩側實體鎖、GM／查詢失敗、角色／場景退場與STOPPED已受理工作。舊0269195c同fixture104案觀察到16個鎖／窗口缺口，新版歸零；新版無鎖內socket寫入，FIFO與全部receipt清空，沒有狀態檔。正常自我／跨角色封包與父版逐位元組相同；Ctrl成功封包亦同普通施放，不能繞過血盟目標限制，不能跨目標角色退場繼續施法。

另外72案驗證相反方向Ctrl、外層例外、逐封包目標退場、旁觀者接收者切角、施法者／目標輸出IO失敗、目標斷線與application退場。兩側各持一個外層鎖時，技能入口先嘗試非阻塞取得缺少的一側；若忙碌，在Source材料／動作／reuse受理前拒絕，避免原Source成對鎖API在這種巢狀情境互等。其餘情況仍進入原Source排序鎖演算法。額外借用的鎖在同一既有action scope結束前釋放，原始輸出仍等所有實體鎖解除。這是明示的受理前安全門檻，並非宣稱完整Source巢狀等待語意已移植。

真正Main本SHA10案：一般／GM／查詢失敗／施法者退場／目標退場，各正常與中斷關閉。repository只關閉一次、關閉後呼叫零；已受理工作有資源，退場批次輸出零，handler與receipt歸零。技能73的Main8案回歸也在本SHA通過。另實跑1068案既有材料與Light邊界：自我384、龍騎192、方塊192、道具武器96、技能73施放88／退場32／STOPPED16、Light68。Light舊root fixture仍以未移植前的raw MP值與重施時序為前提，第一次舊候選回放失敗已保留；本次凍結測試改用獨立Source費用oracle、明設原隱身中斷旗標並等待真實Source時鐘後再檢查原有邊界斷言，未改production或放寬JVM期限。

只變更4個既有entry、新增3個helper class；PC僅技能68原方法的有界材料／鎖定接點與一個保留原數值／效果／尾段的body。C0與父版byte-equal，整類重建與獨立inverse通過；無新增PC欄位、ThreadLocal、worker或scheduler。兩次只用凍結輸入重建得到相同SHA。Source另有既存拍賣、精確持久化扣款及解散血盟保存增補，整檔inverse確認材料selector與角色AccessLevel不變，沒有把Source持久化寫入搬入LiTo。

材料邊界進度25／58，其餘33個已按實際metadata逐項列在機器可驗證清單；舊呼叫剩44個方法／45個call site，不能當作技能數。技能68完整Source距離／LOS／地形、Buff刷新取消／訊息／圖示、其餘Ctrl與範圍多目標、NPC／道具／能力／倉庫、accepted-cast與buffered attacks、補償／quarantine、正式0.6.69讀取類別語意合併及最後560＋60數值／完整階段驗證仍開放。祖先外控及完整回歸保留原SHA，不改標成本次新證據。

競技模式、角色／道具／技能／XP記憶體生命週期、Source與玩家SQL只讀限制保留；只有character_lito／settlement可持久化。正式檔、固定輸入與WAL保持原雜湊；未部署、未清理。失敗場次與測試接點凍結保留供追溯。[父階段](native-retired-reuse-timer-progress.zh-TW.md)。
