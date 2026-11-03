package local.lito381.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ProtocolBuilderControlTest {
    @Test
    void parsesEveryExactNativeBuilderControlWireFamily() {
        Protocol.BuilderControlRequest signal = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x12).bytes());
        assertEquals(0x12, signal.subtype());
        assertEquals(Protocol.BuilderControlLayout.SIGNAL, signal.layout());
        assertNull(signal.firstDword());

        Protocol.BuilderControlRequest put = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x80).s("question text").bytes());
        assertEquals(Protocol.BuilderControlLayout.TEXT, put.layout());
        assertEquals("question text", put.text());

        Protocol.BuilderControlRequest target = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x87).d(0xFFFF_FFFFL).bytes());
        assertEquals(Protocol.BuilderControlLayout.DWORD, target.layout());
        assertEquals(0xFFFF_FFFFL, target.firstDword());

        Protocol.BuilderControlRequest dog = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x8A).c(49).c(1).s("Dog").bytes());
        assertEquals(49, dog.firstByte());
        assertEquals(1, dog.secondByte());
        assertEquals("Dog", dog.text());

        Protocol.BuilderControlRequest monster = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x8A).c(1).c(16).s("Monster").bytes());
        assertEquals(Protocol.BuilderControlLayout.TWO_BYTES_TEXT, monster.layout());

        assertEquals(99, parse(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL)
                .c(0x8C).c(99).bytes()).firstByte());
        assertEquals(8, parse(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL)
                .c(0x8E).c(8).bytes()).firstByte());

        Protocol.BuilderControlRequest flags = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x93).d(2).d(123).bytes());
        assertEquals(Protocol.BuilderControlLayout.TWO_DWORDS, flags.layout());
        assertEquals(2L, flags.firstDword());
        assertEquals(123L, flags.secondDword());

        Protocol.BuilderControlRequest setstat = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x97).c(5).c(25).bytes());
        assertEquals(Protocol.BuilderControlLayout.TWO_BYTES, setstat.layout());
        assertEquals(5, setstat.firstByte());
        assertEquals(25, setstat.secondByte());

        Protocol.BuilderControlRequest suspend = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x99).d(72).s("Player").bytes());
        assertEquals(Protocol.BuilderControlLayout.DWORD_TEXT, suspend.layout());
        assertEquals(72L, suspend.firstDword());
        assertEquals("Player", suspend.text());

        Protocol.BuilderControlRequest notice = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x9A)
                .d(0x19BF_CC00L).d(60_000).c(255).s("Notice").bytes());
        assertEquals(Protocol.BuilderControlLayout.NOTICE, notice.layout());
        assertEquals(60_000L, notice.secondDword());
        assertEquals(255, notice.firstByte());

        Protocol.BuilderControlRequest castleWar = parse(new Protocol.Writer()
                .c(Protocol.C_BUILDER_CONTROL).c(0x9B).d(8).bytes());
        assertEquals(8L, castleWar.firstDword());

        // A known body subtype is also a valid no-body packet because the
        // native /control command can emit any low-byte subtype dynamically.
        assertEquals(Protocol.BuilderControlLayout.SIGNAL,
                parse(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x9A).bytes()).layout());
    }

    @Test
    void rejectsShapesAndRangesNoExactProducerCanEmit() {
        assertInvalid(new Protocol.Writer().c(Protocol.C_SUMMON).c(0).bytes());
        assertInvalid(new byte[] {(byte) Protocol.C_BUILDER_CONTROL});
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x7E).c(1).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x80).s("").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x80)
                .s("X".repeat(0x100)).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x8A)
                .c(50).c(1).s("Dog").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x8A)
                .c(1).c(17).s("Monster").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x8C).c(100).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x8E).c(0).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x93).d(3).d(1).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x94).d(9).d(1).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x97).c(6).c(25).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x97).c(0).c(2).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x99)
                .d(72).s("X".repeat(0x10)).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x9A)
                .d(0).d(0).c(1).s("Notice").bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x9B).d(9).bytes());
        assertInvalid(new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x98).d(1).c(0).bytes());
    }

    private static Protocol.BuilderControlRequest parse(byte[] payload) {
        return Protocol.builderControlRequest(payload);
    }

    private static void assertInvalid(byte[] payload) {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.builderControlRequest(payload));
    }
}
