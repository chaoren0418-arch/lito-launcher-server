// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import local.lito381.server.NativeDeArmorTemplate;
import local.lito381.server.NativeDeDarkItem;
import local.lito381.server.NativeDeDeletionOwner;
import local.lito381.server.NativeDeDeletionPorts;
import local.lito381.server.NativeDeDeletionState;
import local.lito381.server.NativeDeItemAbility;
import local.lito381.server.NativeDeItemMetadata;
import local.lito381.server.NativeDeItemTemplate;
import local.lito381.server.NativeDeItemVipTemplate;
import local.lito381.server.NativeDeObjectFields;
import local.lito381.server.NativeDeResurrectionEffects;
import local.lito381.server.NativeDeWeaponTemplate;
import local.lito381.server.NativeDeWorldLog;

final class NativeDeWorldKinds {
    NativeDeWorldKinds() {
    }

    static class Dwarf
    extends Npc {
        Dwarf(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static class Merchant
    extends Npc {
        Merchant(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static class Bow
    extends Npc {
        Bow(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static class Effect
    extends Npc {
        Effect(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static class Doll
    extends Npc {
        Doll(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static class Pet
    extends Npc {
        Pet(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static abstract class Npc
    extends NativeDeDeletionState {
        Npc(int n, NativeDeResurrectionEffects nativeDeResurrectionEffects, NativeDeDeletionOwner nativeDeDeletionOwner) {
            super(n, nativeDeResurrectionEffects, nativeDeDeletionOwner);
        }
    }

    static class Trap
    extends NativeDeObjectFields
    implements NativeDeDeletionPorts.ObjectRef {
        private final int nativeObjectId;

        Trap(int n) {
            this.nativeObjectId = n;
        }

        @Override
        protected int getId() {
            return this.nativeObjectId;
        }
    }

    static class Item
    extends NativeDeObjectFields {
        private static final NativeDeWorldLog _log = NativeDeItemMetadata.Logs.getLogger(Item.class);
        private static final long serialVersionUID = 1L;
        private static final AtomicLong SCHEDULED_COUNT = new AtomicLong();
        private static final AtomicLong SUBMITTED_COUNT = new AtomicLong();
        private static final AtomicLong ACTIVE_COUNT = new AtomicLong();
        private static final AtomicLong COMPLETED_COUNT = new AtomicLong();
        private static final AtomicLong SKIP_COUNT = new AtomicLong();
        private static final AtomicLong REJECTED_COUNT = new AtomicLong();
        private static final AtomicLong MAGIC_STONE_SCHEDULE_COUNT = new AtomicLong();
        private static final AtomicLong MAGIC_STONE_PERSIST_COUNT = new AtomicLong();
        private static final AtomicLong MAGIC_STONE_REFRESH_COUNT = new AtomicLong();
        private static final AtomicLong EQUIPMENT_TIMER_COUNT = new AtomicLong();
        private static final AtomicLong ERROR_COUNT = new AtomicLong();
        private volatile long _count;
        private final AtomicLong _countMutationVersion;
        private int _itemId;
        private boolean _isEquipped;
        private boolean _isEquippedTemp;
        private int _enchantLevel;
        private boolean _isIdentified;
        private int _durability;
        private int _chargeCount;
        private final AtomicInteger _remainingTime;
        private int _lastWeight;
        private int _bless;
        private boolean _isSealed;
        private int _attrEnchantKind;
        private int _attrEnchantLevel;
        private int _greater;
        private int _enchantSaveMode;
        private int _updateSaveMode;
        private String _dropSourceName;
        private NativeDeDarkItem _darkItem;
        private int[] _inlayCards;
        private volatile int _magicStoneItemId;
        private volatile int _magicStoneLevel;
        private volatile Timestamp _magicStoneTime;
        private final List<Integer> _awakenPowerIds;
        private String _gamno;
        private String _lotto;
        private NativeDeItemTemplate _item;
        private volatile Timestamp _lastUsed;
        private final Object _delayEffectStateLock;
        private final AtomicReference<Long> _delayEffectRuntimeDeadlineNanos;
        private final LastStatus _lastStatus;
        private final NativeDeItemAbility _ability;
        private final ReentrantLock _abilityLock;
        private volatile transient NativeDeItemVipTemplate _appliedVipTemplate;
        private volatile boolean _abilityDirty;
        private boolean _abilityBatchLoading;
        private volatile ScheduledFuture<?> _magicStoneExpireFuture;
        private static final int MAGIC_STONE_EXPIRY_MAX_SUBMIT_ATTEMPTS = 20;
        private static final long MAGIC_STONE_EXPIRY_MAX_RETRY_NANOS = 30000000000L;
        private static final long MAGIC_STONE_EXPIRY_RETRY_DELAY_MILLIS = 100L;
        private long _magicStoneGeneration;
        private long _pendingMagicStoneExpiryGeneration;
        private long _magicStoneExpiryStartedNanos;
        private int _magicStoneExpirySubmitAttempts;
        private boolean _magicStoneExpirySubmissionInFlight;
        private boolean _magicStoneExpiryRetryScheduled;
        private boolean _magicStoneExpiryExhausted;
        private byte[] _cachedStatusBytes;
        private int _cachedStatusWeightOffset;
        private final ReentrantLock _statusCacheLock;
        private int _itemOwnerId;
        private static final int EQUIPMENT_EXPIRY_MAX_SUBMIT_ATTEMPTS = 20;
        private static final long EQUIPMENT_EXPIRY_MAX_RETRY_NANOS = 30000000000L;
        private static final long EQUIPMENT_EXPIRY_RETRY_DELAY_MILLIS = 100L;
        private final ReentrantLock _equipmentTimerLock;
        private volatile ScheduledFuture<?> _equipmentTimerFuture;
        private long _equipmentTimerGeneration;
        private EquipmentTimerLease _activeEquipmentTimer;
        private EquipmentTimerLease _pendingEquipmentExpiry;
        private boolean _equipmentExpiryCommitted;
        private boolean _isNowLighting;
        private boolean _isMatch;
        private int _char_objid;
        private Timestamp _time;
        private int _equipWindow;
        private final NativeDeItemMetadata nativeMetadata;
        private final int nativeObjectId;

        public Item(int n, NativeDeItemMetadata nativeDeItemMetadata) {
            this.nativeObjectId = n;
            this.nativeMetadata = NativeDeItemMetadata.require(n, nativeDeItemMetadata);
            this._countMutationVersion = new AtomicLong();
            this._isEquipped = false;
            this._isEquippedTemp = false;
            this._isIdentified = false;
            this._remainingTime = new AtomicInteger();
            this._isSealed = false;
            this._greater = 0;
            this._enchantSaveMode = 0;
            this._updateSaveMode = 0;
            this._dropSourceName = null;
            this._darkItem = null;
            this._inlayCards = new int[5];
            this._magicStoneItemId = 0;
            this._magicStoneLevel = 0;
            this._magicStoneTime = null;
            this._awakenPowerIds = new ArrayList<Integer>();
            this._gamno = null;
            this._lotto = null;
            this._lastUsed = null;
            this._delayEffectStateLock = new Object();
            this._delayEffectRuntimeDeadlineNanos = new AtomicReference();
            this._lastStatus = new LastStatus();
            this._ability = new NativeDeItemAbility();
            this._abilityLock = new ReentrantLock();
            this._abilityDirty = true;
            this._abilityBatchLoading = false;
            this._pendingMagicStoneExpiryGeneration = -1L;
            this._cachedStatusWeightOffset = -1;
            this._statusCacheLock = new ReentrantLock();
            this._itemOwnerId = 0;
            this._equipmentTimerLock = new ReentrantLock();
            this._isNowLighting = false;
            this._isMatch = false;
            this._char_objid = -1;
            this._time = null;
            this._count = 1L;
            this._enchantLevel = 0;
        }

        public Item(int n, NativeDeItemMetadata nativeDeItemMetadata, NativeDeItemTemplate nativeDeItemTemplate, long l) {
            this(n, nativeDeItemMetadata);
            this.setItem(nativeDeItemTemplate);
            this.setCount(l);
        }

        public boolean isIdentified() {
            return this._isIdentified;
        }

        public void setIdentified(boolean bl) {
            this._isIdentified = bl;
            this.invalidateStatusBytesCache();
        }

        public String getName() {
            return this._item.getNameId();
        }

        public long getCount() {
            return this._count;
        }

        public void setCount(long l) {
            this._count = l;
            this._countMutationVersion.incrementAndGet();
        }

        public long getCountMutationVersion() {
            return this._countMutationVersion.get();
        }

        public String getGamNo() {
            return this._gamno;
        }

        public void setGamNo(String string) {
            this._gamno = string;
        }

        public String getLotto() {
            return this._lotto;
        }

        public void setLotto(String string) {
            this._lotto = string;
        }

        public boolean isEquipped() {
            return this._isEquipped;
        }

        public void setEquipped(boolean bl) {
            if (this._isEquipped == bl) {
                return;
            }
            this._isEquipped = bl;
            this.invalidateStatusBytesCache();
        }

        public NativeDeItemTemplate getItem() {
            return this._item;
        }

        public void setItem(NativeDeItemTemplate nativeDeItemTemplate) {
            this._item = nativeDeItemTemplate;
            this._itemId = nativeDeItemTemplate.getItemId();
            if (nativeDeItemTemplate.getGreaterTier() > 0) {
                this.setGreater(nativeDeItemTemplate.getGreaterTier());
            }
            this.markAbilityDirty();
            this.loadDarkItem();
        }

        public void markAbilityDirty() {
            this._abilityDirty = true;
            this.invalidateStatusBytesCache();
        }

        private void invalidateStatusBytesCache() {
            this._cachedStatusBytes = null;
            this._cachedStatusWeightOffset = -1;
        }

        public void invalidateItemStatusCache() {
            this.invalidateStatusBytesCache();
        }

        public boolean isAbilityDirty() {
            return this._abilityDirty;
        }

        public void beginAbilityBatchLoad() {
            this._abilityLock.lock();
            try {
                this._abilityBatchLoading = true;
            }
            finally {
                this._abilityLock.unlock();
            }
        }

        private boolean applyMagicStoneExpiryIfNeeded() {
            if (this._magicStoneItemId <= 0) {
                return false;
            }
            if (this._magicStoneTime != null && System.currentTimeMillis() >= this._magicStoneTime.getTime()) {
                this.clearMagicStoneFieldsOnly();
                this._pendingMagicStoneExpiryGeneration = ++this._magicStoneGeneration;
                this._magicStoneExpiryStartedNanos = System.nanoTime();
                this._magicStoneExpirySubmitAttempts = 0;
                this._magicStoneExpirySubmissionInFlight = false;
                this._magicStoneExpiryRetryScheduled = false;
                this._magicStoneExpiryExhausted = false;
                return true;
            }
            return false;
        }

        private void clearMagicStoneFieldsOnly() {
            this._magicStoneItemId = 0;
            this._magicStoneLevel = 0;
            this._magicStoneTime = null;
            this.cancelMagicStoneExpiryTimer();
        }

        private void beginMagicStoneMutationLocked() {
            ++this._magicStoneGeneration;
            this._pendingMagicStoneExpiryGeneration = -1L;
            this._magicStoneExpirySubmitAttempts = 0;
            this._magicStoneExpirySubmissionInFlight = false;
            this._magicStoneExpiryRetryScheduled = false;
            this._magicStoneExpiryExhausted = false;
        }

        private void cancelMagicStoneExpiryTimer() {
            if (this._magicStoneExpireFuture != null) {
                this._magicStoneExpireFuture.cancel(false);
                this._magicStoneExpireFuture = null;
            }
        }

        private void hydrateAllAbilities(NativeDeItemTemplate nativeDeItemTemplate) {
            int n;
            int n2;
            int n3;
            int n4;
            int n5;
            int n6;
            int n7;
            int n8;
            int n9;
            int n10;
            int n11;
            NativeDeItemTemplate nativeDeItemTemplate2;
            int n12;
            int n13;
            int n14;
            byte by;
            byte by2;
            byte by3;
            byte by4;
            byte by5;
            this._ability.clear();
            byte by6 = nativeDeItemTemplate.get_addstr();
            if (by6 != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.STR, by6);
            }
            if ((by5 = nativeDeItemTemplate.get_adddex()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.DEX, by5);
            }
            if ((by4 = nativeDeItemTemplate.get_addcon()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.CON, by4);
            }
            if ((by3 = nativeDeItemTemplate.get_addint()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.INT, by3);
            }
            if ((by2 = nativeDeItemTemplate.get_addwis()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.WIS, by2);
            }
            if ((by = nativeDeItemTemplate.get_addcha()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.CHA, by);
            }
            if ((n14 = nativeDeItemTemplate.get_addhp()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.HP, n14);
            }
            if ((n13 = nativeDeItemTemplate.get_addmp()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.MP, n13);
            }
            if ((n12 = nativeDeItemTemplate.get_addsp()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.SP, n12);
            }
            if (nativeDeItemTemplate instanceof NativeDeArmorTemplate) {
                int n15;
                nativeDeItemTemplate2 = (NativeDeArmorTemplate)nativeDeItemTemplate;
                n11 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_ac();
                if (n11 != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.AC, n11);
                }
                if ((n10 = nativeDeItemTemplate2.get_mdef()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.MR, n10);
                }
                if ((n9 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_defense_fire()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.DEF_FIRE, n9);
                }
                if ((n8 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_defense_water()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.DEF_WATER, n8);
                }
                if ((n7 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_defense_earth()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.DEF_EARTH, n7);
                }
                if ((n6 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_defense_wind()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.DEF_WIND, n6);
                }
                if ((n5 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_regist_freeze()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.REGIST_FREEZE, n5);
                }
                if ((n4 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_regist_stone()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.REGIST_STONE, n4);
                }
                if ((n15 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_regist_sleep()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.REGIST_SLEEP, n15);
                }
                if ((n3 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_regist_blind()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.REGIST_BLIND, n3);
                }
                if ((n2 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_regist_stun()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.REGIST_STUN, n2);
                }
                if ((n = ((NativeDeArmorTemplate)nativeDeItemTemplate2).get_regist_sustain()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.REGIST_SUSTAIN, n);
                }
            }
            if (nativeDeItemTemplate instanceof NativeDeWeaponTemplate) {
                nativeDeItemTemplate2 = (NativeDeWeaponTemplate)nativeDeItemTemplate;
                this._ability.set(NativeDeItemAbility.AbilityType.BASE_DMG_SMALL, nativeDeItemTemplate2.getDmgSmall());
                this._ability.set(NativeDeItemAbility.AbilityType.BASE_DMG_LARGE, nativeDeItemTemplate2.getDmgLarge());
            }
            if (nativeDeItemTemplate instanceof NativeDeWeaponTemplate) {
                nativeDeItemTemplate2 = (NativeDeWeaponTemplate)nativeDeItemTemplate;
                n11 = ((NativeDeWeaponTemplate)nativeDeItemTemplate2).getHitModifier();
                n10 = ((NativeDeWeaponTemplate)nativeDeItemTemplate2).getDmgModifier();
                n9 = ((NativeDeWeaponTemplate)nativeDeItemTemplate2).getAddHitup();
                n8 = ((NativeDeWeaponTemplate)nativeDeItemTemplate2).getAddDmgup();
                n7 = ((NativeDeWeaponTemplate)nativeDeItemTemplate2).getAddBowHitup();
                n6 = ((NativeDeWeaponTemplate)nativeDeItemTemplate2).getAddBowDmgup();
                if (n11 != 0 || n9 != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.HIT, n11 + n9);
                }
                if (n10 != 0 || n8 != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.DMG, n10 + n8);
                }
                if (n11 != 0 || n7 != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.BOW_HIT, n11 + n7);
                }
                if (n10 != 0 || n6 != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.BOW_DMG, n10 + n6);
                }
            } else if (nativeDeItemTemplate instanceof NativeDeArmorTemplate) {
                nativeDeItemTemplate2 = (NativeDeArmorTemplate)nativeDeItemTemplate;
                n11 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).getHitModifierByArmor();
                n10 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).getDmgModifierByArmor();
                n9 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).getAddHitup();
                n8 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).getAddDmgup();
                if (n11 != 0 || n9 != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.HIT, n11 + n9);
                }
                if (n10 != 0 || n8 != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.DMG, n10 + n8);
                }
                if ((n7 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).getBowHitModifierByArmor() + ((NativeDeArmorTemplate)nativeDeItemTemplate2).getAddBowHitup()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.BOW_HIT, n7);
                }
                if ((n6 = ((NativeDeArmorTemplate)nativeDeItemTemplate2).getBowDmgModifierByArmor() + ((NativeDeArmorTemplate)nativeDeItemTemplate2).getAddBowDmgup()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.BOW_DMG, n6);
                }
            }
            int n16 = nativeDeItemTemplate.getAddStunSuccess();
            if (n16 != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.STUN_SUCCESS, n16);
            }
            if ((n11 = nativeDeItemTemplate.getMagicDmgModifier()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.MAGIC_DMG_MODIFIER, n11);
            }
            if ((n10 = nativeDeItemTemplate.getAddPotionRecovery()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.POTION_RECOVERY, n10);
            }
            if ((n9 = nativeDeItemTemplate.getAddPoisonResist()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.POISON_RESIST, n9);
            }
            if ((n8 = nativeDeItemTemplate.getAddExpRate()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.EXP_RATE, n8);
            }
            if ((n7 = nativeDeItemTemplate.getAddPierce()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.PIERCE, n7);
            }
            if ((n6 = nativeDeItemTemplate.getMagicBow()) != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.MAGIC_BOW, n6);
            }
            n5 = nativeDeItemTemplate.getArrowSpr();
            this._ability.add(NativeDeItemAbility.AbilityType.ARROW_SPR, n5);
            n4 = nativeDeItemTemplate.getAddMagicHit();
            if (n4 != 0) {
                this._ability.add(NativeDeItemAbility.AbilityType.MAGIC_HIT, n4);
            }
            if (nativeDeItemTemplate instanceof NativeDeWeaponTemplate) {
                int n17;
                int n18;
                int n19;
                int n20;
                int n21;
                int n22;
                NativeDeWeaponTemplate nativeDeWeaponTemplate = (NativeDeWeaponTemplate)nativeDeItemTemplate;
                n3 = nativeDeWeaponTemplate.getAddIgnoreReduction();
                if (n3 != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.IGNORE_REDUCTION, n3);
                }
                if ((n2 = nativeDeWeaponTemplate.getAddPvpDmg()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.PVP_DMG, n2);
                }
                if ((n = nativeDeWeaponTemplate.getAddPvpRedux()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.PVP_REDUX, n);
                }
                if ((n22 = nativeDeWeaponTemplate.getAddBossDmg()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.DMG_BOSS, n22);
                }
                if ((n21 = nativeDeWeaponTemplate.getAddBossRedux()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.BOSS_REDUX, n21);
                }
                if ((n20 = nativeDeWeaponTemplate.getAddDodge()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.DODGE, n20);
                }
                if ((n19 = nativeDeWeaponTemplate.getAddEr()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.ER, n19);
                }
                if ((n18 = nativeDeWeaponTemplate.getAddAdenaRate()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.ADENA_RATE, n18);
                }
                if ((n17 = nativeDeWeaponTemplate.getAddDropRate()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.DROP_RATE, n17);
                }
            } else if (nativeDeItemTemplate instanceof NativeDeArmorTemplate) {
                int n23;
                int n24;
                int n25;
                int n26;
                int n27;
                int n28;
                int n29;
                int n30;
                NativeDeArmorTemplate nativeDeArmorTemplate = (NativeDeArmorTemplate)nativeDeItemTemplate;
                n3 = nativeDeArmorTemplate.getAddIgnoreReduction();
                if (n3 != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.IGNORE_REDUCTION, n3);
                }
                if ((n2 = nativeDeArmorTemplate.getAddPvpRedux()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.PVP_REDUX, n2);
                }
                if ((n = nativeDeArmorTemplate.getAddPvpDmg()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.PVP_DMG, n);
                }
                if ((n30 = nativeDeArmorTemplate.getAddBossDmg()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.DMG_BOSS, n30);
                }
                if ((n29 = nativeDeArmorTemplate.getAddBossRedux()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.BOSS_REDUX, n29);
                }
                if ((n28 = nativeDeItemTemplate.getAddMeleeRedux()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.MELEE_REDUX, n28);
                }
                if ((n27 = nativeDeItemTemplate.getAddBowRedux()) != 0) {
                    this._ability.add(NativeDeItemAbility.AbilityType.BOW_REDUX, n27);
                }
                if ((n26 = nativeDeArmorTemplate.getAddDodge()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.DODGE, n26);
                }
                if ((n25 = nativeDeArmorTemplate.getAddEr()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.ER, n25);
                }
                if ((n24 = nativeDeArmorTemplate.getAddAdenaRate()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.ADENA_RATE, n24);
                }
                if ((n23 = nativeDeArmorTemplate.getAddDropRate()) != 0) {
                    this._ability.set(NativeDeItemAbility.AbilityType.DROP_RATE, n23);
                }
            }
        }

        public int getItemId() {
            return this._itemId;
        }

        public void setItemId(int n) {
            this._itemId = n;
        }

        public void setDropSourceName(String string) {
            this._dropSourceName = string;
        }

        public String getDropSourceName() {
            return this._dropSourceName;
        }

        public NativeDeDarkItem getDarkItem() {
            return this._darkItem;
        }

        public void setDarkItem(NativeDeDarkItem nativeDeDarkItem) {
            this._darkItem = nativeDeDarkItem;
            this.markAbilityDirty();
        }

        public void loadDarkItem() {
            if (this.getId() > 0) {
                NativeDeItemMetadata nativeDeItemMetadata = this.nativeMetadata;
                this._darkItem = nativeDeItemMetadata.getDarkItem(this.getId());
                if (this._darkItem != null) {
                    this.markAbilityDirty();
                }
            }
        }

        public boolean isStackable() {
            return this._item.isStackable();
        }

        @Override
        public void onAction(NativeDeDeletionPorts.Viewer viewer) {
        }

        public int getEnchantLevel() {
            return this._enchantLevel;
        }

        public void setEnchantLevel(int n) {
            this._enchantLevel = n;
            this.markAbilityDirty();
        }

        public int get_gfxid() {
            int n = this._item.getGfxId();
            return n;
        }

        public int get_durability() {
            return this._durability;
        }

        public int getChargeCount() {
            return this._chargeCount;
        }

        public void setChargeCount(int n) {
            this._chargeCount = n;
            this.invalidateStatusBytesCache();
        }

        public int getRemainingTime() {
            return this._remainingTime.get();
        }

        public void setRemainingTime(int n) {
            this._remainingTime.set(n);
            this.invalidateStatusBytesCache();
        }

        private boolean compareAndSetRemainingTime(int n, int n2) {
            if (!this._remainingTime.compareAndSet(n, n2)) {
                return false;
            }
            this.invalidateStatusBytesCache();
            return true;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void setLastUsed(Timestamp timestamp) {
            Object object = this._delayEffectStateLock;
            synchronized (object) {
                this._lastUsed = timestamp;
            }
        }

        public Timestamp getLastUsed() {
            return this._lastUsed;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public long getDelayEffectRuntimeRemainingMillis() {
            Object object = this._delayEffectStateLock;
            synchronized (object) {
                return this.getDelayEffectRuntimeRemainingMillisAt(System.nanoTime());
            }
        }

        long getDelayEffectRuntimeRemainingMillisAt(long l) {
            Long l2 = this._delayEffectRuntimeDeadlineNanos.get();
            if (l2 == null) {
                return 0L;
            }
            long l3 = l2 - l;
            if (l3 <= 0L) {
                return 0L;
            }
            return TimeUnit.NANOSECONDS.toMillis(l3 - 1L) + 1L;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void reserveDelayEffectRuntime(int n) {
            Object object = this._delayEffectStateLock;
            synchronized (object) {
                this.reserveDelayEffectRuntimeAt(n, System.nanoTime());
            }
        }

        void reserveDelayEffectRuntimeAt(int n, long l) {
            long l2 = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, (long)n));
            this._delayEffectRuntimeDeadlineNanos.set(l + l2);
        }

        public long getDelayEffectRemainingMillis(int n, long l) {
            return this.getDelayEffectRemainingMillisAt(n, l, System.nanoTime());
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        long getDelayEffectRemainingMillisAt(int n, long l, long l2) {
            Object object = this._delayEffectStateLock;
            synchronized (object) {
                long l3;
                long l4 = 0L;
                Timestamp timestamp = this._lastUsed;
                if (timestamp != null && n > 0 && (l3 = l - timestamp.getTime()) < (long)n) {
                    long l5 = (long)n - l3;
                    l4 = l5 > 0L ? l5 : Long.MAX_VALUE;
                }
                return Math.max(l4, this.getDelayEffectRuntimeRemainingMillisAt(l2));
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        Timestamp markDelayEffectUsedAt(int n, long l, long l2) {
            Object object = this._delayEffectStateLock;
            synchronized (object) {
                Timestamp timestamp;
                this.reserveDelayEffectRuntimeAt(n, l2);
                this._lastUsed = timestamp = new Timestamp(l);
                return timestamp;
            }
        }

        public int getLastWeight() {
            return this._lastWeight;
        }

        public void setLastWeight(int n) {
            this._lastWeight = n;
        }

        public void setBless(int n) {
            this._bless = n;
            this.markAbilityDirty();
        }

        public int getBless() {
            return this._bless;
        }

        public boolean isSealed() {
            return this._isSealed;
        }

        public void setSealed(boolean bl) {
            this._isSealed = bl;
        }

        public void setAttrEnchantKind(int n) {
            this._attrEnchantKind = n;
            this.markAbilityDirty();
        }

        public int getAttrEnchantKind() {
            return this._attrEnchantKind;
        }

        public void setAttrEnchantLevel(int n) {
            this._attrEnchantLevel = n;
            this.markAbilityDirty();
        }

        public int getAttrEnchantLevel() {
            return this._attrEnchantLevel;
        }

        public int getGreater() {
            return this._greater;
        }

        public void setGreater(int n) {
            this._greater = n;
            this.markAbilityDirty();
        }

        public int getEnchantSaveMode() {
            return this._enchantSaveMode;
        }

        public void setEnchantSaveMode(int n) {
            this._enchantSaveMode = n;
        }

        public int getUpdateSaveMode() {
            return this._updateSaveMode;
        }

        public void setUpdateSaveMode(int n) {
            this._updateSaveMode = n;
        }

        public void set_durability(int n) {
            if (n < 0) {
                n = 0;
            }
            if (n > 127) {
                n = 127;
            }
            if (this._durability != n) {
                this._durability = n;
                this.markAbilityDirty();
            }
        }

        public int getWeight() {
            int n = this.getItem().getWeight();
            if (n < 0 || this.getCount() <= 0L) {
                return Integer.MAX_VALUE;
            }
            if (n == 0) {
                return 0;
            }
            try {
                long l = Math.max(Math.multiplyExact(this.getCount(), (long)n) / 1000L, 1L);
                return l >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)l;
            }
            catch (ArithmeticException arithmeticException) {
                return Integer.MAX_VALUE;
            }
        }

        public LastStatus getLastStatus() {
            return this._lastStatus;
        }

        public int getRecordingColumns() {
            int n = 0;
            if (this.getCount() != this._lastStatus.count) {
                n += 16;
            }
            if (this.getItemId() != this._lastStatus.itemId) {
                n += 64;
            }
            if (this.isEquipped() != this._lastStatus.isEquipped) {
                n += 8;
            }
            if (this.getEnchantLevel() != this._lastStatus.enchantLevel) {
                n += 4;
            }
            if (this.get_durability() != this._lastStatus.durability) {
                ++n;
            }
            if (this.getChargeCount() != this._lastStatus.chargeCount) {
                n += 128;
            }
            if (this.getLastUsed() != this._lastStatus.lastUsed) {
                n += 32;
            }
            if (this.isIdentified() != this._lastStatus.isIdentified) {
                n += 2;
            }
            if (this.getRemainingTime() != this._lastStatus.remainingTime) {
                n += 256;
            }
            if (this.getBless() != this._lastStatus.bless) {
                n += 512;
            }
            if (this.getAttrEnchantKind() != this._lastStatus.attrEnchantKind) {
                n += 1024;
            }
            if (this.getAttrEnchantLevel() != this._lastStatus.attrEnchantLevel) {
                n += 2048;
            }
            if (this.getGreater() != this._lastStatus.greater) {
                n += 4096;
            }
            return n;
        }

        public NativeDeItemAbility getAbility() {
            return this._ability;
        }

        public NativeDeItemVipTemplate getAppliedVipTemplate() {
            return this._appliedVipTemplate;
        }

        public void setAppliedVipTemplate(NativeDeItemVipTemplate nativeDeItemVipTemplate) {
            this._appliedVipTemplate = nativeDeItemVipTemplate;
        }

        public int[] getInlayCards() {
            return this._inlayCards != null ? (int[])this._inlayCards.clone() : new int[5];
        }

        public void setInlayCards(int[] nArray) {
            if (nArray != null && nArray.length >= 5) {
                this._inlayCards = (int[])nArray.clone();
                this.markAbilityDirty();
            }
        }

        public int getOpenSlotCount() {
            if (this._inlayCards == null) {
                return 0;
            }
            int n = 0;
            for (int n2 : this._inlayCards) {
                if (n2 < 1) continue;
                ++n;
            }
            return n;
        }

        public int getFirstClosedSlotIndex() {
            if (this._inlayCards == null) {
                return -1;
            }
            for (int i = 0; i < this._inlayCards.length; ++i) {
                if (this._inlayCards[i] != 0) continue;
                return i;
            }
            return -1;
        }

        public int getFirstEmptySlotIndex() {
            if (this._inlayCards == null) {
                return -1;
            }
            for (int i = 0; i < this._inlayCards.length; ++i) {
                if (this._inlayCards[i] != 1) continue;
                return i;
            }
            return -1;
        }

        public int getOpenSlots() {
            return this.getOpenSlotCount();
        }

        public int getMagicStoneItemId() {
            return this._magicStoneItemId;
        }

        public int getMagicStoneLevel() {
            return this._magicStoneLevel;
        }

        public Timestamp getMagicStoneTime() {
            return this._magicStoneTime;
        }

        public void clearMagicStone() {
            this._abilityLock.lock();
            try {
                this.beginMagicStoneMutationLocked();
                this.clearMagicStoneFieldsOnly();
                this.markAbilityDirty();
            }
            finally {
                this._abilityLock.unlock();
            }
        }

        private byte[] materializeStatusBytes(byte[] byArray, int n) {
            if (n < 0) {
                return byArray;
            }
            int n2 = this.getWeight();
            byte[] byArray2 = (byte[])byArray.clone();
            byArray2[n] = (byte)(n2 & 0xFF);
            byArray2[n + 1] = (byte)(n2 >> 8 & 0xFF);
            byArray2[n + 2] = (byte)(n2 >> 16 & 0xFF);
            byArray2[n + 3] = (byte)(n2 >> 24 & 0xFF);
            return byArray2;
        }

        public int getMr() {
            return this._ability.get(NativeDeItemAbility.AbilityType.MR);
        }

        public int getItemOwnerId() {
            return this._itemOwnerId;
        }

        public void setItemOwnerId(int n) {
            this._itemOwnerId = n;
        }

        private void terminalizeEquipmentLocked(EquipmentTimerLease equipmentTimerLease) {
            if (this._activeEquipmentTimer != equipmentTimerLease) {
                return;
            }
            this._activeEquipmentTimer = null;
            this._equipmentTimerFuture = null;
            if (equipmentTimerLease.future != null) {
                equipmentTimerLease.future.cancel(false);
            }
            this._pendingEquipmentExpiry = equipmentTimerLease;
            this._equipmentExpiryCommitted = false;
            equipmentTimerLease.expiryStartedNanos = System.nanoTime();
        }

        private void cancelActiveEquipmentTimerLocked() {
            ++this._equipmentTimerGeneration;
            EquipmentTimerLease equipmentTimerLease = this._activeEquipmentTimer;
            this._activeEquipmentTimer = null;
            this._equipmentTimerFuture = null;
            if (equipmentTimerLease != null && equipmentTimerLease.future != null) {
                equipmentTimerLease.future.cancel(false);
            }
        }

        private void stopEquipmentTimerSerialized() {
            this._equipmentTimerLock.lock();
            try {
                this.cancelActiveEquipmentTimerLocked();
            }
            finally {
                this._equipmentTimerLock.unlock();
            }
        }

        public boolean isNowLighting() {
            return this._isNowLighting;
        }

        public void setNowLighting(boolean bl) {
            this._isNowLighting = bl;
        }

        public boolean isEquippedTemp() {
            return this._isEquippedTemp;
        }

        public void set_isEquippedTemp(boolean bl) {
            this._isEquippedTemp = bl;
        }

        public void setIsMatch(boolean bl) {
            this._isMatch = bl;
            this.markAbilityDirty();
        }

        public boolean isMatch() {
            return this._isMatch;
        }

        public void set_char_objid(int n) {
            this._char_objid = n;
        }

        public int get_char_objid() {
            return this._char_objid;
        }

        public void set_time(Timestamp timestamp) {
            this._time = timestamp;
        }

        public Timestamp get_time() {
            return this._time;
        }

        public final int getEquipWindow() {
            return this._equipWindow;
        }

        public final void setEquipWindow(int n) {
            this._equipWindow = n;
        }

        public List<Integer> getAwakenPowerIds() {
            return new ArrayList<Integer>(this._awakenPowerIds);
        }

        public void setAwakenPowerIds(List<Integer> list) {
            this._awakenPowerIds.clear();
            if (list != null) {
                for (Integer n : list) {
                    if (n == null || n <= 0) continue;
                    this._awakenPowerIds.add(n);
                }
            }
        }

        public void addAwakenPowerId(int n) {
            if (n > 0 && !this._awakenPowerIds.contains(n)) {
                this._awakenPowerIds.add(n);
            }
        }

        public void clearAwakenPowerIds() {
            this._awakenPowerIds.clear();
        }

        public static String getMonitorLine() {
            return String.format("L1ItemInstance scheduled=%d submit=%d active=%d done=%d skip=%d rejected=%d magicStoneExpire=%d magicStonePersist=%d magicStoneRefresh=%d equipmentTimer=%d error=%d", SCHEDULED_COUNT.get(), SUBMITTED_COUNT.get(), ACTIVE_COUNT.get(), COMPLETED_COUNT.get(), SKIP_COUNT.get(), REJECTED_COUNT.get(), MAGIC_STONE_SCHEDULE_COUNT.get(), MAGIC_STONE_PERSIST_COUNT.get(), MAGIC_STONE_REFRESH_COUNT.get(), EQUIPMENT_TIMER_COUNT.get(), ERROR_COUNT.get());
        }

        private static /* synthetic */ void lambda$monitoredTask$0(Runnable runnable) {
            ACTIVE_COUNT.incrementAndGet();
            try {
                runnable.run();
            }
            catch (RuntimeException runtimeException) {
                ERROR_COUNT.incrementAndGet();
                throw runtimeException;
            }
            catch (Error error) {
                ERROR_COUNT.incrementAndGet();
                throw error;
            }
            finally {
                COMPLETED_COUNT.incrementAndGet();
                ACTIVE_COUNT.decrementAndGet();
            }
        }

        @Override
        protected int getId() {
            return this.nativeObjectId;
        }

        public Item(int n) {
            this(n, NativeDeItemMetadata.empty(n));
        }

        public Item(int n, NativeDeItemTemplate nativeDeItemTemplate, long l) {
            this(n, NativeDeItemMetadata.empty(n), nativeDeItemTemplate, l);
        }

        public class LastStatus {
            public long count;
            public int itemId;
            public boolean isEquipped = false;
            public int enchantLevel;
            public boolean isIdentified = true;
            public int durability;
            public int chargeCount;
            public int remainingTime;
            public Timestamp lastUsed = null;
            public int bless;
            public int attrEnchantKind;
            public int attrEnchantLevel;
            public int greater;

            public void updateAll() {
                this.count = Item.this.getCount();
                this.itemId = Item.this.getItemId();
                this.isEquipped = Item.this.isEquipped();
                this.isIdentified = Item.this.isIdentified();
                this.enchantLevel = Item.this.getEnchantLevel();
                this.durability = Item.this.get_durability();
                this.chargeCount = Item.this.getChargeCount();
                this.remainingTime = Item.this.getRemainingTime();
                this.lastUsed = Item.this.getLastUsed();
                this.bless = Item.this.getBless();
                this.attrEnchantKind = Item.this.getAttrEnchantKind();
                this.attrEnchantLevel = Item.this.getAttrEnchantLevel();
                this.greater = Item.this.getGreater();
            }

            public void updateCount() {
                this.count = Item.this.getCount();
            }

            public void updateItemId() {
                this.itemId = Item.this.getItemId();
            }

            public void updateEquipped() {
                this.isEquipped = Item.this.isEquipped();
            }

            public void updateIdentified() {
                this.isIdentified = Item.this.isIdentified();
            }

            public void updateEnchantLevel() {
                this.enchantLevel = Item.this.getEnchantLevel();
            }

            public void updateDuraility() {
                this.durability = Item.this.get_durability();
            }

            public void updateChargeCount() {
                this.chargeCount = Item.this.getChargeCount();
            }

            public void updateRemainingTime() {
                this.remainingTime = Item.this.getRemainingTime();
            }

            public void updateLastUsed() {
                this.lastUsed = Item.this.getLastUsed();
            }

            public void updateBless() {
                this.bless = Item.this.getBless();
            }

            public void updateAttrEnchantKind() {
                this.attrEnchantKind = Item.this.getAttrEnchantKind();
            }

            public void updateAttrEnchantLevel() {
                this.attrEnchantLevel = Item.this.getAttrEnchantLevel();
            }

            public void updateGreater() {
                this.greater = Item.this.getGreater();
            }
        }

        private static final class EquipmentTimerLease {
            private final long generation;
            private final NativeDeDeletionPorts.Viewer pc;
            private long expiryStartedNanos;
            private ScheduledFuture<?> future;
            private int expirySubmitAttempts;
            private boolean expirySubmissionInFlight;
            private boolean expiryRetryScheduled;

            private EquipmentTimerLease(long l, NativeDeDeletionPorts.Viewer viewer) {
                this.generation = l;
                this.pc = viewer;
            }
        }
    }
}
