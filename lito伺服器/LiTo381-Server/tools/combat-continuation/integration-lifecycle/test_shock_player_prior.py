"""Replay sealed prior bodies on the new runtime; change only paths/receipt namespace."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];direct={
 'companion-admission':('test_shock_companion_admission.py','pc87-prior-companion-admission-after',4),
 'companion-boundaries':('test_shock_companion_boundaries.py','pc87-prior-companion-boundaries',276),
 'companion-publication':('test_shock_companion_publication.py','pc87-prior-companion-publication-after',60),
 'companion-controls':('test_shock_companion_controls.py','pc87-prior-companion-controls',96),
 'companion-death-bags':('test_shock_companion_death_bags.py','pc87-prior-companion-death-bags',24)}
if kind in direct:file,receipt,expected=direct[kind];args=[]
else:assert kind in ['gap','actors','life','publication','fault','originals','gates','area'];file='test_shock_prior_regressions.py';args=[kind];receipt='pc87-prior-negative-'+kind+'-adapter';expected=None
source=R/file;build=read(D/'shock-player-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];name='shock-player-prior-'+kind;run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+file))
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if isinstance(node.value,str):node.value=node.value.replace('shock-companion-build-current.json','shock-player-build-current.json').replace('shock-companion-','pc87-prior-companion-').replace('shock-prior-negative-','pc87-prior-negative-')
  return node
driver=inputs/file;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),fixture_behavior_or_goldens_changed=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=2400)
 index=D/(receipt+'-current.json');i=read(index);assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m.get('observations',m)
 if expected is not None:assert o['cases']==expected
 frozen=run/'selected-receipt.json';shutil.copyfile(index,frozen);v.update(status='passed-exact-SHA-original-'+kind,cases=o['cases'],known_checks=o.get('checks',o.get('known_checks')),receipt=pin(frozen),selected_manifest=i['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'adapter-manifest.json',v);write(D/(name+'-adapter-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'adapter-manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
