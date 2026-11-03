package local.lito381.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Independent modern LiTo handshake, NOT a native Protocol/C_VERSION extension.
 * All integers are big endian. Header: "LTM1", u32 total length (including the
 * 16-byte header), nonzero u32 request id, u16 kind, zero u16 reserved.
 * HELLO (kind 1, 32 bytes): u16 min/max version, u32 supported/required
 * capabilities, u16 numeric schema, zero u16 reserved.
 * ACK (kind 2, 24 bytes): u16 version/schema, u32 selected capabilities.
 *
 * There are intentionally no credentials or gameplay messages. A future remote
 * transport MUST authenticate the server and protect credentials with TLS (or
 * equivalent) before adding login. Native XOR and NativeClientIntegrity are not
 * encryption/authentication substitutes. Transport deadlines are caller-owned.
 */
final class ModernSessionProtocol {
    static final int VERSION = 1;
    static final int MAX_VERSION = 47;
    // Modern schema namespace: never read NumericBreakthrough's JVM-wide mode.
    // Commits future stat fields to explicit signed i64; no stat packet exists yet.
    static final int NUMERIC_SCHEMA_I64_V1 = 1;
    static final int CAP_EXPLICIT_I64_STATS = 1;
    static final int CAP_SOURCE_SCOPED_IDENTITIES = 2;
    static final int CAP_TOURNAMENT_NAVIGATION = 4;
    static final int CAP_CHARACTER_DATA = 8;
    static final int CAP_EQUIPMENT = 16;
    static final int CAP_ROOMS = 32;
    static final int CAP_WORLD = 64;
    static final int CAP_COMBAT = 128;
    static final int CAP_GM_BOTS = 256;
    static final int CAP_SHOP = 512;
    static final int CAP_WEIGHT = 1024;
    static final int CAP_TIMED = 2048;
    static final int CAP_ARROW = 4096;
    static final int CAP_REGEN = 8192;
    static final int CAP_HONOR = 16384;
    static final int CAP_FIRE_STORM = 32768;
    static final int CAP_MATCH_DESCRIPTOR = 65536;
    static final int CAP_PET_RACE = 131072;
    static final int CAP_NPC = 262144;
    static final int CAP_DISINTEGRATE = 524288;
    // Server routing is opt-in; player renderer admission is still separate.
    static final int CAP_NPC_DISINTEGRATE = 1048576;
    static final int CAP_NPC_FIRE_STORM = 2097152;
    static final int CAP_CHAT = 4194304;
    static final int CAP_DIRECT_CHAT = 8388608;
    static final int CAP_FILTERED_CHAT = 16777216;
    static final int CAP_MATCHMAKING = 33554432;
    static final int CAP_LARGE_MAPS = 67108864;
    /** Explicit read-only canonical observations; not equipment SET permission. */
    static final int CAP_BATTLE_DATA = 134217728;
    static final int CAP_ACTION_LIFE = 268435456;
    static final int CAP_GREATER_HASTE = 536870912;
    static final int CAP_HASTE = 1073741824;
    /** v46 unsigned high bit: full speed/material DATA, not cast authority. */
    static final int CAP_SLOW_DATA = 0x80000000;
    static final int KNOWN_CAPABILITIES = CAP_EXPLICIT_I64_STATS | CAP_SOURCE_SCOPED_IDENTITIES
            | CAP_TOURNAMENT_NAVIGATION | CAP_CHARACTER_DATA | CAP_EQUIPMENT | CAP_ROOMS | CAP_WORLD | CAP_COMBAT | CAP_GM_BOTS | CAP_SHOP | CAP_WEIGHT | CAP_TIMED | CAP_ARROW | CAP_REGEN | CAP_HONOR | CAP_FIRE_STORM | CAP_MATCH_DESCRIPTOR | CAP_PET_RACE | CAP_NPC | CAP_DISINTEGRATE | CAP_NPC_DISINTEGRATE | CAP_NPC_FIRE_STORM | CAP_CHAT | CAP_DIRECT_CHAT | CAP_FILTERED_CHAT | CAP_MATCHMAKING | CAP_LARGE_MAPS | CAP_BATTLE_DATA | CAP_ACTION_LIFE | CAP_GREATER_HASTE | CAP_HASTE | CAP_SLOW_DATA;
    // NPC codec is not yet a production encounter provider/renderer admission.
    static final int RUNTIME_CAPABILITIES = KNOWN_CAPABILITIES & ~(CAP_NPC|CAP_DISINTEGRATE|CAP_NPC_DISINTEGRATE|CAP_NPC_FIRE_STORM|CAP_CHAT|CAP_DIRECT_CHAT|CAP_FILTERED_CHAT|CAP_MATCHMAKING|CAP_LARGE_MAPS|CAP_BATTLE_DATA|CAP_ACTION_LIFE|CAP_GREATER_HASTE|CAP_HASTE|CAP_SLOW_DATA);
    static final int HEADER_BYTES = 16;
    static final int MAX_FRAME_BYTES = 40;
    private static final byte[] MAGIC = {'L', 'T', 'M', '1'};
    private static final int HELLO_KIND = 1;
    private static final int ACK_KIND = 2;

