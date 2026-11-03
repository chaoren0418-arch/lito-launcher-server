package local.lito381.server;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Test classpath only. All DDL and seed writes use the newly owned MySQL
 * fixture, never a configured user/source server. Production Main reads these
 * explicit tables with SELECT-only credentials and writes only result SQL. */
final class ModernQuestSqlFixture implements AutoCloseable {
    static final String ACCOUNT="fixture-user",PASSWORD="fixture-password",READER="quest_reader",WRITER="quest_writer";
    static final String CATALOG=ModernNpcCatalogRealMysqlTest.FIRST;
    final ModernSettlementMysqlFixture mysql;
    final Path directory;
    final ModernServerConfig config;
    private boolean successful;
    private ModernQuestSqlFixture(ModernSettlementMysqlFixture mysql,Path directory,ModernServerConfig config){this.mysql=mysql;this.directory=directory;this.config=config;}
    static ModernQuestSqlFixture open(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,false);
    }
    static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies)throws Exception {
        return open(installation,shared,tls,supplies,false);
    }
    static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport)throws Exception {
        return open(installation,shared,tls,supplies,teleport,false);
    }
    static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic)throws Exception {
        return open(installation,shared,tls,supplies,teleport,npcMagic,false);
    }
    static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic,boolean bots)throws Exception {
        return open(installation,shared,tls,supplies,teleport,npcMagic,bots,false);
    }
    static ModernQuestSqlFixture openTimeAttack(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,true,false,false,false,true);
    }
    static ModernQuestSqlFixture openNpcMagicTimeAttack(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,true,false,true,false,true);
    }
    static ModernQuestSqlFixture openNpc77TimeAttack(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,true,false,false,false,true,true);
    }
    static ModernQuestSqlFixture openNpc77Quest(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,false,false,false,false,false,true);
    }
    static ModernQuestSqlFixture openNpc70Quest(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,false,false,false,false,false,false,true);
    }
    static ModernQuestSqlFixture openMatchmakingQuest(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,false,false,false,false,false,false,true,true);
    }
    static ModernQuestSqlFixture openNpc70TimeAttack(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,true,false,false,false,true,false,true);
    }
    /** Separate normal PvP binding: enabling GH must not reinterpret NPC quest rules. */
    static ModernQuestSqlFixture openGreaterHaste(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,false);
    }
    static ModernQuestSqlFixture openTargetHaste(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,true);
    }
    static ModernQuestSqlFixture openSlow(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,false,true);
    }
    static ModernQuestSqlFixture openSlow(Path installation,Path shared,Path tls,long activeTimeoutMillis)throws Exception {
        return openHaste(installation,shared,tls,false,true,activeTimeoutMillis);
    }
    /** R383 operator-only Map514 fixture: one Main exposes both Slow and
     * player-healing policies without changing either production ruleset. */
    static ModernQuestSqlFixture openSlowPlayerHealingWindow(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,false,true,600000,true);
    }
    /** Operator-owned local showcase: real Map514, one GM plus bots, finite 30-minute matches. */
    static ModernQuestSqlFixture openSlowPlayerHealingDev(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,false,true,1_800_000,true,false,false,0,true);
    }
    static ModernQuestSqlFixture openSlowQuest(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,true,false,true,true,false,false,false,false,false,true);
    }
    /** One real Main hosts all six policies; no reconnection or synthetic source repository. */
    static ModernQuestSqlFixture openMixedModes(Path installation,Path shared,Path tls,boolean combatResults)throws Exception {
        return openMixedModes(installation,shared,tls,combatResults,false);
    }
    static ModernQuestSqlFixture openMixedModes(Path installation,Path shared,Path tls,boolean combatResults,boolean allResults)throws Exception {
        if(allResults&&!combatResults)throw new IllegalArgumentException("completed combat policy required");
        long started=System.nanoTime();
        var mysql=ModernSettlementMysqlFixture.open(installation);ModernServerConfig config=null;
        ModernMixedModesSqlTestPeer.boot("mysql-ready",started);
        try {
            var directory=Files.createTempDirectory("modern-mixed-modes-sql-").toAbsolutePath().normalize();
            for(String name:List.of("equipment","outbox","race-outbox"))Files.createDirectory(directory.resolve(name));
            String secret=UUID.randomUUID().toString();install(mysql,secret,true,false,true,true);
            ModernPetRaceTestPeer.install(mysql,secret);
            ModernMixedModesSqlTestPeer.boot("base-source-schema",started);
            var p=ModernMixedModesSqlTestPeer.worlds(shared);
            // Only the new combat-result scenario qualifies damage magic;
            // the original withdrawal scenario retains its exact policy.
            if(combatResults)p.setProperty("world.1.514.spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);
            ModernMixedModesSqlTestPeer.boot("pinned-policies",started);
            Map.of("enabled","true","outboxDirectory",portable(directory.resolve("race-outbox")),
                    "mysql.host","127.0.0.1","mysql.port",Integer.toString(mysql.port()),
                    "mysql.database",ModernPetRaceTestPeer.DATABASE,"mysql.username.env","R_U",
                    "mysql.password.env","R_P","mysql.tls","disabled")
                    .forEach((k,v)->p.setProperty("raceSettlement."+k,v));
            config=configuration(mysql,directory,tls,secret,p,true);
            var fixture=new ModernQuestSqlFixture(mysql,directory,config);
            ModernGreaterHasteRealMysqlTest.install(fixture);installSlow(fixture);
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                table(c,CATALOG,"角色_智力成長",List.of("id BIGINT PRIMARY KEY"),"治癒骰加成");
                insertZeroGrowthRows(c,"角色_智力成長",127);
                execute(c,"UPDATE `"+source(7)+"`.`characters` SET Intel=5000000000");
                // The new all-mode owned fixture uses the same GM weapon as
                // the existing shared Siege test. Keep the real pinned Kent
                // geometry and 10,000-HP towers; never change player SQL.
                if(allResults)execute(c,"UPDATE `"+CATALOG+"`.`weapon` SET dmgmodifier=20000 WHERE item_id=1");
            }
            ModernMixedModesSqlTestPeer.boot("source-qualification-installed",started);
            return fixture;
        }catch(Throwable failure){try{if(config!=null)config.close();}finally{mysql.close();}throw failure;}
    }
    static ModernQuestSqlFixture openTargetHasteQuest(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,true,false,true,true,false,false,false,false,true);
    }
    static ModernQuestSqlFixture openTargetHasteTimeAttack(Path installation,Path shared,Path tls)throws Exception {
        return open(installation,shared,tls,true,false,true,false,true,false,false,false,true);
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted)throws Exception {
        return openHaste(installation,shared,tls,targeted,false);
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted,boolean slow)throws Exception {
        return openHaste(installation,shared,tls,targeted,slow,120000);
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted,boolean slow,long activeTimeoutMillis)throws Exception {
        return openHaste(installation,shared,tls,targeted,slow,activeTimeoutMillis,false);
    }
    static ModernQuestSqlFixture openPlayerHealing(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,false,false,120000,true);
    }
    static ModernQuestSqlFixture openArmorProtection(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,false,false,120000,true,false,true);
    }
    static ModernQuestSqlFixture openFullVip(Path installation,Path shared,Path tls,boolean bothSources)throws Exception {
        return openHaste(installation,shared,tls,false,false,120000,true,false,false,bothSources?2:1);
    }
    static ModernQuestSqlFixture openMixedPlayerHealing(Path installation,Path shared,Path tls)throws Exception {
        return openHaste(installation,shared,tls,false,false,120000,true,true);
    }
    /** Two genuine Quest lanes, source-qualified healing, passive BOTs and durable results. */
    static ModernQuestSqlFixture openQuestPlayerHealing(Path installation,Path shared,Path tls)throws Exception {
        var mysql=ModernSettlementMysqlFixture.open(installation);ModernServerConfig config=null;
        try {
            var directory=Files.createTempDirectory("modern-healing-quest-sql-").toAbsolutePath().normalize();
            Files.createDirectory(directory.resolve("equipment"));Files.createDirectory(directory.resolve("outbox"));
            String secret=UUID.randomUUID().toString();install(mysql,secret,true,false,true,true);
            // Reuse the already-qualified Quest timed/target-haste policy;
            // bare timed rules are deliberately forbidden for NPC worlds.
            var p=worlds(shared,false,true,true,false,null,false,false,false,true,false);
            p.setProperty("world.2.554.playerHealingRules",ModernPlayerHealing.RULESET);
            config=configuration(mysql,directory,shared,tls,secret,false,true,true,false,p);
            var fixture=new ModernQuestSqlFixture(mysql,directory,config);
            ModernGreaterHasteRealMysqlTest.install(fixture);ModernHasteRealMysqlTest.install(fixture);
            ModernPlayerHealingRealMysqlTest.install(fixture);
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                for(int id:List.of(7,42))execute(c,"UPDATE `"+source(id)+"`.`characters` SET MaxHp=1000,CurHp=1000");
                execute(c,"UPDATE `"+source(7)+"`.`characters` SET Intel=5000000000");
            }
            return fixture;
        }catch(Throwable failure){try{if(config!=null)config.close();}finally{mysql.close();}throw failure;}
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted,boolean slow,long activeTimeoutMillis,boolean playerHealing)throws Exception {
        return openHaste(installation,shared,tls,targeted,slow,activeTimeoutMillis,playerHealing,false);
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted,boolean slow,long activeTimeoutMillis,boolean playerHealing,boolean mixedHealing)throws Exception {
        return openHaste(installation,shared,tls,targeted,slow,activeTimeoutMillis,playerHealing,mixedHealing,false);
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted,boolean slow,long activeTimeoutMillis,boolean playerHealing,boolean mixedHealing,boolean armorProtection)throws Exception {
        return openHaste(installation,shared,tls,targeted,slow,activeTimeoutMillis,playerHealing,mixedHealing,armorProtection,0);
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted,boolean slow,long activeTimeoutMillis,boolean playerHealing,boolean mixedHealing,boolean armorProtection,int vipSources)throws Exception {
        return openHaste(installation,shared,tls,targeted,slow,activeTimeoutMillis,playerHealing,mixedHealing,armorProtection,vipSources,false);
    }
    private static ModernQuestSqlFixture openHaste(Path installation,Path shared,Path tls,boolean targeted,boolean slow,long activeTimeoutMillis,boolean playerHealing,boolean mixedHealing,boolean armorProtection,int vipSources,boolean devBots)throws Exception {
        if(mixedHealing&&!playerHealing)throw new IllegalArgumentException("explicit healing policy required");
        if(activeTimeoutMillis!=120000&&!(slow&&activeTimeoutMillis==600000)&&!(devBots&&slow&&playerHealing&&activeTimeoutMillis==1_800_000))
            throw new IllegalArgumentException("unsupported owned fixture world timeout");
        var mysql=ModernSettlementMysqlFixture.open(installation);ModernServerConfig config=null;
        try {
            var directory=Files.createTempDirectory("modern-haste-sql-").toAbsolutePath().normalize();
            Files.createDirectory(directory.resolve("equipment"));Files.createDirectory(directory.resolve("outbox"));
            String secret=UUID.randomUUID().toString();install(mysql,secret,true,false,mixedHealing,mixedHealing||devBots);
            ModernSlowSqlTestPeer.boot("source-schema");
            try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                if(!mixedHealing) {
                    table(c,CATALOG,"角色_精神成長",List.of("id BIGINT PRIMARY KEY"),"魔防");
                    insertZeroGrowthRows(c,"角色_精神成長",50);
                }
                execute(c,"UPDATE `"+source(7)+"`.`characters` SET Intel=5000000000");
            }
            var map=ModernWorldTestPeer.sharedMap(shared);ModernSlowSqlTestPeer.boot("shared-map");
            var p=new Properties();String prefix="world.1.514.";
            p.setProperty("world.bindings","1:514");p.setProperty("world.sharedRoot",portable(shared));
            Map.ofEntries(Map.entry("physicalMap","514"),Map.entry("spawnX","32724"),Map.entry("spawnY","32784"),
                    Map.entry("sectors","7fff8000"),Map.entry("sectorPins","7fff8000:137784:"+ModernWorldTestPeer.RAW_SHA),
                    Map.entry("manifestSha256",map.digest()),Map.entry("roomCapacity","2"),Map.entry("stepMillis","200"),
                    Map.entry("combatRules",ModernPhysicalCombat.RULESET),Map.entry("attackMillis","700"),
                    Map.entry("spellRules",ModernWorldConfiguration.SPELL_RULESET),Map.entry("castMillis","700"),
                    Map.entry("itemRules",ModernWorldConfiguration.ITEM_RULESET),Map.entry("weightRules",ModernWorldConfiguration.WEIGHT_RULESET),
                    Map.entry("buffRules",ModernWorldConfiguration.TIMED_RULESET),Map.entry(slow?"slowRules":targeted?"hasteRules":"greaterHasteRules",slow?ModernSlow.RULESET:targeted?ModernHaste.RULESET:ModernGreaterHaste.RULESET),
                    Map.entry("countdownNanos","0"),Map.entry("activeTimeoutMillis",Long.toString(activeTimeoutMillis)))
                    .forEach((k,v)->p.setProperty(prefix+k,v));
            if(devBots)p.setProperty(prefix+"botRules",ModernWorldConfiguration.BOT_RULESET);
            if(playerHealing) {
                p.remove(prefix+"greaterHasteRules");
                p.setProperty(prefix+"playerHealingRules",ModernPlayerHealing.RULESET);
                p.setProperty(prefix+"regenRules",ModernNaturalRegen.RULESET);
            }
            if(mixedHealing) {
                ModernMixedModesSqlTestPeer.addHonor(p,shared);
                p.setProperty("world.bindings","1:514,5:532");
                p.setProperty("world.5.532.botRules",ModernWorldConfiguration.BOT_RULESET);
                p.setProperty("world.5.532.playerHealingRules",ModernPlayerHealing.RULESET);
            }
            if(vipSources>0)p.remove(prefix+"playerHealingRules");
            if(armorProtection||vipSources>0) {
                // Same shared physical map, two explicit logical policies.
                // The ordinary map must never inherit the armor contract.
                var keys=List.copyOf(p.stringPropertyNames());
                for(String key:keys)if(key.startsWith(prefix))p.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
                p.setProperty("world.bindings","1:514,1:509");
                if(armorProtection)p.setProperty(prefix+"armorProtectionRules",ModernArmorProtectionCatalog.RULESET);
            }
            if(vipSources>0) {
                p.setProperty(prefix+"fullVipRules",ModernWorldConfiguration.FULL_VIP_RULESET);
                p.setProperty("source.7.fullVipRules",ModernWorldConfiguration.FULL_VIP_RULESET);
                if(vipSources==2)p.setProperty("source.42.fullVipRules",ModernWorldConfiguration.FULL_VIP_RULESET);
            }
            config=configuration(mysql,directory,shared,tls,secret,false,false,false,false,p);
            ModernSlowSqlTestPeer.boot("configuration");
            var fixture=new ModernQuestSqlFixture(mysql,directory,config);
            ModernGreaterHasteRealMysqlTest.install(fixture);
            if(targeted)ModernHasteRealMysqlTest.install(fixture);
            if(slow)installSlow(fixture);
            if(playerHealing)ModernPlayerHealingRealMysqlTest.install(fixture);
            ModernSlowSqlTestPeer.boot("skill-schema");
            return fixture;
        }catch(Throwable failure){try{if(config!=null)config.close();}finally{mysql.close();}throw failure;}
    }
    /** Isolated SQL + caller-owned synthetic shared S32 policy for v26 Main tests. */
    static ModernQuestSqlFixture openSiege(Path installation,Path tls,Properties worlds)throws Exception {
        var mysql=ModernSettlementMysqlFixture.open(installation);ModernServerConfig config=null;
        try {var directory=Files.createTempDirectory("modern-quest-sql-").toAbsolutePath().normalize();
            Files.createDirectory(directory.resolve("equipment"));Files.createDirectory(directory.resolve("outbox"));
            String secret=UUID.randomUUID().toString();install(mysql,secret,true,false,false,true);
            config=configuration(mysql,directory,null,tls,secret,false,false,true,false,worlds);return new ModernQuestSqlFixture(mysql,directory,config);
        }catch(Throwable failure){try{if(config!=null)config.close();}finally{mysql.close();}throw failure;}
    }
    private static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack)throws Exception {
        return open(installation,shared,tls,supplies,teleport,npcMagic,bots,timeAttack,false);
    }
    private static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,boolean npc77)throws Exception {
        return open(installation,shared,tls,supplies,teleport,npcMagic,bots,timeAttack,npc77,false);
    }
    private static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,boolean npc77,boolean npc70)throws Exception {
        return open(installation,shared,tls,supplies,teleport,npcMagic,bots,timeAttack,npc77,npc70,false);
    }
    private static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,boolean npc77,boolean npc70,boolean matchmaking)throws Exception {
        return open(installation,shared,tls,supplies,teleport,npcMagic,bots,timeAttack,npc77,npc70,matchmaking,false);
    }
    private static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,boolean npc77,boolean npc70,boolean matchmaking,boolean targetedHaste)throws Exception {
        return open(installation,shared,tls,supplies,teleport,npcMagic,bots,timeAttack,npc77,npc70,matchmaking,targetedHaste,false);
    }
    private static ModernQuestSqlFixture open(Path installation,Path shared,Path tls,boolean supplies,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,boolean npc77,boolean npc70,boolean matchmaking,boolean targetedHaste,boolean slow)throws Exception {
        var mysql=ModernSettlementMysqlFixture.open(installation);Path directory=null;ModernServerConfig config=null;
        try {
            directory=Files.createTempDirectory("modern-quest-sql-").toAbsolutePath().normalize();
            Files.createDirectory(directory.resolve("equipment"));Files.createDirectory(directory.resolve("outbox"));
            String secret=UUID.randomUUID().toString();install(mysql,secret,supplies,teleport,npcMagic,bots);
            if(npc77)try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                if(!npcMagic) {
                    table(c,CATALOG,"角色_精神成長",List.of("id BIGINT PRIMARY KEY"),"魔防");
                    for(int id=1;id<=50;id++)execute(c,"INSERT INTO `"+CATALOG+"`.`角色_精神成長` VALUES("+id+",0)");
                }
                ModernNpcDisintegrateRealMysqlTest.install(c,1);
                ModernNpcDisintegrateRealMysqlTest.execute(c,"INSERT INTO `"+source(42)+"`.`character_skills` VALUES(3333,77)");
                // Own fixture only: short real attack cycles make death/revive
                // coverage bounded while leaving the production RNG untouched.
                if(!timeAttack)execute(c,"UPDATE `"+CATALOG+"`.`npc` SET passispeed=4000,atkspeed=5000");
            }
            if(npc70)try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(10))) {
                if(npc77||npcMagic)throw new IllegalArgumentException("separate NPC70 qualification fixture");
                table(c,CATALOG,"角色_精神成長",List.of("id BIGINT PRIMARY KEY"),"魔防");
                for(int id=1;id<=50;id++)execute(c,"INSERT INTO `"+CATALOG+"`.`角色_精神成長` VALUES("+id+",0)");
                ModernNpcFireStormSqlFixture.install(c);
            }
            config=configuration(mysql,directory,shared,tls,secret,teleport,npcMagic,bots,timeAttack,null,npc77,npc70,matchmaking,targetedHaste,slow);
            var result=new ModernQuestSqlFixture(mysql,directory,config);
            if(targetedHaste||slow) {
                ModernGreaterHasteRealMysqlTest.install(result);
                if(targetedHaste)ModernHasteRealMysqlTest.install(result);
                if(slow)installSlow(result);
                try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
                    execute(c,"UPDATE `"+source(7)+"`.`characters` SET Intel=5000000000");
                }
            }
            return result;
        }catch(Throwable failure){try{if(config!=null)config.close();}finally{mysql.close();}throw failure;}
    }
    private static void installSlow(ModernQuestSqlFixture fixture)throws Exception {
        ModernSlowRealMysqlTest.install(fixture);
        try(var c=fixture.mysql.openConnection(ModernSettlementMysqlFixture.deadline(5))) {
            execute(c,"INSERT INTO `"+CATALOG+"`.`etcitem` (item_id,name_id,classname,item_type,use_type,material,invgfx,grdgfx,weight,stackable,limit_class) VALUES(40318,'$508','0','material','none','gemstone',235,773,2000,1,127)");
            execute(c,"INSERT INTO `"+CATALOG+"`.`lito_tournament_shop` VALUES(40318,1,100,50,1)");
        }
    }
    static String source(int id){if(id!=7&&id!=42)throw new IllegalArgumentException();return "quest_source_"+id;}
    private static void install(ModernSettlementMysqlFixture mysql,String secret,boolean supplies,boolean teleport,boolean npcMagic,boolean bots)throws Exception {
        ModernNpcCatalogRealMysqlTest.install(mysql,secret);
        try(var c=mysql.openConnection(ModernSettlementMysqlFixture.deadline(20))) {
            execute(c,"UPDATE `"+CATALOG+"`.`npc` SET hp=1,passispeed="+(supplies?500:4000)+",atkspeed="+(supplies?1000:60000));
            catalog(c);
            for(int id:List.of(7,42)){
                characters(c,id);
                // Explicit test balance for real RNG damage followed by real
                // potion recovery. Never changes a configured source server.
                if(supplies)execute(c,"UPDATE `"+source(id)+"`.`characters` SET MaxHp=1000,CurHp=1000");
                if(teleport)execute(c,"INSERT INTO `"+source(id)+"`.`character_skills` VALUES(3333,5)");
                if(npcMagic)execute(c,"INSERT INTO `"+source(id)+"`.`character_skills` VALUES(3333,4)");
            }
            if(bots) {
                execute(c,"UPDATE `"+source(7)+"`.`accounts` SET access_level=200");
                execute(c,"INSERT INTO `"+CATALOG+"`.`commands`(name,class_name,access_level,`system`) VALUES('de','L1SpawnDe',200,0)");
            }
            if(teleport)execute(c,"INSERT INTO `"+CATALOG+"`.`skills`(skill_id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,type,action_id,castgfx) VALUES(5,'$1440','Teleport','none',1,4,10,10,128,19,169)");
            if(npcMagic){
                execute(c,"INSERT INTO `"+CATALOG+"`.`skills`(skill_id,nameid,name,target,skill_level,skill_number,mpConsume,reuseDelay,type,action_id,castgfx,target_to,damage_value,damage_dice,damage_dice_count,attr,ranged) VALUES(4,'$1439','Light Arrow','attack',1,3,3,10,64,18,167,3,6,10,1,16,10)");
                table(c,CATALOG,"角色_精神成長",List.of("id BIGINT PRIMARY KEY"),"魔防");
                for(int id=1;id<=50;id++)execute(c,"INSERT INTO `"+CATALOG+"`.`角色_精神成長` VALUES("+id+",0)");
            }
            for(String user:List.of(READER,WRITER))try(var s=c.prepareStatement("CREATE USER '"+user+"'@'127.0.0.1' IDENTIFIED BY ?")){s.setQueryTimeout(3);s.setString(1,secret);s.executeUpdate();}
            for(String db:List.of(source(7),source(42),CATALOG))execute(c,"GRANT SELECT ON `"+db+"`.* TO '"+READER+"'@'127.0.0.1'");
            execute(c,"GRANT SELECT,INSERT,UPDATE ON `"+ModernSettlementMysqlFixture.DATABASE+"`.* TO '"+WRITER+"'@'127.0.0.1'");
        }
    }
    private static void characters(Connection c,int source)throws Exception {
        String db=source(source);execute(c,"CREATE DATABASE `"+db+"` CHARACTER SET utf8mb4");
        table(c,db,"accounts",List.of("login VARCHAR(64) PRIMARY KEY","password VARCHAR(256) NOT NULL"),"banned access_level");
        table(c,db,"characters",List.of("objid BIGINT PRIMARY KEY","account_name VARCHAR(64) NOT NULL","char_name VARCHAR(256) NOT NULL"),
                "Type Sex level Banned MaxHp MaxMp CurHp CurMp Ac Str Dex Con Intel Wis Cha Exp Lawful OriginalStr OriginalDex OriginalCon OriginalInt OriginalWis Food");
        table(c,db,"character_items",List.of("id BIGINT PRIMARY KEY","magic_stone_time DATETIME NULL"),
                "char_id item_id count is_equipped enchantlvl durability is_id bless is_sealed charge_count greater magic_stone_item_id magic_stone_level attr_enchant_kind attr_enchant_level remaining_time");
        table(c,db,"character_items_card",List.of("item_objid BIGINT PRIMARY KEY"),"card_1 card_2 card_3 card_4 card_5");
        table(c,db,"character_items_dark",List.of("item_objid BIGINT PRIMARY KEY"),
                "add_str add_dex add_con add_int add_wis add_cha add_hp add_mp add_potion_recovery add_sp add_mr add_magic_dmg_modifier add_magic_hit "
                +"add_regist_blind add_regist_sleep add_regist_freeze add_regist_stone add_regist_sustain add_regist_stun add_stun_success "
                +"add_def_fire add_def_water add_def_wind add_def_earth add_dmg_redux add_hp_regen add_mp_regen add_weight_reduction "
                +"add_hit add_dmg add_bow_hit add_bow_dmg add_ac add_er add_melee_redux add_bow_redux add_ignore_reduction");
        table(c,db,"character_items_time",List.of("itemr_obj_id BIGINT PRIMARY KEY","usertime DATETIME NOT NULL"),"");
        table(c,db,"character_skills",List.of("char_obj_id BIGINT NOT NULL","skill_id BIGINT NOT NULL","PRIMARY KEY(char_obj_id,skill_id)"),"");
        try(var s=c.prepareStatement("INSERT INTO `"+db+"`.`accounts`(login,password) VALUES(?,?)")){s.setQueryTimeout(3);s.setString(1,ACCOUNT);s.setString(2,PASSWORD);s.executeUpdate();}
        execute(c,"INSERT INTO `"+db+"`.`characters`(objid,account_name,char_name,Type,Sex,level,MaxHp,MaxMp,CurHp,CurMp,Ac,Str,Dex,Con,Intel,Wis,Cha,Exp,Lawful,OriginalStr,OriginalDex,OriginalCon,OriginalInt,OriginalWis,Food)"
                +" VALUES(3333,'fixture-user','"+(source==7?"SQL Sun Knight":"SQL Moon Mage")+"',"+(source==7?1:3)+","+(source==7?0:1)+",80,1,"+(source==7?30:40)+",1,"+(source==7?30:40)
                +",10,18,18,18,12,18,18,"+(source==7?9007199254740993L:9007199254740995L)+",0,18,18,18,12,18,"+(source==7?225:180)+")");
        execute(c,"INSERT INTO `"+db+"`.`character_items`(id,char_id,item_id,count,is_equipped,is_id,bless) VALUES(1001,3333,1,1,1,1,0),(1002,3333,40010,"+(source==7?2:5)+",0,1,0)");
    }
    private static void catalog(Connection c)throws Exception {
        String base="invgfx grdgfx weight min_lvl max_lvl safenchant use_royal use_knight use_elf use_mage use_darkelf use_dragonknight use_illusionist max_use_time ";
        String common="add_str add_dex add_con add_int add_wis add_cha add_hp add_mp add_potion_recovery add_sp magicdmgmodifier add_magic_hit "
                +"regist_blind regist_sleep regist_freeze regist_stone regist_sustain regist_stun add_stun_success add_hpr add_mpr add_hitup add_dmgup add_bow_hitup add_bow_dmgup "
                +"add_er add_melee_redux add_bow_redux add_ignore_reduction add_pvp_redux ";
        var identity=List.of("item_id BIGINT PRIMARY KEY","name_id VARCHAR(256) NOT NULL DEFAULT ''","type VARCHAR(32) NOT NULL DEFAULT ''","set_id VARCHAR(256) NULL","classname VARCHAR(256) NOT NULL DEFAULT '0'");
        table(c,CATALOG,"weapon",concat(identity,List.of("material VARCHAR(32) NOT NULL DEFAULT 'iron'","dmg_cri DECIMAL(10,3) NOT NULL DEFAULT 0")),
                base+common+"add_pierce hitmodifier dmgmodifier dmg_small dmg_large range bless dmg_cri_chance dmg_max_chance");
        table(c,CATALOG,"armor",identity,base+common+"m_def defense_fire defense_water defense_wind defense_earth ac hit_modifier dmg_modifier bow_hit_modifier bow_dmg_modifier damage_reduction weight_reduction");
        table(c,CATALOG,"etcitem",List.of("item_id BIGINT PRIMARY KEY","name_id VARCHAR(256) NOT NULL DEFAULT ''","classname VARCHAR(256) NOT NULL DEFAULT '0'",
                "item_type VARCHAR(32) NOT NULL DEFAULT 'other'","use_type VARCHAR(32) NOT NULL DEFAULT 'none'","material VARCHAR(32) NOT NULL DEFAULT 'none'"),
                "invgfx grdgfx weight stackable max_charge_count min_lvl max_lvl bless trade cant_delete delay_id delay_time delay_effect food_volume save_at_once limit_class");
        execute(c,"INSERT INTO `"+CATALOG+"`.`weapon`(item_id,name_id,type,invgfx,grdgfx,weight,use_royal,use_knight,use_elf,use_mage,use_darkelf,use_dragonknight,use_illusionist,safenchant,dmg_small,dmg_large,`range`) VALUES(1,'$1','sword',1,1,1000,1,1,1,1,1,1,1,6,8,12,1)");
        execute(c,"INSERT INTO `"+CATALOG+"`.`etcitem`(item_id,name_id,classname,item_type,use_type,material,invgfx,grdgfx,weight,stackable,delay_id,delay_time,limit_class)"
                +" VALUES(40010,'$26','hp.UserAddHp 10 20 189 true 0 0','potion','hpr','glass',80,45,3900,1,2,500,127)");
        String rules="add_str add_dex add_int add_wis add_con add_cha add_hp add_mp add_potion_recovery add_sp add_mr add_hpr add_mpr add_magic_dmg add_magic_hit "
                +"regist_blind regist_sleep regist_freeze regist_stone regist_sustain regist_stun add_stun_success add_hit add_dmg add_bow_hit add_bow_dmg add_pierce add_ac add_er "
                +"add_reduction add_melee_redux add_bow_redux add_ignore_reduction add_pvp_redux add_fire add_water add_wind add_earth";
        table(c,CATALOG,"裝備_weapon_greater_類型",List.of(),"weapon_type tier_id "+rules);
        table(c,CATALOG,"裝備_weapon_bless_類型",List.of(),"weapon_type bless_id "+rules);
        table(c,CATALOG,"裝備_weapon_bless_指定",List.of(),"item_id bless_id "+rules);
        table(c,CATALOG,"裝備_特殊_卡片_組合",List.of("combo_id BIGINT PRIMARY KEY","require_cards VARCHAR(4096) NOT NULL"),rules+" add_weight_reduction");
        // MySQL 5.5 on this Windows filesystem returns a file error (not
        // certified 1146 absence) for this missing Unicode table. Retain the
        // production fail-closed guard; explicitly qualify an empty table.
        table(c,CATALOG,"道具_自訂穿脫能力",List.of("`道具編號` BIGINT PRIMARY KEY"),
                "力量 敏捷 智力 精神 體質 抗魔 魔攻 回血 回魔 暗黑耐性 睡眠耐性 寒冰耐性 石化耐性 支撐耐性 昏迷耐性 火屬性防禦 水屬性防禦 風屬性防禦 地屬性防禦 近戰命中 近戰傷害 遠攻命中 遠攻傷害");
        table(c,CATALOG,"道具_物品時間限制",List.of("itemid BIGINT PRIMARY KEY"),"h");
        table(c,CATALOG,"裝備_weapon_魔法",List.of("id BIGINT PRIMARY KEY"),"weapon_id");
        table(c,CATALOG,"裝備_armor_魔法",List.of("id BIGINT PRIMARY KEY"),"item_id");
        table(c,CATALOG,"角色_體質成長",List.of("id BIGINT PRIMARY KEY"),"回血");
        for(int id=1;id<=50;id++)execute(c,"INSERT INTO `"+CATALOG+"`.`角色_體質成長` VALUES("+id+",1)");
        table(c,CATALOG,"lito_tournament_shop",List.of("item_id BIGINT PRIMARY KEY"),"sort_order buy_price sell_price enabled");
        execute(c,"INSERT INTO `"+CATALOG+"`.`lito_tournament_shop` VALUES(40010,0,40,20,1)");
        table(c,CATALOG,"commands",List.of("name VARCHAR(32) PRIMARY KEY","class_name VARCHAR(128) NOT NULL"),"access_level system");
        table(c,CATALOG,"skills",List.of("skill_id BIGINT PRIMARY KEY","nameid VARCHAR(256) NOT NULL DEFAULT ''","name VARCHAR(256) NOT NULL DEFAULT ''","target VARCHAR(256) NOT NULL DEFAULT ''"),
                "skill_level skill_number mpConsume hpConsume itemConsumeId itemConsumeCount reuseDelay type action_id castgfx effect_skill_id buffDuration target_to damage_value damage_dice damage_dice_count probability_value probability_dice attr lawful ranged area through castgfx2");
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path shared,Path tls,String secret,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack)throws Exception {
        return configuration(mysql,directory,shared,tls,secret,teleport,npcMagic,bots,timeAttack,null);
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path shared,Path tls,String secret,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,Properties override)throws Exception {
        return configuration(mysql,directory,shared,tls,secret,teleport,npcMagic,bots,timeAttack,override,false);
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path shared,Path tls,String secret,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,Properties override,boolean npc77)throws Exception {
        return configuration(mysql,directory,shared,tls,secret,teleport,npcMagic,bots,timeAttack,override,npc77,false);
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path shared,Path tls,String secret,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,Properties override,boolean npc77,boolean npc70)throws Exception {
        return configuration(mysql,directory,shared,tls,secret,teleport,npcMagic,bots,timeAttack,override,npc77,npc70,false);
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path shared,Path tls,String secret,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,Properties override,boolean npc77,boolean npc70,boolean matchmaking)throws Exception {
        return configuration(mysql,directory,shared,tls,secret,teleport,npcMagic,bots,timeAttack,override,npc77,npc70,matchmaking,false);
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path shared,Path tls,String secret,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,Properties override,boolean npc77,boolean npc70,boolean matchmaking,boolean targetedHaste)throws Exception {
        return configuration(mysql,directory,shared,tls,secret,teleport,npcMagic,bots,timeAttack,override,npc77,npc70,matchmaking,targetedHaste,false);
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path shared,Path tls,String secret,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,Properties override,boolean npc77,boolean npc70,boolean matchmaking,boolean targetedHaste,boolean slow)throws Exception {
        Properties p=worlds(shared,teleport,npcMagic,bots,timeAttack,override,npc77,npc70,matchmaking,targetedHaste,slow);
        return configuration(mysql,directory,tls,secret,p,override!=null);
    }
    /** Same pinned policies as the single-mode fixtures, reusable without starting another Main. */
    static Properties worlds(Path shared,boolean teleport,boolean npcMagic,boolean bots,boolean timeAttack,Properties override,boolean npc77,boolean npc70,boolean matchmaking,boolean targetedHaste,boolean slow)throws Exception {
        Properties p=new Properties();if(override!=null)p.putAll(override);else {
        var map=ModernWorldMap.load(shared,554,ModernQuestMapQualificationTest.PINS.stream().map(ModernWorldMap.SectorManifest::key).collect(java.util.stream.Collectors.toSet()),()->false);
        var points=map.serverChosenSpawns(ModernQuestMapQualificationTest.ANCHOR,8);var camp=points.get(0);var birth=points.get(1);
        var target=points.stream().filter(c->!c.equals(camp)&&!c.equals(birth)&&Math.max(Math.abs(c.x()-camp.x()),Math.abs(c.y()-camp.y()))==1).findFirst().orElseThrow();
        String prefix="world.2.554.";p.setProperty("world.bindings","2:554");p.setProperty("world.sharedRoot",portable(shared));
        p.setProperty(prefix+"physicalMap","554");p.setProperty(prefix+"spawnX",""+camp.x());p.setProperty(prefix+"spawnY",""+camp.y());
        p.setProperty(prefix+"sectors",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"sectorPins",map.manifest().sectors().stream().map(s->String.format("%04x%04x",s.key().x(),s.key().y())+":"+s.byteLength()+":"+s.rawSha256()).collect(java.util.stream.Collectors.joining(",")));
        p.setProperty(prefix+"manifestSha256",map.digest());
        if(teleport||npcMagic||npc77||npc70){p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","1200");}
        if(npcMagic)p.setProperty(prefix+"npcMagicRules",ModernNpcMagic.RULESET);
        if(npc77)p.setProperty(prefix+"npcDisintegrateRules",ModernDisintegrate.RULESET);
        if(npc70)p.setProperty(prefix+"npcFireStormRules",ModernNpcFireStorm.RULESET);
        if(bots)p.setProperty(prefix+"botRules",ModernWorldConfiguration.BOT_RULESET);
        Map.ofEntries(Map.entry("stepMillis","100"),Map.entry("attackMillis","700"),Map.entry("combatRules",ModernPhysicalCombat.RULESET),
                Map.entry("itemRules",ModernWorldConfiguration.ITEM_RULESET),Map.entry("weightRules",ModernWorldConfiguration.WEIGHT_RULESET),Map.entry("regenRules",ModernWorldConfiguration.REGEN_RULESET),
                Map.entry("npcRules",ModernNpcMelee.RULESET),Map.entry("npcCatalogSource","7"),Map.entry("npcCatalogDatabase",CATALOG),Map.entry("npcIds","45262"),
                Map.entry("npcCapacity","8"),Map.entry("npcPlayerCapacity","4"),Map.entry("npcCorpseMillis","1000"),Map.entry("npcPathBudget","16"),
                Map.entry("npcClock",ModernNpcConfiguration.CLOCK),Map.entry("npcNightStartSecond","64800"),Map.entry("npcNightEndSecond","21600"),
                Map.entry("questRules",ModernQuestConfiguration.RULESET),Map.entry("questLives",ModernQuestConfiguration.LIVES),
                Map.entry("countdownNanos","0"),Map.entry("activeTimeoutMillis","120000")).forEach((k,v)->p.setProperty(prefix+k,v));
        for(int lane=0;lane<2;lane++){p.setProperty(prefix+"npcCamp"+lane,camp.x()+","+camp.y());for(int stage=1;stage<=4;stage++)p.setProperty(prefix+ModernQuestConfiguration.waveKey(lane,stage),"45262,"+birth.x()+","+birth.y()+",2");}
        p.setProperty(prefix+"questObjectives","0,4,"+target.x()+","+target.y()+",2665,1;1,4,"+target.x()+","+target.y()+",2669,1");
        if(timeAttack) {
            for(String suffix:ModernQuestConfiguration.SUFFIXES)p.remove(prefix+suffix);
            p.remove(prefix+"npcCamp1");p.setProperty(prefix+"activeTimeoutMillis","60000");
            p.setProperty(prefix+"timeAttackRules",ModernTimeAttackConfiguration.RULESET);
            p.setProperty(prefix+"timeAttackTower",target.x()+","+target.y()+",2665,1");p.setProperty(prefix+"timeAttackDurationMillis","20000");
            p.setProperty(prefix+"timeAttackWaves","ORDINARY,FINAL_BOSSES");
            for(int i=0;i<2;i++)p.setProperty(prefix+"timeAttackWave"+i,"45262,"+birth.x()+","+birth.y()+",2");
            var copy=new Properties();for(String name:p.stringPropertyNames())copy.setProperty(name.replace(prefix,"world.4.540."),p.getProperty(name));
            p.clear();p.putAll(copy);p.setProperty("world.bindings","4:540");
        }
        }
        if(targetedHaste||slow) {
            if(override!=null||!npcMagic||bots==timeAttack)throw new IllegalArgumentException("explicit targeted cooperative or Quest fixture");
            String prefix=timeAttack?"world.4.540.":"world.2.554.";
            p.setProperty(prefix+"buffRules",ModernWorldConfiguration.TIMED_RULESET);
            if(targetedHaste)p.setProperty(prefix+"hasteRules",ModernHaste.RULESET);
            if(slow)p.setProperty(prefix+"slowRules",ModernSlow.RULESET);
        }
        if(matchmaking){p.setProperty("matchmaking.enabled","true");p.setProperty("world.2.554.countdownNanos","2000000000");}
        return p;
    }
    private static ModernServerConfig configuration(ModernSettlementMysqlFixture mysql,Path directory,Path tls,String secret,Properties p,boolean overridden)throws Exception {
        var text=new StringBuilder("schema=1\nbind.port=0\ntls.pkcs12=").append(portable(tls.resolve("server.p12"))).append("\ntls.password.env=T\nsources=7,42\n")
                .append("equipment.storeDirectory=").append(portable(directory.resolve("equipment"))).append("\nshop.allowance=20000\nlimits.backendDeadlineMillis=5000\n");
        for(int id:List.of(7,42))text.append("source.").append(id).append(".name=").append(id==7?"太陽神":"月亮").append('\n')
                .append("source.").append(id).append(".mysql.host=127.0.0.1\nsource.").append(id).append(".mysql.port=").append(mysql.port())
                .append("\nsource.").append(id).append(".mysql.database=").append(source(id)).append("\nsource.").append(id).append(".mysql.username.env=U\nsource.").append(id)
                .append(".mysql.password.env=P\nsource.").append(id).append(".mysql.passwordScheme=plain\nsource.").append(id).append(".mysql.tls=disabled\nsource.").append(id)
                .append(".catalog.database=").append(CATALOG).append("\nsource.").append(id).append(".weight.rate=1\nsource.").append(id).append(".npc.blessingConvention=LEGACY_ZERO_BLESSED\n");
        text.append(ModernQuestConfigurationTest.settlement(directory).replace(portable(directory.resolve("quest-results")),portable(directory.resolve("outbox")))
                .replace("mysql.port=3306","mysql.port="+mysql.port()).replace("mysql.database=modern_results","mysql.database="+ModernSettlementMysqlFixture.DATABASE));
        text.append(ModernNpcConfigurationTest.lines(p));
        String input=text.toString();if(overridden){var features=ModernWorldConfiguration.parse(p);
            if(!features.itemsEnabled())input=input.replace("shop.allowance=20000\n","");
            for(int id:List.of(7,42)){
                if(!features.weightEnabled())input=input.replace("source."+id+".weight.rate=1\n","");
                if(!features.npcEnabled())input=input.replace("source."+id+".npc.blessingConvention=LEGACY_ZERO_BLESSED\n","");
            }
        }
        if (System.getenv("LITO_MODERN_TEST_SERVER_JAR") != null) {
            // Test-only export contains env names, never credential values.
            Files.writeString(directory.resolve("packaged-modern.properties"), input, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        }
        return ModernServerConfig.parse(input.getBytes(StandardCharsets.UTF_8),name->switch(name){case "T"->"modern-lito-fixture-only";case "U"->READER;case "WU"->WRITER;case "R_U"->ModernPetRaceTestPeer.USER;case "P","WP","R_P"->secret;default->null;});
    }
    private static List<String> concat(List<String> a,List<String> b){var values=new ArrayList<>(a);values.addAll(b);return values;}
    private static String portable(Path p){return p.toString().replace('\\','/');}
    private static void table(Connection c,String db,String table,List<String> explicit,String numeric)throws Exception {
        var columns=new ArrayList<>(explicit);for(String column:numeric.split(" "))if(!column.isEmpty())columns.add("`"+column+"` BIGINT NOT NULL DEFAULT 0");
        execute(c,"CREATE TABLE `"+db+"`.`"+table+"` ("+String.join(",",columns)+") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
    private static void execute(Connection c,String sql)throws Exception{try(var s=c.createStatement()){s.setQueryTimeout(3);s.execute(sql);}}
    /** Same fixture rows, one bounded statement instead of N round trips within a connection deadline. */
    static void insertZeroGrowthRows(Connection c,String table,int count)throws Exception {
        if(!List.of("角色_智力成長","角色_精神成長").contains(table)||count<1||count>127)
            throw new IllegalArgumentException("Unexpected fixture growth catalog");
        var rows=new ArrayList<String>();for(int id=1;id<=count;id++)rows.add("("+id+",0)");
        execute(c,"INSERT INTO `"+CATALOG+"`.`"+table+"` VALUES"+String.join(",",rows));
    }
    void successful(){successful=true;mysql.successful();}
    @Override public void close()throws Exception {
        try{config.close();}finally{mysql.close();}
        if(successful) {
            for (String name : List.of("packaged-modern.properties", "packaged-stdout.log", "packaged-stderr.log")) {
                Path file = directory.resolve(name);
                if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IllegalStateException("unexpected packaged fixture file");
                    Files.delete(file);
                }
            }
            // Only fixed fixture subdirectories and plain files after all
            // owners retire; unexpected contents are evidence, not deleted.
            for(String child:config.raceSettlement()==null?List.of("equipment","outbox"):List.of("equipment","outbox","race-outbox")) {
                Path dir=directory.resolve(child);
                try(var entries=Files.list(dir)){for(var file:entries.toList()){if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||!file.getParent().equals(dir))throw new IllegalStateException("unexpected owned fixture contents");Files.delete(file);}}
                Files.delete(dir);
            }
            Files.delete(directory);
        }
    }
}
