"""Select the exact new JAR and independent receipt namespaces; original Java/Source gates stay frozen."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,sys,shutil,ast
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'blind-scene-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];gates='--gates'in sys.argv;name='blind-scene-gates'if gates else'blind-scene-area-regressions';source=R/('test_companion_wind_gates.py'if gates else'test_companion_wind_regressions.py');run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+source.name))
tree=ast.parse(source.read_text('utf-8'))
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if isinstance(node.value,str):node.value=node.value.replace('companion-wind-','blind-scene-').replace('review_companion_wind_source.py','review_blind_scene_source.py')
  return node
tree=Select().visit(tree);text=ast.unparse(ast.fix_missing_locations(tree))+'\n';assert 'blind-scene-build-current.json'in text
# The strict Source220b inverse review is a sealed input, not a receipt namespace.
assert 'review_blind_scene_source.py'in text or not gates
driver=inputs/source.name;driver.write_text(text,'utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],source=pin(source),adapted=pin(driver),fixture_behavior_or_goldens_changed=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver)],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=2200)
 result=read(D/(name+'-current.json'));assert result['status'].startswith('passed')and result['runtime']==build['runtime'];assert sha(Path(result['manifest']['path']))==result['manifest']['sha256'];observed=read(Path(result['manifest']['path']));assert observed['cases']==(352 if gates else 232);frozen=run/'selected-receipt.json';shutil.copyfile(D/(name+'-current.json'),frozen);v.update(status='passed-exact-SHA-352-Ref-pair-area-Main68-73'if gates else'passed-exact-SHA-232-original-161-material-NPC-area-lifecycle',cases=observed['cases'],known_checks=observed.get('known_checks',observed.get('checks')),receipt=pin(frozen),selected_manifest=result['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'adapter-manifest.json',v);write(D/(name+'-adapter-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'adapter-manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
