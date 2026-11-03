package local.lito381.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/** Strict post-HELLO LTM1 codec. This does not widen the separate 32-byte HELLO codec. */
final class ModernApplicationProtocol {
    static final int HEADER_BYTES = 16;
    static final int MAX_FRAME_BYTES = 65_536;
    static final int MAX_ROWS = 64;
    private ModernApplicationProtocol() {}

    sealed interface Message extends AutoCloseable permits Auth, AuthResult, CharacterRequest,
            CharacterResult, Ping, Pong, CharacterSelection, ChannelEnter, CharacterReturn, LobbyLeave, NavigationResult,
            CharacterDataRequest, CharacterDataResult, EquipmentRequest, EquipmentResult, RoomRequest, RoomResult, WorldRequest, WorldResult, NativeWorldRequest, NativeWorldResult, NativeCanonicalWorldRequest, NativeCanonicalWorldResult, NativeCatalogWorldRequest, NativeCatalogWorldResult, NativeActorWorldRequest, NativeActorWorldResult, CombatRequest, CombatResult,
            SkillsRequest, SkillsResult, CastRequest, CastResult, RecordRequest, RecordResult, GmRequest, GmResult, BattleEquipmentRequest, BattleEquipmentResult, GreaterHasteRequest, GreaterHasteResult, HasteRequest, HasteResult, SlowRequest, SlowResult, PlayerHealingRequest, PlayerHealingResult, VipEquipmentRequest, VipEquipmentResult, VipWorldRequest, VipWorldResult,
            ShopRequest, ShopResult, ItemRequest, ItemResult, AmmoRequest, AmmoResult, NpcRequest, NpcResult, NpcSpellRequest, NpcSpellResult, SiegeRequest, SiegeResult, DisintegrateRequest, DisintegrateResult, NpcDisintegrateRequest, NpcDisintegrateResult, NpcFireStormRequest, NpcFireStormResult, ChatRequest, ChatResult, DirectChatRequest, DirectChatResult, FilteredChatRequest, FilteredChatResult, ModeratedDirectRequest, ModeratedDirectResult, MatchmakingRequest, MatchmakingResult, BattleReadRequest, BattleReadResult, BattleActionRequest, BattleActionResult, BattleOperationRequest, BattleOperationResult, BattleWorldRequest, BattleWorldResult, BattleRevivalRequest, BattleRevivalResult {
        int requestId();
        @Override default void close() {}
    }

