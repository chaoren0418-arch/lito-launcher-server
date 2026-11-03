"""Atomic, pinned hooks for frame events, draw-list reinsertion and destruction."""
import struct
from native_sound_extension import Code

PINS={0x4E1565:bytes.fromhex('f644911204'),0x4E0200:bytes.fromhex('568bf133c0'),
      0x4DFF00:bytes.fromhex('558bec6aff')}

def plan(base,exports,read_bytes):
    for address,data in PINS.items():
        if read_bytes(address,len(data))!=data:raise ValueError(f'fade hook pin mismatch {address:08X}')
    blobs=[];patches=[]
    c=Code(base);c.put(bytes.fromhex('9c6056'));c.call(exports['FadeFrameEvent'])
    c.put(bytes.fromhex('83c404619d')+PINS[0x4E1565]);c.jump(0x4E156A);blobs.append((base,c.finish()))
    patches.append((0x4E1565,b'\xE9'+struct.pack('<i',base-0x4E156A)))
    trampoline=base+0x100;c=Code(trampoline);c.put(PINS[0x4E0200]);c.jump(0x4E0205)
    blobs.append((trampoline,c.finish()))
    stub=base+0x200;c=Code(stub);c.put(b'\x51');c.call(trampoline)
    c.put(bytes.fromhex('599c6051'));c.call(exports['FadeApply'])
    c.put(bytes.fromhex('83c404619dc3'));blobs.append((stub,c.finish()))
    patches.append((0x4E0200,b'\xE9'+struct.pack('<i',stub-0x4E0205)))
    stub=base+0x300;c=Code(stub);c.put(bytes.fromhex('9c6051'));c.call(exports['FadeForget'])
    c.put(bytes.fromhex('83c404619d')+PINS[0x4DFF00]);c.jump(0x4DFF05);blobs.append((stub,c.finish()))
    patches.append((0x4DFF00,b'\xE9'+struct.pack('<i',stub-0x4DFF05)))
    return blobs,patches
