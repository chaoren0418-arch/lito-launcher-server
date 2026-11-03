"""Execute memory cleanup failure cases on the exact staged negative-final runtime."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
canonical=read(D/'source-program-update-progress-current.json');stage_index=read(D/'source-negative-final-frame-probe-current.json');stage=read(checked(stage_index['manifest']));base=checked(stage['runtime']);assert stage['base']==canonical['runtime'];parent=Path(canonical['runtime']['path']).parent
run=D/'source-negative-final-cleanup'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['SourceNegativeFinalCleanupPlan.java','SourceMaterialElementalIngressPlan.java',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
cp=';'.join(map(str,[run/'plan',base,parent/'plan',S/'lib/*']));args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'plan'),str(inputs/'SourceNegativeFinalCleanupPlan.java'),str(inputs/'SourceMaterialElementalIngressPlan.java')];arg=run/'javac.args';arg.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v=dict(status='running-negative-final-cleanup-memory-only-faults',runtime=stage['runtime'],basis=stage_index['manifest'],inputs=[pin(p)for p in sorted(inputs.iterdir())],new_SQL_connections=0,real_backend_started=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False)
try:
 with (run/'probe.log').open('w',encoding='utf-8')as out:
  subprocess.run([str(J/'javac.exe'),'@'+str(arg)],env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
  subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'local.lito381.server.SourceNegativeFinalCleanupPlan'],env=env,cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=45)
 assert pin(base)==stage['runtime'];v['status']='passed-negative-final-cleanup-three-memory-fault-cases-runtime-unchanged'
except BaseException as e:v.update(status='failed-negative-final-cleanup-memory-fault',error=repr(e));raise
finally:v['log']=pin(run/'probe.log');v['javac_args']=pin(arg);write(run/'manifest.json',v)
write(D/'source-negative-final-cleanup-current.json',dict(status=v['status'],runtime=stage['runtime'],manifest=pin(run/'manifest.json')));print(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json')),ensure_ascii=False))
