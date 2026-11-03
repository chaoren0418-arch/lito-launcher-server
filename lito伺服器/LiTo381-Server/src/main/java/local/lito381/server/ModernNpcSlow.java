package local.lito381.server;

import java.util.Objects;

/** Victim-side skill29 CAS only, like the roster's incoming-damage plans.
 * The enclosing engine must authorize the caster, target/range/instance,
 * source template, equipment, immunity, probability, material, MP and cooldown, then publish both sides under
 * its lock. No current service calls this candidate; it grants no cast. */
final class ModernNpcSlow {
    record Incoming(ModernNpcRoster.Scope scope, long revision, long elapsedMillis, ModernNpcRoster.Actor before) {
        Incoming {
            Objects.requireNonNull(scope); Objects.requireNonNull(before);
            if (!scope.equals(before.id().scope()) || revision < before.changedRevision() || !before.alive()) throw bad();
            before.speed().at(elapsedMillis);
        }
        ModernNpcSpeed.State after() {
            return ModernNpcSpeed.slow(before.speed(), elapsedMillis, Math.incrementExact(revision));
        }
    }
    static Incoming plan(ModernNpcRoster.Snapshot snapshot, ModernNpcRoster.Id id, long life) {
        Objects.requireNonNull(snapshot); Objects.requireNonNull(id);
        if (snapshot.closed() || !snapshot.scope().equals(id.scope()) || life < 1) throw bad();
        var target = snapshot.actors().stream().filter(a -> a.id().equals(id) && a.life() == life)
                .findFirst().orElseThrow(ModernNpcSlow::bad);
        return new Incoming(snapshot.scope(), snapshot.revision(), snapshot.elapsedMillis(), target);
    }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern NPC slow plan"); }
    private ModernNpcSlow() {}
}
