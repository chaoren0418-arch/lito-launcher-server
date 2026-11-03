package local.lito381.server;

/** Source areaMagicAttack uses the actual NPC map before attempting any player casts. */
final class NativeCloseoutNpcAreaScene {
    static ArenaMapCatalog.Arena arena(Object encounter, Object actor) {
        if (encounter == null || actor == null || !Thread.holdsLock(encounter)
                || !actor.getClass().getName().equals(encounter.getClass().getName() + "$Monster")) return null;
        if (encounter instanceof BattleQuestEncounter) {
            // The existing spawn and movement paths index arenas by force, not roomNumber.
            var arenas = (ArenaMapCatalog.Arena[]) CombatAccess.field(encounter, "arenas");
            int force = NativeSourceRawDamage.number(actor, "force");
            return force >= 0 && force < arenas.length ? arenas[force] : null;
        }
        if (encounter instanceof BattleTimeAttackEncounter)
            return (ArenaMapCatalog.Arena) CombatAccess.field(encounter, "arena");
        return null;
    }
    static boolean attempt(Object actor, long now, BattleTimeAttackMagicSkillCatalog.MobskillRow row, Object encounter) {
        if (NativeCloseoutNpcAreaCost.matches(actor, row)) {
            var arena = arena(encounter, actor);
            if (arena == null || arena.mapId() == 93) return false;
        }
        return NativeCloseoutNpcSpellAdmission.attempt(actor, now, row);
    }
    private NativeCloseoutNpcAreaScene() {}
}
