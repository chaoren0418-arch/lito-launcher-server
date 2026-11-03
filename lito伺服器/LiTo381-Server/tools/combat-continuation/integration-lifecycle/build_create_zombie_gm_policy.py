from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,zipfile,os
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(D/'create-zombie-build-current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='4982a8c0d911e99ee20ea41f685b0f15305ecc57aae31f652d988067829769d1';seed=read(Path(parent['manifest']['path']));run=D/'create-zombie-GM-policy-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'NativeCreateZombieLifecycle.java',R/'LifecycleMethodDelta.java']:shutil.copyfile(p,inputs/p.name)
text=(inputs/'NativeCreateZombieLifecycle.java').read_text('utf-8');old='return LiToGmNpcs.active(encounter)?encounter:null;';new='var rules=((BattleRules.MatchState)CombatAccess.field(record,"match")).rules();return LiToGmNpcs.active(encounter)&&LiToGmNpcs.combatMode(rules)==BattleRules.Mode.TIME_ATTACK?encounter:null;';assert text.count(old)==1;text=text.replace(old,new);(inputs/'NativeCreateZombieLifecycle.java').write_text(text,'utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],parent_build=parent['manifest'],asm=seed['asm'],frozen_inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(a):subprocess.run(list(map(str,a)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  classes=run/'classes';invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[base,S/'lib/*'])),'-d',classes,inputs/'NativeCreateZombieLifecycle.java'])
  with zipfile.ZipFile(base) as z:entries={n:z.read(n) for n in z.namelist()}
  before=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in before if entries[n]!=before[n]);assert changed==['local/lito381/server/NativeCreateZombieLifecycle.class'] and set(entries)==set(before)
  jar=run/'candidate-integrated-create-zombie-GM-policy-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
   for name,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(name,(1980,1,1,0,0,0)),data)
  tools=run/'tools';cp=';'.join(map(str,[tools,*[p['path'] for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',cp,'-d',tools,inputs/'LifecycleMethodDelta.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'LifecycleMethodDelta',base,jar,'local/lito381/server/NativeCreateZombieLifecycle','gmEncounter'])
 v.update(status='built-existing-GM-rule-identity-guard-Source-zombie-qualification-pending',runtime=pin(jar),changed=changed,fresh=[],production_PC_fields_added=0,new_workers_schedulers_ThreadLocals=0,all_other_archive_entries_byte_identical=True,only_helper_method_changed='gmEncounter');print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
except BaseException as e:v.update(status='failed',error=str(e));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'create-zombie-GM-policy-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
