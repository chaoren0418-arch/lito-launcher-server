"""Freeze actual native173/174 ingress failures on the unchanged parent; memory fixtures only."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,subprocess,shutil
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'source-material-pair-guard-build-current.json');assert pin(index['runtime']['path'])==index['runtime'];base=Path(index['runtime']['path'])
run=D/'source-material-pair-ingress-gap'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
plan_source=R/'SourceMaterialPairIngressPlan.java'
prior_gap=D/'source-material-pair-ingress-gap-current.json'
if prior_gap.exists():
 prior=read(prior_gap);assert pin(prior['manifest']['path'])==prior['manifest'];historical=read(prior['manifest']['path']);ref=next(x for x in historical['inputs']if Path(x['path']).name==plan_source.name);assert pin(ref['path'])==ref;plan_source=Path(ref['path'])
shutil.copyfile(plan_source,inputs/plan_source.name);shutil.copyfile(Path(__file__),inputs/Path(__file__).name)
cp=';'.join(map(str,[base,base.parent/'plan',S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
args=['-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'plan'),str(inputs/'SourceMaterialPairIngressPlan.java')]
argfile=run/'javac.args';argfile.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8')
subprocess.run([str(J/'javac.exe'),'@'+str(argfile)],env=env,check=True,timeout=30,capture_output=True)
results=[]
for mode,expected in [('lifetime','IllegalStateException'),('source-status','NullPointerException'),('owner-status','NullPointerException')]:
 p=subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',str(run/'plan')+';'+cp,'local.lito381.server.SourceMaterialPairIngressPlan',mode],env=env,capture_output=True,timeout=30)
 text=(p.stdout+p.stderr).decode('utf-8',errors='replace');log=run/(mode+'.log');log.write_text(text,'utf-8')
 assert p.returncode!=0 and expected in text,(mode,text)
 assert 'NativeLightAction.commitPair' in text and ('Binding.capture' in text if mode!='lifetime' else 'PC application lifetime missing' in text)
 results.append(dict(mode=mode,expected_failure=expected,exit_code=p.returncode,log=pin(log)))
v=dict(status='reproduced-original-pair-ingress-first-status-and-lifetime-gaps',runtime=index['runtime'],inputs=[pin(p)for p in inputs.iterdir()],results=results,new_SQL_connections=0,real_backend_started=False,formal_deployment_changed=False,full_goal_achieved=False)
write(run/'manifest.json',v);write(D/'source-material-pair-ingress-gap-current.json',dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json')),ensure_ascii=False))
