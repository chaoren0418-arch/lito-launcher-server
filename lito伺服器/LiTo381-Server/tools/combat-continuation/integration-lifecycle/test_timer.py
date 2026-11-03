from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
S=Path(__file__).resolve().parents[3];B=S/'tools/resource-database/build';D=Path(__file__).resolve().parent/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
bp=read(D/'build-current.json');jar=Path(bp['runtime']['path']);seed=read(Path(bp['manifest']['path']));old=Path(seed['base']['path']);assert sha(jar)==bp['runtime']['sha256'] and sha(old)==seed['base']['sha256']
run=D/'retired-timer-probes'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';classes=run/'classes';inputs.mkdir(parents=True);classes.mkdir()
names=['NativeRetiredReuseProbe.java','NativeLightCastProbe.java','SourceActionPairProbe.java','NativeCastMaterialProbe.java']
for p in [Path(__file__).resolve(),*[B/n for n in names]]:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';deps=[B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip'];cp=';'.join(map(str,[classes,*deps,jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
log=run/'probe.log';v=dict(status='running',runtime=bp['runtime'],parent=seed['base'],build=bp['manifest'],frozen_inputs=[pin(p) for p in inputs.iterdir()],production_PC_overlays=0,Source_or_player_SQL_or_WAL_written=False,formal_deployment_changed=False,full_goal_achieved=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[inputs/n for n in names]])
  results=[]
  for mode,runtime in [('before',old),('after',jar)]:
   branch=run/mode;branch.mkdir();observation=branch/'observations.json';runtimecp=';'.join(map(str,[classes,*deps,runtime,S/'lib/*']))
   invoke([J/'java.exe','-Xverify:all','-cp',runtimecp,'local.lito381.server.NativeRetiredReuseProbe',S/'config/server.properties',branch/'runtime',observation,runtime,mode]);result=read(observation)
   assert result['cases']==32 and result['retired_timer_gaps']==(8 if mode=='before' else 0) and result['retired_timer_rejections']==(8 if mode=='before' else 0) and result['deliberate_OPEN_rejections_preserved']==8 and result['accepted_STOPPED_schedules_preserved']==8 and result['production_PC_overlays']==0
   results.append(dict(mode=mode,runtime=pin(runtime),results=result,observations=pin(observation)))
  v.update(status='passed-native-retired-reuse-timer-and-live-rejections-and-accepted-STOPPED-control',results=results)
 print(json.dumps(dict(status=v['status'],results=[r['results'] for r in results])))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-7000:]);raise
finally:v.update(log=pin(log),compiled_fixture=[pin(p) for p in classes.rglob('*.class')]);write(run/'manifest.json',v);write(D/'retired-timer-current.json',dict(status=v['status'],runtime=bp['runtime'],manifest=pin(run/'manifest.json')))