    /** Owns a private password copy; never exposes it through toString. Close after use. */
    static final class Auth implements Message {
        private final int requestId;
        private final String account;
        private final char[] password;
        Auth(int requestId, String account, char[] password) {
            this.requestId = requestId;
            this.account = account;
            this.password = password == null ? null : password.clone();
        }
        @Override public int requestId() { return requestId; }
        String account() { return account; }
        /** Borrowed until close, must not be retained or logged. */
        char[] password() { return password; }
        @Override public void close() { ModernAccountAuthentication.clear(password); }
        @Override public String toString() { return "Auth[credentials redacted]"; }
    }
    record Source(int id, String name) {}
    record CharacterRow(long objectId, String name, int characterClass, int sex, long level) {}
    record AuthResult(int requestId, int status, String account, List<Source> sources) implements Message {
        AuthResult { sources = List.copyOf(sources); }
    }
    record CharacterRequest(int requestId, int sourceId, long selectionEpoch) implements Message {}
    record CharacterResult(int requestId, int sourceId, long selectionEpoch, int status,
                           List<CharacterRow> characters) implements Message {
        CharacterResult { characters = List.copyOf(characters); }
    }
    record Ping(int requestId) implements Message {}
    record Pong(int requestId) implements Message {}
    record CharacterSelection(int requestId, int sourceId, long objectId, long selectionEpoch) implements Message {}
    record ChannelEnter(int requestId, int sourceId, long objectId, long selectionEpoch, int channelId) implements Message {}
    record CharacterReturn(int requestId, int sourceId, long objectId, long selectionEpoch) implements Message {}
    record LobbyLeave(int requestId, int sourceId, long objectId, long selectionEpoch) implements Message {}
    record CharacterDataRequest(int requestId, int sourceId, long objectId, long selectionEpoch) implements Message {}
    record CharacterDataResult(int requestId, int sourceId, long objectId, long selectionEpoch,
                               int status, ModernCharacterData.Snapshot snapshot) implements Message {}
    record EquipmentRequest(int requestId, ModernEquipmentAuthority.Request request) implements Message {
        @Override public String toString() { return "EquipmentRequest[redacted]"; }
    }
    record EquipmentResult(int requestId, ModernEquipmentAuthority.Request echo,
                           ModernEquipmentAuthority.Result result) implements Message {
        @Override public String toString() { return "EquipmentResult[redacted]"; }
    }
    record ChannelRow(int id, String name, int population, int capacity, long minimumLevel, long maximumLevel) {}
    record WorldRequest(int requestId, ModernWorldProtocol.Request request) implements Message {}
    record NativeWorldRequest(ModernWorldResourceProtocol.Request request) implements Message {
        NativeWorldRequest {java.util.Objects.requireNonNull(request);}
        @Override public int requestId(){return request.requestId();}
    }
    record NativeWorldResult(ModernWorldResourceProtocol.Result result) implements Message {
        NativeWorldResult {java.util.Objects.requireNonNull(result);}
        @Override public int requestId(){return result.echo().requestId();}
    }
    record NativeCanonicalWorldRequest(ModernNativeCanonicalWorldProtocol.Request request) implements Message {
        NativeCanonicalWorldRequest {java.util.Objects.requireNonNull(request);}
        @Override public int requestId(){return request.requestId();}
    }
    record NativeActorWorldRequest(ModernNativeActorWorldProtocol.Request request) implements Message {
        NativeActorWorldRequest {java.util.Objects.requireNonNull(request);}
        @Override public int requestId(){return request.requestId();}
    }
    record NativeActorWorldResult(ModernNativeActorWorldProtocol.Result result) implements Message {
        NativeActorWorldResult {java.util.Objects.requireNonNull(result);}
        @Override public int requestId(){return result.echo().requestId();}
    }
    record NativeCatalogWorldRequest(ModernNativeCatalogWorldProtocol.Request request) implements Message {
        NativeCatalogWorldRequest {java.util.Objects.requireNonNull(request);}
        @Override public int requestId(){return request.requestId();}
    }
    record NativeCatalogWorldResult(ModernNativeCatalogWorldProtocol.Result result) implements Message {
        NativeCatalogWorldResult {java.util.Objects.requireNonNull(result);}
        @Override public int requestId(){return result.echo().requestId();}
    }
    record NativeCanonicalWorldResult(ModernNativeCanonicalWorldProtocol.Result result) implements Message {
        NativeCanonicalWorldResult {java.util.Objects.requireNonNull(result);}
        @Override public int requestId(){return result.echo().requestId();}
    }
    record VipWorldRequest(int requestId,ModernBattleWorldProtocol.Request request) implements Message {
        VipWorldRequest {java.util.Objects.requireNonNull(request);if(requestId==0)throw new IllegalArgumentException("zero VIP lifecycle id");}
        @Override public String toString(){return "VipWorldRequest[redacted]";}
    }
    record VipWorldResult(int requestId,ModernBattleWorldProtocol.Request echo,ModernWorldService.VipWorldReply reply) implements Message {
        VipWorldResult {java.util.Objects.requireNonNull(echo);java.util.Objects.requireNonNull(reply);}
        @Override public String toString(){return "VipWorldResult[redacted]";}
    }
    record VipEquipmentRequest(ModernVipEquipmentProtocol.Request request) implements Message {
        VipEquipmentRequest {java.util.Objects.requireNonNull(request);}
        @Override public int requestId(){return request.requestId();}
        @Override public String toString(){return "VipEquipmentRequest[redacted]";}
    }
    record VipEquipmentResult(ModernVipEquipmentProtocol.Reply reply) implements Message {
        VipEquipmentResult {java.util.Objects.requireNonNull(reply);}
        @Override public int requestId(){return reply.echo().requestId();}
        @Override public String toString(){return "VipEquipmentResult[redacted]";}
    }
    record PlayerHealingRequest(ModernPlayerHealingProtocol.Request request) implements Message {
        PlayerHealingRequest {java.util.Objects.requireNonNull(request);}
        public int requestId(){return request.requestId();}
        @Override public String toString(){return "PlayerHealingRequest[redacted]";}
    }
    record PlayerHealingResult(ModernPlayerHealingProtocol.Reply reply) implements Message {
        PlayerHealingResult {java.util.Objects.requireNonNull(reply);}
        public int requestId(){return reply.echo().requestId();}
        @Override public String toString(){return "PlayerHealingResult[redacted]";}
    }
    record SlowRequest(ModernSlowProtocol.Request request) implements Message {
        SlowRequest {java.util.Objects.requireNonNull(request);}
        public int requestId(){return request.requestId();}
        @Override public String toString(){return "SlowRequest[redacted]";}
    }
    record SlowResult(ModernSlowProtocol.Reply reply) implements Message {
        SlowResult {java.util.Objects.requireNonNull(reply);}
        public int requestId(){return reply.echo().requestId();}
        @Override public String toString(){return "SlowResult[redacted]";}
    }
    record HasteRequest(ModernHasteProtocol.Request request) implements Message {
        HasteRequest {java.util.Objects.requireNonNull(request);}
        public int requestId(){return request.requestId();}
        @Override public String toString(){return "HasteRequest[redacted]";}
    }
    record GreaterHasteRequest(ModernGreaterHasteProtocol.Request request) implements Message {
        GreaterHasteRequest {java.util.Objects.requireNonNull(request);}
        public int requestId(){return request.requestId();}
        @Override public String toString(){return "GreaterHasteRequest[redacted]";}
    }
    record HasteResult(ModernHasteProtocol.Reply reply) implements Message {
        HasteResult {java.util.Objects.requireNonNull(reply);}
        public int requestId(){return reply.echo().requestId();}
        @Override public String toString(){return "HasteResult[redacted]";}
    }
    record GreaterHasteResult(ModernGreaterHasteProtocol.Reply reply) implements Message {
        GreaterHasteResult {java.util.Objects.requireNonNull(reply);}
        public int requestId(){return reply.echo().requestId();}
        @Override public String toString(){return "GreaterHasteResult[redacted]";}
    }
    record BattleEquipmentRequest(int requestId,ModernBattleEquipmentProtocol.Request request) implements Message {
        BattleEquipmentRequest {java.util.Objects.requireNonNull(request);if(requestId==0)throw new IllegalArgumentException("zero equipment id");}
        @Override public String toString(){return "BattleEquipmentRequest[redacted]";}
    }
    record BattleEquipmentResult(int requestId,ModernBattleEquipmentProtocol.Request echo,ModernBattleEquipmentProtocol.Reply reply) implements Message {
        BattleEquipmentResult {java.util.Objects.requireNonNull(echo);java.util.Objects.requireNonNull(reply);}
        @Override public String toString(){return "BattleEquipmentResult[redacted]";}
    }
    record BattleRevivalRequest(int requestId,ModernBattleRevivalProtocol.Request request) implements Message {
        BattleRevivalRequest {java.util.Objects.requireNonNull(request);if(requestId==0)throw new IllegalArgumentException("zero revival id");}
        @Override public String toString(){return "BattleRevivalRequest[redacted]";}
    }
    record BattleRevivalResult(int requestId,ModernBattleRevivalProtocol.Request echo,ModernBattleRevivalProtocol.Reply reply) implements Message {
        BattleRevivalResult {java.util.Objects.requireNonNull(echo);java.util.Objects.requireNonNull(reply);}
        @Override public String toString(){return "BattleRevivalResult[redacted]";}
    }
    record BattleWorldRequest(int requestId,ModernBattleWorldProtocol.Request request) implements Message {
        BattleWorldRequest {java.util.Objects.requireNonNull(request);if(requestId==0)throw new IllegalArgumentException("zero lifecycle id");}
        @Override public String toString(){return "BattleWorldRequest[redacted]";}
    }
    record BattleWorldResult(int requestId,ModernBattleWorldProtocol.Request echo,ModernBattleWorldProtocol.Reply reply) implements Message {
        BattleWorldResult {java.util.Objects.requireNonNull(echo);java.util.Objects.requireNonNull(reply);}
        @Override public String toString(){return "BattleWorldResult[redacted]";}
    }
    record BattleReadRequest(int requestId,ModernBattleReadProtocol.Request request) implements Message {
        @Override public String toString(){return "BattleReadRequest[redacted]";}
    }
    record BattleOperationRequest(int requestId,ModernBattleActionProtocol.Request request) implements Message {
        BattleOperationRequest {java.util.Objects.requireNonNull(request);if(requestId==0||requestId!=request.command().requestId())throw new IllegalArgumentException("invalid battle operation id");}
        @Override public String toString(){return "BattleOperationRequest[redacted]";}
    }
    record BattleOperationResult(int requestId,ModernBattleActionProtocol.Request echo,ModernBattleActionReceipt receipt) implements Message {
        BattleOperationResult {java.util.Objects.requireNonNull(echo);java.util.Objects.requireNonNull(receipt);}
        @Override public String toString(){return "BattleOperationResult[redacted]";}
    }
    record BattleReadResult(int requestId,ModernBattleReadProtocol.Request echo,ModernBattleReadProtocol.Reply reply) implements Message {
        @Override public String toString(){return "BattleReadResult[redacted]";}
    }
    record WorldResult(int requestId, ModernWorldProtocol.Request echo, ModernWorldProtocol.Reply reply) implements Message {}
    record BattleActionRequest(int requestId,ModernBattleActionProtocol.Request request) implements Message {
        BattleActionRequest {java.util.Objects.requireNonNull(request);if(requestId==0||requestId!=request.command().requestId())throw new IllegalArgumentException("invalid battle action id");}
        @Override public String toString(){return "BattleActionRequest[redacted]";}
    }
    record BattleActionResult(int requestId,ModernBattleActionProtocol.Request echo,ModernBattleCommandLife.Rejection rejection,Message result) implements Message {
        BattleActionResult {java.util.Objects.requireNonNull(echo);java.util.Objects.requireNonNull(rejection);java.util.Objects.requireNonNull(result);}
        @Override public String toString(){return "BattleActionResult[redacted]";}
    }
    record CombatRequest(int requestId, ModernCombatProtocol.Request request) implements Message {}
    record CombatResult(int requestId, ModernCombatProtocol.Request echo, ModernCombatProtocol.Reply reply) implements Message {}
    record SkillsRequest(int requestId, ModernSpellProtocol.Context context) implements Message {}
    record SkillsResult(int requestId, ModernSpellProtocol.Context echo, ModernSpellProtocol.SkillsReply reply) implements Message {}
    record CastRequest(int requestId, ModernSpellProtocol.Request request) implements Message {}
    record CastResult(int requestId, ModernSpellProtocol.Request echo, ModernSpellProtocol.Reply reply) implements Message {}
    record RecordRequest(int requestId, ModernCharacterRecordProtocol.Context context) implements Message {}
    record RecordResult(int requestId, ModernCharacterRecordProtocol.Reply result) implements Message {}
    record GmRequest(int requestId,ModernGmCommandProtocol.Request request) implements Message {}
    record DirectChatRequest(int requestId,ModernDirectChatProtocol.Request request) implements Message {}
    record DirectChatResult(int requestId,ModernDirectChatProtocol.Request echo,ModernDirectChat.Result result) implements Message {}
    record ChatRequest(int requestId,ModernChatProtocol.Request request) implements Message {}
    record ChatResult(int requestId,ModernChatProtocol.Request echo,ModernChat.Result result) implements Message {}
    record FilteredChatRequest(int requestId,ModernFilteredChat.Request request) implements Message {}
    record FilteredChatResult(int requestId,ModernFilteredChat.Request echo,ModernFilteredChat.Result result) implements Message {}
    /** v33 kinds 56/57; only these private frames can represent SILENCED. */
    record ModeratedDirectRequest(int requestId,ModernDirectChatProtocol.Request request) implements Message {}
    record ModeratedDirectResult(int requestId,ModernDirectChatProtocol.Request echo,ModernDirectChat.Result result) implements Message {}
    record MatchmakingRequest(int requestId,ModernMatchmakingProtocol.Request request) implements Message {}
    record MatchmakingResult(int requestId,ModernMatchmakingProtocol.Request echo,ModernMatchmakingProtocol.Reply reply) implements Message {}
    record GmResult(int requestId,ModernGmCommandProtocol.Request echo,ModernGmCommandProtocol.Reply reply) implements Message {}
    record ShopRequest(int requestId,ModernShopProtocol.Request request) implements Message {}
    record ShopResult(int requestId,ModernShopProtocol.Request echo,ModernShopProtocol.Reply reply) implements Message {}
    record ItemRequest(int requestId,ModernItemProtocol.Request request) implements Message {}
    record ItemResult(int requestId,ModernItemProtocol.Request echo,ModernItemProtocol.Reply reply) implements Message {}
    record AmmoRequest(int requestId,ModernAmmoProtocol.Request request) implements Message {}
    record AmmoResult(int requestId,ModernAmmoProtocol.Request echo,ModernAmmoProtocol.Reply reply) implements Message {}
    record NpcRequest(int requestId,ModernNpcProtocol.Request request) implements Message {}
    record NpcResult(int requestId,ModernNpcProtocol.Request echo,ModernNpcProtocol.Reply reply) implements Message {}
    record SiegeRequest(int requestId,ModernSiegeProtocol.Request request) implements Message {}
    record SiegeResult(int requestId,ModernSiegeProtocol.Request echo,ModernSiegeProtocol.Reply reply) implements Message {}
    record NpcFireStormRequest(int requestId,ModernNpcFireStormState.Request request) implements Message {}
    record NpcFireStormResult(int requestId,ModernNpcFireStormState.Request echo,ModernNpcFireStormProtocol.Reply reply) implements Message {}
    record NpcDisintegrateRequest(int requestId,ModernNpcDisintegrateState.Request request) implements Message {}
    record NpcDisintegrateResult(int requestId,ModernNpcDisintegrateState.Request echo,ModernNpcDisintegrateProtocol.Reply reply) implements Message {}
    record NpcSpellRequest(int requestId,ModernNpcSpellProtocol.Request request) implements Message {}
    record NpcSpellResult(int requestId,ModernNpcSpellProtocol.Request echo,ModernNpcSpellProtocol.Reply reply) implements Message {}
    record DisintegrateRequest(int requestId,ModernDisintegrateState.Request request) implements Message {}
    record DisintegrateResult(int requestId,ModernDisintegrateState.Request echo,ModernDisintegrateProtocol.Reply reply) implements Message {}
    record RoomRequest(int requestId, ModernRooms.Command command) implements Message {
        @Override public String toString() { return "RoomRequest[redacted]"; }
    }
    record RoomResult(int requestId, ModernRoomProtocol.Echo echo, ModernRooms.Result result) implements Message {
        RoomResult(int requestId, ModernRooms.Command command, ModernRooms.Result result) {
            this(requestId, ModernRoomProtocol.Echo.of(command), result);
        }
        @Override public String toString() { return "RoomResult[redacted]"; }
    }
    record NavigationResult(int requestId, int action, int status, int phase, int channelId,
                            int sourceId, long objectId, long selectionEpoch, CharacterRow selected,
                            List<ChannelRow> channels) implements Message {
        NavigationResult { channels = List.copyOf(channels); }
    }

    static final class ProtocolException extends IOException {
        ProtocolException() { super("invalid modern application frame"); }
    }

    /** Returned AUTH frame contains a secret; caller MUST zero it after writing. */
    static byte[] encode(Message message) throws ProtocolException {
        return encode(message, ModernSessionProtocol.VERSION);
    }

