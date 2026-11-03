"""Candidate-only installer; caller must hold the graphics initialization gate.

All native sites are checked before any executable patch is committed. Failure
leaves the gate closed; the owning probe must terminate its private process job.
This module does not edit executables, archives, launchers, shortcuts or SQL.
"""
import hashlib
import json
import struct
from pathlib import Path

import pefile
import native_arrow_interruption as arrow_interruption
import native_bounds_guards as bounds
import native_resource_trace as resource_trace
import native_tbt_hooks as tbt
import native_daynight_extension as daynight
import native_fade_hooks as fade
import native_furniture_extension as furniture
import native_loader_hooks as loader
import native_morph_extension as morph
import native_sound_extension as sound
from native_sound_extension import Code


def resolve_export(modules, module_name, export_name, depth=0):
    if depth > 8:
        raise ValueError('export forwarder recursion')
    module = next((m for m in modules if m['name'].casefold() == module_name.casefold()
                   and int(m['base'], 16) < 0x100000000), None)
    if module is None:
        raise ValueError(f'required 32-bit module not loaded: {module_name}')
    pe = pefile.PE(module['path'])
    symbol = next((s for s in pe.DIRECTORY_ENTRY_EXPORT.symbols
                   if s.name == export_name.encode('ascii')), None)
    if symbol is None:
        raise ValueError(f'export missing: {module_name}!{export_name}')
    if symbol.forwarder:
        target, name = symbol.forwarder.decode('ascii').rsplit('.', 1)
        return resolve_export(modules, target + '.dll', name, depth + 1)
    return int(module['base'], 16) + symbol.address


def gate_code(base, release, resume, sleep, exit_process):
    """Preserve all registers/flags; wait at most 30 seconds, then exit owned game."""
    c = Code(base)
    c.put(bytes.fromhex('9c60bb b80b0000'))  # 3000 * Sleep(10)
    c.label('wait')
    c.put(b'\x83\x3D' + struct.pack('<I', release) + b'\x01')
    c.branch(b'\x0F\x84', 'ready')
    c.put(bytes.fromhex('6a0a')); c.call(sleep)
    c.put(b'\x4B'); c.branch(b'\x0F\x85', 'wait')
    c.put(b'\x68' + struct.pack('<I', 0xE0)); c.call(exit_process)
    c.put(b'\xCC')  # ExitProcess must not return
    c.label('ready'); c.put(bytes.fromhex('619dff25') + struct.pack('<I', resume))
    return c.finish()


def map_compatibility_image(raw, address):
    pe = pefile.PE(data=raw)
    if pe.FILE_HEADER.Machine != 0x14c:
        raise ValueError('compatibility image must be x86')
    if hasattr(pe, 'DIRECTORY_ENTRY_IMPORT') or hasattr(pe, 'DIRECTORY_ENTRY_TLS'):
        raise ValueError('manual-map image must have no imports or TLS initializers')
    if address != pe.OPTIONAL_HEADER.ImageBase:
        if not hasattr(pe, 'DIRECTORY_ENTRY_BASERELOC'):
            raise ValueError('compatibility image lacks relocations')
        pe.relocate_image(address)
    exports = {}
    for symbol in pe.DIRECTORY_ENTRY_EXPORT.symbols:
        if symbol.forwarder or not symbol.name:
            raise ValueError('unsupported export')
        name = symbol.name.decode('ascii').lstrip('_@').split('@')[0]
        exports[name] = address + symbol.address
    required = {'SharedConfigure', 'SharedLoad', 'SharedPostparse', 'SharedState',
                'FadeFrameEvent', 'FadeApply', 'FadeForget', 'NormalizeTbt', 'TbtRepairCount', 'TbtRejectCount'}
    if not required <= exports.keys():
        raise ValueError('missing compatibility exports')
    protection = [(address, pe.OPTIONAL_HEADER.SizeOfHeaders, 0x02)]
    for section in pe.sections:
        executable = bool(section.Characteristics & 0x20000000)
        writable = bool(section.Characteristics & 0x80000000)
        if executable and writable:
            raise ValueError('compatibility image has writable executable section')
        protection.append((address + section.VirtualAddress,
                           max(section.Misc_VirtualSize, section.SizeOfRawData),
                           0x20 if executable else (0x04 if writable else 0x02)))
    return pe.get_memory_mapped_image(), exports, protection


