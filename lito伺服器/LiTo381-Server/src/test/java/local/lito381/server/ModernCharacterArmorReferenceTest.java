package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Executes the two actual workspace source method bodies with only a Pc
 * identity/OriginalDex stub. Not a native executable, full classic player,
 * int-clamping, equipment, permanent-buff or client-bin equivalence claim. */
class ModernCharacterArmorReferenceTest {
    @TempDir Path directory;
    @Test void modernI64FormulaAgreesWithActualClassicMethodsAcrossAllClassesAndBoundaries()throws Exception {
        var root=System.getenv("LITO_MODERN_TEST_SERVER_ROOT");Assumptions.assumeTrue(root!=null);
        Path workspace=Path.of(root).getParent();
        Path calcPath=workspace.resolve("1-伺服器/src/com/lineage/server/utils/CalcStat.java");
        Path originalPath=workspace.resolve("1-伺服器/src/com/lineage/server/model/Instance/L1PcOriginal.java");
        var calc=Files.readString(calcPath);var original=Files.readString(originalPath);
        var login=Files.readString(workspace.resolve("1-伺服器/src/com/lineage/server/datatables/user/CharacterTable.java"));
        var load=method(login,"public L1PcInstance loadCharacter(");
        assertFalse(java.util.regex.Pattern.compile("\\brow\\.(?:ac|Ac)\\b").matcher(load).find());
        assertTrue(load.contains("pc.refresh()"));assertTrue(load.contains("pc.setOriginalDex(row.originalDex)"));
        var pc=Files.readString(workspace.resolve("1-伺服器/src/com/lineage/server/model/Instance/L1PcInstance.java"));
        assertTrue(method(pc,"public void resetBaseAc()").contains("CalcStat.calcAc(this.getLevel(), this.getOriginalDex())"));
        assertTrue(method(pc,"public void resetOriginalAc()").contains("this.addAc(0 - this._originalAc)"));
        String source="public class NativeArmorReference {\n"+method(calc,"public static int calcAc(")+"\n"
                +method(original,"public static int resetOriginalAc(")+"\n"
                +"public static int origin(int type,int dex){return resetOriginalAc(new L1PcInstance(type,dex));}\n"
                +"static class L1PcInstance { final int type,dex;L1PcInstance(int type,int dex){this.type=type;this.dex=dex;}int getOriginalDex(){return dex;}"
                +"boolean isCrown(){return type==0;}boolean isKnight(){return type==1;}boolean isElf(){return type==2;}boolean isWizard(){return type==3;}"
                +"boolean isDarkelf(){return type==4;}boolean isDragonKnight(){return type==5;}boolean isIllusionist(){return type==6;}}}";
        Path file=directory.resolve("NativeArmorReference.java");Files.writeString(file,source,StandardCharsets.UTF_8);
        var compiler=ToolProvider.getSystemJavaCompiler();assertNotNull(compiler);
        assertEquals(0,compiler.run(null,null,null,"-encoding","UTF-8","-d",directory.toString(),file.toString()));
        int cases=0;
        try(var loader=new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()},null)) {
            var type=loader.loadClass("NativeArmorReference");var base=type.getMethod("calcAc",int.class,int.class);var bonus=type.getMethod("origin",int.class,int.class);
            for(int level:new int[]{1,3,4,7,8,30,60,80,99,255,65535,Integer.MAX_VALUE})for(int cls=0;cls<7;cls++)for(int d=0;d<=61;d++) {
                int dex=d==61?Integer.MAX_VALUE:d;
                int expectedBase=(Integer)base.invoke(null,level,dex),expectedBonus=(Integer)bonus.invoke(null,cls,dex);
                assertEquals(expectedBase,ModernCharacterArmor.base(level,dex));assertEquals(expectedBonus,ModernCharacterArmor.originalBonus(cls,dex));
                assertEquals((long)expectedBase-expectedBonus,ModernCharacterArmor.intrinsic(cls,level,dex));cases++;
            }
        }
        assertEquals(5208,cases);
        System.err.println("CHARACTER_ARMOR_NATIVE_SOURCE cases="+cases+" calcSha256="+hash(calcPath)+" originalSha256="+hash(originalPath)+" savedAcNotApplied=true nativeBinary=false");
    }
    private static String method(String source,String signature) {
        int start=source.indexOf(signature);assertTrue(start>=0);int body=source.indexOf('{',start);assertTrue(body>start);
        int depth=1,end=body+1;
        while(end<source.length()&&depth>0){char ch=source.charAt(end++);if(ch=='{')depth++;else if(ch=='}')depth--;}
        assertEquals(0,depth);return source.substring(start,end);
    }
    private static String hash(Path path)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));}
}
