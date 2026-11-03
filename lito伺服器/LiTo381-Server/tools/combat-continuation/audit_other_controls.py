"""Current Source combat-presentation controls omitted by the historical key matrix."""
from pathlib import Path
import json,hashlib,re,zipfile
R=Path(__file__).resolve().parent;S=R.parents[1];SRC=Path('D:/lineage開發/1-伺服器/src/com/lineage');D=R/'build/control-inventory'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
sources=[SRC/'config/ConfigOther.java',SRC/'server/model/L1AttackPc.java',SRC/'server/model/Instance/L1PcInstance.java',SRC/'server/model/Instance/L1MonsterInstance.java',SRC/'server/model/combat/PhysicalHitCommitPlan.java',SRC/'server/model/combat/MagicHitCommitPlan.java',SRC/'config/ConfigCharSetting.java']
text=sources[0].read_text('utf-8');keys={field:prop for field,prop in re.findall(r'\b(\w+)\s*=\s*(?:Boolean\.parseBoolean|Integer\.parseInt)\(set\.getProperty\("([^"]+)"',text)}
fields=[key for key in keys if key.startswith('DMG_') or key.startswith('KIRINGKU_EFFECT') or key in ('HPBAR','ENCOUNTER_LV')]
assert 'DMG_DISPLAY_ENABLE' in fields and 'KIRINGKU_EFFECT_SPR' in fields and 'ENCOUNTER_LV' in fields
build=json.loads((R/'negative-material/build/build.json').read_text('utf-8'));jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
with zipfile.ZipFile(jar) as z:
 classes={n:z.read(n) for n in z.namelist() if n.startswith('local/lito381/server/') and n.endswith('.class')}
rows=[]
for field in fields:
 hits=[]
 for p in sources[1:]:
  hits += [dict(path=str(p),line=i,text=line.strip()) for i,line in enumerate(p.read_text('utf-8').splitlines(),1) if 'ConfigOther.'+field in line]
 rows.append(dict(source_field=field,property=keys[field],source_consumers=hits,candidate_constant_occurrences=[n for n,data in classes.items() if keys[field].encode() in data or field.encode() in data],classification='excluded-death-penalty-rule' if field=='ENCOUNTER_LV' else 'combat-presentation-consumer-and-native-resource-validation-pending'))
v=dict(status='source-scope-audit-not-implementation-proof',runtime=build['runtime'],sources=[pin(p)for p in sources],controls=rows,notes=['ENCOUNTER_LV implements isEncounter death-exp/item protection. LiTo has no death loss by explicit user requirement; do not add a penalty to match it.','Kiringku effect is emitted only by successful close-range weaponType17 attacks in Source actionX2; must not emit on terrain-blocked or missed attacks.','Damage display has multiple numeric sprite bases in addition to its switch. The dated 195-row matrix is not a complete current configuration inventory.','Seven CHA_FOR_PET fields feed getChaForPet/canTakePet for collar pets; they must not replace hardcoded Summon Monster or Elemental Summon CHA rules without consumer evidence.','Constant occurrences are static discovery only; absence is not a behavioral test or permission to add a duplicate implementation.'],formal_deployment_changed=False,source_database_mutated=False,full_goal_achieved=False)
(D/'other-controls-scope.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(dict(status=v['status'],controls=len(rows),pending=sum(x['classification'].endswith('pending') for x in rows))))
