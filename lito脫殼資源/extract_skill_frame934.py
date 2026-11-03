"""Authorized batch8 single native IMG member extraction; no other pak payload reads."""
from audit import *
from extract_member import load_catalogue,extract

def main():
    cat,tables=load_catalogue();names={'934.img','934e.img','934c.img','934j.img','934h.img'}
    matches=[{'archive':k,'namespace':cat[k]['namespace'],'source_index':cat[k]['decoded_index'],'rows':[r for r in rows if r['name'].lower()in names]}for k,rows in tables.items()if any(r['name'].lower()in names for r in rows)]
    a=cat['LTsurf'];rows=[r for r in tables['LTsurf']if r['name'].lower()in names]
    assert rows==[{'row':1557,'name':'934.img','offset':91239391,'bytes':119654}]
    checks=[]
    for key in ['index_source','decoded_index','pak_source']:
        pin=record(Path(a[key]['path']));assert pin==a[key];checks.append({'kind':key,**pin,'match':True})
    result=extract(a,rows[0],'skill-frame934')
    assert result['raw']==result['decoded']
    raw=Path(result['raw']['path']).read_bytes()
    result.update(authorization='Batch8 supervisor authorizes only exact frame member read/extraction after static binding. LTsurf overlay is tried before baseSprite for64798C.',
      index_candidates_only=matches,source_checks=checks,new_pak_members_read_or_extracted=1,shared_Sprite_payload_read=False,
      header_first_32_hex=raw[:32].hex(),first_four_u16=list(struct.unpack_from('<4H',raw)),first_two_i16=list(struct.unpack_from('<2h',raw)),
      source_namespace_qualification='Native LTsurf priority candidate. SharedSprite row90927 is indexed fallback only; actual live load success/order not observed.',native_GUI_accepted=False)
    write('skill-pane-frame-coordinate/frame934-extraction.json',result)
    print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':main()
