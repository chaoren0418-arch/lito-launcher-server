# 原生物品與地面背包：來源依賴盤點

物品建構／模板與純狀態已接續完成來源比對及原生驗證；完整能力重算與地面物品流程仍開放。詳見 [物品狀態證據](native-de-item-state-current.zh-TW.md)。

尚未實作或資格化完整物品／背包鏈。來源11類、568方法、307個欄位引用及385個呼叫已盤點；22個class／Java輸入凍結重跑得到相同清單，沒有執行SQL。收據入口：tools/resource-database/build/de-ground-items-development/inspection-current.json；邊界與剩餘工作：review-current.json。

來源L1Inventory繼承L1Object、L1GroundInventory繼承L1Inventory，L1ItemInstance繼承L1Object。盤點時NativeDeNpcInventory繼承Object、Item繼承Character標記根；這個差異已由共用物件根階段修正。現行cb2292…候選的Inventory／Item／Trap繼承NativeDeObjectFields，單一World綁定、不可變原生身分、空間與native remove已資格化，詳見native-de-inventory-root-current.zh-TW.md。這仍不等於完整物品／陷阱／地面背包狀態，原始盤點父版資訊保留供追溯。

需要完整實作來源checkAddItem與findStackTarget／重量快取／count overflow、記憶體store／trade／drop，以及地面onPerceive、insert／update／delete與來源ServerDeleteItemTimer、房屋排除等鏈。searchItemFromAir需要真正的ground inventory及item模板／數量／堆疊／重量，不能只移植迴圈或顯示封包。物品instance、模板與source S_NPCPack_Item的依賴也需逐項接線。

來源還會呼叫LetterTable.deleteLetter、FurnitureSpawnTable.deleteFurniture、CharacterInlayTable.loadInlay與CharacterDarkItemTable.getDarkItem等儲存邊界。這些必須明列區分來源只讀定義／讀取與原生自有暫時狀態；不得把來源SQL刪除行為直接帶入原生玩家流程。所有物品、背包、技能及經驗保留暫時記憶體生命週期，玩家SQL／WAL寫入限制不變。

完成上列物品鏈後，才整合來源NPC approachPlayer／appearOnGround／searchItemFromAir的hidden狀態、真正hate／AI、對話與原生輸出。完整PC／NPC感知與scene factory仍未完成，本盤點不是功能通過或部署宣告。這些來源證據仍會被後續實作引用，暫不清理。
