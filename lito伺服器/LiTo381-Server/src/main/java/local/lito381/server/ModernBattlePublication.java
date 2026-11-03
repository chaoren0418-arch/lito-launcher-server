package local.lito381.server;

import static local.lito381.server.ModernMatchDescriptor.*;
import java.util.Map;
import java.util.Objects;

/** Fully checked service candidate in ENGINE revision space. Contains private
 * owner facts and unfiltered force/catalog state: NOT a wire response. Legacy
 * wire projections are separate and cannot grant advanced equipment SET. */
final class ModernBattlePublication {
    private final ModernBattleObservation.Capture capture;
    private final ModernBattleObservation.World world;
    private final ModernMatchDescriptor descriptor;
    private final Map<ModernRooms.Scope,ModernBattleObservation.OwnerView> owners;
    private final Map<ModernRooms.Scope,ModernBattleAudience.View> audiences;
    private final ModernNpcProtocol.Resources resources;
    private final ModernNpcNames.Table names;
    private final ModernBattleVipObservation.Publication vip;

    ModernBattlePublication(ModernBattleObservation.Capture capture,ModernMatchDescriptor descriptor,
            ModernNpcProtocol.Resources resources,ModernNpcNames.Table names) {
        this(capture,descriptor,resources,names,false,false);
    }
    /** Explicit candidate only; does not authorize any transport or service. */
    static ModernBattlePublication playerHealing(ModernBattleObservation.Capture capture,ModernMatchDescriptor descriptor,
            ModernNpcProtocol.Resources resources,ModernNpcNames.Table names) {
        return new ModernBattlePublication(capture,descriptor,resources,names,true,false);
    }
    /** Server-only candidate; healing may coexist, but no transport is enabled. */
    static ModernBattlePublication armorProtection(ModernBattleObservation.Capture capture,ModernMatchDescriptor descriptor,
            ModernNpcProtocol.Resources resources,ModernNpcNames.Table names) {
        return new ModernBattlePublication(capture,descriptor,resources,names,capture.features().playerHealing(),true);
    }
    /** Explicit full saved/runtime publication. Old audience/owner adapters stay
     * closed; common service metadata never converts through a legacy frame. */
    static ModernBattlePublication fullVip(ModernBattleObservation.Capture capture,ModernMatchDescriptor descriptor) {
        return new ModernBattlePublication(capture,descriptor,null,null,capture.features().playerHealing(),capture.features().armorProtection(),true);
    }
    private ModernBattlePublication(ModernBattleObservation.Capture capture,ModernMatchDescriptor descriptor,
            ModernNpcProtocol.Resources resources,ModernNpcNames.Table names,boolean healingCandidate,boolean protectionCandidate) {
        this(capture,descriptor,resources,names,healingCandidate,protectionCandidate,false);
    }
    private ModernBattlePublication(ModernBattleObservation.Capture capture,ModernMatchDescriptor descriptor,
            ModernNpcProtocol.Resources resources,ModernNpcNames.Table names,boolean healingCandidate,boolean protectionCandidate,boolean fullVip) {
        this.capture=Objects.requireNonNull(capture);this.descriptor=Objects.requireNonNull(descriptor);
        world=fullVip?capture.vipWorld():capture.world();descriptor.validateMode(world.room().summary().config().mode());
        var f=world.features();var m=world.modes();
        if(f.playerHealing()!=healingCandidate)throw new IllegalArgumentException("player healing publication requires an explicit candidate contract");
        if(f.armorProtection()!=protectionCandidate)throw new IllegalArgumentException("armor protection publication requires an explicit candidate contract");
        int features=(fullVip?FULL_VIP:0)|(f.combat()?COMBAT:0)|(f.spells()?SPELLS:0)|(f.arrow()?LIGHT_ARROW:0)|(f.items()?ITEMS:0)
                |(f.weight()?WEIGHT:0)|(f.timed()?TIMED:0)|(f.ammo()?AMMO:0)|(f.regen()?REGEN:0)
                |(f.fire()?FIRE_STORM:0)|(f.disintegrate()?DISINTEGRATE:0)|(f.npcArrow()?NPC_LIGHT_ARROW:0)
                |(f.npcFire()?NPC_FIRE_STORM:0)|(f.npcDisintegrate()?NPC_DISINTEGRATE:0)|(f.greaterHaste()?GREATER_HASTE:0)|(f.haste()?HASTE:0)|(f.slow()?SLOW:0)|(f.playerHealing()?PLAYER_HEALING:0)|(f.armorProtection()?ARMOR_PROTECTION:0)
                |(m.honor().enabled()?HONOR:0)|(m.race()!=null?PET_RACE:0)|(world.npcs()!=null?NPC:0)
                |(m.isolatedForces()?ISOLATED_FORCES:0)
                |(m.quest().progress()!=null&&m.quest().progress().plan().objectives()!=null?QUEST_OBJECTIVES:0)
                |(!m.quest().lives().isEmpty()?QUEST_LIVES:0)|(m.timeAttack().enabled()?TIME_ATTACK:0)|(m.siege().enabled()?SIEGE:0);
        if(features!=descriptor.features()||!capture.limits().equals(descriptor.engineLimits())||world.result()!=null&&!descriptor.settlementEnabled()
                ||descriptor.has(NPC)!=(resources!=null)||descriptor.has(NPC)!=(names!=null))throw bad();
        this.resources=resources;this.names=names;
        ModernBattleModeProof.publication(world,descriptor);
        ModernBattleRaceProof.publication(world,descriptor);
        if(names!=null) {
            if(!names.rows().stream().map(ModernNpcNames.Name::graphic).distinct().sorted().toList().equals(resources.graphics()))throw bad();
            for(var actor:world.npcs().roster().actors()) {
                var t=actor.template();var name=names.rows().stream().filter(n->n.sourceId()==t.catalogSourceId()&&n.npcId()==t.npcId()).findFirst().orElseThrow(ModernBattlePublication::bad);
                if(name.graphic()!=t.graphic())throw bad();
            }
        }
        if(fullVip) {
            vip=ModernBattleVipObservation.from(capture);owners=Map.of();audiences=Map.of();return;
        }
        vip=null;owners=capture.ownerViews(world);
        if(owners.size()!=world.actors().stream().filter(a->a.pose().member().identity().isHuman()).count())throw bad();
        audiences=ModernBattleAudience.project(world,descriptor,resources,names,owners,capture);
    }
    ModernBattleObservation.Capture capture(){return capture;}
    ModernBattleObservation.World world(){return world;}
    ModernMatchDescriptor descriptor(){return descriptor;}
    ModernNpcProtocol.Resources resources(){return resources;}
    ModernNpcNames.Table names(){return names;}
    ModernBattleVipObservation.View vipView(ModernRooms.Scope scope) {
        if(vip==null)throw new SecurityException("full VIP publication not present");return vip.forOwner(scope);
    }
    ModernBattleVipWire.Payload vip(ModernRooms.Scope scope) {return ModernBattleVipWire.fromPublication(this,scope);}
    /** Trusted lookup only. The service/transport must authenticate the current
     * connection owner and exact registration before selecting this scope. */
    ModernBattleAudience.View audience(ModernRooms.Scope scope) {
        var view=audiences.get(scope);if(view==null)throw new SecurityException("publication audience not present");return view;
    }
    ModernBattleObservation.OwnerView owner(ModernRooms.Scope scope) {
        var owner=owners.get(scope);if(owner==null)throw new SecurityException("publication owner not present");return owner;
    }
    /** Same authenticated owner lookup requirement as audience(). This is NOT
     * a LOAD acknowledgement or playable-scene admission. */
    ModernArmorPresentation.Candidate armorPresentation(ModernRooms.Scope scope) {
        var candidate=audience(scope).armorPresentation();
        if(candidate==null)throw new SecurityException("publication armor presentation not present");return candidate;
    }
    @Override public String toString(){return "BattlePublication[redacted]";}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid complete battle publication");}
}