    /** Version is the verified session selection; never inferred from payload length. */
    static byte[] encode(Message message, int version) throws ProtocolException {
        if (version < ModernSessionProtocol.VERSION || version > ModernSessionProtocol.MAX_VERSION) fail();
        if(message instanceof PlayerHealingRequest||message instanceof PlayerHealingResult||message instanceof VipEquipmentRequest||message instanceof VipEquipmentResult||message instanceof VipWorldRequest||message instanceof VipWorldResult
                ||message instanceof NativeWorldRequest||message instanceof NativeWorldResult||message instanceof NativeCanonicalWorldRequest||message instanceof NativeCanonicalWorldResult||message instanceof NativeCatalogWorldRequest||message instanceof NativeCatalogWorldResult||message instanceof NativeActorWorldRequest||message instanceof NativeActorWorldResult)fail();
        if(message instanceof SlowRequest r)return ModernSlowProtocol.encodeRequest(r.request(),version);
        if(message instanceof SlowResult r)return ModernSlowProtocol.encodeReply(r.reply(),version);
        if(message instanceof HasteRequest r)return ModernHasteProtocol.encodeRequest(r.request(),version);
        if(message instanceof HasteResult r)return ModernHasteProtocol.encodeReply(r.reply(),version);
        if(message instanceof GreaterHasteRequest r)return ModernGreaterHasteProtocol.encodeRequest(r.request(),version);
        if(message instanceof GreaterHasteResult r)return ModernGreaterHasteProtocol.encodeReply(r.reply(),version);
        if(message instanceof BattleEquipmentRequest r)return ModernBattleEquipmentProtocol.encodeRequest(r,version);
        if(message instanceof BattleEquipmentResult r)return ModernBattleEquipmentProtocol.encodeResult(r,version);
        if(message instanceof BattleRevivalRequest r)return ModernBattleRevivalProtocol.encodeRequest(r,version);
        if(message instanceof BattleRevivalResult r)return ModernBattleRevivalProtocol.encodeResult(r,version);
        if(message instanceof BattleWorldRequest r)return ModernBattleWorldProtocol.encodeRequest(r,version);
        if(message instanceof BattleWorldResult r)return ModernBattleWorldProtocol.encodeResult(r,version);
        if(message instanceof BattleOperationRequest r)return ModernBattleOperationProtocol.encodeRequest(r,version);
        if(message instanceof BattleOperationResult r)return ModernBattleOperationProtocol.encodeResult(r,version);
        if(message instanceof BattleReadResult r)return ModernBattleReadProtocol.encodeResult(r,version);
        if(message instanceof BattleActionRequest r)return ModernBattleActionProtocol.encodeRequest(r,version);
        if(message instanceof BattleActionResult r)return ModernBattleActionProtocol.encodeResult(r,version);
        byte[] scratch = new byte[message instanceof MatchmakingResult ? ModernMatchmakingProtocol.MAX_FRAME_BYTES
                : message instanceof FilteredChatResult ? ModernFilteredChatProtocol.MAX_FRAME_BYTES
                : message instanceof DirectChatResult || message instanceof ModeratedDirectResult ? ModernDirectChatProtocol.MAX_FRAME_BYTES
                : message instanceof ChatResult ? ModernChatProtocol.MAX_FRAME_BYTES
                : message instanceof NpcResult || message instanceof NpcSpellResult || message instanceof NpcDisintegrateResult || message instanceof NpcFireStormResult ? ModernNpcProtocol.maxFrameBytes(version)
                : message instanceof ShopResult ? ModernShopProtocol.MAX_FRAME_BYTES
                : message instanceof ItemResult || message instanceof AmmoResult ? ModernCombatProtocol.maxFrameBytes(version)
                : message instanceof GmResult ? ModernGmCommandProtocol.MAX_FRAME_BYTES
                : message instanceof EquipmentResult ? ModernEquipmentProtocol.MAX_FRAME_BYTES
                : message instanceof CombatResult || message instanceof CastResult || message instanceof SiegeResult || message instanceof DisintegrateResult ? ModernCombatProtocol.maxFrameBytes(version)
                : message instanceof WorldResult ? ModernWorldProtocol.MAX_FRAME_BYTES
                : message instanceof RoomResult ? ModernRoomProtocol.MAX_FRAME_BYTES
                : message instanceof CharacterDataResult ? ModernCharacterDataProtocol.MAX_FRAME_BYTES : MAX_FRAME_BYTES];
        try {
            validate(message);
            ByteBuffer out = ByteBuffer.wrap(scratch);
            out.put(new byte[] {'L', 'T', 'M', '1'}).putInt(0).putInt(message.requestId());
            int kind = message instanceof Auth ? 3 : message instanceof AuthResult ? 4
                    : message instanceof CharacterRequest ? 5 : message instanceof CharacterResult ? 6
                    : message instanceof Ping ? 7 : message instanceof Pong ? 8
                    : message instanceof CharacterSelection ? 9 : message instanceof NavigationResult ? 10
                    : message instanceof ChannelEnter ? 11 : message instanceof CharacterReturn ? 12
                    : message instanceof LobbyLeave ? 13 : message instanceof CharacterDataRequest ? 14
                    : message instanceof CharacterDataResult ? 15 : message instanceof EquipmentRequest ? 16
                    : message instanceof EquipmentResult ? 17 : message instanceof RoomRequest ? 18
                    : message instanceof RoomResult ? 19 : message instanceof WorldRequest ? 20 : message instanceof WorldResult ? 21 : message instanceof CombatRequest ? 22
                    : message instanceof CombatResult ? 23 : message instanceof SkillsRequest ? 24 : message instanceof SkillsResult ? 25
                    : message instanceof CastRequest ? 26 : message instanceof CastResult ? 27 : message instanceof RecordRequest ? 28
                    : message instanceof RecordResult ? 29 : message instanceof GmRequest ? 30 : message instanceof GmResult ? 31
                    : message instanceof ShopRequest ? 32 : message instanceof ShopResult ? 33 : message instanceof ItemRequest ? 34
                    : message instanceof ItemResult ? 35 : message instanceof AmmoRequest ? 36 : message instanceof AmmoResult ? 37
                    : message instanceof NpcRequest ? 38 : message instanceof NpcResult ? 39 : message instanceof NpcSpellRequest ? 40 : message instanceof NpcSpellResult ? 41 : message instanceof SiegeRequest ? 42 : message instanceof SiegeResult ? 43 : message instanceof DisintegrateRequest ? 44 : message instanceof DisintegrateResult ? 45 : message instanceof NpcDisintegrateRequest ? 46 : message instanceof NpcDisintegrateResult ? 47 : message instanceof NpcFireStormRequest ? 48 : message instanceof NpcFireStormResult ? 49 : message instanceof ChatRequest ? 50 : message instanceof ChatResult ? 51 : message instanceof DirectChatRequest ? 52 : message instanceof DirectChatResult ? 53 : message instanceof FilteredChatRequest ? 54 : message instanceof FilteredChatResult ? 55 : message instanceof ModeratedDirectRequest ? 56 : message instanceof ModeratedDirectResult ? 57
                    : message instanceof BattleReadRequest ? ModernBattleReadProtocol.REQUEST_KIND
                    : message instanceof MatchmakingRequest ? ModernMatchmakingProtocol.REQUEST_KIND : ModernMatchmakingProtocol.RESULT_KIND;
            if(kind==60&&version<ModernBattleReadProtocol.VERSION)fail();
            if (kind >= 58 && version < ModernMatchmakingProtocol.VERSION) fail();
            if (kind >= 54 && version < ModernFilteredChatProtocol.VERSION) fail();
            if (kind >= 50 && version < ModernChatProtocol.VERSION) fail();
            if (kind >= 44 && version < 27) fail();
            if (kind >= 42 && version < 26) fail();
            if (kind >= 48 && version < 31) fail();
            if (kind >= 46 && version < 29) fail();
            if (kind >= 40 && version < 24) fail();
            if (kind >= 38 && version < 17) fail();
            if (kind >= 24 && version < 4) fail();
            if (kind >= 28 && version < 5) fail();
            if (kind >= 30 && version < 7) fail();
            if (kind >= 32 && version < 8) fail();
            if (kind >= 36 && version < 11) fail();
            out.putShort((short) kind).putShort((short) 0);
            if(message instanceof BattleReadRequest request) {
                ModernBattleReadProtocol.putRequest(out,request.request());
            } else if (message instanceof MatchmakingRequest request) {
                ModernMatchmakingProtocol.putRequest(out,request.request());
            } else if (message instanceof MatchmakingResult result) {
                ModernMatchmakingProtocol.putResult(out,result.echo(),result.reply(),version);
            } else if (message instanceof ModeratedDirectRequest request) {
                ModernDirectChatProtocol.putRequest(out,request.request());
            } else if (message instanceof ModeratedDirectResult result) {
                ModernDirectChatProtocol.putResult(out,result.echo(),result.result(),true);
            } else if (message instanceof FilteredChatRequest request) {
                ModernFilteredChatProtocol.putRequest(out,request.request());
            } else if (message instanceof FilteredChatResult result) {
                ModernFilteredChatProtocol.putResult(out,result.echo(),result.result());
            } else if (message instanceof DirectChatRequest request) {
                ModernDirectChatProtocol.putRequest(out,request.request());
            } else if (message instanceof DirectChatResult result) {
                ModernDirectChatProtocol.putResult(out,result.echo(),result.result());
            } else if (message instanceof ChatRequest request) {
                ModernChatProtocol.putRequest(out,request.request());
            } else if (message instanceof ChatResult result) {
                ModernChatProtocol.putResult(out,result.echo(),result.result());
            } else if (message instanceof Auth auth) {
                putText(out, auth.account(), 64, false);
                putChars(out, auth.password(), 256, true);
            } else if (message instanceof AuthResult result) {
                out.putShort((short) result.status()).putShort((short) result.sources().size());
                putText(out, result.account(), 64, result.status() != 0);
                for (Source source : result.sources()) {
                    out.putInt(source.id());
                    putText(out, source.name(), 256, false);
                }
            } else if (message instanceof CharacterRequest request) {
                out.putInt(request.sourceId()).putLong(request.selectionEpoch());
            } else if (message instanceof CharacterResult result) {
                out.putInt(result.sourceId()).putLong(result.selectionEpoch());
                out.putShort((short) result.status()).putShort((short) result.characters().size());
                for (CharacterRow row : result.characters()) putCharacter(out, row);
            } else if (message instanceof CharacterSelection request) {
                putContext(out, request.sourceId(), request.objectId(), request.selectionEpoch());
            } else if (message instanceof ChannelEnter request) {
                putContext(out, request.sourceId(), request.objectId(), request.selectionEpoch());
                out.put((byte) request.channelId()).put((byte) 0).putShort((short) 0);
            } else if (message instanceof CharacterReturn request) {
                putContext(out, request.sourceId(), request.objectId(), request.selectionEpoch());
            } else if (message instanceof LobbyLeave request) {
                putContext(out, request.sourceId(), request.objectId(), request.selectionEpoch());
            } else if (message instanceof CharacterDataRequest request) {
                putContext(out, request.sourceId(), request.objectId(), request.selectionEpoch());
            } else if (message instanceof CharacterDataResult result) {
                putContext(out, result.sourceId(), result.objectId(), result.selectionEpoch());
                out.put((byte) result.status()).put((byte) 0).putShort((short) 0);
                if (result.snapshot() != null) ModernCharacterDataProtocol.put(out, result.snapshot());
            } else if (message instanceof EquipmentRequest request) {
                ModernEquipmentProtocol.putRequest(out, request.request());
            } else if (message instanceof EquipmentResult result) {
                ModernEquipmentProtocol.putResult(out, result, version);
            } else if (message instanceof RoomRequest request) {
                ModernRoomProtocol.putRequest(out, request.command());
            } else if (message instanceof RoomResult result) {
                ModernRoomProtocol.putResult(out, result, version);
            } else if (message instanceof WorldRequest request) {
                ModernWorldProtocol.putRequest(out, request.request());
            } else if (message instanceof WorldResult result) {
                ModernWorldProtocol.putResult(out, result, version);
            } else if (message instanceof CombatRequest request) {
                ModernCombatProtocol.putRequest(out,request.request());
            } else if (message instanceof CombatResult result) {
                ModernCombatProtocol.putResult(out,result,version);
            } else if (message instanceof SkillsRequest request) {
                ModernSpellProtocol.putContext(out,request.context());
            } else if (message instanceof SkillsResult result) {
                ModernSpellProtocol.putSkills(out,result,version);
            } else if (message instanceof CastRequest request) {
                ModernSpellProtocol.putRequest(out,request.request(),version);
            } else if (message instanceof CastResult result) {
                ModernSpellProtocol.putResult(out,result,version);
            } else if (message instanceof NpcFireStormRequest request) {
                ModernNpcFireStormWire.putRequest(out,request.request(),version);
            } else if (message instanceof NpcFireStormResult result) {
                ModernNpcFireStormProtocol.putResult(out,result,version);
            } else if (message instanceof NpcDisintegrateRequest request) {
                ModernNpcDisintegrateProtocol.putRequest(out,request.request(),version);
            } else if (message instanceof NpcDisintegrateResult result) {
                ModernNpcDisintegrateProtocol.putResult(out,result,version);
            } else if (message instanceof DisintegrateRequest request) {
                ModernDisintegrateProtocol.putRequest(out,request.request());
            } else if (message instanceof DisintegrateResult result) {
                ModernDisintegrateProtocol.putResult(out,result,version);
            } else if (message instanceof RecordRequest request) {
                ModernCharacterRecordProtocol.putContext(out, request.context());
            } else if (message instanceof RecordResult result) {
                ModernCharacterRecordProtocol.putReply(out, result.result());
            } else if (message instanceof GmRequest request) {
                ModernGmCommandProtocol.putRequest(out,request.request());
            } else if (message instanceof GmResult result) {
                ModernGmCommandProtocol.putResult(out,result.echo(),result.reply(),version);
            } else if (message instanceof ShopRequest request) {
                ModernShopProtocol.putRequest(out,request.request());
            } else if (message instanceof ShopResult result) {
                ModernShopProtocol.putResult(out,result,version);
            } else if (message instanceof ItemRequest request) {
                ModernItemProtocol.putRequest(out,request.request());
            } else if (message instanceof ItemResult result) {
                ModernItemProtocol.putResult(out,result,version);
            } else if (message instanceof SiegeRequest request) {
                ModernSiegeProtocol.putRequest(out,request.request());
            } else if (message instanceof SiegeResult result) {
                ModernSiegeProtocol.putResult(out,result,version);
            } else if (message instanceof NpcSpellRequest request) {
                ModernNpcSpellProtocol.putRequest(out,request.request());
            } else if (message instanceof NpcSpellResult result) {
                ModernNpcSpellProtocol.putResult(out,result,version);
            } else if (message instanceof NpcRequest request) {
                ModernNpcProtocol.putRequest(out,request.request(),version);
            } else if (message instanceof NpcResult result) {
                ModernNpcProtocol.putResult(out,result,version);
            } else if (message instanceof AmmoRequest request) {
                ModernAmmoProtocol.putRequest(out,request.request());
            } else if (message instanceof AmmoResult result) {
                ModernAmmoProtocol.putResult(out,result,version);
            } else if (message instanceof NavigationResult result) {
                out.put((byte) result.action()).put((byte) result.status()).put((byte) result.phase()).put((byte) result.channelId());
                putContext(out, result.sourceId(), result.objectId(), result.selectionEpoch());
                out.put((byte) (result.selected() == null ? 0 : 1)).put((byte) result.channels().size()).putShort((short) 0);
                if (result.selected() != null) putCharacter(out, result.selected());
                for (ChannelRow channel : result.channels()) {
                    out.put((byte) channel.id()).put((byte) 0).putShort((short) 0);
                    out.putInt(channel.population()).putInt(channel.capacity()).putLong(channel.minimumLevel()).putLong(channel.maximumLevel());
                    putText(out, channel.name(), 256, false);
                }
            }
            out.putInt(4, out.position());
            return Arrays.copyOf(scratch, out.position());
        } catch (RuntimeException invalid) {
            throw new ProtocolException();
        } finally {
            Arrays.fill(scratch, (byte) 0);
        }
    }

