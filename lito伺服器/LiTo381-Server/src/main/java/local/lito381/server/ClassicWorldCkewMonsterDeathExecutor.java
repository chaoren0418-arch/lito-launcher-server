package local.lito381.server;

/** Exact retained quest-item decisions of the CKEW50_* monster executors. */
final class ClassicWorldCkewMonsterDeathExecutor {
    private ClassicWorldCkewMonsterDeathExecutor() {}

    static long questItem(
            int npcId, boolean destructionMedicineActive,
            boolean owns49165, boolean owns49166,
            boolean owns49168, int roll0To99) {
        if (roll0To99 < 0 || roll0To99 > 99) {
            throw new IllegalArgumentException("CKEW death roll must be 0..99");
        }
        if (destructionMedicineActive) return 0;
        return switch (npcId) {
            // Exact IDs are stated in the retained executor source comments:
            // Guardian1=87001, Soldiers=87002, ChinoSoldiers=87003.
            case 87001, 87002, 87003 ->
                    !owns49165 && roll0To99 < 40 ? 49165 : 0;
            // The four class-specific Gladiators are guaranteed until owned.
            case 87007, 87008, 87009, 87010 -> owns49166 ? 0 : 49166;
            // Guardian2=87011 intentionally has no existing-item guard.
            case 87011 -> roll0To99 < 40 ? 49167 : 0;
            // One of the four class-specific Chino actors.
            case 87015, 87016, 87017, 87018 -> owns49168 ? 0 : 49168;
            default -> 0;
        };
    }
}
