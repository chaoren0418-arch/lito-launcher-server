"""Replay affected original fixture bodies on the exact role-identity candidate; isolate every receipt."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
BUILD='companion-portal-identity-build-current.json'
builds={'companion-area-scene-build-current.json','companion-target-build-current.json','companion-resurrection-scene-build-current.json','companion-negative-scene-build-current.json','companion-earth-scene-build-current.json','companion-nature-scene-build-current.json'}
def rename(value):
 if value==BUILD:return value
 if value in builds:return BUILD
 if value.startswith('companion-'):return 'on-companion-role-'+value[len('companion-'):]
 if value.startswith('on-companion-area-gates-'):return 'on-companion-role-area-gates-'+value[len('on-companion-area-gates-'):]
 if value.startswith('on-nature-stage-'):return 'on-companion-role-nature-stage-'+value[len('on-nature-stage-'):]
 return value
def adapt(text,source):
 class Select(ast.NodeTransformer):
  def visit_Call(self,node):
   self.generic_visit(node)
   if isinstance(node.func,ast.Name) and node.func.id=='Path' and len(node.args)==1 and isinstance(node.args[0],ast.Name) and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
   return node
  def visit_Dict(self,node):
   # Mapping keys describe unchanged original scripts; only selected destinations change.
   node.values=[self.visit(v) for v in node.values];return node
  def visit_Constant(self,node):
   if isinstance(node.value,str):
    if source.name=="test_companion_nature_stage_regressions.py" and node.value=="test_companion_death_publication.py":node.value="test_companion_role_death_publication.py"
    else:node.value=rename(node.value)
   return node
 tree=Select().visit(ast.parse(text))
 if source.name=='test_companion_queued_targets.py':
  loops=[i for i,n in enumerate(tree.body) if isinstance(n,ast.For) and any(isinstance(x,ast.Call) and isinstance(x.func,ast.Attribute) and isinstance(x.func.value,ast.Name) and x.func.value.id=='shutil' and x.func.attr=='copyfile' for x in ast.walk(n))]
  assert len(loops)==1
  patch=ast.parse('''_role_probe=inputs/'CompanionQueuedTargetProbe.java'
_role_text=_role_probe.read_text('utf-8')
assert _role_text.count('frame.getMethodName().equals("run")')==1
_role_probe.write_text(_role_text.replace('frame.getMethodName().equals("run")','frame.getMethodName().equals("nativeCompanionAreaCapturedRun")'),'utf-8')
''').body
  tree.body[loops[0]+1:loops[0]+1]=patch
 return ast.unparse(ast.fix_missing_locations(tree))+'\n'
build=read(D/BUILD);assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']
run=D/'companion-role-regressions'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name)
specs=[('test_companion_area_scene.py','companion-area-scene-after',16),('test_companion_area_secondary.py','companion-area-secondary-after',16),('test_companion_area_lifecycle.py','companion-area-lifecycle-after',64),('test_companion_area_publication.py','companion-area-publication-after',32),('test_companion_area_secondary_retirement.py','companion-area-secondary-retirement-after',16),('test_companion_actor_targets.py','companion-actor-target-scene',24),('test_companion_queued_targets.py','companion-queued-target-lifecycle',36),('test_companion_area_regressions.py','companion-area-regressions',800),('test_companion_resurrection_regressions.py','companion-resurrection-regressions',700),('test_companion_negative_regressions.py','companion-negative-regressions',792),('test_companion_earth_regressions.py','companion-earth-regressions',208),('test_companion_nature_regressions.py','companion-nature-regressions',180),('test_companion_nature_stage_regressions.py','companion-nature-stage-regressions',124),('test_companion_area_gates.py','companion-area-gates-regressions',332)]
v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],results=[],driver_adaptation='Runtime selection, receipt namespaces and original Path(__file__) resolution. Nested source mapping keys stay unchanged. Only the frozen queued-target stack probe names nativeCompanionAreaCapturedRun instead of run; its LockSupport park and all behavior/packet/role/cost assertions remain. Frozen death publication probe saves the actual pre-close Scope to assert closed/empty/weak-owner-cleared plus Context scope pointer null; all semantic assertions remain. Original Java and fixed weapon-break/source/golden paths are retained.',full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);log=run/'runner.log';log.write_text('Preparing selected existing drivers.\n','utf-8')
def verify(value):
 if isinstance(value,dict):
  if 'path'in value and 'sha256'in value:assert sha(Path(value['path']))==value['sha256'],value['path']
  for x in value.values():verify(x)
 elif isinstance(value,list):
  for x in value:verify(x)
try:
 for name,receipt,cases in specs:
  source=R/name;text=adapt(source.read_text('utf-8'),source);assert BUILD in text
  original_fixed={n.value for n in ast.walk(ast.parse(source.read_text('utf-8'))) if isinstance(n,ast.Constant) and isinstance(n.value,str) and n.value.startswith('weapon-break-')};selected={n.value for n in ast.walk(ast.parse(text)) if isinstance(n,ast.Constant) and isinstance(n.value,str)};assert original_fixed<=selected
  driver=inputs/name;driver.write_text(text,'utf-8');index=D/(rename(receipt)+'-current.json');log=run/(name+'.log');print('RUN '+name,flush=True)
  with log.open('w',encoding='utf-8') as out:
   if index.exists() and read(index)['status'].startswith('passed') and read(index)['runtime']==build['runtime']:out.write('Reused immutable hash-verified receipt for this exact runtime.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',str(driver)],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1500)
  current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'];verify(current);m=read(Path(current['manifest']['path']));verify(m)
  o=m['observations'] if 'observations'in m else dict(cases=m['cases'],checks=m.get('checks',m.get('known_checks')));assert o['cases']==cases
  frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(original=pin(source),adapted=pin(driver),receipt=pin(frozen),manifest=current['manifest'],observations=dict(cases=cases,checks=o.get('checks')),log=pin(log)));write(run/'manifest.json',v);print('PASS '+name+' cases='+str(cases),flush=True)
 v.update(status='passed-same-SHA-3340-existing-role-actor-material-stage-and-Main-scope',cases=sum(r['observations']['cases'] for r in v['results']),known_checks=sum(r['observations']['checks'] or 0 for r in v['results']),check_count_complete=False);assert v['cases']==3340
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-role-regressions-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
