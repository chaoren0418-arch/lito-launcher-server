# Native 選角資源：有限清單與缺口

四個動作按鈕可沿用；兩張背景圖已存在，但此批不授 Directory 子場景映射。79／80與portraitPNG bank尚未解析到具體檔案。沒有新增抽取或美術，沒有用Classic跨版本圖資替代。

|用途／資源|既有原始檔絕對路徑|SHA256|尺寸|LTsurf row／PAK offset|
|---|---|---|---|---|
|6201.img：Native Enter/Back action pair already source bound|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row00891-6201.img|D38AC62924BEC12F6C6E6D38B2EECCB4BF70E3CA068D836353EAFA3FC491D737|51×23|891／49038213|
|6202.img：Native Enter/Back action pair already source bound|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row00894-6202.img|22742E9A3B2358238121F11DE6609E7CEBC3691DC2997B7A7E8F9142E91115D0|51×23|894／49045275|
|6303.img：Native Enter/Back action pair already source bound|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row00999-6303.img|494D2C32B01322773BF8BE52FEFE92D30AFC1B4F903EF0F5D31985637EBCAA15|51×23|999／51718555|
|6304.img：Native Enter/Back action pair already source bound|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row01002-6304.img|1EFCAAE803CE3217C84EAADD55B128276A50075A870091F92BDABA1B21E33D90|51×23|1002／51725617|
|6314.img：Existing Native640x480 resource; associated branch below, selected-scene assignment unresolved|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row01014-6314.img|848F97A410457991EF36F44E8518239E28B20D17279FEF32FB15B41AA1A9F1D5|640×480|1014／53590027|
|7482.img：Existing Native640x480 resource; associated branch below, selected-scene assignment unresolved|D:\lineage開發\3-lito開發\lito脫殼資源\resources\raw\LTsurf\row01359-7482.img|FC6B2DE5A26BE165C8CE97C7658465F6B84170D597832BC67092801C6160181B|640×480|1359／81252417|

LTsurf source index SHA `C699CCF22C5F59EB37E8936D939D2632C15AC381980A6FE3F26F2565185193BF`；source PAK SHA `42E676C255B248C295A71097A53B1B67BF73A0C2C3DC0FE11D12BDB9EE79621E`。六筆offset／name／size對原始index一致，PAK slice hash也與既有抽取檔一致。來源路徑見Native-source-container-offset-check.json。

|既有預覽|SHA256|尺寸|
|---|---|---|
|D:\lineage開發\3-lito開發\lito脫殼資源\resources\previews\LTsurf\row00891-6201.img.png|C6B5516B857A148BF80E46D142D6A774FC1468ADFB3BD4BE1E4334475822FFDB|51×23|
|D:\lineage開發\3-lito開發\lito脫殼資源\resources\previews\LTsurf\row00894-6202.img.png|A16BA1BD071F2CDB476DB272F3C81457ECB5F482B28C43795093A509C8BEE73F|51×23|
|D:\lineage開發\3-lito開發\lito脫殼資源\resources\previews\LTsurf\row00999-6303.img.png|92E601C7EF2BFBFC6886B2E43AE678CC95B8E244F77B34C2A057F829F73F1A3A|51×23|
|D:\lineage開發\3-lito開發\lito脫殼資源\resources\previews\LTsurf\row01002-6304.img.png|C7D0FFBF129BE864AC5724451B3F342BF43F5877F6CDE9E5814459D49F605DEE|51×23|
|D:\lineage開發\3-lito開發\lito脫殼資源\resources\previews\LTsurf\row01014-6314.img.png|627F6652FA5664E9A6FDC8EF891B91B7E1E749E592DF00F44DBD20ACB2594F03|640×480|
|D:\lineage開發\3-lito開發\lito脫殼資源\resources\previews\LTsurf\row01359-7482.img.png|A090AA4BDBC4EC0EAB12E1167E8CD3B091F347764A5B83A7C0739DB45D5AC456|640×480|

