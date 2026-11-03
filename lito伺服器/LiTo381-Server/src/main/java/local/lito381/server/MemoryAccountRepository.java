// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import local.lito381.server.AccountInfoSnapshot;
import local.lito381.server.AccountMutationResult;
import local.lito381.server.AccountRepository;
import local.lito381.server.AuthResult;
import local.lito381.server.BattleInventoryWeight;
import local.lito381.server.BattleNaturalHpRegen;
import local.lito381.server.CastleSummary;
import local.lito381.server.CastleWarSchedule;
import local.lito381.server.CharacterSummary;
import local.lito381.server.ClanEmblemData;
import local.lito381.server.ClassicWorldDropCatalog;
import local.lito381.server.GmCommandDefinition;
import local.lito381.server.InventoryCatalog;
import local.lito381.server.QuizRegistrationSnapshot;
import local.lito381.server.RepositoryTournamentShopOffer;
import local.lito381.server.SourceBaseMagicResistance;
import local.lito381.server.SourceEquipmentEvaluation;
import local.lito381.server.SourceEquipmentMagicStats;
import local.lito381.server.SourceEquipmentPhysicalStats;
import local.lito381.server.SourceEquipmentVitalStats;
import local.lito381.server.SourceEquippedFireResistance;
import local.lito381.server.SourceInventoryItem;
import local.lito381.server.SourceInventoryWeightStats;
import local.lito381.server.SourceNpcPhysicalStats;
import local.lito381.server.SourcePet;
import local.lito381.server.SourcePetTemplate;
import local.lito381.server.SourcePhysicalGrowthStats;
import local.lito381.server.SourceProjectilePhysicalStats;
import local.lito381.server.SourceSummonTemplate;
import local.lito381.server.SourceWeaponCombatTraits;
import local.lito381.server.SourceWeaponPhysicalStats;
import local.lito381.server.TournamentShopCatalog;

