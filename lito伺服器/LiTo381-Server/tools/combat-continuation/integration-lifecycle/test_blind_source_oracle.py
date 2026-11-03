"""Execute unchanged CURSE_BLIND in an isolated Source-only link-stub classpath; no Hub, DB or Source server."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'blind-scene-build-current.json');run=D/'blind-source-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);src=run/'Source-only';refs=[]
for relative in ['model/skill/skillmode/CURSE_BLIND.java','model/skill/L1SkillId.java']:
 source=T/relative;target=src/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target);refs.append(dict(original=pin(source),frozen=pin(target)))
contracts={n:(T/n).read_text('utf-8') for n in ['model/skill/L1SkillMode.java','model/skill/L1SkillUse.java','model/skill/skillmode/CANCELLATION.java']}
mode=contracts['model/skill/L1SkillMode.java']
for name in ['CURSE_BLIND','DARKNESS','DARK_BLIND']:assert f'_skillMode.put({name}, new CURSE_BLIND())'in mode
use=contracts['model/skill/L1SkillUse.java'];compact=re.sub(r'\s+','',use).replace('this.','');assert 'if(_skill.getRanged()!=-1)'in compact and 'getTileLineDistance(_target.getLocation())>_skill.getRanged()'in compact
assert 'if(_skill.getArea()==0)'in compact
assert 'if (mode == null) {\n\t\t\tcha.setSkillEffect(getEffectSkillId(), _getBuffDuration);'in use
for name in contracts:
 target=inputs/Path(name).name;shutil.copyfile(T/name,target);refs.append(dict(original=pin(T/name),frozen=pin(target)))
stubs={
 'model/L1Character.java':'''package com.lineage.server.model; public class L1Character {public int effectId=-1,millis=-1;public boolean floating;public void setSkillEffect(int id,int duration){effectId=id;millis=duration;}public boolean hasSkillEffect(int id){return id==com.lineage.server.model.skill.L1SkillId.STATUS_FLOATING_EYE&&floating;}}''',
 'model/L1Magic.java':'''package com.lineage.server.model; public class L1Magic {}''',
 'model/Instance/L1PcInstance.java':'''package com.lineage.server.model.Instance; public class L1PcInstance extends com.lineage.server.model.L1Character {public int packet=-1,count;public void sendPackets(com.lineage.server.serverpackets.S_CurseBlind value){packet=value.mode;count++;}}''',
 'model/Instance/L1NpcInstance.java':'''package com.lineage.server.model.Instance; public class L1NpcInstance extends com.lineage.server.model.L1Character {}''',
 'serverpackets/S_CurseBlind.java':'''package com.lineage.server.serverpackets; public class S_CurseBlind {public final int mode;public S_CurseBlind(int m){mode=m;}}''',
 'model/skill/skillmode/SkillMode.java':'''package com.lineage.server.model.skill.skillmode; public abstract class SkillMode {}''',
}
for name,text in stubs.items():
 p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text+'\n','utf-8')
main=src/'SourceBlindOracle.java';main.write_text('''import com.lineage.server.model.*;import com.lineage.server.model.Instance.*;import com.lineage.server.model.skill.skillmode.*;import java.nio.file.*;
public class SourceBlindOracle {public static void main(String[]args)throws Exception{int cases=0;for(int alias:new int[]{20,40,103})for(boolean sourceNpc:new boolean[]{false,true})for(int targetKind=0;targetKind<3;targetKind++)for(int seconds:new int[]{1,64,120}){var target=targetKind==2?new L1NpcInstance():new L1PcInstance();target.floating=targetKind==1;var mode=new CURSE_BLIND();int damage=sourceNpc?mode.start(new L1NpcInstance(),target,null,seconds):mode.start(new L1PcInstance(),target,null,seconds);if(damage!=0||target.effectId!=40||target.millis!=seconds*1000)throw new AssertionError("actual Source alias40 state");if(target instanceof L1PcInstance pc){if(pc.packet!=(targetKind==1?2:1)||pc.count!=1)throw new AssertionError("actual Source visual mode");mode.stop(pc);if(pc.packet!=0||pc.count!=2)throw new AssertionError("actual Source stop packet");}cases++;}if(cases!=54)throw new AssertionError();Files.writeString(Path.of(args[0]),"{\\"cases\\":"+cases+",\\"actual_unchanged_Source_mode\\":true,\\"created_native_Hub\\":false,\\"real_Source_actors_constructed\\":false,\\"full_goal_achieved\\":false}\\n");}}
''','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'oracle.log';v=dict(status='running',runtime=index['runtime'],Source=refs,link_only_stubs=[pin(src/n)for n in stubs],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  subprocess.run(list(map(str,[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',run/'classes',*src.rglob('*.java')])),env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',run/'classes','SourceBlindOracle',run/'observations.json'])),env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=30)
 v.update(status='passed-54-isolated-actual-Source-blind-mode-alias40-duration-and-visual-packets',observations=read(run/'observations.json'),driver=pin(inputs/Path(__file__).name),main=pin(main))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'blind-source-oracle-current.json',dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
