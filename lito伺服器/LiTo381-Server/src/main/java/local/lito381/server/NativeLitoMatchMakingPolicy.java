package local.lito381.server;

/**
 * Evidence boundary for the original LiTo automatic-matchmaking objective.
 *
 * <p>A contemporary 2003-11-10 GameMeca report attributes the premium-server
 * update to NCsoft and describes Match Making as finding gamers with a win
 * rate similar to the requester.  That is direct evidence for sorting
 * compatible waiters by win-rate proximity.  The article and selected client
 * do not preserve a maximum permitted difference, zero-history treatment or
 * equal-distance tie-break, so those details remain explicit server policy
 * rather than historical claims.</p>
 */
final class NativeLitoMatchMakingPolicy {
    static final String HISTORICAL_SOURCE_URL =
            "https://www.gamemeca.com/view.php?gid=45989";
    static final String HISTORICAL_SOURCE_DATE = "2003-11-10";
    static final String HISTORICAL_SELECTION_GOAL = "similar-player-win-rates";
    static final String CURRENT_DISTANCE_POLICY =
            "absolute-wins-over-wins-plus-losses-distance";
    static final String CURRENT_ZERO_HISTORY_POLICY =
            "both-zero-equal-one-zero-maximally-distant";
    static final String CURRENT_TIE_BREAK_POLICY = "fifo-sequence";

    private NativeLitoMatchMakingPolicy() {}

    static boolean maximumDistanceRecovered() {
        return false;
    }

    static boolean zeroHistoryTreatmentRecovered() {
        return false;
    }

    static boolean equalDistanceTieBreakRecovered() {
        return false;
    }

    static double distance(GameStateStore.TournamentStatistics left,
                           GameStateStore.TournamentStatistics right) {
        int leftGames = left.winRateDenominator();
        int rightGames = right.winRateDenominator();
        if (leftGames == 0 && rightGames == 0) return 0.0;
        if (leftGames == 0 || rightGames == 0) return 1.0;
        return Math.abs((double) left.wins() / leftGames
                - (double) right.wins() / rightGames);
    }

    static int compareCandidates(GameStateStore.TournamentStatistics requester,
                                 GameStateStore.TournamentStatistics left,
                                 long leftSequence,
                                 GameStateStore.TournamentStatistics right,
                                 long rightSequence) {
        int distance = Double.compare(
                distance(requester, left), distance(requester, right));
        return distance != 0 ? distance : Long.compare(leftSequence, rightSequence);
    }
}
