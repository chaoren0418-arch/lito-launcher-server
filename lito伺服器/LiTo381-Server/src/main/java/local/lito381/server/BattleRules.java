package local.lito381.server;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Native LiTo match rules and server-owned match state.
 *
 * <p>The protected 3.81C client manages the native 0..5 mode independently
 * through C_D1/S_D5 subtype 2, while the map travels independently through
 * subtype 3. C_CC/C_DD carry event flags, event type, winner count, a five-
 * class mask and up to eight non-zero original-server ids. The server
 * keeps the independent mode in {@code option0} so gameplay code does not
 * confuse it with those four room-dialog fields. Reverse engineering of
 * sub_46B7D0/sub_4A9E50 pins the separate mode and map paths.</p>
 *
 * <p>The exact numeric identities come from agreement between the protected
 * client's LTmapstr category parser ({@code sub_46F9C0}) and room renderer
 * ({@code sub_46DFB0}).  Presentation order is a separate concern.</p>
 */
final class BattleRules {
    // NativeLitoSiegePolicy preserves the 2003 initial 30v30 announcement and
    // the later 2005 live 15v15..50v50 expansion. The selected client retains
    // room capacity through 100 and LTstring[285]/[286] per-force warnings.
    static final int SIEGE_MINIMUM_PLAYERS_PER_FORCE =
            NativeLitoSiegePolicy.ACTIVE_2005_MINIMUM_PER_FORCE;
    static final int SIEGE_MAXIMUM_PLAYERS_PER_FORCE =
            NativeLitoSiegePolicy.ACTIVE_2005_MAXIMUM_PER_FORCE;
    static final int SIEGE_MINIMUM_PLAYERS = SIEGE_MINIMUM_PLAYERS_PER_FORCE * 2;
    static final int SIEGE_MAXIMUM_PLAYERS = SIEGE_MAXIMUM_PLAYERS_PER_FORCE * 2;

    enum Mode {
        // The native S_FD receiver renders maximum/current lap as two HUD
        // values. Contemporary LiTo instructions independently pin the race
        // to four laps and a five-minute limit.
        PET_RACE(0, "petrace", "Pet Race", false, false, false,
                BattlePetRaceCourseCatalog.LAPS, 300,
                List.of(565, 566, 569, 570)),
        DEATHMATCH(1, "deathmatch", "Death Match", true, false, true, 1, 180,
                List.of(564, 514, 509, 508, 551, 504, 505, 552, 506, 526, 525, 516, 531, 534, 529)),
        // Exact LTmapstr parser sub_46F9C0 assigns resource category
        // "questbattle" to wire mode 2, while room renderer sub_46DFB0 maps
        // mode 2 to LTstring[125] "Quest Battle".  Do not use the localized
        // string-table presentation order as a numeric wire order.
        QUEST_BATTLE(2, "questbattle", "Quest Battle", true, true, false, 1, 600,
                List.of(554, 553, 513, 527, 518, 519, 520, 521, 522)),
        // sub_46F9C0 assigns "siegewar" to mode 3.  The ordinary role menu
        // then keeps the odd representative side of each paired resource and
        // excludes the 549/550 event pair, yielding exactly 573/563/547.
        SIEGE_WAR(3, "siegewar", "Siege War", true, true, true, 1,
                NativeLitoSiegePolicy.ACTIVE_DURATION_SECONDS,
                List.of(573, 563, 547)),
        // NC Japan's retained 2005 Battle GP reports explicitly define Time
        // Attack as a seven-minute battle.  The contemporary 06:37 frame is
        // consistent with this exact 420-second contract.
        TIME_ATTACK(4, "timeattack", "Time Attack", false, false, false, 1, 420,
                List.of(540)),
        // Exact parser/renderer agreement: "honormatch" and LTstring[124]
        // are both keyed by wire mode 5.
        HONOR_MATCH(5, "honormatch", "Honor Match", true, true, true, 1, 600,
                List.of(510, 528, 532, 512, 517, 524, 530, 535)),
        // Exact LTmapstr-c.tbl category "town".  The selected LiTo.bin maps
        // that category to wire value 6, enumerates maps 500/560 after the
        // server supplies S_D5 subtype 2 value 6, and can load either scene
        // through S_WORLD followed by S_LITO_START_GAME.  It is not an arena
        // rule and must never be aliased to a development-only battle mode.
        TOWN(6, "town", "Town", false, false, false, 0, 0,
                List.of(500, 560));

