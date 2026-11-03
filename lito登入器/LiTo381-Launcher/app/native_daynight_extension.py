"""TW SwitchingSpriteType=1 through LiTo's walk-family/frame preparation paths."""
import hashlib,struct
from pathlib import Path
import pefile
from native_sound_extension import Code

TW=Path(r'D:\lineage開發\登入器bin資源\TW13081901bin拆解\TW13081901.unpacked.exe')
TW_HASH='653da514f750d4714fde2c83e5162aeef553c876ec541fe1cfebf6970f5c62a4'

def daylight_from_tm():
    # Relocate the reviewed pure TW day/night arithmetic. It has only internal
    # relative branches once its clock lookup is replaced by a tm argument.
    raw=TW.read_bytes()
    if hashlib.sha256(raw).hexdigest()!=TW_HASH:raise ValueError('TW daylight source changed')
    image=pefile.PE(data=raw).get_memory_mapped_image()
    code=bytearray(image[0x387040:0x387163])
    assert code[0x22:0x30]==bytes.fromhex('a17c1ec30050e8939ee0ff83c404')
    assert code[-4:]==bytes.fromhex('8be55dc3')
    code[0x22:0x30]=bytes.fromhex('8b4508')+b'\x90'*11
    return bytes(code)

def selector(base,table,count,arithmetic,gmtime32):
    # ECX actor -> EAX=-1 (ordinary), 0 (day family), 11 (night family).
    c=Code(base);c.put(bytes.fromhex('0fbf4114'))
    c.put(b'\x3D'+struct.pack('<I',count));c.branch(b'\x0F\x83','ordinary')
    c.put(b'\x80\xB8'+struct.pack('<I',table)+b'\x01');c.branch(b'\x0F\x85','ordinary')
    c.put(bytes.fromhex('a1ac488300 3d00000080'));c.branch(b'\x0F\x82','timestamp')
    c.put(bytes.fromhex('2d00f86178')) # same post-2038 folding as TW590F00
    c.label('timestamp');c.put(bytes.fromhex('50 8bc4 50'));c.call(gmtime32)
    c.put(bytes.fromhex('83c408 85c0'));c.branch(b'\x0F\x84','ordinary')
    c.put(b'\x50');c.call(arithmetic);c.put(bytes.fromhex('83c404 84c0'))
    c.branch(b'\x0F\x84','night');c.put(bytes.fromhex('33c0 c3'))
    c.label('night');c.put(bytes.fromhex('b80b000000 c3'))
    c.label('ordinary');c.put(bytes.fromhex('83c8ff c3'));return c.finish()

def constructor_hook(base,select):
    c=Code(base);c.put(bytes.fromhex('9c 60'));c.call(select)
    c.put(bytes.fromhex('83f8ff'));c.branch(b'\x0F\x84','done')
    c.put(bytes.fromhex('884618 83c003 884610'))
    c.label('done');c.put(bytes.fromhex('61 9d'));c.jump(0x4E5C00)
    return c.finish()

def family_hook(base,select):
    c=Code(base);c.put(bytes.fromhex('9c 60 8bce'));c.call(select)
    c.put(bytes.fromhex('83f8ff'));c.branch(b'\x0F\x84','done')
    c.put(bytes.fromhex('89442410')) # PUSHAD saved EBX
    c.label('done');c.put(bytes.fromhex('61 9d 0fbe4618 3bc3'));c.jump(0x4E0430)
    return c.finish()

def action_hook(base,select):
    c=Code(base);c.put(bytes.fromhex('9c 60 8bce'));c.call(select)
    c.put(bytes.fromhex('83f8ff'));c.branch(b'\x0F\x84','ordinary')
    c.put(bytes.fromhex('83c003 884610'));c.branch(b'\xE9','done')
    c.label('ordinary');c.put(bytes.fromhex('885e10'))
    c.label('done');c.put(bytes.fromhex('c6461300 61 9d'));c.jump(0x4E0459)
    return c.finish()

PINS={0x4E7E91:bytes.fromhex('e86addffff'),0x4E042A:bytes.fromhex('0fbe46183bc3'),
      0x4E0452:bytes.fromhex('885e10c6461300')}

def plan(base,table,count,gmtime32,read_bytes):
    for address,data in PINS.items():
        if read_bytes(address,len(data))!=data:raise ValueError(f'day/night pin mismatch {address:08X}')
    blobs=[(base,daylight_from_tm()),(base+0x200,selector(base+0x200,table,count,base,gmtime32))]
    patches=[]
    for address,offset,emit,call in [(0x4E7E91,0x400,constructor_hook,True),
                                   (0x4E042A,0x500,family_hook,False),(0x4E0452,0x600,action_hook,False)]:
        stub=base+offset;blobs.append((stub,emit(stub,base+0x200)))
        patches.append((address,(b'\xE8' if call else b'\xE9')+struct.pack('<i',stub-address-5)+b'\x90'*(len(PINS[address])-5)))
    return blobs,patches
