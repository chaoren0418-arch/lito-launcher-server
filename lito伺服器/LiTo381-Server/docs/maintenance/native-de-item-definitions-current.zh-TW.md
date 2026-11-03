# 物品靜態能力定義與唯讀快照

物品靜態能力定義（2026-10-17，最新隔離候選）：來源16類、234方法／126欄位inverse、15段SELECT的單筆owned唯讀交易與78案／6,742項通過；全部既有JAR entry逐位元不變。完整Item能力／到期生命週期仍未連接，未部署。

候選SHA `9ee6ece29a227739fd51e0bb4011ee0caf5a8c895acf32a0e35de7f4c960f548`，接續物品純狀態3c6ba6b1…；canonical仍2ad485…，未改正式部署。完整來源的祝福類型／指定ID、強化通用／指定、Greater、屬性武器、卡片、魔法石、武器／裝備Proc表與DTO共16類已投影，包含所有查詢、能力映射及計算方法。十個來源getInstance改接明確owner；28個載入入口、46個receiver logger、10個constructor owner及唯一static map轉instance map均有限列出並可反向還原，234方法及126欄位完整inverse通過。

十五段來源SELECT共用一條實體連線，明確readOnly=true、autoCommit=false、REPEATABLE_READ；來源try-with-resources關閉的是十五個logical lease。查詢白名單、十五組statement／result set、source error收集及資源計數均完整後，必須先rollback及physical close成功，才可凍結、返回並由owner原子發布。任一必需欄位／列／查詢、設定、logger Exception／Error、rollback／close失敗均拒絕新快照並保留舊世代。原來源支援的optional-column fallback則保留並與raw source比對。

發布後的巢狀map／list與能力容器禁止修改；來源卡片與魔法石內的能力也凍結，實際actor仍用原可變NativeDeItemAbility。資料表不能原地reload，需經owner建立完整新世代。十個singleton不再擁有來源static實例；不同owner及巢狀scope彼此隔離，scope結束即還原，沒有新executor或控制資源。

探針78案／6,742項：直接共用SQL與離線重播的全欄位物件圖一致；test-only十六類raw source僅更名及轉接SQL／logger，未改body／singleton／map／guard，與新表的所有map／DTO資料一致；兩張實際空的指定能力表另在disconnected fixture補列，不寫資料庫。逐查詢注入查詢、必需資料列與讀取中斷，另驗設定／關閉／logger失敗、完整舊世代保留、深層不可變性、nested owner及optional欄位分支。實際資料含屬性武器40、魔石324、卡片117、武器Proc50、裝備Proc54列。裝備Proc與既有native catalog的18欄位及54列、極值／負值／一般強化bucket選取一致；沒有從旗標載入冒稱武器Proc效果已接妥。

測試擷取曾遇CachedRowSet將TINYINT(1)值2先變成Boolean，遺失數值；fixture改由原JDBC getLong保留數值，之後十張表逐欄與live driver圖一致，production來源計算未為此修改。擴充清理測試另重現native leak-drain的unchecked close缺口；現已收集該錯誤後繼續rollback／close。同一份編譯fixture對舊e40f4807…失敗、對新9ee6ece2…通過，兩JAR只差NativeDeItemDefinitionsSql.class。

兩次乾淨建置一致。父候選所有entry與資源逐位元保留，只新增31個class；既有戰鬥／TCP／12組回歸證據以此完整identity承接，本階段沒有冒稱重新執行那些套件。這是完整來源定義及實際SQL載入驗證，仍須把快照owner接入真正Item的updateAbility／ensureAbilityFresh、魔石／裝備到期、PC owner刷新與記憶體保存回呼，並完成50筆武器Proc的效果消費端、完整inventory／ground流程、技能／Ctrl／範圍及scene factory。L1De維持UNSUPPORTED，完整目標尚未完成。

來源服、玩家SQL／WAL、正式部署與四個固定基準均未改。收據：tools/resource-database/build/native-de-item-definitions-current.json；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-item-definitions.py`。依精確路徑及SHA驗證封存後清理416個重複／失敗檔案，保留21個凍結輸入與必要證據。原de-item-ability-development/review-current.json已被上一階段收據引用，保留為原盤點快照；本階段實際狀態以新收據及progress-current.json為準。
