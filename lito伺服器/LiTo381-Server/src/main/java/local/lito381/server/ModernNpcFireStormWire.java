package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Candidate v31 payload sections, not yet admitted by session negotiation or
 * an application frame. The enclosing atomic NPC frame must supply scope and
 * validate actors, sequence/MP budgets, lives and mode progress before use.
 * All integers are big endian. Structure history is NOT a fresh cast effect. */
final class ModernNpcFireStormWire {
    static final int VERSION=31, REQUEST_BYTES=64, HEADER_BYTES=72, TARGET_BYTES=64, STRUCTURE_BYTES=104, MAX_ACTORS=100;
    // The section alone can exceed the old NPC 256 KiB envelope. Admission
    // must introduce a versioned envelope bound; never trim receipts to fit.
    static final int MAX_SECTION_BYTES=8+MAX_ACTORS*(HEADER_BYTES+ModernNpcFireStorm.MAX_TARGETS*TARGET_BYTES+STRUCTURE_BYTES);
    static void putRequest(ByteBuffer out,ModernNpcFireStormState.Request r,int version) {
        require(out,version);Objects.requireNonNull(r);if(out.remaining()<REQUEST_BYTES)throw bad();
        var p=r.player();var w=r.world();
        out.putInt(p.sourceId()).putLong(p.objectId()).putLong(p.epoch()).put((byte)p.channelId());zeroes(out,3);
        out.putLong(w.roomId()).putLong(w.matchId()).putLong(w.generation()).putLong(r.sequence()).putLong(r.casterLife());
    }
    static ModernNpcFireStormState.Request getRequest(ByteBuffer in,int version) {
        require(in,version);var b=in.duplicate();if(b.remaining()<REQUEST_BYTES)throw bad();
        int source=b.getInt();long obj=b.getLong(),epoch=b.getLong();int channel=Byte.toUnsignedInt(b.get());zero(b,3);
        var player=new ModernRooms.Scope(source,obj,epoch,channel);
        var scope=new ModernNpcRoster.Scope(b.getLong(),b.getLong(),b.getLong());
        var r=new ModernNpcFireStormState.Request(player,scope,b.getLong(),b.getLong());in.position(b.position());return r;
    }
    static int bytes(ModernNpcRoster.Scope scope,List<ModernNpcFireStormState.State> states,int actors) {
        Objects.requireNonNull(scope);Objects.requireNonNull(states);count(states.size(),actors);long previous=0;int size=8;
        for(var s:states) {
            ordered(s.entityId(),previous);previous=s.entityId();
            if(s.castCount()>0&&!scope.equals(s.scope()))throw bad();
            size=Math.addExact(size,HEADER_BYTES+s.targets().size()*TARGET_BYTES+(s.structure()==null?0:STRUCTURE_BYTES));
        }
        return size;
    }
    static void putStates(ByteBuffer out,ModernNpcRoster.Scope scope,List<ModernNpcFireStormState.State> states,int actors,int version) {
        require(out,version);states=List.copyOf(states);int size=bytes(scope,states,actors);if(out.remaining()<size)throw bad();
        out.putShort((short)states.size());zeroes(out,6);
        for(var s:states) {
            out.putLong(s.entityId()).putLong(s.castCount()).putLong(s.revision()).putLong(s.lastSequence()).putLong(s.mpSpent())
                    .putInt(s.mpCost()).putInt(s.mpCost()==0?0:ModernFireStorm.REUSE_MILLIS).putLong(s.casterLife());
            cell(out,s.origin());out.put((byte)s.lane()).put((byte)s.targets().size()).put((byte)(s.structure()==null?0:1));zeroes(out,5);
            for(var t:s.targets()) {
                out.putLong(t.id().serial()).putLong(t.life());cell(out,t.cell());out.putLong(t.hpBefore());outcome(out,t.outcome());
                out.put((byte)(t.outcome().death()?1:0));zeroes(out,7);
            }
            if(s.structure()!=null) {
                var e=s.structure();var h=e.hit();var q=h instanceof ModernNpcFireStormStructures.QuestHit v?v:null;
                out.putLong(e.castCount()).putLong(e.revision()).putLong(e.sequence()).putLong(e.casterLife());cell(out,e.origin());
                out.put((byte)(q==null?4:q.kind().ordinal()+1)).put((byte)(q==null?0:q.id().slot())).put((byte)(h.outcome().death()?1:0));zeroes(out,5);
                cell(out,h.cell());out.putLong(h.maximumHp()).putLong(h.hpBefore()).putLong(q==null?0:q.bornRevision());outcome(out,h.outcome());
            }
        }
    }
    static List<ModernNpcFireStormState.State> getStates(ByteBuffer in,ModernNpcRoster.Scope scope,int actors,int version) {
        require(in,version);Objects.requireNonNull(scope);var b=in.duplicate();
        if(b.remaining()<8)throw bad();int n=Short.toUnsignedInt(b.getShort());zero(b,6);count(n,actors);
        if(b.remaining()<n*HEADER_BYTES)throw bad();var result=new ArrayList<ModernNpcFireStormState.State>(n);long previous=0;
        for(int i=0;i<n;i++) {
            long entity=b.getLong(),casts=b.getLong(),rev=b.getLong(),seq=b.getLong(),spent=b.getLong();
            int cost=b.getInt(),reuse=b.getInt();long life=b.getLong();var origin=cell(b);
            int lane=Byte.toUnsignedInt(b.get()),hits=Byte.toUnsignedInt(b.get()),present=Byte.toUnsignedInt(b.get());zero(b,5);
            if(hits>ModernNpcFireStorm.MAX_TARGETS||present>1||reuse!=(cost==0?0:ModernFireStorm.REUSE_MILLIS)
                    ||b.remaining()<hits*TARGET_BYTES+present*STRUCTURE_BYTES
                    ||casts==0&&(lane!=255||origin.x()!=0||origin.y()!=0))throw bad();
            var targets=new ArrayList<ModernNpcFireStormState.Target>(hits);
            for(int j=0;j<hits;j++) {
                var id=new ModernNpcRoster.Id(scope,b.getLong());long targetLife=b.getLong();var to=cell(b);long hp=b.getLong();
                long raw=b.getLong(),applied=b.getLong(),remaining=b.getLong();int flags=Byte.toUnsignedInt(b.get());zero(b,7);
                targets.add(new ModernNpcFireStormState.Target(id,targetLife,lane,to,hp,outcome(raw,applied,remaining,flags)));
            }
            ModernNpcFireStormStructures.Receipt structure=null;
            if(present==1) {
                long count=b.getLong(),revision=b.getLong(),sequence=b.getLong(),casterLife=b.getLong();var from=cell(b);
                int kind=Byte.toUnsignedInt(b.get()),slot=Byte.toUnsignedInt(b.get()),flags=Byte.toUnsignedInt(b.get());zero(b,5);
                var to=cell(b);long maximum=b.getLong(),before=b.getLong(),born=b.getLong();
                var outcome=outcome(b.getLong(),b.getLong(),b.getLong(),flags);ModernNpcFireStormStructures.Hit hit;
                if(kind>=1&&kind<=3)hit=new ModernNpcFireStormStructures.QuestHit(new ModernQuestObjectives.Id(scope,slot),
                        ModernQuestObjectives.Kind.values()[kind-1],to,maximum,before,born,outcome);
                else if(kind==4&&slot==0&&born==0)hit=new ModernNpcFireStormStructures.TowerHit(new ModernTimeAttack.TowerId(scope),to,maximum,before,outcome);
                else throw bad();
                structure=new ModernNpcFireStormStructures.Receipt(count,revision,sequence,casterLife,from,hit);
            }
            var s=new ModernNpcFireStormState.State(entity,casts,rev,seq,spent,cost,life,casts==0?null:scope,casts==0?null:origin,casts==0?-1:lane,targets,structure);
            ordered(entity,previous);previous=entity;result.add(s);
        }
        in.position(b.position());return List.copyOf(result);
    }
    /** Ledger-only transition check. Full atomic frame validation is mandatory
     * in addition; this cannot authenticate world facts or authorize damage. */
    static boolean follows(ModernNpcRoster.Scope beforeScope,List<ModernNpcFireStormState.State> before,long beforeRevision,
            ModernNpcRoster.Scope afterScope,List<ModernNpcFireStormState.State> after,long afterRevision) {
        try {
            if(beforeScope==null||!beforeScope.equals(afterScope)||beforeRevision<1||afterRevision<beforeRevision||before.size()!=after.size())return false;
            bytes(beforeScope,before,Math.max(1,before.size()));bytes(afterScope,after,Math.max(1,after.size()));
            for(int i=0;i<before.size();i++) {
                var a=before.get(i);var b=after.get(i);
                if(a.entityId()!=b.entityId()||a.mpCost()!=b.mpCost()||a.revision()>beforeRevision||b.revision()>afterRevision||b.castCount()<a.castCount())return false;
                if(a.castCount()==b.castCount()){if(!a.equals(b))return false;continue;}
                if(b.revision()<=beforeRevision||b.lastSequence()<=a.lastSequence()||b.casterLife()<a.casterLife()
                        ||b.castCount()-a.castCount()>b.lastSequence()-a.lastSequence()
                        ||b.castCount()-a.castCount()>afterRevision-beforeRevision||a.castCount()>0&&a.lane()!=b.lane())return false;
                for(var old:a.targets())for(var hit:b.targets())if(old.id().equals(hit.id())
                        &&(hit.life()<old.life()||hit.life()==old.life()&&hit.hpBefore()>old.outcome().remainingHp()))return false;
                var x=a.structure();var y=b.structure();
                if(Objects.equals(x,y))continue;
                if(y==null||y.castCount()<=a.castCount()||y.revision()<=beforeRevision||y.sequence()<=a.lastSequence()||y.casterLife()<a.casterLife()
                        ||y.castCount()-a.castCount()>y.revision()-beforeRevision||y.castCount()-a.castCount()>y.sequence()-a.lastSequence())return false;
                if(x!=null&&!structureFollows(x,y))return false;
            }
            return true;
        }catch(IllegalArgumentException|ArithmeticException ex){return false;}
    }
    private static boolean structureFollows(ModernNpcFireStormStructures.Receipt a,ModernNpcFireStormStructures.Receipt b) {
        var x=a.hit();var y=b.hit();
        if(x instanceof ModernNpcFireStormStructures.QuestHit q) {
            if(!(y instanceof ModernNpcFireStormStructures.QuestHit r)||q.id().lane()!=r.id().lane()||q.id().stage()>r.id().stage())return false;
            if(q.id().equals(r.id()))return q.kind()==r.kind()&&q.bornRevision()==r.bornRevision()&&sameTarget(x,y);
            return q.kind()==ModernQuestObjectives.Kind.ROOM_SYMBOL&&r.kind()!=ModernQuestObjectives.Kind.FINAL_GUARDIAN&&r.bornRevision()>a.revision();
        }
        return y instanceof ModernNpcFireStormStructures.TowerHit&&sameTarget(x,y);
    }
    private static boolean sameTarget(ModernNpcFireStormStructures.Hit a,ModernNpcFireStormStructures.Hit b) {
        return a.cell().equals(b.cell())&&a.maximumHp()==b.maximumHp()&&b.hpBefore()<=a.outcome().remainingHp();
    }
    private static void cell(ByteBuffer b,ModernWorldMap.Cell c){b.putInt(c==null?0:c.x()).putInt(c==null?0:c.y());}
    private static ModernWorldMap.Cell cell(ByteBuffer b){return new ModernWorldMap.Cell(b.getInt(),b.getInt());}
    private static void outcome(ByteBuffer b,ModernFireStorm.Outcome o){b.putLong(o.rawDamage()).putLong(o.appliedDamage()).putLong(o.remainingHp());}
    private static ModernFireStorm.Outcome outcome(long raw,long applied,long remaining,int flags){if(flags>1)throw bad();return new ModernFireStorm.Outcome(raw,applied,remaining,flags==1);}
    private static void require(ByteBuffer b,int v){if(v<VERSION||v>ModernWorldProtocol.CODEC_MAX_VERSION||b.order()!=ByteOrder.BIG_ENDIAN)throw bad();}
    private static void ordered(long id,long previous){if(id<=previous||id>MAX_ACTORS)throw bad();}
    private static void count(int count,int actors){if(actors<1||actors>MAX_ACTORS||count<0||count>MAX_ACTORS||count!=0&&count!=actors)throw bad();}
    private static void zeroes(ByteBuffer b,int n){while(n-->0)b.put((byte)0);}
    private static void zero(ByteBuffer b,int n){while(n-->0)if(b.get()!=0)throw bad();}
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid NPC fire storm wire section");}
    private ModernNpcFireStormWire(){}
}
