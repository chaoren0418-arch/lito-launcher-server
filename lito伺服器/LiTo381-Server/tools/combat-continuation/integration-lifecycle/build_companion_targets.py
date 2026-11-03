from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(D/'companion-placement-build-current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='f08e1f95bd1c7372ecb36925c567d23f0a9c3cc514e0f4fdf789f786742cfa4b'
seed=read(Path(read(D/'summon-retirement-build-current.json')['manifest']['path']));run=D/'companion-target-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_companion_targets.py','CompanionTargetMethods.java','NativeCompanionTargets.java']:shutil.copyfile(R/name,inputs/name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],asm=seed['asm'],inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,all_scene_consumers_wired=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  tcp=';'.join(map(str,[run/'tools',*[p['path'] for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',inputs/'CompanionTargetMethods.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'CompanionTargetMethods','splice',base,classes]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeCompanionTargets.java'])
  with zipfile.ZipFile(base) as z:entries={n:z.read(n) for n in z.namelist()};old=dict(entries)
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in old if entries[n]!=old[n]);fresh=sorted(set(entries)-set(old));assert changed==['local/lito381/server/'+n+'.class' for n in ['ClientSession','NativeAreaMaterialAction$Context','NativeAreaMaterialAction','NativeLightAction']] and fresh==['local/lito381/server/NativeCompanionTargets$Death.class','local/lito381/server/NativeCompanionTargets$Scope.class','local/lito381/server/NativeCompanionTargets.class'],(changed,fresh);jar=run/'candidate-companion-targets-unqualified.jar'
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'CompanionTargetMethods','verify',base,jar])
 generated=pin(jar);current_index=D/'companion-target-build-current.json';canonical=read(current_index).get('runtime') if current_index.exists() else None
 if canonical and canonical['sha256']==generated['sha256']:
  assert sha(Path(canonical['path']))==canonical['sha256'];jar=Path(canonical['path'])
 v.update(status='built-actor-scene-targets-lifecycle-unqualified',runtime=pin(jar),generated_runtime=generated,canonical_runtime_reused=pin(jar)!=generated,changed=changed,fresh=fresh,new_player_fields_workers_schedulers_ThreadLocals_global_registries=0)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'companion-target-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],changed=v['changed'],fresh=v['fresh'])))
