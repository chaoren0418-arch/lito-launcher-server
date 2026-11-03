from pathlib import Path
import hashlib,json,subprocess,os,shutil
R=Path(__file__).resolve().parent;P=R.parent/'return-nature-material';S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/edges';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];classes=D/'classes';classes.mkdir(exist_ok=True);frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
cp=';'.join(map(str,[classes,P/'build/boundaries/memory-observer',R/'build/tests/classes',R.parent/'elemental-material/build/boundaries/classes',R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],formal_deployment_changed=False,full_goal_achieved=False)
try:
 hook=D/'DeItemTransactionProbe.java';hook.write_text('package local.lito381.server; public final class DeItemTransactionProbe {public static void beforeSave(){ReturnNatureTransferEdgesProbe.beforeSave();}public static void beforeFlush()throws Exception{ReturnNatureTransferEdgesProbe.beforeFlush();}}',encoding='utf-8')
 sources=[R/'ReturnNatureTransferEdgesProbe.java',P/'ReturnNatureMaterialProbe.java',hook]
 for p in [*sources,Path(__file__)]:
  q=frozen/(sha(p)+'-'+p.name)
  if not q.exists():shutil.copyfile(p,q)
 v['inputs']=[pin(p)for p in frozen.iterdir()];v['observer']=pin(P/'build/boundaries/memory-observer/local/lito381/server/GameStateStore.class')
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=240)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,*sources)
  run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.ReturnNatureTransferEdgesProbe',S/'config/server.properties',D/'runtime',D/'observations.json',jar)
 obs=read(D/'observations.json');assert obs['cases']==20 and obs['owner_save_faults']==4 and obs['locked_writes']==0
 v.update(status='passed',results=obs,observation=pin(D/'observations.json'))
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(v['status'])
