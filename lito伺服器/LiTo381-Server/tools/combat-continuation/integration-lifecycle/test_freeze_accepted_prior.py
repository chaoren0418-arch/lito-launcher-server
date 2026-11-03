"""Replay prior native bodies on the exact respawn SHA; only runtime and receipt namespaces change."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];specs={'motion':('test_static_replacement_motion_prior.py',['motion'],'freeze-accepted-prior-motion-adapter',8),'motion-life':('test_static_replacement_motion_prior.py',['lifecycle'],'freeze-accepted-prior-motion-lifecycle-adapter',24),'originals':('test_static_replacement_originals.py',[],'freeze-accepted-originals',288),'hp':('test_shock_player_hp_prior.py',[],'freeze-accepted-hp-originals',26),'hp-main':('test_shock_player_hp_main.py',[],'freeze-accepted-hp-main',2),'review':('review_blind_scene_source.py',['--static'],'on-freeze-accepted-source-drift-review',0),'main68':('test_pair_main.py',['--static'],'on-freeze-accepted-pair-Main',10),'main73':('test_skill73_main.py',['--static'],'on-freeze-accepted-skill73-Main',8),'gates':('test_controlled_negative_gates.py',[],'freeze-accepted-gates-adapter',352),'area':('test_controlled_negative_gates.py',['--area'],'freeze-accepted-area-regressions-adapter',232)}
specs.update({'shock-mutation':('test_shock_accepted_mutation.py',[],'freeze-accepted-prior-shock-mutation-after',64),'shock-publication':('test_shock_accepted_publication.py',[],'freeze-accepted-prior-shock-publication',64),'shock-death':('test_shock_player_death_receipt.py',[],'freeze-accepted-death-receipt',8)})
file,args,receipt,expected=specs[kind];source=R/file;build=read(D/'freeze-accepted-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];name='freeze-accepted-retained-'+kind;run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+file))
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if kind in ['motion','motion-life']and node.value=='static-motion-build-current.json':return node
  if isinstance(node.value,str):
   node.value=node.value.replace('shock-accepted-build-current.json','freeze-accepted-build-current.json').replace('shock-accepted-','freeze-accepted-prior-shock-').replace('static-motion-build-current.json','freeze-accepted-build-current.json').replace('static-replacement-','freeze-accepted-').replace('shock-player-','freeze-accepted-').replace('controlled-negative-material-build-current.json','freeze-accepted-build-current.json').replace('controlled-negative-','freeze-accepted-')
   if node.value.startswith('on-static-'):node.value=node.value.replace('on-static-','on-freeze-accepted-',1)
  return node
driver=inputs/file;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),behavior_or_goldens_changed_by_this_adapter=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 if '--reuse-receipt'in sys.argv:
  previous=read(D/(name+'-current.json'));prior=read(Path(previous['manifest']['path']));assert prior['status']=='failed'and prior['runtime']==build['runtime']and prior['adapted']['sha256']==sha(driver)and prior['source']==pin(source)
  v['reused_from_outer_receipt_lookup_failure']=previous['manifest'];log.write_text('Native run already passed; adopt only the exact-SHA, same-driver, hash-verified receipt after correcting the outer receipt name.\n','utf-8')
 else:
  with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
 index=D/(receipt+'-current.json');i=read(index);assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m.get('observations',m);cases=0 if kind=='review'else len(m['results'])if kind in ['main68','main73']else o['cases'];assert cases==expected
 if kind in ['main68','main73']:assert all(r['runtime']==build['runtime']for r in m['results'])
 frozen=run/'selected-receipt.json';shutil.copyfile(index,frozen);v.update(status='passed-exact-SHA-retained-'+kind,cases=cases,known_checks=o.get('checks',o.get('known_checks')),receipt=pin(frozen),selected_manifest=i['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2000:]);raise
finally:v['log']=pin(log);write(run/'adapter-manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'adapter-manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
