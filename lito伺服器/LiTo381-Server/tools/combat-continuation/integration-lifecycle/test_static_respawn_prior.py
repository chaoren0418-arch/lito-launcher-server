"""Replay prior native bodies on the exact respawn SHA; only runtime and receipt namespaces change."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];specs={'motion':('test_static_replacement_motion_prior.py',['motion'],'static-respawn-prior-motion-adapter',8),'motion-life':('test_static_replacement_motion_prior.py',['lifecycle'],'static-respawn-prior-motion-lifecycle-adapter',24),'originals':('test_static_replacement_originals.py',[],'static-respawn-originals',288),'hp':('test_shock_player_hp_prior.py',[],'static-respawn-hp-originals',26),'hp-main':('test_shock_player_hp_main.py',[],'static-respawn-hp-main',2),'review':('review_blind_scene_source.py',['--static'],'on-static-respawn-source-drift-review',0),'main68':('test_pair_main.py',['--static'],'on-static-respawn-pair-Main',10),'main73':('test_skill73_main.py',['--static'],'on-static-respawn-skill73-Main',8),'gates':('test_controlled_negative_gates.py',[],'static-respawn-gates-adapter',352),'area':('test_controlled_negative_gates.py',['--area'],'static-respawn-area-regressions-adapter',232)}
file,args,receipt,expected=specs[kind];source=R/file;build=read(D/'static-respawn-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];name='static-respawn-retained-'+kind;run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+file))
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if kind in ['motion','motion-life']and node.value=='static-motion-build-current.json':return node
  if isinstance(node.value,str):
   node.value=node.value.replace('static-motion-build-current.json','static-respawn-build-current.json').replace('static-replacement-','static-respawn-').replace('shock-player-','static-respawn-').replace('controlled-negative-material-build-current.json','static-respawn-build-current.json').replace('controlled-negative-','static-respawn-')
   if node.value.startswith('on-static-'):node.value=node.value.replace('on-static-','on-static-respawn-',1)
  return node
driver=inputs/file;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),behavior_or_goldens_changed_by_this_adapter=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
 index=D/(receipt+'-current.json');i=read(index);assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m.get('observations',m);cases=0 if kind=='review'else len(m['results'])if kind in ['main68','main73']else o['cases'];assert cases==expected
 if kind in ['main68','main73']:assert all(r['runtime']==build['runtime']for r in m['results'])
 frozen=run/'selected-receipt.json';shutil.copyfile(index,frozen);v.update(status='passed-exact-SHA-retained-'+kind,cases=cases,known_checks=o.get('checks',o.get('known_checks')),receipt=pin(frozen),selected_manifest=i['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2000:]);raise
finally:v['log']=pin(log);write(run/'adapter-manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'adapter-manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
