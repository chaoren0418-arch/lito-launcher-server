"""Verbatim Source Point screen function versus installed material-range projection, memory metadata only."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def block(s,start):
 a=s.index(start);end=s.index('{',a)+1;depth=1
 while depth:
  if s[end]=='{':depth+=1
  elif s[end]=='}':depth-=1
  end+=1
 return s[a:end]
build=read(D/'source-visibility-range-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'source-screen-range-oracle'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');src=run/'src/local/lito381/server';src.mkdir(parents=True);original=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/types/Point.java');text=original.read_text('utf-8');screen=block(text,'public boolean isInScreen(');distance=block(text,'public int getTileDistance(');fragment=(screen+'\n'+distance).replace('Point','SourceScreenPoint');shutil.copyfile(original,run/original.name);shutil.copyfile(__file__,run/Path(__file__).name)
(src/'SourceScreenPoint.java').write_text('package local.lito381.server;final class SourceScreenPoint {final int x,y;SourceScreenPoint(int x,int y){this.x=x;this.y=y;}int getX(){return x;}int getY(){return y;}\n'+fragment+'\n}\n','utf-8')
(src/'SourceScreenRangeOracle.java').write_text('''package local.lito381.server;
import java.util.*;import java.nio.file.*;
public final class SourceScreenRangeOracle {
 static int checks,cases;static void need(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[]a)throws Exception{
  var f=NativeSkillTargetMetadata.class.getDeclaredField("rows");f.setAccessible(true);Object previous=f.get(null);var map=new HashMap<Integer,NativeSkillTargetMetadata.Row>();for(int id:new int[]{29,44,76})map.put(id,new NativeSkillTargetMetadata.Row("buff",3,-1,id==76?3:0,false,32,0,0));f.set(null,Map.copyOf(map));
  try{for(int sx:new int[]{0,128,33089})for(int sy:new int[]{0,128,32717})for(int dx=-20;dx<=20;dx++)for(int dy=-20;dy<=20;dy++){
   boolean expected=new SourceScreenPoint(sx,sy).isInScreen(new SourceScreenPoint(sx+dx,sy+dy));need(BattleSpellCatalog.isInNativeScreen(sx,sy,sx+dx,sy+dy)==expected,"existing native screen matches verbatim Source");int range=expected?Math.max(Math.abs(dx),Math.abs(dy)):Integer.MAX_VALUE;
   for(int id:new int[]{29,44,76}){need(NativeSourceVisibilityRange.value(id,3)==19,"Source screen contains directional19-tile edges");need(NativeSourceVisibilityRange.distance(id,sx,sy,sx+dx,sy+dy)==range,"original coordinate range projection");need(NativeSourceVisibilityRange.signedDistance(id,-dx,-dy)==range,"signed coordinate differences preserve Source direction");cases++;}
  }need(cases==45387,"source geometry matrix");need(new SourceScreenPoint(0,0).isInScreen(new SourceScreenPoint(-19,0))&&!new SourceScreenPoint(0,0).isInScreen(new SourceScreenPoint(19,0)),"actual Source directional19 edge");}
  finally{f.set(null,previous);}Files.writeString(Path.of(a[0]),"{\\"cases\\":"+cases+",\\"checks\\":"+checks+",\\"Source_fragment_and_projection_only\\":true,\\"native_Hub_cases\\":0,\\"full_goal_achieved\\":false}\\n");
 }
}
''','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[run/'classes',build['runtime']['path'],S/'lib/*']));log=run/'verify.log';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');v=dict(status='running',runtime=build['runtime'],Source=dict(original=pin(original),frozen=pin(run/original.name)),Source_screen_and_tile_distance_verbatim_except_class_name=True,fixture_static_metadata_memory_only=True,created_Hub=False,created_SQL_connection=False,native_Hub_cases=0,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',*sorted(src.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.SourceScreenRangeOracle',run/'observations.json'])
 o=read(run/'observations.json');assert o['cases']==45387;v.update(status='passed-verbatim-Source-screen-direction-and-material-range-projection',observations=o,generated_sources=[pin(p)for p in sorted(src.glob('*.java'))]);assert sha(original)==v['Source']['original']['sha256']
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'source-screen-range-oracle-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
