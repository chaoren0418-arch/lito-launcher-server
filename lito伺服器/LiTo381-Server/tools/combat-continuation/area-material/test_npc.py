from pathlib import Path
import json,hashlib,subprocess,os,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/npc';D.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
for p in [R/'AreaMaterialProbe.java',R/'AreaNpcProbe.java',Path(__file__)]:
 q=frozen/(sha(p)+'-'+p.name)
 if not q.exists():shutil.copyfile(p,q)
F=B/'baseline-PC-ingress-efd4-v2';deps=[classes,R.parent/'cancellation-npc-material/build/tests/classes',R.parent/'cancellation-probability/build/boundaries/classes',R.parent/'elemental-material/build/boundaries/classes',R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v={'status':'running','runtime':b['runtime'],'base':b['base'],'formal_deployment_changed':False,'inputs':[pin(p) for p in frozen.iterdir()]}
try:
 with (D/'test.log').open('w',encoding='utf-8') as log:
  def invoke(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=300)
  for mode,p in [('after',b['runtime'])]:
   jar=Path(p['path']);assert sha(jar)==p['sha256'];cp=';'.join(map(str,[*deps,jar,S/'lib/*']));dest=D/mode;dest.mkdir(exist_ok=True)
   invoke(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,R/'AreaMaterialProbe.java',R/'AreaNpcProbe.java')
   if '--compile-only' in sys.argv:continue
   invoke(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.AreaNpcProbe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,mode)
   v[mode]=read(dest/'observations.json');assert v[mode]['cases']==64 and v[mode]['locked_write_calls']==0
  if '--compile-only' in sys.argv:v['status']='compiled-only-not-qualified'
  else:
   v.update(status='passed-area-161-167-world-npc',stable_wire_equal=False,outputs=[pin(p) for p in D.glob('*/observations.json')])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v[k] for k in ['status','runtime']}))
