package local.lito381.server;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/** NEW_MATCH_HONOR_V1. Objectives are world objects, never room members or SQL characters. */
final class ModernHonor {
    static final String RULESET="NEW_MATCH_HONOR_V1";
    static final long RESPAWN_MILLIS=5_000;
    record Tower(int team,ModernWorldMap.Cell cell,int graphic,long maximumHp) {
        Tower {
            Objects.requireNonNull(cell);
            if(team<0||team>1||!List.of(2665,2667,2669,2671).contains(graphic)||maximumHp<1)throw bad();
        }
    }
    record Config(List<Tower> towers,List<ModernWorldMap.Cell> teamSpawns) {
        Config(List<Tower> towers){this(towers,List.of());}
        Config {
            towers=List.copyOf(towers);
            teamSpawns=List.copyOf(teamSpawns);
            if(!teamSpawns.isEmpty()&&(teamSpawns.size()!=2||teamSpawns.get(0).equals(teamSpawns.get(1))))throw bad();
            if(towers.size()!=2||towers.get(0).team()!=0||towers.get(1).team()!=1
                    ||towers.get(0).cell().equals(towers.get(1).cell()))throw bad();
        }
        void validate(ModernWorldBinding binding) throws ModernWorldMap.MapException {
            if(binding.key().mode()!=5)throw bad();
            binding.map().validateSpawns(towers.stream().map(Tower::cell).toList());
            if(!teamSpawns.isEmpty()) {
                var all=new java.util.ArrayList<>(towers.stream().map(Tower::cell).toList());all.addAll(teamSpawns);binding.map().validateSpawns(all);
            }
        }
        List<ModernWorldMap.Cell> playerSpawns(ModernWorldMap map,List<ModernRooms.Member> roster,ModernWorldMap.Cell fallback)throws ModernWorldMap.MapException {
            var excluded=new java.util.HashSet<>(towers.stream().map(Tower::cell).toList());
            if(teamSpawns.isEmpty())return map.serverChosenSpawns(fallback,roster.size(),excluded);
            var result=new ModernWorldMap.Cell[roster.size()];
            for(int team=0;team<2;team++) {
                int chosenTeam=team;int count=(int)roster.stream().filter(m->m.team()==chosenTeam).count();if(count==0)throw bad();
                var cells=map.serverChosenSpawns(teamSpawns.get(team),count,excluded);excluded.addAll(cells);int next=0;
                for(int i=0;i<roster.size();i++)if(roster.get(i).team()==team)result[i]=cells.get(next++);
            }
            return List.of(result);
        }
    }
    /** Interaction revision includes a successful full-HP heal; hurt is positive damage only. */
    record Objective(long entityId,int team,int action,int graphic,int x,int y,long hp,long maximumHp,
            long revision,long hurtRevision,long deathRevision,long damageLoss,long healGain,long lastActorEntityId,int lastKind) {
        Objective {
            if(entityId<3||team<0||team>1||!List.of(2665,2667,2669,2671).contains(graphic)
                    ||maximumHp<1||hp<0||hp>maximumHp||revision<0||hurtRevision<0||hurtRevision>revision
                    ||deathRevision<0||deathRevision>revision||damageLoss<0||healGain<0||lastActorEntityId<0
                    ||lastKind<0||lastKind>4||action!=ModernHonor.action(hp,maximumHp)
                    ||(hp==0)!=(deathRevision>0)||deathRevision>0&&deathRevision!=hurtRevision
                    ||(revision==0)!=(lastKind==0)||(revision==0)!=(lastActorEntityId==0)
                    ||revision==0&&(hp!=maximumHp||hurtRevision!=0||damageLoss!=0||healGain!=0)
                    ||!BigInteger.valueOf(maximumHp).add(BigInteger.valueOf(healGain)).subtract(BigInteger.valueOf(damageLoss)).equals(BigInteger.valueOf(hp)))throw bad();
        }
        static Objective initial(int participants,Tower tower){return new Objective(participants+tower.team()+1,tower.team(),0,tower.graphic(),tower.cell().x(),tower.cell().y(),tower.maximumHp(),tower.maximumHp(),0,0,0,0,0,0,0);}
        ModernWorldMap.Cell cell(){return new ModernWorldMap.Cell(x,y);}
        Objective damaged(long amount,long actor,int kind,long next) {
            if(hp==0||amount<0||amount>hp||actor<1||kind<1||kind>4||kind==3||next<=revision)throw bad();
            long after=Math.subtractExact(hp,amount);
            return new Objective(entityId,team,ModernHonor.action(after,maximumHp),graphic,x,y,after,maximumHp,next,
                    amount==0?hurtRevision:next,after==0?next:0,Math.addExact(damageLoss,amount),healGain,actor,kind);
        }
        Objective healed(long amount,long actor,long next) {
            if(hp==0||amount<0||amount>maximumHp-hp||actor<1||next<=revision)throw bad();
            long after=Math.addExact(hp,amount);
            return new Objective(entityId,team,ModernHonor.action(after,maximumHp),graphic,x,y,after,maximumHp,next,hurtRevision,0,damageLoss,Math.addExact(healGain,amount),actor,3);
        }
        Objective wireRevision(){return new Objective(entityId,team,action,graphic,x,y,hp,maximumHp,wire(revision),wire(hurtRevision),wire(deathRevision),damageLoss,healGain,lastActorEntityId,lastKind);}
    }
    record Life(long entityId,long deathCount,long lastDeathRevision,long respawnCount,long respawnRevision,
            long respawnHpGain,long pendingDueMillis,int deathX,int deathY) {
        Life {
            if(entityId<1||deathCount<0||respawnCount<0||respawnCount>deathCount||deathCount-respawnCount>1
                    ||lastDeathRevision<0||respawnRevision<0||respawnHpGain<0||pendingDueMillis<0
                    ||pendingDueMillis>1_805_000||(deathCount==0)!=(lastDeathRevision==0)
                    ||(respawnCount==0)!=(respawnRevision==0)||(respawnCount==0)!=(respawnHpGain==0)
                    ||deathCount==0&&(deathX!=0||deathY!=0||pendingDueMillis!=0)
                    ||deathCount==respawnCount&&(pendingDueMillis!=0||deathCount>0&&respawnRevision<=lastDeathRevision)
                    ||deathCount>respawnCount&&lastDeathRevision<=respawnRevision)throw bad();
        }
        static Life initial(long entity){return new Life(entity,0,0,0,0,0,0,0,0);}
        Life died(long revision,long elapsedMillis,ModernWorldMap.Cell cell){
            if(deathCount!=respawnCount||revision<=Math.max(lastDeathRevision,respawnRevision))throw bad();
            return new Life(entityId,Math.incrementExact(deathCount),revision,respawnCount,respawnRevision,respawnHpGain,Math.addExact(elapsedMillis,RESPAWN_MILLIS),cell.x(),cell.y());
        }
        Life respawn(long revision,long maximumHp){
            if(deathCount!=respawnCount+1||revision<=lastDeathRevision||maximumHp<1)throw bad();
            return new Life(entityId,deathCount,lastDeathRevision,Math.incrementExact(respawnCount),revision,Math.addExact(respawnHpGain,maximumHp),0,deathX,deathY);
        }
        Life terminal(){return new Life(entityId,deathCount,lastDeathRevision,respawnCount,respawnRevision,respawnHpGain,0,deathX,deathY);}
        Life wireRevision(){return new Life(entityId,deathCount,wire(lastDeathRevision),respawnCount,wire(respawnRevision),respawnHpGain,pendingDueMillis,deathX,deathY);}
    }
    record Heal(long entityId,long castCount,long revision,long mpSpent,long targetEntityId,long hpBefore,long hpAfter,long restored,
            int fromX,int fromY,int toX,int toY,int skillId,int mpCost) {
        Heal {
            if(entityId<1||castCount<0||revision<0||mpSpent<0||targetEntityId<0||hpBefore<0||hpAfter<hpBefore
                    ||restored<0||hpAfter-hpBefore!=restored||mpCost<0||mpCost>4||Math.multiplyExact(castCount,mpCost)!=mpSpent)throw bad();
            if(castCount==0){if(revision!=0||targetEntityId!=0||hpBefore!=0||hpAfter!=0||restored!=0||fromX!=0||fromY!=0||toX!=0||toY!=0||skillId!=0)throw bad();}
            else if(revision==0||targetEntityId<3||hpBefore==0||skillId!=1||!inScreen(fromX,fromY,toX,toY))throw bad();
        }
        static Heal initial(long entity,int cost){return new Heal(entity,0,0,0,0,0,0,0,0,0,0,0,0,cost);}
        Heal cast(long next,Objective before,Objective after,ModernWorldMap.Cell from){var spending=ModernSkillSpending.advance(castCount,mpSpent,mpCost);return new Heal(entityId,spending.casts(),next,spending.spent(),before.entityId(),before.hp(),after.hp(),after.hp()-before.hp(),from.x(),from.y(),before.x(),before.y(),1,mpCost);}
        Heal wireRevision(){return new Heal(entityId,castCount,wire(revision),mpSpent,targetEntityId,hpBefore,hpAfter,restored,fromX,fromY,toX,toY,skillId,mpCost);}
    }
    record Snapshot(List<Objective> objectives,List<Life> lives,List<Heal> heals) {
        Snapshot {
            objectives=List.copyOf(objectives);lives=List.copyOf(lives);heals=List.copyOf(heals);
            if(objectives.isEmpty()){if(!lives.isEmpty()||!heals.isEmpty())throw bad();}
            else {
                int n=lives.size();if(objectives.size()!=2||n<2||n>100||heals.size()!=n)throw bad();
                for(int i=0;i<2;i++)if(objectives.get(i).entityId()!=n+i+1||objectives.get(i).team()!=i||objectives.get(i).lastActorEntityId()>n)throw bad();
                if(objectives.get(0).cell().equals(objectives.get(1).cell()))throw bad();
                for(int i=0;i<n;i++)if(lives.get(i).entityId()!=i+1||heals.get(i).entityId()!=i+1)throw bad();
            }
        }
        static Snapshot disabled(){return new Snapshot(List.of(),List.of(),List.of());}
        boolean enabled(){return !objectives.isEmpty();}
        Objective objective(long entity){if(!enabled())return null;long index=entity-lives.size()-1L;return index>=0&&index<2?objectives.get((int)index):null;}
        Snapshot wireRevision(){return new Snapshot(objectives.stream().map(Objective::wireRevision).toList(),lives.stream().map(Life::wireRevision).toList(),heals.stream().map(Heal::wireRevision).toList());}
    }
    static int action(long hp,long max){if(hp==0)return 35;return hp<max/4?34:hp<max/2?33:
            BigInteger.valueOf(hp).compareTo(BigInteger.valueOf(max).multiply(BigInteger.valueOf(3)).divide(BigInteger.valueOf(4)))<0?32:0;}
    static boolean inScreen(int x,int y,int tx,int ty){long dx=(long)tx-x,dy=(long)ty-y,d=Math.abs(dx)+Math.abs(dy);return d<=18||d==19&&Math.abs(dx+18)+Math.abs(dy+18)<=52;}
    private static long wire(long revision){return revision==0?0:Math.incrementExact(revision);}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid honor authority value");}
    private ModernHonor(){}
}
