"""Exact fixture/runtime selection over the existing native state driver; no production overlays."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];before='--before'in sys.argv;assert kind in ['visible','reject']and not(before and kind=='reject')
source=R/'test_source_negative_target_native.py';selected=read(D/('source-negative-target-build-current.json'if before else'erase-source-range-build-current.json'));assert sha(Path(selected['runtime']['path']))==selected['runtime']['sha256'];run=D/('source-negative-range-'+kind+('-before'if before else'-after'))/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+source.name))
cls='SourceNegativeVisibleRangeProbe'if kind=='visible'else'SourceNegativeScreenRejectProbe'
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Assign(self,node):
  self.generic_visit(node)
  if len(node.targets)==1 and isinstance(node.targets[0],ast.Name):
   if node.targets[0].id=='cls':node.value=ast.Constant(cls)
   if node.targets[0].id=='name':node.value=ast.Constant('source-negative-range-'+kind+('-before'if before else'-after'))
  return node
 def visit_Constant(self,node):
  if node.value=='source-negative-failure-fixtures':node.value='source-negative-range-fixtures'
  elif node.value=='selected-player-intent-build-current.json':node.value='source-negative-target-build-current.json'
  elif node.value=='source-negative-target-build-current.json':node.value='erase-source-range-build-current.json'
  elif node.value==216:node.value=108
  return node
driver=inputs/source.name;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=selected['runtime'],source=pin(source),adapted=pin(driver),full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),'state',*(['--before']if before else[])],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
 i=read(D/('source-negative-range-'+kind+('-before'if before else'-after')+'-current.json'));assert i['runtime']==selected['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m['observations'];assert o['cases']==108;shutil.copyfile(D/('source-negative-range-'+kind+('-before'if before else'-after')+'-current.json'),run/'selected-receipt.json');v.update(status='observed-Source153-visible-admission-gap'if before else'passed-Source29-152-153-'+kind,observations=o,selected_manifest=i['manifest'],receipt=pin(run/'selected-receipt.json'))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-5000:]);raise
finally:v['log']=pin(log);write(run/'adapter-manifest.json',v);write(D/('source-negative-range-adapter-'+kind+('-before'if before else'-after')+'-current.json'),dict(status=v['status'],runtime=selected['runtime'],manifest=pin(run/'adapter-manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
