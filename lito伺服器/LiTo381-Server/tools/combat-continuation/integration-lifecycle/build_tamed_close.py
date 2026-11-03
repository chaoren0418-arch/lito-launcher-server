from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
parent=read(R/'current.json');jar=Path(parent['runtime']['path']);assert sha(jar)==parent['runtime']['sha256']=='ef781449052476748ec5bd64a77954bd681f9e0388c2c52428354974f09f3867';seed=read(Path(read(D/'weapon-target-build-current.json')['manifest']['path']));origin=read(Path(read(D/'tamed-build-current.json')['manifest']['path']));original=next(p for p in origin['frozen_inputs'] if p['path'].endswith('NativeTamedWorldLifecycle.java'));assert sha(Path(original['path']))==original['sha256']
run=D/'tamed-close-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);old=run/'original';old.mkdir();shutil.copyfile(original['path'],old/'NativeTamedWorldLifecycle.java')
original_text=(old/'NativeTamedWorldLifecycle.java').read_text('utf-8');before_guard='if(population(h,map,instance)!=p)return;';after_guard='if((boolean)CombatAccess.field(h,"nativeStaticMotionClosed")||population(h,map,instance)!=p)return;';assert original_text.count(before_guard)==1 and (R/'NativeTamedWorldLifecycle.java').read_text('utf-8')==original_text.replace(before_guard,after_guard)
for name in ['build_tamed_close.py','NativeTamedWorldLifecycle.java','LifecycleMethodDelta.java']:shutil.copyfile(R/name,inputs/name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],parent=pin(R/'current.json'),original_helper=original,frozen_inputs=[pin(p) for p in inputs.iterdir()],asm=seed['asm'],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  compiler=[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[jar,S/'lib/*']))];c0=run/'C0';classes=run/'classes';invoke([*compiler,'-d',c0,old/'NativeTamedWorldLifecycle.java']);invoke([*compiler,'-d',classes,inputs/'NativeTamedWorldLifecycle.java'])
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()};before=dict(entries)
  for p in c0.rglob('*.class'):assert entries[p.relative_to(c0).as_posix()]==p.read_bytes(),'TAMED C0 differs: '+p.name
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  changed=sorted(n for n in before if entries[n]!=before[n]);assert changed==['local/lito381/server/NativeTamedWorldLifecycle$Respawn.class'] and set(entries)==set(before),changed
  candidate=run/'candidate-integrated-tamed-close-unqualified.jar'
  with zipfile.ZipFile(candidate,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  tools=run/'tools';tools.mkdir();tcp=';'.join(map(str,[tools,*[p['path'] for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tools,inputs/'LifecycleMethodDelta.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'LifecycleMethodDelta',jar,candidate,'local/lito381/server/NativeTamedWorldLifecycle$Respawn','run'])
 v.update(status='built-TAMED-late-callback-close-proof-pending',runtime=pin(candidate),changed=changed,fresh=[],production_PC_fields_added=0,new_workers_schedulers_ThreadLocals=0,all_other_archive_entries_byte_identical=True);print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'tamed-close-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
