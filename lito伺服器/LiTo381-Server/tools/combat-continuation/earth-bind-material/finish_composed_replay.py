"""Retain verified completed cases; finish the async-completion probe and unchanged tail."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,shutil
E=Path(__file__).resolve().parent;R=E.parent;S=R.parents[1];B=S/'tools/resource-database/build';P=R/'poison-publication';D=E/'build/composed-replay'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
failed=D/'20261018T224110513225Z/result.json';assert sha(failed)=='b373939e415cdfcf550424f3260770a6c5148a69f75e75cc82773b02926f60e4';old=read(failed)
build=read(E/'build/build.json');runtime=build['runtime'];jar=Path(runtime['path']);assert sha(jar)==runtime['sha256'] and old['runtime']==runtime
assert old['status']=='failed' and old['results'][-1]['probe']=='PoisonScopeClock' and sum(x['results']['cases']for x in old['results'])==4464
failed_log=failed.parent/'PoisonScopeConsumer/probe.log';assert 'AssertionError: no locked output or retained receipt' in failed_log.read_text('utf-8')
for row in old['results']:
 for ref in [row['observations'],row['log'],*row['compiled_inputs']]:assert sha(Path(ref['path']))==ref['sha256'],ref['path']
consumer=E/'build/consumer-completion/result.json';cv=read(consumer);assert cv['status']=='passed' and cv['runtime']==runtime and cv['base']==build['base']
assert cv['before']['cases']==cv['after']['cases']==8 and cv['before']['locked_writes']==cv['after']['locked_writes']==0
for ref in [*cv['inputs'],*cv['outputs']]:assert sha(Path(ref['path']))==ref['sha256']
run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir();shutil.copyfile(__file__,run/Path(__file__).name)
v=dict(status='running',runtime=runtime,driver=pin(run/Path(__file__).name),basis=[*old['basis'],pin(consumer)],results=list(old['results']),reused_failed_run=pin(failed),original_failure_log=pin(failed_log),formal_deployment_changed=False,full_goal_achieved=False,test_contract_change=dict(original=pin(P/'PoisonScopeConsumerProbe.java'),corrected=pin(E/'PoisonScopeConsumerProbe.java'),same_completion_wait_reference=pin(P/'PoisonScopeTimerProbe.java'),reason='Wait for actual asynchronous HP publication completion within the existing three-second bound before separately asserting unlocked output and no retained receipt. Parent and child both tested; no runtime code or original failed evidence changed.'))
inputs=[pin(p)for d in [E/'build/consumer-completion/classes',P/'build/boundaries/classes']for p in d.rglob('*.class')]
v['results'].append(dict(probe='PoisonScopeConsumer',results=cv['after'],observations=pin(E/'build/consumer-completion/after/observations.json'),log=pin(E/'build/consumer-completion/test.log'),compiled_inputs=inputs,corrected_test_receipt=pin(consumer)))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';deps=[R/'elemental-material/build/lifecycle/classes',R/'elemental-material/build/boundaries/classes',R/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
try:
 for name,folder,count in [('Timer','timers',8),('CurseConsumer','curse-consumer',24)]:
  original=P/'build'/folder/'result.json';receipt=read(original);assert receipt['status']=='passed' and receipt['runtime']==build['base'];v['basis'].append(pin(original))
  overlays=[P/'build'/folder/'classes']
  if name=='CurseConsumer':overlays.append(P/'build/timers/classes')
  overlays.append(P/'build/boundaries/classes');inputs=[pin(p)for d in overlays for p in d.rglob('*.class')]
  dest=run/('PoisonScope'+name);dest.mkdir();log=dest/'probe.log';cp=';'.join(map(str,[*overlays,*deps]))
  with log.open('w',encoding='utf-8')as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.PoisonScope'+name+'Probe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=300)
  result=read(dest/'observations.json');assert result['cases']==count and result.get('locked_writes',result.get('locked_write_calls'))==0
  for key in ['early_warning_clocks','early_first_ticks']:
   if key in result:assert result[key]==0
  v['results'].append(dict(probe='PoisonScope'+name,results=result,observations=pin(dest/'observations.json'),log=pin(log),compiled_inputs=inputs));print('PASS','PoisonScope'+name,count,flush=True)
 assert sum(x['results']['cases']for x in v['results'])==4504
 v['status']='passed';print('PASS 4504 cases; 4464 reused with exact runtime/input/result hashes; corrected async-completion probe and remaining tail passed',flush=True)
except BaseException as error:v.update(status='failed',error=str(error));raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
 (D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=runtime,receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
