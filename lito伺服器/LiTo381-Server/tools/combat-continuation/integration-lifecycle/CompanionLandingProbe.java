package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Compare complete candidate traces and mutations with an extracted current Source method. */
public final class CompanionLandingProbe {
 static int cases,checks;static void need(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static final class Board implements NativeCompanionLanding.Terrain,SourceCompanionLandingReference.Terrain {
  final int seed,single;final boolean missing;final Set<Long> occupied=new HashSet<>();final List<Long> trace=new ArrayList<>();int x=99,y=98,commits,failAt;
  Board(int seed,int single,boolean missing){this.seed=seed;this.single=single;this.missing=missing;}
  public boolean isNull(){return missing;}
  public boolean acceptAndRelocate(int x,int y){long key=((long)x<<32)^(y&0xffffffffL);trace.add(key);if(failAt>0&&trace.size()==failAt)throw new IllegalStateException("fixture atomic landing commit rejection");boolean allowed=single>=0?x==single/11-5&&y==single%11-5:seed==-2||seed>=0&&((mix(seed,x,y)>>>1)%5!=0);if(!allowed||occupied.contains(key))return false;this.x=x;this.y=y;commits++;occupied.add(key);return true;}
 }
 static int mix(int seed,int x,int y){int v=seed*0x45d9f3b+x*0x119de1f3+y*0x3449;v^=v>>>16;v*=0x7feb352d;v^=v>>>15;return v;}
 static void compare(Board nativeMap,Board sourceMap){int beforeX=nativeMap==null?0:nativeMap.x,beforeY=nativeMap==null?0:nativeMap.y,commits=nativeMap==null?0:nativeMap.commits;var actual=NativeCompanionLanding.find(nativeMap,0,0);var expected=SourceCompanionLandingReference.find(sourceMap,0,0,5);need(actual==null?expected==null:expected!=null&&actual.x()==expected[0]&&actual.y()==expected[1],"same exact Source landing");if(nativeMap!=null){need(nativeMap.trace.equals(sourceMap.trace),"same center and complete ring traversal, including stopped commits");need(nativeMap.x==sourceMap.x&&nativeMap.y==sourceMap.y&&nativeMap.commits==sourceMap.commits&&nativeMap.occupied.equals(sourceMap.occupied),"same authoritative position and occupied cells");need(nativeMap.commits-commits==(actual==null?0:1),"commit at most once, and never for failed landing");if(actual==null)need(nativeMap.x==beforeX&&nativeMap.y==beforeY,"rejected landing retains original position");nativeMap.trace.clear();sourceMap.trace.clear();}cases++;}
 public static void main(String[]a)throws Exception{
  var jar=Path.of(a[0]).toRealPath();need(Path.of(NativeCompanionLanding.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual executable component from pinned JAR");need(Path.of(NativeCompanionLanding.Terrain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual production terrain interface");
  for(int seed=0;seed<8192;seed++)compare(new Board(seed,-1,false),new Board(seed,-1,false));for(int cell=0;cell<121;cell++)compare(new Board(-1,cell,false),new Board(-1,cell,false));compare(new Board(-1,-1,false),new Board(-1,-1,false));compare(new Board(-2,-1,false),new Board(-2,-1,false));compare(null,null);compare(new Board(-2,-1,true),new Board(-2,-1,true));
  for(int at:new int[]{1,2,3,9,25,49,81,121}){var n=new Board(-1,-1,false);var s=new Board(-1,-1,false);n.failAt=s.failAt=at;compare(n,s);}
  var n=new Board(-1,127,false);var s=new Board(-1,127,false);compare(n,s);need(n.commits==0,"radius five never accepts the next ring");
  for(int seed=0;seed<64;seed++){n=new Board(seed,-1,false);s=new Board(seed,-1,false);for(int actor=0;actor<128;actor++){n.x=s.x=99;n.y=s.y=98;compare(n,s);}need(n.commits<=121&&n.commits==n.occupied.size(),"sequential Source companion placements never overlap, including exhaustion");}
  need(cases==16518&&SourceCompanionLandingReference.failures==8,"all patterns and real Source catch branches observed");Files.writeString(Path.of(a[1]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"Source_commit_failures\":"+SourceCompanionLandingReference.failures+",\"production_dispatch_wired\":false,\"full_goal_achieved\":false}\n");
 }
}
