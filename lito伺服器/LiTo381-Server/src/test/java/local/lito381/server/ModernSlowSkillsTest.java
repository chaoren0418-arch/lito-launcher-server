package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;

class ModernSlowSkillsTest {
    static ModernSkills.Skill row(ModernSkills.Support support){return new ModernSkills.Skill(29,"slow",20,500,support);}
    static ModernSkills.Snapshot skills(List<ModernSkills.Skill> rows){return new ModernSkills.Snapshot(7,new ModernApplicationProtocol.CharacterRow(3333,"Owner",1,0,50),rows);}
    static ModernSlowCatalog.Snapshot catalog(ModernSlowCatalog.Status status){return new ModernSlowCatalog.Snapshot(new TournamentPresence.CharacterKey(7,3333),status,OptionalLong.of(0));}
    static ModernApplicationProtocol.SkillsResult message(ModernSkills.Support support){return new ModernApplicationProtocol.SkillsResult(1,new ModernSpellProtocol.Context(7,3333,1),new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(row(support))));}
    @Test void qualifiedLearnedSourceJoinsWithoutMutatingRawSkills()throws Exception {
        var arrow=new ModernSkills.Skill(4,"arrow",3,10,ModernSkills.Support.LIGHT_ARROW);
        var raw=skills(List.of(arrow,row(ModernSkills.Support.UNIMPLEMENTED)));
        assertEquals(List.of(arrow,row(ModernSkills.Support.SLOW)),ModernSlowSkills.qualify(raw,catalog(ModernSlowCatalog.Status.QUALIFIED)));
        assertEquals(ModernSkills.Support.UNIMPLEMENTED,raw.skills().get(1).support());
        assertEquals(List.of(arrow),ModernSlowSkills.qualify(skills(List.of(arrow)),catalog(ModernSlowCatalog.Status.UNLEARNED)));
        assertEquals(List.of(arrow,row(ModernSkills.Support.UNSUPPORTED_TEMPLATE)),ModernSlowSkills.qualify(raw,catalog(ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE)));
    }
    @Test void mismatchedSourceMissingRowOrChangedCostCannotGrantSlow() {
        var raw=skills(List.of(row(ModernSkills.Support.UNIMPLEMENTED)));
        assertThrows(IOException.class,()->ModernSlowSkills.qualify(raw,null));
        for(var key:List.of(new TournamentPresence.CharacterKey(42,3333),new TournamentPresence.CharacterKey(7,3334)))
            assertThrows(IOException.class,()->ModernSlowSkills.qualify(raw,new ModernSlowCatalog.Snapshot(key,ModernSlowCatalog.Status.QUALIFIED)));
        assertThrows(IOException.class,()->ModernSlowSkills.qualify(raw,catalog(ModernSlowCatalog.Status.UNLEARNED)));
        for(var status:List.of(ModernSlowCatalog.Status.QUALIFIED,ModernSlowCatalog.Status.UNSUPPORTED_TEMPLATE))
            assertThrows(IOException.class,()->ModernSlowSkills.qualify(skills(List.of()),catalog(status)));
        for(var costs:List.of(new long[]{19,500},new long[]{20,499}))
            assertThrows(IOException.class,()->ModernSlowSkills.qualify(skills(List.of(new ModernSkills.Skill(29,"slow",costs[0],costs[1],ModernSkills.Support.UNIMPLEMENTED))),catalog(ModernSlowCatalog.Status.QUALIFIED)));
    }
    @Test void version47AndExplicitPermissionAreBothNeededToAdvertiseSlow()throws Exception {
        var on=new ModernSessionBoundary.Negotiated(1,47,1,ModernSlowServiceTest.CAPS);
        var off=new ModernSessionBoundary.Negotiated(1,47,1,ModernSlowServiceTest.CAPS&~ModernSessionProtocol.CAP_SLOW_DATA);
        for(var support:List.of(ModernSkills.Support.SLOW,ModernSkills.Support.UNSUPPORTED_TEMPLATE)) {
            var msg=message(support);
            assertEquals(msg,ModernApplicationProtocol.decode(ModernApplicationProtocol.encode(msg,47),47));
            assertEquals(msg,ModernMatchDescriptor.forPeer(msg,on));assertEquals(message(ModernSkills.Support.UNIMPLEMENTED),ModernMatchDescriptor.forPeer(msg,off));
            for(int version=4;version<47;version++) {
                var wire=ModernApplicationProtocol.encode(msg,version);
                assertEquals(message(ModernSkills.Support.UNIMPLEMENTED),ModernApplicationProtocol.decode(wire,version));
                wire[48]=(byte)support.ordinal();final int v=version;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(wire,v));
            }
        }
    }
}
