"""Read-only source healing/order/target-mask and effective ConfigSkill field audit."""
from pathlib import Path
import hashlib,json,re,subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];W=S.parents[2];B=R/'build';source=W/'1-伺服器'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
config=source/'src/com/lineage/config/ConfigSkill.java';text=config.read_text(encoding='utf-8-sig')
load=text.split('private static void _loadFromProperties',1)[1].split('private static int[]',1)[0]
assert 'NATURES_BLESSING_HEAL' not in load,'Source now loads Nature extra; re-audit actual binding'
assert re.findall(r'NATURES_BLESSING_HEAL\s*=\s*([^;]+);',text)==['0'],'Source Nature field default changed'
clazz=source/'target/classes/com/lineage/config/ConfigSkill.class'
java=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/javap.exe')
p=subprocess.run([str(java),'-c','-p','-classpath',str(source/'target/classes'),'com.lineage.config.ConfigSkill'],capture_output=True,check=True)
bytecode=p.stdout.decode('utf-8',errors='replace');(B/'source-group-config-probe.txt').write_text(bytecode,encoding='utf-8')
writes=re.findall(r'iconst_0\s+\d+: putstatic[^\n]*NATURES_BLESSING_HEAL:I',bytecode)
assert len(writes)==1 and len(re.findall(r'putstatic[^\n]*NATURES_BLESSING_HEAL:I',bytecode))==1,'Compiled source loader/default contradicts Java'
use=source/'src/com/lineage/server/model/skill/L1SkillUse.java';body=use.read_text(encoding='utf-8-sig');heal=body.split('case L1Skills.TYPE_HEAL:',1)[1][:1800]
order=[heal.index(token) for token in ['WATER_LIFE','POLLUTE_WATER','ADLV80_2_2','ADLV80_2_3','NATURES_BLESSING_HEAL']];assert order==sorted(order)
entry=S/'config/combat.properties';pointers=dict(line.strip().split('=',1) for line in entry.read_text(encoding='utf-8').splitlines() if line.strip() and not line.startswith('#'))
properties=(entry.parent/pointers['source.skill.file']).resolve();raw=properties.read_text(encoding='utf-8-sig');declared=re.search(r'^NATURES_BLESSING_HEAL\s*=\s*(.*)$',raw,re.M).group(1).strip()
coverage=json.loads((S/'docs/maintenance/combat-coverage-current.json').read_text(encoding='utf-8'))
policies={str(row['skill_id']):row['sql']['target_to'] for row in coverage['skill_matrix'] if row['skill_id'] in [49,164,170]};assert policies=={'49':'20','164':'24','170':'1'}
assert coverage['candidate_sha256']==sha(B/'candidate.jar')
files=[config,clazz,use,source/'src/com/lineage/server/templates/L1Skills.java',source/'src/com/lineage/server/model/proc/ProcSkillEffectHelper.java']
receipt={'candidate_sha256':sha(B/'candidate.jar'),'source_files':[{'path':str(p),'sha256':sha(p)} for p in files],'source_skill_properties':{'path':str(properties),'sha256':sha(properties),'declared_nature_extra':declared,'read_by_source_loader':False,'effective_source_field':0},'source_type_heal_order':['Water Life','Pollute','dirty current','healing erosion','Nature field extra','NPC undead conversion'],'source_skill_target_to':policies,'source_config_probe_sha256':sha(B/'source-group-config-probe.txt'),'status':'passed','full_behavior_parity':False}
(B/'group-healing-source-current.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('PASS source healing order/target masks; ConfigSkill Java and compiled loader both leave Nature field0; property '+declared+' is ignored; no source or SQL writes')
