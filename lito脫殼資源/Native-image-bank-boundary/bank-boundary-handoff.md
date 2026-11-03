# Batch36：Native 圖片 bank 的直接來源與阻斷

已證834BD0是PNG bank，79slot基本圖片入口使用另一個834BD4 IMG bank；初始化／ordinal／vtable／lazy-read入口已找到。尚無新的可抽取asset，阻斷收斂到private resolver/provider到container/member的來源契約。

|bank|直接initializer／global寫入|pointer array request／entry count|entry構造／lazy-read|
|---|---|---|---|
|PNG834BD0|4D4A50／4D4A8A `[EDI]=EAX`|0CA8 bytes／810；entry0C bytes|4D5220→共享4D4D40，vt4FA738→4D5270|
|IMG834BD4|4D4940／4D497A `[EDI]=EAX`|7B08 bytes／7874；entry0C bytes|4D4D40，vt4FA734→4D4D90|

以上是靜態allocationrequest與conditionalsourceflow，不是已執行runtimebank，也不是角色資料容量。IDA endp在allocatorcall後提前結束，rawbytes保留reachablepost-endpstore/loop。直接clear writer為4D4B39(PNG)、4D4A29(IMG)，只核該單一statement，不擴張teardown生命周期。

numericPNG：415DA0對24byte/state取base，加+38frame與可能+3E的1，EDI進mode0／1的834BD0[id*4]。上界compare32A；payload+4為zero時callvtable[0]。沒有追statehelper415D70或class/sex/timer。IMG：414910由inputimageID（79）經834BD4[id*4]到payload/virtualread。

PNG entry+8 ordinal與literal `%d.png` 在4D5293..4D52EE送往formatter，再43FFC0(filename)得EAXrouteindex；6479E8[EAX*4]provider的42EA50讀取失敗／null則以64798C defaultprovider重試。

IMG selector64B6DC的jump table為1 `%de.img`、3 `%dc.img`、4 `%dj.img`、5 `%dh.img`；2／其它fallback `%d.img`，同類route/defaultprovider讀取。四個新查找僅79e/c/j/h.img，一份LTsurfindex，均無命中；沒有重查plain79或五index／803PNG。這些source-literals不是filename推class/sex或推live語言。

固定disk834BD0/4=0，64798C=0，僅6479E8第一bucket=0；不能說整provider表zero。ASM中的歷史heapvalues不能替代currentrawobjects，也不能被dereference。沒有開Native／process／GUI或newcapture，沒有把此資源缺口當作三項GUI失敗原因。

精確STOP：externalformatter773F5200、private43FFC0、dynamicprovider6479E8[index]/64798C、private42EA50。未跟allocator4F118C、decoder4B8B10、renderer/logger/exit或任何更深callee。最多directinitializer與必要entrybridge；class/sex、64AFA0背景、selectedanimation生命周期均未擴張。

最小剩餘證據：給一筆actualrequest(bank/ordinal/filename+sourceSHA/VA)、qualifiedprovider到existingcontainer/loosefile的identity+SHA、memberoffset/length/encoding、originalblobSHA與尺寸。minimum-missing-resource-contract.json列完整欄位與工具可取得性。現有磁碟檔工具可在這種來源契約成立後小量複製／驗hash與header；本scope尚無offset或blob可取，不能猜extract。Native/sourceGUI/renderer／classsex／lifecycle驗收仍分開。

Batch35原件保持；只增加直接sourcefacts和精確contract，不再反覆包同一unknown。R527保持只讀、不打斷，0Modernedit/build/test/GUI、0新asset/art/capture/wire/servernotification。
