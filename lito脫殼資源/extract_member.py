"""CLI: extract a row from a hash-pinned archive with explicit duplicate identity."""
from audit import *
from extract_resources import index,MAX_MEMBER,EXE
import subprocess,argparse
def load_catalogue():
    doc=json.loads((ROOT/'resources/archive-manifest.json').read_text(encoding='utf-8'))
    tables={a['stem']:index(Path(a['decoded_index']['path']).read_bytes(),a['pak_source']['bytes'])for a in doc['archives']}
    return {a['stem']:a for a in doc['archives']},tables
def extract(a,row,folder='on-demand'):
    if row['bytes']>MAX_MEMBER:raise ValueError('Member budget exceeded')
    directory=ROOT/'resources'/folder/a['stem'];directory.mkdir(parents=True,exist_ok=True)
    if not directory.resolve().is_relative_to(ROOT):raise ValueError('Output escapes ownedroot')
    p=directory/(f"row{row['row']:05}-"+row['name'])
    with Path(a['pak_source']['path']).open('rb')as f:
        if os.fstat(f.fileno()).st_size!=a['pak_source']['bytes']:raise ValueError('Container size drift')
        f.seek(row['offset']);raw=f.read(row['bytes'])
    if len(raw)!=row['bytes']:raise ValueError('Short member')
    p.write_bytes(raw);result={'namespace':a['namespace'],'archive':a['stem'],**row,'source_index':a['index_source'],'source_pak':a['pak_source'],'raw':record(p)}
    if a['codec'].startswith('L1'):
        decoded=p.with_name(p.name+'.decoded');subprocess.run([str(EXE),'payload',str(p),str(decoded)],check=True,cwd=ROOT);result['decoded']=record(decoded)
    else:result['decoded']=record(p)
    return result
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--archive',required=True);parser.add_argument('--row',type=int,required=True);args=parser.parse_args()
    catalog,tables=load_catalogue();a=catalog[args.archive]
    if not 0<=args.row<len(tables[args.archive]):raise ValueError('Row outside index')
    for key in ['index_source','pak_source']:
        if record(Path(a[key]['path']))['sha256']!=a[key]['sha256']:raise ValueError('Archive hash drift')
    row=tables[args.archive][args.row];result=extract(a,row)
    write(f"resources/on-demand/{args.archive}/row{args.row:05}-provenance.json",result)
    print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
