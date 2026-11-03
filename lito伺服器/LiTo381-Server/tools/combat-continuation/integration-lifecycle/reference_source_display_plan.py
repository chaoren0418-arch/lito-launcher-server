from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'source-kiringku-presentation-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];run=D/'source-display-plan-reference'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
for p in [Path(__file__),R/'SourceDisplayPlanProbe.java']:shutil.copyfile(p,run/p.name)
build=read(Path(read(D/'source-other-control-build-current.json')['manifest']['path']));source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/utils/DamageDisplayUtil.java');config=Path(next(x['path']for x in build['inputs']if Path(x['path']).name=='ConfigOther.java'));settings=Path('D:/lineage開發/1-伺服器/config/other.properties')
text=source.read_text('utf-8');names=['buildPhysicalDamagePackets','buildMagicDamagePackets','isDisplayAllowed','buildDamageNumberPackets','splitDigits','getDigitCount','getPhysicalSprIds','getMagicSprIds'];methods=[]
for name in names:
 match=re.search(r'    (?:public|private) static [^\n]+\b'+name+r'\(',text);assert match,name
 start=match.start();opening=text.index('{',start);depth=1;end=opening+1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 methods.append(text[start:end])
c=config.read_text('utf-8');fields=[];assignments=[]
for line in c.splitlines():
 if re.search(r'public static (?:boolean|int) (?:HPBAR|DMG_|KIRINGKU_)',line):fields.append(line.split('//')[0].strip())
 if re.match(r'\s*(HPBAR|DMG_\w+|KIRINGKU_\w+)\s*=',line):assignments.append(line.strip())
assert len(fields)==len(assignments)==16
reference='''package local.lito381.server;import java.util.*;
final class SourceDisplayReference {
 static class L1Character {final int id;L1Character(int id){this.id=id;}int getId(){return id;}}
 static class L1PcInstance extends L1Character {final boolean show,dead;L1PcInstance(int id,boolean show,boolean dead){super(id);this.show=show;this.dead=dead;}boolean isShowDamage(){return show;}boolean isDead(){return dead;}}
 static class ServerBasePacket {final long object;final int sprite;ServerBasePacket(long object,int sprite){this.object=object;this.sprite=sprite;}}
 static class S_SkillSound extends ServerBasePacket {S_SkillSound(long object,int sprite){super(object,sprite);}}
 static final class ConfigOther {FIELDS}
 static void configure(Properties set){ASSIGNMENTS}
METHODS
}
'''.replace('FIELDS','\n'.join(fields)).replace('ASSIGNMENTS','\n'.join('ConfigOther.'+line for line in assignments)).replace('METHODS','\n'.join(methods))
ref=run/'SourceDisplayReference.java';ref.write_text(reference,'utf-8');shutil.copyfile(settings,run/'Source-other.properties');
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'verify.log';v=dict(status='running',runtime=b['runtime'],created_Hub=False,created_SQL_connection=False,initialized_game_classes=False,native_runtime_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  cp=';'.join(map(str,[run/'classes',b['runtime']['path'],S/'lib/*']));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',run/'SourceDisplayPlanProbe.java',ref]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceDisplayPlanProbe',run/'Source-other.properties',run/'observations.json']);v['observations']=read(run/'observations.json')
 v.update(status='passed4800-pure-Source-display-plans-no-consumer-qualification',inputs=[pin(p)for p in [run/'SourceDisplayPlanProbe.java',ref,run/'Source-other.properties',run/Path(__file__).name]])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-display-plan-reference-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],verified_classes=v.get('verified_classes'))))
