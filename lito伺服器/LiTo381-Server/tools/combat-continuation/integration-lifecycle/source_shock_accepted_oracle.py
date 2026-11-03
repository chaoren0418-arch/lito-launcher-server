"""Compile verbatim current Source commit-plan records and SHOCK_STUN against explicit memory-only stubs.
This qualifies fragment behavior, not complete Source server integration or LiTo formula equivalence.
"""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src');run=D/'source-shock-accepted-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');src=run/'src';src.mkdir(parents=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def java(name,text):
 p=src/(name.replace('.','/')+'.java');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,'utf-8');return p
def block(text,start):
 at=text.index(start);brace=text.index('{',at);depth=1;end=brace+1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 return text[at:end]
original=T/'com/lineage/server/model/skill/L1SkillUse.java';shock=T/'com/lineage/server/model/skill/skillmode/SHOCK_STUN.java';text=original.read_text('utf-8');commit=block(text,'private record SkillTargetCommitPlan(');mutation=block(text,'private record SkillTargetMutationPlan(')
frozen=run/'Source';frozen.mkdir();shutil.copyfile(original,frozen/original.name);shutil.copyfile(shock,frozen/shock.name);shutil.copyfile(__file__,run/Path(__file__).name)
package='com.lineage.server.model.';skill=package+'skill.';inst=package+'Instance.'
java('com.lineage.config.ConfigSkill','package com.lineage.config; public final class ConfigSkill { public static int[] SHOCK_STUN_TIMER; }')
java(skill+'L1SkillId','package '+skill[:-1]+'; public final class L1SkillId { public static final int SHOCK_STUN=87,ERASE_MAGIC=153; }')
java(skill+'L1SkillTimer','package '+skill[:-1]+'; public class L1SkillTimer {}')
java(package+'L1Magic','package '+package[:-1]+'; public class L1Magic {}')
java(skill+'skillmode.SkillMode','package '+skill+'skillmode; public class SkillMode {}')
java('com.lineage.server.serverpackets.S_Paralysis','package com.lineage.server.serverpackets; public record S_Paralysis(int type,boolean active) { public static final int TYPE_STUN=1; }')
java(package+'L1Character','''package com.lineage.server.model;
import java.util.*;import com.lineage.server.model.skill.*;
public class L1Character { public final List<String> events=new ArrayList<>();public int sets,millis,remaining,heals,kills;public boolean has;
 public boolean hasSkillEffect(int id){if(id!=87)throw new AssertionError();return has;} public int getSkillEffectTimeSec(int id){return remaining;}
 public void setSkillEffect(int id,int ms){if(id!=87)throw new AssertionError();sets++;millis=ms;events.add("status");}
 public int getX(){return 12;}public int getY(){return 34;}public short getMapId(){return 56;}
 public void adjustCurrentHpIfAlive(int hp,int mp){heals+=hp;}public void killSkillEffectTimer(int id,L1SkillTimer lease){kills++;}
}''')
java(inst+'L1PcInstance','''package com.lineage.server.model.Instance;
import com.lineage.server.serverpackets.S_Paralysis;public class L1PcInstance extends com.lineage.server.model.L1Character {public int apply,stop;public void sendPackets(S_Paralysis p){if(p.type()!=S_Paralysis.TYPE_STUN)throw new AssertionError();if(p.active())apply++;else stop++;events.add(p.active()?"paralysis":"stop");}}''')
java(inst+'L1NpcInstance','''package com.lineage.server.model.Instance;
public class L1NpcInstance extends com.lineage.server.model.L1Character {public int apply,stop;public void setParalyzed(boolean p){if(p)apply++;else stop++;events.add(p?"paralysis":"stop");}}''')
for name in ['Monster','Summon','Guardian','Guard','Pet']:java(inst+'L1'+name+'Instance','package '+inst[:-1]+'; public class L1'+name+'Instance extends L1NpcInstance {}')
java('com.lineage.server.utils.L1SpawnUtil','''package com.lineage.server.utils;
import com.lineage.server.model.L1Character;public final class L1SpawnUtil {public static int count,seconds;public static void spawnEffect(int id,int duration,int x,int y,int map,L1Character who,int z){if(id!=81162||x!=12||y!=34||map!=56||z!=0)throw new AssertionError("factory args");count++;seconds=duration;who.events.add("factory");}}''')
java(package+'talent.TalentDamageCalculator','package '+package+'talent; public final class TalentDamageCalculator {public static void tryTalentStun(com.lineage.server.model.Instance.L1PcInstance a,com.lineage.server.model.L1Character t,int id){throw new AssertionError("unselected talent path");}}')
java(skill+'DamageCommitOutcome','package '+skill[:-1]+'; public record DamageCommitOutcome(boolean accepted,boolean terminal) {}')
java(skill+'DamageCommitPlan','package '+skill[:-1]+'; public interface DamageCommitPlan { void afterAccepted(DamageCommitOutcome outcome); }')
java(skill+'skillmode.SHOCK_STUN',shock.read_text('utf-8'))
wrapper='''package com.lineage.server.model.skill;
import static com.lineage.server.model.skill.L1SkillId.ERASE_MAGIC;
import com.lineage.server.model.*;import com.lineage.server.model.Instance.*;import com.lineage.config.ConfigSkill;import com.lineage.server.utils.L1SpawnUtil;import java.util.*;
public class L1SkillUse {
 static final java.util.concurrent.atomic.AtomicInteger ERROR_COUNT=new java.util.concurrent.atomic.AtomicInteger();static final Log _log=new Log();static class Log {void error(String m,Object... args){throw new AssertionError(m);}}
 L1Character _user;int sides,gfx;void applyFreezeTargetEffect(L1Character t){throw new AssertionError();}void applyThunderGrabTargetEffect(L1Character t,int ms){throw new AssertionError();}void applyTargetSideEffects(L1Character t,int n){sides++;t.events.add("side");}void sendGrfx(boolean hit){gfx++;}
'''+commit+'\n'+mutation+'''
 static int checks,cases;static void need(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static L1Character target(int kind){return switch(kind){case 0->new L1PcInstance();case 1->new L1MonsterInstance();case 2->new L1SummonInstance();case 3->new L1GuardianInstance();case 4->new L1GuardInstance();case 5->new L1PetInstance();case 6->new L1NpcInstance();default->new L1Character();};}
 public static void main(String[] args)throws Exception {
 int[][] timers={new int[]{2,0},new int[]{2,2},null,new int[]{1},new int[]{3,0}};
 DamageCommitOutcome[] outcomes={null,new DamageCommitOutcome(false,false),new DamageCommitOutcome(false,true),new DamageCommitOutcome(true,true),new DamageCommitOutcome(true,false)};
 for(int kind=0;kind<8;kind++)for(int[] timer:timers)for(boolean refresh:new boolean[]{false,true})for(DamageCommitOutcome outcome:outcomes){
  ConfigSkill.SHOCK_STUN_TIMER=timer;var t=target(kind);t.has=refresh;t.remaining=7;var caster=new L1PcInstance();var cast=new L1SkillUse();cast._user=caster;L1SpawnUtil.count=0;var plan=new SkillTargetCommitPlan(cast,t,3,null,87,new SkillTargetMutationPlan(null,false,0,true),true);
  need(t.sets==0&&L1SpawnUtil.count==0,"planning has no mutation or effect factory");plan.afterAccepted(outcome);boolean accepted=outcome!=null&&outcome.accepted(),apply=accepted&&!outcome.terminal();need(t.sets==(apply?1:0)&&L1SpawnUtil.count==(apply?1:0),"Source accepted and nonterminal only");need(caster.heals==(accepted?3:0)&&cast.sides==(apply?1:0)&&cast.gfx==(accepted?1:0),"unchanged Source heal/side/impact gate");need(t.kills==0&&ERROR_COUNT.get()==0,"no unselected mutations or swallowed failures");
  if(apply){int base=timer!=null&&timer.length>=2?timer[0]:2,random=timer!=null&&timer.length>=2?timer[1]:2,max=base+random;need(t.millis>=base*1000&&t.millis<=max*1000&&t.millis%1000==0,"original timer range");if(refresh&&kind==0)need(t.millis==max*1000,"PC existing stun added and capped");need(L1SpawnUtil.seconds*1000==t.millis,"status and factory duration agree");if(kind<=5)need(t.events.equals(List.of("status","factory","paralysis","side")),"original status/factory/paralysis/side order");else need(t.events.equals(List.of("status","factory","side")),"unlisted NPC/base receives no paralysis");}
  int sets=t.sets;new com.lineage.server.model.skill.skillmode.SHOCK_STUN().stop(t);need(t.sets==sets,"stop does not set status");if(t instanceof L1PcInstance pc)need(pc.apply==(apply?1:0)&&pc.stop==1,"PC original STUN apply and stop packets");else if(t instanceof L1NpcInstance npc)need(npc.apply==(kind<=5&&apply?1:0)&&npc.stop==(kind<=5?1:0),"specific NPC type apply/stop guard");cases++;
 }
 need(cases==400,"fragment matrix");java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]),"{\\"cases\\":"+cases+",\\"checks\\":"+checks+",\\"Source_fragment_only\\":true,\\"full_goal_achieved\\":false}\\n");
 }
}
'''
java(skill+'L1SkillUse',wrapper)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',Source=[dict(original=pin(p),frozen=pin(frozen/p.name))for p in [original,shock]],extracted_records_verbatim=True,stubbed_dependencies_are_memory_observers=True,created_Hub=False,native_LiTo_cases=0,full_goal_achieved=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  subprocess.run(list(map(str,[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',run/'classes',*sorted(src.rglob('*.java'))])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',run/'classes',skill+'L1SkillUse',run/'observations.json'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
 observations=json.loads((run/'observations.json').read_text('utf-8'));assert observations['cases']==400;v.update(status='passed-verbatim-Source-afterAccepted-and-SHOCK_STUN-fragment-oracle',observations=observations,generated_sources=[pin(p)for p in sorted(src.rglob('*.java'))])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-shock-accepted-oracle-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=observations)))
