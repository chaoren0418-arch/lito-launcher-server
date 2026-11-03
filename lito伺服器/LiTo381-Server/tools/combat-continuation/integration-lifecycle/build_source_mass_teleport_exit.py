"""Integrate the verified exit splice with a self-contained frozen compiler/plan closure."""
from pathlib import Path
from datetime import datetime,timezone
import ast,collections,copy,csv,hashlib,json,os,re,shutil,subprocess,sys,zipfile
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
replay=len(sys.argv)>1 and sys.argv[1]=='--replay'
seed=read(sys.argv[2])if replay else None
R=Path(seed['integration_root'])if replay else Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
if replay:
 parent_ref=seed['parent_build'];parent=read(checked(parent_ref));base=checked(seed['base']);staged=read(checked(seed['staged_program_verification']));sources={Path(x['path']).name:checked(x)for x in seed['inputs']}
else:
 canonical=read(D/'source-program-update-progress-current.json');progress=read(checked(canonical['manifest']));parent_ref=progress['build'];parent=read(checked(parent_ref));base=checked(canonical['runtime']);staged_ref=read(D/'source-mass-teleport-exit-verification-current.json')['manifest'];staged=read(checked(staged_ref));assert staged['canonical_program_runtime']==canonical['runtime'];sources={}
assert pin(base)['sha256']=='07a334d7aa8da829af7e4b5543ac12e305b07541706e2b9ae254741766397d9b'
run=D/'source-mass-teleport-exit-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);classes=run/'classes';jar=run/'candidate-source-mass-teleport-exit-unqualified.jar';log=run/'build.log';argfiles=[]
def collect(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:
   p=Path(v['path'])
   if p.parent==base.parent/'inputs':
    checked(v)
    if p.name in sources:assert pin(sources[p.name])['sha256']==v['sha256']
    else:sources[p.name]=p
  for x in v.values():collect(x)
 elif isinstance(v,list):
  for x in v:collect(x)
if not replay:
 collect(parent)
 for x in staged['inputs']:sources[Path(x['path']).name]=checked(x)
 stage=read(checked(staged['build']))
 for x in stage['inputs']:sources[Path(x['path']).name]=checked(x)
 sources[Path(__file__).name]=Path(__file__)
for name,p in sources.items():shutil.copyfile(p,inputs/name)
names=[Path(x['path']).name for x in parent['inputs']]
asm=[checked(x)for x in parent['asm']];cp=';'.join(map(str,[run/'tools',*asm]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
template=inputs/'build_source_self_final_frame.py';tree=ast.parse(template.read_text('utf-8'));scope=dict(J=J,run=run,jar=jar,S=S,inputs=inputs,cp=cp,base=base,classes=classes,names=names,tie_fixture=run/'test-only-forced-tie.jar',str=str,map=map)
def evaluate(node):return eval(compile(ast.Expression(node),str(template),'eval'),dict(__builtins__={},**scope))
calls=[node for node in ast.walk(tree)if isinstance(node,ast.Call)and isinstance(node.func,ast.Name)and node.func.id=='invoke'and node.args]
compiler=[];plans=[]
for node in calls:
 constants=[x.value for x in ast.walk(node.args[0])if isinstance(x,ast.Constant)and isinstance(x.value,str)]
 if 'javac.exe'in constants:compiler.append((node,constants))
 native=[x for x in constants if x.startswith('local.lito381.server.')]
 if len(native)==1 and 'forced-tie'not in constants:plans.append((node,native[0]))
assert len(compiler)==3 and len(plans)==124
v=copy.deepcopy(parent);v.update(status='running-common-mass-teleport-exit-build',integration_root=str(R),server_root=str(S),base=pin(base),parent_build=parent_ref,base_build_for_expectation=parent_ref,staged_program_verification=seed['staged_program_verification']if replay else staged_ref,inputs=[pin(p)for p in sorted(inputs.iterdir())],full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
def invoke(args,out):
 args=list(map(str,args))
 if Path(args[0]).name=='javac.exe':
  p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args[1:]),'utf-8');argfiles.append(p);args=[args[0],'@'+str(p)]
 out.flush();start=log.stat().st_size;subprocess.run(args,cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180);out.flush()
 with log.open('rb')as fragment:fragment.seek(start);return fragment.read().decode('utf-8')
try:
 with log.open('w',encoding='utf-8')as out:
  tool_node=next(node for node,c in compiler if any(isinstance(x,ast.ListComp)for x in ast.walk(node.args[0])))
  tool_args=evaluate(tool_node.args[0]);tool_args.extend(inputs/n for n in ['SourceMassTeleportExitMethods.java','SourceMassTeleportExitDelta.java','SourceMassTeleportExitLoad.java']);invoke(tool_args,out)
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceMassTeleportExitMethods','splice',base,classes,S/'lib'],out)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceMassTeleportExit.java'],out)
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  assert pin(jar)['sha256']==staged['runtime']['sha256']=='c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6'
  for cmd in [[J/'java.exe','-Xverify:all','-cp',cp,'SourceMassTeleportExitMethods','verify',base,jar,S/'lib'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceMassTeleportExitDelta',base,jar],[J/'java.exe','-Xverify:all','-cp',cp,'SourceMassTeleportExitLoad',jar,S/'lib'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceManualActionLockCensus',base,run/'parent-direct-lock-census.tsv'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceManualActionLockCensus',jar,run/'candidate-direct-lock-census.tsv']]:invoke(cmd,out)
  plan_node=next(node for node,c in compiler if 'plan'in c);plan_args=evaluate(plan_node.args[0]);plan_args.extend(inputs/n for n in ['SourceMassTeleportExitPlan.java','SourceMaterialConfiguredIngressPlan.java']);invoke(plan_args,out)
  runtime_cp=';'.join(map(str,[run/'plan',jar,S/'lib/*']));receipts=[]
  for node,name in plans:
   args=evaluate(node.args[0]);args[args.index('-cp')+1]=runtime_cp;part=invoke(args,out);counts=re.findall(r'^PASS(\d+) ',part,re.M);assert counts,(name,'missing plan receipt');receipts.append(dict(plan=name,checks=int(counts[-1]),frozen_template_line=node.lineno))
  old_counts={k:value for k,value in parent.items()if k.endswith('_contract_scenarios')};assert len(old_counts)==124 and sum(old_counts.values())==15910
  assert collections.Counter(r['checks']for r in receipts)==collections.Counter(old_counts.values())and sum(r['checks']for r in receipts)==15910,'all inherited contract counts must be observed again'
  invoke([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceMassTeleportExitPlan'],out)
  invoke([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceMaterialConfiguredIngressPlan',inputs/'material-cases.tsv',run/'configured-material-ingress-results.tsv',inputs/'target-metadata.tsv',inputs],out)
  invoke([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceMaterialConfiguredIngressPlan',inputs/'material-cases.tsv',run/'native69-exception-exit.tsv',inputs/'target-metadata.tsv',inputs,'69','exit-exception'],out)
  tie=run/'test-only-forced-tie.jar';invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourcePairAdmissionTie',jar,tie],out);invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',tie,S/'lib/*'])),'local.lito381.server.SourcePairAdmissionPlan','forced-tie'],out)
 text=log.read_text('utf-8');assert 'PASS193 mass-teleport exit original-lock'in text and 'PASS7 pair admission test-only'in text
 with (run/'configured-material-ingress-results.tsv').open(encoding='utf-8')as f:configured=list(csv.DictReader(f,delimiter='\t'))
 assert len(configured)==232 and all(not r['error_type']and not r['cleanup_error']for r in configured)
 with (run/'native69-exception-exit.tsv').open(encoding='utf-8')as f:fault=list(csv.DictReader(f,delimiter='\t'))
 assert len(fault)==1 and fault[0]['error_type']=='java.lang.NullPointerException'and 'privateShops'in fault[0]['error_message']and fault[0]['encrypted_memory_bytes']=='4'and not fault[0]['cleanup_error']
 ports=run/'mass-teleport-exit-ports.tsv';ports.write_text('owner\tmethod\tphase\nNativeAreaMaterialAction\tmassTeleport\tfirst proof capture;2 finally role checks;2 lambda proof captures;2 publication captures\nNativeAreaMaterialAction\tnativeSourceMassTeleportExitOriginalUnlock\tcomplete original lambda;readonly role and explicit final sender\nNativeLightAction\tnativeSourceMassTeleportIngress\treadonly existing ingress bridge\n','utf-8')
 v.update(status='built-common-mass-teleport-exit-first-proof-original-final-cipher-program-unqualified',runtime=pin(jar),log=pin(log),inherited_same_SHA_scenarios=15910,inherited_same_SHA_plan_receipts=receipts,pure_mass_teleport_exit_contract_scenarios=193,mass_teleport_exit_port_census=pin(ports),material_configured_ingress_outcomes=pin(run/'configured-material-ingress-results.tsv'),material_native69_exception_exit=pin(run/'native69-exception-exit.tsv'),configured_material_outcome_counts=dict(collections.Counter(r['status']for r in configured)),all_material_effects_qualified=False,actual_Ctrl_cursor_qualified=False,parent_direct_lock_census=pin(run/'parent-direct-lock-census.tsv'),candidate_direct_lock_census=pin(run/'candidate-direct-lock-census.tsv'),test_only_forced_tie_fixture=pin(tie),test_only_forced_tie_fixture_checks=7,javac_argument_files=[pin(p)for p in argfiles])
 assert v['test_only_forced_tie_fixture']['sha256']!=v['runtime']['sha256']
 if replay:assert v['runtime']['sha256']==seed['runtime']['sha256']
except BaseException as e:
 v.update(status='failed-frozen-common-mass-teleport-exit-build',error=repr(e),log=pin(log));write(run/'manifest.json',v);print(json.dumps(dict(status=v['status'],error=v['error'],manifest=pin(run/'manifest.json')),ensure_ascii=False));raise
write(run/'manifest.json',v)
target=D/('source-mass-teleport-exit-reproduction-current.json'if replay else 'source-mass-teleport-exit-build-current.json')
write(target,dict(status='passed-frozen-byte-identical-common-build'if replay else v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],same_SHA_checks=16103,separate_forced_tie_checks=7,manifest=pin(run/'manifest.json')),ensure_ascii=False))
