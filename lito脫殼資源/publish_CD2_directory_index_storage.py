"""Seal Batch32 finite counters/query scope and proposed Modern safety table."""
import sys
sys.dont_write_bytecode=True
from audit import *
from seal_delta import seal
def load(n):return json.loads((ROOT/n).read_text('utf-8'))
def main():
    p=load('CD2-directory-index-storage-contract.json');pin=record(ROOT/'CD2-directory-index-storage-contract.json');r=load('CD2-directory-index-storage/independent-review.json');v=p['verification']
    assert r['all_match']and r['primary']==pin and(r['check_count'],r['decoded_instruction_rows'],r['model_count'])==(151,79,14)
    assert(v['new_code_ranges'],v['new_data_ranges'],v['necessary_reused_raw_ranges'],v['instruction_assertions'],v['prior_primary_pins_preserved'])==(0,1,8,35,33)
    for q in p['sources']+list(p['existing_evidence'].values())+p['outputs']+[r['models']]:
        a=record(q['path']);assert all(a[k]==q[k]for k in ['path','bytes','sha256'])
    for q in p['prior_primary_pins']:
        a=record(q['path']);assert a['sha256']==q['expected_sha256']and a['bytes']==q['bytes']
    packet=record(ROOT/'server-packet-delta.json');assert packet==v['server_packet_unchanged']
    reader=ROOT/'CD2-directory-index-storage/READ-ME.txt'
    reader.write_text('''第三十二批：directory count／cursor／selected index／storage有限契約

主入口 ../CD2-directory-index-storage-contract.json；count-cursor-selected-index-contract.json提供VA/RVA/sourceSHA/width/trigger/phase/條件/未知；Modern-safe-directory-adaptation-table.json是可實作的strictadaptation提案，並非已改Modern或原版Native驗收。
必要code全在既存封存：0newcode、1data（76079C/B0共8bytefixedsnapshot）、8minimalreused slices；35operand assertions、79decoded rows、151newchecks、14newconditionalmodels全match，33priorpins未改。沒有oldPASS/generator/review/model重跑。

Count與cursor來源：
S03 afteropcode c BYTE→local→4A7464 MOVSX signedDWORD，4A746B存7607A0；4A7471 cursor76079C=0。BYTE128..255變-128..-1；該82byteprefix沒有0..8guard。
Fixedraw兩DWORD全0，只是snapshot；不授loader/currentstate、epoch或選中角色資格。NamedASMcursor6rows/count5rows包括各definition，exactdirectxref不是alias/privatebody/fullprogramwriteclosure。
CB entry4A7572→4D6970第一unknownSTOP；其後獨立4A7577/7C讀index/count，4A7582CMP、84signedJL index<count到row。沒有nonnegative或arrayend guard。Alternative4A7587→496E80 unknownSTOP。
4A758C/7591/7594兩LEA*5/*9/*4：ESI=62FAA0+index*180，32bit算術可能wrap，只有地址表達式，沒有validrow/currentepoch/storageowner證明。
Rowparser前兩strings沿Batch31 externalcopy/seek未知；postparser4A763C準備ECXrecord、EDX0、stackrecord+95，4A7647→46EA30第一unknownSTOP，不展開body。
其後獨立4A764C重新讀globalcursor，INC、讀globalcount、storecursor，signedCMP；next<count返回，else4A7667 count0、4A7671 cursor0，再4A767B→49A510第一unknownSTOP。
這不是frozen原始rowindex的atomictransaction：未知helper或reentry可以影響globals，沒有新動態mutation/race證據。Even正常helperreturn、stableglobal模型完成Nrows，也不授wholeparse、GUI或wireACK。Localcount清0不應作Modern row內容數據丟棄規則。

Selected index來源：
Ordinarycallback49290D EDX3、492912 ECX0，492914→494120第一privateunknownSTOP。只授傳入數字，不能把3猜成capacity/maximumindex；EAXreturndomain未證明。
其後492919僅cmp-1→492F81reject；其它值進stride與+2DFF/+2EFF/+37nonzero markers，68byteisland沒有index>=0或index<count。
Receivecursor76079C與queryEAX是兩個不同來源，沒有same-row/epoch identity橋接；markerchecks不是ownership或bounds。後492958→49A510STOP。
Batch31其它query0/4並不把ordinary0/3域補成可用上界；未開494120、其virtual/helper或任何未知body。

Storage可證明範圍：
8個ordinal的180byte spans，62FAA0..63003F共1440bytes，其端點都在同fixedPEsection且rawbacked。僅metadata映射，不是Nativearrayallocation/initializedrow/合法8容量。
已知四組markerreset及四slotaliasupdate是各自有限loop；不能反推全storage4容量，也不能用server0..8producerpolicy授Native8容量安全。
沒有arrayend/allocatedextent/helperreturndomain/runtimeownership資格；本批沒有新增1440byteextract、heap/runtimeprobe或完整程序掃描。Server8-rowpolicy不要求縮減。

Modern安全表（strictadaptation，需Modern本組未來驗收）：
1 Count：wireu8按pinnedAD3680..8policy驗證，在ownedlist保存expectedCount，與NativeMOVSX或completionzero分離。
2 Row：只收activebuilder/currenttransport/source/directorygeneration的CB；boundedMS950CStrings/exact79bytesuffix/exactbodyend，完整parse後commit，received<expectedCount。
3 Completion：精確Nvalidatedrows完成；N0empty完成、無defaultrow0；extraCB拒絕out-of-sequence，missingrows維持incomplete；沒有猜S04/S05endmarker或timercompletion。
4 Selection：只能completeimmutablecurrentrows中0<=index<actualRows且generation匹配，dispatchintent前revalidatehandle，使用server-issuedname與canonicalsource，不採未知NativeEAX作authority。
5 Epoch：transportreconnect/reauth/sourcegenerationchange/newS03snapshot失效builder/pendingrow/UIhandle，丟lateoldgeneration。這只失效localdirectoryhandle，不能斷言serverphysicalselected/ready已clear。
6 Ready：explicituserintent與currentroom/join/session/servergates獨立；+3E/nativeindex/marker/protectedselector不得授ready；D1/E1receipt、GUI與WORLDgap分開。
PhysicalID/sourceID仍server-ownedtuple；不得由index或truncateddisplayalias反推。這份table沒有修改或測試liveModern，也沒有打斷其它chat。

KnownS03 body2/frame4、CB雙MS950CStrings+79bytessuffix保持，沒有index/epoch新wirefield、沒有newwire/opcode/correction/capture/frameadoption；server-packet-delta.json26509 SHA B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C不變，不向server要求縮domain或改JavaSQL。
Batch29/30/31原SHA及localguard/intent/servercommit/D1/E1/GUI層級保留。GUI/runtime/query/storage仍open；Modernauthorizedintegration需要另驗builder/selection/epochrefusal，未知Nativehelper不擅開。
只寫自有ROOT，Python-B與sys.dont_write_bytecode=True；沒有Native/GUI/JavaSQL/provider/JVM/Modern/登入器/BIN動作、unknowncallee/WORLD/bot/matching擴張或被攔截事項接手。
Prior externalpyc清理auto-review拒絕blocked by policy仍open，scope-write-correction.json SHA B51ECB6CC2AD37DA279E03F71B92C6B75DA52F00027737A6AB8AC2A02C97DFE4保持；未重試、改述、轉派、替代或繞過。
''',encoding='utf-8')
    summary={'evidence':pin,'finite_directory_count_index_storage_complete':True,**v,'independent_checks':151,'decoded_instruction_rows':79,'new_conditional_models':14,'Modern_safety_concerns':6,'native_capacity_not_inferred':True,'selected_query_return_domain_open':True,'runtime_GUI_authority_open':True}
    complete=load('completeness-audit.json');complete['CD2_directory_index_storage_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第三十二批directory count/index/storage：')[0]
    entry.write_text(txt+'\n第三十二批directory count/index/storage：CD2-directory-index-storage-contract.json + CD2-directory-index-storage/READ-ME.txt + Modern-safe-directory-adaptation-table.json。7607A0 signedcount/76079Ccursor來源、postunknown46EA30再讀increment/compare/clear；selectedquery494120 ECX0/EDX3 STOP，EAX只-1gate，非安全bound。Rawbacked62FAA0..63003F不授arraycapacity，四marker也不授4/8，server8policy不縮。Modernstrictcount0..8/rowexactN/ownedindex/generation失效/ready分層提案，非liveModern或Native驗收。0code/1data/8reuse/35assert/79decoded/151checks/14models/33priorpins，newwire/capture0/packet不變，cache清理拒絕仍open。\n',encoding='utf-8')
    phase='BATCH32_DIRECTORY_COUNT_INDEX_STORAGE_SEALED_NATIVE_QUERY_CAPACITY_RUNTIME_GUI_CACHE_CLEANUP_OPEN'
    h=load('handoff.json');incident=h['scope_write_correction'];assert incident==v['scope_write_correction']
    h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),CD2_directory_index_storage_contract=pin,finite_directory_count_index_storage_complete=True,Modern_safe_directory_adaptation_table=record(ROOT/'CD2-directory-index-storage/Modern-safe-directory-adaptation-table.json'))
    added=[pin]+[record(q)for q in sorted((ROOT/'CD2-directory-index-storage').iterdir())if q.is_file()]
    for group in ['modern','launcher']:
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k]for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json')if q['path']==str(ROOT/'completeness-audit.json')else q for q in h['shared']]
    h['server']['known_directory_count_index_storage_boundary']=pin
    h['next_steps']=[
      'Batch32 finitecount/cursorsealed:7607A0 fromS03BYTE MOVSX signed;76079C reset0;CBsignedindex<count thenbase+index180. Unknownentry4D6970/alt496E80/rowstrings remainSTOP. Post46EA30 unknownrereads globalcursor,count beforeincrement/compare/clear0;notatomicfrozenrow orwireACK.',
      'Selectedordinaryquery492914→494120 ECX0/EDX3 firstunknownSTOP;outputEAXdomainunknown,calleronlyrejects-1,notindex>=0/countbound. Markeravailability/privatequery/receivecursor distinctfromphysicalID/source/room/readyauthority. No helperbody expansion orguess4/8bound.',
      'Rawbackedendpointspan62FAA0..63003F/8x180 mappedfixedPEbytes only;notallocated/initializedNativearraycapacity. Batch31 fourmarker/fouraliasloops limitedscope. Neither4nor8safeNativecapacityproved;serverAD3688-rowpolicy unchanged,no reduction/newpacket/JavaSQLfix requested.',
      'Modern-safe-directory-adaptation-table.json proposesstrictownedcount0..8,boundedexactCBrowparse,currentbuilderreceived<N,exactNcompletion/noendmarker,completeownedrowindexbounds,currenttransport/source/directorygenerationhandles andinvalidation,newS03snapshot. Localhandleinvalidation doesnotassertserverphysicalselected/readyclear;proposalneedsModernauthorizedintegrationacceptance,notNative/liveModernproof.',
      'Batch29/30/31 localrecord/guard/userintent/servercommit/D1_E1/GUI dependencies andSHAretained. Readyneedsindependentexplicitintent/currentroom/join/servereligibility;no3E/nativeindex/protectedselector permission orlocaltimerWORLDgrant. CurrentModernchat notmessaged/interrupted.',
      '33priorpins;0newcode/1data8bytes/8minimalreused slices/35assert/79decodedrows/151checks/14newconditionalmodels. Newwire/opcode/correction/capture/frameadoption0,packetB4C00BF1unchanged. OnlyownedROOTwrites,Python-B/dont_write_bytecode;noNativeGUI/JavaSQL/provider/JVM/Modern/launcher/BIN orunknowncallee/WORLD/bot/matching actions;finitebatchstop.',
      'RemainingNativequerydomain/helpercompletion/index/storage/GUI andModernepoch/row/selectionrefusal acceptance open. Prior externalpyc cleanup rejectionblocked bypolicy preservedsamepin;no retry/reframe/transfer/alternate/bypass orotherintercepts resumed.',
    ]
    assert h['scope_write_correction']==incident and h['server']['packet_delta']==packet;write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch32 count/cursor/directselectedquery andrawmappedspan sealed;Modernstrictdirectory/epochadaptationtable ready;Nativecapacity/query/runtimeGUI andpriorcachecleanupopen.',CD2_directory_index_storage_summary=summary)
    progress['handoffs']['modern']='CD2-directory-index-storage-contract.json + Modern-safe-directory-adaptation-table.json:ownedcount/rowconsistency/index/generation/readyseparation;Nativehelperreturn/capacityunknown,posthelperglobalrereadnotatomic. Proposedadaptationonly,no liveModern acceptance.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['CD2_directory_index_storage_audit.py','review_CD2_directory_index_storage.py','publish_CD2_directory_index_storage.py','CD2-directory-index-storage-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT))for q in (ROOT/'CD2-directory-index-storage').iterdir()if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':pin,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),'priorpins':33,'packet_unchanged':packet,'scope_write_correction':incident},ensure_ascii=False))
if __name__=='__main__':main()
