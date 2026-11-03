"""Read-only selection of existing sample and catalogue evidence."""
from audit import *
from extract_member import load_catalogue

def main():
    cat,tables=load_catalogue()
    for stem,rows in tables.items():
        found=[r for r in rows if r['name'].lower()=='91.tbt']
        if found:print(json.dumps({'archive':stem,'rows':found,'decoded_index':cat[stem]['decoded_index'],'index_source':cat[stem]['index_source'],'pak_source':cat[stem]['pak_source']},ensure_ascii=False))
    m=json.loads((ROOT/'resources/LTicon-decode-manifest.json').read_text(encoding='utf-8'))
    for r in m['members']:
        if r['member'] in ['91.lbt','106.lbt','122.lbt']:print(json.dumps(r,ensure_ascii=False))
    m=json.loads((ROOT/'skill-initialization/shared-icon-resources.json').read_text(encoding='utf-8'))
    for r in m['outputs']:
        if r['name']=='383.tbt':print(json.dumps(r,ensure_ascii=False))
if __name__=='__main__':main()
