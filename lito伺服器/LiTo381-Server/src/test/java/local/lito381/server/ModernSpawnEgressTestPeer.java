package local.lito381.server;

/** Opt-in source-only fixture. One authenticated Sun owner, passive GM bots,
 * real pinned 514 or complete 532, v13, MP zero and no learned spells. The Rust
 * owner must walk beyond the entire initial roster halo using only MOVE, then
 * World.LEAVE/Rooms.LEAVE/close. SQL must still contain zero result/total rows.
 * Uses the existing GO-gated, owned process-tree/isolated-InnoDB lifecycle. */
public final class ModernSpawnEgressTestPeer {
    private ModernSpawnEgressTestPeer() {}
    static int botCount(String mode) {
        return switch(mode) { case "spawn3"->3;case "spawn8"->8;case "spawn20","spawn-honor20"->20;default->0; };
    }
    public static void main(String[] args) {
        if(args.length!=4||botCount(args[2])==0)throw new IllegalArgumentException("invalid spawn fixture mode");
        ModernCombatTestPeer.main(args);
    }
}
