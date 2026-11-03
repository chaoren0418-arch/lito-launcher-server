package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import static local.lito381.server.ModernBattleWire.*;
import static local.lito381.server.ModernMatchDescriptor.*;

/** Explicit full-VIP schema10 candidate, never a legacy Audience conversion.
 * Common fields use schema8 layouts (including tagged LHI4/5), independently
 * of whether this human deathmatch enables armor, healing or speed. The new
 * feature bit survives in WORLD. Only explicit FULL_VIP_DATA admits READ;
 * parsing it is not authentication, resource, SQL or command authority. */
final class ModernBattleVipWire {
    static final int SCHEMA=10, SECTION=18, ABILITY_FIELDS=31;
    static final int APPEARANCE_SCHEMA=11, APPEARANCE_SECTION=19, APPEARANCE_LIMIT=4+100*(6*8+4);
    static final int ITEM_LIMIT=8+4*8+ABILITY_FIELDS*8+8+1+8;
    static final int LIMIT=3*8+8+24+8+8+3*8+4+512*ITEM_LIMIT+ABILITY_FIELDS*8+4+512*8+1+8+3+ABILITY_FIELDS*8;
    static final int PRESENTATION_LIMIT=1+ModernBattleArmorPresentationWire.LIMIT;
    static final int MAX_BYTES=ModernBattleArmorWire.MAX_BYTES+8+PRESENTATION_LIMIT+8+LIMIT;
    static final int MAX_APPEARANCE_BYTES=MAX_BYTES+8+APPEARANCE_LIMIT;

