package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** v26 dedicated siege contract. Tower identities never alias player/NPC ids.
 * Engine revisions remain engine revisions; world/vital revisions add one.
 * The full configured gate and camps let the shared-resource client apply the
 * same movement policy. No LT resource identifiers or client damage inputs. */
final class ModernSiegeProtocol {
    static final int VERSION=26, REQUEST_BYTES=56;
    enum Action { ATTACK_TOWER, PORTAL }
    enum Reason { NONE, NOT_ACTIVE, DEAD, TARGET_DEAD, SAME_TEAM, OUT_OF_RANGE, BLOCKED, TOO_FAST,
        UNSUPPORTED, NUMERIC, TOO_HEAVY, STALE_EFFECT, NO_AMMO, STALE_REGEN, ATTACKER, NOT_AT_PORTAL, OCCUPIED }
    record Request(ModernRooms.Scope player,ModernSiege.Scope world,Action action,long sequence,ModernSiege.Slot tower) {
        Request {Objects.requireNonNull(player);Objects.requireNonNull(world);Objects.requireNonNull(action);
            if(sequence<1||(action==Action.ATTACK_TOWER)!=(tower!=null))throw bad();}
    }
    record Receipt(UUID id,long revision,long recordedAtMillis,ModernWorldProtocol.SettlementState state) {
        Receipt {Objects.requireNonNull(id);Objects.requireNonNull(state);
            if((id.getMostSignificantBits()|id.getLeastSignificantBits())==0||revision<1||recordedAtMillis<0)throw bad();}
    }
    record Data(ModernSiege.Snapshot snapshot,Receipt receipt) {
        Data {Objects.requireNonNull(snapshot);if(!snapshot.enabled()||snapshot.finished()!=(receipt!=null)
                ||receipt!=null&&receipt.revision()!=snapshot.state().finishRevision())throw bad();}
        boolean finished(){return snapshot.finished();}
        Data withSettlement(ModernWorldProtocol.SettlementState state){if(receipt==null)throw bad();
            return new Data(snapshot,new Receipt(receipt.id(),receipt.revision(),receipt.recordedAtMillis(),state));}
        Data abortedBeforeEngine(){var s=snapshot.state();if(s.phase()!=ModernSiege.Phase.WAITING||s.revision()!=1)throw bad();
            return new Data(new ModernSiege.Snapshot(s.abort(2),snapshot.lives()),null);}
        ModernMatchSettlement.Event event(ModernWorldProtocol.Frame w) {
            if(receipt==null)throw bad();var s=snapshot.state();var c=s.config();
            var proof=new ModernSiegeSettlement.Proof(s.scope().roomId(),w.manifest().mapId(),w.manifest().digest(),ModernSiegeSettlement.configHash(c),
                    s.attacker(),s.winner(),s.winner()==s.attacker()?ModernSiegeSettlement.End.INNER_DESTROYED:ModernSiegeSettlement.End.TIMEOUT,
                    Math.toIntExact(c.durationMillis()),Math.toIntExact(c.respawnMillis()),s.finishedAtMillis(),s.gateRevision(),s.objectives());
            var rows=w.actors().stream().map(a->new ModernMatchSettlement.Participant(a.entityId(),a.member().identity(),a.member().team(),
                    a.member().team()==s.winner()?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS)).toList();
            return new ModernMatchSettlement.Event(receipt.id(),w.matchId(),w.generation(),receipt.revision(),3,c.key().logicalMapId(),receipt.recordedAtMillis(),rows,null,proof);
        }
    }
    static Data preparing(ModernSiege.Scope scope,ModernSiege.Config config,int attacker,int count) {
        var lives=new ArrayList<ModernSiege.Life>();for(int i=1;i<=count;i++)lives.add(ModernSiege.Life.initial(i));
        return new Data(new ModernSiege.Snapshot(ModernSiege.initial(scope,config,attacker),lives),null);
    }
    static ModernWorldProtocol.Frame frame(ModernBattleEngine.Snapshot w,long revision,ModernMatchDescriptor descriptor,
            ModernMatchSettlement.Event event,ModernWorldProtocol.SettlementState state) {
        w.siege().validate(w);if(!w.siege().enabled()||w.siege().finished()!=(event!=null))throw bad();
        if(event!=null&&!ModernSiegeSettlement.event(w,event.resultId(),event.finishedAtMillis()).equals(event))throw bad();
        return new ModernWorldProtocol.Frame(w.matchId(),w.generation(),revision,ModernWorldProtocol.Phase.valueOf(w.phase().name()),
                ModernWorldProtocol.AbortReason.valueOf(w.abortReason().name()),w.room(),w.manifest(),w.actors(),null,
                w.casts().stream().map(ModernSpellProtocol.CastState::wireRevision).toList(),descriptor,w.race(),-1,ModernQuestProtocol.Data.disabled(),null,
                new Data(w.siege(),event==null?null:new Receipt(event.resultId(),event.engineRevision(),event.finishedAtMillis(),state)));
    }
    static void validate(ModernWorldProtocol.Frame w) {
        var d=w.siege();boolean enabled=w.descriptor()!=null&&w.descriptor().has(ModernMatchDescriptor.SIEGE);
        if(enabled!=(d!=null))throw bad();if(!enabled)return;
        var s=d.snapshot().state();var c=s.config();int[] teams={0,0};
        if(!s.scope().equals(new ModernSiege.Scope(w.room().summary().id(),w.matchId(),w.generation()))
                ||!c.key().equals(new ModernWorldBinding.Key(w.room().summary().config().mode(),w.room().summary().config().mapId()))
                ||w.audienceLane()!=-1||w.completion()!=null||w.race().enabled()||w.quest().enabled()||w.timeAttack()!=null
                ||d.snapshot().lives().size()!=w.actors().size()||s.revision()>w.revision()
                ||w.descriptor().activeTimeoutMillis()!=c.durationMillis()
                ||(w.phase()==ModernWorldProtocol.Phase.FINISHED)!=s.finished()
                ||(w.phase()==ModernWorldProtocol.Phase.ABORTED)!=(s.phase()==ModernSiege.Phase.ABORTED)
                ||(w.phase()==ModernWorldProtocol.Phase.PREPARING||w.phase()==ModernWorldProtocol.Phase.LOADING||w.phase()==ModernWorldProtocol.Phase.COUNTDOWN)!=(s.phase()==ModernSiege.Phase.WAITING))throw bad();
        var geometry=new HashSet<>(c.gate());geometry.addAll(c.towerCells());geometry.addAll(c.attackCamp());geometry.addAll(c.defenseCamp());
        geometry.add(c.portal().outside());geometry.add(c.portal().inside());for(var p:geometry)if(!ModernWorldProtocol.contains(w.manifest(),p))throw bad();
        for(int i=0;i<w.actors().size();i++) {
            var a=w.actors().get(i);var life=d.snapshot().lives().get(i);teams[a.member().team()]++;
            if(c.towerCells().contains(a.position())||!s.open()&&c.gate().contains(a.position())
                    ||life.deathRevision()>=w.revision()||life.respawnRevision()>=w.revision()
                    ||s.phase()==ModernSiege.Phase.WAITING&&(!life.equals(ModernSiege.Life.initial(a.entityId()))||!c.camp(a.member().team(),s.attacker()).contains(a.position()))
                    ||w.phase()!=ModernWorldProtocol.Phase.ACTIVE&&life.dueMillis()!=0)throw bad();
        }
        if(teams[0]<15||teams[1]<15||teams[0]>50||teams[1]>50)throw bad();
        boolean preEngine=w.revision()==1&&w.phase()==ModernWorldProtocol.Phase.PREPARING
                ||w.revision()==2&&w.phase()==ModernWorldProtocol.Phase.ABORTED&&w.actors().stream().noneMatch(ModernBattleEngine.Actor::loaded)
                    &&s.objectives().stream().allMatch(t->t.hitCount()==0);
        if(s.revision()>=w.revision()&&!preEngine)throw bad();
        for(var t:s.objectives())if(t.lastActor()>w.actors().size()||t.hitCount()>0&&w.actors().get((int)t.lastActor()-1).member().team()!=s.attacker()
                ||t.hitCount()>t.revision()||t.hp()==0&&t.deathRevision()!=t.revision()
                ||t.tower().slot()==ModernSiege.Slot.INNER&&t.hitCount()>0&&(s.gateRevision()==0||t.revision()<=s.gateRevision()))throw bad();
        if(d.receipt()!=null){if(d.receipt().revision()>=w.revision())throw bad();d.event(w);}
    }
    static void validateCombat(ModernWorldProtocol.Frame w,List<ModernCombatProtocol.Vital> vitals) {
        if(w.siege()==null)return;var lives=w.siege().snapshot().lives();if(lives.size()!=vitals.size())throw bad();
        for(int i=0;i<lives.size();i++){var l=lives.get(i);var v=vitals.get(i);
            if(v.entityId()!=l.entityId()||(l.deathRevision()==0?0:Math.incrementExact(l.deathRevision()))!=v.deathRevision()
                    ||l.hpGain()!=Math.multiplyExact(l.respawns(),v.maxHp())||l.deaths()>l.deathRevision()||l.respawns()>l.respawnRevision()
                    ||(v.hp()==0)!=(l.deaths()>l.respawns())
                    ||w.phase()==ModernWorldProtocol.Phase.ACTIVE&&v.hp()==0&&l.dueMillis()==0)throw bad();}
    }
    record Reply(ModernCombatProtocol.Status status,Reason reason,ModernCombatProtocol.Frame frame) {
        Reply {Objects.requireNonNull(status);Objects.requireNonNull(reason);
            if((status==ModernCombatProtocol.Status.OK)!=(reason==Reason.NONE)
                    ||frame==null&&(status!=ModernCombatProtocol.Status.UNAVAILABLE||reason!=Reason.NOT_ACTIVE)
                    ||frame!=null&&frame.world().siege()==null)throw bad();}
    }
    static void validate(ModernApplicationProtocol.SiegeResult r) {
        var q=Objects.requireNonNull(r.echo());var reply=Objects.requireNonNull(r.reply());if(reply.frame()==null)return;
        var w=reply.frame().world();var s=w.siege().snapshot().state();
        if(!s.scope().equals(q.world()))throw bad();var key=ModernParticipantId.human(q.player().sourceId(),q.player().objectId());
        var actor=w.actors().stream().filter(a->a.member().identity().equals(key)).findFirst().orElseThrow(ModernSiegeProtocol::bad);
        if(reply.status()==ModernCombatProtocol.Status.OK) {
            if(q.action()==Action.ATTACK_TOWER){var t=s.objectives().get(q.tower().ordinal());
                if(t.lastActor()!=actor.entityId()||t.revision()!=w.revision()-1||reply.frame().vital(actor.entityId()).attackSequence()!=q.sequence())throw bad();
            }else if(actor.member().team()==s.attacker()||actor.lastMoveSequence()!=q.sequence()
                    ||s.config().portal().destination(actor.position())==null||w.phase()!=ModernWorldProtocol.Phase.ACTIVE)throw bad();
        }
    }
    static void putRequest(ByteBuffer b,Request q){var p=q.player();var s=q.world();b.putInt(p.sourceId()).putLong(p.objectId()).putLong(p.epoch()).put((byte)p.channelId())
            .put((byte)q.action().ordinal()).put((byte)(q.tower()==null?255:q.tower().ordinal())).put((byte)0).putLong(s.roomId()).putLong(s.matchId()).putLong(s.generation()).putLong(q.sequence());}
    static Request getRequest(ByteBuffer b){var p=new ModernRooms.Scope(b.getInt(),b.getLong(),b.getLong(),u8(b));var a=at(Action.values(),u8(b));int slot=u8(b);zero(b,1);
        return new Request(p,new ModernSiege.Scope(b.getLong(),b.getLong(),b.getLong()),a,b.getLong(),slot==255?null:at(ModernSiege.Slot.values(),slot));}
    static void putResult(ByteBuffer b,ModernApplicationProtocol.SiegeResult r,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();validate(r);putRequest(b,r.echo());var v=r.reply();b.put((byte)v.status().ordinal()).put((byte)v.reason().ordinal()).put((byte)(v.frame()==null?0:1)).put((byte)0);
        if(v.frame()!=null)ModernCombatProtocol.putFrame(b,v.frame(),version);
    }
    static ModernApplicationProtocol.SiegeResult getResult(ByteBuffer b,int id,int version)throws ModernApplicationProtocol.ProtocolException {
        if(version<VERSION)throw bad();var q=getRequest(b);var status=at(ModernCombatProtocol.Status.values(),u8(b));var reason=at(Reason.values(),u8(b));int present=u8(b);zero(b,1);if(present>1)throw bad();
        return new ModernApplicationProtocol.SiegeResult(id,q,new Reply(status,reason,present==0?null:ModernCombatProtocol.getFrame(b,version)));
    }
    static void put(ByteBuffer b,Data d) {
        b.put((byte)(d==null?0:1));zeroes(b,7);if(d==null)return;var s=d.snapshot().state();var c=s.config();
        b.putShort((short)1).put((byte)s.phase().ordinal()).put((byte)s.attacker()).put((byte)(s.winner()+1)).put((byte)(d.receipt()==null?0:1))
                .put((byte)c.gate().size()).put((byte)c.attackCamp().size()).put((byte)c.defenseCamp().size()).put((byte)d.snapshot().lives().size()).putShort((short)0);
        b.putLong(s.revision()).putLong(s.gateRevision()).putLong(s.finishRevision()).putLong(s.finishedAtMillis()).putLong(c.durationMillis()).putLong(c.respawnMillis());
        cell(b,c.portal().outside());cell(b,c.portal().inside());
        for(var t:s.objectives()){cell(b,t.tower().cell());b.putInt(t.tower().gfx()).putLong(t.tower().maximumHp()).putLong(t.hp()).putLong(t.damageLoss()).putLong(t.hitCount()).putLong(t.revision()).putLong(t.deathRevision()).putLong(t.lastActor());}
        for(var p:c.gate().stream().sorted(Comparator.comparingInt(ModernWorldMap.Cell::x).thenComparingInt(ModernWorldMap.Cell::y)).toList())cell(b,p);
        for(var p:c.attackCamp())cell(b,p);for(var p:c.defenseCamp())cell(b,p);
        for(var l:d.snapshot().lives())b.putLong(l.entityId()).putLong(l.deaths()).putLong(l.deathRevision()).putLong(l.respawns()).putLong(l.respawnRevision()).putLong(l.hpGain()).putLong(l.dueMillis());
        if(d.receipt()!=null){var r=d.receipt();b.putLong(r.id().getMostSignificantBits()).putLong(r.id().getLeastSignificantBits()).putLong(r.revision()).putLong(r.recordedAtMillis()).put((byte)r.state().ordinal());zeroes(b,7);}
    }
    static Data get(ByteBuffer b,ModernSiege.Scope scope,ModernWorldBinding.Key key) {
        int present=u8(b);zero(b,7);if(present==0)return null;if(present!=1||b.getShort()!=1)throw bad();
        var phase=at(ModernSiege.Phase.values(),u8(b));int attacker=u8(b),winner=u8(b)-1,receipt=u8(b),gate=u8(b),attack=u8(b),defend=u8(b),lives=u8(b);zero(b,2);
        if(receipt>1||gate<1||gate>64||attack<1||attack>50||defend<1||defend>50||lives<30||lives>100
                ||b.remaining()<268+(gate+attack+defend)*8+lives*56+receipt*40)throw bad();
        long revision=b.getLong(),opened=b.getLong(),finish=b.getLong(),elapsed=b.getLong(),duration=b.getLong(),respawn=b.getLong();
        var portal=new ModernSiege.Portal(cell(b),cell(b));var towers=new ArrayList<ModernSiege.Objective>();
        for(var slot:ModernSiege.Slot.values()){var t=new ModernSiege.Tower(slot,cell(b),b.getInt(),b.getLong());towers.add(new ModernSiege.Objective(t,b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong()));}
        var gates=new HashSet<ModernWorldMap.Cell>();for(int i=0;i<gate;i++)if(!gates.add(cell(b)))throw bad();
        var attacks=new ArrayList<ModernWorldMap.Cell>();for(int i=0;i<attack;i++)attacks.add(cell(b));
        var defenses=new ArrayList<ModernWorldMap.Cell>();for(int i=0;i<defend;i++)defenses.add(cell(b));
        var rows=new ArrayList<ModernSiege.Life>();for(int i=0;i<lives;i++)rows.add(new ModernSiege.Life(b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong(),b.getLong()));
        Receipt r=null;if(receipt==1){r=new Receipt(new UUID(b.getLong(),b.getLong()),b.getLong(),b.getLong(),at(ModernWorldProtocol.SettlementState.values(),u8(b)));zero(b,7);}
        var config=new ModernSiege.Config(key,towers.stream().map(ModernSiege.Objective::tower).toList(),gates,attacks,defenses,portal,duration,respawn);
        return new Data(new ModernSiege.Snapshot(new ModernSiege.State(scope,config,attacker,phase,towers,revision,opened,finish,elapsed,winner),rows),r);
    }
    private static void cell(ByteBuffer b,ModernWorldMap.Cell c){b.putInt(c.x()).putInt(c.y());}
    private static ModernWorldMap.Cell cell(ByteBuffer b){return new ModernWorldMap.Cell(b.getInt(),b.getInt());}
    private static int u8(ByteBuffer b){return Byte.toUnsignedInt(b.get());}
    private static void zero(ByteBuffer b,int n){while(n-->0)if(b.get()!=0)throw bad();}
    private static void zeroes(ByteBuffer b,int n){while(n-->0)b.put((byte)0);}
    private static <T>T at(T[] values,int i){if(i<0||i>=values.length)throw bad();return values[i];}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid dedicated siege wire value");}
    private ModernSiegeProtocol(){}
}
