package local.lito381.server;

import static local.lito381.server.ModernBattleWire.*;

/** Full schema6 selected only by v46 and explicit CAP_SLOW_DATA.
 * Extends actor/detail buffs and owner LHI4/materials; section14 replaces
 * haste-only motion with both speed lanes, not a lossy legacy projection. */
final class ModernBattleSpeedWire {
    static final int VERSION=46;
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        int required=ModernSessionProtocol.CAP_SLOW_DATA|ModernSessionProtocol.CAP_TIMED
            |ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
        return p!=null&&p.version()>=VERSION&&p.version()<=ModernSessionProtocol.MAX_VERSION
            &&p.numericSchema()==ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1&&(p.capabilities()&required)==required;
    }
    static final int SCHEMA=6, LIMIT=512*1024, MAX_BYTES=MAX_PRESENTATION_BYTES+8+LIMIT;
    static void buff(Writer w,ModernTimedEffects.State s) {
        w.l(s.entityId());w.l(s.revision());w.l(s.observedNanos());w.l(s.hasteUntilNanos());w.l(s.braveUntilNanos());w.l(s.wisdomUntilNanos());
        w.i(s.braveKind());w.e(s.hasteOrigin());w.l(s.slowUntilNanos());w.l(s.slowRevision());
    }
    static void speed(Writer w,ModernNpcSpeed.State s) {
        w.l(s.revision());w.l(s.observedMillis());w.l(s.hasteUntilMillis());w.l(s.slowUntilMillis());w.l(s.slowRevision());
    }
    static void haste(Writer w,ModernHasteReceipt d) {
        w.l(d.activeElapsedNanos());
        if(d.target() instanceof ModernHasteReceipt.Player p) {
            w.u8(0);ModernBattleHasteWire.participant(w,p.participant());w.l(p.life());w.l(p.equipmentRevision());cell(w,p.cell());buff(w,p.before());buff(w,p.after());
        } else {
            var n=(ModernHasteReceipt.Npc)d.target();w.u8(1);ModernBattleHasteWire.id(w,n.id());w.l(n.life());w.i(n.lane());cell(w,n.cell());
            w.l(n.rosterRevision());speed(w,n.before());speed(w,n.after());
        }
    }
    static void slow(Writer w,ModernSlowReceipt d) {
        w.l(d.activeElapsedNanos());w.i(d.probability());w.i(d.roll());w.l(d.equipmentHasteItems());
        w.optional(d.missingEffect(),(b,m)->{b.i(m.npcTemplateId());b.i(m.requiredSkill());});
        if(d.target() instanceof ModernSlowReceipt.Player p) {
            w.u8(0);ModernBattleHasteWire.participant(w,p.participant());w.l(p.life());w.l(p.equipmentRevision());cell(w,p.cell());buff(w,p.before());buff(w,p.after());
        } else {
            var n=(ModernSlowReceipt.Npc)d.target();w.u8(1);ModernBattleHasteWire.id(w,n.id());w.l(n.life());w.i(n.lane());cell(w,n.cell());
            w.l(n.rosterRevision());speed(w,n.before());speed(w,n.after());
        }
    }
    static void materials(Writer w,ModernSpellMaterials.State m) {
        world(w,m.world());player(w,m.scope());ModernBattleHasteWire.participant(w,m.caster());
        w.l(m.revision());w.l(m.uses());w.l(m.lastSequence());w.l(m.lastLife());w.l(m.lastEquipmentRevision());
        w.list(m.stocks(),ModernSpellMaterials.MAX_STOCKS,(b,s)->{b.l(s.itemId());b.l(s.facts().unitWeight());b.l(s.initial());b.l(s.remaining());});
    }
    private static void grant(Writer w,ModernSpeedEffects.Grant g) {
        ModernBattleHasteWire.participant(w,g.caster());receipt(w,g.receipt(),SCHEMA);
    }
    static void effects(Writer w,ModernSpeedEffects.State s) {
        world(w,s.world());w.l(s.revision());
        w.list(s.targets(),228,(b,e)->{
            if(e.target() instanceof ModernSpeedEffects.Player p){b.u8(0);ModernBattleHasteWire.participant(b,p.participant());}
            else {b.u8(1);ModernBattleHasteWire.id(b,((ModernSpeedEffects.Npc)e.target()).id());}
            b.optional(e.active(),ModernBattleSpeedWire::grant);b.optional(e.walk(),ModernBattleSpeedWire::grant);
        });
        w.optional(s.npcs(),(b,n)->{
            scope(b,n.scope());b.l(n.revision());b.l(n.elapsedMillis());
            b.list(n.actors(),128,(c,m)->{
                ModernBattleHasteWire.id(c,m.actor());c.l(m.life());c.i(m.base().walkMillis());c.i(m.base().attackMillis());
                speed(c,m.speed());speed(c,m.walkSpeed());
                c.optional(m.walk(),(d,x)->{d.l(x.sequence());d.l(x.life());d.l(x.rosterRevision());d.l(x.elapsedMillis());d.i(x.durationMillis());cell(d,x.from());cell(d,x.to());d.i(x.heading());});
            });
        });
    }
    private ModernBattleSpeedWire(){}
}
