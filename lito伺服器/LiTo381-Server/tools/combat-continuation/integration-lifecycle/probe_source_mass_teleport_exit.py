"""Build a bounded cumulative candidate and test original mass-teleport exit ports, never deploy."""
from pathlib import Path
from datetime import datetime,timezone
import ast,collections,csv,hashlib,json,os,re,shutil,subprocess,sys,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref;return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
replay=len(sys.argv)>1 and sys.argv[1]=='--replay'
if replay:
 previous=read(sys.argv[2]);base=checked(previous['parent_runtime']);parent=read(checked(previous['parent_build']));gap=read(checked(previous['negative_configured_ingress']));origin={Path(x['path']).name:checked(x)for x in previous['inputs']}
else:
 progress=read(D/'source-program-update-progress-current.json');base=checked(progress['runtime']);current=read(checked(progress['manifest']));parent=read(checked(current['build']));gap_index=read(D/'source-material-configured-ingress-current.json');gap_path=checked(gap_index['manifest']);gap=read(gap_path);assert gap['runtime']==progress['runtime'];origin=None
assert pin(base)['sha256']=='07a334d7aa8da829af7e4b5543ac12e305b07541706e2b9ae254741766397d9b'
parent_dir=base.parent;run=D/'source-mass-teleport-exit-probes'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
names=['NativeSourceMassTeleportExit.java','SourceMassTeleportExitMethods.java','SourceMassTeleportExitDelta.java','SourceMassTeleportExitPlan.java','SourceMaterialConfiguredIngressPlan.java',Path(__file__).name]
for name in names:shutil.copyfile(origin[name]if origin else R/name,inputs/name)
gap_inputs={Path(x['path']).name:checked(x)for x in gap['inputs']}
for name in ['material-cases.tsv','target-metadata.tsv','skill.properties','damage.properties','character.properties']:shutil.copyfile(gap_inputs[name],inputs/name)
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
asm=[checked(x)for x in parent['asm']];tools_cp=';'.join(map(str,[run/'tools',parent_dir/'tools',*asm]));classes=run/'classes';jar=run/'candidate-source-mass-teleport-exit-unqualified.jar';log=run/'probe.log';argfiles=[];inherited=[]
v=dict(status='running-not-qualified',parent_runtime=pin(base),parent_build=previous['parent_build']if replay else current['build'],negative_configured_ingress=previous['negative_configured_ingress']if replay else pin(gap_path),inputs=[pin(p)for p in inputs.iterdir()],new_SQL_connections=0,Source_or_player_SQL_or_WAL_written=False,real_backend_started=False,formal_deployment_changed=False,qualified_for_release=False,full_goal_achieved=False)
def invoke(a,out):
 a=list(map(str,a))
 if Path(a[0]).name=='javac.exe':
  p=run/('javac-'+str(len(argfiles)+1)+'.args');p.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in a[1:]),'utf-8');argfiles.append(p);a=[a[0],'@'+str(p)]
 subprocess.run(a,cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
try:
 with log.open('w',encoding='utf-8')as out:
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tools_cp,'-d',run/'tools',inputs/'SourceMassTeleportExitMethods.java',inputs/'SourceMassTeleportExitDelta.java'],out)
  invoke([J/'java.exe','-Xverify:all','-cp',tools_cp,'SourceMassTeleportExitMethods','splice',base,classes,S/'lib'],out)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[classes,base,S/'lib/*'])),'-d',classes,inputs/'NativeSourceMassTeleportExit.java'],out)
  with zipfile.ZipFile(base)as z:entries={n:z.read(n)for n in z.namelist()}
  for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED)as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tools_cp,'SourceMassTeleportExitMethods','verify',base,jar,S/'lib'],out)
  invoke([J/'java.exe','-Xverify:all','-cp',tools_cp,'SourceMassTeleportExitDelta',base,jar],out)
  cp=';'.join(map(str,[run/'plan',jar,parent_dir/'plan',S/'lib/*']))
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'plan',inputs/'SourceMassTeleportExitPlan.java',inputs/'SourceMaterialConfiguredIngressPlan.java'],out)
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceMassTeleportExitPlan'],out)
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceMaterialConfiguredIngressPlan',inputs/'material-cases.tsv',run/'configured-material-ingress-results.tsv',inputs/'target-metadata.tsv',inputs],out)
  with (run/'configured-material-ingress-results.tsv').open(encoding='utf-8')as f:configured=list(csv.DictReader(f,delimiter='\t'))
  assert len(configured)==232 and all(not r['error_type']and not r['cleanup_error']for r in configured),'configured native ingress/cleanup exceptions remain'
  closed=[r for r in configured if r['mode']in ('Main','STOPPED')]
  assert len(closed)==116 and all(r['remaining_mp']=='2000'and r['encrypted_memory_bytes']=='0'and r['status']=='rejected-or-no-output'for r in closed),'all closed material ingress must remain inert'
  invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceMaterialConfiguredIngressPlan',inputs/'material-cases.tsv',run/'native69-exception-exit.tsv',inputs/'target-metadata.tsv',inputs,'69','exit-exception'],out)
  with (run/'native69-exception-exit.tsv').open(encoding='utf-8')as f:fault=list(csv.DictReader(f,delimiter='\t'))
  assert len(fault)==1 and fault[0]['error_type']=='java.lang.NullPointerException'and 'privateShops'in fault[0]['error_message']and fault[0]['encrypted_memory_bytes']=='4'and not fault[0]['cleanup_error'],'original native body exception must survive exit publication, with one original unlock frame'
  # Reuse frozen plan/oracle classes, never recompile historical runtime helper sources.
  builder=next(checked(x)for x in parent['inputs']if Path(x['path']).name=='build_source_self_final_frame.py')
  tree=ast.parse(builder.read_text('utf-8'))
  scope=dict(J=J,run=run,jar=jar,S=S,inputs=parent_dir/'inputs',cp=cp,tie_fixture=parent_dir/'test-only-forced-tie.jar')
  for node in ast.walk(tree):
   if not(isinstance(node,ast.Call)and isinstance(node.func,ast.Name)and node.func.id=='invoke'and node.args):continue
   constants=[x.value for x in ast.walk(node.args[0])if isinstance(x,ast.Constant)and isinstance(x.value,str)]
   plans=[x for x in constants if x.startswith('local.lito381.server.')]
   if len(plans)!=1 or any('forced-tie' in x for x in constants):continue
   args=eval(compile(ast.Expression(node.args[0]),str(builder),'eval'),{'__builtins__':{'str':str,'map':map}},scope)
   cpi=args.index('-cp')+1;args[cpi]=cp
   invoke(args,out);inherited.append(plans[0])
 v.update(status='passed-cumulative-mass-teleport-exit-program-probe-not-promoted',runtime=pin(jar),inherited_plan_runs=inherited,inherited_counts_not_recounted=True,log=pin(log),material_ingress_results=pin(run/'configured-material-ingress-results.tsv'),native_exception_exit=pin(run/'native69-exception-exit.tsv'),configured_native_outcomes=dict(collections.Counter(r['status']for r in configured)),configured_attempts=232,closed_material_attempts=116,full_material_semantics_qualified=False,actual_Ctrl_or_cursor_qualified=False,synthetic_fixture_limits=['NPC123 CON/WIS=10','summon memory-fixture rows; actual Source SQL data not loaded','DragonKnightAwakeClassicMode=0; source_map_directory=None'],new_contract_checks=int(re.search(r'PASS(\d+) mass-teleport exit original-lock',log.read_text('utf-8'))[1]),source_lock_waits=13,final_monitor_waits=13,nested_original_root_cases=5)
 if replay:assert v['runtime']['sha256']==previous['runtime']['sha256'],'deterministic byte-for-byte replay'
except BaseException as e:
 v.update(status='failed-frozen-probe-not-promoted',error=repr(e),log=pin(log),inherited_plan_runs=inherited)
 write(run/'manifest.json',v);print(json.dumps(dict(status=v['status'],error=v['error'],manifest=pin(run/'manifest.json')),ensure_ascii=False));raise
write(run/'manifest.json',v)
if not replay:write(D/'source-mass-teleport-exit-probe-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],new_checks=v['new_contract_checks'],inherited_plans=len(inherited),manifest=pin(run/'manifest.json')),ensure_ascii=False))
