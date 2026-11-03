package local.lito381.server;

import java.nio.ByteBuffer;
import static local.lito381.server.ModernApplicationProtocol.*;

/** Immutable server policy, not client permission and not a remaining-time clock. */
record ModernMatchDescriptor(Ruleset ruleset, int features, int preparationTimeoutMillis,
                             int loadTimeoutMillis, long countdownNanos, int activeTimeoutMillis) {
    static final int COMBAT=1, SPELLS=2, LIGHT_ARROW=4, ITEMS=8, WEIGHT=16, TIMED=32,
            AMMO=64, REGEN=128, HONOR=256, FIRE_STORM=512, PET_RACE=1024, NPC=2048, ISOLATED_FORCES=4096, QUEST_OBJECTIVES=8192, QUEST_LIVES=16384, NPC_LIGHT_ARROW=32768, TIME_ATTACK=65536, SIEGE=131072, DISINTEGRATE=262144, NPC_DISINTEGRATE=524288, NPC_FIRE_STORM=1048576, KNOWN_FEATURES=2097151;
    static final int BYTES=32;
    static final int GREATER_HASTE=2097152, HASTE=4194304, SLOW=8388608, PLAYER_HEALING=16777216, ARMOR_PROTECTION=33554432,
            FULL_VIP=67108864, CANONICAL_FEATURES=KNOWN_FEATURES|GREATER_HASTE|HASTE|SLOW|PLAYER_HEALING|ARMOR_PROTECTION|FULL_VIP;
    enum Ruleset { SCENE_ONLY, PVP_SKIRMISH, DEATHMATCH_ELIMINATION, HONOR_TOWERS, PET_RACE, NPC_ENCOUNTER, COOPERATIVE_TIME_ATTACK, SIEGE_OBJECTIVES }
    ModernMatchDescriptor {
        if(ruleset==null || (features&~CANONICAL_FEATURES)!=0 || preparationTimeoutMillis<50 || preparationTimeoutMillis>30_000)
            throw bad();
        new ModernBattleEngine.Limits(loadTimeoutMillis,countdownNanos,activeTimeoutMillis);
        dependency(features,SPELLS,COMBAT);dependency(features,LIGHT_ARROW,SPELLS);dependency(features,ITEMS,COMBAT);
        dependency(features,WEIGHT,ITEMS);dependency(features,TIMED,WEIGHT);dependency(features,AMMO,TIMED);
        dependency(features,REGEN,WEIGHT);dependency(features,HONOR,REGEN|LIGHT_ARROW);dependency(features,FIRE_STORM,REGEN|LIGHT_ARROW);
        dependency(features,ISOLATED_FORCES,NPC);
        dependency(features,QUEST_OBJECTIVES,NPC|ISOLATED_FORCES);
        dependency(features,QUEST_LIVES,QUEST_OBJECTIVES|REGEN);
        dependency(features,NPC_LIGHT_ARROW,NPC|SPELLS);
        dependency(features,DISINTEGRATE,REGEN|LIGHT_ARROW);
        dependency(features,NPC_DISINTEGRATE,NPC|SPELLS|REGEN);
        dependency(features,NPC_FIRE_STORM,NPC|SPELLS|REGEN);
        dependency(features,GREATER_HASTE,TIMED|SPELLS);
        dependency(features,HASTE,TIMED|SPELLS);
        dependency(features,SLOW,TIMED|SPELLS);
        dependency(features,PLAYER_HEALING,SPELLS|WEIGHT);
        dependency(features,ARMOR_PROTECTION,COMBAT);
        dependency(features,FULL_VIP,COMBAT);
        if((features&FULL_VIP)!=0&&ruleset!=Ruleset.PVP_SKIRMISH&&ruleset!=Ruleset.DEATHMATCH_ELIMINATION)throw bad();
        if(ruleset==Ruleset.PET_RACE ? features!=PET_RACE : (features&PET_RACE)!=0)throw bad();
        if(ruleset!=Ruleset.PET_RACE&&(ruleset==Ruleset.SCENE_ONLY ? features!=0 : (features&COMBAT)==0))throw bad();
        if(((features&HONOR)!=0)!=(ruleset==Ruleset.HONOR_TOWERS))throw bad();
        if(((features&SIEGE)!=0)!=(ruleset==Ruleset.SIEGE_OBJECTIVES)
                ||ruleset==Ruleset.SIEGE_OBJECTIVES&&(features&~(SIEGE|COMBAT|SPELLS|ITEMS|WEIGHT|REGEN|PLAYER_HEALING|ARMOR_PROTECTION|((features&SLOW)!=0?TIMED|SLOW|HASTE|GREATER_HASTE:0)))!=0)throw bad();
        if(((features&TIME_ATTACK)!=0)!=(ruleset==Ruleset.COOPERATIVE_TIME_ATTACK)
                ||((features&NPC)!=0)!=(ruleset==Ruleset.NPC_ENCOUNTER||ruleset==Ruleset.COOPERATIVE_TIME_ATTACK)
                ||ruleset==Ruleset.NPC_ENCOUNTER&&((features&~(NPC|COMBAT|SPELLS|ITEMS|WEIGHT|REGEN|ISOLATED_FORCES|QUEST_OBJECTIVES|QUEST_LIVES|NPC_LIGHT_ARROW|NPC_DISINTEGRATE|NPC_FIRE_STORM|TIMED|HASTE|SLOW|PLAYER_HEALING|ARMOR_PROTECTION))!=0
                    ||((features&TIMED)!=0)!=((features&(HASTE|SLOW))!=0))
                ||ruleset==Ruleset.COOPERATIVE_TIME_ATTACK&&((features&(NPC|COMBAT))!=(NPC|COMBAT)
                    ||(features&~(TIME_ATTACK|NPC|COMBAT|SPELLS|ITEMS|WEIGHT|REGEN|NPC_LIGHT_ARROW|NPC_DISINTEGRATE|NPC_FIRE_STORM|TIMED|HASTE|SLOW|PLAYER_HEALING|ARMOR_PROTECTION))!=0
                    ||((features&TIMED)!=0)!=((features&(HASTE|SLOW))!=0)||activeTimeoutMillis>60_000))throw bad();
    }
    boolean has(int feature){return (features&feature)==feature;}
    boolean settlementEnabled(){return ruleset==Ruleset.DEATHMATCH_ELIMINATION || ruleset==Ruleset.HONOR_TOWERS||has(QUEST_OBJECTIVES);}
    boolean journaledResult(){return settlementEnabled()||has(TIME_ATTACK)||has(SIEGE);}
    ModernBattleEngine.Limits engineLimits(){return new ModernBattleEngine.Limits(loadTimeoutMillis,countdownNanos,activeTimeoutMillis);}
    void validateMode(int mode){
        if(has(FULL_VIP)&&mode!=BattleRules.Mode.DEATHMATCH.wireId())throw bad();
        if(mode<0||mode>5 || ruleset==Ruleset.PVP_SKIRMISH&&!BattleRules.Mode.fromWire(mode).playerVersusPlayer()
                || ruleset==Ruleset.DEATHMATCH_ELIMINATION&&mode!=1 || ruleset==Ruleset.HONOR_TOWERS&&mode!=5
                || (has(FIRE_STORM)||has(DISINTEGRATE))&&mode!=1&&!has(HONOR)||ruleset==Ruleset.PET_RACE&&mode!=0
                || ruleset==Ruleset.NPC_ENCOUNTER&&mode!=2||ruleset==Ruleset.COOPERATIVE_TIME_ATTACK&&mode!=4||ruleset==Ruleset.SIEGE_OBJECTIVES&&mode!=3)throw bad();
    }
    static ModernMatchDescriptor of(ModernWorldService.Mapping mapping,ModernWorldService.Limits limits,boolean persisted){
        int flags=(mapping.combatEnabled()?COMBAT:0)|(mapping.spellsEnabled()?SPELLS:0)|(mapping.damageMagic()?LIGHT_ARROW:0)
                |(mapping.matchItems()?ITEMS:0)|(mapping.weightEnabled()?WEIGHT:0)|(mapping.timedEnabled()?TIMED:0)
                |(mapping.bowEnabled()?AMMO:0)|(mapping.regenEnabled()?REGEN:0)|(mapping.honor()!=null?HONOR:0)
                |(mapping.fireStormEnabled()?FIRE_STORM:0)|(mapping.race()!=null?PET_RACE:0)|(mapping.npc()!=null?NPC:0)
                |(mapping.npc()!=null&&mapping.npc().questPlan()!=null?ISOLATED_FORCES:0)
                |(mapping.npc()!=null&&mapping.npc().questPlan()!=null&&mapping.npc().questPlan().objectives()!=null?QUEST_OBJECTIVES:0)
                |(mapping.npc()!=null&&mapping.npc().questPlan()!=null&&mapping.npc().questPlan().participantLives()?QUEST_LIVES:0)
                |(mapping.npc()!=null&&mapping.npc().lightArrow()?NPC_LIGHT_ARROW:0)|(mapping.timeAttack()!=null?TIME_ATTACK:0)|(mapping.siege()!=null?SIEGE:0)|(mapping.disintegrateEnabled()?DISINTEGRATE:0)|(mapping.npcDisintegrateEnabled()?NPC_DISINTEGRATE:0)|(mapping.npcFireStormEnabled()?NPC_FIRE_STORM:0)|(mapping.greaterHasteEnabled()?GREATER_HASTE:0)|(mapping.hasteEnabled()?HASTE:0)|(mapping.slowEnabled()?SLOW:0);
        if(mapping.playerHealingEnabled())flags|=PLAYER_HEALING;
        if(mapping.armorProtectionEnabled())flags|=ARMOR_PROTECTION;
        if(mapping.fullVipEnabled())flags|=FULL_VIP;
        var rule=mapping.siege()!=null?Ruleset.SIEGE_OBJECTIVES:mapping.timeAttack()!=null?Ruleset.COOPERATIVE_TIME_ATTACK:mapping.npc()!=null?Ruleset.NPC_ENCOUNTER:mapping.race()!=null?Ruleset.PET_RACE:!mapping.combatEnabled()?Ruleset.SCENE_ONLY:mapping.honor()!=null?Ruleset.HONOR_TOWERS:
                persisted&&mapping.binding().key().mode()==1?Ruleset.DEATHMATCH_ELIMINATION:Ruleset.PVP_SKIRMISH;
        var clock=mapping.clockOverrides()==null?limits.engineLimits():mapping.clockOverrides().resolve(limits.engineLimits());
        var result=new ModernMatchDescriptor(rule,flags,limits.preparationTimeoutMillis(),clock.loadTimeoutMillis(),clock.countdownNanos(),clock.activeTimeoutMillis());
        result.validateMode(mapping.binding().key().mode());return result;
    }
    void validateWorld(ModernWorldProtocol.Frame frame){
        if(has(FULL_VIP))throw bad();
        if(has(GREATER_HASTE)||has(HASTE)||has(SLOW)||has(PLAYER_HEALING)||has(ARMOR_PROTECTION))throw bad();
        validateMode(frame.room().summary().config().mode());
        if(has(SPELLS)!=!frame.casts().isEmpty() || frame.completion()!=null&&!settlementEnabled())throw bad();
    }
    void validateCombat(ModernCombatProtocol.Frame frame){
        if(has(FULL_VIP))throw bad();
        if(has(GREATER_HASTE)||has(HASTE)||has(SLOW)||has(PLAYER_HEALING)||has(ARMOR_PROTECTION))throw bad();
        if(!has(COMBAT) || has(LIGHT_ARROW)!=!frame.damages().isEmpty() || has(ITEMS)!=!frame.items().isEmpty()
                || has(WEIGHT)!=!frame.weights().isEmpty() || has(TIMED)!=!frame.buffs().isEmpty()
                || has(AMMO)!=!frame.ammo().isEmpty() || has(REGEN)!=!frame.regen().isEmpty()
                || has(HONOR)!=frame.honor().enabled() || has(FIRE_STORM)!=!frame.fireStorms().isEmpty()||has(NPC_LIGHT_ARROW)!=!frame.npcSpells().isEmpty()
                || has(DISINTEGRATE)!=!frame.disintegrations().isEmpty()||has(NPC_DISINTEGRATE)!=!frame.npcDisintegrations().isEmpty()||has(NPC_FIRE_STORM)!=!frame.npcFireStorms().isEmpty())throw bad();
    }
    boolean supportedBy(int version,int capabilities){
        // An old capability word cannot imply the independent extension word.
        if(has(FULL_VIP)||has(PLAYER_HEALING)||has(ARMOR_PROTECTION))return false;
        return baseSupportedBy(version,capabilities);
    }
    boolean supportedBy(ModernSessionBoundary.Negotiated peer) {
        return peer!=null&&(!has(PLAYER_HEALING)||ModernSessionExtensions.allowsData(peer))
                &&(!has(ARMOR_PROTECTION)||ModernSessionExtensions.allowsArmor(peer))
                &&(!has(FULL_VIP)||ModernSessionExtensions.allowsVipData(peer)
                    &&(!has(ARMOR_PROTECTION)||ModernSessionExtensions.allowsArmorPresentation(peer)))
                &&baseSupportedBy(peer.version(),peer.capabilities());
    }
    private boolean baseSupportedBy(int version,int capabilities) {
        if(has(SLOW)&&!ModernBattleSpeedWire.negotiated(new ModernSessionBoundary.Negotiated(1,version,1,capabilities)))return false;
        // The checked publication exists before any transport is enabled.
        if(has(HASTE)&&(version<ModernBattleHasteWire.minimumVersion(this)||!ModernBattleHasteWire.negotiated(new ModernSessionBoundary.Negotiated(1,version,1,capabilities))))return false;
        if(has(GREATER_HASTE)&&!ModernGreaterHasteProtocol.negotiated(new ModernSessionBoundary.Negotiated(1,version,1,capabilities)))return false;
        if(has(NPC_FIRE_STORM)&&(version<31||(capabilities&ModernSessionProtocol.CAP_NPC_FIRE_STORM)==0))return false;
        if(has(NPC_DISINTEGRATE)&&(version<29||(capabilities&ModernSessionProtocol.CAP_NPC_DISINTEGRATE)==0))return false;
        if(has(DISINTEGRATE)&&(version<27||(capabilities&(ModernSessionProtocol.CAP_DISINTEGRATE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR))
                !=(ModernSessionProtocol.CAP_DISINTEGRATE|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)))return false;
        if(has(SIEGE)&&(version<26||(capabilities&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)==0))return false;
        if(has(TIME_ATTACK|NPC_LIGHT_ARROW)&&version<30)return false;
        if(has(TIME_ATTACK)&&version<25)return false;
        if(has(NPC_LIGHT_ARROW)&&version<24)return false;
        if(has(NPC|SPELLS)&&version<23)return false;
        if(has(QUEST_LIVES)&&version<22)return false;
        if(has(QUEST_OBJECTIVES)&&version<21)return false;
        if(has(ISOLATED_FORCES)&&version<20)return false;
        if(has(NPC)&&(version<17||(capabilities&(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR))
                !=(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)))return false;
        if(has(PET_RACE)&&(version<16||(capabilities&ModernSessionProtocol.CAP_PET_RACE)==0))return false;
        int[] flags={COMBAT,SPELLS,LIGHT_ARROW,ITEMS,WEIGHT,TIMED,AMMO,REGEN,HONOR,FIRE_STORM};
        int[] versions={2,4,6,8,9,10,11,12,13,14};
        int[] caps={128,128,128,512,1024,2048,4096,8192,16384,32768};
        for(int i=0;i<flags.length;i++)if(has(flags[i])&&(version<versions[i]||(capabilities&caps[i])==0))return false;
        return !settlementEnabled()||version>=3;
    }
    static void put(ByteBuffer out,ModernMatchDescriptor descriptor){
        if(descriptor!=null&&descriptor.has(FULL_VIP))throw bad();
        if(descriptor!=null&&(descriptor.has(GREATER_HASTE)||descriptor.has(HASTE)||descriptor.has(SLOW)||descriptor.has(PLAYER_HEALING)||descriptor.has(ARMOR_PROTECTION)))throw bad();
        out.put((byte)(descriptor==null?0:1));for(int i=0;i<7;i++)out.put((byte)0);
        if(descriptor==null)return;
        out.putShort((short)1).put((byte)descriptor.ruleset.ordinal()).put((byte)0).putInt(descriptor.features)
                .putInt(descriptor.preparationTimeoutMillis).putInt(descriptor.loadTimeoutMillis).putLong(descriptor.countdownNanos)
                .putInt(descriptor.activeTimeoutMillis).putInt(0);
    }
    static ModernMatchDescriptor get(ByteBuffer in){
        int present=Byte.toUnsignedInt(in.get());if(present>1)throw bad();
        for(int i=0;i<7;i++)if(in.get()!=0)throw bad();if(present==0)return null;
        if(in.getShort()!=1)throw bad();int rule=Byte.toUnsignedInt(in.get());if(rule>=Ruleset.values().length||in.get()!=0)throw bad();
        var result=new ModernMatchDescriptor(Ruleset.values()[rule],in.getInt(),in.getInt(),in.getInt(),in.getLong(),in.getInt());
        if(result.has(FULL_VIP))throw bad();
        if(in.getInt()!=0||result.has(GREATER_HASTE)||result.has(HASTE)||result.has(SLOW)||result.has(PLAYER_HEALING)||result.has(ARMOR_PROTECTION))throw bad();return result;
    }
    private static void dependency(int flags,int feature,int required){if((flags&feature)!=0&&(flags&required)!=required)throw bad();}
    static Message forPeer(Message message,ModernSessionBoundary.Negotiated peer){
        if(message instanceof VipEquipmentResult r) {
            if(!ModernSessionExtensions.allowsVipEquipment(peer))throw bad();r.reply().payload().requirePeer(peer);return message;
        }
        if(message instanceof VipEquipmentRequest)throw bad();
        if(message instanceof PlayerHealingResult r) {
            var v=r.reply().view();
            if(!ModernSessionExtensions.allowsCommands(peer)||!v.world().descriptor().supportedBy(peer)
                    ||!ModernLargeMaps.supported(v.world().manifest(),peer))throw bad();
            return message;
        }
        if(message instanceof PlayerHealingRequest)throw bad();
        if(message instanceof GreaterHasteResult r) {
            var v=r.reply().view();
            if(!ModernGreaterHasteProtocol.negotiated(peer)||!v.world().descriptor().supportedBy(peer)
                    ||!ModernLargeMaps.supported(v.world().manifest(),peer))throw bad();
            return message;
        }
        if(message instanceof GreaterHasteRequest)throw bad();
        if(message instanceof SlowResult r) {
            var v=r.reply().view();
            if(!ModernSlowProtocol.negotiated(peer)||!v.world().descriptor().supportedBy(peer)
                    ||!ModernLargeMaps.supported(v.world().manifest(),peer))throw bad();
            return message;
        }
        if(message instanceof SlowRequest)throw bad();
        if(message instanceof HasteResult r) {
            var v=r.reply().view();
            if(!ModernHasteProtocol.negotiated(peer)||!v.world().descriptor().supportedBy(peer)
                    ||!ModernLargeMaps.supported(v.world().manifest(),peer))throw bad();
            return message;
        }
        if(message instanceof HasteRequest)throw bad();
        if(message instanceof BattleEquipmentResult r) {
            if(!ModernBattleEquipmentProtocol.negotiated(peer))throw bad();
            try{ModernBattleEquipmentProtocol.validate(r);}catch(ModernApplicationProtocol.ProtocolException failure){throw bad();}
            var v=r.reply().view();
            if(!v.world().descriptor().supportedBy(peer)||!ModernLargeMaps.supported(v.world().manifest(),peer))throw bad();
            return message;
        }
        if(message instanceof BattleEquipmentRequest)throw bad();
        if(message instanceof BattleRevivalResult r) {
            if(!ModernBattleRevivalProtocol.negotiated(peer))throw bad();ModernBattleRevivalProtocol.validate(r);
            var v=r.reply().view();
            if(!v.world().descriptor().supportedBy(peer)||!ModernLargeMaps.supported(v.world().manifest(),peer))throw bad();
            return message;
        }
        if(message instanceof BattleRevivalRequest)throw bad();
        if(message instanceof VipWorldResult r) {
            if(!ModernVipWorldProtocol.negotiated(peer))throw bad();ModernVipWorldProtocol.validate(r);
            if(r.reply().payload()!=null)r.reply().payload().requirePeer(peer);return message;
        }
        if(message instanceof VipWorldRequest)throw bad();
        if(message instanceof BattleWorldResult r) {
            if(!ModernBattleWorldProtocol.negotiated(peer))throw bad();ModernBattleWorldProtocol.validate(r);
            var view=r.reply().view();
            if(view!=null&&(!view.world().descriptor().supportedBy(peer)
                    ||!ModernLargeMaps.supported(view.world().manifest(),peer)))throw bad();
            return message;
        }
        if(message instanceof BattleWorldRequest)throw bad();
        if(message instanceof BattleOperationResult r) {
            if(!ModernBattleOperationProtocol.negotiated(peer))throw bad();
            ModernBattleOperationProtocol.validate(r);var v=r.receipt().view();
            if(!v.world().descriptor().supportedBy(peer)
                    ||!ModernLargeMaps.supported(v.world().manifest(),peer))throw bad();
            return message;
        }
        if(message instanceof BattleOperationRequest)throw bad();
        if(message instanceof BattleActionResult r) {
            if(!ModernBattleActionProtocol.negotiated(peer))throw new IllegalArgumentException("battle action not negotiated");
            return new BattleActionResult(r.requestId(),r.echo(),r.rejection(),forPeer(r.result(),peer));
        }
        if(message instanceof BattleReadResult r) {
            if(!ModernBattleReadProtocol.negotiated(peer))throw bad();
            if(r.reply().vip()!=null)r.reply().vip().requirePeer(peer);
            var v=r.reply().view();
            if(v!=null&&(!v.world().descriptor().supportedBy(peer)
                    ||!ModernLargeMaps.supported(v.world().manifest(),peer)))throw bad();
            return message;
        }
        // Requests are inbound only; replies require explicit server-side admission.
        if(message instanceof NpcFireStormRequest)throw bad();
        if(message instanceof ShopResult r) {
            if(r.reply().outcome().snapshot()!=null&&r.reply().outcome().snapshot().materialsEnabled()
                    &&!ModernSlowProtocol.negotiated(peer))throw bad();
        }
        if(message instanceof SkillsResult r) {
            boolean skill77=ModernDisintegrateProtocol.negotiated(peer)||ModernNpcDisintegrateProtocol.negotiated(peer);
            boolean skill54=ModernGreaterHasteProtocol.negotiated(peer);
            boolean skill43=ModernHasteProtocol.negotiated(peer);
            boolean skill29=ModernSlowProtocol.negotiated(peer);
            boolean skill70=ModernNpcFireStormProtocol.negotiated(peer)||peer.version()>=14&&(peer.capabilities()&ModernSessionProtocol.CAP_FIRE_STORM)!=0;
            var skills=r.reply().skills().stream().map(s->s.skillId()!=77&&s.skillId()!=70&&s.skillId()!=54&&s.skillId()!=43&&s.skillId()!=29||s.skillId()==77&&skill77||s.skillId()==70&&skill70||s.skillId()==54&&skill54||s.skillId()==43&&skill43||s.skillId()==29&&skill29?s:
                    new ModernSkills.Skill(s.skillId(),s.nameToken(),s.mpCost(),s.reuseMillis(),ModernSkills.Support.UNIMPLEMENTED)).toList();
            return new SkillsResult(r.requestId(),r.echo(),new ModernSpellProtocol.SkillsReply(r.reply().status(),skills));
        }
        if(message instanceof DisintegrateResult r){
            if(!ModernDisintegrateProtocol.negotiated(peer))throw bad();
            var v=r.reply();return new DisintegrateResult(r.requestId(),r.echo(),new ModernDisintegrateProtocol.Reply(v.status(),v.reason(),forPeer(v.frame(),peer)));
        }
        if(message instanceof SiegeResult r){
            if(peer.version()<26||(peer.capabilities()&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)==0)throw bad();
            if(r.reply().frame()!=null)forPeer(r.reply().frame(),peer);return message;
        }
        if(message instanceof NpcSpellResult r){
            if(peer.version()<24||(peer.capabilities()&(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR))
                    !=(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR))throw bad();
            var v=r.reply();var frame=v.frame();
            if(frame!=null){forPeer(frame.combat(),peer);if(peer.version()<ModernNpcNames.VERSION)frame=frame.withNames(null);}
            return new NpcSpellResult(r.requestId(),r.echo(),new ModernNpcSpellProtocol.Reply(v.status(),v.reason(),frame));
        }
        if(message instanceof NpcFireStormResult r){
            if(!ModernNpcFireStormProtocol.negotiated(peer))throw bad();
            var v=r.reply();var frame=v.frame();if(frame!=null)forPeer(frame.combat(),peer);
            return new NpcFireStormResult(r.requestId(),r.echo(),new ModernNpcFireStormProtocol.Reply(v.status(),v.reason(),frame));
        }
        if(message instanceof NpcDisintegrateResult r){
            if(!ModernNpcDisintegrateProtocol.negotiated(peer))throw bad();
            var v=r.reply();var frame=v.frame();if(frame!=null)forPeer(frame.combat(),peer);
            return new NpcDisintegrateResult(r.requestId(),r.echo(),new ModernNpcDisintegrateProtocol.Reply(v.status(),v.reason(),frame));
        }
        if(message instanceof NpcResult r){
            if(peer.version()<17||(peer.capabilities()&(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR))
                    !=(ModernSessionProtocol.CAP_NPC|ModernSessionProtocol.CAP_MATCH_DESCRIPTOR))throw bad();
            var v=r.reply();var frame=v.frame();
            if(frame!=null){forPeer(frame.combat(),peer);if(peer.version()<ModernNpcNames.VERSION)frame=frame.withNames(null);}
            return new NpcResult(r.requestId(),r.echo(),new ModernNpcProtocol.Reply(v.status(),v.reason(),frame));
        }
        if(message instanceof WorldResult r){var v=r.reply();return new WorldResult(r.requestId(),r.echo(),new ModernWorldProtocol.Reply(v.status(),v.reason(),forPeer(v.frame(),peer)));}
        if(message instanceof CombatResult r){var v=r.reply();return new CombatResult(r.requestId(),r.echo(),new ModernCombatProtocol.Reply(v.status(),v.reason(),forPeer(v.frame(),peer)));}
        if(message instanceof CastResult r){var v=r.reply();return new CastResult(r.requestId(),r.echo(),new ModernSpellProtocol.Reply(v.status(),v.reason(),forPeer(v.frame(),peer)));}
        if(message instanceof ItemResult r){var v=r.reply();return new ItemResult(r.requestId(),r.echo(),new ModernItemProtocol.Reply(v.status(),v.reason(),forPeer(v.frame(),peer)));}
        if(message instanceof AmmoResult r){var v=r.reply();return new AmmoResult(r.requestId(),r.echo(),new ModernAmmoProtocol.Reply(v.status(),v.reason(),forPeer(v.frame(),peer)));}
        return message;
    }
    private static ModernWorldProtocol.Frame forPeer(ModernWorldProtocol.Frame frame,ModernSessionBoundary.Negotiated peer){
        if(frame==null)return null;
        if(!ModernLargeMaps.supported(frame.manifest(),peer))throw bad();
        if(frame.descriptor()!=null&&frame.descriptor().has(DISINTEGRATE)&&!frame.descriptor().supportedBy(peer.version(),peer.capabilities()))throw bad();
        if(frame.siege()!=null&&(frame.descriptor()==null||!frame.descriptor().supportedBy(peer.version(),peer.capabilities())))throw bad();
        if(frame.descriptor()!=null&&frame.descriptor().has(NPC)&&!frame.descriptor().supportedBy(peer.version(),peer.capabilities()))throw bad();
        if((peer.capabilities()&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)==0)return frame.withDescriptor(null);
        if(peer.version()<15||frame.descriptor()==null||!frame.descriptor().supportedBy(peer.version(),peer.capabilities()))throw bad();
        frame.descriptor().validateWorld(frame);return frame;
    }
    private static ModernCombatProtocol.Frame forPeer(ModernCombatProtocol.Frame frame,ModernSessionBoundary.Negotiated peer){
        if(frame==null)return null;
        var world=forPeer(frame.world(),peer);
        if(world.descriptor()!=null)world.descriptor().validateCombat(frame);
        return new ModernCombatProtocol.Frame(world,frame.vitals(),frame.damages(),frame.items(),frame.weights(),frame.buffs(),frame.ammo(),frame.regen(),frame.honor(),frame.fireStorms(),frame.npcSpells(),frame.disintegrations(),frame.npcDisintegrations(),frame.npcFireStorms());
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("invalid immutable match descriptor");}
}
