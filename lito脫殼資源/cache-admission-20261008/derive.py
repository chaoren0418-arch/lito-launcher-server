"""Bounded, read-only derivation from mode1/map534's 16 sectors and four pins.
Writes only this task directory. Does not invoke Modern, decode pixels or extract assets.
"""
import bisect, collections, csv, hashlib, json, re, struct, sys
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8')
OUT = Path(__file__).resolve().parent
OWNER = OUT.parent
MODERN = OWNER.parent / 'lito重建版' / 'LiTo381-Modern'
BINDING = MODERN / 'logs/continuation-20261003/r486-map-binding.json'
BINDING_SHA = '14805BC5D26A88EEAAE3B182A4A477D30AE7B0F26212B760F1CFA29B452DCA6E'
MAX_RECORDS = 262144

def require(ok, reason):
    if not ok: raise ValueError(reason)

def sha(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as f:
        for block in iter(lambda: f.read(1 << 20), b''): h.update(block)
    return h.hexdigest().upper()

def record(path):
    p = Path(path)
    return dict(path=str(p), bytes=p.stat().st_size, sha256=sha(p))

def write(name, obj):
    (OUT / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

class Reader:
    def __init__(self, data): self.data, self.at = data, 0
    def take(self, n):
        require(0 <= n <= len(self.data)-self.at, f'truncated at {self.at} need {n}')
        p=self.at; self.at+=n
        return self.data[p:self.at]
    def value(self, fmt): return struct.unpack('<'+fmt, self.take(struct.calcsize('<'+fmt)))[0]
    def count(self, fmt, maximum):
        n=self.value(fmt); require(0 <= n <= maximum, f'count {n} at {self.at}')
        return n
    def remaining(self): return len(self.data)-self.at

def sector_parse(data, si, rows, witnesses, required):
    r=Reader(data); ranges=[]; counts={}; starts=[]
    def ref(layer, offset, x, y, depth, slot, tid):
        require(tid >= 0, 'negative object tile id')
        # Overlay/object coordinates remain raw u8; city visitor accepts them.
        row=[si,layer,offset,x,y,depth,tid,slot]
        rows.writerow(row)
        required[tid].add(slot)
        witnesses.setdefault((tid,slot),row)
    start=r.at
    for i in range(8192):
        off=r.at; slot=r.value('B'); tid=r.value('H'); r.take(1)
        ref(1,off,i%128,i//128,-1,slot,tid)
    ranges.append([start,r.at]); counts['floor']=8192
    start=r.at; n=r.count('H',MAX_RECORDS); counts['overlays']=n
    for _ in range(n):
        off=r.at; x=r.value('B'); y=r.value('B'); slot=r.value('B'); tid=r.value('H'); r.take(1)
        ref(2,off,x,y,0,slot,tid)
    ranges.append([start,r.at]); start=r.at; r.take(4096*4); ranges.append([start,r.at])
    start=r.at; n=r.count('i',65536); counts['object_groups']=n; total=0
    for _ in range(n):
        r.take(2); m=r.count('H',MAX_RECORDS); total+=m
        require(total<=MAX_RECORDS,'object total limit')
        for _ in range(m):
            off=r.at; x=r.value('B'); y=r.value('B'); depth=r.value('B'); slot=r.value('B'); tid=r.value('h'); r.take(1)
            ref(4,off,x,y,depth,slot,tid)
    counts['objects']=total; ranges.append([start,r.at])
    require(8192+counts['overlays']+total<=MAX_RECORDS,'city per-sector references limit')
    for layer in (5,6,7,8):
        if not r.remaining(): break
        start=r.at
        if layer in (5,6):
            n=r.count('i',MAX_RECORDS); r.take(n*(5 if layer==5 else 4))
        elif layer==7:
            n=r.count('H',MAX_RECORDS)
            for _ in range(n):
                size=r.value('B'); r.take(size); x=r.value('B'); y=r.value('B'); r.take(6)
                require(x<128 and y<64,'portal coordinate')
                if r.remaining() and 100<=r.data[r.at]<=102: r.take(1)
        else:
            packed=r.value('H'); n=packed & 0x7fff; r.take(n*(10 if packed&0x8000 else 6))
        counts[f'layer{layer}']=n; ranges.append([start,r.at])
    require(r.remaining()==0,'unknown S32 trailing bytes')
    return dict(sector_index=si,counts=counts,layer_byte_ranges=ranges,parsed_bytes=r.at)

def index_parse(pin, pak):
    data=Path(pin['path']).read_bytes(); r=Reader(data); n=r.count('I',250000)
    require(n>0 and len(data)==4+28*n,'index count/size')
    entries={}
    for i in range(n):
        off=r.at; pos=r.value('I'); raw=r.take(20); size=r.value('I')
        name, sep, padding=raw.partition(b'\0')
        require(not sep or not any(padding),'index nonzero padding')
        require(re.fullmatch(rb'[A-Za-z0-9._-]+',name) is not None,'index name charset')
        key=name.decode('ascii').lower()
        require(key not in entries,'duplicate member')
        require(size<=8<<20 and pos+size<=pak['bytes'],'member range/limit')
        entries[key]=dict(index_row=i,index_offset=off,pak_offset=pos,bytes=size)
    return entries

def validate_block(block):
    r=Reader(block); kind=r.value('B')
    if kind in (0,1,8,9,16,17):
        require(len(block)==577,'diamond shape'); r.take(576)
    else:
        require(kind in (2,3,6,7,18,19,22,23,34,35),'unsupported kind')
        x=r.value('B'); y=r.value('B'); width=r.value('B'); height=r.value('B')
        require(x+width<=24 and y+height<=24,'RLE rectangle')
        for _ in range(height):
            cursor=x
            for _ in range(r.value('B')):
                skip=r.value('B'); count=r.value('B'); require(skip%2==0,'odd skip')
                cursor+=skip//2; require(cursor<=x+width and cursor+count<=x+width,'RLE run range')
                r.take(count*2); cursor+=count
    require(not r.remaining(),'tile trailing bytes')
    return kind

def main():
    require(sha(BINDING)==BINDING_SHA,'binding source drift')
    b=next(x for x in json.loads(BINDING.read_text(encoding='utf-8'))['bindings'] if x['mode']==1 and x['map_id']==534)
    require(len(b['sectors'])==16 and len(b['archives'])==4,'bounded source scope')
    pins=[s['pin'] for s in b['sectors']]+b['archives']
    for pin in pins:
        require(Path(pin['path']).stat().st_size==pin['bytes'] and sha(pin['path'])==pin['sha256'].upper(),f'pin mismatch {pin["path"]}')
    source_paths=['map.rs','native_resources.rs','tile.rs','native_city_resources.rs','native_city_tile_cache.rs','world_depth.rs','world_map.rs']
    sources=[record(MODERN/'src/modern_lito'/p) for p in source_paths]
    required=collections.defaultdict(set); witnesses={}; sectors=[]
    with (OUT/'references.csv').open('w',newline='',encoding='utf-8') as f:
        rows=csv.writer(f); rows.writerow(['sector_index','layer','record_offset','x','y','depth','tile_id','slot'])
        for si,s in enumerate(b['sectors']):
            parsed=sector_parse(Path(s['pin']['path']).read_bytes(),si,rows,witnesses,required)
            parsed.update(x=s['x'],y=s['y'],source=s['pin']); sectors.append(parsed)
    indexes=[index_parse(b['archives'][0],b['archives'][1]),index_parse(b['archives'][2],b['archives'][3])]
    members=[]; slots=[]; kind_counts=collections.Counter(); encoded=0; aliases=0
    with Path(b['archives'][1]['path']).open('rb') as pak:
        for tid,selected in sorted(required.items()):
            name=f'{tid}.til'; require(name in indexes[0],f'missing {name}')
            entry=indexes[0][name]; require(entry['bytes']>0,'empty member')
            pak.seek(entry['pak_offset']); data=pak.read(entry['bytes']); encoded+=len(data)
            require(len(data)==entry['bytes'],'short member read')
            r=Reader(data); count=r.count('I',16384); require(count>0,'zero tile count')
            offsets=[r.value('I') for _ in range(count)]; end=r.value('I'); data_start=r.at
            require(end==len(data)-data_start and all(o<=end for o in offsets),'TIL body range')
            if end in offsets:
                first=offsets.index(end); require(first>0 and all(o==end for o in offsets[first:]),'nonterminal empty slots')
            boundaries=sorted(set(o for o in offsets if o<end)); require(boundaries and boundaries[0]==0,'TIL prefix')
            boundaries.append(end); used=[]
            member_sha=hashlib.sha256(data).hexdigest().upper()
            for slot in sorted(selected):
                require(slot<count and offsets[slot]<end,'selected missing/empty slot')
                start=offsets[slot]; stop=boundaries[bisect.bisect_right(boundaries,start)]
                block=data[data_start+start:data_start+stop]; kind=validate_block(block); kind_counts[kind]+=1; used.append(start)
                slots.append(dict(tile_id=tid,slot=slot,first_sector_reference=witnesses[(tid,slot)],til_table_offset=4+slot*4,member_block_range=[data_start+start,data_start+stop],pak_block_range=[entry['pak_offset']+data_start+start,entry['pak_offset']+data_start+stop],block_sha256=hashlib.sha256(block).hexdigest().upper(),kind=kind,rgba_bytes=2304))
            aliases+=len(used)-len(set(used))
            members.append(dict(tile_id=tid,name=name,**entry,sha256=member_sha,total_slots=count,data_start=data_start,selected_slots=sorted(selected)))
    n=sum(len(s) for s in required.values()); demand=n*2304; cap=16<<20; ceiling=64<<20
    total_refs=sum(s['counts']['floor']+s['counts']['overlays']+s['counts']['objects'] for s in sectors)
    summary=dict(mode=1,map_id=534,sectors=len(sectors),required_members=len(members),qualified_slots=n,source_references=total_refs,non_floor_references=total_refs-16*8192,encoded_source_bytes_validated=encoded,decoded_bytes_per_slot=2304,whole_required_decoded_bytes=demand,cache_bytes=cap,decoded_limit_bytes=ceiling,current_predicate_paged=demand>ceiling,required_cache_admission_paged=demand>cap,maximum_resident_slots=cap//2304,maximum_resident_slot_bytes=(cap//2304)*2304,selected_alias_slots=aliases,selected_kind_counts=dict(sorted(kind_counts.items())),archive_index_rows=[len(i) for i in indexes])
    require(cap<demand<ceiling,'expected admission witness inequality fails')
    require(encoded<=32<<20,'encoded source limit')
    write('input-sources.json',dict(binding=record(BINDING),sectors=b['sectors'],archives=b['archives'],decode_and_consumer_sources=sources,canonical_sha256_declared=b['canonical_sha256'],canonical_geometry_recomputed=False,va_rva='N/A: raw S32/IDX/PAK bytes and Rust text; no PE mapping claim'))
    write('sector-parse.json',sectors); write('members.json',members); write('qualified-slots.json',slots); write('arithmetic.json',summary)
    print(json.dumps(summary,sort_keys=True))
    print('PASS: all 20 raw input pins matched; complete sectors parsed; each required member/selected block structurally validated; no pixel/runtime/parity claim')

if __name__=='__main__':
    try: main()
    except Exception as error:
        write('failure.json',dict(error_type=type(error).__name__,error=str(error)))
        raise
