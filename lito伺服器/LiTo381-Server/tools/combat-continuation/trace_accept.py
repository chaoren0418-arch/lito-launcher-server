from pathlib import Path
import json,os,subprocess,time,threading
R=Path(__file__).resolve().parent;S=R.parents[1];D=R/'build/integration-regression'
old=json.loads((D/'accept-recheck/receipt.json').read_text('utf-8'));out=D/'accept-timing';out.mkdir(exist_ok=False)
cmd=[str(out/'observations.json') if x.endswith('accept-recheck\\observations.json') else str(out/'runtime') if x.endswith('accept-recheck\\runtime') else x for x in old['command']]
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
start=time.monotonic();p=subprocess.Popen(cmd,cwd=S,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
def sample():
 time.sleep(10)
 if p.poll() is None:
  q=subprocess.run([str(Path(cmd[0]).with_name('jcmd.exe')),str(p.pid),'Thread.print'],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=15)
  (out/'threads-at-10s.txt').write_text(q.stdout+q.stderr,encoding='utf-8')
t=threading.Thread(target=sample);t.start()
with (out/'timed.log').open('w',encoding='utf-8') as f:
 for line in p.stdout:f.write(f'{time.monotonic()-start:.3f} '+line);f.flush()
p.wait();t.join();(out/'receipt.json').write_text(json.dumps({'exit_code':p.returncode,'seconds':time.monotonic()-start,'command':cmd},indent=2),encoding='utf-8');print('exit',p.returncode,'seconds',time.monotonic()-start)
