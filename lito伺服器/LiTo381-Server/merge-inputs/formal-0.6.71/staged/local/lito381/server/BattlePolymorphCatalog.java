package local.lito381.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Exact selected 3.80 polymorph rows used by LiTo's native 3.81 UI. */
final class BattlePolymorphCatalog {
    static final String DEFAULT_RESOURCE = "/battle/polymorph-native-380.csv";
    static final int ORDINARY_SCROLL_DURATION_SECONDS = 1_800;
    static final int BLESSED_SCROLL_DURATION_SECONDS = 2_100;
    static final int SHAPE_CHANGE_DURATION_SECONDS = 7_200;
    static final int RANDOM_TOTAL_WEIGHT = 1_000_000;

    record Form(String selection, int gfx, int minimumLevel,
                int weaponEquipMask, int armorEquipMask,
                boolean skillUsable, int causeMask,
                boolean standardMenu, int randomWeight) {
        Form {
            selection = selection == null ? "" : selection.trim();
            if (selection.isEmpty() || gfx <= 0 || gfx > 0xFFFF
                    || minimumLevel <= 0 || minimumLevel > 0xFF
                    || weaponEquipMask < 0 || weaponEquipMask > 0x7FF
                    || armorEquipMask < 0 || armorEquipMask > 0xFFF
                    || causeMask < 0 || causeMask > 7
                    || randomWeight < 0 || randomWeight > RANDOM_TOTAL_WEIGHT) {
                throw new IllegalArgumentException("invalid native polymorph form");
            }
            // All selected forms expose native walk 0.  Ten bow-native forms
            // intentionally omit attack 1 and use their proven 20/21 family
            // when no external weapon remains equipped.
            BattleAnimationCatalog.get().requireWalkAction(gfx, BattleActionCodes.WALK);
            BattlePolymorphCatalog.resolveBattleWalkAction(
                    gfx, weaponEquipMask, BattleActionCodes.WALK);
        }

        boolean supportsWalkAction(int walkAction) {
            if (walkAction == BattleActionCodes.WALK) return true;
            int weaponBit = weaponBitForWalkAction(walkAction);
            BattleAnimationCatalog animations = BattleAnimationCatalog.get();
            return weaponBit != 0 && (weaponEquipMask & weaponBit) != 0
                    && animations.supportsTimedAction(gfx, walkAction)
                    && animations.supportsTimedAction(
                            gfx, BattleActionCodes.attackForWalk(walkAction));
        }

        int resolveBattleWalkAction(int requestedWalkAction) {
            return BattlePolymorphCatalog.resolveBattleWalkAction(
                    gfx, weaponEquipMask, requestedWalkAction);
        }
    }

    private static final Catalog CATALOG = load(DEFAULT_RESOURCE);
    static final Form DEATH_KNIGHT = require("death knight");

    private BattlePolymorphCatalog() {}

    static Form find(String selection) {
        if (selection == null || selection.isBlank()) return null;
        return CATALOG.byName().get(key(selection));
    }

    static Form require(String selection) {
        Form form = find(selection);
        if (form == null) {
            throw new IllegalArgumentException("unknown native polymorph form: " + selection);
        }
        return form;
    }

    static List<Form> standardMenuForms() {
        return CATALOG.standardMenuForms();
    }

    static List<Form> allForms() {
        return CATALOG.allForms();
    }

    static Form nativePackagedRandomForm(int weightedRoll) {
        if (weightedRoll < 1 || weightedRoll > RANDOM_TOTAL_WEIGHT) {
            throw new IllegalArgumentException("random polymorph roll outside native range");
        }
        int cumulative = 0;
        for (Form form : CATALOG.randomForms()) {
            cumulative = Math.addExact(cumulative, form.randomWeight());
            if (weightedRoll <= cumulative) return form;
        }
        throw new IllegalStateException("native polymorph weights do not cover roll");
    }

    /** Deterministic 1..1,000,000 selector for the retained no-control-ring path. */
    static Form randomForm(int weightedRoll) {
        Form selected = NativePolymorphSource.selectOrNull(weightedRoll);
        return selected != null ? selected : nativePackagedRandomForm(weightedRoll);
    }

    static int scrollDurationSeconds(int blessing) {
        return blessing == 1
                ? BLESSED_SCROLL_DURATION_SECONDS
                : ORDINARY_SCROLL_DURATION_SECONDS;
    }

