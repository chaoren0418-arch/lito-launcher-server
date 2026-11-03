# 物件座標與 World 共用狀態

2026-10-17 的隔離候選 `c4a8579c4f76340c2e1331b8d14086c6e2ba515d274befbba9d16033eed5b228` 兩次乾淨建置相同，尚未部署。

來源 L1Object 的 26 個座標、地圖、位置世代、距離、回呼及副本方法，與 L1Location 的全部 18 個資料方法已獨立逐指令核對；隨機選點方法另列未完成。共用原生 Character 現在繼承來源物件狀態，NPC 的 showId 沿用父類同一欄位；角色與 World Entry 保持同一個 Location 實例。保留原生不可變 objectId，來源 `_id` 的零值初始化省略一處，`setId` 僅接受目前ID；這是明確的原生識別契約，未宣稱來源可變ID或完整原始階層等價。既有未綁定 NPC 的 Effects 座標契約保留，完整 NPC 建構狀態新增三個來源座標橋接。

獨立 raw source oracle 保留全部四個來源物件欄位與可變ID，46 個 oracle 方法亦完整逆向核對。兩輪來源各 6,108 案／30,282 項，向量 `0dfd174fe61eb7ff06800310591540abfba4b69e9ed2a611ab47d84fec6a0ec1` 一致；涵蓋六種 Location 建構、全部資料 setter、地圖參照身分與 null-map／真正地圖0、short截斷、整數／long邊界、無效面向的例外前綴、巢狀scope的成功／Exception／Error還原。實際物件monitor、remove／mutation競爭及真正World空間索引另以 development probe 與凍結World回歸驗證。

兩輪 application 各 8 場／199 項、16 個真實cipher／TCP frame。來源共用位置在NPC發布前、發布後及appearance座標上均一致；加密封包另行獨立解密。兩種 statePersistenceEnabled 設定均使用實際記憶體 store；來源NPC建構、範本／只讀JDBC、感知、刪除、已知物件、helper、native、48組廣域與同伴回歸均重播。原生class及控制資源變更0，8個既有helper改動、5個新helper，沒有新executor。

凍結 Character 建構測試每種設定會遍歷 NPC 宣告欄位。移除一個 NPC 自有 showId 並改由來源父類保存後，兩種設定共少計2項欄位遍歷檢查（3293→3291）；全部262案及原向量 b962d1c0… 完全相同。初始統計比對失敗、原始driver及修正後的明確欄位差異收據均保留，沒有更改來源行為測試或放寬逾時。

完整原始 L1Object／NPC／De 階層、De constructor／initializer／scene factory、地形／隨機傳送／實際移動、完整inventory／AI／技能及所有writer／lifecycle仍未完成。既有NPC排空競爭逾時的根因仍未證明；來源56個executor與28種建構需求仍開放。De接收器仍為UNSUPPORTED，canonical receipt仍2ad485…；四個固定基準與來源服、玩家SQL／WAL未改。

主收據：`tools/resource-database/build/native-de-object-current.json`；重驗：`python -B -X utf8 tools/resource-database/build/audit-de-object.py`。495個本階段舊嘗試／重複檔案已逐SHA封存驗證後精確移除，保留37個凍結輸入，見`native-de-object-cleanup-current.json`。