上述預覽為既有RGB555解碼参考；只讀IHDR/hash，不授Native renderer／透明處理／GUI一致性。

|R498數值class／variant_nonzero|table VA／fixed PE offset|既有96byte檔／SHA256|
|---|---|---|
|-1／0|0x62fd70／2293104|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-62FD70.bin／365A4EF28339D5D250DA1A7B242896BC21461D1413FB99863B98B4DCD9B55B59|
|0／0|0x630130／2294064|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-630130.bin／F769C6E8D7E10FF5126A92E0AAE3EF30DD6319F292DE354751D2A17A1B803798|
|0／1|0x6300d0／2293968|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-6300D0.bin／07294DA30A781F0841A02877CB3DE14E32E31B8A7F4C2E9B88D7306570CE3E33|
|1／0|0x62ffb0／2293680|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-62FFB0.bin／59A0F1EE09691EC053C6079199CE23C797B0FFA68F361F15B5FB0E3624FC8934|
|1／1|0x62ff50／2293584|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-62FF50.bin／B2C52CC5AB8462E0648AEBEEB5FA3C55E8956620A5A9A176BE98149C894DED9D|
|2／0|0x62fef0／2293488|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-62FEF0.bin／C877F8A04B32C2484CC5D1B6FF96538CDCB1FC76287EF3D7B13C9DA063914BAF|
|2／1|0x62fe90／2293392|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-62FE90.bin／98CF408C3DEE8F418EE17F926C402732A146ABEDF09DC01DFF1F58106CA6C368|
|3／0|0x630070／2293872|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-630070.bin／731A18F9B426D84E095F278D91CC1754B2B5D53CF075FB3009A1A9E2BB331D87|
|3／1|0x630010／2293776|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-630010.bin／1F07079DEE65C28E900AF261FE75657FCBA5B57CE2A5B15F591C2E798D46BDA0|
|4／0|0x62fe30／2293296|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-62FE30.bin／FB33A53CF082FF750C0452766FCA6A2F72B29D329E442EBDC1042EB35BBCD4E2|
|4／1|0x62fdd0／2293200|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-62FDD0.bin／7AD1573E5C9EE5A0506F0658C1C7EB03FD4B033E4A5AF7831D415D517E7D219E|
|5／0|0x630190／2294160|D:\lineage開發\3-lito開發\lito重建版\LiTo381-Modern\target\r498-character-ui-study\animation-630190.bin／B8DD1B7C2E74834938EB1E6217E79704F236E1E89BB0DE585981F8C401963A07|

12表共48state的base_png/frame_limit/delay/draw_mode/dx/dy在Native-animation-and-slot-data.json。表原始bytes與固定PE一致；class/variant association保留R498metadata，沒有新讀selectorbody，也沒有將variant當成sex。無portraitfile可給尺寸，無bankentry可給imageoffset。

6314/7482：固定PE VA496F05/RVA96F05/fileoffset618245長40bytes，10個既有指令bytes一致。64AFA0非零push7482，零push6314，kind7進495520；STOP於call。不能單靠這段宣称兩張是Directory背景。

未知項的精確分類：

- 已知indexed但未抽取：0；六筆已抽取。
- namespace／bank索引未對應：79.img、80.img及保守0..802PNG查找。五Native idx已解析且完整，共3882records；不是說LTsurf.idx本身未解碼。834BD0所在PNG bank／載入規則尚未qualified。
- 映射未知：class／sex語義、80用途、79尺寸／hitbounds、6314/7482到Directory或其他character子場景。
- 動態初始化／生命周期未知：834BD0建立、64AFA0writer／sceneowner、selected+3Dh啟動／到期／reset、timer/prefetch/ redraw。

查找缺失只限五個既有Nativeindex與非遞迴Native LTSurf目錄；不推全資源不存在，不追shared/Classic或privatecallee，不猜Native4/8容量。最小binding維持四個已綁按鈕+fullN文字名單，人物動畫與背景sceneassignment保持open。
