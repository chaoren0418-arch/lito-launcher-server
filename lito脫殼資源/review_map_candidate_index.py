"""Independent source partition/conservation and finite query regression checks."""
import json
import re
from pathlib import Path
from audit import ROOT, record, write
from map_candidate_index import CandidateIndex, parse_bytes, sha

def main():
    primary = json.loads((ROOT/'map-candidate-index-contract.json').read_text(encoding='utf-8'))
    checks = []
    def check(name, condition):
        checks.append({'check':name,'match':bool(condition)})
        assert condition, name
    for p in primary['prior_primary_pins']:
        current = record(Path(p['path']))
        check('prior '+Path(p['path']).name, current['bytes']==p['bytes'] and current['sha256']==p['expected_sha256'])
    for p in primary['source_pins']+primary['optional_existing_display_pins']:
        check('source unchanged '+Path(p['path']).name,record(Path(p['path']))==p)
    for p in list(primary['input_evidence'].values())+primary['outputs']+[primary['parser']]:
        check('index/evidence pin '+Path(p['path']).name,record(Path(p['path']))==p)
    index = CandidateIndex(ROOT/'map-candidate-index/catalog.json')
    coverage = []
    for entry in index.catalog['candidates']:
        name = entry['candidate']
        parsed = index._load(name)
        source = Path(entry['provenance']['decoded']['path']).read_bytes()
        # Independent regex partition, not the production parser loop/splitlines.
        oracle = [(m.start(),m.group(1),m.group(2)) for m in re.finditer(rb'([^\r\n]*)(\r\n|\r|\n|$)',source) if m.end()>m.start()]
        check(name+' full physical partition',len(oracle)==len(parsed['rows']) and all(
            (r['byte_offset'],bytes.fromhex(r['content_hex']),bytes.fromhex(r['eol_hex']))==o
            for r,o in zip(parsed['rows'],oracle)))
        check(name+' raw bytes conservation',b''.join(bytes.fromhex(r['raw_bytes_hex']) for r in parsed['rows'])==source)
        next_offset = 0
        token_bytes, delimiters = 0, 0
        for r,(offset,content,eol) in zip(parsed['rows'],oracle):
            assert r['byte_offset']==next_offset
            next_offset += r['full_byte_length']
            assert r['full_byte_length']==len(content)+len(eol)
            assert r['content_byte_length']==len(content) and r['content_sha256']==sha(content)
            starts=[0]+[i+1 for i,b in enumerate(content) if b==35]
            stops=[i for i,b in enumerate(content) if b==35]+[len(content)]
            assert len(starts)==len(r['tokens'])
            for n,(a,b,t) in enumerate(zip(starts,stops,r['tokens'])):
                piece=content[a:b]
                assert t['token_0based']==n and t['byte_offset']==offset+a and t['byte_length']==b-a
                assert bytes.fromhex(t['raw_hex'])==piece
                assert t['printable_ascii']==(piece.decode('ascii') if all(32<=x<=126 for x in piece) else None)
                token_bytes+=len(piece)
            assert r['hash_delimiter_byte_offsets']==[offset+i for i,b in enumerate(content) if b==35]
            assert r['semicolon_byte_offsets']==[offset+i for i,b in enumerate(content) if b==59]
            delimiters+=content.count(b'#')
        check(name+' complete nonoverlapping token intervals',next_offset==len(source) and
              token_bytes+delimiters+sum(len(o[2]) for o in oracle)==len(source))
        check(name+' deterministic full parse',parse_bytes(source,name,entry['provenance'])==parsed)
        after=oracle[parsed['header_physical_line_1based']:]
        nonblank=[o for o in after if o[1].strip(b' \t')]
        active=[o for o in nonblank if not o[1].lstrip(b' \t').startswith(b';')]
        explicit=[o for o in active if b'#' in o[1] and re.fullmatch(rb'[+-]?[0-9]+',o[1].split(b'#')[0].strip(b' \t'))]
        check(name+' independent counts',parsed['counts']['physical_rows_after_header']==len(after) and
              parsed['counts']['nonblank_rows_after_header']==len(nonblank) and
              parsed['counts']['noncomment_nonblank_rows_after_header']==len(active) and
              parsed['counts']['explicit_numeric_key_rows']==len(explicit) and
              parsed['counts']['hash_delimiters']==delimiters)
        coverage.append({'candidate':name,'source_pin':entry['provenance']['decoded'],
                         'physical_rows':len(oracle),'token_bytes':token_bytes,'hash_delimiter_bytes':delimiters,
                         'eol_bytes':sum(len(o[2]) for o in oracle),'covered_bytes':next_offset,
                         'all_rows_tokens_intervals_match':True})
    check('five independent candidates',index.list_candidates()==['LTmapstr-c.tbl','LTmapstr-h.tbl','LTmapstr-h.txt','LTmapstr-j.tbl','LTmapstr-k.tbl'])
    check('c 500 explicit line2', [r['physical_line_1based'] for r in index.key('LTmapstr-c.tbl',500)]==[2])
    check('k 500 explicit line2', [r['physical_line_1based'] for r in index.key('LTmapstr-k.tbl',500)]==[2])
    for name in ('LTmapstr-h.tbl','LTmapstr-h.txt','LTmapstr-j.tbl'):
        check(name+' no synthesized numeric keys',index.metadata(name)['counts']['explicit_numeric_key_rows']==0 and index.key(name,0)==[] and index.key(name,500)==[])
    check('c inactive571 retained separately',index.key('LTmapstr-c.tbl',571)==[] and
          len(index.key('LTmapstr-c.tbl',571,True))==1 and index.key('LTmapstr-c.tbl',571,True)[0]['file_declared_key'] is None)
    check('h opaque label not decoded',index.line('LTmapstr-h.tbl',2)['tokens'][1]['printable_ascii'] is None)
    check('h.txt plain one opaque token',index.line('LTmapstr-h.txt',2)['kind']=='plain_bytes' and len(index.line('LTmapstr-h.txt',2)['tokens'])==1)
    j=index.metadata('LTmapstr-j.tbl')
    check('j declared57 actual55 after header',j['header_declared_count']==57 and j['counts']['physical_rows_after_header']==55 and j['count_minus_header']['physical_rows_after_header']==-2)
    check('j last row retained unterminated',not j['terminal_eol'] and index.line('LTmapstr-j.tbl',56)['eol_kind']=='NONE' and index.line('LTmapstr-j.tbl',56)['placeholder_markers'])
    opaque=bytes.fromhex(index.line('LTmapstr-h.tbl',2)['tokens'][1]['raw_hex'])
    check('opaque token exact query',index.line('LTmapstr-h.tbl',2) in index.token('LTmapstr-h.tbl',opaque))
    check('label metadata has no qualified title column',index.label_metadata_for_key('LTmapstr-c.tbl',500)['label_column_qualified'] is False)
    def rejects(name, fn, exception):
        try: fn()
        except exception: check(name,True)
        else: check(name,False)
    rejects('candidate suffix fallback prohibited',lambda:index.metadata('c'),KeyError)
    rejects('line zero prohibited',lambda:index.line('LTmapstr-c.tbl',0),ValueError)
    rejects('line outside candidate prohibited',lambda:index.line('LTmapstr-c.tbl',69),IndexError)
    rejects('float key prohibited',lambda:index.key('LTmapstr-c.tbl',500.0),TypeError)
    rejects('bool key prohibited',lambda:index.key('LTmapstr-c.tbl',True),TypeError)
    rejects('Unicode token requires explicit bytes',lambda:index.token('LTmapstr-h.tbl','城市'),TypeError)
    # Explicit expected corner cases; 0x85/NUL must remain token bytes.
    fixture=b'\t \r\n3\r+005#A##\n \t;005#inactive#\r\n005#\x85\x00\xff#x;y\rno exist#no exsist\n-1#Z'
    parsed=parse_bytes(fixture,'synthetic-only',{})
    rows=parsed['rows']
    check('synthetic mixed EOL exactly seven lines',len(rows)==7 and [r['eol_kind'] for r in rows]==['CRLF','CR','LF','CRLF','CR','LF','NONE'])
    check('synthetic first nonblank header',parsed['header_physical_line_1based']==2 and parsed['header_declared_count']==3)
    check('synthetic blank retained',rows[0]['kind']=='blank' and rows[0]['content_hex']=='0920')
    check('synthetic empty/trailing tokens retained',[t['raw_hex'] for t in rows[2]['tokens']]==['2b303035','41','',''])
    check('synthetic comment lexical bytes inactive',rows[3]['file_declared_key'] is None and rows[3]['commented_file_key']==5 and rows[3]['content_hex']=='20093b30303523696e61637469766523')
    check('synthetic duplicate numeric canonical key preserves lexical forms',parsed['duplicate_explicit_file_keys']=={'5':[3,5]} and rows[2]['file_key_lexeme_hex']=='2b303035' and rows[4]['file_key_lexeme_hex']=='303035')
    check('synthetic nonASCII NUL85 not split/replaced',rows[4]['tokens'][1]['raw_hex']=='8500ff' and rows[4]['tokens'][1]['printable_ascii'] is None)
    check('synthetic inline semicolon preserved',rows[4]['tokens'][2]['raw_hex']=='783b79' and not rows[4]['full_line_comment'])
    check('synthetic both placeholder spellings retained',[m['marker_ascii'] for m in rows[5]['placeholder_markers']]==['no exist','no exsist'])
    check('synthetic negative key no width coercion',rows[6]['file_declared_key']==-1)
    check('synthetic complete coverage',b''.join(bytes.fromhex(r['raw_bytes_hex']) for r in rows)==fixture)
    synthetic_reader=CandidateIndex.__new__(CandidateIndex)
    synthetic_reader._load=lambda candidate:parsed if candidate=='synthetic-only' else None
    check('duplicate query returns all ordered matches',[r['physical_line_1based'] for r in synthetic_reader.key('synthetic-only',5)]==[3,5])
    check('duplicate query comments opt-in only',[r['physical_line_1based'] for r in synthetic_reader.key('synthetic-only',5,True)]==[3,4,5])
    check('empty byte source no phantom row',parse_bytes(b'','empty',{})['rows']==[])
    check('terminal EOL no phantom row',len(parse_bytes(b'1\r\n','terminal',{})['rows'])==1)
    check('leading numeric plain label no invented key',parse_bytes(b'2\n500\n','plain',{})['rows'][1]['file_declared_key'] is None)
    check('nondecimal header stays opaque',parse_bytes(b'not-count\n1#x','no-header',{})['header_declared_count'] is None)
    check('packet unchanged',record(ROOT/'server-packet-delta.json')==primary['verification']['server_packet_unchanged'])
    report={'primary':record(ROOT/'map-candidate-index-contract.json'),'all_match':True,
            'checks':checks,'check_count':len(checks),'candidate_coverage':coverage,
            'total_physical_rows':sum(x['physical_rows'] for x in coverage),
            'total_source_bytes':sum(x['covered_bytes'] for x in coverage),
            'synthetic_fixture_hex':fixture.hex(), 'synthetic_checks_only_offline':True,
            'prior_primary_pins_preserved':23,'raw_decoded_pins_preserved':10,
            'archive_reextract_codec_redecode_PE_Native_GUI_provider_SQL':False,
            'Batch20_Batch21_PASS_replayed':False,'runtime_resource_binding_qualified':False}
    write('map-candidate-index/independent-review.json',report)
    print(json.dumps({k:v for k,v in report.items() if k not in ('checks','synthetic_fixture_hex','candidate_coverage')},ensure_ascii=False))

if __name__=='__main__':main()
