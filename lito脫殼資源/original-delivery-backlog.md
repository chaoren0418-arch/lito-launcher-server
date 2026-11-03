# 原始交付總待辦：整體未完成

最新有限階段完成，不代表原始 PE/OEP/IAT、動態初始化、資源綁定與 GUI 驗收全部完成。既有兩張背景RGBA已交付；本次再核兩個64AFA0直接writer及一層caller，具名scene與draw位置仍不足。

|原始交付|已交付／採用|仍缺的獨立證據|接收組別／邊界|
|---|---|---|---|
|PE/OEP／可攜執行檔|Snapshot 可解析、entryVA004F162A；37 embedded leaves 已交付。未交付可攜執行檔。|完整匯入與外部跳轉來源版本、loader/initializer 需求、修復後可重現的啟動驗收；OEP snapshot 不能取代這些證據。|登入器／PE 組；本組不改 PE/BIN、不執行 Native。|
|IAT／外部函式|standard imports0、import directory invalid；四個 priority API 為歷史 base＋本機 exportRVA 的有限比對。|Dump 同期 DLL 原始檔及 SHA；每個未解析 thunk/外部目標的模組、export/forwarder 與版本對應清單，才可重建 IAT。|登入器／PE 組；本機 DLL 不等於 dump 同期 DLL；不重跑既有 Winsock PASS。|
|Host／啟動初始化|64AD74 writer、64AD54 alias 與64AD70 port已分離；私有050104CD前提與整段實際執行未閉合。|已接受的050104CD前提/回傳契約，或已有同一期 host 初始化、alias選擇、owner與執行順序記錄。|登入器組＋監製；本次不展開私有body、不新增runtime capture。|
|Skill／class/order/metadata owner|32個明確class4 producer與176 metadata slots已交付；147非空/29NULL；早期T73缺S1E與後續單一S1E均不是owner證據。|同一期完整 S1E payload、class、order、metadata/cache owner及reset/teardown順序；class4 slots21/22/23/29/30/37/38/39 的實際producer證據。|登入器組＋Modern組；監製整合同期證據；不得把較晚frame存在提升為typed owner已驗收。|
|Native 圖形／skill pane 一致性|226 LTicon PNG、149 shared skill icons、933/934/935及座標/色表/層次契約已交付。|935 conversion邊界4F1593→7741AC20的同期模組/rounding契約；實際初始化LUT、pitch、signed offset、alpha/blit與對照像素驗收。|登入器／Native驗收組＋Modern渲染組；參考解碼與靜態色表不等於Native GUI parity。|
|LTicon 173..179|LTlist.ico宣告233，qualified source實際decoded226；七個ID缺少qualified原始member。|每個缺ID所屬來源版本、namespace、明確member row/offset/length/encoding及原始blob SHA，或已接受的缺失/替代政策。|資源組接收證據；監製決定替代政策；不跨版本猜圖、不掃全archive當成進度。|
|選角人物PNG／IMG79/80 provider|兩個bank建立、leaf ctor、filename route與Sprite/Sprite03 constructor stem已閉合；實際backing identity未閉合。|一筆已接受provider→實際檔案/來源SHA→精確member offset/length/encoding→原始blob契約，先選一個ordinal即可；matching stem不足。|監製接收獨立來源證據；資源組有來源後抽取；Modern組綁定；42E550/42E6D0/42EA50保持STOPPED；不逐批向下展開。|
|背景／class variant／動畫生命周期|四個按鈕、12表48states及6314/7482 raw/PNG/RGBA已交付；64AFA0兩個直接writer、owner數值檢查與一層S03/4AACF0 caller前提現已核對。|數值owner/factory/caller到具名子場景的已接受映射；495520結果到背景draw座標/transform/mode的契約；同版本initialized owner/flag lifecycle。class/variant語義及selected+3D啟動/到期/reset、timer/prefetch redraw仍需獨立契約。|Modern UI組＋登入器證據組；監製整合；直接writer已核，具名scene與draw位置仍null；STOP在495520/owner語義，不授Directory/sex，不追private下一層。|
|Map 候選到 runtime mapping|五個既有LTmapstr的完整token/row與離線候選index已交付。|Runtime key/formatter/private05040E1D與外部773F5200輸出對實際candidate來源/row的已接受binding記錄。|Modern map組＋監製；離線candidate不是已載入地圖；不重跑完整候選解析。|
|Modern 採用／真連線／GUI|Batch33/34有限source comparison及8個候選UI可達已交付；R526 D2/E1由監製報告完成，非本組重開缺口。|Modern組封存R527結果、目前實際採用/合併清單及default入口證據；完成受授權的真連線與GUI驗收後回交監製。|Modern組＋監製；本組未驗收R527完成，不編譯/改Modern、不干擾在途工作。|
|Server wire 接收|F3/F4已有adoption ack；後續有限wire/source契約交付且本次wire差異0。|只有發現新的精確wire差異才產生有opcode/欄位/byte witness的通知；真連線對比由監製/既有執行組提供。|監製＋伺服器組；沒有新wire不發通知；不承接伺服器被拒工作。|
|其餘 archive／未qualified格式|24 archives/305295 records範圍已驗證，594初始members/19loose＋後續限定輸出；不是全量GUI語義解碼。|具體consumer需求與可驗證namespace/member row/offset/length/encoding、缺少的輸出格式；有已qualified來源才做最小抽取。|資源組；Modern提出具體consumer契約；現在無其他已證明未抽取的必要member；不批量抽全部305295 records。|

背景有限契約見Native-background-scene-contract.json；其他原始待辦保持。無新wire，不通知伺服器。Private backing三支仍停止。既有scope-write事件保持open；先前外部pyc清理自動批准審查以blocked by policy拒絕，不重試、不转交繞過。
