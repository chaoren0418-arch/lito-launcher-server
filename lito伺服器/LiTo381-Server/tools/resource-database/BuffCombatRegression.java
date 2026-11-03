package local.lito381.server;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Source SQL changes, shared lease coexistence and real native casts. */
public class BuffCombatRegression {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static Object get(Object o,String n)throws Exception{return NativeDirectoryRegression.get(o,n);}
    static void set(Object o,String n,Object v)throws Exception{NativeDirectoryRegression.set(o,n,v);}
    static Object call(Object o,String n,Class<?>[] types,Object...v)throws Exception{return NativeDirectoryRegression.call(o,n,types,v);}
    static void cast(ClientSession caster,int id)throws Exception {
        ((Map<?,?>)get(caster,"spellReadyNanos")).clear();set(caster,"playerMp",1000);
        var writer=new Protocol.Writer().c(0x14).c((id-1)/8).c((id-1)%8);
        var shield=BattleSpellCatalog.shieldSpell(id);var ability=BattleSpellCatalog.abilityBuffSpell(id);
        if(shield!=null&&shield.targeting()==BattleSpellCatalog.Targeting.PLAYER_OBJECT
                ||ability!=null&&ability.requestShape()==BattleSpellCatalog.RequestShape.OBJECT_TARGET)writer.d(caster.playerObjectId());
        call(caster,"useSpell",new Class[]{byte[].class},writer.bytes());
    }
    static CombatDatabase.Snapshot controls(CombatDatabase.Snapshot data,Map<Integer,Map<String,Integer>> abilities,boolean coexist) {
        var c=data.controls();return new CombatDatabase.Snapshot(data.skills(),new SourceSkillControls.Snapshot(c.probabilities(),c.bossModifiers(),abilities,c.npcAttributes(),coexist,c.cancellationImmune(),c.eyes(),c.blessings()));
    }
    public static void main(String[] args)throws Exception {
        var config=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(config);
        var df=CombatDatabase.class.getDeclaredField("current");df.setAccessible(true);var data=(CombatDatabase.Snapshot)df.get(null);
        check(data.controls().skillCoexist(),"read-only current server_event enables source coexistence");
        try {
            long now=System.nanoTime();var status=new BattleAbilityStatus();
            status.activateStrength(42,now,20,5);check(status.activateStrength(109,now,10,2)==0&&status.strength(now).amount()==7,"coexisting STR IDs add and suppress replacement identity");
            check(status.strength(now+10_000_000_000L).amount()==5,"shorter STR lease expires independently at exact boundary");
            status.expireStrengthSkillId(now+10_000_000_000L);check(status.strength(now+11_000_000_000L).amount()==5,"legacy expiration cannot erase a longer coexisting lease");
            status.activateStrength(109,now,10,2);check(status.removeSkill(42)&&status.strength(now).amount()==2,"selective removal can remove non-current native STR ID");
            status.activateDexterity(26,now,20,5);status.activateDexterity(110,now,10,2);
            check(status.dexterity(now).amount()==7&&status.dexterity(now+10_000_000_000L).amount()==5,"coexisting DEX expiration");
            status.activateArmorProtection(3,now,20,-2);status.activateArmorProtection(99,now,10,-3);status.activateArmorProtection(151,now,5,-6);
            check(status.shield(now).amount()==-11&&status.armorProtection(now).armorClass()==-11,"three shared armor IDs use SQL sum");
            check(status.shield(now+5_000_000_000L).amount()==-5&&status.shield(now+10_000_000_000L).amount()==-2,"armor IDs expire independently");
            status.activateRoyalAura(114,now,20,0,5,5,20);status.activateRoyalAura(115,now,5,-8,0,0,0);
            var aura=status.royalAura(now);check(aura.armorClass()==-8&&aura.meleeHit()==5&&aura.rangedHit()==5&&aura.magicResistance()==20,"coexisting auras preserve direct MR20 and table abilities");
            aura=status.royalAura(now+5_000_000_000L);check(aura.armorClass()==0&&aura.meleeHit()==5&&aura.magicResistance()==20,"aura expiration leaves other aura and direct MR");
            status=new BattleAbilityStatus();status.activateStrength(42,now,20,5);status.activateStrength(109,now,10,2);
            status.activateStrength(42,now,30,5);check(status.strength(now).amount()==7,"same ID refresh does not stack twice");
            status.activateDexterity(26,now,20,5);status.activateDexterity(110,now,20,2);
            status.activateArmorProtection(3,now,20,-2);status.activateArmorProtection(99,now,20,-3);status.activateArmorProtection(151,now,20,-6);
            status.activateRoyalAura(114,now,20,0,5,5,20);status.activateRoyalAura(115,now,20,-8,0,0,0);
            var owned=(CombatBuffLeases)get(status,"sourceBuffLeases");check(((Map<?,?>)get(owned,"deadlines")).size()<=14,"per-status source map is bounded by fourteen allowed IDs");
            boolean invalid=false;try{owned.put(999999,now,20);}catch(IllegalArgumentException expected){invalid=true;}check(invalid,"unsupported ID cannot expand source buff map");
            var isolated=new BattleAbilityStatus();check(isolated.strength(now).amount()==0,"new status owns independent leases");
            status.cancelMagicEffects();check(status.strength(now).amount()==0&&status.dexterity(now).amount()==0&&status.shield(now).amount()==0&&status.royalAura(now).magicResistance()==0
                    &&((Map<?,?>)get(owned,"deadlines")).isEmpty(),"Cancellation clears all shared source leases and direct aura effect");
            df.set(null,controls(data,data.controls().abilities(),false));status=new BattleAbilityStatus();
            status.activateStrength(42,now,30,5);check(status.activateStrength(109,now,10,2)==42&&status.strength(now).amount()==2,"disabled coexist replaces source STR group");
            status.activateDexterity(26,now,30,5);status.activateDexterity(110,now,10,2);check(status.dexterity(now).amount()==2,"disabled coexist replaces source DEX group");
            status.activateArmorProtection(3,now,30,-2);status.activateArmorProtection(99,now,10,-3);check(status.shield(now).amount()==-3,"disabled coexist replaces covered armor IDs");
            status.activateRoyalAura(114,now,30,0,5,5,20);status.activateRoyalAura(115,now,10,-8,0,0,0);
            check(status.royalAura(now).magicResistance()==0&&status.royalAura(now).meleeHit()==0,"disabled coexist removes prior aura including direct MR");
            var abilities=new HashMap<>(data.controls().abilities());
            abilities.put(3,Map.of("AC",-9));abilities.put(99,Map.of("AC",4));abilities.put(151,Map.of("AC",-13));
            abilities.put(42,Map.of("STR",8));abilities.put(109,Map.of("STR",-3));abilities.put(26,Map.of("DEX",-2));abilities.put(110,Map.of("DEX",9));
            abilities.put(114,Map.of("HIT",-4,"BOW_HIT",7));abilities.put(115,Map.of("AC",6));
            abilities.put(55,Map.of("AC",-7,"HIT",-2,"DMG",9));abilities.put(89,Map.of("AC",7,"HIT",-5));
            abilities.put(204,Map.of("HIT",-4,"DMG",12));abilities.put(219,Map.of("DMG",13,"BOW_DMG",-9));abilities.put(214,Map.of("AC",4));abilities.put(117,Map.of("DMG",-6));
            abilities.put(159,Map.of("AC",-9));abilities.put(168,Map.of("AC",3));abilities.put(183,Map.of("AC",-11));abilities.put(137,Map.of("WIS",-4));
            abilities.put(148,Map.of("DMG",-2));abilities.put(155,Map.of("DMG",12));abilities.put(149,Map.of("BOW_HIT",-9));
            abilities.put(156,Map.of("BOW_HIT",-3,"BOW_DMG",7));abilities.put(166,Map.of("BOW_HIT",2,"BOW_DMG",-8));
            df.set(null,controls(data,abilities,true));status=new BattleAbilityStatus();
            status.activateBerserk(now,10,10,2,5);var berserk=status.berserk(now);
            check(berserk.armorClass()==-7&&berserk.meleeHit()==-2&&berserk.meleeDamage()==9,"all three Berserk SQL fields allow signed changes");
            status.activateBounceAttack(now,10,-2,6,10);var bounce=status.bounceAttack(now);
            check(bounce.armorClass()==7&&bounce.meleeHit()==-5&&bounce.weaponDamageChance()==10,"Bounce SQL fields preserve direct weapon effect");
            status.activateIllusionOgre(now,10,4,4);check(status.illusionOgre(now).meleeHit()==-4&&status.illusionOgre(now).meleeDamage()==12,"Ogre SQL hit and damage");
            status.activateIllusionAvatar(now,10,10,10,8,1.5);var avatar=status.illusionAvatar(now);
            check(avatar.meleeDamage()==13&&avatar.rangedDamage()==-9&&avatar.spellPower()==8&&avatar.incomingDamageRate()==1.05,"Avatar SQL physical fields retain direct SP and source configured incoming rate");
            status.activateDiamondGolem(now,10,-20);status.activateBraveAura(now,10,5);
            check(status.diamondGolem(now).armorClass()==4&&status.braveAura(now).meleeDamage()==-6,"Diamond and Brave signed SQL values");
            var earth=new BattleEarthBlessStatus();earth.activate(now,10,-7);check(earth.armorClass(now)==-9,"Earth Bless SQL AC");
            var iron=new BattleIronSkinStatus();iron.activate(now,10,-10);check(iron.armorClass(now)==3,"Iron Skin signed SQL AC");
            var guard=new BattleGuardBrakeStatus();guard.activate(now,10,15);check(guard.armorClass(now)==-11,"Guard Brake signed SQL AC");
            var mind=new BattleClearMindStatus();mind.activate(now,10,3);check(mind.snapshot(now).wisdomModifier()==-4,"Clear Mind signed SQL WIS");
            var fire=new BattleFireWeaponStatus();fire.activate(now,10,4);check(fire.physicalDamage(false,now)==-2&&fire.physicalDamage(true,now)==0,"Fire Weapon SQL damage stays melee only");
            var bless=new BattleFireBlessStatus();bless.activate(now,10,4);check(bless.physicalDamage(false,now)==12,"Fire Bless SQL damage");
            var wind=new BattleWindShotStatus();wind.activate(now,10,6);check(wind.physicalHit(true,now)==-9&&wind.physicalHit(false,now)==0,"Wind Shot SQL hit stays ranged only");
            var eye=new BattleStormEyeStatus();eye.activate(now,10,2,3);check(eye.physicalHit(true,now)==-3&&eye.physicalDamage(true,now)==7,"Storm Eye independent SQL hit/damage");
            var shot=new BattleStormShotStatus();shot.activate(now,10,-1,5);check(shot.physicalHit(true,now)==2&&shot.physicalDamage(true,now)==-8,"Storm Shot independent SQL hit/damage");
            check(earth.armorClass(now+10_000_000_000L)==0&&iron.armorClass(now+10_000_000_000L)==0&&guard.armorClass(now+10_000_000_000L)==0
                    &&mind.snapshot(now+10_000_000_000L).wisdomModifier()==0&&fire.physicalDamage(false,now+10_000_000_000L)==0&&bless.physicalDamage(false,now+10_000_000_000L)==0
                    &&wind.physicalHit(true,now+10_000_000_000L)==0&&eye.physicalDamage(true,now+10_000_000_000L)==0&&shot.physicalDamage(true,now+10_000_000_000L)==0,"expired source scalar leases contribute zero");
            status.clear();check(status.berserk(now).meleeDamage()==0&&status.illusionOgre(now).meleeDamage()==0&&status.illusionAvatar(now).meleeDamage()==0,"clear removes scalar effects");
            df.set(null,data);
            Path root=Path.of(args[1]);Files.createDirectories(root);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);var state=new GameStateStore(root.resolve("unused"),false,true);
            try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
                var f=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var mate=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
                var other=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
                var clock=(java.util.concurrent.ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();check(clock.awaitTermination(1,java.util.concurrent.TimeUnit.SECONDS),"fixture clock stopped");
                var fixtures=List.of(f,peer,mate,other);
                for(int i=0;i<4;i++) {
                    NativeDirectoryRegression.init(fixtures.get(i),"BuffSource"+i,0,config.classicServerNames().get(0));
                    var summary=((List<CharacterSummary>)get(fixtures.get(i).session,"characters")).get(0);
                    set(fixtures.get(i).session,"characters",List.of(AwakenCombatRegression.copy(summary,Map.of("objectId",0x38130001L+i))));
                }
                FlowRegression.setup(hub,f.session,peer.session,mate.session,other.session);
                set(f.session,"learnedSkills",Set.of(3,26,42,99,109,110,114,115,151));set(f.session,"playerHp",1000);set(f.session,"playerMaxHp",1000);set(f.session,"playerMaxMp",1000);
                for(var fixture:fixtures){set(fixture.session,"positionX",32740);set(fixture.session,"positionY",32798);}
                var live=(BattleAbilityStatus)get(f.session,"abilityStatus");int baseAc=f.session.battleArmorClass();
                int baseStr=(int)call(f.session,"battleStrength",new Class[]{long.class},System.nanoTime()),baseDex=(int)call(f.session,"battleDexterity",new Class[]{long.class},System.nanoTime());
                var cipher=new Protocol.CipherState(0x37110001);NativeDirectoryRegression.drain(f,cipher);
                for(int skill:new int[]{3,99,151,42,109,26,110})cast(f.session,skill);
                check(live.shield(System.nanoTime()).amount()==-11&&f.session.battleArmorClass()==baseAc-11,"native casts retain all three shared armor buffs on actual AC");
                check((int)call(f.session,"battleStrength",new Class[]{long.class},System.nanoTime())==baseStr+7
                        &&(int)call(f.session,"battleDexterity",new Class[]{long.class},System.nanoTime())==baseDex+7,"native casts retain both STR and DEX skills on actual stats");
                var frames=NativeDirectoryRegression.drain(f,cipher);
                check(frames.stream().noneMatch(frame->frame.length==4&&(frame[0]&255)==Protocol.S_MAGE_SHIELD&&frame[1]==0&&frame[2]==0),"coexisting armor casts do not emit replacement stop icons");
                cast(f.session,114);cast(f.session,115);aura=live.royalAura(System.nanoTime());
                check(aura.armorClass()==-8&&aura.meleeHit()==5&&aura.rangedHit()==5&&aura.magicResistance()==20,"native royal aura casts coexist with direct MR20");
                set(f.session,"magicDollAutoSkillIds",List.of(3,42,26));
                var liveOwned=(CombatBuffLeases)get(live,"sourceBuffLeases");var deadlines=Map.copyOf((Map<?,?>)get(liveOwned,"deadlines"));
                call(f.session,"maintainNativeMagicDollAutoSkills",new Class[]{long.class,String.class},System.nanoTime(),"source-coexist-regression");
                check(deadlines.equals(get(liveOwned,"deadlines")),"auto skills recognize their own coexisting IDs without hidden repeated refresh");
                check(((BattleAbilityStatus)get(peer.session,"abilityStatus")).strength(System.nanoTime()).amount()==0,"native self buff does not leak to opponent");
                live.cancelMagicEffects();check(f.session.battleArmorClass()==baseAc&&(int)call(f.session,"battleStrength",new Class[]{long.class},System.nanoTime())==baseStr
                        &&(int)call(f.session,"battleDexterity",new Class[]{long.class},System.nanoTime())==baseDex,"native cancellation restores actual AC/STR/DEX");
                df.set(null,controls(data,abilities,true));
                for(int skill:new int[]{3,99,151,42,109,26,110})cast(f.session,skill);
                check(f.session.battleArmorClass()==baseAc-18&&(int)call(f.session,"battleStrength",new Class[]{long.class},System.nanoTime())==baseStr+5
                        &&(int)call(f.session,"battleDexterity",new Class[]{long.class},System.nanoTime())==baseDex+7,"altered signed SQL abilities change actual native cast results");
                call(f.session,"takePlayerDamage",new Class[]{int.class,ClientSession.class,boolean.class},100000,peer.session,false);
                check(((Map<?,?>)get(liveOwned,"deadlines")).isEmpty()&&live.shield(System.nanoTime()).amount()==0&&live.strength(System.nanoTime()).amount()==0,"fatal native damage clears bounded source buff map: hp="+get(f.session,"playerHp")+" leases="+get(liveOwned,"deadlines")+" current-same="+(get(f.session,"abilityStatus")==live));
                check((int)get(f.session,"playerHp")==0,"death remains fatal without reviving from source buff state");
            }
        } finally {df.set(null,data);}
        System.out.println("PASS source BUFF checks="+checks+"; source coexist event, bounded shared leases, signed SQL values, native casts/AC/abilities/icons/auto-skills/cancellation/death; no SQL writes or deployment");
    }
}
