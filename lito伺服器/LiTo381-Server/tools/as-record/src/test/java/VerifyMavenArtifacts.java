import java.net.URLClassLoader;
import java.nio.file.Path;

/** 使用 platform parent，確保 record 真的從指定 JAR 載入。 */
class VerifyMavenArtifacts {
    public static void main(String[] args) throws Exception {
        try (var loader = new URLClassLoader(new java.net.URL[]{Path.of(args[0]).toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> c = Class.forName(args[1], true, loader);
            if (!c.isRecord() || c.getSuperclass() != Record.class) throw new AssertionError("not a record");
            var components = c.getRecordComponents();
            if (components.length != 1 || !components[0].getName().equals("id")
                    || components[0].getType() != int.class) throw new AssertionError("components");
            Object left = c.getConstructor(int.class).newInstance(7);
            Object right = c.getConstructor(int.class).newInstance(7);
            var extra = c.getDeclaredField("extra");
            extra.setAccessible(true);
            extra.setInt(left, 19);
            if (extra.getInt(left) != 19 || !left.equals(right) || left.hashCode() != right.hashCode()
                    || !left.toString().endsWith("[id=7]")) throw new AssertionError("record behavior");
            for (String forbidden : new String[]{"org.objectweb.asm.ClassReader",
                    "com.lineage.server.model.proc.ProcRandomHelper", "local.lito381.asrecord.AsRecord"}) {
                try {
                    Class.forName(forbidden, false, loader);
                    throw new AssertionError("runtime leaked: " + forbidden);
                } catch (ClassNotFoundException expected) { }
            }
            System.out.println("JAR RECORD PASS: " + c.getName());
        }
    }
}
