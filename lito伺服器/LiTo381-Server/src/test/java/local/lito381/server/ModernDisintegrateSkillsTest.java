package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernDisintegrateSkillsTest {
    private static final ModernSpellProtocol.Context CONTEXT = new ModernSpellProtocol.Context(7,3333,-1);
    private static final String GOLDEN = "4c544d3100000049fedcba9800190000"
            + "000000070000000000000d05ffffffffffffffff0000000000010000"
            + "0000004d06000000000000000000004600000000000013880003646973";
    private static ModernSkills.Skill row(ModernSkills.Support support) {
        return new ModernSkills.Skill(77,"dis",70,5000,support);
    }
    private static ModernSkills.Snapshot skills(List<ModernSkills.Skill> rows) {
        return new ModernSkills.Snapshot(7,new ModernApplicationProtocol.CharacterRow(3333,"Owner",1,0,50),rows);
    }
    private static ModernDisintegrateCatalog.Snapshot catalog(ModernDisintegrateCatalog.Status status) {
        return new ModernDisintegrateCatalog.Snapshot(new TournamentPresence.CharacterKey(7,3333),status);
    }
    private static ModernApplicationProtocol.SkillsResult message(ModernSkills.Skill row) {
        return new ModernApplicationProtocol.SkillsResult(0xfedcba98,CONTEXT,
                new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(row)));
    }
    @Test void learnedAndFullyQualifiedFactsJoinWithoutChangingOtherSkillsOrRawSnapshot() throws Exception {
        var arrow=new ModernSkills.Skill(4,"arrow",3,10,ModernSkills.Support.LIGHT_ARROW);
        var raw=skills(List.of(arrow,row(ModernSkills.Support.UNIMPLEMENTED)));
        var qualified=ModernDisintegrateSkills.qualify(raw,catalog(ModernDisintegrateCatalog.Status.QUALIFIED));
        assertEquals(List.of(arrow,row(ModernSkills.Support.DISINTEGRATE)),qualified);
        assertEquals(ModernSkills.Support.UNIMPLEMENTED,raw.skills().get(1).support());
        assertThrows(UnsupportedOperationException.class,()->qualified.clear());
        assertEquals(List.of(arrow),ModernDisintegrateSkills.qualify(skills(List.of(arrow)),catalog(ModernDisintegrateCatalog.Status.UNLEARNED)));
        assertEquals(List.of(arrow,row(ModernSkills.Support.UNSUPPORTED_TEMPLATE)),
                ModernDisintegrateSkills.qualify(raw,catalog(ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE)));
    }
    @Test void foreignIdentityMissingOwnershipAndDisagreeingReadsFailClosed() {
        var raw=skills(List.of(row(ModernSkills.Support.UNIMPLEMENTED)));
        for(var key:List.of(new TournamentPresence.CharacterKey(8,3333),new TournamentPresence.CharacterKey(7,3334)))
            assertThrows(IOException.class,()->ModernDisintegrateSkills.qualify(raw,new ModernDisintegrateCatalog.Snapshot(key,ModernDisintegrateCatalog.Status.QUALIFIED)));
        assertThrows(IOException.class,()->ModernDisintegrateSkills.qualify(raw,null));
        assertThrows(IOException.class,()->ModernDisintegrateSkills.qualify(raw,catalog(ModernDisintegrateCatalog.Status.UNLEARNED)));
        for(var status:List.of(ModernDisintegrateCatalog.Status.QUALIFIED,ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE))
            assertThrows(IOException.class,()->ModernDisintegrateSkills.qualify(skills(List.of()),catalog(status)));
        for(var values:List.of(new long[]{69,5000},new long[]{70,4999}))
            assertThrows(IOException.class,()->ModernDisintegrateSkills.qualify(skills(List.of(new ModernSkills.Skill(77,"dis",values[0],values[1],ModernSkills.Support.UNIMPLEMENTED))),catalog(ModernDisintegrateCatalog.Status.QUALIFIED)));
    }
    @Test void unsupportedWideSourceValuesArePreservedAndNeverGranted() throws Exception {
        var raw=new ModernSkills.Skill(77,"dis",Long.MAX_VALUE,Long.MAX_VALUE,ModernSkills.Support.UNIMPLEMENTED);
        var result=ModernDisintegrateSkills.qualify(skills(List.of(raw)),catalog(ModernDisintegrateCatalog.Status.UNSUPPORTED_TEMPLATE));
        assertEquals(new ModernSkills.Skill(77,"dis",Long.MAX_VALUE,Long.MAX_VALUE,ModernSkills.Support.UNSUPPORTED_TEMPLATE),result.get(0));
        for(int id:new int[]{1,5,70})assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(id,"dis",70,5000,ModernSkills.Support.DISINTEGRATE));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(77,"dis",69,5000,ModernSkills.Support.DISINTEGRATE));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(77,"dis",70,4999,ModernSkills.Support.DISINTEGRATE));
    }
    @Test void literalV27CatalogAndEveryOlderWireProjectionAreExact() throws Exception {
        var current=message(row(ModernSkills.Support.DISINTEGRATE));
        byte[] golden=HexFormat.of().parseHex(GOLDEN);
        assertArrayEquals(golden,ModernApplicationProtocol.encode(current,27));
        assertEquals(current,ModernApplicationProtocol.decode(golden,27));
        for(int version=1;version<=26;version++) {
            final int v=version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(golden,v));
            if(v<4)assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(current,v));
            else for(var support:List.of(ModernSkills.Support.DISINTEGRATE,ModernSkills.Support.UNSUPPORTED_TEMPLATE)) {
                byte[] expected=golden.clone();expected[48]=1;
                assertArrayEquals(expected,ModernApplicationProtocol.encode(message(row(support)),v));
                assertEquals(message(row(ModernSkills.Support.UNIMPLEMENTED)),ModernApplicationProtocol.decode(expected,v));
                expected[48]=2;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(expected,v));
            }
        }
    }
    @Test void V27WithoutCapabilityCannotGrantSkillEvenWhenTheSourceIsQualified() {
        var off=new ModernSessionBoundary.Negotiated(1,27,1,0);
        var on=new ModernSessionBoundary.Negotiated(1,27,1,ModernSessionProtocol.CAP_DISINTEGRATE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR);
        for(var support:List.of(ModernSkills.Support.DISINTEGRATE,ModernSkills.Support.UNSUPPORTED_TEMPLATE)) {
            var original=message(row(support));
            assertEquals(message(row(ModernSkills.Support.UNIMPLEMENTED)),ModernMatchDescriptor.forPeer(original,off));
            assertEquals(original,ModernMatchDescriptor.forPeer(original,on));
        }
    }
}
