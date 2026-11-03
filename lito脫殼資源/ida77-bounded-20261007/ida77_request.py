"""Pure Python 3.8+ validation. No IDA loading and no source execution."""
import sys
sys.dont_write_bytecode = True
import hashlib
import struct
from pathlib import Path

CAPS = {'timeout_seconds': 60, 'max_functions': 8, 'max_function_bytes': 4096,
        'max_instructions_per_function': 512, 'max_xrefs_per_function': 64,
        'max_pseudocode_chars': 8192, 'max_output_bytes': 262144,
        'max_job_disk_bytes': 67108864}
DEFAULTS = dict(CAPS, timeout_seconds=30)
CORE_PLUGINS = {'idapython3.dll', 'hexrays.dll', 'plugins.cfg'}


class Refusal(ValueError):
    def __init__(self, code, detail):
        self.code, self.detail = code, detail
        super().__init__(code + ': ' + detail)


def sha(raw):
    return hashlib.sha256(raw).hexdigest().upper()


def require(value, code, detail):
    if not value:
        raise Refusal(code, detail)


def parse_pe32(raw):
    require(64 <= len(raw) <= 16 * 1024 * 1024, 'INVALID_PE32', 'File budget/header')
    require(raw[:2] == b'MZ', 'INVALID_PE32', 'DOS signature')
    nt = struct.unpack_from('<I', raw, 60)[0]
    require(nt + 24 <= len(raw) and raw[nt:nt+4] == b'PE\0\0', 'INVALID_PE32', 'NT header')
    machine, count = struct.unpack_from('<HH', raw, nt+4)
    optional_size = struct.unpack_from('<H', raw, nt+20)[0]
    opt = nt + 24
    require(machine == 0x14c and 1 <= count <= 32 and optional_size >= 96,
            'INVALID_PE32', 'Require i386 / bounded section count / PE32 optional header')
    require(opt + optional_size + count*40 <= len(raw), 'INVALID_PE32', 'Section table bounds')
    require(struct.unpack_from('<H', raw, opt)[0] == 0x10b, 'INVALID_PE32', 'PE32 magic')
    base = struct.unpack_from('<I', raw, opt+28)[0]
    size = struct.unpack_from('<I', raw, opt+56)[0]
    require(0 < size <= 64*1024*1024 and base + size <= 0x100000000,
            'INVALID_PE32', 'Image VA range')
    sections = []
    for i in range(count):
        off = opt + optional_size + 40*i
        vs, rva, rs, ro = struct.unpack_from('<IIII', raw, off+8)
        flags = struct.unpack_from('<I', raw, off+36)[0]
        require(ro+rs <= len(raw) and rva+max(vs,rs) <= size,
                'INVALID_PE32', 'Raw/image section bounds')
        sections.append({'rva':rva,'raw_bytes':rs,'raw_offset':ro,
                         'executable':bool(flags & 0x20000000)})
    return {'image_base':base,'size_of_image':size,'sections':sections}


