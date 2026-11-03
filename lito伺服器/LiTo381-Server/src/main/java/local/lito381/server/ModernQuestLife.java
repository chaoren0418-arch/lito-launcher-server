package local.lito381.server;

import java.util.Objects;

/** Quest death confirmation and spawn resurrection, not Honor's timed in-place
 * revival. Counters survive a resurrection; old requests/receipts cannot claim
 * the next life. All arithmetic is checked before the owning engine commits. */
final class ModernQuestLife {
    record State(long entityId,int lane,ModernWorldMap.Cell spawn,long deaths,long deathRevision,
                 long respawns,long respawnRevision,long hpGain,ModernWorldMap.Cell deathCell) {
        State {
            Objects.requireNonNull(spawn);Objects.requireNonNull(deathCell);
            if(entityId<1||entityId>100||lane<0||lane>1||deaths<0||respawns<0||respawns==Long.MAX_VALUE||respawns>deaths||deaths-respawns>1
                    ||deathRevision<0||respawnRevision<0||hpGain<0
                    ||(deaths==0)!=(deathRevision==0)||(respawns==0)!=(respawnRevision==0)||(respawns==0)!=(hpGain==0)
                    ||deaths==0&&!deathCell.equals(new ModernWorldMap.Cell(0,0))
                    ||deaths>0&&(deaths==respawns?respawnRevision<=deathRevision:deathRevision<=respawnRevision))throw bad();
        }
        static State initial(long entity,int lane,ModernWorldMap.Cell spawn){return new State(entity,lane,spawn,0,0,0,0,0,new ModernWorldMap.Cell(0,0));}
        long incarnation(){return Math.incrementExact(respawns);}
        boolean dead(){return deaths>respawns;}
        State died(long revision,ModernWorldMap.Cell cell){
            if(dead()||revision<=Math.max(deathRevision,respawnRevision))throw bad();
            return new State(entityId,lane,spawn,Math.incrementExact(deaths),revision,respawns,respawnRevision,hpGain,cell);
        }
        State revived(long revision,long maximumHp){
            if(!dead()||revision<=deathRevision||maximumHp<1)throw bad();
            return new State(entityId,lane,spawn,deaths,deathRevision,Math.incrementExact(respawns),revision,Math.addExact(hpGain,maximumHp),deathCell);
        }
        State wire(){return new State(entityId,lane,spawn,deaths,wireRevision(deathRevision),respawns,wireRevision(respawnRevision),hpGain,deathCell);}
        void validate(long revision,ModernWorldMap.Manifest manifest,ModernBattleEngine.Actor actor,ModernCombatProtocol.Vital vital){
            if(actor.entityId()!=entityId||actor.member().team()!=lane||vital.entityId()!=entityId||dead()!=(vital.hp()==0)||deathRevision!=vital.deathRevision()
                    ||deathRevision>revision||respawnRevision>revision||!ModernWorldProtocol.contains(manifest,spawn)
                    ||deaths>0&&!ModernWorldProtocol.contains(manifest,deathCell)
                    ||respawnRevision==revision&&(vital.hp()!=vital.maxHp()||!actor.position().equals(spawn))
                    ||deathRevision==revision&&!actor.position().equals(deathCell)
                    ||!java.math.BigInteger.valueOf(vital.maxHp()).multiply(java.math.BigInteger.valueOf(respawns)).equals(java.math.BigInteger.valueOf(hpGain)))throw bad();
        }
    }
    enum Reason { REVIVED, NOT_ACTIVE, NOT_DEAD, STALE_LIFE, SPAWN_BLOCKED, NUMERIC }
    record Result(Reason reason,ModernBattleObservation.Capture observation) implements ModernBattleCommand {
        Result{Objects.requireNonNull(reason);Objects.requireNonNull(observation);}
        ModernNpcBattle.Frame frame(){return ModernLegacyBattleProjection.npcs(observation);}
    }
    private static long wireRevision(long value){return value==0?0:Math.incrementExact(value);}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid Quest participant life");}
    private ModernQuestLife(){}
}
