package local.lito381.server;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** v4 source-owned skills and targetless server-authoritative same-map teleport. */
final class ModernSpellProtocol {
    static final int CAST_REQUEST_BYTES = 56;
    enum Status { OK, DENIED, BUSY, UNAVAILABLE }
    enum Reason { NONE, NOT_ACTIVE, DEAD, UNLEARNED, UNSUPPORTED, TOO_FAST, NO_MP, NO_DESTINATION, NUMERIC,
        TARGET_DEAD, SAME_TEAM, OUT_OF_RANGE, BLOCKED, TOO_HEAVY, STALE_EFFECT, STALE_REGEN }
    record Context(int sourceId, long objectId, long epoch) {
        Context { if (sourceId < 1 || objectId < 1 || epoch == 0) throw bad(); }
    }
    record SkillsReply(Status status, List<ModernSkills.Skill> skills) {
        SkillsReply {
            Objects.requireNonNull(status); skills = List.copyOf(skills);
            if (skills.size() > ModernSkills.MAX_SKILLS || status != Status.OK && !skills.isEmpty()) throw bad();
            int previous = 0;
            for (var skill : skills) { if (skill.skillId() <= previous) throw bad(); previous = skill.skillId(); }
        }
    }
    record Request(ModernRooms.Scope scope, long matchId, long generation, long sequence, int skillId, long targetEntityId) {
        Request(ModernRooms.Scope scope,long matchId,long generation,long sequence,int skillId) {
            this(scope,matchId,generation,sequence,skillId,0);
        }
        Request {
            Objects.requireNonNull(scope);
            if (matchId < 1 || generation < 1 || sequence < 1 || skillId < 1 || skillId > ModernSkills.MAX_SKILLS
                    || targetEntityId < 0 || skillId != 4 && skillId!=1&&targetEntityId != 0) throw bad();
        }
    }
    record CastState(long entityId, long requestSequence, long castCount, long castRevision, long mpSpent,
                     int skillId, int fromX, int fromY, int toX, int toY, int castIntervalMillis) {
        CastState {
            if (entityId < 1 || castCount < 0 || requestSequence < castCount || castRevision < 0 || mpSpent < 0
                    || castIntervalMillis < 10 || castIntervalMillis > 10_000) throw bad();
            if (castCount == 0) {
                if (castRevision != 0 || mpSpent != 0 || skillId != 0 || fromX != 0 || fromY != 0 || toX != 0 || toY != 0) throw bad();
            } else if (skillId != 5 || castRevision == 0 || Math.multiplyExact(castCount, 10) != mpSpent
                    || fromX == toX && fromY == toY) throw bad();
        }
        static CastState initial(long entityId, int castIntervalMillis) {
            return new CastState(entityId, 0, 0, 0, 0, 0, 0, 0, 0, 0, castIntervalMillis);
        }
        CastState wireRevision() {
            return new CastState(entityId, requestSequence, castCount, castRevision == 0 ? 0 : Math.addExact(castRevision, 1),
                    mpSpent, skillId, fromX, fromY, toX, toY, castIntervalMillis);
        }
    }
    record Reply(Status status, Reason reason, ModernCombatProtocol.Frame frame) {
        Reply {
            Objects.requireNonNull(status); Objects.requireNonNull(reason);
            if ((status == Status.OK) != (reason == Reason.NONE)
                    || frame == null && !(status == Status.UNAVAILABLE && reason == Reason.NOT_ACTIVE)) throw bad();
        }
    }
    private ModernSpellProtocol() {}
    static void validate(ModernApplicationProtocol.CastResult result) throws ModernApplicationProtocol.ProtocolException {
        if (result.echo() == null || result.reply() == null) fail();
        var request = result.echo(); var reply = result.reply();
        if (reply.frame() == null) return;
        var frame = reply.frame(); var world = frame.world();
        if (world.matchId() != request.matchId() || world.generation() != request.generation()
                || world.casts().size() != world.actors().size()) fail();
        int self = -1;
        for (int i = 0; i < world.actors().size(); i++) {
            var key = world.actors().get(i).member().identity();
            if (key.equals(ModernParticipantId.human(request.scope().sourceId(),request.scope().objectId()))) self = i;
        }
        if (self < 0) fail();
        var cast = world.casts().get(self); var actor = world.actors().get(self);
        if (reply.reason() == Reason.TOO_HEAVY) {
            // The authority checks carried weight before learned/template support.
            // Do not invent a narrower skill allowlist for this denial.
            if (reply.status() != Status.DENIED || world.phase() != ModernWorldProtocol.Phase.ACTIVE
                    || frame.vitals().get(self).hp() == 0 || frame.weights().isEmpty() || !frame.weights().get(self).snapshot().view().applicable()
                    || frame.weights().get(self).snapshot().view().permitsAttackCast()) fail();
        }
        if(reply.reason()==Reason.STALE_EFFECT && (reply.status()!=Status.DENIED || world.phase()!=ModernWorldProtocol.Phase.ACTIVE
                || frame.buffs().isEmpty() || frame.buffs().get(self).revision()==0 || frame.vitals().get(self).hp()==0
                || !(request.skillId()==5 && request.targetEntityId()==0 || request.skillId()==4 && request.targetEntityId()>0
                    ||request.skillId()==1&&frame.honor().objective(request.targetEntityId())!=null
                    ||request.skillId()==70&&request.targetEntityId()==0&&!frame.fireStorms().isEmpty())))fail();
        if(reply.reason()==Reason.STALE_REGEN) {
            if(reply.status()!=Status.DENIED||world.phase()!=ModernWorldProtocol.Phase.ACTIVE||frame.regen().isEmpty()
                    ||frame.vitals().get(self).hp()==0||cast.requestSequence()!=request.sequence())fail();
            if(request.skillId()==5&&request.targetEntityId()==0){if(frame.regen().get(self).mpRevision()==0)fail();}
            else if(request.skillId()==70&&request.targetEntityId()==0&&!frame.fireStorms().isEmpty()) {
                boolean proof=frame.regen().get(self).mpRevision()>0;
                for(int i=0;i<world.actors().size();i++){var target=world.actors().get(i);
                    if(target.member().team()!=actor.member().team()&&frame.vitals().get(i).hp()>0&&frame.regen().get(i).hpRevision()>0
                            &&Math.max(Math.abs((long)target.position().x()-actor.position().x()),Math.abs((long)target.position().y()-actor.position().y()))<=3)proof=true;}
                if(!proof)fail();
            }
            else if((request.skillId()==4||request.skillId()==1)&&frame.honor().objective(request.targetEntityId())!=null) {
                var target=frame.honor().objective(request.targetEntityId());
                if(target.hp()==0||frame.regen().get(self).mpRevision()==0||(request.skillId()==1)!=(actor.member().team()==target.team()))fail();
            }else if(request.skillId()==4&&request.targetEntityId()>0) {
                int target=-1;for(int i=0;i<world.actors().size();i++)if(world.actors().get(i).entityId()==request.targetEntityId())target=i;
                if(target<0||target==self||frame.vitals().get(target).hp()==0
                        ||frame.regen().get(self).mpRevision()==0&&frame.regen().get(target).hpRevision()==0)fail();
            }else fail();
        }
        if (reply.status() == Status.OK) {
            if(request.skillId()==70) {
                if(request.targetEntityId()!=0||frame.fireStorms().size()!=world.actors().size())fail();var fire=frame.fireStorms().get(self);
                if(world.phase()!=ModernWorldProtocol.Phase.ACTIVE&&world.phase()!=ModernWorldProtocol.Phase.FINISHED
                        ||fire.castRevision()!=world.revision()||fire.lastCastSequence()!=request.sequence()||cast.requestSequence()!=request.sequence())fail();
            }else if(request.skillId()==1&&frame.honor().enabled()) {
                var heal=frame.honor().heals().get(self);var target=frame.honor().objective(request.targetEntityId());
                if(world.phase()!=ModernWorldProtocol.Phase.ACTIVE||cast.requestSequence()!=request.sequence()||target==null
                        ||heal.revision()!=world.revision()||heal.targetEntityId()!=request.targetEntityId()||heal.skillId()!=1
                        ||actor.position().x()!=heal.fromX()||actor.position().y()!=heal.fromY())fail();
            }else if(request.targetEntityId()>0) {
                if(frame.damages().size()!=world.actors().size())fail();
                var damage=frame.damages().get(self);
                if((world.phase()!=ModernWorldProtocol.Phase.ACTIVE && world.phase()!=ModernWorldProtocol.Phase.FINISHED)
                        ||cast.requestSequence()!=request.sequence() ||damage.castRevision()!=world.revision()
                        ||damage.skillId()!=request.skillId() ||damage.targetEntityId()!=request.targetEntityId()
                        ||actor.position().x()!=damage.fromX() ||actor.position().y()!=damage.fromY())fail();
                var target=world.actors().stream().filter(a->a.entityId()==request.targetEntityId()).findFirst();
                var objective=frame.honor().objective(request.targetEntityId());
                if(objective==null?(target.isEmpty() || target.get().position().x()!=damage.toX() || target.get().position().y()!=damage.toY()):
                        objective.x()!=damage.toX()||objective.y()!=damage.toY())fail();
            } else if (world.phase() != ModernWorldProtocol.Phase.ACTIVE || cast.requestSequence() != request.sequence()
                    || cast.castRevision() != world.revision() || cast.skillId() != request.skillId()
                    || actor.position().x() != cast.toX() || actor.position().y() != cast.toY()) fail();
        } else if (world.phase() != ModernWorldProtocol.Phase.FINISHED && world.phase() != ModernWorldProtocol.Phase.ABORTED
                && cast.requestSequence() != request.sequence()) fail();
    }
    static void putContext(ByteBuffer out, Context context) {
        out.putInt(context.sourceId()).putLong(context.objectId()).putLong(context.epoch());
    }
    static Context getContext(ByteBuffer in) { return new Context(in.getInt(), in.getLong(), in.getLong()); }
    static void putSkills(ByteBuffer out, ModernApplicationProtocol.SkillsResult result) {
        putSkills(out,result,4);
    }
    static void putSkills(ByteBuffer out, ModernApplicationProtocol.SkillsResult result,int version) {
        putContext(out, result.echo()); var reply = result.reply();
        out.put((byte) reply.status().ordinal()).put((byte) 0).putShort((short) 0);
        if (reply.status() != Status.OK) return;
        out.putShort((short) reply.skills().size()).putShort((short) 0);
        for (var skill : reply.skills()) {
            byte[] name = skill.nameToken().getBytes(StandardCharsets.UTF_8);
            // v4/v5 only defined UNSUPPORTED_TEMPLATE for teleport. Both a
            // qualified and an invalid Light Arrow tuple remain unimplemented
            // on those peers; do not leak a new meaning for old ordinal 2.
            int support=skill.skillId()==4 && version<6?ModernSkills.Support.UNIMPLEMENTED.ordinal():skill.support().ordinal();
            if(skill.skillId()==1&&version<13)support=ModernSkills.Support.UNIMPLEMENTED.ordinal();
            if(skill.skillId()==70&&version<14)support=ModernSkills.Support.UNIMPLEMENTED.ordinal();
            if(skill.skillId()==77&&version<27)support=ModernSkills.Support.UNIMPLEMENTED.ordinal();
            if((skill.skillId()==54&&version<42)||(skill.skillId()==43&&version<44)||(skill.skillId()==29&&version<47))
                support=ModernSkills.Support.UNIMPLEMENTED.ordinal();
            out.putInt(skill.skillId()).put((byte) support).put((byte) 0).putShort((short) 0)
                    .putLong(skill.mpCost()).putLong(skill.reuseMillis()).putShort((short) name.length).put(name);
        }
    }
    static ModernApplicationProtocol.SkillsResult getSkills(ByteBuffer in, int id) throws ModernApplicationProtocol.ProtocolException {
        return getSkills(in,id,4);
    }
    static ModernApplicationProtocol.SkillsResult getSkills(ByteBuffer in, int id,int version) throws ModernApplicationProtocol.ProtocolException {
        var context = getContext(in); var status = at(Status.values(), u8(in)); zero(in, 3);
        var skills = new ArrayList<ModernSkills.Skill>();
        if (status == Status.OK) {
            int count = Short.toUnsignedInt(in.getShort()); zero(in, 2);
            if (count > ModernSkills.MAX_SKILLS || in.remaining() < count * 27) fail();
            for (int i = 0; i < count; i++) {
                int skillId = in.getInt(); var support = at(ModernSkills.Support.values(), u8(in)); zero(in, 3);
                if(version<6 && (support==ModernSkills.Support.LIGHT_ARROW
                        || skillId==4 && support==ModernSkills.Support.UNSUPPORTED_TEMPLATE))fail();
                if(version<13&&(support==ModernSkills.Support.BASIC_HEAL||skillId==1&&support==ModernSkills.Support.UNSUPPORTED_TEMPLATE))fail();
                if(version<14&&(support==ModernSkills.Support.FIRE_STORM||skillId==70&&support==ModernSkills.Support.UNSUPPORTED_TEMPLATE))fail();
                if(version<27&&(support==ModernSkills.Support.DISINTEGRATE||skillId==77&&support==ModernSkills.Support.UNSUPPORTED_TEMPLATE))fail();
                if(version<42&&(support==ModernSkills.Support.GREATER_HASTE||skillId==54&&support==ModernSkills.Support.UNSUPPORTED_TEMPLATE))fail();
                if(version<44&&(support==ModernSkills.Support.HASTE||skillId==43&&support==ModernSkills.Support.UNSUPPORTED_TEMPLATE))fail();
                if(version<47&&(support==ModernSkills.Support.SLOW||skillId==29&&support==ModernSkills.Support.UNSUPPORTED_TEMPLATE))fail();
                long mp = in.getLong(), reuse = in.getLong(); int size = Short.toUnsignedInt(in.getShort());
                if (size < 1 || size > 256 || size > in.remaining()) fail();
                var slice = in.slice(); slice.limit(size); String name;
                try { name = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(slice).toString(); }
                catch (java.nio.charset.CharacterCodingException invalid) { throw new ModernApplicationProtocol.ProtocolException(); }
                in.position(in.position() + size); skills.add(new ModernSkills.Skill(skillId, name, mp, reuse, support));
            }
        }
        return new ModernApplicationProtocol.SkillsResult(id, context, new SkillsReply(status, skills));
    }
    static void putRequest(ByteBuffer out, Request request) {
        putRequestFields(out,request);
        if(request.targetEntityId()!=0)throw bad();
    }
    static void putRequest(ByteBuffer out, Request request,int version) throws ModernApplicationProtocol.ProtocolException {
        if(request.skillId()==1&&request.targetEntityId()>0&&version<13||request.skillId()==1&&version>=13&&request.targetEntityId()==0)fail();
        if(version>=6 ? request.skillId()==4 && request.targetEntityId()==0 : request.targetEntityId()!=0)fail();
        putRequestFields(out,request);
        if(version>=6)out.putLong(request.targetEntityId());
    }
    private static void putRequestFields(ByteBuffer out,Request request) {
        var s = request.scope(); putContext(out, new Context(s.sourceId(), s.objectId(), s.epoch()));
        out.put((byte) s.channelId()).put((byte) 0).putShort((short) 0).putLong(request.matchId())
                .putLong(request.generation()).putLong(request.sequence()).putInt(request.skillId()).putInt(0);
    }
    static Request getRequest(ByteBuffer in) throws ModernApplicationProtocol.ProtocolException {
        return getRequest(in,4);
    }
    static Request getRequest(ByteBuffer in,int version) throws ModernApplicationProtocol.ProtocolException {
        var context = getContext(in); int channel = u8(in); zero(in, 3);
        long match=in.getLong(),generation=in.getLong(),sequence=in.getLong();int skill=in.getInt();zero(in,4);
        long target=version>=6?in.getLong():0;
        if(skill==1&&target>0&&version<13||skill==1&&version>=13&&target==0)fail();
        if(version>=6 && skill==4 && target==0)fail();
        return new Request(new ModernRooms.Scope(context.sourceId(), context.objectId(), context.epoch(), channel),match,generation,sequence,skill,target);
    }
    static void putResult(ByteBuffer out, ModernApplicationProtocol.CastResult result, int version) throws ModernApplicationProtocol.ProtocolException {
        if (version < 6 && result.reply().reason().ordinal() > Reason.NUMERIC.ordinal()) fail();
        if (version < 9 && result.reply().reason() == Reason.TOO_HEAVY) fail();
        if (version < 10 && result.reply().reason() == Reason.STALE_EFFECT) fail();
        if(version<12&&result.reply().reason()==Reason.STALE_REGEN)fail();
        putRequest(out, result.echo(),version); var reply = result.reply();
        out.put((byte) reply.status().ordinal()).put((byte) reply.reason().ordinal())
                .put((byte) (reply.frame() == null ? 0 : 1)).put((byte) 0);
        if (reply.frame() != null) ModernCombatProtocol.putFrame(out, reply.frame(), version);
    }
    static ModernApplicationProtocol.CastResult getResult(ByteBuffer in, int id, int version) throws ModernApplicationProtocol.ProtocolException {
        var echo = getRequest(in,version); var status = at(Status.values(), u8(in)); var reason = at(Reason.values(), u8(in));
        if(version<6 && reason.ordinal()>Reason.NUMERIC.ordinal())fail();
        if(version<9 && reason==Reason.TOO_HEAVY)fail();
        if(version<10 && reason==Reason.STALE_EFFECT)fail();
        if(version<12&&reason==Reason.STALE_REGEN)fail();
        int present = u8(in); if (present > 1) fail(); zero(in, 1);
        return new ModernApplicationProtocol.CastResult(id, echo,
                new Reply(status, reason, present == 0 ? null : ModernCombatProtocol.getFrame(in, version)));
    }
    static void putStates(ByteBuffer out, List<CastState> casts) {
        out.putShort((short) casts.size()).putShort((short) 0);
        for (var c : casts) out.putLong(c.entityId()).putLong(c.requestSequence()).putLong(c.castCount()).putLong(c.castRevision())
                .putLong(c.mpSpent()).putInt(c.skillId()).putInt(c.fromX()).putInt(c.fromY()).putInt(c.toX()).putInt(c.toY()).putInt(c.castIntervalMillis());
    }
    static List<CastState> getStates(ByteBuffer in, int actorCount) throws ModernApplicationProtocol.ProtocolException {
        int count = Short.toUnsignedInt(in.getShort()); zero(in, 2);
        if (count != 0 && count != actorCount || count > 100 || in.remaining() < count * 64) fail();
        var casts = new ArrayList<CastState>(count);
        for (int i = 0; i < count; i++) {
            casts.add(new CastState(in.getLong(), in.getLong(), in.getLong(), in.getLong(), in.getLong(),
                    in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt(), in.getInt()));
        }
        return casts;
    }
    private static int u8(ByteBuffer in) { return Byte.toUnsignedInt(in.get()); }
    private static void zero(ByteBuffer in, int count) throws ModernApplicationProtocol.ProtocolException { for (int i = 0; i < count; i++) if (in.get() != 0) fail(); }
    private static <T> T at(T[] values, int i) throws ModernApplicationProtocol.ProtocolException { if (i >= values.length) fail(); return values[i]; }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern spell value"); }
    private static void fail() throws ModernApplicationProtocol.ProtocolException { throw new ModernApplicationProtocol.ProtocolException(); }
}
