from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
S=Path(__file__).resolve().parents[3];B=S/'tools/resource-database/build';C=S/'tools/combat-continuation';D=Path(__file__).resolve().parent/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
parent=read(C/'cancellation-static-runtime/build/qualification-current.json');jar=Path(parent['runtime']['path']);assert sha(jar)==parent['runtime']['sha256']=='743b35d17ea45b495af78a0208c69aca1bef977a0cdf56706bb0e7c7a52b8507';our=read(B/'de-native-pair-material-development/build-current.json');seed=read(Path(our['manifest']['path']))
run=D/'timer-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),*[B/n for n in ['NativeRetiredReuseTimer.java','NativeRetiredReuseMethods.java','NativeThunderTeleportMethods.java','NativeSkillConsumptionMethods.java']]]:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8');log=run/'build.log';v=dict(status='building',parent=pin(C/'cancellation-static-runtime/build/qualification-current.json'),base=parent['runtime'],asm=seed['asm'],frozen_inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  tools=run/'tools';tools.mkdir();tcp=';'.join(map(str,[tools,*[r['path'] for r in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',tools,inputs/'NativeRetiredReuseMethods.java',inputs/'NativeThunderTeleportMethods.java',inputs/'NativeSkillConsumptionMethods.java'])
  output=run/'classes';invoke([J/'java.exe','-Xverify:all','-cp',tcp,'NativeRetiredReuseMethods','splice',jar,output]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[output,jar,S/'lib/*'])),'-d',output,inputs/'NativeRetiredReuseTimer.java'])
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()};before=dict(entries)
  for p in output.rglob('*.class'):entries[p.relative_to(output).as_posix()]=p.read_bytes()
  changed=sorted(n for n in before if before[n]!=entries[n]);fresh=sorted(set(entries)-set(before));assert changed==['local/lito381/server/NativeReuseActor.class'] and fresh==['local/lito381/server/NativeRetiredReuseTimer$Retired.class','local/lito381/server/NativeRetiredReuseTimer.class']
  candidate=run/'candidate-integrated-retired-timer-unqualified.jar'
  with zipfile.ZipFile(candidate,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'NativeRetiredReuseMethods','verify',jar,candidate])
  with zipfile.ZipFile(our['runtime']['path']) as z:
   for n in ['NativePlayerLifetime','NativePlayerVitalCompletions','NativeSourcePlayerActionLock','NativeSourceSkillDelay']:
    name='local/lito381/server/'+n+'.class';assert before[name]==z.read(name),n+' old proven lifecycle ABI drift'
   for n in fresh:assert entries[n]==z.read(n),'retired helper differs from already qualified implementation'
 v.update(status='passed-integrated-retired-timer-build-targeted-proof-pending',runtime=pin(candidate),changed=changed,fresh=fresh,production_PC_fields_added=0,new_workers_schedulers_ThreadLocals=0);print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-7000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
