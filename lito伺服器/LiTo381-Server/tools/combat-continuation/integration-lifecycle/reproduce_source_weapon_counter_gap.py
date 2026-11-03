"""Freeze actual native material27 Counter Magic failure without any server or SQL."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib, json, os, shutil, subprocess

R=Path(__file__).resolve().parent
D=R/'build'; S=R.parents[2]
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def pin(p):
    p=Path(p).resolve()
    return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
old=json.loads((D/'source-weapon-target-life-build-current.json').read_text('utf-8'))
jar=Path(old['runtime']['path']);assert pin(jar)==old['runtime']
assert old['runtime']['sha256']=='8d753f5d0e351fecf603b80d497f871a04d66207469080badee8a558118bf4a2'
run=D/'source-weapon-counter-gap'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
run.mkdir(parents=True)
source=run/'SourceWeaponFullPathPlan.java'
gap_index=D/'source-weapon-counter-gap-current.json'
if gap_index.exists():
 prior=json.loads(gap_index.read_text('utf-8'));assert pin(prior['manifest']['path'])==prior['manifest']
 gap=json.loads(Path(prior['manifest']['path']).read_text('utf-8'));original=gap['plan_source'];assert pin(original['path'])==original
 shutil.copyfile(original['path'],source)
else:shutil.copyfile(R/source.name,source)
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
cp=';'.join(map(str,[jar,jar.parent/'plan',S/'lib/*']))
compile_result=subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'plan'),str(source)],cwd=S,env=env,capture_output=True,timeout=180)
(run/'compile.log').write_bytes(compile_result.stdout+compile_result.stderr);compile_result.check_returncode()
command=[str(J/'java.exe'),'-Xverify:all','-cp',str(run/'plan')+';'+cp,'local.lito381.server.SourceWeaponFullPathPlan','counter']
result=subprocess.run(command,cwd=S,env=env,capture_output=True,timeout=180)
(run/'failure.log').write_bytes(result.stdout+result.stderr)
assert result.returncode!=0 and b'counter stacks=1' in result.stderr
v=dict(status='reproduced-original-Source27-controlled-Counter-Magic-gap',runtime=pin(jar),plan_source=pin(source),compile_log=pin(run/'compile.log'),failure_log=pin(run/'failure.log'),command=command,returncode=result.returncode,Source_skill_use=pin(Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java')),new_SQL_connections=0,real_server_started=False,formal_deployment_changed=False,full_goal_achieved=False)
write(run/'manifest.json',v)
write(D/'source-weapon-counter-gap-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json')),ensure_ascii=False))
