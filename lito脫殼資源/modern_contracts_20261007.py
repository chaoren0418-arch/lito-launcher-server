"""Normalize qualified existing evidence into one Modern consumer contract.

No new PE disassembly, archive extraction, DLL execution or old tests.
"""
import sys
sys.dont_write_bytecode=True
from pathlib import Path
import json
import hashlib
from datetime import datetime,timezone,timedelta

ROOT=Path(__file__).resolve().parent
TASK='bin_resources-modern-contracts-20261007'


def stamp():
    return datetime.now(timezone(timedelta(hours=8))).isoformat()


def pin(path):
    path=Path(path);raw=path.read_bytes()
    return {'path':str(path),'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest().upper()}


def read(name):
    return json.loads((ROOT/name).read_text('utf-8-sig'))


def write(name,value):
    path=ROOT/name
    assert path.resolve().is_relative_to(ROOT)
    path.parent.mkdir(parents=True,exist_ok=True)
    temp=path.with_name(path.name+'.tmp')
    temp.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf-8');temp.replace(path)


def main():
    assert not (ROOT/'modern-contracts-20261007.json').exists(), 'Do not repeat an unchanged consumer delivery'
    names=['Native-background-scene-contract.json','resources/modern-rgba/manifest.json',
           'modern-F4-contract.json','priority-api-resolution.json','host-alias-lifecycle.json',
           'reconnect-source-index/host-alias-port-contract.json','server-packet-delta.json',
           'Native-selection-resource-gap/Native-animation-and-slot-data.json']
    sources={n:pin(ROOT/n) for n in names}
    inputs={n:read(n) for n in names}
    bg=inputs[names[0]];rgba=inputs[names[1]];f4=inputs[names[2]]
    api=inputs[names[3]];host=inputs[names[4]];reconnect=inputs[names[5]];packet=inputs[names[6]]
    slot=inputs[names[7]]
    pe=bg['source_PE']
    assert pe['sha256']=='4271D92FB2B9A2FA8D118DDE0767F6D8A67A896227BCC24127438FCB5C2AB6E9'
    assert sources[names[0]]['sha256']=='DDD86F5D197D53987A46DFBBBB879E8FBE0C77C1AE3B68C296EF4F62325056A7'
    assert sources[names[1]]['sha256']=='D25BED57BE58F0AB3EB17F6A716F8202329B4E02BEE98F63B50D84ADA8218540'
    assert sources['server-packet-delta.json']['sha256']=='B4C00BF1C7F8D5FC17F0B819965527A63E9F9F3BDE822E1F1D4BA59A7733B54C'

    def address(va):
        value=int(va,16) if isinstance(va,str) else va
        return {'va':f'{value:08X}','rva':f'{value-0x400000:08X}','source_PE_sha256':pe['sha256']}

    def ref(*ns):
        return [sources[n] for n in ns]

    assets=[]
    for a in rgba['assets']:
        assets.append({'id':a['id'],'namespace':a['namespace'],'archive':a['archive'],
                       'row':a['row'],'offset':a['source_offset'],'length':a['source_length'],
                       'source_index_sha256':a['source_index_sha256'],'source_pak_sha256':a['source_pak_sha256'],
                       'raw':a['raw'],'rgba':a['rgba'],'pixel_format':'RGBA8','width':a['width'],
                       'height':a['height'],'stride_bytes':a['stride_bytes'],'row_order':a['row_order'],
                       'alpha':a['alpha'],'source_encoding':'plain IMG; qualified RGB555 reference decoding',
                       'scene_binding':None,'draw_position':None,'native_render_parity':False})
    background={
        'id':'native_background_assets','category':'drawing_resources','source_PE':pe,
        'existing_status':'completed: raw IMG, preview PNG and direct RGBA buffers already delivered',
        'increment':'New unified resource/selector representation only; no new pixels or extraction',
        'anchors':[address(0x496F05),address(0x496F28),address(0x64AFA0)],
        'resources':assets,
        'selector':{'input':{'name':'flag_64AFA0','type':'u8','initialized_value_known':False},
                    'branches':[{'test':'eq','value':0,'resource_id':6314},
                                {'test':'ne','value':0,'resource_id':7482}],
                    'on_missing_input':'unresolved; do not treat fixed disk0 as live flag0',
                    'output_kind':'source-conditional asset ID; no scene name'},
        'offline_consumer_ready':True,
        'offline_operations':['Load already pinned RGBA into native_mode_menu_skin::Img or a bounded resource reader.',
                              'Select asset using an explicitly supplied source flag; preserve uninitialized state.',
                              'CPU preview may use explicitly caller-chosen preview geometry, distinguished from unknown Native draw geometry.'],
        'unknowns':['Named scene / Directory association','Backdrop origin/crop/clip/transform/draw mode',
                    'Current initialized flag/owner and reset lifecycle'],
        'consumer':{'owner':'modern','module':'src/modern_lito/native_mode_menu_skin.rs',
                    'input_type':'Img {width:u32,height:u32,rgba:Vec<u8>}; resource reader must verify pin/length',
                    'native_scene_binding_allowed':False},
        'handoff_refs':ref(names[0],names[1]),'human_presence_required_for_offline_consumption':False}

    draw={
        'id':'495520_call_boundary','category':'infrastructure_and_drawing','source_PE':pe,
        'existing_status':'completed source callsite/owner/one-hop predicates; actual backdrop draw contract unknown',
        'increment':'Register/stack/numeric predicates represented as machine fields; no constructor body expanded',
        'anchors':[address(v) for v in [0x496EB0,0x496EB8,0x496F28,0x631EC8,0x631ECC,0x4A74CB,0x4AAD02]],
        'call':{'site':address(0x496F28),'target':address(0x495520),
                'registers':{'ECX':{'load_from':address(0x631ECC),'fixed_data_value':0x630DF8},
                             'EDX':{'load_from':address(0x631EC8),'fixed_data_value':8}},
                'stack_push_sequence':[{'name':'image_id','value_source':'native_background_assets.selector'},
                                       {'name':'numeric_kind_argument','value':7}],
                'return':{'register':'EAX','saved_to':'EDI','save_site':address(0x496F2D)},
                'role_limit':'Existing source describes a UI factory/constructor call; a direct draw primitive or coordinate calling convention is not established.',
                'backdrop_x':None,'backdrop_y':None},
        'owner':{'global':address(0x75F6E8),'field_offset':12,'compared_value':5,
                 'non_equal_path':'496F05: background selector',
                 'equal_path':'Opaque helper calls;495520@496EFB with631E38/EDX5/push310/push0;return EAX stored75F6E8.',
                 'live_instance_known':False,'numeric_type_name':None},
        'one_hop_predicates':[{'caller':address(0x4A7450),'transfer':address(0x4A74CB),
                              'source_event':'existing S03 signed countBYTE handler',
                              'all':[{'field':'decoded_count','op':'eq','value':0},
                                     {'field':'63D54C','type':'i32','op':'lt','value':30811},
                                     {'field':'64AFA3','type':'u8','op':'eq','value':0}],
                              'opaque_precondition':'49A510(ECX1) returns normally',
                              'after_normal_return':'64AFA3=1'},
                             {'caller':address(0x4AACF0),'transfer':address(0x4AAD02),
                              'all':[{'field':'64AFBC','type':'u8','op':'eq','value':0},
                                     {'field':'64AFBD','type':'u8','op':'eq','value':0}],
                              'transfer_kind':'tail jump'}],
        'child_descriptor_metadata':{'table':address(0x630DF8),'fixed_count':8,
                                      'previously_qualified_first_four_slots':slot['slots'],
                                      'limit':'Child x/y and count8 do not establish background x/y or total character capacity.'},
        'offline_consumer_ready':True,
        'offline_operations':['Consume explicit source predicates/descriptor metadata in an offline call model.',
                              'Retain source numeric values instead of inventing scene/type names.'],
        'blocked_extension':{'missing':'Accepted existing495520 result-to-backdrop draw/owner semantics or independently qualified saved instance artifact',
                             'supplier':'Qualified existing BIN/Modern evidence supplied through supervisor',
                             'reason':'Retained stop at495520 constructor/owner semantics; no permission to continue a private next layer.',
                             'human_presence_dependency':False},
        'unknowns':['Named scene','Constructor return ownership semantics','Backdrop geometry/renderer mode',
                    'Opaque helper/virtual callback side effects and live initialized owner'],
        'consumer':{'owner':'modern','module':'offline background call/scene model plus native_mode_menu_skin.rs',
                    'default_scene_promotion_allowed':False},'handoff_refs':ref(names[0],names[7])}

    f4_packet=next(p for p in packet['packets'] if p.get('opcode_decimal')==244)
    api_targets=[]
    for a in api['resolutions']:
        api_targets.append({'name':a['current_export']['name'],'main_image':a['main_image_thunk'],
                            'branch':a['branch_instruction'],'export_rva':a['current_export']['rva'],
                            'confidence':a['confidence'],'dump_time_dll_sha256':a['historical_dll_sha256']})
    transfer={
        'id':'S_F4_244','category':'packets_and_external_conversion','source_PE':pe,
        'existing_status':'completed source-qualified schema / external identity / IPv4 conversion; previously adopted F3/F4',
        'increment':'Typed fields and transition conditions normalized for consumer; no new wire or API execution',
        'anchors':[address(v) for v in [0x4B3057,0x4AC570,0x502F2C,0x4AC5B0,0x4AC5BD,0x46ED50]],
        'wire':{'direction':'S','opcode_decimal':244,'opcode_hex':'F4',
                'body_length_including_opcode':8,'format':'cdh','fields':f4_packet['fields'],
                'native_exact_length_guard_proven':False,
                'modern_exact_length_guard':8,'new_or_corrected_wire':False},
        'ipv4_conversion':{'packet_offset':2,'width_bytes':4,'integer_assembly':'little endian',
                           'octet_view':'The same four bytes are network IPv4 octets',
                           'modern_operation':'Ipv4Addr::from(address_word.to_le_bytes())',
                           'constant_compare':{'literal_va':address(0x62C6A4),'text':'127.0.0.1',
                                               'x86_u32_hex':f4['canonical_vectors'][0]['x86_u32'],
                                               'limit':'Fixed PE constant, not universal configured endpoint'}},
        'transition_order':[{'test':'table[signed_index]==0xFFFFFFFF','effect':'No transition'},
                            {'test':'address_word==fixed inet_addr(127.0.0.1)','effect':'46EB30 same-address path'},
                            {'test':'otherwise','effect':'46ED50 redirect: inet_ntoa; host64AE74/64A9C4; port low16 to64AD70/64A9C0; callback469B10'}],
        'table':{'base':address(0x64AD2C),'cells':10,'cell_width_bytes':4,
                 'index_type':'i8 sign-extended','native_local_bounds_guard_proven':False,
                 'modern_safety_domain':{'min':0,'max':9,'qualification':'Modern safety policy / valid producer domain, not native rejection proof'}},
        'external_api_identities':api_targets,
        'return_mode_2':{'source_entry':address(0x46F010),'saved_host':address(0x64AD74),
                         'destination_host':address(0x64AE74),'port_word':2000,'callback':address(0x469BD0),
                         'limit':'Separate return path; do not treat this as allF4 packets'},
        'offline_consumer_ready':True,'offline_operations':['Deserialize existing typed schema / source branch model.',
                                                           'Use existing pure address_from_word mapping; no DLL execution needed.'],
        'unknowns':f4_packet['unknowns'],
        'consumer':{'owner':'modern','module':'src/modern_lito/native_entry_transfer.rs',
                    'existing_symbols':['decode','address_from_word','consume_source_transfer_event'],
                    'requires_human_or_GUI_before_offline_use':False},
        'handoff_refs':ref(names[2],names[3],names[6])}

    saved=reconnect['saved_startup_host'];alias=reconnect['saved_character_alias']
    buffers=[]
    for b in host['buffer_map']:
        buffers.append({**b,**address(b['va'])})
    host_model={
        'id':'saved_host_64AD74','category':'infrastructure','source_PE':pe,
        'existing_status':'completed direct writer/source/buffer ownership contract; actual runtime host/init chain remains unproven',
        'increment':'Typed buffer map and event-bound ownership/copy specification for Modern',
        'anchors':[address(v) for v in [0x47C9BC,0x47C9D0,0x47C9D8,0x47C9DF,0x64B7E4,0x64AD74,0x64AD54,0x64AD70]],
        'buffers':buffers,
        'saved_host':{'source':address(0x64B7E4),'destination':address(0x64AD74),
                      'inline_bytes':saved['capacity'],'pointer_global':False,
                      'copy_through_first_NUL':True,'copy_includes_NUL':True,
                      'encoding':saved['encoding'],'native_capacity_guard_proven':False,
                      'safe_bounded_payload_bytes_excluding_NUL':saved['in_bounds_payload_max_bytes_excluding_NUL'],
                      'source_event':saved['acquisition_event'],'writers':saved['writer'],
                      'startup_sources':saved['startup_sources_already_established'],
                      'COM_candidate_qualified':saved['fourth_candidate_qualification']['usable_COM_derived_host_proven'],
                      'actual_initialized_bytes':None},
        'saved_alias':{'destination':address(0x64AD54),'inline_bytes':alias['capacity'],
                       'safe_bounded_payload_bytes_excluding_NUL':alias['in_bounds_payload_max_bytes_excluding_NUL'],
                       'source':alias['record_source'],'source_event':alias['acquisition_event'],
                       'encoding':alias['encoding'],'copies_own_bytes':True,
                       'is_startup_host':False,'native_capacity_guard_proven':False},
        'port_neighbor':reconnect['neighbor_port'],
        'clear_scope':{'half_open_va':['0064AD2C','0064AD54'],'bytes':40,
                       'clears_alias_port_or_host':False},
        'offline_consumer_ready':True,
        'offline_operations':['Represent distinct owned byte buffers and copy-through-NUL in Modern state.',
                              'Keep saved_host uninitialized until an explicit qualified startup source/event is supplied.',
                              'Apply bounded byte validation as Modern safety; do not claim Native enforced that limit.'],
        'unknowns':host['host_initializer']['unclosed']+['Upstream codepage/IDN normalization','Current value / execution epoch',
                    'Private or indirect clearing beyond the qualified direct scope'],
        'blocked_extension':{'missing':'Actual initialization/050104CD success and same-epoch owner/value evidence',
                             'scope':'Only that dynamic conclusion is unavailable; pure buffer/packet modelling remains offline usable',
                             'supplier':'Independently qualified existing startup evidence; not a fictional separate PE group prerequisite'},
        'consumer':{'owner':'modern','module':'src/modern_lito/native_entry_transfer.rs plus startup host state',
                    'saved_primary_endpoint_may_be_invented':False,'requires_human_or_GUI_before_offline_use':False},
        'handoff_refs':ref(names[4],names[5])}
    contracts=[background,draw,transfer,host_model]
    requests=[]
    modern_root=ROOT.parent/'lito重建版'
    for base in [modern_root,modern_root/'LiTo381-Modern'/'logs'/'continuation-20261003']:
        p=base/'modern-bin-consumer-20261007.json'
        if p.exists():requests.append(pin(p))
    result={
        'schema':'lito-modern-consumer-contracts-v1','task_id':TASK,'updated_at':stamp(),
        'generator':pin(__file__),'source_PE':pe,'contracts':contracts,
        'delivery_kind':'Incremental machine-readable bridge from existing accepted/qualified source artifacts',
        'completion':{'unified_offline_bridge_delivered':True,'new_PE_ranges':0,'new_private_bodies':0,
                      'new_resource_extractions':0,'old_API_or_source_matrix_tests_replayed':False,
                      'Native_or_GUI_runtime_evidence_added':False,'overall_product_complete':False},
        'wire_delta':{'baseline':sources['server-packet-delta.json'],'new_opcodes':0,'corrected_wire_contracts':0,
                      'envelopes':[],'server_notification_requested':False},
        'Modern_request_artifacts_at_read':requests,
        'request_status':'Observed supplied request refs if present; absence does not block this offline delivery',
        'remaining_actions':[
            {'id':'modern-consume-offline-bridge','owner':'modern','state':'offline_available',
             'input':'This typed JSON plus cited existing asset/evidence files','human_presence_required':False},
            {'id':'native-background-draw-binding','state':'blocked_at_retained_scope',
             'missing':'Named scene / backdrop geometry /495520 result semantics','source_call_parameters_already_complete':True},
            {'id':'native-runtime-host-owner-proof','state':'dynamic_evidence_unknown',
             'missing':'Actual same-epoch initialized host/owner/private prerequisite outcomes','offline_model_blocked':False}],
        'boundaries':['42E550/42E6D0/42EA50 and prohibited private next layers remain stopped',
                      'No PE/BIN, Modern code, server Java/SQL, other processes or retained failed cleanup actions',
                      'No T291/T27/refund/rebid/auction/castle/full-WORLD transfer or workaround'],
    }
    write('modern-contracts-20261007.json',result)
    # Verify the new bridge's mapping, not old API semantics/rendering PASS.
    out=read('modern-contracts-20261007.json')
    assert len(out['contracts'])==4
    by={c['id']:c for c in out['contracts']}
    assert {a['id'] for a in by['native_background_assets']['resources']}=={6314,7482}
    assert all(a['rgba']==b['rgba'] for a,b in zip(by['native_background_assets']['resources'],rgba['assets']))
    assert all(a['width']*4==a['stride_bytes'] and a['stride_bytes']*a['height']==a['rgba']['bytes'] for a in by['native_background_assets']['resources'])
    assert by['495520_call_boundary']['call']['backdrop_x'] is None and by['495520_call_boundary']['call']['backdrop_y'] is None
    assert by['S_F4_244']['wire']['fields']==f4_packet['fields']
    assert by['S_F4_244']['wire']['body_length_including_opcode']==f4_packet['body_length_including_opcode']==8
    assert by['saved_host_64AD74']['saved_host']['inline_bytes']==256 and by['saved_host_64AD74']['saved_alias']['inline_bytes']==28
    assert not by['saved_host_64AD74']['clear_scope']['clears_alias_port_or_host']
    assert all(c['source_PE']['sha256']==pe['sha256'] and c['anchors'] and c['unknowns'] and c['consumer'] and c['handoff_refs'] for c in out['contracts'])
    assert all(int(a['va'],16)-int(a['rva'],16)==0x400000 for c in out['contracts'] for a in c['anchors'])
    assert all(pin(ROOT/n)==sources[n] for n in names)
    write('modern-contracts-20261007/bridge-verification.json',{
        'updated_at':stamp(),'contract':pin(ROOT/'modern-contracts-20261007.json'),
        'checks':{'four_consumable_categories':True,'asset_pins_and_stride_preserved':True,
                  'F4_wire_fields_preserved_exactly':True,'host_alias_port_separate':True,
                  'all_anchor_VA_RVA_consistent':True,'unknown_draw_and_runtime_values_preserved':True,
                  'cited_input_artifacts_unchanged':True,'no_new_wire':True},
        'method':'Targeted verification of new JSON bridge only; no old vectors, DLL, PE disassembly or resource decode rerun',
        'input_refs':list(sources.values()),'all_pass':True})
    p=ROOT/'監製進度.json';progress=read('監製進度.json')
    progress.update(updated_at=stamp(),workflow_task_id=TASK,workflow_task_state='review',
                    current_task='20261007離線Modern契約轉接已交付：背景RGBA/495520呼叫邊界/F4 typed轉換/64AD74 buffer map；0重抽、0私有body、0wire更正。',
                    workflow_handoff={'contract':pin(ROOT/'modern-contracts-20261007.json'),
                                      'verification':pin(ROOT/'modern-contracts-20261007/bridge-verification.json'),
                                      'handoff_to':['modern','supervisor'],'proposal_state':'review',
                                      'offline_consumer_ready':True,'overall_product_complete':False,
                                      'Native_scene_draw_and_runtime_owner_unknown':True,
                                      'server_notification_requested':False})
    write('監製進度.json',progress)
    write('modern-contracts-20261007/delivery.json',{
        'updated_at':stamp(),'task_id':TASK,'state':'review',
        'files':[pin(ROOT/n) for n in ['modern-contracts-20261007.json','modern_contracts_20261007.py',
                                     'modern-contracts-20261007/bridge-verification.json','監製進度.json']],
        'old_stage_reports_and_source_outputs_modified':False,'product_accepted':False})
    print(json.dumps({'contract':pin(ROOT/'modern-contracts-20261007.json'),
                      'verification':pin(ROOT/'modern-contracts-20261007/bridge-verification.json'),
                      'request_refs_observed':requests,'state':'review','offline_consumer_ready':True},ensure_ascii=True))


if __name__=='__main__':
    main()
