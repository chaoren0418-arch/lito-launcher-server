"""Finite owned evidence consistency review, no Modern function/build/GUI execution."""
import sys
sys.dont_write_bytecode=True
import hashlib,struct
from audit import ROOT,record,write,stamp,Path,json
def load(name):return json.loads((ROOT/name).read_text('utf-8'))
def main():
    p=load('Native-selection-resource-gap-contract.json');checks=[]
    def check(name,value):checks.append({'name':name,'match':bool(value)});assert value,name
    for q in p['outputs']:check('own output '+Path(q['path']).name,Path(q['path']).resolve().is_relative_to(ROOT) and record(q['path'])==q)
    old=load('Modern-character-ui-reachability-contract.json')
    check('Batch34 immutable primary',record(ROOT/'Modern-character-ui-reachability-contract.json')==p['Batch34_historical_primary'] and p['Batch34_historical_primary']['sha256']=='FC83ED02485F28661AC5550A8C6D8DB0F4ABA3EE06FB6B68EB26ED38CE4BDC9B')
    for q in old['outputs']:check('Batch34 original preserved '+Path(q['path']).name,record(q['path'])==q)
    cat=load('Native-selection-resource-gap/retained-read-provenance.json')
    for n,r in enumerate(cat['reads']):
        check('owned read record '+str(n),record(r['owned_capture']['path'])==r['owned_capture'])
        check('before/content/after stable '+str(n),r['before']==r['after'] and r['content_sha256']==r['before']['sha256'] and r['stable'])
    c=load('Native-selection-resource-gap/R526-selection-compatibility-addendum.json')
    check('only two selected sourcefiles',sorted(x['name'] for x in c['reads'])==['native_city_runtime.rs','native_source_send.rs'])
    check('R526 manifest correct',c['R526_frozen_manifest_read']['before']['sha256']=='514D978CAABECF04F20BDB03F941274E90274803FBEDFD32026E1C529C6FC82C')
    for r in c['reads']:
        ref=next(q for q in c['selected_source_references'] if Path(q['path']).name==r['name'])
        check('frozen pin '+r['name'],r['frozen_read']['before']['bytes']==ref['bytes'] and r['frozen_read']['before']['sha256']==ref['sha256'])
        check('live matches frozen at read '+r['name'],r['live_matches_frozen'] and r['live_read']['before']['sha256']==ref['sha256'])
    for i,a in enumerate(c['anchor_comparisons']):
        oldsnap=json.loads(Path(a['historical_anchor']['owned_capture']['path']).read_text('utf-8'));ol={x['line']:x['text'] for x in oldsnap['implementation_lines']}
        name=Path(a['frozen_source_pin']['path']).name;newsnap=load('Native-selection-resource-gap/'+name+'.frozen-slices.json');nl={x['line']:x['text'] for x in newsnap['lines']}
        check('selected anchored span '+str(i),record(a['historical_anchor']['owned_capture']['path'])==a['historical_anchor']['owned_capture'] and a['old_lines']==[ol[k] for k in range(a['historical_anchor']['start_line'],a['historical_anchor']['end_line']+1)] and a['old_lines']==a['frozen_lines']==[nl[k] for k in range(a['frozen_start_line'],a['frozen_end_line']+1)] and a['required_start_token'] in a['frozen_lines'][0] and a['start_text_unchanged'] and a['full_anchored_span_unchanged'])
    check('eight anchors all unchanged',len(c['anchor_comparisons'])==8 and c['all_selected_spans_unchanged'])
    data=load('Native-selection-resource-gap/Native-animation-and-slot-data.json')
    for t in data['animation_tables']:
        packed=b''.join(struct.pack('<4I2i',*(s[k] for k in ['base_png','frame_limit','delay','draw_mode','dx','dy'])) for s in t['states'])
        check('12 numeric table value hash '+t['table_va'],len(packed)==96 and hashlib.sha256(packed).hexdigest().upper()==t['raw']['sha256'] and t['raw_read']['before']==t['raw'] and t['fixed_PE_origin']['bytes']==96 and t['matches_fixed_PE'] and t['decoded_states_match_metadata'])
    d=data['descriptor_table'];packed=b''.join(struct.pack('<15I',*r) for r in d['rows'])
    check('descriptor rows raw fingerprint',len(packed)==540 and hashlib.sha256(packed).hexdigest().upper()==d['raw']['sha256'] and d['read']['before']==d['raw'] and d['matches_fixed_PE'])
    check('four79descriptor anchors only',[(x['image_id'],x['x'],x['y'],x['receiver']) for x in data['slots']]==[(79,19,0,0),(79,176,0,1),(79,333,0,2),(79,490,0,3)])
    check('selector numeric metadata no sex guess',len(data['animation_tables'])==12 and [(x['class_byte'],x['variant_nonzero']) for x in data['animation_tables']]==[(-1,0),(0,0),(0,1),(1,0),(1,1),(2,0),(2,1),(3,0),(3,1),(4,0),(4,1),(5,0)])
    envelope=set()
    for t in data['animation_tables']:
        for s in t['states']:envelope.update(range(s['base_png'],s['base_png']+s['frame_limit']+1))
    check('conservative lookup envelope only',data['numeric_png_lookup_envelope']==sorted(envelope)==list(range(803)))
    inv=load('Native-selection-resource-gap/Native-existing-resource-inventory.json');offset=load('Native-selection-resource-gap/Native-source-container-offset-check.json')
    check('five Native index boundaries',[(x['archive'],x['rows_in_complete_existing_index']) for x in inv['native_index_lookups']]==[('LTicon',227),('LTtext',353),('LTsurf',1623),('LTsprite',469),('LTtile',1210)])
    check('no targetedPNG79/80 index hits',all(x['name'].lower() in {'6314.img','7482.img','6303.img','6304.img','6201.img','6202.img'} for ix in inv['native_index_lookups'] for x in ix['target_rows']) and sum(len(ix['target_rows']) for ix in inv['native_index_lookups'])==6)
    check('exact loose Native LTSurf scope',inv['loose_native_LTSurf_exact_lookup']['before_names']==inv['loose_native_LTSurf_exact_lookup']['after_names']==['8044.img'] and not inv['loose_native_LTSurf_exact_lookup']['target_existing_names'])
    for a in inv['existing_assets']:
        q=a['member'];o=next(x for x in offset['checks'] if x['name']==q['name']);ix=next(x for x in inv['native_index_lookups'] if x['archive']=='LTsurf');row=next(x for x in ix['target_rows'] if x['name']==q['name'])
        check('asset dimension/source offset '+q['name'],a['existing_raw_read']['before']==q['decoded'] and record(q['decoded']['path'])==q['decoded'] and a['dimensions']=={'width':a['IMG_header_words'][0],'height':a['IMG_header_words'][1]} and o['raw_sha256']==q['raw']['sha256'] and (o['row'],o['PAK_offset'],o['bytes'])==(q['row'],q['offset'],q['bytes'])==(int(row['row']),int(row['offset']),int(row['bytes'])) and o['matches_original_index_and_source_PAK'] and a['no_new_decode'])
        check('existing preview immutable '+q['name'],record(q['preview']['output']['path'])==q['preview']['output']==a['existing_preview_read']['before'] and a['dimensions']=={'width':q['preview']['width'],'height':q['preview']['height']})
    check('fourbutton/twobackground exactdimensions',len(inv['existing_assets'])==6 and sum(a['dimensions']=={'width':51,'height':23} for a in inv['existing_assets'])==4 and sum(a['dimensions']=={'width':640,'height':480} for a in inv['existing_assets'])==2)
    check('source PAK/index pinned no extraction',offset['matches_sealed_source_fingerprints'] and len(offset['checks'])==6 and not offset['new_raw_extraction'] and not offset['new_decode'])
    bg=load('Native-selection-resource-gap/Native-background-branch-reference.json');ib=b''.join(bytes.fromhex(x['bytes']) for x in bg['existing_fragment_lines'])
    check('ten existingbackground instructions exact40bytes',len(bg['existing_fragment_lines'])==10 and len(ib)==40 and hashlib.sha256(ib).hexdigest().upper()==bg['fixed_bytes_sha256'] and bg['fixed_bytes_match'] and bg['fixed_PE_origin']['va']=='00496F05')
    check('background IDs/conditional source only','push 0x1d3a' in [x['instruction'] for x in bg['existing_fragment_lines']] and 'push 0x18aa' in [x['instruction'] for x in bg['existing_fragment_lines']])
    gap=load('Native-selection-resource-gap/resource-gap-classification.json');plan=load('Native-selection-resource-gap/minimal-Native-binding-handoff.json')
    check('no known indexed but unextracted',gap['known_indexed_but_unextracted_count']==0 and not gap['categories']['known_indexed_but_not_extracted'])
    check('finite exact unknown categories',len(gap['categories']['resource_namespace_or_bank_unresolved'])==2 and len(gap['categories']['mapping_unknown'])==3 and len(gap['categories']['dynamic_initialization_or_lifecycle_unknown'])==3 and not gap['original_full_scene_resources_sufficient'])
    check('bindingproposal no fallback art/native scene claim',not plan['original_scene_complete'] and not plan['new_small_extraction_needed'] and len(plan['ready_for_minimal_binding'])==2 and len(plan['do_not_bind_until_qualified'])==3)
    check('no wire change/server notification',plan['wire_delta']['count']==0 and not plan['wire_delta']['new_differences'] and not plan['wire_delta']['server_notification_requested'])
    for q in p['prior_primary_pins']:check('prior immutable '+Path(q['path']).name,record(q['path'])['sha256']==q['expected_sha256'] and record(q['path'])['bytes']==q['bytes'])
    for q in p['sources']:check('fixed source pin '+Path(q['path']).name,record(q['path'])==q)
    v=p['verification']
    check('42 retained reads/36priors',len(cat['reads'])==42==v['retained_stable_read_records'] and len(p['prior_primary_pins'])==36==v['prior_primary_pins_preserved'])
    check('14reusedPEranges/1732bytes truthful',v['reused_fixed_PE_ranges']==14 and v['reused_fixed_PE_bytes']==12*96+540+40 and v['existing_background_instruction_bytes_checked']==10)
    check('all newactions zero',all(v[k]==0 for k in ['new_raw_extractions','new_code_ranges','new_data_ranges','new_art_assets','new_capture_frames','new_wire_differences','new_opcodes','wire_corrections','Modern_code_edits','new_branches_or_tests','tests_builds_compiles_or_GUI_runtime_runs','external_helper_imports','new_unknown_callee_bodies_expanded']))
    check('R527 unaffected/no full scene GUI proof',v['R527_uninterrupted'] and not v['GUI_or_product_complete'] and not v['native_full_selection_resource_binding_complete'])
    check('packet immutable',record(v['server_packet_unchanged']['path'])==v['server_packet_unchanged'])
    check('scope incident open immutable',record(v['scope_write_correction']['path'])==v['scope_write_correction'] and not v['cache_cleanup_completed'])
    result={'updated_at':stamp(),'primary':record(ROOT/'Native-selection-resource-gap-contract.json'),'all_match':True,'check_count':len(checks),'checks':checks,'scope':'Owned evidence, historical primary/outputs, rawtable checksum, exact source spans/index dimensions/provenance and immutable pins only. No Modern function/model/tests/build/GUI run. External live R527 sources not reread or frozen.','GUI_runtime_acceptance':False}
    write('Native-selection-resource-gap/finite-owned-evidence-review.json',result);print(json.dumps({k:result[k] for k in ['primary','all_match','check_count']},ensure_ascii=False))
if __name__=='__main__':main()
