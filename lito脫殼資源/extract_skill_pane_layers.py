"""Batch9: extract exact native933/935 once; no934 or other payload replay."""
from audit import *
from extract_member import load_catalogue,extract

def main():
    out=ROOT/'skill-pane-layer-composition/layer-extraction.json'
    assert not out.exists(),'Single extraction already recorded; do not rerun.'
    cat,tables=load_catalogue();names={f'{n}{v}.img'for n in (933,935)for v in ('','e','c','j','h')}
    candidates=[{'archive':k,'namespace':cat[k]['namespace'],'source_index':cat[k]['decoded_index'],
      'rows':[r for r in rows if r['name'].lower()in names]}for k,rows in tables.items()if any(r['name'].lower()in names for r in rows)]
    a=cat['LTsurf'];rows=[r for r in tables['LTsurf']if r['name']in {'933.img','935.img'}]
    assert rows==[{'row':1556,'name':'933.img','offset':91134715,'bytes':104676},{'row':1558,'name':'935.img','offset':91359045,'bytes':105974}]
    checks=[]
    for k in ['index_source','decoded_index','pak_source']:
        actual=record(Path(a[k]['path']));assert actual==a[k];checks.append({'kind':k,**actual,'match':True})
    results=[extract(a,row,'skill-pane-layers')for row in rows]
    for r in results:
        assert r['raw']==r['decoded'];raw=Path(r['decoded']['path']).read_bytes()
        r.update(header_first_32_hex=raw[:32].hex(),header=list(struct.unpack_from('<hhHH',raw)),
          namespace_qualification='Native LTsurf default-name priority candidate after hashed shard miss. Actual language variant/cache/I/O unobserved;933c/933h exist in sharedSprite and may win before default933.',native_GUI_accepted=False)
    write('skill-pane-layer-composition/layer-extraction.json',{'authorization':'Supervisor batch9: each exact933.img and935.img one extraction; no other payloads.',
      'index_candidates_only':candidates,'source_checks':checks,'members':results,'new_pak_members_read_or_extracted':2,'shared_Sprite_payload_read':False,'934_replayed':False})
    print(json.dumps({'members':[r['decoded']for r in results],'headers':[r['header']for r in results]},ensure_ascii=False))

if __name__=='__main__':main()
