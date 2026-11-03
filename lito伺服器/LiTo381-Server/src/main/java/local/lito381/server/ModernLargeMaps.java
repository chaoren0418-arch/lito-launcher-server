package local.lito381.server;

/** Optional v34 world admission. Same manifest bytes, unchanged byte/texture budgets. */
final class ModernLargeMaps {
    static final int LEGACY_SECTORS=16, VERSION=34;
    private ModernLargeMaps(){}
    static boolean negotiated(ModernSessionBoundary.Negotiated peer) {
        int required=ModernSessionProtocol.CAP_LARGE_MAPS|ModernSessionProtocol.CAP_WORLD|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR;
        return peer!=null&&peer.version()>=VERSION&&(peer.capabilities()&required)==required;
    }
    static boolean supported(ModernWorldMap.Manifest manifest,ModernSessionBoundary.Negotiated peer) {
        return manifest.sectors().size()<=LEGACY_SECTORS||negotiated(peer);
    }
}
