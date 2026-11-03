package local.lito381.server;

import static local.lito381.server.ModernBattleWire.*;

/** Schema7 complete candidate. Not negotiated by any HELLO/application version.
 * Sections1-13 use full speed field layouts and LHI5 owner history. Section14
 * is a tagged, mandatory motion/status state; section15 contains one healing
 * entry per visible actor in exactly the same order, including empty ledgers.
 * Public receipts contain no source selection epoch, private inventory or SQL. */
final class ModernBattleHealingWire {
    static final int SCHEMA=7, SECTION=15, LIMIT=4+100*1024;
    static final int STATUS_LIMIT=1+ModernBattleSpeedWire.LIMIT;
    static final int MAX_BYTES=MAX_PRESENTATION_BYTES+8+STATUS_LIMIT+8+LIMIT;
    static void sections(Writer w,ModernBattleAudience.View v) {
        statusSection(w,v);
        w.section(SECTION,LIMIT,b->actors(b,v));
    }
    static void statusSection(Writer w,ModernBattleAudience.View v) {
        var a=v.world();
        w.section(14,STATUS_LIMIT,b->{
            if(a.speedEffects()!=null){b.u8(2);ModernBattleSpeedWire.effects(b,a.speedEffects());}
            else if(a.hasteEffects()!=null){b.u8(1);ModernBattleHasteWire.effects(b,a.hasteEffects());}
            else {b.u8(0);b.optional(v.npcMotion(),(c,s)->{
                scope(c,s.scope());c.l(s.revision());c.l(s.elapsedMillis());
                c.list(s.actors(),128,(d,n)->{d.l(n.actor().serial());d.l(n.life());d.i(n.pace().walkMillis());d.i(n.pace().attackMillis());
                    d.optional(n.walk(),(e,x)->{e.l(x.sequence());e.l(x.life());e.l(x.rosterRevision());e.l(x.elapsedMillis());e.i(x.durationMillis());cell(e,x.from());cell(e,x.to());e.i(x.heading());});});
            });}
        });
    }
    static void actors(Writer b,ModernBattleAudience.View v) {
        actors(b,v.world().actors());
    }
    static void actors(Writer b,java.util.List<ModernBattleObservation.Actor> actors) {
        b.list(actors,100,(c,x)->{
            c.l(x.pose().entityId());var h=x.playerHealing();var i=h.incoming();
            world(c,i.world());person(c,i.recipient());c.l(i.received());c.l(i.restored());c.optional(i.last(),ModernBattleHealingWire::impact);
            c.optional(h.outgoing(),(d,o)->{ledger(d,o.current());d.optional(h.receipt(),(e,r)->{
                impact(e,r.impact());e.l(r.casts());e.l(r.spent());e.l(r.loadingElapsedNanos());
            });});
        });
    }
    private static void person(Writer w,ModernTargetHistory.Participant p){w.l(p.entityId());participant(w,p.identity());}
    private static void impact(Writer w,ModernPlayerHealingHistory.Impact i) {
        world(w,i.world());person(w,i.caster());cast(w,i.use());w.l(i.targetEffectRevision());var e=i.effects();
        w.u8((e.waterLife()?1:0)|(e.polluteWater()?2:0)|(e.dirtyCurrent()?4:0)|(e.healingErosion()?8:0));
        w.l(i.baseAmount());w.l(i.adjustedAmount());effect(w,i.target());
    }
    private ModernBattleHealingWire(){}
}
