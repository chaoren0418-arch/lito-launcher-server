"""Read-only seven-class resource audit against the retained TW memory evidence."""
from pathlib import Path
import hashlib, io, json, re, struct
from PIL import Image

L=Path(__file__).resolve().parents[2]
CLIENT=Path(r'D:\lineage開發\0-客戶端')
REFERENCE=Path(r'D:\lineage開發\經典版UI資源\證據原檔\TW13081901.memory.bin')
reference=REFERENCE.read_bytes()
source=(L/'native/class_compat.h').read_text('utf-8')
block=source.split('ExtraPortraits[4][4] = {',1)[1].split('\n};',1)[0]
actual=[int(s) for s in re.findall(r'-?\d+',block)]
expected=[]
for address in (0x96F130,0x96F0D0,0x96F1F0,0x96F190):
    expected.extend(struct.unpack_from('<24i',reference,address-0x400000))
assert actual==expected,'Portrait descriptor differs from reference client'
required={f'{i}.png' for i in range(841,1128)}
assets={}
for index in CLIENT.glob('Sprite*.idx'):
    raw=index.read_bytes();count=struct.unpack_from('<I',raw)[0]
    assert len(raw)==4+count*28
    with index.with_suffix('.pak').open('rb') as pak:
        for i in range(count):
            offset,name,size=struct.unpack_from('<I20sI',raw,4+i*28)
            name=name.split(b'\0')[0].decode('cp950','replace')
            if name not in required:continue
            assert name not in assets,('ambiguous archive resource',name)
            pak.seek(offset);encoded=pak.read(size);data=bytearray(encoded)
            pnx=data[:4]==b'\x89PNX'
            if pnx:
                data[3]=0x47
                for j in range(len(data)-1,4,-1):data[j]^=data[j-1]^0x52
            with Image.open(io.BytesIO(data)) as image:
                dimensions=image.size;image.verify() # includes PNG chunk CRC checks
            assert dimensions==(160,270),(name,dimensions)
            assets[name]={'archive':index.stem,'bytes':size,'pnx':pnx,'sha256':hashlib.sha256(encoded).hexdigest()}
assert set(assets)==required,sorted(required-set(assets))
report={'reference_sha256':hashlib.sha256(reference).hexdigest(),
        'descriptor_match':True,'verified_frames':len(assets),'assets':assets}
output=L/'docs/class-compat-resources.json'
output.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS: four original sex/class animation descriptors; all 287 PNG/PNX frames decode with valid CRC and dimensions.')
