package local.lito381.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure on-hurt Immune-to-Harm core used by the explicit internal engine opt-in.
 * Public admission still requires complete visual/skill resources and a dedicated
 * wire contract. No HP, SQL, timer or socket mutation here. */
final class ModernArmorProcProtection {
    static final long SECOND=1_000_000_000L,MAX_TIME=1800*SECOND;
    static final long SKILL=68;
    enum Failure { INVALID_INPUT, UNSUPPORTED_EFFECT, STALE_CANDIDATE }
    static final class ProcException extends IllegalArgumentException {
        private final Failure failure;
        ProcException(Failure failure){super("modern armor protection: "+failure);this.failure=failure;}
        Failure failure(){return failure;}
    }
    record Actor(UUID match,TournamentPresence.CharacterKey owner,long entity,long life) {
        Actor {Objects.requireNonNull(match);Objects.requireNonNull(owner);if(entity<1||life<0)throw bad();}
        @Override public String toString(){return "ArmorProcActor[redacted]";}
    }
    record Equipped(long objectId,ModernArmorProcs.Rule rule) {
        Equipped {if(objectId<1)throw bad();Objects.requireNonNull(rule);}
        @Override public String toString(){return "EquippedArmorProc[redacted]";}
    }
    /** Every applicable row is retained; an unimplemented sibling row rejects the whole selection. */
    record Selection(Actor actor,long equipmentRevision,List<Equipped> rules) {
        Selection {
            Objects.requireNonNull(actor);rules=List.copyOf(rules);
            if(equipmentRevision<0||rules.size()>ModernArmorProcs.MAX_ROWS)throw bad();
            var unique=new HashSet<String>();
            for(var entry:rules) {
                if(!unique.add(entry.objectId()+":"+entry.rule().id()))throw bad();
                if(!supported(entry.rule()))throw new ProcException(Failure.UNSUPPORTED_EFFECT);
            }
        }
        @Override public String toString(){return "ArmorProcSelection[redacted]";}
    }
    static Selection select(ModernPhysicalCombat.Source source,ModernCombatProfile profile,Actor actor) {
        Objects.requireNonNull(source);Objects.requireNonNull(profile);Objects.requireNonNull(actor);
        if(!source.input().equipment().source().equals(profile.source())
                ||!actor.owner().equals(new TournamentPresence.CharacterKey(profile.source().sourceId(),profile.source().character().objectId())))throw bad();
        var result=new ArrayList<Equipped>();
        for(var flag:profile.equipped()) {
            if(!flag.equipped())continue;
            var item=source.input().equipment().items().get(flag.objectId()).source();
            if(item.template().kind()!=ModernCharacterData.ItemKind.ARMOR||!source.procCatalogIds().contains(item.template().catalogId()))continue;
            if(source.armorProcs()==null)throw new ProcException(Failure.UNSUPPORTED_EFFECT);
            for(var rule:source.armorProcs().selected(item.template().catalogId(),item.enchantment()))result.add(new Equipped(item.objectId(),rule));
        }
        return new Selection(actor,profile.equipmentRevision(),result);
    }
    static boolean supported(ModernArmorProcs.Rule rule) {
        // A disabled row is still fully read, but cannot emit any execution.
        if(rule.chance(false)==0&&rule.chance(true)==0)return true;
        return rule.chance(false)==0&&rule.chance(true)>0&&rule.flatDamage()==0&&rule.randomDamage()==0
                &&rule.hp()==0&&rule.randomHp()==0&&rule.mp()==0&&rule.randomMp()==0
                &&(rule.healMode()==0||rule.healMode()==1)&&rule.skill()==SKILL&&rule.seconds()>0&&rule.seconds()<=1800
                &&rule.skillTarget()==1&&rule.graphics()>=0&&rule.graphics()<=65535
                &&rule.graphicsMode()>=0&&rule.graphicsMode()<=2;
    }
    /** Match-relative lease; no imported classic status and no wall/client clock. */
    record State(Actor actor,long sequence,long observedNanos,long untilNanos) {
        State {
            Objects.requireNonNull(actor);time(observedNanos);
            if(sequence<0||untilNanos!=0&&(untilNanos<=observedNanos||untilNanos>Math.addExact(observedNanos,MAX_TIME))
                    ||sequence==0&&(observedNanos!=0||untilNanos!=0))throw bad();
        }
        boolean active(long elapsed){time(elapsed);if(elapsed<observedNanos)throw bad();return untilNanos>elapsed;}
        @Override public String toString(){return "ArmorProtectionState[redacted]";}
    }
    /** An event declares required effects, not loaded resources or delivered packets. */
    record Trigger(long objectId,long procId,long graphics,long graphicsMode,boolean appliedProtection,long durationSeconds) {}
    static final class Candidate {
        private final Selection selection;
        private final State before,after;
        private final long hpBefore,hpAfter;
        private final List<Trigger> triggers;
        private Candidate(Selection selection,State before,State after,long hpBefore,long hpAfter,List<Trigger> triggers) {
            Objects.requireNonNull(selection);Objects.requireNonNull(before);Objects.requireNonNull(after);triggers=List.copyOf(triggers);
            if(!selection.actor().equals(before.actor())||!before.actor().equals(after.actor())
                    ||after.sequence()!=Math.incrementExact(before.sequence())||after.observedNanos()<before.observedNanos()
                    ||hpBefore<1||hpAfter<0||hpAfter>hpBefore||triggers.size()>selection.rules().size()||hpAfter==0&&!triggers.isEmpty())throw bad();
            this.selection=selection;this.before=before;this.after=after;this.hpBefore=hpBefore;this.hpAfter=hpAfter;this.triggers=triggers;
        }
        Selection selection(){return selection;} State before(){return before;} State after(){return after;}
        long hpBefore(){return hpBefore;} long hpAfter(){return hpAfter;} List<Trigger> triggers(){return triggers;}
        @Override public String toString(){return "ArmorProtectionCandidate[redacted]";}
    }
    static State initial(Actor actor){return new State(actor,0,0,0);}

