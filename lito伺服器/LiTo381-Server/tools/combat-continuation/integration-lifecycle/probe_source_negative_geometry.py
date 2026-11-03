"""Trace a rejected decoded NPC skill in a separate test SHA; no live backend or SQL."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v;return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=pin(D/'source-program-update-progress-current.json');canonical=read(index['path']);base=checked(canonical['runtime']);progress=read(checked(canonical['manifest']));build=read(checked(progress['build']))
run=D/'source-negative-geometry-probes'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['SourceNegativeGeometryTrace.java','SourceNegativeGeometryTraceMethods.java',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
for ref in build['inputs']:
 if Path(ref['path']).name in ['material-cases.tsv','target-metadata.tsv','skill.properties','damage.properties','character.properties']:shutil.copyfile(checked(ref),inputs/Path(ref['path']).name)
asm=[checked(v)for v in build['asm']];toolcp=';'.join(map(str,[run/'tools',*asm]));cp=';'.join(map(str,[base,base.parent/'plan',S/'lib/*']));argsfiles=[];env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');test=run/'TEST-ONLY-negative-geometry.jar'
v=dict(status='running-test-only-NPC-negative-geometry',runtime=canonical['runtime'],basis=canonical['manifest'],inputs=[pin(p)for p in sorted(inputs.iterdir())],asm=build['asm'],new_SQL_connections=0,real_backend_started=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False,qualified_for_release=False)
try:
 with (run/'probe.log').open('w',encoding='utf-8')as out:
  def invoke(a):
   a=list(map(str,a))
   if Path(a[0]).name=='javac.exe':
    p=run/('javac-'+str(len(argsfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in a[1:]),'utf-8');argsfiles.append(p);a=[a[0],'@'+str(p)]
   subprocess.run(a,cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',toolcp,'-d',run/'tools',inputs/'SourceNegativeGeometryTraceMethods.java'])
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',inputs/'SourceNegativeGeometryTrace.java'])
  invoke([J/'java.exe','-Xverify:all','-cp',toolcp,'SourceNegativeGeometryTraceMethods',base,test,run/'classes'])
  with zipfile.ZipFile(base)as a,zipfile.ZipFile(test)as b:
   assert set(b.namelist())-set(a.namelist())=={'local/lito381/server/SourceNegativeGeometryTrace.class'}
   assert {n for n in a.namelist()if a.read(n)!=b.read(n)}=={'local/lito381/server/ClientSession.class'}
  assert pin(test)['sha256']!=canonical['runtime']['sha256']
  testcp=';'.join(map(str,[test,base.parent/'plan',S/'lib/*']))
  for mode in ['enemy-pet','ctrl-enemy-pet','own-pet','ctrl-own-pet']:
   invoke([J/'java.exe','-Xverify:all','-cp',testcp,'local.lito381.server.SourceMaterialElementalIngressPlan',inputs/'material-cases.tsv',run/(mode+'.tsv'),inputs/'target-metadata.tsv',inputs,'29',mode])
 v.update(status='observed-test-only-NPC-negative-geometry-not-semantic-qualification',test_only_runtime=pin(test),log=pin(run/'probe.log'),outcomes=[pin(run/(mode+'.tsv'))for mode in ['enemy-pet','ctrl-enemy-pet','own-pet','ctrl-own-pet']],candidate_unchanged=pin(base)==canonical['runtime'],program_index_unchanged=pin(index['path'])==index)
 assert v['candidate_unchanged']and v['program_index_unchanged']
except BaseException as error:v.update(status='failed-test-only-negative-geometry-probe',error=repr(error),log=pin(run/'probe.log'));raise
finally:write(run/'manifest.json',v)
write(D/'source-negative-geometry-current.json',dict(status=v['status'],runtime=canonical['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json')),ensure_ascii=False))
