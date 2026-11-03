package local.lito381.server;

import static local.lito381.server.ModernSessionProtocol.*;

/** A separate extension word negotiated by explicit kinds 80/81. The legacy
 * 32-bit word and v1..47 packet shapes are unchanged. Compatibility is not auth. */
final class ModernSessionExtensions {
    static final int BASE_VERSION=47,HELLO_KIND=80,ACK_KIND=81;
    static final int PLAYER_HEALING_DATA=1,PLAYER_HEALING_COMMANDS=2,ARMOR_PROTECTION_DATA=4,ARMOR_PRESENTATION_DATA=8,FULL_VIP_DATA=16,FULL_VIP_EQUIPMENT=32,FULL_VIP_WORLD=64,FULL_VIP_APPEARANCE=128,NATIVE_WORLD_RESOURCES_V1=256,NATIVE_CATALOG_RESOURCES_V1=512,NATIVE_ACTOR_DEPENDENCIES_V1=1024,KNOWN=2047;
    static final int NATIVE_DEPENDENCIES=CAP_EXPLICIT_I64_STATS|CAP_SOURCE_SCOPED_IDENTITIES|CAP_TOURNAMENT_NAVIGATION|CAP_CHARACTER_DATA|CAP_ROOMS|CAP_WORLD;
    static final int HEALING_MASK=PLAYER_HEALING_DATA|PLAYER_HEALING_COMMANDS;
    static final int DEPENDENCIES=CAP_EXPLICIT_I64_STATS|CAP_SOURCE_SCOPED_IDENTITIES|CAP_TOURNAMENT_NAVIGATION
            |CAP_CHARACTER_DATA|CAP_EQUIPMENT|CAP_ROOMS|CAP_WORLD|CAP_COMBAT|CAP_SHOP|CAP_WEIGHT|CAP_TIMED
            |CAP_REGEN|CAP_MATCH_DESCRIPTOR|CAP_BATTLE_DATA|CAP_ACTION_LIFE;
    record Offer(int supported,int required) {
        Offer {
            if((supported&~KNOWN)!=0||(required&~supported)!=0||!closed(supported)||!closed(required))
                throw new IllegalArgumentException("invalid session extensions");
        }
    }
    static final Offer NONE=new Offer(0,0);
    static final Offer HEALING=new Offer(HEALING_MASK,HEALING_MASK);
    static final Offer ARMOR=new Offer(ARMOR_PROTECTION_DATA,ARMOR_PROTECTION_DATA);
    static final Offer HEALING_ARMOR=new Offer(HEALING_MASK|ARMOR_PROTECTION_DATA,HEALING_MASK|ARMOR_PROTECTION_DATA);
    static final Offer ARMOR_PRESENTATION=new Offer(ARMOR_PROTECTION_DATA|ARMOR_PRESENTATION_DATA,ARMOR_PROTECTION_DATA|ARMOR_PRESENTATION_DATA);
    static final Offer HEALING_ARMOR_PRESENTATION=new Offer(HEALING_MASK|ARMOR_PROTECTION_DATA|ARMOR_PRESENTATION_DATA,
            HEALING_MASK|ARMOR_PROTECTION_DATA|ARMOR_PRESENTATION_DATA);
    private static boolean closed(int bits){return ((bits&PLAYER_HEALING_COMMANDS)==0||(bits&PLAYER_HEALING_DATA)!=0)
            &&((bits&ARMOR_PRESENTATION_DATA)==0||(bits&ARMOR_PROTECTION_DATA)!=0)
            &&((bits&(FULL_VIP_EQUIPMENT|FULL_VIP_WORLD|FULL_VIP_APPEARANCE))==0||(bits&FULL_VIP_DATA)!=0)
            &&((bits&NATIVE_CATALOG_RESOURCES_V1)==0||(bits&NATIVE_WORLD_RESOURCES_V1)!=0)
            &&((bits&NATIVE_ACTOR_DEPENDENCIES_V1)==0||(bits&768)==768);}
    static void validateOffer(Hello base,Offer offer)throws ProtocolException {
        if(base.minVersion()!=BASE_VERSION||base.maxVersion()!=BASE_VERSION||base.numericSchema()!=NUMERIC_SCHEMA_I64_V1)
            throw error("extension handshake requires exact base v47/schema1");
        validateBits(base.supportedCapabilities(),offer.supported());
        validateBits(base.requiredCapabilities(),offer.required());
    }
    static void validateSelected(Ack base,int extensions)throws ProtocolException {
        if(base.version()!=BASE_VERSION||base.numericSchema()!=NUMERIC_SCHEMA_I64_V1)throw error("invalid extension base selection");
        validateBits(base.capabilities(),extensions);
    }
    private static void validateBits(int base,int bits)throws ProtocolException {
        if((bits&~KNOWN)!=0||!closed(bits)||((bits&255)!=0&&(base&DEPENDENCIES)!=DEPENDENCIES)
                ||(bits&NATIVE_WORLD_RESOURCES_V1)!=0&&(base&NATIVE_DEPENDENCIES)!=NATIVE_DEPENDENCIES)throw error("extension dependencies missing");
    }
    static boolean allowsNativeResources(ModernSessionBoundary.Negotiated n){return allows(n,NATIVE_WORLD_RESOURCES_V1);}
    static boolean allowsNativeActor(ModernSessionBoundary.Negotiated n){return allows(n,1792);}
    static boolean allowsNativeCatalog(ModernSessionBoundary.Negotiated n){return allows(n,NATIVE_WORLD_RESOURCES_V1|NATIVE_CATALOG_RESOURCES_V1);}
    static boolean allowsData(ModernSessionBoundary.Negotiated n){return allows(n,PLAYER_HEALING_DATA);}
    static boolean allowsVipData(ModernSessionBoundary.Negotiated n){return allows(n,FULL_VIP_DATA);}
    static boolean allowsVipAppearance(ModernSessionBoundary.Negotiated n){return allows(n,FULL_VIP_DATA|FULL_VIP_APPEARANCE);}
    static boolean allowsVipEquipment(ModernSessionBoundary.Negotiated n){return allows(n,FULL_VIP_DATA|FULL_VIP_EQUIPMENT);}
    static boolean allowsVipWorld(ModernSessionBoundary.Negotiated n){return allows(n,FULL_VIP_DATA|FULL_VIP_WORLD);}
    static boolean allowsArmor(ModernSessionBoundary.Negotiated n){return allows(n,ARMOR_PROTECTION_DATA);}
    static boolean allowsArmorPresentation(ModernSessionBoundary.Negotiated n){return allows(n,ARMOR_PROTECTION_DATA|ARMOR_PRESENTATION_DATA);}
    static boolean allowsCommands(ModernSessionBoundary.Negotiated n){return allows(n,HEALING_MASK);}
    /** Validate the complete selected context, including the original word.
     * Zero extensions retain every old version's exact semantics. */
    static void requireContext(ModernSessionBoundary.Negotiated n) {
        if(n==null)throw new IllegalArgumentException("missing session context");
        var ack=new Ack(n.requestId(),n.version(),n.numericSchema(),n.capabilities());
        try {
            ModernSessionProtocol.encode(ack);
            if(n.extensions()!=0)validateSelected(ack,n.extensions());
        }catch(ProtocolException invalid){throw new IllegalArgumentException("invalid session context",invalid);}
    }
    private static boolean allows(ModernSessionBoundary.Negotiated n,int required) {
        if(n==null||(n.extensions()&required)!=required)return false;
        try{validateSelected(new Ack(n.requestId(),n.version(),n.numericSchema(),n.capabilities()),n.extensions());return true;}
        catch(ProtocolException invalid){return false;}
    }
    static ProtocolException error(String detail){return new ProtocolException(ErrorCode.UNSUPPORTED_CAPABILITY,detail);}
    private ModernSessionExtensions(){}
}
