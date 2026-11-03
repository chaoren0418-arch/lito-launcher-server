"""Seal finite Batch28 mode semantics, C7 map version evidence and scope deviation."""
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD1-mode-step-contract.json');pin=record(ROOT/'CD1-mode-step-contract.json');review=load('CD1-mode-step/independent-review.json');v=p['verification']
    assert review['all_match']and review['primary']==pin
    assert (review['check_count'],v['new_code_ranges'],v['new_data_ranges'],v['necessary_reused_raw_ranges'],v['instruction_assertions'],v['prior_primary_pins_preserved'])==(248,7,0,3,21,29)
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']+[review['computed_models'],review['scope_write_correction']]:assert record(q['path'])==q
    for q in p['prior_primary_pins']:
        a=record(q['path']);assert a['bytes']==q['bytes']and a['sha256']==q['expected_sha256']
    incident=load('CD1-mode-step/scope-write-correction.json');assert not incident['cleanup_completed']and incident['automatic_approval_review_rejected_cleanup']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'CD1-mode-step/READ-ME.txt'
    reader.write_text('''第二十八批：C_D1 subtype2 mode-step 有限caller／Java語義與C7版本補充

主入口 ../CD1-mode-step-contract.json；wire-contract.json提供字段/length/端序/觸發條件/來源SHA/VA-RVA/等級/未知；native-caller-and-unknown-matrix.json列Native缺口及Java發布邊界。
固定PE7094272 SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9，base400000；raw ranges含VA/RVA/fileoffset/hex/逐指令。
7新code、0data，3必要既存raw引用，21operand assertions。246技術checks加2範圍紀錄checks共248全match；29priorprimarypins保持，Batch27原契約不變。
6個Javaclasses／12必要methods離線read-as-data，13 exact source/archivedIL slices；兩個新conditionalcaller模型、12新relativecycle模型。已合格sender/cipher/D5effectmodels不重跑。
沒有Native/GUI/input/SQL/provider/JVM/Java classloading/新capture或Java/Modern/BIN變更；無unknown Nativehelper bodies擴張、被攔截工作接手或Modern scratch刪除。檔案寫入偏差及未完成清理見末段，不能宣稱本批歷程全程僅寫ROOT。

Native input與sender沿Batch27直接引用
46B7D0 inputECX0→46B9E6 step2；inputECX1→46B9E2 step1；46B9E8 pushsubtype2，46B9EA pushD1，46B9EF literal5028CC ccc，46B9F4→4C6AE0，RVA0006B9F4。
C方向3BYTE：D1 / 2 / directionBYTE；body3/frame5，既存sender總長u16LEprefix5。沒有roomID、absoluteMode值或字串codec。
必要0/1分支內未讀Nativephase64AF98、version63D54C或owner；限定此有限分支，非全client無權限檢查結論。
實際caller/click、inputECX交付、room/epoch/owner/UI接受未知；actualtransport仍沿Batch26 external759058A0 STOP，非send/delivery/ACK。

新增的conditionalcaller控制參數與首個缺口
固定ASM三個callbackpointer候選，僅control0/1兩個必要caller準備島；第三control2 menu不展開。
46D09E..46D0BE→4154E0參數stack順序[sprite187F,x1A7,yF2,callback46B7D0,control0,last0]。
46D0EC..46D10C→4154E0參數[sprite1881,x246,yF2,callback46B7D0,control1,last0]。
只授固定callbackpointer與參數位置，不靠sprite/README名稱猜left/right或實際control語義。
上游46D088／46D0D6 call4F118C allocation是第一個unknown，STOP；callerargs是其後獨立條件島，不跨越allocation成功/物件memory資格。
4154E0 prefix讀EBP+14 callback與EBP+18 control，沿六stackargs準備至4154FE call414910第一unknown，STOP。没有callback存址、virtualclick/reentry、control變成ECX的完整資格。
另46D0CF／46D11D→41B3F0準備parent64AD04/index0或1/conditionalEAX，directhelper未知STOP；不展開insert/helper/parentownership。
因此完整UI event→mode-step仍缺證；這次未沿414910、41B3F0或4F118C追body。

Java parser與相對循環：由actualclass與CPsymbols證明
Java-bounded-class-methods.json記AD368/C7 JARpins、classentry/bytes/SHA、methodsignature/fullcodeSHA、有限BCI片段/hex/CPsymbol。
Protocol、Direction、Mode wholeclasses相同；ClientSession/GameHub class整體不同，但12個所選methods exact code bytes和解出的CPsymbols都相同。只授所選methods，不宣稱wholeclass/lifecycle等價。
Source slices用凍結CS99DF8581...及Protocol A3D5498A...／BattleRules571638CC...／GameHub98E03203...引用，CS/GH/Protocol archivedIL各有exact行號/SHA；參考source不冒充所有C7凍結source。
Actual Protocol.litoGameInfoRequest讀D1→sub2→remaining恰1→readBYTE→Direction.fromWire。FromWire lookupswitch1→NEXT、2→PREVIOUS，其它值（含0/3）throw；malformed parse在ROOMguard之前。
Direction syntheticSwitchMap必要尾段證NEXT→case1、PREVIOUS→case2；Mode.stepNative guardvalue0..5，建立order{0,1,5,2,4,3}，deltaNEXT+1／PREVIOUS-1，Math.floorMod6後fromWire。
這是相對presentation cycle，不能把第三BYTE1/2當absolute mode1/2。order是該Java版本的明確政策，不推Native UI/runtime排序已執行。
Town6在ordinary mode stepping之外；CS讀option0&7F→Mode，!isBattleScene則selfsnapshot並return，不用request嘗試指定Town。
12個pure arithmetic symboliccycle模型皆computed，無JVM/classloading/Java runtime；新method證據是read-as-data，沒有重跑先前Javaunits或mode矩陣PASS。

Ordinary處理與D5/2、0/1、3關係
ActualCS gameInfo先parse，phaseROOM/currentRoomId!=0後才dispatch，rentalbranch在ordinary前且本批排除；LitoModeStepRequest.direction傳changeRoomMode。
Currentname/password/cap/options1..4/selections保留；replacement設新mode wireID並依teamBased更新attributes EQUAL_SIDESbit32。
ActualGH updateSettings gates：room存在、exactowner、notbattleStarted、combatants<=nativecapacity。不將這些servergate轉成Native權限資格。
Normal update true後CS BCI142 resetRoomReadyAfterSettingsChange；151 litoRoomMode，158 decode(replacement)取得defaultmap，172 litoRoomMap；每peer BCI236 mode(D5sub2)再250 map(D5sub3)。有限prefix止256，不追bot/matchmaking後續。
Readyhelper只對已ready members設false並各發D1state0；不能從D5推定所有ready清除。
!isBattleScene或updateSettingsfalse各在BCI31/137呼叫selfsendGameInfoSnapshot：summary0後configuration1，沒有在這些reject分支授D5sub2成功。
updateSettings本身settings/selectedmap/ready寫入在decode/reconfigure之前；exceptions/races/partialsend維持未知。C7 map-specificupdateMap順序修復不會自動資格mode/updateSettings失敗原子性。
Normalperpeer2→3、rejectself0→1都是source/actualbytecode條件結論，不是動態mode成功／ACK／allpeercommit或完整函式完成保證。
S_D5sub2格式D5,u8sub2,u32LEroom,u8absoluteMode，body7/frame9；sub3格式D5,u8sub3,u32LEroom,u16LEmap，body8/frame10。方向step與回覆absoluteMode是不同欄位。
Batch24 Native sub2只寫64ACCA低BYTE，保留64ACCB與map64ACC9，沒有UIedge；sub3獨立WORD→BYTEprojection。Batch23sub0/1獨立gate與store；沒有aggregate ACK或atomicpair。原receive報告只引用不重跑。

新版本表：原Batch27保持不可變
version-evidence-table.json與C7-saved-map-frame-witnesses.json補充：
AD368 original SHA AD368F49CF47E37A76A954AAFAAE1962AB2AB761057AAD4EE3145F7BBFD71219：舊static restore firstmap564 versus selected514、舊七capturehits0，只屬其原scope。
0612833B restoreparent SHA0612833BED929B72B5AD37D19CEEA88848A8CEED1C6797957AEF021A0936C17B：沿新report記其source/eightunits，未在本批或C7networkbatch啟動。
C7 SHA C7B169C00955B444BCDB357E6A63625A87AABC5B23F8F3A21E75988D8270D43A：已驗收singleWAITING ordinary map/auth網路；mode必要12methods static與AD368一致。
server-ordinary-room-map-network.json SHA3BBAA920A2B379B7562A38C3FF2CADC8842B98F7033C3A5C103174F4A28A8545，frames16427 SHA1FA280195BCA621AD7AB16A6E2B07CC8AABFD026FD92AE480FFA1396F3D52A26。
僅精確採用arrayindices73..90這18既存rows；沒有cipher驗證重播、recapture、JVM/SQL/provider動作或全capture重掃。
73 ownerC_D1sub3 map514→74/75兩peerD5sub3 WORD514。
76 Guestready1；79 ownermap509→80/81 Host D1state0再D5WORD509、82/83 Guest D1state0再D5WORD509。
84/86 owner/Guest FFFF→85/87各selfD5sub3 current509。
88 Guestvalid514→89 selfD5summary0 current509、90 config1，無map514commit（採用已驗收wire/state報告資格）。
這是server test-client動態map證據；Native GUI／mode-step D1sub2動態witness仍0。舊hits0不能再寫成目前整體無mapdynamic；舊564 restore不能冒充C7 current509。
Quietpeer與單casebefore/after仍非wireACK或一般race/atomictransaction保證；COUNTDOWN/RUNNING/disabledmapmode/network失敗未知。

給監製的精確缺口，非執行或新增Java包請求
要補dynamic mode需版本pin下C_D1sub2 NEXT/PREVIOUS與before/aftermode/map，readyperpeer→D5sub2/sub3，及Town/nonowner/gatefailure self0/1區分；actualUI仍另需414910/callback/constructor資格。
本批不自行啟Java/SQL、不要求新增opcode/封包、放寬domain或Modern中斷receive；mode/parser/response layout均既知補證。C7是採用已接受版本補充，不新增wirecorrection。
Newwire/newopcode/wirecorrection/newcapture0，server-packet-delta.json 26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變。

寫入範圍偏差與清理未完成
Python importlib載入唯讀ClassFileReadOnly.py時，2026-10-06 07:03:44在該server來源目錄產生ClassFileReadOnly.cpython-312.pyc（11746bytes），屬本次不應發生的externalcache寫入。
scope-write-correction.json記exactpath/SHA、原因、來源檔未改動、兩份owned腳本已設sys.dont_write_bytecode=True。
移除該cache的PowerShell操作被自動審查拒絕，工具僅報blocked by policy；未執行刪除，也未用其它方式繞過，cache目前保留，cleanup_completed=False。
這個範圍偏差不改變固定PE/Java source/JAR或契約技術checks，但未完成cleanup必須在主契約/handoff/監製進度明示。
其餘保留交付產物均在本組ROOT，29priorpins與原27不變；有限Batch28封存即停，UI/mode dynamic/transport/authority及cachecleanup仍open。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_mode_step_caller_Java_semantics_complete':True,
      'UI_mode_dynamic_transport_authority_guard_retained':True,'new_code_ranges':7,'new_data_ranges':0,
      'necessary_reused_raw_ranges':3,'instruction_assertions':21,'technical_checks':246,'scope_record_checks':2,'independent_checks':248,
      'bounded_Java_classes':6,'bounded_Java_methods':12,'source_IL_slices':13,'new_caller_models':2,'new_relative_cycle_models':12,
      'saved_C7_map_frame_rows_adopted':18,'mode_step_dynamic_witnesses':0,'Native_dynamic_witnesses':0,
      'C7_map_dynamic_evidence_adopted':True,'prior_AD368_7captures_absence_is_version_scope_only':True,
      'prior_primary_pins_preserved':29,'new_capture_frames':0,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'packet_modified':False,
      'Native_GUI_SQL_provider_Java_runtime_Modern_BIN_modified':False,'old_PASS_or_capture_replayed':False,
      'Java_new_packet_or_dynamic_run_requested':False,'unintended_external_python_cache_cleanup_open':True,
      'scope_write_correction':review['scope_write_correction'],'automatic_cache_cleanup_review_rejected':True}
    complete=load('completeness-audit.json');complete['CD1_mode_step_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第二十八批C_D1 mode-step：')[0]
    entry.write_text(txt+'\n第二十八批C_D1 mode-step：CD1-mode-step-contract.json + CD1-mode-step/READ-ME.txt + native-caller-and-unknown-matrix.json。Callerconditionalcallback46B7D0/control0/1→4154FE firstunknown414910，allocation/insert缺口保留；46B9F4 D1/2/BYTEdirection body3/frame5，Java1NEXT/2PREVIOUS relativeorder0,1,5,2,4,3，12methods AD368/C7 code+CPsymbols一致。Normalreadyreset→perpeerD5/2then3；Town/updatefalse self0/1，非ACK或mode dynamic。新versions採C7 acceptedWAITING mapframes73..90 real514→509/FFFFcurrent509，舊AD368restore564/7hits0只舊scope，原27不變。7code/3reused/21assert/248checks/29priorpins，newwire0/packet不變；scope-write-correction.json記externalpyc誤寫、清理policy拒絕仍open，腳本已禁bytecode寫入。\n',encoding='utf-8')
    phase='BATCH28_CD1_MODE_STEP_STATIC_C7_MAP_VERSION_SEALED_UI_MODE_DYNAMIC_CACHE_CLEANUP_OPEN'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),
      CD1_mode_step_contract=pin,finite_mode_step_caller_Java_semantics_complete=True,
      scope_write_correction=review['scope_write_correction'])
    added=[pin]+[record(q)for q in sorted((ROOT/'CD1-mode-step').iterdir())if q.is_file()]
    for group in ('modern','launcher'):
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if q['path']==str(ROOT/'completeness-audit.json')else q for q in h['shared']]
    h['server']['known_CD1_mode_step_boundary']=pin
    h['next_steps']=[
      'Batch28 finite Native control0/1 caller args and Java direction contract sealed. D1/sub2/BYTE1NEXT or2PREVIOUS is relativecycle0,1,5,2,4,3, body3/frame5, not absolute mode. Existing sender/serializer/D5 contracts referenced without oldPASS.',
      'UI firstgaps allocation46D088/46D0D6→4F118C, ctor4154FE→414910, conditionalparentinsert46D0CF/46D11D→41B3F0. Actualcallback/control→ECX/click/owner/phase/transport unknown; do not bridge or expandunknownNativebodies.',
      '12 selectedJava methods match AD368/C7 code andCPsymbols: parser exact1remaining anddirections1/2; ROOM/currentroom ordinary guard; updateSettings serverowner gate; normalreadyreset→perpeerD5/2then3, Town/updatefalse selfD5/0then1. Mode dynamics/failure atomicity/ACK remainunknown.',
      'Adopt existingC7 WAITING mapnetwork report3BBAA920 andsavedindices73..90:514→509,Host/GuestFFFFselfrestorecurrent509,Guestvalid514self0/1 current509. OldAD368restore564/7capturehits0 applyoldversion/scope only; Batch27 immutable. Mapdynamic doesnot qualifymode-stepdynamic orNativeGUI.',
      'Exact missingmode evidence forsupervisor: versionpinned D1sub2 NEXT/PREVIOUS withbefore/aftermode/map,readyperpeer thenD5/2/3,andTown/nonowner/gatefailure0/1distinctions. No Java/SQL/provider/newrun/extra packet orModern interruption requested; knowncorroboration only,packetunchanged.',
      '29priorpins retained;7newcode/3reused/21assert/246technical+2scopechecks/6Java classes12methods/13texts/18savedC7maprows. Native/GUI/SQL/provider/Javaexecution/Modern/BIN/newcapture0; oldPASS/capture not replayed; finitebatchstop.',
      'Scope deviation: importlib generatedexternal ClassFileReadOnly.cpython-312.pyc; cleanup auto-review rejected withblocked bypolicy, cache remains. Owned scripts disablebytecode writing; exactpath/hash andcleanup_open recordedinscope-write-correction.json. No bypasscleanup orothergroup source/product modification.',
    ]
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,
      current_task='Batch28 finite mode-step caller/Javarelativecycle andC7mapversion supplement sealed; UI/mode dynamic/transport authorityopen; unintendedexternalPythoncache cleanup rejected anddocumented.',
      CD1_mode_step_summary=summary)
    progress['handoffs']['modern']='CD1-mode-step-contract.json: direction1NEXT/2PREVIOUS relativeJava permutation, normalD5/2→3 vsreject0/1; C7 mapdynamic version table supersedes oldglobalabsence claim withoutchangingBatch27. UI/mode dynamic/authority andexternalcachecleanupopen.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['CD1_mode_step_audit.py','review_CD1_mode_step.py','publish_CD1_mode_step.py',
      'CD1-mode-step-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT))for q in (ROOT/'CD1-mode-step').iterdir()if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),
      'prior_primary_pins_preserved':29,'packet_unchanged':packet,'scope_write_correction':review['scope_write_correction'],'cache_cleanup_completed':False},ensure_ascii=False))
if __name__=='__main__':main()
