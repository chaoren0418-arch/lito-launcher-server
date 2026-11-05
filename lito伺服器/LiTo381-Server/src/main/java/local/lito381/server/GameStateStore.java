package local.lito381.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Durable local state that is intentionally separate from the legacy read-only Lineage database. */
final class GameStateStore {
    record Economy(long points, Map<Long, Integer> inventory,
                   Map<Long, List<RuntimeInventory.PersistentItem>> itemStates) {
        Economy {
            inventory = Map.copyOf(inventory);
            itemStates = immutableItemStates(itemStates);
        }

        /** Format-23 compatibility projection used by retained migration tests. */
        Map<Long, List<Integer>> inventoryEnchantments() {
            return enchantmentsFromItemStates(itemStates, inventory);
        }
    }
    record SourceInventoryReconciliation(
            Economy economy,
            Map<Long, InventoryCatalog.Definition> definitions,
            Map<Long, SourceWeaponPhysicalStats> sourceWeapons,
            Map<Long, Long> sourceEquipmentObjects,
            Map<Long, Long> sourceProjectileObjects,
            Map<Long, SourceMagicDollType> sourceMagicDolls,
            Set<Long> consumedSourceEquipmentObjects,
            int observedSourceWeight, int retainedSourceWeight,
            int sourceRows, int importableRows, int activeSourceObjects,
            int importedRows, int resetRows, int removedRows,
            int deferredEquipmentRows, int deferredPetCollarRows,
            int deferredWeaponArmorRows, int deferredProjectileRows,
            int importedWeaponRows, int qualifiedSourceWeaponObjects,
            int importedArmorRows, int equippedSourceArmorObjects,
            int importedProjectileRows, int qualifiedSourceProjectileObjects) {
        SourceInventoryReconciliation {
            if (economy == null || definitions == null || sourceWeapons == null
                    || sourceEquipmentObjects == null || sourceProjectileObjects == null
                    || sourceMagicDolls == null
                    || consumedSourceEquipmentObjects == null
                    || observedSourceWeight < 0
                    || retainedSourceWeight < 0 || sourceRows < 0
                    || importableRows < 0 || activeSourceObjects < 0
                    || importedRows < 0 || resetRows < 0 || removedRows < 0
                    || deferredEquipmentRows < 0 || deferredPetCollarRows < 0
                    || deferredWeaponArmorRows < 0 || deferredProjectileRows < 0
                    || importedWeaponRows < 0 || qualifiedSourceWeaponObjects < 0
                    || qualifiedSourceWeaponObjects > 1 || importedArmorRows < 0
                    || equippedSourceArmorObjects < 0
                    || equippedSourceArmorObjects > importedArmorRows
                    || importedProjectileRows < 0
                    || qualifiedSourceProjectileObjects < 0
                    || qualifiedSourceProjectileObjects > 1
                    || qualifiedSourceProjectileObjects > importedProjectileRows) {
                throw new IllegalArgumentException("invalid source inventory reconciliation");
            }
            definitions = Map.copyOf(definitions);
            sourceWeapons = Map.copyOf(sourceWeapons);
            sourceEquipmentObjects = Map.copyOf(sourceEquipmentObjects);
            sourceProjectileObjects = Map.copyOf(sourceProjectileObjects);
            sourceMagicDolls = Map.copyOf(sourceMagicDolls);
            consumedSourceEquipmentObjects = Set.copyOf(
                    consumedSourceEquipmentObjects);
        }
    }
    record SourceInventoryBinding(
            long sourceObjectId, long localObjectId, int catalogId, boolean consumed,
            boolean litoOwned) {
        SourceInventoryBinding(long sourceObjectId, long localObjectId,
                               int catalogId, boolean consumed) {
            this(sourceObjectId, localObjectId, catalogId, consumed, false);
        }
        SourceInventoryBinding {
            if (sourceObjectId <= 0 || localObjectId <= 0 || catalogId <= 0) {
                throw new IllegalArgumentException("invalid source inventory binding");
            }
        }
    }
    record Warehouse(Map<Long, Integer> inventory,
                     Map<Long, List<RuntimeInventory.PersistentItem>> itemStates) {
        Warehouse {
            inventory = Map.copyOf(inventory);
            itemStates = immutableItemStates(itemStates);
        }

        Warehouse(Map<Long, Integer> inventory) {
            this(inventory, Map.of());
        }
    }
    record StoryQuestState(Map<Integer, Integer> observedSource,
                           Map<Integer, Integer> effective) {
        StoryQuestState {
            observedSource = immutableQuestSteps(observedSource);
            effective = immutableQuestSteps(effective);
        }

        int step(int questId) {
            return effective.getOrDefault(questId, 0);
        }

        boolean contains(int questId) {
            return effective.containsKey(questId);
        }
    }
    record ElfAttributeState(int observedSource, int effective,
                             Set<Integer> forgottenSkills) {
        ElfAttributeState {
            requireElfAttribute(observedSource, "observed source ElfAttr");
            requireElfAttribute(effective, "effective ElfAttr");
            forgottenSkills = immutableNativeSkills(forgottenSkills);
        }
    }
    record LawfulBalance(int observedSource, int effective) {
        LawfulBalance {
            requireLawful(observedSource, "observed source lawful");
            requireLawful(effective, "effective lawful");
        }
    }
    record LawfulMutation(int before, int after, int requestedDelta) {
        LawfulMutation {
            requireLawful(before, "lawful before mutation");
            requireLawful(after, "lawful after mutation");
        }

        boolean changed() { return before != after; }
        int appliedDelta() { return after - before; }
    }
    record GeneralPkState(int count, long lastPkEpochSeconds) {
        GeneralPkState {
            if (count < 0 || lastPkEpochSeconds < 0) {
                throw new IllegalArgumentException("invalid native general-PK state");
            }
        }

        boolean wantedAt(long nowEpochSeconds) {
            return ClassicWorldGuardCatalog.wanted(
                    lastPkEpochSeconds, nowEpochSeconds);
        }
    }
    record GeneralPkBalance(GeneralPkState observedSource,
                            GeneralPkState effective) {
        GeneralPkBalance {
            if (observedSource == null || effective == null) {
                throw new IllegalArgumentException(
                        "missing native general-PK balance");
            }
        }
    }
    record ClassicKillDeathState(int kills, int deaths) {
        ClassicKillDeathState {
            if (kills < 0 || deaths < 0) {
                throw new IllegalArgumentException(
                        "invalid classic kill/death state");
            }
        }
    }
    record ClassicKillDeathBalance(ClassicKillDeathState observedSource,
                                   ClassicKillDeathState effective) {
        ClassicKillDeathBalance {
            if (observedSource == null || effective == null) {
                throw new IllegalArgumentException(
                        "missing classic kill/death balance");
            }
        }
    }
    record ClassicPlayerKillMutation(
            ClassicKillDeathState attackerBefore,
            ClassicKillDeathState attackerAfter,
            ClassicKillDeathState victimBefore,
            ClassicKillDeathState victimAfter) {
        ClassicPlayerKillMutation {
            if (attackerBefore == null || attackerAfter == null
                    || victimBefore == null || victimAfter == null) {
                throw new IllegalArgumentException(
                        "missing classic player-kill mutation state");
            }
        }
    }
    record HellState(int remainingTicks) {
        HellState {
            if (remainingTicks < 0) {
                throw new IllegalArgumentException("invalid native HellTime");
            }
        }
    }
    record HellBalance(HellState observedSource, HellState effective) {
        HellBalance {
            if (observedSource == null || effective == null) {
                throw new IllegalArgumentException("missing native HellTime balance");
            }
        }
    }
    record ElfPkState(int count, long lastPkEpochSeconds) {
        ElfPkState {
            if (count < 0 || lastPkEpochSeconds < 0) {
                throw new IllegalArgumentException("invalid native elf-PK state");
            }
        }

        boolean wantedAt(long nowEpochSeconds) {
            return lastPkEpochSeconds > 0 && nowEpochSeconds >= lastPkEpochSeconds
                    && nowEpochSeconds - lastPkEpochSeconds
                        <= ClassicWorldGuardianCatalog.WANTED_SECONDS;
        }
    }
    record ElfPkBalance(ElfPkState observedSource, ElfPkState effective) {
        ElfPkBalance {
            if (observedSource == null || effective == null) {
                throw new IllegalArgumentException("missing native elf-PK balance");
            }
        }
    }
    record FactionKarmaBalance(int value) {
        FactionKarmaBalance {
            requireFactionKarma(value, "faction karma");
        }

        int level() { return factionKarmaLevel(value); }
        int percent() { return factionKarmaPercent(value); }
    }
    record FactionKarmaMutation(int before, int after, int requestedDelta) {
        FactionKarmaMutation {
            requireFactionKarma(before, "faction karma before mutation");
            requireFactionKarma(after, "faction karma after mutation");
        }

        boolean changed() { return before != after; }
        int appliedDelta() { return after - before; }
        int level() { return factionKarmaLevel(after); }
    }
    record ClassicExperienceBalance(
            long observedSourceExperience, int observedSourceLevel,
            long effectiveExperience, int effectiveLevel) {
        ClassicExperienceBalance {
            requireClassicExperience(observedSourceExperience, "observed source experience");
            requireClassicLevel(observedSourceLevel, "observed source level");
            requireClassicExperience(effectiveExperience, "effective experience");
            requireClassicLevel(effectiveLevel, "effective level");
        }
    }
    record ClassicExperienceMutation(
            long beforeExperience, int beforeLevel,
            long afterExperience, int afterLevel,
            long requestedBaseExperience, long adjustedExperience) {
        ClassicExperienceMutation {
            requireClassicExperience(beforeExperience, "experience before mutation");
            requireClassicLevel(beforeLevel, "level before mutation");
            requireClassicExperience(afterExperience, "experience after mutation");
            requireClassicLevel(afterLevel, "level after mutation");
            if (requestedBaseExperience < 0 || adjustedExperience < 0
                    || afterExperience < beforeExperience) {
                throw new IllegalArgumentException("invalid classic experience mutation");
            }
        }

        long appliedExperience() { return afterExperience - beforeExperience; }
        boolean changed() { return beforeExperience != afterExperience; }
        boolean levelChanged() { return beforeLevel != afterLevel; }
    }
    record ClassicDeathPenaltyMutation(
            long beforeExperience, int beforeLevel,
            long afterExperience, int afterLevel,
            long requestedPenalty) {
        ClassicDeathPenaltyMutation {
            requireClassicExperience(beforeExperience, "experience before death penalty");
            requireClassicLevel(beforeLevel, "level before death penalty");
            requireClassicExperience(afterExperience, "experience after death penalty");
            requireClassicLevel(afterLevel, "level after death penalty");
            if (requestedPenalty < 0 || afterExperience > beforeExperience) {
                throw new IllegalArgumentException("invalid classic death penalty mutation");
            }
        }

        long appliedPenalty() { return beforeExperience - afterExperience; }
        boolean changed() { return beforeExperience != afterExperience; }
        boolean levelChanged() { return beforeLevel != afterLevel; }
    }
    record TimedExperienceBuff(int bonusExperience, int remainingSeconds) {
        TimedExperienceBuff {
            if (bonusExperience < 0 || remainingSeconds < 0
                    || (bonusExperience == 0) != (remainingSeconds == 0)
                    || (bonusExperience > 0
                        && BattleExperienceGemCatalog.findByBonus(bonusExperience) == null)
                    || remainingSeconds > BattleExperienceGemCatalog.DURATION_SECONDS) {
                throw new IllegalArgumentException("invalid durable timed EXP buff");
            }
        }

        boolean active() { return bonusExperience > 0; }

        static TimedExperienceBuff none() {
            return new TimedExperienceBuff(0, 0);
        }
    }
    record TimedFireWeaponBuff(int remainingSeconds) {
        TimedFireWeaponBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Fire Weapon buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedFireWeaponBuff none() {
            return new TimedFireWeaponBuff(0);
        }
    }
    record TimedBurningWeaponBuff(int remainingSeconds) {
        TimedBurningWeaponBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Burning Weapon buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedBurningWeaponBuff none() {
            return new TimedBurningWeaponBuff(0);
        }
    }
    record TimedWindShotBuff(int remainingSeconds) {
        TimedWindShotBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Wind Shot buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedWindShotBuff none() {
            return new TimedWindShotBuff(0);
        }
    }
    record TimedFireBlessBuff(int remainingSeconds) {
        TimedFireBlessBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Fire Bless buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedFireBlessBuff none() {
            return new TimedFireBlessBuff(0);
        }
    }
    record TimedStormEyeBuff(int remainingSeconds) {
        TimedStormEyeBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Storm Eye buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedStormEyeBuff none() {
            return new TimedStormEyeBuff(0);
        }
    }
    record TimedStormShotBuff(int remainingSeconds) {
        TimedStormShotBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Storm Shot buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedStormShotBuff none() {
            return new TimedStormShotBuff(0);
        }
    }
    record TimedEarthBlessBuff(int remainingSeconds) {
        TimedEarthBlessBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Earth Bless buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedEarthBlessBuff none() {
            return new TimedEarthBlessBuff(0);
        }
    }
    record TimedIronSkinBuff(int remainingSeconds) {
        TimedIronSkinBuff {
            if (remainingSeconds < 0 || remainingSeconds > 960) {
                throw new IllegalArgumentException(
                        "invalid durable Iron Skin buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedIronSkinBuff none() {
            return new TimedIronSkinBuff(0);
        }
    }
    record TimedResistFearBuff(int remainingSeconds) {
        TimedResistFearBuff {
            if (remainingSeconds < 0 || remainingSeconds > 60) {
                throw new IllegalArgumentException(
                        "invalid durable Resist Fear buff");
            }
        }

        boolean active() { return remainingSeconds > 0; }

        static TimedResistFearBuff none() {
            return new TimedResistFearBuff(0);
        }
    }
    record TimedMovementBuff(int skillId, int remainingSeconds) {
        TimedMovementBuff {
            BattleSpellCatalog.MovementBuffSpell spell =
                    BattleSpellCatalog.movementBuffSpell(skillId);
            if (skillId == 0) {
                if (remainingSeconds != 0) {
                    throw new IllegalArgumentException(
                            "inactive durable movement buff has time");
                }
            } else if (spell == null || remainingSeconds <= 0
                    || remainingSeconds > spell.durationSeconds()) {
                throw new IllegalArgumentException(
                        "invalid durable movement buff");
            }
        }

        boolean active() { return skillId != 0; }

        static TimedMovementBuff none() {
            return new TimedMovementBuff(0, 0);
        }
    }
    record TimedArmorProtection(int skillId, int remainingSeconds) {
        TimedArmorProtection {
            BattleSpellCatalog.ShieldSpell spell =
                    BattleSpellCatalog.shieldSpell(skillId);
            if (skillId == 0) {
                if (remainingSeconds != 0) {
                    throw new IllegalArgumentException(
                            "inactive durable armor protection has time");
                }
            } else if (spell == null || remainingSeconds <= 0
                    || remainingSeconds > spell.durationSeconds()) {
                throw new IllegalArgumentException(
                        "invalid durable armor protection");
            }
        }

        boolean active() { return skillId != 0; }

        static TimedArmorProtection none() {
            return new TimedArmorProtection(0, 0);
        }
    }
    record TimedBuffSnapshot(
            TimedExperienceBuff experience,
            TimedFireWeaponBuff fireWeapon,
            TimedBurningWeaponBuff burningWeapon,
            TimedWindShotBuff windShot,
            TimedFireBlessBuff fireBless,
            TimedStormEyeBuff stormEye,
            TimedStormShotBuff stormShot,
            TimedEarthBlessBuff earthBless,
            TimedIronSkinBuff ironSkin,
            TimedResistFearBuff resistFear,
            TimedMovementBuff movement,
            TimedArmorProtection armorProtection) {
        TimedBuffSnapshot {
            if (experience == null || fireWeapon == null || burningWeapon == null
                    || windShot == null || fireBless == null || stormEye == null
                    || stormShot == null || earthBless == null || ironSkin == null
                    || resistFear == null || movement == null
                    || armorProtection == null) {
                throw new IllegalArgumentException("timed buff snapshot is incomplete");
            }
        }
    }

    @FunctionalInterface
    interface TimedBuffPersistenceHook {
        /** False means SQL is authoritative but intentionally read-only. */
        boolean persist(String character, TimedBuffSnapshot replacement)
                throws IOException;
    }
    record WaterDragonPassState(
            String sourceId, long itemObjectId, int showId, int roomNumber,
            String renterIdentity, long expiresAtEpochSeconds) {
        WaterDragonPassState {
            sourceId = normalizedSourceId(sourceId);
            renterIdentity = renterIdentity == null ? "" : renterIdentity.trim();
            ProfileIdentity renter = profileIdentity(renterIdentity);
            if (sourceId.isEmpty()
                    || itemObjectId <= 0 || itemObjectId > 0x7FFF_FFFFL
                    || showId <= 0
                    || roomNumber < 0
                    || roomNumber >= ClassicWorldWaterDragonCatalog.ROOM_COUNT
                    || renterIdentity.isEmpty()
                    || !renter.source().equalsIgnoreCase(sourceId)
                    || expiresAtEpochSeconds <= 0) {
                throw new IllegalArgumentException("invalid durable Water Dragon pass");
            }
        }

        WaterDragonPassState(
                long itemObjectId, int showId, int roomNumber,
                String renterIdentity, long expiresAtEpochSeconds) {
            this(profileIdentity(renterIdentity).source(), itemObjectId, showId,
                    roomNumber, renterIdentity, expiresAtEpochSeconds);
        }

        long remainingSeconds(long nowEpochSeconds) {
            return Math.max(0, expiresAtEpochSeconds - nowEpochSeconds);
        }
    }
    record HotelLeaseState(
            int showId, int npcId, int roomNumber, boolean meetingHall,
            String renterIdentity, long expiresAtEpochSeconds, boolean released) {
        HotelLeaseState {
            renterIdentity = renterIdentity == null ? "" : renterIdentity.trim();
            if (showId <= 0 || ClassicWorldHotelCatalog.forNpc(npcId) == null
                    || roomNumber < 0
                    || roomNumber >= ClassicWorldHotelCatalog.LEASE_SLOT_COUNT
                    || renterIdentity.isEmpty() || expiresAtEpochSeconds <= 0) {
                throw new IllegalArgumentException("invalid durable hotel lease");
            }
        }

        boolean active(long nowEpochSeconds) {
            return !released && expiresAtEpochSeconds > nowEpochSeconds;
        }

        HotelLeaseState markReleased() {
            return released ? this : new HotelLeaseState(
                    showId, npcId, roomNumber, meetingHall, renterIdentity,
                    expiresAtEpochSeconds, true);
        }
    }
    record HotelKeyState(
            long itemObjectId, int showId, int npcId, boolean meetingHall,
            long expiresAtEpochSeconds) {
        HotelKeyState {
            if (itemObjectId <= 0 || itemObjectId > 0x7FFF_FFFFL
                    || showId <= 0 || ClassicWorldHotelCatalog.forNpc(npcId) == null
                    || expiresAtEpochSeconds <= 0) {
                throw new IllegalArgumentException("invalid durable hotel key");
            }
        }
    }
    enum HotelRentStatus {
        RENTED, ALREADY_RENTED, ROOMS_FULL, HALLS_FULL, NO_FREE_SLOT
    }
    record HotelOccupancy(
            HotelLeaseState renterLease, int activeRooms, int activeHalls,
            int firstFreeSlot) {}
    record HotelRentMutation(HotelRentStatus status, HotelLeaseState lease) {
        boolean rented() { return status == HotelRentStatus.RENTED; }
    }
    @FunctionalInterface
    interface HotelInventoryPersistenceHook {
        /** SQL is authoritative; throwing refuses the in-memory lease publication. */
        void persist(HotelLeaseState lease) throws IOException;
    }
    @FunctionalInterface
    interface HotelReturnPersistenceHook {
        /** SQL is authoritative; throwing refuses the in-memory return publication. */
        void persist(HotelReturnQuote quote) throws IOException;
    }
    record HotelReturnQuote(
            int npcId, String renterIdentity, int refund,
            boolean releasesLease, Set<Long> returnedKeyObjectIds) {
        HotelReturnQuote {
            renterIdentity = renterIdentity == null ? "" : renterIdentity.trim();
            returnedKeyObjectIds = Set.copyOf(returnedKeyObjectIds);
            if (ClassicWorldHotelCatalog.forNpc(npcId) == null
                    || renterIdentity.isEmpty() || refund < 0) {
                throw new IllegalArgumentException("invalid hotel return quote");
            }
        }

        boolean present() {
            return releasesLease || !returnedKeyObjectIds.isEmpty();
        }
    }
    record PetState(
            String sourceKey, long collarObjectId, long objectId,
            int collarCatalogId, int npcId, String name, int level,
            int maxHp, int maxMp, long experience, int lawful,
            boolean litoOwned) {
        /** Format-40 compatibility before pet SQL ownership was explicit. */
        PetState(String sourceKey, long collarObjectId, long objectId,
                 int collarCatalogId, int npcId, String name, int level,
                 int maxHp, int maxMp, long experience, int lawful) {
            this(sourceKey, collarObjectId, objectId, collarCatalogId, npcId,
                    name, level, maxHp, maxMp, experience, lawful,
                    sourceKey == null || sourceKey.isBlank());
        }
        PetState {
            sourceKey = sourceKey == null ? "" : sourceKey.trim();
            if (collarObjectId <= 0 || collarObjectId > 0xFFFF_FFFFL
                    || objectId < FIRST_PET_OBJECT_ID || objectId > LAST_PET_OBJECT_ID
                    || (collarCatalogId != InventoryCatalog.PET_COLLAR
                        && collarCatalogId != InventoryCatalog.HIGH_PET_COLLAR)
                    || npcId <= 0 || name == null || name.isBlank()
                    || level <= 0 || level > 0xFF || maxHp <= 0 || maxMp < 0
                    || experience < 0 || experience > 0xFFFF_FFFFL
                    || lawful < Short.MIN_VALUE || lawful > Short.MAX_VALUE) {
                throw new IllegalArgumentException("invalid durable pet state");
            }
        }

        PetState withProgress(String newName, int newLevel, int newMaxHp,
                              int newMaxMp, long newExperience) {
            return new PetState(sourceKey, collarObjectId, objectId,
                    collarCatalogId, npcId, newName, newLevel,
                    newMaxHp, newMaxMp, newExperience, lawful, litoOwned);
        }

        long sourceCollarObjectId() {
            if (sourceKey.isBlank()) return 0;
            int separator = sourceKey.lastIndexOf(SOURCE_CHARACTER_SEPARATOR);
            if (separator < 0 || separator == sourceKey.length() - 1) return 0;
            try {
                long value = Long.parseLong(sourceKey.substring(separator + 1));
                return value > 0 && value <= 0xFFFF_FFFFL ? value : 0;
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        LitoPetPersistence persistence() {
            return new LitoPetPersistence(collarObjectId, objectId, collarCatalogId,
                    npcId, name, level, maxHp, maxMp, experience, lawful,
                    litoOwned, sourceCollarObjectId());
        }
    }
    record SourceProjectileBalance(
            long objectId, int itemId, long observedCount, long remainingCount) {
        SourceProjectileBalance {
            if (objectId <= 0 || objectId > 0xFFFF_FFFFL || itemId <= 0
                    || observedCount <= 0 || remainingCount < 0
                    || remainingCount > observedCount) {
                throw new IllegalArgumentException("invalid source projectile balance");
            }
        }

        boolean available() {
            return remainingCount > 0;
        }
    }
    record SourceProjectileReservation(
            boolean accepted, SourceProjectileBalance balance) {
        SourceProjectileReservation {
            if (balance == null || (accepted && balance.remainingCount() < 0)) {
                throw new IllegalArgumentException("invalid source projectile reservation");
            }
        }
    }
    record SourceWeaponDurabilityBalance(
            int itemId, int observedDurability, int effectiveDurability) {
        SourceWeaponDurabilityBalance {
            if (itemId <= 0 || observedDurability < 0 || observedDurability > 0x7F
                    || effectiveDurability < observedDurability
                    || effectiveDurability > 0x7F) {
                throw new IllegalArgumentException(
                        "invalid source weapon durability balance");
            }
        }
    }
    record MercenaryRoster(int type0, int type1, int type2, int type3) {
        MercenaryRoster {
            if (type0 < 0 || type0 > 0xFFFF || type1 < 0 || type1 > 0xFFFF
                    || type2 < 0 || type2 > 0xFFFF || type3 < 0 || type3 > 0xFFFF) {
                throw new IllegalArgumentException("invalid LiTo castle mercenary roster");
            }
        }

        int count(int entryId) {
            return switch (entryId) {
                case 0 -> type0;
                case 1 -> type1;
                case 2 -> type2;
                case 3 -> type3;
                default -> throw new IllegalArgumentException("invalid mercenary entry id");
            };
        }

        int total() {
            return Math.addExact(Math.addExact(type0, type1), Math.addExact(type2, type3));
        }

        MercenaryRoster add(int entryId, int quantity) {
            if (quantity <= 0) throw new IllegalArgumentException("invalid mercenary quantity");
            return switch (entryId) {
                case 0 -> new MercenaryRoster(Math.addExact(type0, quantity), type1, type2, type3);
                case 1 -> new MercenaryRoster(type0, Math.addExact(type1, quantity), type2, type3);
                case 2 -> new MercenaryRoster(type0, type1, Math.addExact(type2, quantity), type3);
                case 3 -> new MercenaryRoster(type0, type1, type2, Math.addExact(type3, quantity));
                default -> throw new IllegalArgumentException("invalid mercenary entry id");
            };
        }

        MercenaryRoster subtract(int entryId, int quantity) {
            if (quantity <= 0 || quantity > count(entryId)) {
                throw new IllegalArgumentException("invalid mercenary roster subtraction");
            }
            return switch (entryId) {
                case 0 -> new MercenaryRoster(type0 - quantity, type1, type2, type3);
                case 1 -> new MercenaryRoster(type0, type1 - quantity, type2, type3);
                case 2 -> new MercenaryRoster(type0, type1, type2 - quantity, type3);
                case 3 -> new MercenaryRoster(type0, type1, type2, type3 - quantity);
                default -> throw new IllegalArgumentException("unknown mercenary entry id");
            };
        }
    }
    record MercenaryAssignment(int castleId, MercenaryRoster mercenaries) {
        MercenaryAssignment {
            if (castleId < 0 || mercenaries == null
                    || (castleId == 0 && mercenaries.total() != 0)) {
                throw new IllegalArgumentException("invalid character mercenary assignment");
            }
        }

        static MercenaryAssignment empty() {
            return new MercenaryAssignment(0, new MercenaryRoster(0, 0, 0, 0));
        }
    }
    record CastleState(String sourceId, int castleId, int taxRate, long publicMoney,
                       long warTimeMillis, boolean securityEnabled,
                       MercenaryRoster mercenaries, long revision) {
        CastleState {
            sourceId = normalizedCastleSource(sourceId);
            if (castleId <= 0 || taxRate < 10 || taxRate > 50
                    || publicMoney < 0 || publicMoney > Integer.MAX_VALUE
                    || warTimeMillis < 0
                    || mercenaries == null || revision <= 0) {
                throw new IllegalArgumentException("invalid LiTo castle state");
            }
        }

        CastleState(int castleId, int taxRate, long publicMoney,
                    boolean securityEnabled, MercenaryRoster mercenaries,
                    long revision) {
            this("", castleId, taxRate, publicMoney, 0,
                    securityEnabled, mercenaries, revision);
        }
    }
    enum CastleSecuritySettlementStatus {
        READY, INSUFFICIENT_FUNDS, ALREADY_SETTLED, DISABLED, NOT_FOUND,
        CONFLICT
    }
    record CastleSecuritySettlement(
            CastleSecuritySettlementStatus status, CastleState snapshot,
            long warStartMillis, long chargedAdena) {
        CastleSecuritySettlement {
            if (status == null || warStartMillis <= 0 || chargedAdena < 0
                    || chargedAdena > Integer.MAX_VALUE
                    || (status == CastleSecuritySettlementStatus.READY
                            && (snapshot == null || chargedAdena == 0))) {
                throw new IllegalArgumentException(
                        "invalid sidecar castle security settlement");
            }
        }
    }
    /** Durable owner override; the selected clan/castle tables remain read-only. */
    record CastleOwnershipState(String sourceId, int castleId, String clanName,
                                String leaderName, long leaderObjectId,
                                long revision) {
        CastleOwnershipState {
            sourceId = normalizedCastleSource(sourceId);
            clanName = clanName == null ? "" : clanName.trim();
            leaderName = leaderName == null ? "" : leaderName.trim();
            if (castleId < 1 || castleId > 8 || clanName.isEmpty()
                    || leaderName.isEmpty() || leaderObjectId <= 0
                    || leaderObjectId > 0xFFFF_FFFFL || revision <= 0) {
                throw new IllegalArgumentException("invalid LiTo castle ownership");
            }
        }
    }
    enum WarehouseScope { PERSONAL, CLAN, ELF }
    enum WarehousePasswordStatus { CHANGED, WRONG_OLD, UNCHANGED }
    record Progress(long shopPoints, long rankingPoints,
                    int wins, int losses, int disconnects,
                    long experience, int level) {}
    record TournamentProfileState(
            long rankingPoints, int wins, int losses, int disconnects,
            int monthlyPeriod, long monthlyRankingPoints,
            int monthlyWins, int monthlyLosses,
            List<Integer> legacyCertificateCodes,
            List<Integer> victoryBadgeEditions, boolean litoMaster,
            String classicServerName, int channel, int sourceLevel, int race,
            boolean rankingSnapshotPresent, long snapshotRankingPoints,
            int snapshotWins, int snapshotLosses,
            String snapshotClassicServerName, int snapshotChannel,
            int snapshotSourceLevel, int snapshotRace,
            boolean monthlyRankingSnapshotPresent,
            int snapshotMonthlyPeriod, long snapshotMonthlyRankingPoints,
            int snapshotMonthlyWins, int snapshotMonthlyLosses,
            int snapshotMonthlyMovement, int snapshotOverallMovement,
            int snapshotClassicServerMovement, int snapshotChannelMovement,
            int snapshotLevelMovement, int snapshotRaceMovement,
            long experience, int level,
            long rankingSnapshotBoundaryEpochSeconds,
            long monthlyRankingSnapshotBoundaryEpochSeconds) {
        TournamentProfileState {
            legacyCertificateCodes = legacyCertificateCodes == null
                    ? List.of() : List.copyOf(legacyCertificateCodes);
            victoryBadgeEditions = victoryBadgeEditions == null
                    ? List.of() : List.copyOf(victoryBadgeEditions);
            classicServerName = classicServerName == null ? "" : classicServerName.trim();
            snapshotClassicServerName = snapshotClassicServerName == null
                    ? "" : snapshotClassicServerName.trim();
            if (rankingPoints < 0 || wins < 0 || losses < 0 || disconnects < 0
                    || disconnects > losses
                    || (long) wins + losses > Integer.MAX_VALUE
                    || monthlyRankingPoints < 0
                    || monthlyWins < 0 || monthlyLosses < 0
                    || legacyCertificateCodes.size() != TOURNAMENT_LEGACY_CERTIFICATE_SLOTS
                    || victoryBadgeEditions.size() != TOURNAMENT_VICTORY_BADGE_MODE_COUNT
                    || legacyCertificateCodes.stream().anyMatch(
                            value -> value == null || !validTournamentLegacyCertificate(value))
                    || victoryBadgeEditions.stream().anyMatch(
                            value -> value == null || value < 0 || value > 0x7F)
                    || classicServerName.length() > 63
                    || snapshotClassicServerName.length() > 63
                    || channel < -1 || channel > 4 || snapshotChannel < -1
                    || snapshotChannel > 4 || sourceLevel < -1 || sourceLevel > 0xFF
                    || snapshotSourceLevel < -1 || snapshotSourceLevel > 0xFF
                    || race < -1 || race > 6 || snapshotRace < -1 || snapshotRace > 6
                    || snapshotRankingPoints < 0 || snapshotWins < 0
                    || snapshotLosses < 0 || snapshotMonthlyRankingPoints < 0
                    || snapshotMonthlyWins < 0 || snapshotMonthlyLosses < 0
                    || experience < 0 || level < 1 || level > 99
                    || rankingSnapshotBoundaryEpochSeconds < 0
                    || monthlyRankingSnapshotBoundaryEpochSeconds < 0
                    || monthlyPeriod != 0 && !validRankingPeriod(monthlyPeriod)
                    || snapshotMonthlyPeriod != 0
                            && !validRankingPeriod(snapshotMonthlyPeriod)) {
                throw new IllegalArgumentException("invalid Tournament profile state");
            }
        }
    }

    @FunctionalInterface
    interface TournamentProfilePersistenceHook {
        void persist(TournamentProfileState expected,
                     TournamentProfileState replacement) throws IOException;
    }
    record TournamentStatistics(int winRateDenominator, int wins, int losses,
                                int disconnects) {
        TournamentStatistics {
            if (winRateDenominator < 0 || wins < 0 || losses < 0 || disconnects < 0
                    || disconnects > losses
                    || (long) wins + losses != winRateDenominator) {
                throw new IllegalArgumentException("invalid Tournament statistics");
            }
        }
    }
    static final int TOURNAMENT_LEGACY_CERTIFICATE_SLOTS = 12;
    static final int TOURNAMENT_VICTORY_BADGE_MODE_COUNT = 5;

    /**
     * Server-awarded Tournament honours rendered by the selected S_CD client.
     *
     * <p>Each legacy byte is either zero or {@code generation << 4 | material},
     * where the only native material tags are 1=Mithril, 2=Gold and 4=Silver.
     * The five newer bytes are externally awarded edition numbers for Death
     * Match, Honor Match, Quest Battle, Time Attack and Siege War. They are
     * not ordinary per-mode win counters: the selected client labels them as
     * numbered event editions, and no recovered result packet mutates them.
     * The final dword's only proved consumer is bit zero, the first LiTo Master
     * badge. Award assignment therefore remains an explicit backend/import
     * operation rather than a guessed consequence of an ordinary victory.</p>
     */
    record TournamentAwards(List<Integer> legacyCertificateCodes,
                            List<Integer> victoryBadgeEditions,
                            boolean litoMaster) {
        TournamentAwards {
            if (legacyCertificateCodes == null
                    || legacyCertificateCodes.size()
                        != TOURNAMENT_LEGACY_CERTIFICATE_SLOTS
                    || victoryBadgeEditions == null
                    || victoryBadgeEditions.size()
                        != TOURNAMENT_VICTORY_BADGE_MODE_COUNT) {
                throw new IllegalArgumentException("invalid Tournament award slot count");
            }
            legacyCertificateCodes = List.copyOf(legacyCertificateCodes);
            victoryBadgeEditions = List.copyOf(victoryBadgeEditions);
            for (Integer code : legacyCertificateCodes) {
                if (code == null || !validTournamentLegacyCertificate(code)) {
                    throw new IllegalArgumentException(
                            "invalid Tournament legacy certificate code");
                }
            }
            for (Integer edition : victoryBadgeEditions) {
                // sub_459AE0 uses a signed-byte >0 gate before formatting it.
                if (edition == null || edition < 0 || edition > 0x7F) {
                    throw new IllegalArgumentException(
                            "invalid Tournament victory-badge edition");
                }
            }
        }

        static TournamentAwards none() {
            return new TournamentAwards(
                    java.util.Collections.nCopies(
                            TOURNAMENT_LEGACY_CERTIFICATE_SLOTS, 0),
                    java.util.Collections.nCopies(
                            TOURNAMENT_VICTORY_BADGE_MODE_COUNT, 0), false);
        }
    }
    /** Exact current-version S_D9 row; displayed rank itself is the list position. */
    record Ranking(String name, int classIcon, int wins, int losses,
                   int rankMovement) {}
    record RankingQuery(String classicServerName, int channel, int level, int race) {
        RankingQuery {
            classicServerName = classicServerName == null ? "" : classicServerName.trim();
        }
    }
    record Bookmark(String name, int x, int y, int mapId) {
        Bookmark {
            name = name == null ? "" : name.trim();
            if (name.isBlank() || name.length() > 45 || x < 0 || x > 0xFFFF
                    || y < 0 || y > 0xFFFF || mapId < 0 || mapId > 0xFFFF) {
                throw new IllegalArgumentException("invalid durable bookmark");
            }
        }
    }
    record BoardTopic(String sourceId, long id, String author, String authorIdentity,
                      String date, String title, String content) {
        BoardTopic {
            sourceId = normalizedSourceId(sourceId);
            author = author == null ? "" : author;
            authorIdentity = authorIdentity == null || authorIdentity.isBlank()
                    ? author : authorIdentity;
        }

        BoardTopic(long id, String author, String authorIdentity,
                   String date, String title, String content) {
            this(profileIdentity(authorIdentity).source(), id, author,
                    authorIdentity, date, title, content);
        }

        BoardTopic(long id, String author, String date, String title, String content) {
            this("", id, author, author, date, title, content);
        }

        BoardTopic withSource(String sourceId) {
            return new BoardTopic(sourceId, id, author, authorIdentity,
                    date, title, content);
        }
    }
    record Letter(long id, int code, String sender, String receiver, String date,
                  int templateId, String subject, String content, boolean opened) {
        Letter {
            if (id <= 0 || code < 0 || code > 0xFFFF
                    || templateId < 0 || templateId > 0xFF) {
                throw new IllegalArgumentException("invalid durable physical letter identity");
            }
            // Reuse the exact recovered receiver contract as the durable-data
            // validator. The runtime object id is assigned separately.
            Protocol.readMail(1, InventoryCatalog.LETTER_OPEN_INVENTORY_GFX,
                    code, sender, receiver, subject, content, templateId, date);
        }

        Letter openedCopy() {
            return opened ? this : new Letter(id, code, sender, receiver, date,
                    templateId, subject, content, true);
        }
    }
    enum LetterDeliveryStatus { DELIVERED, NO_PAPER, RECIPIENT_FULL }
    record LetterDelivery(LetterDeliveryStatus status, Letter letter,
                          Map<Long, Integer> senderInventory,
                          Map<Long, Integer> receiverInventory) {
        LetterDelivery {
            senderInventory = senderInventory == null ? Map.of() : Map.copyOf(senderInventory);
            receiverInventory = receiverInventory == null ? Map.of() : Map.copyOf(receiverInventory);
        }

        boolean delivered() { return status == LetterDeliveryStatus.DELIVERED; }
    }
    enum LetterOpenStatus { OPENED, ALREADY_OPEN, NOT_FOUND, INVENTORY_CHANGED }
    record LetterOpenMutation(LetterOpenStatus status, Letter letter,
                              Map<Long, Integer> inventory) {
        LetterOpenMutation {
            inventory = inventory == null ? Map.of() : Map.copyOf(inventory);
        }
    }
    record LetterDeleteMutation(boolean deleted, Map<Long, Integer> inventory) {
        LetterDeleteMutation {
            inventory = inventory == null ? Map.of() : Map.copyOf(inventory);
        }
    }
    record HouseAuction(String sourceId, long houseId, String name, int area,
                        long deadlineEpochSeconds,
                        long price, String location, String oldOwner,
                        long oldOwnerObjectId, String bidder,
                        long bidderObjectId, long revision) {
        HouseAuction {
            sourceId = normalizedSourceId(sourceId);
            if (oldOwnerObjectId < 0 || oldOwnerObjectId > 0xFFFF_FFFFL) {
                throw new IllegalArgumentException("invalid old house owner object id");
            }
        }

        HouseAuction(String sourceId, long houseId, String name, int area,
                     long deadlineEpochSeconds, long price, String location,
                     String oldOwner, String bidder, long bidderObjectId,
                     long revision) {
            this(sourceId, houseId, name, area, deadlineEpochSeconds, price,
                    location, oldOwner, 0, bidder, bidderObjectId, revision);
        }

        HouseAuction(long houseId, String name, int area, long deadlineEpochSeconds,
                     long price, String location, String oldOwner,
                     String bidder, long bidderObjectId, long revision) {
            this("", houseId, name, area, deadlineEpochSeconds, price,
                    location, oldOwner, 0, bidder, bidderObjectId, revision);
        }
    }
    enum HouseBidStatus {
        ACCEPTED, NOT_FOUND, STALE, EXPIRED, ALREADY_BIDDER,
        INVALID_AMOUNT, INVENTORY_CHANGED, REFUND_OVERFLOW
    }
    record HouseBidMutation(HouseBidStatus status, HouseAuction auction,
                            String previousBidder, String previousBidderIdentity,
                            long previousPrice,
                            Map<Long, Integer> previousBidderInventory) {
        HouseBidMutation {
            previousBidder = previousBidder == null ? "" : previousBidder;
            previousBidderIdentity = previousBidderIdentity == null
                    ? "" : previousBidderIdentity;
            previousBidderInventory = previousBidderInventory == null
                    ? Map.of() : Map.copyOf(previousBidderInventory);
        }

        boolean accepted() { return status == HouseBidStatus.ACCEPTED; }
    }
    enum MarriageStatus { MARRIED, FIRST_ALREADY_MARRIED, SECOND_ALREADY_MARRIED, INVALID }
    record MarriageMutation(MarriageStatus status, String first, String second) {
        MarriageMutation {
            first = first == null ? "" : first;
            second = second == null ? "" : second;
        }
    }
    record DivorceMutation(boolean divorced, String partner) {
        DivorceMutation {
            partner = partner == null ? "" : partner;
        }
    }
    enum CreationFailure { NONE, DUPLICATE_NAME, ACCOUNT_LIMIT }
    record CharacterCreation(CharacterSummary character, CreationFailure failure) {
        boolean accepted() { return character != null && failure == CreationFailure.NONE; }
    }
    enum DeletionStatus { DELETED, NOT_FOUND, NOT_OWNED }
    record CharacterDeletion(CharacterSummary character, DeletionStatus status) {
        boolean deleted() { return character != null && status == DeletionStatus.DELETED; }
    }

    private static final long INITIAL_POINTS = 5_000;
    /** Official guides exclude characters with fewer than twenty decided matches. */
    static final int MINIMUM_RANKED_MATCHES = 20;
    static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Taipei");
    private static final LocalTime RANKING_REFRESH_TIME = LocalTime.of(5, 0);
    private static final Map<Long, Integer> INITIAL_INVENTORY = Map.ofEntries(
            Map.entry(InventoryCatalog.ARENA_TOKEN, 1),
            Map.entry(InventoryCatalog.MAGIC_LANTERN, 1),
            Map.entry(InventoryCatalog.SELF_HASTE_POTION, 1),
            Map.entry(InventoryCatalog.BRAVE_POTION, 1),
            Map.entry(InventoryCatalog.CURE_POISON_POTION, 1),
            Map.entry(InventoryCatalog.BLINDNESS_POTION, 1),
            Map.entry(InventoryCatalog.BLESS_OF_EVA, 2),
            // Retained Dragon Gem fixed-EXP paths remain reachable in an
            // isolated LiTo sidecar without writing the selected source DB.
            Map.entry(InventoryCatalog.DRAGON_DIAMOND, 1),
            Map.entry(InventoryCatalog.DRAGON_SAPPHIRE, 1),
            Map.entry(InventoryCatalog.DRAGON_RUBY, 1),
            // Retained C_GIVE-driven two-stage pet evolution fixtures.
            Map.entry(InventoryCatalog.EVOLUTION_FRUIT, 1),
            Map.entry(InventoryCatalog.VICTORY_FRUIT, 1),
            // Isolated/fresh LiTo profiles expose all exact reusable
            // 42518..42524 GenericBuffItem paths without mutating a legacy DB.
            Map.entry(InventoryCatalog.EARTH_DRAGON_EYE, 1),
            Map.entry(InventoryCatalog.WATER_DRAGON_EYE, 1),
            Map.entry(InventoryCatalog.WIND_DRAGON_EYE, 1),
            Map.entry(InventoryCatalog.FIRE_DRAGON_EYE, 1),
            Map.entry(InventoryCatalog.BIRTH_DRAGON_EYE, 1),
            Map.entry(InventoryCatalog.IMAGE_DRAGON_EYE, 1),
            Map.entry(InventoryCatalog.LIFE_DRAGON_EYE, 1),
            Map.entry(InventoryCatalog.WHISTLE, 1),
            // Sidecar/native-flow fixture for exact Pet_Collar consumption;
            // the retained legacy databases remain strictly read-only.
            Map.entry(InventoryCatalog.PET_CALLING_FLUTE, 8),
            Map.entry(InventoryCatalog.LIQUOR, 1),
            Map.entry(InventoryCatalog.POLYMORPH_SCROLL, 2),
            // The classic beginner table grants 20 appraisal scrolls. Keep
            // that native selector flow reachable in isolated LiTo profiles.
            Map.entry(InventoryCatalog.APPRAISAL_SCROLL, 20),
            Map.entry(InventoryCatalog.WORLD_MAP, 1),
            Map.entry(InventoryCatalog.MAGIC_GEM, 3),
            Map.entry(InventoryCatalog.SPIRIT_CRYSTAL, 3),
            // New sidecar profiles receive enough native currency to exercise
            // one standard clan creation without altering Tournament points.
            Map.entry(InventoryCatalog.ADENA, 100_000));
    private static final long FIRST_CREATED_OBJECT_ID = 0x6000_0000L;
    private static final long FIRST_PET_OBJECT_ID = 0x9000_0000L;
    private static final long LAST_PET_OBJECT_ID = 0x90FF_FFFFL;
    private static final int MAX_CHARACTERS_PER_ACCOUNT = 8;
    private static final long INITIAL_HOUSE_ID = 262_145L;
    private static final long INITIAL_HOUSE_PRICE = 100_000L;
    private static final long INITIAL_HOUSE_AUCTION_SECONDS = 5L * 24L * 60L * 60L;
    private static final char SOURCE_CHARACTER_SEPARATOR = '\u001F';

    private final Path file;
    private final boolean persistent;
    /**
     * Read-only shared-database sessions are working sets, not a second
     * character database. Source-qualified profiles and their pets must never
     * survive in the LiTo sidecar after the owning session leaves.
     */
    private final boolean ephemeralSourceProfiles;
    private final Clock clock;
    private final Map<String, MutableProfile> profiles = new LinkedHashMap<>();
    private final Map<String, TimedBuffPersistenceHook> timedBuffPersistenceHooks =
            new LinkedHashMap<>();
    private final Map<String, Map<Long, Integer>> accountWarehouses = new LinkedHashMap<>();
    private final Map<String, Map<Long, Integer>> clanWarehouses = new LinkedHashMap<>();
    private final Map<String, Map<Long, Integer>> elfWarehouses = new LinkedHashMap<>();
    private final Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>>
            accountWarehouseItemStates = new LinkedHashMap<>();
    private final Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>>
            clanWarehouseItemStates = new LinkedHashMap<>();
    private final Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>>
            elfWarehouseItemStates = new LinkedHashMap<>();
    private final Map<String, Integer> warehousePasswords = new LinkedHashMap<>();
    private final Map<String, MutableCastle> castles = new LinkedHashMap<>();
    private final Map<String, CastleSecurityLedger> castleSecuritySettlements =
            new LinkedHashMap<>();
    private final Map<String, MutableCastleOwnership> castleOwnerships =
            new LinkedHashMap<>();
    private final Map<String, List<CharacterSummary>> createdCharacters = new LinkedHashMap<>();
    private final Set<String> bannedAccounts = new LinkedHashSet<>();
    private final Map<String, BoardTopic> boardTopics = new LinkedHashMap<>();
    private final Map<String, MutableHouseAuction> houseAuctions = new LinkedHashMap<>();
    private final Map<Long, PetState> pets = new LinkedHashMap<>();
    private final Map<String, WaterDragonPassState> waterDragonPasses =
            new LinkedHashMap<>();
    private final Map<Integer, HotelLeaseState> hotelLeases = new LinkedHashMap<>();
    private final Map<Long, HotelKeyState> hotelKeys = new LinkedHashMap<>();
    private final Set<String> importedSourcePetKeys = new LinkedHashSet<>();
    private long nextCreatedObjectId = FIRST_CREATED_OBJECT_ID;
    private long nextPetObjectId = FIRST_PET_OBJECT_ID;
    private final Map<String, Long> nextBoardTopicIds = new LinkedHashMap<>();
    private long nextLetterId = 1;
    private long rankingSnapshotBoundaryEpochSeconds;
    private long monthlyRankingSnapshotBoundaryEpochSeconds;

    GameStateStore(Path file, boolean persistent) throws IOException {
        this(file, persistent, Clock.system(RANKING_ZONE), false);
    }

    GameStateStore(Path file, boolean persistent, boolean ephemeralSourceProfiles)
            throws IOException {
        this(file, persistent, Clock.system(RANKING_ZONE),
                ephemeralSourceProfiles);
    }

    GameStateStore(Path file, boolean persistent, Clock clock) throws IOException {
        this(file, persistent, clock, false);
    }

    GameStateStore(Path file, boolean persistent, Clock clock,
                   boolean ephemeralSourceProfiles) throws IOException {
        this.file = file;
        this.persistent = persistent;
        this.ephemeralSourceProfiles = ephemeralSourceProfiles;
        this.clock = clock == null ? Clock.system(RANKING_ZONE) : clock;
        if (persistent) load();
        // Keep the original deterministic development ranking populated even
        // before both local test accounts have logged in.
        if (profiles.values().stream().noneMatch(
                profile -> profile.name.equalsIgnoreCase("TestKnight"))) {
            profiles.put(key("TestKnight"),
                    new MutableProfile("TestKnight", INITIAL_POINTS, 10, 4,
                            1_200, 1, INITIAL_INVENTORY));
        }
        if (profiles.values().stream().noneMatch(
                profile -> profile.name.equalsIgnoreCase("TestMage"))) {
            profiles.put(key("TestMage"),
                    new MutableProfile("TestMage", INITIAL_POINTS, 8, 5,
                            950, 1, INITIAL_INVENTORY));
        }
        if (houseAuctions.isEmpty()) ensureHouseAuctionSeed("");
        if (persistent) save();
    }

    synchronized Economy economy(String character) {
        MutableProfile profile = profile(character);
        return new Economy(profile.points, Map.copyOf(profile.inventory),
                normalizedItemStates(profile.inventoryItemStates, profile.inventory));
    }

    /**
     * Replaces only non-source-bound durable objects with the authoritative
     * LiTo-private SQL snapshot. Shared character_items rows have already been
     * reconciled as source-bound objects and are deliberately preserved.
     */
    synchronized Economy reconcileSqlPrivateInventory(
            String character, List<LitoOwnedInventoryItem> privateItems)
            throws IOException {
        List<LitoOwnedInventoryItem> rows = privateItems == null
                ? List.of() : List.copyOf(privateItems);
        MutableProfile profile = profile(character);
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        RuntimeInventory staged = RuntimeInventory.fromDurableState(
                profile.inventory, profile.inventoryItemStates);
        for (RuntimeInventory.Item item : List.copyOf(staged.items())) {
            if (!item.sourceBound()) staged.removeEntireForDeath(item.objectId());
        }
        for (LitoOwnedInventoryItem row : rows) {
            if (row == null) {
                throw new IOException("LiTo-private SQL inventory contains a null row");
            }
            InventoryCatalog.Definition definition = InventoryCatalog.find(row.itemId());
            if (definition == null) {
                throw new IOException("LiTo-private SQL inventory has no runtime template: "
                        + row.itemId());
            }
            long admitted = staged.addDistinctState(definition,
                    new RuntimeInventory.PersistentItem(
                            row.objectId(), row.quantity(), row.enchantLevel(),
                            row.durability(), row.identified(), row.blessing(),
                            false, row.tournamentReward(), row.equipped(), null,
                            row.chargeCount(), row.expiresAtEpochMillis(),
                            row.sealed(), false));
            if (admitted != row.objectId()) {
                throw new IOException("LiTo-private SQL inventory object collision/capacity: "
                        + row.objectId());
            }
        }
        replacePositiveCounts(profile.inventory, staged.catalogCounts());
        replaceItemStates(profile.inventoryItemStates, staged.catalogItemStates());
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            throw failure;
        }
        return economy(character);
    }

    synchronized long currentEpochSeconds() {
        return clock.instant().getEpochSecond();
    }

    synchronized List<WaterDragonPassState> activeWaterDragonPasses()
            throws IOException {
        if (pruneExpiredWaterDragonPasses()) save();
        return List.copyOf(waterDragonPasses.values());
    }

    synchronized List<WaterDragonPassState> activeWaterDragonPasses(String sourceId)
            throws IOException {
        String source = normalizedSourceId(sourceId);
        if (pruneExpiredWaterDragonPasses()) save();
        return waterDragonPasses.values().stream()
                .filter(pass -> pass.sourceId().equalsIgnoreCase(source))
                .toList();
    }

    synchronized void saveEconomyStateAndWaterDragonPass(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            long itemObjectId, int showId, int roomNumber,
            String renterIdentity) throws IOException {
        long now = currentEpochSeconds();
        long duration = ClassicWorldWaterDragonCatalog.PASS_RENTAL_SECONDS;
        long expiry = now > Long.MAX_VALUE - duration
                ? Long.MAX_VALUE : now + duration;
        saveEconomyStateAndWaterDragonPass(
                profileIdentity(renterIdentity).source(), character, points,
                inventory, itemStates, itemObjectId, showId, roomNumber,
                renterIdentity, expiry);
    }

    synchronized void saveEconomyStateAndWaterDragonPass(
            String sourceId, String character, long points,
            Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            long itemObjectId, int showId, int roomNumber,
            String renterIdentity, long expiresAtEpochSeconds) throws IOException {
        if (inventory == null || itemStates == null) {
            throw new IllegalArgumentException("inventory and item states are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<String, WaterDragonPassState> previousPasses =
                new LinkedHashMap<>(waterDragonPasses);
        pruneExpiredWaterDragonPasses();
        String source = normalizedSourceId(sourceId);
        String storageKey = sourceScopedId(source, itemObjectId);
        boolean roomOccupied = waterDragonPasses.values().stream()
                .anyMatch(pass -> pass.sourceId().equalsIgnoreCase(source)
                        && (pass.roomNumber() == roomNumber
                                && pass.itemObjectId() != itemObjectId
                            || pass.renterIdentity().equalsIgnoreCase(renterIdentity)));
        if (roomOccupied || waterDragonPasses.containsKey(storageKey)
                || expiresAtEpochSeconds <= currentEpochSeconds()) {
            throw new IllegalStateException("Water Dragon pass room is no longer available");
        }
        WaterDragonPassState pass = new WaterDragonPassState(
                source, itemObjectId, showId, roomNumber, renterIdentity,
                expiresAtEpochSeconds);
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        waterDragonPasses.put(storageKey, pass);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            waterDragonPasses.clear();
            waterDragonPasses.putAll(previousPasses);
            throw failure;
        }
    }

    synchronized void cancelWaterDragonPass(long itemObjectId) throws IOException {
        boolean removed = waterDragonPasses.values().removeIf(
                pass -> pass.itemObjectId() == itemObjectId);
        if (removed) save();
    }

    synchronized void cancelWaterDragonPass(String sourceId, long itemObjectId)
            throws IOException {
        if (waterDragonPasses.remove(sourceScopedId(sourceId, itemObjectId)) != null) save();
    }

    synchronized int waterDragonBloodRemainingSeconds(String character) {
        MutableProfile profile = profile(character);
        long remaining = Math.max(0,
                profile.waterDragonBloodExpiresAtEpochSeconds
                        - currentEpochSeconds());
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    synchronized void saveWaterDragonBlood(
            String character, int durationSeconds) throws IOException {
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("negative Water Dragon blood duration");
        }
        MutableProfile profile = profile(character);
        long previous = profile.waterDragonBloodExpiresAtEpochSeconds;
        long now = currentEpochSeconds();
        profile.waterDragonBloodExpiresAtEpochSeconds = durationSeconds == 0
                ? 0 : now > Long.MAX_VALUE - durationSeconds
                        ? Long.MAX_VALUE : now + durationSeconds;
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.waterDragonBloodExpiresAtEpochSeconds = previous;
            throw failure;
        }
    }

    private boolean pruneExpiredWaterDragonPasses() {
        long now = currentEpochSeconds();
        return waterDragonPasses.values().removeIf(
                pass -> pass.remainingSeconds(now) <= 0);
    }

    synchronized HotelOccupancy hotelOccupancy(int npcId, String renterIdentity) {
        if (ClassicWorldHotelCatalog.forNpc(npcId) == null) {
            throw new IllegalArgumentException("unknown hotel innkeeper");
        }
        String identity = renterIdentity == null ? "" : renterIdentity.trim();
        long now = currentEpochSeconds();
        HotelLeaseState renterLease = null;
        int rooms = 0;
        int halls = 0;
        boolean[] occupied = new boolean[ClassicWorldHotelCatalog.LEASE_SLOT_COUNT];
        for (HotelLeaseState lease : hotelLeases.values()) {
            if (lease.npcId() != npcId || !lease.active(now)) continue;
            occupied[lease.roomNumber()] = true;
            if (lease.meetingHall()) halls++;
            else rooms++;
            if (lease.renterIdentity().equalsIgnoreCase(identity)) {
                renterLease = lease;
            }
        }
        int free = -1;
        for (int room = 0; room < occupied.length; room++) {
            if (!occupied[room]) {
                free = room;
                break;
            }
        }
        return new HotelOccupancy(renterLease, rooms, halls, free);
    }

    synchronized HotelLeaseState activeHotelLeaseForInventory(
            int npcId, Set<Long> inventoryObjectIds) {
        if (inventoryObjectIds == null || inventoryObjectIds.isEmpty()) return null;
        long now = currentEpochSeconds();
        for (Long objectId : inventoryObjectIds) {
            HotelKeyState keyState = objectId == null ? null : hotelKeys.get(objectId);
            if (keyState == null || keyState.npcId() != npcId) continue;
            HotelLeaseState lease = hotelLeases.get(keyState.showId());
            if (lease != null && lease.npcId() == npcId
                    && lease.showId() == keyState.showId()
                    && lease.meetingHall() == keyState.meetingHall()
                    && lease.expiresAtEpochSeconds()
                        == keyState.expiresAtEpochSeconds()
                    && lease.active(now)) {
                return lease;
            }
        }
        return null;
    }

    synchronized HotelRentMutation saveEconomyStateAndHotelLease(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            int npcId, boolean meetingHall, String renterIdentity,
            List<Long> keyObjectIds) throws IOException {
        return saveEconomyStateAndHotelLease(character, points, inventory,
                itemStates, npcId, meetingHall, renterIdentity, keyObjectIds, null);
    }

    synchronized HotelRentMutation saveEconomyStateAndHotelLease(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            int npcId, boolean meetingHall, String renterIdentity,
            List<Long> keyObjectIds,
            HotelInventoryPersistenceHook sqlPersistence) throws IOException {
        if (inventory == null || itemStates == null || keyObjectIds == null
                || keyObjectIds.isEmpty()
                || keyObjectIds.size()
                    > ClassicWorldHotelCatalog.maximumKeys(meetingHall)) {
            throw new IllegalArgumentException("invalid hotel rental inventory");
        }
        LinkedHashSet<Long> distinctKeys = new LinkedHashSet<>(keyObjectIds);
        if (distinctKeys.size() != keyObjectIds.size()
                || distinctKeys.stream().anyMatch(id -> id == null || id <= 0
                        || id > 0x7FFF_FFFFL || hotelKeys.containsKey(id))) {
            throw new IllegalArgumentException("invalid or reused hotel key object id");
        }
        String identity = renterIdentity == null ? "" : renterIdentity.trim();
        if (identity.isEmpty()) {
            throw new IllegalArgumentException("hotel renter identity is required");
        }
        HotelOccupancy occupancy = hotelOccupancy(npcId, identity);
        if (occupancy.renterLease() != null) {
            return new HotelRentMutation(
                    HotelRentStatus.ALREADY_RENTED, occupancy.renterLease());
        }
        if (!meetingHall && occupancy.activeRooms()
                >= ClassicWorldHotelCatalog.ROOM_CAPACITY) {
            return new HotelRentMutation(HotelRentStatus.ROOMS_FULL, null);
        }
        if (meetingHall && occupancy.activeHalls()
                >= ClassicWorldHotelCatalog.HALL_CAPACITY) {
            return new HotelRentMutation(HotelRentStatus.HALLS_FULL, null);
        }
        if (occupancy.firstFreeSlot() < 0) {
            return new HotelRentMutation(HotelRentStatus.NO_FREE_SLOT, null);
        }

        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        List<RuntimeInventory.PersistentItem> stagedKeyStates =
                normalizedStates.getOrDefault(
                        ClassicWorldHotelCatalog.KEY_ITEM_ID, List.of());
        Set<Long> stagedKeyIds = stagedKeyStates.stream()
                .map(RuntimeInventory.PersistentItem::objectId)
                .collect(java.util.stream.Collectors.toSet());
        if (normalizedInventory.getOrDefault(
                        ClassicWorldHotelCatalog.KEY_ITEM_ID, 0)
                    != keyObjectIds.size()
                || stagedKeyStates.size() != keyObjectIds.size()
                || stagedKeyStates.stream().anyMatch(item -> item.quantity() != 1)
                || !stagedKeyIds.equals(distinctKeys)) {
            throw new IllegalArgumentException(
                    "staged hotel keys do not exactly match the new lease");
        }
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<Integer, HotelLeaseState> previousLeases =
                new LinkedHashMap<>(hotelLeases);
        Map<Long, HotelKeyState> previousKeys = new LinkedHashMap<>(hotelKeys);

        int showId = Math.toIntExact(keyObjectIds.get(0));
        long now = currentEpochSeconds();
        long expiry = now > Long.MAX_VALUE - ClassicWorldHotelCatalog.LEASE_SECONDS
                ? Long.MAX_VALUE : now + ClassicWorldHotelCatalog.LEASE_SECONDS;
        HotelLeaseState lease = new HotelLeaseState(
                showId, npcId, occupancy.firstFreeSlot(), meetingHall,
                identity, expiry, false);
        if (hotelLeases.put(showId, lease) != null) {
            throw new IllegalStateException("hotel showId is already leased");
        }
        for (long objectId : keyObjectIds) {
            hotelKeys.put(objectId, new HotelKeyState(
                    objectId, showId, npcId, meetingHall, expiry));
        }
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        boolean sqlCommitted = false;
        try {
            if (sqlPersistence != null) {
                sqlPersistence.persist(lease);
                sqlCommitted = true;
            }
            save();
        } catch (IOException | RuntimeException failure) {
            if (sqlCommitted) {
                // SQL is the durable authority. Keep the matching in-memory state
                // even if the restart/debug sidecar cannot be refreshed.
                System.out.println("[LiTo] hotel rental sidecar mirror failed after SQL commit: "
                        + failure.getMessage());
                return new HotelRentMutation(HotelRentStatus.RENTED, lease);
            }
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            hotelLeases.clear();
            hotelLeases.putAll(previousLeases);
            hotelKeys.clear();
            hotelKeys.putAll(previousKeys);
            throw failure;
        }
        return new HotelRentMutation(HotelRentStatus.RENTED, lease);
    }

    synchronized HotelReturnQuote hotelReturnQuote(
            int npcId, String renterIdentity, Set<Long> inventoryObjectIds) {
        String identity = renterIdentity == null ? "" : renterIdentity.trim();
        long now = currentEpochSeconds();
        HotelLeaseState owned = hotelLeases.values().stream()
                .filter(lease -> lease.npcId() == npcId && !lease.released()
                        && lease.renterIdentity().equalsIgnoreCase(identity))
                .sorted(Comparator.comparingInt(
                                (HotelLeaseState lease) -> lease.active(now) ? 0 : 1)
                        .thenComparingInt(HotelLeaseState::roomNumber))
                .findFirst().orElse(null);
        LinkedHashSet<Long> returnedKeys = new LinkedHashSet<>();
        if (inventoryObjectIds != null) {
            for (Long objectId : inventoryObjectIds) {
                HotelKeyState keyState = objectId == null ? null : hotelKeys.get(objectId);
                if (keyState != null && keyState.npcId() == npcId) {
                    returnedKeys.add(objectId);
                }
            }
        }
        int refund = Math.multiplyExact(
                returnedKeys.size(), ClassicWorldHotelCatalog.REFUND_PER_RETURNED_KEY);
        if (owned != null && owned.active(now)) {
            refund = Math.addExact(
                    refund, ClassicWorldHotelCatalog.ACTIVE_LEASE_BASE_REFUND);
        }
        return new HotelReturnQuote(
                npcId, identity, refund, owned != null, returnedKeys);
    }

    synchronized void saveEconomyStateAndHotelReturn(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            HotelReturnQuote expected) throws IOException {
        saveEconomyStateAndHotelReturn(character, points, inventory,
                itemStates, expected, null);
    }

    synchronized void saveEconomyStateAndHotelReturn(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            HotelReturnQuote expected,
            HotelReturnPersistenceHook sqlPersistence) throws IOException {
        if (expected == null || !expected.present()) {
            throw new IllegalArgumentException("hotel return quote is empty");
        }
        HotelReturnQuote current = hotelReturnQuote(
                expected.npcId(), expected.renterIdentity(),
                expected.returnedKeyObjectIds());
        if (!current.equals(expected)) {
            throw new IllegalStateException("hotel return state changed");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        if (normalizedStates.getOrDefault(
                        ClassicWorldHotelCatalog.KEY_ITEM_ID, List.of()).stream()
                .map(RuntimeInventory.PersistentItem::objectId)
                .anyMatch(expected.returnedKeyObjectIds()::contains)) {
            throw new IllegalArgumentException(
                    "returned hotel key remains in staged inventory");
        }
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<Integer, HotelLeaseState> previousLeases =
                new LinkedHashMap<>(hotelLeases);
        Map<Long, HotelKeyState> previousKeys = new LinkedHashMap<>(hotelKeys);

        if (expected.releasesLease()) {
            HotelLeaseState owned = hotelLeases.values().stream()
                    .filter(lease -> lease.npcId() == expected.npcId()
                            && !lease.released()
                            && lease.renterIdentity().equalsIgnoreCase(
                                    expected.renterIdentity()))
                    .sorted(Comparator.comparingInt(
                                    (HotelLeaseState lease) -> lease.active(
                                            currentEpochSeconds()) ? 0 : 1)
                            .thenComparingInt(HotelLeaseState::roomNumber))
                    .findFirst().orElse(null);
            if (owned == null) {
                throw new IllegalStateException("hotel lease disappeared before return");
            }
            hotelLeases.put(owned.showId(), owned.markReleased());
        }
        for (long objectId : expected.returnedKeyObjectIds()) {
            hotelKeys.remove(objectId);
        }
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        boolean sqlCommitted = false;
        try {
            if (sqlPersistence != null) {
                sqlPersistence.persist(expected);
                sqlCommitted = true;
            }
            save();
        } catch (IOException | RuntimeException failure) {
            if (sqlCommitted) {
                System.out.println("[LiTo] hotel return sidecar mirror failed after SQL commit: "
                        + failure.getMessage());
                return;
            }
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            hotelLeases.clear();
            hotelLeases.putAll(previousLeases);
            hotelKeys.clear();
            hotelKeys.putAll(previousKeys);
            throw failure;
        }
    }

    /**
     * Reconciles read-only selected-source inventory into LiTo-owned durable
     * objects. Unchanged source rows preserve LiTo consumption; a changed row
     * resets only its own mapped object. Ordinary etcitems, weapon instances
     * and immutable armor equip mirrors live in the sidecar; pet collars keep
     * their dedicated source-authoritative pipeline.
     */
    synchronized SourceInventoryReconciliation reconcileSourceInventory(
            String character, List<SourceInventoryItem> sourceItems) throws IOException {
        return reconcileSourceInventory(
                character, sourceItems, SourceEquipmentMagicStats.none());
    }

    synchronized List<SourceInventoryBinding> sourceInventoryBindings(String character) {
        if (character == null || character.isBlank()) return List.of();
        MutableProfile profile = profile(character);
        return profile.sourceInventory.values().stream()
                .map(mapping -> new SourceInventoryBinding(
                        mapping.sourceObjectId, mapping.localObjectId,
                        mapping.catalogId, mapping.consumed, mapping.litoOwned))
                .sorted(Comparator.comparingLong(SourceInventoryBinding::sourceObjectId))
                .toList();
    }

    /**
     * Moves source-binding authority with an SQL warehouse transfer.  A row
     * placed in a warehouse is no longer a character_items mutation target;
     * an exact-id row retrieved from a warehouse becomes one for this player.
     */
    synchronized void reconcileWarehouseSourceBindings(
            String character, RuntimeInventory beforeInventory,
            RuntimeInventory beforeWarehouse, RuntimeInventory afterInventory,
            RuntimeInventory afterWarehouse, Map<Long, Long> localToSourceObjectIds)
            throws IOException {
        if (character == null || character.isBlank() || beforeInventory == null
                || beforeWarehouse == null || afterInventory == null
                || afterWarehouse == null || localToSourceObjectIds == null) {
            throw new IllegalArgumentException("warehouse source binding state is required");
        }
        MutableProfile profile = profile(character);
        Map<Long, MutableSourceInventory> previous =
                new LinkedHashMap<>(profile.sourceInventory);
        Set<Long> afterInventoryIds = afterInventory.items().stream()
                .map(RuntimeInventory.Item::objectId)
                .collect(java.util.stream.Collectors.toSet());
        for (RuntimeInventory.Item item : beforeInventory.items()) {
            if (!item.sourceBound() || item.litoOwned()
                    || afterInventoryIds.contains(item.objectId())) continue;
            long sourceId = localToSourceObjectIds.getOrDefault(
                    item.objectId(), item.objectId());
            profile.sourceInventory.remove(sourceId);
        }
        Set<Long> beforeInventoryIds = beforeInventory.items().stream()
                .map(RuntimeInventory.Item::objectId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> beforeWarehouseIds = beforeWarehouse.items().stream()
                .map(RuntimeInventory.Item::objectId)
                .collect(java.util.stream.Collectors.toSet());
        for (RuntimeInventory.Item item : afterInventory.items()) {
            if (!item.sourceBound() || item.litoOwned()
                    || beforeInventoryIds.contains(item.objectId())
                    || !beforeWarehouseIds.contains(item.objectId())) continue;
            long sourceId = localToSourceObjectIds.getOrDefault(
                    item.objectId(), item.objectId());
            profile.sourceInventory.put(sourceId, new MutableSourceInventory(
                    sourceId, item.objectId(), Math.toIntExact(
                            item.definition().catalogId()), "0".repeat(64), false, false));
        }
        if (profile.sourceInventory.equals(previous)) return;
        try {
            save();
        } catch (IOException failure) {
            profile.sourceInventory.clear();
            profile.sourceInventory.putAll(previous);
            throw failure;
        }
    }

    synchronized SourceInventoryReconciliation reconcileSourceInventory(
            String character, List<SourceInventoryItem> sourceItems,
            SourceEquipmentMagicStats equipmentSnapshot) throws IOException {
        List<SourceInventoryItem> snapshot = sourceItems == null
                ? List.of() : List.copyOf(sourceItems);
        SourceEquipmentMagicStats equipment = equipmentSnapshot == null
                ? SourceEquipmentMagicStats.none() : equipmentSnapshot;
        SourceWeaponPhysicalStats qualifiedWeapon = equipment.physical().weapon();
        SourceProjectilePhysicalStats qualifiedProjectile = qualifiedWeapon.projectile();
        SourceInventoryItem qualifiedWeaponRow = null;
        SourceInventoryItem qualifiedProjectileRow = null;
        if (!snapshot.isEmpty()) {
            List<SourceInventoryItem> equippedWeaponRows = snapshot.stream()
                    .filter(SourceInventoryItem::equipped)
                    .filter(item -> item.wire().kind().equals("weapon"))
                    .toList();
            if (qualifiedWeapon.present()) {
                List<SourceInventoryItem> matches = equippedWeaponRows.stream()
                        .filter(item -> item.catalogId() == qualifiedWeapon.itemId())
                        .filter(item -> item.nativeEnchantLevel()
                                == (byte) qualifiedWeapon.enchantLevel())
                        .filter(item -> item.durability() == qualifiedWeapon.durability())
                        .toList();
                if (equippedWeaponRows.size() != 1 || matches.size() != 1) {
                    throw new IOException(
                            "selected-source weapon inventory and combat snapshot differ");
                }
                qualifiedWeaponRow = matches.get(0);
            } else if (!equippedWeaponRows.isEmpty()) {
                throw new IOException(
                        "selected-source inventory has an unqualified equipped weapon");
            }
            if (qualifiedProjectile.present()) {
                List<SourceInventoryItem> matches = snapshot.stream()
                        .filter(SourceInventoryItem::sourceProjectileImportable)
                        .filter(item -> item.sourceObjectId()
                                == qualifiedProjectile.objectId())
                        .filter(item -> item.catalogId() == qualifiedProjectile.itemId())
                        .filter(item -> item.quantity() == qualifiedProjectile.count())
                        .filter(item -> item.wire().useToken().equals(
                                qualifiedProjectile.useType() == -3 ? "sting" : "arrow"))
                        .toList();
                if (matches.size() != 1) {
                    throw new IOException(
                            "selected-source projectile inventory and combat snapshot differ");
                }
                qualifiedProjectileRow = matches.get(0);
            }
            long equippedArmorRows = snapshot.stream()
                    .filter(SourceInventoryItem::equipped)
                    .filter(item -> item.wire().kind().equals("armor"))
                    .count();
            if (equippedArmorRows != equipment.matchedArmorRows()) {
                throw new IOException(
                        "selected-source armor inventory and equipment snapshot differ");
            }
        }
        MutableProfile profile = profile(character);
        Map<Long, SourceInventoryItem> importable = new LinkedHashMap<>();
        Map<Long, InventoryCatalog.Definition> definitions = new LinkedHashMap<>();
        int deferredEquipment = 0;
        int deferredPetCollars = 0;
        int deferredWeaponArmor = 0;
        int deferredProjectiles = 0;
        int observedSourceWeight = 0;
        int retainedSourceWeight = 0;
        int importedWeaponRows = 0;
        int importedArmorRows = 0;
        int equippedSourceArmorObjects = 0;
        int importedProjectileRows = 0;
        for (SourceInventoryItem item : snapshot) {
            if (item == null) throw new IOException("selected-source inventory contains null row");
            observedSourceWeight = BattleInventoryWeight.addWeight(
                    observedSourceWeight, item.nativeWeight());
            if (!item.sidecarImportable()) {
                if (item.equipped()) deferredEquipment++;
                if (item.linkedPetCollar()) deferredPetCollars++;
                if (!item.wire().kind().equals("etc")) deferredWeaponArmor++;
                if (item.wire().useToken().equals("arrow")
                        || item.wire().useToken().equals("sting")) deferredProjectiles++;
                // A linked collar is already materialized by importSourcePets;
                // retaining its source weight here would count it twice.
                if (!item.linkedPetCollar()) {
                    retainedSourceWeight = BattleInventoryWeight.addWeight(
                            retainedSourceWeight, item.nativeWeight());
                }
                continue;
            }
            if (item.sourceWeaponImportable()) importedWeaponRows++;
            if (item.sourceArmorImportable()) {
                importedArmorRows++;
                if (item.equipped()) equippedSourceArmorObjects++;
            }
            if (item.sourceProjectileImportable()) importedProjectileRows++;
            if (importable.putIfAbsent(item.sourceObjectId(), item) != null) {
                throw new IOException("duplicate selected-source inventory object: "
                        + item.sourceObjectId());
            }
            InventoryCatalog.Definition previous = definitions.putIfAbsent(
                    (long) item.catalogId(), item.definition());
            if (previous != null && !previous.equals(item.definition())) {
                throw new IOException("conflicting selected-source item template: "
                        + item.catalogId());
            }
        }

        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<Long, MutableSourceInventory> previousMappings =
                new LinkedHashMap<>(profile.sourceInventory);
        Map<Long, MutableSourceProjectile> previousSourceProjectiles =
                new LinkedHashMap<>(profile.sourceProjectiles);
        MutableSourceWeaponDurability previousSourceWeaponDurability =
                profile.sourceWeaponDurability;
        Map<Long, Integer> stagedCounts = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> stagedStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<Long, MutableSourceInventory> nextMappings =
                new LinkedHashMap<>(profile.sourceInventory);
        Map<Long, MutableSourceProjectile> nextSourceProjectiles =
                new LinkedHashMap<>(profile.sourceProjectiles);
        int importedRows = 0;
        int resetRows = 0;
        int removedRows = 0;
        Set<Long> resetSourceObjects = new LinkedHashSet<>();

        Map<Long, SourceInventoryItem> projectileRows = new LinkedHashMap<>();
        for (SourceInventoryItem source : importable.values()) {
            if (!source.sourceProjectileImportable()) continue;
            projectileRows.put(source.sourceObjectId(), source);
        }
        nextSourceProjectiles.keySet().removeIf(
                sourceObjectId -> !projectileRows.containsKey(sourceObjectId));
        for (SourceInventoryItem source : projectileRows.values()) {
            MutableSourceProjectile current = nextSourceProjectiles.get(
                    source.sourceObjectId());
            if (current == null || current.itemId != source.catalogId()
                    || current.observedCount != source.quantity()) {
                nextSourceProjectiles.put(source.sourceObjectId(),
                        new MutableSourceProjectile(
                                source.sourceObjectId(), source.catalogId(),
                                source.quantity(), source.quantity()));
            }
        }

        for (MutableSourceInventory mapped : List.copyOf(nextMappings.values())) {
            SourceInventoryItem current = importable.get(mapped.sourceObjectId);
            if (current == null) {
                removeMappedSourceObject(stagedCounts, stagedStates, mapped);
                nextMappings.remove(mapped.sourceObjectId);
                removedRows++;
                continue;
            }
            String fingerprint = sourceInventoryFingerprint(current);
            if (mapped.catalogId != current.catalogId()
                    || mapped.litoOwned != current.litoOwned()
                    || !mapped.observedFingerprint.equals(fingerprint)) {
                removeMappedSourceObject(stagedCounts, stagedStates, mapped);
                nextMappings.remove(mapped.sourceObjectId);
                resetSourceObjects.add(mapped.sourceObjectId);
                resetRows++;
            }
        }
        RuntimeInventory staged = RuntimeInventory.fromDurableState(
                stagedCounts, stagedStates, definitions);

        try {
            for (SourceInventoryItem source : importable.values()) {
                String fingerprint = sourceInventoryFingerprint(source);
                MutableSourceInventory previous = nextMappings.get(source.sourceObjectId());
                if (previous != null && previous.catalogId == source.catalogId()
                        && previous.litoOwned == source.litoOwned()
                        && previous.observedFingerprint.equals(fingerprint)) {
                    continue;
                }
                if (!resetSourceObjects.contains(source.sourceObjectId())) {
                    importedRows++;
                }
                int effectiveDurability = source.durability();
                if (source == qualifiedWeaponRow
                        && previousSourceWeaponDurability != null
                        && previousSourceWeaponDurability.itemId == source.catalogId()
                        && previousSourceWeaponDurability.observedDurability
                                == source.durability()) {
                    effectiveDurability =
                            previousSourceWeaponDurability.effectiveDurability;
                }
                MutableSourceProjectile projectile = nextSourceProjectiles.get(
                        source.sourceObjectId());
                int effectiveQuantity = projectile == null
                        ? source.quantity() : Math.toIntExact(projectile.remainingCount);
                if (effectiveQuantity == 0) {
                    throw new IOException(
                            "new selected-source projectile mapping has no effective quantity: "
                                    + source.sourceObjectId());
                }
                if (source.litoOwned()) {
                    RuntimeInventory.Item stale = staged.item(source.sourceObjectId());
                    if (stale != null) staged.removeEntireForDeath(source.sourceObjectId());
                }
                long localObjectId = staged.addDistinctState(
                        source.definition(), new RuntimeInventory.PersistentItem(
                                source.litoOwned() ? source.sourceObjectId() : 0,
                                effectiveQuantity, source.nativeEnchantLevel(),
                                effectiveDurability, source.identified(), source.blessing(),
                                true, source.tournamentReward(), source.equipped(), null,
                                Math.min(source.chargeCount(),
                                        source.definition().maximumChargeCount()),
                                source.expiresAtEpochMillis(), source.sealed(),
                                source.litoOwned()));
                if (localObjectId == 0) {
                    throw new IOException("source inventory exceeds native object capacity at "
                            + "source object " + source.sourceObjectId()
                            + " catalog " + source.catalogId());
                }
                nextMappings.put(source.sourceObjectId(), new MutableSourceInventory(
                        source.sourceObjectId(), localObjectId, source.catalogId(),
                        fingerprint, false, source.litoOwned()));
            }

            Map<Long, SourceWeaponPhysicalStats> sourceWeapons = new LinkedHashMap<>();
            if (qualifiedWeaponRow != null) {
                MutableSourceInventory mapped =
                        nextMappings.get(qualifiedWeaponRow.sourceObjectId());
                RuntimeInventory.Item item = mapped == null || mapped.consumed
                        ? null : staged.item(mapped.localObjectId);
                if (mapped == null || !mapped.consumed
                        && (item == null || item.definition().catalogId()
                            != qualifiedWeaponRow.catalogId())) {
                    throw new IOException(
                            "qualified selected-source weapon has no durable runtime object");
                }
                if (item != null) sourceWeapons.put(item.objectId(), qualifiedWeapon);
                profile.sourceWeaponDurability = null;
            }

            Map<Long, Long> sourceEquipmentObjects = new LinkedHashMap<>();
            Set<Long> consumedSourceEquipmentObjects = new LinkedHashSet<>();
            for (SourceInventoryItem source : importable.values()) {
                if (!source.sourceWeaponImportable()
                        && !source.sourceArmorImportable()) continue;
                MutableSourceInventory mapped = nextMappings.get(source.sourceObjectId());
                if (mapped != null && mapped.consumed) {
                    consumedSourceEquipmentObjects.add(source.sourceObjectId());
                    continue;
                }
                RuntimeInventory.Item runtime = mapped == null
                        ? null : staged.item(mapped.localObjectId);
                if (runtime == null || runtime.definition().catalogId() != source.catalogId()
                        || !runtime.sourceBound()
                        || (!runtime.definition().weapon()
                                && !runtime.definition().sourceArmor())) {
                    throw new IOException(
                            "selected-source equipment has no durable runtime object: "
                                    + source.sourceObjectId());
                }
                if (sourceEquipmentObjects.putIfAbsent(
                        runtime.objectId(), source.sourceObjectId()) != null) {
                    throw new IOException("two source equipment rows share runtime object "
                            + runtime.objectId());
                }
            }

            Map<Long, Long> sourceProjectileObjects = new LinkedHashMap<>();
            for (SourceInventoryItem source : projectileRows.values()) {
                MutableSourceInventory mapped = nextMappings.get(source.sourceObjectId());
                MutableSourceProjectile balance = nextSourceProjectiles.get(
                        source.sourceObjectId());
                if (mapped == null || balance == null) {
                    throw new IOException(
                            "selected-source projectile mapping/ledger is missing: "
                                    + source.sourceObjectId());
                }
                if (mapped.consumed) {
                    if (balance.remainingCount != 0) {
                        throw new IOException(
                                "consumed selected-source projectile has a remaining balance: "
                                        + source.sourceObjectId());
                    }
                    continue;
                }
                RuntimeInventory.Item runtime = staged.item(mapped.localObjectId);
                if (balance.remainingCount == 0) {
                    if (runtime != null) {
                        throw new IOException(
                                "exhausted selected-source projectile still has an object: "
                                        + source.sourceObjectId());
                    }
                    continue;
                }
                if (runtime == null || runtime.definition().catalogId() != source.catalogId()
                        || !runtime.sourceBound() || !runtime.definition().projectile()
                        || runtime.quantity() != balance.remainingCount) {
                    throw new IOException(
                            "selected-source projectile has no exact durable runtime object: "
                                    + source.sourceObjectId());
                }
                if (sourceProjectileObjects.putIfAbsent(
                        source.sourceObjectId(), runtime.objectId()) != null) {
                    throw new IOException("duplicate selected-source projectile object "
                            + source.sourceObjectId());
                }
            }

            Map<Long, SourceMagicDollType> sourceMagicDolls = new LinkedHashMap<>();
            for (SourceInventoryItem source : importable.values()) {
                if (source.magicDollType() == null) continue;
                MutableSourceInventory mapped = nextMappings.get(source.sourceObjectId());
                RuntimeInventory.Item runtime = mapped == null || mapped.consumed
                        ? null : staged.item(mapped.localObjectId);
                if (runtime == null || runtime.definition().catalogId() != source.catalogId()
                        || runtime.definition().effect()
                                != InventoryCatalog.Effect.MAGIC_DOLL) {
                    throw new IOException(
                            "selected-source magic doll has no durable runtime object: "
                                    + source.sourceObjectId());
                }
                sourceMagicDolls.put(runtime.objectId(), source.magicDollType());
            }

            profile.inventory.clear();
            profile.inventory.putAll(staged.catalogCounts());
            profile.inventoryItemStates.clear();
            profile.inventoryItemStates.putAll(
                    mutableItemStates(staged.catalogItemStates()));
            profile.sourceInventory.clear();
            profile.sourceInventory.putAll(nextMappings);
            profile.sourceProjectiles.clear();
            profile.sourceProjectiles.putAll(nextSourceProjectiles);
            save();

            return new SourceInventoryReconciliation(
                    new Economy(profile.points, Map.copyOf(profile.inventory),
                            normalizedItemStates(
                                    profile.inventoryItemStates, profile.inventory)),
                    definitions, sourceWeapons, sourceEquipmentObjects,
                    sourceProjectileObjects, sourceMagicDolls,
                    consumedSourceEquipmentObjects,
                    observedSourceWeight, retainedSourceWeight,
                    snapshot.size(), importable.size(), (int) nextMappings.values().stream()
                            .filter(mapping -> !mapping.consumed).count(),
                    importedRows, resetRows, removedRows, deferredEquipment,
                    deferredPetCollars, deferredWeaponArmor, deferredProjectiles,
                    importedWeaponRows, sourceWeapons.size(),
                    importedArmorRows, staged.equippedSourceArmorCount(),
                    importedProjectileRows,
                    qualifiedProjectileRow != null
                            && sourceProjectileObjects.containsKey(
                                    qualifiedProjectileRow.sourceObjectId()) ? 1 : 0);
        } catch (IOException | RuntimeException failure) {
            profile.inventory.clear();
            profile.inventory.putAll(previousInventory);
            profile.inventoryItemStates.clear();
            profile.inventoryItemStates.putAll(previousStates);
            profile.sourceInventory.clear();
            profile.sourceInventory.putAll(previousMappings);
            profile.sourceProjectiles.clear();
            profile.sourceProjectiles.putAll(previousSourceProjectiles);
            profile.sourceWeaponDurability = previousSourceWeaponDurability;
            throw failure;
        }
    }

    /**
     * Imports legacy character_pets rows into LiTo's durable state. A collar
     * first seen in character inventory receives a namespaced LiTo id; after
     * an SQL warehouse round trip, an exact native row already present in the
     * inventory is adopted and the durable pet is rekeyed instead of cloned.
     */
    synchronized List<PetState> importSourcePets(
            String character, List<SourcePet> sourcePets) throws IOException {
        return importSourcePets(character, sourcePets, Map.of());
    }

    synchronized List<PetState> importSourcePets(
            String character, List<SourcePet> sourcePets,
            Map<Long, InventoryCatalog.Definition> sourceDefinitions)
            throws IOException {
        if (sourcePets == null || sourcePets.isEmpty()) return List.of();
        MutableProfile profile = profile(character);
        ProfileIdentity identity = profileIdentity(character);
        String sourceNamespace = key(identity.source().isBlank()
                ? "memory" : identity.source());
        RuntimeInventory staged = RuntimeInventory.fromDurableState(
                profile.inventory, profile.inventoryItemStates,
                sourceDefinitions == null ? Map.of() : sourceDefinitions);
        Map<Long, PetState> previousPets = new LinkedHashMap<>(pets);
        Set<String> previousImports = new LinkedHashSet<>(importedSourcePetKeys);
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        long previousNextPetObjectId = nextPetObjectId;
        List<PetState> imported = new ArrayList<>();
        Set<Long> sourceCollars = new LinkedHashSet<>();
        try {
            for (SourcePet sourcePet : sourcePets) {
                if (sourcePet == null || !sourceCollars.add(sourcePet.sourceCollarObjectId())) {
                    continue;
                }
                String sourceKey = sourceNamespace + SOURCE_CHARACTER_SEPARATOR
                        + sourcePet.sourceCollarObjectId();
                if (sourcePet.litoOwned()) {
                    PetState existing = pets.get(sourcePet.sourceCollarObjectId());
                    if (existing != null) {
                        if (!existing.litoOwned()
                                || existing.objectId() != sourcePet.sourcePetObjectId()
                                || existing.collarCatalogId()
                                        != sourcePet.collarCatalogId()) {
                            throw new IOException(
                                    "LiTo-owned SQL pet conflicts with durable identity");
                        }
                        PetState adopted = new PetState(sourceKey,
                                sourcePet.sourceCollarObjectId(),
                                sourcePet.sourcePetObjectId(),
                                sourcePet.collarCatalogId(), sourcePet.npcId(),
                                sourcePet.name(), sourcePet.level(), sourcePet.maxHp(),
                                sourcePet.maxMp(), sourcePet.experience(),
                                sourcePet.lawful(), true);
                        if (!existing.sourceKey().isBlank()
                                && !existing.sourceKey().equals(sourceKey)) {
                            importedSourcePetKeys.remove(existing.sourceKey());
                        }
                        pets.put(adopted.collarObjectId(), adopted);
                        importedSourcePetKeys.add(sourceKey);
                        imported.add(adopted);
                        continue;
                    }
                }
                if (importedSourcePetKeys.contains(sourceKey)) {
                    PetState existingImported = pets.values().stream()
                            .filter(candidate -> candidate.sourceKey().equals(sourceKey))
                            .findFirst().orElse(null);
                    RuntimeInventory.Item exactWarehouseCollar = staged.item(
                            sourcePet.sourceCollarObjectId());
                    if (!sourcePet.litoOwned() && existingImported != null
                            && existingImported.collarObjectId()
                                    != sourcePet.sourceCollarObjectId()
                            && exactWarehouseCollar != null
                            && exactWarehouseCollar.sourceBound()
                            && !exactWarehouseCollar.litoOwned()
                            && exactWarehouseCollar.definition().catalogId()
                                    == sourcePet.collarCatalogId()) {
                        PetState rekeyed = new PetState(sourceKey,
                                sourcePet.sourceCollarObjectId(),
                                existingImported.objectId(), sourcePet.collarCatalogId(),
                                sourcePet.npcId(), sourcePet.name(), sourcePet.level(),
                                sourcePet.maxHp(), sourcePet.maxMp(), sourcePet.experience(),
                                sourcePet.lawful(), false);
                        pets.remove(existingImported.collarObjectId());
                        pets.put(rekeyed.collarObjectId(), rekeyed);
                        imported.add(rekeyed);
                    }
                    continue;
                }
                RuntimeInventory.Item added;
                if (sourcePet.litoOwned()) {
                    added = staged.item(sourcePet.sourceCollarObjectId());
                    if (added == null || !added.litoOwned() || !added.sourceBound()
                            || added.definition().catalogId()
                                    != sourcePet.collarCatalogId()) {
                        throw new IOException("LiTo-owned SQL pet collar was not reconciled");
                    }
                } else {
                    RuntimeInventory.Item exactWarehouseCollar = staged.item(
                            sourcePet.sourceCollarObjectId());
                    if (exactWarehouseCollar != null
                            && exactWarehouseCollar.sourceBound()
                            && !exactWarehouseCollar.litoOwned()
                            && exactWarehouseCollar.definition().catalogId()
                                    == sourcePet.collarCatalogId()) {
                        // A collar retrieved from SQL warehouse already owns
                        // the native row id; do not manufacture a duplicate.
                        added = exactWarehouseCollar;
                    } else {
                        InventoryCatalog.Definition collar = InventoryCatalog.require(
                                sourcePet.collarCatalogId());
                        Set<Long> beforeIds = staged.items().stream()
                                .map(RuntimeInventory.Item::objectId)
                                .collect(java.util.stream.Collectors.toSet());
                        if (!staged.add(collar, 1)) {
                            throw new IOException(
                                    "native inventory has no slot for source pet collar");
                        }
                        added = staged.items().stream()
                                .filter(item -> !beforeIds.contains(item.objectId()))
                                .filter(item -> item.definition().catalogId()
                                        == sourcePet.collarCatalogId())
                                .findFirst().orElseThrow(() -> new IOException(
                                        "source pet collar allocation was not observable"));
                    }
                }
                PetState pet = new PetState(sourceKey, added.objectId(),
                        sourcePet.litoOwned() ? sourcePet.sourcePetObjectId()
                                : allocatePetObjectId(), sourcePet.collarCatalogId(),
                        sourcePet.npcId(), sourcePet.name(), sourcePet.level(),
                        sourcePet.maxHp(), sourcePet.maxMp(), sourcePet.experience(),
                        sourcePet.lawful(), sourcePet.litoOwned());
                if (pets.putIfAbsent(pet.collarObjectId(), pet) != null) {
                    throw new IOException("duplicate durable pet collar object id");
                }
                importedSourcePetKeys.add(sourceKey);
                imported.add(pet);
            }
            if (imported.isEmpty()) return List.of();
            replacePositiveCounts(profile.inventory, staged.catalogCounts());
            replaceItemStates(profile.inventoryItemStates, staged.catalogItemStates());
            save();
            return List.copyOf(imported);
        } catch (IOException | RuntimeException failure) {
            pets.clear();
            pets.putAll(previousPets);
            importedSourcePetKeys.clear();
            importedSourcePetKeys.addAll(previousImports);
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            nextPetObjectId = previousNextPetObjectId;
            throw failure;
        }
    }

    synchronized PetState petForCollar(long collarObjectId) {
        return pets.get(collarObjectId);
    }

    synchronized PetState petForObject(long objectId) {
        return pets.values().stream()
                .filter(pet -> pet.objectId() == objectId)
                .findFirst().orElse(null);
    }

    synchronized List<LitoPetPersistence> petsForInventory(RuntimeInventory inventory) {
        if (inventory == null) return List.of();
        return pets.values().stream()
                .filter(pet -> inventory.item(pet.collarObjectId()) != null)
                .map(PetState::persistence)
                .toList();
    }

    synchronized Set<Long> deletedSourcePetCollarIds(String character) {
        if (character == null || character.isBlank()) return Set.of();
        ProfileIdentity identity = profileIdentity(character);
        String prefix = key(identity.source().isBlank() ? "memory" : identity.source())
                + SOURCE_CHARACTER_SEPARATOR;
        Set<String> live = pets.values().stream()
                .map(PetState::sourceKey)
                .filter(sourceKey -> !sourceKey.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> deleted = new LinkedHashSet<>();
        for (String imported : importedSourcePetKeys) {
            if (!imported.startsWith(prefix) || live.contains(imported)) continue;
            try {
                long sourceId = Long.parseLong(imported.substring(prefix.length()));
                if (sourceId > 0 && sourceId <= 0xFFFF_FFFFL) deleted.add(sourceId);
            } catch (NumberFormatException ignored) {
                // Damaged provenance remains fail-closed and is never sent to SQL.
            }
        }
        return Set.copyOf(deleted);
    }

    synchronized void savePet(PetState pet) throws IOException {
        if (pet == null) throw new IllegalArgumentException("durable pet is required");
        PetState current = pets.get(pet.collarObjectId());
        if (current == null || current.objectId() != pet.objectId()
                || current.npcId() != pet.npcId()
                || current.collarCatalogId() != pet.collarCatalogId()
                || !current.sourceKey().equals(pet.sourceKey())) {
            throw new IllegalArgumentException("durable pet identity changed");
        }
        pets.put(pet.collarObjectId(), pet);
        try {
            save();
        } catch (IOException failure) {
            pets.put(current.collarObjectId(), current);
            throw failure;
        }
    }

    /**
     * Atomically publishes the retained tamePet collar, owner inventory and
     * new persistent pet identity.  The selected source creates a newly tamed
     * pet at template level with 750 experience and neutral lawful alignment.
     */
    synchronized PetState saveEconomyStateCreatingPet(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            long collarObjectId, SourcePetTemplate template) throws IOException {
        if (inventory == null || itemStates == null || template == null
                || collarObjectId <= 0 || collarObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(
                    "inventory, collar and selected pet template are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        if (pets.containsKey(collarObjectId)
                || !containsInventoryObject(normalizedStates, collarObjectId)
                || normalizedStates.getOrDefault(
                        (long) template.collarCatalogId(), List.of()).stream()
                    .noneMatch(item -> item.objectId() == collarObjectId)) {
            throw new IllegalArgumentException("invalid newly tamed pet collar identity");
        }

        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        long previousNextPetObjectId = nextPetObjectId;
        PetState created = new PetState(
                "", collarObjectId, allocatePetObjectId(),
                template.collarCatalogId(), template.npcId(), template.name(),
                template.sourceLevel(), template.baseMaxHp(), template.baseMaxMp(),
                750, 0);
        if (pets.putIfAbsent(collarObjectId, created) != null) {
            nextPetObjectId = previousNextPetObjectId;
            throw new IllegalArgumentException("duplicate newly tamed pet collar identity");
        }
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        try {
            save();
            return created;
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            pets.remove(collarObjectId);
            nextPetObjectId = previousNextPetObjectId;
            throw failure;
        }
    }

    synchronized boolean deletePet(long collarObjectId) throws IOException {
        PetState removed = pets.remove(collarObjectId);
        if (removed == null) return false;
        try {
            save();
            return true;
        } catch (IOException failure) {
            pets.put(collarObjectId, removed);
            throw failure;
        }
    }

    /**
     * Atomically replaces the retained evolvePet collar/pet identity together
     * with the owner's exact native inventory object set.
     */
    synchronized void saveEconomyStateEvolvingPet(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            long previousCollarObjectId, PetState evolved) throws IOException {
        if (inventory == null || itemStates == null || evolved == null
                || previousCollarObjectId <= 0
                || previousCollarObjectId > 0xFFFF_FFFFL
                || evolved.collarObjectId() == previousCollarObjectId) {
            throw new IllegalArgumentException(
                    "inventory and changed pet collar identity are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        PetState previous = pets.get(previousCollarObjectId);
        if (previous == null || previous.objectId() != evolved.objectId()
                || previous.npcId() == evolved.npcId()
                || !previous.sourceKey().equals(evolved.sourceKey())
                || evolved.level() != 1 || evolved.experience() != 0
                || pets.containsKey(evolved.collarObjectId())
                || containsInventoryObject(normalizedStates, previousCollarObjectId)
                || !containsInventoryObject(normalizedStates, evolved.collarObjectId())
                || normalizedStates.getOrDefault(
                        (long) evolved.collarCatalogId(), List.of()).stream()
                    .noneMatch(item -> item.objectId() == evolved.collarObjectId())) {
            throw new IllegalArgumentException("invalid durable pet evolution transition");
        }

        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        pets.remove(previousCollarObjectId);
        pets.put(evolved.collarObjectId(), evolved);
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            pets.remove(evolved.collarObjectId());
            pets.put(previousCollarObjectId, previous);
            throw failure;
        }
    }

    private static boolean containsInventoryObject(
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            long objectId) {
        return itemStates.values().stream().flatMap(List::stream)
                .anyMatch(item -> item.objectId() == objectId);
    }

    /**
     * Reconciles a LiTo-side lawful overlay with the selected read-only legacy
     * character row. A changed source value is an external authoritative
     * mutation and atomically resets the overlay, exactly like projectile
     * reconciliation, without ever writing the legacy database.
     */
    synchronized LawfulBalance reconcileLawful(String character, int sourceLawful)
            throws IOException {
        requireLawful(sourceLawful, "source lawful");
        MutableProfile profile = profile(character);
        if (profile.lawfulPresent && profile.observedSourceLawful == sourceLawful) {
            return new LawfulBalance(profile.observedSourceLawful, profile.effectiveLawful);
        }

        boolean previousPresent = profile.lawfulPresent;
        int previousObserved = profile.observedSourceLawful;
        int previousEffective = profile.effectiveLawful;
        profile.lawfulPresent = true;
        profile.observedSourceLawful = sourceLawful;
        profile.effectiveLawful = sourceLawful;
        try {
            save();
        } catch (IOException failure) {
            profile.lawfulPresent = previousPresent;
            profile.observedSourceLawful = previousObserved;
            profile.effectiveLawful = previousEffective;
            throw failure;
        }
        return new LawfulBalance(sourceLawful, sourceLawful);
    }

    /** Applies one native lawful delta and clamps to the signed-word domain. */
    synchronized LawfulMutation addLawful(
            String character, int sourceLawful, int requestedDelta) throws IOException {
        requireLawful(sourceLawful, "source lawful");
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.lawfulPresent;
        int previousObserved = profile.observedSourceLawful;
        int previousEffective = profile.effectiveLawful;
        if (!profile.lawfulPresent || profile.observedSourceLawful != sourceLawful) {
            profile.lawfulPresent = true;
            profile.observedSourceLawful = sourceLawful;
            profile.effectiveLawful = sourceLawful;
        }
        int before = profile.effectiveLawful;
        long candidate = (long) before + requestedDelta;
        int after = (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, candidate));
        profile.effectiveLawful = after;
        boolean needsSave = !previousPresent || previousObserved != sourceLawful
                || previousEffective != after;
        if (needsSave) {
            try {
                save();
            } catch (IOException failure) {
                profile.lawfulPresent = previousPresent;
                profile.observedSourceLawful = previousObserved;
                profile.effectiveLawful = previousEffective;
                throw failure;
            }
        }
        return new LawfulMutation(before, after, requestedDelta);
    }

    /** Reconciles selected read-only characters.PKcount/LastPk. */
    synchronized GeneralPkBalance reconcileGeneralPk(
            String character, int sourceCount, long sourceLastPkEpochSeconds)
            throws IOException {
        GeneralPkState source = new GeneralPkState(
                sourceCount, sourceLastPkEpochSeconds);
        MutableProfile profile = profile(character);
        GeneralPkState observed = new GeneralPkState(
                profile.observedSourceGeneralPkCount,
                profile.observedSourceLastPkEpochSeconds);
        GeneralPkState effective = new GeneralPkState(
                profile.effectiveGeneralPkCount,
                profile.effectiveLastPkEpochSeconds);
        if (profile.generalPkPresent && observed.equals(source)) {
            return new GeneralPkBalance(observed, effective);
        }
        boolean previousPresent = profile.generalPkPresent;
        profile.generalPkPresent = true;
        profile.observedSourceGeneralPkCount = source.count();
        profile.observedSourceLastPkEpochSeconds = source.lastPkEpochSeconds();
        profile.effectiveGeneralPkCount = source.count();
        profile.effectiveLastPkEpochSeconds = source.lastPkEpochSeconds();
        try {
            save();
        } catch (IOException failure) {
            profile.generalPkPresent = previousPresent;
            profile.observedSourceGeneralPkCount = observed.count();
            profile.observedSourceLastPkEpochSeconds =
                    observed.lastPkEpochSeconds();
            profile.effectiveGeneralPkCount = effective.count();
            profile.effectiveLastPkEpochSeconds = effective.lastPkEpochSeconds();
            throw failure;
        }
        return new GeneralPkBalance(source, source);
    }

    synchronized GeneralPkState recordGeneralPkKill(
            String character, boolean incrementCount) throws IOException {
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.generalPkPresent;
        if (!profile.generalPkPresent) profile.generalPkPresent = true;
        int previousCount = profile.effectiveGeneralPkCount;
        long previousLast = profile.effectiveLastPkEpochSeconds;
        profile.effectiveGeneralPkCount = incrementCount
                ? Math.addExact(previousCount, 1) : previousCount;
        profile.effectiveLastPkEpochSeconds = currentEpochSeconds();
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.generalPkPresent = previousPresent;
            profile.effectiveGeneralPkCount = previousCount;
            profile.effectiveLastPkEpochSeconds = previousLast;
            throw failure;
        }
        return new GeneralPkState(profile.effectiveGeneralPkCount,
                profile.effectiveLastPkEpochSeconds);
    }

    synchronized GeneralPkState clearGeneralPkAfterGuardDeath(String character)
            throws IOException {
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.generalPkPresent;
        int previousCount = profile.effectiveGeneralPkCount;
        long previousLast = profile.effectiveLastPkEpochSeconds;
        profile.generalPkPresent = true;
        profile.effectiveGeneralPkCount = Math.max(0, previousCount - 1);
        profile.effectiveLastPkEpochSeconds = 0;
        try {
            save();
        } catch (IOException failure) {
            profile.generalPkPresent = previousPresent;
            profile.effectiveGeneralPkCount = previousCount;
            profile.effectiveLastPkEpochSeconds = previousLast;
            throw failure;
        }
        return new GeneralPkState(profile.effectiveGeneralPkCount, 0);
    }

    /** Reconciles retained character_other killCount/deathCount read-only rows. */
    synchronized ClassicKillDeathBalance reconcileClassicKillDeath(
            String character, SourceKillDeathCounts sourceCounts) throws IOException {
        if (sourceCounts == null) {
            throw new IllegalArgumentException("source kill/death counts are required");
        }
        ClassicKillDeathState source = new ClassicKillDeathState(
                sourceCounts.kills(), sourceCounts.deaths());
        MutableProfile profile = profile(character);
        ClassicKillDeathState observed = new ClassicKillDeathState(
                profile.observedSourceClassicKills,
                profile.observedSourceClassicDeaths);
        ClassicKillDeathState effective = new ClassicKillDeathState(
                profile.effectiveClassicKills,
                profile.effectiveClassicDeaths);
        if (profile.classicKillDeathPresent && observed.equals(source)) {
            return new ClassicKillDeathBalance(observed, effective);
        }
        boolean previousPresent = profile.classicKillDeathPresent;
        profile.classicKillDeathPresent = true;
        profile.observedSourceClassicKills = source.kills();
        profile.observedSourceClassicDeaths = source.deaths();
        profile.effectiveClassicKills = source.kills();
        profile.effectiveClassicDeaths = source.deaths();
        try {
            save();
        } catch (IOException failure) {
            profile.classicKillDeathPresent = previousPresent;
            profile.observedSourceClassicKills = observed.kills();
            profile.observedSourceClassicDeaths = observed.deaths();
            profile.effectiveClassicKills = effective.kills();
            profile.effectiveClassicDeaths = effective.deaths();
            throw failure;
        }
        return new ClassicKillDeathBalance(source, source);
    }

    /** Atomically increments winner killCount and loser deathCount in one save. */
    synchronized ClassicPlayerKillMutation recordClassicPlayerKill(
            String attackerCharacter, String victimCharacter) throws IOException {
        if (attackerCharacter == null || victimCharacter == null
                || key(attackerCharacter).equals(key(victimCharacter))) {
            throw new IllegalArgumentException(
                    "classic player kill requires two distinct characters");
        }
        MutableProfile attacker = profile(attackerCharacter);
        MutableProfile victim = profile(victimCharacter);
        boolean attackerPresent = attacker.classicKillDeathPresent;
        boolean victimPresent = victim.classicKillDeathPresent;
        ClassicKillDeathState attackerBefore = new ClassicKillDeathState(
                attacker.effectiveClassicKills, attacker.effectiveClassicDeaths);
        ClassicKillDeathState victimBefore = new ClassicKillDeathState(
                victim.effectiveClassicKills, victim.effectiveClassicDeaths);
        attacker.classicKillDeathPresent = true;
        victim.classicKillDeathPresent = true;
        try {
            attacker.effectiveClassicKills = Math.addExact(
                    attacker.effectiveClassicKills, 1);
            victim.effectiveClassicDeaths = Math.addExact(
                    victim.effectiveClassicDeaths, 1);
            save();
        } catch (IOException | RuntimeException failure) {
            attacker.classicKillDeathPresent = attackerPresent;
            victim.classicKillDeathPresent = victimPresent;
            attacker.effectiveClassicKills = attackerBefore.kills();
            attacker.effectiveClassicDeaths = attackerBefore.deaths();
            victim.effectiveClassicKills = victimBefore.kills();
            victim.effectiveClassicDeaths = victimBefore.deaths();
            throw failure;
        }
        return new ClassicPlayerKillMutation(
                attackerBefore,
                new ClassicKillDeathState(attacker.effectiveClassicKills,
                        attacker.effectiveClassicDeaths),
                victimBefore,
                new ClassicKillDeathState(victim.effectiveClassicKills,
                        victim.effectiveClassicDeaths));
    }

    /** Reconciles selected read-only characters.HellTime without mutating it. */
    synchronized HellBalance reconcileHell(String character, int sourceTicks)
            throws IOException {
        HellState source = new HellState(sourceTicks);
        MutableProfile profile = profile(character);
        HellState observed = new HellState(profile.observedSourceHellTicks);
        HellState effective = new HellState(profile.effectiveHellTicks);
        if (profile.hellPresent && observed.equals(source)) {
            return new HellBalance(observed, effective);
        }
        boolean previousPresent = profile.hellPresent;
        profile.hellPresent = true;
        profile.observedSourceHellTicks = sourceTicks;
        profile.effectiveHellTicks = sourceTicks;
        try {
            save();
        } catch (IOException failure) {
            profile.hellPresent = previousPresent;
            profile.observedSourceHellTicks = observed.remainingTicks();
            profile.effectiveHellTicks = effective.remainingTicks();
            throw failure;
        }
        return new HellBalance(source, source);
    }

    synchronized HellState startHell(String character, int pkCount) throws IOException {
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.hellPresent;
        int previousTicks = profile.effectiveHellTicks;
        profile.hellPresent = true;
        profile.effectiveHellTicks = ClassicWorldHell.durationTicks(pkCount);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.hellPresent = previousPresent;
            profile.effectiveHellTicks = previousTicks;
            throw failure;
        }
        return new HellState(profile.effectiveHellTicks);
    }

    synchronized HellState elapseHell(String character, int ticks) throws IOException {
        if (ticks < 0) throw new IllegalArgumentException("negative HellTime elapsed ticks");
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.hellPresent;
        int previousTicks = profile.effectiveHellTicks;
        profile.hellPresent = true;
        profile.effectiveHellTicks = Math.max(0, previousTicks - ticks);
        if (profile.effectiveHellTicks != previousTicks || !previousPresent) {
            try {
                save();
            } catch (IOException failure) {
                profile.hellPresent = previousPresent;
                profile.effectiveHellTicks = previousTicks;
                throw failure;
            }
        }
        return new HellState(profile.effectiveHellTicks);
    }

    /** Reconciles selected read-only characters.PkCountForElf/LastPkForElf. */
    synchronized ElfPkBalance reconcileElfPk(
            String character, int sourceCount, long sourceLastPkEpochSeconds)
            throws IOException {
        ElfPkState source = new ElfPkState(sourceCount, sourceLastPkEpochSeconds);
        MutableProfile profile = profile(character);
        ElfPkState observed = new ElfPkState(
                profile.observedSourceElfPkCount,
                profile.observedSourceLastPkForElfEpochSeconds);
        ElfPkState effective = new ElfPkState(
                profile.effectiveElfPkCount,
                profile.effectiveLastPkForElfEpochSeconds);
        if (profile.elfPkPresent && observed.equals(source)) {
            return new ElfPkBalance(observed, effective);
        }
        boolean previousPresent = profile.elfPkPresent;
        profile.elfPkPresent = true;
        profile.observedSourceElfPkCount = source.count();
        profile.observedSourceLastPkForElfEpochSeconds = source.lastPkEpochSeconds();
        profile.effectiveElfPkCount = source.count();
        profile.effectiveLastPkForElfEpochSeconds = source.lastPkEpochSeconds();
        try {
            save();
        } catch (IOException failure) {
            profile.elfPkPresent = previousPresent;
            profile.observedSourceElfPkCount = observed.count();
            profile.observedSourceLastPkForElfEpochSeconds =
                    observed.lastPkEpochSeconds();
            profile.effectiveElfPkCount = effective.count();
            profile.effectiveLastPkForElfEpochSeconds =
                    effective.lastPkEpochSeconds();
            throw failure;
        }
        return new ElfPkBalance(source, source);
    }

    synchronized ElfPkState recordElfPkKill(
            String character, boolean incrementCount) throws IOException {
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.elfPkPresent;
        if (!profile.elfPkPresent) profile.elfPkPresent = true;
        int previousCount = profile.effectiveElfPkCount;
        long previousLast = profile.effectiveLastPkForElfEpochSeconds;
        profile.effectiveElfPkCount = incrementCount
                ? Math.addExact(previousCount, 1) : previousCount;
        profile.effectiveLastPkForElfEpochSeconds = currentEpochSeconds();
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.elfPkPresent = previousPresent;
            profile.effectiveElfPkCount = previousCount;
            profile.effectiveLastPkForElfEpochSeconds = previousLast;
            throw failure;
        }
        return new ElfPkState(profile.effectiveElfPkCount,
                profile.effectiveLastPkForElfEpochSeconds);
    }

    synchronized ElfPkState clearElfPkAfterGuardianDeath(String character)
            throws IOException {
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.elfPkPresent;
        int previousCount = profile.effectiveElfPkCount;
        long previousLast = profile.effectiveLastPkForElfEpochSeconds;
        profile.elfPkPresent = true;
        profile.effectiveElfPkCount = Math.max(0, previousCount - 1);
        profile.effectiveLastPkForElfEpochSeconds = 0;
        try {
            save();
        } catch (IOException failure) {
            profile.elfPkPresent = previousPresent;
            profile.effectiveElfPkCount = previousCount;
            profile.effectiveLastPkForElfEpochSeconds = previousLast;
            throw failure;
        }
        return new ElfPkState(profile.effectiveElfPkCount, 0);
    }

    /** LiTo-side persistence for the native Desire-Cave faction scale. */
    synchronized FactionKarmaBalance factionKarma(String character) {
        return new FactionKarmaBalance(profile(character).factionKarma);
    }

    synchronized FactionKarmaMutation setFactionKarma(
            String character, int requestedValue) throws IOException {
        MutableProfile profile = profile(character);
        int before = profile.factionKarma;
        int after = clampFactionKarma(requestedValue);
        profile.factionKarma = after;
        if (before != after) {
            try {
                save();
            } catch (IOException failure) {
                profile.factionKarma = before;
                throw failure;
            }
        }
        return new FactionKarmaMutation(before, after, after - before);
    }

    synchronized FactionKarmaMutation addFactionKarma(
            String character, int requestedDelta) throws IOException {
        MutableProfile profile = profile(character);
        int before = profile.factionKarma;
        long candidate = (long) before + requestedDelta;
        int after = (int) Math.max(-15_500_000L,
                Math.min(15_500_000L, candidate));
        profile.factionKarma = after;
        if (before != after) {
            try {
                save();
            } catch (IOException failure) {
                profile.factionKarma = before;
                throw failure;
            }
        }
        return new FactionKarmaMutation(before, after, requestedDelta);
    }

    /**
     * Reconciles an EXP/level overlay against the selected read-only character
     * row. A changed source snapshot resets only this LiTo-side overlay.
     */
    synchronized ClassicExperienceBalance reconcileClassicExperience(
            String character, long sourceExperience, int sourceLevel) throws IOException {
        requireClassicExperience(sourceExperience, "source experience");
        requireClassicLevel(sourceLevel, "source level");
        MutableProfile profile = profile(character);
        if (profile.classicExperiencePresent
                && profile.observedSourceExperience == sourceExperience
                && profile.observedSourceLevel == sourceLevel) {
            return new ClassicExperienceBalance(
                    profile.observedSourceExperience, profile.observedSourceLevel,
                    profile.effectiveClassicExperience, profile.effectiveClassicLevel);
        }

        boolean previousPresent = profile.classicExperiencePresent;
        long previousObservedExperience = profile.observedSourceExperience;
        int previousObservedLevel = profile.observedSourceLevel;
        long previousEffectiveExperience = profile.effectiveClassicExperience;
        int previousEffectiveLevel = profile.effectiveClassicLevel;
        profile.classicExperiencePresent = true;
        profile.observedSourceExperience = sourceExperience;
        profile.observedSourceLevel = sourceLevel;
        profile.effectiveClassicExperience = sourceExperience;
        profile.effectiveClassicLevel = sourceLevel;
        try {
            save();
        } catch (IOException failure) {
            profile.classicExperiencePresent = previousPresent;
            profile.observedSourceExperience = previousObservedExperience;
            profile.observedSourceLevel = previousObservedLevel;
            profile.effectiveClassicExperience = previousEffectiveExperience;
            profile.effectiveClassicLevel = previousEffectiveLevel;
            throw failure;
        }
        return new ClassicExperienceBalance(
                sourceExperience, sourceLevel, sourceExperience, sourceLevel);
    }

    /** Applies retained ExpTable penalty and a positive native NPC EXP reward. */
    synchronized ClassicExperienceMutation addClassicExperience(
            String character, long sourceExperience, int sourceLevel,
            long requestedBaseExperience) throws IOException {
        return addClassicExperience(
                character, sourceExperience, sourceLevel, requestedBaseExperience, 1.0D);
    }

    /** Applies retained RateXp and ExpTable penalty in their original double order. */
    synchronized ClassicExperienceMutation addClassicExperience(
            String character, long sourceExperience, int sourceLevel,
            long requestedBaseExperience, double sharedExperienceRate) throws IOException {
        return addClassicExperience(character, sourceExperience, sourceLevel,
                requestedBaseExperience, sharedExperienceRate, 0);
    }

    /** Adds the retained Dragon Gem fixed award after penalty and shared rates. */
    synchronized ClassicExperienceMutation addClassicExperience(
            String character, long sourceExperience, int sourceLevel,
            long requestedBaseExperience, double sharedExperienceRate,
            int fixedBonusExperience) throws IOException {
        requireClassicExperience(sourceExperience, "source experience");
        requireClassicLevel(sourceLevel, "source level");
        if (requestedBaseExperience < 0 || !Double.isFinite(sharedExperienceRate)
                || sharedExperienceRate < 0.0D || fixedBonusExperience < 0
                || (fixedBonusExperience > 0
                    && BattleExperienceGemCatalog.findByBonus(fixedBonusExperience) == null)) {
            throw new IllegalArgumentException("negative classic experience reward");
        }
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.classicExperiencePresent;
        long previousObservedExperience = profile.observedSourceExperience;
        int previousObservedLevel = profile.observedSourceLevel;
        long previousEffectiveExperience = profile.effectiveClassicExperience;
        int previousEffectiveLevel = profile.effectiveClassicLevel;
        if (!profile.classicExperiencePresent
                || profile.observedSourceExperience != sourceExperience
                || profile.observedSourceLevel != sourceLevel) {
            profile.classicExperiencePresent = true;
            profile.observedSourceExperience = sourceExperience;
            profile.observedSourceLevel = sourceLevel;
            profile.effectiveClassicExperience = sourceExperience;
            profile.effectiveClassicLevel = sourceLevel;
        }

        long beforeExperience = profile.effectiveClassicExperience;
        int beforeLevel = profile.effectiveClassicLevel;
        long adjusted = saturatedAdd(ClassicExperienceCatalog.adjustedReward(
                beforeLevel, requestedBaseExperience, sharedExperienceRate),
                fixedBonusExperience);
        long afterExperience = Math.min(0xFFFF_FFFFL,
                saturatedAdd(beforeExperience, adjusted));
        int afterLevel = classicLevelAfterReward(
                beforeLevel, beforeExperience, afterExperience);
        profile.effectiveClassicExperience = afterExperience;
        profile.effectiveClassicLevel = afterLevel;
        boolean needsSave = !previousPresent
                || previousObservedExperience != sourceExperience
                || previousObservedLevel != sourceLevel
                || previousEffectiveExperience != afterExperience
                || previousEffectiveLevel != afterLevel;
        if (needsSave) {
            try {
                save();
            } catch (IOException failure) {
                profile.classicExperiencePresent = previousPresent;
                profile.observedSourceExperience = previousObservedExperience;
                profile.observedSourceLevel = previousObservedLevel;
                profile.effectiveClassicExperience = previousEffectiveExperience;
                profile.effectiveClassicLevel = previousEffectiveLevel;
                throw failure;
            }
        }
        return new ClassicExperienceMutation(
                beforeExperience, beforeLevel, afterExperience, afterLevel,
                requestedBaseExperience, adjusted);
    }

    /** Atomically applies retained L1PcInstance.deathPenalty to the LiTo overlay. */
    synchronized ClassicDeathPenaltyMutation applyClassicDeathPenalty(
            String character, long sourceExperience, int sourceLevel) throws IOException {
        requireClassicExperience(sourceExperience, "source experience");
        requireClassicLevel(sourceLevel, "source level");
        MutableProfile profile = profile(character);
        boolean previousPresent = profile.classicExperiencePresent;
        long previousObservedExperience = profile.observedSourceExperience;
        int previousObservedLevel = profile.observedSourceLevel;
        long previousEffectiveExperience = profile.effectiveClassicExperience;
        int previousEffectiveLevel = profile.effectiveClassicLevel;
        if (!profile.classicExperiencePresent
                || profile.observedSourceExperience != sourceExperience
                || profile.observedSourceLevel != sourceLevel) {
            profile.classicExperiencePresent = true;
            profile.observedSourceExperience = sourceExperience;
            profile.observedSourceLevel = sourceLevel;
            profile.effectiveClassicExperience = sourceExperience;
            profile.effectiveClassicLevel = sourceLevel;
        }

        long beforeExperience = profile.effectiveClassicExperience;
        int beforeLevel = profile.effectiveClassicLevel;
        long requestedPenalty = ClassicExperienceCatalog.deathPenalty(beforeLevel);
        long afterExperience = Math.max(0, beforeExperience - requestedPenalty);
        int afterLevel = ClassicExperienceCatalog.isConsistent(
                beforeLevel, beforeExperience)
                ? ClassicExperienceCatalog.levelForExperience(afterExperience)
                : beforeLevel;
        profile.effectiveClassicExperience = afterExperience;
        profile.effectiveClassicLevel = afterLevel;
        boolean needsSave = !previousPresent
                || previousObservedExperience != sourceExperience
                || previousObservedLevel != sourceLevel
                || previousEffectiveExperience != afterExperience
                || previousEffectiveLevel != afterLevel;
        if (needsSave) {
            try {
                save();
            } catch (IOException failure) {
                profile.classicExperiencePresent = previousPresent;
                profile.observedSourceExperience = previousObservedExperience;
                profile.observedSourceLevel = previousObservedLevel;
                profile.effectiveClassicExperience = previousEffectiveExperience;
                profile.effectiveClassicLevel = previousEffectiveLevel;
                throw failure;
            }
        }
        return new ClassicDeathPenaltyMutation(
                beforeExperience, beforeLevel, afterExperience, afterLevel,
                requestedPenalty);
    }

    /**
     * Reconciles the local read-only-source debit ledger with the latest
     * legacy inventory row.  A changed legacy count is treated as an external
     * authoritative mutation; an unchanged row preserves LiTo-side debits
     * across reconnects and server restarts without writing the legacy DB.
     */
    synchronized SourceProjectileBalance sourceProjectileBalance(
            String character, SourceProjectilePhysicalStats projectile) throws IOException {
        if (projectile == null || !projectile.present()) {
            throw new IllegalArgumentException("source projectile snapshot is required");
        }
        MutableProfile profile = profile(character);
        MutableSourceProjectile previous = profile.sourceProjectiles.get(projectile.objectId());
        boolean changed = previous == null || previous.itemId != projectile.itemId()
                || previous.observedCount != projectile.count();
        if (!changed) return previous.snapshot();

        MutableSourceProjectile replacement = new MutableSourceProjectile(
                projectile.objectId(), projectile.itemId(),
                projectile.count(), projectile.count());
        profile.sourceProjectiles.put(projectile.objectId(), replacement);
        try {
            save();
        } catch (IOException failure) {
            if (previous == null) profile.sourceProjectiles.remove(projectile.objectId());
            else profile.sourceProjectiles.put(projectile.objectId(), previous);
            throw failure;
        }
        return replacement.snapshot();
    }

    /** Atomically reserves one qualified arrow/sting before S_ATTACK is emitted. */
    synchronized SourceProjectileReservation reserveSourceProjectile(
            String character, SourceProjectilePhysicalStats projectile) throws IOException {
        SourceProjectileBalance current = sourceProjectileBalance(character, projectile);
        if (!current.available()) return new SourceProjectileReservation(false, current);
        if (!projectile.consumeOnAttack()) {
            return new SourceProjectileReservation(true, current);
        }

        MutableProfile profile = profile(character);
        MutableSourceProjectile previous = profile.sourceProjectiles.get(projectile.objectId());
        MutableSourceProjectile replacement = new MutableSourceProjectile(
                current.objectId(), current.itemId(), current.observedCount(),
                current.remainingCount() - 1);
        profile.sourceProjectiles.put(projectile.objectId(), replacement);
        try {
            save();
        } catch (IOException failure) {
            profile.sourceProjectiles.put(projectile.objectId(), previous);
            throw failure;
        }
        return new SourceProjectileReservation(true, replacement.snapshot());
    }

    /**
     * Atomically commits the projectile debit ledger and its visible durable
     * source-bound stack. The legacy character_items row remains read-only.
     */
    synchronized SourceProjectileReservation reserveSourceProjectile(
            String character, SourceProjectilePhysicalStats projectile,
            long runtimeObjectId, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates)
            throws IOException {
        if (runtimeObjectId <= 0 || runtimeObjectId > 0xFFFF_FFFFL
                || inventory == null || itemStates == null) {
            throw new IllegalArgumentException(
                    "runtime projectile object and candidate inventory are required");
        }
        SourceProjectileBalance current = sourceProjectileBalance(character, projectile);
        if (!current.available()) return new SourceProjectileReservation(false, current);

        MutableProfile profile = profile(character);
        MutableSourceInventory mapping = profile.sourceInventory.get(projectile.objectId());
        if (mapping == null || mapping.localObjectId != runtimeObjectId
                || mapping.catalogId != projectile.itemId()) {
            throw new IOException("source projectile runtime mapping differs");
        }
        int currentRuntimeQuantity = persistentObjectQuantity(
                profile.inventoryItemStates, projectile.itemId(), runtimeObjectId);
        if (currentRuntimeQuantity != current.remainingCount()) {
            throw new IOException("source projectile durable stack and ledger differ");
        }
        if (!projectile.consumeOnAttack()) {
            return new SourceProjectileReservation(true, current);
        }

        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        long expectedRemaining = current.remainingCount() - 1;
        int candidateRuntimeQuantity = persistentObjectQuantity(
                normalizedStates, projectile.itemId(), runtimeObjectId);
        if (candidateRuntimeQuantity != expectedRemaining) {
            throw new IOException("candidate projectile stack does not contain one debit");
        }

        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        MutableSourceProjectile previous = profile.sourceProjectiles.get(
                projectile.objectId());
        MutableSourceProjectile replacement = new MutableSourceProjectile(
                current.objectId(), current.itemId(), current.observedCount(),
                expectedRemaining);
        profile.sourceProjectiles.put(projectile.objectId(), replacement);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.sourceProjectiles.put(projectile.objectId(), previous);
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            throw failure;
        }
        return new SourceProjectileReservation(true, replacement.snapshot());
    }

    private static int persistentObjectQuantity(
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            int catalogId, long objectId) {
        return itemStates.getOrDefault((long) catalogId, List.of()).stream()
                .filter(item -> item.objectId() == objectId)
                .mapToInt(RuntimeInventory.PersistentItem::quantity)
                .findFirst().orElse(0);
    }

    /** Reconciles a read-only equipped weapon with LiTo's durable damage overlay. */
    synchronized SourceWeaponDurabilityBalance sourceWeaponDurabilityBalance(
            String character, SourceWeaponPhysicalStats weapon) throws IOException {
        if (weapon == null || !weapon.present()) {
            throw new IllegalArgumentException("source weapon snapshot is required");
        }
        MutableProfile profile = profile(character);
        MutableSourceWeaponDurability previous = profile.sourceWeaponDurability;
        boolean changed = previous == null || previous.itemId != weapon.itemId()
                || previous.observedDurability != weapon.durability();
        if (!changed) return previous.snapshot();

        MutableSourceWeaponDurability replacement =
                new MutableSourceWeaponDurability(
                        weapon.itemId(), weapon.durability(), weapon.durability());
        profile.sourceWeaponDurability = replacement;
        try {
            save();
        } catch (IOException failure) {
            profile.sourceWeaponDurability = previous;
            throw failure;
        }
        return replacement.snapshot();
    }

    /** Adds one native durability point without writing the selected legacy DB. */
    synchronized SourceWeaponDurabilityBalance damageSourceWeapon(
            String character, SourceWeaponPhysicalStats weapon) throws IOException {
        return damageSourceWeapon(character, weapon, 1);
    }

    /** Adds retained durability damage without writing the selected legacy DB. */
    synchronized SourceWeaponDurabilityBalance damageSourceWeapon(
            String character, SourceWeaponPhysicalStats weapon, int points)
            throws IOException {
        if (points <= 0 || points > 0x7F) {
            throw new IllegalArgumentException("invalid source weapon damage points");
        }
        SourceWeaponDurabilityBalance current =
                sourceWeaponDurabilityBalance(character, weapon);
        int nextDurability = BattleWeaponBreak.durabilityAfterDamage(
                current.effectiveDurability(), weapon.enchantLevel(), points);
        if (nextDurability == current.effectiveDurability()) return current;
        MutableProfile profile = profile(character);
        MutableSourceWeaponDurability previous = profile.sourceWeaponDurability;
        MutableSourceWeaponDurability replacement =
                new MutableSourceWeaponDurability(
                        current.itemId(), current.observedDurability(),
                        nextDurability);
        profile.sourceWeaponDurability = replacement;
        try {
            save();
        } catch (IOException failure) {
            profile.sourceWeaponDurability = previous;
            throw failure;
        }
        return replacement.snapshot();
    }

    synchronized List<Letter> letters(String character) {
        return List.copyOf(profile(character).letters);
    }

    /**
     * Replaces the sidecar cache with SQL-authoritative physical-letter rows.
     * Inventory counts/objects are reconciled independently from character_items;
     * this method must never manufacture or remove a backing item.
     */
    synchronized void reconcileSqlLetters(String character, List<Letter> letters) {
        MutableProfile profile = profile(character);
        List<Letter> replacement = letters == null ? List.of() : letters.stream()
                .sorted(java.util.Comparator.comparingLong(Letter::id)).toList();
        Set<Long> identities = new LinkedHashSet<>();
        if (replacement.stream().anyMatch(letter -> letter == null
                || !identities.add(letter.id()))) {
            throw new IllegalArgumentException("duplicate/null SQL physical letter");
        }
        profile.letters.clear();
        profile.letters.addAll(replacement);
        for (Letter letter : replacement) {
            if (letter.id() >= nextLetterId && letter.id() < Long.MAX_VALUE) {
                nextLetterId = letter.id() + 1;
            }
        }
    }

    /**
     * Atomically consumes one blank 40310 paper, creates one closed 49016
     * physical letter in the recipient inventory and stores its UTF-16 body.
     */
    synchronized LetterDelivery deliverLetter(
            String senderName, String receiverName, int code, String date,
            String subject, String content) throws IOException {
        MutableProfile sender = profile(senderName);
        MutableProfile receiver = profile(receiverName);
        Map<Long, Integer> senderBefore = new LinkedHashMap<>(sender.inventory);
        Map<Long, Integer> receiverBefore = sender == receiver
                ? senderBefore : new LinkedHashMap<>(receiver.inventory);
        List<Letter> receiverLettersBefore = new ArrayList<>(receiver.letters);
        long previousNextLetterId = nextLetterId;

        if (sender.inventory.getOrDefault(InventoryCatalog.LETTER_PAPER, 0) <= 0) {
            return new LetterDelivery(LetterDeliveryStatus.NO_PAPER, null,
                    sender.inventory, receiver.inventory);
        }

        Map<Long, Integer> senderAfter = new LinkedHashMap<>(sender.inventory);
        decrementCount(senderAfter, InventoryCatalog.LETTER_PAPER);
        Map<Long, Integer> receiverAfter = sender == receiver
                ? senderAfter : new LinkedHashMap<>(receiver.inventory);
        int closed = receiverAfter.getOrDefault(InventoryCatalog.LETTER_CLOSED, 0);
        if (closed == Integer.MAX_VALUE) {
            return new LetterDelivery(LetterDeliveryStatus.RECIPIENT_FULL, null,
                    sender.inventory, receiver.inventory);
        }
        receiverAfter.put(InventoryCatalog.LETTER_CLOSED, closed + 1);
        if (inventorySlots(receiverAfter) > RuntimeInventory.MAX_SLOTS) {
            return new LetterDelivery(LetterDeliveryStatus.RECIPIENT_FULL, null,
                    sender.inventory, receiver.inventory);
        }

        if (nextLetterId <= 0 || nextLetterId == Long.MAX_VALUE) {
            throw new IllegalStateException("durable physical-letter id range exhausted");
        }
        Letter letter = new Letter(nextLetterId++, code, sender.name, receiver.name,
                date, 0, subject, content, false);
        replacePositiveCounts(sender.inventory, senderAfter);
        if (sender != receiver) replacePositiveCounts(receiver.inventory, receiverAfter);
        receiver.letters.add(letter);
        try {
            save();
        } catch (IOException failure) {
            replacePositiveCounts(sender.inventory, senderBefore);
            if (sender != receiver) replacePositiveCounts(receiver.inventory, receiverBefore);
            receiver.letters.clear();
            receiver.letters.addAll(receiverLettersBefore);
            nextLetterId = previousNextLetterId;
            throw failure;
        }
        return new LetterDelivery(LetterDeliveryStatus.DELIVERED, letter,
                sender.inventory, receiver.inventory);
    }

    /** Atomically changes one owned closed physical letter into its open form. */
    synchronized LetterOpenMutation openLetter(String character, long letterId)
            throws IOException {
        MutableProfile profile = profile(character);
        int index = letterIndex(profile, letterId);
        if (index < 0) {
            return new LetterOpenMutation(
                    LetterOpenStatus.NOT_FOUND, null, profile.inventory);
        }
        Letter current = profile.letters.get(index);
        if (current.opened()) {
            return new LetterOpenMutation(
                    LetterOpenStatus.ALREADY_OPEN, current, profile.inventory);
        }
        if (profile.inventory.getOrDefault(InventoryCatalog.LETTER_CLOSED, 0) <= 0) {
            return new LetterOpenMutation(
                    LetterOpenStatus.INVENTORY_CHANGED, current, profile.inventory);
        }

        Map<Long, Integer> before = new LinkedHashMap<>(profile.inventory);
        Map<Long, Integer> after = new LinkedHashMap<>(profile.inventory);
        decrementCount(after, InventoryCatalog.LETTER_CLOSED);
        after.merge(InventoryCatalog.LETTER_OPEN, 1, Math::addExact);
        Letter opened = current.openedCopy();
        replacePositiveCounts(profile.inventory, after);
        profile.letters.set(index, opened);
        try {
            save();
        } catch (IOException failure) {
            replacePositiveCounts(profile.inventory, before);
            profile.letters.set(index, current);
            throw failure;
        }
        return new LetterOpenMutation(LetterOpenStatus.OPENED, opened, profile.inventory);
    }

    /** Removes the body row when its owned physical letter item is destroyed. */
    synchronized LetterDeleteMutation deleteLetter(String character, long letterId)
            throws IOException {
        MutableProfile profile = profile(character);
        int index = letterIndex(profile, letterId);
        if (index < 0) return new LetterDeleteMutation(false, profile.inventory);
        Letter letter = profile.letters.get(index);
        long catalogId = letter.opened()
                ? InventoryCatalog.LETTER_OPEN : InventoryCatalog.LETTER_CLOSED;
        if (profile.inventory.getOrDefault(catalogId, 0) <= 0) {
            return new LetterDeleteMutation(false, profile.inventory);
        }
        Map<Long, Integer> before = new LinkedHashMap<>(profile.inventory);
        Map<Long, Integer> after = new LinkedHashMap<>(profile.inventory);
        decrementCount(after, catalogId);
        replacePositiveCounts(profile.inventory, after);
        profile.letters.remove(index);
        try {
            save();
        } catch (IOException failure) {
            replacePositiveCounts(profile.inventory, before);
            profile.letters.add(index, letter);
            throw failure;
        }
        return new LetterDeleteMutation(true, profile.inventory);
    }

    synchronized String marriagePartner(String character) {
        return profile(character).partner;
    }

    synchronized String marriagePartnerIdentity(String character) {
        MutableProfile owner = profile(character);
        MutableProfile partner = profiles.get(owner.partnerStorageKey);
        if (partner == null || partner.partner.isBlank()) return "";
        return sourceCharacter(partner.identitySource, partner.name);
    }

    /** Mirrors one already-validated SQL PartnerID relation into the sidecar. */
    synchronized void reconcileSqlMarriage(String character, String partnerIdentity)
            throws IOException {
        ProfileIdentity firstIdentity = profileIdentity(character);
        ProfileIdentity secondIdentity = partnerIdentity == null || partnerIdentity.isBlank()
                ? null : profileIdentity(partnerIdentity);
        if (firstIdentity.source().isBlank()
                || secondIdentity != null
                        && (!firstIdentity.source().equalsIgnoreCase(secondIdentity.source())
                                || firstIdentity.storageKey().equals(
                                        secondIdentity.storageKey()))) {
            throw new IllegalArgumentException("invalid SQL marriage identity");
        }
        MutableProfile first = profile(character);
        MutableProfile second = secondIdentity == null ? null : profile(partnerIdentity);
        String desiredSecondKey = secondIdentity == null ? "" : secondIdentity.storageKey();
        if (first.partnerStorageKey.equals(desiredSecondKey)
                && (second == null ? first.partner.isBlank()
                        : second.partnerStorageKey.equals(firstIdentity.storageKey())
                        && first.partner.equals(second.name)
                        && second.partner.equals(first.name))) {
            return;
        }

        Map<MutableProfile, String[]> before = new LinkedHashMap<>();
        snapshotMarriageRelation(first, before);
        if (second != null) snapshotMarriageRelation(second, before);
        clearMarriageRelation(first);
        if (second != null) {
            clearMarriageRelation(second);
            first.partner = second.name;
            first.partnerStorageKey = secondIdentity.storageKey();
            second.partner = first.name;
            second.partnerStorageKey = firstIdentity.storageKey();
        }
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            for (Map.Entry<MutableProfile, String[]> entry : before.entrySet()) {
                entry.getKey().partner = entry.getValue()[0];
                entry.getKey().partnerStorageKey = entry.getValue()[1];
            }
            throw failure;
        }
    }

    private void snapshotMarriageRelation(
            MutableProfile owner, Map<MutableProfile, String[]> before) {
        before.putIfAbsent(owner, new String[] {owner.partner, owner.partnerStorageKey});
        MutableProfile linked = profiles.get(owner.partnerStorageKey);
        if (linked != null) {
            before.putIfAbsent(linked,
                    new String[] {linked.partner, linked.partnerStorageKey});
        }
    }

    private void clearMarriageRelation(MutableProfile owner) {
        String ownerKey = profileStorageKey(owner.identitySource, owner.name);
        MutableProfile linked = profiles.get(owner.partnerStorageKey);
        owner.partner = "";
        owner.partnerStorageKey = "";
        if (linked != null && linked.partnerStorageKey.equals(ownerKey)) {
            linked.partner = "";
            linked.partnerStorageKey = "";
        }
    }

    /** Atomically persists both halves of the classic marriage relation. */
    synchronized MarriageMutation marry(String firstCharacter, String secondCharacter)
            throws IOException {
        if (firstCharacter == null || firstCharacter.isBlank()
                || secondCharacter == null || secondCharacter.isBlank()
                || key(firstCharacter).equals(key(secondCharacter))) {
            return new MarriageMutation(MarriageStatus.INVALID, firstCharacter, secondCharacter);
        }
        MutableProfile first = profile(firstCharacter);
        MutableProfile second = profile(secondCharacter);
        if (!first.partner.isBlank()) {
            return new MarriageMutation(
                    MarriageStatus.FIRST_ALREADY_MARRIED, first.name, second.name);
        }
        if (!second.partner.isBlank()) {
            return new MarriageMutation(
                    MarriageStatus.SECOND_ALREADY_MARRIED, first.name, second.name);
        }
        first.partner = second.name;
        first.partnerStorageKey = profileIdentity(secondCharacter).storageKey();
        second.partner = first.name;
        second.partnerStorageKey = profileIdentity(firstCharacter).storageKey();
        try {
            save();
        } catch (IOException failure) {
            first.partner = "";
            first.partnerStorageKey = "";
            second.partner = "";
            second.partnerStorageKey = "";
            throw failure;
        }
        return new MarriageMutation(MarriageStatus.MARRIED, first.name, second.name);
    }

    /**
     * One-shot divorce transition.  The expected partner prevents a stale
     * question-653 answer from dissolving a later relationship.
     */
    synchronized DivorceMutation divorce(String character, String expectedPartner)
            throws IOException {
        if (character == null || character.isBlank()
                || expectedPartner == null || expectedPartner.isBlank()) {
            return new DivorceMutation(false, "");
        }
        MutableProfile first = profiles.get(profileIdentity(character).storageKey());
        MutableProfile second = first == null ? null : profiles.get(first.partnerStorageKey);
        if (first == null || second == null
                || !profileIdentity(expectedPartner).storageKey().equals(first.partnerStorageKey)
                || !first.partner.equalsIgnoreCase(second.name)
                || !second.partner.equalsIgnoreCase(first.name)
                || !second.partnerStorageKey.equals(profileIdentity(character).storageKey())) {
            return new DivorceMutation(false, "");
        }
        String oldFirstPartner = first.partner;
        String oldFirstPartnerStorageKey = first.partnerStorageKey;
        String oldSecondPartner = second.partner;
        String oldSecondPartnerStorageKey = second.partnerStorageKey;
        first.partner = "";
        first.partnerStorageKey = "";
        second.partner = "";
        second.partnerStorageKey = "";
        try {
            save();
        } catch (IOException failure) {
            first.partner = oldFirstPartner;
            first.partnerStorageKey = oldFirstPartnerStorageKey;
            second.partner = oldSecondPartner;
            second.partnerStorageKey = oldSecondPartnerStorageKey;
            throw failure;
        }
        return new DivorceMutation(true, second.name);
    }

    synchronized Warehouse personalWarehouse(String account, String legacyCharacter)
            throws IOException {
        return personalWarehouse(account, List.of(legacyCharacter));
    }

    synchronized Warehouse personalWarehouse(String account, List<String> legacyCharacters)
            throws IOException {
        List<String> candidates = legacyCharacters == null ? List.of() : legacyCharacters;
        String fallback = candidates.stream().filter(name -> name != null && !name.isBlank())
                .findFirst().orElse("");
        String owner = warehouseOwner(account, fallback);
        Map<Long, Integer> stored = claimScopedWarehouse(accountWarehouses, owner);
        Map<Long, List<RuntimeInventory.PersistentItem>> storedStates =
                claimScopedWarehouseStates(accountWarehouseItemStates, owner);
        // Version 6 stored personal storage below each selected character.
        // Merge every known character of this account so migration cannot
        // strand items merely because a different character logged in first.
        for (String character : candidates) {
            if (character == null || character.isBlank()) continue;
            MutableProfile legacy = profiles.get(key(character.trim()));
            if (legacy == null || legacy.warehouse.isEmpty()) continue;
            for (Map.Entry<Long, Integer> entry : legacy.warehouse.entrySet()) {
                int previous = stored.getOrDefault(entry.getKey(), 0);
                long merged = (long) previous + entry.getValue();
                if (merged > Integer.MAX_VALUE) {
                    throw new IllegalStateException("legacy account warehouse quantity overflow");
                }
                stored.put(entry.getKey(), (int) merged);
            }
            legacy.warehouse.clear();
        }
        replaceItemStates(storedStates, normalizedItemStates(storedStates, stored));
        return new Warehouse(stored, storedStates);
    }

    synchronized Warehouse clanWarehouse(String clanName) throws IOException {
        return scopedWarehouse(WarehouseScope.CLAN, clanName);
    }

    synchronized Warehouse elfWarehouse(String account) throws IOException {
        return scopedWarehouse(WarehouseScope.ELF, account);
    }

    /** Account-level classic warehouse password; zero means disabled. */
    synchronized int warehousePassword(String account) throws IOException {
        if (account == null || account.isBlank()) return 0;
        String owner = warehouseOwner(account, "");
        if (!warehousePasswords.containsKey(owner)) {
            ProfileIdentity identity = profileIdentity(owner);
            String legacyOwner = key(identity.name());
            Integer legacy = identity.source().isEmpty()
                    ? null : warehousePasswords.remove(legacyOwner);
            if (legacy != null) {
                warehousePasswords.put(owner, legacy);
                try {
                    save();
                } catch (IOException failure) {
                    warehousePasswords.remove(owner);
                    warehousePasswords.put(legacyOwner, legacy);
                    throw failure;
                }
            }
        }
        return warehousePasswords.getOrDefault(owner, 0);
    }

    /**
     * Atomically changes the account-level warehouse password only when the
     * caller's old value still matches.  Password zero removes the optional
     * sidecar entry and restores the native unlocked retrieval path.
     */
    synchronized WarehousePasswordStatus changeWarehousePassword(
            String account, int expectedOld, int replacement) throws IOException {
        if (account == null || account.isBlank()
                || expectedOld < 0 || expectedOld > 999_999
                || replacement < 0 || replacement > 999_999) {
            throw new IllegalArgumentException("invalid warehouse password mutation");
        }
        String owner = warehouseOwner(account, "");
        int current = warehousePassword(account);
        if (current != expectedOld) return WarehousePasswordStatus.WRONG_OLD;
        if (current == replacement) return WarehousePasswordStatus.UNCHANGED;
        Integer previous = warehousePasswords.get(owner);
        if (replacement == 0) warehousePasswords.remove(owner);
        else warehousePasswords.put(owner, replacement);
        try {
            save();
        } catch (IOException failure) {
            if (previous == null) warehousePasswords.remove(owner);
            else warehousePasswords.put(owner, previous);
            throw failure;
        }
        return WarehousePasswordStatus.CHANGED;
    }

    /** Seeds one castle once from the immutable legacy database. */
    synchronized CastleState castle(CastleSummary seed) throws IOException {
        return castle("", seed);
    }

    /**
     * Seeds one source-qualified castle once from its immutable legacy database.
     * A format-18 unqualified castle is claimed by the first concrete source
     * that actually loads it; another source with the same castle id receives
     * an independent snapshot.
     */
    synchronized CastleState castle(String sourceId, CastleSummary seed) throws IOException {
        if (seed == null) throw new IllegalArgumentException("legacy castle seed is required");
        String source = normalizedCastleSource(sourceId);
        String storageKey = castleStorageKey(source, seed.castleId());
        MutableCastle stored = castles.get(storageKey);
        if (stored == null && !source.isEmpty()) {
            String legacyKey = castleStorageKey("", seed.castleId());
            MutableCastle legacy = castles.remove(legacyKey);
            if (legacy != null) {
                stored = legacy.withSource(source);
                castles.put(storageKey, stored);
                try {
                    save();
                } catch (IOException failure) {
                    castles.remove(storageKey);
                    castles.put(legacyKey, legacy);
                    throw failure;
                }
            }
        }
        if (stored == null) {
            int taxRate = seed.taxRate() >= 10 && seed.taxRate() <= 50
                    ? seed.taxRate() : 10;
            long publicMoney = Math.min(Integer.MAX_VALUE, seed.publicMoney());
            stored = new MutableCastle(source, seed.castleId(), taxRate, publicMoney,
                    seed.warTimeMillis(), false,
                    new MercenaryRoster(0, 0, 0, 0), 1);
            castles.put(storageKey, stored);
            try {
                save();
            } catch (IOException failure) {
                castles.remove(storageKey);
                throw failure;
            }
        }
        if (stored.warTimeMillis == 0 && seed.warTimeMillis() > 0) {
            stored.warTimeMillis = seed.warTimeMillis();
            try {
                save();
            } catch (IOException failure) {
                stored.warTimeMillis = 0;
                throw failure;
            }
        }
        return stored.snapshot();
    }

    /**
     * Replaces the source-qualified sidecar cache with the exact SQL castle
     * snapshot. SQL owns the native tax/treasury fields and LiTo metadata;
     * this file remains only a restart/debug mirror once the repository is
     * authoritative. Repository version zero maps to sidecar revision one so
     * the existing one-shot dialog capabilities retain their positive token.
     */
    synchronized CastleState reconcileSqlCastle(
            String sourceId, RepositoryCastleSnapshot sql) throws IOException {
        if (sql == null || !sql.authoritative()) {
            throw new IllegalArgumentException(
                    "authoritative SQL castle snapshot is required");
        }
        String source = normalizedCastleSource(sourceId);
        long revision = Math.addExact(sql.snapshotVersion(), 1);
        MutableCastle replacement = new MutableCastle(
                source, sql.castleId(), sql.taxRate(), sql.publicMoney(),
                sql.warTimeMillis(), sql.securityEnabled(), sql.mercenaries(), revision);
        String storageKey = castleStorageKey(source, sql.castleId());
        MutableCastle previous = castles.get(storageKey);
        if (previous != null && previous.snapshot().equals(replacement.snapshot())) {
            return previous.snapshot();
        }
        castles.put(storageKey, replacement);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            if (previous == null) castles.remove(storageKey);
            else castles.put(storageKey, previous);
            throw failure;
        }
        return replacement.snapshot();
    }

    /** Whole-snapshot CAS used by non-SQL fixtures and legacy compatibility. */
    synchronized boolean saveCastleState(
            String sourceId, CastleState expected, int taxRate, long publicMoney,
            boolean securityEnabled, MercenaryRoster mercenaries) throws IOException {
        if (expected == null || taxRate < 10 || taxRate > 50
                || publicMoney < 0 || publicMoney > Integer.MAX_VALUE
                || mercenaries == null) {
            throw new IllegalArgumentException("invalid castle state transaction");
        }
        MutableCastle castle = castles.get(
                castleStorageKey(sourceId, expected.castleId()));
        if (castle == null || !castle.snapshot().equals(expected)) return false;
        int oldTaxRate = castle.taxRate;
        long oldPublicMoney = castle.publicMoney;
        boolean oldSecurityEnabled = castle.securityEnabled;
        MercenaryRoster oldMercenaries = castle.mercenaries;
        long oldRevision = castle.revision;
        castle.taxRate = taxRate;
        castle.publicMoney = publicMoney;
        castle.securityEnabled = securityEnabled;
        castle.mercenaries = mercenaries;
        castle.revision++;
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            castle.taxRate = oldTaxRate;
            castle.publicMoney = oldPublicMoney;
            castle.securityEnabled = oldSecurityEnabled;
            castle.mercenaries = oldMercenaries;
            castle.revision = oldRevision;
            throw failure;
        }
        return true;
    }

    /**
     * Resolves one immutable source schedule against LiTo's durable castle
     * sidecar.  A missing/legacy sidecar time falls back to the selected
     * read-only {@code server_城堡.war_time}; only an observed war end creates
     * a durable override, matching {@code ServerWarExecutor} rather than
     * inventing recurrence for an expired inactive row.
     */
    synchronized CastleWarSchedule effectiveCastleWarSchedule(
            String sourceId, CastleWarSchedule source) {
        if (source == null) throw new IllegalArgumentException("castle-war schedule is required");
        MutableCastle castle = castles.get(castleStorageKey(sourceId, source.castleId()));
        long effectiveWarTime = castle == null || castle.warTimeMillis == 0
                ? source.warTimeMillis() : castle.warTimeMillis;
        return effectiveWarTime == source.warTimeMillis() ? source
                : new CastleWarSchedule(source.castleId(), source.castleName(),
                        effectiveWarTime, source.warDurationMinutes(),
                        source.warIntervalMinutes());
    }

    /**
     * Durable equivalent of the original CastleTable.updateCastle call made
     * after an observed siege ends.  The selected legacy database remains
     * immutable: next war time, tax 10 and cleared public money are committed
     * atomically to LiTo's source-qualified sidecar.
     */
    synchronized CastleState applyCastleWarEnd(
            String sourceId, int castleId, long nextWarTimeMillis) throws IOException {
        if (castleId < 1 || castleId > 8 || nextWarTimeMillis <= 0) {
            throw new IllegalArgumentException("invalid castle-war end state");
        }
        String source = normalizedCastleSource(sourceId);
        String storageKey = castleStorageKey(source, castleId);
        MutableCastle castle = castles.get(storageKey);
        if (castle == null) {
            castle = new MutableCastle(source, castleId, 10, 0,
                    nextWarTimeMillis, false,
                    new MercenaryRoster(0, 0, 0, 0), 1);
            castles.put(storageKey, castle);
            try {
                save();
            } catch (IOException failure) {
                castles.remove(storageKey);
                throw failure;
            }
            return castle.snapshot();
        }
        int previousTaxRate = castle.taxRate;
        long previousPublicMoney = castle.publicMoney;
        long previousWarTimeMillis = castle.warTimeMillis;
        long previousRevision = castle.revision;
        castle.taxRate = 10;
        castle.publicMoney = 0;
        castle.warTimeMillis = nextWarTimeMillis;
        castle.revision++;
        try {
            save();
        } catch (IOException failure) {
            castle.taxRate = previousTaxRate;
            castle.publicMoney = previousPublicMoney;
            castle.warTimeMillis = previousWarTimeMillis;
            castle.revision = previousRevision;
            throw failure;
        }
        return castle.snapshot();
    }

    synchronized CastleOwnershipState castleOwnership(
            String sourceId, int castleId) {
        if (castleId < 1 || castleId > 8) {
            throw new IllegalArgumentException("invalid castle ownership id");
        }
        MutableCastleOwnership ownership = castleOwnerships.get(
                castleStorageKey(sourceId, castleId));
        return ownership == null ? null : ownership.snapshot();
    }

    /**
     * Atomically records a successful retained crown acquisition without
     * updating {@code clan_data.hascastle}.  One row per source/castle is the
     * complete override: a replaced legacy clan therefore resolves to no
     * castle while the new clan resolves to this castle after restart.
     */
    synchronized CastleOwnershipState transferCastleOwnership(
            String sourceId, int castleId, String clanName,
            String leaderName, long leaderObjectId) throws IOException {
        CastleOwnershipState validated = new CastleOwnershipState(
                sourceId, castleId, clanName, leaderName, leaderObjectId, 1);
        String storageKey = castleStorageKey(validated.sourceId(), castleId);
        MutableCastleOwnership previous = castleOwnerships.get(storageKey);
        long revision = previous == null ? 1 : Math.addExact(previous.revision, 1);
        MutableCastleOwnership replacement = new MutableCastleOwnership(
                validated.sourceId(), castleId, validated.clanName(),
                validated.leaderName(), validated.leaderObjectId(), revision);
        castleOwnerships.put(storageKey, replacement);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            if (previous == null) castleOwnerships.remove(storageKey);
            else castleOwnerships.put(storageKey, previous);
            throw failure;
        }
        return replacement.snapshot();
    }

    /**
     * Atomically commits a native castle operation and the player's Adena
     * inventory. The expected revision turns each modal response into a
     * one-shot optimistic capability and rejects concurrent stale dialogs.
     */
    synchronized boolean saveInventoryAndCastle(
            String character, Map<Long, Integer> inventory,
            CastleState expected, int taxRate, long publicMoney) throws IOException {
        return saveInventoryAndCastle(
                "", character, inventory, expected, taxRate, publicMoney);
    }

    synchronized boolean saveInventoryAndCastle(
            String sourceId, String character, Map<Long, Integer> inventory,
            CastleState expected, int taxRate, long publicMoney) throws IOException {
        if (inventory == null || expected == null || taxRate < 10 || taxRate > 50
                || publicMoney < 0 || publicMoney > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid castle transaction");
        }
        MutableCastle castle = castles.get(castleStorageKey(sourceId, expected.castleId()));
        if (castle == null || !castle.snapshot().equals(expected)) return false;
        MutableProfile profile = profile(character);
        Map<Long, Integer> oldInventory = new LinkedHashMap<>(profile.inventory);
        int oldTaxRate = castle.taxRate;
        long oldPublicMoney = castle.publicMoney;
        long oldRevision = castle.revision;
        replacePositiveCounts(profile.inventory, inventory);
        castle.taxRate = taxRate;
        castle.publicMoney = publicMoney;
        castle.revision++;
        try {
            save();
        } catch (IOException failure) {
            profile.inventory.clear();
            profile.inventory.putAll(oldInventory);
            castle.taxRate = oldTaxRate;
            castle.publicMoney = oldPublicMoney;
            castle.revision = oldRevision;
            throw failure;
        }
        return true;
    }

    /**
     * Atomically commits one server-priced S40/C3C mercenary employment.
     * The legacy castle tables remain read-only; only the LiTo sidecar roster
     * and treasury snapshot are changed.
     */
    synchronized boolean saveCastleMercenaries(
            CastleState expected, MercenaryRoster mercenaries,
            long publicMoney) throws IOException {
        return saveCastleMercenaries("", expected, mercenaries, publicMoney);
    }

    synchronized boolean saveCastleMercenaries(
            String sourceId, CastleState expected, MercenaryRoster mercenaries,
            long publicMoney) throws IOException {
        if (expected == null || mercenaries == null
                || publicMoney < 0 || publicMoney > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("invalid castle mercenary transaction");
        }
        MutableCastle castle = castles.get(castleStorageKey(sourceId, expected.castleId()));
        if (castle == null || !castle.snapshot().equals(expected)) return false;
        long oldPublicMoney = castle.publicMoney;
        MercenaryRoster oldMercenaries = castle.mercenaries;
        long oldRevision = castle.revision;
        castle.publicMoney = publicMoney;
        castle.mercenaries = mercenaries;
        castle.revision++;
        try {
            save();
        } catch (IOException failure) {
            castle.publicMoney = oldPublicMoney;
            castle.mercenaries = oldMercenaries;
            castle.revision = oldRevision;
            throw failure;
        }
        return true;
    }

    synchronized MercenaryAssignment mercenaryAssignment(String character) {
        return profile(character).mercenaryAssignment;
    }

    /**
     * Atomically moves hired castle soldiers into one character's pending
     * collection roster.  The original castle tables stay read-only and the
     * expected snapshots reject stale S43/C3F callbacks.
     */
    synchronized boolean assignCastleMercenaries(
            CastleState expectedCastle, MercenaryRoster castleMercenaries,
            String character, MercenaryAssignment expectedAssignment,
            MercenaryAssignment assignment) throws IOException {
        return assignCastleMercenaries(
                "", expectedCastle, castleMercenaries, character,
                expectedAssignment, assignment);
    }

    synchronized boolean assignCastleMercenaries(
            String sourceId, CastleState expectedCastle,
            MercenaryRoster castleMercenaries, String character,
            MercenaryAssignment expectedAssignment,
            MercenaryAssignment assignment) throws IOException {
        if (expectedCastle == null || castleMercenaries == null
                || character == null || character.isBlank()
                || expectedAssignment == null || assignment == null
                || assignment.castleId() != expectedCastle.castleId()) {
            throw new IllegalArgumentException("invalid castle mercenary assignment transaction");
        }
        MutableCastle castle = castles.get(
                castleStorageKey(sourceId, expectedCastle.castleId()));
        if (castle == null || !castle.snapshot().equals(expectedCastle)) return false;
        MutableProfile profile = profile(character);
        if (!profile.mercenaryAssignment.equals(expectedAssignment)) return false;
        MercenaryRoster oldCastleMercenaries = castle.mercenaries;
        long oldRevision = castle.revision;
        MercenaryAssignment oldAssignment = profile.mercenaryAssignment;
        castle.mercenaries = castleMercenaries;
        castle.revision++;
        profile.mercenaryAssignment = assignment;
        try {
            save();
        } catch (IOException failure) {
            castle.mercenaries = oldCastleMercenaries;
            castle.revision = oldRevision;
            profile.mercenaryAssignment = oldAssignment;
            throw failure;
        }
        return true;
    }

    /** Atomically changes the inherited pre-siege castle-security switch. */
    synchronized boolean saveCastleSecurity(
            CastleState expected, boolean enabled) throws IOException {
        return saveCastleSecurity("", expected, enabled);
    }

    synchronized boolean saveCastleSecurity(
            String sourceId, CastleState expected, boolean enabled) throws IOException {
        if (expected == null) {
            throw new IllegalArgumentException("castle security snapshot is required");
        }
        MutableCastle castle = castles.get(castleStorageKey(sourceId, expected.castleId()));
        if (castle == null || !castle.snapshot().equals(expected)) return false;
        boolean oldEnabled = castle.securityEnabled;
        long oldRevision = castle.revision;
        castle.securityEnabled = enabled;
        castle.revision++;
        try {
            save();
        } catch (IOException failure) {
            castle.securityEnabled = oldEnabled;
            castle.revision = oldRevision;
            throw failure;
        }
        return true;
    }

    /**
     * Crash-resumable memory/sidecar equivalent of the SQL security ledger.
     * READY may be returned again after restart until the caller records that
     * the world expulsion completed; the treasury is never charged twice.
     */
    synchronized CastleSecuritySettlement prepareCastleSecurity(
            String sourceId, int castleId, long warStartMillis,
            long chargeAdena) throws IOException {
        if (castleId < 1 || castleId > 7 || warStartMillis <= 0
                || chargeAdena <= 0 || chargeAdena > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "invalid sidecar castle security settlement");
        }
        String source = normalizedCastleSource(sourceId);
        MutableCastle castle = castles.get(castleStorageKey(source, castleId));
        if (castle == null) {
            return new CastleSecuritySettlement(
                    CastleSecuritySettlementStatus.NOT_FOUND, null,
                    warStartMillis, 0);
        }
        if (!castle.securityEnabled) {
            return new CastleSecuritySettlement(
                    CastleSecuritySettlementStatus.DISABLED, castle.snapshot(),
                    warStartMillis, 0);
        }
        String settlementKey = castleSecuritySettlementKey(
                source, castleId, warStartMillis);
        CastleSecurityLedger existing = castleSecuritySettlements.get(settlementKey);
        if (existing != null) {
            if (existing.chargeAdena() != chargeAdena) {
                return new CastleSecuritySettlement(
                        CastleSecuritySettlementStatus.CONFLICT, castle.snapshot(),
                        warStartMillis, existing.executed()
                                ? existing.chargeAdena() : 0);
            }
            CastleSecuritySettlementStatus status = !existing.completed()
                    ? CastleSecuritySettlementStatus.READY
                    : existing.executed()
                            ? CastleSecuritySettlementStatus.ALREADY_SETTLED
                            : CastleSecuritySettlementStatus.INSUFFICIENT_FUNDS;
            return new CastleSecuritySettlement(status, castle.snapshot(),
                    warStartMillis, existing.executed()
                            ? existing.chargeAdena() : 0);
        }

        boolean affordable = castle.publicMoney >= chargeAdena;
        long oldMoney = castle.publicMoney;
        long oldRevision = castle.revision;
        CastleSecurityLedger prepared = new CastleSecurityLedger(
                source, castleId, warStartMillis, chargeAdena,
                affordable, !affordable);
        castleSecuritySettlements.put(settlementKey, prepared);
        if (affordable) {
            castle.publicMoney = Math.subtractExact(
                    castle.publicMoney, chargeAdena);
            castle.revision = Math.addExact(castle.revision, 1);
        }
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            castleSecuritySettlements.remove(settlementKey);
            castle.publicMoney = oldMoney;
            castle.revision = oldRevision;
            throw failure;
        }
        return new CastleSecuritySettlement(
                affordable ? CastleSecuritySettlementStatus.READY
                        : CastleSecuritySettlementStatus.INSUFFICIENT_FUNDS,
                castle.snapshot(), warStartMillis,
                affordable ? chargeAdena : 0);
    }

    synchronized CastleSecuritySettlement completeCastleSecurity(
            String sourceId, int castleId, long warStartMillis)
            throws IOException {
        if (castleId < 1 || castleId > 7 || warStartMillis <= 0) {
            throw new IllegalArgumentException(
                    "invalid sidecar castle security completion");
        }
        String source = normalizedCastleSource(sourceId);
        MutableCastle castle = castles.get(castleStorageKey(source, castleId));
        String settlementKey = castleSecuritySettlementKey(
                source, castleId, warStartMillis);
        CastleSecurityLedger existing = castleSecuritySettlements.get(settlementKey);
        if (castle == null || existing == null) {
            return new CastleSecuritySettlement(
                    CastleSecuritySettlementStatus.NOT_FOUND,
                    castle == null ? null : castle.snapshot(), warStartMillis, 0);
        }
        if (!existing.executed()) {
            return new CastleSecuritySettlement(
                    CastleSecuritySettlementStatus.INSUFFICIENT_FUNDS,
                    castle.snapshot(), warStartMillis, 0);
        }
        if (existing.completed()) {
            return new CastleSecuritySettlement(
                    CastleSecuritySettlementStatus.ALREADY_SETTLED,
                    castle.snapshot(), warStartMillis, existing.chargeAdena());
        }
        CastleSecurityLedger completed = existing.completedCopy();
        castleSecuritySettlements.put(settlementKey, completed);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            castleSecuritySettlements.put(settlementKey, existing);
            throw failure;
        }
        return new CastleSecuritySettlement(
                CastleSecuritySettlementStatus.ALREADY_SETTLED,
                castle.snapshot(), warStartMillis, existing.chargeAdena());
    }

    private Warehouse scopedWarehouse(WarehouseScope scope, String owner) throws IOException {
        String normalized = warehouseOwner(owner, "");
        Map<Long, Integer> stored = claimScopedWarehouse(
                warehouseStore(scope), normalized);
        Map<Long, List<RuntimeInventory.PersistentItem>> storedStates =
                claimScopedWarehouseStates(warehouseStateStore(scope), normalized);
        replaceItemStates(storedStates, normalizedItemStates(storedStates, stored));
        return new Warehouse(stored, storedStates);
    }

    /**
     * Replaces the sidecar cache with an exact SQL-authoritative warehouse
     * snapshot.  The sidecar remains a restart/debug mirror; it is never used
     * to merge extra rows into the shared database.
     */
    synchronized Warehouse reconcileSqlWarehouse(
            WarehouseScope scope, String owner, RuntimeInventory sqlWarehouse)
            throws IOException {
        if (scope == null || owner == null || owner.isBlank() || sqlWarehouse == null) {
            throw new IllegalArgumentException("SQL warehouse reconciliation is required");
        }
        String normalizedOwner = warehouseOwner(owner, "");
        Map<Long, Integer> counts = warehouseStore(scope).computeIfAbsent(
                normalizedOwner, ignored -> new LinkedHashMap<>());
        Map<Long, List<RuntimeInventory.PersistentItem>> states =
                warehouseStateStore(scope).computeIfAbsent(
                        normalizedOwner, ignored -> new LinkedHashMap<>());
        Map<Long, Integer> previousCounts = new LinkedHashMap<>(counts);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(states);
        replacePositiveCounts(counts, sqlWarehouse.catalogCounts());
        replaceItemStates(states, normalizedItemStates(
                sqlWarehouse.catalogItemStates(), counts));
        try {
            save();
        } catch (IOException failure) {
            counts.clear();
            counts.putAll(previousCounts);
            replaceItemStates(states, previousStates);
            throw failure;
        }
        return new Warehouse(counts, states);
    }

    private Map<Long, Integer> claimScopedWarehouse(
            Map<String, Map<Long, Integer>> store, String normalizedOwner) throws IOException {
        Map<Long, Integer> stored = store.get(normalizedOwner);
        if (stored != null) return stored;
        ProfileIdentity identity = profileIdentity(normalizedOwner);
        String legacyOwner = key(identity.name());
        Map<Long, Integer> legacy = identity.source().isEmpty()
                ? null : store.remove(legacyOwner);
        if (legacy != null) {
            store.put(normalizedOwner, legacy);
            try {
                save();
            } catch (IOException failure) {
                store.remove(normalizedOwner);
                store.put(legacyOwner, legacy);
                throw failure;
            }
            return legacy;
        }
        Map<Long, Integer> created = new LinkedHashMap<>();
        store.put(normalizedOwner, created);
        return created;
    }

    private Map<Long, List<RuntimeInventory.PersistentItem>> claimScopedWarehouseStates(
            Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>> store,
            String normalizedOwner) throws IOException {
        Map<Long, List<RuntimeInventory.PersistentItem>> stored = store.get(normalizedOwner);
        if (stored != null) return stored;
        ProfileIdentity identity = profileIdentity(normalizedOwner);
        String legacyOwner = key(identity.name());
        Map<Long, List<RuntimeInventory.PersistentItem>> legacy = identity.source().isEmpty()
                ? null : store.remove(legacyOwner);
        if (legacy != null) {
            store.put(normalizedOwner, legacy);
            try {
                save();
            } catch (IOException failure) {
                store.remove(normalizedOwner);
                store.put(legacyOwner, legacy);
                throw failure;
            }
            return legacy;
        }
        Map<Long, List<RuntimeInventory.PersistentItem>> created = new LinkedHashMap<>();
        store.put(normalizedOwner, created);
        return created;
    }

    /** Skills bought through LiTo are kept outside the legacy read-only database. */
    synchronized Set<Integer> purchasedSkills(String character) {
        return Set.copyOf(profile(character).purchasedSkills);
    }

    /** Skills removed by ordinary-world death, independently of ElfAttr resets. */
    synchronized Set<Integer> deathLostSkills(String character) {
        return Set.copyOf(profile(character).deathLostSkills);
    }

    synchronized void saveDeathLostSkills(String character, Set<Integer> skillIds)
            throws IOException {
        Set<Integer> normalized = immutableNativeSkills(skillIds);
        MutableProfile profile = profile(character);
        Set<Integer> previous = new LinkedHashSet<>(profile.deathLostSkills);
        profile.deathLostSkills.clear();
        profile.deathLostSkills.addAll(normalized);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.deathLostSkills.clear();
            profile.deathLostSkills.addAll(previous);
            throw failure;
        }
    }

    /**
     * Reconciles immutable {@code characters.ElfAttr} into LiTo's sidecar.
     * An unchanged source preserves LiTo choices; an externally changed source
     * starts a new baseline and clears the old forgotten-skill mask.
     */
    synchronized ElfAttributeState reconcileElfAttribute(
            String character, int sourceAttribute) throws IOException {
        requireElfAttribute(sourceAttribute, "source ElfAttr");
        MutableProfile profile = profile(character);
        if (profile.elfAttributePresent
                && profile.observedSourceElfAttribute == sourceAttribute) {
            return elfAttributeSnapshot(profile);
        }
        boolean previousPresent = profile.elfAttributePresent;
        int previousObserved = profile.observedSourceElfAttribute;
        int previousEffective = profile.effectiveElfAttribute;
        Set<Integer> previousForgotten =
                new LinkedHashSet<>(profile.forgottenSkills);
        profile.elfAttributePresent = true;
        profile.observedSourceElfAttribute = sourceAttribute;
        profile.effectiveElfAttribute = sourceAttribute;
        profile.forgottenSkills.clear();
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.elfAttributePresent = previousPresent;
            profile.observedSourceElfAttribute = previousObserved;
            profile.effectiveElfAttribute = previousEffective;
            profile.forgottenSkills.clear();
            profile.forgottenSkills.addAll(previousForgotten);
            throw failure;
        }
        return elfAttributeSnapshot(profile);
    }

    synchronized ElfAttributeState elfAttribute(String character) {
        return elfAttributeSnapshot(profile(character));
    }

    /** Atomically persists an Ellyonne mutation and its learned-skill overlay. */
    synchronized void saveElfAttributeState(
            String character, int effectiveAttribute,
            Set<Integer> learnedSkillIds, Set<Integer> forgottenSkillIds)
            throws IOException {
        requireElfAttribute(effectiveAttribute, "effective ElfAttr");
        Set<Integer> normalizedLearned = immutableNativeSkills(learnedSkillIds);
        Set<Integer> normalizedForgotten = immutableNativeSkills(forgottenSkillIds);
        if (!java.util.Collections.disjoint(normalizedLearned, normalizedForgotten)) {
            throw new IllegalArgumentException(
                    "learned and forgotten skill overlays overlap");
        }
        MutableProfile profile = profile(character);
        if (!profile.elfAttributePresent) {
            throw new IllegalStateException("ElfAttr source was not reconciled");
        }
        int previousEffective = profile.effectiveElfAttribute;
        Set<Integer> previousLearned =
                new LinkedHashSet<>(profile.purchasedSkills);
        Set<Integer> previousForgotten =
                new LinkedHashSet<>(profile.forgottenSkills);
        profile.effectiveElfAttribute = effectiveAttribute;
        profile.purchasedSkills.clear();
        profile.purchasedSkills.addAll(normalizedLearned);
        profile.forgottenSkills.clear();
        profile.forgottenSkills.addAll(normalizedForgotten);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.effectiveElfAttribute = previousEffective;
            profile.purchasedSkills.clear();
            profile.purchasedSkills.addAll(previousLearned);
            profile.forgottenSkills.clear();
            profile.forgottenSkills.addAll(previousForgotten);
            throw failure;
        }
    }

    synchronized void saveEconomy(String character, long points,
                                  Map<Long, Integer> inventory) throws IOException {
        MutableProfile profile = profile(character);
        saveEconomyState(character, points, inventory, profile.inventoryItemStates);
    }

    synchronized void saveEconomy(String character, long points,
                                  Map<Long, Integer> inventory,
                                  Map<Long, List<Integer>> inventoryEnchantments)
            throws IOException {
        if (inventory == null || inventoryEnchantments == null) {
            throw new IllegalArgumentException("inventory and enchantments are required");
        }
        Map<Long, Integer> normalizedInventory = new LinkedHashMap<>();
        inventory.forEach((item, count) -> {
            if (item != null && item > 0 && count != null && count > 0) {
                normalizedInventory.put(item, count);
            }
        });
        saveEconomyState(character, points, normalizedInventory,
                itemStatesFromEnchantments(inventoryEnchantments, normalizedInventory));
    }

    synchronized void saveEconomyState(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates) throws IOException {
        if (inventory == null || itemStates == null) {
            throw new IllegalArgumentException("inventory and item states are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            throw failure;
        }
    }

    /**
     * Persists one retained {@code ServerItemUserTimer} scan. Source rows live
     * in a read-only database, so any expired imported object is tombstoned in
     * the same sidecar commit that removes its durable inventory state.
     */
    synchronized void saveEconomyStateExpiringTimedItems(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            Set<Long> expiredLocalObjectIds, long nowEpochMillis) throws IOException {
        if (inventory == null || itemStates == null || expiredLocalObjectIds == null
                || expiredLocalObjectIds.isEmpty() || nowEpochMillis < 0) {
            throw new IllegalArgumentException(
                    "candidate inventory, expired objects and scan timestamp are required");
        }
        MutableProfile profile = profile(character);
        Set<Long> candidateObjects = itemStates.values().stream()
                .flatMap(List::stream)
                .map(RuntimeInventory.PersistentItem::objectId)
                .collect(java.util.stream.Collectors.toSet());
        for (Long objectId : expiredLocalObjectIds) {
            if (objectId == null || objectId <= 0 || candidateObjects.contains(objectId)) {
                throw new IllegalArgumentException(
                        "expired timed item remains in candidate inventory: " + objectId);
            }
            RuntimeInventory.PersistentItem previous = profile.inventoryItemStates.values()
                    .stream().flatMap(List::stream)
                    .filter(item -> item.objectId() == objectId)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "expired timed item has no durable state: " + objectId));
            if (previous.expiresAtEpochMillis() == 0L
                    || previous.expiresAtEpochMillis() >= nowEpochMillis) {
                throw new IllegalArgumentException(
                        "item deadline has not elapsed: " + objectId);
            }
        }
        Set<Long> mappedSourceObjects = profile.sourceInventory.values().stream()
                .filter(mapping -> expiredLocalObjectIds.contains(mapping.localObjectId))
                .map(mapping -> mapping.localObjectId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        saveEconomyStateAndStoryQuestsConsumingSourceObjects(
                character, points, inventory, itemStates,
                new LinkedHashMap<>(profile.effectiveQuestSteps), mappedSourceObjects);
    }

    /**
     * Retained offline branch of {@code ServerItemUserTimer}. Profiles owned by
     * live sessions are excluded because their in-memory inventories must emit
     * the native removal packet in the same lifecycle step.
     */
    synchronized int expireOfflineTimedItems(
            long nowEpochMillis, Set<String> onlineCharacterIdentities) throws IOException {
        if (nowEpochMillis < 0 || onlineCharacterIdentities == null) {
            throw new IllegalArgumentException(
                    "scan timestamp and online character set are required");
        }
        Set<String> onlineStorageKeys = onlineCharacterIdentities.stream()
                .filter(java.util.Objects::nonNull)
                .map(GameStateStore::profileIdentity)
                .map(ProfileIdentity::storageKey)
                .collect(java.util.stream.Collectors.toSet());
        int expiredCount = 0;
        for (Map.Entry<String, MutableProfile> profileEntry
                : List.copyOf(profiles.entrySet())) {
            if (onlineStorageKeys.contains(profileEntry.getKey())) continue;
            MutableProfile profile = profileEntry.getValue();
            Map<Long, Integer> candidateCounts = new LinkedHashMap<>(profile.inventory);
            Map<Long, List<RuntimeInventory.PersistentItem>> candidateStates =
                    mutableItemStates(profile.inventoryItemStates);
            Set<Long> expiredObjects = new LinkedHashSet<>();
            for (Map.Entry<Long, List<RuntimeInventory.PersistentItem>> stateEntry
                    : List.copyOf(candidateStates.entrySet())) {
                List<RuntimeInventory.PersistentItem> retained = new ArrayList<>();
                int removedQuantity = 0;
                for (RuntimeInventory.PersistentItem item : stateEntry.getValue()) {
                    if (item.expiresAtEpochMillis() != 0L
                            && item.expiresAtEpochMillis() < nowEpochMillis) {
                        expiredObjects.add(item.objectId());
                        removedQuantity = Math.addExact(
                                removedQuantity, item.quantity());
                    } else {
                        retained.add(item);
                    }
                }
                if (removedQuantity == 0) continue;
                long catalogId = stateEntry.getKey();
                int previousCount = candidateCounts.getOrDefault(catalogId, 0);
                if (previousCount < removedQuantity) {
                    throw new IOException(
                            "timed item quantity exceeds durable inventory: " + catalogId);
                }
                if (previousCount == removedQuantity) candidateCounts.remove(catalogId);
                else candidateCounts.put(catalogId, previousCount - removedQuantity);
                if (retained.isEmpty()) candidateStates.remove(catalogId);
                else candidateStates.put(catalogId, List.copyOf(retained));
            }
            if (expiredObjects.isEmpty()) continue;
            String identity = profile.identitySource.isBlank()
                    ? profile.name
                    : sourceCharacter(profile.identitySource, profile.name);
            saveEconomyStateExpiringTimedItems(
                    identity, profile.points, candidateCounts, candidateStates,
                    expiredObjects, nowEpochMillis);
            expiredCount = Math.addExact(expiredCount, expiredObjects.size());
        }
        return expiredCount;
    }

    /**
     * Reconciles the immutable selected-source character_quests snapshot with
     * LiTo's durable overlay. An unchanged source keeps LiTo mutations; a
     * genuinely changed source becomes the new authoritative baseline.
     */
    synchronized StoryQuestState reconcileStoryQuests(
            String character, Map<Integer, Integer> sourceSteps) throws IOException {
        Map<Integer, Integer> normalized = normalizedQuestSteps(sourceSteps);
        MutableProfile profile = profile(character);
        if (profile.storyQuestsPresent
                && profile.observedSourceQuestSteps.equals(normalized)) {
            return storyQuestSnapshot(profile);
        }
        boolean previousPresent = profile.storyQuestsPresent;
        Map<Integer, Integer> previousObserved =
                new LinkedHashMap<>(profile.observedSourceQuestSteps);
        Map<Integer, Integer> previousEffective =
                new LinkedHashMap<>(profile.effectiveQuestSteps);
        profile.storyQuestsPresent = true;
        replaceQuestSteps(profile.observedSourceQuestSteps, normalized);
        replaceQuestSteps(profile.effectiveQuestSteps, normalized);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.storyQuestsPresent = previousPresent;
            replaceQuestSteps(profile.observedSourceQuestSteps, previousObserved);
            replaceQuestSteps(profile.effectiveQuestSteps, previousEffective);
            throw failure;
        }
        return storyQuestSnapshot(profile);
    }

    synchronized StoryQuestState storyQuests(String character) {
        return storyQuestSnapshot(profile(character));
    }

    synchronized void saveStoryQuestSteps(
            String character, Map<Integer, Integer> effectiveSteps) throws IOException {
        Map<Integer, Integer> normalized = normalizedQuestSteps(effectiveSteps);
        MutableProfile profile = profile(character);
        Map<Integer, Integer> previous = new LinkedHashMap<>(profile.effectiveQuestSteps);
        replaceQuestSteps(profile.effectiveQuestSteps, normalized);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            replaceQuestSteps(profile.effectiveQuestSteps, previous);
            throw failure;
        }
    }

    /** Atomically persists a story reward/consume transaction and quest steps. */
    synchronized void saveEconomyStateAndStoryQuests(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            Map<Integer, Integer> effectiveSteps) throws IOException {
        saveEconomyStateAndStoryQuestsConsumingSourceObjects(
                character, points, inventory, itemStates, effectiveSteps, Set.of());
    }

    /**
     * Atomically persists one Desire Cave inventory/quest/karma transaction.
     * This prevents a crash between the native item mutation and its faction
     * karma mutation from leaving a reward that was never paid for (or the
     * inverse).
     */
    synchronized void saveDesireCaveState(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            Map<Integer, Integer> effectiveSteps, int factionKarma) throws IOException {
        if (inventory == null || itemStates == null || effectiveSteps == null) {
            throw new IllegalArgumentException(
                    "inventory, item states and story quest steps are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        Map<Integer, Integer> normalizedQuests = normalizedQuestSteps(effectiveSteps);
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        int previousKarma = profile.factionKarma;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<Integer, Integer> previousQuests =
                new LinkedHashMap<>(profile.effectiveQuestSteps);
        profile.points = Math.max(0, points);
        profile.factionKarma = clampFactionKarma(factionKarma);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        replaceQuestSteps(profile.effectiveQuestSteps, normalizedQuests);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            profile.factionKarma = previousKarma;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            replaceQuestSteps(profile.effectiveQuestSteps, previousQuests);
            throw failure;
        }
    }

    /**
     * Atomically persists a quest transaction and source-object tombstones.
     * The immutable legacy rows are never edited; unchanged source snapshots
     * therefore cannot resurrect native quest items on the next login.
     */
    synchronized void saveEconomyStateAndStoryQuestsConsumingSourceObjects(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            Map<Integer, Integer> effectiveSteps,
            Set<Long> consumedLocalObjectIds) throws IOException {
        if (inventory == null || itemStates == null || effectiveSteps == null) {
            throw new IllegalArgumentException(
                    "inventory, item states and story quest steps are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        Map<Integer, Integer> normalizedQuests = normalizedQuestSteps(effectiveSteps);
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<Integer, Integer> previousQuests =
                new LinkedHashMap<>(profile.effectiveQuestSteps);
        Map<Long, MutableSourceInventory> previousSourceInventory =
                new LinkedHashMap<>(profile.sourceInventory);
        Map<Long, MutableSourceProjectile> previousSourceProjectiles =
                new LinkedHashMap<>(profile.sourceProjectiles);
        if (consumedLocalObjectIds == null) {
            throw new IllegalArgumentException("consumed source objects are required");
        }
        List<MutableSourceInventory> consumedMappings = new ArrayList<>();
        for (Long localObjectId : consumedLocalObjectIds) {
            if (localObjectId == null || localObjectId <= 0) {
                throw new IllegalArgumentException("invalid consumed source object");
            }
            MutableSourceInventory mapped = profile.sourceInventory.values().stream()
                    .filter(candidate -> candidate.localObjectId == localObjectId)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "consumed runtime object has no source mapping: "
                                    + localObjectId));
            consumedMappings.add(mapped);
        }
        for (MutableSourceInventory mapped : consumedMappings) {
            profile.sourceInventory.put(mapped.sourceObjectId, mapped.consumed());
            MutableSourceProjectile projectile = profile.sourceProjectiles.get(
                    mapped.sourceObjectId);
            if (projectile != null) {
                profile.sourceProjectiles.put(mapped.sourceObjectId,
                        new MutableSourceProjectile(
                                projectile.objectId, projectile.itemId,
                                projectile.observedCount, 0));
            }
        }
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        replaceQuestSteps(profile.effectiveQuestSteps, normalizedQuests);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            replaceQuestSteps(profile.effectiveQuestSteps, previousQuests);
            profile.sourceInventory.clear();
            profile.sourceInventory.putAll(previousSourceInventory);
            profile.sourceProjectiles.clear();
            profile.sourceProjectiles.putAll(previousSourceProjectiles);
            throw failure;
        }
    }

    synchronized void registerTimedBuffPersistence(
            String character, TimedBuffPersistenceHook hook) {
        if (hook == null) throw new IllegalArgumentException("timed buff hook is required");
        timedBuffPersistenceHooks.put(key(character), hook);
    }

    synchronized void unregisterTimedBuffPersistence(
            String character, TimedBuffPersistenceHook hook) {
        timedBuffPersistenceHooks.remove(key(character), hook);
    }

    synchronized void reconcileSqlTimedBuffs(
            String character, TimedBuffSnapshot snapshot) throws IOException {
        if (snapshot == null) throw new IllegalArgumentException("timed buffs are required");
        MutableProfile profile = profile(character);
        TimedBuffSnapshot previous = timedBuffSnapshot(profile);
        applyTimedBuffSnapshot(profile, snapshot);
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            applyTimedBuffSnapshot(profile, previous);
            throw failure;
        }
    }

    private boolean persistTimedBuffs(String character, MutableProfile profile)
            throws IOException {
        TimedBuffPersistenceHook hook = timedBuffPersistenceHooks.get(key(character));
        return hook == null || hook.persist(character, timedBuffSnapshot(profile));
    }

    private boolean saveTimedBuffMutation(String character, MutableProfile profile)
            throws IOException {
        if (!persistTimedBuffs(character, profile)) return false;
        save();
        return true;
    }

    private static TimedBuffSnapshot timedBuffSnapshot(MutableProfile profile) {
        return new TimedBuffSnapshot(
                profile.timedExperienceBonus > 0
                        ? new TimedExperienceBuff(profile.timedExperienceBonus,
                                profile.timedExperienceRemainingSeconds)
                        : TimedExperienceBuff.none(),
                new TimedFireWeaponBuff(Math.max(0, profile.fireWeaponRemainingSeconds)),
                new TimedBurningWeaponBuff(Math.max(0, profile.burningWeaponRemainingSeconds)),
                new TimedWindShotBuff(Math.max(0, profile.windShotRemainingSeconds)),
                new TimedFireBlessBuff(Math.max(0, profile.fireBlessRemainingSeconds)),
                new TimedStormEyeBuff(Math.max(0, profile.stormEyeRemainingSeconds)),
                new TimedStormShotBuff(Math.max(0, profile.stormShotRemainingSeconds)),
                new TimedEarthBlessBuff(Math.max(0, profile.earthBlessRemainingSeconds)),
                new TimedIronSkinBuff(Math.max(0, profile.ironSkinRemainingSeconds)),
                new TimedResistFearBuff(Math.max(0, profile.resistFearRemainingSeconds)),
                profile.movementBuffSkillId == 0 ? TimedMovementBuff.none()
                        : new TimedMovementBuff(profile.movementBuffSkillId,
                                profile.movementBuffRemainingSeconds),
                profile.armorProtectionSkillId == 0 ? TimedArmorProtection.none()
                        : new TimedArmorProtection(profile.armorProtectionSkillId,
                                profile.armorProtectionRemainingSeconds));
    }

    private static void applyTimedBuffSnapshot(
            MutableProfile profile, TimedBuffSnapshot snapshot) {
        profile.timedExperienceBonus = snapshot.experience().bonusExperience();
        profile.timedExperienceRemainingSeconds = snapshot.experience().remainingSeconds();
        profile.fireWeaponRemainingSeconds = snapshot.fireWeapon().remainingSeconds();
        profile.burningWeaponRemainingSeconds = snapshot.burningWeapon().remainingSeconds();
        profile.windShotRemainingSeconds = snapshot.windShot().remainingSeconds();
        profile.fireBlessRemainingSeconds = snapshot.fireBless().remainingSeconds();
        profile.stormEyeRemainingSeconds = snapshot.stormEye().remainingSeconds();
        profile.stormShotRemainingSeconds = snapshot.stormShot().remainingSeconds();
        profile.earthBlessRemainingSeconds = snapshot.earthBless().remainingSeconds();
        profile.ironSkinRemainingSeconds = snapshot.ironSkin().remainingSeconds();
        profile.resistFearRemainingSeconds = snapshot.resistFear().remainingSeconds();
        profile.movementBuffSkillId = snapshot.movement().skillId();
        profile.movementBuffRemainingSeconds = snapshot.movement().remainingSeconds();
        profile.armorProtectionSkillId = snapshot.armorProtection().skillId();
        profile.armorProtectionRemainingSeconds =
                snapshot.armorProtection().remainingSeconds();
    }

    synchronized TimedExperienceBuff timedExperienceBuff(String character) {
        MutableProfile profile = profile(character);
        if (profile.timedExperienceBonus <= 0
                || profile.timedExperienceRemainingSeconds <= 0) {
            return TimedExperienceBuff.none();
        }
        return new TimedExperienceBuff(profile.timedExperienceBonus,
                profile.timedExperienceRemainingSeconds);
    }

    /** Persists an in-flight buff timer at graceful disconnect or expiry. */
    synchronized void saveTimedExperienceBuff(
            String character, TimedExperienceBuff buff) throws IOException {
        if (buff == null) throw new IllegalArgumentException("timed EXP buff is required");
        MutableProfile profile = profile(character);
        int previousBonus = profile.timedExperienceBonus;
        int previousSeconds = profile.timedExperienceRemainingSeconds;
        profile.timedExperienceBonus = buff.bonusExperience();
        profile.timedExperienceRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.timedExperienceBonus = previousBonus;
                profile.timedExperienceRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.timedExperienceBonus = previousBonus;
            profile.timedExperienceRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedFireWeaponBuff timedFireWeaponBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedFireWeaponBuff(
                Math.max(0, profile.fireWeaponRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedFireWeaponBuff(
            String character, TimedFireWeaponBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Fire Weapon buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.fireWeaponRemainingSeconds;
        profile.fireWeaponRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.fireWeaponRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.fireWeaponRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedBurningWeaponBuff timedBurningWeaponBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedBurningWeaponBuff(
                Math.max(0, profile.burningWeaponRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedBurningWeaponBuff(
            String character, TimedBurningWeaponBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Burning Weapon buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.burningWeaponRemainingSeconds;
        profile.burningWeaponRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.burningWeaponRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.burningWeaponRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedWindShotBuff timedWindShotBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedWindShotBuff(
                Math.max(0, profile.windShotRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedWindShotBuff(
            String character, TimedWindShotBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Wind Shot buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.windShotRemainingSeconds;
        profile.windShotRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.windShotRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.windShotRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedFireBlessBuff timedFireBlessBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedFireBlessBuff(
                Math.max(0, profile.fireBlessRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedFireBlessBuff(
            String character, TimedFireBlessBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Fire Bless buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.fireBlessRemainingSeconds;
        profile.fireBlessRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.fireBlessRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.fireBlessRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedStormEyeBuff timedStormEyeBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedStormEyeBuff(
                Math.max(0, profile.stormEyeRemainingSeconds));
    }

    synchronized TimedEarthBlessBuff timedEarthBlessBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedEarthBlessBuff(
                Math.max(0, profile.earthBlessRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedEarthBlessBuff(
            String character, TimedEarthBlessBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Earth Bless buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.earthBlessRemainingSeconds;
        profile.earthBlessRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.earthBlessRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.earthBlessRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedIronSkinBuff timedIronSkinBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedIronSkinBuff(
                Math.max(0, profile.ironSkinRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedIronSkinBuff(
            String character, TimedIronSkinBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Iron Skin buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.ironSkinRemainingSeconds;
        profile.ironSkinRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.ironSkinRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.ironSkinRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedResistFearBuff timedResistFearBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedResistFearBuff(
                Math.max(0, profile.resistFearRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable skill188 remaining-time persistence. */
    synchronized void saveTimedResistFearBuff(
            String character, TimedResistFearBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Resist Fear buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.resistFearRemainingSeconds;
        profile.resistFearRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.resistFearRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.resistFearRemainingSeconds = previousSeconds;
            throw failure;
        }
    }


    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedStormEyeBuff(
            String character, TimedStormEyeBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Storm Eye buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.stormEyeRemainingSeconds;
        profile.stormEyeRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.stormEyeRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.stormEyeRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedStormShotBuff timedStormShotBuff(String character) {
        MutableProfile profile = profile(character);
        return new TimedStormShotBuff(
                Math.max(0, profile.stormShotRemainingSeconds));
    }

    /** Mirrors retained CharBuffTable remaining-time persistence. */
    synchronized void saveTimedStormShotBuff(
            String character, TimedStormShotBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("Storm Shot buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSeconds = profile.stormShotRemainingSeconds;
        profile.stormShotRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.stormShotRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.stormShotRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedMovementBuff timedMovementBuff(String character) {
        MutableProfile profile = profile(character);
        if (profile.movementBuffSkillId == 0) {
            return TimedMovementBuff.none();
        }
        return new TimedMovementBuff(profile.movementBuffSkillId,
                profile.movementBuffRemainingSeconds);
    }

    /** Mirrors retained CharBuffTable for the mutually exclusive brave family. */
    synchronized void saveTimedMovementBuff(
            String character, TimedMovementBuff buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("movement buff is required");
        }
        MutableProfile profile = profile(character);
        int previousSkill = profile.movementBuffSkillId;
        int previousSeconds = profile.movementBuffRemainingSeconds;
        profile.movementBuffSkillId = buff.skillId();
        profile.movementBuffRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.movementBuffSkillId = previousSkill;
                profile.movementBuffRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.movementBuffSkillId = previousSkill;
            profile.movementBuffRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    synchronized TimedArmorProtection timedArmorProtection(String character) {
        MutableProfile profile = profile(character);
        if (profile.armorProtectionSkillId == 0) {
            return TimedArmorProtection.none();
        }
        return new TimedArmorProtection(profile.armorProtectionSkillId,
                profile.armorProtectionRemainingSeconds);
    }

    /** Mirrors retained CharBuffTable for the mutually exclusive AC family. */
    synchronized void saveTimedArmorProtection(
            String character, TimedArmorProtection buff) throws IOException {
        if (buff == null) {
            throw new IllegalArgumentException("armor protection is required");
        }
        MutableProfile profile = profile(character);
        int previousSkill = profile.armorProtectionSkillId;
        int previousSeconds = profile.armorProtectionRemainingSeconds;
        profile.armorProtectionSkillId = buff.skillId();
        profile.armorProtectionRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.armorProtectionSkillId = previousSkill;
                profile.armorProtectionRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.armorProtectionSkillId = previousSkill;
            profile.armorProtectionRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    /** Atomically consumes a gem and replaces the durable timed EXP lease. */
    synchronized void saveEconomyStateActivatingExperienceBuff(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            TimedExperienceBuff buff) throws IOException {
        if (inventory == null || itemStates == null || buff == null || !buff.active()) {
            throw new IllegalArgumentException(
                    "inventory, item states and active timed EXP buff are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        int previousBonus = profile.timedExperienceBonus;
        int previousSeconds = profile.timedExperienceRemainingSeconds;
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        profile.timedExperienceBonus = buff.bonusExperience();
        profile.timedExperienceRemainingSeconds = buff.remainingSeconds();
        try {
            if (!saveTimedBuffMutation(character, profile)) {
                profile.points = previousPoints;
                replacePositiveCounts(profile.inventory, previousInventory);
                replaceItemStates(profile.inventoryItemStates, previousStates);
                profile.timedExperienceBonus = previousBonus;
                profile.timedExperienceRemainingSeconds = previousSeconds;
            }
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            profile.timedExperienceBonus = previousBonus;
            profile.timedExperienceRemainingSeconds = previousSeconds;
            throw failure;
        }
    }

    /** Atomically removes an inactive collar's pet row with the collar object itself. */
    synchronized boolean saveEconomyStateDeletingPet(
            String character, long points, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            long collarObjectId) throws IOException {
        if (inventory == null || itemStates == null || collarObjectId <= 0
                || collarObjectId > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(
                    "inventory, item states and collar object id are required");
        }
        Map<Long, Integer> normalizedInventory = normalizedPositiveCounts(inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> normalizedStates =
                normalizedItemStates(itemStates, normalizedInventory);
        MutableProfile profile = profile(character);
        long previousPoints = profile.points;
        Map<Long, Integer> previousInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> previousStates =
                mutableItemStates(profile.inventoryItemStates);
        PetState removed = pets.remove(collarObjectId);
        profile.points = Math.max(0, points);
        replacePositiveCounts(profile.inventory, normalizedInventory);
        replaceItemStates(profile.inventoryItemStates, normalizedStates);
        try {
            save();
            return removed != null;
        } catch (IOException | RuntimeException failure) {
            profile.points = previousPoints;
            replacePositiveCounts(profile.inventory, previousInventory);
            replaceItemStates(profile.inventoryItemStates, previousStates);
            if (removed != null) pets.put(collarObjectId, removed);
            throw failure;
        }
    }

    /** Atomically persists both sides of a native player-to-player shop transfer. */
    synchronized void saveEconomies(String firstCharacter, long firstPoints,
                                    Map<Long, Integer> firstInventory,
                                    String secondCharacter, long secondPoints,
                                    Map<Long, Integer> secondInventory) throws IOException {
        MutableProfile first = profile(firstCharacter);
        MutableProfile second = profile(secondCharacter);
        saveEconomyStates(firstCharacter, firstPoints, firstInventory,
                first.inventoryItemStates, secondCharacter, secondPoints,
                secondInventory, second.inventoryItemStates);
    }

    /** Atomically persists both complete native item sets after a player transfer. */
    synchronized void saveEconomyStates(
            String firstCharacter, long firstPoints, Map<Long, Integer> firstInventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> firstItemStates,
            String secondCharacter, long secondPoints, Map<Long, Integer> secondInventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> secondItemStates) throws IOException {
        if (firstInventory == null || secondInventory == null
                || firstItemStates == null || secondItemStates == null
                || key(firstCharacter).equals(key(secondCharacter))) {
            throw new IllegalArgumentException("two distinct private-shop economies are required");
        }
        MutableProfile first = profile(firstCharacter);
        MutableProfile second = profile(secondCharacter);
        long oldFirstPoints = first.points;
        long oldSecondPoints = second.points;
        Map<Long, Integer> oldFirstInventory = new LinkedHashMap<>(first.inventory);
        Map<Long, Integer> oldSecondInventory = new LinkedHashMap<>(second.inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> oldFirstStates =
                mutableItemStates(first.inventoryItemStates);
        Map<Long, List<RuntimeInventory.PersistentItem>> oldSecondStates =
                mutableItemStates(second.inventoryItemStates);
        first.points = Math.max(0, firstPoints);
        second.points = Math.max(0, secondPoints);
        replacePositiveCounts(first.inventory, firstInventory);
        replacePositiveCounts(second.inventory, secondInventory);
        replaceItemStates(first.inventoryItemStates,
                normalizedItemStates(firstItemStates, first.inventory));
        replaceItemStates(second.inventoryItemStates,
                normalizedItemStates(secondItemStates, second.inventory));
        try {
            save();
        } catch (IOException failure) {
            first.points = oldFirstPoints;
            second.points = oldSecondPoints;
            first.inventory.clear();
            first.inventory.putAll(oldFirstInventory);
            second.inventory.clear();
            second.inventory.putAll(oldSecondInventory);
            replaceItemStates(first.inventoryItemStates, oldFirstStates);
            replaceItemStates(second.inventoryItemStates, oldSecondStates);
            throw failure;
        }
    }

    /**
     * Atomically persists inventory and the native account-scoped personal
     * warehouse.  The expected map prevents two sessions from committing a
     * stale list capability over a newer warehouse state.
     */
    synchronized boolean saveInventoryAndPersonalWarehouse(
            String character, String account, Map<Long, Integer> inventory,
            Map<Long, Integer> expectedWarehouse, Map<Long, Integer> warehouse)
            throws IOException {
        return saveInventoryAndScopedWarehouseCounts(character, WarehouseScope.PERSONAL,
                account, inventory, expectedWarehouse, warehouse);
    }

    synchronized boolean saveInventoryAndPersonalWarehouse(
            String character, String account, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> inventoryItemStates,
            Warehouse expectedWarehouse, Map<Long, Integer> warehouse,
            Map<Long, List<RuntimeInventory.PersistentItem>> warehouseItemStates)
            throws IOException {
        return saveInventoryAndScopedWarehouse(character, WarehouseScope.PERSONAL,
                account, inventory, inventoryItemStates, expectedWarehouse,
                warehouse, warehouseItemStates);
    }

    synchronized boolean saveInventoryAndClanWarehouse(
            String character, String clanName, Map<Long, Integer> inventory,
            Map<Long, Integer> expectedWarehouse, Map<Long, Integer> warehouse)
            throws IOException {
        return saveInventoryAndScopedWarehouseCounts(character, WarehouseScope.CLAN,
                clanName, inventory, expectedWarehouse, warehouse);
    }

    synchronized boolean saveInventoryAndClanWarehouse(
            String character, String clanName, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> inventoryItemStates,
            Warehouse expectedWarehouse, Map<Long, Integer> warehouse,
            Map<Long, List<RuntimeInventory.PersistentItem>> warehouseItemStates)
            throws IOException {
        return saveInventoryAndScopedWarehouse(character, WarehouseScope.CLAN,
                clanName, inventory, inventoryItemStates, expectedWarehouse,
                warehouse, warehouseItemStates);
    }

    synchronized boolean saveInventoryAndElfWarehouse(
            String character, String account, Map<Long, Integer> inventory,
            Map<Long, Integer> expectedWarehouse, Map<Long, Integer> warehouse)
            throws IOException {
        return saveInventoryAndScopedWarehouseCounts(character, WarehouseScope.ELF,
                account, inventory, expectedWarehouse, warehouse);
    }

    synchronized boolean saveInventoryAndElfWarehouse(
            String character, String account, Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> inventoryItemStates,
            Warehouse expectedWarehouse, Map<Long, Integer> warehouse,
            Map<Long, List<RuntimeInventory.PersistentItem>> warehouseItemStates)
            throws IOException {
        return saveInventoryAndScopedWarehouse(character, WarehouseScope.ELF,
                account, inventory, inventoryItemStates, expectedWarehouse,
                warehouse, warehouseItemStates);
    }

    private boolean saveInventoryAndScopedWarehouseCounts(
            String character, WarehouseScope scope, String owner,
            Map<Long, Integer> inventory, Map<Long, Integer> expectedWarehouse,
            Map<Long, Integer> warehouse) throws IOException {
        if (expectedWarehouse == null) {
            throw new IllegalArgumentException("expected warehouse is required");
        }
        MutableProfile profile = profile(character);
        String normalizedOwner = warehouseOwner(owner, character);
        Map<Long, List<RuntimeInventory.PersistentItem>> currentStates =
                warehouseStateStore(scope).computeIfAbsent(
                        normalizedOwner, ignored -> new LinkedHashMap<>());
        return saveInventoryAndScopedWarehouse(character, scope, owner,
                inventory, profile.inventoryItemStates,
                new Warehouse(expectedWarehouse,
                        normalizedItemStates(currentStates, expectedWarehouse)),
                warehouse, currentStates);
    }

    private boolean saveInventoryAndScopedWarehouse(
            String character, WarehouseScope scope, String owner,
            Map<Long, Integer> inventory,
            Map<Long, List<RuntimeInventory.PersistentItem>> inventoryItemStates,
            Warehouse expectedWarehouse, Map<Long, Integer> warehouse,
            Map<Long, List<RuntimeInventory.PersistentItem>> warehouseItemStates)
            throws IOException {
        if (inventory == null || inventoryItemStates == null || expectedWarehouse == null
                || warehouse == null || warehouseItemStates == null) {
            throw new IllegalArgumentException("inventory and warehouse state is required");
        }
        MutableProfile profile = profile(character);
        String normalizedOwner = warehouseOwner(owner, character);
        Map<Long, Integer> currentWarehouse = warehouseStore(scope).computeIfAbsent(
                normalizedOwner, ignored -> new LinkedHashMap<>());
        Map<Long, List<RuntimeInventory.PersistentItem>> currentWarehouseStates =
                warehouseStateStore(scope).computeIfAbsent(
                        normalizedOwner, ignored -> new LinkedHashMap<>());
        Warehouse current = new Warehouse(currentWarehouse,
                normalizedItemStates(currentWarehouseStates, currentWarehouse));
        if (!current.equals(expectedWarehouse)) return false;
        Map<Long, Integer> oldInventory = new LinkedHashMap<>(profile.inventory);
        Map<Long, Integer> oldWarehouse = new LinkedHashMap<>(currentWarehouse);
        Map<Long, List<RuntimeInventory.PersistentItem>> oldInventoryStates =
                mutableItemStates(profile.inventoryItemStates);
        Map<Long, List<RuntimeInventory.PersistentItem>> oldWarehouseStates =
                mutableItemStates(currentWarehouseStates);
        replacePositiveCounts(profile.inventory, inventory);
        replacePositiveCounts(currentWarehouse, warehouse);
        replaceItemStates(profile.inventoryItemStates,
                normalizedItemStates(inventoryItemStates, profile.inventory));
        replaceItemStates(currentWarehouseStates,
                normalizedItemStates(warehouseItemStates, currentWarehouse));
        try {
            save();
        } catch (IOException failure) {
            profile.inventory.clear();
            profile.inventory.putAll(oldInventory);
            currentWarehouse.clear();
            currentWarehouse.putAll(oldWarehouse);
            replaceItemStates(profile.inventoryItemStates, oldInventoryStates);
            replaceItemStates(currentWarehouseStates, oldWarehouseStates);
            throw failure;
        }
        return true;
    }

    private Map<String, Map<Long, Integer>> warehouseStore(WarehouseScope scope) {
        if (scope == null) throw new IllegalArgumentException("warehouse scope is required");
        return switch (scope) {
            case PERSONAL -> accountWarehouses;
            case CLAN -> clanWarehouses;
            case ELF -> elfWarehouses;
        };
    }

    private Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>>
            warehouseStateStore(WarehouseScope scope) {
        if (scope == null) throw new IllegalArgumentException("warehouse scope is required");
        return switch (scope) {
            case PERSONAL -> accountWarehouseItemStates;
            case CLAN -> clanWarehouseItemStates;
            case ELF -> elfWarehouseItemStates;
        };
    }

    /** Atomically persists the Adena charge and the newly learned LiTo skills. */
    synchronized void saveSpellPurchase(String character, long points,
                                        Map<Long, Integer> inventory,
                                        Set<Integer> learnedSkillIds) throws IOException {
        if (learnedSkillIds == null || learnedSkillIds.stream().anyMatch(
                skillId -> skillId == null || skillId <= 0
                        || skillId > SpellShopCatalog.MAX_NATIVE_SKILL_ID
                        || BattleSpellCatalog.reservedSkillSlot(skillId) != null)) {
            throw new IllegalArgumentException("invalid durable spell-shop skill set");
        }
        MutableProfile profile = profile(character);
        long oldPoints = profile.points;
        Map<Long, Integer> oldInventory = new LinkedHashMap<>(profile.inventory);
        Set<Integer> oldSkills = new LinkedHashSet<>(profile.purchasedSkills);
        profile.points = Math.max(0, points);
        profile.inventory.clear();
        inventory.forEach((item, count) -> {
            if (item > 0 && count != null && count > 0) profile.inventory.put(item, count);
        });
        profile.purchasedSkills.clear();
        profile.purchasedSkills.addAll(learnedSkillIds);
        try {
            save();
        } catch (IOException failure) {
            profile.points = oldPoints;
            profile.inventory.clear();
            profile.inventory.putAll(oldInventory);
            profile.purchasedSkills.clear();
            profile.purchasedSkills.addAll(oldSkills);
            throw failure;
        }
    }

    synchronized TournamentStatistics tournamentStatistics(String character) {
        MutableProfile profile = profile(character);
        return tournamentStatistics(profile);
    }

    /** Tournament excludes source consumables without consuming their SQL rows.
     * Drop only absent, non-consumed mappings for those excluded items before reimport.
     * Equipment, prizes, private inventory and consumption tombstones remain strict. */
    synchronized SourceInventoryReconciliation reconcileTournamentSourceInventory(
            String character, List<SourceInventoryItem> sourceItems,
            SourceEquipmentMagicStats equipment, boolean tournament) throws IOException {
        if (!tournament) return reconcileSourceInventory(character, sourceItems, equipment);
        MutableProfile current = profile(character);
        Map<Long, MutableSourceInventory> oldMappings = new LinkedHashMap<>(current.sourceInventory);
        Map<Long, MutableSourceProjectile> oldProjectiles = new LinkedHashMap<>(current.sourceProjectiles);
        Set<Long> present = new HashSet<>();
        for (List<RuntimeInventory.PersistentItem> items : current.inventoryItemStates.values())
            for (RuntimeInventory.PersistentItem item : items) present.add(item.objectId());
        try {
            for (SourceInventoryItem source : sourceItems) {
                if (source.sourceWeaponImportable() || source.sourceArmorImportable()
                        || source.tournamentReward() || source.litoOwned()) continue;
                MutableSourceInventory mapped = current.sourceInventory.get(source.sourceObjectId());
                if (mapped != null && !mapped.consumed && !present.contains(mapped.localObjectId)) {
                    current.sourceInventory.remove(source.sourceObjectId());
                    current.sourceProjectiles.remove(source.sourceObjectId());
                }
            }
            return reconcileSourceInventory(character, sourceItems, equipment);
        } catch (IOException | RuntimeException failure) {
            current.sourceInventory.clear(); current.sourceInventory.putAll(oldMappings);
            current.sourceProjectiles.clear(); current.sourceProjectiles.putAll(oldProjectiles);
            throw failure;
        }
    }

    synchronized TournamentProfileState tournamentProfileState(String character) {
        return tournamentProfileState(profile(character));
    }

    /** Initializes unbound SQL roster entries without changing authoritative snapshot boundaries. */
    synchronized void initializeTournamentRosterDefaults(
            Map<String, TournamentProfileState> defaults) throws IOException {
        if (defaults == null) throw new IllegalArgumentException("Tournament defaults are required");
        for (Map.Entry<String, TournamentProfileState> entry : defaults.entrySet()) {
            TournamentProfileState initial = entry.getValue();
            String source = profileIdentity(entry.getKey()).source();
            if (initial == null || source.isEmpty()
                    || !source.equalsIgnoreCase(initial.classicServerName())
                    || initial.rankingSnapshotPresent() || initial.monthlyRankingSnapshotPresent()) {
                throw new IllegalArgumentException("invalid source-qualified Tournament default");
            }
        }
        Map<MutableProfile, TournamentProfileState> previous = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, TournamentProfileState> entry : defaults.entrySet()) {
                MutableProfile current = profile(entry.getKey());
                if (!current.classicServerName.isEmpty()) {
                    if (!current.classicServerName.equalsIgnoreCase(entry.getValue().classicServerName()))
                        throw new IllegalArgumentException("Tournament default source differs from current profile");
                    continue;
                }
                previous.put(current, tournamentProfileState(current));
                installTournamentProfileState(current, entry.getValue());
            }
            if (!previous.isEmpty()) save();
        } catch (IOException | RuntimeException failure) {
            for (Map.Entry<MutableProfile, TournamentProfileState> entry : previous.entrySet())
                installTournamentProfileState(entry.getKey(), entry.getValue());
            throw failure;
        }
    }

    synchronized void reconcileTournamentProfileStates(
            Map<String, TournamentProfileState> authoritative) throws IOException {
        if (authoritative == null) {
            throw new IllegalArgumentException("Tournament profile map is required");
        }
        Map<MutableProfile, TournamentProfileState> previous = new LinkedHashMap<>();
        long oldBoundary = rankingSnapshotBoundaryEpochSeconds;
        long oldMonthlyBoundary = monthlyRankingSnapshotBoundaryEpochSeconds;
        try {
            long boundary = Long.MAX_VALUE;
            long monthlyBoundary = Long.MAX_VALUE;
            for (Map.Entry<String, TournamentProfileState> entry : authoritative.entrySet()) {
                TournamentProfileState replacement = entry.getValue();
                if (replacement == null) {
                    throw new IllegalArgumentException("null Tournament profile state");
                }
                MutableProfile profile = profile(entry.getKey());
                previous.put(profile, tournamentProfileState(profile));
                installTournamentProfileState(profile, replacement);
                boundary = Math.min(boundary,
                        replacement.rankingSnapshotBoundaryEpochSeconds());
                monthlyBoundary = Math.min(monthlyBoundary,
                        replacement.monthlyRankingSnapshotBoundaryEpochSeconds());
            }
            if (!authoritative.isEmpty()) {
                rankingSnapshotBoundaryEpochSeconds = boundary;
                monthlyRankingSnapshotBoundaryEpochSeconds = monthlyBoundary;
            }
            save();
        } catch (IOException | RuntimeException failure) {
            previous.forEach(GameStateStore::installTournamentProfileState);
            rankingSnapshotBoundaryEpochSeconds = oldBoundary;
            monthlyRankingSnapshotBoundaryEpochSeconds = oldMonthlyBoundary;
            throw failure;
        }
    }

    synchronized TournamentAwards tournamentAwards(String character) {
        MutableProfile profile = profile(character);
        return tournamentAwards(profile);
    }

    /** Replaces an externally adjudicated native award profile atomically. */
    synchronized void replaceTournamentAwards(String character,
                                               TournamentAwards awards)
            throws IOException {
        if (awards == null) {
            throw new IllegalArgumentException("Tournament awards are required");
        }
        MutableProfile profile = profile(character);
        int[] oldLegacy = profile.tournamentLegacyCertificateCodes.clone();
        int[] oldVictory = profile.tournamentVictoryBadgeEditions.clone();
        boolean oldMaster = profile.tournamentLitoMaster;
        for (int index = 0; index < TOURNAMENT_LEGACY_CERTIFICATE_SLOTS; index++) {
            profile.tournamentLegacyCertificateCodes[index] =
                    awards.legacyCertificateCodes().get(index);
        }
        for (int index = 0; index < TOURNAMENT_VICTORY_BADGE_MODE_COUNT; index++) {
            profile.tournamentVictoryBadgeEditions[index] =
                    awards.victoryBadgeEditions().get(index);
        }
        profile.tournamentLitoMaster = awards.litoMaster();
        try {
            save();
        } catch (IOException | RuntimeException failure) {
            System.arraycopy(oldLegacy, 0,
                    profile.tournamentLegacyCertificateCodes, 0,
                    TOURNAMENT_LEGACY_CERTIFICATE_SLOTS);
            System.arraycopy(oldVictory, 0,
                    profile.tournamentVictoryBadgeEditions, 0,
                    TOURNAMENT_VICTORY_BADGE_MODE_COUNT);
            profile.tournamentLitoMaster = oldMaster;
            throw failure;
        }
    }

    /**
     * Binds read-only source-character dimensions used by native ranking partitions.
     * These values live only in the LiTo sidecar and never write to the legacy DB.
     */
    synchronized void bindRankingIdentity(String character, String classicServerName,
                                          int channel, int level, int race)
            throws IOException {
        String source = classicServerName == null ? "" : classicServerName.trim();
        if (source.isEmpty() || source.length() > 63
                || channel < 0 || channel > 4 || level < 0 || level > 0xFF
                || race < 0 || race > 6) {
            throw new IllegalArgumentException("invalid Tournament ranking identity");
        }
        MutableProfile profile = profile(character);
        if (profile.classicServerName.equals(source)
                && profile.channel == channel && profile.sourceLevel == level
                && profile.race == race) {
            return;
        }
        profile.classicServerName = source;
        profile.channel = channel;
        profile.sourceLevel = level;
        profile.race = race;
        save();
    }

    synchronized Progress recordResult(String character, boolean won, int pointReward)
            throws IOException {
        return recordResult(character, won, false, pointReward, 0);
    }

    synchronized Progress recordResult(String character, boolean won, boolean disconnected,
                                       int pointReward) throws IOException {
        return recordResult(character, won, disconnected, pointReward, 0);
    }

    synchronized Progress recordResult(String character, boolean won, boolean disconnected,
                                       int pointReward, int experienceBonusPercent)
            throws IOException {
        return recordResult(character, won, disconnected, pointReward,
                experienceBonusPercent, null);
    }

    synchronized Progress recordResult(String character, boolean won, boolean disconnected,
                                       int pointReward, int experienceBonusPercent,
                                       TournamentProfilePersistenceHook persistence)
            throws IOException {
        if (won && disconnected) {
            throw new IllegalArgumentException("a disconnected result cannot be a win");
        }
        if (experienceBonusPercent < 0 || experienceBonusPercent > 100) {
            throw new IllegalArgumentException("invalid result experience bonus");
        }
        MutableProfile profile = profile(character);
        TournamentProfileState previousState = tournamentProfileState(profile);
        int previousWins = profile.wins;
        int previousLosses = profile.losses;
        int previousDisconnects = profile.disconnects;
        long previousRankingPoints = profile.rankingPoints;
        int previousMonthlyPeriod = profile.monthlyRankingPeriod;
        long previousMonthlyRankingPoints = profile.monthlyRankingPoints;
        int previousMonthlyWins = profile.monthlyWins;
        int previousMonthlyLosses = profile.monthlyLosses;
        long previousExperience = profile.experience;
        int previousLevel = profile.level;
        boolean externallyCommitted = false;
        try {
            ensureMonthlyAccumulator(profile, currentRankingPeriodKey());
            if (won) {
                profile.wins = Math.incrementExact(profile.wins);
                profile.monthlyWins = Math.incrementExact(profile.monthlyWins);
            } else {
                profile.losses = Math.incrementExact(profile.losses);
                profile.monthlyLosses = Math.incrementExact(profile.monthlyLosses);
                if (disconnected) {
                    profile.disconnects = Math.incrementExact(profile.disconnects);
                }
            }
            // The selected 2007 rules award a smaller positive score for an
            // ordinary loss; only forced disconnects lose score.  Exact
            // level/contribution magnitudes remain operator-configured.
            long signedPointDelta = disconnected
                    ? -Math.max(0, pointReward) : Math.max(0, pointReward);
            profile.rankingPoints = Math.max(0,
                    Math.addExact(profile.rankingPoints, signedPointDelta));
            profile.monthlyRankingPoints = Math.max(0,
                    Math.addExact(profile.monthlyRankingPoints, signedPointDelta));
            long baseExperience = won ? 100 : 25;
            long awardedExperience = BattleCookingCatalog.experienceAfterBonus(
                    baseExperience, experienceBonusPercent);
            profile.experience = Math.addExact(profile.experience, awardedExperience);
            profile.level = Math.max(profile.level,
                    1 + (int) Math.min(98, profile.experience / 1_000));
            tournamentStatistics(profile);
            if (persistence != null) {
                persistence.persist(previousState, tournamentProfileState(profile));
                externallyCommitted = true;
            }
            save();
        } catch (IOException | RuntimeException failure) {
            if (!externallyCommitted) {
                profile.wins = previousWins;
                profile.losses = previousLosses;
                profile.disconnects = previousDisconnects;
                profile.rankingPoints = previousRankingPoints;
                profile.monthlyRankingPeriod = previousMonthlyPeriod;
                profile.monthlyRankingPoints = previousMonthlyRankingPoints;
                profile.monthlyWins = previousMonthlyWins;
                profile.monthlyLosses = previousMonthlyLosses;
                profile.experience = previousExperience;
                profile.level = previousLevel;
            }
            throw failure;
        }
        return new Progress(profile.points, profile.rankingPoints,
                profile.wins, profile.losses,
                profile.disconnects, profile.experience, profile.level);
    }

    private static TournamentStatistics tournamentStatistics(MutableProfile profile) {
        long denominator = (long) profile.wins + profile.losses;
        if (denominator > Integer.MAX_VALUE) {
            throw new IllegalStateException("Tournament result count exceeds native signed dword");
        }
        return new TournamentStatistics((int) denominator, profile.wins, profile.losses,
                profile.disconnects);
    }

    private static TournamentAwards tournamentAwards(MutableProfile profile) {
        List<Integer> legacy = new ArrayList<>(TOURNAMENT_LEGACY_CERTIFICATE_SLOTS);
        for (int value : profile.tournamentLegacyCertificateCodes) legacy.add(value);
        List<Integer> victory = new ArrayList<>(TOURNAMENT_VICTORY_BADGE_MODE_COUNT);
        for (int value : profile.tournamentVictoryBadgeEditions) victory.add(value);
        return new TournamentAwards(legacy, victory, profile.tournamentLitoMaster);
    }

    private TournamentProfileState tournamentProfileState(MutableProfile profile) {
        return new TournamentProfileState(
                profile.rankingPoints, profile.wins, profile.losses,
                profile.disconnects, profile.monthlyRankingPeriod,
                profile.monthlyRankingPoints, profile.monthlyWins,
                profile.monthlyLosses,
                java.util.Arrays.stream(profile.tournamentLegacyCertificateCodes)
                        .boxed().toList(),
                java.util.Arrays.stream(profile.tournamentVictoryBadgeEditions)
                        .boxed().toList(),
                profile.tournamentLitoMaster, profile.classicServerName,
                profile.channel, profile.sourceLevel, profile.race,
                profile.rankingSnapshotPresent, profile.snapshotRankingPoints,
                profile.snapshotWins, profile.snapshotLosses,
                profile.snapshotClassicServerName, profile.snapshotChannel,
                profile.snapshotSourceLevel, profile.snapshotRace,
                profile.monthlyRankingSnapshotPresent,
                profile.snapshotMonthlyRankingPeriod,
                profile.snapshotMonthlyRankingPoints, profile.snapshotMonthlyWins,
                profile.snapshotMonthlyLosses, profile.snapshotMonthlyMovement,
                profile.snapshotOverallMovement, profile.snapshotClassicServerMovement,
                profile.snapshotChannelMovement, profile.snapshotLevelMovement,
                profile.snapshotRaceMovement, profile.experience, profile.level,
                rankingSnapshotBoundaryEpochSeconds,
                monthlyRankingSnapshotBoundaryEpochSeconds);
    }

    private static void installTournamentProfileState(
            MutableProfile profile, TournamentProfileState state) {
        profile.rankingPoints = state.rankingPoints();
        profile.wins = state.wins();
        profile.losses = state.losses();
        profile.disconnects = state.disconnects();
        profile.monthlyRankingPeriod = state.monthlyPeriod();
        profile.monthlyRankingPoints = state.monthlyRankingPoints();
        profile.monthlyWins = state.monthlyWins();
        profile.monthlyLosses = state.monthlyLosses();
        for (int index = 0; index < TOURNAMENT_LEGACY_CERTIFICATE_SLOTS; index++) {
            profile.tournamentLegacyCertificateCodes[index] =
                    state.legacyCertificateCodes().get(index);
        }
        for (int index = 0; index < TOURNAMENT_VICTORY_BADGE_MODE_COUNT; index++) {
            profile.tournamentVictoryBadgeEditions[index] =
                    state.victoryBadgeEditions().get(index);
        }
        profile.tournamentLitoMaster = state.litoMaster();
        profile.classicServerName = state.classicServerName();
        profile.channel = state.channel();
        profile.sourceLevel = state.sourceLevel();
        profile.race = state.race();
        profile.rankingSnapshotPresent = state.rankingSnapshotPresent();
        profile.snapshotRankingPoints = state.snapshotRankingPoints();
        profile.snapshotWins = state.snapshotWins();
        profile.snapshotLosses = state.snapshotLosses();
        profile.snapshotClassicServerName = state.snapshotClassicServerName();
        profile.snapshotChannel = state.snapshotChannel();
        profile.snapshotSourceLevel = state.snapshotSourceLevel();
        profile.snapshotRace = state.snapshotRace();
        profile.monthlyRankingSnapshotPresent =
                state.monthlyRankingSnapshotPresent();
        profile.snapshotMonthlyRankingPeriod = state.snapshotMonthlyPeriod();
        profile.snapshotMonthlyRankingPoints = state.snapshotMonthlyRankingPoints();
        profile.snapshotMonthlyWins = state.snapshotMonthlyWins();
        profile.snapshotMonthlyLosses = state.snapshotMonthlyLosses();
        profile.snapshotMonthlyMovement = state.snapshotMonthlyMovement();
        profile.snapshotOverallMovement = state.snapshotOverallMovement();
        profile.snapshotClassicServerMovement = state.snapshotClassicServerMovement();
        profile.snapshotChannelMovement = state.snapshotChannelMovement();
        profile.snapshotLevelMovement = state.snapshotLevelMovement();
        profile.snapshotRaceMovement = state.snapshotRaceMovement();
        profile.experience = state.experience();
        profile.level = state.level();
    }

    private static boolean validTournamentLegacyCertificate(int code) {
        if (code == 0) return true;
        if (code < 0 || code > 0xFF || (code >>> 4) == 0) return false;
        int material = code & 0x0F;
        return material == 1 || material == 2 || material == 4;
    }

    synchronized List<Ranking> ranking(Protocol.LitoRankingScope requestedScope,
                                       RankingQuery query, int limit) throws IOException {
        ensureRankingSnapshot();
        Protocol.LitoRankingScope scope = requestedScope == Protocol.LitoRankingScope.OPEN
                ? Protocol.LitoRankingScope.OVERALL : requestedScope;
        List<MutableProfile> ranked = scope == Protocol.LitoRankingScope.MONTHLY
                ? rankedMonthlyProfiles() : rankedProfiles(scope, query);
        return ranked.stream()
                .limit(Math.max(0, limit))
                // Native sub_4AA380 converts this byte to the class icon type.
                // sub_4638C0 then reads LTlist[type], except class type 5 which
                // deliberately uses the later-added LTlist[45] resource.
                .map(p -> new Ranking(qualifiedRankingName(p), p.snapshotRace,
                        scope == Protocol.LitoRankingScope.MONTHLY
                                ? p.snapshotMonthlyWins : p.snapshotWins,
                        scope == Protocol.LitoRankingScope.MONTHLY
                                ? p.snapshotMonthlyLosses : p.snapshotLosses,
                        movementFor(p, scope)))
                .toList();
    }

    synchronized Protocol.LitoCharacterRankings characterRankings(String character)
            throws IOException {
        ensureRankingSnapshot();
        MutableProfile profile = profiles.get(key(character));
        if (profile == null || !rankEligible(profile)
                || profile.snapshotClassicServerName.isEmpty()
                || profile.snapshotChannel < 0 || profile.snapshotSourceLevel < 0
                || profile.snapshotRace < 0) {
            return Protocol.LitoCharacterRankings.unknown();
        }
        RankingQuery query = new RankingQuery(profile.snapshotClassicServerName,
                profile.snapshotChannel, profile.snapshotSourceLevel, profile.snapshotRace);
        return new Protocol.LitoCharacterRankings(
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.OVERALL, query)),
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.CLASSIC_SERVER, query)),
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.CHANNEL, query)),
                rankOf(profile, rankedProfilesByLevel(query)),
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.RACE, query)),
                profile.snapshotOverallMovement,
                profile.snapshotClassicServerMovement,
                profile.snapshotChannelMovement,
                profile.snapshotLevelMovement,
                profile.snapshotRaceMovement);
    }

    private List<MutableProfile> rankedProfiles(Protocol.LitoRankingScope scope,
                                                RankingQuery query) {
        return profiles.values().stream()
                .filter(GameStateStore::rankEligible)
                .filter(profile -> switch (scope) {
                    case OPEN, OVERALL -> true;
                    case CLASSIC_SERVER -> query != null
                            && profile.snapshotClassicServerName.equalsIgnoreCase(
                                    query.classicServerName());
                    case CHANNEL -> query != null
                            && profile.snapshotChannel == query.channel();
                    case RACE -> query != null && profile.snapshotRace == query.race();
                    case MONTHLY -> false;
                })
                // Official LiTo material states that score directly determines
                // rank. Wins/losses are display columns, not the primary sort key.
                .sorted(Comparator.comparingLong(
                                (MutableProfile p) -> p.snapshotRankingPoints).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (MutableProfile p) -> p.snapshotWins).reversed())
                        .thenComparing(p -> p.name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Calendar-month score is independent from the season/lifetime score.
     *
     * <p>The archived official 2007 Monthly table keeps the already-qualified
     * top-100 pool even when a row has 0/0 current-month results.  Eligibility
     * therefore remains the official twenty lifetime matches, while ordering
     * and displayed wins/losses come from the current monthly accumulator.
     * The original tie predicate is not published; lifetime score then name is
     * the explicit deterministic server policy after monthly score and wins.</p>
     */
    private List<MutableProfile> rankedMonthlyProfiles() {
        return profiles.values().stream()
                .filter(GameStateStore::rankEligible)
                .filter(profile -> profile.monthlyRankingSnapshotPresent)
                .sorted(Comparator.comparingLong(
                                (MutableProfile p) -> p.snapshotMonthlyRankingPoints)
                                .reversed()
                        .thenComparing(Comparator.comparingInt(
                                (MutableProfile p) -> p.snapshotMonthlyWins).reversed())
                        .thenComparing(Comparator.comparingLong(
                                (MutableProfile p) -> p.snapshotRankingPoints).reversed())
                        .thenComparing(p -> p.name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<MutableProfile> rankedProfilesByLevel(RankingQuery query) {
        if (query == null) return List.of();
        return profiles.values().stream()
                .filter(GameStateStore::rankEligible)
                .filter(profile -> profile.snapshotSourceLevel == query.level())
                .sorted(Comparator.comparingLong(
                                (MutableProfile p) -> p.snapshotRankingPoints).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (MutableProfile p) -> p.snapshotWins).reversed())
                        .thenComparing(p -> p.name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static boolean rankEligible(MutableProfile profile) {
        return profile.rankingSnapshotPresent
                && (long) profile.snapshotWins + profile.snapshotLosses
                >= MINIMUM_RANKED_MATCHES;
    }

    private static int rankOf(MutableProfile target, List<MutableProfile> ranking) {
        for (int index = 0; index < ranking.size(); index++) {
            if (ranking.get(index) == target) return index + 1;
        }
        return 0;
    }

    private static int movementFor(MutableProfile profile,
                                   Protocol.LitoRankingScope scope) {
        return switch (scope) {
            case OPEN, OVERALL -> profile.snapshotOverallMovement;
            case CLASSIC_SERVER -> profile.snapshotClassicServerMovement;
            case CHANNEL -> profile.snapshotChannelMovement;
            case RACE -> profile.snapshotRaceMovement;
            case MONTHLY -> profile.snapshotMonthlyMovement;
        };
    }

    private int[] snapshotRanks(MutableProfile profile) {
        if (!rankEligible(profile)) return new int[5];
        RankingQuery query = new RankingQuery(profile.snapshotClassicServerName,
                profile.snapshotChannel, profile.snapshotSourceLevel, profile.snapshotRace);
        return new int[] {
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.OVERALL, query)),
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.CLASSIC_SERVER, query)),
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.CHANNEL, query)),
                rankOf(profile, rankedProfilesByLevel(query)),
                rankOf(profile, rankedProfiles(Protocol.LitoRankingScope.RACE, query))
        };
    }

    /** Materializes the latest official daily 05:00 ranking boundary. */
    private void ensureRankingSnapshot() throws IOException {
        LiveBattleStats.beforeSnapshot(this);
        ZonedDateTime boundary = currentRankingBoundary();
        long boundaryEpochSeconds = boundary.toEpochSecond();
        boolean refreshLifetime = rankingSnapshotBoundaryEpochSeconds
                < boundaryEpochSeconds;
        boolean refreshMonthly = monthlyRankingSnapshotBoundaryEpochSeconds
                < boundaryEpochSeconds;
        if (!refreshLifetime && !refreshMonthly) return;

        Map<MutableProfile, int[]> previousRanks = new LinkedHashMap<>();
        if (refreshLifetime && rankingSnapshotBoundaryEpochSeconds > 0) {
            for (MutableProfile profile : profiles.values()) {
                previousRanks.put(profile, snapshotRanks(profile));
            }
        }

        int monthlyPeriod = rankingPeriodKey(boundary);
        Map<MutableProfile, Integer> previousMonthlyRanks = new LinkedHashMap<>();
        if (refreshMonthly && monthlyRankingSnapshotBoundaryEpochSeconds > 0) {
            List<MutableProfile> previousMonthly = rankedMonthlyProfiles();
            for (MutableProfile profile : profiles.values()) {
                if (profile.snapshotMonthlyRankingPeriod == monthlyPeriod) {
                    previousMonthlyRanks.put(profile, rankOf(profile, previousMonthly));
                }
            }
        }

        if (refreshLifetime) {
            for (MutableProfile profile : profiles.values()) {
                profile.rankingSnapshotPresent = true;
                profile.snapshotRankingPoints = profile.rankingPoints;
                profile.snapshotWins = profile.wins;
                profile.snapshotLosses = profile.losses;
                profile.snapshotClassicServerName = profile.classicServerName;
                profile.snapshotChannel = profile.channel;
                profile.snapshotSourceLevel = profile.sourceLevel;
                profile.snapshotRace = profile.race;
            }
            for (MutableProfile profile : profiles.values()) {
                int[] previous = previousRanks.getOrDefault(profile, new int[5]);
                int[] current = snapshotRanks(profile);
                profile.snapshotOverallMovement = rankMovement(previous[0], current[0]);
                profile.snapshotClassicServerMovement = rankMovement(previous[1], current[1]);
                profile.snapshotChannelMovement = rankMovement(previous[2], current[2]);
                profile.snapshotLevelMovement = rankMovement(previous[3], current[3]);
                profile.snapshotRaceMovement = rankMovement(previous[4], current[4]);
            }
            rankingSnapshotBoundaryEpochSeconds = boundaryEpochSeconds;
        }

        if (refreshMonthly) {
            for (MutableProfile profile : profiles.values()) {
                ensureMonthlyAccumulator(profile, monthlyPeriod);
                profile.monthlyRankingSnapshotPresent = true;
                profile.snapshotMonthlyRankingPeriod = monthlyPeriod;
                profile.snapshotMonthlyRankingPoints = profile.monthlyRankingPoints;
                profile.snapshotMonthlyWins = profile.monthlyWins;
                profile.snapshotMonthlyLosses = profile.monthlyLosses;
            }
            List<MutableProfile> currentMonthly = rankedMonthlyProfiles();
            for (MutableProfile profile : profiles.values()) {
                int previous = previousMonthlyRanks.getOrDefault(profile, 0);
                int current = rankOf(profile, currentMonthly);
                profile.snapshotMonthlyMovement = rankMovement(previous, current);
            }
            monthlyRankingSnapshotBoundaryEpochSeconds = boundaryEpochSeconds;
        }
        save();
    }

    private ZonedDateTime currentRankingBoundary() {
        ZonedDateTime now = Instant.ofEpochMilli(clock.millis()).atZone(RANKING_ZONE);
        ZonedDateTime boundary = now.toLocalDate().atTime(RANKING_REFRESH_TIME)
                .atZone(RANKING_ZONE);
        return now.isBefore(boundary) ? boundary.minusDays(1) : boundary;
    }

    private int currentRankingPeriodKey() {
        return rankingPeriodKey(currentRankingBoundary());
    }

    private static int rankingPeriodKey(ZonedDateTime boundary) {
        return Math.addExact(Math.multiplyExact(boundary.getYear(), 100),
                boundary.getMonthValue());
    }

    private static boolean validRankingPeriod(int period) {
        int year = period / 100;
        int month = period % 100;
        return year >= 2000 && year <= 9999 && month >= 1 && month <= 12;
    }

    private static void ensureMonthlyAccumulator(MutableProfile profile, int period) {
        if (profile.monthlyRankingPeriod == period) return;
        profile.monthlyRankingPeriod = period;
        profile.monthlyRankingPoints = 0;
        profile.monthlyWins = 0;
        profile.monthlyLosses = 0;
    }

    /** Native S_D9 evaluates {@code currentRank + movement} as the old rank. */
    private static int rankMovement(int previousRank, int currentRank) {
        return previousRank > 0 && currentRank > 0 ? previousRank - currentRank : 0;
    }

    private static String qualifiedRankingName(MutableProfile profile) {
        return profile.snapshotClassicServerName.isEmpty()
                ? profile.name
                : profile.name + "(" + profile.snapshotClassicServerName + ")";
    }

    synchronized List<CharacterSummary> createdCharacters(String account) {
        if (account == null || account.isBlank()) return List.of();
        return List.copyOf(createdCharacters.getOrDefault(key(account), List.of()));
    }

    synchronized boolean createdCharacterExists(String name) {
        if (name == null || name.isBlank()) return false;
        return createdCharacters.values().stream().flatMap(List::stream)
                .anyMatch(character -> character.name().equalsIgnoreCase(name));
    }

    synchronized boolean createdCharacterExists(String accountScope, String name) {
        if (accountScope == null || accountScope.isBlank()
                || name == null || name.isBlank()) return false;
        String namespace = sourceNamespace(accountScope);
        return createdCharacters.entrySet().stream()
                .filter(entry -> sourceNamespace(entry.getKey()).equals(namespace))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(character -> character.name().equalsIgnoreCase(name));
    }

    synchronized CharacterCreation createCharacter(String account,
                                                    Protocol.CustomCharacterRequest request,
                                                    int originalCharacterCount) throws IOException {
        if (account == null || account.isBlank() || request == null) {
            throw new IllegalArgumentException("account and native character request are required");
        }
        String namespace = sourceNamespace(account);
        boolean duplicate = createdCharacters.entrySet().stream()
                .filter(entry -> sourceNamespace(entry.getKey()).equals(namespace))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(character -> character.name().equalsIgnoreCase(request.name()));
        if (duplicate) return new CharacterCreation(null, CreationFailure.DUPLICATE_NAME);
        List<CharacterSummary> accountCharacters = createdCharacters.computeIfAbsent(
                key(account), ignored -> new ArrayList<>());
        if (Math.max(0, originalCharacterCount) + accountCharacters.size()
                >= MAX_CHARACTERS_PER_ACCOUNT) {
            return new CharacterCreation(null, CreationFailure.ACCOUNT_LIMIT);
        }

        List<Integer> attributes = request.attributes();
        long objectId = allocateCreatedObjectId();
        CharacterSummary character = new CharacterSummary(
                request.name(), "", request.characterClass(), request.sex(),
                1, 50, 10, 10,
                attributes.get(0), attributes.get(2), attributes.get(1),
                attributes.get(4), attributes.get(5), attributes.get(3),
                attributes.get(5), attributes.get(3),
                objectId, 0, 0, 0, request.characterClass(), 32600, 32700, 4);
        accountCharacters.add(character);
        save();
        return new CharacterCreation(character, CreationFailure.NONE);
    }

    /**
     * Deletes only a character created in the LiTo sidecar.  Legacy SQL
     * characters are never represented in {@code createdCharacters}, so this
     * mutation cannot write or delete the official character repository.
     */
    synchronized CharacterDeletion deleteCreatedCharacter(String account, String name)
            throws IOException {
        if (account == null || account.isBlank() || name == null || name.isBlank()) {
            return new CharacterDeletion(null, DeletionStatus.NOT_FOUND);
        }
        String accountKey = key(account.trim());
        String characterNameKey = key(name.trim());
        String characterKey = profileStorageKey(profileIdentity(account).source(), name.trim());
        List<CharacterSummary> accountCharacters = createdCharacters.get(accountKey);
        CharacterSummary character = accountCharacters == null ? null : accountCharacters.stream()
                .filter(entry -> key(entry.name()).equals(characterNameKey))
                .findFirst().orElse(null);
        if (character == null) {
            String namespace = sourceNamespace(account);
            boolean belongsToAnotherAccount = createdCharacters.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(accountKey))
                    .filter(entry -> sourceNamespace(entry.getKey()).equals(namespace))
                    .flatMap(entry -> entry.getValue().stream())
                    .anyMatch(entry -> key(entry.name()).equals(characterNameKey));
            return new CharacterDeletion(null, belongsToAnotherAccount
                    ? DeletionStatus.NOT_OWNED : DeletionStatus.NOT_FOUND);
        }

        List<CharacterSummary> accountBefore = new ArrayList<>(accountCharacters);
        MutableProfile removedProfile = profiles.remove(characterKey);
        MutableProfile partner = removedProfile == null || removedProfile.partner.isBlank()
                ? null : profiles.get(removedProfile.partnerStorageKey);
        String partnerBefore = partner == null ? "" : partner.partner;
        String partnerStorageKeyBefore = partner == null ? "" : partner.partnerStorageKey;
        Map<String, HouseAuction> auctionsBefore = new LinkedHashMap<>();
        Map<String, String> auctionBidderKeysBefore = new LinkedHashMap<>();

        accountCharacters.remove(character);
        if (accountCharacters.isEmpty()) createdCharacters.remove(accountKey);
        if (partner != null && partner.partner.equalsIgnoreCase(character.name())) {
            partner.partner = "";
            partner.partnerStorageKey = "";
        }
        for (Map.Entry<String, MutableHouseAuction> entry : houseAuctions.entrySet()) {
            MutableHouseAuction auction = entry.getValue();
            if (!auction.bidderStorageKey.equals(characterKey)) continue;
            auctionsBefore.put(entry.getKey(), auction.snapshot());
            auctionBidderKeysBefore.put(entry.getKey(), auction.bidderStorageKey);
            auction.bidder = "";
            auction.bidderStorageKey = "";
            auction.bidderObjectId = 0;
            auction.revision++;
        }

        try {
            save();
        } catch (IOException failure) {
            createdCharacters.put(accountKey, accountBefore);
            if (removedProfile != null) profiles.put(characterKey, removedProfile);
            if (partner != null) {
                partner.partner = partnerBefore;
                partner.partnerStorageKey = partnerStorageKeyBefore;
            }
            auctionsBefore.forEach((storageKey, snapshot) -> {
                MutableHouseAuction auction = houseAuctions.get(storageKey);
                if (auction != null) {
                    auction.restore(snapshot);
                    auction.bidderStorageKey = auctionBidderKeysBefore.getOrDefault(
                            storageKey, "");
                }
            });
            throw failure;
        }
        return new CharacterDeletion(character, DeletionStatus.DELETED);
    }

    synchronized List<Bookmark> bookmarks(String character) {
        return List.copyOf(profile(character).bookmarks.values());
    }

    synchronized void saveBookmark(String character, Bookmark bookmark) throws IOException {
        if (bookmark == null || bookmark.name() == null || bookmark.name().isBlank()) {
            throw new IllegalArgumentException("bookmark is required");
        }
        MutableProfile profile = profile(character);
        String bookmarkKey = key(bookmark.name());
        Bookmark previous = profile.bookmarks.put(bookmarkKey, bookmark);
        try {
            save();
        } catch (IOException failure) {
            if (previous == null) profile.bookmarks.remove(bookmarkKey);
            else profile.bookmarks.put(bookmarkKey, previous);
            throw failure;
        }
    }

    synchronized void deleteBookmark(String character, String name) throws IOException {
        if (name == null || name.isBlank()) return;
        MutableProfile profile = profile(character);
        String bookmarkKey = key(name);
        Bookmark previous = profile.bookmarks.remove(bookmarkKey);
        if (previous == null) return;
        try {
            save();
        } catch (IOException failure) {
            profile.bookmarks.put(bookmarkKey, previous);
            throw failure;
        }
    }

    /** Replaces the sidecar cache with one SQL-authoritative bookmark snapshot. */
    synchronized void reconcileSqlBookmarks(String character, List<Bookmark> bookmarks)
            throws IOException {
        List<Bookmark> source = bookmarks == null ? List.of() : bookmarks;
        if (source.size() > 50) {
            throw new IllegalArgumentException("native bookmark snapshot exceeds 50 rows");
        }
        Map<String, Bookmark> replacement = new LinkedHashMap<>();
        for (Bookmark bookmark : source) {
            if (bookmark == null
                    || replacement.putIfAbsent(key(bookmark.name()), bookmark) != null) {
                throw new IllegalArgumentException("duplicate or null SQL bookmark");
            }
        }
        MutableProfile profile = profile(character);
        if (profile.bookmarks.equals(replacement)) return;
        Map<String, Bookmark> previous = new LinkedHashMap<>(profile.bookmarks);
        profile.bookmarks.clear();
        profile.bookmarks.putAll(replacement);
        try {
            save();
        } catch (IOException failure) {
            profile.bookmarks.clear();
            profile.bookmarks.putAll(previous);
            throw failure;
        }
    }

    synchronized List<BoardTopic> boardTopicsBefore(long cursor, int limit) {
        return boardTopicsBeforeInSource("", cursor, limit);
    }

    synchronized List<BoardTopic> boardTopicsBefore(
            String sourceId, long cursor, int limit) throws IOException {
        String source = normalizedSourceId(sourceId);
        claimBoardSource(source);
        return boardTopicsBeforeInSource(source, cursor, limit);
    }

    private List<BoardTopic> boardTopicsBeforeInSource(
            String sourceId, long cursor, int limit) {
        long threshold = cursor == 0 || cursor == Protocol.BOARD_INITIAL_CURSOR
                ? Protocol.BOARD_INITIAL_CURSOR : cursor;
        String sourceKey = key(normalizedSourceId(sourceId));
        return boardTopics.values().stream()
                .filter(topic -> key(topic.sourceId()).equals(sourceKey))
                .filter(topic -> topic.id() < threshold)
                .sorted(Comparator.comparingLong(BoardTopic::id).reversed())
                .limit(Math.max(0, Math.min(limit, Protocol.BOARD_PAGE_SIZE)))
                .toList();
    }

    synchronized BoardTopic boardTopic(long topicId) {
        return boardTopics.get(sourceScopedId("", topicId));
    }

    synchronized BoardTopic boardTopic(String sourceId, long topicId) throws IOException {
        String source = normalizedSourceId(sourceId);
        claimBoardSource(source);
        return boardTopics.get(sourceScopedId(source, topicId));
    }

    /** Deletes only a topic owned by the requesting character. */
    synchronized boolean deleteBoardTopic(String character, long topicId) throws IOException {
        ProfileIdentity requester = profileIdentity(character);
        claimBoardSource(requester.source());
        String storageKey = sourceScopedId(requester.source(), topicId);
        BoardTopic topic = boardTopics.get(storageKey);
        if (topic == null || character == null
                || !boardTopicOwnedBy(topic, character)) {
            return false;
        }
        boardTopics.remove(storageKey);
        try {
            save();
        } catch (IOException failure) {
            boardTopics.put(storageKey, topic);
            throw failure;
        }
        return true;
    }

    /** Atomically charges the durable inventory and appends one source-local board topic. */
    synchronized BoardTopic postBoardTopic(String character, long points,
                                           Map<Long, Integer> inventory,
                                           String date, String title, String content)
            throws IOException {
        ProfileIdentity authorIdentity = profileIdentity(character);
        String source = authorIdentity.source();
        claimBoardSource(source);
        long nextBoardTopicId = nextBoardTopicIds.getOrDefault(key(source), 1L);
        if (nextBoardTopicId <= 0 || nextBoardTopicId >= Protocol.BOARD_INITIAL_CURSOR) {
            throw new IllegalStateException("native board topic-id range exhausted");
        }
        MutableProfile profile = profile(character);
        long oldPoints = profile.points;
        Map<Long, Integer> oldInventory = new LinkedHashMap<>(profile.inventory);
        long topicId = nextBoardTopicId;
        BoardTopic topic = new BoardTopic(
                source, topicId, profile.name, character, date, title, content);
        // Reuse the exact receiver bounds before allowing state to become durable.
        Protocol.boardRead(topic);
        nextBoardTopicIds.put(key(source), nextBoardTopicId + 1);
        profile.points = Math.max(0, points);
        profile.inventory.clear();
        inventory.forEach((item, count) -> {
            if (item > 0 && count != null && count > 0) profile.inventory.put(item, count);
        });
        String storageKey = sourceScopedId(source, topicId);
        boardTopics.put(storageKey, topic);
        try {
            save();
        } catch (IOException failure) {
            boardTopics.remove(storageKey);
            nextBoardTopicIds.put(key(source), topicId);
            profile.points = oldPoints;
            profile.inventory.clear();
            profile.inventory.putAll(oldInventory);
            throw failure;
        }
        return topic;
    }

    private static boolean boardTopicOwnedBy(BoardTopic topic, String characterIdentity) {
        if (topic.authorIdentity().equalsIgnoreCase(characterIdentity)) return true;
        ProfileIdentity stored = profileIdentity(topic.authorIdentity());
        ProfileIdentity requester = profileIdentity(characterIdentity);
        return stored.source().isEmpty()
                && stored.name().equalsIgnoreCase(requester.name());
    }

    synchronized List<HouseAuction> houseAuctions() {
        return houseAuctionsInSource("");
    }

    synchronized List<HouseAuction> houseAuctions(String sourceId) throws IOException {
        String source = normalizedSourceId(sourceId);
        claimHouseAuctionSource(source);
        return houseAuctionsInSource(source);
    }

    private List<HouseAuction> houseAuctionsInSource(String sourceId) {
        String sourceKey = key(normalizedSourceId(sourceId));
        return houseAuctions.values().stream()
                .filter(auction -> key(auction.sourceId).equals(sourceKey))
                .sorted(Comparator.comparingLong(auction -> auction.houseId))
                .map(MutableHouseAuction::snapshot)
                .toList();
    }

    synchronized HouseAuction houseAuction(long houseId) {
        MutableHouseAuction auction = houseAuctions.get(sourceScopedId("", houseId));
        return auction == null ? null : auction.snapshot();
    }

    synchronized HouseAuction houseAuction(String sourceId, long houseId) throws IOException {
        String source = normalizedSourceId(sourceId);
        claimHouseAuctionSource(source);
        MutableHouseAuction auction = houseAuctions.get(sourceScopedId(source, houseId));
        return auction == null ? null : auction.snapshot();
    }

    /**
     * Commits one native agapply callback atomically.  The expected revision
     * is the capability issued with S_HYPERTEXT_INPUT; the exact inventory
     * transition must remove only the offered Adena.  An earlier bidder is
     * refunded in the same sidecar write, including while offline.
     */
    synchronized HouseBidMutation bidHouse(String bidderName, long bidderObjectId,
                                           long houseId, long expectedRevision,
                                           long amount,
                                           Map<Long, Integer> expectedInventory,
                                           Map<Long, Integer> nextInventory)
            throws IOException {
        return bidHouse("", bidderName, bidderObjectId, houseId, expectedRevision,
                amount, expectedInventory, nextInventory);
    }

    synchronized HouseBidMutation bidHouse(
            String sourceId, String bidderName, long bidderObjectId,
            long houseId, long expectedRevision, long amount,
            Map<Long, Integer> expectedInventory,
            Map<Long, Integer> nextInventory) throws IOException {
        String source = normalizedSourceId(sourceId);
        claimHouseAuctionSource(source);
        MutableHouseAuction auction = houseAuctions.get(sourceScopedId(source, houseId));
        if (auction == null) return houseBid(HouseBidStatus.NOT_FOUND, null);
        if (auction.revision != expectedRevision) {
            return houseBid(HouseBidStatus.STALE, auction);
        }
        if (System.currentTimeMillis() / 1_000L > auction.deadlineEpochSeconds) {
            return houseBid(HouseBidStatus.EXPIRED, auction);
        }
        if (bidderName == null || bidderName.isBlank() || bidderObjectId <= 0
                || bidderObjectId > 0xFFFF_FFFFL || expectedInventory == null
                || nextInventory == null) {
            return houseBid(HouseBidStatus.INVENTORY_CHANGED, auction);
        }
        String bidderStorageKey = profileIdentity(bidderName).storageKey();
        if (auction.bidderStorageKey.equals(bidderStorageKey)) {
            return houseBid(HouseBidStatus.ALREADY_BIDDER, auction);
        }
        long minimum = auction.bidder.isBlank() ? auction.price : auction.price + 1;
        if (minimum <= 0 || minimum > 2_000_000_000L
                || amount < minimum || amount > 2_000_000_000L) {
            return houseBid(HouseBidStatus.INVALID_AMOUNT, auction);
        }

        MutableProfile bidder = profile(bidderName);
        if (!bidder.inventory.equals(expectedInventory)) {
            return houseBid(HouseBidStatus.INVENTORY_CHANGED, auction);
        }
        Map<Long, Integer> calculatedNext = new LinkedHashMap<>(bidder.inventory);
        int adena = calculatedNext.getOrDefault(InventoryCatalog.ADENA, 0);
        if (adena < amount) return houseBid(HouseBidStatus.INVALID_AMOUNT, auction);
        int remaining = (int) (adena - amount);
        if (remaining == 0) calculatedNext.remove(InventoryCatalog.ADENA);
        else calculatedNext.put(InventoryCatalog.ADENA, remaining);
        if (!calculatedNext.equals(nextInventory)) {
            return houseBid(HouseBidStatus.INVENTORY_CHANGED, auction);
        }

        String previousBidder = auction.bidder;
        String previousBidderIdentity = auction.bidderStorageKey;
        MutableProfile previous = previousBidder.isBlank()
                ? null : profiles.get(auction.bidderStorageKey);
        Map<Long, Integer> oldBidderInventory = new LinkedHashMap<>(bidder.inventory);
        Map<Long, Integer> oldPreviousInventory = previous == null
                ? Map.of() : new LinkedHashMap<>(previous.inventory);
        if (!previousBidder.isBlank() && previous == null) {
            return houseBid(HouseBidStatus.REFUND_OVERFLOW, auction);
        }
        Map<Long, Integer> refundedInventory = Map.of();
        if (previous != null) {
            long refundedAdena = (long) previous.inventory.getOrDefault(
                    InventoryCatalog.ADENA, 0) + auction.price;
            if (refundedAdena > Integer.MAX_VALUE) {
                return houseBid(HouseBidStatus.REFUND_OVERFLOW, auction);
            }
            previous.inventory.put(InventoryCatalog.ADENA, (int) refundedAdena);
            refundedInventory = new LinkedHashMap<>(previous.inventory);
        }

        HouseAuction oldAuction = auction.snapshot();
        String oldBidderStorageKey = auction.bidderStorageKey;
        replacePositiveCounts(bidder.inventory, nextInventory);
        auction.price = amount;
        auction.bidder = bidder.name;
        auction.bidderStorageKey = bidderStorageKey;
        auction.bidderObjectId = bidderObjectId;
        auction.revision++;
        try {
            save();
        } catch (IOException failure) {
            replacePositiveCounts(bidder.inventory, oldBidderInventory);
            if (previous != null) replacePositiveCounts(previous.inventory, oldPreviousInventory);
            auction.restore(oldAuction);
            auction.bidderStorageKey = oldBidderStorageKey;
            throw failure;
        }
        return new HouseBidMutation(HouseBidStatus.ACCEPTED, auction.snapshot(),
                previousBidder, previousBidderIdentity,
                oldAuction.price(), refundedInventory);
    }

    private static HouseBidMutation houseBid(HouseBidStatus status,
                                             MutableHouseAuction auction) {
        return new HouseBidMutation(status,
                auction == null ? null : auction.snapshot(), "", "", 0, Map.of());
    }

    private void claimBoardSource(String sourceId) throws IOException {
        String source = normalizedSourceId(sourceId);
        if (source.isEmpty()) return;
        String sourceKey = key(source);
        boolean alreadyPartitioned = boardTopics.values().stream()
                .anyMatch(topic -> key(topic.sourceId()).equals(sourceKey));
        if (alreadyPartitioned) return;
        List<Map.Entry<String, BoardTopic>> legacy = boardTopics.entrySet().stream()
                .filter(entry -> entry.getValue().sourceId().isEmpty()).toList();
        if (legacy.isEmpty()) return;
        for (Map.Entry<String, BoardTopic> entry : legacy) {
            BoardTopic claimed = entry.getValue().withSource(source);
            boardTopics.remove(entry.getKey());
            boardTopics.put(sourceScopedId(source, claimed.id()), claimed);
        }
        long next = legacy.stream().mapToLong(entry -> entry.getValue().id())
                .max().orElse(0) + 1;
        nextBoardTopicIds.remove("");
        nextBoardTopicIds.put(sourceKey, Math.max(1, next));
        save();
    }

    private void claimHouseAuctionSource(String sourceId) throws IOException {
        String source = normalizedSourceId(sourceId);
        if (source.isEmpty()) return;
        String sourceKey = key(source);
        boolean alreadyPartitioned = houseAuctions.values().stream()
                .anyMatch(auction -> key(auction.sourceId).equals(sourceKey));
        if (alreadyPartitioned) return;
        List<Map.Entry<String, MutableHouseAuction>> legacy = houseAuctions.entrySet().stream()
                .filter(entry -> entry.getValue().sourceId.isEmpty()).toList();
        if (!legacy.isEmpty()) {
            for (Map.Entry<String, MutableHouseAuction> entry : legacy) {
                MutableHouseAuction claimed = entry.getValue().withSource(source);
                houseAuctions.remove(entry.getKey());
                houseAuctions.put(sourceScopedId(source, claimed.houseId), claimed);
            }
        } else {
            ensureHouseAuctionSeed(source);
        }
        save();
    }

    synchronized String description() {
        String storage = persistent ? file.toString() : "memory(non-persistent)";
        return ephemeralSourceProfiles
                ? storage + " source-profiles=ephemeral" : storage;
    }

    record SourceProfileRelease(boolean released, int pets, int provenanceKeys) {}
    record SourceOwnerRelease(boolean released, int warehouses, int pets) {}

    /**
     * Drops the complete per-character working set after the last live session
     * has released it. This is intentionally enabled only for the formal
     * read-only shared-database topology; ordinary writable and test stores
     * retain their historical durable behavior.
     */
    synchronized SourceProfileRelease releaseSourceCharacter(String character)
            throws IOException {
        if (!ephemeralSourceProfiles) {
            return new SourceProfileRelease(false, 0, 0);
        }
        ProfileIdentity identity = profileIdentity(character);
        if (identity.source().isEmpty()) {
            return new SourceProfileRelease(false, 0, 0);
        }
        MutableProfile removed = profiles.remove(identity.storageKey());
        timedBuffPersistenceHooks.remove(identity.storageKey());
        if (removed == null) {
            return new SourceProfileRelease(false, 0, 0);
        }

        Set<Long> ownedObjectIds = profileInventoryObjectIds(removed);
        Set<Long> sourceObjectIds = new LinkedHashSet<>(removed.sourceInventory.keySet());
        Set<String> removedPetSourceKeys = pets.values().stream()
                .filter(pet -> ownedObjectIds.contains(pet.collarObjectId()))
                .map(PetState::sourceKey)
                .filter(sourceKey -> !sourceKey.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        int petCount = 0;
        for (Iterator<Map.Entry<Long, PetState>> iterator =
                pets.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Long, PetState> entry = iterator.next();
            if (!ownedObjectIds.contains(entry.getKey())) continue;
            iterator.remove();
            petCount++;
        }
        String sourcePrefix = key(identity.source()) + SOURCE_CHARACTER_SEPARATOR;
        int provenanceBefore = importedSourcePetKeys.size();
        importedSourcePetKeys.removeIf(sourceKey ->
                removedPetSourceKeys.contains(sourceKey)
                        || sourceObjectIds.stream().anyMatch(sourceObjectId ->
                            sourceKey.equals(sourcePrefix + sourceObjectId)));
        int provenanceCount = provenanceBefore - importedSourcePetKeys.size();
        save();
        return new SourceProfileRelease(true, petCount, provenanceCount);
    }

    synchronized SourceOwnerRelease releaseSourceAccount(String account)
            throws IOException {
        if (!ephemeralSourceProfiles || !sourceQualifiedKey(account)) {
            return new SourceOwnerRelease(false, 0, 0);
        }
        String owner = key(account.trim());
        Set<Long> objectIds = new LinkedHashSet<>();
        int warehouseCount = removeWarehouseOwner(
                accountWarehouses, accountWarehouseItemStates, owner, objectIds);
        warehouseCount += removeWarehouseOwner(
                elfWarehouses, elfWarehouseItemStates, owner, objectIds);
        boolean password = warehousePasswords.remove(owner) != null;
        boolean characters = createdCharacters.remove(owner) != null;
        int petCount = removePetsByCollarIds(objectIds);
        boolean released = warehouseCount > 0 || password || characters || petCount > 0;
        if (released) save();
        return new SourceOwnerRelease(released, warehouseCount, petCount);
    }

    synchronized SourceOwnerRelease releaseSourceClan(String clan)
            throws IOException {
        if (!ephemeralSourceProfiles || !sourceQualifiedKey(clan)) {
            return new SourceOwnerRelease(false, 0, 0);
        }
        String owner = key(clan.trim());
        Set<Long> objectIds = new LinkedHashSet<>();
        int warehouseCount = removeWarehouseOwner(
                clanWarehouses, clanWarehouseItemStates, owner, objectIds);
        int petCount = removePetsByCollarIds(objectIds);
        boolean released = warehouseCount > 0 || petCount > 0;
        if (released) save();
        return new SourceOwnerRelease(released, warehouseCount, petCount);
    }

    private static int removeWarehouseOwner(
            Map<String, Map<Long, Integer>> warehouses,
            Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>> itemStates,
            String owner, Set<Long> objectIds) {
        Map<Long, Integer> removed = warehouses.remove(owner);
        Map<Long, List<RuntimeInventory.PersistentItem>> removedStates =
                itemStates.remove(owner);
        if (removedStates != null) {
            for (List<RuntimeInventory.PersistentItem> rows : removedStates.values()) {
                for (RuntimeInventory.PersistentItem item : rows) {
                    if (item.objectId() > 0) objectIds.add(item.objectId());
                }
            }
        }
        return removed != null || removedStates != null ? 1 : 0;
    }

    private int removePetsByCollarIds(Set<Long> collarIds) {
        if (collarIds.isEmpty()) return 0;
        Set<String> sourceKeys = pets.values().stream()
                .filter(pet -> collarIds.contains(pet.collarObjectId()))
                .map(PetState::sourceKey)
                .filter(sourceKey -> !sourceKey.isBlank())
                .collect(java.util.stream.Collectors.toSet());
        int before = pets.size();
        pets.entrySet().removeIf(entry -> collarIds.contains(entry.getKey()));
        importedSourcePetKeys.removeAll(sourceKeys);
        return before - pets.size();
    }

    synchronized boolean isAccountBanned(String account) {
        return account != null && bannedAccounts.contains(key(account));
    }

    synchronized boolean banAccount(String account) throws IOException {
        if (account == null || account.isBlank()) return false;
        boolean added = bannedAccounts.add(key(account));
        if (added) save();
        return added;
    }

    /**
     * Internal source-qualified identity.  The control separator cannot occur
     * in a validated native character/source name and is never written to a
     * packet or displayed to another client.
     */
    static String sourceCharacter(String classicServerName, String characterName) {
        String source = classicServerName == null ? "" : classicServerName.trim();
        String character = characterName == null ? "" : characterName.trim();
        if (source.isEmpty() || character.isEmpty()
                || source.indexOf(SOURCE_CHARACTER_SEPARATOR) >= 0
                || character.indexOf(SOURCE_CHARACTER_SEPARATOR) >= 0) {
            throw new IllegalArgumentException("invalid source-qualified character identity");
        }
        return source + SOURCE_CHARACTER_SEPARATOR + character;
    }

    private MutableProfile profile(String character) {
        ProfileIdentity identity = profileIdentity(character);
        MutableProfile present = profiles.get(identity.storageKey());
        if (present != null) return present;
        if (!identity.source().isEmpty()) {
            String legacyKey = key(identity.name());
            MutableProfile legacy = profiles.get(legacyKey);
            if (legacy != null && (legacy.identitySource.isEmpty()
                    || legacy.identitySource.equalsIgnoreCase(identity.source()))) {
                migrateLegacyProfile(legacyKey, identity.storageKey(),
                        identity.source(), legacy);
                return legacy;
            }
        }
        MutableProfile created = new MutableProfile(identity.name(), INITIAL_POINTS,
                0, 0, 0, 1, INITIAL_INVENTORY);
        created.identitySource = identity.source();
        profiles.put(identity.storageKey(), created);
        return created;
    }

    private void migrateLegacyProfile(String legacyKey, String replacementKey,
                                      String source, MutableProfile legacy) {
        profiles.remove(legacyKey);
        legacy.identitySource = source;
        profiles.put(replacementKey, legacy);
        for (MutableProfile candidate : profiles.values()) {
            if (candidate.partnerStorageKey.equals(legacyKey)) {
                candidate.partnerStorageKey = replacementKey;
            }
        }
        for (MutableHouseAuction auction : houseAuctions.values()) {
            if (auction.bidderStorageKey.equals(legacyKey)) {
                auction.bidderStorageKey = replacementKey;
            }
        }
        String rawPartnerKey = legacy.partnerStorageKey;
        MutableProfile partner = rawPartnerKey.isEmpty() ? null : profiles.get(rawPartnerKey);
        if (partner == null || !partner.identitySource.isEmpty()
                || !partner.partnerStorageKey.equals(replacementKey)) return;
        String partnerReplacementKey = profileStorageKey(source, partner.name);
        profiles.remove(rawPartnerKey);
        partner.identitySource = source;
        partner.partnerStorageKey = replacementKey;
        profiles.put(partnerReplacementKey, partner);
        legacy.partnerStorageKey = partnerReplacementKey;
        for (MutableHouseAuction auction : houseAuctions.values()) {
            if (auction.bidderStorageKey.equals(rawPartnerKey)) {
                auction.bidderStorageKey = partnerReplacementKey;
            }
        }
    }

    private static ProfileIdentity profileIdentity(String value) {
        String normalized = value == null || value.isBlank() ? "Unknown" : value.trim();
        int separator = normalized.indexOf(SOURCE_CHARACTER_SEPARATOR);
        if (separator <= 0 || separator == normalized.length() - 1) {
            return new ProfileIdentity("", normalized, key(normalized));
        }
        String source = normalized.substring(0, separator).trim();
        String name = normalized.substring(separator + 1).trim();
        if (source.isEmpty() || name.isEmpty()
                || name.indexOf(SOURCE_CHARACTER_SEPARATOR) >= 0) {
            throw new IllegalArgumentException("invalid source-qualified character identity");
        }
        return new ProfileIdentity(source, name,
                key(source) + SOURCE_CHARACTER_SEPARATOR + key(name));
    }

    private static String profileStorageKey(String source, String name) {
        if (source == null || source.isBlank()) return key(name);
        return key(source.trim()) + SOURCE_CHARACTER_SEPARATOR + key(name);
    }

    private static String normalizedCastleSource(String sourceId) {
        return normalizedSourceId(sourceId);
    }

    private static String normalizedSourceId(String sourceId) {
        String source = sourceId == null ? "" : sourceId.trim();
        if (source.indexOf(SOURCE_CHARACTER_SEPARATOR) >= 0) {
            throw new IllegalArgumentException("invalid source-qualified castle identity");
        }
        return source;
    }

    private static String castleStorageKey(String sourceId, int castleId) {
        if (castleId <= 0) throw new IllegalArgumentException("invalid castle id");
        return key(normalizedCastleSource(sourceId))
                + SOURCE_CHARACTER_SEPARATOR + castleId;
    }

    private static String castleSecuritySettlementKey(
            String sourceId, int castleId, long warStartMillis) {
        if (warStartMillis <= 0) {
            throw new IllegalArgumentException("invalid castle security war start");
        }
        return castleStorageKey(sourceId, castleId)
                + SOURCE_CHARACTER_SEPARATOR + warStartMillis;
    }

    private static String sourceScopedId(String sourceId, long id) {
        if (id <= 0) throw new IllegalArgumentException("invalid source-scoped id");
        return key(normalizedSourceId(sourceId)) + SOURCE_CHARACTER_SEPARATOR + id;
    }

    private void ensureHouseAuctionSeed(String sourceId) {
        String source = normalizedSourceId(sourceId);
        String storageKey = sourceScopedId(source, INITIAL_HOUSE_ID);
        houseAuctions.computeIfAbsent(storageKey, ignored -> new MutableHouseAuction(
                source, INITIAL_HOUSE_ID, "$1242 1$1195", 78,
                System.currentTimeMillis() / 1_000L + INITIAL_HOUSE_AUCTION_SECONDS,
                INITIAL_HOUSE_PRICE, "$1242  1", "", "", 0, 1));
    }

    private static String sourceNamespace(String sourceQualifiedValue) {
        ProfileIdentity identity = profileIdentity(sourceQualifiedValue);
        return key(identity.source());
    }

    private String uniqueProfileStorageKey(String displayName) {
        String found = "";
        for (Map.Entry<String, MutableProfile> entry : profiles.entrySet()) {
            if (!entry.getValue().name.equalsIgnoreCase(displayName)) continue;
            if (!found.isEmpty()) return "";
            found = entry.getKey();
        }
        return found;
    }

    private void load() throws IOException {
        if (!Files.isRegularFile(file)) return;
        Properties source = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            source.load(reader);
        }
        Set<Long> staleSourceProfileObjectIds = ephemeralSourceProfiles
                ? sourceProfileObjectIds(source) : Set.of();
        if (ephemeralSourceProfiles) {
            staleSourceProfileObjectIds.addAll(sourceWarehouseObjectIds(source));
        }
        int waterDragonPassCount = Math.max(0,
                integer(source, "waterDragon.pass.count", 0));
        Set<String> occupiedWaterDragonRooms = new LinkedHashSet<>();
        long nowEpochSeconds = currentEpochSeconds();
        for (int index = 0; index < waterDragonPassCount; index++) {
            String prefix = "waterDragon.pass." + index + ".";
            try {
                String renterIdentity = source.getProperty(
                        prefix + "renterIdentity", "");
                String passSource = source.getProperty(prefix + "sourceId",
                        profileIdentity(renterIdentity).source());
                WaterDragonPassState pass = new WaterDragonPassState(
                        passSource,
                        number(source, prefix + "itemObjectId", 0),
                        integer(source, prefix + "showId", 0),
                        integer(source, prefix + "roomNumber", -1),
                        renterIdentity,
                        number(source, prefix + "expiresAtEpochSeconds", 0));
                String storageKey = sourceScopedId(
                        pass.sourceId(), pass.itemObjectId());
                String roomKey = key(pass.sourceId())
                        + SOURCE_CHARACTER_SEPARATOR + pass.roomNumber();
                if (pass.remainingSeconds(nowEpochSeconds) <= 0
                        || waterDragonPasses.containsKey(storageKey)
                        || !occupiedWaterDragonRooms.add(roomKey)) continue;
                waterDragonPasses.put(storageKey, pass);
            } catch (IllegalArgumentException damaged) {
                // Preserve every other valid rental if one sidecar row is damaged.
            }
        }
        int hotelLeaseCount = Math.max(0,
                integer(source, "hotel.lease.count", 0));
        Set<String> occupiedHotelSlots = new LinkedHashSet<>();
        for (int index = 0; index < hotelLeaseCount; index++) {
            String prefix = "hotel.lease." + index + ".";
            try {
                HotelLeaseState lease = new HotelLeaseState(
                        integer(source, prefix + "showId", 0),
                        integer(source, prefix + "npcId", 0),
                        integer(source, prefix + "roomNumber", -1),
                        Boolean.parseBoolean(source.getProperty(prefix + "hall", "false")),
                        source.getProperty(prefix + "renterIdentity", ""),
                        number(source, prefix + "expiresAtEpochSeconds", 0),
                        Boolean.parseBoolean(source.getProperty(
                                prefix + "released", "false")));
                String slot = lease.npcId() + ":" + lease.roomNumber();
                if (hotelLeases.containsKey(lease.showId())
                        || lease.active(nowEpochSeconds)
                            && !occupiedHotelSlots.add(slot)) continue;
                hotelLeases.put(lease.showId(), lease);
            } catch (IllegalArgumentException damaged) {
                // Retain other hotel rows if a single lease is malformed.
            }
        }
        int hotelKeyCount = Math.max(0,
                integer(source, "hotel.key.count", 0));
        for (int index = 0; index < hotelKeyCount; index++) {
            String prefix = "hotel.key." + index + ".";
            try {
                HotelKeyState keyState = new HotelKeyState(
                        number(source, prefix + "itemObjectId", 0),
                        integer(source, prefix + "showId", 0),
                        integer(source, prefix + "npcId", 0),
                        Boolean.parseBoolean(source.getProperty(prefix + "hall", "false")),
                        number(source, prefix + "expiresAtEpochSeconds", 0));
                HotelLeaseState lease = hotelLeases.get(keyState.showId());
                if (lease == null || lease.npcId() != keyState.npcId()
                        || lease.meetingHall() != keyState.meetingHall()
                        || lease.expiresAtEpochSeconds()
                            != keyState.expiresAtEpochSeconds()
                        || hotelKeys.putIfAbsent(
                                keyState.itemObjectId(), keyState) != null) continue;
            } catch (IllegalArgumentException damaged) {
                // Retain other hotel keys if a single item metadata row is malformed.
            }
        }
        nextPetObjectId = number(source, "pet.nextObjectId", FIRST_PET_OBJECT_ID);
        Set<String> petTokens = new LinkedHashSet<>();
        for (String property : source.stringPropertyNames()) {
            if (property.startsWith("pet.") && property.endsWith(".collarObjectId")) {
                petTokens.add(property.substring("pet.".length(),
                        property.length() - ".collarObjectId".length()));
            }
        }
        Set<Long> loadedPetObjectIds = new LinkedHashSet<>();
        for (String petToken : petTokens) {
            String prefix = "pet." + petToken + ".";
            try {
                PetState pet = new PetState(
                        source.getProperty(prefix + "sourceKey", ""),
                        number(source, prefix + "collarObjectId", 0),
                        number(source, prefix + "objectId", 0),
                        integer(source, prefix + "collarCatalogId", 0),
                        integer(source, prefix + "npcId", 0),
                        source.getProperty(prefix + "name", ""),
                        integer(source, prefix + "level", 0),
                        integer(source, prefix + "maxHp", 0),
                        integer(source, prefix + "maxMp", 0),
                        number(source, prefix + "experience", 0),
                        integer(source, prefix + "lawful", 0),
                        Boolean.parseBoolean(source.getProperty(prefix + "litoOwned",
                                source.getProperty(prefix + "sourceKey", "").isBlank()
                                        ? "true" : "false")));
                if (ephemeralSourceProfiles
                        && (!pet.sourceKey().isBlank()
                            || staleSourceProfileObjectIds.contains(
                                    pet.collarObjectId()))) {
                    continue;
                }
                if (pets.containsKey(pet.collarObjectId())
                        || !loadedPetObjectIds.add(pet.objectId())) continue;
                pets.put(pet.collarObjectId(), pet);
                if (!pet.sourceKey().isBlank()) importedSourcePetKeys.add(pet.sourceKey());
                nextPetObjectId = Math.max(nextPetObjectId, pet.objectId() + 1);
            } catch (IllegalArgumentException damaged) {
                // Preserve every valid pet when one sidecar row is damaged.
            }
        }
        int importedPetCount = Math.max(0, integer(source, "pet.imported.count", 0));
        for (int index = 0; index < importedPetCount; index++) {
            String sourceKey = source.getProperty("pet.imported." + index, "").trim();
            if (!ephemeralSourceProfiles && !sourceKey.isEmpty()) {
                importedSourcePetKeys.add(sourceKey);
            }
        }
        if (nextPetObjectId < FIRST_PET_OBJECT_ID
                || nextPetObjectId > LAST_PET_OBJECT_ID + 1) {
            nextPetObjectId = FIRST_PET_OBJECT_ID;
        }
        Map<String, String> names = new LinkedHashMap<>();
        Set<Long> loadedLetterIds = new LinkedHashSet<>();
        for (String property : source.stringPropertyNames()) {
            if (property.startsWith("profile.") && property.endsWith(".name")) {
                String token = property.substring("profile.".length(), property.length() - ".name".length());
                names.put(token, source.getProperty(property));
            }
        }
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String prefix = "profile." + entry.getKey() + ".";
            String name = entry.getValue();
            String persistedIdentitySource = source.getProperty(
                    prefix + "identity.source",
                    source.getProperty(prefix + "ranking.server", "")).trim();
            if (ephemeralSourceProfiles && !persistedIdentitySource.isEmpty()) {
                continue;
            }
            MutableProfile profile = new MutableProfile(
                    name,
                    number(source, prefix + "points", INITIAL_POINTS),
                    integer(source, prefix + "wins", 0),
                    integer(source, prefix + "losses", 0),
                    number(source, prefix + "experience", 0),
                    integer(source, prefix + "level", 1),
                    parseInventory(source.getProperty(prefix + "inventory", "2001:1")));
            Map<Long, List<RuntimeInventory.PersistentItem>> durableItemStates =
                    parseItemStates(source.getProperty(prefix + "inventory.items", ""),
                            profile.inventory);
            if (durableItemStates.isEmpty()) {
                durableItemStates = itemStatesFromEnchantments(parseEnchantments(
                        source.getProperty(prefix + "inventory.enchantments", ""),
                        profile.inventory), profile.inventory);
            }
            profile.inventoryItemStates.putAll(durableItemStates);
            profile.rankingPoints = Math.max(0,
                    number(source, prefix + "ranking.points", 0));
            int monthlyPeriod = integer(
                    source, prefix + "ranking.monthly.period", 0);
            if (validRankingPeriod(monthlyPeriod)) {
                profile.monthlyRankingPeriod = monthlyPeriod;
                profile.monthlyRankingPoints = Math.max(0,
                        number(source, prefix + "ranking.monthly.points", 0));
                profile.monthlyWins = Math.max(0,
                        integer(source, prefix + "ranking.monthly.wins", 0));
                profile.monthlyLosses = Math.max(0,
                        integer(source, prefix + "ranking.monthly.losses", 0));
            }
            profile.disconnects = Math.min(profile.losses,
                    Math.max(0, integer(source, prefix + "disconnects", 0)));
            for (int index = 0;
                 index < TOURNAMENT_LEGACY_CERTIFICATE_SLOTS; index++) {
                int code = integer(source,
                        prefix + "tournament.awards.legacy." + index, 0);
                profile.tournamentLegacyCertificateCodes[index] =
                        validTournamentLegacyCertificate(code) ? code : 0;
            }
            for (int index = 0;
                 index < TOURNAMENT_VICTORY_BADGE_MODE_COUNT; index++) {
                int edition = integer(source,
                        prefix + "tournament.awards.victory." + index, 0);
                profile.tournamentVictoryBadgeEditions[index] =
                        edition >= 0 && edition <= 0x7F ? edition : 0;
            }
            profile.tournamentLitoMaster = Boolean.parseBoolean(source.getProperty(
                    prefix + "tournament.awards.litoMaster", "false"));
            profile.classicServerName = source.getProperty(
                    prefix + "ranking.server", "").trim();
            profile.identitySource = persistedIdentitySource;
            boolean lawfulPresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "lawful.present", "false"));
            int observedSourceLawful = integer(
                    source, prefix + "lawful.observedSource", 0);
            int effectiveLawful = integer(source, prefix + "lawful.effective", 0);
            if (lawfulPresent && validLawful(observedSourceLawful)
                    && validLawful(effectiveLawful)) {
                profile.lawfulPresent = true;
                profile.observedSourceLawful = observedSourceLawful;
                profile.effectiveLawful = effectiveLawful;
            }
            profile.generalPkPresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "generalPk.present", "false"));
            profile.observedSourceGeneralPkCount = Math.max(0, integer(
                    source, prefix + "generalPk.observedCount", 0));
            profile.observedSourceLastPkEpochSeconds = Math.max(0, number(
                    source, prefix + "generalPk.observedLastEpochSeconds", 0));
            profile.effectiveGeneralPkCount = Math.max(0, integer(
                    source, prefix + "generalPk.effectiveCount", 0));
            profile.effectiveLastPkEpochSeconds = Math.max(0, number(
                    source, prefix + "generalPk.effectiveLastEpochSeconds", 0));
            profile.classicKillDeathPresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "classicKillDeath.present", "false"));
            profile.observedSourceClassicKills = Math.max(0, integer(
                    source, prefix + "classicKillDeath.observedKills", 0));
            profile.observedSourceClassicDeaths = Math.max(0, integer(
                    source, prefix + "classicKillDeath.observedDeaths", 0));
            profile.effectiveClassicKills = Math.max(0, integer(
                    source, prefix + "classicKillDeath.effectiveKills", 0));
            profile.effectiveClassicDeaths = Math.max(0, integer(
                    source, prefix + "classicKillDeath.effectiveDeaths", 0));
            profile.hellPresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "hell.present", "false"));
            profile.observedSourceHellTicks = Math.max(0, integer(
                    source, prefix + "hell.observedTicks", 0));
            profile.effectiveHellTicks = Math.max(0, integer(
                    source, prefix + "hell.effectiveTicks", 0));
            profile.elfPkPresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "elfPk.present", "false"));
            profile.observedSourceElfPkCount = Math.max(0, integer(
                    source, prefix + "elfPk.observedCount", 0));
            profile.observedSourceLastPkForElfEpochSeconds = Math.max(0, number(
                    source, prefix + "elfPk.observedLastEpochSeconds", 0));
            profile.effectiveElfPkCount = Math.max(0, integer(
                    source, prefix + "elfPk.effectiveCount", 0));
            profile.effectiveLastPkForElfEpochSeconds = Math.max(0, number(
                    source, prefix + "elfPk.effectiveLastEpochSeconds", 0));
            profile.factionKarma = clampFactionKarma(integer(
                    source, prefix + "desireCave.factionKarma", 0));
            boolean classicExperiencePresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "classicExperience.present", "false"));
            long observedSourceExperience = number(
                    source, prefix + "classicExperience.observedSourceExperience", 0);
            int observedSourceLevel = integer(
                    source, prefix + "classicExperience.observedSourceLevel", 1);
            long effectiveClassicExperience = number(
                    source, prefix + "classicExperience.effectiveExperience", 0);
            int effectiveClassicLevel = integer(
                    source, prefix + "classicExperience.effectiveLevel", 1);
            if (classicExperiencePresent
                    && validClassicExperience(observedSourceExperience)
                    && validClassicLevel(observedSourceLevel)
                    && validClassicExperience(effectiveClassicExperience)
                    && validClassicLevel(effectiveClassicLevel)) {
                profile.classicExperiencePresent = true;
                profile.observedSourceExperience = observedSourceExperience;
                profile.observedSourceLevel = observedSourceLevel;
                profile.effectiveClassicExperience = effectiveClassicExperience;
                profile.effectiveClassicLevel = effectiveClassicLevel;
            }
            profile.storyQuestsPresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "storyQuests.present", "false"));
            int sourceQuestCount = Math.max(0,
                    integer(source, prefix + "storyQuests.source.count", 0));
            for (int index = 0; index < sourceQuestCount; index++) {
                String questPrefix = prefix + "storyQuests.source." + index + ".";
                int questId = integer(source, questPrefix + "id", 0);
                int step = integer(source, questPrefix + "step", -1);
                if (questId > 0 && step >= 0) {
                    profile.observedSourceQuestSteps.putIfAbsent(questId, step);
                }
            }
            int effectiveQuestCount = Math.max(0,
                    integer(source, prefix + "storyQuests.effective.count", 0));
            for (int index = 0; index < effectiveQuestCount; index++) {
                String questPrefix = prefix + "storyQuests.effective." + index + ".";
                int questId = integer(source, questPrefix + "id", 0);
                int step = integer(source, questPrefix + "step", -1);
                if (questId > 0 && step >= 0) {
                    profile.effectiveQuestSteps.putIfAbsent(questId, step);
                }
            }
            int timedExperienceBonus = integer(
                    source, prefix + "timedExperience.bonus", 0);
            int timedExperienceRemainingSeconds = integer(
                    source, prefix + "timedExperience.remainingSeconds", 0);
            if (timedExperienceBonus > 0 && timedExperienceRemainingSeconds > 0
                    && timedExperienceRemainingSeconds
                        <= BattleExperienceGemCatalog.DURATION_SECONDS
                    && BattleExperienceGemCatalog.findByBonus(timedExperienceBonus) != null) {
                profile.timedExperienceBonus = timedExperienceBonus;
                profile.timedExperienceRemainingSeconds =
                        timedExperienceRemainingSeconds;
            }
            int fireWeaponRemainingSeconds = integer(
                    source, prefix + "fireWeapon.remainingSeconds", 0);
            if (fireWeaponRemainingSeconds > 0
                    && fireWeaponRemainingSeconds <= 960) {
                profile.fireWeaponRemainingSeconds =
                        fireWeaponRemainingSeconds;
            }
            int burningWeaponRemainingSeconds = integer(
                    source, prefix + "burningWeapon.remainingSeconds", 0);
            if (burningWeaponRemainingSeconds > 0
                    && burningWeaponRemainingSeconds <= 960) {
                profile.burningWeaponRemainingSeconds =
                        burningWeaponRemainingSeconds;
            }
            int windShotRemainingSeconds = integer(
                    source, prefix + "windShot.remainingSeconds", 0);
            if (windShotRemainingSeconds > 0
                    && windShotRemainingSeconds <= 960) {
                profile.windShotRemainingSeconds = windShotRemainingSeconds;
            }
            int fireBlessRemainingSeconds = integer(
                    source, prefix + "fireBless.remainingSeconds", 0);
            if (fireBlessRemainingSeconds > 0
                    && fireBlessRemainingSeconds <= 960) {
                profile.fireBlessRemainingSeconds = fireBlessRemainingSeconds;
            }
            int stormEyeRemainingSeconds = integer(
                    source, prefix + "stormEye.remainingSeconds", 0);
            if (stormEyeRemainingSeconds > 0
                    && stormEyeRemainingSeconds <= 960) {
                profile.stormEyeRemainingSeconds = stormEyeRemainingSeconds;
            }
            int stormShotRemainingSeconds = integer(
                    source, prefix + "stormShot.remainingSeconds", 0);
            if (stormShotRemainingSeconds > 0
                    && stormShotRemainingSeconds <= 960) {
                profile.stormShotRemainingSeconds = stormShotRemainingSeconds;
            }
            int earthBlessRemainingSeconds = integer(
                    source, prefix + "earthBless.remainingSeconds", 0);
            if (earthBlessRemainingSeconds > 0
                    && earthBlessRemainingSeconds <= 960) {
                profile.earthBlessRemainingSeconds = earthBlessRemainingSeconds;
            }
            int ironSkinRemainingSeconds = integer(
                    source, prefix + "ironSkin.remainingSeconds", 0);
            if (ironSkinRemainingSeconds > 0
                    && ironSkinRemainingSeconds <= 960) {
                profile.ironSkinRemainingSeconds = ironSkinRemainingSeconds;
            }
            int resistFearRemainingSeconds = integer(
                    source, prefix + "resistFear.remainingSeconds", 0);
            if (resistFearRemainingSeconds > 0
                    && resistFearRemainingSeconds <= 60) {
                profile.resistFearRemainingSeconds = resistFearRemainingSeconds;
            }
            int movementBuffSkillId = integer(
                    source, prefix + "movementBuff.skillId", 0);
            int movementBuffRemainingSeconds = integer(
                    source, prefix + "movementBuff.remainingSeconds", 0);
            BattleSpellCatalog.MovementBuffSpell movementBuff =
                    BattleSpellCatalog.movementBuffSpell(movementBuffSkillId);
            if (movementBuff != null && movementBuffRemainingSeconds > 0
                    && movementBuffRemainingSeconds
                        <= movementBuff.durationSeconds()) {
                profile.movementBuffSkillId = movementBuffSkillId;
                profile.movementBuffRemainingSeconds =
                        movementBuffRemainingSeconds;
            }
            int armorProtectionSkillId = integer(
                    source, prefix + "armorProtection.skillId", 0);
            int armorProtectionRemainingSeconds = integer(
                    source, prefix + "armorProtection.remainingSeconds", 0);
            BattleSpellCatalog.ShieldSpell armorProtection =
                    BattleSpellCatalog.shieldSpell(armorProtectionSkillId);
            if (armorProtection != null && armorProtectionRemainingSeconds > 0
                    && armorProtectionRemainingSeconds
                        <= armorProtection.durationSeconds()) {
                profile.armorProtectionSkillId = armorProtectionSkillId;
                profile.armorProtectionRemainingSeconds =
                        armorProtectionRemainingSeconds;
            }
            profile.waterDragonBloodExpiresAtEpochSeconds = Math.max(0,
                    number(source,
                            prefix + "waterDragon.blood.expiresAtEpochSeconds", 0));
            profile.channel = integer(source, prefix + "ranking.channel", -1);
            profile.sourceLevel = integer(source, prefix + "ranking.level", -1);
            profile.race = integer(source, prefix + "ranking.race", -1);
            profile.rankingSnapshotPresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "ranking.snapshot.present", "false"));
            profile.snapshotRankingPoints = number(
                    source, prefix + "ranking.snapshot.points", 0);
            profile.snapshotWins = integer(source, prefix + "ranking.snapshot.wins", 0);
            profile.snapshotLosses = integer(source, prefix + "ranking.snapshot.losses", 0);
            profile.snapshotClassicServerName = source.getProperty(
                    prefix + "ranking.snapshot.server", "").trim();
            profile.snapshotChannel = integer(
                    source, prefix + "ranking.snapshot.channel", -1);
            profile.snapshotSourceLevel = integer(
                    source, prefix + "ranking.snapshot.level", -1);
            profile.snapshotRace = integer(source, prefix + "ranking.snapshot.race", -1);
            int monthlySnapshotPeriod = integer(
                    source, prefix + "ranking.monthly.snapshot.period", 0);
            if (Boolean.parseBoolean(source.getProperty(
                        prefix + "ranking.monthly.snapshot.present", "false"))
                    && validRankingPeriod(monthlySnapshotPeriod)) {
                profile.monthlyRankingSnapshotPresent = true;
                profile.snapshotMonthlyRankingPeriod = monthlySnapshotPeriod;
                profile.snapshotMonthlyRankingPoints = Math.max(0,
                        number(source,
                                prefix + "ranking.monthly.snapshot.points", 0));
                profile.snapshotMonthlyWins = Math.max(0,
                        integer(source,
                                prefix + "ranking.monthly.snapshot.wins", 0));
                profile.snapshotMonthlyLosses = Math.max(0,
                        integer(source,
                                prefix + "ranking.monthly.snapshot.losses", 0));
                profile.snapshotMonthlyMovement = integer(source,
                        prefix + "ranking.monthly.snapshot.movement", 0);
            }
            profile.snapshotOverallMovement = integer(
                    source, prefix + "ranking.snapshot.movement.overall", 0);
            profile.snapshotClassicServerMovement = integer(
                    source, prefix + "ranking.snapshot.movement.server", 0);
            profile.snapshotChannelMovement = integer(
                    source, prefix + "ranking.snapshot.movement.channel", 0);
            profile.snapshotLevelMovement = integer(
                    source, prefix + "ranking.snapshot.movement.level", 0);
            profile.snapshotRaceMovement = integer(
                    source, prefix + "ranking.snapshot.movement.race", 0);
            profile.warehouse.putAll(parseWarehouse(
                    source.getProperty(prefix + "warehouse", "")));
            profile.purchasedSkills.addAll(parseSkills(
                    source.getProperty(prefix + "skills", "")));
            profile.deathLostSkills.addAll(parseSkills(
                    source.getProperty(prefix + "death.lostSkills", "")));
            boolean elfAttributePresent = Boolean.parseBoolean(
                    source.getProperty(prefix + "elfAttribute.present", "false"));
            int observedElfAttribute = integer(
                    source, prefix + "elfAttribute.source", 0);
            int effectiveElfAttribute = integer(
                    source, prefix + "elfAttribute.effective", 0);
            if (elfAttributePresent && validElfAttribute(observedElfAttribute)
                    && validElfAttribute(effectiveElfAttribute)) {
                profile.elfAttributePresent = true;
                profile.observedSourceElfAttribute = observedElfAttribute;
                profile.effectiveElfAttribute = effectiveElfAttribute;
                profile.forgottenSkills.addAll(parseSkills(
                        source.getProperty(prefix + "elfAttribute.forgottenSkills", "")));
            }
            profile.partner = source.getProperty(prefix + "partner", "").trim();
            String partnerSource = source.getProperty(
                    prefix + "partner.source", profile.identitySource).trim();
            profile.partnerStorageKey = profile.partner.isBlank() ? ""
                    : profileStorageKey(partnerSource, profile.partner);
            try {
                int mercenaryCastleId = integer(
                        source, prefix + "mercenary.castleId", 0);
                profile.mercenaryAssignment = new MercenaryAssignment(
                        mercenaryCastleId,
                        new MercenaryRoster(
                                integer(source, prefix + "mercenary.0", 0),
                                integer(source, prefix + "mercenary.1", 0),
                                integer(source, prefix + "mercenary.2", 0),
                                integer(source, prefix + "mercenary.3", 0)));
            } catch (IllegalArgumentException damaged) {
                profile.mercenaryAssignment = MercenaryAssignment.empty();
            }
            int sourceProjectileCount = Math.max(0,
                    integer(source, prefix + "sourceProjectile.count", 0));
            for (int index = 0; index < sourceProjectileCount; index++) {
                String projectilePrefix = prefix + "sourceProjectile." + index + ".";
                try {
                    MutableSourceProjectile projectile = new MutableSourceProjectile(
                            number(source, projectilePrefix + "objectId", 0),
                            integer(source, projectilePrefix + "itemId", 0),
                            number(source, projectilePrefix + "observedCount", 0),
                            number(source, projectilePrefix + "remainingCount", -1));
                    profile.sourceProjectiles.put(projectile.objectId, projectile);
                } catch (IllegalArgumentException damaged) {
                    // Preserve every valid debit ledger if one row was edited or damaged.
                }
            }
            int sourceInventoryCount = Math.max(0,
                    integer(source, prefix + "sourceInventory.count", 0));
            for (int index = 0; index < sourceInventoryCount; index++) {
                String inventoryPrefix = prefix + "sourceInventory." + index + ".";
                try {
                    MutableSourceInventory item = new MutableSourceInventory(
                            number(source, inventoryPrefix + "sourceObjectId", 0),
                            number(source, inventoryPrefix + "localObjectId", 0),
                            integer(source, inventoryPrefix + "catalogId", 0),
                            source.getProperty(
                                    inventoryPrefix + "observedFingerprint", ""),
                            integer(source, inventoryPrefix + "consumed", 0) == 1,
                            integer(source, inventoryPrefix + "litoOwned", 0) == 1);
                    profile.sourceInventory.put(item.sourceObjectId, item);
                } catch (IllegalArgumentException damaged) {
                    // Preserve every other valid source-object mapping.
                }
            }
            try {
                int sourceWeaponItemId = integer(
                        source, prefix + "sourceWeapon.itemId", 0);
                if (sourceWeaponItemId > 0) {
                    profile.sourceWeaponDurability =
                            new MutableSourceWeaponDurability(
                                    sourceWeaponItemId,
                                    integer(source,
                                            prefix + "sourceWeapon.observedDurability", -1),
                                    integer(source,
                                            prefix + "sourceWeapon.effectiveDurability", -1));
                }
            } catch (IllegalArgumentException damaged) {
                profile.sourceWeaponDurability = null;
            }
            int bookmarkCount = Math.max(0, integer(source, prefix + "bookmarks.count", 0));
            for (int index = 0; index < bookmarkCount; index++) {
                String bookmarkPrefix = prefix + "bookmark." + index + ".";
                String bookmarkName = source.getProperty(bookmarkPrefix + "name", "").trim();
                int x = integer(source, bookmarkPrefix + "x", -1);
                int y = integer(source, bookmarkPrefix + "y", -1);
                int mapId = integer(source, bookmarkPrefix + "mapId", -1);
                if (!bookmarkName.isEmpty() && x >= 0 && x <= 0xFFFF
                        && y >= 0 && y <= 0xFFFF && mapId >= 0 && mapId <= 0xFFFF) {
                    profile.bookmarks.put(key(bookmarkName),
                            new Bookmark(bookmarkName, x, y, mapId));
                }
            }
            int closedRemaining = profile.inventory.getOrDefault(
                    InventoryCatalog.LETTER_CLOSED, 0);
            int openRemaining = profile.inventory.getOrDefault(
                    InventoryCatalog.LETTER_OPEN, 0);
            int letterCount = Math.max(0, integer(source, prefix + "letters.count", 0));
            for (int index = 0; index < letterCount; index++) {
                String letterPrefix = prefix + "letter." + index + ".";
                long id = number(source, letterPrefix + "id", 0);
                boolean opened = Boolean.parseBoolean(
                        source.getProperty(letterPrefix + "opened", "false"));
                if (id <= 0 || id == Long.MAX_VALUE || loadedLetterIds.contains(id)
                        || (opened ? openRemaining : closedRemaining) <= 0) {
                    continue;
                }
                try {
                    Letter letter = new Letter(
                            id,
                            integer(source, letterPrefix + "code", -1),
                            source.getProperty(letterPrefix + "sender", ""),
                            source.getProperty(letterPrefix + "receiver", ""),
                            source.getProperty(letterPrefix + "date", ""),
                            integer(source, letterPrefix + "templateId", -1),
                            source.getProperty(letterPrefix + "subject", ""),
                            source.getProperty(letterPrefix + "content", ""),
                            opened);
                    profile.letters.add(letter);
                    loadedLetterIds.add(id);
                    if (opened) openRemaining--; else closedRemaining--;
                    nextLetterId = Math.max(nextLetterId, id + 1);
                } catch (IllegalArgumentException damaged) {
                    // Preserve all valid physical letters when one row is damaged.
                }
            }
            profiles.put(profileStorageKey(profile.identitySource, name), profile);
        }
        rankingSnapshotBoundaryEpochSeconds = Math.max(0,
                number(source, "ranking.snapshot.boundary.epochSeconds", 0));
        // A pre-monthly sidecar has already crossed this daily boundary.  Use
        // the same value as the migration default so first access cannot create
        // a mid-day monthly snapshot; the next official 05:00 refresh will
        // materialize the new table from a clean current-period accumulator.
        monthlyRankingSnapshotBoundaryEpochSeconds = Math.max(0,
                number(source, "ranking.monthly.snapshot.boundary.epochSeconds",
                        rankingSnapshotBoundaryEpochSeconds));
        // Keep only reciprocal pairs. A damaged or manually edited one-sided
        // relation must not make either character permanently unavailable.
        for (MutableProfile profile : profiles.values()) {
            MutableProfile partner = profiles.get(profile.partnerStorageKey);
            if (profile.partner.isBlank() || partner == null || partner == profile
                    || !partner.partner.equalsIgnoreCase(profile.name)
                    || !partner.partnerStorageKey.equals(
                            profileStorageKey(profile.identitySource, profile.name))) {
                profile.partner = "";
                profile.partnerStorageKey = "";
            }
        }
        int warehouseAccountCount = Math.max(0,
                integer(source, "warehouse.account.count", 0));
        for (int index = 0; index < warehouseAccountCount; index++) {
            String prefix = "warehouse.account." + index + ".";
            String owner = source.getProperty(prefix + "owner", "").trim();
            if (owner.isEmpty()
                    || ephemeralSourceProfiles && sourceQualifiedKey(owner)) continue;
            Map<Long, Integer> inventory = parseWarehouse(
                    source.getProperty(prefix + "inventory", ""));
            String normalizedOwner = key(owner);
            accountWarehouses.put(normalizedOwner, inventory);
            accountWarehouseItemStates.put(normalizedOwner, parseItemStates(
                    source.getProperty(prefix + "items", ""), inventory));
        }
        loadScopedWarehouses(source, "warehouse.clan", clanWarehouses,
                clanWarehouseItemStates);
        loadScopedWarehouses(source, "warehouse.elf", elfWarehouses,
                elfWarehouseItemStates);
        if (ephemeralSourceProfiles) {
            clanWarehouses.keySet().removeIf(GameStateStore::sourceQualifiedKey);
            clanWarehouseItemStates.keySet().removeIf(
                    GameStateStore::sourceQualifiedKey);
            elfWarehouses.keySet().removeIf(GameStateStore::sourceQualifiedKey);
            elfWarehouseItemStates.keySet().removeIf(
                    GameStateStore::sourceQualifiedKey);
        }
        int warehousePasswordCount = Math.max(0,
                integer(source, "warehouse.password.count", 0));
        for (int index = 0; index < warehousePasswordCount; index++) {
            String prefix = "warehouse.password." + index + ".";
            String owner = source.getProperty(prefix + "owner", "").trim();
            int password = integer(source, prefix + "value", 0);
            if (!owner.isEmpty()
                    && (!ephemeralSourceProfiles || !sourceQualifiedKey(owner))
                    && password > 0 && password <= 999_999) {
                warehousePasswords.put(key(owner), password);
            }
        }
        int castleCount = Math.max(0, integer(source, "castle.count", 0));
        for (int index = 0; index < castleCount; index++) {
            String prefix = "castle." + index + ".";
            String castleSource;
            try {
                castleSource = normalizedCastleSource(
                        source.getProperty(prefix + "source", ""));
            } catch (IllegalArgumentException damaged) {
                continue;
            }
            int castleId = integer(source, prefix + "id", 0);
            int taxRate = integer(source, prefix + "taxRate", 10);
            long publicMoney = number(source, prefix + "publicMoney", 0);
            long warTimeMillis = number(source, prefix + "warTimeMillis", 0);
            boolean securityEnabled = Boolean.parseBoolean(
                    source.getProperty(prefix + "securityEnabled", "false"));
            long revision = number(source, prefix + "revision", 1);
            MercenaryRoster mercenaries;
            try {
                mercenaries = new MercenaryRoster(
                        integer(source, prefix + "mercenary.0", 0),
                        integer(source, prefix + "mercenary.1", 0),
                        integer(source, prefix + "mercenary.2", 0),
                        integer(source, prefix + "mercenary.3", 0));
            } catch (IllegalArgumentException damaged) {
                mercenaries = new MercenaryRoster(0, 0, 0, 0);
            }
            if (castleId <= 0 || taxRate < 10 || taxRate > 50
                    || publicMoney < 0 || publicMoney > Integer.MAX_VALUE
                    || warTimeMillis < 0
                    || revision <= 0) continue;
            String storageKey = castleStorageKey(castleSource, castleId);
            if (castles.containsKey(storageKey)) continue;
            castles.put(storageKey,
                    new MutableCastle(castleSource, castleId, taxRate, publicMoney,
                            warTimeMillis, securityEnabled, mercenaries, revision));
        }
        int castleSecuritySettlementCount = Math.max(0,
                integer(source, "castle.security.settlement.count", 0));
        for (int index = 0; index < castleSecuritySettlementCount; index++) {
            String prefix = "castle.security.settlement." + index + ".";
            try {
                CastleSecurityLedger settlement = new CastleSecurityLedger(
                        source.getProperty(prefix + "source", ""),
                        integer(source, prefix + "id", 0),
                        number(source, prefix + "warStartMillis", 0),
                        number(source, prefix + "chargeAdena", 0),
                        Boolean.parseBoolean(source.getProperty(
                                prefix + "executed", "false")),
                        Boolean.parseBoolean(source.getProperty(
                                prefix + "completed", "false")));
                String settlementKey = castleSecuritySettlementKey(
                        settlement.sourceId(), settlement.castleId(),
                        settlement.warStartMillis());
                if (castles.containsKey(castleStorageKey(
                                settlement.sourceId(), settlement.castleId()))) {
                    castleSecuritySettlements.putIfAbsent(
                            settlementKey, settlement);
                }
            } catch (IllegalArgumentException damaged) {
                // Ignore only the damaged ledger row. A READY settlement can
                // then be retried; the castle snapshot remains independently
                // readable.
            }
        }
        int ownershipCount = Math.max(0,
                integer(source, "castle.ownership.count", 0));
        for (int index = 0; index < ownershipCount; index++) {
            String prefix = "castle.ownership." + index + ".";
            try {
                CastleOwnershipState ownership = new CastleOwnershipState(
                        source.getProperty(prefix + "source", ""),
                        integer(source, prefix + "id", 0),
                        source.getProperty(prefix + "clan", ""),
                        source.getProperty(prefix + "leader", ""),
                        number(source, prefix + "leaderObjectId", 0),
                        number(source, prefix + "revision", 0));
                castleOwnerships.putIfAbsent(
                        castleStorageKey(ownership.sourceId(), ownership.castleId()),
                        new MutableCastleOwnership(
                                ownership.sourceId(), ownership.castleId(),
                                ownership.clanName(), ownership.leaderName(),
                                ownership.leaderObjectId(), ownership.revision()));
            } catch (IllegalArgumentException damaged) {
                // Ignore only the damaged ownership row; other durable state
                // remains independently recoverable.
            }
        }
        int bannedCount = Math.max(0, integer(source, "banned.count", 0));
        for (int index = 0; index < bannedCount; index++) {
            String account = source.getProperty("banned." + index + ".account", "").trim();
            if (!account.isEmpty()) bannedAccounts.add(key(account));
        }
        int createdCount = Math.max(0, integer(source, "created.count", 0));
        for (int index = 0; index < createdCount; index++) {
            String prefix = "created." + index + ".";
            String account = source.getProperty(prefix + "account", "").trim();
            String name = source.getProperty(prefix + "name", "").trim();
            long objectId = number(source, prefix + "objectId", 0);
            if (account.isEmpty() || name.isEmpty()
                    || ephemeralSourceProfiles && sourceQualifiedKey(account)
                    || objectId <= 0
                    || objectId > 0xFFFF_FFFFL) continue;
            CharacterSummary character = new CharacterSummary(
                    name,
                    source.getProperty(prefix + "clan", ""),
                    integer(source, prefix + "type", 0),
                    integer(source, prefix + "sex", 0),
                    integer(source, prefix + "level", 1),
                    integer(source, prefix + "maxHp", 50),
                    integer(source, prefix + "maxMp", 10),
                    integer(source, prefix + "armorClass", 10),
                    integer(source, prefix + "strength", 13),
                    integer(source, prefix + "constitution", 10),
                    integer(source, prefix + "dexterity", 10),
                    integer(source, prefix + "charisma", 13),
                    integer(source, prefix + "intelligence", 10),
                    integer(source, prefix + "wisdom", 11),
                    integer(source, prefix + "originalIntelligence",
                            integer(source, prefix + "intelligence", 10)),
                    integer(source, prefix + "originalWisdom",
                            integer(source, prefix + "wisdom", 11)),
                    objectId,
                    number(source, prefix + "experience", 0),
                    number(source, prefix + "lawful", 0),
                    integer(source, prefix + "pkCount", 0),
                    number(source, prefix + "classId", 0),
                    number(source, prefix + "locationX", 32600),
                    number(source, prefix + "locationY", 32700),
                    number(source, prefix + "mapId", 4));
            String namespace = sourceNamespace(account);
            boolean duplicate = createdCharacters.entrySet().stream()
                    .filter(entry -> sourceNamespace(entry.getKey()).equals(namespace))
                    .flatMap(entry -> entry.getValue().stream())
                    .anyMatch(existing -> existing.name().equalsIgnoreCase(name));
            if (duplicate) continue;
            createdCharacters.computeIfAbsent(key(account), ignored -> new ArrayList<>()).add(character);
            if (objectId >= FIRST_CREATED_OBJECT_ID) {
                nextCreatedObjectId = Math.max(nextCreatedObjectId, objectId + 1);
            }
        }
        int boardCount = Math.max(0, integer(source, "board.count", 0));
        for (int index = 0; index < boardCount; index++) {
            String prefix = "board." + index + ".";
            long id = number(source, prefix + "id", 0);
            String authorIdentity = source.getProperty(prefix + "author.identity",
                    source.getProperty(prefix + "author", ""));
            String topicSource;
            try {
                topicSource = normalizedSourceId(source.getProperty(
                        prefix + "source", profileIdentity(authorIdentity).source()));
            } catch (IllegalArgumentException damaged) {
                continue;
            }
            String storageKey = id <= 0 ? "" : sourceScopedId(topicSource, id);
            if (id <= 0 || id >= Protocol.BOARD_INITIAL_CURSOR
                    || boardTopics.containsKey(storageKey)) continue;
            BoardTopic topic = new BoardTopic(
                    topicSource,
                    id,
                    source.getProperty(prefix + "author", ""),
                    authorIdentity,
                    source.getProperty(prefix + "date", ""),
                    source.getProperty(prefix + "title", ""),
                    source.getProperty(prefix + "content", ""));
            try {
                Protocol.boardRead(topic);
            } catch (IllegalArgumentException damaged) {
                continue;
            }
            boardTopics.put(storageKey, topic);
            nextBoardTopicIds.merge(key(topicSource), id + 1, Math::max);
        }
        int houseAuctionCount = Math.max(0,
                integer(source, "house.auction.count", 0));
        for (int index = 0; index < houseAuctionCount; index++) {
            String prefix = "house.auction." + index + ".";
            long houseId = number(source, prefix + "houseId", 0);
            long domain = (houseId >>> 16) & 0xFFFFL;
            String name = source.getProperty(prefix + "name", "").trim();
            int area = integer(source, prefix + "area", -1);
            long deadline = number(source, prefix + "deadlineEpochSeconds", 0);
            long price = number(source, prefix + "price", 0);
            String location = source.getProperty(prefix + "location", "").trim();
            String oldOwner = source.getProperty(prefix + "oldOwner", "").trim();
            long oldOwnerObjectId = number(source, prefix + "oldOwnerObjectId", 0);
            String bidder = source.getProperty(prefix + "bidder", "").trim();
            String bidderSource = source.getProperty(prefix + "bidder.source", "").trim();
            String auctionSource;
            try {
                auctionSource = normalizedSourceId(source.getProperty(
                        prefix + "source", bidderSource));
            } catch (IllegalArgumentException damaged) {
                continue;
            }
            long bidderObjectId = number(source, prefix + "bidderObjectId", 0);
            long revision = number(source, prefix + "revision", 0);
            if (houseId <= 0 || houseId > 0xFFFF_FFFFL || domain < 4 || domain >= 8
                    || name.isEmpty() || area < 0 || area > 0xFFFF || deadline <= 0
                    || price <= 0 || price > 2_000_000_000L || revision <= 0
                    || oldOwnerObjectId < 0 || oldOwnerObjectId > 0xFFFF_FFFFL
                    || houseAuctions.containsKey(sourceScopedId(auctionSource, houseId))) continue;
            String bidderStorageKey = bidder.isEmpty() ? ""
                    : bidderSource.isEmpty() ? uniqueProfileStorageKey(bidder)
                    : profileStorageKey(bidderSource, bidder);
            if (!bidder.isEmpty() && (bidderObjectId <= 0
                    || bidderObjectId > 0xFFFF_FFFFL
                    || bidderStorageKey.isEmpty()
                    || !profiles.containsKey(bidderStorageKey))) {
                bidder = "";
                bidderStorageKey = "";
                bidderObjectId = 0;
            }
            MutableHouseAuction auction = new MutableHouseAuction(
                    auctionSource, houseId, name, area, deadline, price, location, oldOwner,
                    oldOwnerObjectId, bidder, bidderObjectId, revision);
            auction.bidderStorageKey = bidderStorageKey;
            houseAuctions.put(sourceScopedId(auctionSource, houseId), auction);
        }
    }

    private void save() throws IOException {
        if (!persistent) return;
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent != null) Files.createDirectories(parent);
        Properties target = new Properties();
        // Format 27 adds selected-source story quest reconciliation and the
        // LiTo-owned effective quest overlay. Format 26 adds the retained Dragon Gem bonus and paused remaining
        // seconds. Format 25 additionally stores the stable native object id. A pet's
        // collar uses that id as its durable primary key, so trade, warehouse
        // transfer and restart must never silently renumber the object.
        // Format 37 adds the independently accumulated, daily-frozen calendar
        // month ranking state proved by the archived official Monthly table.
        // Format 36 adds read-only source reconciliation plus the LiTo-owned
        // effective PKcount/LastPk overlay used by L1Guard.
        // Format 35 adds read-only source reconciliation plus the LiTo-owned
        // effective PkCountForElf/LastPkForElf overlay used by L1Guardian.
        // Format 34 also accepts the optional LiTo-side CharBuff remaining
        // time for native skill148 Fire Weapon.  The additive property is
        // backward compatible and does not require renumbering the format.
        // Format 34 appends per-object Tournament reward provenance so an
        // opened PET_RACE prize cannot merge with or reset like a bought
        // stack carrying the same catalog id. Format 33 adds native nine-inn leases and per-object shared key
        // metadata. Format 32 stores Dragon Gate pass leases and the Water Dragon Blood
        // completion cooldown as wall-clock deadlines so restart cannot erase
        // the retained InnKey/CharBuff semantics. Format 31's variable-length item row uses its optional trailing
        // field for the native per-object usable-charge byte. Older 5..9-field
        // rows remain readable and migrate from catalog max_charge_count.
        // Format 43 adds the crash-resumable, source-qualified castle-security
        // settlement ledger. Format 42 qualifies Water Dragon pass leases by classic source so
        // identical dynamic-database object IDs cannot collide across routes.
        target.setProperty("format.version", "43");
        target.setProperty("ranking.monthly.snapshot.boundary.epochSeconds",
                Long.toString(monthlyRankingSnapshotBoundaryEpochSeconds));
        List<WaterDragonPassState> durableWaterDragonPasses =
                waterDragonPasses.values().stream()
                        .filter(pass -> pass.remainingSeconds(
                                currentEpochSeconds()) > 0)
                        .sorted(Comparator.comparing(
                                        WaterDragonPassState::sourceId,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparingInt(
                                        WaterDragonPassState::roomNumber))
                        .toList();
        target.setProperty("waterDragon.pass.count",
                Integer.toString(durableWaterDragonPasses.size()));
        for (int index = 0; index < durableWaterDragonPasses.size(); index++) {
            WaterDragonPassState pass = durableWaterDragonPasses.get(index);
            String prefix = "waterDragon.pass." + index + ".";
            target.setProperty(prefix + "sourceId", pass.sourceId());
            target.setProperty(prefix + "itemObjectId",
                    Long.toString(pass.itemObjectId()));
            target.setProperty(prefix + "showId",
                    Integer.toString(pass.showId()));
            target.setProperty(prefix + "roomNumber",
                    Integer.toString(pass.roomNumber()));
            target.setProperty(prefix + "renterIdentity",
                    pass.renterIdentity());
            target.setProperty(prefix + "expiresAtEpochSeconds",
                    Long.toString(pass.expiresAtEpochSeconds()));
        }
        Set<Integer> keyBackedHotelShows = hotelKeys.values().stream()
                .map(HotelKeyState::showId)
                .collect(java.util.stream.Collectors.toSet());
        List<HotelLeaseState> durableHotelLeases = hotelLeases.values().stream()
                .filter(lease -> !lease.released()
                        || keyBackedHotelShows.contains(lease.showId()))
                .sorted(Comparator.comparingInt(HotelLeaseState::npcId)
                        .thenComparingInt(HotelLeaseState::roomNumber)
                        .thenComparingInt(HotelLeaseState::showId))
                .toList();
        target.setProperty("hotel.lease.count",
                Integer.toString(durableHotelLeases.size()));
        for (int index = 0; index < durableHotelLeases.size(); index++) {
            HotelLeaseState lease = durableHotelLeases.get(index);
            String prefix = "hotel.lease." + index + ".";
            target.setProperty(prefix + "showId", Integer.toString(lease.showId()));
            target.setProperty(prefix + "npcId", Integer.toString(lease.npcId()));
            target.setProperty(prefix + "roomNumber",
                    Integer.toString(lease.roomNumber()));
            target.setProperty(prefix + "hall",
                    Boolean.toString(lease.meetingHall()));
            target.setProperty(prefix + "renterIdentity", lease.renterIdentity());
            target.setProperty(prefix + "expiresAtEpochSeconds",
                    Long.toString(lease.expiresAtEpochSeconds()));
            target.setProperty(prefix + "released",
                    Boolean.toString(lease.released()));
        }
        List<HotelKeyState> durableHotelKeys = hotelKeys.values().stream()
                .filter(keyState -> hotelLeases.containsKey(keyState.showId()))
                .sorted(Comparator.comparingLong(HotelKeyState::itemObjectId))
                .toList();
        target.setProperty("hotel.key.count",
                Integer.toString(durableHotelKeys.size()));
        for (int index = 0; index < durableHotelKeys.size(); index++) {
            HotelKeyState keyState = durableHotelKeys.get(index);
            String prefix = "hotel.key." + index + ".";
            target.setProperty(prefix + "itemObjectId",
                    Long.toString(keyState.itemObjectId()));
            target.setProperty(prefix + "showId", Integer.toString(keyState.showId()));
            target.setProperty(prefix + "npcId", Integer.toString(keyState.npcId()));
            target.setProperty(prefix + "hall",
                    Boolean.toString(keyState.meetingHall()));
            target.setProperty(prefix + "expiresAtEpochSeconds",
                    Long.toString(keyState.expiresAtEpochSeconds()));
        }
        target.setProperty("pet.nextObjectId", Long.toString(nextPetObjectId));
        int importedPetIndex = 0;
        for (String sourceKey : importedSourcePetKeys) {
            if (ephemeralSourceProfiles) continue;
            target.setProperty("pet.imported." + importedPetIndex++, sourceKey);
        }
        target.setProperty("pet.imported.count", Integer.toString(importedPetIndex));
        Set<Long> ephemeralProfileObjectIds = ephemeralSourceProfiles
                ? sourceProfileObjectIds() : Set.of();
        for (PetState pet : pets.values()) {
            if (ephemeralSourceProfiles
                    && (!pet.sourceKey().isBlank()
                        || ephemeralProfileObjectIds.contains(
                                pet.collarObjectId()))) {
                continue;
            }
            String prefix = "pet." + pet.collarObjectId() + ".";
            target.setProperty(prefix + "sourceKey", pet.sourceKey());
            target.setProperty(prefix + "collarObjectId",
                    Long.toString(pet.collarObjectId()));
            target.setProperty(prefix + "objectId", Long.toString(pet.objectId()));
            target.setProperty(prefix + "collarCatalogId",
                    Integer.toString(pet.collarCatalogId()));
            target.setProperty(prefix + "npcId", Integer.toString(pet.npcId()));
            target.setProperty(prefix + "name", pet.name());
            target.setProperty(prefix + "level", Integer.toString(pet.level()));
            target.setProperty(prefix + "maxHp", Integer.toString(pet.maxHp()));
            target.setProperty(prefix + "maxMp", Integer.toString(pet.maxMp()));
            target.setProperty(prefix + "experience", Long.toString(pet.experience()));
            target.setProperty(prefix + "lawful", Integer.toString(pet.lawful()));
            target.setProperty(prefix + "litoOwned", Boolean.toString(pet.litoOwned()));
        }
        target.setProperty("ranking.snapshot.boundary.epochSeconds",
                Long.toString(rankingSnapshotBoundaryEpochSeconds));
        for (Map.Entry<String, MutableProfile> profileEntry : profiles.entrySet()) {
            MutableProfile profile = profileEntry.getValue();
            if (ephemeralSourceProfiles && !profile.identitySource.isBlank()) {
                continue;
            }
            String prefix = "profile." + token(profileEntry.getKey()) + ".";
            target.setProperty(prefix + "name", profile.name);
            target.setProperty(prefix + "identity.source", profile.identitySource);
            target.setProperty(prefix + "lawful.present",
                    Boolean.toString(profile.lawfulPresent));
            target.setProperty(prefix + "lawful.observedSource",
                    Integer.toString(profile.observedSourceLawful));
            target.setProperty(prefix + "lawful.effective",
                    Integer.toString(profile.effectiveLawful));
            target.setProperty(prefix + "generalPk.present",
                    Boolean.toString(profile.generalPkPresent));
            target.setProperty(prefix + "generalPk.observedCount",
                    Integer.toString(profile.observedSourceGeneralPkCount));
            target.setProperty(prefix + "generalPk.observedLastEpochSeconds",
                    Long.toString(profile.observedSourceLastPkEpochSeconds));
            target.setProperty(prefix + "generalPk.effectiveCount",
                    Integer.toString(profile.effectiveGeneralPkCount));
            target.setProperty(prefix + "generalPk.effectiveLastEpochSeconds",
                    Long.toString(profile.effectiveLastPkEpochSeconds));
            target.setProperty(prefix + "classicKillDeath.present",
                    Boolean.toString(profile.classicKillDeathPresent));
            target.setProperty(prefix + "classicKillDeath.observedKills",
                    Integer.toString(profile.observedSourceClassicKills));
            target.setProperty(prefix + "classicKillDeath.observedDeaths",
                    Integer.toString(profile.observedSourceClassicDeaths));
            target.setProperty(prefix + "classicKillDeath.effectiveKills",
                    Integer.toString(profile.effectiveClassicKills));
            target.setProperty(prefix + "classicKillDeath.effectiveDeaths",
                    Integer.toString(profile.effectiveClassicDeaths));
            target.setProperty(prefix + "hell.present",
                    Boolean.toString(profile.hellPresent));
            target.setProperty(prefix + "hell.observedTicks",
                    Integer.toString(profile.observedSourceHellTicks));
            target.setProperty(prefix + "hell.effectiveTicks",
                    Integer.toString(profile.effectiveHellTicks));
            target.setProperty(prefix + "elfPk.present",
                    Boolean.toString(profile.elfPkPresent));
            target.setProperty(prefix + "elfPk.observedCount",
                    Integer.toString(profile.observedSourceElfPkCount));
            target.setProperty(prefix + "elfPk.observedLastEpochSeconds",
                    Long.toString(profile.observedSourceLastPkForElfEpochSeconds));
            target.setProperty(prefix + "elfPk.effectiveCount",
                    Integer.toString(profile.effectiveElfPkCount));
            target.setProperty(prefix + "elfPk.effectiveLastEpochSeconds",
                    Long.toString(profile.effectiveLastPkForElfEpochSeconds));
            target.setProperty(prefix + "desireCave.factionKarma",
                    Integer.toString(profile.factionKarma));
            target.setProperty(prefix + "classicExperience.present",
                    Boolean.toString(profile.classicExperiencePresent));
            target.setProperty(prefix + "classicExperience.observedSourceExperience",
                    Long.toString(profile.observedSourceExperience));
            target.setProperty(prefix + "classicExperience.observedSourceLevel",
                    Integer.toString(profile.observedSourceLevel));
            target.setProperty(prefix + "classicExperience.effectiveExperience",
                    Long.toString(profile.effectiveClassicExperience));
            target.setProperty(prefix + "classicExperience.effectiveLevel",
                    Integer.toString(profile.effectiveClassicLevel));
            target.setProperty(prefix + "storyQuests.present",
                    Boolean.toString(profile.storyQuestsPresent));
            saveQuestSteps(target, prefix + "storyQuests.source",
                    profile.observedSourceQuestSteps);
            saveQuestSteps(target, prefix + "storyQuests.effective",
                    profile.effectiveQuestSteps);
            target.setProperty(prefix + "timedExperience.bonus",
                    Integer.toString(profile.timedExperienceBonus));
            target.setProperty(prefix + "timedExperience.remainingSeconds",
                    Integer.toString(profile.timedExperienceRemainingSeconds));
            target.setProperty(prefix + "fireWeapon.remainingSeconds",
                    Integer.toString(profile.fireWeaponRemainingSeconds));
            target.setProperty(prefix + "burningWeapon.remainingSeconds",
                    Integer.toString(profile.burningWeaponRemainingSeconds));
            target.setProperty(prefix + "windShot.remainingSeconds",
                    Integer.toString(profile.windShotRemainingSeconds));
            target.setProperty(prefix + "fireBless.remainingSeconds",
                    Integer.toString(profile.fireBlessRemainingSeconds));
            target.setProperty(prefix + "stormEye.remainingSeconds",
                    Integer.toString(profile.stormEyeRemainingSeconds));
            target.setProperty(prefix + "stormShot.remainingSeconds",
                    Integer.toString(profile.stormShotRemainingSeconds));
            target.setProperty(prefix + "earthBless.remainingSeconds",
                    Integer.toString(profile.earthBlessRemainingSeconds));
            target.setProperty(prefix + "ironSkin.remainingSeconds",
                    Integer.toString(profile.ironSkinRemainingSeconds));
            target.setProperty(prefix + "resistFear.remainingSeconds",
                    Integer.toString(profile.resistFearRemainingSeconds));
            target.setProperty(prefix + "movementBuff.skillId",
                    Integer.toString(profile.movementBuffSkillId));
            target.setProperty(prefix + "movementBuff.remainingSeconds",
                    Integer.toString(profile.movementBuffRemainingSeconds));
            target.setProperty(prefix + "armorProtection.skillId",
                    Integer.toString(profile.armorProtectionSkillId));
            target.setProperty(prefix + "armorProtection.remainingSeconds",
                    Integer.toString(profile.armorProtectionRemainingSeconds));
            target.setProperty(prefix + "waterDragon.blood.expiresAtEpochSeconds",
                    Long.toString(profile.waterDragonBloodExpiresAtEpochSeconds));
            target.setProperty(prefix + "points", Long.toString(profile.points));
            target.setProperty(prefix + "ranking.points",
                    Long.toString(profile.rankingPoints));
            target.setProperty(prefix + "ranking.monthly.period",
                    Integer.toString(profile.monthlyRankingPeriod));
            target.setProperty(prefix + "ranking.monthly.points",
                    Long.toString(profile.monthlyRankingPoints));
            target.setProperty(prefix + "ranking.monthly.wins",
                    Integer.toString(profile.monthlyWins));
            target.setProperty(prefix + "ranking.monthly.losses",
                    Integer.toString(profile.monthlyLosses));
            target.setProperty(prefix + "wins", Integer.toString(profile.wins));
            target.setProperty(prefix + "losses", Integer.toString(profile.losses));
            target.setProperty(prefix + "disconnects", Integer.toString(profile.disconnects));
            for (int index = 0;
                 index < TOURNAMENT_LEGACY_CERTIFICATE_SLOTS; index++) {
                target.setProperty(prefix + "tournament.awards.legacy." + index,
                        Integer.toString(
                                profile.tournamentLegacyCertificateCodes[index]));
            }
            for (int index = 0;
                 index < TOURNAMENT_VICTORY_BADGE_MODE_COUNT; index++) {
                target.setProperty(prefix + "tournament.awards.victory." + index,
                        Integer.toString(
                                profile.tournamentVictoryBadgeEditions[index]));
            }
            target.setProperty(prefix + "tournament.awards.litoMaster",
                    Boolean.toString(profile.tournamentLitoMaster));
            target.setProperty(prefix + "ranking.server",
                    profile.classicServerName);
            target.setProperty(prefix + "ranking.channel", Integer.toString(profile.channel));
            target.setProperty(prefix + "ranking.level", Integer.toString(profile.sourceLevel));
            target.setProperty(prefix + "ranking.race", Integer.toString(profile.race));
            target.setProperty(prefix + "ranking.snapshot.present",
                    Boolean.toString(profile.rankingSnapshotPresent));
            target.setProperty(prefix + "ranking.snapshot.points",
                    Long.toString(profile.snapshotRankingPoints));
            target.setProperty(prefix + "ranking.snapshot.wins",
                    Integer.toString(profile.snapshotWins));
            target.setProperty(prefix + "ranking.snapshot.losses",
                    Integer.toString(profile.snapshotLosses));
            target.setProperty(prefix + "ranking.snapshot.server",
                    profile.snapshotClassicServerName);
            target.setProperty(prefix + "ranking.snapshot.channel",
                    Integer.toString(profile.snapshotChannel));
            target.setProperty(prefix + "ranking.snapshot.level",
                    Integer.toString(profile.snapshotSourceLevel));
            target.setProperty(prefix + "ranking.snapshot.race",
                    Integer.toString(profile.snapshotRace));
            target.setProperty(prefix + "ranking.monthly.snapshot.present",
                    Boolean.toString(profile.monthlyRankingSnapshotPresent));
            target.setProperty(prefix + "ranking.monthly.snapshot.period",
                    Integer.toString(profile.snapshotMonthlyRankingPeriod));
            target.setProperty(prefix + "ranking.monthly.snapshot.points",
                    Long.toString(profile.snapshotMonthlyRankingPoints));
            target.setProperty(prefix + "ranking.monthly.snapshot.wins",
                    Integer.toString(profile.snapshotMonthlyWins));
            target.setProperty(prefix + "ranking.monthly.snapshot.losses",
                    Integer.toString(profile.snapshotMonthlyLosses));
            target.setProperty(prefix + "ranking.monthly.snapshot.movement",
                    Integer.toString(profile.snapshotMonthlyMovement));
            target.setProperty(prefix + "ranking.snapshot.movement.overall",
                    Integer.toString(profile.snapshotOverallMovement));
            target.setProperty(prefix + "ranking.snapshot.movement.server",
                    Integer.toString(profile.snapshotClassicServerMovement));
            target.setProperty(prefix + "ranking.snapshot.movement.channel",
                    Integer.toString(profile.snapshotChannelMovement));
            target.setProperty(prefix + "ranking.snapshot.movement.level",
                    Integer.toString(profile.snapshotLevelMovement));
            target.setProperty(prefix + "ranking.snapshot.movement.race",
                    Integer.toString(profile.snapshotRaceMovement));
            target.setProperty(prefix + "experience", Long.toString(profile.experience));
            target.setProperty(prefix + "level", Integer.toString(profile.level));
            target.setProperty(prefix + "inventory", formatInventory(profile.inventory));
            target.setProperty(prefix + "inventory.items",
                    formatItemStates(profile.inventoryItemStates));
            target.setProperty(prefix + "inventory.enchantments",
                    formatEnchantments(enchantmentsFromItemStates(
                            profile.inventoryItemStates, profile.inventory)));
            target.setProperty(prefix + "warehouse", formatInventory(profile.warehouse));
            target.setProperty(prefix + "skills", formatSkills(profile.purchasedSkills));
            target.setProperty(prefix + "death.lostSkills",
                    formatSkills(profile.deathLostSkills));
            target.setProperty(prefix + "elfAttribute.present",
                    Boolean.toString(profile.elfAttributePresent));
            target.setProperty(prefix + "elfAttribute.source",
                    Integer.toString(profile.observedSourceElfAttribute));
            target.setProperty(prefix + "elfAttribute.effective",
                    Integer.toString(profile.effectiveElfAttribute));
            target.setProperty(prefix + "elfAttribute.forgottenSkills",
                    formatSkills(profile.forgottenSkills));
            target.setProperty(prefix + "partner", profile.partner);
            MutableProfile partner = profiles.get(profile.partnerStorageKey);
            target.setProperty(prefix + "partner.source",
                    partner == null ? "" : partner.identitySource);
            target.setProperty(prefix + "mercenary.castleId",
                    Integer.toString(profile.mercenaryAssignment.castleId()));
            target.setProperty(prefix + "mercenary.0",
                    Integer.toString(profile.mercenaryAssignment.mercenaries().type0()));
            target.setProperty(prefix + "mercenary.1",
                    Integer.toString(profile.mercenaryAssignment.mercenaries().type1()));
            target.setProperty(prefix + "mercenary.2",
                    Integer.toString(profile.mercenaryAssignment.mercenaries().type2()));
            target.setProperty(prefix + "mercenary.3",
                    Integer.toString(profile.mercenaryAssignment.mercenaries().type3()));
            List<MutableSourceProjectile> sourceProjectiles =
                    profile.sourceProjectiles.values().stream()
                            .sorted(Comparator.comparingLong(projectile -> projectile.objectId))
                            .toList();
            target.setProperty(prefix + "sourceProjectile.count",
                    Integer.toString(sourceProjectiles.size()));
            for (int index = 0; index < sourceProjectiles.size(); index++) {
                MutableSourceProjectile projectile = sourceProjectiles.get(index);
                String projectilePrefix = prefix + "sourceProjectile." + index + ".";
                target.setProperty(projectilePrefix + "objectId",
                        Long.toString(projectile.objectId));
                target.setProperty(projectilePrefix + "itemId",
                        Integer.toString(projectile.itemId));
                target.setProperty(projectilePrefix + "observedCount",
                        Long.toString(projectile.observedCount));
                target.setProperty(projectilePrefix + "remainingCount",
                        Long.toString(projectile.remainingCount));
            }
            List<MutableSourceInventory> sourceInventory =
                    profile.sourceInventory.values().stream()
                            .sorted(Comparator.comparingLong(item -> item.sourceObjectId))
                            .toList();
            target.setProperty(prefix + "sourceInventory.count",
                    Integer.toString(sourceInventory.size()));
            for (int index = 0; index < sourceInventory.size(); index++) {
                MutableSourceInventory item = sourceInventory.get(index);
                String inventoryPrefix = prefix + "sourceInventory." + index + ".";
                target.setProperty(inventoryPrefix + "sourceObjectId",
                        Long.toString(item.sourceObjectId));
                target.setProperty(inventoryPrefix + "localObjectId",
                        Long.toString(item.localObjectId));
                target.setProperty(inventoryPrefix + "catalogId",
                        Integer.toString(item.catalogId));
                target.setProperty(inventoryPrefix + "observedFingerprint",
                        item.observedFingerprint);
                target.setProperty(inventoryPrefix + "consumed",
                        item.consumed ? "1" : "0");
                target.setProperty(inventoryPrefix + "litoOwned",
                        item.litoOwned ? "1" : "0");
            }
            MutableSourceWeaponDurability sourceWeapon =
                    profile.sourceWeaponDurability;
            target.setProperty(prefix + "sourceWeapon.itemId",
                    Integer.toString(sourceWeapon == null ? 0 : sourceWeapon.itemId));
            target.setProperty(prefix + "sourceWeapon.observedDurability",
                    Integer.toString(sourceWeapon == null
                            ? 0 : sourceWeapon.observedDurability));
            target.setProperty(prefix + "sourceWeapon.effectiveDurability",
                    Integer.toString(sourceWeapon == null
                            ? 0 : sourceWeapon.effectiveDurability));
            List<Bookmark> bookmarks = profile.bookmarks.values().stream()
                    .sorted(Comparator.comparing(Bookmark::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            target.setProperty(prefix + "bookmarks.count", Integer.toString(bookmarks.size()));
            for (int index = 0; index < bookmarks.size(); index++) {
                Bookmark bookmark = bookmarks.get(index);
                String bookmarkPrefix = prefix + "bookmark." + index + ".";
                target.setProperty(bookmarkPrefix + "name", bookmark.name());
                target.setProperty(bookmarkPrefix + "x", Integer.toString(bookmark.x()));
                target.setProperty(bookmarkPrefix + "y", Integer.toString(bookmark.y()));
                target.setProperty(bookmarkPrefix + "mapId", Integer.toString(bookmark.mapId()));
            }
            List<Letter> letters = profile.letters.stream()
                    .sorted(Comparator.comparingLong(Letter::id)).toList();
            target.setProperty(prefix + "letters.count", Integer.toString(letters.size()));
            for (int index = 0; index < letters.size(); index++) {
                Letter letter = letters.get(index);
                String letterPrefix = prefix + "letter." + index + ".";
                target.setProperty(letterPrefix + "id", Long.toString(letter.id()));
                target.setProperty(letterPrefix + "code", Integer.toString(letter.code()));
                target.setProperty(letterPrefix + "sender", letter.sender());
                target.setProperty(letterPrefix + "receiver", letter.receiver());
                target.setProperty(letterPrefix + "date", letter.date());
                target.setProperty(letterPrefix + "templateId",
                        Integer.toString(letter.templateId()));
                target.setProperty(letterPrefix + "subject", letter.subject());
                target.setProperty(letterPrefix + "content", letter.content());
                target.setProperty(letterPrefix + "opened",
                        Boolean.toString(letter.opened()));
            }
        }
        List<Map.Entry<String, Map<Long, Integer>>> warehouses = accountWarehouses.entrySet()
                .stream()
                .filter(entry -> !ephemeralSourceProfiles
                        || !sourceQualifiedKey(entry.getKey()))
                .sorted(Map.Entry.comparingByKey()).toList();
        target.setProperty("warehouse.account.count", Integer.toString(warehouses.size()));
        for (int index = 0; index < warehouses.size(); index++) {
            Map.Entry<String, Map<Long, Integer>> entry = warehouses.get(index);
            String prefix = "warehouse.account." + index + ".";
            target.setProperty(prefix + "owner", entry.getKey());
            target.setProperty(prefix + "inventory", formatInventory(entry.getValue()));
            target.setProperty(prefix + "items", formatItemStates(
                    accountWarehouseItemStates.getOrDefault(entry.getKey(), Map.of())));
        }
        saveScopedWarehouses(target, "warehouse.clan", clanWarehouses,
                clanWarehouseItemStates, ephemeralSourceProfiles);
        saveScopedWarehouses(target, "warehouse.elf", elfWarehouses,
                elfWarehouseItemStates, ephemeralSourceProfiles);
        List<Map.Entry<String, Integer>> passwords = warehousePasswords.entrySet()
                .stream().filter(entry -> entry.getValue() != null
                        && entry.getValue() > 0 && entry.getValue() <= 999_999
                        && (!ephemeralSourceProfiles
                            || !sourceQualifiedKey(entry.getKey())))
                .sorted(Map.Entry.comparingByKey()).toList();
        target.setProperty("warehouse.password.count", Integer.toString(passwords.size()));
        for (int index = 0; index < passwords.size(); index++) {
            Map.Entry<String, Integer> entry = passwords.get(index);
            String prefix = "warehouse.password." + index + ".";
            target.setProperty(prefix + "owner", entry.getKey());
            target.setProperty(prefix + "value", Integer.toString(entry.getValue()));
        }
        List<MutableCastle> durableCastles = castles.values().stream()
                .sorted(Comparator.comparing((MutableCastle castle) -> key(castle.sourceId))
                        .thenComparingInt(castle -> castle.castleId)).toList();
        target.setProperty("castle.count", Integer.toString(durableCastles.size()));
        for (int index = 0; index < durableCastles.size(); index++) {
            MutableCastle castle = durableCastles.get(index);
            String prefix = "castle." + index + ".";
            target.setProperty(prefix + "source", castle.sourceId);
            target.setProperty(prefix + "id", Integer.toString(castle.castleId));
            target.setProperty(prefix + "taxRate", Integer.toString(castle.taxRate));
            target.setProperty(prefix + "publicMoney", Long.toString(castle.publicMoney));
            target.setProperty(prefix + "warTimeMillis",
                    Long.toString(castle.warTimeMillis));
            target.setProperty(prefix + "securityEnabled",
                    Boolean.toString(castle.securityEnabled));
            target.setProperty(prefix + "mercenary.0",
                    Integer.toString(castle.mercenaries.type0()));
            target.setProperty(prefix + "mercenary.1",
                    Integer.toString(castle.mercenaries.type1()));
            target.setProperty(prefix + "mercenary.2",
                    Integer.toString(castle.mercenaries.type2()));
            target.setProperty(prefix + "mercenary.3",
                    Integer.toString(castle.mercenaries.type3()));
            target.setProperty(prefix + "revision", Long.toString(castle.revision));
        }
        List<CastleSecurityLedger> durableCastleSecuritySettlements =
                castleSecuritySettlements.values().stream()
                        .sorted(Comparator.comparing(
                                        CastleSecurityLedger::sourceId,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparingInt(CastleSecurityLedger::castleId)
                                .thenComparingLong(
                                        CastleSecurityLedger::warStartMillis))
                        .toList();
        target.setProperty("castle.security.settlement.count",
                Integer.toString(durableCastleSecuritySettlements.size()));
        for (int index = 0;
                index < durableCastleSecuritySettlements.size(); index++) {
            CastleSecurityLedger settlement =
                    durableCastleSecuritySettlements.get(index);
            String prefix = "castle.security.settlement." + index + ".";
            target.setProperty(prefix + "source", settlement.sourceId());
            target.setProperty(prefix + "id",
                    Integer.toString(settlement.castleId()));
            target.setProperty(prefix + "warStartMillis",
                    Long.toString(settlement.warStartMillis()));
            target.setProperty(prefix + "chargeAdena",
                    Long.toString(settlement.chargeAdena()));
            target.setProperty(prefix + "executed",
                    Boolean.toString(settlement.executed()));
            target.setProperty(prefix + "completed",
                    Boolean.toString(settlement.completed()));
        }
        List<MutableCastleOwnership> durableOwnerships =
                castleOwnerships.values().stream()
                        .sorted(Comparator.comparing(
                                (MutableCastleOwnership ownership) ->
                                        key(ownership.sourceId))
                                .thenComparingInt(ownership -> ownership.castleId))
                        .toList();
        target.setProperty("castle.ownership.count",
                Integer.toString(durableOwnerships.size()));
        for (int index = 0; index < durableOwnerships.size(); index++) {
            MutableCastleOwnership ownership = durableOwnerships.get(index);
            String prefix = "castle.ownership." + index + ".";
            target.setProperty(prefix + "source", ownership.sourceId);
            target.setProperty(prefix + "id", Integer.toString(ownership.castleId));
            target.setProperty(prefix + "clan", ownership.clanName);
            target.setProperty(prefix + "leader", ownership.leaderName);
            target.setProperty(prefix + "leaderObjectId",
                    Long.toString(ownership.leaderObjectId));
            target.setProperty(prefix + "revision", Long.toString(ownership.revision));
        }
        List<String> banned = bannedAccounts.stream().sorted().toList();
        target.setProperty("banned.count", Integer.toString(banned.size()));
        for (int index = 0; index < banned.size(); index++) {
            target.setProperty("banned." + index + ".account", banned.get(index));
        }
        List<Map.Entry<String, CharacterSummary>> flattened = new ArrayList<>();
        createdCharacters.forEach((account, characters) -> {
            if (!ephemeralSourceProfiles || !sourceQualifiedKey(account)) {
                characters.forEach(character ->
                        flattened.add(Map.entry(account, character)));
            }
        });
        flattened.sort(Comparator.comparing((Map.Entry<String, CharacterSummary> entry) -> entry.getKey())
                .thenComparing(entry -> entry.getValue().name(), String.CASE_INSENSITIVE_ORDER));
        target.setProperty("created.count", Integer.toString(flattened.size()));
        for (int index = 0; index < flattened.size(); index++) {
            Map.Entry<String, CharacterSummary> entry = flattened.get(index);
            CharacterSummary character = entry.getValue();
            String prefix = "created." + index + ".";
            target.setProperty(prefix + "account", entry.getKey());
            target.setProperty(prefix + "name", character.name());
            target.setProperty(prefix + "clan", character.clanName());
            target.setProperty(prefix + "type", Integer.toString(character.type()));
            target.setProperty(prefix + "sex", Integer.toString(character.sex()));
            target.setProperty(prefix + "level", Integer.toString(character.level()));
            target.setProperty(prefix + "maxHp", Integer.toString(character.maxHp()));
            target.setProperty(prefix + "maxMp", Integer.toString(character.maxMp()));
            target.setProperty(prefix + "armorClass", Integer.toString(character.armorClass()));
            target.setProperty(prefix + "strength", Integer.toString(character.strength()));
            target.setProperty(prefix + "constitution", Integer.toString(character.constitution()));
            target.setProperty(prefix + "dexterity", Integer.toString(character.dexterity()));
            target.setProperty(prefix + "charisma", Integer.toString(character.charisma()));
            target.setProperty(prefix + "intelligence", Integer.toString(character.intelligence()));
            target.setProperty(prefix + "wisdom", Integer.toString(character.wisdom()));
            target.setProperty(prefix + "originalIntelligence",
                    Integer.toString(character.originalIntelligence()));
            target.setProperty(prefix + "originalWisdom",
                    Integer.toString(character.originalWisdom()));
            target.setProperty(prefix + "objectId", Long.toString(character.objectId()));
            target.setProperty(prefix + "experience", Long.toString(character.experience()));
            target.setProperty(prefix + "lawful", Long.toString(character.lawful()));
            target.setProperty(prefix + "pkCount", Integer.toString(character.pkCount()));
            target.setProperty(prefix + "classId", Long.toString(character.classId()));
            target.setProperty(prefix + "locationX", Long.toString(character.locationX()));
            target.setProperty(prefix + "locationY", Long.toString(character.locationY()));
            target.setProperty(prefix + "mapId", Long.toString(character.mapId()));
        }
        List<BoardTopic> durableTopics = boardTopics.values().stream()
                .sorted(Comparator.comparing((BoardTopic topic) -> key(topic.sourceId()))
                        .thenComparingLong(BoardTopic::id))
                .toList();
        target.setProperty("board.count", Integer.toString(durableTopics.size()));
        for (int index = 0; index < durableTopics.size(); index++) {
            BoardTopic topic = durableTopics.get(index);
            String prefix = "board." + index + ".";
            target.setProperty(prefix + "source", topic.sourceId());
            target.setProperty(prefix + "id", Long.toString(topic.id()));
            target.setProperty(prefix + "author", topic.author());
            target.setProperty(prefix + "author.identity", topic.authorIdentity());
            target.setProperty(prefix + "date", topic.date());
            target.setProperty(prefix + "title", topic.title());
            target.setProperty(prefix + "content", topic.content());
        }
        List<MutableHouseAuction> durableAuctions = houseAuctions.values().stream()
                .sorted(Comparator.comparing(
                                (MutableHouseAuction auction) -> key(auction.sourceId))
                        .thenComparingLong(auction -> auction.houseId))
                .toList();
        target.setProperty("house.auction.count", Integer.toString(durableAuctions.size()));
        for (int index = 0; index < durableAuctions.size(); index++) {
            MutableHouseAuction auction = durableAuctions.get(index);
            String prefix = "house.auction." + index + ".";
            target.setProperty(prefix + "source", auction.sourceId);
            target.setProperty(prefix + "houseId", Long.toString(auction.houseId));
            target.setProperty(prefix + "name", auction.name);
            target.setProperty(prefix + "area", Integer.toString(auction.area));
            target.setProperty(prefix + "deadlineEpochSeconds",
                    Long.toString(auction.deadlineEpochSeconds));
            target.setProperty(prefix + "price", Long.toString(auction.price));
            target.setProperty(prefix + "location", auction.location);
            target.setProperty(prefix + "oldOwner", auction.oldOwner);
            target.setProperty(prefix + "oldOwnerObjectId",
                    Long.toString(auction.oldOwnerObjectId));
            target.setProperty(prefix + "bidder", auction.bidder);
            MutableProfile bidder = profiles.get(auction.bidderStorageKey);
            target.setProperty(prefix + "bidder.source",
                    bidder == null ? "" : bidder.identitySource);
            target.setProperty(prefix + "bidderObjectId",
                    Long.toString(auction.bidderObjectId));
            target.setProperty(prefix + "revision", Long.toString(auction.revision));
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            target.store(writer, "LiTo381 local server state; written atomically");
        }
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Map<Long, Integer> parseInventory(String source) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (source != null) {
            for (String entry : source.split(",")) {
                String[] pair = entry.trim().split(":", 2);
                if (pair.length != 2) continue;
                try {
                    long item = Long.parseLong(pair[0]);
                    int count = Integer.parseInt(pair[1]);
                    if (item > 0 && count > 0) result.put(item, count);
                } catch (NumberFormatException ignored) {
                    // Ignore a damaged entry without discarding the rest of the profile.
                }
            }
        }
        if (result.isEmpty()) result.putAll(INITIAL_INVENTORY);
        return result;
    }

    private static Map<Long, List<Integer>> parseEnchantments(
            String source, Map<Long, Integer> inventory) {
        Map<Long, List<Integer>> parsed = new LinkedHashMap<>();
        if (source != null && !source.isBlank()) {
            for (String entry : source.split(",")) {
                String[] pair = entry.trim().split(":", 2);
                if (pair.length != 2) continue;
                try {
                    long catalogId = Long.parseLong(pair[0]);
                    List<Integer> levels = new ArrayList<>();
                    for (String token : pair[1].split("/")) {
                        int level = Integer.parseInt(token.trim());
                        if (level < -128 || level > 127) {
                            throw new NumberFormatException("enchant level out of range");
                        }
                        levels.add(level);
                    }
                    if (catalogId > 0 && !levels.isEmpty()) {
                        parsed.put(catalogId, List.copyOf(levels));
                    }
                } catch (NumberFormatException ignored) {
                    // Preserve every valid catalog when one enchant row is damaged.
                }
            }
        }
        return normalizedEnchantments(parsed, inventory);
    }

    private static Map<Long, Integer> parseWarehouse(String source) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (source == null || source.isBlank()) return result;
        for (String entry : source.split(",")) {
            String[] pair = entry.trim().split(":", 2);
            if (pair.length != 2) continue;
            try {
                long item = Long.parseLong(pair[0]);
                int count = Integer.parseInt(pair[1]);
                if (item > 0 && count > 0) result.put(item, count);
            } catch (NumberFormatException ignored) {
                // Preserve all valid rows when one warehouse entry is damaged.
            }
        }
        return result;
    }

    private static Map<Long, List<RuntimeInventory.PersistentItem>> parseItemStates(
            String source, Map<Long, Integer> inventory) {
        Map<Long, List<RuntimeInventory.PersistentItem>> parsed = new LinkedHashMap<>();
        if (source != null && !source.isBlank()) {
            for (String entry : source.split(",")) {
                String[] pair = entry.trim().split(":", 2);
                if (pair.length != 2) continue;
                try {
                    long catalogId = Long.parseLong(pair[0]);
                    List<RuntimeInventory.PersistentItem> items = new ArrayList<>();
                    for (String itemToken : pair[1].split("/")) {
                        String[] fields = itemToken.trim().split("~", -1);
                        if (fields.length < 5 || fields.length > 14) {
                            throw new NumberFormatException("invalid item row");
                        }
                        int identified = Integer.parseInt(fields[3]);
                        if (identified != 0 && identified != 1) {
                            throw new NumberFormatException("invalid identified bit");
                        }
                        long objectId = fields.length >= 6
                                ? Long.parseLong(fields[5]) : 0;
                        int sourceBound = fields.length >= 7
                                ? Integer.parseInt(fields[6]) : 0;
                        if (sourceBound != 0 && sourceBound != 1) {
                            throw new NumberFormatException("invalid source-bound bit");
                        }
                        int equipped = fields.length >= 8
                                ? Integer.parseInt(fields[7]) : 0;
                        if (equipped != 0 && equipped != 1) {
                            throw new NumberFormatException("invalid equipped bit");
                        }
                        InventoryCatalog.Definition definitionOverride = fields.length >= 9
                                && !fields[8].isEmpty()
                                ? DurableItemDefinitionCodec.decode(fields[8]) : null;
                        if (definitionOverride != null
                                && definitionOverride.catalogId() != catalogId) {
                            throw new NumberFormatException(
                                    "item definition/catalog id mismatch");
                        }
                        InventoryCatalog.Definition effectiveDefinition =
                                definitionOverride == null
                                        ? InventoryCatalog.find(catalogId)
                                        : definitionOverride;
                        int chargeCount = fields.length >= 10
                                ? Integer.parseInt(fields[9])
                                : effectiveDefinition == null
                                        ? 0 : effectiveDefinition.initialChargeCount();
                        int tournamentReward = fields.length >= 11
                                ? Integer.parseInt(fields[10]) : 0;
                        if (tournamentReward != 0 && tournamentReward != 1) {
                            throw new NumberFormatException(
                                    "invalid tournament-reward bit");
                        }
                        long expiresAtEpochMillis = fields.length >= 12
                                ? Long.parseLong(fields[11]) : 0L;
                        int sealed = fields.length >= 13
                                ? Integer.parseInt(fields[12]) : 0;
                        int litoOwned = fields.length >= 14
                                ? Integer.parseInt(fields[13]) : 0;
                        if (expiresAtEpochMillis < 0 || sealed != 0 && sealed != 1
                                || litoOwned != 0 && litoOwned != 1) {
                            throw new NumberFormatException(
                                    "invalid timed/sealed item state");
                        }
                        items.add(new RuntimeInventory.PersistentItem(
                                objectId, Integer.parseInt(fields[0]),
                                Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                                identified == 1, Integer.parseInt(fields[4]),
                                sourceBound == 1, tournamentReward == 1,
                                equipped == 1, definitionOverride,
                                chargeCount, expiresAtEpochMillis, sealed == 1,
                                litoOwned == 1));
                    }
                    if (catalogId > 0 && !items.isEmpty()) {
                        parsed.put(catalogId, List.copyOf(items));
                    }
                } catch (IllegalArgumentException ignored) {
                    // Preserve every valid catalog when one item-state row is damaged.
                }
            }
        }
        return normalizedItemStates(parsed, inventory);
    }

    private static void replacePositiveCounts(Map<Long, Integer> target,
                                              Map<Long, Integer> source) {
        target.clear();
        source.forEach((item, count) -> {
            if (item != null && item > 0 && count != null && count > 0) {
                target.put(item, count);
            }
        });
    }

    private static Map<Long, Integer> normalizedPositiveCounts(Map<Long, Integer> source) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        if (source != null) replacePositiveCounts(result, source);
        return result;
    }

    private static void decrementCount(Map<Long, Integer> inventory, long catalogId) {
        int current = inventory.getOrDefault(catalogId, 0);
        if (current <= 0) throw new IllegalStateException("validated inventory debit failed");
        if (current == 1) inventory.remove(catalogId);
        else inventory.put(catalogId, current - 1);
    }

    private static int inventorySlots(Map<Long, Integer> inventory) {
        long slots = 0;
        for (Map.Entry<Long, Integer> entry : inventory.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) continue;
            InventoryCatalog.Definition definition = InventoryCatalog.find(entry.getKey());
            if (definition == null || definition.stackable()) slots++;
            else slots += entry.getValue();
            if (slots > RuntimeInventory.MAX_SLOTS) return RuntimeInventory.MAX_SLOTS + 1;
        }
        return (int) slots;
    }

    private static int letterIndex(MutableProfile profile, long letterId) {
        if (letterId <= 0) return -1;
        for (int index = 0; index < profile.letters.size(); index++) {
            if (profile.letters.get(index).id() == letterId) return index;
        }
        return -1;
    }

    private static String formatInventory(Map<Long, Integer> inventory) {
        List<String> values = new ArrayList<>();
        inventory.forEach((item, count) -> {
            if (item > 0 && count != null && count > 0) values.add(item + ":" + count);
        });
        return String.join(",", values);
    }

    private static String formatEnchantments(Map<Long, List<Integer>> enchantments) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Long, List<Integer>> entry : enchantments.entrySet()) {
            if (result.length() > 0) result.append(',');
            result.append(entry.getKey()).append(':');
            for (int index = 0; index < entry.getValue().size(); index++) {
                if (index > 0) result.append('/');
                result.append(entry.getValue().get(index));
            }
        }
        return result.toString();
    }

    private static String formatItemStates(
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates) {
        StringBuilder result = new StringBuilder();
        if (itemStates == null) return "";
        for (Map.Entry<Long, List<RuntimeInventory.PersistentItem>> entry
                : itemStates.entrySet()) {
            if (entry.getKey() == null || entry.getKey() <= 0
                    || entry.getValue() == null || entry.getValue().isEmpty()) continue;
            if (result.length() > 0) result.append(',');
            result.append(entry.getKey()).append(':');
            for (int index = 0; index < entry.getValue().size(); index++) {
                RuntimeInventory.PersistentItem item = entry.getValue().get(index);
                if (index > 0) result.append('/');
                result.append(item.quantity()).append('~')
                        .append(item.enchantLevel()).append('~')
                        .append(item.durability()).append('~')
                        .append(item.identified() ? 1 : 0).append('~')
                        .append(item.blessing()).append('~')
                        .append(item.objectId()).append('~')
                        .append(item.sourceBound() ? 1 : 0).append('~')
                        .append(item.equipped() ? 1 : 0).append('~');
                if (item.definitionOverride() != null) {
                    result.append(DurableItemDefinitionCodec.encode(
                            item.definitionOverride()));
                }
                result.append('~').append(item.chargeCount()).append('~')
                        .append(item.tournamentReward() ? 1 : 0).append('~')
                        .append(item.expiresAtEpochMillis()).append('~')
                        .append(item.sealed() ? 1 : 0).append('~')
                        .append(item.litoOwned() ? 1 : 0);
            }
        }
        return result.toString();
    }

    private static Map<Long, List<RuntimeInventory.PersistentItem>> normalizedItemStates(
            Map<Long, List<RuntimeInventory.PersistentItem>> source,
            Map<Long, Integer> inventory) {
        Map<Long, List<RuntimeInventory.PersistentItem>> result = new LinkedHashMap<>();
        if (source == null || inventory == null) return result;
        for (Map.Entry<Long, List<RuntimeInventory.PersistentItem>> entry : source.entrySet()) {
            Long catalogId = entry.getKey();
            Integer count = catalogId == null ? null : inventory.get(catalogId);
            if (catalogId == null || catalogId <= 0 || count == null || count <= 0
                    || entry.getValue() == null) continue;
            InventoryCatalog.Definition definition = InventoryCatalog.find(catalogId);
            if (definition == null) definition = InventoryCatalog.retainedUnknown(catalogId);
            List<RuntimeInventory.PersistentItem> accepted = new ArrayList<>();
            int total = 0;
            boolean hasNonDefault = false;
            for (RuntimeInventory.PersistentItem item : entry.getValue()) {
                InventoryCatalog.Definition itemDefinition;
                try {
                    itemDefinition = item == null ? null
                            : item.effectiveDefinition(definition, catalogId);
                } catch (IllegalArgumentException damaged) {
                    continue;
                }
                if (item == null || total + item.quantity() > count
                        || (!itemDefinition.stackable() && item.quantity() != 1)
                        || ((item.enchantLevel() != 0 || item.durability() != 0)
                                && !itemDefinition.weapon() && !item.sourceBound())
                        || (item.equipped() && !item.sourceBound()
                                && !itemDefinition.weapon() && !itemDefinition.ring()
                                && !itemDefinition.light())
                        || item.chargeCount() < 0
                        || item.chargeCount() > itemDefinition.maximumChargeCount()) continue;
                accepted.add(item);
                total += item.quantity();
                hasNonDefault |= !item.isDefaultFor(itemDefinition);
            }
            if (!hasNonDefault) continue;
            while (total < count) {
                int quantity = definition.stackable() ? count - total : 1;
                accepted.add(RuntimeInventory.PersistentItem.defaults(definition, quantity));
                total += quantity;
            }
            result.put(catalogId, List.copyOf(accepted));
        }
        return result;
    }

    private static Map<Long, List<RuntimeInventory.PersistentItem>> itemStatesFromEnchantments(
            Map<Long, List<Integer>> enchantments, Map<Long, Integer> inventory) {
        Map<Long, List<Integer>> normalized = normalizedEnchantments(enchantments, inventory);
        Map<Long, List<RuntimeInventory.PersistentItem>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Integer>> entry : normalized.entrySet()) {
            InventoryCatalog.Definition definition = InventoryCatalog.find(entry.getKey());
            if (definition == null) definition = InventoryCatalog.retainedUnknown(entry.getKey());
            int count = inventory.getOrDefault(entry.getKey(), 0);
            List<RuntimeInventory.PersistentItem> items = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                int level = index < entry.getValue().size() ? entry.getValue().get(index) : 0;
                items.add(new RuntimeInventory.PersistentItem(1, level, 0,
                        definition.identified(), definition.blessing()));
            }
            if (!items.isEmpty()) result.put(entry.getKey(), List.copyOf(items));
        }
        return normalizedItemStates(result, inventory);
    }

    private static Map<Long, List<Integer>> enchantmentsFromItemStates(
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            Map<Long, Integer> inventory) {
        Map<Long, List<Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<RuntimeInventory.PersistentItem>> entry
                : normalizedItemStates(itemStates, inventory).entrySet()) {
            if (InventoryCatalog.nativeWeaponStatus(entry.getKey()) == null) continue;
            List<Integer> levels = new ArrayList<>();
            int lastNonZero = -1;
            for (RuntimeInventory.PersistentItem item : entry.getValue()) {
                for (int index = 0; index < item.quantity(); index++) {
                    levels.add(item.enchantLevel());
                    if (item.enchantLevel() != 0) lastNonZero = levels.size() - 1;
                }
            }
            if (lastNonZero >= 0) {
                result.put(entry.getKey(), List.copyOf(levels.subList(0, lastNonZero + 1)));
            }
        }
        return result;
    }

    private static Map<Long, List<RuntimeInventory.PersistentItem>> immutableItemStates(
            Map<Long, List<RuntimeInventory.PersistentItem>> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<Long, List<RuntimeInventory.PersistentItem>> result = new LinkedHashMap<>();
        source.forEach((catalogId, items) -> result.put(catalogId, List.copyOf(items)));
        return Map.copyOf(result);
    }

    private static Map<Long, List<RuntimeInventory.PersistentItem>> mutableItemStates(
            Map<Long, List<RuntimeInventory.PersistentItem>> source) {
        Map<Long, List<RuntimeInventory.PersistentItem>> result = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((catalogId, items) -> result.put(catalogId, List.copyOf(items)));
        }
        return result;
    }

    private static void replaceItemStates(
            Map<Long, List<RuntimeInventory.PersistentItem>> target,
            Map<Long, List<RuntimeInventory.PersistentItem>> source) {
        target.clear();
        if (source != null) {
            source.forEach((catalogId, items) -> target.put(catalogId, List.copyOf(items)));
        }
    }

    private static Map<Long, List<Integer>> normalizedEnchantments(
            Map<Long, List<Integer>> source, Map<Long, Integer> inventory) {
        Map<Long, List<Integer>> result = new LinkedHashMap<>();
        if (source == null || inventory == null) return result;
        for (Map.Entry<Long, List<Integer>> entry : source.entrySet()) {
            Long catalogId = entry.getKey();
            Integer count = catalogId == null ? null : inventory.get(catalogId);
            InventoryCatalog.NativeWeaponStatus status = catalogId == null ? null
                    : InventoryCatalog.nativeWeaponStatus(catalogId);
            if (catalogId == null || catalogId <= 0 || count == null || count <= 0
                    || status == null || entry.getValue() == null) continue;
            List<Integer> levels = new ArrayList<>();
            int limit = Math.min(count, entry.getValue().size());
            int lastNonZero = -1;
            for (int index = 0; index < limit; index++) {
                Integer level = entry.getValue().get(index);
                int value = level == null ? 0 : level;
                if (value < -128 || value > 127) {
                    throw new IllegalArgumentException("invalid durable enchant level");
                }
                levels.add(value);
                if (value != 0) lastNonZero = index;
            }
            if (lastNonZero >= 0) {
                result.put(catalogId,
                        List.copyOf(levels.subList(0, lastNonZero + 1)));
            }
        }
        return result;
    }

    private static Map<Long, List<Integer>> immutableEnchantments(
            Map<Long, List<Integer>> source) {
        Map<Long, List<Integer>> result = new LinkedHashMap<>();
        source.forEach((catalogId, levels) ->
                result.put(catalogId, List.copyOf(levels)));
        return Map.copyOf(result);
    }

    private static void replaceEnchantments(Map<Long, List<Integer>> target,
                                            Map<Long, List<Integer>> source) {
        target.clear();
        source.forEach((catalogId, levels) ->
                target.put(catalogId, List.copyOf(levels)));
    }

    private static Set<Integer> parseSkills(String source) {
        Set<Integer> result = new LinkedHashSet<>();
        if (source == null || source.isBlank()) return result;
        for (String entry : source.split(",")) {
            try {
                int skillId = Integer.parseInt(entry.trim());
                if (skillId > 0 && skillId <= SpellShopCatalog.MAX_NATIVE_SKILL_ID) {
                    result.add(skillId);
                }
            } catch (NumberFormatException ignored) {
                // Preserve valid rows when one durable entry is damaged.
            }
        }
        return result;
    }

    private static Set<Integer> immutableNativeSkills(Set<Integer> source) {
        if (source == null || source.stream().anyMatch(skillId -> skillId == null
                || skillId <= 0 || skillId > SpellShopCatalog.MAX_NATIVE_SKILL_ID
                || BattleSpellCatalog.reservedSkillSlot(skillId) != null)) {
            throw new IllegalArgumentException("invalid native skill overlay");
        }
        return Set.copyOf(new LinkedHashSet<>(source));
    }

    private static String formatSkills(Set<Integer> skillIds) {
        return skillIds.stream().sorted().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static long number(Properties source, String key, long fallback) {
        try { return Long.parseLong(source.getProperty(key, Long.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int integer(Properties source, String key, int fallback) {
        long value = number(source, key, fallback);
        return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? fallback : (int) value;
    }

    private static boolean validLawful(int value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }

    private static final int[] FACTION_KARMA_POINTS = {
            10_000, 20_000, 100_000, 500_000, 1_500_000,
            3_000_000, 5_000_000, 10_000_000, 15_500_000
    };

    private static int clampFactionKarma(int value) {
        return Math.max(-15_500_000, Math.min(15_500_000, value));
    }

    private static void requireFactionKarma(int value, String name) {
        if (value < -15_500_000 || value > 15_500_000) {
            throw new IllegalArgumentException(name + " is outside native karma range");
        }
    }

    private static int factionKarmaLevel(int value) {
        boolean negative = value < 0;
        int magnitude = Math.abs(value);
        int level = 0;
        for (int point : FACTION_KARMA_POINTS) {
            if (magnitude < point) break;
            if (++level >= 8) break;
        }
        return negative ? -level : level;
    }

    private static int factionKarmaPercent(int value) {
        int level = factionKarmaLevel(value);
        if (level == 0) return 0;
        int magnitude = Math.abs(value);
        int index = Math.abs(level) - 1;
        return 100 * (magnitude - FACTION_KARMA_POINTS[index])
                / (FACTION_KARMA_POINTS[index + 1]
                    - FACTION_KARMA_POINTS[index]);
    }

    private static void requireLawful(int value, String name) {
        if (!validLawful(value)) {
            throw new IllegalArgumentException(name + " is outside signed-word range");
        }
    }

    private static boolean validElfAttribute(int value) {
        return value >= 0 && (value & ~0x0F) == 0;
    }

    private static void requireElfAttribute(int value, String name) {
        if (!validElfAttribute(value)) {
            throw new IllegalArgumentException(name + " is outside the native attribute mask");
        }
    }

    private static boolean validClassicExperience(long value) {
        return value >= 0 && value <= 0xFFFF_FFFFL;
    }

    private static void requireClassicExperience(long value, String name) {
        if (!validClassicExperience(value)) {
            throw new IllegalArgumentException(name + " is outside unsigned-dword range");
        }
    }

    private static boolean validClassicLevel(int value) {
        return value >= 1 && value <= ClassicExperienceCatalog.MAXIMUM_LEVEL;
    }

    private static void requireClassicLevel(int value, String name) {
        if (!validClassicLevel(value)) {
            throw new IllegalArgumentException(name + " is outside selected EXP table");
        }
    }

    private static long saturatedAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) return Long.MAX_VALUE;
        return left + right;
    }

    private static int classicLevelAfterReward(
            int beforeLevel, long beforeExperience, long afterExperience) {
        if (beforeLevel >= ClassicExperienceCatalog.MAXIMUM_LEVEL) return beforeLevel;
        if (ClassicExperienceCatalog.isConsistent(beforeLevel, beforeExperience)) {
            return ClassicExperienceCatalog.levelForExperience(afterExperience);
        }
        // Memory fixtures predate source EXP qualification and may deliberately
        // carry level N with EXP 0. Preserve that authoritative source level
        // until the overlay crosses its next selected-table threshold.
        int result = beforeLevel;
        while (result < ClassicExperienceCatalog.MAXIMUM_LEVEL
                && afterExperience >= ClassicExperienceCatalog
                    .minimumExperienceForLevel(result + 1)) {
            result++;
        }
        return result;
    }

    private long allocatePetObjectId() {
        if (nextPetObjectId < FIRST_PET_OBJECT_ID
                || nextPetObjectId > LAST_PET_OBJECT_ID) {
            throw new IllegalStateException("LiTo pet object-id range exhausted");
        }
        return nextPetObjectId++;
    }

    private long allocateCreatedObjectId() {
        if (nextCreatedObjectId < FIRST_CREATED_OBJECT_ID
                || nextCreatedObjectId > 0x6FFF_FFFFL) {
            throw new IllegalStateException("LiTo character object-id range exhausted");
        }
        return nextCreatedObjectId++;
    }

    private static void loadScopedWarehouses(
            Properties source, String propertyPrefix,
            Map<String, Map<Long, Integer>> destination,
            Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>> stateDestination) {
        int count = Math.max(0, integer(source, propertyPrefix + ".count", 0));
        for (int index = 0; index < count; index++) {
            String prefix = propertyPrefix + "." + index + ".";
            String owner = source.getProperty(prefix + "owner", "").trim();
            if (owner.isEmpty()) continue;
            String normalizedOwner = key(owner);
            Map<Long, Integer> inventory = parseWarehouse(
                    source.getProperty(prefix + "inventory", ""));
            destination.put(normalizedOwner, inventory);
            stateDestination.put(normalizedOwner, parseItemStates(
                    source.getProperty(prefix + "items", ""), inventory));
        }
    }

    private static void saveScopedWarehouses(
            Properties target, String propertyPrefix,
            Map<String, Map<Long, Integer>> source,
            Map<String, Map<Long, List<RuntimeInventory.PersistentItem>>> stateSource,
            boolean excludeSourceQualified) {
        List<Map.Entry<String, Map<Long, Integer>>> rows = source.entrySet().stream()
                .filter(entry -> !excludeSourceQualified
                        || !sourceQualifiedKey(entry.getKey()))
                .sorted(Map.Entry.comparingByKey()).toList();
        target.setProperty(propertyPrefix + ".count", Integer.toString(rows.size()));
        for (int index = 0; index < rows.size(); index++) {
            Map.Entry<String, Map<Long, Integer>> entry = rows.get(index);
            String prefix = propertyPrefix + "." + index + ".";
            target.setProperty(prefix + "owner", entry.getKey());
            target.setProperty(prefix + "inventory", formatInventory(entry.getValue()));
            target.setProperty(prefix + "items", formatItemStates(
                    stateSource.getOrDefault(entry.getKey(), Map.of())));
        }
    }

    private static String warehouseOwner(String account, String character) {
        String owner = account == null ? "" : account.trim();
        if (owner.isEmpty()) owner = character == null ? "" : character.trim();
        if (owner.isEmpty()) owner = "Unknown";
        return key(owner);
    }

    private static StoryQuestState storyQuestSnapshot(MutableProfile profile) {
        return new StoryQuestState(
                profile.observedSourceQuestSteps, profile.effectiveQuestSteps);
    }

    private static ElfAttributeState elfAttributeSnapshot(MutableProfile profile) {
        return new ElfAttributeState(
                profile.observedSourceElfAttribute,
                profile.effectiveElfAttribute,
                profile.forgottenSkills);
    }

    private static Map<Integer, Integer> immutableQuestSteps(
            Map<Integer, Integer> source) {
        return Map.copyOf(normalizedQuestSteps(source));
    }

    private static Map<Integer, Integer> normalizedQuestSteps(
            Map<Integer, Integer> source) {
        if (source == null) throw new IllegalArgumentException("quest steps are required");
        Map<Integer, Integer> result = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Integer questId = entry.getKey();
                    Integer step = entry.getValue();
                    if (questId == null || questId <= 0 || step == null || step < 0) {
                        throw new IllegalArgumentException("invalid story quest step");
                    }
                    result.put(questId, step);
                });
        return result;
    }

    private static void replaceQuestSteps(
            Map<Integer, Integer> target, Map<Integer, Integer> source) {
        target.clear();
        target.putAll(source);
    }

    private static void saveQuestSteps(
            Properties target, String prefix, Map<Integer, Integer> steps) {
        List<Map.Entry<Integer, Integer>> rows = steps.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList();
        target.setProperty(prefix + ".count", Integer.toString(rows.size()));
        for (int index = 0; index < rows.size(); index++) {
            Map.Entry<Integer, Integer> row = rows.get(index);
            String rowPrefix = prefix + "." + index + ".";
            target.setProperty(rowPrefix + "id", Integer.toString(row.getKey()));
            target.setProperty(rowPrefix + "step", Integer.toString(row.getValue()));
        }
    }

    private static void removeMappedSourceObject(
            RuntimeInventory inventory, MutableSourceInventory mapped) throws IOException {
        RuntimeInventory.Item present = inventory.item(mapped.localObjectId);
        if (present == null) return;
        if (present.definition().catalogId() != mapped.catalogId) {
            throw new IOException("source inventory mapping points at a different catalog: "
                    + mapped.localObjectId);
        }
        inventory.removeEntire(mapped.localObjectId);
    }

    /** Removes a mapped source object before dynamic templates are rebuilt. */
    private static void removeMappedSourceObject(
            Map<Long, Integer> counts,
            Map<Long, List<RuntimeInventory.PersistentItem>> itemStates,
            MutableSourceInventory mapped) throws IOException {
        List<RuntimeInventory.PersistentItem> states = itemStates.get(
                (long) mapped.catalogId);
        if (states == null) return;
        List<RuntimeInventory.PersistentItem> retained = new ArrayList<>();
        RuntimeInventory.PersistentItem removed = null;
        for (RuntimeInventory.PersistentItem item : states) {
            if (item.objectId() == mapped.localObjectId) {
                if (removed != null) {
                    throw new IOException("duplicate durable source inventory object: "
                            + mapped.localObjectId);
                }
                removed = item;
            } else {
                retained.add(item);
            }
        }
        if (removed == null) return;
        int current = counts.getOrDefault((long) mapped.catalogId, 0);
        if (current < removed.quantity()) {
            throw new IOException("source inventory count is below its mapped object: "
                    + mapped.localObjectId);
        }
        if (current == removed.quantity()) counts.remove((long) mapped.catalogId);
        else counts.put((long) mapped.catalogId, current - removed.quantity());
        if (retained.isEmpty()) itemStates.remove((long) mapped.catalogId);
        else itemStates.put((long) mapped.catalogId, List.copyOf(retained));
    }

    private static String sourceInventoryFingerprint(SourceInventoryItem item) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    item.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private Set<Long> sourceProfileObjectIds() {
        Set<Long> objectIds = new LinkedHashSet<>();
        for (MutableProfile profile : profiles.values()) {
            if (profile.identitySource.isBlank()) continue;
            objectIds.addAll(profileInventoryObjectIds(profile));
        }
        return objectIds;
    }

    private static Set<Long> profileInventoryObjectIds(MutableProfile profile) {
        Set<Long> objectIds = new LinkedHashSet<>();
        for (List<RuntimeInventory.PersistentItem> itemStates
                : profile.inventoryItemStates.values()) {
            for (RuntimeInventory.PersistentItem item : itemStates) {
                if (item.objectId() > 0) objectIds.add(item.objectId());
            }
        }
        for (MutableSourceInventory item : profile.sourceInventory.values()) {
            if (item.localObjectId > 0) objectIds.add(item.localObjectId);
        }
        return objectIds;
    }

    /**
     * Finds pet collars owned by stale source-qualified sidecar profiles before
     * those profiles are intentionally skipped during read-only startup.
     */
    private static Set<Long> sourceProfileObjectIds(Properties source) {
        Set<Long> objectIds = new LinkedHashSet<>();
        for (String property : source.stringPropertyNames()) {
            if (!property.startsWith("profile.") || !property.endsWith(".name")) {
                continue;
            }
            String profileToken = property.substring("profile.".length(),
                    property.length() - ".name".length());
            String prefix = "profile." + profileToken + ".";
            String identitySource = source.getProperty(
                    prefix + "identity.source",
                    source.getProperty(prefix + "ranking.server", "")).trim();
            if (identitySource.isEmpty()) continue;
            try {
                Map<Long, Integer> inventory = parseInventory(
                        source.getProperty(prefix + "inventory", "2001:1"));
                Map<Long, List<RuntimeInventory.PersistentItem>> states =
                        parseItemStates(source.getProperty(
                                prefix + "inventory.items", ""), inventory);
                for (List<RuntimeInventory.PersistentItem> rows : states.values()) {
                    for (RuntimeInventory.PersistentItem item : rows) {
                        if (item.objectId() > 0) objectIds.add(item.objectId());
                    }
                }
                int mappingCount = Math.max(0,
                        integer(source, prefix + "sourceInventory.count", 0));
                for (int index = 0; index < mappingCount; index++) {
                    long localObjectId = number(source,
                            prefix + "sourceInventory." + index + ".localObjectId", 0);
                    if (localObjectId > 0) objectIds.add(localObjectId);
                }
            } catch (IllegalArgumentException damaged) {
                // A damaged stale profile must not prevent the valid world
                // sidecar from starting; nonblank pet provenance is still
                // independently rejected by the ephemeral load path.
            }
        }
        return objectIds;
    }

    private static Set<Long> sourceWarehouseObjectIds(Properties source) {
        Set<Long> objectIds = new LinkedHashSet<>();
        for (String warehousePrefix : List.of(
                "warehouse.account", "warehouse.clan", "warehouse.elf")) {
            int count = Math.max(0, integer(source, warehousePrefix + ".count", 0));
            for (int index = 0; index < count; index++) {
                String prefix = warehousePrefix + "." + index + ".";
                String owner = source.getProperty(prefix + "owner", "").trim();
                if (!sourceQualifiedKey(owner)) continue;
                try {
                    Map<Long, Integer> inventory = parseWarehouse(
                            source.getProperty(prefix + "inventory", ""));
                    Map<Long, List<RuntimeInventory.PersistentItem>> states =
                            parseItemStates(source.getProperty(
                                    prefix + "items", ""), inventory);
                    for (List<RuntimeInventory.PersistentItem> rows : states.values()) {
                        for (RuntimeInventory.PersistentItem item : rows) {
                            if (item.objectId() > 0) objectIds.add(item.objectId());
                        }
                    }
                } catch (IllegalArgumentException damaged) {
                    // Ignore only this stale cache row. It is omitted from the
                    // rewritten ephemeral sidecar regardless.
                }
            }
        }
        return objectIds;
    }

    private static boolean sourceQualifiedKey(String value) {
        return value != null && value.indexOf(SOURCE_CHARACTER_SEPARATOR) > 0;
    }

    private static String key(String name) { return name.toLowerCase(java.util.Locale.ROOT); }
    private static String token(String name) { return URLEncoder.encode(name, StandardCharsets.UTF_8); }

    private record ProfileIdentity(String source, String name, String storageKey) {}

    private static final class MutableProfile {
        final String name;
        String identitySource = "";
        long points;
        long rankingPoints;
        int wins;
        int losses;
        int disconnects;
        int monthlyRankingPeriod;
        long monthlyRankingPoints;
        int monthlyWins;
        int monthlyLosses;
        final int[] tournamentLegacyCertificateCodes =
                new int[TOURNAMENT_LEGACY_CERTIFICATE_SLOTS];
        final int[] tournamentVictoryBadgeEditions =
                new int[TOURNAMENT_VICTORY_BADGE_MODE_COUNT];
        boolean tournamentLitoMaster;
        String classicServerName = "";
        int channel = -1;
        int sourceLevel = -1;
        int race = -1;
        boolean rankingSnapshotPresent;
        long snapshotRankingPoints;
        int snapshotWins;
        int snapshotLosses;
        String snapshotClassicServerName = "";
        int snapshotChannel = -1;
        int snapshotSourceLevel = -1;
        int snapshotRace = -1;
        boolean monthlyRankingSnapshotPresent;
        int snapshotMonthlyRankingPeriod;
        long snapshotMonthlyRankingPoints;
        int snapshotMonthlyWins;
        int snapshotMonthlyLosses;
        int snapshotMonthlyMovement;
        int snapshotOverallMovement;
        int snapshotClassicServerMovement;
        int snapshotChannelMovement;
        int snapshotLevelMovement;
        int snapshotRaceMovement;
        long experience;
        int level;
        boolean lawfulPresent;
        int observedSourceLawful;
        int effectiveLawful;
        boolean generalPkPresent;
        int observedSourceGeneralPkCount;
        long observedSourceLastPkEpochSeconds;
        int effectiveGeneralPkCount;
        long effectiveLastPkEpochSeconds;
        boolean classicKillDeathPresent;
        int observedSourceClassicKills;
        int observedSourceClassicDeaths;
        int effectiveClassicKills;
        int effectiveClassicDeaths;
        boolean hellPresent;
        int observedSourceHellTicks;
        int effectiveHellTicks;
        boolean elfPkPresent;
        int observedSourceElfPkCount;
        long observedSourceLastPkForElfEpochSeconds;
        int effectiveElfPkCount;
        long effectiveLastPkForElfEpochSeconds;
        int factionKarma;
        boolean classicExperiencePresent;
        long observedSourceExperience;
        int observedSourceLevel = 1;
        long effectiveClassicExperience;
        int effectiveClassicLevel = 1;
        boolean storyQuestsPresent;
        final Map<Integer, Integer> observedSourceQuestSteps = new LinkedHashMap<>();
        final Map<Integer, Integer> effectiveQuestSteps = new LinkedHashMap<>();
        int timedExperienceBonus;
        int timedExperienceRemainingSeconds;
        int fireWeaponRemainingSeconds;
        int burningWeaponRemainingSeconds;
        int windShotRemainingSeconds;
        int fireBlessRemainingSeconds;
        int stormEyeRemainingSeconds;
        int stormShotRemainingSeconds;
        int earthBlessRemainingSeconds;
        int ironSkinRemainingSeconds;
        int resistFearRemainingSeconds;
        int movementBuffSkillId;
        int movementBuffRemainingSeconds;
        int armorProtectionSkillId;
        int armorProtectionRemainingSeconds;
        long waterDragonBloodExpiresAtEpochSeconds;
        String partner = "";
        String partnerStorageKey = "";
        final Map<Long, Integer> inventory = new LinkedHashMap<>();
        final Map<Long, List<RuntimeInventory.PersistentItem>> inventoryItemStates =
                new LinkedHashMap<>();
        final Map<Long, Integer> warehouse = new LinkedHashMap<>();
        final Map<Long, MutableSourceProjectile> sourceProjectiles = new LinkedHashMap<>();
        final Map<Long, MutableSourceInventory> sourceInventory = new LinkedHashMap<>();
        MutableSourceWeaponDurability sourceWeaponDurability;
        final Map<String, Bookmark> bookmarks = new LinkedHashMap<>();
        final Set<Integer> purchasedSkills = new LinkedHashSet<>();
        final Set<Integer> deathLostSkills = new LinkedHashSet<>();
        boolean elfAttributePresent;
        int observedSourceElfAttribute;
        int effectiveElfAttribute;
        final Set<Integer> forgottenSkills = new LinkedHashSet<>();
        final List<Letter> letters = new ArrayList<>();
        MercenaryAssignment mercenaryAssignment = MercenaryAssignment.empty();

        MutableProfile(String name, long points, int wins, int losses, long experience, int level,
                       Map<Long, Integer> inventory) {
            this.name = name;
            this.points = points;
            this.wins = wins;
            this.losses = losses;
            this.experience = experience;
            this.level = level;
            this.inventory.putAll(inventory);
        }
    }

    private static final class MutableSourceProjectile {
        final long objectId;
        final int itemId;
        final long observedCount;
        final long remainingCount;

        MutableSourceProjectile(long objectId, int itemId,
                                long observedCount, long remainingCount) {
            SourceProjectileBalance validated = new SourceProjectileBalance(
                    objectId, itemId, observedCount, remainingCount);
            this.objectId = validated.objectId();
            this.itemId = validated.itemId();
            this.observedCount = validated.observedCount();
            this.remainingCount = validated.remainingCount();
        }

        SourceProjectileBalance snapshot() {
            return new SourceProjectileBalance(
                    objectId, itemId, observedCount, remainingCount);
        }
    }

    private static final class MutableSourceInventory {
        final long sourceObjectId;
        final long localObjectId;
        final int catalogId;
        final String observedFingerprint;
        final boolean consumed;
        final boolean litoOwned;

        MutableSourceInventory(long sourceObjectId, long localObjectId,
                               int catalogId, String observedFingerprint) {
            this(sourceObjectId, localObjectId, catalogId,
                    observedFingerprint, false, false);
        }

        MutableSourceInventory(long sourceObjectId, long localObjectId,
                               int catalogId, String observedFingerprint,
                               boolean consumed) {
            this(sourceObjectId, localObjectId, catalogId,
                    observedFingerprint, consumed, false);
        }

        MutableSourceInventory(long sourceObjectId, long localObjectId,
                               int catalogId, String observedFingerprint,
                               boolean consumed, boolean litoOwned) {
            if (sourceObjectId <= 0 || sourceObjectId > 0xFFFF_FFFFL
                    || localObjectId <= 0 || localObjectId > 0xFFFF_FFFFL
                    || catalogId <= 0 || observedFingerprint == null
                    || !observedFingerprint.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid source inventory mapping");
            }
            this.sourceObjectId = sourceObjectId;
            this.localObjectId = localObjectId;
            this.catalogId = catalogId;
            this.observedFingerprint = observedFingerprint;
            this.consumed = consumed;
            this.litoOwned = litoOwned;
        }

        MutableSourceInventory consumed() {
            return consumed ? this : new MutableSourceInventory(
                    sourceObjectId, localObjectId, catalogId,
                    observedFingerprint, true, litoOwned);
        }
    }

    private static final class MutableSourceWeaponDurability {
        final int itemId;
        final int observedDurability;
        final int effectiveDurability;

        MutableSourceWeaponDurability(
                int itemId, int observedDurability, int effectiveDurability) {
            SourceWeaponDurabilityBalance validated =
                    new SourceWeaponDurabilityBalance(
                            itemId, observedDurability, effectiveDurability);
            this.itemId = validated.itemId();
            this.observedDurability = validated.observedDurability();
            this.effectiveDurability = validated.effectiveDurability();
        }

        SourceWeaponDurabilityBalance snapshot() {
            return new SourceWeaponDurabilityBalance(
                    itemId, observedDurability, effectiveDurability);
        }
    }

    private static final class MutableCastle {
        final String sourceId;
        final int castleId;
        int taxRate;
        long publicMoney;
        long warTimeMillis;
        boolean securityEnabled;
        MercenaryRoster mercenaries;
        long revision;

        MutableCastle(String sourceId, int castleId, int taxRate, long publicMoney,
                      long warTimeMillis, boolean securityEnabled,
                      MercenaryRoster mercenaries, long revision) {
            this.sourceId = normalizedCastleSource(sourceId);
            this.castleId = castleId;
            this.taxRate = taxRate;
            this.publicMoney = publicMoney;
            this.warTimeMillis = warTimeMillis;
            this.securityEnabled = securityEnabled;
            this.mercenaries = mercenaries;
            this.revision = revision;
        }

        CastleState snapshot() {
            return new CastleState(
                    sourceId, castleId, taxRate, publicMoney, warTimeMillis,
                    securityEnabled, mercenaries, revision);
        }

        MutableCastle withSource(String sourceId) {
            return new MutableCastle(sourceId, castleId, taxRate, publicMoney,
                    warTimeMillis, securityEnabled, mercenaries, revision);
        }
    }

    private record CastleSecurityLedger(
            String sourceId, int castleId, long warStartMillis,
            long chargeAdena, boolean executed, boolean completed) {
        CastleSecurityLedger {
            sourceId = normalizedCastleSource(sourceId);
            if (castleId < 1 || castleId > 7 || warStartMillis <= 0
                    || chargeAdena <= 0 || chargeAdena > Integer.MAX_VALUE
                    || (completed && !executed && chargeAdena <= 0)) {
                throw new IllegalArgumentException(
                        "invalid castle security ledger");
            }
        }

        CastleSecurityLedger completedCopy() {
            return completed ? this : new CastleSecurityLedger(
                    sourceId, castleId, warStartMillis,
                    chargeAdena, executed, true);
        }
    }

    private static final class MutableCastleOwnership {
        final String sourceId;
        final int castleId;
        final String clanName;
        final String leaderName;
        final long leaderObjectId;
        final long revision;

        MutableCastleOwnership(String sourceId, int castleId, String clanName,
                               String leaderName, long leaderObjectId,
                               long revision) {
            CastleOwnershipState validated = new CastleOwnershipState(
                    sourceId, castleId, clanName, leaderName,
                    leaderObjectId, revision);
            this.sourceId = validated.sourceId();
            this.castleId = validated.castleId();
            this.clanName = validated.clanName();
            this.leaderName = validated.leaderName();
            this.leaderObjectId = validated.leaderObjectId();
            this.revision = validated.revision();
        }

        CastleOwnershipState snapshot() {
            return new CastleOwnershipState(sourceId, castleId, clanName,
                    leaderName, leaderObjectId, revision);
        }
    }

    private static final class MutableHouseAuction {
        final String sourceId;
        final long houseId;
        final String name;
        final int area;
        long deadlineEpochSeconds;
        long price;
        final String location;
        final String oldOwner;
        final long oldOwnerObjectId;
        String bidder;
        String bidderStorageKey = "";
        long bidderObjectId;
        long revision;

        MutableHouseAuction(String sourceId, long houseId, String name, int area,
                            long deadlineEpochSeconds, long price,
                            String location, String oldOwner, String bidder,
                            long bidderObjectId, long revision) {
            this(sourceId, houseId, name, area, deadlineEpochSeconds, price,
                    location, oldOwner, 0, bidder, bidderObjectId, revision);
        }

        MutableHouseAuction(String sourceId, long houseId, String name, int area,
                            long deadlineEpochSeconds, long price,
                            String location, String oldOwner,
                            long oldOwnerObjectId, String bidder,
                            long bidderObjectId, long revision) {
            this.sourceId = normalizedSourceId(sourceId);
            this.houseId = houseId;
            this.name = name;
            this.area = area;
            this.deadlineEpochSeconds = deadlineEpochSeconds;
            this.price = price;
            this.location = location;
            this.oldOwner = oldOwner;
            this.oldOwnerObjectId = oldOwnerObjectId;
            this.bidder = bidder;
            this.bidderObjectId = bidderObjectId;
            this.revision = revision;
        }

        HouseAuction snapshot() {
            return new HouseAuction(sourceId, houseId, name, area, deadlineEpochSeconds,
                    price, location, oldOwner, oldOwnerObjectId,
                    bidder, bidderObjectId, revision);
        }

        MutableHouseAuction withSource(String sourceId) {
            MutableHouseAuction copy = new MutableHouseAuction(
                    sourceId, houseId, name, area, deadlineEpochSeconds,
                    price, location, oldOwner, oldOwnerObjectId,
                    bidder, bidderObjectId, revision);
            copy.bidderStorageKey = bidderStorageKey;
            return copy;
        }

        void restore(HouseAuction source) {
            deadlineEpochSeconds = source.deadlineEpochSeconds();
            price = source.price();
            bidder = source.bidder();
            bidderObjectId = source.bidderObjectId();
            revision = source.revision();
        }
    }
}
