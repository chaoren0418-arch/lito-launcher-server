"""Execute the actual Source calc-type assignment and area0 target-list prefix; no native Hub/SQL."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server');run=D/'source-line-target-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');src=run/'src';src.mkdir(parents=True);sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def java(name,text):
 p=src/(name+'.java');p.write_text(text,'utf-8')
def block(text,start):
 a=text.index(start);end=text.index('{',a)+1;depth=1
 while depth:
  if text[end]=='{':depth+=1
  elif text[end]=='}':depth-=1
  end+=1
 return text[a:end]
paths=[T/'model/skill/L1SkillUse.java',T/'model/skill/L1SkillId.java',T/'templates/L1Skills.java'];sources=[]
for p in paths:shutil.copyfile(p,run/p.name);sources.append(dict(original=pin(p),frozen=pin(run/p.name)))
use=paths[0].read_text('utf-8');ids=paths[1].read_text('utf-8');skills=paths[2].read_text('utf-8');start=use.index('private void makeTargetList()');cut=use.index('// 範圍攻擊',start);end=use.index('} else {',cut)+1;prefix=use[start:end];assert 'switch (this._calcType)'in prefix and 'getVisibleLineObjects'in prefix and 'if (this._skill.getArea() == 0)'in prefix
a=use.index('if (_user instanceof L1PcInstance) {',use.index('final L1Character targetCharacter'));z=use.index('\n\t\tswitch (_skillId)',a);assignment=use[a:z];assert assignment.count('_calcType =')==5
contains=block(use,'private boolean containsTarget(');constants='\n'.join(re.search(r'(?:public|private) static final int '+name+r'\s*=\s*\d+;',text)[0]for name,text in [('PC_PC',use),('PC_NPC',use),('NPC_PC',use),('NPC_NPC',use),('TYPE_LOGIN',use),('LIGHTNING',ids),('FREEZING_BREATH',ids),('IMMUNE_TO_HARM',ids)])
type_attack=re.search(r'public static final int TYPE_ATTACK\s*=\s*\d+;',skills)[0];to_me=re.search(r'public static final int TARGET_TO_ME\s*=\s*\d+;',skills)[0]
java('L1Object','class L1Object {int id,x;L1Object(int id,int x){this.id=id;this.x=x;}int getId(){return id;}int getX(){return x;}int getY(){return 0;}Location getLocation(){return new Location(x);}} record Location(int x){int getTileLineDistance(Location b){return Math.abs(x-b.x);}boolean isInScreen(Location b){return Math.abs(x-b.x)<=18;}}')
java('L1Character','class L1Character extends L1Object {boolean glance=true;L1Character(int id,int x){super(id,x);}boolean glanceCheck(int x,int y){return glance;}}')
java('L1PcInstance','class L1PcInstance extends L1Character {L1PcInstance(int id,int x){super(id,x);}}')
java('L1NpcInstance','class L1NpcInstance extends L1Character {L1NpcInstance(int id,int x){super(id,x);}}')
java('L1Skills','class L1Skills {'+type_attack+to_me+'int getTargetTo(){return 3;}int getType(){return TYPE_ATTACK;}int getRanged(){return 10;}String getTarget(){return "attack";}int getArea(){return 0;}}')
java('World','import java.util.*;class World {static final World singleton=new World();static World get(){return singleton;}int calls;L1Character primary,secondary;ArrayList<L1Object> getVisibleLineObjects(L1Object a,L1Object b){calls++;if(b!=primary)throw new AssertionError();return new ArrayList<>(List.of(primary,secondary));}}')
wrapper='''import java.util.*;public final class SourceLineTargetOracle {
'''+constants+'''
 L1Character _user,_target;L1NpcInstance _targetNpc;int _calcType,_type,_skillId;boolean _procCast,allowed=true;L1Skills _skill=new L1Skills();ArrayList<TargetStatus> _targetList=new ArrayList<>();
 record TargetStatus(L1Character target,boolean hit){TargetStatus(L1Character c){this(c,true);}L1Character getTarget(){return target;}}
 boolean isTarget(L1Character t){return allowed;}
 boolean assign(L1Object object){
'''+assignment+'''return true;}
'''+contains+'\n'+prefix+''' }catch(Exception e){throw new AssertionError(e);}}
 static int cases,counterfactual,checks,provided;static void need(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static SourceLineTargetOracle cast(int type,int skill,boolean proc,boolean glance,boolean allowed){var c=new SourceLineTargetOracle();c._skillId=skill;c._procCast=proc;c.allowed=allowed;c._user=type<=2?new L1PcInstance(1,0):new L1NpcInstance(1,0);c._target=type==1||type==3?new L1PcInstance(2,2):new L1NpcInstance(2,2);c._user.glance=glance;need(c.assign(c._target)&&c._calcType==type,"verbatim Source assigns only four actor combinations");World.get().primary=c._target;World.get().secondary=new L1PcInstance(3,1);World.get().calls=0;return c;}
 public static void main(String[]a)throws Exception{
  for(int type=1;type<=4;type++)for(int skill:new int[]{LIGHTNING,FREEZING_BREATH})for(boolean proc:new boolean[]{false,true})for(boolean glance:new boolean[]{false,true})for(boolean allowed:new boolean[]{false,true}){var c=cast(type,skill,proc,glance,allowed);c.makeTargetList();need(World.get().calls==0,"actual Source calcType does not enter skill-ID line labels");need(c._targetList.size()==(allowed?1:0),"Source area0 target count");if(allowed)need(c._targetList.get(0).target==c._target&&c._targetList.get(0).hit==glance,"only selected target; original glance-failure marker preserved");if(skill==FREEZING_BREATH)cases++;else provided++;}
  for(int wrong:new int[]{LIGHTNING,FREEZING_BREATH})for(int skill:new int[]{LIGHTNING,FREEZING_BREATH})for(boolean proc:new boolean[]{false,true})for(boolean glance:new boolean[]{false,true})for(boolean allowed:new boolean[]{false,true}){var c=cast(1,skill,proc,glance,allowed);c._calcType=wrong;c.makeTargetList();need(World.get().calls==(allowed?1:0)&&c._targetList.size()==(allowed?2:0),"counterfactual branch is functional only with an impossible calcType assignment");counterfactual++;}
  need(cases==32&&provided==32&&counterfactual==32,"complete observed prefix matrix");java.nio.file.Files.writeString(java.nio.file.Path.of(a[0]),"{\\"Source_actual_cases\\":"+cases+",\\"counterfactual_cases\\":"+counterfactual+",\\"checks\\":"+checks+",\\"actual_Source_line_branch_calls\\":0,\\"provided_skill_area\\":0,\\"Source_fragment_only\\":true,\\"full_goal_achieved\\":false}\\n");
 }
}
'''
java('SourceLineTargetOracle',wrapper);shutil.copyfile(__file__,run/Path(__file__).name);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');log=run/'test.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');v=dict(status='running',Source=sources,assignment_and_target_prefix_verbatim=True,provided_metadata_scope='194 actual row is area0/through0/ranged10/attack/target_to3; the 17 area0 cases are synthetic provided-metadata cases (actual17 row has area3). Native row proof recorded separately.',created_Hub=False,created_SQL_connection=False,native_LiTo_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',run/'classes',*sorted(src.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',run/'classes','SourceLineTargetOracle',run/'observations.json'])
 o=json.loads((run/'observations.json').read_text());assert o['Source_actual_cases']==32 and o['counterfactual_cases']==32;v.update(status='passed-current-Source-calcType-area0-single-target-prefix-with-unreachable-line-labels',observations=o,generated_sources=[pin(p)for p in sorted(src.glob('*.java'))])
 for row in sources:assert sha(Path(row['original']['path']))==row['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-line-target-oracle-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
