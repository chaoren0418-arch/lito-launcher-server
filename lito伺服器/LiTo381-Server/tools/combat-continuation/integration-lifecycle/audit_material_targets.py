"""Readonly startup snapshot and exact native catalog audit; no Hub or actor fixtures."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, shutil, subprocess
R=Path(__file__).resolve().parent; S=R.parents[2]; D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'companion-wind-build-current.json'); jar=Path(build['runtime']['path']); assert sha(jar)==build['runtime']['sha256']
run=D/'material-target-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'); inputs=run/'inputs'; inputs.mkdir(parents=True)
for p in [Path(__file__),R/'MaterialTargetAuditProbe.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin'); classes=run/'classes'; cp=';'.join(map(str,[classes,jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8'); log=run/'audit.log'
v=dict(status='running-readonly-no-Hub',runtime=build['runtime'],inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,inputs/'MaterialTargetAuditProbe.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.MaterialTargetAuditProbe',S/'config/server.properties',run/'targets.tsv',jar])
 observations=read(run/'observations.json');assert observations['material_rows']==58 and observations['matched_getter_records']==58 and not observations['created_Hub']
 v.update(status='audited-current-native-material-target-metadata-not-full-semantics',observations=observations,table=pin(run/'targets.tsv'))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4000:]);raise
finally:
 v['log']=pin(log);write(run/'manifest.json',v);write(D/'material-target-audit-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