        private static final int NATIVE_MODE_COUNT = 6;

        private final int wireId;
        private final String key;
        private final String displayName;
        private final boolean teamBased;
        private final boolean respawn;
        private final boolean playerVersusPlayer;
        private final int defaultGoal;
        private final int defaultSeconds;
        private final List<Integer> maps;

        Mode(int wireId, String key, String displayName, boolean teamBased, boolean respawn,
             boolean playerVersusPlayer, int defaultGoal, int defaultSeconds, List<Integer> maps) {
            this.wireId = wireId;
            this.key = key;
            this.displayName = displayName;
            this.teamBased = teamBased;
            this.respawn = respawn;
            this.playerVersusPlayer = playerVersusPlayer;
            this.defaultGoal = defaultGoal;
            this.defaultSeconds = defaultSeconds;
            this.maps = List.copyOf(maps);
        }

        int wireId() { return wireId; }
        String key() { return key; }
        String displayName() { return displayName; }
        boolean teamBased() { return teamBased; }
        boolean respawn() { return respawn; }
        boolean playerVersusPlayer() { return playerVersusPlayer; }
        int defaultGoal() { return defaultGoal; }
        int defaultSeconds() { return defaultSeconds; }
        List<Integer> maps() { return maps; }
        boolean usesArenaBot() {
            // Formal rooms contain only actual players or explicitly created GM fixtures.
            return false;
        }

        boolean isBattleScene() { return this != TOWN; }

        static Mode fromWire(int value) {
            for (Mode mode : values()) if (mode.wireId == value) return mode;
            throw new IllegalArgumentException("unknown native LiTo mode " + value);
        }

        static Mode stepNative(int value, Protocol.LitoModeStepDirection direction) {
            if (value < 0 || value >= NATIVE_MODE_COUNT) {
                throw new IllegalArgumentException(
                        "server-gated LiTo mode cannot use ordinary mode stepping");
            }
            // The native room arrows send only direction 1/2.  The original
            // product presentation order is Racing, Death, Honor, Quest,
            // Time, Siege, while the exact wire/category ids are
            // 0,1,5,2,4,3.  The server therefore owns this permutation.
            int[] presentationOrder = {0, 1, 5, 2, 4, 3};
            int position = -1;
            for (int index = 0; index < presentationOrder.length; index++) {
                if (presentationOrder[index] == value) {
                    position = index;
                    break;
                }
            }
            if (position < 0) throw new IllegalArgumentException("unknown native mode " + value);
            int delta = switch (direction) {
                case NEXT -> 1;
                case PREVIOUS -> -1;
            };
            return fromWire(presentationOrder[Math.floorMod(
                    position + delta, presentationOrder.length)]);
        }
    }

    enum Phase { WAITING, COUNTDOWN, RUNNING, FINISHED }

    record RuleSet(Mode mode, int mapId, List<Integer> mapPool, int timeLimitSeconds,
                   int goal, int minimumPlayers, boolean teamBased, boolean respawn,
                   String objective) {
        RuleSet {
            mapPool = List.copyOf(mapPool);
        }
    }

    record Snapshot(Phase phase, RuleSet rules, int secondsRemaining,
                    Map<Long, Integer> playerScores, Map<Long, Integer> playerDeaths,
                    Map<Integer, Integer> teamScores,
                    Set<Long> alivePlayers, int objectivesRemaining, String finishReason) {
        Snapshot {
            playerScores = Map.copyOf(playerScores);
            playerDeaths = Map.copyOf(playerDeaths);
            teamScores = Map.copyOf(teamScores);
            alivePlayers = Set.copyOf(alivePlayers);
        }
    }

    record Resolution(boolean ended, boolean respawn, Set<Long> winners,
                      Set<Long> losers, String reason) {
        Resolution {
            winners = Set.copyOf(winners);
            losers = Set.copyOf(losers);
        }

        static Resolution continuing(boolean respawn, String reason) {
            return new Resolution(false, respawn, Set.of(), Set.of(), reason);
        }
    }

    static RuleSet decode(GameHub.RoomSettings settings) {
        Mode mode = Mode.fromWire(settings.option0() & 0x7F);
        return decode(settings, mode.maps().get(0));
    }

