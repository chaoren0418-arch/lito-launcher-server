"""Direct exact-SHA legacy gates, avoiding nested baseline ancestry as an accidental policy oracle."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';B=S/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];spec={
 'review':('review_blind_scene_source.py',['--weapon'],'source-drift-review',0),
 'pair':('test_pair.py',['--weapon'],'pair-tests',288),
 'main68':('test_pair_main.py',['--weapon'],'pair-Main',10),
 'main73':('test_skill73_main.py',['--weapon'],'skill73-Main',8),
 'physical-area':('test_npc_physical_area_lifecycle.py',['--weapon'],'npc-physical-area-lifecycle',26),
 'ref-life':('test_companion_ref_lifecycle.py',[],'ref-lifecycle-after',10),
 'ref-feedback':('test_companion_ref_feedback.py',[],'ref-feedback-after',10),
}
name,args,receipt,expected=spec[kind];source=R/name;build=read(D/'source-kiringku-presentation-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']
run=D/('source-presentation-common-adapter-'+kind)/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+name))
projection=read(R/'competition-pair-fixtures/manifest.json')
for row in projection['rows']:
 for key in ['original','versioned']:assert sha(Path(row[key]['path']))==row[key]['sha256']
 text=Path(row['versioned']['path']).read_text('utf-8')
 for old,new in reversed(row['changes']):assert text.count(new)==1;text=text.replace(new,old)
 assert text==Path(row['original']['path']).read_text('utf-8')
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if isinstance(node.value,str):
   if node.value in ['weapon-target-build-current.json','companion-ref-scene-build-current.json','companion-portal-identity-build-current.json']:node.value='source-kiringku-presentation-build-current.json'
   elif node.value.startswith('on-weapon-'):node.value=node.value.replace('on-weapon-','source-presentation-common-',1)
   elif node.value.startswith('weapon-')and not node.value.startswith('weapon-break-'):node.value=node.value.replace('weapon-','source-presentation-common-',1)
   elif node.value.startswith('companion-ref-lifecycle-'):node.value=node.value.replace('companion-ref-lifecycle-','source-presentation-common-ref-lifecycle-',1)
   elif node.value.startswith('companion-ref-feedback-'):node.value=node.value.replace('companion-ref-feedback-','source-presentation-common-ref-feedback-',1)
  return node
tree=Select().visit(ast.parse(source.read_text('utf-8')))
if kind in ['pair','main68']:
 # Only copied Java fixtures change. Production classes, lock/cost/reuse/output assertions stay exact.
 patch=ast.parse("""_friendly_basis=read(Path(PROJECTION))
for _row in _friendly_basis['rows']:
 _target=inputs/Path(_row['versioned']['path']).name
 if _target.exists():
  assert sha(_target)==_row['original']['sha256']
  shutil.copyfile(_row['versioned']['path'],_target)
""".replace('PROJECTION',repr(str(R/'competition-pair-fixtures/manifest.json')))).body
 # Insert immediately after fixture-copy loop, before original fixture authority adaptations.
 loops=[i for i,n in enumerate(tree.body)if isinstance(n,ast.For)and any(isinstance(x,ast.Call)and isinstance(x.func,ast.Attribute)and isinstance(x.func.value,ast.Name)and x.func.value.id=='shutil'and x.func.attr=='copyfile'for x in ast.walk(n))]
 assert loops;tree.body[loops[0]+1:loops[0]+1]=patch
if kind=='pair':
 # Legacy comparator is the sealed pre-intent weapon runtime, never a newer policy parent.
 assignment=next(n for n in tree.body if isinstance(n,ast.Assign)and any(isinstance(t,ast.Name)and t.id=='old'for t in n.targets))
 assignment.value=ast.parse("Path(read(B/'native-item-weapon-material-progress-current.json')['runtime']['path'])",mode='eval').body
 at=tree.body.index(assignment);tree.body[at:at]=ast.parse("seed=dict(seed,base=read(B/'native-item-weapon-material-progress-current.json')['runtime'])").body
 for node in ast.walk(tree):
  if isinstance(node,ast.Assign)and any(isinstance(t,ast.Name)and t.id=='original'for t in node.targets)and isinstance(node.value,ast.Call)and isinstance(node.value.func,ast.Name)and node.value.func.id=='pin':node.value=ast.parse("pin(B/'NativePairMaterialBoundaryProbe.java')",mode='eval').body
driver=inputs/name;driver.write_text(ast.unparse(ast.fix_missing_locations(tree))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),friendly_fixture_projection=projection if kind in ['pair','main68']else None,production_PC_or_NPC_overlays=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
 index=D/('source-presentation-common-'+receipt+'-current.json');i=read(index);assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']))
 if kind=='pair':o=dict(cases=sum(x['results']['cases']for x in m['results']if x['runtime']==build['runtime']),checks=sum(x['results']['checks']for x in m['results']if x['runtime']==build['runtime']))
 elif kind.startswith('main'):assert all(x['runtime']==build['runtime']for x in m['results']);o=dict(cases=len(m['results']),checks=sum(x['results']['checks']for x in m['results']))
 elif kind=='review':o=dict(cases=0,checks=0)
 else:o=m['observations']
 assert o['cases']==expected;shutil.copyfile(index,run/'selected-receipt.json');v.update(status='passed-exact-SHA-common-'+kind,observations=o,selected_manifest=i['manifest'],receipt=pin(run/'selected-receipt.json'))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-8000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('source-presentation-common-adapter-'+kind+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
