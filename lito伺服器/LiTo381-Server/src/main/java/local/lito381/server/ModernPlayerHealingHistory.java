package local.lito381.server;

import java.util.Objects;

/** Participant recovery is neither a potion nor Honor-tower healing. These
 * immutable histories are not a negotiated wire capability or cast authority. */
final class ModernPlayerHealingHistory {
    /** Recipient/public view: deliberately has NO caster selection epoch. */
    record Impact(ModernTargetHistory.World world, ModernTargetHistory.Participant caster,
            ModernUseHistory.Cast use, long targetEffectRevision, ModernPlayerHealing.Effects effects,
            long baseAmount, long adjustedAmount, ModernTargetHistory.Effect target) {
        Impact {
            Objects.requireNonNull(world); Objects.requireNonNull(caster); Objects.requireNonNull(use);
            Objects.requireNonNull(effects); Objects.requireNonNull(target);
            if (!caster.identity().isHuman() || targetEffectRevision<0 || targetEffectRevision>=use.revision()
                    || baseAmount<0 || baseAmount>ModernHonorHealing.MAX_DICE*8L-1
                    || use.cost()>4 || use.sequence()>=use.revision()
                    || adjustedAmount!=ModernPlayerHealing.adjustedAmount(baseAmount,effects)
                    || target.kind()!=ModernTargetHistory.Kind.HEAL
                    || !(target.before().target() instanceof ModernTargetHistory.Participant)
                    || target.raw()!=Math.max(0,adjustedAmount)) throw bad();
            var recipient=(ModernTargetHistory.Participant)target.before().target();
            boolean entity=recipient.entityId()==caster.entityId(), identity=recipient.identity().equals(caster.identity());
            if (entity!=identity) throw bad();
            if (entity) {
                var a=use.before(); var t=target.before();
                if (a.life()!=t.life() || a.equipmentRevision()!=t.equipmentRevision()
                        || a.x()!=t.cell().x() || a.y()!=t.cell().y()
                        || a.vitals().hp()!=t.hp() || a.vitals().maximumHp()!=t.maximumHp()) throw bad();
            }
        }
        ModernTargetHistory.Participant recipient() { return (ModernTargetHistory.Participant)target.before().target(); }
        boolean self() { return caster.equals(recipient()); }
        @Override public String toString() { return "PlayerHealingImpact[redacted]"; }
    }
    record Incoming(ModernTargetHistory.World world, ModernTargetHistory.Participant recipient,
            long received, long restored, Impact last) {
        Incoming {
            Objects.requireNonNull(world); Objects.requireNonNull(recipient);
            if (received<0 || restored<0 || !bounded(restored,received,(ModernHonorHealing.MAX_DICE*8L-1)*2)
                    || (received==0)!=(last==null) || received==0 && restored!=0) throw bad();
            if (last!=null && (!last.world().equals(world) || !last.recipient().equals(recipient)
                    || received>=last.use().revision() || restored<last.target().applied()
                    || received==1 && restored!=last.target().applied())) throw bad();
        }
        static Incoming initial(ModernTargetHistory.World world,ModernTargetHistory.Participant recipient) {
            return new Incoming(world,recipient,0,0,null);
        }
        void validate(ModernVitalBalance.State resources) {
            if(last==null)return;var h=last.target().before();
            if(last.use().revision()>resources.revision()||h.life()>Math.incrementExact(resources.life())
                    ||h.equipmentRevision()>resources.equipmentRevision()
                    ||h.equipmentRevision()==resources.equipmentRevision()&&h.maximumHp()!=resources.vitals().maximumHp())throw bad();
        }
        Incoming receive(Impact next) {
            Objects.requireNonNull(next);
            if (last!=null) {
                var a=last.target().before(); var b=next.target().before();
                boolean sameEntity=last.caster().entityId()==next.caster().entityId();
                boolean sameIdentity=last.caster().identity().equals(next.caster().identity());
                if (next.use().revision()<=last.use().revision() || b.life()<a.life()
                        || b.equipmentRevision()<a.equipmentRevision() || next.targetEffectRevision()<last.targetEffectRevision() || sameEntity!=sameIdentity
                        || sameEntity && (next.use().sequence()<=last.use().sequence()
                            || next.use().before().life()<last.use().before().life()
                            || next.use().before().equipmentRevision()<last.use().before().equipmentRevision())) throw bad();
            }
            return new Incoming(world,recipient,Math.incrementExact(received),Math.addExact(restored,next.target().applied()),next);
        }
        @Override public String toString() { return "IncomingPlayerHealing[redacted]"; }
    }
    record Outgoing(ModernUseHistory.Spell spending, ModernPlayerHealingTransaction.Receipt last) {
        Outgoing {
            checkSpending(spending);
            if ((spending.last()==null)!=(last==null) || last!=null && !last.use().equals(spending.last())) throw bad();
        }
        static Outgoing initial(long gear,long revision,int cost) {
            return new Outgoing(ModernUseHistory.Spell.initial(gear,revision,cost),null);
        }
        Outgoing cast(ModernPlayerHealingTransaction.Receipt next) {
            Objects.requireNonNull(next);
            if (last!=null && (!last.world().equals(next.world()) || !last.source().equals(next.source())
                    || !last.caster().equals(next.caster()))) throw bad();
            return new Outgoing(spending.cast(next.use().revision(),next.use().sequence(),next.use().before()),next);
        }
        Outgoing reprice(long gear,long revision,int cost) { return new Outgoing(spending.reprice(gear,revision,cost),last); }
        @Override public String toString() { return "OutgoingPlayerHealing[redacted]"; }
    }
    record Entry(Incoming incoming, Outgoing outgoing) {
        Entry { Objects.requireNonNull(incoming); }
        Entry reprice(long gear,long revision,Integer cost) {
            if ((outgoing==null)!=(cost==null)) throw bad();
            return new Entry(incoming,outgoing==null?null:outgoing.reprice(gear,revision,cost));
        }
        /** Bind both ledgers to the owner, not the last foreign caster. */
        void check(ModernEquipmentHistory.Identity identity,ModernVitalBalance.State resources) {
            if (!incoming.world().equals(identity.world()) || !incoming.recipient().equals(identity.actor())) throw bad();
            incoming.validate(resources);
            var received=incoming.last(); var sent=outgoing==null?null:outgoing.last();
            if (received!=null) {
                if (received.self() && (sent==null || sent.use().revision()<received.use().revision()
                        || sent.use().revision()==received.use().revision() && !sent.impact().equals(received)
                        || sent.use().revision()>received.use().revision() && sent.use().sequence()<=received.use().sequence())) throw bad();
            }
            if (sent!=null) {
                if (!sent.world().equals(identity.world()) || !sent.source().equals(identity.source())
                        || !sent.caster().equals(identity.actor())) throw bad();
                if (sent.impact().self() && (received==null || received.use().revision()<sent.use().revision()
                        || received.use().revision()==sent.use().revision() && !sent.impact().equals(received))) throw bad();
            }
        }
    }
    /** The epoch-free public form must retain the private form's cost bounds. */
    static void checkSpending(ModernUseHistory.Spell spending) {
        Objects.requireNonNull(spending);var ledger=spending.current();
        if(ledger.cost()>4||!bounded(ledger.spent(),ledger.casts(),4)||!bounded(ledger.priorSpent(),ledger.priorCasts(),4))throw bad();
    }
    /** Avoid multiplying a wide accumulated count by the per-event maximum. */
    private static boolean bounded(long total,long count,long maximum) {
        return total/maximum<count || total/maximum==count && total%maximum==0;
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid participant healing history"); }
    private ModernPlayerHealingHistory() { }
}
