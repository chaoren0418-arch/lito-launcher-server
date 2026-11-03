"""Seal finite Batch25 scalar field/UI/producer boundary; zero wire changes."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    p=load('D5-scalar-receive-contract.json');pin=record(ROOT/'D5-scalar-receive-contract.json')
    review=load('D5-scalar-receive/independent-review.json');v=p['verification']
    assert review['all_match'] and review['primary']==pin
    assert (review['check_count'],v['new_code_ranges'],v['new_data_ranges'],v['necessary_reused_raw_slices'],v['distinct_reused_raw_pins'],v['instruction_assertions'],v['aligned_in_slice_branch_targets'])==(243,0,0,16,7,33,7)
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:assert record(Path(q['path']))==q
    for q in p['prior_primary_pins']:
        actual=record(Path(q['path']));assert actual['bytes']==q['bytes'] and actual['sha256']==q['expected_sha256']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'D5-scalar-receive/READ-ME.txt'
    reader.write_text('''第二十五批：S_D5 scalar subtype4/6/7/8/9 完整有限store與直接UI路徑

主入口 ../D5-scalar-receive-contract.json；scalar-effect-unknown-matrix.json含每branch精確VA/RVA、目的地、相鄰/sibling保留、UIedge/callee/return、producer與未知。
全部所需raw早已在Batch18封存，0newPE/0新bin副本；16必要slices來自7個rawpins，33operand assertions、7alignedbranches、243獨立checks全match。
固定PE7094272 SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9，base400000；每slice有sourceoffset/size/SHA與VA/RVA/fileoffset/rawhex/逐指令。
26priorprimarypins保持，包括Batch24 9F1B9863C7F0316A4C580BA83DCD14CA92A366418555073FB4FB1181F544ABAE。
只讀固定PE/既存serverreport及原raw，只寫本組；無Native/GUI/input/SQL/provider/process/newcapture、Java/Modern/BIN改動、WORLD/private/external/virtualcallee body擴張、舊PASS/capture重播或server攔截工作。
沒有重做subtypes0/1/2/3/5；共同gate/reader/return是必要引用，不重跑其先前模型矩陣。

Gate（沿Batch18/23/24，非新runtimeauthority）
既有primary/fallbackD5→4B2F67→4A9E50，cd從body+1讀subtype localEBP-1、roomDWORD localEBP-8，payload body+6。
phase64AF98==6：4A9E7F先把incomingroom寫64ABF4，再檢subtype。其它phase incomingroom不等64ABF4則4AA06Creturn，相等後無額外phasewhitelist。
MOVSX i8 subtype後unsignedJA>9容0..9；各scalarpacket獨立gate和return。
有限路徑沒有額外version/owner/self/ready/sourcecheck；actualupstream/runtimephase/input未知。Modern須以ownedepoch/currentCEroom先驗證，不採Nativephase6overwrite作authority。

精確分支矩陣（summaryindex只是server來源designation，實際value語義unknown）
Subtype sourceindex EntryVA   Destination BYTE  Sharedtail UIedge    Callee       ReturnVA   Callerpathbytes
4       0           004A9FA5  0064ACCC          004AA05B   none      none         004AA070   32
6       1           004AA028  0064ACD0          004AA03B   004AA04C  004A9030 RET 004AA055   34
7       2           004AA02F  0064ACD1          004AA03B   004AA04C  004A9030 RET 004AA055   34
8       3           004AA036  0064ACD2          004AA03B   004AA04C  004A9030 RET 004AA055   32
9       4           004AA056  0064ACD3          004AA05B   none      none         004AA070   27
RVA=VA-400000，每branch見主契約/wire-contracts.json。Callerpathbytes不含callee內部bytes；UIcallee另外已封1byteC3。

4/9 noUI tail
Subtype4 push64ACCC、4A9FAA jump4AA05B；subtype9 push64ACD3後fallthrough4AA05B。
4AA05B ESI+=6，push"c"@5025A8、pushinput，4AA064 call4A2A50(input,c,destination)，4AA069 cleanup，4AA070return。
這條tail不經4AA03B/4AA04C，沒有4A9030 UIedge。

6/7/8 UI edge
6 push64ACD0後4AA02D jump4AA03B；7 push64ACD1後4AA034 jump4AA03B；8 push64ACD2後直接fallthrough4AA03B。
4AA03B ESI+=6、push"c"與input，4AA044 call4A2A50；4AA049 cleanup，4AA04C call4A9030，4AA055return。
Store在reader內完成，然後才call UIedge；固定PE4A9030只有C3 RET。這條edge没有明確redraw/store/virtual/private/externalcallback或實際GUI接受資格。
各branch直接callee只有既資格c typedreader，6/7/8多一個固定RET；本次有限paths沒有第一個unknownprivate/external/virtualcallee要下探。Actualruntime/GUI未知，不做整個client沒有其它observer的結論。

BYTE store／相鄰保留／未知value meaning
Existingtypedreader c@4A2A8A讀input1BYTE，4A2A93 BYTE store到傳入destination，cursor+1並returnEAX；c char dispatch、formatloop/return只是有限引用，沒有s/stringbackend。
没有boolean/sign/domain轉換、bitmask/clamp或比較value；80/FF只按原BYTE写入的static條件，不賦server合法值/flag含義。
Sub4只64ACCC，64ACCB(mode高BYTE)及64ACCD..CFgap不被它写。
Sub6只64ACD0、sub7只D1、sub8只D2、sub9只D3；其它四個summarybytes保持，sub9相鄰64ACD4是configoption，沒有被覆寫。
每branch保留64ACC9 mapprojection、64ACCA mode低BYTE/64ACCB高BYTE、attributes64ABE6/password64ABE8/capacity64ABF0、options64ACD4..D7、selections64ACD8..DF。
沒有roomname64AB70/ownerdisplay64ABCA、catalogUI64A958 row+74、previewUI64A98C WORD+0C/BYTE+0F directstore或對应consumercall。
沒有ready/member/phase/owner/source/physical/sessionidentitystore，除了共同headerphase6的roomDWORD。不是ACK/ready/start/ownexit/owner成功或source轉移事件。
每packet只寫一個summaryBYTE，不是whole-summary replacement，不自動補0/1/2/3/5或清siblingfields，不推aggregate atomicity/requestresult/rollback。
「保留」限這些finite paths無明確destination，非全image無writer或actualruntimeGUI結果。
Nativeinputbounds/exactend、當前phase/room/storagevalidity、實際value domain/control意思、serverproducer/delivery、GUIobserver都保持unknown。

Serverbuilder存在 != producer caller資格
沿用 server-room-D5-CF-consumer-supplement.json 12535bytes SHA7774EA7546EEF33CB5240A58A36FA9FFF7A465303049C3C3E238081887279BEF。
其AD368 helper/source/archivedIL只合格scalarlayout：d5 subtype + roomu32LE + valueu8，body7/frame9，source-summaryindex0..4對4/6/7/8/9。
原report明確「source explicitly withholds production caller/effect pending trigger evidence」。本批不搜尋/執行Javaproducer，不要求新增Java發包，不把builder存在當實際caller/trigger/publication可用。
7774那段selectedordinaryexit只觀察D5sub5，沒有本批scalardynamicwitness資格。本批不重掃captures，不宣稱全servertrace沒有scalar。
五個computed-bytefootprint-examples逐bytes展示只改指定目的地及7body/9frame格式，每項actual_trace=False/Native_executed=False，沒有frames.json或saved-field-witnesses。
Modern可保存subtype/rawvalue/候選summaryindex/store/UIedge metadata供後續控制規格引用；不能猜controllabel/bitdomain、outboundC協定、Javaproducer或actualGUI動作。
未來任何設定action仍需其自身C方向protocol/caller/trigger與ownedepoch/currentroom/authority資格，不能用本批S scalar資料授權。

封包分類／未知矩陣／交接
S方向D5=213，每種scalarfields：subtypeu8 body1、roomDWORDu32LE body2、valueu8 body6，body7/frame9，無文字codec。
wire-contracts.json逐subtype提供VA/RVA/fixedPE SHA、欄位/length/endian/encoding、trigger/state、證據等級與unknown。
既知layout補證與新增Native内部store/UIpath資格；newwire/newopcode/wirecorrection/newcapture/newPE=0。server-packet-delta 26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變。
scalar-effect-unknown-matrix.json逐列列firstunknowncallee=None（此有限callee已資格），但actualproducer/meaning/bounds/runtimeGUI/ACK/ready/owner/source/physicalauthority均False或unknown，不能把RETedge當GUI通過。
獨立review從raw依jmp/fallthrough走每條path，重建3varargs、typedBYTE destination、call/return/bytecoverage、模型只改1byte及原pin；243checks全match。
監製進度已更新，供Modern後續設定controls引用；有限此批完成，producer/runtime/GUI/authority未知即停。
''',encoding='utf-8')
    summary={
      'evidence':pin,'finite_D5_scalar_branches_complete':True,'producer_runtime_GUI_authority_guard_retained':True,
      'new_code_ranges':0,'new_data_ranges':0,'necessary_reused_raw_slices':16,'distinct_reused_raw_pins':7,
      'instruction_assertions':33,'aligned_in_slice_branches':7,'independent_checks':243,
      'scalar_branches':5,'direct_RET_UI_edge_subtypes':[6,7,8],'no_UI_edge_subtypes':[4,9],
      'offline_bytefootprint_examples':5,'dynamic_scalar_witnesses':0,'prior_primary_pins_preserved':26,
      'builder_layout_exists':True,'production_caller_trigger_qualified':False,'Java_new_packets_requested':False,
      'ACK_ready_owner_source_GUI_qualified':False,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
      'new_capture_frames':0,'packet_modified':False,'Native_GUI_provider_SQL_Java_Modern_BIN_modified':False,
      'old_PASS_or_capture_replayed':False,
    }
    complete=load('completeness-audit.json');complete['D5_scalar_receive_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';text=entry.read_text(encoding='utf-8').split('\n第二十五批D5 scalar：')[0]
    entry.write_text(text+'\n第二十五批D5 scalar：D5-scalar-receive-contract.json + D5-scalar-receive/READ-ME.txt + scalar-effect-unknown-matrix.json。Subtype4→64ACCC、6/7/8/9→64ACD0/D1/D2/D3，eachoneBYTE/sibling保留；只有6/7/8經4AA03B/4AA04C→4A9030固定RET，4/9sharednoUItail。7774只授builderlayout，不授productioncaller/trigger/controlmeaning/ACK/ready/owner/source/GUI；不要求Java新增發包。0newPE/16必要slices/7rawpins/33assertions/7branches/243checks/26priorpins全match；newwire0/packet不變、dynamicwitness0，有限封存即停。\n',encoding='utf-8')
    phase='BATCH25_D5_SCALAR_STORES_UI_RET_EDGES_SEALED_PRODUCER_RUNTIME_GUI_AUTHORITY_OPEN'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),
                                 D5_scalar_receive_contract=pin,finite_D5_scalar_branches_complete=True)
    added=[pin]+[record(q)for q in sorted((ROOT/'D5-scalar-receive').iterdir())if q.is_file()]
    for group in ('modern','launcher'):
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if q['path']==str(ROOT/'completeness-audit.json')else q for q in h['shared']]
    h['server']['known_D5_scalar_receive_boundary']=pin
    h['next_steps']=[
      'Batch25 fiveD5scalarfinitebranches sealed. 4→64ACCC,6/7/8/9→64ACD0/D1/D2/D3, eachoneBYTE only; adjacent/sibling/mode/map/config/catalog/preview/ready/owner/sourcefields retained in finitepaths.',
      'Only6/7/8 use4AA03B creaderthen4AA04C→4A9030 fixedsingleRET;4/9 use4AA05B creader/noUIreturn. Nounknownprivate/external/virtualcallee expansion oractualGUIacceptance.',
      '7774servercontract supportsbuilder/source/archivedILscalar7body9frame andsummaryindex→subtype only; productioncaller/trigger/valuecontrolmeaning/deliveryunknown. Do notrequestJavaextra packets oroutboundcontrols frombuilderexistence.',
      'Modernfuturecontrolmetadata mayreference exactsubtype/rawBYTE/destination/UIedge/unknownmatrix withownedepoch/currentCEroomguard; noACK/ready/owner/source/physical/session/GUIauthority, noautomaticotherD5subtypes/whole-summary replacement.',
      '26priorpins unchanged;0newPE/16necessaryslices/7rawpins/33assertions/7branches/243checks/fivecomputedfootprints; dynamicwitness/newcapture/newwire/opcode/correction0/packetunchanged. Native/SQL/provider/GUI/Java/Modern/BIN0;oldPASSnotreplayed/sub0/1/2/3/5notredone/serverinterceptedworknotresumed; finitebatchstop.',
    ]
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,
      current_task='Batch25 D5scalar4/6/7/8/9 exactBYTE stores and678 fixedRET UIedges sealed; buildervsproductioncaller/valuecontrol/GUI/authority unknown; zero newwire.',
      D5_scalar_receive_summary=summary)
    progress['handoffs']['modern']='D5-scalar-receive-contract.json/scalar-effect-unknown-matrix.json: perBYTEdestination/siblingpreservation,678UIedge fixedRET,4/9noUI; builderonly/notproducer/control/ACK/ready/owner/source/GUIauthority.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['D5_scalar_receive_audit.py','review_D5_scalar_receive.py','publish_D5_scalar_receive.py',
           'D5-scalar-receive-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT))for q in (ROOT/'D5-scalar-receive').iterdir()if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),
                      'prior_primary_pins_preserved':26,'packet_unchanged':packet,
                      'producer_runtime_GUI_authority_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
