package local.lito381.server;

/** Exact accumulated human spell spending across equipment epochs. A cast may
 * legitimately cost zero (qualified INT-reduced Healing). This is a
 * candidate ledger, not skill authorization, a world receipt or a new wire
 * capability. Legacy fixed-cost packets must keep their stricter invariants. */
final class ModernSkillSpending {
    record Totals(long casts, long spent) {
        Totals {
            if (casts < 0 || spent < 0 || casts == 0 && spent != 0
                    || casts > 0 && (spent / casts > Integer.MAX_VALUE
                        || spent / casts == Integer.MAX_VALUE && spent % casts != 0)) throw invalid();
        }
    }
    static Totals advance(long casts, long spent, int cost) {
        new Totals(casts, spent);
        if (cost < 0) throw invalid();
        return new Totals(Math.incrementExact(casts), Math.addExact(spent, cost));
    }

    /** prior* is the immutable cumulative anchor when this profile began.
     * Last-cast revision/sequence remain historical when the cost changes.
     * Storage is constant-size, regardless of match duration or gear changes. */
    record Ledger(long equipmentRevision, long profileRevision, int cost,
            long priorCasts, long priorSpent, long casts, long spent,
            long lastCastRevision, long lastCastSequence) {
        Ledger {
            new Totals(priorCasts, priorSpent); new Totals(casts, spent);
            if (equipmentRevision < 0 || profileRevision < 1 || cost < 0
                    || casts < priorCasts || spent < priorSpent || lastCastRevision < 0 || lastCastSequence < casts
                    || casts > lastCastRevision || priorCasts >= profileRevision
                    || (casts == 0) != (lastCastRevision == 0)
                    || casts == 0 && lastCastSequence != 0
                    || casts == priorCasts && lastCastRevision >= profileRevision
                    || casts > priorCasts && lastCastRevision <= profileRevision
                    || Math.addExact(priorSpent, Math.multiplyExact(casts - priorCasts, cost)) != spent)
                throw invalid();
        }
        static Ledger initial(long equipmentRevision, long profileRevision, int cost) {
            return new Ledger(equipmentRevision, profileRevision, cost, 0, 0, 0, 0, 0, 0);
        }
        Ledger cast(long revision, long sequence) {
            if (revision <= profileRevision || revision <= lastCastRevision || sequence <= lastCastSequence) throw invalid();
            var totals = advance(casts, spent, cost);
            return new Ledger(equipmentRevision, profileRevision, cost, priorCasts, priorSpent,
                    totals.casts(), totals.spent(), revision, sequence);
        }
        Ledger reprice(long nextEquipmentRevision, long revision, int nextCost) {
            if (nextEquipmentRevision != Math.incrementExact(equipmentRevision)
                    || revision <= profileRevision || revision <= lastCastRevision) throw invalid();
            return new Ledger(nextEquipmentRevision, revision, nextCost, casts, spent, casts, spent,
                    lastCastRevision, lastCastSequence);
        }
    }
    /** One event at a time; no skipped profile change or rewritten prior epoch.
     * World/source/life/skill binding and current-frame revision are separate
     * mandatory checks for the eventual negotiated envelope. */
    static boolean follows(Ledger before, Ledger after) {
        if (before == null || after == null) return false;
        if (before.equals(after)) return true;
        try {
            return before.cast(after.lastCastRevision(), after.lastCastSequence()).equals(after);
        } catch (IllegalArgumentException | ArithmeticException rejected) {
            // A reprice is not a cast and preserves the last cast exactly.
        }
        try {
            return before.reprice(after.equipmentRevision(), after.profileRevision(), after.cost()).equals(after);
        } catch (IllegalArgumentException | ArithmeticException rejected) { return false; }
    }
    private ModernSkillSpending() {}
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid accumulated spell spending"); }
}
