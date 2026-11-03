package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Native Gereng-style Adena spell teacher eligibility and price rules. */
final class SpellShopCatalog {
    static final int BASE_PRICE = 6_000;
    private static final int[] NATIVE_TEACHER_BASE_PRICE =
            {2_150, 2_450, 1_800, 5_580, 1_950, 620, 470};
    static final int MAX_NATIVE_SKILL_ID = 220;

    private SpellShopCatalog() {}

    /** Wire ids are zero-based; S_ADD_SPELL and server skill ids are one-based. */
    static List<Integer> availableWireSkillIds(CharacterSummary character,
                                               Set<Integer> learnedSkillIds) {
        if (character == null) return List.of();
        Set<Integer> eligible = new LinkedHashSet<>();
        int level = Math.max(1, character.level());
        switch (character.type()) {
            case 0 -> { // Crown
                addCircle(eligible, level, 10, 0);
                addCircle(eligible, level, 20, 1);
            }
            case 1 -> addCircle(eligible, level, 50, 0); // Knight
            case 2 -> { // Elf
                addCircle(eligible, level, 8, 0);
                addCircle(eligible, level, 16, 1);
                addCircle(eligible, level, 24, 2);
            }
            case 3 -> { // Wizard
                addCircle(eligible, level, 4, 0);
                addCircle(eligible, level, 8, 1);
                addCircle(eligible, level, 12, 2);
            }
            case 4 -> { // Dark elf: common magic teacher portion
                addCircle(eligible, level, 12, 0);
                addCircle(eligible, level, 24, 1);
            }
            default -> { }
        }
        Set<Integer> learned = learnedSkillIds == null ? Set.of() : learnedSkillIds;
        List<Integer> available = new ArrayList<>();
        for (int wireId : eligible) {
            if (!learned.contains(wireId + 1)) available.add(wireId);
        }
        return List.copyOf(available);
    }

    static long priceForWireSkillId(int wireSkillId) {
        if (wireSkillId < 0 || wireSkillId >= MAX_NATIVE_SKILL_ID) {
            throw new IllegalArgumentException("invalid native spell-shop skill id");
        }
        long circle = wireSkillId / 8L + 1L;
        return Math.multiplyExact(BASE_PRICE, Math.multiplyExact(circle, circle));
    }

    static long totalPrice(List<Integer> wireSkillIds) {
        return totalPrice(wireSkillIds, BASE_PRICE);
    }

    static List<Integer> availableNativeTeacherWireSkillIds(
            CharacterSummary character, Set<Integer> learnedSkillIds) {
        if (character == null) return List.of();
        LinkedHashSet<Integer> eligible = new LinkedHashSet<>();
        int level = Math.max(1, character.level());
        switch (character.type()) {
            case 5 -> {
                addRange(eligible, level, 15, 180, 183);
                addRange(eligible, level, 30, 184, 191);
                addRange(eligible, level, 45, 192, 194);
            }
            case 6 -> {
                addRange(eligible, level, 10, 200, 207);
                addRange(eligible, level, 20, 208, 211);
                addRange(eligible, level, 30, 212, 215);
                addRange(eligible, level, 40, 216, 219);
            }
            default -> eligible.addAll(availableWireSkillIds(character, Set.of()));
        }
        Set<Integer> learned = learnedSkillIds == null ? Set.of() : learnedSkillIds;
        return eligible.stream().filter(wire -> !learned.contains(wire + 1)).toList();
    }

    static int nativeTeacherBasePrice(CharacterSummary character) {
        if (character == null || character.type() < 0
                || character.type() >= NATIVE_TEACHER_BASE_PRICE.length) {
            return BASE_PRICE;
        }
        return NATIVE_TEACHER_BASE_PRICE[character.type()];
    }

    static long totalPrice(List<Integer> wireSkillIds, int basePrice) {
        if (wireSkillIds == null || wireSkillIds.isEmpty()) {
            throw new IllegalArgumentException("native spell purchase is empty");
        }
        if (basePrice <= 0) throw new IllegalArgumentException("invalid spell base price");
        long total = 0;
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer wireId : wireSkillIds) {
            if (wireId == null || !unique.add(wireId)) {
                throw new IllegalArgumentException("duplicate native spell purchase row");
            }
            if (wireId < 0 || wireId >= MAX_NATIVE_SKILL_ID) {
                throw new IllegalArgumentException("invalid native spell-shop skill id");
            }
            long circle = wireId / 8L + 1L;
            total = Math.addExact(total,
                    Math.multiplyExact(basePrice, Math.multiplyExact(circle, circle)));
        }
        return total;
    }

    private static void addCircle(Set<Integer> target, int level,
                                  int requiredLevel, int circleIndex) {
        if (level < requiredLevel) return;
        int first = circleIndex * 8;
        for (int wireId = first; wireId < first + 8; wireId++) target.add(wireId);
    }

    private static void addRange(Set<Integer> target, int level, int requiredLevel,
                                 int firstWireId, int lastWireId) {
        if (level < requiredLevel) return;
        for (int wireId = firstWireId; wireId <= lastWireId; wireId++) {
            target.add(wireId);
        }
    }
}