    private ModernSessionProtocol() {}

    enum ErrorCode {
        MALFORMED_FRAME, UNSUPPORTED_VERSION, UNSUPPORTED_CAPABILITY,
        UNSUPPORTED_SCHEMA, UNEXPECTED_MESSAGE, REQUEST_ID_MISMATCH, CLOSED
    }

    static final class ProtocolException extends IOException {
        private final ErrorCode code;
        ProtocolException(ErrorCode code, String detail) {
            super(detail);
            this.code = code;
        }
        ErrorCode code() { return code; }
    }

    sealed interface Message permits Hello, Ack, ExtendedHello, ExtendedAck { int requestId(); }

    // int requestId carries all u32 bit patterns. Only zero is forbidden.
    record Hello(int requestId, int minVersion, int maxVersion,
                 int supportedCapabilities, int requiredCapabilities,
                 int numericSchema) implements Message {}

    record Ack(int requestId, int version, int numericSchema,
               int capabilities) implements Message {}

    /** Distinct kind/size: never silently reinterpret a legacy HELLO/ACK. */
    record ExtendedHello(Hello base,ModernSessionExtensions.Offer extensions) implements Message {
        ExtendedHello {java.util.Objects.requireNonNull(base);java.util.Objects.requireNonNull(extensions);}
        @Override public int requestId(){return base.requestId();}
    }
    record ExtendedAck(Ack base,int extensions) implements Message {
        ExtendedAck {java.util.Objects.requireNonNull(base);}
        @Override public int requestId(){return base.requestId();}
    }

