import struct
from native_sound_extension import Code
SITE=0x43F9D5
ORIGINAL=bytes.fromhex('85c9740ea098126400')

def plan(base,normalize,read):
    if read(SITE,len(ORIGINAL))!=ORIGINAL:raise ValueError('TBT normalization hook pin mismatch')
    c=Code(base);c.put(bytes.fromhex('9c60ff75fc51'));c.call(normalize)
    c.put(bytes.fromhex('894424188b15c88e64008904b2')) # new ECX and item table[ESI]
    c.put(bytes.fromhex('619d85c9'));c.branch(b'\x0F\x84','empty')
    c.put(bytes.fromhex('a098126400'));c.jump(0x43F9DE)
    c.label('empty');c.jump(0x43F9E7)
    return [(base,c.finish())],[(SITE,b'\xE9'+struct.pack('<i',base-SITE-5)+b'\x90'*4)]
