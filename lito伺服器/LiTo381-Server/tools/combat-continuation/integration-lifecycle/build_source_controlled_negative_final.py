"""Self-contained common assembly; compile only the new runtime helper, never older helpers."""
from pathlib import Path
from datetime import datetime,timezone
import ast,collections,copy,csv,hashlib,json,os,re,shutil,subprocess,sys,zipfile
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(v):assert pin(v['path'])==v,v['path'];return Path(v['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
replay=len(sys.argv)>1 and sys.argv[1]=='--replay';seed=read(sys.argv[2])if replay else None
R=Path(seed['integration_root'])if replay else Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
sources={}
if replay:
 parent_ref=seed['parent_build'];parent=read(checked(parent_ref));base=checked(seed['base']);staged_ref=seed['staged_program_verification'];staged=read(checked(staged_ref));sources={Path(x['path']).name:checked(x)for x in seed['inputs']}
else:
 canonical=read(D/'source-program-update-progress-current.json');progress=read(checked(canonical['manifest']));parent_ref=progress['build'];parent=read(checked(parent_ref));base=checked(canonical['runtime']);staged_ref=read(D/'source-controlled-negative-final-verification-current.json')['manifest'];staged=read(checked(staged_ref));assert staged['canonical_runtime']==canonical['runtime']
assert pin(base)['sha256']=='c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6'
run=D/'source-controlled-negative-final-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);classes=run/'classes';jar=run/'candidate-controlled-negative-final-unqualified.jar';log=run/'build.log';argfiles=[]
if not replay:
 for ref in parent['inputs']:sources[Path(ref['path']).name]=checked(ref)
 for m in [read(checked(staged['build'])),staged]:
  for ref in m['inputs']:sources[Path(ref['path']).name]=checked(ref)
 sources[Path(__file__).name]=Path(__file__)
 sources['SourceSelfFinalFramePlan.java']=R/'SourceSelfFinalFramePlan.java'
for name,p in sources.items():shutil.copyfile(p,inputs/name)
tool_names=seed['tool_sources']if replay else [Path(json.loads(line)).name for line in checked(parent['javac_argument_files'][0]).read_text('utf-8').splitlines()if json.loads(line).endswith('.java')]
tool_names=list(dict.fromkeys(tool_names+['SourceControlledNegativeFinalMethods.java','SourceControlledNegativeFinalDelta.java','SourceControlledNegativeFinalLoad.java']))
assert not any(n.startswith('Native')for n in tool_names),'old runtime helpers must not be compiled'
asm=[checked(x)for x in parent['asm']];cp=';'.join(map(str,[run/'tools',*asm]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
template=inputs/'build_source_self_final_frame.py';tree=ast.parse(template.read_text('utf-8'));scope=dict(J=J,run=run,jar=jar,S=S,inputs=inputs,cp=cp,str=str,map=map)
def evaluate(node):return eval(compile(ast.Expression(node),str(template),'eval'),dict(__builtins__={},**scope))
plans=[];plan_node=None
for node in ast.walk(tree):
 if not(isinstance(node,ast.Call)and isinstance(node.func,ast.Name)and node.func.id=='invoke'and node.args):continue
 constants=[x.value for x in ast.walk(node.args[0])if isinstance(x,ast.Constant)and isinstance(x.value,str)]
 if 'javac.exe'in constants and 'plan'in constants:plan_node=node
 native=[x for x in constants if x.startswith('local.lito381.server.')]
 if len(native)==1 and 'forced-tie'not in constants:plans.append((node,native[0]))
assert plan_node is not None and len(plans)==124
v=copy.deepcopy(parent);v.update(status='running-common-controlled-negative-final-build',integration_root=str(R),server_root=str(S),base=pin(base),parent_build=parent_ref,base_build_for_expectation=parent_ref,staged_program_verification=staged_ref,inputs=[pin(p)for p in sorted(inputs.iterdir())],tool_sources=tool_names,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
def invoke(args,out):
 args=list(map(str,args))
 if Path(args[0]).name=='javac.exe':
  p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args[1:]),'utf-8');argfiles.append(p);args=[args[0],'@'+str(p)]
 out.flush();start=log.stat().st_size;subprocess.run(args,cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180);out.flush()
 with log.open('rb')as fragment:fragment.seek(start);return fragment.read().decode('utf-8')
try:
 with log.open('w',encoding='utf-8')as out:
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'tools',*[inputs/n for n in tool_names]],out)
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalMethods','splice',base,classes,S/'lib'],out)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceControlledNegativeFinal.java'],out)
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  assert pin(jar)['sha256']==staged['runtime']['sha256']=='e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a'
  for cmd in [[J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalMethods','verify',base,jar,S/'lib'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalDelta',base,jar],[J/'java.exe','-Xverify:all','-cp',cp,'SourceControlledNegativeFinalLoad',jar,S/'lib'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceManualActionLockCensus',base,run/'parent-direct-lock-census.tsv'],[J/'java.exe','-Xverify:all','-cp',cp,'SourceManualActionLockCensus',jar,run/'candidate-direct-lock-census.tsv']]:invoke(cmd,out)
  extra=['SourceMassTeleportExitPlan.java','SourceMaterialConfiguredIngressPlan.java','SourceMaterialControlIngressPlan.java','SourceMaterialControlLifecyclePlan.java','SourceMaterialElementalIngressPlan.java','SourceMaterialNpcLifecyclePlan.java','SourceMaterialNpcFinalPlan.java','SourceControlledNegativeFinalPlan.java']
  plan_args=evaluate(plan_node.args[0]);plan_args.extend(inputs/n for n in extra if str(inputs/n)not in map(str,plan_args));invoke(plan_args,out)
  runtime_cp=';'.join(map(str,[run/'plan',jar,S/'lib/*']));receipts=[]
  for node,name in plans:
   args=evaluate(node.args[0]);args[args.index('-cp')+1]=runtime_cp;part=invoke(args,out);counts=re.findall(r'^PASS(\d+) ',part,re.M);assert counts,(name,'missing plan receipt');receipts.append(dict(plan=name,checks=int(counts[-1]),frozen_template_line=node.lineno))
  old_counts={k:value for k,value in parent.items()if k.endswith('_contract_scenarios')and k!='pure_mass_teleport_exit_contract_scenarios'};assert len(old_counts)==124 and sum(old_counts.values())==15910
  assert collections.Counter(r['checks']for r in receipts)==collections.Counter(old_counts.values())and sum(r['checks']for r in receipts)==15910
  invoke([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceMassTeleportExitPlan'],out)
  for name,filename in [('SourceControlledNegativeFinalPlan','final-results.tsv'),('SourceMaterialNpcLifecyclePlan','owner-pair-results.tsv'),('SourceMaterialControlLifecyclePlan','PC-pair-results.tsv'),('SourceMaterialControlIngressPlan','decoded-material-results.tsv'),('SourceMaterialElementalIngressPlan','elemental-material-results.tsv'),('SourceMaterialConfiguredIngressPlan','configured-material-ingress-results.tsv')]:invoke([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.'+name,inputs/'material-cases.tsv',run/filename,inputs/'target-metadata.tsv',inputs],out)
  invoke([J/'java.exe','-Xverify:all','-cp',runtime_cp,'local.lito381.server.SourceMaterialConfiguredIngressPlan',inputs/'material-cases.tsv',run/'native69-exception-exit.tsv',inputs/'target-metadata.tsv',inputs,'69','exit-exception'],out)
  tie=run/'test-only-forced-tie.jar';invoke([J/'java.exe','-Xverify:all','-cp',cp,'SourcePairAdmissionTie',jar,tie],out);invoke([J/'java.exe','-Xverify:all','-cp',';'.join(map(str,[run/'plan',tie,S/'lib/*'])),'local.lito381.server.SourcePairAdmissionPlan','forced-tie'],out)
 text=log.read_text('utf-8')
 for marker in ['PASS193 mass-teleport exit original-lock','NPC FINAL LIFECYCLE checks=695','actual-decoded-NPC-final-monitor-waits=78','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','PASS7 pair admission test-only']:assert marker in text,marker
 artifacts={}
 for filename,count in [('final-results.tsv',78),('owner-pair-results.tsv',60),('PC-pair-results.tsv',42),('decoded-material-results.tsv',464),('elemental-material-results.tsv',464),('configured-material-ingress-results.tsv',232)]:
  with (run/filename).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
  assert len(rows)==count and all(not r['error_type']and not r['cleanup_error']for r in rows),filename
  artifacts[filename]=pin(run/filename)
 with (run/'native69-exception-exit.tsv').open(encoding='utf-8')as f:fault=list(csv.DictReader(f,delimiter='\t'))
 assert len(fault)==1 and fault[0]['error_type']=='java.lang.NullPointerException'and 'privateShops'in fault[0]['error_message']and fault[0]['encrypted_memory_bytes']=='4'and not fault[0]['cleanup_error']
 ports=run/'controlled-negative-final-ports.tsv';ports.write_text('owner\tmethod\tphase\nNativeControlledNegativeMaterial\tcast\tinstall after original first successful NPC capture, before peer wait\nNativeControlledNegativeMaterial\tlambda$negative\twrap complete original producer; clear viewer scope before deferred final wait\nNativeSourceTargetNegativeLife\tclose\tclear producing scope backstop\nNativeSourceWeaponFinalFrame\tcapture\ttyped immutable controlled-negative output on original stage2\nNativeSourceWeaponFinalFrame\tframe;allow\ttyped original final monitor routing\nNativeAreaMaterialAction$Context\tnativeSourceControlledNegativeFinal\ttransient producing slot only\n','utf-8')
 v.update(status='built-common-controlled-negative-final-program-unqualified',runtime=pin(jar),log=pin(log),inherited_same_SHA_scenarios=15910,inherited_same_SHA_plan_receipts=receipts,pure_mass_teleport_exit_contract_scenarios=193,controlled_negative_final_assertions=695,controlled_negative_final_monitor_waits=78,controlled_NPC_pair_assertions=456,controlled_NPC_pair_waits=60,PC_pair_assertions=312,PC_pair_waits=42,controlled_negative_final_port_census=pin(ports),controlled_negative_final_artifacts=artifacts,material_configured_ingress_outcomes=artifacts['configured-material-ingress-results.tsv'],material_native69_exception_exit=pin(run/'native69-exception-exit.tsv'),all_material_effects_qualified=False,actual_Ctrl_cursor_qualified=False,parent_direct_lock_census=pin(run/'parent-direct-lock-census.tsv'),candidate_direct_lock_census=pin(run/'candidate-direct-lock-census.tsv'),test_only_forced_tie_fixture=pin(tie),test_only_forced_tie_fixture_checks=7,javac_argument_files=[pin(p)for p in argfiles],fresh_plan_closure_has_parent_classpath=False)
 assert v['test_only_forced_tie_fixture']['sha256']!=v['runtime']['sha256']
 if replay:assert v['runtime']['sha256']==seed['runtime']['sha256']
except BaseException as e:
 v.update(status='failed-frozen-common-controlled-negative-final-build',error=repr(e),log=pin(log));write(run/'manifest.json',v);print(json.dumps(dict(status=v['status'],error=v['error'],manifest=pin(run/'manifest.json')),ensure_ascii=False));raise
write(run/'manifest.json',v)
write(D/('source-controlled-negative-final-common-reproduction-current.json'if replay else 'source-controlled-negative-final-build-current.json'),dict(status='passed-frozen-byte-identical-common-build'if replay else v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],ordinary_inherited_checks=16103,new_final_assertions=695,NPC_pair_assertions=456,PC_pair_assertions=312,separate_forced_tie_checks=7,manifest=pin(run/'manifest.json')),ensure_ascii=False))
