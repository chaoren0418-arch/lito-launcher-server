package local.lito381.server;

import java.math.BigInteger;
import java.util.Objects;

/** Incoming skill68 layer of the selected LiTo damage pipeline, AFTER ordinary
 * defense/MR/skill critical and BEFORE HP clipping. Not a status grant or a live
 * engine commit. ClientSession.combatDamageAfterCooking is the retained LiTo
 * reference; do not move classic L1MagicPc's pre-MR SKD3 layer into this route. */
final class ModernIncomingDamage {
    static final Protection NONE=new Protection(null,0);
    static final class Protection {
        private final ModernArmorProcProtection.State state;
        private final long elapsed;
        private final boolean active;
        private Protection(ModernArmorProcProtection.State state,long elapsed) {
            this.state=state;this.elapsed=elapsed;this.active=state!=null&&state.active(elapsed);
        }
        ModernArmorProcProtection.State state(){return state;}
        long elapsed(){return elapsed;}
        boolean active(){return active;}
        /** A read/expiry/life transition may invalidate a calculated hit even
         * if no HP changed. The engine must check this again before publication. */
        boolean currentAt(ModernArmorProcProtection.State current,long now) {
            if(state==null||current==null||now<elapsed||!state.equals(current))return false;
            return current.active(now)==active;
        }
        void requireTarget(ModernParticipantId target) {
            Objects.requireNonNull(target);
            if(state!=null&&!ModernParticipantId.human(state.actor().owner()).equals(target))throw bad();
        }
        Impact apply(long damage,long hp) {return apply(BigInteger.valueOf(damage),hp);}
        Impact apply(BigInteger damage,long hp) {
            Objects.requireNonNull(damage);
            // All supported calculators have bounded i64 inputs and fixed
            // factors; prevent an unbounded external bigint from entering.
            if(hp<1||damage.signum()<0||damage.bitLength()>160)throw bad();
            long incoming=(active?damage.shiftRight(1):damage).longValueExact();
            return new Impact(this,hp,incoming,damage.signum()>0);
        }
        @Override public String toString(){return "IncomingProtection[redacted]";}
    }
    static Protection at(ModernArmorProcProtection.State state,long elapsed) {
        return new Protection(Objects.requireNonNull(state),elapsed);
    }
    /** Immutable pre/post-HP receipt created at the incoming calculation stage.
     * It is not permission to mutate an actor or proof of resource qualification. */
    static final class Impact {
        private final Protection protection;
        private final long hpBefore,damage,applied,remaining;
        private final boolean positiveIncoming;
        private Impact(Protection protection,long hpBefore,long damage,boolean positiveIncoming) {
            this.protection=protection;this.hpBefore=hpBefore;this.damage=damage;
            this.positiveIncoming=positiveIncoming;
            applied=Math.min(hpBefore,damage);remaining=hpBefore-applied;
        }
        Protection protection(){return protection;}
        long hpBefore(){return hpBefore;} long damage(){return damage;}
        long applied(){return applied;} long remaining(){return remaining;}
        boolean positiveIncoming(){return positiveIncoming;}
        boolean death(){return remaining==0;}
        @Override public String toString(){return "IncomingImpact[redacted]";}
    }
    record Resolved<T>(T outcome,Impact impact) {
        Resolved {Objects.requireNonNull(outcome);Objects.requireNonNull(impact);}
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid incoming damage binding");}
    private ModernIncomingDamage(){}
}
