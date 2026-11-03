from pathlib import Path
import hashlib,json,os,subprocess,zipfile,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
build=read(D/'build.json');jar=Path(build['runtime']['path']);old=Path(build['base']['path']);assert sha(jar)==build['runtime']['sha256']
run=D/'boundaries';run.mkdir(exist_ok=True);classes=run/'classes';classes.mkdir(exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
log=run/'test.log';result={'status':'running','runtime':build['runtime'],'formal_deployment_changed':False};rows=[]
for name in ['test.log','result.json']:
 p=run/name
 if p.exists():shutil.copyfile(p,run/('previous-'+sha(p)[:12]+'-'+name))
frozen=run/'frozen';frozen.mkdir(exist_ok=True)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  hook=run/'DeItemTransactionProbe.java';hook.write_text('package local.lito381.server; public final class DeItemTransactionProbe {public static void beforeSave(){PolymorphFaultProbe.beforeSave();}public static void beforeFlush()throws Exception{PolymorphFaultProbe.beforeFlush();}}',encoding='utf-8')
  sources=[*R.glob('Polymorph*Probe.java'),hook,B/'NativeCastMaterialProbe.java',B/'NativeLightCastProbe.java',B/'NativeLightStoppedProbe.java',B/'SourceActionPairProbe.java',B/'de-native-skill-consumption-development/reference/SourceSkillConsumptionReference.java',B/'de-native-skill-consumption-development/source-inputs/L1SkillId.java']
  for p in [*sources,Path(__file__),B/'ObserveDeItemTransaction.java']:
   q=frozen/(sha(p)+'-'+p.name)
   if not q.exists():shutil.copyfile(p,q)
  result['inputs']=[{'path':str(p),'sha256':sha(p)} for p in frozen.iterdir()]
  invoke(J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sources)
  if '--compile-only' in sys.argv:
   result['status']='compiled-only-not-qualified';print(result['status']);sys.exit(0)
  asm=Path.home()/'.m2/repository/org/ow2/asm';tool=run/'tools';tool.mkdir(exist_ok=True);tcp=';'.join(map(str,[tool,asm/'asm/9.7/asm-9.7.jar',asm/'asm-tree/9.7/asm-tree-9.7.jar']))
  invoke(J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,B/'ObserveDeItemTransaction.java')
  overlay=run/'memory-observer';invoke(J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',old,overlay)
  with zipfile.ZipFile(old) as a,zipfile.ZipFile(jar) as z:assert a.read('local/lito381/server/GameStateStore.class')==z.read('local/lito381/server/GameStateStore.class')
  for test,mode,runtime,cases in [('Fault','before',old,8),('Fault','after',jar,8),('Lifecycle','after',jar,56)]:
   branch=run/(test+'-'+mode);branch.mkdir(exist_ok=True);observation=branch/'observations.json';runtimecp=cp.replace(str(jar),str(runtime))
   if test=='Fault':runtimecp=str(classes)+';'+str(overlay)+';'+runtimecp.removeprefix(str(classes)+';')
   invoke(J/'java.exe','-Xverify:all','-cp',runtimecp,'local.lito381.server.Polymorph'+test+'Probe',S/'config/server.properties',branch/'runtime',observation,runtime,mode)
   row=read(observation);assert row['cases']==cases and row['locked_write_calls']==0
   if test=='Fault':assert row['actual_memory_faults']==8 and row['failed_material_casts_with_partial_commit']==(8 if mode=='before' else 0)
   rows.append(dict(test=test,mode=mode,results=row));print(test,mode,json.dumps(row),flush=True)
  result.update(status='passed',results=rows)
except Exception as error:
 result.update(status='failed',error=str(error),results=rows);print(log.read_text('utf-8',errors='replace')[-6000:]);raise
finally:(run/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