    /** Input ownership stays with caller, including wiping any AUTH bytes. */
    static Message decode(byte[] bytes) throws ProtocolException {
        return decode(bytes, ModernSessionProtocol.VERSION);
    }

    /** Full selected context is mandatory for new opcodes and schema7 output.
     * A read capability never grants the command opcode. */
    static byte[] encode(Message message,ModernSessionBoundary.Negotiated peer)throws ProtocolException {
        ModernSessionExtensions.requireContext(peer);
        if(message instanceof NativeActorWorldRequest r)return ModernNativeActorWorldProtocol.encodeRequest(r.request(),peer);
        if(message instanceof NativeActorWorldResult r)return ModernNativeActorWorldProtocol.encodeResult(r.result(),peer);
        if(message instanceof NativeCatalogWorldRequest r)return ModernNativeCatalogWorldProtocol.encodeRequest(r.request(),peer);
        if(message instanceof NativeCatalogWorldResult r)return ModernNativeCatalogWorldProtocol.encodeResult(r.result(),peer);
        if(message instanceof NativeCanonicalWorldRequest r)return ModernNativeCanonicalWorldProtocol.encodeRequest(r.request(),peer);
        if(message instanceof NativeCanonicalWorldResult r)return ModernNativeCanonicalWorldProtocol.encodeResult(r.result(),peer);
        if(message instanceof NativeWorldRequest r)return ModernWorldResourceProtocol.encodeRequest(r.request(),peer);
        if(message instanceof NativeWorldResult r)return ModernWorldResourceProtocol.encodeResult(r.result(),peer);
        if(message instanceof VipWorldRequest r)return ModernVipWorldProtocol.encodeRequest(r,peer);
        if(message instanceof VipWorldResult r)return ModernVipWorldProtocol.encodeResult(r,peer);
        if(message instanceof VipEquipmentRequest r)return ModernVipEquipmentProtocol.encodeRequest(r.request(),peer);
        if(message instanceof VipEquipmentResult r) {
            ModernMatchDescriptor.forPeer(message,peer);return ModernVipEquipmentProtocol.encodeReply(r.reply(),peer);
        }
        if(message instanceof PlayerHealingRequest r) {
            if(!ModernSessionExtensions.allowsCommands(peer))throw new ProtocolException();
            return ModernPlayerHealingProtocol.encodeCandidateRequest(r.request());
        }
        if(message instanceof PlayerHealingResult r) {
            ModernMatchDescriptor.forPeer(message,peer);return ModernPlayerHealingProtocol.encodeReply(r.reply(),peer);
        }
        if(message instanceof BattleReadResult||message instanceof BattleOperationResult||message instanceof BattleWorldResult
                ||message instanceof BattleRevivalResult||message instanceof BattleEquipmentResult
                ||message instanceof GreaterHasteResult||message instanceof HasteResult||message instanceof SlowResult)
            ModernMatchDescriptor.forPeer(message,peer);
        if(message instanceof BattleReadResult r)return ModernBattleReadProtocol.encodeResult(r,peer);
        if(message instanceof BattleOperationResult r)return ModernBattleOperationProtocol.encodeResult(r,peer);
        if(message instanceof BattleWorldResult r)return ModernBattleWorldProtocol.encodeResult(r,peer);
        if(message instanceof BattleRevivalResult r)return ModernBattleRevivalProtocol.encodeResult(r,peer);
        if(message instanceof BattleEquipmentResult r)return ModernBattleEquipmentProtocol.encodeResult(r,peer);
        if(message instanceof GreaterHasteResult r)return ModernGreaterHasteProtocol.encodeReply(r.reply(),peer);
        if(message instanceof HasteResult r)return ModernHasteProtocol.encodeReply(r.reply(),peer);
        if(message instanceof SlowResult r)return ModernSlowProtocol.encodeReply(r.reply(),peer);
        return encode(message,peer.version());
    }
    static Message decode(byte[] bytes,ModernSessionBoundary.Negotiated peer)throws ProtocolException {
        ModernSessionExtensions.requireContext(peer);
        if(bytes!=null&&bytes.length>=HEADER_BYTES) {
            int kind=Short.toUnsignedInt(ByteBuffer.wrap(bytes).getShort(12));
            if(kind==ModernNativeActorWorldProtocol.REQUEST_KIND)return new NativeActorWorldRequest(ModernNativeActorWorldProtocol.decodeRequest(bytes,peer));
            if(kind==ModernNativeActorWorldProtocol.RESULT_KIND)fail();
            if(kind==ModernNativeCatalogWorldProtocol.REQUEST_KIND)return new NativeCatalogWorldRequest(ModernNativeCatalogWorldProtocol.decodeRequest(bytes,peer));
            if(kind==ModernNativeCatalogWorldProtocol.RESULT_KIND)fail();
            if(kind==ModernNativeCanonicalWorldProtocol.REQUEST_KIND)return new NativeCanonicalWorldRequest(ModernNativeCanonicalWorldProtocol.decodeRequest(bytes,peer));
            if(kind==ModernNativeCanonicalWorldProtocol.RESULT_KIND)fail(); // Directional server codec: canonical DATA is consumer-decoded.
            if(kind==ModernWorldResourceProtocol.REQUEST_KIND)return new NativeWorldRequest(ModernWorldResourceProtocol.decodeRequest(bytes,peer));
            if(kind==ModernWorldResourceProtocol.RESULT_KIND)return new NativeWorldResult(ModernWorldResourceProtocol.decodeResult(bytes,peer));
        }
        if(bytes!=null&&bytes.length>=HEADER_BYTES&&Short.toUnsignedInt(ByteBuffer.wrap(bytes).getShort(12))==ModernVipWorldProtocol.REQUEST_KIND) {
            header(ByteBuffer.wrap(bytes),bytes.length,peer.version(),peer);
            return ModernVipWorldProtocol.decodeRequest(bytes,peer);
        }
        if(bytes!=null&&bytes.length>=HEADER_BYTES&&Short.toUnsignedInt(ByteBuffer.wrap(bytes).getShort(12))==ModernVipEquipmentProtocol.REQUEST_KIND) {
            header(ByteBuffer.wrap(bytes),bytes.length,peer.version(),peer);
            try{return new VipEquipmentRequest(ModernVipEquipmentProtocol.decodeRequest(bytes,peer));}
            catch(IllegalArgumentException failure){throw new ProtocolException();}
        }
        if(bytes!=null&&bytes.length>=HEADER_BYTES&&Short.toUnsignedInt(ByteBuffer.wrap(bytes).getShort(12))==ModernPlayerHealingProtocol.REQUEST_KIND) {
            header(ByteBuffer.wrap(bytes),bytes.length,peer.version(),peer);
            try{return new PlayerHealingRequest(ModernPlayerHealingProtocol.decodeCandidateRequest(bytes));}
            catch(IllegalArgumentException failure){throw new ProtocolException();}
        }
        return decode(bytes,peer.version());
    }
    static Message decode(byte[] bytes, int version) throws ProtocolException {
        if (version < ModernSessionProtocol.VERSION || version > ModernSessionProtocol.MAX_VERSION) fail();
        Message result = null;
        try {
            if (bytes == null || bytes.length < HEADER_BYTES) fail();
            if(bytes.length>ModernEquipmentProtocol.MAX_FRAME_BYTES) {
                int largeKind=Short.toUnsignedInt(ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getShort(12));
                boolean largeNpc=version>=31&&(largeKind==39||largeKind==41||largeKind==47||largeKind==49);
                if(largeNpc ? bytes.length>ModernNpcProtocol.maxFrameBytes(version)
                        : version<14||largeKind!=23&&largeKind!=27&&largeKind!=35&&largeKind!=37&&largeKind!=45&&largeKind!=47
                            ||bytes.length>ModernCombatProtocol.maxFrameBytes(version))fail();
            }
            ByteBuffer in = ByteBuffer.wrap(bytes);
            int kind = header(in, bytes.length,version);
            if(kind>=40&&version<24)fail();
            if (kind >= 32 && version < 8) fail();
            if (kind >= 36 && version < 11) fail();
            if (kind >= 24 && version < 4) fail();
            if (kind >= 28 && version < 5) fail();
            if (kind >= 30 && version < 7) fail();
            int requestId = in.getInt(8);
            switch (kind) {
                case 3 -> {
                    String account = text(in, 64, false);
                    char[] password = chars(in, 256, false, true);
                    try { result = new Auth(requestId, account, password); }
                    finally { ModernAccountAuthentication.clear(password); }
                }
                case 4 -> {
                    int status = u16(in), count = count(in);
                    String account = text(in, 64, status != 0);
                    var sources = new java.util.ArrayList<Source>(count);
                    for (int index = 0; index < count; index++) sources.add(new Source(in.getInt(), text(in, 256, false)));
                    result = new AuthResult(requestId, status, account, sources);
                }
                case 5 -> result = new CharacterRequest(requestId, in.getInt(), in.getLong());
                case 6 -> {
                    int sourceId = in.getInt(); long epoch = in.getLong();
                    int status = u16(in), count = count(in);
                    var rows = new java.util.ArrayList<CharacterRow>(count);
                    for (int index = 0; index < count; index++) rows.add(character(in));
                    result = new CharacterResult(requestId, sourceId, epoch, status, rows);
                }
                case 7 -> result = new Ping(requestId);
                case 8 -> result = new Pong(requestId);
                case 9 -> result = new CharacterSelection(requestId, in.getInt(), in.getLong(), in.getLong());
                case 11 -> {
                    int source = in.getInt(); long object = in.getLong(), epoch = in.getLong();
                    int channel = Byte.toUnsignedInt(in.get());
                    if (in.get() != 0 || in.getShort() != 0) fail();
                    result = new ChannelEnter(requestId, source, object, epoch, channel);
                }
                case 12 -> result = new CharacterReturn(requestId, in.getInt(), in.getLong(), in.getLong());
                case 13 -> result = new LobbyLeave(requestId, in.getInt(), in.getLong(), in.getLong());
                case 14 -> result = new CharacterDataRequest(requestId, in.getInt(), in.getLong(), in.getLong());
                case 15 -> {
                    int source = in.getInt(); long object = in.getLong(), epoch = in.getLong();
                    int status = Byte.toUnsignedInt(in.get());
                    if (in.get() != 0 || in.getShort() != 0 || status > 3) fail();
                    result = new CharacterDataResult(requestId, source, object, epoch, status,
                            status == 0 ? ModernCharacterDataProtocol.get(in, source) : null);
                }
                case 16 -> result = new EquipmentRequest(requestId, ModernEquipmentProtocol.getRequest(in));
                case 17 -> result = ModernEquipmentProtocol.getResult(in, requestId, version);
                case 18 -> result = new RoomRequest(requestId, ModernRoomProtocol.getRequest(in));
                case 19 -> result = ModernRoomProtocol.getResult(in, requestId, version);
                case 20 -> result = new WorldRequest(requestId, ModernWorldProtocol.getRequest(in));
                case 60 -> result = new BattleReadRequest(requestId,ModernBattleReadProtocol.getRequest(in));
                case 62 -> result = new BattleActionRequest(requestId,ModernBattleActionProtocol.getRequest(in,requestId,version));
                case 64 -> result = new BattleOperationRequest(requestId,ModernBattleActionProtocol.getRequest(in,requestId,version));
                case 66 -> result = new BattleWorldRequest(requestId,ModernBattleWorldProtocol.getRequest(in));
                case 70 -> result = new BattleEquipmentRequest(requestId,ModernBattleEquipmentProtocol.getRequest(in));
                case 76 -> {result=new SlowRequest(ModernSlowProtocol.decodeRequest(bytes,version));in.position(in.limit());}
                case 74 -> {result=new HasteRequest(ModernHasteProtocol.decodeRequest(bytes,version));in.position(in.limit());}
                case 72 -> {result=new GreaterHasteRequest(ModernGreaterHasteProtocol.decodeRequest(bytes,version));in.position(in.limit());}
                case 68 -> result = new BattleRevivalRequest(requestId,ModernBattleRevivalProtocol.getRequest(in));
                case 21 -> result = ModernWorldProtocol.getResult(in, requestId, version);
                case 22 -> result = new CombatRequest(requestId,ModernCombatProtocol.getRequest(in));
                case 23 -> result = ModernCombatProtocol.getResult(in,requestId,version);
                case 24 -> result = new SkillsRequest(requestId,ModernSpellProtocol.getContext(in));
                case 25 -> result = ModernSpellProtocol.getSkills(in,requestId,version);
                case 26 -> result = new CastRequest(requestId,ModernSpellProtocol.getRequest(in,version));
                case 27 -> result = ModernSpellProtocol.getResult(in,requestId,version);
                case 28 -> result = new RecordRequest(requestId, ModernCharacterRecordProtocol.getContext(in));
                case 29 -> result = new RecordResult(requestId, ModernCharacterRecordProtocol.getReply(in));
                case 30 -> result = new GmRequest(requestId,ModernGmCommandProtocol.getRequest(in));
                case 50 -> result = new ChatRequest(requestId,ModernChatProtocol.getRequest(in));
                case 51 -> result = ModernChatProtocol.getResult(in,requestId);
                case 52 -> result = new DirectChatRequest(requestId,ModernDirectChatProtocol.getRequest(in));
                case 53 -> result = ModernDirectChatProtocol.getResult(in,requestId);
                case 54 -> result = new FilteredChatRequest(requestId,ModernFilteredChatProtocol.getRequest(in));
                case 55 -> result = ModernFilteredChatProtocol.getResult(in,requestId);
                case 58 -> result = new MatchmakingRequest(requestId,ModernMatchmakingProtocol.getRequest(in));
                case 59 -> result = ModernMatchmakingProtocol.getResult(in,requestId,version);
                case 56 -> result = new ModeratedDirectRequest(requestId,ModernDirectChatProtocol.getRequest(in));
                case 57 -> {
                    var direct=ModernDirectChatProtocol.getResult(in,requestId,true);
                    result=new ModeratedDirectResult(requestId,direct.echo(),direct.result());
                }
                case 31 -> result = ModernGmCommandProtocol.getResult(in,requestId,version);
                case 32 -> result = new ShopRequest(requestId,ModernShopProtocol.getRequest(in));
                case 33 -> result = ModernShopProtocol.getResult(in,requestId,version);
                case 34 -> result = new ItemRequest(requestId,ModernItemProtocol.getRequest(in));
                case 35 -> result = ModernItemProtocol.getResult(in,requestId,version);
                case 36 -> result = new AmmoRequest(requestId,ModernAmmoProtocol.getRequest(in));
                case 38 -> result = new NpcRequest(requestId,ModernNpcProtocol.getRequest(in,version));
                case 39 -> result = ModernNpcProtocol.getResult(in,requestId,version);
                case 40 -> result = new NpcSpellRequest(requestId,ModernNpcSpellProtocol.getRequest(in));
                case 41 -> result = ModernNpcSpellProtocol.getResult(in,requestId,version);
                case 42 -> result = new SiegeRequest(requestId,ModernSiegeProtocol.getRequest(in));
                case 43 -> result = ModernSiegeProtocol.getResult(in,requestId,version);
                case 48 -> result = new NpcFireStormRequest(requestId,ModernNpcFireStormWire.getRequest(in,version));
                case 49 -> result = ModernNpcFireStormProtocol.getResult(in,requestId,version);
                case 46 -> result = new NpcDisintegrateRequest(requestId,ModernNpcDisintegrateProtocol.getRequest(in,version));
                case 47 -> result = ModernNpcDisintegrateProtocol.getResult(in,requestId,version);
                case 44 -> result = new DisintegrateRequest(requestId,ModernDisintegrateProtocol.getRequest(in));
                case 45 -> result = ModernDisintegrateProtocol.getResult(in,requestId,version);
                case 37 -> result = ModernAmmoProtocol.getResult(in,requestId,version);
                case 10 -> {
                    int action = Byte.toUnsignedInt(in.get()), status = Byte.toUnsignedInt(in.get());
                    int phase = Byte.toUnsignedInt(in.get()), channel = Byte.toUnsignedInt(in.get());
                    int source = in.getInt(); long object = in.getLong(), epoch = in.getLong();
                    int present = Byte.toUnsignedInt(in.get()), count = Byte.toUnsignedInt(in.get());
                    if (present > 1 || count > 4 || in.getShort() != 0) fail();
                    CharacterRow selected = present == 0 ? null : character(in);
                    var channels = new java.util.ArrayList<ChannelRow>(count);
                    for (int index = 0; index < count; index++) {
                        int id = Byte.toUnsignedInt(in.get());
                        if (in.get() != 0 || in.getShort() != 0) fail();
                        int population = in.getInt(), capacity = in.getInt();
                        long minimum = in.getLong(), maximum = in.getLong();
                        channels.add(new ChannelRow(id, text(in, 256, false), population, capacity, minimum, maximum));
                    }
                    result = new NavigationResult(requestId, action, status, phase, channel, source, object, epoch, selected, channels);
                }
                default -> fail();
            }
            if (in.hasRemaining()) fail();
            validate(result);
            return result;
        } catch (ProtocolException failure) {
            if (result != null) result.close();
            throw failure;
        } catch (RuntimeException invalid) {
            if (result != null) result.close();
            throw new ProtocolException();
        }
    }

