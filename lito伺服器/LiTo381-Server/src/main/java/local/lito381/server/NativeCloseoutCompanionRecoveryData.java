package local.lito381.server;

/**
 * T135 (#386), ported from candidate 046c58c5: recovery cadence of the actual pet / summon NPC
 * template, frozen at the existing controlled factory boundary.
 *
 * <p>Source: {@code timecontroller/pet/HprPet.java:44-53} and {@code MprPet.java:48-57}. A
 * non-positive interval is 20 seconds and a non-positive amount is 1 point, for HP and MP alike.
 * (Natural monsters use 10 / 2 / 30 / 1 in {@code NpcHprTimer}; that is a different table and is
 * not part of this port.)
 */
final class NativeCloseoutCompanionRecoveryData {
  static final int DEFAULT_INTERVAL_SECONDS = 20;
  static final int DEFAULT_AMOUNT = 1;

  /** Seconds between ticks and points per tick. All four values are positive. */
  record Data(int hpInterval, int hpAmount, int mpInterval, int mpAmount) {
    Data {
      if (hpInterval <= 0 || hpAmount <= 0 || mpInterval <= 0 || mpAmount <= 0)
        throw new IllegalArgumentException("invalid controlled recovery cadence");
    }
  }

  static Data of(NativeDeNpcTemplate npc) {
    return new Data(
        npc.get_hprinterval() > 0 ? npc.get_hprinterval() : DEFAULT_INTERVAL_SECONDS,
        npc.get_hpr() > 0 ? npc.get_hpr() : DEFAULT_AMOUNT,
        npc.get_mprinterval() > 0 ? npc.get_mprinterval() : DEFAULT_INTERVAL_SECONDS,
        npc.get_mpr() > 0 ? npc.get_mpr() : DEFAULT_AMOUNT);
  }

  private NativeCloseoutCompanionRecoveryData() {}
}
