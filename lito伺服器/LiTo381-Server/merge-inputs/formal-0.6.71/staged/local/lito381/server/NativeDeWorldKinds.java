// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

import local.lito381.server.NativeDeDeletionPorts.ObjectRef;
import local.lito381.server.NativeDeDeletionPorts.Viewer;
import local.lito381.server.NativeDeItemAbility.AbilityType;
import local.lito381.server.NativeDeItemMetadata.Logs;

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

        public void setIdentified(boolean var1) {
         this._isIdentified = var1;
         this.invalidateStatusBytesCache();
      }

        public String getName() {
         return this._item.getNameId();
      }

        public long getCount() {
         return this._count;
      }

        public void setCount(long var1) {
         this._count = var1;
         this._countMutationVersion.incrementAndGet();
      }

        public long getCountMutationVersion() {
         return this._countMutationVersion.get();
      }

        public String getGamNo() {
         return this._gamno;
      }

        public void setGamNo(String var1) {
         this._gamno = var1;
      }

        public String getLotto() {
         return this._lotto;
      }

        public void setLotto(String var1) {
         this._lotto = var1;
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

        public void setItem(NativeDeItemTemplate var1) {
         this._item = var1;
         this._itemId = var1.getItemId();
         if (var1.getGreaterTier() > 0) {
            this.setGreater(var1.getGreaterTier());
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
         } finally {
            this._abilityLock.unlock();
         }
      }

        private boolean applyMagicStoneExpiryIfNeeded() {
         if (this._magicStoneItemId <= 0) {
            return false;
         } else if (this._magicStoneTime != null && System.currentTimeMillis() >= this._magicStoneTime.getTime()) {
            this.clearMagicStoneFieldsOnly();
            this._pendingMagicStoneExpiryGeneration = ++this._magicStoneGeneration;
            this._magicStoneExpiryStartedNanos = System.nanoTime();
            this._magicStoneExpirySubmitAttempts = 0;
            this._magicStoneExpirySubmissionInFlight = false;
            this._magicStoneExpiryRetryScheduled = false;
            this._magicStoneExpiryExhausted = false;
            return true;
         } else {
            return false;
         }
      }

        private void clearMagicStoneFieldsOnly() {
         this._magicStoneItemId = 0;
         this._magicStoneLevel = 0;
         this._magicStoneTime = null;
         this.cancelMagicStoneExpiryTimer();
      }

        private void beginMagicStoneMutationLocked() {
         this._magicStoneGeneration++;
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

        private void hydrateAllAbilities(NativeDeItemTemplate var1) {
         this._ability.clear();
         byte var2 = var1.get_addstr();
         if (var2 != 0) {
            this._ability.add(AbilityType.STR, var2);
         }

         byte var3 = var1.get_adddex();
         if (var3 != 0) {
            this._ability.add(AbilityType.DEX, var3);
         }

         byte var4 = var1.get_addcon();
         if (var4 != 0) {
            this._ability.add(AbilityType.CON, var4);
         }

         byte var5 = var1.get_addint();
         if (var5 != 0) {
            this._ability.add(AbilityType.INT, var5);
         }

         byte var6 = var1.get_addwis();
         if (var6 != 0) {
            this._ability.add(AbilityType.WIS, var6);
         }

         byte var7 = var1.get_addcha();
         if (var7 != 0) {
            this._ability.add(AbilityType.CHA, var7);
         }

         int var8 = var1.get_addhp();
         if (var8 != 0) {
            this._ability.add(AbilityType.HP, var8);
         }

         int var9 = var1.get_addmp();
         if (var9 != 0) {
            this._ability.add(AbilityType.MP, var9);
         }

         int var10 = var1.get_addsp();
         if (var10 != 0) {
            this._ability.add(AbilityType.SP, var10);
         }

         if (var1 instanceof NativeDeArmorTemplate var11) {
            int var12 = var11.get_ac();
            if (var12 != 0) {
               this._ability.add(AbilityType.AC, var12);
            }

            int var13 = var11.get_mdef();
            if (var13 != 0) {
               this._ability.add(AbilityType.MR, var13);
            }

            int var14 = var11.get_defense_fire();
            if (var14 != 0) {
               this._ability.add(AbilityType.DEF_FIRE, var14);
            }

            int var15 = var11.get_defense_water();
            if (var15 != 0) {
               this._ability.add(AbilityType.DEF_WATER, var15);
            }

            int var16 = var11.get_defense_earth();
            if (var16 != 0) {
               this._ability.add(AbilityType.DEF_EARTH, var16);
            }

            int var17 = var11.get_defense_wind();
            if (var17 != 0) {
               this._ability.add(AbilityType.DEF_WIND, var17);
            }

            int var18 = var11.get_regist_freeze();
            if (var18 != 0) {
               this._ability.add(AbilityType.REGIST_FREEZE, var18);
            }

            int var19 = var11.get_regist_stone();
            if (var19 != 0) {
               this._ability.add(AbilityType.REGIST_STONE, var19);
            }

            int var20 = var11.get_regist_sleep();
            if (var20 != 0) {
               this._ability.add(AbilityType.REGIST_SLEEP, var20);
            }

            int var21 = var11.get_regist_blind();
            if (var21 != 0) {
               this._ability.add(AbilityType.REGIST_BLIND, var21);
            }

            int var22 = var11.get_regist_stun();
            if (var22 != 0) {
               this._ability.add(AbilityType.REGIST_STUN, var22);
            }

            int var23 = var11.get_regist_sustain();
            if (var23 != 0) {
               this._ability.add(AbilityType.REGIST_SUSTAIN, var23);
            }
         }

         if (var1 instanceof NativeDeWeaponTemplate var32) {
            this._ability.set(AbilityType.BASE_DMG_SMALL, var32.getDmgSmall());
            this._ability.set(AbilityType.BASE_DMG_LARGE, var32.getDmgLarge());
         }

         if (var1 instanceof NativeDeWeaponTemplate var33) {
            int var36 = var33.getHitModifier();
            int var39 = var33.getDmgModifier();
            int var42 = var33.getAddHitup();
            int var45 = var33.getAddDmgup();
            int var48 = var33.getAddBowHitup();
            int var51 = var33.getAddBowDmgup();
            if (var36 != 0 || var42 != 0) {
               this._ability.add(AbilityType.HIT, var36 + var42);
            }

            if (var39 != 0 || var45 != 0) {
               this._ability.add(AbilityType.DMG, var39 + var45);
            }

            if (var36 != 0 || var48 != 0) {
               this._ability.add(AbilityType.BOW_HIT, var36 + var48);
            }

            if (var39 != 0 || var51 != 0) {
               this._ability.add(AbilityType.BOW_DMG, var39 + var51);
            }
         } else if (var1 instanceof NativeDeArmorTemplate var34) {
            int var37 = var34.getHitModifierByArmor();
            int var40 = var34.getDmgModifierByArmor();
            int var43 = var34.getAddHitup();
            int var46 = var34.getAddDmgup();
            if (var37 != 0 || var43 != 0) {
               this._ability.add(AbilityType.HIT, var37 + var43);
            }

            if (var40 != 0 || var46 != 0) {
               this._ability.add(AbilityType.DMG, var40 + var46);
            }

            int var49 = var34.getBowHitModifierByArmor() + var34.getAddBowHitup();
            if (var49 != 0) {
               this._ability.add(AbilityType.BOW_HIT, var49);
            }

            int var52 = var34.getBowDmgModifierByArmor() + var34.getAddBowDmgup();
            if (var52 != 0) {
               this._ability.add(AbilityType.BOW_DMG, var52);
            }
         }

         int var35 = var1.getAddStunSuccess();
         if (var35 != 0) {
            this._ability.add(AbilityType.STUN_SUCCESS, var35);
         }

         int var38 = var1.getMagicDmgModifier();
         if (var38 != 0) {
            this._ability.add(AbilityType.MAGIC_DMG_MODIFIER, var38);
         }

         int var41 = var1.getAddPotionRecovery();
         if (var41 != 0) {
            this._ability.add(AbilityType.POTION_RECOVERY, var41);
         }

         int var44 = var1.getAddPoisonResist();
         if (var44 != 0) {
            this._ability.add(AbilityType.POISON_RESIST, var44);
         }

         int var47 = var1.getAddExpRate();
         if (var47 != 0) {
            this._ability.add(AbilityType.EXP_RATE, var47);
         }

         int var50 = var1.getAddPierce();
         if (var50 != 0) {
            this._ability.add(AbilityType.PIERCE, var50);
         }

         int var53 = var1.getMagicBow();
         if (var53 != 0) {
            this._ability.add(AbilityType.MAGIC_BOW, var53);
         }

         int var54 = var1.getArrowSpr();
         this._ability.add(AbilityType.ARROW_SPR, var54);
         int var55 = var1.getAddMagicHit();
         if (var55 != 0) {
            this._ability.add(AbilityType.MAGIC_HIT, var55);
         }

         if (var1 instanceof NativeDeWeaponTemplate var56) {
            int var58 = var56.getAddIgnoreReduction();
            if (var58 != 0) {
               this._ability.set(AbilityType.IGNORE_REDUCTION, var58);
            }

            int var60 = var56.getAddPvpDmg();
            if (var60 != 0) {
               this._ability.set(AbilityType.PVP_DMG, var60);
            }

            int var62 = var56.getAddPvpRedux();
            if (var62 != 0) {
               this._ability.set(AbilityType.PVP_REDUX, var62);
            }

            int var24 = var56.getAddBossDmg();
            if (var24 != 0) {
               this._ability.set(AbilityType.DMG_BOSS, var24);
            }

            int var25 = var56.getAddBossRedux();
            if (var25 != 0) {
               this._ability.set(AbilityType.BOSS_REDUX, var25);
            }

            int var26 = var56.getAddDodge();
            if (var26 != 0) {
               this._ability.set(AbilityType.DODGE, var26);
            }

            int var27 = var56.getAddEr();
            if (var27 != 0) {
               this._ability.set(AbilityType.ER, var27);
            }

            int var28 = var56.getAddAdenaRate();
            if (var28 != 0) {
               this._ability.set(AbilityType.ADENA_RATE, var28);
            }

            int var29 = var56.getAddDropRate();
            if (var29 != 0) {
               this._ability.set(AbilityType.DROP_RATE, var29);
            }
         } else if (var1 instanceof NativeDeArmorTemplate var57) {
            int var59 = var57.getAddIgnoreReduction();
            if (var59 != 0) {
               this._ability.set(AbilityType.IGNORE_REDUCTION, var59);
            }

            int var61 = var57.getAddPvpRedux();
            if (var61 != 0) {
               this._ability.set(AbilityType.PVP_REDUX, var61);
            }

            int var63 = var57.getAddPvpDmg();
            if (var63 != 0) {
               this._ability.set(AbilityType.PVP_DMG, var63);
            }

            int var64 = var57.getAddBossDmg();
            if (var64 != 0) {
               this._ability.set(AbilityType.DMG_BOSS, var64);
            }

            int var65 = var57.getAddBossRedux();
            if (var65 != 0) {
               this._ability.set(AbilityType.BOSS_REDUX, var65);
            }

            int var66 = var1.getAddMeleeRedux();
            if (var66 != 0) {
               this._ability.add(AbilityType.MELEE_REDUX, var66);
            }

            int var67 = var1.getAddBowRedux();
            if (var67 != 0) {
               this._ability.add(AbilityType.BOW_REDUX, var67);
            }

            int var68 = var57.getAddDodge();
            if (var68 != 0) {
               this._ability.set(AbilityType.DODGE, var68);
            }

            int var69 = var57.getAddEr();
            if (var69 != 0) {
               this._ability.set(AbilityType.ER, var69);
            }

            int var30 = var57.getAddAdenaRate();
            if (var30 != 0) {
               this._ability.set(AbilityType.ADENA_RATE, var30);
            }

            int var31 = var57.getAddDropRate();
            if (var31 != 0) {
               this._ability.set(AbilityType.DROP_RATE, var31);
            }
         }
      }

        public int getItemId() {
         return this._itemId;
      }

        public void setItemId(int var1) {
         this._itemId = var1;
      }

        public void setDropSourceName(String var1) {
         this._dropSourceName = var1;
      }

        public String getDropSourceName() {
         return this._dropSourceName;
      }

        public NativeDeDarkItem getDarkItem() {
         return this._darkItem;
      }

        public void setDarkItem(NativeDeDarkItem var1) {
         this._darkItem = var1;
         this.markAbilityDirty();
      }

        public void loadDarkItem() {
         if (this.getId() > 0) {
            NativeDeItemMetadata var1 = this.nativeMetadata;
            this._darkItem = var1.getDarkItem(this.getId());
            if (this._darkItem != null) {
               this.markAbilityDirty();
            }
         }
      }

        public boolean isStackable() {
         return this._item.isStackable();
      }

        @Override
      public void onAction(Viewer var1) {
      }

        public int getEnchantLevel() {
         return this._enchantLevel;
      }

        public void setEnchantLevel(int var1) {
         this._enchantLevel = var1;
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

        public void setChargeCount(int var1) {
         this._chargeCount = var1;
         this.invalidateStatusBytesCache();
      }

        public int getRemainingTime() {
         return this._remainingTime.get();
      }

        public void setRemainingTime(int var1) {
         this._remainingTime.set(var1);
         this.invalidateStatusBytesCache();
      }

        private boolean compareAndSetRemainingTime(int var1, int var2) {
         if (!this._remainingTime.compareAndSet(var1, var2)) {
            return false;
         } else {
            this.invalidateStatusBytesCache();
            return true;
         }
      }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void setLastUsed(Timestamp var1) {
         synchronized (this._delayEffectStateLock) {
            this._lastUsed = var1;
         }
      }

        public Timestamp getLastUsed() {
         return this._lastUsed;
      }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public long getDelayEffectRuntimeRemainingMillis() {
         synchronized (this._delayEffectStateLock) {
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
        public void reserveDelayEffectRuntime(int var1) {
         synchronized (this._delayEffectStateLock) {
            this.reserveDelayEffectRuntimeAt(var1, System.nanoTime());
         }
      }

        void reserveDelayEffectRuntimeAt(int var1, long var2) {
         long var4 = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, (long)var1));
         this._delayEffectRuntimeDeadlineNanos.set(var2 + var4);
      }

        public long getDelayEffectRemainingMillis(int var1, long var2) {
         return this.getDelayEffectRemainingMillisAt(var1, var2, System.nanoTime());
      }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        long getDelayEffectRemainingMillisAt(int var1, long var2, long var4) {
         synchronized (this._delayEffectStateLock) {
            long var7 = 0L;
            Timestamp var9 = this._lastUsed;
            if (var9 != null && var1 > 0) {
               long var10 = var2 - var9.getTime();
               if (var10 < var1) {
                  long var12 = var1 - var10;
                  var7 = var12 > 0L ? var12 : Long.MAX_VALUE;
               }
            }

            return Math.max(var7, this.getDelayEffectRuntimeRemainingMillisAt(var4));
         }
      }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        Timestamp markDelayEffectUsedAt(int var1, long var2, long var4) {
         synchronized (this._delayEffectStateLock) {
            this.reserveDelayEffectRuntimeAt(var1, var4);
            Timestamp var7 = new Timestamp(var2);
            this._lastUsed = var7;
            return var7;
         }
      }

        public int getLastWeight() {
         return this._lastWeight;
      }

        public void setLastWeight(int var1) {
         this._lastWeight = var1;
      }

        public void setBless(int var1) {
         this._bless = var1;
         this.markAbilityDirty();
      }

        public int getBless() {
         return this._bless;
      }

        public boolean isSealed() {
         return this._isSealed;
      }

        public void setSealed(boolean var1) {
         this._isSealed = var1;
      }

        public void setAttrEnchantKind(int var1) {
         this._attrEnchantKind = var1;
         this.markAbilityDirty();
      }

        public int getAttrEnchantKind() {
         return this._attrEnchantKind;
      }

        public void setAttrEnchantLevel(int var1) {
         this._attrEnchantLevel = var1;
         this.markAbilityDirty();
      }

        public int getAttrEnchantLevel() {
         return this._attrEnchantLevel;
      }

        public int getGreater() {
         return this._greater;
      }

        public void setGreater(int var1) {
         this._greater = var1;
         this.markAbilityDirty();
      }

        public int getEnchantSaveMode() {
         return this._enchantSaveMode;
      }

        public void setEnchantSaveMode(int var1) {
         this._enchantSaveMode = var1;
      }

        public int getUpdateSaveMode() {
         return this._updateSaveMode;
      }

        public void setUpdateSaveMode(int var1) {
         this._updateSaveMode = var1;
      }

        public void set_durability(int var1) {
         if (var1 < 0) {
            var1 = 0;
         }

         if (var1 > 127) {
            var1 = 127;
         }

         if (this._durability != var1) {
            this._durability = var1;
            this.markAbilityDirty();
         }
      }

        public int getWeight() {
         int var1 = this.getItem().getWeight();
         if (var1 < 0 || this.getCount() <= 0L) {
            return Integer.MAX_VALUE;
         } else if (var1 == 0) {
            return 0;
         } else {
            try {
               long var2 = Math.max(Math.multiplyExact(this.getCount(), (long)var1) / 1000L, 1L);
               return var2 >= 2147483647L ? Integer.MAX_VALUE : (int)var2;
            } catch (ArithmeticException var4) {
               return Integer.MAX_VALUE;
            }
         }
      }

        public NativeDeWorldKinds.Item.LastStatus getLastStatus() {
         return this._lastStatus;
      }

        public int getRecordingColumns() {
         int var1 = 0;
         if (this.getCount() != this._lastStatus.count) {
            var1 += 16;
         }

         if (this.getItemId() != this._lastStatus.itemId) {
            var1 += 64;
         }

         if (this.isEquipped() != this._lastStatus.isEquipped) {
            var1 += 8;
         }

         if (this.getEnchantLevel() != this._lastStatus.enchantLevel) {
            var1 += 4;
         }

         if (this.get_durability() != this._lastStatus.durability) {
            var1++;
         }

         if (this.getChargeCount() != this._lastStatus.chargeCount) {
            var1 += 128;
         }

         if (this.getLastUsed() != this._lastStatus.lastUsed) {
            var1 += 32;
         }

         if (this.isIdentified() != this._lastStatus.isIdentified) {
            var1 += 2;
         }

         if (this.getRemainingTime() != this._lastStatus.remainingTime) {
            var1 += 256;
         }

         if (this.getBless() != this._lastStatus.bless) {
            var1 += 512;
         }

         if (this.getAttrEnchantKind() != this._lastStatus.attrEnchantKind) {
            var1 += 1024;
         }

         if (this.getAttrEnchantLevel() != this._lastStatus.attrEnchantLevel) {
            var1 += 2048;
         }

         if (this.getGreater() != this._lastStatus.greater) {
            var1 += 4096;
         }

         return var1;
      }

        public NativeDeItemAbility getAbility() {
         return this._ability;
      }

        public NativeDeItemVipTemplate getAppliedVipTemplate() {
         return this._appliedVipTemplate;
      }

        public void setAppliedVipTemplate(NativeDeItemVipTemplate var1) {
         this._appliedVipTemplate = var1;
      }

        public int[] getInlayCards() {
         return this._inlayCards != null ? (int[])this._inlayCards.clone() : new int[5];
      }

        public void setInlayCards(int[] var1) {
         if (var1 != null && var1.length >= 5) {
            this._inlayCards = (int[])var1.clone();
            this.markAbilityDirty();
         }
      }

        public int getOpenSlotCount() {
         if (this._inlayCards == null) {
            return 0;
         } else {
            int var1 = 0;

            for (int var5 : this._inlayCards) {
               if (var5 >= 1) {
                  var1++;
               }
            }

            return var1;
         }
      }

        public int getFirstClosedSlotIndex() {
         if (this._inlayCards == null) {
            return -1;
         } else {
            for (int var1 = 0; var1 < this._inlayCards.length; var1++) {
               if (this._inlayCards[var1] == 0) {
                  return var1;
               }
            }

            return -1;
         }
      }

        public int getFirstEmptySlotIndex() {
         if (this._inlayCards == null) {
            return -1;
         } else {
            for (int var1 = 0; var1 < this._inlayCards.length; var1++) {
               if (this._inlayCards[var1] == 1) {
                  return var1;
               }
            }

            return -1;
         }
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
         } finally {
            this._abilityLock.unlock();
         }
      }

        private byte[] materializeStatusBytes(byte[] var1, int var2) {
         if (var2 < 0) {
            return var1;
         } else {
            int var3 = this.getWeight();
            byte[] var4 = (byte[])var1.clone();
            var4[var2] = (byte)(var3 & 0xFF);
            var4[var2 + 1] = (byte)(var3 >> 8 & 0xFF);
            var4[var2 + 2] = (byte)(var3 >> 16 & 0xFF);
            var4[var2 + 3] = (byte)(var3 >> 24 & 0xFF);
            return var4;
         }
      }

        public int getMr() {
         return this._ability.get(AbilityType.MR);
      }

        public int getItemOwnerId() {
         return this._itemOwnerId;
      }

        public void setItemOwnerId(int var1) {
         this._itemOwnerId = var1;
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
         this._equipmentTimerGeneration++;
         NativeDeWorldKinds.Item.EquipmentTimerLease var1 = this._activeEquipmentTimer;
         this._activeEquipmentTimer = null;
         this._equipmentTimerFuture = null;
         if (var1 != null && var1.future != null) {
            var1.future.cancel(false);
         }
      }

        private void stopEquipmentTimerSerialized() {
         this._equipmentTimerLock.lock();

         try {
            this.cancelActiveEquipmentTimerLocked();
         } finally {
            this._equipmentTimerLock.unlock();
         }
      }

        public boolean isNowLighting() {
         return this._isNowLighting;
      }

        public void setNowLighting(boolean var1) {
         this._isNowLighting = var1;
      }

        public boolean isEquippedTemp() {
         return this._isEquippedTemp;
      }

        public void set_isEquippedTemp(boolean var1) {
         this._isEquippedTemp = var1;
      }

        public void setIsMatch(boolean var1) {
         this._isMatch = var1;
         this.markAbilityDirty();
      }

        public boolean isMatch() {
         return this._isMatch;
      }

        public void set_char_objid(int var1) {
         this._char_objid = var1;
      }

        public int get_char_objid() {
         return this._char_objid;
      }

        public void set_time(Timestamp var1) {
         this._time = var1;
      }

        public Timestamp get_time() {
         return this._time;
      }

        public final int getEquipWindow() {
         return this._equipWindow;
      }

        public final void setEquipWindow(int var1) {
         this._equipWindow = var1;
      }

        public List<Integer> getAwakenPowerIds() {
         return new ArrayList<>(this._awakenPowerIds);
      }

        public void setAwakenPowerIds(List<Integer> var1) {
         this._awakenPowerIds.clear();
         if (var1 != null) {
            for (Integer var3 : var1) {
               if (var3 != null && var3 > 0) {
                  this._awakenPowerIds.add(var3);
               }
            }
         }
      }

        public void addAwakenPowerId(int var1) {
         if (var1 > 0 && !this._awakenPowerIds.contains(var1)) {
            this._awakenPowerIds.add(var1);
         }
      }

        public void clearAwakenPowerIds() {
         this._awakenPowerIds.clear();
      }

        public static String getMonitorLine() {
         return String.format(
            "L1ItemInstance scheduled=%d submit=%d active=%d done=%d skip=%d rejected=%d magicStoneExpire=%d magicStonePersist=%d magicStoneRefresh=%d equipmentTimer=%d error=%d",
            SCHEDULED_COUNT.get(),
            SUBMITTED_COUNT.get(),
            ACTIVE_COUNT.get(),
            COMPLETED_COUNT.get(),
            SKIP_COUNT.get(),
            REJECTED_COUNT.get(),
            MAGIC_STONE_SCHEDULE_COUNT.get(),
            MAGIC_STONE_PERSIST_COUNT.get(),
            MAGIC_STONE_REFRESH_COUNT.get(),
            EQUIPMENT_TIMER_COUNT.get(),
            ERROR_COUNT.get()
         );
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

