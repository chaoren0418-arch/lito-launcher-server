from pathlib import Path
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(D/'build.json');run=D/'tests';run.mkdir(exist_ok=True);classes=run/'classes';classes.mkdir(exist_ok=True)
prior=read(run/'result.json') if (run/'result.json').exists() else {}
prior_log=pin(run/'test.log') if (run/'test.log').exists() else None
for name in ['test.log','result.json']:
 p=run/name
 if p.exists():shutil.copyfile(p,run/('previous-'+sha(p)[:12]+'-'+name))
F=B/'baseline-PC-ingress-efd4-v2';deps=[classes,R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');v={'status':'running','runtime':b['runtime'],'formal_deployment_changed':False,'inputs':[pin(R/'InvisibilityLeaseProbe.java'),pin(Path(__file__))]}
frozen=run/'frozen-inputs';frozen.mkdir(exist_ok=True)
def freeze(p):
 target=frozen/(sha(p)+'-'+p.name)
 if not target.exists():shutil.copyfile(p,target)
 assert sha(target)==sha(p)
 v.setdefault('frozen_inputs',[]).append({'original':pin(p),'frozen':pin(target)})
 return target
freeze(Path(__file__))
try:
 with (run/'test.log').open('w',encoding='utf-8') as log:
  def invoke(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=300)
  jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];cp=';'.join(map(str,[*deps,jar,S/'lib/*']))
  for name,source,extra in [('InvisibilityLeaseProbe',R/'InvisibilityLeaseProbe.java',[jar]),('InvisibilityControlProbe',R.parent/'invisibility-control/InvisibilityControlProbe.java',[jar,'after']),('NativeAcceptedStateRegression',B/'NativeAcceptedStateRegression.java',[])]:
   dest=run/name;dest.mkdir(exist_ok=True)
   frozen_source=freeze(source)
   if name in prior and prior.get('runtime')==b['runtime'] and pin(source) in prior['inputs']:
    assert read(dest/'observations.json')==prior[name]
    v[name]=prior[name];v['inputs'].append(pin(source));v.setdefault('reused',[]).append({'name':name,'previous_log':pin(run/('previous-'+prior_log['sha256'][:12]+'-test.log')),'observation':pin(dest/'observations.json')});continue
   dependencies=[B/'NativeIncomingLeaseProbe.java'] if name=='NativeAcceptedStateRegression' else []
   v['inputs'].extend(pin(p) for p in dependencies)
   for dependency in dependencies:freeze(dependency)
   # Public Java class names require their original filenames; compile the verified live bytes.
   assert sha(source)==sha(frozen_source)
   invoke(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,source,*dependencies)
   invoke(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+name,S/'config/server.properties',dest/'runtime',dest/'observations.json',*extra)
   v[name]=read(dest/'observations.json');v['inputs'].append(pin(source))
  assert v['InvisibilityLeaseProbe']['cases']==29
  assert v['InvisibilityControlProbe']['cases']==72
  for name in ['InvisibilityLeaseProbe','InvisibilityControlProbe']:assert v[name]['locked_writes']==0
  v.update(status='passed',outputs=[pin(p) for p in run.glob('*/observations.json')])
except BaseException as e:v.update(status='failed',error=str(e));print((run/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v))
