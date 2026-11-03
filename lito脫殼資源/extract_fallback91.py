"""Authorized single-member fallback extraction; never extract another member."""
from audit import *
from extract_member import load_catalogue,extract

def main():
    cat,tables=load_catalogue();a=cat['Sprite'];row=tables['Sprite'][90223]
    assert row=={'row':90223,'name':'91.tbt','offset':903342224,'bytes':403}
    checks=[]
    for key in ['index_source','decoded_index','pak_source']:
        pin=record(Path(a[key]['path']));assert pin==a[key];checks.append({'kind':key,**pin,'match':True})
    result=extract(a,row,'graphics-fallback91')
    assert result['raw']==result['decoded'] and result['decoded']['bytes']==403
    result.update(authorization='Supervisor supplemental scope: necessary single Sprite/91.tbt extraction allowed;5 actual samples total. No other archive member read or extracted.',source_checks=checks,new_archive_members_extracted=1)
    write('graphics-parity/fallback91-extraction.json',result)
    print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
