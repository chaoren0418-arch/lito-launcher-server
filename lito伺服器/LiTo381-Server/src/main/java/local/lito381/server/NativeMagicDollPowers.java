package local.lito381.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable runtime view of the exact etcitem_doll_power_new rows owned by one
 * summoned doll. Values are grouped without discarding the original rows so
 * timed, active and status executors retain every native parameter.
 */
final class NativeMagicDollPowers {
    private static final NativeMagicDollPowers EMPTY =
            new NativeMagicDollPowers(List.of());

    private final List<SourceMagicDollPower> definitions;
    private final Map<String, Integer> values;

    private NativeMagicDollPowers(List<SourceMagicDollPower> definitions) {
        this.definitions = List.copyOf(definitions);
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (SourceMagicDollPower power : definitions) {
            totals.merge(power.className(), power.valueMain(), Math::addExact);
        }
        values = Map.copyOf(totals);
    }

    static NativeMagicDollPowers empty() {
        return EMPTY;
    }

    static NativeMagicDollPowers from(SourceMagicDollType type) {
        if (type == null || type.powerDefinitions().isEmpty()) return EMPTY;
        return new NativeMagicDollPowers(type.powerDefinitions());
    }

    int value(String className) {
        return values.getOrDefault(className, 0);
    }

    boolean has(String className) {
        return values.containsKey(className);
    }

    /**
     * Retained economic abilities store an additive percentage (5 means
     * +5 percent) and their consumers multiply the ordinary server rate by
     * {@code 1 + value / 100}.
     */
    double percentMultiplier(String className) {
        int percent = value(className);
        if (percent <= -100) {
            throw new IllegalStateException(
                    "native magic-doll percentage must remain above -100: "
                            + className + "=" + percent);
        }
        return 1.0D + percent / 100.0D;
    }

    List<SourceMagicDollPower> definitions() {
        return definitions;
    }

    List<SourceMagicDollPower> definitions(String className) {
        List<SourceMagicDollPower> result = new ArrayList<>();
        for (SourceMagicDollPower power : definitions) {
            if (power.className().equals(className)) result.add(power);
        }
        return List.copyOf(result);
    }

    List<SourceMagicDollPower> activeDefinitions() {
        List<SourceMagicDollPower> result = new ArrayList<>();
        for (SourceMagicDollPower power : definitions) {
            if (power.className().startsWith("ACTIVE_")) result.add(power);
        }
        return List.copyOf(result);
    }

    List<SourceMagicDollPower> timedDefinitions() {
        List<SourceMagicDollPower> result = new ArrayList<>();
        for (SourceMagicDollPower power : definitions) {
            if (power.className().startsWith("TIMED_")) result.add(power);
        }
        return List.copyOf(result);
    }

    List<Integer> autoSkillIds() {
        List<Integer> result = new ArrayList<>();
        for (SourceMagicDollPower power : definitions) {
            if (power.className().equals("AUTO_SKILL")) {
                result.add(power.passiveSkillId());
            }
        }
        return List.copyOf(result);
    }
}
