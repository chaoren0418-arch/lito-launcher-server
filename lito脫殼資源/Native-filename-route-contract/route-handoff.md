# Batch37：43FFC0 的檔名 route 契約

43FFC0 是65byte／35instruction的leaf，沒有任何callee或static string/table分類依賴。它對ECX所指穩定、可讀、NUL結尾CString逐unsignedbyte相加（DWORD wrap），回傳signed_int32(sum)的remainder16；不是解析副檔名／namespace。

一般return範圍−15..15；sum signbit0時low4，signbit1時low4=0回0，否則low4−16。空字串0。函式沒有maxlen／providerindexguard；DF0是ABI條件，不能把一般輸入說成一律0..15。

目前已封存source factory與format條件：PNG ordinal0..809、IMG0..7873，%d.png／%d[ecjh].img ASCII byte-sum保守上界542／697，故這些正確formatted短檔名的index0..15。外部formatter並未執行，class／sex／language enum、fileexistence不從這裡推定。

0.png、79.png、0.PNG均bucket3；19.png／91.png均bucket13。No explicitcasefold，但ASCIIcase差32使low4碰撞；這不授backingfilelookup的casepolicy。單純byteaddition也忽略字序，不是uniqueassetidentity或archive/memberrow。

Caller的6479E8+4*index只指向provider pointer cell。16個非負domain地址6479E8..647A24（end647A28）已核固定PE raw bytes，全部0；不是說Native live table empty、不是證allocatedarray extent。若泛用輸入產生負remainder，callee不會阻止caller讀base以下；這不屬目前短ASCIIdomain的授權。

第一個新阻斷：dynamic provider pointer及其backing source identity。本leaf沒有container/path/memberoffset；所以無法從hash推出LTsurf或其它namespace。42EA50 reader／provider writer一律未開，bankinitializer、五index、803filename搜尋未重做。

剩餘只需一筆qualified request → 已算bucket的acceptedprovider/backingfileSHA → memberoffset/length/encoding → originalblobSHA/尺寸。remaining-provider-evidence-contract.json明列最小資料與工具能力；classifier body已收斂，勿再包同一unknown。現有只讀磁碟工具在這個scope無可靠新asset可抽，得到上述來源契約後才可小量copy/header/hash。

Batch36原件保持。R527有限實接不打斷、Modern不改／不build／不test，Native/process/GUI不操作，0新asset/art/capture/wire，無server通知；64AFA0／lifecycle／三項GUI原因均未擴張。既有blockedpyccleanup不碰、不重試。這是有限source route契約，不是產品／GUI驗收。
