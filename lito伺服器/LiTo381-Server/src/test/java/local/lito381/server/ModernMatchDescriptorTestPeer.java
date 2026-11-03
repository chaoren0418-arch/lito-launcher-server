package local.lito381.server;

/** Owned two-source, same-TLS scene/Honor/scene regression; no production defaults. */
public final class ModernMatchDescriptorTestPeer {
    private ModernMatchDescriptorTestPeer(){}
    public static void main(String[] args){
        if(args.length!=4||!java.util.Set.of("descriptor-honor","descriptor-legacy").contains(args[2]))throw new IllegalArgumentException();
        ModernCombatTestPeer.main(args);
    }
}
