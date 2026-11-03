package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernGmCommandsTest {
    private static final ModernApplicationProtocol.CharacterRow ROW = new ModernApplicationProtocol.CharacterRow(3333,"GM",1,0,80);
    private static ModernGmCommands.Definition definition(int access) {
        return new ModernGmCommands.Definition("de",access,"L1SpawnDe",false);
    }
    @Test void exactBoundedCommandZeroClearsPositiveAdds() {
        for (int count : new int[]{0,1,20,99,100}) {
            assertEquals(count,ModernGmCommands.SpawnRequest.parse(".de " + count).count());
            assertEquals(count,ModernGmCommands.SpawnRequest.parse("  .DE   " + count + "  ").count());
        }
    }
    @Test void malformedCommandsCannotBecomeAnotherExecutorOrAnUnboundedCount() {
        for (String text : List.of("", ".de", ".de -1", ".de +1", ".de 01", ".de 101", ".de 999999999999999",
                ".de ２０", ".de ١", ".de 1 2", ".de 1;shutdown", ".de 1\n", ".de\t1", ".de 1\0", ".debug 1", "de 1", "x".repeat(33)))
            assertThrows(IllegalArgumentException.class,() -> ModernGmCommands.SpawnRequest.parse(text),text);
        assertThrows(IllegalArgumentException.class,() -> ModernGmCommands.SpawnRequest.parse(null));
        assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.SpawnRequest(-1));
        assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.SpawnRequest(101));
    }
    @Test void databaseThresholdIsNotGlobalAdminOrImplicitDefault() {
        assertEquals(ModernGmCommands.Decision.INSUFFICIENT_ACCESS,new ModernGmCommands.Snapshot(7,"owner",ROW,199,false,false,definition(200)).decision());
        assertEquals(ModernGmCommands.Decision.ALLOWED,new ModernGmCommands.Snapshot(7,"owner",ROW,200,false,false,definition(200)).decision());
        assertEquals(ModernGmCommands.Decision.ALLOWED,new ModernGmCommands.Snapshot(7,"owner",ROW,0,false,false,definition(0)).decision());
        assertEquals(ModernGmCommands.Decision.COMMAND_DISABLED,new ModernGmCommands.Snapshot(7,"owner",ROW,200,false,false,null).decision());
    }
    @Test void bansAndUnsupportedDefinitionWinEvenForMaximumGm() {
        assertEquals(ModernGmCommands.Decision.ACCOUNT_BANNED,new ModernGmCommands.Snapshot(7,"owner",ROW,200,true,true,definition(200)).decision());
        assertEquals(ModernGmCommands.Decision.CHARACTER_BANNED,new ModernGmCommands.Snapshot(7,"owner",ROW,200,false,true,definition(200)).decision());
        for (var wrong : List.of(new ModernGmCommands.Definition("de",0,"OtherClass",false),new ModernGmCommands.Definition("de",0,"L1SpawnDe",true)))
            assertEquals(ModernGmCommands.Decision.UNSUPPORTED_EXECUTOR,new ModernGmCommands.Snapshot(7,"owner",ROW,200,false,false,wrong).decision());
    }
    @Test void permissionAndIdentityValidationNeverClamps() {
        for (int access : new int[]{-1,201,Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,() -> definition(access));
            assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.Snapshot(7,"owner",ROW,access,false,false,null));
        }
        assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.Definition("DE",200,"L1SpawnDe",false));
        assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.Definition("de",200,"\ud800",false));
        assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.Snapshot(0,"owner",ROW,200,false,false,null));
        assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.Snapshot(7,"x".repeat(65),ROW,200,false,false,null));
        assertThrows(IllegalArgumentException.class,() -> new ModernGmCommands.Snapshot(7,"owner",new ModernApplicationProtocol.CharacterRow(1,"GM",1,0,0),200,false,false,null));
    }
    @Test void defaultStringRepresentationsDoNotExposeAccountOrCommandContents() {
        assertEquals("ModernGmSnapshot[redacted]",new ModernGmCommands.Snapshot(7,"secret-owner",ROW,200,false,false,definition(200)).toString());
        assertEquals("ModernGmDefinition[redacted]",definition(200).toString());
        assertEquals("ModernGmSpawnRequest[redacted]",new ModernGmCommands.SpawnRequest(20).toString());
    }
}
