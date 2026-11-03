"""Publish bounded batch12 four-opcode known contracts; only resource-group outputs."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('status-family-50-51-52-56-contract.json');v=load('status-family-50-51-52-56/new-evidence-verification.json')
    assert v['new_raw_ranges']==18 and v['new_instructions_asserted']==72 and v['inherited_instruction_assertions']==16
    assert len(v['instruction_assertions'])==88 and len(v['prior_primary_pins'])==13
    for p in v['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['reusable_outputs']:assert record(Path(p['path']))==p
    assert set(report['contracts'])=={'S50','S51','S52','S56'}
    assert[report['contracts'][k]['body_including_opcode']for k in ['S50','S51','S52','S56']]==[8,6,6,3]
    assert report['server_handoff']['new_wire_differences']==0
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    pin=record(ROOT/'status-family-50-51-52-56-contract.json');readme=ROOT/'status-family-50-51-52-56/READ-ME.txt'
    readme.write_text('''第十二批：S50／S51／S52／S56 四個狀態 handler

主入口：../status-family-50-51-52-56-contract.json
本批恰限四個opcode，不擴其他opcodes、深層private或wholeprogram。
原始來源SHA：
BIN CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292
fixedPE 4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9
ASM 2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A
PE base00400000；各證據含VA/RVA/fileoffset/raw SHA。ASM僅導航，fingerprint不同，不當作raw authority。
18new ranges（8code、10data），72new+16inherited共88固定expected指令斷言；8份inherited raw逐byte對fixedPE確認，沒有重跑舊producer／PASS。

Schema／欄位型別
Direction全為server_to_client，body含opcode，sealed wire另含2byte header。
opcode  native handler  body  parameters  layout       fields（body offset→destination）
50      4AF0A0         8     7           manual7c     STR1→8348B0、INT2→8348B1、WIS3→8348B2、DEX4→8348B3、CON5→8348B4、CHA6→8348B5、weight7→8348CB
51      4AF0E0         6     5           cd           level1(c)→8348CA、experience2(d,4bytesLE)→8348C4
52      4AF100         6     5           ccccc        AC1→8348B6、fire2→8348C1、water3→8348C0、wind4→8348BF、earth5→8348BE
56      4AF320         3     2           cc           SPmodifier1→8348DF、MRmodifier2→8348E0

Native exact copytypes：c/manual load只copy8bits，d以LE組4bytes並copy32bits；c沒有sign-extension。S50 abilities及weight、S51level/EXP的u8/u32 domain來自既有Java。S52 AC/抗性與S56 SP/MR的i8 signed views來自Java validation/domain；fixed handler只提供raw8 store，未展開其下游所有算術，不宣稱native universal signed interpretation。S56兩個modifier名称也由既有Java/same-family識別補證，raw確定的是offset/width/兩個destination。
與S4D不同：本四個handler不含currentMP movsx或protected setter。S51只copylevel/EXP，沒有S0C的level-change effect或clock helpers。
S50是手寫byte loads，最後weight用CL覆蓋ECX低byte，不存在最後input pointer contract。S51/52/56實際ASCII literals分別5028F8(cd)、62B9FC(ccccc)、502778(cc)，均含NUL；指針ECX先+1再送numeric reader。

兩dispatch分支（完全相同handler）
Mode64BBD4==4且signed phase64AF98>=3用primary；其餘用fallback。兩分支都沒有這四opcode的owner/readiness gate。
opcode primary map/slot/jumpentry    fallback map/slot/jumpentry     sharedcase/call
50     4B34C0/70/4B3320              4B3704/28/4B35E0              4B2D2B／4B2D2D→4AF0A0
51     4B34C1/71/4B3324              4B3705/29/4B35E4              4B2D37／4B2D39→4AF0E0
52     4B34C2/72/4B3328              4B3706/30/4B35E8              4B2D43／4B2D45→4AF100
56     4B34C6/76/4B3338              4B370A/31/4B35EC              4B2D71／4B2D73→4AF320
Slot為decimal；jump DWORDs都LE，targets已逐byte核對。四個caller case各0Cbytes；四handler範圍3B/1A/29/1Abytes均完整decode至ret。
沿用完整receivepump、selector、optionaldiagnosticprefix。64AFA2非零時selector前private/external logging邊界保留，不推實際log/dispatch成功。

直接callee與消費長度
S50無call。S51/52/56唯一直接call為4A2A50，格式只用c/d：map4A2B90及jumptable4A2B78分派c→4A2A8A、d→4A2AB9；c cursor+1且BYTE寫，d cursor+4且DWORD寫，4A2B56looptail讀格式NUL，4A2B69 EAX=cursor。
固定c/d path/loop沒有其他calls，不涉及string、external或virtual decode path。Inherit slice也包含h bytes但本批不使用；未展開其它generic reader分支。
三handler不改reader回傳EAX，ret保留body+expectedsize（6/6/3）；directdispatcher case也只pop/ret、沒有檢查EAX。S50則沒有consumed pointer return。這些handler/reader無body-length參數、exact-size/truncated/trailing guard；8/6/6/3是structural必要長度，不能冒稱native malformed拒絕驗收。
副作用僅列明globals存入；沒有其他直接callee、packet send、actor lookup、allocation、protected setter或owner構造。每欄的push/load/store/primitive證據均在各Sxx-fields.json。

封存AD368 frame與共享狀態
固定frames.jsonl SHA E6840F9A98E51188DF4651D49C7A189F77D595A3A727BEEBB381AF5B0CDD7EED。
第一組line45/46/47/48；第二組75/76/77/78，依次S50/S51/S52/S56。每opcode兩筆，共8frames／32fields：
S50 body 50 12 08 09 0D 12 0C 00：18/8/9/13/18/12、weight0，wire10。
S51 body 51 34 00 00 00 00：level52、EXP0，wire8。
S52 body 52 0A 00 00 00 00：AC10、四抗性0，wire8。
S56 body 56 00 00：SP/MR modifiers0/0，wire5。
S50/51/52共享14個stat destinations，也與之前line43/73 S0C相同欄位values吻合。S56沒有S0C對應欄位，不推值或owner等價。
這是read-only sealed logs，不是新native/Modern執行或capture。R509原報告firstunsupported S0C仍作歷史背景，Modern當前是否執行／接受這四handler不由本批靜態分析宣告。兩條capture的sequence0C→4D→50→51→52→56成立，不推全program current callers。

既有Java writer對照／patched profile邊界
Protocol.abilityScores、experience、armorClass、magicStatus baseline writer都與原版layout一致：已知補證KNOWN_CONTRACT_CORROBORATION，newwire0／newopcode0／wirecorrection0。不改Java或packet delta。
S50 Java method的參數排列不同，但真正writer已正確重排STR/INT/WIS/DEX/CON/CHA。Weight用(clamp(weight240,0,240)*29+120)//240投影0..29；raw byte不當retained240本值。
S51 validateslevel1..255及EXP0..FFFFFFFF，native不拒level0，只copybits。S52/S56既有requireSignedByte限制-128..127；Nativec讀取不做同validation/extension。
既有numeric opt-ins（只比較source，不證明currentenvironment或patchedreceiver完成）：
S50 schema>=2：6abilities→d、weight仍c，body26（baseline8）。
S51 schema>=2：level→d、EXP仍d，body9（baseline6）。
S52 schema1..3：AC→d、4抗性c，body9；schema4：5欄都d，body21（baseline6）。
S56 schema>=3：SP/MR→d，body9（baseline3）。
它們需matchedpatchedclient，超出fixednativebaseline；八筆實際frame均baseline長度。本批沒有重跑Java／SQL或producer。

證據等級／unknown／owner邊界
FIXED_PE_RAW：VA/RVA、raw、格式、offset/width/endian/dest、directcalls。
SEALED_CAPTURE：這8bodies/32values與其順序，沒有Native/Modern decoder完成證據。
EXISTING_JAVA_SOURCE：欄位語義names/domain、i8 views、numericprofiles；沒有Java execution。
UNOBSERVED_RUNTIME：body/global有效性、liveinit、downstream所有算術、owner epoch、actor、player、GUI。
四handler/singlecaller都無explicit834888 operand／4C9190 ownerconstructor；不是全程式aliases/deeperreaders不涉及owner的證明。所需numericc/d scope沒有深層private/virtual缺口可補造；optionaldispatch logging及liveworld未知保持原邊界。
已接受A16B1FA66350D50056B78CF5B62F8ABECBC6D399D048B3409A66779C565B6963修正持續採納：ordinary首次/重入S1E是test-onlyoverlay，originalB1/AD368无該hook，不能當nativeowner/publicationtiming證據。沒有重新分析或接手server攔截工作。

封存
13個priorprimary/ranges/snapshotpins保留，含batch11 AD09F778...。16newstaticchecks（四套nonsymmetric/highbit與truncated/trailing/wrongopcode）僅auditmodel；不把audit拒絕或highbit案例說成native拒絕／realframe。
new-evidence-verification.json historical_pre_batch_seal_metadata是本批封存前彙總pin，之後currentmanifest/delivery自然更新；它不屬現行core drift。
server-packet-delta.json原SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變。
只寫ownresource目錄。native/observer/liveprocess/input/injection/screenshot、Java/SQL/test/producer、新archive/capture、他組修改均0。owner/player/GUI/overallunpacking未驗收。本finitefour-opcodebatch完成即止。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_four_status_contracts_complete':True,'opcodes':['50','51','52','56'],
      'body_bytes':{'S50':8,'S51':6,'S52':6,'S56':3},'field_counts':{'S50':7,'S51':2,'S52':5,'S56':2},
      'new_raw_ranges':18,'new_instructions_asserted':72,'inherited_instruction_assertions':16,'instruction_assertions':88,
      'existing_actual_AD368_frames':8,'existing_actual_AD368_fields':32,'new_static_checks':16,
      'classification':'KNOWN_CONTRACT_CORROBORATION','new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
      'server_packet_primary_modified':False,'native_GUI_accepted':False,'skill_owner_or_player_ready':False,
      'wholeprogram_indirect_owner_absence_proven':False,'prior_primary_pins_preserved':13}
    complete=load('completeness-audit.json');complete['status_family_50_51_52_56_batch']=summary;write('completeness-audit.json',complete)
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第十二批S50/S51/S52/S56：')[0]
    reader.write_text(text+'\n第十二批S50/S51/S52/S56：status-family-50-51-52-56-contract.json + status-family-50-51-52-56/READ-ME.txt。四handler body8/6/6/3，各兩dispatch；18newraw、72new+16inherited=88指令斷言，8既有frames32欄位、16newaudit-onlycases。baselineJava相符，newwire0／wirecorrection0／packet不改。保留rawcopy與Java signedviews分級、numericpatchedprofile邊界，三handlerEAX消費指標不等於length guard。owner/player/GUI未驗收，13priorpins保留。\n',encoding='utf-8')
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH12_S50_S51_S52_S56_STATIC_CONTRACTS_SEALED',entrypoint=record(reader),
      status_family_50_51_52_56_contract=pin,finite_four_status_handler_audit_complete=True)
    added=[pin,record(readme)]+report['reusable_outputs']
    for group in ['modern','launcher']:
        mapped={x['path']:x for x in handoff[group]};mapped.update({x['path']:x for x in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['server']['status_family_known_contract_corroboration']=pin
    handoff['next_steps']=[
      'Supervisor/Modern consume exactlyS50/S51/S52/S56 body8/6/6/3 fields andglobaldestinations; fixedrawcopy evidence differsfromJava signedviews. NextS50 layoutSTR/INT/WIS/DEX/CON/CHA/weight.',
      'Three numeric handlers preserveEAX endpointer withoutlengthvalidation. Decode orstatpublication doesnotqualifyowner/actor/player/GUI.',
      'ExistingbaselineJava and8sealedAD368frames32fields match. newwire0/newopcode0/wirecorrection0; preservedefaultpacketdelta and13priorpins.',
      'Finitefour-opcodebatchends here; retainnumericpatchedprofile,optionaldispatchlogging,liveglobal/downstream/ownerunknowns. Nootheropcode/wholeprogram oroldPASS/native/SQL expansion.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],
      current_task='Batch12 exactlyS50/S51/S52/S56 finitecontractssealed;8baselineframes32fields corroborated, noowner/player/GUIacceptance orwirechange.',status_family_50_51_52_56_summary=summary)
    progress['handoffs']['modern']='status-family-50-51-52-56-contract.json + fourtypedlayouts/body8/6/6/3 + 8sealedframes; rawcopy/domain evidencelevels separate, noowner/playerready'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    assert record(ROOT/'server-packet-delta.json')==packet
    names=['status_family_50_51_52_56_audit.py','publish_status_family_50_51_52_56.py','status-family-50-51-52-56-contract.json',
      'completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'status-family-50-51-52-56').rglob('*')if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'report':pin,'delta_files_checked':count,'prior_pins_preserved':13,'delivery':record(ROOT/'delivery-verification.json'),
      'packet_unchanged':packet,'new_wire_differences':0,'skill_owner_or_player_ready':False},ensure_ascii=False))

if __name__=='__main__':main()