    static int weaponBitForWalkAction(int walkAction) {
        return switch (walkAction) {
            case BattleActionCodes.DAGGER_WALK -> 1;
            case BattleActionCodes.SWORD_WALK -> 2;
            case BattleActionCodes.TWO_HAND_SWORD_WALK -> 4;
            case BattleActionCodes.AXE_WALK -> 8;
            case BattleActionCodes.SPEAR_WALK -> 16;
            case BattleActionCodes.STAFF_WALK -> 32;
            case BattleActionCodes.EDORYU_WALK -> 64;
            case BattleActionCodes.CLAW_WALK -> 128;
            case BattleActionCodes.BOW_WALK -> 256;
            case BattleActionCodes.THROWING_KNIFE_WALK -> 512;
            case BattleActionCodes.CHAINSWORD_WALK -> 1024;
            default -> 0;
        };
    }

    private static int resolveBattleWalkAction(
            int gfx, int weaponEquipMask, int requestedWalkAction) {
        BattleAnimationCatalog animations = BattleAnimationCatalog.get();
        if (BattleActionCodes.isWalkAction(requestedWalkAction)
                && animations.supportsTimedAction(gfx, requestedWalkAction)
                && animations.supportsTimedAction(gfx,
                        BattleActionCodes.attackForWalk(requestedWalkAction))) {
            return requestedWalkAction;
        }
        int[] nativeFamilies = {
                BattleActionCodes.DAGGER_WALK, BattleActionCodes.SWORD_WALK,
                BattleActionCodes.TWO_HAND_SWORD_WALK, BattleActionCodes.AXE_WALK,
                BattleActionCodes.SPEAR_WALK, BattleActionCodes.STAFF_WALK,
                BattleActionCodes.EDORYU_WALK, BattleActionCodes.CLAW_WALK,
                BattleActionCodes.BOW_WALK, BattleActionCodes.THROWING_KNIFE_WALK,
                BattleActionCodes.CHAINSWORD_WALK
        };
        for (int candidate : nativeFamilies) {
            int bit = weaponBitForWalkAction(candidate);
            if ((weaponEquipMask & bit) != 0
                    && animations.supportsTimedAction(gfx, candidate)
                    && animations.supportsTimedAction(
                            gfx, BattleActionCodes.attackForWalk(candidate))) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("polymorph has no battle-capable action family: gfx="
                + gfx + " requested=" + requestedWalkAction
                + " weapon-mask=" + weaponEquipMask);
    }

    private static Catalog load(String resource) {
        InputStream input = BattlePolymorphCatalog.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IllegalStateException("Required polymorph resource is missing: " + resource);
        }
        Map<String, Form> forms = new LinkedHashMap<>();
        List<Form> menu = new ArrayList<>();
        List<Form> random = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#")
                        || value.startsWith("selection,")) continue;
                String[] fields = value.split(",", -1);
                if (fields.length != 9) {
                    throw new IllegalStateException(resource + ":" + lineNumber
                            + " expected nine CSV fields, got " + fields.length);
                }
                Form form;
                try {
                    form = new Form(fields[0], Integer.parseInt(fields[1]),
                            Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                            Integer.parseInt(fields[4]), parseBoolean(fields[5]),
                            Integer.parseInt(fields[6]), parseBoolean(fields[7]),
                            Integer.parseInt(fields[8]));
                } catch (RuntimeException error) {
                    throw new IllegalStateException(resource + ":" + lineNumber
                            + " contains invalid polymorph metadata", error);
                }
                Form previous = forms.put(key(form.selection()), form);
                if (previous != null) {
                    throw new IllegalStateException(resource + ":" + lineNumber
                            + " duplicates selection " + form.selection());
                }
                if (form.standardMenu()) menu.add(form);
                if (form.randomWeight() > 0) random.add(form);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read polymorph resource: " + resource, error);
        }
        int randomTotal = random.stream().mapToInt(Form::randomWeight).sum();
        if (menu.size() != 96 || random.size() != 21
                || randomTotal != RANDOM_TOTAL_WEIGHT) {
            throw new IllegalStateException("Invalid selected polymorph resource totals: forms="
                    + forms.size() + " menu=" + menu.size() + " random=" + random.size()
                    + " weight=" + randomTotal);
        }
        return new Catalog(Map.copyOf(forms), List.copyOf(forms.values()),
                List.copyOf(menu), List.copyOf(random));
    }

    private static boolean parseBoolean(String value) {
        if (value.equals("1")) return true;
        if (value.equals("0")) return false;
        throw new IllegalArgumentException("boolean field must be zero or one");
    }

    private static String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record Catalog(Map<String, Form> byName, List<Form> allForms,
                           List<Form> standardMenuForms, List<Form> randomForms) {}
}
