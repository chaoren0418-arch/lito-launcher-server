package local.lito381.server;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Stack-local owned target view for the native WORLD physical pipeline. */
final class CombatWorldPhysical {
    private static final String P="local/lito381/server/";
    record Target(ClientSession owner,BattleCompanion controlled,BattleCompanion.Snapshot snapshot,
                  SourceNpcPhysicalStats stats,ClassicWorldMonsterPopulation.Snapshot view) {}
    static int integer(ClientSession actor,String field){return CombatCompanionSpells.integer(actor,field);}
    static boolean world(ClientSession actor){return ((Enum<?>)CombatAccess.field(actor,"phase")).name().equals("WORLD");}
    static boolean same(ClientSession caster,ClientSession owner){return world(caster)&&world(owner)&&(boolean)CombatAccess.call(caster,"sameClassicWorldMap","(L"+P+"ClientSession;)Z",owner);}
    static Target prepare(ClassicWorldMonsterPopulation.Snapshot ordinary,ClientSession caster,Protocol.AttackRequest request) {
        if(!CombatRules.loaded()||ordinary!=null)return null;
        var owners=(List<ClientSession>)CombatAccess.call(caster,"classicWorldMembers","(I)Ljava/util/List;",integer(caster,"classicWorldMapId"));
        for(var owner:owners) {
            if(!same(caster,owner))continue;
            var controlled=(BattleCompanion)CombatAccess.call(owner,"ownedCompanion","(J)L"+P+"BattleCompanion;",request.targetObjectId());if(controlled==null)continue;var actor=controlled.snapshot();
            if(!actor.active()||actor.hp()<=0||!actor.kind().classicWorldEligible())continue;
            var accounts=(AccountRepository)CombatAccess.call(caster,"selectedSourceAccounts","()L"+P+"AccountRepository;");SourceNpcPhysicalStats stats;
            try{stats=accounts.npcPhysicalStats(actor.npcId());}catch(Exception failure){throw new IllegalStateException("owned WORLD physical source NPC missing="+actor.npcId(),failure);}if(stats==null)throw new IllegalStateException("owned WORLD physical source NPC missing="+actor.npcId());
            var template=new ClassicWorldMonsterCatalog.MonsterTemplate(actor.npcId(),actor.name(),"L1Monster",actor.gfx(),actor.level(),actor.maxHp(),actor.maxMp(),actor.armorClass(),actor.strength(),0,actor.dexterity(),0,0,stats.magicResistance(),0,actor.lawful(),stats.large()?"large":"small",stats.weakAttribute(),0,0,0,0,stats.undead(),0,0,false,stats.damageReduction(),stats.hard(),false,stats.boss(),0,false);
            int map=integer(caster,"classicWorldMapId");var definition=new ClassicWorldMonsterCatalog.SpawnDefinition(map,actor.npcId(),"owned-physical-view",1,0,actor.x(),actor.y(),0,0,0,0,0,0,actor.heading(),0,0,false,0,false,0,template);
            var view=new ClassicWorldMonsterPopulation.Snapshot(map,actor.npcId(),1,actor.objectId(),actor.npcId(),actor.x(),actor.y(),actor.heading(),actor.hp(),actor.maxHp(),actor.mp(),actor.maxMp(),true,true,definition);
            return new Target(owner,controlled,actor,stats,view);
        }
        return null;
    }
    static ClassicWorldMonsterPopulation.Snapshot projection(ClassicWorldMonsterPopulation.Snapshot ordinary,Target target){return target==null?ordinary:target.view();}
    static boolean available(ClientSession caster,Target target) {
        if(target==null||!same(caster,target.owner()))return false;
        if(CombatAccess.call(target.owner(),"ownedCompanion","(J)L"+P+"BattleCompanion;",target.snapshot().objectId())!=target.controlled())return false;
        var now=target.controlled().snapshot();var old=target.snapshot();return now.active()&&now.hp()>0&&now.kind().classicWorldEligible()&&now.objectId()==old.objectId()&&now.x()==old.x()&&now.y()==old.y();
    }
    static boolean motion(ClientSession caster,Target target,SourceWeaponPhysicalStats weapon,boolean ranged,AtomicLong sequence) {
        if(target==null)return false;if(!available(caster,target))return true;var map=ClassicWorldMapCatalog.forMap(integer(caster,"classicWorldMapId"));var actor=target.snapshot();int x=integer(caster,"positionX"),y=integer(caster,"positionY");
        if(!map.isSafetyZone(x,y)&&!map.isSafetyZone(actor.x(),actor.y()))return false;
        CombatAccess.call(caster,"revealInvisibility","(Ljava/lang/String;)V","classic-world-owned-safe-zone-motion");CombatAccess.call(caster,"cancelMeditation","(Ljava/lang/String;)V","classic-world-owned-safe-zone-motion");
        CombatAccess.call(CombatAccess.field(caster,"naturalHpRegen"),"noteAttack","()V");CombatAccess.call(CombatAccess.field(caster,"naturalMpRegen"),"noteAttack","()V");
        int action=BattleActionCodes.attackForWalk(caster.battleWalkAction()),heading=CombatCompanionSpells.heading(x,y,actor.x(),actor.y());CombatAccess.set(caster,"playerHeading",heading);
        byte[] packet=ranged?Protocol.rangedPhysicalAttack(action,caster.playerObjectId(),0,0,heading,sequence.incrementAndGet(),weapon.projectile().effectGfx(),x,y,actor.x(),actor.y()):new Protocol.Writer().c(Protocol.S_ATTACK).c(action).d(caster.playerObjectId()).d(actor.objectId()).c(0).c(0).d(0).bytes();
        var members=(List<ClientSession>)CombatAccess.call(caster,"classicWorldMembers","(I)Ljava/util/List;",integer(caster,"classicWorldMapId"));for(var member:members)if(BattleSpellCatalog.isInNativeScreen(integer(member,"positionX"),integer(member,"positionY"),x,y)||BattleSpellCatalog.isInNativeScreen(integer(member,"positionX"),integer(member,"positionY"),actor.x(),actor.y()))CombatCompanionSpells.send(member,packet,"S_ATTACK owned-world safety-motion target="+actor.objectId());
        return true;
    }
    static boolean hit(boolean original,ClientSession caster,Target target,long now) {return target==null?original:original&&available(caster,target)&&!target.controlled().earthBindActive(now)&&!target.controlled().freezeActive(now);}
    static boolean altar(boolean original,Target target){return target!=null||original;}
    static boolean guardian(boolean original,Target target){return target==null&&original;}
    static CombatCompanionDamage.Physical attempt(BattlePhysicalDefense.NpcOutcome defense,int raw,int preCritical,BattleWeaponCritical.DamageOutcome critical,Target target) {return target==null?null:CombatCompanionDamage.physicalAttempt(defense,raw,preCritical,critical,target.controlled());}
    static int finish(int legacy,ClientSession caster,ClientSession player,SourceWeaponPhysicalStats weapon,BattleWeaponCritical.DamageOutcome critical,CombatKiringku.Defense defense,boolean hit,boolean armorBreak,CombatCompanionDamage.Physical attempt,Target target) {
        if(target==null)return CombatKiringku.finish(legacy,caster,player,weapon,critical,defense,hit,armorBreak);
        return available(caster,target)?CombatCompanionDamage.physical(legacy,caster,player,weapon,critical,defense,hit,armorBreak,attempt,target.controlled()):0;
    }
    static boolean commit(ClientSession caster,Target target,int damage) {
        if(target==null)return false;if(damage<=0||!available(caster,target))return true;
        target.controlled().selectMasterTarget(caster.playerObjectId());
        CombatAccess.call(target.owner(),"applyClassicWorldMonsterCompanionDamage","(L"+P+"BattleCompanion;L"+P+"BattleCompanion$Snapshot;IJLjava/lang/String;)V",target.controlled(),target.snapshot(),damage,caster.playerObjectId(),"player-physical-owned");
        return true;
    }
}
