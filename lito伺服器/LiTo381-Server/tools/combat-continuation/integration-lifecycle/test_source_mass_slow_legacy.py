"""Version Source76 actor-failure expectations with complete inverse evidence; retain skill66 assertions."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1]
file,java,expected={
 'scene':('test_companion_area_scene.py','CompanionAreaSceneProbe.java',16),
 'lifecycle':('test_companion_area_lifecycle.py','CompanionAreaLifecycleProbe.java',64),
 'publication':('test_companion_area_publication.py','CompanionAreaPublicationProbe.java',32),
 'secondary':('test_companion_area_secondary.py','CompanionAreaSecondaryProbe.java',16),
 'secondary-retirement':('test_companion_area_secondary_retirement.py','CompanionAreaSecondaryRetirementProbe.java',16),
}[kind]
versions=R/'source76-legacy-fixtures';basis=read(versions/'manifest.json')
row=next(x for x in basis['rows'] if Path(x['original']['path']).name==java)
for key in ['original','versioned']:assert sha(Path(row[key]['path']))==row[key]['sha256']
inverse=Path(row['versioned']['path']).read_text('utf-8')
for old,new in reversed(row['changes']):
 assert inverse.count(new)==1
 inverse=inverse.replace(new,old)
assert inverse==Path(row['original']['path']).read_text('utf-8')
source=R/file;build=read(D/'source-mass-slow-target-build-current.json')
assert sha(Path(build['runtime']['path']))==build['runtime']['sha256']
run=D/('source76-legacy-'+kind)/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),versions/'manifest.json',source,Path(row['versioned']['path'])]:shutil.copyfile(p,inputs/p.name)
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if isinstance(node.value,str):
   if node.value in ['companion-area-scene-build-current.json','companion-resurrection-scene-build-current.json']:node.value='source-mass-slow-target-build-current.json'
   elif node.value==java:node.value='source76-legacy-fixtures/'+java
   elif node.value.startswith('companion-area-'):node.value=node.value.replace('companion-area-','source76-legacy-area-',1)
  return node
driver=inputs/('selected-'+file);driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8')
v=dict(status='running',runtime=build['runtime'],original_driver=pin(source),selected_driver=pin(driver),version_basis=pin(versions/'manifest.json'),versioned_fixture=row,whole_file_inverse_passed=True,Source76_failure_and_STOPPED_expectations_only=True,all_skill66_assertions_retained=True,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);log=run/'test.log'
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver)],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
 receipt=read(D/('source76-legacy-area-'+kind+'-after-current.json'))
 assert receipt['runtime']==build['runtime']and receipt['status'].startswith('passed')and sha(Path(receipt['manifest']['path']))==receipt['manifest']['sha256']
 m=read(Path(receipt['manifest']['path']));o=m['observations'];assert o['cases']==expected
 v.update(status='passed-Source76-versioned-legacy-'+kind,observations=o,selected_manifest=receipt['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:
 v['log']=pin(log);write(run/'manifest.json',v);write(D/('source76-legacy-'+kind+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
