from pathlib import Path
import hashlib,json,os,shutil,subprocess
R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
build=read(D/'build.json');jar=Path(build['runtime']['path']);old=Path(build['base']['path']);assert sha(jar)==build['runtime']['sha256'] and sha(old)==build['base']['sha256']
run=D/'tests';run.mkdir(exist_ok=True);classes=run/'classes';classes.mkdir(exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
log=run/'test.log';result={'status':'running','runtime':build['runtime'],'formal_deployment_changed':False}
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(*args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  sources=[R/'NativeMagicalWeaponProbe.java',B/'NativeCastMaterialProbe.java',B/'NativeLightCastProbe.java',B/'SourceActionPairProbe.java',B/'de-native-skill-consumption-development/reference/SourceSkillConsumptionReference.java',B/'de-native-skill-consumption-development/source-inputs/L1SkillId.java']
  invoke(J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sources)
  rows=[]
  for mode,runtime in [('before',old),('after',jar)]:
   branch=run/mode;branch.mkdir(exist_ok=True);observation=branch/'observations.json'
   invoke(J/'java.exe','-Xverify:all','-cp',cp.replace(str(jar),str(runtime)),'local.lito381.server.NativeMagicalWeaponProbe',S/'config/server.properties',branch/'runtime',observation,runtime,mode)
   row=read(observation);assert row['cases']==48 and row['Source_window_bypasses']==(8 if mode=='before' else 0)
   # Parent is expected to expose its old locked missing-material send.
   if mode=='after':assert row['locked_write_calls']==0
   rows.append(dict(mode=mode,results=row));print(mode,json.dumps(row),flush=True)
  assert (run/'before/item-weapon-goldens.txt').read_bytes()==(run/'after/item-weapon-goldens.txt').read_bytes(),'normal original packets changed'
  result.update(status='passed',results=rows,normal_packets_byte_equal=True)
except BaseException as error:
 result.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6500:]);raise
finally:(run/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