    /** A private factory binds descriptor and audience to one validated capture.
     * Caller must still authenticate the exact source/selection scope. */
    static final class Payload {
        private final ModernBattleVipObservation.View view;
        private final ModernMatchDescriptor descriptor;
        private Payload(ModernBattlePublication publication,ModernRooms.Scope scope) {
            Objects.requireNonNull(publication);descriptor=publication.descriptor();
            if(!descriptor.has(FULL_VIP)||!publication.capture().hasVip())throw bad();
            view=publication.vipView(scope);
        }
        ModernBattleVipObservation.View view(){return view;}
        ModernMatchDescriptor descriptor(){return descriptor;}
        void requirePeer(ModernSessionBoundary.Negotiated peer) {
            ModernSessionExtensions.requireContext(peer);
            if(!ModernSessionExtensions.allowsVipData(peer)||!descriptor.supportedBy(peer)
                    ||!ModernLargeMaps.supported(view.world().manifest(),peer))throw bad();
        }
        @Override public String toString(){return "VipWirePayload[redacted]";}
    }
    static Payload candidate(ModernBattleObservation.Capture capture,ModernMatchDescriptor descriptor,ModernRooms.Scope owner) {
        return ModernBattlePublication.fullVip(capture,descriptor).vip(owner);
    }
    static Payload fromPublication(ModernBattlePublication publication,ModernRooms.Scope owner) {return new Payload(publication,owner);}
    static byte[] encode(Payload payload) {
        return encode(payload,false);
    }
    static byte[] encodeSelected(Payload payload,ModernSessionBoundary.Negotiated peer) {
        payload.requirePeer(peer);return encode(payload,ModernSessionExtensions.allowsVipAppearance(peer));
    }
    private static byte[] encode(Payload payload,boolean appearance) {
        int max=appearance?MAX_APPEARANCE_BYTES:MAX_BYTES;
        Objects.requireNonNull(payload);var count=new Writer(null,max);write(count,payload,appearance);
        var bytes=new byte[count.position];var out=new Writer(ByteBuffer.wrap(bytes),max);write(out,payload,appearance);
        if(out.position!=bytes.length)throw bad();out.buffer.putInt(8,bytes.length);return bytes;
    }
    private static void write(Writer w,Payload p,boolean appearance) {
        var v=p.view;var a=v.world();var d=p.descriptor;
        w.i(MAGIC);w.u16(appearance?APPEARANCE_SCHEMA:SCHEMA);w.u16(0);w.i(0);w.u16(appearance?APPEARANCE_SECTION:SECTION);w.u16(0);
        w.section(WORLD,b->{world(b,a.identity());b.l(a.revision());b.e(a.phase());b.e(a.abortReason());b.i(-1);
            b.e(d.ruleset());b.i(d.features());b.i(d.preparationTimeoutMillis());b.i(d.loadTimeoutMillis());b.l(d.countdownNanos());b.i(d.activeTimeoutMillis());
            b.l(a.clock().loadingElapsedNanos());b.l(a.clock().activeElapsedNanos());});
        w.section(ROOM,b->room(b,a.room()));w.section(MANIFEST,b->manifest(b,a.manifest()));
        w.section(ACTORS,b->b.list(a.actors(),100,(c,x)->{int start=c.position;actor(c,x,ModernBattleArmorWire.SCHEMA);if(c.position-start>ACTOR_LIMIT)throw bad();}));
        w.section(ModernBattleWire.HONOR,b->ModernBattleWireModes.honor(b,new ModernBattleObservation.Honor(List.of(),List.of())));
        // Human deathmatch has no foreign-mode or NPC state to discard.
        for(int section=QUEST;section<=NPCS;section++)w.section(section,b->b.flag(false));
        w.section(PERSONAL,b->ModernBattleWirePersonal.putVip(b,v.personal()));
        w.section(RESULT,b->b.optional(a.result(),ModernBattleWireModes::result));
        w.section(PRESENTATION,b->presentation(b,a.actors()));
        w.section(14,ModernBattleHealingWire.STATUS_LIMIT,b->{
            if(a.speedEffects()!=null){b.u8(2);ModernBattleSpeedWire.effects(b,a.speedEffects());}
            else if(a.hasteEffects()!=null){b.u8(1);ModernBattleHasteWire.effects(b,a.hasteEffects());}
            else {b.u8(0);b.flag(false);}
        });
        w.section(15,ModernBattleArmorWire.HEALING_LIMIT,b->{b.flag(a.features().playerHealing());if(a.features().playerHealing())ModernBattleHealingWire.actors(b,a.actors());});
        w.section(16,ModernBattleArmorWire.LIMIT,b->ModernBattleArmorWire.actors(b,a.actors()));
        w.section(17,PRESENTATION_LIMIT,b->b.optional(v.armorPresentation(),ModernBattleArmorPresentationWire::put));
        w.section(SECTION,LIMIT,b->owner(b,v));
        if(appearance)w.section(APPEARANCE_SECTION,APPEARANCE_LIMIT,b->b.list(a.appearances(),100,(c,x)->{
            c.l(x.entityId());c.i(x.sourceId());c.l(x.objectId());c.l(x.life());c.l(x.runtimeAbilityRevision());c.l(x.lifecycleSequence());c.l(x.graphicsId());
        }));
    }
    private static void owner(Writer w,ModernBattleVipObservation.View v) {
        var a=v.world();var p=v.personal();var e=p.equipment();var versions=e.versions();
        world(w,a.identity());w.l(a.revision());player(w,p.scope());w.l(p.entityId());w.l(p.history().resources().life());
        w.l(versions.savedEquipmentRevision());w.l(versions.runtimeAbilityRevision());w.l(versions.lifecycleSequence());
        w.list(e.vipItems(),512,(b,i)->{b.l(i.objectId());var t=i.applied();b.l(t.catalogId());b.l(t.exclusiveType());b.l(t.iconId());b.l(t.polymorphId());abilities(b,t.abilities());
            var l=i.lifetime();b.l(l.itemObjectId());b.e(l.state());b.l(l.expiresAtMillis());});
        abilities(w,e.vipAbilities());w.list(e.iconIds(),512,Writer::l);
        w.optional(e.configuredShape(),(b,s)->{b.l(s.graphicsId());b.flag(s.present());b.flag(s.noDeathExpLoss());b.flag(s.retainsShapeOnCancellation());abilities(b,s.abilities());});
    }
    private static void abilities(Writer w,ModernVipEquipment.Abilities a) {
        if(ModernVipEquipment.Field.values().length!=ABILITY_FIELDS)throw bad();
        for(var f:ModernVipEquipment.Field.values())w.l(a.get(f));
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid full VIP payload");}
    private ModernBattleVipWire(){}
}
