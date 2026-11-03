from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;W=R.parent/'weakness-material'
t=(W/'WeaknessMaterialProbe.java').read_text('utf-8');main=t[t.index(' public static void main('):]
main=main.replace('mode<13','mode<9').replace('"weaknessFixturePercentileRoll",mode==1?100:1,','')
body='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Actual cccHH ground packets, placement gates and canonical material ownership. */
public class GroundMaterialProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={58,63};static int selectedSkill,selectedMode,targetX,targetY,gaps;
 static final List<String>goldens=new ArrayList<>();
 static Object spell(int skill){return BattleSpellCatalog.groundSpell(skill);}
 static void cast(ClientSession pc,int skill,Object row)throws Exception{call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).h(targetX).h(targetY).bytes());}
 static Object room(ClientSession pc,GameHub hub)throws Exception{return ((Map<?,?>)get(hub,"rooms")).get((long)get(pc,"currentRoomId"));}
 static int active(ClientSession pc,GameHub hub,boolean world)throws Exception{
  if(!world)return ((BattleGroundEffects)get(room(pc,hub),"groundEffects")).activeCount();
  int count=0;for(var effects:((Map<?,ClassicWorldGroundEffects>)get(hub,"classicWorldGroundEffects")).values())count+=effects.activeCount(System.nanoTime());return count;
 }
 static void setup(ClientSession pc,ClientSession peer,GameHub hub,boolean world)throws Exception{
  reset(pc,peer,hub,world);var c=((List<CharacterSummary>)get(pc,"characters")).get(0);set(pc,"characters",List.of(copy(c,Map.of("type",3,"originalIntelligence",10))));
  set(pc,"playerHp",4000);set(pc,"playerMaxHp",5000);set(pc,"playerMp",1000);set(pc,"playerMaxMp",1000);set(pc,"observer",false);set(pc,"learnedSkills",new HashSet<>(Set.of(2,58,63)));call(pc,"clearInvisibilityStateSilently",new Class[]{});
  if(!world){var arena=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));var point=arena.spawns().get(0);set(pc,"positionX",point.x());set(pc,"positionY",point.y());}
  int sx=(int)get(pc,"positionX"),sy=(int)get(pc,"positionY");targetX=sx+1;targetY=sy;
  var row=BattleSpellCatalog.groundSpell(selectedSkill);boolean found=false;
  for(int[]direction:List.of(new int[]{1,0},new int[]{0,1},new int[]{-1,0},new int[]{0,-1})){
   int x=sx+direction[0],y=sy+direction[1];boolean valid;
   if(world){var map=ClassicWorldMapCatalog.forMap((int)get(pc,"classicWorldMapId"));valid=map.standingPassable(x,y)&&new ClassicWorldGroundEffects().cast(row,pc.playerObjectId(),sx,sy,x,y,map,System.nanoTime()).accepted();}
   else{var map=ArenaMapCatalog.forMap(hub.battleMapId((long)get(pc,"currentRoomId"),pc));valid=map.nativeStandingFloor(x,y)&&new BattleGroundEffects().cast(row,pc.playerObjectId(),sx,sy,x,y,map,System.nanoTime()).accepted();}
   if(valid){targetX=x;targetY=y;found=true;break;}
  }
  need(found,"real native geometry offers valid ground position");set(peer,"positionX",sx+2);set(peer,"positionY",sy);
 }
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;var row=BattleSpellCatalog.groundSpell(skill);
  if(mode==2)set(pc,"playerMp",0);if(mode==4)targetX=(int)get(pc,"positionX")+40;if(mode==5)set(pc,"playerHp",0);if(mode==6)set(pc,"observer",true);
  if(mode==7){if(world)((Map<?,?>)get(hub,"classicWorldMembership")).remove(pc);else set(room(pc,hub),"battleStarted",false);}
  int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(item!=0&&mode!=1)NativeDeItemPublication.grant(pc,item,count+3);
  try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   if(mode==3){NativeCastMaterialProbe.cast(pc,2);long ready=((Map<Integer,Long>)get(pc,"spellReadyNanos")).get(2);while(System.nanoTime()-ready<=0)Thread.sleep(1);need(!pc.nativeCastTimelineState().timeline.canReserveSkillAction(),"real prior Light action window");}
   int mp=(int)get(pc,"playerMp"),hp=(int)get(pc,"playerHp"),bytes=wire.raw.size();boolean accepted=mode==0||mode==8||mode==1&&item==0||mode==3&&before;
   cast(pc,skill,row);
   need((int)get(pc,"playerMp")==Math.max(0,Math.min(1000,mp-(accepted?cost(pc,skill,row):0)))&&(int)get(pc,"playerHp")==hp,"ground Source resources mode="+mode+" world="+world+" skill="+skill);
   if(item!=0)need(pc.nativeDeInventory(0).countCatalog(item)==(mode==1?0:count+3-(accepted?count:0)),"ground material exactly once");
   need((active(pc,hub,world)>0)==accepted,"live ground lease only after accepted placement and material");
   need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accepted,"rejected ground placement has no reuse");
   if(mode==3){need((wire.raw.size()>bytes)==before,"ground shared Source action window");if(before)gaps++;}
   if(mode==8){int installed=active(pc,hub,world),spent=(int)get(pc,"playerMp"),sent=wire.raw.size();cast(pc,skill,row);need(active(pc,hub,world)==installed&&(int)get(pc,"playerMp")==spent&&wire.raw.size()==sent,"immediate replay cannot install or charge twice");}
   var self=collect(wire);var peers=collect(other);
   if(mode==1&&item!=0)need(self.equals(before?List.of():List.of(HexFormat.of().formatHex(Protocol.messageCode(299))))&&peers.isEmpty(),"missing material legacy silence becomes visible299");
   if(mode==7)need(self.isEmpty()&&peers.isEmpty(),"actual placement rejection has no cast packets");
   if(mode==0)goldens.add("skill="+skill+" world="+world+" mp="+get(pc,"playerMp")+" self="+self+" peer="+peers);
   need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"ground output unlocked and receipts drained");
  }cases++;
 }
'''
(R/'GroundMaterialProbe.java').write_text(body+main,encoding='utf-8')
t=(W/'test.py').read_text('utf-8').replace('WeaknessMaterialProbe','GroundMaterialProbe').replace("v[mode]['cases']==208","v[mode]['cases']==144")
(R/'test.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'test-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[W/'WeaknessMaterialProbe.java',W/'test.py',Path(__file__)]],scope='actual ground58/63 packets, resource/window/reuse, missing material, player admission and actual hub placement rejection; not pulse damage/healing or all terrain/mode coverage'),indent=2),encoding='utf-8')
