from pathlib import Path
import json,hashlib,subprocess,os,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/tests';D.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
for p in [R/'PetWorldLifetimeProbe.java',Path(__file__)]:
 q=frozen/(sha(p)+'-'+p.name)
 if not q.exists():shutil.copyfile(p,q)
F=B/'baseline-PC-ingress-efd4-v2';deps=[classes,R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],base=b['base'],formal_deployment_changed=False,full_goal_achieved=False,inputs=[pin(p)for p in frozen.iterdir()])
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=240)
  for mode,p in [('before',b['base']),('after',b['runtime'])]:
   jar=Path(p['path']);assert sha(jar)==p['sha256'];cp=';'.join(map(str,[*deps,jar,S/'lib/*']));dest=D/mode;dest.mkdir(exist_ok=True)
   run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,R/'PetWorldLifetimeProbe.java')
   if '--compile-only'in sys.argv:continue
   run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.PetWorldLifetimeProbe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,mode)
   result=read(dest/'observations.json');assert result['cases']==32 and result['stale_target_or_publication_gaps']==(16 if mode=='before'else 0) and result['locked_writes']==0
   v[mode]=result;print(mode,json.dumps(result),flush=True)
  v['status']='compiled-only-not-qualified'if '--compile-only'in sys.argv else'passed-world-target-lifetime-only'
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(v['status'])
