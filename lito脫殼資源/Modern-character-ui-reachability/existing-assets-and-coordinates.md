# 既有選角圖資與座標索引

只讀既有檔案／IMG header／PNG IHDR／既有 JSON 與 plist；沒有新增解圖、裁切或美術。SHA 與路徑採讀取當下前後一致的封存值，外部後續修改不要求 freeze。Native LiTo 與經典版 Lineage 是不同圖資家族。

|家族／用途|既有絕對路徑|Bytes|SHA256|尺寸|
|---|---|---:|---|---|
|Native LTsurf 6201.img／返回|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row00891-6201.img|2354|D38AC62924BEC12F6C6E6D38B2EECCB4BF70E3CA068D836353EAFA3FC491D737|51×23|
|Native LTsurf 6202.img／返回|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row00894-6202.img|2354|22742E9A3B2358238121F11DE6609E7CEBC3691DC2997B7A7E8F9142E91115D0|51×23|
|Native LTsurf 6303.img／進入角色|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row00999-6303.img|2354|494D2C32B01322773BF8BE52FEFE92D30AFC1B4F903EF0F5D31985637EBCAA15|51×23|
|Native LTsurf 6304.img／進入角色|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row01002-6304.img|2354|1EFCAAE803CE3217C84EAADD55B128276A50075A870091F92BDABA1B21E33D90|51×23|
|經典版 Bg_CharacterSelect.png|D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標\UI_Resource\Outgames\Bg_CharacterSelect.png|71670|0B528A0ED5183A4DB5C60A81D62DA138199E8E22CAABC261B57F7D86CF0889C2|1280×960|
|經典版 Tx_CC_Ok_N.png|D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標\UI_Resource\Outgames\Tx_CC_Ok_N.png|1049|F76C54D26B56649F69E001B02CC51E430689B460DB65D1CA1D01FF489485465B|110×38|
|經典版 Tx_CC_Ok_C.png|D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標\UI_Resource\Outgames\Tx_CC_Ok_C.png|1730|FDFD364F83CE02C508A7E291BFA3A8B7F1B31288F078F2D10BB7C1F078785EBE|110×38|
|經典版 Tx_CC_Cancel_N.png|D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標\UI_Resource\Outgames\Tx_CC_Cancel_N.png|2040|C0DB894B66FAD4259228A1BAE5FD8FDBED19540AD25CFC5467E7DF22B32EB698|192×35|
|經典版 Tx_CC_Cancel_C.png|D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標\UI_Resource\Outgames\Tx_CC_Cancel_C.png|2800|85F289B5BFF91DD52022AF784E0BF0CBD94997CA24CA5E4AC81AA70F2229F663|192×35|

Native 四個按鈕已在候選 skin binding 指定，window owner draw 有 normal/pressed 選擇及文字 fallback；本批沒有畫面驗收。SUBMIT=(497,404,51,23)，BACK=(552,404,51,23)；Modern resize 加 vertical232 並做 DPI scale。Native x19/176/333/490、y0 是四個既有控制描述，不證明 record 容量。來源函式／行號／SHA 在 character-flow-findings.json 的 U04、D03，既有 Native R498 report 的原始 pin 在 Native-existing-asset-metadata.json。

經典版既有 CSB 場景1280×960，Layout center640,480；ListView local(-545,-55)、1100×534；OK local(478,-339)、110×38；Cancel local(478,-374)、192×35。local position／anchor／scale 與 Modern 螢幕／Native hit coordinate 不可直接混用。

|經典版座標／圖集來源|Bytes|SHA256|
|---|---:|---|
|D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標\csb_json\CharacterSelectScene.json|5308|AAB8DCBC50B7A71E505C763EA25D6A50CAF96041F98017C06AC4B48421234329|
|D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標\csb_json\CharacterSelect\CharacterSelectLayout.json|1098997|722962765CBDE30F3AEB17CFE66F65A8185C7F631D21676702737E1A129E2B46|
|D:\lineage開發\介面UI\經典版\0429經典服UI\UI_Resource\OutGames.plist|154746|43CA77A6E0FB24F181F871AC2B068621D7CD850E8656F69D347D46729E3F517A|

|既有 PNG frame|plist frame|rotated|
|---|---|---|
|Bg_CharacterSelect.png|{{1,1},{1280,960}}|False|
|Tx_CC_Ok_N.png|{{1805,3774},{110,38}}|True|
|Tx_CC_Ok_C.png|{{1765,3774},{110,38}}|True|
|Tx_CC_Cancel_N.png|{{1848,4060},{192,35}}|False|
|Tx_CC_Cancel_C.png|{{36,1220},{192,35}}|False|

以上五個經典版 PNG 尚未在已讀 Modern window/skin closure 中載入。plist rotation 是圖集來源資料，已有 PNG 自身尺寸另由 IHDR 驗證。本批未執行裁切。Native 79/80 的既有查找缺口只限已封存 R498／精確 LTsurf index，不能說所有資源都不存在；不以經典版檔名推 Native class／sex portrait mapping。