    /** Call at the traced damage-reduction stage, BEFORE HP clipping. Never halve already-clipped loss. */
    static long protectedDamage(State state,long elapsed,long eligibleSubtotal) {
        if(eligibleSubtotal<0)throw bad();return state.active(elapsed)?eligibleSubtotal/2:eligibleSubtotal;
    }

    /** Damage/proc composition uses exactly the old protection snapshot and
     * pre-clipping incoming damage produced by the calculator, not a caller's
     * reinterpretation of raw physical damage or final HP loss. */
    static Candidate prepareHurt(Selection selection,ModernIncomingDamage.Impact impact,boolean feedbackAllowed,
            ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(impact);var protection=impact.protection();
        if(protection.state()==null)throw bad();
        return prepareHurt(selection,protection.state(),protection.elapsed(),impact.hpBefore(),impact.positiveIncoming()?1:0,impact.applied(),feedbackAllowed,random);
    }

    /** Damage is already calculated using the OLD status. The new proc cannot protect
     * the triggering hit. eligibleIncomingDamage is the positive-hit subtotal at
     * receiveDamageInternal BEFORE defense procs, not final HP loss: later reduction
     * can make appliedDamage zero without cancelling the queued proc.
     * RNG is server-owned; lethal hits may roll but never grant a buff.
     * Counter/secondary feedback calls set feedbackAllowed=false to avoid proc recursion. */
    static Candidate prepareHurt(Selection selection,State before,long elapsed,long hpBefore,long eligibleIncomingDamage,long appliedDamage,
            boolean feedbackAllowed,ModernPhysicalCombat.BoundedRandom random) {
        Objects.requireNonNull(selection);Objects.requireNonNull(before);Objects.requireNonNull(random);time(elapsed);
        if(!selection.actor().equals(before.actor())||elapsed<before.observedNanos()||hpBefore<1||eligibleIncomingDamage<0||appliedDamage<0||appliedDamage>hpBefore)throw bad();
        long hpAfter=hpBefore-appliedDamage,until=before.active(elapsed)?before.untilNanos():0;
        var events=new ArrayList<Trigger>();
        if(feedbackAllowed&&eligibleIncomingDamage>0)for(var entry:selection.rules()) {
            var rule=entry.rule();
            if(!ModernArmorProcs.roll(rule,true,random)||hpAfter==0)continue;
            boolean applied=until==0;
            // ProcSkillEffectHelper: an existing self buff is NOT refreshed, even
            // though the independent configured proc GFX still occurs on a winning roll.
            if(applied)until=Math.addExact(elapsed,Math.multiplyExact(rule.seconds(),SECOND));
            events.add(new Trigger(entry.objectId(),rule.id(),rule.graphics(),rule.graphicsMode(),applied,rule.seconds()));
        }
        var after=new State(before.actor(),Math.incrementExact(before.sequence()),elapsed,hpAfter==0?0:until);
        return new Candidate(selection,before,after,hpBefore,hpAfter,events);
    }
    /** Compare-and-commit result only; the caller owns atomic publication and all other
     * engine guards. Life/world/gear/rule changes, HP races and candidate replay reject. */
    static State accept(Candidate candidate,Selection currentSelection,State current,long currentHp) {
        Objects.requireNonNull(candidate);
        if(!candidate.selection().equals(currentSelection)||!candidate.before().equals(current)||candidate.hpBefore()!=currentHp)
            throw new ProcException(Failure.STALE_CANDIDATE);
        return candidate.after();
    }
    /** Live callers revalidate after advancing the engine clock. Expiry of a
     * protection used by the damage calculation invalidates the whole hit;
     * never retry RNG here. A NEW proc starts at accepted commit time, while
     * an existing buff keeps its original deadline (no refresh). */
    static State acceptAt(Candidate candidate,Selection currentSelection,State current,long currentHp,long elapsed) {
        accept(candidate,currentSelection,current,currentHp);time(elapsed);
        if(elapsed<candidate.after().observedNanos()
                ||current.active(candidate.after().observedNanos())!=current.active(elapsed))
            throw new ProcException(Failure.STALE_CANDIDATE);
        long until=current.active(elapsed)?current.untilNanos():0;
        if(candidate.hpAfter()!=0)for(var event:candidate.triggers())if(event.appliedProtection()) {
            until=Math.addExact(elapsed,Math.multiplyExact(event.durationSeconds(),SECOND));break;
        }
        return new State(current.actor(),candidate.after().sequence(),elapsed,candidate.hpAfter()==0?0:until);
    }
    static State expire(State state,long elapsed) {
        Objects.requireNonNull(state);time(elapsed);if(elapsed<state.observedNanos())throw bad();
        return state.untilNanos()!=0&&!state.active(elapsed)
                ?new State(state.actor(),Math.incrementExact(state.sequence()),elapsed,0):state;
    }
    private static void time(long elapsed){if(elapsed<0||elapsed>MAX_TIME)throw bad();}
    private static ProcException bad(){return new ProcException(Failure.INVALID_INPUT);}
    private ModernArmorProcProtection(){}
}
