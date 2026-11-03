"""Execute frozen Source methods/tail in isolation; no Hub, login, DB connection or Source build."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'static-respawn-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];bm=read(Path(build['manifest']['path']));source={Path(x['original']['path']).name:Path(x['frozen']['path'])for x in bm['Source']}
run=D/'static-respawn-Source-fragments'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name)
for p in source.values():shutil.copyfile(p,inputs/p.name)
def method(text,start):
 a=text.index(start);b=text.index('\n\t}',a)+3;return text[a:b]
spawn=source['L1Spawn.java'].read_text('utf-8');npc=source['L1NpcInstance.java'].read_text('utf-8')
calc=method(spawn,'private int calcRespawnDelay()');execute=method(spawn,'public void executeSpawnTask(final int spawnNumber, final int objectId)')
a=npc.index('final boolean removedFromWorld = !world.isObjectRegistered(this);');b=npc.index('finishDeletionObservation(NpcDeletionVisibilityState.Phase.TERMINAL);',a)+len('finishDeletionObservation(NpcDeletionVisibilityState.Phase.TERMINAL);');tail=npc[a:b]
code='''package local.lito381.server;
import java.util.*;import java.util.concurrent.atomic.AtomicLong;
/** Only interfaces around exact Source fragments are stubbed. */
public final class StaticRespawnSourceFragments {
 static int checks,cases;static void need(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static final class Log {void warn(String s,Object...o){}void error(String s,Object...o){}}
 static final class L1GameTime {Date toTime(){return new Date(0);}}
 static final class L1GameTimeClock {static L1GameTimeClock getInstance(){return new L1GameTimeClock();}L1GameTime currentTime(){return new L1GameTime();}}
 static final class Period {boolean includes(L1GameTime t){return true;}}
 static final class Slot {Period getTimePeriod(){return new Period();}Date getTimeStart(){return new Date(0);}}
 static final class Spawn {
  int _minRespawnDelay,_delayInterval,_id=1,_npcid=71092,scheduled,released;long scheduledDelay;int scheduledNumber,scheduledId;
  final Set<Integer> _pendingRespawnSlots=new HashSet<>();static final AtomicLong SPAWN_DUPLICATE_REQUEST_SUPPRESSED_COUNT=new AtomicLong();static final Log _log=new Log();static final Random _random=new Random(0);Slot _time;Object _nextSpawnTime;
  String getSpawnConfigTable(){return "spawnlist_npc";}int getId(){return _id;}void releaseActiveNpc(Npc n){released++;}
  final class SpawnTask {final int n,id;final long delay;SpawnTask(int n,int id,long d){this.n=n;this.id=id;delay=d;}void getStart(){scheduled++;scheduledDelay=Math.max(0,delay);scheduledNumber=n;scheduledId=id;}}
  CALC
  EXECUTE
 }
 static final class World {boolean registered;boolean isObjectRegistered(Npc n){return registered;}}
 static final class NpcDeletionVisibilityState {enum Phase{FAILED,TERMINAL}}
 static final class Npc {
  final World world=new World();Spawn _spawn;boolean respawn;int _spawnNumber=3;NpcDeletionVisibilityState.Phase phase;
  static final AtomicLong DELETE_VERIFY_FAILURE_COUNT=new AtomicLong();static final Log _log=new Log();
  int getId(){return 23;}int getNpcId(){return 71092;}boolean isReSpawn(){return respawn;}void finishDeletionObservation(NpcDeletionVisibilityState.Phase p){phase=p;}
  void completeDeletion(){TAIL}
 }
 public static void main(String[] args)throws Exception {
  for(boolean registered:new boolean[]{false,true})for(boolean owner:new boolean[]{false,true})for(boolean respawn:new boolean[]{false,true}){
   var n=new Npc();n.world.registered=registered;n.respawn=respawn;var s=new Spawn();s._minRespawnDelay=7;if(owner)n._spawn=s;n.completeDeletion();
   need(s.released==(!registered&&owner?1:0),"Source releases owner only after world removal");need(s.scheduled==(!registered&&owner&&respawn?1:0),"Source respawns only verified removal with owner and flag");
   need(n.phase==(registered?NpcDeletionVisibilityState.Phase.FAILED:NpcDeletionVisibilityState.Phase.TERMINAL),"Source deletion completion phase");
   if(s.scheduled>0){need(s.scheduledDelay==7000&&s.scheduledId==0&&s.scheduledNumber==3,"Source preserves spawn slot and requests new object");s.executeSpawnTask(3,0);need(s.scheduled==1,"Source duplicate pending slot suppressed");}cases++;
  }
  for(int delay:new int[]{Integer.MIN_VALUE,-2147484,-1,0,1,7,2147483,2147484,Integer.MAX_VALUE}){
   var s=new Spawn();s._minRespawnDelay=delay;int source=s.calcRespawnDelay();s.executeSpawnTask(3,0);var row=new NativeStaticSpawnMetadata.Row(1,71092,1,0,0,0,0,0,delay,4,0,false);
   need(row.delayMillis()==Math.max(0,source)&&row.delayMillis()==s.scheduledDelay,"native metadata preserves Source int overflow and scheduler clamp "+delay);need(row.fixed(),"fixed untimed row");cases++;
  }
  for(int mode=0;mode<7;mode++){
   var row=new NativeStaticSpawnMetadata.Row(1,71092,mode==0?0:1,0,0,mode==1?1:0,mode==2?1:0,mode==3?-1:mode==4?8:0,0,4,mode==6?3:0,mode==5);
   need(row.fixed()==(mode==6),"unsupported random/time/heading/count explicit; movement retains native motion");cases++;
  }
  java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]),"{\\"cases\\":"+cases+",\\"checks\\":"+checks+",\\"fragment_scope_only\\":true}\\n");
 }
}
'''.replace('CALC',calc).replace('EXECUTE',execute).replace('TAIL',tail)
java=inputs/'StaticRespawnSourceFragments.java';java.write_text(code,'utf-8');classes=run/'classes';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],fragments=[dict(owner='L1Spawn',method='calcRespawnDelay',body=calc),dict(owner='L1Spawn',method='executeSpawnTask',body=execute),dict(owner='L1NpcInstance',method='deleteClaimed',scope='verified world-removal tail only',body=tail)],Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False,same_SHA_full_regression_passed=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=120)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',jar,'-d',classes,java]);invoke([J/'java.exe','-Xverify:all','-cp',str(classes)+';'+str(jar),'local.lito381.server.StaticRespawnSourceFragments',run/'observations.json'])
 o=read(run/'observations.json');assert o['cases']==24;v.update(status='passed-isolated-Source-fragments-and-native-metadata-delay',observations=o)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'static-respawn-Source-fragments-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
