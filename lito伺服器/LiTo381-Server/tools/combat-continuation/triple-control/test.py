from pathlib import Path
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
b=read(D/'build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];run=D/'tests';run.mkdir(exist_ok=True);classes=run/'classes';classes.mkdir(exist_ok=True)
if (run/'result.json').exists():
 for name in ['test.log','result.json']:
  p=run/name
  if p.exists():shutil.copyfile(p,run/('previous-'+sha(p)[:12]+'-'+name))
F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v={'status':'running','runtime':b['runtime'],'formal_changed':False,'full_goal_achieved':False}
try:
 with (run/'test.log').open('w',encoding='utf-8') as log:
  def invoke(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
  invoke(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,R/'TripleControlProbe.java',R/'TripleDispatchProbe.java',D/'inputs/source-reference/SourceTripleReference.java')
  invoke(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.TripleControlProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar)
  obs=read(run/'observations.json');assert obs['parser_cases']==13 and obs['multiplier_cases']==450 and obs['scope_cases']==4;v.update(status='passed',observations=obs)
  invoke(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.TripleDispatchProbe',S/'config/server.properties',run/'dispatch-runtime',run/'dispatch-observations.json')
  dispatch=read(run/'dispatch-observations.json');assert dispatch['cases']==4 and dispatch['locked_writes']==0;v['dispatch']=dispatch
except BaseException as e:v.update(status='failed',error=str(e));print((run/'test.log').read_text('utf-8',errors='replace')[-5000:]);raise
finally:(run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(v))
