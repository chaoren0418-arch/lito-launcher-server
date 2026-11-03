"""Seal Batch30 direct-source evidence and time-scoped version supplement."""
import sys
sys.dont_write_bytecode=True
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-ready-guard-source-contract.json');pin=record(ROOT/'CD2-ready-guard-source-contract.json');r=load('CD2-ready-guard-source/independent-review.json');v=p['verification']
    assert r['all_match']and r['primary']==pin and(r['check_count'],r['decoded_instruction_rows'],r['model_count'])==(395,140,25)
    assert(v['new_code_ranges'],v['new_data_ranges'],v['necessary_reused_raw_ranges'],v['instruction_assertions'],v['prior_primary_pins_preserved'])==(6,3,9,60,31)
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']+[r['models']]:assert record(q['path'])==q
    for q in p['prior_primary_pins']:
        a=record(q['path']);assert a['sha256']==q['expected_sha256']and a['bytes']==q['bytes']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'CD2-ready-guard-source/READ-ME.txt'
    reader.write_text('''第三十批：C_D2 state1 guard 的直接來源／485F 時間差版本補證

主入口 ../CD2-ready-guard-source-contract.json；source-width-trigger-and-unknown-matrix.json列sourceSHA/VA/RVA/fileoffset/width/trigger/phase/等級/缺口，direct-source-xrefs.json是有限ASM named operand導航，ranges.json是固定PE逐byte依據。
固定PE7094272 SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9；6newcode/3data/9necessaryreused slices，60operand assertions、140decoded rows、395newchecks、25sourceconditionalmodels全match；31priorprimarypins未改。
先採既存CE/SD0/CDD selector讀值、D1/E1 phase/alias及D5fieldstore契約，以及S0C/S4D91byteprotectedsetter。這些舊consumer不是初始化或語意證據，未重跑舊PASS/model/generator/review。

BYTE64AF9D直接來源：
本固定PE VA64AF9D/RVA24AF9D raw00，只是snapshot，不授loader初始化已執行或runtime目前值。
NamedASM inventory11directreads、1directwrite、1definition。唯一named直接write：481447 movBYTE[64AF9D],1。
481410取EBP-1C pointer，481413 literal62D25C ASCII lteventsvr NUL；inline兩byte循環比較，equal→48143A EAX0→481443 test→481447 set1。Mismatch→481453跳過write，沒有clear。
Source conditionalstage：480C4D tokenpointer，首BYTE '/'或'-'到480C8D，ESI=EDI+1，480C9C存EBP-1C。480C0D tokenizer47EC50未知STOP；480C9F間接callEDI（取自DWORD4F72E4）第一unknownSTOP。未跨其success/earlier比較鏈去斷言整段option已被實際接受。
這是ClientApplication_Run startup literal-match本地flag；沒有phase64AF98/room/mode/owner/readygate。不能命名serverevent狀態或ready權限。
有限namedliteral集合沒有clear寫入，不等於永不reset；alias/bulk/runtime/其他初始化仍未知。Modern不能自動在room或ready轉換清除該flag，也不能把fixedraw0當當前flag0。

XOR selector寬度與初始化缺口：
Object8316B0(indexDWORD)、8316B4(pointerDWORD)、8316B8(keyDWORD)，reader index=DWORD[B0] XOR B104FA87；result=DWORD[DWORD[B4]+index*4] XOR DWORD[B8]，full32bits只測zero。
三DWORD固定PE raw全00，pointer0、encodedindex0不能當有效runtime decode（XORindex會B104FA87）。沒有本地object/storage/index guard；禁止從snapshot合成value0/owner/player。
ASMdata標B104FA84/057E0D48/2FE3681D是不同recovered初始化，與固定PE不相等，只保留導航，不採其index或pointer為本版本runtime事實。
4C3760 directcall4C3770；4C3770傳ECX8316B0，4C3775 JMP4C3CC0第一unknownSTOP，constructor/allocation/storage成功与actualstartupinvoker未知。
4C3780 pushcallback4C3790，4C3785 CALL4F10E0第一unknownSTOP；4C3790傳同object，4C3795 JMP4C3D20第一unknownSTOP。不展開其body、不猜註冊成功、destructor或清除已執行。
NamedB0共有45directreads/4addressargs/1definition，B4/B8各47directreads/1definition，沒有named直接globalstore。不是alias/ECXobjectcallee/任意objectinstance/fullprogram write closure；其他reader包括WORLD只導航，不開body。

本地selection的常值overwrite request：
4928C0是既存ordinary characterselection UI callback，不是roomready packet。Entry4928E8→4C8790第一unknownSTOP；rowquery492914→494120第一unknownSTOP。
其後條件島492919..49295D：EAXindex不是-1時，LEA*5、*9、*4，EBX=62FAA0+index*180；記錄一些BYTEguard後492958→49A510第一unknownSTOP。只授record地址表達式，indexbounds/recordproducer/生命期未合格。
另一獨立join島492E6B push0，ECX8316B0，492E72→既存protectedsetter4C3E90；這是request fullDWORD0。
之後492E85讀BYTE[EBX+3E]，zero跳492EB0；nonzero則492E8E push1、492E93同object、492E9E setter。Compare7只決定另BYTE64AFBF的SETZ，不是setter1門檻。
因此record+3E==0有setter0 request；任意nonzero（含1/7/FF）另有setter1 request，不能把+3E擅稱owner/mode/ready或直接視為線上packetfield。
沿已封存4C3E90 91bytes，argument32bits XORkey寫selectedslot並存encodedindex；只有validobject/storage/helper/externalreturn/index條件下成立。Firstunknown4C3E97/4C3EA1→4C3CA0，external4C3EB6→773DC650仍STOP，沒有新展開、randomkeybody或執行。
第一次setterreturn、selectedrecordread及第二settercompletion可失敗；不能把常值request或正常source順序當初始化/值已commit/GUI接受。

Modern ready writer邊界：
沿Batch29 46B6F0 guard：flagBYTE0直接跳46B74A D201而不讀selector；flagnonzero需要validselector；decoded0同直接D201，nonzero到46B726→458BB0第一unknown，再只有獨立callbackregistration島。
此59byteguard沒有讀numericphase/version/room/owner，僅該有限段，caller資格仍未知。BYTEstartupflag與protectedlocalrecordselector都不能給owner/ready/mode/transport授權。
KnownD201 body2/frame4沿Batch29不變；此批不新增wire。Explicitauthorizedlocalintent與currentepoch/room/join/alias另設，serverreadycommit/D1projection/E1countdown/WORLDgrant/NativeGUI不能混用；不得製造XORobject作Modernready權限。
CurrentNativeopcode-only、actualUIcallback/click、transportexternal759058A0、D1alias/private/table及E1expiry→WORLD缺口保持先前契約。

新版本增補（不回寫Batch29）：
Batch29主契約36083 SHA F3867EC1B86DA3DF66A7493E92CD239E40AEC59B658EC0B588F463EFF13A2667與當時485Funit-only敘述保持原值。
本批採監製接受server-ordinary-room-settings-candidate-network.json 16155 SHA8C1776AA503098E09B42EFE8CA45E07E651DD4570A07CB6A8994006421CCEF41。
ActualJAR485F599DA9BA1A13504CB48DDCA3D53C6CAD91D7BAFDF738D104E65A05E1DFD5。只新增singleordinaryauthenticatedWAITING settings network等級：extended設options[2,1,7,9]/selections[3,6,8]/mode1/map514，compact改commonfields但保留上述值。
每owneredit在Guestready後逐peerGuestD1state0→D5summary0/config1；nonowneredit只selfsnapshot，acceptedindependentstate未改且Guestready保留。不是D5ACK或allpeeratomicity。
只採report/JAR/unitreport/productclasspins及settings_result/必要findings摘要，沒有讀新frames/cipher/proof/PASS重跑或JavaSQLrun。
這不是485F ready-state-machine動態、COUNTDOWN/RUNNING/WORLD/GUI或一般race驗收；舊AD368readycancel、C7map與485Funit各有版本scope。networksettings不會給Nativeguard賦義。

Newwire/newopcode/wirecorrection/newcapture/newframeadoption均0；server-packet-delta.json26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C未改。
本批只写lito脫殼資源，Python-B與sys.dont_write_bytecode=True；沒有Native/GUI/input/injection/SQL/provider/JVM/Java執行/Modern/BIN變更，沒有unknownprivate/allocation/virtual/externalbody或WORLD/bot/matching擴張。
Batch28 externalpyc清理auto-review拒絕blocked by policy保持open，scope-write-correction.json SHA B51ECB6CC2AD37DA279E03F71B92C6B75DA52F00027737A6AB8AC2A02C97DFE4不變；未重試、改述、替代機制或轉派。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_CD2_ready_guard_direct_sources_complete':True,**v,'independent_checks':395,'decoded_instruction_rows':140,'new_conditional_models':25,'runtime_selector_UI_authority_open':True,'named_flag_direct_writes':1,'named_flag_direct_clears_found':0,'selector_constant_setter_calls':2}
    complete=load('completeness-audit.json');complete['CD2_ready_guard_source_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第三十批C_D2 ready guard source：')[0]
    entry.write_text(txt+'\n第三十批C_D2 ready guard source：CD2-ready-guard-source-contract.json + CD2-ready-guard-source/READ-ME.txt + source-width-trigger-and-unknown-matrix.json。64AF9D raw0，唯一namedwrite481447在startup lteventsvr literal比對equal時設BYTE1，無namedclear/fullruntime資格；8316B0/B4/B8 fixedraw全0與ASM常值不同，初始化/清理4C3CC0/4C3D20/registration4F10E0 STOP。既存selection492E72 setter0、recordBYTE+3E nonzero→492E9E setter1只是localrequest，非owner/ready/mode授權。6code/3data/9reuse/60assert/395checks/25models/31priorpins。另採accepted485F WAITINGextended→compact/nonownersettingsnetwork report8C1776AA，保留Batch29unit-only當時封存原SHA，不授readydynamic/COUNTDOWN/WORLD/GUI。newwire/capture0/packet不變，cache清理拒絕仍open。\n',encoding='utf-8')
    phase='BATCH30_CD2_READY_GUARD_DIRECT_SOURCES_SEALED_RUNTIME_SELECTOR_UI_AUTHORITY_CACHE_CLEANUP_OPEN'
    h=load('handoff.json');incident=h['scope_write_correction'];assert incident==v['scope_write_correction']
    h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),CD2_ready_guard_source_contract=pin,finite_CD2_ready_guard_direct_sources_complete=True)
    added=[pin]+[record(q)for q in sorted((ROOT/'CD2-ready-guard-source').iterdir())if q.is_file()]
    for group in ['modern','launcher']:
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if q['path']==str(ROOT/'completeness-audit.json')else q for q in h['shared']]
    h['server']['known_CD2_ready_guard_source_boundary']=pin
    h['next_steps']=[
      'Batch30 finite directsources sealed:BYTE64AF9D snapshot0;unique namedwriter481447 set1 ifinline lteventsvr equality. SourceEBP-1C←EDI+1 afterslash/hyphentoken, tokenizer47EC50 andindirect[4F72E4] firstunknownSTOP. Localstartupflag only;noowner/mode/ready semantic orruntimevalue upgrade;no namedclear isnotalias/bulkabsence.',
      'Protectedselector8316B0/index,B4/storageptr,B8/key areDWORDs;fixedsnapshotall0 differsASMdefinitions,invalidlive decode. Init4C3775→4C3CC0,registration4C3785→4F10E0,cleanup4C3795→4C3D20 firstunknownSTOP. Runtimeinvocation/storage/key/index/helpers unqualified;no body expansion.',
      'Existingcharacterselection conditionalrecord62FAA0+index180:entry4C8790,rowquery494120 and49A510 firstunknownSTOP. Later492E72 setterrequest0;BYTErecord+3Enonzero→492E9E request1;cmp7 onlyseparate64AFBF. Existing91byteprotectedsetter conditionalfull32bitstore reused,4C3CA0/external773DC650 STOP. No fullUI/ownercreation/recordfieldmeaning authority.',
      'Readyguard flag0 skipsselectormemory andreachesD201;nonzeroflag+decoded0 same,decodednonzero notice458BB0 unknown. Thisfinite59byteguard hasno phase/version/room/ownerread;caller/actualUI/transport remainunknown. Modernuseexplicitauthorizedlocalintent andqualifiedcurrentepoch/room/join/alias;do not manufactureXORobject orflag asready/owner permission. Servercommit/D1/E1/WORLDgrant remainseparate.',
      'Time-scopedversion supplement adopts accepted485F WAITING extended→compact→nonownersettingsnetwork report8C1776AA:options2,1,7,9/selections3,6,8/mode1/map514 preserved,perpeerD1state0→D5/0,1;nonownerindependentstateunchanged. Batch29 F3867EC1 unit-only textandSHA immutable. No485Freadydynamic/COUNTDOWN/RUNNING/WORLD/GUI/generalrace transfer;no newframe/cipher/PASS replay.',
      '31priorpins;6newcode/3data/9reused slices/60assert/140decodedrows/395checks/25newconditionalmodels;155namedcode xrefrows+4definitions. Newwire/opcodes/corrections/capture/frameadoption0,packetunchanged. OnlyownedROOTwrites,Python-B/dont_write_bytecode;noNative/GUI/input/injection/SQL/provider/JVM/Java/Modern/BIN orunknownbodies/WORLD/bot/matching actions;finitebatchstop.',
      'Prior externalpyc remainsopen:cleanup automaticreview rejected blocked bypolicy;exact scope-write-correction pin preserved. No retry/reframe/alternative/delegation andall existingintercepts retained.',
    ]
    assert h['scope_write_correction']==incident and h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch30 directstartupBYTEflag andlocalprotectedselector constantcaller sources sealed;runtime/recordmeaning/UI/authority open;accepted485FWAITINGsettingsnetwork supplementedwithoutchangingBatch29.',CD2_ready_guard_source_summary=summary)
    progress['handoffs']['modern']='CD2-ready-guard-source-contract.json:startup literalflag andlocalrecord+3E-conditioned setter0/1 requests are notready/owner/mode authority;initialization/runtimevalue/UIunknown. Accepted485FWAITINGsettingsnetworksupplement distinct fromready/COUNTDOWN/WORLD/GUI.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['CD2_ready_guard_source_audit.py','review_CD2_ready_guard_source.py','publish_CD2_ready_guard_source.py','CD2-ready-guard-source-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT))for q in (ROOT/'CD2-ready-guard-source').iterdir()if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),'priorpins':31,'packet_unchanged':packet,'scope_write_correction':incident},ensure_ascii=False))
if __name__=='__main__':main()
