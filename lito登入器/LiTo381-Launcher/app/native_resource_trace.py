"""Bounded resource-name ring for candidate startup diagnosis only."""
import struct
from native_sound_extension import Code
SITE=0x42EA50
ORIGINAL=bytes.fromhex('558bec83ec20')
SLOTS=512
STRIDE=80

def plan(base,ring,read):
    if read(SITE,len(ORIGINAL))!=ORIGINAL:raise ValueError('resource trace pin mismatch')
    c=Code(base);c.put(bytes.fromhex('9c60b801000000'))
    c.put(bytes.fromhex('f00fc105')+struct.pack('<I',ring)) # atomic next slot
    c.put(bytes.fromhex('8bd84025ff0100006bc05005')+struct.pack('<I',ring+16))
    c.put(bytes.fromhex('8bd08b4c2424894a048b4c2418894a088b4c242c894a0c'))
    c.put(bytes.fromhex('8b7424288d7a10b93f000000'))
    c.label('copy');c.put(bytes.fromhex('8a064688074784c0'));c.branch(b'\x0F\x84','done')
    c.put(b'\x49');c.branch(b'\x0F\x85','copy')
    c.put(bytes.fromhex('c60700'))
    c.label('done');c.put(bytes.fromhex('43891a619d')+ORIGINAL);c.jump(SITE+6)
    return [(base,c.finish())],[(SITE,b'\xE9'+struct.pack('<i',base-SITE-5)+b'\x90')]

def decode(raw):
    count=struct.unpack_from('<I',raw)[0];rows=[]
    for index in range(SLOTS):
        offset=16+index*STRIDE
        seq,caller,archive,size_pointer=struct.unpack_from('<4I',raw,offset)
        if max(0,count-SLOTS)<seq<=count:
            name=raw[offset+16:offset+80].split(b'\0',1)[0].decode('cp950',errors='replace')
            rows.append(dict(sequence=seq,caller=hex(caller),archive=hex(archive),name=name))
    return sorted(rows,key=lambda r:r['sequence'])
