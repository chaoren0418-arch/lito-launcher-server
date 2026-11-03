from pathlib import Path
import json,hashlib,subprocess,os,sys,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
b=read(D/'build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];run=D/'tests';run.mkdir(exist_ok=True);classes=run/'classes';classes.mkdir(exist_ok=True)
if (run/'result.json').exists() and read(run/'result.json')['status']=='failed':
 for name in ['test.log','result.json']:
  p=run/name
  if p.exists():shutil.copyfile(p,run/('failed-'+sha(p)[:12]+'-'+name))
F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');v={'status':'running','runtime':b['runtime'],'formal_deployment_changed':False}
try:
 with (run/'test.log').open('w',encoding='utf-8') as log:
  def invoke(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,R/'SolidControlProbe.java',D/'inputs/source-reference/SourceSolidReference.java')
  if '--compile-only' in sys.argv:v['status']='compiled-only'
  else:
   invoke(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SolidControlProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar)
   obs=read(run/'observations.json');assert obs['cases']==24 and obs['parser_cases']==14 and obs['stats_cases']==16;v.update(status='passed',observations=obs)
except BaseException as e:v.update(status='failed',error=str(e));print((run/'test.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v))
