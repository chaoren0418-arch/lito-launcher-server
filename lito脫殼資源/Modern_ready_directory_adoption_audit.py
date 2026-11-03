"""Batch33: finite read-only Modern source snapshots; never import Modern helpers."""
import sys
sys.dont_write_bytecode = True
import hashlib, json, re
from pathlib import Path
from datetime import datetime
ROOT = Path(__file__).resolve().parent
MODERN = ROOT.parent / 'lito重建版' / 'LiTo381-Modern' / 'src' / 'modern_lito'
OUT = ROOT / 'Modern-ready-directory-adoption'
def sha(data): return hashlib.sha256(data).hexdigest().upper()
def pin(path):
    data = path.read_bytes()
    return {'path':str(path),'bytes':len(data),'sha256':sha(data)}
def write(name, obj):
    p = OUT / name
    assert p.resolve().is_relative_to(ROOT)
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def test_names(name):
    p=MODERN.parent.parent/'tools'/name
    before=pin(p);data=p.read_bytes();after=pin(p)
    lines=data.decode('utf-8-sig').splitlines();tests=[]
    for i,s in enumerate(lines,1):
        if '#[test]' in s or any('#[test]' in x for x in lines[max(0,i-3):i-1]):
            m=re.search(r'\bfn\s+(\w+)\s*\(',s)
            if m:tests.append({'line':i,'name':m.group(1)})
    OUT.mkdir(exist_ok=True)
    functions=[];labels=[]
    for i,s in enumerate(lines,1):
        functions.extend({'line':i,'name':m.group(1)} for m in re.finditer(r'\bfn\s+(\w+)\s*\(',s))
        labels.extend({'line':i,'name':m.group(1)} for m in re.finditer(r'"name"\s*:\s*"([^"]+)"',s))
    obj={'source_before':before,'captured_content_sha256':sha(data),'source_after':after,'stable_during_read':before==after and before['sha256']==sha(data),'read_at':datetime.now().astimezone().isoformat(),'existing_test_names_only':tests,'function_names_only_not_test_claim':functions,'literal_case_names_only_not_pass_claim':labels,'script_executed_or_imported':False,'scope':'Only existing test annotations, function headers and literal name labels; no body adoption, no claim compiled/current production test pass.'}
    write(name+'.test-names.json',obj);print(json.dumps(obj,ensure_ascii=False))
def capture(name, ranges=None):
    p=MODERN/name
    before=pin(p); data=p.read_bytes(); after=pin(p)
    lines=data.decode('utf-8-sig').splitlines()
    cutoff=next((i for i,s in enumerate(lines,1) if '#[cfg(test)]' in s),len(lines)+1)
    spans=ranges or [(1,cutoff-1)]
    rows=[{'line':i,'text':lines[i-1]} for a,b in spans for i in range(a,min(b,len(lines))+1)]
    tests=[]
    for i,s in enumerate(lines,1):
        m=re.search(r'\bfn\s+(\w+)\s*\(',s)
        if m and any('#[test]' in x for x in lines[max(0,i-4):i]): tests.append({'line':i,'name':m.group(1)})
    obj={'source_before':before,'captured_content_sha256':sha(data),'source_after':after,'stable_during_read':before==after and before['sha256']==sha(data),'read_at':datetime.now().astimezone().isoformat(),'ranges':spans,'line_count':len(lines),'implementation_lines':rows,'existing_test_names_only':tests,'runtime_or_test_execution':False}
    OUT.mkdir(exist_ok=True)
    write(name+'.snapshot.json',obj)
    print(json.dumps({'name':name,'stable':obj['stable_during_read'],'pin':before,'implementation':rows,'tests':tests},ensure_ascii=False))
