# 實際 RuntimeInventory 與來源 Item 顯式綁定

2026-10-17 隔離候選；**顯式綁定 API 已驗證，PC 自動接入及完整 Item 生命週期尚未完成，未部署。**

候選 SHA-256：`6aa20dcbfb04cf490c4aead5ee995d8f5c6a296da90a2bc7d0b270d1d8d36f63`。父收據為完整物品模板階段；canonical 與四個固定基準保持原雜湊。

RuntimeInventory 保留不可變 Item record／withX／既有演算法。新儲存介面以每物件一個可變來源 Item 作為已解析狀態的依據，回傳原生不可變快照；原生修改寫回同一來源物件。來源與原生的數量、換裝、鑑定、祝福、耐久、強化、充能及封印狀態對接；物件 ID 保留 unsigned dword，綁定數量範圍明確限制為原生正 int，數量零移除物件並拒絕舊 actor 再修改。祝福表示顯式交換 0／1。

顯式捕捉完整 5,107 個凍結來源模板和靜態能力定義 snapshot，不以原生摘要補造模板數值。缺少模板、類型不相容及原生專用物品保留為可查詢的 opaque native entry。已綁定物件的變形必須同時有原生 definition 與捕捉的來源模板，避免部分更新。

copy／sourceEquipmentOnly／withoutObjects 複製休眠 Item 狀態，包含能力、VIP、暗黑、鑲嵌、覺醒、Timestamp alias、LastStatus 與 count mutation fence；不共用鎖及可變狀態。shuffle 保留 actor 身分與附加狀態。活躍或待執行的到期 future／lease／submission 拒絕複製。同步原生精確武器快取的強化、耐久及可表示的祝福；變形移除舊快取。

驗證結果：

- 兩次乾淨建置 SHA 完全一致；19 案／80 項實際背包 API 測試通過，包含雙向變更、不可變快照、複製隔離、未知模板、變形、篩選、shuffle、到期拒絕、unsigned ID 及並行數量更新。
- 獨立位元碼 inverse 核對 131 個既有 RuntimeInventory 方法，僅允許儲存介面、83 個共同 monitor、97 個呼叫運算元、四個複製回傳點及 shuffle dispatch 的有限轉接。115 個來源 Item 方法與欄位不變；補齊兩個來源內部類別的 InnerClasses 宣告，修復實測的反射不一致錯誤。父 JAR 僅兩個 entry 改變、新增四個 helper，其餘 entry 逐位元相同。
- 既有 12 支原生回歸測試通過；沿用原斷言、截止時間及測試程式，更換證據目的目錄並明列四個傷害佇列上限（2／256／64／64）。另重播凍結來源 Item 9 案／201 項、Object root 3 案／52 項與兩組各兩次實際 TCP 驗證（12／73／12 frames、32／125／32 frames），封包獨立解密比對通過。TCP 重播驗證既有路徑，並未宣稱新的 PC 綁定已接入。

仍待完成：所有 ClientSession inventory／warehouse／rental 切換與 action／commit 所有權、DTO 轉移的完整來源附加狀態、繼承 Object 的空間／World 發布及 live owner handoff；60 個來源能力／到期／狀態方法、實際 PC 裝備刷新／封包、來源附件唯讀匯入、工廠與執行器。現有 copy 證據只涵蓋休眠 Item 實例狀態；沒有宣告完整 live lifetime／空間複製或完整傷害重算。其餘全目標缺口繼承父收據。

未新增 executor 或外控資源，未寫來源服、玩家 SQL／WAL、正式部署。依精確路徑與 SHA 驗證封存後清理 1447 個重複／失敗生成檔，保留 14 個凍結輸入及必要證據。

收據：`tools/resource-database/build/native-de-item-binding-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-item-binding.py`。初始 `de-item-binding-development/review-current.json` 保留為盤點快照，實作進度以本收據為準。
