"""Replace only the embedded base Reader; retain native LT archive loading."""
import struct
from native_sound_extension import Code

PINS={0x4D3FF0:bytes.fromhex('558bec83ec08'),0x43EC56:bytes.fromhex('e8e5f0ffff')}

def plan(base,exports,blob,blob_size,sounds,sound_count,read_bytes,completion=None):
    for address,data in PINS.items():
        if read_bytes(address,len(data))!=data:raise ValueError(f'catalog loader pin mismatch {address:08X}')
    c=Code(base);c.put(bytes.fromhex('9c60'))
    c.call(exports['SharedState']);c.put(bytes.fromhex('85c0'));c.branch(b'\x0F\x85','configured')
    for value in (sound_count,sounds,blob_size,blob):c.put(b'\x68'+struct.pack('<I',value))
    c.call(exports['SharedConfigure'])
    c.label('configured');c.put(bytes.fromhex('619d'));c.jump(exports['SharedLoad'])
    blobs=[(base,c.finish())]
    patches=[(0x4D3FF0,b'\xE9'+struct.pack('<i',base-0x4D3FF5)+b'\x90')]
    stub=base+0x100;c=Code(stub);c.put(bytes.fromhex('9c60'));c.call(exports['SharedPostparse'])
    c.put(bytes.fromhex('85c0'));c.branch(b'\x0F\x84','failed')
    c.put(bytes.fromhex('619d'));c.call(0x43DD40);c.jump(0x43EC5B)
    c.label('failed');c.put(bytes.fromhex('619d'));c.jump(0x43ECCF)
    blobs.append((stub,c.finish()))
    patches.append((0x43EC56,b'\xE9'+struct.pack('<i',stub-0x43EC5B)))
    if completion is not None:
        original=bytes.fromhex('b0015f5e5b')
        if read_bytes(0x43ECBC,5)!=original:raise ValueError('parser completion pin mismatch')
        stub=base+0x200;c=Code(stub)
        c.put(bytes.fromhex('c705')+struct.pack('<II',completion,1)+original);c.jump(0x43ECC1)
        blobs.append((stub,c.finish()))
        patches.append((0x43ECBC,b'\xE9'+struct.pack('<i',stub-0x43ECC1)))
    return blobs,patches
