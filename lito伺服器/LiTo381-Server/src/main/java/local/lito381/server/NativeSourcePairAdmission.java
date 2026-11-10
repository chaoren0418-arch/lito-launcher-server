package local.lito381.server;

/**
 * Explicit new-action ports only; accepted continuations keep original synchronous completion
 * semantics.
 */
final class NativeSourcePairAdmission {
  static boolean both(ClientSession first, ClientSession second) {
    return second != null
        && NativeSourcePairedScope.enabled(first)
        && first.nativePlayerActionLock().isHeldByCurrentThread()
        && second.nativePlayerActionLock().isHeldByCurrentThread();
  }

  static void nativeSourcePairThirdOriginalAdmit(
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
    ClientSession acquired = null;
    if (a.isHeldByCurrentThread() && !b.isHeldByCurrentThread()) {
      if (!b.tryLock()) return;
      acquired = second;
    } else if (b.isHeldByCurrentThread() && !a.isHeldByCurrentThread()) {
      if (!a.tryLock()) return;
      acquired = first;
    }
    try {
      NativeSourcePlayerActionLock.runPairOrdered(first, second, action);
    } finally {
      if (acquired != null) {
        acquired.nativePlayerActionLock().unlock();
        if (!a.isHeldByCurrentThread() && !b.isHeldByCurrentThread())
          NativePlayerActionScope.unlocked(acquired);
      }
    }
  }

  static void raw(
      ClientSession first,
      ClientSession second,
      Runnable action,
      NativeSourceRawDamage.Action source) {
    if (source.accepted) NativeSourcePlayerActionLock.runPairOrdered(first, second, action);
    else admit(first, second, action);
  }

  static void doll(ClientSession first, ClientSession second, Runnable action) {
    var frame = NativePlayerCommit.frame();
    if (frame != null && frame.accepted)
      NativeSourcePlayerActionLock.runPairOrdered(first, second, action);
    else admit(first, second, action);
  }

  private NativeSourcePairAdmission() {}

  // ported from candidate 50635f7e, post-freeze ASM members.
  static void admit(ClientSession clientSession, ClientSession clientSession2, Runnable runnable) {
    NativeSourcePairThird.run(clientSession, clientSession2, runnable, 2);
  }
}
