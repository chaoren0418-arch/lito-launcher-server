"""Retain prior50/80/87/HP/Main/area native assertions; Source194 NPC expectations have a separate oracle-backed run."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];spec={}
for k,n in {'pc50-stage':16,'pc50-pre':64,'pc50-post':64,'pc50-duration':8,'pc50-io':32,'pc50-relations':16,'pc80-stage':8,'pc80-pre':36,'pc80-post':48,'pc80-duration':4,'pc80-io':16,'pc80-producer':12,'pc80-action':4,'hp':26,'hp-main':2,'shock-mutation':64,'shock-publication':64,'shock-death':8,'gates':352,'area':232,'selector':236}.items():spec[k]=('test_freeze_source_retained.py',[k],'source167-retained-prior-prior-prior-retained-'+k,n)
for k,n in {'stage':16,'boundary':64,'completion':64,'duration':8,'io':32,'relations':16}.items():spec['194-'+k]=('test_freeze_source_projection.py',[k],'source167-retained-prior-prior-prior-projection-'+k,n)
for k,file,receipt,n in [('194-shape','test_freeze_source_target_shape.py','source167-retained-prior-prior-prior-target-shape-after',8),('194-range','test_freeze_source_range.py','source167-retained-prior-prior-prior-range-after',8),('194-range-boundary','test_freeze_source_range_boundary.py','source167-retained-prior-prior-prior-range-boundary',16),('npc','test_freeze_source_npc.py','source167-retained-prior-prior-prior-NPC-source-selector',126)]:spec[k]=(file,[],receipt,n)
for k,file,receipt,n in [('range','test_source_material_range.py','source167-retained-prior-range-range-after',24),('screen-reject','test_source_material_screen_reject.py','source167-retained-prior-range-screen-reject',24),('screen-boundary','test_source_material_screen_boundary.py','source167-retained-prior-range-screen-boundary',192),('range-pair','test_source_material_range_pair.py','source167-retained-prior-range-range-pair',24),('npc-range','test_source_material_npc_range.py','source167-retained-prior-range-NPC-range',144)]:spec[k]=(file,[],receipt,n)
file,args,receipt,expected=spec[kind];source=R/file;build=read(D/'source-wind-target-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/('source167-retained-prior-prior-retained-'+kind)/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+file))
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if node.value=='freeze-source-metadata-current.json':return node
  if node.value in ['freeze-source-target-build-current.json','source-visibility-range-build-current.json']:return ast.copy_location(ast.Constant('source-wind-target-build-current.json'),node)
  if isinstance(node.value,str):node.value=node.value.replace('freeze-source-','source167-retained-prior-prior-prior-').replace('source-material-','source167-retained-prior-range-')
  return node
driver=inputs/file;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),behavior_or_goldens_changed=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
 i=read(D/(receipt+'-current.json'));assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m.get('observations',m);assert o['cases']==expected;shutil.copyfile(D/(receipt+'-current.json'),run/'selected-receipt.json');v.update(status='passed-exact-SHA-retained-'+kind,cases=expected,known_checks=o.get('checks',o.get('known_checks')),receipt=pin(run/'selected-receipt.json'),selected_manifest=i['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2400:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('source167-retained-prior-prior-retained-'+kind+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=expected)))
