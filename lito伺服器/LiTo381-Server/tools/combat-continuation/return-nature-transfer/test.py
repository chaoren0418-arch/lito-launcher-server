from pathlib import Path
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;C=R.parent;P=C/'return-nature-material';S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/tests';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];classes=D/'classes';classes.mkdir(exist_ok=True);frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';deps=[classes,P/'build/boundaries/classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],results=[],formal_deployment_changed=False,full_goal_achieved=False)
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=240)
  hook=D/'DeItemTransactionProbe.java';hook.write_text('package local.lito381.server; public final class DeItemTransactionProbe {public static void beforeSave(){ReturnNatureTransferFaultProbe.beforeSave();}public static void beforeFlush()throws Exception{ReturnNatureTransferFaultProbe.beforeFlush();}}',encoding='utf-8')
  sources=[P/'ReturnNatureMaterialProbe.java',P/'ReturnNatureTransferFaultProbe.java',hook]
  for p in [*sources,Path(__file__)]:
   q=frozen/(sha(p)+'-'+p.name)
   if not q.exists():shutil.copyfile(p,q)
  v['inputs']=[pin(p)for p in frozen.iterdir()]
  cp=';'.join(map(str,deps));run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,*sources)
  for test,mode,runtime,cases in [('Material','after',jar,136),('TransferFault','before',Path(b['base']['path']),8),('TransferFault','after',jar,8),('Lifecycle','after',jar,36)]:
   dest=D/(test+'-'+mode);dest.mkdir(exist_ok=True);cp=';'.join(map(str,[*deps[:-2],runtime,S/'lib/*']))
   if test=='TransferFault':cp=str(classes)+';'+str(P/'build/boundaries/memory-observer')+';'+cp.removeprefix(str(classes)+';')
   run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.ReturnNature'+test+'Probe',S/'config/server.properties',dest/'runtime',dest/'observations.json',runtime,mode)
   obs=read(dest/'observations.json');assert obs['cases']==cases
   if test=='TransferFault':assert obs['owner_save_faults']==8 and obs['partial_owner_transfers']==(8 if mode=='before'else 0)
   if test=='Material':assert (dest/'goldens.txt').read_bytes()==(P/'build/tests/after/goldens.txt').read_bytes()
   for key in ['locked_writes','locked_write_calls']:
    if key in obs:assert obs[key]==0
   v['results'].append(dict(test=test,mode=mode,results=obs,observation=pin(dest/'observations.json')));print('PASS',test,mode,cases,flush=True)
  v['status']='passed'
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
