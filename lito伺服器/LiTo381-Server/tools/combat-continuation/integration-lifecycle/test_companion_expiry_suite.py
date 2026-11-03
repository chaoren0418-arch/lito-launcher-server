"""Replay the prior qualified actor scope on the off-scene expiry runtime, without moving any integrated pointer."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys,ast
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def original_paths(text,source):
 class Paths(ast.NodeTransformer):
  def visit_Call(self,node):
   self.generic_visit(node)
   if isinstance(node.func,ast.Name) and node.func.id=='Path' and len(node.args)==1 and isinstance(node.args[0],ast.Name) and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
   return node
 return ast.unparse(ast.fix_missing_locations(Paths().visit(ast.parse(text))))+'\n'

build=read(D/'companion-expiry-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'companion-expiry-suite'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name)
specs=[('test_companion_actor_targets.py','on-companion-expiry-actor-target-scene',24),('test_companion_queued_targets.py','on-companion-expiry-queued-target-lifecycle',36),('test_companion_death_publication.py','on-companion-expiry-death-publication',24),('test_companion_target_regressions.py','companion-expiry-regressions',132),('test_companion_target_compatibility.py','companion-expiry-compatibility',416)]
v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],results=[],driver_adaptation='Only runtime/index, receipt names and original driver path resolution; all fixture assertions unchanged.',full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for name,receipt,cases in specs:
  source=R/name;original=source.read_text('utf-8');text=original_paths(original,source).replace('companion-target-build-current.json','companion-expiry-build-current.json').replace('on-companion-targets-','on-companion-expiry-').replace('companion-target-regressions','companion-expiry-regressions').replace('companion-target-compatibility','companion-expiry-compatibility').replace("name='companion-actor-target-scene'","name='on-companion-expiry-actor-target-scene'").replace("name='companion-queued-target-lifecycle'","name='on-companion-expiry-queued-target-lifecycle'").replace("name='companion-death-publication'","name='on-companion-expiry-death-publication'");assert text!=original;driver=inputs/name;ast.parse(text);driver.write_text(text,'utf-8');log=run/(name+'.log');print('RUN '+name,flush=True)
  index=D/(receipt+'-current.json');reuse=index.exists() and read(index)['status'].startswith('passed') and read(index)['runtime']==build['runtime']
  with log.open('w',encoding='utf-8') as out:
   if reuse:out.write('Reused immutable hash-verified receipt for this exact runtime.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',str(driver)],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1200)
  current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'] and sha(Path(current['manifest']['path']))==current['manifest']['sha256'];m=read(Path(current['manifest']['path']));observations=m['observations'] if 'observations' in m else dict(cases=m['cases'],checks=m['known_checks']);assert observations['cases']==cases;frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(original=pin(source),adapted=pin(driver),receipt=pin(frozen),manifest=current['manifest'],observations=observations,log=pin(log)));write(run/'manifest.json',v);print('PASS '+name+' cases='+str(cases),flush=True)
 index=D/'companion-expiry-after-current.json';current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'] and sha(Path(current['manifest']['path']))==current['manifest']['sha256'];m=read(Path(current['manifest']['path']));assert m['observations']['cases']==24;frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(receipt=pin(frozen),manifest=current['manifest'],observations=m['observations']));v.update(status='passed-same-SHA-offscene-expiry-actor-Ctrl-queued-STOPPED-Main-and-BATTLE-scope',cases=sum(row['observations']['cases'] for row in v['results']),known_checks=sum(row['observations']['checks'] for row in v['results']),check_count_complete=False);assert v['cases']==656
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-5000:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-expiry-suite-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'],known_checks=v['known_checks'])))