final class MemoryAccountRepository
implements AccountRepository {
    private static final long DEFAULT_CASTLE_WAR_START_MILLIS = 1787971500000L;
    private final String baseAccount;
    private final String authenticationFailureAccount;
    private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap();
    private final Set<String> litoOwnedAccounts = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, AccountInfoSnapshot> accountInfo = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, QuizRegistrationSnapshot> registeredQuiz = new ConcurrentHashMap();
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

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount) {
        this(account, password, authenticationFailureAccount, 0, 1.0);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, 40, 52);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, sourceFood, 52);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood, int sourcePrimaryLevel) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, sourceFood, sourcePrimaryLevel, 50, 110, false, false);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood, int sourcePrimaryLevel, int sourceSecondaryMaxMp) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, sourceFood, sourcePrimaryLevel, 50, sourceSecondaryMaxMp, false, false);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood, int sourcePrimaryLevel, int sourceSecondaryMaxMp, boolean sourceInventoryFixtureEnabled) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, sourceFood, sourcePrimaryLevel, 50, sourceSecondaryMaxMp, sourceInventoryFixtureEnabled, false);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood, int sourcePrimaryLevel, int sourceSecondaryMaxMp, boolean sourceInventoryFixtureEnabled, boolean sourceProjectileFixtureEnabled) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, sourceFood, sourcePrimaryLevel, 50, sourceSecondaryMaxMp, sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood, int sourcePrimaryLevel, int sourcePrimaryMaxHp, int sourceSecondaryMaxMp, boolean sourceInventoryFixtureEnabled, boolean sourceProjectileFixtureEnabled) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, sourceFood, sourcePrimaryLevel, sourcePrimaryMaxHp, sourceSecondaryMaxMp, sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled, -1, 60, 10080);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood, int sourcePrimaryLevel, int sourcePrimaryMaxHp, int sourceSecondaryMaxMp, boolean sourceInventoryFixtureEnabled, boolean sourceProjectileFixtureEnabled, int castleWarFixtureStartDelaySeconds, int castleWarFixtureDurationMinutes, int castleWarFixtureIntervalMinutes) {
        this(account, password, authenticationFailureAccount, sourceInventoryWeight, sourceWeightRate, sourceFood, sourcePrimaryLevel, sourcePrimaryMaxHp, 100, 10, 13, 10, 10, 9, 8, 11, sourceSecondaryMaxMp, sourceInventoryFixtureEnabled, sourceProjectileFixtureEnabled, castleWarFixtureStartDelaySeconds, castleWarFixtureDurationMinutes, castleWarFixtureIntervalMinutes);
    }

    MemoryAccountRepository(String account, String password, String authenticationFailureAccount, int sourceInventoryWeight, double sourceWeightRate, int sourceFood, int sourcePrimaryLevel, int sourcePrimaryMaxHp, int sourcePrimaryMaxMp, int sourcePrimaryArmorClass, int sourcePrimaryStrength, int sourcePrimaryConstitution, int sourcePrimaryDexterity, int sourcePrimaryCharisma, int sourcePrimaryIntelligence, int sourcePrimaryWisdom, int sourceSecondaryMaxMp, boolean sourceInventoryFixtureEnabled, boolean sourceProjectileFixtureEnabled, int castleWarFixtureStartDelaySeconds, int castleWarFixtureDurationMinutes, int castleWarFixtureIntervalMinutes) {
        this.baseAccount = account;
        this.authenticationFailureAccount = MemoryAccountRepository.key(authenticationFailureAccount);
        if (sourceInventoryWeight < 0 || !Double.isFinite(sourceWeightRate) || sourceWeightRate <= 0.0 || sourceFood < 0 || sourceFood > 255 || sourcePrimaryLevel < 1 || sourcePrimaryLevel > 999999 || sourcePrimaryMaxHp < 1 || sourcePrimaryMaxHp > 999999 || sourcePrimaryMaxMp < 1 || sourcePrimaryMaxMp > 999999 || sourcePrimaryArmorClass < -999999 || sourcePrimaryArmorClass > 999999 || sourcePrimaryStrength < 0 || sourcePrimaryStrength > 999999 || sourcePrimaryConstitution < 0 || sourcePrimaryConstitution > 999999 || sourcePrimaryDexterity < 0 || sourcePrimaryDexterity > 999999 || sourcePrimaryCharisma < 0 || sourcePrimaryCharisma > 999999 || sourcePrimaryIntelligence < 0 || sourcePrimaryIntelligence > 999999 || sourcePrimaryWisdom < 0 || sourcePrimaryWisdom > 999999 || sourceSecondaryMaxMp < 1 || sourceSecondaryMaxMp > 999999 || castleWarFixtureStartDelaySeconds < -1 || castleWarFixtureStartDelaySeconds > 3600 || castleWarFixtureDurationMinutes < 1 || castleWarFixtureIntervalMinutes <= castleWarFixtureDurationMinutes) {
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
        this.sourceCastleWarStartMillis = castleWarFixtureStartDelaySeconds < 0 ? 1787971500000L : Math.addExact(System.currentTimeMillis(), Math.multiplyExact((long)castleWarFixtureStartDelaySeconds, 1000L));
        this.sourceCastleWarDurationMinutes = castleWarFixtureDurationMinutes;
        this.sourceCastleWarIntervalMinutes = castleWarFixtureIntervalMinutes;
        this.credentials.put(MemoryAccountRepository.key(account), password);
        this.credentials.put(MemoryAccountRepository.key(account + "2"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "3"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "4"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "5"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "6"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "7"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "8"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "9"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "10"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "11"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "12"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "13"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "14"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "15"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "16"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "17"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "18"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "19"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "20"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "21"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "22"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "23"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "24"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "25"), password);
        this.credentials.put(MemoryAccountRepository.key(account + "26"), password);
    }

    @Override
    public void healthCheck() {
    }

    @Override
    public GmCommandDefinition gmCommand(String name) {
        return "de".equalsIgnoreCase(name == null ? "" : name.trim()) ? new GmCommandDefinition("de", 200, "L1SpawnDe", "loopback equivalent of lito_static.commands", false) : null;
    }

    @Override
    public List<RepositoryTournamentShopOffer> tournamentShopOffers() {
        return TournamentShopCatalog.defaultSourceRows();
    }

    @Override
    public AuthResult authenticate(String candidate, String suppliedPassword) throws SQLException {
        if (!this.authenticationFailureAccount.isEmpty() && this.authenticationFailureAccount.equals(MemoryAccountRepository.key(candidate))) {
            throw new SQLException("configured memory authentication failure fixture");
        }
        String expected = this.credentials.get(MemoryAccountRepository.key(candidate));
        boolean ok = expected != null && expected.equals(suppliedPassword);
        return ok ? new AuthResult(true, false, candidate) : AuthResult.rejected(candidate);
    }

    @Override
    public boolean accountExists(String account) {
        return this.credentials.containsKey(MemoryAccountRepository.key(account));
    }

    @Override
    public boolean deleteLitoAccount(String account) {
        String normalized = MemoryAccountRepository.key(account);
        return this.litoOwnedAccounts.remove(normalized) && this.credentials.remove(normalized) != null;
    }

    @Override
    public AccountMutationResult createAccount(String account, String password) {
        String normalized = MemoryAccountRepository.key(account);
        if (this.credentials.putIfAbsent(normalized, password) != null) {
            return AccountMutationResult.EXISTS;
        }
        this.litoOwnedAccounts.add(normalized);
        return AccountMutationResult.SUCCESS;
    }

    @Override
    public AccountMutationResult changePassword(String account, String oldPassword, String newPassword) {
        String normalized = MemoryAccountRepository.key(account);
        String current = this.credentials.get(normalized);
        if (current == null) {
            return AccountMutationResult.NOT_FOUND;
        }
        if (!current.equals(oldPassword)) {
            return AccountMutationResult.WRONG_PASSWORD;
        }
        return this.credentials.replace(normalized, oldPassword, newPassword) ? AccountMutationResult.SUCCESS : AccountMutationResult.WRONG_PASSWORD;
    }

    @Override
    public synchronized AccountMutationResult registerQuiz(String account, String oldPassword, String newPassword, List<String> quizFields) {
        String normalized = MemoryAccountRepository.key(account);
        String current = this.credentials.get(normalized);
        if (current == null) {
            return AccountMutationResult.NOT_FOUND;
        }
        if (!current.equals(oldPassword)) {
            return AccountMutationResult.WRONG_PASSWORD;
        }
        this.credentials.put(normalized, newPassword);
        this.registeredQuiz.put(normalized, new QuizRegistrationSnapshot(quizFields));
        return AccountMutationResult.SUCCESS;
    }

    QuizRegistrationSnapshot registeredQuizForTest(String account) {
        return this.registeredQuiz.get(MemoryAccountRepository.key(account));
    }

    @Override
    public AccountMutationResult changeAccountInfo(String account, int type, List<String> fields) {
        String normalized = MemoryAccountRepository.key(account);
        if (!this.credentials.containsKey(normalized)) {
            return AccountMutationResult.NOT_FOUND;
        }
        this.accountInfo.put(normalized, new AccountInfoSnapshot(type, fields));
        return AccountMutationResult.SUCCESS;
    }

    AccountInfoSnapshot accountInfoForTest(String account) {
        return this.accountInfo.get(MemoryAccountRepository.key(account));
    }

    @Override
    public List<CharacterSummary> characters(String requestedAccount) {
        boolean basePlayer = this.baseAccount.equalsIgnoreCase(requestedAccount);
        boolean secondPlayer = (this.baseAccount + "2").equalsIgnoreCase(requestedAccount);
        boolean observer = (this.baseAccount + "3").equalsIgnoreCase(requestedAccount);
        boolean emblemPlayer = (this.baseAccount + "4").equalsIgnoreCase(requestedAccount);
        boolean elfPlayer = (this.baseAccount + "5").equalsIgnoreCase(requestedAccount);
        boolean clanMate = (this.baseAccount + "6").equalsIgnoreCase(requestedAccount);
        boolean kentOwner = (this.baseAccount + "7").equalsIgnoreCase(requestedAccount);
        boolean femalePlayer = (this.baseAccount + "8").equalsIgnoreCase(requestedAccount);
        boolean giranOwner = (this.baseAccount + "9").equalsIgnoreCase(requestedAccount);
        boolean giranMate = (this.baseAccount + "10").equalsIgnoreCase(requestedAccount);
        boolean physicalPlayer = (this.baseAccount + "11").equalsIgnoreCase(requestedAccount);
        boolean defensePlayer = (this.baseAccount + "12").equalsIgnoreCase(requestedAccount);
        boolean clawPlayer = (this.baseAccount + "13").equalsIgnoreCase(requestedAccount);
        boolean edoryuPlayer = (this.baseAccount + "14").equalsIgnoreCase(requestedAccount);
        boolean weaponDummy = (this.baseAccount + "15").equalsIgnoreCase(requestedAccount);
        boolean kiringkuPlayer = (this.baseAccount + "16").equalsIgnoreCase(requestedAccount);
        boolean chainswordPlayer = (this.baseAccount + "17").equalsIgnoreCase(requestedAccount);
        boolean bowPlayer = (this.baseAccount + "18").equalsIgnoreCase(requestedAccount);
        boolean counterBarrierPlayer = (this.baseAccount + "19").equalsIgnoreCase(requestedAccount);
        boolean windawoodOwner = (this.baseAccount + "20").equalsIgnoreCase(requestedAccount);
        boolean windawoodMate = (this.baseAccount + "21").equalsIgnoreCase(requestedAccount);
        boolean dwarfOwner = (this.baseAccount + "22").equalsIgnoreCase(requestedAccount);
        boolean dwarfMate = (this.baseAccount + "23").equalsIgnoreCase(requestedAccount);
        boolean adenOwner = (this.baseAccount + "24").equalsIgnoreCase(requestedAccount);
        boolean adenMate = (this.baseAccount + "25").equalsIgnoreCase(requestedAccount);
        boolean siegeOwner = (this.baseAccount + "26").equalsIgnoreCase(requestedAccount);
        if (!(basePlayer || secondPlayer || observer || emblemPlayer || elfPlayer || clanMate || kentOwner || femalePlayer || giranOwner || giranMate || physicalPlayer || defensePlayer || clawPlayer || edoryuPlayer || weaponDummy || kiringkuPlayer || chainswordPlayer || bowPlayer || counterBarrierPlayer || windawoodOwner || windawoodMate || dwarfOwner || dwarfMate || adenOwner || adenMate || siegeOwner)) {
            return List.of();
        }
        return List.of(new CharacterSummary(siegeOwner ? "TestSiegeLord" : (adenMate ? "TestAdenMate" : (adenOwner ? "TestAdenLord" : (dwarfMate ? "TestDwarfMate" : (dwarfOwner ? "TestDwarfLord" : (windawoodMate ? "TestWindawoodMate" : (windawoodOwner ? "TestWindawoodLord" : (counterBarrierPlayer ? "TestCounterBarrier" : (bowPlayer ? "TestBow" : (chainswordPlayer ? "TestChainsword" : (kiringkuPlayer ? "TestKiringku" : (weaponDummy ? "TestWeaponDummy" : (edoryuPlayer ? "TestEdoryu" : (clawPlayer ? "TestClaw" : (defensePlayer ? "TestDefense" : (physicalPlayer ? "TestPhysical" : (giranMate ? "TestGiranMate" : (giranOwner ? "TestGiranLord" : (femalePlayer ? "TestPrincess" : (kentOwner ? "TestKentLord" : (clanMate ? "TestClanMate" : (elfPlayer ? "TestElf" : (emblemPlayer ? "TestEmblem" : (observer ? "TestObserver" : (secondPlayer ? "TestMage" : "TestKnight")))))))))))))))))))))))), siegeOwner ? "LegacySiege" : (adenOwner || adenMate ? "LegacyAden" : (dwarfOwner || dwarfMate ? "LegacyDwarf" : (windawoodOwner || windawoodMate ? "LegacyWindawood" : (giranOwner || giranMate ? "LegacyGiran" : (kentOwner ? "LegacyKent" : (emblemPlayer || clanMate ? "LegacyEmblem" : "")))))), kiringkuPlayer ? 6 : (chainswordPlayer ? 5 : (bowPlayer ? 2 : (counterBarrierPlayer ? 1 : (clawPlayer || edoryuPlayer ? 4 : (elfPlayer ? 2 : (secondPlayer || observer || clanMate || giranMate || windawoodMate || dwarfMate || adenMate || physicalPlayer || defensePlayer || weaponDummy ? 1 : 0)))))), femalePlayer ? 1 : 0, observer ? 15 : (secondPlayer ? 47 : (elfPlayer ? 45 : (clanMate || giranMate || windawoodMate || dwarfMate ? 40 : (kentOwner || windawoodOwner || dwarfOwner || siegeOwner ? 55 : this.sourcePrimaryLevel)))), weaponDummy || counterBarrierPlayer ? 2000 : (secondPlayer ? 100 : this.sourcePrimaryMaxHp), observer ? 10 : (secondPlayer ? this.sourceSecondaryMaxMp : (basePlayer ? this.sourcePrimaryMaxMp : 100)), defensePlayer ? -10 : (elfPlayer ? 100 : (basePlayer ? this.sourcePrimaryArmorClass : 10)), basePlayer ? this.sourcePrimaryStrength : 13, basePlayer ? this.sourcePrimaryConstitution : 10, basePlayer ? this.sourcePrimaryDexterity : 10, basePlayer ? this.sourcePrimaryCharisma : (siegeOwner ? 18 : (adenOwner || adenMate ? 18 : (dwarfOwner || dwarfMate ? 18 : (windawoodOwner || windawoodMate ? 18 : (giranOwner || giranMate ? 18 : 9))))), basePlayer ? this.sourcePrimaryIntelligence : (kiringkuPlayer ? 13 : 8), basePlayer ? this.sourcePrimaryWisdom : 11, kiringkuPlayer ? 13 : 8, 11, siegeOwner ? 27L : (adenMate ? 26L : (adenOwner ? 25L : (dwarfMate ? 24L : (dwarfOwner ? 23L : (windawoodMate ? 22L : (windawoodOwner ? 21L : (counterBarrierPlayer ? 20L : (bowPlayer ? 19L : (chainswordPlayer ? 18L : (kiringkuPlayer ? 17L : (weaponDummy ? 16L : (edoryuPlayer ? 15L : (clawPlayer ? 14L : (defensePlayer ? 13L : (physicalPlayer ? 12L : (giranMate ? 11L : (giranOwner ? 10L : (femalePlayer ? 9L : (kentOwner ? 8L : (clanMate ? 7L : (elfPlayer ? 6L : (emblemPlayer ? 5L : (observer ? 4L : (secondPlayer ? 3L : 1L)))))))))))))))))))))))), 0L, basePlayer ? 1234L : (secondPlayer ? -1234L : 0L), basePlayer ? 7 : 0, 0L, 32600L, 32700L, 4L, 10, this.sourceFood, BattleNaturalHpRegen.fallbackConHprMaximum(10), 13, 10, elfPlayer ? 1 : 0));
    }

    @Override
    public Set<Integer> learnedSkills(long characterObjectId) {
        if (characterObjectId == 1L) {
            return Set.of(1, 2, 4, 6, 7, 11, 15, 16, 17, 18, 25, 34, 35, 44, 58, 63, 77, 78, 131);
        }
        if (characterObjectId == 3L) {
            return Set.of(1, 3, 4, 7, 49, 8, 9, 10, 28, 29, 30, 22, 23, 13, 14, 19, 20, 26, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 51, 52, 53, 54, 56, 57, 59, 60, 61, 62, 64, 65, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 131, 157, 206);
        }
        if (characterObjectId == 5L) {
            return Set.of(Integer.valueOf(113), Integer.valueOf(114), Integer.valueOf(115), Integer.valueOf(116), Integer.valueOf(117), Integer.valueOf(118), Integer.valueOf(131));
        }
        if (characterObjectId == 7L) {
            return Set.of(Integer.valueOf(49), Integer.valueOf(55), Integer.valueOf(69), Integer.valueOf(131));
        }
        if (characterObjectId == 12L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 8L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 9L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 10L) {
            return Set.of(Integer.valueOf(21), Integer.valueOf(131));
        }
        if (characterObjectId == 11L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 21L || characterObjectId == 22L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 23L || characterObjectId == 24L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 25L || characterObjectId == 26L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 27L) {
            return Set.of(Integer.valueOf(131));
        }
        if (characterObjectId == 6L) {
            return Set.of(129, 130, 131, 133, 134, 137, 138, 146, 154, 162, 163, 164, 170, 157, 169, 176);
        }
        if (characterObjectId == 19L) {
            return Set.of(Integer.valueOf(131), Integer.valueOf(132));
        }
        if (characterObjectId == 18L) {
            return Set.of(131, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195);
        }
        if (characterObjectId == 17L) {
            return Set.of(Integer.valueOf(131), Integer.valueOf(202), Integer.valueOf(207), Integer.valueOf(208), Integer.valueOf(213));
        }
        if (characterObjectId == 15L) {
            return Set.of(Integer.valueOf(8), Integer.valueOf(12), Integer.valueOf(87), Integer.valueOf(131));
        }
        if (characterObjectId == 16L) {
            return Set.of(Integer.valueOf(27), Integer.valueOf(31), Integer.valueOf(44), Integer.valueOf(60), Integer.valueOf(131), Integer.valueOf(88), Integer.valueOf(89), Integer.valueOf(90));
        }
        if (characterObjectId == 20L) {
            return Set.of(Integer.valueOf(44), Integer.valueOf(91), Integer.valueOf(131));
        }
        return Set.of();
    }

    @Override
    public Map<Integer, Integer> questSteps(long characterObjectId) {
        if (this.charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        return characterObjectId == 3L ? Map.of(10701, 1) : Map.of();
    }

    @Override
    public List<SourceInventoryItem> sourceInventory(long characterObjectId) {
        if (this.sourceInventoryFixtureEnabled && (characterObjectId == 8L || characterObjectId == 10L || characterObjectId == 21L || characterObjectId == 23L || characterObjectId == 25L)) {
            ClassicWorldDropCatalog.ItemWire armor = new ClassicWorldDropCatalog.ItemWire(20089, "armor", "$116", "armor", 51, 3830, 0, false, true, 70000, 0, "", 0, 0, "leather", 4, 127);
            InventoryCatalog.NativeArmorStatus status = new InventoryCatalog.NativeArmorStatus(2, ClassicWorldDropCatalog.materialCode("leather"), 70000, 127, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2);
            return List.of(new SourceInventoryItem(251666440L, 20089, 1, true, 0, 0, true, 0, false, armor, status));
        }
        if (this.sourceInventoryFixtureEnabled && this.sourceProjectileFixtureEnabled && characterObjectId == 1L) {
            ClassicWorldDropCatalog.ItemWire token = new ClassicWorldDropCatalog.ItemWire(65001, "etc", "Source Trade Token", "none", 321, 322, 1, true, true, 1250, 87, "", 0, 0, "plant", -1, 0);
            return List.of(new SourceInventoryItem(2001L, 65001, 3, false, 0, 0, true, 1, false, token));
        }
        if (this.sourceInventoryFixtureEnabled && !this.sourceProjectileFixtureEnabled && characterObjectId == 3L) {
            ClassicWorldDropCatalog.ItemWire claw = new ClassicWorldDropCatalog.ItemWire(40590, "etc", "$2023", "none", 139, 3963, 0, false, false, 1000, 0, "", 0, 0, "bone", -1, 0);
            return List.of(new SourceInventoryItem(251674713L, 40590, 1, false, 0, 0, true, 0, false, claw));
        }
        if (this.sourceProjectileFixtureEnabled && characterObjectId == 19L) {
            ClassicWorldDropCatalog.ItemWire bow = new ClassicWorldDropCatalog.ItemWire(172, "weapon", "$24", "weapon", 278, 12, 0, false, true, 30000, 0, "bow", 2, 2, "wood", 6, 79);
            ClassicWorldDropCatalog.ItemWire arrow = new ClassicWorldDropCatalog.ItemWire(40743, "etc", "$61", "arrow", 7, 66, 0, true, true, 30, 0, "", 0, 0, "wood", -1, 0);
            return List.of(new SourceInventoryItem(1900L, 172, 1, true, 0, 0, true, 0, false, bow), new SourceInventoryItem(1901L, 40743, 100, false, 0, 0, true, 0, false, arrow));
        }
        return List.of();
    }

    @Override
    public SourceEquippedFireResistance equippedFireResistance(long characterObjectId) {
        return characterObjectId == 3L ? new SourceEquippedFireResistance(30, 1) : SourceEquippedFireResistance.none();
    }

    @Override
    public SourceBaseMagicResistance baseMagicResistance(long characterObjectId) {
        CharacterSummary character = this.charactersForObjectId(characterObjectId);
        if (character == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        return SourceBaseMagicResistance.from(character);
    }

    @Override
    public SourceEquipmentMagicStats equipmentMagicStats(long characterObjectId) {
        if (this.charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        if (characterObjectId == 12L) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(24, 1, 6, 8, 1, 0, 0, 0, 0).withCombatTraits(new SourceWeaponCombatTraits(11, 1, 0, 0, 0)).withAbility(1, 2, 0, 0);
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(weapon, 4, 5, 0, 0, 6, 7, 0, 0, 8, 9, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 3, 1);
        }
        if (this.sourceInventoryFixtureEnabled && (characterObjectId == 8L || characterObjectId == 10L || characterObjectId == 21L || characterObjectId == 23L || characterObjectId == 25L)) {
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats.none(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2, 0);
            return new SourceEquipmentMagicStats(physical, 1, 1);
        }
        if (characterObjectId == 13L) {
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats.none(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 5);
            return new SourceEquipmentMagicStats(physical, 15, 0, 1, 1);
        }
        if (characterObjectId == 14L || characterObjectId == 15L) {
            boolean claw = characterObjectId == 14L;
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(claw ? 152 : 69, claw ? 11 : 12, 8, claw ? 7 : 6, 1, 2, 0, 0, 0).withCombatTraits(new SourceWeaponCombatTraits(11, 0, 0, 0, 0, claw ? 0 : 10, claw ? 0.0 : 1.2, claw ? 33 : 0, false, false));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(weapon, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            return characterObjectId == 15L ? new SourceEquipmentMagicStats(physical, 1, 0, 100) : new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 17L) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(270, 17, 16, 16, 1, 1, 2, 0, 0).withCombatTraits(new SourceWeaponCombatTraits(13, 0, 0, 0, 0, 0, 0.0, 0, false, false, 3018));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(weapon, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 27L) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(52, 3, 17, 21, 1, 0, 0, 0, 0).withNameId("$3");
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(weapon, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 18L) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(272, 18, 16, 12, 2, 2, 5, 0, 0).withCombatTraits(new SourceWeaponCombatTraits(13, 0, 0, 0, 0, 0, 0.0, 0, false, false, 3033));
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(weapon, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 19L) {
            SourceProjectilePhysicalStats arrow = new SourceProjectilePhysicalStats(1901L, 40743, -2, 5, 7, 66, true, 100L);
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(172, 4, 6, 6, 15, 0, 0, 0, 0).withCombatTraits(new SourceWeaponCombatTraits(8, 1, 0, 0, 0)).withProjectile(arrow);
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(weapon, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 20L) {
            SourceWeaponPhysicalStats weapon = new SourceWeaponPhysicalStats(52, 3, 17, 21, 1, 0, 0, 0, 0).withNameId("$3");
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(weapon, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        if (characterObjectId == 16L) {
            SourceEquipmentPhysicalStats physical = new SourceEquipmentPhysicalStats(SourceWeaponPhysicalStats.none(), true, false);
            return new SourceEquipmentMagicStats(physical, 1, 0);
        }
        return characterObjectId == 3L ? new SourceEquipmentMagicStats(0, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0) : SourceEquipmentMagicStats.none();
    }

    @Override
    public SourceEquipmentEvaluation equipmentEvaluation(long characterObjectId, Map<Long, Boolean> equippedOverrides) {
        if (equippedOverrides == null) {
            throw new IllegalArgumentException("memory source equipment overrides are required");
        }
        if (equippedOverrides.isEmpty()) {
            return new SourceEquipmentEvaluation(this.equipmentMagicStats(characterObjectId), SourceEquipmentVitalStats.none());
        }
        if (this.sourceInventoryFixtureEnabled && (characterObjectId == 8L || characterObjectId == 10L || characterObjectId == 21L || characterObjectId == 23L || characterObjectId == 25L) && equippedOverrides.keySet().equals(Set.of(Long.valueOf(251666440L)))) {
            SourceEquipmentMagicStats magic = Boolean.TRUE.equals(equippedOverrides.get(251666440L)) ? this.equipmentMagicStats(characterObjectId) : SourceEquipmentMagicStats.none();
            return new SourceEquipmentEvaluation(magic, SourceEquipmentVitalStats.none());
        }
        if (!this.sourceProjectileFixtureEnabled || characterObjectId != 19L || !equippedOverrides.keySet().equals(Set.of(Long.valueOf(1900L)))) {
            throw new IllegalArgumentException("unknown memory source equipment override");
        }
        SourceEquipmentMagicStats magic = Boolean.TRUE.equals(equippedOverrides.get(1900L)) ? this.equipmentMagicStats(characterObjectId) : new SourceEquipmentMagicStats(SourceEquipmentPhysicalStats.none(), 0, 0);
        return new SourceEquipmentEvaluation(magic, SourceEquipmentVitalStats.none());
    }

    @Override
    public SourcePhysicalGrowthStats physicalGrowthStats() {
        return SourcePhysicalGrowthStats.classic381();
    }

    @Override
    public SourceNpcPhysicalStats npcPhysicalStats(int npcId) {
        return SourceNpcPhysicalStats.memoryFixture(npcId);
    }

    @Override
    public SourceSummonTemplate summonTemplate(int npcId) {
        return SourceSummonTemplate.memoryFixture(npcId);
    }

    @Override
    public List<SourcePet> pets(long characterObjectId) {
        if (this.charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        if (characterObjectId == 3L) {
            return List.of(new SourcePet(14352L, 14353L, 40314, 45034, "\u7267\u7f8a\u72ac", 5, 30, 5, 750L, 0));
        }
        if (characterObjectId == 19L) {
            return List.of(new SourcePet(14368L, 14369L, 40314, 45034, "\u9032\u5316\u9a57\u8b49\u72ac", 30, 120, 24, 60000L, 0));
        }
        return List.of();
    }

    @Override
    public SourcePetTemplate petTemplate(int npcId) {
        return SourcePetTemplate.memoryFixture(npcId);
    }

    @Override
    public SourceInventoryWeightStats inventoryWeightStats(long characterObjectId) {
        int fixtureWeight;
        if (this.charactersForObjectId(characterObjectId) == null) {
            throw new IllegalArgumentException("unknown memory character object id");
        }
        int ordinaryFixtureRows = this.sourceInventoryFixtureEnabled && !this.sourceProjectileFixtureEnabled && characterObjectId == 3L ? 1 : 0;
        int transferFixtureRows = this.sourceInventoryFixtureEnabled && this.sourceProjectileFixtureEnabled && characterObjectId == 1L ? 1 : 0;
        int projectileFixtureRows = this.sourceProjectileFixtureEnabled && characterObjectId == 19L ? 2 : 0;
        int armorFixtureRows = this.sourceInventoryFixtureEnabled && (characterObjectId == 8L || characterObjectId == 10L || characterObjectId == 21L || characterObjectId == 23L || characterObjectId == 25L) ? 1 : 0;
        int fixtureRows = ordinaryFixtureRows + transferFixtureRows + projectileFixtureRows + armorFixtureRows;
        int baseRows = this.sourceInventoryWeight == 0 ? 0 : 1;
        int n = fixtureWeight = ordinaryFixtureRows == 0 ? 0 : BattleInventoryWeight.itemWeight(1L, 1000);
        if (transferFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(fixtureWeight, BattleInventoryWeight.itemWeight(3L, 1250));
        }
        if (projectileFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(fixtureWeight, BattleInventoryWeight.itemWeight(1L, 30000));
            fixtureWeight = BattleInventoryWeight.addWeight(fixtureWeight, BattleInventoryWeight.itemWeight(100L, 30));
        }
        if (armorFixtureRows != 0) {
            fixtureWeight = BattleInventoryWeight.addWeight(fixtureWeight, BattleInventoryWeight.itemWeight(1L, 70000));
        }
        return new SourceInventoryWeightStats(BattleInventoryWeight.addWeight(this.sourceInventoryWeight, fixtureWeight), baseRows + fixtureRows, armorFixtureRows, projectileFixtureRows == 0 ? 0 : 1, baseRows + ordinaryFixtureRows + transferFixtureRows + (projectileFixtureRows == 0 ? 0 : 1), this.sourceWeightRate);
    }

    private CharacterSummary charactersForObjectId(long characterObjectId) {
        for (int suffix = 0; suffix <= 26; ++suffix) {
            Object account = suffix == 0 ? this.baseAccount : this.baseAccount + suffix;
            CharacterSummary matched = this.characters((String)account).stream().filter(character -> character.objectId() == characterObjectId).findFirst().orElse(null);
            if (matched == null) continue;
            return matched;
        }
        return null;
    }

    @Override
    public ClanEmblemData clanEmblem(String clanName) {
        boolean sourceFixture = "LegacyEmblem".equalsIgnoreCase(clanName);
        boolean siegeFixture = "LegacySiege".equalsIgnoreCase(clanName);
        if (!sourceFixture && !siegeFixture) {
            return null;
        }
        byte[] pixels = new byte[384];
        for (int pixel = 0; pixel < pixels.length / 2; ++pixel) {
            int color = (pixel / 16 & 1) == (pixel & 1) ? Short.MAX_VALUE : 1057;
            pixels[pixel * 2] = (byte)color;
            pixels[pixel * 2 + 1] = (byte)(color >>> 8);
        }
        return new ClanEmblemData(siegeFixture ? 270544961L : 270544960L, pixels);
    }

    @Override
    public CastleSummary castleForClan(String clanName) {
        if ("LegacyEmblem".equalsIgnoreCase(clanName)) {
            return new CastleSummary(5, "LegacyEmblem", "TestEmblem", "\u6d77\u97f3\u57ce", 1787971500000L, 10, 10000L, 5L);
        }
        if ("LegacyKent".equalsIgnoreCase(clanName)) {
            return new CastleSummary(1, "LegacyKent", "TestKentLord", "\u80af\u7279\u57ce", 1221408000000L, 0, 0L, 8L);
        }
        if ("LegacyGiran".equalsIgnoreCase(clanName)) {
            return new CastleSummary(4, "LegacyGiran", "TestGiranLord", "\u5947\u5ca9\u57ce", 1787971500000L, 10, 60000L, 10L);
        }
        if ("LegacyWindawood".equalsIgnoreCase(clanName)) {
            return new CastleSummary(3, "LegacyWindawood", "TestWindawoodLord", "\u98a8\u6728\u57ce", 1787971500000L, 10, 446L, 21L);
        }
        if ("LegacyDwarf".equalsIgnoreCase(clanName)) {
            return new CastleSummary(6, "LegacyDwarf", "TestDwarfLord", "\u4f8f\u5112\u57ce", 1221408000000L, 0, 0L, 23L);
        }
        if ("LegacyAden".equalsIgnoreCase(clanName)) {
            return new CastleSummary(7, "LegacyAden", "TestAdenLord", "\u4e9e\u4e01\u57ce", 1221408000000L, 0, 49L, 25L);
        }
        return null;
    }

    @Override
    public CastleSummary castleById(int castleId) {
        for (String clan : List.of("LegacyEmblem", "LegacyKent", "LegacyGiran", "LegacyWindawood", "LegacyOrc", "LegacyDwarf", "LegacyAden")) {
            CastleSummary castle = this.castleForClan(clan);
            if (castle == null || castle.castleId() != castleId) continue;
            return castle;
        }
        return null;
    }

    @Override
    public List<CastleWarSchedule> castleWarSchedules() {
        long start = this.sourceCastleWarStartMillis;
        int duration = this.sourceCastleWarDurationMinutes;
        int interval = this.sourceCastleWarIntervalMinutes;
        return List.of(new CastleWarSchedule(1, "\u80af\u7279\u57ce", start, duration, interval), new CastleWarSchedule(2, "\u5996\u9b54\u57ce", start, duration, interval), new CastleWarSchedule(3, "\u98a8\u6728\u57ce", start, duration, interval), new CastleWarSchedule(4, "\u5947\u5ca9\u57ce", start, duration, interval), new CastleWarSchedule(5, "\u6d77\u97f3\u57ce", start, duration, interval), new CastleWarSchedule(6, "\u4f8f\u5112\u57ce", start, duration, interval), new CastleWarSchedule(7, "\u4e9e\u4e01\u57ce", start, duration, interval), new CastleWarSchedule(8, "\u72c4\u4e9e\u5f97\u8981\u585e", start, duration, interval));
    }

    @Override
    public boolean characterExists(String characterName) {
        return characterName != null && (characterName.equalsIgnoreCase("TestKnight") || characterName.equalsIgnoreCase("TestMage") || characterName.equalsIgnoreCase("TestObserver") || characterName.equalsIgnoreCase("TestEmblem") || characterName.equalsIgnoreCase("TestElf") || characterName.equalsIgnoreCase("TestClanMate") || characterName.equalsIgnoreCase("TestKentLord") || characterName.equalsIgnoreCase("TestPrincess") || characterName.equalsIgnoreCase("TestGiranLord") || characterName.equalsIgnoreCase("TestGiranMate") || characterName.equalsIgnoreCase("TestPhysical") || characterName.equalsIgnoreCase("TestDefense") || characterName.equalsIgnoreCase("TestClaw") || characterName.equalsIgnoreCase("TestEdoryu") || characterName.equalsIgnoreCase("TestKiringku") || characterName.equalsIgnoreCase("TestChainsword") || characterName.equalsIgnoreCase("TestBow") || characterName.equalsIgnoreCase("TestCounterBarrier") || characterName.equalsIgnoreCase("TestWeaponDummy") || characterName.equalsIgnoreCase("TestWindawoodLord") || characterName.equalsIgnoreCase("TestWindawoodMate") || characterName.equalsIgnoreCase("TestDwarfLord") || characterName.equalsIgnoreCase("TestDwarfMate") || characterName.equalsIgnoreCase("TestAdenLord") || characterName.equalsIgnoreCase("TestAdenMate") || characterName.equalsIgnoreCase("TestSiegeLord"));
    }

    @Override
    public String description() {
        return "memory(loopback self-test only)";
    }

    private static String key(String account) {
        return account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
    }
}
