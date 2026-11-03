"""Verbatim Source target-failure predicate with actual readonly types; not runtime qualification."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server');read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def block(text,start):
 a=text.index(start);end=text.index('{',a)+1;depth=1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 return text[a:end]
build=read(D/'source-visibility-range-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'source-target-failure-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');src=run/'src';src.mkdir(parents=True);sources=[]
paths=[T/'model/skill/L1SkillUse.java',T/'model/skill/L1SkillId.java',T/'templates/L1Skills.java']+[T/('model/Instance/'+name+'.java')for name in ['L1PetInstance','L1SummonInstance','L1MonsterInstance']]
for p in paths:shutil.copyfile(p,run/p.name);sources.append(dict(original=pin(p),frozen=pin(run/p.name)))
use=paths[0].read_text('utf-8');ids=paths[1].read_text('utf-8');skill=paths[2].read_text('utf-8');predicate=block(use,'private boolean isTargetFailure(')
normal_start=use.index('case TYPE_NORMAL -> {');normal_end=use.index('case TYPE_LOGIN ->',normal_start);normal=use[normal_start:normal_end];side_effects=block(use,'private void applySideEffectsAfterNormalCast()');failure_start=use.index('if (((_skill.getType() == L1Skills.TYPE_CURSE)');failure_guard=block(use,'if (((_skill.getType() == L1Skills.TYPE_CURSE)')
assert normal.count('runSkill();')==1 and normal.count('applySideEffectsAfterNormalCast();')==1 and failure_guard.count('iter.remove();')==1
for name in ['L1PetInstance','L1SummonInstance','L1MonsterInstance']:assert re.search(r'class\s+'+name+r'\s+extends\s+L1NpcInstance', (T/('model/Instance/'+name+'.java')).read_text('utf-8'))
assert '&& isTargetFailure(cha)'in use and 'final L1SummonInstance summon = new L1SummonInstance'in use
constants='\n'.join(re.search(r'(?:public|private) static final int '+name+r'\s*=\s*\d+;',text)[0]for name,text in [('PC_PC',use),('TYPE_NORMAL',use),('TURN_UNDEAD',ids),('ERASE_MAGIC',ids),('SLOW',ids),('MANA_DRAIN',ids),('MASS_SLOW',ids),('ENTANGLE',ids),('WIND_SHACKLE',ids),('TYPE_CURSE',skill),('TYPE_PROBABILITY',skill)])
def java(name,s):p=src/(name+'.java');p.write_text('package local.lito381.server;\n'+s,'utf-8');return p
meta=java('TargetFailureMetadataProbe','''import java.nio.file.*;import java.util.*;public final class TargetFailureMetadataProbe {public static void main(String[]a)throws Exception{ResourceDatabase.load(ServerConfig.load(Path.of(a[0]),"standard"));var rows=new ArrayList<String>();for(int id:new int[]{18,29,39,44,76,152,153,167}){var r=NativeSkillTargetMetadata.row(id);rows.add(id+"\\t"+r.type());}Files.write(Path.of(a[1]),rows);var actors=new ArrayList<String>();for(int id:new int[]{45034,45306,45065})actors.add(id+"\\t"+SourceNpcPhysicalStats.memoryFixture(id).eraseEligible());Files.write(Path.of(a[2]),actors);}}''')
java('L1Character','class L1Character {int id;L1Character(int id){this.id=id;}int getId(){return id;}}')
java('L1PcInstance','class L1PcInstance extends L1Character {int clan;boolean nonpvp;L1PcInstance(int id,int clan){super(id);this.clan=clan;}int getClanid(){return clan;}boolean checkNonPvP(L1PcInstance a,L1Character b){return nonpvp;}}')
java('L1NpcInstance','class L1NpcInstance extends L1Character {L1NpcInstance(int id){super(id);}}')
java('L1PetInstance','class L1PetInstance extends L1NpcInstance {L1PetInstance(){super(3);}}')
java('L1SummonInstance','class L1SummonInstance extends L1NpcInstance {L1SummonInstance(){super(4);}}')
java('L1MonsterInstance','class L1MonsterInstance extends L1NpcInstance {NpcTemplate template;L1MonsterInstance(boolean erase,boolean tu,int undead){super(5);template=new NpcTemplate(erase,tu,undead);}NpcTemplate getNpcTemplate(){return template;}} record NpcTemplate(boolean erase,boolean tu,int undead){boolean get_IsErase(){return erase;}boolean get_IsTU(){return tu;}int get_undead(){return undead;}}')
java('L1TowerInstance','class L1TowerInstance extends L1NpcInstance {L1TowerInstance(){super(6);}}')
java('L1DoorInstance','class L1DoorInstance extends L1NpcInstance {L1DoorInstance(){super(7);}}')
java('L1Skills','class L1Skills {'+'\n'.join(re.search(r'public static final int '+name+r'\s*=\s*\d+;',skill)[0]for name in ['TYPE_CURSE','TYPE_PROBABILITY'])+'int type;L1Skills(int type){this.type=type;}int getType(){return type;}int getArea(){return 0;}String getTarget(){return "buff";}}')
wrapper='''import java.nio.file.*;import java.util.*;public final class SourceTargetFailureOracle {
'''+constants+'''
 int _calcType,_skillId;L1PcInstance _player=new L1PcInstance(1,8);L1Skills _skill;boolean _isGlanceCheckFail;int material=10,mp=10;final List<L1Character> targets=new ArrayList<>();final List<String> trace=new ArrayList<>();
'''+predicate+'''
 boolean cast(int type){switch(type){
'''+normal+'''}return true;}
'''+side_effects+'''
 boolean commitMaterialAndAcquireCastLocks(){material-=2;trace.add("material");return true;}void failSkill(){throw new AssertionError("provided cast is already admitted");}void breakInterruptedCastEffects(){}boolean toggleOffInvisibilityRecast(){return false;}void breakInvisibilityOnCast(){}void sendCastMotion(){trace.add("motion");}void flushTargetSideEffects(){trace.add("flush");}void useConsume(){mp-=3;trace.add("MP");}void sendGrfx(boolean value){trace.add("gfx");}void sendFailMessageHandle(){trace.add("failure-message");}
 void runSkill(){trace.add("run");for(var iter=targets.iterator();iter.hasNext();){var cha=iter.next();
'''+failure_guard+'''
 trace.add("effect");}}
 static int cases,checks,controlledFailure,controlledPass,ordered;static void need(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[]a)throws Exception{
  var types=new TreeMap<Integer,Integer>();for(var line:Files.readAllLines(Path.of(a[0]))){var p=line.split("\\t");types.put(Integer.parseInt(p[0]),Integer.parseInt(p[1]));}var rows=new ArrayList<String>();rows.add("skill\\tactual_type\\tcalcType\\tnonpvp\\tactor\\tpredicate_failure\\tactual_runSkill_failure_branch");
  for(var entry:types.entrySet())for(int calc=1;calc<=4;calc++)for(boolean nonpvp:new boolean[]{false,true}){
   var c=new SourceTargetFailureOracle();c._skillId=entry.getKey();c._calcType=calc;c._player.nonpvp=nonpvp;
   var actors=new ArrayList<L1Character>();actors.add(c._player);actors.add(new L1PcInstance(2,8));actors.add(new L1PcInstance(3,9));actors.add(new L1PetInstance());actors.add(new L1SummonInstance());actors.add(new L1TowerInstance());actors.add(new L1DoorInstance());for(boolean erase:new boolean[]{false,true})for(boolean tu:new boolean[]{false,true})for(int undead=0;undead<4;undead++)actors.add(new L1MonsterInstance(erase,tu,undead));
   for(var actor:actors){boolean failed=c.isTargetFailure(actor),expected;if(actor instanceof L1TowerInstance||actor instanceof L1DoorInstance)expected=true;else if(actor instanceof L1PcInstance pc)expected=calc==PC_PC&&nonpvp&&pc.getId()!=1&&pc.getClanid()!=8;else{boolean monster=actor instanceof L1MonsterInstance;var t=monster?((L1MonsterInstance)actor).template:new NpcTemplate(false,false,0);int id=entry.getKey();expected=id==TURN_UNDEAD&&(!t.tu()||t.undead()==0||t.undead()==2)||Set.of(ERASE_MAGIC,SLOW,MANA_DRAIN,MASS_SLOW,ENTANGLE,WIND_SHACKLE).contains(id)&&!t.erase()||id==MANA_DRAIN&&!monster;}
    need(failed==expected,"verbatim Source predicate actor="+actor.getClass().getSimpleName()+" skill="+entry.getKey());boolean applies=entry.getValue()==TYPE_CURSE||entry.getValue()==TYPE_PROBABILITY;boolean branch=applies&&failed;if(actor instanceof L1PetInstance||actor instanceof L1SummonInstance){if(branch)controlledFailure++;else controlledPass++;}rows.add(entry.getKey()+"\\t"+entry.getValue()+"\\t"+calc+"\\t"+nonpvp+"\\t"+actor.getClass().getSimpleName()+"\\t"+failed+"\\t"+branch);cases++;
    c.targets.clear();c.targets.add(actor);c.trace.clear();c.material=10;c.mp=10;c._skill=new L1Skills(entry.getValue());c.cast(TYPE_NORMAL);need(c.material==8&&c.mp==7,"provided normal cast pays material and MP even if target fails");need(c.targets.size()==(branch?0:1)&&c.trace.contains("effect")==!branch,"verbatim target failure removes target before effect");need(c.trace.equals(branch?List.of("material","motion","run","flush","MP","gfx","failure-message"):List.of("material","motion","run","effect","flush","MP","gfx","failure-message")),"verbatim normal cast/side-effect call ordering with memory stubs");ordered++;
   }
  }need(cases==1472&&ordered==1472&&controlledFailure>0&&controlledPass>0,"actual metadata and actor inheritance matrix");Files.write(Path.of(a[1]),rows);Files.writeString(Path.of(a[2]),"{\\"Source_reference_cases\\":"+cases+",\\"normal_cast_order_cases\\":"+ordered+",\\"checks\\":"+checks+",\\"controlled_actor_failure_branches\\":"+controlledFailure+",\\"controlled_actor_nonfailure_branches\\":"+controlledPass+",\\"native_Hub_cases\\":0,\\"full_goal_achieved\\":false}\\n");
 }
}
'''
java('SourceTargetFailureOracle',wrapper);shutil.copyfile(__file__,run/Path(__file__).name);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',build['runtime']['path'],S/'lib/*']));log=run/'test.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');v=dict(status='running',runtime=build['runtime'],Source=sources,predicate_verbatim=True,actual_Source_inheritance_verified=True,actual_static_types_from_existing_readonly_bootstrap=True,created_Hub=False,created_SQL_connection_beyond_existing_readonly_bootstrap=False,native_runtime_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',meta]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.TargetFailureMetadataProbe',S/'config/server.properties',run/'types.tsv',run/'native-template-eligibility.tsv'])
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',*[p for p in sorted(src.glob('*.java'))if p!=meta]]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceTargetFailureOracle',run/'types.tsv',run/'cases.tsv',run/'observations.json'])
 o=read(run/'observations.json');v.update(status='passed-verbatim-Source-target-failure-reference-native-consumers-still-unqualified',observations=o,types=pin(run/'types.tsv'),native_template_locator=pin(run/'native-template-eligibility.tsv'),cases=pin(run/'cases.tsv'),generated_sources=[pin(p)for p in sorted(src.glob('*.java'))])
 for row in sources:assert sha(Path(row['original']['path']))==row['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-target-failure-oracle-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
