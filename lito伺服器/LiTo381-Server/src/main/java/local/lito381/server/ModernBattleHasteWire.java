package local.lito381.server;

import static local.lito381.server.ModernBattleWire.*;

/** Complete schema5 selected by v44+ and explicit CAP_HASTE negotiation.
 * Cooperative TimeAttack+Haste requires v45; old v44 clients reject this combination.
 * Sections 1-13 retain all facts (actors gain lane9/detail7/origin3; owner LHI3).
 * Section14 replaces frozen schema4 motion with complete target/status/motion.
 * No legacy projection, silent loss, unbounded journal or new private scope. */
final class ModernBattleHasteWire {
    static final int SCHEMA=5, SECTION=14, LIMIT=256*1024;
    static final int VERSION=44, COOPERATIVE_VERSION=45;
    static int minimumVersion(ModernMatchDescriptor d) {
        return d.has(ModernMatchDescriptor.TIME_ATTACK)?COOPERATIVE_VERSION:VERSION;
    }
    static boolean negotiated(ModernSessionBoundary.Negotiated p) {
        int required=ModernSessionProtocol.CAP_HASTE|ModernSessionProtocol.CAP_TIMED
                |ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE
                |ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
        return p!=null&&p.version()>=VERSION&&(p.capabilities()&required)==required;
    }
    static final int MAX_BYTES=MAX_PRESENTATION_BYTES+8+LIMIT;
    static void participant(Writer w,ModernTargetHistory.Participant p){w.l(p.entityId());ModernBattleWire.participant(w,p.identity());}
    static void id(Writer w,ModernNpcRoster.Id id){scope(w,id.scope());w.l(id.serial());}
    static void speed(Writer w,ModernNpcSpeed.State s){ModernNpcSpeed.requireNoSlow(s);w.l(s.revision());w.l(s.observedMillis());w.l(s.hasteUntilMillis());}
    static void detail(Writer w,ModernHasteReceipt d){
        w.l(d.activeElapsedNanos());
        if(d.target() instanceof ModernHasteReceipt.Player p){w.u8(0);participant(w,p.participant());w.l(p.life());w.l(p.equipmentRevision());cell(w,p.cell());buff(w,p.before(),true);buff(w,p.after(),true);}
        else {var n=(ModernHasteReceipt.Npc)d.target();w.u8(1);id(w,n.id());w.l(n.life());w.i(n.lane());cell(w,n.cell());w.l(n.rosterRevision());speed(w,n.before());speed(w,n.after());}
    }
    private static void grant(Writer w,ModernHasteEffects.Grant g){participant(w,g.caster());receipt(w,g.receipt());}
    static void effects(Writer w,ModernHasteEffects.State s){
        world(w,s.world());w.l(s.revision());
        w.list(s.targets(),228,(b,e)->{
            if(e.target() instanceof ModernHasteEffects.Player p){b.u8(0);participant(b,p.participant());}
            else {b.u8(1);id(b,((ModernHasteEffects.Npc)e.target()).id());}
            b.optional(e.active(),ModernBattleHasteWire::grant);b.optional(e.walk(),ModernBattleHasteWire::grant);
        });
        w.optional(s.npcs(),(b,n)->{
            scope(b,n.scope());b.l(n.revision());b.l(n.elapsedMillis());b.list(n.actors(),128,(c,m)->{
                id(c,m.actor());c.l(m.life());c.i(m.base().walkMillis());c.i(m.base().attackMillis());speed(c,m.speed());speed(c,m.walkSpeed());
                c.optional(m.walk(),(d,x)->{d.l(x.sequence());d.l(x.life());d.l(x.rosterRevision());d.l(x.elapsedMillis());d.i(x.durationMillis());cell(d,x.from());cell(d,x.to());d.i(x.heading());});
            });
        });
    }
    private ModernBattleHasteWire(){}
}
