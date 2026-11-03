package local.lito381.server;

import static local.lito381.server.ModernWorldProtocol.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Owned world-entry/movement, opt-in combat and explicitly journaled results.
 * Registration is a trusted Navigation operation, never a wire credential. The
 * reader must retain that Navigation's authenticated source/selection authority
 * and perform its actual bounded backend read using the supplied absolute deadline.
 *
 * No Rooms, Navigation, reader, Engine or ticket method is called with gate held.
 * A cancelled reader which ignores interruption still occupies its preparation
 * capacity until its real finally. Terminal frames are bounded by maxMatches;
 * cancellation is not worker retirement or proof of a rolled-back disk operation.
 */
final class ModernWorldService implements AutoCloseable {
    @FunctionalInterface interface FreshReader {
        ModernApplicationProtocol.CharacterRow read(long absoluteDeadlineNanos) throws Exception;
    }
    @FunctionalInterface interface PhysicalReader {
        ModernPhysicalCombat.Fighter read(long absoluteDeadlineNanos) throws Exception;
        default ModernPhysicalCombat.Fighter read(long absoluteDeadlineNanos,boolean armorProtection) throws Exception {
            return read(absoluteDeadlineNanos);
        }
    }
    /** Trusted source opt-in, independent of a peer claiming DATA support. A
     * fresh fighter includes the complete VIP ledger and independent store. */
    @FunctionalInterface interface FullVipReader extends PhysicalReader {
        ModernPhysicalCombat.Fighter readVip(long absoluteDeadlineNanos,boolean armorProtection)throws Exception;
        @Override default ModernPhysicalCombat.Fighter read(long deadline)throws Exception {
            throw new IllegalStateException("full VIP reader requires an explicit world selection");
        }
    }
    @FunctionalInterface interface SkillsReader {
        ModernSkills.Snapshot read(long absoluteDeadlineNanos) throws Exception;
    }
    @FunctionalInterface interface DisintegrateReader {
        ModernDisintegrateCatalog.Snapshot read(long absoluteDeadlineNanos) throws Exception;
    }
    @FunctionalInterface interface GreaterHasteReader {
        ModernGreaterHasteCatalog.Snapshot read(long absoluteDeadlineNanos) throws Exception;
    }
    @FunctionalInterface interface HasteReader {
        ModernHasteCatalog.Snapshot read(long absoluteDeadlineNanos) throws Exception;
    }
    @FunctionalInterface interface SlowReader {
        ModernSlowCatalog.Snapshot read(long absoluteDeadlineNanos) throws Exception;
    }
    interface MatchItems {
        List<ModernShopAuthority.Stack> capture(long matchId,long generation,long absoluteDeadlineNanos) throws Exception;
        default List<ModernShopAuthority.Stack> capture(long matchId,long generation,long absoluteDeadlineNanos,ModernInventoryWeight.Basis basis) throws Exception {
            if(basis!=null)throw new IllegalStateException("weight capture was not negotiated");
            return capture(matchId,generation,absoluteDeadlineNanos);
        }
        void terminal(long matchId,long generation);
        default List<ModernShopAuthority.Stack> capture(long match,long generation,long deadline,ModernInventoryWeight.Basis basis,ModernMatchDescriptor descriptor)throws Exception {
            return capture(match,generation,deadline,basis);
        }
    }
    /** Optional binding-local phase durations; preparation and journal budgets remain global. */
    record ClockOverrides(Integer loadTimeoutMillis, Long countdownNanos, Integer activeTimeoutMillis) {
        ClockOverrides {
            new ModernBattleEngine.Limits(loadTimeoutMillis==null?50:loadTimeoutMillis,
                    countdownNanos==null?0:countdownNanos,activeTimeoutMillis==null?50:activeTimeoutMillis);
        }
        ModernBattleEngine.Limits resolve(ModernBattleEngine.Limits fallback) {
            Objects.requireNonNull(fallback);
            return new ModernBattleEngine.Limits(loadTimeoutMillis==null?fallback.loadTimeoutMillis():loadTimeoutMillis,
                    countdownNanos==null?fallback.countdownNanos():countdownNanos,
                    activeTimeoutMillis==null?fallback.activeTimeoutMillis():activeTimeoutMillis);
        }
    }
    record Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos,
                   long castIntervalNanos, boolean damageMagic, boolean serverBots, boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled,int roomCapacity,boolean greaterHasteEnabled,boolean hasteEnabled,boolean slowEnabled,boolean playerHealingEnabled,boolean armorProtectionEnabled,boolean fullVipEnabled) {
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,
                long castIntervalNanos,boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled,int roomCapacity,boolean greaterHasteEnabled,boolean hasteEnabled,boolean slowEnabled,boolean playerHealingEnabled,boolean armorProtectionEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,
                long castIntervalNanos,boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled,int roomCapacity,boolean greaterHasteEnabled,boolean hasteEnabled,boolean slowEnabled,boolean playerHealingEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,false);
        }
        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos,
                   long castIntervalNanos, boolean damageMagic, boolean serverBots, boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled,int roomCapacity,boolean greaterHasteEnabled,boolean hasteEnabled,boolean slowEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,false);
        }
        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos,
                   long castIntervalNanos, boolean damageMagic, boolean serverBots, boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled,int roomCapacity,boolean greaterHasteEnabled,boolean hasteEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,false);
        }

        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos,
                   long castIntervalNanos, boolean damageMagic, boolean serverBots, boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled,int roomCapacity,boolean greaterHasteEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled,int roomCapacity){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege,boolean disintegrateEnabled){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,100);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc,ModernSiege.Config siege){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race,ModernNpcEncounter npc){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,null);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps,ModernPetRace.Course race){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,null);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides,List<ModernWorldMap.Cell> teamCamps){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,null);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled,ClockOverrides clockOverrides){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,List.of());
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor,boolean fireStormEnabled){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,null);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled,ModernHonor.Config honor){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled,boolean regenEnabled){
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,null);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled,boolean bowEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,bowEnabled,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,long castIntervalNanos,
                boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled,boolean timedEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,timedEnabled,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,
                long castIntervalNanos,boolean damageMagic,boolean serverBots,boolean matchItems,boolean weightEnabled) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,weightEnabled,false);
        }
        Mapping(ModernWorldBinding binding,ModernWorldMap.Cell anchor,long stepIntervalNanos,long attackIntervalNanos,
                long castIntervalNanos,boolean damageMagic,boolean serverBots,boolean matchItems) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,matchItems,false);
        }
        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos,
                long castIntervalNanos, boolean damageMagic, boolean serverBots) {
            this(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,false);
        }
        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos,
                long castIntervalNanos, boolean damageMagic) {
            this(binding, anchor, stepIntervalNanos, attackIntervalNanos, castIntervalNanos, damageMagic, false);
        }
        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos, long castIntervalNanos) {
            this(binding, anchor, stepIntervalNanos, attackIntervalNanos, castIntervalNanos, false);
        }
        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos, long attackIntervalNanos) {
            this(binding, anchor, stepIntervalNanos, attackIntervalNanos, 0);
        }
        Mapping(ModernWorldBinding binding, ModernWorldMap.Cell anchor, long stepIntervalNanos) {
            this(binding, anchor, stepIntervalNanos, 0);
        }
        Mapping {
            if(fullVipEnabled&&(binding==null||binding.key().mode()!=BattleRules.Mode.DEATHMATCH.wireId()
                    ||attackIntervalNanos==0||serverBots||honor!=null||race!=null||npc!=null||siege!=null))throw invalid();
            if(armorProtectionEnabled&&(attackIntervalNanos==0||!weightEnabled||race!=null))throw invalid();
            if(playerHealingEnabled&&(!weightEnabled||castIntervalNanos==0||race!=null))throw invalid();
            Objects.requireNonNull(binding); Objects.requireNonNull(anchor);
            if(slowEnabled&&(!timedEnabled||castIntervalNanos==0||race!=null))throw invalid();
            if(hasteEnabled&&(!timedEnabled||castIntervalNanos==0||race!=null||siege!=null&&!slowEnabled))throw invalid();
            if(greaterHasteEnabled&&(!timedEnabled||castIntervalNanos==0||npc!=null||race!=null||siege!=null&&!slowEnabled))throw invalid();
            if(roomCapacity<2||roomCapacity>100||roomCapacity!=100&&(binding.key().mode()!=1||attackIntervalNanos==0))throw invalid();
            teamCamps=List.copyOf(teamCamps);
            if(disintegrateEnabled&&(!regenEnabled||!damageMagic||npc!=null||race!=null||siege!=null
                    ||binding.key().mode()!=1&&(binding.key().mode()!=5||honor==null)))throw invalid();
            if(siege!=null&&(!siege.key().equals(binding.key())||attackIntervalNanos==0||damageMagic||timedEnabled&&!slowEnabled||bowEnabled
                    ||honor!=null||fireStormEnabled||race!=null||npc!=null||!teamCamps.isEmpty()
                    ||clockOverrides==null||clockOverrides.activeTimeoutMillis()==null
                    ||clockOverrides.activeTimeoutMillis()!=siege.durationMillis()))throw invalid();
            if(npc!=null) {
                npc.validate(binding);
                if(attackIntervalNanos==0||(npc.lightArrow()||npc.disintegrate()||npc.fireStorm())&&castIntervalNanos==0||(npc.disintegrate()||npc.fireStorm())&&!regenEnabled||damageMagic||timedEnabled&&!hasteEnabled&&!slowEnabled||bowEnabled||honor!=null||fireStormEnabled||race!=null
                        ||!teamCamps.equals(npc.lanes())||npc.timeAttack()!=null&&serverBots)throw invalid();
            }
            if(race!=null&&(binding.key().mode()!=0||!race.key().equals(binding.key())||!race.map().manifest().equals(binding.map().manifest())
                    ||race.stepIntervalNanos()!=stepIntervalNanos||attackIntervalNanos!=0||castIntervalNanos!=0||damageMagic||serverBots||matchItems
                    ||weightEnabled||timedEnabled||bowEnabled||regenEnabled||honor!=null||fireStormEnabled||!teamCamps.isEmpty()))throw invalid();
            if(!teamCamps.isEmpty()) {
                boolean isolated=npc!=null&&npc.questPlan()!=null;
                boolean cooperative=npc!=null&&npc.timeAttack()!=null;
                if(cooperative?teamCamps.size()!=1:teamCamps.size()!=2 || !isolated&&teamCamps.get(0).equals(teamCamps.get(1))
                        || !BattleRules.Mode.fromWire(binding.key().mode()).teamBased())throw invalid();
                try{if(!isolated)binding.map().validateSpawns(teamCamps);else for(var camp:teamCamps)binding.map().validateSpawns(List.of(camp));}catch(ModernWorldMap.MapException failure){throw invalid();}
                if(honor!=null&&!teamCamps.equals(honor.teamSpawns()))throw invalid();
            }
            if(honor!=null){if(!regenEnabled||!damageMagic)throw invalid();try{honor.validate(binding);}catch(ModernWorldMap.MapException e){throw invalid();}}
            if(fireStormEnabled&&(!regenEnabled||!damageMagic||binding.key().mode()!=1&&(binding.key().mode()!=5||honor==null)))throw invalid();
            if (damageMagic && castIntervalNanos == 0) throw invalid();
            if (serverBots && attackIntervalNanos == 0) throw invalid();
            if (matchItems && attackIntervalNanos == 0) throw invalid();
            if(weightEnabled&&!matchItems)throw invalid();
            if(timedEnabled && (!weightEnabled || stepIntervalNanos%1_000_000!=0 || attackIntervalNanos%1_000_000!=0))throw invalid();
            if(bowEnabled&&!timedEnabled)throw invalid();
            if(regenEnabled&&(!weightEnabled||attackIntervalNanos==0))throw invalid();
            if (!binding.map().standing(anchor) || stepIntervalNanos < TimeUnit.MILLISECONDS.toNanos(1)
                    || stepIntervalNanos > TimeUnit.SECONDS.toNanos(10)
                    || attackIntervalNanos != 0 && (attackIntervalNanos < TimeUnit.MILLISECONDS.toNanos(1)
                        || attackIntervalNanos > TimeUnit.SECONDS.toNanos(10)
                        || npc==null&&!BattleRules.Mode.fromWire(binding.key().mode()).playerVersusPlayer())) throw invalid();
            if (castIntervalNanos != 0 && (attackIntervalNanos == 0 || castIntervalNanos < TimeUnit.MILLISECONDS.toNanos(10)
                    || castIntervalNanos > TimeUnit.SECONDS.toNanos(10) || castIntervalNanos % 1_000_000 != 0)) throw invalid();
        }
        boolean npcFireStormEnabled(){return npc!=null&&npc.fireStorm();}
        boolean npcDisintegrateEnabled(){return npc!=null&&npc.disintegrate();}
        boolean combatEnabled() { return attackIntervalNanos != 0; }
        int playerCapacity(){return npc==null?roomCapacity:Math.min(roomCapacity,npc.playerCapacity());}
        ModernTimeAttack.Plan timeAttack(){return npc==null?null:npc.timeAttack();}
        boolean spellsEnabled() { return castIntervalNanos != 0; }
        Mapping withClockOverrides(ClockOverrides clocks) {
            return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                    matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clocks,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,fullVipEnabled);
        }
        Mapping withRace(ModernPetRace.Course course){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,course,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,fullVipEnabled);}
        Mapping withSiege(ModernSiege.Config config){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,
                new ClockOverrides(clockOverrides==null?null:clockOverrides.loadTimeoutMillis(),clockOverrides==null?null:clockOverrides.countdownNanos(),Math.toIntExact(config.durationMillis())),teamCamps,race,npc,config,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,fullVipEnabled);}
        Mapping withDisintegrate(boolean enabled){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,enabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,fullVipEnabled);}
        Mapping withGreaterHaste(boolean enabled){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,enabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,fullVipEnabled);}
        Mapping withHaste(boolean enabled){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,enabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,fullVipEnabled);}
        Mapping withSlow(boolean enabled){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,enabled,playerHealingEnabled,armorProtectionEnabled,fullVipEnabled);}
        Mapping withPlayerHealing(boolean enabled){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,enabled,armorProtectionEnabled,fullVipEnabled);}
        Mapping withArmorProtection(boolean enabled){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,enabled,fullVipEnabled);}
        List<ModernWorldMap.Cell> spawnCamps() {
            return !teamCamps.isEmpty()?teamCamps:honor!=null&&!honor.teamSpawns().isEmpty()?honor.teamSpawns():List.of(anchor);
        }
        Mapping withFullVip(boolean enabled){return new Mapping(binding,anchor,stepIntervalNanos,attackIntervalNanos,castIntervalNanos,damageMagic,serverBots,
                matchItems,weightEnabled,timedEnabled,bowEnabled,regenEnabled,honor,fireStormEnabled,clockOverrides,teamCamps,race,npc,siege,disintegrateEnabled,roomCapacity,greaterHasteEnabled,hasteEnabled,slowEnabled,playerHealingEnabled,armorProtectionEnabled,enabled);}
    }
    record Limits(int maxMatches, int preparationWorkers, int preparationQueue,
                  int preparationTimeoutMillis, int tickMillis, ModernBattleEngine.Limits engineLimits) {
        Limits {
            if (maxMatches < 1 || maxMatches > 100 || preparationWorkers < 1 || preparationWorkers > 16
                    || preparationQueue < 0 || preparationQueue > 100
                    || preparationTimeoutMillis < 50 || preparationTimeoutMillis > 30_000
                    || tickMillis < 5 || tickMillis > 1_000 || engineLimits == null) throw invalid();
        }
    }
    private static final int MAX_REGISTRATIONS = 10_000;
    private enum JobState { NEW, QUEUED, RUNNING, RETIRED }
    private static final class Registration {
        final TournamentPresence.Owner owner; final ModernRooms.Scope scope; final FreshReader reader;
        final PhysicalReader physical;
        final SkillsReader skills;
        final DisintegrateReader disintegrate;
        final GreaterHasteReader greaterHaste;
        final HasteReader haste;
        final SlowReader slow;
        final boolean playerHealingSource;
        final boolean armorProtectionSource;
        final boolean resultProtocol;
        final boolean damageMagicProtocol;
        final MatchItems matchItems;
        final boolean weightProtocol;
        final boolean timedProtocol;
        final boolean bowProtocol;
        final boolean regenProtocol;
        final boolean honorProtocol;
        final boolean fireStormProtocol;
        final ModernSessionBoundary.Negotiated protocol;
        final ModernNativeResourceLedger nativeLedger=new ModernNativeResourceLedger();
        boolean reserving; Entry entry;
        Registration(TournamentPresence.Owner owner, ModernRooms.Scope scope, FreshReader reader, PhysicalReader physical, boolean resultProtocol, SkillsReader skills, boolean damageMagicProtocol, MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol,DisintegrateReader disintegrate,GreaterHasteReader greaterHaste,HasteReader haste,SlowReader slow,boolean playerHealingSource,boolean armorProtectionSource) {
            this.armorProtectionSource=armorProtectionSource;
            this.playerHealingSource=playerHealingSource;
            this.slow=slow;
            this.haste=haste;
            this.protocol=protocol;
            this.disintegrate=disintegrate;
            this.greaterHaste=greaterHaste;
            this.fireStormProtocol=fireStormProtocol;
            this.owner = owner; this.scope = scope; this.reader = reader; this.physical = physical;
            this.resultProtocol = resultProtocol;
            this.skills = skills;
            this.damageMagicProtocol = damageMagicProtocol;
            this.matchItems=matchItems;
            this.weightProtocol=weightProtocol;
            this.timedProtocol=timedProtocol;
            this.bowProtocol=bowProtocol;
            this.regenProtocol=regenProtocol;
            this.honorProtocol=honorProtocol;
        }
    }
    private static final class Entry {
        final ModernRooms.FrozenPreparation ticket;
        final Mapping mapping;
        final ModernMatchDescriptor descriptor;
        final List<Registration> participants;
        final List<ModernWorldMap.Cell> spawns;
        final long deadline;
        final Set<Registration> followers;
        final AtomicBoolean ticketReleased = new AtomicBoolean();
        final AtomicReference<Thread> releaseOwner = new AtomicReference<>();
        final CountDownLatch releaseDone = new CountDownLatch(1);
        final CountDownLatch terminationDone = new CountDownLatch(1);
        Thread terminationOwner;
        boolean terminationFailed;
        final AtomicBoolean cancelled = new AtomicBoolean();
        Publication output;
        ModernBattleEngine engine;
        Frame frame(){return output.legacy().frame();}
        ModernCombatProtocol.Frame combat(){return output.legacy().combat();}
        ModernNpcProtocol.Frame npc(){return output.legacy().npc();}
        ModernBattlePublication publication;
        final ModernNpcProtocol.Resources npcResources;
        final ModernNpcNames.Table npcNames;
        volatile ModernMatchSettlement.Event cooperativeResult;
        volatile ModernMatchSettlement.Event siegeResult;
        final Object npcGate=new Object();
        boolean npcStarted;
        long engineRevision;
        boolean terminal, retained = true, retired;
        boolean settlementQueued;
        final ModernMatchSettlement.Reservation settlementReservation;
        final ModernPetRaceSettlement.Reservation raceReservation;
        JobState jobState = JobState.NEW;
        Thread worker;
        PreparationJob job;
        Entry(ModernRooms.FrozenPreparation ticket, Mapping mapping, List<Registration> participants,
              List<ModernWorldMap.Cell> spawns, long deadline, Frame frame, ModernMatchSettlement.Reservation settlementReservation,ModernPetRaceSettlement.Reservation raceReservation) {
            this.ticket = ticket; this.mapping = mapping; this.participants = List.copyOf(participants);
            this.spawns = List.copyOf(spawns); this.deadline = deadline;
            this.descriptor=Objects.requireNonNull(frame.descriptor());
            npcResources=mapping.npc()==null?null:ModernNpcProtocol.Resources.from(mapping.npc().catalog());
            npcNames=mapping.npc()==null?null:ModernNpcNames.Table.from(mapping.npc().catalog());
            followers = new HashSet<>(participants);
            this.settlementReservation = settlementReservation;
            this.raceReservation=raceReservation;
            output=new Publication(this,null,frame,ModernBattleLifecycle.preparing(frame),null);
        }
    }

    /** Canonical validation and lifecycle are eager and atomic. Strict legacy
     * wire projections are requested only by a legacy consumer, never by the
     * scheduler, canonical READ, terminal fence or settlement writer. */
    private record LegacyViews(Frame frame,ModernCombatProtocol.Frame combat,ModernNpcProtocol.Frame npc) {
        LegacyViews {
            Objects.requireNonNull(frame);
            if(combat!=null&&!combat.world().equals(frame)||npc!=null&&!npc.combat().equals(combat))throw invalid();
        }
    }
    private static final class Publication {
        final Entry entry;
        final ModernBattlePublication authority;
        final Frame preparing;
        final ModernBattleLifecycle lifecycle;
        private volatile LegacyViews legacy;
        Publication(Entry entry,ModernBattlePublication authority,Frame preparing,ModernBattleLifecycle lifecycle,LegacyViews legacy) {
            this.entry=entry;this.authority=authority;this.preparing=preparing;this.lifecycle=Objects.requireNonNull(lifecycle);this.legacy=legacy;
            if(authority==null?preparing==null:preparing!=null||lifecycle.revision()<=authority.world().revision()
                    ||!authority.descriptor().equals(entry.descriptor)
                    ||authority.world().identity().matchId()!=entry.ticket.matchId()
                    ||authority.world().identity().generation()!=entry.ticket.generation()
                    ||!authority.world().room().equals(entry.ticket.snapshot()))throw invalid();
        }
        ModernBattlePublication authority(){return authority;}
        Publication withLifecycle(ModernBattleLifecycle next){return next==lifecycle?this:new Publication(entry,authority,preparing,next,null);}
        Publication withClock(ModernBattlePublication next){return new Publication(entry,next,null,lifecycle,legacy);}
        synchronized LegacyViews legacy() {
            if(legacy!=null)return legacy;
            var c=authority==null?null:authority.capture();
            var snapshot=c==null?null:ModernLegacyBattleProjection.from(c);
            var raw=snapshot==null?preparing:fromEngine(snapshot,Math.incrementExact(c.revision()),entry,c.modes().timeAttackResult(),c.modes().siegeResult());
            var completion=raw.completion()==null?null:new Completion(raw.completion().event(),lifecycle.settlement());
            var time=raw.timeAttack();var siege=raw.siege();
            if(lifecycle.phase()==Phase.ABORTED&&c==null) {
                if(time!=null)time=time.abortedBeforeEngine();
                if(siege!=null)siege=siege.abortedBeforeEngine();
            }
            if(lifecycle.result()!=null) {
                if(time!=null&&time.receipt()!=null)time=time.withSettlement(lifecycle.settlement());
                if(siege!=null&&siege.receipt()!=null)siege=siege.withSettlement(lifecycle.settlement());
            }
            var frame=new Frame(raw.matchId(),raw.generation(),lifecycle.revision(),lifecycle.phase(),lifecycle.abortReason(),
                    raw.room(),raw.manifest(),raw.actors(),completion,raw.casts(),entry.descriptor,lifecycle.race(),
                    raw.audienceLane(),raw.quest(),time,siege);
            var combat=snapshot==null?null:fromCombat(snapshot,frame);
            var npc=c==null||c.npcs()==null?null:npcFrame(entry,ModernLegacyBattleProjection.npcs(c),c.modes().timeAttackResult()).withCombat(combat);
            var candidate=new LegacyViews(frame,combat,npc);
            legacy=candidate;return candidate;
        }
        @Override public String toString(){return "ServiceBattlePublication[redacted]";}
    }
    private static Publication publication(Entry entry,ModernBattleObservation.Capture capture) {
        var authority=authority(entry,capture);
        return new Publication(entry,authority,null,ModernBattleLifecycle.from(authority),null);
    }
    private static ModernBattlePublication authority(Entry entry,ModernBattleObservation.Capture capture) {
        if(entry.mapping.fullVipEnabled())return ModernBattlePublication.fullVip(capture,entry.descriptor);
        if(entry.mapping.armorProtectionEnabled())return ModernBattlePublication.armorProtection(capture,entry.descriptor,entry.npcResources,entry.npcNames);
        return entry.mapping.playerHealingEnabled()
                ?ModernBattlePublication.playerHealing(capture,entry.descriptor,entry.npcResources,entry.npcNames)
                :new ModernBattlePublication(capture,entry.descriptor,entry.npcResources,entry.npcNames);
    }
    /** Called under gate only. Contains no fallible operations. */
    private static void assignPublication(Entry entry,Publication next) {
        entry.output=next;entry.publication=next.authority();
        if(next.authority()!=null) {
            entry.engineRevision=next.authority().world().revision();
            entry.cooperativeResult=next.authority().world().modes().timeAttackResult();
            entry.siegeResult=next.authority().world().modes().siegeResult();
        }
    }

    private final Object gate = new Object();
    private final ModernRooms rooms;
    private final Map<ModernWorldBinding.Key, Mapping> mappings;
    private NativePlayerLoadoutAdapter nativePlayerLoadout;
    private Map<ModernWorldBinding.Key,ModernWorldResourceProtocol.Plan> nativeResourcePlans=Map.of();
    private Map<ModernWorldBinding.Key,ModernNativeCanonicalWorldProtocol.SourceAttachment> nativeCanonicalSources=Map.of();
    private ModernNativeCatalogWorldProtocol.Catalog nativeCatalog;
    private ModernNativeActorWorldProtocol.Identity nativeActorIdentity;
    private final Map<ModernWorldBinding.Key, ModernSiege.Space> siegeSpaces;
    private final Map<Integer,Double> weightRates;
    private final Limits limits;
    private final ModernPhysicalCombat.BoundedRandom random;
    private final ModernMatchSettlement.Store settlementStore;
    private final ModernMatchSettlement.JournaledStore journal;
    private final ModernPetRaceSettlement.JournaledStore raceJournal;
    private final ThreadPoolExecutor settlementWorkers;
    private final Map<TournamentPresence.Owner, Registration> registrations = new HashMap<>();
    private final Set<Entry> entries = new HashSet<>();
    private final ThreadPoolExecutor workers;
    private final ScheduledThreadPoolExecutor scheduler;
    private final Semaphore preparationAdmission;
    private int reservations;
    private int settlementTasks;
    private boolean closed;
    private volatile boolean cleanupFailed;

    private final class PreparationJob implements Runnable {
        final Entry entry;
        PreparationJob(Entry entry) { this.entry = entry; }
        @Override public void run() { prepareWorker(entry); }
    }

    ModernWorldService(ModernRooms rooms, Map<ModernWorldBinding.Key, Mapping> mappings, Limits limits) {
        this(rooms, mappings, limits, bound -> java.util.concurrent.ThreadLocalRandom.current().nextLong(bound));
    }

    /** Default-off source adapter opt-in for the existing numerical PC melee route.
     * This grants no Native resource/actor envelope or renderer qualification. */
    static ModernWorldService withNativeSourceMelee(ModernRooms rooms,
            Map<ModernWorldBinding.Key, Mapping> mappings, Limits limits, NativePlayerLoadoutAdapter adapter) {
        Objects.requireNonNull(adapter);
        if (mappings == null || mappings.isEmpty()) throw invalid();
        for (var mapping : mappings.values()) {
            var descriptor = ModernMatchDescriptor.of(mapping, limits, false);
            if (descriptor.ruleset() != ModernMatchDescriptor.Ruleset.PVP_SKIRMISH
                    || descriptor.features() != ModernMatchDescriptor.COMBAT || mapping.serverBots())
                throw new IllegalArgumentException("Native source adapter requires NEW_MATCH_MELEE_V1 PC melee only");
        }
        var service = new ModernWorldService(rooms, mappings, limits);
        service.nativePlayerLoadout = adapter;
        return service;
    }
    NativePlayerLoadoutAdapter nativePlayerLoadoutAdapter() { return nativePlayerLoadout; }
    /** Explicit candidate factory; v1 admits navigation only until combat proofs gain a Native envelope. */
    static ModernWorldService withNativeResources(ModernRooms rooms,Map<ModernWorldBinding.Key,Mapping> mappings,Limits limits,
                                                  Map<ModernWorldBinding.Key,ModernWorldResourceProtocol.Plan> plans) {
        var safe=Map.copyOf(plans);if(safe.isEmpty())throw invalid();
        for(var e:safe.entrySet()) {
            var m=mappings.get(e.getKey());var p=e.getValue();
            if(m==null||m.combatEnabled()||m.binding().map().mapId()!=p.mapId()||!m.binding().map().digest().equals(p.geometryDigest()))throw invalid();
        }
        var service=new ModernWorldService(rooms,mappings,limits);service.nativeResourcePlans=safe;return service;
    }
    boolean nativeResourcesEnabled(){return !nativeResourcePlans.isEmpty();}
    static ModernWorldService withNativeCanonicalResources(ModernRooms rooms,Map<ModernWorldBinding.Key,Mapping> mappings,Limits limits,
                                                          Map<ModernWorldBinding.Key,ModernWorldResourceProtocol.Plan> plans) {
        var safe=Map.copyOf(plans);if(safe.isEmpty())throw invalid();
        var sources=new HashMap<ModernWorldBinding.Key,ModernNativeCanonicalWorldProtocol.SourceAttachment>();
        for(var e:safe.entrySet()) {
            var mapping=mappings.get(e.getKey());
            if(mapping==null||mapping.fullVipEnabled())throw invalid();
            sources.put(e.getKey(),new ModernNativeCanonicalWorldProtocol.SourceAttachment(e.getValue(),mapping.binding().map().manifest()));
        }
        var service=new ModernWorldService(rooms,mappings,limits);
        service.nativeResourcePlans=safe;service.nativeCanonicalSources=Map.copyOf(sources);return service;
    }
    static ModernWorldService withNativeCatalogCanonicalResources(ModernRooms rooms,Map<ModernWorldBinding.Key,Mapping> mappings,Limits limits,
            Map<ModernWorldBinding.Key,ModernWorldResourceProtocol.Plan> plans,ModernNativeCatalogWorldProtocol.Catalog catalog) {
        Objects.requireNonNull(catalog);
        var service=withNativeCanonicalResources(rooms,mappings,limits,plans);service.nativeCatalog=catalog;return service;
    }
    boolean nativeCatalogEnabled(){return nativeCatalog!=null;}
    static ModernWorldService withNativeActorCanonicalResources(ModernRooms rooms,Map<ModernWorldBinding.Key,Mapping> mappings,Limits limits,
            Map<ModernWorldBinding.Key,ModernWorldResourceProtocol.Plan> plans,ModernNativeActorWorldProtocol.Identity identity) {
        Objects.requireNonNull(identity);if(mappings.values().stream().anyMatch(Mapping::combatEnabled))throw invalid();
        var service=withNativeCatalogCanonicalResources(rooms,mappings,limits,plans,identity.catalog());service.nativeActorIdentity=identity;return service;
    }
    boolean nativeActorEnabled(){return nativeActorIdentity!=null;}
    ModernNativeActorWorldProtocol.Result nativeActorWorld(TournamentPresence.Owner owner,
            ModernNativeActorWorldProtocol.Request request,ModernSessionBoundary.Negotiated peer) {
        if(nativeActorIdentity==null||!ModernNativeActorWorldProtocol.negotiated(peer))throw new SecurityException("Native actor dependency route unavailable");
        var context=nativeContext(owner,request.catalogRequest().nativeRequest().world().scope());
        if(context.source()!=null) {
            var scope=request.catalogRequest().nativeRequest().world().scope();
            var snapshot=context.entry()==null?rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view().joined():context.entry().ticket.snapshot();
            var member=snapshot.members().stream().filter(m->m.identity().equals(ModernParticipantId.human(scope.sourceId(),scope.objectId()))).findFirst().orElseThrow(()->new SecurityException("Native actor selected member absent"));
            if(member.characterClass()!=0||member.sex()!=0)throw new SecurityException("Native actor dependency profile class/sex unsupported");
            ModernNativeActorWorldProtocol.authorize(nativeActorIdentity,context.source(),request,peer);
        }
        else if(!ModernNativeActorWorldProtocol.ZERO.equals(request.expectedIdentity()))throw new SecurityException("Native actor dependency source unavailable");
        var actual=nativeCatalogWorldInternal(owner,request.catalogRequest(),peer);
        return new ModernNativeActorWorldProtocol.Result(request,actual.catalog()==null?null:nativeActorIdentity,actual);
    }

    ModernWorldService(ModernRooms rooms, Map<ModernWorldBinding.Key, Mapping> mappings, Limits limits,
                       ModernPhysicalCombat.BoundedRandom random) {
        this(rooms, mappings, limits, random, null);
    }
    /** Persistence is an explicit, separately owned writer, never a readonly character source. */
    ModernWorldService(ModernRooms rooms, Map<ModernWorldBinding.Key, Mapping> mappings, Limits limits,
                       ModernPhysicalCombat.BoundedRandom random, ModernMatchSettlement.Store settlementStore) {
        this(rooms,mappings,limits,random,settlementStore,Map.of());
    }
    ModernWorldService(ModernRooms rooms,Map<ModernWorldBinding.Key,Mapping> mappings,Limits limits,
                       ModernPhysicalCombat.BoundedRandom random,ModernMatchSettlement.Store settlementStore,Map<Integer,Double> weightRates) {
        this(rooms,mappings,limits,random,settlementStore,weightRates,null);
    }
    ModernWorldService(ModernRooms rooms,Map<ModernWorldBinding.Key,Mapping> mappings,Limits limits,
                       ModernPhysicalCombat.BoundedRandom random,ModernMatchSettlement.Store settlementStore,Map<Integer,Double> weightRates,
                       ModernPetRaceSettlement.JournaledStore raceJournal) {
        this.rooms = Objects.requireNonNull(rooms); this.limits = Objects.requireNonNull(limits);
        this.random = Objects.requireNonNull(random);
        this.settlementStore = settlementStore;
        this.raceJournal=raceJournal;
        journal = settlementStore instanceof ModernMatchSettlement.JournaledStore durable ? durable : null;
        if (mappings == null || mappings.isEmpty() || mappings.size() > 40) throw invalid();
        this.mappings = Map.copyOf(mappings);
        boolean questResults=this.mappings.values().stream().anyMatch(ModernWorldService::questObjectives);
        if((questResults||this.mappings.values().stream().anyMatch(m->m.timeAttack()!=null||m.siege()!=null))&&journal==null)throw new IllegalArgumentException("siege/cooperative/Quest objectives require durable result journal");
        if(this.mappings.values().stream().anyMatch(m->m.race()!=null)!=(raceJournal!=null))throw invalid();
        this.weightRates=Map.copyOf(weightRates);
        this.weightRates.forEach((source,rate)->{if(source<1||!Double.isFinite(rate)||rate<=0)throw invalid();});
        long weighted=this.mappings.values().stream().filter(Mapping::weightEnabled).count();
        if((weighted==0)!=this.weightRates.isEmpty())throw invalid();
        this.mappings.forEach((key, value) -> { if (!key.equals(value.binding().key())) throw invalid(); });
        this.mappings.values().forEach(mapping->ModernMatchDescriptor.of(mapping,limits,settlementStore!=null));
        rooms.configureLargeWorlds(this.mappings.entrySet().stream()
                .filter(e->e.getValue().binding().map().manifest().sectors().size()>ModernLargeMaps.LEGACY_SECTORS)
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet()));
        // Immutable startup geometry proof, before any worker/listener is
        // created. PREPARE and each match must not repeat 65K-cell flood fills
        // on a latency-bounded TLS request or consume its preparation budget.
        var qualifiedSieges=new java.util.HashMap<ModernWorldBinding.Key,ModernSiege.Space>();
        this.mappings.forEach((key,m)->{if(m.siege()!=null)qualifiedSieges.put(key,m.siege().qualify(m.binding(),()->false));});
        siegeSpaces=Map.copyOf(qualifiedSieges);
        long combatCount = this.mappings.values().stream().filter(Mapping::combatEnabled).count();
        if(settlementStore!=null&&this.mappings.values().stream().noneMatch(m->m.combatEnabled()&&(m.binding().key().mode()==1||m.honor()!=null||questObjectives(m)||m.timeAttack()!=null||m.siege()!=null)))throw invalid();
        if(this.mappings.values().stream().anyMatch(m->m.honor()!=null)&&settlementStore==null)throw invalid();
        settlementWorkers = settlementStore == null || journal != null ? null : new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(limits.maxMatches()), action -> new Thread(action, "modern-match-settlement"),
                new ThreadPoolExecutor.AbortPolicy());
        preparationAdmission = new Semaphore(limits.preparationWorkers() + limits.preparationQueue());
        var ids = new AtomicInteger();
        // The admission semaphore is the strict workers+queue limit. A single
        // physical handoff slot also covers a worker between afterExecute and
        // taking its next task when the configured *logical* queue is zero.
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(Math.max(1, limits.preparationQueue()));
        workers = new ThreadPoolExecutor(limits.preparationWorkers(), limits.preparationWorkers(), 0,
                TimeUnit.MILLISECONDS, queue, action -> new Thread(action, "modern-world-prepare-" + ids.incrementAndGet()),
                new ThreadPoolExecutor.AbortPolicy()) {
            @Override protected void afterExecute(Runnable action, Throwable failure) {
                try {
                    super.afterExecute(action, failure);
                    if (failure != null) terminate(((PreparationJob) action).entry, AbortReason.PREPARATION_FAILED);
                } finally { finishWorker(((PreparationJob) action).entry); }
            }
        };
        scheduler = new ScheduledThreadPoolExecutor(1, action -> new Thread(action, "modern-world-tick"));
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.scheduleAtFixedRate(this::scheduledTick, limits.tickMillis(), limits.tickMillis(), TimeUnit.MILLISECONDS);
    }

    /** Caller already owns the exact selected character and actual lobby membership. */
    void register(TournamentPresence.Owner owner, ModernRooms.Scope scope, FreshReader reader) {
        register(owner, scope, reader, null);
    }
    void register(TournamentPresence.Owner owner, ModernRooms.Scope scope, FreshReader reader, PhysicalReader physical) {
        register(owner, scope, reader, physical, false);
    }
    void register(TournamentPresence.Owner owner, ModernRooms.Scope scope, FreshReader reader, PhysicalReader physical, boolean resultProtocol) {
        register(owner, scope, reader, physical, resultProtocol, null);
    }
    void register(TournamentPresence.Owner owner, ModernRooms.Scope scope, FreshReader reader, PhysicalReader physical, boolean resultProtocol, SkillsReader skills) {
        register(owner, scope, reader, physical, resultProtocol, skills, false);
    }
    void register(TournamentPresence.Owner owner, ModernRooms.Scope scope, FreshReader reader, PhysicalReader physical, boolean resultProtocol, SkillsReader skills, boolean damageMagicProtocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,null);
    }
    void register(TournamentPresence.Owner owner, ModernRooms.Scope scope, FreshReader reader, PhysicalReader physical, boolean resultProtocol, SkillsReader skills, boolean damageMagicProtocol, MatchItems matchItems) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,false);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,false);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,false);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,false);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,false);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,false);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,null);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,null);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol,DisintegrateReader disintegrate) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,disintegrate,null);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol,DisintegrateReader disintegrate,GreaterHasteReader greaterHaste) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,disintegrate,greaterHaste,null);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol,DisintegrateReader disintegrate,GreaterHasteReader greaterHaste,HasteReader hasteReader) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,disintegrate,greaterHaste,hasteReader,null);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol,DisintegrateReader disintegrate,GreaterHasteReader greaterHaste,HasteReader hasteReader,SlowReader slowReader) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,disintegrate,greaterHaste,hasteReader,slowReader,false);
    }
    /** Trusted source opt-in is independent of Honor and of the peer's capability.
     * The fresh fighter still has to prove complete healing arithmetic at preparation. */
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol,DisintegrateReader disintegrate,GreaterHasteReader greaterHaste,HasteReader hasteReader,SlowReader slowReader,boolean playerHealingSource) {
        register(owner,scope,reader,physical,resultProtocol,skills,damageMagicProtocol,matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,disintegrate,greaterHaste,hasteReader,slowReader,playerHealingSource,false);
    }
    void register(TournamentPresence.Owner owner,ModernRooms.Scope scope,FreshReader reader,PhysicalReader physical,boolean resultProtocol,SkillsReader skills,
                  boolean damageMagicProtocol,MatchItems matchItems,boolean weightProtocol,boolean timedProtocol,boolean bowProtocol,boolean regenProtocol,boolean honorProtocol,boolean fireStormProtocol,ModernSessionBoundary.Negotiated protocol,DisintegrateReader disintegrate,GreaterHasteReader greaterHaste,HasteReader hasteReader,SlowReader slowReader,boolean playerHealingSource,boolean armorProtectionSource) {
        Objects.requireNonNull(owner); Objects.requireNonNull(scope); Objects.requireNonNull(reader);
        synchronized (gate) {
            requireOpen();
            Registration old = registrations.get(owner);
            if (old != null && (old.reserving || old.entry != null)) throw new IllegalStateException("world registration is in use");
            if (old == null && registrations.size() >= MAX_REGISTRATIONS) throw new IllegalStateException("world registrations exhausted");
            for (Registration row : registrations.values()) {
                if (row.owner != owner && row.scope.sourceId() == scope.sourceId() && row.scope.objectId() == scope.objectId())
                    throw new SecurityException("world character already registered");
            }
            if(weightProtocol&&(!weightEnabled()||matchItems==null||physical==null))throw invalid();
            if(timedProtocol&&(!weightProtocol || !timedEnabled()))throw invalid();
            if(bowProtocol&&(!timedProtocol||!bowEnabled()))throw invalid();
            if(regenProtocol&&(!weightProtocol||!regenEnabled()||physical==null))throw invalid();
            if(honorProtocol&&(!regenProtocol||!honorEnabled()))throw invalid();
            if(fireStormProtocol&&(!regenProtocol||!damageMagicProtocol||!fireStormEnabled()))throw invalid();
            if(ModernNpcFireStormProtocol.negotiated(protocol)&&(!regenProtocol||skills==null||physical==null||!npcFireStormEnabled()))throw invalid();
            if(protocol!=null&&(protocol.capabilities()&ModernSessionProtocol.CAP_NPC_FIRE_STORM)!=0&&!ModernNpcFireStormProtocol.negotiated(protocol))throw invalid();
            boolean pc77=ModernDisintegrateProtocol.negotiated(protocol),npc77=ModernNpcDisintegrateProtocol.negotiated(protocol);
            if(disintegrate!=null&&(!(pc77||npc77)||!regenProtocol||skills==null||pc77&&(!damageMagicProtocol||!disintegrateEnabled())||npc77&&!npcDisintegrateEnabled()))throw invalid();
            if((pc77||npc77)&&disintegrate==null)throw invalid();
            boolean haste=ModernGreaterHasteProtocol.negotiated(protocol);
            if((greaterHaste!=null)!=haste||haste&&(!greaterHasteEnabled()||!timedProtocol||skills==null||physical==null))throw invalid();
            if(protocol!=null&&(protocol.capabilities()&ModernSessionProtocol.CAP_GREATER_HASTE)!=0&&!haste)throw invalid();
            boolean haste43=ModernHasteProtocol.negotiated(protocol);
            if((hasteReader!=null)!=haste43||haste43&&(!hasteEnabled()||!timedProtocol||skills==null||physical==null))throw invalid();
            if(protocol!=null&&(protocol.capabilities()&ModernSessionProtocol.CAP_HASTE)!=0&&!haste43)throw invalid();
            boolean slow29=ModernBattleSpeedWire.negotiated(protocol);
            if((slowReader!=null)!=slow29||slow29&&(!slowEnabled()||!timedProtocol||skills==null||physical==null||matchItems==null))throw invalid();
            if(protocol!=null&&(protocol.capabilities()&ModernSessionProtocol.CAP_SLOW_DATA)!=0&&!slow29)throw invalid();
            if(playerHealingSource!=ModernSessionExtensions.allowsData(protocol)
                    ||playerHealingSource&&(!playerHealingEnabled()||!weightProtocol||skills==null||physical==null||matchItems==null))throw invalid();
            if(armorProtectionSource!=ModernSessionExtensions.allowsArmor(protocol)
                    ||armorProtectionSource&&(!armorProtectionEnabled()||!weightProtocol||physical==null||matchItems==null))throw invalid();
            if(old!=null)old.nativeLedger.retire();
            registrations.put(owner, new Registration(owner, scope, reader, physical, resultProtocol, skills, damageMagicProtocol, matchItems,weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,disintegrate,greaterHaste,hasteReader,slowReader,playerHealingSource,armorProtectionSource));
        }
    }

    boolean combatEnabled() { return mappings.values().stream().anyMatch(Mapping::combatEnabled); }
    boolean spellsEnabled() { return mappings.values().stream().anyMatch(Mapping::spellsEnabled); }
    boolean itemsEnabled() { return mappings.values().stream().anyMatch(Mapping::matchItems); }
    boolean weightEnabled() { return mappings.values().stream().anyMatch(Mapping::weightEnabled); }
    boolean timedEnabled() { return mappings.values().stream().anyMatch(Mapping::timedEnabled); }
    boolean bowEnabled(){return mappings.values().stream().anyMatch(Mapping::bowEnabled);}
    boolean regenEnabled(){return mappings.values().stream().anyMatch(Mapping::regenEnabled);}
    boolean honorEnabled(){return mappings.values().stream().anyMatch(m->m.honor()!=null);}
    boolean fireStormEnabled(){return mappings.values().stream().anyMatch(Mapping::fireStormEnabled);}
    boolean disintegrateEnabled(){return mappings.values().stream().anyMatch(Mapping::disintegrateEnabled);}
    boolean greaterHasteEnabled(){return mappings.values().stream().anyMatch(Mapping::greaterHasteEnabled);}
    boolean hasteEnabled(){return mappings.values().stream().anyMatch(Mapping::hasteEnabled);}
    boolean slowEnabled(){return mappings.values().stream().anyMatch(Mapping::slowEnabled);}
    boolean playerHealingEnabled(){return mappings.values().stream().anyMatch(Mapping::playerHealingEnabled);}
    boolean armorProtectionEnabled(){return mappings.values().stream().anyMatch(Mapping::armorProtectionEnabled);}
    boolean fullVipEnabled(){return mappings.values().stream().anyMatch(Mapping::fullVipEnabled);}
    boolean npcFireStormEnabled(){return mappings.values().stream().anyMatch(Mapping::npcFireStormEnabled);}
    boolean npcDisintegrateEnabled(){return mappings.values().stream().anyMatch(Mapping::npcDisintegrateEnabled);}
    boolean raceEnabled(){return mappings.values().stream().anyMatch(m->m.race()!=null);}
    boolean largeMapsEnabled(){return mappings.values().stream().anyMatch(m->m.binding().map().manifest().sectors().size()>ModernLargeMaps.LEGACY_SECTORS);}
    boolean npcEnabled(){return mappings.values().stream().anyMatch(m->m.npc()!=null);}
    ModernSiegeProtocol.Reply siege(TournamentPresence.Owner owner,ModernSiegeProtocol.Request request) {
        return siege(owner,request,null);
    }
    private ModernSiegeProtocol.Reply siege(TournamentPresence.Owner owner,ModernSiegeProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        Registration registration;Entry entry;
        synchronized(gate){requireOpen();registration=requireOrdinaryRegistration(owner,request.player());entry=registration.entry;
            if(entry==null||entry.mapping.siege()==null||registration.physical==null||registration.protocol==null
                    ||!entry.descriptor.supportedBy(registration.protocol)
                    ||!request.world().equals(new ModernSiege.Scope(entry.ticket.snapshot().summary().id(),entry.ticket.matchId(),entry.ticket.generation())))
                throw new SecurityException("siege scope or negotiation mismatch");}
        advance(entry);
        var dispatch=published(entry,()->{requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("siege attachment changed");
            if(entry.combat()==null)return new SiegeDispatch(null,new ModernSiegeProtocol.Reply(ModernCombatProtocol.Status.UNAVAILABLE,ModernSiegeProtocol.Reason.NOT_ACTIVE,null));
            if(entry.terminal||entry.engine==null)return new SiegeDispatch(null,new ModernSiegeProtocol.Reply(ModernCombatProtocol.Status.DENIED,ModernSiegeProtocol.Reason.NOT_ACTIVE,entry.combat()));
            return new SiegeDispatch(entry.engine,null);});
        if(dispatch.reply()!=null)return dispatch.reply();var engine=dispatch.engine();ModernBattleEngine.Snapshot snapshot;ModernSiegeProtocol.Reason reason;ModernBattleObservation.Capture observation;
        if(request.action()==ModernSiegeProtocol.Action.ATTACK_TOWER){var observed=observeAction(entry,engine,owner,guard,()->engine.attackSiegeTower(owner,request.player(),request.world().matchId(),request.world().generation(),request.sequence(),new ModernSiege.Target(request.world(),request.tower())));var result=observed.outcome();observation=observed.observation();
            snapshot=result.snapshot();reason=result.status()==ModernBattleEngine.AttackStatus.ATTACKED?ModernSiegeProtocol.Reason.NONE:ModernSiegeProtocol.Reason.valueOf(result.status().name());}
        else {var observed=observeAction(entry,engine,owner,guard,()->engine.useSiegePortal(owner,request.player(),request.world().matchId(),request.world().generation(),request.sequence()));var result=observed.outcome();observation=observed.observation();snapshot=result.snapshot();
            reason=result.reason()==ModernSiege.PortalReason.MOVED?ModernSiegeProtocol.Reason.NONE:ModernSiegeProtocol.Reason.valueOf(result.reason().name());}
        publish(entry,engine,observation);
        var exact=fromCombat(snapshot,fromEngine(snapshot,Math.incrementExact(snapshot.revision()),entry));
        return published(entry,()->{requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("siege attachment changed");
            return new ModernSiegeProtocol.Reply(reason==ModernSiegeProtocol.Reason.NONE?ModernCombatProtocol.Status.OK:ModernCombatProtocol.Status.DENIED,reason,exact);});
    }
    private record SiegeDispatch(ModernBattleEngine engine,ModernSiegeProtocol.Reply reply){}
    double weightRate(int sourceId) { var value=weightRates.get(sourceId);if(value==null)throw new SecurityException("source weight rate unavailable");return value; }

    ModernSpellProtocol.Reply cast(TournamentPresence.Owner owner, ModernSpellProtocol.Request request) {
        return cast(owner,request,null);
    }
    private ModernSpellProtocol.Reply cast(TournamentPresence.Owner owner,ModernSpellProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        var result=castInternal(owner,request,guard);return new ModernSpellProtocol.Reply(result.status(),result.reason(),ModernQuestVisibility.combat(result.frame(),request.scope()));
    }
    private ModernSpellProtocol.Reply castInternal(TournamentPresence.Owner owner,ModernSpellProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);
        Registration registration; Entry entry;
        synchronized (gate) {
            requireOpen(); registration = requireOrdinaryRegistration(owner, request.scope()); entry = registration.entry;
            if (registration.skills == null || registration.physical == null) throw new SecurityException("spells were not negotiated");
            if(request.skillId()==70&&!registration.fireStormProtocol)throw new SecurityException("fire storm was not negotiated");
            if (entry == null || request.matchId() != entry.ticket.matchId() || request.generation() != entry.ticket.generation())
                throw new SecurityException("spell match generation mismatch");
        }
        advance(entry);
        var dispatch = published(entry, () -> {
            requireActionCurrent(registration);
            if (registration.entry != entry) throw new SecurityException("spell attachment changed");
            if (!entry.mapping.spellsEnabled() || entry.combat() == null)
                return new CastDispatch(null, new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.UNAVAILABLE, ModernSpellProtocol.Reason.NOT_ACTIVE, null));
            if (entry.terminal || entry.engine == null)
                return new CastDispatch(null, new ModernSpellProtocol.Reply(ModernSpellProtocol.Status.DENIED, ModernSpellProtocol.Reason.NOT_ACTIVE, entry.combat()));
            return new CastDispatch(entry.engine, null);
        });
        if (dispatch.reply() != null) return dispatch.reply();
        var engine = dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->engine.cast(owner,request));var result=observed.outcome();
        publish(entry, engine, observed.observation());
        var snapshot=result.snapshot();
        var exact = fromCombat(snapshot, fromEngine(snapshot, Math.addExact(snapshot.revision(), 1),entry));
        return published(entry, () -> {
            requireActionCurrent(registration);
            if (registration.entry != entry) throw new SecurityException("spell attachment changed");
            return new ModernSpellProtocol.Reply(result.reason() == ModernSpellProtocol.Reason.NONE
                    ? ModernSpellProtocol.Status.OK : ModernSpellProtocol.Status.DENIED, result.reason(), exact);
        });
    }
    private record CastDispatch(ModernBattleEngine engine, ModernSpellProtocol.Reply reply) {}

    ModernDisintegrateProtocol.Reply disintegrate(TournamentPresence.Owner owner,ModernDisintegrateState.Request request) {
        return disintegrate(owner,request,null);
    }
    private ModernDisintegrateProtocol.Reply disintegrate(TournamentPresence.Owner owner,ModernDisintegrateState.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);Registration registration;Entry entry;
        synchronized(gate){
            requireOpen();registration=requireOrdinaryRegistration(owner,request.scope());entry=registration.entry;
            if(registration.disintegrate==null||!ModernDisintegrateProtocol.negotiated(registration.protocol))throw new SecurityException("Disintegrate was not negotiated");
            if(entry==null||request.matchId()!=entry.ticket.matchId()||request.generation()!=entry.ticket.generation())throw new SecurityException("Disintegrate match generation mismatch");
        }
        advance(entry);
        var dispatch=published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("Disintegrate attachment changed");
            if(!entry.mapping.disintegrateEnabled()||entry.combat()==null)return new DisintegrateDispatch(null,new ModernDisintegrateProtocol.Reply(ModernSpellProtocol.Status.UNAVAILABLE,ModernDisintegrateState.Reason.NOT_ACTIVE,null));
            if(entry.terminal||entry.engine==null)return new DisintegrateDispatch(null,new ModernDisintegrateProtocol.Reply(ModernSpellProtocol.Status.DENIED,ModernDisintegrateState.Reason.NOT_ACTIVE,entry.combat()));
            return new DisintegrateDispatch(entry.engine,null);
        });
        if(dispatch.reply()!=null)return dispatch.reply();
        var engine=dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->engine.disintegrate(owner,request));var result=observed.outcome();publish(entry,engine,observed.observation());
        var snapshot=result.snapshot();
        var exact=fromCombat(snapshot,fromEngine(snapshot,Math.incrementExact(snapshot.revision()),entry));
        return published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("Disintegrate attachment changed");
            return new ModernDisintegrateProtocol.Reply(result.reason()==ModernDisintegrateState.Reason.NONE?ModernSpellProtocol.Status.OK:ModernSpellProtocol.Status.DENIED,result.reason(),exact);
        });
    }
    private record DisintegrateDispatch(ModernBattleEngine engine,ModernDisintegrateProtocol.Reply reply){}

    ModernItemProtocol.Reply item(TournamentPresence.Owner owner,ModernItemProtocol.Request request) {
        return item(owner,request,null);
    }
    private ModernItemProtocol.Reply item(TournamentPresence.Owner owner,ModernItemProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        var result=itemInternal(owner,request,guard);return new ModernItemProtocol.Reply(result.status(),result.reason(),ModernQuestVisibility.combat(result.frame(),request.scope()));
    }
    private ModernItemProtocol.Reply itemInternal(TournamentPresence.Owner owner,ModernItemProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);
        Registration registration;Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,request.scope());entry=registration.entry;
            if(registration.matchItems==null || registration.physical==null)throw new SecurityException("match items were not negotiated");
            if(entry==null || request.matchId()!=entry.ticket.matchId() || request.generation()!=entry.ticket.generation())
                throw new SecurityException("match item generation mismatch");
        }
        advance(entry);
        var dispatch=published(entry,()-> {
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("match item attachment changed");
            if(!entry.mapping.matchItems() || entry.combat()==null)
                return new ItemDispatch(null,new ModernItemProtocol.Reply(ModernItemProtocol.Status.UNAVAILABLE,ModernItemProtocol.Reason.NOT_ACTIVE,null));
            if(request.action()==ModernItemProtocol.Action.READ)
                return new ItemDispatch(null,new ModernItemProtocol.Reply(ModernItemProtocol.Status.OK,ModernItemProtocol.Reason.NONE,entry.combat()));
            if(entry.terminal || entry.engine==null)
                return new ItemDispatch(null,new ModernItemProtocol.Reply(ModernItemProtocol.Status.DENIED,ModernItemProtocol.Reason.NOT_ACTIVE,entry.combat()));
            return new ItemDispatch(entry.engine,null);
        });
        if(dispatch.reply()!=null)return dispatch.reply();
        var engine=dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->engine.item(owner,request));var result=observed.outcome();
        publish(entry,engine,observed.observation());
        var snapshot=result.snapshot();
        var exact=fromCombat(snapshot,fromEngine(snapshot,Math.addExact(snapshot.revision(),1),entry));
        return published(entry,()-> {
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("match item attachment changed");
            return new ModernItemProtocol.Reply(result.reason()==ModernItemProtocol.Reason.NONE?ModernItemProtocol.Status.OK:ModernItemProtocol.Status.DENIED,result.reason(),exact);
        });
    }
    private record ItemDispatch(ModernBattleEngine engine,ModernItemProtocol.Reply reply) {}
    ModernAmmoProtocol.Reply ammo(TournamentPresence.Owner owner,ModernAmmoProtocol.Request request) {
        return ammo(owner,request,null);
    }
    private ModernAmmoProtocol.Reply ammo(TournamentPresence.Owner owner,ModernAmmoProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        var result=ammoInternal(owner,request,guard);return new ModernAmmoProtocol.Reply(result.status(),result.reason(),ModernQuestVisibility.combat(result.frame(),request.scope()));
    }
    private ModernAmmoProtocol.Reply ammoInternal(TournamentPresence.Owner owner,ModernAmmoProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);Registration registration;Entry entry;
        synchronized(gate){requireOpen();registration=requireOrdinaryRegistration(owner,request.scope());entry=registration.entry;
            if(!registration.bowProtocol)throw new SecurityException("arrows not negotiated");
            if(entry==null||request.matchId()!=entry.ticket.matchId()||request.generation()!=entry.ticket.generation())throw new SecurityException("arrow generation mismatch");}
        advance(entry);
        var dispatch=published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("arrow attachment changed");
            if(!entry.mapping.bowEnabled()||entry.combat()==null)return new AmmoDispatch(null,new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.UNAVAILABLE,ModernAmmoProtocol.Reason.NOT_ACTIVE,null));
            if(request.action()==ModernAmmoProtocol.Action.READ)return new AmmoDispatch(null,new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.OK,ModernAmmoProtocol.Reason.NONE,entry.combat()));
            if(entry.terminal||entry.engine==null)return new AmmoDispatch(null,new ModernAmmoProtocol.Reply(ModernAmmoProtocol.Status.DENIED,ModernAmmoProtocol.Reason.NOT_ACTIVE,entry.combat()));
            return new AmmoDispatch(entry.engine,null);
        });
        if(dispatch.reply()!=null)return dispatch.reply();var engine=dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->engine.ammo(owner,request));var result=observed.outcome();publish(entry,engine,observed.observation());
        var snapshot=result.snapshot();
        var exact=fromCombat(snapshot,fromEngine(snapshot,Math.incrementExact(snapshot.revision()),entry));
        return published(entry,()->{requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("arrow attachment changed");
            return new ModernAmmoProtocol.Reply(result.reason()==ModernAmmoProtocol.Reason.NONE?ModernAmmoProtocol.Status.OK:ModernAmmoProtocol.Status.DENIED,result.reason(),exact);});
    }
    private record AmmoDispatch(ModernBattleEngine engine,ModernAmmoProtocol.Reply reply){}

    ModernNpcProtocol.Reply npc(TournamentPresence.Owner owner,ModernNpcProtocol.Request request) {
        return npc(owner,request,null);
    }
    private ModernNpcProtocol.Reply npc(TournamentPresence.Owner owner,ModernNpcProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        var result=npcInternal(owner,request,guard);return new ModernNpcProtocol.Reply(result.status(),result.reason(),ModernQuestVisibility.npc(result.frame(),request.player()));
    }
    ModernNpcSpellProtocol.Reply castNpc(TournamentPresence.Owner owner,ModernNpcSpellProtocol.Request request) {
        return castNpc(owner,request,null);
    }
    private ModernNpcSpellProtocol.Reply castNpc(TournamentPresence.Owner owner,ModernNpcSpellProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);
        Registration registration;Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,request.player());entry=registration.entry;
            if(entry==null||entry.mapping.npc()==null||!entry.mapping.npc().lightArrow()||registration.skills==null
                    ||registration.physical==null||!npcNegotiated(registration.protocol)||registration.protocol.version()<24
                    ||!entry.descriptor.has(ModernMatchDescriptor.NPC_LIGHT_ARROW)
                    ||request.world().roomId()!=entry.ticket.snapshot().summary().id()||request.world().matchId()!=entry.ticket.matchId()
                    ||request.world().generation()!=entry.ticket.generation())throw new SecurityException("NPC spell owner/match capability mismatch");
        }
        advance(entry);
        var dispatch=published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC spell attachment changed");
            if(entry.npc()==null)return new NpcSpellDispatch(null,new ModernNpcSpellProtocol.Reply(ModernNpcProtocol.Status.UNAVAILABLE,ModernNpcMagic.Reason.NOT_ACTIVE,null));
            if(entry.terminal||entry.engine==null)return new NpcSpellDispatch(null,new ModernNpcSpellProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcMagic.Reason.NOT_ACTIVE,entry.npc()));
            return new NpcSpellDispatch(entry.engine,null);
        });
        if(dispatch.reply()!=null){var r=dispatch.reply();return new ModernNpcSpellProtocol.Reply(r.status(),r.reason(),ModernQuestVisibility.npc(r.frame(),request.player()));}
        var engine=dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->engine.castNpc(owner,request.player(),request.world().matchId(),request.world().generation(),
                request.sequence(),request.casterLife(),request.target(),request.targetLife()));var result=observed.outcome();
        publish(entry,engine,observed.observation());var exact=npcFrame(entry,result.frame());
        return published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC spell attachment changed");
            return new ModernNpcSpellProtocol.Reply(result.reason()==ModernNpcMagic.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,
                    result.reason(),ModernQuestVisibility.npc(exact,request.player()));
        });
    }
    private record NpcSpellDispatch(ModernBattleEngine engine,ModernNpcSpellProtocol.Reply reply){}
    ModernNpcDisintegrateProtocol.Reply disintegrateNpc(TournamentPresence.Owner owner,ModernNpcDisintegrateState.Request request) {
        return disintegrateNpc(owner,request,null);
    }
    private ModernNpcDisintegrateProtocol.Reply disintegrateNpc(TournamentPresence.Owner owner,ModernNpcDisintegrateState.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);
        Registration registration;Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,request.player());entry=registration.entry;
            if(entry==null||entry.mapping.npc()==null||!entry.mapping.npcDisintegrateEnabled()||registration.disintegrate==null||registration.skills==null
                    ||registration.physical==null||!ModernNpcDisintegrateProtocol.negotiated(registration.protocol)
                    ||!entry.descriptor.has(ModernMatchDescriptor.NPC_DISINTEGRATE)
                    ||request.target().scope().roomId()!=entry.ticket.snapshot().summary().id()||request.target().scope().matchId()!=entry.ticket.matchId()
                    ||request.target().scope().generation()!=entry.ticket.generation())throw new SecurityException("NPC77 owner/match capability mismatch");
        }
        advance(entry);
        var dispatch=published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC77 attachment changed");
            if(entry.npc()==null)return new NpcDisintegrateDispatch(null,new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.UNAVAILABLE,ModernNpcDisintegrateState.Reason.NOT_ACTIVE,null));
            if(entry.terminal||entry.engine==null)return new NpcDisintegrateDispatch(null,new ModernNpcDisintegrateProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcDisintegrateState.Reason.NOT_ACTIVE,entry.npc()));
            return new NpcDisintegrateDispatch(entry.engine,null);
        });
        if(dispatch.reply()!=null){var r=dispatch.reply();return new ModernNpcDisintegrateProtocol.Reply(r.status(),r.reason(),ModernQuestVisibility.npc(r.frame(),request.player()));}
        var engine=dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->engine.disintegrateNpc(owner,request));var result=observed.outcome();
        publish(entry,engine,observed.observation());var exact=npcFrame(entry,result.frame());
        return published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC77 attachment changed");
            return new ModernNpcDisintegrateProtocol.Reply(result.reason()==ModernNpcDisintegrateState.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,
                    result.reason(),ModernQuestVisibility.npc(exact,request.player()));
        });
    }
    private record NpcDisintegrateDispatch(ModernBattleEngine engine,ModernNpcDisintegrateProtocol.Reply reply){}
    ModernNpcFireStormProtocol.Reply fireStormNpc(TournamentPresence.Owner owner,ModernNpcFireStormState.Request request) {
        return fireStormNpc(owner,request,null);
    }
    private ModernNpcFireStormProtocol.Reply fireStormNpc(TournamentPresence.Owner owner,ModernNpcFireStormState.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);
        Registration registration;Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,request.player());entry=registration.entry;
            if(entry==null||entry.mapping.npc()==null||!entry.mapping.npcFireStormEnabled()||registration.skills==null
                    ||registration.physical==null||!ModernNpcFireStormProtocol.negotiated(registration.protocol)
                    ||!entry.descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM)
                    ||request.world().roomId()!=entry.ticket.snapshot().summary().id()||request.world().matchId()!=entry.ticket.matchId()
                    ||request.world().generation()!=entry.ticket.generation())throw new SecurityException("NPC70 owner/match capability mismatch");
        }
        advance(entry);
        var dispatch=published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC70 attachment changed");
            if(entry.npc()==null)return new NpcFireStormDispatch(null,new ModernNpcFireStormProtocol.Reply(ModernNpcProtocol.Status.UNAVAILABLE,ModernNpcFireStormState.Reason.NOT_ACTIVE,null));
            if(entry.terminal||entry.engine==null)return new NpcFireStormDispatch(null,new ModernNpcFireStormProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcFireStormState.Reason.NOT_ACTIVE,entry.npc()));
            return new NpcFireStormDispatch(entry.engine,null);
        });
        if(dispatch.reply()!=null){var r=dispatch.reply();return new ModernNpcFireStormProtocol.Reply(r.status(),r.reason(),ModernQuestVisibility.npc(r.frame(),request.player()));}
        var engine=dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->engine.fireStormNpc(owner,request));var result=observed.outcome();
        publish(entry,engine,observed.observation());var exact=npcFrame(entry,result.frame());
        return published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC70 attachment changed");
            return new ModernNpcFireStormProtocol.Reply(result.reason()==ModernNpcFireStormState.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,
                    result.reason(),ModernQuestVisibility.npc(exact,request.player()));
        });
    }
    private record NpcFireStormDispatch(ModernBattleEngine engine,ModernNpcFireStormProtocol.Reply reply){}
    private ModernNpcProtocol.Reply npcInternal(TournamentPresence.Owner owner,ModernNpcProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);
        Registration registration;Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,request.player());entry=registration.entry;
            if(entry==null||entry.mapping.npc()==null||registration.physical==null||!npcNegotiated(registration.protocol)
                    ||request.world().roomId()!=entry.ticket.snapshot().summary().id()||request.world().matchId()!=entry.ticket.matchId()
                    ||request.world().generation()!=entry.ticket.generation())throw new SecurityException("NPC owner/match capability mismatch");
            if(request.action()==ModernNpcProtocol.Action.ATTACK_OBJECTIVE&&!entry.descriptor.has(ModernMatchDescriptor.QUEST_OBJECTIVES))throw new SecurityException("Quest objective capability mismatch");
            if(request.action()==ModernNpcProtocol.Action.ATTACK_TOWER&&(!entry.descriptor.has(ModernMatchDescriptor.TIME_ATTACK)
                    ||registration.protocol.version()<25))throw new SecurityException("cooperative tower capability mismatch");
            if(request.action()==ModernNpcProtocol.Action.CONFIRM_DEATH&&(!entry.descriptor.has(ModernMatchDescriptor.QUEST_LIVES)
                    ||registration.protocol.version()<22||entry.frame().actors().stream().noneMatch(a->a.entityId()==request.serial()
                        &&a.member().identity().equals(ModernParticipantId.human(request.player().sourceId(),request.player().objectId())))))throw new SecurityException("Quest participant life capability mismatch");
        }
        advance(entry);
        var dispatch=published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC attachment changed");
            if(entry.npc()==null)return new NpcDispatch(null,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.UNAVAILABLE,ModernNpcProtocol.Reason.NOT_ACTIVE,null));
            if(request.action()==ModernNpcProtocol.Action.READ)return new NpcDispatch(null,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,entry.npc()));
            if(entry.terminal||entry.engine==null)return new NpcDispatch(null,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcProtocol.Reason.NOT_ACTIVE,entry.npc()));
            return new NpcDispatch(entry.engine,null);
        });
        if(dispatch.reply()!=null)return dispatch.reply();
        if(request.action()==ModernNpcProtocol.Action.CONFIRM_DEATH) {
            var observed=dispatch.engine().observeCommand(()->dispatch.engine().confirmQuestDeath(owner,request.player(),request.world().matchId(),request.world().generation(),request.life(),Math.subtractExact(request.sequence(),1)));var result=observed.outcome();
            publish(entry,dispatch.engine(),observed.observation());var exact=npcFrame(entry,result.frame());
            return published(entry,()->{
                requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("Quest life attachment changed");
                return new ModernNpcProtocol.Reply(result.reason()==ModernQuestLife.Reason.REVIVED?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,
                        result.reason()==ModernQuestLife.Reason.REVIVED?ModernNpcProtocol.Reason.NONE:ModernNpcProtocol.Reason.valueOf(result.reason().name()),exact);
            });
        }
        var engine=dispatch.engine();var observed=observeAction(entry,engine,owner,guard,()->request.action()==ModernNpcProtocol.Action.ATTACK_OBJECTIVE
                ?engine.attackQuestObjective(owner,request.player(),request.world().matchId(),request.world().generation(),request.sequence(),request.objective())
                :request.action()==ModernNpcProtocol.Action.ATTACK_TOWER
                ?engine.attackTimeAttackTower(owner,request.player(),request.world().matchId(),request.world().generation(),request.sequence(),request.tower())
                :engine.attackNpc(owner,request.player(),request.world().matchId(),request.world().generation(),request.sequence(),request.target(),request.life()));var result=observed.outcome();
        publish(entry,engine,observed.observation());
        var exact=npcFrame(entry,result.frame());
        return published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("NPC attachment changed");
            return new ModernNpcProtocol.Reply(result.reason()==ModernNpcBattle.Reason.ATTACKED?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,
                    result.reason()==ModernNpcBattle.Reason.ATTACKED?ModernNpcProtocol.Reason.NONE:ModernNpcProtocol.Reason.valueOf(result.reason().name()),entry.terminal?entry.npc():exact);
        });
    }
    private record NpcDispatch(ModernBattleEngine engine,ModernNpcProtocol.Reply reply){}
    private static boolean npcNegotiated(ModernSessionBoundary.Negotiated protocol) {
        return protocol!=null&&protocol.version()>=ModernNpcProtocol.RESOURCE_VERSION
                &&(protocol.capabilities()&(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_COMBAT))
                ==(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_COMBAT);
    }

    ModernCombatProtocol.Reply combat(TournamentPresence.Owner owner, ModernCombatProtocol.Request request) {
        return combat(owner,request,null);
    }
    private ModernCombatProtocol.Reply combat(TournamentPresence.Owner owner,ModernCombatProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        var result=combatInternal(owner,request,guard);return new ModernCombatProtocol.Reply(result.status(),result.reason(),ModernQuestVisibility.combat(result.frame(),request.scope()));
    }
    private ModernCombatProtocol.Reply combatInternal(TournamentPresence.Owner owner,ModernCombatProtocol.Request request,ModernBattleActionProtocol.Request guard) {
        Objects.requireNonNull(request);
        Registration registration; Entry entry;
        synchronized (gate) {
            requireOpen(); registration = requireOrdinaryRegistration(owner, request.scope()); entry = registration.entry;
            if (registration.physical == null) throw new SecurityException("combat was not negotiated");
            if (entry == null || request.matchId() != entry.ticket.matchId() || request.generation() != entry.ticket.generation())
                throw new SecurityException("combat match generation mismatch");
        }
        advance(entry);
        var dispatch = published(entry, () -> {
            requireActionCurrent(registration);
            if (registration.entry != entry) throw new SecurityException("combat attachment changed");
            if (entry.combat() == null) return new CombatDispatch(null, combatReply(ModernCombatProtocol.Status.UNAVAILABLE, ModernCombatProtocol.Reason.NOT_ACTIVE, null));
            if (request.action() == ModernCombatProtocol.Action.READ)
                return new CombatDispatch(null, combatReply(ModernCombatProtocol.Status.OK, ModernCombatProtocol.Reason.NONE, entry.combat()));
            if (entry.terminal || entry.engine == null)
                return new CombatDispatch(null, combatReply(ModernCombatProtocol.Status.DENIED, ModernCombatProtocol.Reason.NOT_ACTIVE, entry.combat()));
            return new CombatDispatch(entry.engine, null);
        });
        if (dispatch.reply() != null) return dispatch.reply();
        var engine = dispatch.engine();
        var observed=observeAction(entry,engine,owner,guard,()->engine.attack(owner, request.scope(), request.matchId(), request.generation(), request.sequence(), request.targetEntityId()));var result=observed.outcome();
        publish(entry, engine, observed.observation());
        var snapshot=result.snapshot();
        var exactFrame = fromCombat(snapshot, fromEngine(snapshot, Math.addExact(snapshot.revision(), 1),entry));
        return published(entry, () -> {
            requireActionCurrent(registration);
            if (registration.entry != entry) throw new SecurityException("combat attachment changed");
            return result.status() == ModernBattleEngine.AttackStatus.ATTACKED
                    ? combatReply(ModernCombatProtocol.Status.OK, ModernCombatProtocol.Reason.NONE, exactFrame)
                    : combatReply(ModernCombatProtocol.Status.DENIED, ModernCombatProtocol.Reason.valueOf(result.status().name()), exactFrame);
        });
    }
    private record CombatDispatch(ModernBattleEngine engine, ModernCombatProtocol.Reply reply) {}
    private static ModernCombatProtocol.Reply combatReply(ModernCombatProtocol.Status status, ModernCombatProtocol.Reason reason,
                                                         ModernCombatProtocol.Frame frame) {
        return new ModernCombatProtocol.Reply(status, reason, frame);
    }

    /** One authenticated domain path for life-bound actions. v37 is only its
     * legacy output adapter; canonical execution never constructs legacy DTOs. */
    ModernApplicationProtocol.BattleActionResult action(TournamentPresence.Owner owner,
            ModernApplicationProtocol.BattleActionRequest message) {
        synchronized(gate) {
            var row=requireOrdinaryRegistration(owner,message.request().proof().player());
            if(row.entry!=null&&nativeCanonicalSources.containsKey(row.entry.mapping.binding().key()))
                throw new SecurityException("Native binding rejects the legacy action result adapter");
        }
        var execution=executeAction(owner,message);
        var receipt=execution.receipt();
        var result=receipt.rejection()!=ModernBattleCommandLife.Rejection.NONE
                ?recoverAction(execution.registration(),execution.entry(),message.requestId(),message.request(),execution.output())
                :legacyAction(message,execution);
        return new ModernApplicationProtocol.BattleActionResult(message.requestId(),message.request(),receipt.rejection(),result);
    }
    /** Internal service seam, not a new network opcode or SET authorization. */
    ModernBattleActionReceipt canonicalAction(TournamentPresence.Owner owner,
            ModernApplicationProtocol.BattleActionRequest message) {
        return executeAction(owner,message).receipt();
    }
    ModernApplicationProtocol.BattleOperationResult operation(TournamentPresence.Owner owner,
            ModernApplicationProtocol.BattleOperationRequest message) {
        synchronized(gate) {
            requireOpen();var registration=requireOrdinaryRegistration(owner,message.request().proof().player());
            if(!ModernBattleOperationProtocol.negotiated(registration.protocol))throw new SecurityException("canonical operations not negotiated");
        }
        var receipt=canonicalAction(owner,new ModernApplicationProtocol.BattleActionRequest(message.requestId(),message.request()));
        return new ModernApplicationProtocol.BattleOperationResult(message.requestId(),message.request(),receipt);
    }
    /** One owner-scoped, exact capture; no legacy projection or second read. */
    ModernGreaterHasteProtocol.Reply greaterHaste(TournamentPresence.Owner owner,ModernGreaterHasteProtocol.Request q) {
        var proof=q.proof();final Registration registration;final Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,proof.player());entry=registration.entry;
            if(!ModernGreaterHasteProtocol.negotiated(registration.protocol)||registration.greaterHaste==null||entry==null
                    ||!entry.descriptor.has(ModernMatchDescriptor.GREATER_HASTE)
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation())
                throw new SecurityException("greater haste attachment mismatch");
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry||entry.publication==null)throw new SecurityException("greater haste attachment unavailable");
            var actor=entry.publication.audience(proof.player()).world().actors().stream()
                    .filter(a->a.pose().entityId()==proof.entity()).findFirst().orElseThrow(SecurityException::new);
            if(!actor.pose().member().identity().equals(ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))
                    ||proof.revision()>entry.publication.world().revision())throw new SecurityException("greater haste owner mismatch");
            return new RevivalAdmission(entry.engine,entry.output,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null)
            return greaterHasteReply(q,admission.output(),ModernGreaterHasteCast.Reason.NOT_ACTIVE);
        var engine=admission.engine();var observed=engine.observeCommand(()->engine.canonicalGreaterHaste(owner,q));
        var exact=publication(entry,observed.observation());publish(entry,engine,observed.observation());
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("greater haste completion retired");
            var output=entry.output.authority!=null&&entry.output.authority.capture()==observed.observation()?entry.output:exact;
            return greaterHasteReply(q,output,observed.outcome().reason());
        });
    }
    private static ModernGreaterHasteProtocol.Reply greaterHasteReply(ModernGreaterHasteProtocol.Request q,Publication output,ModernGreaterHasteCast.Reason reason) {
        return new ModernGreaterHasteProtocol.Reply(q,reason,output.authority.audience(q.proof().player()),output.lifecycle);
    }
    /** One owner-scoped, exact capture; no legacy projection or second read. */
    ModernHasteProtocol.Reply haste(TournamentPresence.Owner owner,ModernHasteProtocol.Request q) {
        var proof=q.proof();final Registration registration;final Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,proof.player());entry=registration.entry;
            if(!ModernHasteProtocol.negotiated(registration.protocol)||registration.haste==null||entry==null
                    ||!entry.descriptor.has(ModernMatchDescriptor.HASTE)
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation())
                throw new SecurityException("haste attachment mismatch");
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry||entry.publication==null)throw new SecurityException("haste attachment unavailable");
            var actor=entry.publication.audience(proof.player()).world().actors().stream()
                    .filter(a->a.pose().entityId()==proof.entity()).findFirst().orElseThrow(SecurityException::new);
            if(!actor.pose().member().identity().equals(ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))
                    ||proof.revision()>entry.publication.world().revision())throw new SecurityException("haste owner mismatch");
            return new RevivalAdmission(entry.engine,entry.output,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null)
            return hasteReply(q,admission.output(),ModernHasteCast.Reason.NOT_ACTIVE);
        var engine=admission.engine();var observed=engine.observeCommand(()->engine.canonicalHaste(owner,q));
        var exact=publication(entry,observed.observation());publish(entry,engine,observed.observation());
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("haste completion retired");
            var output=entry.output.authority!=null&&entry.output.authority.capture()==observed.observation()?entry.output:exact;
            return hasteReply(q,output,observed.outcome().reason());
        });
    }
    private static ModernHasteProtocol.Reply hasteReply(ModernHasteProtocol.Request q,Publication output,ModernHasteCast.Reason reason) {
        return new ModernHasteProtocol.Reply(q,reason,output.authority.audience(q.proof().player()),output.lifecycle);
    }
    /** One owner-scoped, exact capture; no legacy projection or second read. */
    ModernSlowProtocol.Reply slow(TournamentPresence.Owner owner,ModernSlowProtocol.Request q) {
        var proof=q.proof();final Registration registration;final Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,proof.player());entry=registration.entry;
            if(!ModernSlowProtocol.negotiated(registration.protocol)||registration.slow==null||entry==null
                    ||!entry.descriptor.has(ModernMatchDescriptor.SLOW)
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation())
                throw new SecurityException("slow attachment mismatch");
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry||entry.publication==null)throw new SecurityException("slow attachment unavailable");
            var actor=entry.publication.audience(proof.player()).world().actors().stream()
                    .filter(a->a.pose().entityId()==proof.entity()).findFirst().orElseThrow(SecurityException::new);
            if(!actor.pose().member().identity().equals(ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))
                    ||proof.revision()>entry.publication.world().revision())throw new SecurityException("slow owner mismatch");
            return new RevivalAdmission(entry.engine,entry.output,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null)
            return slowReply(q,admission.output(),ModernSlowCast.Reason.NOT_ACTIVE);
        var engine=admission.engine();var observed=engine.observeCommand(()->engine.canonicalSlow(owner,q));
        var exact=publication(entry,observed.observation());publish(entry,engine,observed.observation());
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("slow completion retired");
            var output=entry.output.authority!=null&&entry.output.authority.capture()==observed.observation()?entry.output:exact;
            return slowReply(q,output,observed.outcome().reason());
        });
    }
    private static ModernSlowProtocol.Reply slowReply(ModernSlowProtocol.Request q,Publication output,ModernSlowCast.Reason reason) {
        return new ModernSlowProtocol.Reply(q,reason,output.authority.audience(q.proof().player()),output.lifecycle);
    }
    /** The authenticated registration, explicit source and command capability
     * must all agree. A DATA-only peer can observe but cannot spend MP. */
    ModernPlayerHealingProtocol.Reply playerHeal(TournamentPresence.Owner owner,ModernPlayerHealingProtocol.Request q) {
        var proof=q.proof();final Registration registration;final Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,proof.player());entry=registration.entry;
            if(!registration.playerHealingSource||!ModernSessionExtensions.allowsCommands(registration.protocol)||entry==null
                    ||!entry.descriptor.has(ModernMatchDescriptor.PLAYER_HEALING)
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation())
                throw new SecurityException("player healing attachment mismatch");
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry||entry.publication==null)throw new SecurityException("player healing attachment unavailable");
            var actor=entry.publication.audience(proof.player()).world().actors().stream()
                    .filter(a->a.pose().entityId()==proof.entity()).findFirst().orElseThrow(SecurityException::new);
            if(!actor.pose().member().identity().equals(ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))
                    ||proof.revision()>entry.publication.world().revision())throw new SecurityException("player healing owner mismatch");
            return new RevivalAdmission(entry.engine,entry.output,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null)
            return playerHealReply(q,admission.output(),ModernPlayerHealingCast.Reason.NOT_ACTIVE);
        var engine=admission.engine();var observed=engine.observeCommand(()->engine.canonicalPlayerHeal(owner,q));
        var exact=publication(entry,observed.observation());publish(entry,engine,observed.observation());
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("player healing completion retired");
            var output=entry.output.authority!=null&&entry.output.authority.capture()==observed.observation()?entry.output:exact;
            return playerHealReply(q,output,observed.outcome().reason());
        });
    }
    private static ModernPlayerHealingProtocol.Reply playerHealReply(ModernPlayerHealingProtocol.Request q,Publication output,ModernPlayerHealingCast.Reason reason) {
        return new ModernPlayerHealingProtocol.Reply(q,reason,output.authority.audience(q.proof().player()),output.lifecycle);
    }
    /** Dead-player command with its own exact-death admission. No legacy NPC
     * frame, live-action bypass, optimistic revival or second engine read. */
    ModernApplicationProtocol.BattleRevivalResult revival(TournamentPresence.Owner owner,
            ModernApplicationProtocol.BattleRevivalRequest message) {
        var q=message.request();var proof=q.proof();final Registration registration;final Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,proof.player());entry=registration.entry;
            if(!ModernBattleRevivalProtocol.negotiated(registration.protocol)||entry==null
                    ||!entry.descriptor.has(ModernMatchDescriptor.QUEST_LIVES)
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation())
                throw new SecurityException("death confirmation attachment mismatch");
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry||entry.publication==null)throw new SecurityException("death confirmation attachment retired");
            var authority=entry.publication;
            var actor=authority.world().actors().stream().filter(a->a.pose().member().identity().equals(
                    ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))).findFirst().orElseThrow(SecurityException::new);
            if(actor.pose().entityId()!=proof.entity()||entry.terminal&&proof.revision()>authority.world().revision())
                throw new SecurityException("death confirmation owner mismatch");
            return new RevivalAdmission(entry.engine,entry.output,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null)
            return revivalReply(message,registration,admission.output(),ModernBattleRevivalProtocol.Reason.NOT_ACTIVE);
        var engine=admission.engine();
        var observed=engine.observeCommand(()->engine.confirmCanonicalQuestDeath(owner,proof,q.deathRevision()));
        var exact=publication(entry,observed.observation());publish(entry,engine,observed.observation());
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("death confirmation completion retired");
            var output=entry.output.authority!=null&&entry.output.authority.capture()==observed.observation()?entry.output:exact;
            return revivalReply(message,registration,output,ModernBattleRevivalProtocol.Reason.valueOf(observed.outcome().reason().name()));
        });
    }
    private record RevivalAdmission(ModernBattleEngine engine,Publication output,boolean terminal) {}
    /** Dedicated complete-VIP worker path. No provider/store I/O under the
     * service gate; ordinary equipment DTOs cannot carry independent versions. */
    ModernApplicationProtocol.VipEquipmentResult vipEquipment(TournamentPresence.Owner owner,
            ModernApplicationProtocol.VipEquipmentRequest message,ModernVipEquipmentAuthority authority,
            ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
            ModernEquipmentLease.Operation operation)throws Exception {
        var q=message.request();var proof=q.proof().life();final Registration registration;final Entry entry;
        if(ModernBackendScope.current()!=operation.scope())throw new SecurityException("VIP equipment worker scope mismatch");
        operation.scope().checkActive();
        synchronized(gate) {
            requireOpen();registration=requireRegistration(owner,proof.player());entry=registration.entry;
            if(!ModernSessionExtensions.allowsVipEquipment(registration.protocol)||!(registration.physical instanceof FullVipReader)
                    ||entry==null||!entry.mapping.fullVipEnabled()
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation()
                    ||!operation.matchesSelection(owner,proof.player().epoch())
                    ||!operation.key().equals(new TournamentPresence.CharacterKey(proof.player().sourceId(),proof.player().objectId()))
                    ||!principal.authorizesSource(proof.player().sourceId())||selected.objectId()!=proof.player().objectId())
                throw new SecurityException("VIP equipment attachment mismatch");
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry||entry.publication==null)throw new SecurityException("VIP equipment attachment retired");
            var view=entry.publication.vipView(proof.player());
            if(view.personal().entityId()!=proof.entity()||entry.terminal&&proof.revision()>view.world().revision())
                throw new SecurityException("VIP equipment owner mismatch");
            return new RevivalAdmission(entry.engine,entry.output,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null) {
            return published(entry,()->{
                requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("VIP equipment terminal attachment retired");
                var output=entry.output;var refusal=new ModernBattleVipCommandState.EquipmentAdmission(
                        ModernBattleVipCommandState.Rejection.NOT_ACTIVE,null,output.authority.capture());
                var reply=ModernVipEquipmentProtocol.rejected(q,refusal,entry.descriptor,output.lifecycle);reply.payload().requirePeer(registration.protocol);
                return new ModernApplicationProtocol.VipEquipmentResult(reply);
            });
        }
        var engine=admission.engine();var reservation=engine.admitVipEquipmentChange(owner,q.proof());
        ModernVipEquipmentAuthority.Inspection inspection=null;ModernVipEquipmentAuthority.Result committed=null;
        var capture=reservation.observation();
        if(reservation.change()!=null) {
            if(q.action()==ModernVipEquipmentProtocol.Action.INSPECT) {
                inspection=authority.inspectCaptured(principal,selected,operation,q.proof(),reservation.change());capture=inspection.observation();
            } else {committed=authority.execute(principal,selected,operation,q.command(),reservation.change());capture=committed.observation();}
        }
        var exact=publication(entry,capture);publish(entry,engine,capture);
        final var retained=capture;final var inspected=inspection;final var result=committed;
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("VIP equipment completion retired");
            var output=entry.output.authority!=null&&entry.output.authority.capture()==retained?entry.output:exact;
            var receipt=inspected!=null?ModernVipEquipmentProtocol.inspected(q,inspected,entry.descriptor,output.lifecycle)
                    :result!=null?ModernVipEquipmentProtocol.executed(q,result,entry.descriptor,output.lifecycle)
                    :ModernVipEquipmentProtocol.rejected(q,reservation,entry.descriptor,output.lifecycle);
            receipt.payload().requirePeer(registration.protocol);return new ModernApplicationProtocol.VipEquipmentResult(receipt);
        });
    }
    /** Navigation's selected-owner backend worker owns the lease and I/O.
     * No service/engine monitor is held across provider or durable store work. */
    ModernApplicationProtocol.BattleEquipmentResult equipment(TournamentPresence.Owner owner,
            ModernApplicationProtocol.BattleEquipmentRequest message,ModernEquipmentAuthority authority,
            ModernAccountAuthentication.Principal principal,ModernApplicationProtocol.CharacterRow selected,
            ModernEquipmentLease.Operation operation)throws Exception {
        var q=message.request();var proof=q.proof();final Registration registration;final Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,proof.player());entry=registration.entry;
            if(!ModernBattleEquipmentProtocol.negotiated(registration.protocol)||registration.physical==null||entry==null
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation()
                    ||!operation.matchesSelection(owner,proof.player().epoch()))
                throw new SecurityException("live equipment attachment mismatch");
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry||entry.publication==null)throw new SecurityException("live equipment retired");
            var actor=entry.publication.world().actors().stream().filter(a->a.pose().member().identity().equals(
                    ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))).findFirst().orElseThrow(SecurityException::new);
            if(actor.pose().entityId()!=proof.entity()||entry.terminal&&proof.revision()>entry.publication.world().revision())
                throw new SecurityException("live equipment owner mismatch");
            return new RevivalAdmission(entry.engine,entry.output,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null)
            return equipmentReply(message,registration,admission.output(),ModernBattleEquipmentProtocol.Reason.NOT_ACTIVE,null,null);
        var engine=admission.engine();
        var reservation=engine.admitCanonicalEquipmentChange(owner,proof);
        ModernEquipmentAuthority.MatchResult committed=null;
        var capture=reservation.observation();
        if(reservation.change()!=null) {
            committed=authority.executeInMatch(principal,selected,operation,q.equipment(),reservation.change());
            capture=Objects.requireNonNull(committed.observation(),"equipment publication retired before acknowledgement");
        }
        // Exact response capture, not an unrelated later engine READ. A later
        // service result may be inherited only when it retains this capture.
        var exact=publication(entry,capture);publish(entry,engine,capture);
        final var retained=capture;final var result=committed;
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("live equipment completion retired");
            var output=entry.output.authority!=null&&entry.output.authority.capture()==retained?entry.output:exact;
            return equipmentReply(message,registration,output,reservation.reason(),
                    result==null?null:result.equipment(),result==null?null:result.publication());
        });
    }
    private static ModernApplicationProtocol.BattleEquipmentResult equipmentReply(
            ModernApplicationProtocol.BattleEquipmentRequest q,Registration registration,Publication output,
            ModernBattleEquipmentProtocol.Reason reason,ModernEquipmentAuthority.Result equipment,
            ModernBattleEngine.EquipmentPublication publication) {
        var disposition=publication==null?ModernBattleEquipmentProtocol.Disposition.NOT_COMMITTED
                :ModernBattleEquipmentProtocol.Disposition.valueOf(publication.disposition().name());
        return new ModernApplicationProtocol.BattleEquipmentResult(q.requestId(),q.request(),
                new ModernBattleEquipmentProtocol.Reply(reason,disposition,publication==null?-1:publication.equipmentRevision(),
                        equipment,output.authority.audience(registration.scope),output.lifecycle));
    }
    private static ModernApplicationProtocol.BattleRevivalResult revivalReply(
            ModernApplicationProtocol.BattleRevivalRequest q,Registration registration,Publication output,ModernBattleRevivalProtocol.Reason reason) {
        return new ModernApplicationProtocol.BattleRevivalResult(q.requestId(),q.request(),
                new ModernBattleRevivalProtocol.Reply(reason,output.authority.audience(registration.scope),output.lifecycle));
    }
    private record ActionExecution(Registration registration,Entry entry,Publication output,
            ModernBattleActionReceipt receipt) {}
    private record ActionAdmission(ModernBattleEngine engine,Publication output,boolean available,boolean terminal) {}

    private ActionExecution executeAction(TournamentPresence.Owner owner,
            ModernApplicationProtocol.BattleActionRequest message) {
        Objects.requireNonNull(message);var guard=message.request();var proof=guard.proof();
        final Registration registration;final Entry entry;final boolean available;
        synchronized(gate) {
            requireOpen();registration=requireOrdinaryRegistration(owner,proof.player());entry=registration.entry;
            if(!ModernBattleActionProtocol.negotiated(registration.protocol)||entry==null
                    ||entry.ticket.snapshot().summary().id()!=proof.world().roomId()
                    ||entry.ticket.matchId()!=proof.world().matchId()||entry.ticket.generation()!=proof.world().generation())
                throw new SecurityException("battle action attachment mismatch");
            available=actionAvailable(registration,entry,guard.command());
        }
        var admission=published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("battle action attachment changed");
            var authority=entry.publication;
            if(authority==null)throw new SecurityException("battle action not prepared");
            var actor=authority.world().actors().stream().filter(a->a.pose().member().identity().equals(
                    ModernParticipantId.human(proof.player().sourceId(),proof.player().objectId()))).findFirst().orElseThrow(SecurityException::new);
            // The live engine validates newer revisions under its command monitor.
            if(actor.pose().entityId()!=proof.entity()||entry.terminal&&proof.revision()>authority.world().revision())
                throw new SecurityException("battle action identity mismatch");
            return new ActionAdmission(entry.engine,entry.output,available,entry.terminal);
        });
        if(admission.terminal()||admission.engine()==null)
            return actionReceipt(registration,entry,admission.output(),ModernBattleCommandLife.Rejection.NOT_ACTIVE,true,null);
        if(!admission.available())
            return actionReceipt(registration,entry,admission.output(),ModernBattleCommandLife.Rejection.NONE,false,null);
        var engine=admission.engine();
        var result=engine.observeLifeCommand(owner,proof,guard.target(),()->dispatchAction(engine,owner,guard.command()));
        // Construct the exact immutable candidate outside all service locks.
        var exact=publication(entry,result.observation());
        publish(entry,engine,result.observation());
        return published(entry,()->{
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("battle action completion attachment changed");
            // Only the identical capture may inherit a later persistence receipt.
            // Never attach a newer terminal/SAVED result to an older active ACK.
            var output=entry.output.authority!=null&&entry.output.authority.capture()==result.observation()?entry.output:exact;
            return actionReceipt(registration,entry,output,result.rejection(),true,
                    result.accepted()==null?null:result.accepted().outcome());
        });
    }
    private static ActionExecution actionReceipt(Registration registration,Entry entry,Publication output,
            ModernBattleCommandLife.Rejection rejection,boolean available,ModernBattleCommand outcome) {
        return new ActionExecution(registration,entry,output,new ModernBattleActionReceipt(rejection,available,outcome,
                output.authority.audience(registration.scope),output.lifecycle));
    }
    /** Request binding already proves every inner scope/world/target. Preserve
     * each feature's original negotiation checks before touching the engine. */
    private static boolean actionAvailable(Registration r,Entry e,ModernApplicationProtocol.Message command) {
        if(command instanceof ModernApplicationProtocol.WorldRequest)return true;
        if(command instanceof ModernApplicationProtocol.CombatRequest) {
            if(r.physical==null)throw new SecurityException("combat was not negotiated");
            return e.descriptor.has(ModernMatchDescriptor.COMBAT);
        }
        if(command instanceof ModernApplicationProtocol.CastRequest q) {
            if(r.skills==null||r.physical==null||q.request().skillId()==70&&!r.fireStormProtocol)
                throw new SecurityException("spells were not negotiated");
            return e.mapping.spellsEnabled();
        }
        if(command instanceof ModernApplicationProtocol.ItemRequest) {
            if(r.matchItems==null||r.physical==null)throw new SecurityException("match items were not negotiated");
            return e.mapping.matchItems();
        }
        if(command instanceof ModernApplicationProtocol.AmmoRequest) {
            if(!r.bowProtocol)throw new SecurityException("arrows not negotiated");
            return e.mapping.bowEnabled();
        }
        if(command instanceof ModernApplicationProtocol.DisintegrateRequest) {
            if(r.disintegrate==null||!ModernDisintegrateProtocol.negotiated(r.protocol))
                throw new SecurityException("Disintegrate was not negotiated");
            return e.mapping.disintegrateEnabled();
        }
        if(command instanceof ModernApplicationProtocol.SiegeRequest) {
            if(e.mapping.siege()==null||r.physical==null||r.protocol==null
                    ||!e.descriptor.supportedBy(r.protocol))
                throw new SecurityException("siege scope or negotiation mismatch");
            return true;
        }
        if(e.mapping.npc()==null||r.physical==null||!npcNegotiated(r.protocol))
            throw new SecurityException("NPC owner/match capability mismatch");
        if(command instanceof ModernApplicationProtocol.NpcRequest q) {
            if(q.request().action()==ModernNpcProtocol.Action.ATTACK_OBJECTIVE&&!e.descriptor.has(ModernMatchDescriptor.QUEST_OBJECTIVES)
                    ||q.request().action()==ModernNpcProtocol.Action.ATTACK_TOWER
                        &&(!e.descriptor.has(ModernMatchDescriptor.TIME_ATTACK)||r.protocol.version()<25))
                throw new SecurityException("NPC objective capability mismatch");
            return true;
        }
        if(command instanceof ModernApplicationProtocol.NpcSpellRequest) {
            if(!e.mapping.npc().lightArrow()||r.skills==null||r.protocol.version()<24||!e.descriptor.has(ModernMatchDescriptor.NPC_LIGHT_ARROW))
                throw new SecurityException("NPC spell capability mismatch");
            return true;
        }
        if(command instanceof ModernApplicationProtocol.NpcDisintegrateRequest) {
            if(!e.mapping.npcDisintegrateEnabled()||r.disintegrate==null||r.skills==null
                    ||!ModernNpcDisintegrateProtocol.negotiated(r.protocol)||!e.descriptor.has(ModernMatchDescriptor.NPC_DISINTEGRATE))
                throw new SecurityException("NPC77 capability mismatch");
            return true;
        }
        if(command instanceof ModernApplicationProtocol.NpcFireStormRequest) {
            if(!e.mapping.npcFireStormEnabled()||r.skills==null||!ModernNpcFireStormProtocol.negotiated(r.protocol)
                    ||!e.descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM))
                throw new SecurityException("NPC70 capability mismatch");
            return true;
        }
        throw new SecurityException("battle action opcode mismatch");
    }
    private static ModernBattleCommand dispatchAction(ModernBattleEngine engine,TournamentPresence.Owner owner,
            ModernApplicationProtocol.Message command) {
        if(command instanceof ModernApplicationProtocol.WorldRequest q) {
            var r=q.request();return engine.move(owner,r.scope(),r.matchId(),r.generation(),r.sequence(),r.fromX(),r.fromY(),r.heading());
        }
        if(command instanceof ModernApplicationProtocol.CombatRequest q) {
            var r=q.request();return engine.attack(owner,r.scope(),r.matchId(),r.generation(),r.sequence(),r.targetEntityId());
        }
        if(command instanceof ModernApplicationProtocol.CastRequest q)return engine.cast(owner,q.request());
        if(command instanceof ModernApplicationProtocol.ItemRequest q)return engine.item(owner,q.request());
        if(command instanceof ModernApplicationProtocol.AmmoRequest q)return engine.ammo(owner,q.request());
        if(command instanceof ModernApplicationProtocol.DisintegrateRequest q)return engine.disintegrate(owner,q.request());
        if(command instanceof ModernApplicationProtocol.NpcDisintegrateRequest q)return engine.disintegrateNpc(owner,q.request());
        if(command instanceof ModernApplicationProtocol.NpcFireStormRequest q)return engine.fireStormNpc(owner,q.request());
        if(command instanceof ModernApplicationProtocol.NpcSpellRequest q) {
            var r=q.request();return engine.castNpc(owner,r.player(),r.world().matchId(),r.world().generation(),
                    r.sequence(),r.casterLife(),r.target(),r.targetLife());
        }
        if(command instanceof ModernApplicationProtocol.SiegeRequest q) {
            var r=q.request();return r.action()==ModernSiegeProtocol.Action.ATTACK_TOWER
                    ?engine.attackSiegeTower(owner,r.player(),r.world().matchId(),r.world().generation(),r.sequence(),new ModernSiege.Target(r.world(),r.tower()))
                    :engine.useSiegePortal(owner,r.player(),r.world().matchId(),r.world().generation(),r.sequence());
        }
        if(command instanceof ModernApplicationProtocol.NpcRequest q) {
            var r=q.request();return r.action()==ModernNpcProtocol.Action.ATTACK_OBJECTIVE
                    ?engine.attackQuestObjective(owner,r.player(),r.world().matchId(),r.world().generation(),r.sequence(),r.objective())
                    :r.action()==ModernNpcProtocol.Action.ATTACK_TOWER
                    ?engine.attackTimeAttackTower(owner,r.player(),r.world().matchId(),r.world().generation(),r.sequence(),r.tower())
                    :engine.attackNpc(owner,r.player(),r.world().matchId(),r.world().generation(),r.sequence(),r.target(),r.life());
        }
        throw new SecurityException("battle action opcode mismatch");
    }

    /** Strict legacy projection is deliberately confined to this v37 adapter.
     * The canonical service result remains valid when historical prices differ. */
    private ModernApplicationProtocol.Message legacyAction(ModernApplicationProtocol.BattleActionRequest message,ActionExecution execution) {
        var r=execution.receipt();var entry=execution.entry();var registration=execution.registration();
        var outcome=r.outcome();var command=message.request().command();int id=message.requestId();boolean available=r.available();
        var exact=available?execution.output().legacy():null;
        return published(entry,()->{
            requireActionCurrent(registration);if(registration.entry!=entry)throw new SecurityException("battle action adapter attachment changed");
            var scope=registration.scope;
            var combat=exact==null?null:ModernQuestVisibility.combat(exact.combat(),scope);
            var npc=exact==null?null:ModernQuestVisibility.npc(exact.npc(),scope);
            if(command instanceof ModernApplicationProtocol.WorldRequest q) {
                var status=((ModernBattleEngine.MoveResult)outcome).status();
                Reason reason=entry.terminal?Reason.NOT_ACTIVE:switch(status) {
                    case MOVED -> Reason.NONE;case NOT_ACTIVE,ABORTED -> Reason.NOT_ACTIVE;
                    case STALE_POSITION -> Reason.STALE_POSITION;case TOO_FAST -> Reason.TOO_FAST;
                    case BLOCKED -> Reason.BLOCKED;case OCCUPIED -> Reason.OCCUPIED;
                };
                return new ModernApplicationProtocol.WorldResult(id,q.request(),reply(reason==Reason.NONE?Status.OK:Status.DENIED,reason,ModernQuestVisibility.world(entry.frame(),scope)));
            }
            if(command instanceof ModernApplicationProtocol.CombatRequest q) {
                var reason=!available?ModernCombatProtocol.Reason.NOT_ACTIVE:
                        ((ModernBattleEngine.AttackResult)outcome).status()==ModernBattleEngine.AttackStatus.ATTACKED?ModernCombatProtocol.Reason.NONE:
                        ModernCombatProtocol.Reason.valueOf(((ModernBattleEngine.AttackResult)outcome).status().name());
                return new ModernApplicationProtocol.CombatResult(id,q.request(),new ModernCombatProtocol.Reply(!available?ModernCombatProtocol.Status.UNAVAILABLE:reason==ModernCombatProtocol.Reason.NONE?ModernCombatProtocol.Status.OK:ModernCombatProtocol.Status.DENIED,reason,combat));
            }
            if(command instanceof ModernApplicationProtocol.CastRequest q) {
                var reason=available?((ModernBattleEngine.CastResult)outcome).reason():ModernSpellProtocol.Reason.NOT_ACTIVE;
                return new ModernApplicationProtocol.CastResult(id,q.request(),new ModernSpellProtocol.Reply(!available?ModernSpellProtocol.Status.UNAVAILABLE:reason==ModernSpellProtocol.Reason.NONE?ModernSpellProtocol.Status.OK:ModernSpellProtocol.Status.DENIED,reason,combat));
            }
            if(command instanceof ModernApplicationProtocol.ItemRequest q) {
                var reason=available?((ModernBattleEngine.ItemResult)outcome).reason():ModernItemProtocol.Reason.NOT_ACTIVE;
                return new ModernApplicationProtocol.ItemResult(id,q.request(),new ModernItemProtocol.Reply(!available?ModernItemProtocol.Status.UNAVAILABLE:reason==ModernItemProtocol.Reason.NONE?ModernItemProtocol.Status.OK:ModernItemProtocol.Status.DENIED,reason,combat));
            }
            if(command instanceof ModernApplicationProtocol.AmmoRequest q) {
                var reason=available?((ModernBattleEngine.AmmoResult)outcome).reason():ModernAmmoProtocol.Reason.NOT_ACTIVE;
                return new ModernApplicationProtocol.AmmoResult(id,q.request(),new ModernAmmoProtocol.Reply(!available?ModernAmmoProtocol.Status.UNAVAILABLE:reason==ModernAmmoProtocol.Reason.NONE?ModernAmmoProtocol.Status.OK:ModernAmmoProtocol.Status.DENIED,reason,combat));
            }
            if(command instanceof ModernApplicationProtocol.DisintegrateRequest q) {
                var reason=available?((ModernBattleEngine.DisintegrateResult)outcome).reason():ModernDisintegrateState.Reason.NOT_ACTIVE;
                return new ModernApplicationProtocol.DisintegrateResult(id,q.request(),new ModernDisintegrateProtocol.Reply(!available?ModernSpellProtocol.Status.UNAVAILABLE:reason==ModernDisintegrateState.Reason.NONE?ModernSpellProtocol.Status.OK:ModernSpellProtocol.Status.DENIED,reason,combat));
            }
            if(command instanceof ModernApplicationProtocol.SiegeRequest q) {
                var reason=outcome instanceof ModernBattleEngine.AttackResult a
                        ?a.status()==ModernBattleEngine.AttackStatus.ATTACKED?ModernSiegeProtocol.Reason.NONE:ModernSiegeProtocol.Reason.valueOf(a.status().name())
                        :((ModernBattleEngine.SiegePortalResult)outcome).reason()==ModernSiege.PortalReason.MOVED?ModernSiegeProtocol.Reason.NONE:ModernSiegeProtocol.Reason.valueOf(((ModernBattleEngine.SiegePortalResult)outcome).reason().name());
                return new ModernApplicationProtocol.SiegeResult(id,q.request(),new ModernSiegeProtocol.Reply(reason==ModernSiegeProtocol.Reason.NONE?ModernCombatProtocol.Status.OK:ModernCombatProtocol.Status.DENIED,reason,combat));
            }
            if(command instanceof ModernApplicationProtocol.NpcRequest q) {
                var reason=((ModernNpcBattle.Result)outcome).reason()==ModernNpcBattle.Reason.ATTACKED?ModernNpcProtocol.Reason.NONE:ModernNpcProtocol.Reason.valueOf(((ModernNpcBattle.Result)outcome).reason().name());
                return new ModernApplicationProtocol.NpcResult(id,q.request(),new ModernNpcProtocol.Reply(reason==ModernNpcProtocol.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,reason,entry.terminal?ModernQuestVisibility.npc(entry.npc(),scope):npc));
            }
            if(command instanceof ModernApplicationProtocol.NpcSpellRequest q) {
                var reason=((ModernNpcMagic.Result)outcome).reason();
                return new ModernApplicationProtocol.NpcSpellResult(id,q.request(),new ModernNpcSpellProtocol.Reply(reason==ModernNpcMagic.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,reason,npc));
            }
            if(command instanceof ModernApplicationProtocol.NpcDisintegrateRequest q) {
                var reason=((ModernNpcDisintegrateState.Result)outcome).reason();
                return new ModernApplicationProtocol.NpcDisintegrateResult(id,q.request(),new ModernNpcDisintegrateProtocol.Reply(reason==ModernNpcDisintegrateState.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,reason,npc));
            }
            if(command instanceof ModernApplicationProtocol.NpcFireStormRequest q) {
                var reason=((ModernNpcFireStormState.Result)outcome).reason();
                return new ModernApplicationProtocol.NpcFireStormResult(id,q.request(),new ModernNpcFireStormProtocol.Reply(reason==ModernNpcFireStormState.Reason.NONE?ModernNpcProtocol.Status.OK:ModernNpcProtocol.Status.DENIED,reason,npc));
            }
            throw new SecurityException("battle action adapter opcode mismatch");
        });
    }
    private <T extends ModernBattleCommand> ModernBattleEngine.Observed<T> observeAction(Entry entry,
            ModernBattleEngine engine,TournamentPresence.Owner owner,ModernBattleActionProtocol.Request guard,Supplier<T> command) {
        if(guard==null)return engine.observeCommand(command);
        var result=engine.observeLifeCommand(owner,guard.proof(),guard.target(),command);
        if(result.rejection()!=ModernBattleCommandLife.Rejection.NONE)
            throw new ActionRejected(entry,engine,result.rejection(),result.observation());
        return result.accepted();
    }
    private static final class ActionRejected extends RuntimeException {
        final Entry entry;final ModernBattleEngine engine;final ModernBattleCommandLife.Rejection reason;final ModernBattleObservation.Capture observation;
        ActionRejected(Entry e,ModernBattleEngine b,ModernBattleCommandLife.Rejection r,ModernBattleObservation.Capture c) {
            super("battle action admission rejected",null,false,false);entry=e;engine=b;reason=r;observation=c;
        }
    }
    private ModernApplicationProtocol.Message recoverAction(Registration registration,Entry entry,int id,
            ModernBattleActionProtocol.Request guard,Publication output) {
        var p=guard.proof();var s=p.player();var w=p.world();
        // Project this receipt, not an independently reconstructed engine frame:
        // terminal recovery must retain its already-confirmed SAVED lifecycle.
        var exact=output.legacy();
        return published(entry,()-> {
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("battle action recovery attachment changed");
            var n=exact.npc();
            if(n!=null)return new ModernApplicationProtocol.NpcResult(id,new ModernNpcProtocol.Request(s,ModernNpcProtocol.Action.READ,
                    new ModernNpcRoster.Scope(w.roomId(),w.matchId(),w.generation()),0,0,0),
                    new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,ModernQuestVisibility.npc(n,s)));
            var c=exact.combat();
            if(c!=null)return new ModernApplicationProtocol.CombatResult(id,new ModernCombatProtocol.Request(s,ModernCombatProtocol.Action.READ,w.matchId(),w.generation(),0,0),
                    new ModernCombatProtocol.Reply(ModernCombatProtocol.Status.OK,ModernCombatProtocol.Reason.NONE,ModernQuestVisibility.combat(c,s)));
            var f=exact.frame();
            return new ModernApplicationProtocol.WorldResult(id,new ModernWorldProtocol.Request(s,ModernWorldProtocol.Action.READ,0,0,0,w.matchId(),w.generation(),0,0,0,ModernWorldProtocol.ZERO_DIGEST),
                    new ModernWorldProtocol.Reply(ModernWorldProtocol.Status.OK,ModernWorldProtocol.Reason.NONE,ModernQuestVisibility.world(f,s)));
        });
    }

    /** Current authenticated attachment -> one immutable canonical audience.
     * Never reconstructs data or engine revisions from a legacy frame. */
    ModernBattleReadProtocol.Reply readBattle(TournamentPresence.Owner owner,ModernBattleReadProtocol.Request request) {
        Objects.requireNonNull(request);final Registration registration;final Entry entry;
        synchronized(gate) {
            requireOpen();registration=requireRegistration(owner,request.scope());
            if(!ModernBattleReadProtocol.negotiated(registration.protocol))throw new SecurityException("battle read not negotiated");
            requireNativeVerified(registration);
            entry=registration.entry;
            if(entry==null||entry.ticket.snapshot().summary().id()!=request.world().roomId()
                    ||entry.ticket.matchId()!=request.world().matchId()||entry.ticket.generation()!=request.world().generation())
                throw new SecurityException("battle read attachment mismatch");
        }
        advance(entry);
        return published(entry,()-> {
            requireActionCurrent(registration);
            if(registration.entry!=entry)throw new SecurityException("battle read attachment changed");
            if(entry.publication==null)return new ModernBattleReadProtocol.Reply(entry.terminal
                    ?ModernBattleReadProtocol.State.UNAVAILABLE:ModernBattleReadProtocol.State.PREPARING,null);
            if(entry.mapping.fullVipEnabled()) {
                var payload=entry.publication.vip(request.scope());payload.requirePeer(registration.protocol);
                return ModernBattleReadProtocol.Reply.vip(payload);
            }
            return new ModernBattleReadProtocol.Reply(ModernBattleReadProtocol.State.DATA,entry.publication.audience(request.scope()));
        });
    }

    Reply handle(TournamentPresence.Owner owner, Request request) {
        if(!nativeResourcePlans.isEmpty()&&nativeResourcePlan(owner,request.scope())!=null)
            throw new SecurityException("Native binding rejects legacy WORLD route");
        return handle(owner,request,null);
    }
    ModernWorldResourceProtocol.Result nativeWorld(TournamentPresence.Owner owner,ModernWorldResourceProtocol.Request request,
                                                    ModernSessionBoundary.Negotiated peer) {
        if(!nativeCanonicalSources.isEmpty())throw new SecurityException("Native canonical binding rejects diagnostic WORLD route");
        var plan=nativeResourcePlan(owner,request.world().scope());
        ModernWorldResourceProtocol.authorize(plan,request,peer);
        return new ModernWorldResourceProtocol.Result(request,plan,handle(owner,request.world(),null));
    }
    private ModernWorldResourceProtocol.Plan nativeResourcePlan(TournamentPresence.Owner owner,ModernRooms.Scope scope) {
        final Registration row;
        synchronized(gate){requireOpen();row=requireRegistration(owner,scope);if(row.entry!=null)return nativeResourcePlans.get(row.entry.mapping.binding().key());}
        var joined=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view().joined();
        synchronized(gate){requireCurrent(row);if(row.entry!=null)throw new SecurityException("world discovery changed");}
        return joined==null?null:nativeResourcePlans.get(new ModernWorldBinding.Key(joined.summary().config().mode(),joined.summary().config().mapId()));
    }
    private record NativeContext(Registration registration,Entry entry,
            ModernNativeCanonicalWorldProtocol.SourceAttachment source,long room,long revision) {}
    private NativeContext nativeContext(TournamentPresence.Owner owner,ModernRooms.Scope scope) {
        final Registration registration;final Entry entry;
        synchronized(gate){requireOpen();registration=requireRegistration(owner,scope);entry=registration.entry;}
        if(entry!=null)return new NativeContext(registration,entry,nativeCanonicalSources.get(entry.mapping.binding().key()),
                entry.ticket.snapshot().summary().id(),entry.ticket.generation()-1);
        // Capture Rooms outside the service gate; preparation rechecks its authoritative ticket.
        var joined=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,"")).view().joined();
        synchronized(gate){requireCurrent(registration);if(registration.entry!=null)throw new SecurityException("Native discovery attachment changed");}
        if(joined==null)return new NativeContext(registration,null,null,0,0);
        var summary=joined.summary();var config=summary.config();
        return new NativeContext(registration,null,nativeCanonicalSources.get(new ModernWorldBinding.Key(config.mode(),config.mapId())),summary.id(),summary.revision());
    }
    void retireNativeDiscovery(TournamentPresence.Owner owner) {
        synchronized(gate){var row=registrations.get(owner);if(row!=null)row.nativeLedger.retireDiscovery();}
    }
    ModernNativeCanonicalWorldProtocol.Result nativeCanonicalWorld(TournamentPresence.Owner owner,
            ModernNativeCanonicalWorldProtocol.Request request,ModernSessionBoundary.Negotiated peer) {
        if(nativeCatalog!=null)throw new SecurityException("Catalog binding requires the catalog envelope");
        return nativeCanonicalWorldInternal(owner,request,peer);
    }
    ModernNativeCatalogWorldProtocol.Result nativeCatalogWorld(TournamentPresence.Owner owner,
            ModernNativeCatalogWorldProtocol.Request request,ModernSessionBoundary.Negotiated peer) {
        if(nativeActorIdentity!=null)throw new SecurityException("Actor dependency binding requires the actor envelope");
        return nativeCatalogWorldInternal(owner,request,peer);
    }
    private ModernNativeCatalogWorldProtocol.Result nativeCatalogWorldInternal(TournamentPresence.Owner owner,
            ModernNativeCatalogWorldProtocol.Request request,ModernSessionBoundary.Negotiated peer) {
        if(nativeCatalog==null||!ModernNativeCatalogWorldProtocol.negotiated(peer))throw new SecurityException("Native catalog route unavailable");
        var context=nativeContext(owner,request.nativeRequest().world().scope());
        if(context.source()!=null)ModernNativeCatalogWorldProtocol.authorize(nativeCatalog,context.source(),request,peer);
        else if(!ModernNativeCatalogWorldProtocol.ZERO.equals(request.expectedCatalog()))throw new SecurityException("Native catalog source unavailable");
        var actual=nativeCanonicalWorldInternal(owner,request.nativeRequest(),peer);
        return new ModernNativeCatalogWorldProtocol.Result(request,actual.source()==null?null:nativeCatalog,actual);
    }
    private ModernNativeCanonicalWorldProtocol.Result nativeCanonicalWorldInternal(TournamentPresence.Owner owner,
            ModernNativeCanonicalWorldProtocol.Request request,ModernSessionBoundary.Negotiated peer) {
        if(nativeCanonicalSources.isEmpty()||!ModernNativeCanonicalWorldProtocol.negotiated(peer))throw new SecurityException("Native canonical route unavailable");
        if(ModernSessionExtensions.allowsVipData(peer))throw new SecurityException("Native/full VIP selection unavailable");
        var q=request.world();var context=nativeContext(owner,q.scope());var registration=context.registration();var source=context.source();
        ModernNativeResourceLedger.LoadAdmission load=null;
        synchronized(gate) {
            requireCurrent(registration);
            if(!peer.equals(registration.protocol)||registration.entry!=context.entry())throw new SecurityException("Native selection changed");
            if(source==null) {
                if(context.room()!=0||q.action()!=ModernBattleWorldProtocol.Action.READ||q.matchId()!=0
                        ||!request.expectedDigest().equals(ModernWorldProtocol.ZERO_DIGEST))throw new SecurityException("Native source unavailable");
                registration.nativeLedger.retireDiscovery();
                return new ModernNativeCanonicalWorldProtocol.Result(request,null,new ModernBattleWorldProtocol.Reply(Status.OK,Reason.NONE,null,null,null));
            }
            ModernNativeCanonicalWorldProtocol.authorize(source,request,peer);
            if(context.entry()==null) {
                if(q.action()==ModernBattleWorldProtocol.Action.READ&&q.matchId()==0) {
                    registration.nativeLedger.rememberDiscovery(owner,q.scope(),context.room(),context.revision(),source,peer);
                    return new ModernNativeCanonicalWorldProtocol.Result(request,source,new ModernBattleWorldProtocol.Reply(Status.OK,Reason.NONE,null,null,null));
                }
                registration.nativeLedger.admitPrepare(owner,request,context.room(),context.revision(),source,peer);
            }else {
                var ticket=context.entry().ticket;var world=new ModernTargetHistory.World(context.room(),ticket.matchId(),ticket.generation());
                if(q.matchId()!=0&&(q.roomId()!=world.roomId()||q.matchId()!=world.matchId()||q.generation()!=world.generation()))
                    throw new SecurityException("Native world generation differs");
                registration.nativeLedger.requireAttachment(owner,q.scope(),world,source);
                if(q.action()==ModernBattleWorldProtocol.Action.PREPARE)throw new SecurityException("Native already attached");
                if(q.action()==ModernBattleWorldProtocol.Action.LOADED)load=registration.nativeLedger.admitLoaded(owner,request,peer);
            }
        }
        var exchange=handleInternal(owner,q.command(),null);var output=exchange.output();
        var reply=output==null?new ModernBattleWorldProtocol.Reply(exchange.status(),exchange.reason(),null,null,null)
                :new ModernBattleWorldProtocol.Reply(exchange.status(),exchange.reason(),
                    new ModernTargetHistory.World(output.entry.ticket.snapshot().summary().id(),output.entry.ticket.matchId(),output.entry.ticket.generation()),
                    output.lifecycle,output.authority==null?null:output.authority.audience(q.scope()));
        synchronized(gate) {
            requireCurrent(registration);
            if(output!=null) {
                if(registration.entry!=output.entry)throw new SecurityException("Native canonical completion retired");
                registration.nativeLedger.requireAttachment(owner,q.scope(),reply.world(),source);
            }else if(q.action()==ModernBattleWorldProtocol.Action.LEAVE&&registration.entry!=null)
                throw new SecurityException("Native leave completion changed");
            else if(q.action()==ModernBattleWorldProtocol.Action.PREPARE)
                registration.nativeLedger.requirePendingAttachment(owner,q.scope(),context.room(),context.revision(),source,peer);
            if(load!=null&&reply.status()==Status.OK)registration.nativeLedger.completeLoaded(load,reply);
            return new ModernNativeCanonicalWorldProtocol.Result(request,source,reply);
        }
    }
    /** Trusted service result, deliberately not a legacy world packet. Main
     * dispatch and the client lifecycle transport must opt in separately. */
    record VipWorldReply(Status status,Reason reason,ModernTargetHistory.World world,
            ModernBattleLifecycle lifecycle,ModernBattleVipWire.Payload payload) {
        VipWorldReply {
            Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==Status.OK)!=(reason==Reason.NONE)||(world==null)!=(lifecycle==null)||world==null&&payload!=null)throw invalid();
            if(lifecycle!=null) {
                if(lifecycle.race().enabled()||lifecycle.timeAttackFinished()||lifecycle.siegeFinished())throw invalid();
                if(payload==null) {
                    if(lifecycle.phase()!=Phase.PREPARING&&lifecycle.phase()!=Phase.ABORTED||lifecycle.result()!=null)throw invalid();
                }else if(!world.equals(payload.view().world().identity())||lifecycle.revision()<=payload.view().world().revision()
                        ||!Objects.equals(lifecycle.result(),payload.view().world().result()))throw invalid();
                if(payload!=null) {
                    var w=payload.view().world();boolean overlay=lifecycle.phase()==Phase.ABORTED&&w.phase()!=ModernBattleEngine.Phase.FINISHED&&w.result()==null;
                    if(!overlay&&(lifecycle.phase()!=Phase.valueOf(w.phase().name())||lifecycle.abortReason()!=AbortReason.valueOf(w.abortReason().name())))throw invalid();
                }
            }
        }
        @Override public String toString(){return "VipWorldServiceReply[redacted]";}
    }
    VipWorldReply vipWorld(TournamentPresence.Owner owner,ModernBattleWorldProtocol.Request request) {
        if(!nativeResourcePlans.isEmpty()&&nativeResourcePlan(owner,request.scope())!=null)throw new SecurityException("Native/full VIP world combination unavailable");
        final Registration registration;
        synchronized(gate) {
            requireOpen();registration=requireRegistration(owner,request.scope());
            if(!ModernBattleWorldProtocol.negotiated(registration.protocol)||!ModernSessionExtensions.allowsVipData(registration.protocol)
                    ||!(registration.physical instanceof FullVipReader))throw new SecurityException("VIP world source/selection unavailable");
            if(request.matchId()!=0&&(registration.entry==null||registration.entry.ticket.snapshot().summary().id()!=request.roomId()
                    ||registration.entry.ticket.matchId()!=request.matchId()||registration.entry.ticket.generation()!=request.generation()))
                throw new SecurityException("VIP world attachment mismatch");
        }
        var result=handleInternal(owner,request.command(),null,true);var output=result.output();
        if(output==null){synchronized(gate){requireCurrent(registration);}return new VipWorldReply(result.status(),result.reason(),null,null,null);}
        return published(output.entry,()->{
            requireCurrent(registration);if(registration.entry!=output.entry)throw new SecurityException("VIP world completion retired");
            var ticket=output.entry.ticket;var identity=new ModernTargetHistory.World(ticket.snapshot().summary().id(),ticket.matchId(),ticket.generation());
            var payload=output.authority==null?null:output.authority.vip(request.scope());
            if(payload!=null)payload.requirePeer(registration.protocol);
            return new VipWorldReply(result.status(),result.reason(),identity,output.lifecycle,payload);
        });
    }
    ModernBattleWorldProtocol.Reply battleWorld(TournamentPresence.Owner owner,ModernBattleWorldProtocol.Request request) {
        if(!nativeResourcePlans.isEmpty()&&nativeResourcePlan(owner,request.scope())!=null)throw new SecurityException("Native binding requires resource lifecycle");
        synchronized(gate) {
            requireOpen();var registration=requireRegistration(owner,request.scope());
            if(!ModernBattleWorldProtocol.negotiated(registration.protocol))throw new SecurityException("canonical lifecycle not negotiated");
            if(request.matchId()!=0&&(registration.entry==null||registration.entry.ticket.snapshot().summary().id()!=request.roomId()
                    ||registration.entry.ticket.matchId()!=request.matchId()||registration.entry.ticket.generation()!=request.generation()))
                throw new SecurityException("canonical lifecycle attachment mismatch");
        }
        var result=handleInternal(owner,request.command(),null);var output=result.output();
        if(output==null)return new ModernBattleWorldProtocol.Reply(result.status(),result.reason(),null,null,null);
        var ticket=output.entry.ticket;
        var identity=new ModernTargetHistory.World(ticket.snapshot().summary().id(),ticket.matchId(),ticket.generation());
        var view=output.authority==null?null:output.authority.audience(request.scope());
        return new ModernBattleWorldProtocol.Reply(result.status(),result.reason(),identity,output.lifecycle,view);
    }
    /** Shared lifecycle authority. Only the legacy adapter requests a Frame. */
    private record WorldExchange(Status status,Reason reason,Publication output) {
        WorldExchange {Objects.requireNonNull(status);Objects.requireNonNull(reason);if((status==Status.OK)!=(reason==Reason.NONE))throw invalid();}
    }
    private static WorldExchange exchange(Status status,Reason reason,Publication output) {
        return new WorldExchange(status,reason,output);
    }
    private static Reply reply(Status status,Reason reason,Frame frame) {return new Reply(status,reason,frame);}
    private Reply handle(TournamentPresence.Owner owner,Request request,ModernBattleActionProtocol.Request guard) {
        var result=handleInternal(owner,request,guard);return new Reply(result.status(),result.reason(),ModernQuestVisibility.world(result.output()==null?null:result.output().legacy().frame(),request.scope()));
    }
    private WorldExchange handleInternal(TournamentPresence.Owner owner,Request request,ModernBattleActionProtocol.Request guard) {
        return handleInternal(owner,request,guard,false);
    }
    private WorldExchange handleInternal(TournamentPresence.Owner owner,Request request,ModernBattleActionProtocol.Request guard,boolean fullVip) {
        Objects.requireNonNull(request);
        Registration registration;
        synchronized (gate) {
            requireOpen();registration=requireRegistration(owner,request.scope());
            if(registration.entry!=null&&registration.entry.mapping.fullVipEnabled()!=fullVip)
                throw new SecurityException("world route differs from selected publication");
        }
        if (request.action() == Action.PREPARE) return prepare(registration, request,fullVip);
        Entry entry;
        synchronized (gate) {
            requireCurrent(registration); entry = registration.entry;
            if (entry == null) {
                if (request.action() == Action.READ && request.matchId() == 0) return exchange(Status.OK, Reason.NONE, null);
                throw new SecurityException("world match unavailable for owner");
            }
            if (request.matchId() != 0 && (request.matchId() != entry.ticket.matchId() || request.generation() != entry.ticket.generation()))
                throw new SecurityException("world match generation mismatch");
        }
        if (request.action() == Action.LEAVE) {
            if (entry.mapping.race() != null) {
                // Only a race needs to retain a pending ranked result. Ordinary
                // combat must fence admission in terminate before waiting for
                // an already-admitted attack or an unpublished final hit.
                advance(entry);
                WorldExchange pendingRace = pendingRaceLeave(registration, entry);
                if (pendingRace != null) return pendingRace;
            }
            if(!withdrawTimeAttack(registration,entry)&&!withdrawRace(registration,entry,ModernPetRace.Dnf.LEFT))terminate(entry, AbortReason.SERVER_ABORT);
            // The fixed finish window may expire during withdrawal itself.
            if (entry.mapping.race() != null) {
                WorldExchange pendingRace = pendingRaceLeave(registration, entry);
                if (pendingRace != null) return pendingRace;
            }
            forgetFollower(registration, entry);
            return exchange(Status.OK, Reason.NONE, null);
        }
        if (request.action() == Action.READ) {
            advance(entry);
            return published(entry, () -> { requireCurrent(registration); return exchange(Status.OK, Reason.NONE, registration.entry == entry ? entry.output : null); });
        }
        ModernBattleEngine engine = published(entry, () -> {
            requireCurrent(registration);
            if (registration.entry != entry) throw new SecurityException("world match attachment changed");
            return entry.terminal ? null : entry.engine;
        });
        if (engine == null) return published(entry, () -> { requireCurrent(registration); return exchange(Status.DENIED, Reason.NOT_ACTIVE, entry.output); });
        if (request.action() == Action.LOADED) {
            var observed=engine.observeCommand(()->engine.ackLoadedObservation(owner, request.scope(), request.matchId(), request.generation(), request.manifestDigest()));
            publish(entry, engine, observed.observation());
            return published(entry, () -> {
                requireCurrent(registration);
                return entry.terminal ? exchange(Status.DENIED, Reason.NOT_ACTIVE, entry.output)
                        : exchange(Status.OK, Reason.NONE, entry.output);
            });
        }
        if (request.action() == Action.MOVE) {
            var observed=observeAction(entry,engine,owner,guard,()->engine.move(owner, request.scope(), request.matchId(), request.generation(), request.sequence(),
                    request.fromX(), request.fromY(), request.heading()));var result=observed.outcome();
            publish(entry, engine, observed.observation());
            return published(entry, () -> {
                requireCurrent(registration);
                if (entry.terminal) return exchange(Status.DENIED, Reason.NOT_ACTIVE, entry.output);
                Reason reason = switch (result.status()) {
                    case MOVED -> Reason.NONE; case NOT_ACTIVE, ABORTED -> Reason.NOT_ACTIVE;
                    case STALE_POSITION -> Reason.STALE_POSITION; case TOO_FAST -> Reason.TOO_FAST;
                    case BLOCKED -> Reason.BLOCKED; case OCCUPIED -> Reason.OCCUPIED;
                };
                return exchange(reason == Reason.NONE ? Status.OK : Status.DENIED, reason, entry.output);
            });
        }
        throw invalid();
    }

    private WorldExchange pendingRaceLeave(Registration registration, Entry entry) {
        return published(entry, () -> {
            requireCurrent(registration);
            if (registration.entry != entry) throw new SecurityException("world match attachment changed");
            return entry.output.lifecycle.race().complete() && entry.output.lifecycle.race().settlement() != ModernPetRaceProtocol.Settlement.SAVED
                    ? exchange(Status.BUSY, Reason.INVALID_STATE, entry.output) : null;
        });
    }

    private WorldExchange prepare(Registration registration, Request request,boolean fullVip) {
        if (journal != null && !journal.accepting()) return exchange(Status.UNAVAILABLE, Reason.CAPACITY, null);
        Entry forgotten = null;
        synchronized (gate) {
            requireCurrent(registration);
            if (registration.reserving) return exchange(Status.BUSY, Reason.INVALID_STATE, output(registration));
            // A previous result belongs to every follower until that follower
            // acknowledges LEAVE. One player's PREPARE must not erase it.
            if (entries.stream().anyMatch(entry -> resultOutstanding(entry) && !entry.followers.isEmpty()
                    && entry.participants.get(0).scope.channelId() == registration.scope.channelId()
                    && entry.ticket.snapshot().summary().id() == request.roomId()))
                return exchange(Status.BUSY, Reason.INVALID_STATE, output(registration));
            if (registration.entry != null) {
                if (resultOutstanding(registration.entry)) return exchange(Status.BUSY, Reason.INVALID_STATE, registration.entry.output);
                if (!registration.entry.terminal) return exchange(Status.BUSY, Reason.INVALID_STATE, registration.entry.output);
                forgotten = registration.entry;
                forgetEntryLocked(forgotten);
            }
        }
        closeForgotten(forgotten);
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(limits.preparationTimeoutMillis());
        synchronized (gate) {
            requireCurrent(registration);
            if (registration.reserving || registration.entry != null)
                return exchange(Status.BUSY, Reason.INVALID_STATE, output(registration));
            if (entries.size() + reservations >= limits.maxMatches() || !preparationAdmission.tryAcquire())
                return exchange(Status.BUSY, Reason.CAPACITY, output(registration));
            registration.reserving = true; reservations++;
        }
        ModernRooms.FrozenPreparation ticket = null;
        Entry accepted = null;
        ModernMatchSettlement.Reservation resultSlot = null;
        ModernPetRaceSettlement.Reservation raceSlot=null;
        boolean transferred = false;
        try {
            // All authoritative Rooms operations and shared-geometry work are
            // outside the service gate. A cancellation wins final publication.
            var capture = rooms.prepareWorld(registration.owner, registration.scope, request.roomId(), request.expectedRoomRevision());
            if (capture.ticket() == null) return rejectedCapture(capture.result());
            ticket = capture.ticket();
            var config = ticket.snapshot().summary().config();
            Mapping mapping = mappings.get(new ModernWorldBinding.Key(config.mode(), config.mapId()));
            if (mapping == null) return exchange(Status.UNAVAILABLE, Reason.NO_BINDING, null);
            if(mapping.fullVipEnabled()!=fullVip)return exchange(Status.UNAVAILABLE,Reason.NO_BINDING,null);
            if(config.capacity()>mapping.playerCapacity()
                    ||ticket.snapshot().members().size()>mapping.playerCapacity())return exchange(Status.UNAVAILABLE,Reason.CAPACITY,null);
            if(mapping.race()!=null&&!raceJournal.accepting())return exchange(Status.UNAVAILABLE,Reason.CAPACITY,null);
            var descriptor=ModernMatchDescriptor.of(mapping,limits,settlementStore!=null);
            if (!ticket.bots().isEmpty() && !mapping.serverBots()) return exchange(Status.UNAVAILABLE, Reason.NO_BINDING, null);
            List<Registration> participants = new ArrayList<>();
            List<ModernRooms.Participant> captured = ticket.participants();
            if (captured.isEmpty()) return exchange(Status.UNAVAILABLE, Reason.INVALID_STATE, null);
            synchronized (gate) {
                requireCurrent(registration);
                for (var participant : captured) {
                    var member = registrations.get(participant.owner());
                    if (member == null || !member.scope.equals(participant.scope()) || mapping.combatEnabled() && member.physical == null
                            || mapping.spellsEnabled() && member.skills == null
                            || mapping.damageMagic() && !member.damageMagicProtocol
                            || mapping.matchItems() && member.matchItems == null
                            || mapping.weightEnabled() && !member.weightProtocol
                            || mapping.timedEnabled() && !member.timedProtocol
                            || mapping.bowEnabled()&&!member.bowProtocol
                            || mapping.regenEnabled()&&!member.regenProtocol
                            || mapping.honor()!=null&&!member.honorProtocol
                            || mapping.fireStormEnabled()&&!member.fireStormProtocol
                            || descriptor.journaledResult() && !member.resultProtocol
                            || member!=null&&(!compatible(member,descriptor,config.mode())
                                ||!ModernLargeMaps.supported(mapping.binding().map().manifest(),member.protocol))
                            || member.reserving && member != registration
                            || member.entry != null && resultOutstanding(member.entry)
                            || member.entry != null && !member.entry.terminal)
                        return exchange(Status.UNAVAILABLE, Reason.INVALID_STATE, null);
                    participants.add(member);
                    var nativeSource=nativeCanonicalSources.get(mapping.binding().key());
                    if(nativeSource!=null)member.nativeLedger.requirePendingAttachment(member.owner,member.scope,
                            ticket.snapshot().summary().id(),request.expectedRoomRevision(),nativeSource,member.protocol);
                }
            }
            List<ModernRooms.Member> roster = ticket.snapshot().members();
            // Geometry/egress work is outside gate and ticket.guard(), shares the
            // existing admission deadline, and publishes nothing until complete.
            final var capturedTicket = ticket;
            var camps = mapping.spawnCamps();
            var obstacles = mapping.honor() == null ? Set.<ModernWorldMap.Cell>of()
                    : Set.copyOf(mapping.honor().towers().stream().map(ModernHonor.Tower::cell).toList());
            if(mapping.npc()!=null)obstacles=mapping.npc().reservedCells();
            int attacker=mapping.siege()==null?-1:ModernSiege.assignAttacker(()->Math.toIntExact(random.nextLong(2)));
            List<ModernWorldMap.Cell> spawns;
            if(mapping.siege()!=null){
                if(!capturedTicket.isLive()||System.nanoTime()-deadline>=0)throw new IllegalStateException("world preparation expired");
                siegeSpaces.get(mapping.binding().key()).require(mapping.binding(),mapping.siege());
                int[] used={0,0};var allocated=new ArrayList<ModernWorldMap.Cell>();
                for(var member:roster){var camp=mapping.siege().camp(member.team(),attacker);
                    if(used[member.team()]>=camp.size())return exchange(Status.UNAVAILABLE,Reason.CAPACITY,null);
                    allocated.add(camp.get(used[member.team()]++));}spawns=List.copyOf(allocated);
            }else spawns=ModernQuestSpace.allocate(mapping, roster,
                    obstacles, () -> !capturedTicket.isLive() || System.nanoTime() - deadline >= 0);
            ModernPetRaceProtocol.Snapshot initialRace=ModernPetRaceProtocol.Snapshot.disabled();
            if(mapping.race()!=null) {
                if(roster.size()<2||roster.size()>10||!ticket.bots().isEmpty())return exchange(Status.UNAVAILABLE,Reason.CAPACITY,null);
                var course=mapping.race();ModernPetRace.qualify(mapping.binding().key(),mapping.binding().map(),course.map().manifest(),course.gates(),course.baseGraphicsId(),course.stepIntervalNanos(),spawns,
                        ()->!capturedTicket.isLive()||System.nanoTime()-deadline>=0);
                initialRace=ModernPetRaceProtocol.fromState(ModernPetRace.initial(ticket.matchId(),ticket.generation(),course,
                        roster.stream().map(m->m.identity().humanKey()).toList(),spawns,descriptor.activeTimeoutMillis()),null);
            }
            List<ModernBattleEngine.Actor> actors = new ArrayList<>();
            for (int index = 0; index < roster.size(); index++) actors.add(new ModernBattleEngine.Actor(index + 1L,
                    roster.get(index), spawns.get(index), 0, false, 0, mapping.stepIntervalNanos()));
            Frame initial = new Frame(ticket.matchId(), ticket.generation(), 1, Phase.PREPARING, AbortReason.NONE,
                    ticket.snapshot(), mapping.binding().map().manifest(), actors, null,
                    mapping.spellsEnabled() ? actors.stream().map(a -> ModernSpellProtocol.CastState.initial(a.entityId(),
                            Math.toIntExact(mapping.castIntervalNanos() / 1_000_000))).toList() : List.of(),descriptor,initialRace,-1,
                            ModernQuestProtocol.initial(mapping.npc()==null?null:mapping.npc().questPlan(),actors),
                            mapping.timeAttack()==null?null:ModernTimeAttackProtocol.preparing(mapping.timeAttack(),
                                    Math.toIntExact(actors.stream().filter(a->a.member().identity().equals(ticketOwner(capturedTicket))).findFirst().orElseThrow().entityId()),actors.size()),
                            mapping.siege()==null?null:ModernSiegeProtocol.preparing(new ModernSiege.Scope(ticket.snapshot().summary().id(),ticket.matchId(),ticket.generation()),mapping.siege(),attacker,actors.size()));
            // Reserve durable capacity outside service/Rooms/Engine locks and
            // before a client can observe admission to a result-bearing match.
            if (System.nanoTime() - deadline >= 0 || !ticket.isLive()) throw new IllegalStateException("world preparation expired");
            if (journal != null && descriptor.journaledResult()) resultSlot = journal.reserve(ticket.matchId(), ticket.generation());
            if(mapping.race()!=null)raceSlot=raceJournal.reserve(ticket.matchId(),ticket.generation());
            accepted = new Entry(ticket, mapping, participants, spawns, deadline, initial, resultSlot,raceSlot);
            accepted.job = new PreparationJob(accepted);
            var initialOutput=accepted.output;
            List<Entry> oldEntries = new ArrayList<>();
            try (var ignored = ticket.guard()) {
                synchronized (gate) {
                    if (System.nanoTime() - deadline >= 0) throw new IllegalStateException("world preparation expired");
                    requireCurrent(registration);
                    for (Registration row : participants) {
                        if (registrations.get(row.owner) != row || row.entry != null && (!row.entry.terminal || resultOutstanding(row.entry)))
                            throw new IllegalStateException("world participant changed before publication");
                        var nativeSource=nativeCanonicalSources.get(mapping.binding().key());
                        if(nativeSource!=null)row.nativeLedger.requirePendingAttachment(row.owner,row.scope,
                                ticket.snapshot().summary().id(),request.expectedRoomRevision(),nativeSource,row.protocol);
                    }
                    for (Registration row : participants) {
                        if (row.entry != null && !oldEntries.contains(row.entry)) { oldEntries.add(row.entry); forgetEntryLocked(row.entry); }
                    }
                    entries.add(accepted); for (Registration row : participants) {
                        var nativeSource=nativeCanonicalSources.get(mapping.binding().key());
                        if(nativeSource!=null)row.nativeLedger.attachPreparedWorld(row.owner,row.scope,
                                new ModernTargetHistory.World(ticket.snapshot().summary().id(),ticket.matchId(),ticket.generation()),nativeSource,row.protocol);
                        row.entry = accepted;
                    }
                    reservations--; registration.reserving = false; transferred = true;
                }
            }
            for (Entry old : oldEntries) closeForgotten(old);
            if (!submit(accepted)) {
                terminate(accepted, AbortReason.PREPARATION_FAILED); forgetEntry(accepted);
                return exchange(Status.BUSY, Reason.CAPACITY, null);
            }
            return exchange(Status.OK, Reason.NONE, initialOutput);
        } catch (SecurityException failure) { throw failure; }
        catch (Exception failure) {
            if (accepted != null && transferred) { terminate(accepted, AbortReason.PREPARATION_FAILED); forgetEntry(accepted); }
            return exchange(Status.UNAVAILABLE, Reason.PREPARATION_FAILED, null);
        } finally {
            if (!transferred) {
                if (resultSlot != null) resultSlot.close();
                if(raceSlot!=null)raceSlot.close();
                // A failed capture still occupies admission until its real
                // cancellation finishes, not merely until cancellation starts.
                if (ticket != null) releaseTicket(ticket);
                synchronized (gate) { registration.reserving = false; reservations--; }
                preparationAdmission.release();
            }
        }
    }

    private boolean submit(Entry entry) {
        boolean retire = false;
        synchronized (gate) {
            if (closed || entry.cancelled.get()) retire = true;
            else try { entry.jobState = JobState.QUEUED; workers.execute(entry.job); return true; }
            catch (RejectedExecutionException rejected) { retire = true; }
        }
        if (retire) finishWorker(entry);
        return false;
    }
    private void prepareWorker(Entry entry) {
        synchronized (gate) {
            if (entry.jobState == JobState.RETIRED) return;
            entry.jobState = JobState.RUNNING; entry.worker = Thread.currentThread();
        }
        ModernBattleEngine candidate = null;
        try {
            List<ModernBattleEngine.PreparedMember> prepared = new ArrayList<>();
            var roster = entry.ticket.snapshot().members();
            var positions = new HashMap<ModernParticipantId, ModernWorldMap.Cell>();
            for (int index = 0; index < roster.size(); index++)
                positions.put(roster.get(index).identity(), entry.spawns.get(index));
            for (int index = 0; index < entry.participants.size(); index++) {
                requirePreparing(entry);
                Registration row = entry.participants.get(index);
                var fighter = entry.mapping.fullVipEnabled()
                        ?Objects.requireNonNull(((FullVipReader)row.physical).readVip(entry.deadline,entry.mapping.armorProtectionEnabled()))
                        :entry.mapping.combatEnabled()?Objects.requireNonNull(row.physical.read(entry.deadline,entry.mapping.armorProtectionEnabled())):null;
                if((fighter!=null&&fighter.vip()!=null)!=entry.mapping.fullVipEnabled())throw new IllegalStateException("VIP source differs from selected world");
                // Capability/source opt-in cannot replace fresh, same-bundle
                // qualification; reject both missing and unsolicited protection.
                if((fighter!=null&&fighter.armorProtection()!=null)!=entry.mapping.armorProtectionEnabled())
                    throw new IllegalStateException("armor protection source differs from selected world");
                var fresh = fighter == null ? row.reader.read(entry.deadline) : fighter.profile().source().character();
                requirePreparing(entry);
                var skills = entry.mapping.spellsEnabled() ? Objects.requireNonNull(row.skills.read(entry.deadline)) : null;
                requirePreparing(entry);
                var disintegrate=entry.mapping.disintegrateEnabled()||entry.mapping.npcDisintegrateEnabled()?Objects.requireNonNull(row.disintegrate.read(entry.deadline)):null;
                requirePreparing(entry);
                var greaterHaste=entry.mapping.greaterHasteEnabled()?Objects.requireNonNull(row.greaterHaste.read(entry.deadline)):null;
                requirePreparing(entry);
                var haste=entry.mapping.hasteEnabled()?Objects.requireNonNull(row.haste.read(entry.deadline)):null;
                requirePreparing(entry);
                var slow=entry.mapping.slowEnabled()?Objects.requireNonNull(row.slow.read(entry.deadline)):null;
                if(slow!=null&&!slow.key().equals(Objects.requireNonNull(fighter).key()))throw invalid();
                requirePreparing(entry);
                var basis=entry.mapping.weightEnabled()?ModernInventoryWeight.qualify(Objects.requireNonNull(fighter).profile(),weightRate(row.scope.sourceId())):null;
                var matchItems=entry.mapping.matchItems()?Objects.requireNonNull(row.matchItems.capture(entry.ticket.matchId(),entry.ticket.generation(),entry.deadline,basis,entry.descriptor)):null;
                if(matchItems!=null)for(var stack:matchItems) {
                    if(stack.offer().timed()!=null&&!entry.mapping.timedEnabled() || stack.offer().ammo()!=null&&!entry.mapping.bowEnabled() || stack.offer().material()!=null&&!entry.mapping.slowEnabled())throw invalid();
                }
                requirePreparing(entry);
                var spawn = Objects.requireNonNull(positions.get(ModernParticipantId.human(row.scope.sourceId(), row.scope.objectId())));
                var member=new ModernBattleEngine.PreparedMember(row.owner, row.scope, fresh, spawn,
                        entry.mapping.stepIntervalNanos(), fighter, entry.mapping.attackIntervalNanos(), skills, entry.mapping.castIntervalNanos(), entry.mapping.damageMagic(),slow==null?matchItems:matchItems.stream().filter(stack->stack.offer().material()==null).toList(),basis,entry.mapping.timedEnabled(),entry.mapping.bowEnabled(),entry.mapping.regenEnabled(),entry.mapping.fireStormEnabled(),entry.mapping.disintegrateEnabled()?disintegrate:null,
                        null,null,null,null,false,entry.mapping.fullVipEnabled())
                        .withNpcDisintegrate(entry.mapping.npcDisintegrateEnabled()?disintegrate:null).withGreaterHaste(greaterHaste).withHaste(haste);
                if(slow!=null)member=member.withSlow(slow,matchItems);
                prepared.add(entry.mapping.playerHealingEnabled()?member.withPlayerHealing():member);
            }
            List<ModernBattleEngine.BotPrepared> bots = new ArrayList<>();
            for (var member : entry.ticket.bots()) {
                requirePreparing(entry);
                if (!entry.mapping.serverBots()) throw new IllegalStateException("server bots were not enabled");
                bots.add(new ModernBattleEngine.BotPrepared(member.identity(),
                        ModernBotTemplate.roomTestDummy(member.characterClass(), member.sex()),
                        Objects.requireNonNull(positions.get(member.identity()))));
            }
            requirePreparing(entry);
            candidate = new ModernBattleEngine(entry.ticket, entry.mapping.binding(), prepared, bots, entry.descriptor.engineLimits(),
                    System::nanoTime, System::currentTimeMillis, random, entry.descriptor.settlementEnabled(),entry.mapping.honor(),
                    () -> entry.cancelled.get() || System.nanoTime() - entry.deadline >= 0,entry.mapping.race(),
                    entry.mapping.npc()==null?null:entry.mapping.npc().capture(prepared),entry.mapping.siege(),
                    entry.frame().siege()==null?-1:entry.frame().siege().snapshot().state().attacker(),siegeSpaces.get(entry.mapping.binding().key()));
            var observation=candidate.serviceObservation();
            var preparedPublication=publication(entry,observation);
            requirePreparing(entry);
            boolean terminal;
            try (var ignored = entry.ticket.guard()) {
                synchronized (gate) {
                    if (closed || entry.cancelled.get() || entry.terminal || !entry.retained || !entries.contains(entry))
                        throw new IllegalStateException("world preparation retired");
                    if(nativeCanonicalSources.containsKey(entry.mapping.binding().key())) {
                        for(var row:entry.participants) {
                            if(registrations.get(row.owner)!=row||row.entry!=entry)throw new SecurityException("Native worker owner retired");
                            row.nativeLedger.requireAttachment(row.owner,row.scope,
                                    new ModernTargetHistory.World(entry.ticket.snapshot().summary().id(),entry.ticket.matchId(),entry.ticket.generation()),
                                    nativeCanonicalSources.get(entry.mapping.binding().key()));
                        }
                    }
                    assignPublication(entry,preparedPublication);
                    entry.engine = candidate;
                    terminal = observation.phase() == ModernBattleEngine.Phase.ABORTED;
                    candidate = null;
                }
            }
            if (terminal) terminate(entry, AbortReason.valueOf(observation.abortReason().name()));
        } catch (Exception failure) {
            terminate(entry, System.nanoTime() - entry.deadline >= 0 ? AbortReason.LOAD_TIMEOUT : AbortReason.PREPARATION_FAILED);
        } finally {
            if (candidate != null) candidate.close();
            // This worker owns its cancellation interruption, not the next job.
            Thread.interrupted();
        }
    }
    private void requirePreparing(Entry entry) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("world preparation cancelled");
        if (System.nanoTime() - entry.deadline >= 0 || entry.cancelled.get() || !entry.ticket.isLive())
            throw new IllegalStateException("world preparation expired");
        synchronized (gate) {
            if (closed || entry.terminal || !entries.contains(entry)) throw new IllegalStateException("world preparation unavailable");
            for (Registration row : entry.participants) {
                if (registrations.get(row.owner) != row || row.entry != entry)throw new IllegalStateException("world participant changed");
                var source=nativeCanonicalSources.get(entry.mapping.binding().key());
                if(source!=null)row.nativeLedger.requireAttachment(row.owner,row.scope,
                        new ModernTargetHistory.World(entry.ticket.snapshot().summary().id(),entry.ticket.matchId(),entry.ticket.generation()),source);
            }
        }
    }
    private void finishWorker(Entry entry) {
        boolean release = false;
        synchronized (gate) {
            if (!entry.retired) {
                entry.retired = true; entry.worker = null; entry.jobState = JobState.RETIRED;
                removeRetiredEntry(entry);
                release = true;
            }
        }
        if (release) preparationAdmission.release();
    }

    private void scheduledTick() {
        try { tickNow(); }
        catch (RuntimeException failure) { cleanupFailed = true; /* No private identity/exception logging. */ }
    }
    /** Also useful for a server-owned deterministic pump; no client time is accepted. */
    void tickNow() {
        List<Entry> current;
        synchronized (gate) { if (closed) return; current = List.copyOf(entries); }
        for (Entry entry : current) {
            try { advance(entry); }
            catch (RuntimeException failure) {
                boolean alreadyFailed;
                synchronized (gate) { alreadyFailed = entry.terminationFailed; }
                if (!alreadyFailed) terminate(entry, AbortReason.SERVER_ABORT);
            }
        }
    }
    private void advance(Entry entry) {
        ModernBattleEngine engine;
        boolean terminal;
        synchronized (gate) { if (!entry.retained) return; terminal = entry.terminal; engine = entry.engine; }
        if (terminal) { refreshReceipt(entry); return; }
        if (!entry.ticket.isLive()) { terminate(entry, AbortReason.PREPARATION_CANCELLED); return; }
        if (engine == null) {
            if (System.nanoTime() - entry.deadline >= 0) terminate(entry, AbortReason.LOAD_TIMEOUT);
        } else if(entry.mapping.npc()!=null)advanceNpcs(entry,engine);
        else publish(entry, engine, engine.serviceObservation());
    }
    private static ModernNpcProtocol.Frame npcFrame(Entry entry,ModernNpcBattle.Frame snapshot) {
        return npcFrame(entry,snapshot,entry.cooperativeResult);
    }
    private static ModernNpcProtocol.Frame npcFrame(Entry entry,ModernNpcBattle.Frame snapshot,ModernMatchSettlement.Event cooperative) {
        var frame=entry.mapping.timeAttack()!=null?ModernTimeAttackProtocol.frame(snapshot,entry.descriptor,snapshot.world().timeAttack().finished()?cooperative:null,
                SettlementState.PENDING,entry.npcResources):ModernNpcProtocol.Frame.from(snapshot,entry.descriptor).withResources(entry.npcResources);
        return frame.withNames(entry.npcNames);
    }
    private void advanceNpcs(Entry entry,ModernBattleEngine engine) {
        // One existing service tick owner, no new executor or client-controlled
        // catch-up count. The per-entry gate only surrounds Engine operations.
        ModernBattleObservation.Capture observation;
        synchronized(entry.npcGate) {
            observation=engine.serviceObservation();
            if(observation.phase()==ModernBattleEngine.Phase.ACTIVE&&!entry.cancelled.get()) {
                var setup=entry.mapping.npc();
                if(!entry.npcStarted) {
                    if(setup.questPlan()==null&&setup.timeAttack()==null) {
                        var scope=observation.npcs().roster().scope();
                        observation=engine.observeCommand(()->engine.spawnNpcsObservation(scope,setup.initial())).observation();
                    }
                    entry.npcStarted=true;
                }
                boolean night=setup.night().getAsBoolean();
                for(var actor:observation.npcs().roster().actors()) {
                    if(entry.cancelled.get())break;
                    if(actor.alive())engine.stepNpcUnpublished(actor.id(),actor.life(),setup.pathBudget(),night);
                }
                // No client consumes the per-NPC intermediate captures. Keep
                // each step separately locked, then publish one atomic state.
                observation=engine.npcTickObservation();
            }
        }
        publish(entry,engine,observation);
    }
    private void publish(Entry entry, ModernBattleEngine engine, ModernBattleObservation.Capture observation) {
        if (observation.phase() == ModernBattleEngine.Phase.FINISHED) {
            terminate(entry, AbortReason.SERVER_ABORT); return;
        }
        if (observation.phase() != ModernBattleEngine.Phase.ABORTED) {
            final boolean clockOnly;
            synchronized(gate){
                if(closed||entry.terminal||entry.engine!=engine||!entry.retained||observation.revision()<entry.engineRevision)return;
                clockOnly=observation.revision()==entry.engineRevision;
                if(clockOnly && entry.participants.stream().noneMatch(p->ModernBattleReadProtocol.negotiated(p.protocol)))return;
            }
            if(clockOnly) {
                // Engine observations can advance clocks without an event:
                // waiting/countdown, full HP/MP regen, and race/time watches.
                // Do not rewrite the legacy same-revision caches or fabricate
                // an engine event just to deliver current canonical time.
                var next=authority(entry,observation);
                try(var ignored=entry.ticket.guard()){publishObservationClock(entry,engine,next);}
                catch(IllegalStateException failure){
                    if(!entry.ticket.isLive())terminate(entry,AbortReason.PREPARATION_CANCELLED);
                    else throw failure;
                }
                return;
            }
            var next=publication(entry,observation);
            try (var ignored = entry.ticket.guard()) { publishLive(entry, engine, next); }
            catch (IllegalStateException failure) {
                if (!entry.ticket.isLive()) terminate(entry, AbortReason.PREPARATION_CANCELLED);
                else throw failure;
            }
            return;
        }
        terminate(entry, AbortReason.valueOf(observation.abortReason().name()));
    }
    private void publishObservationClock(Entry entry,ModernBattleEngine engine,ModernBattlePublication next) {
        synchronized(gate) {
            if(closed||entry.terminal||entry.engine!=engine||!entry.retained||entry.publication==null
                    ||next.world().revision()!=entry.engineRevision)return;
            var previous=entry.publication;
            if(!next.world().identity().equals(previous.world().identity())
                    ||!next.world().room().equals(previous.world().room())
                    ||!next.world().manifest().equals(previous.world().manifest()))throw invalid();
            var before=previous.world().clock();var after=next.world().clock();
            if(after.loadingElapsedNanos()<=before.loadingElapsedNanos()
                    ||after.activeElapsedNanos()<before.activeElapsedNanos())return;
            var candidate=entry.output.withClock(next);
            assignPublication(entry,candidate);
        }
    }
    private void publishLive(Entry entry, ModernBattleEngine engine, Publication next) {
        synchronized (gate) {
            if (closed || entry.terminal || entry.engine != engine || !entry.retained || next.authority().world().revision() <= entry.engineRevision) return;
            assignPublication(entry,next);
        }
    }
    private void terminate(Entry entry, AbortReason reason) {
        Thread interrupt;
        boolean removed = false, first;
        synchronized (gate) {
            first = !entry.terminal;
            if (first) {
                entry.terminal = true; entry.cancelled.set(true);
                entry.terminationOwner = Thread.currentThread();
            }
            interrupt = entry.worker;
            if (first && entry.jobState == JobState.QUEUED) removed = workers.remove(entry.job);
        }
        if (!first) { awaitTerminalPublication(entry); return; }
        try {
            if (interrupt != null && interrupt != Thread.currentThread()) interrupt.interrupt();
            if (removed) finishWorker(entry);
            // First fence future commits and wait for any already-admitted
            // Engine Guard. Cached service data is NOT an authoritative terminal.
            releaseEntryTicket(entry);
            ModernBattleEngine engine;
            synchronized (gate) { engine = entry.engine; }
            var observation=engine==null?null:engine.serviceObservation();
            final Publication previous;
            synchronized(gate){previous=entry.output;}
            var exact=observation==null?previous:publication(entry,observation);
            var finalPublication=exact.withLifecycle(exact.lifecycle.terminal(exact.authority(),previous.lifecycle.revision(),reason));
            var result=finalPublication.lifecycle.result();
            if (entry.settlementReservation != null) {
                var event=result;
                if (event != null) {
                    try {
                        entry.settlementReservation.record(event, System.nanoTime()
                                + TimeUnit.MILLISECONDS.toNanos(limits.preparationTimeoutMillis()));
                    } catch (Exception failure) {
                        if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                        throw new JournalPublicationException();
                    }
                } else entry.settlementReservation.close();
            }
            if(entry.raceReservation!=null) {
                if(finalPublication.lifecycle.race().complete()) {
                    try{entry.raceReservation.record(ModernPetRaceSettlement.event(finalPublication.authority().capture(),finalPublication.lifecycle.race(),System.currentTimeMillis()),System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(limits.preparationTimeoutMillis()));}
                    catch(Exception failure){if(failure instanceof InterruptedException)Thread.currentThread().interrupt();throw new JournalPublicationException();}
                }else entry.raceReservation.close();
            }
            synchronized (gate) {
                assignPublication(entry,finalPublication);
            }
            // Admission precedes the terminal latch: shutdown/LEAVE cannot
            // acknowledge completion and close the executor before this event
            // has been submitted (or explicitly marked unconfirmed).
            // Pure per-selection callbacks, outside gate/Engine/Rooms/Guard.
            // A tombstone prevents a slow captured source query from reviving
            // this match after terminal publication or giving a second allowance.
            if(entry.mapping.matchItems())for(var row:entry.participants)row.matchItems.terminal(entry.ticket.matchId(),entry.ticket.generation());
            submitSettlement(entry);
            refreshRaceReceipt(entry);
        } catch (RuntimeException failure) {
            synchronized (gate) { entry.terminationFailed = true; }
            // A failed durable publication prevents all replies for this entry,
            // but is not evidence that a still-owned worker can never retire.
            if (!(failure instanceof JournalPublicationException)) cleanupFailed = true;
            throw failure;
        } finally {
            entry.terminationDone.countDown();
            synchronized (gate) { removeRetiredEntry(entry); }
        }
        boolean orphan; synchronized(gate){orphan=(entry.mapping.race()!=null||entry.mapping.timeAttack()!=null)&&entry.followers.isEmpty();}
        if(orphan)forgetEntry(entry); // Durable result is now owned by its journal, not by a disconnected UI.
    }
    private void submitSettlement(Entry entry) {
        if (journal != null) { refreshReceipt(entry); return; }
        ModernMatchSettlement.Event event;
        synchronized (gate) {
            if (entry.output.lifecycle.result() == null || entry.settlementQueued) return;
            entry.settlementQueued = true; event = entry.output.lifecycle.result(); settlementTasks++;
        }
        try {
            settlementWorkers.execute(() -> {
                try {
                SettlementState state = SettlementState.FAILED;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                for (int attempt = 0; attempt < 3; attempt++) {
                    try {
                        settlementStore.persist(event, deadline);
                        state = SettlementState.SAVED; break;
                    } catch (Exception failure) {
                        // Retry only explicit transient/uncertain results, with
                        // the exact event/UUID and the original absolute deadline.
                        // No retry changes the winner or counts a lookup miss as
                        // proof of rollback. Crash recovery still needs an outbox.
                        if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                        boolean retry = failure instanceof ModernMatchSettlement.SettlementException settlement
                                && (settlement.failure() == ModernMatchSettlement.Failure.COMMIT_UNKNOWN
                                    || settlement.failure() == ModernMatchSettlement.Failure.UNAVAILABLE);
                        if (!retry || attempt == 2 || Thread.currentThread().isInterrupted()
                                || deadline - System.nanoTime() <= TimeUnit.MILLISECONDS.toNanos(50)) break;
                        java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
                        if (Thread.currentThread().isInterrupted()) break;
                    }
                }
                publishSettlement(entry, state);
                } finally { synchronized (gate) { settlementTasks--; } }
            });
        } catch (RejectedExecutionException unavailable) {
            try { publishSettlement(entry, SettlementState.FAILED); }
            finally { synchronized (gate) { settlementTasks--; } }
        }
    }
    private void publishSettlement(Entry entry, SettlementState state) {
        if (state == SettlementState.PENDING) return;
        synchronized (gate) {
            assignPublication(entry,entry.output.withLifecycle(entry.output.lifecycle.settled(state)));
        }
    }
    private void refreshReceipt(Entry entry) {
        if (entry.settlementReservation != null)
            publishSettlement(entry, SettlementState.valueOf(entry.settlementReservation.state().name()));
        refreshRaceReceipt(entry);
    }
    private void refreshRaceReceipt(Entry entry) {
        if(entry.raceReservation==null)return;var state=entry.raceReservation.state();
        if(state!=ModernPetRaceProtocol.Settlement.SAVED&&state!=ModernPetRaceProtocol.Settlement.FAILED)return;
        synchronized(gate){assignPublication(entry,entry.output.withLifecycle(entry.output.lifecycle.raceSettled(state)));}
    }
    private static final class JournalPublicationException extends IllegalStateException {
        JournalPublicationException() { super("world result local recording was not confirmed"); }
    }
    private void awaitTerminalPublication(Entry entry) {
        synchronized (gate) {
            if (!entry.terminal || entry.terminationOwner == Thread.currentThread() && entry.terminationDone.getCount() != 0) return;
        }
        boolean interrupted = false;
        while (entry.terminationDone.getCount() != 0) {
            try { entry.terminationDone.await(); }
            catch (InterruptedException ignored) { interrupted = true; }
        }
        if (interrupted) Thread.currentThread().interrupt();
        synchronized (gate) { if (entry.terminationFailed) throw new IllegalStateException("world terminal publication failed"); }
    }
    /** Publication check and cache access are atomic; a late cancellation fence restarts outside gate. */
    private <T> T published(Entry entry, Supplier<T> read) {
        for (;;) {
            awaitTerminalPublication(entry);
            synchronized (gate) {
                if (entry.terminal && entry.terminationDone.getCount() != 0) continue;
                if (entry.terminationFailed) throw new IllegalStateException("world terminal publication failed");
                return read.get();
            }
        }
    }

    void detach(TournamentPresence.Owner owner) {
        Registration registration; Entry entry;
        synchronized (gate) {
            registration = registrations.remove(owner); if (registration == null) return;
            registration.nativeLedger.retire();
            entry = registration.entry;
        }
        if (entry != null) {
            try { if(!withdrawTimeAttack(registration,entry)&&!withdrawRace(registration,entry,ModernPetRace.Dnf.DISCONNECTED))terminate(entry, AbortReason.DISCONNECTED); }
            finally { forgetFollower(registration, entry); }
        }
    }
    private boolean withdrawRace(Registration registration,Entry entry,ModernPetRace.Dnf reason) {
        if (entry.mapping.race() == null) return false;
        ModernBattleEngine engine; boolean terminal;
        synchronized(gate){engine=entry.engine;terminal=entry.terminal;}
        // A withdrawal can itself cross the finish deadline. Its owner has
        // already retired from the engine while the service keeps the pending
        // receipt; a later SAVED acknowledgement must not withdraw it twice.
        if (terminal) { awaitTerminalPublication(entry); return true; }
        if(engine==null||!engine.individualRaceExit())return false;
        var observed=engine.observeCommand(()->engine.withdrawRacerObservation(registration.owner,reason));
        rooms.raceParticipantWithdrawn(registration.owner,entry.ticket); // No Rooms call while Engine/Guard/service gate is held.
        publish(entry,engine,observed.observation());return true;
    }
    private boolean withdrawTimeAttack(Registration registration,Entry entry) {
        if(entry.mapping.timeAttack()==null)return false;
        ModernBattleEngine engine;boolean terminal;
        synchronized(gate){engine=entry.engine;terminal=entry.terminal;}
        if(terminal){awaitTerminalPublication(entry);return true;}
        if(engine==null||!engine.individualTimeAttackExit())return false;
        var observed=engine.observeCommand(()->engine.withdrawTimeAttackObservation(registration.owner));
        // Must retire Engine ownership before Rooms permits a leave/disconnect.
        rooms.timeAttackParticipantWithdrawn(registration.owner,entry.ticket);
        publish(entry,engine,observed.observation());return true;
    }
    private void forgetFollower(Registration registration, Entry entry) {
        ModernBattleEngine close = null;
        synchronized (gate) {
            if (registration.entry == entry) {registration.entry = null;registration.nativeLedger.retire();}
            entry.followers.remove(registration);
            if (entry.followers.isEmpty()&&(entry.mapping.race()==null&&entry.mapping.timeAttack()==null||entry.terminal)) { forgetEntryLocked(entry); close = entry.engine; }
        }
        if (close != null) close.close();
        if (close != null && entry.settlementReservation != null) entry.settlementReservation.close();
        if(close!=null&&entry.raceReservation!=null)entry.raceReservation.close();
    }
    private void forgetEntry(Entry entry) {
        synchronized (gate) { forgetEntryLocked(entry); }
        closeForgotten(entry);
    }
    private void forgetEntryLocked(Entry entry) {
        entry.retained = false;
        for (Registration row : entry.followers) if (row.entry == entry) {row.entry = null;row.nativeLedger.retire();}
        entry.followers.clear();
        removeRetiredEntry(entry);
    }
    private void closeForgotten(Entry entry) {
        if (entry != null) {
            try { awaitTerminalPublication(entry); }
            finally {
                if (entry.engine != null) entry.engine.close();
                if (entry.settlementReservation != null) entry.settlementReservation.close();
                if(entry.raceReservation!=null)entry.raceReservation.close();
            }
        }
    }
    private void releaseEntryTicket(Entry entry) {
        Thread current = Thread.currentThread();
        if (entry.ticketReleased.compareAndSet(false, true)) {
            entry.releaseOwner.set(current);
            try { releaseTicket(entry.ticket); }
            finally {
                entry.releaseDone.countDown();
                synchronized (gate) { removeRetiredEntry(entry); }
            }
        } else if (entry.releaseOwner.get() != current) {
            // Invalidation may be queued behind an admitted Engine Guard. A
            // concurrent LEAVE/detach cannot acknowledge room release before
            // that real authority retirement finishes. Never wait under gate.
            boolean interrupted = false;
            while (entry.releaseDone.getCount() != 0) {
                try { entry.releaseDone.await(); }
                catch (InterruptedException ignored) { interrupted = true; }
            }
            if (interrupted) current.interrupt();
        }
    }
    private void releaseTicket(ModernRooms.FrozenPreparation ticket) {
        try { rooms.releasePreparation(ticket); }
        catch (RuntimeException failure) { cleanupFailed = true; }
    }
    /** Only called with gate held; cancellation acknowledgement is not final frame/engine retirement. */
    private void removeRetiredEntry(Entry entry) {
        if (!entry.retained && entry.retired && entry.releaseDone.getCount() == 0
                && (!entry.terminal || entry.terminationDone.getCount() == 0)) entries.remove(entry);
    }

    @Override public void close() {
        List<Entry> current;
        synchronized (gate) {
            if (closed) return; closed = true; current = List.copyOf(entries);
            for(var row:registrations.values())row.nativeLedger.retire();registrations.clear();
        }
        scheduler.shutdown();
        try {
            for (Entry entry : current) {
                try { terminate(entry, AbortReason.SERVER_ABORT); }
                catch (RuntimeException failure) { /* Publication is fail-closed; still retire every owned entry. */ }
                try { forgetEntry(entry); }
                catch (RuntimeException failure) { /* closeForgotten finally closes engine and result reservation. */ }
            }
        } finally {
            workers.shutdown();
            if (settlementWorkers != null) settlementWorkers.shutdown();
            if (journal != null) journal.close();
            if(raceJournal!=null)raceJournal.close();
        }
    }
    boolean awaitTermination(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0 || timeoutMillis > 60_000) throw invalid();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        if (!scheduler.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) return false;
        if (!workers.awaitTermination(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) return false;
        if (settlementWorkers != null && !settlementWorkers.awaitTermination(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) return false;
        if (journal != null && !journal.awaitTermination(Math.max(0, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())))) return false;
        if(raceJournal!=null&&!raceJournal.awaitTermination(Math.max(0,TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime()))))return false;
        synchronized (gate) { return closed && reservations == 0 && settlementTasks == 0 && entries.isEmpty() && !cleanupFailed; }
    }
    int active() { synchronized (gate) { return (int) entries.stream().filter(entry -> entry.jobState == JobState.RUNNING).count(); } }
    int queued() { synchronized (gate) { return (int) entries.stream().filter(entry -> entry.jobState == JobState.QUEUED).count(); } }
    int matchCount() { synchronized (gate) { return entries.size() + reservations; } }
    int registrationCount() { synchronized (gate) { return registrations.size(); } }
    boolean isQuiescent() {
        boolean persistenceRetired = (journal == null || journal.isQuiescent())&&(raceJournal==null||raceJournal.isQuiescent());
        synchronized (gate) { return entries.isEmpty() && reservations == 0 && settlementTasks == 0 && !cleanupFailed && persistenceRetired; }
    }
    private Registration requireOrdinaryRegistration(TournamentPresence.Owner owner,ModernRooms.Scope scope) {
        var row=requireRegistration(owner,scope);
        requireNativeVerified(row);
        if(row.entry!=null&&row.entry.mapping.fullVipEnabled())throw new SecurityException("legacy command cannot target a full VIP world");
        return row;
    }
    private Registration requireRegistration(TournamentPresence.Owner owner, ModernRooms.Scope scope) {
        // Ownership is checked independently of the payload/feature selection.
        Registration value = registrations.get(owner);
        if (value == null || !value.scope.equals(scope)) throw new SecurityException("world owner scope is not registered");
        return value;
    }
    private void requireCurrent(Registration row) {
        requireOpen(); if (registrations.get(row.owner) != row) throw new SecurityException("world owner registration changed");
    }
    private void requireActionCurrent(Registration row) {requireCurrent(row);requireNativeVerified(row);}
    private void requireNativeVerified(Registration row) {
        if(row.entry==null)return;
        var source=nativeCanonicalSources.get(row.entry.mapping.binding().key());if(source==null)return;
        var ticket=row.entry.ticket;
        row.nativeLedger.requireVerified(row.owner,row.scope,
                new ModernTargetHistory.World(ticket.snapshot().summary().id(),ticket.matchId(),ticket.generation()),source);
    }
    private void requireOpen() { if (closed) throw new IllegalStateException("world service closed"); }
    private static Publication output(Registration registration) { return registration.entry == null ? null : registration.entry.output; }
    /** Called only under gate. Terminal intent alone is not a published abort/result. */
    private static boolean resultOutstanding(Entry entry) {
        return entry.output.lifecycle.resultOutstanding() || entry.terminal && entry.terminationDone.getCount() != 0;
    }
    private static ModernParticipantId ticketOwner(ModernRooms.FrozenPreparation ticket){var key=ticket.snapshot().owner();return ModernParticipantId.human(key.sourceId(),key.objectId());}
    private static Frame fromEngine(ModernBattleEngine.Snapshot snapshot,long revision,Entry entry) {
        return fromEngine(snapshot,revision,entry,entry.cooperativeResult,entry.siegeResult);
    }
    private static Frame fromEngine(ModernBattleEngine.Snapshot snapshot,long revision,Entry entry,ModernMatchSettlement.Event cooperativeEvent,ModernMatchSettlement.Event siege) {
        requireDisintegrateDescriptor(snapshot,entry.descriptor);
        if(entry.mapping.siege()!=null)return ModernSiegeProtocol.frame(snapshot,revision,entry.descriptor,
                snapshot.siege().finished()?siege:null,SettlementState.PENDING);
        if(entry.mapping.timeAttack()==null)return fromEngine(snapshot,revision,entry.descriptor);
        var result=snapshot.timeAttack().finished()?cooperativeEvent:null;
        // The live service must use the same descriptor-gated projection as
        // the atomic NPC frame. The legacy projection deliberately rejects
        // skill70 ledgers, including their initial zero-cast loading state.
        var cooperative=entry.descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM)
                ?ModernTimeAttackProtocol.fromFireStorm(snapshot,result,SettlementState.PENDING)
                :ModernTimeAttackProtocol.from(snapshot,result,SettlementState.PENDING);
        return new Frame(snapshot.matchId(),snapshot.generation(),revision,Phase.valueOf(snapshot.phase().name()),
                AbortReason.valueOf(snapshot.abortReason().name()),snapshot.room(),snapshot.manifest(),snapshot.actors(),null,
                snapshot.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList(),entry.descriptor,snapshot.race(),-1,
                ModernQuestProtocol.Data.disabled(),cooperative);
    }
    static Frame fromEngine(ModernBattleEngine.Snapshot snapshot, long revision,ModernMatchDescriptor descriptor) {
        requireDisintegrateDescriptor(snapshot,descriptor);
        if(snapshot.siege().enabled())throw new IllegalArgumentException("siege requires a dedicated wire and settlement contract");
        if(snapshot.timeAttack().enabled())throw new IllegalArgumentException("cooperative Time Attack requires a dedicated wire contract");
        if(!snapshot.npcSpells().isEmpty()!=(descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_LIGHT_ARROW)))
            throw new IllegalArgumentException("NPC magic descriptor mismatch");
        return new Frame(snapshot.matchId(), snapshot.generation(), revision, Phase.valueOf(snapshot.phase().name()),
                AbortReason.valueOf(snapshot.abortReason().name()), snapshot.room(), snapshot.manifest(), snapshot.actors(),
                snapshot.result() == null ? null : new Completion(snapshot.result(), SettlementState.PENDING),
                snapshot.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList(),descriptor,snapshot.race(),-1,descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM)?ModernQuestProtocol.fromFireStorm(snapshot):ModernQuestProtocol.from(snapshot));
    }
    private static boolean questObjectives(Mapping mapping){return mapping.npc()!=null&&mapping.npc().questPlan()!=null&&mapping.npc().questPlan().objectives()!=null;}
    /** Legacy clients use listener-global polling/presence, not a per-match mask. */
    private static boolean compatible(Registration row,ModernMatchDescriptor descriptor,int mode){
        if(descriptor.has(ModernMatchDescriptor.FULL_VIP)&&(!(row.physical instanceof FullVipReader)||!ModernSessionExtensions.allowsVipData(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.ARMOR_PROTECTION)&&(!row.armorProtectionSource||!ModernSessionExtensions.allowsArmor(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.PLAYER_HEALING)&&(!row.playerHealingSource||!ModernSessionExtensions.allowsData(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.SLOW)&&(row.slow==null||!ModernBattleSpeedWire.negotiated(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.HASTE)&&(row.haste==null||!ModernHasteProtocol.negotiated(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.GREATER_HASTE)&&(row.greaterHaste==null||!ModernGreaterHasteProtocol.negotiated(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM)&&(row.skills==null||row.physical==null||!row.regenProtocol||!ModernNpcFireStormProtocol.negotiated(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.NPC_DISINTEGRATE)&&(row.disintegrate==null||!ModernNpcDisintegrateProtocol.negotiated(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.DISINTEGRATE)&&(row.disintegrate==null||!ModernDisintegrateProtocol.negotiated(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.SIEGE)&&(row.protocol==null||!descriptor.supportedBy(row.protocol)))return false;
        if(descriptor.has(ModernMatchDescriptor.NPC)&&!npcNegotiated(row.protocol))return false;
        if(row.protocol==null)return true; // Trusted compatibility constructors retain their existing provider checks.
        int version=row.protocol.version(),caps=row.protocol.capabilities();
        if(!descriptor.supportedBy(row.protocol))return false;
        if((caps&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)!=0)return version>=15;
        int[] flags={ModernMatchDescriptor.COMBAT,ModernMatchDescriptor.ITEMS,ModernMatchDescriptor.WEIGHT,
                ModernMatchDescriptor.TIMED,ModernMatchDescriptor.AMMO,ModernMatchDescriptor.REGEN};
        int[] capabilities={128,512,1024,2048,4096,8192};
        for(int i=0;i<flags.length;i++)if(descriptor.has(flags[i])!=((caps&capabilities[i])!=0))return false;
        return mode!=5 || descriptor.has(ModernMatchDescriptor.HONOR)==((caps&ModernSessionProtocol.CAP_HONOR)!=0);
    }
    static ModernCombatProtocol.Frame fromCombat(ModernBattleEngine.Snapshot snapshot, Frame frame) {
        requireDisintegrateDescriptor(snapshot,frame==null?null:frame.descriptor());
        if(!snapshot.npcSpells().isEmpty()!=(frame!=null&&frame.descriptor()!=null&&frame.descriptor().has(ModernMatchDescriptor.NPC_LIGHT_ARROW)))
            throw new IllegalArgumentException("NPC magic descriptor mismatch");
        if (snapshot.vitals().isEmpty()) return null;
        // PREPARING is service revision 1; engine events use a stable +1 mapping,
        // including concurrent updates that skip an intermediate publication.
        return new ModernCombatProtocol.Frame(frame, snapshot.vitals().stream().map(v -> new ModernCombatProtocol.Vital(
                v.entityId(), v.hp(), v.maxHp(), v.mp(), v.maxMp(), v.ac(), v.weaponType(), v.attackSequence(),
                eventRevision(v.attackRevision()), eventRevision(v.hurtRevision()), eventRevision(v.deathRevision()),
                v.lastTargetEntityId(), v.attackIntervalNanos())).toList(),
                snapshot.damages().stream().map(ModernDamageSpellProtocol.State::wireRevision).toList(),
                snapshot.items().stream().map(ModernItemProtocol.State::wireRevision).toList(),snapshot.weights(),
                snapshot.buffs().stream().map(ModernTimedEffects.State::wireRevision).toList(),snapshot.ammo().stream().map(ModernAmmo.State::wireRevision).toList(),
                snapshot.regen().stream().map(ModernRegenProtocol.State::wireRevision).toList(),snapshot.honor().wireRevision(),snapshot.fireStorms().stream().map(ModernFireStormProtocol.State::wireRevision).toList(),snapshot.npcSpells().stream().map(ModernNpcMagic.CastState::wireRevision).toList(),snapshot.disintegrations().stream().map(ModernDisintegrateState.State::wireRevision).toList(),snapshot.npcDisintegrations().stream().map(ModernNpcDisintegrateState.State::wireRevision).toList(),snapshot.npcFireStorms().stream().map(ModernNpcFireStormState.State::wireRevision).toList());
    }
    private static long eventRevision(long revision) { return revision == 0 ? 0 : Math.addExact(revision, 1); }
    private static void requireDisintegrateDescriptor(ModernBattleEngine.Snapshot snapshot,ModernMatchDescriptor descriptor) {
        if(!snapshot.npcFireStorms().isEmpty()!=(descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM)))throw new IllegalArgumentException("NPC fire storm requires its own typed wire contract");
        if(!snapshot.npcDisintegrations().isEmpty()!=(descriptor!=null&&descriptor.has(ModernMatchDescriptor.NPC_DISINTEGRATE)))
            throw new IllegalArgumentException("NPC disintegrate descriptor mismatch");
        if(!snapshot.disintegrations().isEmpty()!=(descriptor!=null&&descriptor.has(ModernMatchDescriptor.DISINTEGRATE)))
            throw new IllegalArgumentException("disintegrate descriptor mismatch");
    }
    private static WorldExchange rejectedCapture(ModernRooms.Result result) {
        Reason reason = switch (result.reason()) {
            case NOT_OWNER -> Reason.NOT_OWNER; case REVISION_MISMATCH -> Reason.ROOM_REVISION;
            case INVALID_STATE -> Reason.NOT_READY; case CAPACITY -> Reason.CAPACITY;
            default -> Reason.INVALID_STATE;
        };
        return exchange(result.status() == ModernRooms.Status.UNAVAILABLE ? Status.UNAVAILABLE : Status.DENIED, reason, null);
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern world service value"); }
}
