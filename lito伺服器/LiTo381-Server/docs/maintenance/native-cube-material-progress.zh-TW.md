# 立方體材料與動作提交施工紀錄

2026-10-18 隔離原型：`218d54c4c301fe16b7519fe1c57b5a7254ed6718a1ec0de1f7483427dac61086`，父版為18材料技能原型 `a9c10856…`。新增立方：燃燒205／地裂210／衝擊215／和諧220，合計22個材料技能及97／102兩個無材料變體。**其餘36個材料技能、全部職業／Ctrl／目標／範圍／item／trigger 的完整覆蓋及整體目標仍未完成，未部署。**

本輪修正效果期限起點：成功通過Source action與材料提交後，於原Cube建構前取得System.nanoTime，保留原期限公式及原place方法。來源L1SpawnUtil於建立／登錄效果後呼叫set_spawnTime，L1NpcInstance以當時System.nanoTime起算存在時間；完整來源檔與片段證據已凍結。相對前版46c5bee5只改四個效果class，其餘entry逐位元組相同。

原PC四個方法的數值、效果與封包body保留。原GameHub八個放置方法及四個效果類別的原place方法完全不改，只新增有界alias；位置、場景、競技模式、門／塔／任務／TA阻擋及附近同種立方體檢查仍由原方法判定。通過這些檢查後才進入Source action／reuse與材料提交，再建立立方體。重複或無效放置保留1412；缺料／保存失敗只回299；Source時間窗拒絕不建立效果、不扣材料、不額外送1412。

角色材料旗標先在場景、狀態及lifetime monitor之外查詢並完成快取，再依既有life admission → source item gate → canonical owner／inventory → memory state → hub → cube effects的順序進入放置。材料核心沿用父版完整operation block與選取器，未開啟另一筆巢狀交易；一般item／lifetime／SQL類別逐位元組不變。沒有新PC欄位、worker、scheduler或ThreadLocal，只有既有cast收據的兩個暫時旗標。

四個真正handle入口前後各192案，兩種persistence旗標、WORLD／BATTLE、有／無item application及正常／跨技能Source時間窗／缺料／重複放置／無效位置／MP不足。32次時間窗繞過歸零，正常解密封包相同；前後864／640 frame，鎖內寫入零。只有在各獨立fixture開頭固定該效果類別的object-ID起點，避免故意拒絕的施法改變後續案例的識別號；沒有修改正式物件或計時。

真正GameStateStore保存前故障，前後各32案：父版32次已放置效果、扣料、MP與reuse的部分提交歸零；修正版保留背包identity及原economy，只回299，取消Source provisional tokens且可立即重試。前後96／32 frame，鎖內寫入零。只覆蓋經原指令inverse驗證的GameStateStore觀察接點，PC／hub／effect／item／timeline皆來自真正JAR。型別化角色旗標provider確認新初始查詢只執行一次，且位於hub／state／lifetime monitor之外；沒有真實玩家SQL連線。

新增同一份編譯前後各128案，涵蓋兩旗標／WORLD與BATTLE／有無item application／四技能的政策延遲、延遲時STOPPED、provider失敗與兩名角色同點並發。64次被政策等待吃掉的效果壽命歸零；每版32次並發只有一個效果、一筆材料與MP／reuse，32次provider失敗可立即重試，32次等待中STOPPED接受工作一次完成。每版192次政策讀取、1088個frame，鎖內寫入零。

真正Main的正常／中斷關閉前後各8案，16次原生觀察均通過、128個frame；期限不足8次歸零，repository關閉各一次、關閉後呼叫零。政策等待時同時保留封包作用域與已接受施法收據兩份application保護，延後釋放資源；late packet不重扣、不重建。Main／SourceLogin只使用獨立整類inverse核對的七個記憶體／port／觀察接點，所有其他指令與真正JAR相同；PC、hub、材料、lifetime直接來自候選。初版fixture錯把Main觀察層當成JAR來源且漏計封包作用域，已核對正確載入位置及兩份保護；原失敗保留。16案完成後摘要列印重複skill鍵失敗，另作雜湊／解密postcheck核對，未重跑native或改斷言。

32案preaccepted STOPPED通過：接受的材料、MP、reuse與立方體一次完成，256 frame、鎖內寫入零；新施法、一般道具與Source setter仍拒絕。128案涵蓋提交後角色退役、排隊中角色／場景切換及首次真正socket故障，不重扣、不重建、不向替換角色送舊封包。最初候選在原dispatcher的鎖前尚無receipt，測試實際記錄queued=true／pending=0；已把四技能接到既有dispatcher預先保留入口，未改原斷言與期限。兩次失敗原文及初始候選保留。

既有16技能384／384、四龍騎士192／192、兩組STOPPED128／32與兩組邊界512／128，以及Counter Magic材料／保存故障／封印／GM40／選取器171／過期副本／授權及停止控制，均在本SHA重跑。完整20組回歸、14組生命週期、10組既有控制與兩次凍結重現通過。重現只讀凍結Java／父JAR／ASM／library，不執行可變preparer或Source classpath。獨立整類inverse與精確9改／1新增entry檢查保留其他方法及一般授權類別；1543項進度稽核通過。

最初沿用增益fixture的100／120虛擬位置，原GameHub正確拒絕；已改用真正arena.spawn與世界會員位置。原失敗保存，production未因測試座標放寬檢查。

生命週期索引曾拒絕整理結果：外層標示本候選，但未轉換的子工具仍跑父版；該14組外層標籤已明確否定，原manifest／log及routing-correction保留。修正全部子工具路由後在本SHA重跑14組，逐一稽核各子案例的runtime與凍結輸入。其他19項結果只在相同runtime、driver及原log雜湊核對一致後重用；未重啟有效原測試，未調整斷言或期限。

較早來源證據依既有cleanup收據的精確原路徑／SHA找到封存，逐archive及內容雜湊驗證後放入本輪archive-verifications，原檔案及封存不變。政策邊界與既有timeline邊界曾共用current索引名稱，已用唯一cube-policy-boundary-current索引連回原128／128案manifest，原生未重跑。來源CharacterTable期間更新為帶版本的mapTime儲存快照；全檔差異已凍結並核對只含這一段，材料角色旗標及載入文字保持一致，其餘四個材料來源全檔SHA相同。此變更屬來源角色儲存快照，原生維持角色唯讀及記憶體生命週期。

仍需完成36個材料技能與其他producer；政策失敗／角色退役與Main關閉同時交會的額外組合；保存後安裝異常回復及quarantine、opaque與Source long數量、執行期間GM旗標、Source accepted state與buffered physical-attack cancellation。立方體政策延遲／Main等待的效果起點已按來源校正；其他技能與producer的阻塞I/O時間戳仍待逐一核對。最後生產變更後仍須必要560＋60數值重播、完整階段稽核及依SHA／精確路徑封存清理；本收據不宣稱完整材料階段或整體目標完成。

保留競技／隊伍／TA及記憶體角色／道具／技能／XP，只允許character_lito／settlement持久化。正式核心、固定base、聯合基準、canonical收據及來源專案／玩家SQL／WAL未改。所有施工輸入、反例及原始紀錄尚保留，未清理。

進度收據：`tools/resource-database/build/native-cube-material-progress-current.json`。核對：`python -B -X utf8 tools/resource-database/build/audit-native-cube-material-progress.py`。父版證據見[18材料技能紀錄](native-cast-material-progress.zh-TW.md)。
