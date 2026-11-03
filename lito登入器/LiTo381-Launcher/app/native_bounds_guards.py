"""Reject invalid effects without substituting another sprite or returning NULL.

Both native effect constructors return a valid object even for a rejected ID.
Its -1 sprite sentinel is consumed by the display guard, which goes straight to
the existing object removal/destructor path, before frame/target-list reads.
"""
import struct
from native_sound_extension import Code

PINS = {0x44EE4A: bytes.fromhex('668b0442884e12'),
        0x44EF4C: bytes.fromhex('668b0442884e12'),
        0x44EFA4: bytes.fromhex('8a863c010000')}


def attribute_guard(base, resume, diagnostics):
    c = Code(base); c.put(b'\x9C')
    c.put(bytes.fromhex('3b057c7e6400')); c.branch(b'\x0F\x83', 'invalid')
    c.put(bytes.fromhex('85d2')); c.branch(b'\x0F\x84', 'invalid')
    c.put(bytes.fromhex('668b0442')); c.branch(b'\xE9', 'done')
    c.label('invalid')
    c.put(bytes.fromhex('f0ff05') + struct.pack('<I', diagnostics))
    c.put(b'\xA3' + struct.pack('<I', diagnostics + 4))
    c.put(bytes.fromhex('66c74614ffff6631c0'))
    c.label('done'); c.put(bytes.fromhex('9d884e12')); c.jump(resume)
    return c.finish()


def display_guard(base, diagnostics):
    c = Code(base); c.put(bytes.fromhex('9c60'))
    c.put(bytes.fromhex('0fbf46143b057c7e6400')); c.branch(b'\x0F\x83', 'invalid')
    c.put(bytes.fromhex('0fbe4e1083f964')); c.branch(b'\x0F\x83', 'invalid')
    c.put(bytes.fromhex('69c08f00000003c16bc00c8b15704e8300'))
    c.put(bytes.fromhex('85d2')); c.branch(b'\x0F\x84', 'invalid')
    c.put(bytes.fromhex('03c283780800')); c.branch(b'\x0F\x84', 'invalid')
    c.put(bytes.fromhex('80780100')); c.branch(b'\x0F\x84', 'invalid')
    c.put(bytes.fromhex('619d') + PINS[0x44EFA4]); c.jump(0x44EFAA)
    c.label('invalid')
    c.put(bytes.fromhex('f0ff05') + struct.pack('<I', diagnostics + 8))
    c.put(bytes.fromhex('619d')); c.jump(0x44F2FE)
    return c.finish()


def plan(base, diagnostics, read_bytes):
    for address, data in PINS.items():
        if read_bytes(address, len(data)) != data:
            raise ValueError(f'effect bounds pin mismatch {address:08X}')
    blobs = []; patches = []
    for index, site in enumerate((0x44EE4A, 0x44EF4C)):
        stub = base + index * 0x100
        blobs.append((stub, attribute_guard(stub, site + len(PINS[site]), diagnostics)))
        patches.append((site, b'\xE9' + struct.pack('<i', stub - site - 5) + b'\x90' * 2))
    stub = base + 0x200
    blobs.append((stub, display_guard(stub, diagnostics)))
    patches.append((0x44EFA4, b'\xE9' + struct.pack('<i', stub - 0x44EFA9) + b'\x90'))
    return blobs, patches
