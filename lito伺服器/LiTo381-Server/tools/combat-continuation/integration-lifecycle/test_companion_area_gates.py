"""Reuse frozen regression bodies with only candidate/index and receipt-directory selection changed."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys,ast
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def adapt(text,source):
 class Select(ast.NodeTransformer):
  def visit_Call(self,node):
   self.generic_visit(node)
   if isinstance(node.func,ast.Name) and node.func.id=='Path' and len(node.args)==1 and isinstance(node.args[0],ast.Name) and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
   return node
  def visit_Assign(self,node):
   self.generic_visit(node)
   if source.name=="test_pair.py" and len(node.targets)==1 and isinstance(node.targets[0],ast.Name) and node.targets[0].id=="original" and isinstance(node.value,ast.Call) and isinstance(node.value.func,ast.Name) and node.value.func.id=="pin":
    assert ast.unparse(node.value)=="pin(p)"
    node.value=ast.parse("pin(B / 'NativePairMaterialBoundaryProbe.java')",mode="eval").body
   return node
  def visit_Constant(self,node):
   value=node.value
   if isinstance(value,str):
    if value=='weapon-target-build-current.json':node.value='companion-area-scene-build-current.json'
    elif value.startswith('on-weapon-'):node.value='on-companion-area-gates-'+value[len('on-weapon-'):]
    elif value.startswith('weapon-') and not value.startswith('weapon-break-'):node.value='on-companion-area-gates-'+value[len('weapon-'):]
   return node
 return ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(text))))+'\n'
build=read(D/'companion-area-scene-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'companion-area-gates-regressions'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name)
specs=[('review_source.py',['--weapon'],'on-companion-area-gates-source-drift-review',0),('test_pair.py',['--weapon'],'on-companion-area-gates-pair-tests',288),('test_npc_physical_area_lifecycle.py',['--weapon'],'on-companion-area-gates-npc-physical-area-lifecycle',26),('test_pair_main.py',['--weapon'],'on-companion-area-gates-pair-Main',10),('test_skill73_main.py',['--weapon'],'on-companion-area-gates-skill73-Main',8)]
v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],results=[],driver_adaptation='Only candidate/index selection, receipt directory names and original driver path resolution; all fixture bodies and assertions retained. Pair original-source pin names the unchanged canonical source instead of the temporary adapted path.',full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
log=run/'runner.log';log.write_text('Preparing selected drivers.\n','utf-8')
try:
 for name,args,receipt,cases in specs:
  source=R/name;original=source.read_text('utf-8');text=adapt(original,source);assert text!=original and 'companion-area-scene-build-current.json' in text;driver=inputs/name;adapted_ast=ast.parse(text);fixed={n.value for n in ast.walk(ast.parse(original)) if isinstance(n,ast.Constant) and isinstance(n.value,str) and n.value.startswith('weapon-break-')};selected={n.value for n in ast.walk(adapted_ast) if isinstance(n,ast.Constant) and isinstance(n.value,str)};assert fixed<=selected,'fixed fixture/qualification paths must stay unchanged';driver.write_text(text,'utf-8');log=run/(name+'.log');print('RUN '+name,flush=True)
  index=D/(receipt+'-current.json');reuse=index.exists() and read(index)['status'].startswith('passed') and read(index)['runtime']==build['runtime']
  with log.open('w',encoding='utf-8') as out:
   if reuse:out.write('Reused hash-verified immutable receipt for the exact same runtime.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  index=D/(receipt+'-current.json');current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'];assert sha(Path(current['manifest']['path']))==current['manifest']['sha256'];m=read(Path(current['manifest']['path']));observations=dict(cases=sum(row['results']['cases'] for row in m['results'] if row['runtime']==build['runtime']),checks=sum(row['results']['checks'] for row in m['results'] if row['runtime']==build['runtime'])) if name=='test_pair.py' else dict(cases=m['cases'],checks=m['checks']) if name=='replay_weapon_targets.py' else dict(cases=len(m['results']),checks=None) if name.endswith('_main.py') else dict(cases=0,checks=0) if name=='review_source.py' else m['observations'];assert observations['cases']==cases;frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(original=pin(source),adapted=pin(driver),receipt=pin(frozen),manifest=current['manifest'],observations=observations,log=pin(log)));write(run/'manifest.json',v);print('PASS '+name+' cases='+str(cases),flush=True)
 v.update(status='passed-same-SHA-332-pair-area-and-Main-scope',cases=sum(row['observations']['cases'] for row in v['results']),known_checks=sum(row['observations']['checks'] or 0 for row in v['results']),check_count_complete=False)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-area-gates-regressions-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
