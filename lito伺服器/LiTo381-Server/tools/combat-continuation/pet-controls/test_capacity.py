from pathlib import Path
import json,hashlib,subprocess,os,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/capacity';D.mkdir(parents=True,exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p),sha256=sha(p))
runtime=read(R/'build/build.json')['runtime'];jar=Path(runtime['path']);assert sha(jar)==runtime['sha256'];F=B/'baseline-PC-ingress-efd4-v2';deps=[classes,R.parent/'elemental-material/build/boundaries/classes',R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*'];cp=';'.join(map(str,deps))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');frozen=D/'frozen';frozen.mkdir(exist_ok=True)
sources=[R/'PetCapacityProbe.java',R/'build/plan/SourcePetReference.java',R/'build/plan/L1NpcInstance.java']
for p in [Path(__file__),*sources]:shutil.copyfile(p,frozen/(sha(p)+'-'+p.name))
for name in ['result.json','test.log']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
v=dict(status='running',runtime=runtime,inputs=[pin(p)for p in frozen.iterdir()],formal_deployment_changed=False)
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=240)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,*sources)
  if '--compile-only' in sys.argv:v['status']='compiled-only-not-qualified'
  else:
   run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.PetCapacityProbe',S/'config/server.properties',D/'runtime',D/'observations.json',jar);v.update(status='passed-capacity-only',results=read(D/'observations.json'));assert v['results']['cases']==56 and v['results']['decisions']==3584
except BaseException as error:v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(v['status'])
