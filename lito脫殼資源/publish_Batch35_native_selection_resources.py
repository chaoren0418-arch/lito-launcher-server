"""Seal finite source compatibility and Native-only resource gap handoff."""
import sys
sys.dont_write_bytecode=True
from audit import *
from seal_delta import seal
def load(name):return json.loads((ROOT/name).read_text('utf-8'))
def main():
    p=load('Native-selection-resource-gap-contract.json');primary=record(ROOT/'Native-selection-resource-gap-contract.json');v=p['verification'];review=load('Native-selection-resource-gap/finite-owned-evidence-review.json');folder=ROOT/'Native-selection-resource-gap'
    assert review['all_match'] and review['primary']==primary and v['prior_primary_pins_preserved']==36
    for q in p['outputs']+p['sources']:assert record(q['path'])==q
    for q in p['prior_primary_pins']:assert record(q['path'])['sha256']==q['expected_sha256']
    assert record(ROOT/'server-packet-delta.json')==v['server_packet_unchanged']
    (folder/'READ-ME.txt').write_text('''第三十五批：兩檔R526條件式相容附頁／Native選角原版資源有限缺口

主入口 ../Native-selection-resource-gap-contract.json。
R526-selection-compatibility-addendum.json：只讀 runtime.rs、source_send.rs 及監製指定 frozen manifest SHA514D978CAABECF04F20BDB03F941274E90274803FBEDFD32026E1C529C6FC82C；8個existingselection anchors完整行段與Batch34一致，含fullNpublication／bounded8controls／opaque choose+submit revalidation／rawname C9。Runtime frozen SHA A3BE905AECC0A8F62682D5C05E3969A87BAACE63D3811393EB8F2C7391476287；Sender frozen SHA51095E688463FC829049922364695D3566B42BD5ABF0C78916AB76630A7E3C5D。兩檔live讀取當下也與frozen一致，後續可變；沒重掃其他Modernsource、不要求freeze／不打斷R527。這是選定callchain的conditional source相容，不授wholeR526/R527compile／backend接線／GUIruntime。Batch34原件與20個原outputs不改。

Native-resource-handoff.md：可用原始檔／既有preview絕對path／SHA／dimension／indexrow／PAKoffset及12animationtable原始檔／fixedPEoffset。
Native-existing-resource-inventory.json：四個Native Enter/Back action51x23、6314與7482兩個Native640x480 IMG皆已存在抽取品，六個既有preview僅IHDR/hash核對，沒有新解碼或GUI驗收。
Native-source-container-offset-check.json：原LTsurf.idx C699CCF22C5F59EB37E8936D939D2632C15AC381980A6FE3F26F2565185193BF／PAK42E676C255B248C295A71097A53B1B67BF73A0C2C3DC0FE11D12BDB9EE79621E前後fingerprint一致。六筆index row/offset/name/size與既有manifest一致；source PAK exactsliceSHA與已抽取raw一致。不是新增抽取。

Native-animation-and-slot-data.json：四79 descriptor和12組animation data table共48states對fixedPE原始bytes一致；12×96+540 bytes，數值base_png/frame_limit/delay/draw_mode/dx/dy保留。Class/variant numeric association沿用R498metadata，不是本批新selectorbodyproof；不把非零variant叫sex，不推humanclass enum。保守base..base+frame_limit PNG查找envelope0..802共803names不是宣稱803requiredframes或inclusive semantics。沒有portraitimage尺寸/offset可授。
Native-background-branch-reference.json：既有496EB0 fragment中496F05..496F2C共40bytes/10instructions逐bytes對fixedPE一致，64AFA0非零push7482，零push6314，kind7/631EC8/631ECC進495520。STOP於call，不跟constructor/private/virtual/externalbody。兩圖確實Native資源，但此有限branch不足以指認Directory或其他character子場景，也不授flagwriter/lifecycle/hitgeometry。

resource-gap-classification.json明確區分：
已知indexed但未抽取=0；六筆已抽取，不用再extract。
namespace/bank未對應：79.img/80.img與PNGbank。五個既有Native index完整3882records均exactlookup無此目標；looseNativeLTSurf非遞迴只有8044.img，亦無目標。這不是LTsurfidx沒decode、不是全assetsabsence，也不是把同名SPR/TIL/LBT當image。
mappingunknown：79尺寸/80角色、numericclass到humanclass/sex與真實PNGentry、6314/7482到Directoryscene。四slotx19/176/333/490,y0只是descriptor，不證Nativecapacity4或8。
dynamicunknown：834BD0PNGbank初始化/ownership、64AFA0writer/sceneowner、selected+3Dh啟動/到期/reset與timer/prefetch/redraw。R498drawchain及lifecyclenote只作原有metadata，不追無限callee，不模擬timer授GUI。

minimal-Native-binding-handoff.json：能立即沿用的只有已sourcebound四個原始動作按鈕；保留目前完整0..8ownedlist/details/opaqueabsolutehandles/rawnameC9。背景二圖待phaseassignment，portrait/animation待bankentry及生命周期，仍不能稱完整原版選角資源binding完成。未用Classic或跨版本圖資替代，也沒造新圖；未解析offset者做guessrawextract無益，所以必要新增抽取=0。
R527監製準備真backend接線，本批read-only、不打斷／不改Modern／不開新task，不接server工作。

42 retained evidence read records前/content/後一致；不是total exploratoryreadoperation數。217finiteownedchecks是capturedmetadata／line spans／rawtablechecksums／dimensions／sourceoffset與immutablepins，沒有Modernfunction/model/test/build/compile/NativeGUIrun。36priorprimarypins和Batch34歷史輸出不變。14reusedfixedPEranges/1732bytes、10existinginstructionbyteschecked，0新code/data/raw/art/capture/wire/opcode/correction/Modernedit/branch/test。Packet B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變；无新封包差異，不要求重複server通知。
只寫自有ROOT，Python-B/dont_write_bytecode；無externalhelperimport、Native/input/injection/JavaSQL/provider/JVM、newunknowncallee或WORLD/bot/matching，沒重跑oldPASS/generator/review。
此前externalpyc cleanup被automaticapprovalreview以blocked by policy拒絕；scope-write-correction SHA B51ECB6CC2AD37DA279E03F71B92C6B75DA52F00027737A6AB8AC2A02C97DFE4仍open、不變。未碰／重試／改述／轉派／替代／繞過或查讀外部cache。有限資源交接完成後停止；原版完整人物動畫／背景scenebinding與GUI產品完成仍open。
''',encoding='utf-8')
    summary={'evidence':primary,'finite_native_selection_resource_inventory_complete':True,**v,'finite_owned_evidence_checks':review['check_count'],'whole_R526_R527_compile_GUI_acceptance':False}
    complete=load('completeness-audit.json');complete['Native_selection_resource_gap_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第三十五批Native選角有限資源交接：')[0]
    entry.write_text(txt+'\n第三十五批Native選角有限資源交接：Native-selection-resource-gap-contract.json + Native-selection-resource-gap/READ-ME.txt／R526-selection-compatibility-addendum.json／Native-resource-handoff.md／resource-gap-classification.json／minimal-Native-binding-handoff.json。只對2changedfiles補8個完整selectionspan相容；Batch34原件保持。6既有NativeIMG+6preview對pathSHA/dimensions，原LTsurfindex/PAK6offsetchecked；12animationtables/48states+四descriptor對fixedPE，10backgroundbranchinstructionbytes。79/80/portraitPNGbank未對應、class/sex與backgroundphase映射未知、dynamicselectedlifecycle未知；knownindexednotextracted0，不猜offset造图，不拿Classic替代。42retainedstable reads／217staticownedchecks／36priorpins，不授GUI/productcomplete。R527backendACTIVE只讀不打斷、0Modernedit/test/build/wire/servernotification；cachecleanup拒絕open。\n',encoding='utf-8')
    phase='BATCH35_NATIVE_SELECTION_RESOURCES_FINITE_SEALED_R527_BACKEND_ACTIVE_MAPPING_LIFECYCLE_GUI_CACHE_OPEN'
    h=load('handoff.json');incident=h['scope_write_correction'];assert incident==v['scope_write_correction']
    h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),Native_selection_resource_gap_contract=primary,finite_native_selection_resource_inventory_complete=True,Native_full_selection_UI_binding_complete=False,R526_selection_conditional_compatibility=record(folder/'R526-selection-compatibility-addendum.json'),Native_selection_resources_finite_handoff=record(folder/'Native-resource-handoff.md'),Native_selection_resource_unknowns=record(folder/'resource-gap-classification.json'),Native_selection_minimal_binding=record(folder/'minimal-Native-binding-handoff.json'))
    added=[primary]+[record(q) for q in sorted(folder.iterdir()) if q.is_file()]
    for group in ['modern','launcher']:
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k] for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json') if q['path']==str(ROOT/'completeness-audit.json') else q for q in h['shared']]
    h['server']['read_only_Native_selection_resource_handoff']=primary
    h['next_steps']=[
        'Batch35 finitehandoff sealed:only2Modernfiles andR526frozenmanifest;8existingselection anchoredspans unchanged,liveatreadsameSHA. Batch34 historicalprimary/20outputs preserved. No crossfileR526/R527compile/backend/GUIacceptance,nonew source/rescan/freeze/interrupt.',
        'Nativeoriginalfour51x23actionassets source-bound,plus6314/7482Native640x480existingassets/previews. Six row/offset/name/size/sourcePAKSHAchecks matchoriginalLTsurfidx/PAK. Reusefouractionbuttons+full0..8ownedlist/details/currentopaque/rawnamecommandpath;do not reduceproducerorclaim4/8Nativecapacity.',
        'Two640x480backgroundassets areknownnativefiles andnumeric496F05branch isfixedPE40bytesverified;Directoryversusothercharacterphase/64AFA0writer/geometry remainsunknown. Do notbindtoDirectorysolelyfromdimensions/name. NoClassiccrossversionreplacement/newart.',
        'Knownindexedbutunextracted0. FivecompleteNativeindexes3882rows+exactlooseLTSurf scope lack79/80IMG andconservative0..802PNGlookup. Resource bank834BD0namespace/offset/format unresolved,notallassetsabsence; no arbitrarynewrawextraction. Humanclass/sex/80role/79dimensions mappingunknown;nofilenamederivedidentity.',
        '12fixedPE96byteanimationtables/48numericstates+540bytefour-slotdescriptor preserved;class/variantassociation isexistingR498metadata,notnewselectorbodyproof. Stateorder/frame_limitendpoint/selected3Dhstartup-expiry-reset/timer/prefetch/renderlifecycleunknown. Stopatexistingcalls,nonewunknown/private/externalbodyorWORLD/bot/matching expansion.',
        '42retainedreadrecords individuallystable,217finiteownedchecks,36priorpins;14reusedPEranges1732bytes/10existinginstructionbyteschecked. Newraw/code/data/art/capture/wire/opcode/correction/Modernedits/branches/tests/buildGUI0. R527realbackendpreparationreadonlyuninterrupted,noserverwork/newtask/norepeatedservernotification;packetB4C00BF1 unchanged.',
        'Prior externalpyccleanup automaticapprovalreview blocked bypolicy remainsopen withsameincidentpin;no touch/read/retry/reframe/transfer/alternate/bypass. Finiteinventorycomplete;Nativefullselectionresourcebinding/lifecycle/GUIfacts/productcompletion remainopen,notdocumentationquantity completion.'
    ]
    assert h['scope_write_correction']==incident and h['server']['packet_delta']==v['server_packet_unchanged'];write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch35 finiteNative resource handoff sealed:2file/8selectionspanR526conditionalcompatibility;6existingIMG/sourceoffsets,12animationtables;79/80PNGbank/mapping/lifecycleGUIopen;R527uninterrupted/cacheopen.',Native_selection_resource_gap_summary=summary)
    progress['handoffs']['modern']='Native-selection-resource-gap-contract.json + R526-selection-compatibility-addendum.json + Native-resource-handoff.md + minimal-Native-binding-handoff.json:4actionassetsreusable,2backgroundphasecandidates,portraitbank/mapping/lifecycleopen;fullNdata retained;R527readonly/notinterrupted/noGUIproductclaim.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['Batch35_native_selection_resources.py','finalize_Batch35_native_selection_resources.py','review_Batch35_native_selection_resources.py','publish_Batch35_native_selection_resources.py','Native-selection-resource-gap-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT)) for q in folder.iterdir() if q.is_file()]
    n=seal(names,Path(__file__));print(json.dumps({'primary':primary,'changed_files_checked':n,'delivery':record(ROOT/'delivery-verification.json'),'priorpins':36,'incident':incident},ensure_ascii=False))
if __name__=='__main__':main()
