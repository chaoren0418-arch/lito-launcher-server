package local.lito381.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AsRecordBuildTest {
    @Test
    void aLandProductionAnnotationsAreAppliedBeforeTests() throws Exception {
        Class<?> target=Class.forName("local.lito381.server.CombatCurseLifecycle$Target");
        assertTrue(target.isRecord());
        assertEquals(8,target.getRecordComponents().length);
        assertFalse(java.lang.reflect.Modifier.isFinal(target.getDeclaredField("nativeRoleOwner").getModifiers()));
        for(String name:java.util.List.of("_armorTypes","_weaponTypes","_weaponId","_materialTypes","_etcItemTypes","_useTypes")) {
            assertTrue(java.lang.reflect.Modifier.isFinal(NativeDeItemTable.class.getDeclaredField(name).getModifiers()),name);
        }
    }
    @Test
    void mavenTransformsTestClassesBeforeSurefire() {
        assertTrue(AsRecordBuildFixture.class.isRecord());
        var components = AsRecordBuildFixture.class.getRecordComponents();
        assertEquals(1, components.length);
        assertEquals("id", components[0].getName());
        assertEquals(int.class, components[0].getType());
        var left = new AsRecordBuildFixture(7);
        var right = new AsRecordBuildFixture(7);
        assertEquals(19, AsRecordBuildFixture.Access.update(left, 19));
        right.extra = 23;
        assertEquals(19, left.extra);
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertEquals("AsRecordBuildFixture[id=7]", left.toString());
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.objectweb.asm.ClassReader"));
    }

    @Test
    void compileOnlyStubIsAvailableToJavacAndRejectsExecution() {
        // 此測試只證明編譯接線及 stub 防誤用，並不聲稱舊 API 已實作。
        assertThrows(UnsupportedOperationException.class, () ->
                com.lineage.server.model.proc.ProcRandomHelper
                        .addOneToInclusive(1, 2, new java.util.Random(0)));
    }
}
