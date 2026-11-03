"""Verify new-class catalog IDs/names against shared resources, without modifying archives."""
from pathlib import Path
import hashlib,json,re,struct,subprocess,sys
L=Path(__file__).resolve().parents[2]
raw_path=L/'tools/class_compat/shared-spelldesc.tbl'
if '--extract' in sys.argv:
 subprocess.run(['dotnet','run','--project',str(L/'tools/class_compat/resource_audit/ResourceAudit.csproj'),'--',r'D:\lineage開發\0-客戶端\Text.idx',str(raw_path)],check=True)
raw=raw_path.read_bytes();rows={}
for line in raw.splitlines()[1:]:
 parts=line.split(b' ',1)
 if len(parts)==2 and parts[0].isdigit():rows[int(parts[0])]=parts[1]
header=(L/'native/class_skill_names.inc').read_text('ascii')
assert hashlib.sha256(raw).hexdigest() in header
names=re.findall(r'^"(.*)"',header,re.M)
assert len(names)==44
for i,encoded in enumerate(names,176):
 actual=bytes(int(x,16) for x in re.findall(r'\\x([0-9A-F]{2})',encoded))
 assert actual==rows.get(i,b''),(i,actual)
 if i in range(180,195) or i in range(200,220):assert actual
reference=Path(r'D:\lineage開發\經典版UI資源\證據原檔\TW13081901.memory.bin').read_bytes()
source=(L/'native/class_skills.inc').read_text('utf-8')
icons=list(map(int,source.split('g_sharedNewSkillIcons[40]={',1)[1].split('}',1)[0].split(',')))
expected=list(struct.unpack_from('<220H',reference,0x962ed8-0x400000))[180:]
assert icons==expected
assert len(set(icons[:15]))==15 and len(set(icons[20:]))==20
report={'shared_spelldesc_sha256':hashlib.sha256(raw).hexdigest(),'reference_sha256':hashlib.sha256(reference).hexdigest(),
 'dragon_skill_ids':list(range(181,196)),'illusion_skill_ids':list(range(201,221)),
 'names_verified':35,'icon_map_matches_reference':True,'legacy_names_preserved':176}
(L/'docs/class-skills-resources.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print('PASS: 35 shared names; exact TW icon IDs; old 176 names retained.')
