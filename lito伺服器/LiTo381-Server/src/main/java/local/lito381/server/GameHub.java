package local.lito381.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/** Four LiTo.bin Tournament channels, multi-room membership and battle lifecycle. */
final class GameHub implements AutoCloseable {
    static final int MAX_BUDDIES = 0xFF;
    static final long CLONE_PROBE_NATIVE_ROOM_HANDOFF_MILLIS = 1_500;
    static final long BATTLE_DISCONNECT_GRACE_MS = 300;
    /** Historical Honor Match rule: revive at the death position after five seconds. */
    static final long HONOR_RESPAWN_DELAY_MS = 5_000;
    static final String HONOR_RESPAWN_RULE_SOURCE =
            "https://lineagecompendium.com/www.lineagecompendium.com/lito.html";
    static final long CLASSIC_SOURCE_HANDOFF_TTL_NANOS = 30_000_000_000L;
    static final long CHANNEL_PORT_HANDOFF_TTL_NANOS = 30_000_000_000L;
    static final long RENTAL_BATTLE_HANDOFF_TTL_NANOS = 30_000_000_000L;
    static final long RENTAL_BATTLE_HANDOFF_TTL_MILLIS =
            TimeUnit.NANOSECONDS.toMillis(RENTAL_BATTLE_HANDOFF_TTL_NANOS);
    static final int ARENA_BOT_MAX_HP = 100;
    static final long TRADE_INVITE_TIMEOUT_NANOS = 30_000_000_000L;
    static final long PVE_AI_PUBLICATION_GRACE_NANOS = 100_000_000L;
    // Native S_F6/S_F7 records and C_7C callbacks use one shared role domain.
    // C_7C permits assignments 1..3 and explicitly refuses an existing 4.
    static final int CLAN_ROLE_OFFICER = 1;
    static final int CLAN_ROLE_MEMBER = 2;
    static final int CLAN_ROLE_CANDIDATE = 3;
    static final int CLAN_ROLE_OWNER = 4;
    static final long CASTLE_SECURITY_CHARGE_ADENA = 100_000L;

    enum BuddyAddResult { ADDED, DUPLICATE, FULL, INVALID, PERSISTENCE_REJECTED }
    enum WantedWatchMutation { ADDED, EXISTS, REMOVED, NOT_FOUND }

    record WantedWatchEntry(String account, String source) {
        WantedWatchEntry {
            account = account == null ? "" : account;
            source = source == null ? "" : source;
        }
    }

    private record ClassicSourceHandoff(String source, long expiresAtNanos) {}

    record ChannelPortHandoff(String character, String source, int channelId,
                              long expiresAtNanos) {}

    record RentalBattleHandoff(String character, RuntimeInventory inventory,
                               long expiresAtNanos) {
        RentalBattleHandoff {
            if (character == null || character.isBlank() || inventory == null
                    || expiresAtNanos <= 0) {
                throw new IllegalArgumentException("invalid rental battle handoff");
            }
            inventory = inventory.copy();
        }

        @Override public RuntimeInventory inventory() {
            return inventory.copy();
        }
    }

    static final int CHANNEL_CAPACITY = TournamentPresence.CHANNEL_CAPACITY;
    static final List<ChannelTier> CHANNEL_TIERS = TournamentPresence.TIERS.stream()
            .map(tier -> new ChannelTier(tier.id(), tier.name(), (int) tier.minimumLevel(),
                    (int) Math.min(Integer.MAX_VALUE, tier.maximumLevel()))).toList();
    static final int CHANNEL_COUNT = CHANNEL_TIERS.size();
    static final List<String> CHANNEL_NAMES = CHANNEL_TIERS.stream()
            .map(ChannelTier::name).toList();

    /** Exact level bands painted by this target LiTo.bin's four-button UI. */
    record ChannelTier(int id, String name, int minimumLevel, int maximumLevel) {
        boolean containsLevel(int level) {
            return level >= minimumLevel && level <= maximumLevel;
        }
    }

    record RoomSettings(String name, String password, int capacity, int attributes,
                         int option0, int option1, int option2, int option3, int option4,
                         List<Integer> selections) {
        static final int PASSWORD_PRESENT = 0x04;
        static final int ALLOW_OBSERVERS = 0x08;
        static final int ALLOW_UPPER_TIER = 0x10;
        static final int EQUAL_SIDES = 0x20;
        static final int EVENT_CLASS_RESTRICTED = 0x01;
        static final int EVENT_SERVER_RESTRICTED = 0x02;
        static final int EVENT_RESULT_MAIL = 0x04;
        static final int ALL_FIVE_CLASS_MASK = 0x1F;

        RoomSettings {
            password = password == null ? "" : password;
            attributes &= 0xFF;
            attributes = password.isEmpty()
                    ? attributes & ~PASSWORD_PRESENT
                    : attributes | PASSWORD_PRESENT;
            selections = selections == null ? List.of() : List.copyOf(selections);
        }

        RoomSettings(String name, String password, int capacity,
                     int option0, int option1, int option2, int option3, int option4,
                     List<Integer> selections) {
            this(name, password, capacity,
                    password == null || password.isEmpty() ? 0 : PASSWORD_PRESENT,
                    option0, option1, option2, option3, option4, selections);
        }

        RoomSettings(String name, String password, int capacity,
                     int option0, int option1, int option2, int option3, int option4) {
            this(name, password, capacity, option0, option1, option2, option3, option4,
                    List.of());
        }

        /**
         * S_CC carries a display summary rather than the full create/edit
         * attribute byte.  The native receiver maps 0 to the public icon,
         * 0x04 to the private/password icon, and gives 0x02 precedence for
         * the battle-started icon.  S_CE remains the packet that carries the
         * original 0x04/0x08/0x10/0x20 room settings.
         */
        int roomListAttributes(boolean battleStarted) {
            if (battleStarted) return 0x02;
            return (attributes & PASSWORD_PRESENT) == 0 ? 0 : 0x04;
        }

        boolean allowsObservers() {
            return (attributes & ALLOW_OBSERVERS) != 0;
        }

        boolean allowsUpperTier() {
            return (attributes & ALLOW_UPPER_TIER) != 0;
        }

        boolean requiresEqualSides() {
            return (attributes & EQUAL_SIDES) != 0;
        }

        int eventFlags() { return option1; }
        int eventType() { return option2; }
        int winnerCount() { return option3; }
        int allowedClassMask() { return option4; }
        List<Integer> allowedServerIds() { return selections; }

        boolean restrictsClasses() {
            return (eventFlags() & EVENT_CLASS_RESTRICTED) != 0;
        }

        boolean restrictsServers() {
            return (eventFlags() & EVENT_SERVER_RESTRICTED) != 0;
        }

        boolean sendsResultMail() {
            return (eventFlags() & EVENT_RESULT_MAIL) != 0;
        }

        /**
         * The affiliation string carried by S_CB/S_CD is also the ordinary
         * source character's clan name.  It becomes an event-team admission
         * key only for the extended event-room shape; applying it to compact
         * player-created rooms prevents every clanned character from creating
         * or joining a normally named room.
         */
        boolean restrictsEventTeam() {
            return winnerCount() > 0;
        }

        boolean allowsCharacterClass(int characterType) {
            if (!restrictsClasses()) return true;
            // The native event dialog exposes exactly Royal/Knight/Elf/Mage/
            // Dark Elf. Later 3.81 types 5/6 have no selectable bit.
            return characterType >= 0 && characterType < 5
                    && (allowedClassMask() & (1 << characterType)) != 0;
        }

        boolean allowsSourceServer(int serverId) {
            return !restrictsServers() || allowedServerIds().contains(serverId);
        }
    }

    /** One accepted native C_E3 request owned by the server matchmaking queue. */
    private record MatchMakingTicket(ClientSession session, int channelId,
                                     Protocol.MatchMakingRequest request,
                                     GameStateStore.TournamentStatistics statistics,
                                     long sequence) {}

    /**
     * A queue admission either remains waiting or atomically allocates one
     * server-owned automatic room for the returned cohort.
     */
    record MatchMakingQueueResult(long roomId, List<ClientSession> sessions) {
        MatchMakingQueueResult {
            sessions = List.copyOf(sessions);
            if ((roomId == 0) != sessions.isEmpty()) {
                throw new IllegalArgumentException("inconsistent matchmaking queue result");
            }
        }

        static MatchMakingQueueResult waiting() {
            return new MatchMakingQueueResult(0, List.of());
        }

        boolean matched() { return roomId != 0; }
    }
    record RoomSnapshot(long id, int channelId, RoomSettings settings,
                        int users, int combatants, boolean battleStarted) {}
    /** Server-owned test participant created by the database-authorized .de command. */
    record RoomVirtualPlayer(long objectId, String name, String classicServerName,
                             int force, int characterType, int sex) {
        RoomVirtualPlayer {
            if (objectId <= 0 || objectId > 0xFFFF_FFFFL || name == null || name.isBlank()
                    || name.getBytes(Protocol.LEGACY_TEXT).length > 27
                    || classicServerName == null || (force != 0 && force != 1)
                    || characterType < 0 || characterType > 4 || sex < 0 || sex > 1) {
                throw new IllegalArgumentException("invalid room virtual player");
            }
        }
    }
    record VirtualPlayerUpdate(boolean accepted, List<RoomVirtualPlayer> added,
                               List<RoomVirtualPlayer> removed, int capacity,
                               int realCombatants, int virtualCombatants, String reason) {
        VirtualPlayerUpdate {
            added = List.copyOf(added);
            removed = List.copyOf(removed);
            reason = reason == null ? "" : reason;
        }
    }
    enum RoomJoinFailure {
        NONE, BATTLE_STARTED, BATTLE_ENDED, FULL, KICKED,
        TEAM_ROOM_ONLY, CLASS_RESTRICTED, SERVER_RESTRICTED
    }
    record RoomJoinOutcome(boolean joined, List<ClientSession> members,
                           int combatantsBefore, RoomJoinFailure failure) {
        static RoomJoinOutcome rejected(RoomJoinFailure failure) {
            if (failure == RoomJoinFailure.NONE) {
                throw new IllegalArgumentException("rejected join requires a failure reason");
            }
            return new RoomJoinOutcome(false, List.of(), 0, failure);
        }
    }
    record ClassicWorldLocation(int mapId, int x, int y, int heading,
                                int instanceId) {
        ClassicWorldLocation(int mapId, int x, int y, int heading) {
            this(mapId, x, y, heading, -1);
        }

        ClassicWorldLocation {
            heading &= 7;
            boolean validInstance = instanceId == -1
                    || mapId == ClassicWorldCkewCatalog.MAP_ID
                        && instanceId >= ClassicWorldCkewCatalog.FIRST_SHOW_ID
                    || mapId == ClassicWorldWaterDragonCatalog.MAP_ID
                        && instanceId > 0
                    || ClassicWorldHotelCatalog.isHotelMap(mapId)
                        && instanceId > 0;
            if (!validInstance) {
                throw new IllegalArgumentException("invalid classic-world instance id");
            }
            ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
            if (!map.standingPassable(x, y)) {
                throw new IllegalArgumentException("classic-world location is not passable");
            }
        }
    }
    private record BotTick(long roomId, ClientSession target, int x, int y, int damage) {}
    record MonsterAiTick(long roomId, BattleRules.Mode mode, long objectId,
                         int force, int sourceX, int sourceY,
                         int destinationX, int destinationY, int heading,
                          ClientSession target, int damage, boolean attack,
                          BattleTimeAttackMagicSkillCatalog.Definition magicSkill,
                          BattleTimeAttackMagicSkillCatalog.VisualDefinition visualSkill,
                          BattleTimeAttackMagicSkillCatalog.PhysicalDefinition
                                  physicalSkill,
                         BattleTimeAttackMagicSkillCatalog.SummonDefinition
                                 summonSkill,
                         BattleTimeAttackMagicSkillCatalog.GroundDefinition
                                 groundSkill,
                         List<BattleTimeAttackEncounter.Snapshot> spawned,
                         List<BattleQuestEncounter.Snapshot> questSpawned,
                         int magicTriggerRoll,
                         String source) {
        MonsterAiTick(long roomId, BattleRules.Mode mode, long objectId,
                      int force, int sourceX, int sourceY,
                      int destinationX, int destinationY, int heading,
                      ClientSession target, int damage, boolean attack,
                      BattleTimeAttackMagicSkillCatalog.Definition magicSkill,
                      BattleTimeAttackMagicSkillCatalog.PhysicalDefinition physicalSkill,
                      BattleTimeAttackMagicSkillCatalog.SummonDefinition summonSkill,
                      List<BattleTimeAttackEncounter.Snapshot> spawned,
                      int magicTriggerRoll, String source) {
            this(roomId, mode, objectId, force, sourceX, sourceY,
                    destinationX, destinationY, heading, target, damage, attack,
                    magicSkill, null, physicalSkill, summonSkill, null, spawned,
                    List.of(), magicTriggerRoll, source);
        }
    }
    private record CompanionTick(long roomId, ClientSession owner) {}
    private record WorldCompanionTick(int mapId, ClientSession owner) {}
    private record CkewEndWork(ClassicWorldCkewInstances.Ended ended,
                               List<ClientSession> recipients) {}
    private record CkewCountdownWork(
            ClassicWorldCkewInstances.Countdown countdown,
            List<ClientSession> recipients) {}
    private record WaterDragonEventWork(
            ClassicWorldWaterDragonInstances.Event event,
            List<ClientSession> recipients) {}
    private record WaterDragonEndWork(
            ClassicWorldWaterDragonInstances.Ended ended,
            List<ClientSession> recipients) {}
    private record WaterDragonAfflictionWork(
            int showId, ClientSession recipient,
            BattleWaterDragonAffliction.Kind kind) {}
    private record WaterDragonTickWork(
            List<WaterDragonEventWork> events,
            List<WaterDragonEndWork> ended,
            List<WaterDragonAfflictionWork> afflictions) {}
    private record CastleWarWork(String source,
                                 ClassicWorldCastleWarClock.Event event) {}
    enum ClassicCastleSiegeMutation {
        DECLARED, ALREADY_DECLARED, SURRENDERED, CEASED, NOT_ACTIVE,
        NOT_PARTICIPANT, INVALID
    }
    record ClassicCastleWarNotice(Protocol.ClanWarNoticeType type,
                                  String clanOne, String clanTwo,
                                  String reason) {
        ClassicCastleWarNotice {
            if ((type != Protocol.ClanWarNoticeType.DECLARED
                    && type != Protocol.ClanWarNoticeType.SURRENDERED
                    && type != Protocol.ClanWarNoticeType.ENDED
                    && type != Protocol.ClanWarNoticeType.WON)
                    || clanOne == null || clanOne.isBlank()
                    || clanTwo == null || clanTwo.isBlank()) {
                throw new IllegalArgumentException("invalid castle-war notice");
            }
            reason = reason == null ? "" : reason;
        }
    }
    record ClassicCastleSiegeResult(ClassicCastleSiegeMutation mutation,
                                    int castleId, String defenderClan,
                                    Set<String> attackingClans,
                                    List<ClassicCastleWarNotice> notices) {
        ClassicCastleSiegeResult {
            defenderClan = defenderClan == null ? "" : defenderClan;
            attackingClans = attackingClans == null
                    ? Set.of() : Set.copyOf(attackingClans);
            notices = notices == null ? List.of() : List.copyOf(notices);
        }
    }
    record ClassicCastleClaim(
            GameStateStore.CastleOwnershipState ownership,
            ClassicWorldCastleWarField.Transition field,
            ClassicCastleSiegeResult siege) {
        ClassicCastleClaim {
            if (field == null || siege == null) {
                throw new IllegalArgumentException("invalid castle claim");
            }
        }
    }
    private record WaterDragonPassKey(String sourceId, long itemObjectId) {
        WaterDragonPassKey {
            sourceId = key(sourceId);
            if (sourceId.isEmpty() || itemObjectId <= 0) {
                throw new IllegalArgumentException("invalid Water Dragon pass key");
            }
        }
    }
    record WaterDragonPass(String sourceId, long itemObjectId, int showId, int roomNumber,
                           String renterIdentity, long dueNanos) {
        WaterDragonPass {
            sourceId = sourceId == null ? "" : sourceId.trim();
            renterIdentity = key(renterIdentity);
            if (sourceId.isEmpty() || renterIdentity.isEmpty()) {
                throw new IllegalArgumentException("invalid Water Dragon pass");
            }
        }

        boolean validAt(long nowNanos) { return nowNanos < dueNanos; }
    }
    private enum ClassicWorldTargetKind { PLAYER, COMPANION, DSPY_FOLLOWER }
    private record ClassicWorldTarget(ClientSession owner, long objectId,
                                      int x, int y,
                                      ClassicWorldTargetKind kind) {
        ClassicWorldTarget {
            if (owner == null || objectId <= 0 || x <= 0 || y <= 0
                    || kind == null) {
                throw new IllegalArgumentException("invalid classic-world target");
            }
        }

        boolean companion() { return kind == ClassicWorldTargetKind.COMPANION; }
        boolean player() { return kind == ClassicWorldTargetKind.PLAYER; }
        boolean dspyFollower() {
            return kind == ClassicWorldTargetKind.DSPY_FOLLOWER;
        }
    }
    private record ClassicWorldEffectKey(int mapId, int instanceId) {}
    private record GroundTick(long roomId, ClientSession caster,
                              ClientSession target, boolean bot, int skillId,
                              long npcCasterObjectId) {
        GroundTick(long roomId, ClientSession caster, ClientSession target,
                   boolean bot, int skillId) {
            this(roomId, caster, target, bot, skillId, 0);
        }
    }
    private record WorldGroundTick(
            int mapId, int instanceId, List<ClientSession> recipients,
            ClientSession caster, ClientSession playerTarget,
            ClassicWorldMonsterPopulation.Snapshot monsterTarget,
            int skillId) {
        WorldGroundTick {
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }
    }
    private record CubeTick(long roomId, int mapId, int instanceId,
                            List<ClientSession> recipients,
                            ClientSession caster, ClientSession target,
                            BattleCubeIgnitionEffects.Event event) {
        CubeTick {
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }

        boolean world() { return mapId >= 0; }
    }
    private record CubeQuakeTick(long roomId, int mapId, int instanceId,
                                 List<ClientSession> recipients,
                                 ClientSession caster, ClientSession target,
                                 BattleCubeQuakeEffects.Event event) {
        CubeQuakeTick {
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }

        boolean world() { return mapId >= 0; }
    }
    private record CubeShockTick(long roomId, int mapId, int instanceId,
                                 List<ClientSession> recipients,
                                 ClientSession caster, ClientSession target,
                                 BattleCubeShockEffects.Event event) {
        CubeShockTick {
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }

        boolean world() { return mapId >= 0; }
    }
    private record CubeHarmonyTick(long roomId, int mapId, int instanceId,
                                   List<ClientSession> recipients,
                                   ClientSession caster, ClientSession target,
                                   BattleCubeHarmonyEffects.Event event) {
        CubeHarmonyTick {
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }

        boolean world() { return mapId >= 0; }
    }
    enum CookingFireStatus { PLACED, NEARBY, INVALID }
    record CookingFirePlacement(CookingFireStatus status, BattleCookingFires.Fire fire) {}
    private record CookingFireExpiry(long roomId, List<ClientSession> recipients,
                                     BattleCookingFires.Fire fire) {}
    record GroundTake(BattleGroundItems.Item taken, BattleGroundItems.Item remaining) {}
    record ClassicGroundTake(ClassicWorldGroundItems.Item taken,
                             ClassicWorldGroundItems.Item remaining) {}
    private record ClassicMonsterGroundPickup(
            ClassicWorldMonsterPopulation.ItemPickup pickup,
            ClassicWorldGroundItems.Item ground) {}
    record TimeAttackActorHit(BattleTimeAttackEncounter.Hit hit,
                             BattleRules.Resolution resolution) {}
    record QuestMonsterPoisonUpdate(BattleQuestEncounter.PoisonUpdate update,
                                    ClientSession attacker) {}
    record TimeAttackPoisonUpdate(BattleTimeAttackEncounter.PoisonUpdate update,
                                  ClientSession attacker,
                                  BattleRules.Resolution resolution) {}
    record PetRaceMove(BattlePetRaceCourseCatalog.Transition transition,
                       BattlePetRaceCourseCatalog.PanelTransition panel,
                       BattleRules.Resolution resolution) {}
    private record CountdownTick(List<ClientSession> members, int value) {}
    private record BattleStart(long roomId, List<ClientSession> members) {}
    private record BattleEnd(long roomId, List<ClientSession> members,
                             BattleRules.Resolution resolution) {}
    private record BattleDisconnectWork(long roomId, List<ClientSession> recipients,
                                        List<ClientSession> roster,
                                        BattleRules.Resolution resolution,
                                        List<ClientSession> roomDeleteRecipients,
                                        boolean roomDeleted) {}
    enum ReadyTransition { NONE, COUNTDOWN_STARTED, COUNTDOWN_CANCELLED }
    enum NpcGiveResult { ACCEPTED, INVALID_PHASE, UNKNOWN_TARGET, STALE_POSITION, CAPACITY }
    enum TradeAnswerStatus { INVALID, REJECTED, OPENED }
    enum TradeConfirmStatus { INVALID, WAITING, COMPLETED, FAILED }
    enum PrivateShopActionStatus { INVALID, OPENED, CLOSED }

    record TradeAnswerOutcome(TradeAnswerStatus status, ClientSession initiator,
                              ClientSession invitee) {}
    record TradeInventoryChange(ClientSession session, RuntimeInventory before,
                                RuntimeInventory after) {}
    record TradeOfferOutcome(ClientSession owner, ClientSession partner,
                             RuntimeInventory.Item item, long quantity,
                             RuntimeInventory beforeDisplay,
                             RuntimeInventory afterDisplay) {}
    record TradeTerminalOutcome(TradeConfirmStatus status,
                                List<ClientSession> participants,
                                List<TradeInventoryChange> inventoryChanges,
                                boolean exchangeWindowOpen) {
        TradeTerminalOutcome {
            participants = participants == null ? List.of() : List.copyOf(participants);
            inventoryChanges = inventoryChanges == null
                    ? List.of() : List.copyOf(inventoryChanges);
        }
    }

    record PrivateShopActionOutcome(PrivateShopActionStatus status,
                                    List<ClientSession> recipients,
                                    byte[] announcement) {
        PrivateShopActionOutcome {
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
            announcement = announcement == null ? new byte[0] : announcement.clone();
        }

        @Override public byte[] announcement() { return announcement.clone(); }
    }
    record PrivateShopViewOutcome(boolean accepted, int type, long targetObjectId,
                                  List<Protocol.PersonalShopSellRow> sellRows,
                                  List<Protocol.PersonalShopBuyRow> buyRows) {
        PrivateShopViewOutcome {
            sellRows = sellRows == null ? List.of() : List.copyOf(sellRows);
            buyRows = buyRows == null ? List.of() : List.copyOf(buyRows);
        }
    }
    record PrivateShopTransactionOutcome(boolean accepted, ClientSession owner,
                                         RuntimeInventory visitorBefore,
                                         RuntimeInventory visitorAfter,
                                         RuntimeInventory ownerBefore,
                                         RuntimeInventory ownerAfter,
                                         boolean shopClosed,
                                         List<ClientSession> closeRecipients) {
        PrivateShopTransactionOutcome {
            closeRecipients = closeRecipients == null ? List.of() : List.copyOf(closeRecipients);
        }
    }

    record ReadyUpdate(boolean ready, ReadyTransition transition,
                       int minimumPlayersPerForceWarning) {}
    record ForceUpdate(int force, boolean changed, ReadyTransition transition,
                       int firstDisallowedPlayersPerForceWarning) {}
    record ChannelSnapshot(int id, String name, int users, int capacity) {}
    record BuddyGroup(int id, String name) {}
    record BuddySnapshot(String name, int groupId, int relationshipStyle,
                         int paletteOffset, String origin,
                         Protocol.LitoBuddyActivity activity) {
        boolean online() {
            return activity.online();
        }
    }
    record BuddyData(List<BuddyGroup> groups, List<BuddySnapshot> buddies) {}
    record BuddyPersistentEntry(String name, long buddyObjectId, String targetSource,
                                int groupId, int relationshipStyle,
                                int paletteOffset, String origin) {}
    record BuddyPersistentData(List<BuddyGroup> groups,
                               List<BuddyPersistentEntry> buddies) {
        BuddyPersistentData {
            groups = groups == null ? List.of() : List.copyOf(groups);
            buddies = buddies == null ? List.of() : List.copyOf(buddies);
        }
    }
    @FunctionalInterface
    interface BuddyPersistenceHook {
        boolean persist(BuddyPersistentData replacement);
    }
    /** Exact S_F6/S_F7 {@code shccsc} member row in native receiver order. */
    record ClanMemberSnapshot(String name, Protocol.LitoClanRole role,
                              int relationshipStyle, int paletteOffset,
                              String title, Protocol.LitoClanActivity activity) {
        ClanMemberSnapshot {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("native clan member name is blank");
            }
            Objects.requireNonNull(role, "native clan role");
            if (relationshipStyle < 0 || relationshipStyle > 4) {
                throw new IllegalArgumentException(
                        "native clan relationship style is invalid: " + relationshipStyle);
            }
            if (paletteOffset < 0 || paletteOffset > 0xC2) {
                throw new IllegalArgumentException(
                        "native clan palette offset is invalid: " + paletteOffset);
            }
            title = title == null ? "" : title;
            Objects.requireNonNull(activity, "native clan activity");
        }
    }
    record ClanData(String name, List<ClanMemberSnapshot> members, boolean destroyPending) {
        ClanData {
            members = members == null ? List.of() : List.copyOf(members);
        }

        static ClanData empty() {
            return new ClanData("", List.of(), false);
        }
    }
    enum ClassicClanCreateStatus { CREATED, OWNER_ALREADY_MEMBER, DUPLICATE_NAME, INVALID }
    enum ClassicClanJoinStatus { JOINED, FULL, INVALID }
    record ClassicClanJoinResult(ClassicClanJoinStatus status, ClanData clan) {
        ClassicClanJoinResult {
            clan = clan == null ? ClanData.empty() : clan;
        }
    }
    enum ClassicClanWarRequestStatus {
        PENDING, INVALID, NOT_OWNER, TARGET_NOT_FOUND, TARGET_OWNER_OFFLINE,
        REQUESTER_AT_WAR, TARGET_AT_WAR, NOT_AT_WAR, NOT_SAME_WAR, UNSUPPORTED
    }
    enum ClassicClanWarResolutionStatus {
        STARTED, SURRENDERED, CEASED, REFUSED, INVALID
    }
    record ClassicClanWarData(String clanOne, String clanTwo) {
        static ClassicClanWarData empty() {
            return new ClassicClanWarData("", "");
        }

        boolean active() {
            return !clanOne.isBlank() && !clanTwo.isBlank();
        }
    }
    record ClassicClanWarProposal(ClassicClanWarRequestStatus status,
                                  ClientSession targetOwner,
                                  String requesterClan, String targetClan) {
        ClassicClanWarProposal {
            requesterClan = requesterClan == null ? "" : requesterClan;
            targetClan = targetClan == null ? "" : targetClan;
        }
    }
    record ClassicClanWarResolution(ClassicClanWarResolutionStatus status,
                                    ClassicClanWarData war) {
        ClassicClanWarResolution {
            war = war == null ? ClassicClanWarData.empty() : war;
        }
    }
    record ClassicClanWarLeaderDeathResolution(
            boolean ended, boolean attackerSameWar,
            ClassicClanWarData war, List<ClientSession> recipients) {
        ClassicClanWarLeaderDeathResolution {
            war = war == null ? ClassicClanWarData.empty() : war;
            recipients = recipients == null ? List.of() : List.copyOf(recipients);
        }

        static ClassicClanWarLeaderDeathResolution none() {
            return new ClassicClanWarLeaderDeathResolution(
                    false, false, ClassicClanWarData.empty(), List.of());
        }
    }
    enum ClassicPartyJoinStatus { JOINED, ALREADY_MEMBER, NOT_LEADER, FULL, INVALID }
    enum ClassicPartyRemoveStatus {
        LEFT,
        LEFT_AND_DISBANDED,
        LEADER_DISBANDED,
        KICKED,
        KICKED_AND_DISBANDED,
        NOT_MEMBER,
        NOT_LEADER,
        TARGET_NOT_FOUND
    }
    record ClassicPartyData(String leader, List<String> members,
                            String leaderIdentity, List<String> memberIdentities) {
        ClassicPartyData {
            leader = leader == null ? "" : leader;
            members = members == null ? List.of() : List.copyOf(members);
            leaderIdentity = leaderIdentity == null ? "" : leaderIdentity;
            memberIdentities = memberIdentities == null
                    ? List.of() : List.copyOf(memberIdentities);
        }

        ClassicPartyData(String leader, List<String> members) {
            this(leader, members, leader, members);
        }

        static ClassicPartyData empty() {
            return new ClassicPartyData("", List.of(), "", List.of());
        }

        boolean inParty() {
            return !leader.isBlank() && members.size() >= 2;
        }
    }
    record ClassicPartyJoinResult(ClassicPartyJoinStatus status, ClassicPartyData party) {
        ClassicPartyJoinResult {
            party = party == null ? ClassicPartyData.empty() : party;
        }
    }
    record ClassicPartyRemoveResult(ClassicPartyRemoveStatus status,
                                    ClassicPartyData before, String removedName,
                                    String removedIdentity) {
        ClassicPartyRemoveResult {
            before = before == null ? ClassicPartyData.empty() : before;
            removedName = removedName == null ? "" : removedName;
            removedIdentity = removedIdentity == null ? "" : removedIdentity;
        }
    }

    private final Map<Integer, Channel> channels = new LinkedHashMap<>();
    private final TournamentPresence tournamentPresence;
    private final Map<ClientSession, TournamentPresence.Owner> nativePresenceOwners = new LinkedHashMap<>();
    private final Map<TournamentPresence.Owner, ClientSession> nativePresencePeers = new LinkedHashMap<>();
    private final Set<ClientSession> connectedSessions = new LinkedHashSet<>();
    private final Map<String, ClientSession> authenticatedGameAccounts = new LinkedHashMap<>();
    /**
     * One-shot LOGIN -> GAME source identity selected by native C_C8.
     *
     * <p>The original selector sends the selected S_SERVER_LIST record name on
     * the LOGIN socket, while S_F4 reconnects the same account to GAME.  The
     * source therefore cannot live in either ClientSession alone.</p>
     */
    private final Map<String, ClassicSourceHandoff> classicSourceHandoffs =
            new LinkedHashMap<>();
    /** One-shot GAME -> dedicated channel-port continuation keyed by account. */
    private final Map<String, ChannelPortHandoff> channelPortHandoffs =
            new LinkedHashMap<>();
    /** One-shot rental-town -> Tournament equipment capsule, keyed by account. */
    private final Map<String, RentalBattleHandoff> rentalBattleHandoffs =
            new LinkedHashMap<>();
    /** Native social-recipient projection only; all reservation decisions live in TournamentPresence. */
    private final Map<ClientSession, String> selectedCharacters = new LinkedHashMap<>();
    private final Map<ClientSession, ClassicWorldLocation> classicWorldMembership =
            new LinkedHashMap<>();
    private final Map<Long, Integer> classicWorldNpcHeadings = new LinkedHashMap<>();
    /** Retained 1880ms autonomous attack clocks for immutable NPC60520 towers. */
    private final Map<Long, Long> classicWorldMonsterGuardReadyAtNanos =
            new LinkedHashMap<>();
    /** Runtime-only GFX mutations for retained static NPC objects. */
    private final Map<Long, Integer> classicWorldNpcGfxOverrides = new LinkedHashMap<>();
    /** Per-owner and abandoned runtime actors for retained quest EWLv40_1. */
    private final ClassicWorldRoiFollowers classicWorldRoiFollowers =
            new ClassicWorldRoiFollowers();
    /** Per-owner and abandoned map-4 Searcher/Endia quest followers. */
    private final ClassicWorldRescueFollowers classicWorldRescueFollowers =
            new ClassicWorldRescueFollowers();
    /** Per-owner, abandoned, and dying ElfLv50_1 NPC80012 followers. */
    private final ClassicWorldDspyEscorts classicWorldDspyEscorts =
            new ClassicWorldDspyEscorts();
    /** Retained WorldQuest showId registry for native CKEW quest122. */
    private final ClassicWorldCkewInstances classicWorldCkewInstances =
            new ClassicWorldCkewInstances();
    private final ClassicWorldWaterDragonInstances classicWorldWaterDragonInstances =
            new ClassicWorldWaterDragonInstances();
    private final ClassicWorldMonsterPopulation classicWorldMonsters;
    /** Runtime bags for non-promoted retained C_GIVE NPC recipients. */
    private final ClassicWorldStaticNpcInventories classicWorldStaticNpcInventories =
            new ClassicWorldStaticNpcInventories();
    /** Selected WorldTrap rows whose maps are already admitted by classic WORLD. */
    private final ClassicWorldTrapPopulation classicWorldTraps =
            new ClassicWorldTrapPopulation();
    /** Skills58/63 coordinate leases, isolated by ordinary-WORLD map instance. */
    private final Map<ClassicWorldEffectKey, ClassicWorldGroundEffects>
            classicWorldGroundEffects = new LinkedHashMap<>();
    /** Short-lived NPC 81162 effects are isolated by classic map instance. */
    private final Map<ClassicWorldEffectKey, BattleShockStunEffects>
            classicWorldShockStunEffects = new LinkedHashMap<>();
    /** Skill208 NPC86123/GFX6552 effects isolated by classic map instance. */
    private final Map<ClassicWorldEffectKey, BattleBoneBreakEffects>
            classicWorldBoneBreakEffects = new LinkedHashMap<>();
    /** Short-lived NPC 81168/GFX176 freeze effects isolated by map instance. */
    private final Map<ClassicWorldEffectKey, BattleFreezeEffects>
            classicWorldFreezeEffects = new LinkedHashMap<>();
    /** NPC80149/GFX6706 and its target leases are isolated by map instance. */
    private final Map<ClassicWorldEffectKey, BattleCubeIgnitionEffects>
            classicWorldCubeIgnitionEffects = new LinkedHashMap<>();
    /** NPC80150/GFX6712 and Quake target/bind leases isolated by map instance. */
    private final Map<ClassicWorldEffectKey, BattleCubeQuakeEffects>
            classicWorldCubeQuakeEffects = new LinkedHashMap<>();
    /** NPC80151/GFX6718 and Shock target/MR leases isolated by map instance. */
    private final Map<ClassicWorldEffectKey, BattleCubeShockEffects>
            classicWorldCubeShockEffects = new LinkedHashMap<>();
    /** NPC80152/GFX6724 and Harmony target leases isolated by map instance. */
    private final Map<ClassicWorldEffectKey, BattleCubeHarmonyEffects>
            classicWorldCubeHarmonyEffects = new LinkedHashMap<>();
    /** Ordinary skill211 leases isolated by classic map instance. */
    private final Map<ClassicWorldEffectKey, BattlePatienceStatus>
            classicWorldPatienceStatuses = new LinkedHashMap<>();
    /** One isolated one-shot quest122 population per retained WorldQuest showId. */
    private final Map<Integer, ClassicWorldMonsterPopulation>
            classicWorldCkewMonsters = new LinkedHashMap<>();
    private long nextClassicWorldCkewMonsterObjectBase = 0x6600_0000L;
    /** One isolated quest117 population per retained Dragon Gate pass showId. */
    private final Map<Integer, ClassicWorldMonsterPopulation>
            classicWorldWaterDragonMonsters = new LinkedHashMap<>();
    private long nextClassicWorldWaterDragonMonsterObjectBase = 0x6700_0000L;
    private final Map<Integer, Long> classicWorldWaterDragonAfflictionAfterNanos =
            new LinkedHashMap<>();
    private final Map<WaterDragonPassKey, WaterDragonPass>
            classicWorldWaterDragonPasses =
            new LinkedHashMap<>();
    private int nextClassicWorldWaterDragonShowId = 0x6900_0000;
    private final ClassicWorldCastleDoorState giranCastleDoors =
            new ClassicWorldCastleDoorState();
    private final ClassicWorldWindawoodCastleDoorState windawoodCastleDoors =
            new ClassicWorldWindawoodCastleDoorState();
    private final ClassicWorldKentCastleDoorState kentCastleDoors =
            new ClassicWorldKentCastleDoorState();
    private final ClassicWorldDwarfCastleDoorState dwarfCastleDoors =
            new ClassicWorldDwarfCastleDoorState();
    private final ClassicWorldAdenCastleDoorState adenCastleDoors =
            new ClassicWorldAdenCastleDoorState();
    private final ClassicWorldHeineCastleDoorState heineCastleDoors =
            new ClassicWorldHeineCastleDoorState();
    /** One exact selected-source ServerWarExecutor timeline per character source. */
    private final Map<String, ClassicWorldCastleWarClock> classicCastleWarClocks =
            new LinkedHashMap<>();
    /** Source-qualified L1WarSpawn flags/trebuchets owned by the classic world. */
    private final ClassicWorldCastleWarField classicCastleWarField =
            new ClassicWorldCastleWarField();
    private final ClassicWorldGroundItems classicWorldGroundItems;
    /** LiTo-only free-hotel credentials and map/lease-scoped key drops. */
    private final RentalTownHotelRegistry rentalTownHotels =
            new RentalTownHotelRegistry();
    private final Map<Long, Room> rooms = new LinkedHashMap<>();
    private final Map<String, BuddyProfile> buddyProfiles = new LinkedHashMap<>();
    private final Map<String, BuddyPersistenceHook> buddyPersistenceHooks =
            new LinkedHashMap<>();
    private final Map<String, ClanProfile> clanProfiles = new LinkedHashMap<>();
    private final Map<String, String> clanMembership = new LinkedHashMap<>();
    private final Map<String, ClassicClanWar> classicClanWars = new LinkedHashMap<>();
    private final Map<String, ClassicCastleSiege> classicCastleSieges =
            new LinkedHashMap<>();
    private final Map<String, ClanWarehouseLease> clanWarehouseLeases = new LinkedHashMap<>();
    private final Map<Long, ClanEmblem> classicClanEmblems = new LinkedHashMap<>();
    private final Map<String, ClassicParty> classicPartyMembership = new LinkedHashMap<>();
    private final Map<ClientSession, PlayerTrade> playerTrades = new LinkedHashMap<>();
    private final Map<ClientSession, PrivateShop> privateShops = new LinkedHashMap<>();
    private final Map<ClientSession, PrivateShopView> privateShopViews = new LinkedHashMap<>();
    private final Map<ClientSession, MatchMakingTicket> matchMakingQueue =
            new LinkedHashMap<>();
    private long nextMatchMakingSequence = 1;
    private final Map<String, Long> chatSilencedUntilNanos = new LinkedHashMap<>();
    private final Map<String, WantedWatchEntry> wantedAccountWatches = new LinkedHashMap<>();

    synchronized void connect(ClientSession session) {
        if (session != null && connectedSessions.add(session)) {
            TournamentPresence.Owner owner = tournamentPresence.connect();
            nativePresenceOwners.put(session, owner);
            nativePresencePeers.put(owner, session);
        }
    }

    synchronized List<ClientSession> connectedSessionsSnapshot() {
        return new ArrayList<>(connectedSessions);
    }

    /**
     * Serializes native account ownership at the gameplay endpoint.
     *
     * <p>A LOGIN session is only a directory/handoff connection and therefore
     * does not claim the account.  Both endpoints must still reject while an
     * authenticated GAME session owns it; this produces native
     * S_LOGIN_CHECK result 0x16 without breaking the normal LOGIN -> GAME
     * handoff overlap.</p>
     */
    synchronized boolean acceptAuthenticatedAccount(ClientSession session,
                                                    ClientSession.Endpoint endpoint,
                                                    String account) {
        String accountKey = key(account);
        if (session == null || !connectedSessions.contains(session) || accountKey.isEmpty()) {
            return false;
        }
        ClientSession active = authenticatedGameAccounts.get(accountKey);
        if (active != null && active != session && connectedSessions.contains(active)) {
            if (!active.authorizesAuthenticatedEndpointTransfer(account)) {
                return false;
            }
            authenticatedGameAccounts.remove(accountKey);
            clearNativeSelection(active);
            active.completeAuthenticatedEndpointTransfer();
        }
        if (active != null && !connectedSessions.contains(active)) {
            authenticatedGameAccounts.remove(accountKey);
        }
        if (endpoint == ClientSession.Endpoint.GAME) {
            authenticatedGameAccounts.put(accountKey, session);
        }
        return true;
    }

    synchronized void clearClassicSourceHandoff(String account) {
        String accountKey = key(account);
        if (!accountKey.isEmpty()) classicSourceHandoffs.remove(accountKey);
    }

    synchronized void stageClassicSourceHandoff(String account, String source) {
        String accountKey = key(account);
        if (accountKey.isEmpty() || source == null || source.isBlank()) {
            throw new IllegalArgumentException("invalid classic-source handoff");
        }
        classicSourceHandoffs.put(accountKey, new ClassicSourceHandoff(
                source, System.nanoTime() + CLASSIC_SOURCE_HANDOFF_TTL_NANOS));
    }

    synchronized String pendingClassicSourceHandoff(String account) {
        String accountKey = key(account);
        ClassicSourceHandoff handoff = classicSourceHandoffs.get(accountKey);
        if (handoff == null) return null;
        if (System.nanoTime() - handoff.expiresAtNanos() >= 0) {
            classicSourceHandoffs.remove(accountKey);
            return null;
        }
        return handoff.source();
    }

    synchronized boolean consumeClassicSourceHandoff(String account, String source) {
        String accountKey = key(account);
        ClassicSourceHandoff handoff = classicSourceHandoffs.get(accountKey);
        if (handoff == null || source == null
                || !handoff.source().equalsIgnoreCase(source)
                || System.nanoTime() - handoff.expiresAtNanos() >= 0) {
            if (handoff != null && System.nanoTime() - handoff.expiresAtNanos() >= 0) {
                classicSourceHandoffs.remove(accountKey);
            }
            return false;
        }
        classicSourceHandoffs.remove(accountKey);
        return true;
    }

    synchronized boolean stageChannelPortHandoff(
            String account, String character, String source, int channelId) {
        String accountKey = key(account);
        String characterKey = key(character);
        if (accountKey.isEmpty() || characterKey.isEmpty()
                || source == null || source.isBlank()
                || channelId < 0 || channelId >= CHANNEL_COUNT) return false;
        channelPortHandoffs.put(accountKey, new ChannelPortHandoff(
                character, source, channelId,
                System.nanoTime() + CHANNEL_PORT_HANDOFF_TTL_NANOS));
        return true;
    }

    synchronized ChannelPortHandoff pendingChannelPortHandoff(
            String account, int boundChannelId) {
        String accountKey = key(account);
        ChannelPortHandoff handoff = channelPortHandoffs.get(accountKey);
        if (handoff == null) return null;
        if (System.nanoTime() - handoff.expiresAtNanos() >= 0) {
            channelPortHandoffs.remove(accountKey);
            return null;
        }
        // A negative binding is the shared GAME listener.  It legitimately
        // serves all four channel buttons, so the authenticated account's
        // one-shot handoff determines the target there.  Dedicated listeners
        // still require an exact channel/port match.
        return boundChannelId < 0 || handoff.channelId() == boundChannelId
                ? handoff : null;
    }

    synchronized boolean consumeChannelPortHandoff(
            String account, String character, String source, int channelId) {
        String accountKey = key(account);
        ChannelPortHandoff handoff = channelPortHandoffs.get(accountKey);
        if (handoff == null
                || System.nanoTime() - handoff.expiresAtNanos() >= 0
                || !key(handoff.character()).equals(key(character))
                || source == null || !handoff.source().equalsIgnoreCase(source)
                || handoff.channelId() != channelId) {
            if (handoff != null && System.nanoTime() - handoff.expiresAtNanos() >= 0) {
                channelPortHandoffs.remove(accountKey);
            }
            return false;
        }
        channelPortHandoffs.remove(accountKey);
        return true;
    }

    synchronized boolean stageRentalBattleHandoff(
            String account, String character, RuntimeInventory inventory) {
        String accountKey = key(account);
        String characterKey = key(character);
        if (accountKey.isEmpty() || characterKey.isEmpty()
                || inventory == null || inventory.size() == 0) return false;
        if (rentalBattleHandoffStore.enabled()) {
            try {
                return rentalBattleHandoffStore.stage(
                        accountKey, characterKey, inventory,
                        RENTAL_BATTLE_HANDOFF_TTL_MILLIS);
            } catch (IOException | IllegalArgumentException error) {
                System.out.println("[LiTo] shared rental handoff stage rejected account="
                        + accountKey + " file=" + rentalBattleHandoffStore.file()
                        + " reason=" + error.getClass().getSimpleName()
                        + ": " + error.getMessage());
                return false;
            }
        }
        rentalBattleHandoffs.put(accountKey, new RentalBattleHandoff(
                characterKey, inventory,
                System.nanoTime() + RENTAL_BATTLE_HANDOFF_TTL_NANOS));
        return true;
    }

    synchronized boolean hasRentalBattleHandoff(String account, String character) {
        String accountKey = key(account);
        if (rentalBattleHandoffStore.enabled()) {
            try {
                return rentalBattleHandoffStore.has(accountKey, key(character));
            } catch (IOException error) {
                System.out.println("[LiTo] shared rental handoff lookup rejected account="
                        + accountKey + " file=" + rentalBattleHandoffStore.file()
                        + " reason=" + error.getClass().getSimpleName()
                        + ": " + error.getMessage());
                return false;
            }
        }
        RentalBattleHandoff handoff = rentalBattleHandoffs.get(accountKey);
        if (handoff == null) return false;
        if (System.nanoTime() - handoff.expiresAtNanos() >= 0) {
            rentalBattleHandoffs.remove(accountKey);
            return false;
        }
        return handoff.character().equals(key(character));
    }

    synchronized RentalBattleHandoff consumeRentalBattleHandoff(
            String account, String character) {
        String accountKey = key(account);
        if (rentalBattleHandoffStore.enabled()) {
            try {
                RentalBattleHandoffStore.Stored stored =
                        rentalBattleHandoffStore.consume(accountKey, key(character));
                if (stored == null) return null;
                long remainingMillis = Math.max(1L,
                        stored.expiresAtEpochMillis() - System.currentTimeMillis());
                return new RentalBattleHandoff(
                        stored.character(), stored.inventory(),
                        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(
                                Math.min(remainingMillis,
                                        RENTAL_BATTLE_HANDOFF_TTL_MILLIS)));
            } catch (IOException | IllegalArgumentException error) {
                System.out.println("[LiTo] shared rental handoff consume rejected account="
                        + accountKey + " file=" + rentalBattleHandoffStore.file()
                        + " reason=" + error.getClass().getSimpleName()
                        + ": " + error.getMessage());
                return null;
            }
        }
        RentalBattleHandoff handoff = rentalBattleHandoffs.get(accountKey);
        if (handoff == null) return null;
        boolean expired = System.nanoTime() - handoff.expiresAtNanos() >= 0;
        if (expired) rentalBattleHandoffs.remove(accountKey);
        if (expired || !handoff.character().equals(key(character))) return null;
        rentalBattleHandoffs.remove(accountKey);
        return handoff;
    }

    /**
     * Releases only the identity locks needed for an explicit native
     * GAME-to-GAME endpoint transfer. Ordinary duplicate logins remain
     * rejected; normal disconnect cleanup still owns room membership.
     */
    synchronized boolean releaseAuthenticatedAccountForTransfer(
            ClientSession session, String account) {
        String accountKey = key(account);
        if (session == null || accountKey.isEmpty()
                || authenticatedGameAccounts.get(accountKey) != session) {
            return false;
        }
        authenticatedGameAccounts.remove(accountKey);
        clearNativeSelection(session);
        return true;
    }

    synchronized WantedWatchMutation addWantedAccount(String account) {
        String accountKey = key(account);
        if (accountKey.isEmpty()) throw new IllegalArgumentException("wanted account is empty");
        if (wantedAccountWatches.containsKey(accountKey)) return WantedWatchMutation.EXISTS;
        wantedAccountWatches.put(accountKey, new WantedWatchEntry(account, ""));
        return WantedWatchMutation.ADDED;
    }

    synchronized WantedWatchMutation removeWantedAccount(String account) {
        String accountKey = key(account);
        if (accountKey.isEmpty()) throw new IllegalArgumentException("wanted account is empty");
        return wantedAccountWatches.remove(accountKey) == null
                ? WantedWatchMutation.NOT_FOUND : WantedWatchMutation.REMOVED;
    }

    synchronized boolean recordWantedLogin(String account, String source) {
        String accountKey = key(account);
        if (accountKey.isEmpty() || source == null || source.isBlank()) {
            throw new IllegalArgumentException("invalid wanted login record");
        }
        if (!wantedAccountWatches.containsKey(accountKey)) return false;
        wantedAccountWatches.put(accountKey, new WantedWatchEntry(account, source));
        return true;
    }

    synchronized List<WantedWatchEntry> wantedAccountSnapshot() {
        return List.copyOf(wantedAccountWatches.values());
    }

    synchronized boolean selectCharacter(ClientSession session, String characterName) {
        String characterKey = key(characterName);
        if (session == null || !connectedSessions.contains(session)
                || characterKey.isEmpty()) {
            return false;
        }
        if (!tournamentPresence.reserveLegacyAlias(nativePresenceOwners.get(session), characterName)) return false;
        selectedCharacters.put(session, characterKey);
        return true;
    }

    /** Live native admission uses the matched owned DB row, never playerObjectId()/first-row fallbacks. */
    synchronized boolean selectCharacter(ClientSession session, int sourceId, String sourceName,
                                         CharacterSummary character) {
        if (session == null || character == null || !connectedSessions.contains(session)) return false;
        TournamentPresence.Selection selection = new TournamentPresence.Selection(
                new TournamentPresence.CharacterKey(sourceId, character.objectId()), sourceName,
                character.name(), character.level(), character.type(), character.sex());
        if (tournamentPresence.select(nativePresenceOwners.get(session), selection)
                != TournamentPresence.SelectStatus.ACCEPTED) return false;
        selectedCharacters.put(session, key(selection.legacyAlias()));
        return true;
    }

    private void clearNativeSelection(ClientSession session) {
        tournamentPresence.clearSelection(nativePresenceOwners.get(session));
        selectedCharacters.remove(session);
    }

    synchronized boolean beginCharacterDeletion(String characterName,
                                                ClientSession requester) {
        return tournamentPresence.beginDeletion(characterName, nativePresenceOwners.get(requester));
    }

    synchronized void endCharacterDeletion(String characterName) {
        tournamentPresence.endDeletion(characterName);
    }
    private final ScheduledExecutorService battleClock;
    private final boolean aiEnabled;
    private final boolean petRaceMinimumPlayersEnforced;
    private final boolean siegeRosterEnforced;
    private final boolean classicWorldMonsterAiEnabled;
    private final boolean classicWorldMonsterAggressiveAiEnabled;
    private final int classicWorldMonsterPhysicalSkillFixtureBranch;
    private final int classicWorldMonsterPhysicalSkillFixtureTriggerRoll;
    private final int classicWorldMonsterMagicFixtureBranch;
    private final int classicWorldMonsterMagicFixtureTriggerRoll;
    private final int timeAttackDocumentedPatternFixtureIndex;
    private final int timeAttackDragonOutcomeFixtureIndex;
    private final boolean timeAttackDragonCombatFixtureOnly;
    private final int timeAttackDragonCombatFixtureMinimumTargets;
    private final int timeAttackMonsterActNoFixture;
    private final int timeAttackMonsterMagicFixtureTriggerRoll;
    private final int timeAttackMonsterPhysicalFixtureTriggerRoll;
    private final int timeAttackMonsterCandidateFixtureSelector;
    private final int aiDamage;
    private static final long MONSTER_RECOVERY_LOG_INTERVAL_NANOS =
            TimeUnit.SECONDS.toNanos(30);
    private long classicWorldMonsterLastRecoveryLogNanos;
    private long classicWorldMonsterSuppressedRecoveryCount;
    /** Optional durable sidecar used by the production server lifecycle. */
    private final GameStateStore state;
    /** Optional routed SQL authority used by the production server lifecycle. */
    private final AccountRepository accounts;
    /** Startup-validated common static-database battle controls. */
    private final BattleControlCatalog battleControls;
    private final boolean ephemeralSourceProfiles;
    private final List<String> classicSourceNames;
    /** Optional cross-process S_F4 rental equipment handoff journal. */
    private final RentalBattleHandoffStore rentalBattleHandoffStore;
    private long nextRoomId = 1;
    // 0x50..0x7F are already partitioned among items, effects, NPCs and companions.
    private long nextVirtualPlayerObjectId = 0x4000_0000L;
    private long nextPrivateShopGeneration = 1;

    GameHub(ServerConfig config) {
        this(config, null, null);
    }

    GameHub(ServerConfig config, GameStateStore state) {
        this(config, state, null);
    }

    GameHub(ServerConfig config, GameStateStore state, AccountRepository accounts) {
        this(config, state, accounts, new TournamentPresence());
    }

    GameHub(ServerConfig config, GameStateStore state, AccountRepository accounts,
            TournamentPresence tournamentPresence) {
        this.tournamentPresence = java.util.Objects.requireNonNull(tournamentPresence);
        for (int index = 0; index < config.classicServerIds().size(); index++) {
            tournamentPresence.registerSource(config.classicServerIds().get(index), config.classicServerNames().get(index));
        }
        this.state = state;
        this.accounts = accounts;
        battleControls = BattleControlCatalog.load(accounts);
        NativeRoomRules.install(this, config, accounts);
        this.ephemeralSourceProfiles = config.ephemeralSourceProfiles();
        this.classicSourceNames = List.copyOf(config.classicServerNames());
        rentalBattleHandoffStore =
                new RentalBattleHandoffStore(config.rentalBattleHandoffFile());
        aiEnabled = config.battleAiEnabled();
        petRaceMinimumPlayersEnforced = config.nativePetRaceMinimumPlayersEnforced();
        siegeRosterEnforced = config.nativeSiegeRosterEnforced();
        classicWorldMonsterAiEnabled = config.classicWorldMonsterAiEnabled();
        classicWorldMonsterAggressiveAiEnabled =
                config.classicWorldMonsterAggressiveAiEnabled();
        classicWorldMonsterPhysicalSkillFixtureBranch =
                config.classicWorldMonsterPhysicalSkillFixtureBranch();
        classicWorldMonsterPhysicalSkillFixtureTriggerRoll =
                config.classicWorldMonsterPhysicalSkillFixtureTriggerRoll();
        classicWorldMonsterMagicFixtureBranch =
                config.classicWorldMonsterMagicFixtureBranch();
        classicWorldMonsterMagicFixtureTriggerRoll =
                config.classicWorldMonsterMagicFixtureTriggerRoll();
        timeAttackDocumentedPatternFixtureIndex =
                config.timeAttackDocumentedPatternFixtureIndex();
        timeAttackDragonOutcomeFixtureIndex =
                config.timeAttackDragonOutcomeFixtureIndex();
        timeAttackDragonCombatFixtureOnly =
                config.timeAttackDragonCombatFixtureOnly();
        timeAttackDragonCombatFixtureMinimumTargets =
                config.timeAttackDragonCombatFixtureMinimumTargets();
        timeAttackMonsterActNoFixture =
                config.timeAttackMonsterActNoFixture();
        timeAttackMonsterMagicFixtureTriggerRoll =
                config.timeAttackMonsterMagicFixtureTriggerRoll();
        timeAttackMonsterPhysicalFixtureTriggerRoll =
                config.timeAttackMonsterPhysicalFixtureTriggerRoll();
        timeAttackMonsterCandidateFixtureSelector =
                config.timeAttackMonsterCandidateFixtureSelector();
        aiDamage = config.battleAiDamage();
        classicWorldMonsters = new ClassicWorldMonsterPopulation();
        classicWorldGroundItems = new ClassicWorldGroundItems();
        for (ChannelTier tier : CHANNEL_TIERS) {
            channels.put(tier.id(), new Channel(tier.id(), tier.name(), CHANNEL_CAPACITY));
        }
        // Preserve the historical channel-0 default room id used by existing
        // clients and packet fixtures. Other channels create their default
        // room lazily when the first player enters them.
        ensureDefaultRoom(requiredChannel(0));
        battleClock = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "lito381-battle-clock");
            thread.setDaemon(true);
            return thread;
        });
        battleClock.scheduleWithFixedDelay(this::countdownTickSafely,
                250, 250, TimeUnit.MILLISECONDS);
        battleClock.scheduleWithFixedDelay(this::groundTickSafely,
                100, 100, TimeUnit.MILLISECONDS);
        battleClock.scheduleWithFixedDelay(this::statusTickSafely,
                100, 100, TimeUnit.MILLISECONDS);
        // Retained ServerItemUserTimer cadence: absolute inventory deadlines
        // are scanned independently every minute.
        battleClock.scheduleWithFixedDelay(this::itemUserTickSafely,
                60, 60, TimeUnit.SECONDS);
        // Retained ServerAuctionTimer semantics, applied independently to
        // every selected 380_ac_N authority. Exact snapshots make retries
        // idempotent when another process settles the same deadline first.
        if (accounts != null && "lito_mysql".equals(config.databaseMode())) {
            battleClock.scheduleWithFixedDelay(this::houseAuctionTickSafely,
                    5, 5, TimeUnit.MINUTES);
            // Retained ServerHouseTaxTimer cadence. Tax default is a separate
            // exact-snapshot mutation so auction settlement and tax eviction
            // cannot partially publish one another.
            battleClock.scheduleWithFixedDelay(this::houseTaxTickSafely,
                    10, 10, TimeUnit.MINUTES);
        }
        // Retained DollTimer decrements every second, independently of item expiry.
        battleClock.scheduleWithFixedDelay(this::magicDollTickSafely,
                1, 1, TimeUnit.SECONDS);
        battleClock.scheduleWithFixedDelay(this::companionTickSafely,
                50, 50, TimeUnit.MILLISECONDS);
        battleClock.scheduleWithFixedDelay(this::classicWorldCkewTickSafely,
                1, 1, TimeUnit.SECONDS);
        battleClock.scheduleWithFixedDelay(this::classicWorldWaterDragonTickSafely,
                100, 100, TimeUnit.MILLISECONDS);
        battleClock.scheduleWithFixedDelay(this::classicWorldShipTickSafely,
                5, 5, TimeUnit.SECONDS);
        // Retained NpcDigestItemTimer is independent from ordinary monster AI.
        battleClock.scheduleWithFixedDelay(this::classicWorldMonsterDigestTickSafely,
                5, 5, TimeUnit.SECONDS);
        battleClock.scheduleWithFixedDelay(this::classicCastleWarTickSafely,
                1, 1, TimeUnit.SECONDS);
        // Retained NpcBowTimer is independent of ordinary monster AI and
        // continues to fire every 1500 ms whenever a player recognizes an
        // aperture.  Do not gate it behind classic.world.monster.ai.enabled.
        battleClock.scheduleWithFixedDelay(this::classicWorldBowTickSafely,
                ClassicWorldBowCatalog.TICK_INTERVAL_MILLIS,
                ClassicWorldBowCatalog.TICK_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS);
        if (aiEnabled) {
            battleClock.scheduleWithFixedDelay(this::tickSafely,
                    config.battleAiTickMs(), config.battleAiTickMs(), TimeUnit.MILLISECONDS);
        }
        if (classicWorldMonsterAiEnabled) {
            battleClock.scheduleWithFixedDelay(this::classicWorldMonsterTickSafely,
                    config.classicWorldMonsterAiTickMs(),
                    config.classicWorldMonsterAiTickMs(), TimeUnit.MILLISECONDS);
        }
    }

    synchronized ChannelSnapshot enterChannel(int channelId, ClientSession session) {
        Channel target = channels.get(channelId);
        if (target == null) return null;
        Integer previousId = nativeChannel(session);
        TournamentPresence.ChannelSnapshot admitted = tournamentPresence.enterChannel(nativePresenceOwners.get(session), channelId);
        if (admitted == null) return null;
        if (previousId != null && previousId != channelId) {
            matchMakingQueue.remove(session);
        }
        ensureDefaultRoom(target);
        return snapshot(target);
    }

    static ChannelTier recommendedChannel(int level) {
        return CHANNEL_TIERS.stream().filter(tier -> tier.containsLevel(level))
                .findFirst().orElse(null);
    }

    /**
     * S_CB row +0x5C is a 0..4 ranking-label selector.  The target client has
     * four playable channel buttons (0..3); selector 4 is retained only for a
     * character below the minimum playable Tournament level.
     */
    static int characterRankingSelector(int level) {
        ChannelTier tier = recommendedChannel(level);
        return tier == null ? CHANNEL_COUNT : tier.id();
    }

    /**
     * Exact dynamic C_E3 channel dropdown domain from {@code sub_454670}:
     * all higher tiers, the character's own tier and at most one lower tier.
     * Selector 4 is the native below-playable-level label and remains the
     * terminal value; 0x7F is the independently exposed "none" sentinel.
     */
    static boolean canRequestMatchMakingChannel(int level, int requestedChannel) {
        if (requestedChannel == Protocol.MatchMakingRequest.NO_CHANNEL_RESTRICTION) return true;
        int own = characterRankingSelector(level);
        int maximum = own >= 4 ? 4 : own + 1;
        return requestedChannel >= 0 && requestedChannel <= maximum;
    }

    /**
     * Official LitoHelp2 rule: a player may freely use a higher-level channel,
     * but cannot create a room after entering a lower-level channel.
     */
    static boolean canCreateRoom(int channelId, int characterLevel) {
        ChannelTier recommended = recommendedChannel(characterLevel);
        return recommended != null && channelId >= 0 && channelId < CHANNEL_COUNT
                && channelId <= recommended.id();
    }

    /** Selected Taiwan/Korea LTstring[41] labels the common room field 2..100. */
    static boolean nativeRoomCapacity(int capacity) {
        return TournamentRoomPolicy.nativeRoomCapacity(capacity);
    }

    static int nativeCombatantCapacity(RoomSettings settings) {
        BattleRules.Mode mode = BattleRules.Mode.fromWire(settings.option0() & 0x7F);
        return switch (mode) {
            case PET_RACE -> Math.min(
                    settings.capacity(), BattlePetRaceCourseCatalog.MAXIMUM_PLAYERS);
            case TIME_ATTACK -> Math.min(
                    settings.capacity(), BattleTimeAttackEncounter.MAXIMUM_PLAYERS);
            case SIEGE_WAR -> Math.min(
                    settings.capacity(), BattleRules.SIEGE_MAXIMUM_PLAYERS);
            default -> settings.capacity();
        };
    }

    /**
     * In a lower-level channel, an upper-tier player may enter only rooms whose
     * native create-room attribute enables upper-tier participation (0x10).
     */
    static boolean canJoinRoom(int channelId, int characterLevel, RoomSettings settings) {
        // Keep the native adapter's lazy access to settings for inadmissible
        // levels/channels and players already admitted by their tier.
        return TournamentRoomPolicy.canJoin(channelId, characterLevel, false)
                || (TournamentRoomPolicy.canJoin(channelId, characterLevel, true)
                && settings.allowsUpperTier());
    }

    synchronized void leaveChannel(int channelId, ClientSession session) {
        boolean inRoom = rooms.values().stream().anyMatch(room -> room.members.contains(session));
        if (inRoom) return;
        matchMakingQueue.remove(session);
        if (java.util.Objects.equals(nativeChannel(session), channelId)) {
            tournamentPresence.leaveChannel(nativePresenceOwners.get(session));
        }
    }

    synchronized long createRoom(int channelId, RoomSettings settings) {
        Channel channel = requiredChannel(channelId);
        // User-created rooms own a fresh id and are removed when the last member leaves.
        ensureDefaultRoom(channel);
        long id = nextRoomId++;
        rooms.put(id, new Room(id, channelId, settings, battleControls));
        channel.roomIds.add(id);
        return id;
    }

    synchronized boolean roomExists(int channelId, long roomId) {
        Room room = rooms.get(roomId);
        return room != null && room.channelId == channelId;
    }

    /**
     * Finds a room that the caller can actually enter before S_EB reports
     * matchmaking success.  A zero result means that no public eligible room
     * exists; room ids are allocated from one, so zero is an unambiguous
     * sentinel and must never be serialized as a successful S_CE join.
     */
    synchronized long matchRoom(int channelId, int characterLevel, boolean observer) {
        return matchRoom(channelId, characterLevel, observer, null);
    }

    synchronized long matchRoom(int channelId, int characterLevel, boolean observer,
                                BattleRules.Mode requestedMode) {
        Channel channel = requiredChannel(channelId);
        ensureDefaultRoom(channel);
        return channel.roomIds.stream().map(rooms::get)
                .filter(java.util.Objects::nonNull)
                .filter(room -> !room.battleStarted)
                // Native C_E3 exposes only the six ordinary battle modes
                // (0..5).  Server-gated mode 6 is the rental town scene and
                // must never be selected by battle matchmaking.
                .filter(room -> room.match.rules().mode().isBattleScene())
                .filter(room -> requestedMode == null
                        || room.match.rules().mode() == requestedMode)
                .filter(room -> room.settings.password().isEmpty())
                .filter(room -> canJoinRoom(channelId, characterLevel, room.settings))
                .filter(room -> !observer || room.settings.allowsObservers())
                .filter(room -> observer || room.combatantCount()
                        < nativeCombatantCapacity(room.settings))
                .mapToLong(room -> room.id).findFirst().orElse(0);
    }

    /**
     * Enqueues one already acknowledged C_E3 request. Exact request fields are
     * compatibility constraints.  A contemporary 2003 report of NCsoft's
     * premium-server update says Match Making finds players with similar win
     * rates; production therefore orders compatible waiters through
     * {@link NativeLitoMatchMakingPolicy}.  The selected client's
     * Random/自動選擇 labels independently prove server-side grouping.
     * Maximum distance, zero-history treatment and equal-distance tie-break
     * remain explicit server policy rather than recovered historical values.
     * LTstring[285/286] further proves that participantRestriction is a
     * per-force participant-count interval, so tickets must preserve exact
     * interval equality.  Its numeric cutoffs remain intentionally absent
     * because the client receives those values from the original server.
     */
    synchronized MatchMakingQueueResult enqueueMatchMaking(
            ClientSession session, int channelId, Protocol.MatchMakingRequest request,
            GameStateStore.TournamentStatistics statistics) {
        if (session == null || request == null || statistics == null) {
            throw new IllegalArgumentException("matchmaking queue requires session/request/statistics");
        }
        requiredChannel(channelId);
        MatchMakingTicket incoming = new MatchMakingTicket(session, channelId, request,
                statistics, nextMatchMakingSequence++);
        matchMakingQueue.put(session, incoming);

        RoomSettings automatic = automaticMatchMakingRoomSettings(request.requestedMode());
        int requiredPlayers = BattleRules.decode(
                automatic, battleControls).minimumPlayers();
        if (requiredPlayers <= 1) {
            matchMakingQueue.remove(session);
            return new MatchMakingQueueResult(createRoom(channelId, automatic), List.of(session));
        }

        List<MatchMakingTicket> candidates = matchMakingQueue.values().stream()
                .filter(ticket -> ticket.session() != session)
                .filter(ticket -> compatibleMatchMakingTickets(incoming, ticket))
                .sorted((left, right) -> NativeLitoMatchMakingPolicy.compareCandidates(
                        incoming.statistics(), left.statistics(), left.sequence(),
                        right.statistics(), right.sequence()))
                .toList();
        if (candidates.size() < requiredPlayers - 1) return MatchMakingQueueResult.waiting();

        List<MatchMakingTicket> cohort = new ArrayList<>();
        cohort.add(incoming);
        cohort.addAll(candidates.subList(0, requiredPlayers - 1));
        cohort.sort(java.util.Comparator.comparingLong(MatchMakingTicket::sequence));
        for (MatchMakingTicket ticket : cohort) matchMakingQueue.remove(ticket.session());
        long roomId = createRoom(channelId, automatic);
        return new MatchMakingQueueResult(roomId,
                cohort.stream().map(MatchMakingTicket::session).toList());
    }

    synchronized void cancelMatchMaking(ClientSession session) {
        matchMakingQueue.remove(session);
    }

    synchronized int queuedMatchMakingCount() {
        return matchMakingQueue.size();
    }

    String battleControlDescription() {
        return "lito_static(rules=" + battleControls.configuredRuleCount()
                + ", objects=" + battleControls.configuredObjectCount()
                + ", npc-spawn-rows=" + battleControls.configuredSpawnRowCount()
                + ", player-spawn-rows="
                + battleControls.configuredPlayerSpawnRowCount() + ")";
    }

    static double winRateDistance(GameStateStore.TournamentStatistics left,
                                  GameStateStore.TournamentStatistics right) {
        return NativeLitoMatchMakingPolicy.distance(left, right);
    }

    private static boolean compatibleMatchMakingTickets(MatchMakingTicket left,
                                                         MatchMakingTicket right) {
        return left.channelId() == right.channelId()
                && left.request().fixedSelector() == right.request().fixedSelector()
                && left.request().channelRestriction() == right.request().channelRestriction()
                && left.request().matchMethod() == right.request().matchMethod()
                && left.request().participantRestriction()
                    == right.request().participantRestriction();
    }

    private RoomSettings automaticMatchMakingRoomSettings(BattleRules.Mode mode) {
        if (mode == null || !mode.isBattleScene()) {
            throw new IllegalArgumentException("automatic matchmaking requires a battle mode");
        }
        int attributes = RoomSettings.ALLOW_OBSERVERS
                | (mode.teamBased() ? RoomSettings.EQUAL_SIDES : 0);
        RoomSettings provisional = new RoomSettings("Auto Match", "", 2, attributes,
                mode.wireId(), 0, 0, 0, 0, List.of());
        int minimumPlayers = BattleRules.decode(
                provisional, battleControls).minimumPlayers();
        return new RoomSettings("Auto Match", "", Math.max(2, minimumPlayers), attributes,
                mode.wireId(), 0, 0, 0, 0, List.of());
    }

    synchronized boolean passwordMatches(int channelId, long roomId, String supplied) {
        Room room = rooms.get(roomId);
        return room != null && room.channelId == channelId
                && (room.settings.password().isEmpty() || room.settings.password().equals(supplied));
    }

    synchronized boolean hasCapacity(int channelId, long roomId, boolean observer) {
        Room room = rooms.get(roomId);
        return room != null && room.channelId == channelId
                && (observer || room.combatantCount()
                        < nativeCombatantCapacity(room.settings));
    }

    /**
     * Atomically commits a room seat.  ClientSession uses this before sending
     * S_EB success/S_CE so two concurrent joiners cannot both observe the last
     * free slot and receive a false success.
     */
    synchronized RoomJoinOutcome tryJoinRoom(long roomId, ClientSession session) {
        return tryJoinRoom(roomId, session, session.characterType(),
                session.nativeClassicServerId(),
                session.nativeTournamentAffiliationName());
    }

    synchronized RoomJoinOutcome tryJoinRoom(
            long roomId, ClientSession session, int characterType, int sourceServerId) {
        return tryJoinRoom(roomId, session, characterType, sourceServerId,
                session.nativeTournamentAffiliationName());
    }

    synchronized RoomJoinOutcome tryJoinRoom(
            long roomId, ClientSession session, int characterType, int sourceServerId,
            String tournamentTeamName) {
        Room room = required(roomId);
        Integer channelId = nativeChannel(session);
        if (channelId == null || room.channelId != channelId) {
            throw new IllegalArgumentException("room does not belong to session channel");
        }
        if (room.members.contains(session)) {
            int before = Math.max(0, room.combatantCount() - (session.isObserver() ? 0 : 1));
            return new RoomJoinOutcome(true, new ArrayList<>(room.members), before,
                    RoomJoinFailure.NONE);
        }
        if (room.kickedParticipantIdentities.contains(roomParticipantIdentity(session))) {
            return RoomJoinOutcome.rejected(RoomJoinFailure.KICKED);
        }
        if (room.resultPending) {
            return RoomJoinOutcome.rejected(RoomJoinFailure.BATTLE_ENDED);
        }
        if (room.battleStarted && !session.isObserver()) {
            return RoomJoinOutcome.rejected(RoomJoinFailure.BATTLE_STARTED);
        }
        String teamName = tournamentTeamName == null ? "" : tournamentTeamName.trim();
        if (!session.isObserver() && room.settings.restrictsEventTeam()
                && !teamName.isEmpty()
                && !room.settings.name().trim().equalsIgnoreCase(teamName)) {
            return RoomJoinOutcome.rejected(RoomJoinFailure.TEAM_ROOM_ONLY);
        }
        if (!session.isObserver()
                && !room.settings.allowsCharacterClass(characterType)) {
            return RoomJoinOutcome.rejected(RoomJoinFailure.CLASS_RESTRICTED);
        }
        if (!session.isObserver()
                && !room.settings.allowsSourceServer(sourceServerId)) {
            return RoomJoinOutcome.rejected(RoomJoinFailure.SERVER_RESTRICTED);
        }
        int combatantsBefore = room.combatantCount();
        if (!session.isObserver()
                && combatantsBefore >= nativeCombatantCapacity(room.settings)) {
            return RoomJoinOutcome.rejected(RoomJoinFailure.FULL);
        }
        // Channel membership was already atomically admitted by TournamentPresence.
        room.members.add(session);
        if (room.owner == null) room.owner = session;
        room.ready.remove(session);
        if (!session.isObserver()
                && room.match.rules().mode() == BattleRules.Mode.SIEGE_WAR) {
            room.forces.put(session, smallerSiegeForce(room, session));
        }
        return new RoomJoinOutcome(true, new ArrayList<>(room.members), combatantsBefore,
                RoomJoinFailure.NONE);
    }

    synchronized List<ClientSession> joinRoom(long roomId, ClientSession session) {
        RoomJoinOutcome outcome = tryJoinRoom(roomId, session);
        if (!outcome.joined()) throw new IllegalStateException("room has no joinable combatant seat");
        return outcome.members();
    }

    synchronized boolean leaveRoom(long roomId, ClientSession session) {
        Room room = rooms.get(roomId);
        if (room == null) return false;
        removePrivateShop(session);
        privateShopViews.remove(session);
        room.members.remove(session);
        room.ready.remove(session);
        room.forces.remove(session);
        cancelCountdownIfNotReady(room);
        transferOwner(room, session);
        boolean deleted = room.id != requiredChannel(room.channelId).defaultRoomId
                && room.members.isEmpty();
        resetOrRemove(room);
        return deleted;
    }

    synchronized List<Long> disconnect(ClientSession session) {
        SourceAdministration.clear(session);
        String departingSourceIdentity = "";
        String departingAccountIdentity = "";
        String departingClanIdentity = "";
        String departingClanKey = "";
        if (state != null && session != null
                && !session.selectedCharacterName().isBlank()) {
            try {
                departingSourceIdentity = session.sourceCharacterIdentity();
                departingAccountIdentity = session.sourceAccountIdentity();
                departingClanKey = clanMembership.getOrDefault(
                        key(departingSourceIdentity), "");
                ClanProfile clan = departingClanKey.isBlank()
                        ? null : clanProfiles.get(departingClanKey);
                if (clan != null && !clan.name.isBlank()) {
                    departingClanIdentity = session.sourceClanIdentity(clan.name);
                }
            } catch (IllegalArgumentException incompleteSelection) {
                // Character selection without a valid classic source never
                // created a source-qualified profile, so there is no working
                // set to release.
            }
        }
        connectedSessions.remove(session);
        matchMakingQueue.remove(session);
        authenticatedGameAccounts.values().removeIf(owner -> owner == session);
        clearNativeSelection(session);
        ClassicWorldLocation departingClassicWorldLocation =
                classicWorldMembership.remove(session);
        if (!departingSourceIdentity.isBlank()
                && departingClassicWorldLocation != null
                && departingClassicWorldLocation.mapId()
                    == ClassicWorldWaterDragonCatalog.MAP_ID
                && departingClassicWorldLocation.instanceId() > 0) {
            classicWorldWaterDragonInstances.leave(
                    departingSourceIdentity, "disconnect");
        }
        clearClassicWorldTargetsForOwner(session);
        releaseClanWarehouse(session);
        removePrivateShop(session);
        privateShopViews.remove(session);
        PlayerTrade staleTrade = playerTrades.get(session);
        if (staleTrade != null) clearPlayerTrade(staleTrade);
        List<Long> deletedRooms = new ArrayList<>();
        for (Room room : new ArrayList<>(rooms.values())) {
            room.members.remove(session);
            room.ready.remove(session);
            room.forces.remove(session);
            cancelCountdownIfNotReady(room);
            transferOwner(room, session);
            if (room.id != requiredChannel(room.channelId).defaultRoomId
                    && room.members.isEmpty() && !room.disconnectResolutionPending
                    && DurableBattleSettlement.cleanupAllowed(this, room.id)) {
                deletedRooms.add(room.id);
            }
            resetOrRemove(room);
        }
        TournamentPresence.Owner presenceOwner = nativePresenceOwners.remove(session);
        nativePresencePeers.remove(presenceOwner);
        tournamentPresence.disconnect(presenceOwner);
        if (state != null && !departingSourceIdentity.isBlank()) {
            String identity = departingSourceIdentity;
            boolean stillActive = selectedCharacters.keySet().stream()
                    .filter(member -> !member.selectedCharacterName().isBlank())
                    .anyMatch(member -> {
                        try {
                            return member.sourceCharacterIdentity()
                                    .equalsIgnoreCase(identity);
                        } catch (IllegalArgumentException incompleteSelection) {
                            return false;
                        }
                    });
            if (!stillActive) {
                try {
                    GameStateStore.SourceProfileRelease released =
                            state.releaseSourceCharacter(identity);
                    if (released.released()) {
                        System.out.println("[LiTo] released ephemeral source profile="
                                + identity.replace('\u001F', '/')
                                + " pets=" + released.pets()
                                + " provenance=" + released.provenanceKeys());
                    }
                } catch (IOException error) {
                    System.out.println("[LiTo] ephemeral source-profile sidecar cleanup "
                            + "failed identity=" + identity.replace('\u001F', '/')
                            + ": " + error.getMessage());
                }
            }
        }
        if (state != null && !departingAccountIdentity.isBlank()) {
            String accountIdentity = departingAccountIdentity;
            boolean accountStillActive = selectedCharacters.keySet().stream()
                    .filter(member -> !member.selectedCharacterName().isBlank())
                    .anyMatch(member -> {
                        try {
                            return member.sourceAccountIdentity()
                                    .equalsIgnoreCase(accountIdentity);
                        } catch (IllegalArgumentException incompleteSelection) {
                            return false;
                        }
                    });
            if (!accountStillActive) {
                try {
                    state.releaseSourceAccount(accountIdentity);
                } catch (IOException error) {
                    System.out.println("[LiTo] ephemeral source-account cleanup failed "
                            + "identity=" + accountIdentity.replace('\u001F', '/')
                            + ": " + error.getMessage());
                }
            }
        }
        if (state != null && !departingClanIdentity.isBlank()
                && !departingClanKey.isBlank()) {
            String clanKey = departingClanKey;
            boolean clanStillActive = selectedCharacters.keySet().stream()
                    .map(member -> {
                        try {
                            return key(member.sourceCharacterIdentity());
                        } catch (IllegalArgumentException incompleteSelection) {
                            return "";
                        }
                    })
                    .map(clanMembership::get)
                    .anyMatch(clanKey::equals);
            if (!clanStillActive) {
                try {
                    state.releaseSourceClan(departingClanIdentity);
                } catch (IOException error) {
                    System.out.println("[LiTo] ephemeral source-clan cleanup failed "
                            + "identity=" + departingClanIdentity.replace('\u001F', '/')
                            + ": " + error.getMessage());
                }
                if (ephemeralSourceProfiles) {
                    ClanProfile removedClan = clanProfiles.remove(clanKey);
                    if (removedClan != null && removedClan.emblemId != 0) {
                        ClanEmblem emblem = classicClanEmblems.get(
                                removedClan.emblemId);
                        if (emblem != null && emblem.clanKey.equals(clanKey)) {
                            classicClanEmblems.remove(removedClan.emblemId);
                        }
                    }
                    clanMembership.entrySet().removeIf(
                            entry -> clanKey.equals(entry.getValue()));
                    clanWarehouseLeases.remove(clanKey);
                }
            }
        }
        if (ephemeralSourceProfiles && !departingSourceIdentity.isBlank()) {
            String characterKey = key(departingSourceIdentity);
            buddyProfiles.remove(characterKey);
            buddyPersistenceHooks.remove(characterKey);
            chatSilencedUntilNanos.remove(characterKey);
        }
        // ClientSession normally performs the native leave notification first.
        // Keep the hub authoritative when a socket/session aborts before that
        // best-effort layer can run, so source-qualified party identities never
        // outlive their selected character working set.
        if (!departingSourceIdentity.isBlank()) {
            leaveClassicParty(departingSourceIdentity);
        }
        return deletedRooms;
    }

    synchronized List<ClientSession> enterClassicWorld(
            ClientSession session, int mapId, int x, int y, int heading) {
        return enterClassicWorld(session, mapId, x, y, heading, -1);
    }

    synchronized List<ClientSession> enterClassicWorld(
            ClientSession session, int mapId, int x, int y, int heading,
            int instanceId) {
        if (session == null || !connectedSessions.contains(session)) {
            throw new IllegalArgumentException("classic-world session is not connected");
        }
        ClassicWorldLocation location = new ClassicWorldLocation(
                mapId, x, y, heading, instanceId);
        List<ClientSession> occupants = classicWorldMembers(mapId, instanceId);
        occupants.remove(session);
        classicWorldMembership.put(session, location);
        return occupants;
    }

    /**
     * Atomically replaces an existing WORLD membership.  The CKEW audit uses
     * this same hub monitor, so it can observe either the old location or the
     * installed destination, never the transient gap between leave and enter.
     */
    synchronized List<ClientSession> transitionClassicWorld(
            ClientSession session, int mapId, int x, int y, int heading,
            int instanceId) {
        if (session == null || !connectedSessions.contains(session)
                || !classicWorldMembership.containsKey(session)) {
            throw new IllegalArgumentException(
                    "classic-world transition session is not present");
        }
        ClassicWorldLocation previous = classicWorldMembership.get(session);
        ClassicWorldLocation location = new ClassicWorldLocation(
                mapId, x, y, heading, instanceId);
        clearClassicWorldTargetsForOwner(session);
        if (previous.mapId() == ClassicWorldWaterDragonCatalog.MAP_ID
                && previous.instanceId() > 0
                && (mapId != previous.mapId()
                    || instanceId != previous.instanceId())) {
            classicWorldWaterDragonInstances.leave(
                    session.sourceCharacterIdentity(), "map-leave");
        }
        List<ClientSession> occupants = classicWorldMembers(mapId, instanceId);
        occupants.remove(session);
        classicWorldMembership.put(session, location);
        return occupants;
    }

    synchronized boolean updateClassicWorld(
            ClientSession session, int mapId, int x, int y, int heading) {
        ClassicWorldLocation current = classicWorldMembership.get(session);
        if (current == null || current.mapId() != mapId) return false;
        classicWorldMembership.put(session,
                new ClassicWorldLocation(mapId, x, y, heading,
                        current.instanceId()));
        return true;
    }

    synchronized ClassicWorldLocation classicWorldLocation(ClientSession session) {
        return classicWorldMembership.get(session);
    }

    synchronized List<ClientSession> classicWorldMembers(int mapId) {
        return classicWorldMembers(mapId, -1);
    }

    synchronized List<ClientSession> classicWorldMembers(
            int mapId, int instanceId) {
        List<ClientSession> result = new ArrayList<>();
        for (Map.Entry<ClientSession, ClassicWorldLocation> entry
                : classicWorldMembership.entrySet()) {
            if (entry.getValue().mapId() == mapId
                    && entry.getValue().instanceId() == instanceId) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    synchronized void leaveClassicWorld(ClientSession session) {
        ClassicWorldLocation previous = classicWorldMembership.remove(session);
        if (previous != null
                && previous.mapId() == ClassicWorldWaterDragonCatalog.MAP_ID
                && previous.instanceId() > 0) {
            classicWorldWaterDragonInstances.leave(
                    session.sourceCharacterIdentity(), "disconnect");
        }
        clearClassicWorldTargetsForOwner(session);
    }

    private void clearClassicWorldTargetsForOwner(ClientSession session) {
        if (session == null) return;
        clearClassicWorldMonsterTargets(session.playerObjectId());
        for (BattleCompanion.Snapshot companion
                : session.classicWorldCompanionSnapshots()) {
            clearClassicWorldMonsterTargets(companion.objectId());
        }
    }

    synchronized int classicWorldNpcHeading(ClassicWorldNpcCatalog.StaticNpc npc) {
        if (npc == null) throw new IllegalArgumentException("classic NPC is null");
        return classicWorldNpcHeadings.getOrDefault(npc.objectId(), npc.heading());
    }

    synchronized int classicWorldNpcGfx(ClassicWorldNpcCatalog.StaticNpc npc) {
        if (npc == null || ClassicWorldNpcCatalog.npc(
                npc.mapId(), npc.objectId()) != npc) {
            throw new IllegalArgumentException("classic NPC is not canonical");
        }
        return classicWorldNpcGfxOverrides.getOrDefault(npc.objectId(), npc.gfxId());
    }

    /**
     * Atomically applies an exact static-NPC GFX transition.
     *
     * <p>The retained Cancellation transform keeps the world object and NPC
     * template identity unchanged.  This compare-and-set boundary prevents
     * two simultaneous casters from both claiming the one native mutation.</p>
     */
    synchronized boolean transformClassicWorldNpcGfx(
            ClassicWorldNpcCatalog.StaticNpc npc, int expectedGfx, int targetGfx) {
        if (npc == null || ClassicWorldNpcCatalog.npc(
                npc.mapId(), npc.objectId()) != npc
                || expectedGfx != npc.gfxId() || targetGfx <= 0
                || targetGfx > 0xFFFF || targetGfx == expectedGfx) {
            return false;
        }
        int current = classicWorldNpcGfxOverrides.getOrDefault(
                npc.objectId(), npc.gfxId());
        if (current != expectedGfx) return false;
        classicWorldNpcGfxOverrides.put(npc.objectId(), targetGfx);
        return true;
    }

    /** Restores an exact runtime-mutated static actor to its canonical GFX. */
    synchronized boolean restoreClassicWorldNpcGfx(
            ClassicWorldNpcCatalog.StaticNpc npc, int expectedCurrentGfx) {
        if (npc == null || ClassicWorldNpcCatalog.npc(
                npc.mapId(), npc.objectId()) != npc
                || expectedCurrentGfx <= 0 || expectedCurrentGfx > 0xFFFF) {
            return false;
        }
        int current = classicWorldNpcGfxOverrides.getOrDefault(
                npc.objectId(), npc.gfxId());
        if (current != expectedCurrentGfx || current == npc.gfxId()) return false;
        classicWorldNpcGfxOverrides.remove(npc.objectId());
        return true;
    }

    ClassicWorldRoiFollowers classicWorldRoiFollowers() {
        return classicWorldRoiFollowers;
    }

    ClassicWorldRescueFollowers classicWorldRescueFollowers() {
        return classicWorldRescueFollowers;
    }

    ClassicWorldDspyEscorts classicWorldDspyEscorts() {
        return classicWorldDspyEscorts;
    }

    int repairGiranCastleDoors() {
        return giranCastleDoors.repairAll();
    }

    List<ClassicWorldCastleDoorState.Snapshot> giranCastleDoors() {
        return giranCastleDoors.snapshots();
    }

    int repairWindawoodCastleDoors() {
        return windawoodCastleDoors.repairAll();
    }

    List<ClassicWorldWindawoodCastleDoorState.Snapshot> windawoodCastleDoors() {
        return windawoodCastleDoors.snapshots();
    }

    int repairKentCastleDoors() {
        return kentCastleDoors.repairAll();
    }

    List<ClassicWorldKentCastleDoorState.Snapshot> kentCastleDoors() {
        return kentCastleDoors.snapshots();
    }

    int repairDwarfCastleDoors() {
        return dwarfCastleDoors.repairAll();
    }

    List<ClassicWorldDwarfCastleDoorState.Snapshot> dwarfCastleDoors() {
        return dwarfCastleDoors.snapshots();
    }

    ClassicWorldDwarfCastleDoorState.Snapshot dwarfCastleDoor(
            int mapId, long objectId) {
        return dwarfCastleDoors.snapshot(mapId, objectId);
    }

    ClassicWorldMonsterPopulation.Snapshot dwarfCastleDoorCombatSnapshot(
            int mapId, long objectId, int attackerX, int attackerY) {
        return dwarfCastleDoors.combatSnapshot(
                mapId, objectId, attackerX, attackerY);
    }

    ClassicWorldDwarfCastleDoorState.DoorHit damageDwarfCastleDoor(
            int mapId, long objectId, int damage) {
        return dwarfCastleDoors.damage(mapId, objectId, damage);
    }

    List<ClassicWorldDwarfCastleDoorState.Snapshot> setDwarfCastleDoorsOpen(
            int keeperNpcId, boolean open) {
        return dwarfCastleDoors.setOpenForKeeper(keeperNpcId, open);
    }

    boolean dwarfCastleDoorBlocks(int mapId, int x, int y) {
        return mapId == ClassicWorldDwarfCastleCatalog.MAP_ID
                && dwarfCastleDoors.blocks(x, y);
    }

    List<ClassicWorldCastleDoorCombat.View> mainlandCastleDoors() {
        List<ClassicWorldCastleDoorCombat.View> result = new ArrayList<>();
        result.addAll(kentCastleDoors.views());
        result.addAll(windawoodCastleDoors.views());
        result.addAll(giranCastleDoors.views());
        return List.copyOf(result);
    }

    ClassicWorldCastleDoorCombat.Target mainlandCastleDoorCombatTarget(
            int mapId, long objectId, int attackerX, int attackerY) {
        ClassicWorldCastleDoorCombat.Target target = kentCastleDoors.combatSnapshot(
                mapId, objectId, attackerX, attackerY);
        if (target != null) return target;
        target = windawoodCastleDoors.combatSnapshot(
                mapId, objectId, attackerX, attackerY);
        if (target != null) return target;
        target = giranCastleDoors.combatSnapshot(
                mapId, objectId, attackerX, attackerY);
        if (target != null) return target;
        return heineCastleDoors.combatSnapshot(
                mapId, objectId, attackerX, attackerY);
    }

    ClassicWorldCastleDoorCombat.Hit damageMainlandCastleDoor(
            ClassicWorldCastleDoorCombat.Family family,
            int mapId, long objectId, int damage) {
        if (family == null) return null;
        return switch (family) {
            case KENT -> kentCastleDoors.damage(mapId, objectId, damage);
            case WINDAWOOD -> windawoodCastleDoors.damage(mapId, objectId, damage);
            case GIRAN -> giranCastleDoors.damage(mapId, objectId, damage);
            case HEINE -> heineCastleDoors.damage(mapId, objectId, damage);
        };
    }

    boolean mainlandCastleDoorBlocks(int mapId, int x, int y) {
        return kentCastleDoors.blocks(mapId, x, y)
                || windawoodCastleDoors.blocks(mapId, x, y)
                || giranCastleDoors.blocks(mapId, x, y);
    }

    int repairAdenCastleDoors() {
        return adenCastleDoors.repairAll();
    }

    List<ClassicWorldAdenCastleDoorState.Snapshot> adenCastleDoors() {
        return adenCastleDoors.snapshots();
    }

    List<ClassicWorldAdenCastleDoorState.Snapshot> setAdenCastleDoorsOpen(
            int keeperNpcId, boolean open) {
        return adenCastleDoors.setOpenForKeeper(keeperNpcId, open);
    }

    boolean adenCastleDoorBlocks(int mapId, int x, int y) {
        return mapId == ClassicWorldMapCatalog.WORLD_TREE_MAP_ID
                && adenCastleDoors.blocks(x, y);
    }

    int repairHeineCastleDoors() {
        return heineCastleDoors.repairAll();
    }

    int repairClassicCastleDoors(int castleId) {
        return switch (castleId) {
            case ClassicWorldKentCastleCatalog.CASTLE_ID -> repairKentCastleDoors();
            case ClassicWorldWindawoodCastleCatalog.CASTLE_ID ->
                    repairWindawoodCastleDoors();
            case ClassicWorldGiranCastleCatalog.CASTLE_ID -> repairGiranCastleDoors();
            case ClassicWorldHeineCatalog.CASTLE_ID -> repairHeineCastleDoors();
            case ClassicWorldDwarfCastleCatalog.CASTLE_ID -> repairDwarfCastleDoors();
            case ClassicWorldAdenToiCatalog.CASTLE_ID -> repairAdenCastleDoors();
            default -> 0;
        };
    }

    void registerClassicCastleWarSchedules(
            String source, List<CastleWarSchedule> schedules) {
        String sourceKey = key(source);
        if (sourceKey.isEmpty()) {
            throw new IllegalArgumentException("classic castle-war source is empty");
        }
        if (schedules == null || schedules.isEmpty()) {
            throw new IllegalArgumentException("castle-war schedules are required");
        }
        List<CastleWarSchedule> effectiveSchedules = state == null ? schedules
                : schedules.stream()
                        .map(schedule -> state.effectiveCastleWarSchedule(
                                sourceKey, schedule))
                        .toList();
        List<ClassicWorldCastleWarClock.Event> events;
        synchronized (this) {
            ClassicWorldCastleWarClock clock = classicCastleWarClocks.computeIfAbsent(
                    sourceKey, ignored -> new ClassicWorldCastleWarClock());
            clock.register(effectiveSchedules);
            classicCastleWarField.ensureTowers(sourceKey);
            events = clock.tick(System.currentTimeMillis());
        }
        publishClassicCastleWarEvents(sourceKey, events);
    }

    synchronized boolean classicCastleWarActive(String source, int castleId) {
        ClassicWorldCastleWarClock clock = classicCastleWarClocks.get(key(source));
        return clock != null && clock.active(castleId);
    }

    synchronized long classicCastleWarStartMillis(
            String source, int castleId, long fallback) {
        ClassicWorldCastleWarClock clock = classicCastleWarClocks.get(key(source));
        ClassicWorldCastleWarClock.Snapshot snapshot =
                clock == null ? null : clock.snapshot(castleId);
        return snapshot == null ? fallback : snapshot.startMillis();
    }

    synchronized List<Integer> activeClassicCastleWars(String source) {
        ClassicWorldCastleWarClock clock = classicCastleWarClocks.get(key(source));
        return clock == null ? List.of() : clock.activeCastleIds();
    }

    List<ClassicWorldCastleWarField.WarObject> classicCastleWarObjectsInScreen(
            String source, int mapId, int x, int y) {
        return classicCastleWarField.inScreen(source, mapId, x, y);
    }

    List<ClassicWorldCastleWarField.WarObject> classicCastleWarObjects(
            String source) {
        return classicCastleWarField.objects(source);
    }

    ClassicWorldCastleWarField.WarObject classicCastleWarObject(
            String source, int mapId, long objectId) {
        return classicCastleWarField.object(source, mapId, objectId);
    }

    ClassicWorldMonsterPopulation.Snapshot classicCastleTowerCombatSnapshot(
            String source, int mapId, long objectId) {
        return classicCastleWarField.towerCombatSnapshot(
                source, mapId, objectId);
    }

    ClassicWorldMonsterPopulation.Snapshot classicCastleTrebuchetCombatSnapshot(
            String source, int mapId, long objectId) {
        return classicCastleWarField.trebuchetCombatSnapshot(
                source, mapId, objectId);
    }

    ClassicWorldMonsterPopulation.ManaDrain
            drainClassicCastleTrebuchetManaIfStrictlyAbove(
                    String source, int mapId, long objectId,
                    int requestedDrain) {
        return classicCastleWarField.drainTrebuchetManaIfStrictlyAbove(
                source, mapId, objectId, requestedDrain);
    }

    boolean confusionSilenceClassicCastleTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        return classicCastleWarField.confusionSilenceTrebuchet(
                source, mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicCastleTrebuchetSilenceActive(
            String source, int mapId, long objectId, long nowNanos) {
        return classicCastleWarField.trebuchetSilenceActive(
                source, mapId, objectId, nowNanos);
    }

    boolean thunderGrabClassicCastleTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        return classicCastleWarField.thunderGrabTrebuchet(
                source, mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicCastleTrebuchetThunderGrabActive(
            String source, int mapId, long objectId, long nowNanos) {
        return classicCastleWarField.trebuchetThunderGrabActive(
                source, mapId, objectId, nowNanos);
    }

    boolean boneBreakClassicCastleTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        return classicCastleWarField.boneBreakTrebuchet(
                source, mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicCastleTrebuchetBoneBreakActive(
            String source, int mapId, long objectId, long nowNanos) {
        return classicCastleWarField.trebuchetBoneBreakActive(
                source, mapId, objectId, nowNanos);
    }

    BattleArmBreakerStatus.Activation armBreakerClassicCastleTrebuchet(
            String source, int mapId, long objectId,
            long nowNanos, int durationSeconds) {
        return classicCastleWarField.armBreakerTrebuchet(
                source, mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicCastleTrebuchetArmBreakerActive(
            String source, int mapId, long objectId, long nowNanos) {
        return classicCastleWarField.trebuchetArmBreakerActive(
                source, mapId, objectId, nowNanos);
    }

    synchronized ClassicCastleSiegeResult mutateClassicCastleSiege(
            String source, int castleId, String attackerClan,
            String defenderClan, Protocol.ClanWarRequestType type) {
        String sourceKey = key(source);
        String attacker = attackerClan == null ? "" : attackerClan.trim();
        String defender = defenderClan == null || defenderClan.isBlank()
                ? "NPC" : defenderClan.trim();
        if (sourceKey.isEmpty() || castleId < 1 || castleId > 8
                || attacker.isEmpty() || attacker.equalsIgnoreCase(defender)
                || (type != Protocol.ClanWarRequestType.DECLARE
                    && type != Protocol.ClanWarRequestType.SURRENDER
                    && type != Protocol.ClanWarRequestType.CEASE)
                || !classicCastleWarActive(sourceKey, castleId)) {
            return siegeResult(ClassicCastleSiegeMutation.NOT_ACTIVE,
                    castleId, defender, Set.of(), List.of());
        }
        String storageKey = castleSiegeKey(sourceKey, castleId);
        ClassicCastleSiege siege = classicCastleSieges.get(storageKey);
        if (type == Protocol.ClanWarRequestType.DECLARE) {
            // Retained C_War only emits S_WAR(attacker, "NPC") for an
            // unowned castle.  It does not create an L1War entry; crown
            // acquisition is intentionally open while that castle is unowned.
            if (defender.equalsIgnoreCase("NPC")) {
                return siegeResult(ClassicCastleSiegeMutation.DECLARED,
                        castleId, "NPC", Set.of(attacker),
                        List.of(new ClassicCastleWarNotice(
                                Protocol.ClanWarNoticeType.DECLARED,
                                attacker, "NPC", "declare-unowned-castle")));
            }
            if (siege == null) {
                siege = new ClassicCastleSiege(
                        sourceKey, castleId, defender);
                classicCastleSieges.put(storageKey, siege);
            }
            if (!siege.defenderClan.equalsIgnoreCase(defender)) {
                return siegeResult(ClassicCastleSiegeMutation.INVALID,
                        castleId, siege.defenderClan,
                        siege.attackingClans, List.of());
            }
            if (!siege.attackingClans.add(attacker)) {
                return siegeResult(ClassicCastleSiegeMutation.ALREADY_DECLARED,
                        castleId, siege.defenderClan,
                        siege.attackingClans, List.of());
            }
            List<ClassicCastleWarNotice> notices = List.of(
                    new ClassicCastleWarNotice(
                            Protocol.ClanWarNoticeType.DECLARED,
                            attacker, defender, "declare-castle"));
            return siegeResult(ClassicCastleSiegeMutation.DECLARED,
                    castleId, siege.defenderClan,
                    siege.attackingClans, notices);
        }
        if (siege == null || siege.attackingClans.stream().noneMatch(
                clan -> clan.equalsIgnoreCase(attacker))) {
            return siegeResult(ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                    castleId, defender, Set.of(), List.of());
        }
        siege.attackingClans.removeIf(clan -> clan.equalsIgnoreCase(attacker));
        List<ClassicCastleWarNotice> notices =
                type == Protocol.ClanWarRequestType.SURRENDER
                ? List.of(
                    new ClassicCastleWarNotice(
                            Protocol.ClanWarNoticeType.SURRENDERED, attacker,
                            siege.defenderClan, "castle-surrender"),
                    new ClassicCastleWarNotice(
                            Protocol.ClanWarNoticeType.WON, siege.defenderClan,
                            attacker, "castle-defender-won"))
                : List.of(new ClassicCastleWarNotice(
                        Protocol.ClanWarNoticeType.ENDED, attacker,
                        siege.defenderClan, "castle-cease"));
        if (siege.attackingClans.isEmpty()) classicCastleSieges.remove(storageKey);
        return siegeResult(type == Protocol.ClanWarRequestType.SURRENDER
                        ? ClassicCastleSiegeMutation.SURRENDERED
                        : ClassicCastleSiegeMutation.CEASED,
                castleId, siege.defenderClan,
                siege.attackingClans, notices);
    }

    /**
     * Retained {@code castleWarResult}: the crown who owns an attacking clan
     * leaves the first matching castle war immediately on death.  Other
     * attacking clans remain in the same siege.
     */
    synchronized ClassicCastleSiegeResult
            ceaseClassicCastleSiegeForAttackingLeaderDeath(
                    String source, String victimOwnerIdentity) {
        ClanProfile victimClan = ownedClan(victimOwnerIdentity);
        if (victimClan == null) {
            return siegeResult(ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                    0, "", Set.of(), List.of());
        }
        String sourceKey = key(source);
        String matchedStorageKey = null;
        ClassicCastleSiege matched = null;
        String attackerName = null;
        for (Map.Entry<String, ClassicCastleSiege> entry
                : classicCastleSieges.entrySet()) {
            ClassicCastleSiege candidate = entry.getValue();
            if (!candidate.source.equals(sourceKey)) continue;
            attackerName = candidate.attackingClans.stream()
                    .filter(name -> name.equalsIgnoreCase(victimClan.name))
                    .findFirst().orElse(null);
            if (attackerName != null) {
                matchedStorageKey = entry.getKey();
                matched = candidate;
                break;
            }
        }
        if (matched == null) {
            return siegeResult(ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                    0, "", Set.of(), List.of());
        }
        matched.attackingClans.remove(attackerName);
        Set<String> remaining = Set.copyOf(matched.attackingClans);
        if (matched.attackingClans.isEmpty()) {
            classicCastleSieges.remove(matchedStorageKey, matched);
        }
        return siegeResult(ClassicCastleSiegeMutation.CEASED,
                matched.castleId, matched.defenderClan, remaining,
                List.of(new ClassicCastleWarNotice(
                        Protocol.ClanWarNoticeType.ENDED,
                        attackerName, matched.defenderClan,
                        "castle-attacking-leader-death")));
    }

    synchronized boolean classicCastleSiegeParticipant(
            String source, int castleId, String clanName) {
        ClassicCastleSiege siege = classicCastleSieges.get(
                castleSiegeKey(source, castleId));
        return siege != null && clanName != null
                && (siege.defenderClan.equalsIgnoreCase(clanName)
                    || siege.attackingClans.stream().anyMatch(
                        clan -> clan.equalsIgnoreCase(clanName)));
    }

    synchronized ClassicCastleSiegeResult winClassicCastleSiege(
            String source, int castleId, String winnerClan) {
        String storageKey = castleSiegeKey(source, castleId);
        ClassicCastleSiege siege = classicCastleSieges.get(storageKey);
        if (siege == null || winnerClan == null || winnerClan.isBlank()
                || siege.attackingClans.stream().noneMatch(
                        clan -> clan.equalsIgnoreCase(winnerClan))) {
            return siegeResult(ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                    castleId, siege == null ? "" : siege.defenderClan,
                    siege == null ? Set.of() : siege.attackingClans,
                    List.of());
        }
        classicCastleSieges.remove(storageKey);
        List<ClassicCastleWarNotice> notices = new ArrayList<>();
        if (!siege.defenderClan.equalsIgnoreCase("NPC")) {
            notices.add(new ClassicCastleWarNotice(
                    Protocol.ClanWarNoticeType.WON,
                    winnerClan, siege.defenderClan, "castle-crown-winner"));
            for (String attacker : siege.attackingClans) {
                if (!attacker.equalsIgnoreCase(winnerClan)) {
                    notices.add(new ClassicCastleWarNotice(
                            Protocol.ClanWarNoticeType.WON,
                            siege.defenderClan, attacker,
                            "castle-defender-over-other-attacker"));
                }
            }
            for (String attacker : siege.attackingClans) {
                notices.add(new ClassicCastleWarNotice(
                        Protocol.ClanWarNoticeType.ENDED,
                        siege.defenderClan, attacker, "castle-war-ended"));
            }
        }
        return siegeResult(ClassicCastleSiegeMutation.CEASED,
                castleId, siege.defenderClan, siege.attackingClans, notices);
    }

    private synchronized ClassicCastleSiegeResult ceaseClassicCastleSiege(
            String source, int castleId) {
        ClassicCastleSiege siege = classicCastleSieges.remove(
                castleSiegeKey(source, castleId));
        if (siege == null) {
            return siegeResult(ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                    castleId, "", Set.of(), List.of());
        }
        List<ClassicCastleWarNotice> notices = new ArrayList<>();
        if (!siege.defenderClan.equalsIgnoreCase("NPC")) {
            for (String attacker : siege.attackingClans) {
                notices.add(new ClassicCastleWarNotice(
                        Protocol.ClanWarNoticeType.WON,
                        siege.defenderClan, attacker, "castle-timeout-defender-won"));
            }
            for (String attacker : siege.attackingClans) {
                notices.add(new ClassicCastleWarNotice(
                        Protocol.ClanWarNoticeType.ENDED,
                        siege.defenderClan, attacker, "castle-timeout-ended"));
            }
        }
        return siegeResult(ClassicCastleSiegeMutation.CEASED,
                castleId, siege.defenderClan, siege.attackingClans, notices);
    }

    ClassicWorldCastleWarField.TowerHit damageClassicCastleTower(
            String source, int mapId, long objectId, String attackerClan,
            String defenderClan, int damage) {
        ClassicWorldCastleWarField.WarObject target =
                classicCastleWarField.object(source, mapId, objectId);
        if (target == null || target.kind() != ClassicWorldCastleWarField.Kind.TOWER
                || !classicCastleWarActive(source, target.castleId())) return null;
        String defender = defenderClan == null ? "" : defenderClan.trim();
        boolean hasDefender = !defender.isBlank()
                && !defender.equalsIgnoreCase("NPC");
        if (hasDefender && !classicCastleSiegeParticipant(
                source, target.castleId(), attackerClan)) return null;
        return classicCastleWarField.damageTower(
                source, mapId, objectId, damage);
    }

    ClassicWorldCastleWarField.TrebuchetHit damageClassicCastleTrebuchet(
            String source, int mapId, long objectId, int damage) {
        return classicCastleWarField.damageTrebuchet(
                source, mapId, objectId, damage);
    }

    void scheduleClassicCastleTrebuchetCorpseRemoval(
            String source, ClassicWorldCastleWarField.WarObject dead) {
        if (dead == null || dead.kind() != ClassicWorldCastleWarField.Kind.TREBUCHET
                || dead.currentHp() != 0) return;
        String sourceKey = key(source);
        battleClock.schedule(() -> {
            try {
                ClassicWorldCastleWarField.WarObject removed =
                        classicCastleWarField.removeTrebuchetCorpse(
                                sourceKey, dead.mapId(), dead.objectId());
                if (removed == null) return;
                for (ClientSession member : connectedSessionsSnapshot()) {
                    if (member.usesClassicSource(sourceKey)) {
                        member.serverClassicCastleTrebuchetRemoved(removed);
                    }
                }
            } catch (Exception error) {
                System.out.println("[LiTo] castle trebuchet removal failed object="
                        + dead.objectId() + " reason="
                        + error.getClass().getSimpleName() + ": "
                        + error.getMessage());
            }
        }, 10, TimeUnit.SECONDS);
    }

    void scheduleRentalHotelExpiry(
            ClientSession session, long leaseToken, long delayNanos) {
        if (session == null || leaseToken <= 0 || delayNanos < 0) return;
        battleClock.schedule(() -> {
            try {
                session.serverRentalHotelExpiry(leaseToken);
            } catch (Exception error) {
                System.out.println("[LiTo] rental-hotel expiry failed token="
                        + leaseToken + " reason="
                        + error.getClass().getSimpleName() + ": "
                        + error.getMessage());
            }
        }, delayNanos, TimeUnit.NANOSECONDS);
    }

    void scheduleCloneAppearanceProbeRestore(ClientSession session, long roomId) {
        if (session == null || roomId <= 0) return;
        battleClock.schedule(() -> {
            try {
                session.serverCloneAppearanceProbeRestore(roomId);
            } catch (Exception error) {
                System.out.println("[LiTo] clone-appearance probe restore failed room="
                        + roomId + " reason=" + error.getClass().getSimpleName()
                        + ": " + error.getMessage());
            }
        }, 5, TimeUnit.SECONDS);
    }

    void scheduleCloneAppearanceProbeAutoBattle(ClientSession session, int channelId) {
        if (session == null || channelId < 0) return;
        battleClock.schedule(() -> {
            try {
                session.serverCloneAppearanceProbeAutoBattle(channelId);
            } catch (Exception error) {
                System.out.println("[LiTo] clone-appearance fixture auto-battle failed channel="
                        + channelId + " reason=" + error.getClass().getSimpleName()
                        + ": " + error.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
    }

    void scheduleCloneAppearanceProbeAutoReady(ClientSession session, long roomId) {
        if (session == null || roomId <= 0) return;
        // The native S_D1 READY handler compares the packet name against the
        // launcher-provided room identity. Preserve a real handoff interval
        // after S_CE/S_CD instead of batching READY into the room-create write.
        battleClock.schedule(() -> {
            try {
                session.serverCloneAppearanceProbeAutoReady(roomId);
            } catch (Exception error) {
                System.out.println("[LiTo] clone-appearance fixture auto-ready failed room="
                        + roomId + " reason=" + error.getClass().getSimpleName()
                        + ": " + error.getMessage());
            }
        }, CLONE_PROBE_NATIVE_ROOM_HANDOFF_MILLIS, TimeUnit.MILLISECONDS);
    }


    RentalTownHotelRegistry.Lease registerRentalHotelLease(
            long token, long ownerObjectId, int keyCount,
            NativeLitoRentalHotelCatalog.Room room, long expiresAtNanos,
            java.util.Collection<Long> keyObjectIds) {
        return rentalTownHotels.register(token, ownerObjectId, keyCount,
                room, expiresAtNanos, keyObjectIds);
    }

    RentalTownHotelRegistry.Lease rentalHotelLease(
            long token, long nowNanos) {
        return rentalTownHotels.lease(token, nowNanos);
    }

    RentalTownHotelRegistry.Lease rentalHotelLeaseForInventory(
            java.util.Collection<RuntimeInventory.Item> items, long nowNanos) {
        return rentalTownHotels.leaseForInventory(items, nowNanos);
    }

    boolean rentalHotelInventoryOwnsCredential(
            long token, java.util.Collection<RuntimeInventory.Item> items,
            long nowNanos) {
        return rentalTownHotels.inventoryOwnsCredential(token, items, nowNanos);
    }

    RentalTownHotelRegistry.GroundItem putRentalHotelGroundKey(
            long roomId, int mapId, long instanceToken,
            RuntimeInventory.Item source, int x, int y, long nowNanos) {
        return rentalTownHotels.putGround(
                roomId, mapId, instanceToken, source, x, y, nowNanos);
    }

    RentalTownHotelRegistry.GroundItem rentalHotelGroundKey(
            long roomId, int mapId, long instanceToken, long objectId) {
        return rentalTownHotels.groundItem(
                roomId, mapId, instanceToken, objectId);
    }

    java.util.List<RentalTownHotelRegistry.GroundItem> rentalHotelGroundKeys(
            long roomId, int mapId, long instanceToken) {
        return rentalTownHotels.groundItems(roomId, mapId, instanceToken);
    }

    RentalTownHotelRegistry.GroundItem takeRentalHotelGroundKey(
            long roomId, int mapId, long instanceToken, long objectId) {
        return rentalTownHotels.takeGround(
                roomId, mapId, instanceToken, objectId);
    }

    RentalTownHotelRegistry.Invalidation invalidateRentalHotelLease(long token) {
        return rentalTownHotels.invalidate(token);
    }

    void discardRentalHotelKeys(java.util.Collection<Long> objectIds) {
        rentalTownHotels.discardKeys(objectIds);
    }

    synchronized void setClassicCastleDefender(
            String source, int castleId, String defenderClan) {
        String storageKey = castleSiegeKey(source, castleId);
        ClassicCastleSiege siege = classicCastleSieges.get(storageKey);
        if (siege != null) return;
        String defender = defenderClan == null || defenderClan.isBlank()
                ? "NPC" : defenderClan.trim();
        classicCastleSieges.put(storageKey,
                new ClassicCastleSiege(key(source), castleId, defender));
    }

    synchronized String classicCastleDefender(String source, int castleId) {
        ClassicCastleSiege siege = classicCastleSieges.get(
                castleSiegeKey(source, castleId));
        return siege == null ? "" : siege.defenderClan;
    }

    synchronized ClassicCastleClaim claimClassicCastleCrown(
            String source, int mapId, long crownObjectId,
            String clanName, String leaderName, long leaderObjectId)
            throws IOException {
        ClassicWorldCastleWarField.WarObject crown =
                classicCastleWarField.object(source, mapId, crownObjectId);
        if (crown == null
                || crown.kind() != ClassicWorldCastleWarField.Kind.CROWN) return null;
        GameStateStore.CastleOwnershipState ownership = null;
        if (accounts != null) {
            RepositoryCastleOwnershipStatus stored;
            try {
                stored = accounts.repositoryForClassicSource(source)
                        .persistCastleOwnership(leaderObjectId, crown.castleId());
            } catch (Exception failure) {
                throw new IOException("castle ownership SQL mutation failed", failure);
            }
            if (stored != RepositoryCastleOwnershipStatus.SUCCESS) {
                System.err.println("[LiTo] castle ownership SQL rejected source="
                        + source + " castle=" + crown.castleId()
                        + " leader=" + leaderObjectId + " status=" + stored);
                return null;
            }
        } else if (state != null) {
            ownership = state.transferCastleOwnership(
                    source, crown.castleId(), clanName,
                    leaderName, leaderObjectId);
        }
        ClassicWorldCastleWarField.Transition transition =
                classicCastleWarField.claimCrown(source, mapId, crownObjectId);
        if (transition == null) {
            throw new IOException("castle crown vanished during atomic claim");
        }
        ClassicCastleSiegeResult siege = classicCastleSiegeParticipant(
                source, crown.castleId(), clanName)
                ? winClassicCastleSiege(source, crown.castleId(), clanName)
                : siegeResult(ClassicCastleSiegeMutation.NOT_PARTICIPANT,
                        crown.castleId(), "", Set.of(), List.of());
        return new ClassicCastleClaim(ownership, transition, siege);
    }

    private static ClassicCastleSiegeResult siegeResult(
            ClassicCastleSiegeMutation mutation, int castleId,
            String defender, Set<String> attackers,
            List<ClassicCastleWarNotice> notices) {
        return new ClassicCastleSiegeResult(
                mutation, castleId, defender, attackers, notices);
    }

    private static String castleSiegeKey(String source, int castleId) {
        return key(source) + "#" + castleId;
    }

    private void classicCastleWarTickSafely() {
        try {
            classicCastleWarTick(System.currentTimeMillis());
        } catch (RuntimeException failure) {
            System.err.println("[LiTo] classic castle-war clock failed: " + failure);
        }
    }

    /** One deterministic clock step; the scheduler supplies wall time in production. */
    void classicCastleWarTick(long nowMillis) {
        List<CastleWarWork> work = new ArrayList<>();
        synchronized (this) {
            for (Map.Entry<String, ClassicWorldCastleWarClock> entry
                    : classicCastleWarClocks.entrySet()) {
                for (ClassicWorldCastleWarClock.Event event
                        : entry.getValue().tick(nowMillis)) {
                    work.add(new CastleWarWork(entry.getKey(), event));
                }
            }
        }
        for (CastleWarWork item : work) {
            publishClassicCastleWarEvents(item.source(), List.of(item.event()));
        }
    }

    private void publishClassicCastleWarEvents(
            String source, List<ClassicWorldCastleWarClock.Event> events) {
        if (events == null || events.isEmpty()) return;
        for (ClassicWorldCastleWarClock.Event event : events) {
            List<ClientSession> recipients;
            synchronized (this) {
                recipients = connectedSessions.stream()
                        .filter(session -> session.usesClassicSource(source))
                        .toList();
            }
            int repaired;
            ClassicWorldCastleWarField.Transition fieldTransition;
            int expelled = 0;
            int securityExpelled = 0;
            if (event.kind() == ClassicWorldCastleWarClock.EventKind.BEGIN) {
                Set<ClientSession> preSiegeSecurityExpelled =
                        settleClassicCastleSecurity(
                                source, event, recipients);
                securityExpelled = preSiegeSecurityExpelled.size();
                // ServerWarExecutor order: SpawnTrebuchet/SpawnFlag, repair,
                // MSG_WAR_BEGIN, then expel every non-defending non-GM PC.
                fieldTransition = classicCastleWarField.begin(source, event.castleId());
                deliverClassicCastleWarField(
                        source, event, fieldTransition, recipients, "begin-spawn");
                repaired = repairClassicCastleDoors(event.castleId());
                deliverClassicCastleWarDoors(source, event, repaired, recipients);
                deliverClassicCastleWarEvent(source, event, repaired, recipients);
                List<ClassicWorldCastleWarField.Point> returnPoints =
                        ClassicWorldCastleWarField.returnPoints(event.castleId());
                for (ClientSession recipient : recipients) {
                    if (preSiegeSecurityExpelled.contains(recipient)) continue;
                    try {
                        ClassicWorldCastleWarField.Point destination = returnPoints.get(
                                ThreadLocalRandom.current().nextInt(returnPoints.size()));
                        if (recipient.serverClassicCastleWarExpel(event, destination)) {
                            expelled++;
                        }
                    } catch (IOException failure) {
                        System.err.println("[LiTo] castle-war expulsion failed source="
                                + source + " castle=" + event.castleId()
                                + " player=" + recipient.playerName()
                                + " error=" + failure.getMessage());
                    }
                }
            } else {
                // The retained END message precedes field cleanup, tower repair
                // and gate repair.  Preserve that observable packet order.
                repaired = 0;
                deliverClassicCastleWarEvent(source, event, repaired, recipients);
                ClassicCastleSiegeResult siegeEnd = ceaseClassicCastleSiege(
                        source, event.castleId());
                deliverClassicCastleWarNotices(
                        source, siegeEnd.notices(), recipients);
                fieldTransition = classicCastleWarField.end(source, event.castleId());
                deliverClassicCastleWarField(
                        source, event, fieldTransition, recipients, "end-cleanup");
                repaired = repairClassicCastleDoors(event.castleId());
                deliverClassicCastleWarDoors(source, event, repaired, recipients);
            }
            if (event.kind() == ClassicWorldCastleWarClock.EventKind.END
                    && state != null) {
                long nextWarTimeMillis = classicCastleWarStartMillis(
                        source, event.castleId(), 0);
                try {
                    if (accounts != null) {
                        AccountRepository repository = accounts
                                .repositoryForClassicSource(source);
                        RepositoryCastleSnapshot expected = repository
                                .castleState(event.castleId());
                        RepositoryCastleSnapshot replacement =
                                new RepositoryCastleSnapshot(true,
                                        expected.castleId(), 10, 0,
                                        nextWarTimeMillis,
                                        expected.securityEnabled(),
                                        expected.mercenaries(),
                                        expected.snapshotVersion());
                        RepositoryCastleMutation sql = repository
                                .persistCastleWarEnd(expected, replacement);
                        if (sql.status() != RepositoryCastleMutationStatus.SUCCESS) {
                            System.err.println("[LiTo] classic castle-war SQL rejected source="
                                    + source + " castle=" + event.castleId()
                                    + " status=" + sql.status());
                            continue;
                        }
                    }
                    GameStateStore.CastleState durable = state.applyCastleWarEnd(
                            source, event.castleId(), nextWarTimeMillis);
                    System.out.println("[LiTo] classic castle-war sidecar source="
                            + source + " castle=" + event.castleId()
                            + " next-start=" + durable.warTimeMillis()
                            + " tax=" + durable.taxRate()
                            + " public-money=" + durable.publicMoney()
                            + " revision=" + durable.revision());
                } catch (Exception failure) {
                    System.err.println("[LiTo] classic castle-war sidecar failed source="
                            + source + " castle=" + event.castleId()
                            + " next-start=" + nextWarTimeMillis
                            + " error=" + failure.getMessage());
                }
            }
            System.out.println("[LiTo] classic castle-war "
                    + event.kind().name().toLowerCase(Locale.ROOT)
                    + " source=" + source + " castle=" + event.castleId()
                    + " start=" + event.startMillis() + " end=" + event.endMillis()
                    + " repaired-doors=" + repaired
                    + " field-spawned=" + fieldTransition.spawned().size()
                    + " field-removed=" + fieldTransition.removed().size()
                    + " security-expelled=" + securityExpelled
                    + " expelled=" + expelled
                    + " recipients=" + recipients.size());
        }
    }

    /**
     * Executes the official CastleS service immediately before the retained
     * BEGIN ordering. SQL/sidecar preparation charges once and remains READY
     * until this idempotent portal effect is marked complete.
     */
    private Set<ClientSession> settleClassicCastleSecurity(
            String source, ClassicWorldCastleWarClock.Event event,
            List<ClientSession> recipients) {
        if (state == null || event == null || event.castleId() > 7) {
            return Set.of();
        }
        boolean ready = false;
        boolean sqlAuthority = false;
        try {
            RepositoryCastleSecuritySettlement sql =
                    RepositoryCastleSecuritySettlement.unavailable();
            AccountRepository repository = null;
            if (accounts != null) {
                repository = accounts.repositoryForClassicSource(source);
                sql = repository.prepareCastleSecurity(
                        event.castleId(), event.startMillis(),
                        CASTLE_SECURITY_CHARGE_ADENA);
            }
            if (sql.status()
                    != RepositoryCastleSecuritySettlementStatus.UNAVAILABLE) {
                sqlAuthority = true;
                if (sql.snapshot().authoritative()) {
                    state.reconcileSqlCastle(source, sql.snapshot());
                }
                ready = sql.status()
                        == RepositoryCastleSecuritySettlementStatus.READY;
                if (!ready) {
                    System.out.println("[LiTo] castle security pre-siege source="
                            + source + " castle=" + event.castleId()
                            + " start=" + event.startMillis()
                            + " status=" + sql.status()
                            + " charged=" + sql.chargedAdena());
                    return Set.of();
                }
            } else {
                GameStateStore.CastleSecuritySettlement local =
                        state.prepareCastleSecurity(
                                source, event.castleId(), event.startMillis(),
                                CASTLE_SECURITY_CHARGE_ADENA);
                ready = local.status()
                        == GameStateStore.CastleSecuritySettlementStatus.READY;
                if (!ready) {
                    System.out.println("[LiTo] castle security pre-siege source="
                            + source + " castle=" + event.castleId()
                            + " start=" + event.startMillis()
                            + " status=" + local.status()
                            + " charged=" + local.chargedAdena());
                    return Set.of();
                }
            }

            Set<ClientSession> expelled = new LinkedHashSet<>();
            List<ClassicWorldCastleWarField.Point> returnPoints =
                    ClassicWorldCastleWarField.returnPoints(event.castleId());
            for (ClientSession recipient : recipients) {
                try {
                    ClassicWorldCastleWarField.Point destination =
                            returnPoints.get(ThreadLocalRandom.current()
                                    .nextInt(returnPoints.size()));
                    if (recipient.serverClassicCastleWarExpel(event, destination)) {
                        expelled.add(recipient);
                    }
                } catch (IOException failure) {
                    System.err.println("[LiTo] castle security expulsion failed source="
                            + source + " castle=" + event.castleId()
                            + " player=" + recipient.playerName()
                            + " error=" + failure.getMessage());
                }
            }

            if (sqlAuthority) {
                RepositoryCastleSecuritySettlement completed =
                        repository.completeCastleSecurity(
                                event.castleId(), event.startMillis());
                if (completed.status()
                        != RepositoryCastleSecuritySettlementStatus.ALREADY_SETTLED) {
                    System.err.println("[LiTo] castle security completion rejected source="
                            + source + " castle=" + event.castleId()
                            + " status=" + completed.status());
                }
            } else {
                GameStateStore.CastleSecuritySettlement completed =
                        state.completeCastleSecurity(
                                source, event.castleId(), event.startMillis());
                if (completed.status()
                        != GameStateStore.CastleSecuritySettlementStatus
                                .ALREADY_SETTLED) {
                    System.err.println("[LiTo] castle security completion rejected source="
                            + source + " castle=" + event.castleId()
                            + " status=" + completed.status());
                }
            }
            System.out.println("[LiTo] castle security executed source="
                    + source + " castle=" + event.castleId()
                    + " start=" + event.startMillis()
                    + " charge=" + CASTLE_SECURITY_CHARGE_ADENA
                    + " expelled=" + expelled.size());
            return Set.copyOf(expelled);
        } catch (Exception failure) {
            System.err.println("[LiTo] castle security settlement failed source="
                    + source + " castle=" + event.castleId()
                    + " start=" + event.startMillis()
                    + " error=" + failure.getMessage());
            return Set.of();
        }
    }

    private static void deliverClassicCastleWarEvent(
            String source, ClassicWorldCastleWarClock.Event event, int repaired,
            List<ClientSession> recipients) {
        for (ClientSession recipient : recipients) {
            try {
                recipient.serverClassicCastleWarEvent(event, repaired);
            } catch (IOException failure) {
                System.err.println("[LiTo] castle-war event delivery failed source="
                        + source + " castle=" + event.castleId()
                        + " player=" + recipient.playerName()
                        + " error=" + failure.getMessage());
            }
        }
    }

    private static void deliverClassicCastleWarDoors(
            String source, ClassicWorldCastleWarClock.Event event, int repaired,
            List<ClientSession> recipients) {
        if (repaired <= 0) return;
        for (ClientSession recipient : recipients) {
            try {
                recipient.serverClassicCastleWarDoorState(event, repaired);
            } catch (IOException failure) {
                System.err.println("[LiTo] castle-war door delivery failed source="
                        + source + " castle=" + event.castleId()
                        + " player=" + recipient.playerName()
                        + " error=" + failure.getMessage());
            }
        }
    }

    private static void deliverClassicCastleWarField(
            String source, ClassicWorldCastleWarClock.Event event,
            ClassicWorldCastleWarField.Transition transition,
            List<ClientSession> recipients, String reason) {
        if (transition.removed().isEmpty() && transition.spawned().isEmpty()) return;
        for (ClientSession recipient : recipients) {
            try {
                recipient.serverClassicCastleWarFieldTransition(
                        transition, reason + "-castle-" + event.castleId());
            } catch (IOException failure) {
                System.err.println("[LiTo] castle-war field delivery failed source="
                        + source + " castle=" + event.castleId()
                        + " player=" + recipient.playerName()
                        + " error=" + failure.getMessage());
            }
        }
    }

    static void deliverClassicCastleWarNotices(
            String source, List<ClassicCastleWarNotice> notices,
            List<ClientSession> recipients) {
        if (notices == null || notices.isEmpty()) return;
        for (ClassicCastleWarNotice notice : notices) {
            for (ClientSession recipient : recipients) {
                try {
                    recipient.serverClassicCastleWarNotice(notice);
                } catch (IOException failure) {
                    System.err.println("[LiTo] castle-war notice failed source="
                            + source + " player=" + recipient.playerName()
                            + " error=" + failure.getMessage());
                }
            }
        }
    }

    List<ClassicWorldHeineCastleDoorState.Snapshot> heineCastleDoors() {
        return heineCastleDoors.snapshots();
    }

    List<ClassicWorldHeineCastleDoorState.Snapshot> setHeineCastleDoorsOpen(
            int keeperNpcId, boolean open) {
        return heineCastleDoors.setOpenForKeeper(keeperNpcId, open);
    }

    boolean heineCastleDoorBlocks(int mapId, int x, int y) {
        return mapId == ClassicWorldHeineCatalog.CASTLE_EXTERIOR_MAP_ID
                && heineCastleDoors.blocks(x, y);
    }

    synchronized boolean changeClassicWorldNpcHeading(
            ClassicWorldNpcCatalog.StaticNpc npc, int heading) {
        if (npc == null || ClassicWorldNpcCatalog.npc(
                npc.mapId(), npc.objectId()) == null || heading < 0 || heading > 7) {
            return false;
        }
        classicWorldNpcHeadings.put(npc.objectId(), heading);
        return true;
    }

    List<ClassicWorldMonsterPopulation.Snapshot> classicWorldMonstersInScreen(
            int mapId, int x, int y) {
        return classicWorldMonstersInScreen(mapId, -1, x, y);
    }

    List<ClassicWorldMonsterPopulation.Snapshot> classicWorldMonstersInScreen(
            int mapId, int instanceId, int x, int y) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of() : population.inScreen(mapId, x, y);
    }

    List<ClassicWorldMonsterPopulation.Reveal> revealClassicWorldSinkMonsters(
            int mapId, int instanceId, int x, int y, long playerObjectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.revealSinkInScreen(
                        mapId, x, y, playerObjectId);
    }

    ClassicWorldTrapPopulation.Detection detectClassicWorldTraps(
            int mapId, int instanceId, int x, int y, long nowNanos) {
        // Retained spawnlist_trap is a single ordinary-world registry and has
        // no per-showId rows. Quest instances therefore never fabricate traps.
        if (instanceId >= 0) {
            return new ClassicWorldTrapPopulation.Detection(
                    List.of(), List.of());
        }
        return classicWorldTraps.detect(mapId, x, y, nowNanos);
    }

    List<ClassicWorldGroundItems.Item> classicWorldGroundItemsInScreen(
            int mapId, int x, int y) {
        return classicWorldGroundItems.inScreen(mapId, x, y);
    }

    List<ClassicWorldGroundItems.Item> classicWorldGroundItemsInScreen(
            int mapId, int instanceId, int x, int y) {
        return classicWorldGroundItems.inScreen(mapId, instanceId, x, y);
    }

    ClassicWorldGroundItems.Item classicWorldGroundItem(int mapId, long objectId) {
        return classicWorldGroundItems.item(mapId, objectId);
    }

    ClassicWorldGroundItems.Item classicWorldGroundItem(
            int mapId, int instanceId, long objectId) {
        return classicWorldGroundItems.item(mapId, instanceId, objectId);
    }

    ClassicWorldGroundItems.Item putClassicWorldGroundItem(
            int mapId, ClassicWorldDropCatalog.ItemWire item, int quantity,
            int x, int y, int sourceNpcId) {
        return classicWorldGroundItems.put(
                mapId, item, quantity, x, y, sourceNpcId);
    }

    ClassicWorldGroundItems.Item putClassicWorldGroundItem(
            int mapId, int instanceId, ClassicWorldDropCatalog.ItemWire item,
            int quantity, int x, int y, int sourceNpcId) {
        return classicWorldGroundItems.put(
                mapId, instanceId, item, quantity, x, y, sourceNpcId);
    }

    ClassicWorldGroundItems.Item putClassicWorldGroundItem(
            int mapId, int instanceId, InventoryCatalog.Definition item,
            int quantity, int x, int y, int sourceNpcId) {
        return classicWorldGroundItems.put(
                mapId, instanceId, item, quantity, x, y, sourceNpcId);
    }

    ClassicWorldGroundItems.Item putClassicWorldGroundItem(
            int mapId, RuntimeInventory.Item item, int quantity,
            int x, int y, int sourceNpcId) {
        return classicWorldGroundItems.put(
                mapId, item, quantity, x, y, sourceNpcId);
    }

    ClassicWorldGroundItems.Item putClassicWorldGroundItem(
            int mapId, int instanceId, RuntimeInventory.Item item, int quantity,
            int x, int y, int sourceNpcId) {
        return classicWorldGroundItems.put(
                mapId, instanceId, item, quantity, x, y, sourceNpcId);
    }

    ClassicWorldGroundItems.Item putClassicWorldGroundItemPersisted(
            int mapId, RuntimeInventory.Item item, int quantity,
            int x, int y, int sourceNpcId, DurableCommit commit) throws IOException {
        return classicWorldGroundItems.putPersisted(
                mapId, item, quantity, x, y, sourceNpcId, commit);
    }

    ClassicWorldGroundItems.Item putClassicWorldGroundItemPersisted(
            int mapId, int instanceId, RuntimeInventory.Item item, int quantity,
            int x, int y, int sourceNpcId, DurableCommit commit) throws IOException {
        return classicWorldGroundItems.putPersisted(
                mapId, instanceId, item, quantity, x, y, sourceNpcId, commit);
    }

    ClassicGroundTake takeClassicWorldGroundItem(
            int mapId, long objectId, long quantity) {
        ClassicWorldGroundItems.Take take =
                classicWorldGroundItems.take(mapId, objectId, quantity);
        return take == null ? null : new ClassicGroundTake(
                take.taken(), take.remaining());
    }

    ClassicGroundTake takeClassicWorldGroundItem(
            int mapId, int instanceId, long objectId, long quantity) {
        ClassicWorldGroundItems.Take take =
                classicWorldGroundItems.take(mapId, instanceId, objectId, quantity);
        return take == null ? null : new ClassicGroundTake(
                take.taken(), take.remaining());
    }

    ClassicGroundTake takeClassicWorldGroundItemPersisted(
            int mapId, long objectId, long quantity, DurableCommit commit)
            throws IOException {
        ClassicWorldGroundItems.Take take = classicWorldGroundItems.takePersisted(
                mapId, objectId, quantity, commit);
        return take == null ? null : new ClassicGroundTake(
                take.taken(), take.remaining());
    }

    ClassicGroundTake takeClassicWorldGroundItemPersisted(
            int mapId, int instanceId, long objectId, long quantity,
            DurableCommit commit) throws IOException {
        ClassicWorldGroundItems.Take take = classicWorldGroundItems.takePersisted(
                mapId, instanceId, objectId, quantity, commit);
        return take == null ? null : new ClassicGroundTake(
                take.taken(), take.remaining());
    }

    ClassicWorldMonsterPopulation.Snapshot classicWorldMonster(
            int mapId, long objectId) {
        return classicWorldMonster(mapId, -1, objectId);
    }

    ClassicWorldMonsterPopulation.Snapshot classicWorldMonster(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation.Snapshot actor =
                classicWorldMonsterIncludingHidden(
                        mapId, instanceId, objectId);
        return actor == null
                || actor.hiddenStatus()
                    != ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE
                    && actor.hiddenStatus()
                        != ClassicWorldMonsterPopulation.HIDDEN_STATUS_ICE
                ? null : actor;
    }

    ClassicWorldMonsterPopulation.Snapshot classicWorldMonsterIncludingHidden(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.actor(mapId, objectId);
    }

    ClassicWorldMonsterPopulation.Snapshot faceAndRestClassicWorldGuardian(
            int mapId, int instanceId, long objectId,
            int targetX, int targetY, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.faceAndRest(
                mapId, objectId, targetX, targetY, nowNanos,
                ClassicWorldGuardianCatalog.TALK_REST_SECONDS);
    }

    ClassicWorldMonsterPopulation.Capture captureClassicWorldMonster(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.captureTamed(mapId, objectId);
    }

    ClassicWorldMonsterPopulation.TamedRelease releaseClassicWorldTamedMonster(
            int mapId, int instanceId, long objectId,
            BattleCompanion.Snapshot companion) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.releaseTamed(mapId, objectId, companion);
    }

    ClassicWorldMonsterPopulation.Snapshot abandonClassicWorldTamedMonster(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        ClassicWorldMonsterPopulation.Snapshot abandoned = population == null
                ? null : population.abandonTamed(mapId, objectId);
        if (abandoned != null) {
            int delaySeconds = population.nextRespawnDelaySeconds(mapId, objectId);
            if (delaySeconds >= 0) {
                battleClock.schedule(() -> respawnClassicWorldMonsterSafely(
                        mapId, instanceId, objectId), delaySeconds, TimeUnit.SECONDS);
            }
        }
        return abandoned;
    }

    ClassicWorldMonsterPopulation.Resurrection resurrectClassicWorldMonster(
            int mapId, int instanceId, long objectId, int restoredHp) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.resurrect(mapId, objectId, restoredHp);
    }

    ClassicWorldMonsterPopulation.Snapshot consumeClassicWorldMonsterCorpse(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        if (population == null) return null;
        ClassicWorldMonsterPopulation.Snapshot consumed =
                population.removeCorpse(mapId, objectId);
        if (consumed == null) return null;
        int delaySeconds = population.nextRespawnDelaySeconds(mapId, objectId);
        if (delaySeconds >= 0) {
            battleClock.schedule(() -> respawnClassicWorldMonsterSafely(
                    mapId, instanceId, objectId), delaySeconds, TimeUnit.SECONDS);
        }
        return consumed;
    }

    ClassicWorldMonsterPopulation.Snapshot reserveReleasedPet(
            int mapId, BattleCompanion.Snapshot pet, SourcePetTemplate source,
            List<RuntimeInventory.Item> carriedItems) {
        return reserveReleasedPet(mapId, -1, pet, source, carriedItems);
    }

    ClassicWorldMonsterPopulation.Snapshot reserveReleasedPet(
            int mapId, int instanceId, BattleCompanion.Snapshot pet,
            SourcePetTemplate source, List<RuntimeInventory.Item> carriedItems) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.reserveReleasedPet(
                mapId, pet, source, carriedItems);
    }

    ClassicWorldMonsterPopulation.Snapshot commitReleasedPet(long objectId) {
        return classicWorldMonsters.commitReleasedPet(objectId);
    }

    ClassicWorldMonsterPopulation.Snapshot commitReleasedPet(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.commitReleasedPet(objectId);
    }

    boolean cancelReleasedPet(long objectId) {
        return classicWorldMonsters.cancelReleasedPet(objectId);
    }

    boolean cancelReleasedPet(int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.cancelReleasedPet(objectId);
    }

    List<RuntimeInventory.Item> takeReleasedPetItems(int mapId, long objectId) {
        return classicWorldMonsters.takeReleasedPetItems(mapId, objectId);
    }

    List<RuntimeInventory.Item> takeReleasedPetItems(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.takeReleasedPetItems(mapId, objectId);
    }

    List<RuntimeInventory.Item> takeMonsterCarriedItems(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.takeMonsterCarriedItems(mapId, objectId);
    }

    NpcGiveResult canGiveToClassicWorldMonster(
            int mapId, int instanceId, long targetObjectId,
            int targetX, int targetY, RuntimeInventory.Item item, long quantity) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? NpcGiveResult.UNKNOWN_TARGET
                : mapGiveAdmission(population.canReceiveGivenItem(
                        mapId, targetObjectId, targetX, targetY, item, quantity));
    }

    NpcGiveResult giveToClassicWorldMonsterPersisted(
            int mapId, int instanceId, long targetObjectId,
            int targetX, int targetY, RuntimeInventory.Item item, long quantity,
            DurableCommit commit) throws IOException {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        if (population == null) return NpcGiveResult.UNKNOWN_TARGET;
        NpcGiveResult admission = mapGiveAdmission(population.canReceiveGivenItem(
                mapId, targetObjectId, targetX, targetY, item, quantity));
        if (admission != NpcGiveResult.ACCEPTED) return admission;
        ClassicWorldMonsterPopulation.MonsterItemReceipt receipt =
                population.receiveGivenItemPersisted(mapId, targetObjectId,
                        targetX, targetY, item, quantity, commit);
        return receipt == null ? NpcGiveResult.UNKNOWN_TARGET
                : NpcGiveResult.ACCEPTED;
    }

    int classicWorldMonsterLightRange(
            int mapId, int instanceId, long targetObjectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? 0
                : population.lightRange(mapId, targetObjectId);
    }

    NpcGiveResult canGiveToClassicWorldStaticNpc(
            int mapId, int instanceId, long targetObjectId,
            int targetX, int targetY, RuntimeInventory.Item item, long quantity) {
        return mapGiveAdmission(classicWorldStaticNpcInventories.canReceiveGivenItem(
                mapId, instanceId, targetObjectId, targetX, targetY, item, quantity));
    }

    NpcGiveResult giveToClassicWorldStaticNpcPersisted(
            int mapId, int instanceId, long targetObjectId,
            int targetX, int targetY, RuntimeInventory.Item item, long quantity,
            DurableCommit commit) throws IOException {
        NpcGiveResult admission = canGiveToClassicWorldStaticNpc(
                mapId, instanceId, targetObjectId,
                targetX, targetY, item, quantity);
        if (admission != NpcGiveResult.ACCEPTED) return admission;
        ClassicWorldStaticNpcInventories.Receipt receipt =
                classicWorldStaticNpcInventories.receiveGivenItemPersisted(
                        mapId, instanceId, targetObjectId, targetX, targetY,
                        item, quantity, commit);
        return receipt == null ? NpcGiveResult.UNKNOWN_TARGET
                : NpcGiveResult.ACCEPTED;
    }

    int classicWorldStaticNpcLightRange(
            int mapId, int instanceId, long targetObjectId) {
        return classicWorldStaticNpcInventories.lightRange(
                mapId, instanceId, targetObjectId);
    }

    List<RuntimeInventory.Item> classicWorldStaticNpcCarriedItems(
            int mapId, int instanceId, long targetObjectId) {
        return classicWorldStaticNpcInventories.carriedItems(
                mapId, instanceId, targetObjectId);
    }

    ClassicWorldMonsterPopulation.TamedItemReceipt
            giveToClassicWorldMonsterAndCapturePersisted(
                    int mapId, int instanceId, long targetObjectId,
                    int targetX, int targetY, RuntimeInventory.Item item,
                    long quantity,
                    ClassicWorldMonsterPopulation.MonsterTameCommit commit)
                    throws IOException {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        if (population == null) return null;
        return population.receiveGivenItemAndCapturePersisted(
                mapId, targetObjectId, targetX, targetY,
                item, quantity, commit);
    }

    private static NpcGiveResult mapGiveAdmission(
            ClassicWorldMonsterPopulation.GiveAdmission admission) {
        return switch (admission) {
            case ACCEPTED -> NpcGiveResult.ACCEPTED;
            case UNKNOWN_TARGET -> NpcGiveResult.UNKNOWN_TARGET;
            case STALE_POSITION -> NpcGiveResult.STALE_POSITION;
            case CAPACITY -> NpcGiveResult.CAPACITY;
        };
    }

    private static NpcGiveResult mapGiveAdmission(
            ClassicWorldStaticNpcInventories.GiveAdmission admission) {
        return switch (admission) {
            case ACCEPTED -> NpcGiveResult.ACCEPTED;
            case UNKNOWN_TARGET -> NpcGiveResult.UNKNOWN_TARGET;
            case STALE_POSITION -> NpcGiveResult.STALE_POSITION;
            case CAPACITY -> NpcGiveResult.CAPACITY;
        };
    }

    boolean classicWorldMonsterOccupies(int mapId, int x, int y) {
        return classicWorldMonsterOccupies(mapId, -1, x, y);
    }

    boolean classicWorldMonsterOccupies(
            int mapId, int instanceId, int x, int y) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.occupies(mapId, x, y);
    }

    ClassicWorldMonsterPopulation.Hit damageClassicWorldMonster(
            int mapId, long objectId, int damage) {
        return damageClassicWorldMonster(mapId, -1, objectId, damage);
    }

    ClassicWorldMonsterPopulation.Healing healClassicWorldMonster(
            int mapId, int instanceId, long objectId, int amount) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.heal(mapId, objectId, amount);
    }

    ClassicWorldMonsterPopulation.ManaDrain drainClassicWorldMonsterMana(
            int mapId, int instanceId, long objectId, int requestedDrain) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.drainMana(mapId, objectId, requestedDrain);
    }

    int recoverClassicWorldMonsterMana(
            int mapId, int instanceId, long objectId, int amount) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? 0
                : population.recoverMana(mapId, objectId, amount);
    }

    ClassicWorldMonsterPopulation.ManaDrain
            drainClassicWorldMonsterManaIfStrictlyAbove(
                    int mapId, int instanceId, long objectId,
                    int requestedDrain) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.drainManaIfStrictlyAbove(
                        mapId, objectId, requestedDrain);
    }

    ClassicWorldMonsterPopulation.Hit damageClassicWorldMonster(
            int mapId, int instanceId, long objectId, int damage) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.damage(mapId, objectId, damage);
    }

    ClassicWorldMonsterPopulation.Hit damageClassicWorldMonster(
            int mapId, long objectId, long playerObjectId, int damage) {
        return damageClassicWorldMonster(
                mapId, -1, objectId, playerObjectId, damage);
    }

    ClassicWorldMonsterPopulation.Hit damageClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long playerObjectId, int damage) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.damage(mapId, objectId, playerObjectId, damage);
    }

    ClassicWorldMonsterPopulation.Hit damageClassicWorldMonster(
            int mapId, long objectId,
            ClassicWorldMonsterPopulation.ContributorKind kind,
            long sourceObjectId, long playerObjectId, int damage) {
        return damageClassicWorldMonster(mapId, -1, objectId, kind,
                sourceObjectId, playerObjectId, damage);
    }

    ClassicWorldMonsterPopulation.Hit damageClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            ClassicWorldMonsterPopulation.ContributorKind kind,
            long sourceObjectId, long playerObjectId, int damage) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.damage(
                mapId, objectId, kind, sourceObjectId, playerObjectId, damage);
    }

    boolean poisonClassicWorldMonster(
            int mapId, int instanceId, long objectId, long attackerObjectId,
            long nowNanos, int durationSeconds, int tickIntervalMillis) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.poison(
                mapId, objectId, attackerObjectId, nowNanos,
                durationSeconds, tickIntervalMillis);
    }

    ClassicWorldMonsterPopulation.NpcPoison poisonClassicWorldMonsterFromNpc(
            int mapId, int instanceId, long objectId,
            ClassicWorldMonsterPopulation.ContributorKind contributorKind,
            long attackerObjectId, long playerObjectId,
            ClassicWorldMonsterPoisonAttack.Kind kind, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.npcPoison(
                mapId, objectId, contributorKind, attackerObjectId,
                playerObjectId, kind, nowNanos);
    }

    boolean classicWorldMonsterPoisonActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null
                && population.poisonActive(mapId, objectId, nowNanos);
    }

    boolean paralyzeClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationMillis) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.paralysis(
                mapId, objectId, nowNanos, durationMillis);
    }

    boolean classicWorldMonsterParalysisActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null
                && population.paralysisActive(mapId, objectId, nowNanos);
    }

    boolean freezeClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.freeze(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterFreezeActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null
                && population.freezeActive(mapId, objectId, nowNanos);
    }

    boolean cancelClassicWorldMonsterFreeze(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.cancelFreeze(mapId, objectId);
    }

    boolean sleepClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.sleep(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterSleepActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null
                && population.sleepActive(mapId, objectId, nowNanos);
    }

    boolean cancelClassicWorldMonsterSleep(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.cancelSleep(mapId, objectId);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterFreezes(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null
                ? List.of() : population.expireFreezes(mapId, nowNanos);
    }

    boolean earthBindClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.earthBind(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterEarthBindActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null
                && population.earthBindActive(mapId, objectId, nowNanos);
    }

    boolean cancelClassicWorldMonsterEarthBind(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.cancelEarthBind(mapId, objectId);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterEarthBinds(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null
                ? List.of() : population.expireEarthBinds(mapId, nowNanos);
    }

    BattleTimedStatus.ShockStunActivation shockStunClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int rolledSeconds, int maximumSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.shockStun(
                mapId, objectId, nowNanos, rolledSeconds, maximumSeconds);
    }

    boolean classicWorldMonsterShockStunActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.shockStunActive(
                mapId, objectId, nowNanos);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterShockStuns(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.expireShockStuns(mapId, nowNanos);
    }

    boolean boneBreakClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.boneBreak(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterBoneBreakActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.boneBreakActive(
                mapId, objectId, nowNanos);
    }

    BattleArmBreakerStatus.Activation armBreakerClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null
                ? BattleArmBreakerStatus.Activation.rejected()
                : population.armBreaker(
                        mapId, objectId, nowNanos, durationSeconds);
    }

    int classicWorldMonsterArmBreakerMeleeDamageModifier(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? 0
                : population.armBreakerMeleeDamageModifier(
                        mapId, objectId, nowNanos);
    }

    boolean cancelClassicWorldMonsterArmBreaker(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null
                && population.cancelArmBreaker(mapId, objectId);
    }

    boolean panicClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds, int abilityScoreModifier) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.panic(
                mapId, objectId, nowNanos, durationSeconds,
                abilityScoreModifier);
    }

    int classicWorldMonsterPanicAbilityScoreModifier(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? 0
                : population.panicAbilityScoreModifier(
                        mapId, objectId, nowNanos);
    }

    boolean cancelClassicWorldMonsterPanic(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.cancelPanic(mapId, objectId);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterPanics(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.expirePanics(mapId, nowNanos);
    }

    BattleJoyOfPainStatus.Activation joyOfPainClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.joyOfPain(
                mapId, objectId, nowNanos, durationSeconds);
    }

    BattleJoyOfPainStatus.Lease captureClassicWorldMonsterJoyOfPain(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null : population.captureJoyOfPain(
                mapId, objectId, nowNanos);
    }

    boolean consumeClassicWorldMonsterJoyOfPain(
            int mapId, int instanceId, long objectId,
            BattleJoyOfPainStatus.Lease lease) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.consumeJoyOfPain(
                mapId, objectId, lease);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterBoneBreaks(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.expireBoneBreaks(mapId, nowNanos);
    }

    boolean thunderGrabClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.thunderGrab(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterThunderGrabActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.thunderGrabActive(
                mapId, objectId, nowNanos);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterThunderGrabs(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.expireThunderGrabs(mapId, nowNanos);
    }

    boolean horrorOfDeathClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.horrorOfDeath(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterHorrorOfDeathActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.horrorOfDeathActive(
                mapId, objectId, nowNanos);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterHorrorOfDeaths(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.expireHorrorOfDeaths(mapId, nowNanos);
    }

    synchronized BattleShockStunEffects.Placement
            placeClassicWorldShockStunEffect(
                    int mapId, int instanceId, long targetObjectId,
                    int x, int y, long nowNanos, int durationSeconds) {
        if (mapId < 0 || instanceId < -1) return null;
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleShockStunEffects effects = classicWorldShockStunEffects
                .computeIfAbsent(key, ignored -> new BattleShockStunEffects());
        effects.expire(nowNanos);
        return effects.place(
                targetObjectId, x, y, nowNanos, durationSeconds);
    }

    synchronized List<BattleShockStunEffects.Effect>
            expireClassicWorldShockStunEffects(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleShockStunEffects effects = classicWorldShockStunEffects.get(key);
        if (effects == null) return List.of();
        List<BattleShockStunEffects.Effect> expired = effects.expire(nowNanos);
        if (effects.active(nowNanos).isEmpty()) {
            classicWorldShockStunEffects.remove(key);
        }
        return expired;
    }

    synchronized BattleBoneBreakEffects.Effect
            placeClassicWorldBoneBreakEffect(
                    int mapId, int instanceId, long targetObjectId,
                    int x, int y, long nowNanos, int durationSeconds) {
        if (mapId < 0 || instanceId < -1) return null;
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleBoneBreakEffects effects = classicWorldBoneBreakEffects
                .computeIfAbsent(key, ignored -> new BattleBoneBreakEffects());
        effects.expire(nowNanos);
        return effects.placeIfAbsent(
                targetObjectId, x, y, nowNanos, durationSeconds);
    }

    synchronized List<BattleBoneBreakEffects.Effect>
            boneBreakEffects(int mapId, int instanceId, long nowNanos) {
        BattleBoneBreakEffects effects = classicWorldBoneBreakEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null ? List.of() : effects.active(nowNanos);
    }

    synchronized List<BattleBoneBreakEffects.Effect>
            expireClassicWorldBoneBreakEffects(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleBoneBreakEffects effects = classicWorldBoneBreakEffects.get(key);
        if (effects == null) return List.of();
        List<BattleBoneBreakEffects.Effect> expired = effects.expire(nowNanos);
        if (effects.active(nowNanos).isEmpty()) {
            classicWorldBoneBreakEffects.remove(key);
        }
        return expired;
    }

    synchronized BattleBoneBreakEffects.Effect
            removeClassicWorldBoneBreakEffect(
                    int mapId, int instanceId, long targetObjectId) {
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleBoneBreakEffects effects = classicWorldBoneBreakEffects.get(key);
        if (effects == null) return null;
        BattleBoneBreakEffects.Effect removed = effects.remove(targetObjectId);
        if (effects.active(0).isEmpty()) {
            classicWorldBoneBreakEffects.remove(key);
        }
        return removed;
    }

    synchronized BattleFreezeEffects.Placement placeClassicWorldFreezeEffect(
            int mapId, int instanceId, long targetObjectId,
            int x, int y, long nowNanos, int durationSeconds) {
        if (mapId < 0 || instanceId < -1) return null;
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleFreezeEffects effects = classicWorldFreezeEffects
                .computeIfAbsent(key, ignored -> new BattleFreezeEffects());
        effects.expire(nowNanos);
        return effects.place(
                targetObjectId, x, y, nowNanos, durationSeconds);
    }

    synchronized List<BattleFreezeEffects.Effect> classicWorldFreezeEffects(
            int mapId, int instanceId, long nowNanos) {
        BattleFreezeEffects effects = classicWorldFreezeEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null ? List.of() : effects.active(nowNanos);
    }

    synchronized List<BattleFreezeEffects.Effect> expireClassicWorldFreezeEffects(
            int mapId, int instanceId, long nowNanos) {
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleFreezeEffects effects = classicWorldFreezeEffects.get(key);
        if (effects == null) return List.of();
        List<BattleFreezeEffects.Effect> expired = effects.expire(nowNanos);
        if (effects.active(nowNanos).isEmpty()) {
            classicWorldFreezeEffects.remove(key);
        }
        return expired;
    }

    synchronized BattleFreezeEffects.Effect removeClassicWorldFreezeEffect(
            int mapId, int instanceId, long targetObjectId) {
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleFreezeEffects effects = classicWorldFreezeEffects.get(key);
        if (effects == null) return null;
        BattleFreezeEffects.Effect removed = effects.remove(targetObjectId);
        if (effects.active(System.nanoTime()).isEmpty()) {
            classicWorldFreezeEffects.remove(key);
        }
        return removed;
    }

    synchronized BattleCubeIgnitionEffects.Placement
            placeClassicWorldCubeIgnition(
                    ClientSession caster, int mapId, int instanceId,
                    int x, int y, long nowNanos) {
        ClassicWorldLocation location = classicWorldMembership.get(caster);
        if (caster == null || location == null || location.mapId() != mapId
                || location.instanceId() != instanceId
                || location.x() != x || location.y() != y
                || !ClassicWorldMapCatalog.forMap(mapId).standingPassable(x, y)) {
            return BattleCubeIgnitionEffects.Placement.rejected();
        }
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleCubeIgnitionEffects effects = classicWorldCubeIgnitionEffects
                .computeIfAbsent(key, ignored -> new BattleCubeIgnitionEffects());
        return effects.place(caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeIgnitionEffects.Cube>
            classicWorldCubeIgnitions(
                    int mapId, int instanceId, long nowNanos) {
        BattleCubeIgnitionEffects effects = classicWorldCubeIgnitionEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null ? List.of() : effects.active(nowNanos);
    }

    synchronized int classicWorldCubeIgnitionFireResistance(
            int mapId, int instanceId, ClientSession target, long nowNanos) {
        BattleCubeIgnitionEffects effects = classicWorldCubeIgnitionEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null || target == null ? 0
                : effects.fireResistance(target.playerObjectId(), nowNanos);
    }

    synchronized BattleCubeQuakeEffects.Placement placeClassicWorldCubeQuake(
            ClientSession caster, int mapId, int instanceId,
            int x, int y, long nowNanos) {
        ClassicWorldLocation location = classicWorldMembership.get(caster);
        if (caster == null || location == null || location.mapId() != mapId
                || location.instanceId() != instanceId
                || location.x() != x || location.y() != y
                || !ClassicWorldMapCatalog.forMap(mapId).standingPassable(x, y)) {
            return BattleCubeQuakeEffects.Placement.rejected();
        }
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleCubeQuakeEffects effects = classicWorldCubeQuakeEffects
                .computeIfAbsent(key, ignored -> new BattleCubeQuakeEffects());
        return effects.place(caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeQuakeEffects.Cube> classicWorldCubeQuakes(
            int mapId, int instanceId, long nowNanos) {
        BattleCubeQuakeEffects effects = classicWorldCubeQuakeEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null ? List.of() : effects.active(nowNanos);
    }

    synchronized int classicWorldCubeQuakeEarthResistance(
            int mapId, int instanceId, ClientSession target, long nowNanos) {
        BattleCubeQuakeEffects effects = classicWorldCubeQuakeEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null || target == null ? 0
                : effects.earthResistance(target.playerObjectId(), nowNanos);
    }

    synchronized boolean classicWorldCubeQuakeMovementStopped(
            int mapId, int instanceId, ClientSession target, long nowNanos) {
        BattleCubeQuakeEffects effects = classicWorldCubeQuakeEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects != null && target != null
                && effects.movementStopped(target.playerObjectId(), nowNanos);
    }

    synchronized boolean classicWorldCubeQuakeMonsterMovementStopped(
            int mapId, int instanceId, long objectId, long nowNanos) {
        BattleCubeQuakeEffects effects = classicWorldCubeQuakeEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects != null && effects.movementStopped(objectId, nowNanos);
    }

    synchronized BattleCubeShockEffects.Placement placeClassicWorldCubeShock(
            ClientSession caster, int mapId, int instanceId,
            int x, int y, long nowNanos) {
        ClassicWorldLocation location = classicWorldMembership.get(caster);
        if (caster == null || location == null || location.mapId() != mapId
                || location.instanceId() != instanceId
                || location.x() != x || location.y() != y
                || !ClassicWorldMapCatalog.forMap(mapId).standingPassable(x, y)) {
            return BattleCubeShockEffects.Placement.rejected();
        }
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleCubeShockEffects effects = classicWorldCubeShockEffects
                .computeIfAbsent(key, ignored -> new BattleCubeShockEffects());
        return effects.place(caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeShockEffects.Cube> classicWorldCubeShocks(
            int mapId, int instanceId, long nowNanos) {
        BattleCubeShockEffects effects = classicWorldCubeShockEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null ? List.of() : effects.active(nowNanos);
    }

    synchronized int classicWorldCubeShockWindResistance(
            int mapId, int instanceId, ClientSession target, long nowNanos) {
        BattleCubeShockEffects effects = classicWorldCubeShockEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null || target == null ? 0
                : effects.windResistance(target.playerObjectId(), nowNanos);
    }

    synchronized int classicWorldCubeShockMagicResistanceModifier(
            int mapId, int instanceId, long targetObjectId, long nowNanos) {
        BattleCubeShockEffects effects = classicWorldCubeShockEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null ? 0
                : effects.magicResistanceModifier(targetObjectId, nowNanos);
    }

    synchronized BattleCubeHarmonyEffects.Placement placeClassicWorldCubeHarmony(
            ClientSession caster, int mapId, int instanceId,
            int x, int y, long nowNanos) {
        ClassicWorldLocation location = classicWorldMembership.get(caster);
        if (caster == null || location == null || location.mapId() != mapId
                || location.instanceId() != instanceId
                || location.x() != x || location.y() != y
                || !ClassicWorldMapCatalog.forMap(mapId).standingPassable(x, y)) {
            return BattleCubeHarmonyEffects.Placement.rejected();
        }
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattleCubeHarmonyEffects effects = classicWorldCubeHarmonyEffects
                .computeIfAbsent(key, ignored -> new BattleCubeHarmonyEffects());
        return effects.place(caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeHarmonyEffects.Cube> classicWorldCubeHarmonies(
            int mapId, int instanceId, long nowNanos) {
        BattleCubeHarmonyEffects effects = classicWorldCubeHarmonyEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects == null ? List.of() : effects.active(nowNanos);
    }

    synchronized boolean classicWorldCubeHarmonyStatus(
            int mapId, int instanceId, long targetObjectId, long nowNanos) {
        BattleCubeHarmonyEffects effects = classicWorldCubeHarmonyEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return effects != null && effects.hasStatus(targetObjectId, nowNanos);
    }

    synchronized BattlePatienceStatus.Activation activateClassicWorldPatience(
            int mapId, int instanceId, long targetObjectId, long nowNanos,
            int durationSeconds) {
        if (mapId <= 0 || targetObjectId <= 0 || nowNanos < 0) {
            return BattlePatienceStatus.Activation.rejected();
        }
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        return classicWorldPatienceStatuses
                .computeIfAbsent(key, ignored -> new BattlePatienceStatus())
                .activate(targetObjectId, nowNanos, durationSeconds);
    }

    synchronized boolean classicWorldPatienceActive(
            int mapId, int instanceId, long targetObjectId, long nowNanos) {
        BattlePatienceStatus status = classicWorldPatienceStatuses.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return status != null && status.active(targetObjectId, nowNanos);
    }

    synchronized int classicWorldPatiencePhysicalReduction(
            int mapId, int instanceId, ClientSession target, long nowNanos) {
        BattlePatienceStatus status = classicWorldPatienceStatuses.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return status == null || target == null ? 0
                : status.physicalReduction(target.playerObjectId(), nowNanos);
    }

    synchronized boolean cancelClassicWorldPatience(
            int mapId, int instanceId, long targetObjectId) {
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        BattlePatienceStatus status = classicWorldPatienceStatuses.get(key);
        if (status == null) return false;
        boolean cancelled = status.cancel(targetObjectId);
        if (status.activeCount(System.nanoTime()) == 0) {
            classicWorldPatienceStatuses.remove(key);
        }
        return cancelled;
    }

    BattleTimedStatus.ParalysisCancellation
            cureClassicWorldMonsterParalysis(
                    int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null
                ? new BattleTimedStatus.ParalysisCancellation(false, false)
                : population.cureParalysis(mapId, objectId);
    }

    boolean cureClassicWorldMonsterPoison(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.curePoison(mapId, objectId);
    }

    int classicWorldMonsterPoisonPalette(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? 0
                : population.poisonPalette(mapId, objectId, nowNanos);
    }

    List<ClassicWorldMonsterPopulation.Snapshot>
            expireClassicWorldMonsterParalyses(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null
                ? List.of() : population.expireParalyses(mapId, nowNanos);
    }

    List<ClassicWorldMonsterPopulation.ParalysisUpdate>
            pollClassicWorldMonsterParalyses(
                    int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null
                ? List.of() : population.pollParalyses(mapId, nowNanos);
    }

    List<ClassicWorldMonsterPopulation.PoisonUpdate>
            pollClassicWorldMonsterPoisons(
                    int mapId, int instanceId, long nowNanos,
                    int tickIntervalMillis, int tickDamage) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of() : population.pollPoisons(
                mapId, nowNanos, tickIntervalMillis, tickDamage);
    }

    boolean provokeClassicWorldMonster(
            int mapId, long objectId, long playerObjectId) {
        return provokeClassicWorldMonster(
                mapId, -1, objectId, playerObjectId);
    }

    boolean provokeClassicWorldMonster(
            int mapId, int instanceId, long objectId, long playerObjectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null
                && population.provoke(mapId, objectId, playerObjectId);
    }

    /** Spawns NPC80011 and applies retained {@code mob.setLink(dspy)}. */
    ClassicWorldMonsterPopulation.Snapshot spawnClassicWorldDspyAssassin(
            ClassicWorldDspyEscorts.AssassinSpawn request) {
        if (request == null
                || request.mapId() != ClassicWorldDspyCatalog.MAP_ID) return null;
        ClassicWorldDspyEscorts.Snapshot linked =
                classicWorldDspyEscorts.find(request.linkedObjectId());
        if (linked == null || !linked.alive() || !linked.owned()
                || linked.mapId() != request.mapId()) return null;
        ClassicWorldMonsterPopulation.Snapshot spawned =
                classicWorldMonsters.spawnQuestMonsterAt(
                        request.mapId(), ClassicWorldDspyCatalog.ASSASSIN_NPC_ID,
                        request.x(), request.y(), 0,
                        ClassicWorldDspyCatalog.ASSASSIN);
        if (spawned == null) return null;
        if (!classicWorldMonsters.provoke(
                request.mapId(), spawned.objectId(), request.linkedObjectId())) {
            throw new IllegalStateException("cannot link Dspy assassin");
        }
        return spawned;
    }

    int addWeaponBreakStackClassicWorldMonster(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? -1
                : population.addWeaponBreakStack(mapId, objectId);
    }

    int classicWorldMonsterWeaponBreakStacks(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? 0
                : population.weaponBreakStacks(mapId, objectId);
    }

    boolean elementalFallDownClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds, int sourceElfAttribute) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.elementalFallDown(
                mapId, objectId, nowNanos, durationSeconds,
                sourceElfAttribute);
    }

    int classicWorldMonsterElementalFallDownModifier(
            int mapId, int instanceId, long objectId,
            int attributeKind, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? 0 : population.elementalFallDownModifier(
                mapId, objectId, attributeKind, nowNanos);
    }

    boolean areaSilenceClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.areaSilence(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterAreaSilenceActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.areaSilenceActive(
                mapId, objectId, nowNanos);
    }

    boolean windShackleClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.windShackle(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterWindShackleActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.windShackleActive(
                mapId, objectId, nowNanos);
    }

    boolean polluteWaterClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.polluteWater(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterPolluteWaterActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.polluteWaterActive(
                mapId, objectId, nowNanos);
    }

    boolean strikerGaleClassicWorldMonster(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.strikerGale(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterStrikerGaleActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.strikerGaleActive(
                mapId, objectId, nowNanos);
    }

    boolean activateClassicWorldMonsterArmorBreak(
            int mapId, int instanceId, long objectId,
            long nowNanos, int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.armorBreak(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterArmorBreakActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.armorBreakActive(
                mapId, objectId, nowNanos);
    }

    boolean eraseMagicClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.eraseMagic(
                mapId, objectId, nowNanos, durationSeconds);
    }

    int classicWorldMonsterEffectiveMagicResistance(
            int mapId, int instanceId, long objectId,
            int baseMagicResistance, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        int effective = population == null ? baseMagicResistance
                : population.effectiveMagicResistance(
                        mapId, objectId, baseMagicResistance, nowNanos);
        return Math.max(0, effective
                + classicWorldCubeShockMagicResistanceModifier(
                        mapId, instanceId, objectId, nowNanos));
    }

    boolean consumeClassicWorldMonsterEraseMagic(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.consumeEraseMagic(
                mapId, objectId, nowNanos);
    }

    BattleEraseMagicStatus.Lease captureClassicWorldMonsterEraseMagic(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? BattleEraseMagicStatus.Lease.none()
                : population.captureEraseMagic(mapId, objectId, nowNanos);
    }

    boolean consumeClassicWorldMonsterEraseMagic(
            int mapId, int instanceId, long objectId,
            BattleEraseMagicStatus.Lease lease) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.consumeEraseMagic(
                mapId, objectId, lease);
    }

    boolean slowClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.slow(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean hasteClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.haste(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterHasteActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.hasteActive(
                mapId, objectId, nowNanos);
    }

    boolean cancelClassicWorldMonsterHaste(
            int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.cancelHaste(mapId, objectId);
    }

    boolean blindClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.blind(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterBlindActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.blindActive(
                mapId, objectId, nowNanos);
    }

    boolean weaknessClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.weakness(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean diseaseClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.disease(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean silenceClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.silence(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean confusionSilenceClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.confusionSilence(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean decayPotionClassicWorldMonster(
            int mapId, int instanceId, long objectId, long nowNanos,
            int durationSeconds) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.decayPotion(
                mapId, objectId, nowNanos, durationSeconds);
    }

    boolean classicWorldMonsterWeaknessActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.weaknessActive(
                mapId, objectId, nowNanos);
    }

    boolean classicWorldMonsterDiseaseActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.diseaseActive(
                mapId, objectId, nowNanos);
    }

    boolean classicWorldMonsterSilenceActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.silenceActive(
                mapId, objectId, nowNanos);
    }

    boolean classicWorldMonsterDecayPotionActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.decayPotionActive(
                mapId, objectId, nowNanos);
    }

    ClassicWorldMonsterPopulation.MagicCancellation
            cancelClassicWorldMonsterMagic(
                    int mapId, int instanceId, long objectId) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? null
                : population.cancelMagicEffects(mapId, objectId);
    }

    List<ClassicWorldMonsterPopulation.Snapshot> expireClassicWorldMonsterHastes(
            int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.expireHastes(mapId, nowNanos);
    }

    boolean classicWorldMonsterSlowActive(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population != null && population.slowActive(
                mapId, objectId, nowNanos);
    }

    int classicWorldMonsterMoveSpeed(
            int mapId, int instanceId, long objectId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        if (population == null) return BattleTimedStatus.NORMAL;
        if (population.hasteActive(mapId, objectId, nowNanos)) {
            return BattleTimedStatus.HASTE;
        }
        return population.slowActive(mapId, objectId, nowNanos)
                ? BattleTimedStatus.SLOW : BattleTimedStatus.NORMAL;
    }

    List<ClassicWorldMonsterPopulation.Snapshot> expireClassicWorldMonsterSlows(
            int mapId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation population =
                classicWorldMonsterPopulation(mapId, instanceId);
        return population == null ? List.of()
                : population.expireSlows(mapId, nowNanos);
    }

    void clearClassicWorldMonsterTargets(long playerObjectId) {
        classicWorldMonsters.clearTargetsForPlayer(playerObjectId);
        synchronized (this) {
            for (ClassicWorldMonsterPopulation population
                    : classicWorldCkewMonsters.values()) {
                population.clearTargetsForPlayer(playerObjectId);
            }
            for (ClassicWorldMonsterPopulation population
                    : classicWorldWaterDragonMonsters.values()) {
                population.clearTargetsForPlayer(playerObjectId);
            }
        }
    }

    void transferClassicWorldMonsterTargets(
            long sourceObjectId, long destinationObjectId) {
        classicWorldMonsters.transferTargets(sourceObjectId, destinationObjectId);
        synchronized (this) {
            for (ClassicWorldMonsterPopulation population
                    : classicWorldCkewMonsters.values()) {
                population.transferTargets(sourceObjectId, destinationObjectId);
            }
            for (ClassicWorldMonsterPopulation population
                    : classicWorldWaterDragonMonsters.values()) {
                population.transferTargets(sourceObjectId, destinationObjectId);
            }
        }
    }

    private synchronized ClassicWorldMonsterPopulation
            classicWorldMonsterPopulation(int mapId, int instanceId) {
        if (mapId == ClassicWorldCkewCatalog.MAP_ID && instanceId >= 0) {
            return classicWorldCkewMonsters.get(instanceId);
        }
        if (mapId == ClassicWorldWaterDragonCatalog.MAP_ID && instanceId > 0) {
            return classicWorldWaterDragonMonsters.get(instanceId);
        }
        return classicWorldMonsters;
    }

    void scheduleClassicWorldMonsterLifecycle(
            ClassicWorldMonsterPopulation.Snapshot dead) {
        scheduleClassicWorldMonsterLifecycle(-1, dead);
    }

    void scheduleClassicWorldMonsterLifecycle(
            int instanceId, ClassicWorldMonsterPopulation.Snapshot dead) {
        if (dead == null || dead.alive() || !dead.present()) return;
        battleClock.schedule(() -> removeClassicWorldMonsterCorpseSafely(
                dead.mapId(), instanceId, dead.objectId()), 1, TimeUnit.SECONDS);
    }

    private void removeClassicWorldMonsterCorpseSafely(
            int mapId, int instanceId, long objectId) {
        try {
            ClassicWorldMonsterPopulation population =
                    classicWorldMonsterPopulation(mapId, instanceId);
            if (population == null) return;
            ClassicWorldMonsterPopulation.Snapshot removed =
                    population.removeCorpse(mapId, objectId);
            if (removed == null) return;
            for (ClientSession member : classicWorldMembers(mapId, instanceId)) {
                member.serverClassicWorldMonsterRemoved(removed);
            }
            int delaySeconds = population.nextRespawnDelaySeconds(
                    mapId, objectId);
            if (delaySeconds >= 0) {
                battleClock.schedule(() -> respawnClassicWorldMonsterSafely(
                        mapId, instanceId, objectId), delaySeconds, TimeUnit.SECONDS);
            }
        } catch (Exception error) {
            System.out.println("[LiTo] classic-world monster removal failed object="
                    + objectId + " reason=" + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
        }
    }

    private void respawnClassicWorldMonsterSafely(
            int mapId, int instanceId, long objectId) {
        try {
            ClassicWorldMonsterPopulation population =
                    classicWorldMonsterPopulation(mapId, instanceId);
            if (population == null) return;
            ClassicWorldMonsterPopulation.Respawn respawn =
                    population.respawn(mapId, objectId);
            if (respawn == null) return;
            for (ClientSession member : classicWorldMembers(mapId, instanceId)) {
                member.serverClassicWorldMonsterRespawned(respawn.after());
                for (ClassicWorldMonsterPopulation.Snapshot minion
                        : respawn.groupMembers()) {
                    member.serverClassicWorldMonsterRespawned(minion);
                }
            }
        } catch (Exception error) {
            System.out.println("[LiTo] classic-world monster respawn failed object="
                    + objectId + " reason=" + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
        }
    }

    synchronized List<RoomSnapshot> roomSnapshots(int channelId) {
        Channel channel = requiredChannel(channelId);
        ensureDefaultRoom(channel);
        return channel.roomIds.stream().map(rooms::get)
                .filter(java.util.Objects::nonNull)
                .map(GameHub::snapshot)
                .toList();
    }

    /**
     * Native /argo enters pseudo-channel 99 and exposes only running rooms
     * that opted into observation.  S_CC itself has no channel field, so room
     * ids remain globally unique and carry the actual channel here for the
     * authoritative cross-channel join.
     */
    synchronized List<RoomSnapshot> argoRoomSnapshots() {
        return rooms.values().stream()
                .filter(room -> room.battleStarted && room.settings.allowsObservers())
                .map(GameHub::snapshot)
                .toList();
    }

    synchronized RoomSnapshot argoRoomSnapshot(long roomId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && room.settings.allowsObservers()
                ? snapshot(room) : null;
    }

    synchronized ChannelSnapshot argoChannelSnapshot() {
        return new ChannelSnapshot(99, "Argo", tournamentPresence.userCount(),
                CHANNEL_CAPACITY * CHANNEL_COUNT);
    }

    private static RoomSnapshot snapshot(Room room) {
        return new RoomSnapshot(room.id, room.channelId, room.settings,
                room.members.size() + room.virtualPlayers.size(), room.combatantCount(),
                room.battleStarted);
    }

    synchronized List<ClientSession> roomMembers(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? List.of() : new ArrayList<>(room.members);
    }

    synchronized List<ClientSession> channelMembers(int channelId) {
        return tournamentPresence.members(channelId).stream().map(nativePresencePeers::get)
                .filter(java.util.Objects::nonNull).toList();
    }

    TournamentPresence tournamentPresence() { return tournamentPresence; }

    private Integer nativeChannel(ClientSession session) {
        return tournamentPresence.channelOf(nativePresenceOwners.get(session));
    }

    /** Native-only packet recipients derived from the shared authoritative membership. */
    private List<ClientSession> nativeChannelMembers() {
        return tournamentPresence.channelMembers().stream().map(nativePresencePeers::get)
                .filter(java.util.Objects::nonNull).toList();
    }

    synchronized int tournamentUserCount() {
        return tournamentPresence.userCount();
    }

    synchronized ClientSession channelMemberByName(int channelId, String name) {
        Channel channel = channels.get(channelId);
        if (channel == null || name == null) return null;
        return uniqueNamedMember(channelMembers(channelId), null, name);
    }

    synchronized ClientSession channelMemberByName(
            int channelId, ClientSession requester, String name) {
        Channel channel = channels.get(channelId);
        if (channel == null || requester == null || name == null) return null;
        return uniqueNamedMember(channelMembers(channelId), requester, name);
    }

    synchronized ClientSession activeMemberByName(String name) {
        if (name == null) return null;
        return uniqueNamedMember(nativeChannelMembers(), null, name);
    }

    synchronized ClientSession activeMemberByName(ClientSession requester, String name) {
        if (requester == null || name == null) return null;
        return uniqueNamedMember(nativeChannelMembers(), requester, name);
    }

    synchronized ClientSession onlineMemberByName(
            ClientSession requester, String name) {
        if (requester == null || name == null) return null;
        return uniqueNamedMember(selectedCharacters.keySet(), requester, name);
    }

    synchronized ClientSession activeMemberByIdentity(String sourceCharacterIdentity) {
        if (sourceCharacterIdentity == null || sourceCharacterIdentity.isBlank()) return null;
        List<ClientSession> matches = nativeChannelMembers().stream()
                .filter(member -> member.sourceCharacterIdentity()
                        .equalsIgnoreCase(sourceCharacterIdentity))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    /**
     * C_SILENCE carries seconds after the native client converts the entered
     * minute count with {@code value * 60}.  A zero duration is the only
     * native-wire way to remove a named silence without waiting for expiry.
     */
    synchronized long setChatSilence(String name, int durationSeconds, long nowNanos) {
        String normalized = key(name);
        if (normalized.isEmpty() || durationSeconds < 0 || durationSeconds > 0x7D00) {
            throw new IllegalArgumentException("invalid native named silence");
        }
        if (durationSeconds == 0) {
            chatSilencedUntilNanos.remove(normalized);
            return 0;
        }
        long expiresAt = Math.addExact(nowNanos,
                TimeUnit.SECONDS.toNanos(durationSeconds));
        chatSilencedUntilNanos.put(normalized, expiresAt);
        return expiresAt;
    }

    synchronized boolean chatSilenced(String name, long nowNanos) {
        String normalized = key(name);
        Long expiresAt = chatSilencedUntilNanos.get(normalized);
        if (expiresAt == null) return false;
        if (nowNanos - expiresAt >= 0) {
            chatSilencedUntilNanos.remove(normalized);
            return false;
        }
        return true;
    }

    synchronized BuddyData buddyData(String owner, int channelId) {
        BuddyProfile profile = buddyProfile(owner);
        // Keep the development guide from the earlier deterministic profile,
        // then expose real online channel peers to the native buddy panel.
        BuddyEntry guide = profile.entries.computeIfAbsent(key("ArenaGuide"),
                ignored -> new BuddyEntry("ArenaGuide", 1, 1,
                        Protocol.LitoBuddyActivity.WAITING, "Local", 1,
                        0, "", false));
        guide.activity = Protocol.LitoBuddyActivity.WAITING;
        Channel channel = channels.get(channelId);
        if (channel != null) {
            String ownerSource = identitySourceKey(owner);
            for (ClientSession member : channelMembers(channelId)) {
                String memberIdentity = member.sourceCharacterIdentity();
                if (memberIdentity.equalsIgnoreCase(owner)) continue;
                if (!ownerSource.isEmpty()
                        && !ownerSource.equals(identitySourceKey(memberIdentity))) continue;
                Protocol.LitoBuddyActivity activity = member.buddyActivityState();
                BuddyEntry entry = profile.entries.computeIfAbsent(key(member.playerName()),
                        ignored -> new BuddyEntry(member.playerName(), 1, 1,
                                activity, member.originServerName(), 1,
                                0, identitySourceKey(memberIdentity), false));
                entry.activity = activity;
                entry.origin = member.originServerName();
            }
        }
        return buddySnapshot(profile);
    }

    synchronized BuddyAddResult addBuddy(String owner, String requestedName,
                                          String origin, boolean online) {
        return addBuddy(owner, requestedName, origin, "", online);
    }

    synchronized BuddyAddResult addBuddy(String owner, String requestedName,
                                          String origin, String targetSource,
                                          boolean online) {
        BuddyProfile profile = buddyProfile(owner);
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty() || name.length() > 45) return BuddyAddResult.INVALID;
        BuddyEntry existing = profile.entries.get(key(name));
        if (existing != null && existing.durable) return BuddyAddResult.DUPLICATE;
        if (existing == null && profile.entries.size() >= MAX_BUDDIES) {
            return BuddyAddResult.FULL;
        }
        BuddyProfile staged = profile.copy();
        BuddyEntry replacement = existing == null
                ? new BuddyEntry(name, 1, 1,
                    online ? Protocol.LitoBuddyActivity.WAITING
                            : Protocol.LitoBuddyActivity.OFFLINE,
                    origin == null ? "" : origin, 1, 0,
                    targetSource == null ? "" : targetSource, true)
                : existing.copy();
        replacement.durable = true;
        replacement.targetSource = targetSource == null ? "" : targetSource;
        replacement.origin = origin == null ? "" : origin;
        replacement.activity = online ? Protocol.LitoBuddyActivity.WAITING
                : Protocol.LitoBuddyActivity.OFFLINE;
        staged.entries.put(key(name), replacement);
        if (!commitBuddyProfile(owner, staged)) {
            return BuddyAddResult.PERSISTENCE_REJECTED;
        }
        return BuddyAddResult.ADDED;
    }

    synchronized boolean removeBuddy(String owner, String requestedName) {
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty()) return false;
        BuddyProfile profile = buddyProfile(owner);
        BuddyEntry existing = profile.entries.get(key(name));
        if (existing == null) return false;
        BuddyProfile staged = profile.copy();
        staged.entries.remove(key(name));
        if (!existing.durable) {
            buddyProfiles.put(key(owner), staged);
            return true;
        }
        return commitBuddyProfile(owner, staged);
    }

    synchronized int createBuddyGroup(String owner, String requestedName) {
        BuddyProfile profile = buddyProfile(owner);
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty() || name.length() > 45 || profile.groups.size() >= 255
                || hasGroupName(profile, name)) return -1;
        BuddyProfile staged = profile.copy();
        int id = nextBuddyGroupId(staged);
        if (id < 0) return -1;
        staged.nextGroupId = id + 1;
        staged.groups.put(id, name);
        return commitBuddyProfile(owner, staged) ? id : -1;
    }

    synchronized List<String> removeBuddyGroup(String owner, int groupId) {
        BuddyProfile profile = buddyProfile(owner);
        if (groupId == 1 || !profile.groups.containsKey(groupId)) return null;
        BuddyProfile staged = profile.copy();
        staged.groups.remove(groupId);
        List<String> moved = new ArrayList<>();
        for (BuddyEntry entry : staged.entries.values()) {
            if (entry.groupId != groupId) continue;
            entry.groupId = 1;
            moved.add(entry.name);
        }
        return commitBuddyProfile(owner, staged) ? moved : null;
    }

    synchronized boolean renameBuddyGroup(String owner, int groupId, String requestedName) {
        BuddyProfile profile = buddyProfile(owner);
        String name = requestedName == null ? "" : requestedName.trim();
        if (!profile.groups.containsKey(groupId) || name.isEmpty()
                || name.length() > 45) return false;
        boolean duplicate = profile.groups.entrySet().stream()
                .anyMatch(group -> group.getKey() != groupId && group.getValue().equalsIgnoreCase(name));
        if (duplicate) return false;
        BuddyProfile staged = profile.copy();
        staged.groups.put(groupId, name);
        return commitBuddyProfile(owner, staged);
    }

    synchronized boolean changeBuddyGroup(String owner, String buddyName, int groupId) {
        BuddyProfile profile = buddyProfile(owner);
        BuddyEntry entry = profile.entries.get(key(buddyName));
        if (entry == null || !profile.groups.containsKey(groupId)) return false;
        BuddyProfile staged = profile.copy();
        staged.entries.get(key(buddyName)).groupId = groupId;
        if (!entry.durable) {
            buddyProfiles.put(key(owner), staged);
            return true;
        }
        return commitBuddyProfile(owner, staged);
    }

    synchronized void replacePersistentBuddyData(
            String owner, BuddyPersistentData persistent) {
        Objects.requireNonNull(persistent, "persistent buddy data");
        BuddyProfile profile = new BuddyProfile();
        profile.groups.clear();
        int maximumGroupId = 1;
        Set<String> groupNames = new LinkedHashSet<>();
        for (BuddyGroup group : persistent.groups()) {
            if (group == null || group.id() < 1 || group.id() > 255
                    || group.name() == null || group.name().isBlank()
                    || group.name().length() > 45
                    || !groupNames.add(key(group.name()))
                    || profile.groups.putIfAbsent(group.id(), group.name()) != null) {
                throw new IllegalArgumentException("invalid persistent buddy group");
            }
            maximumGroupId = Math.max(maximumGroupId, group.id());
        }
        if (!profile.groups.containsKey(1)) {
            throw new IllegalArgumentException("persistent buddy group 1 is required");
        }
        if (persistent.buddies().size() > MAX_BUDDIES) {
            throw new IllegalArgumentException("persistent buddy capacity exceeded");
        }
        for (BuddyPersistentEntry entry : persistent.buddies()) {
            if (entry == null || entry.name() == null || entry.name().isBlank()
                    || entry.name().length() > 45 || entry.buddyObjectId() < 0
                    || entry.buddyObjectId() > 0xFFFF_FFFFL
                    || entry.targetSource() == null
                    || entry.targetSource().length() > 45
                    || entry.origin() == null || entry.origin().length() > 45
                    || entry.relationshipStyle() < 0
                    || entry.relationshipStyle() > 4
                    || entry.paletteOffset() < 0 || entry.paletteOffset() > 0xC2
                    || !profile.groups.containsKey(entry.groupId())) {
                throw new IllegalArgumentException("invalid persistent buddy entry");
            }
            BuddyEntry loaded = new BuddyEntry(entry.name(), entry.groupId(),
                    entry.relationshipStyle(), Protocol.LitoBuddyActivity.OFFLINE,
                    entry.origin(), entry.paletteOffset(), entry.buddyObjectId(),
                    entry.targetSource(), true);
            if (profile.entries.putIfAbsent(key(entry.name()), loaded) != null) {
                throw new IllegalArgumentException("duplicate persistent buddy entry");
            }
        }
        profile.nextGroupId = Math.min(256, maximumGroupId + 1);
        buddyProfiles.put(key(owner), profile);
    }

    synchronized void registerBuddyPersistence(
            String owner, BuddyPersistenceHook hook) {
        buddyPersistenceHooks.put(key(owner), Objects.requireNonNull(hook, "buddy hook"));
    }

    synchronized void unregisterBuddyPersistence(
            String owner, BuddyPersistenceHook hook) {
        buddyPersistenceHooks.remove(key(owner), hook);
    }

    synchronized boolean setBuddyPresence(String owner, String buddyName, boolean online) {
        BuddyEntry entry = buddyProfile(owner).entries.get(key(buddyName));
        Protocol.LitoBuddyActivity activity = online
                ? Protocol.LitoBuddyActivity.WAITING
                : Protocol.LitoBuddyActivity.OFFLINE;
        if (entry == null || entry.activity == activity) return false;
        entry.activity = activity;
        return true;
    }

    synchronized boolean setBuddyActivity(String owner, String buddyName,
                                           Protocol.LitoBuddyActivity activity) {
        Objects.requireNonNull(activity, "buddy activity");
        BuddyEntry entry = buddyProfile(owner).entries.get(key(buddyName));
        if (entry == null || entry.activity == activity) return false;
        entry.activity = activity;
        return true;
    }

    /** Updates the server-owned fields of one admitted S_F6/S_F7 member row. */
    synchronized boolean setClanRosterState(String characterName,
                                             int relationshipStyle,
                                             int paletteOffset,
                                             String title,
                                             Protocol.LitoClanActivity activity) {
        String characterKey = key(characterName);
        String clanKey = clanMembership.get(characterKey);
        ClanProfile clan = clanKey == null ? null : clanProfiles.get(clanKey);
        ClanMember member = clan == null ? null : clan.members.get(characterKey);
        if (member == null) return false;
        if (relationshipStyle < 0 || relationshipStyle > 4
                || paletteOffset < 0 || paletteOffset > 0xC2) {
            throw new IllegalArgumentException("invalid native clan roster style");
        }
        String normalizedTitle = title == null ? "" : title;
        Objects.requireNonNull(activity, "clan activity");
        boolean changed = member.relationshipStyle != relationshipStyle
                || member.paletteOffset != paletteOffset
                || !member.title.equals(normalizedTitle)
                || member.activity != activity;
        member.relationshipStyle = relationshipStyle;
        member.paletteOffset = paletteOffset;
        member.title = normalizedTitle;
        member.activity = activity;
        return changed;
    }

    /**
     * Imports the character's original clan name into runtime state without
     * changing the legacy database. The SQL schema exposed to this server has
     * no trustworthy Tournament role column, so the first observed character
     * becomes the server-owned native role 4 and later observed members become role 2.
     */
    synchronized ClanData registerOriginalClan(String characterName, String originalClanName) {
        String characterKey = key(characterName);
        String clanName = originalClanName == null ? "" : originalClanName.trim();
        if (characterKey.isEmpty() || clanName.isEmpty()) return clanData(characterName);
        if (clanMembership.containsKey(characterKey)) return clanData(characterName);

        String clanKey = scopedClanKey(characterName, clanName);
        ClanProfile clan = clanProfiles.computeIfAbsent(
                clanKey, ignored -> new ClanProfile(clanKey, clanName));
        ClanMember existing = clan.members.get(characterKey);
        if (existing == null) {
            int role = clan.members.values().stream()
                    .anyMatch(member -> member.role == CLAN_ROLE_OWNER)
                    ? CLAN_ROLE_MEMBER : CLAN_ROLE_OWNER;
            clan.members.put(characterKey, new ClanMember(characterName, role));
        } else if (existing.role == CLAN_ROLE_CANDIDATE) {
            existing.role = clan.members.values().stream()
                    .anyMatch(member -> member != existing && member.role == CLAN_ROLE_OWNER)
                    ? CLAN_ROLE_MEMBER : CLAN_ROLE_OWNER;
        }
        clanMembership.put(characterKey, clanKey);
        return clanSnapshot(clan);
    }

    /** Replaces one source-scoped runtime clan with the selected SQL snapshot. */
    synchronized ClanData reconcileSqlClan(
            String characterIdentity, RepositoryClanSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "SQL clan snapshot");
        if (!snapshot.authoritative()) return clanData(characterIdentity);
        String selectedKey = key(characterIdentity);
        String previousClanKey = clanMembership.get(selectedKey);
        ClanProfile previousClan = previousClanKey == null
                ? null : clanProfiles.get(previousClanKey);
        if (!snapshot.present()) {
            if (previousClan != null) {
                ClanMember previousMember = previousClan.members.get(selectedKey);
                if (previousMember != null && previousMember.role == CLAN_ROLE_OWNER) {
                    previousClan.members.keySet().forEach(memberKey ->
                            clanMembership.remove(memberKey, previousClan.key));
                    clanProfiles.remove(previousClan.key);
                } else {
                    previousClan.members.remove(selectedKey);
                    clanMembership.remove(selectedKey, previousClan.key);
                    if (previousClan.members.isEmpty()) clanProfiles.remove(previousClan.key);
                }
            }
            return ClanData.empty();
        }

        String clanKey = scopedClanKey(characterIdentity, snapshot.name());
        ClanProfile old = clanProfiles.remove(clanKey);
        long retainedEmblemId = old == null ? 0 : old.emblemId;
        if (old != null) {
            old.members.keySet().forEach(memberKey ->
                    clanMembership.remove(memberKey, old.key));
        }
        ClanProfile clan = new ClanProfile(clanKey, snapshot.name());
        clan.destroyPending = snapshot.destroyPending();
        clan.emblemId = retainedEmblemId;
        for (RepositoryClanMember source : snapshot.members()) {
            String identity = scopedMemberIdentity(characterIdentity, source.name());
            String memberKey = key(identity);
            String conflictingKey = clanMembership.get(memberKey);
            if (conflictingKey != null && !conflictingKey.equals(clanKey)) {
                ClanProfile conflicting = clanProfiles.get(conflictingKey);
                if (conflicting != null) {
                    conflicting.members.remove(memberKey);
                    if (conflicting.members.isEmpty()) clanProfiles.remove(conflictingKey);
                }
                clanMembership.remove(memberKey, conflictingKey);
            }
            ClanMember member = new ClanMember(identity, source.role());
            member.title = source.title();
            ClanMember retained = old == null ? null : old.members.get(memberKey);
            if (retained != null) {
                member.relationshipStyle = retained.relationshipStyle;
                member.paletteOffset = retained.paletteOffset;
                member.activity = retained.activity;
            }
            clan.members.put(memberKey, member);
            if (source.role() != CLAN_ROLE_CANDIDATE) {
                clanMembership.put(memberKey, clanKey);
            }
        }
        clanProfiles.put(clanKey, clan);
        return clanSnapshot(clan);
    }

    synchronized ClanData clanData(String characterName) {
        String clanKey = clanMembership.get(key(characterName));
        ClanProfile clan = clanKey == null ? null : clanProfiles.get(clanKey);
        return clan == null ? ClanData.empty() : clanSnapshot(clan);
    }

    synchronized int clanRole(String characterName) {
        String characterKey = key(characterName);
        String clanKey = clanMembership.get(characterKey);
        ClanProfile clan = clanKey == null ? null : clanProfiles.get(clanKey);
        ClanMember member = clan == null ? null : clan.members.get(characterKey);
        return member == null ? 0 : member.role;
    }

    synchronized boolean sameClan(String firstCharacterIdentity,
                                  String secondCharacterIdentity) {
        String firstClanKey = clanMembership.get(key(firstCharacterIdentity));
        String secondClanKey = clanMembership.get(key(secondCharacterIdentity));
        return firstClanKey != null && firstClanKey.equals(secondClanKey);
    }

    synchronized boolean sameClassicParty(String firstCharacterIdentity,
                                          String secondCharacterIdentity) {
        ClassicParty first = classicPartyMembership.get(key(firstCharacterIdentity));
        ClassicParty second = classicPartyMembership.get(key(secondCharacterIdentity));
        return first != null && first == second;
    }

    synchronized boolean claimClanWarehouse(String characterName, ClientSession session,
                                             long expiresAtNanos) {
        if (session == null || expiresAtNanos <= System.nanoTime()) return false;
        String clanKey = clanMembership.get(key(characterName));
        if (clanKey == null) return false;
        ClanWarehouseLease current = clanWarehouseLeases.get(clanKey);
        long now = System.nanoTime();
        if (current != null && current.expiresAtNanos > now && current.session != session) {
            return false;
        }
        clanWarehouseLeases.put(clanKey,
                new ClanWarehouseLease(session, expiresAtNanos));
        return true;
    }

    synchronized void releaseClanWarehouse(String characterName, ClientSession session) {
        String clanKey = clanMembership.get(key(characterName));
        ClanWarehouseLease current = clanKey == null ? null : clanWarehouseLeases.get(clanKey);
        if (current != null && current.session == session) clanWarehouseLeases.remove(clanKey);
    }

    synchronized void releaseClanWarehouse(ClientSession session) {
        clanWarehouseLeases.entrySet().removeIf(entry -> entry.getValue().session == session);
    }

    /** Registers a read-only legacy emblem after the owning clan was imported. */
    synchronized boolean registerClanEmblem(
            String characterIdentity, String clanName, ClanEmblemData emblem) {
        if (emblem == null) return false;
        String clanKey = scopedClanKey(characterIdentity, clanName);
        ClanProfile clan = clanProfiles.get(clanKey);
        if (clan == null || clanKey.isEmpty()) return false;
        ClanEmblem collision = classicClanEmblems.get(emblem.emblemId());
        if (collision != null && !collision.clanKey.equals(clanKey)) return false;

        if (clan.emblemId != 0 && clan.emblemId != emblem.emblemId()) {
            ClanEmblem previous = classicClanEmblems.get(clan.emblemId);
            if (previous != null && previous.clanKey.equals(clanKey)) {
                classicClanEmblems.remove(clan.emblemId);
            }
        }
        clan.emblemId = emblem.emblemId();
        classicClanEmblems.put(emblem.emblemId(),
                new ClanEmblem(clanKey, emblem.pixels()));
        return true;
    }

    synchronized boolean registerClanEmblem(String clanName, ClanEmblemData emblem) {
        List<ClanProfile> matches = clanProfiles.values().stream()
                .filter(clan -> clan.name.equalsIgnoreCase(clanName)).toList();
        if (matches.size() != 1) return false;
        ClanProfile clan = matches.get(0);
        return registerClanEmblem(clan.members.values().stream().findFirst()
                .map(member -> member.identity).orElse(""), clanName, emblem);
    }

    synchronized long clanEmblemId(String characterName) {
        String clanKey = clanMembership.get(key(characterName));
        ClanProfile clan = clanKey == null ? null : clanProfiles.get(clanKey);
        return clan == null ? 0 : clan.emblemId;
    }

    synchronized byte[] clanEmblem(long emblemId) {
        ClanEmblem emblem = classicClanEmblems.get(emblemId);
        return emblem == null ? null : emblem.pixels.clone();
    }

    synchronized boolean createClan(String ownerName, String requestedName) {
        return createClassicClan(ownerName, requestedName) == ClassicClanCreateStatus.CREATED;
    }

    synchronized ClassicClanCreateStatus createClassicClan(String ownerName, String requestedName) {
        ClassicClanCreateStatus status = classicClanCreateStatus(ownerName, requestedName);
        if (status != ClassicClanCreateStatus.CREATED) return status;
        String ownerKey = key(ownerName);
        String clanName = requestedName == null ? "" : requestedName.trim();
        String clanKey = scopedClanKey(ownerName, clanName);
        ClanProfile clan = new ClanProfile(clanKey, clanName);
        clan.members.put(ownerKey, new ClanMember(ownerName, CLAN_ROLE_OWNER));
        clanProfiles.put(clanKey, clan);
        clanMembership.put(ownerKey, clanKey);
        return ClassicClanCreateStatus.CREATED;
    }

    synchronized ClassicClanCreateStatus classicClanCreateStatus(
            String ownerName, String requestedName) {
        String ownerKey = key(ownerName);
        String clanName = requestedName == null ? "" : requestedName.trim();
        if (ownerKey.isEmpty() || clanName.isEmpty()) return ClassicClanCreateStatus.INVALID;
        if (clanMembership.containsKey(ownerKey)) return ClassicClanCreateStatus.OWNER_ALREADY_MEMBER;
        return clanProfiles.containsKey(scopedClanKey(ownerName, clanName))
                ? ClassicClanCreateStatus.DUPLICATE_NAME : ClassicClanCreateStatus.CREATED;
    }

    synchronized void rollbackClassicClanCreation(String ownerName, String clanName) {
        String ownerKey = key(ownerName);
        String clanKey = scopedClanKey(ownerName, clanName);
        ClanProfile clan = clanProfiles.get(clanKey);
        if (clan == null || clan.members.size() != 1) return;
        ClanMember owner = clan.members.get(ownerKey);
        if (owner == null || owner.role != CLAN_ROLE_OWNER
                || !clanKey.equals(clanMembership.get(ownerKey))) return;
        clanMembership.remove(ownerKey);
        clanProfiles.remove(clanKey);
    }

    /** Standard question-97 transition: absent membership -> ordinary member. */
    synchronized ClassicClanJoinResult joinClassicClan(String ownerName, String applicantName,
                                                        int maximumMembers) {
        ClanProfile clan = ownedClan(ownerName);
        String applicantIdentity = scopedMemberIdentity(ownerName, applicantName);
        String applicantKey = key(applicantIdentity);
        if (clan == null || clan.destroyPending || applicantKey.isEmpty()
                || applicantKey.equals(key(ownerName)) || clanMembership.containsKey(applicantKey)
                || clan.members.containsKey(applicantKey) || maximumMembers <= 0) {
            return new ClassicClanJoinResult(ClassicClanJoinStatus.INVALID, ClanData.empty());
        }
        long actualMembers = clan.members.values().stream()
                .filter(member -> member.role != CLAN_ROLE_CANDIDATE).count();
        if (actualMembers >= maximumMembers) {
            return new ClassicClanJoinResult(ClassicClanJoinStatus.FULL, clanSnapshot(clan));
        }
        clan.members.put(applicantKey, new ClanMember(applicantIdentity, CLAN_ROLE_MEMBER));
        clanMembership.put(applicantKey, clan.key);
        return new ClassicClanJoinResult(ClassicClanJoinStatus.JOINED, clanSnapshot(clan));
    }

    /** Native callback role transition: absent/0 -> C_F5 -> role 3. */
    synchronized ClanMemberSnapshot addClanCandidate(String ownerName, String candidateName) {
        ClanProfile clan = ownedClan(ownerName);
        String candidateIdentity = scopedMemberIdentity(ownerName, candidateName);
        String candidateKey = key(candidateIdentity);
        if (clan == null || candidateKey.isEmpty() || candidateKey.equals(key(ownerName))
                || clanMembership.containsKey(candidateKey) || clan.members.containsKey(candidateKey)) {
            return null;
        }
        ClanMember candidate = new ClanMember(candidateIdentity, CLAN_ROLE_CANDIDATE);
        clan.members.put(candidateKey, candidate);
        return clanMemberSnapshot(candidate);
    }

    /** Native callback role transition: role 3 -> C_F6 -> role 2. */
    synchronized ClanData admitClanCandidate(String ownerName, String candidateName) {
        ClanProfile clan = ownedClan(ownerName);
        String candidateKey = key(scopedMemberIdentity(ownerName, candidateName));
        ClanMember candidate = clan == null ? null : clan.members.get(candidateKey);
        if (candidate == null || candidate.role != CLAN_ROLE_CANDIDATE
                || clanMembership.containsKey(candidateKey)) {
            return null;
        }
        candidate.role = CLAN_ROLE_MEMBER;
        clanMembership.put(candidateKey, clan.key);
        return clanSnapshot(clan);
    }

    /** Native callback role transition: role 2 -> C_F7 -> S_F9 removal. */
    synchronized ClanMemberSnapshot removeClanMember(String ownerName, String memberName) {
        ClanProfile clan = ownedClan(ownerName);
        String memberKey = key(scopedMemberIdentity(ownerName, memberName));
        ClanMember member = clan == null ? null : clan.members.get(memberKey);
        if (member == null || member.role != CLAN_ROLE_MEMBER) return null;
        clan.members.remove(memberKey);
        clanMembership.remove(memberKey, clan.key);
        return clanMemberSnapshot(member);
    }

    /** Native C_RANK_CONTROL command 1: role 4 is immutable, assignments are 1..3. */
    synchronized ClanData changeClanRole(String ownerName, String memberName, int requestedRole) {
        ClanProfile clan = ownedClan(ownerName);
        String memberKey = key(scopedMemberIdentity(ownerName, memberName));
        ClanMember member = clan == null ? null : clan.members.get(memberKey);
        if (member == null || member.role == CLAN_ROLE_OWNER
                || requestedRole < CLAN_ROLE_OFFICER
                || requestedRole > CLAN_ROLE_CANDIDATE) {
            return null;
        }
        String clanKey = clan.key;
        String currentMembership = clanMembership.get(memberKey);
        if (requestedRole == CLAN_ROLE_CANDIDATE) {
            clanMembership.remove(memberKey, clanKey);
        } else {
            if (currentMembership != null && !currentMembership.equals(clanKey)) return null;
            clanMembership.put(memberKey, clanKey);
        }
        member.role = requestedRole;
        return clanSnapshot(clan);
    }

    synchronized boolean setClanDestroyPending(String ownerName, boolean pending) {
        ClanProfile clan = ownedClan(ownerName);
        if (clan == null || clan.destroyPending == pending) return false;
        clan.destroyPending = pending;
        return true;
    }

    synchronized List<ClientSession> clanSessions(String characterName) {
        String clanKey = clanMembership.get(key(characterName));
        if (clanKey == null) return List.of();
        return nativeChannelMembers().stream()
                .filter(session -> clanKey.equals(
                        clanMembership.get(key(session.sourceCharacterIdentity()))))
                .toList();
    }

    /** Retained ClanTable online roster spans channel, battle and WORLD. */
    synchronized List<ClientSession> connectedClanSessions(String characterName) {
        String clanKey = clanMembership.get(key(characterName));
        if (clanKey == null) return List.of();
        return connectedSessions.stream()
                .filter(ClientSession::acceptsLitoRosterBroadcast)
                .filter(session -> clanKey.equals(
                        clanMembership.get(key(session.sourceCharacterIdentity()))))
                .toList();
    }

    /**
     * Validates a native C_WAR proposal without mutating war state.  The
     * returned online leader is only a short-lived routing hint; acceptance
     * always revalidates both leaders and both clan war slots atomically.
     */
    synchronized ClassicClanWarProposal proposeClassicClanWar(
            String requesterName, String requestedTargetClan,
            Protocol.ClanWarRequestType type) {
        if (type != Protocol.ClanWarRequestType.DECLARE
                && type != Protocol.ClanWarRequestType.SURRENDER
                && type != Protocol.ClanWarRequestType.CEASE) {
            return clanWarProposal(ClassicClanWarRequestStatus.UNSUPPORTED, null, null, null);
        }
        ClanProfile requesterClan = ownedClan(requesterName);
        if (requesterClan == null || requesterClan.destroyPending) {
            return clanWarProposal(ClassicClanWarRequestStatus.NOT_OWNER, null, null, null);
        }
        String requesterKey = requesterClan.key;
        String targetKey = scopedClanKey(requesterName, requestedTargetClan);
        ClanProfile targetClan = clanProfiles.get(targetKey);
        if (targetClan == null || targetClan.destroyPending || targetKey.equals(requesterKey)) {
            return clanWarProposal(
                    ClassicClanWarRequestStatus.TARGET_NOT_FOUND, null,
                    requesterClan.name, requestedTargetClan);
        }

        ClassicClanWar requesterWar = classicClanWars.get(requesterKey);
        ClassicClanWar targetWar = classicClanWars.get(targetKey);
        if (type == Protocol.ClanWarRequestType.DECLARE && requesterWar != null) {
            return clanWarProposal(ClassicClanWarRequestStatus.REQUESTER_AT_WAR, null,
                    requesterClan.name, targetClan.name);
        }
        if (type == Protocol.ClanWarRequestType.DECLARE && targetWar != null) {
            return clanWarProposal(ClassicClanWarRequestStatus.TARGET_AT_WAR, null,
                    requesterClan.name, targetClan.name);
        }
        if ((type == Protocol.ClanWarRequestType.SURRENDER
                || type == Protocol.ClanWarRequestType.CEASE) && requesterWar == null) {
            return clanWarProposal(ClassicClanWarRequestStatus.NOT_AT_WAR, null,
                    requesterClan.name, targetClan.name);
        }
        if ((type == Protocol.ClanWarRequestType.SURRENDER
                || type == Protocol.ClanWarRequestType.CEASE) && requesterWar != targetWar) {
            return clanWarProposal(ClassicClanWarRequestStatus.NOT_SAME_WAR, null,
                    requesterClan.name, targetClan.name);
        }

        ClanMember targetLeader = targetClan.members.values().stream()
                .filter(member -> member.role == CLAN_ROLE_OWNER).findFirst().orElse(null);
        ClientSession targetOwner = targetLeader == null ? null
                : nativeChannelMembers().stream()
                        .filter(session -> key(session.sourceCharacterIdentity())
                                .equals(key(targetLeader.identity)))
                        .findFirst().orElse(null);
        if (targetOwner == null) {
            return clanWarProposal(ClassicClanWarRequestStatus.TARGET_OWNER_OFFLINE, null,
                    requesterClan.name, targetClan.name);
        }
        return clanWarProposal(ClassicClanWarRequestStatus.PENDING, targetOwner,
                requesterClan.name, targetClan.name);
    }

    /** Atomic question-217/221/222 transition with ownership and replay checks. */
    synchronized ClassicClanWarResolution resolveClassicClanWar(
            String requesterOwnerName, String targetOwnerName,
            String expectedRequesterClan, String expectedTargetClan,
            Protocol.ClanWarRequestType type, boolean accepted) {
        ClanProfile requesterClan = ownedClan(requesterOwnerName);
        ClanProfile targetClan = ownedClan(targetOwnerName);
        if (requesterClan == null || targetClan == null
                || requesterClan.destroyPending || targetClan.destroyPending
                || !requesterClan.name.equalsIgnoreCase(expectedRequesterClan)
                || !targetClan.name.equalsIgnoreCase(expectedTargetClan)
                || requesterClan == targetClan
                || (type != Protocol.ClanWarRequestType.DECLARE
                    && type != Protocol.ClanWarRequestType.SURRENDER
                    && type != Protocol.ClanWarRequestType.CEASE)) {
            return new ClassicClanWarResolution(
                    ClassicClanWarResolutionStatus.INVALID, ClassicClanWarData.empty());
        }
        ClassicClanWarData data = new ClassicClanWarData(
                requesterClan.name, targetClan.name);
        if (!accepted) {
            return new ClassicClanWarResolution(
                    ClassicClanWarResolutionStatus.REFUSED, data);
        }

        String requesterKey = requesterClan.key;
        String targetKey = targetClan.key;
        ClassicClanWar requesterWar = classicClanWars.get(requesterKey);
        ClassicClanWar targetWar = classicClanWars.get(targetKey);
        if (type == Protocol.ClanWarRequestType.DECLARE) {
            if (requesterWar != null || targetWar != null) {
                return new ClassicClanWarResolution(
                        ClassicClanWarResolutionStatus.INVALID, data);
            }
            ClassicClanWar war = new ClassicClanWar(
                    requesterKey, requesterClan.name, targetKey, targetClan.name);
            classicClanWars.put(requesterKey, war);
            classicClanWars.put(targetKey, war);
            return new ClassicClanWarResolution(
                    ClassicClanWarResolutionStatus.STARTED, data);
        }
        if (requesterWar == null || requesterWar != targetWar) {
            return new ClassicClanWarResolution(
                    ClassicClanWarResolutionStatus.INVALID, data);
        }
        classicClanWars.remove(requesterWar.clanOneKey, requesterWar);
        classicClanWars.remove(requesterWar.clanTwoKey, requesterWar);
        return new ClassicClanWarResolution(
                type == Protocol.ClanWarRequestType.SURRENDER
                        ? ClassicClanWarResolutionStatus.SURRENDERED
                        : ClassicClanWarResolutionStatus.CEASED,
                data);
    }

    synchronized ClassicClanWarData classicClanWarData(String characterName) {
        String clanKey = clanMembership.get(key(characterName));
        ClassicClanWar war = clanKey == null ? null : classicClanWars.get(clanKey);
        return war == null ? ClassicClanWarData.empty()
                : new ClassicClanWarData(war.clanOneName, war.clanTwoName);
    }

    /** True only when both source-qualified characters belong to the same live war. */
    synchronized boolean classicClanWarActiveBetween(
            String firstCharacter, String secondCharacter) {
        String firstClan = clanMembership.get(key(firstCharacter));
        String secondClan = clanMembership.get(key(secondCharacter));
        if (firstClan == null || secondClan == null || firstClan.equals(secondClan)) {
            return false;
        }
        ClassicClanWar firstWar = classicClanWars.get(firstClan);
        return firstWar != null && firstWar == classicClanWars.get(secondClan);
    }

    /**
     * Atomically snapshots the fatal same-war relation and then removes the
     * war when the victim is its clan owner.  The snapshot is required because
     * retained {@code simWarResult} still treats that fatal hit as a war kill
     * after {@code ceaseWar} has deleted the live entry.
     */
    synchronized ClassicClanWarLeaderDeathResolution
            endClassicClanWarForLeaderDeath(
                    String victimOwnerIdentity, String attackerIdentity) {
        ClanProfile victimClan = ownedClan(victimOwnerIdentity);
        if (victimClan == null) {
            return ClassicClanWarLeaderDeathResolution.none();
        }
        ClassicClanWar war = classicClanWars.get(victimClan.key);
        if (war == null) {
            return ClassicClanWarLeaderDeathResolution.none();
        }
        String attackerClanKey = clanMembership.get(key(attackerIdentity));
        boolean attackerSameWar = attackerClanKey != null
                && !attackerClanKey.equals(victimClan.key)
                && war == classicClanWars.get(attackerClanKey);
        String enemyKey;
        String enemyName;
        if (war.clanOneKey.equals(victimClan.key)) {
            enemyKey = war.clanTwoKey;
            enemyName = war.clanTwoName;
        } else {
            enemyKey = war.clanOneKey;
            enemyName = war.clanOneName;
        }
        ClassicClanWarData data = new ClassicClanWarData(
                victimClan.name, enemyName);
        classicClanWars.remove(war.clanOneKey, war);
        classicClanWars.remove(war.clanTwoKey, war);
        List<ClientSession> recipients = connectedSessions.stream()
                .filter(ClientSession::acceptsLitoRosterBroadcast)
                .filter(session -> {
                    String memberClan = clanMembership.get(
                            key(session.sourceCharacterIdentity()));
                    return victimClan.key.equals(memberClan)
                            || enemyKey.equals(memberClan);
                })
                .toList();
        return new ClassicClanWarLeaderDeathResolution(
                true, attackerSameWar, data, recipients);
    }

    private static ClassicClanWarProposal clanWarProposal(
            ClassicClanWarRequestStatus status, ClientSession targetOwner,
            String requesterClan, String targetClan) {
        return new ClassicClanWarProposal(
                status, targetOwner, requesterClan, targetClan);
    }

    synchronized ClassicPartyData classicPartyData(String characterName) {
        ClassicParty party = classicPartyMembership.get(key(characterName));
        return party == null ? ClassicPartyData.empty() : classicPartySnapshot(party);
    }

    synchronized ClassicWorldCkewInstances.Admission admitClassicWorldCkew(
            ClientSession caller, long nowNanos) {
        List<ClassicWorldCkewInstances.Member> members =
                classicWorldCkewPartyMembers(caller);
        if (members.isEmpty()) {
            return ClassicWorldCkewInstances.Admission.rejected(
                    ClassicWorldCkewInstances.AdmissionStatus.INVALID_PARTY);
        }
        ClassicWorldCkewInstances.Admission admission =
                classicWorldCkewInstances.admit(
                caller.sourceCharacterIdentity(), members, nowNanos);
        if (admission.admitted() && admission.created()) {
            long objectBase = allocateClassicWorldCkewMonsterObjectBase();
            ClassicWorldMonsterPopulation population =
                    ClassicWorldMonsterPopulation.questInstance(
                            122L ^ admission.showId(), objectBase,
                            objectBase + 0x1000L,
                            ClassicWorldCkewMonsterCatalog.definitions());
            ClassicWorldMonsterPopulation previous =
                    classicWorldCkewMonsters.putIfAbsent(
                            admission.showId(), population);
            if (previous != null) {
                classicWorldCkewInstances.rollbackAdmission(
                        caller.sourceCharacterIdentity(), admission);
                throw new IllegalStateException(
                        "duplicate CKEW monster show " + admission.showId());
            }
        }
        return admission;
    }

    synchronized boolean validClassicWorldCkewParty(ClientSession caller) {
        List<ClassicWorldCkewInstances.Member> members =
                classicWorldCkewPartyMembers(caller);
        return !members.isEmpty() && ClassicWorldCkewCatalog.exactNativeParty(
                members.stream().map(
                        ClassicWorldCkewInstances.Member::characterType).toList());
    }

    synchronized void rollbackClassicWorldCkewAdmission(
            ClientSession caller, ClassicWorldCkewInstances.Admission admission) {
        if (caller == null) return;
        classicWorldCkewInstances.rollbackAdmission(
                caller.sourceCharacterIdentity(), admission);
        if (admission != null && admission.created()
                && classicWorldCkewInstances.snapshot(admission.showId()) == null) {
            classicWorldCkewMonsters.remove(admission.showId());
        }
    }

    synchronized boolean commitClassicWorldCkewAdmission(
            ClientSession caller, ClassicWorldCkewInstances.Admission admission) {
        return caller != null && classicWorldCkewInstances.commitAdmission(
                caller.sourceCharacterIdentity(), admission);
    }

    synchronized boolean commitClassicWorldCkewAdmission(
            ClientSession caller, ClassicWorldCkewInstances.Admission admission,
            ClassicWorldCkewInstances.DurableAdmissionCommit durableCommit)
            throws IOException {
        return caller != null && classicWorldCkewInstances.commitAdmission(
                caller.sourceCharacterIdentity(), admission, durableCommit);
    }

    synchronized boolean publishClassicWorldCkewAdmission(
            ClientSession caller, ClassicWorldCkewInstances.Admission admission,
            ClassicWorldCkewInstances.AdmissionPublication publication)
            throws IOException {
        return caller != null && classicWorldCkewInstances.publishAdmission(
                caller.sourceCharacterIdentity(), admission, publication);
    }

    ClassicWorldCkewInstances classicWorldCkewInstances() {
        return classicWorldCkewInstances;
    }

    synchronized int classicWorldCkewMonsterPopulationSize(int showId) {
        ClassicWorldMonsterPopulation population =
                classicWorldCkewMonsters.get(showId);
        return population == null ? 0 : population.size();
    }

    synchronized boolean classicWorldCkewHasMonsterNpc(
            int showId, Set<Integer> npcIds) {
        ClassicWorldMonsterPopulation population =
                classicWorldCkewMonsters.get(showId);
        return population != null && population.hasPresentNpc(
                ClassicWorldCkewCatalog.MAP_ID, npcIds);
    }

    synchronized ClassicWorldMonsterPopulation.Snapshot
            spawnClassicWorldCkewMonster(
                    int showId, int npcId, int x, int y, int heading) {
        ClassicWorldMonsterPopulation population =
                classicWorldCkewMonsters.get(showId);
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldCkewMonsterCatalog.templateForNpc(npcId);
        return population == null || template == null ? null
                : population.spawnQuestMonsterAt(
                        ClassicWorldCkewCatalog.MAP_ID, 600_000 + npcId,
                        x, y, heading, template);
    }

    void endClassicWorldCkew(int showId, String reason) {
        ClassicWorldCkewInstances.Ended ended =
                classicWorldCkewInstances.end(showId, reason);
        if (ended == null) return;
        synchronized (this) {
            classicWorldCkewMonsters.remove(showId);
        }
        List<ClientSession> recipients;
        synchronized (this) {
            Set<String> identities = Set.copyOf(ended.joinedIdentities());
            recipients = connectedSessions.stream()
                    .filter(session -> identities.contains(
                            key(session.sourceCharacterIdentity())))
                    .toList();
        }
        for (ClientSession recipient : recipients) {
            try {
                recipient.serverClassicWorldCkewEnded(ended);
            } catch (IOException error) {
                System.out.println("[LiTo] CKEW immediate end send failed show="
                        + ended.showId() + " player="
                        + recipient.selectedCharacterName() + ": "
                        + error.getMessage());
            }
        }
    }

    private List<ClassicWorldCkewInstances.Member> classicWorldCkewPartyMembers(
            ClientSession caller) {
        if (caller == null || !connectedSessions.contains(caller)) return List.of();
        String callerIdentity = key(caller.sourceCharacterIdentity());
        ClassicParty party = classicPartyMembership.get(callerIdentity);
        if (party == null || party.members.size() != 4) return List.of();
        LinkedHashMap<String, ClientSession> online = new LinkedHashMap<>();
        for (ClientSession session : connectedSessions) {
            String identity = key(session.sourceCharacterIdentity());
            if (party.members.containsKey(identity)
                    && session.hasSelectedClassicCharacter()) {
                if (online.put(identity, session) != null) return List.of();
            }
        }
        if (!online.keySet().equals(party.members.keySet())) return List.of();
        return party.members.keySet().stream()
                .map(identity -> new ClassicWorldCkewInstances.Member(
                        identity, online.get(identity).selectedCharacterType()))
                .toList();
    }

    private long allocateClassicWorldCkewMonsterObjectBase() {
        long result = nextClassicWorldCkewMonsterObjectBase;
        if (result > 0xFFFF_DFFFL) {
            throw new IllegalStateException("CKEW monster object range exhausted");
        }
        nextClassicWorldCkewMonsterObjectBase += 0x2000L;
        return result;
    }

    synchronized ClassicWorldWaterDragonInstances.Admission
            admitClassicWorldWaterDragon(
                    ClientSession caller, int showId, boolean hasCredential,
                    boolean cooldownActive, long nowNanos) {
        if (caller == null || !connectedSessions.contains(caller)
                || !caller.hasSelectedClassicCharacter()) {
            return ClassicWorldWaterDragonInstances.Admission.rejected(
                    ClassicWorldWaterDragonInstances.AdmissionStatus.INVALID_CREDENTIAL,
                    showId);
        }
        ClassicWorldWaterDragonInstances.Admission admission =
                classicWorldWaterDragonInstances.admit(
                        caller.sourceCharacterIdentity(), showId,
                        caller.battleLevel(), hasCredential, cooldownActive,
                        nowNanos);
        if (admission.admitted() && admission.created()) {
            long objectBase = allocateClassicWorldWaterDragonMonsterObjectBase();
            ClassicWorldMonsterPopulation population =
                    ClassicWorldMonsterPopulation.questInstance(
                            117L ^ admission.showId(), objectBase,
                            objectBase + 0x1000L,
                            ClassicWorldWaterDragonMonsterCatalog.definitions());
            ClassicWorldMonsterPopulation previous =
                    classicWorldWaterDragonMonsters.putIfAbsent(
                            admission.showId(), population);
            if (previous != null) {
                classicWorldWaterDragonInstances.rollbackAdmission(
                        caller.sourceCharacterIdentity(), admission);
                throw new IllegalStateException(
                        "duplicate Water Dragon monster show "
                                + admission.showId());
            }
            classicWorldWaterDragonAfflictionAfterNanos.put(
                    admission.showId(), nowNanos +
                            BattleWaterDragonAffliction.TIMER_INTERVAL_MILLIS
                                    * 1_000_000L);
        }
        return admission;
    }

    synchronized void restoreClassicWorldWaterDragonPasses(
            String sourceId,
            List<GameStateStore.WaterDragonPassState> durablePasses,
            long nowNanos, long nowEpochSeconds) {
        String source = sourceId == null ? "" : sourceId.trim();
        if (source.isEmpty()) {
            throw new IllegalArgumentException("Water Dragon source is required");
        }
        classicWorldWaterDragonPasses.values().removeIf(
                pass -> pass.sourceId().equalsIgnoreCase(source));
        if (durablePasses == null || durablePasses.isEmpty()) return;
        Set<Integer> occupiedRooms = new LinkedHashSet<>();
        Set<String> renters = new LinkedHashSet<>();
        for (GameStateStore.WaterDragonPassState durable : durablePasses) {
            if (durable == null
                    || !durable.sourceId().equalsIgnoreCase(source)
                    || !occupiedRooms.add(durable.roomNumber())
                    || !renters.add(key(durable.renterIdentity()))) continue;
            long remainingSeconds = durable.remainingSeconds(nowEpochSeconds);
            if (remainingSeconds <= 0) continue;
            long remainingNanos;
            try {
                remainingNanos = Math.multiplyExact(remainingSeconds,
                        1_000_000_000L);
            } catch (ArithmeticException overflow) {
                remainingNanos = Long.MAX_VALUE;
            }
            long due = remainingNanos == Long.MAX_VALUE
                    || nowNanos > Long.MAX_VALUE - remainingNanos
                    ? Long.MAX_VALUE : nowNanos + remainingNanos;
            int showId = allocateClassicWorldWaterDragonShowId(durable.showId());
            classicWorldWaterDragonPasses.put(
                    new WaterDragonPassKey(source, durable.itemObjectId()),
                    new WaterDragonPass(
                            source, durable.itemObjectId(), showId,
                            durable.roomNumber(), durable.renterIdentity(), due));
        }
    }

    synchronized WaterDragonPass rentClassicWorldWaterDragonPass(
            String sourceId, ClientSession renter, long itemObjectId, long nowNanos) {
        String source = sourceId == null ? "" : sourceId.trim();
        if (renter == null || !connectedSessions.contains(renter)
                || source.isEmpty() || !renter.usesClassicSource(source)
                || itemObjectId <= 0 || itemObjectId > 0x7FFF_FFFFL) return null;
        classicWorldWaterDragonPasses.values().removeIf(
                pass -> !pass.validAt(nowNanos));
        Set<Integer> occupied = classicWorldWaterDragonPasses.values().stream()
                .filter(pass -> pass.sourceId().equalsIgnoreCase(source))
                .map(WaterDragonPass::roomNumber)
                .collect(java.util.stream.Collectors.toSet());
        int room = -1;
        for (int candidate = 0;
                candidate < ClassicWorldWaterDragonCatalog.ROOM_COUNT;
                candidate++) {
            if (!occupied.contains(candidate)) {
                room = candidate;
                break;
            }
        }
        WaterDragonPassKey passKey = new WaterDragonPassKey(source, itemObjectId);
        if (room < 0 || classicWorldWaterDragonPasses.containsKey(passKey)
                || classicWorldWaterDragonPasses.values().stream().anyMatch(
                        pass -> pass.sourceId().equalsIgnoreCase(source)
                                && pass.renterIdentity().equalsIgnoreCase(
                                        renter.sourceCharacterIdentity()))) {
            return null;
        }
        int showId = allocateClassicWorldWaterDragonShowId(
                Math.toIntExact(itemObjectId));
        long duration = ClassicWorldWaterDragonCatalog.PASS_RENTAL_SECONDS
                * 1_000_000_000L;
        long due = nowNanos > Long.MAX_VALUE - duration
                ? Long.MAX_VALUE : nowNanos + duration;
        WaterDragonPass pass = new WaterDragonPass(
                source, itemObjectId, showId, room,
                renter.sourceCharacterIdentity(), due);
        classicWorldWaterDragonPasses.put(passKey, pass);
        return pass;
    }

    synchronized WaterDragonPass classicWorldWaterDragonPass(
            String sourceId, Set<Long> inventoryObjectIds, long nowNanos) {
        String source = sourceId == null ? "" : sourceId.trim();
        if (inventoryObjectIds == null || inventoryObjectIds.isEmpty()) return null;
        classicWorldWaterDragonPasses.values().removeIf(
                pass -> !pass.validAt(nowNanos));
        return classicWorldWaterDragonPasses.values().stream()
                .filter(pass -> pass.sourceId().equalsIgnoreCase(source)
                        && inventoryObjectIds.contains(pass.itemObjectId()))
                .findFirst().orElse(null);
    }

    synchronized int classicWorldWaterDragonRentedRooms(
            String sourceId, long nowNanos) {
        String source = sourceId == null ? "" : sourceId.trim();
        classicWorldWaterDragonPasses.values().removeIf(
                pass -> !pass.validAt(nowNanos));
        return (int) classicWorldWaterDragonPasses.values().stream()
                .filter(pass -> pass.sourceId().equalsIgnoreCase(source))
                .count();
    }

    synchronized void cancelClassicWorldWaterDragonPass(
            String sourceId, long itemObjectId) {
        classicWorldWaterDragonPasses.remove(
                new WaterDragonPassKey(sourceId, itemObjectId));
    }

    private int allocateClassicWorldWaterDragonShowId(int preferred) {
        boolean preferredUsed = preferred <= 0
                || classicWorldWaterDragonPasses.values().stream()
                        .anyMatch(pass -> pass.showId() == preferred)
                || classicWorldWaterDragonInstances.snapshot(preferred) != null;
        if (!preferredUsed) return preferred;
        for (long attempts = 0; attempts < 0x7FFF_FFFEL; attempts++) {
            int candidate = nextClassicWorldWaterDragonShowId++;
            if (nextClassicWorldWaterDragonShowId <= 0) {
                nextClassicWorldWaterDragonShowId = 1;
            }
            boolean used = classicWorldWaterDragonPasses.values().stream()
                    .anyMatch(pass -> pass.showId() == candidate)
                    || classicWorldWaterDragonInstances.snapshot(candidate) != null;
            if (!used) return candidate;
        }
        throw new IllegalStateException("Water Dragon show id space exhausted");
    }

    synchronized void rollbackClassicWorldWaterDragonAdmission(
            ClientSession caller,
            ClassicWorldWaterDragonInstances.Admission admission) {
        if (caller == null) return;
        classicWorldWaterDragonInstances.rollbackAdmission(
                caller.sourceCharacterIdentity(), admission);
        if (admission != null && admission.created()
                && classicWorldWaterDragonInstances.snapshot(
                        admission.showId()) == null) {
            classicWorldWaterDragonMonsters.remove(admission.showId());
            classicWorldWaterDragonAfflictionAfterNanos.remove(
                    admission.showId());
        }
    }

    ClassicWorldWaterDragonInstances classicWorldWaterDragonInstances() {
        return classicWorldWaterDragonInstances;
    }

    synchronized int classicWorldWaterDragonMonsterPopulationSize(int showId) {
        ClassicWorldMonsterPopulation population =
                classicWorldWaterDragonMonsters.get(showId);
        return population == null ? 0 : population.size();
    }

    synchronized boolean classicWorldWaterDragonDoorBlocks(
            int mapId, int showId, int x, int y) {
        if (mapId != ClassicWorldWaterDragonCatalog.MAP_ID || showId <= 0) {
            return false;
        }
        ClassicWorldWaterDragonInstances.Snapshot snapshot =
                classicWorldWaterDragonInstances.snapshot(showId);
        if (snapshot == null || !snapshot.active()) return false;
        for (ClassicWorldWaterDragonCatalog.Door door
                : ClassicWorldWaterDragonCatalog.doors()) {
            if (Boolean.TRUE.equals(snapshot.openDoors().get(door.doorId()))) {
                continue;
            }
            if (door.blocks(x, y)) return true;
        }
        return false;
    }

    synchronized ClassicWorldWaterDragonCatalog.Door
            classicWorldWaterDragonGuardianDied(int showId, int guardianNpcId) {
        return classicWorldWaterDragonInstances.guardianDied(
                showId, guardianNpcId);
    }

    synchronized ClassicWorldWaterDragonInstances.GateTalk
            talkClassicWorldWaterDragonBossGate(
                    ClientSession caller, long nowNanos) {
        if (caller == null || !connectedSessions.contains(caller)) {
            return new ClassicWorldWaterDragonInstances.GateTalk(
                    false, false, null);
        }
        return classicWorldWaterDragonInstances.talkBossGate(
                caller.sourceCharacterIdentity(), nowNanos);
    }

    synchronized boolean classicWorldWaterDragonBossDied(
            int showId, int npcId, Set<String> hateListIdentities,
            long nowNanos) {
        return classicWorldWaterDragonInstances.bossDied(
                showId, npcId, hateListIdentities, nowNanos);
    }

    private long allocateClassicWorldWaterDragonMonsterObjectBase() {
        long result = nextClassicWorldWaterDragonMonsterObjectBase;
        if (result > 0xFFFF_DFFFL) {
            throw new IllegalStateException(
                    "Water Dragon monster object range exhausted");
        }
        nextClassicWorldWaterDragonMonsterObjectBase += 0x2000L;
        return result;
    }

    /** Question-953 acceptance creates a party or appends to the leader's party. */
    synchronized ClassicPartyJoinResult joinClassicParty(String inviterName,
                                                          String inviteeName,
                                                          int maximumMembers) {
        String inviterKey = key(inviterName);
        String inviteeKey = key(inviteeName);
        if (inviterKey.isEmpty() || inviteeKey.isEmpty() || inviterKey.equals(inviteeKey)
                || maximumMembers < 2) {
            return new ClassicPartyJoinResult(
                    ClassicPartyJoinStatus.INVALID, ClassicPartyData.empty());
        }
        ClassicParty inviteeParty = classicPartyMembership.get(inviteeKey);
        if (inviteeParty != null) {
            return new ClassicPartyJoinResult(
                    ClassicPartyJoinStatus.ALREADY_MEMBER, classicPartySnapshot(inviteeParty));
        }

        ClassicParty party = classicPartyMembership.get(inviterKey);
        if (party != null && !party.leaderKey.equals(inviterKey)) {
            return new ClassicPartyJoinResult(
                    ClassicPartyJoinStatus.NOT_LEADER, classicPartySnapshot(party));
        }
        if (party != null && party.members.size() >= maximumMembers) {
            return new ClassicPartyJoinResult(
                    ClassicPartyJoinStatus.FULL, classicPartySnapshot(party));
        }
        if (party == null) {
            party = new ClassicParty(inviterName);
            classicPartyMembership.put(inviterKey, party);
        }
        String inviteeDisplay = identityDisplayName(inviteeName);
        if (party.members.values().stream()
                .anyMatch(name -> name.equalsIgnoreCase(inviteeDisplay))) {
            return new ClassicPartyJoinResult(
                    ClassicPartyJoinStatus.INVALID, classicPartySnapshot(party));
        }
        party.members.put(inviteeKey, inviteeDisplay);
        classicPartyMembership.put(inviteeKey, party);
        return new ClassicPartyJoinResult(
                ClassicPartyJoinStatus.JOINED, classicPartySnapshot(party));
    }

    synchronized ClassicPartyRemoveResult leaveClassicParty(String characterName) {
        String characterKey = key(characterName);
        ClassicParty party = classicPartyMembership.get(characterKey);
        if (party == null) {
            return new ClassicPartyRemoveResult(
                    ClassicPartyRemoveStatus.NOT_MEMBER, ClassicPartyData.empty(), "", "");
        }
        ClassicPartyData before = classicPartySnapshot(party);
        String removedName = party.members.get(characterKey);
        if (party.leaderKey.equals(characterKey)) {
            disbandClassicParty(party);
            return new ClassicPartyRemoveResult(
                    ClassicPartyRemoveStatus.LEADER_DISBANDED, before,
                    removedName, characterKey);
        }
        if (party.members.size() == 2) {
            disbandClassicParty(party);
            return new ClassicPartyRemoveResult(
                    ClassicPartyRemoveStatus.LEFT_AND_DISBANDED, before,
                    removedName, characterKey);
        }
        party.members.remove(characterKey);
        classicPartyMembership.remove(characterKey, party);
        return new ClassicPartyRemoveResult(
                ClassicPartyRemoveStatus.LEFT, before, removedName, characterKey);
    }

    synchronized ClassicPartyRemoveResult kickClassicParty(String leaderName,
                                                            String memberName) {
        String leaderKey = key(leaderName);
        ClassicParty party = classicPartyMembership.get(leaderKey);
        if (party == null) {
            return new ClassicPartyRemoveResult(
                    ClassicPartyRemoveStatus.NOT_MEMBER, ClassicPartyData.empty(), "", "");
        }
        ClassicPartyData before = classicPartySnapshot(party);
        if (!party.leaderKey.equals(leaderKey)) {
            return new ClassicPartyRemoveResult(
                    ClassicPartyRemoveStatus.NOT_LEADER, before, "", "");
        }
        List<String> matchingKeys = party.members.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(memberName))
                .map(Map.Entry::getKey).toList();
        String memberKey = matchingKeys.size() == 1 ? matchingKeys.get(0) : "";
        String removedName = party.members.get(memberKey);
        if (memberKey.isEmpty() || memberKey.equals(leaderKey) || removedName == null) {
            return new ClassicPartyRemoveResult(
                    ClassicPartyRemoveStatus.TARGET_NOT_FOUND, before, "", "");
        }
        if (party.members.size() == 2) {
            disbandClassicParty(party);
            return new ClassicPartyRemoveResult(
                    ClassicPartyRemoveStatus.KICKED_AND_DISBANDED, before,
                    removedName, memberKey);
        }
        party.members.remove(memberKey);
        classicPartyMembership.remove(memberKey, party);
        return new ClassicPartyRemoveResult(
                ClassicPartyRemoveStatus.KICKED, before, removedName, memberKey);
    }

    synchronized List<ClientSession> onlineCharacterSessions(
            ClientSession requester, List<String> characterNames) {
        if (requester == null || characterNames == null || characterNames.isEmpty()) {
            return List.of();
        }
        Set<String> identities = characterNames.stream()
                .map(requester::sourceCharacterIdentity)
                .map(GameHub::key)
                .collect(java.util.stream.Collectors.toSet());
        return nativeChannelMembers().stream()
                .filter(session -> identities.contains(key(session.sourceCharacterIdentity())))
                .toList();
    }

    synchronized List<ClientSession> onlineCharacterSessionsByIdentity(
            List<String> characterIdentities) {
        if (characterIdentities == null || characterIdentities.isEmpty()) return List.of();
        Set<String> identities = characterIdentities.stream().map(GameHub::key)
                .filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
        return nativeChannelMembers().stream()
                .filter(session -> identities.contains(key(session.sourceCharacterIdentity())))
                .toList();
    }

    private static ClassicPartyData classicPartySnapshot(ClassicParty party) {
        return new ClassicPartyData(
                party.members.get(party.leaderKey), new ArrayList<>(party.members.values()),
                party.leaderKey, new ArrayList<>(party.members.keySet()));
    }

    private void disbandClassicParty(ClassicParty party) {
        for (String memberKey : new ArrayList<>(party.members.keySet())) {
            classicPartyMembership.remove(memberKey, party);
        }
        party.members.clear();
    }

    private ClanProfile ownedClan(String ownerName) {
        String clanKey = clanMembership.get(key(ownerName));
        ClanProfile clan = clanKey == null ? null : clanProfiles.get(clanKey);
        ClanMember owner = clan == null ? null : clan.members.get(key(ownerName));
        return owner != null && owner.role == CLAN_ROLE_OWNER ? clan : null;
    }

    private static ClanData clanSnapshot(ClanProfile clan) {
        return new ClanData(clan.name, clan.members.values().stream()
                .map(GameHub::clanMemberSnapshot).toList(), clan.destroyPending);
    }

    private static ClanMemberSnapshot clanMemberSnapshot(ClanMember member) {
        return new ClanMemberSnapshot(member.name,
                Protocol.LitoClanRole.fromWire(member.role),
                member.relationshipStyle, member.paletteOffset,
                member.title, member.activity);
    }

    private BuddyProfile buddyProfile(String owner) {
        return buddyProfiles.computeIfAbsent(key(owner), ignored -> new BuddyProfile());
    }

    private boolean commitBuddyProfile(String owner, BuddyProfile replacement) {
        String ownerKey = key(owner);
        BuddyPersistenceHook hook = buddyPersistenceHooks.get(ownerKey);
        if (hook != null && !hook.persist(persistentBuddySnapshot(replacement))) {
            return false;
        }
        buddyProfiles.put(ownerKey, replacement);
        return true;
    }

    private static BuddyPersistentData persistentBuddySnapshot(BuddyProfile profile) {
        List<BuddyGroup> groups = profile.groups.entrySet().stream()
                .map(group -> new BuddyGroup(group.getKey(), group.getValue())).toList();
        List<BuddyPersistentEntry> buddies = profile.entries.values().stream()
                .filter(entry -> entry.durable)
                .map(entry -> new BuddyPersistentEntry(entry.name,
                        entry.buddyObjectId, entry.targetSource, entry.groupId,
                        entry.relationshipStyle, entry.paletteOffset, entry.origin))
                .toList();
        return new BuddyPersistentData(groups, buddies);
    }

    private static int nextBuddyGroupId(BuddyProfile profile) {
        for (int id = Math.max(2, profile.nextGroupId); id <= 255; id++) {
            if (!profile.groups.containsKey(id)) return id;
        }
        for (int id = 2; id < Math.max(2, profile.nextGroupId); id++) {
            if (!profile.groups.containsKey(id)) return id;
        }
        return -1;
    }

    private static BuddyData buddySnapshot(BuddyProfile profile) {
        List<BuddyGroup> groups = profile.groups.entrySet().stream()
                .map(group -> new BuddyGroup(group.getKey(), group.getValue())).toList();
        List<BuddySnapshot> buddies = profile.entries.values().stream()
                .map(entry -> new BuddySnapshot(entry.name, entry.groupId,
                        entry.relationshipStyle, entry.paletteOffset,
                        entry.origin, entry.activity)).toList();
        return new BuddyData(groups, buddies);
    }

    private static boolean hasGroupName(BuddyProfile profile, String name) {
        return profile.groups.values().stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
    }

    private static String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String roomParticipantIdentity(ClientSession session) {
        // Authenticated production sessions always have a source-qualified
        // selected character identity.  Before C_C9, playerName() deliberately
        // falls back to TestKnight, so that placeholder must not merge every
        // pre-login fixture into one kicked identity.
        if (session == null) return "session:null";
        if (session.selectedCharacterName().isBlank()) {
            return "session:" + System.identityHashCode(session);
        }
        String identity = session.sourceCharacterIdentity();
        return identity == null || identity.isBlank()
                ? "session:" + System.identityHashCode(session) : key(identity);
    }

    private static String identitySourceKey(String identity) {
        if (identity == null) return "";
        int separator = identity.indexOf('\u001F');
        return separator <= 0 ? "" : key(identity.substring(0, separator).trim());
    }

    private static String identityDisplayName(String identity) {
        if (identity == null) return "";
        int separator = identity.indexOf('\u001F');
        return (separator < 0 ? identity : identity.substring(separator + 1)).trim();
    }

    private static String scopedMemberIdentity(String ownerIdentity, String memberName) {
        String member = memberName == null ? "" : memberName.trim();
        if (member.indexOf('\u001F') >= 0) return member;
        if (ownerIdentity == null) return member;
        int separator = ownerIdentity.indexOf('\u001F');
        if (separator <= 0) return member;
        String source = ownerIdentity.substring(0, separator).trim();
        return source.isEmpty() || member.isEmpty()
                ? member : GameStateStore.sourceCharacter(source, member);
    }

    private static String scopedClanKey(String characterIdentity, String clanName) {
        return key(scopedMemberIdentity(characterIdentity, clanName));
    }

    synchronized List<ClientSession> roomCombatants(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? List.of() : new ArrayList<>(room.combatants());
    }

    synchronized int roomCombatantCount(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? 0 : room.combatantCount();
    }

    synchronized List<RoomVirtualPlayer> roomVirtualPlayers(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? List.of() : new ArrayList<>(room.virtualPlayers);
    }

    synchronized List<RoomVirtualPlayer> battleVirtualPlayers(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return List.of();
        return room.battleVirtualPlayers.isEmpty()
                ? new ArrayList<>(room.virtualPlayers)
                : new ArrayList<>(room.battleVirtualPlayers);
    }

    /**
     * Adds only the seats that fit the room's mode-specific native capacity.
     * A zero request clears all test players. Virtual players are always ready,
     * and team modes place each new player on the currently smaller force.
     */
    synchronized VirtualPlayerUpdate addRoomVirtualPlayers(
            long roomId, ClientSession requester, String classicServerName, int requested) {
        Room room = rooms.get(roomId);
        if (room == null || requester == null || !room.members.contains(requester)
                || requester.isObserver() || room.battleStarted || room.resultPending
                || room.match.phase() != BattleRules.Phase.WAITING
                || requested < 0 || requested > 100) {
            return new VirtualPlayerUpdate(false, List.of(), List.of(),
                    room == null ? 0 : nativeCombatantCapacity(room.settings),
                    room == null ? 0 : room.combatants().size(),
                    room == null ? 0 : room.virtualPlayers.size(), "invalid-room-state");
        }
        if (requested == 0) {
            List<RoomVirtualPlayer> removed = new ArrayList<>(room.virtualPlayers);
            room.virtualPlayers.clear();
            return new VirtualPlayerUpdate(true, List.of(), removed,
                    nativeCombatantCapacity(room.settings), room.combatants().size(), 0,
                    "cleared");
        }
        int capacity = nativeCombatantCapacity(room.settings);
        int available = Math.max(0, capacity - room.combatantCount());
        int accepted = Math.min(requested, available);
        List<RoomVirtualPlayer> added = new ArrayList<>(accepted);
        String source = classicServerName == null ? "" : classicServerName.trim();
        for (int index = 0; index < accepted; index++) {
            int ordinal = room.nextVirtualPlayerOrdinal++;
            int force = room.match.rules().teamBased() ? smallerVirtualForce(room) : 0;
            String name = virtualPlayerDisplayName(ordinal, source);
            RoomVirtualPlayer player = new RoomVirtualPlayer(
                    nextVirtualPlayerObjectId++, name, source, force,
                    Math.floorMod(ordinal - 1, 5), Math.floorMod(ordinal - 1, 2));
            room.virtualPlayers.add(player);
            added.add(player);
        }
        return new VirtualPlayerUpdate(true, added, List.of(), capacity,
                room.combatants().size(), room.virtualPlayers.size(),
                accepted == requested ? "created" : "capacity-limited");
    }

    private static int smallerVirtualForce(Room room) {
        int force0 = 0;
        int force1 = 0;
        for (ClientSession player : room.combatants()) {
            if (forceOf(room, player) == 0) force0++; else force1++;
        }
        for (RoomVirtualPlayer player : room.virtualPlayers) {
            if (player.force() == 0) force0++; else force1++;
        }
        return force0 <= force1 ? 0 : 1;
    }

    private static String virtualPlayerDisplayName(int ordinal, String source) {
        String base = String.format(Locale.ROOT, "BOT%02d", ordinal);
        if (source == null || source.isBlank()) return base;
        int available = 27 - base.getBytes(Protocol.LEGACY_TEXT).length - 2;
        String suffix = truncateLegacyText(source, available);
        return suffix.isBlank() ? base : base + "(" + suffix + ")";
    }

    private static String truncateLegacyText(String value, int maximumBytes) {
        StringBuilder result = new StringBuilder();
        int bytes = 0;
        for (int offset = 0; value != null && offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            String next = new String(Character.toChars(codePoint));
            int nextBytes = next.getBytes(Protocol.LEGACY_TEXT).length;
            if (bytes + nextBytes > maximumBytes) break;
            result.append(next);
            bytes += nextBytes;
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    synchronized int battleMapId(long roomId, ClientSession participant) {
        Room room = rooms.get(roomId);
        if (room == null) return BattleRules.decode(
                defaultRoomSettings(), battleControls).mapId();
        int force = participant == null ? 0 : forceOf(room, participant);
        return BattleRules.arenaMapForForce(room.match.rules(), force);
    }

    synchronized List<ClientSession> visibleBattleCombatants(
            long roomId, ClientSession recipient) {
        Room room = rooms.get(roomId);
        if (room == null) return List.of();
        List<ClientSession> combatants = room.battleRoster.isEmpty()
                ? room.combatants() : new ArrayList<>(room.battleRoster);
        if (room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE
                || recipient == null) return combatants;
        int force = forceOf(room, recipient);
        return combatants.stream()
                .filter(member -> forceOf(room, member) == force).toList();
    }

    synchronized List<ClientSession> battleRecipients(long roomId, int force) {
        Room room = rooms.get(roomId);
        if (room == null) return List.of();
        if (room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) {
            return new ArrayList<>(room.members);
        }
        List<ClientSession> result = new ArrayList<>();
        for (ClientSession member : room.members) {
            // Observers are attached to the first member of a Quest pair.
            int memberForce = member.isObserver() ? 0 : forceOf(room, member);
            if (memberForce == force) result.add(member);
        }
        return result;
    }

    /** Installs or closes the C_80 personal shop advertised by one combatant. */
    synchronized PrivateShopActionOutcome changePrivateShop(
            long roomId, ClientSession owner, Protocol.PersonalShopRequest request) {
        Room room = rooms.get(roomId);
        List<ClientSession> recipients = room == null
                ? List.of(owner) : new ArrayList<>(room.combatants());
        if (request == null || owner == null) {
            return new PrivateShopActionOutcome(PrivateShopActionStatus.INVALID,
                    recipients, new byte[0]);
        }
        if (request.type() == 1) {
            removePrivateShop(owner);
            return new PrivateShopActionOutcome(PrivateShopActionStatus.CLOSED,
                    recipients, new byte[0]);
        }
        if (request.type() != 0 || room == null || !room.battleStarted
                || !room.combatants().contains(owner) || !owner.tradeEligible(roomId)
                || playerTrades.containsKey(owner)
                || (request.sellOffers().isEmpty() && request.buyOffers().isEmpty())) {
            removePrivateShop(owner);
            return new PrivateShopActionOutcome(PrivateShopActionStatus.INVALID,
                    recipients, new byte[0]);
        }

        RuntimeInventory actual = owner.tradeInventoryCopy();
        List<PrivateShopSellOffer> sellOffers = new ArrayList<>();
        List<PrivateShopBuyOffer> buyOffers = new ArrayList<>();
        Set<Long> usedObjectIds = new LinkedHashSet<>();
        Set<InventoryCatalog.Definition> buyDefinitions = new LinkedHashSet<>();
        long reservedAdena = 0;
        try {
            for (int index = 0; index < request.sellOffers().size(); index++) {
                Protocol.PersonalShopOffer row = request.sellOffers().get(index);
                RuntimeInventory.Item item = actual.item(row.objectId());
                if (item == null || item.equipped() || !item.transferable()
                        || item.definition().catalogId() == InventoryCatalog.ADENA
                        || row.quantity() > item.quantity()
                        || !usedObjectIds.add(row.objectId())) {
                    throw new IllegalArgumentException("invalid private-shop sell offer");
                }
                sellOffers.add(new PrivateShopSellOffer(index, row.objectId(),
                        row.unitPrice(), row.quantity(), item.definition()));
            }
            for (int index = 0; index < request.buyOffers().size(); index++) {
                Protocol.PersonalShopOffer row = request.buyOffers().get(index);
                RuntimeInventory.Item sample = actual.item(row.objectId());
                if (sample == null || sample.equipped() || !sample.transferable()
                        || sample.definition().catalogId() == InventoryCatalog.ADENA
                        || !usedObjectIds.add(row.objectId())
                        || !buyDefinitions.add(sample.definition())) {
                    throw new IllegalArgumentException("invalid private-shop buy offer");
                }
                reservedAdena = Math.addExact(reservedAdena,
                        Math.multiplyExact(row.unitPrice(), row.quantity()));
                buyOffers.add(new PrivateShopBuyOffer(index, row.objectId(),
                        row.unitPrice(), row.quantity(), sample.definition()));
            }
        } catch (ArithmeticException | IllegalArgumentException invalid) {
            removePrivateShop(owner);
            return new PrivateShopActionOutcome(PrivateShopActionStatus.INVALID,
                    recipients, new byte[0]);
        }
        if (reservedAdena > catalogCount(actual, InventoryCatalog.ADENA)) {
            removePrivateShop(owner);
            return new PrivateShopActionOutcome(PrivateShopActionStatus.INVALID,
                    recipients, new byte[0]);
        }

        removePrivateShop(owner);
        PrivateShop shop = new PrivateShop(roomId, owner,
                nextPrivateShopGeneration++, sellOffers, buyOffers,
                request.announcement());
        privateShops.put(owner, shop);
        return new PrivateShopActionOutcome(PrivateShopActionStatus.OPENED,
                recipients, shop.announcement);
    }

    synchronized PrivateShopActionOutcome closePrivateShop(ClientSession owner) {
        PrivateShop shop = privateShops.get(owner);
        Room room = shop == null ? null : rooms.get(shop.roomId);
        List<ClientSession> recipients = room == null
                ? List.of(owner) : new ArrayList<>(room.combatants());
        removePrivateShop(owner);
        privateShopViews.remove(owner);
        return new PrivateShopActionOutcome(PrivateShopActionStatus.CLOSED,
                recipients, new byte[0]);
    }

    /** Resolves C_81 and arms a one-shot capability for the following C_DB. */
    synchronized PrivateShopViewOutcome queryPrivateShop(
            long roomId, ClientSession visitor, Protocol.PersonalShopQuery query) {
        privateShopViews.remove(visitor);
        PrivateShop shop = privateShops.values().stream()
                .filter(candidate -> candidate.owner.playerObjectId() == query.targetObjectId())
                .findFirst().orElse(null);
        if (shop == null || shop.owner == visitor || shop.roomId != roomId
                || !visitor.tradeEligible(roomId) || !shop.owner.tradeEligible(roomId)
                || playerTrades.containsKey(visitor) || playerTrades.containsKey(shop.owner)
                || distance(visitor, shop.owner) > 3) {
            return rejectedPrivateShopView(query);
        }

        RuntimeInventory ownerInventory = shop.owner.tradeInventoryCopy();
        Map<Long, Integer> rowToOffer = new LinkedHashMap<>();
        List<Protocol.PersonalShopSellRow> sellRows = new ArrayList<>();
        List<Protocol.PersonalShopBuyRow> buyRows = new ArrayList<>();
        if (query.type() == 0) {
            for (PrivateShopSellOffer offer : shop.sellOffers) {
                if (offer.remaining <= 0) continue;
                RuntimeInventory.Item item = ownerInventory.item(offer.objectId);
                if (item == null || item.equipped() || !item.transferable()
                        || !item.definition().equals(offer.definition)
                        || item.quantity() < offer.remaining) {
                    return rejectedPrivateShopView(query);
                }
                byte[] detail = item.identified()
                        ? item.nativeStatusDetail() : new byte[0];
                sellRows.add(new Protocol.PersonalShopSellRow(offer.index,
                        item.blessing(), item.definition().icon(),
                        offer.remaining, offer.unitPrice, item.nativeViewName(), detail));
                rowToOffer.put((long) offer.index, offer.index);
            }
        } else {
            long reserve = 0;
            try {
                for (PrivateShopBuyOffer offer : shop.buyOffers) {
                    if (offer.remaining > 0) reserve = Math.addExact(reserve,
                            Math.multiplyExact(offer.unitPrice, offer.remaining));
                }
            } catch (ArithmeticException overflow) {
                return rejectedPrivateShopView(query);
            }
            if (reserve > catalogCount(ownerInventory, InventoryCatalog.ADENA)) {
                return rejectedPrivateShopView(query);
            }
            RuntimeInventory visitorInventory = visitor.tradeInventoryCopy();
            for (PrivateShopBuyOffer offer : shop.buyOffers) {
                if (offer.remaining <= 0) continue;
                for (RuntimeInventory.Item item : visitorInventory.items()) {
                    if (item.equipped() || !item.transferable()
                            || !item.definition().equals(offer.definition)) continue;
                    buyRows.add(new Protocol.PersonalShopBuyRow(offer.index,
                            item.objectId(), offer.remaining, offer.unitPrice));
                    rowToOffer.put(item.objectId(), offer.index);
                }
            }
        }
        privateShopViews.put(visitor, new PrivateShopView(shop, query.type(),
                shop.generation, Map.copyOf(rowToOffer)));
        return new PrivateShopViewOutcome(true, query.type(), query.targetObjectId(),
                sellRows, buyRows);
    }

    synchronized boolean hasPendingPrivateShopQuery(ClientSession visitor) {
        return privateShopViews.containsKey(visitor);
    }

    synchronized boolean hasPrivateShop(ClientSession owner) {
        return privateShops.containsKey(owner);
    }

    /** Applies the one-shot contextual C_DB transaction and persists both sides atomically. */
    synchronized PrivateShopTransactionOutcome transactPrivateShop(
            long roomId, ClientSession visitor, Protocol.ShopTransaction transaction)
            throws IOException {
        PrivateShopView view = privateShopViews.remove(visitor);
        if (view == null || transaction == null || !transaction.hasContext()
                || !transaction.exactNativeTrailer()
                || transaction.contextId() != view.shop.owner.playerObjectId()
                || transaction.operation() != view.type || transaction.lines().isEmpty()) {
            return rejectedPrivateShopTransaction();
        }
        PrivateShop shop = privateShops.get(view.shop.owner);
        if (shop != view.shop || shop.roomId != roomId || shop.generation != view.generation
                || !visitor.tradeEligible(roomId) || !shop.owner.tradeEligible(roomId)
                || playerTrades.containsKey(visitor) || playerTrades.containsKey(shop.owner)
                || distance(visitor, shop.owner) > 3) {
            return rejectedPrivateShopTransaction();
        }

        Map<Long, Long> requested = new LinkedHashMap<>();
        try {
            for (Protocol.ShopLine line : transaction.lines()) {
                if (!view.rowToOffer.containsKey(line.itemId())) {
                    return rejectedPrivateShopTransaction();
                }
                requested.merge(line.itemId(), line.quantity(), Math::addExact);
            }
        } catch (ArithmeticException overflow) {
            return rejectedPrivateShopTransaction();
        }

        ClientSession owner = shop.owner;
        RuntimeInventory visitorBefore = visitor.tradeInventoryCopy();
        RuntimeInventory ownerBefore = owner.tradeInventoryCopy();
        RuntimeInventory visitorAfter = visitorBefore.copy();
        RuntimeInventory ownerAfter = ownerBefore.copy();
        Map<Integer, Long> consumed = new LinkedHashMap<>();
        long totalAdena = 0;
        try {
            for (Map.Entry<Long, Long> entry : requested.entrySet()) {
                long rowId = entry.getKey();
                long quantity = entry.getValue();
                int offerIndex = view.rowToOffer.get(rowId);
                if (quantity <= 0 || quantity > Integer.MAX_VALUE) {
                    return rejectedPrivateShopTransaction();
                }
                if (view.type == 0) {
                    PrivateShopSellOffer offer = shop.sellOffer(offerIndex);
                    RuntimeInventory.Item item = offer == null ? null : ownerAfter.item(offer.objectId);
                    if (offer == null || quantity > offer.remaining || item == null
                            || item.equipped() || !item.transferable()
                            || !item.definition().equals(offer.definition)) {
                        return rejectedPrivateShopTransaction();
                    }
                    RuntimeInventory admitted = admitIncoming(visitorAfter, visitor,
                            item, quantity, null, "private-shop");
                    if (admitted == null) return rejectedPrivateShopTransaction();
                    if (!ownerAfter.remove(offer.objectId, quantity)) {
                        throw new IllegalStateException(
                                "validated private-shop owner removal failed");
                    }
                    visitorAfter = admitted;
                    totalAdena = Math.addExact(totalAdena,
                            Math.multiplyExact(offer.unitPrice, quantity));
                } else {
                    PrivateShopBuyOffer offer = shop.buyOffer(offerIndex);
                    RuntimeInventory.Item item = visitorAfter.item(rowId);
                    if (offer == null || quantity > offer.remaining || item == null
                            || item.equipped() || !item.transferable()
                            || !item.definition().equals(offer.definition)) {
                        return rejectedPrivateShopTransaction();
                    }
                    RuntimeInventory admitted = admitIncoming(ownerAfter, owner,
                            item, quantity, null, "private-shop");
                    if (admitted == null) return rejectedPrivateShopTransaction();
                    if (!visitorAfter.remove(rowId, quantity)) {
                        throw new IllegalStateException(
                                "validated private-shop visitor removal failed");
                    }
                    ownerAfter = admitted;
                    totalAdena = Math.addExact(totalAdena,
                            Math.multiplyExact(offer.unitPrice, quantity));
                }
                long aggregate = Math.addExact(consumed.getOrDefault(offerIndex, 0L), quantity);
                long offered = view.type == 0
                        ? shop.sellOffer(offerIndex).remaining
                        : shop.buyOffer(offerIndex).remaining;
                if (aggregate > offered) return rejectedPrivateShopTransaction();
                consumed.put(offerIndex, aggregate);
            }
        } catch (ArithmeticException overflow) {
            return rejectedPrivateShopTransaction();
        }
        if (totalAdena <= 0 || totalAdena > Integer.MAX_VALUE) {
            return rejectedPrivateShopTransaction();
        }
        if (view.type == 0) {
            if (!visitorAfter.removeCatalog(InventoryCatalog.ADENA, totalAdena)
                    || !ownerAfter.add(InventoryCatalog.find(InventoryCatalog.ADENA), totalAdena)) {
                return rejectedPrivateShopTransaction();
            }
        } else if (!ownerAfter.removeCatalog(InventoryCatalog.ADENA, totalAdena)
                || !visitorAfter.add(InventoryCatalog.find(InventoryCatalog.ADENA), totalAdena)) {
            return rejectedPrivateShopTransaction();
        }

        visitor.persistPrivateShopPair(owner, visitorAfter, ownerAfter);
        visitor.tradeInstallInventory(visitorAfter);
        owner.tradeInstallInventory(ownerAfter);
        for (Map.Entry<Integer, Long> entry : consumed.entrySet()) {
            if (view.type == 0) shop.sellOffer(entry.getKey()).remaining -= entry.getValue();
            else shop.buyOffer(entry.getKey()).remaining -= entry.getValue();
        }
        shop.generation = nextPrivateShopGeneration++;
        invalidatePrivateShopViews(owner);
        boolean closed = shop.exhausted();
        List<ClientSession> closeRecipients = List.of();
        if (closed) {
            Room room = rooms.get(shop.roomId);
            closeRecipients = room == null ? List.of(owner) : new ArrayList<>(room.combatants());
            privateShops.remove(owner);
        }
        return new PrivateShopTransactionOutcome(true, owner,
                visitorBefore, visitorAfter.copy(), ownerBefore, ownerAfter.copy(),
                closed, closeRecipients);
    }

    private static PrivateShopViewOutcome rejectedPrivateShopView(
            Protocol.PersonalShopQuery query) {
        return new PrivateShopViewOutcome(false, query == null ? -1 : query.type(),
                query == null ? 0 : query.targetObjectId(), List.of(), List.of());
    }

    private static PrivateShopTransactionOutcome rejectedPrivateShopTransaction() {
        return new PrivateShopTransactionOutcome(false, null,
                null, null, null, null, false, List.of());
    }

    private void removePrivateShop(ClientSession owner) {
        if (owner == null) return;
        privateShops.remove(owner);
        invalidatePrivateShopViews(owner);
    }

    private void invalidatePrivateShopViews(ClientSession owner) {
        privateShopViews.entrySet().removeIf(entry -> entry.getValue().shop.owner == owner);
    }

    private static long catalogCount(RuntimeInventory inventory, long catalogId) {
        return inventory.items().stream()
                .filter(item -> item.definition().catalogId() == catalogId)
                .mapToLong(RuntimeInventory.Item::quantity).sum();
    }

    private static int distance(ClientSession first, ClientSession second) {
        return Math.max(Math.abs(first.tradeX() - second.tradeX()),
                Math.abs(first.tradeY() - second.tradeY()));
    }

    /** Starts the exact question-252 player exchange invitation. */
    synchronized ClientSession beginPlayerTrade(long roomId, ClientSession requester,
                                                 long targetObjectId) {
        Room room = rooms.get(roomId);
        expirePendingPlayerTrade(requester, System.nanoTime());
        if (room == null || !room.battleStarted || requester == null
                || playerTrades.containsKey(requester)
                || privateShops.containsKey(requester)
                || !requester.tradeEligible(roomId)) return null;
        ClientSession target = room.combatants().stream()
                .filter(candidate -> candidate != requester
                        && candidate.playerObjectId() == targetObjectId)
                .findFirst().orElse(null);
        expirePendingPlayerTrade(target, System.nanoTime());
        if (target == null || playerTrades.containsKey(target)
                || privateShops.containsKey(target)
                || !target.tradeEligible(roomId)
                || Math.max(Math.abs(requester.tradeX() - target.tradeX()),
                            Math.abs(requester.tradeY() - target.tradeY())) > 1) return null;
        PlayerTrade trade = new PlayerTrade(roomId, requester, target,
                System.nanoTime() + TRADE_INVITE_TIMEOUT_NANOS);
        playerTrades.put(requester, trade);
        playerTrades.put(target, trade);
        return target;
    }

    synchronized TradeAnswerOutcome answerPlayerTrade(ClientSession invitee, boolean accepted) {
        PlayerTrade trade = playerTrades.get(invitee);
        if (trade == null || trade.state != PlayerTradeState.PENDING
                || trade.invitee != invitee) {
            return new TradeAnswerOutcome(TradeAnswerStatus.INVALID, null, null);
        }
        if (System.nanoTime() - trade.expiresAtNanos >= 0 || !accepted
                || !trade.initiator.tradeEligible(trade.roomId)
                || !trade.invitee.tradeEligible(trade.roomId)
                || Math.max(Math.abs(trade.initiator.tradeX() - trade.invitee.tradeX()),
                            Math.abs(trade.initiator.tradeY() - trade.invitee.tradeY())) > 1) {
            clearPlayerTrade(trade);
            return new TradeAnswerOutcome(TradeAnswerStatus.REJECTED,
                    trade.initiator, trade.invitee);
        }
        trade.state = PlayerTradeState.OPEN;
        trade.display.put(trade.initiator, trade.initiator.tradeInventoryCopy());
        trade.display.put(trade.invitee, trade.invitee.tradeInventoryCopy());
        return new TradeAnswerOutcome(TradeAnswerStatus.OPENED,
                trade.initiator, trade.invitee);
    }

    /** A newer native S_ASK supersedes only an unopened prompt, never an open trade. */
    synchronized boolean cancelPendingPlayerTradePrompt(ClientSession invitee) {
        PlayerTrade trade = playerTrades.get(invitee);
        if (trade == null || trade.state != PlayerTradeState.PENDING || trade.invitee != invitee) {
            return false;
        }
        clearPlayerTrade(trade);
        return true;
    }

    synchronized TradeOfferOutcome addPlayerTradeItem(ClientSession owner, long objectId,
                                                       long quantity) throws IOException {
        PlayerTrade trade = playerTrades.get(owner);
        if (trade == null || trade.state != PlayerTradeState.OPEN
                || !trade.confirmed.isEmpty() || quantity <= 0
                || quantity > Integer.MAX_VALUE) return null;
        ClientSession partner = trade.other(owner);
        if (partner == null || !owner.tradeEligible(trade.roomId)
                || !partner.tradeEligible(trade.roomId)) return null;
        List<TradeOffer> offers = trade.offers.get(owner);
        if (offers.size() >= 16) return null;

        RuntimeInventory actual = owner.tradeInventoryCopy();
        RuntimeInventory.Item item = actual.item(objectId);
        if (item == null || item.equipped() || !item.transferable()) return null;
        long alreadyOffered = offers.stream()
                .filter(offer -> offer.objectId == objectId)
                .mapToLong(offer -> offer.quantity).sum();
        if (alreadyOffered + quantity > item.quantity()) return null;

        // The classic source performs target-capacity admission on every add.
        // Re-run the complete one-way projection so repeated stack offers
        // cannot bypass the native 256-slot or signed-count bounds.
        RuntimeInventory projectedTarget = partner.tradeInventoryCopy();
        for (TradeOffer offer : offers) {
            RuntimeInventory admitted = admitIncoming(projectedTarget, partner,
                    offer.item, offer.quantity, owner, "player-trade");
            if (admitted == null) return null;
            projectedTarget = admitted;
        }
        RuntimeInventory admitted = admitIncoming(projectedTarget, partner,
                item, quantity, owner, "player-trade");
        if (admitted == null) return null;

        RuntimeInventory beforeDisplay = trade.display.get(owner).copy();
        RuntimeInventory afterDisplay = beforeDisplay.copy();
        if (!afterDisplay.remove(objectId, quantity)) return null;
        trade.display.put(owner, afterDisplay);
        offers.add(new TradeOffer(objectId, quantity, item));
        return new TradeOfferOutcome(owner, partner, item, quantity,
                beforeDisplay, afterDisplay.copy());
    }

    private static RuntimeInventory admitIncoming(
            RuntimeInventory current, ClientSession recipient,
            RuntimeInventory.Item item, long quantity,
            ClientSession exchangeOwner, String source) throws IOException {
        RuntimeInventory admitted = current.copy();
        if (item == null || !admitted.add(item, quantity)) {
            if (exchangeOwner == null) recipient.sendInventoryCapacityOver(source);
            else {
                recipient.sendTradeAdmissionRejected(true, false, source);
                exchangeOwner.sendTradeAdmissionRejected(false, false, source);
            }
            return null;
        }
        if (!recipient.permitsInventoryAddition(current, item.definition(), quantity)) {
            if (exchangeOwner == null) recipient.sendInventoryWeightOver(source);
            else {
                recipient.sendTradeAdmissionRejected(true, true, source);
                exchangeOwner.sendTradeAdmissionRejected(false, true, source);
            }
            return null;
        }
        return admitted;
    }

    synchronized TradeTerminalOutcome confirmPlayerTrade(ClientSession participant) {
        PlayerTrade trade = playerTrades.get(participant);
        if (trade == null || trade.state != PlayerTradeState.OPEN
                || !trade.confirmed.add(participant)) {
            return new TradeTerminalOutcome(TradeConfirmStatus.INVALID,
                    List.of(), List.of(), false);
        }
        if (trade.confirmed.size() < 2) {
            return new TradeTerminalOutcome(TradeConfirmStatus.WAITING,
                    trade.participants(), List.of(), true);
        }

        RuntimeInventory initiatorActual = trade.initiator.tradeInventoryCopy();
        RuntimeInventory inviteeActual = trade.invitee.tradeInventoryCopy();
        RuntimeInventory initiatorNext = initiatorActual.copy();
        RuntimeInventory inviteeNext = inviteeActual.copy();
        boolean valid = trade.initiator.tradeEligible(trade.roomId)
                && trade.invitee.tradeEligible(trade.roomId)
                && removeTradeOffers(initiatorNext, trade.offers.get(trade.initiator))
                && removeTradeOffers(inviteeNext, trade.offers.get(trade.invitee))
                && addTradeOffers(inviteeNext, trade.offers.get(trade.initiator))
                && addTradeOffers(initiatorNext, trade.offers.get(trade.invitee));
        if (valid) {
            try {
                trade.initiator.persistPlayerTradePair(
                        trade.invitee, initiatorNext, inviteeNext);
            } catch (IOException failure) {
                valid = false;
                System.out.println("[LiTo] player trade atomic persistence failed initiator="
                        + trade.initiator.playerName() + " invitee="
                        + trade.invitee.playerName() + ": " + failure.getMessage());
            }
        }

        List<TradeInventoryChange> changes;
        TradeConfirmStatus status;
        if (valid) {
            trade.initiator.tradeInstallInventory(initiatorNext);
            trade.invitee.tradeInstallInventory(inviteeNext);
            changes = List.of(
                    new TradeInventoryChange(trade.initiator,
                            trade.display.get(trade.initiator).copy(), initiatorNext.copy()),
                    new TradeInventoryChange(trade.invitee,
                            trade.display.get(trade.invitee).copy(), inviteeNext.copy()));
            status = TradeConfirmStatus.COMPLETED;
        } else {
            changes = tradeRestoreChanges(trade, initiatorActual, inviteeActual);
            status = TradeConfirmStatus.FAILED;
        }
        List<ClientSession> participants = trade.participants();
        clearPlayerTrade(trade);
        return new TradeTerminalOutcome(status, participants, changes, true);
    }

    synchronized TradeTerminalOutcome cancelPlayerTrade(ClientSession participant) {
        PlayerTrade trade = playerTrades.get(participant);
        if (trade == null) {
            return new TradeTerminalOutcome(TradeConfirmStatus.INVALID,
                    List.of(), List.of(), false);
        }
        boolean open = trade.state == PlayerTradeState.OPEN;
        List<TradeInventoryChange> changes = open
                ? tradeRestoreChanges(trade, trade.initiator.tradeInventoryCopy(),
                        trade.invitee.tradeInventoryCopy())
                : List.of();
        List<ClientSession> participants = trade.participants();
        clearPlayerTrade(trade);
        return new TradeTerminalOutcome(TradeConfirmStatus.FAILED,
                participants, changes, open);
    }

    private static boolean removeTradeOffers(RuntimeInventory inventory,
                                             List<TradeOffer> offers) {
        for (TradeOffer offer : offers) {
            RuntimeInventory.Item current = inventory.item(offer.objectId);
            if (current == null || current.equipped() || !current.transferable()
                    || current.definition().catalogId()
                            != offer.item.definition().catalogId()
                    || !sameTransferState(current, offer.item)
                    || !inventory.remove(offer.objectId, offer.quantity)) return false;
        }
        return true;
    }

    private static boolean addTradeOffers(RuntimeInventory inventory,
                                           List<TradeOffer> offers) {
        for (TradeOffer offer : offers) {
            if (!inventory.add(offer.item, offer.quantity)) return false;
        }
        return true;
    }

    private static boolean sameTransferState(RuntimeInventory.Item first,
                                             RuntimeInventory.Item second) {
        return first.enchantLevel() == second.enchantLevel()
                && first.durability() == second.durability()
                && first.identified() == second.identified()
                && first.blessing() == second.blessing();
    }

    private static List<TradeInventoryChange> tradeRestoreChanges(
            PlayerTrade trade, RuntimeInventory initiatorActual,
            RuntimeInventory inviteeActual) {
        if (trade.state != PlayerTradeState.OPEN) return List.of();
        return List.of(
                new TradeInventoryChange(trade.initiator,
                        trade.display.get(trade.initiator).copy(), initiatorActual.copy()),
                new TradeInventoryChange(trade.invitee,
                        trade.display.get(trade.invitee).copy(), inviteeActual.copy()));
    }

    private void clearPlayerTrade(PlayerTrade trade) {
        playerTrades.remove(trade.initiator);
        playerTrades.remove(trade.invitee);
    }

    private void expirePendingPlayerTrade(ClientSession participant, long nowNanos) {
        if (participant == null) return;
        PlayerTrade trade = playerTrades.get(participant);
        if (trade != null && trade.state == PlayerTradeState.PENDING
                && nowNanos - trade.expiresAtNanos >= 0) clearPlayerTrade(trade);
    }

    synchronized List<ClientSession> battleResultCombatants(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null) return List.of();
        return room.battleRoster.isEmpty()
                ? new ArrayList<>(room.combatants())
                : new ArrayList<>(room.battleRoster);
    }

    synchronized boolean battleStarted(long roomId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted;
    }

    /**
     * Defers the native result controller until a lethal spell's rendered
     * effect has finished. The generation guard prevents a delayed task from
     * ever ending a later match that reused the same room id.
     */
    boolean scheduleBattleEndAfterTerminalEffect(
            long roomId, BattleRules.Resolution resolution, long delayMillis) {
        Objects.requireNonNull(resolution, "resolution");
        final long generation;
        synchronized (this) {
            Room room = rooms.get(roomId);
            if (room == null || !room.battleStarted
                    || room.terminalEffectPending || !resolution.ended()) return false;
            room.terminalEffectPending = true;
            generation = room.battleGeneration;
        }
        try {
            battleClock.schedule(
                    () -> concludeBattleAfterTerminalEffectSafely(
                            roomId, generation, resolution),
                    Math.max(0, delayMillis), TimeUnit.MILLISECONDS);
            return true;
        } catch (RuntimeException schedulingFailure) {
            synchronized (this) {
                Room room = rooms.get(roomId);
                if (room != null && room.battleGeneration == generation) {
                    room.terminalEffectPending = false;
                }
            }
            System.out.println("[LiTo] terminal effect result scheduling failed room="
                    + roomId + ": " + schedulingFailure.getMessage());
            return false;
        }
    }

    private void concludeBattleAfterTerminalEffectSafely(
            long roomId, long generation, BattleRules.Resolution resolution) {
        BattleEnd work;
        synchronized (this) {
            Room room = rooms.get(roomId);
            if (room == null || !room.battleStarted
                    || !room.terminalEffectPending
                    || room.battleGeneration != generation) return;
            work = new BattleEnd(roomId, new ArrayList<>(room.members), resolution);
        }
        try {
            for (ClientSession member : work.members()) {
                try {
                    member.serverEndBattle(work.resolution());
                } catch (IOException error) {
                    System.out.println("[LiTo] terminal effect result send failed recipient="
                            + member.playerName() + ": " + error.getMessage());
                }
            }
        } finally {
            synchronized (this) {
                Room room = rooms.get(roomId);
                if (room != null && room.battleStarted
                        && room.terminalEffectPending
                        && room.battleGeneration == generation) {
                    room.terminalEffectPending = false;
                    endBattle(roomId);
                }
            }
        }
    }

    synchronized RoomSettings roomSettings(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? defaultRoomSettings() : room.settings;
    }

    synchronized void endBattle(long roomId) {
        DurableBattleSettlement.requireDurable(this, roomId);
        Room room = rooms.get(roomId);
        if (room == null) return;
        for (ClientSession owner : new ArrayList<>(privateShops.keySet())) {
            PrivateShop shop = privateShops.get(owner);
            if (shop != null && shop.roomId == roomId) removePrivateShop(owner);
        }
        room.battleStarted = false;
        // S_D3 moves the current room into the native result lifecycle. Keep
        // C_CF closed until every result member confirms and leaves; joining
        // earlier would overlap two generations of one room id.
        room.resultPending = !room.members.isEmpty();
        room.terminalEffectPending = false;
        room.disconnectResolutionPending = false;
        room.disconnectedBattleCombatants.clear();
        room.battleGeneration++;
        room.ready.clear();
        room.botHp = NativeRoomRules.botHp(room, this);
        room.botArmBreakerStatus.clear();
        room.botJoyOfPainStatus.clear();
        room.botInventory.clear();
        room.groundEffects.clear();
        room.groundItems.clear();
        room.cookingFires.clear();
        room.cubeIgnitionEffects.clear();
        room.cubeQuakeEffects.clear();
        room.cubeShockEffects.clear();
        room.cubeHarmonyEffects.clear();
        room.patienceStatus.clear();
        room.shockStunEffects.clear();
        room.boneBreakEffects.clear();
        room.freezeEffects.clear();
        room.armorBreakExpiresAtNanos.clear();
        room.door.deactivate();
        room.siegeGate.deactivate();
        room.guardianTowers.deactivate();
        room.questEncounter.deactivate();
        room.questBoundary.deactivate();
        room.timeAttackEncounter.deactivate();
        room.petRace.reset();
        room.countdownValue = 0;
        room.countdownNextNanos = 0;
        room.match.reset();
    }

    synchronized BattleRules.Resolution forceWinner(long roomId, int winningForce) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted) {
            return BattleRules.Resolution.continuing(false, "builder-forcewin-invalid-room");
        }
        LinkedHashSet<Long> winners = new LinkedHashSet<>();
        LinkedHashSet<Long> losers = new LinkedHashSet<>();
        List<ClientSession> combatants = room.battleRoster.isEmpty()
                ? room.combatants() : new ArrayList<>(room.battleRoster);
        for (ClientSession combatant : combatants) {
            long objectId = combatant.playerObjectId();
            if (forceOf(room, combatant) == winningForce) winners.add(objectId);
            else losers.add(objectId);
        }
        List<RoomVirtualPlayer> virtuals = room.battleVirtualPlayers.isEmpty()
                ? room.virtualPlayers : room.battleVirtualPlayers;
        for (RoomVirtualPlayer combatant : virtuals) {
            if (combatant.force() == winningForce) winners.add(combatant.objectId());
            else losers.add(combatant.objectId());
        }
        // A single-player development battle represents ArenaBot as object 2,
        // force 1, without allocating a ClientSession.
        if (NativeRoomRules.bot(room.match.rules().mode(), this, room.id)
                && combatants.size() == 1 && virtuals.isEmpty() && winningForce == 1) winners.add(2L);
        return new BattleRules.Resolution(true, false, winners, losers, "builder-forcewin");
    }

    synchronized boolean toggleRoomSilence(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("unknown room: " + roomId);
        room.silenced = !room.silenced;
        return room.silenced;
    }

    synchronized boolean roomSilenced(long roomId) {
        Room room = rooms.get(roomId);
        return room != null && room.silenced;
    }

    synchronized int damageBot(long roomId, int damage) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted) return 0;
        room.botHp = Math.max(0, room.botHp - Math.max(0, damage));
        if (room.botHp == 0) {
            room.armorBreakExpiresAtNanos.remove(2L);
            room.botArmBreakerStatus.clear();
            room.botJoyOfPainStatus.clear();
        }
        return room.botHp;
    }

    synchronized int healBot(long roomId, int amount) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || room.botHp <= 0 || amount < 0) return 0;
        room.botHp = Math.min(ARENA_BOT_MAX_HP, room.botHp + amount);
        return room.botHp;
    }

    synchronized int botHp(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? 0 : room.botHp;
    }

    synchronized BattleArmBreakerStatus.Activation activateBotArmBreaker(
            long roomId, long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted || room.botHp <= 0
                ? BattleArmBreakerStatus.Activation.rejected()
                : room.botArmBreakerStatus.activateIfAbsent(
                        nowNanos, durationSeconds);
    }

    synchronized int botArmBreakerMeleeDamageModifier(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted || room.botHp <= 0 ? 0
                : room.botArmBreakerStatus.meleeDamageModifier(nowNanos);
    }

    synchronized boolean cancelBotArmBreaker(long roomId) {
        Room room = rooms.get(roomId);
        return room != null && room.botArmBreakerStatus.cancel();
    }

    synchronized BattleJoyOfPainStatus.Activation activateBotJoyOfPain(
            long roomId, long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted || room.botHp <= 0 ? null
                : room.botJoyOfPainStatus.activateOrRefresh(
                        nowNanos, durationSeconds);
    }

    synchronized BattleJoyOfPainStatus.Lease captureBotJoyOfPain(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted || room.botHp <= 0 ? null
                : room.botJoyOfPainStatus.capture(nowNanos);
    }

    synchronized boolean consumeBotJoyOfPain(
            long roomId, BattleJoyOfPainStatus.Lease lease) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && room.botHp > 0
                && room.botJoyOfPainStatus.consume(lease);
    }

    synchronized boolean activateArmorBreak(long roomId, long targetObjectId,
                                             long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || targetObjectId <= 0
                || durationSeconds <= 0) return false;
        room.armorBreakExpiresAtNanos.put(targetObjectId,
                nowNanos + TimeUnit.SECONDS.toNanos(durationSeconds));
        return true;
    }

    synchronized boolean armorBreakActive(long roomId, long targetObjectId,
                                           long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted) return false;
        Long expiresAt = room.armorBreakExpiresAtNanos.get(targetObjectId);
        if (expiresAt == null) return false;
        if (nowNanos - expiresAt < 0) return true;
        room.armorBreakExpiresAtNanos.remove(targetObjectId);
        return false;
    }

    synchronized boolean cancelArmorBreak(long roomId, long targetObjectId) {
        Room room = rooms.get(roomId);
        return room != null
                && room.armorBreakExpiresAtNanos.remove(targetObjectId) != null;
    }

    synchronized ArenaMapCatalog.Point botPosition(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || room.combatantCount() >= 2
                || room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                || room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK) return null;
        ArenaMapCatalog.Point spawn = ArenaMapCatalog
                .forMap(room.match.rules().mapId()).botSpawn();
        int[] offset = {0, 1, 0, -1};
        return new ArenaMapCatalog.Point(spawn.x() + offset[room.botStep & 3], spawn.y());
    }

    synchronized BattleGroundEffects.CastResult placeGroundSpell(
            long roomId, ClientSession caster, BattleSpellCatalog.GroundSpell spell,
            int sourceX, int sourceY, int targetX, int targetY, long nowNanos) {
        return placeGroundSpell(roomId, caster, spell, sourceX, sourceY,
                targetX, targetY, nowNanos, () -> {});
    }

    synchronized BattleGroundEffects.CastResult placeGroundSpell(
            long roomId, ClientSession caster, BattleSpellCatalog.GroundSpell spell,
            int sourceX, int sourceY, int targetX, int targetY, long nowNanos,
            Runnable resourceCommit) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster) || spell == null
                || resourceCommit == null || nowNanos < 0) {
            return BattleGroundEffects.CastResult.rejected();
        }
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(room.match.rules().mapId());
        if (!arena.nativeStandingFloor(sourceX, sourceY)
                || spell.skillId() != BattleSpellCatalog.FIRE_WALL
                    && !arena.nativeStandingFloor(targetX, targetY)
                || room.door.blocks(targetX, targetY)
                || room.siegeGate.blocks(targetX, targetY)
                || room.guardianTowers.blocks(targetX, targetY)
                || room.questEncounter.blocks(
                        forceOf(room, caster), targetX, targetY)
                || room.timeAttackEncounter.blocks(targetX, targetY)) {
            return BattleGroundEffects.CastResult.rejected();
        }
        BattleGroundEffects.CastResult placed = room.groundEffects.cast(
                spell, caster.playerObjectId(),
                sourceX, sourceY, targetX, targetY, arena, nowNanos);
        if (placed.accepted()) resourceCommit.run();
        return placed;
    }

    synchronized ClassicWorldGroundEffects.CastResult placeClassicWorldGroundSpell(
            int mapId, int instanceId, ClientSession caster,
            BattleSpellCatalog.GroundSpell spell,
            int sourceX, int sourceY, int targetX, int targetY,
            long nowNanos) {
        return placeClassicWorldGroundSpell(mapId, instanceId, caster, spell,
                sourceX, sourceY, targetX, targetY, nowNanos, () -> {});
    }

    synchronized ClassicWorldGroundEffects.CastResult placeClassicWorldGroundSpell(
            int mapId, int instanceId, ClientSession caster,
            BattleSpellCatalog.GroundSpell spell,
            int sourceX, int sourceY, int targetX, int targetY,
            long nowNanos, Runnable resourceCommit) {
        ClassicWorldLocation location = classicWorldMembership.get(caster);
        if (location == null || location.mapId() != mapId
                || location.instanceId() != instanceId
                || location.x() != sourceX || location.y() != sourceY
                || spell == null || resourceCommit == null || nowNanos < 0) {
            return ClassicWorldGroundEffects.CastResult.rejected();
        }
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(mapId);
        if (!map.standingPassable(sourceX, sourceY)
                || !map.contains(targetX, targetY)
                || spell.skillId() != BattleSpellCatalog.FIRE_WALL
                    && !map.standingPassable(targetX, targetY)) {
            return ClassicWorldGroundEffects.CastResult.rejected();
        }
        ClassicWorldEffectKey key = new ClassicWorldEffectKey(mapId, instanceId);
        ClassicWorldGroundEffects.CastResult placed =
                classicWorldGroundEffects.computeIfAbsent(
                key, ignored -> new ClassicWorldGroundEffects()).cast(
                        spell, caster.playerObjectId(), sourceX, sourceY,
                        targetX, targetY, map, nowNanos);
        if (placed.accepted()) resourceCommit.run();
        return placed;
    }

    synchronized BattleGroundEffects.CastResult placeTimeAttackGroundSpell(
            long roomId, BattleTimeAttackEncounter.Snapshot caster,
            BattleTimeAttackMagicSkillCatalog.GroundDefinition skill,
            int targetX, int targetY, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                || caster == null || !caster.alive() || skill == null
                || caster.npcId() != skill.npcId()) {
            return BattleGroundEffects.CastResult.rejected();
        }
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                room.match.rules().mapId());
        List<ArenaMapCatalog.Point> points = skill.points(targetX, targetY, arena);
        return room.groundEffects.castPattern(skill.skillId(), caster.objectId(),
                points, 0, skill.durationMillis(), skill.pulseIntervalMillis(),
                arena, nowNanos);
    }

    synchronized BattleCubeIgnitionEffects.Placement placeCubeIgnition(
            long roomId, ClientSession caster, int x, int y, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.phase() != BattleRules.Phase.RUNNING) {
            return BattleCubeIgnitionEffects.Placement.rejected();
        }
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                room.match.rules().mapId());
        if (!arena.contains(x, y) || room.door.blocks(x, y)
                || room.siegeGate.blocks(x, y)
                || room.guardianTowers.blocks(x, y)
                || room.questEncounter.blocks(forceOf(room, caster), x, y)
                || room.timeAttackEncounter.blocks(x, y)) {
            return BattleCubeIgnitionEffects.Placement.rejected();
        }
        return room.cubeIgnitionEffects.place(
                caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeIgnitionEffects.Cube> cubeIgnitions(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? List.of()
                : room.cubeIgnitionEffects.active(nowNanos);
    }

    synchronized int cubeIgnitionFireResistance(
            long roomId, ClientSession target, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || target == null ? 0
                : room.cubeIgnitionEffects.fireResistance(
                        target.playerObjectId(), nowNanos);
    }

    synchronized BattleCubeQuakeEffects.Placement placeCubeQuake(
            long roomId, ClientSession caster, int x, int y, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.phase() != BattleRules.Phase.RUNNING) {
            return BattleCubeQuakeEffects.Placement.rejected();
        }
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                room.match.rules().mapId());
        if (!arena.contains(x, y) || room.door.blocks(x, y)
                || room.siegeGate.blocks(x, y)
                || room.guardianTowers.blocks(x, y)
                || room.questEncounter.blocks(forceOf(room, caster), x, y)
                || room.timeAttackEncounter.blocks(x, y)) {
            return BattleCubeQuakeEffects.Placement.rejected();
        }
        return room.cubeQuakeEffects.place(
                caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeQuakeEffects.Cube> cubeQuakes(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? List.of()
                : room.cubeQuakeEffects.active(nowNanos);
    }

    synchronized int cubeQuakeEarthResistance(
            long roomId, ClientSession target, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || target == null ? 0
                : room.cubeQuakeEffects.earthResistance(
                        target.playerObjectId(), nowNanos);
    }

    synchronized boolean cubeQuakeMovementStopped(
            long roomId, ClientSession target, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && target != null
                && room.cubeQuakeEffects.movementStopped(
                        target.playerObjectId(), nowNanos);
    }

    synchronized BattleCubeShockEffects.Placement placeCubeShock(
            long roomId, ClientSession caster, int x, int y, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.phase() != BattleRules.Phase.RUNNING) {
            return BattleCubeShockEffects.Placement.rejected();
        }
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                room.match.rules().mapId());
        if (!arena.contains(x, y) || room.door.blocks(x, y)
                || room.siegeGate.blocks(x, y)
                || room.guardianTowers.blocks(x, y)
                || room.questEncounter.blocks(forceOf(room, caster), x, y)
                || room.timeAttackEncounter.blocks(x, y)) {
            return BattleCubeShockEffects.Placement.rejected();
        }
        return room.cubeShockEffects.place(
                caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeShockEffects.Cube> cubeShocks(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? List.of()
                : room.cubeShockEffects.active(nowNanos);
    }

    synchronized int cubeShockWindResistance(
            long roomId, ClientSession target, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || target == null ? 0
                : room.cubeShockEffects.windResistance(
                        target.playerObjectId(), nowNanos);
    }

    synchronized int cubeShockMagicResistanceModifier(
            long roomId, long targetObjectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null ? 0
                : room.cubeShockEffects.magicResistanceModifier(
                        targetObjectId, nowNanos);
    }

    synchronized BattleCubeHarmonyEffects.Placement placeCubeHarmony(
            long roomId, ClientSession caster, int x, int y, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.phase() != BattleRules.Phase.RUNNING) {
            return BattleCubeHarmonyEffects.Placement.rejected();
        }
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                room.match.rules().mapId());
        if (!arena.contains(x, y) || room.door.blocks(x, y)
                || room.siegeGate.blocks(x, y)
                || room.guardianTowers.blocks(x, y)
                || room.questEncounter.blocks(forceOf(room, caster), x, y)
                || room.timeAttackEncounter.blocks(x, y)) {
            return BattleCubeHarmonyEffects.Placement.rejected();
        }
        return room.cubeHarmonyEffects.place(
                caster.playerObjectId(), x, y, nowNanos);
    }

    synchronized List<BattleCubeHarmonyEffects.Cube> cubeHarmonies(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? List.of()
                : room.cubeHarmonyEffects.active(nowNanos);
    }

    synchronized boolean cubeHarmonyStatus(
            long roomId, long targetObjectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null
                && room.cubeHarmonyEffects.hasStatus(targetObjectId, nowNanos);
    }

    synchronized BattlePatienceStatus.Activation activatePatience(
            long roomId, long targetObjectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted
                || room.match.phase() != BattleRules.Phase.RUNNING) {
            return BattlePatienceStatus.Activation.rejected();
        }
        return room.patienceStatus.activate(
                targetObjectId, nowNanos, durationSeconds);
    }

    synchronized boolean patienceActive(
            long roomId, long targetObjectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null
                && room.patienceStatus.active(targetObjectId, nowNanos);
    }

    synchronized int patiencePhysicalReduction(
            long roomId, ClientSession target, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || target == null ? 0
                : room.patienceStatus.physicalReduction(
                        target.playerObjectId(), nowNanos);
    }

    synchronized boolean cancelPatience(long roomId, long targetObjectId) {
        Room room = rooms.get(roomId);
        return room != null && room.patienceStatus.cancel(targetObjectId);
    }

    synchronized boolean inLifeStream(long roomId, ClientSession player,
                                      int x, int y, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && player != null
                && room.battleRoster.contains(player)
                && room.groundEffects.contains(
                        BattleSpellCatalog.LIFE_STREAM, x, y, nowNanos);
    }

    synchronized boolean inClassicWorldLifeStream(
            int mapId, int instanceId, ClientSession player,
            int x, int y, long nowNanos) {
        ClassicWorldLocation location = classicWorldMembership.get(player);
        ClassicWorldGroundEffects effects = classicWorldGroundEffects.get(
                new ClassicWorldEffectKey(mapId, instanceId));
        return location != null && location.mapId() == mapId
                && location.instanceId() == instanceId
                && location.x() == x && location.y() == y
                && effects != null && effects.contains(
                        BattleSpellCatalog.LIFE_STREAM, x, y, nowNanos);
    }

    synchronized CookingFirePlacement placeCookingFire(
            long roomId, ClientSession source, int sourceX, int sourceY,
            int heading, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || source == null
                || !room.battleRoster.contains(source) || heading < 0 || heading > 7
                || nowNanos < 0) {
            return new CookingFirePlacement(CookingFireStatus.INVALID, null);
        }
        if (room.cookingFires.near(sourceX, sourceY, nowNanos)) {
            return new CookingFirePlacement(CookingFireStatus.NEARBY, null);
        }
        int[] headingX = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] headingY = {-1, -1, 0, 1, 1, 1, 0, -1};
        int x = sourceX + headingX[heading];
        int y = sourceY + headingY[heading];
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(room.match.rules().mapId());
        if (!arena.contains(x, y)) {
            return new CookingFirePlacement(CookingFireStatus.INVALID, null);
        }
        BattleCookingFires.Fire fire = room.cookingFires.place(x, y, nowNanos);
        return new CookingFirePlacement(fire == null
                ? CookingFireStatus.INVALID : CookingFireStatus.PLACED, fire);
    }

    synchronized boolean nearCookingFire(long roomId, ClientSession source,
                                          int x, int y, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && source != null
                && room.battleRoster.contains(source)
                && room.cookingFires.near(x, y, nowNanos);
    }

    synchronized List<BattleCookingFires.Fire> cookingFires(long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.cookingFires.active(nowNanos);
    }

    synchronized BattleShockStunEffects.Placement placeShockStunEffect(
            long roomId, long targetObjectId, int x, int y,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || targetObjectId == 0) return null;
        return room.shockStunEffects.place(
                targetObjectId, x, y, nowNanos, durationSeconds);
    }

    synchronized List<BattleShockStunEffects.Effect> shockStunEffects(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.shockStunEffects.active(nowNanos);
    }

    synchronized List<BattleShockStunEffects.Effect> expireShockStunEffects(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.shockStunEffects.expire(nowNanos);
    }

    synchronized BattleBoneBreakEffects.Effect placeBoneBreakEffect(
            long roomId, long targetObjectId, int x, int y,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || targetObjectId == 0) {
            return null;
        }
        return room.boneBreakEffects.placeIfAbsent(
                targetObjectId, x, y, nowNanos, durationSeconds);
    }

    synchronized List<BattleBoneBreakEffects.Effect> boneBreakEffects(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.boneBreakEffects.active(nowNanos);
    }

    synchronized List<BattleBoneBreakEffects.Effect> expireBoneBreakEffects(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.boneBreakEffects.expire(nowNanos);
    }

    synchronized BattleBoneBreakEffects.Effect removeBoneBreakEffect(
            long roomId, long targetObjectId) {
        Room room = rooms.get(roomId);
        return room == null ? null
                : room.boneBreakEffects.remove(targetObjectId);
    }

    synchronized BattleFreezeEffects.Placement placeFreezeEffect(
            long roomId, long targetObjectId, int x, int y,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || targetObjectId == 0) return null;
        return room.freezeEffects.place(
                targetObjectId, x, y, nowNanos, durationSeconds);
    }

    synchronized List<BattleFreezeEffects.Effect> freezeEffects(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.freezeEffects.active(nowNanos);
    }

    synchronized List<BattleFreezeEffects.Effect> expireFreezeEffects(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.freezeEffects.expire(nowNanos);
    }

    synchronized BattleFreezeEffects.Effect removeFreezeEffect(
            long roomId, long targetObjectId) {
        Room room = rooms.get(roomId);
        return room == null ? null : room.freezeEffects.remove(targetObjectId);
    }

    synchronized List<BattleGroundItems.Item> groundItems(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted) return List.of();
        return room.groundItems.items();
    }

    synchronized BattleDoor.Snapshot battleDoor(long roomId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? null : room.door.snapshot();
    }

    synchronized BattleDoor.Snapshot toggleBattleDoor(long roomId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? null : room.door.toggle();
    }

    synchronized boolean battleDoorBlocks(long roomId, int x, int y) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && room.door.blocks(x, y);
    }

    synchronized BattleDoor.Snapshot siegeGate(long roomId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? null : room.siegeGate.snapshot();
    }

    synchronized boolean siegeGateBlocks(long roomId, int x, int y) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && room.siegeGate.blocks(x, y);
    }

    synchronized boolean canUseGroundPortal(long roomId, ClientSession player) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && player != null
                && room.guardianTowers.siegePortalAllowed(forceOf(room, player));
    }

    synchronized int siegeAttackerForce(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? -1 : room.guardianTowers.siegeAttackerForce();
    }

    synchronized int battleSpawnIndex(long roomId, ClientSession player) {
        Room room = rooms.get(roomId);
        if (room == null || player == null) return 0;
        List<ClientSession> combatants = room.battleRoster.isEmpty()
                ? room.combatants() : new ArrayList<>(room.battleRoster);
        int fallback = Math.max(0, combatants.indexOf(player));
        if (room.match.rules().mode() != BattleRules.Mode.SIEGE_WAR) {
            return fallback;
        }
        int force = forceOf(room, player);
        int ordinal = 0;
        for (ClientSession candidate : combatants) {
            if (candidate == player) break;
            if (forceOf(room, candidate) == force) ordinal++;
        }
        boolean attacker = force == room.guardianTowers.siegeAttackerForce();
        // The exact production coordinates are server-owned.  Preserve role
        // independence from Force by assigning the two S32-safe anchor lanes
        // according to randomly selected attack/defence roles.
        return Math.floorMod(ordinal * 2 + (attacker ? 0 : 1), 8);
    }

    synchronized ArenaMapCatalog.Point battleSpawn(long roomId, int spawnIndex) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("unknown room for player spawn: " + roomId);
        }
        BattleRules.RuleSet rules = room.match.rules();
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(rules.mapId());
        return battleControls.playerSpawn(rules.mode(), arena, spawnIndex);
    }

    synchronized List<BattleGuardianTower.Snapshot> guardianTowers(long roomId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.guardianTowers.snapshots();
    }

    synchronized List<BattleGuardianTower.Snapshot> visibleGuardianTowers(
            long roomId, ClientSession recipient) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || recipient == null) return List.of();
        List<BattleGuardianTower.Snapshot> towers = room.guardianTowers.snapshots();
        if (room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) return towers;
        int force = forceOf(room, recipient);
        return towers.stream().filter(tower -> tower.force() == force).toList();
    }

    synchronized BattleGuardianTower.Snapshot guardianTower(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? null : room.guardianTowers.find(objectId);
    }

    synchronized boolean canAttackGuardianTower(long roomId, ClientSession attacker,
                                                  long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && attacker != null
                && room.battleRoster.contains(attacker)
                && room.guardianTowers.canAttack(objectId, forceOf(room, attacker));
    }

    synchronized boolean canHealGuardianTower(long roomId, ClientSession healer,
                                               long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && healer != null
                && room.battleRoster.contains(healer)
                && room.guardianTowers.canHeal(objectId, forceOf(room, healer));
    }

    synchronized BattleGuardianTower.Healing healGuardianTower(
            long roomId, ClientSession healer, long objectId, int amount) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || healer == null
                || !room.battleRoster.contains(healer) || amount <= 0) return null;
        return room.guardianTowers.heal(
                objectId, forceOf(room, healer), amount);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPainGuardianTower(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster) ? null
                : room.guardianTowers.joyOfPain(
                        objectId, forceOf(room, caster), nowNanos,
                        durationSeconds);
    }

    synchronized BattleGuardianTowers.Hit damageGuardianTower(
            long roomId, ClientSession attacker, long objectId, int damage) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || attacker == null
                || !room.battleRoster.contains(attacker) || damage <= 0) return null;
        int attackerForce = forceOf(room, attacker);
        boolean timeAttackStartedByLeader = room.match.rules().mode()
                == BattleRules.Mode.TIME_ATTACK
                && room.match.timeAttackLeader() == attacker.playerObjectId();
        String timeAttackStartRole = "";
        boolean questObjective = room.match.rules().mode()
                == BattleRules.Mode.QUEST_BATTLE
                && room.guardianTowers.isQuestObjective(objectId);
        BattleGuardianTowers.DamageResult damageResult = room.guardianTowers.damage(
                objectId, attackerForce, damage);
        if (damageResult == null) return null;
        BattleGuardianTower.Damage transition = damageResult.damage();
        BattleDoor.Snapshot openedSiegeGate = damageResult.siegeGateOpened()
                ? room.siegeGate.open() : null;
        List<BattleTimeAttackEncounter.Snapshot> spawned = List.of();
        List<BattleQuestEncounter.Snapshot> spawnedQuest = List.of();
        int completedQuestRoom = 0;
        BattleRules.Resolution resolution;
        if (transition.destroyedNow()
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK) {
            if (!room.match.startTimeAttack(System.nanoTime())) return null;
            spawned = room.timeAttackEncounter.startAfterTowerDestroyed(
                    timeAttackStartedByLeader);
            timeAttackStartRole = timeAttackStartedByLeader ? "leader" : "member";
            resolution = BattleRules.Resolution.continuing(
                    false, timeAttackStartedByLeader
                            ? "timeattack-start-tower-destroyed-by-leader"
                            : "timeattack-start-tower-destroyed-by-member-"
                                    + "conservative-no-dragon");
        } else if (damageResult.siegeStrongholdDestroyed()) {
            resolution = room.match.strongholdCaptured(
                    room.guardianTowers.siegeAttackerForce());
        } else if (transition.destroyedNow()
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE) {
            BattleQuestEncounter.ObjectiveCompletion completion =
                    questObjective
                            ? room.questEncounter.destroyObjective(attackerForce)
                            : null;
            if (completion == null) {
                throw new IllegalStateException("destroyed Quest tower is not the "
                        + "encounter's pending objective: object=" + objectId
                        + " force=" + attackerForce);
            }
            completedQuestRoom = completion.completedRoom();
            spawnedQuest = completion.spawned();
            room.guardianTowers.deactivateQuestObjective(objectId);
            resolution = completion.matchCompleted()
                    ? room.match.questFinalObjectiveDestroyed(
                            attackerForce,
                            transition.after().objectiveKind()
                                    == BattleGuardianTower.ObjectiveKind
                                            .QUEST_FINAL_SYMBOL)
                    : BattleRules.Resolution.continuing(
                            false, "questbattle-room-symbol-destroyed");
        } else if (transition.destroyedNow()
                && room.match.rules().mode() != BattleRules.Mode.SIEGE_WAR) {
            resolution = room.match.guardianTowerDestroyed(attackerForce);
        } else {
            resolution = BattleRules.Resolution.continuing(
                    false, "guardian-tower-damaged");
        }
        return new BattleGuardianTowers.Hit(
                transition, resolution, spawned, openedSiegeGate,
                spawnedQuest, completedQuestRoom, timeAttackStartRole);
    }

    synchronized boolean guardianTowerBlocks(long roomId, int x, int y) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && room.guardianTowers.blocks(x, y);
    }

    synchronized List<BattleQuestEncounter.Snapshot> visibleQuestMonsters(
            long roomId, ClientSession recipient) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || recipient == null
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) {
            return List.of();
        }
        return room.questEncounter.snapshots(forceOf(room, recipient));
    }

    synchronized BattleQuestEncounter.Snapshot questMonster(long roomId,
                                                               long objectId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? null : room.questEncounter.find(objectId);
    }

    synchronized boolean canAttackQuestMonster(long roomId, ClientSession attacker,
                                                 long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && attacker != null
                && room.battleRoster.contains(attacker)
                && room.questEncounter.canAttack(
                        objectId, forceOf(room, attacker));
    }

    synchronized BattleQuestEncounter.Hit damageQuestMonster(
            long roomId, ClientSession attacker, long objectId, int damage) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || attacker == null
                || !room.battleRoster.contains(attacker) || damage <= 0) return null;
        int force = forceOf(room, attacker);
        BattleQuestEncounter.Hit hit = room.questEncounter.damage(
                objectId, force, damage);
        if (hit == null) return null;
        if (hit.waveCleared()) advanceQuestWave(room, force,
                hit.clearedRoom(), hit.spawned(), hit.objectiveUnlocked());
        return hit;
    }

    synchronized boolean poisonQuestMonster(
            long roomId, ClientSession attacker, long objectId, long nowNanos,
            int durationSeconds, int tickIntervalMillis) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || attacker == null
                || !room.battleRoster.contains(attacker)
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) {
            return false;
        }
        return room.questEncounter.poison(
                objectId, forceOf(room, attacker), attacker.playerObjectId(),
                nowNanos, durationSeconds, tickIntervalMillis);
    }

    synchronized boolean questMonsterPoisonActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.poisonActive(objectId, nowNanos);
    }

    synchronized boolean cureQuestMonsterPoison(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.curePoison(objectId);
    }

    synchronized int questMonsterPoisonPalette(
            long roomId, long objectId, long nowNanos) {
        if (questMonsterFreezeActive(roomId, objectId, nowNanos)
                || questMonsterEarthBindActive(roomId, objectId, nowNanos)
                || questMonsterParalysisActive(roomId, objectId, nowNanos)) return 2;
        return questMonsterPoisonActive(roomId, objectId, nowNanos) ? 1 : 0;
    }

    synchronized List<QuestMonsterPoisonUpdate> pollQuestMonsterPoisons(
            long roomId, long nowNanos, int tickIntervalMillis, int tickDamage) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) {
            return List.of();
        }
        List<QuestMonsterPoisonUpdate> results = new ArrayList<>();
        for (BattleQuestEncounter.PoisonUpdate update
                : room.questEncounter.pollPoisons(
                        nowNanos, tickIntervalMillis, tickDamage)) {
            for (BattleQuestEncounter.Hit hit : update.hits()) {
                int force = hit.damage().after().force();
                if (hit.waveCleared()) advanceQuestWave(room, force,
                        hit.clearedRoom(), hit.spawned(),
                        hit.objectiveUnlocked());
            }
            ClientSession attacker = room.battleRoster.stream()
                    .filter(session -> session.playerObjectId()
                            == update.attackerObjectId())
                    .findFirst().orElse(null);
            results.add(new QuestMonsterPoisonUpdate(update, attacker));
        }
        return List.copyOf(results);
    }

    synchronized BattleQuestEncounter.Capture captureQuestMonster(
            long roomId, ClientSession caster, long objectId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) return null;
        int force = forceOf(room, caster);
        BattleQuestEncounter.Capture capture = room.questEncounter.capture(objectId, force);
        if (capture == null) return null;
        if (capture.waveCleared()) advanceQuestWave(room, force,
                capture.clearedRoom(), capture.spawned(),
                capture.objectiveUnlocked());
        return capture;
    }

    private static void advanceQuestWave(
            Room room, int force, int clearedRoom,
            List<BattleQuestEncounter.Snapshot> spawned,
            boolean objectiveUnlocked) {
        room.match.waveCleared(force);
        if (!spawned.isEmpty()) room.pveAiPublishAfterNanos = Long.MAX_VALUE;
        if (!objectiveUnlocked) return;
        ArenaMapCatalog.Point point = room.questEncounter.pendingObjectivePoint(force);
        BattleGuardianTower.ObjectiveKind objectiveKind =
                room.questEncounter.pendingObjectiveKind(force);
        BattleGuardianTower.Snapshot tower =
                room.guardianTowers.revealQuestObjective(
                        force, point, objectiveKind,
                        room.questEncounter.pendingObjectivePlacementSource(force));
        if (tower == null) {
            throw new IllegalStateException("failed to reveal Quest objective: "
                    + "force=" + force + " room=" + clearedRoom);
        }
    }

    synchronized BattleQuestEncounter.Snapshot releaseQuestTamedCompanion(
            long roomId, ClientSession owner,
            BattleQuestEncounter.Snapshot captured,
            BattleCompanion.Snapshot companion,
            List<RuntimeInventory.Item> carriedItems) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || owner == null
                || !room.battleRoster.contains(owner)
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) {
            return null;
        }
        return room.questEncounter.releaseTamed(
                captured, companion, carriedItems);
    }

    synchronized BattleQuestEncounter.Snapshot questCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) return null;
        return room.questEncounter.findCorpse(
                objectId, forceOf(room, caster), nowNanos);
    }

    synchronized BattleQuestEncounter.Snapshot consumeQuestCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) return null;
        return room.questEncounter.consumeCorpse(
                objectId, forceOf(room, caster), nowNanos);
    }

    synchronized BattleQuestEncounter.Resurrection resurrectQuestCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos) {
        return resurrectQuestCorpse(roomId, caster, objectId, nowNanos, 4);
    }

    synchronized BattleQuestEncounter.Resurrection resurrectQuestCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int hpDivisor) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) return null;
        return room.questEncounter.resurrectCorpse(
                objectId, forceOf(room, caster), nowNanos, hpDivisor);
    }

    synchronized BattleQuestEncounter.Healing healQuestMonster(
            long roomId, ClientSession healer, long objectId, int amount) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || healer == null
                || !room.battleRoster.contains(healer) || amount < 0) return null;
        return room.questEncounter.heal(objectId, forceOf(room, healer), amount);
    }

    synchronized BattleQuestEncounter.ManaDrain drainQuestMonsterMana(
            long roomId, ClientSession caster, long objectId,
            int sourceMaximumMp, int requested) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)) return null;
        return room.questEncounter.drainMana(
                objectId, forceOf(room, caster), sourceMaximumMp, requested);
    }

    synchronized int recoverQuestMonsterMana(
            long roomId, ClientSession caster, long objectId, int amount) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster) || amount <= 0) return 0;
        return room.questEncounter.recoverMana(objectId, amount);
    }

    synchronized BattleQuestEncounter.ManaDrain
            drainQuestMonsterManaIfStrictlyAbove(
                    long roomId, ClientSession caster, long objectId,
                    int sourceMaximumMp, int requested) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)) return null;
        return room.questEncounter.drainManaIfStrictlyAbove(
                objectId, forceOf(room, caster), sourceMaximumMp, requested);
    }

    synchronized boolean hasteQuestMonster(long roomId, ClientSession caster,
                                            long objectId, long nowNanos,
                                            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.haste(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized BattleQuestEncounter.MagicCancellation cancelQuestMonsterMagic(
            long roomId, ClientSession caster, long objectId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) {
            return null;
        }
        return room.questEncounter.cancelMagicEffects(
                objectId, forceOf(room, caster));
    }

    synchronized boolean questMonsterHasteActive(long roomId, long objectId,
                                                   long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.hasteActive(objectId, nowNanos);
    }

    synchronized boolean slowQuestMonster(long roomId, ClientSession caster,
                                           long objectId, long nowNanos,
                                           int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.slow(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterSlowActive(long roomId, long objectId,
                                                  long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.slowActive(objectId, nowNanos);
    }

    synchronized List<BattleQuestEncounter.Snapshot> expireQuestMonsterHastes(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE
                ? List.of() : room.questEncounter.expireHastes(nowNanos);
    }

    synchronized List<BattleQuestEncounter.Snapshot> expireQuestMonsterSlows(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE
                ? List.of() : room.questEncounter.expireSlows(nowNanos);
    }

    synchronized boolean blindQuestMonster(long roomId, ClientSession caster,
                                            long objectId, long nowNanos,
                                            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.blind(
                         objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized int addWeaponBreakStackQuestMonster(
            long roomId, ClientSession caster, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.addWeaponBreakStack(
                        objectId, forceOf(room, caster)) : -1;
    }

    synchronized int questMonsterWeaponBreakStacks(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.weaponBreakStacks(objectId) : 0;
    }

    synchronized boolean weaknessQuestMonster(long roomId, ClientSession caster,
                                               long objectId, long nowNanos,
                                               int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.weakness(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean diseaseQuestMonster(long roomId, ClientSession caster,
                                              long objectId, long nowNanos,
                                              int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.disease(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean silenceQuestMonster(long roomId, ClientSession caster,
                                              long objectId, long nowNanos,
                                              int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.silence(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean confusionSilenceQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.confusionSilence(
                        objectId, forceOf(room, caster), nowNanos,
                        durationSeconds);
    }

    synchronized boolean areaSilenceQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.areaSilence(
                        objectId, forceOf(room, caster),
                        nowNanos, durationSeconds);
    }

    synchronized boolean windShackleQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.windShackle(
                        objectId, forceOf(room, caster),
                        nowNanos, durationSeconds);
    }

    synchronized boolean decayPotionQuestMonster(long roomId, ClientSession caster,
                                                  long objectId, long nowNanos,
                                                  int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.decayPotion(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterDecayPotionActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.decayPotionActive(objectId, nowNanos);
    }

    synchronized boolean polluteWaterQuestMonster(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.polluteWater(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterPolluteWaterActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.polluteWaterActive(objectId, nowNanos);
    }

    synchronized boolean strikerGaleQuestMonster(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.strikerGale(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterStrikerGaleActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.strikerGaleActive(objectId, nowNanos);
    }

    synchronized boolean elementalFallDownQuestMonster(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds, int sourceElfAttribute) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.elementalFallDown(
                        objectId, forceOf(room, caster), nowNanos,
                        durationSeconds, sourceElfAttribute);
    }

    synchronized int questMonsterElementalFallDownModifier(
            long roomId, long objectId, int attributeKind, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.elementalFallDownModifier(
                        objectId, attributeKind, nowNanos) : 0;
    }

    synchronized boolean eraseMagicQuestMonster(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.eraseMagic(
                        objectId, forceOf(room, caster), nowNanos,
                        durationSeconds);
    }

    synchronized int questMonsterEffectiveMagicResistance(
            long roomId, long objectId, int baseMagicResistance,
            long nowNanos) {
        Room room = rooms.get(roomId);
        int effective = room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.effectiveMagicResistance(
                        objectId, baseMagicResistance, nowNanos)
                : baseMagicResistance;
        return Math.max(0, effective + (room == null ? 0
                : room.cubeShockEffects.magicResistanceModifier(
                        objectId, nowNanos)));
    }

    synchronized boolean consumeQuestMonsterEraseMagic(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.consumeEraseMagic(objectId, nowNanos);
    }

    synchronized BattleEraseMagicStatus.Lease captureQuestMonsterEraseMagic(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.captureEraseMagic(objectId, nowNanos)
                : BattleEraseMagicStatus.Lease.none();
    }

    synchronized boolean consumeQuestMonsterEraseMagic(
            long roomId, long objectId, BattleEraseMagicStatus.Lease lease) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                && room.questEncounter.consumeEraseMagic(objectId, lease);
    }

    synchronized boolean consumeQuestMonsterSilenceForSpell(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.consumeSilenceForSpell(objectId, nowNanos);
    }

    synchronized boolean sleepQuestMonster(long roomId, ClientSession caster,
                                            long objectId, long nowNanos,
                                            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.sleep(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean cancelQuestMonsterSleep(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.cancelSleep(objectId);
    }

    synchronized boolean freezeQuestMonster(long roomId, ClientSession caster,
                                             long objectId, long nowNanos,
                                             int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.freeze(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterFreezeActive(long roomId, long objectId,
                                                   long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.freezeActive(objectId, nowNanos);
    }

    synchronized boolean cancelQuestMonsterFreeze(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.cancelFreeze(objectId);
    }

    synchronized List<BattleQuestEncounter.Snapshot> expireQuestMonsterFreezes(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.questEncounter.expireFreezes(nowNanos);
    }

    synchronized boolean paralyzeQuestMonster(long roomId, ClientSession caster,
                                                long objectId, long nowNanos,
                                                int durationMillis) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.paralysis(
                        objectId, forceOf(room, caster), nowNanos, durationMillis);
    }

    synchronized boolean questMonsterParalysisActive(long roomId, long objectId,
                                                       long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.paralysisActive(objectId, nowNanos);
    }

    synchronized boolean shockStunQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.shockStun(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterShockStunActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.shockStunActive(objectId, nowNanos);
    }

    synchronized boolean boneBreakQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.boneBreak(
                        objectId, forceOf(room, caster),
                        nowNanos, durationSeconds);
    }

    synchronized BattleArmBreakerStatus.Activation armBreakerQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                ? room.questEncounter.armBreaker(
                        objectId, forceOf(room, caster),
                        nowNanos, durationSeconds)
                : BattleArmBreakerStatus.Activation.rejected();
    }

    synchronized int questMonsterArmBreakerMeleeDamageModifier(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? 0
                : room.questEncounter.armBreakerMeleeDamageModifier(
                        objectId, nowNanos);
    }

    synchronized boolean cancelQuestMonsterArmBreaker(
            long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.cancelArmBreaker(objectId);
    }

    synchronized boolean panicQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds, int abilityScoreModifier) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.panic(
                        objectId, forceOf(room, caster), nowNanos,
                        durationSeconds, abilityScoreModifier);
    }

    synchronized int questMonsterPanicAbilityScoreModifier(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? 0
                : room.questEncounter.panicAbilityScoreModifier(
                        objectId, nowNanos);
    }

    synchronized boolean cancelQuestMonsterPanic(
            long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.cancelPanic(objectId);
    }

    synchronized List<BattleQuestEncounter.Snapshot> expireQuestMonsterPanics(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? List.of()
                : room.questEncounter.expirePanics(nowNanos);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPainQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster) ? null
                : room.questEncounter.joyOfPain(
                        objectId, forceOf(room, caster), nowNanos,
                        durationSeconds);
    }

    synchronized BattleJoyOfPainStatus.Lease captureQuestMonsterJoyOfPain(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? null
                : room.questEncounter.captureJoyOfPain(objectId, nowNanos);
    }

    synchronized boolean consumeQuestMonsterJoyOfPain(
            long roomId, long objectId, BattleJoyOfPainStatus.Lease lease) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.consumeJoyOfPain(objectId, lease);
    }

    synchronized boolean questMonsterBoneBreakActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.boneBreakActive(objectId, nowNanos);
    }

    synchronized boolean thunderGrabQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.thunderGrab(
                        objectId, forceOf(room, caster),
                        nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterThunderGrabActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.thunderGrabActive(objectId, nowNanos);
    }

    synchronized boolean horrorOfDeathQuestMonster(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.horrorOfDeath(
                        objectId, forceOf(room, caster),
                        nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterHorrorOfDeathActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.horrorOfDeathActive(objectId, nowNanos);
    }

    synchronized List<BattleQuestEncounter.Snapshot>
            expireQuestMonsterHorrorOfDeaths(long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of()
                : room.questEncounter.expireHorrorOfDeaths(nowNanos);
    }

    synchronized boolean cancelQuestMonsterParalysis(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.cancelParalysis(objectId);
    }

    synchronized List<BattleQuestEncounter.Snapshot> expireQuestMonsterParalyses(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.questEncounter.expireParalyses(nowNanos);
    }

    synchronized boolean earthBindQuestMonster(long roomId, ClientSession caster,
                                                long objectId, long nowNanos,
                                                int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.questEncounter.earthBind(
                        objectId, forceOf(room, caster), nowNanos, durationSeconds);
    }

    synchronized boolean questMonsterEarthBindActive(long roomId, long objectId,
                                                      long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.questEncounter.earthBindActive(objectId, nowNanos);
    }

    synchronized List<BattleQuestEncounter.Snapshot> expireQuestMonsterEarthBinds(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.questEncounter.expireEarthBinds(nowNanos);
    }

    synchronized boolean questMonsterBlocks(long roomId, ClientSession mover,
                                               int x, int y) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && mover != null
                && room.questEncounter.blocks(forceOf(room, mover), x, y);
    }

    synchronized List<BattleQuestBoundary.Attribute> questBoundaryAttributes(
            long roomId, ClientSession recipient) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || recipient == null) return List.of();
        int force = forceOf(room, recipient);
        return room.questBoundary.attributes(
                force, room.questEncounter.roomNumber(force));
    }

    synchronized List<BattleQuestBoundary.Attribute> openedQuestBoundary(
            long roomId, int force, int clearedRoom) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.questBoundary.openedAfter(force, clearedRoom);
    }

    synchronized boolean questBoundaryBlocks(long roomId, ClientSession mover,
                                               int fromX, int fromY,
                                               int toX, int toY) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || mover == null) return false;
        int force = forceOf(room, mover);
        return room.questBoundary.blocks(force,
                room.questEncounter.roomNumber(force),
                fromX, fromY, toX, toY);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot> visibleTimeAttackActors(
            long roomId, ClientSession recipient) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || recipient == null
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return List.of();
        }
        return room.timeAttackEncounter.snapshots();
    }

    synchronized BattleTimeAttackEncounter.Snapshot timeAttackActor(
            long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? null : room.timeAttackEncounter.find(objectId);
    }

    synchronized boolean canAttackTimeAttackActor(
            long roomId, ClientSession attacker, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && attacker != null
                && room.battleRoster.contains(attacker)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.canAttack(objectId);
    }

    synchronized TimeAttackActorHit damageTimeAttackActor(
            long roomId, ClientSession attacker, long objectId, int damage) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || attacker == null
                || !room.battleRoster.contains(attacker) || damage <= 0
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return null;
        }
        BattleTimeAttackEncounter.Hit hit =
                room.timeAttackEncounter.damage(objectId, damage);
        if (hit == null) return null;
        if (!hit.spawned().isEmpty()) room.pveAiPublishAfterNanos = Long.MAX_VALUE;
        BattleRules.Resolution resolution = hit.finalBossesCleared()
                ? room.match.timeAttackCompleted(
                        BattleTimeAttackEncounter.isDragonStageActor(
                                hit.damage().before()))
                : BattleRules.Resolution.continuing(
                        false, hit.spawned().isEmpty()
                                ? "timeattack-actor-damaged"
                                : hit.spawned().stream().anyMatch(
                                        BattleTimeAttackEncounter::isDragonStageActor)
                                        ? "timeattack-dragon-stage-spawned"
                                : hit.spawned().stream().allMatch(
                                        BattleTimeAttackEncounter.Snapshot::finalBoss)
                                        ? "timeattack-ordinary-fragments-cleared"
                                        : "timeattack-next-ordinary-fragment");
        return new TimeAttackActorHit(hit, resolution);
    }

    synchronized boolean poisonTimeAttackActor(
            long roomId, ClientSession attacker, long objectId, long nowNanos,
            int durationSeconds, int tickIntervalMillis) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && attacker != null
                && room.battleRoster.contains(attacker)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.poison(
                        objectId, attacker.playerObjectId(), nowNanos,
                        durationSeconds, tickIntervalMillis);
    }

    synchronized boolean timeAttackActorPoisonActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.poisonActive(objectId, nowNanos);
    }

    synchronized boolean cureTimeAttackActorPoison(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.curePoison(objectId);
    }

    synchronized int timeAttackActorPoisonPalette(
            long roomId, long objectId, long nowNanos) {
        if (timeAttackActorFreezeActive(roomId, objectId, nowNanos)
                || timeAttackActorEarthBindActive(roomId, objectId, nowNanos)
                || timeAttackActorParalysisActive(roomId, objectId, nowNanos)) return 2;
        return timeAttackActorPoisonActive(roomId, objectId, nowNanos) ? 1 : 0;
    }

    synchronized List<TimeAttackPoisonUpdate> pollTimeAttackActorPoisons(
            long roomId, long nowNanos, int tickIntervalMillis, int tickDamage) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return List.of();
        }
        List<TimeAttackPoisonUpdate> results = new ArrayList<>();
        for (BattleTimeAttackEncounter.PoisonUpdate update
                : room.timeAttackEncounter.pollPoisons(
                        nowNanos, tickIntervalMillis, tickDamage)) {
            BattleRules.Resolution resolution = BattleRules.Resolution.continuing(
                    false, update.expired()
                            ? "timeattack-poison-expired" : "timeattack-poison-tick");
            for (BattleTimeAttackEncounter.Hit hit : update.hits()) {
                if (!hit.spawned().isEmpty()) {
                    room.pveAiPublishAfterNanos = Long.MAX_VALUE;
                }
                resolution = hit.finalBossesCleared()
                        ? room.match.timeAttackCompleted(
                                BattleTimeAttackEncounter.isDragonStageActor(
                                        hit.damage().before()))
                        : BattleRules.Resolution.continuing(false,
                                hit.spawned().isEmpty()
                                        ? "timeattack-actor-poisoned"
                                        : hit.spawned().stream().anyMatch(
                                                BattleTimeAttackEncounter::isDragonStageActor)
                                                ? "timeattack-dragon-stage-spawned-by-poison"
                                        : hit.spawned().stream().allMatch(
                                                BattleTimeAttackEncounter.Snapshot::finalBoss)
                                                ? "timeattack-ordinary-fragments-cleared-by-poison"
                                                : "timeattack-next-ordinary-fragment-by-poison");
            }
            ClientSession attacker = room.battleRoster.stream()
                    .filter(session -> session.playerObjectId()
                            == update.attackerObjectId())
                    .findFirst().orElse(null);
            results.add(new TimeAttackPoisonUpdate(update, attacker, resolution));
        }
        return List.copyOf(results);
    }

    synchronized BattleTimeAttackEncounter.Capture captureTimeAttackActor(
            long roomId, ClientSession caster, long objectId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) return null;
        BattleTimeAttackEncounter.Capture capture =
                room.timeAttackEncounter.capture(objectId);
        if (capture != null && !capture.spawned().isEmpty()) {
            room.pveAiPublishAfterNanos = Long.MAX_VALUE;
        }
        return capture;
    }

    synchronized BattleTimeAttackEncounter.Snapshot releaseTimeAttackTamedCompanion(
            long roomId, ClientSession owner,
            BattleTimeAttackEncounter.Snapshot captured,
            BattleCompanion.Snapshot companion,
            List<RuntimeInventory.Item> carriedItems) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || owner == null
                || !room.battleRoster.contains(owner)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return null;
        }
        return room.timeAttackEncounter.releaseTamed(
                captured, companion, carriedItems);
    }

    synchronized BattleTimeAttackEncounter.Snapshot timeAttackCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) return null;
        return room.timeAttackEncounter.findCorpse(objectId, nowNanos);
    }

    synchronized BattleTimeAttackEncounter.Snapshot consumeTimeAttackCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) return null;
        return room.timeAttackEncounter.consumeCorpse(objectId, nowNanos);
    }

    synchronized BattleTimeAttackEncounter.Resurrection resurrectTimeAttackCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos) {
        return resurrectTimeAttackCorpse(roomId, caster, objectId, nowNanos, 4);
    }

    synchronized BattleTimeAttackEncounter.Resurrection resurrectTimeAttackCorpse(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int hpDivisor) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) return null;
        return room.timeAttackEncounter.resurrectCorpse(
                objectId, nowNanos, hpDivisor);
    }

    synchronized BattleTimeAttackEncounter.Healing healTimeAttackActor(
            long roomId, ClientSession healer, long objectId, int amount) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || healer == null
                || !room.battleRoster.contains(healer) || amount < 0
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return null;
        }
        return room.timeAttackEncounter.heal(objectId, amount);
    }

    synchronized BattleTimeAttackEncounter.ManaDrain drainTimeAttackActorMana(
            long roomId, ClientSession caster, long objectId,
            int sourceMaximumMp, int requested) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return null;
        }
        return room.timeAttackEncounter.drainMana(
                objectId, sourceMaximumMp, requested);
    }

    synchronized int recoverTimeAttackActorMana(
            long roomId, ClientSession caster, long objectId, int amount) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster) || amount <= 0
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return 0;
        }
        return room.timeAttackEncounter.recoverMana(objectId, amount);
    }

    synchronized BattleTimeAttackEncounter.ManaDrain
            drainTimeAttackActorManaIfStrictlyAbove(
                    long roomId, ClientSession caster, long objectId,
                    int sourceMaximumMp, int requested) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return null;
        }
        return room.timeAttackEncounter.drainManaIfStrictlyAbove(
                objectId, sourceMaximumMp, requested);
    }

    synchronized boolean hasteTimeAttackActor(long roomId, ClientSession caster,
                                               long objectId, long nowNanos,
                                               int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.haste(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized BattleTimeAttackEncounter.MagicCancellation cancelTimeAttackActorMagic(
            long roomId, ClientSession caster, long objectId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK) {
            return null;
        }
        return room.timeAttackEncounter.cancelMagicEffects(objectId);
    }

    synchronized boolean timeAttackActorHasteActive(long roomId, long objectId,
                                                      long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.hasteActive(objectId, nowNanos);
    }

    synchronized boolean slowTimeAttackActor(long roomId, ClientSession caster,
                                              long objectId, long nowNanos,
                                              int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.slow(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorSlowActive(long roomId, long objectId,
                                                     long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.slowActive(objectId, nowNanos);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot> expireTimeAttackActorHastes(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? List.of() : room.timeAttackEncounter.expireHastes(nowNanos);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot> expireTimeAttackActorSlows(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? List.of() : room.timeAttackEncounter.expireSlows(nowNanos);
    }

    synchronized boolean blindTimeAttackActor(long roomId, ClientSession caster,
                                               long objectId, long nowNanos,
                                               int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.blind(
                         objectId, nowNanos, durationSeconds);
    }

    synchronized int addWeaponBreakStackTimeAttackActor(
            long roomId, ClientSession caster, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                ? room.timeAttackEncounter.addWeaponBreakStack(objectId) : -1;
    }

    synchronized int timeAttackActorWeaponBreakStacks(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                ? room.timeAttackEncounter.weaponBreakStacks(objectId) : 0;
    }

    synchronized boolean weaknessTimeAttackActor(long roomId, ClientSession caster,
                                                  long objectId, long nowNanos,
                                                  int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.weakness(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean diseaseTimeAttackActor(long roomId, ClientSession caster,
                                                 long objectId, long nowNanos,
                                                 int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.disease(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean silenceTimeAttackActor(long roomId, ClientSession caster,
                                                 long objectId, long nowNanos,
                                                 int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.silence(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean confusionSilenceTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.confusionSilence(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean areaSilenceTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.areaSilence(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean windShackleTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.windShackle(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean decayPotionTimeAttackActor(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.decayPotion(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorDecayPotionActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.decayPotionActive(objectId, nowNanos);
    }

    synchronized boolean polluteWaterTimeAttackActor(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.polluteWater(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorPolluteWaterActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.polluteWaterActive(objectId, nowNanos);
    }

    synchronized boolean strikerGaleTimeAttackActor(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.strikerGale(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorStrikerGaleActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.strikerGaleActive(objectId, nowNanos);
    }

    synchronized boolean elementalFallDownTimeAttackActor(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds, int sourceElfAttribute) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.elementalFallDown(
                        objectId, nowNanos, durationSeconds, sourceElfAttribute);
    }

    synchronized int timeAttackActorElementalFallDownModifier(
            long roomId, long objectId, int attributeKind, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                ? room.timeAttackEncounter.elementalFallDownModifier(
                        objectId, attributeKind, nowNanos) : 0;
    }

    synchronized boolean eraseMagicTimeAttackActor(
            long roomId, ClientSession caster, long objectId, long nowNanos,
            int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.eraseMagic(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized int timeAttackActorEffectiveMagicResistance(
            long roomId, long objectId, int baseMagicResistance,
            long nowNanos) {
        Room room = rooms.get(roomId);
        int effective = room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                ? room.timeAttackEncounter.effectiveMagicResistance(
                        objectId, baseMagicResistance, nowNanos)
                : baseMagicResistance;
        return Math.max(0, effective + (room == null ? 0
                : room.cubeShockEffects.magicResistanceModifier(
                        objectId, nowNanos)));
    }

    synchronized boolean consumeTimeAttackActorEraseMagic(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.consumeEraseMagic(objectId, nowNanos);
    }

    synchronized BattleEraseMagicStatus.Lease captureTimeAttackActorEraseMagic(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                ? room.timeAttackEncounter.captureEraseMagic(objectId, nowNanos)
                : BattleEraseMagicStatus.Lease.none();
    }

    synchronized boolean consumeTimeAttackActorEraseMagic(
            long roomId, long objectId, BattleEraseMagicStatus.Lease lease) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.consumeEraseMagic(objectId, lease);
    }

    synchronized boolean consumeTimeAttackActorSilenceForSpell(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.consumeSilenceForSpell(
                        objectId, nowNanos);
    }

    synchronized boolean sleepTimeAttackActor(long roomId, ClientSession caster,
                                               long objectId, long nowNanos,
                                               int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.sleep(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean cancelTimeAttackActorSleep(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.timeAttackEncounter.cancelSleep(objectId);
    }

    synchronized boolean freezeTimeAttackActor(long roomId, ClientSession caster,
                                                long objectId, long nowNanos,
                                                int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.freeze(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorFreezeActive(long roomId, long objectId,
                                                      long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.timeAttackEncounter.freezeActive(objectId, nowNanos);
    }

    synchronized boolean cancelTimeAttackActorFreeze(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.timeAttackEncounter.cancelFreeze(objectId);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot> expireTimeAttackActorFreezes(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.timeAttackEncounter.expireFreezes(nowNanos);
    }

    synchronized boolean paralyzeTimeAttackActor(long roomId, ClientSession caster,
                                                   long objectId, long nowNanos,
                                                   int durationMillis) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.paralysis(
                        objectId, nowNanos, durationMillis);
    }

    synchronized boolean timeAttackActorParalysisActive(long roomId, long objectId,
                                                          long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.paralysisActive(objectId, nowNanos);
    }

    synchronized boolean shockStunTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.shockStun(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorShockStunActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.shockStunActive(objectId, nowNanos);
    }

    synchronized boolean boneBreakTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.boneBreak(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized BattleArmBreakerStatus.Activation armBreakerTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                ? room.timeAttackEncounter.armBreaker(
                        objectId, nowNanos, durationSeconds)
                : BattleArmBreakerStatus.Activation.rejected();
    }

    synchronized int timeAttackActorArmBreakerMeleeDamageModifier(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? 0 : room.timeAttackEncounter.armBreakerMeleeDamageModifier(
                        objectId, nowNanos);
    }

    synchronized boolean cancelTimeAttackActorArmBreaker(
            long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.cancelArmBreaker(objectId);
    }

    synchronized boolean panicTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds, int abilityScoreModifier) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.panic(
                        objectId, nowNanos, durationSeconds,
                        abilityScoreModifier);
    }

    synchronized int timeAttackActorPanicAbilityScoreModifier(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? 0 : room.timeAttackEncounter.panicAbilityScoreModifier(
                        objectId, nowNanos);
    }

    synchronized boolean cancelTimeAttackActorPanic(
            long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.cancelPanic(objectId);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot>
            expireTimeAttackActorPanics(long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? List.of() : room.timeAttackEncounter.expirePanics(nowNanos);
    }

    synchronized BattleJoyOfPainStatus.Activation joyOfPainTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted || caster == null
                || !room.battleRoster.contains(caster)
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? null : room.timeAttackEncounter.joyOfPain(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized BattleJoyOfPainStatus.Lease captureTimeAttackActorJoyOfPain(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? null : room.timeAttackEncounter.captureJoyOfPain(
                        objectId, nowNanos);
    }

    synchronized boolean consumeTimeAttackActorJoyOfPain(
            long roomId, long objectId, BattleJoyOfPainStatus.Lease lease) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.consumeJoyOfPain(objectId, lease);
    }

    synchronized boolean timeAttackActorBoneBreakActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.boneBreakActive(
                        objectId, nowNanos);
    }

    synchronized boolean thunderGrabTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.thunderGrab(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorThunderGrabActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.thunderGrabActive(
                        objectId, nowNanos);
    }

    synchronized boolean horrorOfDeathTimeAttackActor(
            long roomId, ClientSession caster, long objectId,
            long nowNanos, int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.horrorOfDeath(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorHorrorOfDeathActive(
            long roomId, long objectId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.horrorOfDeathActive(
                        objectId, nowNanos);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot>
            expireTimeAttackActorHorrorOfDeaths(long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? List.of()
                : room.timeAttackEncounter.expireHorrorOfDeaths(nowNanos);
    }

    synchronized boolean cancelTimeAttackActorParalysis(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.cancelParalysis(objectId);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot> expireTimeAttackActorParalyses(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.TIME_ATTACK
                ? List.of() : room.timeAttackEncounter.expireParalyses(nowNanos);
    }

    synchronized boolean earthBindTimeAttackActor(long roomId, ClientSession caster,
                                                   long objectId, long nowNanos,
                                                   int durationSeconds) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted && caster != null
                && room.battleRoster.contains(caster)
                && room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && room.timeAttackEncounter.earthBind(
                        objectId, nowNanos, durationSeconds);
    }

    synchronized boolean timeAttackActorEarthBindActive(long roomId, long objectId,
                                                         long nowNanos) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.timeAttackEncounter.earthBindActive(objectId, nowNanos);
    }

    synchronized List<BattleTimeAttackEncounter.Snapshot> expireTimeAttackActorEarthBinds(
            long roomId, long nowNanos) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted
                ? List.of() : room.timeAttackEncounter.expireEarthBinds(nowNanos);
    }

    synchronized boolean timeAttackActorBlocks(long roomId, int x, int y) {
        Room room = rooms.get(roomId);
        return room != null && room.battleStarted
                && room.timeAttackEncounter.blocks(x, y);
    }

    synchronized BattleGroundItems.Item groundItem(long roomId, long objectId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? null : room.groundItems.item(objectId);
    }

    synchronized BattleGroundItems.Item putGroundItem(
            long roomId, ClientSession source, InventoryCatalog.Definition definition,
            long quantity, int x, int y) {
        return putGroundItem(roomId, source, definition, null, quantity, x, y);
    }

    synchronized BattleGroundItems.Item putGroundItem(
            long roomId, ClientSession source, RuntimeInventory.Item item,
            long quantity, int x, int y) {
        return putGroundItem(roomId, source,
                item == null ? null : item.definition(), item, quantity, x, y);
    }

    synchronized BattleGroundItems.Item putGroundItemPersisted(
            long roomId, ClientSession source, RuntimeInventory.Item item,
            long quantity, int x, int y, DurableCommit commit) throws IOException {
        Room room = validatedGroundRoom(roomId, source, x, y);
        return room == null || item == null ? null
                : room.groundItems.putPersisted(item, quantity, x, y, commit);
    }

    private BattleGroundItems.Item putGroundItem(
            long roomId, ClientSession source, InventoryCatalog.Definition definition,
            RuntimeInventory.Item item, long quantity, int x, int y) {
        Room room = validatedGroundRoom(roomId, source, x, y);
        if (room == null) return null;
        return item == null ? room.groundItems.put(definition, quantity, x, y)
                : room.groundItems.put(item, quantity, x, y);
    }

    private Room validatedGroundRoom(
            long roomId, ClientSession source, int x, int y) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || source == null
                || !room.battleRoster.contains(source)) return null;
        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(room.match.rules().mapId());
        if (!arena.contains(x, y) || room.door.blocks(x, y)
                || room.siegeGate.blocks(x, y)
                || room.guardianTowers.blocks(x, y)
                || room.questEncounter.blocks(forceOf(room, source), x, y)
                || room.timeAttackEncounter.blocks(x, y)) return null;
        return room;
    }

    synchronized GroundTake takeGroundItem(long roomId, ClientSession source,
                                            long objectId, long quantity) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || source == null
                || !room.battleRoster.contains(source)) {
            return null;
        }
        BattleGroundItems.Take take = room.groundItems.take(objectId, quantity);
        return take == null ? null : new GroundTake(take.taken(), take.remaining());
    }

    synchronized GroundTake takeGroundItemPersisted(
            long roomId, ClientSession source, long objectId, long quantity,
            DurableCommit commit) throws IOException {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || source == null
                || !room.battleRoster.contains(source)) return null;
        BattleGroundItems.Take take = room.groundItems.takePersisted(
                objectId, quantity, commit);
        return take == null ? null : new GroundTake(take.taken(), take.remaining());
    }

    /**
     * Transfers an item into the authoritative inventory of a receivable
     * world NPC.  The current isolated arena has one such object: the
     * single-player ArenaBot monster (object 2).  Two-player combatants are
     * PCs and are intentionally not accepted by C_GIVE.
     */
    synchronized NpcGiveResult giveToNpc(long roomId, long targetObjectId,
                                         int targetX, int targetY,
                                         InventoryCatalog.Definition definition,
                                         long quantity) {
        return giveToNpc(roomId, targetObjectId, targetX, targetY,
                definition, null, quantity);
    }

    synchronized NpcGiveResult giveToNpc(long roomId, long targetObjectId,
                                         int targetX, int targetY,
                                         RuntimeInventory.Item item, long quantity) {
        return giveToNpc(roomId, targetObjectId, targetX, targetY,
                item == null ? null : item.definition(), item, quantity);
    }

    synchronized NpcGiveResult canGiveToNpc(
            long roomId, long targetObjectId, int targetX, int targetY,
            RuntimeInventory.Item item, long quantity) {
        return validateNpcGive(roomId, targetObjectId, targetX, targetY,
                item == null ? null : item.definition(), item, quantity, false);
    }

    synchronized NpcGiveResult giveToNpcPersisted(
            long roomId, long targetObjectId, int targetX, int targetY,
            RuntimeInventory.Item item, long quantity, DurableCommit commit)
            throws IOException {
        NpcGiveResult validation = validateNpcGive(
                roomId, targetObjectId, targetX, targetY,
                item == null ? null : item.definition(), item, quantity, false);
        if (validation != NpcGiveResult.ACCEPTED || commit == null) return validation;
        Room room = rooms.get(roomId);
        RuntimeInventory staged = room.botInventory.copy();
        if (staged.addTransferredResult(item, quantity) == null) {
            throw new IllegalStateException(
                    "validated NPC inventory changed before durable C_GIVE commit");
        }
        commit.commit();
        room.botInventory = staged;
        return NpcGiveResult.ACCEPTED;
    }

    synchronized int npcLightRange(long roomId, long targetObjectId) {
        Room room = rooms.get(roomId);
        return room == null || targetObjectId != 2
                ? 0 : room.botInventory.activeLightRange();
    }

    private NpcGiveResult giveToNpc(long roomId, long targetObjectId,
                                    int targetX, int targetY,
                                    InventoryCatalog.Definition definition,
                                    RuntimeInventory.Item item, long quantity) {
        return validateNpcGive(roomId, targetObjectId, targetX, targetY,
                definition, item, quantity, true);
    }

    private NpcGiveResult validateNpcGive(
            long roomId, long targetObjectId, int targetX, int targetY,
            InventoryCatalog.Definition definition, RuntimeInventory.Item item,
            long quantity, boolean commit) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || room.combatantCount() != 1) {
            return NpcGiveResult.INVALID_PHASE;
        }
        if (targetObjectId != 2 || definition == null || quantity <= 0) {
            return NpcGiveResult.UNKNOWN_TARGET;
        }
        ArenaMapCatalog.Point current = botPosition(roomId);
        if (current == null || current.x() != targetX || current.y() != targetY) {
            return NpcGiveResult.STALE_POSITION;
        }
        RuntimeInventory target = commit ? room.botInventory : room.botInventory.copy();
        return (item == null ? target.add(definition, quantity)
                : target.addTransferredResult(item, quantity) != null)
                ? NpcGiveResult.ACCEPTED : NpcGiveResult.CAPACITY;
    }

    synchronized ChannelSnapshot channelSnapshot(int channelId) {
        Channel channel = channels.get(channelId);
        return channel == null ? null : snapshot(channel);
    }

    synchronized ReadyUpdate setReady(long roomId, ClientSession session, boolean requestedValue) {
        Room room = rooms.get(roomId);
        if (room == null || session.isObserver() || room.battleStarted) {
            return new ReadyUpdate(false, ReadyTransition.NONE, 0);
        }
        if (!room.match.rules().mode().isBattleScene()) {
            // Wire mode 6 is LTmapstr's native town category, not a match.
            // A future evidence-backed town transition must bootstrap its
            // scene directly; it must not fall through the battle countdown.
            return new ReadyUpdate(false, ReadyTransition.NONE, 0);
        }
        // The original LiTo room button emits D2 01 again when it is clicked
        // during an active countdown; it does not emit D2 00.  Treat that
        // repeated native request as a ready toggle while still accepting an
        // explicit zero from protocol tools and future launchers.
        boolean value = requestedValue;
        if (requestedValue
                && room.match.phase() == BattleRules.Phase.COUNTDOWN
                && room.ready.contains(session)) {
            value = false;
        }
        if (value) room.ready.add(session); else room.ready.remove(session);
        List<ClientSession> combatants = room.combatants();
        boolean enoughPlayers = nativeRosterReady(room, combatants);
        boolean allReady = enoughPlayers && !combatants.isEmpty()
                && room.ready.containsAll(combatants);
        if (allReady && room.match.phase() == BattleRules.Phase.WAITING) {
            room.match.countdown();
            room.countdownValue = 3;
            room.countdownNextNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            return new ReadyUpdate(true, ReadyTransition.COUNTDOWN_STARTED, 0);
        }
        if (!allReady && room.match.phase() == BattleRules.Phase.COUNTDOWN) {
            room.match.cancelCountdown();
            room.countdownValue = 0;
            room.countdownNextNanos = 0;
            return new ReadyUpdate(value, ReadyTransition.COUNTDOWN_CANCELLED, 0);
        }
        int minimumWarning = value && siegeRosterEnforced
                && room.match.rules().mode() == BattleRules.Mode.SIEGE_WAR
                && !enoughPlayers
                ? BattleRules.SIEGE_MINIMUM_PLAYERS_PER_FORCE : 0;
        return new ReadyUpdate(value, ReadyTransition.NONE, minimumWarning);
    }

    /**
     * Applies the opcode-only native room-owner start/cancel control without
     * allowing a participant or forged client to acquire owner authority.
     */
    synchronized ReadyUpdate requestOwnerStart(long roomId, ClientSession session) {
        Room room = rooms.get(roomId);
        if (room == null || room.owner != session) {
            return new ReadyUpdate(false, ReadyTransition.NONE, 0);
        }
        return setReady(roomId, session, true);
    }

    /**
     * Initial S_CD state for a room roster row.  Native state 2 is a
     * transient S_D1 shop/list-entry notification and cancels readiness, so a
     * newly rebuilt roster can only authoritatively advertise 0 or 1.
     */
    synchronized Protocol.LitoParticipantState nativeRoomParticipantReadyState(
            long roomId, ClientSession session) {
        Room room = rooms.get(roomId);
        return room != null && session != null && room.ready.contains(session)
                ? Protocol.LitoParticipantState.READY
                : Protocol.LitoParticipantState.NOT_READY;
    }

    synchronized void observerChanged(ClientSession session) {
        for (Room room : rooms.values()) {
            room.ready.remove(session);
            cancelCountdownIfNotReady(room);
        }
    }

    synchronized ForceUpdate setForce(long roomId, ClientSession session, int force) {
        Room room = rooms.get(roomId);
        if (room != null && room.members.contains(session) && !room.battleStarted) {
            // sub_4A9BD0 has explicit roster branches only for force 0 and 1.
            // Keep this defensive clamp even though ClientSession rejects
            // malformed native C_D3 values before reaching the hub.
            int requested = Math.max(0, Math.min(1, force));
            int current = forceOf(room, session);
            if (siegeRosterEnforced
                    && room.match.rules().mode() == BattleRules.Mode.SIEGE_WAR
                    && requested != current
                    && siegeForceCount(room, requested, session)
                            >= BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE) {
                return new ForceUpdate(current, false, ReadyTransition.NONE,
                        BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE + 1);
            }
            room.forces.put(session, requested);
            boolean countdownCancelled = cancelCountdownIfNotReady(room);
            return new ForceUpdate(requested, requested != current,
                    countdownCancelled ? ReadyTransition.COUNTDOWN_CANCELLED
                            : ReadyTransition.NONE,
                    0);
        }
        return new ForceUpdate(0, false, ReadyTransition.NONE, 0);
    }

    private boolean nativeRosterReady(Room room, List<ClientSession> combatants) {
        return NativeRoomRules.rosterReady(this, room, combatants);
    }

    private static int siegeForceCount(Room room, int force, ClientSession excluded) {
        int count = 0;
        for (ClientSession combatant : room.combatants()) {
            if (combatant != excluded && forceOf(room, combatant) == force) count++;
        }
        for (RoomVirtualPlayer combatant : room.virtualPlayers) {
            if (combatant.force() == force) count++;
        }
        return count;
    }

    private static int smallerSiegeForce(Room room, ClientSession joining) {
        int force0 = siegeForceCount(room, 0, joining);
        int force1 = siegeForceCount(room, 1, joining);
        return force0 <= force1 ? 0 : 1;
    }

    private static void balanceSiegeForces(Room room) {
        int force = 0;
        for (ClientSession combatant : room.combatants()) {
            room.forces.put(combatant, force);
            force ^= 1;
        }
        List<RoomVirtualPlayer> balanced = new ArrayList<>(room.virtualPlayers.size());
        for (RoomVirtualPlayer combatant : room.virtualPlayers) {
            balanced.add(new RoomVirtualPlayer(combatant.objectId(), combatant.name(),
                    combatant.classicServerName(), force, combatant.characterType(),
                    combatant.sex()));
            force ^= 1;
        }
        room.virtualPlayers.clear();
        room.virtualPlayers.addAll(balanced);
    }

    synchronized int forceOf(long roomId, ClientSession session) {
        Room room = rooms.get(roomId);
        return room == null ? 0 : forceOf(room, session);
    }

    private static int forceOf(Room room, ClientSession session) {
        Integer current = room.forces.get(session);
        return current != null ? current : room.battleForces.getOrDefault(session, 0);
    }

    synchronized int winningForce(long roomId, BattleRules.Resolution resolution) {
        Room room = rooms.get(roomId);
        if (room == null || !resolution.ended()) return -1;
        List<ClientSession> combatants = room.battleRoster.isEmpty()
                ? room.combatants() : new ArrayList<>(room.battleRoster);
        for (ClientSession member : combatants) {
            if (resolution.winners().contains(member.playerObjectId())) {
                return forceOf(room, member);
            }
        }
        List<RoomVirtualPlayer> virtuals = room.battleVirtualPlayers.isEmpty()
                ? room.virtualPlayers : room.battleVirtualPlayers;
        for (RoomVirtualPlayer member : virtuals) {
            if (resolution.winners().contains(member.objectId())) return member.force();
        }
        // The local one-player arena represents ArenaBot as object 2 / force
        // 1 without creating a ClientSession for it.  A defeated sole player
        // therefore means the bot's force won.
        if (NativeRoomRules.bot(room.match.rules().mode(), this, room.id)
                && combatants.size() == 1 && virtuals.isEmpty()
                && resolution.losers().contains(combatants.get(0).playerObjectId())) {
            return 1;
        }
        return resolution.winners().contains(2L) ? 1 : -1;
    }

    synchronized boolean canAttack(long roomId, ClientSession attacker, ClientSession target) {
        Room room = rooms.get(roomId);
        return ForcedCombat.canAttack(room != null && room.match.canAttack(attacker.playerObjectId(), target.playerObjectId()), this, roomId, attacker, target);
    }

    synchronized BattleRules.RuleSet roomRules(long roomId) {
        Room room = rooms.get(roomId);
        return room == null
                ? BattleRules.decode(defaultRoomSettings(), battleControls)
                : room.match.rules();
    }

    synchronized boolean updateMap(long roomId, ClientSession requester, int mapId) {
        Room room = rooms.get(roomId);
        if (room == null || room.owner != requester || room.battleStarted) return false;
        BattleRules.Mode mode = room.match.rules().mode();
        if (!mode.maps().contains(mapId)) return false;
        room.selectedMapId = mapId;
        room.ready.clear();
        room.match.reconfigure(BattleRules.decode(
                room.settings, mapId, battleControls));
        return true;
    }

    synchronized BattlePetRaceFormCatalog.Form petRaceBaseForm(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted
                || room.match.rules().mode() != BattleRules.Mode.PET_RACE) {
            return null;
        }
        return room.petRace.baseForm();
    }

    synchronized int roomWeather(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? 0 : room.weather;
    }

    synchronized boolean setRoomWeather(long roomId, int encodedWeather) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted) return false;
        room.weather = encodedWeather;
        return true;
    }

    synchronized int matchTimerSeconds(long roomId) {
        Room room = rooms.get(roomId);
        return room == null || !room.battleStarted ? -1 : room.match.timerSeconds(System.nanoTime());
    }

    synchronized BattleRules.Snapshot matchSnapshot(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? null : room.match.snapshot(System.nanoTime());
    }

    synchronized BattleRules.Resolution playerDefeated(long roomId, ClientSession killer, ClientSession victim) {
        Room room = rooms.get(roomId);
        if (room == null) return BattleRules.Resolution.continuing(false, "unknown-room");
        return room.match.playerDefeated(killer == null ? 0 : killer.playerObjectId(), victim.playerObjectId());
    }

    synchronized void respawn(long roomId, ClientSession player) {
        Room room = rooms.get(roomId);
        if (room != null) room.match.respawn(player.playerObjectId());
    }

    /**
     * Defers only Honor Match's native same-position resurrection. Other modes
     * retain their existing immediate spawn behavior and never enter this path.
     */
    synchronized boolean scheduleHonorRespawn(
            long roomId, ClientSession player, long token, int deathX, int deathY) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || player == null
                || !room.members.contains(player) || !room.battleRoster.contains(player)
                || room.match.rules().mode() != BattleRules.Mode.HONOR_MATCH) {
            return false;
        }
        long battleGeneration = room.battleGeneration;
        battleClock.schedule(() -> completeHonorRespawnSafely(
                        roomId, battleGeneration, player, token, deathX, deathY),
                HONOR_RESPAWN_DELAY_MS, TimeUnit.MILLISECONDS);
        return true;
    }

    private void completeHonorRespawnSafely(
            long roomId, long battleGeneration, ClientSession player,
            long token, int deathX, int deathY) {
        try {
            if (!player.honorRespawnPending(roomId, token)
                    || !claimHonorRespawn(roomId, battleGeneration, player)) return;
            player.serverHonorRespawnDue(roomId, token, deathX, deathY);
        } catch (Throwable error) {
            System.out.println("[LiTo] Honor respawn recovered room=" + roomId
                    + " player=" + (player == null ? "none" : player.playerName())
                    + " reason=" + error.getClass().getSimpleName() + ": "
                    + error.getMessage());
        }
    }

    private synchronized boolean claimHonorRespawn(
            long roomId, long battleGeneration, ClientSession player) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted
                || room.battleGeneration != battleGeneration
                || !room.members.contains(player) || !room.battleRoster.contains(player)
                || room.match.rules().mode() != BattleRules.Mode.HONOR_MATCH
                || room.match.phase() != BattleRules.Phase.RUNNING) {
            return false;
        }
        room.match.respawn(player.playerObjectId());
        return true;
    }

    synchronized BattleRules.Resolution guardianTowerDestroyed(long roomId, int winningForce) {
        Room room = rooms.get(roomId);
        return room == null ? BattleRules.Resolution.continuing(false, "unknown-room")
                : room.match.guardianTowerDestroyed(winningForce);
    }

    synchronized BattleRules.Resolution waveCleared(long roomId, int force) {
        Room room = rooms.get(roomId);
        return room == null ? BattleRules.Resolution.continuing(false, "unknown-room")
                : room.match.waveCleared(force);
    }

    synchronized BattleRules.Resolution timeAttackCompleted(long roomId) {
        Room room = rooms.get(roomId);
        return room == null ? BattleRules.Resolution.continuing(false, "unknown-room")
                : room.match.timeAttackCompleted();
    }

    synchronized BattleRules.Resolution strongholdCaptured(long roomId, int winningForce) {
        Room room = rooms.get(roomId);
        return room == null ? BattleRules.Resolution.continuing(false, "unknown-room")
                : room.match.strongholdCaptured(winningForce);
    }

    synchronized BattleRules.Resolution checkpoint(long roomId, ClientSession player, int checkpoint) {
        Room room = rooms.get(roomId);
        return room == null ? BattleRules.Resolution.continuing(false, "unknown-room")
                : room.match.checkpoint(player.playerObjectId(), checkpoint);
    }

    synchronized PetRaceMove petRaceMoved(long roomId, ClientSession player,
                                          int fromX, int fromY, int toX, int toY,
                                          long nowNanos) {
        Room room = rooms.get(roomId);
        if (room == null || player == null || !room.battleStarted
                || room.match.phase() != BattleRules.Phase.RUNNING
                || room.match.rules().mode() != BattleRules.Mode.PET_RACE
                || !room.battleRoster.contains(player)) {
            return new PetRaceMove(BattlePetRaceCourseCatalog.Transition.ignored(0),
                    BattlePetRaceCourseCatalog.PanelTransition.none(),
                    BattleRules.Resolution.continuing(false, "ignored-pet-race-move"));
        }
        BattlePetRaceCourseCatalog.PanelTransition panel = room.petRace.panelMoved(
                player.playerObjectId(), fromX, fromY, toX, toY, nowNanos);
        BattlePetRaceCourseCatalog.Transition transition = room.petRace.moved(
                player.playerObjectId(), fromX, fromY, toX, toY, nowNanos);
        BattleRules.Resolution resolution = transition.lapCompleted()
                ? room.match.checkpoint(player.playerObjectId(), transition.completedLaps())
                : BattleRules.Resolution.continuing(false, "pet-race-course-progress");
        if (transition.finished() && room.petRace.finishWindowExpired(nowNanos)) {
            Set<Long> winners = room.petRace.rewardWinners();
            if (!winners.isEmpty()) resolution = room.match.finishPetRace(winners);
        }
        return new PetRaceMove(transition, panel, resolution);
    }

    synchronized boolean isOwner(long roomId, ClientSession session) {
        Room room = rooms.get(roomId);
        return room != null && room.owner == session;
    }

    synchronized String roomOwnerName(long roomId) {
        Room room = rooms.get(roomId);
        return room == null || room.owner == null ? "" : room.owner.playerName();
    }

    synchronized String roomOwnerDisplayName(long roomId) {
        Room room = rooms.get(roomId);
        return room == null || room.owner == null ? "" : room.owner.litoDisplayName();
    }

    synchronized ClientSession memberByName(long roomId, String name) {
        Room room = rooms.get(roomId);
        if (room == null || name == null) return null;
        return uniqueNamedMember(room.members, null, name);
    }

    synchronized ClientSession memberByName(
            long roomId, ClientSession requester, String name) {
        Room room = rooms.get(roomId);
        if (room == null || requester == null || name == null) return null;
        return uniqueNamedMember(room.members, requester, name);
    }

    synchronized boolean markRoomKick(
            long roomId, ClientSession requester, ClientSession target) {
        Room room = rooms.get(roomId);
        if (room == null || requester == null || target == null
                || room.owner != requester || target == requester
                || room.battleStarted || room.resultPending
                || !room.members.contains(target)) return false;
        room.kickedParticipantIdentities.add(roomParticipantIdentity(target));
        return true;
    }

    private static ClientSession uniqueNamedMember(
            java.util.Collection<ClientSession> candidates,
            ClientSession requester, String displayName) {
        String requestedIdentity = requester == null ? ""
                : requester.sourceCharacterIdentity(displayName);
        List<ClientSession> matches = candidates.stream()
                .filter(member -> member.playerName().equalsIgnoreCase(displayName)
                        || member.litoDisplayName().equalsIgnoreCase(displayName))
                .filter(member -> requester == null
                        || member.litoDisplayName().equalsIgnoreCase(displayName)
                        || member.sourceCharacterIdentity()
                                .equalsIgnoreCase(requestedIdentity))
                .toList();
        // Name-only packets have no wire field with which to disambiguate two
        // original sources. Never choose insertion order as authority.
        return matches.size() == 1 ? matches.get(0) : null;
    }

    synchronized boolean updateSettings(long roomId, ClientSession requester, RoomSettings settings) {
        Room room = rooms.get(roomId);
        if (room == null || room.owner != requester || room.battleStarted) return false;
        if (room.combatantCount() > nativeCombatantCapacity(settings)) return false;
        BattleRules.Mode previousMode = room.match.rules().mode();
        BattleRules.Mode nextMode = BattleRules.decode(
                settings, battleControls).mode();
        int nextMapId = previousMode == nextMode
                && nextMode.maps().contains(room.selectedMapId)
                ? room.selectedMapId : nextMode.maps().get(0);
        room.settings = settings;
        room.selectedMapId = nextMapId;
        room.ready.clear();
        room.match.reconfigure(BattleRules.decode(
                settings, nextMapId, battleControls));
        if (previousMode != BattleRules.Mode.SIEGE_WAR
                && nextMode == BattleRules.Mode.SIEGE_WAR) {
            balanceSiegeForces(room);
        }
        return true;
    }

    private void countdownTickSafely() {
        try {
            CountdownWork work = countdownWork(System.nanoTime());
            for (CountdownTick tick : work.ticks) {
                for (ClientSession member : tick.members) member.serverCountdown(tick.value);
            }
            for (BattleStart start : work.starts) {
                // Prepare every combatant before any client receives its
                // S_PUT_OBJECT roster. Otherwise callback order could expose
                // a peer's stale HP ratio from the preceding match.
                for (ClientSession member : start.members) member.serverPrepareBattle();
                for (ClientSession member : start.members) member.serverStartBattle();
                armPveAi(start.roomId);
            }
            for (BattleEnd end : work.ends) {
                // Keep the authoritative match snapshot alive until every native
                // result controller has received D3 followed by its DB rows.  A
                // timeout used to reset the match inside countdownWork, causing
                // otherwise valid kills/deaths to be published as zero.
                try {
                    for (ClientSession member : end.members) {
                        try {
                            member.serverEndBattle(end.resolution);
                        } catch (IOException error) {
                            System.out.println("[LiTo] timeout result send failed recipient="
                                    + member.playerName() + ": " + error.getMessage());
                        }
                    }
                } finally {
                    endBattle(end.roomId);
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] countdown clock recovered: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
        }
    }

    private record CountdownWork(List<CountdownTick> ticks, List<BattleStart> starts,
                                 List<BattleEnd> ends) {}

    private synchronized CountdownWork countdownWork(long nowNanos) {
        List<CountdownTick> ticks = new ArrayList<>();
        List<BattleStart> starts = new ArrayList<>();
        List<BattleEnd> ends = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (room.match.phase() != BattleRules.Phase.COUNTDOWN
                    || nowNanos < room.countdownNextNanos) continue;
            List<ClientSession> combatants = room.combatants();
            boolean allReady = nativeRosterReady(room, combatants)
                    && !combatants.isEmpty() && room.ready.containsAll(combatants);
            if (!allReady) {
                room.match.cancelCountdown();
                room.countdownValue = 0;
                room.countdownNextNanos = 0;
                ticks.add(new CountdownTick(new ArrayList<>(room.members), 0));
                continue;
            }
            if (room.countdownValue > 1) {
                room.countdownValue--;
                room.countdownNextNanos = nowNanos + TimeUnit.SECONDS.toNanos(1);
                ticks.add(new CountdownTick(new ArrayList<>(room.members), room.countdownValue));
                continue;
            }
            room.countdownValue = 0;
            room.countdownNextNanos = 0;
            room.battleStarted = true;
            room.terminalEffectPending = false;
            room.disconnectResolutionPending = false;
            room.disconnectedBattleCombatants.clear();
            room.battleGeneration++;
            room.botHp = NativeRoomRules.botHp(room, this);
            room.botArmBreakerStatus.clear();
            room.botJoyOfPainStatus.clear();
            room.botStep = 0;
            room.botInventory.clear();
            room.groundEffects.clear();
            room.groundItems.clear();
            room.cookingFires.clear();
            room.cubeIgnitionEffects.clear();
            room.cubeQuakeEffects.clear();
            room.cubeShockEffects.clear();
            room.cubeHarmonyEffects.clear();
            room.patienceStatus.clear();
            room.shockStunEffects.clear();
            room.boneBreakEffects.clear();
            room.freezeEffects.clear();
            room.armorBreakExpiresAtNanos.clear();
            ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(room.match.rules().mapId());
            room.door.deactivate();
            room.siegeGate.deactivate();
            if (room.match.rules().mode() == BattleRules.Mode.SIEGE_WAR) {
                room.siegeGate.reset(arena, arena.featurePoint());
            } else if (room.match.rules().mode() != BattleRules.Mode.PET_RACE
                    && room.match.rules().mode() != BattleRules.Mode.QUEST_BATTLE) {
                room.door.reset(arena);
            }
            room.guardianTowers.reset(
                    room.match.rules(), arena, -1, battleControls);
            room.questEncounter.reset(
                    room.match.rules(), arena, battleControls);
            room.questBoundary.reset(room.match.rules());
            int patternIndex = timeAttackDocumentedPatternFixtureIndex >= 0
                    ? timeAttackDocumentedPatternFixtureIndex
                    : ThreadLocalRandom.current().nextInt(
                            BattleTimeAttackEncounter.documentedPatterns().size());
            int dragonOutcomeIndex = timeAttackDragonOutcomeFixtureIndex >= 0
                    ? timeAttackDragonOutcomeFixtureIndex
                    : ThreadLocalRandom.current().nextInt(
                            BattleTimeAttackEncounter.dragonCandidates().size() + 1);
            room.timeAttackEncounter.reset(
                    room.match.rules(), arena, patternIndex, dragonOutcomeIndex);
            room.pveAiPublishAfterNanos = Long.MAX_VALUE;
            List<Long> players = new ArrayList<>();
            for (ClientSession player : combatants) players.add(player.playerObjectId());
            for (RoomVirtualPlayer player : room.virtualPlayers) players.add(player.objectId());
            Map<Long, Integer> assignedForces = new LinkedHashMap<>();
            for (ClientSession player : combatants) {
                assignedForces.put(player.playerObjectId(), room.forces.getOrDefault(player, 0));
            }
            for (RoomVirtualPlayer player : room.virtualPlayers) {
                assignedForces.put(player.objectId(), player.force());
            }
            room.battleRoster.clear();
            room.battleRoster.addAll(combatants);
            room.battleVirtualPlayers.clear();
            room.battleVirtualPlayers.addAll(room.virtualPlayers);
            room.battleForces.clear();
            for (ClientSession player : combatants) {
                room.battleForces.put(player, room.forces.getOrDefault(player, 0));
            }
            ClientSession leader = combatants.contains(room.owner)
                    ? room.owner : combatants.get(0);
            room.match.begin(players, assignedForces, leader.playerObjectId(), nowNanos);
            if (room.match.rules().mode() == BattleRules.Mode.PET_RACE) {
                long raceDeadlineNanos = nowNanos + TimeUnit.SECONDS.toNanos(
                        room.match.rules().timeLimitSeconds());
                room.petRace.begin(room.match.rules().mapId(), players, nowNanos,
                        raceDeadlineNanos, room.battleGeneration);
            } else {
                room.petRace.reset();
            }
            if (room.match.rules().mode() == BattleRules.Mode.SIEGE_WAR
                    && !room.match.assignSiegeAttackerForce(
                            room.guardianTowers.siegeAttackerForce())) {
                throw new IllegalStateException("Siege attack/defence assignment failed");
            }
            starts.add(new BattleStart(room.id, new ArrayList<>(room.members)));
        }
        for (Room room : rooms.values()) {
            if (!room.battleStarted || room.disconnectResolutionPending
                    || room.match.phase() != BattleRules.Phase.RUNNING) continue;
            BattleRules.Resolution resolution;
            if (room.match.rules().mode() == BattleRules.Mode.PET_RACE
                    && room.petRace.finishWindowExpired(nowNanos)) {
                Set<Long> winners = room.petRace.rewardWinners();
                resolution = winners.isEmpty()
                        ? room.match.timeout(nowNanos) : room.match.finishPetRace(winners);
            } else {
                resolution = room.match.timeout(nowNanos);
            }
            if (!resolution.ended()) continue;
            ends.add(new BattleEnd(room.id, new ArrayList<>(room.members), resolution));
            // Cleanup is intentionally deferred to countdownTickSafely after
            // all clients have consumed the result snapshot.  The scheduler is
            // single-threaded, so no subsequent countdown tick can overlap this
            // delivery window.
        }
        return new CountdownWork(ticks, starts, ends);
    }

    private void tickSafely() {
        try {
            long nowNanos = System.nanoTime();
            for (BotTick tick : botTicks(nowNanos)) {
                try {
                    tick.target.serverBotTick(tick.roomId, tick.x, tick.y, tick.damage);
                } catch (IOException error) {
                    System.out.println("[LiTo] ArenaBot send failed room=" + tick.roomId
                            + " target=" + tick.target.playerName() + ": "
                            + error.getMessage());
                }
            }
            for (MonsterAiTick tick : monsterAiTicks(nowNanos)) {
                try {
                    tick.target.serverMonsterAiTick(tick);
                } catch (IOException error) {
                    System.out.println("[LiTo] PvE monster send failed room=" + tick.roomId
                            + " object=" + tick.objectId + " target="
                            + tick.target.playerName() + ": " + error.getMessage());
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] battle clock recovered: " + error.getClass().getSimpleName()
                    + ": " + error.getMessage());
        }
    }

    private void classicWorldMonsterTickSafely() {
        try {
            long nowNanos = System.nanoTime();
            classicWorldMonsterGuardTick(nowNanos);
            for (int mapId : ClassicWorldMapCatalog.supportedMapIds()) {
                classicWorldMonsterTick(
                        mapId, -1, classicWorldMonsters, nowNanos);
            }
            Map<Integer, ClassicWorldMonsterPopulation> questPopulations;
            synchronized (this) {
                questPopulations = new LinkedHashMap<>(classicWorldCkewMonsters);
            }
            for (Map.Entry<Integer, ClassicWorldMonsterPopulation> entry
                    : questPopulations.entrySet()) {
                classicWorldMonsterTick(
                        ClassicWorldCkewCatalog.MAP_ID, entry.getKey(),
                        entry.getValue(), nowNanos);
            }
            synchronized (this) {
                questPopulations = new LinkedHashMap<>(
                        classicWorldWaterDragonMonsters);
            }
            for (Map.Entry<Integer, ClassicWorldMonsterPopulation> entry
                    : questPopulations.entrySet()) {
                classicWorldMonsterTick(
                        ClassicWorldWaterDragonCatalog.MAP_ID, entry.getKey(),
                        entry.getValue(), nowNanos);
            }
        } catch (Throwable error) {
            logClassicWorldMonsterRecovery(error);
        }
    }

    private void classicWorldMonsterDigestTickSafely() {
        try {
            classicWorldMonsterDigestTick(5);
        } catch (Throwable error) {
            System.out.println("[LiTo] classic-world monster digest clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    void classicWorldMonsterDigestTick(int elapsedSeconds) {
        classicWorldMonsters.advanceDigestItems(elapsedSeconds);
        List<ClassicWorldMonsterPopulation> instances = new ArrayList<>();
        synchronized (this) {
            instances.addAll(classicWorldCkewMonsters.values());
            instances.addAll(classicWorldWaterDragonMonsters.values());
        }
        for (ClassicWorldMonsterPopulation population : instances) {
            population.advanceDigestItems(elapsedSeconds);
        }
    }

    /**
     * A disconnected or deliberately incomplete integration-test session can
     * make the retained monster clock recover on every short scheduler tick.
     * Keep the first diagnostic and periodic summaries without allowing that
     * recoverable condition to monopolize stdout or memory.
     */
    private void logClassicWorldMonsterRecovery(Throwable error) {
        long nowNanos = System.nanoTime();
        long elapsed = nowNanos - classicWorldMonsterLastRecoveryLogNanos;
        if (classicWorldMonsterLastRecoveryLogNanos != 0
                && elapsed >= 0
                && elapsed < MONSTER_RECOVERY_LOG_INTERVAL_NANOS) {
            classicWorldMonsterSuppressedRecoveryCount++;
            return;
        }
        long suppressed = classicWorldMonsterSuppressedRecoveryCount;
        classicWorldMonsterLastRecoveryLogNanos = nowNanos;
        classicWorldMonsterSuppressedRecoveryCount = 0;
        System.out.println("[LiTo] classic-world monster clock recovered: "
                + error.getClass().getSimpleName() + ": " + error.getMessage()
                + (suppressed == 0 ? "" : " (suppressed=" + suppressed + ")"));
    }

    private enum ClassicWorldBowTargetKind { PLAYER, COMPANION, MONSTER }

    private record ClassicWorldBowTarget(
            ClassicWorldBowTargetKind kind, long objectId, int x, int y,
            ClientSession owner, BattleCompanion.Snapshot companion,
            ClassicWorldMonsterPopulation.Snapshot monster) {}

    private void classicWorldBowTickSafely() {
        try {
            classicWorldBowTick();
        } catch (Throwable error) {
            System.out.println("[LiTo] classic-world bow clock recovered: "
                    + error.getClass().getSimpleName() + ": "
                    + error.getMessage());
        }
    }

    /** One exact retained NpcBowTimer pass, package-visible for integration proof. */
    void classicWorldBowTick() throws IOException {
        List<ClientSession> members = classicWorldMembers(
                ClassicWorldBowCatalog.MAP_ID, -1);
        if (members.isEmpty()) return;
        for (ClassicWorldNpcCatalog.StaticNpc bow : ClassicWorldBowCatalog.all()) {
            List<ClientSession> recipients = members.stream()
                    .filter(member -> {
                        ClassicWorldLocation location = classicWorldLocation(member);
                        return location != null
                                && BattleSpellCatalog.isInNativeScreen(
                                        bow.x(), bow.y(),
                                        location.x(), location.y());
                    })
                    .toList();
            // Exact checkPc(): no recognizing player stops this aperture.
            if (recipients.isEmpty()) continue;

            ClassicWorldBowCatalog.Lane lane = ClassicWorldBowCatalog.lane(bow);
            ClassicWorldBowTarget target = classicWorldBowTarget(
                    members, bow, lane);
            long targetObjectId = target == null ? 0 : target.objectId();
            int projectileX = target == null ? lane.endpointX()
                    : lane.projectileX(target.x());
            int projectileY = target == null ? lane.endpointY()
                    : lane.projectileY(target.y());

            // L1BowInstance invokes receiveDamage before broadcasting
            // S_UseArrowSkill.  Preserve that producer order and the null
            // attacker semantics for all four supported character families.
            if (target != null) {
                try {
                    switch (target.kind()) {
                        case PLAYER -> target.owner()
                                .serverClassicWorldBowPlayerDamage(bow);
                        case COMPANION -> target.owner()
                                .serverClassicWorldBowCompanionDamage(
                                        bow, target.companion());
                        case MONSTER -> {
                            ClassicWorldMonsterPopulation.Hit transition =
                                    damageClassicWorldMonster(
                                            bow.mapId(), -1,
                                            target.objectId(),
                                            ClassicWorldBowCatalog.DAMAGE);
                            if (transition != null) {
                                recipients.get(0).serverClassicWorldBowMonsterDamage(
                                        bow, transition);
                            }
                        }
                    }
                } catch (IOException disconnectedTarget) {
                    // This is an asynchronous environmental clock.  A target
                    // socket may close after checkPc/target selection; the
                    // retained producer catches that pass and emits no arrow.
                    continue;
                }
            }
            for (ClientSession recipient : recipients) {
                try {
                    recipient.serverClassicWorldBowProjectile(
                            bow, targetObjectId, projectileX, projectileY);
                } catch (IOException disconnectedRecipient) {
                    // World.broadcastPacketAll tolerates a concurrent client
                    // departure; do not abort the other recognizing viewers.
                }
            }
        }
    }

    private ClassicWorldBowTarget classicWorldBowTarget(
            List<ClientSession> members,
            ClassicWorldNpcCatalog.StaticNpc bow,
            ClassicWorldBowCatalog.Lane lane) {
        List<ClassicWorldBowTarget> candidates = new ArrayList<>();
        for (ClientSession member : members) {
            ClassicWorldLocation location = classicWorldLocation(member);
            if (location == null || location.instanceId() != -1
                    || !member.classicWorldBowTargetEligible(bow.mapId())) continue;
            if (BattleSpellCatalog.isInNativeScreen(
                        bow.x(), bow.y(), location.x(), location.y())
                    && lane.contains(location.x(), location.y())) {
                candidates.add(new ClassicWorldBowTarget(
                        ClassicWorldBowTargetKind.PLAYER,
                        member.playerObjectId(), location.x(), location.y(),
                        member, null, null));
            }
            for (BattleCompanion.Snapshot companion
                    : member.classicWorldCompanionSnapshots()) {
                if (!companion.active()
                        || !BattleSpellCatalog.isInNativeScreen(
                            bow.x(), bow.y(), companion.x(), companion.y())
                        || !lane.contains(companion.x(), companion.y())) continue;
                candidates.add(new ClassicWorldBowTarget(
                        ClassicWorldBowTargetKind.COMPANION,
                        companion.objectId(), companion.x(), companion.y(),
                        member, companion, null));
            }
        }
        for (ClassicWorldMonsterPopulation.Snapshot monster
                : classicWorldMonstersInScreen(
                        bow.mapId(), -1, bow.x(), bow.y())) {
            if (!monster.present() || !lane.contains(monster.x(), monster.y())) continue;
            candidates.add(new ClassicWorldBowTarget(
                    ClassicWorldBowTargetKind.MONSTER,
                    monster.objectId(), monster.x(), monster.y(),
                    null, null, monster));
        }
        // World.getVisibleObjects is backed by an object-id map and promises
        // no category priority.  Object-id ordering gives this sidecar a stable
        // equivalent without inventing PC/pet/monster precedence.
        candidates.sort((left, right) -> Long.compareUnsigned(
                left.objectId(), right.objectId()));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Exact stationary targeting boundary of retained L1MonsterGuardInstance:
     * visible, living players admitted by agrocoi=1 are attacked, including
     * invisible players, except members of the clan that owns Orc Castle. The
     * tower never moves and its empty receiveDamage methods keep HP/MP immutable.
     */
    private void classicWorldMonsterGuardTick(long nowNanos) throws IOException {
        List<ClientSession> members = classicWorldMembers(
                ClassicWorldMonsterGuardCatalog.MAP_ID, -1);
        if (members.isEmpty()) return;
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldMonsterGuardCatalog.MAP_ID);
        for (ClassicWorldNpcCatalog.StaticNpc tower
                : ClassicWorldMonsterGuardCatalog.all()) {
            long readyAt;
            synchronized (this) {
                readyAt = classicWorldMonsterGuardReadyAtNanos.getOrDefault(
                        tower.objectId(), 0L);
            }
            if (nowNanos < readyAt) continue;

            ClientSession target = null;
            ClassicWorldLocation targetLocation = null;
            for (ClientSession candidate : members) {
                if (!candidate.classicWorldMonsterGuardAiEligible(tower.mapId())
                        || candidate.classicWorldClanOwnsCastle(
                                ClassicWorldMonsterGuardCatalog.CASTLE_ID)) {
                    continue;
                }
                ClassicWorldLocation location = classicWorldLocation(candidate);
                if (location == null || location.instanceId() != -1) continue;
                int distance = chebyshev(
                        tower.x(), tower.y(), location.x(), location.y());
                if (distance > ClassicWorldMonsterGuardCatalog.ATTACK_RANGE
                        || !BattleSpellCatalog.isInNativeScreen(
                                tower.x(), tower.y(), location.x(), location.y())
                        || !ClassicWorldMonsterGuardCatalog.attackPosition(
                                map, tower.x(), tower.y(),
                                location.x(), location.y(),
                                ClassicWorldMonsterGuardCatalog.ATTACK_RANGE)) {
                    continue;
                }
                target = candidate;
                targetLocation = location;
                break;
            }
            if (target == null) continue;

            int attackHeading = heading(
                    tower.x(), tower.y(), targetLocation.x(), targetLocation.y());
            synchronized (this) {
                classicWorldNpcHeadings.put(tower.objectId(), attackHeading);
                classicWorldMonsterGuardReadyAtNanos.put(
                        tower.objectId(), Math.addExact(nowNanos,
                                TimeUnit.MILLISECONDS.toNanos(
                                        ClassicWorldMonsterGuardCatalog
                                                .ATTACK_INTERVAL_MILLIS)));
            }
            ClassicWorldMonsterPopulation.Snapshot projection =
                    ClassicWorldMonsterGuardCatalog.combatProjection(
                            tower, attackHeading);
            int distance = chebyshev(
                    tower.x(), tower.y(), targetLocation.x(), targetLocation.y());
            target.serverClassicWorldMonsterAttack(
                    new ClassicWorldMonsterPopulation.Attack(
                            projection, target.playerObjectId(),
                            targetLocation.x(), targetLocation.y(),
                            projection.template().projectileAtDistance(distance)));
        }
    }

    private void classicWorldMonsterTick(
            int mapId, int instanceId,
            ClassicWorldMonsterPopulation population, long nowNanos)
            throws IOException {
        List<ClientSession> members = classicWorldMembers(mapId, instanceId);
        members = members.stream()
                .filter(ClientSession::transportOpen)
                .toList();
        if (members.isEmpty()) return;

        // Retained L1NpcInstance.approachPlayer surfaces a full-HP sink actor
        // at tile-line distance <=2, a full-HP flying actor at distance <=1,
        // or a damaged ICE actor in screen and provokes that player. A damaged
        // flying actor instead scans one visible ground inventory for native
        // potion/food types and lands without adding player hate.
        for (ClientSession candidate : members) {
            ClassicWorldLocation location = classicWorldLocation(candidate);
            if (location == null
                    || !candidate.classicWorldMonsterAiEligible(mapId)) continue;
            for (ClassicWorldMonsterPopulation.Reveal reveal
                    : population.revealSinkNear(
                            mapId, location.x(), location.y(),
                            candidate.playerObjectId())) {
                for (ClientSession recipient : members) {
                    recipient.serverClassicWorldMonsterRevealed(
                            reveal, "native-proximity");
                }
            }
            for (ClassicWorldMonsterPopulation.Reveal reveal
                    : population.revealFlyNear(
                            mapId, location.x(), location.y(),
                            candidate.playerObjectId())) {
                for (ClientSession recipient : members) {
                    recipient.serverClassicWorldMonsterRevealed(
                            reveal, "native-fly-proximity");
                }
            }
            for (ClassicWorldMonsterPopulation.Snapshot flying
                    : population.damagedFlyingInScreen(
                            mapId, location.x(), location.y())) {
                ClassicWorldMonsterPopulation.AirItemLanding landing =
                        population.searchItemFromAir(
                                mapId, flying.objectId(),
                                classicWorldGroundItems.inScreen(
                                        mapId, instanceId,
                                        flying.x(), flying.y()));
                if (landing == null) continue;
                for (ClientSession recipient : members) {
                    recipient.serverClassicWorldMonsterAirItemLanding(landing);
                }
            }
            for (ClassicWorldMonsterPopulation.Reveal reveal
                    : population.revealDamagedIceInScreen(
                            mapId, location.x(), location.y(),
                            candidate.playerObjectId())) {
                for (ClientSession recipient : members) {
                    recipient.serverClassicWorldMonsterRevealed(
                            reveal, "native-ice-damaged");
                }
            }
        }

        Map<Long, ClassicWorldMonsterPopulation.Snapshot> visibleActors =
                new LinkedHashMap<>();
        Set<ClassicWorldMonsterPopulation.GridPoint> occupiedTargetCells =
                new LinkedHashSet<>();
        Set<ClassicWorldMonsterPopulation.GridPoint> playerCells =
                new LinkedHashSet<>();
        for (ClientSession member : members) {
            ClassicWorldLocation location = classicWorldLocation(member);
            if (location == null) continue;
            ClassicWorldMonsterPopulation.GridPoint playerCell =
                    new ClassicWorldMonsterPopulation.GridPoint(
                            location.x(), location.y());
            playerCells.add(playerCell);
            occupiedTargetCells.add(playerCell);
            for (BattleCompanion.Snapshot companion
                    : member.classicWorldCompanionSnapshots()) {
                if (companion.active()) {
                    occupiedTargetCells.add(
                            new ClassicWorldMonsterPopulation.GridPoint(
                                    companion.x(), companion.y()));
                }
            }
            for (ClassicWorldDspyEscorts.Snapshot follower
                    : classicWorldDspyEscorts.inScreen(
                            location.mapId(), location.x(), location.y())) {
                if (follower.alive()) {
                    occupiedTargetCells.add(
                            new ClassicWorldMonsterPopulation.GridPoint(
                                    follower.x(), follower.y()));
                }
            }
            // agrocoi guardians must still be ticked when the only nearby PC
            // is invisible. Ordinary monster target selection below retains
            // its stricter invisibility gate.
            if (!member.classicWorldMonsterGuardAiEligible(location.mapId())) continue;
            for (ClassicWorldMonsterPopulation.Snapshot actor
                    : population.withinTileDistance(
                            location.mapId(), location.x(), location.y(), 20)) {
                visibleActors.putIfAbsent(actor.objectId(), actor);
            }
        }

        for (ClassicWorldMonsterPopulation.HomeReset reset
                : population.resetUnobservedHome(mapId, playerCells, nowNanos)) {
            for (ClientSession member : members) {
                member.serverClassicWorldMonsterHomeReset(reset);
            }
        }

        for (ClassicWorldMonsterPopulation.Snapshot guard
                : population.guardsNeedingHome(mapId)) {
            visibleActors.putIfAbsent(guard.objectId(), guard);
        }

        for (ClassicWorldMonsterPopulation.Snapshot observed
                : visibleActors.values()) {
            if (!observed.present() || !observed.alive()
                    || observed.hiddenStatus()
                        != ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE) {
                continue;
            }
            ClassicWorldMonsterPopulation.Snapshot actor =
                    population.actor(observed.mapId(), observed.objectId());
            if (actor == null || !actor.present() || !actor.alive()
                    || actor.hiddenStatus()
                        != ClassicWorldMonsterPopulation.HIDDEN_STATUS_NONE) continue;
            if (classicWorldCubeQuakeMonsterMovementStopped(
                    mapId, instanceId, actor.objectId(), nowNanos)) continue;

            long retainedTargetId = population.targetObjectId(
                    actor.mapId(), actor.objectId());
            ClassicWorldTarget target = findClassicWorldMonsterTarget(
                    members, actor, retainedTargetId);
            // Retained checkTarget/tagertClear recursively removes an invalid
            // current target and promotes the next highest _hateList entry.
            // Re-resolve each promoted object before autonomous searchTarget.
            while (target == null && retainedTargetId != 0) {
                population.clearTarget(
                        actor.mapId(), actor.objectId(), retainedTargetId);
                long promotedTargetId = population.targetObjectId(
                        actor.mapId(), actor.objectId());
                if (promotedTargetId == retainedTargetId) break;
                retainedTargetId = promotedTargetId;
                target = findClassicWorldMonsterTarget(
                        members, actor, retainedTargetId);
            }
            boolean sourceAutonomousTargeting =
                    ClassicWorldMonsterAggressionCatalog.forTemplate(actor.template()) != null
                    || ClassicWorldGuardianCatalog.isGuardian(actor)
                    || ClassicWorldGuardCatalog.isGuard(actor);
            if (target == null && classicWorldMonsterAggressiveAiEnabled
                    && sourceAutonomousTargeting) {
                target = nearestClassicWorldMonsterTarget(members, actor);
                if (target != null) {
                    population.provoke(actor.mapId(), actor.objectId(),
                            target.objectId());
                    if (ClassicWorldGuardianCatalog.isGuardian(actor)) {
                        target.owner().serverClassicWorldGuardianWarning(
                                actor,
                                target.owner().classicWorldGuardianDisposition());
                    }
                }
            }
            if (!population.actionReady(
                    actor.mapId(), actor.objectId(), nowNanos)) {
                continue;
            }
            // Exact L1MonsterInstance.onItemUse sequence: initial haste,
            // player-only doppel remove/rebuild, then low-HP healing.
            ClassicWorldMonsterPopulation.ItemUse hasteUse =
                    population.itemUseHaste(
                            actor.mapId(), actor.objectId(), nowNanos);
            if (hasteUse != null) {
                for (ClientSession member : members) {
                    member.serverClassicWorldMonsterUsedItem(hasteUse);
                }
            }
            if (target != null && target.player()) {
                ClassicWorldMonsterPopulation.DoppelTransformation doppel =
                        population.doppelTransform(
                                actor.mapId(), actor.objectId(), target.objectId(),
                                target.owner().classicDoppelName(),
                                target.owner().classicDoppelTitle(),
                                target.owner().classicDoppelLawful(),
                                target.owner().classicDoppelClassId());
                if (doppel != null) {
                    for (ClientSession member : members) {
                        member.serverClassicWorldMonsterDoppelTransformed(doppel);
                    }
                    actor = doppel.after();
                }
            }
            ClassicWorldMonsterPopulation.ItemUse healingUse =
                    population.itemUseHealing(
                            actor.mapId(), actor.objectId(), nowNanos);
            if (healingUse != null) {
                for (ClientSession member : members) {
                    member.serverClassicWorldMonsterUsedItem(healingUse);
                }
                actor = healingUse.after();
            }
            if (target != null) {
                // Retained onTarget sets actived and clears both item-target
                // fields only after onItemUse has run.
                population.beginTargetAction(
                        actor.mapId(), actor.objectId());
            }
            if (target == null && ClassicWorldGuardCatalog.isGuard(actor)) {
                ClassicWorldMonsterPopulation.GuardHomecoming homecoming =
                        population.returnGuardHome(
                                actor.mapId(), actor.objectId(),
                                occupiedTargetCells, nowNanos);
                if (homecoming != null) {
                    for (ClientSession member : members) {
                        member.serverClassicWorldGuardHomecoming(homecoming);
                    }
                }
                continue;
            }
            if (target == null) {
                ClassicWorldMonsterPopulation.ItemAction itemAction =
                        population.itemAction(
                                actor.mapId(), actor.objectId(),
                                classicWorldGroundItems.inScreen(
                                        actor.mapId(), instanceId,
                                        actor.x(), actor.y()),
                                occupiedTargetCells, nowNanos);
                if (itemAction != null) {
                    if (itemAction.kind()
                            == ClassicWorldMonsterPopulation.ItemActionKind.MOVED) {
                        for (ClientSession member : members) {
                            member.serverClassicWorldMonsterMoved(
                                    itemAction.movement());
                        }
                    } else if (itemAction.kind()
                            == ClassicWorldMonsterPopulation.ItemActionKind.PICKUP_READY) {
                        ClassicMonsterGroundPickup pickup =
                                takeClassicWorldMonsterGroundItem(
                                        population, actor,
                                        itemAction.targetItemObjectId(),
                                        instanceId, nowNanos);
                        if (pickup == null) {
                            population.groundItemUnavailable(
                                    actor.mapId(), actor.objectId(),
                                    itemAction.targetItemObjectId(), nowNanos);
                        } else {
                            for (ClientSession member : members) {
                                member.serverClassicWorldMonsterPickedItem(
                                        pickup.pickup(), pickup.ground());
                            }
                        }
                    }
                    continue;
                }
                ClassicWorldMonsterPopulation.IdleMovement idleMovement =
                        population.idleMove(
                                actor.mapId(), actor.objectId(),
                                occupiedTargetCells, nowNanos);
                if (idleMovement != null) {
                    for (ClientSession member : members) {
                        member.serverClassicWorldMonsterIdleMovement(
                                idleMovement);
                    }
                }
                continue;
            }
            if (actor.atkspeed() <= 0) {
                if (actor.passispeed() <= 0) continue;
                ClassicWorldMonsterPopulation.Movement escape =
                        population.moveAway(
                                actor.mapId(), actor.objectId(),
                                target.objectId(), target.x(), target.y(),
                                occupiedTargetCells, nowNanos);
                if (escape != null) {
                    for (ClientSession member : members) {
                        member.serverClassicWorldMonsterMoved(escape);
                    }
                }
                continue;
            }

            if (actor.mapId() == ClassicWorldCkewCatalog.MAP_ID) {
                int branchFixture =
                        classicWorldMonsterPhysicalSkillFixtureBranch != 0
                                ? classicWorldMonsterPhysicalSkillFixtureBranch
                                : classicWorldMonsterMagicFixtureBranch;
                int triggerFixture =
                        classicWorldMonsterPhysicalSkillFixtureTriggerRoll != 0
                                ? classicWorldMonsterPhysicalSkillFixtureTriggerRoll
                                : classicWorldMonsterMagicFixtureTriggerRoll;
                ClassicWorldMonsterPopulation.CkewSkillAttack skillAttack =
                        population.ckewSkillAttack(
                                actor.mapId(), actor.objectId(),
                                target.objectId(), target.x(), target.y(),
                                nowNanos, occupiedTargetCells,
                                branchFixture, triggerFixture, 0);
                if (skillAttack != null) {
                    if (skillAttack.physical() != null) {
                        target.owner().serverClassicWorldMonsterPhysicalSkillAttack(
                                skillAttack.physical());
                    } else if (skillAttack.magic() != null) {
                        target.owner().serverClassicWorldMonsterMagicAttack(
                                skillAttack.magic());
                    } else {
                        target.owner().serverClassicWorldMonsterStatusSkillAttack(
                                skillAttack.status());
                    }
                    continue;
                }
            } else {
                ClassicWorldMonsterPopulation.PhysicalSkillAttack
                        physicalSkillAttack = population.physicalSkillAttack(
                                actor.mapId(), actor.objectId(),
                                target.objectId(), target.x(), target.y(),
                                nowNanos,
                                classicWorldMonsterPhysicalSkillFixtureBranch,
                                classicWorldMonsterPhysicalSkillFixtureTriggerRoll);
                if (physicalSkillAttack != null) {
                    target.owner().serverClassicWorldMonsterPhysicalSkillAttack(
                            physicalSkillAttack);
                    continue;
                }

                ClassicWorldMonsterPopulation.MagicAttack magicAttack =
                        population.magicAttack(
                                actor.mapId(), actor.objectId(),
                                target.objectId(), target.x(), target.y(),
                                nowNanos, occupiedTargetCells,
                                classicWorldMonsterMagicFixtureBranch,
                                classicWorldMonsterMagicFixtureTriggerRoll, 0);
                if (magicAttack != null) {
                    target.owner().serverClassicWorldMonsterMagicAttack(magicAttack);
                    continue;
                }
            }

            ClassicWorldMonsterPopulation.Attack attack =
                    population.attack(
                            actor.mapId(), actor.objectId(),
                            target.objectId(), target.x(), target.y(), nowNanos);
            if (attack != null) {
                if (target.dspyFollower()) {
                    target.owner().serverClassicWorldDspyFollowerAttack(
                            attack, target.objectId());
                } else if (target.companion()) {
                    target.owner().serverClassicWorldMonsterCompanionAttack(
                            attack, target.objectId());
                } else {
                    target.owner().serverClassicWorldMonsterAttack(attack);
                }
                continue;
            }

            ClassicWorldMonsterPopulation.NearTeleport nearTeleport =
                    population.nearTeleport(
                            actor.mapId(), actor.objectId(),
                            target.objectId(), target.x(), target.y(),
                            occupiedTargetCells, nowNanos);
            if (nearTeleport != null) {
                for (ClientSession member : members) {
                    member.serverClassicWorldMonsterNearTeleport(nearTeleport);
                }
                continue;
            }

            ClassicWorldMonsterPopulation.Movement movement =
                    population.moveToward(
                            actor.mapId(), actor.objectId(),
                            target.objectId(), target.x(), target.y(),
                            occupiedTargetCells, nowNanos);
            if (movement == null) continue;
            for (ClientSession member : members) {
                member.serverClassicWorldMonsterMoved(movement);
            }
        }
    }

    private ClassicMonsterGroundPickup takeClassicWorldMonsterGroundItem(
            ClassicWorldMonsterPopulation population,
            ClassicWorldMonsterPopulation.Snapshot actor,
            long itemObjectId, int instanceId, long nowNanos) {
        ClassicWorldMonsterPopulation.ItemPickup[] accepted =
                new ClassicWorldMonsterPopulation.ItemPickup[1];
        ClassicWorldGroundItems.Take take = classicWorldGroundItems.takeWholeIf(
                actor.mapId(), instanceId, itemObjectId, item -> {
                    accepted[0] = population.acceptGroundItem(
                            actor.mapId(), actor.objectId(), item, nowNanos);
                    return accepted[0] != null;
                });
        return take == null || accepted[0] == null ? null
                : new ClassicMonsterGroundPickup(accepted[0], take.taken());
    }

    private void classicWorldCkewTickSafely() {
        try {
            long nowNanos = System.nanoTime();
            for (CkewCountdownWork work
                    : classicWorldCkewCountdownWorks(nowNanos)) {
                for (ClientSession recipient : work.recipients()) {
                    try {
                        recipient.serverClassicWorldCkewCountdown(
                                work.countdown().showId(),
                                work.countdown().remainingSeconds());
                    } catch (IOException error) {
                        System.out.println("[LiTo] CKEW countdown send failed show="
                                + work.countdown().showId() + " player="
                                + recipient.selectedCharacterName() + ": "
                                + error.getMessage());
                    }
                }
            }
            for (CkewEndWork work : classicWorldCkewEndWorks(nowNanos)) {
                for (ClientSession recipient : work.recipients()) {
                    try {
                        recipient.serverClassicWorldCkewEnded(work.ended());
                    } catch (IOException error) {
                        System.out.println("[LiTo] CKEW end send failed show="
                                + work.ended().showId() + " player="
                                + recipient.selectedCharacterName() + ": "
                                + error.getMessage());
                    }
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] CKEW clock recovered: "
                    + error.getClass().getSimpleName() + ": "
                    + error.getMessage());
        }
    }

    private void classicWorldShipTickSafely() {
        try {
            for (ClientSession session : connectedSessionsSnapshot()) {
                try {
                    session.serverClassicWorldShipDeparture();
                } catch (IOException error) {
                    System.out.println("[LiTo] classic-world ship departure failed player="
                            + session.selectedCharacterName() + ": "
                            + error.getMessage());
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] classic-world ship clock recovered: "
                    + error.getClass().getSimpleName() + ": "
                    + error.getMessage());
        }
    }

    private synchronized List<CkewCountdownWork>
            classicWorldCkewCountdownWorks(long nowNanos) {
        List<CkewCountdownWork> result = new ArrayList<>();
        for (ClassicWorldCkewInstances.Countdown countdown
                : classicWorldCkewInstances.countdowns(nowNanos)) {
            List<ClientSession> recipients = connectedSessions.stream()
                    .filter(session -> countdown.joinedIdentities().contains(
                            key(session.sourceCharacterIdentity())))
                    .toList();
            result.add(new CkewCountdownWork(countdown, recipients));
        }
        return List.copyOf(result);
    }

    private synchronized List<CkewEndWork> classicWorldCkewEndWorks(
            long nowNanos) {
        List<ClassicWorldCkewInstances.Ended> ended = new ArrayList<>(
                classicWorldCkewInstances.expire(nowNanos));
        for (ClassicWorldCkewInstances.Snapshot snapshot
                : classicWorldCkewInstances.activeSnapshots()) {
            String departed = snapshot.joinedIdentities().stream()
                    .filter(identity -> connectedSessions.stream()
                            .filter(session -> key(session.sourceCharacterIdentity())
                                    .equals(identity))
                            .noneMatch(session -> {
                                ClassicWorldLocation location =
                                        classicWorldMembership.get(session);
                                boolean entered = location != null
                                        && location.mapId()
                                            == ClassicWorldCkewCatalog.MAP_ID
                                        && location.instanceId()
                                            == snapshot.showId();
                                return entered || session
                                        .pendingClassicWorldCkewEntryShowId()
                                        == snapshot.showId();
                            }))
                    .findFirst().orElse("");
            if (!departed.isEmpty()) {
                ClassicWorldCkewInstances.Ended outStop =
                        classicWorldCkewInstances.leave(
                                departed, "out-stop");
                if (outStop != null) ended.add(outStop);
            }
        }
        List<CkewEndWork> result = new ArrayList<>();
        for (ClassicWorldCkewInstances.Ended value : ended) {
            classicWorldCkewMonsters.remove(value.showId());
            Set<String> identities = Set.copyOf(value.joinedIdentities());
            List<ClientSession> recipients = connectedSessions.stream()
                    .filter(session -> identities.contains(
                            key(session.sourceCharacterIdentity())))
                    .toList();
            result.add(new CkewEndWork(value, recipients));
        }
        return List.copyOf(result);
    }

    private void classicWorldWaterDragonTickSafely() {
        try {
            long nowNanos = System.nanoTime();
            WaterDragonTickWork work = classicWorldWaterDragonTickWork(nowNanos);
            for (WaterDragonEventWork eventWork : work.events()) {
                ClassicWorldWaterDragonInstances.Event event = eventWork.event();
                if (event.kind()
                        == ClassicWorldWaterDragonInstances.EventKind.MESSAGE) {
                    for (ClientSession recipient : eventWork.recipients()) {
                        try {
                            recipient.serverClassicWorldWaterDragonMessage(
                                    event.showId(), event.value());
                        } catch (IOException error) {
                            System.out.println("[LiTo] Water Dragon message send failed show="
                                    + event.showId() + " player="
                                    + recipient.selectedCharacterName() + ": "
                                    + error.getMessage());
                        }
                    }
                    continue;
                }
                if (event.kind()
                        == ClassicWorldWaterDragonInstances.EventKind.SPAWN_BOSS) {
                    ClassicWorldMonsterPopulation.Snapshot spawned =
                            spawnClassicWorldWaterDragonBoss(
                                    event.showId(), event.value());
                    if (spawned == null) {
                        endClassicWorldWaterDragon(
                                event.showId(), "boss-spawn-failed");
                        continue;
                    }
                    for (ClientSession recipient : eventWork.recipients()) {
                        try {
                            recipient.serverClassicWorldMonsterSpawned(spawned);
                        } catch (IOException error) {
                            System.out.println("[LiTo] Water Dragon boss spawn send failed show="
                                    + event.showId() + " player="
                                    + recipient.selectedCharacterName() + ": "
                                    + error.getMessage());
                        }
                    }
                    continue;
                }
                if (event.kind()
                        == ClassicWorldWaterDragonInstances.EventKind.EXIT) {
                    finishClassicWorldWaterDragonPopulation(event.showId());
                    for (ClientSession recipient : eventWork.recipients()) {
                        try {
                            recipient.serverClassicWorldWaterDragonEnded(
                                    event.showId(), "complete",
                                    event.destination(),
                                    event.bloodRecipients().contains(key(
                                            recipient.sourceCharacterIdentity())));
                        } catch (IOException error) {
                            System.out.println("[LiTo] Water Dragon completion send failed show="
                                    + event.showId() + " player="
                                    + recipient.selectedCharacterName() + ": "
                                    + error.getMessage());
                        }
                    }
                }
            }
            for (WaterDragonEndWork endWork : work.ended()) {
                for (ClientSession recipient : endWork.recipients()) {
                    try {
                        recipient.serverClassicWorldWaterDragonEnded(
                                endWork.ended().showId(),
                                endWork.ended().reason(),
                                ClassicWorldWaterDragonCatalog.RESTART_EXIT,
                                false);
                    } catch (IOException error) {
                        System.out.println("[LiTo] Water Dragon end send failed show="
                                + endWork.ended().showId() + " player="
                                + recipient.selectedCharacterName() + ": "
                                + error.getMessage());
                    }
                }
            }
            for (WaterDragonAfflictionWork affliction : work.afflictions()) {
                try {
                    affliction.recipient().applyWaterDragonAffliction(
                            affliction.kind(), nowNanos);
                } catch (IOException error) {
                    System.out.println("[LiTo] Water Dragon affliction send failed show="
                            + affliction.showId() + " player="
                            + affliction.recipient().selectedCharacterName() + ": "
                            + error.getMessage());
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] Water Dragon clock recovered: "
                    + error.getClass().getSimpleName() + ": "
                    + error.getMessage());
        }
    }

    private synchronized WaterDragonTickWork classicWorldWaterDragonTickWork(
            long nowNanos) {
        List<WaterDragonEndWork> ended = new ArrayList<>();
        for (ClassicWorldWaterDragonInstances.Ended value
                : classicWorldWaterDragonInstances.expire(nowNanos)) {
            Set<String> identities = Set.copyOf(value.joinedIdentities());
            List<ClientSession> recipients = connectedSessions.stream()
                    .filter(session -> identities.contains(
                            key(session.sourceCharacterIdentity())))
                    .toList();
            classicWorldWaterDragonMonsters.remove(value.showId());
            classicWorldWaterDragonAfflictionAfterNanos.remove(value.showId());
            ended.add(new WaterDragonEndWork(value, recipients));
        }

        List<WaterDragonEventWork> events = new ArrayList<>();
        for (ClassicWorldWaterDragonInstances.Event event
                : classicWorldWaterDragonInstances.poll(nowNanos)) {
            Set<String> identities = event.recipients();
            List<ClientSession> recipients = connectedSessions.stream()
                    .filter(session -> identities.contains(
                            key(session.sourceCharacterIdentity())))
                    .toList();
            events.add(new WaterDragonEventWork(event, recipients));
        }

        List<WaterDragonAfflictionWork> afflictions = new ArrayList<>();
        long interval = BattleWaterDragonAffliction.TIMER_INTERVAL_MILLIS
                * 1_000_000L;
        for (ClassicWorldWaterDragonInstances.Snapshot snapshot
                : classicWorldWaterDragonInstances.activeSnapshots()) {
            long due = classicWorldWaterDragonAfflictionAfterNanos.getOrDefault(
                    snapshot.showId(), nowNanos + interval);
            if (nowNanos < due) continue;
            classicWorldWaterDragonAfflictionAfterNanos.put(
                    snapshot.showId(), nowNanos + interval);
            for (ClientSession recipient : classicWorldMembers(
                    ClassicWorldWaterDragonCatalog.MAP_ID,
                    snapshot.showId())) {
                if (!snapshot.joinedIdentities().contains(
                        key(recipient.sourceCharacterIdentity()))) continue;
                ClassicWorldLocation location = classicWorldMembership.get(recipient);
                if (location == null || !BattleWaterDragonAffliction.insideTimerArea(
                        location.x(), location.y())) continue;
                afflictions.add(new WaterDragonAfflictionWork(
                        snapshot.showId(), recipient,
                        BattleWaterDragonAffliction.timerKind(
                                ThreadLocalRandom.current().nextInt(3))));
            }
        }
        return new WaterDragonTickWork(
                List.copyOf(events), List.copyOf(ended),
                List.copyOf(afflictions));
    }

    private synchronized ClassicWorldMonsterPopulation.Snapshot
            spawnClassicWorldWaterDragonBoss(int showId, int npcId) {
        ClassicWorldMonsterPopulation population =
                classicWorldWaterDragonMonsters.get(showId);
        ClassicWorldMonsterCatalog.MonsterTemplate template =
                ClassicWorldWaterDragonMonsterCatalog.templateForNpc(npcId);
        if (population == null || template == null) return null;
        ClassicWorldMapCatalog.ClassicMap map = ClassicWorldMapCatalog.forMap(
                ClassicWorldWaterDragonCatalog.MAP_ID);
        int attempts = npcId == 71026 ? 121 : 400;
        int start = ThreadLocalRandom.current().nextInt(attempts);
        for (int offset = 0; offset < attempts; offset++) {
            int slot = (start + offset) % attempts;
            int x;
            int y;
            if (npcId == 71026) {
                ClassicWorldWaterDragonCatalog.RandomLocation area =
                        ClassicWorldWaterDragonCatalog.FIRST_BOSS_SPAWN;
                int diameter = area.radius() * 2 + 1;
                x = area.centerX() + slot % diameter - area.radius();
                y = area.centerY() + slot / diameter - area.radius();
            } else {
                ClassicWorldWaterDragonCatalog.RandomRectangle area =
                        ClassicWorldWaterDragonCatalog.LATER_BOSS_SPAWN;
                int width = area.maxX() - area.minX() + 1;
                x = area.minX() + slot % width;
                y = area.minY() + slot / width;
            }
            if (!map.standingPassable(x, y)
                    || population.occupies(
                            ClassicWorldWaterDragonCatalog.MAP_ID, x, y)
                    || classicWorldMembers(
                            ClassicWorldWaterDragonCatalog.MAP_ID, showId)
                            .stream().map(classicWorldMembership::get)
                            .anyMatch(location -> location != null
                                    && location.x() == x && location.y() == y)) {
                continue;
            }
            return population.spawnQuestMonsterAt(
                    ClassicWorldWaterDragonCatalog.MAP_ID,
                    710_000 + npcId, x, y,
                    ThreadLocalRandom.current().nextInt(8), template);
        }
        return null;
    }

    private synchronized void finishClassicWorldWaterDragonPopulation(
            int showId) {
        classicWorldWaterDragonMonsters.remove(showId);
        classicWorldWaterDragonAfflictionAfterNanos.remove(showId);
    }

    private void endClassicWorldWaterDragon(int showId, String reason) {
        ClassicWorldWaterDragonInstances.Ended ended =
                classicWorldWaterDragonInstances.end(showId, reason);
        if (ended == null) return;
        List<ClientSession> recipients;
        synchronized (this) {
            finishClassicWorldWaterDragonPopulation(showId);
            Set<String> identities = Set.copyOf(ended.joinedIdentities());
            recipients = connectedSessions.stream()
                    .filter(session -> identities.contains(
                            key(session.sourceCharacterIdentity())))
                    .toList();
        }
        for (ClientSession recipient : recipients) {
            try {
                recipient.serverClassicWorldWaterDragonEnded(
                        showId, reason,
                        ClassicWorldWaterDragonCatalog.RESTART_EXIT, false);
            } catch (IOException error) {
                System.out.println("[LiTo] Water Dragon immediate end send failed show="
                        + showId + " player="
                        + recipient.selectedCharacterName() + ": "
                        + error.getMessage());
            }
        }
    }

    private ClassicWorldTarget findClassicWorldMonsterTarget(
            List<ClientSession> members,
            ClassicWorldMonsterPopulation.Snapshot actor,
            long targetObjectId) {
        if (targetObjectId <= 0) return null;
        for (ClientSession member : members) {
            ClassicWorldLocation location = classicWorldLocation(member);
            if (location == null || location.mapId() != actor.mapId()) continue;
            boolean playerEligible = member
                    .classicWorldMonsterRetainedTargetEligible(actor.mapId());
            if (member.playerObjectId() == targetObjectId
                    && playerEligible
                    && BattleSpellCatalog.isInNativeScreen(
                            actor.x(), actor.y(), location.x(), location.y())) {
                return new ClassicWorldTarget(member, targetObjectId,
                        location.x(), location.y(),
                        ClassicWorldTargetKind.PLAYER);
            }
            BattleCompanion.Snapshot companion =
                    member.classicWorldCompanionSnapshot(targetObjectId);
            if (companion != null && companion.active()
                    && BattleSpellCatalog.isInNativeScreen(
                            actor.x(), actor.y(), companion.x(), companion.y())) {
                return new ClassicWorldTarget(member, targetObjectId,
                        companion.x(), companion.y(),
                        ClassicWorldTargetKind.COMPANION);
            }
        }
        ClassicWorldDspyEscorts.Snapshot follower =
                classicWorldDspyEscorts.find(targetObjectId);
        if (follower != null && follower.alive()
                && follower.mapId() == actor.mapId()
                && BattleSpellCatalog.isInNativeScreen(
                        actor.x(), actor.y(), follower.x(), follower.y())) {
            for (ClientSession member : members) {
                if (follower.ownerIdentity().equals(key(
                        member.sourceCharacterIdentity()))) {
                    return new ClassicWorldTarget(member, targetObjectId,
                            follower.x(), follower.y(),
                            ClassicWorldTargetKind.DSPY_FOLLOWER);
                }
            }
        }
        return null;
    }

    private ClassicWorldTarget nearestClassicWorldMonsterTarget(
            List<ClientSession> members,
            ClassicWorldMonsterPopulation.Snapshot actor) {
        if (ClassicWorldGuardianCatalog.isGuardian(actor)) {
            // Retained World.getVisiblePlayer iteration chooses the first
            // visible qualifying PC. Guardians never spontaneously acquire
            // pets/summons; setLink still permits a provoked retained target.
            for (ClientSession candidate : members) {
                if (!candidate.classicWorldMonsterGuardAiEligible(actor.mapId())
                        || candidate.classicWorldGuardianDisposition()
                            == ClassicWorldGuardianCatalog.TargetDisposition.NONE) {
                    continue;
                }
                ClassicWorldLocation location = classicWorldLocation(candidate);
                if (location == null || !BattleSpellCatalog.isInNativeScreen(
                        actor.x(), actor.y(), location.x(), location.y())) continue;
                int distance = Math.max(Math.abs(actor.x() - location.x()),
                        Math.abs(actor.y() - location.y()));
                if (distance <= 12 || ClassicWorldMapCatalog.forMap(actor.mapId())
                        .hasLineOfSight(actor.x(), actor.y(),
                                location.x(), location.y())) {
                    return new ClassicWorldTarget(candidate,
                            candidate.playerObjectId(), location.x(), location.y(),
                            ClassicWorldTargetKind.PLAYER);
                }
            }
            return null;
        }
        if (ClassicWorldGuardCatalog.isGuard(actor)) {
            // Retained L1GuardInstance iterates visible players and takes the
            // first non-GM, living, non-ghost PC whose one-hour LastPk lease
            // remains active. It never spontaneously acquires companions.
            for (ClientSession candidate : members) {
                if (!candidate.classicWorldMonsterAiEligible(actor.mapId())
                        || !candidate.classicWorldGuardWanted()) continue;
                ClassicWorldLocation location = classicWorldLocation(candidate);
                if (location == null || !BattleSpellCatalog.isInNativeScreen(
                        actor.x(), actor.y(), location.x(), location.y())) continue;
                int distance = Math.max(Math.abs(actor.x() - location.x()),
                        Math.abs(actor.y() - location.y()));
                if (distance <= 12 || ClassicWorldMapCatalog.forMap(actor.mapId())
                        .hasLineOfSight(actor.x(), actor.y(),
                                location.x(), location.y())) {
                    return new ClassicWorldTarget(candidate,
                            candidate.playerObjectId(),
                            location.x(), location.y(),
                            ClassicWorldTargetKind.PLAYER);
                }
            }
            return null;
        }
        ClassicWorldMonsterAggressionCatalog.Profile profile =
                ClassicWorldMonsterAggressionCatalog.forTemplate(actor.template());
        // Retained World.getVisiblePlayer iteration returns the first qualifying
        // PC. Pets and summons are only legal after setLink/provoke and are
        // therefore handled by findClassicWorldMonsterTarget(), never here.
        for (ClientSession candidate : members) {
            if (!candidate.classicWorldMonsterRetainedTargetEligible(actor.mapId())) continue;
            ClassicWorldLocation location = classicWorldLocation(candidate);
            if (location == null) continue;
            if (BattleSpellCatalog.isInNativeScreen(
                    actor.x(), actor.y(), location.x(), location.y())) {
                int distance = Math.max(Math.abs(actor.x() - location.x()),
                        Math.abs(actor.y() - location.y()));
                boolean playerLineAllowed = actor.template().boss() || distance <= 12
                        || ClassicWorldMapCatalog.forMap(actor.mapId())
                                .hasLineOfSight(actor.x(), actor.y(),
                                        location.x(), location.y());
                ClassicWorldMonsterAggressionCatalog.PlayerDisposition disposition =
                        candidate.classicWorldMonsterDisposition(profile);
                if (playerLineAllowed
                        && ClassicWorldMonsterAggressionCatalog.acquisition(
                                actor.mapId(), profile, disposition)
                            != ClassicWorldMonsterAggressionCatalog.Acquisition.NONE) {
                    return new ClassicWorldTarget(candidate,
                            candidate.playerObjectId(), location.x(), location.y(),
                            ClassicWorldTargetKind.PLAYER);
                }
            }
        }
        return null;
    }

    private void groundTickSafely() {
        try {
            long nowNanos = System.nanoTime();
            for (GroundTick tick : groundTicks(nowNanos)) {
                try {
                    if (tick.npcCasterObjectId > 0 && tick.target != null) {
                        tick.target.serverTimeAttackGroundPulse(
                                tick.roomId, tick.npcCasterObjectId, tick.skillId);
                    } else if (tick.bot) {
                        tick.caster.serverGroundBotPulse(tick.roomId, tick.skillId);
                    } else if (tick.target != null) {
                        tick.target.serverGroundPulse(tick.roomId, tick.caster, tick.skillId);
                    }
                } catch (IOException error) {
                    System.out.println("[LiTo] ground-effect send failed room=" + tick.roomId
                            + " skill=" + tick.skillId + ": " + error.getMessage());
                }
            }
            for (WorldGroundTick tick : classicWorldGroundTicks(nowNanos)) {
                try {
                    if (tick.playerTarget != null) {
                        tick.playerTarget.serverClassicWorldGroundPlayerPulse(
                                tick.mapId, tick.instanceId, tick.caster,
                                tick.skillId);
                    } else if (tick.monsterTarget != null && tick.caster != null) {
                        tick.caster.serverClassicWorldGroundMonsterPulse(
                                tick.mapId, tick.instanceId,
                                tick.monsterTarget, tick.skillId);
                    }
                } catch (IOException error) {
                    System.out.println("[LiTo] classic-world ground-effect send failed map="
                            + tick.mapId + " instance=" + tick.instanceId
                            + " skill=" + tick.skillId + ": " + error.getMessage());
                }
            }
            for (CookingFireExpiry expiry : cookingFireExpiries(nowNanos)) {
                for (ClientSession recipient : expiry.recipients) {
                    try {
                        recipient.serverCookingFireExpired(expiry.roomId, expiry.fire);
                    } catch (IOException error) {
                        System.out.println("[LiTo] cooking-fire expiry send failed room="
                                + expiry.roomId + " object=" + expiry.fire.objectId()
                                + ": " + error.getMessage());
                    }
                }
            }
            for (CubeTick tick : cubeTicks(nowNanos)) {
                try {
                    switch (tick.event.kind()) {
                        case CUBE_EXPIRED -> {
                            for (ClientSession recipient : tick.recipients) {
                                recipient.serverCubeIgnitionExpired(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event.cube());
                            }
                        }
                        case ALLY_APPLIED, ENEMY_APPLIED,
                                ALLY_EXPIRED, ENEMY_EXPIRED -> {
                            if (tick.target != null) {
                                tick.target.serverCubeIgnitionStatus(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.caster, tick.event);
                            }
                        }
                        case ENEMY_DAMAGE -> {
                            if (tick.target != null) {
                                tick.target.serverCubeIgnitionDamage(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.caster, tick.event);
                            } else if (tick.caster != null
                                    && tick.event.targetKind()
                                        == BattleCubeIgnitionEffects.ActorKind.BOT) {
                                tick.caster.serverCubeIgnitionBotDamage(
                                        tick.roomId, tick.event);
                            } else if (tick.caster != null) {
                                tick.caster.serverCubeIgnitionMonsterDamage(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            }
                        }
                    }
                } catch (IOException error) {
                    System.out.println("[LiTo] Cube: Ignition send failed context="
                            + (tick.world() ? "world:" + tick.mapId + ":"
                                + tick.instanceId : "room:" + tick.roomId)
                            + " event=" + tick.event.kind()
                            + " object=" + tick.event.cube().objectId()
                            + ": " + error.getMessage());
                }
            }
            for (CubeQuakeTick tick : cubeQuakeTicks(nowNanos)) {
                try {
                    switch (tick.event.kind()) {
                        case CUBE_EXPIRED -> {
                            for (ClientSession recipient : tick.recipients) {
                                recipient.serverCubeQuakeExpired(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event.cube());
                            }
                        }
                        case ALLY_APPLIED, ENEMY_APPLIED,
                                ALLY_EXPIRED, ENEMY_EXPIRED -> {
                            if (tick.target != null) {
                                tick.target.serverCubeQuakeStatus(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.caster, tick.event);
                            }
                        }
                        case ENEMY_BIND_STARTED, ENEMY_BIND_EXPIRED -> {
                            if (tick.target != null) {
                                tick.target.serverCubeQuakeBind(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            } else if (tick.caster != null) {
                                tick.caster.serverCubeQuakeNonPlayerBind(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            }
                        }
                    }
                } catch (IOException error) {
                    System.out.println("[LiTo] Cube: Quake send failed context="
                            + (tick.world() ? "world:" + tick.mapId + ":"
                                + tick.instanceId : "room:" + tick.roomId)
                            + " event=" + tick.event.kind()
                            + " object=" + tick.event.cube().objectId()
                            + ": " + error.getMessage());
                }
            }
            for (CubeShockTick tick : cubeShockTicks(nowNanos)) {
                try {
                    switch (tick.event.kind()) {
                        case CUBE_EXPIRED -> {
                            for (ClientSession recipient : tick.recipients) {
                                recipient.serverCubeShockExpired(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event.cube());
                            }
                        }
                        case ALLY_APPLIED, ENEMY_APPLIED,
                                ALLY_EXPIRED, ENEMY_EXPIRED -> {
                            if (tick.target != null) {
                                tick.target.serverCubeShockStatus(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.caster, tick.event);
                            } else if (tick.caster != null) {
                                tick.caster.serverCubeShockNonPlayerStatus(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            }
                        }
                        case ENEMY_MR_STARTED, ENEMY_MR_EXPIRED -> {
                            if (tick.target != null) {
                                tick.target.serverCubeShockMagicResistance(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            }
                        }
                    }
                } catch (IOException error) {
                    System.out.println("[LiTo] Cube: Shock send failed context="
                            + (tick.world() ? "world:" + tick.mapId + ":"
                                + tick.instanceId : "room:" + tick.roomId)
                            + " event=" + tick.event.kind()
                            + " object=" + tick.event.cube().objectId()
                            + ": " + error.getMessage());
                }
            }
            for (CubeHarmonyTick tick : cubeHarmonyTicks(nowNanos)) {
                try {
                    switch (tick.event.kind()) {
                        case CUBE_EXPIRED -> {
                            for (ClientSession recipient : tick.recipients) {
                                recipient.serverCubeHarmonyExpired(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event.cube());
                            }
                        }
                        case STATUS_APPLIED, STATUS_EXPIRED -> {
                            if (tick.target != null) {
                                tick.target.serverCubeHarmonyStatus(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.caster, tick.event);
                            } else if (tick.caster != null) {
                                tick.caster.serverCubeHarmonyNonPlayerStatus(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            }
                        }
                        case MP_RECOVERY -> {
                            if (tick.target != null) {
                                tick.target.serverCubeHarmonyMpRecovery(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            } else if (tick.caster != null) {
                                tick.caster.serverCubeHarmonyNonPlayerMpRecovery(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            }
                        }
                        case HP_DAMAGE -> {
                            if (tick.target != null) {
                                tick.target.serverCubeHarmonyDamage(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.caster, tick.event);
                            } else if (tick.caster != null) {
                                tick.caster.serverCubeHarmonyNonPlayerDamage(
                                        tick.roomId, tick.mapId, tick.instanceId,
                                        tick.event);
                            }
                        }
                    }
                } catch (IOException error) {
                    System.out.println("[LiTo] Cube: Harmony send failed context="
                            + (tick.world() ? "world:" + tick.mapId + ":"
                                + tick.instanceId : "room:" + tick.roomId)
                            + " event=" + tick.event.kind()
                            + " object=" + tick.event.cube().objectId()
                            + ": " + error.getMessage());
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] ground-effect clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private synchronized List<CookingFireExpiry> cookingFireExpiries(long nowNanos) {
        List<CookingFireExpiry> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.battleStarted || room.match.phase() != BattleRules.Phase.RUNNING) {
                room.cookingFires.clear();
                room.cubeIgnitionEffects.clear();
                room.cubeQuakeEffects.clear();
                room.cubeShockEffects.clear();
                room.cubeHarmonyEffects.clear();
                room.patienceStatus.clear();
                room.shockStunEffects.clear();
                room.boneBreakEffects.clear();
                room.freezeEffects.clear();
                room.armorBreakExpiresAtNanos.clear();
                continue;
            }
            List<ClientSession> recipients = new ArrayList<>(room.members);
            for (BattleCookingFires.Fire fire : room.cookingFires.expire(nowNanos)) {
                result.add(new CookingFireExpiry(room.id, recipients, fire));
            }
        }
        return result;
    }

    private synchronized List<CubeTick> cubeTicks(long nowNanos) {
        List<CubeTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.battleStarted
                    || room.match.phase() != BattleRules.Phase.RUNNING) {
                room.cubeIgnitionEffects.clear();
                room.cubeQuakeEffects.clear();
                room.cubeShockEffects.clear();
                room.cubeHarmonyEffects.clear();
                room.patienceStatus.clear();
                continue;
            }
            List<ClientSession> recipients = new ArrayList<>(room.members);
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeIgnitionEffects.Actor> actors = new ArrayList<>();
            for (ClientSession combatant : room.combatants()) {
                BattleGroundEffects.Actor actor = combatant.groundEffectActor(room.id);
                if (actor == null) continue;
                sessions.put(actor.objectId(), combatant);
                actors.add(new BattleCubeIgnitionEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeIgnitionEffects.ActorKind.PLAYER));
            }
            if (room.combatantCount() < 2 && room.botHp > 0) {
                ArenaMapCatalog.Point bot = botPosition(room.id);
                if (bot != null) {
                    actors.add(new BattleCubeIgnitionEffects.Actor(
                            2, bot.x(), bot.y(), true,
                            BattleCubeIgnitionEffects.ActorKind.BOT));
                }
            }
            for (BattleQuestEncounter.Snapshot actor
                    : room.questEncounter.snapshots()) {
                actors.add(new BattleCubeIgnitionEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeIgnitionEffects.ActorKind.MONSTER));
            }
            for (BattleTimeAttackEncounter.Snapshot actor
                    : room.timeAttackEncounter.snapshots()) {
                actors.add(new BattleCubeIgnitionEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeIgnitionEffects.ActorKind.MONSTER));
            }
            BattleCubeIgnitionEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                return caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()))
                        ? BattleCubeIgnitionEffects.Relation.ALLY
                        : BattleCubeIgnitionEffects.Relation.ENEMY;
            };
            for (BattleCubeIgnitionEffects.Event event
                    : room.cubeIgnitionEffects.advance(
                            nowNanos, actors, alliance)) {
                result.add(new CubeTick(room.id, -1, -1, recipients,
                        sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
        }

        for (Iterator<Map.Entry<ClassicWorldEffectKey,
                BattleCubeIgnitionEffects>> iterator =
                classicWorldCubeIgnitionEffects.entrySet().iterator();
                iterator.hasNext();) {
            Map.Entry<ClassicWorldEffectKey, BattleCubeIgnitionEffects> entry =
                    iterator.next();
            ClassicWorldEffectKey key = entry.getKey();
            BattleCubeIgnitionEffects effects = entry.getValue();
            List<ClientSession> recipients = classicWorldMembers(
                    key.mapId(), key.instanceId());
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeIgnitionEffects.Actor> actors = new ArrayList<>();
            for (ClientSession member : recipients) {
                BattleGroundEffects.Actor actor = member.classicWorldGroundEffectActor(
                        key.mapId(), key.instanceId());
                if (actor == null) continue;
                sessions.put(actor.objectId(), member);
                actors.add(new BattleCubeIgnitionEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeIgnitionEffects.ActorKind.PLAYER));
            }
            LinkedHashMap<Long, ClassicWorldMonsterPopulation.Snapshot> monsters =
                    new LinkedHashMap<>();
            for (BattleCubeIgnitionEffects.Cube cube : effects.active(nowNanos)) {
                for (ClassicWorldMonsterPopulation.Snapshot monster
                        : classicWorldMonstersInScreen(key.mapId(), key.instanceId(),
                                cube.x(), cube.y())) {
                    if (monster.present()) monsters.put(monster.objectId(), monster);
                }
            }
            for (ClassicWorldMonsterPopulation.Snapshot monster : monsters.values()) {
                actors.add(new BattleCubeIgnitionEffects.Actor(
                        monster.objectId(), monster.x(), monster.y(), monster.alive(),
                        BattleCubeIgnitionEffects.ActorKind.MONSTER));
            }
            BattleCubeIgnitionEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                ClassicWorldLocation targetLocation =
                        classicWorldMembership.get(target);
                if (targetLocation != null
                        && ClassicWorldMapCatalog.forMap(key.mapId())
                            .isSafetyZone(
                                targetLocation.x(), targetLocation.y())) {
                    return BattleCubeIgnitionEffects.Relation.IGNORE;
                }
                return caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()))
                        ? BattleCubeIgnitionEffects.Relation.ALLY
                        : BattleCubeIgnitionEffects.Relation.ENEMY;
            };
            for (BattleCubeIgnitionEffects.Event event
                    : effects.advance(nowNanos, actors, alliance)) {
                result.add(new CubeTick(0, key.mapId(), key.instanceId(),
                        recipients, sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
            if (effects.active(nowNanos).isEmpty()
                    && effects.statusCount(nowNanos) == 0) iterator.remove();
        }
        return result;
    }

    private synchronized List<CubeQuakeTick> cubeQuakeTicks(long nowNanos) {
        List<CubeQuakeTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.battleStarted
                    || room.match.phase() != BattleRules.Phase.RUNNING) {
                room.cubeQuakeEffects.clear();
                room.cubeShockEffects.clear();
                room.cubeHarmonyEffects.clear();
                room.patienceStatus.clear();
                continue;
            }
            List<ClientSession> recipients = new ArrayList<>(room.members);
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeQuakeEffects.Actor> actors = new ArrayList<>();
            for (ClientSession combatant : room.combatants()) {
                BattleGroundEffects.Actor actor = combatant.groundEffectActor(room.id);
                if (actor == null) continue;
                sessions.put(actor.objectId(), combatant);
                actors.add(new BattleCubeQuakeEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeQuakeEffects.ActorKind.PLAYER));
            }
            if (room.combatantCount() < 2 && room.botHp > 0) {
                ArenaMapCatalog.Point bot = botPosition(room.id);
                if (bot != null) {
                    actors.add(new BattleCubeQuakeEffects.Actor(
                            2, bot.x(), bot.y(), true,
                            BattleCubeQuakeEffects.ActorKind.BOT));
                }
            }
            for (BattleQuestEncounter.Snapshot actor
                    : room.questEncounter.snapshots()) {
                actors.add(new BattleCubeQuakeEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeQuakeEffects.ActorKind.MONSTER));
            }
            for (BattleTimeAttackEncounter.Snapshot actor
                    : room.timeAttackEncounter.snapshots()) {
                actors.add(new BattleCubeQuakeEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeQuakeEffects.ActorKind.MONSTER));
            }
            BattleCubeQuakeEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                return caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()))
                        ? BattleCubeQuakeEffects.Relation.ALLY
                        : BattleCubeQuakeEffects.Relation.ENEMY;
            };
            BattleCubeQuakeEffects.BindProtection protection =
                    (targetId, kind, callbackNanos) -> {
                        if (kind == BattleCubeQuakeEffects.ActorKind.PLAYER) {
                            ClientSession target = sessions.get(targetId);
                            return target != null
                                    && target.cubeQuakeBindProtected(callbackNanos);
                        }
                        if (kind == BattleCubeQuakeEffects.ActorKind.MONSTER) {
                            return room.questEncounter.freezeActive(
                                        targetId, callbackNanos)
                                    || room.questEncounter.earthBindActive(
                                        targetId, callbackNanos)
                                    || room.timeAttackEncounter.freezeActive(
                                        targetId, callbackNanos)
                                    || room.timeAttackEncounter.earthBindActive(
                                        targetId, callbackNanos);
                        }
                        return false;
                    };
            for (BattleCubeQuakeEffects.Event event
                    : room.cubeQuakeEffects.advance(
                            nowNanos, actors, alliance, protection)) {
                result.add(new CubeQuakeTick(room.id, -1, -1, recipients,
                        sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
        }

        for (Iterator<Map.Entry<ClassicWorldEffectKey,
                BattleCubeQuakeEffects>> iterator =
                classicWorldCubeQuakeEffects.entrySet().iterator();
                iterator.hasNext();) {
            Map.Entry<ClassicWorldEffectKey, BattleCubeQuakeEffects> entry =
                    iterator.next();
            ClassicWorldEffectKey key = entry.getKey();
            BattleCubeQuakeEffects effects = entry.getValue();
            List<ClientSession> recipients = classicWorldMembers(
                    key.mapId(), key.instanceId());
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeQuakeEffects.Actor> actors = new ArrayList<>();
            for (ClientSession member : recipients) {
                BattleGroundEffects.Actor actor = member.classicWorldGroundEffectActor(
                        key.mapId(), key.instanceId());
                if (actor == null) continue;
                sessions.put(actor.objectId(), member);
                actors.add(new BattleCubeQuakeEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeQuakeEffects.ActorKind.PLAYER));
            }
            LinkedHashMap<Long, ClassicWorldMonsterPopulation.Snapshot> monsters =
                    new LinkedHashMap<>();
            for (BattleCubeQuakeEffects.Cube cube : effects.active(nowNanos)) {
                for (ClassicWorldMonsterPopulation.Snapshot monster
                        : classicWorldMonstersInScreen(key.mapId(), key.instanceId(),
                                cube.x(), cube.y())) {
                    if (monster.present()) monsters.put(monster.objectId(), monster);
                }
            }
            for (ClassicWorldMonsterPopulation.Snapshot monster : monsters.values()) {
                actors.add(new BattleCubeQuakeEffects.Actor(
                        monster.objectId(), monster.x(), monster.y(), monster.alive(),
                        BattleCubeQuakeEffects.ActorKind.MONSTER));
            }
            BattleCubeQuakeEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                ClassicWorldLocation targetLocation =
                        classicWorldMembership.get(target);
                if (targetLocation != null
                        && ClassicWorldMapCatalog.forMap(key.mapId())
                            .isSafetyZone(targetLocation.x(), targetLocation.y())) {
                    return BattleCubeQuakeEffects.Relation.IGNORE;
                }
                return caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()))
                        ? BattleCubeQuakeEffects.Relation.ALLY
                        : BattleCubeQuakeEffects.Relation.ENEMY;
            };
            BattleCubeQuakeEffects.BindProtection protection =
                    (targetId, kind, callbackNanos) -> {
                        if (kind == BattleCubeQuakeEffects.ActorKind.PLAYER) {
                            ClientSession target = sessions.get(targetId);
                            return target != null
                                    && target.cubeQuakeBindProtected(callbackNanos);
                        }
                        return kind == BattleCubeQuakeEffects.ActorKind.MONSTER
                                && classicWorldMonsterFreezeActive(
                                    key.mapId(), key.instanceId(),
                                    targetId, callbackNanos);
                    };
            for (BattleCubeQuakeEffects.Event event
                    : effects.advance(nowNanos, actors, alliance, protection)) {
                result.add(new CubeQuakeTick(0, key.mapId(), key.instanceId(),
                        recipients, sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
            if (effects.active(nowNanos).isEmpty()
                    && effects.statusCount(nowNanos) == 0
                    && effects.bindCount(nowNanos) == 0) iterator.remove();
        }
        return result;
    }

    private synchronized List<CubeShockTick> cubeShockTicks(long nowNanos) {
        List<CubeShockTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.battleStarted
                    || room.match.phase() != BattleRules.Phase.RUNNING) {
                room.cubeShockEffects.clear();
                room.cubeHarmonyEffects.clear();
                continue;
            }
            List<ClientSession> recipients = new ArrayList<>(room.members);
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeShockEffects.Actor> actors = new ArrayList<>();
            for (ClientSession combatant : room.combatants()) {
                BattleGroundEffects.Actor actor = combatant.groundEffectActor(room.id);
                if (actor == null) continue;
                sessions.put(actor.objectId(), combatant);
                actors.add(new BattleCubeShockEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeShockEffects.ActorKind.PLAYER));
            }
            if (room.combatantCount() < 2 && room.botHp > 0) {
                ArenaMapCatalog.Point bot = botPosition(room.id);
                if (bot != null) {
                    actors.add(new BattleCubeShockEffects.Actor(
                            2, bot.x(), bot.y(), true,
                            BattleCubeShockEffects.ActorKind.BOT));
                }
            }
            for (BattleQuestEncounter.Snapshot actor
                    : room.questEncounter.snapshots()) {
                actors.add(new BattleCubeShockEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeShockEffects.ActorKind.MONSTER));
            }
            for (BattleTimeAttackEncounter.Snapshot actor
                    : room.timeAttackEncounter.snapshots()) {
                actors.add(new BattleCubeShockEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeShockEffects.ActorKind.MONSTER));
            }
            BattleCubeShockEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                return caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()))
                        ? BattleCubeShockEffects.Relation.ALLY
                        : BattleCubeShockEffects.Relation.ENEMY;
            };
            for (BattleCubeShockEffects.Event event
                    : room.cubeShockEffects.advance(
                            nowNanos, actors, alliance)) {
                result.add(new CubeShockTick(room.id, -1, -1, recipients,
                        sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
        }

        for (Iterator<Map.Entry<ClassicWorldEffectKey,
                BattleCubeShockEffects>> iterator =
                classicWorldCubeShockEffects.entrySet().iterator();
                iterator.hasNext();) {
            Map.Entry<ClassicWorldEffectKey, BattleCubeShockEffects> entry =
                    iterator.next();
            ClassicWorldEffectKey key = entry.getKey();
            BattleCubeShockEffects effects = entry.getValue();
            List<ClientSession> recipients = classicWorldMembers(
                    key.mapId(), key.instanceId());
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeShockEffects.Actor> actors = new ArrayList<>();
            for (ClientSession member : recipients) {
                BattleGroundEffects.Actor actor = member.classicWorldGroundEffectActor(
                        key.mapId(), key.instanceId());
                if (actor == null) continue;
                sessions.put(actor.objectId(), member);
                actors.add(new BattleCubeShockEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeShockEffects.ActorKind.PLAYER));
            }
            LinkedHashMap<Long, ClassicWorldMonsterPopulation.Snapshot> monsters =
                    new LinkedHashMap<>();
            for (BattleCubeShockEffects.Cube cube : effects.active(nowNanos)) {
                for (ClassicWorldMonsterPopulation.Snapshot monster
                        : classicWorldMonstersInScreen(key.mapId(), key.instanceId(),
                                cube.x(), cube.y())) {
                    if (monster.present()) monsters.put(monster.objectId(), monster);
                }
            }
            for (ClassicWorldMonsterPopulation.Snapshot monster : monsters.values()) {
                actors.add(new BattleCubeShockEffects.Actor(
                        monster.objectId(), monster.x(), monster.y(), monster.alive(),
                        BattleCubeShockEffects.ActorKind.MONSTER));
            }
            BattleCubeShockEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                boolean ally = caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()));
                if (ally) return BattleCubeShockEffects.Relation.ALLY;
                ClassicWorldLocation targetLocation =
                        classicWorldMembership.get(target);
                if (targetLocation != null
                        && ClassicWorldMapCatalog.forMap(key.mapId())
                            .isSafetyZone(targetLocation.x(), targetLocation.y())) {
                    return BattleCubeShockEffects.Relation.IGNORE;
                }
                return BattleCubeShockEffects.Relation.ENEMY;
            };
            for (BattleCubeShockEffects.Event event
                    : effects.advance(nowNanos, actors, alliance)) {
                result.add(new CubeShockTick(0, key.mapId(), key.instanceId(),
                        recipients, sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
            if (effects.active(nowNanos).isEmpty()
                    && effects.statusCount(nowNanos) == 0
                    && effects.mrLeaseCount(nowNanos) == 0) iterator.remove();
        }
        return result;
    }

    private synchronized List<CubeHarmonyTick> cubeHarmonyTicks(long nowNanos) {
        List<CubeHarmonyTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.battleStarted
                    || room.match.phase() != BattleRules.Phase.RUNNING) {
                room.cubeHarmonyEffects.clear();
                continue;
            }
            List<ClientSession> recipients = new ArrayList<>(room.members);
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeHarmonyEffects.Actor> actors = new ArrayList<>();
            for (ClientSession combatant : room.combatants()) {
                BattleGroundEffects.Actor actor = combatant.groundEffectActor(room.id);
                if (actor == null) continue;
                sessions.put(actor.objectId(), combatant);
                actors.add(new BattleCubeHarmonyEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeHarmonyEffects.ActorKind.PLAYER));
            }
            if (room.combatantCount() < 2 && room.botHp > 0) {
                ArenaMapCatalog.Point bot = botPosition(room.id);
                if (bot != null) {
                    actors.add(new BattleCubeHarmonyEffects.Actor(
                            2, bot.x(), bot.y(), true,
                            BattleCubeHarmonyEffects.ActorKind.BOT));
                }
            }
            for (BattleQuestEncounter.Snapshot actor
                    : room.questEncounter.snapshots()) {
                actors.add(new BattleCubeHarmonyEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeHarmonyEffects.ActorKind.MONSTER));
            }
            for (BattleTimeAttackEncounter.Snapshot actor
                    : room.timeAttackEncounter.snapshots()) {
                actors.add(new BattleCubeHarmonyEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeHarmonyEffects.ActorKind.MONSTER));
            }
            BattleCubeHarmonyEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                return caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()))
                        ? BattleCubeHarmonyEffects.Relation.ALLY
                        : BattleCubeHarmonyEffects.Relation.ENEMY;
            };
            for (BattleCubeHarmonyEffects.Event event
                    : room.cubeHarmonyEffects.advance(
                            nowNanos, actors, alliance)) {
                result.add(new CubeHarmonyTick(room.id, -1, -1, recipients,
                        sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
        }

        for (Iterator<Map.Entry<ClassicWorldEffectKey,
                BattleCubeHarmonyEffects>> iterator =
                classicWorldCubeHarmonyEffects.entrySet().iterator();
                iterator.hasNext();) {
            Map.Entry<ClassicWorldEffectKey, BattleCubeHarmonyEffects> entry =
                    iterator.next();
            ClassicWorldEffectKey key = entry.getKey();
            BattleCubeHarmonyEffects effects = entry.getValue();
            List<ClientSession> recipients = classicWorldMembers(
                    key.mapId(), key.instanceId());
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleCubeHarmonyEffects.Actor> actors = new ArrayList<>();
            for (ClientSession member : recipients) {
                BattleGroundEffects.Actor actor = member.classicWorldGroundEffectActor(
                        key.mapId(), key.instanceId());
                if (actor == null) continue;
                sessions.put(actor.objectId(), member);
                actors.add(new BattleCubeHarmonyEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        BattleCubeHarmonyEffects.ActorKind.PLAYER));
            }
            LinkedHashMap<Long, ClassicWorldMonsterPopulation.Snapshot> monsters =
                    new LinkedHashMap<>();
            for (BattleCubeHarmonyEffects.Cube cube : effects.active(nowNanos)) {
                for (ClassicWorldMonsterPopulation.Snapshot monster
                        : classicWorldMonstersInScreen(key.mapId(), key.instanceId(),
                                cube.x(), cube.y())) {
                    if (monster.present()) monsters.put(monster.objectId(), monster);
                }
            }
            for (ClassicWorldMonsterPopulation.Snapshot monster : monsters.values()) {
                actors.add(new BattleCubeHarmonyEffects.Actor(
                        monster.objectId(), monster.x(), monster.y(), monster.alive(),
                        BattleCubeHarmonyEffects.ActorKind.MONSTER));
            }
            BattleCubeHarmonyEffects.Alliance alliance = (casterId, targetId) -> {
                ClientSession caster = sessions.get(casterId);
                ClientSession target = sessions.get(targetId);
                boolean ally = caster != null && target != null
                        && (sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())
                            || sameClassicParty(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity()));
                if (ally) return BattleCubeHarmonyEffects.Relation.ALLY;
                ClassicWorldLocation targetLocation =
                        classicWorldMembership.get(target);
                if (targetLocation != null
                        && ClassicWorldMapCatalog.forMap(key.mapId())
                            .isSafetyZone(targetLocation.x(), targetLocation.y())) {
                    return BattleCubeHarmonyEffects.Relation.IGNORE;
                }
                return BattleCubeHarmonyEffects.Relation.ENEMY;
            };
            for (BattleCubeHarmonyEffects.Event event
                    : effects.advance(nowNanos, actors, alliance)) {
                result.add(new CubeHarmonyTick(0, key.mapId(), key.instanceId(),
                        recipients, sessions.get(event.cube().casterObjectId()),
                        sessions.get(event.targetObjectId()), event));
            }
            if (effects.active(nowNanos).isEmpty()
                    && effects.statusCount(nowNanos) == 0) iterator.remove();
        }
        return result;
    }

    private void statusTickSafely() {
        try {
            long nowNanos = System.nanoTime();
            for (ClientSession session : timedStatusSessions()) {
                try {
                    session.serverExpireTimedStatuses(nowNanos);
                } catch (IOException error) {
                    System.out.println("[LiTo] timed-status send failed player="
                            + session.playerName() + ": " + error.getMessage());
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] timed-status clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private void itemUserTickSafely() {
        try {
            long nowEpochMillis = System.currentTimeMillis();
            List<ClientSession> sessions = connectedSessionsSnapshot();
            Set<String> onlineCharacters = sessions.stream()
                    .map(ClientSession::timedInventoryCharacterIdentity)
                    .filter(identity -> identity != null && !identity.isBlank())
                    .collect(java.util.stream.Collectors.toSet());
            for (ClientSession session : sessions) {
                try {
                    session.serverExpireTimedInventory(nowEpochMillis);
                } catch (IOException error) {
                    System.out.println("[LiTo] timed-inventory scan failed player="
                            + session.playerName() + ": " + error.getMessage());
                }
            }
            int offlineExpired = state == null ? 0
                    : state.expireOfflineTimedItems(nowEpochMillis, onlineCharacters);
            if (offlineExpired != 0) {
                System.out.println("[LiTo] timed-inventory offline expired objects="
                        + offlineExpired);
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] timed-inventory clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    void houseAuctionTickSafely() {
        try {
            long now = System.currentTimeMillis() / 1_000L;
            for (String source : classicSourceNames) {
                try {
                    settleHouseAuctionsForSource(source, now);
                } catch (Throwable sourceFailure) {
                    System.err.println("[LiTo] house-auction source recovered source="
                            + source + " error=" + sourceFailure.getClass().getSimpleName()
                            + ": " + sourceFailure.getMessage());
                }
            }
        } catch (Throwable error) {
            System.err.println("[LiTo] house-auction clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private void settleHouseAuctionsForSource(String source, long now)
            throws Exception {
        AccountRepository repository = accounts.repositoryForClassicSource(source);
        RepositoryHouseAuctionSnapshot snapshot = repository.houseAuctions();
        if (!snapshot.authoritative()) return;
        for (GameStateStore.HouseAuction auction : snapshot.auctions()) {
            if (auction.deadlineEpochSeconds() > now) continue;
            RepositoryHouseSettlement result = repository
                    .persistHouseAuctionSettlement(auction, now, 10);
            if (!result.settled()) {
                if (result.status() != RepositoryHouseSettlementStatus.STALE
                        && result.status() != RepositoryHouseSettlementStatus.NOT_FOUND) {
                    System.err.println("[LiTo] house-auction settlement rejected source="
                            + source + " house=" + auction.houseId()
                            + " status=" + result.status());
                }
                continue;
            }
            if (result.status() != RepositoryHouseSettlementStatus.EXTENDED) {
                for (ClientSession session : connectedSessionsSnapshot()) {
                    try {
                        session.serverHouseAuctionSettlement(source, auction, result);
                    } catch (IOException notificationFailure) {
                        System.err.println("[LiTo] house-auction notification failed "
                                + "source=" + source + " house=" + auction.houseId()
                                + " player=" + session.playerName() + " error="
                                + notificationFailure.getMessage());
                    }
                }
            }
            System.out.println("[LiTo] house-auction settled source="
                    + source + " house=" + auction.houseId()
                    + " status=" + result.status() + " payout=" + result.payout());
        }
    }

    void houseTaxTickSafely() {
        try {
            long now = System.currentTimeMillis() / 1_000L;
            for (String source : classicSourceNames) {
                try {
                    settleHouseTaxesForSource(source, now);
                } catch (Throwable sourceFailure) {
                    System.err.println("[LiTo] house-tax source recovered source="
                            + source + " error="
                            + sourceFailure.getClass().getSimpleName()
                            + ": " + sourceFailure.getMessage());
                }
            }
        } catch (Throwable error) {
            System.err.println("[LiTo] house-tax clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private void settleHouseTaxesForSource(String source, long now)
            throws Exception {
        AccountRepository repository = accounts.repositoryForClassicSource(source);
        RepositoryHouseTaxSnapshot snapshot = repository.expiredHouseTaxes(now);
        if (!snapshot.authoritative()) return;
        for (RepositoryHouseTaxState house : snapshot.houses()) {
            RepositoryHouseTaxMutation result = repository.persistHouseTaxExpiry(
                    house, now, 10);
            if (!result.auctioned()) {
                if (result.status() != RepositoryHouseTaxStatus.STALE
                        && result.status() != RepositoryHouseTaxStatus.NOT_FOUND
                        && result.status() != RepositoryHouseTaxStatus.ALREADY_ON_SALE) {
                    System.err.println("[LiTo] house-tax expiry rejected source="
                            + source + " house=" + house.houseId()
                            + " status=" + result.status());
                }
                continue;
            }
            System.out.println("[LiTo] house-tax auctioned source=" + source
                    + " house=" + house.houseId()
                    + " previous-clan=" + result.previousClanId());
        }
    }

    private void magicDollTickSafely() {
        try {
            long nowNanos = System.nanoTime();
            for (ClientSession session : connectedSessionsSnapshot()) {
                try {
                    session.serverMagicDollTick(nowNanos);
                } catch (IOException error) {
                    System.out.println("[LiTo] magic-doll timer send failed player="
                            + session.playerName() + ": " + error.getMessage());
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] magic-doll clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private void companionTickSafely() {
        try {
            long nowNanos = System.nanoTime();
            for (CompanionTick tick : companionTicks()) {
                try {
                    tick.owner.serverCompanionTick(tick.roomId, nowNanos);
                } catch (IOException error) {
                    System.out.println("[LiTo] companion send failed room=" + tick.roomId
                            + " owner=" + tick.owner.playerName() + ": " + error.getMessage());
                }
            }
            for (WorldCompanionTick tick : worldCompanionTicks()) {
                try {
                    tick.owner.serverClassicWorldCompanionTick(
                            tick.mapId, nowNanos);
                } catch (IOException error) {
                    System.out.println("[LiTo] classic-world companion send failed map="
                            + tick.mapId + " owner=" + tick.owner.playerName()
                            + ": " + error.getMessage());
                }
            }
        } catch (Throwable error) {
            System.out.println("[LiTo] companion clock recovered: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private synchronized List<CompanionTick> companionTicks() {
        List<CompanionTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.battleStarted || room.match.phase() != BattleRules.Phase.RUNNING) continue;
            for (ClientSession owner : room.combatants()) {
                result.add(new CompanionTick(room.id, owner));
            }
        }
        return result;
    }

    private synchronized List<WorldCompanionTick> worldCompanionTicks() {
        List<WorldCompanionTick> result = new ArrayList<>();
        for (Map.Entry<ClientSession, ClassicWorldLocation> entry
                : classicWorldMembership.entrySet()) {
            result.add(new WorldCompanionTick(
                    entry.getValue().mapId(), entry.getKey()));
        }
        return result;
    }

    private synchronized List<ClientSession> timedStatusSessions() {
        LinkedHashSet<ClientSession> sessions = new LinkedHashSet<>();
        for (Room room : rooms.values()) sessions.addAll(room.members);
        sessions.addAll(classicWorldMembership.keySet());
        return new ArrayList<>(sessions);
    }

    private synchronized List<GroundTick> groundTicks(long nowNanos) {
        List<GroundTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.battleStarted || room.match.phase() != BattleRules.Phase.RUNNING) {
                room.groundEffects.clear();
                room.groundItems.clear();
                room.cookingFires.clear();
                room.cubeIgnitionEffects.clear();
                room.cubeQuakeEffects.clear();
                room.cubeShockEffects.clear();
                room.cubeHarmonyEffects.clear();
                room.patienceStatus.clear();
                room.shockStunEffects.clear();
                room.boneBreakEffects.clear();
                room.freezeEffects.clear();
                room.armorBreakExpiresAtNanos.clear();
                continue;
            }
            List<ClientSession> combatants = room.combatants();
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<BattleGroundEffects.Actor> actors = new ArrayList<>();
            for (ClientSession combatant : combatants) {
                BattleGroundEffects.Actor actor = combatant.groundEffectActor(room.id);
                if (actor == null) continue;
                sessions.put(actor.objectId(), combatant);
                actors.add(actor);
            }
            if (combatants.size() < 2 && room.botHp > 0) {
                ArenaMapCatalog.Point bot = botPosition(room.id);
                if (bot != null) {
                    actors.add(new BattleGroundEffects.Actor(2, bot.x(), bot.y(), true, true));
                }
            }
            for (BattleGroundEffects.Pulse pulse : room.groundEffects.advance(nowNanos, actors)) {
                ClientSession caster = sessions.get(pulse.casterObjectId());
                if (caster == null) {
                    boolean npcCaster = room.match.rules().mode()
                            == BattleRules.Mode.TIME_ATTACK
                            && room.timeAttackEncounter.snapshots().stream()
                                .anyMatch(actor -> actor.alive()
                                    && actor.objectId() == pulse.casterObjectId());
                    if (!npcCaster || pulse.bot()) continue;
                    ClientSession target = sessions.get(pulse.targetObjectId());
                    if (target != null) {
                        result.add(new GroundTick(room.id, null, target, false,
                                pulse.skillId(), pulse.casterObjectId()));
                    }
                    continue;
                }
                if (pulse.bot()) {
                    if (combatants.size() < 2 && pulse.targetObjectId() == 2 && room.botHp > 0) {
                        result.add(new GroundTick(room.id, caster, null, true, pulse.skillId()));
                    }
                    continue;
                }
                ClientSession target = sessions.get(pulse.targetObjectId());
                if (target == null) continue;
                if (pulse.skillId() == BattleSpellCatalog.FIRE_WALL
                        && !room.match.canAttack(caster.playerObjectId(), target.playerObjectId())) {
                    continue;
                }
                result.add(new GroundTick(room.id, caster, target, false, pulse.skillId()));
            }
        }
        return result;
    }

    private synchronized List<WorldGroundTick> classicWorldGroundTicks(
            long nowNanos) {
        List<WorldGroundTick> result = new ArrayList<>();
        for (Iterator<Map.Entry<ClassicWorldEffectKey,
                ClassicWorldGroundEffects>> iterator =
                classicWorldGroundEffects.entrySet().iterator();
                iterator.hasNext();) {
            Map.Entry<ClassicWorldEffectKey, ClassicWorldGroundEffects> entry =
                    iterator.next();
            ClassicWorldEffectKey key = entry.getKey();
            ClassicWorldGroundEffects effects = entry.getValue();
            List<ClientSession> recipients = classicWorldMembers(
                    key.mapId(), key.instanceId());
            Map<Long, ClientSession> sessions = new LinkedHashMap<>();
            List<ClassicWorldGroundEffects.Actor> actors = new ArrayList<>();
            for (ClientSession member : recipients) {
                BattleGroundEffects.Actor actor = member.classicWorldGroundEffectActor(
                        key.mapId(), key.instanceId());
                if (actor == null) continue;
                sessions.put(actor.objectId(), member);
                actors.add(new ClassicWorldGroundEffects.Actor(
                        actor.objectId(), actor.x(), actor.y(), actor.alive(),
                        ClassicWorldGroundEffects.ActorKind.PLAYER));
            }
            LinkedHashMap<Long, ClassicWorldMonsterPopulation.Snapshot> monsters =
                    new LinkedHashMap<>();
            for (ClassicWorldGroundEffects.Active active : effects.active(nowNanos)) {
                if (active.skillId() != BattleSpellCatalog.FIRE_WALL) continue;
                for (ClassicWorldMonsterPopulation.Snapshot monster
                        : classicWorldMonstersInScreen(key.mapId(), key.instanceId(),
                                active.x(), active.y())) {
                    if (monster.present() && monster.alive()) {
                        monsters.put(monster.objectId(), monster);
                    }
                }
            }
            for (ClassicWorldMonsterPopulation.Snapshot monster : monsters.values()) {
                actors.add(new ClassicWorldGroundEffects.Actor(
                        monster.objectId(), monster.x(), monster.y(), monster.alive(),
                        ClassicWorldGroundEffects.ActorKind.MONSTER));
            }
            for (ClassicWorldGroundEffects.Pulse pulse
                    : effects.advance(nowNanos, actors)) {
                ClientSession caster = sessions.get(pulse.casterObjectId());
                if (caster == null || pulse.skillId() != BattleSpellCatalog.FIRE_WALL) {
                    continue;
                }
                if (pulse.targetKind() == ClassicWorldGroundEffects.ActorKind.PLAYER) {
                    ClientSession target = sessions.get(pulse.targetObjectId());
                    ClassicWorldLocation targetLocation =
                            classicWorldMembership.get(target);
                    if (target == null || targetLocation == null
                            || ClassicWorldMapCatalog.forMap(key.mapId())
                                .isSafetyZone(targetLocation.x(), targetLocation.y())
                            || sameClan(caster.sourceCharacterIdentity(),
                                    target.sourceCharacterIdentity())) {
                        continue;
                    }
                    result.add(new WorldGroundTick(
                            key.mapId(), key.instanceId(), recipients,
                            caster, target, null, pulse.skillId()));
                } else {
                    ClassicWorldMonsterPopulation.Snapshot target =
                            monsters.get(pulse.targetObjectId());
                    if (target != null) {
                        result.add(new WorldGroundTick(
                                key.mapId(), key.instanceId(), recipients,
                                caster, null, target, pulse.skillId()));
                    }
                }
            }
            if (effects.activeCount(nowNanos) == 0) iterator.remove();
        }
        return result;
    }

    private synchronized List<BotTick> botTicks(long nowNanos) {
        List<BotTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            List<ClientSession> combatants = room.combatants();
            if (!room.battleStarted || room.disconnectResolutionPending
                    || combatants.size() != 1 || room.botHp <= 0
                    || room.match.rules().mode() == BattleRules.Mode.PET_RACE
                    || room.match.rules().mode() == BattleRules.Mode.QUEST_BATTLE
                    || room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK) continue;
            if (room.cubeQuakeEffects.movementStopped(2, nowNanos)) continue;
            int[] offset = {0, 1, 0, -1};
            room.botStep = (room.botStep + 1) & 3;
            ArenaMapCatalog.Point spawn = ArenaMapCatalog
                    .forMap(room.match.rules().mapId()).botSpawn();
            result.add(new BotTick(room.id, combatants.get(0),
                    spawn.x() + offset[room.botStep], spawn.y(),
                    Math.max(0, Math.addExact(aiDamage,
                            room.botArmBreakerStatus.meleeDamageModifier(
                                    nowNanos)))));
        }
        return result;
    }

    synchronized List<MonsterAiTick> monsterAiTicks(long nowNanos) {
        List<MonsterAiTick> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            BattleRules.Mode mode = room.match.rules().mode();
            if (!room.battleStarted || room.disconnectResolutionPending
                    || room.match.phase() != BattleRules.Phase.RUNNING
                    || nowNanos < room.pveAiPublishAfterNanos
                    || (mode != BattleRules.Mode.QUEST_BATTLE
                        && mode != BattleRules.Mode.TIME_ATTACK)) continue;

            if (mode == BattleRules.Mode.QUEST_BATTLE) {
                for (BattleQuestEncounter.Snapshot actor
                        : room.questEncounter.snapshots()) {
                    ClientSession target = nearestMonsterTarget(
                            room, actor.force(), actor.x(), actor.y(), true);
                    if (target == null) continue;
                    appendMonsterAiTick(result, room, mode, actor.objectId(),
                            actor.force(), actor.x(), actor.y(), target, nowNanos);
                }
            } else {
                for (BattleTimeAttackEncounter.Snapshot actor
                        : room.timeAttackEncounter.snapshots()) {
                    if (timeAttackDragonCombatFixtureOnly
                            && !BattleTimeAttackEncounter.isDragonNpc(
                                    actor.npcId())) continue;
                    if (timeAttackDragonCombatFixtureOnly
                            && room.battleRoster.stream().filter(candidate ->
                                    candidate.battleAiTargetEligible(room.id)
                                    && chebyshev(actor.x(), actor.y(),
                                            candidate.battleX(), candidate.battleY())
                                            <= 3).count()
                                    < timeAttackDragonCombatFixtureMinimumTargets) {
                        continue;
                    }
                    ClientSession target = nearestMonsterTarget(
                            room, 0, actor.x(), actor.y(), false);
                    if (target == null) continue;
                    appendMonsterAiTick(result, room, mode, actor.objectId(),
                            0, actor.x(), actor.y(), target, nowNanos);
                }
            }
        }
        return result;
    }

    synchronized void armPveAi(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted) return;
        BattleRules.Mode mode = room.match.rules().mode();
        if (mode != BattleRules.Mode.QUEST_BATTLE
                && mode != BattleRules.Mode.TIME_ATTACK) return;
        room.pveAiPublishAfterNanos = System.nanoTime()
                + PVE_AI_PUBLICATION_GRACE_NANOS;
    }

    private void appendMonsterAiTick(List<MonsterAiTick> result, Room room,
                                     BattleRules.Mode mode, long objectId,
                                     int force, int sourceX, int sourceY,
                                     ClientSession target, long nowNanos) {
        // Retained Cube: Quake applies STATUS_FREEZE/paralyzed to NPC targets;
        // unlike PC MOVE_STOP this suppresses the monster's complete AI tick.
        if (room.cubeQuakeEffects.movementStopped(objectId, nowNanos)) return;
        int targetX = target.battleX();
        int targetY = target.battleY();
        int range = chebyshev(sourceX, sourceY, targetX, targetY);
        boolean sleeping = mode == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.sleepActive(objectId, nowNanos)
                : room.timeAttackEncounter.sleepActive(objectId, nowNanos);
        boolean frozen = mode == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.freezeActive(objectId, nowNanos)
                : room.timeAttackEncounter.freezeActive(objectId, nowNanos);
        if (sleeping || frozen) return;
        String source = mode == BattleRules.Mode.QUEST_BATTLE
                ? BattleQuestEncounter.AI_SOURCE + ";damage="
                    + BattleQuestEncounter.AI_DAMAGE_SOURCE
                : BattleTimeAttackEncounter.AI_SOURCE + ";damage="
                    + BattleTimeAttackEncounter.AI_DAMAGE_SOURCE;
        // aiDamage remains the legacy ArenaBot fixture. Quest and Time Attack
        // NPC damage is recomputed from the selected source at publication.
        int tickDamage = 0;
        if (mode == BattleRules.Mode.QUEST_BATTLE) {
            BattleQuestEncounter.AiAttack action =
                    timeAttackMonsterActNoFixture >= 0
                        ? room.questEncounter.aiAttackForActNoFixture(
                                objectId, targetX, targetY, nowNanos,
                                timeAttackMonsterActNoFixture,
                                timeAttackMonsterCandidateFixtureSelector)
                        : room.questEncounter.aiAttackFor(
                                objectId, targetX, targetY, nowNanos);
            if (action != null) {
                result.add(new MonsterAiTick(room.id, mode, objectId, force,
                        sourceX, sourceY, sourceX, sourceY,
                        heading(sourceX, sourceY, targetX, targetY), target,
                        tickDamage, true, action.magicSkill(), null,
                        action.physicalSkill(), action.summonSkill(), null,
                        List.of(), action.spawned(), action.triggerRoll(),
                        source + ";mobskill=" + BattleQuestEncounter.MOBSKILL_SOURCE));
                return;
            }
            if (range <= 1) return;
        } else {
            BattleTimeAttackEncounter.AiAttack action =
                    timeAttackMonsterActNoFixture >= 0
                        ? room.timeAttackEncounter.aiAttackForActNoFixture(
                                objectId, targetX, targetY, nowNanos,
                                timeAttackMonsterActNoFixture,
                                timeAttackMonsterCandidateFixtureSelector)
                    : timeAttackMonsterMagicFixtureTriggerRoll > 0
                        ? room.timeAttackEncounter.aiAttackFor(
                                objectId, targetX, targetY, nowNanos,
                                timeAttackMonsterMagicFixtureTriggerRoll,
                                timeAttackMonsterPhysicalFixtureTriggerRoll > 0
                                    ? timeAttackMonsterPhysicalFixtureTriggerRoll : 100,
                                timeAttackMonsterCandidateFixtureSelector)
                        : room.timeAttackEncounter.aiAttackFor(
                                objectId, targetX, targetY, nowNanos);
            if (action != null) {
                result.add(new MonsterAiTick(room.id, mode, objectId, force,
                        sourceX, sourceY, sourceX, sourceY,
                        heading(sourceX, sourceY, targetX, targetY), target,
                        tickDamage, true, action.magicSkill(),
                        action.visualSkill(), action.physicalSkill(), action.summonSkill(),
                        action.groundSkill(),
                        action.spawned(), List.of(), action.triggerRoll(),
                        source + ";mobskill="
                                + BattleTimeAttackMagicSkillCatalog.SOURCE));
                return;
            }
            if (range <= 1) return;
        }

        // Retained NpcMove.moveDirection refuses pursuit while DARKNESS is
        // active once line distance reaches two tiles. Adjacent attacks remain
        // legal, so this gate belongs only on the movement branch above range 1.
        boolean blinded = mode == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.blindActive(objectId, nowNanos)
                : room.timeAttackEncounter.blindActive(objectId, nowNanos);
        if (blinded) return;

        ArenaMapCatalog.Arena arena = ArenaMapCatalog.forMap(
                mode == BattleRules.Mode.QUEST_BATTLE
                        ? BattleRules.arenaMapForForce(room.match.rules(), force)
                        : room.match.rules().mapId());
        int[] step = chooseMonsterStep(room, mode, force, objectId,
                sourceX, sourceY, targetX, targetY, arena);
        if (step == null) return;
        boolean moved = mode == BattleRules.Mode.QUEST_BATTLE
                ? room.questEncounter.moveForAi(objectId, step[0], step[1], nowNanos) != null
                : room.timeAttackEncounter.moveForAi(objectId, step[0], step[1], nowNanos) != null;
        if (moved) {
            result.add(new MonsterAiTick(room.id, mode, objectId, force,
                    sourceX, sourceY, step[0], step[1], step[2], target,
                    tickDamage, false, null, null, null, List.of(), 0, source));
        }
    }

    private static ClientSession nearestMonsterTarget(Room room, int force,
                                                       int x, int y,
                                                       boolean sameForceOnly) {
        ClientSession nearest = null;
        int nearestRange = Integer.MAX_VALUE;
        for (ClientSession candidate : room.battleRoster) {
            if (!candidate.battleAiTargetEligible(room.id)
                    || (sameForceOnly && forceOf(room, candidate) != force)) continue;
            int range = chebyshev(x, y, candidate.battleX(), candidate.battleY());
            if (nearest == null || range < nearestRange
                    || (range == nearestRange
                        && Long.compareUnsigned(candidate.playerObjectId(),
                            nearest.playerObjectId()) < 0)) {
                nearest = candidate;
                nearestRange = range;
            }
        }
        return nearest;
    }

    private static int[] chooseMonsterStep(Room room, BattleRules.Mode mode,
                                           int force, long movingObjectId,
                                           int sourceX, int sourceY,
                                           int targetX, int targetY,
                                           ArenaMapCatalog.Arena arena) {
        int preferred = heading(sourceX, sourceY, targetX, targetY);
        int[] order = {
                preferred, (preferred + 1) & 7, (preferred + 7) & 7,
                (preferred + 2) & 7, (preferred + 6) & 7,
                (preferred + 3) & 7, (preferred + 5) & 7, (preferred + 4) & 7
        };
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        int currentRange = chebyshev(sourceX, sourceY, targetX, targetY);
        for (int candidate : order) {
            int x = sourceX + dx[candidate];
            int y = sourceY + dy[candidate];
            if ((x == targetX && y == targetY)
                    || !arena.nativeCanStep(sourceX, sourceY, x, y)) {
                continue;
            }
            if (monsterTileOccupied(room, mode, force, movingObjectId, x, y)) continue;
            if (chebyshev(x, y, targetX, targetY) <= currentRange) {
                return new int[] {x, y, candidate};
            }
        }
        return null;
    }

    private static boolean monsterTileOccupied(Room room, BattleRules.Mode mode,
                                               int force, long movingObjectId,
                                               int x, int y) {
        if (room.door.blocks(x, y) || room.siegeGate.blocks(x, y)
                || room.guardianTowers.blocks(x, y)) return true;
        if (mode == BattleRules.Mode.QUEST_BATTLE) {
            for (BattleQuestEncounter.Snapshot actor
                    : room.questEncounter.snapshots(force)) {
                if (actor.objectId() != movingObjectId
                        && actor.x() == x && actor.y() == y) return true;
            }
        } else {
            for (BattleTimeAttackEncounter.Snapshot actor
                    : room.timeAttackEncounter.snapshots()) {
                if (actor.objectId() != movingObjectId
                        && actor.x() == x && actor.y() == y) return true;
            }
        }
        for (ClientSession combatant : room.battleRoster) {
            if (mode == BattleRules.Mode.QUEST_BATTLE
                    && forceOf(room, combatant) != force) continue;
            if (combatant.battleAiTargetEligible(room.id)
                    && combatant.battleX() == x && combatant.battleY() == y) return true;
            for (BattleCompanion.Snapshot companion
                    : combatant.controlledCompanionSnapshots()) {
                if (companion.active() && companion.x() == x && companion.y() == y) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int chebyshev(int leftX, int leftY, int rightX, int rightY) {
        return Math.max(Math.abs(leftX - rightX), Math.abs(leftY - rightY));
    }

    private static int heading(int sourceX, int sourceY, int targetX, int targetY) {
        int dx = Integer.compare(targetX, sourceX);
        int dy = Integer.compare(targetY, sourceY);
        if (dx > 0 && dy > 0) return 3;
        if (dx < 0 && dy < 0) return 7;
        if (dx > 0 && dy == 0) return 2;
        if (dx < 0 && dy == 0) return 6;
        if (dx == 0 && dy > 0) return 4;
        if (dx < 0) return 5;
        if (dx > 0) return 1;
        return 0;
    }

    private Room required(long roomId) {
        Room room = rooms.get(roomId);
        if (room == null) throw new IllegalArgumentException("unknown room: " + roomId);
        return room;
    }

    private Channel requiredChannel(int channelId) {
        Channel channel = channels.get(channelId);
        if (channel == null) throw new IllegalArgumentException("unknown channel: " + channelId);
        return channel;
    }

    private Room ensureDefaultRoom(Channel channel) {
        if (!NativeRoomRules.enabled(this)) return null;
        if (channel.defaultRoomId != 0) return required(channel.defaultRoomId);
        long roomId = nextRoomId++;
        Room room = new Room(
                roomId, channel.id, defaultRoomSettings(), battleControls);
        rooms.put(roomId, room);
        channel.roomIds.add(roomId);
        channel.defaultRoomId = roomId;
        return room;
    }

    private ChannelSnapshot snapshot(Channel channel) {
        TournamentPresence.ChannelSnapshot shared = tournamentPresence.snapshot(channel.id);
        return new ChannelSnapshot(channel.id, channel.name, shared.users(), shared.capacity());
    }

    private void resetOrRemove(Room room) {
        if (!DurableBattleSettlement.cleanupAllowed(this, room.id)) return;
        if (!room.members.isEmpty() || room.disconnectResolutionPending) return;
        Channel channel = requiredChannel(room.channelId);
        if (room.id != channel.defaultRoomId) {
            rooms.remove(room.id);
            channel.roomIds.remove(room.id);
            return;
        }
        room.battleStarted = false;
        room.terminalEffectPending = false;
        room.ready.clear();
        room.botHp = NativeRoomRules.botHp(room, this);
        room.botArmBreakerStatus.clear();
        room.botJoyOfPainStatus.clear();
        room.botInventory.clear();
        room.groundEffects.clear();
        room.groundItems.clear();
        room.cookingFires.clear();
        room.cubeIgnitionEffects.clear();
        room.cubeQuakeEffects.clear();
        room.cubeShockEffects.clear();
        room.cubeHarmonyEffects.clear();
        room.patienceStatus.clear();
        room.shockStunEffects.clear();
        room.boneBreakEffects.clear();
        room.freezeEffects.clear();
        room.armorBreakExpiresAtNanos.clear();
        room.door.deactivate();
        room.siegeGate.deactivate();
        room.guardianTowers.deactivate();
        room.questEncounter.deactivate();
        room.questBoundary.deactivate();
        room.timeAttackEncounter.deactivate();
        room.petRace.reset();
        room.countdownValue = 0;
        room.countdownNextNanos = 0;
        room.forces.clear();
        room.battleRoster.clear();
        room.virtualPlayers.clear();
        room.battleVirtualPlayers.clear();
        room.nextVirtualPlayerOrdinal = 1;
        room.battleForces.clear();
        room.disconnectedBattleCombatants.clear();
        room.silenced = false;
        room.owner = null;
        room.resultPending = false;
        room.kickedParticipantIdentities.clear();
        room.settings = defaultRoomSettings();
        room.selectedMapId = BattleRules.decode(
                room.settings, battleControls).mapId();
        room.match.reconfigure(BattleRules.decode(
                room.settings, room.selectedMapId, battleControls));
    }

    /**
     * Native help defines a disconnected side as losing, except when both
     * sides disconnect together, which invalidates the match. Defer the
     * authoritative decision briefly so two adjacent TCP closes are observed
     * as one event; observers never enter this state machine.
     */
    synchronized boolean registerBattleDisconnect(long roomId, ClientSession session) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || session.isObserver()
                || room.terminalEffectPending
                || !room.battleRoster.contains(session)) return false;
        room.disconnectedBattleCombatants.add(session);
        if (!room.disconnectResolutionPending) {
            room.disconnectResolutionPending = true;
            long generation = room.battleGeneration;
            battleClock.schedule(() -> resolveBattleDisconnectSafely(roomId, generation),
                    BATTLE_DISCONNECT_GRACE_MS, TimeUnit.MILLISECONDS);
        }
        return true;
    }

    private void resolveBattleDisconnectSafely(long roomId, long generation) {
        try {
            BattleDisconnectWork work = battleDisconnectWork(roomId, generation);
            if (work == null) return;
            TeamDisconnectSettlement.freezeWork(work, this);
            if (work.resolution().ended()) {
                for (ClientSession recipient : work.recipients()) {
                    try {
                        recipient.serverEndBattle(work.resolution());
                    } catch (IOException error) {
                        System.out.println("[LiTo] disconnect result send failed recipient="
                                + recipient.playerName() + ": " + error.getMessage());
                    }
                }
            }
            if (!work.resolution().winners().isEmpty()
                    || !work.resolution().losers().isEmpty()) {
                for (ClientSession combatant : work.roster()) {
                    long objectId = combatant.playerObjectId();
                    if (work.resolution().winners().contains(objectId)) {
                        combatant.serverRecordBattleResult(true);
                    } else if (work.resolution().losers().contains(objectId)) {
                        combatant.serverRecordBattleDisconnect();
                    }
                }
            }
            if (work.roomDeleted()) {
                for (ClientSession recipient : work.roomDeleteRecipients()) {
                    try {
                        recipient.serverRoomDeleted(work.roomId());
                    } catch (IOException error) {
                        System.out.println("[LiTo] delayed room-delete send failed recipient="
                                + recipient.playerName() + ": " + error.getMessage());
                    }
                }
            }
        } catch (Throwable error) {
            TeamDisconnectSettlement.retryDisconnect(this, roomId, generation);
            System.out.println("[LiTo] disconnect resolver recovered room=" + roomId + ": "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private synchronized BattleDisconnectWork battleDisconnectWork(long roomId, long generation) {
        Room room = rooms.get(roomId);
        if (room == null || !room.battleStarted || !room.disconnectResolutionPending
                || room.battleGeneration != generation) return null;
        List<ClientSession> roster = new ArrayList<>(room.battleRoster);
        Set<Long> rosterIds = new LinkedHashSet<>();
        Set<Long> disconnectedIds = new LinkedHashSet<>();
        for (ClientSession combatant : roster) {
            long objectId = combatant.playerObjectId();
            rosterIds.add(objectId);
            if (room.disconnectedBattleCombatants.contains(combatant)) {
                disconnectedIds.add(objectId);
            }
        }
        if (room.match.rules().mode() == BattleRules.Mode.TIME_ATTACK
                && !disconnectedIds.containsAll(rosterIds)) {
            for (long disconnectedId : disconnectedIds) {
                room.match.timeAttackPlayerDisconnected(disconnectedId);
            }
            if (disconnectedIds.contains(room.match.timeAttackLeader())) {
                ClientSession successor = roster.stream()
                        .filter(candidate ->
                                !room.disconnectedBattleCombatants.contains(candidate))
                        .filter(room.members::contains)
                        .findFirst().orElse(null);
                if (successor == null
                        || !room.match.transferTimeAttackLeader(successor.playerObjectId())) {
                    throw new IllegalStateException(
                            "Time Attack leader disconnect has no surviving successor");
                }
            }
            room.battleRoster.removeAll(room.disconnectedBattleCombatants);
            room.disconnectedBattleCombatants.clear();
            room.disconnectResolutionPending = false;
            BattleRules.Resolution continuing = new BattleRules.Resolution(
                    false, false, Set.of(), disconnectedIds,
                    "timeattack-member-disconnected-continues");
            System.out.println("[LiTo] Time Attack disconnect continued room=" + roomId
                    + " disconnected=" + disconnectedIds
                    + " leader=" + room.match.timeAttackLeader());
            return new BattleDisconnectWork(roomId, List.of(), roster, continuing,
                    List.of(), false);
        }
        if (room.match.rules().mode() == BattleRules.Mode.PET_RACE
                && !disconnectedIds.containsAll(rosterIds)) {
            LinkedHashSet<Long> recordedWinners = new LinkedHashSet<>();
            LinkedHashSet<Long> recordedDisconnects = new LinkedHashSet<>();
            Long firstFinisher = room.petRace.firstFinisher();
            for (long disconnectedId : disconnectedIds) {
                room.match.petRacePlayerDisconnected(disconnectedId);
                if (firstFinisher != null && firstFinisher == disconnectedId) {
                    recordedWinners.add(disconnectedId);
                } else {
                    recordedDisconnects.add(disconnectedId);
                }
            }
            room.battleRoster.removeAll(room.disconnectedBattleCombatants);
            room.disconnectedBattleCombatants.clear();
            room.disconnectResolutionPending = false;
            BattleRules.Resolution continuing = new BattleRules.Resolution(
                    false, false, recordedWinners, recordedDisconnects,
                    "pet-race-runner-disconnected-continues");
            System.out.println("[LiTo] Pet Race disconnect continued room=" + roomId
                    + " disconnected=" + disconnectedIds
                    + " first-finisher=" + firstFinisher);
            return new BattleDisconnectWork(roomId, List.of(), roster, continuing,
                    List.of(), false);
        }
        BattleRules.Resolution resolution = TeamDisconnectSettlement.resolve(rosterIds, disconnectedIds, this, roomId);
        Object continuing = TeamDisconnectSettlement.continuingWork(this, roomId, roster, resolution, disconnectedIds);
        if (continuing != null) return (BattleDisconnectWork) continuing;
        List<ClientSession> recipients = new ArrayList<>(room.members);
        room.battleStarted = false;
        room.terminalEffectPending = false;
        // Surviving recipients still have to acknowledge the native result
        // screen.  A disconnected combatant must not make the room joinable
        // while those recipients remain in that result generation.
        room.resultPending = !room.members.isEmpty();
        room.disconnectResolutionPending = false;
        room.ready.clear();
        room.botHp = NativeRoomRules.botHp(room, this);
        room.botArmBreakerStatus.clear();
        room.botJoyOfPainStatus.clear();
        room.botInventory.clear();
        room.groundEffects.clear();
        room.groundItems.clear();
        room.cookingFires.clear();
        room.cubeIgnitionEffects.clear();
        room.cubeQuakeEffects.clear();
        room.cubeShockEffects.clear();
        room.cubeHarmonyEffects.clear();
        room.patienceStatus.clear();
        room.shockStunEffects.clear();
        room.boneBreakEffects.clear();
        room.freezeEffects.clear();
        room.armorBreakExpiresAtNanos.clear();
        room.door.deactivate();
        room.siegeGate.deactivate();
        room.guardianTowers.deactivate();
        room.questEncounter.deactivate();
        room.questBoundary.deactivate();
        room.timeAttackEncounter.deactivate();
        room.petRace.reset();
        room.countdownValue = 0;
        room.countdownNextNanos = 0;
        room.match.reset();

        Channel channel = requiredChannel(room.channelId);
        boolean deleted = room.members.isEmpty() && room.id != channel.defaultRoomId;
        List<ClientSession> deleteRecipients = deleted
                ? channelMembers(room.channelId) : List.of();
        if (deleted) {
            rooms.remove(room.id);
            channel.roomIds.remove(room.id);
        } else if (room.members.isEmpty()) {
            resetOrRemove(room);
        }
        System.out.println("[LiTo] battle disconnect resolved room=" + roomId
                + " roster=" + rosterIds + " disconnected=" + disconnectedIds
                + " reason=" + resolution.reason());
        return new BattleDisconnectWork(roomId, recipients, roster, resolution,
                deleteRecipients, deleted);
    }

    static BattleRules.Resolution resolveBattleDisconnect(Set<Long> roster,
                                                           Set<Long> disconnected) {
        LinkedHashSet<Long> players = new LinkedHashSet<>(roster);
        LinkedHashSet<Long> departed = new LinkedHashSet<>(disconnected);
        departed.retainAll(players);
        if (players.size() >= 2 && departed.containsAll(players)) {
            return new BattleRules.Resolution(true, false, Set.of(), Set.of(),
                    "all-combatants-disconnected-invalid");
        }
        LinkedHashSet<Long> winners = new LinkedHashSet<>(players);
        winners.removeAll(departed);
        LinkedHashSet<Long> losers = new LinkedHashSet<>(departed);
        return new BattleRules.Resolution(true, false, winners, losers,
                "combatant-disconnected");
    }

    private boolean cancelCountdownIfNotReady(Room room) {
        if (room.match.phase() != BattleRules.Phase.COUNTDOWN) return false;
        List<ClientSession> combatants = room.combatants();
        boolean allReady = nativeRosterReady(room, combatants)
                && !combatants.isEmpty() && room.ready.containsAll(combatants);
        if (!allReady) {
            room.match.cancelCountdown();
            room.countdownValue = 0;
            room.countdownNextNanos = 0;
            return true;
        }
        return false;
    }

    private static void transferOwner(Room room, ClientSession departed) {
        if (room.owner == departed) room.owner = room.members.stream().findFirst().orElse(null);
    }

    private static RoomSettings defaultRoomSettings() {
        // Native wire mode 1 is 死鬥模式. Keep the visible room selector and
        // the server's baseline deathmatch rules in agreement.
        // The native create dialog stores the unchecked "無法觀戰" control
        // inversely as ALLOW_OBSERVERS (0x08), so the ordinary default is
        // observable unless the room owner explicitly checks that option.
        return new RoomSettings("Local Lobby", "", 8, RoomSettings.ALLOW_OBSERVERS,
                1, 0, 0, 0, 0, List.of());
    }

    @Override public void close() {
        battleClock.shutdownNow();
        synchronized (this) {
            for (TournamentPresence.Owner owner : nativePresenceOwners.values()) tournamentPresence.disconnect(owner);
            nativePresenceOwners.clear(); nativePresencePeers.clear(); selectedCharacters.clear();
        }
    }

    private enum PlayerTradeState { PENDING, OPEN }

    private static final class PrivateShopSellOffer {
        final int index;
        final long objectId;
        final long unitPrice;
        long remaining;
        final InventoryCatalog.Definition definition;

        PrivateShopSellOffer(int index, long objectId, long unitPrice,
                             long remaining, InventoryCatalog.Definition definition) {
            this.index = index;
            this.objectId = objectId;
            this.unitPrice = unitPrice;
            this.remaining = remaining;
            this.definition = definition;
        }
    }

    private static final class PrivateShopBuyOffer {
        final int index;
        final long sampleObjectId;
        final long unitPrice;
        long remaining;
        final InventoryCatalog.Definition definition;

        PrivateShopBuyOffer(int index, long sampleObjectId, long unitPrice,
                            long remaining, InventoryCatalog.Definition definition) {
            this.index = index;
            this.sampleObjectId = sampleObjectId;
            this.unitPrice = unitPrice;
            this.remaining = remaining;
            this.definition = definition;
        }
    }

    private static final class PrivateShop {
        final long roomId;
        final ClientSession owner;
        long generation;
        final List<PrivateShopSellOffer> sellOffers;
        final List<PrivateShopBuyOffer> buyOffers;
        final byte[] announcement;

        PrivateShop(long roomId, ClientSession owner, long generation,
                    List<PrivateShopSellOffer> sellOffers,
                    List<PrivateShopBuyOffer> buyOffers, byte[] announcement) {
            this.roomId = roomId;
            this.owner = owner;
            this.generation = generation;
            this.sellOffers = List.copyOf(sellOffers);
            this.buyOffers = List.copyOf(buyOffers);
            this.announcement = announcement == null ? new byte[0] : announcement.clone();
        }

        PrivateShopSellOffer sellOffer(int index) {
            return sellOffers.stream().filter(offer -> offer.index == index)
                    .findFirst().orElse(null);
        }

        PrivateShopBuyOffer buyOffer(int index) {
            return buyOffers.stream().filter(offer -> offer.index == index)
                    .findFirst().orElse(null);
        }

        boolean exhausted() {
            return sellOffers.stream().allMatch(offer -> offer.remaining <= 0)
                    && buyOffers.stream().allMatch(offer -> offer.remaining <= 0);
        }
    }

    private record PrivateShopView(PrivateShop shop, int type, long generation,
                                   Map<Long, Integer> rowToOffer) {}

    private record TradeOffer(long objectId, long quantity,
                              RuntimeInventory.Item item) {}

    private static final class PlayerTrade {
        final long roomId;
        final ClientSession initiator;
        final ClientSession invitee;
        final long expiresAtNanos;
        final Map<ClientSession, List<TradeOffer>> offers = new LinkedHashMap<>();
        final Map<ClientSession, RuntimeInventory> display = new LinkedHashMap<>();
        final Set<ClientSession> confirmed = new LinkedHashSet<>();
        PlayerTradeState state = PlayerTradeState.PENDING;

        PlayerTrade(long roomId, ClientSession initiator, ClientSession invitee,
                    long expiresAtNanos) {
            this.roomId = roomId;
            this.initiator = initiator;
            this.invitee = invitee;
            this.expiresAtNanos = expiresAtNanos;
            offers.put(initiator, new ArrayList<>());
            offers.put(invitee, new ArrayList<>());
        }

        ClientSession other(ClientSession participant) {
            if (participant == initiator) return invitee;
            if (participant == invitee) return initiator;
            return null;
        }

        List<ClientSession> participants() {
            return List.of(initiator, invitee);
        }
    }

    private static final class ClassicParty {
        final String leaderKey;
        final Map<String, String> members = new LinkedHashMap<>();

        ClassicParty(String leaderName) {
            leaderKey = key(leaderName);
            members.put(leaderKey, identityDisplayName(leaderName));
        }
    }

    private static final class Channel {
        final int id;
        final String name;
        final int capacity;
        final Set<Long> roomIds = new LinkedHashSet<>();
        long defaultRoomId;

        Channel(int id, String name, int capacity) {
            this.id = id;
            this.name = name;
            this.capacity = capacity;
        }
    }

    private static final class Room {
        long botHasteDeadline, botHasteGeneration;
        final long id;
        final int channelId;
        final Set<ClientSession> members = new LinkedHashSet<>();
        final Set<ClientSession> ready = new LinkedHashSet<>();
        final Map<ClientSession, Integer> forces = new LinkedHashMap<>();
        final Set<ClientSession> battleRoster = new LinkedHashSet<>();
        final List<RoomVirtualPlayer> virtualPlayers = new ArrayList<>();
        final List<RoomVirtualPlayer> battleVirtualPlayers = new ArrayList<>();
        int nextVirtualPlayerOrdinal = 1;
        final Set<String> kickedParticipantIdentities = new LinkedHashSet<>();
        final Map<ClientSession, Integer> battleForces = new LinkedHashMap<>();
        final Set<ClientSession> disconnectedBattleCombatants = new LinkedHashSet<>();
        RuntimeInventory botInventory = new RuntimeInventory();
        final BattleGroundEffects groundEffects = new BattleGroundEffects();
        final BattleGroundItems groundItems = new BattleGroundItems();
        final BattleCookingFires cookingFires = new BattleCookingFires();
        final BattleCubeIgnitionEffects cubeIgnitionEffects =
                new BattleCubeIgnitionEffects();
        final BattleCubeQuakeEffects cubeQuakeEffects =
                new BattleCubeQuakeEffects();
        final BattleCubeShockEffects cubeShockEffects =
                new BattleCubeShockEffects();
        final BattleCubeHarmonyEffects cubeHarmonyEffects =
                new BattleCubeHarmonyEffects();
        final BattlePatienceStatus patienceStatus = new BattlePatienceStatus();
        final BattleShockStunEffects shockStunEffects = new BattleShockStunEffects();
        final BattleBoneBreakEffects boneBreakEffects =
                new BattleBoneBreakEffects();
        final BattleFreezeEffects freezeEffects = new BattleFreezeEffects();
        final Map<Long, Long> armorBreakExpiresAtNanos = new LinkedHashMap<>();
        final BattleDoor door = new BattleDoor();
        final BattleDoor siegeGate = new BattleDoor();
        final BattleGuardianTowers guardianTowers = new BattleGuardianTowers();
        final BattleQuestEncounter questEncounter = new BattleQuestEncounter();
        final BattleQuestBoundary questBoundary = new BattleQuestBoundary();
        final BattleTimeAttackEncounter timeAttackEncounter =
                new BattleTimeAttackEncounter();
        final BattlePetRaceCourseCatalog.State petRace =
                new BattlePetRaceCourseCatalog.State();
        RoomSettings settings;
        boolean battleStarted;
        boolean resultPending;
        boolean terminalEffectPending;
        boolean disconnectResolutionPending;
        long battleGeneration;
        long pveAiPublishAfterNanos = Long.MAX_VALUE;
        boolean silenced;
        int weather;
        int botHp = 0;
        final BattleArmBreakerStatus botArmBreakerStatus =
                new BattleArmBreakerStatus();
        final BattleJoyOfPainStatus botJoyOfPainStatus =
                new BattleJoyOfPainStatus();
        int botStep;
        int countdownValue;
        long countdownNextNanos;
        int selectedMapId;
        ClientSession owner;
        final BattleRules.MatchState match;

        Room(long id, int channelId, RoomSettings settings,
             BattleControlCatalog battleControls) {
            this.id = id;
            this.channelId = channelId;
            this.settings = settings;
            selectedMapId = BattleRules.decode(settings, battleControls).mapId();
            match = new BattleRules.MatchState(BattleRules.decode(
                    settings, selectedMapId, battleControls));
        }

        List<ClientSession> combatants() {
            return members.stream().filter(session -> !session.isObserver()).toList();
        }

        int combatantCount() {
            return combatants().size() + virtualPlayers.size();
        }
    }

    private static final class BuddyProfile {
        final Map<Integer, String> groups = new LinkedHashMap<>();
        final Map<String, BuddyEntry> entries = new LinkedHashMap<>();
        int nextGroupId = 2;

        BuddyProfile() {
            groups.put(1, "Friends");
        }

        BuddyProfile copy() {
            BuddyProfile result = new BuddyProfile();
            result.groups.clear();
            result.groups.putAll(groups);
            entries.forEach((name, entry) -> result.entries.put(name, entry.copy()));
            result.nextGroupId = nextGroupId;
            return result;
        }
    }

    private static final class BuddyEntry {
        final String name;
        int groupId;
        final int relationshipStyle;
        Protocol.LitoBuddyActivity activity;
        String origin;
        final int paletteOffset;
        final long buddyObjectId;
        String targetSource;
        boolean durable;

        BuddyEntry(String name, int groupId, int relationshipStyle,
                   Protocol.LitoBuddyActivity activity, String origin,
                   int paletteOffset, long buddyObjectId,
                   String targetSource, boolean durable) {
            this.name = name;
            this.groupId = groupId;
            this.relationshipStyle = relationshipStyle;
            this.activity = Objects.requireNonNull(activity, "buddy activity");
            this.origin = origin;
            this.paletteOffset = paletteOffset;
            this.buddyObjectId = buddyObjectId;
            this.targetSource = targetSource == null ? "" : targetSource;
            this.durable = durable;
        }

        BuddyEntry copy() {
            return new BuddyEntry(name, groupId, relationshipStyle, activity,
                    origin, paletteOffset, buddyObjectId, targetSource, durable);
        }
    }

    private static final class ClanProfile {
        final String key;
        final String name;
        final Map<String, ClanMember> members = new LinkedHashMap<>();
        boolean destroyPending;
        long emblemId;

        ClanProfile(String key, String name) {
            this.key = key;
            this.name = name;
        }
    }

    private static final class ClassicClanWar {
        final String clanOneKey;
        final String clanOneName;
        final String clanTwoKey;
        final String clanTwoName;

        ClassicClanWar(String clanOneKey, String clanOneName,
                       String clanTwoKey, String clanTwoName) {
            this.clanOneKey = clanOneKey;
            this.clanOneName = clanOneName;
            this.clanTwoKey = clanTwoKey;
            this.clanTwoName = clanTwoName;
        }
    }

    private static final class ClassicCastleSiege {
        final String source;
        final int castleId;
        final String defenderClan;
        final Set<String> attackingClans = new LinkedHashSet<>();

        ClassicCastleSiege(String source, int castleId, String defenderClan) {
            this.source = key(source);
            this.castleId = castleId;
            this.defenderClan = defenderClan;
        }
    }

    private static final class ClanEmblem {
        final String clanKey;
        final byte[] pixels;

        ClanEmblem(String clanKey, byte[] pixels) {
            this.clanKey = clanKey;
            this.pixels = pixels.clone();
        }
    }

    private static final class ClanWarehouseLease {
        final ClientSession session;
        final long expiresAtNanos;

        ClanWarehouseLease(ClientSession session, long expiresAtNanos) {
            this.session = session;
            this.expiresAtNanos = expiresAtNanos;
        }
    }

    private static final class ClanMember {
        final String identity;
        final String name;
        int role;
        int relationshipStyle;
        int paletteOffset;
        String title = "";
        Protocol.LitoClanActivity activity = Protocol.LitoClanActivity.OFFLINE;

        ClanMember(String identity, int role) {
            this.identity = identity;
            this.name = identityDisplayName(identity);
            this.role = role;
        }
    }
    synchronized boolean hasteArenaBot(long roomId,int casterX,int casterY,long now,int seconds) {
        Room room=rooms.get(roomId);
        var point=botPosition(roomId);
        if(room==null || !room.battleStarted || room.botHp<=0 || point==null
                || seconds<=0 || !BattleSpellCatalog.isInNativeScreen(casterX,casterY,point.x(),point.y())) return false;
        room.botHasteGeneration=room.battleGeneration;
        room.botHasteDeadline=now+seconds*1_000_000_000L;
        return true;
    }
    synchronized boolean arenaBotHasteActive(long roomId,long now) {
        Room room=rooms.get(roomId);
        return room!=null && room.battleStarted && room.botHp>0
            && room.botHasteGeneration==room.battleGeneration && room.botHasteDeadline>now;
    }
    synchronized boolean expireArenaBotHaste(long roomId,long now) {
        Room room=rooms.get(roomId);
        if(room==null || !room.battleStarted || room.botHasteDeadline==0) return false;
        if(room.botHasteGeneration!=room.battleGeneration || room.botHp<=0) {
            room.botHasteDeadline=0;return false;
        }
        if(room.botHasteDeadline>now) return false;
        room.botHasteDeadline=0;return true;
    }
}
