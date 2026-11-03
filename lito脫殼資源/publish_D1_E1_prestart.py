"""Publish Batch17 finite prestart static contract in owned directory."""
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('D1-E1-prestart-contract.json');pin=record(ROOT/'D1-E1-prestart-contract.json');v=report['verification'];review=load('D1-E1-prestart/independent-review.json')
    assert review['all_match']and review['primary']==pin
    assert (v['instruction_assertions'],v['new_code_ranges'],v['new_data_ranges'],v['inherited_raw_ranges'],v['sealed_D1_frames'],v['sealed_E1_frames'])==(81,11,9,23,8,12)
    assert len(report['prior_primary_pins'])==18
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    for r in report['existing_evidence'].values():assert record(Path(r['path']))==r
    for r in report['reusable_outputs']:assert record(Path(r['path']))==r
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'D1-E1-prestart/READ-ME.txt'
    reader.write_text('''第十七批：完整 S_D1 自身狀態分支／S_E1 倒數接收 prestart 有限契約

主入口 ../D1-E1-prestart-contract.json。有限靜態證據封存，原版table／self identity／UI／opaque準備與callback資格仍未閉合，guard保留。停在WORLD/preparation入口call，不讀其body或後續WORLD封包，不推導倒數結束或世界開始。
只寫本組目錄。無native、GUI input、SQL、provider、新capture、舊矩陣重跑；不接手Java或改述受攔截工作，不展開battle/world/results或完整反編譯。

來源
server-room-ready-prestart-consumer-contract.json SHA57F324FE093C80A8DAE8B7E207A9A8A628AA508E04EBB38AFE17F3C33D6274F8；只引用其native-ready、consumer-handoff與existing-ready-countdown-wire sealed附件，Java producer/commit/publication由伺服器組負責。
fixedPE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9；BIN CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292；ASM 2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A僅導航，raw優先。
Batch15 comparison 1C2519F9、Batch16 SD0 50A26C65主契約及先前18份固定pin保持。Lead/fold table startup/S_3A reinit與actualprovider未知沿用，不重抽零表，不猜ASCII/codepage。

新增／沿用／核對
11新code＋9新data＝20ranges：D1 gap197bytes、E1完整272、scene3 branch210、兩dispatcher、fallback return、sound thunk、format notification wrapper、3presentationcallbacks、sc literal與兩opcode map/jump資料。
23份既有raw逐byte對fixedPE：D1 R496prefix88、Batch15 membertail244及UIhelpers、reader/dispatchselectors、scene prefix/mode2/UIhelpers、scheduler完整254與clock prefix31、allocator/free/thunk及c literal。
D1完整529bytes =88舊prefix＋197新gap＋244舊tail，4A9550..4A9760。E1完整272bytes，4AABB0..4AACBF。
81指令expected斷言；independent-review另查完整bytecoverage、32個aligned internalbranch、dispatcher/table字節、全部封存frame、Native E1/3callbacks沒有直接phase write或packetwriter與opaque入口未被展開。
這是新有限靜態核對，不是Native handler／UI／scheduler執行驗收。

Dispatcher不同
Selectors沿用：mode64BBD4==4且signed64AF98>=3→primary，否則fallback。
D1=209：primary4B3541=77hex slot119→4B33E4；fallback4B3785=33hex slot51→4B363C；兩者→4B2F37，4B2F39 call4A9550。
E1=225：primary4B3551=7Fhex slot127→4B3404→4B2F97，4B2F99 call4AABB0；fallback4B3795=50hex slot80→4B36B0→4B3200 epilogue return，沒有處理。
不能因D1兩dispatcher都handle就宣稱E1相同。JavaROOM也不能直接代換mode4/nativephase3。

Known wire：沒有新增或更正
S_D1 schema sc，body d1＋NUL terminated8-bit alias＋state byte；body3+A/frame5+A。Aliascapacity28包含NUL，state只1byte，singlebyte無額外端序。沒有roomId/epoch/request/owner/source/physicalID。
Native這段没有固定codec declaration；capturednames是ASCII，Java MS950是另一producer證據，Native相等仍需initialized lead/fold authority。
S_E1 schema c，body e1＋unsigneddisplay byte；body2/frame4。沒有room/epoch/generation/deadline/startgrant。Native沒有0..3或0..5 rangecheck。
External reader stringcopy/seek／nativeexactbodyend/inputbounds未知，Modern checkedexactend是adapter policy，不是Native證明。
Knownstate0NOT_READY、1READY、2SHOP_LIST；state2不是booleanready。Native self branch只測==1，其他值皆非ready；native本地沒0..2domain validation，strictadapter可按Java契約拒绝其他值。

D1完整self branch
只准numeric64AF98=2/3/6；無roomDWORD可比，依currentglobalcontext與incomingalias vs64ABF8 via4D3A80。64ABF8是presentationglobal，不能授權physical/source/owner。
Nonself跳common tail；尾部再次讀phase，==6直接return，否則combatant先找第一aliasmatch，寫record+44 statebyte、UI465530(name,MOVSX i8state)，找到即return；沒找到才otherkind。UI465530/463BD0沿用Batch15，沒有新UI執行。
Self且state1：先把「當時phase==6」SETE結果寫648F1C；若648EB8!=0且834C18!=0，call47E360，否則call47CF00。
47E360／47CF00僅封存呼叫端與選擇條件，作opaque WORLD/preparation入口depth-stop；不讀body/descendants、未執行、不宣稱世界啟動或Native ready authority。
Opaquecall返回後重讀64AF98；仍==6則return，否則ECX3→469CA0。Call可能已改phase，因此不假設它保持原狀。
Self非1（含0/2）：原phase6直接return，不呼叫上述opaque準備入口；其他ECX2→469CA0。
64AFC7!=0時，先call469CA0(2或3)，返回才將當時62C9B8/BC/C0保存64A9B4/B8/BC，再設62C9C0=469BD0、BC=47B700、B8=47B6F0。Saved callbacks是AFTER helper，不是收到D1前的值。
64AFC7==0只callscenehelper不作這段override。完成後共同phase6 gate再決定是否進membertail。
D1 boundedhandler沒有4C6AE0，但opaque準備/scene/callback descendants效果未知，不能從receipt臆造C_D2意圖或宣稱所有深層都不送包。

Scene mode2／3 direct邊界
469CA0若62C21C==0，只把signedWORD argument存64AF98後return。非零用既有jump表。
Mode2沿用Batch14 469EC3完整264bytebranch，phase2與UI/defaultcallbacks；mode3新補469FCB..46A09C共210bytes，寫phase3，UIvisibility/selectionflaghelpers、virtual+20/+24、child0..7flagclear與selectedchild1、defaultcallback46D640/46D8A0/46D630。
直接flaghelper的raw已沿用核對，UI object/vtable/children initialized/lifetime以及callback啟用未知。Callback可改global，沒有atomic finalphase或GUI acceptance。

E1 Native phase3／6
Reader讀c至local低byte；phase3或6時count==1呼叫4C8790(ECX2776,EDX7F)，其他含0呼叫2775。4C8790是JMP4C85E0，soundbackend/resource stop，不展開。
Resourcecount834B90>4C時取*[834B94]+130 formatpointer，否則63D618；count&FF傳458C60與5034A0 color。
458C60完整61bytes，external773F6680格式化到512byte stackbuffer，return>0才458BB0 notification；actualAPI/body/resource/UI與bounds未知，不把call當渲染成功。
E1handler没有直接phase write，也沒有直接packetwriter。Phase3/6維持時不enqueue phase4timer。
Sound/message callbacks後重新讀phase64AF98；若已被回呼改為4，可能接著走phase4queue，不能假設state frozen。

Native phase4：presentation queue，另列
這不是JavaROOM/COUNTDOWN資格。n=u8，沒有範圍check。
i=0..n-1：4D65F0(delay=i*1000,callback471E30,param=n-i)。
n==5：再queue delay5000 callback471E60,param0；n!=5：queue delayn*1000 callback471E80,param0。
最後queue delay(n+1)*1000 callback471E30,param-1。
n0不跑第一loop，但仍queue471E80 delay0與471E30(-1)delay1000。沒有queueclear/cancel、phasewrite或WORLD/start instruction。
471E30完整35bytes：CL→62C478，signedCL>0且64B090==0時tail-jump聲音2777。471E60完整32bytes：62C478=FE，flag0時sound2778；471E80完整32：62C478=FD，flag0時sound2779。
三callback都沒有直接world/start/phase/packetwriter；只封存提示資料與soundcall，不能把FE/FD或最後param-1賦予倒數結束/世界開始語義。
4D65F0完整254byte既有raw：call4D69F0取得時間基準，adddelay，插入12byte {due,callback,param}到834C08，count834C00/cap834C04；容量不足allocator/free及external77409100 copy。此helper沒有callbackinvoke/queueclear。
4D69F0沿用31byteprefix，只證private05010863呼叫等入口；actualclock/queueinit/allocator/timeunits/timerpump/removal/cancellation/epoch依賴未知。本批不追其他schedulercaller或執行clock。
handler arithmetic使用1000等常數，delay不是arrival/deadline精度或Java250ms poll對Native的證明。重复E1可能增加presentationqueue，沒有qualified去重/cancel語義。

E1/00與三種狀態域
Java已接受契約：ClientSession保持ROOM，GameHub有WAITING/COUNTDOWN，source/IL可發E1/00取消；這與Nativephase2/3/6不同。
Native E1/00在phase3/6走一般2775/message0，沒有直接phase2reset；phase4走n0presentationqueue。No localWORLD/start grant。
Adapter按明確AD368政策可更新/清空countdisplay，但必須標為adapter消費規則，不能聲稱這是原版phase回復或start。
D1 readyprojection不是localC_D2 command intent、ownerauthority、all-ready、scene/world/physical/source/playerready。Nativeexactself需table/global/context資格。

封存 frame 與 R515缺口
5ED frames SHA A9BCA0ECC611F76EE1BD2D6BE6F3B512DC5ABD169C60DACEB6FC74C6D7E0D69F；49A SHA283A3DBFA77668FDAA0D55CF67239218CF34D546B53968DFFFBD973A8D093004。
全部是1based；另列0based原arrayindex：
5ED Host D1 77/78→E1 79/80/81=3/2/1；Guest D1 75/94→E1 95/96/97。
49A Host D1 78/79→E1 80/81/82；Guest D1 76/85→E1 86/87/88。
8D1皆state1，alias Host/Guest BattleMoon或ReturnMoon，body25/26、frame27/28；12E1 body2/frame4。全部consumeexactbody，remaining0。
Delegated R515 Guest frame95 E1/03與5ED一基95相符；沒有讀取或重播R515實際trace，不宣稱其Nativephase/GUI資格。
逐peer封存receipt順序D1先於3/2/1，不拿globalCaptureLog先後當所有peer atomiccommit證明。
沒有E1/00/state0/state2/repeated/partial失敗的新動態證據；那些Java路徑仍只用source/actualIL。沒有採用/解碼後續WORLD marker或body。

Consumer與精確未知
Modern explicitAD368 adapter可依既知sc/c格式消費D1/E1，綁ownedtransportepoch/currentCEroom/localjoingeneration，D1投影與本地意圖分開，state2不作boolready。
Native comparison/private/globalphase與UIguard仍留，不能從server顯示count/alias推Nativeauthority，不能用diskzero/ASCII/casefold或newtableinjection替代。
E1沒有room/epoch/nonce/timestamp，repeated/races/freshness與queuedtickcancel不可僅靠wire解決；不同epoch/room不沿用intent/display/queue。
不在本地timer到0時合成WORLD/start/送包；後續World、source/physicaladmission另需資格。本批只交prestart可消費契約與opaque入口unknown。
Javaproducer/postcommit/publishrace/partialsend與原受攔截工作不接管；unknown列表詳direct-helper-boundaries.json。

Seal
newwire0/newopcode0/wirecorrection0；server-packet-delta B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C保持；18priorpins保持。
有限prestart契約完成，actualNative table/UI/world/physical/source/player/ownerready皆未qualified，overallfalse。本批封存即停。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_D1_E1_prestart_complete':True,'comparison_guard_retained':True,'WORLD_preparation_entry_stopped':True,
      'instruction_assertions':81,'aligned_internal_branches':32,'new_code_ranges':11,'new_data_ranges':9,'inherited_raw_ranges':23,
      'complete_D1_handler_bytes':529,'complete_E1_handler_bytes':272,'sealed_D1_frames':8,'sealed_E1_frames':12,'prior_primary_pins_preserved':18,
      'Java_ROOM_COUNTDOWN_Native_phases_distinct':True,'state2_not_boolean_ready':True,'E1_zero_not_world_start_or_native_phase2reset':True,
      'E1_direct_phase_write_or_packetwriter':False,'actual_native_tables_UI_world_qualified':False,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
      'packet_modified':False,'Java_taken_over':False,'physical_source_player_owner_GUI_accepted':False}
    complete=load('completeness-audit.json');complete['D1_E1_prestart_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text(encoding='utf-8').split('\n第十七批D1/E1 prestart：')[0]
    entry.write_text(txt+'\n第十七批D1/E1 prestart：D1-E1-prestart-contract.json + D1-E1-prestart/READ-ME.txt。D1完整529/E1完整272bytes，81指令/32branch/11code+9data/23沿用raw/8D1+12E1sealed。D1self1選opaque準備入口後scene3，非1scene2除phase6；入口depth-stop不讀WORLD。E1primaryonly，phase3/6提示無directphasewrite/packet，phase4presentationqueue另列；00非worldstart/Nativephase2reset。Batch15guard/18priorpins/newwire0/packet不改，不接Java或受攔截工作。\n',encoding='utf-8')
    phase='BATCH17_D1_E1_PRESTART_BOUNDARY_SEALED_WORLD_ENTRY_STOPPED_GUARD_RETAINED'
    h=load('handoff.json');h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),D1_E1_prestart_contract=pin,finite_D1_E1_prestart_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'D1-E1-prestart').iterdir())if p.is_file()]
    for group in ['modern','launcher']:
        mapped={p['path']:p for p in h[group]};mapped.update({p['path']:p for p in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if p['path']==str(ROOT/'completeness-audit.json')else p for p in h['shared']]
    h['server']['D1_E1_known_native_prestart_corroboration']=pin
    h['next_steps']=[
      'Modern explicitAD368 adapter consumeGuest5ED1based95 E103 andorderedD1/E1 sc/c underownedepoch/currentCEroom; noNativephase/table/UIauthority fromJavaCOUNTDOWN.',
      'D1 self testsstate==1;2shop/non1notbooleanready. Nominalscene3for1/2fornon1 except6; opaque47E360/47CF00 callconditions only,stopbeforeWORLDdescendants. PreserveBatch15comparison guard.',
      'E1primaryonly/fallbackignore; phase3/6 sound/countnotifications withnodirectphasewrite. Re-readphaseaftercallbacks. Phase4timersarepresentationevents,notserverstart; E1/00 neitherWORLD norNativephase2reset.',
      'No localcountdownexpiryWORLD/start/send synthesis. Bindtransportepoch/currentroom/generation; wirelacksroom/deadline/nonce. Repeat/cancel/partialwire/timerpump/privateclock/UI/sound stillunknown.',
      '18priorpins/packet unchanged,newwire0; Javaownsproducer/postcommit/publishraces. Native/capture/SQL/provider/oldmatrix/playerclaim0; prestartfinitebatchstop.']
    assert h['server']['packet_delta']==packet;write('handoff.json',h)
    p=load('監製進度.json');p.update(updated_at=stamp(),phase=phase,current_task='Batch17 D1self/E1countdown prestartfiniteboundarysealed; opaqueWORLD/preparationentriesstopped,table/UI/callback/timerauthorityunknown,guardretained.',D1_E1_prestart_summary=summary)
    p['handoffs']['modern']='D1-E1-prestart-contract.json: D1 fullselfstate, E1primaryonly/phase3or6notifications/phase4presentationcallbacks, 20sealedframes, state2notboolready/E100notstart; WORLDentrydepthstop andtableguard'
    p['next_steps']=h['next_steps'];write('監製進度.json',p)
    for r in report['prior_primary_pins']:assert record(Path(r['path']))['sha256']==r['expected_sha256']
    names=['D1_E1_prestart_audit.py','review_D1_E1_prestart.py','publish_D1_E1_prestart.py','D1-E1-prestart-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in (ROOT/'D1-E1-prestart').iterdir()if p.is_file()]
    count=seal(names,Path(__file__));print(json.dumps({'primary':pin,'changed_files_checked':count,'prior_pins_preserved':18,'delivery':record(ROOT/'delivery-verification.json'),'packet_unchanged':packet,'comparison_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
