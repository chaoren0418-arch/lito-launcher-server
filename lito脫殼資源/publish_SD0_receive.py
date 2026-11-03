"""Seal Batch16 evidence and consumer handoffs in the resource directory only."""
from audit import *
from seal_delta import seal

def load(n):return json.loads((ROOT/n).read_text(encoding='utf-8'))

def main():
    report=load('SD0-receive-contract.json');pin=record(ROOT/'SD0-receive-contract.json');v=report['verification'];review=load('SD0-receive/independent-review.json')
    assert review['all_match']and review['primary']==pin
    assert (v['instruction_assertions'],v['new_code_ranges'],v['new_data_ranges'],v['inherited_raw_ranges'],v['sealed_D0_frames'])==(75,5,7,20,3)
    assert len(report['prior_primary_pins'])==17
    for p in report['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    for p in report['existing_evidence'].values():assert record(Path(p['path']))==p
    for p in report['reusable_outputs']:assert record(Path(p['path']))==p
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'SD0-receive/READ-ME.txt'
    reader.write_text('''第十六批：原版 S_D0 成員移除／自身離房接收有限契約

主入口 ../SD0-receive-contract.json。有限靜態證據已封存；comparison table／UI／場景回呼／普通self-exit動態與server postcommit仍未閉合，guard保留。
只寫本組資源目錄。Java producer/postcommit由伺服器組負責，本批沒有接管。無native啟動、GUI輸入、SQL、provider、capture、舊矩陣重跑；不展開整個battle/results/ready，不改述受攔截工作。

來源與核對
先讀server-room-D0-consumer-routing-supplement.json SHA7BF90D85A297E8AAA9D90D3BD4AF11DFD13992D685FF7211CBD1A9433CA898E6，route-boundary.json SHA CB5AA1542DF31E242DD45BDAA3134AF58BE52073825DD8177527CB76BA9C8A72。
固定PE SHA4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9；BIN CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292；ASM只導航，不凌駕fixed raw。
5新code＋7新data＝12 ranges：完整D0 handler1216bytes、shareddispatch12、UIremove191、selectedpreviewhelper132、mode1scenebranch506；兩map/jump、ds/cd literals與格式化functionpointer。
20份既有raw逐byte對fixedPE並boundeddecode：reader與selector、scene prefix/jump表、直接UIflaghelpers、listreset、packetwriter等。Batch15 comparison完整93bytes與初始化資格直接沿用1C2519F9封存主契約，不重抽、不用disk zero表。
75固定expected指令斷言。Independent readback另核對完整coverage、45個內部分支aligned落點、literal/未使用extraargument、3sealedframes與17immutablepriorpins。沒有執行原版handler或新增runtime測試。

Dispatcher
Primary mode64BBD4==4且signedphase64AF98>=3，otherwise fallback，selectors沿用已有raw。
S_D0=208/D0：primary4B3540=76hex slot118，jump4B33E0；fallback4B3784=32hex slot50，jump4B3638。兩者→4B2F2B；4B2F2D ECX=ESI packetbody call4A9090。

Known wire：仍是 ds，不是新packet或更正
Body d0＋DWORD little-endian room context＋NUL terminated8-bit alias，body6+A/frame8+A；A不含NUL。不是physical/source ID，沒有self/peer flag、kind、success、epoch、generation、requestID、roster終止符。
Handler跳opcode後reader4A2A50，DWORD→EBP-0C，alias→EBP-7C，capacity100包含NUL。Reader s外部copy/seek、native inputlength/全body exact-end檢查仍未知。Java MS950屬另一producer證據；Native這段沒有固定codec declaration，不能用ASCII equality代替動態表authority。
Signed63D54C門檻78B9：低版4A90D5與高版4A90BA都push同一634370 literal ds。高版額外push EBP-14 pointer，但format只含ds，reader遇NUL停止，不消費第三variadicfield。
完整handler只見三處lea引用EBP-14，沒有可見local寫入；phase4/5後續傳給opaquecollectionhelpers的來源未閉合。不能虛構dsd、額外nodeID或把未使用argument當wirefield。這是內部資料資格缺口，不是wire correction。

Phase與room/name gates
64AF98==1或6跳4A9472預覽分支，incomingroom DWORD須0；不是所有phase都比對64ABF4。
其他所有數值phase走commonbranch，沒有此處2/3/6 whitelist：先incomingDWORD對64ABF4，不同→4A9549return；相同才比較64ABF8與incomingalias via4D3A80。
Native self比較zero不要求名單內有列，也不檢查pending C_D0或requestnonce。64ABF8是selected/member displayglobal，亦可被S_CD在其roomgate前覆寫，不能當獨立physical/source/player/owner identity授權。

Common peer removal
Nonzero selfcompare→掃combatant begin64AB64/end64AB68，然後other begin64AB54/end64AB58；record stride8C/140，以4D3A80(incomingalias,record+4)==0找各分類第一match。
D0沒有kind欄位，兩個vector都掃；同名跨分類時可能各刪一列，不能概括成只刪全域第一match。
每個match呼叫465370，參數是stored record+4原始名字；再把後續140byte records向前copy35DWORD，end減140，capacity不變。Common roster compaction沒有allocator/free call。執行仍要求vector coherent/initialized與table已有效。
兩個vector均無match時在phase4/5其他效果前return；沒有列可刪是no-op，不是自身返回。普通peer match也不呼叫469CA0(1)。
Phase4/5且確實有刪除才進formatting／opaque collection分支。只記完整handler的直接callsite與未知，止於4B4B00/4B4A10/46F7E0/46F7A0/46F380等，不重建world或results。

UI removal直接helper
465370完整191bytes，item+10 text和傳入storedrecord名作逐byte NUL exact comparison，這裡沒有4D3A80。
匹配時virtual[edx](1)、external77409100移動pointerarray，count+2C減一，selection+28調整；刪除後重查同一index，因此會刪所有matching UIitems。
不能將此helper的raw comparison推广成Native self/roster comparison；incoming經動態表匹配後，傳UI的是既有record原始名字。UIruntime object/vtable與memorycopy actualprovider/body未知，沒有GUI consumption acceptance。

Native self分支及場景邊界
4A911D先call469CA0(1)，返回後才450B90查64A958 selectedrow；index>=0再查一次，row+4 WORD傳46A540，無選列傳0。Scene callbacks可能先改UIselection，不能假設取到原選列或owned列。
469CA0若62C21C==0只將signedWORD1存64AF98並return。若非零，jump表mode1→469CC9。
Mode1先virtual+50，再寫phase1，走UIvisibility/selectionhelpers；把62C9B8/BC/C0設46D640/46D8A0/46D630，458830 reset、protected8316B0/B4/B8條件notification、4687B0(0)、46A520、virtual+4C、49BB40(0)，75A998/9C清零。
46A520是已知C_CE room-list request。動態回呼可再改global，不能宣稱phase/callback最終不變或atomic transition。
46A540完整132bytes：先450230(UI64A954)，再預覽vectorclear。Local copy loop source==oldend因此跳過；64ACF8=64ACF4；4687B0接WORDarg。SI非0才送既有C_CD=205，format cd，DWORD是WORD zeroextension；SI0不送。
C_CE/C_CD是已知helper呼叫補證，不是新增wire、D0 ack、leave Confirm或servercommit證明。
virtual/private、4687B0、49BB40、UItype/lifetime、protecteddata、格式化pointer actualprovider与opaquephase4/5collection止於精確unknown。

Phase1/6預覽分支
incomingroom須0，再以4D3A80掃64ACF4/F8 previewrecords，刪第一match，UI64A954走465370、compact140bytes，4B3B80 RET8後64ACF8減140。沒有self比較或導航。
Room1 ArenaBot若實際Nativephase6會在room0gate被拒絕；5ED/49A沒有Native實際phase證據，不能把adapter ROOM直接等同某個Native phase。

封存frames：沒有普通self-exit往返
5ED frames SHA A9BCA0ECC611F76EE1BD2D6BE6F3B512DC5ABD169C60DACEB6FC74C6D7E0D69F：1based69 Host-GAME ArenaBot body14/frame16，在所有同epochCD前；137 Guest-GAME BattleHost(BattleMoon) body28/frame30。
49A frames SHA283A3DBFA77668FDAA0D55CF67239218CF34D546B53968DFFFBD973A8D093004：1based70 Host-GAME ArenaBot body14/frame16，在所有同epochCD前。
全部roomDWORD1、NUL/exactbody對sealed資料consume0remaining；trace沒有C_D0，沒有ordinary own C_D0/self-D0 dynamic acceptance。Guest收到另一人名字不能改寫成自己返回。來源索引同時列0based與1based，不從R514數字猜trace來源。

最小consumer交接
Explicit AD368 adapter在owned epoch/CE-established room1可consume ArenaBot：有列刪除、無列no-op，維持ROOM與self/source/skills identity，繼續後續CD。不要求先有bot CD，不合成roster completion/tombstone/leave intent。
Knownpeer只刪roomlocal row/projection；ownself通知須獨立qualified ownrawalias及同context已實際emit一次ordinary C_D0意圖，consume一次；adapter localCHANNELpolicy不等於原版comparison或serveratomicpostcommit。
Native自身分支沒有pendingintentcheck，這不給adapter免除自己的freshness/intent檢查。CE owneralias也不是一般self proof。
D0沒有epoch/joingeneration/nonce；用transportepoch＋localjoingeneration綁事件，舊intent/removal不跨join，sameepoch重用roomID仍有ambiguity。Native phase split與private tables guard保持。
比較表沿用Batch15：startup與S_3A可reinit，private05010B41與externalfold773B2A40 actualoutputs/locale/lifecycle缺失；diskzero或ASCII/casefold不是authority，不新增tableinjection API。
Java原本指出selfsend先於membership/CHANNELcommit；本組不接管其producer/postcommit資格，不以C_CE/C_CD/peerD0補成commit。

封存範圍
newwire0/newopcode0/wirecorrection0；server-packet-delta B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C保持；17priorpins含Batch15 1C2519F9保持。
有限Native接收契約完成，overall/native UI/physical/source/player/owner ready皆false。本批到此停止，後續runtime或private provider資格須另有授權與證據。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_SD0_receive_boundary_complete':True,'comparison_guard_retained':True,'actual_native_tables_UI_scene_qualified':False,
      'instruction_assertions':75,'aligned_internal_branch_targets':45,'new_code_ranges':5,'new_data_ranges':7,'inherited_raw_ranges':20,
      'complete_handler_bytes':1216,'UI_remove_bytes':191,'scene_mode1_bytes':506,'selected_preview_bytes':132,'sealed_D0_frames':3,'prior_primary_pins_preserved':17,
      'ArenaBot_before_any_CD_qualified_as_sealed_wire':True,'peer_self_preview_branches_distinct':True,'ds_both_versions_no_third_field':True,
      'ordinary_C_D0_self_D0_dynamic_or_server_commit_qualified':False,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,
      'packet_modified':False,'Java_taken_over':False,'physical_source_player_owner_GUI_accepted':False}
    complete=load('completeness-audit.json');complete['SD0_receive_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text(encoding='utf-8').split('\n第十六批S_D0 receive：')[0]
    entry.write_text(txt+'\n第十六批S_D0 receive：SD0-receive-contract.json + SD0-receive/READ-ME.txt。4A9090完整1216bytes；75指令／45alignedbranch／5code+7data／20沿用raw／3sealedD0。Common room/self/peer與phase1/6 room0 preview分開；peer absent-row不導航，self先scene後selectedpreview/C_CD。兩版都是ds，extraargument非第三欄，phase4/5 localdata來源unknown。Batch15 comparison guard、17priorpins/newwire0/packet不改；不接管Java/postcommit或擴大battle/results/ready。\n',encoding='utf-8')
    phase='BATCH16_SD0_RECEIVE_BOUNDARY_SEALED_NATIVE_AUTHORITY_GUARD_RETAINED'
    handoff=load('handoff.json');handoff.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),SD0_receive_contract=pin,finite_SD0_receive_boundary_complete=True)
    added=[pin]+[record(p)for p in sorted((ROOT/'SD0-receive').iterdir())if p.is_file()]
    for group in ['modern','launcher']:
        mapped={p['path']:p for p in handoff[group]};mapped.update({p['path']:p for p in added});handoff[group]=[mapped[k]for k in sorted(mapped)]
    handoff['shared']=[record(ROOT/'completeness-audit.json')if p['path']==str(ROOT/'completeness-audit.json')else p for p in handoff['shared']]
    handoff['server']['SD0_known_native_receive_corroboration']=pin
    handoff['next_steps']=[
      'Modern explicitAD368 adapter consume currentownedepoch/CEroom ArenaBot absent-rowD0 withoutleavingROOM; continueorderedCD. Native4D3A80/private tableguard remainsBatch15.',
      'Distinguishcommon currentroom/self/peer fromnativephase1or6 room0 preview. Commonpeer removesfirstmatch fromeachkindvector, notselfnavigation; Nativeactualphase/vector/UIauthority remainsunknown.',
      'Native selfbranch doesnotcheckC_D0 intent; adapter independentlyrequiresqualifiedownrawalias + actuallyemittedsamecontextleaveintent. Noordinaryself-exit dynamicacceptance/serverpostcommit granted.',
      'Bothversions literal634370 ds. >=78B9 extraEBP-14pointer unusedbyreader; phase4/5 opaqueuse remainsuninitializeddata/provenancegap, notthirdwirefield.',
      '469CA0(1) directscene callbacks then46A540 optionalknownC_CDpreview qualifiedonlystatic. Virtual/external/UI/deepcollectionunknown. Javaownsproducer/postcommit; native/capture/SQL/oldmatrix/playerclaim0; finitebatchstop.']
    assert handoff['server']['packet_delta']==packet;write('handoff.json',handoff)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch16 S_D0 receive finiteboundarysealed: peer/self/preview gates anddirectscenehelpers qualifiedstatic; nativecomparison/UI/callback/serverpostcommit unknown,guardretained.',SD0_receive_summary=summary)
    progress['handoffs']['modern']='SD0-receive-contract.json: knownD0 ds/room/name removal; absentArenaBot no-op; nativephase1/6 previewdistinct; selfintent/epochadapterpolicyseparate; Batch15tableguard'
    progress['next_steps']=handoff['next_steps'];write('監製進度.json',progress)
    for p in report['prior_primary_pins']:assert record(Path(p['path']))['sha256']==p['expected_sha256']
    names=['SD0_receive_audit.py','review_SD0_receive.py','publish_SD0_receive.py','SD0-receive-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(p.relative_to(ROOT))for p in (ROOT/'SD0-receive').iterdir()if p.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'prior_pins_preserved':17,'delivery':record(ROOT/'delivery-verification.json'),'packet_unchanged':packet,'comparison_guard_retained':True},ensure_ascii=False))

if __name__=='__main__':main()
