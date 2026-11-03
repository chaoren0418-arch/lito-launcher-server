"""Variable occupancy arrays using LiTo's native coordinate/register routines.

Every array created by this adapter has an eight-byte prefix: byte length and
first direction. Cleanup/movement read this allocation metadata, never the
actor's mutable sprite ID. Install all patches atomically before actor creation.
"""
import struct
from native_sound_extension import Code

def creator(base):
    # ECX=actor, EDX=node count, EAX=first direction; returns nodes or null.
    c=Code(base)
    c.put(bytes.fromhex('55 8bec 83ec14 53 56 57 894dfc 8955f8 8945f4'))
    c.put(bytes.fromhex('6bd218 8955f0 83c208 52'));c.call(0x4F118C)
    c.put(bytes.fromhex('83c404 85c0'));c.branch(b'\x0F\x84','done')
    c.put(bytes.fromhex('8b55f0 8910 8b55f4 895004 83c008 8945ec 8bf0 33ff'))
    c.label('loop')
    c.put(bytes.fromhex('a1204f8300 8906 ff05204f8300 8bc7 0345f4 50 8d45e4 50 8b4dfc 83c12c'))
    # Extra eight bytes below locals provide coordinate return storage.
    c.call(0x4B9360)
    c.put(bytes.fromhex('8b10 895604 89560c 8b5004 895608 895610 8b55fc 895614 8bce'))
    c.call(0x4DFC80)
    c.put(bytes.fromhex('83c618 47 3b7df8'));c.branch(b'\x0F\x8C','loop')
    c.put(bytes.fromhex('8b45ec'))
    c.label('done');c.put(bytes.fromhex('5f 5e 5b 8be5 5d c3'))
    # Locals include the coordinate buffer (EBP-0x1c), so reserve 0x1c.
    data=bytearray(c.finish());data[5]=0x1C
    return bytes(data)

def constructor_hook(base,helper,table,count):
    c=Code(base);c.put(bytes.fromhex('9c 60 a1607e6400 8a0438 884622 0fbed0'))
    c.put(b'\x81\xFF'+struct.pack('<I',count));c.branch(b'\x0F\x83','legacy')
    c.put(b'\x0F\xB6\x04\x3D'+struct.pack('<I',table))
    c.put(bytes.fromhex('83f802'));c.branch(b'\x0F\x84','one')
    c.put(bytes.fromhex('83f803'));c.branch(b'\x0F\x84','three')
    c.put(bytes.fromhex('83f808'));c.branch(b'\x0F\x84','five')
    c.label('legacy');c.put(bytes.fromhex('83fa01'));c.branch(b'\x0F\x85','none')
    c.put(bytes.fromhex('ba08000000 33c0'));c.branch(b'\xE9','create')
    c.label('one');c.put(bytes.fromhex('ba01000000 b802000000'));c.branch(b'\xE9','create')
    c.label('three');c.put(bytes.fromhex('ba03000000 b802000000'));c.branch(b'\xE9','create')
    c.label('five');c.put(bytes.fromhex('ba05000000 33c0'))
    c.label('create');c.put(bytes.fromhex('8bce'));c.call(helper);c.branch(b'\xE9','store')
    c.label('none');c.put(bytes.fromhex('33c0'))
    c.label('store');c.put(bytes.fromhex('894624 61 9d'));c.jump(0x4E7C9C)
    return c.finish()

def bound_hook(base,resume):
    c=Code(base)
    # Preserve EAX and other registers; flags match CMP EDI, allocation length.
    c.put(bytes.fromhex('50 8b4624 3b78f8 58'));c.jump(resume)
    return c.finish()

def direction_hook(base):
    c=Code(base)
    # Reproduce LEA EAX,[EBP-18], PUSH direction, PUSH EAX, LEA ECX,[EBP+8].
    c.put(bytes.fromhex('8b4624 8b48fc 03cb 51 8d45e8 50 8d4d08'))
    c.jump(0x4E19BE);return c.finish()

def free_hook(base):
    c=Code(base)
    # cdecl argument lives above return address. Convert nodes to allocation.
    c.put(bytes.fromhex('836c240408'));c.jump(0x4F103C);return c.finish()

PINS={0x4E7C7A:bytes.fromhex('a1607e6400'),
      0x4DFFF3:bytes.fromhex('81ffc0000000'),
      0x4E1653:bytes.fromhex('81ffc0000000'),
      0x4E19FE:bytes.fromhex('81ffc0000000'),
      0x4E19B6:bytes.fromhex('8d45e853508d4d08'),
      0x4DFFFF:bytes.fromhex('e838100100')}

def plan(base,table,count,read_bytes):
    for address,data in PINS.items():
        if read_bytes(address,len(data))!=data:raise ValueError(f'furniture pin mismatch {address:08X}')
    blobs=[(base,creator(base)),(base+0x200,constructor_hook(base+0x200,base,table,count))]
    patches=[(0x4E7C7A,b'\xE9'+struct.pack('<i',base+0x200-0x4E7C7F))]
    for index,address in enumerate((0x4DFFF3,0x4E1653,0x4E19FE)):
        stub=base+0x400+index*0x40
        blobs.append((stub,bound_hook(stub,address+6)))
        patches.append((address,b'\xE9'+struct.pack('<i',stub-address-5)+b'\x90'))
    stub=base+0x500;blobs.append((stub,direction_hook(stub)))
    patches.append((0x4E19B6,b'\xE9'+struct.pack('<i',stub-0x4E19BB)+b'\x90'*3))
    stub=base+0x600;blobs.append((stub,free_hook(stub)))
    patches.append((0x4DFFFF,b'\xE8'+struct.pack('<i',stub-0x4E0004)))
    return blobs,patches
