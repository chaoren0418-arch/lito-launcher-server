package local.lito381.server;

/** Pure, transport-neutral subset of the retained Tournament admission rules. */
final class TournamentRoomPolicy {
    private TournamentRoomPolicy() {}

    static boolean nativeRoomCapacity(int capacity) { return capacity >= 2 && capacity <= 100; }

    static boolean canCreate(int channelId, long level) {
        TournamentPresence.Tier tier = TournamentPresence.recommendedChannel(level);
        return tier != null && channelId >= 0 && channelId < 4 && channelId <= tier.id();
    }

    static boolean canJoin(int channelId, long level, boolean allowsUpperTier) {
        TournamentPresence.Tier tier = TournamentPresence.recommendedChannel(level);
        return tier != null && channelId >= 0 && channelId < 4
                && (channelId <= tier.id() || allowsUpperTier);
    }

    static int combatantCapacity(int capacity, BattleRules.Mode mode) {
        if (!nativeRoomCapacity(capacity) || mode == null || !mode.isBattleScene())
            throw new IllegalArgumentException("invalid room capacity or mode");
        return switch (mode) {
            case PET_RACE -> Math.min(capacity, BattlePetRaceCourseCatalog.MAXIMUM_PLAYERS);
            case TIME_ATTACK -> Math.min(capacity, BattleTimeAttackEncounter.MAXIMUM_PLAYERS);
            case SIEGE_WAR -> Math.min(capacity, BattleRules.SIEGE_MAXIMUM_PLAYERS);
            default -> capacity;
        };
    }

    static BattleRules.RuleSet rules(int mode, int mapId) {
        BattleRules.Mode selected = BattleRules.Mode.fromWire(mode);
        if (!selected.isBattleScene()) throw new IllegalArgumentException("non-battle room mode");
        return BattleRules.decode(selected, mapId, BattleControlCatalog.defaults());
    }

    /** Readiness is only a waiting-room fact, never permission to start a world. */
    static boolean readySatisfied(BattleRules.RuleSet rules, int users, int ready,
                                  int firstTeam, int secondTeam, boolean equalSides) {
        if (rules == null || users < 0 || ready < 0 || ready > users
                || firstTeam < 0 || secondTeam < 0 || firstTeam + secondTeam != users)
            throw new IllegalArgumentException("invalid ready roster");
        if (!rules.mode().isBattleScene() || users < rules.minimumPlayers() || users != ready) return false;
        if (!rules.teamBased()) return true;
        if (firstTeam == 0 || secondTeam == 0 || (equalSides && firstTeam != secondTeam)) return false;
        return rules.mode() != BattleRules.Mode.SIEGE_WAR
                || (firstTeam >= BattleRules.SIEGE_MINIMUM_PLAYERS_PER_FORCE
                && secondTeam >= BattleRules.SIEGE_MINIMUM_PLAYERS_PER_FORCE
                && firstTeam <= BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE
                && secondTeam <= BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE);
    }
}
