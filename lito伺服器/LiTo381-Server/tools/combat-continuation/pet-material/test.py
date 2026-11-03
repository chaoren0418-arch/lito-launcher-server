from pathlib import Path
import json,hashlib,subprocess,os,shutil,zipfile,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build/tests';D.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for name in ['test.log','result.json']:
 p=D/name
 if p.exists():shutil.copyfile(p,D/('previous-'+sha(p)[:12]+'-'+name))
hook=D/'DeItemTransactionProbe.java';hook.write_text('package local.lito381.server; public final class DeItemTransactionProbe {public static void beforeSave(){PetMaterialProbe.beforeSave();}public static void beforeFlush()throws Exception{PetMaterialProbe.beforeFlush();}}',encoding='utf-8')
sources=[R/'PetMaterialProbe.java',hook]
for p in [*sources,Path(__file__),B/'ObserveDeItemTransaction.java']:
 q=frozen/(sha(p)+'-'+p.name)
 if not q.exists():shutil.copyfile(p,q)
F=B/'baseline-PC-ingress-efd4-v2';overlay=D/'memory-observer';deps=[classes,overlay,R.parent/'elemental-material/build/boundaries/classes',R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],base=b['base'],formal_deployment_changed=False,full_goal_achieved=False,inputs=[pin(p)for p in frozen.iterdir()])
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  def run(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=300)
  asm=Path.home()/'.m2/repository/org/ow2/asm';tools=D/'tools';tools.mkdir(exist_ok=True);tcp=';'.join(map(str,[tools,asm/'asm/9.7/asm-9.7.jar',asm/'asm-tree/9.7/asm-tree-9.7.jar']))
  run(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tools,B/'ObserveDeItemTransaction.java')
  run(J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',b['base']['path'],overlay)
  with zipfile.ZipFile(b['base']['path'])as a,zipfile.ZipFile(b['runtime']['path'])as z:assert a.read('local/lito381/server/GameStateStore.class')==z.read('local/lito381/server/GameStateStore.class')
  for mode,p in [('before',b['base']),('after',b['runtime'])]:
   jar=Path(p['path']);assert sha(jar)==p['sha256'];cp=';'.join(map(str,[*deps,jar,S/'lib/*']));dest=D/mode;dest.mkdir(exist_ok=True)
   run(J/'javac.exe','-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',classes,*sources)
   if '--compile-only'in sys.argv:continue
   run(J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.PetMaterialProbe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,mode)
   result=read(dest/'observations.json');assert result['cases']==72 and result['actual_memory_faults']==8 and result['Source_window_bypasses']==(8 if mode=='before'else 0) and result['failed_material_casts_with_partial_commit']==(8 if mode=='before'else 0) and result['locked_writes']==0
   v[mode]=result;print(mode,json.dumps(result),flush=True)
  if '--compile-only'in sys.argv:v['status']='compiled-only-not-qualified'
  else:
   assert (D/'before/goldens.txt').read_bytes()==(D/'after/goldens.txt').read_bytes()
   v.update(status='passed-caster-material-world-only',stable_wire_equal=True,outputs=[pin(p)for p in D.glob('*/observations.json')])
except BaseException as error:
 v.update(status='failed',error=str(error));print((D/'test.log').read_text('utf-8',errors='replace')[-6500:]);raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print(v['status'])
