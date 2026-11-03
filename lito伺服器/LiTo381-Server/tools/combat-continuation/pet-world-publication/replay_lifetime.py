"""Re-use the parent lifetime probe exactly; no consumer overlays."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,subprocess
R=Path(__file__).resolve().parent;C=R.parent;S=C.parents[1];B=S/'tools/resource-database/build';P=C/'pet-world-lifetime'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];basis=P/'build/tests/result.json';original=read(basis);assert original['status']=='passed-world-target-lifetime-only';assert original['runtime']==b['base']
classes=P/'build/tests/classes';inputs=[pin(p)for p in classes.rglob('*.class')];assert inputs and all(Path(x['path']).name.startswith('PetWorldLifetimeProbe')for x in inputs)
D=R/'build/lifetime-replay';D.mkdir(exist_ok=True);run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir();J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[classes,C/'elemental-material/build/lifecycle/classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');v=dict(status='running',runtime=b['runtime'],basis=pin(basis),compiled_inputs=inputs,driver=pin(Path(__file__)),formal_deployment_changed=False,full_goal_achieved=False);log=run/'test.log'
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',';'.join(map(str,deps)),'local.lito381.server.PetWorldLifetimeProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
 result=read(run/'observations.json');assert result['cases']==32 and result['stale_target_or_publication_gaps']==0 and result['locked_writes']==0
 v.update(status='passed',results=result,observations=pin(run/'observations.json'));print('PASS frozen WORLD lifetime',result,flush=True)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-5000:]);raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');(D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=b['runtime'],receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
