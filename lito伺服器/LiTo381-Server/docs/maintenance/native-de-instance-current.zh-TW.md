# De實體建構與初始化

De實體建構、初始化與實際外觀輸出（2026-10-17，最新隔離候選）：五個來源constructor、43個宣告欄位與196次初始化寫入已投影到共用NPC狀態；79個來源方法與129個既有方法inverse核對通過。兩次乾淨建置SHA `1b3002d94459d84b857f1b52a56a4d8a1b900fba1c097ea0dacf5e798ac05469` 相同。兩輪來源各1,912案／27,030項，兩輪實際application各8案／264項、24個cipher／TCP frame；共用名稱快取保留不可變定義，各World持有獨立可變名稱並於close清除。完整AI／De receiver／factory及技能生命週期仍開放，未部署。

七個來源欄位沿用同一份實體狀態，36個新增；不省略NPC父類回呼之後的顯式初始化。來源自己的final死亡世代AtomicLong在完整NPC父類返回後才建立，新增精確NPC setDead super-body thunk，防止經過舊De adapter重複失效。NPC建構回呼仍看到De欄位的JVM預設值，包含來源復活在此時的例外前綴。保留原生不可變ID與服務ABI，沒有宣稱完整原始階層等價。

完整來源Name DTO的19個方法與八個可變欄位保存在每World的實體值中；共用控制目錄的八個final欄位與九個既有方法均保持不變。名稱以定義的identity在World內canonicalize；不同World不共享可變名稱，close在原有admission write gate內清除名稱空間。新增guard阻止對共用定義呼叫來源setter。初版把共用定義直接改成可變值，原有啟動不可變性檢查因此失敗；該失敗候選35979c…、log與原始fixture已保留。修正後原fixture未放寬，25案／29,850項通過。119項development probe另驗證名稱隔離、close拒絕、完整建構次序、獨立死亡世代與真正外觀封包中巢狀建構成功／Exception／Error的scope還原。

獨立raw source oracle保留43個自有欄位、196次store、55個De方法與完整DeInitializer／DTO的13個來源方法，不借用native欄位alias。兩輪1,912案／27,030項向量965f9dcd9b311848dfbb87a470cf91e6e33f33827c21361fc4a89e87453e7ddd一致；包含五種constructor、當前控制表與sprite、初始化與companions分支及故障前綴。source向量輸出曾超過256MiB heap，測試改為逐行寫入並增量雜湊，沒有提高heap或刪減案例；另保留早期oracle遺失stack-map frames的JVM拒絕證據。

實際來源單封包WorldBroadcast閉包四方法已接到既有World觀看者與原生cipher／TCP路徑；兩輪各8案／264項與24個實際封包（出現、初始化外觀、刪除），另行獨立解密。兩種statePersistenceEnabled設定皆使用真正memory store；名稱刪除與初始化使用同一實體identity。原有strict perception scope未修改，另加nested入口；沒有新增executor。六個舊helper改動、17個新helper，原生class與控制資源變更0。

既有12組driver依序重播，含來源範本、感知、World、known、完整刪除、51,635案／1,049,774項helper、10,783案native、裝備、sprite與控制表啟動、48套／1,728案廣域及887案同伴。先前NPC deadline／reset排空偶發逾時的根因仍未證明，原失敗證據與限制繼續保留，未放寬timeout。

De getName目前仍沿用native NPC adapter；來源De覆寫、speed／spawn、完整Runnable／run／startAI／wakeup／stopAI、De專用receiveDamage／死亡handoff、完整movement／inventory／companions producer與自動live scene factory均未接通。新實體目前仍繼承generic NPC receiveDamage；來源56個executor、28種建構需求與完整技能／writer生命週期仍開放。L1De仍UNSUPPORTED，canonical仍2ad485…，完整目標未完成，不部署。來源服、玩家SQL／WAL與四個固定基準未改。

主收據：`tools/resource-database/build/native-de-instance-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-instance.py`。2834個舊嘗試／重複檔案已按SHA逐內容封存驗證後精確移除，保留41個凍結輸入；見`native-de-instance-cleanup-current.json`。
