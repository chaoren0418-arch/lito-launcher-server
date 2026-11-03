from pathlib import Path
import hashlib,json,os,subprocess,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/tests';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');classes=D/'classes';classes.mkdir(exist_ok=True);frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
for p in [R/'WorldPotionProbe.java',Path(__file__)]:
 q=frozen/(sha(p)+'-'+p.name)
 if not q.exists():shutil.copyfile(p,q)
fixture=R.parent/'decay-material/build/boundaries/classes'
if (D/'dependency-freeze.json').exists():
 frozen_dependency=read(D/'dependency-freeze.json')['archive'];fixture=Path(frozen_dependency['path']);assert sha(fixture)==frozen_dependency['sha256']
F=B/'baseline-PC-ingress-efd4-v2';deps=[classes,fixture,R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],base=b['base'],formal_deployment_changed=False,full_goal_achieved=False,inputs=[pin(p)for p in frozen.iterdir()])
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
  for mode,ref in [('before',b['base']),('after',b['runtime'])]:
   jar=Path(ref['path']);assert sha(jar)==ref['sha256'];cp=';'.join(map(str,[*deps,jar,S/'lib/*']));dest=D/mode;dest.mkdir(exist_ok=True)
   run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,R/'WorldPotionProbe.java')
   run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.WorldPotionProbe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,mode)
   row=read(dest/'observations.json');assert row['cases']==64 and row['world_heal_gaps']==(12 if mode=='before' else 0) and row['locked_write_calls']==0;v[mode]=row;print(mode,json.dumps(row),flush=True)
 v.update(status='passed',outputs=[pin(p)for p in D.glob('*/observations.json')])
except BaseException as e:v.update(status='failed',error=str(e));print((D/'test.log').read_text('utf-8',errors='replace')[-4500:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
