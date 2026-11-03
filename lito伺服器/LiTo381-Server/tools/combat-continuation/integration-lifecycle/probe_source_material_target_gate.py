"""Trace a selected material's unchanged target gate on a separate strict-memory test SHA."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess,sys,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref;return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
idx=read(D/'source-program-update-progress-current.json');base=checked(idx['runtime']);progress=read(checked(idx['manifest']));build=read(checked(progress['build']));sources={Path(x['path']).name:checked(x)for x in build['inputs']}
run=D/'source-material-target-gate'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['material-cases.tsv','target-metadata.tsv','skill.properties','damage.properties','character.properties','SourceMaterialConfiguredIngressPlan.java']:shutil.copyfile(sources[name],inputs/name)
for name in [Path(__file__).name,'SourceMaterialGateTrace.java']:shutil.copyfile(R/name,inputs/name)
asm=[checked(x)for x in build['asm']];cp=';'.join(map(str,[run/'tools',*asm]));test=run/'TEST-ONLY-original-gate-observations.jar';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');argfiles=[]
with (run/'trace.log').open('w',encoding='utf-8')as log:
 def call(cmd):
  cmd=list(map(str,cmd))
  if Path(cmd[0]).name=='javac.exe':
   p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in cmd[1:]),'utf-8');argfiles.append(p);cmd=[cmd[0],'@'+str(p)]
  subprocess.run(cmd,cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=90)
 call([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'tools',inputs/'SourceMaterialGateTrace.java'])
 call([J/'java.exe','-Xverify:all','-cp',cp,'SourceMaterialGateTrace',base,test])
 runtime_cp=';'.join(map(str,[run/'plan',test,base.parent/'plan',S/'lib/*']))
 call([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',runtime_cp,'-d',run/'plan',inputs/'SourceMaterialConfiguredIngressPlan.java'])
 call([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceMaterialConfiguredIngressPlan',inputs/'material-cases.tsv',run/'results.tsv',inputs/'target-metadata.tsv',inputs,sys.argv[1]if len(sys.argv)>1 else '27'])
with zipfile.ZipFile(base)as old,zipfile.ZipFile(test)as new:
 assert set(old.namelist())==set(new.namelist());changed={n for n in old.namelist()if old.read(n)!=new.read(n)};assert changed=={'local/lito381/server/NativeSourceTargetNegativeLife.class','local/lito381/server/NativeSourceWeaponTargetLife.class'}
assert pin(base)==idx['runtime']
v=dict(status='observed-original-target-gates-on-separate-test-SHA-not-production',runtime=idx['runtime'],test_only_runtime=pin(test),basis=idx['manifest'],candidate_build=progress['build'],inputs=[pin(p)for p in inputs.iterdir()],artifacts=[pin(run/'trace.log'),pin(run/'results.tsv')],production_entry_bytes_unchanged_except_two_test_observer_owners=sorted(changed),original_gate_return_values_retained=True,production_runtime_unchanged=True,full_goal_achieved=False,new_SQL_connections=0,real_backend_started=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'source-material-target-gate-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json')),ensure_ascii=False))
