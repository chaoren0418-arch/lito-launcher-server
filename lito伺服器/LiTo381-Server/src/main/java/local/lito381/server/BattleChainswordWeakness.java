package local.lito381.server;

/**
 * Dragon Knight weakness lease. Loaded native combat follows current source rules;
 * isolated legacy fixtures retain their original contract below.
 *
 * <p>The isolated legacy fallback uses a 16-second, target-bound,
 * three-level state.  A successful chainsword attack uses one 0..99 roll: level 0
 * can expose level 1 at 20%; levels 1/2 retain their current level below 20
 * or advance below 40; level 3 refreshes only below 20.  The successful
 * exposure damage is +3/+6/+9. Foe Slayer consumes +20/+40/+60. The selected
 * LiTo.bin reaches skill 187 through its generic post-175 selector producer,
 * but it has no legacy subtype-75 icon receiver, so the server intentionally
 * omits only that incompatible exposure-icon packet.
 */
final class BattleChainswordWeakness {
    static final int DRAGON_KNIGHT_TYPE = 5;
    static final int WEAPON_TYPE = 18;
    static final int PROC_CHANCE = 20;
    static final long DURATION_NANOS = 16_000_000_000L;

    private int level;
    private long targetObjectId;
    private long expiresAtNanos;

    synchronized AttackOutcome attack(
            int characterType, int weaponType, long target, long nowNanos,
            int chanceRoll) {
        if (target <= 0 || nowNanos < 0 || chanceRoll < 0 || chanceRoll >= 100) {
            throw new IllegalArgumentException("invalid chainsword weakness attack");
        }
        if(CombatRules.loaded())return sourceAttack(characterType,weaponType,target,nowNanos,chanceRoll);
        Expiry expiry = expire(nowNanos);
        int beforeLevel = level;
        boolean targetReset = level != 0 && targetObjectId != target;
        if (targetReset) clearInternal();
        boolean eligible = characterType == DRAGON_KNIGHT_TYPE
                && weaponType == WEAPON_TYPE;
        if (!eligible) {
            return new AttackOutcome(false, chanceRoll, beforeLevel, level, 0,
                    false, targetReset, expiry.expired(), targetObjectId,
                    remainingNanos(nowNanos));
        }

        int nextLevel = level;
        if (level == 0) {
            if (chanceRoll < PROC_CHANCE) nextLevel = 1;
        } else if (level == 1) {
            if (chanceRoll < PROC_CHANCE) nextLevel = 1;
            else if (chanceRoll < PROC_CHANCE * 2) nextLevel = 2;
        } else if (level == 2) {
            if (chanceRoll < PROC_CHANCE) nextLevel = 2;
            else if (chanceRoll < PROC_CHANCE * 2) nextLevel = 3;
        } else if (chanceRoll < PROC_CHANCE) {
            nextLevel = 3;
        }

        boolean triggered = nextLevel != 0
                && (level == 0 || chanceRoll < PROC_CHANCE * (level < 3 ? 2 : 1));
        int damageBonus = 0;
        if (triggered) {
            level = nextLevel;
            targetObjectId = target;
            expiresAtNanos = Math.addExact(nowNanos, DURATION_NANOS);
            damageBonus = level * 3;
        }
        return new AttackOutcome(true, chanceRoll, beforeLevel, level,
                damageBonus, triggered, targetReset, expiry.expired(),
                targetObjectId, remainingNanos(nowNanos));
    }

    synchronized ConsumeOutcome consumeFoeSlayer(
            int characterType, long target, long nowNanos) {
        if (target <= 0 || nowNanos < 0) {
            throw new IllegalArgumentException("invalid Foe Slayer target/time");
        }
        // Source consumes only inside the first successful ordinary attack.
        // A cast itself neither expires the lease nor binds it to its old target.
        if(CombatRules.loaded())return new ConsumeOutcome(characterType==DRAGON_KNIGHT_TYPE?level:0,
                0,false,false,false);
        Expiry expiry = expire(nowNanos);
        int consumedLevel = level;
        boolean targetMismatch = level != 0 && targetObjectId != target;
        int damageBonus = characterType == DRAGON_KNIGHT_TYPE
                && !targetMismatch ? consumedLevel * 20 : 0;
        boolean consumed = damageBonus != 0;
        if (level != 0 && (consumed || targetMismatch)) clearInternal();
        return new ConsumeOutcome(consumedLevel, damageBonus, consumed,
                targetMismatch, expiry.expired());
    }

    synchronized Snapshot snapshot(long nowNanos) {
        if (nowNanos < 0) throw new IllegalArgumentException("invalid weakness time");
        if(!CombatRules.loaded())expire(nowNanos);
        return new Snapshot(level, targetObjectId, remainingNanos(nowNanos));
    }

    synchronized void clear() {
        clearInternal();
    }

    private AttackOutcome sourceAttack(int type,int weapon,long target,long now,int roll) {
        var expiry=expire(now);int before=level;
        if(type!=DRAGON_KNIGHT_TYPE||weapon!=WEAPON_TYPE)
            return new AttackOutcome(false,roll,before,level,0,false,false,expiry.expired(),targetObjectId,remainingNanos(now));
        int threshold=(int)CombatRules.number("VULNERABILITY_ROM",20);
        boolean triggered=level==1||level==2||roll<threshold;
        int damage=0;
        if(triggered) {
            if(level==0)level=1;
            else if(level<3&&roll>=threshold)level++;
            targetObjectId=target; // last-hit metadata; no target binding in source
            expiresAtNanos=CombatDragon.weaknessDeadline(now);
            String key=switch(level){case 1->"VULNERABILITY_1";case 2->"VULNERABILITY_2";case 3->"VULNERABILITY_3";default->throw new AssertionError("weakness phase");};
            damage=(int)CombatRules.number(key,level*3);
        }
        return new AttackOutcome(true,roll,before,level,damage,triggered,false,expiry.expired(),targetObjectId,remainingNanos(now));
    }
    synchronized void sourceNormalAttack(long now) {if(CombatRules.loaded())expire(now);}
    synchronized int sourceConsumeOnHit() {
        int consumed=level;clearInternal();return consumed;
    }

    private Expiry expire(long nowNanos) {
        boolean expired = level != 0 && nowNanos >= expiresAtNanos;
        if (expired) clearInternal();
        return new Expiry(expired);
    }

    private long remainingNanos(long nowNanos) {
        return level == 0 ? 0 : Math.max(0, expiresAtNanos - nowNanos);
    }

    private void clearInternal() {
        level = 0;
        targetObjectId = 0;
        expiresAtNanos = 0;
    }

    record AttackOutcome(
            boolean eligible, int chanceRoll, int beforeLevel, int afterLevel,
            int damageBonus, boolean triggered, boolean targetReset,
            boolean expired, long targetObjectId, long remainingNanos) {
        static AttackOutcome none() {
            return new AttackOutcome(false, 0, 0, 0, 0,
                    false, false, false, 0, 0);
        }
    }

    record ConsumeOutcome(
            int consumedLevel, int damageBonus, boolean consumed,
            boolean targetMismatch, boolean expired) {}

    record Snapshot(int level, long targetObjectId, long remainingNanos) {}

    private record Expiry(boolean expired) {}
}
