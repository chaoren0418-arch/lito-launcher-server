"""Seal finite Batch27 map selected-key request evidence with UI gaps retained."""
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD1-map-outbound-contract.json');pin=record(ROOT/'CD1-map-outbound-contract.json')
    review=load('CD1-map-outbound/independent-review.json');v=p['verification']
    assert review['all_match'] and review['primary']==pin
    assert (review['check_count'],v['new_code_ranges'],v['new_data_ranges'],v['necessary_reused_raw_ranges'],v['instruction_assertions'],v['prior_primary_pins_preserved'])==(214,10,3,6,47,28)
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']+[review['computed_models']]:assert record(q['path'])==q
    for q in p['prior_primary_pins']:
        a=record(q['path']);assert a['bytes']==q['bytes'] and a['sha256']==q['expected_sha256']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'CD1-map-outbound/READ-ME.txt'
    reader.write_text('''第二十七批：Native C_D1 subtype3 map selected-key 靜態 callsite

主入口 ../CD1-map-outbound-contract.json；wire-contract.json提供方向/opcode/欄位/長度/端序/VA-RVA/來源SHA/觸發條件/證據等級/未知。selected-key-and-unknown-matrix.json列每段邊界。
固定PE7094272 SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9，base400000；ranges.json有VA/RVA/fileoffset/rawhex/逐指令與既存rawpin。
10新code+3data ranges；6必要既存raw引用，47operand assertions，214獨立checks全match；28priorprimarypins保持。無新capture/Native outbound witness。
只讀固定PE/ASM導航與既存server交接；只寫本組ROOT。無Native/GUI/input/SQL/provider/Java/Modern/BIN變更，無unknown private/external/virtual body展開、他人程序或被攔截server工作，無舊PASS/cipher/GUI/capture重播。

既存等價交接先查
沿map-table-source/map-candidate-index/D5-mode-map與server-map-selection；前者UIselector停465110、後者D5明確不授Native producer。既存450B90 selectedlookup與450BC0 row基底初始化有完整raw，直接重用。
固定ASM exact push0D1h僅有限導航兩候選：464E28 map，46B9F4 adjacent mode-step。不宣稱全PE direct/indirectcaller completeness。

完整可見的有限 handler selected-data→sender 邊界
464AD0 inputECX→ESI，讀物件DWORD+68；unsigned>13退，14DWORD jump table464EE4的case7=464DF1。其它case不展開。
case7先464DF3→450B90找selectedindex，negative跳464E30不send；非負時464DFE再次呼叫450B90，讀ESI+58 array、array[index] itempointer、itemDWORD+4到EAX。
450B90完整既存37bytes：countECX+2C、pointerarray+58，依序找第一個itemBYTE+0C非零，沒有選取返回FFFFFFFF。沒有內部callee。
實際event/callback、物件kind7 producer、UIclick/旗標+0C寫入、有效記憶體/count/array/物件身份、兩次掃描間generation或穩定性仍未知；此有限handler不證完整UI接受。
從case7入口到4C6AE0之間沒有未知call；這是有條件的靜態來源到已合格sender邊界，非actualruntime送達。

Opcode／欄位／端序／sentinel是request用值
464E0C CMP EAX,FFFFFFFF；464E11 CMP EAX,FFFFFF9C。命中任一直接到464E1B；其餘464E16 ADD500，32bitwrap。
464E1B pushmapDWORD；464E1C push3；464E1E pushD1；464E23 push62C078 literal cch；464E28 call4C6AE0，RVA00064E28。
以Batch26已合格c/h serializer：D1低BYTE、3低BYTE、mapDWORD低WORD littleendian。Body4，frame6，prefix u16LE總長6，無roomID/字串/codec欄位。
FFFFFFFF作參數不是wireDWORD欄位，它確實序列化成FFFF；FFFFFF9C確實成FF9C。這兩種在此request路径可見，不能只當D5接收projection。
非sentinel item0000FFFF會先+500，wire01F3（bytesf301），與itemFFFFFFFF不同；不用原值直接截斷取代native轉換。
Numeric source為完整DWORD bit pattern；table/index/selector的signed comparisons不授實際producer合法域或resourceID。
Computed examples：item14→map514 d1030202；item64→map564 d1033402；FFFFFFFF→d103ffff；FFFFFF9C→d1039cff；FFFFFFFE→map498 d103f201。
全部computed，不是Native執行或新frames；computed-request-source-models.json另有高位截斷、選取空列/多列/80/FF旗標模型。
Formatter/frame/transport只引用Batch26契約与必要c/h及frame raw，不重跑serializer/cipher旧PASS。實際transport仍停4EFEA4 rawE9F7594175 JMP external759058A0，無delivery/ACK。
Send後或未選取的464E30 cleanup，若64B094非零，464E40 rawFF10 callvirtualslot0是第一個未知，停止；不資格cleanup效果或UI結果。

從table row+20到item尚有缺口
既存maptable物件64AFD0 count、64AFE0 recordpointer、stride36；rowDWORD+20是候選key，+4 selector對zeroextendedWORD64ACCA，+0 labelpointer。
既存46B926..46B9D0有限island有條件filter，再key-500作465110 arg1、labelpointer作arg2；不把offline檔行號或candidate token當live recordkey。
465110 prefix讀arg1→EDI；檢existing itemDWORD+4重複、最多256；46515D→4F118C allocationhelper是第一unknown，STOP不展開。加item完成、memory/ownership/entrycount更新均未授權推定。
獨立conditionalpostallocation465173..17E把key/label傳4630F0；constructor prefix在463114呼叫已合格450BC0，基底把argDWORD存item+4並把item+0C=0。
46313C→463B70是constructor剩餘第一未知，STOP。基底初值selection0不證後續selection/click；不橋接allocation、constructor completion、iteminsert或實際tablegeneration。
因此(key-500)+500可作conditional algebra modulo32，但非已證完整row→UI→request鏈。
Sentinel碰撞條件模型：tablekey499減500→itemFFFFFFFF→wireFFFF；tablekey400減500→itemFFFFFF9C→wireFF9C。只提醒這條algebra有例外，不宣稱runtime table含499/400或新server差異。
Actualtableprovider/load/runtime recordkeys、locale/資源binding仍沿Batch21/22 unknown，無重新extract/decode/archive/modelPASS。
Menu input2 prefix已知41B410 lookup後46B828 rawFF10 virtualslot0 STOP；alternative46B847 push70/46B849→4F118C allocation STOP。後段tablefilter是独立island，沒有跨越這些邊界。

相鄰C_D1 subtype2只列直接Native證據
46B7D0以inputECX0/1直接跳46B9E6/46B9E2，分別push step2/1；46B9E8 pushsubtype2，46B9EA pushD1，46B9EF literal5028CC ccc，46B9F4→4C6AE0（RVA0006B9F4）。
三BYTE body3/frame5。這是step operand，不是absolute mode value；inputUI控件/觸發、Java stepenum/union語義/權限/成功未補資格。
Servermapcontract明確排除subtype2unionarm，本批不擴servermode action、不據此要求Java新packet或Modern打斷receive工作。

與D5 projection、catalog、preview、候選資源保持各自來源
本request讀selected itemDWORD+4；沒有讀D5mapBYTE64ACC9、CCcatalogrow+74或preview+0F，也沒有直接讀offlinecandidate ordinal。
D5sub3收到u16→localWORD→lowBYTE+12，FFFF/FF9C有例外；這是獨立接收投影。數值相似不證request逆轉、同一runtime table或資源綁定。
不同UI物件可共用generic selectedlookup，不能把此kind7物件自動命名成64A958 catalog或實際64A968現存instance；upstreamobjectidentity未知。

既存server契約對照
server-ordinary-room-map-selection-contract.json 5713 SHA3D0B5BAA46238ED5589C193FBDE22B966B945EFDEAB70D2A2C3933A62CA8E15F；detail7116 SHA BF77DF02B0D6A7AE7892EA7DD0B173AE9A65E8627B08EE8EF4B8D48C2C52FEFA。
C方向D1/subtype3/u16LEmap exact4body/6frame，map unsigned0..65535；FFFF parse合法，membership後拒絕。room/ROOM/currentroom/owner/notbattle/ready等是servergate，Native有限island不授phase/owner/version/epoch。
Acceptedbroadcast S_D5sub3、illegal-map restore S_D5sub3、validgatefailure selfD5sub0/1是既知不同分支；本次不重做serverproducer/發包。
既存conditionalrestore default564 versus selected514差異保持原報告資格，actualtracefalse；沒有新修正、runtime/rollback推定或packetdelta。
既存七組finitecaptures C_D1sub3/S_D5sub3hits0只採用封存report，不重掃，不宣稱全server沒有request。沒有Native dynamicwitness。
D5無request/result/role/generation，因此非ACK/owner成功/atomiccommit/rollback/readyreset或allpeer送達保證。

封存交接
214checks獨立rawdisasm/operand、selectedlookup路徑、map/modevarargs與模型：7map、5selection、6conditionaltablecomposition、2adjacentmode。
28priorprimarypins不變；newwire/newopcode/wirecorrection/newcapture=0。server-packet-delta.json 26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變。
Modern日後mapwriter可引用C_D1sub3 exactlayout與有限source metadata；仍須其ownedepoch/currentroom/authority與UIaction規格，不能用本批S_D5/Native BYTE投影替代權限或資源選擇。
主契約/unknownmatrix/監製進度/handoff已更新。有限batch完成；UItrigger/tableproducer/ctorinsert/selection/phase/owner/transport/ACK未知即停。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_selected_key_to_map_sender_complete':True,
      'UI_table_producer_ctor_insert_selection_transport_authority_guard_retained':True,
      'new_code_ranges':10,'new_data_ranges':3,'necessary_reused_raw_ranges':6,'instruction_assertions':47,
      'independent_checks':214,'map_callsites':1,'adjacent_mode_callsites':1,
      'map_models':7,'selected_models':5,'conditional_table_composition_models':6,'adjacent_mode_models':2,
      'prior_primary_pins_preserved':28,'Native_outbound_dynamic_witnesses':0,'new_capture_frames':0,
      'request_FFFF_FF9C_static_use_qualified':True,'complete_row_UI_request_chain_qualified':False,
      'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'packet_modified':False,
      'Native_SQL_provider_GUI_Java_Modern_BIN_modified':False,'old_PASS_or_capture_replayed':False,
      'Java_change_extra_packet_or_Modern_interrupt_requested':False}
    complete=load('completeness-audit.json');complete['CD1_map_outbound_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第二十七批C_D1 map outbound：')[0]
    entry.write_text(txt+'\n第二十七批C_D1 map outbound：CD1-map-outbound-contract.json + CD1-map-outbound/READ-ME.txt + selected-key-and-unknown-matrix.json。464AD0 case7→464DF1兩次450B90選列、itemDWORD+4一般+500/sentinelFFFFFFFF或FFFFFF9C保留→464E28 cch D1/3/u16LEmap body4/frame6，FFFF/FF9C確有request靜態用途。Tablekey-500→465110先停allocation4F118C；conditionalctor450BC0 key+4 store後463B70未知，UIselection/caller/phase/owner/transport/資源仍未知。鄰接46B9F4 ccc D1/2/step2或1 only，不授absolute mode/serverunion動作；D5BYTE/CCrow74/preview0F/候選行號不綁request或resource。10code/3data/6reused/47assert/214checks/28priorpins保持，newwire0/packet不變，有限封存即停。\n',encoding='utf-8')
    phase='BATCH27_CD1_MAP_SELECTED_KEY_CALL_SEALED_UI_TABLE_SELECTION_TRANSPORT_AUTHORITY_OPEN'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),
      CD1_map_outbound_contract=pin,finite_selected_key_to_map_sender_complete=True)
    added=[pin]+[record(q) for q in sorted((ROOT/'CD1-map-outbound').iterdir()) if q.is_file()]
    for group in ('modern','launcher'):
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k] for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json') if q['path']==str(ROOT/'completeness-audit.json') else q for q in h['shared']]
    h['server']['known_CD1_map_outbound_boundary']=pin
    h['next_steps']=[
      'Batch27 finite Native kind7 selected-item key to sender chain sealed: 464E28 cch D1/sub3/u16LEmap exact4body6frame; itemDWORD+4 normally+500, FFFFFFFF/FFFFFF9C bypass→FFFF/FF9C actual static request values.',
      'Existing row+20 key-500→465110 prefix stops46515D allocation4F118C. Independent conditional ctor4630F0→450BC0 storesitem+4 then46313C unknown463B70. Actualruntime table/insert/selection+0C/UIcallback/objectidentity remain unknown; no full row-to-UI-to-request qualification.',
      'Menu prefix firstvirtual46B828 FF10 or alternative allocation46B849→4F118C; postrequest firstvirtual464E40 FF10. Accepted serializer reused, no oldPASS; actualdelivery external759058A0 boundary unchanged. Version/phase/owner/ACK/rollback/ready/physical/source authority unknown.',
      'Adjacent46B9F4 Native ccc D1/sub2 step2(input0) orstep1(input1),3body5frame only; not absolute mode or qualified Java union action/UI trigger. No extraJava packet/Modern interruption requested.',
      'D5sub3 BYTE64ACC9,CCrow+74,preview+0F,offlinecandidateordinal/runtime tablekey remain distinct; numeric similarity grants noresourcebinding. Server exactmap grammar and existing restore564vs514 conditionalmismatch unchanged; no dynamictrace/newwire/correction.',
      '28priorpins unchanged;10code/3data/6reused/47assert/214checks,7map/5selection/6conditionaltable/2modecomputedmodels;Native/GUI/SQL/provider/Java/Modern/BIN/newcapture0,oldPASS not replayed,packet unchanged. Finitebatchstop.',
    ]
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,
      current_task='Batch27 C_D1sub3 selected-item DWORD key→map sender static chain sealed, sentinel request use proved; full table/UIselection/phase/owner/transport remains unknown; adjacent mode-step only;zero newwire.',
      CD1_map_outbound_summary=summary)
    progress['handoffs']['modern']='CD1-map-outbound-contract.json/selected-key-and-unknown-matrix.json: exactC_D1sub3 lowWORD request, sentinelFFFF/FF9C use, tablekey arithmetic conditional only; UI/table/selection/transport/authority gaps retained. AdjacentD1sub2 Native step only.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['CD1_map_outbound_audit.py','review_CD1_map_outbound.py','publish_CD1_map_outbound.py',
      'CD1-map-outbound-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT)) for q in (ROOT/'CD1-map-outbound').iterdir() if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),
      'prior_primary_pins_preserved':28,'packet_unchanged':packet,'full_UI_table_selection_authority_gap_retained':True},ensure_ascii=False))
if __name__=='__main__':main()