    static RuleSet decode(GameHub.RoomSettings settings,
                          BattleControlCatalog controls) {
        Mode mode = Mode.fromWire(settings.option0() & 0x7F);
        BattleControlCatalog selected = controls == null
                ? BattleControlCatalog.defaults() : controls;
        return decode(settings, selected.firstEnabledMap(mode), selected);
    }

    static RuleSet decode(GameHub.RoomSettings settings, int selectedMapId) {
        return decode(settings, selectedMapId, BattleControlCatalog.defaults());
    }

    static RuleSet decode(GameHub.RoomSettings settings, int selectedMapId,
                          BattleControlCatalog controls) {
        // option0 is the server model's independent S_D5 subtype-2 mode; it is
        // not one of the four editable C_CC/C_DD option bytes.
        Mode mode = Mode.fromWire(settings.option0() & 0x7F);
        if (!mode.maps().contains(selectedMapId)) {
            throw new IllegalArgumentException(
                    "map " + selectedMapId + " does not belong to native mode " + mode.wireId());
        }
        List<Integer> pool = mode.maps();
        // Contemporary LiTo play instructions pin Siege War to a fixed
        // twenty-minute attack/defend window. Native sub_462620 proves the
        // room dialog's option3 is Winner count, never a duration.
        int seconds = mode.defaultSeconds();
        int minimumPlayers = switch (mode) {
            case SIEGE_WAR -> SIEGE_MINIMUM_PLAYERS;
            case PET_RACE, DEATHMATCH, HONOR_MATCH, QUEST_BATTLE -> 2;
            case TOWN -> 0;
            default -> 1;
        };
        String objective = switch (mode) {
            case PET_RACE -> "complete-checkpoints";
            case DEATHMATCH -> "eliminate-opposing-team";
            case HONOR_MATCH -> "destroy-enemy-guardian-tower";
            case QUEST_BATTLE -> BattleQuestEncounter.usesPerRoomSymbols(selectedMapId)
                    ? "clear-each-monster-room+destroy-each-room-symbol"
                    : "clear-four-monster-rooms+destroy-final-guardian-tower";
            case TIME_ATTACK -> "clear-sequential-waves-and-final-boss-before-timeout";
            case SIEGE_WAR -> "capture-or-defend-stronghold";
            case TOWN -> "native-server-gated-town-scene";
        };
        RuleSet fallback = new RuleSet(mode, selectedMapId, pool, seconds,
                mode.defaultGoal(), minimumPlayers,
                mode.teamBased(), mode.respawn(), objective);
        return (controls == null ? BattleControlCatalog.defaults() : controls)
                .apply(fallback);
    }

    /**
     * Returns the exact room-selected arena for either force.
     *
     * <p>The previous implementation treated mode-3 Siege resources
     * 573/574, 563/562 and 547/548 as force-specific Quest lanes.  Exact
     * {@code sub_46F9C0}, {@code sub_46DFB0} and {@code sub_46B7D0} evidence
     * proves mode 3 is Siege War and mode 2 is Quest Battle.  C_D1 subtype 3
     * carries only one map id and no force field, so no unproved companion-map
     * substitution is performed.</p>
     */
    static int arenaMapForForce(RuleSet rules, int force) {
        return rules == null ? 0 : rules.mapId();
    }

    /** Mutable state owned by one room and guarded by GameHub's monitor. */
    static final class MatchState {
        private RuleSet rules;
        private Phase phase = Phase.WAITING;
        private long deadlineNanos;
        private final Set<Long> participants = new LinkedHashSet<>();
        private final Set<Long> alive = new LinkedHashSet<>();
        private final Map<Long, Integer> forces = new LinkedHashMap<>();
        private final Map<Long, Integer> scores = new LinkedHashMap<>();
        private final Map<Long, Integer> deaths = new LinkedHashMap<>();
        private final Map<Integer, Integer> teamScores = new LinkedHashMap<>();
        private final Map<Long, Integer> checkpoints = new LinkedHashMap<>();
        private int objectivesRemaining;
        private int siegeAttackerForce = -1;
        private long timeAttackLeader;
        private String finishReason = "";

        MatchState(RuleSet rules) {
            this.rules = rules;
            objectivesRemaining = rules.goal();
        }

        void reconfigure(RuleSet replacement) {
            if (phase == Phase.RUNNING || phase == Phase.COUNTDOWN) {
                throw new IllegalStateException("cannot reconfigure an active match");
            }
            rules = replacement;
            reset();
        }

