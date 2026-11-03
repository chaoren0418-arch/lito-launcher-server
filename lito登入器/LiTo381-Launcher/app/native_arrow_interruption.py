"""Release an acknowledged physical shot before its attack event is lost to hurt.

LiTo keeps the arrow on actor+0x80 until the attack-frame event. Hurt switches
that action to family+2 without consuming the arrow. Use the existing release
routine (including ownership clearing), only for a pending native bow arrow.
"""
import struct
from native_sound_extension import Code
SITE=0x4E479F
ORIGINAL=bytes.fromhex('8bcee8fab9ffff')

def plan(base,read):
    if read(SITE,len(ORIGINAL))!=ORIGINAL:raise ValueError('arrow interruption pin mismatch')
    c=Code(base);c.put(bytes.fromhex('9c60'))
    c.put(bytes.fromhex('83fb02'));c.branch(b'\x0F\x85','resume')
    c.put(bytes.fromhex('807e1015'));c.branch(b'\x0F\x84','pending')
    c.put(bytes.fromhex('807e103f'));c.branch(b'\x0F\x85','resume')
    c.label('pending');c.put(bytes.fromhex('80bec000000000'));c.branch(b'\x0F\x84','resume')
    c.put(bytes.fromhex('8b8e8000000085c9'));c.branch(b'\x0F\x84','resume')
    c.put(bytes.fromhex('81395c774f00'));c.branch(b'\x0F\x85','resume')
    c.put(bytes.fromhex('8bce'));c.call(0x4E45C0)
    c.label('resume');c.put(bytes.fromhex('619d8bce'));c.call(0x4E01A0);c.jump(SITE+len(ORIGINAL))
    return [(base,c.finish())],[(SITE,b'\xE9'+struct.pack('<i',base-SITE-5)+b'\x90'*2)]
