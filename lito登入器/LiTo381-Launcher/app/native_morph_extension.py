"""Emit TW MorphEffect through LiTo's original effect constructor/display path."""
import struct
from native_sound_extension import Code

SITE=0x4A70EE
ORIGINAL=bytes.fromhex('e86d100400')

def hook(base,effects,count):
    c=Code(base)
    c.call(0x4E8160) # original object lookup; preserve its return for the caller
    c.put(b'\x9C\x60\x8B\xF0\x85\xF6');c.branch(b'\x0F\x84','done')
    c.put(b'\x0F\xBF\x7D\xF8\x85\xFF');c.branch(b'\x0F\x88','done')
    c.put(b'\x81\xFF'+struct.pack('<I',count));c.branch(b'\x0F\x83','done')
    c.put(b'\x0F\xB7\x3C\x7D'+struct.pack('<I',effects))
    c.put(b'\x85\xFF');c.branch(b'\x0F\x84','done')
    c.put(b'\x81\xFF'+struct.pack('<I',count));c.branch(b'\x0F\x83','done')
    c.put(b'\x68\x40\x01\0\0');c.call(0x4F118C)
    c.put(b'\x83\xC4\x04\x85\xC0');c.branch(b'\x0F\x84','done')
    # Same ctor arguments as native S_EFFECT and TW S_POLYMORPH: no bound ID,
    # effect gfx, actor heading, mode0, actor world position, two zero options.
    c.put(b'\x6A\0\x6A\0\xFF\x76\x30\xFF\x76\x2C\x6A\0')
    c.put(b'\x0F\xBE\x56\x11\x52\x57\x6A\0\x8B\xC8')
    c.call(0x44EDE0)
    c.put(b'\x85\xC0');c.branch(b'\x0F\x84','done')
    c.put(b'\x8B\xC8\x8B\x10\xFF\x52\x04') # original virtual display
    c.label('done');c.put(b'\x61\x9D\xC3')
    return c.finish()

def patch(base,read_bytes):
    if read_bytes(SITE,len(ORIGINAL))!=ORIGINAL:raise ValueError('polymorph lookup call pin mismatch')
    return SITE,b'\xE8'+struct.pack('<i',base-(SITE+5))
