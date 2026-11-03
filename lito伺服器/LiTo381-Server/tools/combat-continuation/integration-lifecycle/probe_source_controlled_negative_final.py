"""Stage explicit controlled-negative final frames on the exact cumulative candidate, no deployment."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess,sys,zipfile,csv,collections,ast,re
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref,ref['path'];return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
replay=len(sys.argv)>1 and sys.argv[1]=='--replay';seed=read(sys.argv[2])if replay else None
if replay:R=Path(seed['base']['path']).parents[3];D=R/'build';S=R.parents[2]
parent_index=pin(D/'source-program-update-progress-current.json');idx=dict(runtime=seed['base'],manifest=seed['basis'])if replay else read(D/'source-program-update-progress-current.json');base=checked(idx['runtime']);progress=read(checked(idx['manifest']));build=read(checked(progress['build']));gap_index=dict(runtime=seed['base'],manifest=seed['negative_outcome_basis'])if replay else read(D/'source-material-NPC-final-current.json');gap=read(checked(gap_index['manifest']));assert gap_index['runtime']==idx['runtime']
full=seed['full_regression']if replay else '--full'in sys.argv;case_args=[]if replay else [a for a in sys.argv[1:]if a!='--full']
run=D/'source-controlled-negative-final-probes'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
if replay:
 for ref in seed['inputs']:shutil.copyfile(checked(ref),inputs/Path(ref['path']).name)
else:
 for name in ['NativeSourceControlledNegativeFinal.java','SourceControlledNegativeFinalMethods.java','SourceControlledNegativeFinalDelta.java',Path(__file__).name,'SourceControlledNegativeFinalPlan.java','SourceMaterialNpcLifecyclePlan.java','SourceMaterialControlLifecyclePlan.java','SourceMaterialControlIngressPlan.java','SourceMaterialElementalIngressPlan.java']:shutil.copyfile(R/name,inputs/name)
 for ref in gap['inputs']:shutil.copyfile(checked(ref),inputs/Path(ref['path']).name)
asm=[checked(x)for x in build['asm']];cp=';'.join(map(str,[run/'tools',base.parent/'tools',*asm]));classes=run/'classes';jar=run/'candidate-controlled-negative-final-unqualified.jar';argfiles=[];env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
v=dict(status='running-staged-controlled-negative-final',base=idx['runtime'],basis=idx['manifest'],canonical_program_index=parent_index,negative_outcome_basis=gap_index['manifest'],inputs=[pin(p)for p in sorted(inputs.iterdir())],asm=build['asm'],full_regression=full,new_SQL_connections=0,real_backend_started=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False,qualified_for_release=False,canonical_promoted=False)
try:
 with (run/'probe.log').open('w',encoding='utf-8')as log:
  def call(cmd):
   cmd=list(map(str,cmd))
   if Path(cmd[0]).name=='javac.exe':
    p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in cmd[1:]),'utf-8');argfiles.append(p);cmd=[cmd[0],'@'+str(p)]
   subprocess.run(cmd,cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
  call([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'tools',inputs/'SourceControlledNegativeFinalMethods.java',inputs/'SourceControlledNegativeFinalDelta.java'])
  call([J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalMethods','splice',base,classes,S/'lib'])
  call([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceControlledNegativeFinal.java'])
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  call([J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalMethods','verify',base,jar,S/'lib'])
  call([J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalDelta',base,jar])
  runtime_cp=';'.join(map(str,[run/'plan',jar,base.parent/'plan',S/'lib/*']))
  plan_names=['SourceControlledNegativeFinalPlan.java','SourceMaterialElementalIngressPlan.java','SourceMaterialNpcLifecyclePlan.java','SourceMaterialControlLifecyclePlan.java','SourceMaterialControlIngressPlan.java']
  call([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',runtime_cp,'-d',run/'plan',*[inputs/n for n in plan_names]])
  call([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceControlledNegativeFinalPlan',inputs/'material-cases.tsv',run/'final-results.tsv',inputs/'target-metadata.tsv',inputs,*case_args])
  if full:
   for name,filename in [('SourceMaterialNpcLifecyclePlan','owner-pair-results.tsv'),('SourceMaterialControlLifecyclePlan','PC-pair-results.tsv'),('SourceMaterialControlIngressPlan','decoded-material-results.tsv'),('SourceMaterialElementalIngressPlan','elemental-material-results.tsv')]:call([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.'+name,inputs/'material-cases.tsv',run/filename,inputs/'target-metadata.tsv',inputs])
   template=next(checked(x)for x in build['inputs']if Path(x['path']).name=='build_source_self_final_frame.py');tree=ast.parse(template.read_text('utf-8'));scope=dict(J=J,run=run,jar=jar,S=S,inputs=base.parent/'inputs',cp=runtime_cp,str=str,map=map);inherited=[]
   for node in ast.walk(tree):
    if not(isinstance(node,ast.Call)and isinstance(node.func,ast.Name)and node.func.id=='invoke'and node.args):continue
    constants=[x.value for x in ast.walk(node.args[0])if isinstance(x,ast.Constant)and isinstance(x.value,str)];plans=[x for x in constants if x.startswith('local.lito381.server.')]
    if len(plans)!=1 or 'forced-tie'in constants:continue
    args=eval(compile(ast.Expression(node.args[0]),str(template),'eval'),dict(__builtins__={},**scope));args[args.index('-cp')+1]=runtime_cp;call(args);inherited.append(plans[0])
   assert len(inherited)==124
   call([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceMassTeleportExitPlan']);v['inherited_regular_plan_runs']=inherited;v['additional_mass_exit_plan_rerun']=True
 with (run/'final-results.tsv').open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
 v.update(status='staged-controlled-negative-final-observed-not-promoted',runtime=pin(jar),attempts=len(rows),errors=[dict(skill_id=r['skill_id'],mode=r['mode'],error=r['error_message'])for r in rows if r['error_type']or r['cleanup_error']],outcomes=dict(collections.Counter(r['status']for r in rows)),artifacts=[pin(run/'probe.log'),pin(run/'final-results.tsv')])
 if full:
  for name,count in [('owner-pair-results.tsv',60),('PC-pair-results.tsv',42),('decoded-material-results.tsv',464),('elemental-material-results.tsv',464)]:
   with (run/name).open(encoding='utf-8')as f:extra=list(csv.DictReader(f,delimiter='\t'))
   assert len(extra)==count and all(not r['error_type']and not r['cleanup_error']for r in extra),name
   v['artifacts'].append(pin(run/name))
  assert not v['errors']and v['attempts']==78
 if replay:assert v['runtime']['sha256']==seed['runtime']['sha256'],'frozen byte-identical runtime replay'
except BaseException as error:
 v.update(status='failed-frozen-controlled-negative-final-probe',error=repr(error),log=pin(run/'probe.log'));write(run/'manifest.json',v);print(json.dumps(dict(status=v['status'],error=v['error'],manifest=pin(run/'manifest.json')),ensure_ascii=False));raise
assert pin(D/'source-program-update-progress-current.json')==parent_index
write(run/'manifest.json',v);write(D/('source-controlled-negative-final-reproduction-current.json'if replay else 'source-controlled-negative-final-probe-current.json'),dict(status='passed-frozen-byte-identical-staged-controlled-negative-final'if replay else v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],attempts=v['attempts'],errors=len(v['errors']),manifest=pin(run/'manifest.json')),ensure_ascii=False))
