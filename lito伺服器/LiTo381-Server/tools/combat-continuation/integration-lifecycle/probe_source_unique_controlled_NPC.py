"""Revalidate the exact common runtime with globally unique real NPC fixture identities."""
from pathlib import Path
from datetime import datetime,timezone
import csv,hashlib,json,os,shutil,subprocess,collections
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v;return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=pin(D/'source-program-update-progress-current.json');canonical=read(index['path']);base=checked(canonical['runtime']);progress=read(checked(canonical['manifest']));build=read(checked(progress['build']))
run=D/'source-unique-controlled-NPC-probes'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
plans=['SourceMaterialElementalIngressPlan.java','SourceMaterialNpcLifecyclePlan.java','SourceControlledNegativeFinalPlan.java','SourceMaterialNpcFinalPlan.java']
for name in [*plans,Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
for ref in build['inputs']:
 if Path(ref['path']).name in ['material-cases.tsv','target-metadata.tsv','skill.properties','damage.properties','character.properties']:shutil.copyfile(checked(ref),inputs/Path(ref['path']).name)
cp=';'.join(map(str,[run/'plan',base,base.parent/'plan',S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');argfiles=[]
v=dict(status='running-unique-real-controlled-NPC-fixtures',runtime=canonical['runtime'],basis=canonical['manifest'],inputs=[pin(p)for p in sorted(inputs.iterdir())],original_candidate_build=progress['build'],production_runtime_changed=False,new_SQL_connections=0,real_backend_started=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False,qualified_for_release=False)
try:
 with (run/'probe.log').open('w',encoding='utf-8')as out:
  def invoke(a):
   a=list(map(str,a))
   if Path(a[0]).name=='javac.exe':
    p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in a[1:]),'utf-8');argfiles.append(p);a=[a[0],'@'+str(p)]
   subprocess.run(a,cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=150)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'plan',*[inputs/n for n in plans]])
  for name,file in [('SourceMaterialNpcLifecyclePlan','owner-pair-results.tsv'),('SourceControlledNegativeFinalPlan','final-results.tsv'),('SourceMaterialElementalIngressPlan','elemental-material-results.tsv')]:invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+name,inputs/'material-cases.tsv',run/file,inputs/'target-metadata.tsv',inputs])
 outcomes={}
 for file,count in [('owner-pair-results.tsv',60),('final-results.tsv',78),('elemental-material-results.tsv',464)]:
  with (run/file).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
  assert len(rows)==count and all(not r['error_type']and not r['cleanup_error']for r in rows),file
  outcomes[file]=dict(artifact=pin(run/file),count=len(rows),statuses=dict(collections.Counter(r['status']for r in rows)))
 text=(run/'probe.log').read_text('utf-8')
 for marker in ['PAIR LIFECYCLE checks=456','actual-decoded-Ctrl-NPC-owner-pair-waits=60','NPC FINAL LIFECYCLE checks=695','actual-decoded-NPC-final-monitor-waits=78']:assert marker in text,marker
 v.update(status='passed-unique-controlled-NPC-native-pair-final-and-material-ingress',outcomes=outcomes,log=pin(run/'probe.log'),new_final_assertions=695,actual_final_monitor_waits=78,NPC_pair_assertions=456,actual_NPC_pair_waits=60,fixture_identity='All four roles have distinct inactive primary placeholders and only the real45306 actor is active in the selected owner list; native complete roster rejects duplicate live player/NPC IDs before any input. No runtime ID or selection relaxation.',candidate_unchanged=pin(base)==canonical['runtime'],program_index_unchanged=pin(index['path'])==index)
 assert v['candidate_unchanged']and v['program_index_unchanged']
except BaseException as error:v.update(status='failed-unique-controlled-NPC-probe',error=repr(error),log=pin(run/'probe.log'));raise
finally:write(run/'manifest.json',v)
write(D/'source-unique-controlled-NPC-current.json',dict(status=v['status'],runtime=canonical['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],manifest=pin(run/'manifest.json'),outcomes={k:x['statuses']for k,x in outcomes.items()}),ensure_ascii=False))
