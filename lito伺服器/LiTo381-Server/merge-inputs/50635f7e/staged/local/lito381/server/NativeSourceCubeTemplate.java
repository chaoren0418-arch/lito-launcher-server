package local.lito381.server;
import java.util.*;import java.util.concurrent.ThreadLocalRandom;import java.util.function.IntUnaryOperator;
/** Detached initial effect presentation from the existing static NPC snapshot; never opens SQL or creates an NPC actor. */
final class NativeSourceCubeTemplate {
 record Template(String database,int npc,int gfx,String name,int level,int randomLevel,int exp,int randomExp,int lawful,int randomLawful){}
 record Presentation(int npc,int gfx,int status,int exp,int lawful,String name){}
 static int npc(Object engine){return engine instanceof BattleCubeIgnitionEffects?80149:engine instanceof BattleCubeQuakeEffects?80150:engine instanceof BattleCubeShockEffects?80151:engine instanceof BattleCubeHarmonyEffects?80152:0;}
 static Object freeze(Object engine){int id=npc(engine);if(id==0)return null;try{return freeze(NativeDeNpcTemplateResources.snapshot(),id);}catch(IllegalStateException unavailable){return null;}}
 static Template freeze(NativeDeNpcTemplateResources.Snapshot snapshot,int id){
  if(snapshot==null||snapshot.lifecycleFailure()||snapshot.npcs()==null||id<80149||id>80152)return null;var row=snapshot.npcs().get(id);if(row==null||row.get_npcId()!=id||!"L1Effect".equals(row.getImpl()))return null;
  int level=row.get_level(),randomLevel=row.get_randomlevel(),randomExp=row.get_randomexp();
  if(randomExp<0||randomLevel!=0&&((long)randomLevel-level+1<=0||(long)randomLevel-level+1>Integer.MAX_VALUE))return null;
  return new Template(snapshot.database(),id,row.get_gfxid(),String.valueOf(row.get_nameid()),level,randomLevel,row.get_exp(),randomExp,row.get_lawful(),row.get_randomlawful());
 }
 static NativeSourceCubePlacement.Draft ready(NativeSourceCubePlacement.Draft draft){return draft!=null&&CombatAccess.field(draft,"nativeSourceCubeTemplate")instanceof Template?draft:null;}
 static int status(int gfx){return switch(gfx){case 51,147,110->24;case 57,816,3137,3140,3145,3148,3151,7621->20;case 111->4;default->0;};}
 static int gfx(int gfx,IntUnaryOperator random){int[] choices=switch(gfx){case 998,999,1002,1003->new int[]{998,999,1002,1003};case 1597,1600->new int[]{1597,1600};case 5942->new int[]{5942,5135,5137,5139,5141,5143,5145,5156,5158,5160,5162};case 1318,1321->new int[]{1318,1321};default->null;};return choices==null?gfx:choices[random.applyAsInt(choices.length)];}
 static Presentation instantiate(Template template,IntUnaryOperator random){
  double rate=0;if(template.randomLevel()!=0){int offset=random.applyAsInt(template.randomLevel()-template.level()+1);double diff=template.randomLevel()-template.level();rate=offset/diff;}
  int gfx=gfx(template.gfx(),random);int exp=template.exp()+(template.randomExp()==0?0:random.applyAsInt(template.randomExp()));
  int lawful=template.randomLawful()==0?template.lawful():(int)(template.lawful()+rate*(template.randomLawful()-template.lawful()));
  return new Presentation(template.npc(),gfx,status(gfx),exp,lawful,template.name());
 }
 static Object capture(Object cube){
  var e=NativeSourceCubeVisibility.entity(cube);if(e==null||e.scene()==null)return null;
  if(!(NativeSourceCubeOrigin.installing(e.engine())instanceof NativeSourceCubePlacement.Draft draft)||draft.engine()!=e.engine()||draft.epoch()!=e.epoch()||draft.scene()!=e.scene()||draft.creator()!=e.creator()||!(CombatAccess.field(draft,"nativeSourceCubeTemplate")instanceof Template template)||template.npc()!=e.npcId())throw new IllegalStateException("effect lost its original static template");
  return instantiate(template,bound->ThreadLocalRandom.current().nextInt(bound));
 }
 static byte[] packet(Object cube){var e=NativeSourceCubeVisibility.entity(cube);if(e==null||e.scene()==null)return null;var value=CombatAccess.field(cube,"nativeSourceCubePresentation");if(!(value instanceof Presentation presentation)||presentation.npc()!=e.npcId())throw new IllegalStateException("effect lost its original presentation");return packet(e.objectId(),(int)CombatAccess.call(cube,"x","()I"),(int)CombatAccess.call(cube,"y","()I"),presentation);}
 static byte[] packet(long object,int x,int y,Presentation p){
  // Source heading/move/chaLight/title start at zero/null. Template lightSize is a different field.
  // Keep the existing native client tail and receiver resource rewriting, including hidden HP.
  return new Protocol.Writer().c(Protocol.S_PUT_OBJECT).h(x).h(y).d(object).h(p.gfx()).c(p.status()).c(0).c(0).c(0).d(p.exp()).h(p.lawful()).s(p.name()).s(null).c(0).d(0).s("").s("").c(0).c(0xff).c(0).c(0).s("").h(0).c(0).bytes();
 }
 private NativeSourceCubeTemplate(){}
}
