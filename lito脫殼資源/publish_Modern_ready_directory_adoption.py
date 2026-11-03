"""Seal finite Batch33 read-only comparison and one minimal future integration scope."""
import sys
sys.dont_write_bytecode=True
from audit import *
from seal_delta import seal
def load(name):return json.loads((ROOT/name).read_text('utf-8'))
def main():
    p=load('Modern-ready-directory-adoption-contract.json');primary=record(ROOT/'Modern-ready-directory-adoption-contract.json');r=load('Modern-ready-directory-adoption/finite-static-review.json');v=p['verification']
    assert r['all_match'] and r['primary']==primary and v['prior_primary_pins_preserved']==34
    for q in p['outputs']+p['sources']+list(p['existing_evidence'].values()):assert record(Path(q['path']))=={k:q[k] for k in ['path','bytes','sha256']}
    for q in p['prior_primary_pins']:assert record(Path(q['path']))['sha256']==q['expected_sha256']
    assert record(ROOT/'server-packet-delta.json')==v['server_packet_unchanged']
    reader=ROOT/'Modern-ready-directory-adoption/READ-ME.txt'
    reader.write_text('''第三十三批：Modern directory／ready 只讀實作差距比較

主入口 ../Modern-ready-directory-adoption-contract.json。
implementation-comparison-table.json：13項，每項 source SHA／function／line／trigger／預期拒絕或保留及資格限制。
source-read-catalog.json：14個實作檔與12個既有 harness/build 名稱來源，共26個讀取；讀取前/content/後 hash一致。每檔穩定，跨檔非atomic R525版本，不授編譯或runtime驗收。外部來源後續可變；自有captures是封存依賴，不要求freeze、不打斷R525。

8項已實作（source presence，非測試PASS）：
M01 Java count0..8，非adapted四slot UI另有政策；不推Native容量。
M02 cap28/17 CStrings、exact79 suffix／bodyend、MS950／Java domain／reserved0；完整next row驗證後才commit identity。直接consumer失敗可能已retire scene，非全狀態transaction；stream拒絕則退役epoch。
M03 exactN rows、count0empty、extra/no-count拒絕、missing保持incomplete。
M04 complete owned row/get(slot)、opaque epoch/roster/scene/selection revisions、dispatch再驗證與raw server-issued name。
M05 newS03失效localrow/handle/self；active navigation/pending selected write拒絕替換並在stream退役；不推server selected/skills/ready clear。
M06 ownerArc+epoch、fresh GAME redirect／reauth boundary清localroster／sender／stream／room；source control另驗bootstrap revision。
M07 S_D1 bounded exact0/1/2、current joined room/CE revision；unknown/ambiguous alias保留row並記錄ignored，不buffer成人物。
M08 complete singleton來源+matched completed C9/C8 serial/current source/roster/join qualification；owner display link只是觀察，不能由index／reserved3E授self/member/owner/ready。

2項具體 ordinary adapter pipeline缺口：
M09 Sender完整typed接口與Runtime Intent目前沒有user C_D2 state0/1/2；fixture rawbody不是product ready API。
M10 S_E1/225缺constructor與ingress route，落generic Snapshot206/213拒絕，UnsupportedBody退役epoch。加入qualified exactE1 prestart receipt projection即可；不能用localtimer補WORLD。
不主張Modern TLS rooms ActionReady不存在或可替代ordinary D2；比較範圍只限已讀的Native ordinary／explicit Java adapter pipeline。

3項產品決策／新證據：
M11 CB/D1沒有wire directory/join generation；sameepoch新S03或rejoin後不可辨識old同形body。現有epoch/handleguard不能授這種stale detection；未證明reachable race，不增wirefield。
M12 Java核心8rows與四slot anchor/command mapping分離；UI5..8列需產品呈現決策，本批沒讀windowrouting／沒GUI證據，不減server8policy。
M13 AD368 ready source/finite network接受狀態保留；485F只已接受settings unit／limited network，ready comparison仍active/unaccepted，不搶讀或升格。Repeatstate1可能取消COUNTDOWN、state2表示unready但D1state2；localwrite不是ACK。

最小採用順序 minimal-adoption-order.json：先等既有R525及485F ready比對由監製驗收，選定serverpin；直接沿用M01–M08；只派一個ordinary joined-room ready/prestart task補M09/M10。初始scope限獨立qualified singleton self/currentroom；更廣self／observer／owner／battle資格需accepted evidence+server gates。不得重做directory／D1已存在保護。UI5..8與newwire/Nativehelper/WORLD不加進最小task。

既有 #[test]名稱只有本批選定self來源3項：count_or_multirow_cannot_supply_singleton_alias、complete_full_frame_required_once、failed_partial_or_changed_context_cannot_complete；只記source名稱與line，沒執行／沒採body／不宣稱coverage或PASS。其它選定consumer來源只列function headers，非測試PASS，非repo全域absence；沒有新測試／build／compile。
34priorpins保持；本批0新code/data/rawslice/assert/capture/wire/opcode/correction，packet delta維持B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C。
只寫自有ROOT，Python-B及dont_write_bytecode；沒有Modern/登入器/BIN改動、Native/GUI/input/injection/Java/SQL/provider/JVM、unknown/private/virtual/externalbody、WORLD/bot/matching擴張或外部helperimports。其他攔截均保持，有限封存後停止。
此前externalpyc清理被automatic approval review以blocked by policy拒絕，仍open，scope-write-correction SHA B51ECB6CC2AD37DA279E03F71B92C6B75DA52F00027737A6AB8AC2A02C97DFE4不變；未重試／改述／轉派／替代／繞過。
''',encoding='utf-8')
    summary={'evidence':primary,'finite_read_only_Modern_comparison_complete':True,**v,'finite_static_checks':r['check_count'],'source_anchor_checks':r['anchor_count'],'Modern_runtime_GUI_acceptance':False,'ready_integration_not_implemented':True}
    complete=load('completeness-audit.json');complete['Modern_ready_directory_adoption_batch']=summary;write('completeness-audit.json',complete)
    entry=ROOT/'閱讀入口.md';txt=entry.read_text('utf-8').split('\n第三十三批Modern只讀比較：')[0]
    entry.write_text(txt+'\n第三十三批Modern只讀比較：Modern-ready-directory-adoption-contract.json + Modern-ready-directory-adoption/READ-ME.txt／implementation-comparison-table.json／minimal-adoption-order.json。26前後hash一致read/14implementationfiles，8implemented source-only、2concretegap（ordinarytypedD2及E1route）、3product/evidence，34priorpins。R525及485F ready active不打斷／不升格；沿用directory/opaquehandle/epoch/D1/self資格，未來只派一個ready/prestart task；無Modern編輯測試build/NativeGUI/newwire，packet不變/cachecleanup拒絕open。\n',encoding='utf-8')
    phase='BATCH33_MODERN_READ_ONLY_COMPARISON_SEALED_READY_INTEGRATION_VERSION_NATIVE_GUI_CACHE_CLEANUP_OPEN'
    h=load('handoff.json');incident=h['scope_write_correction'];assert incident==v['scope_write_correction']
    h.update(updated_at=stamp(),phase=phase,entrypoint=record(entry),Modern_ready_directory_adoption_contract=primary,finite_read_only_Modern_comparison_complete=True,Modern_ready_directory_implementation_comparison_table=record(ROOT/'Modern-ready-directory-adoption/implementation-comparison-table.json'),Modern_ready_directory_minimal_adoption_order=record(ROOT/'Modern-ready-directory-adoption/minimal-adoption-order.json'))
    added=[primary]+[record(q) for q in sorted((ROOT/'Modern-ready-directory-adoption').iterdir()) if q.is_file()]
    for group in ['modern','launcher']:
        mapped={q['path']:q for q in h[group]};mapped.update({q['path']:q for q in added});h[group]=[mapped[k] for k in sorted(mapped)]
    h['shared']=[record(ROOT/'completeness-audit.json') if q['path']==str(ROOT/'completeness-audit.json') else q for q in h['shared']]
    h['server']['read_only_Modern_ready_directory_comparison']=primary
    h['next_steps']=[
        'Batch33 sealed read-only comparison:26hash-stable per-file reads/14implementationfiles,13findings=8implemented source-only+2concretegaps+3product/new-evidence;34priorpins. Per-file stable doesnotgrant atomicR525/build/runtime/NativeGUI acceptance. No freeze/messages/interrupts.',
        'Reuse existing Java count0..8/bounded exactCB/exactNcompletion/currentownedrow/opaque epoch+roster+scene+selection handles/newS03invalidation/reconnectreauth/D1currentjoin+uniquealias/singletonself qualification. No duplicate directory/D1 protections orserver8policyreduction. Serverphysicalselected/skills/ready clear notinferred.',
        'After existingR525 and485F readycomparison supervisor acceptance/selectingserverpin, dispatchONE ordinaryjoinedroom ready/prestarttask forM09typedexplicitD2state0/1/2 control andM10qualifiedexactE1route/projection. Reuse currentepoch/source/roster/roomCE/navigation/self contexts andSender; localwrite notACK, D1/E1observations notowner/allready/WORLD grant.',
        'M11sameepochCB/D1 generation ambiguity needsserialization/order evidence, M12UIrows5..8presentationneedsproductdecision, M13AD368readybaselineaccepted versus485Fsettings-onlyaccepted/readyactiveunaccepted. Do notguesswiregeneration/physicalID/nativecapacity/observerownerbattleeligibility orpromoteactivecomparison.',
        'No newcode/data/rawslices/assert/capture/wire/opcode/correction;packetdelta B4C00BF1 unchanged. OnlyownROOTwrites,Python-B/dont_write_bytecode;no Modern/launcher/BIN edits,NativeGUI/input/injection/JavaSQL/provider/JVM/tests/builds/compile/externalhelperimports/unknownbody/WORLD/bot/matching actions. Existingtestnames3 sourceonly,oldPASS/generators/reviews notrerun.',
        'Nativehelperquery/storage/GUI/runtime andfutureModernreadyintegration/versionacceptance remainopen. Prior externalpyc cleanup auto-review rejectionblocked bypolicy retainedsamepin;no retry/reframe/transfer/alternate/bypass orotherintercepts resumed. Finitebatchstop.'
    ]
    assert h['scope_write_correction']==incident and h['server']['packet_delta']==v['server_packet_unchanged'];write('handoff.json',h)
    progress=load('監製進度.json');progress.update(updated_at=stamp(),phase=phase,current_task='Batch33 read-only Modern comparison sealed:8existing protections,2ordinaryD2/E1 integration gaps,3decisions/evidence;R525/485Freadyactiveuninterrupted;NativeGUI/cachecleanupopen.',Modern_ready_directory_adoption_summary=summary)
    progress['handoffs']['modern']='Modern-ready-directory-adoption-contract.json + minimal-adoption-order.json:reuse existingdirectory/selection/epoch/currentjoin/D1/self;afterR525+acceptedreadyversion onlytypedordinaryD2/E1integration. Sourceinspectiononly,no runtimetest/GUI proof.'
    progress['next_steps']=h['next_steps'];write('監製進度.json',progress)
    names=['Modern_ready_directory_adoption_audit.py','review_Modern_ready_directory_adoption.py','publish_Modern_ready_directory_adoption.py','Modern-ready-directory-adoption-contract.json','handoff.json','completeness-audit.json','閱讀入口.md']+[str(q.relative_to(ROOT)) for q in (ROOT/'Modern-ready-directory-adoption').iterdir() if q.is_file()]
    count=seal(names,Path(__file__))
    print(json.dumps({'primary':primary,'changed_files_checked':count,'delivery':record(ROOT/'delivery-verification.json'),'priorpins':34,'scope_write_correction':incident},ensure_ascii=False))
if __name__=='__main__':main()
