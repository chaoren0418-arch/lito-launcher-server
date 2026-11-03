"""Seal Batch26 conditional C_DD serialization with explicit UI and delivery gaps."""
from audit import *
from seal_delta import seal

def load(name):
    return json.loads((ROOT/name).read_text(encoding='utf-8'))

def main():
    p=load('CDD-outbound-contract.json');pin=record(ROOT/'CDD-outbound-contract.json')
    review=load('CDD-outbound/independent-review.json');v=p['verification']
    assert review['all_match'] and review['primary']==pin
    assert (review['check_count'],v['new_code_ranges'],v['new_data_ranges'],v['necessary_reused_raw_ranges'],v['instruction_assertions'],v['conditional_CDD_callsites'])==(241,22,5,4,51,3)
    assert len(p['prior_primary_pins'])==v['prior_primary_pins_preserved']==27
    assert review['reconstructed_callsite_varargs']==3 and review['serializer_models']==4 and review['selection_compaction_models']==3
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']:
        assert record(Path(q['path']))==q
    for q in p['prior_primary_pins']:
        actual=record(Path(q['path']));assert actual['bytes']==q['bytes'] and actual['sha256']==q['expected_sha256']
    for q in load('CDD-outbound/ranges.json')['necessary_reused_raw']:
        assert record(Path(q['source_pin']['path']))==q['source_pin']
        assert record(Path(q['index']['path']))==q['index']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'CDD-outbound/READ-ME.txt'
    reader.write_text('''第二十六批：C_DD ordinary room edit 條件式 outbound 契約與有限未知邊界

主入口 ../CDD-outbound-contract.json；conditional-callsite-and-source-matrix.json 是三個候選 callsite、參數來源和未知矩陣；wire-contract.json 提供欄位、length/endian/encoding與證據等級。
固定PE7094272 SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9，base400000。所有VA/RVA/fileoffset/rawhex/逐指令見ranges.json。
27新raw ranges=22code+5data；4必要既存raw引用；51指令assertions；3callsite/3vararg重建、4serializer模型、3selection模型、241獨立checks全match。27priorprimarypins保持。
只讀固定PE、既存ASM導航、既存server契約/frames；只寫本組ROOT。無Native/SQL/provider/GUI/input/injection/新capture，無Java/Modern/BIN改動，無WORLD/private/external/virtual body擴張，無舊PASS或capture重播。

先查既存等價交接
Owned JSON/PY/TXT/MD只找到既存D5非ACK限制，未找到完整Native C_DD參數/serializer資格。既存reconnect保留4C68D0/4F11A0未知，本次僅補必要formatter路徑與stackprobe。
固定ASM exact push0DDh有限導航有5候選，3屬本次C_DD；45FFAD/46E6DA區域是UI430D20座標參數。搜尋不宣稱全image direct/indirect caller完備。
41B410 childlookup、4C6AE0 frame sender、4C6770 sendprefix、4C6700 encodeleaf沿既存raw引用，不重跑舊cipher/runtime測試。

UI submit qualification：第一個未知即停
461050 rawE90B000000→461060；ECX物件入ESI，已知41B410 lookup從[ESI+0C]取index8。46107B rawFF5254 call[EDX+54] 是第一個未知virtual，prefix到此停止。
462910→462620；ECX物件入EBX，已知41B410 lookup從[EBX+08]取index4。462648 rawFF5254 call[EDX+54] 是第一個未知virtual，prefix到此停止。
41B410只讀indexed pointer並ret4，不授actualchild/vtable/runtime物件。後段send準備/capacity/selection各是獨立條件式static island，沒有橋接未知virtual getters/converters。
461153/46286F各call4C8790；4C8790五bytes JMP4C85E0，private body未知即停，不能推helper成功/sideeffect。
因此本批有限完成不等於完整UI提交→send資格；actualclick、輸入接受、phase/owner/version、codec、delivery/ACK仍未知。

三個條件式參數堆疊（name/password/options名稱是與server grammar對照，非getter語義證明）
Simple物件[ESI+8]BYTE!=0才入已見DD支線；zero branch未完整資格，不宣稱該branch為C_CC。
461158 runtime XOR selector使用8316B0/B4/B8與B104FA87，decoded==1選五零尾；不能命名為version/phase/owner，與既知wireversion63D54C/phase64AF98分開。
Compact DD push4611C5→shared literal62BF90 cscsh，461236 call4C6AE0：
  DD, pointerEBP-70, MOVSX(BL), pointerEBP-14, EDI；各vararg為DWORDstackslot。
  Wire：DD u8 + name rawNULbytes + attrs lowBYTE + password rawNULbytes + capacity lowWORD u16LE。
  N/P不含NUL，body6+N+P，frame8+N+P。
Legacy DD push461198，literal62BF98 cscshccccc，4611A2 call4C6AE0：
  相同first5 + 五個0；符合extended四option0、K0，body11+N+P/frame13+N+P。
Advanced462874 island固定掃8個localBYTE [EBP-14+index]，保留非零值原序到EBP-2C，K=0..8；不轉換非零80/FF或授server合法域。
[EBX+4C]BYTE!=0→4628C4 pushDD；zero→4628CB pushCC（僅對照literal，無C_CC body擴張）。
62BFA4 cscshcccccb，4628D5 call4C6AE0：
  DD, pointerEBP-90, SX(BYTE EBP-2), pointerEBP-24, EDI,
  SX(BYTE EBP-3), SX(BYTE EBP-4), SX(BYTE EBP-5), SX(BYTE EBP-1),
  K(c), K(b DWORDcount), pointerEBP-2C。
K重複作wire count與rawblock length，body11+N+P+K/frame13+N+P+K。沒有roomID/mapWORD欄位；server currentroom是其上下文。
461119/462835兩capacity islands只見signedEDI>1且<=200，reject路徑virtual+70未知；upstream conversion未知，不能推實際UI接受。Native可見2..200與server ordinary edit2..100不同，僅既知限制對照，不是新wire差異。

必要serializer資格
4F11A0 stackprobe47bytes到4F11CE RET，無callee；4C6AE0 frame sender既存80bytes，bodycapacity13FE=5118，4C6B00→4C68D0。
Formatter char map4C6A90、jump table4C6A74：c→4C6916、h→4C6930、s→4C6989、b→4C69D6。Commonprefix/tail補必要loop/return。
c取DWORDvararg低BYTE；h取低WORD，以低BYTE再SAR8低BYTE寫u16LE；高位截斷。
s REPNE SCASB找NUL再REP copy含NUL，純raw8bit，沒有encoding轉換。Native字串來源/codec/addressavailability未知，不套MS950猜測。
b取DWORDcount與pointer，copy恰K rawbytes不加NUL；used+K>=capacity則return0，要求strict<capacity。c/h可剛好填滿；s guard使含NUL可剛好填滿。
未使用S/wide-format支線的external4C6A10不展開。必要c/s/h/b有限路徑沒有未知external/privatecall。
Formatter return0跳過send，非零長度+2寫frameprefix：4C6B19高BYTE在EBP-13FF，4C6B21低BYTE在EBP-1400，即u16LE。
4C6B27 call4C6770 flag1只授條件式邊界，非實際socketdelivery。
4C6770既存282byte raw只用前101bytes至4C67D0 CALL4EFEA4；8317D0非零/countpositive guard、flag1至既存4C6700 encodeleaf。
4EFEA4 rawE9F7594175 JMP固定external759058A0即停；不依ASM/API標籤推IAT/module/socket/send/ACK，不執行完整encryption或網路。

只讀server契約對照與既存compact witnesses
server-ordinary-room-DD-edit-network.json 10460 SHA A13ECBFE9DC9BFCBA1D32649C829FB960DA995591C59C8613BB403FA4D62C168。
Servercompact與extended layout相同：nameMS950 N1..89、passwordMS950 P0..4、roundtrip/no controls；attrs只mask3C且bit4匹配password非空。
Parsercapacity2..200，ordinaryedit2..100。K0..8、exactremaining、selection非零；全零optiontail可，否則option1<=7、2<=2、3=1..127、4<=31。
先parse後phaseguard；ordinaryROOM/currentroom、exactowner、notbattleStarted、combatants<=capacity是servergate，不能轉成Native UI已資格。
Compact保留modes/options/selections，extended替換options/list並保留mode；acceptedupdate→readyreset D1→D5summary/config，rejectselfD5可同snapshot，phaseinvalidsilent/malformedthrow。
沿savedframes只取76Host-GAME compact body19/frame21（EditAfterRoom attrs40 pwempty cap4）、83Guest-GAME compact body23/frame25（RejectedGuestRoom attrs40 pwempty cap6）。
這兩個是既存server test-client witnesses，Native outbound witness0、extended dynamic witness0，無新capture。不能當Native UI/value/codec或owner成功證明。
Batch23 success/reject D5snapshot bodies相同，仍獨立S方向consumer資料；非C_DD ACK、非原子pair、非allpeer delivery保證。

離線模型與交接
computed-serializer-models.json：compact匹配saved76；legacy為compact+5零；extended Cfg/attrs40/cap4/options3,2,5,7/K2/[1,7]→dd436667002800040003020507020107 body16/frame18。
Opaque raw81FF字串、attrs-128、capacity123456→dd81ff0080005634 body8/frame10；只示rawwidth截斷，不授codec或server合法值。
另3個selection模型與capacity guards都是computed，未Native執行/新capture。
既知layout補證與新的有限Native參數/serializer資格；newwire/newopcode/wirecorrection/newcapture=0。
server-packet-delta.json 26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變。
Modern可引用精確format、width/endian、field-source metadata與unknowns；UI提交、version/phase/owner、codec、private4C85E0、external759058A0、delivery/ACK仍缺資格。
監製進度與handoff已更新；不要求額外Java封包，不接被攔截server工作。此有限批封存即停。
''',encoding='utf-8')
    summary={
      'evidence':pin,'finite_conditional_CDD_outbound_complete':True,
      'UI_submit_phase_owner_codec_delivery_ACK_guard_retained':True,
      'new_code_ranges':22,'new_data_ranges':5,'necessary_reused_raw_ranges':4,
      'instruction_assertions':51,'conditional_CDD_callsites':3,'reconstructed_callsite_varargs':3,
      'serializer_models':4,'selection_compaction_models':3,'independent_checks':241,
      'first_UI_virtual_boundaries':2,'saved_compact_server_test_client_witnesses':2,
      'Native_outbound_witnesses':0,'extended_dynamic_witnesses':0,'prior_primary_pins_preserved':27,
      'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'new_capture_frames':0,
      'packet_modified':False,'Native_GUI_provider_SQL_Java_Modern_BIN_modified':False,
      'old_PASS_or_capture_replayed':False,'Java_new_packets_requested':False,
    }
    complete=load('completeness-audit.json');complete['CDD_outbound_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';text=entry.read_text(encoding='utf-8').split('\n第二十六批C_DD outbound：')[0]
    entry.write_text(text+'\n第二十六批C_DD outbound：CDD-outbound-contract.json + CDD-outbound/READ-ME.txt + conditional-callsite-and-source-matrix.json。三個條件式callsite：compact cscsh、五零尾cscshccccc、selection cscshcccccb；rawNULbytes/c低BYTE/h低WORDLE/b exactcount，frame u16LE。UI先停46107B/462648 virtual+54；private4C8790→4C85E0、transport4EFEA4→external759058A0仍unknown。Runtime XOR selector不是已資格version/phase/owner，Nativecodec/delivery/ACK未知，saved76/83僅server test-client；D5同snapshot非ACK。22code/5data/4reused/51assertions/241checks/27priorpins保持，newwire0/packet不變，有限封存即停。\n',encoding='utf-8')
    phase='BATCH26_CDD_CONDITIONAL_OUTBOUND_SEALED_UI_PHASE_OWNER_CODEC_DELIVERY_OPEN'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),
      CDD_outbound_contract=pin,finite_conditional_CDD_outbound_complete=True)
    added=[pin]+[record(q) for q in sorted((ROOT/'CDD-outbound').iterdir()) if q.is_file()]
    for group in ('modern','launcher'):
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k] for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json') if q['path']==str(ROOT/'completeness-audit.json') else q for q in h['shared']]
    h['server']['known_CDD_outbound_boundary']=pin
    h['next_steps']=[
      'Batch26 finite conditional C_DD callsite/serializer qualification sealed: compact cscsh, five-zero-tail cscshccccc, advanced cscshcccccb with K duplicated for wire count/rawblock size. Raw NUL byte strings, c lowBYTE, h lowWORD LE, b exactcount, u16LE frame prefix.',
      'UI first unknown virtuals: 46107B and 462648 raw FF5254 slot+54. Later preparation islands do not bridge these getters/converters. Private 4C8790 JMP4C85E0 and transport 4EFEA4 E9F7594175 JMPexternal759058A0 remain stops; no full UI-submit/send/delivery qualification.',
      'Runtime XOR selector decoded==1 is not qualified version/phase/owner. Native capacity islands2..200 versus server ordinary2..100 are corroborated constraints, not newwire. Native codec/address/runtime inputs and server authority/UI acceptance remain unknown.',
      'A13E server contract and savedcompact76/83 are existing server test-client evidence only. No Native outbound or extended dynamic witness. Batch23 independent D5 success/reject same snapshots are not ACK or atomic/allpeer delivery. Do not request extraJava packets or resume intercepted server work.',
      '27 priorpins unchanged;22code/5data/4reused/51assertions/3callsites/241checks/4serializer+3selection computedmodels. Newwire/opcode/correction/capture0;packet unchanged;Native/SQL/provider/GUI/Java/Modern/BIN0,oldPASS not replayed. Finite batch stop with explicit UI/phase/owner/codec/delivery/ACK gaps.',
    ]
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,
      current_task='Batch26 conditional C_DD argument/serializer contract sealed; UI virtual+54, private4C85E0, external759058A0, phase/owner/codec/delivery/ACK remain unknown; zero newwire.',
      CDD_outbound_summary=summary)
    progress['handoffs']['modern']='CDD-outbound-contract.json/conditional-callsite-and-source-matrix.json: three conditional formats, rawBYTE/u16LE/exactblock, explicit UI+54/private/external stops; no qualified UI/phase/owner/codec/delivery/ACK.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['CDD_outbound_audit.py','review_CDD_outbound.py','publish_CDD_outbound.py',
      'CDD-outbound-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT)) for q in (ROOT/'CDD-outbound').iterdir() if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),
      'prior_primary_pins_preserved':27,'packet_unchanged':packet,'finite_UI_gap_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
