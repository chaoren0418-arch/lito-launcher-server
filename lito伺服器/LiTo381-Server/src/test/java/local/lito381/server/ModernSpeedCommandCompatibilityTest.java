package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

/** Real engine command captures: neutralization is paid success, not a new haste lease. */
class ModernSpeedCommandCompatibilityTest {
    @Test void paidMissCarriesNoTargetEffectButConsumesMpAndMaterial()throws Exception {
        var dir=Path.of("target/modern-speed-command-fixtures");Files.createDirectories(dir);
        try(var f=ModernBattleSlowTest.fixture()) {
            var c=ModernBattleSlowTest.active(f);
            var v=ModernSpeedEffectsTest.publish(c,null).audience(c.actors().get(0).scope());
            var q=ModernSlowProtocolTest.intent(v,1,1);
            var result=f.engine.canonicalSlow(f.players.get(0).owner(),q);
            assertEquals(ModernSlowCast.Reason.NONE,result.reason());
            var detail=(ModernSlowReceipt)result.observation().actors().get(0).runtime().receipts().get(ModernUseHistory.Lane.SLOW).detail();
            assertEquals(ModernSlowReceipt.Outcome.MISS,detail.outcome());
            var p=ModernSpeedEffectsTest.publish(result.observation(),null);
            var r=new ModernSlowProtocol.Reply(q,result.reason(),p.audience(q.proof().player()),ModernBattleLifecycle.from(p));
            Files.write(dir.resolve("miss.before"),ModernBattleWire.encodeSpeed(v));
            Files.write(dir.resolve("miss.request"),ModernSlowProtocol.encodeRequest(q,47));
            Files.write(dir.resolve("miss.reply"),ModernSlowProtocol.encodeReply(r,47));
        }
    }
    @Test void schema6RetainsBothHasteCommandContractsAndNeutralization()throws Exception {
        var dir=Path.of("target/modern-speed-command-fixtures");Files.createDirectories(dir);
        try(var f=ModernBattleSlowTest.dualSpeedFixture()) {
            ModernBattleSlowTest.active(f);f.randomMode=3;
            var initial=f.engine.serviceObservation();
            var slowBefore=ModernSpeedEffectsTest.publish(initial,null).audience(initial.actors().get(0).scope());
            var sq=ModernSlowProtocolTest.intent(slowBefore,1,1);
            var so=f.engine.canonicalSlow(f.players.get(0).owner(),sq);
            var sp=ModernSpeedEffectsTest.publish(so.observation(),null);
            var sr=new ModernSlowProtocol.Reply(sq,so.reason(),sp.audience(sq.proof().player()),ModernBattleLifecycle.from(sp));
            assertEquals(ModernSlowCast.Reason.NONE,sr.reason());
            Files.write(dir.resolve("slow.before"),ModernBattleWire.encodeSpeed(slowBefore));
            Files.write(dir.resolve("slow.request"),ModernSlowProtocol.encodeRequest(sq,47));
            Files.write(dir.resolve("slow.reply"),ModernSlowProtocol.encodeReply(sr,47));
            var before=f.engine.serviceObservation();
            var q=ModernGreaterHasteProtocolTest.intent(before,1,1);
            var out=f.engine.canonicalGreaterHaste(f.players.get(1).owner(),q);
            var p=ModernSpeedEffectsTest.publish(out.observation(),null);
            var reply=new ModernGreaterHasteProtocol.Reply(q,out.reason(),p.audience(q.proof().player()),ModernBattleLifecycle.from(p));
            assertEquals(ModernGreaterHasteCast.Reason.NONE,reply.reason());
            assertEquals(ModernTimedEffects.HasteOrigin.NONE,reply.view().world().actors().get(1).buffs().hasteOrigin());
            Files.write(dir.resolve("neutralized.before"),ModernBattleWire.encodeSpeed(ModernSpeedEffectsTest.publish(before,null).audience(q.proof().player())));
            Files.write(dir.resolve("neutralized.request"),ModernGreaterHasteProtocol.encodeRequest(q,47));
            Files.write(dir.resolve("neutralized.reply"),ModernGreaterHasteProtocol.encodeReply(reply,47));
            assertThrows(IllegalArgumentException.class,()->ModernGreaterHasteProtocol.encodeReply(reply,45));
            assertThrows(IllegalArgumentException.class,()->new ModernGreaterHasteProtocol.Reply(q,ModernGreaterHasteCast.Reason.NO_MP,reply.view(),reply.lifecycle()));
            f.elapsed(2_000_000_000L);before=f.engine.serviceObservation();
            var h=ModernHasteProtocolTest.intent(before,0,2,ModernBattleHasteTest.target(before,1));
            var ho=f.engine.canonicalHaste(f.players.get(0).owner(),h);
            var hp=ModernSpeedEffectsTest.publish(ho.observation(),null);
            var hr=ModernHasteProtocolTest.reply(h,ho,hp);
            assertEquals(ModernHasteCast.Reason.NONE,hr.reason());
            Files.write(dir.resolve("haste.before"),ModernBattleWire.encodeSpeed(ModernSpeedEffectsTest.publish(before,null).audience(h.proof().player())));
            Files.write(dir.resolve("haste.request"),ModernHasteProtocol.encodeRequest(h,47));
            Files.write(dir.resolve("haste.reply"),ModernHasteProtocol.encodeReply(hr,47));
        }
    }
}