def install(memory, directory, trace_resources=False):
    """memory supplies alloc/read/write/protect/patch/flush/export; gate held."""
    directory = Path(directory)
    manifest = json.loads((directory / 'common_catalog_manifest.json').read_text('utf-8'))
    payload = (directory / 'common_catalog.bin').read_bytes()
    if len(payload) != manifest['payload_bytes'] or hashlib.sha256(payload).hexdigest() != manifest['payload_sha256']:
        raise ValueError('catalog payload hash/length mismatch')
    count = manifest['shared_count'] + manifest['lito_count']
    if (manifest['shared_count'], manifest['lito_count']) != (30000, 285):
        raise ValueError('unexpected catalog namespace')
    layouts = json.loads((directory / 'native-layout-patches.json').read_text('utf-8'))
    # Refuse late installation: no native Reader/table/actor may exist yet.
    for address in (0x647E90, 0x647E7C, 0x834E70):
        if memory.read(address, 4) != b'\0' * 4:
            raise ValueError(f'catalog already initialized at {address:08X}')

    native = (directory / 'native_fade_extension.dll').read_bytes()
    image_size = pefile.PE(data=native).OPTIONAL_HEADER.SizeOfImage
    image_base = memory.alloc(image_size)
    image, exports, protections = map_compatibility_image(native, image_base)
    state_rva = exports['SharedState'] - image_base
    state_reader = image[state_rva:state_rva + 6]
    if state_reader[:1] != b'\xA1' or state_reader[5:] != b'\xC3':
        raise ValueError('unrecognized SharedState diagnostic accessor')
    state_address = struct.unpack_from('<I', state_reader, 1)[0]
    tbt_statistics = {}
    for name in ('TbtRepairCount','TbtRejectCount'):
        offset = exports[name] - image_base
        reader = image[offset:offset+6]
        if reader[:1] != b'\xA1' or reader[5:] != b'\xC3':
            raise ValueError('unrecognized TBT diagnostic accessor')
        tbt_statistics[name] = hex(struct.unpack_from('<I',reader,1)[0])
    blob = memory.alloc(len(payload))
    sounds = memory.alloc(max(16, len(manifest['sound_events']) * 16))
    table_allocations = []
    def table(name, width):
        values = bytearray(count * width)
        for key, value in manifest['metadata'][name].items():
            key = int(key)
            if not 0 <= key < count or not 0 <= value < (1 << (width * 8)):
                raise ValueError(f'invalid {name} entry')
            values[key * width:(key + 1) * width] = value.to_bytes(width, 'little')
        address = memory.alloc(len(values))
        table_allocations.append((address, bytes(values)))
        return address
    morph_table = table('morph_effects', 2)
    furniture_table = table('furniture_sizes', 1)
    day_table = table('switching_types', 1)
    code_base = memory.alloc(0x7000)
    diagnostics = memory.alloc(0x1000)
    blobs = []
    patches = []
    def add(plan):
        emitted, changes = plan
        blobs.extend(emitted); patches.extend(changes)
    read = memory.read
    add(loader.plan(code_base, exports, blob, len(payload), sounds,
                    len(manifest['sound_events']), read, completion=diagnostics + 16))
    add(sound.plan(code_base + 0x1000, sounds, len(manifest['sound_events']), read))
    add(furniture.plan(code_base + 0x2000, furniture_table, count, read))
    add(daynight.plan(code_base + 0x3000, day_table, count,
                     memory.export('msvcrt.dll', '_gmtime32'), read))
    add(fade.plan(code_base + 0x4000, exports, read))
    blobs.append((code_base + 0x5000, morph.hook(code_base + 0x5000, morph_table, count)))
    patches.append(morph.patch(code_base + 0x5000, read))
    add(bounds.plan(code_base + 0x6000, diagnostics, read))
    add(tbt.plan(code_base + 0x6800, exports['NormalizeTbt'], read))
    add(arrow_interruption.plan(code_base + 0x6E00, read))
    trace_ring = None
    if trace_resources:
        trace_ring = memory.alloc(16 + resource_trace.SLOTS * resource_trace.STRIDE)
        add(resource_trace.plan(code_base + 0x6400, trace_ring, read))
    for row in layouts['patches']:
        address = int(row['address'], 16)
        before, after = bytes.fromhex(row['old']), bytes.fromhex(row['new'])
        if read(address, len(before)) != before or len(before) != len(after):
            raise ValueError(f'layout pin mismatch {address:08X}')
        patches.append((address, after))
    occupied = set()
    for address, data in patches:
        locations = set(range(address, address + len(data)))
        if occupied & locations:
            raise ValueError(f'overlapping patch at {address:08X}')
        occupied.update(locations)
    occupied = set()
    for address, data in blobs:
        if not code_base <= address < address + len(data) <= code_base + 0x7000:
            raise ValueError('stub outside allocation')
        locations = set(range(address, address + len(data)))
        if occupied & locations:
            raise ValueError('overlapping emitted stubs')
        occupied.update(locations)
    # All pins/plans are now verified. Only private allocations are written first.
    for address, data in [(image_base, image), (blob, payload), *table_allocations, *blobs]:
        memory.write(address, data)
        if memory.read(address, len(data)) != data:
            raise RuntimeError(f'private allocation verification failed {address:08X}')
    for address, size, protection in protections:
        memory.protect(address, size, protection)
    for address, data in [(blob, payload), *table_allocations]:
        memory.protect(address, len(data), 0x02)
    memory.protect(code_base, 0x7000, 0x20)
    for address, data in patches:
        memory.patch(address, data)
        if memory.read(address, len(data)) != data:
            raise RuntimeError(f'native patch verification failed {address:08X}')
    memory.flush()
    return {'status': 'installed_at_graphics_init_gate', 'candidate_only': True,
            'shared_count': 30000, 'lito_count': 285, 'patch_count': len(patches),
            'payload_sha256': manifest['payload_sha256'],
            'compatibility_sha256': hashlib.sha256(native).hexdigest(),
            'compatibility_base': hex(image_base), 'code_base': hex(code_base),
            'bounds_diagnostics': hex(diagnostics),
            'state_address': hex(state_address),
            'parser_completion_address': hex(diagnostics + 16),
            'resource_trace_address': hex(trace_ring) if trace_ring else None,
            'tbt_statistics': tbt_statistics,
            'exports': {name: hex(value) for name, value in exports.items()},
            'live_parser_verified': False}


