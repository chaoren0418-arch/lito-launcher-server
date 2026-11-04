package local.lito381.server;
/** Scalar Source factory class/template proof belongs to each actual actor birth, never a registry. */
final class NativeSourceNpcFactoryType {
 static final String FIELD="nativeSourceNpcFactoryType";
 record Stamp(String database,int npc,String impl,int kind,int undead){}
 static int kind(String impl){if(impl==null)return 0;return switch(impl){case"L1Bow"->2;case"L1Cn"->2;case"L1Crown"->2;case"L1De"->2;case"L1Doll"->2;case"L1Door"->4;case"L1Dwarf"->2;case"L1Effect"->2;case"L1FieldObject"->2;case"L1Follower"->2;case"L1Furniture"->2;case"L1Gam"->2;case"L1Gambling"->2;case"L1Guard"->2;case"L1Guardian"->2;case"L1Housekeeper"->2;case"L1Illusory"->2;case"L1Lotto"->2;case"L1Merchant"->2;case"L1Monster"->1;case"L1MonsterGuard"->2;case"L1Npc"->2;case"L1NpcBox"->2;case"L1NpcTouch"->2;case"L1Pet"->5;case"L1Quest"->2;case"L1Request"->2;case"L1Scarecrow"->2;case"L1Signboard"->2;case"L1Summon"->6;case"L1Teleporter"->2;case"L1Tower"->3;default->0;};}
 static Stamp row(NativeDeNpcTemplateResources.Snapshot snapshot,int npc){if(snapshot==null||snapshot.lifecycleFailure()||snapshot.npcs()==null)return null;var row=snapshot.npcs().get(npc);if(row==null||row.get_npcId()!=npc)return null;String impl=row.getImpl();int kind=kind(impl);return kind==0?null:new Stamp(snapshot.database(),npc,impl,kind,row.get_undead());}
 static Stamp controlled(int npc,BattleCompanion.Kind kind){return kind==BattleCompanion.Kind.PET?new Stamp("",npc,"L1Pet",5,0):kind==BattleCompanion.Kind.SUMMON||kind==BattleCompanion.Kind.TAMED?new Stamp("",npc,"L1Summon",6,0):null;}
 static Object birth(Object actor,BattleCompanion.Snapshot transferred){
  int npc;if(actor instanceof BattleCompanion pet){npc=((BattleCompanion.Profile)CombatAccess.field(pet,"profile")).npcId();var owned=controlled(npc,(BattleCompanion.Kind)CombatAccess.field(pet,"kind"));if(owned!=null)return owned;}
  else if(actor.getClass().getName().equals("local.lito381.server.BattleQuestEncounter$Monster")){npc=(int)CombatAccess.call(CombatAccess.field(actor,"template"),"npcId","()I");}
  else if(actor.getClass().getName().equals("local.lito381.server.BattleTimeAttackEncounter$Monster"))npc=(int)CombatAccess.field(actor,"npcId");else return null;
  if(transferred!=null){var owned=controlled(npc,transferred.kind());if(owned!=null)return owned;}
  try{return row(NativeDeNpcTemplateResources.snapshot(),npc);}catch(IllegalStateException unavailable){return null;}
 }
 static Stamp stamp(Object actor){return actor==null||!(actor instanceof BattleCompanion)&&!actor.getClass().getName().equals("local.lito381.server.BattleQuestEncounter$Monster")&&!actor.getClass().getName().equals("local.lito381.server.BattleTimeAttackEncounter$Monster")?null:CombatAccess.field(actor,FIELD) instanceof Stamp stamp?stamp:null;}
 static int delta(Stamp stamp,int healing){int damage=-healing;if(stamp.kind()==1){if(stamp.undead()==1)damage*=-1;if(stamp.undead()==3)damage=0;}if((stamp.kind()==3||stamp.kind()==4)&&damage<0)damage=0;return -damage;}
 static int npcDelta(BattleSpellCatalog.HealingSpell spell,int undead,int healing,ClientSession caster,long id){int original=spell.npcHpDelta(undead,healing);var cast=NativeSourceHealingCast.current();var c=NativeAreaMaterialAction.current.get();if(cast==null||cast.closed||c==null||c.pc!=caster||!NativeSourcePlayerFactories.scoped(c))return original;var n=c.npcs.get(id);var type=n==null?null:stamp(n.actor());return type==null?original:delta(type,healing);}
 static int ownedDelta(int original,BattleCompanion actor,int healing){var c=NativeAreaMaterialAction.current.get();var cast=NativeSourceHealingCast.current();if(cast==null||cast.closed||!NativeSourcePlayerFactories.scoped(c))return original;var type=stamp(actor);return type==null?original:delta(type,healing);}
 private NativeSourceNpcFactoryType(){}
}
