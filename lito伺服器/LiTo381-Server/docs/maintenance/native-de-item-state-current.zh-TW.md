# 物品建構、模板與純狀態

物品建構與純狀態（2026-10-17，最新隔離候選）：來源11類、323欄位與451方法inverse通過，兩次乾淨建置一致；9案／201項、真正World與兩組TCP、12組既有回歸通過。完整能力重算、到期生命週期及地面物品仍開放，未部署。

候選 SHA `3c6ba6b161f0ac55ae419e1d6a7dbfcf7f1bc9f77ba258d2479dcd2b4adf27a4`，接續共用物件根 cb229205…；canonical 仍為 2ad485…，本候選未部署。實際 NativeDeWorldKinds.Item 已擁有來源 L1ItemInstance 的全部欄位初始化、兩種 constructor body 及112個封閉狀態方法，不另造平行角色。L1Item／EtcItem／Weapon／Armor／VIP、L1ItemAbility 與81種能力enum、DarkItem、LastStatus及EquipmentTimerLease的來源宣告／方法完整保留。共有451個來源方法、323個欄位通過獨立反向比對；60個Item方法明列 deferred，沒有替成成功回傳或空回呼。

原生不可變ID、Object父鏈與單一World綁定沿用已驗證基礎。兩個原JAR entry變更（Item與其nest host），新增12個class，沒有新增資源或executor。來源預設count=1、AtomicLong變更版本、remainingTime原子操作、能力鎖、狀態cache、五格鑲嵌、覺醒清單等均由完整來源constructor建立。新增native ID及暫時資料參數後，來源constructor與其他方法可精確還原。

CharacterDarkItemTable的唯一來源存取改接每個物品明確持有的NativeDeItemMetadata，無全域registry、SQL或WAL。metadata與DarkItem的物件ID不一致時立即拒絕。loadDarkItem保留來源signed getId()>0條件；負int的原生unsigned ID仍有正確World身分，若需附加資料可走來源setDarkItem，完整factory尚未資格化。來源Timestamp setter／getter保留同一引用的語意，沒有自行改成copy；來源count setter也保留原值與每次呼叫遞增版本的語意。

獨立狀態探針9案／201項：實際Item建構與欄位隔離、非空附加能力與foreign ID拒收、Integer.MIN_VALUE、81種能力與同步累加、原子remainingTime，以及delay_effect雙時鐘、nanoTime溢位、毫秒向上取整與到期邊界。真正memory World根探針3案／52項，兩輪物品根TCP各12案／73項／12 frames，以及兩輪視野TCP各32案／125項／32 frames通過；封包由實際native TCP輸出並獨立解密。兩輪constructor各1,912案／27,030項與父候選向量逐位元一致；12組既有回歸、NPC對話、商店及地形檢查均通過，等待門檻沒有放寬。

完整Item的能力重算／靜態資料載入、魔法石與裝備到期、PC owner刷新／持久化邊界、完整Inventory及GroundInventory／物品封包仍未完成。後續來源定義盤點在de-item-ability-development/review-current.json：16類、234方法、15段SELECT；來源loader會吞SQL／單列錯誤，必須在owned唯讀交易中收集錯誤並拒絕部分快照，rollback與close完成後才能發布。尚未執行該來源runtime，未冒稱完整能力系統已實作。

全職業技能／Ctrl／範圍、完整NPC factory／AI／hidden approach、PC效果writer與完整技能生命週期仍開放；L1De仍UNSUPPORTED。來源服、玩家SQL／WAL及四個固定基準均未改動。

收據：tools/resource-database/build/native-de-item-state-current.json。重驗：`python -B -X utf8 tools/resource-database/build/audit-de-item-state.py`。依精確路徑／SHA封存並驗證後清理50個重複或失敗檔案；保留36個凍結輸入及必要證據。
