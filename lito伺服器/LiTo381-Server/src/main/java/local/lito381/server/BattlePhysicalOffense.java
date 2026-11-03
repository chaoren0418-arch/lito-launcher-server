package local.lito381.server;

/** Exact 3.81 weapon material/blessing and retained passive attribute damage. */
final class BattlePhysicalOffense {
    private static final int MATERIAL_SILVER = 14;
    private static final int MATERIAL_MITHRIL = 17;
    private static final int MATERIAL_ORIHARUKON = 22;

    private BattlePhysicalOffense() {}

    record NpcOutcome(
            int materialMaximum, int materialRoll,
            int blessingMaximum, int blessingRoll,
            int attributeResistance, int attributeDamage,
            int totalBonus) {
        static NpcOutcome none() {
            return new NpcOutcome(0, 0, 0, 0, 0, 0, 0);
        }
    }

    record PlayerOutcome(int attributeResistance, int attributeDamage) {
        static PlayerOutcome none() {
            return new PlayerOutcome(0, 0);
        }
    }

    static int materialMaximum(
            SourceWeaponPhysicalStats weapon, SourceNpcPhysicalStats target) {
        if (weapon == null || target == null) {
            throw new IllegalArgumentException("missing material damage input");
        }
        if (!weapon.present()) return 0;
        int material = weapon.attackMaterial();
        return switch (target.undead()) {
            case 1, 3, 5 -> silverFamily(material) ? 20 : 0;
            case 2 -> material == MATERIAL_MITHRIL || material == MATERIAL_ORIHARUKON
                    ? 3 : 0;
            default -> 0;
        };
    }

    static int blessingMaximum(
            SourceWeaponPhysicalStats weapon, SourceNpcPhysicalStats target) {
        if (weapon == null || target == null) {
            throw new IllegalArgumentException("missing blessing damage input");
        }
        // Original 3.81 item convention is 0=blessed, 1=normal, 2=cursed.
        return weapon.present() && weapon.attackBlessing() == 0
                && (target.undead() == 1 || target.undead() == 2 || target.undead() == 3)
                ? 4 : 0;
    }

    static int attributeResistance(
            SourceWeaponPhysicalStats weapon, SourceNpcPhysicalStats target) {
        if (weapon == null || target == null) {
            throw new IllegalArgumentException("missing attribute damage input");
        }
        int kind = weapon.combatTraits().attributeKind();
        if (kind == 0) return 0;
        if (kind == 16 || kind == 32 || kind == 64 || kind == 128) {
            return target.magicResistance();
        }
        if (target.weakAttribute() == kind) return -50;
        if (target.weakAttribute() == -kind) return 50;
        return 0;
    }

    static int attributeDamage(
            SourceWeaponPhysicalStats weapon, SourceNpcPhysicalStats target) {
        int base = weapon.combatTraits().attributeDamage();
        if (base == 0) return 0;
        int resistance = attributeResistance(weapon, target);
        return attributeDamage(base, resistance);
    }

    static int attributeDamage(int base, int resistance) {
        if (base < 0) {
            throw new IllegalArgumentException("negative passive attribute damage");
        }
        if (base == 0) return 0;
        int resistanceFloor = (int) (0.32D * Math.abs(resistance));
        if (resistance < 0) resistanceFloor *= -1;
        double coefficient = 1.0D - resistanceFloor / 32.0D;
        // The retained compound assignment truncates toward zero and does not
        // clamp this layer; resistance above 100 can therefore subtract from
        // the physical subtotal before the final non-negative clamp.
        return (int) (base * coefficient);
    }

    static PlayerOutcome againstPlayer(
            SourceWeaponPhysicalStats weapon, int attributeResistance) {
        if (weapon == null) {
            throw new IllegalArgumentException("missing player attribute weapon");
        }
        int base = weapon.combatTraits().attributeDamage();
        return new PlayerOutcome(attributeResistance,
                attributeDamage(base, attributeResistance));
    }

    static NpcOutcome againstNpc(
            SourceWeaponPhysicalStats weapon, SourceNpcPhysicalStats target,
            int materialRoll, int blessingRoll) {
        int materialMaximum = materialMaximum(weapon, target);
        int blessingMaximum = blessingMaximum(weapon, target);
        validateRoll("material", materialRoll, materialMaximum);
        validateRoll("blessing", blessingRoll, blessingMaximum);
        int resistance = attributeResistance(weapon, target);
        int attributeDamage = attributeDamage(weapon, target);
        int total = Math.addExact(materialRoll, blessingRoll);
        total = Math.addExact(total, attributeDamage);
        return new NpcOutcome(
                materialMaximum, materialRoll, blessingMaximum, blessingRoll,
                resistance, attributeDamage, total);
    }

    private static void validateRoll(String name, int roll, int maximum) {
        if ((maximum == 0 && roll != 0)
                || (maximum > 0 && (roll < 1 || roll > maximum))) {
            throw new IllegalArgumentException("invalid " + name + " damage roll");
        }
    }

    private static boolean silverFamily(int material) {
        return material == MATERIAL_SILVER
                || material == MATERIAL_MITHRIL
                || material == MATERIAL_ORIHARUKON;
    }
}
