from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'companion-landing-build-current.json');assert sha(Path(build['manifest']['path']))==build['manifest']['sha256'];bm=read(Path(build['manifest']['path']));jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/'companion-landing-Source-search'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'CompanionLandingProbe.java']:shutil.copyfile(p,inputs/p.name)
row=next(row for row in bm['Source'] if row['original']['path'].endswith('CheckUtil.java'))
for ref in row.values():assert sha(Path(ref['path']))==ref['sha256']
frozen=inputs/'CheckUtil.java';shutil.copyfile(row['frozen']['path'],frozen);source=frozen.read_text('utf-8');start=source.index('private static int[] findNearestLivingClearLanding(');first=source.index('{',start);depth=1;last=first+1
while depth:
 if source[last]=='{':depth+=1
 elif source[last]=='}':depth-=1
 last+=1
body=source[first:last];assert body.count('acceptLanding(')==2 and body.count('_log.error(e.getLocalizedMessage(), e);')==1
body,ports=re.subn(r'acceptLanding\(map, (centerX|nx), (centerY|ny), mapId, showId, chaForTopo, selfToSkip, relocate\)',r'map.acceptAndRelocate(\1, \2)',body);assert ports==2
body=body.replace('_log.error(e.getLocalizedMessage(), e);','failures++;')
projection=inputs/'SourceCompanionLandingReference.java';projection.write_text('package local.lito381.server; final class SourceCompanionLandingReference { static int failures; interface Terrain {boolean isNull();default short getId(){return 0;}boolean acceptAndRelocate(int x,int y);}static int[] find(Terrain map,int centerX,int centerY,int maxRadius)'+body+'}\n','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';cp=';'.join(map(str,[classes,jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],Source_reference=row,projection=dict(input=pin(frozen),output=pin(projection),transforms=['Extract complete actual private findNearestLivingClearLanding body without rewriting center/ring traversal, radius, returns or exception boundary','Only two acceptLanding calls project to the supplied atomic terrain port; log side effect projects to a fixture counter; map id getter has no behavior in this fixture']),inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,production_dispatch_wired=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,inputs/'CompanionLandingProbe.java',projection]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.CompanionLandingProbe',jar,run/'observations.json'])
 result=read(run/'observations.json');assert result['cases']==16518 and result['Source_commit_failures']==8;v.update(status='passed-unwired-Source-landing-complete-search-trace-and-atomic-commit-port',observations=result)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'companion-landing-search-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