    /** Null means clean EOF at a frame boundary; a partial frame is always an error. */
    static byte[] readFrame(InputStream input) throws IOException {
        return readFrame(input,ModernSessionProtocol.VERSION);
    }
    static byte[] readFrame(InputStream input,int version) throws IOException {
        return readFrame(input,version,null);
    }
    static byte[] readFrame(InputStream input,ModernSessionBoundary.Negotiated peer)throws IOException {
        ModernSessionExtensions.requireContext(peer);return readFrame(input,peer.version(),peer);
    }
    private static byte[] readFrame(InputStream input,int version,ModernSessionBoundary.Negotiated peer)throws IOException {
        if(version<ModernSessionProtocol.VERSION || version>ModernSessionProtocol.MAX_VERSION)fail();
        byte[] header = new byte[HEADER_BYTES];
        int first = input.read();
        if (first == -1) return null;
        header[0] = (byte) first;
        readFully(input, header, 1, HEADER_BYTES - 1);
        ByteBuffer parsed = ByteBuffer.wrap(header);
        int length = parsed.getInt(4);
        header(parsed, length,version,peer); // Validate before allocating from peer-controlled length.
        byte[] frame = Arrays.copyOf(header, length);
        try {
            readFully(input, frame, HEADER_BYTES, length - HEADER_BYTES);
            return frame;
        } catch (IOException | RuntimeException failure) {
            Arrays.fill(frame, (byte) 0);
            throw failure;
        }
    }

