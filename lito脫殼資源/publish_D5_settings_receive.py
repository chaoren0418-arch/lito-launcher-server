"""Seal Batch23 finite D5 settings evidence without old PASS or execution."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('D5-settings-receive-contract.json');pin=record(ROOT/'D5-settings-receive-contract.json')
    review=load('D5-settings-receive/independent-review.json');v=report['verification']
    assert review['all_match'] and review['primary']==pin
    assert (review['check_count'],v['new_code_ranges'],v['new_data_ranges'],v['inherited_necessary_raw_ranges'],v['instruction_assertions'],v['aligned_in_slice_branch_targets'])==(192,2,0,13,66,10)
    for p in report['sources']+list(report['existing_evidence'].values())+report['outputs']:
        assert record(Path(p['path']))==p
    for p in report['prior_primary_pins']:
        current=record(Path(p['path']));assert current['bytes']==p['bytes'] and current['sha256']==p['expected_sha256']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    readme=ROOT/'D5-settings-receive/READ-ME.txt'
    readme.write_text('''第二十三批：Native S_D5 subtype0/1 完整有限分支／直接UI与state副作用

主入口 ../D5-settings-receive-contract.json。
Batch18 C8E20CD020392C0D26E21F8FB1B830B9009998B942CABF65EBDF59AA355EDA2C 已保存header、dispatch、subtype0/1欄位讀取。
本次只補其 subtype0 EE9..EF5 13bytes與map projection9F89..9FA4 28bytes兩個缺口；沒有重抽之前已完整的subtype1或header。
固定PE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9，base400000；每個新range有VA/RVA/fileoffset/rawhex/SHA/逐指令。
必要沿用13個raw範圍，66 operand assertions、10 aligned branches、192獨立checks全match；24prior primary pins包含Batch22保持。
沒有Native/GUI/input/SQL/provider執行、新capture、Java/Modern/BIN修改、WORLD/private/external/virtual body扩張；不重跑Batch18/22或其它舊PASS。

Native phase/room/subtype gate（沿Batch18，非新runtimeauthority）
D5既有primary/fallback dispatch均到4B2F67→4A9E50；mode4/signedphase>=3 primary選路資格沿用，不重跑整個dispatch。
4A2A50按cd从body+1讀subtype到EBP-1、roomDWORD到EBP-8；payload从body+6。
phase64AF98==6：4A9E7F直接把incoming room寫64ABF4，再檢subtype，沒有原room相等gate。即使subtype invalid，也可先改roomglobal。
其它phase：4A9E8F比incoming room與64ABF4；不等則4AA06C return，相等則繼續，這裡沒有額外phase whitelist或owner/member/self身份檢查。
MOVSX i8 subtype後 unsignedJA >9：只0..9可dispatch。01分支、header、map projection、RET stub中沒有wireversion63D54C判定；不推任何上游/actualversion值。
Modern使用獨立owned connection/epoch/currentroom gate，不能採用Nativephase6先overwrite行為作room/source/ownerauthority。

Subtype0，entryVA004A9EAB / RVA000A9EAB，format schcscccc
欄位依wire順序，已知名稱只作candidate/source designation：
  room_name          s →64AB70，capacity90含NUL
  option0/mode byte  c →64ACCA BYTE；64ACCB相鄰高BYTE不被此cstore改寫
  map_word           h →stackEBP-4 WORD；不是64ACCA WORD，也沒有全WORDglobalcopy
  summary0           c →64ACCC BYTE
  owner_display      s →64ABCA，capacity28含NUL；不連結session/member/physical/sourceauthority
  summary1..4        c →64ACD0..64ACD3，每項1BYTE
4A9EE4 reader返回後，4A9EEC call4A9030；4A9030固定PE只有C3 RET（必要沿用），沒有UI redraw或callback body。
4A9EF1 jump4A9F89：讀localmapWORD，若整WORD==FFFF或FF9C，直接採低BYTE FF/9C；其它WORD對AL加0C、mod256，4A9F9B存64ACC9 BYTE，return4A9FA4。
map564(0234)→64(40)；0→12；FFFF→FF；FF9C→9C；FFFE→0A；8000→0C；00F8→04。
這裡沒有runtime map tablelookup、index、count、locale、version判斷或mode减法。不得套用新版CC lookup或其特殊模式投影；64ACC9不是map/resourceID。
此分支不明確寫attributes64ABE6/password64ABE8/capacity64ABF0/options64ACD4..D7/selections64ACD8..DF。
只可證conditional stores與RET call；ready reset、catalog mutation、phase change、actualGUI不從此handler推出。

Subtype1，entryVA004A9EF6 / RVA000A9EF6，format ssccccccc
  room_name         s →64AB70，capacity90含NUL（與subtype0同buffer）
  password          s →64ABE8，capacity5含NUL；不是64ABE6 attributes
  attributes        c →64ABE6 BYTE
  capacity          c →64ABF0 BYTE；不是DWORD寫入，64ABF1..F3保留於此有限store路径
  options1..4       c →64ACD4..64ACD7，每項1BYTE
  count K           c →stackEBP-2 BYTE；無persistent count store，不是64AFD0 map count
4A9F2F reader返回cursorEAX；ESI索引0到7固定8次，每次MOVSX i8 K，比較ESI與signedK。
ESI<signedK時把cursorEAX傳c reader，selection存64ACD8+ESI，reader EAX cursor進一；否則4A9F58將該格0。
讀取數=min(8,max(0,signed_i8(K)))，8格總是被寫：K0→清0全部；K1..8→讀K個並零尾；K9..7F→只讀8，餘bytes不驗exact-end；K80..FF負數→讀0/清8。
4A9F69 ownreturn；這条完整分支沒有4A9030或其它directUIcall，沒有map/owner/mode/summary0/summary1..4/ready/phase改寫或ACK result。
Native這裏沒有attribute/capacity clamp、payload length或exactend驗證。AD368已知合法producerK0..8/u8capacity與房間capacity policy另有來源資格，不能把Native permissive counts改成新wire要求。

Typed reader與未知邊界
沿用4A2A50 296bytes資格：c明確1BYTE store、h明確2BYTE LE store、d明確DWORD LE store；EAX返回cursor。
s傳入destination/capacity，4A2AF9→external77409B70第一次unknown即STOP；不追callee、不猜strcpy/API/module/codec/bounds或actualcopy成功。
條件continuation4A2B04把dest+capacity-1置NUL，4A2B09→external774095E0再次STOP；cursor取unknown回值再+1。
因此以上後續store/field保留資格以parser與stringbackend返回為條件。非ASCIIcodec、copiedbytecount、trailingbuffer內容、重入/lifetime/provider結果皆unknown。
「保留欄位」只是有限路徑沒有明確destination，不是全image無writer或未知external無旁效應斷言。

兩種snapshot各自獨立，無ACK或atomicpair資格
每次D5呼叫各讀自己的cd header、room gate、自己的subtype分支並各自return；本次完整有限路徑沒有pendingpair、generation/result欄位、等另一subtype、rollback或combinedcommit。
subtype0(A)與subtype1(B)都可能通過phase6各自overwrite room；若backend返回，可以留下summary A /config B。其它phase mismatchedroom會先return。
兩者共同寫room_name buffer，後到且通過gate的一份可以overwrite；numeric footprint其餘是分開fields，不因兩種bytefieldsdisjoint就推coherentpair。
Native沒有用subtype0/1發出ACK或成功/拒絕結果。Server的ready清理/D1state0是另個packet/event，不從D5推定ready、ownleave、start或owner成功。
不推全client沒有其它snapshot機制；此有限handler與savedframes只是不提供atomiccoherentpair資格。Server separategetters/sendfailure/concurrency/allpeerdelivery仍unknown。

Server只讀保存field witness（沒有新程序或capture）
server-ordinary-room-DD-edit-network.json SHA A13ECBFE9DC9BFCBA1D32649C829FB960DA995591C59C8613BB403FA4D62C168。
frames.json 16142bytes SHA0FD252B12934E39BF42BD118BDD04A9D780D0125F2578B5483CAD7280E83E9D8。
只採array index0based78/79、84/85：Host-GAME成功路徑sub0/1與Guest-GAME拒絕selfsnapshot；frame1based79/80、85/86。
sub0 body47/frame49：room1、EditAfterRoom、mode1、map564、summary0=0、EditHost(EditMoon)、summary1..4=0。
sub1 body28/frame30：room1、EditAfterRoom、password空、attributes40、capacity4、options1..4=0、K0。
78與84的body完全相同；79與85的body完全相同。成功/拒絕是serverreport與自身poststate資格，D5內容不帶outcomecode，不能把收到snapshot當C_DD ACK。
完整欄位offset/bytes/value/native候選destination及consumed/remaining在saved-field-witnesses.json，四份均exact還原。
Body公式：sub0=16+N+O、frame18+N+O；sub1=15+N+P+K、frame17+N+P+K。N/O/P為producer8bitstring byte length excludingNUL，Native不賦MS950codec資格；保存四例都是ASCII。

分類與Modern交接
direction S／opcodeD5=213／subtype0/1；VA/RVA、fixedPE SHA、field types/destinations、phase/room/trigger/evidencelevel/unknowns在wire-contracts.json與主契約。
全部是既知欄位補證及新增Native内部store資格，不是新opcode/newwire/wirecorrection；packet不變 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C。
Modern接收時保留rawmapWORD与projectedBYTE區分；每份summary/config獨立validatedcurrentepoch/room及qualification，不猜pair、ACK、self/owner/session/source/physicalauthority。
保持Batch22候選key/row/codec/runtimeindex/resourceguard；不能把D5 projected64或map564直接綁到LTmapstr ordinal/key/resource。
7map/8signedcount/8phase-room-type有限offline operand models，非Native執行；192checks验证raw/CFG/13varargs/typedstore/fields/conservation/priors。
有限本批完成，監製進度已更新；NativeGUI/provider/backend/runtime/ACK/atomicity未知即停。
''',encoding='utf-8')
    summary={
        'evidence':pin,'finite_D5_subtype0_1_complete':True,'runtime_GUI_ACK_atomicity_guard_retained':True,
        'new_code_ranges':2,'new_data_ranges':0,'inherited_necessary_raw_ranges':13,
        'instruction_assertions':66,'aligned_in_slice_branches':10,'independent_checks':192,
        'map_models':7,'count_models':8,'gate_models':8,'saved_field_witness_frames':4,
        'prior_primary_pins_preserved':24,'subtype0_UI_helper_fixed_RET':True,
        'subtype1_onlyreader_anddirectselection_stores':True,'independent_snapshots_qualified_only':True,
        'ACK_or_atomic_pair_qualified':False,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
        'new_capture_frames':0,'packet_modified':False,'Native_GUI_provider_SQL_Java_Modern_BIN_modified':False,
        'Batch18_Batch22_or_old_PASS_replayed':False,
    }
    complete=load('completeness-audit.json');complete['D5_settings_receive_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text(encoding='utf-8').split('\n第二十三批D5 settings：')[0]
    entry.write_text(txt+'\n第二十三批D5 settings：D5-settings-receive-contract.json + D5-settings-receive/READ-ME.txt。Subtype0/1完整有限分支，4A9030只有RET；sub0localmapWORD→64ACC9低BYTE(+12或FFFF/FF9C)，sub1BYTEattrs/capacity/options、signedK固定8selectionstores。每份獨立roomgate/return，无atomicpair/ACK。保存78/79成功與84/85拒絕D5 bodies同樣，僅fieldwitness。2newPE/13必要沿用raw/66operand/10branch/192checks/24priorpins；newwire0、packet不變，stringexternals/runtimeGUI/atomicity未知STOP，不重跑Batch18/22舊PASS。\n',encoding='utf-8')
    phase='BATCH23_D5_SUBTYPE0_1_SETTINGS_EFFECTS_SEALED_RUNTIME_GUI_ACK_ATOMICITY_OPEN'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),
                                 D5_settings_receive_contract=pin,finite_D5_subtype0_1_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'D5-settings-receive').iterdir())if p.is_file()]
    for group in ('modern','launcher'):
        mapped={p['path']:p for p in h[group]};mapped.update({p['path']:p for p in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if p['path']==str(ROOT/'completeness-audit.json')else p for p in h['shared']]
    h['server']['known_D5_subtype0_1_settings_effects_boundary']=pin
    h['next_steps']=[
        'Batch23 finite D5 subtype0/1 effects sealed. Reuse Batch18 header/dispatch/gate; phase6 overwrites room before validity, otherphases require room equality. Modern independently owns epoch/currentroom validation; no Native owner/session authority.',
        'Subtype0 schcscccc fields then4A9030 RET thenlocalWORD map projection→64ACC9 BYTE (+0C exceptFFFF/FF9C). No Native tablelookup/version/resourceID binding; preserve rawmapWORD independently. Subtype1 ssccccccc BYTEfields andfixed8signedK selectionstores, noUIcall/ACK/ready/phase transition.',
        'Each D5 packet has independent header/gate/path/return. Shared namebuffer can change per packet; no atomicpair/result/ACK qualified. Existing server success78/79 and rejection84/85 bodies identical, fields only; D1readyreset separate.',
        'String reader firstexternal4A2AF9→77409B70 and4A2B09→774095E0 STOP. Actual copy/codec/cursor/return/lifetime, runtimeglobals/GUI, snapshotcoherence/partialsend/concurrency/allpeerdelivery unknown. No deep private/external/virtual bodies.',
        '24priorpins includingBatch22 unchanged; 2newcode/13necessaryreusedraw/66assertions/10branches/192checks, four savedfieldwitnesses only. newwire/opcode/correction/capture0, packetunchanged; Native/GUI/provider/SQL/Java/Modern/BIN0. OldPASSnotreplayed; finite batch stop.',
    ]
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json')
    progress.update(updated_at=stamp(),phase=phase,
                    current_task='Batch23 D5 subtype0/1 full finite branches/directRET/UI andstate stores sealed; savedowner-success/nonowner-reject snapshots do not qualify ACK/atomicpair/runtimeGUI.',
                    D5_settings_receive_summary=summary)
    progress['handoffs']['modern']='D5-settings-receive-contract.json: independentvalidatedsummary/config, typedfieldwidths/retainedfields, rawmapWORD vs64ACC9 projection, signedK8stores; noACK/atomicpair, unknownstringexternals/runtimeGUI.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['D5_settings_receive_audit.py','review_D5_settings_receive.py','publish_D5_settings_receive.py',
           'D5-settings-receive-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in (ROOT/'D5-settings-receive').iterdir()if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),
                      'prior_primary_pins_preserved':24,'packet_unchanged':packet,
                      'runtime_GUI_ACK_atomicity_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
