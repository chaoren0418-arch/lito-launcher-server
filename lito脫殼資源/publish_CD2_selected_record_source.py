"""Seal Batch31 field mapping and compact Modern dependency handoff."""
import sys
sys.dont_write_bytecode=True
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-selected-record-source-contract.json');pin=record(ROOT/'CD2-selected-record-source-contract.json');r=load('CD2-selected-record-source/independent-review.json');v=p['verification']
    assert r['all_match']and r['primary']==pin and(r['check_count'],r['decoded_instruction_rows'],r['model_count'])==(239,275,13)
    assert(v['new_code_ranges'],v['new_data_ranges'],v['necessary_reused_raw_ranges'],v['instruction_assertions'],v['prior_primary_pins_preserved'])==(8,4,11,55,32)
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']+[r['models']]:
        a=record(q['path']);assert all(a[k]==q[k]for k in ['path','bytes','sha256'])
    for q in p['prior_primary_pins']:
        a=record(q['path']);assert a['sha256']==q['expected_sha256']and a['bytes']==q['bytes']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'CD2-selected-record-source/READ-ME.txt'
    reader.write_text('''第三十一批：selected character record+3E BYTE 的producer／copy／marker clear來源

主入口 ../CD2-selected-record-source-contract.json；producer-copy-clear-and-field-contract.json有C/S/opcode/字段/端序編碼/length/trigger/phase/VA-RVA/fileoffset/sourceSHA/等級/未知；Modern-ready-dependency-matrix.json是Batch29/30/31精簡依賴對照。
固定PE7094272 SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9；8新code、4data、11必要既存raw切片，55operand assertions，275decoded rows、239新checks、13新模型全部match；32priorprimarypins保持。
先讀既存lifecycle/selectedcallback/reconnect契約及acceptedserver-character-directory-consumer-contract.json13072 SHA412748DE715782AEC88D00251DEE791A472EF7C180F905EDF43AA3A36B9464C7。既存parserraw／fieldproducer／finiteIL來源只read-as-data，無oldPASS/generator/review或golden/capture重播。

S_CB條件式producer：
KnownS方向opcodeCB，4B3095→4A7550；Native入場64AFBC/BD alternate-return flags可skiprow，4A7572→4D6970第一unknownSTOP。後count/index島及caller是獨立條件，沒有跨helper成功或actualphase/GUI接受。
4A7577讀DWORD76079C index，4A757C count7607A0；signedindex<count才4A758C。兩LEA*5/*9再*4證record=62FAA0+index*B4，180bytes是internalstride，不是packetbodylength。
4A75E9 LEA EDX,record+3E，4A75F0 pushEDX；4A7631已知4A2A50 parsercall。重構36stackwords，只授去opcode後format tokens1..16 destination及兩stringcaps28/17inclNUL；其他taildestinations/extra stackslot/currentparsecompletion不提升。
Format6343B4是sscchhhcccccccccddddddddddcdddddc NUL；+3E對token16（含前兩個string）、第14個數值field、numeric suffix byteoffset16。不能稱第16個數值BYTE。
沿既存reader minimalc charmap1/jump4A2A8A：4A2A8D讀BYTE，4A2A93向傳入pointer寫BYTE。前兩s的copy77409B70、seek774095E0仍外部unknownSTOP；typedwrite是條件式，非完整收包／runtime／codec證據。
AcceptedAD368 fields/reference/finiteIL證此位置reserved_byte總送.c(0)。只用server版本名稱，Native+3E仍unnamedBYTE；不得命名owner、mode、ready或特定職業資格。
Server ordinaryGAME auth→matchedE2→LOBBY→S03count→count個CB rows的既存版本scope；沒有新JavaSQL/native/dynamic試驗。Nativehandler沒有本批合格numericphase/owner/storagecapacityguard。
N為displayname MS950 bytes不含NUL，M為clanbytes；B=3+N+M，該BYTE bodyoffsetB+16；完整body82+N+M、frame84+N+M含u16LEtotal，suffix79bytes。AD368 producerN1..27/M0..16；Native externalcodec仍未知，不等同全文codec認證。

Clear／lifetime限定：
S03方向S opcode03，4B3177→4A7450，afteropcode c，body2/frame4。先c讀localBYTE後MOVSX到signedDWORD7607A0，76079C=0；Native此prefix沒有0..8guard，server0..8是producer政策。
4A747B..4A7494：從62FACE起，每B4写[EAX-1]/[EAX]FF，到62FD9E；精確四個slot的record+2D/+2E markerpairs，沒有+3Eclear或整180bytes歸零。
Countzero的4A749D→49A510第一unknownSTOP，nonzero4A74E0替代分支未展開。四slot markerclear不是完整Native容量4/8證明；existingserver native8row註解不足以授fixedPE index/storage安全。
492394..4923AC另localreset島也只同四組markerFF，upstreamUI/trigger未知。4940D7→494120選中query第一unknownSTOP；之後獨立4940EA只写selected+2D=FF，4940F8 virtual[EDX]第一unknownSTOP。
497150只取+2D/+2E markers，到497168→494E00第一unknownSTOP，不拿後續helper猜clear。
本PEfirstslot+2D/+2E rawFFFF、+3E raw00，只是snapshot，不代表当前row或loader/每次reset已完成。Markerreset可保留舊+3E；不自動把invalidslot metadata當選中有效row。

Directtablexref call-free copy／overwrite：
497600..4976AA為直接62FAD0 tablexref發現的170byte call-free boundedbody，沒有沿caller/helper呼叫深挖。Caller/opcode/phase/epoch仍未知。
EBX=62FAD0+slot*B4=record+30；inlineNUL比對inputECXalias與recordbase，四slot掃描，可能每個samealias都update，不能授physicalID/unique/currentowner。
Aliasmatch時inputEDX newclanpointer→local，REPNE SCASB取strlen+NUL，REP MOVSD/MOVSB copy到EBX-14=record+1C，無localcapacityclamp。有效source/destination memory本身未知。
Scalar stores是record+30 WORD、+32 WORD、+34 WORD、+36 BYTE、+37 BYTE，沒有直接scalar+3Ewrite。
但copy最後offset=1C+strlen，strlen>=34會碰+3E；strlen34的NUL可把+3E設0，strlen35等可覆寫nonzero。不能 blanket宣稱此body保存+3E。
若輸入受AD368clan<=16政策約束，copy止<=2C，則不碰+3E；其它caller/input驗證與字串安全沒合格。本批不設漏洞修復、放寬domain或新增packet要求。

Modern精簡採用：
localrecord：CB reservedmetadata與marker/copylifetime；需currentselectedrow/index/storage/epoch和helper返回，不給ready權限。
localguard：Batch30 startup64AF9D／protectedselector；selection先requestsetter0，+3Enonzero另request1；actualobject/key/index/helpercompletion未知。
userintent：Batch29七個conditionalD2+statecalls；現映像opcodeonlycallsite、NativeUIevent/click與transport仍未知。Explicitauthorizedintent獨立於metadata。
servercommit：AD368participantready與exactownerseparateopcodeonly各有servergates；會員/phase/owner資格由server/currentroom授，不能從record反推。
D1/E1：serverprojection／countdown-cancel receipts獨立，綁currentepoch/room/join/alias，partialsend/race及localtimerexpiry→WORLD缺口保持。
GUI：沒有Native/GUIrun，callback/constructor/virtual/private與actualcontrolacceptance另需證據。
Batch29 F3867EC1...與Batch30 FCD54F0D...原SHA不改；485F已接受WAITINGsettingsnetwork只是其版本補充，不移轉為ready-state-machine/COUNTDOWN/WORLD/GUI驗收。

Known-evidence-delta.json明列既知S_CB/03欄位與內部destination補證，newwire/newopcode/wirecorrection/newcapture/newframeadoption0；server-packet-delta.json26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C保持，不通知server改packet。
本批只寫lito脫殼資源，Python-B與sys.dont_write_bytecode=True；沒有Java/SQL/Native/GUI/input/injection/provider/JVM/Modern/登入器/BIN變更，沒有未知callee/virtual/externalbody或WORLD/bot/matching擴張。
剩餘必要工作是helper/runtimeselectedepoch/index/storage/copycallerbounds及Modern本組authorizedreadywriter/receipt整合驗收；本批不擅開run或unknownbody。
Prior externalpyc清理auto-review拒絕blocked by policy仍open，scope-write-correction.json SHA B51ECB6CC2AD37DA279E03F71B92C6B75DA52F00027737A6AB8AC2A02C97DFE4保持；未重試、改述、轉派或換機制。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_selected_record3E_sources_complete':True,**v,'independent_checks':239,'decoded_instruction_rows':275,'new_conditional_models':13,'Modern_dependency_stages':6,'reserved_BYTE_native_conditional_mapping':True,'whole_record_runtime_GUI_authority_open':True,'marker_reset_slots_only':4,'marker_reset_clears3E':False,'unbounded_clan_copy_reaches3E_at_strlen':34}
    complete=load('completeness-audit.json');complete['CD2_selected_record_source_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第三十一批selected record+3E：')[0]
    entry.write_text(txt+'\n第三十一批selected record+3E：CD2-selected-record-source-contract.json + CD2-selected-record-source/READ-ME.txt + Modern-ready-dependency-matrix.json。CB token16 c（數值field14/suffixoffset16）→record62FAA0+index180+3E，AD368reserved.c(0)既知producer，Native先前strings/helpers未知僅conditionalstore；S03/localreset只四slot+2D/+2E FF不clear3E，不能推容量。Call-free497600 aliasmatchedclancopy無localcap，strlen>=34可覆3E；scalar止37，其它caller未知。8code/4data/11reuse/55assert/275decoded/239checks/13models/32priorpins。六stage依賴分localrecord/guard/intent/servercommit/D1E1/GUI，Batch29/30 SHA不改，newwire/capture0/packet不變，cache清理拒絕仍open。\n',encoding='utf-8')
    phase='BATCH31_SELECTED_RECORD3E_KNOWN_CB_SOURCE_COPY_MARKER_SEALED_RUNTIME_UI_AUTHORITY_CACHE_CLEANUP_OPEN'
    h=load('handoff.json');incident=h['scope_write_correction'];assert incident==v['scope_write_correction']
    h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),CD2_selected_record_source_contract=pin,finite_selected_record3E_sources_complete=True,Modern_ready_dependency_matrix=record(ROOT/'CD2-selected-record-source/Modern-ready-dependency-matrix.json'))
    added=[pin]+[record(q)for q in sorted((ROOT/'CD2-selected-record-source').iterdir())if q.is_file()]
    for group in ['modern','launcher']:
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if q['path']==str(ROOT/'completeness-audit.json')else q for q in h['shared']]
    h['server']['known_selected_record3E_boundary']=pin
    h['next_steps']=[
      'Batch31 knownCB token16 c (numericfield14/suffixoffset16) conditionalNativepointer→record+3E sealed;AD368reservedbyte.c(0) acceptedproducerreference. Record62FAA0+index180 internalstride distinctfromwirelength. Entry4D6970 andearlierscopy77409B70/seek774095E0 unknownSTOP;noactualNativeparse/codec/GUI/value authority.',
      'S03countBYTE→signedDWORD7607A0,index0;4A747B loopand492394localreset writefour+2D/+2EmarkerpairsFF,no+3Eclear. Fourmarkers are not completeNativecapacity4or8;AD3680..8producerpolicy distinctfromfixedindex/storage safety. Selectedmarkerquery494120/virtual[EDX] andmarkerconsumer494E00 STOP.',
      'Directtablexref497600 call-free170byte aliasmatchedrecordupdate:clanstrlen+NUL to+1C unbounded;scalar+30/32/34WORD,+36/37BYTE. strlen>=34 canoverwrite3E;AD368<=16clan policywouldnot,othercaller/epoch/inputvalidationunknown. No blanketpreservation/clear/physicalID/uniquematch orready/owner/class/mode semantic.',
      'Modern-ready-dependency-matrix.json has6stages:localrecord/localguard/userintent/servercommit/D1_E1receipts/GUI. Batch29 conditionalD2calls andopcodeonlygap, Batch30 startupflag/protectedselectorrequest0/1, newCBreservedmetadata staylocal;explicitauthorizedreadyintent/currentepoch/room/join/alias andservergates independent. No localtimerexpiry WORLDgrant.',
      'Batch29F3867EC1 andBatch30FCD54F0D immutable;prior485FWAITINGsettingsnetwork acceptedscope unchanged,notready/COUNTDOWN/WORLD/GUI. NewCBdestinationmapping known补證,notnewwire/opcode/fieldcorrection;packetB4C00BF1unchanged,norepeatedserverpacketnotification.',
      '32priorpins;8newcode/4data/11reused slices/55assert/275decodedrows/239checks/13newmodels. Newcapture/frameadoption/wire/opcode/correction0. OnlyownedROOTwrites;Python-B/dont_write_bytecode;noJavaSQL/NativeGUI/input/injection/provider/JVM/Modern/launcher/BIN orunknowncallee/WORLD/bot/matching actions;finitebatchstop.',
      'Remainingneeded runtime/helper/selectedepoch/index/storage/copycallerbounds andModernauthorizedreadywriter/receipthandling acceptance stayopen;no unauthorizedrun orunknownbody expansion requested. Prior externalpyc cleanup rejectionblocked bypolicy remainsopenwithsamepin;no retry/reframe/transfer/alternative mechanism.',
    ]
    assert h['scope_write_correction']==incident and h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch31 selectedrecord3E knownCBreservedfield conditionalproducer/copy/markerbounds andcompactModerndependencytable sealed;runtime/index/storage/helper/GUI/authority andpriorcachecleanupopen.',CD2_selected_record_source_summary=summary)
    progress['handoffs']['modern']='CD2-selected-record-source-contract.json + Modern-ready-dependency-matrix.json:CBtoken16 reservedmetadata→record3E conditional;markersdo notclear3E,unboundedclancopycanreachit. Localguard/intent/servercommit/D1E1/GUIstagesseparate;Batch29/30SHAunchanged.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['CD2_selected_record_source_audit.py','review_CD2_selected_record_source.py','publish_CD2_selected_record_source.py','CD2-selected-record-source-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT))for q in (ROOT/'CD2-selected-record-source').iterdir()if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),'priorpins':32,'packet_unchanged':packet,'scope_write_correction':incident},ensure_ascii=False))
if __name__=='__main__':main()
