"""Select the final PC/NPC candidate and unique receipt names; preserve original native fixture assertions."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
family=sys.argv[1];kind=sys.argv[2]if len(sys.argv)>2 else''
spec={
 'native':('test_source_negative_target_native.py',[kind],'latest-negative-native-'+kind+'-after',{'state':216,'boundary':1044,'io':144,'faults':72}),
 'source167':('test_source_negative_wind_regression.py',[kind],'latest-negative-source167-'+kind+'-after',{'state':72,'player-boundary':96,'player-io':36,'boundary':696,'io':144,'faults':48}),
 'source76':('test_source_negative_prior_native.py',[kind],'latest-negative76-'+kind,{'own':64,'io':144}),
 'intent':('test_source_negative_intent_regression.py',[],'latest-negative-intent',{'':76}),
 'negative':('test_source_negative_selected_pc.py',[],'latest-negative-selected-PC',{'':36}),
 'common':('test_source_negative_common_gates.py',[kind],'latest-negative-common-adapter-'+kind,{'review':0,'pair':288,'main68':10,'main73':8,'physical-area':26,'ref-life':10,'ref-feedback':10}),
}
name,args,receipt,counts=spec[family];expected=counts[kind];source=R/name;build=read(D/'source-negative-player-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/('latest-negative-adapter-'+family+'-'+kind)/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+name))
selectors={'source-negative-target-build-current.json','source-negative-self-build-current.json'}
replacements=[('source-negative-target-','latest-negative-native-'),('source-negative-retained-source167-','latest-negative-source167-'),('source-negative-retained-native-','latest-negative76-'),('source-negative-retained-intent-native','latest-negative-intent'),('source-negative-selected-PC-after','latest-negative-selected-PC'),('negative-common-','latest-negative-common-')]
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if node.value in selectors if isinstance(node.value,str)else False:return ast.copy_location(ast.Constant('source-negative-player-build-current.json'),node)
  if isinstance(node.value,str):
   for a,b in replacements:node.value=node.value.replace(a,b)
  return node
driver=inputs/name;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),fixture_behavior_changed_by_this_adapter=False,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
 i=read(D/(receipt+'-current.json'));assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m.get('observations',m);assert o['cases']==expected;shutil.copyfile(D/(receipt+'-current.json'),run/'selected-receipt.json');v.update(status='passed-exact-SHA-'+family+'-'+kind,observations=o,selected_manifest=i['manifest'],receipt=pin(run/'selected-receipt.json'))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-7000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('latest-negative-adapter-'+family+'-'+kind+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
