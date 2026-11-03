from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'source-equipment-columns-after-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];run=D/'source-lightning-scope-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for p in [Path(__file__),R/'SourceLightningScopeAudit.java',R/'FreezeSourceTargetDelta.java',R/'FreezeAcceptedMethods.java']:shutil.copyfile(p,run/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'verify.log';v=dict(status='running',runtime=b['runtime'],created_Hub=False,created_SQL_connection=False,initialized_game_classes=False,native_runtime_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  seed=read(Path(b['manifest']['path']));tcp=';'.join(map(str,[run/'classes',*[x['path']for x in seed['asm']]]))
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',run/'classes',run/'SourceLightningScopeAudit.java',run/'FreezeSourceTargetDelta.java',run/'FreezeAcceptedMethods.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'SourceLightningScopeAudit',b['runtime']['path']])
 v.update(status='passed-independent-scope-wrapper-storage-and-nonblocking-final-frame-audit',verified_classes=3,inputs=[pin(run/p)for p in ['SourceLightningScopeAudit.java','FreezeSourceTargetDelta.java','FreezeAcceptedMethods.java',Path(__file__).name]])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-lightning-scope-audit-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],verified_classes=v.get('verified_classes'))))
