"""Independent pure planner comparison with unchanged Source method bodies and minimal DTO stubs."""
from pathlib import Path
import hashlib,json,re,subprocess,os,shutil
R=Path(__file__).resolve().parent;D=R/'build/plan';D.mkdir(parents=True,exist_ok=True)
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/utils/DamageDisplayUtil.java');config=Path('D:/lineage開發/1-伺服器/src/com/lineage/config/ConfigOther.java');settings=Path('D:/lineage開發/1-伺服器/config/other.properties')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
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
ref=D/'SourceDisplayReference.java';ref.write_text(reference,encoding='utf-8');classes=D/'classes';classes.mkdir(exist_ok=True)
inputs=[source,config,settings,R/'NativeCombatDisplayPlan.java',R/'DisplayPlanProbe.java',Path(__file__),ref]
frozen=D/'frozen';frozen.mkdir(exist_ok=True)
for p in inputs:shutil.copyfile(p,frozen/(sha(p)+'-'+p.name))
j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx128m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8');v=dict(status='running',inputs=[pin(p)for p in inputs],scope='pure Source method-body comparison; DTO and packet stubs only; no native consumer, delay, queue or GUI qualification',formal_deployment_changed=False)
try:
 with (D/'test.log').open('w',encoding='utf-8')as log:
  for args in [[j/'javac.exe','-g:none','-encoding','UTF-8','-d',classes,ref,R/'NativeCombatDisplayPlan.java',R/'DisplayPlanProbe.java'],[j/'java.exe','-Xverify:all','-cp',classes,'local.lito381.server.DisplayPlanProbe',settings,D/'observations.json']]:subprocess.run(list(map(str,args)),env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=45)
 v.update(status='passed-pure-planning-only',observations=pin(D/'observations.json'))
except BaseException as e:v.update(status='failed',error=str(e));print((D/'test.log').read_text('utf-8',errors='replace'));raise
finally:(D/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
print((D/'observations.json').read_text('utf-8'))
