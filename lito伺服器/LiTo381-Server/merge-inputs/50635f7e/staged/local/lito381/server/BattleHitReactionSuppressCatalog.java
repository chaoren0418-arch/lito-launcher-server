// ported from candidate 50635f7e, integration-lifecycle final frame, issue #67
package local.lito381.server;

/** Selected-source {@code 系統_受擊無動作gfx} snapshot. */
import java.util.Set;

final class BattleHitReactionSuppressCatalog {
    private static final Set<Integer> SUPPRESSED = Set.of(53, 173, 183, 185, 187, 240, 1011, 1062, 1180, 1390, 1572, 1773, 1791, 2389, 2402, 2419, 2421, 2495, 2738, 2741, 2747, 2749, 2755, 2757, 2773, 2803, 2807, 3049, 3066, 3086, 3101, 3180, 3387, 3664, 3744, 4068, 4168, 4171, 4218, 4466, 4493, 4527, 4533, 4569, 4587, 4595, 4677, 4698, 4746, 4790, 4851, 4912, 5008, 5072, 5212, 5274, 5284, 5293, 5297, 5365, 5373, 5471, 5478, 5507, 5547, 5708, 5767, 5786, 5818, 6566, 6569, 6581, 6595, 6778, 7234, 7236, 10042, 10596, 10621, 10626, 10632, 10655, 10773, 10884, 10891, 11666, 12289, 12848, 12854, 12870, 13001, 13005, 13011, 13024, 13030, 13038, 13042, 13044, 13058, 13064, 13118, 13132, 14036, 14049, 14084, 14182, 14273);

    private BattleHitReactionSuppressCatalog() {
    }

    static boolean nativeSourceHitReactionOriginalShouldSend(int n) {
        return n <= 0 || !SUPPRESSED.contains(n);
    }

    static Set<Integer> nativeSourceHitReactionOriginalSuppressedGfx() {
        return SUPPRESSED;
    }

    static boolean shouldSendDamageAction(int gfx) {
        return NativeSourceHitReaction.shouldSendDamageAction(gfx);
    }

    static Set suppressedGfx() {
        return NativeSourceHitReaction.suppressedGfx();
    }
}
