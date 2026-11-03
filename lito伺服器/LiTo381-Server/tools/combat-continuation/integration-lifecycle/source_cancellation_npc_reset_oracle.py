"""Verbatim Source cancellation loops and explicit non-PC speed reset; no Hub/SQL."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os,re
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';T=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill');run=D/'source-cancellation-NPC-reset-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
sources=[]
for p in [T/'skillmode/CANCELLATION.java',T/'L1SkillId.java']:shutil.copyfile(p,run/p.name);sources.append(dict(original=pin(p),frozen=pin(run/p.name)))
source=(T/'skillmode/CANCELLATION.java').read_text('utf-8');ids=(T/'L1SkillId.java').read_text('utf-8');a=source.index('// 技能狀態解除');b=source.index('// 對象是PC',a);loops=source[a:b];a=source.index('else {// 對象不是PC',b);end=source.index('\n\t\t}',a)+4;npc=source[a:end];assert npc.count('setMoveSpeed(0)')==1 and npc.count('setBraveSpeed(0)')==1 and npc.count('setParalyzed(false)')==1
constants='\n'.join(re.search(r'public static final int '+name+r'\s*=\s*\d+;',ids)[0]for name in ['SKILLS_BEGIN','SKILLS_END','STATUS_BEGIN','STATUS_END','COOKING_BEGIN','COOKING_END','STATUS_FREEZE'])
java='''import java.util.*;import java.nio.file.*;public final class SourceCancellationNpcResetOracle {
'''+constants+'''
 static final class L1SkillMode {static final L1SkillMode INSTANCE=new L1SkillMode();Set<Integer> immune=Set.of();static L1SkillMode get(){return INSTANCE;}boolean isNotCancelable(int id){return immune.contains(id);}}
 static final class L1NpcInstance {final Set<Integer> effects=new HashSet<>();final List<Object> packets=new ArrayList<>();boolean dead,paralyzed;int speed,brave,stack;int getId(){return 9;}boolean isDead(){return dead;}void removeSkillEffect(int id){effects.remove(id);}void curePoison(){}void cureParalaysis(){}void setMoveSpeed(int n){speed=n;}void setBraveSpeed(int n){brave=n;}void setWeaponBreakStack(int n){stack=n;}void setParalyzed(boolean n){paralyzed=n;}void broadcastPacketAll(Object p){packets.add(p);}}
 record S_SkillHaste(int id,int speed,int time){}record S_SkillBrave(int id,int speed,int time){}
 static void apply(L1NpcInstance cha){
'''+loops+'''
 if(false){}'''+npc+'''
 }
 static int cases,checks;static void need(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[]a)throws Exception{for(boolean hasteImmune:new boolean[]{false,true})for(boolean forestImmune:new boolean[]{false,true})for(boolean dead:new boolean[]{false,true})for(int speed=0;speed<=2;speed++)for(int brave=0;brave<2;brave++)for(boolean paralyzed:new boolean[]{false,true})for(int stack=0;stack<3;stack++){
  var flags=new HashSet<Integer>();if(hasteImmune)flags.add(43);if(forestImmune)flags.add(161);flags.add(STATUS_FREEZE);L1SkillMode.INSTANCE.immune=Set.copyOf(flags);var c=new L1NpcInstance();c.effects.addAll(Set.of(43,161,STATUS_FREEZE));c.dead=dead;c.speed=speed;c.brave=brave;c.paralyzed=paralyzed;c.stack=stack;apply(c);
  need(c.effects.contains(43)==(hasteImmune&&!dead)&&c.effects.contains(161)==(forestImmune&&!dead),"Source loops retain live immune skill IDs");need(!c.effects.contains(STATUS_FREEZE),"Source always removes STATUS_FREEZE");need(c.speed==0&&c.brave==0&&c.stack==0&&!c.paralyzed,"Source NPC explicit reset overrides movement even with immune skill43");need(c.packets.equals(List.of(new S_SkillHaste(9,0,0),new S_SkillBrave(9,0,0))),"Source emits the two NPC movement reset packets");cases++;
 }need(cases==288,"provided state/control combinations");Files.writeString(Path.of(a[0]),"{\\"Source_reference_cases\\":"+cases+",\\"checks\\":"+checks+",\\"native_Hub_cases\\":0,\\"full_goal_achieved\\":false}\\n");}
}
''';p=run/'SourceCancellationNpcResetOracle.java';p.write_text(java,'utf-8');shutil.copyfile(__file__,run/Path(__file__).name);J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',Source=sources,loops_and_NPC_reset_verbatim=True,created_Hub=False,created_SQL_connection=False,native_runtime_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-d',run/'classes',p]);invoke([J/'java.exe','-Xverify:all','-cp',run/'classes','SourceCancellationNpcResetOracle',run/'observations.json'])
 o=json.loads((run/'observations.json').read_text('utf-8'));v.update(status='passed-verbatim-Source-non-PC-cancellation-reset-reference',observations=o,generated_source=pin(p))
 for row in sources:assert sha(Path(row['original']['path']))==row['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-cancellation-NPC-reset-oracle-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
