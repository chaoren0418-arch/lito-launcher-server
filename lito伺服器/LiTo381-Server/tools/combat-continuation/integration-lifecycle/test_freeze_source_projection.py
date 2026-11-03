"""Reversible194 projections of original50 native fixtures; original assertions/goldens retained."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent;D=R/'build';B=S/'tools/resource-database/build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];spec={'stage':('FreezeAcceptedStageProbe',16),'boundary':('FreezeAcceptedBoundaryProbe',64),'completion':('FreezeAcceptedCompletionProbe',64),'io':('FreezeAcceptedIoProbe',32),'duration':('FreezeAcceptedDurationProbe',8),'relations':('FreezeAcceptedRelationsProbe',16)};original_name,expected=spec[kind];original=R/(original_name+'.java');new_name=original_name.replace('FreezeAccepted','FreezeSourceAccepted');text=original.read_text('utf-8');source=text;changes=[]
def replace(a,b):
 global text
 if a in text:changes.append((a,b,text.count(a)));text=text.replace(a,b)
replace(original_name,new_name)
for a,b in [('new int[]{50})','new int[]{194})'),('selectedSkill=50;','selectedSkill=194;'),('freezeAttackSpell(50)','freezeAttackSpell(194)'),('cost(pc,50,spell)','cost(pc,194,spell)'),('values.get(50)','values.get(194)'),('values.put(50,','values.put(194,')]:replace(a,b)
if kind in ['boundary','io','relations']:replace('prepare(pc,peer,hub);','prepare(pc,peer,hub);set(pc,"characters",List.of(copy(((List<CharacterSummary>)get(pc,"characters")).get(0),Map.of("type",5,"level",99,"originalIntelligence",10))));')
inverse=text
for old,new,count in reversed(changes):assert inverse.count(new)==count,(old,new);inverse=inverse.replace(new,old)
assert inverse==source
build=read(D/'freeze-source-target-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];name='freeze-source-projection-'+kind;run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);(inputs/(new_name+'.java')).write_text(text,'utf-8');(inputs/('original-'+original_name+'.txt')).write_text(source,'utf-8');(R/(new_name+'.java')).write_text(text,'utf-8')
for p in [Path(__file__),R/'ShockPlayerAdmissionProbe.java',R/'ShockCompanionAdmissionProbe.java',R/'BlindSceneProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java']+([]if kind=='stage'else[R/'FreezeAcceptedStageProbe.java']):shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],original_fixture=pin(original),projection=changes,whole_file_inverse_verified=True,original_assertions_and_goldens_unchanged=True,inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+new_name,S/'config/server.properties',run/'runtime',run/'observations.json',jar,'after'])
 o=read(run/'observations.json');assert o['cases']==expected;v.update(status='passed-Source194-projected-original-'+kind,observations=o)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
