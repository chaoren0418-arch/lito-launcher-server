package local.lito381.server;

import java.util.function.IntSupplier;

/**
 * Versioned historical policy for native LiTo Siege War.
 *
 * <p>The 2003 NCsoft update announcement reported an initial 30v30 maximum.
 * A later 2005 LiTo/LWC gameplay guide records the live expanded contract:
 * 15v15 minimum, 50v50 maximum, twenty minutes, and attack/defence roles
 * randomized every match independently of the selected Force.  The later
 * contract is production authority because it documents observed gameplay
 * and its 100-player total matches the selected client's room-capacity domain.
 * Both revisions remain preserved; the earlier value is not treated as an
 * error or silently overwritten.</p>
 */
final class NativeLitoSiegePolicy {
    static final String INITIAL_2003_SOURCE_URL =
            "https://www.gamemeca.com/view.php?gid=45989";
    static final int INITIAL_2003_MAXIMUM_PER_FORCE = 30;
    static final int ACTIVE_2005_MINIMUM_PER_FORCE = 15;
    static final int ACTIVE_2005_MAXIMUM_PER_FORCE = 50;
    static final int ACTIVE_DURATION_SECONDS = 20 * 60;
    static final int OUTER_TOWERS_REQUIRED = 2;
    static final String ACTIVE_2005_SOURCE_URL =
            "https://www.gamemeca.com/view.php?gid=125822";
    static final String PRODUCTION_AUTHORITY =
            "later-2005-observed-gameplay-contract";

    private NativeLitoSiegePolicy() {}

    static int activeMaximumTotal() {
        return ACTIVE_2005_MAXIMUM_PER_FORCE * 2;
    }

    static boolean historicalCapacityExpanded() {
        return ACTIVE_2005_MAXIMUM_PER_FORCE > INITIAL_2003_MAXIMUM_PER_FORCE;
    }

    static boolean participantBandCutoffsRecovered() {
        return false;
    }

    static boolean timedSupplyPolicyRecovered() {
        return false;
    }

    static boolean mpRecoveryCirclePolicyRecovered() {
        return false;
    }

    static int resolveAttackerForce(int requestedForce, IntSupplier randomBinary) {
        if (requestedForce == 0 || requestedForce == 1) return requestedForce;
        if (requestedForce != -1 || randomBinary == null) {
            throw new IllegalArgumentException("invalid Siege attacker force");
        }
        int selected = randomBinary.getAsInt();
        if (selected != 0 && selected != 1) {
            throw new IllegalArgumentException("Siege random force must be binary");
        }
        return selected;
    }
}
