"""Locate source-vs-catalog target model differences; this does not certify runtime consumers or skill completion."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/(sys.argv[1]if len(sys.argv)>1 else'freeze-source-target-build-current.json'));assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'native-source-shape-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'NativeSourceShapeAudit.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',build['runtime']['path'],S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'audit.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],created_Hub=False,created_SQL_session_beyond_existing_readonly_startup_transaction=False,native_runtime_cases=0,full_goal_achieved=False,Source_or_player_SQL_or_WAL_written=False,formal_deployment_changed=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',inputs/'NativeSourceShapeAudit.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.NativeSourceShapeAudit',S/'config/server.properties',run/'models.tsv',run/'observations.json'])
 o=read(run/'observations.json');v.update(status='audited-source-vs-native-target-models-consumers-still-require-runtime-verification',observations=o,models=pin(run/'models.tsv'),factory_errors=pin(run/'factory-errors.tsv'))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'native-source-shape-audit-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
