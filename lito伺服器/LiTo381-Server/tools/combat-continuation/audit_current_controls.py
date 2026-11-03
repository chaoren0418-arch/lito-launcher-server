"""Inventory actual configured keys; loading is explicitly not consumer qualification."""
from pathlib import Path
import subprocess,json,hashlib,re
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build/control-inventory';D.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=json.loads((R/'summon-ring/build/build.json').read_text('utf-8'));jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[D,jar,S/'lib/*']))
with (D/'audit.log').open('w',encoding='utf-8') as log:
 for cmd in [[J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',D,R/'ControlAudit.java'],[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.ControlAudit',S/'config/server.properties',jar,D/'actual.properties']]:
  subprocess.run(list(map(str,cmd)),check=True,stdout=log,stderr=subprocess.STDOUT,timeout=30)
matrix=S/'docs/maintenance/combat-coverage-current.json';old=json.loads(matrix.read_text('utf-8'))
loaded={line.split('=',1)[0] for line in (D/'actual.properties').read_text('utf-8').splitlines() if line and not line.startswith('#')}
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/config/ConfigPcDamage.java')
aliases=dict(re.findall(r'\b(\w+)\s*=\s*_getDouble\(p,\s*"([^"]+)"',source.read_text('utf-8')))
assert len(aliases)==25
rows=[]
for row in old['external_controls']:
 if row['owner'] not in ['ConfigSkill','ConfigPcDamage']:continue
 key=aliases[row['key']] if row['owner']=='ConfigPcDamage' else 'Triple_Arrow_Dmg' if row['key']=='TRIPLE_ARROW_DMG' else row['key']
 keys=[key+'.base',key+'.random'] if key.endswith('_TIMER') else [key]
 rows.append(dict(owner=row['owner'],source_key=row['key'],runtime_keys=keys,loaded=all(k in loaded for k in keys),historical_installed_consumer=row['installed_consumer']))
assert all(x['loaded'] for x in rows),[x for x in rows if not x['loaded']]
v=dict(status='configured-key-inventory-only',runtime=b['runtime'],historical_matrix=pin(matrix),loaded_keys=sorted(loaded),source_skill_and_damage_rows=rows,loaded_key_count=len(loaded),source_property_aliases=pin(source),scope='ConfigSkill and ConfigPcDamage keys from the dated matrix are all present after exact Source field/property aliases and timer expansion; this is not proof of every current Source config or every runtime consumer.',initial_audit_limitation='Direct field-name comparison incorrectly reported 21 property aliases and two split timers as absent; mapping was corrected against Source loader and existing snapshot.',evidence=[pin(D/'actual.properties'),pin(D/'audit.log'),pin(R/'ControlAudit.java')],source_database_mutated=False,formal_deployment_changed=False,full_goal_achieved=False)
(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(dict(status=v['status'],loaded=len(loaded),source_skill_and_damage_keys=len(rows))))
