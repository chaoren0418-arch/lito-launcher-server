package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** v25 cooperative world extension. Revisions here are engine revisions;
 * the enclosing world/vital wire revision is engine + 1. A terminal receipt
 * must be supplied from the engine's captured result, never minted by a codec. */
final class ModernTimeAttackProtocol {
    static final int VERSION=25, EXCHANGE_BYTES=56;
    record Receipt(UUID id,long revision,long finishedAtMillis,ModernWorldProtocol.SettlementState state) {
        Receipt {Objects.requireNonNull(id);Objects.requireNonNull(state);
            if((id.getMostSignificantBits()|id.getLeastSignificantBits())==0||revision<1||finishedAtMillis<0)throw bad();}
    }
    record Data(long revision,ModernTimeAttack.Phase phase,ModernTimeAttack.End end,
                List<ModernTimeAttack.Kind> waves,int wave,int leader,int durationMillis,
                long startedMillis,long observedMillis,long aliveMask,long departedMask,
                ModernTimeAttack.Tower tower,long towerHp,long towerHurtRevision,long towerDeathRevision,
                String planSha256,List<Long> living,List<ModernTimeAttack.Exchange> exchanges,Receipt receipt) {
        Data {
            Objects.requireNonNull(phase);Objects.requireNonNull(end);Objects.requireNonNull(tower);
            waves=List.copyOf(waves);living=List.copyOf(living);exchanges=List.copyOf(exchanges);
            if(revision<0||revision==0&&(phase!=ModernTimeAttack.Phase.WAITING_FOR_TOWER||observedMillis!=0||departedMask!=0)
                    ||waves.size()<2||waves.size()>32||wave<0||wave>=waves.size()||leader<1||leader>60
                    ||durationMillis<50||durationMillis>1_800_000||startedMillis< -1||startedMillis>=60_000
                    ||observedMillis<0||observedMillis>ModernNpcRoster.MAX_ELAPSED_MILLIS||startedMillis>observedMillis
                    ||aliveMask<0||aliveMask>>>60!=0||departedMask<0||departedMask>>>60!=0
                    ||towerHp<0||towerHp>tower.maximumHp()||towerHurtRevision<0||towerHurtRevision>revision
                    ||towerDeathRevision<0||towerDeathRevision>revision||(towerHp==0)!=(towerDeathRevision>0)
                    ||(towerHp<tower.maximumHp())!=(towerHurtRevision>0)||(startedMillis>=0)!=(towerHp==0)
                    ||towerDeathRevision>0&&towerDeathRevision!=towerHurtRevision
                    ||planSha256==null||!planSha256.matches("[0-9a-f]{64}")||planSha256.equals("0".repeat(64))
                    ||living.size()>128||exchanges.size()>60||(phase==ModernTimeAttack.Phase.FINISHED)!=(receipt!=null)
                    ||(receipt!=null)!=(end!=ModernTimeAttack.End.NONE)||receipt!=null&&receipt.revision()<revision)throw bad();
            int finals=0,dragons=0;for(int i=0;i<waves.size();i++) {
                var k=waves.get(i);if(i==0&&k!=ModernTimeAttack.Kind.ORDINARY
                        ||k==ModernTimeAttack.Kind.ORDINARY&&(finals!=0||dragons!=0)
                        ||k==ModernTimeAttack.Kind.FINAL_BOSSES&&(++finals!=1||dragons!=0)
                        ||k==ModernTimeAttack.Kind.DRAGON&&(finals!=1||++dragons!=1||i!=waves.size()-1))throw bad();
            }
            if(finals!=1||phase==ModernTimeAttack.Phase.WAITING_FOR_TOWER&&(startedMillis!=-1||wave!=0||!living.isEmpty())
                    ||phase==ModernTimeAttack.Phase.FIGHTING&&(startedMillis<0||living.isEmpty())
                    ||phase==ModernTimeAttack.Phase.SPAWN_PENDING&&(startedMillis<0||!living.isEmpty())
                    ||startedMillis<0&&(wave!=0||!living.isEmpty())
                    ||end==ModernTimeAttack.End.CLEARED&&!living.isEmpty())throw bad();
            if(phase!=ModernTimeAttack.Phase.FINISHED&&phase!=ModernTimeAttack.Phase.ABORTED
                    &&((aliveMask&~departedMask)&(1L<<(leader-1)))==0)throw bad();
            long previous=0;for(long serial:living){if(serial<=previous)throw bad();previous=serial;}
            var seen=new HashSet<Long>();previous=0;
            for(var e:exchanges){var o=e.outcome();if(!seen.add(e.entityId())||e.revision()<=previous||e.revision()>revision
                    ||o.remainingHp()<towerHp||o.remainingHp()>tower.maximumHp()||o.appliedDamage()>tower.maximumHp()-o.remainingHp()
                    ||o.death()!=(o.remainingHp()==0))throw bad();previous=e.revision();}
            // Proof kind is checked with the enclosing descriptor/frame. The
            // legacy codec still requires a melee proof; skill70 is atomic.
        }
        boolean departed(long entity){return (departedMask&(1L<<(entity-1)))!=0;}
        boolean finished(){return phase==ModernTimeAttack.Phase.FINISHED;}
        Data withSettlement(ModernWorldProtocol.SettlementState state) {
            if(receipt==null)throw bad();
            return new Data(revision,phase,end,waves,wave,leader,durationMillis,startedMillis,observedMillis,aliveMask,departedMask,
                    tower,towerHp,towerHurtRevision,towerDeathRevision,planSha256,living,exchanges,
                    new Receipt(receipt.id(),receipt.revision(),receipt.finishedAtMillis(),state));
        }
        Data abortedBeforeEngine() {
            if(revision!=0||receipt!=null)throw bad();
            return new Data(1,ModernTimeAttack.Phase.ABORTED,end,waves,wave,leader,durationMillis,startedMillis,observedMillis,aliveMask,departedMask,
                    tower,towerHp,towerHurtRevision,towerDeathRevision,planSha256,living,exchanges,null);
        }
        ModernTimeAttackSettlement.Proof proof(ModernWorldProtocol.Frame world) {
            return new ModernTimeAttackSettlement.Proof(world.room().summary().id(),world.manifest().mapId(),world.manifest().digest(),planSha256,
                    end,wave,waves.size(),leader,durationMillis,startedMillis,observedMillis,aliveMask,departedMask,towerDeathRevision);
        }
        ModernMatchSettlement.Event event(ModernWorldProtocol.Frame world) {
            if(receipt==null)throw bad();
            var rows=world.actors().stream().map(a->new ModernMatchSettlement.Participant(a.entityId(),a.member().identity(),0,
                    end==ModernTimeAttack.End.CLEARED&&(aliveMask&(1L<<(a.entityId()-1)))!=0&&!departed(a.entityId())
                            ?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
            return new ModernMatchSettlement.Event(receipt.id(),world.matchId(),world.generation(),receipt.revision(),4,540,receipt.finishedAtMillis(),rows,proof(world));
        }
    }
    /** PREPARING owns no combat engine yet; zero is reserved for that stage. */
    static Data preparing(ModernTimeAttack.Plan plan,int leader,int participants) {
        if(participants<1||participants>60||leader<1||leader>participants)throw bad();
        return new Data(0,ModernTimeAttack.Phase.WAITING_FOR_TOWER,ModernTimeAttack.End.NONE,
                plan.waves().stream().map(ModernTimeAttack.Wave::kind).toList(),0,leader,plan.durationMillis(),-1,0,(1L<<participants)-1,0,
                plan.tower(),plan.tower().maximumHp(),0,0,ModernTimeAttackSettlement.planHash(plan),List.of(),List.of(),null);
    }
    static Data from(ModernBattleEngine.Snapshot w,ModernMatchSettlement.Event result,ModernWorldProtocol.SettlementState state) {
        if(!w.npcFireStorms().isEmpty())throw new IllegalArgumentException("NPC fire storm requires its own typed wire contract");
        return fromFireStorm(w,result,state);
    }
    /** Explicit atomic-frame projection seam; not runtime admission. */
    static Data fromFireStorm(ModernBattleEngine.Snapshot w,ModernMatchSettlement.Event result,ModernWorldProtocol.SettlementState state) {
        w.timeAttack().validate(w);if(!w.timeAttack().enabled())throw bad();var s=w.timeAttack().state();long alive=0,left=0;
        for(var v:w.vitals())if(v.hp()>0)alive|=1L<<(v.entityId()-1);for(long id:s.departed())left|=1L<<(id-1);
        if(w.timeAttack().finished()!=(result!=null))throw bad();
        if(result!=null&&!ModernTimeAttackSettlement.event(w,result.resultId(),result.finishedAtMillis()).equals(result))throw bad();
        return new Data(s.revision(),s.phase(),s.end(),s.plan().waves().stream().map(ModernTimeAttack.Wave::kind).toList(),s.wave(),Math.toIntExact(s.leader()),s.plan().durationMillis(),
                s.startedMillis(),s.observedMillis(),alive,left,s.plan().tower(),s.towerHp(),s.towerHurtRevision(),s.towerDeathRevision(),ModernTimeAttackSettlement.planHash(s.plan()),
                s.living().stream().map(ModernNpcRoster.Id::serial).sorted().toList(),w.timeAttack().exchanges(),
                result==null?null:new Receipt(result.resultId(),result.engineRevision(),result.finishedAtMillis(),state));
    }
    static void validate(ModernWorldProtocol.Frame w) {
        var d=w.timeAttack();boolean enabled=w.descriptor()!=null&&w.descriptor().has(ModernMatchDescriptor.TIME_ATTACK);
        if(enabled!=(d!=null))throw bad();if(!enabled)return;
        if(!w.descriptor().has(ModernMatchDescriptor.NPC_FIRE_STORM))requireMeleeDeath(d);
        int count=w.actors().size();var scope=new ModernNpcRoster.Scope(w.room().summary().id(),w.matchId(),w.generation());
        if(w.room().summary().config().mode()!=4||w.room().summary().config().mapId()!=540||count>60||w.audienceLane()!=-1
                ||w.completion()!=null||w.race().enabled()||w.quest().enabled()||d.revision()>=w.revision()||d.leader()>count
                ||(w.phase()==ModernWorldProtocol.Phase.PREPARING)!=(d.revision()==0)
                ||d.aliveMask()>>>count!=0||d.departedMask()>>>count!=0||!ModernWorldProtocol.contains(w.manifest(),d.tower().cell())
                ||(w.phase()==ModernWorldProtocol.Phase.FINISHED)!=d.finished()
                ||(w.phase()==ModernWorldProtocol.Phase.ABORTED)!=(d.phase()==ModernTimeAttack.Phase.ABORTED)
                ||(w.phase()==ModernWorldProtocol.Phase.PREPARING||w.phase()==ModernWorldProtocol.Phase.LOADING||w.phase()==ModernWorldProtocol.Phase.COUNTDOWN)
                    &&(d.phase()!=ModernTimeAttack.Phase.WAITING_FOR_TOWER||d.observedMillis()!=0||!d.exchanges().isEmpty()||d.departedMask()!=0||d.aliveMask()!=((1L<<count)-1))
                ||w.actors().stream().anyMatch(a->!a.member().identity().isHuman()||a.member().team()!=0
                    ||!d.departed(a.entityId())&&d.towerHp()>0&&a.position().equals(d.tower().cell())))throw bad();
        for(var e:d.exchanges())if(e.entityId()>count||!e.target().scope().equals(scope))throw bad();
        if(d.receipt()!=null){if(d.receipt().revision()>=w.revision())throw bad();d.event(w);}
        else if(d.startedMillis()>=0&&d.phase()!=ModernTimeAttack.Phase.ABORTED&&d.observedMillis()-d.startedMillis()>=d.durationMillis())throw bad();
    }
    static void validateCombat(ModernWorldProtocol.Frame world,List<ModernCombatProtocol.Vital> vitals) {
        var d=world.timeAttack();if(d==null)return;long alive=0;for(var v:vitals)if(v.hp()>0)alive|=1L<<(v.entityId()-1);
        if(alive!=d.aliveMask())throw bad();
        for(var e:d.exchanges())if(e.sequence()>vitals.stream().filter(v->v.entityId()==e.entityId()).findFirst().orElseThrow(ModernTimeAttackProtocol::bad).attackSequence())throw bad();
    }
    static void validateNpcs(ModernCombatProtocol.Frame combat,long spawned,boolean closed,List<ModernNpcProtocol.Actor> actors) {
        var d=combat.world().timeAttack();if(d==null)return;
        var living=actors.stream().filter(a->a.hp()>0).map(ModernNpcProtocol.Actor::serial).toList();
        if(!closed&&!living.equals(d.living())||d.living().stream().anyMatch(id->id>spawned)
                ||actors.stream().anyMatch(a->a.lane()!=0||a.hp()>0&&a.stage()!=d.waves().get(d.wave()).ordinal()+1))throw bad();
        validateCombat(combat.world(),combat.vitals());
    }
    /** Explicit projection seam; runtime admission/journaling is not implied. */
    static ModernNpcProtocol.Frame frame(ModernNpcBattle.Frame value,ModernMatchDescriptor descriptor,
            ModernMatchSettlement.Event result,ModernWorldProtocol.SettlementState state,ModernNpcProtocol.Resources resources) {
        var w=value.world();var n=value.npcs();var data=descriptor.has(ModernMatchDescriptor.NPC_FIRE_STORM)?fromFireStorm(w,result,state):from(w,result,state);
        var world=new ModernWorldProtocol.Frame(w.matchId(),w.generation(),Math.incrementExact(w.revision()),
                ModernWorldProtocol.Phase.valueOf(w.phase().name()),ModernWorldProtocol.AbortReason.valueOf(w.abortReason().name()),
                w.room(),w.manifest(),w.actors(),null,w.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList(),
                descriptor,w.race(),-1,ModernQuestProtocol.Data.disabled(),data);
        return new ModernNpcProtocol.Frame(ModernWorldService.fromCombat(w,world),n.scope(),n.revision(),n.elapsedMillis(),n.spawned(),n.closed(),
                n.actors().stream().map(ModernNpcProtocol.Actor::from).toList(),value.exchanges().stream().map(e->new ModernNpcBattle.Exchange(
                    e.direction(),e.participant(),e.participantLife(),e.npc(),e.npcLife(),e.requestSequence(),Math.incrementExact(e.revision()),e.outcome())).toList(),resources);
    }
    static void put(ByteBuffer b,Data d) {
        put(b,d,false);
    }
    static void put(ByteBuffer b,Data d,boolean npcFireStorm) {
        if(!npcFireStorm&&d!=null)requireMeleeDeath(d);
        b.put((byte)(d==null?0:1));zeroes(b,7);if(d==null)return;
        b.putShort((short)1).put((byte)d.phase().ordinal()).put((byte)d.end().ordinal()).put((byte)d.wave()).put((byte)d.leader())
                .put((byte)d.waves().size()).put((byte)(d.receipt()==null?0:1));
        b.putLong(d.revision()).putLong(d.startedMillis()).putLong(d.observedMillis()).putLong(d.aliveMask()).putLong(d.departedMask());
        b.putInt(d.tower().cell().x()).putInt(d.tower().cell().y()).putInt(d.tower().graphic()).putInt(d.durationMillis())
                .putLong(d.tower().maximumHp()).putLong(d.towerHp()).putLong(d.towerHurtRevision()).putLong(d.towerDeathRevision());
        b.put(HexFormat.of().parseHex(d.planSha256())).putShort((short)d.living().size()).putShort((short)d.exchanges().size());zeroes(b,4);
        for(var k:d.waves())b.put((byte)k.ordinal());for(long id:d.living())b.putLong(id);
        for(var e:d.exchanges()){var o=e.outcome();b.putLong(e.entityId()).putLong(e.sequence()).putLong(e.revision())
                .put((byte)((o.hit()?1:0)|(o.death()?2:0)|(o.maximumTriggered()?4:0)|(o.criticalTriggered()?8:0)));zeroes(b,7);
            b.putLong(o.rawDamage()).putLong(o.appliedDamage()).putLong(o.remainingHp());}
        if(d.receipt()!=null){var r=d.receipt();b.putLong(r.id().getMostSignificantBits()).putLong(r.id().getLeastSignificantBits())
                .putLong(r.revision()).putLong(r.finishedAtMillis()).put((byte)r.state().ordinal());zeroes(b,7);}
    }
    static Data get(ByteBuffer b,ModernNpcRoster.Scope scope) {
        return get(b,scope,false);
    }
    static Data get(ByteBuffer b,ModernNpcRoster.Scope scope,boolean npcFireStorm) {
        int present=u8(b);zero(b,7);if(present==0)return null;if(present!=1||b.getShort()!=1)throw bad();
        var phase=at(ModernTimeAttack.Phase.values(),u8(b));var end=at(ModernTimeAttack.End.values(),u8(b));
        int wave=u8(b),leader=u8(b),count=u8(b),receipt=u8(b);if(count<2||count>32||receipt>1)throw bad();
        long revision=b.getLong(),start=b.getLong(),observed=b.getLong(),alive=b.getLong(),left=b.getLong();
        int x=b.getInt(),y=b.getInt(),gfx=b.getInt(),duration=b.getInt();long max=b.getLong(),hp=b.getLong(),hurt=b.getLong(),death=b.getLong();
        byte[] hash=new byte[32];b.get(hash);int livingCount=Short.toUnsignedInt(b.getShort()),hits=Short.toUnsignedInt(b.getShort());zero(b,4);
        if(livingCount>128||hits>60||b.remaining()<count+livingCount*8+hits*EXCHANGE_BYTES+receipt*40)throw bad();
        var waves=new ArrayList<ModernTimeAttack.Kind>(count);for(int i=0;i<count;i++)waves.add(at(ModernTimeAttack.Kind.values(),u8(b)));
        var living=new ArrayList<Long>(livingCount);for(int i=0;i<livingCount;i++)living.add(b.getLong());
        var exchanges=new ArrayList<ModernTimeAttack.Exchange>(hits);for(int i=0;i<hits;i++){
            long entity=b.getLong(),seq=b.getLong(),rev=b.getLong();int flags=u8(b);if(flags>15)throw bad();zero(b,7);
            exchanges.add(new ModernTimeAttack.Exchange(entity,new ModernTimeAttack.TowerId(scope),seq,rev,
                    new ModernPhysicalCombat.Outcome((flags&1)!=0,b.getLong(),b.getLong(),b.getLong(),(flags&2)!=0,(flags&4)!=0,(flags&8)!=0)));}
        Receipt result=null;if(receipt==1){result=new Receipt(new UUID(b.getLong(),b.getLong()),b.getLong(),b.getLong(),at(ModernWorldProtocol.SettlementState.values(),u8(b)));zero(b,7);}
        var data=new Data(revision,phase,end,waves,wave,leader,duration,start,observed,alive,left,new ModernTimeAttack.Tower(new ModernWorldMap.Cell(x,y),gfx,max),hp,hurt,death,HexFormat.of().formatHex(hash),living,exchanges,result);
        if(!npcFireStorm)requireMeleeDeath(data);return data;
    }
    private static void requireMeleeDeath(Data d) {
        if(d.towerDeathRevision()>0&&d.exchanges().stream().noneMatch(e->e.revision()==d.towerDeathRevision()&&e.outcome().death()))throw bad();
    }
    private static int u8(ByteBuffer b){return Byte.toUnsignedInt(b.get());}
    private static void zero(ByteBuffer b,int n){while(n-->0)if(b.get()!=0)throw bad();}
    private static void zeroes(ByteBuffer b,int n){while(n-->0)b.put((byte)0);}
    private static <T>T at(T[] values,int i){if(i>=values.length)throw bad();return values[i];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid cooperative wire value");}
    private ModernTimeAttackProtocol(){}
}
