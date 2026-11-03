import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.objectweb.asm.Opcodes.*;

/** Static archive graph including lambda implementation handles; no application loading. */
public final class CloseoutSkillCensus {
    static final String P = "local/lito381/server/";
    static void edge(List<String> rows, String owner, MethodNode method, String kind, String to, String name, String desc) {
        if (to.startsWith(P)) rows.add(owner + "\t" + method.name + "\t" + method.desc + "\t" + kind + "\t" + to + "\t" + name + "\t" + desc);
    }
    static void dragonReflection(ClassNode c, List<String> edges) {
        if (!c.name.equals(P + "DragonSkillRequests")) return;
        boolean stored = false; int invoked = 0;
        for (var m : c.methods) {
            var nodes = Arrays.stream(m.instructions.toArray()).filter(n -> n.getOpcode() >= 0).toList();
            for (int i = 9; i < nodes.size(); i++) if (m.name.equals("<clinit>") && nodes.get(i) instanceof FieldInsnNode f
                    && f.getOpcode() == PUTSTATIC && f.owner.equals(c.name) && f.name.equals("LEGACY")) {
                stored = nodes.get(i-9) instanceof LdcInsnNode owner && Type.getObjectType(P + "ClientSession").equals(owner.cst)
                        && nodes.get(i-8) instanceof LdcInsnNode name && name.cst.equals("useSpellBeforeDragonTargets")
                        && nodes.get(i-7).getOpcode() == ICONST_1
                        && nodes.get(i-6) instanceof TypeInsnNode array && array.desc.equals("java/lang/Class") && array.getOpcode() == ANEWARRAY
                        && nodes.get(i-5).getOpcode() == DUP && nodes.get(i-4).getOpcode() == ICONST_0
                        && nodes.get(i-3) instanceof LdcInsnNode arg && Type.getType("[B").equals(arg.cst)
                        && nodes.get(i-2).getOpcode() == AASTORE
                        && nodes.get(i-1) instanceof MethodInsnNode lookup && lookup.owner.equals("java/lang/Class") && lookup.name.equals("getDeclaredMethod");
            }
            for (int i = 8; i < nodes.size(); i++) if (m.name.equals("dispatch") && nodes.get(i) instanceof MethodInsnNode call
                    && call.owner.equals("java/lang/reflect/Method") && call.name.equals("invoke")) {
                if (!(nodes.get(i-8) instanceof FieldInsnNode f && f.getOpcode() == GETSTATIC && f.owner.equals(c.name) && f.name.equals("LEGACY")
                        && nodes.get(i-7) instanceof VarInsnNode receiver && receiver.getOpcode() == ALOAD && receiver.var == 0
                        && nodes.get(i-6).getOpcode() == ICONST_1
                        && nodes.get(i-5) instanceof TypeInsnNode array && array.getOpcode() == ANEWARRAY && array.desc.equals("java/lang/Object")
                        && nodes.get(i-4).getOpcode() == DUP && nodes.get(i-3).getOpcode() == ICONST_0
                        && nodes.get(i-2) instanceof VarInsnNode packet && packet.getOpcode() == ALOAD && packet.var == 3
                        && nodes.get(i-1).getOpcode() == AASTORE)) throw new AssertionError("Review Dragon LEGACY invocation");
                edge(edges, c.name, m, "verified-field-reflection", P + "ClientSession", "useSpellBeforeDragonTargets", "([B)V"); invoked++;
            }
        }
        if (!stored || invoked != 1) throw new AssertionError("Review exact Dragon reflection bridge");
    }
    public static void main(String[] args) throws Exception {
        var methods = new ArrayList<String>(); var edges = new ArrayList<String>(); var npcFields = new ArrayList<String>();
        try (var jar = new JarFile(args[0])) {
            for (var entry : Collections.list(jar.entries())) if (entry.getName().startsWith(P) && entry.getName().endsWith(".class")) {
                var c = new ClassNode();
                try (var in = jar.getInputStream(entry)) { new ClassReader(in).accept(c, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES); }
                dragonReflection(c, edges);
                for (var m : c.methods) {
                    methods.add(c.name + "\t" + m.name + "\t" + m.desc);
                    if (c.name.equals(P + "BattleTimeAttackMagicSkillCatalog$Definition")
                            && Set.of("mpCost", "baseDamage", "diceCount", "dieSides", "reuseDelayMillis").contains(m.name) && m.desc.equals("()I")) {
                        var ops = Arrays.stream(m.instructions.toArray()).filter(n -> n.getOpcode() >= 0).toList();
                        boolean direct = ops.size() == 3 && ops.get(0) instanceof VarInsnNode load && load.getOpcode() == ALOAD && load.var == 0
                                && ops.get(1) instanceof FieldInsnNode field && field.getOpcode() == GETFIELD && field.owner.equals(c.name) && field.name.equals(m.name)
                                && ops.get(2).getOpcode() == IRETURN;
                        npcFields.add(c.name + "\t" + m.name + "\t" + m.desc + "\t" + direct);
                    }
                    for (var n : m.instructions) {
                        if (n instanceof MethodInsnNode call) edge(edges, c.name, m, "direct", call.owner, call.name, call.desc);
                        else if (n instanceof InvokeDynamicInsnNode indy)
                            for (var arg : indy.bsmArgs) if (arg instanceof Handle h)
                                edge(edges, c.name, m, "implementation-handle", h.getOwner(), h.getName(), h.getDesc());
                    }
                }
            }
        }
        Collections.sort(methods); Collections.sort(edges);
        methods.add(0, "owner\tmethod\tdescriptor");
        edges.add(0, "owner\tmethod\tdescriptor\tkind\tcallee_owner\tcallee\tcallee_descriptor");
        Files.write(Path.of(args[1]), methods); Files.write(Path.of(args[2]), edges);
        if (args.length > 3) { Collections.sort(npcFields); npcFields.add(0, "owner\tmethod\tdescriptor\tdirect_stored_field"); Files.write(Path.of(args[3]), npcFields); }
        System.out.println("Archive graph: " + (methods.size()-1) + " methods, " + (edges.size()-1) + " edges; no application execution.");
    }
}
