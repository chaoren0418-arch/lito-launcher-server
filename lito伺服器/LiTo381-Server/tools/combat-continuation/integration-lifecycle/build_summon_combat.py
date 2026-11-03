from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
parent=read(R/'current.json');jar=Path(parent['runtime']['path']);assert sha(jar)==parent['runtime']['sha256']=='09ea36d1ce95eb15504715d16f243c0f7f3cb6748cabb2a4cd6c6dce5b8eda4c';seed=read(Path(read(D/'tamed-close-build-current.json')['manifest']['path']))
run=D/'summon-combat-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_summon_combat.py','SummonCombatMethods.java','NativeSummonTerminalLifecycle.java']:shutil.copyfile(R/name,inputs/name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],parent=pin(R/'current.json'),asm=seed['asm'],frozen_inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  tools=run/'tools';tools.mkdir();tcp=';'.join(map(str,[tools,*[p['path'] for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tools,inputs/'SummonCombatMethods.java']);classes=run/'classes';invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SummonCombatMethods','splice',jar,classes]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,jar,S/'lib/*'])),'-d',classes,inputs/'NativeSummonTerminalLifecycle.java'])
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()};before=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in before if entries[n]!=before[n]);fresh=sorted(set(entries)-set(before));assert changed==['local/lito381/server/ClientSession.class','local/lito381/server/NativeLightAction.class','local/lito381/server/NativeReturnNatureMaterialAction.class','local/lito381/server/NativeSummonTerminalLifecycle.class'] and fresh==[],(changed,fresh)
  candidate=run/'candidate-integrated-summon-combat-unqualified.jar'
  with zipfile.ZipFile(candidate,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SummonCombatMethods','verify',jar,candidate])
 v.update(status='built-Source-ordinary-summon-combat-death-retained-producer-proof-pending',runtime=pin(candidate),changed=changed,fresh=fresh,production_PC_fields_added=0,new_workers_schedulers_ThreadLocals=0,Source_disconnect_relocation_and_full_combat_matrix_remaining=True);print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'summon-combat-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
