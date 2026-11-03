from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,shutil,zipfile,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();read=lambda p:json.loads(p.read_text('utf-8'));pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'summon-group-build-current.json');assert sha(Path(build['manifest']['path']))==build['manifest']['sha256'];m=read(Path(build['manifest']['path']));base=Path(m['base']['path']);jar=Path(build['runtime']['path']);assert sha(base)==m['base']['sha256'] and sha(jar)==build['runtime']['sha256'];run=D/'summon-group-delta'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'SummonGroupDelta.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'tools';cp=';'.join(map(str,[classes,*[p['path'] for p in m['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'verify.log';v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',cp,'-d',classes,inputs/'SummonGroupDelta.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'SummonGroupDelta',base,jar])
 with zipfile.ZipFile(base) as a,zipfile.ZipFile(jar) as b:
  before=set(a.namelist());after=set(b.namelist());changed=sorted(n for n in before&after if a.read(n)!=b.read(n));fresh=sorted(after-before);assert not(before-after) and changed==m['changed'] and fresh==m['fresh']
 v.update(status='passed-exact-owner-command-edges-helper-group-entries-and-field-preservation',changed=changed,fresh=fresh,production_PC_fields_added=0,new_workers_schedulers_ThreadLocals=0,Source_group_members_captured_before_lock=True,old_selected_stamp_reused=True)
except BaseException as e:v.update(status='failed',error=str(e));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'summon-group-delta-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],changed=len(changed),fresh=len(fresh))))
