"""Bounded raw LTtext witness for map534 route identity missing from geometry SHA.
Independent bitwise implementation of the source-qualified L1 resource format.
Each bounded input is read once; its SHA describes exactly the parsed bytes.
No old decoder executable/tests or Modern binary runs. No whole asset extraction.
"""
import hashlib,json,re,struct,sys
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
OUT=Path(__file__).resolve().parent;OWNER=OUT.parent;MODERN=OWNER.parent/'lito重建版/LiTo381-Modern'
BINDING=MODERN/'logs/continuation-20261003/r486-map-binding.json'
def require(ok,why):
 if not ok:raise ValueError(why)
def digest(raw):return hashlib.sha256(raw).hexdigest().upper()
def read_pin(p,expected=None,maximum=1<<20):
 p=Path(p);raw=p.read_bytes();require(len(raw)<=maximum,'input bound')
 actual=dict(path=str(p),bytes=len(raw),sha256=digest(raw))
 if expected:require(actual['bytes']==expected['bytes'] and actual['sha256']==expected['sha256'].upper(),f'pin mismatch {p}')
 return raw,actual
def put(name,obj):(OUT/name).write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def constants(text,name,count):
 m=re.search(r'const '+name+r'\s*:[^=]+?=\s*\[(.*?)\];',text,re.S)
 require(m is not None,'cipher constant absent')
 values=[int(s,0) for s in re.findall(r'0x[0-9a-fA-F]+|\b\d+\b',m.group(1))]
 require(len(values)==count,f'{name} length');return values
def permute(word,width,dest):
 value=0
 for bit,target in enumerate(dest):
  if word & (1 << (width-1-bit)):value|=1 << (width-1-target)
 return value
