package local.lito381.server;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** One immutable engine observation, in ENGINE revision space. Not a wire ABI
 * or permission to enable advanced SET. Capture stays server-internal; only
 * forOwner may assemble an owner response. No legacy World/Combat Frame is
 * needed to construct or validate the new world and its private owner data. */
final class ModernBattleObservation {
    record Features(boolean combat,boolean spells,boolean arrow,boolean items,boolean weight,
            boolean timed,boolean ammo,boolean regen,boolean fire,boolean disintegrate,
            boolean npcArrow,boolean npcFire,boolean npcDisintegrate,boolean greaterHaste,boolean haste,boolean slow,boolean playerHealing,boolean armorProtection) {
        Features(boolean combat,boolean spells,boolean arrow,boolean items,boolean weight,boolean timed,boolean ammo,boolean regen,
                boolean fire,boolean disintegrate,boolean npcArrow,boolean npcFire,boolean npcDisintegrate,boolean greaterHaste,boolean haste,boolean slow,boolean playerHealing) {
            this(combat,spells,arrow,items,weight,timed,ammo,regen,fire,disintegrate,npcArrow,npcFire,npcDisintegrate,greaterHaste,haste,slow,playerHealing,false);
        }
        Features(boolean combat,boolean spells,boolean arrow,boolean items,boolean weight,boolean timed,boolean ammo,boolean regen,
                boolean fire,boolean disintegrate,boolean npcArrow,boolean npcFire,boolean npcDisintegrate,boolean greaterHaste,boolean haste,boolean slow) {
            this(combat,spells,arrow,items,weight,timed,ammo,regen,fire,disintegrate,npcArrow,npcFire,npcDisintegrate,greaterHaste,haste,slow,false);
        }
        Features(boolean combat,boolean spells,boolean arrow,boolean items,boolean weight,boolean timed,boolean ammo,boolean regen,
                boolean fire,boolean disintegrate,boolean npcArrow,boolean npcFire,boolean npcDisintegrate,boolean greaterHaste,boolean haste) {
            this(combat,spells,arrow,items,weight,timed,ammo,regen,fire,disintegrate,npcArrow,npcFire,npcDisintegrate,greaterHaste,haste,false);
        }
        Features(boolean combat,boolean spells,boolean arrow,boolean items,boolean weight,boolean timed,boolean ammo,boolean regen,
                boolean fire,boolean disintegrate,boolean npcArrow,boolean npcFire,boolean npcDisintegrate,boolean greaterHaste) {
            this(combat,spells,arrow,items,weight,timed,ammo,regen,fire,disintegrate,npcArrow,npcFire,npcDisintegrate,greaterHaste,false);
        }
        Features(boolean combat,boolean spells,boolean arrow,boolean items,boolean weight,boolean timed,boolean ammo,boolean regen,
                boolean fire,boolean disintegrate,boolean npcArrow,boolean npcFire,boolean npcDisintegrate) {
            this(combat,spells,arrow,items,weight,timed,ammo,regen,fire,disintegrate,npcArrow,npcFire,npcDisintegrate,false);
        }
    }
    record Clock(long loadingElapsedNanos,long activeElapsedNanos) {
        Clock {if(loadingElapsedNanos<0||activeElapsedNanos< -1||activeElapsedNanos>loadingElapsedNanos)throw bad();}
    }
    record Vital(ModernEquipmentVitals.Vitals values,long ac,int weaponType,long attackSequence,
            long attackRevision,long hurtRevision,long deathRevision,long target,long attackIntervalNanos) {
        Vital {
            Objects.requireNonNull(values);
            if(weaponType<0||weaponType>18||attackSequence<0||attackRevision<0||hurtRevision<0||deathRevision<0
                    ||values.hp()==0&&deathRevision==0||target<0||(attackRevision==0)!=(target==0)
                    ||attackIntervalNanos<1_000_000||attackIntervalNanos>10_000_000_000L)throw bad();
        }
    }
    record Supplies(long requestSequence,long requestRevision,ModernUseHistory.Items history,List<ModernItemProtocol.Stock> stocks,int effect,int delayMillis) {
        Supplies {
            Objects.requireNonNull(history);stocks=List.copyOf(stocks);
            new ModernItemRuntime.State(requestSequence,requestRevision,stocks).validate(history);
            if(effect!=ModernItemRuntime.effect(history)||delayMillis!=ModernItemRuntime.delayMillis(history))throw bad();
        }
    }
    record RegenAxis(long gain,long loss,long revision,long before,long after) {
        RegenAxis {if(gain<0||loss<0||revision<0||before<0||after<0)throw bad();}
    }
    record Regeneration(RegenAxis hp,RegenAxis mp,long itemGain,long damageLoss) {
        Regeneration {Objects.requireNonNull(hp);Objects.requireNonNull(mp);if(itemGain<0||damageLoss<0)throw bad();}
    }
    /** Source scope/epoch is deliberately absent from visible spell batches. */
    record Impact(ModernUseHistory.Lane lane,ModernUseHistory.Cast use,List<ModernTargetHistory.Effect> targets) {
        Impact {Objects.requireNonNull(lane);Objects.requireNonNull(use);targets=List.copyOf(targets);}
        static Impact from(ModernTargetHistory.Batch b){return new Impact(b.lane(),b.use(),b.targets());}
    }
    record SpellReceipt(Impact impact,long casts,long spent,long loadingElapsedNanos,ModernSpellRuntime.Detail detail) {
        SpellReceipt {Objects.requireNonNull(impact);Objects.requireNonNull(detail);if(casts<1||spent<0||loadingElapsedNanos<0)throw bad();}
    }
    record Structure(long casts,Impact impact,ModernQuestObjectives.Kind questKind) {
        Structure {Objects.requireNonNull(impact);if(casts<1||impact.lane()!=ModernUseHistory.Lane.NPC_FIRE_STORM)throw bad();}
    }
    record Spells(long sequence,long requestRevision,long initialLawful,long lawful,
            Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories,Map<ModernUseHistory.Lane,SpellReceipt> receipts,Structure structure) {
        Spells {
            histories=Map.copyOf(histories);receipts=Map.copyOf(receipts);
            if(sequence<0||requestRevision<0||(sequence==0)!=(requestRevision==0)||!histories.keySet().containsAll(receipts.keySet()))throw bad();
            for(var entry:histories.entrySet()) {
                var h=entry.getValue();var r=receipts.get(entry.getKey());
                if(h.last()==null?r!=null:r==null||r.impact().lane()!=entry.getKey()||!r.impact().use().equals(h.last())
                        ||r.casts()!=h.current().casts()||r.spent()!=h.current().spent()
                        ||r.impact().use().sequence()>sequence||r.impact().use().revision()>requestRevision)throw bad();
            }
        }
        static Spells from(CapturedActor a,long loadingOrigin) {
            var r=a.runtime();var receipts=new EnumMap<ModernUseHistory.Lane,SpellReceipt>(ModernUseHistory.Lane.class);
            if(r!=null)r.receipts().forEach((lane,s)->receipts.put(lane,new SpellReceipt(Impact.from(s.batch()),s.casts(),s.spent(),s.atNanos()-loadingOrigin,s.detail())));
            var structure=r==null?null:r.structure();
            return new Spells(r==null?0:r.sequence(),r==null?0:r.requestRevision(),r==null?0:r.initialLawful(),r==null?0:r.lawful(),a.histories(),receipts,
                    structure==null?null:new Structure(structure.casts(),Impact.from(structure.batch()),structure.questKind()));
        }
    }
    record PlayerHealingReceipt(ModernPlayerHealingHistory.Impact impact,long casts,long spent,long loadingElapsedNanos) {
        PlayerHealingReceipt {
            Objects.requireNonNull(impact);
            if(casts<1||casts>impact.use().sequence()||spent<impact.use().cost()||casts==1&&spent!=impact.use().cost()
                    ||spent/4>casts||spent/4==casts&&spent%4!=0||loadingElapsedNanos<0)throw bad();
        }
    }
    /** Public participant healing contains no selection scope/epoch. */
    record PlayerHealing(ModernPlayerHealingHistory.Incoming incoming,ModernUseHistory.Spell outgoing,PlayerHealingReceipt receipt) {
        PlayerHealing {
            Objects.requireNonNull(incoming);
            if(outgoing!=null)ModernPlayerHealingHistory.checkSpending(outgoing);
            if(outgoing==null?receipt!=null:(outgoing.last()==null)!=(receipt==null))throw bad();
            if(receipt!=null&&(!receipt.impact().use().equals(outgoing.last())||receipt.casts()!=outgoing.current().casts()
                    ||receipt.spent()!=outgoing.current().spent()))throw bad();
        }
        static PlayerHealing from(CapturedActor a,long loadingOrigin) {
            if(a.playerHealing()==null)return null;
            var h=a.playerHealing();var r=a.runtime()==null?null:a.runtime().playerHealing();
            return new PlayerHealing(h.incoming(),h.outgoing()==null?null:h.outgoing().spending(),r==null?null:
                    new PlayerHealingReceipt(r.receipt().impact(),r.casts(),r.spent(),r.atNanos()-loadingOrigin));
        }
        void validate(ModernTargetHistory.World world,ModernTargetHistory.Participant actor,ModernVitalBalance.State resources,
                Spells spells,Clock clock) {
            if(!incoming.world().equals(world)||!incoming.recipient().equals(actor)||actor.identity().isHuman()!=(outgoing!=null))throw bad();
            incoming.validate(resources);
            if(outgoing!=null) {
                var l=outgoing.current();
                if(l.equipmentRevision()!=resources.equipmentRevision()||l.profileRevision()>resources.revision()||l.cost()>4)throw bad();
            }
            if(receipt!=null) {
                var r=receipt.impact();var u=r.use();
                if(!r.world().equals(world)||!r.caster().equals(actor)||u.sequence()>spells.sequence()
                        ||u.revision()>spells.requestRevision()||receipt.loadingElapsedNanos()>clock.loadingElapsedNanos())throw bad();
                ModernEquipmentHistory.checkAnchor(resources,u.before(),u.revision());
                long count=receipt.casts();
                for(var ordinary:spells.receipts().values()) {
                    var o=ordinary.impact().use();count=Math.addExact(count,ordinary.casts());
                    if(o.sequence()==u.sequence()||o.revision()==u.revision()
                            ||(o.sequence()<u.sequence())!=(o.revision()<u.revision())
                            ||(o.sequence()<u.sequence()?ordinary.loadingElapsedNanos()>receipt.loadingElapsedNanos()
                                :ordinary.loadingElapsedNanos()<receipt.loadingElapsedNanos()))throw bad();
                }
                if(count>spells.sequence())throw bad();
                var received=incoming.last();
                if(r.self()&&(received==null||received.use().revision()<u.revision()
                        ||received.use().revision()==u.revision()&&!received.equals(r)))throw bad();
            }
            if(incoming.last()!=null&&incoming.last().self()) {
                var received=incoming.last();
                if(receipt==null||receipt.impact().use().revision()<received.use().revision()
                        ||receipt.impact().use().revision()==received.use().revision()&&!received.equals(receipt.impact()))throw bad();
            }
        }
    }
    /** No source inventory, selection epoch, loadout or private clocks here. */
    record Actor(ModernBattleEngine.Actor pose,Vital vital,ModernVitalBalance.State resources,Spells spells,
            long castIntervalNanos,ModernTimedEffects.State buffs,Regeneration regeneration,ModernBattlePresentation.State presentation,PlayerHealing playerHealing,
            ModernArmorProtectionEffects.State armorProtection) {
        Actor(ModernBattleEngine.Actor pose,Vital vital,ModernVitalBalance.State resources,Spells spells,
                long castIntervalNanos,ModernTimedEffects.State buffs,Regeneration regeneration,ModernBattlePresentation.State presentation,PlayerHealing playerHealing) {
            this(pose,vital,resources,spells,castIntervalNanos,buffs,regeneration,presentation,playerHealing,null);
        }
        Actor(ModernBattleEngine.Actor pose,Vital vital,ModernVitalBalance.State resources,Spells spells,
                long castIntervalNanos,ModernTimedEffects.State buffs,Regeneration regeneration,ModernBattlePresentation.State presentation) {
            this(pose,vital,resources,spells,castIntervalNanos,buffs,regeneration,presentation,null);
        }
    }
    record Honor(List<ModernHonor.Objective> objectives,List<ModernHonor.Life> lives) {
        Honor {objectives=List.copyOf(objectives);lives=List.copyOf(lives);if(objectives.isEmpty()!=lives.isEmpty()||!objectives.isEmpty()&&objectives.size()!=2)throw bad();}
        boolean enabled(){return !objectives.isEmpty();}
    }
    record Quest(ModernQuestProgress.State progress,List<ModernQuestObjectives.Actor> objectives,
            List<ModernQuestObjectives.Exchange> exchanges,List<ModernQuestLife.State> lives) {
        Quest {objectives=List.copyOf(objectives);exchanges=List.copyOf(exchanges);lives=List.copyOf(lives);
            if(progress==null&&(!objectives.isEmpty()||!exchanges.isEmpty()||!lives.isEmpty()))throw bad();}
    }
    record Modes(boolean isolatedForces,Honor honor,Quest quest,ModernTimeAttack.Snapshot timeAttack,
            ModernSiege.Snapshot siege,ModernPetRace.State race,UUID raceResultId,
            ModernMatchSettlement.Event timeAttackResult,ModernMatchSettlement.Event siegeResult) {
        Modes {Objects.requireNonNull(honor);Objects.requireNonNull(quest);Objects.requireNonNull(timeAttack);Objects.requireNonNull(siege);}
    }
    record Npcs(ModernNpcRoster.Snapshot roster,List<ModernNpcBattle.Exchange> exchanges) {
        Npcs {Objects.requireNonNull(roster);exchanges=List.copyOf(exchanges);}
    }
    record World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,List<Actor> actors,
            Modes modes,Npcs npcs,ModernMatchSettlement.Event result,ModernHasteEffects.State hasteEffects,ModernSpeedEffects.State speedEffects,boolean vip) {
        World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,List<Actor> actors,
                Modes modes,Npcs npcs,ModernMatchSettlement.Event result,ModernHasteEffects.State hasteEffects,ModernSpeedEffects.State speedEffects) {
            this(identity,revision,phase,abortReason,room,manifest,features,clock,actors,modes,npcs,result,hasteEffects,speedEffects,false);
        }
        World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,List<Actor> actors,
                Modes modes,Npcs npcs,ModernMatchSettlement.Event result,ModernHasteEffects.State hasteEffects) {
            this(identity,revision,phase,abortReason,room,manifest,features,clock,actors,modes,npcs,result,hasteEffects,null);
        }
        World(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,List<Actor> actors,
                Modes modes,Npcs npcs,ModernMatchSettlement.Event result) {
            this(identity,revision,phase,abortReason,room,manifest,features,clock,actors,modes,npcs,result,null);
        }
        World {
            Objects.requireNonNull(identity);Objects.requireNonNull(phase);Objects.requireNonNull(abortReason);Objects.requireNonNull(room);
            Objects.requireNonNull(manifest);Objects.requireNonNull(features);Objects.requireNonNull(clock);Objects.requireNonNull(modes);actors=List.copyOf(actors);
            // A structural marker survives canonical validation. Old audience
            // projection must not erase VIP's saved/runtime version split.
            if(vip&&(!features.combat()||room.summary().config().mode()!=BattleRules.Mode.DEATHMATCH.wireId()
                    ||npcs!=null||modes.isolatedForces()||actors.stream().anyMatch(a->!a.pose().member().identity().isHuman())))throw bad();
            if(revision<1||identity.roomId()!=room.summary().id()||identity.generation()!=room.summary().revision()
                    ||features.playerHealing()&&(!features.combat()||!features.spells()||!features.weight())
                    ||features.armorProtection()&&!features.combat()
                    ||actors.size()!=room.members().size()||actors.isEmpty()||actors.size()>100
                    ||(phase==ModernBattleEngine.Phase.ABORTED)!=(abortReason!=ModernBattleEngine.AbortReason.NONE))throw bad();
            boolean terminal=result!=null||modes.timeAttack().finished()||modes.siege().finished()
                    ||modes.race()!=null&&modes.race().phase()==ModernPetRace.Phase.COMPLETE;
            if((phase==ModernBattleEngine.Phase.FINISHED)!=terminal)throw bad();
            var occupied=new HashSet<ModernQuestSpace.Cell>();
            for(int i=0;i<actors.size();i++) {
                var a=actors.get(i);var p=a.pose();
                if(p.entityId()!=i+1||!p.member().equals(room.members().get(i))||!ModernWorldProtocol.contains(manifest,p.position())
                        ||features.combat()!=(a.vital()!=null)||features.combat()!=(a.resources()!=null)
                        ||features.timed()!=(a.buffs()!=null)||features.regen()!=(a.regeneration()!=null)
                        ||features.playerHealing()!=(a.playerHealing()!=null)
                        ||(features.armorProtection()&&p.member().identity().isHuman())!=(a.armorProtection()!=null)
                        ||(phase==ModernBattleEngine.Phase.COUNTDOWN||phase==ModernBattleEngine.Phase.ACTIVE||phase==ModernBattleEngine.Phase.FINISHED)&&!p.loaded())throw bad();
                boolean participating=(modes.race()==null||modes.race().runners().get(i).competing())
                        &&(!modes.timeAttack().enabled()||!modes.timeAttack().state().departed().contains(p.entityId()));
                if(a.armorProtection()!=null)a.armorProtection().validate(revision,phase,clock,p,a.resources(),participating);
                if(participating&&!occupied.add(ModernQuestSpace.cell(modes.isolatedForces(),p.member().team(),p.position())))throw bad();
                if(a.vital()!=null) {
                    var v=a.vital();var resources=a.resources();
                    if(!v.values().equals(resources.vitals())||resources.revision()>revision
                            ||v.attackRevision()>revision||v.hurtRevision()>revision||v.deathRevision()>revision)throw bad();
                    long spent=0;
                    for(var h:a.spells().histories().values()) {
                        if(h.current().equipmentRevision()!=resources.equipmentRevision()||h.current().profileRevision()>revision)throw bad();
                        spent=Math.addExact(spent,h.current().spent());
                    }
                    long incomingHealing=0;
                    if(a.playerHealing()!=null) {
                        var h=a.playerHealing();h.validate(identity,new ModernTargetHistory.Participant(p.entityId(),p.member().identity()),resources,a.spells(),clock);
                        if(h.outgoing()!=null)spent=Math.addExact(spent,h.outgoing().current().spent());
                        incomingHealing=h.incoming().restored();
                    }
                    if(spent!=resources.flows().spent())throw bad();
                    if(a.regeneration()!=null) {
                        var r=a.regeneration();var f=resources.flows();
                        if(r.hp().revision()>revision||r.mp().revision()>revision||r.hp().gain()!=f.hpGain()||r.hp().loss()!=f.hpLoss()
                                ||r.mp().gain()!=f.mpGain()||r.mp().loss()!=f.mpLoss()||Math.addExact(r.itemGain(),incomingHealing)!=f.restored()||r.damageLoss()!=f.damaged())throw bad();
                    }
                }
                if(a.spells().requestRevision()>revision||a.spells().receipts().values().stream().anyMatch(r->r.impact().use().revision()>revision||r.loadingElapsedNanos()>clock.loadingElapsedNanos()))throw bad();
                if(!features.slow()&&a.buffs()!=null)ModernTimedEffects.requireNoSlow(a.buffs());
                if(features.slow()!=a.spells().histories().containsKey(ModernUseHistory.Lane.SLOW)
                        ||features.slow()&&(!features.timed()||!features.spells()||!features.weight()))throw bad();
                var slowReceipt=a.spells().receipts().get(ModernUseHistory.Lane.SLOW);
                if(slowReceipt!=null) {
                    if(!(slowReceipt.detail() instanceof ModernSlowReceipt d)||d.activeElapsedNanos()>clock.activeElapsedNanos()
                            ||slowReceipt.loadingElapsedNanos()-d.activeElapsedNanos()!=clock.loadingElapsedNanos()-clock.activeElapsedNanos())throw bad();
                    d.validate(identity,new ModernTargetHistory.Participant(p.entityId(),p.member().identity()),ModernUseHistory.Lane.SLOW,
                            slowReceipt.impact().use(),slowReceipt.impact().targets());
                    d.validateNpc(npcs,slowReceipt.impact().use().revision(),revision,phase);
                }
                if(features.haste()!=a.spells().histories().containsKey(ModernUseHistory.Lane.HASTE)
                        ||features.haste()&&(!features.timed()||!features.spells())
                        ||!features.haste()&&a.buffs()!=null&&a.buffs().hasteOrigin()==ModernTimedEffects.HasteOrigin.HASTE)throw bad();
                if(features.greaterHaste()!=a.spells().histories().containsKey(ModernUseHistory.Lane.GREATER_HASTE))throw bad();
                if(features.greaterHaste()&&(!features.timed()||!features.spells()))throw bad();
                var hasteReceipt=a.spells().receipts().get(ModernUseHistory.Lane.GREATER_HASTE);
                // The retained event clock remains bound after potion overwrite,
                // death, revival or retirement, not only while its buff is live.
                if(hasteReceipt!=null) {
                    long at;
                    if(hasteReceipt.detail() instanceof ModernSpellRuntime.GreaterHaste d)at=d.activeElapsedNanos();
                    else if(features.slow()&&hasteReceipt.detail() instanceof ModernSpellRuntime.GreaterHasteNeutralized n)at=n.activeElapsedNanos();
                    else throw bad();
                    if(at>clock.activeElapsedNanos()||hasteReceipt.loadingElapsedNanos()-at!=clock.loadingElapsedNanos()-clock.activeElapsedNanos())throw bad();
                }
                if(a.buffs()!=null&&a.buffs().hasteOrigin()==ModernTimedEffects.HasteOrigin.GREATER_HASTE) {
                    var receipt=a.spells().receipts().get(ModernUseHistory.Lane.GREATER_HASTE);
                    if(!features.greaterHaste()||!p.member().identity().isHuman()||phase!=ModernBattleEngine.Phase.ACTIVE
                            ||a.vital().values().hp()==0||receipt==null||!(receipt.detail() instanceof ModernSpellRuntime.GreaterHaste d)
                            ||d.hasteUntilNanos()!=a.buffs().hasteUntilNanos()||d.activeElapsedNanos()>a.buffs().observedNanos()
                            ||a.buffs().observedNanos()>clock.activeElapsedNanos()
                            ||receipt.loadingElapsedNanos()-d.activeElapsedNanos()!=clock.loadingElapsedNanos()-clock.activeElapsedNanos()
                            ||receipt.impact().use().revision()>a.buffs().revision()||a.buffs().revision()>revision
                            ||a.buffs().hasteUntilNanos()<=clock.activeElapsedNanos()
                            ||receipt.impact().use().before().life()!=Math.incrementExact(a.resources().life())
                            ||receipt.impact().use().before().equipmentRevision()>a.resources().equipmentRevision())throw bad();
                }
            }
            ModernBattlePresentation.world(revision,clock,manifest,features,actors,modes);
            validateModes(identity,revision,phase,clock,room,manifest,actors,modes,npcs,result);
            // Combined worlds replace the old haste-only extension with ALL
            // speed grants and motion. Existing publication/wire still refuse
            // Slow until the new material/status contract is negotiated.
            if((features.haste()&&!features.slow())!=(hasteEffects!=null)||features.slow()!=(speedEffects!=null))throw bad();
            if(hasteEffects!=null)hasteEffects.validate(identity,revision,phase,clock,manifest,actors,modes.isolatedForces(),npcs);
            if(speedEffects!=null)speedEffects.validate(identity,revision,phase,clock,manifest,actors,modes.isolatedForces(),npcs);
        }
    }
    /** Immutable raw capture is private server state, not a network response.
     * Legacy projection intentionally does not construct a full owner history:
     * an old read must not move a NUMERIC fault-injection rejection to an
     * earlier unrelated operation. forOwner performs the full new validation. */
    record CapturedActor(ModernBattleEngine.Actor pose,ModernRooms.Scope scope,ModernBattleLoadout loadout,ModernSkills.Snapshot skills,
            Vital vital,ModernVitalBalance.State resources,long castIntervalNanos,
            Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories,Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets,
            ModernSpellRuntime.State runtime,Supplies supplies,ModernInventoryWeight.Snapshot weight,ModernTimedEffects.State buffs,
            ModernAmmo.State ammo,Regeneration regeneration,ModernNaturalRegen.Clock regenClock,ModernBattlePresentation.State presentation,ModernSpellMaterials.State materials,ModernPlayerHealingHistory.Entry playerHealing,
            ModernBattleEngine.ArmorProtectionView armorProtection) {
        CapturedActor(ModernBattleEngine.Actor pose,ModernRooms.Scope scope,ModernBattleLoadout loadout,ModernSkills.Snapshot skills,
                Vital vital,ModernVitalBalance.State resources,long castIntervalNanos,
                Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories,Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets,
                ModernSpellRuntime.State runtime,Supplies supplies,ModernInventoryWeight.Snapshot weight,ModernTimedEffects.State buffs,
                ModernAmmo.State ammo,Regeneration regeneration,ModernNaturalRegen.Clock regenClock,ModernBattlePresentation.State presentation,ModernSpellMaterials.State materials,ModernPlayerHealingHistory.Entry playerHealing) {
            this(pose,scope,loadout,skills,vital,resources,castIntervalNanos,histories,targets,runtime,supplies,weight,buffs,ammo,regeneration,regenClock,presentation,materials,playerHealing,null);
        }
        CapturedActor(ModernBattleEngine.Actor pose,ModernRooms.Scope scope,ModernBattleLoadout loadout,ModernSkills.Snapshot skills,
                Vital vital,ModernVitalBalance.State resources,long castIntervalNanos,
                Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories,Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets,
                ModernSpellRuntime.State runtime,Supplies supplies,ModernInventoryWeight.Snapshot weight,ModernTimedEffects.State buffs,
                ModernAmmo.State ammo,Regeneration regeneration,ModernNaturalRegen.Clock regenClock,ModernBattlePresentation.State presentation,ModernSpellMaterials.State materials) {
            this(pose,scope,loadout,skills,vital,resources,castIntervalNanos,histories,targets,runtime,supplies,weight,buffs,ammo,regeneration,regenClock,presentation,materials,null);
        }
        CapturedActor(ModernBattleEngine.Actor pose,ModernRooms.Scope scope,ModernBattleLoadout loadout,ModernSkills.Snapshot skills,
                Vital vital,ModernVitalBalance.State resources,long castIntervalNanos,
                Map<ModernUseHistory.Lane,ModernUseHistory.Spell> histories,Map<ModernUseHistory.Lane,ModernTargetHistory.History> targets,
                ModernSpellRuntime.State runtime,Supplies supplies,ModernInventoryWeight.Snapshot weight,ModernTimedEffects.State buffs,
                ModernAmmo.State ammo,Regeneration regeneration,ModernNaturalRegen.Clock regenClock,ModernBattlePresentation.State presentation) {
            this(pose,scope,loadout,skills,vital,resources,castIntervalNanos,histories,targets,runtime,supplies,weight,buffs,ammo,regeneration,regenClock,presentation,null);
        }
        CapturedActor {Objects.requireNonNull(pose);histories=Map.copyOf(histories);targets=Map.copyOf(targets);}
        @Override public String toString(){return "CapturedActor[redacted]";}
    }
    record Capture(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
            ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,ModernBattleEngine.Limits limits,long loadingOriginNanos,
            List<CapturedActor> actors,Modes modes,Npcs npcs,ModernMatchSettlement.Event result,ModernHasteTargets.State hasteTargets,ModernSlowTargets.State slowTargets,UUID armorWorld) implements ModernBattleCommand {
        Capture(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,ModernBattleEngine.Limits limits,long loadingOriginNanos,
                List<CapturedActor> actors,Modes modes,Npcs npcs,ModernMatchSettlement.Event result,ModernHasteTargets.State hasteTargets,ModernSlowTargets.State slowTargets) {
            this(identity,revision,phase,abortReason,room,manifest,features,clock,limits,loadingOriginNanos,actors,modes,npcs,result,hasteTargets,slowTargets,null);
        }
        Capture(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,ModernBattleEngine.Limits limits,long loadingOriginNanos,
                List<CapturedActor> actors,Modes modes,Npcs npcs,ModernMatchSettlement.Event result,ModernHasteTargets.State hasteTargets) {
            this(identity,revision,phase,abortReason,room,manifest,features,clock,limits,loadingOriginNanos,actors,modes,npcs,result,hasteTargets,null);
        }
        Capture(ModernTargetHistory.World identity,long revision,ModernBattleEngine.Phase phase,ModernBattleEngine.AbortReason abortReason,
                ModernRooms.RoomSnapshot room,ModernWorldMap.Manifest manifest,Features features,Clock clock,ModernBattleEngine.Limits limits,long loadingOriginNanos,
                List<CapturedActor> actors,Modes modes,Npcs npcs,ModernMatchSettlement.Event result) {
            this(identity,revision,phase,abortReason,room,manifest,features,clock,limits,loadingOriginNanos,actors,modes,npcs,result,null);
        }
        Capture {
            Objects.requireNonNull(limits);actors=List.copyOf(actors);
            UUID vipContext=null;
            boolean vip=actors.stream().anyMatch(a->a.loadout()!=null&&a.loadout().fighter()!=null&&a.loadout().fighter().vip()!=null);
            for(var a:actors)if(vip&&a.pose().member().identity().isHuman()) {
                if(a.loadout()==null||a.loadout().fighter()==null||a.loadout().fighter().vip()==null||a.resources()==null)throw bad();
                var fighter=a.loadout().fighter();var state=fighter.vip().state();var actor=state.ledger().actor();
                if(!actor.owner().equals(a.pose().member().key())||actor.entity()!=a.pose().entityId()||actor.life()!=a.resources().life()
                        ||fighter.profile().equipmentRevision()!=a.resources().equipmentRevision()
                        ||fighter.profile().equipmentRevision()<state.ledger().equipmentRevision())throw bad();
                if(vipContext!=null&&!vipContext.equals(actor.context()))throw bad();vipContext=actor.context();
            }
            if(features.armorProtection()!=(armorWorld!=null)||features.armorProtection()&&!features.combat())throw bad();
            if(actors.stream().anyMatch(a->a.histories().containsKey(ModernUseHistory.Lane.HASTE))!=(hasteTargets!=null)
                    ||features.haste()!=(hasteTargets!=null))throw bad();
            if(hasteTargets!=null)hasteTargets.validate(identity,revision,phase,clock,loadingOriginNanos,actors,npcs);
            if(features.slow()!=(slowTargets!=null))throw bad();
            if(slowTargets!=null)slowTargets.validate(identity,revision,phase,clock,loadingOriginNanos,actors,npcs);
            for(var a:actors) {
                boolean protectedActor=features.armorProtection()&&a.pose().member().identity().isHuman();
                if(protectedActor!=(a.armorProtection()!=null)
                        ||protectedActor!=(a.loadout()!=null&&a.loadout().fighter()!=null&&a.loadout().fighter().armorProtection()!=null))throw bad();
                if(protectedActor) {
                    var s=a.armorProtection().state();var id=s.actor();
                    if(!id.match().equals(armorWorld)||!id.owner().equals(a.pose().member().identity().humanKey())
                            ||id.entity()!=a.pose().entityId()||a.resources()==null||id.life()!=a.resources().life()
                            ||a.armorProtection().revision()>revision)throw bad();
                    var anchor=a.armorProtection().anchor();
                    if(anchor!=null)anchor.validate(manifest,a.pose(),a.armorProtection().revision(),revision);
                }
                if(features.playerHealing()!=(a.playerHealing()!=null)||a.runtime()!=null&&features.playerHealing()!=a.runtime().playerHealingEnabled())throw bad();
                var slow=a.runtime()==null?null:a.runtime().receipts().get(ModernUseHistory.Lane.SLOW);
                if(slow!=null)((ModernSlowReceipt)slow.detail()).validateNpc(npcs,slow.batch().use().revision(),revision,phase);
                if(features.slow()!=a.histories().containsKey(ModernUseHistory.Lane.SLOW)
                        ||(features.slow()&&a.scope()!=null)!=(a.materials()!=null))throw bad();
                if(a.materials()!=null)new ModernSlowTransaction.State(new ModernEquipmentHistory.State(a.runtime().identity(),revision,
                        a.resources(),a.histories(),a.supplies().history(),a.targets(),a.playerHealing()),a.runtime(),a.materials());
            }
        }
        @Override public Capture observation(){return this;}
        boolean hasVip(){return actors.stream().anyMatch(a->a.loadout()!=null&&a.loadout().fighter()!=null&&a.loadout().fighter().vip()!=null);}
        Capture withModes(Modes value){return new Capture(identity,revision,phase,abortReason,room,manifest,features,clock,limits,loadingOriginNanos,actors,value,npcs,result,hasteTargets,slowTargets,armorWorld);}
        World world() {
            if(hasVip())throw new IllegalArgumentException("VIP requires a saved-version and runtime-ability projection");
            return projectWorld(false);
        }
        World vipWorld() {
            if(!hasVip())throw bad();
            return projectWorld(true);
        }
        private World projectWorld(boolean vip) {
            actors.forEach(ModernBattlePresentation::capture);
            return new World(identity,revision,phase,abortReason,room,manifest,features,clock,actors.stream().map(a->new Actor(
                    a.pose(),a.vital(),a.resources(),Spells.from(a,loadingOriginNanos),a.castIntervalNanos(),a.buffs(),a.regeneration(),a.presentation(),PlayerHealing.from(a,loadingOriginNanos),ModernArmorProtectionEffects.State.from(a.armorProtection()))).toList(),modes,npcs,result,
                    hasteTargets==null||features.slow()?null:ModernHasteEffects.State.from(this),features.slow()?ModernSpeedEffects.State.from(this):null,vip);
        }
        OwnerView forOwner(ModernRooms.Scope scope) {
            var a=actors.stream().filter(v->scope.equals(v.scope())).findFirst().orElseThrow(()->new SecurityException("observation owner not present"));
            return owner(a,world());
        }
        /** Validate one common world once before constructing all PRIVATE
         * owner envelopes. This map is server-only, never a player response. */
        Map<ModernRooms.Scope,OwnerView> ownerViews(World world) {
            if(hasVip())throw new IllegalArgumentException("VIP owner projection unavailable");
            return projectOwners(world,false);
        }
        Map<ModernRooms.Scope,OwnerView> vipOwnerViews(World world) {
            if(!hasVip())throw bad();
            return projectOwners(world,true);
        }
        private Map<ModernRooms.Scope,OwnerView> projectOwners(World world,boolean vip) {
            if(world.vip()!=vip)throw bad();
            if(!identity.equals(world.identity())||revision!=world.revision())throw bad();
            var values=new java.util.HashMap<ModernRooms.Scope,OwnerView>();
            for(var a:actors)if(a.scope()!=null&&values.put(a.scope(),owner(a,world))!=null)throw bad();
            return Map.copyOf(values);
        }
        private OwnerView owner(CapturedActor a,World world) {
            var scope=a.scope();ModernSpellRuntime.Snapshot history=null;
            if(a.loadout()!=null) {
                var id=new ModernEquipmentHistory.Identity(identity,scope,new ModernTargetHistory.Participant(a.pose().entityId(),a.pose().member().identity()));
                var state=new ModernEquipmentHistory.State(id,revision,a.resources(),a.histories(),a.supplies()==null?null:a.supplies().history(),a.targets(),a.playerHealing());
                if(!state.profile().equals(a.loadout().historyProfile(a.histories().keySet(),a.supplies()!=null)))throw bad();
                history=new ModernSpellRuntime.Snapshot(state,a.runtime());
            }
            return new OwnerView(world,new PrivateOwner(scope,a.pose().entityId(),a.loadout(),a.skills(),history,a.supplies(),a.weight(),a.ammo(),a.regenClock(),a.materials()));
        }
        @Override public String toString(){return "BattleCapture[redacted]";}
    }
    record PrivateOwner(ModernRooms.Scope scope,long entityId,ModernBattleLoadout loadout,ModernSkills.Snapshot skills,
            ModernSpellRuntime.Snapshot history,Supplies supplies,ModernInventoryWeight.Snapshot weight,ModernAmmo.State ammo,ModernNaturalRegen.Clock regenClock,ModernSpellMaterials.State materials) {
        PrivateOwner(ModernRooms.Scope scope,long entityId,ModernBattleLoadout loadout,ModernSkills.Snapshot skills,
                ModernSpellRuntime.Snapshot history,Supplies supplies,ModernInventoryWeight.Snapshot weight,ModernAmmo.State ammo,ModernNaturalRegen.Clock regenClock) {
            this(scope,entityId,loadout,skills,history,supplies,weight,ammo,regenClock,null);
        }
        @Override public String toString(){return "BattlePrivateOwner[redacted]";}
    }
    record OwnerView(World world,PrivateOwner owner) {
        OwnerView {
            Objects.requireNonNull(world);Objects.requireNonNull(owner);Objects.requireNonNull(owner.scope());
            if(owner.entityId()<1||owner.entityId()>world.actors().size())throw bad();
            var a=world.actors().get(Math.toIntExact(owner.entityId()-1));var id=a.pose().member().identity();
            if(!id.isHuman()||id.sourceId()!=owner.scope().sourceId()||id.id()!=owner.scope().objectId()
                    ||world.features().combat()!=(owner.history()!=null)||world.features().combat()!=(owner.loadout()!=null)
                    ||world.features().items()!=(owner.supplies()!=null)
                    ||world.features().weight()!=(owner.weight()!=null)||world.features().ammo()!=(owner.ammo()!=null)||world.features().slow()!=(owner.materials()!=null)
                        ||world.features().regen()!=(owner.regenClock()!=null)||world.features().spells()!=(owner.skills()!=null))throw bad();
            if(owner.history()!=null) {
                if(world.vip()!=(owner.loadout().fighter().vip()!=null))throw bad();
                if(world.features().armorProtection()!=(owner.loadout().fighter().armorProtection()!=null))throw bad();
                var h=owner.history().history();var r=owner.history().spells();
                if(world.features().playerHealing()!=(h.healing()!=null))throw bad();
                if(h.healing()!=null) {
                    var publicHealing=a.playerHealing();var privateHealing=h.healing();var sent=r.playerHealing();
                    if(publicHealing==null||!privateHealing.incoming().equals(publicHealing.incoming())||privateHealing.outgoing()==null
                            ||!privateHealing.outgoing().spending().equals(publicHealing.outgoing())||(sent==null)!=(publicHealing.receipt()==null))throw bad();
                    if(privateHealing.outgoing().spending().current().cost()!=ModernPlayerHealing.qualify(owner.loadout().fighter()).mpCost())throw bad();
                    if(sent!=null&&(!sent.receipt().impact().equals(publicHealing.receipt().impact())
                            ||sent.casts()!=publicHealing.receipt().casts()||sent.spent()!=publicHealing.receipt().spent()))throw bad();
                }
                if(!h.identity().world().equals(world.identity())||!h.identity().source().equals(owner.scope())||h.identity().actor().entityId()!=owner.entityId()
                        ||h.worldRevision()!=world.revision()||!h.resources().equals(a.resources())||!h.spells().equals(a.spells().histories())
                        ||r.sequence()!=a.spells().sequence()||r.requestRevision()!=a.spells().requestRevision()||r.lawful()!=a.spells().lawful()
                        ||r.initialLawful()!=a.spells().initialLawful()||!r.receipts().keySet().equals(a.spells().receipts().keySet())
                        ||owner.loadout()==null||!h.profile().equals(owner.loadout().historyProfile(h.spells().keySet(),owner.supplies()!=null)))throw bad();
                var source=owner.loadout().fighter().profile().source();
                if(source.sourceId()!=owner.scope().sourceId()||source.character().objectId()!=owner.scope().objectId()
                        ||owner.skills()!=null&&(owner.skills().sourceId()!=owner.scope().sourceId()||!owner.skills().character().equals(source.character())))throw bad();
                for(var entry:r.receipts().entrySet()) {
                    var actual=entry.getValue();var visible=a.spells().receipts().get(entry.getKey());
                    if(!Impact.from(actual.batch()).equals(visible.impact())||actual.casts()!=visible.casts()||actual.spent()!=visible.spent()||!actual.detail().equals(visible.detail()))throw bad();
                }
                var structure=r.structure();
                if(!Objects.equals(structure==null?null:new Structure(structure.casts(),Impact.from(structure.batch()),structure.questKind()),a.spells().structure()))throw bad();
                if(owner.supplies()!=null&&(!owner.supplies().history().equals(h.items())||owner.supplies().requestRevision()>world.revision()))throw bad();
                if(owner.ammo()!=null&&(owner.ammo().entityId()!=owner.entityId()||owner.ammo().revision()>world.revision()
                        ||owner.ammo().attackSequence()>a.vital().attackSequence()))throw bad();
                if(owner.weight()!=null&&!owner.weight().equals(owner.loadout().reweight(owner.supplies().stocks(),
                        owner.ammo()==null?List.of():owner.ammo().stocks(),owner.weight().units(),owner.materials()==null?List.of():owner.materials().stocks())))throw bad();
                if(owner.materials()!=null)new ModernSlowTransaction.State(h,r,owner.materials());
            }
        }
        @Override public String toString(){return "BattleOwnerView[redacted]";}
    }
    private static void validateModes(ModernTargetHistory.World id,long revision,ModernBattleEngine.Phase phase,Clock clock,ModernRooms.RoomSnapshot room,
            ModernWorldMap.Manifest manifest,List<Actor> actors,Modes m,Npcs npcs,ModernMatchSettlement.Event result) {
        int mode=room.summary().config().mode();
        if(m.isolatedForces()&&mode!=2||m.honor().enabled()&&mode!=5||m.quest().progress()!=null&&mode!=2
                ||m.timeAttack().enabled()&&mode!=4||m.siege().enabled()&&mode!=3||m.race()!=null&&mode!=0)throw bad();
        if(result!=null&&mode!=1&&mode!=2&&mode!=5||m.timeAttackResult()!=null&&!m.timeAttack().enabled()
                ||m.siegeResult()!=null&&!m.siege().enabled())throw bad();
        if(m.honor().enabled()) {
            ModernBattleLifeProof.honor(revision,phase,clock,manifest,actors,m.honor().lives());
            ModernBattleHonorProof.validate(revision,phase,manifest,actors,m.honor(),result);
        }
        var scope=new ModernNpcRoster.Scope(id.roomId(),id.matchId(),id.generation());
        if(m.quest().progress()!=null&&!m.quest().progress().scope().equals(scope))throw bad();
        boolean questLives=m.quest().progress()!=null&&m.quest().progress().plan().participantLives();
        if(questLives!=!m.quest().lives().isEmpty())throw bad();
        if(questLives)ModernBattleLifeProof.quest(revision,phase,clock,manifest,actors,m.quest().lives());
        ModernBattleQuestProof.validate(revision,phase,room,manifest,actors,m.isolatedForces(),m.quest(),npcs,result);
        if(m.timeAttack().enabled()) {
            if(!m.timeAttack().state().scope().equals(scope)||m.timeAttack().state().revision()>revision)throw bad();
            ModernBattleTimeAttackProof.validate(revision,phase,clock,room,manifest,actors,m.timeAttack(),npcs);
        }
        if(m.siege().enabled()) {
            var s=m.siege().state();if(!s.scope().equals(new ModernSiege.Scope(id.roomId(),id.matchId(),id.generation()))||s.revision()>revision||m.siege().lives().size()!=actors.size())throw bad();
            ModernBattleLifeProof.siege(revision,phase,clock,manifest,actors,m.siege());
            ModernBattleModeProof.siege(revision,phase,clock,room,manifest,actors,m.siege());
        }
        if(!m.honor().enabled()&&!questLives&&!m.siege().enabled())ModernBattleLifeProof.absent(actors);
        if(m.race()!=null&&(m.race().matchId()!=id.matchId()||m.race().generation()!=id.generation()||m.race().revision()>revision||m.race().entrants().size()!=actors.size()))throw bad();
        ModernBattleRaceProof.validate(revision,phase,clock,room,manifest,actors,m.race(),m.raceResultId());
        if(npcs!=null) {
            var n=npcs.roster();if(!n.scope().equals(scope)||!n.manifest().equals(manifest)||n.revision()>revision||n.closed()!=(phase==ModernBattleEngine.Phase.ABORTED)||n.isolatedForces()!=m.isolatedForces())throw bad();
            long previous=0;for(var e:npcs.exchanges()) {
                if(!e.npc().scope().equals(scope)||e.revision()<=previous||e.revision()>revision||e.npc().serial()>n.spawned()
                        ||actors.stream().noneMatch(a->a.pose().member().identity().equals(e.participant())))throw bad();previous=e.revision();
            }
        }
        for(var event:new ModernMatchSettlement.Event[]{result,m.timeAttackResult(),m.siegeResult()})if(event!=null) {
            if(phase!=ModernBattleEngine.Phase.FINISHED||event.matchId()!=id.matchId()||event.generation()!=id.generation()||event.engineRevision()!=revision
                    ||event.mode()!=mode||event.mapId()!=room.summary().config().mapId()||event.participants().size()!=actors.size())throw bad();
            for(int i=0;i<actors.size();i++)if(!event.participants().get(i).identity().equals(actors.get(i).pose().member().identity())||event.participants().get(i).team()!=actors.get(i).pose().member().team())throw bad();
        }
        ModernBattleModeProof.elimination(revision,actors,result);
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid canonical battle observation");}
    private ModernBattleObservation(){}
}
