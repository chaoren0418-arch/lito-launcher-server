from pathlib import Path
import json, hashlib, subprocess, os
R=Path(__file__).resolve().parent;C=R.parent;S=C.parents[1];B=S/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
D=R/'build/speed-replay';D.mkdir(exist_ok=True)
source=C/'cancellation-speed-immunity/build/tests';accepted=read(source/'result.json')
assert accepted['status']=='passed-five-speed-immunity-consumers-only'
F=B/'baseline-PC-ingress-efd4-v2'
deps=[source/'classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
inputs=[pin(p)for p in (source/'classes').rglob('*.class')];assert inputs and all(Path(p['path']).name not in ('ClientSession.class','NativeCancellationSpeedImmunity.class')for p in inputs)
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],basis=pin(source/'result.json'),compiled_inputs=inputs,formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'test.log').open('w',encoding='utf-8')as out:
  subprocess.run(list(map(str,[Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin/java.exe'),'-Xverify:all','-cp',';'.join(map(str,deps)),'local.lito381.server.CancellationSpeedImmunityProbe',S/'config/server.properties',D/'runtime',D/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=300)
 result=read(D/'observations.json');assert result['cases']==240 and result['source_immunity_gaps']==0 and result['locked_writes']==0
 v.update(status='passed-inherited-speed-immunity-only',result=result,observations=pin(D/'observations.json'))
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-4000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(v['status'])
