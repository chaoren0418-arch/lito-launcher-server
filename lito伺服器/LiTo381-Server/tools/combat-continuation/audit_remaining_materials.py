"""Pin the remaining native consumers; this census is not behavioral qualification."""
from pathlib import Path
import hashlib,json,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build/remaining-material-consumers';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
runtime=read(R/'pet-controls/build/build.json')['runtime'];jar=Path(runtime['path']);assert sha(jar)==runtime['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=D/'tools';classes.mkdir(exist_ok=True)
asm=[Path(p['path'])for p in read(R/'build/integration-regression/preparation.json')['asm']];cp=';'.join(map(str,[classes,*asm]))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx128m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(R/'DumpMethod.java')],env=env,check=True,capture_output=True,timeout=30)
groups={27:['useWeaponBreakSpell','useClassicWorldWeaponBreakSpell'],36:['useTamingSpell'],41:['useCreateZombieSpell'],44:['useCancellationSpell','useWorldRoiCancellation','useWorldNpcCancellation','useWorldPlayerCancellation'],66:['useSleepSpell','useClassicWorldSleepSpell'],67:['usePolymorphSpell'],69:['useMassTeleportSpell'],76:['useMassSlowSpell','useClassicWorldMassSlowSpell'],145:['useReturnToNatureSpell'],161:['useAreaSilenceSpell'],165:['useCallOfNatureSpell'],167:['useWindShackleSpell']}
rows=[]
for skill,methods in groups.items():
 for method in methods:
  output=subprocess.run([str(J/'java.exe'),'-cp',cp,'DumpMethod',str(jar),method],env=env,check=True,capture_output=True,timeout=30).stdout
  assert output.strip(),method
  p=D/(method+'.txt');p.write_bytes(output);text=output.decode('utf-8')
  rows.append(dict(skill=skill,method=method,bytecode=pin(p),calls=[line for line in text.splitlines()if any(x in line for x in ['consumeMaterial','removeCatalog','NativeCastMaterial','canTakeControlledCompanion','saveEconomyState','NativeDeItemApplication.replace','returnToNature','ownedCompanion','NativePlayerCommit'])]))
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java')
v=dict(status='static-consumer-census-only',runtime=runtime,driver=pin(Path(__file__)),source=pin(source),remaining_material_skill_ids=list(groups),rows=rows,formal_deployment_changed=False,source_database_mutated=False,full_goal_achieved=False,limits=['Earth Bind is separately under composed replay; excluding 157 here does not declare its full regression passed','Native item, NPC creation, area target collection and role-lifetime consumers need distinct behavioral probes; one-player material admission cannot authorize another player inventory','Taming and Create Zombie source paths call canTakePet(6); recall capacity validation alone does not qualify their factories','Return to Nature source dispatch applies to L1SummonInstance; other targets receive message79; inspect probability/admission order before changing it'])
(D/'current.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print('Pinned',len(rows),'actual roots for',len(groups),'remaining skills; not behaviorally qualified')
