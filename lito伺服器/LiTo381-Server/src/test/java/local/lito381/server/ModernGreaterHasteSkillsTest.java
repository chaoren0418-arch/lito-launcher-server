package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModernGreaterHasteSkillsTest {
    private static final ModernSpellProtocol.Context CONTEXT = new ModernSpellProtocol.Context(7,3333,-1);
    private static final String GOLDEN = "4c544d3100000049fedcba9800190000"
            + "000000070000000000000d05ffffffffffffffff0000000000010000"
            + "0000003607000000000000000000003c000000000000000a0003686173";
    private static ModernSkills.Skill row(ModernSkills.Support support) {
        return new ModernSkills.Skill(54,"has",60,10,support);
    }
    private static ModernSkills.Snapshot skills(List<ModernSkills.Skill> rows) {
        return new ModernSkills.Snapshot(7,new ModernApplicationProtocol.CharacterRow(3333,"Owner",1,0,50),rows);
    }
    private static ModernGreaterHasteCatalog.Snapshot catalog(ModernGreaterHasteCatalog.Status status) {
        return new ModernGreaterHasteCatalog.Snapshot(new TournamentPresence.CharacterKey(7,3333),status);
    }
    private static ModernApplicationProtocol.SkillsResult message(ModernSkills.Skill row) {
        return new ModernApplicationProtocol.SkillsResult(0xfedcba98,CONTEXT,
                new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,List.of(row)));
    }
    @Test void learnedAndFullyQualifiedFactsJoinWithoutChangingOtherSkillsOrRawSnapshot() throws Exception {
        var arrow=new ModernSkills.Skill(4,"arrow",3,10,ModernSkills.Support.LIGHT_ARROW);
        var raw=skills(List.of(arrow,row(ModernSkills.Support.UNIMPLEMENTED)));
        var qualified=ModernGreaterHasteSkills.qualify(raw,catalog(ModernGreaterHasteCatalog.Status.QUALIFIED));
        assertEquals(List.of(arrow,row(ModernSkills.Support.GREATER_HASTE)),qualified);
        assertEquals(ModernSkills.Support.UNIMPLEMENTED,raw.skills().get(1).support());
        assertThrows(UnsupportedOperationException.class,()->qualified.clear());
        assertEquals(List.of(arrow),ModernGreaterHasteSkills.qualify(skills(List.of(arrow)),catalog(ModernGreaterHasteCatalog.Status.UNLEARNED)));
        assertEquals(List.of(arrow,row(ModernSkills.Support.UNSUPPORTED_TEMPLATE)),
                ModernGreaterHasteSkills.qualify(raw,catalog(ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE)));
    }
    @Test void foreignIdentityMissingOwnershipAndDisagreeingReadsFailClosed() {
        var raw=skills(List.of(row(ModernSkills.Support.UNIMPLEMENTED)));
        for(var key:List.of(new TournamentPresence.CharacterKey(8,3333),new TournamentPresence.CharacterKey(7,3334)))
            assertThrows(IOException.class,()->ModernGreaterHasteSkills.qualify(raw,new ModernGreaterHasteCatalog.Snapshot(key,ModernGreaterHasteCatalog.Status.QUALIFIED)));
        assertThrows(IOException.class,()->ModernGreaterHasteSkills.qualify(raw,null));
        assertThrows(IOException.class,()->ModernGreaterHasteSkills.qualify(raw,catalog(ModernGreaterHasteCatalog.Status.UNLEARNED)));
        for(var status:List.of(ModernGreaterHasteCatalog.Status.QUALIFIED,ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE))
            assertThrows(IOException.class,()->ModernGreaterHasteSkills.qualify(skills(List.of()),catalog(status)));
        for(var values:List.of(new long[]{59,10},new long[]{60,9}))
            assertThrows(IOException.class,()->ModernGreaterHasteSkills.qualify(skills(List.of(new ModernSkills.Skill(54,"has",values[0],values[1],ModernSkills.Support.UNIMPLEMENTED))),catalog(ModernGreaterHasteCatalog.Status.QUALIFIED)));
    }
    @Test void unsupportedWideSourceValuesArePreservedAndNeverGranted() throws Exception {
        var raw=new ModernSkills.Skill(54,"has",Long.MAX_VALUE,Long.MAX_VALUE,ModernSkills.Support.UNIMPLEMENTED);
        var result=ModernGreaterHasteSkills.qualify(skills(List.of(raw)),catalog(ModernGreaterHasteCatalog.Status.UNSUPPORTED_TEMPLATE));
        assertEquals(new ModernSkills.Skill(54,"has",Long.MAX_VALUE,Long.MAX_VALUE,ModernSkills.Support.UNSUPPORTED_TEMPLATE),result.get(0));
        for(int id:new int[]{1,5,60})assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(id,"has",60,10,ModernSkills.Support.GREATER_HASTE));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(54,"has",59,10,ModernSkills.Support.GREATER_HASTE));
        assertThrows(IllegalArgumentException.class,()->new ModernSkills.Skill(54,"has",60,9,ModernSkills.Support.GREATER_HASTE));
    }
    @Test void literalV42CatalogAndEveryOlderWireProjectionAreExact() throws Exception {
        var current=message(row(ModernSkills.Support.GREATER_HASTE));
        byte[] golden=HexFormat.of().parseHex(GOLDEN);
        assertArrayEquals(golden,ModernApplicationProtocol.encode(current,42));
        assertEquals(current,ModernApplicationProtocol.decode(golden,42));
        for(int version=1;version<=41;version++) {
            final int v=version;
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(golden,v));
            if(v<4)assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(current,v));
            else for(var support:List.of(ModernSkills.Support.GREATER_HASTE,ModernSkills.Support.UNSUPPORTED_TEMPLATE)) {
                byte[] expected=golden.clone();expected[48]=1;
                assertArrayEquals(expected,ModernApplicationProtocol.encode(message(row(support)),v));
                assertEquals(message(row(ModernSkills.Support.UNIMPLEMENTED)),ModernApplicationProtocol.decode(expected,v));
                expected[48]=2;
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(expected,v));
            }
        }
    }
    @Test void V42WithoutCapabilityCannotGrantSkillEvenWhenTheSourceIsQualified() {
        var off=new ModernSessionBoundary.Negotiated(1,42,1,0);
        var on=new ModernSessionBoundary.Negotiated(1,42,1,ModernSessionProtocol.CAP_GREATER_HASTE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR|ModernSessionProtocol.CAP_TIMED|ModernSessionProtocol.CAP_BATTLE_DATA|ModernSessionProtocol.CAP_ACTION_LIFE);
        for(var support:List.of(ModernSkills.Support.GREATER_HASTE,ModernSkills.Support.UNSUPPORTED_TEMPLATE)) {
            var original=message(row(support));
            assertEquals(message(row(ModernSkills.Support.UNIMPLEMENTED)),ModernMatchDescriptor.forPeer(original,off));
            assertEquals(original,ModernMatchDescriptor.forPeer(original,on));
        }
    }
}
