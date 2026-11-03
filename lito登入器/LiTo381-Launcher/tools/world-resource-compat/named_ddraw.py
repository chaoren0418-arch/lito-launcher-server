"""Load the LiTo-only proxy at the existing held WinMain gate, before graphics."""
from pathlib import Path
import ctypes,struct,hashlib
from ctypes import wintypes as W
import pefile

def validate_references(references):
    actual={(r['kind'],r['address'],r['export']) for r in references}
    # The unpacker emits its export jump with or without a leading NOP.
    variants=[{('pointer',0x4f7018,'DirectDrawCreate'),('relative',slot,'DirectDrawCreate')}
              for slot in (0x4efe3f,0x4efe40)]
    if actual not in variants or len(references)!=2:
        raise RuntimeError('Native DirectDraw reference contract changed: '+repr(actual))

def install(probe,handle,pid,directory):
    from shared_bootstrap import resolve_export
    path=Path(directory)/'litoddraw.dll'
    if (Path(directory)/'ddraw.dll').exists():
        raise RuntimeError('Old client ddraw.dll must be removed before named loading')
    expected=__import__('os').environ['LITO_NAMED_DDRAW_SHA256']
    if hashlib.sha256((Path(directory)/'LiTo.bin').read_bytes()).hexdigest()!='ce6ee68ada8fd37e6c096b1a27e46cb6888f1179f85defb5007ac60287360292':
        raise RuntimeError('Native LiTo identity changed')
    if hashlib.sha256(path.read_bytes()).hexdigest()!=expected:
        raise RuntimeError('Named LiTo DLL identity changed')
    modules=probe.module_snapshot(pid)
    system=next(m for m in modules if m['name'].lower()=='ddraw.dll')
    if Path(system['path']).parent.resolve()==Path(directory).resolve():
        raise RuntimeError('Local generic DirectDraw proxy still loaded')
    names=[s.name.decode('ascii') for s in pefile.PE(str(path)).DIRECTORY_ENTRY_EXPORT.symbols
           if s.name and s.name.startswith(b'DirectDraw')]
    original={resolve_export(modules,'ddraw.dll',n):n for n in names}
    # Read-only plan first: the unpacker resolves imports into native calls and
    # thunks. Only references proven to target an exact system export qualify.
    image=probe.read_process(handle,0x400000,0x6c4000)
    if len(image)!=0x6c4000:raise RuntimeError('Incomplete unpacked LiTo image')
    references=[]
    for address,name in original.items():
        needle=struct.pack('<I',address);start=0
        while True:
            i=image.find(needle,start)
            if i<0:break
            references.append({'kind':'pointer','address':0x400000+i,'target':address,'export':name})
            start=i+4
    # Direct E8/E9 references use relative operands, never the absolute value.
    for i in range(0x1000,len(image)-5):
        if image[i] not in (0xe8,0xe9):continue
        target=(0x400000+i+5+struct.unpack_from('<i',image,i+1)[0])&0xffffffff
        if target in original:
            references.append({'kind':'relative','address':0x400000+i+1,'target':target,'export':original[target]})
    if not references:raise RuntimeError('No qualified DirectDraw references at held native entry')
    # Keep only aligned pointer slots; instruction operands are covered above.
    references=[r for r in references if r['kind']=='relative' or r['address']%4==0]
    if not any(r['export'] in ('DirectDrawCreate','DirectDrawCreateEx') for r in references):
        raise RuntimeError('No DirectDraw creation reference found: '+str(references))
    validate_references(references)
    jump=next(r['address'] for r in references if r['kind']=='relative')-0x400000-1
    if image[jump]!=0xe9:raise RuntimeError('Expected native DirectDraw tail jump')
    raw=(str(path.resolve())+'\0').encode('utf-16le')
    remote=int(probe.kernel32.VirtualAllocEx(handle,None,len(raw),0x3000,0x04) or 0)
    if not remote:raise RuntimeError('Named DLL path allocation failed')
    thread=None;done=False
    try:
        if probe.write_process(handle,remote,raw)!=len(raw):raise RuntimeError('DLL path write failed')
        load=resolve_export(modules,'kernel32.dll','LoadLibraryW')
        thread=probe.kernel32.CreateRemoteThread(handle,None,0,ctypes.c_void_p(load),ctypes.c_void_p(remote),0,None)
        if not thread:raise RuntimeError('Named DLL loader thread failed')
        if probe.kernel32.WaitForSingleObject(thread,5000)!=0:raise RuntimeError('Named DLL loader timed out')
        done=True;exitcode=W.DWORD()
        if not probe.kernel32.GetExitCodeThread(thread,ctypes.byref(exitcode)) or not exitcode.value:
            raise RuntimeError('LoadLibraryW(litoddraw.dll) failed')
        modules=probe.module_snapshot(pid)
        loaded=next(m for m in modules if m['name'].lower()=='litoddraw.dll')
        if Path(loaded['path']).resolve()!=path.resolve():raise RuntimeError('Wrong named DLL loaded')
        for ref in references:
            to=resolve_export(modules,'litoddraw.dll',ref['export'])
            old=struct.pack('<I',ref['target']) if ref['kind']=='pointer' else struct.pack('<i',ref['target']-ref['address']-4)
            new=struct.pack('<I',to) if ref['kind']=='pointer' else struct.pack('<i',to-ref['address']-4)
            if probe.read_process(handle,ref['address'],4)!=old:raise RuntimeError('DirectDraw reference changed')
            if probe.write_process(handle,ref['address'],new)!=4:raise RuntimeError('DirectDraw route write failed')
            if probe.read_process(handle,ref['address'],4)!=new:raise RuntimeError('DirectDraw route verify failed')
        return {'status':'installed-before-WinMain','module':loaded,'system_module':system,
                'references':references,'dll_sha256':expected,'executable_modified':False}
    finally:
        if thread:probe.kernel32.CloseHandle(thread)
        if done:probe.kernel32.VirtualFreeEx(handle,ctypes.c_void_p(remote),0,0x8000)
