package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernWorldConfigurationTest {
    private Properties fullVip()throws Exception {
        var p=combat(temporary);String prefix="world.1.514.";
        var pins=pinnedWorld();p.setProperty(prefix+"sectorPins",pins.getProperty("world.4.540.sectorPins"));
        p.setProperty(prefix+"manifestSha256",pins.getProperty("world.4.540.manifestSha256"));
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(prefix+"buffRules",ModernWorldConfiguration.TIMED_RULESET);
        p.setProperty(prefix+"regenRules",ModernWorldConfiguration.REGEN_RULESET);
        p.setProperty(prefix+"fullVipRules",ModernWorldConfiguration.FULL_VIP_RULESET);
        p.setProperty("source.7.fullVipRules",ModernWorldConfiguration.FULL_VIP_RULESET);
        p.setProperty("source.7.catalog.database","catalog");p.setProperty("source.7.weight.rate","1");
        p.setProperty("equipment.storeDirectory",portable(Files.createDirectories(temporary.resolve("equipment"))));
        p.setProperty("shop.allowance","20000");return p;
    }
    private ModernServerConfig vipConfig(Properties p)throws Exception {
        Files.createDirectories(temporary.resolve("quest-results"));
        return ModernServerConfig.parse((base()+ModernQuestConfigurationTest.settlement(temporary)+lines(p)).getBytes(StandardCharsets.UTF_8),n->"fixture-only");
    }
    private byte[] vipBinding(Properties p)throws Exception {try(var c=vipConfig(p)){return ModernSettlementCoordinator.binding(c);}}
    @Test void fullVipRequiresExplicitMapAndSourceAndPreservesOrdinaryMapping()throws Exception {
        var p=fullVip();String prefix="world.1.514.";
        var folder=Files.createDirectories(temporary.resolve("map/514"));Files.write(folder.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        p.setProperty("world.bindings","1:514,1:509");
        for(String key:List.copyOf(p.stringPropertyNames()))if(key.startsWith(prefix)&&!key.endsWith("fullVipRules"))p.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
        try(var c=vipConfig(p)) {
            assertTrue(c.sources().get(0).fullVip());assertTrue(c.worlds().fullVipEnabled());
            var mappings=c.worlds().load();assertTrue(mappings.get(new ModernWorldBinding.Key(1,514)).fullVipEnabled());
            assertFalse(mappings.get(new ModernWorldBinding.Key(1,509)).fullVipEnabled());
        }
        var first=vipBinding(p);p.setProperty("world.bindings","1:509,1:514");assertArrayEquals(first,vipBinding(p));
        p.setProperty("world.1.509.fullVipRules",ModernWorldConfiguration.FULL_VIP_RULESET);assertFalse(java.util.Arrays.equals(first,vipBinding(p)));
        p.remove("world.1.509.fullVipRules");p.remove(prefix+"fullVipRules");p.remove("source.7.fullVipRules");
        try(var c=vipConfig(p)) {
            assertFalse(c.worlds().fullVipEnabled());assertFalse(c.sources().get(0).fullVip());
            var out=new java.io.ByteArrayOutputStream();c.worlds().writeFullVipBinding(new java.io.DataOutputStream(out));assertEquals(0,out.size());
        }
        assertFalse(java.util.Arrays.equals(first,vipBinding(p)));
    }
    @Test void fullVipInvalidDependenciesRejectBeforeAnySecrets()throws Exception {
        var p=fullVip();try(var c=vipConfig(p)){assertTrue(c.worlds().fullVipEnabled());}
        for(String key:List.of("world.1.514.fullVipRules","source.7.fullVipRules","source.7.catalog.database","equipment.storeDirectory",
                "world.1.514.itemRules","world.1.514.weightRules","world.1.514.buffRules","world.1.514.regenRules","world.1.514.manifestSha256","world.1.514.sectorPins")) {
            var bad=(Properties)p.clone();bad.remove(key);rejectsBeforeSecrets(bad);
        }
        for(String key:List.of("world.1.514.fullVipRules","source.7.fullVipRules"))for(String value:List.of("","false","NATIVE_AUTO")) {
            var bad=(Properties)p.clone();bad.setProperty(key,value);rejectsBeforeSecrets(bad);
        }
        var bad=(Properties)p.clone();bad.setProperty("world.1.514.botRules",ModernWorldConfiguration.BOT_RULESET);rejectsBeforeSecrets(bad);
        var unpinned=(Properties)p.clone();unpinned.remove("world.1.514.manifestSha256");unpinned.remove("world.1.514.sectorPins");rejectsBeforeSecrets(unpinned);
    }
    @Test void fullVipSourceSubsetChangesSettlementIdentityButNamesAndOrderDoNot()throws Exception {
        var p=fullVip();p.setProperty("sources","7,42");
        var baseProps=new Properties();baseProps.load(new java.io.StringReader(base()));
        for(String key:baseProps.stringPropertyNames())if(key.startsWith("source.7."))p.setProperty(key.replace("source.7.","source.42."),baseProps.getProperty(key));
        p.setProperty("source.42.name","Moon");p.setProperty("source.42.mysql.database","synthetic_moon");
        p.setProperty("source.42.catalog.database","catalog");p.setProperty("source.42.weight.rate","1");
        // Avoid duplicating the base sources property in the strict input parser.
        var prefix=base().replace("sources=7\n","");
        Files.createDirectories(temporary.resolve("quest-results"));
        byte[] first;
        try(var c=ModernServerConfig.parse((prefix+ModernQuestConfigurationTest.settlement(temporary)+lines(p)).getBytes(StandardCharsets.UTF_8),n->"fixture-only")) {
            assertTrue(c.sources().stream().filter(s->s.id()==7).findFirst().orElseThrow().fullVip());
            assertFalse(c.sources().stream().filter(s->s.id()==42).findFirst().orElseThrow().fullVip());first=ModernSettlementCoordinator.binding(c);
        }
        p.setProperty("sources","42,7");p.setProperty("source.42.name","Renamed");
        try(var c=ModernServerConfig.parse((prefix+ModernQuestConfigurationTest.settlement(temporary)+lines(p)).getBytes(StandardCharsets.UTF_8),n->"fixture-only")) {assertArrayEquals(first,ModernSettlementCoordinator.binding(c));}
        p.setProperty("source.42.fullVipRules",ModernWorldConfiguration.FULL_VIP_RULESET);
        try(var c=ModernServerConfig.parse((prefix+ModernQuestConfigurationTest.settlement(temporary)+lines(p)).getBytes(StandardCharsets.UTF_8),n->"fixture-only")) {assertFalse(java.util.Arrays.equals(first,ModernSettlementCoordinator.binding(c)));}
    }
    @Test void armorIsExplicitPerMapAndPreservesOldNamespaceAndCopies()throws Exception {
        var p=combat(temporary);String prefix="world.1.514.";
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        var old=ModernWorldConfiguration.parse(p);assertFalse(old.armorProtectionEnabled());
        var empty=new java.io.ByteArrayOutputStream();old.writeArmorProtectionBinding(new java.io.DataOutputStream(empty));assertEquals(0,empty.size());
        p.setProperty(prefix+"armorProtectionRules",ModernArmorProtectionCatalog.RULESET);
        var enabled=ModernWorldConfiguration.parse(p);assertTrue(enabled.armorProtectionEnabled());assertFalse(enabled.playerHealingEnabled());
        var bytes=new java.io.ByteArrayOutputStream();enabled.writeArmorProtectionBinding(new java.io.DataOutputStream(bytes));assertTrue(bytes.size()>0);
        var folder=Files.createDirectories(temporary.resolve("map/514"));Files.write(folder.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var m=enabled.load().get(new ModernWorldBinding.Key(1,514));assertTrue(m.armorProtectionEnabled());
        for(var copy:List.of(m.withClockOverrides(null),m.withRace(null),m.withDisintegrate(false),m.withGreaterHaste(false),m.withHaste(false),m.withSlow(false),m.withPlayerHealing(false)))assertTrue(copy.armorProtectionEnabled());
        assertFalse(m.withArmorProtection(false).armorProtectionEnabled());
        for(String key:List.of("weightRules","itemRules","combatRules")) {var bad=(Properties)p.clone();bad.remove(prefix+key);rejectsBeforeSecrets(bad);}
        var bad=(Properties)p.clone();bad.setProperty(prefix+"armorProtectionRules","NATIVE_AUTO");rejectsBeforeSecrets(bad);
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String key:p.stringPropertyNames())if(key.startsWith(prefix)&&!key.endsWith("armorProtectionRules"))mixed.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
        var mappings=ModernWorldConfiguration.parse(mixed).load();assertTrue(mappings.get(new ModernWorldBinding.Key(1,514)).armorProtectionEnabled());assertFalse(mappings.get(new ModernWorldBinding.Key(1,509)).armorProtectionEnabled());
        var first=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writeArmorProtectionBinding(new java.io.DataOutputStream(first));assertArrayEquals(bytes.toByteArray(),first.toByteArray());
        mixed.setProperty("world.bindings","1:509,1:514");var second=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writeArmorProtectionBinding(new java.io.DataOutputStream(second));assertArrayEquals(first.toByteArray(),second.toByteArray());
    }
    @Test void playerHealingRequiresExplicitRulesAndPreservesOldNamespaces()throws Exception {
        var p=combat(temporary);String prefix="world.1.514.";
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        var old=ModernWorldConfiguration.parse(p);assertFalse(old.playerHealingEnabled());assertFalse(old.magicQualificationEnabled());
        var empty=new java.io.ByteArrayOutputStream();old.writePlayerHealingBinding(new java.io.DataOutputStream(empty));assertEquals(0,empty.size());
        p.setProperty(prefix+"playerHealingRules",ModernPlayerHealing.RULESET);
        var enabled=ModernWorldConfiguration.parse(p);assertTrue(enabled.playerHealingEnabled());assertTrue(enabled.magicQualificationEnabled());assertFalse(enabled.honorEnabled());
        var bytes=new java.io.ByteArrayOutputStream();enabled.writePlayerHealingBinding(new java.io.DataOutputStream(bytes));assertTrue(bytes.size()>0);
        var folder=Files.createDirectories(temporary.resolve("map/514"));Files.write(folder.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var m=enabled.load().get(new ModernWorldBinding.Key(1,514));assertTrue(m.playerHealingEnabled());
        assertTrue(m.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60000)).playerHealingEnabled());
        for(String key:List.of("weightRules","itemRules","combatRules","spellRules","castMillis")) {
            var bad=(Properties)p.clone();bad.remove(prefix+key);rejectsBeforeSecrets(bad);
        }
        var bad=(Properties)p.clone();bad.setProperty(prefix+"playerHealingRules","NATIVE_AUTO");rejectsBeforeSecrets(bad);
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String key:p.stringPropertyNames())if(key.startsWith(prefix))mixed.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
        var first=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writePlayerHealingBinding(new java.io.DataOutputStream(first));
        mixed.setProperty("world.bindings","1:509,1:514");var second=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writePlayerHealingBinding(new java.io.DataOutputStream(second));
        assertArrayEquals(first.toByteArray(),second.toByteArray());assertFalse(java.util.Arrays.equals(bytes.toByteArray(),first.toByteArray()));
    }
    @Test void greaterHasteRequiresExplicitTimedSpellsAndKeepsDisabledNamespaceBytes()throws Exception {
        var p=combat(temporary);String prefix="world.1.514.";
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(prefix+"buffRules",ModernWorldConfiguration.TIMED_RULESET);
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        var disabled=ModernWorldConfiguration.parse(p);assertFalse(disabled.greaterHasteEnabled());
        var empty=new java.io.ByteArrayOutputStream();disabled.writeGreaterHasteBinding(new java.io.DataOutputStream(empty));assertEquals(0,empty.size());
        p.setProperty(prefix+"greaterHasteRules",ModernGreaterHaste.RULESET);
        var enabled=ModernWorldConfiguration.parse(p);assertTrue(enabled.greaterHasteEnabled());assertTrue(enabled.magicQualificationEnabled());assertFalse(enabled.damageMagicEnabled());
        var bytes=new java.io.ByteArrayOutputStream();enabled.writeGreaterHasteBinding(new java.io.DataOutputStream(bytes));assertTrue(bytes.size()>0);
        var folder=Files.createDirectories(temporary.resolve("map/514"));Files.write(folder.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var m=enabled.load().get(new ModernWorldBinding.Key(1,514));assertTrue(m.greaterHasteEnabled());
        assertTrue(m.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60000)).greaterHasteEnabled());
        assertTrue(m.withDisintegrate(false).greaterHasteEnabled());
        for(String key:List.of("buffRules","weightRules","itemRules","combatRules","spellRules","castMillis")) {
            var bad=(Properties)p.clone();bad.remove(prefix+key);rejectsBeforeSecrets(bad);
        }
        var bad=(Properties)p.clone();bad.setProperty(prefix+"greaterHasteRules","NATIVE_AUTO");rejectsBeforeSecrets(bad);
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String key:p.stringPropertyNames())if(key.startsWith(prefix))mixed.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
        var ordered=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writeGreaterHasteBinding(new java.io.DataOutputStream(ordered));
        mixed.setProperty("world.bindings","1:509,1:514");var reversed=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writeGreaterHasteBinding(new java.io.DataOutputStream(reversed));
        assertArrayEquals(ordered.toByteArray(),reversed.toByteArray());assertFalse(java.util.Arrays.equals(bytes.toByteArray(),ordered.toByteArray()));
    }
    @Test void hasteRequiresExplicitTimedSpellsAndKeepsDisabledNamespaceBytes()throws Exception {
        var p=combat(temporary);String prefix="world.1.514.";
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(prefix+"buffRules",ModernWorldConfiguration.TIMED_RULESET);
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        var disabled=ModernWorldConfiguration.parse(p);assertFalse(disabled.hasteEnabled());
        var empty=new java.io.ByteArrayOutputStream();disabled.writeHasteBinding(new java.io.DataOutputStream(empty));assertEquals(0,empty.size());
        p.setProperty(prefix+"hasteRules",ModernHaste.RULESET);
        var enabled=ModernWorldConfiguration.parse(p);assertTrue(enabled.hasteEnabled());assertTrue(enabled.magicQualificationEnabled());assertFalse(enabled.damageMagicEnabled());
        var bytes=new java.io.ByteArrayOutputStream();enabled.writeHasteBinding(new java.io.DataOutputStream(bytes));assertTrue(bytes.size()>0);
        var folder=Files.createDirectories(temporary.resolve("map/514"));Files.write(folder.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var m=enabled.load().get(new ModernWorldBinding.Key(1,514));assertTrue(m.hasteEnabled());
        assertTrue(m.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60000)).hasteEnabled());
        assertTrue(m.withDisintegrate(false).hasteEnabled());
        for(String key:List.of("buffRules","weightRules","itemRules","combatRules","spellRules","castMillis")) {
            var bad=(Properties)p.clone();bad.remove(prefix+key);rejectsBeforeSecrets(bad);
        }
        var bad=(Properties)p.clone();bad.setProperty(prefix+"hasteRules","NATIVE_AUTO");rejectsBeforeSecrets(bad);
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String key:p.stringPropertyNames())if(key.startsWith(prefix))mixed.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
        var ordered=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writeHasteBinding(new java.io.DataOutputStream(ordered));
        mixed.setProperty("world.bindings","1:509,1:514");var reversed=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(mixed).writeHasteBinding(new java.io.DataOutputStream(reversed));
        assertArrayEquals(ordered.toByteArray(),reversed.toByteArray());assertFalse(java.util.Arrays.equals(bytes.toByteArray(),ordered.toByteArray()));
    }
    @Test void npcHasteRequiresExplicitRulesAndPreservesQualifiedCamps()throws Exception {
        var p=ModernNpcConfigurationTest.world(temporary);String prefix=ModernNpcConfigurationTest.PREFIX;
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(prefix+"buffRules",ModernWorldConfiguration.TIMED_RULESET);
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(p));
        p.setProperty(prefix+"hasteRules",ModernHaste.RULESET);
        var parsed=ModernWorldConfiguration.parse(p);var encounters=ModernNpcConfigurationTest.encounter(parsed);
        var mapping=parsed.load(null,()->false,encounters).get(ModernNpcConfigurationTest.KEY);
        assertTrue(mapping.hasteEnabled());assertTrue(mapping.timedEnabled());assertFalse(mapping.greaterHasteEnabled());
        assertNotNull(mapping.npc());assertEquals(mapping.npc().lanes(),mapping.teamCamps());
        assertTrue(mapping.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60000)).hasteEnabled());
        assertThrows(IllegalArgumentException.class,()->mapping.withHaste(false));
        assertThrows(IllegalArgumentException.class,()->mapping.withGreaterHaste(true));
    }
    @Test void disintegrateIsExplicitRequiresQualifiedCombatAndPreservesOldDisabledMappings()throws Exception {
        var p=combat(temporary);String prefix="world.1.514.";
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        p.setProperty(prefix+"regenRules",ModernNaturalRegen.RULESET);
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        assertFalse(ModernWorldConfiguration.parse(p).disintegrateEnabled());
        p.setProperty(prefix+"disintegrateRules",ModernDisintegrate.RULESET);
        assertTrue(ModernWorldConfiguration.parse(p).disintegrateEnabled());
        var folder=Files.createDirectories(temporary.resolve("map/514"));Files.write(folder.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var m=ModernWorldConfiguration.parse(p).load().get(new ModernWorldBinding.Key(1,514));assertTrue(m.disintegrateEnabled());
        assertTrue(m.withClockOverrides(new ModernWorldService.ClockOverrides(3000,0L,60000)).disintegrateEnabled());
        for(String key:List.of("regenRules","weightRules","spellRules","combatRules","itemRules")){
            var bad=(Properties)p.clone();bad.remove(prefix+key);rejectsBeforeSecrets(bad);
        }
        var bad=(Properties)p.clone();bad.setProperty(prefix+"disintegrateRules","NATIVE_AUTO");rejectsBeforeSecrets(bad);
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String key:p.stringPropertyNames())if(key.startsWith(prefix)&&!key.endsWith("disintegrateRules"))mixed.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
        var maps=ModernWorldConfiguration.parse(mixed).load();assertTrue(maps.get(new ModernWorldBinding.Key(1,514)).disintegrateEnabled());assertFalse(maps.get(new ModernWorldBinding.Key(1,509)).disintegrateEnabled());
        var hp=honor();hp.setProperty("world.5.532.disintegrateRules",ModernDisintegrate.RULESET);
        assertTrue(ModernWorldConfiguration.parse(hp).load().get(new ModernWorldBinding.Key(5,532)).disintegrateEnabled());
    }
    @Test void fireStormIsExplicitSourceRulesAndMayCoexistWithDisabledBindingsWithoutHonorDependency()throws Exception{
        var p=combat(temporary);String prefix="world.1.514.";
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);p.setProperty(prefix+"regenRules",ModernNaturalRegen.RULESET);
        p.setProperty(prefix+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);p.setProperty(prefix+"castMillis","700");
        assertFalse(ModernWorldConfiguration.parse(p).fireStormEnabled());p.setProperty(prefix+"fireStormRules",ModernFireStorm.RULESET);
        var selected=ModernWorldConfiguration.parse(p);assertTrue(selected.fireStormEnabled());assertFalse(selected.honorEnabled());assertEquals(List.of(new ModernWorldBinding.Key(1,514)),selected.fireStormBindings());
        for(String key:List.of("regenRules","weightRules","spellRules","combatRules")){var bad=(Properties)p.clone();bad.remove(prefix+key);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bad));}
        var bad=(Properties)p.clone();bad.setProperty(prefix+"fireStormRules","NATIVE_AUTO");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bad));
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String key:p.stringPropertyNames())if(key.startsWith(prefix)&&!key.endsWith("fireStormRules"))mixed.setProperty(key.replace(prefix,"world.1.509."),p.getProperty(key));
        var parsed=ModernWorldConfiguration.parse(mixed);assertEquals(List.of(new ModernWorldBinding.Key(1,514)),parsed.fireStormBindings());
    }
    private Properties honor()throws Exception {
        var original=combat(temporary);var p=new Properties();original.forEach((k,v)->p.setProperty(k.toString().replace("1.514","5.532"),v.toString().replace("1:514","5:532")));
        String prefix="world.5.532.";p.setProperty(prefix+"physicalMap","532");p.setProperty(prefix+"spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);p.setProperty(prefix+"castMillis","1200");
        p.setProperty(prefix+"itemRules",ModernWorldConfiguration.ITEM_RULESET);p.setProperty(prefix+"weightRules",ModernWorldConfiguration.WEIGHT_RULESET);p.setProperty(prefix+"regenRules",ModernWorldConfiguration.REGEN_RULESET);
        p.setProperty(prefix+"honorRules",ModernHonor.RULESET);p.setProperty(prefix+"tower0","32724,32783,2665,3000");p.setProperty(prefix+"tower1","32730,32783,2669,3000");
        p.setProperty(prefix+"spawn0","32724,32784");p.setProperty(prefix+"spawn1","32730,32784");
        byte[] bytes=ModernWorldMapTest.openSectorBytes();var map=ModernWorldMap.fromSectorBytes(532,java.util.Map.of(new ModernWorldMap.SectorKey(0x7fff,0x8000),bytes),()->false);
        p.setProperty(prefix+"manifestSha256",map.digest());var sector=map.manifest().sectors().get(0);p.setProperty(prefix+"sectorPins","7fff8000:"+sector.byteLength()+":"+sector.rawSha256());
        var folder=Files.createDirectories(temporary.resolve("map/532"));Files.write(folder.resolve("7fff8000.s32"),bytes);return p;
    }
    @Test void honorNeedsExplicitTwoTowersTeamSpawnsAndCompletePinnedGeometry()throws Exception{
        var p=honor();var parsed=ModernWorldConfiguration.parse(p);assertTrue(parsed.honorEnabled());var mapping=parsed.load().get(new ModernWorldBinding.Key(5,532));
        assertEquals(2,mapping.honor().towers().size());assertEquals(new ModernWorldMap.Cell(32724,32784),mapping.honor().teamSpawns().get(0));
        for(String key:List.of("tower0","tower1","spawn0","spawn1","manifestSha256","sectorPins","regenRules","spellRules")){
            var invalid=(Properties)p.clone();invalid.remove("world.5.532."+key);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(invalid),key);
        }
    }
    @Test void honorAndDeathmatchMayCoexistButNoOtherModeCanBorrowHonorProof()throws Exception{
        var p=honor();p.setProperty("world.bindings","5:532,1:514");
        for(String key:new java.util.ArrayList<>(p.stringPropertyNames()))if(key.startsWith("world.5.532.")&&!List.of("honorRules","tower0","tower1","spawn0","spawn1").contains(key.substring("world.5.532.".length())))
            p.setProperty(key.replace("world.5.532.","world.1.514."),p.getProperty(key));
        var parsed=ModernWorldConfiguration.parse(p);var maps=parsed.load();assertNotNull(maps.get(new ModernWorldBinding.Key(5,532)).honor());assertNull(maps.get(new ModernWorldBinding.Key(1,514)).honor());
        var wrong=honor();var converted=new Properties();wrong.forEach((k,v)->converted.setProperty(k.toString().replace("5.532","1.514"),v.toString().replace("5:532","1:514")));
        assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(converted));
    }
    @Test void honorRejectsOverlappingOffMapAndUnqualifiedTowerOrAnchorWithoutGuessingLocations()throws Exception{
        var p=honor();for(String bad:List.of("32724,32783,189,3000","32724,32783,2665,0","32724,32783,2665,-1","32724,32783,2665,9223372036854775808")){
            var invalid=(Properties)p.clone();invalid.setProperty("world.5.532.tower0",bad);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(invalid));}
        for(String key:List.of("tower1","spawn0")){var invalid=(Properties)p.clone();invalid.setProperty("world.5.532."+key,key.equals("tower1")?"32724,32783,2669,3000":"32724,32783");
            if(key.equals("tower1"))assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(invalid));
            else assertThrows(IOException.class,()->ModernWorldConfiguration.parse(invalid).load());}
    }
    @Test void honorOutboxBindingPinsTheObjectivesAndSpawnsWithoutChangingDisabledBytes()throws Exception{
        var p=honor();var a=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(p).writeHonorBinding(new java.io.DataOutputStream(a));assertTrue(a.size()>0);
        var q=(Properties)p.clone();q.setProperty("world.5.532.tower0","32724,32783,2665,3001");var b=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(q).writeHonorBinding(new java.io.DataOutputStream(b));assertFalse(java.util.Arrays.equals(a.toByteArray(),b.toByteArray()));
        var disabled=new java.io.ByteArrayOutputStream();ModernWorldConfiguration.parse(combat(temporary)).writeHonorBinding(new java.io.DataOutputStream(disabled));assertEquals(0,disabled.size());
    }
    @Test void naturalRegenIsExplicitHomogeneousAndRequiresWeightedCombatButNotBowOrTimed()throws Exception {
        var p=combat(temporary);p.setProperty("world.1.514.itemRules",ModernWorldConfiguration.ITEM_RULESET);p.setProperty("world.1.514.weightRules",ModernWorldConfiguration.WEIGHT_RULESET);
        assertFalse(ModernWorldConfiguration.parse(p).regenEnabled());p.setProperty("world.1.514.regenRules",ModernNaturalRegen.RULESET);
        var parsed=ModernWorldConfiguration.parse(p);assertTrue(parsed.regenEnabled());assertFalse(parsed.timedEnabled());assertFalse(parsed.bowEnabled());assertEquals(List.of(new ModernWorldBinding.Key(1,514)),parsed.regenBindings());
        for(String key:List.of("weightRules","combatRules","itemRules")){var invalid=(Properties)p.clone();invalid.remove("world.1.514."+key);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(invalid));}
        var wrong=(Properties)p.clone();wrong.setProperty("world.1.514.regenRules","DEFAULT");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(wrong));
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");for(String key:p.stringPropertyNames())if(key.startsWith("world.1.514.")&&!key.endsWith("regenRules"))mixed.setProperty(key.replace("world.1.514.","world.1.509."),p.getProperty(key));
        assertEquals(List.of(new ModernWorldBinding.Key(1,514)),ModernWorldConfiguration.parse(mixed).regenBindings());
    }
    @Test void arrowsNeedExplicitHomogeneousTimedWeightedItemCombatPolicy()throws Exception {
        var p=combat(temporary);p.setProperty("world.1.514.itemRules",ModernWorldConfiguration.ITEM_RULESET);
        p.setProperty("world.1.514.weightRules",ModernWorldConfiguration.WEIGHT_RULESET);p.setProperty("world.1.514.buffRules",ModernWorldConfiguration.TIMED_RULESET);
        assertFalse(ModernWorldConfiguration.parse(p).bowEnabled());p.setProperty("world.1.514.bowRules",ModernWorldConfiguration.BOW_RULESET);
        assertTrue(ModernWorldConfiguration.parse(p).bowEnabled());assertEquals(List.of(new ModernWorldBinding.Key(1,514)),ModernWorldConfiguration.parse(p).bowBindings());
        for(String suffix:List.of("itemRules","weightRules","buffRules","combatRules")){var invalid=(Properties)p.clone();invalid.remove("world.1.514."+suffix);assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(invalid));}
        var invalid=(Properties)p.clone();invalid.setProperty("world.1.514.bowRules","AUTO");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(invalid));
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String key:p.stringPropertyNames())if(key.startsWith("world.1.514.")&&!key.endsWith("bowRules"))mixed.setProperty(key.replace("world.1.514.","world.1.509."),p.getProperty(key));
        assertEquals(List.of(new ModernWorldBinding.Key(1,514)),ModernWorldConfiguration.parse(mixed).bowBindings());
    }
    @TempDir Path temporary;
    private static String portable(Path path) {return path.toString().replace('\\','/');}
    private static Properties world(Path root) {
        var p=new Properties();p.setProperty("world.sharedRoot",portable(root));p.setProperty("world.bindings","4:540");
        p.setProperty("world.4.540.physicalMap","514");p.setProperty("world.4.540.sectors","7fff8000");
        p.setProperty("world.4.540.spawnX","32724");p.setProperty("world.4.540.spawnY","32784");
        p.setProperty("world.4.540.stepMillis","50");return p;
    }
    private String base() {
        return "schema=1\nbind.port=0\ntls.pkcs12="+portable(temporary.resolve("unused.p12"))
                +"\ntls.password.env=TEST_TLS\nsources=7\nsource.7.name=Source\nsource.7.mysql.host=127.0.0.1\n"
                +"source.7.mysql.port=3306\nsource.7.mysql.database=synthetic\nsource.7.mysql.username.env=TEST_USER\n"
                +"source.7.mysql.password.env=TEST_PASSWORD\nsource.7.mysql.passwordScheme=plain\nsource.7.mysql.tls=disabled\n";
    }
    private static String lines(Properties p) {
        return p.stringPropertyNames().stream().sorted().map(key->key+"="+p.getProperty(key)+"\n").reduce("",String::concat);
    }
    private void rejectsBeforeSecrets(Properties p) {
        AtomicInteger env=new AtomicInteger();
        var error=assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(
                (base()+lines(p)).getBytes(StandardCharsets.UTF_8),name->{env.incrementAndGet();return "synthetic-secret";}));
        assertEquals(0,env.get());assertNull(error.getCause());assertFalse(error.toString().contains("synthetic-secret"));
    }
    @Test void noWorldKeysMeansDisabledAndNoResourceRead() throws Exception {
        var disabled=ModernWorldConfiguration.parse(new Properties());assertFalse(disabled.enabled());assertTrue(disabled.load().isEmpty());
        var p=world(temporary);var parsed=ModernWorldConfiguration.parse(p);assertTrue(parsed.enabled());
        assertFalse(Files.exists(temporary.resolve("map")),"parse cannot create or open shared maps");
        assertThrows(IOException.class,parsed::load);
        assertEquals("ModernWorldConfiguration[redacted]",parsed.toString());
    }
    private static Properties combat(Path root) {
        var original=world(root);var p=new Properties();
        original.forEach((key,value)->p.setProperty(key.toString().replace("4.540","1.514"),value.toString().replace("4:540","1:514")));
        p.setProperty("world.1.514.combatRules",ModernPhysicalCombat.RULESET);
        p.setProperty("world.1.514.attackMillis","1000");return p;
    }
    @Test void namedCombatRulesAndBoundedIntervalRequireAnExplicitPair() throws Exception {
        assertFalse(ModernWorldConfiguration.parse(world(temporary)).combatEnabled());
        assertTrue(ModernWorldConfiguration.parse(combat(temporary)).combatEnabled());
        for(String key:List.of("combatRules","attackMillis")) {
            var p=combat(temporary);p.remove("world.1.514."+key);rejectsBeforeSecrets(p);
        }
        for(String value:List.of("0","10001","01","1.5","+1","-1")) {
            var p=combat(temporary);p.setProperty("world.1.514.attackMillis",value);rejectsBeforeSecrets(p);
        }
        var unknown=combat(temporary);unknown.setProperty("world.1.514.combatRules","classic-auto");rejectsBeforeSecrets(unknown);
        var nonPvp=world(temporary);nonPvp.setProperty("world.4.540.combatRules",ModernPhysicalCombat.RULESET);
        nonPvp.setProperty("world.4.540.attackMillis","1000");rejectsBeforeSecrets(nonPvp);
    }
    @Test void botsRequireExplicitNamedCombatMappingAndNeverEnableByDefault() throws Exception {
        var p=combat(temporary);assertFalse(ModernWorldConfiguration.parse(p).botsEnabled());
        p.setProperty("world.1.514.botRules",ModernWorldConfiguration.BOT_RULESET);
        var config=ModernWorldConfiguration.parse(p);assertTrue(config.botsEnabled());
        assertEquals(List.of(new ModernWorldBinding.Key(1,514)),config.botBindings());
        Path map=Files.createDirectories(temporary.resolve("map/514"));Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        assertTrue(config.load().get(new ModernWorldBinding.Key(1,514)).serverBots());
        for(String value:List.of("", "native", "ROOM_TEST_DUMMY_V2")) {
            var wrong=(Properties)p.clone();wrong.setProperty("world.1.514.botRules",value);rejectsBeforeSecrets(wrong);
        }
        p.remove("world.1.514.combatRules");p.remove("world.1.514.attackMillis");rejectsBeforeSecrets(p);
    }
    @Test void teleportRequiresNamedOptInBoundedPaceAndCombatWithoutChangingOldMappings() throws Exception {
        var plain=combat(temporary);assertFalse(ModernWorldConfiguration.parse(plain).spellsEnabled());
        var p=combat(temporary);p.setProperty("world.1.514.spellRules",ModernWorldConfiguration.SPELL_RULESET);
        p.setProperty("world.1.514.castMillis","700");
        assertTrue(ModernWorldConfiguration.parse(p).spellsEnabled());
        Path map=Files.createDirectories(temporary.resolve("map/514"));
        Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var mapping=ModernWorldConfiguration.parse(p).load().get(new ModernWorldBinding.Key(1,514));
        assertTrue(mapping.spellsEnabled());assertEquals(700_000_000,mapping.castIntervalNanos());
        for(String value:List.of("0","9","10001","0700","+700","-1")) {
            var invalid=(Properties)p.clone();invalid.setProperty("world.1.514.castMillis",value);rejectsBeforeSecrets(invalid);
        }
        for(String suffix:List.of("spellRules","castMillis","combatRules","attackMillis")) {
            var invalid=(Properties)p.clone();invalid.remove("world.1.514."+suffix);rejectsBeforeSecrets(invalid);
        }
        var invalid=(Properties)p.clone();invalid.setProperty("world.1.514.spellRules","native-bin-auto");rejectsBeforeSecrets(invalid);
    }
    @Test void matchItemsRequireNamedCombatAndCannotSilentlyDisappearInAnotherOfferedWorld() throws Exception {
        var p=combat(temporary);assertFalse(ModernWorldConfiguration.parse(p).itemsEnabled());
        p.setProperty("world.1.514.itemRules",ModernWorldConfiguration.ITEM_RULESET);
        var parsed=ModernWorldConfiguration.parse(p);assertTrue(parsed.itemsEnabled());
        assertEquals(List.of(new ModernWorldBinding.Key(1,514)),parsed.itemBindings());
        Path map=Files.createDirectories(temporary.resolve("map/514"));
        Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var mapping=parsed.load().get(new ModernWorldBinding.Key(1,514));assertTrue(mapping.matchItems());
        assertThrows(IllegalArgumentException.class,()->new ModernWorldService.Mapping(mapping.binding(),mapping.anchor(),50_000_000,0,0,false,false,true));
        for(String value:List.of("","NEW_MATCH_HEALING_ITEMS_V2","native-auto","ROOM_TEST_DUMMY_V1")) {
            var invalid=(Properties)p.clone();invalid.setProperty("world.1.514.itemRules",value);rejectsBeforeSecrets(invalid);
        }
        var missing=(Properties)p.clone();missing.remove("world.1.514.combatRules");missing.remove("world.1.514.attackMillis");rejectsBeforeSecrets(missing);
        var mixed=(Properties)p.clone();mixed.setProperty("world.bindings","1:514,1:509");
        for(String suffix:List.of("physicalMap","sectors","spawnX","spawnY","stepMillis","combatRules","attackMillis"))
            mixed.setProperty("world.1.509."+suffix,mixed.getProperty("world.1.514."+suffix));
        assertEquals(List.of(new ModernWorldBinding.Key(1,514)),ModernWorldConfiguration.parse(mixed).itemBindings());
    }
    @Test void combatRequiresAllSourceCatalogsAndStoreBeforeAnySecretRead() {
        rejectsBeforeSecrets(combat(temporary));
        var p=combat(temporary);p.setProperty("equipment.storeDirectory",portable(temporary));
        rejectsBeforeSecrets(p);
        p.setProperty("source.7.catalog.database","catalog");
        assertDoesNotThrow(()->{
            try(var config=ModernServerConfig.parse((base()+lines(p)).getBytes(StandardCharsets.UTF_8),name->"fixture-only")) {
                assertTrue(config.worlds().combatEnabled());assertEquals(temporary,config.equipmentStoreDirectory());
            }
        });
    }
    @Test void damageMagicHasDistinctExplicitOptInAndRetainsLegacyTeleportMappings() throws Exception {
        var p=combat(temporary);p.setProperty("world.1.514.spellRules",ModernWorldConfiguration.SPELL_RULESET);
        p.setProperty("world.1.514.castMillis","700");
        assertFalse(ModernWorldConfiguration.parse(p).damageMagicEnabled());
        assertTrue(ModernWorldConfiguration.parse(p).damageMagicBindings().isEmpty());
        p.setProperty("world.1.514.spellRules",ModernWorldConfiguration.DAMAGE_SPELL_RULESET);
        var parsed=ModernWorldConfiguration.parse(p);
        assertTrue(parsed.damageMagicEnabled());assertTrue(parsed.spellsEnabled());assertTrue(parsed.combatEnabled());
        assertEquals(List.of(new ModernWorldBinding.Key(1,514)),parsed.damageMagicBindings());
        Path map=Files.createDirectories(temporary.resolve("map/514"));
        Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var mapping=parsed.load().get(new ModernWorldBinding.Key(1,514));
        assertTrue(mapping.damageMagic());assertTrue(mapping.spellsEnabled());assertEquals(700_000_000L,mapping.castIntervalNanos());
        assertThrows(IllegalArgumentException.class,()->new ModernWorldService.Mapping(mapping.binding(),mapping.anchor(),50_000_000,1_000_000_000,0,true));
        for(String suffix:List.of("castMillis","attackMillis","combatRules")) {
            var invalid=(Properties)p.clone();invalid.remove("world.1.514."+suffix);rejectsBeforeSecrets(invalid);
        }
    }
    @Test void mixedCombatAvailabilityIsExplicitAndAdmissionRequiresMatchingClientPolicy() throws Exception {
        Path map=Files.createDirectories(temporary.resolve("map/514"));Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var p=combat(temporary);p.setProperty("world.bindings","1:514,4:540");
        for(String suffix:List.of("physicalMap","sectors","spawnX","spawnY","stepMillis"))p.setProperty("world.4.540."+suffix,p.getProperty("world.1.514."+suffix));
        var mappings=ModernWorldConfiguration.parse(p).load();assertFalse(mappings.get(new ModernWorldBinding.Key(4,540)).combatEnabled());var battle=mappings.get(new ModernWorldBinding.Key(1,514));
        assertTrue(battle.combatEnabled());assertEquals(1_000_000_000L,battle.attackIntervalNanos());
    }
    @Test void incompleteMisspelledAndUnusedValidBindingKeysFailBeforeSecrets() {
        var complete=world(temporary);
        for(String remove:complete.stringPropertyNames()) {
            var p=world(temporary);p.remove(remove);rejectsBeforeSecrets(p);
        }
        for(String extra:List.of("world.4.540.spawnx","world.4.540.secret","world.1.514.spawnX","world.enabled")) {
            var p=world(temporary);p.setProperty(extra,"1");rejectsBeforeSecrets(p);
        }
        var only=new Properties();only.setProperty("world.sharedRoot",portable(temporary));rejectsBeforeSecrets(only);
    }
    @Test void duplicateBindingsPropertiesAndInvalidNativePairsFailBeforeSecrets() {
        for(String value:List.of("4:540,4:540","4:540,","04:540","4:0540","4:514","6:540","4:540:1","4:540, 1:514")) {
            var p=world(temporary);p.setProperty("world.bindings",value);rejectsBeforeSecrets(p);
        }
        AtomicInteger env=new AtomicInteger();
        var failure=assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(
                (base()+lines(world(temporary))+"world.bindings=4:540\n").getBytes(StandardCharsets.UTF_8),name->{env.incrementAndGet();return "never";}));
        assertEquals(ModernServerConfig.Failure.DUPLICATE_PROPERTY,failure.failure());assertEquals(0,env.get());
    }
    @Test void sectorsAreLowercaseExactUniqueAndBounded() {
        for(String value:List.of("","7FFF8000","7fff8000.s32","7fff8000,7fff8000","7fff8000,","../map","7fff800","7fff80000")) {
            var p=world(temporary);p.setProperty("world.4.540.sectors",value);rejectsBeforeSecrets(p);
        }
        for(int count:List.of(16,17,64)) {
            var accepted=world(temporary);accepted.setProperty("world.4.540.sectors",java.util.stream.IntStream.range(0,count)
                    .mapToObj(i->String.format(java.util.Locale.ROOT,"%04x8000",i)).collect(java.util.stream.Collectors.joining(",")));
            assertDoesNotThrow(()->ModernWorldConfiguration.parse(accepted)); // Peer capability is checked before room admission.
        }
        var p=world(temporary);p.setProperty("world.4.540.sectors",java.util.stream.IntStream.range(0,65)
                .mapToObj(i->String.format(java.util.Locale.ROOT,"%04x8000",i)).collect(java.util.stream.Collectors.joining(",")));
        rejectsBeforeSecrets(p);
    }
    @Test void signedSpawnAndStepBoundsDoNotTruncateOrAcceptFormattingAliases() {
        for(String value:List.of("2147483648","-2147483649","1.5","1e2","+1","-0","01","1 ")) {
            var p=world(temporary);p.setProperty("world.4.540.spawnX",value);rejectsBeforeSecrets(p);
        }
        for(String value:List.of("0","10001","-1","1.5","01","+1")) {
            var p=world(temporary);p.setProperty("world.4.540.stepMillis",value);rejectsBeforeSecrets(p);
        }
        for(String value:List.of("1","10000")) {
            var p=world(temporary);p.setProperty("world.4.540.stepMillis",value);
            p.setProperty("world.4.540.spawnX",Integer.toString(Integer.MIN_VALUE));
            p.setProperty("world.4.540.spawnY",Integer.toString(Integer.MAX_VALUE));
            assertDoesNotThrow(()->ModernWorldConfiguration.parse(p)); // Geometry, not integer parsing, rejects this anchor at load.
        }
    }
    @Test void rootIsAbsoluteLocalAndNoPathNormalizationEscapeIsAccepted() {
        for(String value:List.of("relative",portable(temporary.resolve("../escape")),portable(temporary.resolve("x:stream")))) {
            var p=world(temporary);p.setProperty("world.sharedRoot",value);rejectsBeforeSecrets(p);
        }
    }
    @Test void loaderUsesExplicitPhysicalMapAndAdvertisesOnlyItsConfiguredLogicalPair() throws Exception {
        Path map=Files.createDirectories(temporary.resolve("map/514"));
        Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var mappings=ModernWorldConfiguration.parse(world(temporary)).load();
        var key=new ModernWorldBinding.Key(4,540);assertEquals(Set.of(key),mappings.keySet());
        var mapping=mappings.get(key);assertEquals(514,mapping.binding().map().mapId());assertEquals(50_000_000,mapping.stepIntervalNanos());
        assertEquals(new ModernWorldMap.Cell(32724,32784),mapping.anchor());
        assertFalse(Files.exists(temporary.resolve("map/540")),"logical ID cannot imply a same-number resource lookup");
        assertThrows(UnsupportedOperationException.class,mappings::clear);
        var options=options(new ModernRooms(mappings.keySet()));assertEquals(40,options.size());
        assertEquals(List.of(key),options.stream().filter(ModernRooms.Option::worldAvailable)
                .map(option->new ModernWorldBinding.Key(option.mode(),option.mapId())).toList());
        assertTrue(options(new ModernRooms()).stream().noneMatch(ModernRooms.Option::worldAvailable));
    }
    private static List<ModernRooms.Option> options(ModernRooms rooms) {
        var presence=new TournamentPresence();var owner=presence.connect();
        var selected=new TournamentPresence.Selection(new TournamentPresence.CharacterKey(7,3333),"Source","Player",80,1,0);
        var scope=new ModernRooms.Scope(7,3333,1,0);
        try {
            assertEquals(TournamentPresence.SelectStatus.ACCEPTED,presence.select(owner,selected));
            rooms.enterLobby(owner,selected,1,0);
            var result=rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,""));
            assertEquals(ModernRooms.Status.OK,result.status());
            return result.view().options();
        } finally {rooms.disconnect(owner);presence.disconnect(owner);}
    }
    @Test void twoBindingsMayShareOneImmutableGeometryAndMissingSectorStillFailsClosed() throws Exception {
        Path map=Files.createDirectories(temporary.resolve("map/514"));Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var p=world(temporary);p.setProperty("world.bindings","4:540,1:514");
        for(String suffix:List.of("physicalMap","sectors","spawnX","spawnY","stepMillis"))p.setProperty("world.1.514."+suffix,p.getProperty("world.4.540."+suffix));
        var mappings=ModernWorldConfiguration.parse(p).load();
        assertSame(mappings.get(new ModernWorldBinding.Key(4,540)).binding().map(),mappings.get(new ModernWorldBinding.Key(1,514)).binding().map());
        p.setProperty("world.1.514.sectors","7fff8000,7fff8001");
        assertThrows(IOException.class,()->ModernWorldConfiguration.parse(p).load());
    }
    @Test void loadRejectsAnUnwalkableOrOutsideSpawnRatherThanChoosingAnother() throws Exception {
        Path map=Files.createDirectories(temporary.resolve("map/514"));byte[] bytes=ModernWorldMapTest.openSectorBytes();
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)ModernWorldMapTest.setAttributes(bytes,x,y,(short)1,(short)1);
        Files.write(map.resolve("7fff8000.s32"),bytes);
        assertThrows(IOException.class,()->ModernWorldConfiguration.parse(world(temporary)).load());
        Files.write(map.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        var p=world(temporary);p.setProperty("world.4.540.spawnX",Integer.toString(Integer.MAX_VALUE));
        assertThrows(IOException.class,()->ModernWorldConfiguration.parse(p).load());
    }
    @Test void explicitlyOptedInRealShared514BindingMatchesIndependentManifest() throws Exception {
        String resourceRoot=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        assumeTrue(resourceRoot!=null && !resourceRoot.isBlank(),"explicit shared-resource root required");
        var mappings=ModernWorldConfiguration.parse(world(Path.of(resourceRoot))).load();
        var map=mappings.get(new ModernWorldBinding.Key(4,540)).binding().map();
        assertEquals("2feecc83de156a03c62e44effb7106067b24098a9ef7250cc471f7bf826da7a8",map.digest());
        assertEquals(137784,map.manifest().sectors().get(0).byteLength());
        assertEquals("ccfe52b614dfbdf97fd713f80d61e4cdd35e7c8c3000a06b4bba8868c96d1271",map.manifest().sectors().get(0).rawSha256());
        assertTrue(map.standing(new ModernWorldMap.Cell(32724,32784)));
        assertEquals(1,options(new ModernRooms(mappings.keySet())).stream().filter(ModernRooms.Option::worldAvailable).count());
    }
    private static String pinRows(ModernWorldMap.Manifest manifest) {
        return manifest.sectors().stream().map(s->s.key().fileName().replace(".s32","")+":"+s.byteLength()+":"+s.rawSha256())
                .collect(java.util.stream.Collectors.joining(","));
    }
    private Properties pinnedWorld() throws Exception {
        var p=world(temporary);var map=ModernWorldMap.fromSectorBytes(514,
                java.util.Map.of(new ModernWorldMap.SectorKey(0x7fff,0x8000),ModernWorldMapTest.openSectorBytes()),()->false);
        p.setProperty("world.4.540.manifestSha256",map.digest());p.setProperty("world.4.540.sectorPins",pinRows(map.manifest()));return p;
    }
    @Test void completePinsAreOptionalButAnyProvidedEvidenceMustBePairedStrictAndCoverEverySectorBeforeSecrets()throws Exception {
        var valid=pinnedWorld();assertTrue(ModernWorldConfiguration.parse(valid).enabled());
        assertFalse(Files.exists(temporary.resolve("map")));
        for(String key:List.of("manifestSha256","sectorPins")){var p=(Properties)valid.clone();p.remove("world.4.540."+key);rejectsBeforeSecrets(p);}
        String correct=valid.getProperty("world.4.540.sectorPins");
        for(String pin:List.of("",correct+",",correct+","+correct,correct.replace("7fff8000","7fff8001"),
                correct.replace(":49170:",":049170:"),correct.replace(":49170:",":49157:"),
                correct.replace(":49170:",":8388609:"),correct.toUpperCase(java.util.Locale.ROOT),
                correct+":extra",correct.replace("7fff8000","../7fff8000"),"x".repeat(1313))) {
            var p=(Properties)valid.clone();p.setProperty("world.4.540.sectorPins",pin);rejectsBeforeSecrets(p);
        }
        for(String hash:List.of("", "0".repeat(64), "A".repeat(64), "a".repeat(63), "a".repeat(65), " a".repeat(64))) {
            var p=(Properties)valid.clone();p.setProperty("world.4.540.manifestSha256",hash);rejectsBeforeSecrets(p);
        }
        var missing=(Properties)valid.clone();missing.setProperty("world.4.540.sectors","7fff8000,7fff8001");rejectsBeforeSecrets(missing);
    }
    @Test void exactPinsLoadOneHundredSpawnsAndChangedOrMissingResourcesNeverFallBack()throws Exception {
        var p=pinnedWorld();var parsed=ModernWorldConfiguration.parse(p);assertThrows(IOException.class,parsed::load);
        Path directory=Files.createDirectories(temporary.resolve("map/514"));Path file=directory.resolve("7fff8000.s32");
        Files.write(file,ModernWorldMapTest.openSectorBytes());var mappings=parsed.load();
        var map=mappings.get(new ModernWorldBinding.Key(4,540)).binding().map();assertEquals(100,map.serverChosenSpawns(new ModernWorldMap.Cell(32724,32784),100).size());
        byte[] changed=ModernWorldMapTest.openSectorBytes();ModernWorldMapTest.setAttributes(changed,0,0,(short)1,(short)0);Files.write(file,changed);
        assertThrows(IOException.class,parsed::load);
        p.remove("world.4.540.manifestSha256");p.remove("world.4.540.sectorPins");
        assertTrue(ModernWorldConfiguration.parse(p).load().containsKey(new ModernWorldBinding.Key(4,540)),"unpinned legacy load remains explicit but makes no evidence claim");
    }
    @Test void sharedGeometryCacheCannotBypassAnotherLogicalBindingsDifferentRawOrCanonicalPin()throws Exception {
        var p=pinnedWorld();Path directory=Files.createDirectories(temporary.resolve("map/514"));Files.write(directory.resolve("7fff8000.s32"),ModernWorldMapTest.openSectorBytes());
        p.setProperty("world.bindings","4:540,1:509");
        for(String suffix:List.of("physicalMap","sectors","spawnX","spawnY","stepMillis","manifestSha256","sectorPins"))p.setProperty("world.1.509."+suffix,p.getProperty("world.4.540."+suffix));
        var mappings=ModernWorldConfiguration.parse(p).load();assertSame(mappings.get(new ModernWorldBinding.Key(4,540)).binding().map(),mappings.get(new ModernWorldBinding.Key(1,509)).binding().map());
        for(String suffix:List.of("manifestSha256","sectorPins")) {
            var forged=(Properties)p.clone();forged.setProperty("world.1.509."+suffix,suffix.equals("manifestSha256")?"a".repeat(64):p.getProperty("world.1.509.sectorPins").substring(0,15)+"b".repeat(64));
            assertThrows(IOException.class,()->ModernWorldConfiguration.parse(forged).load());
        }
    }
    @Test void explicitlyOptedInPinned509AndSelected514GeometryBothQualifyOneHundredConnectedSpawns()throws Exception {
        String resourceRoot=System.getenv("LITO_SHARED_RESOURCE_ROOT");
        assumeTrue(resourceRoot!=null&&!resourceRoot.isBlank(),"explicit shared-resource root required");
        var p=combat(Path.of(resourceRoot));p.setProperty("world.bindings","1:514,1:509");
        p.setProperty("world.1.514.manifestSha256","2feecc83de156a03c62e44effb7106067b24098a9ef7250cc471f7bf826da7a8");
        p.setProperty("world.1.514.sectorPins","7fff8000:137784:ccfe52b614dfbdf97fd713f80d61e4cdd35e7c8c3000a06b4bba8868c96d1271");
        for(String key:List.copyOf(p.stringPropertyNames()))if(key.startsWith("world.1.514."))p.setProperty(key.replace("world.1.514.","world.1.509."),p.getProperty(key));
        p.setProperty("world.1.509.physicalMap","509");p.setProperty("world.1.509.spawnX","32740");p.setProperty("world.1.509.spawnY","32796");
        p.setProperty("world.1.509.manifestSha256","1178261c2d6a968251f2eadfb2029bd0febade95b7151c15872f355784df87f8");
        p.setProperty("world.1.509.sectorPins","7fff8000:106355:8580337a4ad3593d697a416062c4da063f8fd6c2ba311f0b694fcfc59710cb9d");
        var mappings=ModernWorldConfiguration.parse(p).load();assertEquals(Set.of(new ModernWorldBinding.Key(1,509),new ModernWorldBinding.Key(1,514)),mappings.keySet());
        for(var mapping:mappings.values()) {
            var map=mapping.binding().map();assertEquals(mapping.binding().logicalMapId(),map.mapId());
            assertEquals(1,map.manifest().sectors().size());var spawns=map.serverChosenSpawns(mapping.anchor(),100);assertEquals(100,spawns.size());map.validateSpawns(spawns);
        }
        var map509=mappings.get(new ModernWorldBinding.Key(1,509)).binding().map();assertEquals(324,map509.teleportCells().size());
        assertEquals(255,map509.directionMask(new ModernWorldMap.Cell(32740,32796)));
        assertTrue(map509.canStep(new ModernWorldMap.Cell(32740,32796),new ModernWorldMap.Cell(32740,32795)));
        // This pins all of shared 509's single known sector, but only a selected 1/20 sectors of shared 514.
        // Geometry qualification does not claim rendered opacity/effects or recover a native timeout tie-break.
    }
}
