"""Publish finite Batch24 mode/map effects, existing raw only, zero new wire."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('D5-mode-map-receive-contract.json');pin=record(ROOT/'D5-mode-map-receive-contract.json')
    review=load('D5-mode-map-receive/independent-review.json');v=report['verification']
    assert review['all_match'] and review['primary']==pin
    assert (review['check_count'],v['new_code_ranges'],v['new_data_ranges'],v['inherited_necessary_raw_slices'],v['distinct_reused_raw_pins'],v['instruction_assertions'],v['aligned_in_slice_branch_targets'])==(215,0,0,15,9,42,7)
    for q in report['sources']+list(report['existing_evidence'].values())+report['outputs']:
        assert record(Path(q['path']))==q
    for q in report['prior_primary_pins']:
        p=record(Path(q['path']));assert p['bytes']==q['bytes'] and p['sha256']==q['expected_sha256']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'D5-mode-map-receive/READ-ME.txt'
    reader.write_text('''第二十四批：Native S_D5 subtype2(mode)／subtype3(map)有限接收完整分支

主入口 ../D5-mode-map-receive-contract.json。
本次所有所需raw均已封存於Batch18/23，無新PE範圍或bin副本。只補完整路徑、typedstore、UI邊界與欄位區分資格；原gate/mapprojection/reader沿用，不重跑舊PASS。
15必要raw slices，來自9份既有rawpins，slice有原sourceSHA/offset/size、VA/RVA/fileoffset/rawhex/逐指令與originalindexpin。
固定PE7094272 SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9，base400000。
42指令斷言、7 aligned branches、215獨立checks全match。Mode完整32bytes，map完整49bytes；25priorprimarypins保持，包括Batch23 8A6C74A34CFAC1BC1AFF04215F80E347853069ACFA611C076269443B4671DFD8。
沒有Native/GUI/input/SQL/provider/process/newcapture、Java/Modern/BIN改動、WORLD/private/external/virtual body擴張；不接server攔截工作、不重掃captures、不重跑Batch18/20/21/23或舊PASS。

共同gate（沿Batch18/23）
既有D5 primary/fallback→4B2F67→4A9E50；header cd从body+1讀subtype BYTE(EBP-1)與roomDWORD(EBP-8)，payload body+6。
phase64AF98==6，4A9E7F先把incoming room寫64ABF4，再檢subtype；不對原room做相等gate，invalid subtype亦可能先覆寫room。
其它phase incomingroom與64ABF4不等→4AA06C return，相等才dispatch；這裏沒有額外phase whitelist、owner/self/ready check。
MOVSX i8 subtype後unsignedJA>9只容0..9。Mode2與map3各自header/gate/return，不是atomicpair。
這些finite paths無explicit63D54C version predicate；上游dispatch/actualruntimeversion仍是繼承的條件資格。
Modern先以自有connection/epoch與CE建立的currentroom驗證，不能把Nativephase6overwrite當source/roomauthority。

Subtype2 mode，entryVA004A9F6A / RVA000A9F6A
Body d5 02 + roomDWORDu32LE + modeu8，exact已知producerbody7/frame9；沒有文字/codec。
4A9F6A push64ACCA；4A9F6F jmp4AA05B；4AA05B inputESI+=6、push"c"@5025A8；4AA064 call4A2A50(input,c,64ACCA)；4AA070return。
typedreader4A2A93是BYTE store，僅64ACCA低BYTE被寫，64ACCB高BYTE保留。不做domainmask、signconversion、valueclamp或WORD/DWORD清零。
mode值80/FF仍是raw BYTE store條件，不因此授予server合法mode或NativeactualUI模式資格。
共有tail的入口是4AA05B，跳過4AA03B/4AA04C；其它subtypes的4A9030 edge不在mode2路徑中。此路徑只有typedreadercall，沒有directUI/private/external/virtual call。
Mode更新不重算64ACC9、不讀rawmapWORD、不載入defaultmap/provider或查mode-mapmembership。
即使mode變動，原mapBYTE可保持；不要合成subtype3、CC/preview刷新或自動rollback。

Subtype3 map，entryVA004A9F74 / RVA000A9F74
Body d5 03 + roomDWORDu32LE + mapWORDu16LE，exact已知producerbody8/frame10；沒有文字/codec。
4A9F74 localEBP-4，4A9F81 call4A2A50(body+6,"h"@632594,localWORD)，typedreader4A2AB1明確LE WORDstore。
4A9F86 cleanup後fallthrough4A9F89，是Batch23已封存的subtype0共用mapprojectiontail；4A9F9B存64ACC9 BYTE，4A9FA4return。
先比整WORD==FFFF或FF9C，這兩者採原低BYTEFF/9C；其它對AL加0C並mod256。無lookup/index/tablecount、locale/version/modebranch或specialmode減法。
Wire514(0202)→projected0E；wire564(0234)→40；wireFF9C→9C。例子只是computedoperand，不是capture或resourcebinding。
RawWORD只在此packet/local，這條路徑不另存global完整WORD。64ACC9是lossyprojection，Modern應另保留rawWORD與來源context。
Map更新保留mode低BYTE64ACCA與相鄰高BYTE64ACCB；沒有directUI/private/external/virtual call，只typedreader與直接BYTEstore。

Reader／保留欄位／UI邊界
只沿用typedreader commondispatch、c/h store、formatloop/return與char/jumptables；沒有s/string/backend call或body擴張。
c讀1byte→store1byte→cursor+1，h組littleendian2bytes→storeWORD→cursor+2；EAX返回cursor。Handler無bodylength/exactend驗證，actualinputavailability/runtimepointers仍unknown。
兩条有限branch都不明確改roomname64AB70、ownerdisplay64ABCA、attributes64ABE6、password64ABE8、capacity64ABF0、summary0_64ACCC、summary1..4_64ACD0..D3、options64ACD4..D7、selections64ACD8..DF。
沒有ready/member/source/physical/sessionidentity或phase轉移store；roomglobal64ABF4只在繼承headerphase6gate寫入。
「保留」指這些finite c/h storepaths無對應destination，不是全image無writer或actualruntime/UI outcome聲明。
此批沒有碰到需要下探的private/external/virtualcallee，直接callee到已資格typedreader及return即止。ActualGUI/menu/mapresource仍不合格。

Mode / wireWORD / D5BYTE / catalog / preview 必須分開
mode低BYTE64ACCA ≠ 整WORD64ACCA：Batch21 UIselector條件讀zeroextendedWORD，高BYTE64ACCB當前值未知，不能默認為0。
wiremapWORD ≠ D5global64ACC9 BYTE：後者截斷/加0C，沒有逆向binding。
D5global64ACC9 ≠ CCcatalogrow+74：Batch20新CC lookupindex&FF或舊projection只屬catalogrow路径；這裡沒有CCappend/reset/row修改。
UI64A958 catalog ≠ UI64A98C selectedpreview：Batch21 4658B0→4687D0把selectedrow+74傳preview+0F，preview+0C是另外roomWORD；D5sub2/3沒有直接寫這些fields或call這條consumer。
64AFD0/64AFE0 currentgeneration/count/order、LTmapstr filekey/physicalordinal/labelcodec與map/WORLDresourceID皆保留unknown，不從D5BYTE或wire514/564對上候選列。
ready採獨立已資格D1state0，owner/source/physical/session資格保持；D5mode/map不授ACK、readyreset、owner成功、pair或transactioncoherence。

Server只讀來源對照；無dynamic sub3 witness
server-ordinary-room-map-selection-contract.json 5713bytes SHA3D0B5BAA46238ED5589C193FBDE22B966B945EFDEAB70D2A2C3933A62CA8E15F。
detail map-selection-contract.json SHA BF77DF02B0D6A7AE7892EA7DD0B173AE9A65E8627B08EE8EF4B8D48C2C52FEFA。
既有D5 layout server-room-D5-CF-consumer-supplement.json SHA7774EA7546EEF33CB5240A58A36FA9FFF7A465303049C3C3E238081887279BEF。
Server static成功map path：selectedMapId/roomready/reconfigure，再sessionready D1reset，S_D5sub3 requestedmap broadcast；noatomic/allpeerdelivery資格。
非法out-of-pool/FFFF：self S_D5sub3，值來自BattleRules.decode(settings)第一poolmap，不做hubupdate/readyreset；與acceptedbroadcast一樣8body/10frame格式，沒有ACK/結果/flowrole欄位。
Validmap但hubfalse則selfD5summary0/config1，不是sub3restore。Native只看incomingD5gate/subtype/value，不知道C_D1guard或request outcome。
Server既有staticcounterexample：modeDEATHMATCH、已選514、非法requestFFFF，restore回564但hub仍514。這是已接納的conditional value mismatch候選修正，非本批新增wirelayout/opcode correction；不改server/source/JAR、不重複通知封包更新。
Native收到514會投影0E，收到564會投影40。這僅computedstore差異，不證接受/拒絕、hubrollback、transactioncommit或實際UI結果。
保留received-vs-expected conflict未知；不自動rewrite收到map、不retry、不依D5製造rollback/ACK。
Server report只有7組finite selectedcaptures的C_D1sub3/S_D5sub3皆0；本批只採用此已封存scope，不重掃capture、不宣稱全server無紀錄。
7774 mode2來源只有builder/source/actualIL layout，沒有mode runtime資格；mapcontract本身排除C_D1sub2 mode-step unionarm，不追其serverproducer/action。
所有computed例明確actual_trace=False，沒有saved-field-witnesses/frames.json。本批capture0。

封包與交接分類
Known S_D5=213，sub2 fields:u8subtype/body1、u32LEroom/body2、u8mode/body6，body7/frame9。
Known S_D5 sub3 fields:u8subtype/body1、u32LEroom/body2、u16LEmap/body6，body8/frame10。VA/RVA/SHA、trigger/state、等級/未知在wire-contracts.json。
C_D1sub3 body4/frame6只作server既有parserdirection對照，不是新Nativeproducer或本批wire採用；S_D5不帶request/role/generation/成功或拒絕碼。
新wire/newopcode/wirecorrection/newcapture/newPE均0；server-packet-delta 26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C保持。
Modern可消費獨立epoch/currentroom qualified modeBYTE與rawmapWORD投影，保留相鄰fields、summary/config區別、catalog/preview/resource/ready/owner/sourceauthority邊界。
本批metadata和有限檢查完成，監製進度更新；未知runtime/UI/resources/ACK/rollback與server失敗/partialsend/concurrency處即停。
''',encoding='utf-8')
    summary={
      'evidence':pin,'finite_D5_subtype2_3_complete':True,'runtime_UI_resource_ACK_rollback_guard_retained':True,
      'new_code_ranges':0,'new_data_ranges':0,'inherited_necessary_raw_slices':15,'distinct_reused_raw_pins':9,
      'instruction_assertions':42,'aligned_in_slice_branches':7,'independent_checks':215,
      'mode_complete_path_bytes':32,'map_complete_path_bytes':49,'mode_effect_examples':3,'map_effect_examples':3,
      'prior_primary_pins_preserved':25,'dynamic_subtype3_witnesses':0,
      'mode_highBYTE_and_mapBYTE_retained':True,'map_updates_mode_retained':True,
      'direct_UI_calls_in_subtype2_or3':0,'ACK_or_automatic_rollback_qualified':False,
      'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'new_capture_frames':0,'packet_modified':False,
      'Native_GUI_provider_SQL_Java_Modern_BIN_modified':False,'old_PASS_or_captures_replayed':False,
    }
    complete=load('completeness-audit.json');complete['D5_mode_map_receive_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';text=entry.read_text(encoding='utf-8').split('\n第二十四批D5 mode/map：')[0]
    entry.write_text(text+'\n第二十四批D5 mode/map：D5-mode-map-receive-contract.json + D5-mode-map-receive/READ-ME.txt。全部已有raw，0newPE/15必要slices/9distinctpins/42operand/7branch/215checks/25priorpins。Sub2只寫64ACCA mode低BYTE，保留高BYTE/原map投影；sub3localmapWORD→共用64ACC9BYTEprojection，保留mode；兩者沒有directUI/catalog/preview/ready/owner/source改寫。Serveracceptedbroadcast/illegalrestore同D5sub3格式，existingstaticfirstmap-vs-current候選差異不授ACK/rollback；無dynamic sub3witness。Newwire0/packet不變，有限封存即停。\n',encoding='utf-8')
    phase='BATCH24_D5_MODE_MAP_RECEIVE_SEALED_RUNTIME_UI_RESOURCE_ACK_ROLLBACK_OPEN'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),
                                 D5_mode_map_receive_contract=pin,finite_D5_subtype2_3_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'D5-mode-map-receive').iterdir())if p.is_file()]
    for group in ('modern','launcher'):
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if q['path']==str(ROOT/'completeness-audit.json')else q for q in h['shared']]
    h['server']['known_D5_mode_map_receive_boundary']=pin
    h['next_steps']=[
      'Batch24 D5sub2/3 finitebranches sealed usingexistingraw only. Sub2modeBYTE→64ACCA retains64ACCB and64ACC9, no mapreprojection/UIcall. Sub3u16LE localWORD→sharedBatch23lowBYTEprojection64ACC9 retainsmode, noUI/catalog/previewstore.',
      'InheritedD5phase6roomoverwrite/otherroom-equalitygate applieseachmessage independently; Modern uses ownedepoch/currentCEroomguard. RawmapWORD/projectedBYTE/catalogrow+74/previewWORD+0C/BYTE+0F/table/resource remain separate.',
      'Serverexisting3D0Bmapcontract acceptedbroadcast andillegalrestore use sameS_D5sub3layout, noACK/result/rollbackfield. Existingconditionalrestorefirstmap564vscurrent514 mismatch remainscandidateonly; noserver/JAR changes orrepeatpacketnotification.',
      'No dynamicC_D1sub3/S_D5sub3 witnessadopted; examplescomputedonly, noframesrescan/capture. ReadyD1/owner/source/physical/session/GUI/resourcebinding retains independentqualification; sendfailure/partialdelivery/concurrency/atomicityunknown.',
      '25priorpins unchanged; 0newPE/15necessaryslicesfrom9pins/42assertions/7branches/215checks. newwire/opcode/correction/capture0 andpacketunchanged; Native/GUI/provider/SQL/Java/Modern/BIN0. OldPASSnotreplayed, serverinterceptedworknotresumed; finitebatchstop.',
    ]
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json')
    progress.update(updated_at=stamp(),phase=phase,
                    current_task='Batch24 NativeD5mode/map completefinitebranches usingalreadysealedraw; directtypedstores only, noUI/ACK/rollback/resourceauthority; serverstaticvalueconflict retained.',
                    D5_mode_map_receive_summary=summary)
    progress['handoffs']['modern']='D5-mode-map-receive-contract.json: exactsub2modeBYTE/sub3rawmapWORD reception, shared64ACC9projection, highBYTE/otherstate preservation; separatecatalog/preview/resource, noACK/automaticrollback/dynamicwitness.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['D5_mode_map_receive_audit.py','review_D5_mode_map_receive.py','publish_D5_mode_map_receive.py',
           'D5-mode-map-receive-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in (ROOT/'D5-mode-map-receive').iterdir()if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),
                      'prior_primary_pins_preserved':25,'new_PE_ranges':0,'packet_unchanged':packet,
                      'runtime_UI_resource_ACK_rollback_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
