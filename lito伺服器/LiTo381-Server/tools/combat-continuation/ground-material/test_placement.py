from pathlib import Path
import hashlib,json,os,subprocess,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/placement';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];bound=read(R/'build/boundaries/result.json');assert bound['status']=='passed' and bound['runtime']==b['runtime']
classes=D/'classes';classes.mkdir(exist_ok=True);frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[classes,R/'build/boundaries/classes',R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*'];cp=';'.join(map(str,deps))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],formal_deployment_changed=False,full_goal_achieved=False,boundary_basis=pin(R/'build/boundaries/result.json'))
try:
 for p in [R/'GroundPlacementProbe.java',Path(__file__)]:
  q=frozen/(sha(p)+'-'+p.name)
  if not q.exists():shutil.copyfile(p,q)
 v['inputs']=[pin(p) for p in frozen.iterdir()];v['compiled_dependencies']=[pin(p)for d in deps[1:-2] if d.is_dir()for p in d.rglob('*.class')]
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
  run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,R/'GroundPlacementProbe.java')
  run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.GroundPlacementProbe',S/'config/server.properties',D/'runtime',D/'observations.json',jar,'after')
 row=read(D/'observations.json');assert row['cases']==32 and row['locked_write_calls']==0;v.update(status='passed',results=row,observations=pin(D/'observations.json'),log=pin(D/'test.log'))
except BaseException as e:v.update(status='failed',error=str(e));print((D/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v['results']))
