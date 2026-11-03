"""Original Source radius predicates plus calcType constants; pure memory geometry, no Hub/SQL."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server');read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def block(s,start):
 a=s.index(start);end=s.index('{',a)+1;depth=1
 while depth:
  if s[end]=='{':depth+=1
  elif s[end]=='}':depth-=1
  end+=1
 return s[a:end]
build=read(D/'source-lightning-area-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'source-lightning-area-reference'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');src=run/'src';src.mkdir(parents=True);files=[T/'types/Point.java',T/'world/World.java',T/'model/skill/L1SkillUse.java',T/'model/skill/L1SkillId.java'];refs=[]
for p in files:shutil.copyfile(p,run/p.name);refs.append(dict(original=pin(p),frozen=pin(run/p.name)))
point,world,use,ids=[p.read_text('utf-8')for p in files];make=block(use,'private void makeTargetList(');assert'switch (this._calcType)'in make and'case LIGHTNING:'in make and'case FREEZING_BREATH:'in make and'getVisibleObjects(this._target, this._skill.getArea())'in make
calcs={name:int(re.search(r'(?:public|private) static final int '+name+r'\s*=\s*(\d+);',use)[1])for name in ['PC_PC','PC_NPC','NPC_PC','NPC_NPC']};switches={name:int(re.search(r'public static final int '+name+r'\s*=\s*(\d+);',ids)[1])for name in ['LIGHTNING','FREEZING_BREATH']};assert sorted(calcs.values())==[1,2,3,4]and switches=={'LIGHTNING':17,'FREEZING_BREATH':194}and not set(calcs.values())&set(switches.values())
fragments='\n'.join(block(point,start)for start in ['public boolean isInScreen(','public int getTileDistance(','public int getTileLineDistance(']).replace('Point','SourceAreaPoint');(src/'SourceAreaPoint.java').write_text('package local.lito381.server;final class SourceAreaPoint {final int x,y;SourceAreaPoint(int x,int y){this.x=x;this.y=y;}int getX(){return x;}int getY(){return y;}boolean isSamePoint(SourceAreaPoint p){return x==p.x&&y==p.y;}\n'+fragments+'\n}\n','utf-8')
radius=re.sub(r'\bPoint\b','SourceAreaPoint',block(world,'private static boolean matchesNearbyRadius('))
(src/'SourceLightningAreaReference.java').write_text('''package local.lito381.server;
import java.util.*;import java.nio.file.*;
public final class SourceLightningAreaReference {
'''+radius+'''
 public static void main(String[]a)throws Exception{int cases=0,checks=0;var field=NativeSkillTargetMetadata.class.getDeclaredField("rows");field.setAccessible(true);Object previous=field.get(null);field.set(null,Map.of(17,new NativeSkillTargetMetadata.Row("attack",3,6,3,false,64,0,0)));
 try{for(int x:new int[]{128,33000,65530})for(int y:new int[]{128,32700,65530}){var center=new SourceAreaPoint(x,y);var points=NativeSourceLightningArea.points(x-6,y,x,y,BattleSpellCatalog.lineAttackSpell(17));if(points.size()!=49||new HashSet<>(points).size()!=49)throw new AssertionError("bounded49 distinct tiles");checks++;for(int dx=-12;dx<=12;dx++)for(int dy=-12;dy<=12;dy++){boolean expected=matchesNearbyRadius(center,new SourceAreaPoint(x+dx,y+dy),3);if(BattleStraightLine.contains(points,x+dx,y+dy)!=expected)throw new AssertionError("Source radius predicate");checks++;cases++;}var old=BattleStraightLine.points(x-6,y,x,y);if(!NativeSourceLightningArea.points(x-6,y,x,y,BattleSpellCatalog.freezeAttackSpell(194)).equals(old))throw new AssertionError("all other skills retain byte-identical original line enumeration");checks++;}}finally{field.set(null,previous);}Files.writeString(Path.of(a[0]),"{\\"Source_reference_cases\\":"+cases+",\\"checks\\":"+checks+",\\"native_Hub_cases\\":0,\\"full_goal_achieved\\":false}\\n");}
}
''','utf-8')
shutil.copyfile(__file__,run/Path(__file__).name);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',build['runtime']['path'],S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],Source=refs,calcType_constants=calcs,switch_skill_constants=switches,calcType_switch_analysis='Original switch compares calcType1..4 to skill IDs17/194; current Source17 falls through to area3. This is source-structure analysis, not a live Source cast.',memory_metadata_only=True,created_Hub=False,created_SQL_connection=False,native_Hub_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',*src.glob('*.java')]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceLightningAreaReference',run/'observations.json'])
 o=read(run/'observations.json');assert o['Source_reference_cases']==5625;v.update(status='passed-verbatim-Source-radius-and-statically-proven-calcType-fallthrough',observations=o,generated_sources=[pin(p)for p in src.glob('*.java')])
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-lightning-area-reference-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
