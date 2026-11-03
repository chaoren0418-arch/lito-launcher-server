"""Seal batch13 evidence index without modifying accepted lifecycle or packet contracts."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('reconnect-source-index-contract.json');v=load('reconnect-source-index/new-evidence-verification.json')
    assert v['new_code_ranges']==0 and v['new_data_ranges']==1 and v['inherited_raw_ranges']==18
    assert v['assertion_count']==65 and v['confirmed_literal_operands']==25 and v['unmatched_candidates']==0
    assert len(v['prior_primary_pins'])==14
    for p in v['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['reusable_outputs']:assert record(Path(p['path']))==p
    for p in report['existing_evidence'].values():assert record(Path(p['path']))==p
    packet=record(ROOT/'server-packet-delta.json');assert packet['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'
    pin=record(ROOT/'reconnect-source-index-contract.json');readme=ROOT/'reconnect-source-index/READ-ME.txt'
    readme.write_text('''第十三批：64AD74 saved host／64AD54 character alias 來源索引

主入口：../reconnect-source-index-contract.json
這個來源已在host-alias-lifecycle.json與receive-boundary證據大部分解明，本批直接索引原raw，只補未分級的來源/clear/capacity/encoding缺口。
原始SHA：
fixedPE 4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9
BIN CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292
ASM 2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A
舊host-alias-lifecycle.json CA359ECAFEAA2BD97239414858F62C02C9AD9DCBEA728921C7E62EBFD66EB644保持不變。
18份existing raw逐byte核對fixedPE；65項固定expected指令斷言。0newcode ranges，只有1newdata range：502498 ASCII cs+NUL共3bytes。25個confirmedaligned operands（四地址pattern）全部由既有已知entry decode對上，不從unaligned literal byte猜指令，也沒有重跑oldproducer/PASS。

儲存地址與pointer：核心區分
64AD74：inline256-byte saved startup/master host區，通過立即地址掃描/傳入host API；不是角色名稱，也不是globalDWORD pointer。
64AD54：inline角色別名區到64AD70之前共28bytes，IDA7DWORDlabel只是資料宣告樣式，使用方式是byte string而非pointer。
64AD70：2-byte current reconnectport，低16bit保存，有些caller用MOVSX；不是name length。64AD72..73是2alignmentbytes。
64AE74：current connect host區，saved host可copy進它。
64B7E4：inline256-byte mutable startup host；defaultlist/positional/provider改它，不等同savedhost自動同步。
Defaultsource另有真正pointer：480BA4 EAX=[64B498]是address-list pointer，480B9E用64B49C listcount，480BAC存64B4A0 index，480BB2取[listpointer+index*4]的string pointer；這些不是host length。

Host取得事件／來源
已知direct writer：47C8A0 login/initworker，在47C9B7 private050104CD返回之後，47C9BC掃startup64B7E4；47C9C6 repneSCASB／notECX得到含NUL長度，47C9D0 EDI=64AD74、47C9D8 repMOVSD＋47C9DF repMOVSB複製含firstNUL的bytes。
Startup defaultlist於480BC1/BC9/BD0→64B7E4；first nonoption positional token於480C77/7F/86→64B7E4；provider nonempty64B950於481861/69/70→64B7E4。這些directcopy已是舊證據，本批不重跑producer/worker。
47CAD0的worker dispatch/完成仍只為既有sourceorder，native實際thread/worker執行與hostvalue未驗收。
來源候選特別限定：舊報告另列481DC7..481DEB為COM-derived來源，但fixedfragment若從481DC7進入，先xorESI／movEDI,ESI產生EDI0，再481DD2 byte scan；此片段不能證明取得有效COM字串。上游control/registersetup/private轉換是否另有條件未閉合；Modern不能把這個候選當成已成功產生host的source。本批保留舊報告pin，只以新索引限定原本未閉合的語義。

Host直接讀者
469B4C掃savedhost，469B67以64AE74為destinationcopy含NUL，用於master-returntimer；46F13B掃savedhost，46F152→64AE74copy，用於returnmode2。
4760FF/476113把inline64AD74地址傳4EFE56/4EFE5C；已有inet_addr/gethostbyname identity inference。Raw targets分別758F57E0、7591FF10，但originaldump-timeDLL SHA/body不明，沒有重新export解析/DNS/native執行。
startup64B7E4另有4806F6→480703 external773F5200 formatting/read call，本來已在receive-boundary raw；僅補索引，old sprintf inference不等於actual成功或可寫入hostname。

角色別名取得／返回C9
4928C0 ordinaryselection：492922/25/31 arithmetic建立EBX=62FAA0+selectedindex*B4。此選中roster record的base bytes是savedaliascopy source，不是64AD74或incoming packet body。
492F02 ordinaryC9 senderreturn之後，492F33 EDI=EBX，492F38 scanfirstNUL；492F42 EDI=64AD54，492F4A repMOVSD、492F51 repMOVSB保存含NUL字節。舊raw另有同源75F71C copy。新索引不展開上游rosterpopulation、selectionindexcallee或其未知codepage/Sopcode。
Cachedreturn4A7550：64AFBC/64AFBD分支→4A769A且AFBE==0時，4A76A3 push64AD54、4A76A8 pushC9、4A76AD push502498(cs)、4A76B2→4C6AE0；返回後4A76BA AFBE=1。
這證明C_C9格式/aliasargument來源；它沒有ordinaryselection的owner建立/reset/readiness檢查。Sendercompletion、sameconnection/character/owner epoch及實際inheritedowner均不能由cachedalias推論。
4C6AE0原body沿用，必要semantic終點4C6B00→4C68D0 second-layerformatter不再展開；本批不新宣稱s string codec或實際wirebody完成。既有C_C9補證不是新增opcode/wire更正。

編碼、NUL、最大長度、生命期
兩條copy都是8-bit SCASB/MOVSB/MOVSD，保留raw高bitbytes，firstNUL包含在copy；本地無Unicode/UTF8/CP950轉換。Host old API名為char* inference，但上游codepage/IDN規則未證；aliasrosterencoding及legalcharlimit未證。28/256都是bytes，不能當中文字數。
Savedhost256-byte到下一currenthost的span，NUL後inbounds payload255bytes；savedalias28-byte到port的span，payload27bytes。這是鄰接地址/ASMlabel及raw使用所得regionbytebudget，不是originalPDB/allocator type或nativeforcedmax驗收。本地scan/count沒有容量clamp；upstreamvalidator/stringcapacity仍未知。
Copiedbytes獨立於sourcepointer生命期。Laterstartuphostoverwrite不自動刷新savedhost；laterselectioncopy覆寫aliasprefix通過NUL，不清整個tail。Source record pointer无需保留來讀已保存bytes，但copy成功/epoch仍須獨立證據。
四地址fixedliteralinventory：64AD74=5、64AD54=2、64AD70=11、64B7E4=7，共25；所有candidate均對上已知aligned指令operand。Savedhost/name沒有在這些directrefs中被clear；此absence不是wholeprogramalias/bulk/privateclear排除證明，不能宣稱neverclear/perpetuallifetime。
唯一相關直接clear：47C9E1 ECX10、47C9E6 EDI64AD2C、47C9EB repSTOSD，EAX由先前xor保持0。半開範圍[64AD2C,64AD54)共40bytes，只清channel table，恰好停在name起點；不清name/port/host。AFBEflagreset也不能視為namebuffer清除。

原始／分析PE差異
固定PE的64AD54/64AD74/64B7E4 bytes皆零；ASM560604另一recoveredinput記錄210.200.182.14,NUL。BIN原packedinputhash只identify來源，沒有證明原版livebuffer、startupendpoint或charactername值。
不得把固定snapshot空字串、ASM異imageliteral、fixtureendpoint當成Modern正式地址/角色身分。

精確停止邊界
47C9B7→050104CD：fixedPE無body/SHA，return與hostcopy完成未知。
481836 indirectCALL EAX：providertarget及64B950 outputencoding/body未知。
480703→773F5200；4EFE56→758F57E0；4EFE5D→7591FF10：externalbody unavailable，historicalDLL SHA未知；只引用已接受staticidentity inference，不重跑DLL/exports或livecall。
4C6B00→4C68D0：raw-backed second-layerformatter未展開；另4C6AE8→4F11A0、4C6B27→4C6770保留senderstack/transport邊界，不聲稱發送成功。
Upstreamrosterpopulation/codepage、494120selectionindex取得不展開。範圍只到savedbuffer直接資料流，不擴wholeprogram/S0或攔截server工作。

Modern可用／不可推論
可將savedStartupHost bytes、savedCharacterAlias bytes、port16分開，保留source與connection/character epoch，將255/27作inbounds bytebudget、字串截至firstNUL；這是資料流交接，沒有代改Modern。
不可從copy推出CP950/UTF8/合法字數或nativeforcedmax；不可用snapshot/ASM給預設endpoint；不可由cachedC9或bufferpresence宣告owner/player/GUIready；不可將未闭合COM/provider來源當成successfulsource。

封存與wire分類
KNOWN_CONTRACT_INDEX_AND_BOUNDED_SOURCE_QUALIFICATION，newwire0/newopcode0/wirecorrection0。server-packet-delta原SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C維持。
6newstaticmodels只核對255/256、27/28 payload+NUL區域邊界、opaquehighbytesfirstNUL、tableclear邊界，不是假nativevalidation或oldPASS重播。
14priorprimary/ranges/snapshotpins及5份existinglifecycle/API報告保持。Historicalprebatchmanifest/deliverypins是歷史彙總，不是postpublishcore drift。
只寫本組。native/observer/liveprocess/input/injection/screenshot、Java/SQL/oldPASS/producer/newcapture/archive、他組改動均0；原worker、livehost/alias、owner/player/GUI/overallunpacking未驗收。Finitebatch完成即停。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_reconnect_source_index_complete':True,'existing_host_source_reused':True,'new_code_ranges':0,
      'new_data_ranges':1,'new_data_bytes':3,'inherited_raw_ranges':18,'instruction_assertions':65,'confirmed_aligned_operands':25,
      'new_static_models':6,'host64AD74_vs_alias64AD54_separate':True,'port64AD70_not_length':True,
      'COM_candidate_success_unproven':True,'native_capacity_guard_or_codepage_proven':False,
      'classification':'KNOWN_CONTRACT_INDEX_AND_BOUNDED_SOURCE_QUALIFICATION','new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
      'server_packet_primary_modified':False,'native_GUI_accepted':False,'skill_owner_or_player_ready':False,'prior_primary_pins_preserved':14}
    complete=load('completeness-audit.json');complete['reconnect_source_index_batch']=summary;write('completeness-audit.json',complete)
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第十三批reconnect source index：')[0]
    reader.write_text(text+'\n第十三批reconnect source index：reconnect-source-index-contract.json + reconnect-source-index/READ-ME.txt。64AD74 inline256byte savedhost與64AD54 inline28byte alias分開、64AD70是port；mainwriter/raw已有證據，18existingraw/65指令/25alignedoperand索引，0newcode/1new3-bytecsdata。NUL/copy/tail/clear/255與27bytebudget分級；481DC7candidate不能證成功COMsource，encoding/nativeforcedmax/indirectclear/currentepoch未知。newwire0、packet不改、14priorpins保留。\n',encoding='utf-8')
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH13_RECONNECT_HOST_ALIAS_SOURCE_INDEX_SEALED',entrypoint=record(reader),
      reconnect_source_index_contract=pin,finite_reconnect_source_index_complete=True)
    added=[pin,record(readme)]+report['reusable_outputs']
    for group in ['modern','launcher']:
        mapped={x['path']:x for x in handoff[group]};mapped.update({x['path']:x for x in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['server']['reconnect_known_source_corroboration']=pin
    handoff['next_steps']=[
      'Supervisor/Modern consumeexisting-sourceindex:64AD74savedhost inlinebytes versus64AD54savedselectedalias and64AD70port; cachedC9alias isnotowner/player readiness.',
      'Keep255host/27alias inboundsbytebudgets separatefromunprovennativeforcedmax/codepage/legalcharlimit; no defaultendpoint fromzeroPE/otherASM literal.',
      'Do not qualify481DC7 COMcandidate as successfulhostsource: fixedfragmentsetsEDI0 beforebyte scan. Private050104CD/provider/serializer/currentepoch/aliasclear gapsremainlisted.',
      'Finiteindexcompletedwith18reusedraw,65instructionassertions,25alignedoperands,0newcode/1formatdata. newwire0,14pins/packetpreserved; nooldPASS/native/SQL/deeperrosterprotocol work.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],
      current_task='Batch13 existingreconnectsourceindexsealed; savedhost/alias/port distinguished, COMcandidate/encoding/nativeforcedmax/clear/epoch remain bounded unknown.',reconnect_source_index_summary=summary)
    progress['handoffs']['modern']='reconnect-source-index-contract.json: existinghostwriter/source indexed, aliasrecord/cachedC9 source and25literaloperands; capacity/codepage/COM/owner/livevalues unqualified'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    for q in report['existing_evidence'].values():assert record(Path(q['path']))==q
    assert record(ROOT/'server-packet-delta.json')==packet
    names=['reconnect_source_index_audit.py','publish_reconnect_source_index.py','reconnect-source-index-contract.json',
      'completeness-audit.json','handoff.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'reconnect-source-index').rglob('*')if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'report':pin,'delta_files_checked':count,'prior_pins_preserved':14,'delivery':record(ROOT/'delivery-verification.json'),
      'packet_unchanged':packet,'new_wire_differences':0,'skill_owner_or_player_ready':False},ensure_ascii=False))

if __name__=='__main__':main()
