"""Seal Batch29 owned artifacts and handoff; no prior batch regeneration."""
import sys
sys.dont_write_bytecode=True
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-ready-outbound-contract.json');pin=record(ROOT/'CD2-ready-outbound-contract.json');r=load('CD2-ready-outbound/independent-review.json');v=p['verification']
    assert r['all_match']and r['primary']==pin and r['check_count']==292
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']+[r['computed_models']]:assert record(q['path'])==q
    for q in p['prior_primary_pins']:assert record(q['path'])['sha256']==q['expected_sha256']and record(q['path'])['bytes']==q['bytes']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'CD2-ready-outbound/READ-ME.txt'
    reader.write_text('''第二十九批：Native C_D2 ready/start outbound 有限固定PE契約

主入口 ../CD2-ready-outbound-contract.json；wire-contract.json列direction/opcode/field lengths/sources/VA-RVA/條件/未知；native-callsite-inventory.json有固定ASM11個literal pushD2命中（7packet／4位置參數），raw PE優先，非全映像indirectcall完整性。
16新code、1data（502778的cc NUL），1必要既存ctor35bytes，66operand assertions，292新有限checks全match，64新pure conditionalmodels；30priorprimarypins未改。Java-ready-reference.json僅採7必要既存IL方法、source/consumer pins；無Java執行或unknown body擴張。

固定PE七個發包islands（全call4C6AE0，c,c format）：
state0：46ACF4、46B51C、476AB4；state1：46B756、46B7A2；state2：407DFA、407E5E。
參數immediate opcodeD2 + BYTEstate0/1/2，C方向body2/frame4，既存sender總長u16LEprefix0400。沒有room/epoch/name/owner/source/requestID。
46ACD3和476A90兩state0 islands有signedversionDWORD63D54C>=52E0、phaseDWORD64AF98==2。46B510的21byte directhandler無上述gate；caller資格未知，不推全client無gate。
407DD5/407E39 state2需EAX==0加同version/phase條件；upstreamcommand/input未合格，不展開dispatch其它命令。
46B6F0的BYTE64AF9D==0或XOR-decoded selector==0到46B74A state1；這些runtimepredicate不能命名owner/mode。其非零替代路徑46B726→458BB0第一unknown STOP。
未知notice之後的獨立registration島46B733 pushcallback46B760，46B746 virtual+64第一unknown STOP；不推registration成功或callback實際執行。
46B760 callback條件ECX==0、input y/Y，selectorBYTE64B6DC==4另接受rawWORD9982/7882（LEbytes82 99/82 78），EDX來源/生命期/codec未知。state1send後46B7AA→458CA0 STOP。無owner/phase gate資格。
46ACB6→4EBB30是entry第一unknown；後46ACD3 send島獨立條件，不跨bridge。後46ACFE→4EBEC0、476ABF→4EBEB0亦STOP。ASM將476A90島掛在sub476A20下，契約用rawentry476A90，不宣稱476A20發D2。

UI finite caller與unknown：46CDD5、46CE23→allocation4F118C第一unknown；其後獨立caller準備島：46CDEB callback46B6F0/control2/sprite186B，46CE39 callback46B510/control0/sprite1869，x195/y168/last0。
重用Batch28 ctor4154E0 prefix35bytes，EBP+14 callback/EBP+18 control，4154FE→414910第一unknown STOP。沒有callbackstore/click/control→ECX/owner/mode/實際UI接受資格。

Opcode-only D2有Java AD368 parser OWNER_START body1/frame3（prefix0300），不是defaultstate0。既存savedtest-client index79真實opcode-only；舊0.7.664 physicalNativeownerclick第一request僅保留歷史版本等級。
這批有限固定PE11literal命中沒有opcode-only callsite資格；不能由Java支援或歷史envelope反推目前Native可發，亦不宣稱全映像不存在。

Localintent、serverconfirmation、D1observation、E1countdown、WORLDgap分離：
CS先guardROOM再parse，participant state0/1/2 exactremaining；owneropcodeonly須serverroom.owner==requester。GH普通ready明確roomexists/!observer/!battleStarted/battle-scene，未額外檢查members.contains或來源等；不能當任意session授權。
state2 hub/localreadyfalse但D1state2；WAITING repeatedready1仍true；COUNTDOWN alreadyready1會togglefalse並cancel。無separateownerstartlatch，最後participantready也可能啟COUNTDOWN。
非owner opcodeonly回false/NONE不移除既存hubreadyentry，但CSlocalfalse並D1state0，僅source/ILscope，不提出fix或良性no-op宣稱。
GH lock先commit ready/match/value/deadline，再CSlocalready、sequentialpeerD1，然後STARTED E103/CANCELLED E100；send可中斷後续recipient/E1。queuedtick/cancel一般race與partialsend沒有新動態資格。
S_D1 aliasNUL+state body3+N/frame5+N無room；只serverprojection，非localintent、全部ready、owner或WORLDgrant。Nativealias/table/privategates與數字phase2/3/6沿既存D1-E1契約，不重跑。
S_E1 count body2/frame4無generation/room；3/2/1與取消0都是receipt，timerexpiry不得本地生成WORLD。CSROOM/GHWAITING-COUNTDOWN/Nativephase獨立，Modern沿currentepoch/room/joingeneration及合格alias處理。

既存AD368 frame精確採用73..88共16rows，未重掃captures/cipher或PASS：73D202→74/75 D1state2；76D201→77/78state1；79ownerD2→80/82ownerstate1與81/83E103；84repeatedD201→85/87state0及86/88E100。順序按peer，不推跨stream freshness。
採已驗收beforefirsttick取消case及4second/15samplewatch；state0 dynamic、NativeGUI、一般race、world/renderer/bot/matching未知，沒有newcapture。

Version-evidence-table：AD368具該ready/cancelnetwork；C7具先前WAITINGmapnetwork且Batch28 settings先write後decode是其版本正確immutable敘述；485F599D...只採監製已接受parent12/candidate12seededunit修復。
485F只換GameHub.updateSettings Code及remapdebug/StackMap，decode/reconfigure移到settings/map/ready寫入前；其他1013methods/CP/ClientSession/updateMap不變依acceptedproof。485Fnetwork封存尚未驗收，不借用C7network，不推一般transaction/rollback。

Newwire/newopcode/wirecorrection/newcapture0；server-packet-delta.json 26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C未改。
本批Python -B及sys.dont_write_bytecode=True；無Native/GUI/input/injection/SQL/provider/JVM/Java執行/Modern/BIN改動，無未知helperbody擴張。採既存Batch26formatter/transport，到external759058A0 STOP，不授physicalsend/ACK。
Batch28 scope-write-correction.json SHA B51ECB6C...保持open：externalpyc仍保留，清理auto-review拒絕blocked by policy，未重試、換機制或委派。有限封存即停。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_Native_CD2_outbound_complete':True,'current_opcode_only_UI_authority_dynamic_open':True,**v,'independent_checks':292,'new_conditional_models':64,'callsite_counts':p['callsite_state_counts'],'actual_Native_dynamic_witnesses':0,'settings_485F_unit_only_network_not_adopted':True}
    complete=load('completeness-audit.json');complete['CD2_ready_outbound_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第二十九批C_D2 ready/start：')[0]
    entry.write_text(txt+'\n第二十九批C_D2 ready/start：CD2-ready-outbound-contract.json + CD2-ready-outbound/READ-ME.txt + intent-receipt-and-unknown-matrix.json。固定PE7callsite：state0三、state1兩、state2兩，全cc/D2+BYTE body2/frame4；有限literalinventory無opcode-onlycurrentPE資格，Javaparser/舊版Nativeclick分版本引用。16code/1data/1reuse/66assert/292checks/64conditionalmodels/30priorpins。既存AD36873..88 state2/ownerD2/repeatedready1/D1→E100採16rows，no recapture；intent/GHcommit/D1/E1/WORLDgap分開。485F僅acceptedunit順序修復，network未驗收；packet不變，externalcache清理拒絕保持open。\n',encoding='utf-8')
    phase='BATCH29_CD2_READY_OUTBOUND_STATIC_SEALED_CURRENT_OPCODE_ONLY_UI_AUTHORITY_DYNAMIC_CACHE_CLEANUP_OPEN'
    h=load('handoff.json');incident=h['scope_write_correction'];assert incident==v['scope_write_correction']
    h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),CD2_ready_outbound_contract=pin,finite_Native_CD2_outbound_complete=True)
    added=[pin]+[record(q)for q in sorted((ROOT/'CD2-ready-outbound').iterdir())if q.is_file()]
    for group in ['modern','launcher']:
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if q['path']==str(ROOT/'completeness-audit.json')else q for q in h['shared']]
    h['server']['known_CD2_ready_outbound_boundary']=pin
    h['next_steps']=[
      'Batch29 finite7Native cc calls sealed:state0 at46ACF4/46B51C/476AB4,state1 at46B756/46B7A2,state2 at407DFA/407E5E. D2+BYTEstate body2/frame4. Currentopcode-onlycallsite unknown in finite11literalinventory; Javaownerparser andolderNativeclick cannot upgrade currentPE.',
      'UI firstunknownallocation46CDD5/46CE23→4F118C;conditionalcallback46B6F0/control2 and46B510/control0 callerargs;reusedctor4154FE→414910 STOP. No callbackstorage/click/control→ECX/owner/mode authority. Entry46ACB6→4EBB30 andstate1notice458BB0/registrationvirtual64/tailsSTOP; no unknownbody expansion.',
      'Nativeversion signed>=52E0 andphase==2 on two state0 andtwo state2islands;state2 alsoEAX==0. State1BYTE64AF9D/XORselector/input y/Y orselector4 WORD9982/7882 unnamedpredicates,notowner/codec qualification. Direct46B510 localgateabsence finiteonly.',
      'Adopt acceptedAD36816savedframes73..88:state2→D1state2;participantready1 thenowneropcodeD2→D1/E103;repeatedready1→perpeerD1state0/E100. No capture/PASS replay,NativeGUI/state0dynamic/generalraces unknown. Localintent/GHcommit/D1receipt/E1countdown/WORLDgrant separate; perpeer order andepoch/room/join/alias policy retained.',
      '485Fsettings candidate acceptedunitonly:decode/reconfigure beforewrites,other1013methods/CP/ClientSession/updateMap preserved perproof;485Fnetworknotaccepted. C7mapnetwork andBatch28oldsettingsordering remain correctversionscope; no oldprimary modifications or generalatomicity upgrade.',
      '30priorpins,16newcode/1data/1reuse/66assert/292checks/64newconditionalmodels/7adoptedJavaILmethods/16savedframes. Newwire/opcodes/corrections/capture0,packetunchanged. No Native/GUI/input/SQL/provider/JVM/Java/Modern/BIN actions orunknownbody expansion;finitebatchstop.',
      'Batch28 externalpyc scopeincident remains open:automaticcleanupreview rejected blocked bypolicy;cache retained. Exactownedscope-write-correction pin unchanged. No retry/reframe/alternative/delegation;Python -B anddont_write_bytecode used.',
    ]
    assert h['scope_write_correction']==incident and h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch29 finite Native D2+state0/1/2 callsites sealed; currentopcode-only/UI/authority/dynamic and priorcachecleanup remainopen;485Funit-only version adopted.',CD2_ready_outbound_summary=summary)
    progress['handoffs']['modern']='CD2-ready-outbound-contract.json:localintent vsGHcommit/D1receipt/E1countdown/WORLDgap;AD368exactcancelwitnesses;currentNativeopcode-only/UIunknown;485Funitonly networknotaccepted.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['CD2_ready_outbound_audit.py','review_CD2_ready_outbound.py','publish_CD2_ready_outbound.py','CD2-ready-outbound-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT))for q in (ROOT/'CD2-ready-outbound').iterdir()if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),'priorpins':30,'packet_unchanged':packet,'scope_write_correction':incident},ensure_ascii=False))
if __name__=='__main__':main()