    private static int header(ByteBuffer in,int length,int version,ModernSessionBoundary.Negotiated peer)throws ProtocolException {
        if(in.remaining()>=HEADER_BYTES) {
            int kind=Short.toUnsignedInt(in.getShort(12));
            if(kind==ModernNativeActorWorldProtocol.REQUEST_KIND||kind==ModernNativeActorWorldProtocol.RESULT_KIND) {
                if(!ModernNativeActorWorldProtocol.negotiated(peer)
                        ||kind==ModernNativeActorWorldProtocol.REQUEST_KIND&&length!=ModernNativeActorWorldProtocol.REQUEST_BYTES
                        ||kind==ModernNativeActorWorldProtocol.RESULT_KIND&&(length<ModernNativeActorWorldProtocol.MIN_RESULT_BYTES||length>ModernNativeActorWorldProtocol.maximumResult(peer))
                        ||in.getInt()!=0x4c544d31||in.getInt()!=length||in.getInt()==0||Short.toUnsignedInt(in.getShort())!=kind||in.getShort()!=0)fail();
                return kind;
            }
            if(kind==ModernNativeCatalogWorldProtocol.REQUEST_KIND||kind==ModernNativeCatalogWorldProtocol.RESULT_KIND) {
                if(!ModernNativeCatalogWorldProtocol.negotiated(peer)
                        ||kind==ModernNativeCatalogWorldProtocol.REQUEST_KIND&&length!=ModernNativeCatalogWorldProtocol.REQUEST_BYTES
                        ||kind==ModernNativeCatalogWorldProtocol.RESULT_KIND&&(length<ModernNativeCatalogWorldProtocol.MIN_RESULT_BYTES||length>ModernNativeCatalogWorldProtocol.maximumResult(peer))
                        ||in.getInt()!=0x4c544d31||in.getInt()!=length||in.getInt()==0||Short.toUnsignedInt(in.getShort())!=kind||in.getShort()!=0)fail();
                return kind;
            }
            if(kind==ModernNativeCanonicalWorldProtocol.REQUEST_KIND||kind==ModernNativeCanonicalWorldProtocol.RESULT_KIND) {
                if(!ModernNativeCanonicalWorldProtocol.negotiated(peer)
                        ||kind==ModernNativeCanonicalWorldProtocol.REQUEST_KIND&&length!=ModernNativeCanonicalWorldProtocol.REQUEST_BYTES
                        ||kind==ModernNativeCanonicalWorldProtocol.RESULT_KIND&&(length<ModernNativeCanonicalWorldProtocol.MIN_RESULT_BYTES||length>ModernNativeCanonicalWorldProtocol.maximumResult(peer))
                        ||in.getInt()!=0x4c544d31||in.getInt()!=length||in.getInt()==0||Short.toUnsignedInt(in.getShort())!=kind||in.getShort()!=0)fail();
                return kind;
            }
            if(kind==ModernWorldResourceProtocol.REQUEST_KIND||kind==ModernWorldResourceProtocol.RESULT_KIND) {
                if(!ModernWorldResourceProtocol.negotiated(peer)||kind==ModernWorldResourceProtocol.REQUEST_KIND&&length!=ModernWorldResourceProtocol.REQUEST_BYTES
                        ||kind==ModernWorldResourceProtocol.RESULT_KIND&&(length<412||length>ModernWorldResourceProtocol.MAX_RESULT_BYTES)
                        ||in.getInt()!=0x4c544d31||in.getInt()!=length||in.getInt()==0||Short.toUnsignedInt(in.getShort())!=kind||in.getShort()!=0)fail();
                return kind;
            }
        }
        if(in.remaining()>=HEADER_BYTES&&Short.toUnsignedInt(in.getShort(12))==ModernVipWorldProtocol.REQUEST_KIND) {
            if(!ModernVipWorldProtocol.negotiated(peer)||length!=ModernVipWorldProtocol.REQUEST_BYTES
                    ||in.getInt()!=0x4c544d31||in.getInt()!=length||in.getInt()==0
                    ||in.getShort()!=ModernVipWorldProtocol.REQUEST_KIND||in.getShort()!=0)fail();
            return ModernVipWorldProtocol.REQUEST_KIND;
        }
        if(in.remaining()>=HEADER_BYTES&&Short.toUnsignedInt(in.getShort(12))==ModernVipEquipmentProtocol.REQUEST_KIND) {
            if(!ModernSessionExtensions.allowsVipEquipment(peer)||length!=ModernVipEquipmentProtocol.REQUEST_BYTES
                    ||in.getInt()!=0x4c544d31||in.getInt()!=length||in.getInt()==0
                    ||in.getShort()!=ModernVipEquipmentProtocol.REQUEST_KIND||in.getShort()!=0)fail();
            return ModernVipEquipmentProtocol.REQUEST_KIND;
        }
        if(in.remaining()>=HEADER_BYTES&&Short.toUnsignedInt(in.getShort(12))==ModernPlayerHealingProtocol.REQUEST_KIND) {
            if(!ModernSessionExtensions.allowsCommands(peer)||length!=ModernPlayerHealingProtocol.REQUEST_BYTES
                    ||in.getInt()!=0x4c544d31||in.getInt()!=length||in.getInt()==0
                    ||in.getShort()!=ModernPlayerHealingProtocol.REQUEST_KIND||in.getShort()!=0)fail();
            return ModernPlayerHealingProtocol.REQUEST_KIND;
        }
        return header(in,length,version);
    }
    private static int header(ByteBuffer in, int length,int version) throws ProtocolException {
        if (in.getInt() != 0x4c544d31 || length < HEADER_BYTES
                || in.getInt() != length || in.getInt() == 0) fail();
        int kind = u16(in);
        // DATA (61) and action RESULT (63) are outbound only. The new inbound
        // envelope has its own small bound before any peer-sized allocation.
        if(kind==70) {
            if(in.getShort()!=0||version<ModernBattleEquipmentProtocol.VERSION||length!=ModernBattleEquipmentProtocol.REQUEST_BYTES)fail();
            return kind;
        }
        if(kind==68) {
            if(in.getShort()!=0||version<ModernBattleRevivalProtocol.VERSION||length!=ModernBattleRevivalProtocol.REQUEST_BYTES)fail();
            return kind;
        }
        if(kind==76) {
            if(in.getShort()!=0||version<ModernSlowProtocol.VERSION||length!=ModernSlowProtocol.REQUEST_BYTES)fail();
            return kind;
        }
        if(kind==74) {
            if(in.getShort()!=0||version<ModernHasteProtocol.VERSION||length!=ModernHasteProtocol.REQUEST_BYTES)fail();
            return kind;
        }
        if(kind==72) {
            if(in.getShort()!=0||version<ModernGreaterHasteProtocol.VERSION||length!=ModernGreaterHasteProtocol.REQUEST_BYTES)fail();
            return kind;
        }
        if(kind==66) {
            if(in.getShort()!=0||version<ModernBattleWorldProtocol.VERSION||length!=ModernBattleWorldProtocol.REQUEST_BYTES)fail();
            return kind;
        }
        if(kind==62||kind==64) {
            if(in.getShort()!=0||version<(kind==64?ModernBattleOperationProtocol.VERSION:ModernBattleActionProtocol.VERSION)||length<ModernBattleActionProtocol.MIN_REQUEST_BYTES||length>ModernBattleActionProtocol.MAX_REQUEST_BYTES)fail();
            return kind;
        }
        if (kind < 3 || kind > 60 || in.getShort() != 0
                || kind==60&&(version<ModernBattleReadProtocol.VERSION||length!=ModernBattleReadProtocol.REQUEST_BYTES)
                || kind >= 58 && version < ModernMatchmakingProtocol.VERSION
                || kind == 58 && length != ModernMatchmakingProtocol.REQUEST_BYTES
                || kind == 59 && length < ModernMatchmakingProtocol.MIN_RESULT_BYTES
                || kind >= 54 && version < ModernFilteredChatProtocol.VERSION
                || kind == 54 && (length < ModernFilteredChatProtocol.MIN_REQUEST_BYTES || length > ModernFilteredChatProtocol.MAX_REQUEST_BYTES)
                || kind == 55 && length < ModernFilteredChatProtocol.MIN_RESULT_BYTES
                || kind >= 50 && version < ModernChatProtocol.VERSION
                || kind == 50 && (length < 76 || length > ModernChatProtocol.MAX_REQUEST_BYTES)
                || kind == 51 && length < 104
                || (kind == 52 || kind == 56) && (length < ModernDirectChatProtocol.MIN_REQUEST_BYTES || length > ModernDirectChatProtocol.MAX_REQUEST_BYTES)
                || (kind == 53 || kind == 57) && length < ModernDirectChatProtocol.MIN_RESULT_BYTES
                || kind >= 48 && version < 31 || kind == 48 && length != 80 || kind == 49 && length < 84
                || kind >= 46 && version < 29 || kind == 46 && length != 96 || kind == 47 && length < 100
                || kind >= 44 && version < 27 || kind == 44 && length != 88 || kind == 45 && length < 92
                || kind >= 42 && version < 26 || kind == 42 && length != 72 || kind == 43 && length < 76
                || kind >= 40 && version < 24 || kind == 40 && length != 96 || kind == 41 && length < 100
                || kind >= 38 && version < 17
                || kind == 38 && length != 88 || kind == 39 && length < 92
                || kind >= 36 && version < 11
                || kind == 28 && length != 36 || kind == 29 && length != 64
                || kind == 30 && length != 60 || kind == 31 && length < 72
                || kind == 32 && length != 124 || kind == 33 && length < 128 || kind == 34 && length != 72
                || kind == 36 && length != 72 || kind == 37 && length < 76
                || length > (kind == 59 ? ModernMatchmakingProtocol.MAX_FRAME_BYTES : kind == 55 ? ModernFilteredChatProtocol.MAX_FRAME_BYTES : kind == 53 || kind == 57 ? ModernDirectChatProtocol.MAX_FRAME_BYTES : kind == 51 ? ModernChatProtocol.MAX_FRAME_BYTES
                : kind == 39 || kind == 41 || kind == 47 || kind == 49 ? ModernNpcProtocol.maxFrameBytes(version)
                : kind == 17 ? ModernEquipmentProtocol.MAX_FRAME_BYTES
                : kind == 33 ? ModernShopProtocol.MAX_FRAME_BYTES
                : kind == 35 || kind == 37 ? ModernCombatProtocol.maxFrameBytes(version)
                : kind == 31 ? ModernGmCommandProtocol.MAX_FRAME_BYTES
                : kind == 21 ? ModernWorldProtocol.MAX_FRAME_BYTES
                : kind == 23 || kind == 27 || kind == 43 || kind == 45 ? ModernCombatProtocol.maxFrameBytes(version)
                : kind == 19 ? ModernRoomProtocol.MAX_FRAME_BYTES
                : kind == 15 ? ModernCharacterDataProtocol.MAX_FRAME_BYTES : MAX_FRAME_BYTES)) fail();
        return kind;
    }

