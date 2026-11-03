# 原生光術時間窗缺口與來源服契約控制

2026-10-18：延續已通過的光術 action／輸出鎖邊界候選389663…，取得尚未修正的施法時間窗差異。這份是缺口與來源行為證據，不是新程式候選或全技能驗收；正式部署不變。

兩個 memory-only 旗標、BATTLE／WORLD 共四案均直接呼叫實際 handle。角色 gfx0 的 action19 原生動畫為1480ms，光術 reuse為10ms。第一個實際光術提交後，只等待原 reuse 真正到期，沒有清空／改寫 reuse，再呼叫實際 handler；第二次在第一段動畫內提交，MP100→96→92。未修改的目前來源 ClientActionTimeline 與 PacketExecutionContext，在相同兩個時間戳及相同1480ms輸入下拒絕第二次。共64個原生 frame 獨立解密，自己每案兩組 motion／effect／sound／light／MP，MP分別96、92；旁觀者兩組 motion／effect／light。實際 PC、commit、completion與動畫 catalog來自固定候選 JAR，沒有 production overlay，原 receipt全部釋放，沒有持 action/status lock送包。

此控制把原生動畫間隔提供給未修改的來源時間窗核心，因此證明的是相同輸入下 admission差異。尚未證明完整來源 SprTable／AcceleratorChecker／ActionIntervalCalculator狀態與原生動畫時長一致；完整 sprite與haste／slow／brave等速度對應仍開放。

另以目前來源原檔完整編譯執行：時間窗10案52檢查，包括零／負／wrap ingress、精確到期與stale邊界、材料等待後runtime rebase、晚執行的重疊封包、精確token取消、ATTACK→SKILL、獨立MOVE債、16執行緒單一reservation勝者及nested ingress例外還原。SkillCastAdmission原排序核心6案16檢查涵蓋preflight拒絕、reservation拒絕、材料拒絕、成功與RuntimeException／Error的取消及例外identity。沒有執行來源PC、JDBC、材料庫存或L1SkillDelay scheduler，不能將純核心結果延伸成整體移植通過。

下一步須在真正原生施法消費端加入來源時間窗與不可變ingress，對齊action與reuse同時開始、材料失敗取消與runtime rebase，保留競技／隊伍／TA及記憶體角色／道具／技能／XP。新增Source規則可能改變舊fixture的快速連續施法預期，必須保留原斷言／期限並明確記錄差異，不能靠放寬期限、替換PC或跳過cooldown隱藏失敗。完整目標仍開放，未部署。

收據：`tools/resource-database/build/native-cast-timeline-control-current.json`。重驗：`python -B -X utf8 tools/resource-database/build/audit-native-cast-timeline-controls.py`。原始四案／來源原檔與控制程式保留在獨立development目錄，供後續修正前後使用同一編譯控制，不複製整套專案。
