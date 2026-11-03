package local.lito381.server;

import static local.lito381.server.ModernBattleEngineVipAdvancedTest.elapsed;
import static local.lito381.server.ModernBattleEngineVipHealingTest.heal;
import static local.lito381.server.ModernVipEquipment.Field.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;

/** Real engine captures, no fabricated world or production database/network. */
public final class ModernBattleVipWirePeer {
    record Frame(String label,ModernBattleVipWire.Payload payload){}
    static void pair(List<Frame> frames,String label,ModernBattleEngineVipTest.Fixture f,ModernBattleObservation.Capture c,boolean settlement) {
        var d=ModernBattleVipWireTest.descriptor(c,settlement);
        for(var p:f.players)frames.add(new Frame(label+"-"+p.scope().sourceId(),ModernBattleVipWire.candidate(c,d,p.scope())));
    }
    static List<Frame> frames()throws Exception {
        return frames(false);
    }
    /** Separate, explicit fixture policy. Never rewrite a captured packet or
     * the normal 11101 rule; only this test world is configured with gfx0. */
    static List<Frame> frames(boolean sharedArmorFixture)throws Exception {
        var frames=new ArrayList<Frame>();
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,100L))) {
            pair(frames,"basic-loading",f,f.view(),false);pair(frames,"basic-active",f,f.active(),false);pair(frames,"basic-aborted",f,f.engine.abortObservation(),false);
        }
        try(var f=new ModernBattleEngineVipTest.Fixture(false,Map.of(MAX_HP,5_000_000_000L,MAX_MP,6_000_000_000L,EXP_RATE,7_000_000_000L,ADENA_RATE,-5_000_000_000L))) {
            pair(frames,"wide",f,f.active(),false);
        }
        for(boolean armor:new boolean[]{false,true})try(var f=ModernBattleEngineVipHealingTest.fixture(4,5,armor,0,b->b-1,
                s->sharedArmorFixture&&armor?sharedArmor(s):s,java.util.function.UnaryOperator.identity())) {
            String label=armor?"armor":"healing";
            pair(frames,label+"-loading",f,f.view(),false);pair(frames,label+"-active",f,f.active(),false);
            assertEquals(ModernBattleEngine.AttackStatus.ATTACKED,f.attack(0,1,2).status());pair(frames,label+"-bow",f,f.view(),false);
            elapsed(f,2);assertEquals(ModernPlayerHealingCast.Reason.NONE,heal(f,0,1,1).reason());pair(frames,label+"-paid",f,f.view(),false);
            elapsed(f,6);pair(frames,label+"-expired",f,f.view(),false);
            assertEquals(ModernPlayerHealingCast.Reason.NONE,heal(f,0,2,1).reason());pair(frames,label+"-repaid",f,f.view(),false);
        }
        for(int skill:new int[]{43,54})try(var f=ModernBattleEngineVipSpeedTest.fixture(4,5,0,b->0)) {
            String label="haste"+skill;pair(frames,label+"-loading",f,f.view(),false);f.active();
            assertEquals("NONE",ModernBattleEngineVipSpeedTest.speed(f,0,1,skill,1).reason());pair(frames,label+"-cast",f,f.view(),false);
            elapsed(f,6);pair(frames,label+"-expired",f,f.view(),false);
        }
        try(var f=ModernBattleEngineVipSlowTest.fixture(4,5,0,b->0,4)) {
            f.active();assertEquals(ModernSlowCast.Reason.NONE,ModernBattleEngineVipSlowTest.slow(f,0,1,1).reason());pair(frames,"slow-cast",f,f.view(),false);
            elapsed(f,6);pair(frames,"slow-expired",f,f.view(),false);
            assertEquals("NONE",ModernBattleEngineVipSpeedTest.speed(f,1,1,43,1).reason());pair(frames,"slow-neutralized",f,f.view(),false);
        }
        var root=Files.createTempDirectory("modern-vip-wire-").toRealPath();
        try(var f=new ModernBattleEngineVipTest.Fixture(true,Map.of(MAX_HP,100L))) {
            pair(frames,"durable-loading",f,f.view(),false);
            pair(frames,"durable-active",f,f.active(),false);
            f.epoch.set(2000);pair(frames,"durable-expiry",f,f.view(),false);
            try(var change=f.reserve(0);var store=new ModernEquipmentOverrideStore(root)) {
                var q=f.sources.get(0).input().equipment();
                var fresh=ModernVipLoadout.qualifyForRebuild(f.sources.get(0),ModernCombatProfileTest.binding(q),ModernCombatProfileTest.stored(q,1,Map.of(3L,true)),2000,0);
                var candidate=ModernVipCharacterState.prepareOrdinaryEquipment(change.vipState(),fresh,2000);change.prepareVip(candidate,2000);
                var commit=ModernBattleEngineVipTest.commit(store,change,candidate);
                assertEquals(ModernBattleEngine.EquipmentDisposition.APPLIED,change.publish(commit).disposition());
                pair(frames,"durable",f,f.view(),false);
            }
        } finally {
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList()){
                if(!p.toAbsolutePath().normalize().startsWith(root))throw new IllegalStateException("fixture cleanup scope");Files.delete(p);
            }}
        }
        try(var f=ModernBattleEngineVipAdvancedTest.fixture(4,2,true,1,1000,500,225,b->0)) {
            pair(frames,"elimination-loading",f,f.view(),true);pair(frames,"elimination-active",f,f.active(),true);
            pair(frames,"elimination-finished",f,ModernBattleEngineVipAdvancedTest.cast(f,0,1,77).observation(),true);
        }
        return List.copyOf(frames);
    }
    private static ModernPhysicalCombat.Source sharedArmor(ModernPhysicalCombat.Source source) {
        var rules=new TreeMap<Long,List<ModernArmorProcs.Rule>>();
        source.armorProcs().byItem().forEach((id,rows)->rules.put(id,rows.stream().map(r->new ModernArmorProcs.Rule(
                r.id(),r.catalogId(),r.enchantment(),r.group(),r.attackChance(),r.hurtChance(),r.flatDamage(),r.randomDamage(),
                r.hp(),r.randomHp(),r.mp(),r.randomMp(),r.healMode(),r.skill(),r.seconds(),r.skillTarget(),0,r.graphicsMode())).toList()));
        return ModernArmorProtectionLoadoutTest.withRules(source,source.weaponProcCatalogIds(),rules);
    }
    public static void main(String[] args)throws Exception {
        var input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.US_ASCII));int count=0;
        for(var frame:frames()) {
            var bytes=ModernBattleVipWire.encode(frame.payload());
            System.out.println("FRAME:"+frame.label()+":"+HexFormat.of().formatHex(bytes));System.out.flush();
            var reply=new StringBuilder();int ch;
            while((ch=input.read())!=-1&&ch!='\n'){if(ch=='\r')continue;if(reply.length()>=64)throw new IllegalArgumentException("oversized digest");reply.append((char)ch);}
            assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),reply.toString());count++;
        }
        System.out.println("CLOSED:"+count);System.out.flush();
    }
}