    private static void validate(Message message) throws ProtocolException {
        if(message instanceof GreaterHasteRequest||message instanceof GreaterHasteResult||message instanceof HasteRequest||message instanceof HasteResult||message instanceof SlowRequest||message instanceof SlowResult)return;
        if(message instanceof BattleEquipmentRequest r){if(r.requestId()==0)fail();return;}
        if(message instanceof BattleEquipmentResult r){ModernBattleEquipmentProtocol.validate(r);return;}
        if(message instanceof BattleRevivalRequest r){if(r.requestId()==0)fail();return;}
        if(message instanceof BattleRevivalResult r){ModernBattleRevivalProtocol.validate(r);return;}
        if(message instanceof BattleWorldRequest r){if(r.requestId()==0)fail();return;}
        if(message instanceof BattleWorldResult r){ModernBattleWorldProtocol.validate(r);return;}
        if(message instanceof BattleOperationRequest r){if(r.requestId()==0||r.requestId()!=r.request().command().requestId())fail();return;}
        if(message instanceof BattleOperationResult r){ModernBattleOperationProtocol.validate(r);return;}
        if(message instanceof BattleActionRequest r){if(r.requestId()==0||r.requestId()!=r.request().command().requestId())fail();return;}
        if(message instanceof BattleActionResult r){ModernBattleActionProtocol.validateResult(r);return;}
        if (message == null || message.requestId() == 0) fail();
        if(message instanceof BattleReadRequest r){if(r.request()==null)fail();return;}
        if(message instanceof MatchmakingRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof MatchmakingResult result) {ModernMatchmakingProtocol.validate(result.echo(),result.reply());return;}
        if(message instanceof FilteredChatRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof FilteredChatResult result) {ModernFilteredChatProtocol.validate(result.echo(),result.result());return;}
        if(message instanceof ModeratedDirectRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof ModeratedDirectResult result) {ModernDirectChatProtocol.validate(result.echo(),result.result(),true);return;}
        if(message instanceof DirectChatRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof DirectChatResult result) {ModernDirectChatProtocol.validate(result.echo(),result.result());return;}
        if(message instanceof ChatRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof ChatResult result) {ModernChatProtocol.validate(result.echo(),result.result());return;}
        if(message instanceof NpcFireStormRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof NpcFireStormResult result) {ModernNpcFireStormProtocol.validate(result);return;}
        if(message instanceof NpcDisintegrateRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof NpcDisintegrateResult result) {ModernNpcDisintegrateProtocol.validate(result);return;}
        if(message instanceof DisintegrateRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof DisintegrateResult result) {ModernDisintegrateProtocol.validate(result);return;}
        if(message instanceof SiegeRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof SiegeResult result) {ModernSiegeProtocol.validate(result);return;}
        if(message instanceof NpcRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof NpcResult result) {ModernNpcProtocol.validate(result);return;}
        if(message instanceof NpcSpellRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof NpcSpellResult result) {ModernNpcSpellProtocol.validate(result);return;}
        if(message instanceof ShopRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof ShopResult result) {ModernShopProtocol.validate(result,ModernSessionProtocol.MAX_VERSION);return;}
        if(message instanceof ItemRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof ItemResult result) {ModernItemProtocol.validate(result);return;}
        if(message instanceof AmmoRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof AmmoResult result) {ModernAmmoProtocol.validate(result);return;}
        if(message instanceof GmRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof GmResult result) {ModernGmCommandProtocol.validate(result.echo(),result.reply(),7);return;}
        if(message instanceof RecordRequest request) {if(request.context()==null)fail();return;}
        if(message instanceof RecordResult result) {if(result.result()==null)fail();return;}
        if(message instanceof SkillsRequest request) {if(request.context()==null)fail();return;}
        if(message instanceof SkillsResult result) {if(result.echo()==null || result.reply()==null)fail();return;}
        if(message instanceof CastRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof CastResult result) {ModernSpellProtocol.validate(result);return;}
        if(message instanceof CombatRequest request) {if(request.request()==null)fail();return;}
        if(message instanceof CombatResult result) {ModernCombatProtocol.validate(result);return;}
        if (message instanceof WorldRequest request) {
            if (request.request() == null) fail();
            return;
        }
        if (message instanceof WorldResult result) {
            ModernWorldProtocol.validate(result);
            return;
        }
        if (message instanceof RoomRequest request) {
            if (request.command() == null) fail();
            return;
        }
        if (message instanceof RoomResult result) {
            ModernRoomProtocol.validate(result);
            return;
        }
        if (message instanceof EquipmentRequest request) {
            if (request.request() == null) fail();
            return;
        }
        if (message instanceof EquipmentResult result) {
            ModernEquipmentProtocol.validate(result);
            return;
        }
        if (message instanceof CharacterDataRequest request) {
            validateContext(request.sourceId(), request.objectId(), request.selectionEpoch());
            return;
        }
        if (message instanceof CharacterDataResult result) {
            validateContext(result.sourceId(), result.objectId(), result.selectionEpoch());
            if (result.status() < 0 || result.status() > 3 || (result.status() == 0) != (result.snapshot() != null)) fail();
            if (result.snapshot() != null && (result.snapshot().sourceId() != result.sourceId()
                    || result.snapshot().character().objectId() != result.objectId())) fail();
            return;
        }
        if (message instanceof Auth auth) {
            validateText(auth.account(), 64, false);
            validateChars(auth.password(), 256, false, true);
        } else if (message instanceof AuthResult result) {
            if (result.status() < 0 || result.status() > 2 || result.sources().size() > MAX_ROWS) fail();
            if (result.status() == 0) {
                if (result.sources().isEmpty()) fail();
                validateText(result.account(), 64, false);
            } else if (!result.sources().isEmpty() || !"".equals(result.account())) fail();
            int previous = 0;
            var names = new HashSet<String>();
            for (Source source : result.sources()) {
                if (source.id() <= previous || !names.add(source.name())) fail();
                previous = source.id();
                validateText(source.name(), 256, false);
            }
        } else if (message instanceof CharacterRequest request) {
            if (request.sourceId() <= 0 || request.selectionEpoch() == 0) fail();
        } else if (message instanceof CharacterResult result) {
            if (result.sourceId() <= 0 || result.selectionEpoch() == 0 || result.status() < 0
                    || result.status() > 2 || result.characters().size() > MAX_ROWS
                    || (result.status() != 0 && !result.characters().isEmpty())) fail();
            long previous = 0;
            for (CharacterRow row : result.characters()) {
                validateCharacter(row);
                if (row.objectId() <= previous) fail();
                previous = row.objectId();
            }
        } else if (message instanceof CharacterSelection request) {
            validateContext(request.sourceId(), request.objectId(), request.selectionEpoch());
        } else if (message instanceof ChannelEnter request) {
            validateContext(request.sourceId(), request.objectId(), request.selectionEpoch());
            if (request.channelId() < 0 || request.channelId() > 3) fail();
        } else if (message instanceof CharacterReturn request) {
            validateContext(request.sourceId(), request.objectId(), request.selectionEpoch());
        } else if (message instanceof LobbyLeave request) {
            validateContext(request.sourceId(), request.objectId(), request.selectionEpoch());
        } else if (message instanceof NavigationResult result) {
            validateContext(result.sourceId(), result.objectId(), result.selectionEpoch());
            if (result.status() < 0 || result.status() > 3 || result.phase() < 0 || result.phase() > 2) fail();
            switch (result.action()) {
                case 9 -> { if (result.phase() != (result.status() == 0 ? 1 : 0)) fail(); }
                case 11 -> { if (result.phase() == 0 || (result.status() == 0 && result.phase() != 2)) fail(); }
                case 12 -> {
                    // A selection with an already admitted durable operation
                    // stays in its current Channels/Lobby phase until retry.
                    if (result.status() == 0 ? result.phase() != 0
                            : result.status() != 3 || result.phase() == 0) fail();
                }
                case 13 -> { if (result.status() != 0 || result.phase() != 1) fail(); }
                default -> fail();
            }
            if (result.phase() == 0) {
                if (result.selected() != null || !result.channels().isEmpty() || result.channelId() != 255) fail();
            } else {
                validateCharacter(result.selected());
                if (result.selected().objectId() != result.objectId() || result.channels().size() != 4
                        || (result.phase() == 1 ? result.channelId() != 255 : result.channelId() < 0 || result.channelId() > 3)) fail();
                var names = new HashSet<String>();
                for (int index = 0; index < 4; index++) {
                    ChannelRow row = result.channels().get(index);
                    if (row.id() != index || row.capacity() < 1 || row.capacity() > 65535 || row.population() < 0
                            || row.population() > row.capacity() || row.minimumLevel() < 1 || row.maximumLevel() < row.minimumLevel()
                            || !names.add(row.name())) fail();
                    validateText(row.name(), 256, false);
                }
            }
        }
    }

