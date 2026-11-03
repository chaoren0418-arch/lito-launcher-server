package local.lito381.server;

import static local.lito381.server.ModernBattleWire.*;

/** Complete schema8 candidate, deliberately unavailable to live negotiation.
 * Sections1-13 retain full speed layouts; owner history explicitly tags LHI4/5.
 * Section14 is the existing tagged status, 15 optional complete healing, and
 * 16 one armor entry for EACH visible actor (null for bots). No proc row IDs,
 * inventory IDs or match nonce occur in the public armor section. */
final class ModernBattleArmorWire {
    static final int SCHEMA=8, SECTION=16, MAX_EFFECTS=1024;
    static final int STATE_LIMIT=6*8+4+MAX_EFFECTS*(8+8+1+8);
    static final int LIMIT=4+100*(8+1+STATE_LIMIT);
    static final int HEALING_LIMIT=1+ModernBattleHealingWire.LIMIT;
    static final int MAX_BYTES=MAX_PRESENTATION_BYTES+8+ModernBattleHealingWire.STATUS_LIMIT+8+HEALING_LIMIT+8+LIMIT;
    static void sections(Writer w,ModernBattleAudience.View v) {
        ModernBattleHealingWire.statusSection(w,v);
        w.section(15,HEALING_LIMIT,b->{
            boolean healing=v.world().descriptor().has(ModernMatchDescriptor.PLAYER_HEALING);
            b.flag(healing);if(healing)ModernBattleHealingWire.actors(b,v);
        });
        w.section(SECTION,LIMIT,b->actors(b,v.world().actors()));
    }
    static void actors(Writer b,java.util.List<ModernBattleObservation.Actor> actors) {
        b.list(actors,100,(c,a)->{
            c.l(a.pose().entityId());c.optional(a.armorProtection(),(d,s)->{
                d.l(s.entityId());d.l(s.life());d.l(s.sequence());d.l(s.revision());d.l(s.observedNanos());d.l(s.untilNanos());
                d.list(s.effects(),MAX_EFFECTS,(e,f)->{e.l(f.graphics());e.l(f.graphicsMode());e.flag(f.appliedProtection());e.l(f.durationSeconds());});
            });
        });
    }
    private ModernBattleArmorWire(){}
}
