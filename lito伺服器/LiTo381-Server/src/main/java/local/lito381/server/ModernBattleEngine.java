package local.lito381.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Bounded modern world-entry, movement and opt-in NEW_MATCH_MELEE_V1 authority.
 * A live room preparation capability and exact qualified roster are mandatory.
 * Geometry is from the explicitly selected shared resources, not native LTmap.
 * This never calls Rooms, JDBC, sockets, rendering or a client-provided clock.
 * The only nested lock order is Engine -> preparation Guard; Rooms cancellation
 * may take Rooms -> Guard but MUST NOT call back into this engine.
 */
final class ModernBattleEngine implements AutoCloseable {
    enum Phase { LOADING, COUNTDOWN, ACTIVE, ABORTED, FINISHED }
    enum AbortReason { NONE, DISCONNECTED, LOAD_TIMEOUT, ACTIVE_TIMEOUT, SERVER_ABORT, CLOCK_REGRESSED, PREPARATION_CANCELLED }
    enum MoveStatus { MOVED, NOT_ACTIVE, STALE_POSITION, TOO_FAST, BLOCKED, OCCUPIED, ABORTED }
    enum AttackStatus { ATTACKED, NOT_ACTIVE, DEAD, TARGET_DEAD, SAME_TEAM, OUT_OF_RANGE, BLOCKED, TOO_FAST, UNSUPPORTED, NUMERIC, TOO_HEAVY, STALE_EFFECT, NO_AMMO, STALE_REGEN }
    record Limits(int loadTimeoutMillis, long countdownNanos, int activeTimeoutMillis) {
        Limits {
            if (loadTimeoutMillis < 50 || loadTimeoutMillis > 30_000
                    || countdownNanos < 0 || countdownNanos > TimeUnit.SECONDS.toNanos(30)
                    || activeTimeoutMillis < 50 || activeTimeoutMillis > 1_800_000) throw invalid();
        }
    }
    /**
     * Fresh qualified row supplied by the bounded server preparation pipeline.
     * Interval is explicit server-owned policy, NOT an inferred native SPR speed.
     * No effective HP/AC, weapon damage or unsupported ability is fabricated here.
     */
    record PreparedMember(TournamentPresence.Owner owner, ModernRooms.Scope scope,
                          ModernApplicationProtocol.CharacterRow character,
                          ModernWorldMap.Cell spawn, long stepIntervalNanos,
                          ModernPhysicalCombat.Fighter fighter, long attackIntervalNanos,
                          ModernSkills.Snapshot skills, long castIntervalNanos, boolean damageMagic,
                          List<ModernShopAuthority.Stack> matchItems,ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled,ModernDisintegrateCatalog.Snapshot disintegrate,ModernDisintegrateCatalog.Snapshot npcDisintegrate,ModernGreaterHasteCatalog.Snapshot greaterHaste,ModernHasteCatalog.Snapshot haste,ModernSlowCatalog.Snapshot slow,boolean playerHealing,boolean vipEnabled) {
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,
                ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled,
                ModernDisintegrateCatalog.Snapshot disintegrate,ModernDisintegrateCatalog.Snapshot npcDisintegrate,
                ModernGreaterHasteCatalog.Snapshot greaterHaste,ModernHasteCatalog.Snapshot haste,ModernSlowCatalog.Snapshot slow,boolean playerHealing) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,
                    matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,haste,slow,playerHealing,false);
        }
        /** Explicit internal engine admission. Public projections still refuse
         * VIP until the saved/ability versions and visual contract are carried. */
        PreparedMember withVip(ModernPhysicalCombat.Fighter next) {
            if(next==null||next.vip()==null)throw invalid();
            var weight=weightBasis==null?null:weightBasis.requalify(next.profile());
            return new PreparedMember(owner,scope,character,spawn,stepIntervalNanos,next,attackIntervalNanos,skills,castIntervalNanos,damageMagic,
                    matchItems,weight,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,haste,slow,playerHealing,true);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,
                ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled,
                ModernDisintegrateCatalog.Snapshot disintegrate,ModernDisintegrateCatalog.Snapshot npcDisintegrate,
                ModernGreaterHasteCatalog.Snapshot greaterHaste,ModernHasteCatalog.Snapshot haste,ModernSlowCatalog.Snapshot slow) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,
                    matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,haste,slow,false);
        }
        PreparedMember withPlayerHealing() {
            return new PreparedMember(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,
                    damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,haste,slow,true,vipEnabled);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,
                ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled,
                ModernDisintegrateCatalog.Snapshot disintegrate,ModernDisintegrateCatalog.Snapshot npcDisintegrate,
                ModernGreaterHasteCatalog.Snapshot greaterHaste,ModernHasteCatalog.Snapshot haste) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,
                    matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,haste,null);
        }
        PreparedMember withSlow(ModernSlowCatalog.Snapshot catalog,List<ModernShopAuthority.Stack> stocks) {
            return new PreparedMember(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,
                    damageMagic,stocks,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,haste,catalog,playerHealing,vipEnabled);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,
                ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled,
                ModernDisintegrateCatalog.Snapshot disintegrate,ModernDisintegrateCatalog.Snapshot npcDisintegrate,ModernGreaterHasteCatalog.Snapshot greaterHaste) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,
                    matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,null);
        }
        PreparedMember withHaste(ModernHasteCatalog.Snapshot catalog) {
            return new PreparedMember(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,
                    damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,greaterHaste,catalog,slow,playerHealing,vipEnabled);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,
                ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled,
                ModernDisintegrateCatalog.Snapshot disintegrate,ModernDisintegrateCatalog.Snapshot npcDisintegrate) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,
                    matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,null);
        }
        PreparedMember withGreaterHaste(ModernGreaterHasteCatalog.Snapshot catalog) {
            return new PreparedMember(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,
                    damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,npcDisintegrate,catalog,haste,slow,playerHealing,vipEnabled);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,
                ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled,ModernDisintegrateCatalog.Snapshot disintegrate) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,null);
        }
        PreparedMember withNpcDisintegrate(ModernDisintegrateCatalog.Snapshot catalog) {
            return new PreparedMember(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,
                    damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrate,catalog,greaterHaste,haste,slow,playerHealing,vipEnabled);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,
                ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,boolean fireStormEnabled) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,null);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled){
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,regenEnabled,false);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,ModernInventoryWeight.Basis weightBasis,boolean timedEnabled,boolean bowEnabled) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,matchItems,weightBasis,timedEnabled,bowEnabled,false);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,ModernInventoryWeight.Basis weightBasis,boolean timedEnabled) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,matchItems,weightBasis,timedEnabled,false);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems,ModernInventoryWeight.Basis weightBasis) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,matchItems,weightBasis,false);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                ModernSkills.Snapshot skills,long castIntervalNanos,boolean damageMagic,List<ModernShopAuthority.Stack> matchItems) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,matchItems,null);
        }
        PreparedMember(TournamentPresence.Owner owner, ModernRooms.Scope scope,
                       ModernApplicationProtocol.CharacterRow character, ModernWorldMap.Cell spawn, long stepIntervalNanos,
                       ModernPhysicalCombat.Fighter fighter, long attackIntervalNanos,
                       ModernSkills.Snapshot skills, long castIntervalNanos, boolean damageMagic) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,damageMagic,null);
        }
        PreparedMember(TournamentPresence.Owner owner,ModernRooms.Scope scope,ModernApplicationProtocol.CharacterRow character,
                       ModernWorldMap.Cell spawn,long stepIntervalNanos,ModernPhysicalCombat.Fighter fighter,long attackIntervalNanos,
                       ModernSkills.Snapshot skills,long castIntervalNanos) {
            this(owner,scope,character,spawn,stepIntervalNanos,fighter,attackIntervalNanos,skills,castIntervalNanos,false);
        }
        PreparedMember(TournamentPresence.Owner owner, ModernRooms.Scope scope,
                       ModernApplicationProtocol.CharacterRow character, ModernWorldMap.Cell spawn, long stepIntervalNanos,
                       ModernPhysicalCombat.Fighter fighter, long attackIntervalNanos) {
            this(owner, scope, character, spawn, stepIntervalNanos, fighter, attackIntervalNanos, null, 0);
        }
        PreparedMember(TournamentPresence.Owner owner, ModernRooms.Scope scope,
                       ModernApplicationProtocol.CharacterRow character, ModernWorldMap.Cell spawn, long stepIntervalNanos) {
            this(owner, scope, character, spawn, stepIntervalNanos, null, 0);
        }
        PreparedMember {
            if(vipEnabled!=(fighter!=null&&fighter.vip()!=null))throw invalid();
            // VIP permits ordinary spells, items and proven weight leases.
            // Regen uses the separate query-clock-qualified expiry timeline.
            // PvP fire/disintegrate retain versioned costs and target receipts.
            // Qualified armor procs retain their independent clock/life lease
            // while VIP expiry/equipment changes rebind the selected rules.
            // Timed items and Haste43/54 retain independent speed leases and
            // historical costs across VIP requalification. Slow also preserves
            // its paid material stock, target grants and qualified immunity.
            // Ranged uses match-owned stocks and event-time shot provenance.
            // Player healing preserves separate incoming/outgoing histories;
            // expiry and durable equipment qualify each new healing price.
            // Remaining mode-specific histories still need integration.
            if(vipEnabled&&npcDisintegrate!=null)throw invalid();
            if(playerHealing) {
                if(skills==null||fighter==null||weightBasis==null)throw invalid();
                ModernPlayerHealing.qualify(fighter);
            }
            if (owner == null || scope == null || character == null || spawn == null
                    || stepIntervalNanos < TimeUnit.MILLISECONDS.toNanos(1)
                    || stepIntervalNanos > TimeUnit.SECONDS.toNanos(10)
                    || (fighter == null ? attackIntervalNanos != 0 : attackIntervalNanos < TimeUnit.MILLISECONDS.toNanos(1)
                        || attackIntervalNanos > TimeUnit.SECONDS.toNanos(10))) throw invalid();
            if (fighter != null && (!fighter.key().equals(new TournamentPresence.CharacterKey(scope.sourceId(), scope.objectId()))
                    || !fighter.profile().source().character().equals(character)
                    || fighter.profile().vitalPolicy() != ModernCombatProfile.VitalPolicy.FULL_NEW_MATCH)) throw invalid();
            if (skills == null ? castIntervalNanos != 0 : fighter == null || !skills.character().equals(character)
                    || skills.sourceId() != scope.sourceId() || castIntervalNanos < TimeUnit.MILLISECONDS.toNanos(10)
                    || castIntervalNanos > TimeUnit.SECONDS.toNanos(10) || castIntervalNanos % 1_000_000 != 0) throw invalid();
            if(damageMagic && (skills==null || fighter==null || fighter.magic()==null))throw invalid();
            if(timedEnabled && (weightBasis==null || stepIntervalNanos%1_000_000!=0 || attackIntervalNanos%1_000_000!=0
                    || castIntervalNanos%1_000_000!=0))throw invalid();
            if(bowEnabled&&(!timedEnabled||fighter==null||fighter.ranged()==null))throw invalid();
            if(regenEnabled&&(weightBasis==null||fighter==null||fighter.regen()==null||!fighter.regen().supported()))throw invalid();
            if(fireStormEnabled){if(!regenEnabled||!damageMagic||skills==null)throw invalid();ModernFireStorm.qualify(fighter);}
            if(disintegrate!=null) {
                if(!regenEnabled||!damageMagic||skills==null||!disintegrate.key().equals(fighter.key()))throw invalid();
                ModernDisintegrate.qualify(fighter);
            }
            if(npcDisintegrate!=null) {
                if(!regenEnabled||damageMagic||skills==null||fighter==null||disintegrate!=null||!npcDisintegrate.key().equals(fighter.key()))throw invalid();
                ModernDisintegrate.qualify(fighter);
            }
            if(greaterHaste!=null) {
                if(!timedEnabled||skills==null||fighter==null||!greaterHaste.key().equals(fighter.key()))throw invalid();
                ModernGreaterHaste.qualify(fighter);
                var learned=skills.skills().stream().filter(s->s.skillId()==ModernGreaterHaste.SKILL_ID).findFirst();
                if((greaterHaste.status()==ModernGreaterHasteCatalog.Status.UNLEARNED)!=learned.isEmpty())throw invalid();
                if(greaterHaste.status()==ModernGreaterHasteCatalog.Status.QUALIFIED
                        &&(learned.orElseThrow().mpCost()!=ModernGreaterHaste.BASE_MP
                            ||learned.orElseThrow().reuseMillis()!=ModernGreaterHaste.REUSE_MILLIS))throw invalid();
            }
            if(haste!=null) {
                if(!timedEnabled||skills==null||fighter==null||!haste.key().equals(fighter.key()))throw invalid();
                ModernHaste.qualify(fighter);
                var learned=skills.skills().stream().filter(s->s.skillId()==ModernHaste.SKILL_ID).findFirst();
                if((haste.status()==ModernHasteCatalog.Status.UNLEARNED)!=learned.isEmpty())throw invalid();
                if(haste.status()==ModernHasteCatalog.Status.QUALIFIED
                        &&(learned.orElseThrow().mpCost()!=ModernHaste.BASE_MP
                            ||learned.orElseThrow().reuseMillis()!=ModernHaste.REUSE_MILLIS))throw invalid();
            }
            if(slow!=null) {
                if(!timedEnabled||skills==null||fighter==null||!slow.key().equals(fighter.key())||slow.accessLevel().isEmpty())throw invalid();
                ModernSlow.qualify(fighter);ModernSlow.equipmentImmune(fighter);
                var learned=skills.skills().stream().filter(x->x.skillId()==ModernSlow.SKILL_ID).findFirst();
                if((slow.status()==ModernSlowCatalog.Status.UNLEARNED)!=learned.isEmpty())throw invalid();
                if(slow.status()==ModernSlowCatalog.Status.QUALIFIED&&(learned.orElseThrow().mpCost()!=ModernSlow.BASE_MP
                        ||learned.orElseThrow().reuseMillis()!=ModernSlow.REUSE_MILLIS))throw invalid();
            }
            if(weightBasis!=null) {
                if(fighter==null || matchItems==null)throw invalid();
                var proven=ModernInventoryWeight.qualify(fighter.profile(),weightBasis.sourceRate());
                if(!weightBasis.sameFacts(proven) || proven.validUntilMillis()>=0
                        && (weightBasis.validUntilMillis()<0 || weightBasis.validUntilMillis()>proven.validUntilMillis()))throw invalid();
                ModernInventoryWeight.fromShop(weightBasis,matchItems);
            }
            if(matchItems!=null) {
                matchItems=List.copyOf(matchItems);
                if(fighter==null || matchItems.size()>(bowEnabled?9:timedEnabled?ModernItemProtocol.MAX_TIMED_STOCKS:ModernItemProtocol.MAX_STOCKS)+(slow==null?0:ModernSpellMaterials.MAX_STOCKS))throw invalid();
                long previous=0;
                for(var stack:matchItems) {
                    var offer=stack.offer();var kind=ModernItemProtocol.HealingKind.find(offer.itemId());
                    if(offer.itemId()<=previous)throw invalid();
                    if(offer.ammo()!=null){if(!bowEnabled||offer.support()!=ModernShopCatalog.Support.AMMO)throw invalid();new ModernAmmo.Stock(offer.ammo(),stack.quantity(),stack.quantity());previous=offer.itemId();continue;}
                    if(offer.material()!=null){if(slow==null||offer.support()!=ModernShopCatalog.Support.MATERIAL)throw invalid();new ModernSpellMaterials.Stock(offer.material(),stack.quantity(),stack.quantity());previous=offer.itemId();continue;}
                    if(kind!=null) {
                        if(offer.support()!=ModernShopCatalog.Support.HEALING || offer.healing()==null || offer.healing().min()!=kind.minimum
                                || offer.healing().max()!=kind.maximum || offer.healing().effect()!=kind.effect || offer.healing().delayGroup()!=2
                                || offer.healing().delayMillis()!=kind.delayMillis)throw invalid();
                    } else if(!timedEnabled || offer.support()!=ModernShopCatalog.Support.TIMED || offer.timed()==null
                            || offer.timed().kind()!=ModernTimedEffects.Kind.find(offer.itemId()))throw invalid();
                    new ModernItemProtocol.Stock(offer.itemId(),stack.quantity(),stack.quantity());
                    previous=offer.itemId();
                }
            }
        }
    }
    /** No fake session owner, human scope, source row or learned skills for a server BOT. */
    record BotPrepared(ModernParticipantId identity, ModernBotTemplate template, ModernWorldMap.Cell spawn) {
        BotPrepared {
            if (identity == null || identity.isHuman() || template == null || spawn == null) throw invalid();
        }
    }
    record Actor(long entityId, ModernRooms.Member member, ModernWorldMap.Cell position,
                 int heading, boolean loaded, long lastMoveSequence, long stepIntervalNanos) {
        Actor {
            if (entityId < 1 || member == null || position == null || heading < 0 || heading > 7
                    || lastMoveSequence < 0 || stepIntervalNanos < TimeUnit.MILLISECONDS.toNanos(1)
                    || stepIntervalNanos > TimeUnit.SECONDS.toNanos(10)) throw invalid();
        }
    }
    record Snapshot(long matchId, long generation, long revision, Phase phase, AbortReason abortReason,
                    ModernRooms.RoomSnapshot room, ModernWorldMap.Manifest manifest, List<Actor> actors,
                    List<ModernCombatProtocol.Vital> vitals, ModernMatchSettlement.Event result,
                    List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,
                    List<ModernItemProtocol.State> items,List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces,ModernQuestObjectives.Snapshot quest,List<ModernNpcMagic.CastState> npcSpells,ModernTimeAttack.Snapshot timeAttack,ModernSiege.Snapshot siege,List<ModernDisintegrateState.State> disintegrations,List<ModernNpcDisintegrateState.State> npcDisintegrations,List<ModernNpcFireStormState.State> npcFireStorms) {
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,
                ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces,
                ModernQuestObjectives.Snapshot quest,List<ModernNpcMagic.CastState> npcSpells,ModernTimeAttack.Snapshot timeAttack,
                ModernSiege.Snapshot siege,List<ModernDisintegrateState.State> disintegrations,List<ModernNpcDisintegrateState.State> npcDisintegrations) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,isolatedForces,quest,npcSpells,timeAttack,siege,disintegrations,npcDisintegrations,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,
                ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces,
                ModernQuestObjectives.Snapshot quest,List<ModernNpcMagic.CastState> npcSpells,ModernTimeAttack.Snapshot timeAttack,
                ModernSiege.Snapshot siege,List<ModernDisintegrateState.State> disintegrations) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,isolatedForces,quest,npcSpells,timeAttack,siege,disintegrations,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,
                ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces,
                ModernQuestObjectives.Snapshot quest,List<ModernNpcMagic.CastState> npcSpells,ModernTimeAttack.Snapshot timeAttack,ModernSiege.Snapshot siege) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,isolatedForces,quest,npcSpells,timeAttack,siege,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces,ModernQuestObjectives.Snapshot quest,List<ModernNpcMagic.CastState> npcSpells,ModernTimeAttack.Snapshot timeAttack){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,isolatedForces,quest,npcSpells,timeAttack,ModernSiege.Snapshot.disabled());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces,ModernQuestObjectives.Snapshot quest,List<ModernNpcMagic.CastState> npcSpells){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,isolatedForces,quest,npcSpells,ModernTimeAttack.Snapshot.disabled());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces,ModernQuestObjectives.Snapshot quest){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,isolatedForces,quest,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race,boolean isolatedForces){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,isolatedForces,ModernQuestObjectives.Snapshot.disabled());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms,ModernPetRaceProtocol.Snapshot race){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,race,false);
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor,List<ModernFireStormProtocol.State> fireStorms){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,fireStorms,ModernPetRaceProtocol.Snapshot.disabled());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen,ModernHonor.Snapshot honor){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,honor,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo,List<ModernRegenProtocol.State> regen){
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,regen,ModernHonor.Snapshot.disabled());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs,List<ModernAmmo.State> ammo) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,ammo,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,ModernMatchSettlement.Event result,
                List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,List<ModernItemProtocol.State> items,
                List<ModernWeightProtocol.State> weights,List<ModernTimedEffects.State> buffs) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,buffs,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,
                ModernMatchSettlement.Event result,List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,
                List<ModernItemProtocol.State> items,List<ModernWeightProtocol.State> weights) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,weights,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,
                ModernMatchSettlement.Event result,List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages,
                List<ModernItemProtocol.State> items) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,items,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                 ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,
                 ModernMatchSettlement.Event result,List<ModernSpellProtocol.CastState> casts,List<ModernDamageSpellProtocol.State> damages) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,damages,List.of());
        }
        Snapshot(long matchId,long generation,long revision,Phase phase,AbortReason abortReason,ModernRooms.RoomSnapshot room,
                 ModernWorldMap.Manifest manifest,List<Actor> actors,List<ModernCombatProtocol.Vital> vitals,
                 ModernMatchSettlement.Event result,List<ModernSpellProtocol.CastState> casts) {
            this(matchId,generation,revision,phase,abortReason,room,manifest,actors,vitals,result,casts,List.of());
        }
        Snapshot(long matchId, long generation, long revision, Phase phase, AbortReason abortReason,
                 ModernRooms.RoomSnapshot room, ModernWorldMap.Manifest manifest, List<Actor> actors,
                 List<ModernCombatProtocol.Vital> vitals, ModernMatchSettlement.Event result) {
            this(matchId, generation, revision, phase, abortReason, room, manifest, actors, vitals, result, List.of());
        }
        Snapshot(long matchId, long generation, long revision, Phase phase, AbortReason abortReason,
                 ModernRooms.RoomSnapshot room, ModernWorldMap.Manifest manifest, List<Actor> actors,
                 List<ModernCombatProtocol.Vital> vitals) {
            this(matchId, generation, revision, phase, abortReason, room, manifest, actors, vitals, null);
        }
        Snapshot(long matchId, long generation, long revision, Phase phase, AbortReason abortReason,
                 ModernRooms.RoomSnapshot room, ModernWorldMap.Manifest manifest, List<Actor> actors) {
            this(matchId, generation, revision, phase, abortReason, room, manifest, actors, List.of());
        }
        Snapshot {
            Objects.requireNonNull(siege);
            Objects.requireNonNull(timeAttack);
            Objects.requireNonNull(quest);if(quest.enabled()&&(!isolatedForces||honor.enabled()||race.enabled()))throw invalid();
            if(isolatedForces&&(room==null||room.summary().config().mode()!=2))throw invalid();
            if (matchId < 1 || generation < 1 || revision < 1 || phase == null || abortReason == null
                    || room == null || manifest == null || actors == null || actors.isEmpty() || actors.size() > 100
                    || actors.size() != room.members().size()
                    || (phase == Phase.ABORTED) != (abortReason != AbortReason.NONE)
                    || (phase == Phase.FINISHED) != (result != null||race.complete()||timeAttack.finished()||siege.finished())) throw invalid();
            Objects.requireNonNull(race);
            if(race.enabled()&&(race.runners().size()!=actors.size()||result!=null||!vitals.isEmpty()||!casts.isEmpty()||honor.enabled()))throw invalid();
            actors = List.copyOf(actors); vitals = List.copyOf(vitals); casts = List.copyOf(casts);damages=List.copyOf(damages);items=List.copyOf(items);
            weights=List.copyOf(weights);buffs=List.copyOf(buffs);ammo=List.copyOf(ammo);regen=List.copyOf(regen);fireStorms=List.copyOf(fireStorms);
            npcSpells=List.copyOf(npcSpells);
            disintegrations=List.copyOf(disintegrations);
            npcDisintegrations=List.copyOf(npcDisintegrations);
            npcFireStorms=List.copyOf(npcFireStorms);
            if(!npcFireStorms.isEmpty()&&(!disintegrations.isEmpty()||!damages.isEmpty()||!buffs.isEmpty()||!ammo.isEmpty()||!fireStorms.isEmpty()||honor.enabled()||race.enabled()||siege.enabled()))throw invalid();
            ModernNpcFireStormState.validate(matchId,generation,revision,phase,room,manifest,actors,vitals,casts,items,regen,quest,timeAttack,npcSpells,npcDisintegrations,npcFireStorms);
            if(!npcDisintegrations.isEmpty()&&(!disintegrations.isEmpty()||!damages.isEmpty()||!buffs.isEmpty()||!ammo.isEmpty()
                    ||!fireStorms.isEmpty()||honor.enabled()||race.enabled()||siege.enabled()))throw invalid();
            ModernNpcDisintegrateState.validate(matchId,generation,revision,phase,room,manifest,actors,vitals,casts,items,regen,quest,timeAttack,npcSpells,npcDisintegrations);
            if(!disintegrations.isEmpty()&&(!npcSpells.isEmpty()||siege.enabled()||quest.enabled()||timeAttack.enabled()||race.enabled()))throw invalid();
            ModernDisintegrateState.validate(revision,phase,room,manifest,actors,vitals,casts,damages,items,regen,honor,fireStorms,disintegrations);
            if(!npcSpells.isEmpty()) {
                if((timeAttack.enabled()?room.summary().config().mode()!=4:room.summary().config().mode()!=2)||npcSpells.size()!=actors.size()||casts.size()!=actors.size()
                        ||vitals.size()!=actors.size()||!damages.isEmpty()||!buffs.isEmpty()||!ammo.isEmpty()
                        ||!fireStorms.isEmpty()||honor.enabled()||race.enabled())throw invalid();
                for(int i=0;i<npcSpells.size();i++) {
                    var s=npcSpells.get(i);var actor=actors.get(i);var cast=casts.get(i);
                    if(s.entityId()!=i+1||s.revision()>revision||s.requestSequence()>cast.requestSequence()
                            ||Math.addExact(s.castCount(),cast.castCount())>cast.requestSequence()
                            ||(actor.member().identity().isHuman()?s.mpCost()<1:!s.equals(ModernNpcMagic.CastState.initial(i+1,0))))throw invalid();
                    if(s.castCount()>0) {
                        var scope=s.target().scope();
                        var life=quest.lives().stream().filter(l->l.entityId()==actor.entityId()).findFirst().orElse(null);
                        if(scope.roomId()!=room.summary().id()||scope.matchId()!=matchId||scope.generation()!=generation
                                ||s.targetLane()!=actor.member().team()||!ModernWorldProtocol.contains(manifest,s.from())
                                ||!ModernWorldProtocol.contains(manifest,s.to())
                                ||(life==null?s.casterLife()!=1:s.casterLife()>life.incarnation()
                                ||(s.casterLife()==life.incarnation()?s.revision()<=life.respawnRevision():s.revision()>life.respawnRevision())))throw invalid();
                    }
                }
            }
            if(!fireStorms.isEmpty()&&(fireStorms.size()!=actors.size()||regen.size()!=actors.size()))throw invalid();
            Objects.requireNonNull(honor);
            if(!regen.isEmpty()) {
                if(regen.size()!=actors.size()||weights.size()!=actors.size())throw invalid();
                for(int i=0;i<regen.size();i++) {
                    var s=regen.get(i);if(s.entityId()!=i+1||s.hpRevision()>revision||s.mpRevision()>revision)throw invalid();
                    ModernRegenProtocol.validateTotals(s,vitals.get(i),casts.isEmpty()?0:casts.get(i).mpSpent(),damages.isEmpty()?0:damages.get(i).mpSpent(),honor.enabled()?honor.lives().get(i).respawnHpGain():siege.enabled()?siege.lives().get(i).hpGain():quest.lives().isEmpty()?0:quest.lives().get(i).hpGain(),honor.enabled()?honor.heals().get(i).mpSpent():0,fireStorms.isEmpty()?0:fireStorms.get(i).mpSpent(),npcSpells.isEmpty()?0:npcSpells.get(i).mpSpent(),disintegrations.isEmpty()?0:disintegrations.get(i).mpSpent(),npcDisintegrations.isEmpty()?0:npcDisintegrations.get(i).mpSpent(),npcFireStorms.isEmpty()?0:npcFireStorms.get(i).mpSpent());
                    if(!actors.get(i).member().identity().isHuman()&&!s.equals(ModernRegenProtocol.State.initial(s.entityId()).damaged(s.damageLoss())))throw invalid();
                }
            }
            if(!ammo.isEmpty()) {
                if(ammo.size()!=actors.size()||buffs.size()!=actors.size())throw invalid();
                for(int i=0;i<ammo.size();i++){var a=ammo.get(i);var v=vitals.get(i);
                    if(a.entityId()!=i+1||a.revision()>revision||a.shotRevision()!=0&&a.shotRevision()!=v.attackRevision()
                            ||!actors.get(i).member().identity().isHuman()&&(!a.stocks().isEmpty()||a.requestSequence()!=0))throw invalid();}
            }
            if(!buffs.isEmpty()) {
                if(buffs.size()!=actors.size() || weights.size()!=actors.size() || vitals.size()!=actors.size())throw invalid();
                for(int i=0;i<buffs.size();i++) {
                    var buff=buffs.get(i);var actor=actors.get(i);
                    if(buff.entityId()!=actor.entityId() || buff.revision()>revision
                            || !actor.member().identity().isHuman()&&!buff.equals(ModernTimedEffects.initial(actor.entityId()))
                            || (phase!=Phase.ACTIVE || vitals.get(i).hp()==0)&&buff.hasEffects()
                            || (phase==Phase.LOADING || phase==Phase.COUNTDOWN)&&buff.revision()!=0)throw invalid();
                }
            }
            if(!weights.isEmpty()) {
                if(weights.size()!=actors.size() || items.size()!=actors.size())throw invalid();
                for(int i=0;i<weights.size();i++) {
                    var w=weights.get(i);var a=actors.get(i);
                    if(w.entityId()!=a.entityId() || w.snapshot().view().applicable()!=a.member().identity().isHuman())throw invalid();
                    ModernWeightProtocol.validateStocks(w.snapshot(),items.get(i).stocks(),ammo.isEmpty()?List.of():ammo.get(i).stocks());
                }
            }
            if(!damages.isEmpty() && (damages.size()!=actors.size() || casts.size()!=actors.size()))throw invalid();
            if (!casts.isEmpty() && (casts.size() != actors.size() || vitals.size() != actors.size())) throw invalid();
            if (!vitals.isEmpty() && vitals.size() != actors.size()) throw invalid();
            if(!items.isEmpty()) {
                if(items.size()!=actors.size() || vitals.size()!=actors.size())throw invalid();
                for(int i=0;i<items.size();i++) {
                    var item=items.get(i);var actor=actors.get(i);var vital=vitals.get(i);
                    if(item.entityId()!=actor.entityId() || item.useRevision()>revision || item.hpAfter()>vital.maxHp()
                            || !actor.member().identity().isHuman() && (!item.stocks().isEmpty() || item.requestSequence()!=0 || item.fixedRecovery()!=0)
                            || item.useCount()>0 && (!ModernWorldProtocol.contains(manifest,new ModernWorldMap.Cell(item.x(),item.y()))
                            || item.useRevision()==revision && vital.hp()!=item.hpAfter()))throw invalid();
                }
            }
            long previous = 0; var keys = new HashSet<ModernParticipantId>();
            var cells = new HashSet<ModernQuestSpace.Cell>();
            for (Actor actor : actors) {
                if (actor.entityId() != previous + 1 || !keys.add(actor.member().identity())
                        ||(!race.enabled()||race.runners().get((int)previous).competing())
                            &&(!timeAttack.enabled()||!timeAttack.state().departed().contains(actor.entityId()))
                            &&!cells.add(ModernQuestSpace.cell(isolatedForces,actor.member().team(),actor.position()))) throw invalid();
                if (!actor.member().equals(room.members().get((int) previous))) throw invalid();
                previous = actor.entityId();
                if ((phase == Phase.COUNTDOWN || phase == Phase.ACTIVE || phase == Phase.FINISHED) && !actor.loaded()) throw invalid();
            }
            for (int index = 0; index < vitals.size(); index++) {
                var vital = vitals.get(index);
                if (vital.entityId() != actors.get(index).entityId() || vital.attackRevision() > revision
                        || vital.hurtRevision() > revision || vital.deathRevision() > revision) throw invalid();
            }
            for (int i = 0; i < casts.size(); i++) {
                var c = casts.get(i); var v = vitals.get(i);
                if (c.entityId() != i + 1 || c.castRevision() > revision)throw invalid();
                if(regen.isEmpty()) {long spent=Math.addExact(Math.addExact(c.mpSpent(),damages.isEmpty()?0:damages.get(i).mpSpent()),npcSpells.isEmpty()?0:npcSpells.get(i).mpSpent());
                    if(spent>v.maxMp()||v.mp()!=v.maxMp()-spent)throw invalid();}
                if(!damages.isEmpty()) {
                    var d=damages.get(i);
                    if(d.entityId()!=i+1 || d.castRevision()>revision || d.targetEntityId()>actors.size()+(honor.enabled()?2:0)
                            || Math.addExact(c.castCount(),d.castCount())>c.requestSequence())throw invalid();
                }
            }
            if (result != null) {
                if (result.matchId() != matchId || result.generation() != generation || result.engineRevision() != revision
                        || result.mode() != room.summary().config().mode() || result.mapId() != room.summary().config().mapId()
                        || result.participants().size() != actors.size() || vitals.isEmpty()) throw invalid();
                for (int index = 0; index < actors.size(); index++) {
                    var participant = result.participants().get(index); var actor = actors.get(index);
                    if (participant.entityId() != actor.entityId() || !participant.identity().equals(actor.member().identity())
                            || participant.team() != actor.member().team()
                            || result.mode()==1&&participant.verdict() == ModernMatchSettlement.Verdict.LOSS && vitals.get(index).hp() != 0) throw invalid();
                }
                if (result.mode()==1&&vitals.stream().noneMatch(v -> v.hp() > 0)) throw invalid();
                boolean finalDeath = false;
                for (int index = 0; index < vitals.size(); index++) {
                    if (result.participants().get(index).verdict() == ModernMatchSettlement.Verdict.LOSS
                            && vitals.get(index).deathRevision() == revision) finalDeath = true;
                }
                if (result.mode()==1&&!finalDeath) throw invalid();
                if(honor.enabled()) {
                    var dead=honor.objectives().stream().filter(o->o.hp()==0).toList();
                    if(result.mode()!=5||dead.size()!=1||dead.get(0).deathRevision()!=revision)throw invalid();
                    for(var p:result.participants())if((p.verdict()==ModernMatchSettlement.Verdict.LOSS)!=(p.team()==dead.get(0).team()))throw invalid();
                }
            }
            quest.validate(room.summary().id(),matchId,generation,revision,phase,actors,vitals,result,npcFireStorms);
            for(int i=0;i<quest.lives().size();i++)quest.lives().get(i).validate(revision,manifest,actors.get(i),vitals.get(i));
        }
    }
    record MoveResult(MoveStatus status, ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        MoveResult { if (status == null || observation == null || (status == MoveStatus.ABORTED) != (observation.phase() == Phase.ABORTED)) throw invalid(); }
        Snapshot snapshot(){return ModernLegacyBattleProjection.from(observation);}
    }
    record AttackResult(AttackStatus status, ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        AttackResult { Objects.requireNonNull(status); Objects.requireNonNull(observation); }
        Snapshot snapshot(){return ModernLegacyBattleProjection.from(observation);}
    }
    record CastResult(ModernSpellProtocol.Reason reason, ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        CastResult { Objects.requireNonNull(reason); Objects.requireNonNull(observation); }
        Snapshot snapshot(){return ModernLegacyBattleProjection.from(observation);}
    }
    record DisintegrateResult(ModernDisintegrateState.Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        DisintegrateResult {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
        Snapshot snapshot(){return ModernLegacyBattleProjection.from(observation);}
    }
    record GreaterHasteResult(ModernGreaterHasteCast.Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {}
    record SlowResult(ModernSlowCast.Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        SlowResult {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
    }
    record HasteResult(ModernHasteCast.Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        HasteResult {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
    }
    record PlayerHealingResult(ModernPlayerHealingCast.Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        PlayerHealingResult {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
    }
    record ItemResult(ModernItemProtocol.Reason reason, ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        ItemResult { Objects.requireNonNull(reason); Objects.requireNonNull(observation); }
        Snapshot snapshot(){return ModernLegacyBattleProjection.from(observation);}
    }
    record AmmoResult(ModernAmmoProtocol.Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        AmmoResult{Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
        Snapshot snapshot(){return ModernLegacyBattleProjection.from(observation);}
    }
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    private static final class MutableActor {
        final long entityId; final PreparedMember prepared; final ModernRooms.Member member;
        ModernBattleLoadout loadout;
        EquipmentChange equipmentChange;
        ModernVitalBalance.State equipmentResources;
        final long stepIntervalNanos, attackIntervalNanos, castIntervalNanos;
        ModernWorldMap.Cell position; int heading; boolean loaded; long sequence;
        boolean moved; long lastMoveNanos;
        long hp, mp, attackSequence, attackRevision, hurtRevision, deathRevision, targetEntityId, lastAttackNanos;
        boolean attacked;
        ModernItemRuntime.State itemUse;
        Map<ModernUseHistory.Lane,ModernUseHistory.Spell> spellHistory=Map.of();
        Map<ModernUseHistory.Lane,ModernTargetHistory.History> targetHistory=Map.of();
        ModernUseHistory.Items itemHistory;
        ModernSpellRuntime.State spellRuntime;
        ModernPlayerHealingHistory.Entry playerHealing;
        ModernInventoryWeight.Snapshot weight;
        ModernTimedEffects.State buffs;
        ArmorProtectionView armor;
        ModernAmmo.State ammo;
        ModernSpellMaterials.State materials;
        ModernBattlePresentation.State presentation=ModernBattlePresentation.State.EMPTY;
        ModernNaturalRegen.Clock regenClock=ModernNaturalRegen.Clock.initial();
        ModernRegenProtocol.State regen;
        ModernHonor.Life life;
        ModernSiege.Life siegeLife;
        ModernQuestLife.State questLife;
        boolean healed;long lastHealingNanos;int healingDelayMillis;
        MutableActor(long entityId, PreparedMember prepared, ModernRooms.Member member, boolean npcFireStorm) {
            this.entityId = entityId; this.prepared = prepared; this.member = member; position = prepared.spawn();
            buffs=ModernTimedEffects.initial(entityId);
            regen=ModernRegenProtocol.State.initial(entityId);
            stepIntervalNanos = prepared.stepIntervalNanos(); attackIntervalNanos = prepared.attackIntervalNanos();
            castIntervalNanos = prepared.castIntervalNanos();
            loadout = prepared.fighter() == null ? null : ModernBattleLoadout.initial(prepared.fighter(),
                    new ModernBattleLoadout.Features(prepared.bowEnabled(),prepared.fireStormEnabled(),npcFireStorm,
                            prepared.disintegrate()!=null||prepared.npcDisintegrate()!=null,prepared.matchItems()!=null,prepared.regenEnabled(),
                            prepared.greaterHaste()!=null,prepared.haste()!=null,prepared.slow()!=null),prepared.weightBasis());
            var combatant = loadout == null ? null : loadout.combatant();
            if (combatant != null) { hp = combatant.initialHp(); mp = combatant.initialMp(); }
            if(prepared.matchItems()!=null)itemUse=ModernItemRuntime.State.initial(prepared.matchItems().stream()
                    .filter(s->s.offer().ammo()==null&&s.offer().material()==null).map(s->new ModernItemProtocol.Stock(s.offer().itemId(),s.quantity(),s.quantity())).toList());
            if(prepared.bowEnabled())ammo=ModernAmmo.State.initial(entityId,prepared.matchItems().stream().filter(s->s.offer().ammo()!=null)
                    .map(s->new ModernAmmo.Stock(s.offer().ammo(),s.quantity(),s.quantity())).toList());
            if(loadout!=null&&loadout.weightBasis()!=null)weight=ModernInventoryWeight.fromShop(loadout.weightBasis(),prepared.matchItems());
        }
        MutableActor(long entityId, BotPrepared bot, ModernRooms.Member member, PreparedMember policy) {
            this.entityId = entityId; this.prepared = null; this.member = member; position = bot.spawn();
            buffs=ModernTimedEffects.initial(entityId);
            regen=ModernRegenProtocol.State.initial(entityId);
            loadout = ModernBattleLoadout.bot(bot.identity(), bot.template());
            var combatant = loadout.combatant();
            stepIntervalNanos = policy.stepIntervalNanos(); attackIntervalNanos = policy.attackIntervalNanos();
            castIntervalNanos = policy.castIntervalNanos();
            hp = combatant.initialHp(); mp = combatant.initialMp(); loaded = true;
            if(policy.matchItems()!=null)itemUse=ModernItemRuntime.State.initial(List.of());
            if(policy.weightBasis()!=null)weight=ModernInventoryWeight.notApplicable();
            if(policy.bowEnabled())ammo=ModernAmmo.State.initial(entityId,List.of());
        }
        Actor snapshot() { return new Actor(entityId, member, position, heading, loaded, sequence, stepIntervalNanos); }
        long spellSequence() {return spellRuntime==null?0:spellRuntime.sequence();}
    }
    private final ModernRooms.FrozenPreparation preparation;
    private final ModernWorldMap map;
    private final Limits limits;
    private final LongSupplier nanoClock;
    private final LongSupplier epochClock;
    private final ModernPhysicalCombat.BoundedRandom random;
    private final boolean combatEnabled;
    private final boolean spellsEnabled;
    private final boolean damageMagicEnabled;
    private final boolean npcMagicEnabled;
    private final boolean npcFireStormEnabled;
    private final boolean npcDisintegrateEnabled;
    private final boolean itemsEnabled;
    private final boolean weightEnabled;
    private final boolean timedEnabled;
    private final boolean bowEnabled;
    private final boolean regenEnabled;
    private final boolean fireStormEnabled;
    private final boolean disintegrateEnabled;
    private final boolean greaterHasteEnabled;
    private final boolean hasteEnabled;
    private final boolean slowEnabled;
    private final boolean playerHealingEnabled;
    private final boolean armorProtectionEnabled;
    private final java.util.UUID armorWorld;
    private final boolean vipEnabled;
    private final java.util.UUID vipWorld;
    private long vipMutation;
    private VipOperation vipOperation;
    private boolean planningVipRegen;
    private static final class VipOperation {boolean sampled;long version;}
    private static final class VipStaleCalculation extends RuntimeException {}
    private ModernSlowTargets.State slowTargets;
    private ModernHasteTargets.State hasteTargets;
    private final ModernHonor.Config honorConfig;
    private final ModernSiege.Space siegeSpace;
    private ModernSiege.State siegeState;
    private ModernMatchSettlement.Event siegeResult;
    private final ModernNpcBattle.Config npcConfig;
    private ModernNpcRoster npcRoster;
    private ModernQuestProgress.State questProgress;
    private ModernTimeAttack.State timeAttackState;
    private ModernMatchSettlement.Event timeAttackResult;
    private final Map<Long,ModernTimeAttack.Exchange> timeAttackExchanges=new HashMap<>();
    private ModernNpcRoster.Scope npcScope;
    private final Map<ModernParticipantId, ModernNpcBattle.Exchange> playerNpcExchanges = new HashMap<>();
    private final Map<ModernNpcRoster.Id, ModernNpcBattle.Exchange> npcExchanges = new HashMap<>();
    private final List<ModernHonor.Objective> objectives=new ArrayList<>();
    private final Map<Integer,ModernQuestObjectives.Actor> questObjectives=new java.util.TreeMap<>();
    private final Map<ModernParticipantId,ModernQuestObjectives.Exchange> questExchanges=new HashMap<>();
    private final List<ModernWorldMap.Cell> teleportCells;
    private final boolean settlementEnabled;
    private ModernPetRace.State raceState;
    private java.util.UUID raceResultId;
    private final List<MutableActor> actors;
    private final Map<TournamentPresence.Owner, MutableActor> owners;
    private final long loadingStartedNanos;
    private long lastObservedNanos, countdownStartedNanos, activeStartedNanos;
    private boolean activeClockStarted;
    private long lastObservedEpochMillis = -1;
    private long revision = 1;
    private Phase phase = Phase.LOADING;
    private AbortReason abortReason = AbortReason.NONE;
    private ModernMatchSettlement.Event result;
    private boolean closed;

    ModernBattleEngine(ModernRooms.FrozenPreparation preparation, ModernWorldBinding binding,
                       List<PreparedMember> prepared, Limits limits, LongSupplier nanoClock) {
        this(preparation, binding, prepared, limits, nanoClock,
                bound -> java.util.concurrent.ThreadLocalRandom.current().nextLong(bound));
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation, ModernWorldBinding binding,
                       List<PreparedMember> prepared, Limits limits, LongSupplier nanoClock,
                       ModernPhysicalCombat.BoundedRandom random) {
        this(preparation, binding, prepared, limits, nanoClock, System::currentTimeMillis, random);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation, ModernWorldBinding binding,
                       List<PreparedMember> prepared, Limits limits, LongSupplier nanoClock,
                       LongSupplier epochClock, ModernPhysicalCombat.BoundedRandom random) {
        this(preparation, binding, prepared, limits, nanoClock, epochClock, random, false);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation, ModernWorldBinding binding,
                       List<PreparedMember> prepared, Limits limits, LongSupplier nanoClock,
                       LongSupplier epochClock, ModernPhysicalCombat.BoundedRandom random, boolean settlementEnabled) {
        this(preparation, binding, prepared, List.of(), limits, nanoClock, epochClock, random, settlementEnabled);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation, ModernWorldBinding binding,
                       List<PreparedMember> prepared, List<BotPrepared> bots, Limits limits, LongSupplier nanoClock,
                       LongSupplier epochClock, ModernPhysicalCombat.BoundedRandom random, boolean settlementEnabled) {
        this(preparation,binding,prepared,bots,limits,nanoClock,epochClock,random,settlementEnabled,null);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation,ModernWorldBinding binding,
                       List<PreparedMember> prepared,List<BotPrepared> bots,Limits limits,LongSupplier nanoClock,
                       LongSupplier epochClock,ModernPhysicalCombat.BoundedRandom random,boolean settlementEnabled,ModernHonor.Config honor) {
        this(preparation,binding,prepared,bots,limits,nanoClock,epochClock,random,settlementEnabled,honor,()->false);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation,ModernWorldBinding binding,
                       List<PreparedMember> prepared,List<BotPrepared> bots,Limits limits,LongSupplier nanoClock,
                       LongSupplier epochClock,ModernPhysicalCombat.BoundedRandom random,boolean settlementEnabled,
                       ModernHonor.Config honor,BooleanSupplier preparationCancelled) {
        this(preparation,binding,prepared,bots,limits,nanoClock,epochClock,random,settlementEnabled,honor,preparationCancelled,null);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation,ModernWorldBinding binding,
                       List<PreparedMember> prepared,List<BotPrepared> bots,Limits limits,LongSupplier nanoClock,
                       LongSupplier epochClock,ModernPhysicalCombat.BoundedRandom random,boolean settlementEnabled,
                       ModernHonor.Config honor,BooleanSupplier preparationCancelled,ModernPetRace.Course race) {
        this(preparation,binding,prepared,bots,limits,nanoClock,epochClock,random,settlementEnabled,honor,preparationCancelled,race,null);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation,ModernWorldBinding binding,
                       List<PreparedMember> prepared,List<BotPrepared> bots,Limits limits,LongSupplier nanoClock,
                       LongSupplier epochClock,ModernPhysicalCombat.BoundedRandom random,boolean settlementEnabled,
                       ModernHonor.Config honor,BooleanSupplier preparationCancelled,ModernPetRace.Course race,ModernNpcBattle.Config npcs) {
        this(preparation,binding,prepared,bots,limits,nanoClock,epochClock,random,settlementEnabled,honor,preparationCancelled,race,npcs,null,-1);
    }
    /** Internal siege seam only. Dedicated protocol, result journal, shared art
     * and production mapping must be qualified before admission is enabled. */
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation,ModernWorldBinding binding,
                       List<PreparedMember> prepared,List<BotPrepared> bots,Limits limits,LongSupplier nanoClock,
                       LongSupplier epochClock,ModernPhysicalCombat.BoundedRandom random,boolean settlementEnabled,
                       ModernHonor.Config honor,BooleanSupplier preparationCancelled,ModernPetRace.Course race,ModernNpcBattle.Config npcs,
                       ModernSiege.Config siege,int attackerTeam) {
        this(preparation,binding,prepared,bots,limits,nanoClock,epochClock,random,settlementEnabled,honor,preparationCancelled,race,npcs,siege,attackerTeam,null);
    }
    ModernBattleEngine(ModernRooms.FrozenPreparation preparation,ModernWorldBinding binding,
                       List<PreparedMember> prepared,List<BotPrepared> bots,Limits limits,LongSupplier nanoClock,
                       LongSupplier epochClock,ModernPhysicalCombat.BoundedRandom random,boolean settlementEnabled,
                       ModernHonor.Config honor,BooleanSupplier preparationCancelled,ModernPetRace.Course race,ModernNpcBattle.Config npcs,
                       ModernSiege.Config siege,int attackerTeam,ModernSiege.Space qualifiedSiege) {
        Objects.requireNonNull(preparationCancelled);
        this.honorConfig=honor;
        this.npcConfig=npcs;
        this.npcMagicEnabled=npcs!=null&&npcs.lightArrow();
        this.npcFireStormEnabled=npcs!=null&&npcs.fireStorm();
        this.npcDisintegrateEnabled=npcs!=null&&npcs.disintegrate();
        this.preparation = Objects.requireNonNull(preparation);
        this.map = Objects.requireNonNull(binding).map(); this.limits = Objects.requireNonNull(limits);
        this.nanoClock = Objects.requireNonNull(nanoClock);
        this.epochClock = Objects.requireNonNull(epochClock);
        Objects.requireNonNull(random);
        this.random = bound -> vipRandom(random,bound);
        if (prepared == null || prepared.isEmpty() || prepared.size() > 100) throw invalid();
        List<PreparedMember> safe = List.copyOf(prepared);
        if (bots == null || bots.size() > 100 - safe.size()) throw invalid();
        List<BotPrepared> safeBots = List.copyOf(bots);
        combatEnabled = safe.get(0).fighter() != null;
        vipEnabled=safe.get(0).vipEnabled();vipWorld=vipEnabled?java.util.UUID.randomUUID():null;
        if(safe.stream().anyMatch(row->row.vipEnabled()!=vipEnabled))throw invalid();
        if(vipEnabled&&(binding.key().mode()!=BattleRules.Mode.DEATHMATCH.wireId()||!safeBots.isEmpty()
                ||honor!=null||race!=null||npcs!=null||siege!=null))throw invalid();
        armorProtectionEnabled=combatEnabled&&safe.get(0).fighter().armorProtection()!=null;
        armorWorld=armorProtectionEnabled?java.util.UUID.randomUUID():null;
        if(safe.stream().anyMatch(row->(row.fighter()!=null&&row.fighter().armorProtection()!=null)!=armorProtectionEnabled))throw invalid();
        if (!safeBots.isEmpty() && !combatEnabled) throw invalid();
        if (!safeBots.isEmpty() && safe.stream().anyMatch(row -> row.stepIntervalNanos() != safe.get(0).stepIntervalNanos()
                || row.attackIntervalNanos() != safe.get(0).attackIntervalNanos()
                || row.castIntervalNanos() != safe.get(0).castIntervalNanos())) throw invalid();
        spellsEnabled = safe.get(0).skills() != null;
        damageMagicEnabled=safe.get(0).damageMagic();
        itemsEnabled=safe.get(0).matchItems()!=null;
        timedEnabled=safe.get(0).timedEnabled();
        bowEnabled=safe.get(0).bowEnabled();if(safe.stream().anyMatch(row->row.bowEnabled()!=bowEnabled))throw invalid();
        regenEnabled=safe.get(0).regenEnabled();if(safe.stream().anyMatch(row->row.regenEnabled()!=regenEnabled))throw invalid();
        fireStormEnabled=safe.get(0).fireStormEnabled();if(safe.stream().anyMatch(row->row.fireStormEnabled()!=fireStormEnabled))throw invalid();
        disintegrateEnabled=safe.get(0).disintegrate()!=null;
        greaterHasteEnabled=safe.get(0).greaterHaste()!=null;
        hasteEnabled=safe.get(0).haste()!=null;
        slowEnabled=safe.get(0).slow()!=null;
        playerHealingEnabled=safe.get(0).playerHealing();
        if(safe.stream().anyMatch(row->row.playerHealing()!=playerHealingEnabled))throw invalid();
        if(safe.stream().anyMatch(row->(row.slow()!=null)!=slowEnabled))throw invalid();
        if(slowEnabled)slowTargets=ModernSlowTargets.State.empty(new ModernTargetHistory.World(preparation.snapshot().summary().id(),preparation.matchId(),preparation.generation()));
        if(hasteEnabled)hasteTargets=ModernHasteTargets.State.empty(new ModernTargetHistory.World(
                preparation.snapshot().summary().id(),preparation.matchId(),preparation.generation()));
        if(safe.stream().anyMatch(row->(row.haste()!=null)!=hasteEnabled))throw invalid();
        if(safe.stream().anyMatch(row->(row.greaterHaste()!=null)!=greaterHasteEnabled))throw invalid();
        if(safe.stream().anyMatch(row->(row.npcDisintegrate()!=null)!=npcDisintegrateEnabled))throw invalid();
        if(safe.stream().anyMatch(row->(row.disintegrate()!=null)!=disintegrateEnabled)
                ||disintegrateEnabled&&(siege!=null||npcs!=null||race!=null||binding.key().mode()!=1&&honor==null))throw invalid();
        if(safe.stream().anyMatch(row->row.timedEnabled()!=timedEnabled))throw invalid();
        weightEnabled=safe.get(0).weightBasis()!=null;
        if(safe.stream().anyMatch(row->(row.weightBasis()!=null)!=weightEnabled))throw invalid();
        if(safe.stream().anyMatch(row->(row.matchItems()!=null)!=itemsEnabled))throw invalid();
        if(safe.stream().anyMatch(row->row.damageMagic()!=damageMagicEnabled))throw invalid();
        if (safe.stream().anyMatch(row -> (row.skills() != null) != spellsEnabled)) throw invalid();
        this.settlementEnabled = settlementEnabled;
        if (safe.stream().anyMatch(row -> (row.fighter() != null) != combatEnabled)) throw invalid();
        // Captured roster and geometry are immutable. In particular, the bounded
        // connected-component scan must not retain the cancellation Guard while
        // it traverses up to 65,536 cells. None of this assembly publishes state.
        var room = preparation.snapshot(); var participants = preparation.participants();
        if(siege!=null) {
            // Unsupported siege magic/ranged/timed projections are not silently
            // accepted as ordinary PvP. Existing non-siege contracts are intact.
            if(!combatEnabled||honor!=null||npcs!=null||race!=null||settlementEnabled||damageMagicEnabled||fireStormEnabled||bowEnabled||timedEnabled&&!slowEnabled
                    ||attackerTeam<0||attackerTeam>1||limits.activeTimeoutMillis()!=siege.durationMillis()
                    ||room.members().stream().map(ModernRooms.Member::team).distinct().count()!=2
                    ||room.members().stream().anyMatch(m->m.team()<0||m.team()>1))throw invalid();
            if(!preparation.isLive()||preparationCancelled.getAsBoolean())throw invalid();
            siegeSpace=qualifiedSiege==null?siege.qualify(binding,()->!preparation.isLive()||preparationCancelled.getAsBoolean()):qualifiedSiege.require(binding,siege);
            siegeState=ModernSiege.initial(new ModernSiege.Scope(room.summary().id(),preparation.matchId(),preparation.generation()),siege,attackerTeam);
        } else {if(attackerTeam!=-1||qualifiedSiege!=null)throw invalid();siegeSpace=null;}
        if(honor!=null) {
            if(!settlementEnabled||!regenEnabled||!damageMagicEnabled||safe.stream().anyMatch(p->p.fighter().healing()==null))throw invalid();
            try{honor.validate(binding);}catch(ModernWorldMap.MapException failure){throw invalid();}
            for(var tower:honor.towers())objectives.add(ModernHonor.Objective.initial(room.members().size(),tower));
            if(safe.stream().anyMatch(p->objectives.stream().anyMatch(o->o.cell().equals(p.spawn())))
                    ||safeBots.stream().anyMatch(p->objectives.stream().anyMatch(o->o.cell().equals(p.spawn()))))throw invalid();
        }
        if (questObjectivesEnabled()&&!settlementEnabled)throw invalid();
        if (questLivesEnabled()&&!regenEnabled)throw invalid();
        if (settlementEnabled && (!combatEnabled || honor==null&&!questObjectivesEnabled()&&room.summary().config().mode() != BattleRules.Mode.DEATHMATCH.wireId()
                || room.members().stream().map(ModernRooms.Member::team).distinct().count() != 2)) throw invalid();
        // PreparedMember already qualifies each healing stock against the source
        // template. NPC damage and potions share this engine's HP/regen/weight
        // ledger. NPC light-arrow is a separate internal opt-in, never the PvP
        // damageMagic contract; timed/ammo/PvP fire-storm remain unavailable here.
        // Typed NPC fire storm/haste are separately qualified internal opt-ins.
        // Cooperative haste is qualified internally before Mapping/configuration
        // and the public descriptor admit it; no legacy projection may hide it.
        if(npcs!=null && (!npcs.key().equals(binding.key()) || !combatEnabled || settlementEnabled&&!questObjectivesEnabled() || honor!=null || race!=null
                || damageMagicEnabled
                || timedEnabled&&!hasteEnabled&&!slowEnabled
                || greaterHasteEnabled || bowEnabled || fireStormEnabled)) throw invalid();
        if(npcs!=null&&npcs.timeAttack()!=null&&(settlementEnabled||!safeBots.isEmpty()||room.members().size()>60||limits.activeTimeoutMillis()>60_000
                ||room.members().stream().anyMatch(m->m.team()!=0)))throw invalid();
        if(npcMagicEnabled&&(!spellsEnabled||safe.stream().anyMatch(p->p.fighter().magic()==null)))throw invalid();
        if(npcFireStormEnabled) {
            if(!spellsEnabled||!regenEnabled)throw invalid();
            for(var p:safe)ModernFireStorm.qualify(p.fighter());
        }
        if (combatEnabled && npcs==null && !BattleRules.Mode.fromWire(room.summary().config().mode()).playerVersusPlayer()) throw invalid();
        if (preparation.matchId() < 1 || preparation.generation() < 1 || !room.summary().readySatisfied()
                || !binding.matches(room) || safe.size() != participants.size()
                || safeBots.size() != preparation.bots().size()
                || safe.size() + safeBots.size() != room.members().size()) throw invalid();
        var byOwner = new HashMap<TournamentPresence.Owner, PreparedMember>();
        for (PreparedMember member : safe) if (byOwner.put(member.owner(), member) != null) throw invalid();
        var byBot = new HashMap<ModernParticipantId, BotPrepared>();
        for (BotPrepared bot : safeBots) if (byBot.put(bot.identity(), bot) != null) throw invalid();
        var preparedOwners = new HashMap<TournamentPresence.Owner, MutableActor>();
        var ordered = new ArrayList<MutableActor>(room.members().size());
        var cells = new ArrayList<ModernWorldMap.Cell>(room.members().size());
        // Entity handles follow the immutable, typed-identity-sorted room roster,
        // never database object IDs, input order, class IDs or a native u32 cast.
        for (ModernRooms.Member member : room.members()) {
            if (!member.identity().isHuman()) {
                BotPrepared bot = byBot.remove(member.identity());
                if (bot == null || !preparation.bots().contains(member)
                        || member.level() != bot.template().level()
                        || member.characterClass() != bot.template().characterClass()
                        || member.sex() != bot.template().sex() || !member.ready()) throw invalid();
                var actor = new MutableActor(ordered.size() + 1L, bot, member, safe.get(0));
                ordered.add(actor); cells.add(bot.spawn()); continue;
            }
            var participant = participants.stream().filter(row -> row.member().key().equals(member.key())).findFirst().orElseThrow(ModernBattleEngine::invalid);
            PreparedMember input = byOwner.remove(participant.owner());
            if (input == null || !participant.member().equals(member) || !participant.scope().equals(input.scope())
                    || !qualified(input, member) || preparedOwners.containsKey(input.owner())) throw invalid();
            var actor = new MutableActor(ordered.size() + 1L, input, member, npcFireStormEnabled);
            if(vipEnabled)actor.loadout=actor.loadout.rebindVip(vipActor(actor));
            ordered.add(actor); cells.add(input.spawn()); preparedOwners.put(input.owner(), actor);
        }
        if (!byOwner.isEmpty() || !byBot.isEmpty()) throw invalid();
        if(siegeState!=null)for(var actor:ordered) {
            if(!siege.camp(actor.member.team(),attackerTeam).contains(actor.position)||actor.loadout.combatant().bow())throw invalid();
            actor.siegeLife=ModernSiege.Life.initial(actor.entityId);
        }
        if(npcs!=null) {
            if(!npcs.players().keySet().equals(ordered.stream().filter(a->a.prepared!=null)
                    .map(a->a.member.identity()).collect(java.util.stream.Collectors.toSet())))throw invalid();
            for(var actor:ordered) {
                var id=actor.member.identity();
                if(actor.prepared!=null)actor.loadout=actor.loadout.withNpc(npcs.players().get(id));
            }
            npcScope=new ModernNpcRoster.Scope(room.summary().id(),preparation.matchId(),preparation.generation());
            npcRoster=new ModernNpcRoster(npcScope,map,npcs.laneAnchors(),npcs.capacity(),npcs.corpseMillis(),isolatedForces(),npcs.timeAttack()!=null);
            if(npcs.timeAttack()!=null) {
                npcs.timeAttack().qualify(binding,npcs.catalog(),npcs.laneAnchors(),npcs.capacity());
                if(ordered.stream().anyMatch(a->npcs.timeAttack().reservedCells().contains(a.position)))throw invalid();
                var leader=ordered.stream().filter(a->a.member.key().equals(room.owner())).findFirst().orElseThrow(ModernBattleEngine::invalid);
                timeAttackState=ModernTimeAttack.initial(npcScope,npcs.timeAttack(),leader.entityId);
            }
            if(npcs.questPlan()!=null) {
                npcs.questPlan().qualify(binding,npcs.catalog(),npcs.laneAnchors(),npcs.capacity());
                questProgress=ModernQuestProgress.initial(npcScope,npcs.questPlan());
            }
        }
        if(race!=null) {
            if(!race.key().equals(binding.key())||!race.map().manifest().equals(map.manifest())||combatEnabled||spellsEnabled||itemsEnabled||honor!=null||settlementEnabled
                    ||!safeBots.isEmpty()||safe.stream().anyMatch(row->row.stepIntervalNanos()!=race.stepIntervalNanos()))throw invalid();
            raceState=ModernPetRace.initial(preparation.matchId(),preparation.generation(),race,
                    ordered.stream().map(a->a.member.identity().humanKey()).toList(),cells,limits.activeTimeoutMillis());
        }
        if(honorConfig!=null)for(var actor:ordered)actor.life=ModernHonor.Life.initial(actor.entityId);
        if(questLivesEnabled())for(var actor:ordered)actor.questLife=ModernQuestLife.State.initial(actor.entityId,actor.member.team(),actor.position);
        if(armorProtectionEnabled)for(var actor:ordered)if(actor.prepared!=null)
            actor.armor=new ArmorProtectionView(ModernArmorProcProtection.initial(armorActor(actor)),revision,List.of(),null);
        for(var actor:ordered)if(actor.loadout!=null)initializeUseHistory(actor);
        try { ModernQuestSpace.qualify(map, room.members(), cells,
                Set.copyOf(objectives.stream().map(ModernHonor.Objective::cell).toList()),
                () -> !preparation.isLive() || preparationCancelled.getAsBoolean(),isolatedForces());
            // Deathmatch births already share a walkable arena; teleport may
            // not bypass that qualification by selecting a different island.
            // Keep Siege/Quest/Honor/race policies and connected-map RNG order.
            boolean arenaTeleport=binding.key().mode()==BattleRules.Mode.DEATHMATCH.wireId()
                    &&honor==null&&npcs==null&&siege==null&&race==null;
            teleportCells=!spellsEnabled?List.of():arenaTeleport
                    ?ModernDeathmatchTeleportSpace.candidates(map,cells.get(0),()->!preparation.isLive()||preparationCancelled.getAsBoolean())
                    :map.teleportCells();
        }
        catch (ModernWorldMap.MapException failure) {
            if (failure.failure() == ModernWorldMap.Failure.CANCELLED)
                throw new IllegalStateException("world preparation cancelled");
            throw invalid();
        }
        if (combatEnabled) for (var actor : ordered)
            actor.equipmentResources = ModernVitalBalance.State.initial(revision,
                    actor.loadout.fighter() == null ? 0 : actor.loadout.fighter().profile().equipmentRevision(),
                    new ModernEquipmentVitals.Vitals(actor.hp,actor.loadout.combatant().maxHp(),actor.mp,actor.loadout.combatant().maxMp()));
        var preparedActors = List.copyOf(ordered);
        try (var ignored = preparation.guard()) {
            // guard() checks live again after all pure validation; the final
            // ticket fields still identify exactly the captured immutable data.
            owners = preparedOwners; actors = preparedActors;
            loadingStartedNanos = nanoClock.getAsLong(); lastObservedNanos = loadingStartedNanos;
            // Last constructor step: invalid qualification/geometry/clock must
            // not consume the capability, but a successful instance is its
            // sole world authority forever, including after this engine closes.
            if (Thread.currentThread().isInterrupted() || preparationCancelled.getAsBoolean())
                throw new IllegalStateException("world preparation cancelled");
            preparation.claimEngine();
        }
    }
    private void initializeUseHistory(MutableActor actor) {
        long gear=actor.loadout.fighter()==null?0:actor.loadout.fighter().profile().equipmentRevision();
        var histories=new java.util.EnumMap<ModernUseHistory.Lane,ModernUseHistory.Spell>(ModernUseHistory.Lane.class);
        if(spellsEnabled)histories.put(ModernUseHistory.Lane.TELEPORT,ModernUseHistory.Spell.initial(gear,revision,10));
        boolean bot=actor.prepared==null;
        if(damageMagicEnabled)histories.put(ModernUseHistory.Lane.LIGHT_ARROW,ModernUseHistory.Spell.initial(gear,revision,Math.toIntExact(actor.loadout.combatant().lightArrowMpCost())));
        if(honorConfig!=null)histories.put(ModernUseHistory.Lane.HEALING,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.healing().mpCost()));
        if(fireStormEnabled)histories.put(ModernUseHistory.Lane.FIRE_STORM,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.fireStorm().mpCost()));
        if(disintegrateEnabled)histories.put(ModernUseHistory.Lane.DISINTEGRATE,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.disintegrate().mpCost()));
        if(npcMagicEnabled)histories.put(ModernUseHistory.Lane.NPC_LIGHT_ARROW,ModernUseHistory.Spell.initial(gear,revision,bot?0:Math.toIntExact(actor.loadout.combatant().lightArrowMpCost())));
        if(npcFireStormEnabled)histories.put(ModernUseHistory.Lane.NPC_FIRE_STORM,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.fireStorm().mpCost()));
        if(npcDisintegrateEnabled)histories.put(ModernUseHistory.Lane.NPC_DISINTEGRATE,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.disintegrate().mpCost()));
        if(greaterHasteEnabled)histories.put(ModernUseHistory.Lane.GREATER_HASTE,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.greaterHaste().mpCost()));
        if(hasteEnabled)histories.put(ModernUseHistory.Lane.HASTE,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.haste().mpCost()));
        if(slowEnabled)histories.put(ModernUseHistory.Lane.SLOW,ModernUseHistory.Spell.initial(gear,revision,bot?0:actor.loadout.slow().mpCost()));
        actor.spellHistory=Map.copyOf(histories);
        if(playerHealingEnabled) {
            var world=new ModernTargetHistory.World(preparation.snapshot().summary().id(),preparation.matchId(),preparation.generation());
            actor.playerHealing=new ModernPlayerHealingHistory.Entry(
                    ModernPlayerHealingHistory.Incoming.initial(world,new ModernTargetHistory.Participant(actor.entityId,actor.member.identity())),
                    bot?null:ModernPlayerHealingHistory.Outgoing.initial(gear,revision,ModernPlayerHealing.qualify(actor.loadout.fighter()).mpCost()));
        }
        if(actor.itemUse!=null)actor.itemHistory=ModernUseHistory.Items.initial(gear,revision,actor.loadout.potionRecovery());
        if(actor.prepared!=null) {
            var world=new ModernTargetHistory.World(preparation.snapshot().summary().id(),preparation.matchId(),preparation.generation());
            var identity=new ModernEquipmentHistory.Identity(world,actor.prepared.scope(),new ModernTargetHistory.Participant(actor.entityId,actor.member.identity()));
            long lawful=actor.loadout.fighter().profile().source().persisted().lawful();
            actor.spellRuntime=ModernSpellRuntime.State.initial(identity,histories.keySet(),revision,lawful,playerHealingEnabled);
            if(slowEnabled)actor.materials=ModernSpellMaterials.State.initial(world,actor.prepared.scope(),identity.actor(),actor.prepared.matchItems());
        }
    }
    private static List<ModernSpellMaterials.Stock> materialStocks(MutableActor actor){return actor.materials==null?List.of():actor.materials.stocks();}
    private static long resourceLife(MutableActor actor) {
        return actor.questLife!=null?actor.questLife.incarnation()-1:actor.siegeLife!=null?actor.siegeLife.respawns()
                :actor.life!=null?actor.life.respawnCount():0;
    }
    /** Internal state inspection only. No legacy packet silently acquires these fields. */
    record ArmorProtectionView(ModernArmorProcProtection.State state,long revision,List<ModernArmorProcProtection.Trigger> triggers,
            ModernArmorPresentation.Anchor anchor) {
        ArmorProtectionView {
            Objects.requireNonNull(state);triggers=List.copyOf(triggers);
            if(revision<1||triggers.isEmpty()!=(anchor==null))throw invalid();
        }
    }
    synchronized Map<Long,ArmorProtectionView> armorProtectionSnapshot() {
        requireOpen();if(!armorProtectionEnabled)throw invalid();
        return guarded(()->{advance();return armorViews();},this::armorViews);
    }
    private Map<Long,ArmorProtectionView> armorViews() {
        var result=new HashMap<Long,ArmorProtectionView>();for(var a:actors)if(a.armor!=null)result.put(a.entityId,a.armor);return Map.copyOf(result);
    }
    private ModernArmorProcProtection.Actor armorActor(MutableActor actor) {
        return new ModernArmorProcProtection.Actor(armorWorld,actor.member.identity().humanKey(),actor.entityId,resourceLife(actor));
    }
    private record ArmorStamp(MutableActor actor,ModernBattleLoadout loadout,ModernVitalBalance.State resources,
            long hp,long mp,long move,long attack,long life,ModernWorldMap.Cell cell,
            ModernSpellRuntime.State spells,ModernTimedEffects.State buffs,ModernAmmo.State ammo,ModernInventoryWeight.Snapshot weight,ArmorProtectionView armor) {
        static ArmorStamp of(MutableActor a){return new ArmorStamp(a,a.loadout,a.equipmentResources,a.hp,a.mp,a.sequence,a.attackSequence,
                resourceLife(a),a.position,a.spellRuntime,a.buffs,a.ammo,a.weight,a.armor);}
        boolean current(){return actor.loadout==loadout&&actor.equipmentResources==resources&&actor.hp==hp&&actor.mp==mp
                &&actor.sequence==move&&actor.attackSequence==attack&&resourceLife(actor)==life&&actor.position.equals(cell)
                &&actor.spellRuntime==spells&&actor.buffs==buffs&&actor.ammo==ammo&&actor.weight==weight&&actor.armor==armor;}
    }
    private record ArmorGuard(ArmorStamp target,ArmorStamp attacker,ModernArmorProcProtection.Selection selection,
            ModernIncomingDamage.Protection protection) {}
    private record ArmorHit(ArmorGuard guard,ModernArmorProcProtection.Candidate candidate) {}
    private ArmorGuard armorGuard(MutableActor target,MutableActor attacker,long now) {
        if(target.armor==null)return null;
        var identity=armorActor(target);
        if(!identity.equals(target.armor.state().actor()))throw invalid();
        return new ArmorGuard(ArmorStamp.of(target),attacker==null?null:ArmorStamp.of(attacker),
                target.loadout.fighter().armorProtection().select(identity),ModernIncomingDamage.at(target.armor.state(),elapsed(now)));
    }
    private static ModernIncomingDamage.Protection protection(ArmorGuard guard){return guard==null?ModernIncomingDamage.NONE:guard.protection();}
    private ArmorHit planArmorHit(ArmorGuard guard,ModernIncomingDamage.Impact impact) {
        return guard==null?null:new ArmorHit(guard,ModernArmorProcProtection.prepareHurt(guard.selection(),impact,true,random));
    }
    private ArmorProtectionView acceptArmorHit(ArmorHit hit,long now) {
        if(hit==null)return null;
        var guard=hit.guard();var a=guard.target().actor();
        if(!guard.target().current()||guard.attacker()!=null&&!guard.attacker().current())
            throw new ModernArmorProcProtection.ProcException(ModernArmorProcProtection.Failure.STALE_CANDIDATE);
        var selection=a.loadout.fighter().armorProtection().select(armorActor(a));
        var after=ModernArmorProcProtection.acceptAt(hit.candidate(),selection,a.armor.state(),a.hp,elapsed(now));
        var triggers=hit.candidate().triggers();
        // Capture at the accepted damage commit, never at a later READ/movement.
        var anchor=triggers.isEmpty()?null:new ModernArmorPresentation.Anchor(guard.target().move(),guard.target().cell());
        return new ArmorProtectionView(after,Math.incrementExact(revision),triggers,anchor);
    }
    private static void commitArmorHit(MutableActor target,ArmorProtectionView next){if(next!=null)target.armor=next;}
    private void clearArmor(MutableActor actor,long now,long next) {
        if(actor.armor==null)return;var state=actor.armor.state();
        if(state.untilNanos()==0&&actor.armor.triggers().isEmpty())return;
        // Clearing cannot overflow a sequence after HP has committed. The exact
        // immutable state/view still changes, invalidating outstanding guards.
        actor.armor=new ArmorProtectionView(new ModernArmorProcProtection.State(state.actor(),state.sequence(),
                Math.max(state.observedNanos(),elapsed(now)),0),next,List.of(),null);
    }
    private void clearAllArmor(long now,long next){if(armorProtectionEnabled)for(var a:actors)clearArmor(a,now,next);}
    private void syncArmorLife(MutableActor actor,long next) {
        if(actor.armor!=null&&!actor.armor.state().actor().equals(armorActor(actor)))
            actor.armor=new ArmorProtectionView(ModernArmorProcProtection.initial(armorActor(actor)),next,List.of(),null);
    }
    private void expireArmor(long now) {
        if(!armorProtectionEnabled||phase!=Phase.ACTIVE)return;
        var planned=new ArrayList<ArmorProtectionView>(actors.size());boolean changed=false;
        try {
            for(var a:actors) {
                var prior=a.armor;var next=prior;
                if(prior!=null) {
                    var state=prior.state();var identity=armorActor(a);
                    var after=!state.actor().equals(identity)?ModernArmorProcProtection.initial(identity):ModernArmorProcProtection.expire(state,elapsed(now));
                    if(after!=state)next=new ArmorProtectionView(after,Math.incrementExact(revision),List.of(),null);
                }
                planned.add(next);changed|=next!=prior;
            }
        }catch(ArithmeticException|IllegalArgumentException failure){abortInternal(AbortReason.SERVER_ABORT);return;}
        if(changed){nextRevision();for(int i=0;i<actors.size();i++)actors.get(i).armor=planned.get(i);}
    }
    private ModernUseHistory.Anchor useAnchor(MutableActor actor) {
        long life=Math.incrementExact(resourceLife(actor));
        var c=actor.loadout.combatant();
        return new ModernUseHistory.Anchor(actor.loadout.fighter()==null?0:actor.loadout.fighter().profile().equipmentRevision(),life,
                new ModernEquipmentVitals.Vitals(actor.hp,c.maxHp(),actor.mp,c.maxMp()),actor.position.x(),actor.position.y());
    }
    private ModernBattlePresentation.Shot shot(MutableActor actor,ModernAmmo.State ammo,long now,ModernTargetHistory.Target target,long targetLife) {
        return new ModernBattlePresentation.Shot(ammo.shotCount(),ammo.shotRevision(),ammo.attackSequence(),now-loadingStartedNanos,
                Math.incrementExact(resourceLife(actor)),actor.equipmentResources.equipmentRevision(),
                new ModernWorldMap.Cell(ammo.fromX(),ammo.fromY()),target,targetLife,new ModernWorldMap.Cell(ammo.toX(),ammo.toY()),ammo.projectileSprite(),ammo.hit());
    }
    private ModernEquipmentHistory.State planCastHistory(MutableActor actor,ModernUseHistory.Lane lane,
            long next,long sequence,List<ModernTargetHistory.Effect> targets) {
        return equipmentHistory(actor).cast(lane,next,sequence,actor.position,targets);
    }
    private int spellCost(MutableActor actor,ModernUseHistory.Lane lane) {
        var spell=actor.spellHistory.get(lane);if(spell==null)throw invalid();return spell.current().cost();
    }
    private void preflightSpell(MutableActor actor,ModernUseHistory.Lane lane,long sequence) {
        actor.spellHistory.get(lane).current().cast(Math.incrementExact(revision),sequence);
    }
    private ModernSpellRuntime.State planSpellRuntime(MutableActor actor,ModernEquipmentHistory.State committed,
            ModernUseHistory.Lane lane,long now,ModernSpellRuntime.Detail detail) {
        return actor.spellRuntime.cast(equipmentHistory(actor),committed,lane,now,detail);
    }
    private ModernTargetHistory.Effect participantEffect(MutableActor target,long raw,long applied,long after) {
        var a=useAnchor(target);
        return new ModernTargetHistory.Effect(ModernTargetHistory.Kind.DAMAGE,new ModernTargetHistory.Health(
                new ModernTargetHistory.Participant(target.entityId,target.member.identity()),a.life(),a.equipmentRevision(),target.position,
                a.vitals().hp(),a.vitals().maximumHp()),raw,applied,after);
    }
    private ModernTargetHistory.Effect honorEffect(ModernHonor.Objective target,ModernTargetHistory.Kind kind,long raw,long applied,long after) {
        return new ModernTargetHistory.Effect(kind,new ModernTargetHistory.Health(new ModernTargetHistory.Honor(target.entityId(),target.team()),
                1,0,target.cell(),target.hp(),target.maximumHp()),raw,applied,after);
    }
    private ModernTargetHistory.Effect npcEffect(ModernNpcRoster.Actor target,long raw,long applied,long after) {
        return new ModernTargetHistory.Effect(ModernTargetHistory.Kind.DAMAGE,new ModernTargetHistory.Health(new ModernTargetHistory.Npc(target.id()),
                target.life(),0,target.cell(),target.hp(),target.template().maximumHp()),raw,applied,after);
    }
    private ModernTargetHistory.Effect structureEffect(ModernNpcFireStormStructures.Hit hit) {
        ModernTargetHistory.Target id=hit instanceof ModernNpcFireStormStructures.QuestHit q
                ?new ModernTargetHistory.Quest(q.id(),q.bornRevision()):new ModernTargetHistory.TimeTower(((ModernNpcFireStormStructures.TowerHit)hit).id());
        var result=hit.outcome();
        return new ModernTargetHistory.Effect(ModernTargetHistory.Kind.DAMAGE,new ModernTargetHistory.Health(id,1,0,hit.cell(),hit.hpBefore(),hit.maximumHp()),
                result.rawDamage(),result.appliedDamage(),result.remainingHp());
    }
    record UseHistories(ModernRooms.Scope scope,long matchId,long generation,long entityId,long worldRevision,
            Map<ModernUseHistory.Lane,ModernUseHistory.Spell> spells,ModernUseHistory.Items items,
            Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets) {
        UseHistories {spells=Map.copyOf(spells);targets=Map.copyOf(targets);}
    }
    /** Owner-bound internal read only; no new old-version packet or SET authority. */
    synchronized UseHistories useHistories(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation) {
        var actor=requireActor(owner,scope,matchId,generation);
        return new UseHistories(scope,matchId,generation,actor.entityId,revision,actor.spellHistory,actor.itemHistory,actor.targetHistory);
    }
    private static boolean qualified(PreparedMember input, ModernRooms.Member member) {
        var row = input.character(); var scope = input.scope();
        return member.identity().isHuman() && scope.sourceId() == member.key().sourceId() && scope.objectId() == member.key().objectId()
                && row.objectId() == member.key().objectId() && Objects.equals(row.name(), member.name())
                && row.level() == member.level() && row.characterClass() == member.characterClass() && row.sex() == member.sex();
    }

    synchronized Snapshot snapshot(TournamentPresence.Owner owner, ModernRooms.Scope scope, long matchId, long generation) {
        requireActor(owner, scope, matchId, generation);
        return guarded(() -> { advance(); return current(); }, this::current);
    }

    enum EquipmentDisposition { APPLIED, PERSISTED_RETIRED, UNCHANGED }
    record EquipmentResources(ModernRooms.Scope scope, long matchId, long generation, long entityId,
            long deathRevision, ModernVitalBalance.State balance) {}
    record EquipmentPublication(EquipmentDisposition disposition, long entityId, long equipmentRevision,
            long worldRevision, EquipmentResources resources,ModernEquipmentHistory.State history) {}

    private boolean baseEquipmentResources() {
        return combatEnabled && !spellsEnabled && !damageMagicEnabled && !npcMagicEnabled && !npcFireStormEnabled
                && !npcDisintegrateEnabled && !itemsEnabled && !weightEnabled && !timedEnabled && !bowEnabled
                && !regenEnabled && !fireStormEnabled && !disintegrateEnabled && honorConfig == null && npcConfig == null
                && siegeState == null && raceState == null && timeAttackState == null;
    }
    private void requireResourceBalance(MutableActor actor) {
        var value = actor.equipmentResources;
        if (value == null || value.revision() > revision || value.life() != resourceLife(actor)
                || !value.vitals().equals(new ModernEquipmentVitals.Vitals(actor.hp,actor.loadout.combatant().maxHp(),actor.mp,actor.loadout.combatant().maxMp()))
                || value.equipmentRevision() != (actor.loadout.fighter() == null ? 0 : actor.loadout.fighter().profile().equipmentRevision()))
            throw new IllegalStateException("equipment resource projection mismatch");
    }
    private ModernVitalBalance.State planDamage(MutableActor actor,long next,long applied,long hpAfter) {
        requireResourceBalance(actor);
        var result=actor.equipmentResources.damage(next,applied);
        if(result.vitals().hp()!=hpAfter)throw invalid();
        return result;
    }
    private ModernVitalBalance.State planRespawn(MutableActor actor,long next) {
        requireResourceBalance(actor);
        return actor.equipmentResources.respawn(next,actor.loadout.combatant().maxHp(),0);
    }
    private EquipmentResources resourceProof(MutableActor actor) {
        requireResourceBalance(actor);
        return new EquipmentResources(actor.prepared.scope(),preparation.matchId(),preparation.generation(),
                actor.entityId,actor.deathRevision,actor.equipmentResources);
    }
    private ModernEquipmentHistory.State equipmentHistory(MutableActor actor) {
        requireResourceBalance(actor);
        var world=new ModernTargetHistory.World(preparation.snapshot().summary().id(),preparation.matchId(),preparation.generation());
        var identity=new ModernEquipmentHistory.Identity(world,actor.prepared.scope(),new ModernTargetHistory.Participant(actor.entityId,actor.member.identity()));
        var state=new ModernEquipmentHistory.State(identity,revision,actor.equipmentResources,actor.spellHistory,actor.itemHistory,actor.targetHistory,actor.playerHealing);
        if(!state.profile().equals(actor.loadout.historyProfile(actor.spellHistory.keySet(),actor.itemHistory!=null)))throw invalid();
        if(actor.playerHealing!=null&&actor.playerHealing.outgoing().spending().current().cost()!=ModernPlayerHealing.qualify(actor.loadout.fighter()).mpCost())throw invalid();
        return state;
    }
    /** Owner-bound observation for the eventual negotiated envelope. */
    synchronized ModernEquipmentHistory.State equipmentHistory(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation) {
        var actor=requireActor(owner,scope,matchId,generation);
        return guarded(()->{advance();return equipmentHistory(actor);},()->{throw new IllegalStateException("equipment history world retired");});
    }
    synchronized ModernSpellRuntime.Snapshot spellRuntime(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation) {
        var actor=requireActor(owner,scope,matchId,generation);
        return guarded(()->{advance();return new ModernSpellRuntime.Snapshot(equipmentHistory(actor),actor.spellRuntime);},
                ()->{throw new IllegalStateException("spell runtime world retired");});
    }
    private ModernEquipmentHistory.State planEquipmentHistory(MutableActor actor,ModernBattleLoadout candidate) {
        return equipmentHistory(actor).reprofile(Math.incrementExact(revision),candidate.historyProfile(actor.spellHistory.keySet(),actor.itemHistory!=null),
                playerHealingEnabled?ModernPlayerHealing.qualify(candidate.fighter()).mpCost():null);
    }
    /** Internal owner-scoped observation, not registered as a network handler. */
    synchronized EquipmentResources equipmentResources(TournamentPresence.Owner owner, ModernRooms.Scope scope,
            long matchId, long generation) {
        var actor = requireActor(owner,scope,matchId,generation);
        return guarded(() -> { advance(); return resourceProof(actor); },
                () -> { throw new IllegalStateException("equipment resource world retired"); });
    }

    /** Internal transaction seam, NOT exposed by protocol <= 34. Only the base
     * physical profile is admitted until spell/item/weight/life receipts and the
     * negotiated client follower are implemented. No caller may enable a wire
     * capability merely because this transaction succeeds. */
    synchronized EquipmentChange beginEquipmentChange(TournamentPresence.Owner owner, ModernRooms.Scope scope,
            long matchId, long generation) {
        var actor = requireActor(owner, scope, matchId, generation);
        if (!baseEquipmentResources())
            throw new IllegalStateException("equipment live projections not qualified");
        return guarded(() -> {
            advance();
            if (phase != Phase.ACTIVE || actor.hp == 0 || actor.equipmentChange != null)
                throw new IllegalStateException("equipment world reservation unavailable");
            requireResourceBalance(actor);
            var change = new EquipmentChange(actor, false);
            actor.equipmentChange = change;
            return change;
        }, () -> { throw new IllegalStateException("equipment world retired"); });
    }

    /** Canonical-only transaction admission. The v41 service retains the
     * caller's observed life, then owner-projects the exact capture.
     * This does not enable the legacy SET handler or its fixed-cost DTOs. */
    synchronized EquipmentChange beginCanonicalEquipmentChange(TournamentPresence.Owner owner,
            ModernBattleCommandLife.Proof proof) {
        var admission=admitCanonicalEquipmentChange(owner,proof);
        if(admission.change()==null)throw new IllegalStateException("equipment world reservation unavailable");
        return admission.change();
    }
    record EquipmentAdmission(ModernBattleEquipmentProtocol.Reason reason,EquipmentChange change,
                              ModernBattleObservation.Capture observation) {
        EquipmentAdmission {
            Objects.requireNonNull(reason);Objects.requireNonNull(observation);
            if((reason==ModernBattleEquipmentProtocol.Reason.NONE)!=(change!=null))throw invalid();
        }
    }
    synchronized EquipmentAdmission admitCanonicalEquipmentChange(TournamentPresence.Owner owner,
            ModernBattleCommandLife.Proof proof) {
        Objects.requireNonNull(proof);
        var actor=requireActor(owner,proof.player(),proof.world().matchId(),proof.world().generation());
        if(proof.world().roomId()!=preparation.snapshot().summary().id()||proof.entity()!=actor.entityId||proof.revision()>revision)
            throw new SecurityException("equipment command identity mismatch");
        return guarded(()->{
            advance();
            var rejected=commandLifeRejection(actor,proof,ModernBattleActionProtocol.NO_TARGET);
            if(rejected!=ModernBattleCommandLife.Rejection.NONE)
                return new EquipmentAdmission(ModernBattleEquipmentProtocol.Reason.valueOf(rejected.name()),null,capture());
            // v41 has only a life proof and one equipment version. It cannot
            // authorize full-VIP SET even when saved/runtime happen to match.
            if(vipEnabled||!combatEnabled||actor.loadout==null||actor.loadout.fighter()==null)
                return new EquipmentAdmission(ModernBattleEquipmentProtocol.Reason.UNAVAILABLE,null,capture());
            if(actor.equipmentChange!=null)
                return new EquipmentAdmission(ModernBattleEquipmentProtocol.Reason.BUSY,null,capture());
            requireResourceBalance(actor);
            var observation=capture();
            var change=new EquipmentChange(actor,true);actor.equipmentChange=change;
            return new EquipmentAdmission(ModernBattleEquipmentProtocol.Reason.NONE,change,observation);
        },()->new EquipmentAdmission(ModernBattleEquipmentProtocol.Reason.NOT_ACTIVE,null,capture()));
    }

    /** Internal full-VIP admission, not a public SET handler. Tick expiry first,
     * then compare the caller's original observation under this engine monitor.
     * The reservation retains this exact loadout and rechecks it before durable
     * admission; a later expiry cannot be papered over with a fresh proof.
     */
    synchronized ModernBattleVipCommandState.EquipmentAdmission admitVipEquipmentChange(
            TournamentPresence.Owner owner,ModernBattleVipCommandState.Proof proof) {
        Objects.requireNonNull(proof);var life=proof.life();
        var actor=requireActor(owner,life.player(),life.world().matchId(),life.world().generation());
        if(life.world().roomId()!=preparation.snapshot().summary().id()||life.entity()!=actor.entityId||life.revision()>revision)
            throw new SecurityException("VIP equipment command identity mismatch");
        return guarded(()->{
            advance();
            var rejected=commandLifeRejection(actor,life,ModernBattleActionProtocol.NO_TARGET);
            if(rejected!=ModernBattleCommandLife.Rejection.NONE)
                return new ModernBattleVipCommandState.EquipmentAdmission(
                        ModernBattleVipCommandState.Rejection.valueOf(rejected.name()),null,capture());
            if(!vipEnabled||!combatEnabled||actor.loadout==null||actor.loadout.fighter()==null||actor.loadout.fighter().vip()==null)
                return new ModernBattleVipCommandState.EquipmentAdmission(ModernBattleVipCommandState.Rejection.UNAVAILABLE,null,capture());
            var fighter=actor.loadout.fighter();var ledger=fighter.vip().state().ledger();
            var current=new ModernBattleVipObservation.Versions(ledger.equipmentRevision(),fighter.profile().equipmentRevision(),ledger.sequence());
            var state=ModernBattleVipCommandState.compare(proof.versions(),current);
            if(state!=ModernBattleVipCommandState.Rejection.NONE)
                return new ModernBattleVipCommandState.EquipmentAdmission(state,null,capture());
            if(actor.equipmentChange!=null)
                return new ModernBattleVipCommandState.EquipmentAdmission(ModernBattleVipCommandState.Rejection.BUSY,null,capture());
            requireResourceBalance(actor);
            var observation=capture();var change=new EquipmentChange(actor,true);actor.equipmentChange=change;
            return new ModernBattleVipCommandState.EquipmentAdmission(ModernBattleVipCommandState.Rejection.NONE,change,observation);
        },()->new ModernBattleVipCommandState.EquipmentAdmission(ModernBattleVipCommandState.Rejection.NOT_ACTIVE,null,capture()));
    }

    /** One opaque engine-owned reservation. Qualification and durable I/O run
     * outside this engine; combat continues during force/rename. Publication
     * clips CURRENT HP/MP and never restores a captured snapshot or its clocks.
     * Lock order: store I/O gate -> engine -> preparation Guard. No engine path
     * calls the store/lease or performs I/O while holding these monitors. */
    final class EquipmentChange implements AutoCloseable {
        private final MutableActor actor;
        private final ModernBattleLoadout previousLoadout;
        private final ModernPhysicalCombat.Fighter previous;
        private final long deathAtReservation;
        private final long lifeAtReservation;
        private final long revisionAtReservation;
        private final boolean canonical;
        private final long capturedHp, capturedMp;
        private ModernPhysicalCombat.Fighter candidate;
        private ModernVipCharacterState.Candidate vipCandidate;
        private ModernBattleLoadout candidateLoadout;
        private ModernEquipmentOverrideStore.Stored expectedStored;
        private boolean admitted, done;
        private EquipmentPublication publication;
        private ModernBattleObservation.Capture observation;

        private EquipmentChange(MutableActor actor, boolean canonical) {
            this.actor = actor; previousLoadout = actor.loadout; previous = previousLoadout.fighter();
            deathAtReservation = actor.deathRevision; capturedHp = actor.hp; capturedMp = actor.mp;
            lifeAtReservation=resourceLife(actor);revisionAtReservation=revision;this.canonical=canonical;
        }
        boolean armorProtectionEnabled(){return previous.armorProtection()!=null;}
        ModernVipCharacterState vipState() {
            synchronized(ModernBattleEngine.this) {
                if(!eligible()||previous.vip()==null)throw invalid();return previous.vip().state();
            }
        }
        /** Selection lease is inspected OUTSIDE the engine monitor. The
         * immutable reservation then binds the complete original command proof;
         * numeric source/epoch equality alone is not authority. */
        void requireVipRequest(ModernBattleVipCommandState.Proof proof,ModernEquipmentLease.Operation operation) {
            Objects.requireNonNull(proof);Objects.requireNonNull(operation);
            var scope=actor.prepared.scope();var life=proof.life();
            if(!operation.matchesKey(new TournamentPresence.CharacterKey(scope.sourceId(),scope.objectId()))
                    ||!operation.matchesSelection(actor.prepared.owner(),scope.epoch()))
                throw new SecurityException("VIP equipment selection lease mismatch");
            synchronized(ModernBattleEngine.this) {
                if(!canonical||previous.vip()==null||!eligible())throw new IllegalStateException("VIP equipment reservation retired");
                var ledger=previous.vip().state().ledger();
                if(!life.player().equals(scope)||life.world().roomId()!=preparation.snapshot().summary().id()
                        ||life.world().matchId()!=preparation.matchId()||life.world().generation()!=preparation.generation()
                        ||life.entity()!=actor.entityId||life.life()!=Math.incrementExact(lifeAtReservation)
                        ||life.revision()>revisionAtReservation||ModernBattleVipCommandState.compare(proof.versions(),
                            new ModernBattleVipObservation.Versions(ledger.equipmentRevision(),previous.profile().equipmentRevision(),ledger.sequence()))
                            !=ModernBattleVipCommandState.Rejection.NONE)
                    throw new SecurityException("VIP equipment reservation proof mismatch");
            }
        }
        void requireVipSource(ModernEquipmentAuthority.PhysicalLoaded loaded,ModernEquipmentReconciliation.ExpectedBinding binding,
                java.util.Optional<ModernEquipmentOverrideStore.Stored> stored,long now) {
            var state=vipState();state.requireSource(loaded.source(),binding,stored,now);
            if((loaded.protection()!=null)!=(previous.armorProtection()!=null))throw invalid();
            if(loaded.protection()!=null&&!previous.armorProtection().sameFacts(
                    ModernArmorProtectionLoadout.qualify(loaded.protection(),previous.profile())))
                throw new IllegalStateException("VIP armor source requires re-admission");
            synchronized(ModernBattleEngine.this){if(!eligible())throw new IllegalStateException("VIP equipment reservation retired");}
        }
        /** The authority must perform the proposal's exact expected/next store
         * CAS. No I/O or SQL mutation is performed by this engine reservation. */
        void prepareVip(ModernVipCharacterState.Candidate value,long now) {
            Objects.requireNonNull(value);
            if(previous.vip()==null||!value.expectedStore().equals(previous.vip().state().stored())
                    ||value.nextStore().isEmpty()||value.nextStore().orElseThrow().revision()!=Math.incrementExact(previous.vip().state().ledger().equipmentRevision()))throw invalid();
            var next=vipFighter(value,previous,now,capturedHp,capturedMp);
            var loadout=previousLoadout.replacement(next);
            // Fighter.supported is the melee calculator's qualification. A
            // bow-to-unarmed (or reverse) save must check the enabled runtime
            // attack AND defense projections, including qualified ranged facts.
            if(!previousLoadout.combatant().physicalSupported()||!loadout.combatant().physicalSupported()
                    ||!previousLoadout.combatant().physicalDefenseSupported()||!loadout.combatant().physicalDefenseSupported())throw invalid();
            synchronized(ModernBattleEngine.this) {
                if(done||admitted||candidate!=null||!eligible())throw invalid();
                vipCandidate=value;candidate=next;candidateLoadout=loadout;expectedStored=value.nextStore().orElseThrow();
            }
        }
        void requireRequest(ModernEquipmentAuthority.Request request, ModernEquipmentLease.Operation operation) {
            var scope = actor.prepared.scope();
            if (request.action() != ModernEquipmentAuthority.SET || request.sourceId() != scope.sourceId()
                    || request.objectId() != scope.objectId() || request.epoch() != scope.epoch()
                    || !operation.matchesSelection(actor.prepared.owner(), scope.epoch()))
                throw new SecurityException("equipment world request mismatch");
        }
        void requireSource(ModernPhysicalCombat.Source source, ModernEquipmentReconciliation.ExpectedBinding binding,
                java.util.Optional<ModernEquipmentOverrideStore.Stored> stored, long now) {
            requireSource(source,binding,stored,now,null);
        }
        void requireSource(ModernPhysicalCombat.Source source, ModernEquipmentReconciliation.ExpectedBinding binding,
                java.util.Optional<ModernEquipmentOverrideStore.Stored> stored, long now,ModernArmorProtectionCatalog.BoundSource protection) {
            if(previous.vip()!=null)throw new IllegalStateException("full VIP source requires VIP equipment authority");
            if(protection!=null&&protection.physical()!=source)throw invalid();
            if (!previous.profile().source().equals(source.input().equipment().source())
                    || !previous.catalogDatabase().equals(source.growth().database())
                    || stored.map(ModernEquipmentOverrideStore.Stored::revision).orElse(0L) != previous.profile().equipmentRevision()
                    || now < previous.profile().evaluatedAtMillis())
                throw new IllegalStateException("equipment world source requires reconciliation");
            // A same SQL row does not prove an unchanged catalog/growth table.
            // Pin the currently active base projection before considering SET,
            // including no-op intents; do not silently refresh it as a side effect.
            var current = protection==null?ModernPhysicalCombat.qualify(source, binding, stored, now)
                    :ModernPhysicalCombat.qualifyProtection(protection,binding,stored,now);
            var old = previous.profile(); var fresh = current.profile(); var e = fresh.effective();
            var normalized = new ModernCombatProfile.Effective(e.maximumHp(), e.maximumMp(),
                    old.effective().initialHp(), old.effective().initialMp(), e.armorClass(), e.strength(),
                    e.dexterity(), e.constitution(), e.intelligence(), e.wisdom(), e.charisma());
            if (!normalized.equals(old.effective()) || !current.values().equals(previous.values())
                    || !previousLoadout.matchesDerivedFacts(current)
                    || current.attackRange() != previous.attackRange() || !fresh.weapon().equals(old.weapon())
                    || !fresh.sourceBaseline().equals(old.sourceBaseline())
                    || !fresh.equipmentEvaluation().equals(old.equipmentEvaluation())
                    || !fresh.equipped().equals(old.equipped()) || !fresh.sourceLifetimes().equals(old.sourceLifetimes())
                    || fresh.validUntilMillis() != old.validUntilMillis())
                throw new IllegalStateException("equipment world catalog requires reconciliation");
            synchronized (ModernBattleEngine.this) {
                if (!eligible()) throw new IllegalStateException("equipment world reservation retired");
            }
        }
        void prepare(ModernPhysicalCombat.Source source, ModernEquipmentReconciliation.ExpectedBinding binding,
                ModernEquipmentOverrideStore.Stored stored, long now) throws ModernPhysicalCombat.CombatException {
            prepare(source,binding,stored,now,null);
        }
        void prepare(ModernPhysicalCombat.Source source, ModernEquipmentReconciliation.ExpectedBinding binding,
                ModernEquipmentOverrideStore.Stored stored, long now,ModernArmorProtectionCatalog.BoundSource protection) throws ModernPhysicalCombat.CombatException {
            if(previous.vip()!=null)throw new IllegalStateException("full VIP equipment requires a full VIP candidate");
            if(protection!=null&&protection.physical()!=source)throw invalid();
            // All evaluation (and overflow/unsupported checks) precedes admission.
            var next = protection==null?ModernPhysicalCombat.qualifyEquipmentChange(source, binding, java.util.Optional.of(stored),
                    now, previous, capturedHp, capturedMp):ModernPhysicalCombat.qualifyProtectionEquipmentChange(protection,binding,
                    java.util.Optional.of(stored),now,previous,capturedHp,capturedMp);
            if (!previous.supported() || !next.supported())
                throw new IllegalStateException("equipment physical replacement unsupported");
            var nextLoadout = previousLoadout.replacement(next);
            synchronized (ModernBattleEngine.this) {
                if (done || admitted || candidate != null || !eligible())
                    throw new IllegalStateException("equipment candidate retired");
                candidate = next; candidateLoadout = nextLoadout; expectedStored = stored;
            }
        }
        private boolean eligible() {
            return !closed && !done && actor.equipmentChange == this
                    && owners.get(actor.prepared.owner()) == actor && actor.loadout == previousLoadout
                    && phase == Phase.ACTIVE && participating(actor) && actor.deathRevision == deathAtReservation
                    && resourceLife(actor)==lifeAtReservation && actor.hp > 0;
        }
        private ModernInventoryWeight.Snapshot planWeight() {
            if(actor.weight==null)return null;
            return candidateLoadout.reweight(actor.itemUse.stocks(),actor.ammo==null?List.of():actor.ammo.stocks(),actor.weight.units(),materialStocks(actor));
        }
        private void requireClippingCapacity() {
            // Healing/regen may raise live values after admission. Reserve the
            // worst possible clipping delta under the still-current maxima;
            // no second gear transaction can change these counters meanwhile.
            var f=actor.equipmentResources.flows();var old=previousLoadout.combatant();var next=candidateLoadout.combatant();
            Math.addExact(f.hpClipped(),Math.max(0,old.maxHp()-next.maxHp()));
            Math.addExact(f.mpClipped(),Math.max(0,old.maxMp()-next.maxMp()));
        }
        boolean admit() {
            synchronized (ModernBattleEngine.this) {
                if (admitted || candidate == null || !eligible()) return false;
                return guarded(() -> {
                    advance();
                    if (!eligible() || revision == Long.MAX_VALUE || !candidateCurrent()) return false;
                    requireResourceBalance(actor);
                    // All replacement projections precede irreversible rename.
                    // Combat/consumption/regen remain free to progress in I/O.
                    try { requireClippingCapacity();planEquipmentHistory(actor,candidateLoadout);planWeight();if(vipCandidate!=null)Math.incrementExact(vipMutation); }
                    catch (ArithmeticException | IllegalArgumentException rejected) { return false; }
                    admitted = true; return true;
                }, () -> false);
            }
        }
        EquipmentPublication confirmUnchanged() {
            synchronized (ModernBattleEngine.this) {
                if (admitted || !eligible()) throw new IllegalStateException("equipment no-op retired");
                return guarded(() -> {
                    advance();
                    if (!eligible()) throw new IllegalStateException("equipment no-op retired");
                    publication = new EquipmentPublication(EquipmentDisposition.UNCHANGED, actor.entityId,
                            previous.vip()==null?previous.profile().equipmentRevision():previous.vip().state().ledger().equipmentRevision(),
                            revision, resourceProof(actor),equipmentHistory(actor));
                    if(canonical)observation=capture();
                    done = true; actor.equipmentChange = null; return publication;
                }, () -> { throw new IllegalStateException("equipment no-op retired"); });
            }
        }
        private boolean candidateCurrent() {
            return candidateLoadout.currentAt(lastObservedEpochMillis);
        }
        EquipmentPublication publish(ModernEquipmentOverrideStore.Commit committed) {
            synchronized (ModernBattleEngine.this) {
                if (done || !admitted || !committed.changed() || !expectedStored.equals(committed.stored()))
                    throw new IllegalStateException("equipment durable outcome requires reconciliation");
                // Terminal/closed worlds cannot receive a newer snapshot. A
                // successful rename remains saved for the next qualified match.
                if (closed) return finishPublication(EquipmentDisposition.PERSISTED_RETIRED);
                return guarded(() -> { advance(); return publishCurrent(); },
                        () -> finishPublication(EquipmentDisposition.PERSISTED_RETIRED));
            }
        }
        private EquipmentPublication publishCurrent() {
            var disposition = EquipmentDisposition.PERSISTED_RETIRED;
            if (!closed && phase == Phase.ACTIVE && preparation.isLive()
                    && actor.equipmentChange == this && owners.get(actor.prepared.owner()) == actor
                    && actor.loadout == previousLoadout && candidateCurrent() && participating(actor)
                    && resourceLife(actor)==lifeAtReservation) {
                // A death in this life is retained; a later incarnation must
                // not inherit a delayed command admitted for the previous one.
                requireResourceBalance(actor);
                if(vipCandidate!=null) {
                    // Rebuild with the latest damaged/dead vitals, not the
                    // preparation-time values. Every projection still precedes publication.
                    var live=vipFighter(vipCandidate,previous,lastObservedEpochMillis,actor.hp,actor.mp);
                    candidateLoadout=previousLoadout.replacement(live);candidate=live;
                }
                var history=planEquipmentHistory(actor,candidateLoadout);
                var weight=planWeight();
                var resources=history.resources();
                var vitals = resources.vitals();
                long mutation=vipCandidate==null?vipMutation:Math.incrementExact(vipMutation);
                nextRevision();
                actor.hp = vitals.hp(); actor.mp = vitals.mp();
                actor.loadout = candidateLoadout;
                vipMutation=mutation;
                actor.weight=weight;
                actor.equipmentResources = resources;
                actor.spellHistory=history.spells();actor.itemHistory=history.items();actor.targetHistory=history.targets();actor.playerHealing=history.healing();
                disposition = EquipmentDisposition.APPLIED;
            } else if (!closed && phase == Phase.ACTIVE) {
                // A durable write cannot masquerade as rollback while this
                // world continues with an unacknowledgeable old profile.
                abortInternal(AbortReason.SERVER_ABORT);
            }
            return finishPublication(disposition);
        }
        private EquipmentPublication finishPublication(EquipmentDisposition disposition) {
            publication = new EquipmentPublication(disposition, actor.entityId, expectedStored.revision(), revision,
                    disposition == EquipmentDisposition.APPLIED ? resourceProof(actor) : null,
                    disposition == EquipmentDisposition.APPLIED ? equipmentHistory(actor) : null);
            if(canonical&&!closed)observation=capture();
            done = true; if (actor.equipmentChange == this) actor.equipmentChange = null;
            return publication;
        }
        EquipmentPublication publication() { synchronized (ModernBattleEngine.this) { return publication; } }
        /** Exact post-publication private capture, never a later independent
         * READ. Null only when no publication occurred or the engine closed. */
        ModernBattleObservation.Capture observation() { synchronized(ModernBattleEngine.this){return observation;} }
        /** A denied/conflicting CAS made no durable publication. Capture its
         * current recovery state once, without fabricating an unchanged ACK. */
        void captureNoMutation() {
            synchronized(ModernBattleEngine.this) {
                if(!canonical)return;
                if(done||admitted||publication!=null)throw new IllegalStateException("equipment rejection already resolved");
                requireOpen();
                observation=guarded(()->{advance();return capture();},ModernBattleEngine.this::capture);
                done=true;if(actor.equipmentChange==this)actor.equipmentChange=null;
            }
        }
        @Override public void close() {
            synchronized (ModernBattleEngine.this) {
                if (done) return;
                // Any abandoned post-admission outcome is uncertain, not an
                // inverse SET. Preserve durable evidence; stop the live match.
                if (admitted && !closed) abortInternal(AbortReason.SERVER_ABORT);
                done = true; if (actor.equipmentChange == this) actor.equipmentChange = null;
            }
        }
        @Override public String toString() { return "ModernWorldEquipmentChange[redacted]"; }
    }
    /** A server-owned bounded scheduler may expire an idle match without client polling. */
    synchronized Snapshot tick() {
        requireOpen(); return guarded(() -> { advance(); return current(); }, this::current);
    }
    synchronized Snapshot ackLoaded(TournamentPresence.Owner owner, ModernRooms.Scope scope,
                                    long matchId, long generation, String manifestDigest) {
        return ModernLegacyBattleProjection.from(ackLoadedObservation(owner,scope,matchId,generation,manifestDigest));
    }
    synchronized ModernBattleObservation.Capture ackLoadedObservation(TournamentPresence.Owner owner,ModernRooms.Scope scope,
            long matchId,long generation,String manifestDigest) {
        MutableActor actor = requireActor(owner, scope, matchId, generation);
        if (!map.digest().equals(manifestDigest)) throw new SecurityException("world manifest mismatch");
        return guarded(() -> {
            long now = advance();
            if (phase == Phase.ABORTED || actor.loaded) return capture();
            if (phase != Phase.LOADING) throw new IllegalStateException("invalid world loading state");
            nextRevision(); actor.loaded = true;
            if (actors.stream().allMatch(value -> value.loaded)) {
                countdownStartedNanos = now;
                if (limits.countdownNanos() == 0) { phase = Phase.ACTIVE; activeStartedNanos = now;activeClockStarted=true;activateRace(); }
                else phase = Phase.COUNTDOWN;
            }
            return capture();
        }, this::capture);
    }
    synchronized MoveResult move(TournamentPresence.Owner owner, ModernRooms.Scope scope, long matchId,
                                 long generation, long sequence, int fromX, int fromY, int heading) {
        MutableActor actor = requireActor(owner, scope, matchId, generation);
        if (sequence < 1 || heading < 0 || heading > 7) throw invalid();
        return guarded(() -> {
            long now = advance();
            if (phase == Phase.ABORTED) return moved(MoveStatus.ABORTED);
            if (phase == Phase.FINISHED) return moved(MoveStatus.NOT_ACTIVE);
            // Race-inactive actors are immutable receipts, not a new denial event.
            // Keep the established sequence-consuming denials for every older mode.
            if(raceState!=null&&(phase!=Phase.ACTIVE||!raceState.entrant(actor.entityId).runner().competing()))return moved(MoveStatus.NOT_ACTIVE);
            if (actor.sequence == Long.MAX_VALUE || sequence != actor.sequence + 1)
                throw new SecurityException("world movement sequence mismatch");
            MoveStatus status;
            ModernWorldMap.Cell destination = actor.position;
            if (phase != Phase.ACTIVE || combatEnabled && actor.hp == 0) status = MoveStatus.NOT_ACTIVE;
            else if (fromX != actor.position.x() || fromY != actor.position.y()) status = MoveStatus.STALE_POSITION;
            else if (actor.moved && now - actor.lastMoveNanos < pace(actor,ModernTimedEffects.Action.MOVE)
                    || actor.attacked && now - actor.lastAttackNanos < pace(actor,ModernTimedEffects.Action.ATTACK)
                    || castCooling(actor, now)) status = MoveStatus.TOO_FAST;
            else {
                destination = new ModernWorldMap.Cell(Math.addExact(fromX, DX[heading]), Math.addExact(fromY, DY[heading]));
                if (!map.canStep(actor.position, destination)||siegeSpace!=null&&!siegeSpace.step(actor.position,destination,siegeState.open())) status = MoveStatus.BLOCKED;
                else {
                    ModernWorldMap.Cell checked = destination;
                    status = objectives.stream().anyMatch(o->o.cell().equals(checked))||questObjectiveOccupies(checked,actor.member.team())||npcOccupies(checked,actor.member.team())||actors.stream().anyMatch(value -> value != actor && participating(value)&&sameForce(value.member.team(),actor.member.team())&&value.position.equals(checked)
                            &&(raceState==null||raceState.entrant(value.entityId).runner().competing()))
                            ? MoveStatus.OCCUPIED : MoveStatus.MOVED;
                    if(status==MoveStatus.MOVED&&npcRoster!=null&&!npcStepOpen(actor.position,checked,actor.member.team()))status=MoveStatus.BLOCKED;
                }
            }
            // A valid, scoped request consumes exactly one actor sequence even
            // when rejected by world state/geometry. Both are visible in this revision.
            if (!equipmentCurrent()) return moved(MoveStatus.ABORTED);
            ModernPetRace.State racePlan=raceState==null?null:ModernPetRace.moved(raceState,matchId,generation,actor.entityId,sequence,actor.position,
                    status==MoveStatus.MOVED?destination:actor.position,status==MoveStatus.MOVED,phase==Phase.ACTIVE?Math.max(0,now-activeStartedNanos):0,Math.incrementExact(revision));
            nextRevision(); actor.sequence = sequence;
            if(racePlan!=null)raceState=racePlan;
            if (status == MoveStatus.MOVED) {
                actor.position = destination; actor.heading = heading; actor.lastMoveNanos = now; actor.moved = true;
                if(regenEnabled)actor.regenClock=actor.regenClock.noteMove();
            }
            return moved(status);
        }, () -> moved(MoveStatus.ABORTED));
    }

    /** One trusted authority: positions, cooldown, HP and event revisions commit together. */
    synchronized AttackResult attack(TournamentPresence.Owner owner, ModernRooms.Scope scope, long matchId,
                                      long generation, long sequence, long targetEntityId) {
        MutableActor actor = requireActor(owner, scope, matchId, generation);
        if(npcRoster!=null)return guarded(()->{advance();return attacked(AttackStatus.NOT_ACTIVE);},()->attacked(AttackStatus.NOT_ACTIVE));
        if(honorConfig!=null&&targetEntityId>actors.size())return attackObjective(actor,sequence,targetEntityId);
        if (sequence < 1 || targetEntityId < 1 || targetEntityId > actors.size() || targetEntityId == actor.entityId)
            throw new SecurityException("combat target or sequence invalid");
        MutableActor target = actors.get((int) targetEntityId - 1);
        return guarded(() -> {
            long now = advance();
            if (!combatEnabled || phase == Phase.ABORTED || phase == Phase.FINISHED) return attacked(AttackStatus.NOT_ACTIVE);
            if (actor.attackSequence == Long.MAX_VALUE || sequence != actor.attackSequence + 1)
                throw new SecurityException("combat attack sequence mismatch");
            AttackStatus status;
            ModernPhysicalCombat.Outcome outcome = null;
            ArmorHit armorHit=null;
            ArmorStamp shotCaster=null,shotTarget=null;
            boolean resolved = false;
            long timedRevision=actor.buffs.revision();
            long targetRegenRevision=target.regen.hpRevision();
            if(regenEnabled&&phase==Phase.ACTIVE&&actor.hp>0&&actor.weight.view().permitsAttackCast())actor.regenClock=actor.regenClock.noteAttack();
            if (phase != Phase.ACTIVE) status = AttackStatus.NOT_ACTIVE;
            else if (actor.hp == 0) status = AttackStatus.DEAD;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())status=AttackStatus.TOO_HEAVY;
            else if (target.hp == 0) status = AttackStatus.TARGET_DEAD;
            else if (TournamentRoomPolicy.rules(preparation.snapshot().summary().config().mode(), preparation.snapshot().summary().config().mapId()).teamBased()
                    && actor.member.team() == target.member.team()) status = AttackStatus.SAME_TEAM;
            else if (!actor.loadout.combatant().physicalSupported() || !target.loadout.combatant().physicalDefenseSupported()) status = AttackStatus.UNSUPPORTED;
            else if (actor.attacked && now - actor.lastAttackNanos < pace(actor,ModernTimedEffects.Action.ATTACK)
                    || actor.moved && now - actor.lastMoveNanos < pace(actor,ModernTimedEffects.Action.MOVE)
                    || castCooling(actor, now)) status = AttackStatus.TOO_FAST;
            else if (distance(actor.position, target.position) > actor.loadout.combatant().attackRange()) status = AttackStatus.OUT_OF_RANGE;
            else if (!strikeLineOpen(actor.position, target.position)) status = AttackStatus.BLOCKED;
            else if(actor.loadout.combatant().bow()&&(actor.ammo==null||actor.ammo.selected()==null||actor.ammo.selected().remaining()==0))status=AttackStatus.NO_AMMO;
            else {
                try {
                    resolved = true;
                    // Even without an armor proc, arrow identity, positions,
                    // lives and resources must still be the ones used by RNG.
                    if(actor.loadout.combatant().bow()){shotCaster=ArmorStamp.of(actor);shotTarget=ArmorStamp.of(target);}
                    var guard=armorGuard(target,actor,now);
                    var damage=ModernCombatant.resolveProtectedPhysical(actor.loadout.combatant(), target.loadout.combatant(), target.hp, random,
                            actor.weight==null?0:actor.weight.view().hitPenalty(),actor.loadout.combatant().bow()?actor.ammo.selected().facts():null,protection(guard));
                    outcome=damage.outcome();armorHit=planArmorHit(guard,damage.impact());
                    status = AttackStatus.ATTACKED;
                } catch (IllegalArgumentException|ArithmeticException failure) {
                    outcome=null;
                    status = failure instanceof ModernPhysicalCombat.CombatException e&&e.failure()==ModernPhysicalCombat.Failure.UNSUPPORTED ? AttackStatus.UNSUPPORTED : AttackStatus.NUMERIC;
                }
            }
            // No HP/clock/heading/event mutation occurs before resolver validation.
            // Denial consumes its scoped sequence, but never starts a visual attack.
            if (resolved) {
                now = advance();
                if (phase != Phase.ACTIVE) return attacked(AttackStatus.NOT_ACTIVE);
                if(actor.buffs.revision()!=timedRevision){status=AttackStatus.STALE_EFFECT;outcome=null;}
                else if(regenEnabled&&target.regen.hpRevision()!=targetRegenRevision){status=AttackStatus.STALE_REGEN;outcome=null;}
            }
            if (!equipmentCurrent()) return attacked(AttackStatus.NOT_ACTIVE);
            ArmorProtectionView nextArmor=null;
            if(outcome!=null&&armorHit!=null) {
                now=advanceTemporal();if(phase!=Phase.ACTIVE)return attacked(AttackStatus.NOT_ACTIVE);
                try{nextArmor=acceptArmorHit(armorHit,now);}catch(IllegalArgumentException|ArithmeticException failure){outcome=null;status=AttackStatus.STALE_EFFECT;}
            }
            // Nested commands may have advanced the attack sequence while the
            // resolver was running. Never rewind it when this old call returns.
            if(resolved&&(actor.attackSequence==Long.MAX_VALUE||sequence!=actor.attackSequence+1))
                throw new SecurityException("combat attack sequence changed during calculation");
            if(outcome!=null&&shotCaster!=null&&(!shotCaster.current()||!shotTarget.current())) {
                outcome=null;status=AttackStatus.STALE_EFFECT;
            }
            ModernAmmo.State nextAmmo=null;ModernInventoryWeight.Snapshot nextWeight=null;
            ModernBattlePresentation.Shot shot=null;
            ModernRegenProtocol.State targetRegen=target.regen;
            ModernHonor.Life nextLife=target.life;
            ModernSiege.Life nextSiegeLife=target.siegeLife;
            if(regenEnabled&&outcome!=null)try{targetRegen=target.regen.damaged(outcome.appliedDamage());}
                catch(ArithmeticException failure){outcome=null;status=AttackStatus.NUMERIC;}
            if(honorConfig!=null&&outcome!=null&&outcome.death())try{nextLife=target.life.died(Math.incrementExact(revision),elapsedMillis(now),target.position);}
                catch(ArithmeticException|IllegalArgumentException failure){outcome=null;status=AttackStatus.NUMERIC;}
            if(siegeState!=null&&outcome!=null&&outcome.death())try{nextSiegeLife=target.siegeLife.died(Math.incrementExact(revision),elapsedMillis(now),siegeState.config().respawnMillis());}
                catch(ArithmeticException|IllegalArgumentException failure){outcome=null;status=AttackStatus.NUMERIC;}
            if(outcome!=null&&actor.loadout.combatant().bow()) {
                try{nextAmmo=actor.ammo.shot(Math.incrementExact(revision),sequence,target.entityId,actor.position.x(),actor.position.y(),target.position.x(),target.position.y(),outcome.hit());
                    nextWeight=actor.loadout.reweight(actor.itemUse.stocks(),nextAmmo.stocks(),actor.weight.units(),materialStocks(actor));
                    shot=shot(actor,nextAmmo,now,new ModernTargetHistory.Participant(target.entityId,target.member.identity()),Math.incrementExact(resourceLife(target)));}
                catch(ArithmeticException|IllegalArgumentException failure){outcome=null;status=AttackStatus.NUMERIC;}
            }
            ModernVitalBalance.State nextResources=target.equipmentResources;
            if(outcome!=null && nextResources!=null) {
                try { nextResources=planDamage(target,Math.incrementExact(revision),outcome.appliedDamage(),outcome.remainingHp()); }
                catch(ArithmeticException | IllegalArgumentException failure){outcome=null;status=AttackStatus.NUMERIC;}
            }
            nextRevision(); actor.attackSequence = sequence;
            if (outcome != null) {
                commitArmorHit(target,nextArmor);
                if(nextAmmo!=null){actor.ammo=nextAmmo;actor.weight=nextWeight;actor.presentation=actor.presentation.withShot(shot);}
                actor.attacked = true; actor.lastAttackNanos = now; actor.attackRevision = revision; actor.targetEntityId = target.entityId;
                target.hp = outcome.remainingHp();
                target.equipmentResources=nextResources;
                if(regenEnabled)target.regen=targetRegen;
                if (outcome.appliedDamage() > 0) target.hurtRevision = revision;
                if (outcome.death()) { target.deathRevision = revision;clearBuffs(target,now,revision);if(honorConfig!=null)target.life=nextLife;if(siegeState!=null)target.siegeLife=nextSiegeLife; }
                if (outcome.death() && settlementEnabled&&honorConfig==null) finishElimination();
            }
            return attacked(status);
        }, () -> attacked(AttackStatus.NOT_ACTIVE));
    }
    /** Typed objectives never alias a PC/BOT entity, NPC spawn or Honor tower. */
    synchronized AttackResult attackSiegeTower(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation,
                                               long sequence,ModernSiege.Target target) {
        var actor=requireActor(owner,scope,matchId,generation);
        if(siegeState==null||target==null||!siegeState.scope().equals(target.scope())||sequence<1)throw new SecurityException("siege target scope mismatch");
        return guarded(()->{
            long now=advance();if(phase==Phase.FINISHED||phase==Phase.ABORTED)return attacked(AttackStatus.NOT_ACTIVE);
            if(actor.attackSequence==Long.MAX_VALUE||sequence!=actor.attackSequence+1)throw new SecurityException("siege attack sequence mismatch");
            var before=siegeState;var objective=before.objectives().get(target.slot().ordinal());
            AttackStatus status;ModernPhysicalCombat.Outcome outcome=null;
            if(phase!=Phase.ACTIVE)status=AttackStatus.NOT_ACTIVE;
            else if(actor.hp==0)status=AttackStatus.DEAD;
            else if(actor.member.team()!=before.attacker())status=AttackStatus.SAME_TEAM;
            else if(objective.hp()==0)status=AttackStatus.TARGET_DEAD;
            else if(!before.attackable(actor.member.team(),target.slot()))status=AttackStatus.BLOCKED;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())status=AttackStatus.TOO_HEAVY;
            else if(!actor.loadout.combatant().physicalSupported())status=AttackStatus.UNSUPPORTED;
            else if(actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)||castCooling(actor,now))status=AttackStatus.TOO_FAST;
            else if(distance(actor.position,objective.tower().cell())>actor.loadout.combatant().attackRange())status=AttackStatus.OUT_OF_RANGE;
            else if(!strikeLineOpen(actor.position,objective.tower().cell()))status=AttackStatus.BLOCKED;
            else {
                try{outcome=actor.loadout.combatant().attackObjective(objective.hp(),objective.tower().maximumHp(),random,actor.weight==null?0:actor.weight.view().hitPenalty(),null);status=AttackStatus.ATTACKED;}
                catch(ModernPhysicalCombat.CombatException failure){status=failure.failure()==ModernPhysicalCombat.Failure.UNSUPPORTED?AttackStatus.UNSUPPORTED:AttackStatus.NUMERIC;}
                now=advance();if(phase!=Phase.ACTIVE||!equipmentCurrent())return attacked(AttackStatus.NOT_ACTIVE);
                // A reentrant resolver or delayed clock cannot spend a stale
                // target, lifecycle, scope or shared attack sequence.
                if(siegeState!=before||actor.attackSequence!=sequence-1)throw new IllegalStateException("siege authority changed during resolution");
            }
            if(!equipmentCurrent())return attacked(AttackStatus.NOT_ACTIVE);
            ModernSiege.State planned=null;
            if(outcome!=null)try{planned=before.damage(target,actor.member.team(),actor.entityId,outcome.appliedDamage(),elapsedMillis(now),Math.incrementExact(revision));}
                catch(IllegalArgumentException|ArithmeticException failure){outcome=null;status=AttackStatus.NUMERIC;}
            nextRevision();actor.attackSequence=sequence;
            if(planned!=null){siegeState=planned;actor.attacked=true;actor.lastAttackNanos=now;
                if(regenEnabled)actor.regenClock=actor.regenClock.noteAttack();
                if(planned.finished())finishSiege();}
            return attacked(status);
        },()->attacked(AttackStatus.NOT_ACTIVE));
    }
    record SiegePortalResult(ModernSiege.PortalReason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        SiegePortalResult {Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
        Snapshot snapshot(){return ModernLegacyBattleProjection.from(observation);}
    }
    /** A defender-only two-way server-owned portal; no client destination. Its
     * attempts share movement sequencing/cooldowns, including rejected uses. */
    synchronized SiegePortalResult useSiegePortal(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation,long sequence) {
        var actor=requireActor(owner,scope,matchId,generation);
        if(siegeState==null||sequence<1)throw new SecurityException("siege portal unavailable");
        return guarded(()->{
            long now=advance();if(phase==Phase.FINISHED||phase==Phase.ABORTED)return portalResult(ModernSiege.PortalReason.NOT_ACTIVE);
            if(actor.sequence==Long.MAX_VALUE||sequence!=actor.sequence+1)throw new SecurityException("siege portal sequence mismatch");
            var destination=siegeState.config().portal().destination(actor.position);ModernSiege.PortalReason reason;
            if(phase!=Phase.ACTIVE)reason=ModernSiege.PortalReason.NOT_ACTIVE;
            else if(actor.hp==0)reason=ModernSiege.PortalReason.DEAD;
            else if(actor.member.team()==siegeState.attacker())reason=ModernSiege.PortalReason.ATTACKER;
            else if(destination==null)reason=ModernSiege.PortalReason.NOT_AT_PORTAL;
            else if(actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)||castCooling(actor,now))reason=ModernSiege.PortalReason.TOO_FAST;
            else if(actors.stream().anyMatch(a->a!=actor&&a.position.equals(destination)))reason=ModernSiege.PortalReason.OCCUPIED;
            else reason=ModernSiege.PortalReason.MOVED;
            if(!equipmentCurrent())return portalResult(ModernSiege.PortalReason.NOT_ACTIVE);
            nextRevision();actor.sequence=sequence;
            if(reason==ModernSiege.PortalReason.MOVED){actor.position=destination;actor.lastMoveNanos=now;actor.moved=true;
                if(regenEnabled)actor.regenClock=actor.regenClock.noteMove();}
            return portalResult(reason);
        },()->portalResult(ModernSiege.PortalReason.NOT_ACTIVE));
    }
    private SiegePortalResult portalResult(ModernSiege.PortalReason reason){return new SiegePortalResult(reason,capture());}
    /** No existing world encoder carries this frame. Production admission remains unchanged. */
    synchronized ModernNpcBattle.Frame npcSnapshot(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation) {
        requireActor(owner,scope,matchId,generation);requireNpc();
        return guarded(()->{advance();return npcCurrent();},this::npcCurrent);
    }
    /** Owned service pump only; remains available after the final owner retires. */
    synchronized ModernNpcBattle.Frame serviceNpcSnapshot() {
        requireOpen();if(npcRoster==null)throw new IllegalStateException("NPC authority absent");
        return guarded(()->{advance();return npcCurrent();},this::npcCurrent);
    }
    /** Trusted encounter input only: templates must equal the generation's frozen selected catalog. */
    synchronized ModernNpcBattle.Frame spawnNpcs(ModernNpcRoster.Scope scope,List<ModernNpcRoster.Spawn> batch) {
        return ModernLegacyBattleProjection.npcs(spawnNpcsObservation(scope,batch));
    }
    synchronized ModernBattleObservation.Capture spawnNpcsObservation(ModernNpcRoster.Scope scope,List<ModernNpcRoster.Spawn> batch) {
        requireNpcScope(scope);
        if(questProgress!=null||timeAttackState!=null)throw new IllegalStateException("encounter waves are owned by the stage authority");
        var safe=List.copyOf(batch);
        for(var spawn:safe) {
            var t=spawn.template();
            var row=npcConfig.catalog().require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId()));
            if(!row.rosterTemplate().equals(t))throw invalid();
        }
        return guarded(()->{
            advance();if(phase!=Phase.ACTIVE)throw new IllegalStateException("NPC spawn outside active world");
            Math.incrementExact(revision);npcRoster.spawn(npcScope,safe,npcPlayerCells());nextRevision();return capture();
        },this::capture);
    }
    /** Shares participant movement/attack clocks and request sequence, never a second HP owner. */
    synchronized ModernNpcBattle.Result attackNpc(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,
            long generation,long sequence,ModernNpcRoster.Id id,long life) {
        var actor=requireActor(owner,scope,matchId,generation);requireNpcId(id,life);
        if(sequence<1)throw new SecurityException("NPC attack sequence invalid");
        return guarded(()->{
            long now=advance();if(phase!=Phase.ACTIVE)return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
            if(actor.attackSequence==Long.MAX_VALUE||sequence!=actor.attackSequence+1)throw new SecurityException("NPC attack sequence mismatch");
            var before=npcActor(id,life);ModernNpcMelee.Outcome outcome=null;
            var position=actor.position;long priorSequence=actor.attackSequence,priorHp=actor.hp,priorMoveSequence=actor.sequence;
            ModernNpcBattle.Reason reason;
            if(actor.hp==0)reason=ModernNpcBattle.Reason.DEAD;
            else if(before==null||!before.alive())reason=ModernNpcBattle.Reason.TARGET_DEAD;
            else if(before.lane()!=actor.member.team())reason=isolatedForces()?ModernNpcBattle.Reason.TARGET_DEAD:ModernNpcBattle.Reason.WRONG_LANE;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernNpcBattle.Reason.TOO_HEAVY;
            else if(actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)||castCooling(actor,now))reason=ModernNpcBattle.Reason.TOO_FAST;
            else if(distance(actor.position,before.cell())>actor.loadout.combatant().attackRange())reason=ModernNpcBattle.Reason.OUT_OF_RANGE;
            else if(!npcLineOpen(actor.position,before.cell(),actor.member.team()))reason=ModernNpcBattle.Reason.BLOCKED;
            else {
                try {outcome=ModernNpcMelee.againstNpc(actor.loadout.npcPlayer(),npcConfig.catalog(),before,
                        actor.weight==null?0:actor.weight.view().hitPenalty(),random);reason=ModernNpcBattle.Reason.ATTACKED;}
                catch(ModernNpcMelee.CombatException failure){reason=npcFailure(failure);}
                now=advance();if(phase!=Phase.ACTIVE||!participating(actor))return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
                if(actor.attackSequence!=priorSequence)return npcResult(ModernNpcBattle.Reason.STALE_STATE);
                if(!position.equals(actor.position)||priorMoveSequence!=actor.sequence||priorHp!=actor.hp||!Objects.equals(before,npcActor(id,life))) {
                    outcome=null;reason=ModernNpcBattle.Reason.STALE_STATE;
                }
            }
            if(!equipmentCurrent())return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
            long next=Math.incrementExact(revision);
            ModernNpcBattle.Exchange exchange=null;var regenClock=actor.regenClock;
            var nextQuest=questProgress;
            var nextTimeAttack=timeAttackState;
            if(questProgress!=null&&outcome!=null&&outcome.death())nextQuest=ModernQuestProgress.defeated(questProgress,before);
            if(timeAttackState!=null&&outcome!=null&&outcome.death())nextTimeAttack=ModernTimeAttack.defeated(timeAttackState,before,next);
            var terminalBuffs=planTimeAttackClear(nextTimeAttack,now,next);
            if(outcome!=null) {
                exchange=new ModernNpcBattle.Exchange(ModernNpcBattle.Direction.PLAYER_TO_NPC,actor.member.identity(),participantLife(actor),id,life,sequence,next,outcome);
                if(regenEnabled)regenClock=regenClock.noteAttack();
                // All participant-side values exist before the victim changes.
                // Zero damage still consumes the participant request/clock and
                // typed receipt, but never invents a victim HURT event.
                npcRoster.damage(npcScope,id,life,outcome.appliedDamage(),terminalBuffs!=null);
            }
            nextRevision();actor.attackSequence=sequence;
            questProgress=nextQuest;
            timeAttackState=nextTimeAttack;
            if(timeAttackFinished(timeAttackState))finishTimeAttack(terminalBuffs);
            if(outcome!=null) {
                actor.attacked=true;actor.lastAttackNanos=now;actor.regenClock=regenClock;
                playerNpcExchanges.put(actor.member.identity(),exchange);
                // Do not place an NPC serial in Vital.lastTargetEntityId. The
                // existing PvP encoder is intentionally untouched.
            }
            return npcResult(reason);
        },()->npcResult(ModernNpcBattle.Reason.NOT_ACTIVE));
    }
    /** Same-engine NPC skill 4 transaction. The caller supplies identity/life,
     * never damage, position, MP or a PvP entity alias. */
    synchronized ModernNpcMagic.Result castNpc(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,
            long generation,long sequence,long casterLife,ModernNpcRoster.Id id,long life) {
        var actor=requireActor(owner,scope,matchId,generation);requireNpc();requireNpcId(id,life);
        if(sequence<1||casterLife!=participantLife(actor))throw new SecurityException("NPC spell life/sequence mismatch");
        return guarded(()->{
            long now=advance();
            if(!npcMagicEnabled||phase!=Phase.ACTIVE||!participating(actor))return npcCasted(ModernNpcMagic.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE||sequence!=actor.spellSequence()+1)throw new SecurityException("NPC spell sequence mismatch");
            if(casterLife!=participantLife(actor))throw new SecurityException("NPC spell caster life changed");
            var before=npcActor(id,life);var from=actor.position;
            long priorSequence=actor.spellSequence(),priorMove=actor.sequence,priorAttack=actor.attackSequence,priorHp=actor.hp,priorMp=actor.mp;
            var priorRegen=actor.regen;var priorItems=actor.itemUse;
            var learned=actor.prepared.skills().skills().stream().filter(s->s.skillId()==4).findFirst();
            ModernNpcMagic.Reason reason;ModernMagicCombat.Outcome outcome=null;
            if(actor.hp==0)reason=ModernNpcMagic.Reason.DEAD;
            else if(learned.isEmpty())reason=ModernNpcMagic.Reason.UNLEARNED;
            else if(learned.get().support()!=ModernSkills.Support.LIGHT_ARROW||!actor.loadout.magic().supported())reason=ModernNpcMagic.Reason.UNSUPPORTED;
            else if(before==null||!before.alive())reason=ModernNpcMagic.Reason.TARGET_DEAD;
            else if(before.lane()!=actor.member.team())reason=isolatedForces()?ModernNpcMagic.Reason.TARGET_DEAD:ModernNpcMagic.Reason.WRONG_LANE;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernNpcMagic.Reason.TOO_HEAVY;
            else if(castCooling(actor,now)||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK))reason=ModernNpcMagic.Reason.TOO_FAST;
            else if(distance(from,before.cell())>ModernMagicCombat.RANGE)reason=ModernNpcMagic.Reason.OUT_OF_RANGE;
            else if(!npcLineOpen(from,before.cell(),actor.member.team()))reason=ModernNpcMagic.Reason.BLOCKED;
            else if(actor.mp<spellCost(actor,ModernUseHistory.Lane.NPC_LIGHT_ARROW))reason=ModernNpcMagic.Reason.NO_MP;
            else {
                try{preflightSpell(actor,ModernUseHistory.Lane.NPC_LIGHT_ARROW,sequence);outcome=ModernNpcMagic.againstNpc(actor.loadout.magic(),npcConfig.catalog(),before,random);reason=ModernNpcMagic.Reason.NONE;}
                catch(ModernMagicCombat.CombatException failure){reason=failure.failure()==ModernMagicCombat.Failure.UNSUPPORTED?ModernNpcMagic.Reason.UNSUPPORTED:ModernNpcMagic.Reason.NUMERIC;}
                catch(ArithmeticException failure){reason=ModernNpcMagic.Reason.NUMERIC;}
                // Revalidate after a clock-advancing/reentrant random source.
                // Normal roster clock ticks are allowed, changed actors are not.
                now=advance();if(phase!=Phase.ACTIVE||!participating(actor))return npcCasted(ModernNpcMagic.Reason.NOT_ACTIVE);
                if(actor.spellSequence()!=priorSequence)return npcCasted(ModernNpcMagic.Reason.STALE_STATE);
                var targetNow=npcRoster.snapshot().actors().stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
                if(regenEnabled&&actor.regen.mpRevision()!=priorRegen.mpRevision()) {
                    outcome=null;reason=ModernNpcMagic.Reason.STALE_REGEN;
                } else if(actor.sequence!=priorMove||actor.attackSequence!=priorAttack||actor.hp!=priorHp||actor.mp!=priorMp
                        ||participantLife(actor)!=casterLife||!from.equals(actor.position)||!priorRegen.equals(actor.regen)
                        ||!Objects.equals(priorItems,actor.itemUse)||!Objects.equals(before,targetNow)
                        ||!npcLineOpen(from,before.cell(),actor.member.team())) {
                    outcome=null;reason=ModernNpcMagic.Reason.STALE_STATE;
                }
            }
            if(!equipmentCurrent())return npcCasted(ModernNpcMagic.Reason.NOT_ACTIVE);
            long next=Math.incrementExact(revision);var progress=questProgress;var nextTime=timeAttackState;
            var deniedRuntime=actor.spellRuntime.denied(next,sequence);var runtime=deniedRuntime;
            var resources=actor.equipmentResources;
            var history=actor.spellHistory;
            var targets=actor.targetHistory;
            List<ModernTimedEffects.State> terminalBuffs=null;
            if(outcome!=null) {
                try {
                    var committed=planCastHistory(actor,ModernUseHistory.Lane.NPC_LIGHT_ARROW,next,sequence,
                            List.of(npcEffect(before,outcome.rawDamage(),outcome.appliedDamage(),outcome.remainingHp())));
                    resources=committed.resources();history=committed.spells();targets=committed.targets();
                    runtime=planSpellRuntime(actor,committed,ModernUseHistory.Lane.NPC_LIGHT_ARROW,now,new ModernSpellRuntime.Arrow(outcome.critical()));
                    if(progress!=null&&outcome.death())progress=ModernQuestProgress.defeated(progress,before);
                    if(nextTime!=null&&outcome.death())nextTime=ModernTimeAttack.defeated(nextTime,before,next);
                    terminalBuffs=planTimeAttackClear(nextTime,now,next);
                } catch(ArithmeticException|IllegalArgumentException failure){outcome=null;reason=ModernNpcMagic.Reason.NUMERIC;}
            }
            // Every fallible caster/progress calculation precedes the victim
            // mutation. One monitor/guard owns all HP, MP and wave state.
            if(outcome!=null)npcRoster.damage(npcScope,id,life,outcome.appliedDamage(),terminalBuffs!=null);
            nextRevision();actor.spellRuntime=outcome==null?deniedRuntime:runtime;
            if(outcome!=null) {
                actor.mp=resources.vitals().mp();actor.equipmentResources=resources;actor.spellHistory=history;actor.targetHistory=targets;questProgress=progress;
                timeAttackState=nextTime;
                if(timeAttackFinished(timeAttackState))finishTimeAttack(terminalBuffs);
            }
            return npcCasted(reason);
        },()->npcCasted(ModernNpcMagic.Reason.NOT_ACTIVE));
    }
    private ModernNpcMagic.Result npcCasted(ModernNpcMagic.Reason reason){return new ModernNpcMagic.Result(reason,capture());}

    /** Internal self-centred skill70. Client supplies no victims/damage. The
     * typed wire/UI are not yet admitted; no old PvP/skill4 reply may carry it. */
    synchronized ModernNpcFireStormState.Result fireStormNpc(TournamentPresence.Owner owner,ModernNpcFireStormState.Request request) {
        var scope=request.world();var actor=requireActor(owner,request.player(),scope.matchId(),scope.generation());requireNpc();
        if(!scope.equals(npcScope)||request.casterLife()!=participantLife(actor))throw new SecurityException("NPC fire storm scope/life mismatch");
        return guarded(()->{
            long now=advance();
            if(!npcFireStormEnabled||phase!=Phase.ACTIVE||!participating(actor))return npcFireStormed(ModernNpcFireStormState.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE||request.sequence()!=actor.spellSequence()+1)throw new SecurityException("NPC fire storm sequence mismatch");
            if(request.casterLife()!=participantLife(actor))throw new SecurityException("NPC fire storm caster life changed");
            var prior=actor.spellHistory.get(ModernUseHistory.Lane.NPC_FIRE_STORM);var origin=actor.position;var regen=actor.regen;var items=actor.itemUse;
            long seq=actor.spellSequence(),move=actor.sequence,attack=actor.attackSequence,hp=actor.hp,mp=actor.mp;
            var learned=actor.prepared.skills().skills().stream().filter(s->s.skillId()==ModernFireStorm.SKILL_ID).findFirst();
            var reason=ModernNpcFireStormState.Reason.NONE;ModernNpcFireStorm.Incoming batch=null;boolean attempted=false;
            ModernQuestObjectives.Actor questTarget=null;ModernTimeAttack.State towerTarget=null;
            ModernNpcFireStormStructures.Hit structure=null;
            if(hp==0)reason=ModernNpcFireStormState.Reason.DEAD;
            else if(learned.isEmpty())reason=ModernNpcFireStormState.Reason.UNLEARNED;
            else if(learned.get().support()!=ModernSkills.Support.FIRE_STORM)reason=ModernNpcFireStormState.Reason.UNSUPPORTED;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernNpcFireStormState.Reason.TOO_HEAVY;
            else if(castCooling(actor,now)||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.spellRuntime.cooling(ModernUseHistory.Lane.NPC_FIRE_STORM,now,TimeUnit.MILLISECONDS.toNanos(ModernFireStorm.REUSE_MILLIS)))reason=ModernNpcFireStormState.Reason.TOO_FAST;
            else if(mp<spellCost(actor,ModernUseHistory.Lane.NPC_FIRE_STORM))reason=ModernNpcFireStormState.Reason.NO_MP;
            else try {
                var before=npcRoster.snapshot();
                var candidates=before.actors().stream().filter(n->n.alive()&&n.lane()==actor.member.team()
                        &&distance(origin,n.cell())<=ModernFireStorm.RADIUS&&npcLineOpen(origin,n.cell(),actor.member.team())).toList();
                var q=questObjectives.get(actor.member.team());
                if(q!=null&&q.alive()&&distance(origin,q.cell())<=ModernFireStorm.RADIUS&&npcLineOpen(origin,q.cell(),actor.member.team()))questTarget=q;
                var t=timeAttackState;
                if(t!=null&&t.phase()==ModernTimeAttack.Phase.WAITING_FOR_TOWER&&distance(origin,t.plan().tower().cell())<=ModernFireStorm.RADIUS
                        &&npcLineOpen(origin,t.plan().tower().cell(),0))towerTarget=t;
                if(candidates.size()+(questTarget!=null||towerTarget!=null?1:0)>ModernNpcFireStorm.MAX_TARGETS)throw invalid();
                preflightSpell(actor,ModernUseHistory.Lane.NPC_FIRE_STORM,request.sequence());
                attempted=true;batch=ModernNpcFireStorm.prepareIncoming(actor.loadout.fighter(),npcConfig.catalog(),before,origin,actor.member.team(),candidates,random);
                if(questTarget!=null)structure=new ModernNpcFireStormStructures.QuestHit(questTarget,
                        ModernNpcFireStorm.againstObjective(actor.loadout.fireStorm(),questTarget.hp(),questTarget.target().maximumHp(),random));
                if(towerTarget!=null)structure=new ModernNpcFireStormStructures.TowerHit(towerTarget,
                        ModernNpcFireStorm.againstObjective(actor.loadout.fireStorm(),towerTarget.towerHp(),towerTarget.plan().tower().maximumHp(),random));
            }catch(ModernFireStorm.CombatException failure) {
                batch=null;
                reason=failure.failure()==ModernFireStorm.Failure.UNSUPPORTED?ModernNpcFireStormState.Reason.UNSUPPORTED:ModernNpcFireStormState.Reason.NUMERIC;
            }catch(ArithmeticException|IllegalArgumentException failure){batch=null;reason=ModernNpcFireStormState.Reason.NUMERIC;}
            if(attempted) {
                now=advance();
                if(phase!=Phase.ACTIVE||!participating(actor))return npcFireStormed(ModernNpcFireStormState.Reason.NOT_ACTIVE);
                if(actor.spellSequence()!=seq)return npcFireStormed(ModernNpcFireStormState.Reason.STALE_STATE);
                if(actor.regen.mpRevision()!=regen.mpRevision()){batch=null;reason=ModernNpcFireStormState.Reason.STALE_REGEN;}
                else if(actor.sequence!=move||actor.attackSequence!=attack||actor.hp!=hp||actor.mp!=mp
                        ||participantLife(actor)!=request.casterLife()||!origin.equals(actor.position)||actor.spellHistory.get(ModernUseHistory.Lane.NPC_FIRE_STORM)!=prior
                        ||!actor.regen.equals(regen)||!Objects.equals(items,actor.itemUse)) {
                    batch=null;reason=ModernNpcFireStormState.Reason.STALE_STATE;
                } else if(batch!=null)try {
                    batch=batch.revalidated(npcRoster.snapshot());
                    for(var hit:batch.hits())if(!npcLineOpen(origin,hit.before().cell(),actor.member.team()))throw invalid();
                    if(questTarget!=null&&(!questTarget.equals(questObjectives.get(actor.member.team()))||!npcLineOpen(origin,questTarget.cell(),actor.member.team())))throw invalid();
                    if(towerTarget!=null&&(timeAttackState.phase()!=ModernTimeAttack.Phase.WAITING_FOR_TOWER
                            ||timeAttackState.towerHp()!=towerTarget.towerHp()||timeAttackState.towerHurtRevision()!=towerTarget.towerHurtRevision()
                            ||!npcLineOpen(origin,towerTarget.plan().tower().cell(),0)))throw invalid();
                }catch(IllegalArgumentException stale){batch=null;reason=ModernNpcFireStormState.Reason.STALE_STATE;}
            }
            if(!equipmentCurrent())return npcFireStormed(ModernNpcFireStormState.Reason.NOT_ACTIVE);
            long next=Math.incrementExact(revision),remainingMp=mp;var progress=questProgress;var nextTime=timeAttackState;
            var deniedRuntime=actor.spellRuntime.denied(next,request.sequence());var runtime=deniedRuntime;
            var resources=actor.equipmentResources;
            var history=actor.spellHistory;
            var targets=actor.targetHistory;
            var questAfter=questTarget;ModernMatchSettlement.Event completed=null;
            List<ModernTimedEffects.State> terminalBuffs=null;
            if(batch!=null)try {
                var impacts=new ArrayList<ModernTargetHistory.Effect>();
                for(var hit:batch.hits())impacts.add(npcEffect(hit.before(),hit.outcome().rawDamage(),hit.outcome().appliedDamage(),hit.outcome().remainingHp()));
                if(structure!=null)impacts.add(structureEffect(structure));
                var committed=planCastHistory(actor,ModernUseHistory.Lane.NPC_FIRE_STORM,next,request.sequence(),impacts);
                resources=committed.resources();remainingMp=resources.vitals().mp();history=committed.spells();targets=committed.targets();
                runtime=planSpellRuntime(actor,committed,ModernUseHistory.Lane.NPC_FIRE_STORM,now,
                        new ModernSpellRuntime.NpcFireStorm(structure instanceof ModernNpcFireStormStructures.QuestHit q?q.kind():null));
                var defeated=batch.hits().stream().filter(h->h.outcome().death()).map(ModernNpcFireStorm.Hit::before).toList();
                if(progress!=null)for(var target:defeated)progress=ModernQuestProgress.defeated(progress,target);
                if(nextTime!=null&&!defeated.isEmpty())nextTime=ModernTimeAttack.defeated(nextTime,defeated,next);
                if(structure instanceof ModernNpcFireStormStructures.QuestHit hit) {
                    questAfter=questTarget.damaged(hit.outcome(),next);
                    if(hit.outcome().death())progress=ModernQuestProgress.objectiveDestroyed(progress,npcScope,hit.lane(),hit.id().stage());
                    completed=questVictory(progress,next);
                }
                if(structure instanceof ModernNpcFireStormStructures.TowerHit hit)nextTime=ModernTimeAttack.towerHit(nextTime,hit.outcome(),next);
                if(timedEnabled&&(completed!=null||timeAttackFinished(nextTime)))terminalBuffs=planClearedBuffs(now,next);
            }catch(ArithmeticException|IllegalArgumentException failure){batch=null;reason=ModernNpcFireStormState.Reason.NUMERIC;}
            // Roster prepares all replacement victims before publishing once.
            // All fallible caster/quest/cooperative calculations are above.
            if(batch!=null)try {npcRoster.commitIncoming(batch,terminalBuffs!=null);}
            catch(ArithmeticException|IllegalArgumentException failure){batch=null;reason=ModernNpcFireStormState.Reason.NUMERIC;}
            nextRevision();actor.spellRuntime=batch==null?deniedRuntime:runtime;
            if(batch!=null) {
                actor.mp=remainingMp;actor.equipmentResources=resources;actor.spellHistory=history;actor.targetHistory=targets;
                questProgress=progress;timeAttackState=nextTime;
                if(structure instanceof ModernNpcFireStormStructures.QuestHit)questObjectives.put(actor.member.team(),questAfter);
                if(completed!=null){commitClearedBuffs(terminalBuffs);clearAllArmor(now,revision);result=completed;phase=Phase.FINISHED;}
                if(timeAttackFinished(nextTime))finishTimeAttack(terminalBuffs);
            }
            return npcFireStormed(reason);
        },()->npcFireStormed(ModernNpcFireStormState.Reason.NOT_ACTIVE));
    }
    private ModernNpcFireStormState.Result npcFireStormed(ModernNpcFireStormState.Reason reason){return new ModernNpcFireStormState.Result(reason,capture());}

    /** One authority transaction for typed NPC skill77. No source SQL write or
     * PC target alias; all potentially failing receipts/progress precede HP. */
    synchronized ModernNpcDisintegrateState.Result disintegrateNpc(TournamentPresence.Owner owner,ModernNpcDisintegrateState.Request request) {
        var id=request.target();var scope=id.scope();
        var actor=requireActor(owner,request.player(),scope.matchId(),scope.generation());requireNpc();requireNpcId(id,request.targetLife());
        if(request.casterLife()!=participantLife(actor))throw new SecurityException("NPC disintegrate caster life mismatch");
        return guarded(()->{
            long now=advance();
            if(!npcDisintegrateEnabled||phase!=Phase.ACTIVE||!participating(actor))return npcDisintegrated(ModernNpcDisintegrateState.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE||request.sequence()!=actor.spellSequence()+1)
                throw new SecurityException("NPC disintegrate sequence mismatch");
            if(request.casterLife()!=participantLife(actor))throw new SecurityException("NPC disintegrate caster life changed");
            var before=npcActor(id,request.targetLife());var from=actor.position;var prior=actor.spellHistory.get(ModernUseHistory.Lane.NPC_DISINTEGRATE);
            long sequence=actor.spellSequence(),move=actor.sequence,attack=actor.attackSequence,hp=actor.hp,mp=actor.mp;
            var regen=actor.regen;var items=actor.itemUse;
            var reason=ModernNpcDisintegrateState.Reason.NONE;ModernDisintegrate.Outcome outcome=null;boolean attempted=false;
            if(hp==0)reason=ModernNpcDisintegrateState.Reason.DEAD;
            else if(actor.prepared.npcDisintegrate().status()==ModernDisintegrateCatalog.Status.UNLEARNED)reason=ModernNpcDisintegrateState.Reason.UNLEARNED;
            else if(!actor.prepared.npcDisintegrate().accepts(actor.loadout.disintegrate()))reason=ModernNpcDisintegrateState.Reason.UNSUPPORTED;
            else if(before==null||!before.alive())reason=ModernNpcDisintegrateState.Reason.TARGET_DEAD;
            else if(before.lane()!=actor.member.team())reason=isolatedForces()?ModernNpcDisintegrateState.Reason.TARGET_DEAD:ModernNpcDisintegrateState.Reason.WRONG_LANE;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernNpcDisintegrateState.Reason.TOO_HEAVY;
            else if(!ModernDisintegrate.lawfulAllows(actor.spellRuntime.lawful()))reason=ModernNpcDisintegrateState.Reason.LAWFUL;
            else if(castCooling(actor,now)||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.spellRuntime.cooling(ModernUseHistory.Lane.NPC_DISINTEGRATE,now,TimeUnit.MILLISECONDS.toNanos(ModernDisintegrate.REUSE_MILLIS)))reason=ModernNpcDisintegrateState.Reason.TOO_FAST;
            else if(distance(from,before.cell())>ModernDisintegrate.RANGE)reason=ModernNpcDisintegrateState.Reason.OUT_OF_RANGE;
            else if(!npcLineOpen(from,before.cell(),actor.member.team()))reason=ModernNpcDisintegrateState.Reason.BLOCKED;
            else if(mp<spellCost(actor,ModernUseHistory.Lane.NPC_DISINTEGRATE))reason=ModernNpcDisintegrateState.Reason.NO_MP;
            else try {
                preflightSpell(actor,ModernUseHistory.Lane.NPC_DISINTEGRATE,request.sequence());
                ModernDisintegrate.lawfulAfterCast(actor.spellRuntime.lawful());
                attempted=true;
                outcome=ModernNpcDisintegrate.againstNpc(actor.loadout.disintegrate(),npcConfig.catalog(),before,random,0);
            }catch(ModernDisintegrate.CombatException failure) {
                reason=failure.failure()==ModernDisintegrate.Failure.UNSUPPORTED?ModernNpcDisintegrateState.Reason.UNSUPPORTED:ModernNpcDisintegrateState.Reason.NUMERIC;
            }catch(ArithmeticException|IllegalArgumentException failure){reason=ModernNpcDisintegrateState.Reason.NUMERIC;}
            if(attempted) {
                now=advance();
                if(phase!=Phase.ACTIVE||!participating(actor))return npcDisintegrated(ModernNpcDisintegrateState.Reason.NOT_ACTIVE);
                if(actor.spellSequence()!=sequence)return npcDisintegrated(ModernNpcDisintegrateState.Reason.STALE_STATE);
                var targetNow=npcRoster.snapshot().actors().stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
                if(actor.regen.mpRevision()!=regen.mpRevision()) {outcome=null;reason=ModernNpcDisintegrateState.Reason.STALE_REGEN;}
                else if(actor.sequence!=move||actor.attackSequence!=attack||actor.hp!=hp||actor.mp!=mp
                        ||participantLife(actor)!=request.casterLife()||actor.spellHistory.get(ModernUseHistory.Lane.NPC_DISINTEGRATE)!=prior||!from.equals(actor.position)
                        ||!regen.equals(actor.regen)||!Objects.equals(items,actor.itemUse)||!Objects.equals(before,targetNow)
                        ||!npcLineOpen(from,before.cell(),actor.member.team())) {
                    outcome=null;reason=ModernNpcDisintegrateState.Reason.STALE_STATE;
                }
            }
            if(!equipmentCurrent())return npcDisintegrated(ModernNpcDisintegrateState.Reason.NOT_ACTIVE);
            long next=Math.incrementExact(revision),remainingMp=actor.mp;
            var deniedRuntime=actor.spellRuntime.denied(next,request.sequence());var runtime=deniedRuntime;
            var nextQuest=questProgress;var nextTime=timeAttackState;
            var resources=actor.equipmentResources;
            var history=actor.spellHistory;
            var targets=actor.targetHistory;
            List<ModernTimedEffects.State> terminalBuffs=null;
            if(outcome!=null)try {
                var committed=planCastHistory(actor,ModernUseHistory.Lane.NPC_DISINTEGRATE,next,request.sequence(),
                        List.of(npcEffect(before,outcome.rawDamage(),outcome.appliedDamage(),outcome.remainingHp())));
                resources=committed.resources();remainingMp=resources.vitals().mp();history=committed.spells();targets=committed.targets();
                runtime=planSpellRuntime(actor,committed,ModernUseHistory.Lane.NPC_DISINTEGRATE,now,
                        new ModernSpellRuntime.Disintegrate(outcome.critical(),actor.spellRuntime.lawful(),ModernDisintegrate.lawfulAfterCast(actor.spellRuntime.lawful())));
                if(outcome.death()&&questProgress!=null)nextQuest=ModernQuestProgress.defeated(questProgress,before);
                if(outcome.death()&&timeAttackState!=null)nextTime=ModernTimeAttack.defeated(timeAttackState,before,next);
                terminalBuffs=planTimeAttackClear(nextTime,now,next);
            }catch(ArithmeticException|IllegalArgumentException failure){outcome=null;reason=ModernNpcDisintegrateState.Reason.NUMERIC;}
            // Only non-throwing assignments remain after the victim mutation.
            // Zero damage consumes the cast/MP/lawful/cooldown but not NPC HURT.
            if(outcome!=null)npcRoster.damage(npcScope,id,before.life(),outcome.appliedDamage(),terminalBuffs!=null);
            nextRevision();actor.spellRuntime=outcome==null?deniedRuntime:runtime;
            if(outcome!=null) {
                actor.mp=remainingMp;actor.equipmentResources=resources;actor.spellHistory=history;actor.targetHistory=targets;
                questProgress=nextQuest;timeAttackState=nextTime;
                if(timeAttackFinished(timeAttackState))finishTimeAttack(terminalBuffs);
            }
            return npcDisintegrated(reason);
        },()->npcDisintegrated(ModernNpcDisintegrateState.Reason.NOT_ACTIVE));
    }
    private ModernNpcDisintegrateState.Result npcDisintegrated(ModernNpcDisintegrateState.Reason reason){return new ModernNpcDisintegrateState.Result(reason,capture());}

    /** Qualified Quest target damage uses the same participant clock/sequence,
     * but never aliases a participant or NPC target. No client HP claims. */
    synchronized ModernNpcBattle.Result attackQuestObjective(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,
            long generation,long sequence,ModernQuestObjectives.Id id) {
        var actor=requireActor(owner,scope,matchId,generation);requireNpc();
        if(!questObjectivesEnabled()||id==null||!id.scope().equals(npcScope)||sequence<1)throw new SecurityException("Quest objective scope mismatch");
        return guarded(()->{
            long now=advance();if(phase!=Phase.ACTIVE)return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
            if(actor.attackSequence==Long.MAX_VALUE||sequence!=actor.attackSequence+1)throw new SecurityException("Quest attack sequence mismatch");
            var before=questObjectives.get(id.lane());var position=actor.position;
            long priorSequence=actor.attackSequence,priorHp=actor.hp,priorMove=actor.sequence;
            ModernPhysicalCombat.Outcome outcome=null;ModernNpcBattle.Reason reason;
            if(actor.hp==0)reason=ModernNpcBattle.Reason.DEAD;
            else if(before==null||!before.id().equals(id)||!before.alive()||id.lane()!=actor.member.team())reason=ModernNpcBattle.Reason.TARGET_DEAD;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernNpcBattle.Reason.TOO_HEAVY;
            else if(actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)||castCooling(actor,now))reason=ModernNpcBattle.Reason.TOO_FAST;
            else if(distance(actor.position,before.cell())>actor.loadout.combatant().attackRange())reason=ModernNpcBattle.Reason.OUT_OF_RANGE;
            else if(!npcLineOpen(actor.position,before.cell(),actor.member.team()))reason=ModernNpcBattle.Reason.BLOCKED;
            else {
                try{outcome=actor.loadout.combatant().attackObjective(before.hp(),before.target().maximumHp(),random,actor.weight==null?0:actor.weight.view().hitPenalty(),null);reason=ModernNpcBattle.Reason.ATTACKED;}
                catch(ModernPhysicalCombat.CombatException failure){reason=failure.failure()==ModernPhysicalCombat.Failure.UNSUPPORTED?ModernNpcBattle.Reason.UNSUPPORTED:ModernNpcBattle.Reason.NUMERIC;}
                catch(ArithmeticException failure){reason=ModernNpcBattle.Reason.NUMERIC;}
                now=advance();if(phase!=Phase.ACTIVE||!participating(actor))return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
                if(actor.attackSequence!=priorSequence)return npcResult(ModernNpcBattle.Reason.STALE_STATE);
                if(actor.hp!=priorHp||actor.sequence!=priorMove||!actor.position.equals(position)||!Objects.equals(before,questObjectives.get(id.lane()))
                        ||!npcLineOpen(actor.position,before.cell(),actor.member.team())) {
                    outcome=null;reason=ModernNpcBattle.Reason.STALE_STATE;
                }
            }
            if(!equipmentCurrent())return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
            long next=Math.incrementExact(revision);var after=before;var progress=questProgress;
            ModernQuestObjectives.Exchange receipt=null;ModernMatchSettlement.Event completed=null;
            List<ModernTimedEffects.State> terminalBuffs=null;
            var regenClock=actor.regenClock;
            if(outcome!=null) {
                after=before.damaged(outcome,next);
                receipt=new ModernQuestObjectives.Exchange(actor.member.identity(),id,sequence,next,outcome);
                if(regenEnabled)regenClock=regenClock.noteAttack();
                if(outcome.death())progress=ModernQuestProgress.objectiveDestroyed(progress,npcScope,id.lane(),id.stage());
                completed=questVictory(progress,next);
            }
            if(completed!=null&&timedEnabled)try {
                terminalBuffs=planClearedBuffs(now,next);
                // Last fallible operation: all NPC replacements precede its
                // atomic swap, followed only by the engine assignments below.
                npcRoster.clearHaste(npcScope);
            }catch(ArithmeticException|IllegalArgumentException failure){outcome=null;reason=ModernNpcBattle.Reason.NUMERIC;}
            // Build target, progress, receipt and result before committing any
            // HP or clock. Neither NPC serials nor objective slots enter the
            // participant/PvP target field.
            nextRevision();actor.attackSequence=sequence;
            if(outcome!=null) {
                questObjectives.put(id.lane(),after);questProgress=progress;questExchanges.put(actor.member.identity(),receipt);
                actor.attacked=true;actor.lastAttackNanos=now;actor.regenClock=regenClock;
                if(completed!=null){commitClearedBuffs(terminalBuffs);clearAllArmor(now,revision);result=completed;phase=Phase.FINISHED;}
            }
            return npcResult(reason);
        },()->npcResult(ModernNpcBattle.Reason.NOT_ACTIVE));
    }
    /** Time Attack starts only on actual start-tower destruction. This internal
     * typed seam shares melee sequence/clock and never accepts client damage. */
    synchronized ModernNpcBattle.Result attackTimeAttackTower(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,
            long generation,long sequence,ModernTimeAttack.TowerId target) {
        var actor=requireActor(owner,scope,matchId,generation);requireNpc();
        if(timeAttackState==null||target==null||!target.scope().equals(npcScope)||sequence<1)throw new SecurityException("Time Attack tower scope mismatch");
        return guarded(()->{
            long now=advance();if(phase!=Phase.ACTIVE)return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
            if(actor.attackSequence==Long.MAX_VALUE||sequence!=actor.attackSequence+1)throw new SecurityException("Time Attack melee sequence mismatch");
            var before=timeAttackState;var tower=before.plan().tower();var position=actor.position;
            long hp=actor.hp,move=actor.sequence,priorSequence=actor.attackSequence;
            ModernPhysicalCombat.Outcome outcome=null;ModernNpcBattle.Reason reason;
            if(actor.hp==0)reason=ModernNpcBattle.Reason.DEAD;
            else if(before.phase()!=ModernTimeAttack.Phase.WAITING_FOR_TOWER)reason=ModernNpcBattle.Reason.TARGET_DEAD;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernNpcBattle.Reason.TOO_HEAVY;
            else if(actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)||castCooling(actor,now))reason=ModernNpcBattle.Reason.TOO_FAST;
            else if(distance(position,tower.cell())>actor.loadout.combatant().attackRange())reason=ModernNpcBattle.Reason.OUT_OF_RANGE;
            else if(!npcLineOpen(position,tower.cell(),0))reason=ModernNpcBattle.Reason.BLOCKED;
            else {
                try{outcome=actor.loadout.combatant().attackObjective(before.towerHp(),tower.maximumHp(),random,actor.weight==null?0:actor.weight.view().hitPenalty(),null);reason=ModernNpcBattle.Reason.ATTACKED;}
                catch(ModernPhysicalCombat.CombatException failure){reason=failure.failure()==ModernPhysicalCombat.Failure.UNSUPPORTED?ModernNpcBattle.Reason.UNSUPPORTED:ModernNpcBattle.Reason.NUMERIC;}
                catch(ArithmeticException failure){reason=ModernNpcBattle.Reason.NUMERIC;}
                now=advance();if(phase!=Phase.ACTIVE)return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
                if(!participating(actor))return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
                if(actor.attackSequence!=priorSequence)return npcResult(ModernNpcBattle.Reason.STALE_STATE);
                if(actor.hp!=hp||actor.sequence!=move||!actor.position.equals(position)||timeAttackState.towerHp()!=before.towerHp()
                        ||timeAttackState.towerHurtRevision()!=before.towerHurtRevision()||!npcLineOpen(position,tower.cell(),0)) {
                    outcome=null;reason=ModernNpcBattle.Reason.STALE_STATE;
                }
            }
            if(!equipmentCurrent())return npcResult(ModernNpcBattle.Reason.NOT_ACTIVE);
            long next=Math.incrementExact(revision);var state=timeAttackState;var clock=actor.regenClock;
            ModernTimeAttack.Exchange receipt=null;
            if(outcome!=null) {
                state=ModernTimeAttack.towerHit(state,outcome,next);
                receipt=new ModernTimeAttack.Exchange(actor.entityId,target,sequence,next,outcome);
                if(regenEnabled)clock=clock.noteAttack();
            }
            nextRevision();actor.attackSequence=sequence;timeAttackState=state;
            if(receipt!=null){timeAttackExchanges.put(actor.entityId,receipt);actor.attacked=true;actor.lastAttackNanos=now;actor.regenClock=clock;}
            return npcResult(reason);
        },()->npcResult(ModernNpcBattle.Reason.NOT_ACTIVE));
    }
    /** One bounded server-owned pursuit/strike with an exact immutable receipt. */
    synchronized ModernNpcBattle.Result stepNpc(ModernNpcRoster.Id id,long life,int budget,boolean night) {
        return stepNpc(id,life,budget,night,this::npcResult);
    }
    /** Scheduler-only step. No network request or intermediate publication.
     * Release the engine monitor between NPCs exactly as the observed path does.
     * Terminal capture is retained here: it seals settlement identity/time at
     * the actual finishing operation, not at a later scheduler publication. */
    synchronized ModernNpcBattle.Reason stepNpcUnpublished(ModernNpcRoster.Id id,long life,int budget,boolean night) {
        return stepNpc(id,life,budget,night,reason->{
            if(phase!=Phase.ACTIVE)capture();
            return reason;
        });
    }
    /** One final scheduler capture, without another clock advance/regen/RNG.
     * The same engine monitor and preparation guard protect the complete facts.
     * This is not a command receipt and must never be used to acknowledge one. */
    synchronized ModernBattleObservation.Capture npcTickObservation() {
        requireNpc();
        return guarded(this::capture,this::capture);
    }
    private <T> T stepNpc(ModernNpcRoster.Id id,long life,int budget,boolean night,java.util.function.Function<ModernNpcBattle.Reason,T> finish) {
        requireNpcId(id,life);
        if(budget<1||budget>ModernNpcRoster.MAX_PATH_CELLS)throw invalid();
        return guarded(()->{
            long now=advance();if(phase!=Phase.ACTIVE)return finish.apply(ModernNpcBattle.Reason.NOT_ACTIVE);
            var before=npcActor(id,life);if(before==null||!before.alive())return finish.apply(ModernNpcBattle.Reason.DEAD);
            var plan=npcPlan(id,life,budget);
            if(plan.kind()==ModernNpcRoster.PlanKind.MOVE) {
                if(!equipmentCurrent())return finish.apply(ModernNpcBattle.Reason.NOT_ACTIVE);
                Math.incrementExact(revision);npcRoster.move(npcScope,plan,npcObstacleCells(before.lane()));nextRevision();
                return finish.apply(ModernNpcBattle.Reason.MOVED);
            }
            if(plan.kind()==ModernNpcRoster.PlanKind.CANCELLED){advance();return finish.apply(ModernNpcBattle.Reason.NOT_ACTIVE);}
            if(plan.kind()!=ModernNpcRoster.PlanKind.IN_RANGE)return finish.apply(switch(plan.kind()) {
                case WAIT->ModernNpcBattle.Reason.WAIT;case BUDGET->ModernNpcBattle.Reason.BUDGET;
                case CANCELLED->ModernNpcBattle.Reason.NOT_ACTIVE;default->ModernNpcBattle.Reason.NO_TARGET;});
            long elapsed=npcRoster.snapshot().elapsedMillis();
            if(elapsed<before.nextMoveMillis()||elapsed<before.nextAttackMillis())return finish.apply(ModernNpcBattle.Reason.TOO_FAST);
            var targetId=plan.target();var target=actors.stream().filter(a->a.member.identity().equals(targetId)).findFirst().orElseThrow(ModernBattleEngine::invalid);
            long hp=target.hp,moveSequence=target.sequence;var position=target.position;var ledger=target.regen;
            ModernNpcMelee.Outcome outcome=null;ModernNpcBattle.Reason reason;ArmorHit armorHit=null;
            try{var guard=armorGuard(target,null,now);
                var damage=ModernNpcMelee.againstProtectedParticipant(npcConfig.catalog(),before,target.loadout.npcVictim(),hp,night,random,protection(guard));
                outcome=damage.outcome();armorHit=planArmorHit(guard,damage.impact());reason=ModernNpcBattle.Reason.ATTACKED;}
            catch(IllegalArgumentException|ArithmeticException failure){outcome=null;reason=failure instanceof ModernNpcMelee.CombatException e?npcFailure(e):ModernNpcBattle.Reason.NUMERIC;}
            now=advance();if(phase!=Phase.ACTIVE)return finish.apply(ModernNpcBattle.Reason.NOT_ACTIVE);
            if(hp!=target.hp||moveSequence!=target.sequence||!position.equals(target.position)||!ledger.equals(target.regen)||!Objects.equals(before,npcActor(id,life)))return finish.apply(ModernNpcBattle.Reason.STALE_STATE);
            if(outcome==null)return finish.apply(reason);
            // Time alone may have advanced the roster revision while RNG ran.
            // Rebuild geometry from live state; never re-roll damage or reuse a
            // stale Plan, and never choose another victim for the old outcome.
            plan=npcPlan(id,life,budget);
            if(plan.kind()!=ModernNpcRoster.PlanKind.IN_RANGE||!targetId.equals(plan.target()))return finish.apply(ModernNpcBattle.Reason.STALE_STATE);
            if(!equipmentCurrent())return finish.apply(ModernNpcBattle.Reason.NOT_ACTIVE);
            ArmorProtectionView nextArmor=null;
            if(armorHit!=null) {
                now=advanceTemporal();if(phase!=Phase.ACTIVE)return finish.apply(ModernNpcBattle.Reason.NOT_ACTIVE);
                try{nextArmor=acceptArmorHit(armorHit,now);}catch(IllegalArgumentException|ArithmeticException failure){return finish.apply(ModernNpcBattle.Reason.STALE_STATE);}
            }
            ModernRegenProtocol.State nextRegen=target.regen;ModernQuestLife.State nextLife=target.questLife;
            ModernTimedEffects.State nextBuffs=target.buffs;
            ModernVitalBalance.State resources;
            try{resources=planDamage(target,Math.incrementExact(revision),outcome.appliedDamage(),outcome.remainingHp());
                if(regenEnabled)nextRegen=nextRegen.damaged(outcome.appliedDamage());
                if(nextLife!=null&&outcome.death())nextLife=nextLife.died(Math.incrementExact(revision),position);
                if(timedEnabled&&outcome.death()&&nextBuffs.hasEffects())
                    nextBuffs=ModernTimedEffects.clear(nextBuffs,Math.max(nextBuffs.observedNanos(),elapsed(now)),Math.incrementExact(revision));}
            catch(ArithmeticException|IllegalArgumentException failure){return finish.apply(ModernNpcBattle.Reason.NUMERIC);}
            long next=Math.incrementExact(revision);
            var exchange=new ModernNpcBattle.Exchange(ModernNpcBattle.Direction.NPC_TO_PARTICIPANT,targetId,participantLife(target),id,life,0,next,outcome);
            var nextTimeAttack=timeAttackState!=null&&outcome.death()?ModernTimeAttack.leaderDefeated(timeAttackState,target.entityId,next):timeAttackState;
            var terminalBuffs=planTimeAttackClear(nextTimeAttack,now,next);
            npcRoster.attack(npcScope,plan,npcTarget(target),npcObstacleCells(before.lane()),terminalBuffs!=null);
            nextRevision();target.hp=outcome.remainingHp();target.equipmentResources=resources;target.regen=nextRegen;target.questLife=nextLife;target.buffs=nextBuffs;
            commitArmorHit(target,nextArmor);
            if(outcome.appliedDamage()>0)target.hurtRevision=revision;
            if(outcome.death())target.deathRevision=revision;
            timeAttackState=nextTimeAttack;
            if(timeAttackFinished(timeAttackState))finishTimeAttack(terminalBuffs);
            npcExchanges.put(id,exchange);
            return finish.apply(ModernNpcBattle.Reason.ATTACKED);
        },()->finish.apply(ModernNpcBattle.Reason.NOT_ACTIVE));
    }
    private static ModernNpcBattle.Reason npcFailure(ModernNpcMelee.CombatException failure) {
        return failure.failure()==ModernNpcMelee.Failure.UNSUPPORTED?ModernNpcBattle.Reason.UNSUPPORTED:ModernNpcBattle.Reason.NUMERIC;
    }
    private ModernNpcRoster.Plan npcPlan(ModernNpcRoster.Id id,long life,int budget) {
        int lane=npcActor(id,life).lane();
        return npcRoster.pursue(npcScope,id,life,actors.stream().filter(a->sameForce(a.member.team(),lane)).map(this::npcTarget).toList(),npcObstacleCells(lane),budget,()->!preparation.isLive());
    }
    private ModernNpcRoster.Target npcTarget(MutableActor actor){return new ModernNpcRoster.Target(actor.member.identity(),actor.member.team(),actor.position,actor.hp>0&&participating(actor));}
    private boolean participating(MutableActor actor){return timeAttackState==null||!timeAttackState.departed().contains(actor.entityId);}
    private boolean isolatedForces(){return npcConfig!=null&&npcConfig.questPlan()!=null;}
    private boolean questObjectivesEnabled(){return isolatedForces()&&npcConfig.questPlan().objectives()!=null;}
    private ModernMatchSettlement.Event questVictory(ModernQuestProgress.State progress,long next) {
        if(progress.status()!=ModernQuestProgress.Status.FINISHED)return null;int winner=progress.winningLane();
        var rows=actors.stream().map(a->new ModernMatchSettlement.Participant(a.entityId,a.member.identity(),a.member.team(),
                a.member.team()==winner?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
        return new ModernMatchSettlement.Event(java.util.UUID.randomUUID(),preparation.matchId(),preparation.generation(),next,
                2,preparation.snapshot().summary().config().mapId(),lastObservedEpochMillis,rows);
    }
    private boolean questLivesEnabled(){return questObjectivesEnabled()&&npcConfig.questPlan().participantLives();}
    private static long participantLife(MutableActor actor){return actor.questLife==null?1:actor.questLife.incarnation();}
    /** A confirmation is for this exact authoritative death, not a client HP,
     * coordinate, attack sequence or a request to resurrect another player. */
    synchronized ModernQuestLife.Result confirmQuestDeath(TournamentPresence.Owner owner,ModernRooms.Scope scope,
            long matchId,long generation,long incarnation,long deathRevision) {
        var actor=requireActor(owner,scope,matchId,generation);requireNpc();
        if(!questLivesEnabled()||incarnation<1||deathRevision<1)throw new SecurityException("Quest life capability mismatch");
        return guarded(()->{
            advance();if(phase!=Phase.ACTIVE)return questLifeResult(ModernQuestLife.Reason.NOT_ACTIVE);
            var before=actor.questLife;
            if(before.incarnation()!=incarnation||before.deathRevision()!=deathRevision)return questLifeResult(ModernQuestLife.Reason.STALE_LIFE);
            if(!before.dead()||actor.hp!=0)return questLifeResult(ModernQuestLife.Reason.NOT_DEAD);
            var spawn=before.spawn();int lane=actor.member.team();
            if(actors.stream().anyMatch(a->a!=actor&&sameForce(lane,a.member.team())&&a.position.equals(spawn))
                    ||npcRoster.snapshot().actors().stream().anyMatch(n->n.alive()&&sameForce(lane,n.lane())&&n.cell().equals(spawn))
                    ||questObjectiveOccupies(spawn,lane))return questLifeResult(ModernQuestLife.Reason.SPAWN_BLOCKED);
            ModernQuestLife.State after;ModernVitalBalance.State resources;long move;
            try{after=before.revived(Math.incrementExact(revision),actor.loadout.combatant().maxHp());move=Math.incrementExact(actor.sequence);
                resources=planRespawn(actor,Math.incrementExact(revision));}
            catch(ArithmeticException|IllegalArgumentException failure){return questLifeResult(ModernQuestLife.Reason.NUMERIC);}
            if(!equipmentCurrent())return questLifeResult(ModernQuestLife.Reason.NOT_ACTIVE);
            nextRevision();actor.questLife=after;actor.hp=resources.vitals().hp();actor.equipmentResources=resources;actor.position=spawn;actor.heading=0;actor.sequence=move;
            syncArmorLife(actor,revision);
            return questLifeResult(ModernQuestLife.Reason.REVIVED);
        },()->questLifeResult(ModernQuestLife.Reason.NOT_ACTIVE));
    }
    /** Explicit dead-owner seam. Do not bypass the living-action fence or
     * convert canonical death revisions into legacy wire revisions. */
    synchronized ModernQuestLife.Result confirmCanonicalQuestDeath(TournamentPresence.Owner owner,
            ModernBattleCommandLife.Proof proof,long deathRevision) {
        Objects.requireNonNull(proof);
        var actor=requireActor(owner,proof.player(),proof.world().matchId(),proof.world().generation());
        if(proof.world().roomId()!=preparation.snapshot().summary().id()||proof.entity()!=actor.entityId
                ||proof.revision()>revision||deathRevision<1||deathRevision>proof.revision())
            throw new SecurityException("death confirmation identity mismatch");
        return confirmQuestDeath(owner,proof.player(),proof.world().matchId(),proof.world().generation(),proof.life(),deathRevision);
    }
    private ModernQuestLife.Result questLifeResult(ModernQuestLife.Reason reason){return new ModernQuestLife.Result(reason,capture());}
    private boolean timeAttackTowerOccupies(ModernWorldMap.Cell cell){return timeAttackState!=null&&timeAttackState.towerHp()>0&&timeAttackState.plan().tower().cell().equals(cell);}
    private boolean questObjectiveOccupies(ModernWorldMap.Cell cell,int lane){var actor=questObjectives.get(lane);return timeAttackTowerOccupies(cell)||actor!=null&&actor.alive()&&actor.cell().equals(cell);}
    private Set<ModernWorldMap.Cell> npcObstacleCells(int lane){var cells=new HashSet<>(npcPlayerCells(lane));var objective=questObjectives.get(lane);if(objective!=null&&objective.alive())cells.add(objective.cell());if(timeAttackState!=null&&timeAttackState.towerHp()>0)cells.add(timeAttackState.plan().tower().cell());return Set.copyOf(cells);}
    private boolean sameForce(int a,int b){return !isolatedForces()||a==b;}
    private Set<ModernWorldMap.Cell> npcPlayerCells(){return npcPlayerCells(-1);}
    private Set<ModernWorldMap.Cell> npcPlayerCells(int lane){
        if(isolatedForces()&&(lane<0||lane>1))throw invalid();
        return actors.stream().filter(a->participating(a)&&sameForce(a.member.team(),lane)).map(a->a.position).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    private ModernNpcRoster.Actor npcActor(ModernNpcRoster.Id id,long life) {
        var frame=npcRoster.snapshot();if(id.serial()>frame.spawned())throw new SecurityException("unknown NPC identity");
        var actor=frame.actors().stream().filter(a->a.id().equals(id)).findFirst().orElse(null);
        if(actor!=null&&actor.life()!=life)throw new SecurityException("stale NPC life");return actor;
    }
    private void requireNpc(){requireOpen();if(npcRoster==null)throw new IllegalStateException("NPC authority unavailable");}
    private void requireNpcScope(ModernNpcRoster.Scope scope){requireNpc();if(!npcScope.equals(scope))throw new SecurityException("NPC world mismatch");}
    private void requireNpcId(ModernNpcRoster.Id id,long life){requireNpc();if(id==null||!npcScope.equals(id.scope())||life<1)throw new SecurityException("NPC target mismatch");}
    private boolean npcOccupies(ModernWorldMap.Cell cell,int lane){return npcRoster!=null&&npcRoster.snapshot().actors().stream().anyMatch(a->a.alive()&&sameForce(a.lane(),lane)&&a.cell().equals(cell));}
    private boolean npcCellOccupied(ModernWorldMap.Cell cell,int lane){return questObjectiveOccupies(cell,lane)||npcOccupies(cell,lane)||actors.stream().anyMatch(a->participating(a)&&sameForce(a.member.team(),lane)&&a.position.equals(cell));}
    private boolean npcStepOpen(ModernWorldMap.Cell from,ModernWorldMap.Cell to,int lane) {
        if(!map.canStep(from,to))return false;
        if(from.x()==to.x()||from.y()==to.y())return true;
        var a=new ModernWorldMap.Cell(to.x(),from.y());var b=new ModernWorldMap.Cell(from.x(),to.y());
        return !npcCellOccupied(a,lane)&&map.canStep(from,a)&&map.canStep(a,to)||!npcCellOccupied(b,lane)&&map.canStep(from,b)&&map.canStep(b,to);
    }
    private boolean npcLineOpen(ModernWorldMap.Cell from,ModernWorldMap.Cell target,int lane) {
        if(!strikeLineOpen(from,target))return false;
        long dx=Math.abs((long)target.x()-from.x()),dy=Math.abs((long)target.y()-from.y()),error=dx-dy;
        int sx=Integer.compare(target.x(),from.x()),sy=Integer.compare(target.y(),from.y());var previous=from;
        for(int n=0;n<16;n++) {
            int x=previous.x(),y=previous.y();long twice=error*2;
            if(twice>-dy){error-=dy;x+=sx;}if(twice<dx){error+=dx;y+=sy;}
            var next=new ModernWorldMap.Cell(x,y);
            if(!npcStepOpen(previous,next,lane)||!next.equals(target)&&npcCellOccupied(next,lane))return false;
            if(next.equals(target))return true;previous=next;
        }
        return false;
    }
    private ModernNpcBattle.Frame npcCurrent() {
        return ModernLegacyBattleProjection.npcs(capture());
    }
    private ModernNpcBattle.Result npcResult(ModernNpcBattle.Reason reason){return new ModernNpcBattle.Result(reason,capture());}

    synchronized AmmoResult ammo(TournamentPresence.Owner owner,ModernAmmoProtocol.Request request) {
        var actor=requireActor(owner,request.scope(),request.matchId(),request.generation());
        return guarded(()->{
            advance();if(!bowEnabled)return new AmmoResult(ModernAmmoProtocol.Reason.NOT_ACTIVE,capture());
            if(request.action()==ModernAmmoProtocol.Action.READ)return new AmmoResult(ModernAmmoProtocol.Reason.NONE,capture());
            if(phase!=Phase.ACTIVE)return new AmmoResult(ModernAmmoProtocol.Reason.NOT_ACTIVE,capture());
            if(actor.ammo.requestSequence()==Long.MAX_VALUE||request.sequence()!=actor.ammo.requestSequence()+1)throw new SecurityException("arrow selection sequence mismatch");
            ModernAmmoProtocol.Reason reason=actor.hp==0?ModernAmmoProtocol.Reason.DEAD:
                    ModernAmmo.Kind.find(request.itemId())==null?ModernAmmoProtocol.Reason.UNSUPPORTED:
                    actor.ammo.stocks().stream().noneMatch(s->s.itemId()==request.itemId()&&s.remaining()>0)?ModernAmmoProtocol.Reason.EMPTY:ModernAmmoProtocol.Reason.NONE;
            if(!equipmentCurrent())return new AmmoResult(ModernAmmoProtocol.Reason.NOT_ACTIVE,capture());
            var next=actor.ammo.selected(request.sequence(),Math.incrementExact(revision),request.itemId(),reason==ModernAmmoProtocol.Reason.NONE);
            nextRevision();actor.ammo=next;return new AmmoResult(reason,capture());
        },()->new AmmoResult(bowEnabled&&request.action()==ModernAmmoProtocol.Action.READ
                ?ModernAmmoProtocol.Reason.NONE:ModernAmmoProtocol.Reason.NOT_ACTIVE,capture()));
    }
    /** Match-local use: quantity, healing, shared potion-group cooldown and event commit together. */
    synchronized ItemResult item(TournamentPresence.Owner owner, ModernItemProtocol.Request request) {
        MutableActor actor=requireActor(owner,request.scope(),request.matchId(),request.generation());
        return guarded(()-> {
            // Retire live item authority before timed-expiry planning tries
            // to derive a revision which no longer fits. Terminal reads stay immutable.
            if(phase!=Phase.FINISHED&&phase!=Phase.ABORTED&&revision==Long.MAX_VALUE)nextRevision();
            long now=advance();
            if(!itemsEnabled || phase==Phase.ABORTED || phase==Phase.FINISHED)return used(ModernItemProtocol.Reason.NOT_ACTIVE);
            if(request.action()==ModernItemProtocol.Action.READ)return used(ModernItemProtocol.Reason.NONE);
            var old=actor.itemUse;
            old.validate(revision,actor.itemHistory);
            if(old.requestSequence()==Long.MAX_VALUE || request.sequence()!=old.requestSequence()+1)
                throw new SecurityException("match item sequence mismatch");
            var kind=ModernItemProtocol.HealingKind.find(request.itemId());
            var timed=timedEnabled?ModernTimedEffects.Kind.find(request.itemId()):null;
            var stock=old.stocks().stream().filter(s->s.itemId()==request.itemId()).findFirst().orElse(null);
            ModernItemProtocol.Reason reason;
            long healed=0,nextHp=actor.hp,rolledBase=0;
            boolean resolved=false;
            long hpRegenRevision=actor.regen.hpRevision();
            if(phase!=Phase.ACTIVE)reason=ModernItemProtocol.Reason.NOT_ACTIVE;
            else if(actor.hp==0)reason=ModernItemProtocol.Reason.DEAD;
            else if(kind==null && timed==null)reason=ModernItemProtocol.Reason.UNSUPPORTED;
            else if(stock==null || stock.remaining()==0)reason=ModernItemProtocol.Reason.EMPTY;
            else if(timed!=null&&!timed.eligible(actor.member.characterClass()))reason=ModernItemProtocol.Reason.CLASS_RESTRICTED;
            else if(kind!=null && actor.healed && now-actor.lastHealingNanos<TimeUnit.MILLISECONDS.toNanos(actor.healingDelayMillis))reason=ModernItemProtocol.Reason.TOO_FAST;
            else if(timed!=null)reason=ModernItemProtocol.Reason.NONE;
            else {
                // Only the explicit NEW_MATCH policy is active: no retained
                // percentage producer/buff/status is fabricated from saved SQL.
                // Qualified positive equipment PotionRecovery follows UserAddHp.
                try {
                    resolved=true;
                    long bound=Math.addExact(Math.subtractExact(kind.maximum,kind.minimum),1);
                    long rolled=random.nextLong(bound);
                    if(rolled<0 || rolled>=bound)throw new ArithmeticException("invalid healing random");
                    long amount=Math.addExact(kind.minimum,rolled);
                    rolledBase=amount;
                    long fixed=actor.itemHistory.fixedRecovery();
                    if(fixed>0)amount=Math.addExact(amount,fixed);
                    healed=Math.min(amount,Math.subtractExact(actor.loadout.combatant().maxHp(),actor.hp));
                    nextHp=Math.addExact(actor.hp,healed);
                    reason=ModernItemProtocol.Reason.NONE;
                } catch(ArithmeticException failure) { reason=ModernItemProtocol.Reason.NUMERIC; }
            }
            // A slow/invalid random result cannot outlive the world or equipment
            // deadline. Cancellation wins even over the numeric-error response.
            if(resolved) { now=advance();if(phase!=Phase.ACTIVE||!participating(actor))return used(ModernItemProtocol.Reason.NOT_ACTIVE);
                if(actor.itemUse!=old)throw new SecurityException("reentrant match item request");
                if(regenEnabled&&actor.regen.hpRevision()!=hpRegenRevision)reason=ModernItemProtocol.Reason.STALE_REGEN; }
            if(!equipmentCurrent())return used(ModernItemProtocol.Reason.NOT_ACTIVE);
            if(revision==Long.MAX_VALUE)nextRevision(); // Retire before planning an impossible request revision.
            ModernItemRuntime.State next;
            var nextRegen=actor.regen;
            if(regenEnabled&&reason==ModernItemProtocol.Reason.NONE)try{nextRegen=actor.regen.healed(healed);}
                catch(ArithmeticException failure){reason=ModernItemProtocol.Reason.NUMERIC;}
            var nextBuffs=actor.buffs;
            var nextHistory=actor.itemHistory;
            ModernBattlePresentation.Item itemPresentation=null;
            var resources=actor.equipmentResources;
            if(reason==ModernItemProtocol.Reason.NONE)try {
                requireResourceBalance(actor);
                resources=actor.equipmentResources.restore(Math.incrementExact(revision),healed);
                if(resources.vitals().hp()!=nextHp)throw invalid();
                nextHistory=actor.itemHistory.use(Math.incrementExact(revision),request.sequence(),request.itemId(),useAnchor(actor),rolledBase);
                if(nextHistory.last().restored()!=healed||nextHistory.last().hpAfter()!=nextHp)throw invalid();
                itemPresentation=ModernBattlePresentation.Item.from(nextHistory,now-loadingStartedNanos);
            }catch(ArithmeticException|IllegalArgumentException failure){reason=ModernItemProtocol.Reason.NUMERIC;}
            if(reason==ModernItemProtocol.Reason.NONE) {
                next=old.used(Math.incrementExact(revision),request.sequence(),actor.itemHistory,nextHistory);
                if(timed!=null)nextBuffs=ModernTimedEffects.apply(actor.buffs,timed,elapsed(now),Math.addExact(revision,1),actor.member.characterClass());
            } else next=old.requested(Math.incrementExact(revision),request.sequence());
            var nextWeight=actor.loadout.weightBasis()==null?null:actor.loadout.reweight(next.stocks(),actor.ammo==null?List.of():actor.ammo.stocks(),actor.weight.units(),materialStocks(actor));
            nextRevision();actor.itemUse=next;actor.weight=nextWeight;actor.buffs=nextBuffs;
            if(reason==ModernItemProtocol.Reason.NONE) {
                actor.hp=nextHp;
                actor.equipmentResources=resources;
                actor.itemHistory=nextHistory;
                actor.presentation=actor.presentation.withItem(itemPresentation);
                if(regenEnabled)actor.regen=nextRegen;
                if(kind!=null){actor.healed=true;actor.lastHealingNanos=now;actor.healingDelayMillis=kind.delayMillis;}
            }
            return used(reason);
        },()->used(ModernItemProtocol.Reason.NOT_ACTIVE));
    }

    /** No target coordinate or native pending-portal/cursor handshake exists in modern casting. */
    synchronized CastResult cast(TournamentPresence.Owner owner, ModernSpellProtocol.Request request) {
        MutableActor actor = requireActor(owner, request.scope(), request.matchId(), request.generation());
        if(request.skillId()==70)return fireStorm(actor,request);
        if(request.targetEntityId()>0) return damageCast(actor,request);
        return guarded(() -> {
            long now = advance();
            if (!spellsEnabled || phase == Phase.ABORTED || phase == Phase.FINISHED) return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
            if (actor.spellSequence() == Long.MAX_VALUE || request.sequence() != actor.spellSequence() + 1)
                throw new SecurityException("spell request sequence mismatch");
            ModernSpellProtocol.Reason reason;
            ModernWorldMap.Cell destination = null;
            boolean randomAttempted = false;
            long timedRevision=actor.buffs.revision();
            long mpRegenRevision=actor.regen.mpRevision();
            var learned = actor.prepared.skills().skills().stream().filter(s -> s.skillId() == request.skillId()).findFirst();
            if (phase != Phase.ACTIVE) reason = ModernSpellProtocol.Reason.NOT_ACTIVE;
            else if (actor.hp == 0) reason = ModernSpellProtocol.Reason.DEAD;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernSpellProtocol.Reason.TOO_HEAVY;
            else if (learned.isEmpty()) reason = ModernSpellProtocol.Reason.UNLEARNED;
            else if (learned.get().support() != ModernSkills.Support.TELEPORT || !actor.loadout.combatant().physicalSupported()) reason = ModernSpellProtocol.Reason.UNSUPPORTED;
            else if (castCooling(actor, now) || actor.moved && now - actor.lastMoveNanos < pace(actor,ModernTimedEffects.Action.MOVE)
                    || actor.attacked && now - actor.lastAttackNanos < pace(actor,ModernTimedEffects.Action.ATTACK)) reason = ModernSpellProtocol.Reason.TOO_FAST;
            else if (actor.mp < spellCost(actor,ModernUseHistory.Lane.TELEPORT)) reason = ModernSpellProtocol.Reason.NO_MP;
            else {
                try {
                    preflightSpell(actor,ModernUseHistory.Lane.TELEPORT,request.sequence());
                    var occupied = new HashSet<ModernWorldMap.Cell>();
                    for (var value : actors) if(participating(value)&&sameForce(actor.member.team(),value.member.team()))occupied.add(value.position);
                    for(var objective:objectives)occupied.add(objective.cell());
                    if(npcRoster!=null) {
                        for(var npc:npcRoster.snapshot().actors())if(npc.alive()&&sameForce(actor.member.team(),npc.lane()))occupied.add(npc.cell());
                        var objective=questObjectives.get(actor.member.team());if(objective!=null&&objective.alive())occupied.add(objective.cell());
                        if(timeAttackState!=null&&timeAttackState.towerHp()>0)occupied.add(timeAttackState.plan().tower().cell());
                        if(timeAttackState!=null)occupied.addAll(timeAttackState.plan().reservedCells());
                        // Keep future wave/guardian births usable. A teleport
                        // must not park a participant on a not-yet-live spawn.
                        if(isolatedForces()) {
                            for(var wave:npcConfig.questPlan().waves())if(wave.lane()==actor.member.team())for(var spawn:wave.spawns())occupied.add(spawn.cell());
                            if(questObjectivesEnabled())for(var target:npcConfig.questPlan().objectives().targets())if(target.lane()==actor.member.team())occupied.add(target.cell());
                        }
                    }
                    int available = 0;
                    for (var cell : teleportCells) if (!occupied.contains(cell)&&siegeTeleportAllowed(actor.position,cell)) available++;
                    if (available == 0) reason = ModernSpellProtocol.Reason.NO_DESTINATION;
                    else {
                        randomAttempted = true;
                        long choice = random.nextLong(available);
                        if (choice < 0 || choice >= available) throw new ArithmeticException();
                        for (var cell : teleportCells) if (!occupied.contains(cell)&&siegeTeleportAllowed(actor.position,cell) && choice-- == 0) { destination = cell; break; }
                        reason = ModernSpellProtocol.Reason.NONE;
                    }
                } catch (ArithmeticException invalid) { reason = ModernSpellProtocol.Reason.NUMERIC; destination = null; }
            }
            // A slow or hostile random source cannot commit after timeout/expiry/cancellation.
            if (randomAttempted) {
                now = advance();
                if (phase != Phase.ACTIVE||!participating(actor)) return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
                if(actor.spellSequence()>=request.sequence())throw new SecurityException("reentrant spell request");
                if(actor.buffs.revision()!=timedRevision){reason=ModernSpellProtocol.Reason.STALE_EFFECT;destination=null;}
                else if(regenEnabled&&actor.regen.mpRevision()!=mpRegenRevision){reason=ModernSpellProtocol.Reason.STALE_REGEN;destination=null;}
            }
            if (!equipmentCurrent()) return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
            var deniedRuntime=actor.spellRuntime.denied(Math.incrementExact(revision),request.sequence());var runtime=deniedRuntime;
            var history=actor.spellHistory;
            var targets=actor.targetHistory;
            var resources=actor.equipmentResources;
            if(destination!=null)try {
                var committed=planCastHistory(actor,ModernUseHistory.Lane.TELEPORT,Math.incrementExact(revision),request.sequence(),List.of());
                resources=committed.resources();history=committed.spells();targets=committed.targets();
                runtime=planSpellRuntime(actor,committed,ModernUseHistory.Lane.TELEPORT,now,new ModernSpellRuntime.Teleport(destination));
            }
            catch(IllegalArgumentException|ArithmeticException failure){destination=null;reason=ModernSpellProtocol.Reason.NUMERIC;}
            nextRevision(); actor.spellRuntime=destination==null?deniedRuntime:runtime;
            if (destination != null) {
                actor.position = destination;
                actor.mp=resources.vitals().mp();actor.equipmentResources=resources;
                actor.spellHistory=history;actor.targetHistory=targets;
            }
            return casted(reason);
        }, () -> casted(ModernSpellProtocol.Reason.NOT_ACTIVE));
    }
    /** Internal opt-in only. No v1-26 request can call this path. PC/BOT actors
     * are supported; objective/NPC identities must use their own future contract.
     * The preparation, owner, both life tokens and source qualification are all
     * mandatory. Random work cannot partially publish HP, MP, lawful or victory.
     */
    synchronized DisintegrateResult disintegrate(TournamentPresence.Owner owner,ModernDisintegrateState.Request request) {
        var actor=requireActor(owner,request.scope(),request.matchId(),request.generation());
        if(request.targetEntityId()>actors.size()||request.targetEntityId()==actor.entityId)
            throw new SecurityException("invalid disintegrate actor target");
        var target=actors.get(Math.toIntExact(request.targetEntityId()-1));
        return guarded(()->{
            long now=advance();
            if(!spellsEnabled||phase==Phase.ABORTED||phase==Phase.FINISHED)return disintegrated(ModernDisintegrateState.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE||request.sequence()!=actor.spellSequence()+1)
                throw new SecurityException("spell request sequence mismatch");
            var reason=ModernDisintegrateState.Reason.NONE;
            ModernDisintegrate.Outcome outcome=null;
            ArmorHit armorHit=null;
            long effect=actor.buffs.revision(),mpAxis=actor.regen.mpRevision(),hpAxis=target.regen.hpRevision();
            long hp=target.hp,mp=actor.mp,casterHp=actor.hp,sequence=actor.spellSequence();
            var from=actor.position;var to=target.position;var prior=actor.spellHistory.get(ModernUseHistory.Lane.DISINTEGRATE);
            boolean attempted=false;
            if(phase!=Phase.ACTIVE)reason=ModernDisintegrateState.Reason.NOT_ACTIVE;
            else if(actor.hp==0)reason=ModernDisintegrateState.Reason.DEAD;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernDisintegrateState.Reason.TOO_HEAVY;
            else if(!disintegrateEnabled)reason=ModernDisintegrateState.Reason.UNSUPPORTED;
            else if(actor.prepared.disintegrate().status()==ModernDisintegrateCatalog.Status.UNLEARNED)reason=ModernDisintegrateState.Reason.UNLEARNED;
            else if(!actor.prepared.disintegrate().accepts(actor.loadout.disintegrate())||!target.loadout.combatant().magicSupported())reason=ModernDisintegrateState.Reason.UNSUPPORTED;
            else if(request.casterLife()!=incarnation(actor)||request.targetLife()!=incarnation(target))reason=ModernDisintegrateState.Reason.STALE_LIFE;
            else if(target.hp==0)reason=ModernDisintegrateState.Reason.TARGET_DEAD;
            else if(TournamentRoomPolicy.rules(preparation.snapshot().summary().config().mode(),preparation.snapshot().summary().config().mapId()).teamBased()
                    &&actor.member.team()==target.member.team())reason=ModernDisintegrateState.Reason.SAME_TEAM;
            else if(distance(from,to)>ModernDisintegrate.RANGE)reason=ModernDisintegrateState.Reason.OUT_OF_RANGE;
            else if(!strikeLineOpen(from,to))reason=ModernDisintegrateState.Reason.BLOCKED;
            else if(!ModernDisintegrate.lawfulAllows(actor.spellRuntime.lawful()))reason=ModernDisintegrateState.Reason.LAWFUL;
            else if(castCooling(actor,now)||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.spellRuntime.cooling(ModernUseHistory.Lane.DISINTEGRATE,now,TimeUnit.MILLISECONDS.toNanos(ModernDisintegrate.REUSE_MILLIS)))
                reason=ModernDisintegrateState.Reason.TOO_FAST;
            else if(mp<spellCost(actor,ModernUseHistory.Lane.DISINTEGRATE))reason=ModernDisintegrateState.Reason.NO_MP;
            else try {
                // Detect ledger exhaustion before consuming entropy.
                preflightSpell(actor,ModernUseHistory.Lane.DISINTEGRATE,request.sequence());
                ModernDisintegrate.lawfulAfterCast(actor.spellRuntime.lawful());
                attempted=true;
                var guard=armorGuard(target,actor,now);
                var damage=ModernDisintegrate.resolveProtected(actor.loadout.disintegrate().values(),target.loadout.combatant().disintegrateDefense(),hp,random,actor.buffs.spBonus(),protection(guard));
                outcome=damage.outcome();armorHit=planArmorHit(guard,damage.impact());
            }catch(ArithmeticException|IllegalArgumentException failure){outcome=null;reason=ModernDisintegrateState.Reason.NUMERIC;}
            if(attempted) {
                now=advance();
                if(phase!=Phase.ACTIVE)return disintegrated(ModernDisintegrateState.Reason.NOT_ACTIVE);
                // A supplied RNG is not authority to replay a request, move an
                // actor, change a life, or overwrite an independent regen tick.
                if(actor.spellSequence()!=sequence)throw new SecurityException("reentrant spell request");
                if(request.casterLife()!=incarnation(actor)||request.targetLife()!=incarnation(target)) {
                    reason=ModernDisintegrateState.Reason.STALE_LIFE;outcome=null;
                }else if(actor.buffs.revision()!=effect) {reason=ModernDisintegrateState.Reason.STALE_EFFECT;outcome=null;}
                else if(actor.regen.mpRevision()!=mpAxis||target.regen.hpRevision()!=hpAxis) {
                    reason=ModernDisintegrateState.Reason.STALE_REGEN;outcome=null;
                }else if(target.hp!=hp||actor.mp!=mp||actor.hp!=casterHp||!actor.position.equals(from)||!target.position.equals(to)
                        ||actor.spellHistory.get(ModernUseHistory.Lane.DISINTEGRATE)!=prior) {reason=ModernDisintegrateState.Reason.STALE_STATE;outcome=null;}
            }
            if(!preparation.isLive()){abortInternal(AbortReason.PREPARATION_CANCELLED);return disintegrated(ModernDisintegrateState.Reason.NOT_ACTIVE);}
            if(!equipmentCurrent())return disintegrated(ModernDisintegrateState.Reason.NOT_ACTIVE);
            ArmorProtectionView nextArmor=null;
            if(outcome!=null&&armorHit!=null) {
                now=advanceTemporal();if(phase!=Phase.ACTIVE)return disintegrated(ModernDisintegrateState.Reason.NOT_ACTIVE);
                try{nextArmor=acceptArmorHit(armorHit,now);}catch(IllegalArgumentException|ArithmeticException failure){outcome=null;reason=ModernDisintegrateState.Reason.STALE_EFFECT;}
            }
            // Preconstruct EVERY potentially failing object, including final
            // team result and buff removal, before the first gameplay mutation.
            var ledger=target.regen;var life=target.life;
            ModernMatchSettlement.Event completed=null;
            var leases=actors.stream().map(a->a.buffs).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            long next=Math.incrementExact(revision),remainingMp=mp;
            var deniedRuntime=actor.spellRuntime.denied(next,request.sequence());var runtime=deniedRuntime;
            var resources=actor.equipmentResources;var victimResources=target.equipmentResources;
            var history=actor.spellHistory;
            var targets=actor.targetHistory;
            if(outcome!=null)try {
                ledger=target.regen.damaged(outcome.appliedDamage());
                if(outcome.death()&&honorConfig!=null)life=target.life.died(next,elapsedMillis(now),to);
                if(outcome.death()&&timedEnabled&&target.buffs.hasEffects())
                    leases.set(Math.toIntExact(target.entityId-1),ModernTimedEffects.clear(target.buffs,Math.max(target.buffs.observedNanos(),elapsed(now)),next));
                victimResources=planDamage(target,next,outcome.appliedDamage(),outcome.remainingHp());
                var committed=planCastHistory(actor,ModernUseHistory.Lane.DISINTEGRATE,next,request.sequence(),
                        List.of(participantEffect(target,outcome.rawDamage(),outcome.appliedDamage(),outcome.remainingHp())));
                resources=committed.resources();remainingMp=resources.vitals().mp();history=committed.spells();targets=committed.targets();
                runtime=planSpellRuntime(actor,committed,ModernUseHistory.Lane.DISINTEGRATE,now,
                        new ModernSpellRuntime.Disintegrate(outcome.critical(),actor.spellRuntime.lawful(),ModernDisintegrate.lawfulAfterCast(actor.spellRuntime.lawful()),ledger.damageLoss()));
                if(outcome.death()&&settlementEnabled&&honorConfig==null) {
                    var teams=new HashSet<Integer>();
                    for(var a:actors)if(a!=target&&a.hp>0)teams.add(a.member.team());
                    if(teams.size()==1) {
                        int winner=teams.iterator().next();
                        var rows=actors.stream().map(a->new ModernMatchSettlement.Participant(a.entityId,a.member.identity(),a.member.team(),
                                a.member.team()==winner?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
                        var config=preparation.snapshot().summary().config();
                        completed=new ModernMatchSettlement.Event(java.util.UUID.randomUUID(),preparation.matchId(),preparation.generation(),next,
                                config.mode(),config.mapId(),lastObservedEpochMillis,rows);
                        for(int i=0;i<leases.size();i++)if(timedEnabled&&leases.get(i).hasEffects())
                            leases.set(i,ModernTimedEffects.clear(leases.get(i),Math.max(leases.get(i).observedNanos(),elapsed(now)),next));
                    }
                }
            }catch(ArithmeticException|IllegalArgumentException failure){outcome=null;reason=ModernDisintegrateState.Reason.NUMERIC;}
            nextRevision();actor.spellRuntime=outcome==null?deniedRuntime:runtime;
            if(outcome!=null) {
                actor.mp=remainingMp;actor.equipmentResources=resources;actor.spellHistory=history;actor.targetHistory=targets;
                target.hp=outcome.remainingHp();target.equipmentResources=victimResources;target.regen=ledger;target.life=life;
                commitArmorHit(target,nextArmor);
                if(outcome.appliedDamage()>0)target.hurtRevision=revision;
                if(outcome.death())target.deathRevision=revision;
                for(int i=0;i<actors.size();i++)actors.get(i).buffs=leases.get(i);
                if(completed!=null){clearAllArmor(now,revision);result=completed;phase=Phase.FINISHED;}
            }
            return disintegrated(reason);
        },()->disintegrated(ModernDisintegrateState.Reason.NOT_ACTIVE));
    }
    /** Full wire identity checked under the same engine monitor as the cast.
     * Stale life/equipment/position remain typed, sequence-consuming denials. */
    synchronized GreaterHasteResult canonicalGreaterHaste(TournamentPresence.Owner owner,ModernGreaterHasteProtocol.Request request) {
        var proof=request.proof();var actor=requireActor(owner,proof.player(),proof.world().matchId(),proof.world().generation());
        if(proof.world().roomId()!=preparation.snapshot().summary().id()||proof.entity()!=actor.entityId||proof.revision()>revision)
            throw new SecurityException("greater haste identity mismatch");
        return greaterHaste(owner,request.command());
    }
    /** No I/O or entropy: all successful values are built before publication.
     * Request denial advances only the shared spell sequence, never MP/leases. */
    synchronized GreaterHasteResult greaterHaste(TournamentPresence.Owner owner,ModernGreaterHasteCast.Request request) {
        var actor=requireActor(owner,request.scope(),request.matchId(),request.generation());
        return guarded(()->{
            long now=advance();
            if(!spellsEnabled||phase==Phase.ABORTED||phase==Phase.FINISHED)return greaterHasted(ModernGreaterHasteCast.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE||request.sequence()!=actor.spellSequence()+1)
                throw new SecurityException("spell request sequence mismatch");
            // A clock supplier must not change the authority behind the admission checks.
            if(!equipmentCurrent())return greaterHasted(ModernGreaterHasteCast.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE||request.sequence()!=actor.spellSequence()+1)
                throw new SecurityException("reentrant spell request");
            var reason=ModernGreaterHasteCast.Reason.NONE;
            var lane=ModernUseHistory.Lane.GREATER_HASTE;
            if(phase!=Phase.ACTIVE||!participating(actor))reason=ModernGreaterHasteCast.Reason.NOT_ACTIVE;
            else if(actor.hp==0)reason=ModernGreaterHasteCast.Reason.DEAD;
            else if(!greaterHasteEnabled)reason=ModernGreaterHasteCast.Reason.UNSUPPORTED;
            else if(actor.prepared.greaterHaste().status()==ModernGreaterHasteCatalog.Status.UNLEARNED)reason=ModernGreaterHasteCast.Reason.UNLEARNED;
            else if(!actor.prepared.greaterHaste().accepts(actor.loadout.greaterHaste()))reason=ModernGreaterHasteCast.Reason.UNSUPPORTED;
            else if(request.equipmentRevision()!=actor.equipmentResources.equipmentRevision()
                    ||request.life()!=Math.incrementExact(resourceLife(actor))||request.x()!=actor.position.x()||request.y()!=actor.position.y()
                    ||request.buffRevision()!=actor.buffs.revision())reason=ModernGreaterHasteCast.Reason.STALE_STATE;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernGreaterHasteCast.Reason.TOO_HEAVY;
            else if(castCooling(actor,now)||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.spellRuntime.cooling(lane,now,TimeUnit.MILLISECONDS.toNanos(ModernGreaterHaste.REUSE_MILLIS)))reason=ModernGreaterHasteCast.Reason.TOO_FAST;
            else if(actor.mp<spellCost(actor,lane))reason=ModernGreaterHasteCast.Reason.NO_MP;
            long next=Math.incrementExact(revision);
            var denied=actor.spellRuntime.denied(next,request.sequence());var runtime=denied;
            ModernEquipmentHistory.State committed=null;var lease=actor.buffs;
            if(reason==ModernGreaterHasteCast.Reason.NONE)try {
                committed=planCastHistory(actor,lane,next,request.sequence(),List.of());
                lease=ModernTimedEffects.applyGreaterHaste(actor.buffs,elapsed(now),next);
                runtime=planSpellRuntime(actor,committed,lane,now,lease.hasteUntilNanos()==0
                        ?new ModernSpellRuntime.GreaterHasteNeutralized(elapsed(now),actor.buffs,lease)
                        :new ModernSpellRuntime.GreaterHaste(elapsed(now),lease.hasteUntilNanos()));
            }catch(ArithmeticException|IllegalArgumentException failure){reason=ModernGreaterHasteCast.Reason.NUMERIC;}
            nextRevision();actor.spellRuntime=reason==ModernGreaterHasteCast.Reason.NONE?runtime:denied;
            if(reason==ModernGreaterHasteCast.Reason.NONE) {
                actor.equipmentResources=committed.resources();actor.mp=committed.resources().vitals().mp();
                actor.spellHistory=committed.spells();actor.targetHistory=committed.targets();actor.buffs=lease;
            }
            return greaterHasted(reason);
        },()->greaterHasted(ModernGreaterHasteCast.Reason.NOT_ACTIVE));
    }
    private GreaterHasteResult greaterHasted(ModernGreaterHasteCast.Reason reason){return new GreaterHasteResult(reason,capture());}
    /** Full identity admission and command execute under the same monitor. */
    synchronized HasteResult canonicalHaste(TournamentPresence.Owner owner,ModernHasteProtocol.Request request) {
        var proof=request.proof();var actor=requireActor(owner,proof.player(),proof.world().matchId(),proof.world().generation());
        if(proof.world().roomId()!=preparation.snapshot().summary().id()||proof.entity()!=actor.entityId||proof.revision()>revision)
            throw new SecurityException("haste identity mismatch");
        return haste(owner,request.command());
    }
    /** Target-capable skill43 transaction; publication retains both receipts. */
    synchronized HasteResult haste(TournamentPresence.Owner owner,ModernHasteCast.Request request) {
        var caster=requireActor(owner,request.scope(),request.matchId(),request.generation());
        MutableActor playerTarget=null;
        if(request.target() instanceof ModernHasteCast.Player p) {
            long entity=p.participant().entityId();
            if(entity>actors.size()||!actors.get(Math.toIntExact(entity-1)).member.identity().equals(p.participant().identity()))
                throw new SecurityException("haste target identity mismatch");
            playerTarget=actors.get(Math.toIntExact(entity-1));
        } else if(request.target() instanceof ModernHasteCast.Npc n) {
            var scope=n.id().scope();
            if(scope.roomId()!=preparation.snapshot().summary().id()||scope.matchId()!=preparation.matchId()||scope.generation()!=preparation.generation())
                throw new SecurityException("haste NPC world mismatch");
        }
        final var target=playerTarget;
        return guarded(()->{
            long now=advance();
            if(!spellsEnabled||phase==Phase.ABORTED||phase==Phase.FINISHED)return hasted(ModernHasteCast.Reason.NOT_ACTIVE);
            if(caster.spellSequence()==Long.MAX_VALUE||request.sequence()!=caster.spellSequence()+1)
                throw new SecurityException("spell request sequence mismatch");
            if(!equipmentCurrent())return hasted(ModernHasteCast.Reason.NOT_ACTIVE);
            if(caster.spellSequence()==Long.MAX_VALUE||request.sequence()!=caster.spellSequence()+1)
                throw new SecurityException("reentrant haste request");
            // Synchronize a scheduler-only NPC step before replacing its
            // current grant: the preceding walk may still need the old one.
            if(hasteEnabled)capture();
            var lane=ModernUseHistory.Lane.HASTE;
            var reason=ModernHasteCast.Reason.NONE;
            ModernNpcRoster.Actor npc=null;
            if(phase!=Phase.ACTIVE||!participating(caster))reason=ModernHasteCast.Reason.NOT_ACTIVE;
            else if(caster.hp==0)reason=ModernHasteCast.Reason.DEAD;
            else if(!hasteEnabled)reason=ModernHasteCast.Reason.UNSUPPORTED;
            else if(caster.prepared.haste().status()==ModernHasteCatalog.Status.UNLEARNED)reason=ModernHasteCast.Reason.UNLEARNED;
            else if(!caster.prepared.haste().accepts(caster.loadout.haste()))reason=ModernHasteCast.Reason.UNSUPPORTED;
            else if(request.equipmentRevision()!=caster.equipmentResources.equipmentRevision()
                    ||request.life()!=Math.incrementExact(resourceLife(caster))||!request.cell().equals(caster.position)
                    ||request.buffRevision()!=caster.buffs.revision())reason=ModernHasteCast.Reason.STALE_STATE;
            if(reason==ModernHasteCast.Reason.NONE) {
                if(request.target() instanceof ModernHasteCast.Player p) {
                    if(!participating(target)||!sameForce(caster.member.team(),target.member.team()))reason=ModernHasteCast.Reason.WRONG_INSTANCE;
                    else if(p.life()!=Math.incrementExact(resourceLife(target))||p.equipmentRevision()!=target.equipmentResources.equipmentRevision()
                            ||!p.cell().equals(target.position)||p.buffRevision()!=target.buffs.revision())reason=ModernHasteCast.Reason.STALE_TARGET;
                    else if(target.hp==0)reason=ModernHasteCast.Reason.TARGET_DEAD;
                } else if(request.target() instanceof ModernHasteCast.Npc n) {
                    if(npcRoster==null)reason=ModernHasteCast.Reason.UNSUPPORTED;
                    else {
                        npc=npcRoster.snapshot().actors().stream().filter(a->a.id().equals(n.id())).findFirst().orElse(null);
                        if(npc==null||npc.life()!=n.life()||npc.changedRevision()!=n.changedRevision()||!npc.cell().equals(n.cell()))reason=ModernHasteCast.Reason.STALE_TARGET;
                        else if(!sameForce(caster.member.team(),npc.lane()))reason=ModernHasteCast.Reason.WRONG_INSTANCE;
                        else if(!npc.alive())reason=ModernHasteCast.Reason.TARGET_DEAD;
                    }
                }
            }
            if(reason==ModernHasteCast.Reason.NONE) {
                if(!ModernHasteCast.inScreen(caster.position,request.target().cell()))reason=ModernHasteCast.Reason.OUT_OF_RANGE;
                else if(caster.weight!=null&&!caster.weight.view().permitsAttackCast())reason=ModernHasteCast.Reason.TOO_HEAVY;
                else if(castCooling(caster,now)||caster.moved&&now-caster.lastMoveNanos<pace(caster,ModernTimedEffects.Action.MOVE)
                        ||caster.attacked&&now-caster.lastAttackNanos<pace(caster,ModernTimedEffects.Action.ATTACK)
                        ||caster.spellRuntime.cooling(lane,now,TimeUnit.MILLISECONDS.toNanos(ModernHaste.REUSE_MILLIS)))reason=ModernHasteCast.Reason.TOO_FAST;
                else if(caster.mp<spellCost(caster,lane))reason=ModernHasteCast.Reason.NO_MP;
            }
            long next=Math.incrementExact(revision);
            var denied=caster.spellRuntime.denied(next,request.sequence());var runtime=denied;
            var nextTargets=hasteTargets;
            ModernEquipmentHistory.State committed=null;ModernTimedEffects.State lease=null;ModernNpcHaste.Incoming npcPlan=null;
            if(reason==ModernHasteCast.Reason.NONE)try {
                committed=planCastHistory(caster,lane,next,request.sequence(),List.of());
                ModernHasteReceipt.Target effect;
                if(target!=null) {
                    lease=ModernTimedEffects.applyHaste(target.buffs,elapsed(now),next);
                    effect=new ModernHasteReceipt.Player(new ModernTargetHistory.Participant(target.entityId,target.member.identity()),
                            Math.incrementExact(resourceLife(target)),target.equipmentResources.equipmentRevision(),target.position,target.buffs,lease);
                } else {
                    npcPlan=ModernNpcHaste.plan(npcRoster.snapshot(),npc.id(),npc.life());
                    effect=new ModernHasteReceipt.Npc(npc.id(),npc.life(),npc.lane(),npc.cell(),npcPlan.revision(),npc.speed(),npcPlan.after());
                }
                runtime=planSpellRuntime(caster,committed,lane,now,new ModernHasteReceipt(elapsed(now),effect));
                nextTargets=hasteTargets.cast(runtime.receipts().get(lane));
            }catch(ArithmeticException|IllegalArgumentException failure){reason=ModernHasteCast.Reason.NUMERIC;}
            // All potentially failing preparation is above. The roster is only
            // reachable through this engine monitor; no clock/I/O callback runs
            // between its CAS and the caster assignments below.
            if(reason==ModernHasteCast.Reason.NONE&&npcPlan!=null)npcRoster.commitIncoming(npcPlan);
            nextRevision();caster.spellRuntime=reason==ModernHasteCast.Reason.NONE?runtime:denied;
            if(reason==ModernHasteCast.Reason.NONE) {
                caster.equipmentResources=committed.resources();caster.mp=committed.resources().vitals().mp();
                caster.spellHistory=committed.spells();caster.targetHistory=committed.targets();
                if(target!=null)target.buffs=lease;
                hasteTargets=nextTargets;
            }
            return hasted(reason);
        },()->hasted(ModernHasteCast.Reason.NOT_ACTIVE));
    }
    private HasteResult hasted(ModernHasteCast.Reason reason){return new HasteResult(reason,capture());}
    /** Complete owner/world/painted identity under this monitor; the caller
     * separately requires the explicit player-healing negotiated extension. */
    synchronized PlayerHealingResult canonicalPlayerHeal(TournamentPresence.Owner owner,ModernPlayerHealingProtocol.Request request) {
        var proof=request.proof();var actor=requireActor(owner,proof.player(),proof.world().matchId(),proof.world().generation());
        if(proof.world().roomId()!=preparation.snapshot().summary().id()||proof.entity()!=actor.entityId||proof.revision()>revision)
            throw new SecurityException("player healing identity mismatch");
        return playerHeal(owner,request.command());
    }
    synchronized PlayerHealingResult playerHeal(TournamentPresence.Owner owner,ModernPlayerHealingCast.Request request) {
        var caster=requireActor(owner,request.scope(),request.matchId(),request.generation());
        var id=request.target().participant();
        if(id.entityId()>actors.size()||!actors.get(Math.toIntExact(id.entityId()-1)).member.identity().equals(id.identity()))
            throw new SecurityException("player healing target identity mismatch");
        var target=actors.get(Math.toIntExact(id.entityId()-1));
        return guarded(()->{
            long now=advance();
            if(caster.spellRuntime==null||caster.spellSequence()==Long.MAX_VALUE||request.sequence()!=caster.spellSequence()+1)
                throw new SecurityException("player healing request sequence mismatch");
            if(!equipmentCurrent())return playerHealed(ModernPlayerHealingCast.Reason.NOT_ACTIVE);
            if(request.sequence()!=caster.spellSequence()+1)throw new SecurityException("reentrant healing request");
            var reason=healingAdmission(caster,target,request,now);
            ModernPlayerHealingTransaction.Prepared plan=null;
            ModernEquipmentHistory.State casterBefore=null,targetBefore=null;
            var runtimeBefore=caster.spellRuntime;var casterLoadout=caster.loadout;var targetLoadout=target.loadout;
            if(reason==ModernPlayerHealingCast.Reason.NONE)try {
                casterBefore=equipmentHistory(caster);if(target.prepared!=null)targetBefore=equipmentHistory(target);
                var state=new ModernPlayerHealingTransaction.State(revision,request.scope(),healingActor(caster),healingActor(target),
                        ModernPlayerHealing.qualify(caster.loadout.fighter()),caster.prepared.skills(),
                        caster.playerHealing.outgoing().spending().current(),caster.spellSequence(),caster.spellRuntime.lawful());
                plan=ModernPlayerHealingTransaction.prepare(state,Math.incrementExact(revision),request.sequence(),random);
            }catch(ArithmeticException|IllegalArgumentException rejected){reason=ModernPlayerHealingCast.Reason.NUMERIC;}
            if(plan!=null)now=advance();
            if(!equipmentCurrent()||phase!=Phase.ACTIVE)return playerHealed(ModernPlayerHealingCast.Reason.NOT_ACTIVE);
            if(request.sequence()!=caster.spellSequence()+1)throw new SecurityException("reentrant healing request");
            if(plan!=null) {
                var refreshed=healingAdmission(caster,target,request,now);
                if(refreshed!=ModernPlayerHealingCast.Reason.NONE){reason=refreshed;plan=null;}
                else if(caster.spellRuntime!=runtimeBefore||caster.loadout!=casterLoadout||target.loadout!=targetLoadout
                        ||!plan.before().caster().equals(healingActor(caster))||!plan.before().target().equals(healingActor(target))
                        ||!healingHistoryAt(casterBefore,revision).equals(equipmentHistory(caster))
                        ||targetBefore!=null&&!healingHistoryAt(targetBefore,revision).equals(equipmentHistory(target))) {
                    reason=ModernPlayerHealingCast.Reason.STALE_STATE;plan=null;
                } else try {
                    // An NPC clock/publication can advance without changing
                    // either participant. Stamp the same resolved outcome at
                    // the next world revision after all actor/history checks.
                    plan=ModernPlayerHealingTransaction.atWorldRevision(plan,revision);
                    casterBefore=equipmentHistory(caster);if(targetBefore!=null)targetBefore=equipmentHistory(target);
                } catch(ArithmeticException|IllegalArgumentException rejected) {
                    reason=ModernPlayerHealingCast.Reason.NUMERIC;plan=null;
                }
            }
            long next=Math.incrementExact(revision);var denied=caster.spellRuntime.denied(next,request.sequence());var runtime=denied;
            ModernEquipmentHistory.State casterAfter=null;ModernPlayerHealingHistory.Entry targetHistory=null;
            if(plan!=null)try {
                if(targetBefore!=null) {
                    var complete=ModernPlayerHealingTransaction.complete(plan,casterBefore,targetBefore);
                    casterAfter=complete.caster();targetHistory=complete.target().healing();
                } else {
                    var sent=casterBefore.healing().outgoing().cast(plan.receipt());
                    casterAfter=new ModernEquipmentHistory.State(casterBefore.identity(),next,plan.caster(),casterBefore.spells(),casterBefore.items(),casterBefore.targets(),
                            new ModernPlayerHealingHistory.Entry(casterBefore.healing().incoming(),sent));
                    targetHistory=new ModernPlayerHealingHistory.Entry(target.playerHealing.incoming().receive(plan.receipt().impact()),null);
                    targetHistory.incoming().validate(plan.target());
                }
                runtime=caster.spellRuntime.heal(casterBefore,casterAfter,plan.receipt(),now);
            }catch(ArithmeticException|IllegalArgumentException rejected){reason=ModernPlayerHealingCast.Reason.NUMERIC;plan=null;}
            // No callback, I/O or potentially failing calculation between the
            // one revision commit and publication of both participants.
            nextRevision();caster.spellRuntime=plan==null?denied:runtime;
            if(plan!=null) {
                caster.equipmentResources=casterAfter.resources();caster.playerHealing=casterAfter.healing();
                caster.hp=caster.equipmentResources.vitals().hp();caster.mp=caster.equipmentResources.vitals().mp();
                if(target!=caster) {
                    target.equipmentResources=plan.target();target.playerHealing=targetHistory;
                    target.hp=target.equipmentResources.vitals().hp();target.mp=target.equipmentResources.vitals().mp();
                }
            }
            return playerHealed(reason);
        },()->playerHealed(ModernPlayerHealingCast.Reason.NOT_ACTIVE));
    }
    private ModernPlayerHealingTransaction.Actor healingActor(MutableActor a) {
        // The currently implemented Modern skills do not grant Water Life,
        // Pollute Water, Dirty Current or Healing Erosion. Do not import client
        // flags or persisted native status bits as live authority for them.
        return new ModernPlayerHealingTransaction.Actor(new ModernTargetHistory.World(preparation.snapshot().summary().id(),preparation.matchId(),preparation.generation()),
                new ModernTargetHistory.Participant(a.entityId,a.member.identity()),a.equipmentResources,a.position,0,ModernPlayerHealing.Effects.NONE);
    }
    private static ModernEquipmentHistory.State healingHistoryAt(ModernEquipmentHistory.State captured,long revision) {
        return new ModernEquipmentHistory.State(captured.identity(),revision,captured.resources(),captured.spells(),captured.items(),captured.targets(),captured.healing());
    }
    private ModernPlayerHealingCast.Reason healingAdmission(MutableActor caster,MutableActor target,ModernPlayerHealingCast.Request r,long now) {
        if(phase!=Phase.ACTIVE||!caster.loaded||!participating(caster))return ModernPlayerHealingCast.Reason.NOT_ACTIVE;
        if(caster.hp==0)return ModernPlayerHealingCast.Reason.DEAD;
        if(!playerHealingEnabled)return ModernPlayerHealingCast.Reason.UNSUPPORTED;
        if(!ModernPlayerHealing.qualify(caster.loadout.fighter()).accepts(caster.prepared.skills()))return ModernPlayerHealingCast.Reason.UNLEARNED;
        if(r.life()!=Math.incrementExact(resourceLife(caster))||r.equipmentRevision()!=caster.equipmentResources.equipmentRevision()
                ||!r.cell().equals(caster.position))return ModernPlayerHealingCast.Reason.STALE_STATE;
        if(!target.loaded||!participating(target)||!sameForce(caster.member.team(),target.member.team()))return ModernPlayerHealingCast.Reason.WRONG_INSTANCE;
        if(target.hp==0)return ModernPlayerHealingCast.Reason.TARGET_DEAD;
        if(r.target().life()!=Math.incrementExact(resourceLife(target))||r.target().equipmentRevision()!=target.equipmentResources.equipmentRevision()
                ||!r.target().cell().equals(target.position))return ModernPlayerHealingCast.Reason.STALE_TARGET;
        if(!ModernHonor.inScreen(caster.position.x(),caster.position.y(),target.position.x(),target.position.y()))return ModernPlayerHealingCast.Reason.OUT_OF_RANGE;
        if(caster!=target&&!strikeLineOpen(caster.position,target.position,19))return ModernPlayerHealingCast.Reason.BLOCKED;
        if(caster.weight==null||!caster.weight.view().permitsAttackCast())return ModernPlayerHealingCast.Reason.TOO_HEAVY;
        if(castCooling(caster,now)||caster.moved&&now-caster.lastMoveNanos<pace(caster,ModernTimedEffects.Action.MOVE)
                ||caster.attacked&&now-caster.lastAttackNanos<pace(caster,ModernTimedEffects.Action.ATTACK)
                ||caster.spellRuntime.healingCooling(now,TimeUnit.MILLISECONDS.toNanos(ModernPlayerHealing.REUSE_MILLIS)))return ModernPlayerHealingCast.Reason.TOO_FAST;
        if(caster.mp<caster.playerHealing.outgoing().spending().current().cost())return ModernPlayerHealingCast.Reason.NO_MP;
        return ModernPlayerHealingCast.Reason.NONE;
    }
    private PlayerHealingResult playerHealed(ModernPlayerHealingCast.Reason reason){return new PlayerHealingResult(reason,capture());}
    /** Full identity admission and command execute under the same monitor. */
    synchronized SlowResult canonicalSlow(TournamentPresence.Owner owner,ModernSlowProtocol.Request request) {
        var proof=request.proof();var actor=requireActor(owner,proof.player(),proof.world().matchId(),proof.world().generation());
        if(proof.world().roomId()!=preparation.snapshot().summary().id()||proof.entity()!=actor.entityId||proof.revision()>revision)
            throw new SecurityException("slow identity mismatch");
        return slow(owner,request.command());
    }
    /** Skill29 transaction; public admission also requires v47 and complete data authority. */
    synchronized SlowResult slow(TournamentPresence.Owner owner,ModernSlowCast.Request request) {
        var caster=requireActor(owner,request.scope(),request.matchId(),request.generation());
        MutableActor victim=null;
        if(request.target() instanceof ModernSlowCast.Player p) {
            long entity=p.participant().entityId();
            if(entity>actors.size()||!actors.get(Math.toIntExact(entity-1)).member.identity().equals(p.participant().identity()))
                throw new SecurityException("slow target identity mismatch");
            victim=actors.get(Math.toIntExact(entity-1));
        } else if(request.target() instanceof ModernSlowCast.Npc n) {
            var world=n.id().scope();
            if(world.roomId()!=preparation.snapshot().summary().id()||world.matchId()!=preparation.matchId()||world.generation()!=preparation.generation())
                throw new SecurityException("slow NPC world mismatch");
        }
        final var target=victim;
        return guarded(()->{
            long now=advance();
            if(!spellsEnabled||phase==Phase.ABORTED||phase==Phase.FINISHED)return slowed(ModernSlowCast.Reason.NOT_ACTIVE);
            slowSequence(caster,request);
            if(!preparation.isLive()){abortInternal(AbortReason.PREPARATION_CANCELLED);return slowed(ModernSlowCast.Reason.NOT_ACTIVE);}
            if(slowEnabled)capture(); // retain the preceding NPC walk's grant
            var reason=slowReason(caster,target,request,now);
            var casterResources=caster.equipmentResources;var casterRuntime=caster.spellRuntime;
            var casterLoadout=caster.loadout;var materialBefore=caster.materials;
            var victimResources=target==null?null:target.equipmentResources;
            var victimLoadout=target==null?null:target.loadout;
            long victimLawful=target==null||target.spellRuntime==null?0:target.spellRuntime.lawful();
            var npcBefore=slowNpc(request.target());
            int chance=0,roll=0;long immunity=0;boolean attempted=false;
            ModernSlowNativeRules.MissingEffect missingEffect=null;
            if(reason==ModernSlowCast.Reason.NONE)try {
                preflightSpell(caster,ModernUseHistory.Lane.SLOW,request.sequence());
                attempted=true;
                if(npcBefore!=null)missingEffect=ModernSlowNativeRules.freshMatchNpc(slowNpcRow(npcBefore),npcBefore);
                if(missingEffect==null) {
                    chance=slowChance(caster,target,npcBefore);
                    immunity=target==null||target.prepared==null?0:target.loadout.fighter().hasteItemCount().orElseThrow();
                    long draw=random.nextLong(100);
                    if(draw<0||draw>=100)throw new IllegalArgumentException("invalid slow random");
                    roll=Math.toIntExact(draw)+1;
                }
            }catch(IllegalArgumentException|ArithmeticException failure){reason=ModernSlowCast.Reason.NUMERIC;}
            if(attempted) {
                now=advance();
                if(!preparation.isLive()){abortInternal(AbortReason.PREPARATION_CANCELLED);return slowed(ModernSlowCast.Reason.NOT_ACTIVE);}
                if(phase!=Phase.ACTIVE)return slowed(ModernSlowCast.Reason.NOT_ACTIVE);
                slowSequence(caster,request);
                var rechecked=slowReason(caster,target,request,now);
                if(rechecked!=ModernSlowCast.Reason.NONE)reason=rechecked;
                else if(!casterResources.equals(caster.equipmentResources)||casterRuntime!=caster.spellRuntime
                        ||casterLoadout!=caster.loadout||!materialBefore.equals(caster.materials))reason=ModernSlowCast.Reason.STALE_STATE;
                else if(target!=null&&(!victimResources.equals(target.equipmentResources)||victimLoadout!=target.loadout
                        ||victimLawful!=(target.spellRuntime==null?0:target.spellRuntime.lawful()))
                        ||target==null&&!Objects.equals(npcBefore,slowNpc(request.target())))reason=ModernSlowCast.Reason.STALE_TARGET;
            }
            long next=Math.incrementExact(revision);
            var denied=caster.spellRuntime.denied(next,request.sequence());
            ModernSlowTransaction.Prepared transaction=null;ModernNpcSlow.Incoming npcPlan=null;
            var nextTargets=slowTargets;
            if(reason==ModernSlowCast.Reason.NONE)try {
                boolean apply=missingEffect==null&&roll<=chance&&immunity==0;
                ModernSlowReceipt.Target effect;
                if(target!=null) {
                    var after=apply?ModernTimedEffects.applySlow(target.buffs,elapsed(now),next):target.buffs;
                    effect=new ModernSlowReceipt.Player(new ModernTargetHistory.Participant(target.entityId,target.member.identity()),
                            Math.incrementExact(resourceLife(target)),target.equipmentResources.equipmentRevision(),target.position,target.buffs,after);
                } else {
                    var roster=npcRoster.snapshot();var npc=slowNpc(request.target());
                    if(apply)npcPlan=ModernNpcSlow.plan(roster,npc.id(),npc.life());
                    effect=new ModernSlowReceipt.Npc(npc.id(),npc.life(),npc.lane(),npc.cell(),roster.revision(),npc.speed(),
                            npcPlan==null?npc.speed():npcPlan.after());
                }
                var detail=new ModernSlowReceipt(elapsed(now),chance,roll,immunity,effect,missingEffect);
                transaction=ModernSlowTransaction.prepare(new ModernSlowTransaction.State(equipmentHistory(caster),caster.spellRuntime,caster.materials),
                        caster.loadout.slow(),caster.prepared.slow(),next,request.sequence(),now,caster.position,detail,caster.loadout.weightBasis(),
                        caster.itemUse.stocks(),caster.ammo==null?List.of():caster.ammo.stocks(),caster.weight.units());
                nextTargets=slowTargets.cast(transaction.after().runtime().receipts().get(ModernUseHistory.Lane.SLOW));
            }catch(IllegalArgumentException|ArithmeticException failure){reason=ModernSlowCast.Reason.NUMERIC;transaction=null;npcPlan=null;}
            // Candidates are complete. No external clock, RNG or I/O follows
            // the roster CAS before all caster and player-target assignments.
            if(transaction!=null&&npcPlan!=null)npcRoster.commitIncoming(npcPlan);
            nextRevision();
            if(transaction==null)caster.spellRuntime=denied;
            else {
                var after=transaction.after();caster.equipmentResources=after.history().resources();caster.mp=after.history().resources().vitals().mp();
                caster.spellHistory=after.history().spells();caster.targetHistory=after.history().targets();
                caster.spellRuntime=after.runtime();caster.materials=after.materials();caster.weight=transaction.weight();
                if(target!=null)target.buffs=((ModernSlowReceipt.Player)transaction.effect().target()).after();
                slowTargets=nextTargets;
            }
            return slowed(reason);
        },()->slowed(ModernSlowCast.Reason.NOT_ACTIVE));
    }
    private void slowSequence(MutableActor caster,ModernSlowCast.Request request) {
        if(caster.spellSequence()==Long.MAX_VALUE||request.sequence()!=caster.spellSequence()+1)
            throw new SecurityException("slow spell sequence mismatch");
    }
    private ModernNpcRoster.Actor slowNpc(ModernSlowCast.Target target) {
        if(!(target instanceof ModernSlowCast.Npc n)||npcRoster==null)return null;
        return npcRoster.snapshot().actors().stream().filter(a->a.id().equals(n.id())).findFirst().orElse(null);
    }
    private ModernSlowCast.Reason slowReason(MutableActor caster,MutableActor target,ModernSlowCast.Request request,long now) {
        if(phase!=Phase.ACTIVE||!participating(caster))return ModernSlowCast.Reason.NOT_ACTIVE;
        if(caster.hp==0)return ModernSlowCast.Reason.DEAD;
        if(!slowEnabled)return ModernSlowCast.Reason.UNSUPPORTED;
        if(caster.prepared.slow().status()==ModernSlowCatalog.Status.UNLEARNED)return ModernSlowCast.Reason.UNLEARNED;
        if(!caster.prepared.slow().accepts(caster.loadout.slow()))return ModernSlowCast.Reason.UNSUPPORTED;
        if(request.equipmentRevision()!=caster.equipmentResources.equipmentRevision()||request.life()!=Math.incrementExact(resourceLife(caster))
                ||!request.cell().equals(caster.position)||request.buffRevision()!=caster.buffs.revision())return ModernSlowCast.Reason.STALE_STATE;
        if(request.target() instanceof ModernSlowCast.Player p) {
            if(!participating(target)||!sameForce(caster.member.team(),target.member.team()))return ModernSlowCast.Reason.WRONG_INSTANCE;
            if(p.life()!=Math.incrementExact(resourceLife(target))||p.equipmentRevision()!=target.equipmentResources.equipmentRevision()
                    ||!p.cell().equals(target.position)||p.buffRevision()!=target.buffs.revision())return ModernSlowCast.Reason.STALE_TARGET;
            if(target.hp==0)return ModernSlowCast.Reason.TARGET_DEAD;
            if(target!=caster&&TournamentRoomPolicy.rules(preparation.snapshot().summary().config().mode(),preparation.snapshot().summary().config().mapId()).teamBased()
                    &&caster.member.team()==target.member.team())return ModernSlowCast.Reason.SAME_TEAM;
            if(!target.loadout.combatant().magicSupported())return ModernSlowCast.Reason.UNSUPPORTED;
        } else {
            var n=(ModernSlowCast.Npc)request.target();var npc=slowNpc(n);
            if(npc==null||npc.life()!=n.life()||npc.changedRevision()!=n.changedRevision()||!npc.cell().equals(n.cell()))return ModernSlowCast.Reason.STALE_TARGET;
            if(!sameForce(caster.member.team(),npc.lane()))return ModernSlowCast.Reason.WRONG_INSTANCE;
            if(!npc.alive())return ModernSlowCast.Reason.TARGET_DEAD;
            if(!slowNpcSupported(npc))return ModernSlowCast.Reason.UNSUPPORTED;
        }
        if(!ModernSlow.inSourceRange(caster.position,request.target().cell()))return ModernSlowCast.Reason.OUT_OF_RANGE;
        if(target!=caster&&!strikeLineOpen(caster.position,request.target().cell()))return ModernSlowCast.Reason.BLOCKED;
        if(!caster.weight.view().permitsAttackCast())return ModernSlowCast.Reason.TOO_HEAVY;
        if(castCooling(caster,now)||caster.moved&&now-caster.lastMoveNanos<pace(caster,ModernTimedEffects.Action.MOVE)
                ||caster.attacked&&now-caster.lastAttackNanos<pace(caster,ModernTimedEffects.Action.ATTACK)
                ||caster.spellRuntime.cooling(ModernUseHistory.Lane.SLOW,now,TimeUnit.MILLISECONDS.toNanos(ModernSlow.REUSE_MILLIS)))return ModernSlowCast.Reason.TOO_FAST;
        if(caster.mp<spellCost(caster,ModernUseHistory.Lane.SLOW))return ModernSlowCast.Reason.NO_MP;
        if(!caster.materials.hasSlowMaterial())return ModernSlowCast.Reason.NO_MATERIAL;
        return ModernSlowCast.Reason.NONE;
    }
    private ModernNpcCatalog.Npc slowNpcRow(ModernNpcRoster.Actor npc) {
        var t=npc.template();var row=npcConfig.catalog().require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId()));
        row.validateActor(npc);return row;
    }
    /** Qualified quest-prerequisite NPCs are paid native failures, not an
     * unsupported command. Polymorph-dependent NPC admission remains pending. */
    private boolean slowNpcSupported(ModernNpcRoster.Actor npc) {
        return slowNpcRow(npc).pendingCleanMelee().isEmpty()
                &&(npc.template().npcId()<46069||npc.template().npcId()>46106);
    }
    private int slowChance(MutableActor caster,MutableActor target,ModernNpcRoster.Actor npc) {
        long level,mr;boolean boss=false;
        if(target!=null) {
            if(target.prepared!=null&&target.prepared.slow().gmTarget())return 1;
            level=target.loadout.combatant().slowLevel();mr=target.loadout.combatant().slowMr();
            long lawful=target.spellRuntime==null?0:target.spellRuntime.lawful();
            long guardian=lawful>=10000&&lawful<=19999?3:lawful>=20000&&lawful<=29999?6:lawful>=30000&&lawful<=39999?9:0;
            mr=Math.addExact(mr,guardian);
        } else {
            var t=npc.template();var row=npcConfig.catalog().require(new ModernNpcCatalog.Key(t.catalogSourceId(),t.catalogDatabase(),t.npcId()));
            row.validateActor(npc);level=row.get(ModernNpcCatalog.Field.LEVEL);mr=Math.max(0,row.get(ModernNpcCatalog.Field.MR));boss=row.get(ModernNpcCatalog.Field.BOSS)!=0;
        }
        var fighter=caster.loadout.fighter();
        return ModernSlow.probability(caster.loadout.combatant().slowLevel(),level,fighter.profile().effective().intelligence(),
                fighter.profile().equipmentEvaluation().total(ModernEquipmentFacts.Stat.MAGIC_HIT),mr,boss);
    }
    private SlowResult slowed(ModernSlowCast.Reason reason){return new SlowResult(reason,capture());}
    private long incarnation(MutableActor actor){return honorConfig==null?1:Math.incrementExact(actor.life.respawnCount());}
    private DisintegrateResult disintegrated(ModernDisintegrateState.Reason reason){return new DisintegrateResult(reason,capture());}
    private boolean castCooling(MutableActor actor, long now) {
        // Movement-only/Pet Race worlds and server bots have no spell lane.
        // A prepared human in a spell-enabled world must have the authority.
        if(!spellsEnabled||actor.prepared==null)return false;
        return actor.spellRuntime.cooling(now,pace(actor,ModernTimedEffects.Action.SPELL));
    }
    private boolean siegeTeleportAllowed(ModernWorldMap.Cell from,ModernWorldMap.Cell to){return siegeSpace==null||siegeSpace.teleport(from,to,siegeState.open());}
    /** MP, target HP/death and independent magic event commit under the same guard. */
    private CastResult damageCast(MutableActor actor,ModernSpellProtocol.Request request) {
        if(honorConfig!=null&&request.targetEntityId()>actors.size())return castObjective(actor,request);
        if(request.targetEntityId()>actors.size() || request.targetEntityId()==actor.entityId)
            throw new SecurityException("invalid magic target");
        MutableActor target=actors.get(Math.toIntExact(request.targetEntityId()-1));
        return guarded(()-> {
            long now=advance();
            if(!spellsEnabled || phase==Phase.ABORTED || phase==Phase.FINISHED)return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE || request.sequence()!=actor.spellSequence()+1)
                throw new SecurityException("spell request sequence mismatch");
            var learned=actor.prepared.skills().skills().stream().filter(s->s.skillId()==request.skillId()).findFirst();
            ModernSpellProtocol.Reason reason;
            ModernMagicCombat.Outcome outcome=null;
            ArmorHit armorHit=null;
            boolean resolved=false;
            long timedRevision=actor.buffs.revision();
            long mpRegenRevision=actor.regen.mpRevision(),hpRegenRevision=target.regen.hpRevision();
            if(phase!=Phase.ACTIVE)reason=ModernSpellProtocol.Reason.NOT_ACTIVE;
            else if(actor.hp==0)reason=ModernSpellProtocol.Reason.DEAD;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernSpellProtocol.Reason.TOO_HEAVY;
            else if(learned.isEmpty())reason=ModernSpellProtocol.Reason.UNLEARNED;
            else if(!damageMagicEnabled || learned.get().support()!=ModernSkills.Support.LIGHT_ARROW
                    || !actor.loadout.combatant().magicSupported() || !target.loadout.combatant().magicSupported())reason=ModernSpellProtocol.Reason.UNSUPPORTED;
            else if(target.hp==0)reason=ModernSpellProtocol.Reason.TARGET_DEAD;
            else if(TournamentRoomPolicy.rules(preparation.snapshot().summary().config().mode(),preparation.snapshot().summary().config().mapId()).teamBased()
                    && actor.member.team()==target.member.team())reason=ModernSpellProtocol.Reason.SAME_TEAM;
            else if(distance(actor.position,target.position)>ModernMagicCombat.RANGE)reason=ModernSpellProtocol.Reason.OUT_OF_RANGE;
            else if(!strikeLineOpen(actor.position,target.position))reason=ModernSpellProtocol.Reason.BLOCKED;
            else if(castCooling(actor,now) || actor.moved && now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked && now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK))reason=ModernSpellProtocol.Reason.TOO_FAST;
            else if(actor.mp<spellCost(actor,ModernUseHistory.Lane.LIGHT_ARROW))reason=ModernSpellProtocol.Reason.NO_MP;
            else {
                try {
                    preflightSpell(actor,ModernUseHistory.Lane.LIGHT_ARROW,request.sequence());
                    resolved=true;
                    var guard=armorGuard(target,actor,now);
                    var damage=ModernCombatant.resolveProtectedLightArrow(actor.loadout.combatant(),target.loadout.combatant(),target.hp,random,actor.buffs.spBonus(),protection(guard));
                    outcome=damage.outcome();armorHit=planArmorHit(guard,damage.impact());
                    reason=ModernSpellProtocol.Reason.NONE;
                } catch(IllegalArgumentException|ArithmeticException invalid) {
                    reason=ModernSpellProtocol.Reason.NUMERIC;outcome=null;
                }
            }
            if(resolved) {
                now=advance();if(phase!=Phase.ACTIVE)return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
                if(actor.spellSequence()>=request.sequence())throw new SecurityException("reentrant spell request");
                if(actor.buffs.revision()!=timedRevision){reason=ModernSpellProtocol.Reason.STALE_EFFECT;outcome=null;}
                else if(regenEnabled&&(actor.regen.mpRevision()!=mpRegenRevision||target.regen.hpRevision()!=hpRegenRevision)){
                    reason=ModernSpellProtocol.Reason.STALE_REGEN;outcome=null;}
            }
            if(!equipmentCurrent())return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
            var nextTargetRegen=target.regen;
            ArmorProtectionView nextArmor=null;
            if(outcome!=null&&armorHit!=null) {
                now=advanceTemporal();if(phase!=Phase.ACTIVE)return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
                try{nextArmor=acceptArmorHit(armorHit,now);}catch(IllegalArgumentException|ArithmeticException failure){outcome=null;reason=ModernSpellProtocol.Reason.STALE_EFFECT;}
            }
            var deniedRuntime=actor.spellRuntime.denied(Math.incrementExact(revision),request.sequence());var runtime=deniedRuntime;
            var nextLife=target.life;
            if(regenEnabled&&outcome!=null)try{nextTargetRegen=target.regen.damaged(outcome.appliedDamage());}
                catch(ArithmeticException failure){outcome=null;reason=ModernSpellProtocol.Reason.NUMERIC;}
            if(honorConfig!=null&&outcome!=null&&outcome.death())try{nextLife=target.life.died(Math.incrementExact(revision),elapsedMillis(now),target.position);}
                catch(ArithmeticException|IllegalArgumentException failure){outcome=null;reason=ModernSpellProtocol.Reason.NUMERIC;}
            var history=actor.spellHistory;
            var targets=actor.targetHistory;
            var resources=actor.equipmentResources;var victimResources=target.equipmentResources;
            if(outcome!=null)try {
                var committed=planCastHistory(actor,ModernUseHistory.Lane.LIGHT_ARROW,Math.incrementExact(revision),request.sequence(),
                        List.of(participantEffect(target,outcome.rawDamage(),outcome.appliedDamage(),outcome.remainingHp())));
                resources=committed.resources();history=committed.spells();targets=committed.targets();
                runtime=planSpellRuntime(actor,committed,ModernUseHistory.Lane.LIGHT_ARROW,now,new ModernSpellRuntime.Arrow(outcome.critical()));
                victimResources=planDamage(target,Math.incrementExact(revision),outcome.appliedDamage(),outcome.remainingHp());
            }catch(IllegalArgumentException|ArithmeticException failure){outcome=null;reason=ModernSpellProtocol.Reason.NUMERIC;}
            nextRevision();actor.spellRuntime=outcome==null?deniedRuntime:runtime;
            if(outcome!=null) {
                actor.spellHistory=history;actor.targetHistory=targets;actor.mp=resources.vitals().mp();actor.equipmentResources=resources;
                target.hp=outcome.remainingHp();target.equipmentResources=victimResources;
                commitArmorHit(target,nextArmor);
                if(regenEnabled)target.regen=nextTargetRegen;
                if(outcome.appliedDamage()>0)target.hurtRevision=revision;
                if(outcome.death()){target.deathRevision=revision;clearBuffs(target,now,revision);if(honorConfig!=null)target.life=nextLife;}
                if(outcome.death() && settlementEnabled&&honorConfig==null)finishElimination();
            }
            return casted(reason);
        },()->casted(ModernSpellProtocol.Reason.NOT_ACTIVE));
    }
    private record FireCandidate(MutableActor actor,int objectiveIndex,long hp,long hpRevision,long respawns,ModernWorldMap.Cell cell) {}
    /** Candidate membership is frozen once at admission. A late respawn is not
     * retroactively targeted; captured actors are all revalidated before commit. */
    private CastResult fireStorm(MutableActor actor,ModernSpellProtocol.Request request) {
        if(request.targetEntityId()!=0)throw new SecurityException("fire storm has no client target");
        return guarded(()->{
            long now=advance();
            if(!spellsEnabled||phase==Phase.ABORTED||phase==Phase.FINISHED)return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
            if(actor.spellSequence()==Long.MAX_VALUE||request.sequence()!=actor.spellSequence()+1)throw new SecurityException("spell request sequence mismatch");
            var learned=actor.prepared.skills().skills().stream().filter(s->s.skillId()==70).findFirst();
            ModernSpellProtocol.Reason reason;
            var candidates=new ArrayList<FireCandidate>();var outcomes=new ArrayList<ModernFireStorm.Outcome>();
            var armorHits=new ArrayList<ArmorHit>();var armorPlans=new ArrayList<ArmorProtectionView>();
            long effect=actor.buffs.revision(),mpAxis=actor.regen.mpRevision();
            long casterHp=actor.hp,casterMp=actor.mp,casterLife=incarnation(actor),sequence=actor.spellSequence();
            var origin=actor.position;var priorHistory=actor.spellHistory.get(ModernUseHistory.Lane.FIRE_STORM);
            boolean attempted=false,success=false;
            if(phase!=Phase.ACTIVE)reason=ModernSpellProtocol.Reason.NOT_ACTIVE;
            else if(actor.hp==0)reason=ModernSpellProtocol.Reason.DEAD;
            else if(actor.weight!=null&&!actor.weight.view().permitsAttackCast())reason=ModernSpellProtocol.Reason.TOO_HEAVY;
            else if(learned.isEmpty())reason=ModernSpellProtocol.Reason.UNLEARNED;
            else if(!fireStormEnabled||learned.get().support()!=ModernSkills.Support.FIRE_STORM||!actor.loadout.combatant().fireStormSupported())reason=ModernSpellProtocol.Reason.UNSUPPORTED;
            else if(castCooling(actor,now)||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)
                    ||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.spellRuntime.cooling(ModernUseHistory.Lane.FIRE_STORM,now,TimeUnit.MILLISECONDS.toNanos(ModernFireStorm.REUSE_MILLIS)))reason=ModernSpellProtocol.Reason.TOO_FAST;
            else if(actor.mp<spellCost(actor,ModernUseHistory.Lane.FIRE_STORM))reason=ModernSpellProtocol.Reason.NO_MP;
            else {
                for(var target:actors)if(target!=actor&&target.hp>0&&target.member.team()!=actor.member.team()
                        &&distance(actor.position,target.position)<=ModernFireStorm.RADIUS&&strikeLineOpen(actor.position,target.position,ModernFireStorm.RADIUS))
                    candidates.add(new FireCandidate(target,-1,target.hp,target.regen.hpRevision(),honorConfig==null?0:target.life.respawnCount(),target.position));
                for(int i=0;i<objectives.size();i++){var target=objectives.get(i);if(target.hp()>0&&target.team()!=actor.member.team()
                        &&distance(actor.position,target.cell())<=ModernFireStorm.RADIUS&&strikeLineOpen(actor.position,target.cell(),ModernFireStorm.RADIUS))
                    candidates.add(new FireCandidate(null,i,target.hp(),0,0,target.cell()));}
                if(candidates.stream().anyMatch(c->c.actor()!=null&&!c.actor().loadout.combatant().fireStormSupported()))reason=ModernSpellProtocol.Reason.UNSUPPORTED;
                else try {
                    preflightSpell(actor,ModernUseHistory.Lane.FIRE_STORM,request.sequence());
                    attempted=true;
                    for(var candidate:candidates) {
                        if(candidate.actor()!=null) {
                            var guard=armorGuard(candidate.actor(),actor,now);
                            var damage=ModernFireStorm.resolveProtected(actor.loadout.combatant().fireStormValues(),candidate.actor().loadout.combatant().fireStormValues(),candidate.hp(),random,actor.buffs.spBonus(),protection(guard));
                            outcomes.add(damage.outcome());armorHits.add(planArmorHit(guard,damage.impact()));
                        } else {var target=objectives.get(candidate.objectiveIndex());outcomes.add(actor.loadout.combatant().fireStormObjective(target.hp(),target.maximumHp(),random,actor.buffs.spBonus()));armorHits.add(null);}
                    }
                    success=true;reason=ModernSpellProtocol.Reason.NONE;
                }catch(IllegalArgumentException|ArithmeticException failure){reason=ModernSpellProtocol.Reason.NUMERIC;success=false;}
            }
            if(attempted) {
                now=advance();if(phase!=Phase.ACTIVE)return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
                if(actor.spellSequence()!=sequence)throw new SecurityException("reentrant spell request");
                if(actor.buffs.revision()!=effect){success=false;reason=ModernSpellProtocol.Reason.STALE_EFFECT;}
                else if(actor.regen.mpRevision()!=mpAxis||candidates.stream().anyMatch(c->c.actor()!=null&&c.actor().regen.hpRevision()!=c.hpRevision())){
                    success=false;reason=ModernSpellProtocol.Reason.STALE_REGEN;
                }
                // A supplied RNG can reenter this monitor. Never reinterpret
                // the old radius/price using a new caster pose, life or gear.
                else if(incarnation(actor)!=casterLife||actor.hp!=casterHp||actor.mp!=casterMp||!actor.position.equals(origin)
                        ||actor.spellHistory.get(ModernUseHistory.Lane.FIRE_STORM)!=priorHistory) {
                    success=false;reason=ModernSpellProtocol.Reason.STALE_REGEN;
                }
                // Preserve the captured target set only while each member
                // remains in the same life/cell with unchanged health.
                if(candidates.stream().anyMatch(c->c.actor()!=null&&(c.actor().hp!=c.hp()||!c.actor().position.equals(c.cell())
                        ||honorConfig!=null&&c.actor().life.respawnCount()!=c.respawns()))) {
                    if(reason==ModernSpellProtocol.Reason.NONE){success=false;reason=ModernSpellProtocol.Reason.STALE_REGEN;}
                }
            }
            if(!equipmentCurrent())return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
            if(success&&armorHits.stream().anyMatch(Objects::nonNull)) {
                now=advanceTemporal();if(phase!=Phase.ACTIVE)return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
                try{for(var hit:armorHits)armorPlans.add(acceptArmorHit(hit,now));}
                catch(IllegalArgumentException|ArithmeticException failure){success=false;reason=ModernSpellProtocol.Reason.STALE_EFFECT;}
            }
            ModernMatchSettlement.Event completed=null;
            var hp=new ArrayList<Long>(actors.size());var ledgers=new ArrayList<ModernRegenProtocol.State>(actors.size());
            var lives=new ArrayList<ModernHonor.Life>(actors.size());var leases=new ArrayList<ModernTimedEffects.State>(actors.size());
            var towers=new ArrayList<>(objectives);var losses=new ArrayList<Long>();
            long next=Math.incrementExact(revision),remainingMp=actor.mp;
            var deniedRuntime=actor.spellRuntime.denied(next,request.sequence());var runtime=deniedRuntime;
            var history=actor.spellHistory;
            var targetHistory=actor.targetHistory;
            var resources=new ArrayList<ModernVitalBalance.State>(actors.size());
            if(success)try {
                var impacts=new ArrayList<ModernTargetHistory.Effect>();
                for(var target:actors){hp.add(target.hp);ledgers.add(target.regen);lives.add(target.life);leases.add(target.buffs);resources.add(target.equipmentResources);}
                for(int i=0;i<candidates.size();i++) {
                    var candidate=candidates.get(i);var outcome=outcomes.get(i);long loss;
                    if(candidate.actor()!=null) {
                        var target=candidate.actor();int index=Math.toIntExact(target.entityId-1);
                        impacts.add(participantEffect(target,outcome.rawDamage(),outcome.appliedDamage(),outcome.remainingHp()));
                        var ledger=target.regen.damaged(outcome.appliedDamage());loss=ledger.damageLoss();
                        hp.set(index,outcome.remainingHp());ledgers.set(index,ledger);
                        resources.set(index,planDamage(target,next,outcome.appliedDamage(),outcome.remainingHp()));
                        if(outcome.death()) {
                            if(honorConfig!=null)lives.set(index,target.life.died(next,elapsedMillis(now),target.position));
                            if(timedEnabled&&target.buffs.hasEffects())leases.set(index,ModernTimedEffects.clear(target.buffs,Math.max(target.buffs.observedNanos(),elapsed(now)),next));
                        }
                    } else {
                        var prior=objectives.get(candidate.objectiveIndex());var updated=prior.damaged(outcome.appliedDamage(),actor.entityId,4,next);
                        impacts.add(honorEffect(prior,ModernTargetHistory.Kind.DAMAGE,outcome.rawDamage(),outcome.appliedDamage(),outcome.remainingHp()));
                        towers.set(candidate.objectiveIndex(),updated);loss=updated.damageLoss();
                    }
                    losses.add(loss);
                }
                var committed=planCastHistory(actor,ModernUseHistory.Lane.FIRE_STORM,next,request.sequence(),impacts);
                runtime=planSpellRuntime(actor,committed,ModernUseHistory.Lane.FIRE_STORM,now,
                        new ModernSpellRuntime.FireStorm(losses));
                remainingMp=committed.resources().vitals().mp();resources.set(Math.toIntExact(actor.entityId-1),committed.resources());
                history=committed.spells();targetHistory=committed.targets();
                Integer winner=null;
                if(honorConfig!=null&&towers.stream().anyMatch(t->t.hp()==0))winner=actor.member.team();
                else if(honorConfig==null&&settlementEnabled){var teams=new HashSet<Integer>();for(int i=0;i<actors.size();i++)if(hp.get(i)>0)teams.add(actors.get(i).member.team());if(teams.size()==1)winner=teams.iterator().next();}
                if(winner!=null) {
                    int won=winner;var rows=actors.stream().map(a->new ModernMatchSettlement.Participant(a.entityId,a.member.identity(),a.member.team(),a.member.team()==won?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
                    var config=preparation.snapshot().summary().config();completed=new ModernMatchSettlement.Event(java.util.UUID.randomUUID(),preparation.matchId(),preparation.generation(),next,config.mode(),config.mapId(),lastObservedEpochMillis,rows);
                    for(int i=0;i<actors.size();i++) {
                        var lease=leases.get(i);if(timedEnabled&&lease.hasEffects())leases.set(i,ModernTimedEffects.clear(lease,Math.max(lease.observedNanos(),elapsed(now)),next));
                        if(honorConfig!=null)lives.set(i,lives.get(i).terminal());
                    }
                }
            }catch(IllegalArgumentException|ArithmeticException failure){success=false;completed=null;reason=ModernSpellProtocol.Reason.NUMERIC;}
            nextRevision();actor.spellRuntime=success?runtime:deniedRuntime;
            if(success) {
                actor.mp=remainingMp;actor.spellHistory=history;actor.targetHistory=targetHistory;
                for(int i=0;i<actors.size();i++){var target=actors.get(i);target.hp=hp.get(i);target.equipmentResources=resources.get(i);target.regen=ledgers.get(i);target.life=lives.get(i);target.buffs=leases.get(i);}
                for(int i=0;i<candidates.size();i++){var target=candidates.get(i).actor();var outcome=outcomes.get(i);if(target!=null){
                    if(!armorPlans.isEmpty())commitArmorHit(target,armorPlans.get(i));
                    if(outcome.appliedDamage()>0)target.hurtRevision=revision;if(outcome.death())target.deathRevision=revision;}}
                objectives.clear();objectives.addAll(towers);
                if(completed!=null){clearAllArmor(now,revision);result=completed;phase=Phase.FINISHED;}
            }
            return casted(reason);
        },()->casted(ModernSpellProtocol.Reason.NOT_ACTIVE));
    }
    private CastResult casted(ModernSpellProtocol.Reason reason) { return new CastResult(reason, capture()); }
    private int objectiveIndex(long id){long index=id-actors.size()-1L;if(honorConfig==null||index<0||index>=2)throw new SecurityException("invalid objective target");return (int)index;}
    private AttackResult attackObjective(MutableActor actor,long sequence,long targetId) {
        int index=objectiveIndex(targetId);
        if(sequence<1)throw new SecurityException("invalid attack sequence");
        return guarded(()->{
            long now=advance();if(phase!=Phase.ACTIVE)return attacked(AttackStatus.NOT_ACTIVE);
            if(sequence!=Math.incrementExact(actor.attackSequence))throw new SecurityException("combat attack sequence mismatch");
            var target=objectives.get(index);AttackStatus status;ModernPhysicalCombat.Outcome outcome=null;
            long effect=actor.buffs.revision();boolean attempted=false;
            if(actor.hp==0)status=AttackStatus.DEAD;
            else if(!actor.weight.view().permitsAttackCast())status=AttackStatus.TOO_HEAVY;
            else if(target.hp()==0)status=AttackStatus.TARGET_DEAD;
            else if(actor.member.team()==target.team())status=AttackStatus.SAME_TEAM;
            else if(!actor.loadout.combatant().physicalSupported())status=AttackStatus.UNSUPPORTED;
            else if(actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE)||castCooling(actor,now))status=AttackStatus.TOO_FAST;
            else if(distance(actor.position,target.cell())>actor.loadout.combatant().attackRange())status=AttackStatus.OUT_OF_RANGE;
            else if(!strikeLineOpen(actor.position,target.cell()))status=AttackStatus.BLOCKED;
            else if(actor.loadout.combatant().bow()&&(actor.ammo==null||actor.ammo.selected()==null||actor.ammo.selected().remaining()==0))status=AttackStatus.NO_AMMO;
            else {
                try{attempted=true;outcome=actor.loadout.combatant().attackObjective(target.hp(),target.maximumHp(),random,actor.weight.view().hitPenalty(),actor.loadout.combatant().bow()?actor.ammo.selected().facts():null);status=AttackStatus.ATTACKED;}
                catch(IllegalArgumentException|ArithmeticException e){status=AttackStatus.NUMERIC;}
            }
            if(actor.hp>0&&actor.weight.view().permitsAttackCast())actor.regenClock=actor.regenClock.noteAttack();
            if(attempted){now=advance();if(phase!=Phase.ACTIVE||!equipmentCurrent())return attacked(AttackStatus.NOT_ACTIVE);if(effect!=actor.buffs.revision()){outcome=null;status=AttackStatus.STALE_EFFECT;}}
            ModernHonor.Objective next=null;ModernAmmo.State nextAmmo=null;ModernInventoryWeight.Snapshot nextWeight=null;
            ModernBattlePresentation.Shot shot=null;
            if(outcome!=null)try {
                next=target.damaged(outcome.appliedDamage(),actor.entityId,1,Math.incrementExact(revision));
                if(actor.loadout.combatant().bow()){nextAmmo=actor.ammo.shot(Math.incrementExact(revision),sequence,targetId,actor.position.x(),actor.position.y(),target.x(),target.y(),outcome.hit());
                    nextWeight=actor.loadout.reweight(actor.itemUse.stocks(),nextAmmo.stocks(),actor.weight.units(),materialStocks(actor));
                    shot=shot(actor,nextAmmo,now,new ModernTargetHistory.Honor(target.entityId(),target.team()),1);}
            }catch(IllegalArgumentException|ArithmeticException e){outcome=null;next=null;status=AttackStatus.NUMERIC;}
            nextRevision();actor.attackSequence=sequence;
            if(outcome!=null){actor.attacked=true;actor.lastAttackNanos=now;actor.attackRevision=revision;actor.targetEntityId=targetId;objectives.set(index,next);
                if(nextAmmo!=null){actor.ammo=nextAmmo;actor.weight=nextWeight;actor.presentation=actor.presentation.withShot(shot);}if(next.hp()==0)finishHonor(1-target.team());}
            return attacked(status);
        },()->attacked(AttackStatus.NOT_ACTIVE));
    }
    private CastResult castObjective(MutableActor actor,ModernSpellProtocol.Request request) {
        int index=objectiveIndex(request.targetEntityId());
        return guarded(()->{
            long now=advance();if(phase!=Phase.ACTIVE)return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
            if(request.sequence()!=Math.incrementExact(actor.spellSequence()))throw new SecurityException("spell request sequence mismatch");
            var target=objectives.get(index);boolean healing=request.skillId()==1;
            var supported=healing?ModernSkills.Support.BASIC_HEAL:ModernSkills.Support.LIGHT_ARROW;
            var learned=actor.prepared.skills().skills().stream().filter(s->s.skillId()==request.skillId()).findFirst();
            var lane=healing?ModernUseHistory.Lane.HEALING:ModernUseHistory.Lane.LIGHT_ARROW;
            int cost=spellCost(actor,lane);
            ModernSpellProtocol.Reason reason;ModernHonor.Objective next=null;
            ModernMagicCombat.Outcome damage=null;ModernHonorHealing.Outcome heal=null;
            long effect=actor.buffs.revision(),mpRevision=actor.regen.mpRevision();boolean attempted=false;
            if(actor.hp==0)reason=ModernSpellProtocol.Reason.DEAD;
            else if(!actor.weight.view().permitsAttackCast())reason=ModernSpellProtocol.Reason.TOO_HEAVY;
            else if(learned.isEmpty())reason=ModernSpellProtocol.Reason.UNLEARNED;
            else if(request.skillId()!=1&&request.skillId()!=4||learned.get().support()!=supported||!actor.loadout.combatant().magicSupported()
                    ||healing&&!actor.loadout.healing().supported())reason=ModernSpellProtocol.Reason.UNSUPPORTED;
            else if(target.hp()==0)reason=ModernSpellProtocol.Reason.TARGET_DEAD;
            else if(healing!=(actor.member.team()==target.team()))reason=ModernSpellProtocol.Reason.SAME_TEAM;
            else if(actor.mp<cost)reason=ModernSpellProtocol.Reason.NO_MP;
            else if(castCooling(actor,now)||actor.attacked&&now-actor.lastAttackNanos<pace(actor,ModernTimedEffects.Action.ATTACK)
                    ||actor.moved&&now-actor.lastMoveNanos<pace(actor,ModernTimedEffects.Action.MOVE))reason=ModernSpellProtocol.Reason.TOO_FAST;
            else if(healing?!ModernHonor.inScreen(actor.position.x(),actor.position.y(),target.x(),target.y()):distance(actor.position,target.cell())>10)reason=ModernSpellProtocol.Reason.OUT_OF_RANGE;
            else if(!strikeLineOpen(actor.position,target.cell(),healing?19:16))reason=ModernSpellProtocol.Reason.BLOCKED;
            else {
                try{preflightSpell(actor,lane,request.sequence());attempted=true;if(healing)heal=ModernHonorHealing.resolve(actor.loadout.healing(),target.hp(),target.maximumHp(),random);
                    else damage=actor.loadout.combatant().lightArrowObjective(target.hp(),target.maximumHp(),random,actor.buffs.spBonus());reason=ModernSpellProtocol.Reason.NONE;}
                catch(IllegalArgumentException|ArithmeticException e){reason=ModernSpellProtocol.Reason.NUMERIC;}
            }
            if(attempted){now=advance();if(phase!=Phase.ACTIVE||!equipmentCurrent())return casted(ModernSpellProtocol.Reason.NOT_ACTIVE);
                if(actor.spellSequence()>=request.sequence())throw new SecurityException("reentrant spell request");
                if(effect!=actor.buffs.revision()){heal=null;damage=null;reason=ModernSpellProtocol.Reason.STALE_EFFECT;}
                else if(mpRevision!=actor.regen.mpRevision()){heal=null;damage=null;reason=ModernSpellProtocol.Reason.STALE_REGEN;}}
            var deniedRuntime=actor.spellRuntime.denied(Math.incrementExact(revision),request.sequence());var runtime=deniedRuntime;
            var history=actor.spellHistory;
            var targets=actor.targetHistory;
            var resources=actor.equipmentResources;
            if(heal!=null||damage!=null)try{
                long nextRevision=Math.incrementExact(revision);
                var impact=healing?honorEffect(target,ModernTargetHistory.Kind.HEAL,heal.amount(),heal.restored(),heal.remainingHp())
                        :honorEffect(target,ModernTargetHistory.Kind.DAMAGE,damage.rawDamage(),damage.appliedDamage(),damage.remainingHp());
                var committed=planCastHistory(actor,lane,nextRevision,request.sequence(),List.of(impact));
                resources=committed.resources();history=committed.spells();targets=committed.targets();
                runtime=planSpellRuntime(actor,committed,lane,now,healing?ModernSpellRuntime.Ordinary.INSTANCE:new ModernSpellRuntime.Arrow(damage.critical()));
                next=healing?target.healed(heal.restored(),actor.entityId,nextRevision):target.damaged(damage.appliedDamage(),actor.entityId,2,nextRevision);
            }catch(IllegalArgumentException|ArithmeticException e){next=null;reason=ModernSpellProtocol.Reason.NUMERIC;}
            nextRevision();actor.spellRuntime=next==null?deniedRuntime:runtime;
            if(next!=null){objectives.set(index,next);actor.mp=resources.vitals().mp();actor.equipmentResources=resources;actor.spellHistory=history;actor.targetHistory=targets;
                if(next.hp()==0)finishHonor(1-target.team());}
            return casted(reason);
        },()->casted(ModernSpellProtocol.Reason.NOT_ACTIVE));
    }
    private void finishHonor(int winningTeam) {
        var rows=actors.stream().map(a->new ModernMatchSettlement.Participant(a.entityId,a.member.identity(),a.member.team(),a.member.team()==winningTeam?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
        var config=preparation.snapshot().summary().config();
        result=new ModernMatchSettlement.Event(java.util.UUID.randomUUID(),preparation.matchId(),preparation.generation(),revision,5,config.mapId(),lastObservedEpochMillis,rows);
        clearAllBuffs(lastObservedNanos,revision);clearHonorPending();phase=Phase.FINISHED;
    }
    private long elapsedMillis(long now){return Math.max(0,now-activeStartedNanos)/1_000_000;}
    private void clearHonorPending(){if(honorConfig!=null)for(var a:actors)a.life=a.life.terminal();}
    private void respawnDue(long now) {
        if(honorConfig==null||phase!=Phase.ACTIVE)return;
        var due=actors.stream().filter(a->a.hp==0&&a.life.pendingDueMillis()>0&&elapsedMillis(now)>=a.life.pendingDueMillis()).toList();
        if(due.isEmpty())return;
        var next=new ArrayList<ModernHonor.Life>();
        var resources=new ArrayList<ModernVitalBalance.State>();
        try{long r=Math.incrementExact(revision);for(var a:due){next.add(a.life.respawn(r,a.loadout.combatant().maxHp()));resources.add(planRespawn(a,r));}}
        catch(IllegalArgumentException|ArithmeticException e){abortInternal(AbortReason.SERVER_ABORT);return;}
        nextRevision();for(int i=0;i<due.size();i++){var a=due.get(i);a.life=next.get(i);a.equipmentResources=resources.get(i);a.hp=a.equipmentResources.vitals().hp();}
    }
    private void finishSiege(){clearAllBuffs(lastObservedNanos,revision);for(var a:actors)a.siegeLife=a.siegeLife.terminal();phase=Phase.FINISHED;}
    private void respawnSiegeDue(long now) {
        if(siegeState==null||phase!=Phase.ACTIVE)return;
        // Keep every frozen actor's cell reserved, including corpses, matching
        // the world uniqueness contract. A full camp delays a respawn without
        // granting HP, overlapping actors or choosing an unqualified location.
        var occupied=new HashSet<ModernWorldMap.Cell>();for(var a:actors)occupied.add(a.position);
        var due=new ArrayList<MutableActor>();var cells=new ArrayList<ModernWorldMap.Cell>();var lives=new ArrayList<ModernSiege.Life>();
        var resources=new ArrayList<ModernVitalBalance.State>();
        try{long next=Math.incrementExact(revision);for(var a:actors) {
            if(a.hp>0||a.siegeLife.dueMillis()==0||elapsedMillis(now)<a.siegeLife.dueMillis())continue;
            var destination=siegeState.config().camp(a.member.team(),siegeState.attacker()).stream().filter(c->c.equals(a.position)||!occupied.contains(c)).findFirst().orElse(null);
            if(destination==null)continue;
            var life=a.siegeLife.respawn(next,a.loadout.combatant().maxHp());occupied.remove(a.position);occupied.add(destination);
            resources.add(planRespawn(a,next));
            due.add(a);cells.add(destination);lives.add(life);
        }}catch(ArithmeticException|IllegalArgumentException failure){abortInternal(AbortReason.SERVER_ABORT);return;}
        if(due.isEmpty())return;
        nextRevision();for(int i=0;i<due.size();i++){var a=due.get(i);a.siegeLife=lives.get(i);a.position=cells.get(i);a.equipmentResources=resources.get(i);a.hp=a.equipmentResources.vitals().hp();}
    }
    /** Last hit and result share one authority revision; no JDBC or Rooms call under this guard. */
    private void finishElimination() {
        var livingTeams = actors.stream().filter(actor -> actor.hp > 0).map(actor -> actor.member.team()).distinct().toList();
        if (livingTeams.size() != 1) return;
        int winningTeam = livingTeams.get(0);
        var rows = actors.stream().map(actor -> new ModernMatchSettlement.Participant(actor.entityId, actor.member.identity(),
                actor.member.team(), actor.member.team() == winningTeam ? ModernMatchSettlement.Verdict.WIN : ModernMatchSettlement.Verdict.LOSS)).toList();
        var config = preparation.snapshot().summary().config();
        result = new ModernMatchSettlement.Event(java.util.UUID.randomUUID(), preparation.matchId(), preparation.generation(),
                revision, config.mode(), config.mapId(), lastObservedEpochMillis, rows);
        clearAllBuffs(lastObservedNanos,revision);
        phase = Phase.FINISHED;
    }
    private static long distance(ModernWorldMap.Cell a, ModernWorldMap.Cell b) {
        return Math.max(Math.abs((long)a.x() - b.x()), Math.abs((long)a.y() - b.y()));
    }
    /** Conservative bounded grid ray, using the same qualified blocked-edge/corner policy as movement. */
    private boolean strikeLineOpen(ModernWorldMap.Cell from, ModernWorldMap.Cell target) {
        return strikeLineOpen(from,target,16);
    }
    private boolean strikeLineOpen(ModernWorldMap.Cell from, ModernWorldMap.Cell target,int maximumSteps) {
        long length = distance(from, target);
        if (length < 1 || length > maximumSteps) return false;
        long dx = Math.abs((long)target.x() - from.x()), dy = Math.abs((long)target.y() - from.y());
        int sx = Integer.compare(target.x(), from.x()), sy = Integer.compare(target.y(), from.y());
        long error = dx - dy; ModernWorldMap.Cell previous = from;
        for (int step = 0; step < maximumSteps; step++) {
            int x = previous.x(), y = previous.y(); long twice = error * 2;
            if (twice > -dy) { error -= dy; x += sx; }
            if (twice < dx) { error += dx; y += sy; }
            var next = new ModernWorldMap.Cell(x, y);
            if (!map.canStep(previous, next)||siegeSpace!=null&&!siegeSpace.rayStep(previous,next,siegeState.open(),target)) return false;
            if (next.equals(target)) return true;
            previous = next;
        }
        return false;
    }
    synchronized void disconnect(TournamentPresence.Owner owner) {
        if (closed || !owners.containsKey(owner)) return;
        // Cancellation/closure is always allowed; it never commits gameplay.
        abortInternal(AbortReason.DISCONNECTED); owners.remove(owner);
    }
    /** Race-only individual retirement. Frozen result identity is retained after this owner disappears. */
    synchronized Snapshot withdrawRacer(TournamentPresence.Owner owner,ModernPetRace.Dnf reason) {
        return ModernLegacyBattleProjection.from(withdrawRacerObservation(owner,reason));
    }
    synchronized ModernBattleObservation.Capture withdrawRacerObservation(TournamentPresence.Owner owner,ModernPetRace.Dnf reason) {
        requireOpen();var actor=owners.get(owner);if(actor==null)throw new SecurityException("race owner mismatch");
        return guarded(()->{
            long now=advance();
            if(raceState==null||phase!=Phase.ACTIVE){if(phase!=Phase.FINISHED)abortInternal(AbortReason.DISCONNECTED);owners.remove(owner);return capture();}
            var planned=ModernPetRace.withdraw(raceState,preparation.matchId(),preparation.generation(),actor.entityId,reason,
                    Math.max(0,now-activeStartedNanos),Math.incrementExact(revision));
            if(planned.revision()!=raceState.revision())nextRevision();raceState=planned;
            if(planned.phase()==ModernPetRace.Phase.ABORTED){clearAllArmor(lastObservedNanos,revision);phase=Phase.ABORTED;abortReason=AbortReason.DISCONNECTED;}
            owners.remove(owner);return capture();
        },this::capture);
    }
    synchronized boolean individualRaceExit(){return raceState!=null&&(phase==Phase.ACTIVE||phase==Phase.FINISHED);}
    synchronized boolean individualTimeAttackExit(){return timeAttackState!=null&&(phase==Phase.ACTIVE||phase==Phase.FINISHED);}
    /** Trusted service retirement seam; the service commits this departure
     * before permitting the corresponding Rooms leave/disconnect. */
    synchronized Snapshot withdrawTimeAttack(TournamentPresence.Owner owner) {
        return ModernLegacyBattleProjection.from(withdrawTimeAttackObservation(owner));
    }
    synchronized ModernBattleObservation.Capture withdrawTimeAttackObservation(TournamentPresence.Owner owner) {
        requireOpen();var actor=owners.get(owner);if(actor==null||timeAttackState==null)throw new SecurityException("Time Attack owner mismatch");
        return guarded(()->{
            long now=advance();
            if(phase==Phase.ACTIVE) {
                if(!equipmentCurrent()){owners.remove(owner);return capture();}
                var eligible=actors.stream().filter(a->a.hp>0&&participating(a)).map(a->a.entityId).collect(java.util.stream.Collectors.toSet());
                var next=ModernTimeAttack.depart(timeAttackState,actor.entityId,eligible,Math.incrementExact(revision));
                long nextRevision=Math.incrementExact(revision);
                var terminalBuffs=planTimeAttackClear(next,now,nextRevision);
                var departedBuffs=timedEnabled&&actor.buffs.hasEffects()
                        ?ModernTimedEffects.clear(actor.buffs,Math.max(actor.buffs.observedNanos(),elapsed(now)),nextRevision):actor.buffs;
                if(terminalBuffs!=null)npcRoster.clearHaste(npcScope);
                nextRevision();timeAttackState=next;actor.buffs=departedBuffs;
                clearArmor(actor,now,revision);
                if(timeAttackFinished(next))finishTimeAttack(terminalBuffs);
            } else if(phase!=Phase.FINISHED)abortInternal(AbortReason.DISCONNECTED);
            owners.remove(owner);return capture();
        },this::capture);
    }
    /** Trusted service receipt. It is captured once at the terminal snapshot,
     * not regenerated by polling, retries or SQL completion. */
    synchronized ModernMatchSettlement.Event timeAttackResult(){requireOpen();return timeAttackResult;}
    /** Exactly one internal siege receipt; only a dedicated, journaled service
     * may expose it. Polling or releasing the room never regenerates its ID. */
    synchronized ModernMatchSettlement.Event siegeResult(){requireOpen();return siegeResult;}
    synchronized Snapshot abort() {
        return ModernLegacyBattleProjection.from(abortObservation());
    }
    synchronized ModernBattleObservation.Capture abortObservation() {
        requireOpen(); abortInternal(AbortReason.SERVER_ABORT); return capture();
    }
    synchronized int ownerCount() { return owners.size(); }
    synchronized boolean isClosed() { return closed; }
    @Override public synchronized void close() { if(npcRoster!=null)npcRoster.close();if(questProgress!=null)questProgress=ModernQuestProgress.abort(questProgress);for(var a:actors)a.armor=null;closed = true; owners.clear(); }

    private MutableActor requireActor(TournamentPresence.Owner owner, ModernRooms.Scope scope, long matchId, long generation) {
        requireOpen(); MutableActor actor = owners.get(owner);
        if (actor == null || scope == null || !actor.prepared.scope().equals(scope)
                || matchId != preparation.matchId() || generation != preparation.generation())
            throw new SecurityException("world owner context mismatch");
        if(commandLifeActor!=null&&!commandLifeClaimed) {
            if(actor!=commandLifeActor)throw new SecurityException("battle command owner differs from life proof");
            commandLifeClaimed=true;
        }
        return actor;
    }
    private <T> T guarded(Supplier<T> live, Supplier<T> cancelled) {
        // A completed authority event survives later room release/disconnect.
        // The individual operations still reject gameplay in FINISHED.
        if (phase == Phase.FINISHED) return live.get();
        if (!preparation.isLive()) { abortInternal(AbortReason.PREPARATION_CANCELLED); return cancelled.get(); }
        var outer=vipOperation;
        if(vipEnabled)vipOperation=new VipOperation();
        try (var ignored = preparation.guard()) { return live.get(); }
        catch(VipStaleCalculation stale){return cancelled.get();}
        catch (IllegalStateException failure) {
            if (!preparation.isLive()) { abortInternal(AbortReason.PREPARATION_CANCELLED); return cancelled.get(); }
            throw failure;
        }
        finally {vipOperation=outer;}
    }
    private long advance() {
        long now=advanceTemporal();
        if(regenEnabled&&!vipEnabled&&phase==Phase.ACTIVE)now=advanceRegen(now);
        respawnSiegeDue(now);
        if(npcRoster!=null&&phase==Phase.ACTIVE) {
            var previous=npcRoster.snapshot();long elapsed=elapsedMillis(now);
            if(elapsed!=previous.elapsedMillis()) {
                Math.incrementExact(revision);
                var advanced=npcRoster.advance(npcScope,elapsed);nextRevision();
                var resident=advanced.actors().stream().map(ModernNpcRoster.Actor::id).collect(java.util.stream.Collectors.toSet());
                npcExchanges.keySet().retainAll(resident);
            }
            advanceQuestWaves();
            advanceQuestObjectives();
            advanceTimeAttackWaves();
        }
        checkCommandLife();
        expireArmor(now);
        return now;
    }
    /** Bounded two-lane pulse under this engine's lock. A future birth cell
     * occupied by a participant/NPC, or resident corpses filling the roster,
     * delays the WHOLE wave without consuming IDs/revisions or aborting play. */
    private void advanceQuestWaves() {
        if(questProgress==null||questProgress.status()!=ModernQuestProgress.Status.RUNNING)return;
        for(int lane=0;lane<2;lane++) {
            if(questProgress.lanes().get(lane).phase()!=ModernQuestProgress.Phase.SPAWN_PENDING)continue;
            var roster=npcRoster.snapshot();var step=ModernQuestProgress.spawn(questProgress,lane,roster);
            if(step.batch().size()>npcConfig.capacity()-roster.actors().size())continue;
            var occupied=new java.util.HashSet<>(npcObstacleCells(lane));
            for(var npc:roster.actors())if(npc.alive()&&sameForce(npc.lane(),lane))occupied.add(npc.cell());
            if(step.batch().stream().anyMatch(spawn->occupied.contains(spawn.cell())))continue;
            if(!equipmentCurrent())return;
            Math.incrementExact(revision);
            npcRoster.spawn(npcScope,step.batch(),npcObstacleCells(lane));
            questProgress=step.after();nextRevision();
        }
    }
    private void advanceTimeAttackWaves() {
        if(timeAttackState==null||timeAttackState.phase()!=ModernTimeAttack.Phase.SPAWN_PENDING)return;
        var roster=npcRoster.snapshot();long next=Math.incrementExact(revision);
        var step=ModernTimeAttack.spawn(timeAttackState,roster,next);
        if(step.batch().size()>npcConfig.capacity()-roster.actors().size())return;
        var occupied=new HashSet<>(npcObstacleCells(0));
        for(var npc:roster.actors())if(npc.alive())occupied.add(npc.cell());
        if(step.batch().stream().anyMatch(s->occupied.contains(s.cell()))||!equipmentCurrent())return;
        npcRoster.spawn(npcScope,step.batch(),npcObstacleCells(0));timeAttackState=step.state();nextRevision();
    }
    private void advanceQuestObjectives() {
        if(!questObjectivesEnabled()||questProgress.status()!=ModernQuestProgress.Status.RUNNING)return;
        for(int lane=0;lane<2;lane++) {
            var progress=questProgress.lanes().get(lane);var previous=questObjectives.get(lane);
            if(progress.phase()!=ModernQuestProgress.Phase.OBJECTIVE||previous!=null&&previous.id().stage()==progress.stage())continue;
            var target=npcConfig.questPlan().objectives().target(lane,progress.stage());
            if(npcPlayerCells(lane).contains(target.cell())||npcOccupies(target.cell(),lane))continue;
            var born=ModernQuestObjectives.Actor.born(questProgress,lane,Math.incrementExact(revision));
            if(!equipmentCurrent())return;
            nextRevision();questObjectives.put(lane,born);
        }
    }
    /** Trusted service/qualification view; no old NPC packet guesses Quest
     * progress from an empty roster or treats an objective wait as FINISHED. */
    synchronized ModernQuestProgress.State questProgress(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation) {
        requireActor(owner,scope,matchId,generation);requireNpc();
        if(questProgress==null)throw new IllegalStateException("Quest progression unavailable");
        return guarded(()->{advance();return questProgress;},()->questProgress);
    }
    /** Observe after external epoch validation as well as before it: a slow
     * lookup or reentrant callback must not extend a lease or match lifetime. */
    private long observeTime() {
        long now = nanoClock.getAsLong();
        if (phase == Phase.ABORTED || phase == Phase.FINISHED) return now;
        if(!preparation.isLive()){abortInternal(AbortReason.PREPARATION_CANCELLED);return now;}
        if (now - lastObservedNanos < 0) { abortInternal(AbortReason.CLOCK_REGRESSED); return now; }
        lastObservedNanos = now;
        return now;
    }
    /** Recheck after RNG without recursively invoking natural regeneration. */
    private long advanceTemporal() {
        long now = observeTime();
        if (phase == Phase.ABORTED || phase == Phase.FINISHED || (!(vipEnabled&&regenEnabled)&&!equipmentCurrent())) return now;
        now = observeTime();
        if (phase == Phase.ABORTED || phase == Phase.FINISHED) return now;
        if (phase == Phase.LOADING && now - loadingStartedNanos >= TimeUnit.MILLISECONDS.toNanos(limits.loadTimeoutMillis()))
            abortInternal(AbortReason.LOAD_TIMEOUT);
        else if (phase == Phase.COUNTDOWN && now - countdownStartedNanos >= limits.countdownNanos()) {
            // nanoTime may wrap its signed origin; bounded elapsed subtraction
            // is deliberate. A delayed tick cannot extend the active lifetime.
            long plannedStart = countdownStartedNanos + limits.countdownNanos();
            if (siegeState==null&&now - plannedStart >= TimeUnit.MILLISECONDS.toNanos(limits.activeTimeoutMillis())) abortInternal(AbortReason.ACTIVE_TIMEOUT);
            else { nextRevision(); phase = Phase.ACTIVE; activeStartedNanos = plannedStart;activeClockStarted=true;activateRace(); }
        } else if (siegeState==null&&raceState==null&&(timeAttackState==null||timeAttackState.startedMillis()<0)&&phase == Phase.ACTIVE && now - activeStartedNanos >= TimeUnit.MILLISECONDS.toNanos(limits.activeTimeoutMillis()))
            abortInternal(AbortReason.ACTIVE_TIMEOUT);
        if(phase==Phase.ACTIVE&&siegeState!=null){var next=siegeState.timeout(elapsedMillis(now),Math.incrementExact(revision));
            if(next!=siegeState){nextRevision();siegeState=next;finishSiege();}}
        if(phase==Phase.ACTIVE&&timeAttackState!=null) {
            var next=ModernTimeAttack.advance(timeAttackState,elapsedMillis(now),Math.incrementExact(revision));
            if(next.phase()==ModernTimeAttack.Phase.FINISHED){
                // The equipment check above precedes this fresh monotonic
                // sample. Do not introduce another callback after the sample.
                var terminalBuffs=planTimeAttackClear(next,now,Math.incrementExact(revision));
                if(terminalBuffs!=null)npcRoster.clearHaste(npcScope);
                nextRevision();finishTimeAttack(terminalBuffs);
            }
            timeAttackState=next;
        }
        if(phase==Phase.ACTIVE&&raceState!=null) {
            var plan=ModernPetRace.advance(raceState,Math.max(0,now-activeStartedNanos),Math.incrementExact(revision));
            if(plan.phase()==ModernPetRace.Phase.COMPLETE){nextRevision();raceState=plan;raceResultId=java.util.UUID.randomUUID();clearAllArmor(now,revision);phase=Phase.FINISHED;}
            else if(plan.phase()==ModernPetRace.Phase.ABORTED){nextRevision();raceState=plan;clearAllArmor(now,revision);phase=Phase.ABORTED;abortReason=AbortReason.ACTIVE_TIMEOUT;}
            else raceState=plan;
        }
        if(phase==Phase.ACTIVE && timedEnabled)expireBuffs(now);
        expireArmor(now);
        if(vipEnabled&&regenEnabled&&phase!=Phase.ABORTED&&phase!=Phase.FINISHED) {
            try { equipmentCurrent(true); }
            finally {
                // Recovery RNG can advance the observed clock past a speed
                // lease even when its candidate is rejected as stale VIP.
                // Clear real expiries before either success or rejection is
                // captured; never publish stale haste provenance or SP/pace.
                if(phase==Phase.ACTIVE) {
                    if(timedEnabled)expireBuffs(lastObservedNanos);
                    expireArmor(lastObservedNanos);
                }
            }
            // Recovery can sample time again after slow RNG. The next command
            // must not backdate its cooldown to the pre-recovery observation.
            return lastObservedNanos;
        }
        return now;
    }
    private long advanceRegen(long now) {
        long elapsed=now-activeStartedNanos;
        var departedBefore=timeAttackState==null?Set.<Long>of():timeAttackState.departed();
        // Freeze every actor before the first RNG callback. A reentrant action
        // must not be overwritten by either a changed or a no-op old outcome.
        var resourceBefore=actors.stream().map(a->a.equipmentResources).toList();
        var clockBefore=actors.stream().map(a->a.regenClock).toList();
        var loadoutBefore=actors.stream().map(a->a.loadout).toList();
        var resources=new ArrayList<>(resourceBefore);
        var outcomes=new ArrayList<ModernNaturalRegen.Outcome>(actors.size());
        var deadClocks=new java.util.HashMap<MutableActor,ModernNaturalRegen.Clock>();
        var ledgers=new ArrayList<ModernRegenProtocol.State>(actors.size());boolean changed=false;
        try {
            for(var actor:actors) {
                if(actor.prepared==null||!participating(actor)){outcomes.add(null);ledgers.add(actor.regen);continue;}
                var values=actor.loadout.regen().values();
                boolean eligible=actor.hp>0&&values.fed()&&actor.weight.view().permitsNaturalRegen();
                var due=ModernNaturalRegen.poll(values,actor.regenClock,elapsed,
                        eligible&&actor.hp<actor.loadout.combatant().maxHp(),eligible&&actor.mp<actor.loadout.combatant().maxMp());
                // Retained LiTo consumes the MP counter before the logical
                // battle map-201 suppression; no classic special-map lookup.
                if(preparation.snapshot().summary().config().mapId()==201)due=new ModernNaturalRegen.Due(due.clock(),due.hpEvents(),0);
                if(actor.hp==0){outcomes.add(null);ledgers.add(actor.regen);if(honorConfig!=null||siegeState!=null||questLivesEnabled())deadClocks.put(actor,due.clock());continue;}
                var outcome=ModernNaturalRegen.resolve(values,due,actor.hp,actor.mp,random);
                outcomes.add(outcome);changed|=outcome.changed();ledgers.add(actor.regen);
            }
        } catch(IllegalArgumentException|ArithmeticException failure) {
            now=advanceTemporal();if(phase==Phase.ACTIVE)abortInternal(AbortReason.SERVER_ABORT);return now;
        }
        now=advanceTemporal();if(phase!=Phase.ACTIVE||!preparation.isLive())return now;
        if(timeAttackState!=null&&!departedBefore.equals(timeAttackState.departed()))return now;
        for(int i=0;i<actors.size();i++){var a=actors.get(i);
            if(a.equipmentResources!=resourceBefore.get(i)||a.regenClock!=clockBefore.get(i)||a.loadout!=loadoutBefore.get(i))return now;}
        // A timed expiry may have independently advanced the world while RNG
        // ran. Allocate the regen revision from the final authority state.
        if(changed) {
            try {long next=Math.incrementExact(revision);for(int i=0;i<actors.size();i++) {
                var out=outcomes.get(i);if(out!=null&&out.changed()){
                    var a=actors.get(i);requireResourceBalance(a);
                    resources.set(i,a.equipmentResources.regenerateBatch(next,out.hpGain(),out.hpLoss(),out.mpGain(),out.mpLoss(),out.hp(),out.mp()));
                    ledgers.set(i,a.regen.regenerated(out,next));}}
            }catch(ArithmeticException|IllegalArgumentException failure){abortInternal(AbortReason.SERVER_ABORT);return now;}
            nextRevision();
        }
        for(int i=0;i<actors.size();i++) {var out=outcomes.get(i);if(out!=null){var actor=actors.get(i);
            actor.regenClock=out.clock();actor.hp=out.hp();actor.mp=out.mp();actor.equipmentResources=resources.get(i);actor.regen=ledgers.get(i);}}
        deadClocks.forEach((a,c)->a.regenClock=c);
        respawnDue(now);
        return now;
    }
    private boolean equipmentCurrent() {
        return equipmentCurrent(false);
    }
    private boolean equipmentCurrent(boolean advanceVipTimeline) {
        if (phase == Phase.ABORTED) return false;
        if (!combatEnabled) return true;
        long epoch = epochClock.getAsLong();
        if (epoch < 0 || epoch < lastObservedEpochMillis) {
            abortInternal(AbortReason.CLOCK_REGRESSED); return false;
        }
        lastObservedEpochMillis = epoch;
        if(vipEnabled&&regenEnabled) {
            if(advanceVipTimeline)return advanceVipRegen(epoch);
            if(planningVipRegen)throw new VipStaleCalculation();
            // Commit fences only validate. They cannot secretly run recovery
            // RNG after the command has captured positions, vitals and time.
            for(var a:actors)if(!a.loadout.currentAt(ModernVipRegenTimeline.effectiveEpoch(a.loadout,lastObservedNanos,epoch)))
                throw new VipStaleCalculation();
            if(vipOperation!=null&&vipOperation.sampled&&vipOperation.version!=vipMutation)throw new VipStaleCalculation();
            return true;
        }
        var vipExpiry=new ArrayList<VipReplacement>();
        for (var actor : actors) {
            if(!participating(actor))continue;
            var profile = actor.loadout.combatant();
            if (epoch < profile.evaluatedAtMillis()) {
                abortInternal(AbortReason.CLOCK_REGRESSED); return false;
            }
            if (!actor.loadout.currentAt(epoch)) {
                if(vipEnabled&&actor.prepared!=null) {
                    try {
                        // VIP regeneration is handled above by its complete
                        // clock-qualified timeline, never by this direct swap.
                        var before=actor.loadout.fighter();
                        var change=ModernVipCharacterState.prepareExpiry(before.vip().state(),epoch).orElseThrow(ModernBattleEngine::invalid);
                        var next=vipFighter(change,before,epoch,actor.hp,actor.mp);
                        var loadout=actor.loadout.replacement(next);
                        var history=planEquipmentHistory(actor,loadout);
                        var weight=actor.weight==null?null:loadout.reweight(actor.itemUse.stocks(),actor.ammo==null?List.of():actor.ammo.stocks(),actor.weight.units(),materialStocks(actor));
                        vipExpiry.add(new VipReplacement(actor,loadout,history,weight));continue;
                    }catch(IllegalArgumentException|ArithmeticException failure){abortInternal(AbortReason.SERVER_ABORT);return false;}
                }
                // Current world protocol freezes maxima/AC/weapon. Stop this
                // match instead of retaining expired bonuses or healing through
                // an implicit profile refresh; dynamic in-world expiry is pending.
                abortInternal(AbortReason.SERVER_ABORT); return false;
            }
        }
        if(!vipExpiry.isEmpty()) {
            long mutation=Math.incrementExact(vipMutation);nextRevision();
            for(var next:vipExpiry) {
                var a=next.actor();var h=next.history();a.loadout=next.loadout();a.equipmentResources=h.resources();
                a.hp=h.resources().vitals().hp();a.mp=h.resources().vitals().mp();a.weight=next.weight();
                a.spellHistory=h.spells();a.itemHistory=h.items();a.targetHistory=h.targets();a.playerHealing=h.healing();
            }
            vipMutation=mutation;
        }
        if(vipEnabled&&vipOperation!=null&&vipOperation.sampled&&vipOperation.version!=vipMutation)throw new VipStaleCalculation();
        return true;
    }
    private record VipReplacement(MutableActor actor,ModernBattleLoadout loadout,ModernEquipmentHistory.State history,ModernInventoryWeight.Snapshot weight) {}
    /** No nested command can observe or consume a half-planned catch-up. Each
     * actor and every expiry segment is calculated before a single publication. */
    private boolean advanceVipRegen(long epoch) {
        if(planningVipRegen)throw new VipStaleCalculation();
        long beforeRevision=revision,now=lastObservedNanos;var beforePhase=phase;
        var planned=new ArrayList<ModernVipRegenTimeline.State>();
        planningVipRegen=true;
        try {
            for(var a:actors) {
                var before=new ModernVipRegenTimeline.State(a.loadout,equipmentHistory(a),a.regen,a.regenClock,a.weight,a.itemUse.stocks(),
                        a.ammo==null?List.of():a.ammo.stocks(),materialStocks(a));
                planned.add(ModernVipRegenTimeline.advance(before,activeStartedNanos,now,epoch,phase==Phase.ACTIVE,
                        preparation.snapshot().summary().config().mapId()==201,random));
            }
            long observed=observeTime(),wall=epochClock.getAsLong();
            if(wall<lastObservedEpochMillis){abortInternal(AbortReason.CLOCK_REGRESSED);return false;}
            lastObservedEpochMillis=wall;
            if(phase==Phase.ACTIVE&&observed-activeStartedNanos>=TimeUnit.MILLISECONDS.toNanos(limits.activeTimeoutMillis())) {
                abortInternal(AbortReason.ACTIVE_TIMEOUT);return false;
            }
            if(phase!=beforePhase||revision!=beforeRevision||!preparation.isLive()||closed)throw new VipStaleCalculation();
            long next=revision;boolean abilities=false;
            for(int i=0;i<actors.size();i++) {
                var candidate=planned.get(i);long deadline=ModernVipRegenTimeline.deadline(candidate.loadout());
                if(deadline>=0&&ModernVipRegenTimeline.effectiveEpoch(candidate.loadout(),observed,wall)>=deadline)throw new VipStaleCalculation();
                next=Math.max(next,candidate.history().worldRevision());abilities|=candidate.loadout()!=actors.get(i).loadout;
            }
            long mutation=abilities?Math.incrementExact(vipMutation):vipMutation;
            for(int i=0;i<actors.size();i++) {
                var a=actors.get(i);var candidate=planned.get(i);var h=candidate.history();
                a.loadout=candidate.loadout();a.equipmentResources=h.resources();a.hp=h.resources().vitals().hp();a.mp=h.resources().vitals().mp();
                a.spellHistory=h.spells();a.itemHistory=h.items();a.targetHistory=h.targets();a.playerHealing=h.healing();
                a.regen=candidate.regen();a.regenClock=candidate.clock();a.weight=candidate.weight();
            }
            revision=next;vipMutation=mutation;
            if(vipOperation!=null&&vipOperation.sampled&&vipOperation.version!=vipMutation)throw new VipStaleCalculation();
            return true;
        } catch(IllegalArgumentException|ArithmeticException failure) {
            long observed=observeTime();
            if(phase==Phase.ACTIVE&&observed-activeStartedNanos>=TimeUnit.MILLISECONDS.toNanos(limits.activeTimeoutMillis()))
                abortInternal(AbortReason.ACTIVE_TIMEOUT);
            else abortInternal(AbortReason.SERVER_ABORT);
            return false;
        } finally {planningVipRegen=false;}
    }
    private long vipRandom(ModernPhysicalCombat.BoundedRandom delegate,long bound) {
        // Timeline RNG has its own all-actor revision/phase fence. It must not
        // mark a not-yet-calculated player command as using obsolete abilities.
        if(vipEnabled&&!planningVipRegen&&vipOperation!=null&&!vipOperation.sampled){vipOperation.sampled=true;vipOperation.version=vipMutation;}
        return delegate.nextLong(bound);
    }
    private ModernVipLifecycle.Actor vipActor(MutableActor actor) {
        return new ModernVipLifecycle.Actor(vipWorld,actor.member.identity().humanKey(),actor.entityId,resourceLife(actor));
    }
    private static ModernPhysicalCombat.Fighter vipFighter(ModernVipCharacterState.Candidate candidate,ModernPhysicalCombat.Fighter previous,
            long now,long hp,long mp) {
        var before=previous.vip().state();
        var next=ModernVipCharacterState.acceptAt(candidate,before,before.ledger().actor(),before.ledger().equipmentRevision(),now,hp,mp);
        return ModernPhysicalCombat.qualifyVipRuntime(next,now,Math.incrementExact(previous.profile().equipmentRevision()),previous.armorProtection());
    }
    private void abortInternal(AbortReason reason) {
        if (phase == Phase.ABORTED || phase == Phase.FINISHED) return;
        nextRevision();clearAllBuffs(lastObservedNanos,revision);clearHonorPending();
        if(raceState!=null)raceState=ModernPetRace.abort(raceState,Math.max(raceState.observedElapsedNanos(),phase==Phase.ACTIVE?Math.max(0,lastObservedNanos-activeStartedNanos):0),revision);
        phase = Phase.ABORTED; abortReason = reason;
        if(npcRoster!=null){npcRoster.close();npcExchanges.clear();}
        if(questProgress!=null)questProgress=ModernQuestProgress.abort(questProgress);
        if(timeAttackState!=null)timeAttackState=ModernTimeAttack.abort(timeAttackState,revision);
        if(siegeState!=null){siegeState=siegeState.abort(revision);for(var a:actors)a.siegeLife=a.siegeLife.terminal();}
    }
    private void activateRace(){
        if(raceState!=null){raceState=ModernPetRace.activate(raceState,revision);preparation.enableIndividualRaceExits();}
        if(timeAttackState!=null)preparation.enableIndividualTimeAttackExits();
        if(siegeState!=null)siegeState=siegeState.activate(revision);
    }
    private long pace(MutableActor actor,ModernTimedEffects.Action action) {
        long base=switch(action){case MOVE->actor.stepIntervalNanos;case ATTACK->actor.attackIntervalNanos;case SPELL->actor.castIntervalNanos;};
        return timedEnabled&&base!=0?ModernTimedEffects.intervalNanos(base,action,actor.buffs):base;
    }
    private long elapsed(long now) {
        if(phase==Phase.LOADING || phase==Phase.COUNTDOWN)return 0;
        return Math.min(ModernTimedEffects.MAX_OBSERVED_NANOS,Math.max(0,now-activeStartedNanos));
    }
    private void expireBuffs(long now) {
        long next=Math.addExact(revision,1),elapsed=elapsed(now);boolean changed=false;
        var values=new ArrayList<ModernTimedEffects.State>(actors.size());
        for(var actor:actors) {
            var value=ModernTimedEffects.expire(actor.buffs,elapsed,next);values.add(value);changed|=value!=actor.buffs;
        }
        if(changed){nextRevision();for(int i=0;i<actors.size();i++)actors.get(i).buffs=values.get(i);}
    }
    private void clearBuffs(MutableActor actor,long now,long next) {
        clearArmor(actor,now,next);
        if(timedEnabled && actor.buffs.hasEffects())actor.buffs=ModernTimedEffects.clear(actor.buffs,
                Math.max(actor.buffs.observedNanos(),elapsed(now)),next);
    }
    private void clearAllBuffs(long now,long next){if(timedEnabled||armorProtectionEnabled)for(var actor:actors)clearBuffs(actor,now,next);}
    private static boolean timeAttackFinished(ModernTimeAttack.State state) {
        return state!=null&&state.phase()==ModernTimeAttack.Phase.FINISHED;
    }
    private List<ModernTimedEffects.State> planTimeAttackClear(ModernTimeAttack.State state,long now,long next) {
        return timedEnabled&&timeAttackFinished(state)?planClearedBuffs(now,next):null;
    }
    /** All fallible player/roster replacement plans precede this commit. */
    private void finishTimeAttack(List<ModernTimedEffects.State> cleared) {
        commitClearedBuffs(cleared);clearAllArmor(lastObservedNanos,revision);phase=Phase.FINISHED;
    }
    private List<ModernTimedEffects.State> planClearedBuffs(long now,long next) {
        return actors.stream().map(a->ModernTimedEffects.clear(a.buffs,Math.max(a.buffs.observedNanos(),elapsed(now)),next)).toList();
    }
    private void commitClearedBuffs(List<ModernTimedEffects.State> values) {
        if(values!=null)for(int i=0;i<actors.size();i++)actors.get(i).buffs=values.get(i);
    }
    private void nextRevision() {
        if (revision == Long.MAX_VALUE) { close(); throw new IllegalStateException("world revision exhausted"); }
        revision++;
    }
    /** All output paths start from this one immutable, monitor-owned capture.
     * No clock, SQL, inventory or NPC read occurs after choosing the capture. */
    private ModernBattleObservation.Capture capture() {
        requireOpen();
        for(var actor:actors)validateUseHistory(actor);
        for(var actor:actors)if(actor.equipmentResources!=null)requireResourceBalance(actor);
        var facts=new ArrayList<ModernBattleObservation.CapturedActor>();
        for(var a:actors) {
            var combat=a.loadout==null?null:a.loadout.combatant();
            var vital=combat==null?null:new ModernBattleObservation.Vital(
                    new ModernEquipmentVitals.Vitals(a.hp,combat.maxHp(),a.mp,combat.maxMp()),combat.armorClass(),combat.weaponType(),
                    a.attackSequence,a.attackRevision,a.hurtRevision,a.deathRevision,a.targetEntityId,a.attackIntervalNanos);
            var item=a.itemUse;
            var supplies=item==null?null:new ModernBattleObservation.Supplies(item.requestSequence(),item.requestRevision(),a.itemHistory,item.stocks(),
                    ModernItemRuntime.effect(a.itemHistory),ModernItemRuntime.delayMillis(a.itemHistory));
            var r=a.regen;
            var regen=!regenEnabled?null:new ModernBattleObservation.Regeneration(
                    new ModernBattleObservation.RegenAxis(r.hpGain(),r.hpLoss(),r.hpRevision(),r.hpBefore(),r.hpAfter()),
                    new ModernBattleObservation.RegenAxis(r.mpGain(),r.mpLoss(),r.mpRevision(),r.mpBefore(),r.mpAfter()),r.itemGain(),r.damageLoss());
            facts.add(new ModernBattleObservation.CapturedActor(a.snapshot(),a.prepared==null?null:a.prepared.scope(),a.loadout,
                    a.prepared==null?null:a.prepared.skills(),vital,a.equipmentResources,a.castIntervalNanos,a.spellHistory,a.targetHistory,a.spellRuntime,
                    supplies,weightEnabled?a.weight:null,timedEnabled?a.buffs:null,bowEnabled?a.ammo:null,regen,regenEnabled?a.regenClock:null,a.presentation,a.materials,a.playerHealing,a.armor));
        }
        var features=new ModernBattleObservation.Features(combatEnabled,spellsEnabled,damageMagicEnabled,itemsEnabled,weightEnabled,
                timedEnabled,bowEnabled,regenEnabled,fireStormEnabled,disintegrateEnabled,npcMagicEnabled,npcFireStormEnabled,npcDisintegrateEnabled,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled);
        var honor=new ModernBattleObservation.Honor(objectives,honorConfig==null?List.of():actors.stream().map(a->a.life).toList());
        var quest=new ModernBattleObservation.Quest(questProgress,List.copyOf(questObjectives.values()),
                questExchanges.values().stream().sorted(java.util.Comparator.comparingLong(ModernQuestObjectives.Exchange::revision)).toList(),
                questLivesEnabled()?actors.stream().map(a->a.questLife).toList():List.of());
        var time=timeAttackState==null?ModernTimeAttack.Snapshot.disabled():new ModernTimeAttack.Snapshot(timeAttackState,
                timeAttackExchanges.values().stream().sorted(java.util.Comparator.comparingLong(ModernTimeAttack.Exchange::revision)).toList());
        var siege=siegeState==null?ModernSiege.Snapshot.disabled():new ModernSiege.Snapshot(siegeState,actors.stream().map(a->a.siegeLife).toList());
        var modes=new ModernBattleObservation.Modes(isolatedForces(),honor,quest,time,siege,raceState,raceResultId,timeAttackResult,siegeResult);
        ModernBattleObservation.Npcs npcs=null;
        if(npcRoster!=null) {
            var receipts=new ArrayList<>(playerNpcExchanges.values());receipts.addAll(npcExchanges.values());
            receipts.sort(java.util.Comparator.comparingLong(ModernNpcBattle.Exchange::revision));
            npcs=new ModernBattleObservation.Npcs(npcRoster.snapshot(),receipts);
        }
        var clock=new ModernBattleObservation.Clock(lastObservedNanos-loadingStartedNanos,activeClockStarted?lastObservedNanos-activeStartedNanos:-1);
        // Prune only obsolete evidence, never derive it from current speed.
        // This shares the exact capture revision/clock and performs no I/O.
        if(hasteTargets!=null)hasteTargets=hasteTargets.reconcile(revision,phase,clock,loadingStartedNanos,facts,npcs);
        if(slowTargets!=null)slowTargets=slowTargets.reconcile(revision,phase,clock,loadingStartedNanos,facts,npcs);
        var capture=new ModernBattleObservation.Capture(new ModernTargetHistory.World(preparation.snapshot().summary().id(),preparation.matchId(),preparation.generation()),
                revision,phase,abortReason,preparation.snapshot(),map.manifest(),features,clock,limits,loadingStartedNanos,facts,modes,npcs,result,hasteTargets,slowTargets,armorProtectionEnabled?armorWorld:null);
        if(time.finished()&&timeAttackResult==null||siege.finished()&&siegeResult==null) {
            var world=capture.world();
            if(time.finished()&&timeAttackResult==null)timeAttackResult=ModernTimeAttackSettlement.event(world,java.util.UUID.randomUUID(),lastObservedEpochMillis);
            if(siege.finished()&&siegeResult==null)siegeResult=ModernSiegeSettlement.event(world,java.util.UUID.randomUUID(),lastObservedEpochMillis);
            capture=capture.withModes(new ModernBattleObservation.Modes(isolatedForces(),honor,quest,time,siege,raceState,raceResultId,timeAttackResult,siegeResult));
        }
        return capture;
    }
    /** New owner observation: one advance and one capture, never a legacy
     * Snapshot followed by a second owner/history read. Not a wire handler. */
    synchronized ModernBattleObservation.OwnerView observation(TournamentPresence.Owner owner,ModernRooms.Scope scope,long matchId,long generation) {
        requireActor(owner,scope,matchId,generation);
        return guarded(()->{advance();return capture().forOwner(scope);},()->{throw new IllegalStateException("battle observation retired");});
    }
    /** Trusted server capture for projections/tests; private data must be
     * scoped with forOwner before entering a future client response. */
    synchronized ModernBattleObservation.Capture serviceObservation() {
        requireOpen();return guarded(()->{advance();return capture();},this::capture);
    }
    /** A trusted service command and its exact post-command facts, under ONE
     * engine monitor. The callback may only invoke this engine, never service,
     * Rooms, JDBC or I/O. It does not advance a second time after the command. */
    record Observed<T extends ModernBattleCommand>(T outcome,ModernBattleObservation.Capture observation) {
        Observed {Objects.requireNonNull(outcome);Objects.requireNonNull(observation);if(outcome.observation()!=observation)throw invalid();}
        @Override public String toString(){return "ObservedBattleCommand[redacted]";}
    }
    synchronized <T extends ModernBattleCommand> Observed<T> observeCommand(Supplier<T> command) {
        requireOpen();var outcome=Objects.requireNonNull(Objects.requireNonNull(command).get());requireOpen();
        var observation=outcome.observation();
        if(observation.room()!=preparation.snapshot()||observation.identity().roomId()!=preparation.snapshot().summary().id()||observation.identity().matchId()!=preparation.matchId()
                ||observation.identity().generation()!=preparation.generation()||observation.revision()!=revision||observation.phase()!=phase
                ||observation.abortReason()!=abortReason||observation.clock().loadingElapsedNanos()!=lastObservedNanos-loadingStartedNanos
                ||observation.clock().activeElapsedNanos()!=(activeClockStarted?lastObservedNanos-activeStartedNanos:-1))throw invalid();
        return new Observed<>(outcome,observation);
    }
    // Scoped by this engine's monitor, never a JVM/thread-global setting. The
    // fence also runs after later advance() calls during a slow resolver, so a
    // death/respawn cannot be hidden by restoring the same HP or coordinates.
    private MutableActor commandLifeActor;
    private ModernBattleCommandLife.Proof commandLifeProof;
    private ModernBattleActionProtocol.Target commandLifeTarget;
    private boolean commandLifeClaimed;
    private static final class ChangedCommandLife extends RuntimeException {
        final ModernBattleCommandLife.Rejection reason;
        ChangedCommandLife(ModernBattleCommandLife.Rejection reason) {
            super("battle command life changed", null, false, false); this.reason=reason;
        }
    }
    private void checkCommandLife() {
        if(commandLifeActor==null)return;
        var reason=commandLifeRejection(commandLifeActor,commandLifeProof,commandLifeTarget);
        if(reason!=ModernBattleCommandLife.Rejection.NONE)throw new ChangedCommandLife(reason);
    }
    private ModernBattleCommandLife.Rejection commandLifeRejection(MutableActor actor,ModernBattleCommandLife.Proof proof,ModernBattleActionProtocol.Target target) {
        if(phase!=Phase.ACTIVE||!participating(actor))return ModernBattleCommandLife.Rejection.NOT_ACTIVE;
        if(Math.incrementExact(resourceLife(actor))!=proof.life())return ModernBattleCommandLife.Rejection.STALE_LIFE;
        if(combatEnabled&&actor.hp==0)return ModernBattleCommandLife.Rejection.DEAD;
        if(!commandTargetCurrent(target))return ModernBattleCommandLife.Rejection.STALE_TARGET;
        return ModernBattleCommandLife.Rejection.NONE;
    }
    private boolean commandTargetCurrent(ModernBattleActionProtocol.Target t) {
        return switch(t.kind()) {
            case NONE -> true;
            case PLAYER -> t.id()<=actors.size()&&combatEnabled&&actors.get((int)t.id()-1).hp>0
                    &&participating(actors.get((int)t.id()-1))&&Math.incrementExact(resourceLife(actors.get((int)t.id()-1)))==t.life();
            case NPC -> {
                var n=npcScope==null?null:npcActor(new ModernNpcRoster.Id(npcScope,t.id()),t.life());
                yield n!=null&&n.alive();
            }
            case QUEST -> {
                var a=questObjectives.get(((int)t.id()-1)/4);
                yield a!=null&&a.alive()&&a.id().slot()==t.id()&&a.bornRevision()==t.life();
            }
            case HONOR -> objectives.stream().anyMatch(o->o.entityId()==t.id()&&o.hp()>0);
            case TIME_TOWER -> timeAttackState!=null&&timeAttackState.towerHp()>0;
            case SIEGE -> siegeState!=null&&siegeState.objectives().get((int)t.id()).hp()>0;
        };
    }
    /** Trusted service seam: exactly one typed command on THIS engine, no
     * Rooms/service/JDBC/network call in the callback. Owner/scope is checked
     * before advancing; life is checked after advancing under the same lock.
     * Not yet reachable from a negotiated application message.
     */
    synchronized <T extends ModernBattleCommand> ModernBattleCommandLife.Result<T> observeLifeCommand(
            TournamentPresence.Owner owner,ModernBattleCommandLife.Proof proof,Supplier<T> command) {
        return observeLifeCommand(owner,proof,ModernBattleActionProtocol.NO_TARGET,command);
    }
    synchronized <T extends ModernBattleCommand> ModernBattleCommandLife.Result<T> observeLifeCommand(
            TournamentPresence.Owner owner,ModernBattleCommandLife.Proof proof,ModernBattleActionProtocol.Target target,Supplier<T> command) {
        Objects.requireNonNull(proof);Objects.requireNonNull(command);
        Objects.requireNonNull(target);
        if(commandLifeActor!=null)throw new IllegalStateException("nested battle command admission");
        var actor=requireActor(owner,proof.player(),proof.world().matchId(),proof.world().generation());
        if(proof.world().roomId()!=preparation.snapshot().summary().id()||proof.entity()!=actor.entityId||proof.revision()>revision)
            throw new SecurityException("battle command identity mismatch");
        return guarded(()->{
            advance();
            var rejection=commandLifeRejection(actor,proof,target);
            if(rejection!=ModernBattleCommandLife.Rejection.NONE)return ModernBattleCommandLife.Result.rejected(rejection,capture());
            commandLifeActor=actor;commandLifeProof=proof;commandLifeTarget=target;commandLifeClaimed=false;
            try {
                var observed=observeCommand(command);
                if(!commandLifeClaimed)throw new SecurityException("battle command did not authenticate its actor");
                return ModernBattleCommandLife.Result.accepted(observed);
            }
            catch(ChangedCommandLife changed) { return ModernBattleCommandLife.Result.rejected(changed.reason,capture()); }
            finally { commandLifeActor=null;commandLifeProof=null;commandLifeTarget=null;commandLifeClaimed=false; }
        },()->ModernBattleCommandLife.Result.rejected(ModernBattleCommandLife.Rejection.NOT_ACTIVE,capture()));
    }
    private Snapshot current() {return ModernLegacyBattleProjection.from(capture());}
    /** Canonical spell/target and item request/stock binding. Complete owner
     * export validates resource totals too; no legacy item State is needed. */
    private void validateUseHistory(MutableActor a) {
        if(a.spellRuntime!=null) {
            if(playerHealingEnabled)a.spellRuntime.validate(equipmentHistory(a));
            else a.spellRuntime.validate(revision,a.spellHistory,a.targetHistory);
        }
        if(a.materials!=null)new ModernSlowTransaction.State(equipmentHistory(a),a.spellRuntime,a.materials);
        if(a.itemUse!=null) {
            var h=a.itemHistory;a.itemUse.validate(revision,h);
            if(h.fixedRecovery()!=a.loadout.potionRecovery()||h.equipmentRevision()!=a.equipmentResources.equipmentRevision()
                    ||regenEnabled&&h.restored()!=a.regen.itemGain())throw invalid();
        }
    }
    private MoveResult moved(MoveStatus status) { return new MoveResult(status, capture()); }
    private AttackResult attacked(AttackStatus status) { return new AttackResult(status, capture()); }
    private ItemResult used(ModernItemProtocol.Reason reason) { return new ItemResult(reason,capture()); }
    private void requireOpen() { if (closed) throw new IllegalStateException("world engine closed"); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern world input"); }
}
