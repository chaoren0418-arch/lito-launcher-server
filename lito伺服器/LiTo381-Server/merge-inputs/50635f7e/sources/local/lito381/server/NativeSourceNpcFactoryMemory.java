package local.lito381.server;
/** Restore only a carried scalar factory proof; legacy snapshots never resolve a newer template. */
final class NativeSourceNpcFactoryMemory {
 static NativeSourceNpcFactoryType.Stamp proof(Object snapshot){Object value;int npc;
  if(snapshot instanceof BattleCompanion.Snapshot s){value=s.nativeSourceNpcFactoryMemory;npc=s.npcId();}
  else if(snapshot instanceof BattleQuestEncounter.Snapshot s){value=s.nativeSourceNpcFactoryMemory;npc=s.npcId();}
  else if(snapshot instanceof BattleTimeAttackEncounter.Snapshot s){value=s.nativeSourceNpcFactoryMemory;npc=s.npcId();}else return null;
  return value instanceof NativeSourceNpcFactoryType.Stamp stamp&&stamp.npc()==npc?stamp:null;
 }
 static int npc(Object actor){if(actor instanceof BattleCompanion pet)return((BattleCompanion.Profile)CombatAccess.field(pet,"profile")).npcId();if(actor.getClass().getName().equals("local.lito381.server.BattleQuestEncounter$Monster"))return(int)CombatAccess.call(CombatAccess.field(actor,"template"),"npcId","()I");return(int)CombatAccess.field(actor,"npcId");}
 static Object carry(Object actor){var stamp=NativeSourceNpcFactoryType.stamp(actor);if(stamp==null)return null;int npc=npc(actor);if(stamp.npc()==npc)return stamp;if(actor instanceof BattleCompanion pet){var controlled=NativeSourceNpcFactoryType.controlled(npc,(BattleCompanion.Kind)CombatAccess.field(pet,"kind"));if(controlled!=null&&controlled.kind()==stamp.kind())return controlled;}return null;}
 static Object restore(Object actor,BattleCompanion.Snapshot transferred,Object primary){var stamp=proof(primary);return stamp!=null&&stamp.npc()==npc(actor)?stamp:null;}
 private NativeSourceNpcFactoryMemory(){}
}
