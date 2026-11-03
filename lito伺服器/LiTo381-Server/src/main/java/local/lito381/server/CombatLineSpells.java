package local.lito381.server;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Native line-target adapters. All references are scoped to the current invocation. */
final class CombatLineSpells {
    private static final String P="local/lito381/server/",T="L"+P+"ClientSession$MassSlowCompanionTarget;";
    record Primary(Object target,int x,int y) {}
    static boolean allKinds(){return CombatRules.loaded();}
    static boolean collectKind(Object primary){return allKinds()||primary!=null;}
    static boolean addPrimary(List<Object> targets,Object primary){return primary!=null&&targets.add(primary);}
    static long primaryId(Object primary){return primary==null?0:((Number)CombatAccess.get(primary,"objectId")).longValue();}
    static boolean admit(ClientSession caster,int x,int y,ClientSession primary) {
        if(!allKinds())return true;var hub=(GameHub)CombatAccess.field(caster,"hub");long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();int map=hub.battleMapId(room,caster),sx=CombatCompanionSpells.integer(caster,"positionX"),sy=CombatCompanionSpells.integer(caster,"positionY");
        return (primary==null||hub.battleMapId(room,primary)==map&&ForcedCombat.canAttackTarget(hub,room,caster,primary,primary.playerObjectId()))&&BattleSpellCatalog.isInNativeScreen(sx,sy,x,y)&&BattleSpellCatalog.hasNativeLineOfSight(ArenaMapCatalog.forMap(map),sx,sy,x,y);
    }
    static boolean contains(List<ArenaMapCatalog.Point> line,int x,int y,ClientSession caster,Object target) {
        return BattleStraightLine.contains(line,x,y)&&(!allKinds()||admit(caster,x,y,target instanceof ClientSession player?player:null));
    }
    static Primary primary(ClientSession caster,Protocol.AttackSpellRequest request) {
        if(!allKinds())return null;Object target=CombatAccess.call(caster,"resolveBattleSpellCompanion","(J)"+T,request.targetObjectId());if(target==null||!CombatCompanionSpells.available(caster,target))return null;
        var owner=(ClientSession)CombatAccess.get(target,"owner");var actor=(BattleCompanion.Snapshot)CombatAccess.get(target,"snapshot");var hub=(GameHub)CombatAccess.field(caster,"hub");long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();
        if(owner!=caster&&!hub.canAttack(room,caster,owner)||!admit(caster,actor.x(),actor.y(),null))return null;
        if(BattleSpellCatalog.freezeAttackSpell(request.skillId())!=null&&((BattleCompanion)CombatAccess.get(target,"controlled")).freezeActive(System.nanoTime()))return null;
        return new Primary(target,actor.x(),actor.y());
    }
    static CombatCompanionSpells.Batch capture(ClientSession caster,BattleSpellCatalog.DirectionalLineSpell spell,Primary primary,List<ArenaMapCatalog.Point> line) {
        var hits=new ArrayList<CombatCompanionSpells.Hit>();if(!allKinds())return new CombatCompanionSpells.Batch(hits);var hub=(GameHub)CombatAccess.field(caster,"hub");long room=((Number)CombatAccess.field(caster,"currentRoomId")).longValue();long now=System.nanoTime();var seen=new HashSet<Long>();
        var freeze=spell instanceof BattleSpellCatalog.FreezeAttackSpell f?f:null;
        for(var owner:hub.roomCombatants(room))for(var actor:owner.controlledCompanionSnapshots()) {
            boolean selected=primary!=null&&primaryId(CombatAccess.get(primary.target(),"snapshot"))==actor.objectId();
            if(owner==caster&&!selected||owner!=caster&&!ForcedCombat.canAttackTarget(hub,room,caster,owner,actor.objectId())||!actor.active()||actor.hp()<=0||!contains(line,actor.x(),actor.y(),caster,null)||!seen.add(actor.objectId()))continue;
            Object target=CombatAccess.call(caster,"resolveBattleSpellCompanion","(J)"+T,actor.objectId());if(target==null||!CombatCompanionSpells.available(caster,target))continue;var controlled=(BattleCompanion)CombatAccess.get(target,"controlled");var stats=(SourceNpcPhysicalStats)CombatAccess.get(target,"stats");if(stats==null||freeze!=null&&controlled.freezeActive(now))continue;
            CombatCompanionSpells.Attempt attempt;
            if(freeze!=null)attempt=CombatCompanionSpells.prepareFreeze(caster,controlled,stats,freeze,now);
            else {var lease=controlled.captureEraseMagic(now);attempt=new CombatCompanionSpells.Attempt(lease.present()?stats.magicResistance()>>2:stats.magicResistance(),0,lease);}
            hits.add(new CombatCompanionSpells.Hit(target,attempt,-1,-1));
        }
        return new CombatCompanionSpells.Batch(hits);
    }
    static CombatCompanionSpells.Batch draw(ClientSession caster,BattleSpellCatalog.DirectionalLineSpell spell,CombatCompanionSpells.Batch batch,long now,List<Protocol.AttackManyTarget> wire) {
        var hits=new ArrayList<CombatCompanionSpells.Hit>();if(batch==null)return new CombatCompanionSpells.Batch(hits);var freeze=spell instanceof BattleSpellCatalog.FreezeAttackSpell f?f:null;
        var config=(ServerConfig)CombatAccess.field(caster,"config");
        for(var prepared:batch.hits()) {
            if(!CombatCompanionSpells.available(caster,prepared.target()))continue;var controlled=(BattleCompanion)CombatAccess.get(prepared.target(),"controlled");if(freeze!=null&&controlled.freezeActive(now))continue;
            int source=CombatAccess.integerCall(caster,"rollLineSpellSourceDamage","(L"+P+"BattleSpellCatalog$DirectionalLineSpell;)I",spell);
            int damage=CombatCompanionDamage.ordinaryComplete(caster,spell.skillId(),spell.attribute(),source,0,prepared.attempt().magicResistance(),0,"line-companion="+controlled.snapshot().objectId(),controlled);if(controlled.earthBindActive(now)||controlled.freezeActive(now))damage=0;
            int roll=freeze==null?0:config.freezingBreathFixtureD100()>0?config.freezingBreathFixtureD100():ThreadLocalRandom.current().nextInt(1,101);
            hits.add(new CombatCompanionSpells.Hit(prepared.target(),prepared.attempt(),damage,roll));wire.add(new Protocol.AttackManyTarget(controlled.snapshot().objectId(),damage>0));
        }
        return new CombatCompanionSpells.Batch(hits);
    }
    static void commit(ClientSession caster,BattleSpellCatalog.DirectionalLineSpell spell,CombatCompanionSpells.Batch batch,long now,List<ClientSession> recipients) {
        CombatCompanionSpells.commitTargets(caster,spell.skillId(),spell instanceof BattleSpellCatalog.FreezeAttackSpell f?f:null,batch,now,recipients,"line-spell");
    }
}
