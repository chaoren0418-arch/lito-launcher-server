package local.lito381.server;

import java.util.Objects;
import java.util.Optional;

/**
 * One instance per modern connection. No native version/integrity probe, cipher,
 * database, account, listener, or GameHub side effects. Only wire compatibility
 * is negotiated: capabilities are untrusted declarations, not authorization or
 * evidence that assets exist. NEGOTIATED is deliberately NOT AUTHENTICATED.
 *
 * The modern listener's transport owns TLS, deadlines and
 * closing the actual socket after any error/close. Do not expose password
 * authentication over the legacy XOR transport or this unencrypted codec.
 */
final class ModernSessionBoundary {
    enum State { WAITING_FOR_HELLO, NEGOTIATED, CLOSED }

    record Policy(int supportedCapabilities, int requiredCapabilities, int maxVersion, int slowMinimumVersion) {
        Policy(int supportedCapabilities,int requiredCapabilities,int maxVersion) {
            this(supportedCapabilities,requiredCapabilities,maxVersion,46);
        }
        Policy(int supportedCapabilities,int requiredCapabilities) {
            this(supportedCapabilities,requiredCapabilities,ModernSessionProtocol.MAX_VERSION);
        }
        Policy withMaxVersion(int version) {return new Policy(supportedCapabilities,requiredCapabilities,version,slowMinimumVersion);}
        /** Opt-in until the player chat controller/UI is qualified. */
        Policy withChat() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_CHAT,requiredCapabilities,maxVersion,slowMinimumVersion);}
        Policy withDirectChat() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT,requiredCapabilities,maxVersion,slowMinimumVersion);}
        /** Explicit qualification only; ModernServer does not opt in yet. */
        Policy withFilteredChat() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT|ModernSessionProtocol.CAP_FILTERED_CHAT,requiredCapabilities,maxVersion,slowMinimumVersion);}
        /** Only after injecting qualified room/world and central-record providers. */
        Policy withMatchmaking() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_MATCHMAKING,requiredCapabilities,maxVersion,slowMinimumVersion);}
        Policy withLargeMaps() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_LARGE_MAPS,requiredCapabilities,maxVersion,slowMinimumVersion);}
        /** Read-only opt-in; production UI/advanced SET admission stays separate. */
        Policy withBattleData() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_BATTLE_DATA,requiredCapabilities,maxVersion,slowMinimumVersion);}
        Policy withBattleActions() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE,requiredCapabilities,maxVersion,slowMinimumVersion);}
        /** Requires a qualified source reader and timed, canonical world service. */
        Policy withGreaterHaste() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_GREATER_HASTE,requiredCapabilities,maxVersion,slowMinimumVersion);}
        /** State receipt only; no targeted-cast opcode is granted by this opt-in. */
        Policy withHasteData() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_HASTE,requiredCapabilities,maxVersion,slowMinimumVersion);}
        Policy withSlowData() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_SLOW_DATA,requiredCapabilities,maxVersion,slowMinimumVersion);}
        Policy withSlowCommands() {return new Policy(supportedCapabilities|ModernSessionProtocol.CAP_SLOW_DATA,requiredCapabilities,maxVersion,47);}
        Policy {
            if(slowMinimumVersion!=46&&slowMinimumVersion!=47)throw new IllegalArgumentException("invalid Slow version gate");
            if(maxVersion<slowMinimumVersion&&(requiredCapabilities&ModernSessionProtocol.CAP_SLOW_DATA)!=0)
                throw new IllegalArgumentException("speed/material data requires version forty-six");
            if(maxVersion<44&&(requiredCapabilities&ModernSessionProtocol.CAP_HASTE)!=0)
                throw new IllegalArgumentException("haste state requires version forty-four");
            if(maxVersion<42&&(requiredCapabilities&ModernSessionProtocol.CAP_GREATER_HASTE)!=0)
                throw new IllegalArgumentException("greater haste requires version forty-two");
            if(maxVersion<37&&(requiredCapabilities&ModernSessionProtocol.CAP_ACTION_LIFE)!=0)
                throw new IllegalArgumentException("battle actions require version thirty-seven");
            if(maxVersion<35&&(requiredCapabilities&ModernSessionProtocol.CAP_BATTLE_DATA)!=0)
                throw new IllegalArgumentException("required battle data needs version thirty-five");
            if(maxVersion<34&&(requiredCapabilities&ModernSessionProtocol.CAP_LARGE_MAPS)!=0)
                throw new IllegalArgumentException("required large maps need version thirty-four");
            if(maxVersion<34&&(requiredCapabilities&ModernSessionProtocol.CAP_MATCHMAKING)!=0)
                throw new IllegalArgumentException("required matchmaking capability needs version thirty-four");
            if(maxVersion<33&&(requiredCapabilities&ModernSessionProtocol.CAP_FILTERED_CHAT)!=0)
                throw new IllegalArgumentException("required filtered chat capability needs version thirty-three");
            if(maxVersion<32&&(requiredCapabilities&ModernSessionProtocol.CAP_CHAT)!=0)
                throw new IllegalArgumentException("required chat capability needs version thirty-two");
            if(maxVersion<31&&(requiredCapabilities&ModernSessionProtocol.CAP_NPC_FIRE_STORM)!=0)
                throw new IllegalArgumentException("required NPC70 capability needs version thirty-one");
            if(maxVersion<29&&(requiredCapabilities&ModernSessionProtocol.CAP_NPC_DISINTEGRATE)!=0)
                throw new IllegalArgumentException("required NPC77 capability needs version twenty-nine");
            if(maxVersion<27&&(requiredCapabilities&ModernSessionProtocol.CAP_DISINTEGRATE)!=0)
                throw new IllegalArgumentException("required Disintegrate capability needs version twenty-seven");
            if(maxVersion<17&&(requiredCapabilities&ModernSessionProtocol.CAP_NPC)!=0)
                throw new IllegalArgumentException("required NPC capability needs version seventeen");
            if(maxVersion<16&&(requiredCapabilities&ModernSessionProtocol.CAP_PET_RACE)!=0)
                throw new IllegalArgumentException("required race capability needs version sixteen");
            if(maxVersion<15&&(requiredCapabilities&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)!=0)
                throw new IllegalArgumentException("required match descriptor needs version fifteen");
            if(maxVersion<ModernSessionProtocol.VERSION || maxVersion>ModernSessionProtocol.MAX_VERSION)
                throw new IllegalArgumentException("unsupported policy protocol version");
            if(maxVersion<7 && (requiredCapabilities & ModernSessionProtocol.CAP_GM_BOTS)!=0)
                throw new IllegalArgumentException("required GM capability needs version seven");
            if(maxVersion<8 && (requiredCapabilities & ModernSessionProtocol.CAP_SHOP)!=0)
                throw new IllegalArgumentException("required shop capability needs version eight");
            if(maxVersion<9 && (requiredCapabilities & ModernSessionProtocol.CAP_WEIGHT)!=0)
                throw new IllegalArgumentException("required weight capability needs version nine");
            if(maxVersion<10 && (requiredCapabilities & ModernSessionProtocol.CAP_TIMED)!=0)
                throw new IllegalArgumentException("required timed capability needs version ten");
            if(maxVersion<11&&(requiredCapabilities&ModernSessionProtocol.CAP_ARROW)!=0)throw new IllegalArgumentException("required arrow capability needs version eleven");
            if(maxVersion<12&&(requiredCapabilities&ModernSessionProtocol.CAP_REGEN)!=0)throw new IllegalArgumentException("required regen capability needs version twelve");
            if(maxVersion<13&&(requiredCapabilities&ModernSessionProtocol.CAP_HONOR)!=0)throw new IllegalArgumentException("required Honor capability needs version thirteen");
            if(maxVersion<14&&(requiredCapabilities&ModernSessionProtocol.CAP_FIRE_STORM)!=0)throw new IllegalArgumentException("required Fire Storm capability needs version fourteen");
            try {
                ModernSessionProtocol.validateCapabilities(supportedCapabilities, requiredCapabilities);
            } catch (ModernSessionProtocol.ProtocolException error) {
                throw new IllegalArgumentException(error.getMessage(), error);
            }
            // This first schema is only meaningful when its explicit widths are accepted.
            if ((requiredCapabilities & ModernSessionProtocol.CAP_EXPLICIT_I64_STATS) == 0) {
                throw new IllegalArgumentException("modern schema requires explicit stat widths");
            }
        }

        static Policy current() {
            // A read-only server must not advertise a gear authority it lacks.
            return new Policy(ModernSessionProtocol.RUNTIME_CAPABILITIES & ~(ModernSessionProtocol.CAP_PET_RACE | ModernSessionProtocol.CAP_MATCH_DESCRIPTOR | ModernSessionProtocol.CAP_EQUIPMENT | ModernSessionProtocol.CAP_ROOMS | ModernSessionProtocol.CAP_WORLD | ModernSessionProtocol.CAP_COMBAT | ModernSessionProtocol.CAP_GM_BOTS | ModernSessionProtocol.CAP_SHOP | ModernSessionProtocol.CAP_WEIGHT | ModernSessionProtocol.CAP_TIMED | ModernSessionProtocol.CAP_ARROW | ModernSessionProtocol.CAP_REGEN | ModernSessionProtocol.CAP_HONOR | ModernSessionProtocol.CAP_FIRE_STORM),
                    ModernSessionProtocol.CAP_EXPLICIT_I64_STATS | ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES);
        }

        /** Use only for listeners with an injected equipment authority/provider. */
        static Policy equipment() {
            return new Policy(ModernSessionProtocol.RUNTIME_CAPABILITIES & ~(ModernSessionProtocol.CAP_PET_RACE | ModernSessionProtocol.CAP_MATCH_DESCRIPTOR | ModernSessionProtocol.CAP_ROOMS | ModernSessionProtocol.CAP_WORLD | ModernSessionProtocol.CAP_COMBAT | ModernSessionProtocol.CAP_GM_BOTS | ModernSessionProtocol.CAP_SHOP | ModernSessionProtocol.CAP_WEIGHT | ModernSessionProtocol.CAP_TIMED | ModernSessionProtocol.CAP_ARROW | ModernSessionProtocol.CAP_REGEN | ModernSessionProtocol.CAP_HONOR | ModernSessionProtocol.CAP_FIRE_STORM),
                    ModernSessionProtocol.CAP_EXPLICIT_I64_STATS | ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES);
        }

        static Policy rooms(boolean equipment) {
            return new Policy(ModernSessionProtocol.RUNTIME_CAPABILITIES & ~(ModernSessionProtocol.CAP_PET_RACE | ModernSessionProtocol.CAP_MATCH_DESCRIPTOR | ModernSessionProtocol.CAP_WORLD | ModernSessionProtocol.CAP_COMBAT | ModernSessionProtocol.CAP_SHOP | ModernSessionProtocol.CAP_WEIGHT | ModernSessionProtocol.CAP_TIMED | ModernSessionProtocol.CAP_ARROW | ModernSessionProtocol.CAP_REGEN | ModernSessionProtocol.CAP_HONOR | ModernSessionProtocol.CAP_FIRE_STORM) & (equipment ? -1 : ~ModernSessionProtocol.CAP_EQUIPMENT),
                    ModernSessionProtocol.CAP_EXPLICIT_I64_STATS | ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES);
        }

        static Policy world(boolean equipment) {
            return world(equipment,false);
        }
        static Policy world(boolean equipment,boolean combat) {
            return world(equipment,combat,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop) {
            return world(equipment,combat,shop,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight) {
            return world(equipment,combat,shop,weight,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight,boolean timed) {
            return world(equipment,combat,shop,weight,timed,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight,boolean timed,boolean bow) {
            return world(equipment,combat,shop,weight,timed,bow,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight,boolean timed,boolean bow,boolean regen) {
            return world(equipment,combat,shop,weight,timed,bow,regen,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight,boolean timed,boolean bow,boolean regen,boolean honor) {
            return world(equipment,combat,shop,weight,timed,bow,regen,honor,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight,boolean timed,boolean bow,boolean regen,boolean honor,boolean fireStorm) {
            return world(equipment,combat,shop,weight,timed,bow,regen,honor,fireStorm,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight,boolean timed,boolean bow,boolean regen,boolean honor,boolean fireStorm,boolean race) {
            return world(equipment,combat,shop,weight,timed,bow,regen,honor,fireStorm,race,false);
        }
        static Policy world(boolean equipment,boolean combat,boolean shop,boolean weight,boolean timed,boolean bow,boolean regen,boolean honor,boolean fireStorm,boolean race,boolean npc) {
            if(npc&&!combat)throw new IllegalArgumentException("NPC requires combat authority");
            if(combat && !equipment)throw new IllegalArgumentException("combat requires equipment authority");
            if(shop && !combat)throw new IllegalArgumentException("shop requires combat authority");
            if(weight&&!shop)throw new IllegalArgumentException("weight requires shop authority");
            if(timed&&!weight)throw new IllegalArgumentException("timed items require weight authority");
            if(bow&&!timed)throw new IllegalArgumentException("arrows require timed authority");
            if(regen&&(!weight||!combat))throw new IllegalArgumentException("regen requires weight and combat authority");
            if(honor&&!regen)throw new IllegalArgumentException("Honor requires natural regeneration authority");
            if(fireStorm&&!regen)throw new IllegalArgumentException("Fire Storm requires natural regeneration authority");
            return new Policy((ModernSessionProtocol.RUNTIME_CAPABILITIES | (npc?ModernSessionProtocol.CAP_NPC:0)) & (race?-1:~ModernSessionProtocol.CAP_PET_RACE) & (fireStorm?-1:~ModernSessionProtocol.CAP_FIRE_STORM) & (honor?-1:~ModernSessionProtocol.CAP_HONOR) & (regen?-1:~ModernSessionProtocol.CAP_REGEN) & (bow?-1:~ModernSessionProtocol.CAP_ARROW)& (timed?-1:~ModernSessionProtocol.CAP_TIMED) & (weight?-1:~ModernSessionProtocol.CAP_WEIGHT) & (shop ? -1 : ~ModernSessionProtocol.CAP_SHOP) & (combat ? -1 : ~ModernSessionProtocol.CAP_COMBAT) & (equipment ? -1 : ~ModernSessionProtocol.CAP_EQUIPMENT),
                    ModernSessionProtocol.CAP_EXPLICIT_I64_STATS | ModernSessionProtocol.CAP_SOURCE_SCOPED_IDENTITIES);
        }
    }

    record Negotiated(int requestId, int version, int numericSchema, int capabilities,int extensions) {
        Negotiated(int requestId,int version,int numericSchema,int capabilities){this(requestId,version,numericSchema,capabilities,0);}
    }

    private final Policy policy;
    private final ModernSessionExtensions.Offer extensions;
    private State state = State.WAITING_FOR_HELLO;
    private Negotiated negotiated;

    ModernSessionBoundary(Policy policy) { this(policy,ModernSessionExtensions.NONE); }
    ModernSessionBoundary(Policy policy,ModernSessionExtensions.Offer extensions) {
        this.policy=Objects.requireNonNull(policy);this.extensions=Objects.requireNonNull(extensions);
        if(extensions.supported()!=0)try {
            ModernSessionExtensions.validateOffer(new ModernSessionProtocol.Hello(1,policy.maxVersion(),policy.maxVersion(),
                    policy.supportedCapabilities(),policy.requiredCapabilities(),ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1),extensions);
        }catch(ModernSessionProtocol.ProtocolException invalid){throw new IllegalArgumentException("unqualified extension policy",invalid);}
    }

    synchronized State state() { return state; }
    synchronized Optional<Negotiated> negotiated() { return Optional.ofNullable(negotiated); }

    /** Consumes one complete HELLO and produces ACK bytes; every failure closes this boundary. */
    synchronized byte[] acceptHello(byte[] frame) throws ModernSessionProtocol.ProtocolException {
        try {
            if (state != State.WAITING_FOR_HELLO) {
                throw error(state == State.CLOSED ? ModernSessionProtocol.ErrorCode.CLOSED
                        : ModernSessionProtocol.ErrorCode.UNEXPECTED_MESSAGE,
                        "hello is accepted exactly once per connection");
            }
            var message=ModernSessionProtocol.decode(frame);
            var offered=message instanceof ModernSessionProtocol.ExtendedHello h?h.extensions():ModernSessionExtensions.NONE;
            var base=message instanceof ModernSessionProtocol.ExtendedHello h?h.base():message;
            if (!(base instanceof ModernSessionProtocol.Hello hello)) {
                throw error(ModernSessionProtocol.ErrorCode.UNEXPECTED_MESSAGE, "expected modern hello");
            }
            if (hello.minVersion() > policy.maxVersion()
                    || hello.maxVersion() < ModernSessionProtocol.VERSION) {
                throw error(ModernSessionProtocol.ErrorCode.UNSUPPORTED_VERSION, "no shared protocol version");
            }
            if (hello.numericSchema() != ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1) {
                throw error(ModernSessionProtocol.ErrorCode.UNSUPPORTED_SCHEMA, "no shared modern numeric schema");
            }
            int version = Math.min(hello.maxVersion(), policy.maxVersion());
            int selected = hello.supportedCapabilities() & policy.supportedCapabilities();
            if(version<policy.slowMinimumVersion()) selected &= ~ModernSessionProtocol.CAP_SLOW_DATA;
            if(version<44) selected &= ~ModernSessionProtocol.CAP_HASTE;
            if(version<42) selected &= ~ModernSessionProtocol.CAP_GREATER_HASTE;
            // The version is selected first. An optional new implementation
            // bit cannot leak into a version-one-through-six ACK, even when
            // both peers advertised a wider supported-capability superset.
            if(version<7) selected &= ~ModernSessionProtocol.CAP_GM_BOTS;
            if(version<8) selected &= ~ModernSessionProtocol.CAP_SHOP;
            if(version<9) selected &= ~ModernSessionProtocol.CAP_WEIGHT;
            if(version<10) selected &= ~ModernSessionProtocol.CAP_TIMED;
            if(version<11) selected &= ~ModernSessionProtocol.CAP_ARROW;
            if(version<12) selected &= ~ModernSessionProtocol.CAP_REGEN;
            if(version<13) selected &= ~ModernSessionProtocol.CAP_HONOR;
            if(version<14) selected &= ~ModernSessionProtocol.CAP_FIRE_STORM;
            if(version<15) selected &= ~ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
            if(version<16) selected &= ~ModernSessionProtocol.CAP_PET_RACE;
            if(version<17) selected &= ~ModernSessionProtocol.CAP_NPC;
            if(version<27) selected &= ~ModernSessionProtocol.CAP_DISINTEGRATE;
            if(version<29) selected &= ~ModernSessionProtocol.CAP_NPC_DISINTEGRATE;
            if(version<31) selected &= ~ModernSessionProtocol.CAP_NPC_FIRE_STORM;
            if(version<32) selected &= ~(ModernSessionProtocol.CAP_CHAT|ModernSessionProtocol.CAP_DIRECT_CHAT);
            if(version<33) selected &= ~ModernSessionProtocol.CAP_FILTERED_CHAT;
            if(version<34) selected &= ~(ModernSessionProtocol.CAP_MATCHMAKING|ModernSessionProtocol.CAP_LARGE_MAPS);
            if(version<37) selected &= ~ModernSessionProtocol.CAP_ACTION_LIFE;
            if(version<35) selected &= ~ModernSessionProtocol.CAP_BATTLE_DATA;
            int required = hello.requiredCapabilities() | policy.requiredCapabilities();
            if ((selected & required) != required) {
                throw error(ModernSessionProtocol.ErrorCode.UNSUPPORTED_CAPABILITY,
                        "required capabilities were not mutually offered");
            }
            var ack = new ModernSessionProtocol.Ack(hello.requestId(), version,
                    ModernSessionProtocol.NUMERIC_SCHEMA_I64_V1, selected);
            int extra=offered.supported()&extensions.supported();int requiredExtra=offered.required()|extensions.required();
            if((extra&requiredExtra)!=requiredExtra)throw ModernSessionExtensions.error("required extensions were not mutually offered");
            byte[] response=ModernSessionProtocol.encode(message instanceof ModernSessionProtocol.ExtendedHello?
                    new ModernSessionProtocol.ExtendedAck(ack,extra):ack);
            negotiated = new Negotiated(ack.requestId(), ack.version(), ack.numericSchema(), ack.capabilities(),extra);
            state = State.NEGOTIATED;
            return response;
        } catch (ModernSessionProtocol.ProtocolException failure) {
            close();
            throw failure;
        }
    }

    synchronized void close() {
        negotiated = null;
        state = State.CLOSED;
    }

    private static ModernSessionProtocol.ProtocolException error(
            ModernSessionProtocol.ErrorCode code, String detail) {
        return new ModernSessionProtocol.ProtocolException(code, detail);
    }
}
