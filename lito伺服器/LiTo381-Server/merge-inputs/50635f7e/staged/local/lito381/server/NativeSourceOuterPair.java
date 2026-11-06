package local.lito381.server;

import java.util.concurrent.locks.ReentrantLock;

/** Source outer producer pairs reject nested contention before scope registration or discovery. */
final class NativeSourceOuterPair {
  static void nativeSourcePairThirdOriginalRun(
      ClientSession first, ClientSession second, Runnable action) {
    if (action == null
        || first == null
        || second == null
        || !NativeSourcePairedScope.enabled(first)) {
      NativeSourcePlayerActionLock.runPairOrdered(first, second, action);
      return;
    }
    var a = first.nativePlayerActionLock();
    var b = second.nativePlayerActionLock();
    ReentrantLock acquired = null;
    ClientSession owner = null;
    if (a.isHeldByCurrentThread() && !b.isHeldByCurrentThread()) {
      if (!b.tryLock()) return;
      acquired = b;
      owner = second;
    } else if (b.isHeldByCurrentThread() && !a.isHeldByCurrentThread()) {
      if (!a.tryLock()) return;
      acquired = a;
      owner = first;
    }
    try {
      var proof = NativeSourceLightPairLife.capture(first, second);
      NativeSourcePlayerActionLock.runPairOrdered(
          first,
          second,
          () -> {
            if (proof.locked()) action.run();
          });
    } finally {
      if (acquired != null) {
        acquired.unlock();
        // Match original runLockedPair: never start a peer drain while the caller still owns either
        // mutex.
        if (!a.isHeldByCurrentThread() && !b.isHeldByCurrentThread())
          NativePlayerActionScope.unlocked(owner);
      }
    }
  }

  private NativeSourceOuterPair() {}

  // ported from candidate 50635f7e, post-freeze ASM members.
  static void run(ClientSession clientSession, ClientSession clientSession2, Runnable runnable) {
    NativeSourcePairThird.run(clientSession, clientSession2, runnable, 1);
  }
}