        void countdown() {
            if (phase == Phase.WAITING) phase = Phase.COUNTDOWN;
        }

        void cancelCountdown() {
            if (phase == Phase.COUNTDOWN) phase = Phase.WAITING;
        }

        void begin(List<Long> players, Map<Long, Integer> assignedForces, long nowNanos) {
            long defaultLeader = players == null || players.isEmpty() ? 0 : players.get(0);
            begin(players, assignedForces, defaultLeader, nowNanos);
        }

        void begin(List<Long> players, Map<Long, Integer> assignedForces,
                   long selectedLeader, long nowNanos) {
            if (!rules.mode().isBattleScene()) {
                throw new IllegalStateException("native town scene is not a battle rule");
            }
            participants.clear();
            participants.addAll(players);
            alive.clear();
            alive.addAll(players);
            forces.clear();
            forces.putAll(assignedForces);
            scores.clear();
            deaths.clear();
            teamScores.clear();
            checkpoints.clear();
            objectivesRemaining = rules.goal();
            siegeAttackerForce = -1;
            timeAttackLeader = rules.mode() == Mode.TIME_ATTACK
                    && participants.contains(selectedLeader) ? selectedLeader : 0;
            finishReason = "";
            phase = Phase.RUNNING;
            // Contemporary Time Attack instructions define the room-owner
            // guardian-tower destruction as the actual encounter start. Keep
            // its authoritative deadline dormant until that transition;
            // other native modes begin timing with their world bootstrap.
            deadlineNanos = rules.mode() == Mode.TIME_ATTACK ? 0
                    : nowNanos + rules.timeLimitSeconds() * 1_000_000_000L;
        }

        void reset() {
            phase = Phase.WAITING;
            deadlineNanos = 0;
            participants.clear();
            alive.clear();
            forces.clear();
            scores.clear();
            deaths.clear();
            teamScores.clear();
            checkpoints.clear();
            objectivesRemaining = rules.goal();
            siegeAttackerForce = -1;
            timeAttackLeader = 0;
            finishReason = "";
        }

        boolean canAttack(long attacker, long target) {
            if (!rules.mode().playerVersusPlayer()) return false;
            if (!rules.teamBased()) return attacker != target;
            int attackerForce = forces.getOrDefault(attacker, 0);
            int targetForce = forces.getOrDefault(target, 0);
            return attacker != target && attackerForce != targetForce;
        }

        Resolution playerDefeated(long killer, long victim) {
            if (phase != Phase.RUNNING || !participants.contains(victim)) {
                return Resolution.continuing(false, "ignored-death-outside-running-match");
            }
            // A native death can remain pending while the client displays its
            // confirmation UI.  Count only the first authoritative transition
            // from alive to dead, so a repeated fatal packet cannot inflate the
            // S_DB result row before a real respawn restores membership here.
            if (!alive.remove(victim)) {
                return Resolution.continuing(false, "ignored-duplicate-player-death");
            }
            deaths.merge(victim, 1, Integer::sum);
            if (killer != 0 && killer != victim) scores.merge(killer, 1, Integer::sum);
            return switch (rules.mode()) {
                case PET_RACE -> Resolution.continuing(true, "petrace-respawn");
                case DEATHMATCH -> resolveDeathmatchElimination();
                case HONOR_MATCH -> Resolution.continuing(true, "honor-match-respawn");
                // Contemporary LiTo rules explicitly state that a defeated
                // Quest participant is resurrected.  Death therefore cannot
                // replace the final Guardian Tower objective, even when the
                // last currently-alive member is waiting for confirmation.
                case QUEST_BATTLE -> Resolution.continuing(
                        true, "questbattle-respawn");
                case TIME_ATTACK -> victim == timeAttackLeader
                        ? finish(Set.of(), "timeattack-leader-defeated")
                        : alive.isEmpty()
                        ? finish(Set.of(), "timeattack-party-defeated")
                        : Resolution.continuing(false, "timeattack-member-defeated");
                case SIEGE_WAR -> Resolution.continuing(true, "siegewar-respawn");
                case TOWN -> Resolution.continuing(false, "ignored-town-scene-defeat");
            };
        }

