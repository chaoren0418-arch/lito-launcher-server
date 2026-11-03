"""Run verbatim current Source accepted-plan/erase/freeze-effect fragments with memory-only dependency observers."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';run=D/'source-freeze-accepted-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');src=run/'src';src.mkdir(parents=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def java(name,text):
 p=src/(name.replace('.','/')+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,'utf-8')
def block(s,start):
 a=s.index(start);end=s.index('{',a)+1;depth=1
 while depth:
  if s[end]=='{':depth+=1
  elif s[end]=='}':depth-=1
  end+=1
 return s[a:end]
original=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java');text=original.read_text('utf-8');commit=block(text,'private record SkillTargetCommitPlan(');mutation=block(text,'private record SkillTargetMutationPlan(');freeze=block(text,'private void applyFreezeTargetEffect(');shutil.copyfile(original,run/original.name);shutil.copyfile(__file__,run/Path(__file__).name)
M='com.lineage.server.model.';I=M+'Instance.';K=M+'skill.';P='com.lineage.server.serverpackets.'
java(K+'L1SkillId','package '+K[:-1]+';public final class L1SkillId {public static final int ERASE_MAGIC=153;}')
java(K+'L1SkillTimer','package '+K[:-1]+';public final class L1SkillTimer {}')
java(K+'DamageCommitOutcome','package '+K[:-1]+';public record DamageCommitOutcome(boolean accepted,boolean terminal){}')
java(K+'DamageCommitPlan','package '+K[:-1]+';public interface DamageCommitPlan {void afterAccepted(DamageCommitOutcome outcome);}')
java(M+'L1Character','''package com.lineage.server.model;
import java.util.*;import com.lineage.server.model.skill.*;
public class L1Character {public final List<String> events=new ArrayList<>();public int heals,kills;public L1SkillTimer current;
 public int getX(){return 12;}public int getY(){return 34;}public short getMapId(){return 56;}public int getId(){return 78;}
 public void adjustCurrentHpIfAlive(int hp,int mp){heals+=hp;}public void killSkillEffectTimer(int id,L1SkillTimer expected){if(id!=153)throw new AssertionError();if(current==expected){current=null;kills++;events.add("erase");}}
}''')
java(P+'S_Poison','package '+P[:-1]+';public record S_Poison(int id,int color){}')
java(P+'S_Paralysis','package '+P[:-1]+';public record S_Paralysis(int type,boolean active){public static final int TYPE_FREEZE=2;}')
java(I+'L1PcInstance','''package com.lineage.server.model.Instance;
import com.lineage.server.serverpackets.*;public class L1PcInstance extends com.lineage.server.model.L1Character {public int palettes,locks;public void sendPacketsAll(S_Poison p){if(p.id()!=78||p.color()!=2)throw new AssertionError();palettes++;events.add("palette");}public void sendPackets(S_Paralysis p){if(p.type()!=S_Paralysis.TYPE_FREEZE||!p.active())throw new AssertionError();locks++;events.add("paralysis");}}''')
java(I+'L1NpcInstance','''package com.lineage.server.model.Instance;
import com.lineage.server.serverpackets.*;public class L1NpcInstance extends com.lineage.server.model.L1Character {public int palettes,locks;public void broadcastPacketAll(S_Poison p){if(p.id()!=78||p.color()!=2)throw new AssertionError();palettes++;events.add("palette");}public void setParalyzed(boolean b){if(!b)throw new AssertionError();locks++;events.add("paralysis");}}''')
for name in ['Monster','Summon','Pet','Guard','Guardian']:java(I+'L1'+name+'Instance','package '+I[:-1]+';public class L1'+name+'Instance extends L1NpcInstance{}')
java('com.lineage.server.utils.L1SpawnUtil','''package com.lineage.server.utils;
import com.lineage.server.model.*;public final class L1SpawnUtil {public static int count,seconds;public static L1Character user;public static void spawnEffect(int id,int duration,int x,int y,int map,L1Character owner,int z){if(id!=81168||x!=12||y!=34||map!=56||owner!=user||z!=0)throw new AssertionError("original freeze factory parameters");count++;seconds=duration;user.events.add("factory");}}''')
java(K+'skillmode.SHOCK_STUN','package '+K+'skillmode;public final class SHOCK_STUN {public static void applyStunToTarget(com.lineage.server.model.L1Character c){throw new AssertionError("unselected shock");}}')
java(M+'talent.TalentDamageCalculator','package '+M+'talent;public final class TalentDamageCalculator {public static void tryTalentStun(com.lineage.server.model.Instance.L1PcInstance a,com.lineage.server.model.L1Character b,int id){throw new AssertionError("unselected talent");}}')
wrapper='''package com.lineage.server.model.skill;
import static com.lineage.server.model.skill.L1SkillId.ERASE_MAGIC;
import com.lineage.server.model.*;import com.lineage.server.model.Instance.*;import com.lineage.server.serverpackets.*;import com.lineage.server.utils.L1SpawnUtil;import java.util.*;
public final class L1SkillUse {
 static final java.util.concurrent.atomic.AtomicInteger ERROR_COUNT=new java.util.concurrent.atomic.AtomicInteger();static final Log _log=new Log();static class Log {void error(String m,Object...x){throw new AssertionError(m);}}
 record Skill(int duration){int getBuffDuration(){return duration;}}Skill _skill;L1Character _user;int sides,gfx;void applyThunderGrabTargetEffect(L1Character t,int ms){throw new AssertionError("unselected thunder");}void applyTargetSideEffects(L1Character t,int n){sides++;t.events.add("side");}void sendGrfx(boolean b){gfx++;}
'''+commit+'\n'+mutation+'\n'+freeze+'''
 static int cases,checks;static void need(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static L1Character target(int kind){return switch(kind){case 0->new L1PcInstance();case 1->new L1MonsterInstance();case 2->new L1SummonInstance();case 3->new L1PetInstance();case 4->new L1GuardInstance();case 5->new L1GuardianInstance();case 6->new L1NpcInstance();default->new L1Character();};}
 public static void main(String[]args)throws Exception {
  DamageCommitOutcome[] outcomes={null,new DamageCommitOutcome(false,false),new DamageCommitOutcome(false,true),new DamageCommitOutcome(true,true),new DamageCommitOutcome(true,false)};
  for(int kind=0;kind<8;kind++)for(int duration:new int[]{0,1,7,65535})for(boolean recast:new boolean[]{false,true})for(var outcome:outcomes){
   var cast=new L1SkillUse();cast._user=new L1PcInstance();cast._skill=new Skill(duration);var t=target(kind);var captured=new L1SkillTimer();var latest=recast?new L1SkillTimer():captured;t.current=latest;L1SpawnUtil.count=0;L1SpawnUtil.user=cast._user;var plan=new SkillTargetCommitPlan(cast,t,3,null,50,new SkillTargetMutationPlan(captured,true,0,false),true);
   need(t.kills==0&&L1SpawnUtil.count==0&&t.events.isEmpty(),"planning mutates neither erase timer nor freeze effect");plan.afterAccepted(outcome);boolean accepted=outcome!=null&&outcome.accepted(),apply=accepted&&!outcome.terminal();need(t.kills==(apply&&!recast?1:0)&&t.current==(apply&&!recast?null:latest),"captured Source erase timer identity preserves newer recast");need(L1SpawnUtil.count==(apply?1:0),"freeze factory only after nonterminal acceptance");need(cast._user.heals==(accepted?3:0)&&cast.sides==(apply?1:0)&&cast.gfx==(accepted?1:0),"original accepted heal/side/impact gates");
   if(apply){need(L1SpawnUtil.seconds==duration,"Source passes raw duration including zero to effect factory");int palettes=t instanceof L1PcInstance p?p.palettes:t instanceof L1NpcInstance n?n.palettes:0;int locks=t instanceof L1PcInstance p?p.locks:t instanceof L1NpcInstance n?n.locks:0;need(palettes==(kind<=3?1:0)&&locks==(kind<=3?1:0),"Source exact supported freeze palette/paralysis types");need(cast._user.events.equals(List.of("factory")),"Source factory owner is caster");var expected=new ArrayList<String>();if(!recast)expected.add("erase");if(kind<=3){expected.add("palette");expected.add("paralysis");}expected.add("side");need(t.events.equals(expected),"Source erase/palette/paralysis/side order");}need(ERROR_COUNT.get()==0,"no swallowed fragment exceptions");cases++;
  }
  need(cases==320,"Source freeze fragment matrix");java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]),"{\\"cases\\":"+cases+",\\"checks\\":"+checks+",\\"Source_fragment_only\\":true,\\"full_goal_achieved\\":false}\\n");
 }
}
'''
java(K+'L1SkillUse',wrapper);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',Source=dict(original=pin(original),frozen=pin(run/original.name)),extracted_fragments_verbatim=True,stubbed_dependencies_are_memory_observers=True,created_Hub=False,created_SQL_connection=False,native_LiTo_cases=0,full_goal_achieved=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',run/'classes',*sorted(src.rglob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',run/'classes',K+'L1SkillUse',run/'observations.json'])
 observations=json.loads((run/'observations.json').read_text('utf-8'));assert observations['cases']==320;v.update(status='passed-verbatim-Source-accepted-erase-and-freeze-effect-fragment-oracle',observations=observations,generated_sources=[pin(p)for p in sorted(src.rglob('*.java'))]);assert sha(original)==v['Source']['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2200:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-freeze-accepted-oracle-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=observations)))
