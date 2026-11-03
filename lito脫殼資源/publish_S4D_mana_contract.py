"""Publish/seal finite batch11; preserve packet_delta and all prior primary pins."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    r=load('S4D-mana-point-contract.json');v=load('S4D-mana-point/new-evidence-verification.json')
    assert v['new_raw_ranges']==6 and v['new_instructions_asserted']==17 and v['inherited_instruction_assertions']==16
    assert len(v['instruction_assertions'])==33 and v['new_and_inherited_bytes_match_fixed_PE']
    for p in v['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in r['reusable_outputs']:assert record(Path(p['path']))==p
    assert r['wire']['body_including_opcode']==5 and len(r['wire']['fields'])==2
    assert r['server_handoff']['new_wire_differences']==0
    packet=record(ROOT/'server-packet-delta.json')
    assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    pin=record(ROOT/'S4D-mana-point-contract.json');readme=ROOT/'S4D-mana-point/READ-ME.txt'
    readme.write_text('''第十一批：S4D5 current／max MP 契約

主要入口：../S4D-mana-point-contract.json
固定PE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。
原BIN SHA CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292；ASM SHA2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A。ASM只作導航，與fixedPE逐byte確認，未將IDA input fingerprint當成fixedPE。
6段new raw（2code／4data），17項new指令及16項inherited指令固定expected斷言，合計33。7份既有raw直接驗bytes對PE，不重跑producer或舊PASS。2筆已封存AD368 frame共4欄位；8新static cases。

Direction／Dispatch
這是server_to_client opcode0x4D（77），既有server名S_MANA_POINT。不是client_to_server同opcode的其他契約。
Mode64BBD4==4且signed phase64AF98>=3：opcode map4B3470+4D=4B34BD，byte43hex=slot67，jumpentry4B3314 DWORD4B2D07。
其餘fallback：map4B36B4+4D=4B3701，byte1Bhex=slot27，jumpentry4B35DC DWORD4B2D07。
兩支都經4B2D07 movECX,ESI／4B2D09 call4AEFB0，傳完整含opcode body pointer。直接caller case共0C bytes，handler共1F bytes，皆完整decode至ret。既有receive pump同步dispatch完整body；optional diagnostics64AFA2非零時selector前有private/external log calls，沿用舊邊界，沒有native完成推論。

可用typed欄位與消費長度
body offset  bytes  端序  名稱         Native動作
1            2      LE    currentMP    4AEFB9 movsx EAX,WORD[ESI]；i16延伸成i32，再4AEFBD→4C3E90，ECX8315E0。
3            2      LE    maximumMP    4AEFC2 movCX,WORD[ESI+2]；4AEFC6直接寫WORD8348BA，exact2bytes。
ESI=body+1，合計4 parameter bytes，加opcode=5 body bytes；sealed wire_length=7含2byte header。沒有objectID、ownerID、string或generic format reader。Handler沒有length參數、exact-size check、consumed-pointer return；5只是結構預期，不是native malformed／trailing拒絕驗收。
maximumMP此處僅raw16bits copy；無sign/zero-extension，不能單由此handler宣稱其下游所有算術signedness。Modern可保存raw u16並另附既有producer domain，不能偽稱native universal unsigned arithmetic。
currentMP與S0C不同：S0C先maskFFFF再傳同setter，是0..65535；S4D有movsx，是-32768..32767。例raw8000：S0C傳32768，S4D傳-32768；rawFFFF：S4D傳-1。這些high-bit只是code-derived例子，不是這兩筆實際AD368封包。

狀態更新／直接callee／停止邊界
本handler唯一call是4C3E90。沿用batch10固定PE完整5B-byte setter：object+4為16DWORD storage指標，object+8為key，object+0為encoded index XOR B104FA87；當前MP不等於直接讀DWORD8315E0。它用helper4C3CA0填key及slots，再用external773DC650 opaque result選index，把完整32-bit input XORkey存入slot；回傳object pointer，ret4。
4C3E97／4C3EA1→4C3CA0：second-layer raw-backed，但本批不再展開，helper語義未知。
4C3EB6→773DC650：fixedPE沒有raw body，body SHA unavailable；callsite/raw/sourceSHA已列，不補造外部body或return value。
既有protected object及其+4 storage必须有效；無NULL/init/allocation guard，opaque index／helper實際回傳未驗證。Native maximum讀写在setter返回之後，兩欄位能decode不等同native完整update成功。
完整S4D handler／單caller／直接setter無834888 operand、無4C9190構造；不直接測試/要求/建立skill owner。沒有其他直接packet send、actor lookup或effect allocation。這只證明所列body的explicit absence，不證明任意aliases、deeper private/helpers完全不涉及owner。Protected storage ready、owner ready、actor ready、player ready、GUI均未驗收。

封存AD368對照
來源R509 handoff及frames.jsonl SHA E6840F9A98E51188DF4651D49C7A189F77D595A3A727BEEBB381AF5B0CDD7EED。
line44：4D 64 00 64 00，body5／wire7，current100、max100，前一筆line43是object10201的S0C37。
line74：同body、current100／max100，前一筆line73是object10001的S0C37。
兩筆MP值也與前一筆S0C相符。只讀sealed logs，沒有重新capture／native執行。R509原交接仍是first unsupported S0C；本批補下一個S4D靜態契約，不宣稱Modern已執行/接受或owner/player已就緒。

既有Java比較／wire分類
Protocol.S_MANA_POINT=0x4D；manaPoint目前寫h(current)、h(max)，既有baseline完全吻合。未啟用numeric時validator限制0<=current<=max、1<=max<=32767；這是provider gameplay domain，比fixed handler能copy的bitpattern範圍窄。Native沒有同樣positivity或current<=max的guard。
NumericBreakthrough.enabled（schema>=1）會讓Writer對S_MANA_POINT的下一個兩h寫成LE dwords，使body9，需matched patched receiver，超出fixed native5。本批不推整個server目前environment設定。
ClientSession既有局部source順序是sendNativeBattleStatus後送manaPoint，兩筆sealed frame corroborate這個順序，沒有展開後续50/51/52/56。
分類KNOWN_CONTRACT_CORROBORATION；newwire0／newopcode0／server change0。server-packet-delta.json保持B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C。

S1E來源修正已納入
只讀server-skill-publication-origin-comparison.json，SHA A16B1FA66350D50056B78CF5B62F8ABECBC6D399D048B3409A66779C565B6963。
ordinary首次/重入S1E是test-only overlay hook，original B1/AD368無這個hook。不能用此hook當native owner／發布時機證據；S4D也不能補足它。只引用監製已定案report，沒有重跑其IL分析或Java/SQL。

封存範圍與停止
12個既有primary/ranges/snapshot pins（含batch10 EC24D441...）保留。8new audit-only static cases含signed high-bit、nonsymmetric endian/order與4/6byte/wrongopcode拒絕；audit拒絕不能当native拒絕。
所有新輸出只在資源組目錄。native/observer/live process/input/injection/screenshot、Java/SQL/producer、新archive/capture、其他組修改均0。沒有S0 policy或攔截server工作接手。本finite batch完成即停在上述second-layer/external邊界。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_static_S4D5_contract_complete':True,'new_raw_ranges':6,'new_instructions_asserted':17,
      'inherited_instruction_assertions':16,'total_instruction_assertions':33,'field_count':2,'body_bytes':5,
      'existing_actual_AD368_frames_decoded':2,'new_static_checks':8,'classification':'KNOWN_CONTRACT_CORROBORATION',
      'current_mp_i16_sign_extended_to_i32':True,'maximum_mp_raw16_word_store':True,
      'native_GUI_accepted':False,'skill_owner_or_player_ready':False,'indirect_owner_absence_proven':False,
      'new_wire_differences':0,'new_opcodes':0,'server_packet_primary_modified':False,
      'server_S1E_overlay_origin_correction_adopted':True}
    complete=load('completeness-audit.json');complete['S4D_mana_point_batch']=summary;write('completeness-audit.json',complete)
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第十一批S4D5 mana point：')[0]
    reader.write_text(text+'\n第十一批S4D5 mana point：S4D-mana-point-contract.json + S4D-mana-point/READ-ME.txt。6新raw、17new/16inherited指令斷言；兩dispatch→4AEFB0。currentMP i16→i32 protected8315E0，maximumMP rawWORD8348BA；5-byte body，與S0C currentMP maskFFFF區分。兩筆既有AD368 frame吻合Java baseline，newwire0／packet不改。8新audit-only static cases，protected/private深層與owner/player ready仍未驗收。S1E ordinary test-only overlay來源修正已採納，12既有pins保留。\n',encoding='utf-8')
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH11_S4D5_MANA_POINT_STATIC_CONTRACT_SEALED',entrypoint=record(reader),
      S4D_mana_point_contract=pin,finite_S4D_mana_point_audit_complete=True)
    added=[pin,record(readme)]+r['reusable_outputs']
    for group in ['modern','launcher']:
        mapped={x['path']:x for x in handoff[group]};mapped.update({x['path']:x for x in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['server']['S4D_known_contract_corroboration']=pin
    handoff['next_steps']=[
      'Supervisor/Modern consume S4D5 currentMP signed16→signed32 and maximumMP raw16 fields; preserve S0C unsigned-current distinction. Decode does not initialize protected storage or owner/player ready.',
      'Existing sealedAD368 baseline5 andJava manaPoint match; no newwire/opcodes/provider change. Numeric schema body9 remains outside fixednative5.',
      'Adopt A16B1FA66350... correction: ordinary first/reentryS1E is test-only overlay, absent originalB1/AD368; do not use it for native owner/publication timing.',
      'Preserve batch3..10 andpacket12pins; stop at second-layer4C3CA0 andexternal773DC650. No oldPASS/native/SQL or50/51/52/56 expansion.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],
      current_task='Batch11 finiteS4D5 current/maxMP contract sealed; knownwire corroboration with signed-current distinction, protected/owner/player remain unaccepted.',S4D_mana_point_summary=summary)
    progress['handoffs']['modern']='S4D-mana-point-contract.json + 2typedMPfields + 2sealedAD368frames; distinguish S4D i16current fromS0C u16current; noowner/playerready'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    assert record(ROOT/'server-packet-delta.json')==packet
    names=['S4D_mana_contract_audit.py','publish_S4D_mana_contract.py','S4D-mana-point-contract.json','completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'S4D-mana-point').rglob('*')if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'report':pin,'delta_files_checked':count,'prior_pins_preserved':len(v['prior_primary_pins']),
      'delivery':record(ROOT/'delivery-verification.json'),'packet_unchanged':packet,'new_wire_differences':0,
      'skill_owner_or_player_ready':False},ensure_ascii=False))

if __name__=='__main__':main()