class RemoteMemory:
    def __init__(self, probe, handle, pid):
        self.p, self.handle, self.pid = probe, handle, pid

    def alloc(self, size):
        address = int(self.p.kernel32.VirtualAllocEx(self.handle, None, size, 0x3000, 0x04) or 0)
        if not address or address + size > 0x80000000:
            raise RuntimeError('common-resource allocation failed or outside rel32 range')
        return address

    def read(self, address, size):
        return self.p.read_process(self.handle, address, size)

    def write(self, address, data):
        if self.p.write_process(self.handle, address, data) != len(data):
            raise RuntimeError('short WriteProcessMemory')

    def patch(self, address, data):
        if self.p.write_process_protected(self.handle, address, data) != len(data):
            raise RuntimeError('short protected WriteProcessMemory')

    def protect(self, address, size, protection):
        previous = self.p.wt.DWORD()
        if not self.p.kernel32.VirtualProtectEx(self.handle, self.p.ctypes.c_void_p(address),
                                               size, protection, self.p.ctypes.byref(previous)):
            raise self.p.win_error('common-resource VirtualProtectEx')

    def flush(self):
        if not self.p.kernel32.FlushInstructionCache(self.handle, None, 0):
            raise self.p.win_error('common-resource FlushInstructionCache')

    def export(self, module, name):
        return resolve_export(self.p.module_snapshot(self.pid), module, name)