    private static void validateContext(int source, long object, long epoch) throws ProtocolException {
        if (source <= 0 || object <= 0 || epoch == 0) fail();
    }
    private static void putContext(ByteBuffer out, int source, long object, long epoch) {
        out.putInt(source).putLong(object).putLong(epoch);
    }
    private static void validateCharacter(CharacterRow row) throws ProtocolException {
        if (row == null || row.objectId() <= 0 || row.characterClass() < 0 || row.characterClass() > 65535
                || row.sex() < 0 || row.sex() > 1 || row.level() < 1) fail();
        validateText(row.name(), 256, false);
    }
    static void putCharacter(ByteBuffer out, CharacterRow row) throws ProtocolException {
        out.putLong(row.objectId()); putText(out, row.name(), 256, false);
        out.putShort((short) row.characterClass()).put((byte) row.sex()).put((byte) 0).putLong(row.level());
    }
    static CharacterRow character(ByteBuffer in) throws ProtocolException {
        long id = in.getLong(); String name = text(in, 256, false);
        int characterClass = u16(in), sex = Byte.toUnsignedInt(in.get());
        if (in.get() != 0) fail();
        return new CharacterRow(id, name, characterClass, sex, in.getLong());
    }

    private static int u16(ByteBuffer in) { return Short.toUnsignedInt(in.getShort()); }
    private static int count(ByteBuffer in) throws ProtocolException {
        int count = u16(in);
        if (count > MAX_ROWS) fail();
        return count;
    }
    static String text(ByteBuffer in, int maximum, boolean allowEmpty) throws ProtocolException {
        char[] chars = chars(in, maximum, allowEmpty, false);
        try { return new String(chars); }
        finally { Arrays.fill(chars, '\0'); }
    }
    private static char[] chars(ByteBuffer in, int maximum, boolean allowEmpty, boolean secret) throws ProtocolException {
        int length = u16(in);
        if (length > maximum || length > in.remaining() || (!allowEmpty && length == 0)) fail();
        ByteBuffer encoded = in.slice();
        encoded.limit(length);
        CharBuffer decoded = null;
        char[] result = null;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(encoded);
            result = new char[decoded.remaining()];
            decoded.get(result);
            validateChars(result, maximum, allowEmpty, secret);
            in.position(in.position() + length);
            return result;
        } catch (CharacterCodingException | ProtocolException invalid) {
            ModernAccountAuthentication.clear(result);
            throw new ProtocolException();
        } finally {
            if (decoded != null && decoded.hasArray()) Arrays.fill(decoded.array(), '\0');
        }
    }
    private static void validateText(String text, int maximum, boolean allowEmpty) throws ProtocolException {
        if (text == null) fail();
        char[] chars = text.toCharArray();
        try { validateChars(chars, maximum, allowEmpty, false); }
        finally { Arrays.fill(chars, '\0'); }
    }
    private static void validateChars(char[] chars, int maximum, boolean allowEmpty, boolean secret) throws ProtocolException {
        if (chars == null || (!allowEmpty && chars.length == 0) || chars.length > maximum) fail();
        boolean nonblank = false;
        for (char character : chars) {
            if (character == 0 || Character.isISOControl(character)) fail();
            if (!Character.isWhitespace(character) && !Character.isSpaceChar(character)) nonblank = true;
        }
        if (!secret && chars.length > 0 && !nonblank) fail();
        ByteBuffer encoded = null;
        try {
            encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(chars));
            if (encoded.remaining() > maximum) fail();
        } catch (CharacterCodingException invalid) { throw new ProtocolException(); }
        finally { if (encoded != null && encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0); }
    }
    static void putText(ByteBuffer out, String text, int maximum, boolean allowEmpty) throws ProtocolException {
        char[] chars = text.toCharArray();
        try { putChars(out, chars, maximum, allowEmpty); }
        finally { Arrays.fill(chars, '\0'); }
    }
    private static void putChars(ByteBuffer out, char[] chars, int maximum, boolean ignored) throws ProtocolException {
        ByteBuffer encoded = null;
        try {
            encoded = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(chars));
            if (encoded.remaining() > maximum) fail();
            out.putShort((short) encoded.remaining()).put(encoded);
        } catch (CharacterCodingException invalid) { throw new ProtocolException(); }
        finally { if (encoded != null && encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0); }
    }
    private static void readFully(InputStream input, byte[] bytes, int offset, int length) throws IOException {
        int end = offset + length;
        while (offset < end) {
            int count = input.read(bytes, offset, end - offset);
            if (count == -1) throw new EOFException("partial modern application frame");
            if (count == 0) {
                int value = input.read();
                if (value == -1) throw new EOFException("partial modern application frame");
                bytes[offset++] = (byte) value;
            } else offset += count;
        }
    }
    private static void fail() throws ProtocolException { throw new ProtocolException(); }
}
