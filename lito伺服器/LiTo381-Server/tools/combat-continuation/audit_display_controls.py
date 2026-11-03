"""Pin the actual Source display consumers and non-secret selected settings; no deployment."""
from pathlib import Path
import hashlib,json,zipfile
R=Path(__file__).resolve().parent;D=R/'build/control-inventory';source=Path('D:/lineage開發/1-伺服器')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));read=lambda p:json.loads(p.read_text('utf-8'))
old=read(D/'other-controls-scope.json');keys={r['property'] for r in old['controls'] if r['classification']!='excluded-death-penalty-rule'}
settings=source/'config/other.properties';selected={}
for line in settings.read_text('utf-8-sig').splitlines():
 text=line.strip()
 if not text or text.startswith(('#','!')) or '='not in text:continue
 k,v=text.split('=',1)
 if k.strip() in keys:selected[k.strip()]=v.strip()
assert set(selected)==keys
files=[source/'src/com/lineage/config/ConfigOther.java',source/'src/com/lineage/server/utils/DamageDisplayUtil.java',source/'src/com/lineage/server/model/combat/PhysicalHitCommitPlan.java',source/'src/com/lineage/server/model/combat/MagicHitCommitPlan.java',source/'src/com/lineage/server/model/L1AttackPc.java',source/'src/com/lineage/server/model/Instance/L1MonsterInstance.java',source/'src/com/lineage/server/model/Instance/L1PcInstance.java',settings]
b=read(R/'target-negative-material/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
with zipfile.ZipFile(jar) as z:present={n:n in z.namelist() for n in ['com/lineage/config/ConfigOther.class','com/lineage/server/utils/DamageDisplayUtil.class']}
v=dict(status='consumer-audit-not-implementation-or-resource-proof',runtime=b['runtime'],sources=[pin(p)for p in files],driver=pin(Path(__file__)),selected_source_settings=selected,source_classes_present=present,findings=[
 'DmgDisplayPhysicalEnable gates both physical AND magic numbers; its Source name is not a physical-only switch.',
 'DamageDisplayUtil sends digit batches only to the attacker. Physical miss/critical effects refer to the attacker; numeric effects refer to the target.',
 'Positive damage is clamped to99999, digits are emitted high-to-low with positional SPR bases; nonpositive/disabled SPR bases are skipped.',
 'Source per-player showDamage defaults true in memory. Its optional preference ingress is a separate integration question, not a reason to write source SQL.',
 'Accepted-hit publication precedes display work. Source additionally uses minimum commit delay and drops display work on timer/worker rejection; never move HP commit onto a display worker.',
 'Kiringku type17 effect uses the caster object and sendPacketsAll; enabled plus SPR>0 gates it.',
 'HPBAR is a monster accepted-hit side effect for player attackers, distinct from player HUD HP refresh.',
 'Source fallback defaults are physical bases25001..25041, miss0 and HPBARfalse. This installation overrides physical bases25101..25141, miss25302 and HPBARtrue; number display is false and Kiringku is true.',
 'No control names or values were added to LiTo config. Native resource IDs, receivers, queue/lifetime behavior and actual consumers remain unqualified.'
],formal_deployment_changed=False,source_database_mutations=False,full_goal_achieved=False)
(D/'display-consumer-audit.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(dict(status=v['status'],settings=len(selected),source_classes_present=present)))
