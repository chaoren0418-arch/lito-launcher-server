from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'controlled-negative-material-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/'static-cancellation-route-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for p in [Path(__file__),R/'StaticCancellationRouteAuditProbe.java']:shutil.copyfile(p,run/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'audit.log';v=dict(status='running-readonly-no-Hub',runtime=build['runtime'],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  for args in [[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',run/'StaticCancellationRouteAuditProbe.java'],[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.StaticCancellationRouteAuditProbe',S/'config/server.properties',run/'routes.tsv',jar]]:subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
 observations=read(run/'observations.json');assert observations['current_static_routes_intercepted']==3 and not observations['created_Hub'];v.update(status='audited-current-static-cancellation-fallback-dormant-not-all-control-shapes',observations=observations,table=pin(run/'routes.tsv'),inputs=[pin(run/n)for n in ['StaticCancellationRouteAuditProbe.java',Path(__file__).name]])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2200:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'static-cancellation-route-audit-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
