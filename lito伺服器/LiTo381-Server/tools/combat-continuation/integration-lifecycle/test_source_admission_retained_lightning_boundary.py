"""Versioned PC80 projection of the original raw50 worker-held stage fixture; no PC overlays."""
from pathlib import Path
import hashlib,json,subprocess,shutil,os,sys
from datetime import datetime,timezone
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent;D=R/'build';B=S/'tools/resource-database/build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
before='--before'in sys.argv;build=read(D/'source-kiringku-admission-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];name='source-kiringku-admission-retained-lightning_boundary';run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'SourceLightningBoundaryProbe.java',R/'SourceMaterialRangePairProbe.java',R/'SourceMaterialRangeProbe.java',R/'FreezeAcceptedStageProbe.java',R/'ShockPlayerAdmissionProbe.java',R/'ShockCompanionAdmissionProbe.java',R/'BlindSceneProbe.java',R/'CompanionPlacementProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceLightningBoundaryProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar,'before'if before else'after'])
 obs=read(run/'observations.json');assert obs['cases']==56 and obs['observation_only']==before;v.update(status='observed-or-verified-Source17-lifecycle',observations=obs)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
