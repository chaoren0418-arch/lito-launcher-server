from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
parent=read(D/'pair-build-current.json');jar=Path(parent['runtime']['path']);assert sha(jar)==parent['runtime']['sha256']=='a63b5731165993110f5a5c03d63f338e78d1e34104ed455e0f1e874794296ba2'
seed=read(Path(read(D/'build-current.json')['manifest']['path']))
run=D/'tamed-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_tamed.py','NativeTamedWorldLifecycle.java','NativeTamedCaptureLifecycle.java','TamedLifecycleMethods.java']:shutil.copyfile(R/name,inputs/name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8');log=run/'build.log';v=dict(status='building',parent=pin(D/'pair-build-current.json'),base=parent['runtime'],asm=seed['asm'],frozen_inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  t=run/'tools';t.mkdir();tcp=';'.join(map(str,[t,*[a['path'] for a in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',t,inputs/'TamedLifecycleMethods.java'])
  classes=run/'classes';invoke([J/'java.exe','-Xverify:all','-cp',tcp,'TamedLifecycleMethods','splice',jar,classes])
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,jar,S/'lib/*'])),'-d',classes,inputs/'NativeTamedWorldLifecycle.java',inputs/'NativeTamedCaptureLifecycle.java'])
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()};before=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in before if entries[n]!=before[n]);fresh=sorted(set(entries)-set(before));p='local/lito381/server/';assert changed==sorted(p+n+'.class' for n in ['ClientSession','ClassicWorldMonsterPopulation$Snapshot','ClassicWorldMonsterPopulation$TamedRelease','ClassicWorldMonsterPopulation','ClassicWorldMonsterPopulation$Actor','BattleCompanion']);assert fresh==sorted(p+n+'.class' for n in ['NativeTamedWorldLifecycle','NativeTamedWorldLifecycle$Respawn','NativeTamedCaptureLifecycle','NativeTamedCaptureLifecycle$Preparation'])
  candidate=run/'candidate-integrated-tamed-unqualified.jar'
  with zipfile.ZipFile(candidate,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'TamedLifecycleMethods','verify',jar,candidate])
 v.update(status='built-targeted-proof-pending',runtime=pin(candidate),changed=changed,fresh=fresh,new_gameplay_entrances=0,new_workers_ThreadLocals_strong_actor_registries=0,production_PC_fields_added=0,remaining_capture_drop_initialization_and_capture_respawn=False,unbounded_Source_NPC_bag_parity_pending=True);print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'tamed-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
