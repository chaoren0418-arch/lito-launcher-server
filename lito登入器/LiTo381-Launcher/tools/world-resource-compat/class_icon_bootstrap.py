"""Candidate-only archive selection at the held native entry, before archive open."""
from pathlib import Path
import hashlib,json,struct,os

def install_archive(probe,handle,pid,directory):
    receipt=Path(os.environ['LITO_CLASS_ICON_RECEIPT'])
    if hashlib.sha256(receipt.read_bytes()).hexdigest()!=os.environ['LITO_CLASS_ICON_RECEIPT_SHA256']:
        raise RuntimeError('Class icon receipt changed')
    data=json.loads(receipt.read_text('utf-8'))
    for item in data['original']+data['package']:
        if hashlib.sha256(Path(item['path']).read_bytes()).hexdigest()!=item['sha256']:
            raise RuntimeError('Class icon resource changed: '+item['path'])
    base=Path(data['package'][0]['path']).with_suffix('')
    # Native 42E320/42E550 prepend the client root even for absolute paths.
    # Keep the isolated package outside the client, using a relative pathname.
    relative=os.path.relpath(base,Path(directory))
    raw=(relative+'\0').encode('cp950',errors='strict')
    if len(raw)>=240:raise RuntimeError('Class icon archive path exceeds native limit')
    address=0x42EF22
    original=bytes.fromhex('68 88 5D 50 00')
    if probe.read_process(handle,address,5)!=original:
        raise RuntimeError('Native LTicon archive open contract changed')
    remote=int(probe.kernel32.VirtualAllocEx(handle,None,len(raw),0x3000,0x04) or 0)
    if not remote:raise RuntimeError('Class icon path allocation failed')
    if probe.write_process(handle,remote,raw)!=len(raw):raise RuntimeError('Archive path write failed')
    replacement=b'\x68'+struct.pack('<I',remote)
    if probe.write_process(handle,address,replacement)!=5:raise RuntimeError('Archive route write failed')
    if probe.read_process(handle,address,5)!=replacement or probe.read_process(handle,remote,len(raw))!=raw:
        raise RuntimeError('Archive route readback failed')
    # Native initialization retains this pathname; process lifetime owns allocation.
    return {'status':'installed-before-native-archive-open','base':str(base),'native_relative_path':relative,
            'count':data['native_count'],'path_address':remote,'pid':pid,
            'original_archives_modified':False,'binary_modified':False}