def cipher(raw,initial,final,round_dest,keys,sbox):
 def round_function(word,round):
  a=(word>>24)&255;b=(word>>16)&255;c=(word>>8)&255;d=word&255
  e=[(d<<7|((a&249|a>>2&6)>>1))&255,(((a&1|a<<2)<<3)|((b>>2|b&135)>>3))&255,(c>>7|((b&31|(b&248)<<2)<<1))&255,(b<<7|((c&249|c>>2&6)>>1))&255,(((c&1|c<<2)<<3)|((d>>2|d&135)>>3))&255,(a>>7|((d&31|(d&248)<<2)<<1))&255]
  e=[v^keys[round*6+i] for i,v in enumerate(e)]
  six=[e[0]>>2,((e[0]&3)<<4)|(e[1]>>4),((e[1]&15)<<2)|(e[2]>>6),e[2]&63,e[3]>>2,((e[3]&3)<<4)|(e[4]>>4),((e[4]&15)<<2)|(e[5]>>6),e[5]&63]
  substituted=0
  for i,v in enumerate(six):substituted=(substituted<<4)|sbox[i*64+v]
  return permute(substituted,32,round_dest)
 result=bytearray(raw)
 for offset in range(0,len(raw)//8*8,8):
  state=permute(int.from_bytes(raw[offset:offset+8],'big'),64,initial);left=state>>32;right=state&0xffffffff
  for r in range(15,-1,-1):left,right=right,left^round_function(right,r)
  result[offset:offset+8]=permute((right<<32)|left,64,final).to_bytes(8,'big')
 return bytes(result)

def main():
 binding_raw,binding_pin=read_pin(BINDING,dict(bytes=208039,sha256='14805BC5D26A88EEAAE3B182A4A477D30AE7B0F26212B760F1CFA29B452DCA6E'))
 binding=json.loads(binding_raw);index,index_pin=read_pin(binding['LTtext_index']['path'],binding['LTtext_index']);pak,pak_pin=read_pin(binding['LTtext_pak']['path'],binding['LTtext_pak'])
 cipher_raw,cipher_pin=read_pin(MODERN/'src/modern_lito/resource_cipher.rs');cipher_text=cipher_raw.decode('utf-8')
 args=[constants(cipher_text,'INITIAL_BIT_DEST',64),constants(cipher_text,'FINAL_BIT_DEST',64),constants(cipher_text,'ROUND_BIT_DEST',32),constants(cipher_text,'ROUND_KEYS',96),constants(cipher_text,'SBOXES',512)]
 require(len(index)>=4,'index header');count=struct.unpack_from('<I',index)[0];require(0<count<=100000 and len(index)==4+28*count,'exact encrypted old28 index')
 decoded_index=index[:4]+cipher(index[4:],*args);selected=[]
 for i in range(count):
  row=decoded_index[4+28*i:4+28*(i+1)];offset,name,size=struct.unpack('<I20sI',row)
  require(size<=8<<20 and offset+size<=len(pak),'index member range')
  if name.split(b'\0')[0].lower()==b'ltmapstr-k.tbl':selected.append((i,offset,size,row))
 require(len(selected)==1 and selected[0][2]>0,'unique nonempty catalog member')
 i,offset,size,row=selected[0];encoded=pak[offset:offset+size];decoded=cipher(encoded,*args)
 expected=binding['table_decoded'];require(len(decoded)==expected['bytes'] and digest(decoded)==expected['sha256'],'decoded table declaration mismatch')
 require(len(decoded)<=8192 and b'\0' not in decoded,'decoded table bounds/NUL')
 text=decoded.decode('utf-8');lines=decoded.splitlines(keepends=True);declared=int(lines[0]);require(0<declared<=1000 and len(lines)-1>=declared,'declared row count')
 require(all(not line.rstrip(b'\r\n') for line in lines[declared+1:]),'nonempty trailing table rows')
 matches=[];byte_at=len(lines[0]);valid_categories={'petrace':0,'deathmatch':1,'questbattle':2,'siegewar':3,'timeattack':4,'honormatch':5,'town':6}
 for source_row,line in enumerate(lines[1:declared+1]):
  content=line.rstrip(b'\r\n');fields=content.decode('utf-8').split('#')
  if len(fields)>=3 and fields[0]=='534':
   category=fields[1].lower();require(category in valid_categories,'unknown category')
   matches.append(dict(source_row=source_row,table_byte_range=[byte_at,byte_at+len(content)],line_ending_hex=line[len(content):].hex().upper(),raw_line=content.decode('utf-8'),raw_line_hex=content.hex().upper(),map_id=534,raw_category=fields[1],modern_source_policy_mode=valid_categories[category],name=fields[2],extra_fields=fields[3:]))
  byte_at+=len(line)
 require(len(matches)==1,'map534 row unique');target=matches[0]
 route=next(x for x in binding['catalog40'] if x['mode']==1 and x['map_id']==534)
 for k in ['source_row','raw_line','map_id','name']:require(target[k]==route[k],f'catalog route differs {k}')
 require(target['modern_source_policy_mode']==route['mode'],'mode route mismatch')
 original_raw,original_pin=read_pin(binding['catalog_source']['path'],binding['catalog_source'])
 original=json.loads(original_raw);authority=[r for r in original['rows'] if r['mode']==1 and r['logical_map']==534];require(len(authority)==1,'exact authority row')
 plan=next(x for x in binding['bindings'] if x['mode']==1 and x['map_id']==534)
 exact=authority[0]['native_sector_files'];require(len(exact)==len(plan['sectors'])==16,'complete sector authority count')
 for s in plan['sectors']:
  counterpart=[x for x in exact if Path(x['path']).name==Path(s['pin']['path']).name];require(len(counterpart)==1,'sector authority unique')
  require(counterpart[0]['bytes']==s['pin']['bytes'] and counterpart[0]['sha256']==s['pin']['sha256'],'sector authority byte identity')
 policy_raw,policy_pin=read_pin(MODERN/'src/modern_lito/native_offline_map_flow.rs');policy_text=policy_raw.decode('utf-8')
 require('"deathmatch"=>1' in policy_text,'source category policy drift')
 loader_raw,loader_pin=read_pin(MODERN/'src/modern_lito/native_multisector_resources.rs')
 index_offset=4+28*i;block_start=4+(28*i//8)*8;block_end=min(len(index),4+((28*(i+1)+7)//8)*8)
 result=dict(identity=dict(mode=1,map_id=534,name=target['name']),member=dict(record_index=i,decoded_index_record_range=[index_offset,index_offset+28],encrypted_index_covering_block_range=[block_start,block_end],decoded_index_row_hex=row.hex().upper(),pak_offset=offset,bytes=size,encoded_sha256=digest(encoded),decoded_sha256=digest(decoded)),target_row=target,declared_table_rows=declared,archive_index_rows=count,authority_sector_count=16,canonical_geometry_sha256_declared=plan['canonical_sha256'],mode_is_not_encoded_in_canonical=True)
 put('inputs.json',dict(binding=binding_pin,index=index_pin,pak=pak_pin,original_authority_manifest=original_pin,cipher_source=cipher_pin,category_policy_source=policy_pin,whole_plan_loader_source=loader_pin,same_snapshot_rule='SHA and parse use the same in-memory byte snapshot for every bounded input; encrypted member is sliced from the hashed PAK snapshot.',va_rva='N/A raw resource data and Rust source-policy text; no original PE cipher/mode function mapping is claimed.'))
 put('route-witness.json',result)
 put('scope.json',dict(qualifies='Modern static LTtext archive/category route and exact16-sector authority linkage for mode1/map534, independent of canonical geometry hash.',unknowns=['L1 cipher implementation is source-qualified existing consumer format, not newly mapped original PE cipher function.', 'Numeric deathmatch mode1 is checked Modern source policy; original wire/room/match admission semantics are not established.', 'Extra minimap/xseg/yseg/xPos/yPos text fields remain raw catalog strings; no spawn/camera/network semantics inferred.', 'Original authority manifest is pinned existing resource enumeration; this task checks exact list linkage, not a new native full-world scope.', 'Server external text CSV map parsing has a separate resource format and is not qualified by this S32/LTtext route.']))
 print(json.dumps(result,sort_keys=True));print('PASS independent bounded LTtext decode and exact map534 route witness; same-byte SHA/parse snapshots; static consumer policy only')
if __name__=='__main__':main()
