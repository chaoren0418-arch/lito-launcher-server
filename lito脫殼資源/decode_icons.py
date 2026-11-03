"""Strict L1 row-span RGB555 byte-layout validation for extracted LTicon members."""
from audit import *
from PIL import Image, ImageDraw
def decode_lbt(b):
    if len(b)<4:raise ValueError('Header truncated')
    dx,dy,w,h=b[:4]
    if w==0 or h==0:raise ValueError('Empty dimensions')
    pos=4;pixels=[];actual_w=w
    def byte():
        nonlocal pos
        if pos>=len(b):raise ValueError('Truncated row')
        v=b[pos];pos+=1;return v
    for y in range(h):
        spans=byte();x=0
        for _ in range(spans):
            skip=byte();count=byte()
            if skip&1:raise ValueError('Odd byte skip unsupported')
            x+=skip//2
            if x+count>512:raise ValueError('Width budget')
            for k in range(count):
                v=byte()|byte()<<8
                if v&0x8000:raise ValueError('High-bit variant unsupported')
                pixels.append((x+k,y,v))
            x+=count;actual_w=max(actual_w,x)
    if pos!=len(b):raise ValueError('Trailing bytes unsupported')
    out=bytearray(actual_w*h*4);coverage=bytearray(actual_w*h)
    for x,y,v in pixels:
        i=y*actual_w+x;coverage[i]=1;cs=[(v>>10)&31,(v>>5)&31,v&31];out[i*4:i*4+4]=bytes([(c<<3)|(c>>2)for c in cs]+[255])
    return Image.frombytes('RGBA',(actual_w,h),bytes(out)),{'offset':[dx,dy],'header_width':w,'width':actual_w,'height':h,'coverage_sha256':sha(coverage),'rgba_coverage_alpha_sha256':sha(out),'encoded_pixels':len(pixels),'zero_pixels':sum(v==0 for x,y,v in pixels),'bytes_consumed':pos,'native_offset_alpha_blit_parity':False}
def main():
    m=json.loads((ROOT/'resources/archive-manifest.json').read_text(encoding='utf-8'));out=[];images=[]
    for item in m['selected_members']:
        if item['archive']!='LTicon' or not item['name'].lower().endswith('.lbt'):continue
        raw=Path(item['decoded']['path']).read_bytes();row={'member':item['name'],'index_row':item['row'],'source':item['decoded']}
        try:
            im,details=decode_lbt(raw);p=ROOT/'resources'/'previews'/'LTicon'/(item['name']+'.png');p.parent.mkdir(exist_ok=True);im.save(p);row.update(status='layout_validated',preview=record(p),details=details);images.append((item['name'],im))
        except ValueError as e:row.update(status='unqualified',reason=str(e))
        out.append(row)
    # This is a catalogue of source pixels, not a replacement game UI.
    cols=12;cw,ch=64,64;gallery=Image.new('RGB',(cols*cw,((len(images)+cols-1)//cols)*ch),(35,35,35));draw=ImageDraw.Draw(gallery)
    for n,(name,im)in enumerate(sorted(images,key=lambda x:int(x[0].split('.')[0]))):
        x=n%cols*cw;y=n//cols*ch;gallery.paste(im,(x+4,y+2),im);draw.text((x+3,y+46),name,fill='white')
    p=ROOT/'resources'/'LTicon-contact-sheet.png';gallery.save(p)
    negative=[]
    for case,raw in [('truncated',b'\0\0\1\1\1'),('oddskip',b'\0\0\1\1\1\1\0'),('highbit',b'\0\0\1\1\1\0\1\0\x80'),('tail',b'\0\0\1\1\0\1')]:
        try:decode_lbt(raw);raise RuntimeError('Malformed sample accepted')
        except ValueError as e:negative.append({'case':case,'rejected':str(e)})
    list_item=next(x for x in m['selected_members'] if x['archive']=='LTicon' and x['name']=='LTlist.ico')
    declared=int(Path(list_item['decoded']['path']).read_bytes().strip());available={int(x['member'].split('.')[0])for x in out}
    write('resources/LTicon-decode-manifest.json',{'updated_at':stamp(),'generator':record(Path(__file__)),'format_reference':record(ROOT.parent/'lito重建版'/'LiTo381-Modern'/'src'/'modern_lito'/'inventory_icons.rs'),'scope':'All existing LTicon LBT resources independently validated against L1 fourbyteheader,rowspans,evenskip/2,LE RGB555 layout. Shared TBT routing in reference is not applied to Native LTicon. Native game alpha/offset/render parity remains open. Covered0 retained opaque in preview; uncovered transparent.','list_metadata':{'source':list_item['decoded'],'text':str(declared),'declared_range':declared,'missing_declared_LBT_ids':sorted(set(range(declared))-available),'meaning':'List count is declared range, not availability; absent IDs remain gaps, no generated replacement.'},'members':out,'layout_validated':sum(x['status']=='layout_validated'for x in out),'unqualified':sum(x['status']=='unqualified'for x in out),'negative_cases':negative,'contact_sheet':record(p)})
    print('LTicon',sum(x['status']=='layout_validated'for x in out),'layout-valid;',sum(x['status']=='unqualified'for x in out),'unqualified;4 malformed boundary cases rejected')
if __name__=='__main__':main()
