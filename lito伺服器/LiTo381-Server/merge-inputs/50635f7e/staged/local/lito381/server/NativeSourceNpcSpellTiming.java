package local.lito381.server;
import java.util.*;
/** Immutable original NPC/mobskill facts; skill target is read at the original post-cast scheduling phase. */
final class NativeSourceNpcSpellTiming {
 record Route(int type,int skill,int width,int height){}
 record Data(int attack,int support,Map<Integer,Route> routes){Data{routes=Map.copyOf(routes);}}
 static Object capture(NativeDeNpcTemplateResources.Snapshot snapshot,NativeDeNpcTemplate npc){
  var routes=new HashMap<Integer,Route>();var mob=snapshot.mobSkills()==null?null:snapshot.mobSkills().get(npc.get_npcId());
  if(mob!=null&&mob.get_mobid()==npc.get_npcId())for(int i=0;i<mob.getSkillSize();i++)routes.put(i,new Route(mob.getType(i),mob.getSkillId(i),mob.getAreaWidth(i),mob.getAreaHeight(i)));
  return new Data(npc.getAtkMagicSpeed(),npc.getSubMagicSpeed(),routes);
 }
 static int base(int original,Object actor,Object definition){
  var stamp=NativeSourceNpcFactoryType.stamp(actor);if(stamp==null||!(stamp.nativeSourceNpcSpellTiming instanceof Data data)||!(definition instanceof BattleTimeAttackMagicSkillCatalog.MobskillRow def)||def.npcId()!=stamp.npc())return NativeSourceNpcActionIntervals.base(original,actor);
  var route=data.routes().get(def.actNo());if(route==null)return NativeSourceNpcActionIntervals.base(original,actor);
  int value;
  if(definition instanceof BattleTimeAttackMagicSkillCatalog.SummonDefinition){if(route.type()!=NativeDeNpcMobSkill.TYPE_SUMMON)return NativeSourceNpcActionIntervals.base(original,actor);value=data.support();}
  else{
   int skill=definition instanceof BattleTimeAttackMagicSkillCatalog.Definition d?d.skillId():definition instanceof BattleTimeAttackMagicSkillCatalog.GroundDefinition d?d.skillId():definition instanceof BattleTimeAttackMagicSkillCatalog.VisualDefinition d?d.skillId():0;
   if(route.type()!=NativeDeNpcMobSkill.TYPE_MAGIC_ATTACK||skill<=0||route.skill()!=skill)return NativeSourceNpcActionIntervals.base(original,actor);
   if(route.width()>0||route.height()>0)value=data.support();
   else{NativeSkillTargetMetadata.Row row;try{row=NativeSkillTargetMetadata.row(skill);}catch(IllegalStateException unloaded){return NativeSourceNpcActionIntervals.base(original,actor);}if(row==null||row.target()==null)return NativeSourceNpcActionIntervals.base(original,actor);value=row.target().equals("attack")&&skill!=18?data.attack():data.support();}
  }
  return value<=0?960:value;
 }
 private NativeSourceNpcSpellTiming(){}
}
