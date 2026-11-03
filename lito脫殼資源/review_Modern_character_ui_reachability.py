"""Batch34 finite review of owned source captures and immutable accepted evidence."""
import sys
sys.dont_write_bytecode=True
from audit import ROOT,record,write,stamp,Path,json

def load(name):return json.loads((ROOT/name).read_text('utf-8'))
def main():
    p=load('Modern-character-ui-reachability-contract.json');checks=[]
    def check(name,value):
        checks.append({'name':name,'match':bool(value)});assert value,name
    for q in p['outputs']:
        check('owned output '+Path(q['path']).name,Path(q['path']).resolve().is_relative_to(ROOT) and record(Path(q['path']))==q)
    catalog=load('Modern-character-ui-reachability/read-provenance-catalog.json')
    for i,r in enumerate(catalog['reads']):
        check('owned read capture '+str(i),record(Path(r['owned_capture']['path']))==r['owned_capture'])
        check('stable before/content/after '+str(i),r['source_before']==r['source_after'] and r['captured_content_sha256']==r['source_before']['sha256'] and r['stable_during_read'] and r['adopted'])
    anchors=0
    for f in p['findings']:
        check('qualified finding '+f['id'],f['status'] in ['already_reachable_source_only','source_proven_gap','needs_GUI_or_design_decision'] and all(f[k] for k in ['trigger','source_proven_behavior','qualification_limit']) and not f['GUI_test_build_execution'] and f['adopted'])
        for a in f['anchors']:
            j=json.loads(Path(a['owned_capture']['path']).read_text('utf-8'));lines={x['line']:x['text'] for x in j['implementation_lines']}
            check(f['id']+' '+a['function']+':'+str(a['start_line']),record(Path(a['owned_capture']['path']))==a['owned_capture'] and a['source']==j['source_before'] and j['stable_during_read'] and a['required_start_token'] in lines[a['start_line']] and a['end_line']>=a['start_line'] and all(i in lines for i in range(a['start_line'],a['end_line']+1)))
            anchors+=1
    matrix=load('Modern-character-ui-reachability/count-slot-route-matrix.json')['rows']
    check('nine count cases',len(matrix)==9)
    for n,r in enumerate(matrix):
        check('full owned count/list '+str(n),r['count']==n and r['owned_rows_preserved']==n and r['complete_after_exact_valid_rows']==n and r['directory_list_source_indices']==list(range(n)) and r['indices4_to7_selectable_via_directory_source']==list(range(4,n)) and r['Directory_visibility_under_other_current_guards']==(n>0) and not r['fixed_four_buttons_visible_in_Directory'] and r['source_proof_only_not_model_execution'])
    native=load('Modern-character-ui-reachability/Native-existing-asset-metadata.json');assets=native['selected_native_assets']
    check('exact four Native images',sorted(x['member']['name'] for x in assets)==['6201.img','6202.img','6303.img','6304.img'])
    for x in assets:
        check('original Native51x23 '+x['member']['name'],x['matches_manifest_pin'] and x['raw_read']['source_before']==x['member']['decoded'] and x['dimensions_from_existing_IMG_header']=={'width':51,'height':23} and not x['new_decode_or_image_created'])
    for x in native['existing_R498_evidence']:
        check('R498 unchanged metadata '+Path(x['sealed_reference']['path']).name,x['matches_sealed_pin'] and x['read']['source_before']==x['sealed_reference'] and x['prior_test_pass_not_replayed'])
    check('79/80 absence only selected exact index',sorted(x['name'] for x in native['selected_index_rows'])==['6201.img','6202.img','6303.img','6304.img'])
    classic=load('Modern-character-ui-reachability/Lineage-existing-selection-asset-metadata.json')
    expected={'Bg_CharacterSelect.png':(1280,960,False,'{{1,1},{1280,960}}'),'Tx_CC_Ok_N.png':(110,38,True,'{{1805,3774},{110,38}}'),'Tx_CC_Ok_C.png':(110,38,True,'{{1765,3774},{110,38}}'),'Tx_CC_Cancel_N.png':(192,35,False,'{{1848,4060},{192,35}}'),'Tx_CC_Cancel_C.png':(192,35,False,'{{36,1220},{192,35}}')}
    check('exact five Classic existing PNGs',len(classic['selected_preexisting_PNG_assets'])==5 and set(x['frame_name'] for x in classic['selected_preexisting_PNG_assets'])==set(expected) and not classic['original_assets_modified_or_created'])
    for x in classic['selected_preexisting_PNG_assets']:
        w,h,rotation,frame=expected[x['frame_name']]
        check('Classic dimensions/crop '+x['frame_name'],x['PNG_IHDR_dimensions']=={'width':w,'height':h} and x['existing_plist_frame_metadata']['rotated']==rotation and x['existing_plist_frame_metadata']['frame']==frame and x['layout_reference_present'] and not x['new_image_or_crop'])
    entry=load('Modern-character-ui-reachability/R525-existing-entry-reference.json')
    check('candidate and normal entry accepted source pins',all(entry[k] for k in ['candidate_matches_existing_entry_reference','production_matches_existing_entry_reference','skin_binding_matches_existing_reference']))
    by_id={int(x['member']['name'].split('.')[0]):x['raw_read']['source_before'] for x in assets}
    check('candidate existing four buttons match raw assets',len(entry['selected_skin_character_buttons'])==4 and all(x['bytes']==by_id[x['id']]['bytes'] and x['sha256']==by_id[x['id']]['sha256'] for x in entry['selected_skin_character_buttons']))
    for q in p['prior_primary_pins']:
        check('immutable prior '+Path(q['path']).name,record(Path(q['path']))['sha256']==q['expected_sha256'] and record(Path(q['path']))['bytes']==q['bytes'])
    for q in p['sources']+list(p['existing_evidence'].values()):
        check('fixed/owned accepted pin '+Path(q['path']).name,record(Path(q['path']))=={k:q[k] for k in ['path','bytes','sha256']})
    v=p['verification']
    check('classification4/1/3',v['status_counts']=={s:sum(x['status']==s for x in p['findings']) for s in v['status_counts']}=={'already_reachable_source_only':4,'source_proven_gap':1,'needs_GUI_or_design_decision':3})
    check('30reads12snapshots10files47anchors35priors',len(catalog['reads'])==30 and v['stable_during_read']==30 and v['tentative_unadopted']==0 and v['source_read_snapshots']==12 and v['unique_source_files']==10 and anchors==47==v['source_anchor_count'] and len(p['prior_primary_pins'])==35==v['prior_primary_pins_preserved'])
    check('three window reads same captured source',catalog['same_window_hash_across_three_slices'])
    check('no invented5..8 functional gap',not v['functional_rows5_to8_gap_found'] and not p['minimal_original_UI_adoption_proposal']['functional_change_needed_for_adapted_rows5_to8'])
    check('R526 active unaffected and nonatomic',v['R526_not_interrupted'] and v['non_atomic_source_snapshot_limit'] and catalog['R526_not_interrupted'])
    check('zero new actions and wire',all(v[k]==0 for k in ['new_art_assets','new_code_ranges','new_data_ranges','new_raw_extractions','instruction_assertions','new_capture_frames','new_frame_rows_adopted','new_wire_differences','new_opcodes','wire_corrections','Modern_code_edits','new_branches_or_tests','tests_builds_compiles_or_GUI_runtime_runs','external_helper_imports','unknown_callee_bodies_expanded']))
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==v['server_packet_unchanged'])
    check('incident retained open',record(ROOT/'CD1-mode-step/scope-write-correction.json')==v['scope_write_correction'] and not v['cache_cleanup_completed'])
    check('no server notification',not p['minimal_original_UI_adoption_proposal']['new_wire_delta']['server_notification'])
    result={'updated_at':stamp(),'primary':record(ROOT/'Modern-character-ui-reachability-contract.json'),'all_match':True,'check_count':len(checks),'anchor_count':anchors,'checks':checks,'scope':'Only owned captures, exact source token/line anchors, descriptive count policies and accepted immutable hashes. External mutable Modern source is not reread/frozen. No function/model/test/build/GUI execution.','runtime_acceptance':False}
    write('Modern-character-ui-reachability/finite-static-review.json',result)
    print(json.dumps({k:result[k] for k in ['primary','all_match','check_count','anchor_count']},ensure_ascii=False))
if __name__=='__main__':main()
