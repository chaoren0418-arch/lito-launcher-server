"""Run existing native regressions with only runtime/receipt selectors changed, never fixture behavior."""
from pathlib import Path
from datetime import datetime,timezone
import ast,hashlib,json,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];specs={'pc50-stage':('test_freeze_accepted_stage.py',[],'area-freeze-prior-stage',16),'pc50-pre':('test_freeze_accepted_boundaries.py',[],'area-freeze-prior-boundaries',64),'pc50-post':('test_freeze_accepted_completion.py',[],'area-freeze-prior-completion',64),'pc50-io':('test_freeze_accepted_io.py',[],'area-freeze-prior-IO-after',32),'pc50-duration':('test_freeze_accepted_duration.py',[],'area-freeze-prior-duration-after',8),'pc50-relations':('test_freeze_accepted_relations.py',[],'area-freeze-prior-relations',16),'npc':('test_freeze_accepted_npc_prior.py',[],'area-freeze-prior-NPC-originals',126)}
for k in ['hp','hp-main','shock-mutation','shock-publication','shock-death','gates','area']:
 specs[k]=('test_freeze_accepted_prior.py',[k],'area-freeze-prior-retained-'+k,{'hp':26,'hp-main':2,'shock-mutation':64,'shock-publication':64,'shock-death':8,'gates':352,'area':232}[k])
specs['selector']=('test_selector_only_ctrl.py',[],'area-freeze-prior-selector-only-ctrl',236)
file,args,receipt,expected=specs[kind];source=R/file;build=read(D/'area-freeze-accepted-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/('area-freeze-retained-'+kind)/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+file))
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  if isinstance(node.func,ast.Attribute)and node.func.attr=='replace'and node.args and isinstance(node.args[0],ast.Constant)and node.args[0].value=='shock-player-':node.func.value=ast.Call(func=ast.Attribute(value=node.func.value,attr='replace',ctx=ast.Load()),args=[ast.Constant('shock-player-build-current.json'),ast.Constant('area-freeze-accepted-build-current.json')],keywords=[])
  return node
 def visit_Constant(self,node):
  if node.value in ['freeze-accepted-build-current.json','companion-forest-area-build-current.json']:return ast.copy_location(ast.Constant('area-freeze-accepted-build-current.json'),node)
  if isinstance(node.value,str):node.value=node.value.replace('freeze-accepted-','area-freeze-prior-').replace('selector-only-ctrl','area-freeze-prior-selector-only-ctrl')
  return node
driver=inputs/file;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),behavior_or_goldens_changed=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
 index=D/(receipt+'-current.json');i=read(index);assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m.get('observations',m);assert o['cases']==expected;frozen=run/'selected-receipt.json';shutil.copyfile(index,frozen);v.update(status='passed-exact-SHA-retained-'+kind,cases=expected,known_checks=o.get('checks',o.get('known_checks')),receipt=pin(frozen),selected_manifest=i['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('area-freeze-retained-'+kind+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=expected)))
