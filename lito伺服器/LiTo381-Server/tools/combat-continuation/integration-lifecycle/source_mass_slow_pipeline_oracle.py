"""Source76 area target construction, primary/per-target calc, failure and buff visual fragments. No Hub or SQL."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,re,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def block(s,start):
 a=s.index(start);end=s.index('{',a)+1;depth=1
 while depth:
  if s[end]=='{':depth+=1
  elif s[end]=='}':depth-=1
  end+=1
 return s[a:end]
run=D/'source-mass-slow-pipeline-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');src=run/'src';src.mkdir(parents=True)
paths=[T/'model/skill/L1SkillUse.java',T/'model/skill/L1SkillId.java',T/'templates/L1Skills.java']+[T/('model/Instance/'+n+'.java')for n in ['L1PetInstance','L1SummonInstance','L1MonsterInstance']]
sources=[]
for p in paths:shutil.copyfile(p,run/p.name);sources.append(dict(original=pin(p),frozen=pin(run/p.name)))
use=paths[0].read_text('utf-8');ids=paths[1].read_text('utf-8');skills=paths[2].read_text('utf-8')
models=D/'native-source-shape-audit/20261023T110616667496Z/models.tsv';types=D/'source-target-failure-oracle/20261023T111251313858Z/types.tsv'
model=next(l.split('\t')for l in models.read_text('utf-8').splitlines()if l.startswith('76\t'));assert model[5:10]==['buff','3','-1','3','false'];assert '76\t1'in types.read_text('utf-8').splitlines()
calc=block(use,'private boolean isTargetCalc(');failure=block(use,'private boolean isTargetFailure(');targets=block(use,'private void makeTargetList()')
primary=block(use,'if (!this.isTargetCalc(_target))');per=block(use,'if (!ts.isCalc() || !this.isTargetCalc(cha))');failed=block(use,'if (((_skill.getType() == L1Skills.TYPE_CURSE)')
gfx_start=use.index('private void sendGrfx(');visual=block(use[gfx_start:],'if ((_targetList.size() == 0) && !(_skill.getTarget().equals("none")))')
sound='_player.sendPacketsX10(new S_SkillSound(targetid, castgfx));';assert sound in use[gfx_start:]
normal_start=use.index('case TYPE_NORMAL -> {');normal=use[normal_start:use.index('case TYPE_LOGIN ->',normal_start)];after=block(use,'private void applySideEffectsAfterNormalCast()')
def const(name,text):return re.search(r'(?:public|private) static final int '+name+r'\s*=\s*\d+;',text)[0]
constants='\n'.join(const(n,text)for text,names in [(use,['PC_PC','PC_NPC','TYPE_NORMAL','TYPE_LOGIN']),(ids,['IMMUNE_TO_HARM','LIGHTNING','FREEZING_BREATH','HEAL_ALL','MASS_SLOW','FOG_OF_SLEEPING','MASS_TELEPORT','TURN_UNDEAD','ERASE_MAGIC','SLOW','MANA_DRAIN','ENTANGLE','WIND_SHACKLE'])]for n in names)
def java(name,s):p=src/(name+'.java');p.write_text('package local.lito381.server;\n'+s,'utf-8');return p
java('L1Object','class L1Object {int id;L1Object(int id){this.id=id;}int getId(){return id;}}')
java('L1Character','class L1Character extends L1Object {L1Character(int id){super(id);}int getX(){return 0;}int getY(){return 0;}boolean isSafetyZone(){return false;}boolean glanceCheck(int x,int y){return true;}Location getLocation(){return new Location();}} class Location{int getTileLineDistance(Location other){return 0;}boolean isInScreen(Location other){return true;}}')
java('L1PcInstance','class L1PcInstance extends L1Character {int sounds,lastSoundId;L1PcInstance(int id){super(id);}int getClanid(){return 8;}boolean checkNonPvP(L1PcInstance a,L1Character b){return false;}int getTempCharGfx(){return 0;}void sendPacketsX10(Object p){if(p instanceof S_SkillSound s){sounds++;lastSoundId=s.id();}}}')
java('L1NpcInstance','class L1NpcInstance extends L1Character {L1NpcInstance(int id){super(id);}}')
for name in ['L1DeInstance','L1ScarecrowInstance','L1TowerInstance','L1DoorInstance']:java(name,'class '+name+' extends L1NpcInstance {'+name+'(){super(90);}}')
for name in ['L1PetInstance','L1SummonInstance']:java(name,'class '+name+' extends L1NpcInstance {final L1PcInstance master;'+name+'(int id,L1PcInstance master){super(id);this.master=master;}L1PcInstance getMaster(){return master;}boolean isExsistMaster(){return master!=null;}}')
java('L1MonsterInstance','class L1MonsterInstance extends L1NpcInstance {L1MonsterInstance(){super(80);}NpcTemplate getNpcTemplate(){return new NpcTemplate();}}class NpcTemplate{boolean get_IsErase(){return false;}boolean get_IsTU(){return false;}int get_undead(){return 0;}}')
java('L1Skills','class L1Skills {'+'\n'.join(const(n,skills)for n in ['TYPE_CURSE','TYPE_PROBABILITY','TYPE_ATTACK','TARGET_TO_ME'])+'String getTarget(){return "buff";}int getTargetTo(){return 3;}int getRanged(){return -1;}int getArea(){return 3;}int getType(){return TYPE_PROBABILITY;}}')
java('TargetStatus','class TargetStatus {final L1Character target;boolean calc;TargetStatus(L1Character c){this(c,true);}TargetStatus(L1Character c,boolean calc){target=c;this.calc=calc;}L1Character getTarget(){return target;}boolean isCalc(){return calc;}void isCalc(boolean value){calc=value;}}')
java('World','class World {static final World ONE=new World();final java.util.ArrayList<L1Object> nearby=new java.util.ArrayList<>();static World get(){return ONE;}java.util.ArrayList<L1Object> getVisibleLineObjects(L1Character a,L1Character b){return new java.util.ArrayList<>(nearby);}java.util.List<L1Object> getVisibleObjects(L1Character a){return nearby;}java.util.List<L1Object> getVisibleObjects(L1Character a,int area){return nearby;}}')
java('S_SkillSound','record S_SkillSound(int id,int gfx){}')
java('S_DoActionGFX','record S_DoActionGFX(int id,int action){}')
java('ActionCodes','class ActionCodes {static final int ACTION_SkillBuff=1,ACTION_Attack=2;}')
body='''import java.nio.file.*;import java.util.*;public final class SourceMassSlowPipelineOracle {
'''+constants+'''
 final L1PcInstance _player=new L1PcInstance(1),enemy=new L1PcInstance(2);L1Character _user=_player,_target;final L1Skills _skill=new L1Skills();final ArrayList<TargetStatus> _targetList=new ArrayList<>();int _skillId=MASS_SLOW,_type=TYPE_NORMAL,_calcType;boolean _procCast,_isGlanceCheckFail,_castMotionSent=true;boolean success;int applied,material=10,mp=10;final List<String> trace=new ArrayList<>();
 boolean isTarget(L1Character c){return true;}boolean containsTarget(L1Character c){return _targetList.stream().anyMatch(t->t.getTarget()==c);}
'''+block(use,'private boolean isPcSummonPet(')+'\n'+calc+'\n'+failure+'\n'+targets+'''
 void runSkill(){trace.add("run");
'''+primary+'''
  for(var iter=_targetList.iterator();iter.hasNext();){var ts=iter.next();var cha=ts.getTarget();
'''+per+'\n'+failed+'''
   if(!success){iter.remove();continue;}applied++;
  }
 }
 void sendGrfx(boolean isSkillAction){int actionId=1;final int castgfx=777;final int targetid=_target.getId();
'''+visual+'\n'+sound+'''
 }
 boolean cast(int type){switch(type){
'''+normal+'''}return true;}
'''+after+'''
 boolean commitMaterialAndAcquireCastLocks(){material-=2;trace.add("material");return true;}void failSkill(){throw new AssertionError();}void breakInterruptedCastEffects(){}boolean toggleOffInvisibilityRecast(){return false;}void breakInvisibilityOnCast(){}void sendCastMotion(){trace.add("motion");}void flushTargetSideEffects(){trace.add("flush");}void useConsume(){mp-=3;trace.add("MP");}void sendFailMessageHandle(){trace.add("failure-message");}
 static int cases,checks,ownCases,soleNpcVisuals;static void need(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[]a)throws Exception{var rows=new ArrayList<String>();rows.add("primary\\tnearby\\tprobability_success_stub\\tprimary_calc\\ttarget_list_after_run\\tapplied\\tprimary_visual_count");
  for(int kind=0;kind<6;kind++)for(int nearby=0;nearby<3;nearby++)for(boolean success:new boolean[]{false,true}){
   var c=new SourceMassSlowPipelineOracle();c.success=success;c._target=switch(kind){case 0->new L1PetInstance(3,c.enemy);case 1->new L1SummonInstance(4,c.enemy);case 2->new L1PetInstance(5,c._player);case 3->new L1SummonInstance(6,c._player);case 4->c._player;default->c.enemy;};c._calcType=c._target instanceof L1PcInstance?PC_PC:2;World.get().nearby.clear();for(int i=0;i<nearby;i++)World.get().nearby.add(new L1PcInstance(20+i));c.makeTargetList();need(c._targetList.size()==2+nearby&&c.containsTarget(c._player),"verbatim buff area3 always retains caster entry before effects");boolean calc=c.isTargetCalc(c._target);c.cast(TYPE_NORMAL);
   boolean own=kind>=2&&kind<=4;int expected=own?0:(nearby+(kind==5?1:0))*(success?1:0);need(calc==!own&&c.applied==expected,"verbatim primary calc suppresses entire effect loop for own PC/PET/SUMMON");need(c.material==8&&c.mp==7,"admitted normal cast pays exactly once, including empty effects");need(c._player.sounds==1&&c._player.lastSoundId==c._target.getId(),"retained caster entry keeps one primary-target buff visual even with no applied effect");need(c.containsTarget(c._player),"isCalc(false) continue preserves caster instead of removing it");need(c.trace.equals(List.of("material","motion","run","flush","MP","failure-message")),"verbatim normal cast and post-run side effects");if(own)ownCases++;if(kind<2&&nearby==0)soleNpcVisuals++;rows.add(kind+"\\t"+nearby+"\\t"+success+"\\t"+calc+"\\t"+c._targetList.size()+"\\t"+c.applied+"\\t"+c._player.sounds);cases++;
  }need(cases==36&&ownCases==18&&soleNpcVisuals==4,"full bounded Source76 pipeline reference matrix");Files.write(Path.of(a[0]),rows);Files.writeString(Path.of(a[1]),"{\\"Source_pipeline_reference_cases\\":"+cases+",\\"checks\\":"+checks+",\\"own_primary_whole_effect_loop_suppression_cases\\":"+ownCases+",\\"sole_failed_NPC_primary_visual_cases\\":"+soleNpcVisuals+",\\"native_Hub_cases\\":0,\\"full_goal_achieved\\":false}\\n");
 }
}
'''
java('SourceMassSlowPipelineOracle',body);shutil.copyfile(__file__,run/Path(__file__).name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');log=run/'test.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8')
v=dict(status='running',Source=sources,existing_static_model=pin(models),existing_static_type=pin(types),created_Hub=False,created_SQL_connection=False,probability_outcome_stub_only=True,isTarget_selection_stub_all_true=True,native_runtime_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',run/'classes',*sorted(src.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',run/'classes','local.lito381.server.SourceMassSlowPipelineOracle',run/'cases.tsv',run/'observations.json'])
 v.update(status='passed-36-verbatim-Source76-pipeline-reference-native-visual-and-own-primary-consumers-open',observations=read(run/'observations.json'),generated_sources=[pin(p)for p in src.iterdir()],cases=pin(run/'cases.tsv'))
 for row in sources:assert sha(Path(row['original']['path']))==row['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-mass-slow-pipeline-oracle-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
