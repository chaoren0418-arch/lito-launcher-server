from pathlib import Path
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build';run=D/'tests';run.mkdir(exist_ok=True);classes=run/'classes';classes.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(D/'build.json');frozen=run/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=run/name
 if p.exists():shutil.copyfile(p,run/('previous-'+sha(p)[:12]+'-'+name))
for p in [R/'ElementalMaterialProbe.java',Path(__file__),R/'prepare_probe.py']:
 q=frozen/(sha(p)+'-'+p.name)
 if not q.exists():shutil.copyfile(p,q)
F=B/'baseline-PC-ingress-efd4-v2';deps=[classes,R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip'];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');v={'status':'running','runtime':b['runtime'],'base':b['base'],'formal_deployment_changed':False,'inputs':[pin(p) for p in frozen.iterdir()]}
try:
 with (run/'test.log').open('w',encoding='utf-8') as log:
  def invoke(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=300)
  for mode,p in [('before',b['base']),('after',b['runtime'])]:
   jar=Path(p['path']);assert sha(jar)==p['sha256'];cp=';'.join(map(str,[*deps,jar,S/'lib/*']));dest=run/mode;dest.mkdir(exist_ok=True)
   invoke(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,R/'ElementalMaterialProbe.java')
   invoke(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.ElementalMaterialProbe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,mode)
   v[mode]=read(dest/'observations.json');assert v[mode]['cases']==80 and v[mode]['Source_window_bypasses']==(16 if mode=='before' else 0)
  assert v['after']['locked_write_calls']==0
  assert (run/'before/self-material-goldens.txt').read_bytes()==(run/'after/self-material-goldens.txt').read_bytes()
  v.update(status='passed',normal_wire_equal=True,outputs=[pin(p) for p in run.glob('*/observations.json')])
except BaseException as e:v.update(status='failed',error=str(e));print((run/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v))
