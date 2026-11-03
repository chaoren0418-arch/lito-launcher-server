package local.lito381.server;

import java.nio.ByteBuffer;
import java.util.Objects;

/** Version-seven allow-listed server-bot command. Capability is not a permission grant. */
final class ModernGmCommandProtocol {
    static final int REQUEST_BYTES = 44;
    static final int RESULT_PREFIX_BYTES = 56;
    static final int MAX_FRAME_BYTES = ModernRoomProtocol.MAX_FRAME_BYTES + 64;
    private ModernGmCommandProtocol() {}

    record Request(ModernRooms.Scope scope, long roomId, long expectedRevision, int count) {
        Request {
            Objects.requireNonNull(scope);
            if (roomId <= 0 || expectedRevision < 1 || count < 0 || count > ModernGmCommands.MAX_REQUESTED_BOTS)
                throw bad();
        }
    }
    enum Delivery { RESULT, BUSY, UNAVAILABLE }
    record Reply(Delivery delivery, ModernRooms.BotResult result) {
        Reply {
            Objects.requireNonNull(delivery);
            if ((delivery == Delivery.RESULT) != (result != null)) throw bad();
        }
        static Reply result(ModernRooms.BotResult result) { return new Reply(Delivery.RESULT,Objects.requireNonNull(result)); }
        static Reply busy() { return new Reply(Delivery.BUSY,null); }
        static Reply unavailable() { return new Reply(Delivery.UNAVAILABLE,null); }
    }

    static void putRequest(ByteBuffer out, Request request) {
        var scope = request.scope();
        out.putInt(scope.sourceId()).putLong(scope.objectId()).putLong(scope.epoch()).put((byte)scope.channelId())
                .put((byte)0).putShort((short)0).putLong(request.roomId()).putLong(request.expectedRevision())
                .putShort((short)request.count()).putShort((short)0);
    }
    static Request getRequest(ByteBuffer in) {
        int source = in.getInt(); long object = in.getLong(), epoch = in.getLong(); int channel = Byte.toUnsignedInt(in.get());
        zero(in,3); long room = in.getLong(), revision = in.getLong(); int count = Short.toUnsignedInt(in.getShort()); zero(in,2);
        return new Request(new ModernRooms.Scope(source,object,epoch,channel),room,revision,count);
    }
    static void putResult(ByteBuffer out, Request echo, Reply reply, int version) throws ModernApplicationProtocol.ProtocolException {
        validate(echo,reply,version);
        putRequest(out,echo);
        out.put((byte)reply.delivery().ordinal()).put((byte)0).putShort((short)0);
        var result = reply.result();
        out.putShort((short)(result == null ? 0 : result.added())).putShort((short)(result == null ? 0 : result.removed()));
        int lengthPosition = out.position(); out.putInt(0); int start = out.position();
        if (result != null) ModernRoomProtocol.putBotResult(out,echo.scope(),result.result(),version);
        int length = out.position()-start;
        if (length > ModernRoomProtocol.MAX_FRAME_BYTES-ModernApplicationProtocol.HEADER_BYTES) throw new ModernApplicationProtocol.ProtocolException();
        out.putInt(lengthPosition,length);
    }
    static ModernApplicationProtocol.GmResult getResult(ByteBuffer in, int requestId, int version)
            throws ModernApplicationProtocol.ProtocolException {
        Request echo = getRequest(in);
        int delivery = Byte.toUnsignedInt(in.get()); zero(in,3);
        if (delivery >= Delivery.values().length) throw bad();
        int added = Short.toUnsignedInt(in.getShort()), removed = Short.toUnsignedInt(in.getShort());
        int length = in.getInt();
        if (length < 0 || length > ModernRoomProtocol.MAX_FRAME_BYTES-ModernApplicationProtocol.HEADER_BYTES
                || length > in.remaining()) throw bad();
        ModernRooms.BotResult result = null;
        if (delivery == Delivery.RESULT.ordinal()) {
            if (length == 0) throw bad();
            ByteBuffer payload = in.slice().order(in.order()); payload.limit(length);
            var rooms = ModernRoomProtocol.getBotResult(payload,echo.scope(),version);
            if (payload.hasRemaining()) throw bad();
            in.position(in.position()+length);
            result = new ModernRooms.BotResult(echo.count(),added,removed,rooms);
        } else if (length != 0 || added != 0 || removed != 0) throw bad();
        Reply reply = new Reply(Delivery.values()[delivery],result);
        validate(echo,reply,version);
        return new ModernApplicationProtocol.GmResult(requestId,echo,reply);
    }
    static void validate(Request echo, Reply reply, int version) throws ModernApplicationProtocol.ProtocolException {
        if (version < 7 || echo == null || reply == null) throw new ModernApplicationProtocol.ProtocolException();
        var result = reply.result();
        if (result == null) return;
        if (result.requested() != echo.count() || result.added() < 0 || result.added() > echo.count()
                || result.removed() < 0 || result.removed() > 100
                || echo.count() == 0 && result.added() != 0 || echo.count() > 0 && result.removed() != 0
                || result.result().status() != ModernRooms.Status.OK && (result.added() != 0 || result.removed() != 0))
            throw new ModernApplicationProtocol.ProtocolException();
        if (result.result().status() == ModernRooms.Status.OK) {
            var joined = result.result().view().joined();
            long revision;
            try { revision = Math.addExact(echo.expectedRevision(),result.added() != 0 || result.removed() != 0 ? 1 : 0); }
            catch (ArithmeticException failure) { throw new ModernApplicationProtocol.ProtocolException(); }
            if (joined == null || joined.summary().id() != echo.roomId() || joined.summary().revision() != revision
                    || echo.count() == 0 && joined.members().stream().anyMatch(member -> !member.identity().isHuman()))
                throw new ModernApplicationProtocol.ProtocolException();
        }
    }
    private static void zero(ByteBuffer in, int count) { for (int i=0;i<count;i++) if(in.get()!=0)throw bad(); }
    private static IllegalArgumentException bad() { return new IllegalArgumentException("invalid modern GM command frame"); }
}
