package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static local.lito381.server.ModernBattleWire.*;
import static local.lito381.server.ModernBattlePublicationTest.*;
import static local.lito381.server.ModernBattleAudienceTest.published;
import static local.lito381.server.ModernMatchDescriptor.PET_RACE;
import local.lito381.server.ModernMatchDescriptor.Ruleset;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Independent framing inspection, not a production semantic decoder. */
class ModernBattleWireTest {
    @TempDir Path temporary;
    static Map<Integer,byte[]> sections(byte[] bytes) {
        assertTrue(bytes.length<=MAX_BYTES);var b=ByteBuffer.wrap(bytes);assertEquals(MAGIC,b.getInt());assertEquals(SCHEMA,b.getShort());assertEquals(0,b.getShort());
        assertEquals(bytes.length,b.getInt());assertEquals(SECTION_COUNT,b.getShort());assertEquals(0,b.getShort());var result=new LinkedHashMap<Integer,byte[]>();
        for(int id=1;id<=SECTION_COUNT;id++){assertEquals(id,b.getShort());assertEquals(0,b.getShort());int size=b.getInt();assertTrue(size>=0&&size<=sectionLimit(id));
            var section=new byte[size];b.get(section);result.put(id,section);}assertFalse(b.hasRemaining());return result;
    }
    static Map<Integer,byte[]> encoded(ModernBattleAudience.View v) {
        var bytes=encode(v);assertArrayEquals(bytes,encode(v));var parts=sections(bytes);var b=ByteBuffer.wrap(parts.get(WORLD));
        assertEquals(v.world().identity().roomId(),b.getLong());assertEquals(v.world().identity().matchId(),b.getLong());assertEquals(v.world().identity().generation(),b.getLong());assertEquals(v.world().revision(),b.getLong());
        var p=ByteBuffer.wrap(parts.get(PERSONAL));assertEquals(v.personal().scope().sourceId(),p.getInt());assertEquals(v.personal().scope().objectId(),p.getLong());assertEquals(v.personal().scope().epoch(),p.getLong());
        assertEquals(v.personal().scope().channelId(),p.getInt());assertEquals(v.personal().entityId(),p.getLong());return parts;
    }
    static ModernBattlePublication publication(ModernBattleObservation.Capture c,ModernNpcCatalog.Catalog catalog) {
        var f=c.features();var m=c.modes();
        int flags=(f.combat()?1:0)|(f.spells()?2:0)|(f.arrow()?4:0)|(f.items()?8:0)|(f.weight()?16:0)|(f.timed()?32:0)|(f.ammo()?64:0)|(f.regen()?128:0)
                |(m.honor().enabled()?256:0)|(f.fire()?512:0)|(m.race()!=null?1024:0)|(c.npcs()!=null?2048:0)|(m.isolatedForces()?4096:0)
                |(m.quest().progress()!=null&&m.quest().progress().plan().objectives()!=null?8192:0)|(!m.quest().lives().isEmpty()?16384:0)
                |(f.npcArrow()?32768:0)|(m.timeAttack().enabled()?65536:0)|(m.siege().enabled()?131072:0)|(f.disintegrate()?262144:0)|(f.npcDisintegrate()?524288:0)|(f.npcFire()?1048576:0)
                |(f.haste()?ModernMatchDescriptor.HASTE:0);
        var rule=m.honor().enabled()?Ruleset.HONOR_TOWERS:m.race()!=null?Ruleset.PET_RACE:m.siege().enabled()?Ruleset.SIEGE_OBJECTIVES:m.timeAttack().enabled()?Ruleset.COOPERATIVE_TIME_ATTACK:
                c.npcs()!=null?Ruleset.NPC_ENCOUNTER:c.result()!=null?Ruleset.DEATHMATCH_ELIMINATION:f.combat()?Ruleset.PVP_SKIRMISH:Ruleset.SCENE_ONLY;
        var l=c.limits();var d=new ModernMatchDescriptor(rule,flags,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
        return new ModernBattlePublication(c,d,catalog==null?null:ModernNpcProtocol.Resources.from(catalog),catalog==null?null:ModernNpcNames.Table.from(catalog));
    }
    @Test void completeRealOwnerPayloadKeepsExactEngineDomainAndDifferentPrivateSections()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture()) {
            f.active();f.cast(0,1,1,3);var capture=f.engine.serviceObservation();var publication=new ModernBattlePublication(capture,honor(capture),null,null);
            var a=publication.audience(f.players.get(0).scope());var b=publication.audience(f.players.get(1).scope());var pa=encoded(a);var pb=encoded(b);
            for(int id=1;id<=SECTION_COUNT;id++)if(id!=PERSONAL)assertArrayEquals(pa.get(id),pb.get(id));
            assertFalse(Arrays.equals(pa.get(PERSONAL),pb.get(PERSONAL)));assertEquals(2,ByteBuffer.wrap(pa.get(ACTORS)).getInt());
            var frozen=encode(a);f.elapsed(2_000_000_000L);f.cast(0,2,1,3);assertArrayEquals(frozen,encode(a));
            assertThrows(SecurityException.class,()->publication.audience(new ModernRooms.Scope(7,3333,99,0)));
            assertEquals(1,ByteBuffer.wrap(pa.get(PERSONAL)).get(32));assertEquals(0,pa.get(RESULT)[0]);
        }
    }
    @Test void everyQuestTerminalEncodesBothSparseAudiencesWithoutOpposingTargets()throws Exception {
        for(int map:BattleRules.Mode.QUEST_BATTLE.maps())try(var f=new ModernNpcServiceTest.Fixture(100,true,false,new ModernQuestServiceTest.Journal(),map)) {
            var last=ModernQuestServiceTest.clearToLast(f);f.service.npc(f.a.owner(),last);stopScheduler(f.service);var p=published(f.service,f.a.owner());
            var a=encoded(p.audience(f.a.scope()));var b=encoded(p.audience(f.b.scope()));
            assertArrayEquals(a.get(RESULT),b.get(RESULT));assertEquals(1,a.get(RESULT)[0]);assertEquals(1,a.get(QUEST)[0]);assertEquals(1,b.get(QUEST)[0]);
            var aa=ByteBuffer.wrap(a.get(ACTORS));var ba=ByteBuffer.wrap(b.get(ACTORS));assertEquals(1,aa.getInt());assertEquals(1,ba.getInt());assertEquals(1,aa.getLong());assertEquals(2,ba.getLong());
            assertEquals(p.audience(f.a.scope()).world().actors().get(0).vital().values().hp(),ByteBuffer.wrap(a.get(ACTORS)).getLong(42));
            assertEquals(ModernNpcServiceTest.HUGE,ByteBuffer.wrap(a.get(ACTORS)).getLong(50));
            assertFalse(Arrays.equals(a.get(NPCS),b.get(NPCS)));assertFalse(Arrays.equals(a.get(QUEST),b.get(QUEST)));assertFalse(Arrays.equals(a.get(PERSONAL),b.get(PERSONAL)));
        }
    }
    @Test void realHundredActorsAndCastsExceedLegacy64KiBWithoutTruncation()throws Exception {
        try(var room=new ModernBattleEngineBotTest.RoomFixture(100,0)) {
            var ticket=room.capture();var prepared=new java.util.ArrayList<ModernBattleEngine.PreparedMember>();
            for(var p:room.prepared(ticket)) {
                int n=prepared.size();prepared.add(new ModernBattleEngine.PreparedMember(p.owner(),p.scope(),p.character(),ModernBattleEngineBotTest.cell(10+(n%10)*2,10+(n/10)*2),
                        p.stepIntervalNanos(),p.fighter(),p.attackIntervalNanos(),p.skills(),p.castIntervalNanos(),p.damageMagic()));
            }
            try(var engine=new ModernBattleEngine(ticket,room.binding,prepared,List.of(),ModernBattleEngineBotTest.LIMITS,()->1_000_000_000L,()->1000,bound->0,false)) {
                for(var p:room.players)engine.ackLoadedObservation(p.owner(),p.scope(),ticket.matchId(),ticket.generation(),room.map.digest());
                for(var p:room.players)assertEquals(ModernSpellProtocol.Reason.NONE,engine.cast(p.owner(),new ModernSpellProtocol.Request(p.scope(),ticket.matchId(),ticket.generation(),1,5,0)).reason());
                var c=engine.serviceObservation();var v=publication(c,null).audience(room.players.get(0).scope());var parts=encoded(v);assertEquals(100,ByteBuffer.wrap(parts.get(ACTORS)).getInt());
                assertTrue(encode(v).length>ModernApplicationProtocol.MAX_FRAME_BYTES,"actual full size="+encode(v).length);assertTrue(encode(v).length<MAX_BYTES);
                assertEquals(100,c.actors().stream().filter(a->a.runtime().receipts().containsKey(ModernUseHistory.Lane.TELEPORT)).count());
            }finally{room.rooms.releasePreparation(ticket);}
        }
        try(var f=new ModernBattleEngineAmmoTest.Fixture()) {
            f.active();f.select(1,40746);f.attack(1);var c=f.engine.serviceObservation();var v=publication(c,null).audience(f.room.players.get(0).scope());
            encoded(v);assertNotNull(v.personal().ammo());
        }
    }
    @Test void actualPlayerAndNpcSkillHistoriesAreInTheCompletePayload()throws Exception {
        try(var f=new ModernBattleEngineHonorTest.Fixture(1000,100,500,1_000_000_000L,0,true,true)) {
            f.active();f.cast(0,1,1,3);f.elapsed(2_000_000_000L);f.cast(0,2,4,2);f.elapsed(4_000_000_000L);f.cast(0,3,70,0);
            var c=f.engine.serviceObservation();encoded(publication(c,null).audience(f.players.get(0).scope()));assertTrue(c.actors().get(0).runtime().receipts().size()>=3);
        }
        try(var f=ModernBattleEngineDisintegrateTest.fixture(true,0,1000,1000,500,true)) {
            f.active();ModernBattleEngineDisintegrateTest.cast(f,1,2);var c=f.engine.serviceObservation();encoded(publication(c,null).audience(f.players.get(0).scope()));
        }
        try(var f=ModernBattleEngineNpcFireStormTest.fixture(1000,10000,0,true,true,false,0)) {
            f.active();var n=f.spawn(0,11,10);ModernBattleEngineNpcMagicTest.cast(f,0,1,n);f.elapsed(1200);ModernBattleEngineNpcFireStormTest.cast(f,2);
            f.elapsed(2400);ModernBattleEngineNpcDisintegrateTest.cast(f,0,3,n);var c=f.engine.serviceObservation();encoded(publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
            assertEquals(3,c.actors().get(0).runtime().receipts().size());
        }
        try(var f=ModernNpcFireStormStructuresTest.quest(513,1)) {
            f.active();ModernNpcFireStormStructuresTest.cast(f,0,1);f.elapsed(2000);ModernNpcFireStormStructuresTest.cast(f,0,2);f.elapsed(4000);ModernNpcFireStormStructuresTest.cast(f,0,3);
            var c=f.engine.serviceObservation();assertNotNull(c.actors().get(0).runtime().structure());encoded(publication(c,f.config.catalog()).audience(f.players.get(0).scope()));
        }
    }
    @Test void cooperativeAndSiegeResultsRetainTheirModeSpecificProofs()throws Exception {
        var journal=new ModernTimeAttackServiceTest.Journal();try(var f=new ModernTimeAttackServiceTest.Fixture(temporary,1,journal)) {
            var frame=f.active();stopScheduler(f.service);var player=f.players.get(0);f.tower(player,frame);for(int n=0;n<3;n++)f.hit(player,frame,n+2);
            var v=published(f.service,player.owner()).audience(player.scope());var parts=encoded(v);assertEquals(1,parts.get(TIME_ATTACK)[0]);assertEquals(1,parts.get(RESULT)[0]);assertNotNull(v.world().result().timeAttack());
            assertTrue(parts.get(RESULT).length>ModernTimeAttackSettlement.PROOF_BYTES);assertEquals(0,parts.get(SIEGE)[0]);
        }
        var siegeJournal=new ModernTimeAttackServiceTest.Journal();try(var f=new ModernSiegeServiceTest.Fixture(0,500,siegeJournal)) {
            f.active();stopScheduler(f.service);ModernNpcServiceTest.await(()->f.world().phase()==ModernWorldProtocol.Phase.FINISHED);
            var v=published(f.service,f.players.get(0).owner()).audience(f.players.get(0).scope());var parts=encoded(v);assertEquals(30,ByteBuffer.wrap(parts.get(ACTORS)).getInt());
            assertEquals(1,parts.get(SIEGE)[0]);assertNotNull(v.world().result().siege());assertTrue(parts.get(RESULT).length>ModernSiegeSettlement.PROOF_BYTES);
        }
    }
    @Test void realRaceCarriesCourseAndResultWithoutFakeEquipmentOrCombat()throws Exception {
        try(var f=new ModernBattleEnginePetRaceTest.Fixture(30_000,1_000_000_000L)) {
            f.active();var first=f.finish(0);f.elapsed(first.race().finishWindowDeadlineElapsedNanos());var c=f.engine.serviceObservation();var l=c.limits();
            var descriptor=new ModernMatchDescriptor(Ruleset.PET_RACE,PET_RACE,2000,l.loadTimeoutMillis(),l.countdownNanos(),l.activeTimeoutMillis());
            var v=new ModernBattlePublication(c,descriptor,null,null).audience(f.a.scope());var parts=encoded(v);
            assertEquals(0,parts.get(PERSONAL)[32]);assertEquals(0,parts.get(RESULT)[0]);assertEquals(0,parts.get(NPCS)[0]);
            var race=ByteBuffer.wrap(parts.get(RACE));assertEquals(1,race.get());assertEquals(v.world().modes().race().revision(),race.getLong());assertEquals(ModernPetRace.Phase.COMPLETE.ordinal(),race.get());
            var digest=new byte[32];race.get(digest);assertEquals(v.world().modes().race().courseSha256(),java.util.HexFormat.of().formatHex(digest));
        }
    }
    @Test void unsignedLimitsFlagsAndUtf8AreCheckedBeforeProducingAnyPayload() {
        var count=new Writer(null);assertThrows(IllegalArgumentException.class,()->count.count(101,100));assertThrows(IllegalArgumentException.class,()->count.u8(256));
        assertThrows(IllegalArgumentException.class,()->count.u16(-1));assertThrows(IllegalArgumentException.class,()->count.text("界".repeat(86),256));
        assertThrows(IllegalArgumentException.class,()->count.hash("g".repeat(64)));assertThrows(IllegalArgumentException.class,()->encode(null));
        var max=new Writer(null);max.bytes(new byte[MAX_PRESENTATION_BYTES]);assertEquals(MAX_PRESENTATION_BYTES,max.position);assertThrows(IllegalArgumentException.class,()->max.u8(0));
        var oversized=new Writer(null);assertThrows(IllegalArgumentException.class,()->oversized.section(WORLD,w->w.bytes(new byte[sectionLimit(WORLD)+1])));
        int sum=HEADER_BYTES+SECTION_COUNT*8;for(int i=1;i<=SECTION_COUNT;i++)sum+=sectionLimit(i);assertEquals(MAX_BYTES,sum);
    }
    static void components(Class<?> c,String expected){assertEquals(expected,String.join(",",Arrays.stream(c.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList()),c.getSimpleName()+" changed: update candidate schema and both codecs");}
    static void tags(Class<? extends Enum<?>> c,String names){assertEquals(names,String.join(",",Arrays.stream(c.getEnumConstants()).map(Enum::name).toList()),c.getSimpleName()+" wire tag order changed");}
    @Test void schemaOnePinsEverySerializedEnumOrder() {
        tags(ModernBattleEngine.Phase.class,"LOADING,COUNTDOWN,ACTIVE,ABORTED,FINISHED");
        tags(ModernBattleEngine.AbortReason.class,"NONE,DISCONNECTED,LOAD_TIMEOUT,ACTIVE_TIMEOUT,SERVER_ABORT,CLOCK_REGRESSED,PREPARATION_CANCELLED");
        tags(Ruleset.class,"SCENE_ONLY,PVP_SKIRMISH,DEATHMATCH_ELIMINATION,HONOR_TOWERS,PET_RACE,NPC_ENCOUNTER,COOPERATIVE_TIME_ATTACK,SIEGE_OBJECTIVES");
        tags(ModernParticipantId.Kind.class,"HUMAN,SERVER_BOT");
        assertEquals("TELEPORT,LIGHT_ARROW,HEALING,FIRE_STORM,DISINTEGRATE,NPC_LIGHT_ARROW,NPC_FIRE_STORM,NPC_DISINTEGRATE",
                ModernEquipmentHistoryTest.LEGACY_LANES.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")));
        assertEquals(8,ModernUseHistory.Lane.GREATER_HASTE.ordinal()); // Appended internal lane, rejected by schema1/2.
        tags(ModernTargetHistory.Kind.class,"DAMAGE,HEAL");tags(ModernQuestObjectives.Kind.class,"ROOM_SYMBOL,FINAL_SYMBOL,FINAL_GUARDIAN");
        tags(ModernQuestProgress.Flow.class,"FINAL_GUARDIAN,ROOM_SYMBOLS");tags(ModernQuestProgress.Status.class,"RUNNING,FINISHED,ABORTED");tags(ModernQuestProgress.Phase.class,"SPAWN_PENDING,FIGHTING,OBJECTIVE");
        tags(ModernTimeAttack.Phase.class,"WAITING_FOR_TOWER,SPAWN_PENDING,FIGHTING,FINISHED,ABORTED");tags(ModernTimeAttack.End.class,"NONE,CLEARED,TIMEOUT,LEADER_DEFEATED,PARTY_DEFEATED");tags(ModernTimeAttack.Kind.class,"ORDINARY,FINAL_BOSSES,DRAGON");
        tags(ModernSiege.Slot.class,"OUTER_LEFT,OUTER_RIGHT,INNER");tags(ModernSiege.Phase.class,"WAITING,OUTER,INNER,FINISHED,ABORTED");tags(ModernSiegeSettlement.End.class,"INNER_DESTROYED,TIMEOUT");
        tags(ModernPetRace.Phase.class,"WAITING,RUNNING,FINISH_WINDOW,COMPLETE,ABORTED");tags(ModernPetRace.Dnf.class,"NONE,LEFT,DISCONNECTED,WINDOW_CLOSED");
        tags(ModernNpcRoster.Action.class,"IDLE,WALK,HURT,DEATH,ATTACK");tags(ModernNpcBattle.Direction.class,"PLAYER_TO_NPC,NPC_TO_PARTICIPANT");tags(ModernMatchSettlement.Verdict.class,"WIN,LOSS");
        tags(ModernCharacterData.ItemKind.class,"WEAPON,ARMOR,ETC");
        assertEquals("TELEPORT,UNIMPLEMENTED,UNSUPPORTED_TEMPLATE,LIGHT_ARROW,BASIC_HEAL,FIRE_STORM,DISINTEGRATE",
                java.util.Arrays.stream(ModernSkills.Support.values()).limit(7).map(Enum::name).collect(java.util.stream.Collectors.joining(",")));
        assertEquals(7,ModernSkills.Support.GREATER_HASTE.ordinal()); // Only the v42 qualified menu catalog carries tag7.
        for(var support:ModernSkills.Support.values())if(support==ModernSkills.Support.GREATER_HASTE||support==ModernSkills.Support.UNSUPPORTED_TEMPLATE)
            assertEquals(ModernSkills.Support.UNIMPLEMENTED,ModernBattleWirePersonal.sourceSupport(new ModernSkills.Skill(54,"has",60,10,support)));
        tags(ModernItemLifetime.State.class,"UNQUALIFIED,PERMANENT,ACTIVE,EXPIRED,UNSTARTED_OR_MISSING,RELATIVE_UNQUALIFIED,LIGHTING_UNQUALIFIED");tags(ModernNaturalRegen.Activity.class,"ATTACK,MOVE,NONE");
    }
    @Test void completeProjectionShapeRequiresExplicitEncodingOrRejection()throws Exception {
        assertEquals("world,personal,npcMotion,armorPresentation",java.util.Arrays.stream(ModernBattleAudience.View.class.getDeclaredFields())
                .filter(f->!java.lang.reflect.Modifier.isStatic(f.getModifiers())).map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.joining(",")));
        components(ModernArmorPresentation.Candidate.class,"world,worldRevision,entityId,life,equipmentRevision,required,batch");
        components(ModernArmorPresentation.Requirements.class,"graphics,icons");
        components(ModernArmorPresentation.Batch.class,"sequence,revision,observedNanos,anchor,visuals");
        components(ModernArmorPresentation.Anchor.class,"moveSequence,cell");
        components(ModernArmorPresentation.Visual.class,"ordinal,graphic,role");
        tags(ModernArmorPresentation.Role.class,"PROC_TARGET,PROC_ACTOR,SKILL_CAST");
        components(ModernNpcMotion.State.class,"scope,revision,elapsedMillis,actors");
        components(ModernNpcMotion.View.class,"actor,life,pace,walk");
        components(ModernNpcMotion.Pace.class,"walkMillis,attackMillis");
        components(ModernNpcMotion.Walk.class,"actor,sequence,life,rosterRevision,elapsedMillis,durationMillis,from,to,heading");
        components(ModernBattleAudience.World.class,"identity,revision,phase,abortReason,room,manifest,descriptor,clock,lane,actors,modes,npcs,result,hasteEffects,speedEffects");
        // Schema5 preserves Haste; candidate schema6 adds complete Slow and
        // reagent facts. Every current negotiated version still refuses Slow.
        components(ModernHasteEffects.State.class,"world,revision,targets,npcs");
        components(ModernHasteEffects.Entry.class,"target,active,walk");
        components(ModernHasteEffects.Grant.class,"caster,receipt");
        components(ModernHasteEffects.Player.class,"participant");
        components(ModernHasteEffects.Npc.class,"id");
        components(ModernHasteEffects.Npcs.class,"scope,revision,elapsedMillis,actors");
        components(ModernHasteEffects.Motion.class,"actor,life,base,speed,walkSpeed,walk");
        components(ModernSpeedEffects.State.class,"world,revision,targets,npcs");
        components(ModernSpeedEffects.Entry.class,"target,active,walk");
        components(ModernSpeedEffects.Grant.class,"caster,receipt");
        components(ModernSpeedEffects.Player.class,"participant");
        components(ModernSpeedEffects.Npc.class,"id");
        components(ModernSpeedEffects.Npcs.class,"scope,revision,elapsedMillis,actors");
        components(ModernSpeedEffects.Motion.class,"actor,life,base,speed,walkSpeed,walk");
        components(ModernSlowReceipt.class,"activeElapsedNanos,probability,roll,equipmentHasteItems,target,missingEffect");
        components(ModernSlowReceipt.Player.class,"participant,life,equipmentRevision,cell,before,after");
        components(ModernSlowReceipt.Npc.class,"id,life,lane,cell,rosterRevision,before,after");
        components(ModernSlowNativeRules.MissingEffect.class,"npcTemplateId,requiredSkill");
        components(ModernSpellRuntime.GreaterHasteNeutralized.class,"activeElapsedNanos,before,after");
        components(ModernTimedEffects.State.class,"entityId,revision,observedNanos,hasteUntilNanos,braveUntilNanos,wisdomUntilNanos,braveKind,hasteOrigin,slowUntilNanos,slowRevision");
        components(ModernNpcSpeed.State.class,"revision,observedMillis,hasteUntilMillis,slowUntilMillis,slowRevision");
        components(ModernSpellMaterials.State.class,"world,scope,caster,revision,uses,lastSequence,lastLife,lastEquipmentRevision,stocks");
        components(ModernSpellMaterials.Stock.class,"facts,initial,remaining");
        components(ModernSpellMaterials.Facts.class,"itemId,unitWeight");
        components(ModernHasteReceipt.class,"activeElapsedNanos,target");
        components(ModernHasteReceipt.Player.class,"participant,life,equipmentRevision,cell,before,after");
        components(ModernHasteReceipt.Npc.class,"id,life,lane,cell,rosterRevision,before,after");
        components(ModernBattleAudience.Personal.class,"scope,entityId,equipment,skills,history,spellBirthRevision,supplies,weight,ammo,regenClock,materials");
        components(ModernBattleAudience.Equipment.class,"source,revision,effective,equipped,lifetimes,evaluatedAtEpochMillis,validUntilEpochMillis");
        components(ModernBattleAudience.Modes.class,"honor,quest,timeAttack,siege,race");
        components(ModernBattleObservation.Actor.class,"pose,vital,resources,spells,castIntervalNanos,buffs,regeneration,presentation,playerHealing,armorProtection");
        // Armor is a server-only candidate, NOT an extra field in schemas1..7.
        // A shape update is allowed only with explicit rejection at every
        // existing encoder; otherwise the new state would silently disappear.
        components(ModernArmorProtectionEffects.State.class,"entityId,life,sequence,revision,observedNanos,untilNanos,effects");
        components(ModernArmorProtectionEffects.Effect.class,"graphics,graphicsMode,appliedProtection,durationSeconds");
        try(var f=ModernBattleEngineArmorProtectionTest.fixture(10000,false)) {
            ModernBattleEngineArmorProtectionTest.active(f);
            var c=f.engine.serviceObservation();var p=ModernArmorProtectionObservationTest.publish(c);var v=p.audience(f.players.get(0).scope());
            // Explicit section17 encoding is required; schema8 retains exact
            // old fields and only selection8 may request the added owner data.
            ModernBattleArmorPresentationWireTest.check(v);
            assertThrows(IllegalArgumentException.class,()->ModernLegacyBattleProjection.from(c));
            assertThrows(IllegalArgumentException.class,()->encode(v));
            assertThrows(IllegalArgumentException.class,()->encodeHaste(v));
            assertThrows(IllegalArgumentException.class,()->encodeSpeed(v));
            assertThrows(IllegalArgumentException.class,()->encodePlayerHealing(v));
            for(int schema=1;schema<=7;schema++){int value=schema;assertThrows(IllegalArgumentException.class,()->encode(v,value));}
            for(int version=1;version<=47;version++){int value=version;assertThrows(IllegalArgumentException.class,()->encodeVersioned(v,value));}
        }
        components(ModernBattleObservation.PlayerHealing.class,"incoming,outgoing,receipt");
        components(ModernBattleObservation.PlayerHealingReceipt.class,"impact,casts,spent,loadingElapsedNanos");
        components(ModernBattlePresentation.State.class,"shot,item");
        components(ModernBattlePresentation.Shot.class,"count,revision,attackSequence,loadingElapsedNanos,life,equipmentRevision,from,target,targetLife,to,sprite,hit");
        components(ModernBattlePresentation.Item.class,"count,revision,loadingElapsedNanos,life,equipmentRevision,cell,effect");
        components(ModernBattleObservation.Spells.class,"sequence,requestRevision,initialLawful,lawful,histories,receipts,structure");
        components(ModernBattleAudience.Quest.class,"revision,flow,status,lanes,targets,objectives,exchanges,lives,winningLane,deathRevision");
        components(ModernBattleAudience.TimeAttack.class,"revision,phase,end,waves,wave,leader,durationMillis,startedMillis,observedMillis,departed,living,tower,towerHp,towerHurtRevision,towerDeathRevision,planSha256,exchanges");
        components(ModernBattleAudience.Race.class,"revision,phase,courseSha256,gates,activeLimitNanos,observedElapsedNanos,firstFinisherEntity,finishWindowDeadlineElapsedNanos,terminalRevision,resultId,runners");
        components(ModernBattleAudience.Npcs.class,"revision,elapsedMillis,spawned,closed,actors,exchanges,resources,names");
        components(ModernBattleAudience.Npc.class,"serial,sourceId,npcId,graphic,lane,stage,heading,action,cell,hp,maximumHp,mp,maximumMp,life,bornRevision,changedRevision,corpseUntilMillis,nextMoveMillis,nextAttackMillis");
        components(ModernMatchSettlement.Event.class,"resultId,matchId,generation,engineRevision,mode,mapId,finishedAtMillis,participants,timeAttack,siege");
    }
}
