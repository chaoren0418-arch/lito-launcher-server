from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
t=(R/'GroundLifecycleProbe.java').read_text('utf-8');main=t[t.index(' public static void main('):]
main=main.replace('for(boolean flag:new boolean[]{false,true})','for(boolean flag:new boolean[]{true})').replace('mode<6','mode<6')
body='''package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Configured zero/one/max MP through real ground packets; source tables untouched. */
public final class GroundNumericProbe extends GroundMaterialProbe {
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,boolean world,int mode)throws Exception{
  setup(pc,peer,hub,world);int skill=selectedSkill;var field=CombatDatabase.class.getDeclaredField("current");field.setAccessible(true);var original=(CombatDatabase.Snapshot)field.get(null);var skills=new HashMap<>(original.skills());var old=skills.get(skill);int base=new int[]{0,1,65535}[mode/2],initial=mode%2==0?1000:0;skills.put(skill,old.withBasics(base,old.hp(),old.reuse(),old.duration()));field.set(null,original.withSkills(skills));
  try{var row=BattleSpellCatalog.groundSpell(skill);need(row.mpCost()==base,"actual catalog consumes isolated configured MP");set(pc,"playerMp",initial);int cost=cost(pc,skill,row);boolean accept=initial>=cost;int item=Math.toIntExact(row.itemConsumeId()),count=row.itemConsumeCount();if(item!=0)NativeDeItemPublication.grant(pc,item,count+3);
   try(var wire=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
    cast(pc,skill,row);int expected=accept?Math.max(0,Math.min(1000,initial-cost)):initial;need((int)get(pc,"playerMp")==expected,"source signed consumption bounded by actual max MP");need((active(pc,hub,world)>0)==accept,"configured affordability gates actual ground effect");if(item!=0)need(pc.nativeDeInventory(0).countCatalog(item)==count+3-(accept?count:0),"numeric rejection does not consume material");need(((Map<?,?>)get(pc,"spellReadyNanos")).containsKey(skill)==accept,"numeric rejection does not commit reuse");collect(wire);collect(other);need(wire.locked+other.locked==0&&NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0,"numeric output unlocked and receipt released");
   }
  }finally{field.set(null,original);}cases++;
 }
'''
(R/'GroundNumericProbe.java').write_text(body+main,encoding='utf-8')
t=(R/'test_placement.py').read_text('utf-8').replace('placement','numeric').replace('GroundPlacementProbe','GroundNumericProbe').replace("row['cases']==32","row['cases']==48")
(R/'test_numeric.py').write_text(t,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(R/'numeric-adaptation.json').write_text(json.dumps(dict(inputs=[dict(path=str(p),sha256=sha(p))for p in[R/'GroundLifecycleProbe.java',R/'test_placement.py',Path(__file__)]],scope='legal configurable MP 0/1/65535, two current MP states, actual native58/63, WORLD/BATTLE, both item-app paths; isolated snapshot only'),indent=2),encoding='utf-8')
