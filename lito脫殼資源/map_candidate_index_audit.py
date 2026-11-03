"""Build Batch22 from five already sealed decoded files; no extraction or PE work."""
from audit import ROOT, record, write, stamp
from pathlib import Path
import json
from map_candidate_index import SCHEMA, POLICY, GUARD, parse_bytes, CandidateIndex

PREVIOUS_SHA = '12D76285CC907373A9CBB4D19C88ADD58AB997536E5238004321F83233B803C2'
NAMES = ['LTmapstr-c.tbl','LTmapstr-h.tbl','LTmapstr-h.txt','LTmapstr-j.tbl','LTmapstr-k.tbl']
FOLDER = 'map-candidate-index'

def load(name):
    return json.loads((ROOT/name).read_text(encoding='utf-8'))

def main():
    previous_pin = record(ROOT/'map-table-source-contract.json')
    assert previous_pin['sha256'] == PREVIOUS_SHA
    previous = load('map-table-source-contract.json')
    prior = previous['prior_primary_pins'] + [{**previous_pin,'expected_sha256':PREVIOUS_SHA,'match':True}]
    assert len(prior) == 23
    for pin in prior:
        current = record(Path(pin['path']))
        assert current['bytes'] == pin['bytes'] and current['sha256'] == pin['expected_sha256']
    evidence = load('map-table-source/existing-map-resource-candidates.json')
    assert [c['name'] for c in evidence['candidates']] == NAMES
    assert record(Path(evidence['archive_manifest']['path'])) == evidence['archive_manifest']
    manifest = load('resources/archive-manifest.json')
    # These are the existing equivalent candidates inspected before authoring.
    preflight = {
        'complete_parsed_rows_equivalent_found': False,
        'inspected': [
            {'pin':record(ROOT/'map-table-source/existing-map-resource-candidates.json'),
             'finding':'Five pinned source files; first six nonempty lines only. No complete rows, token boundaries, comment flags or queries.'},
            {'pin':record(ROOT/'resources/archive-manifest.json'),
             'finding':'Archive membership/raw/decoded and assumed-codepage display references only. No full physical-row parse.'},
            {'pin':record(ROOT/'extract_resources.py'),
             'finding':'Resource decode and optional cp950 display creation, no full # token parser/index. Not executed.'},
        ],
        'gap_filled':'Full byte-preserving rows/tokens/counts, separated candidates and explicit-key-only query interface.',
    }
    write(FOLDER+'/equivalence-preflight.json', preflight)
    entries, source_pins, display_pins, summaries = [], [], [], []
    for candidate in evidence['candidates']:
        name = candidate['name']
        member = next(x for x in manifest['selected_members'] if x['namespace']=='native' and x['archive']=='LTtext' and x['name']==name)
        for key in ('row','offset','bytes','source_index_sha256','source_pak_sha256','raw','decoded'):
            assert member[key] == candidate[key]
        for key in ('raw','decoded'):
            pin = candidate[key]
            assert record(Path(pin['path'])) == pin
            source_pins.append(pin)
        view = member.get('text_view')
        if view and 'output' in view:
            assert record(Path(view['output']['path'])) == view['output']
            display_pins.append(view['output'])
        provenance = {
            'namespace':'native', 'archive':'LTtext',
            **{k:candidate[k] for k in ('row','offset','bytes','source_index_sha256','source_pak_sha256','raw','decoded')},
            'existing_resource_encoding_marker':candidate['encoding_reference'],
            'marker_is_text_codec_qualification':False,
            'qualified_text_codec':None,
            'existing_display_reference':view,
            'existing_display_qualification':'Reference only: assumed_code_page 950 if present, not authoritative native decoding. No view created or rewritten.',
        }
        parsed = parse_bytes(Path(candidate['decoded']['path']).read_bytes(), name, provenance)
        relative = name+'.parsed.json'
        write(FOLDER+'/'+relative, parsed)
        entries.append({'candidate':name,'relative_file':relative,
                        'parsed_pin':record(ROOT/FOLDER/relative),'provenance':provenance,
                        'counts':parsed['counts'],'header_declared_count':parsed['header_declared_count']})
        summaries.append({'candidate':name,'header_declared_count':parsed['header_declared_count'],
                          'counts':parsed['counts'],'count_minus_header':parsed['count_minus_header'],
                          'terminal_eol':parsed['terminal_eol'],
                          'duplicate_explicit_file_keys':parsed['duplicate_explicit_file_keys']})
    write(FOLDER+'/catalog.json', {'schema':SCHEMA,'authority_guard':GUARD,
          'candidate_selection':'Exact filename only; five independent files, no default locale or merged key space.',
          'candidates':entries})
    index = CandidateIndex(ROOT/FOLDER/'catalog.json')
    examples = {
        'c_explicit_key_500':index.label_metadata_for_key('LTmapstr-c.tbl',500),
        'j_key_500_no_ordinal_fallback':index.label_metadata_for_key('LTmapstr-j.tbl',500),
        'c_key_571_active_only':index.key('LTmapstr-c.tbl',571),
        'c_key_571_including_inactive_comments':index.key('LTmapstr-c.tbl',571,True),
        'h_table_physical_line_2':index.line('LTmapstr-h.tbl',2),
        'h_text_physical_line_2':index.line('LTmapstr-h.txt',2),
        'j_last_unterminated_line':index.line('LTmapstr-j.tbl',index.metadata('LTmapstr-j.tbl')['counts']['physical_rows']),
        'c_exact_ascii_token_town':index.token('LTmapstr-c.tbl',b'town'),
        'h_exact_opaque_token':index.token('LTmapstr-h.tbl',bytes.fromhex(index.line('LTmapstr-h.tbl',2)['tokens'][1]['raw_hex'])),
    }
    write(FOLDER+'/query-examples.json',examples)
    outputs = [record(p) for p in sorted((ROOT/FOLDER).iterdir()) if p.is_file() and p.name not in ('independent-review.json','READ-ME.txt')]
    report = {
        'schema':'Batch22-map-candidate-index-contract-v1','updated_at':stamp(),
        'status':'FINITE_OFFLINE_BYTE_INDEX_BUILT_RUNTIME_RESOURCE_BINDING_OPEN',
        'prior_primary_pins':prior,'prior_source_contract':previous_pin,
        'input_evidence':{'existing_candidates':record(ROOT/'map-table-source/existing-map-resource-candidates.json'),
                          'archive_manifest':evidence['archive_manifest']},
        'source_pins':source_pins,'optional_existing_display_pins':display_pins,
        'parser':record(ROOT/'map_candidate_index.py'),'policy':POLICY,
        'equivalence_preflight':record(ROOT/FOLDER/'equivalence-preflight.json'),
        'catalog':record(ROOT/FOLDER/'catalog.json'),'candidate_summaries':summaries,
        'query_interface':{
            'library':'map_candidate_index.CandidateIndex',
            'methods':['list_candidates()','metadata(exact_filename)','line(exact_filename, physical_line_1based)',
                       'key(exact_filename, explicit_file_key, include_comments=False)',
                       'token(exact_filename, exact_bytes, include_comments=False)',
                       'label_metadata_for_key(exact_filename, explicit_file_key, include_comments=False)'],
            'CLI':'python map_candidate_index.py --candidate LTmapstr-c.tbl label-metadata 500',
            'unknown_codec_query':'token-hex; token-ascii accepts strict ASCII only; opaque bytes never replaced',
            'query_examples':record(ROOT/FOLDER/'query-examples.json'),
            'portable_reader':'Parsed-file bytes/SHA/candidate/provenance checked against catalog. Original archive/PE/runtime not needed for queries; catalog trust inherited from primary pin.',
        },
        'outputs':outputs,
        'verification':{'candidate_files':5,'raw_decoded_pins':10,'prior_primary_pins_preserved':23,
                        'total_physical_rows':sum(s['counts']['physical_rows'] for s in summaries),
                        'all_source_and_token_bytes_preserved':True,
                        'server_packet_unchanged':record(ROOT/'server-packet-delta.json')},
        'delta':{'new_wire_differences':0,'new_opcodes':0,'wire_corrections':0,'new_capture_frames':0,
                 'new_PE_ranges':0,'packet_modified':False,'Java_Modern_BIN_changed':False},
        'scope':{'writes':'own resource directory only','authority_guard':GUARD,
                 'actual_table_materialization_or_map_resource_binding_qualified':False,
                 'locale_runtime_index_file_key_or_mapID_equivalence':False,
                 'codec_redecode_or_suffix_inference':False,'archive_reextraction':False,
                 'Native_GUI_provider_SQL_WORLD_private_body_expansion':False,
                 'Batch20_Batch21_PASS_replayed':False,
                 'finite_stop':'Full candidate index/query complete; provider/parser/runtime generation/record ordering/resource binding remain unknown.'},
    }
    write('map-candidate-index-contract.json',report)
    print(json.dumps({'primary':record(ROOT/'map-candidate-index-contract.json'),'candidate_summaries':summaries},ensure_ascii=False))

if __name__ == '__main__':
    main()
