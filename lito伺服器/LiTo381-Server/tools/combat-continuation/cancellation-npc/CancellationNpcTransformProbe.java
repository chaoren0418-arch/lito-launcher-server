package local.lito381.server;
import java.nio.file.*;import java.util.*;
/** Exercises Source transform sequencing through observable entity/publication ports. */
public final class CancellationNpcTransformProbe {
 static int cases,checks;
 static void need(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static final class Npc implements NativeCancellationNpcTransform.Target {
  int gfx=100,speed=640;String name="old";boolean deleted;
  final List<String> order=new ArrayList<>();
  public long getId(){return 123;}
  public int getTempCharGfx(){return gfx;}
  public int getGfxId(){return 100;}
  public int getX(){return 32770;}
  public int getY(){return 32771;}
  public int getMapId(){return 4;}
  public int getHeading(){return 6;}
  public void setTempCharGfx(int v){gfx=v;order.add("gfx:"+v);}
  public void setPassispeed(int v){speed=v;order.add("speed:"+v);}
  public void setNameId(String v){name=v;order.add("name:"+v);}
  public void broadcastPacketAll(NativeCancellationNpcTransform.Notice n){
   if(n instanceof NativeCancellationNpcTransform.Name v){need(v.objectId()==getId()&&v.name().equals(name),"name wire matches committed name");order.add("name-packet");}
   else if(n instanceof NativeCancellationNpcTransform.Shape v){need(v.objectId()==getId()&&v.gfx()==gfx,"shape wire matches committed gfx");order.add("shape-packet");}
  }
  public void deleteMe(){deleted=true;order.add("delete");}
 }
 public static void main(String[] args)throws Exception {
  Path jar=Path.of(args[1]).toRealPath();need(Path.of(NativeCancellationNpcTransform.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual candidate consumer");
  for(int mode=0;mode<8;mode++) {
   var n=new Npc();int target=mode==3?100:200;String name=mode==1?"new":"";int speed=mode==1?480:0;
   if(mode==2)n.gfx=999;if(mode==4)n.gfx=target;
   var rule=new NativeCancellationNpcRules.Rule(1,mode>=5?2:1,mode>=5?45641:target,mode==6?300:0,name,speed);
   int[] spawned={0};boolean spawnSucceeds=mode==6||mode==7;int expectedDuration=mode==6?300:0;boolean ok=NativeCancellationNpcTransform.apply(n,rule,(id,x,y,map,heading,duration)->{
    spawned[0]++;n.order.add("spawn");need(id==45641&&x==32770&&y==32771&&map==4&&heading==6,"Source spawn retains exact position/map/heading");
    need(duration==expectedDuration,"Source duration forwarded");need(!n.deleted,"old entity remains during spawn");return spawnSucceeds?new Npc():null;
   });
   if(mode==0)need(ok&&n.gfx==200&&n.speed==640&&n.name.equals("old")&&n.order.equals(List.of("gfx:200","shape-packet")),"type1 preserves unspecified name/speed");
   if(mode==1)need(ok&&n.gfx==200&&n.speed==480&&n.name.equals("new")&&n.order.equals(List.of("gfx:200","speed:480","name:new","name-packet","shape-packet")),"type1 exact Source order");
   if(mode>=2&&mode<=4)need(!ok&&n.order.isEmpty(),"already changed/source equals target is ordinary-cancel fallback");
   if(mode==5)need(!ok&&!n.deleted&&n.order.equals(List.of("spawn")),"spawn failure retains old NPC for ordinary cancellation");
   if(mode==6||mode==7)need(ok&&n.deleted&&n.order.equals(List.of("spawn","delete")),"delete only after successful spawn");
   need(spawned[0]==(mode>=5?1:0),"one spawn attempt only for type2");cases++;
  }
  var n=new Npc();boolean threw=false;
  try{NativeCancellationNpcTransform.apply(n,new NativeCancellationNpcRules.Rule(1,2,45641,0,"",0),(id,x,y,map,heading,duration)->{throw new IllegalStateException("fixture factory error");});}catch(IllegalStateException expected){threw=true;}
  need(threw&&!n.deleted&&n.order.isEmpty(),"factory exception cannot delete old entity");cases++;
  Files.writeString(Path.of(args[0]),"{\"cases\":"+cases+",\"checks\":"+checks+",\"runtime_scene_adapters_complete\":false,\"source_database_mutated\":false}\n");
 }
}
