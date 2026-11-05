package local.lito381.server;

import java.util.Set;

/** Source27 dynamic exception on the original successful PC Counter Magic call. */
final class NativeSourceWeaponPlayerCounter {
    static boolean matches(NativePlayerCommit.Binding binding,ClientSession pc) {
        var context=NativeAreaMaterialAction.current.get();
        if (!NativeSourceWeaponTargetLife.marked(context)) return binding.matches(pc);
        var first=NativeSourceTargetNegativeLife.proof(context);
        if (first==null || !NativeSourceWeaponTargetLife.current(context)) return false;
        var expected=pc==context.pc ? context.caster
            : pc==first.peer() ? context.bindings.get(pc) : null;
        return expected!=null && expected.equals(binding);
    }

    static boolean earth(BattleCompanion pet,long now) {
        boolean active=pet.earthBindActive(now);
        return NativeSourceWeaponTargetLife.marked(NativeAreaMaterialAction.current.get())
            ? BattleEarthBindRules.blocksProbabilityMagic(active,27) : active;
    }

    static boolean consume(BattleTimedStatus timed, long now,
                           ClientSession caster, ClientSession target) {
        var context=NativeAreaMaterialAction.current.get();
        if (NativeSourceWeaponTargetLife.marked(context) && context.pc==caster) {
            var first=NativeSourceTargetNegativeLife.proof(context);
            if (first!=null && first.peer()==target && first.peerTimed()==timed
                    && ((Set<?>)CombatAccess.field(caster,"sourceCounterMagicBypassSkills")).contains(27)) {
                return false;
            }
        }
        return timed.consumeCounterMagic(now);
    }

    private NativeSourceWeaponPlayerCounter() {}
}
