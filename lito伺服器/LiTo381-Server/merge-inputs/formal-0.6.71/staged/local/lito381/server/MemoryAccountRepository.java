package local.lito381.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class MemoryAccountRepository implements AccountRepository {
    private static final long DEFAULT_CASTLE_WAR_START_MILLIS =
            1_787_971_500_000L;
    private final String baseAccount;
    private final String authenticationFailureAccount;
    private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<>();
    private final Set<String> litoOwnedAccounts = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, AccountInfoSnapshot> accountInfo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, QuizRegistrationSnapshot> registeredQuiz = new ConcurrentHashMap<>();
    private final int sourceInventoryWeight;
    private final double sourceWeightRate;
    private final int sourceFood;
    private final int sourcePrimaryLevel;
    private final int sourcePrimaryMaxHp;
    private final int sourcePrimaryMaxMp;
    private final int sourcePrimaryArmorClass;
    private final int sourcePrimaryStrength;
    private final int sourcePrimaryConstitution;
    private final int sourcePrimaryDexterity;
    private final int sourcePrimaryCharisma;
    private final int sourcePrimaryIntelligence;
    private final int sourcePrimaryWisdom;
    private final int sourceSecondaryMaxMp;
    private final boolean sourceInventoryFixtureEnabled;
    private final boolean sourceProjectileFixtureEnabled;
    private final long sourceCastleWarStartMillis;
    private final int sourceCastleWarDurationMinutes;
    private final int sourceCastleWarIntervalMinutes;

    MemoryAccountRepository(String account, String password) {
        this(account, password, "", 0, 1.0);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount) {
        this(account, password, authenticationFailureAccount, 0, 1.0);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, 40, 52);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood, 52);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, 110, false, false);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourceSecondaryMaxMp) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, sourceSecondaryMaxMp, false, false);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, false);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, 50, sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourcePrimaryMaxHp, int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, sourcePrimaryMaxHp, sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled,
                -1, 60, 10_080);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourcePrimaryMaxHp, int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled,
                            int castleWarFixtureStartDelaySeconds,
                            int castleWarFixtureDurationMinutes,
                            int castleWarFixtureIntervalMinutes) {
        this(account, password, authenticationFailureAccount,
                sourceInventoryWeight, sourceWeightRate, sourceFood,
                sourcePrimaryLevel, sourcePrimaryMaxHp,
                100, 10, 13, 10, 10, 9, 8, 11,
                sourceSecondaryMaxMp,
                sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled,
                castleWarFixtureStartDelaySeconds,
                castleWarFixtureDurationMinutes, castleWarFixtureIntervalMinutes);
    }

    MemoryAccountRepository(String account, String password,
                            String authenticationFailureAccount,
                            int sourceInventoryWeight, double sourceWeightRate,
                            int sourceFood, int sourcePrimaryLevel,
                            int sourcePrimaryMaxHp, int sourcePrimaryMaxMp,
                            int sourcePrimaryArmorClass,
                            int sourcePrimaryStrength, int sourcePrimaryConstitution,
                            int sourcePrimaryDexterity, int sourcePrimaryCharisma,
                            int sourcePrimaryIntelligence, int sourcePrimaryWisdom,
                            int sourceSecondaryMaxMp,
                            boolean sourceInventoryFixtureEnabled,
                            boolean sourceProjectileFixtureEnabled,
                            int castleWarFixtureStartDelaySeconds,
                            int castleWarFixtureDurationMinutes,
                            int castleWarFixtureIntervalMinutes) {
        this.baseAccount = account;
        this.authenticationFailureAccount = key(authenticationFailureAccount);
        if (sourceInventoryWeight < 0 || !Double.isFinite(sourceWeightRate)
                || sourceWeightRate <= 0.0 || sourceFood < 0 || sourceFood > 255
                || sourcePrimaryLevel < 1
                || sourcePrimaryLevel > NumericBreakthrough.MAX_LEVEL
                || sourcePrimaryMaxHp < 1
                || sourcePrimaryMaxHp > NumericBreakthrough.MAX_VITAL
                || sourcePrimaryMaxMp < 1
                || sourcePrimaryMaxMp > NumericBreakthrough.MAX_VITAL
                || sourcePrimaryArmorClass < NumericBreakthrough.MIN_ARMOR_CLASS
                || sourcePrimaryArmorClass > NumericBreakthrough.MAX_ARMOR_CLASS
                || sourcePrimaryStrength < 0
                || sourcePrimaryStrength > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryConstitution < 0
                || sourcePrimaryConstitution > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryDexterity < 0
                || sourcePrimaryDexterity > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryCharisma < 0
                || sourcePrimaryCharisma > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryIntelligence < 0
                || sourcePrimaryIntelligence > NumericBreakthrough.MAX_ABILITY
                || sourcePrimaryWisdom < 0
                || sourcePrimaryWisdom > NumericBreakthrough.MAX_ABILITY
                || sourceSecondaryMaxMp < 1
                || sourceSecondaryMaxMp > NumericBreakthrough.MAX_VITAL
                || castleWarFixtureStartDelaySeconds < -1
                || castleWarFixtureStartDelaySeconds > 3_600
                || castleWarFixtureDurationMinutes < 1
                || castleWarFixtureIntervalMinutes <= castleWarFixtureDurationMinutes) {
            throw new IllegalArgumentException("invalid memory inventory-weight fixture");
        }
        this.sourceInventoryWeight = sourceInventoryWeight;
        this.sourceWeightRate = sourceWeightRate;
        this.sourceFood = sourceFood;
        this.sourcePrimaryLevel = sourcePrimaryLevel;
        this.sourcePrimaryMaxHp = sourcePrimaryMaxHp;
        this.sourcePrimaryMaxMp = sourcePrimaryMaxMp;
        this.sourcePrimaryArmorClass = sourcePrimaryArmorClass;
        this.sourcePrimaryStrength = sourcePrimaryStrength;
        this.sourcePrimaryConstitution = sourcePrimaryConstitution;
        this.sourcePrimaryDexterity = sourcePrimaryDexterity;
        this.sourcePrimaryCharisma = sourcePrimaryCharisma;
        this.sourcePrimaryIntelligence = sourcePrimaryIntelligence;
        this.sourcePrimaryWisdom = sourcePrimaryWisdom;
        this.sourceSecondaryMaxMp = sourceSecondaryMaxMp;
        this.sourceInventoryFixtureEnabled = sourceInventoryFixtureEnabled;
        this.sourceProjectileFixtureEnabled = sourceProjectileFixtureEnabled;
        this.sourceCastleWarStartMillis = castleWarFixtureStartDelaySeconds < 0
                ? DEFAULT_CASTLE_WAR_START_MILLIS
                : Math.addExact(System.currentTimeMillis(),
                        Math.multiplyExact(
                                (long) castleWarFixtureStartDelaySeconds, 1_000L));
        this.sourceCastleWarDurationMinutes = castleWarFixtureDurationMinutes;
        this.sourceCastleWarIntervalMinutes = castleWarFixtureIntervalMinutes;
        credentials.put(key(account), password);
        credentials.put(key(account + "2"), password);
        credentials.put(key(account + "3"), password);
        credentials.put(key(account + "4"), password);
        credentials.put(key(account + "5"), password);
        credentials.put(key(account + "6"), password);
        credentials.put(key(account + "7"), password);
        credentials.put(key(account + "8"), password);
        credentials.put(key(account + "9"), password);
        credentials.put(key(account + "10"), password);
        credentials.put(key(account + "11"), password);
        credentials.put(key(account + "12"), password);
        credentials.put(key(account + "13"), password);
        credentials.put(key(account + "14"), password);
        credentials.put(key(account + "15"), password);
        credentials.put(key(account + "16"), password);
        credentials.put(key(account + "17"), password);
        credentials.put(key(account + "18"), password);
        credentials.put(key(account + "19"), password);
        credentials.put(key(account + "20"), password);
        credentials.put(key(account + "21"), password);
        credentials.put(key(account + "22"), password);
        credentials.put(key(account + "23"), password);
        credentials.put(key(account + "24"), password);
        credentials.put(key(account + "25"), password);
        credentials.put(key(account + "26"), password);
    }

    @Override public void healthCheck() {}

    @Override public GmCommandDefinition gmCommand(String name) {
        return "de".equalsIgnoreCase(name == null ? "" : name.trim())
                ? new GmCommandDefinition("de", 200, "L1SpawnDe",
                    "loopback equivalent of lito_static.commands", false)
                : null;
    }



    @Override public List<RepositoryTournamentShopOffer> tournamentShopOffers() {
        return TournamentShopCatalog.defaultSourceRows();
    }

    @Override public AuthResult authenticate(String candidate, String suppliedPassword)
            throws SQLException {
        if (!authenticationFailureAccount.isEmpty()
                && authenticationFailureAccount.equals(key(candidate))) {
            throw new SQLException("configured memory authentication failure fixture");
        }
        String expected = credentials.get(key(candidate));
        boolean ok = expected != null && expected.equals(suppliedPassword);
        return ok ? new AuthResult(true, false, candidate) : AuthResult.rejected(candidate);
    }

    @Override public boolean accountExists(String account) {
        return credentials.containsKey(key(account));
    }

    @Override public boolean deleteLitoAccount(String account) {
        String normalized = key(account);
        return litoOwnedAccounts.remove(normalized)
                && credentials.remove(normalized) != null;
    }

    @Override public AccountMutationResult createAccount(String account, String password) {
        String normalized = key(account);
        if (credentials.putIfAbsent(normalized, password) != null) {
            return AccountMutationResult.EXISTS;
        }
        litoOwnedAccounts.add(normalized);
        return AccountMutationResult.SUCCESS;
    }

    @Override public AccountMutationResult changePassword(
            String account, String oldPassword, String newPassword) {
        String normalized = key(account);
        String current = credentials.get(normalized);
        if (current == null) return AccountMutationResult.NOT_FOUND;
        if (!current.equals(oldPassword)) return AccountMutationResult.WRONG_PASSWORD;
        return credentials.replace(normalized, oldPassword, newPassword)
                ? AccountMutationResult.SUCCESS : AccountMutationResult.WRONG_PASSWORD;
    }

    @Override public synchronized AccountMutationResult registerQuiz(
            String account, String oldPassword, String newPassword, List<String> quizFields) {
        String normalized = key(account);
        String current = credentials.get(normalized);
        if (current == null) return AccountMutationResult.NOT_FOUND;
        if (!current.equals(oldPassword)) return AccountMutationResult.WRONG_PASSWORD;
        credentials.put(normalized, newPassword);
        registeredQuiz.put(normalized, new QuizRegistrationSnapshot(quizFields));
        return AccountMutationResult.SUCCESS;
    }

    QuizRegistrationSnapshot registeredQuizForTest(String account) {
        return registeredQuiz.get(key(account));
    }

    @Override public AccountMutationResult changeAccountInfo(
            String account, int type, List<String> fields) {
        String normalized = key(account);
        if (!credentials.containsKey(normalized)) return AccountMutationResult.NOT_FOUND;
        accountInfo.put(normalized, new AccountInfoSnapshot(type, fields));
        return AccountMutationResult.SUCCESS;
    }

    AccountInfoSnapshot accountInfoForTest(String account) {
        return accountInfo.get(key(account));
    }

    @Override public List<CharacterSummary> characters(String requestedAccount) {
        boolean basePlayer = baseAccount.equalsIgnoreCase(requestedAccount);
        boolean secondPlayer = (baseAccount + "2").equalsIgnoreCase(requestedAccount);
        boolean observer = (baseAccount + "3").equalsIgnoreCase(requestedAccount);
        boolean emblemPlayer = (baseAccount + "4").equalsIgnoreCase(requestedAccount);
        boolean elfPlayer = (baseAccount + "5").equalsIgnoreCase(requestedAccount);
        boolean clanMate = (baseAccount + "6").equalsIgnoreCase(requestedAccount);
        boolean kentOwner = (baseAccount + "7").equalsIgnoreCase(requestedAccount);
        boolean femalePlayer = (baseAccount + "8").equalsIgnoreCase(requestedAccount);
        boolean giranOwner = (baseAccount + "9").equalsIgnoreCase(requestedAccount);
        boolean giranMate = (baseAccount + "10").equalsIgnoreCase(requestedAccount);
        boolean physicalPlayer = (baseAccount + "11").equalsIgnoreCase(requestedAccount);
        boolean defensePlayer = (baseAccount + "12").equalsIgnoreCase(requestedAccount);
        boolean clawPlayer = (baseAccount + "13").equalsIgnoreCase(requestedAccount);
        boolean edoryuPlayer = (baseAccount + "14").equalsIgnoreCase(requestedAccount);
        boolean weaponDummy = (baseAccount + "15").equalsIgnoreCase(requestedAccount);
        boolean kiringkuPlayer = (baseAccount + "16").equalsIgnoreCase(requestedAccount);
        boolean chainswordPlayer = (baseAccount + "17").equalsIgnoreCase(requestedAccount);
        boolean bowPlayer = (baseAccount + "18").equalsIgnoreCase(requestedAccount);
        boolean counterBarrierPlayer =
                (baseAccount + "19").equalsIgnoreCase(requestedAccount);
        boolean windawoodOwner = (baseAccount + "20").equalsIgnoreCase(requestedAccount);
        boolean windawoodMate = (baseAccount + "21").equalsIgnoreCase(requestedAccount);
        boolean dwarfOwner = (baseAccount + "22").equalsIgnoreCase(requestedAccount);
        boolean dwarfMate = (baseAccount + "23").equalsIgnoreCase(requestedAccount);
        boolean adenOwner = (baseAccount + "24").equalsIgnoreCase(requestedAccount);
        boolean adenMate = (baseAccount + "25").equalsIgnoreCase(requestedAccount);
        boolean siegeOwner = (baseAccount + "26").equalsIgnoreCase(requestedAccount);
        if (!basePlayer && !secondPlayer && !observer && !emblemPlayer
                && !elfPlayer && !clanMate && !kentOwner && !femalePlayer
                && !giranOwner && !giranMate && !physicalPlayer && !defensePlayer
                && !clawPlayer && !edoryuPlayer && !weaponDummy
                && !kiringkuPlayer && !chainswordPlayer && !bowPlayer
                && !counterBarrierPlayer && !windawoodOwner
                && !windawoodMate && !dwarfOwner && !dwarfMate
                && !adenOwner && !adenMate && !siegeOwner) return List.of();
        return List.of(new CharacterSummary(
                siegeOwner ? "TestSiegeLord"
                        : adenMate ? "TestAdenMate"
                        : adenOwner ? "TestAdenLord"
                        : dwarfMate ? "TestDwarfMate"
                        : dwarfOwner ? "TestDwarfLord"
                        : windawoodMate ? "TestWindawoodMate"
                        : windawoodOwner ? "TestWindawoodLord"
                        : counterBarrierPlayer ? "TestCounterBarrier"
                        : bowPlayer ? "TestBow" : chainswordPlayer ? "TestChainsword"
                        : kiringkuPlayer ? "TestKiringku" : weaponDummy ? "TestWeaponDummy"
                        : edoryuPlayer ? "TestEdoryu"
                        : clawPlayer ? "TestClaw" : defensePlayer ? "TestDefense"
                        : physicalPlayer ? "TestPhysical" : giranMate ? "TestGiranMate"
                        : giranOwner ? "TestGiranLord" : femalePlayer ? "TestPrincess"
                        : kentOwner ? "TestKentLord"
                        : clanMate ? "TestClanMate" : elfPlayer ? "TestElf" : emblemPlayer ? "TestEmblem"
                        : observer ? "TestObserver" : secondPlayer ? "TestMage" : "TestKnight",
                siegeOwner ? "LegacySiege"
                        : adenOwner || adenMate ? "LegacyAden"
                        : dwarfOwner || dwarfMate ? "LegacyDwarf"
                        : windawoodOwner || windawoodMate ? "LegacyWindawood"
                        : giranOwner || giranMate ? "LegacyGiran" : kentOwner ? "LegacyKent"
                        : emblemPlayer || clanMate ? "LegacyEmblem" : "",
                kiringkuPlayer ? 6 : chainswordPlayer ? 5 : bowPlayer ? 2
                        : counterBarrierPlayer ? 1
                        : clawPlayer || edoryuPlayer ? 4 : elfPlayer ? 2
                        : secondPlayer || observer || clanMate || giranMate || windawoodMate
                        || dwarfMate || adenMate
                        || physicalPlayer || defensePlayer || weaponDummy ? 1 : 0,
                femalePlayer ? 1 : 0,
                observer ? 15 : secondPlayer ? 47 : elfPlayer ? 45
                        : clanMate || giranMate || windawoodMate || dwarfMate ? 40
                        : kentOwner || windawoodOwner || dwarfOwner || siegeOwner ? 55
                        : sourcePrimaryLevel,
                weaponDummy || counterBarrierPlayer ? 2000
                        : secondPlayer ? 100 : sourcePrimaryMaxHp,
                observer ? 10 : secondPlayer ? sourceSecondaryMaxMp
                        : basePlayer ? sourcePrimaryMaxMp : 100,
                defensePlayer ? -10 : elfPlayer ? 100
                        : basePlayer ? sourcePrimaryArmorClass : 10,
                basePlayer ? sourcePrimaryStrength : 13,
                basePlayer ? sourcePrimaryConstitution : 10,
                basePlayer ? sourcePrimaryDexterity : 10,
                basePlayer ? sourcePrimaryCharisma : siegeOwner ? 18
                        : adenOwner || adenMate ? 18
                        : dwarfOwner || dwarfMate ? 18
                        : windawoodOwner || windawoodMate ? 18
                        : giranOwner || giranMate ? 18 : 9,
                basePlayer ? sourcePrimaryIntelligence : kiringkuPlayer ? 13 : 8,
                basePlayer ? sourcePrimaryWisdom : 11,
                kiringkuPlayer ? 13 : 8, 11,
                siegeOwner ? 27
                        : adenMate ? 26 : adenOwner ? 25
                        : dwarfMate ? 24 : dwarfOwner ? 23
                        : windawoodMate ? 22 : windawoodOwner ? 21
                        : counterBarrierPlayer ? 20
                        : bowPlayer ? 19 : chainswordPlayer ? 18 : kiringkuPlayer ? 17
                        : weaponDummy ? 16 : edoryuPlayer ? 15 : clawPlayer ? 14
                        : defensePlayer ? 13 : physicalPlayer ? 12 : giranMate ? 11
                        : giranOwner ? 10 : femalePlayer ? 9 : kentOwner ? 8
                        : clanMate ? 7 : elfPlayer ? 6 : emblemPlayer ? 5
                        : observer ? 4 : secondPlayer ? 3 : 1,
                0, basePlayer ? 1234 : secondPlayer ? -1234 : 0,
                basePlayer ? 7 : 0, 0, 32600, 32700, 4,
                10, sourceFood, BattleNaturalHpRegen.fallbackConHprMaximum(10),
                13, 10, elfPlayer ? BattleSpellCatalog.ATTRIBUTE_EARTH : 0));
    }

    @Override public Set<Integer> learnedSkills(long characterObjectId) {
        if (characterObjectId == 1) {
            // Skill 131 is a loopback-only bridge so the source Knight fixture
            // can prove Light in ordinary WORLD without modifying either
            // retained character_skills database.
            return Set.of(1, 2, 4, 6, 7, 11, 15, 16, 17, 18, 25, 34, 35,
                    44, 58, 63, 77, 78, BattleSpellCatalog.WORLD_TREE_CALL);
        }
        if (characterObjectId == 3) {
            // Wind Cutter is a loopback-only cross-player fixture used to
            // prove that awakening elemental/MR modifiers enter the same
            // retained ordinary-magic combat calculation.  No source DB row
            // is written or inferred from this memory-only account. HEAL_ALL
            // is likewise a loopback-only bridge so the already learned
            // Absolute Barrier and skill49 can prove their commit/cleanup
            // ordering without mutating either retained character-skills DB.
            return Set.of(1, 3, 4, BattleSpellCatalog.WIND_CUTTER,
                    BattleSpellCatalog.HEAL_ALL,
                    BattleSpellCatalog.HOLY_WEAPON,
                    BattleSpellCatalog.CURE_POISON,
                    BattleSpellCatalog.CHILL_TOUCH,
                    BattleSpellCatalog.VAMPIRIC_TOUCH,
                    BattleSpellCatalog.SLOW,
                    BattleSpellCatalog.EARTH_JAIL,
                    BattleSpellCatalog.FROZEN_CLOUD,
                    BattleSpellCatalog.REVEAL_WEAKNESS,
                    BattleSpellCatalog.DETECTION,
                    14, 19, 20, 26, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
                    42, 43, 44, 45, 46, 47, 48, 51, 52, 53, 54, 56, 57, 59, 60, 61, 62, 64,
                    65, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
                    131, BattleSpellCatalog.EARTH_BIND, 206);
        }
        // Loopback-only royal source-clan fixture for native skills 113-118.
        // TestEmblem is character type 0; keep all royal spells off TestElf.
        if (characterObjectId == 5) return Set.of(
                BattleSpellCatalog.TRUE_TARGET, BattleSpellCatalog.GLOWING_AURA,
                BattleSpellCatalog.SHINING_AURA, BattleSpellCatalog.CALL_CLAN,
                BattleSpellCatalog.BRAVE_AURA, BattleSpellCatalog.RUN_CLAN,
                131);
        // Loopback-only source-clan fixture used to prove HEAL_ALL's exact
        // clan/pet target_to=20 behavior and the native WORLD pledge warehouse
        // path without changing the legacy DB. Skill 131 is the already-proven
        // selector-only bridge from Tournament into the imported classic world.
        if (characterObjectId == 7) return Set.of(49, 55, 69, 131);
        // Loopback-only melee carrier for Brave Aura's ordinary-WORLD damage
        // acceptance. Skill131 is the already-qualified bridge into the
        // imported classic world; no retained character_skills row changes.
        if (characterObjectId == 12) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL);
        // Loopback-only Kent owner carrier for the selected map15/240-243
        // component. Skill 131 is the already-qualified WORLD bridge.
        if (characterObjectId == 8) return Set.of(131);
        // Loopback-only female crown carrier for the native Zero -> Gunter
        // story acceptance. Skill 131 is only the already-qualified WORLD
        // bridge; production characters still come from the read-only source.
        if (characterObjectId == 9) return Set.of(131);
        // Loopback-only Giran owner/member carriers use the already-qualified
        // World Tree bridge to reach the retained map52 steward component.
        if (characterObjectId == 10) return Set.of(
                BattleSpellCatalog.BLESSED_ARMOR,
                BattleSpellCatalog.WORLD_TREE_CALL);
        if (characterObjectId == 11) return Set.of(131);
        // Loopback-only Windawood owner/member carriers for map23/24/29.
        if (characterObjectId == 21 || characterObjectId == 22) return Set.of(131);
        // Loopback-only Dwarf Castle owner/member carriers for map66.
        if (characterObjectId == 23 || characterObjectId == 24) return Set.of(131);
        // Loopback-only Aden Castle owner/member carriers for map300.
        if (characterObjectId == 25 || characterObjectId == 26) return Set.of(131);
        // Loopback-only unlanded royal/clan leader for the retained castle-war
        // declaration -> tower -> crown -> ownership integration path.
        if (characterObjectId == 27) return Set.of(131);
        if (characterObjectId == 6) return Set.of(
                129, 130, 131, BattleSpellCatalog.ELEMENTAL_FALL_DOWN,
                BattleSpellCatalog.COUNTER_MIRROR,
                BattleSpellCatalog.CLEAR_MIND,
                BattleSpellCatalog.RESIST_ELEMENTAL,
                BattleSpellCatalog.BLOODY_SOUL,
                BattleSpellCatalog.LESSER_ELEMENTAL,
                BattleSpellCatalog.GREATER_ELEMENTAL,
                 BattleSpellCatalog.BURNING_WEAPON,
                 BattleSpellCatalog.NATURES_BLESSING,
                 BattleSpellCatalog.WATER_LIFE,
                 157, 169, 176);
        // Loopback-only bow Elf fixture. Skill131 is the already-qualified
        // WORLD bridge; skill132 is the selected/retained Triple Arrow row
        // paired with this fixture's qualified bow and source-arrow ledger.
        // No retained character_skills row or client resource is modified.
        if (characterObjectId == 19) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL,
                BattleSpellCatalog.TRIPLE_ARROW);
        // Loopback-only Dragon Knight/chainsword carrier for qualified native
        // Dragon Knight skills181..194.
        // Skill131 is the already-qualified bridge into ordinary WORLD; the
        // selected source databases and client remain read-only.
        if (characterObjectId == 18) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL,
                BattleSpellCatalog.DRAGON_SKIN,
                BattleSpellCatalog.BURNING_SLASH,
                BattleSpellCatalog.GUARD_BRAKE,
                BattleSpellCatalog.MAGMA_BREATH,
                BattleSpellCatalog.AWAKEN_ANTHARAS,
                BattleSpellCatalog.BLOODLUST,
                BattleSpellCatalog.FOE_SLAYER,
                BattleSpellCatalog.RESIST_FEAR,
                 BattleSpellCatalog.SHOCK_SKIN,
                 BattleSpellCatalog.AWAKEN_FAFURION,
                 BattleSpellCatalog.MORTAL_BODY,
                 BattleSpellCatalog.THUNDER_GRAB,
                 BattleSpellCatalog.HORROR_OF_DEATH,
                 BattleSpellCatalog.FREEZING_BREATH,
                 BattleSpellCatalog.AWAKEN_VALAKAS);
        // Loopback-only Illusionist/kiringku carrier for the retained
        // selector-only skills202/207/208/213 in BATTLE and ordinary WORLD.
        // The selected source databases and client remain read-only.
        if (characterObjectId == 17) return Set.of(
                BattleSpellCatalog.WORLD_TREE_CALL,
                BattleSpellCatalog.CONFUSION,
                BattleSpellCatalog.MIND_BREAK,
                BattleSpellCatalog.BONE_BREAK,
                BattleSpellCatalog.ARM_BREAKER);
        // Loopback-only Edoryu fixture proving LiTo's native weapon/action
        // gates in BATTLE and ordinary WORLD. Skills 8 and 12 exercise the
        // retained character-vs-item weapon-enchant replacement group;
        // skill131 is only the already-qualified Tournament -> retained-world
        // bridge. Neither selected source database is modified.
        if (characterObjectId == 15) return Set.of(
                BattleSpellCatalog.HOLY_WEAPON,
                BattleSpellCatalog.ENCHANT_WEAPON,
                BattleSpellCatalog.SHOCK_STUN,
                BattleSpellCatalog.WORLD_TREE_CALL);
        // Loopback-only target fixture for authoritative skill-72 TCP
        // evidence. The selected legacy DB remains read-only and unchanged.
        if (characterObjectId == 16) return Set.of(27, 31, 44, 60, 131,
                BattleSpellCatalog.REDUCTION_ARMOR,
                BattleSpellCatalog.BOUNCE_ATTACK,
                BattleSpellCatalog.SOLID_CARRIAGE);
        // Loopback-only two-hand-sword fixture for the selected-client
        // Counter Barrier gate and retained reflection pipeline.
        if (characterObjectId == 20) {
            return Set.of(44, BattleSpellCatalog.COUNTER_BARRIER,
                    BattleSpellCatalog.WORLD_TREE_CALL);
        }
        return Set.of();
    }

    @Override public Map<Integer, Integer> questSteps(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        // Loopback-only knight carrier. This is the exact prerequisite needed
        // to enter the selected 10702 Gunter chain; no retained database row is
        // created or changed by this fixture.
        return characterObjectId == 3 ? Map.of(10701, 1) : Map.of();
    }

    @Override public List<SourceInventoryItem> sourceInventory(long characterObjectId) {
        if (sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25)) {
            // Exact selected 380_db armor row 20089 (type=armor/type2=2).
            // This isolated carrier makes the retained NPC skill21 item-local
            // AC lease observable without writing either original database.
            ClassicWorldDropCatalog.ItemWire armor =
                    new ClassicWorldDropCatalog.ItemWire(
                            20089, "armor", "$116", "armor",
                            51, 3830, 0, false, true, 70_000, 0,
                            "", 0, 0, "leather", 4, 0x7F);
            InventoryCatalog.NativeArmorStatus status =
                    new InventoryCatalog.NativeArmorStatus(
                            2, ClassicWorldDropCatalog.materialCode("leather"),
                            70_000, 0x7F, 4,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2);
            return List.of(new SourceInventoryItem(
                    0x0F00_2008L, 20089, 1, true, 0, 0,
                    true, RuntimeInventory.BLESSING_BLESSED,
                    false, armor, status));
        }
        if (sourceInventoryFixtureEnabled && sourceProjectileFixtureEnabled
                && characterObjectId == 1) {
            // Combined loopback-only fixture for format-30 warehouse/player
            // transfer acceptance. Existing one-flag source fixtures remain
            // byte-for-byte unchanged.
            ClassicWorldDropCatalog.ItemWire token =
                    new ClassicWorldDropCatalog.ItemWire(
                            65_001, "etc", "Source Trade Token", "none",
                            321, 322, 1, true, true, 1_250, 87,
                            "", 0, 0, "plant", -1, 0);
            return List.of(new SourceInventoryItem(
                    2001L, 65_001, 3, false, 0, 0,
                    true, RuntimeInventory.BLESSING_NORMAL, false, token));
        }
        if (sourceInventoryFixtureEnabled && !sourceProjectileFixtureEnabled
                && characterObjectId == 3) {
            ClassicWorldDropCatalog.ItemWire claw =
                    new ClassicWorldDropCatalog.ItemWire(
                            40590, "etc", "$2023", "none", 139, 3963, 0,
                            false, false, 1000, 0, "", 0, 0, "bone", -1, 0);
            return List.of(new SourceInventoryItem(
                    0x0F00_4059L, 40590, 1, false, 0, 0,
                    true, RuntimeInventory.BLESSING_BLESSED, false, claw));
        }
        if (sourceProjectileFixtureEnabled && characterObjectId == 19) {
            ClassicWorldDropCatalog.ItemWire bow =
                    new ClassicWorldDropCatalog.ItemWire(
                            172, "weapon", "$24", "weapon", 278, 12, 0,
                            false, true, 30_000, 0, "bow", 2, 2,
                            "wood", 6, 0x4F);
            ClassicWorldDropCatalog.ItemWire arrow =
                    new ClassicWorldDropCatalog.ItemWire(
                            40743, "etc", "$61", "arrow", 7, 66, 0,
                            true, true, 30, 0, "", 0, 0,
                            "wood", -1, 0);
            return List.of(
                    new SourceInventoryItem(
                            1900L, 172, 1, true, 0, 0,
                            true, RuntimeInventory.BLESSING_BLESSED, false, bow),
                    new SourceInventoryItem(
                            1901L, 40743, 100, false, 0, 0,
                            true, RuntimeInventory.BLESSING_BLESSED, false, arrow));
        }
        return List.of();
    }

    @Override public SourceEquippedFireResistance equippedFireResistance(
            long characterObjectId) {
        // Loopback-only evidence fixture: TestMage carries one exact 20304
        // Fire Spirit Ring (+30). Other memory characters have no source gear.
        return characterObjectId == 3
                ? new SourceEquippedFireResistance(30, 1)
                : SourceEquippedFireResistance.none();
    }

    @Override public SourceBaseMagicResistance baseMagicResistance(long characterObjectId) {
        CharacterSummary character = charactersForObjectId(characterObjectId);
        if (character == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        return SourceBaseMagicResistance.from(character);
    }

    @Override public SourceEquipmentMagicStats equipmentMagicStats(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        if (characterObjectId == 12) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    24, 1, 6, 8, 1, 0, 0, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(11, 1, 0, 0, 0))
                    .withAbility(1, 2, 0, 0);
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    4, 5, 0, 0,
                    6, 7, 0, 0,
                    8, 9, 0, 0,
                    0, 0, 0, 0,
                    4, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 3, 1);
        }
        if (sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25)) {
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            SourceWeaponPhysicalStats.none(),
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            -2, 0);
            return new SourceEquipmentMagicStats(physical, 1, 1);
        }
        if (characterObjectId == 13) {
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    SourceWeaponPhysicalStats.none(),
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 3, 5);
            return new SourceEquipmentMagicStats(physical, 15, 0, 1, 1);
        }
        if (characterObjectId == 14 || characterObjectId == 15) {
            boolean claw = characterObjectId == 14;
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    claw ? 152 : 69, claw ? 11 : 12,
                    8, claw ? 7 : 6, 1, 2, 0, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            11, 0, 0, 0, 0,
                            claw ? 0 : 10, claw ? 0.0D : 1.2D,
                            claw ? 33 : 0, false, false));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0);
            return characterObjectId == 15
                    ? new SourceEquipmentMagicStats(physical, 1, 0, 100)
                    : new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 17) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    270, 17, 16, 16, 1, 1, 2, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            13, 0, 0, 0, 0,
                            0, 0.0D, 0, false, false, 3018));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 27) {
            // Loopback-only castle-objective carrier.  This is the same exact
            // selected 380_db item 52 contract used by the counter-barrier
            // fixture; it keeps gate/tower verification on L1AttackPc damage
            // instead of reducing native 10,000 HP objectives.
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    52, 3, 17, 21, 1, 0, 0, 0, 0)
                    .withNameId("$3");
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            weapon,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 18) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    272, 18, 16, 12, 2, 2, 5, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            13, 0, 0, 0, 0,
                            0, 0.0D, 0, false, false, 3033));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    weapon,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 19) {
            // Loopback-only bow fixture proving the retained bow-only ER
            // second roll and Solid Carriage's +15 contribution.  It never
            // mutates either retained database or the selected client.
            SourceProjectilePhysicalStats arrow =
                    new SourceProjectilePhysicalStats(
                            1901L, 40743, -2, 5, 7, 66, true, 100);
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    172, 4, 6, 6, 15, 0, 0, 0, 0)
                    .withCombatTraits(new SourceWeaponCombatTraits(
                            8, 1, 0, 0, 0))
                    .withProjectile(arrow);
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            weapon,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 20) {
            // Exact selected 380_db item 52: two-hand sword, native weapon
            // type 3 / walk action 50, large damage 21 and name-id $3.
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(
                    52, 3, 17, 21, 1, 0, 0, 0, 0)
                    .withNameId("$3");
            SourceEquipmentPhysicalStats physical =
                    new SourceEquipmentPhysicalStats(
                            weapon,
                            0, 0, 0, 0,
                            0, 0, 0, 0,
                            0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 16) {
            // Loopback-only exact selected-mode fixture: an equipped shield
            // (armor type 7 / native use type 25) admits Solid Carriage.
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(
                    SourceWeaponPhysicalStats.none(), true, false);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        // Keep the loopback Fire Spirit Ring fixture in the richer source
        // snapshot. Other memory characters deliberately carry no source gear.
        return characterObjectId == 3
                ? new SourceEquipmentMagicStats(
                        0, 0, 0, 0, 0, 30, 0,
                        0, 0, 0, 0, 0,
                        1, 1, 0, 0, 0, 0)
                : SourceEquipmentMagicStats.none();
    }

    @Override public SourceEquipmentEvaluation equipmentEvaluation(
            long characterObjectId, Map<Long, Boolean> equippedOverrides) {
        if (equippedOverrides == null) {
            throw new IllegalArgumentException(
                    "memory source equipment overrides are required");
        }
        if (equippedOverrides.isEmpty()) {
            return new SourceEquipmentEvaluation(
                    equipmentMagicStats(characterObjectId),
                    SourceEquipmentVitalStats.none());
        }
        if (sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25)
                && equippedOverrides.keySet().equals(Set.of(0x0F00_2008L))) {
            SourceEquipmentMagicStats magic = Boolean.TRUE.equals(
                    equippedOverrides.get(0x0F00_2008L))
                    ? equipmentMagicStats(characterObjectId)
                    : SourceEquipmentMagicStats.none();
            return new SourceEquipmentEvaluation(
                    magic, SourceEquipmentVitalStats.none());
        }
        if (!sourceProjectileFixtureEnabled || characterObjectId != 19
                || !equippedOverrides.keySet().equals(Set.of(1900L))) {
            throw new IllegalArgumentException(
                    "unknown memory source equipment override");
        }
        SourceEquipmentMagicStats magic = Boolean.TRUE.equals(
                equippedOverrides.get(1900L))
                ? equipmentMagicStats(characterObjectId)
                : new SourceEquipmentMagicStats(
                        SourceEquipmentPhysicalStats.none(), 0, 0);
        return new SourceEquipmentEvaluation(
                magic, SourceEquipmentVitalStats.none());
    }

    @Override public SourcePhysicalGrowthStats physicalGrowthStats() {
        return SourcePhysicalGrowthStats.classic381();
    }

    @Override public SourceNpcPhysicalStats npcPhysicalStats(int npcId) {
        return SourceNpcPhysicalStats.memoryFixture(npcId);
    }

    @Override public SourceSummonTemplate summonTemplate(int npcId) {
        return SourceSummonTemplate.memoryFixture(npcId);
    }

    @Override public List<SourcePet> pets(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        // One loopback-only retained-shape row keeps collar recall reachable
        // without creating or modifying a legacy character_pets row.
        if (characterObjectId == 3) {
            return List.of(new SourcePet(0x3810, 0x3811,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "牧羊犬", 5, 30, 5, 750, 0));
        }
        // A second loopback-only row pins the qualified native C_GIVE
        // evolution transition without changing any retained source database.
        if (characterObjectId == 19) {
            return List.of(new SourcePet(0x3820, 0x3821,
                    (int) InventoryCatalog.PET_COLLAR, 45034,
                    "進化驗證犬", 30, 120, 24, 60_000, 0));
        }
        return List.of();
    }

    @Override public SourcePetTemplate petTemplate(int npcId) {
        return SourcePetTemplate.memoryFixture(npcId);
    }

    @Override public SourceInventoryWeightStats inventoryWeightStats(long characterObjectId) {
        if (charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        int ordinaryFixtureRows = sourceInventoryFixtureEnabled
                && !sourceProjectileFixtureEnabled && characterObjectId == 3 ? 1 : 0;
        int transferFixtureRows = sourceInventoryFixtureEnabled
                && sourceProjectileFixtureEnabled && characterObjectId == 1 ? 1 : 0;
        int projectileFixtureRows = sourceProjectileFixtureEnabled
                && characterObjectId == 19 ? 2 : 0;
        int armorFixtureRows = sourceInventoryFixtureEnabled
                && (characterObjectId == 8 || characterObjectId == 10
                    || characterObjectId == 21 || characterObjectId == 23
                    || characterObjectId == 25) ? 1 : 0;
        int fixtureRows = ordinaryFixtureRows + transferFixtureRows
                + projectileFixtureRows + armorFixtureRows;
        int baseRows = sourceInventoryWeight == 0 ? 0 : 1;
        int fixtureWeight = ordinaryFixtureRows == 0 ? 0
                : BattleInventoryWeight.itemWeight(1, 1000);
        if (transferFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(3, 1_250));
        }
        if (projectileFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(1, 30_000));
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(100, 30));
        }
        if (armorFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(
                    fixtureWeight, BattleInventoryWeight.itemWeight(1, 70_000));
        }
        return new SourceInventoryWeightStats(
                BattleInventoryWeight.addWeight(sourceInventoryWeight, fixtureWeight),
                baseRows + fixtureRows, armorFixtureRows,
                projectileFixtureRows == 0 ? 0 : 1,
                baseRows + ordinaryFixtureRows + transferFixtureRows
                        + (projectileFixtureRows == 0 ? 0 : 1), sourceWeightRate);
    }

    private CharacterSummary charactersForObjectId(long characterObjectId) {
        for (int suffix = 0; suffix <= 26; suffix++) {
            String account = suffix == 0 ? baseAccount : baseAccount + suffix;
            CharacterSummary matched = characters(account).stream()
                    .filter(character -> character.objectId() == characterObjectId)
                    .findFirst().orElse(null);
            if (matched != null) return matched;
        }
        return null;
    }

    @Override public ClanEmblemData clanEmblem(String clanName) {
        boolean sourceFixture = "LegacyEmblem".equalsIgnoreCase(clanName);
        boolean siegeFixture = "LegacySiege".equalsIgnoreCase(clanName);
        if (!sourceFixture && !siegeFixture) return null;
        byte[] pixels = new byte[Protocol.EMBLEM_BYTES];
        for (int pixel = 0; pixel < pixels.length / 2; pixel++) {
            int color = ((pixel / 16) & 1) == (pixel & 1) ? 0x7FFF : 0x0421;
            pixels[pixel * 2] = (byte) color;
            pixels[pixel * 2 + 1] = (byte) (color >>> 8);
        }
        return new ClanEmblemData(
                siegeFixture ? 0x1020_3041L : 0x1020_3040L, pixels);
    }

    @Override public CastleSummary castleForClan(String clanName) {
        if ("LegacyEmblem".equalsIgnoreCase(clanName)) {
            return new CastleSummary(5, "LegacyEmblem", "TestEmblem", "海音城",
                    1_787_971_500_000L, 10, 10_000, 5);
        }
        if ("LegacyKent".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the exact selected castle-1 row.
            return new CastleSummary(1, "LegacyKent", "TestKentLord", "肯特城",
                    1_221_408_000_000L, 0, 0, 8);
        }
        if ("LegacyGiran".equalsIgnoreCase(clanName)) {
            // Loopback-only fixture for the one preserved castle_soldier row.
            return new CastleSummary(4, "LegacyGiran", "TestGiranLord", "奇岩城",
                    1_787_971_500_000L, 10, 60_000, 10);
        }
        if ("LegacyWindawood".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the retained castle-3 Othmond flow.
            return new CastleSummary(3, "LegacyWindawood", "TestWindawoodLord",
                    "風木城", 1_787_971_500_000L, 10, 446, 21);
        }
        if ("LegacyDwarf".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the exact selected castle-6 row.
            return new CastleSummary(6, "LegacyDwarf", "TestDwarfLord",
                    "侏儒城", 1_221_408_000_000L, 0, 0, 23);
        }
        if ("LegacyAden".equalsIgnoreCase(clanName)) {
            // Loopback-only carrier for the exact selected castle-7 row.
            return new CastleSummary(7, "LegacyAden", "TestAdenLord",
                    "亞丁城", 1_221_408_000_000L, 0, 49, 25);
        }
        return null;
    }

    @Override public CastleSummary castleById(int castleId) {
        for (String clan : List.of("LegacyEmblem", "LegacyKent", "LegacyGiran",
                "LegacyWindawood", "LegacyOrc", "LegacyDwarf", "LegacyAden")) {
            CastleSummary castle = castleForClan(clan);
            if (castle != null && castle.castleId() == castleId) return castle;
        }
        return null;
    }

    @Override public List<CastleWarSchedule> castleWarSchedules() {
        long start = sourceCastleWarStartMillis;
        int duration = sourceCastleWarDurationMinutes;
        int interval = sourceCastleWarIntervalMinutes;
        return List.of(
                new CastleWarSchedule(1, "肯特城", start, duration, interval),
                new CastleWarSchedule(2, "妖魔城", start, duration, interval),
                new CastleWarSchedule(3, "風木城", start, duration, interval),
                new CastleWarSchedule(4, "奇岩城", start, duration, interval),
                new CastleWarSchedule(5, "海音城", start, duration, interval),
                new CastleWarSchedule(6, "侏儒城", start, duration, interval),
                new CastleWarSchedule(7, "亞丁城", start, duration, interval),
                new CastleWarSchedule(8, "狄亞得要塞", start, duration, interval));
    }

    @Override public boolean characterExists(String characterName) {
        return characterName != null && (characterName.equalsIgnoreCase("TestKnight")
                || characterName.equalsIgnoreCase("TestMage")
                || characterName.equalsIgnoreCase("TestObserver")
                || characterName.equalsIgnoreCase("TestEmblem")
                || characterName.equalsIgnoreCase("TestElf")
                || characterName.equalsIgnoreCase("TestClanMate")
                || characterName.equalsIgnoreCase("TestKentLord")
                || characterName.equalsIgnoreCase("TestPrincess")
                || characterName.equalsIgnoreCase("TestGiranLord")
                || characterName.equalsIgnoreCase("TestGiranMate")
                || characterName.equalsIgnoreCase("TestPhysical")
                || characterName.equalsIgnoreCase("TestDefense")
                || characterName.equalsIgnoreCase("TestClaw")
                || characterName.equalsIgnoreCase("TestEdoryu")
                || characterName.equalsIgnoreCase("TestKiringku")
                || characterName.equalsIgnoreCase("TestChainsword")
                || characterName.equalsIgnoreCase("TestBow")
                || characterName.equalsIgnoreCase("TestCounterBarrier")
                || characterName.equalsIgnoreCase("TestWeaponDummy")
                || characterName.equalsIgnoreCase("TestWindawoodLord")
                || characterName.equalsIgnoreCase("TestWindawoodMate")
                || characterName.equalsIgnoreCase("TestDwarfLord")
                || characterName.equalsIgnoreCase("TestDwarfMate")
                || characterName.equalsIgnoreCase("TestAdenLord")
                || characterName.equalsIgnoreCase("TestAdenMate")
                || characterName.equalsIgnoreCase("TestSiegeLord"));
    }

    @Override public String description() { return "memory(loopback self-test only)"; }

    private static String key(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
    }
}

