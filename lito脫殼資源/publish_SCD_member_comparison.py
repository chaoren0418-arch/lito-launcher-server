"""Publish batch15 finite S_CD boundaries without touching Java/Modern."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('SCD-member-comparison-contract.json');pin=record(ROOT/'SCD-member-comparison-contract.json');v=report['verification']
    review=load('SCD-member-comparison/independent-review.json');assert review['all_match'] and review['primary']==pin
    assert (v['instruction_assertions'],v['new_code_ranges'],v['new_data_ranges'],v['inherited_raw_ranges'],v['sealed_CD_frames'])==(81,14,12,12,8)
    assert len(report['prior_primary_pins'])==16
    for p in report['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['existing_evidence'].values():assert record(Path(p['path']))==p
    for p in report['reusable_outputs']:assert record(Path(p['path']))==p
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    readme=ROOT/'SCD-member-comparison/READ-ME.txt'
    readme.write_text('''第十五批：S_CD 成員列／動態姓名比較表／增量ready直接邊界

主入口：../SCD-member-comparison-contract.json
本批有限契約已封存，原版 runtime table／UI／physical/source/player/owner 資格仍未閉合。Modern比較guard保留；Java producer由伺服器組獨立處理，本組沒接管。
fixedPE 4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9。
BIN CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292。
ASM 2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A，只導航，不凌駕raw。

沿用與新增
先查Modern R496既有S_CD decoder/gap及native participant/state/primary-tail/status-grade報告，不重跑舊PASS。
12份既有raw引用並逐byte對fixedPE：R496 S_CD版本prefix、selectedname globalcopy、4D39F0 initializer、4D3A80 comparator、S_3A initializer caller、S_D1 prefix；本組startup、packetreader、兩dispatch selectors、RET8、allocation thunk。
14新code／12新data＝26range：補handler recordbuild與phase/membertail、dispatchcall、必要vector/copy/readyUI helpers、freewrapper/thunk、comparator末RET8的3byte、5版format literals、CDmap/jump及兩256byte disk table快照。Disk table兩個全零，不是runtime值。
R496 comparator raw只有91bytes，末RET8不完整。本批沿用其中90個完整指令bytes，再補4D3ADA..4D3ADC alignedRET8，組合完整93byte comparator。
Handler4A7F60..4A8B9A完整3131bytes由既有prefix/globalcopy與兩新增gap組合。81固定expected指令斷言；independent-review另核對所有raw、指令、handler/comparator全byte覆蓋、8sealedCDframe固定offset與16priorpins。

dispatch／known wire
S_CD兩個dispatch都處理：primary4B353D=75hex(slot117)→4B33DC→4B2F1F；fallback4B3781=2Fhex(slot47)→4B362C→4B2F1F；4B2F21 ECX=ESI packetbody→4A7F60。
既有signed63D54C版本格式保持：低版dscccdddcscsdc；>=52E0增加c；>=75A4再增加c；>=7726末尾改d；>=9D7C dscccdddcscsdccdc並後接12c；>=9F0A再接5c+d。
第一個s capacity28bytes，其他s17bytes，含NUL；reader外部copy/seek與native inputlength validation仍未閉合。Modern bounded/trailing拒絕屬strict adaptation。140bytes是內部record stride，不是networkbody或packettail大小。

sealed5ED/49A frame核對
5ED frames.json SHA A9BCA0ECC611F76EE1BD2D6BE6F3B512DC5ABD169C60DACEB6FC74C6D7E0D69F：CD frame70/71/72/73。
49A frames.json SHA 283A3DBFA77668FDAA0D55CF67239218CF34D546B53968DFFFBD973A8D093004：CD frame71/72/73/74。
各有Host-GAME與Guest-GAME收兩個member；body84／85bytes，wire86／87，17primaryfields＋17tailc＋taild正好consume，無remaining。Sameepoch封存S_3A version>=9F0A支援選定format；不是新capture或從native執行取得version。
Firstd全是room_id1；同房不同member不可用它當個別physical actor／source object ID。舊R496 actoridentifier label在本組roomphase證據中只能保留為歷史命名，不能作identity授權。
名字為BattleHost／Guest(BattleMoon)、ReturnHost／Guest(ReturnMoon)。field9數值state=0；field10empty，field11reserved0，field12是ASCII Ready，不能把文字Ready當numeric ready1或owner/player binding。

比較表從哪裡來
4D3A80是8bit NUL comparison，使用lead834A60[256]與fold834960[256]。若左byte lead非零，比raw lead及下一raw byte；否則比fold[left]-fold[right]。單byte分支相同且左rawbyte0才return0；沒有local length/max/readyflag/locale資格檢查。
4D39F0呼叫private05010B41(ECX numericparameter, pointer20byte stackarea)。ASM原label05A10EEA與fixedraw不同，不能拿ASM label猜actualtarget。
返回後clear256leadbytes；stackDWORD<=1不設lead，>1從stackarea+6走inclusivebytepair，直到00/00；pairwalk没有額外paircount上限。原private是否成功填值／終止符／範圍未知，返回狀態也沒檢查。CPINFO-like只能是既有結構inference，非API identity驗收。
再讀*[4F727C]，loop0..255 call function(argbyte)→AL寫foldtable。Disk pointer773B2A40；原dump-time DLL/module/export/body SHA與全部actualfoldoutputs缺失。沒有用今日DLL或猜ASCII/casefold替代。

不能只說startup一次有效
Startup480A88以ECX0→4D39F0，沿用lifecycle raw。
既有S_3A 4A33E0 raw另有reinit：selector64B6DC=3→ECX950/call4A352B；4→932/4A353E；5→936/4A355C；0→949/4A356E；其他→1252/4A356E。本批只索引這段必要tableproducer，不展開S_3A auth/UI或新增opcode。
Native全球table可在startup後／S_3A更新，不能無條件跨connection/sourceversion/locale generation沿用資格。Minimum可用條件是private成功初始化、256foldcalls成功/來源已證、immutable兩表snapshot綁初始化事件與currentownedepoch/locale、strings bounded且multibyte lead有合法trail；這些仍缺actualruntime evidence。
FixedPE兩表全零不能授權姓名相等；有限counterexample Alice與BOBBY在zero nonlead/fold表被model判equal，僅證明snapshot不能使用，不是native執行。

identity與增量新增
Incoming第一個d存localrecord+0，name存+4；全部record stride8C/140，copy23DWORD。其他padding／字串NUL後tail可能是未初始化stackbytes，不能把140byte memory當成ownednetworkrecord。
4A83F6 incomingname vsselected64ABF8 zero時，4A840F先copy140bytes→64ABF4；這在phase與roomid gate前發生，也包含覆寫64ABF4第一d。不要把native後置roomguard當作Modern atomic ownedroom授權，Modern須在發布前檢查自己currentroom與epoch/revision。
phase1是selectedroom preview branch：450B90找64A958 selectedrow，firstd對selectedrow+4，有original FFFFFFFF/emptyname sentinel；previewvector64ACF4/F8以entry+4 dynamiccompare dedup。UI與vectorruntime尚未知。
roomphase只准2/3/6，firstd對64ABF4。field3(kind)0→combatant64AB60 vector(begin64AB64/end64AB68/cap64AB6C)，kind1→other/spectator64AB50(begin64AB54/end64AB58/cap64AB5C)，kind2notification，其他return。
實際dedup key是currentroom context＋kindvector＋comparator(incomingname,record+4)==0；没有memberGUID／requestnonce／ownedepoch或opaque revision欄位。同room d相同不能區分member，名字更不能推physical/source/owner identity。
兩vector scan的comparezero分別4A864A／4A892A→4A8B94 return：重複S_CD不append、不覆寫既有record ready或UIinitialstate。因selectedname全域copy已先發生，不能擴大成「所有global都不變」。
新member inline capacity/growth branch append140byte；4B4C90 complete可copy140bytes，4B4660 completecopyrange，4B46A0 completerepeatcopy，4B38F0 computesrecords。4F118C→external773C4C20 allocation、4F103C→4F1186 freeexternal thunk的執行與allocator成功未知；4651B0 UIappend、4B4020 previewinsert止於depthboundary。

重複ready更新其實是哪條路徑
S_CD field9只作初始record+44 state，field15為previewUIstate；0普通／1ready／2SHOP_LIST，2不能視為boolean ready。
後续既有S_D1 sc在4A9550；本批只補必要membertail4A966D..4A9760與UIstatehelpers，沒有展開earlier self/world/phasecallback branches或倒數S_D0。
Membertail先combatant再otherkind，以4D3A80 name找第一match，寫record+44 byte，呼叫465530(name,MOVSXstate)。465530完整body掃UI list所有matching item+10 text，463BD0把DWORDstate寫item+6C。
這能交接incremental更新鍵與直接寫入；但S_D1 earlier self-name分支可能先做phase/world effects，fullS_D1 admission仍需另外資格。最小tail不等於world/ready transition已通過。

Modern最小consumer缺口與後續
用R513 frozen native_source_actor.rs與native_city_runtime.rs snapshot做比較，指紋逐一符合r513-production-source-snapshot.json；不拿變動中的src檔當持續core依賴。Index內oldsrc path是historical metadata。
FrozenR513已有strict205版本decoder，但仍拒絕unqualifiedcomparison，沒有安裝actor。結構AD368 roomadapter可以持有opaque display/room roster資料，exact native dedup／selectedglobal／readyUI副作用仍須actualimmutablelead/fold tables、initializedvectors/UI、currentownedroom与revision。
只交原版可見資料流，不新增table injection API，不用plain equality、預設codepage、diskzero表或label判physical/source/player/owner ready。
Private05010B41／external773B2A40、allocator/free、4F1592 conversion、4651B0/4B4020/notification深層未知詳列direct-helper-boundaries.json。
S_D0/countdown/battle/results不decode、不展開。Java producer組工作不接管。原native/capture/SQL/observer/GUI/input/injection/oldPASS及他組writes均0。
newwire0／newopcode0／wirecorrection0，packet_delta B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C維持；16priorpins含batch14 2256D57D...保持。有限契約與最小consumer缺口已封存，本批即停。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_SCD_member_comparison_boundary_complete':True,'comparison_guard_retained':True,
      'actual_runtime_tables_qualified':False,'complete_handler_bytes':3131,'complete_comparator_bytes':93,
      'instruction_assertions':81,'new_code_ranges':14,'new_data_ranges':12,'inherited_raw_ranges':12,'sealed_CD_frames':8,
      'startup_and_S3A_reinit_indexed':True,'CD_duplicate_does_not_update_ready':True,'later_ready_S_D1_tail_only':True,
      'first_dword_room_id_not_physical_id':True,'Modern_frozen_R513_only':True,'prior_primary_pins_preserved':16,
      'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'packet_modified':False,'Java_producer_taken_over':False,
      'physical_source_owner_player_GUI_accepted':False,'S_D0_results_expanded':False}
    complete=load('completeness-audit.json');complete['SCD_member_comparison_batch']=summary;write('completeness-audit.json',complete)
    reader=ROOT/'閱讀入口.md';text=reader.read_text(encoding='utf-8').split('\n第十五批S_CD member comparison：')[0]
    reader.write_text(text+'\n第十五批S_CD member comparison：SCD-member-comparison-contract.json + SCD-member-comparison/READ-ME.txt。4A7F60完整3131bytes／comparator93bytes、81指令／14code+12data／12沿用raw／8sealedCD。Table startup與S_3A reinit已索引，private/external actualtable缺失；CD同kind重複姓名return不更新ready，S_D1只補member/UI直接tail，guard仍留。Firstd roomid不作physicalactorID；R513 frozen比較、16priorpins／newwire0／packet不改；不接管Java，不展開S_D0/results。\n',encoding='utf-8')
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase='BATCH15_SCD_MEMBER_COMPARISON_BOUNDARY_SEALED_GUARD_RETAINED',entrypoint=record(reader),SCD_member_comparison_contract=pin,finite_SCD_member_comparison_boundary_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'SCD-member-comparison').iterdir())if p.is_file()]
    for group in ['modern','launcher']:
        mapped={p['path']:p for p in handoff[group]};mapped.update({p['path']:p for p in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if x['path']==str(ROOT/'completeness-audit.json')else x for x in handoff['shared']]
    handoff['server']['SCD_known_native_boundary_corroboration']=pin
    handoff['next_steps']=[
      'Modern consume S_CD dynamiccomparison provenance andcurrentroom/kind/name key; keepguard absent actualimmutable256lead+256fold tables boundtoinitialization event/ownedepoch/localegeneration.',
      'Initializer isstartup480A88 ECX0 plusknownS_3A numericselectors;private05010B41/external773B2A40 remainunqualified. FixedPE zero tables cannotgrant equality.',
      'S_CD duplicate name inselectedkindvector returnswithoutreadyoverwrite; onlyearlierselectedname globalcopy mayoccur. LaterS_D1 readyrecord+44/UI+6C directmembertail isknown butself/worldeffects arefuturequalifications.',
      'All8sealed5ED/49A CD members firstd=room1,notindividualphysical/sourceID; Ready isfield12string whileinitialfield9state0. R513 frozenfiles only forcontinuingModern comparisons.',
      'Server handles Java S_CD producerindependently; no newwire/packetwrite. Native/SQL/capture/oldPASS/playerclaim0. S_D0/countdown/results stayfuture; finitebatchsealed stop.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=handoff['phase'],current_task='Batch15 SCD member/table qualification boundarysealed; actualruntime lead/fold/privateproviders remainmissing, CDdedup separatefromlaterD1ready. FrozenR513 comparison; no physical/source/owner ready.',SCD_member_comparison_summary=summary)
    progress['handoffs']['modern']='SCD-member-comparison-contract.json: bothdispatch, nativekind/name dedup, startup/S3Areinit tableprovenance, minimalS_D1 memberready helpers; guard retained/frozenR513'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    for p in report['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['existing_evidence'].values():assert record(Path(p['path']))==p
    names=['SCD_member_comparison_audit.py','review_SCD_member_comparison.py','publish_SCD_member_comparison.py','SCD-member-comparison-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in(ROOT/'SCD-member-comparison').iterdir()if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'prior_pins_preserved':16,'delivery':record(ROOT/'delivery-verification.json'),'packet_unchanged':packet,'comparison_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
