"""Bounded read-only archive indexing/extraction. Outputs confined to task root."""
from audit import *
import csv, subprocess, io, shutil
from PIL import Image
CLIENT=PROJECT/'0-客戶端'
EXE=ROOT/'codec'/'l1_decode.exe'
MAX_MEMBER=16*1024*1024
def safe_name(raw):
    name=raw.split(b'\0',1)[0]
    if not name or any(b<32 or b>126 for b in name) or b'/' in name or b'\\' in name or b':' in name or name in (b'.',b'..'):raise ValueError('Unsafe member name')
    return name.decode('ascii')
def index(data,pak_size):
    if len(data)<4:raise ValueError('truncated count')
    count=struct.unpack_from('<I',data)[0]
    if count>1000000 or len(data)!=4+28*count:raise ValueError('count/length mismatch')
    rows=[]
    for i,(offset,name,size) in enumerate(struct.iter_unpack('<I20sI',data[4:])):
        label=safe_name(name); end=name.find(b'\0')
        if end!=-1 and any(name[end:]):raise ValueError('Nonzero filename padding')
        if offset+size>pak_size:raise ValueError('range outside archive')
        rows.append({'row':i,'name':label,'offset':offset,'bytes':size})
    return rows
def decode_img(raw,keyed):
    if len(raw)<8:raise ValueError('IMG header truncated')
    w,h,flag,key=struct.unpack_from('<hhHH',raw)
    if not(0<w<=2048 and 0<h<=2048 and w*h<=1048576 and flag in [0,1]):raise ValueError('IMG variant/budget unqualified')
    if len(raw)!=8+w*h*2:raise ValueError('IMG exact pixel length mismatch')
    out=bytearray()
    for (v,) in struct.iter_unpack('<H',raw[8:]):
        if v&0x8000:raise ValueError('IMG high-bit/RGB565 variant unqualified')
        if keyed and v==key:out.extend(bytes(4))
        else:
            out.extend(((c<<3)|(c>>2)) for c in [(v>>10)&31,(v>>5)&31,v&31]);out.append(255)
    return Image.frombytes('RGBA',(w,h),bytes(out)),{'width':w,'height':h,'flag':flag,'key':key,'pixel_format':'qualified RGB555; expand5 via(v<<3)|(v>>2)','mode':'color_key' if keyed else 'opaque','rgba_sha256':sha(out)}
def selected_surf():
    names=set(['6113.img','6231.img','6232.img','6233.img','6234.img','6235.img','79.img','80.img','6201.img','6202.img','6303.img','6304.img','6314.img','7482.img'])
    sources=[]
    logs=ROOT.parent/'lito重建版'/'LiTo381-Modern'/'logs'/'continuation-20261003'
    for fname in ['r498-character-resource-mapping.json','r498-native-menu-skin-binding.json']:
        p=logs/fname
        if p.exists():
            d=json.loads(p.read_text(encoding='utf-8'));sources.append(record(p))
            names.update(x['name'] for x in d.get('members',[]) if 'name' in x)
    return names,sources