        /** Honor Match concludes at its tower; Quest Battle at the final Guardian Tower. */
        Resolution guardianTowerDestroyed(int winningForce) {
            if (phase != Phase.RUNNING
                    || (rules.mode() != Mode.HONOR_MATCH && rules.mode() != Mode.QUEST_BATTLE)) {
                return Resolution.continuing(false, "ignored-guardian-tower-destruction");
            }
            objectivesRemaining = 0;
            String reason = rules.mode() == Mode.QUEST_BATTLE
                    ? "questbattle-final-guardian-tower-destroyed"
                    : "honormatch-guardian-tower-destroyed";
            return finish(playersInForce(winningForce), reason);
        }

        Resolution questFinalObjectiveDestroyed(int winningForce,
                                                 boolean finalSymbol) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.QUEST_BATTLE) {
                return Resolution.continuing(
                        false, "ignored-quest-final-objective-destruction");
            }
            objectivesRemaining = 0;
            return finish(playersInForce(winningForce), finalSymbol
                    ? "questbattle-final-symbol-destroyed"
                    : "questbattle-final-guardian-tower-destroyed");
        }

        /** A recovered PvE wave advances server state but never substitutes for the final objective. */
        Resolution waveCleared(int force) {
            if (phase != Phase.RUNNING
                    || (rules.mode() != Mode.QUEST_BATTLE && rules.mode() != Mode.TIME_ATTACK)) {
                return Resolution.continuing(false, "ignored-wave-clear");
            }
            teamScores.merge(force, 1, Integer::sum);
            return Resolution.continuing(false, rules.mode().key() + "-wave-cleared");
        }

        Resolution timeAttackCompleted() {
            return timeAttackCompleted(false);
        }

        Resolution timeAttackCompleted(boolean dragonDefeated) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.TIME_ATTACK) {
                return Resolution.continuing(false, "ignored-time-attack-completion");
            }
            objectivesRemaining = 0;
            return finish(new LinkedHashSet<>(alive), dragonDefeated
                    ? "timeattack-dragon-defeated"
                    : "timeattack-final-boss-defeated");
        }

        boolean startTimeAttack(long nowNanos) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.TIME_ATTACK
                    || deadlineNanos != 0) return false;
            deadlineNanos = nowNanos
                    + rules.timeLimitSeconds() * 1_000_000_000L;
            return true;
        }

        /**
         * A disconnected Time Attack member records an individual loss while
         * the surviving cooperative party remains in the encounter. This is
         * intentionally distinct from a combat death: the retained Taiwan
         * rules make leader death terminal, while the Hong Kong rules allow
         * remaining members to continue after a disconnect.
         */
        Resolution timeAttackPlayerDisconnected(long player) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.TIME_ATTACK
                    || !participants.contains(player) || !alive.remove(player)) {
                return Resolution.continuing(false,
                        "ignored-timeattack-disconnect-outside-running-match");
            }
            return new Resolution(false, false, Set.of(), Set.of(player),
                    "timeattack-member-disconnected-continues");
        }

        Resolution petRacePlayerDisconnected(long player) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.PET_RACE
                    || !participants.contains(player) || !alive.remove(player)) {
                return Resolution.continuing(false,
                        "ignored-pet-race-disconnect-outside-running-match");
            }
            return new Resolution(false, false, Set.of(), Set.of(player),
                    "pet-race-runner-disconnected-continues");
        }

        boolean transferTimeAttackLeader(long replacement) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.TIME_ATTACK
                    || !alive.contains(replacement)) return false;
            timeAttackLeader = replacement;
            return true;
        }

        long timeAttackLeader() {
            return timeAttackLeader;
        }

        Resolution strongholdCaptured(int winningForce) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.SIEGE_WAR
                    || winningForce != siegeAttackerForce) {
                return Resolution.continuing(false, "ignored-stronghold-capture");
            }
            objectivesRemaining = 0;
            return finish(playersInForce(winningForce), "siegewar-stronghold-captured");
        }

        boolean assignSiegeAttackerForce(int force) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.SIEGE_WAR
                    || (force != 0 && force != 1) || siegeAttackerForce != -1) {
                return false;
            }
            siegeAttackerForce = force;
            return true;
        }

        int siegeAttackerForce() {
            return siegeAttackerForce;
        }

        Resolution checkpoint(long player, int checkpoint) {
            if (phase != Phase.RUNNING || rules.mode() != Mode.PET_RACE || !participants.contains(player)) {
                return Resolution.continuing(false, "ignored-race-checkpoint");
            }
            int current = checkpoints.getOrDefault(player, 0);
            if (checkpoint != current + 1) {
                return Resolution.continuing(false, "ignored-race-checkpoint-order");
            }
            int progress = checkpoint;
            checkpoints.put(player, progress);
            return Resolution.continuing(false, progress >= rules.goal()
                    ? "pet-race-runner-finished" : "pet-race-checkpoint");
        }

        Resolution finishPetRace(long winner) {
            return finishPetRace(Set.of(winner));
        }

        Resolution finishPetRace(Set<Long> rewardWinners) {
            LinkedHashSet<Long> normalized = rewardWinners == null
                    ? new LinkedHashSet<>() : new LinkedHashSet<>(rewardWinners);
            if (phase != Phase.RUNNING || rules.mode() != Mode.PET_RACE
                    || normalized.isEmpty() || !participants.containsAll(normalized)
                    || normalized.stream().anyMatch(player ->
                        checkpoints.getOrDefault(player, 0) < rules.goal())) {
                return Resolution.continuing(false, "ignored-pet-race-finish-window");
            }
            return finish(normalized, "pet-race-finished");
        }

        Resolution timeout(long nowNanos) {
            if (phase != Phase.RUNNING
                    || (rules.mode() == Mode.TIME_ATTACK && deadlineNanos == 0)
                    || nowNanos < deadlineNanos) {
                return Resolution.continuing(false, "match-still-running");
            }
            Set<Long> winners = switch (rules.mode()) {
                // No exact native tie-break rule has been recovered for these
                // modes. Do not manufacture a score winner at timeout.
                case PET_RACE, DEATHMATCH, HONOR_MATCH, QUEST_BATTLE, TIME_ATTACK -> Set.of();
                case SIEGE_WAR -> siegeAttackerForce < 0
                        ? Set.of() : playersInForce(1 - siegeAttackerForce);
                case TOWN -> Set.of();
            };
            return finish(winners, rules.mode().key() + "-timeout");
        }

        Snapshot snapshot(long nowNanos) {
            int remaining = deadlineNanos == 0 ? rules.timeLimitSeconds()
                    : (int) Math.max(0, (deadlineNanos - nowNanos + 999_999_999L) / 1_000_000_000L);
            return new Snapshot(phase, rules, remaining, scores, deaths, teamScores, alive,
                    objectivesRemaining, finishReason);
        }

        int timerSeconds(long nowNanos) {
            if (phase != Phase.RUNNING || rules.timeLimitSeconds() <= 0 || deadlineNanos == 0) return -1;
            return snapshot(nowNanos).secondsRemaining();
        }

        RuleSet rules() { return rules; }
        Phase phase() { return phase; }

        void respawn(long player) {
            if (phase == Phase.RUNNING && participants.contains(player)) alive.add(player);
        }

        private Resolution finish(Set<Long> winners, String reason) {
            LinkedHashSet<Long> normalizedWinners = new LinkedHashSet<>(winners);
            normalizedWinners.retainAll(participants);
            LinkedHashSet<Long> losers = new LinkedHashSet<>(participants);
            losers.removeAll(normalizedWinners);
            phase = Phase.FINISHED;
            finishReason = reason;
            return new Resolution(true, false, normalizedWinners, losers, reason);
        }

        private Set<Long> playersInForce(int force) {
            LinkedHashSet<Long> result = new LinkedHashSet<>();
            for (long player : participants) {
                if (forces.getOrDefault(player, 0) == force) result.add(player);
            }
            return result;
        }

        private Resolution resolveDeathmatchElimination() {
            if (alive.isEmpty()) return finish(Set.of(), "deathmatch-all-players-eliminated");
            LinkedHashSet<Integer> startingForces = new LinkedHashSet<>();
            for (long player : participants) startingForces.add(forces.getOrDefault(player, 0));
            LinkedHashSet<Integer> survivingForces = new LinkedHashSet<>();
            for (long player : alive) survivingForces.add(forces.getOrDefault(player, 0));
            if (startingForces.size() >= 2 && survivingForces.size() == 1) {
                int force = survivingForces.iterator().next();
                return finish(playersInForce(force), "deathmatch-opposing-team-eliminated");
            }
            return Resolution.continuing(false, "deathmatch-player-eliminated");
        }
    }

    private BattleRules() {}
}
