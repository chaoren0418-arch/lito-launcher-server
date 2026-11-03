// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClientSession;
import local.lito381.server.CombatAccess;
import local.lito381.server.CombatRules;

final class NativePetControls {
    static final List<String> KEYS = List.of("PrinceChaForPet", "KnightChaForPet", "ElfChaForPet", "WizardChaForPet", "DarkelfChaForPet", "DragonKnightChaForPet", "IllusionistChaForPet");
    static final List<Integer> DEFAULTS = List.of(Integer.valueOf(6), Integer.valueOf(0), Integer.valueOf(12), Integer.valueOf(6), Integer.valueOf(6), Integer.valueOf(6), Integer.valueOf(6));

    static List<Integer> parse(Properties properties) {
        ArrayList<Integer> arrayList = new ArrayList<Integer>(7);
        for (int i = 0; i < KEYS.size(); ++i) {
            arrayList.add(Integer.parseInt(properties.getProperty(KEYS.get(i), DEFAULTS.get(i).toString())));
        }
        return List.copyOf(arrayList);
    }

    static void loadInto(Map<String, Double> map, Set<String> set, Properties properties, Path path) throws IOException {
        List<Integer> list;
        try {
            list = NativePetControls.parse(properties);
        }
        catch (NumberFormatException numberFormatException) {
            throw new IOException(String.valueOf(path) + ": invalid source pet charisma control", numberFormatException);
        }
        for (int i = 0; i < KEYS.size(); ++i) {
            String string = KEYS.get(i);
            map.put(string, list.get(i).doubleValue());
            if (properties.containsKey(string)) continue;
            set.add(string);
        }
    }

    static int configuredBonus(int n) {
        return n < 0 || n >= KEYS.size() ? 0 : (int)CombatRules.number(KEYS.get(n), DEFAULTS.get(n).intValue());
    }

    static int configuredBonus(CharacterSummary characterSummary) {
        return NativePetControls.configuredBonus(characterSummary == null ? 0 : characterSummary.type());
    }

    static void loadFromProperties(Properties properties, Map<String, Double> map, Set<String> set, Path path) throws IOException {
        NativePetControls.loadInto(map, set, properties, path);
    }

    static int effectiveCharisma(ClientSession clientSession, CharacterSummary characterSummary) {
        int n = CombatAccess.integerCall(clientSession, "effectiveCharisma", "(Llocal/lito381/server/CharacterSummary;)I", characterSummary);
        return Math.max(1, Math.min(254, n));
    }

    static int available(int n, int n2, int n3) {
        return n + n2 - n3;
    }

    static boolean canTake(int n, int n2, int n3, int n4, int n5) {
        return n < 8 && NativePetControls.available(n2, n3, n4) >= n5;
    }

    private NativePetControls() {
    }
}
