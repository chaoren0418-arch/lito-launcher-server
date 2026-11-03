import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class VerifyBuffPatch {
    static ClassNode read(String jar) throws Exception {
        try(ZipFile z=new ZipFile(jar)) {
            ClassNode c=new ClassNode();new ClassReader(z.getInputStream(z.getEntry("local/lito381/server/LegacyMySqlRepository.class")).readAllBytes()).accept(c,0);return c;
        }
    }
    static byte[] method(ClassNode c,MethodNode m) {
        ClassWriter w=new ClassWriter(0);w.visit(c.version,c.access,c.name,c.signature,c.superName,c.interfaces.toArray(String[]::new));m.accept(w);w.visitEnd();return w.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        ClassNode a=read(args[0]),b=read(args[1]);
        if(a.methods.size()!=b.methods.size() || a.fields.size()!=b.fields.size())throw new AssertionError("Unexpected members");
        Set<String> changed=new TreeSet<>();int unchanged=0;
        for(int i=0;i<a.methods.size();i++) {
            MethodNode x=a.methods.get(i),y=b.methods.get(i);
            if(!x.name.equals(y.name)||!x.desc.equals(y.desc))throw new AssertionError("Method order changed");
            if(Arrays.equals(method(a,x),method(b,y)))unchanged++;else changed.add(x.name+x.desc);
        }
        Set<String> expected=Set.of("healthCheck()V","loadBuffSnapshot(Ljava/sql/Connection;J)Llocal/lito381/server/RepositoryBuffSnapshot;");
        if(!changed.equals(expected))throw new AssertionError(changed);
        System.out.println("PASS changed exactly two BUFF methods; unchanged repository methods="+unchanged);
    }
}