def validate_request(request):
    require(isinstance(request,dict), 'INVALID_REQUEST', 'Object required')
    require(set(request)<={'schema','source_path','source_sha256','image_base','functions','limits','authorization_scope'},
            'INVALID_REQUEST', 'Unknown request field')
    require(request.get('schema') == 'ida77-bounded-request-v1', 'INVALID_REQUEST', 'Schema')
    require(isinstance(request.get('source_path'),str), 'INVALID_SOURCE', 'Path string required')
    path = Path(request.get('source_path', ''))
    require(path.is_absolute() and path.is_file(), 'INVALID_SOURCE', 'Absolute existing file required')
    require(64<=path.stat().st_size<=16*1024*1024, 'INVALID_PE32', 'Source file budget')
    with path.open('rb') as stream:
        raw = stream.read(16*1024*1024+1)
    require(len(raw)<=16*1024*1024, 'INVALID_PE32', 'Source grew beyond budget')
    expected = request.get('source_sha256')
    require(isinstance(expected,str) and len(expected)==64 and sha(raw)==expected.upper(),
            'HASH_MISMATCH', 'Exact input SHA256 required before any IDA process')
    pe = parse_pe32(raw)
    require(type(request.get('image_base')) is int and request['image_base']==pe['image_base'],
            'IMAGE_BASE_MISMATCH', 'Declared PE32 base must equal header')
    limits = dict(DEFAULTS)
    supplied = request.get('limits', {})
    require(isinstance(supplied,dict) and set(supplied)<=set(CAPS), 'INVALID_LIMITS', 'Unknown budget')
    limits.update(supplied)
    for k,v in limits.items():
        require(type(v) is int and 0 < v <= CAPS[k], 'INVALID_LIMITS', k)
    functions = request.get('functions')
    require(isinstance(functions,list) and functions, 'EMPTY_ALLOWLIST', 'Explicit function/RVA allowlist required')
    require(len(functions)<=limits['max_functions'], 'ITEM_LIMIT', 'Function count')
    entries=[]
    for f in functions:
        require(isinstance(f,dict) and set(f)=={'rva','end_rva'}, 'INVALID_ALLOWLIST', 'rva/end_rva only')
        start,end=f['rva'],f['end_rva']
        require(type(start) is int and type(end) is int and 0 <= start < end <= pe['size_of_image'],
                'OUT_OF_RANGE', 'Half-open function RVA range')
        require(end-start<=limits['max_function_bytes'], 'ITEM_LIMIT', 'Function byte range')
        matched=[s for s in pe['sections'] if s['executable'] and s['rva']<=start<end<=s['rva']+s['raw_bytes']]
        require(len(matched)==1, 'OUT_OF_RANGE', 'Exactly one executable raw-backed section required')
        require(not any(start<e['end_rva'] and e['rva']<end for e in entries),
                'OVERLAPPING_ALLOWLIST', 'No duplicate/overlapping ranges')
        s=matched[0];offset=s['raw_offset']+start-s['rva']
        entries.append({'rva':start,'end_rva':end,'va':pe['image_base']+start,
                        'end_va':pe['image_base']+end,'file_offset':offset,
                        'source_range_sha256':sha(raw[offset:offset+end-start])})
    require(request.get('authorization_scope')=='synthetic_tool_fixture_only',
            'SCOPE_NOT_AUTHORIZED', 'This delivery does not authorize any LITO analysis')
    return {'source_path':str(path),'source_sha256':sha(raw),'source_bytes':len(raw),
            'image_base':pe['image_base'],'entries':entries,'limits':limits,
            'authorization_scope':request['authorization_scope']}


def plugin_preflight(install):
    """Fail closed: IDAUSR/timeout alone does not disable installation plugins."""
    install=Path(install)
    plugin_dir=install/'plugins'
    require((install/'idat.exe').is_file() and plugin_dir.is_dir(),
            'INVALID_IDA_DEPENDENCY', 'IDA executable/plugins absent')
    files=sorted(p.name for p in plugin_dir.iterdir() if p.is_file())
    loadable=[x for x in files if Path(x).suffix.lower() in {'.py','.dll','.idc'}]
    disallowed=[x for x in loadable if x.casefold() not in CORE_PLUGINS]
    return {'installation':str(install),'loadable_plugins':loadable,
            'unqualified_plugins':disallowed,'qualified_for_launch':not disallowed,
            'reason':'Installed plugin scan can auto-load code not named in plugins.cfg; no runtime start if any unqualified plugin exists',
            'network_isolation_claimed':False,
            'execution_certificate':'No current launch qualification; require separately accepted isolated plugin/network/write profile'}


def decompiler_probe(importer=__import__):
    try:
        module=importer('ida_hexrays')
    except ImportError:
        return None, {'status':'DECOMPILER_MODULE_UNAVAILABLE','available':False}
    except Exception as e:
        return None, {'status':'DECOMPILER_IMPORT_ERROR','available':False,'error_type':type(e).__name__}
    try:
        if not module.init_hexrays_plugin():
            return None, {'status':'DECOMPILER_NOT_AVAILABLE','available':False}
        return module, {'status':'DECOMPILER_AVAILABLE','available':True,
                        'version':module.get_hexrays_version()}
    except Exception as e:
        return None, {'status':'DECOMPILER_INIT_ERROR','available':False,'error_type':type(e).__name__}
