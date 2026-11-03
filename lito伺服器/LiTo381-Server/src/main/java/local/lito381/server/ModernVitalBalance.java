package local.lito381.server;

import java.math.BigInteger;
import java.util.Objects;

/** Applied resource accounting, independent of CURRENT equipment capacity.
 * Not a wire capability or authorization. Each delta is the amount actually
 * applied, not raw damage/recovery. Existing <=34 validators remain unchanged. */
final class ModernVitalBalance {
    record Flows(long hpGain, long hpLoss, long restored, long damaged, long hpGranted,
            long mpGain, long mpLoss, long spent, long mpGranted, long hpClipped, long mpClipped) {
        static final Flows ZERO = new Flows(0,0,0,0,0,0,0,0,0,0,0);
        Flows {
            if (hpGain < 0 || hpLoss < 0 || restored < 0 || damaged < 0 || hpGranted < 0
                    || mpGain < 0 || mpLoss < 0 || spent < 0 || mpGranted < 0 || hpClipped < 0 || mpClipped < 0) throw invalid();
        }
        private Flows plus(Flows d) {
            return new Flows(Math.addExact(hpGain,d.hpGain),Math.addExact(hpLoss,d.hpLoss),
                    Math.addExact(restored,d.restored),Math.addExact(damaged,d.damaged),Math.addExact(hpGranted,d.hpGranted),
                    Math.addExact(mpGain,d.mpGain),Math.addExact(mpLoss,d.mpLoss),Math.addExact(spent,d.spent),
                    Math.addExact(mpGranted,d.mpGranted),Math.addExact(hpClipped,d.hpClipped),Math.addExact(mpClipped,d.mpClipped));
        }
    }
    record State(long revision, long equipmentRevision, long life, long birthHp, long birthMp,
            ModernEquipmentVitals.Vitals vitals, Flows flows) {
        State {
            Objects.requireNonNull(vitals); Objects.requireNonNull(flows);
            if (revision < 1 || equipmentRevision < 0 || life < 0 || life >= revision || birthHp < 1 || birthMp < 0
                    || life == 0 && (flows.hpGranted != 0 || flows.mpGranted != 0)
                    || life > flows.hpGranted) throw invalid();
            var hp = big(birthHp).add(big(flows.hpGain)).add(big(flows.restored)).add(big(flows.hpGranted))
                    .subtract(big(flows.hpLoss)).subtract(big(flows.damaged)).subtract(big(flows.hpClipped));
            var mp = big(birthMp).add(big(flows.mpGain)).add(big(flows.mpGranted))
                    .subtract(big(flows.mpLoss)).subtract(big(flows.spent)).subtract(big(flows.mpClipped));
            if (!hp.equals(big(vitals.hp())) || !mp.equals(big(vitals.mp()))) throw invalid();
        }
        static State initial(long revision, long equipmentRevision, ModernEquipmentVitals.Vitals vitals) {
            return new State(revision,equipmentRevision,0,vitals.hp(),vitals.mp(),vitals,Flows.ZERO);
        }
        private State step(long next, long gear, long nextLife, ModernEquipmentVitals.Vitals live, Flows delta) {
            if (next <= revision) throw invalid();
            return new State(next,gear,nextLife,birthHp,birthMp,live,flows.plus(delta));
        }
        private ModernEquipmentVitals.Vitals values(long hp, long mp) {
            return new ModernEquipmentVitals.Vitals(hp,vitals.maximumHp(),mp,vitals.maximumMp());
        }
        State damage(long next, long applied) {
            if (applied < 0 || applied > vitals.hp()) throw invalid();
            return step(next,equipmentRevision,life,values(vitals.hp()-applied,vitals.mp()),
                    new Flows(0,0,0,applied,0,0,0,0,0,0,0));
        }
        State restore(long next, long applied) {
            if (vitals.hp() == 0 || applied < 0 || applied > vitals.maximumHp()-vitals.hp()) throw invalid();
            return step(next,equipmentRevision,life,values(vitals.hp()+applied,vitals.mp()),
                    new Flows(0,0,applied,0,0,0,0,0,0,0,0));
        }
        State spend(long next, long applied) {
            if (vitals.hp() == 0 || applied < 0 || applied > vitals.mp()) throw invalid();
            return step(next,equipmentRevision,life,values(vitals.hp(),vitals.mp()-applied),
                    new Flows(0,0,0,0,0,0,0,applied,0,0,0));
        }
        /** One self-healing event: the same actor cannot spend and restore in
         * two independent same-revision assignments. Admission and the two
         * distinct spell/incoming-heal receipts remain the caller's job. */
        State restoreAndSpend(long next, long restored, long spent) {
            if (vitals.hp() == 0 || restored < 0 || spent < 0
                    || restored > vitals.maximumHp()-vitals.hp() || spent > vitals.mp()) throw invalid();
            return step(next,equipmentRevision,life,values(vitals.hp()+restored,vitals.mp()-spent),
                    new Flows(0,0,restored,0,0,0,0,spent,0,0,0));
        }
        State regenerate(long next, long hpGain, long hpLoss, long mpGain, long mpLoss) {
            if (vitals.hp() == 0 || hpGain < 0 || hpLoss < 0 || mpGain < 0 || mpLoss < 0
                    || hpGain > 0 && hpLoss > 0 || mpGain > 0 && mpLoss > 0
                    || hpGain > vitals.maximumHp()-vitals.hp() || hpLoss >= vitals.hp()
                    || mpGain > vitals.maximumMp()-vitals.mp() || mpLoss > vitals.mp()) throw invalid();
            return step(next,equipmentRevision,life,values(vitals.hp()+hpGain-hpLoss,vitals.mp()+mpGain-mpLoss),
                    new Flows(hpGain,hpLoss,0,0,0,mpGain,mpLoss,0,0,0,0));
        }
        /** Applied catch-up ticks can change sign between HP rolls. Keep both
         * gross totals and independently check the final values. This is only
         * accounting, not evidence that arbitrary client-provided ticks ran. */
        State regenerateBatch(long next,long hpGain,long hpLoss,long mpGain,long mpLoss,long hpAfter,long mpAfter) {
            if(vitals.hp()==0||hpAfter<1||hpGain<0||hpLoss<0||mpGain<0||mpLoss<0
                    ||!big(vitals.hp()).add(big(hpGain)).subtract(big(hpLoss)).equals(big(hpAfter))
                    ||!big(vitals.mp()).add(big(mpGain)).subtract(big(mpLoss)).equals(big(mpAfter)))throw invalid();
            return step(next,equipmentRevision,life,values(hpAfter,mpAfter),
                    new Flows(hpGain,hpLoss,0,0,0,mpGain,mpLoss,0,0,0,0));
        }
        /** A mode grants actual resources to a dead life, not max * life count.
         * MP need not refill; existing modes can grant zero. */
        State respawn(long next, long hpGranted, long mpGranted) {
            if (vitals.hp() != 0 || hpGranted < 1 || hpGranted > vitals.maximumHp()
                    || mpGranted < 0 || mpGranted > vitals.maximumMp()-vitals.mp()) throw invalid();
            return step(next,equipmentRevision,Math.incrementExact(life),values(hpGranted,vitals.mp()+mpGranted),
                    new Flows(0,0,0,0,hpGranted,0,0,0,mpGranted,0,0));
        }
        State equip(long next, long gear, long maximumHp, long maximumMp) {
            if (gear != Math.incrementExact(equipmentRevision)) throw invalid();
            var retained = ModernEquipmentVitals.retain(vitals,maximumHp,maximumMp);
            return step(next,gear,life,retained,
                    new Flows(0,0,0,0,0,0,0,0,0,vitals.hp()-retained.hp(),vitals.mp()-retained.mp()));
        }
    }
    /** Only a single equipment event; intervening resource/life events must be
     * validated separately in the eventual scoped negotiated envelope. */
    static boolean followsEquipment(State before, State after) {
        if (before == null || after == null) return false;
        try {
            return before.equip(after.revision(),after.equipmentRevision(),
                    after.vitals().maximumHp(),after.vitals().maximumMp()).equals(after);
        } catch (IllegalArgumentException | ArithmeticException rejected) { return false; }
    }
    private static BigInteger big(long n) { return BigInteger.valueOf(n); }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid applied vital balance"); }
    private ModernVitalBalance() {}
}
