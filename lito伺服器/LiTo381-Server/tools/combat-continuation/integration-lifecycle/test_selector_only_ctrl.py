"""Replay the unchanged frozen nine-family actor fixture against the current candidate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'companion-forest-area-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];run=D/'selector-only-ctrl'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'SelectorOnlyCtrlProbe.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',b['runtime']['path'],S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';obs=run/'observations.json';v=dict(status='running',runtime=b['runtime'],basis=b['manifest'],inputs=[pin(p) for p in inputs.iterdir()],no_Hub_workers_listeners_or_SQL_session_created=True,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',inputs/'SelectorOnlyCtrlProbe.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SelectorOnlyCtrlProbe',obs])
 o=read(obs);assert o['cases']==236 and o['targeted_signature_collision_cases']==10;v.update(status='passed-selector-only161-Ctrl-and-original-targeted-wire-contracts',observations=o)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'selector-only-ctrl-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
