"""Freeze the real PC material27 dynamic Counter Magic exception failure without live services."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, shutil, subprocess

R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'source-weapon-counter-magic-build-current.json')
assert pin(index['runtime']['path'])==index['runtime'] and pin(index['manifest']['path'])==index['manifest']
jar=Path(index['runtime']['path'])
run=D/'source-weapon-player-gap'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
source=run/'SourceWeaponPlayerFullPathPlan.java'
prior=read(D/'source-weapon-player-gap-current.json');assert pin(prior['manifest']['path'])==prior['manifest'];frozen=read(prior['manifest']['path'])['plan_source'];assert pin(frozen['path'])==frozen;shutil.copyfile(frozen['path'],source)
cp=';'.join(map(str,[jar,jar.parent/'plan',S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
compile_result=subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'plan'),str(source)],cwd=S,env=env,capture_output=True,timeout=120)
(run/'compile.log').write_bytes(compile_result.stdout+compile_result.stderr);compile_result.check_returncode()
command=[str(J/'java.exe'),'-Xverify:all','-cp',str(run/'plan')+';'+cp,'local.lito381.server.SourceWeaponPlayerFullPathPlan']
result=subprocess.run(command,cwd=S,env=env,capture_output=True,timeout=120)
(run/'failure.log').write_bytes(result.stdout+result.stderr)
assert result.returncode!=0 and b'dynamic Counter Magic exception retained: bypass=true' in result.stderr
assert result.stdout.count(b'countered=true')==2
v=dict(status='reproduced-actual-PC-material27-dynamic-Counter-Magic-exception-gap',runtime=index['runtime'],basis=index['manifest'],plan_source=pin(source),inputs=[pin(Path(__file__))],compile_log=pin(run/'compile.log'),failure_log=pin(run/'failure.log'),command=command,returncode=result.returncode,actual_native_PC_casts=2,strict_memory_repository=True,nonpersistent_GameStateStore=True,new_SQL_connections=0,real_server_started=False,formal_deployment_changed=False,full_PC_weapon_qualified=False,full_goal_achieved=False,Source_skill_use=pin(Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java')),next_action='Correct the PC successful Counter Magic consumption using the existing Source exception set; retain full original target/life/pair, material/MP, RNG, durability and output body. Add real target weapon/durability and accepted/final lifecycle cases before qualification.')
write(run/'manifest.json',v);write(D/'source-weapon-player-gap-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json'))))
