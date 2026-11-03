"""Replay the frozen, qualified caster transaction against the NPC lifetime child."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib,json,os,subprocess,zipfile
R=Path(__file__).resolve().parent;C=R.parent;S=C.parents[1];B=S/'tools/resource-database/build';P=C/'pet-material'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
basis=P/'build/tests/result.json';original=read(basis);assert original['status']=='passed-caster-material-world-only';oldjar=Path(original['runtime']['path']);assert sha(oldjar)==original['runtime']['sha256']
overlays=[P/'build/tests/classes',P/'build/tests/memory-observer'];inputs=[pin(p)for d in overlays for p in d.rglob('*.class')]
assert inputs and all(Path(x['path']).name not in ['ClientSession.class','NativePetMaterialAction.class','ClassicWorldMonsterPopulation.class']for x in inputs)
with zipfile.ZipFile(oldjar)as a,zipfile.ZipFile(jar)as z:assert a.read('local/lito381/server/GameStateStore.class')==z.read('local/lito381/server/GameStateStore.class')
D=R/'build/material-replay';D.mkdir(exist_ok=True);run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir()
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[*overlays,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],basis=pin(basis),compiled_inputs=inputs,driver=pin(Path(__file__)),formal_deployment_changed=False,full_goal_achieved=False);log=run/'test.log'
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',';'.join(map(str,deps)),'local.lito381.server.PetMaterialProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=300)
 result=read(run/'observations.json');assert result['cases']==72 and result['actual_memory_faults']==8 and result['Source_window_bypasses']==0 and result['failed_material_casts_with_partial_commit']==0 and result['locked_writes']==0
 assert (run/'goldens.txt').read_bytes()==(P/'build/tests/after/goldens.txt').read_bytes()
 v.update(status='passed',results=result,observations=pin(run/'observations.json'),stable_wire_equal=True);print('PASS frozen pet material',result,flush=True)
except BaseException as error:
 v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6000:]);raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');(D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=b['runtime'],receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
