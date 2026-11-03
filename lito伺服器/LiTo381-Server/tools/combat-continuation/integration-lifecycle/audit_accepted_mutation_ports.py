"""Locate retained erase/freeze/thunder consumers before choosing the next native test; no Hub or SQL."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,collections
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'static-respawn-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];bm=read(Path(b['manifest']['path']));run=D/'accepted-mutation-port-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'AuditAcceptedMutationPorts.java']:shutil.copyfile(p,inputs/p.name)
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java');shutil.copyfile(source,inputs/source.name);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';cp=';'.join(map(str,[classes,*[r['path']for r in bm['asm']]]));log=run/'audit.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');v=dict(status='running',runtime=b['runtime'],Source=pin(source),inputs=[pin(p)for p in inputs.iterdir()],created_Hub=False,created_SQL_connection=False,native_cases=0,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',cp,'-d',classes,inputs/'AuditAcceptedMutationPorts.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'AuditAcceptedMutationPorts',b['runtime']['path'],run/'ports.tsv',run/'selected-native-methods.txt'])
 rows=[r.split('\t')for r in(run/'ports.tsv').read_text('utf-8').splitlines()[1:]];v.update(status='audited-native-erase-freeze-thunder-ports-not-runtime-qualification',ports=len(rows),consumers=dict(collections.Counter(r[4]+'.'+r[5]for r in rows)),methods=pin(run/'selected-native-methods.txt'),table=pin(run/'ports.tsv'))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-1800:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'accepted-mutation-port-audit-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],ports=v['ports'],consumers=v['consumers'])))
