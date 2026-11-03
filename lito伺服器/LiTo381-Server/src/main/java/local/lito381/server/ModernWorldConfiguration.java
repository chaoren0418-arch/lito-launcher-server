package local.lito381.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Explicit shared-resource bindings. Absence disables world entry; no legacy map fallback. */
final class ModernWorldConfiguration {
    private static final List<String> SUFFIXES = List.of("physicalMap", "sectors", "spawnX", "spawnY", "stepMillis");
    private static final List<String> COMBAT_SUFFIXES = List.of("combatRules", "attackMillis");
    private static final List<String> SPELL_SUFFIXES = List.of("spellRules", "castMillis");
    private static final List<String> CLOCK_SUFFIXES = List.of("loadTimeoutMillis", "countdownNanos", "activeTimeoutMillis");
    private static final List<String> RACE_SUFFIXES = List.of("raceRules", "raceGates", "raceGraphicsId", "raceCourseSha256");
    static final String SPELL_RULESET = "NEW_MATCH_TELEPORT_V1";
    static final String DAMAGE_SPELL_RULESET = "NEW_MATCH_LIGHT_ARROW_V1";
    static final String BOT_RULESET = "ROOM_TEST_DUMMY_V1";
    static final String ITEM_RULESET = "NEW_MATCH_HEALING_ITEMS_V1";
    static final String WEIGHT_RULESET = "TOURNAMENT_WEIGHT_V1";
    static final String TIMED_RULESET = "NEW_MATCH_TIMED_ITEMS_V1";
    static final String BOW_RULESET="NEW_MATCH_BOW_V1";
    static final String REGEN_RULESET=ModernNaturalRegen.RULESET;
    static final String FULL_VIP_RULESET="FULL_VIP_V1";
    private record Geometry(int mapId, Set<ModernWorldMap.SectorKey> sectors) {
        Geometry { sectors = Set.copyOf(sectors); }
    }
    private record RaceInput(List<ModernPetRace.Gate> gates,long graphicsId,String courseSha256) { RaceInput { gates=List.copyOf(gates); } }
    private record Input(ModernWorldBinding.Key key, Geometry geometry, ModernWorldMap.Cell anchor, int stepMillis, int attackMillis, int castMillis, boolean damageMagic, boolean serverBots, boolean matchItems, boolean weight,boolean timed,boolean bow,boolean regen, ModernWorldMapQualification.Expected expected,ModernHonor.Config honor,boolean fireStorm,ModernWorldService.ClockOverrides clocks,RaceInput race,ModernNpcConfiguration.Spec npc,ModernSiege.Config siege,boolean disintegrate,int roomCapacity,boolean capacityQualified,boolean greaterHaste,boolean haste,boolean slow,boolean playerHealing,boolean armorProtection,boolean fullVip) {}
    private final Path sharedRoot;
    private final List<Input> inputs;
    private ModernWorldConfiguration(Path sharedRoot, List<Input> inputs) {
        this.sharedRoot = sharedRoot; this.inputs = List.copyOf(inputs);
    }
    boolean enabled() { return !inputs.isEmpty(); }
    /** Configurations without this new opt-in retain their exact outbox routing identity. */
    void writeRoomCapacityBinding(java.io.DataOutputStream out)throws IOException {
        var bounded=inputs.stream().filter(Input::capacityQualified)
                .sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        if(bounded.isEmpty())return;
        out.writeUTF("MODERN_PINNED_ROOM_CAPACITY_V1");out.writeInt(bounded.size());
        for(var i:bounded) {
            out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());out.writeInt(i.roomCapacity());
            out.writeInt(i.geometry().mapId());out.writeUTF(i.expected().manifest().digest());
            out.writeInt(i.anchor().x());out.writeInt(i.anchor().y());
        }
    }
    boolean completelyPinned() { return enabled() && inputs.stream().allMatch(input -> input.expected()!=null); }
    boolean combatEnabled() { return inputs.stream().anyMatch(input -> input.attackMillis() != 0); }
    boolean spellsEnabled() { return inputs.stream().anyMatch(input -> input.castMillis() != 0); }
    boolean damageMagicEnabled() { return inputs.stream().anyMatch(Input::damageMagic); }
    boolean npcMagicEnabled() { return inputs.stream().anyMatch(i->i.npc()!=null&&i.npc().lightArrow()); }
    boolean magicQualificationEnabled() { return damageMagicEnabled()||npcMagicEnabled()||npcDisintegrateEnabled()||npcFireStormEnabled()||greaterHasteEnabled()||hasteEnabled()||slowEnabled()||playerHealingEnabled(); }
    boolean botsEnabled() { return inputs.stream().anyMatch(Input::serverBots); }
    boolean itemsEnabled() { return inputs.stream().anyMatch(Input::matchItems); }
    boolean weightEnabled() { return inputs.stream().anyMatch(Input::weight); }
    boolean timedEnabled() { return inputs.stream().anyMatch(Input::timed); }
    boolean bowEnabled(){return inputs.stream().anyMatch(Input::bow);}
    boolean regenEnabled(){return inputs.stream().anyMatch(Input::regen);}
    boolean honorEnabled(){return inputs.stream().anyMatch(i->i.honor()!=null);}
    boolean fireStormEnabled(){return inputs.stream().anyMatch(Input::fireStorm);}
    boolean disintegrateEnabled(){return inputs.stream().anyMatch(Input::disintegrate);}
    boolean greaterHasteEnabled(){return inputs.stream().anyMatch(Input::greaterHaste);}
    boolean hasteEnabled(){return inputs.stream().anyMatch(Input::haste);}
    boolean slowEnabled(){return inputs.stream().anyMatch(Input::slow);}
    boolean playerHealingEnabled(){return inputs.stream().anyMatch(Input::playerHealing);}
    boolean armorProtectionEnabled(){return inputs.stream().anyMatch(Input::armorProtection);}
    boolean fullVipEnabled(){return inputs.stream().anyMatch(Input::fullVip);}
    /** No bytes for old configurations; new rules must not consume old outbox work. */
    void writeFullVipBinding(java.io.DataOutputStream out)throws IOException {
        var selected=inputs.stream().filter(Input::fullVip).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        if(selected.isEmpty())return;
        out.writeUTF(FULL_VIP_RULESET);out.writeInt(selected.size());
        for(var i:selected){out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());}
    }
    /** Explicit armor worlds have a distinct durable settlement namespace;
     * configurations without this feature retain their exact old bytes. */
    void writeArmorProtectionBinding(java.io.DataOutputStream out)throws IOException {
        var selected=inputs.stream().filter(Input::armorProtection).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        if(selected.isEmpty())return;out.writeUTF(ModernArmorProtectionCatalog.RULESET);out.writeInt(selected.size());
        for(var i:selected){out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());}
    }
    /** No bytes for old configurations; explicit healing rules cannot inherit old pending results. */
    void writePlayerHealingBinding(java.io.DataOutputStream out)throws IOException {
        var selected=inputs.stream().filter(Input::playerHealing).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        if(selected.isEmpty())return;out.writeUTF(ModernPlayerHealing.RULESET);out.writeInt(selected.size());
        for(var i:selected){out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());}
    }
    /** Old configuration namespaces receive no new bytes. */
    void writeGreaterHasteBinding(java.io.DataOutputStream out)throws IOException {
        var selected=inputs.stream().filter(Input::greaterHaste).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        if(selected.isEmpty())return;out.writeUTF(ModernGreaterHaste.RULESET);out.writeInt(selected.size());
        for(var i:selected){out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());}
    }
    /** Old configuration namespaces receive no new bytes. */
    void writeHasteBinding(java.io.DataOutputStream out)throws IOException {
        var selected=inputs.stream().filter(Input::haste).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        if(selected.isEmpty())return;out.writeUTF(ModernHaste.RULESET);out.writeInt(selected.size());
        for(var i:selected){out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());}
    }
    /** Old configuration namespaces receive no new bytes. */
    void writeSlowBinding(java.io.DataOutputStream out)throws IOException {
        var selected=inputs.stream().filter(Input::slow).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        if(selected.isEmpty())return;out.writeUTF(ModernSlow.RULESET);out.writeInt(selected.size());
        for(var i:selected){out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());}
    }
    boolean npcFireStormEnabled(){return inputs.stream().anyMatch(i->i.npc()!=null&&i.npc().fireStorm());}
    boolean npcDisintegrateEnabled(){return inputs.stream().anyMatch(i->i.npc()!=null&&i.npc().disintegrate());}
    boolean raceEnabled(){return inputs.stream().anyMatch(i->i.race()!=null);}
    boolean npcEnabled(){return inputs.stream().anyMatch(i->i.npc()!=null);}
    boolean questEnabled(){return inputs.stream().anyMatch(i->i.npc()!=null&&i.npc().quest()!=null);}
    boolean timeAttackEnabled(){return inputs.stream().anyMatch(i->i.npc()!=null&&i.npc().timeAttack()!=null);}
    boolean siegeEnabled(){return inputs.stream().anyMatch(i->i.siege()!=null);}
    /** No bytes for older configurations; changed geometry cannot inherit pending siege events. */
    void writeSiegeBinding(java.io.DataOutputStream out)throws IOException {
        if(!siegeEnabled())return;out.writeUTF(ModernSiegeConfiguration.RULESET);
        var selected=inputs.stream().filter(i->i.siege()!=null).sorted(java.util.Comparator.comparingInt(i->i.key().logicalMapId())).toList();
        out.writeInt(selected.size());for(var i:selected){out.writeInt(i.key().logicalMapId());out.writeInt(i.geometry().mapId());out.writeUTF(i.expected().manifest().digest());
            out.writeUTF(ModernSiegeSettlement.configHash(i.siege()));out.writeInt(i.stepMillis());out.writeInt(i.attackMillis());out.writeInt(i.castMillis());
            out.writeBoolean(i.serverBots());out.writeBoolean(i.matchItems());out.writeBoolean(i.weight());out.writeBoolean(i.regen());
            nullable(out,i.clocks().loadTimeoutMillis());nullable(out,i.clocks().countdownNanos());}
    }
    Map<ModernWorldBinding.Key,ModernNpcConfiguration.Spec> npcSpecs(){return inputs.stream().filter(i->i.npc()!=null)
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Input::key,Input::npc));}
    /** Local policy only: SQL layout revisions/clocks never change a durable-result namespace. */
    void writeRaceBinding(java.io.DataOutputStream out)throws IOException {
        var selected=inputs.stream().filter(i->i.race()!=null).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        out.writeUTF(ModernPetRace.RULESET);out.writeInt(selected.size());
        for(var i:selected){out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());out.writeInt(i.geometry().mapId());out.writeUTF(i.race().courseSha256());}
    }
    List<ModernWorldBinding.Key> fireStormBindings(){return inputs.stream().filter(Input::fireStorm).map(Input::key).sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode).thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();}
    /** Append only when explicitly enabled, keeping every older outbox namespace byte-identical. */
    void writeHonorBinding(java.io.DataOutputStream out)throws IOException {
        if(!honorEnabled())return;
        out.writeUTF(ModernHonor.RULESET);
        var selected=inputs.stream().filter(i->i.honor()!=null).sorted(java.util.Comparator.comparingInt((Input i)->i.key().mode()).thenComparingInt(i->i.key().logicalMapId())).toList();
        out.writeInt(selected.size());
        for(var i:selected){
            out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());out.writeInt(i.geometry().mapId());
            out.writeUTF(i.expected().manifest().digest());
            for(var t:i.honor().towers()){out.writeInt(t.team());out.writeInt(t.cell().x());out.writeInt(t.cell().y());out.writeInt(t.graphic());out.writeLong(t.maximumHp());}
            for(var spawn:i.honor().teamSpawns()){out.writeInt(spawn.x());out.writeInt(spawn.y());}
        }
    }
    /** Local Quest policy is immutable outbox routing. SQL layout pins remain
     * startup qualification, not a reason to strand already journaled results. */
    void writeQuestBinding(java.io.DataOutputStream out)throws IOException {
        if(!questEnabled())return;
        out.writeUTF("modern-quest-configured-routing-v1");
        var selected=inputs.stream().filter(i->i.npc()!=null&&i.npc().quest()!=null)
                .sorted(java.util.Comparator.comparingInt(i->i.key().logicalMapId())).toList();
        out.writeInt(selected.size());
        for(var i:selected) {
            var n=i.npc();out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());out.writeInt(i.geometry().mapId());
            out.writeUTF(i.expected().manifest().digest());ModernQuestConfiguration.cell(out,i.anchor());
            out.writeInt(i.stepMillis());out.writeInt(i.attackMillis());
            out.writeBoolean(i.clocks()!=null);
            if(i.clocks()!=null){nullable(out,i.clocks().loadTimeoutMillis());nullable(out,i.clocks().countdownNanos());nullable(out,i.clocks().activeTimeoutMillis());}
            out.writeUTF(ModernNpcMelee.RULESET);out.writeInt(n.catalogSource());out.writeUTF(n.catalogDatabase());
            out.writeInt(n.ids().size());for(int id:n.ids())out.writeInt(id);
            out.writeInt(n.capacity());out.writeInt(n.corpseMillis());out.writeInt(n.pathBudget());out.writeInt(n.playerCapacity());
            out.writeUTF(ModernNpcConfiguration.CLOCK);out.writeInt(n.nightStart());out.writeInt(n.nightEnd());
            for(var camp:n.camps())ModernQuestConfiguration.cell(out,camp);
            n.quest().writeBinding(out);
            // No bytes change for existing non-casting Quest outboxes. An
            // enabled teleport policy/cadence must not reuse their namespace.
            if(i.castMillis()!=0){out.writeUTF("modern-npc-teleport-v23");out.writeUTF(SPELL_RULESET);out.writeInt(i.castMillis());}
            if(n.lightArrow())out.writeUTF(ModernNpcMagic.RULESET);
            if(n.disintegrate()){out.writeUTF("NPC77");out.writeUTF(ModernDisintegrate.RULESET);}
            if(n.fireStorm()){out.writeUTF("NPC70");out.writeUTF(ModernNpcFireStorm.RULESET);}
        }
    }
    private static void nullable(java.io.DataOutputStream out,Number n)throws IOException{out.writeBoolean(n!=null);if(n!=null)out.writeLong(n.longValue());}
    /** Absent policy writes zero bytes, preserving all existing result namespaces. */
    void writeTimeAttackBinding(java.io.DataOutputStream out)throws IOException {
        if(!timeAttackEnabled())return;
        out.writeUTF("modern-time-attack-configured-routing-v1");
        var selected=inputs.stream().filter(i->i.npc()!=null&&i.npc().timeAttack()!=null)
                .sorted(java.util.Comparator.comparingInt(i->i.key().logicalMapId())).toList();
        out.writeInt(selected.size());
        for(var i:selected) {
            var n=i.npc();out.writeInt(i.key().mode());out.writeInt(i.key().logicalMapId());out.writeInt(i.geometry().mapId());
            out.writeUTF(i.expected().manifest().digest());ModernQuestConfiguration.cell(out,i.anchor());
            out.writeInt(i.stepMillis());out.writeInt(i.attackMillis());out.writeInt(i.castMillis());
            out.writeBoolean(i.clocks()!=null);
            if(i.clocks()!=null){nullable(out,i.clocks().loadTimeoutMillis());nullable(out,i.clocks().countdownNanos());nullable(out,i.clocks().activeTimeoutMillis());}
            out.writeUTF(ModernNpcMelee.RULESET);out.writeInt(n.catalogSource());out.writeUTF(n.catalogDatabase());
            out.writeInt(n.ids().size());for(int id:n.ids())out.writeInt(id);
            out.writeInt(n.capacity());out.writeInt(n.corpseMillis());out.writeInt(n.pathBudget());out.writeInt(n.playerCapacity());
            out.writeUTF(ModernNpcConfiguration.CLOCK);out.writeInt(n.nightStart());out.writeInt(n.nightEnd());
            ModernQuestConfiguration.cell(out,n.camps().get(0));n.timeAttack().writeBinding(out);
            if(n.lightArrow()){out.writeUTF("NPC4");out.writeUTF(ModernNpcMagic.RULESET);}
            if(n.disintegrate()){out.writeUTF("NPC77");out.writeUTF(ModernDisintegrate.RULESET);}
            if(n.fireStorm()){out.writeUTF("NPC70");out.writeUTF(ModernNpcFireStorm.RULESET);}
        }
    }
    List<ModernWorldBinding.Key> regenBindings(){return inputs.stream().filter(Input::regen).map(Input::key).sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode).thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();}
    List<ModernWorldBinding.Key> bowBindings(){return inputs.stream().filter(Input::bow).map(Input::key).sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode).thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();}
    List<ModernWorldBinding.Key> timedBindings() {
        return inputs.stream().filter(Input::timed).map(Input::key).sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode)
                .thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();
    }
    List<ModernWorldBinding.Key> weightBindings() {
        return inputs.stream().filter(Input::weight).map(Input::key)
                .sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode)
                        .thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();
    }
    List<ModernWorldBinding.Key> itemBindings() {
        return inputs.stream().filter(Input::matchItems).map(Input::key)
                .sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode)
                        .thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();
    }
    List<ModernWorldBinding.Key> botBindings() {
        return inputs.stream().filter(Input::serverBots).map(Input::key)
                .sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode)
                        .thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();
    }
    List<ModernWorldBinding.Key> damageMagicBindings() {
        return inputs.stream().filter(Input::damageMagic).map(Input::key)
                .sorted(java.util.Comparator.comparingInt(ModernWorldBinding.Key::mode)
                        .thenComparingInt(ModernWorldBinding.Key::logicalMapId)).toList();
    }
    @Override public String toString() { return "ModernWorldConfiguration[redacted]"; }

    static Set<String> allowedKeys() {
        Set<String> keys = new HashSet<>(Set.of("world.sharedRoot", "world.bindings", ModernSharedSiegePreset.KEY));
        ModernSharedDeathmatchPreset.MAPS.forEach(id->keys.add(ModernSharedDeathmatchPreset.key(id)));
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            for (int map : mode.maps()) {
                for (String suffix : SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                if(mode.wireId()==1)keys.add("world.1."+map+".roomCapacity");
                for (String suffix : COMBAT_SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                for (String suffix : SPELL_SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                for (String suffix : CLOCK_SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                for (String suffix : RACE_SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                for (String suffix : ModernNpcConfiguration.SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                for (String suffix : ModernQuestConfiguration.SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                for (String suffix : ModernTimeAttackConfiguration.SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                for (String suffix : ModernSiegeConfiguration.SUFFIXES) keys.add("world." + mode.wireId() + "." + map + "." + suffix);
                keys.add("world." + mode.wireId() + "." + map + ".botRules");
                keys.add("world." + mode.wireId() + "." + map + ".itemRules");
                keys.add("world." + mode.wireId() + "." + map + ".weightRules");
                keys.add("world." + mode.wireId() + "." + map + ".buffRules");
                keys.add("world." + mode.wireId() + "." + map + ".bowRules");
                keys.add("world." + mode.wireId() + "." + map + ".regenRules");
                keys.add("world." + mode.wireId() + "." + map + ".honorRules");
                keys.add("world." + mode.wireId() + "." + map + ".fireStormRules");
                keys.add("world." + mode.wireId() + "." + map + ".disintegrateRules");
                keys.add("world." + mode.wireId() + "." + map + ".greaterHasteRules");
                keys.add("world." + mode.wireId() + "." + map + ".hasteRules");
                keys.add("world." + mode.wireId() + "." + map + ".slowRules");
                keys.add("world." + mode.wireId() + "." + map + ".playerHealingRules");
                keys.add("world." + mode.wireId() + "." + map + ".armorProtectionRules");
                keys.add("world." + mode.wireId() + "." + map + ".fullVipRules");
                keys.add("world." + mode.wireId() + "." + map + ".tower0");
                keys.add("world." + mode.wireId() + "." + map + ".tower1");
                keys.add("world." + mode.wireId() + "." + map + ".spawn0");
                keys.add("world." + mode.wireId() + "." + map + ".spawn1");
                keys.add("world." + mode.wireId() + "." + map + ".manifestSha256");
                keys.add("world." + mode.wireId() + "." + map + ".sectorPins");
            }
        }
        return Set.copyOf(keys);
    }

    /** Entirely static, read-only validation, before environment secrets are consulted. */
    static ModernWorldConfiguration parse(Properties input) throws ModernServerConfig.ConfigException {
        final Properties properties;
        try { properties=ModernSharedDeathmatchPreset.expand(ModernSharedSiegePreset.expand(input)); }
        catch(IllegalArgumentException failure) { throw invalid(); }
        Set<String> supplied = new HashSet<>();
        for (String key : properties.stringPropertyNames()) if (key.startsWith("world.")) supplied.add(key);
        if (supplied.isEmpty()) return new ModernWorldConfiguration(null, List.of());
        try {
            Path root = Path.of(required(properties, "world.sharedRoot"));
            if (!root.isAbsolute() || root.getFileName() == null) throw invalid();
            ModernServerConfig.checkedLocalPath(root.resolve(".modern-world-readonly-validation"), false);
            String[] bindings = required(properties, "world.bindings").split(",", -1);
            if (bindings.length > 40) throw invalid();
            Set<String> consumed = new HashSet<>(Set.of("world.sharedRoot", "world.bindings"));
            Set<ModernWorldBinding.Key> unique = new HashSet<>();
            List<Input> inputs = new ArrayList<>();
            for (String value : bindings) {
                String[] fields = value.split(":", -1);
                if (fields.length != 2) throw invalid();
                var key = new ModernWorldBinding.Key(number(fields[0], 0, 5), number(fields[1], 0, 65535));
                if (!unique.add(key)) throw invalid();
                String prefix = "world." + key.mode() + "." + key.logicalMapId() + ".";
                int physical = number(required(properties, prefix + "physicalMap"), 0, 65535);
                String[] sectors = required(properties, prefix + "sectors").split(",", -1);
                if (sectors.length > ModernWorldMap.MAX_SECTORS) throw invalid();
                Set<ModernWorldMap.SectorKey> selected = new HashSet<>();
                for (String sector : sectors) {
                    if (!sector.matches("[0-9a-f]{8}")) throw invalid();
                    if (!selected.add(new ModernWorldMap.SectorKey(Integer.parseInt(sector.substring(0, 4), 16),
                            Integer.parseInt(sector.substring(4), 16)))) throw invalid();
                }
                int x = signed(required(properties, prefix + "spawnX"));
                int y = signed(required(properties, prefix + "spawnY"));
                ModernWorldMapQualification.Expected expected = null;
                if (properties.containsKey(prefix + "manifestSha256") || properties.containsKey(prefix + "sectorPins")) {
                    var pinned = sectorPins(required(properties, prefix + "sectorPins"));
                    if (!pinned.stream().map(ModernWorldMap.SectorManifest::key).collect(java.util.stream.Collectors.toSet()).equals(selected)) throw invalid();
                    expected = new ModernWorldMapQualification.Expected(key,
                            new ModernWorldMap.Manifest(physical, pinned, required(properties, prefix + "manifestSha256")),
                            new ModernWorldMap.Cell(x, y), ModernWorldMap.MAX_SPAWNS);
                    consumed.add(prefix + "manifestSha256"); consumed.add(prefix + "sectorPins");
                }
                int step = number(required(properties, prefix + "stepMillis"), 1, 10_000);
                int roomCapacity=100;
                if(properties.containsKey(prefix+"roomCapacity")) {
                    if(key.mode()!=1||expected==null)throw invalid();
                    roomCapacity=number(required(properties,prefix+"roomCapacity"),2,100);
                    expected=new ModernWorldMapQualification.Expected(key,expected.manifest(),expected.anchor(),roomCapacity);
                    consumed.add(prefix+"roomCapacity");
                }
                int attack = 0;
                if (properties.containsKey(prefix + "combatRules") || properties.containsKey(prefix + "attackMillis")) {
                    if (!ModernPhysicalCombat.RULESET.equals(required(properties, prefix + "combatRules"))) throw invalid();
                    if (!BattleRules.Mode.fromWire(key.mode()).playerVersusPlayer()
                            && !((key.mode()==2||key.mode()==4&&ModernTimeAttackConfiguration.present(properties,prefix))&&properties.containsKey(prefix+"npcRules"))) throw invalid();
                    attack = number(required(properties, prefix + "attackMillis"), 1, 10_000);
                    for (String suffix : COMBAT_SUFFIXES) consumed.add(prefix + suffix);
                }
                for (String suffix : SUFFIXES) consumed.add(prefix + suffix);
                int cast = 0;
                boolean damageMagic = false;
                if (properties.containsKey(prefix + "spellRules") || properties.containsKey(prefix + "castMillis")) {
                    String spellRules = required(properties, prefix + "spellRules");
                    damageMagic = DAMAGE_SPELL_RULESET.equals(spellRules);
                    if (attack == 0 || !damageMagic && !SPELL_RULESET.equals(spellRules)) throw invalid();
                    cast = number(required(properties, prefix + "castMillis"), 10, 10_000);
                    for (String suffix : SPELL_SUFFIXES) consumed.add(prefix + suffix);
                }
                boolean bots=properties.containsKey(prefix+"botRules");
                if(bots) {
                    if(attack==0 || !BOT_RULESET.equals(required(properties,prefix+"botRules")))throw invalid();
                    consumed.add(prefix+"botRules");
                }
                boolean items=properties.containsKey(prefix+"itemRules");
                if(items) {
                    if(attack==0 || !ITEM_RULESET.equals(required(properties,prefix+"itemRules")))throw invalid();
                    consumed.add(prefix+"itemRules");
                }
                boolean weight=properties.containsKey(prefix+"weightRules");
                if(weight) {
                    if(!items || attack==0 || !WEIGHT_RULESET.equals(required(properties,prefix+"weightRules")))throw invalid();
                    consumed.add(prefix+"weightRules");
                }
                boolean timed=properties.containsKey(prefix+"buffRules");
                if(timed) {
                    if(!weight || !TIMED_RULESET.equals(required(properties,prefix+"buffRules")))throw invalid();
                    consumed.add(prefix+"buffRules");
                }
                boolean bow=properties.containsKey(prefix+"bowRules");
                if(bow){if(!timed||!BOW_RULESET.equals(required(properties,prefix+"bowRules")))throw invalid();consumed.add(prefix+"bowRules");}
                boolean regen=properties.containsKey(prefix+"regenRules");
                if(regen){if(!weight||attack==0||!REGEN_RULESET.equals(required(properties,prefix+"regenRules")))throw invalid();consumed.add(prefix+"regenRules");}
                ModernHonor.Config honor=null;
                if(properties.containsKey(prefix+"honorRules")||properties.containsKey(prefix+"tower0")||properties.containsKey(prefix+"tower1")) {
                    if(key.mode()!=5||!regen||!damageMagic||expected==null||!ModernHonor.RULESET.equals(required(properties,prefix+"honorRules")))throw invalid();
                    honor=new ModernHonor.Config(List.of(tower(required(properties,prefix+"tower0"),0),tower(required(properties,prefix+"tower1"),1)),
                            List.of(spawn(required(properties,prefix+"spawn0")),spawn(required(properties,prefix+"spawn1"))));
                    consumed.add(prefix+"honorRules");consumed.add(prefix+"tower0");consumed.add(prefix+"tower1");
                    consumed.add(prefix+"spawn0");consumed.add(prefix+"spawn1");
                }
                boolean fireStorm=properties.containsKey(prefix+"fireStormRules");
                if(fireStorm){
                    if(!regen||!damageMagic||(key.mode()!=1&&honor==null)||!ModernFireStorm.RULESET.equals(required(properties,prefix+"fireStormRules")))throw invalid();
                    consumed.add(prefix+"fireStormRules");
                }
                boolean disintegrate=properties.containsKey(prefix+"disintegrateRules");
                boolean greaterHaste=properties.containsKey(prefix+"greaterHasteRules");
                if(greaterHaste) {
                    if(!timed||cast==0||!ModernGreaterHaste.RULESET.equals(required(properties,prefix+"greaterHasteRules")))throw invalid();
                    consumed.add(prefix+"greaterHasteRules");
                }
                boolean haste=properties.containsKey(prefix+"hasteRules");
                if(haste) {
                    if(!timed||cast==0||!ModernHaste.RULESET.equals(required(properties,prefix+"hasteRules")))throw invalid();
                    consumed.add(prefix+"hasteRules");
                }
                boolean slow=properties.containsKey(prefix+"slowRules");
                if(slow) {
                    if(!timed||cast==0||!ModernSlow.RULESET.equals(required(properties,prefix+"slowRules")))throw invalid();
                    consumed.add(prefix+"slowRules");
                }
                boolean playerHealing=properties.containsKey(prefix+"playerHealingRules");
                boolean armorProtection=properties.containsKey(prefix+"armorProtectionRules");
                if(armorProtection) {
                    if(!weight||attack==0||!ModernArmorProtectionCatalog.RULESET.equals(required(properties,prefix+"armorProtectionRules")))throw invalid();
                    consumed.add(prefix+"armorProtectionRules");
                }
                if(playerHealing) {
                    if(!weight||cast==0||!ModernPlayerHealing.RULESET.equals(required(properties,prefix+"playerHealingRules")))throw invalid();
                    consumed.add(prefix+"playerHealingRules");
                }
                if(disintegrate){
                    if(!regen||!damageMagic||(key.mode()!=1&&honor==null)||!ModernDisintegrate.RULESET.equals(required(properties,prefix+"disintegrateRules")))throw invalid();
                    consumed.add(prefix+"disintegrateRules");
                }
                ModernWorldService.ClockOverrides clocks=null;
                if(CLOCK_SUFFIXES.stream().anyMatch(suffix->properties.containsKey(prefix+suffix))) {
                    clocks=new ModernWorldService.ClockOverrides(
                            properties.containsKey(prefix+"loadTimeoutMillis")?number(required(properties,prefix+"loadTimeoutMillis"),50,30_000):null,
                            properties.containsKey(prefix+"countdownNanos")?countdownNanos(required(properties,prefix+"countdownNanos")):null,
                            properties.containsKey(prefix+"activeTimeoutMillis")?number(required(properties,prefix+"activeTimeoutMillis"),50,1_800_000):null);
                    for(String suffix:CLOCK_SUFFIXES)if(properties.containsKey(prefix+suffix))consumed.add(prefix+suffix);
                }
                RaceInput race=null;
                if(RACE_SUFFIXES.stream().anyMatch(suffix->properties.containsKey(prefix+suffix))) {
                    if(key.mode()!=0||attack!=0||cast!=0||bots||items||honor!=null||expected==null
                            ||!ModernPetRace.RULESET.equals(required(properties,prefix+"raceRules")))throw invalid();
                    long graphic=number(required(properties,prefix+"raceGraphicsId"),1,65535);
                    if(!ModernPetRace.BASE_GRAPHICS.contains(graphic))throw invalid();
                    String hash=required(properties,prefix+"raceCourseSha256");if(!hash.matches("[0-9a-f]{64}"))throw invalid();
                    race=new RaceInput(raceGates(required(properties,prefix+"raceGates")),graphic,hash);
                    expected=new ModernWorldMapQualification.Expected(key,expected.manifest(),expected.anchor(),ModernPetRace.MAX_RUNNERS);
                    for(String suffix:RACE_SUFFIXES)consumed.add(prefix+suffix);
                }
                var npc=ModernNpcConfiguration.parse(properties,prefix,key);
                if(npc!=null) {
                    if(expected==null||attack==0||npc.lightArrow()&&cast==0||damageMagic||timed&&!haste&&!slow||bow||honor!=null||fireStorm||race!=null)throw invalid();
                    // Explicit ROOM_TEST_DUMMY_V1 bots can fill a Quest room.
                    // They remain passive typed participants, never SQL humans
                    // or owners capable of confirming somebody else's death.
                    if(npc.quest()!=null&&!regen)throw invalid();
                    if(npc.timeAttack()!=null&&(bots||clocks==null||clocks.activeTimeoutMillis()==null||clocks.activeTimeoutMillis()>60_000))throw invalid();
                    expected=new ModernWorldMapQualification.Expected(key,expected.manifest(),expected.anchor(),npc.playerCapacity());
                    for(String suffix:ModernNpcConfiguration.SUFFIXES)if(properties.containsKey(prefix+suffix))consumed.add(prefix+suffix);
                    for(String suffix:ModernQuestConfiguration.SUFFIXES)if(properties.containsKey(prefix+suffix))consumed.add(prefix+suffix);
                    for(String suffix:ModernTimeAttackConfiguration.SUFFIXES)if(properties.containsKey(prefix+suffix))consumed.add(prefix+suffix);
                }
                var siege=ModernSiegeConfiguration.parse(properties,prefix,key,clocks==null?null:clocks.activeTimeoutMillis());
                if(siege!=null){
                    if(expected==null||attack==0||damageMagic||timed&&!slow||bow||honor!=null||fireStorm||race!=null||npc!=null)throw invalid();
                    for(var suffix:ModernSiegeConfiguration.SUFFIXES)consumed.add(prefix+suffix);
                }
                if(npc!=null&&(npc.disintegrate()||npc.fireStorm())&&(!regen||cast==0))throw invalid();
                if(disintegrate&&(npc!=null||race!=null||siege!=null))throw invalid();
                if(greaterHaste&&(npc!=null||race!=null||siege!=null&&!slow))throw invalid();
                if(haste&&(race!=null||siege!=null&&!slow))throw invalid();
                boolean capacityQualified=properties.containsKey(prefix+"roomCapacity");
                if(capacityQualified&&attack==0)throw invalid();
                boolean fullVip=properties.containsKey(prefix+"fullVipRules");
                if(fullVip) {
                    if(!FULL_VIP_RULESET.equals(required(properties,prefix+"fullVipRules"))||key.mode()!=1||attack==0
                            ||expected==null||!items||!weight||!timed||!regen||bots||honor!=null||race!=null||npc!=null||siege!=null)throw invalid();
                    consumed.add(prefix+"fullVipRules");
                }
                inputs.add(new Input(key, new Geometry(physical, selected), new ModernWorldMap.Cell(x, y), step, attack, cast, damageMagic,bots,items,weight,timed,bow,regen,expected,honor,fireStorm,clocks,race,npc,siege,disintegrate,roomCapacity,capacityQualified,greaterHaste,haste,slow,playerHealing,armorProtection,fullVip));
            }
            if (!consumed.equals(supplied)) throw invalid();
            // v15 freezes each binding's features before LOADING. Legacy peers
            // are admitted only to bindings compatible with their global caps.
            return new ModernWorldConfiguration(root, inputs);
        } catch (ModernServerConfig.ConfigException failure) { throw failure; }
        catch (RuntimeException failure) { throw invalid(); }
    }

    /** Startup only. Geometry is deduplicated; a bounded budget is reserved before every load. */
    Map<ModernWorldBinding.Key, ModernWorldService.Mapping> load() throws IOException {
        return load(null, () -> Thread.currentThread().isInterrupted());
    }
    /** SQL is only a pinned immutable overlay; never mutate the configuration used by outbox routing. */
    Map<ModernWorldBinding.Key, ModernWorldService.Mapping> load(ModernWorldPolicy.Snapshot policy, java.util.function.BooleanSupplier cancelled) throws IOException {
        return load(policy,cancelled,Map.of());
    }
    Map<ModernWorldBinding.Key, ModernWorldService.Mapping> load(ModernWorldPolicy.Snapshot policy, java.util.function.BooleanSupplier cancelled,
            Map<ModernWorldBinding.Key,ModernNpcEncounter> encounters) throws IOException {
        java.util.Objects.requireNonNull(cancelled);
        if(!encounters.keySet().equals(npcSpecs().keySet()))throw new IOException("qualified NPC encounters required");
        Map<ModernWorldBinding.Key,ModernWorldPolicy.Row> rows=policy==null?Map.of():policy.byBinding();
        if(policy!=null && (!completelyPinned() || !rows.keySet().equals(inputs.stream().map(Input::key).collect(java.util.stream.Collectors.toSet()))))
            throw new IOException("world policy binding set invalid");
        Map<Geometry, ModernWorldMap> geometries = new HashMap<>();
        Map<ModernWorldBinding.Key, ModernWorldService.Mapping> mappings = new HashMap<>();
        long remaining = 256L * 1024 * 1024;
        for (Input input : inputs) {
            if(cancelled.getAsBoolean())throw new IOException("world policy qualification cancelled");
            ModernWorldMap map = geometries.get(input.geometry());
            if (map == null) {
                // Reserve the maximum possible input, not an after-allocation accounting check.
                long reservation = Math.min(ModernWorldMap.MAX_TOTAL_BYTES,
                        (long) input.geometry().sectors().size() * ModernWorldMap.MAX_SECTOR_BYTES);
                if (reservation > remaining) throw new IOException("world resource budget exceeded");
                remaining -= reservation;
                map = ModernWorldMap.load(sharedRoot, input.geometry().mapId(), input.geometry().sectors(),
                        cancelled);
                geometries.put(input.geometry(), map);
                long actual = map.manifest().sectors().stream().mapToLong(ModernWorldMap.SectorManifest::byteLength).sum();
                remaining += reservation - actual;
            }
            try {
                var row=rows.get(input.key());
                var anchor=row==null?input.anchor():row.anchor();
                var expected=input.expected();
                var honor=input.honor();
                var camps=List.<ModernWorldMap.Cell>of();
                var clocks=input.clocks();
                var npc=encounters.get(input.key());
                // A preloaded catalog is not permission to substitute a stale
                // future plan underneath the configured durable namespace.
                if(input.npc()!=null&&(input.npc().quest()!=null||input.npc().timeAttack()!=null)) {
                    var spec=input.npc();
                    if(npc==null||npc.catalog().sourceId()!=spec.catalogSource()||!npc.catalog().database().equals(spec.catalogDatabase())
                            ||!npc.catalog().npcs().keySet().equals(new HashSet<>(spec.ids()))
                            ||spec.quest()!=null&&!spec.quest().qualify(npc.catalog()).equals(npc.questPlan())
                            ||spec.timeAttack()!=null&&!spec.timeAttack().qualify(npc.catalog()).equals(npc.timeAttack())||!spec.camps().equals(npc.lanes())
                            ||spec.capacity()!=npc.capacity()||spec.corpseMillis()!=npc.corpseMillis()
                            ||spec.pathBudget()!=npc.pathBudget()||spec.playerCapacity()!=npc.playerCapacity())
                        throw new IOException("configured cooperative/Quest encounter identity invalid");
                }
                if(row!=null) {
                    if(input.siege()!=null)throw new IOException("siege requires its pinned role camps, not generic SQL team-camp overrides");
                    if(expected==null || row.physicalMapId()!=input.geometry().mapId() || !row.manifestSha256().equals(expected.manifest().digest()))
                        throw new IOException("world policy resource identity invalid");
                    int capacity=Math.min(expected.capacity(),TournamentRoomPolicy.combatantCapacity(100,BattleRules.Mode.fromWire(input.key().mode())));
                    expected=new ModernWorldMapQualification.Expected(input.key(),expected.manifest(),anchor,capacity);
                    camps=row.teamCamps();
                    if(honor!=null&&!camps.isEmpty())honor=new ModernHonor.Config(honor.towers(),camps);
                    clocks=ModernWorldPolicy.clocks(clocks,row.clocks());
                }
                // Every binding verifies its own evidence, even when geometry was already cached for another key.
                var binding = expected == null ? new ModernWorldBinding(input.key().mode(), input.key().logicalMapId(), map)
                        : ModernWorldMapQualification.qualify(map, expected);
                if (expected == null)
                    map.serverChosenSpawns(anchor, TournamentRoomPolicy.rules(input.key().mode(), input.key().logicalMapId()).minimumPlayers());
                if(npc!=null) {
                    if(npc.lightArrow()!=input.npc().lightArrow()||npc.disintegrate()!=input.npc().disintegrate()||npc.fireStorm()!=input.npc().fireStorm())throw new IOException("NPC magic policy differs from qualified input");
                    if(camps.isEmpty())camps=npc.lanes();
                    else npc=new ModernNpcEncounter(npc.key(),npc.catalog(),npc.sources(),camps,npc.capacity(),npc.corpseMillis(),npc.initial(),npc.pathBudget(),npc.night(),npc.playerCapacity(),npc.questPlan(),npc.lightArrow(),npc.timeAttack(),npc.disintegrate(),npc.fireStorm());
                }
                var mapping=new ModernWorldService.Mapping(binding, anchor,
                        TimeUnit.MILLISECONDS.toNanos(input.stepMillis()), TimeUnit.MILLISECONDS.toNanos(input.attackMillis()),
                        TimeUnit.MILLISECONDS.toNanos(input.castMillis()), input.damageMagic(),input.serverBots(),input.matchItems(),input.weight(),input.timed(),input.bow(),input.regen(),honor,input.fireStorm(),clocks,camps,null,npc,input.siege(),input.disintegrate(),input.roomCapacity(),input.greaterHaste(),input.haste(),input.slow());
                if(input.playerHealing())mapping=mapping.withPlayerHealing(true);
                if(input.armorProtection())mapping=mapping.withArmorProtection(true);
                if(input.fullVip())mapping=mapping.withFullVip(true);
                if(input.siege()!=null)input.siege().qualify(binding,cancelled);
                if(input.race()!=null) {
                    if(!camps.isEmpty())throw new IOException("race team camps unsupported");
                    var seats=new ArrayList<ModernRooms.Member>();
                    for(int i=0;i<ModernPetRace.MAX_RUNNERS;i++)seats.add(new ModernRooms.Member(
                            ModernParticipantId.human(1,i+1L),"qualification","qualification",80,1,0,0,true));
                    var spawns=ModernWorldSpawns.allocate(map,seats,List.of(anchor),Set.of(),cancelled).positions();
                    var race=ModernPetRace.qualify(input.key(),map,expected.manifest(),input.race().gates(),input.race().graphicsId(),mapping.stepIntervalNanos(),spawns,cancelled);
                    if(!race.digest().equals(input.race().courseSha256()))throw new IOException("race course identity invalid");
                    mapping=mapping.withRace(race);
                }
                if(row!=null||npc!=null||input.capacityQualified())ModernWorldPolicy.qualifyLayout(mapping,expected.capacity(),cancelled);
                if(cancelled.getAsBoolean())throw new IOException("world policy qualification cancelled");
                mappings.put(input.key(),mapping);
            } catch (RuntimeException invalid) { throw new IOException("world resource binding invalid"); }
        }
        return Map.copyOf(mappings);
    }
    private static String required(Properties properties, String key) throws ModernServerConfig.ConfigException {
        String value = properties.getProperty(key);
        if (value == null || value.isEmpty()) throw invalid();
        return value;
    }
    /** Explicit ordered rectangles, not a lookup or alias of retained LT coordinates. */
    private static List<ModernPetRace.Gate> raceGates(String value)throws ModernServerConfig.ConfigException {
        var rows=value.split(";",-1);if(rows.length<2||rows.length>ModernPetRace.MAX_GATES)throw invalid();
        var result=new ArrayList<ModernPetRace.Gate>();
        for(String row:rows){var fields=row.split(",",-1);if(fields.length!=4)throw invalid();
            result.add(new ModernPetRace.Gate(signed(fields[0]),signed(fields[1]),signed(fields[2]),signed(fields[3])));}
        if(new HashSet<>(result).size()!=result.size())throw invalid();return List.copyOf(result);
    }
    /** Explicit server-defined x,y,gfx,maxHP; no LT-coordinate or missing-map fallback. */
    private static ModernHonor.Tower tower(String text,int team)throws ModernServerConfig.ConfigException {
        try {
            if(text.length()>100||!text.matches("-?[0-9]+,-?[0-9]+,[0-9]+,[1-9][0-9]*"))throw invalid();
            var p=text.split(",",-1);
            return new ModernHonor.Tower(team,new ModernWorldMap.Cell(Integer.parseInt(p[0]),Integer.parseInt(p[1])),Integer.parseInt(p[2]),Long.parseLong(p[3]));
        }catch(IllegalArgumentException e){throw invalid();}
    }
    private static ModernWorldMap.Cell spawn(String text)throws ModernServerConfig.ConfigException {
        try{if(text.length()>32||!text.matches("-?[0-9]+,-?[0-9]+"))throw invalid();var p=text.split(",",-1);return new ModernWorldMap.Cell(Integer.parseInt(p[0]),Integer.parseInt(p[1]));}
        catch(IllegalArgumentException e){throw invalid();}
    }
    /** Complete sorted `sector:length:rawSha256` rows; no wildcard, relative path, or omitted sector. */
    private static List<ModernWorldMap.SectorManifest> sectorPins(String value) throws ModernServerConfig.ConfigException {
        if (value.length() > ModernWorldMap.MAX_SECTORS * 82) throw invalid();
        String[] rows = value.split(",", -1);
        if (rows.length < 1 || rows.length > ModernWorldMap.MAX_SECTORS) throw invalid();
        var result = new ArrayList<ModernWorldMap.SectorManifest>();
        for (String row : rows) {
            String[] fields = row.split(":", -1);
            if (fields.length != 3 || !fields[0].matches("[0-9a-f]{8}")) throw invalid();
            var key = new ModernWorldMap.SectorKey(Integer.parseInt(fields[0].substring(0, 4), 16), Integer.parseInt(fields[0].substring(4), 16));
            result.add(new ModernWorldMap.SectorManifest(key, number(fields[1], 49_158, ModernWorldMap.MAX_SECTOR_BYTES), fields[2]));
        }
        return List.copyOf(result);
    }
    private static int number(String value, int min, int max) throws ModernServerConfig.ConfigException {
        if (!value.matches("0|[1-9][0-9]{0,9}")) throw invalid();
        long result = Long.parseLong(value);
        if (result < min || result > max) throw invalid();
        return (int) result;
    }
    /** Decimal nanoseconds, not int milliseconds or floating point; no saturation/truncation. */
    private static long countdownNanos(String value)throws ModernServerConfig.ConfigException {
        if(!value.matches("0|[1-9][0-9]{0,10}"))throw invalid();
        long result=Long.parseLong(value);
        if(result>TimeUnit.SECONDS.toNanos(30))throw invalid();
        return result;
    }
    private static int signed(String value) throws ModernServerConfig.ConfigException {
        if (!value.matches("0|-?[1-9][0-9]{0,9}")) throw invalid();
        return Integer.parseInt(value);
    }
    private static ModernServerConfig.ConfigException invalid() {
        return new ModernServerConfig.ConfigException(ModernServerConfig.Failure.INVALID_VALUE);
    }
}
