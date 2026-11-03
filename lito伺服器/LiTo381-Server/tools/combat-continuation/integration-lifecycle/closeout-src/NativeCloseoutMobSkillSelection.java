package local.lito381.server;

import java.util.ArrayList;
import static local.lito381.server.NativeDeNpcMobSkill.*;

/** Source L1MobSkillUse selection; live actor admission and effect adapters are separate. */
final class NativeCloseoutMobSkillSelection {
    interface Ports<T> {
        T attacker();
        Iterable<T> visible();
        boolean npc(T target);
        boolean player(T target);
        boolean petOrSummon(T target);
        boolean dead(T target);
        boolean ghost(T target);
        boolean hates(T target);
        boolean lineOfSight(T target);
        int family(T target);
        int hp(T target);
        int maxHp(T target);
        int distance(T target);
        boolean delayed(int row);
        int useCount(int row);
        int nextInt(int bound);
        boolean physical(NativeCloseoutControlledNpcData.Row row, T target);
        boolean magic(NativeCloseoutControlledNpcData.Row row, T target);
        boolean summon(NativeCloseoutControlledNpcData.Row row, T target);
        boolean poly(NativeCloseoutControlledNpcData.Row row, T target);
        boolean shockStun(NativeCloseoutControlledNpcData.Row row, T target);
        boolean cancellation(NativeCloseoutControlledNpcData.Row row, T target);
        boolean weaponBreak(NativeCloseoutControlledNpcData.Row row, T target);
        void incrementUseCount(int row);
        void startDelay(int millis, int row);
    }
    record Choice<T>(NativeCloseoutControlledNpcData.Row row, T target) {}
    private static final class Scan<T> { T target; Scan(T target) { this.target = target; } }
    static <T> boolean trigger(NativeCloseoutControlledNpcData.Data data, Ports<T> ports, T original) {
        if (data == null || data.rows().isEmpty() || data.rows().get(0).type() == TYPE_NONE) return false;
        var scan = new Scan<>(original);
        for (var row : data.rows()) {
            if (row.type() == TYPE_NONE) break;
            scan.target = row.changeTarget() > 0 ? changeTarget(row, ports, scan.target) : original;
            if (usable(row, ports, scan, false)) return true;
        }
        return false;
    }
    static <T> Choice<T> select(NativeCloseoutControlledNpcData.Data data, Ports<T> ports, T original, boolean triggerRandom) {
        if (data == null || data.rows().isEmpty() || data.rows().get(0).type() == TYPE_NONE) return null;
        var scan = new Scan<>(original);
        var eligible = new ArrayList<NativeCloseoutControlledNpcData.Row>();
        for (var row : data.rows()) {
            if (row.type() == TYPE_NONE) break;
            scan.target = row.changeTarget() > 0 ? changeTarget(row, ports, scan.target) : original;
            if (usable(row, ports, scan, triggerRandom)) eligible.add(row);
        }
        // Source retains its final scan target when choosing the row; do not silently retarget here.
        return eligible.isEmpty() ? null : new Choice<>(eligible.get(ports.nextInt(eligible.size())), scan.target);
    }
    static <T> boolean use(NativeCloseoutControlledNpcData.Data data, Ports<T> ports, T original, boolean triggerRandom) {
        var choice = select(data, ports, original, triggerRandom);
        if (choice == null) return false;
        var row = choice.row(); var target = choice.target();
        boolean accepted = switch (row.type()) {
            case TYPE_PHYSICAL_ATTACK -> ports.physical(row, target);
            case TYPE_MAGIC_ATTACK -> ports.magic(row, target);
            case TYPE_SUMMON -> ports.summon(row, target);
            case TYPE_POLY -> ports.poly(row, target);
            case AHTHARAS_1 -> ports.shockStun(row, target);
            case AHTHARAS_2 -> ports.cancellation(row, target);
            case AHTHARAS_3 -> ports.weaponBreak(row, target);
            default -> false;
        };
        if (accepted) { ports.incrementUseCount(row.actNo()); ports.startDelay(row.reuse(), row.actNo()); }
        return accepted;
    }
    private static <T> boolean usable(NativeCloseoutControlledNpcData.Row row, Ports<T> ports, Scan<T> scan, boolean triggerRandom) {
        boolean usable = false;
        if (ports.delayed(row.actNo())) return false;
        if (triggerRandom || row.type() == TYPE_SUMMON || row.type() == TYPE_POLY) {
            if (row.triggerRandom() > 0) {
                int chance = ports.nextInt(100) + 1;
                if (chance < row.triggerRandom()) usable = true; else return false;
            }
        }
        if (row.triggerHp() > 0) {
            int ratio = ports.hp(ports.attacker()) * 100 / ports.maxHp(ports.attacker());
            if (ratio <= row.triggerHp()) usable = true; else return false;
        }
        if (row.triggerCompanionHp() > 0) {
            T companion = minimumFamilyHp(ports);
            if (companion == null) return false;
            int ratio = ports.hp(companion) * 100 / ports.maxHp(companion);
            if (ratio <= row.triggerCompanionHp()) { usable = true; scan.target = companion; } else return false;
        }
        if (row.triggerRange() != 0) {
            if (distance(row.triggerRange(), ports.distance(scan.target))) usable = true; else return false;
        }
        if (row.triggerCount() > 0) {
            if (ports.useCount(row.actNo()) < row.triggerCount()) usable = true; else return false;
        }
        return usable;
    }
    static boolean distance(int triggerRange, int distance) {
        return triggerRange < 0 && distance <= Math.abs(triggerRange) || triggerRange > 0 && distance >= triggerRange;
    }
    private static <T> T minimumFamilyHp(Ports<T> ports) {
        T minimum = null; int ratio = 100; int family = ports.family(ports.attacker());
        for (T target : ports.visible()) if (ports.npc(target) && ports.family(target) == family) {
            int candidate = ports.hp(target) * 100 / ports.maxHp(target);
            if (candidate < ratio) { ratio = candidate; minimum = target; }
        }
        return minimum;
    }
    private static <T> T changeTarget(NativeCloseoutControlledNpcData.Row row, Ports<T> ports, T previous) {
        if (row.changeTarget() == CHANGE_TARGET_ME) return ports.attacker();
        if (row.changeTarget() != CHANGE_TARGET_RANDOM) return previous;
        var candidates = new ArrayList<T>();
        for (T target : ports.visible()) if (ports.player(target) || ports.petOrSummon(target)) {
            if (!distance(row.triggerRange(), ports.distance(target))) continue;
            if (!ports.lineOfSight(target)) continue;
            if (!ports.hates(target)) continue;
            if (ports.dead(target)) continue;
            if (ports.player(target) && ports.ghost(target)) continue;
            candidates.add(target);
        }
        return candidates.isEmpty() ? previous : candidates.get(ports.nextInt(candidates.size() * 100) / 100);
    }
    private NativeCloseoutMobSkillSelection() {}
}
