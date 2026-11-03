package local.lito381.server;

/** New opt-in entry point; preserves the existing v42 and legacy fixture contracts. */
public final class ModernOrdinaryMapRotationTestPeer {
    private ModernOrdinaryMapRotationTestPeer() {}
    public static void main(String[] args) {
        if(args.length!=4 || !(args[2].equals("rotation")
                || ModernMapRotationTestPeer.isSharedRotation(args[2])))
            throw new IllegalArgumentException("Explicit qualified map pair required");
        ModernCombatTestPeer.ordinaryRotation(args);
    }
}
