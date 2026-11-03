"""Repeat the failed frozen accept fixture unchanged, in a fresh output directory."""
from pathlib import Path
import json,os,subprocess,hashlib
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build/integration-regression'
old=D/'replays/20261018T135329822221Z/manifest.json'
v=json.loads(old.read_text('utf-8'));cmd=v['commands'][-1]
out=D/'accept-recheck';out.mkdir(exist_ok=False)
cmd=[str(out/'observations.json') if x.endswith('accept-submission-login-observations.json') else str(out/'runtime') if x.endswith('application-accept-submission-login') else x for x in cmd]
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
result={'original_manifest':str(old),'command':cmd,'fixture_and_timeouts_unchanged':True}
try:
 with (out/'run.log').open('w',encoding='utf-8') as log:r=subprocess.run(cmd,cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=180)
 result['exit_code']=r.returncode
 if r.returncode:raise RuntimeError('frozen accept recheck failed')
 result['observations']=json.loads((out/'observations.json').read_text('utf-8'))
 print(json.dumps(result['observations']))
finally:
 (out/'receipt.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
