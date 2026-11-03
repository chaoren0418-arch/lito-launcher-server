"""Select exact new SHA and isolated receipts in sealed prior drivers; retain all Java bodies/goldens."""
from pathlib import Path
from datetime import datetime,timezone
import ast,json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1]
if kind.startswith('prior:'):
 file='test_shock_player_prior.py';args=[kind[6:]];receipt='shock-accepted-prior-'+args[0]+'-adapter';expected=None
else:
 specs={'admission':('admission','admission-after',8),'boundaries':('boundaries','boundaries',200),'controls':('controls','controls',64),'death-receipt':('death_receipt','death-receipt',8),'close':('close','close',4),'enqueue-fault':('hp_enqueue_fault','hp-enqueue-fault',4),'resurrection':('resurrection_prior','resurrection-originals',368),'hp':('hp_prior','hp-originals',26),'main':('hp_main','hp-main',2)}
 suffix,out,expected=specs[kind];file='test_shock_player_'+suffix+'.py';args=[];receipt='shock-accepted-'+out
source=R/file;build=read(D/'shock-accepted-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];name='shock-accepted-retained-'+kind.replace(':','-');run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
shutil.copyfile(__file__,inputs/Path(__file__).name);shutil.copyfile(source,inputs/('original-'+file))
class Select(ast.NodeTransformer):
 def visit_Call(self,node):
  self.generic_visit(node)
  if isinstance(node.func,ast.Name)and node.func.id=='Path'and len(node.args)==1 and isinstance(node.args[0],ast.Name)and node.args[0].id=='__file__':node.args[0]=ast.Constant(str(source))
  return node
 def visit_Constant(self,node):
  if isinstance(node.value,str)and node.value!='shock-player-resurrection-pair-receipt-adaptation.json':node.value=node.value.replace('shock-player-','shock-accepted-').replace('pc87-prior-','accepted87-prior-')
  return node
driver=inputs/file;driver.write_text(ast.unparse(ast.fix_missing_locations(Select().visit(ast.parse(source.read_text('utf-8')))))+'\n','utf-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],source=pin(source),adapted=pin(driver),fixture_behavior_or_goldens_changed=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(driver),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=2400)
 i=read(D/(receipt+'-current.json'));assert i['status'].startswith('passed')and i['runtime']==build['runtime']and sha(Path(i['manifest']['path']))==i['manifest']['sha256'];m=read(Path(i['manifest']['path']));o=m.get('observations',m)
 if expected is not None:assert o.get('scenarios',o['cases'])==expected
 frozen=run/'selected-receipt.json';shutil.copyfile(D/(receipt+'-current.json'),frozen);v.update(status='passed-exact-SHA-retained-'+kind,cases=o['cases'],known_checks=o.get('checks',o.get('known_checks')),receipt=pin(frozen),selected_manifest=i['manifest'])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'adapter-manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'adapter-manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
