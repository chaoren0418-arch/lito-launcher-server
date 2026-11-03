package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ModernQuestLifeProtocolTest {
    static ModernMatchDescriptor descriptor(){
        var d=ModernQuestProtocolTest.descriptor();return new ModernMatchDescriptor(d.ruleset(),d.features()|ModernMatchDescriptor.QUEST_LIVES,
                d.preparationTimeoutMillis(),d.loadTimeoutMillis(),d.countdownNanos(),d.activeTimeoutMillis());
    }
    static ModernNpcProtocol.Frame frame(ModernBattleEngineNpcTest.Fixture f,ModernNpcBattle.Frame snapshot,int player) {
        return ModernQuestVisibility.npc(ModernNpcProtocol.Frame.from(snapshot,descriptor()).withResources(ModernNpcProtocol.Resources.from(f.config.catalog())),f.players.get(player).scope());
    }
    static ModernApplicationProtocol.NpcResult read(ModernBattleEngineNpcTest.Fixture f,ModernNpcBattle.Frame snapshot,int player) {
        return new ModernApplicationProtocol.NpcResult(81,new ModernNpcProtocol.Request(f.players.get(player).scope(),ModernNpcProtocol.Action.READ,f.scope,0,0,0),
                new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,frame(f,snapshot,player)));
    }
    static byte[] wire(ModernApplicationProtocol.Message message)throws Exception {
        var bytes=ModernApplicationProtocol.encode(message,22);assertEquals(message,ModernApplicationProtocol.decode(bytes,22));return bytes;
    }
    static void golden(String key,ModernApplicationProtocol.Message message)throws Exception {
        var bytes=wire(message);if(Boolean.getBoolean("modern.quest.life.goldens"))System.out.println("QUEST_LIFE_"+key+"="+HexFormat.of().formatHex(bytes));
    }
    @Test void actualDeathAndRespawnWireKeepsOwnLifeAndOldNpcReceiptWithIndependentLifeGeneration()throws Exception {
        try(var f=ModernQuestLifeTest.fixture(2)) {
            for(int i=0;i<2;i++){var r=read(f,f.view(),i);wire(r);var q=r.reply().frame().combat().world().quest();assertEquals(1,q.lives().size());assertEquals(i+1,q.lives().get(0).entityId());}
            f.active();f.move(0,1,10,10,0);golden("BEFORE",read(f,f.view(),0));
            var dead=ModernQuestLifeTest.die(f,100);var proof=ModernQuestLifeTest.life(dead,0);golden("DEAD",read(f,dead,0));
            var request=new ModernNpcProtocol.Request(f.players.get(0).scope(),ModernNpcProtocol.Action.CONFIRM_DEATH,f.scope,proof.deathRevision()+1,1,proof.incarnation());
            wire(new ModernApplicationProtocol.NpcRequest(82,request));
            var revived=ModernQuestLifeTest.confirm(f,0,proof);var response=new ModernApplicationProtocol.NpcResult(82,request,
                    new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,frame(f,revived.frame(),0)));
            golden("REVIVED",response);wire(read(f,revived.frame(),1));
            assertEquals(1,response.reply().frame().exchanges().get(0).participantLife());
            assertEquals(2,response.reply().frame().combat().world().quest().lives().get(0).incarnation());
            assertEquals(ModernQuestLife.Reason.STALE_LIFE,ModernQuestLifeTest.confirm(f,0,proof).reason());
            wire(new ModernApplicationProtocol.NpcResult(83,request,new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.DENIED,ModernNpcProtocol.Reason.STALE_LIFE,frame(f,f.view(),0))));
            for(var message:new ModernApplicationProtocol.Message[]{response,new ModernApplicationProtocol.NpcRequest(82,request),read(f,dead,0)}) {
                assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(message,21));
                var bytes=wire(message);for(int n=0;n<bytes.length;n++){var cut=Arrays.copyOf(bytes,n);assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.decode(cut,22));}
            }
        }
    }
    @Test void oldPeerMissingLifeCapabilityAndMalformedSchemaCannotSilentlyLoseTheLedger()throws Exception {
        assertFalse(descriptor().supportedBy(21,ModernSessionProtocol.KNOWN_CAPABILITIES));
        assertTrue(descriptor().supportedBy(22,ModernSessionProtocol.KNOWN_CAPABILITIES));
        try(var f=ModernQuestLifeTest.fixture(2)) {
            assertThrows(IllegalArgumentException.class,()->ModernNpcProtocol.Frame.from(f.view(),ModernQuestProtocolTest.descriptor()));
            var q=read(f,f.view(),0).reply().frame().combat().world().quest();var b=ByteBuffer.allocate(10000);ModernQuestProtocol.put(b,q,22);var bytes=Arrays.copyOf(b.array(),b.position());
            assertEquals(2,bytes[1]);assertEquals(q,ModernQuestProtocol.get(ByteBuffer.wrap(bytes),22));
            assertThrows(IllegalArgumentException.class,()->ModernQuestProtocol.get(ByteBuffer.wrap(bytes),21));
            for(int index:new int[]{1,25,26,bytes.length-64}){var corrupt=bytes.clone();corrupt[index]=(byte)255;assertThrows(RuntimeException.class,()->ModernQuestProtocol.get(ByteBuffer.wrap(corrupt),22));}
            var full=ModernNpcProtocol.Frame.from(f.view(),descriptor()).withResources(ModernNpcProtocol.Resources.from(f.config.catalog()));
            var request=new ModernNpcProtocol.Request(f.players.get(0).scope(),ModernNpcProtocol.Action.READ,f.scope,0,0,0);
            assertThrows(ModernApplicationProtocol.ProtocolException.class,()->ModernApplicationProtocol.encode(new ModernApplicationProtocol.NpcResult(81,request,
                    new ModernNpcProtocol.Reply(ModernNpcProtocol.Status.OK,ModernNpcProtocol.Reason.NONE,full)),22));
        }
    }
}
