package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Candidate complete battle DATA payload, not an application opcode, command
 * ACK, SQL receipt or SET permission. Only a publication-selected audience can
 * enter this encoder. No legacy frame or Engine revision offset is involved.
 * Schema 1: big endian; signed i64 facts, i32 coordinates/counts, u8 enum tags
 * and presence flags, u16 UTF-8 byte lengths; twelve ordered length-delimited
 * sections. Enum order is pinned by schema tests, never inferred by a peer.
 * A count-only pass checks every section before allocating the exact payload.
 * v35 read framing is separate; gameplay admission and commands are not
 * authorized merely because an LTB1 payload has been transferred. */
final class ModernBattleWire {
    static final int MAGIC=0x4c544231, SCHEMA=1, HEADER_BYTES=16, SECTION_COUNT=12;
    static final int PRESENTATION_SCHEMA=2, PRESENTATION=13, PRESENTATION_LIMIT=4+100*256;
    static final int GREATER_HASTE_SCHEMA=3;
    static final int NPC_MOTION_SCHEMA=4, NPC_MOTION=14, NPC_MOTION_LIMIT=45+128*81;
    static final int WORLD=1, ROOM=2, MANIFEST=3, ACTORS=4, HONOR=5, QUEST=6,
            TIME_ATTACK=7, SIEGE=8, RACE=9, NPCS=10, PERSONAL=11, RESULT=12;
    static final int ACTOR_LIMIT=32*1024;
    // The total bound is the sum of independently bounded sections, not a
    // global application limit. Nothing is truncated to fit a legacy packet.
    private static final int[] SECTION_LIMITS={0,256,64*1024,8*1024,4+100*ACTOR_LIMIT,
            32*1024,32*1024,16*1024,32*1024,8*1024,128*1024,512*1024,32*1024};
    static final int MAX_BYTES=HEADER_BYTES+SECTION_COUNT*8+java.util.Arrays.stream(SECTION_LIMITS).sum();
    static final int MAX_PRESENTATION_BYTES=MAX_BYTES+8+PRESENTATION_LIMIT;
    static final int MAX_NPC_MOTION_BYTES=MAX_PRESENTATION_BYTES+8+NPC_MOTION_LIMIT;
    static final int MAX_WIRE_BYTES=ModernBattleSpeedWire.MAX_BYTES;
    /** One lossless payload selector for every canonical reply family. Only
     * an explicit selected extension can carry schema7/8. No flag stripping. */
    static final class Encoding {
        private final int version;
        private final ModernSessionBoundary.Negotiated peer;
        private Encoding(int version,ModernSessionBoundary.Negotiated peer){this.version=version;this.peer=peer;}
        static Encoding legacy(int version){return new Encoding(version,null);}
        static Encoding selected(ModernSessionBoundary.Negotiated peer){
            ModernSessionExtensions.requireContext(peer);return new Encoding(peer.version(),peer);
        }
        int version(){return version;}
        int maximum(){return peer!=null&&ModernSessionExtensions.allowsVipAppearance(peer)?ModernBattleVipWire.MAX_APPEARANCE_BYTES
                :peer!=null&&ModernSessionExtensions.allowsVipData(peer)?ModernBattleVipWire.MAX_BYTES
                :peer!=null&&ModernSessionExtensions.allowsArmorPresentation(peer)?ModernBattleArmorPresentationWire.MAX_BYTES
                :peer!=null&&ModernSessionExtensions.allowsArmor(peer)?ModernBattleArmorWire.MAX_BYTES
                :peer!=null&&ModernSessionExtensions.allowsData(peer)?ModernBattleHealingWire.MAX_BYTES:MAX_WIRE_BYTES;}
        int resultLimit(int legacyLimit){return Math.addExact(legacyLimit,maximum()-MAX_WIRE_BYTES);}
        byte[] payloadVip(ModernBattleVipWire.Payload payload) {
            if(peer==null||payload==null)throw bad();
            return ModernBattleVipWire.encodeSelected(payload,peer);
        }
        byte[] payload(ModernBattleAudience.View view) {
            if(peer==null)return encodeVersioned(view,version);
            if(view==null||!view.world().descriptor().supportedBy(peer)||!ModernLargeMaps.supported(view.world().manifest(),peer))throw bad();
            return view.world().descriptor().has(ModernMatchDescriptor.ARMOR_PROTECTION)?
                    (ModernSessionExtensions.allowsArmorPresentation(peer)?ModernBattleArmorPresentationWire.encode(view):encodeArmorProtection(view))
                    :view.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING)?encodePlayerHealing(view):encodeVersioned(view,version);
        }
    }
    static int sectionLimit(int id){if(id==NPC_MOTION)return NPC_MOTION_LIMIT;if(id==PRESENTATION)return PRESENTATION_LIMIT;if(id<1||id>SECTION_COUNT)throw bad();return SECTION_LIMITS[id];}

    static byte[] encode(ModernBattleAudience.View view) {
        return encode(view,SCHEMA);
    }
    static byte[] encode(ModernBattleAudience.View view,int schema) {
        if(schema==ModernBattleHasteWire.SCHEMA||schema==ModernBattleSpeedWire.SCHEMA||schema==ModernBattleHealingWire.SCHEMA||schema==ModernBattleArmorWire.SCHEMA)throw bad(); // Explicit candidate API only.
        return encode(view,schema,null);
    }
    static byte[] encodePlayerHealing(ModernBattleAudience.View view) {
        return encode(view,ModernBattleHealingWire.SCHEMA,null);
    }
    /** Candidate only: no existing version or selected extension admits it. */
    static byte[] encodeArmorProtection(ModernBattleAudience.View view) {
        return encode(view,ModernBattleArmorWire.SCHEMA,null);
    }
    static byte[] encodeSpeed(ModernBattleAudience.View view) {
        return encode(view,ModernBattleSpeedWire.SCHEMA,null);
    }
    static byte[] encodeHaste(ModernBattleAudience.View view) {
        return encode(view,ModernBattleHasteWire.SCHEMA,null);
    }
    /** All canonical replies use this one negotiated-version choice. v43
     * BATTLE_DATA always carries the complete motion extension, including the
     * explicit no-NPC state; versions through 42 keep their exact old bytes. */
    static byte[] encodeVersioned(ModernBattleAudience.View view,int version) {
        if(view==null||view.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING))throw bad();
        if(version<ModernBattleReadProtocol.VERSION||version>ModernSessionProtocol.MAX_VERSION)throw bad();
        if(view.world().descriptor().has(ModernMatchDescriptor.SLOW)) {
            if(version<ModernBattleSpeedWire.VERSION)throw bad();
            return encodeSpeed(view);
        }
        if(view.world().descriptor().has(ModernMatchDescriptor.HASTE)) {
            if(version<ModernBattleHasteWire.minimumVersion(view.world().descriptor()))throw bad();
            return encodeHaste(view);
        }
        return version>=ModernNpcMotion.VERSION?encodeNpcMotion(ModernNpcMotion.Payload.selected(view))
                :encode(view,ModernGreaterHasteProtocol.dataSchema(view,version));
    }
    /** Complete schema4 codec; encodeVersioned owns transport selection. */
    static byte[] encodeNpcMotion(ModernNpcMotion.Payload payload) {
        if(payload==null)throw bad();
        var view=payload.audience();
        int base=view.world().descriptor().has(ModernMatchDescriptor.GREATER_HASTE)?GREATER_HASTE_SCHEMA:PRESENTATION_SCHEMA;
        return encode(view,base,payload);
    }
    private static byte[] encode(ModernBattleAudience.View view,int schema,ModernNpcMotion.Payload motion) {
        boolean armor=schema==ModernBattleArmorWire.SCHEMA;
        if(view==null||armor!=view.world().descriptor().has(ModernMatchDescriptor.ARMOR_PROTECTION))throw bad();
        boolean healing=schema==ModernBattleHealingWire.SCHEMA, combined=healing||armor;
        if(!armor&&healing!=view.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING)||combined&&motion!=null)throw bad();
        boolean speed=schema==ModernBattleSpeedWire.SCHEMA||combined&&view.world().descriptor().has(ModernMatchDescriptor.SLOW);
        boolean targeted=schema==ModernBattleHasteWire.SCHEMA||combined&&!speed&&view.world().descriptor().has(ModernMatchDescriptor.HASTE);
        if(schema!=SCHEMA&&schema!=PRESENTATION_SCHEMA&&schema!=GREATER_HASTE_SCHEMA&&!targeted&&!speed&&!combined)throw bad();
        if(view==null||speed!=view.world().descriptor().has(ModernMatchDescriptor.SLOW)||speed!=(view.world().speedEffects()!=null)
                ||speed!=(view.personal().materials()!=null)
                ||!speed&&view.world().actors().stream().anyMatch(a->a.spells().histories().containsKey(ModernUseHistory.Lane.SLOW)))throw bad();
        if(targeted!=(view.world().hasteEffects()!=null)||!speed&&targeted!=view.world().descriptor().has(ModernMatchDescriptor.HASTE))throw bad();
        if(!targeted&&!speed&&view.world().actors().stream().anyMatch(a->a.spells().histories().containsKey(ModernUseHistory.Lane.HASTE)
                ||a.buffs()!=null&&a.buffs().hasteOrigin()==ModernTimedEffects.HasteOrigin.HASTE))throw bad();
        boolean haste=view.world().descriptor().has(ModernMatchDescriptor.GREATER_HASTE);
        if(!combined&&!targeted&&!speed&&(schema==GREATER_HASTE_SCHEMA)!=haste)throw bad();
        if(!haste&&view.world().actors().stream().anyMatch(a->a.spells().histories().containsKey(ModernUseHistory.Lane.GREATER_HASTE)
                ||a.buffs()!=null&&a.buffs().hasteOrigin()==ModernTimedEffects.HasteOrigin.GREATER_HASTE))throw bad();
        int limit=armor?ModernBattleArmorWire.MAX_BYTES:healing?ModernBattleHealingWire.MAX_BYTES:speed?ModernBattleSpeedWire.MAX_BYTES:targeted?ModernBattleHasteWire.MAX_BYTES:motion==null?MAX_PRESENTATION_BYTES:MAX_NPC_MOTION_BYTES;
        var count=new Writer(null,limit);write(count,view,schema,motion);
        var bytes=new byte[count.position];var out=new Writer(ByteBuffer.wrap(bytes),limit);write(out,view,schema,motion);
        if(out.position!=bytes.length)throw bad();out.buffer.putInt(8,bytes.length);return bytes;
    }
    private static void write(Writer w,ModernBattleAudience.View v,int schema,ModernNpcMotion.Payload motion) {
        boolean targeted=schema==ModernBattleHasteWire.SCHEMA, speed=schema==ModernBattleSpeedWire.SCHEMA;
        w.i(MAGIC);w.u16(motion==null?schema:NPC_MOTION_SCHEMA);w.u16(0);w.i(0);w.u16(schema==ModernBattleArmorWire.SCHEMA?16:schema==ModernBattleHealingWire.SCHEMA?15:motion!=null||targeted||speed?NPC_MOTION:schema==SCHEMA?SECTION_COUNT:PRESENTATION);w.u16(0);
        var a=v.world();
        w.section(WORLD,b->{world(b,a.identity());b.l(a.revision());b.e(a.phase());b.e(a.abortReason());b.i(a.lane());
            var d=a.descriptor();b.e(d.ruleset());b.i(d.features());b.i(d.preparationTimeoutMillis());b.i(d.loadTimeoutMillis());b.l(d.countdownNanos());b.i(d.activeTimeoutMillis());
            b.l(a.clock().loadingElapsedNanos());b.l(a.clock().activeElapsedNanos());});
        w.section(ROOM,b->room(b,a.room()));w.section(MANIFEST,b->manifest(b,a.manifest()));
        w.section(ACTORS,b->b.list(a.actors(),100,(c,x)->{int start=c.position;actor(c,x,schema);if(c.position-start>ACTOR_LIMIT)throw bad();}));
        w.section(HONOR,b->ModernBattleWireModes.honor(b,a.modes().honor()));
        w.section(QUEST,b->b.optional(a.modes().quest(),ModernBattleWireModes::quest));
        w.section(TIME_ATTACK,b->b.optional(a.modes().timeAttack(),ModernBattleWireModes::time));
        w.section(SIEGE,b->b.optional(a.modes().siege().enabled()?a.modes().siege():null,ModernBattleWireModes::siege));
        w.section(RACE,b->b.optional(a.modes().race(),ModernBattleWireModes::race));
        w.section(NPCS,b->b.optional(a.npcs(),ModernBattleWireModes::npcs));
        w.section(PERSONAL,b->ModernBattleWirePersonal.put(b,v.personal(),schema));
        w.section(RESULT,b->b.optional(a.result(),ModernBattleWireModes::result));
        // Schema 1 deliberately keeps its original twelve sections/bytes.
        // v36/schema 2 adds only public events, with the SAME filtered actors.
        if(schema>=PRESENTATION_SCHEMA)w.section(PRESENTATION,b->presentation(b,a.actors()));
        if(motion!=null)w.section(NPC_MOTION,b->b.optional(motion.state(),(c,s)->{
            scope(c,s.scope());c.l(s.revision());c.l(s.elapsedMillis());
            c.list(s.actors(),128,(d,n)->{
                d.l(n.actor().serial());d.l(n.life());d.i(n.pace().walkMillis());d.i(n.pace().attackMillis());
                d.optional(n.walk(),(e,x)->{
                    e.l(x.sequence());e.l(x.life());e.l(x.rosterRevision());e.l(x.elapsedMillis());e.i(x.durationMillis());
                    cell(e,x.from());cell(e,x.to());e.i(x.heading());
                });
            });
        }));
        if(targeted)w.section(NPC_MOTION,ModernBattleHasteWire.LIMIT,b->ModernBattleHasteWire.effects(b,a.hasteEffects()));
        if(speed)w.section(NPC_MOTION,ModernBattleSpeedWire.LIMIT,b->ModernBattleSpeedWire.effects(b,a.speedEffects()));
        if(schema==ModernBattleHealingWire.SCHEMA)ModernBattleHealingWire.sections(w,v);
        if(schema==ModernBattleArmorWire.SCHEMA)ModernBattleArmorWire.sections(w,v);
    }
    static void presentation(Writer w,List<ModernBattleObservation.Actor> actors) {
        w.list(actors,100,(c,x)->{
            c.l(x.pose().entityId());var p=x.presentation();
            c.optional(p.shot(),(d,s)->{d.l(s.count());d.l(s.revision());d.l(s.attackSequence());d.l(s.loadingElapsedNanos());d.l(s.life());d.l(s.equipmentRevision());
                cell(d,s.from());target(d,s.target());d.l(s.targetLife());cell(d,s.to());d.i(s.sprite());d.flag(s.hit());});
            c.optional(p.item(),(d,s)->{d.l(s.count());d.l(s.revision());d.l(s.loadingElapsedNanos());d.l(s.life());d.l(s.equipmentRevision());cell(d,s.cell());d.i(s.effect());});
        });
    }
    static void world(Writer w,ModernTargetHistory.World v){w.l(v.roomId());w.l(v.matchId());w.l(v.generation());}
    static void scope(Writer w,ModernNpcRoster.Scope v){w.l(v.roomId());w.l(v.matchId());w.l(v.generation());}
    static void player(Writer w,ModernRooms.Scope s){w.i(s.sourceId());w.l(s.objectId());w.l(s.epoch());w.i(s.channelId());}
    static void participant(Writer w,ModernParticipantId v){w.e(v.kind());w.i(v.sourceId());w.l(v.id());}
    static void cell(Writer w,ModernWorldMap.Cell v){w.i(v.x());w.i(v.y());}
    static void vitals(Writer w,ModernEquipmentVitals.Vitals v){w.l(v.hp());w.l(v.maximumHp());w.l(v.mp());w.l(v.maximumMp());}
    static void room(Writer w,ModernRooms.RoomSnapshot v) {
        var s=v.summary();var c=s.config();w.l(s.id());w.l(s.revision());w.text(c.name(),64);w.i(c.capacity());w.i(c.mode());w.i(c.mapId());w.i(c.attributes());
        w.i(s.users());w.i(s.ready());w.flag(s.readySatisfied());w.i(v.owner().sourceId());w.l(v.owner().objectId());
        w.list(v.members(),100,(b,m)->{participant(b,m.identity());b.text(m.name(),256);b.text(m.sourceName(),256);b.l(m.level());
            b.i(m.characterClass());b.i(m.sex());b.i(m.team());b.flag(m.ready());});
    }
    static void manifest(Writer w,ModernWorldMap.Manifest v) {
        w.i(v.mapId());w.hash(v.digest());w.list(v.sectors(),64,(b,s)->{b.i(s.key().x());b.i(s.key().y());b.i(s.byteLength());b.hash(s.rawSha256());});
    }
    static void actor(Writer w,ModernBattleObservation.Actor a,int schema) {
        var p=a.pose();w.l(p.entityId());cell(w,p.position());w.i(p.heading());w.flag(p.loaded());w.l(p.lastMoveSequence());w.l(p.stepIntervalNanos());
        // Member is referenced by global entity ID into ROOM, not repeated or
        // renumbered when an isolated force has sparse visible entity IDs.
        w.optional(a.vital(),(b,v)->{vitals(b,v.values());b.l(v.ac());b.i(v.weaponType());b.l(v.attackSequence());b.l(v.attackRevision());
            b.l(v.hurtRevision());b.l(v.deathRevision());b.l(v.target());b.l(v.attackIntervalNanos());});
        w.optional(a.resources(),ModernBattleWire::resources);spells(w,a.spells(),schema);w.l(a.castIntervalNanos());
        w.optional(a.buffs(),(b,v)->{if(fullSpeed(schema))ModernBattleSpeedWire.buff(b,v);else buff(b,v,schema==GREATER_HASTE_SCHEMA||schema==ModernBattleHasteWire.SCHEMA);});
        w.optional(a.regeneration(),(b,v)->{axis(b,v.hp());axis(b,v.mp());b.l(v.itemGain());b.l(v.damageLoss());});
    }
    static void resources(Writer w,ModernVitalBalance.State v) {
        w.l(v.revision());w.l(v.equipmentRevision());w.l(v.life());w.l(v.birthHp());w.l(v.birthMp());vitals(w,v.vitals());var f=v.flows();
        w.l(f.hpGain());w.l(f.hpLoss());w.l(f.restored());w.l(f.damaged());w.l(f.hpGranted());w.l(f.mpGain());w.l(f.mpLoss());w.l(f.spent());w.l(f.mpGranted());w.l(f.hpClipped());w.l(f.mpClipped());
    }
    private static void axis(Writer w,ModernBattleObservation.RegenAxis v){w.l(v.gain());w.l(v.loss());w.l(v.revision());w.l(v.before());w.l(v.after());}
    private static void spells(Writer w,ModernBattleObservation.Spells v,int schema) {
        w.l(v.sequence());w.l(v.requestRevision());w.l(v.initialLawful());w.l(v.lawful());w.count(v.histories().size(),fullSpeed(schema)?11:schema==ModernBattleHasteWire.SCHEMA?10:schema==GREATER_HASTE_SCHEMA?9:8);
        for(var lane:ModernUseHistory.Lane.values())if(v.histories().containsKey(lane)) {
            w.e(lane);var h=v.histories().get(lane);ledger(w,h.current());w.optional(h.last(),ModernBattleWire::cast);
            w.optional(v.receipts().get(lane),(b,r)->receipt(b,r,schema));
        }
        w.optional(v.structure(),(b,s)->{b.l(s.casts());impact(b,s.impact());b.optional(s.questKind(),Writer::e);});
    }
    static void receipt(Writer w,ModernBattleObservation.SpellReceipt r){receipt(w,r,ModernBattleHasteWire.SCHEMA);}
    static void receipt(Writer w,ModernBattleObservation.SpellReceipt r,int schema){impact(w,r.impact());w.l(r.casts());w.l(r.spent());w.l(r.loadingElapsedNanos());detail(w,r.detail(),schema);}
    static void buff(Writer w,ModernTimedEffects.State v,boolean origin){ModernTimedEffects.requireNoSlow(v);w.l(v.entityId());w.l(v.revision());w.l(v.observedNanos());w.l(v.hasteUntilNanos());w.l(v.braveUntilNanos());w.l(v.wisdomUntilNanos());w.i(v.braveKind());if(origin)w.e(v.hasteOrigin());}
    static void ledger(Writer w,ModernSkillSpending.Ledger l) {
        w.l(l.equipmentRevision());w.l(l.profileRevision());w.i(l.cost());w.l(l.priorCasts());w.l(l.priorSpent());w.l(l.casts());w.l(l.spent());w.l(l.lastCastRevision());w.l(l.lastCastSequence());
    }
    static void anchor(Writer w,ModernUseHistory.Anchor a){w.l(a.equipmentRevision());w.l(a.life());vitals(w,a.vitals());w.i(a.x());w.i(a.y());}
    static void cast(Writer w,ModernUseHistory.Cast c){w.l(c.revision());w.l(c.sequence());anchor(w,c.before());w.i(c.cost());w.l(c.mpAfter());}
    private static void impact(Writer w,ModernBattleObservation.Impact v) {
        w.e(v.lane());cast(w,v.use());int maximum=switch(v.lane()){case TELEPORT,GREATER_HASTE,HASTE,SLOW->0;case FIRE_STORM->101;case NPC_FIRE_STORM->48;default->1;};
        w.list(v.targets(),maximum,ModernBattleWire::effect);
    }
    static void effect(Writer w,ModernTargetHistory.Effect e) {
        var h=e.before();target(w,h.target());
        w.e(e.kind());w.l(h.life());w.l(h.equipmentRevision());cell(w,h.cell());w.l(h.hp());w.l(h.maximumHp());w.l(e.raw());w.l(e.applied());w.l(e.hpAfter());
    }
    static void target(Writer w,ModernTargetHistory.Target t) {
        if(t instanceof ModernTargetHistory.Participant p){w.u8(0);w.l(p.entityId());participant(w,p.identity());}
        else if(t instanceof ModernTargetHistory.Npc n){w.u8(1);scope(w,n.id().scope());w.l(n.id().serial());}
        else if(t instanceof ModernTargetHistory.Honor o){w.u8(2);w.l(o.entityId());w.i(o.team());}
        else if(t instanceof ModernTargetHistory.Quest q){w.u8(3);scope(w,q.id().scope());w.i(q.id().slot());w.l(q.bornRevision());}
        else if(t instanceof ModernTargetHistory.TimeTower q){w.u8(4);scope(w,q.id().scope());}
        else throw bad();
    }
    private static void detail(Writer w,ModernSpellRuntime.Detail d,int schema) {
        if(d==ModernSpellRuntime.Ordinary.INSTANCE)w.u8(0);
        else if(d instanceof ModernSpellRuntime.Arrow a){w.u8(1);w.flag(a.critical());}
        else if(d instanceof ModernSpellRuntime.FireStorm a){w.u8(2);w.longs(a.damageLossAfter(),101);}
        else if(d instanceof ModernSpellRuntime.NpcFireStorm a){w.u8(3);w.optional(a.questKind(),Writer::e);}
        else if(d instanceof ModernSpellRuntime.Teleport a){w.u8(4);cell(w,a.destination());}
        else if(d instanceof ModernSpellRuntime.Disintegrate a){w.u8(5);w.flag(a.critical());w.l(a.lawfulBefore());w.l(a.lawfulAfter());w.l(a.damageLossAfter());}
        else if(d instanceof ModernSpellRuntime.GreaterHaste a){w.u8(6);w.l(a.activeElapsedNanos());w.l(a.hasteUntilNanos());}
        else if(d instanceof ModernHasteReceipt a){w.u8(7);if(fullSpeed(schema))ModernBattleSpeedWire.haste(w,a);else ModernBattleHasteWire.detail(w,a);}
        else if(fullSpeed(schema)&&d instanceof ModernSlowReceipt a){w.u8(8);ModernBattleSpeedWire.slow(w,a);}
        else if(fullSpeed(schema)&&d instanceof ModernSpellRuntime.GreaterHasteNeutralized a){w.u8(9);w.l(a.activeElapsedNanos());ModernBattleSpeedWire.buff(w,a.before());ModernBattleSpeedWire.buff(w,a.after());}
        else throw bad();
    }
    static void outcome(Writer w,ModernPhysicalCombat.Outcome o) {
        w.flag(o.hit());w.l(o.rawDamage());w.l(o.appliedDamage());w.l(o.remainingHp());w.flag(o.death());w.flag(o.maximumTriggered());w.flag(o.criticalTriggered());
    }
    static final class Writer {
        final ByteBuffer buffer;final int limit;int position;
        Writer(ByteBuffer buffer){this(buffer,MAX_PRESENTATION_BYTES);}
        Writer(ByteBuffer buffer,int limit){this.buffer=buffer;this.limit=limit;}
        private void reserve(int n){if(n<0||n>limit-position)throw bad();position+=n;}
        void i(int v){reserve(4);if(buffer!=null)buffer.putInt(v);}
        void l(long v){reserve(8);if(buffer!=null)buffer.putLong(v);}
        void u8(int v){if(v<0||v>255)throw bad();reserve(1);if(buffer!=null)buffer.put((byte)v);}
        void u16(int v){if(v<0||v>65535)throw bad();reserve(2);if(buffer!=null)buffer.putShort((short)v);}
        void flag(boolean v){u8(v?1:0);}void e(Enum<?> v){u8(v.ordinal());}
        void bytes(byte[] v){reserve(v.length);if(buffer!=null)buffer.put(v);}
        void text(String v,int maximum){ModernRooms.text(v,maximum,true);var b=v.getBytes(StandardCharsets.UTF_8);u16(b.length);bytes(b);}
        void hash(String v){if(v==null||!v.matches("[0-9a-f]{64}"))throw bad();bytes(HexFormat.of().parseHex(v));}
        void uuid(UUID v){l(v.getMostSignificantBits());l(v.getLeastSignificantBits());}
        void count(int n,int max){if(n<0||n>max)throw bad();i(n);}
        <T> void optional(T v,BiConsumer<Writer,T> put){flag(v!=null);if(v!=null)put.accept(this,v);}
        <T> void list(List<T> v,int maximum,BiConsumer<Writer,T> put){count(v.size(),maximum);for(var x:v)put.accept(this,x);}
        void longs(List<Long> v,int maximum){list(v,maximum,Writer::l);}
        void cells(List<ModernWorldMap.Cell> v,int maximum){list(v,maximum,ModernBattleWire::cell);}
        void section(int id,Consumer<Writer> put){section(id,sectionLimit(id),put);}
        void section(int id,int maximum,Consumer<Writer> put){u16(id);u16(0);int mark=position;i(0);int start=position;put.accept(this);
            int size=position-start;if(size>maximum)throw bad();if(buffer!=null)buffer.putInt(mark,size);}
    }
    static IllegalArgumentException bad(){return new IllegalArgumentException("invalid complete battle data payload");}
    static boolean fullSpeed(int schema){return schema==ModernBattleSpeedWire.SCHEMA||schema==ModernBattleHealingWire.SCHEMA||schema==ModernBattleArmorWire.SCHEMA;}
    private ModernBattleWire(){}
}
