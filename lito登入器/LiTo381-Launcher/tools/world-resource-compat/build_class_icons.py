"""Create an isolated native LTicon archive, preserving all original members."""
from pathlib import Path
import hashlib,json,struct
from PIL import Image
R=Path(__file__).resolve().parent
W=R/'build/class-icons'
SOURCE=Path(r'C:\Users\CHAOREN\Desktop\新增資料夾 (2)\lito_class_icons_output')
ORIGINAL=Path('D:/lineage開發/0-客戶端/LiTo/LTicon')
def pin(p):return {'path':str(p.resolve()),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
def encode(im):
    assert im.mode=='RGBA' and im.size==(16,12)
    data=bytearray([0,0,16,12])
    for y in range(12):
        spans=[];x=0;last=0
        while x<16:
            if not im.getpixel((x,y))[3]:x+=1;continue
            start=x;pixels=[]
            while x<16 and im.getpixel((x,y))[3]:
                r,g,b,a=im.getpixel((x,y));assert a==255
                pixels.append((r>>3)<<10|(g>>3)<<5|(b>>3));x+=1
            spans.append(bytes([(start-last)*2,len(pixels)])+struct.pack('<'+'H'*len(pixels),*pixels));last=x
        data.append(len(spans));data.extend(b''.join(spans))
    return bytes(data)
def decode(raw):
    assert raw[:4]==bytes([0,0,16,12]);out=Image.new('RGBA',(16,12));pos=4
    for y in range(12):
        spans=raw[pos];pos+=1;x=0
        for _ in range(spans):
            skip,n=raw[pos:pos+2];pos+=2;assert skip%2==0;x+=skip//2
            for _ in range(n):
                v=struct.unpack_from('<H',raw,pos)[0];pos+=2;assert x<16
                channels=[(v>>s)&31 for s in (10,5,0)]
                out.putpixel((x,y),tuple((v<<3)|(v>>2) for v in channels)+(255,));x+=1
    assert pos==len(raw);return out
def main():
    W.mkdir(parents=True,exist_ok=True);out=W/'resources';out.mkdir(exist_ok=True)
    validation=json.loads((SOURCE/'validation.json').read_text('utf-8'))
    assert validation['status']=='passed'
    index=ORIGINAL.with_suffix('.idx').read_bytes();pak=ORIGINAL.with_suffix('.pak').read_bytes()
    for p in (ORIGINAL.with_suffix('.idx'),ORIGINAL.with_suffix('.pak')):
        assert pin(p)['sha256']==validation['source_sha256'][str(p)]
    count=struct.unpack_from('<I',index)[0];assert len(index)==4+28*count
    rows=[];originals={}
    for i in range(count):
        offset,name,size=struct.unpack_from('<I20sI',index,4+28*i);name=name.split(b'\0')[0]
        assert offset+size<=len(pak) and name not in originals
        originals[name]=pak[offset:offset+size]
    assert originals[b'LTlist.ico'] == b'233\r\n' if b'LTlist.ico' in originals else originals[b'ltlist.ico']==b'233\r\n'
    members=dict(originals);assets=[]
    for ci,cls in enumerate(('dragon_knight','illusionist')):
        for vi,variant in enumerate(('color','white','red','silver','gold')):
            p=SOURCE/cls/f'{cls}_{variant}.png';id=233+ci*5+vi
            expected=next(a['sha256'] for a in validation['assets'] if a['file']==p.relative_to(SOURCE).as_posix())
            assert pin(p)['sha256']==expected
            im=Image.open(p);raw=encode(im);assert decode(raw).tobytes()==im.tobytes()
            name=f'{id}.lbt'.encode();assert name not in members;members[name]=raw
            (out/name.decode()).write_bytes(raw)
            assets.append(dict(id=id,job=ci+5,variant=variant,png=pin(p),lbt=pin(out/name.decode())))
    for name in members:
        if name.lower()==b'ltlist.ico':members[name]=b'243\r\n'
    payload=bytearray();idx=bytearray(struct.pack('<I',len(members)))
    for name,value in sorted(members.items(),key=lambda r:r[0].lower()):
        idx.extend(struct.pack('<I20sI',len(payload),name,len(value)));payload.extend(value)
    base=out/'LTicon';base.with_suffix('.pak').write_bytes(payload);base.with_suffix('.idx').write_bytes(idx)
    assert all(members[n]==v for n,v in originals.items() if n.lower()!=b'ltlist.ico')
    report=dict(status='png-lbt-roundtrip-and-original-members-verified',assets=assets,
        original=[pin(ORIGINAL.with_suffix(s)) for s in ('.idx','.pak')],
        package=[pin(base.with_suffix(s)) for s in ('.idx','.pak')],
        original_members_preserved=len(originals)-1,native_count=243,source_modified=False)
    (W/'resources.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),'utf-8')
    print('PASS 10 PNG/LBT exact roundtrips; original members retained; native count 243')
if __name__=='__main__':main()