def finalize():
    from audit import record, stamp, write as root_write
    old=json.loads((ROOT/'CD2-directory-index-storage-contract.json').read_text('utf-8'))
    snapshots={p.name:json.loads(p.read_text('utf-8')) for p in sorted(OUT.glob('*.snapshot.json'))}
    catalog=[]
    for p in sorted(OUT.glob('*.snapshot.json'))+sorted(OUT.glob('*.test-names.json')):
        j=json.loads(p.read_text('utf-8'));catalog.append({'owned_capture':record(p),'read_at':j['read_at'],'before':j['source_before'],'captured_content_sha256':j['captured_content_sha256'],'after':j['source_after'],'stable_during_read':j['stable_during_read'],'adoption':'source text only' if j['stable_during_read'] else 'tentative_unadopted','existing_test_names_only':j['existing_test_names_only']})
    write('source-read-catalog.json',{'reads':catalog,'consistency':'Per-file hash before/content/after only. Non-atomic cross-file capture during active R525; no freeze, build, tests or runtime acceptance. Later external edits do not alter these immutable owned captures.','test_scope':'Only three actual #[test] names found in the selected implementation; other selected consumer files contain function headers, not test pass evidence. No repository-wide absence claim.'})
    def a(n,fn,start,end,token):
        p=OUT/(n+'.snapshot.json');j=snapshots[p.name];line=next(x for x in j['implementation_lines'] if x['line']==start)
        assert token in line['text']
        return {'source':j['source_before'],'owned_capture':record(p),'function':fn,'start_line':start,'end_line':end,'required_start_token':token,'adopted':j['stable_during_read']}
    rows=[]
    def finding(id,status,concern,anchors,trigger,current,expected,limit,tests=None):
        assert all(q['adopted'] for q in anchors)
        rows.append({'id':id,'status':status,'concern':concern,'anchors':anchors,'trigger':trigger,'current_source_behavior':current,'expected_refusal_or_retention':expected,'qualification_limit':limit,'existing_test_name_evidence':tests or [],'tests_run':False})
    finding('M01','implemented','S03 count policy',[
        a('native_source_identity.rs','decode',52,52,'body.len()==2'),a('native_source_identity.rs','JAVA_ROW_BOUND',8,8,'JAVA_ROW_BOUND:usize=8'),a('native_city_runtime.rs','consume_source_identity_event',147,148,'bootstrap_adapter.is_none()')],
        'S03 exact two-byte body, count u8; explicit Java adapter versus ordinary four-slot UI policy.',
        'Java adapter accepts 0..8; decode rejects >8 (and signed-negative values). Non-adapted UI rejects >4 independently.',
        'Keep server 0..8 policy and expected count as owned data; invalid count rejected by decoded constructor before Runtime receives event.',
        'Implemented Modern policy; no Native array capacity proof. Four-slot restriction is explicitly non-adapted UI only.')
    finding('M02','implemented','Bounded exact CB parse and reserved field separation',[
        a('native_source_identity.rs','Reader::string',47,47,'take(cap)'),a('native_source_identity.rs','decode',55,62,'[(0,28),(0x1c,17)]'),a('native_source_identity.rs','decode',60,61,'r.at==body.len()'),a('native_character_directory_fields.rs','Fields::decode',14,15,'b.len()==79'),a('native_character_directory_fields.rs','Fields::validate_java_projection',18,21,'self.reserved==0'),a('native_city_runtime.rs','consume_source_identity_event',149,149,'r.fields().validate_java_projection()?')],
        'CB with incomplete/overlong NUL strings, short/trailing suffix, invalid Java projection or invalid MS950 display bytes.',
        'Cap28 name / cap17 clan, exact79 suffix and exact end; projection validates reserved0 and counters/check-code; Runtime commits owned next identity after validation.',
        'Reject invalid CB without appending a row. Production stream refusal retires epoch. Direct consumer preserves source_identity on projection failure but may already retire login/character scene; no all-state transaction claim.',
        'No +3E readiness grant; reserved is suffix byte16. MS950 strict display comes from Windows API source only, not executed here.')
    finding('M03','implemented','Exact N completion',[
        a('native_source_identity.rs','State::prepare_packet',75,79,'decode(body)?'),a('native_source_identity.rs','State::prepare_packet Start',77,77,'complete:count==0'),a('native_source_identity.rs','State::prepare_packet Record',78,78,'!next.complete')],
        'Count0; before first S03; fewer than N rows; Nth row; extra CB after completion.',
        'Start replaces local State, expected count retained; zero completes empty. Row parsed then clone append iff received<N, complete iff len==N.',
        'No count: refuse; missing rows: incomplete; zero: no row0; extra: refuse and preserve State (production stream then retires). No end-marker/timer inferred.',
        'Immutable references/private Vec mutation suffice; do not require a second builder or replace completed rows with empty when Native globals clear.')
    finding('M04','implemented','Selected row and opaque handle revalidation',[
        a('native_source_identity.rs','State::prepare_select_bounded',87,89,'self.records.get(slot)'),a('native_city_runtime.rs','source_character_control',172,173,'revision:self.source_roster_revision'),a('native_city_runtime.rs','handle_source_character_control',173,173,'control.revision==self.source_roster_revision'),a('native_city_runtime.rs','handle_source_character_submit',183,183,'submit.selection_revision==key.selection_revision'),a('native_city_runtime.rs','prepare_source_send_candidate',214,218,'prepare_select_bounded'),a('native_source_send.rs','Sender::prepare_character',30,30,'body.extend_from_slice(name)')],
        'Incomplete/out-of-range slot, old directory/scene/selection callback, duplicate/pending navigation write.',
        'usize prevents negative slot; complete/bound<=8/get(slot) guard; epoch + roster/scene revision; submit also selection revision. Dispatch clones/revalidates current owned row and sends its exact received raw name.',
        'Refuse stale or unavailable handle before a new reservation. Echo server-issued name, never native query EAX or synthesized ObjectId/sourceId.',
        'Class/variant/+37 checks are availability only; canonical source and physical object tuple remain server-owned. No +3E used by this selection path.')
    finding('M05','implemented','New S03 invalidates local roster handles',[
        a('native_source_identity.rs','State::prepare_packet Start',77,77,'next=Self'),a('native_city_runtime.rs','consume_source_identity_event',145,149,'!self.entry_navigation.active()'),a('native_java_room_self.rs','note_java_self_stream',76,80,'StreamIngress::Roster'),a('native_character_selection.rs','Scene::retire',21,21,'scene_revision')],
        'New S03 while dormant; new S03 while navigation or selected write pending; appended next CB.',
        'Successful Start resets rows/selected; every directory event increments roster revision and retires scene. New Start retires self provenance. Active navigation/pending write refuses replacement; stream error retires source.',
        'Old local row/UI handles cannot select in new snapshot. Fail closed during incompatible navigation; no claim server physical selection, learned skills or ready set cleared.',
        'Compatible with existing serialized Runtime. Same-epoch indistinguishable old CB remains M11, not resolved by a fictitious wire generation.')
    finding('M06','implemented','Owned transport reconnect / fresh authentication invalidation',[
        a('native_source_connection.rs','Scope::admit_epoch',45,45,'Arc::ptr_eq'),a('native_city_runtime.rs','commit_source_boundary',132,132,'self.source_identity=source_identity::State::default()'),a('native_login_events.rs','resume_java_return_fixture',461,464,'begin_source_connection_candidate'),a('native_login_events.rs','reconnect_login_bootstrap_fixture',480,483,'begin_source_connection_candidate'),a('native_login_bootstrap.rs','handle_source_choice',97,101,'c.revision==self.login_bootstrap.key.revision')],
        'Foreign Runtime with same serial; retired transport epoch; source GAME redirect; qualified fresh GAME return.',
        'Epoch binds Arc owner and active serial. New boundary resets local roster/sender/stream/room/self/navigation. Redirect and fresh return call new boundary, restoring only their explicit bootstrap/retained source context.',
        'Reject old epoch events and handles; no reuse of old local roster across fresh GAME. Source controls separately check bootstrap revision.',
        'Normal existing redirect/return paths only; no general arbitrary same-epoch source switching claim. Server resync/selected/ready retention issue remains separate.')
    finding('M07','implemented','D1 receipt parse/current join/unique alias retention',[
        a('native_java_room_participants.rs','MemberState::decode',20,20,'0=>Ok'),a('native_java_room_participants.rs','decode S_D1',47,51,'body[0]==209'),a('native_java_room_participants.rs','Book::observe',113,115,'admit_epoch'),a('native_java_room_participants.rs','Book::observe D1',149,152,'let matches='),a('native_java_room_participants.rs','java_room_participants_route_enabled',176,179,'self.bootstrap_adapter.is_some()'),a('native_java_room_participants.rs','consume_java_room_participants_candidate',187,192,'book.key.room_id==room.id')],
        'D1 0/1/2; malformed state/body; unknown alias; alias shared by multiple kinds; stale epoch or CE room revision.',
        'Bounded cap28 nonempty raw alias/exact end, states0/1/2. Runtime requires adapters, RoomReceipts, non-invalidated book/current room+CE revision; clone observe before commit. Only one raw alias match updates.',
        'Refuse stale join/body; unknown/ambiguous aliases retain rows and record ignored receipt. State2 remains ShopList receipt; never maps directly to ready true or WORLD entry.',
        'No room/generation wire field; current local owned join scope is necessary but cannot detect indistinguishable delayed same-epoch D1. Completeness remains PossiblyIncomplete, not all-ready authority.')
    testj=snapshots['native_java_room_self.rs.snapshot.json'];testrefs=[{'source':testj['source_before'],**t} for t in testj['existing_test_names_only']]
    finding('M08','implemented','Independent singleton self provenance/current room; display owner stays observational',[
        a('native_java_room_self.rs','complete_singleton_alias',7,8,'identity.expected()!=Some(1)'),a('native_java_room_self.rs','singleton_binding_current',53,58,'admit_epoch'),a('native_java_room_self.rs','self_join_current',65,67,'self.singleton_binding_current()'),a('native_java_room_self.rs','note_java_self_stream CharacterAccepted',98,101,'selected.slot()==0'),a('native_java_room_participants.rs','Book::owner_link',98,99,'display_alias==alias'),a('native_java_room_entry.rs','consume_java_room_entry_candidate',139,145,'Phase::AwaitReply')],
        'S03 alone or multirow directory; unmatched C9/C8; changed source/roster/selection serial; another room; owner alias display.',
        'Self requires complete singleton raw row + current source/roster revision + matching selected raw name + completed owned C9 accepted C8 serial. Join is current room/CE/navigation/channel. OwnerLink reports absent/unique/ambiguous display match.',
        'Do not infer self/member/owner eligibility from index/+3E/CE owner name alone. Preserve owner display as observation; use independently qualified self/current join for future intent.',
        'Existing singleton qualification cannot safely identify self for multirow physical aliases; future wider ready scope needs new evidence. These existing test names are source presence only, not pass.',testrefs)
    finding('M09','concrete_gap','Typed ordinary C_D2 user intent/control is absent in examined pipeline',[
        a('native_source_send.rs','Intent',7,7,'SelectCharacter'),a('native_source_send.rs','Sender typed room writers',32,51,'prepare_room_entry'),a('native_city_runtime.rs','prepare_source_send_candidate',213,218,'Intent::SelectCharacter')],
        'User wants ordinary joined-room ready state0/1/2 (not Modern TLS rooms protocol); retired join/source handle or pending write.',
        'Complete selected Sender typed interface has character/source/channel/room entry/settings/mode/map/exit but no D2 method. Runtime public typed Intent branch only accepts SelectCharacter. Crate-private fixture raw writer is not a user ready API.',
        'Proposed integration: explicit opaque current epoch/source/roster/CE/navigation/self-qualified handle; one user intent -> cc(D2,state) for state0/1/2; refuse retired/ineligible/unqualified/pending contexts before reservation. Do not synthesize opcode-only owner-start.',
        'Concrete interface gap only in Native ordinary/explicit Java adapter pipeline; not repository-wide absence. Server is final room/member/battle owner authority; no Native flag/protected selector grant.')
    finding('M10','concrete_gap','S_E1 prestart receipt route absent; valid ordinary E1 currently retires stream',[
        a('native_city_runtime.rs','feed_source_stream_candidate constructor dispatch',245,245,'epoch.decoded_packet(&bytes)'),a('native_source_connection.rs','Epoch::decoded_packet fallback',24,24,'else{Snapshot::decode(body)?;}'),a('native_mode_snapshot.rs','Snapshot::decode opcode domain',34,34,'_=>return Err'),a('native_city_runtime.rs','feed_source_stream_candidate ingress',247,249,'self.consume_source_event(&packet)'),a('native_city_runtime.rs','retire_source_stream',258,259,'disconnect_source_connection_candidate')],
        'Owned joined ordinary room receives exact S_E1 count after ready commits/cancel/tick.',
        'No225 branch in constructor/ingress. Generic Snapshot accepts206/213 only, so E1 constructor fails and UnsupportedBody retires epoch before dispatch.',
        'Proposed integration: exact opcode225+u8 receipt under current epoch/room/CE/navigation generation; retain ordered prestart observations/count/cancel without guessing timer expiry or server ACK. Refuse stale/unqualified/malformed; no automatic owner-start, all-ready or WORLD admission.',
        'Separate prestart projection needed; reuse D1 guards/book rather than rewrite. Current server-version finite count-domain policy must come from accepted ready comparison, not an invented field or end marker.')
    finding('M11','product_decision_or_new_evidence','Same-epoch wire lacks directory/join generation; avoid false stale detection',[
        a('native_source_connection.rs','DecodedPacket',12,12,'epoch:Epoch,body:Arc'),a('native_source_identity.rs','State::prepare_packet Record',78,78,'next.expected.context'),a('native_java_room_participants.rs','decode S_D1',49,51,'let display_alias='),a('native_city_runtime.rs','feed_source_stream_candidate',247,247,'Some(3|203)')],
        'Old CB delivered after new S03 on same epoch, or delayed D1 for same raw alias after same-epoch rejoin.',
        'Owned events carry epoch/body; roster revision protects handles but no wire source/directory/room generation token exists in CB/D1. Current ordered stream/current builder and room scope qualify locally.',
        'Refuse identifiable old epoch/handles; do not claim detection for indistinguishable same-epoch bodies. If product permits overlapping snapshots/rejoin on same connection, decide serialization/epoch retirement with Modern group and obtain actual server ordering evidence.',
        'No new wire field/requested Java change. Not a proven current reachable race, not justification to duplicate epoch/builder protections.')
    finding('M12','product_decision_or_new_evidence','Java count8 versus four-slot presentation policy',[
        a('native_source_identity.rs','JAVA_ROW_BOUND',8,8,'JAVA_ROW_BOUND:usize=8'),a('native_city_runtime.rs','source_character_command_slot',46,46,'SOURCE_CHARACTER_COMMAND_FIRST+4'),a('native_character_selection.rs','slot',10,10,'index<4'),a('native_city_runtime.rs','source_character_control_enabled',171,171,'source_identity::JAVA_ROW_BOUND')],
        'Adapted directory has5..8 rows; UI needs to expose row indices4..7.',
        'Core adapted selection bound supports8, while named four slot anchor/command mapping is4. Current window routing/layout not read or adopted in this batch.',
        'Keep all validated rows/8 producer policy; decide pagination/list/commands if those rows must be interactively visible. No current graphical reachability claim for5..8.',
        'Product presentation decision; four slots do not prove Native storage4. Do not reduce server domain or add UI work to minimal ready dispatch.')
    finding('M13','product_decision_or_new_evidence','AD368 accepted ready semantics versus active unaccepted485F ready comparison',[
        a('native_java_room_entry.rs','Adapter::parse',10,12,'modern_synthetic_AD368_room_entry_v1'),a('native_java_room_participants.rs','Adapter/contract pin',5,5,'CONTRACT_SHA'),a('native_source_send.rs','Sender::prepare_body',53,58,'self.pending.is_none()')],
        'Supervisor chooses candidate server pin for ready task after R525; UI repeats state1, sends state2 or interprets local write as commit.',
        'Accepted AD368 ready report defines repeated state1 countdown cancellation, state2 unready but D1state2, server commit before peer sends.485F settings unit/network report is accepted only for settings; its ready version comparison is still active/unaccepted at this assignment.',
        'Wait for supervisor version comparison acceptance, bind one explicit pin, then integrate. Preserve each user intent and server receipt ordering; no idempotent-set/toggle guess, no local write ACK, no universal transaction/peer rollback or physical ready clear inferred.',
        'Do not inspect/promote active report, freeze Modern or interrupt R525. Existing source text is not Native behavior proof.')
    write('implementation-comparison-table.json',{'scope':'Current read-only Native ordinary adapter pipeline compared with sealed Batch29–32 proposals; implemented = source presence, never executed acceptance.','findings':rows,'status_counts':{s:sum(r['status']==s for r in rows) for s in ['implemented','concrete_gap','product_decision_or_new_evidence']}})
    order={'steps':[
        {'order':1,'action':'Supervisor waits for already active R525 and485F ready comparison; selects accepted server pin. No new task sent by this group.','adoption':'AD368 accepted ready baseline retained;485F settings acceptance does not qualify ready.'},
        {'order':2,'action':'Reuse M01–M08 existing directory/selection/epoch/current join/self/D1 mechanisms; no duplicate directory builder/row handle/parser or protection tests.','adoption':'Source-text inspection only; Modern group owns any later build/runtime acceptance.'},
        {'order':3,'action':'Dispatch ONE ordinary joined-room ready/prestart integration task for M09/M10: typed explicit D2 state0/1/2 control/reservation and exact E1 receipt projection/route under reused owned scope.','adoption':'Minimum initial scope is independently qualified singleton self/current ordinary room; wider self/observer/owner/battle eligibility needs accepted evidence and server gates. D1 is observation, not acknowledgement.'},
        {'order':4,'action':'Carry M11–M13 limits as decisions/evidence prerequisites; row5..8 UI and new protocol/generation fields are outside minimal dispatch.','adoption':'No Native runtime/GUI/WORLD authority or server selected/ready clearing inference.'}],
        'one_task_acceptance_boundaries':['One explicit user intent serializes exact D2,state with existing Sender; refuse stale or unqualified scope before reserving.','Completed local write is not server ready ACK; D1 unique-alias state observation and E1 joined prestart receipt are separate ordered projections.','Valid E1 must stop reaching generic206/213 fallback in qualified ordinary room; malformed/out-of-scope remains refused.','Retire ready handles/projection at owned boundary/join invalidation; never use+3E/index/native marker/protected selector for ready or owner.','Pin accepted server semantics before choosing repeat1/state2 behavior; no inferred all-ready, local-timer WORLD or universal send transaction.'],
        'existing_tests':'Only existing names/header presence catalogued, none executed. No new tests authored or guessed names; these are future task behavior boundaries, not a new test suite.',
        'prohibited_actions_retained':['No Native/GUI/input/injection/Java/SQL/provider/JVM/build/test/capture','No Modern/launcher/BIN writes or external helper imports','No unknown/private/virtual/external body expansion or WORLD/bot/matching scope','No cache cleanup retry/reframe/transfer/bypass']}
    write('minimal-adoption-order.json',order)
    priors=old['prior_primary_pins']+[dict(record(ROOT/'CD2-directory-index-storage-contract.json'),expected_sha256='02B15800508F5608AAB3BF9B04600717E02B3D5042EF7389D31B8F00AE09929D',match=True)]
    for q in priors:assert record(Path(q['path']))['sha256']==q['expected_sha256']
    existing=dict(old['existing_evidence']);existing['Batch32']=record(ROOT/'CD2-directory-index-storage-contract.json');existing['Batch32_safety_table']=record(ROOT/'CD2-directory-index-storage/Modern-safe-directory-adaptation-table.json');existing['Batch31_dependency_matrix']=record(ROOT/'CD2-selected-record-source/Modern-ready-dependency-matrix.json')
    server=ROOT.parent/'lito伺服器'
    for name,expected in [('server-room-ready-prestart-consumer-contract.json','57F324FE093C80A8DAE8B7E207A9A8A628AA508E04EBB38AFE17F3C33D6274F8'),('server-ordinary-room-settings-order-candidate.json','8D969CB59FA4ECD3515EEE794397E4C94D58D97328F8A410A4EA0BBA456CCF9E'),('server-ordinary-room-settings-candidate-network.json','8C1776AA503098E09B42EFE8CA45E07E651DD4570A07CB6A8994006421CCEF41')]:
        q=record(server/name);assert q['sha256']==expected;existing[name]=q
    for q in old['sources']+list(old['existing_evidence'].values()):assert record(Path(q['path']))=={k:q[k] for k in ['path','bytes','sha256']}
    outputs=[record(p) for p in sorted(OUT.iterdir()) if p.is_file()]
    v={'source_read_snapshots':len(catalog),'implementation_files':len(snapshots),'stable_during_read':sum(x['stable_during_read'] for x in catalog),'tentative_unadopted':sum(not x['stable_during_read'] for x in catalog),'findings':len(rows),'status_counts':{s:sum(r['status']==s for r in rows) for s in ['implemented','concrete_gap','product_decision_or_new_evidence']},'actual_existing_test_names':len(testrefs),'prior_primary_pins_preserved':len(priors),'new_code_ranges':0,'new_data_ranges':0,'necessary_reused_raw_ranges':0,'instruction_assertions':0,'new_capture_frames':0,'new_frame_rows_adopted':0,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'Modern_code_edits':0,'tests_builds_compiles_or_runtime_runs':0,'external_helper_imports':0,'unknown_callee_bodies_expanded':0,'server_packet_unchanged':old['verification']['server_packet_unchanged'],'scope_write_correction':old['verification']['scope_write_correction'],'cache_cleanup_completed':False,'non_atomic_current_source_text_only':True}
    root_write('Modern-ready-directory-adoption-contract.json',{'updated_at':stamp(),'batch':33,'qualification':'Finite read-only Modern implementation comparison. Hash-stable within each read, not an atomic R525 revision/build or Native/runtime/GUI acceptance. No proposal promoted to fact.','sources':old['sources'],'existing_evidence':existing,'prior_primary_pins':priors,'volatile_source_reads':catalog,'findings':rows,'minimal_adoption_order':order,'server_version_status':{'accepted_ready_baseline_sha256':'AD368F49CF47E37A76A954AAFAAE1962AB2AB761057AAD4EE3145F7BBFD71219','settings_candidate_sha256':'485F599DA9BA1A13504CB48DDCA3D53C6CAD91D7BAFDF738D104E65A05E1DFD5','settings_unit_and_limited_network_accepted':True,'ready_comparison_485F_active_unaccepted':True,'Modern_R525_active_not_interrupted':True,'adoption_of_new_ready_comparison':False},'outputs':outputs,'verification':v})
    print(json.dumps({'primary':record(ROOT/'Modern-ready-directory-adoption-contract.json'),'verification':v},ensure_ascii=False))
if __name__=='__main__':
    if sys.argv[1:2]==['--finalize']:
        finalize();sys.exit(0)
    if sys.argv[1:2]==['--test-names']:
        for name in sys.argv[2:]:test_names(name)
        sys.exit(0)
    for arg in sys.argv[1:]:
        fields=arg.split(':'); ranges=None
        if len(fields)>1:ranges=[tuple(map(int,s.split('-'))) for s in fields[1].split(',')]
        capture(fields[0],ranges)
