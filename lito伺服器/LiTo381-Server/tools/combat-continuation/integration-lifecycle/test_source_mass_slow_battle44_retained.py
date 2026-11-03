"""Retain all BATTLE44 fixture bodies on Source76 candidate; change only runtime and receipt namespace."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1]
file,receipt,expected={
 'boundary':('test_battle_controlled_cancellation_boundary.py','boundary',420),
 'publication':('test_battle_controlled_cancellation_publication.py','publication',84),
 'io':('test_battle_controlled_cancellation_io.py','io',72),
 'faults':('test_battle_controlled_cancellation_faults.py','material-faults',24),
}[kind]
build=read(D/'source-mass-slow-target-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']
source=R/file;run=D/('source76-battle44-retained-'+kind)/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),source]:shutil.copyfile(p,inputs/p.name)
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if node.value=='battle-controlled-cancellation-build-current.json':node.value='source-mass-slow-target-build-current.json'
  elif isinstance(node.value,str):node.value=node.value.replace('battle-controlled-cancellation-','mass-slow-battle44-')
  return node
driver=inputs/('selected-'+file);driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8')
v=dict(status='running',runtime=build['runtime'],original_driver=pin(source),selected_driver=pin(driver),fixture_behavior_or_goldens_changed=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);log=run/'test.log'
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver)],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
 i=read(D/('mass-slow-battle44-'+receipt+'-current.json'));assert i['runtime']==build['runtime']and i['status'].startswith('passed')and sha(Path(i['manifest']['path']))==i['manifest']['sha256']
 m=read(Path(i['manifest']['path']));o=m['observations'];assert o['cases']==expected
 v.update(status='passed-exact-SHA-original-BATTLE44-'+kind,observations=o,selected_manifest=i['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:
 v['log']=pin(log);write(run/'manifest.json',v);write(D/('source76-battle44-retained-'+kind+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=expected)))