def main():
    out=ROOT/'resources'; out.mkdir(exist_ok=True)
    sources=[];members=[]; summaries=[]; batch=[]; surf,selection_sources=selected_surf()
    stems=[('native',CLIENT/'LiTo',x,x=='LTtext') for x in ['LTicon','LTtext','LTsurf','LTsprite','LTtile']]
    stems += [('shared',CLIENT,x,x=='Text') for x in ['Text','Tile','Sprite']+[f'Sprite{i:02}' for i in range(16)]]
    for namespace,base,stem,encrypted in stems:
        ip=base/(stem+'.idx');pp=base/(stem+'.pak');pak_record=record(pp);sources.extend([record(ip),pak_record])
        snap=out/'indices'/(stem+'.raw.idx');snap.parent.mkdir(exist_ok=True);snap.write_bytes(ip.read_bytes())
        decoded=snap
        if encrypted:
            decoded=out/'indices'/(stem+'.decoded.idx')
            subprocess.run([str(EXE),'idx',str(snap),str(decoded)],check=True,cwd=ROOT)
        rows=index(decoded.read_bytes(),pp.stat().st_size)
        table=out/'indices'/(stem+'.csv')
        with table.open('w',encoding='utf-8',newline='') as f:
            writer=csv.DictWriter(f,fieldnames=['row','name','offset','bytes']);writer.writeheader();writer.writerows(rows)
        counts={}
        for r in rows:counts[r['name'].lower()]=counts.get(r['name'].lower(),0)+1
        summary={'namespace':namespace,'stem':stem,'index_source':record(ip),'pak_source':pak_record,'index_snapshot':record(snap),'decoded_index':record(decoded),'codec':'L1 count plaintext; continuous body8; per-entry independent decode; short tail unchanged' if encrypted else 'plain old28','records':len(rows),'unique_names':len(counts),'ambiguous_names':sum(c>1 for c in counts.values()),'all_ranges_in_bounds':True,'index_table':record(table),'payload_bytes_total':sum(r['bytes'] for r in rows),'max_member_bytes':max((r['bytes'] for r in rows),default=0),'overlap_validation':'ranges bounded; names/duplicates retain record identity; not disjointness or interpretation proof'}
        summaries.append(summary)
        selected=[r for r in rows if stem in ['LTtext','LTicon'] or stem=='LTsurf' and r['name'].lower() in surf or stem=='Text' and r['name'].lower() in ['list.spr','text-c.tbl','string-c.tbl','spell-c.tbl']]
        with pp.open('rb') as f:
            for r in selected:
                if r['bytes']>MAX_MEMBER:raise ValueError('Selected member budget exceeded')
                f.seek(r['offset']);raw=f.read(r['bytes'])
                if len(raw)!=r['bytes']:raise ValueError('Short member read')
                path=out/'raw'/stem/(f"row{r['row']:05}-"+r['name']);path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
                item={'namespace':namespace,'archive':stem,**r,'source_index_sha256':summary['index_source']['sha256'],'source_pak_sha256':pak_record['sha256'],'raw':record(path),'encoding':'L1' if encrypted else 'plain','selection_reason':'complete LTtext/LTicon corpus' if stem in ['LTtext','LTicon'] else 'existing source-qualified consumer subset','ambiguous_name':counts[r['name'].lower()]>1}
                if encrypted:
                    dst=out/'decoded'/stem/path.name;dst.parent.mkdir(parents=True,exist_ok=True);batch.append(('payload',str(path),str(dst)));item['decoded_path']=str(dst)
                else:item['decoded_path']=str(path)
                members.append(item)
        write('resources/archive-manifest.partial.json',{'updated_at':stamp(),'archives':summaries,'members':len(members)})
        print(stem,len(rows),'indexed',len(selected),'selected',flush=True)
    tasklist=out/'decode-batch.tsv';tasklist.write_text('\n'.join('\t'.join(x) for x in batch)+'\n',encoding='utf-8')
    subprocess.run([str(EXE),'batch',str(tasklist)],check=True,cwd=ROOT)
    previews=[]
    for m in members:
        path=Path(m.pop('decoded_path'));m['decoded']=record(path);raw=path.read_bytes();ext=path.suffix.lower()
        if m['archive']=='LTtext':
            # Raw binary remains authoritative; textual view is explicitly CP950.
            try:
                view=raw.decode('cp950'); dst=out/'text-view'/m['archive']/(path.name+'.utf8.txt');dst.parent.mkdir(parents=True,exist_ok=True);dst.write_text(view,encoding='utf-8');m['text_view']={'assumed_code_page':950,'output':record(dst),'binary_authoritative':True}
            except UnicodeDecodeError:m['text_view']={'assumed_code_page':950,'status':'strict decode failed; raw decoded bytes preserved'}
        if ext in ['.img','.png']:
            try:
                if ext=='.img':im,details=decode_img(raw,m['name'].lower() not in ['6113.img'])
                else:im=Image.open(io.BytesIO(raw));im.load();details={'width':im.width,'height':im.height,'format':im.format,'rgba_sha256':sha(im.convert('RGBA').tobytes())}
                dst=out/'previews'/m['archive']/(path.name+'.png');dst.parent.mkdir(parents=True,exist_ok=True);im.save(dst);m['preview']={'output':record(dst),**details,'native_render_parity':False};previews.append(m['preview'])
            except Exception as e:m['preview']={'status':'unqualified variant; raw preserved','reason':str(e)}
    loose=[]
    for p in (CLIENT/'LiTo').glob('*'):
        if p.is_file() and p.suffix.lower() in ['.bmp','.dat']:
            dst=out/'loose'/p.name;dst.parent.mkdir(exist_ok=True);dst.write_bytes(p.read_bytes());item={'source':record(p),'copy':record(dst),'format':'BMP' if p.suffix.lower()=='.bmp' else 'raw DAT; semantic unqualified'}
            if p.suffix.lower()=='.bmp':
                im=Image.open(p);im.load();pv=out/'previews'/'loose'/(p.name+'.png');pv.parent.mkdir(exist_ok=True);im.save(pv);item['preview']={'output':record(pv),'width':im.width,'height':im.height,'rgba_sha256':sha(im.convert('RGBA').tobytes())}
            loose.append(item)
    write('resources/archive-manifest.json',{'updated_at':stamp(),'generator':record(Path(__file__)),'codec_source':record(ROOT/'codec'/'resource_cipher.rs'),'codec_executable':record(EXE),'format':'count u32LE + records(offset u32LE,nameASCII20,sizeu32LE); names casefold only for ambiguity identification','archives':summaries,'selected_members':members,'selection_sources':selection_sources,'loose_resources':loose,'proven_scope':'Exact container hashes/record indices/ranges and decoded resource bytes; IMG subset and BMP/PNG previews. No original process, protocol, sprite/til/LBT semantics or full GUI parity claimed.','remaining':['Full sprite/til/LBT frame decoding relies on separate format/consumer code','Shared index duplicates require explicit row; no first/last override','Language/codepage and UI lifecycle bindings require native source verification']})
    write('resources/validation.json',{'updated_at':stamp(),'archive_count':len(summaries),'entry_count':sum(a['records'] for a in summaries),'all_count_lengths_and_ranges_pass':True,'selected_members':len(members),'previews':len(previews),'loose':len(loose),'unknown_preview_variants':[{'archive':m['archive'],'name':m['name'],'preview':m['preview']} for m in members if m.get('preview',{}).get('status')],'inputs_unchanged':all(record(Path(s['path']))['sha256']==s['sha256'] for s in sources),'GUI_accepted':False})
    print('Extracted',len(members),'members;',len(previews),'previews; 24 archives indexed')
if __name__=='__main__':main()