    static byte[] encode(Message message) throws ProtocolException {
        if (message == null) throw malformed("null message");
        validate(message);
        if(message instanceof ExtendedHello h) {
            var b=ByteBuffer.allocate(40).put(encode(h.base()));
            b.putInt(4,40).putShort(12,(short)ModernSessionExtensions.HELLO_KIND);
            b.putInt(h.extensions().supported()).putInt(h.extensions().required());return b.array();
        }
        if(message instanceof ExtendedAck a) {
            var b=ByteBuffer.allocate(28).put(encode(a.base()));
            b.putInt(4,28).putShort(12,(short)ModernSessionExtensions.ACK_KIND);
            b.putInt(a.extensions());return b.array();
        }
        int size = message instanceof Hello ? 32 : 24;
        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
        buffer.put(MAGIC).putInt(size).putInt(message.requestId());
        buffer.putShort((short) (message instanceof Hello ? HELLO_KIND : ACK_KIND));
        buffer.putShort((short) 0);
        if (message instanceof Hello hello) {
            if(hello.maxVersion()<44&&(hello.supportedCapabilities()&CAP_HASTE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"haste state requires version forty-four");
            if(hello.maxVersion()<37&&(hello.supportedCapabilities()&CAP_ACTION_LIFE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"battle actions require version thirty-seven");
            if(hello.maxVersion()<27&&(hello.supportedCapabilities()&CAP_DISINTEGRATE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"Disintegrate requires version twenty-seven");
            if(hello.maxVersion()<17&&(hello.supportedCapabilities()&CAP_NPC)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"NPC capability requires version seventeen");
            buffer.putShort((short) hello.minVersion()).putShort((short) hello.maxVersion());
            buffer.putInt(hello.supportedCapabilities()).putInt(hello.requiredCapabilities());
            buffer.putShort((short) hello.numericSchema()).putShort((short) 0);
        } else if (message instanceof Ack ack) {
            if(ack.version()<44&&(ack.capabilities()&CAP_HASTE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"haste state requires version forty-four");
            if(ack.version()<37&&(ack.capabilities()&CAP_ACTION_LIFE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"battle actions require version thirty-seven");
            buffer.putShort((short) ack.version()).putShort((short) ack.numericSchema());
            buffer.putInt(ack.capabilities());
        }
        return buffer.array();
    }

    static Message decode(byte[] frame) throws ProtocolException {
        if (frame == null || frame.length < HEADER_BYTES || frame.length > MAX_FRAME_BYTES) {
            throw malformed("frame outside handshake bounds");
        }
        int expected = validateHeader(frame);
        if (frame.length != expected) throw malformed("truncated frame or trailing bytes");
        int outerKind=Short.toUnsignedInt(ByteBuffer.wrap(frame).getShort(12));
        if(outerKind==ModernSessionExtensions.HELLO_KIND||outerKind==ModernSessionExtensions.ACK_KIND) {
            boolean hello=outerKind==ModernSessionExtensions.HELLO_KIND;int baseSize=hello?32:24;
            var base=Arrays.copyOf(frame,baseSize);ByteBuffer.wrap(base).putInt(4,baseSize).putShort(12,(short)(hello?HELLO_KIND:ACK_KIND));
            Message extended;
            try{extended=hello?new ExtendedHello((Hello)decode(base),new ModernSessionExtensions.Offer(ByteBuffer.wrap(frame).getInt(32),ByteBuffer.wrap(frame).getInt(36)))
                    :new ExtendedAck((Ack)decode(base),ByteBuffer.wrap(frame).getInt(24));}
            catch(IllegalArgumentException invalid){throw malformed("invalid extension offer");}
            validate(extended);return extended;
        }
        ByteBuffer buffer = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
        buffer.position(8);
        int requestId = buffer.getInt();
        int kind = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        Message result;
        if (kind == HELLO_KIND) {
            result = new Hello(requestId, Short.toUnsignedInt(buffer.getShort()),
                    Short.toUnsignedInt(buffer.getShort()), buffer.getInt(), buffer.getInt(),
                    Short.toUnsignedInt(buffer.getShort()));
            if (buffer.getShort() != 0) throw malformed("nonzero hello reserved field");
        } else {
            result = new Ack(requestId, Short.toUnsignedInt(buffer.getShort()),
                    Short.toUnsignedInt(buffer.getShort()), buffer.getInt());
        }
        validate(result);
        return result;
    }

    /** Reads exactly one frame; coalesced following frames stay in the stream. */
    static byte[] readFrame(InputStream input) throws IOException {
        byte[] header = new byte[HEADER_BYTES];
        readExactly(input, header, 0, MAGIC.length);
        if (!Arrays.equals(MAGIC, Arrays.copyOf(header, MAGIC.length))) {
            throw malformed("not a modern LTM1 frame");
        }
        readExactly(input, header, MAGIC.length, HEADER_BYTES - MAGIC.length);
        int size = validateHeader(header); // Validate before peer-sized allocation/read.
        byte[] frame = Arrays.copyOf(header, size);
        readExactly(input, frame, HEADER_BYTES, size - HEADER_BYTES);
        decode(frame);
        return frame;
    }

    static void writeFrame(OutputStream output, Message message) throws IOException {
        output.write(encode(message));
    }

    static void validateCapabilities(int supported, int required) throws ProtocolException {
        int actionDependencies=CAP_BATTLE_DATA|CAP_WORLD|CAP_MATCH_DESCRIPTOR;
        if((supported&CAP_ACTION_LIFE)!=0&&(supported&actionDependencies)!=actionDependencies
                ||(required&CAP_ACTION_LIFE)!=0&&(required&actionDependencies)!=actionDependencies)
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"battle actions require canonical world identity");
        int battleDependencies=CAP_WORLD|CAP_MATCH_DESCRIPTOR;
        if((supported&CAP_BATTLE_DATA)!=0&&(supported&battleDependencies)!=battleDependencies
                ||(required&CAP_BATTLE_DATA)!=0&&(required&battleDependencies)!=battleDependencies)
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"battle data requires world and descriptor");
        int largeDependencies=CAP_WORLD|CAP_MATCH_DESCRIPTOR;
        if((supported&CAP_LARGE_MAPS)!=0&&(supported&largeDependencies)!=largeDependencies
                ||(required&CAP_LARGE_MAPS)!=0&&(required&largeDependencies)!=largeDependencies)
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"large maps require qualified world and descriptor");
        int matchmakingDependencies=CAP_EXPLICIT_I64_STATS|CAP_SOURCE_SCOPED_IDENTITIES|CAP_EQUIPMENT|CAP_WORLD|CAP_MATCH_DESCRIPTOR;
        if((supported&CAP_MATCHMAKING)!=0&&(supported&matchmakingDependencies)!=matchmakingDependencies
                ||(required&CAP_MATCHMAKING)!=0&&(required&matchmakingDependencies)!=matchmakingDependencies)
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"matchmaking requires qualified world, equipment and record identity");
        if((supported&CAP_FILTERED_CHAT)!=0&&(supported&CAP_DIRECT_CHAT)==0||(required&CAP_FILTERED_CHAT)!=0&&(required&CAP_DIRECT_CHAT)==0)
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"filtered chat requires qualified direct preferences");
        if((supported&CAP_DIRECT_CHAT)!=0&&(supported&CAP_CHAT)==0||(required&CAP_DIRECT_CHAT)!=0&&(required&CAP_CHAT)==0)
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"direct chat requires chat membership");
        int chatDependencies = CAP_EXPLICIT_I64_STATS | CAP_SOURCE_SCOPED_IDENTITIES | CAP_TOURNAMENT_NAVIGATION | CAP_ROOMS;
        if ((supported & CAP_CHAT) != 0 && (supported & chatDependencies) != chatDependencies
                || (required & CAP_CHAT) != 0 && (required & chatDependencies) != chatDependencies)
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"chat requires qualified room navigation");
        if ((supported & ~KNOWN_CAPABILITIES) != 0 || (required & ~supported) != 0
                || !equipmentDependencies(supported) || !equipmentDependencies(required)
                || !roomDependencies(supported) || !roomDependencies(required)
                || !worldDependencies(supported) || !worldDependencies(required)
                || !combatDependencies(supported) || !combatDependencies(required)
                || !gmDependencies(supported) || !gmDependencies(required)
                || !shopDependencies(supported) || !shopDependencies(required)
                || !weightDependencies(supported) || !weightDependencies(required)
                || !timedDependencies(supported) || !timedDependencies(required)||!arrowDependencies(supported)||!arrowDependencies(required)
                ||!regenDependencies(supported)||!regenDependencies(required)||!honorDependencies(supported)||!honorDependencies(required)
                ||!fireStormDependencies(supported)||!fireStormDependencies(required)
                ||!descriptorDependencies(supported)||!descriptorDependencies(required)
                ||!raceDependencies(supported)||!raceDependencies(required)
                ||!npcDependencies(supported)||!npcDependencies(required)
                ||!disintegrateDependencies(supported)||!disintegrateDependencies(required)||!npcDisintegrateDependencies(supported)||!npcDisintegrateDependencies(required)
                ||!npcFireStormDependencies(supported)||!npcFireStormDependencies(required)
                ||!greaterHasteDependencies(supported)||!greaterHasteDependencies(required)) {
            throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,
                    "unknown capability or required capability not offered");
        }
    }

    private static boolean equipmentDependencies(int capabilities) {
        int dependencies = CAP_TOURNAMENT_NAVIGATION | CAP_CHARACTER_DATA;
        return (capabilities & CAP_EQUIPMENT) == 0 || (capabilities & dependencies) == dependencies;
    }
    private static boolean greaterHasteDependencies(int capabilities) {
        int required=CAP_TIMED|CAP_BATTLE_DATA|CAP_ACTION_LIFE|CAP_MATCH_DESCRIPTOR;
        return (capabilities&(CAP_GREATER_HASTE|CAP_HASTE|CAP_SLOW_DATA))==0||(capabilities&required)==required;
    }

    private static boolean roomDependencies(int capabilities) {
        return (capabilities & CAP_ROOMS) == 0 || (capabilities & CAP_TOURNAMENT_NAVIGATION) != 0;
    }
    private static boolean gmDependencies(int capabilities) {
        return (capabilities & CAP_GM_BOTS)==0 || (capabilities & CAP_ROOMS)!=0;
    }
    private static boolean shopDependencies(int capabilities) {
        int dependencies=CAP_TOURNAMENT_NAVIGATION|CAP_ROOMS|CAP_WORLD|CAP_COMBAT;
        return (capabilities&CAP_SHOP)==0 || (capabilities&dependencies)==dependencies;
    }
    private static boolean weightDependencies(int capabilities) {
        int dependencies=CAP_SHOP|CAP_COMBAT;
        return (capabilities&CAP_WEIGHT)==0 || (capabilities&dependencies)==dependencies;
    }
    private static boolean timedDependencies(int capabilities) {
        int dependencies=CAP_WEIGHT|CAP_SHOP|CAP_COMBAT;
        return (capabilities&CAP_TIMED)==0 || (capabilities&dependencies)==dependencies;
    }
    private static boolean arrowDependencies(int capabilities) {
        int dependencies=CAP_TIMED|CAP_WEIGHT|CAP_SHOP|CAP_COMBAT;
        return (capabilities&CAP_ARROW)==0||(capabilities&dependencies)==dependencies;
    }
    private static boolean regenDependencies(int capabilities) {
        int dependencies=CAP_WEIGHT|CAP_COMBAT;return (capabilities&CAP_REGEN)==0||(capabilities&dependencies)==dependencies;
    }
    private static boolean honorDependencies(int capabilities) {
        int dependencies=CAP_REGEN|CAP_WEIGHT|CAP_COMBAT;return (capabilities&CAP_HONOR)==0||(capabilities&dependencies)==dependencies;
    }
    private static boolean fireStormDependencies(int capabilities) {
        int dependencies=CAP_REGEN|CAP_WEIGHT|CAP_COMBAT;
        return (capabilities&CAP_FIRE_STORM)==0||(capabilities&dependencies)==dependencies;
    }
    private static boolean worldDependencies(int capabilities) {
        int dependencies=CAP_ROOMS | CAP_TOURNAMENT_NAVIGATION;
        return (capabilities & CAP_WORLD)==0 || (capabilities & dependencies)==dependencies;
    }
    private static boolean descriptorDependencies(int capabilities){
        return (capabilities&CAP_MATCH_DESCRIPTOR)==0 || (capabilities&CAP_WORLD)!=0;
    }
    private static boolean raceDependencies(int capabilities){
        int required=CAP_WORLD|CAP_MATCH_DESCRIPTOR;
        return (capabilities&CAP_PET_RACE)==0||(capabilities&required)==required;
    }
    private static boolean npcDependencies(int capabilities){
        int required=CAP_COMBAT|CAP_MATCH_DESCRIPTOR;
        return (capabilities&CAP_NPC)==0||(capabilities&required)==required;
    }
    private static boolean npcFireStormDependencies(int capabilities){
        int required=CAP_NPC|CAP_REGEN|CAP_MATCH_DESCRIPTOR;
        return (capabilities&CAP_NPC_FIRE_STORM)==0||(capabilities&required)==required;
    }
    private static boolean npcDisintegrateDependencies(int capabilities){
        int required=CAP_NPC|CAP_REGEN|CAP_MATCH_DESCRIPTOR;
        return (capabilities&CAP_NPC_DISINTEGRATE)==0||(capabilities&required)==required;
    }
    private static boolean disintegrateDependencies(int capabilities){
        int required=CAP_REGEN|CAP_MATCH_DESCRIPTOR;
        return (capabilities&CAP_DISINTEGRATE)==0||(capabilities&required)==required;
    }
    private static boolean combatDependencies(int capabilities) {
        int dependencies=CAP_EXPLICIT_I64_STATS | CAP_SOURCE_SCOPED_IDENTITIES | CAP_TOURNAMENT_NAVIGATION
                | CAP_CHARACTER_DATA | CAP_EQUIPMENT | CAP_ROOMS | CAP_WORLD;
        return (capabilities & CAP_COMBAT)==0 || (capabilities & dependencies)==dependencies;
    }

    private static void validate(Message message) throws ProtocolException {
        if (message.requestId() == 0) throw malformed("zero request id");
        if(message instanceof ExtendedHello h){validate(h.base());ModernSessionExtensions.validateOffer(h.base(),h.extensions());return;}
        if(message instanceof ExtendedAck a){validate(a.base());ModernSessionExtensions.validateSelected(a.base(),a.extensions());return;}
        if (message instanceof Hello hello) {
            if(hello.maxVersion()<46&&(hello.supportedCapabilities()&CAP_SLOW_DATA)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"speed/material data requires version forty-six");
            if(hello.maxVersion()<42&&(hello.supportedCapabilities()&CAP_GREATER_HASTE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"greater haste requires version forty-two");
            if(hello.maxVersion()<35&&(hello.supportedCapabilities()&CAP_BATTLE_DATA)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"battle data requires version thirty-five");
            if(hello.maxVersion()<34&&(hello.supportedCapabilities()&CAP_LARGE_MAPS)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"large maps require version thirty-four");
            if(hello.maxVersion()<34&&(hello.supportedCapabilities()&CAP_MATCHMAKING)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"matchmaking requires version thirty-four");
            if(hello.maxVersion()<33&&(hello.supportedCapabilities()&CAP_FILTERED_CHAT)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"filtered chat requires version thirty-three");
            if(hello.maxVersion()<32&&(hello.supportedCapabilities()&CAP_CHAT)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"chat requires version thirty-two");
            if(hello.maxVersion()<31&&(hello.supportedCapabilities()&CAP_NPC_FIRE_STORM)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"NPC70 requires version thirty-one");
            if(hello.maxVersion()<29&&(hello.supportedCapabilities()&CAP_NPC_DISINTEGRATE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"NPC77 requires version twenty-nine");
            if (!u16Nonzero(hello.minVersion()) || !u16Nonzero(hello.maxVersion())
                    || hello.minVersion() > hello.maxVersion()) {
                throw malformed("invalid version interval");
            }
            if (!u16Nonzero(hello.numericSchema())) throw malformed("invalid numeric schema");
            validateCapabilities(hello.supportedCapabilities(), hello.requiredCapabilities());
        } else if (message instanceof Ack ack) {
            if(ack.version()<46&&(ack.capabilities()&CAP_SLOW_DATA)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"speed/material data requires version forty-six");
            if(ack.version()<42&&(ack.capabilities()&CAP_GREATER_HASTE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"greater haste requires version forty-two");
            if(ack.version()<35&&(ack.capabilities()&CAP_BATTLE_DATA)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"battle data requires version thirty-five");
            if(ack.version()<34&&(ack.capabilities()&CAP_LARGE_MAPS)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"large maps require version thirty-four");
            if(ack.version()<34&&(ack.capabilities()&CAP_MATCHMAKING)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"matchmaking requires version thirty-four");
            if(ack.version()<33&&(ack.capabilities()&CAP_FILTERED_CHAT)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"filtered chat requires version thirty-three");
            if(ack.version()<32&&(ack.capabilities()&CAP_CHAT)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"chat requires version thirty-two");
            if(ack.version()<31&&(ack.capabilities()&CAP_NPC_FIRE_STORM)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"NPC70 requires version thirty-one");
            if(ack.version()<29&&(ack.capabilities()&CAP_NPC_DISINTEGRATE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"NPC77 requires version twenty-nine");
            if(ack.version()<27&&(ack.capabilities()&CAP_DISINTEGRATE)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"Disintegrate requires version twenty-seven");
            if(ack.version()<17&&(ack.capabilities()&CAP_NPC)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"NPC capability requires version seventeen");
            if(ack.version()<16&&(ack.capabilities()&CAP_PET_RACE)!=0)
                throw malformed("race capability needs version sixteen");
            if(ack.version()<15&&(ack.capabilities()&CAP_MATCH_DESCRIPTOR)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"match descriptor requires version fifteen");
            if (!u16Nonzero(ack.version()) || !u16Nonzero(ack.numericSchema())) {
                throw malformed("invalid selected version/schema");
            }
            validateCapabilities(ack.capabilities(), 0);
            if(ack.version()<7 && (ack.capabilities() & CAP_GM_BOTS)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"GM bots require version 7");
            if(ack.version()<8 && (ack.capabilities() & CAP_SHOP)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"shop requires version 8");
            if(ack.version()<9 && (ack.capabilities() & CAP_WEIGHT)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"weight requires version 9");
            if(ack.version()<10 && (ack.capabilities() & CAP_TIMED)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"timed items require version 10");
            if(ack.version()<11&&(ack.capabilities()&CAP_ARROW)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"arrows require version 11");
            if(ack.version()<12&&(ack.capabilities()&CAP_REGEN)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"natural regeneration requires version 12");
            if(ack.version()<13&&(ack.capabilities()&CAP_HONOR)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"Honor requires version 13");
            if(ack.version()<14&&(ack.capabilities()&CAP_FIRE_STORM)!=0)
                throw new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,"Fire Storm requires version 14");
        }
    }

    private static boolean u16Nonzero(int value) { return value > 0 && value <= 0xffff; }

    private static int validateHeader(byte[] header) throws ProtocolException {
        for (int i = 0; i < MAGIC.length; i++) {
            if (header[i] != MAGIC[i]) throw malformed("not a modern LTM1 frame");
        }
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN);
        buffer.position(4);
        long size = Integer.toUnsignedLong(buffer.getInt());
        if (size < HEADER_BYTES || size > MAX_FRAME_BYTES) throw malformed("oversize/undersize frame");
        if (buffer.getInt() == 0) throw malformed("zero request id");
        int kind = Short.toUnsignedInt(buffer.getShort());
        if (buffer.getShort() != 0) throw malformed("nonzero header reserved field");
        int expected;
        if (kind == ModernSessionExtensions.HELLO_KIND) expected = 40;
        else if (kind == ModernSessionExtensions.ACK_KIND) expected = 28;
        else if (kind == HELLO_KIND) expected = 32;
        else if (kind == ACK_KIND) expected = 24;
        else throw new ProtocolException(ErrorCode.UNEXPECTED_MESSAGE, "unknown handshake message kind");
        if (size != expected) throw malformed("wrong message length");
        return expected;
    }

    private static void readExactly(InputStream input, byte[] target, int offset, int count)
            throws IOException {
        int end = offset + count;
        while (offset < end) {
            int read = input.read(target, offset, end - offset);
            if (read < 0) throw malformed("truncated stream frame");
            if (read == 0) { // Do not spin if an adapter returns a zero-length read.
                int single = input.read();
                if (single < 0) throw malformed("truncated stream frame");
                target[offset++] = (byte) single;
            } else {
                offset += read;
            }
        }
    }

    private static ProtocolException malformed(String detail) {
        return new ProtocolException(ErrorCode.MALFORMED_FRAME, detail);
    }
}
