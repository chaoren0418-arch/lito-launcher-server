"""Batch34 finite read-only UI source and existing asset metadata; own output only."""
import sys
sys.dont_write_bytecode=True
import json,hashlib,re,csv,struct,plistlib
from pathlib import Path
from datetime import datetime
ROOT=Path(__file__).resolve().parent
MODERN=ROOT.parent/'lito重建版'/'LiTo381-Modern'/'src'/'modern_lito'
OUT=ROOT/'Modern-character-ui-reachability'
def sha(b):return hashlib.sha256(b).hexdigest().upper()
def pin(p):
    b=Path(p).read_bytes();return {'path':str(Path(p)),'bytes':len(b),'sha256':sha(b)}
def save(name,obj):
    p=OUT/name;assert p.resolve().is_relative_to(ROOT);OUT.mkdir(exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(p):
    p=Path(p);before=pin(p);b=p.read_bytes();after=pin(p)
    return b,{'source_before':before,'captured_content_sha256':sha(b),'source_after':after,'stable_during_read':before==after and before['sha256']==sha(b),'read_at':datetime.now().astimezone().isoformat(),'adoption':'source/metadata only; non-atomic; no GUI/test/build'}
def capture(name,ranges):
    source_name=name.split('@')[0]
    p=MODERN/source_name
    if source_name=='R525-candidate-entry':p=MODERN.parent.parent/'target/r525-player-entry-candidate/LiToModern-R525.rs'
    if source_name=='production-main':p=MODERN.parent/'main.rs'
    b,m=read(p);ls=b.decode('utf-8-sig').splitlines()
    rows=[{'line':i,'text':ls[i-1]} for a,z in ranges for i in range(a,min(z,len(ls))+1)]
    save(name+'.snapshot.json',{**m,'ranges':ranges,'line_count':len(ls),'implementation_lines':rows})
    print(json.dumps({'name':name,**m,'lines':rows},ensure_ascii=False))
def assets():
    b,m=read(ROOT/'resources/archive-manifest.json');man=json.loads(b)
    wanted={'6303.img','6304.img','6201.img','6202.img','79.img','80.img'}
    members=[q for q in man['selected_members'] if q['archive']=='LTsurf' and q['name'] in wanted]
    native=[]
    for q in members:
        raw=q['decoded'];data,r=read(raw['path']);header=list(struct.unpack('<4H',data[:8]))
        native.append({'member':q,'raw_read':r,'matches_manifest_pin':r['source_before']==raw,'header_words':header,'dimensions_from_existing_IMG_header':{'width':header[0],'height':header[1]},'new_decode_or_image_created':False})
    index,im=read(ROOT/'resources/indices/LTsurf.csv');rows=list(csv.DictReader(index.decode('utf-8-sig').splitlines()));selected=[q for q in rows if q['name'] in wanted]
    reports=[]
    for q in man['selection_sources']:
        data,rm=read(q['path']);j=json.loads(data)
        keys=['slots','animation_tables','missing_members','variant_rule','draw_chain','animation_lifecycle_open','GUI'] if 'resource-mapping' in q['path'] else ['character_buttons','GUI']
        reports.append({'sealed_reference':q,'read':rm,'matches_sealed_pin':rm['source_before']==q,'selected_existing_metadata':{k:j[k] for k in keys if k in j},'prior_test_pass_not_replayed':True})
    save('Native-existing-asset-metadata.json',{'archive_manifest_read':m,'LTsurf_index_read':im,'selected_index_rows':selected,'selected_native_assets':native,'existing_R498_evidence':reports,'qualification':'Existing sealed metadata and exact four already-extracted button IMG headers only.79/80 lookup absence limited to this exact LTsurf index/selected members; no claim absent throughout all assets. Historical ASM/navigation/layout report does not add new fixed-PE or GUI evidence.'})
    base=Path(r'D:\lineage開發\介面UI\經典版\經典服UI_全部拆完+解完的座標')
    files=['CharacterSelectScene.json','CharacterSelect/CharacterSelectLayout.json']
    layouts=[];frames=set()
    wanted_nodes={'Scene','CharacterSelectLayout','Background','ListView','Bt_Ok','Bt_Cancel','CharacterName','Level','HitPoint','ManaPoint','AmorClass','Class','Bloodpldge'}
    for name in files:
        data,lm=read(base/'csb_json'/name);j=json.loads(data);todo=[j['root']];nodes=[]
        while todo:
            n=todo.pop(0);w=n.get('widget',{});todo+=n.get('children',[])
            if w.get('name') in wanted_nodes:
                resources=[r for r in n.get('resources',[]) if r.get('path')]
                nodes.append({'json_node_path':n.get('path'),'name':w.get('name'),'classname':n.get('classname'),'position':w.get('position'),'size':w.get('size'),'anchor':w.get('anchor'),'scale':w.get('scale'),'visible':w.get('visible'),'resources':resources,'file_name':n.get('file_name')})
                frames.update(r['path'] for r in resources if r.get('plist')=='UI_Resource/OutGames.plist')
        layouts.append({'read':lm,'original_metadata_source_string_not_followed':j.get('source'),'relative_path':j.get('relative_path'),'nodes':nodes})
    # Only pre-existing selection background and confirmation/cancel frames are needed.
    selected_frames=['Bg_CharacterSelect.png','Tx_CC_Ok_N.png','Tx_CC_Ok_C.png','Tx_CC_Cancel_N.png','Tx_CC_Cancel_C.png']
    plist_path=Path(r'D:\lineage開發\介面UI\經典版\0429經典服UI\UI_Resource\OutGames.plist')
    data,pm=read(plist_path);plist=plistlib.loads(data)
    pngs=[]
    for name in selected_frames:
        assert name in frames
        p=base/'UI_Resource'/'Outgames'/name;data,am=read(p)
        assert data[:8]==b'\x89PNG\r\n\x1a\n' and data[12:16]==b'IHDR'
        w,h=struct.unpack('>2I',data[16:24]);frame=plist['frames'].get(name)
        pngs.append({'frame_name':name,'read':am,'PNG_IHDR_dimensions':{'width':w,'height':h},'existing_plist_frame_metadata':frame,'layout_reference_present':True,'new_image_or_crop':False})
    save('Lineage-existing-selection-asset-metadata.json',{'family':'Existing0429classicLineage assets/design exports, separate from Native LiTo LTsurf/630DF8 evidence. Not currently loaded by inspected Modern selection window.','layout_reads':layouts,'atlas_plist_read':pm,'selected_preexisting_PNG_assets':pngs,'coordinate_semantics':'Existing CSB JSON local position/anchor/scale and atlas crop metadata only. No inferred Modern screen transform, native hit bounds or executable callback. Exported source strings outside this workspace are historical metadata and were not followed.','original_assets_modified_or_created':False})
    print(json.dumps({'Native_button_assets':len(native),'Native_dimensions':[x['dimensions_from_existing_IMG_header'] for x in native],'index_selected_rows':selected,'existing_reports_pinned':[x['matches_sealed_pin'] for x in reports],'Lineage_PNG_assets':[{'name':x['frame_name'],'pin':x['read']['source_before'],'dimensions':x['PNG_IHDR_dimensions'],'frame':x['existing_plist_frame_metadata']} for x in pngs],'layouts':[x['read']['source_before'] for x in layouts]},ensure_ascii=False))
def entry_metadata():
    p=MODERN.parent.parent/'logs/continuation-20261003/r525-entry-source-contract.json'
    b,m=read(p);j=json.loads(b);b,sm=read(j['skin_binding']['path']);skin=json.loads(b)
    candidate=json.loads((OUT/'R525-candidate-entry.snapshot.json').read_text('utf-8'))
    production=json.loads((OUT/'production-main.snapshot.json').read_text('utf-8'))
    obj={'entry_contract_read':m,'selected_reference_pins':{k:j[k] for k in ['original_main','candidate_main','skin_binding']},'candidate_matches_existing_entry_reference':candidate['source_before']==j['candidate_main'],'production_matches_existing_entry_reference':production['source_before']==j['original_main'],'skin_binding_read':sm,'skin_binding_matches_existing_reference':sm['source_before']==j['skin_binding'],'selected_skin_character_buttons':skin.get('character_buttons'),'scope':'Existing source entry/binding metadata only. No executable/library read or build/GUI/runtime acceptance; R526 active is not inspected.'}
    save('R525-existing-entry-reference.json',obj);print(json.dumps(obj,ensure_ascii=False))
def finalize():
    from audit import record,write as root_write,stamp
    previous=json.loads((ROOT/'Modern-ready-directory-adoption-contract.json').read_text('utf-8'))
    snapshots={p.name:json.loads(p.read_text('utf-8')) for p in OUT.glob('*.snapshot.json')}
    # Preserve only the relevant source lines from the initial compressed window read.
    window_path=OUT/'native_city_window.rs.snapshot.json';window=snapshots[window_path.name]
    window['implementation_lines']=[r for r in window['implementation_lines'] if not 74<=r['line']<=93]
    window['ranges']=[[1,18],[25,37],[47,55],[63,73],[112,130],[138,138],[145,163]]
    save(window_path.name,window)
    native=json.loads((OUT/'Native-existing-asset-metadata.json').read_text('utf-8'))
    for r in native['existing_R498_evidence']:
        detail=r['selected_existing_metadata']
        if 'animation_tables' in detail:
            detail['animation_table_reference_summary']=[{k:t[k] for k in ['class_byte','variant_nonzero','table_va']} for t in detail.pop('animation_tables')]
        if 'missing_members' in detail:
            missing=detail.pop('missing_members');detail['existing_missing_lookup']={'79.img':'79.img' in missing,'80.img':'80.img' in missing,'scope':'Only the unchanged historical R498 report lookup; not whole asset corpus absence.'}
    save('Native-existing-asset-metadata.json',native)
    lineage=json.loads((OUT/'Lineage-existing-selection-asset-metadata.json').read_text('utf-8'))
    entry=json.loads((OUT/'R525-existing-entry-reference.json').read_text('utf-8'))
    reads=[]
    def note(metadata,owned,kind):reads.append({'owned_capture':record(owned),'kind':kind,**metadata,'adopted':metadata['stable_during_read']})
    for p in sorted(OUT.glob('*.snapshot.json')):note(json.loads(p.read_text('utf-8')),p,'source')
    np=OUT/'Native-existing-asset-metadata.json'
    for m in [native['archive_manifest_read'],native['LTsurf_index_read']]+[r['raw_read'] for r in native['selected_native_assets']]+[r['read'] for r in native['existing_R498_evidence']]:note(m,np,'existing Native metadata/asset header')
    lp=OUT/'Lineage-existing-selection-asset-metadata.json'
    for m in [r['read'] for r in lineage['layout_reads']]+[lineage['atlas_plist_read']]+[r['read'] for r in lineage['selected_preexisting_PNG_assets']]:note(m,lp,'existing Lineage design/asset metadata')
    ep=OUT/'R525-existing-entry-reference.json'
    for m in [entry['entry_contract_read'],entry['skin_binding_read']]:note(m,ep,'existing candidate source entry/skin metadata')
    # Catalog does not duplicate captured code: each owned capture retains its exact lines.
    reads=[{k:v for k,v in r.items() if k not in ['implementation_lines','ranges','line_count']} for r in reads]
    same_window_sha=len({j['source_before']['sha256'] for k,j in snapshots.items() if k.startswith('native_city_window.rs')})==1
    save('read-provenance-catalog.json',{'reads':reads,'same_window_hash_across_three_slices':same_window_sha,'source_consistency':'Each read before/content/after hash; not an atomic R526 build revision. Presentation source already contains a new ready field while earlier Runtime capture is its earlier read-time source. No cross-file compile compatibility or runtime GUI acceptance inferred, no freeze/recapture-for-pass requested.','later_external_edits':'Owned immutable snapshots are handoff dependencies; external mutable source need not stay frozen.','R526_not_interrupted':True})
    def a(name,fn,line,end,token):
        p=OUT/(name+'.snapshot.json');j=json.loads(p.read_text('utf-8'));row=next(r for r in j['implementation_lines'] if r['line']==line);assert token in row['text']
        return {'source':j['source_before'],'owned_capture':record(p),'function':fn,'start_line':line,'end_line':end,'required_start_token':token,'adopted':j['stable_during_read']}
    findings=[]
    def f(id,status,concern,anchors,trigger,behavior,limit):
        findings.append({'id':id,'status':status,'concern':concern,'anchors':anchors,'trigger':trigger,'source_proven_behavior':behavior,'qualification_limit':limit,'GUI_test_build_execution':False,'adopted':all(x['adopted'] for x in anchors)})
    f('U01','already_reachable_source_only','Explicit candidate adapter entry is wired, not merely an unused window name',[
        a('R525-candidate-entry','route',37,37,'--candidate-java-bootstrap'),a('R525-candidate-entry','run',94,94,'candidate_java_entry'),a('R525-candidate-entry','candidate_java_entry',193,193,'run_synthetic_java_bootstrap'),a('native_city_window.rs','run_synthetic_java_bootstrap',146,147,'run_inner'),a('native_city_window.rs','run_inner adapter configuration',157,157,'configure_login_producer_fixture(adapter)')],
        'Explicit candidate flag with valid absolute fixture/adapter files and owned resource bindings, then valid GAME S03/exactN CB continuation.',
        'R525 candidate route calls candidate_java_entry -> run_synthetic_java_bootstrap -> run_inner with adapter; Runtime adapter configuration precedes the owned window loop.',
        'Source call-chain only; R525 source entry matches existing pinned entry reference. No candidate/library/GUI execution; current R526 active source does not become an accepted compiled version.')
    f('U02','already_reachable_source_only','Adapted directory displays and controls every validated row including indices4..7',[
        a('native_source_identity.rs','JAVA_ROW_BOUND',8,8,'JAVA_ROW_BOUND:usize=8'),a('native_city_runtime.rs','consume_source_identity_event',148,149,'Stage::Directory'),a('native_city_window.rs','State directory controls',14,14,'directory_controls:RefCell<Vec'),a('native_city_window.rs','refresh roster eligibility',35,35,'source_identity().complete()'),a('native_city_window.rs','refresh directory',47,54,'Stage::Directory'),a('native_city_window.rs','refresh controls',51,52,'0..rows.len()'),a('native_city_window.rs','refresh visibility',63,63,'CHARACTER_DIRECTORY|CHARACTER_DETAILS=>entry&&directory'),a('native_city_window.rs','run_inner control creation',155,156,'i==CHARACTER_DIRECTORY')],
        'Complete current adapted directory count1..8 in Entry/Directory with current selectable rows and no pending navigation/write.',
        'CHARACTER_DIRECTORY is a scrollable/tab-stop CBS_DROPDOWNLIST (ID FIRST+36=8036); it inserts every row display name, builds controls over0..rows.len(), shows selected raw row detail. There is no four-row truncation in this list path.',
        'Valid count0 remains complete empty with no selectable row; empty/missing/pending/stale are deliberately unavailable. OS creation/string insertion return behavior, clipping or actual visible8 rows are not tested.')
    f('U03','already_reachable_source_only','Dropdown selection -> shared choose -> explicit submit -> exact received name',[
        a('native_city_window.rs','proc CBN_SELCHANGE directory',130,130,'CB_GETCURSEL'),a('native_player_entry_commands.rs','handle_player_entry_command ChooseCharacter',58,62,'Command::ChooseCharacter'),a('native_city_runtime.rs','source_character_control_enabled',171,173,'source_identity::JAVA_ROW_BOUND'),a('native_city_runtime.rs','choose_source_character_control',176,180,'choose_source_character_control'),a('native_character_selection.rs','Scene::select',18,18,'self.key.selected=Some(slot)'),a('native_city_window.rs','refresh selected details',54,54,'rows.get(slot)'),a('native_city_window.rs','proc SubmitCharacter',127,127,'EntryCommand::SubmitCharacter'),a('native_city_runtime.rs','source_character_submit',182,183,'selection_revision'),a('native_city_runtime.rs','prepare_source_send_candidate',214,218,'prepare_select_bounded'),a('native_source_send.rs','Sender::prepare_character',30,30,'body.extend_from_slice(name)')],
        'Directory CBN_SELCHANGE chooses absolute row index4..7, refresh publishes selected details/submit token, then user clicks Enter Character.',
        'CB_GETCURSEL -> checked usize -> directory_controls.get -> shared ChooseCharacter(double_click:false) -> epoch/revision/bounded8 checks -> Scene::select(any qualified usize). Submit revalidates selection+scene+roster+epoch and uses bounded row raw name in C9. No four-anchor lookup on this chain.',
        'This proves conditional source reachability/reservation, not physical send/server acceptance or GUI pass; zero/missing rows never default to row0; no ObjectId/sourceId derived from index/name.')
    f('U04','already_reachable_source_only','Four direct buttons and original Enter/Back assets have separate scopes',[
        a('native_city_runtime.rs','source_character_command_slot',46,46,'SOURCE_CHARACTER_COMMAND_FIRST+4'),a('native_character_selection.rs','slot',10,10,'index<4'),a('native_city_window.rs','refresh fixed controls',34,35,'std::array::from_fn'),a('native_city_window.rs','refresh fixed button visibility',63,64,'CHARACTER_FIRST..=16=>entry&&roster&&!directory'),a('native_city_window.rs','proc direct button commands',126,126,'SOURCE_CHARACTER_COMMAND_FIRST)+4'),a('native_city_window.rs','draw_character_button',115,117,'skin.character_buttons.get'),a('native_mode_menu_skin.rs','Skin::load character_buttons',36,36,'[6303,6304,6201,6202]')],
        'Legacy non-Directory four buttons versus adapted Directory list; Enter/Back owner draw with valid original skin binding.',
        'Command0x4960..4963/slots0..3 and four anchor metadata only limit the direct-button path, hidden during adapted Directory. Candidate skin binding includes original LTsurf6303/6304 Enter and6201/6202 Back, all51x23; owner draw selects normal/pressed pair and falls back to text when art unavailable.',
        'Does not prove Native record storage capacity4 or8. Current Modern four buttons use a2x2 layout, not Native four horizontal anchors. Conditional source art path is not a rendered GUI screenshot.')
    f('G01','source_proven_gap','Original portrait/animation/selection backdrop parity is not implemented in inspected render closure',[
        a('native_city_window.rs','run_inner child styles',155,156,'CBS_DROPDOWNLIST'),a('native_city_window.rs@render','proc WM_DRAWITEM',123,123,'FIRST+CHARACTER_SUBMIT'),a('native_city_window.rs@render','paint',111,111,'paint_menu(hwnd,s,dc);paint_channel'),a('native_city_window.rs@render','paint_channel',107,107,'NavigationScene::Channel'),a('native_city_window.rs@render','paint_source_entry',109,109,'biWidth:11,biHeight:-11'),a('native_mode_menu_skin.rs','Skin fields',30,30,'character_buttons:Vec<Img>')],
        'Entry/Directory wants original character portraits, selected/hover animation and selection scene background.',
        'Inspected parent paint fills system background; menu/frame paint is Maps-only, channel art is Channel-only, source entry art is an11x11 icon. Child Directory is standard text combo/details; four character buttons are standard text BS_NOTIFY. WM_DRAWITEM routes Enter/Back/channel buttons only. Skin has four button images, not loaded character portrait/selection backdrop fields.',
        'Exact scoped source render closure gap, not a5..8 functional selection defect or global GUI absence claim. Existing R498 reported unresolved79/80/animation assets and lifecycle remain metadata; no unknown Native callee opened and no new art generated.')
    f('D01','needs_GUI_or_design_decision','Default production entry adoption is separate from candidate source reachability',[
        a('production-main','route default',31,31,'Route::Pick'),a('production-main','route allowed flags/fallback',67,78,'Some(x)'),a('production-main','run profile entry',133,145,'Route::Pick =>'),a('R525-candidate-entry','route explicit candidate',37,37,'--candidate-java-bootstrap')],
        'User launches default production executable with no explicit candidate flag versus candidate adapter source.',
        'src/main route lacks CandidateJavaBootstrap and its default/profile branch calls login_window::run with SessionController. Native adapted selection chain is exposed by explicit R525 candidate entry, not promoted into this main source.',
        'Do not infer deployed executable or alternative TLS character UI is broken. Whether/when to expose this Native adapter in normal player entry is a separate authorized product/adoption decision after R526.')
    f('D02','needs_GUI_or_design_decision','Real GUI geometry/keyboard/render acceptance and adapted interaction wording',[
        a('native_city_window.rs','resize',71,71,'CHARACTER_DIRECTORY'),a('native_city_window.rs','run_inner message loop',159,159,'IsDialogMessageW'),a('native_city_runtime.rs','choose_source_character_control status',179,180,'可按進入角色或雙擊'),a('native_city_window.rs','proc directory choice',130,130,'double_click:false')],
        'Real DPI/window size, dropdown opening, keyboard focus, eight names and details clipping; double-click hint in adapted Directory.',
        'Source sizes dropdown400x280 at DPI-scaled margin24,y120, details y164;height90; submit/back use Native constants plus Modern vertical232 offset. Tab-stop/message loop exists. Adapted choice explicitly uses double_click:false; only direct fixed buttons have explicit BN_DOUBLECLICKED submission path.',
        'Actual native control behavior/visibility/accessibility/doubleclick expectations are untested. The shared hint should be confirmed or tailored if restyling; explicit choose+submit already supplies all8 rows. No new tests authored or executed.')
    f('D03','needs_GUI_or_design_decision','Choose one existing original UI asset family and mapping; preserve all8 data',[
        a('native_character_selection.rs','SUBMIT and BACK',7,10,'pub const SUBMIT'),a('native_mode_menu_skin.rs','Skin::load Native asset family',34,36,'LTsurf_index'),a('native_city_window.rs','resize original button geometry use',71,71,'character_selection::SUBMIT')],
        'Future original-look presentation reuses Native LiTo assets or existing0429Classic Lineage selection exports.',
        'Four Native51x23 button files are already bound. Five verified existing Classic PNGs provide1280x960 selection backdrop,110x38 OK normal/pressed and192x35 Cancel normal/pressed; existing CSB JSON supplies local layout/ListView/button geometry and OutGames.plist supplies crops/rotation.',
        'Classic family is a separate version/design corpus, not proof Native LiTo79/80 recovered. No class/sex portrait mapping from filenames, no mixed coordinate systems, no original hit/owner/physical identity inferred. Scaling/glyphs/portrait mapping need design and eventual GUI acceptance.')
    status_counts={s:sum(r['status']==s for r in findings) for s in ['already_reachable_source_only','source_proven_gap','needs_GUI_or_design_decision']}
    save('character-flow-findings.json',{'qualification':'Source-level conditional reachability, never GUI/build/runtime PASS. No5..8 row selection routing gap is found in the explicit adapted candidate path.','findings':findings,'status_counts':status_counts})
    matrix=[]
    for n in range(9):
        matrix.append({'count':n,'owned_rows_preserved':n,'complete_after_exact_valid_rows':n,'directory_list_source_indices':list(range(n)),'Directory_visibility_under_other_current_guards':n>0,'fixed_four_buttons_visible_in_Directory':False,'indices4_to7_selectable_via_directory_source':list(range(4,n)),'source_proof_only_not_model_execution':True})
    save('count-slot-route-matrix.json',{'scope':'Nine descriptive source-policy cases0..8, conditional on valid exactN Java-projected rows/current epoch/seed/dormant navigation and Directory stage. No Modern function execution, no test PASS and no Native capacity inference. Count0 has no row/selection.','rows':matrix})
    chains=[{'name':'Explicit adapter entry and directory publication','ordered_edges':['R525 route37 -> run94 -> candidate_java_entry193','window146 -> run_inner157 configures adapter; normal owned GAME continuation','Runtime148/149 -> Stage Directory + exactNcomplete -> character_scene.enter','window refresh35/47 -> rows50 -> full controls51/full names52 -> details54/visible63','CreateWindow155/156 -> COMBOBOX FIRST+36 with WS_VSCROLL/CBS_DROPDOWNLIST']},
        {'name':'Absolute dropdown selection through typed raw-name reservation','ordered_edges':['window proc130 CB_GETCURSEL -> checked usize -> directory_controls.get','player_entry_commands58..62 -> Runtime.choose_source_character_control176..180','Runtime171..173 bounded8 + epoch/roster/scene -> Scene.select18; no slot(index) lookup','refresh33/54/63 -> submit token/details/enabled; proc127 -> SubmitCharacter','Runtime182/183 -> handle_source_character_control173 -> prepare214/215/218','Sender30 -> bodyC9 + exact raw received name + NUL; only local reservation qualification']},
        {'name':'Scoped render closure and original button reuse','ordered_edges':['window WM_PAINT138 -> paint111 -> system background + Maps/menu/frame + Channel background + source11x11 icon','WM_DRAWITEM123 -> draw_character_button115..117 -> original Enter/Back pairs or text fallback','Directory/Details use standard COMBOBOX/STATIC; direct four character BUTTON controls use BS_NOTIFY, not portrait owner draw','Skin30/34/36 binds LTsurf index/pak and four character action buttons only; no decoded Classic selection backdrop or portrait fields'] }]
    save('finite-call-chains.json',{'chains':chains,'anchors_in':'character-flow-findings.json','stop_boundaries':'No Windows APIs executed, socket/server acceptance or original private constructor/virtual renderer bodies expanded; all edges are existing source text. Default main and R525 candidate source are separate.'})
    plan={'functional_change_needed_for_adapted_rows5_to8':False,'default_recommendation':'Keep current full0..8 owned directory + checked absolute index controls + exact raw-name C9 pipeline. Keep existing dropdown/details as complete fallback and existing original Native Enter/Back button rendering. Do not create a new eight-row protocol, duplicate selection guards, lower producer count, force four rows or repurpose native record index as identity.',
        'smallest_original_UI_followup_if_requested':['Use already-bound LTsurf6303/6304 and6201/6202 original51x23 action assets; preserve their declared button identity and current opaque Submit/Back controls. Retain fullN names/details and no automatic row0 for count0.','If a Classic Lineage look is chosen, reuse the five verified pre-extracted PNGs and existing1280x960 CSB layout/ListView metadata as one asset family. Render allN<=8 text rows or keep dropdown fallback, with absolute owned indices; scroll/clipping only affects presentation, never data count.','Bind existing ChooseCharacter/SubmitCharacter command path to presentation selection; no new backend roster/handle implementation. Preserve raw bytes for commands and use decoded display text only for labels.','Leave exact Native portrait/animation parity open until matching qualified assets/lifecycle exist; do not synthesize replacements or assume Classic portraits map to Native class/sex.','Treat DPI/coordinate conversion, glyph/keyboard accessibility, visual layering and default player-entry promotion as later design/GUI acceptance after activeR526; no new task/branch/test is started here.'],
        'existing_geometry_not_interchangeable':{'Native_action_rects':'SUBMIT(497,404,51,23),BACK(552,404,51,23) in current source; Modern resize adds vertical232 and applies DPI scale. Native historical slot anchorsx19/176/333/490,y0 belong only to four local control descriptors, not storage capacity.','Classic_scene':'Existing CSB export1280x960, CharacterSelectLayout center640,480; ListView local(-545,-55),1100x534; OK local(478,-339),110x38; Cancel local(478,-374),192x35. Local anchor/scale preserved in metadata; no inferred modern hit coordinates.'},
        'supervisor_status':'M09/M10 already delegated as R526; do not interrupt or expand it. Supervisor says M13 handled by later corrected ready handoff SHA3F31E26FF006B132C4531BA53811597D3D8EF8581531AD04E8599B0C2B170947. This is a message-time note only; that report is not read/promoted in Batch34 and Batch33 stays immutable.',
        'new_wire_delta':{'count':0,'direction_opcode_field_trigger_VA_evidence_corrections':[],'server_notification':False},'scope':'Proposal only, no new art/Modern edit/GUI/test/build/branch. Existing complete route is reported honestly, not a invented5..8 gap.'}
    save('minimal-original-UI-adoption-proposal.json',plan)
    priors=previous['prior_primary_pins']+[dict(record(ROOT/'Modern-ready-directory-adoption-contract.json'),expected_sha256='B746EF77645DD5BE45B005C6C1F808B0DB83963D36F019CB8368548EC088A88E',match=True)]
    for p in priors:assert record(p['path'])['sha256']==p['expected_sha256']
    existing={'Batch33':record(ROOT/'Modern-ready-directory-adoption-contract.json'),'Batch32':record(ROOT/'CD2-directory-index-storage-contract.json'),'Batch31':record(ROOT/'CD2-selected-record-source-contract.json'),'Batch30':record(ROOT/'CD2-ready-guard-source-contract.json'),'owned_archive_manifest':native['archive_manifest_read']['source_before'],'owned_LTsurf_index':native['LTsurf_index_read']['source_before'],'scope_write_correction':previous['verification']['scope_write_correction']}
    v={'source_read_snapshots':len(snapshots),'unique_source_files':len({j['source_before']['path'] for j in snapshots.values()}),'all_read_pins':len(reads),'stable_during_read':sum(r['stable_during_read'] for r in reads),'tentative_unadopted':sum(not r['stable_during_read'] for r in reads),'findings':len(findings),'status_counts':status_counts,'static_count_policy_rows':9,'finite_call_chains':len(chains),'source_anchor_count':sum(len(r['anchors']) for r in findings),'Native_existing_button_assets':4,'Lineage_existing_PNG_assets':5,'new_art_assets':0,'new_code_ranges':0,'new_data_ranges':0,'new_raw_extractions':0,'instruction_assertions':0,'new_capture_frames':0,'new_frame_rows_adopted':0,'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'Modern_code_edits':0,'new_branches_or_tests':0,'tests_builds_compiles_or_GUI_runtime_runs':0,'external_helper_imports':0,'unknown_callee_bodies_expanded':0,'prior_primary_pins_preserved':len(priors),'server_packet_unchanged':previous['verification']['server_packet_unchanged'],'scope_write_correction':previous['verification']['scope_write_correction'],'cache_cleanup_completed':False,'functional_rows5_to8_gap_found':False,'R526_not_interrupted':True,'non_atomic_source_snapshot_limit':True}
    outputs=[record(p) for p in sorted(OUT.iterdir()) if p.is_file()]
    root_write('Modern-character-ui-reachability-contract.json',{'updated_at':stamp(),'batch':34,'qualification':'Finite source-level candidate UI reachability and existing asset metadata only. All reads individually pinned; cross-file R526 compatibility unverified, no GUI/Native/build/runtime acceptance. Original art parity and default main entry adoption are separate from eight-row routing.','sources':previous['sources'],'prior_primary_pins':priors,'existing_evidence':existing,'read_catalog':record(OUT/'read-provenance-catalog.json'),'findings':findings,'call_chains':chains,'minimal_original_UI_adoption_proposal':plan,'outputs':outputs,'verification':v})
    print(json.dumps({'primary':record(ROOT/'Modern-character-ui-reachability-contract.json'),'verification':v},ensure_ascii=False))
if __name__=='__main__':
    if sys.argv[1:2]==['--finalize']:
        finalize();sys.exit(0)
    if sys.argv[1:2]==['--entry-metadata']:
        entry_metadata();sys.exit(0)
    if sys.argv[1:2]==['--assets']:
        assets();sys.exit(0)
    for arg in sys.argv[1:]:
        name,s=arg.split(':');capture(name,[tuple(map(int,x.split('-'))) for x in s.split(',')])
