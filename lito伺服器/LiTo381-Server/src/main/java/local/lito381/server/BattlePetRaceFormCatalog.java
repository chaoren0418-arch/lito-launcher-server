package local.lito381.server;

import java.util.ArrayList;
import java.util.List;

/** Native-era pet forms used by the LiTo PET_RACE reconstruction. */
final class BattlePetRaceFormCatalog {
    static final int TRANSFORM_DURATION_SECONDS = 30;
    static final int SPEED_DURATION_SECONDS = 30;
    static final long PANEL_COOLDOWN_NANOS = 4_000_000_000L;

    enum Pace { FAST, STANDARD, SLOW, SPECIAL }

    record Form(String key, int gfx, Pace pace) {
        Form {
            if (key == null || key.isBlank() || gfx <= 0 || pace == null) {
                throw new IllegalArgumentException("invalid native pet-race form");
            }
            BattleAnimationCatalog.get().requireWalkAction(gfx, BattleActionCodes.WALK);
        }
    }

    /*
     * Contemporary official instructions select one common starting pet for
     * the race.  The retained 3.80 npc table supplies the corresponding gfx.
     */
    private static final List<Form> BASE = List.of(
            new Form("kishu-puppy", 5065, Pace.STANDARD),
            new Form("beagle", 938, Pace.STANDARD),
            new Form("husky", 2145, Pace.STANDARD),
            new Form("high-st-bernard", 3182, Pace.STANDARD));

    private static final List<Form> FAST = List.of(
            new Form("high-wolf", 3199, Pace.FAST),
            new Form("high-husky", 3107, Pace.FAST),
            new Form("high-doberman", 3132, Pace.FAST),
            new Form("high-cat", 3178, Pace.FAST));
    private static final List<Form> STANDARD = List.of(
            new Form("high-collie", 3211, Pace.STANDARD),
            new Form("high-fox", 3156, Pace.STANDARD),
            new Form("high-raccoon", 4133, Pace.STANDARD),
            new Form("high-st-bernard", 3182, Pace.STANDARD));
    private static final List<Form> SLOW = List.of(
            new Form("cow", 945, Pace.SLOW),
            new Form("turtle-dragon", 1649, Pace.SLOW),
            new Form("gelatin-cube", 2541, Pace.SLOW),
            new Form("bear", 1642, Pace.SLOW),
            new Form("mambo-rabbit", 4168, Pace.SLOW),
            new Form("floating-eye", 29, Pace.SLOW));
    private static final List<Form> SPECIAL = List.of(
            new Form("antharas", 1245, Pace.SPECIAL),
            new Form("fafurion", 1590, Pace.SPECIAL),
            new Form("valakas", 2001, Pace.SPECIAL));
    private static final List<Form> TEMPORARY = temporaryForms();
    private static final List<Form> TRAILING = trailingForms();

    private BattlePetRaceFormCatalog() {}

    static List<Form> baseForms() { return BASE; }
    static List<Form> temporaryFormsList() { return TEMPORARY; }

    static Form baseForm(int mapId, long raceGeneration) {
        return select(BASE, mix64(((long) mapId << 32) ^ raceGeneration));
    }

    /**
     * The official rule states that a trailing runner has a higher chance of
     * receiving a fast form, without publishing exact percentages.  Preserve
     * that monotonic bias by adding one extra copy of each fast form; do not
     * claim this evidence-bounded weighting is the unrecovered original RNG.
     */
    static Form temporaryForm(long playerId, long activation, boolean trailing) {
        List<Form> pool = trailing ? TRAILING : TEMPORARY;
        return select(pool, mix64(playerId ^ Long.rotateLeft(activation, 21)));
    }

    private static List<Form> temporaryForms() {
        ArrayList<Form> forms = new ArrayList<>();
        forms.addAll(FAST);
        forms.addAll(STANDARD);
        forms.addAll(SLOW);
        forms.addAll(SPECIAL);
        return List.copyOf(forms);
    }

    private static List<Form> trailingForms() {
        ArrayList<Form> forms = new ArrayList<>(TEMPORARY);
        forms.addAll(FAST);
        return List.copyOf(forms);
    }

    private static Form select(List<Form> forms, long mixed) {
        return forms.get(Math.floorMod(mixed, forms.size()));
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
