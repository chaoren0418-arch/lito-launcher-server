package local.lito381.server;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModernSharedSiegePresetTest {
    @TempDir Path temporary;
    static Properties worlds(Path shared) {
        var p=new Properties();p.setProperty("world.sharedRoot",shared.toAbsolutePath().toString().replace('\\','/'));p.setProperty("world.bindings","3:547");
        p.setProperty(ModernSharedSiegePreset.KEY,ModernSharedSiegePreset.NAME);
        p.setProperty(ModernSharedSiegePreset.PREFIX+"stepMillis","50");p.setProperty(ModernSharedSiegePreset.PREFIX+"attackMillis","1");
        p.setProperty(ModernSharedSiegePreset.PREFIX+"combatRules",ModernPhysicalCombat.RULESET);
        p.setProperty(ModernSharedSiegePreset.PREFIX+"botRules",ModernWorldConfiguration.BOT_RULESET);
        p.setProperty(ModernSharedSiegePreset.PREFIX+"loadTimeoutMillis","30000");p.setProperty(ModernSharedSiegePreset.PREFIX+"countdownNanos","0");return p;
    }
    @Test void optInExpandsAnImmutablePinnedPolicyWithoutTouchingInputOrOldConfigurations()throws Exception {
        var empty=new Properties();assertSame(empty,ModernSharedSiegePreset.expand(empty));
        var p=worlds(temporary);var original=(Properties)p.clone();var expanded=ModernSharedSiegePreset.expand(p);
        assertEquals(original,p);assertFalse(expanded.containsKey(ModernSharedSiegePreset.KEY));assertNotSame(p,expanded);
        assertEquals("0ca6431b02c8e50eacb0edd369c1c547e9cd15703e56b68ebc458bb569a18917",expanded.getProperty(ModernSharedSiegePreset.PREFIX+"siegePolicySha256"));
        var config=ModernWorldConfiguration.parse(p);assertTrue(config.siegeEnabled());assertTrue(config.completelyPinned());
        var c=ModernSharedSiegePreset.policy();assertEquals(50,c.attackCamp().size());assertEquals(50,c.defenseCamp().size());
        var all=new HashSet<>(c.attackCamp());all.addAll(c.defenseCamp());assertEquals(100,all.size());assertEquals(8,c.gate().size());
        assertEquals(1_200_000,c.durationMillis());assertEquals(5_000,c.respawnMillis());
    }
    @Test void ambiguousOverridesUnknownPresetAndUnusedBindingFailBeforeSecrets()throws Exception {
        var p=worlds(temporary);
        for(String field:ModernSharedSiegePreset.fields().keySet()) {var q=(Properties)p.clone();q.setProperty(ModernSharedSiegePreset.PREFIX+field,"unexpected");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(q));}
        var bad=(Properties)p.clone();bad.setProperty(ModernSharedSiegePreset.KEY,"KENT_LATEST");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(bad));
        var unused=(Properties)p.clone();unused.setProperty("world.bindings","3:573");assertThrows(ModernServerConfig.ConfigException.class,()->ModernWorldConfiguration.parse(unused));
        String text=ModernSiegeConfigurationTest.text(temporary,bad);
        assertThrows(ModernServerConfig.ConfigException.class,()->ModernServerConfig.parse(text.getBytes(StandardCharsets.UTF_8),n->{fail("no secret lookup on invalid preset");return "unused";}));
    }
    @Test void actualSharedKentHasBothGateCutsetsConnectedCampsAndExactRawPins()throws Exception {
        String root=System.getenv("LITO_SHARED_RESOURCE_ROOT");Assumptions.assumeTrue(root!=null);
        var mapping=ModernWorldConfiguration.parse(worlds(Path.of(root))).load().get(new ModernWorldBinding.Key(3,547));
        var binding=mapping.binding();var c=mapping.siege();var map=binding.map();
        assertEquals(4,map.manifest().mapId());assertEquals(9,map.manifest().sectors().size());assertEquals(ModernSharedSiegePreset.MANIFEST,map.digest());
        var space=c.qualify(binding,()->false);assertEquals(29725,space.outside().size());assertEquals(4018,space.inside().size());assertEquals(33751,space.opened().size());
        assertSame(space,space.require(binding,c));
        assertThrows(UnsupportedOperationException.class,()->space.outside().clear());
        assertTrue(java.util.Arrays.stream(ModernSiege.Space.class.getDeclaredConstructors()).allMatch(x->java.lang.reflect.Modifier.isPrivate(x.getModifiers())));
        assertThrows(IllegalArgumentException.class,()->space.require(new ModernWorldBinding(3,573,map),c));
        var changed=new ModernSiege.Config(c.key(),c.towers(),c.gate(),c.attackCamp(),c.defenseCamp(),c.portal(),c.durationMillis()-1,c.respawnMillis());
        assertThrows(IllegalArgumentException.class,()->space.require(binding,changed));
        var differentBytes=ModernWorldMap.fromSectorBytes(4,java.util.Map.of(ModernBattleEngineBotTest.SECTOR,ModernWorldMapTest.openSectorBytes()),()->false);
        assertThrows(IllegalArgumentException.class,()->space.require(new ModernWorldBinding(3,547,differentBytes),c));
        assertTrue(c.attackCamp().stream().allMatch(space.outside()::contains));assertTrue(c.defenseCamp().stream().allMatch(space.inside()::contains));
        assertFalse(space.teleport(c.portal().outside(),c.portal().inside(),false));assertTrue(space.teleport(c.portal().outside(),c.portal().inside(),true));
        var westOnly=new ModernSiege.Config(c.key(),c.towers(),c.gate().stream().filter(p->p.x()==33111).collect(java.util.stream.Collectors.toSet()),c.attackCamp(),c.defenseCamp(),c.portal(),c.durationMillis(),c.respawnMillis());
        assertThrows(IllegalArgumentException.class,()->westOnly.qualify(binding,()->false),"south breach must not be hidden by a decorative west gate");
        assertThrows(IllegalStateException.class,()->c.qualify(binding,()->true));
    }
}
