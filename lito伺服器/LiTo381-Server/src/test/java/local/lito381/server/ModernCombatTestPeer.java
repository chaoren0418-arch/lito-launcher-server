package local.lito381.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-classpath only. Actual TLS/Navigation/lease/Rooms/WorldService + shared map;
 * explicitly synthetic qualified source facts, never the user's database. */
public final class ModernCombatTestPeer {
    private ModernCombatTestPeer() {}
    /** Joins even when the later World constructor fails before its finally. */
    private record OwnedCoordinator(ModernSettlementCoordinator value) implements AutoCloseable {
        @Override public void close() throws Exception {
            value.close();
            if (!value.awaitTermination(5_000)) throw new IllegalStateException("SQL replay owner not retired");
        }
    }
    public static void main(String[] args) {
        serve(args,0);
    }
    /** Same pinned rotation rules, source facts, RNG and SQL assertions; only wire changes. */
    static void canonicalRotation(String[] args) {
        serve(args,42);
    }
    /** Same pinned maps, SQL/RNG assertions and bounds through ordinary v47. */
    static void ordinaryRotation(String[] args) {
        serve(args,47);
    }
    /** Same Honor facts, rules, real TLS and SQL audit; v47 ordinary-client contract. */
    static void ordinaryHonor(String[] args) {
        if(args.length!=4||!args[2].equals("canonical-honor"))throw new IllegalArgumentException("Honor-only fixture");
        serve(args,47);
    }
    /** Same source/class/item/RNG/result assertions as the retained v42 fixtures. */
    static void ordinaryInventory(String[] args) {
        if(args.length!=4||!Set.of("inventory-ammo","inventory-ammo-elf").contains(args[2]))
            throw new IllegalArgumentException("inventory-only fixture");
        serve(args,47);
    }
    private static void serve(String[] args,int rotationVersion) {
        try {
            if(rotationVersion!=0 && (args.length!=4
                    || !(rotationVersion==42 && (args[2].equals("rotation")||ModernMapRotationTestPeer.isSharedRotation(args[2]))
                    || rotationVersion==47 && (Set.of("canonical-honor","inventory-ammo","inventory-ammo-elf","rotation").contains(args[2])
                        || ModernMapRotationTestPeer.isSharedRotation(args[2])))))
                throw new IllegalArgumentException("wire override requires a qualified rotation or Honor/inventory fixture");
            if (args.length != 4 || !args[0].equals("serve") || !Set.of("normal", "armed", "settlement", "teleport", "records", "magic", "bots", "items", "weight", "timed", "ammo", "canonical-ammo", "inventory-ammo", "inventory-ammo-elf", "regen", "rotation", "rotation-shared", "rotation-shared-508-531", "rotation-shared-531-534", "rotation-shared-516-531", "rotation-shared-504-531", "rotation-shared-505-531", "rotation-shared-506-531", "honor","fire","fire-honor","spawn3","spawn8","spawn20","spawn-honor20","descriptor-honor","descriptor-legacy","disintegrate","life-ammo","life-honor","life-disintegrate","life-fire","canonical-disintegrate","canonical-equipment","canonical-honor").contains(args[2])) throw new IllegalArgumentException();
            boolean inventory=args[2].equals("inventory-ammo")||args[2].equals("inventory-ammo-elf");
            int actionVersion=rotationVersion!=0?rotationVersion:inventory?42:args[2].equals("canonical-equipment")||args[2].equals("canonical-honor")?41:args[2].equals("canonical-disintegrate")?39:args[2].startsWith("life-")?37:0;
            int lifetime = Integer.parseInt(args[3]);
            int spawnBots=ModernSpawnEgressTestPeer.botCount(args[2]);boolean spawn=spawnBots>0;
            boolean fire=args[2].equals("fire")||args[2].equals("life-fire")||args[2].equals("fire-honor");
            boolean descriptor=args[2].equals("descriptor-honor")||args[2].equals("descriptor-legacy");
            boolean descriptorLegacy=args[2].equals("descriptor-legacy");
            boolean disintegrate=args[2].equals("disintegrate")||args[2].equals("life-disintegrate")||args[2].equals("canonical-disintegrate");
            boolean canonicalAmmo=inventory||args[2].equals("canonical-ammo")||args[2].equals("life-ammo");
            if (lifetime < 5_000 || lifetime > (actionVersion>=39 ? 600_000 : args[2].equals("honor")||args[2].equals("life-honor")||fire||spawn||descriptor||canonicalAmmo ? 180_000 : 60_000)) throw new IllegalArgumentException();
            String root = System.getenv("LITO_SHARED_RESOURCE_ROOT");
            if (root == null || root.isBlank()) throw new IllegalArgumentException();
            boolean bots=args[2].equals("bots")||args[2].equals("fire")||args[2].equals("life-fire")||spawn;
            boolean sharedRotation=ModernMapRotationTestPeer.isSharedRotation(args[2]);
            var sharedIds=sharedRotation?ModernMapRotationTestPeer.sharedIds(args[2]):List.<Integer>of();
            boolean rotation=args[2].equals("rotation")||sharedRotation;
            var sharedMappings=sharedRotation?ModernMapRotationTestPeer.sharedArenas(Path.of(root),args[2]):Map.<ModernWorldBinding.Key,ModernWorldService.Mapping>of();
            boolean honor=args[2].equals("honor")||args[2].equals("life-honor")||args[2].equals("canonical-honor")||args[2].equals("fire-honor")||args[2].equals("spawn-honor20")||descriptor;
            boolean regen=args[2].equals("regen") || honor || fire || spawn || disintegrate;
            boolean ammo=args[2].equals("ammo") || canonicalAmmo || regen;
            boolean timed=args[2].equals("timed") || ammo;
            boolean weight=args[2].equals("weight") || timed;
            boolean items=args[2].equals("items") || weight;
            boolean magic = args[2].equals("magic") || bots || items || rotation;
            boolean records = args[2].equals("records") || magic;
            // The Rust owner must assign this process to its kill-on-close job
            // before releasing this gate. No database descendant exists before GO.
            if ((records || actionVersion==41) && !java.util.Arrays.equals(System.in.readNBytes(3), new byte[]{'G','O','\n'}))
                throw new IllegalArgumentException("missing owned process-tree gate");
            try (var sql = records ? ModernSettlementMysqlFixture.open(
                    Path.of(System.getenv(ModernSettlementMysqlFixture.ENV))) : null) {
                // The v41 equipment fixture is armed, with its own durable gear
                // store, no magic/shop/settlement or SQL. Existing scenarios
                // retain all their original counters and result assertions.
                // canonical-honor retains the exact unarmed -> weight -> bow
                // source composition of honor/life-honor, only negotiating v41.
                run(Path.of(args[1]),sharedRotation?sharedMappings.get(new ModernWorldBinding.Key(1,sharedIds.get(0))).binding().map(): honor ? ModernHonorTestPeer.qualified532(Path.of(root)) : ModernWorldTestPeer.sharedMap(Path.of(root)), lifetime, args[2].equals("armed")||args[2].equals("canonical-equipment"),
                        records || args[2].equals("settlement") || args[2].equals("teleport"),
                        records || args[2].equals("teleport"), magic, bots, items, weight, timed, ammo, regen, sql,
                        sharedRotation?sharedMappings.get(new ModernWorldBinding.Key(1,sharedIds.get(1))).binding().map():descriptor?ModernWorldTestPeer.sharedMap(Path.of(root)):rotation ? ModernMapRotationTestPeer.qualified509(Path.of(root)) : null, honor,fire,spawnBots,descriptor,descriptorLegacy,disintegrate,sharedMappings,canonicalAmmo,actionVersion,args[2].equals("inventory-ammo-elf"));
                if (sql != null) sql.successful();
            }
        } catch (Exception failure) {
            // No exception message, environment, SQL or cause text: class/frames
            // alone locate fixture failures without exposing test credentials.
            System.err.println("MODERN_COMBAT_PEER_FAILED " + failure.getClass().getName());
            java.util.Arrays.stream(failure.getStackTrace()).limit(12).forEach(frame->System.err.println("  at " + frame));
            System.exit(2);
        }
    }
    private static void run(Path fixtures, ModernWorldMap map, int lifetime, boolean armed, boolean settlement, boolean spells, boolean magic,boolean bots,boolean items,boolean weight,boolean timed,boolean ammo,boolean regen,
                            ModernSettlementMysqlFixture sql, ModernWorldMap secondaryMap, boolean honor,boolean fire,int spawnBots,boolean descriptor,boolean descriptorLegacy,boolean disintegrate,Map<ModernWorldBinding.Key,ModernWorldService.Mapping> sharedMappings,boolean canonicalAmmo,int actionVersion,boolean inventoryElf) throws Exception {
        boolean spawn=spawnBots>0;
        boolean rotation = secondaryMap != null&&!descriptor;
        if(descriptor)ModernMapRotationTestPeer.qualify514(secondaryMap);
        if (rotation&&sharedMappings.isEmpty()||spawn&&!honor) ModernMapRotationTestPeer.qualify514(map);
        Path directory = Files.createTempDirectory("modern-combat-peer-").toAbsolutePath().normalize();
        StringBuilder properties = new StringBuilder("schema=1\nbind.port=0\ntls.pkcs12=")
                .append(fixtures.resolve("server.p12").toString().replace('\\','/')).append("\ntls.password.env=T\nsources=7,42\n");
        for (int id : List.of(7,42)) properties.append("source.").append(id).append(".name=Source").append(id).append('\n')
                .append("source.").append(id).append(".mysql.host=127.0.0.1\n")
                .append("source.").append(id).append(".mysql.port=3306\n")
                .append("source.").append(id).append(".mysql.database=fixture_").append(id).append('\n')
                .append("source.").append(id).append(".catalog.database=catalog\n")
                .append("source.").append(id).append(".mysql.username.env=U\n")
                .append("source.").append(id).append(".mysql.password.env=P\n")
                .append("source.").append(id).append(".mysql.passwordScheme=plain\n")
                .append("source.").append(id).append(".mysql.tls=disabled\n");
        try (var config = ModernServerConfig.parse(properties.toString().getBytes(StandardCharsets.UTF_8), name -> "fixture-only");
             var store = new ModernEquipmentOverrideStore(directory)) {
            // The older i64 roster fixture intentionally has a level-120000
            // defender. Its genuine level/2 MR makes ordinary magic zero-damage;
            // use an explicit balanced roster for this lethal magic scenario.
            // Never cap production MR or change any v1-v5 fixture's level.
            var fixture = inventoryElf ? new ModernApplicationTestBackend("inventory-elf",80) : timed && !ammo ? new ModernApplicationTestBackend("timed",80) : magic ? new ModernApplicationTestBackend("auth",80) : new ModernApplicationTestBackend("auth");
            var physicalReads = new AtomicInteger();
            ModernEquipmentAuthority.PhysicalProvider physical = (principal, sourceId, objectId) -> {
                if (!principal.authorizesSource(sourceId) || objectId != 3333) throw new SecurityException();
                String account = sourceId == 7 ? ModernApplicationTestBackend.SUN_ACCOUNT : ModernApplicationTestBackend.MOON_ACCOUNT;
                if (!account.equals(principal.accountForSource(sourceId))) throw new SecurityException();
                var source = config.sources().stream().filter(value -> value.id() == sourceId).findFirst().orElseThrow();
                var row = fixture.characters.characters(principal, sourceId).stream().filter(value -> value.objectId() == objectId).findFirst().orElseThrow();
                physicalReads.incrementAndGet();
                var qualified = ModernPhysicalCombatTest.fixtureSource(sourceId, row, ammo && sourceId==42 ? 40 : items ? 100 : magic ? 40 : armed ? 60 : 20, 10, armed ? 1 : 0);
                if (magic) qualified = ModernMagicCombatTest.enable(ModernMagicCombatTest.stats(qualified,12,0,0,0,spawn?0:fire||disintegrate?300:regen && !honor?6:100));
                if (weight) qualified = weightSource(qualified);
                if (ammo) qualified = ammoSource(qualified,!spawn&&sourceId==7);
                if (regen) qualified = regenSource(qualified,sourceId==7);
                if (honor) qualified = ModernHonorTestPeer.healingSource(qualified);
                if (fire) qualified = ModernFireStormTestPeer.fireSource(qualified,sourceId==7);
                if (disintegrate) qualified = ModernDisintegrateTestPeer.lawfulSource(qualified);
                return new ModernEquipmentAuthority.PhysicalLoaded(qualified,
                        ModernEquipmentBinding.ownerBinding(principal, source));
            };
            var authority = new ModernEquipmentAuthority((principal, sourceId, objectId) -> {
                var loaded = physical.read(principal, sourceId, objectId);
                return new ModernEquipmentAuthority.Loaded(loaded.source().input().equipment(), loaded.ownerBinding());
            }, null, physical, store);
            var presence = new TournamentPresence();
            var binding = new ModernWorldBinding(honor ? 5 : 1, !sharedMappings.isEmpty()?map.mapId():honor ? 532 : 514, map);
            var secondBinding = descriptor?new ModernWorldBinding(1,514,secondaryMap):rotation ? new ModernWorldBinding(1,sharedMappings.isEmpty()?509:secondaryMap.mapId(), secondaryMap) : null;
            var rooms = new ModernRooms(rotation||descriptor ? Set.of(binding.key(),secondBinding.key()) : Set.of(binding.key()),bots?Set.of(binding.key()):Set.of(),
                    sharedMappings.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().playerCapacity())));
            var gmReads=new AtomicInteger();
            var shopReads=new AtomicInteger();
            ModernShopCatalogRepository shopRepository=(principal,sourceId,objectId)-> {
                if(!items || objectId!=3333 || !principal.authorizesSource(sourceId))throw new SecurityException();
                String expected=sourceId==7?ModernApplicationTestBackend.SUN_ACCOUNT:ModernApplicationTestBackend.MOON_ACCOUNT;
                if(!expected.equals(principal.accountForSource(sourceId)))throw new SecurityException();
                var row=fixture.characters.characters(principal,sourceId).stream().filter(c->c.objectId()==objectId).findFirst().orElseThrow();
                shopReads.incrementAndGet();
                var offers=new java.util.ArrayList<>(List.of(potion(40010,0,sourceId==7?40:80),
                        potion(40011,1,sourceId==7?200:400),potion(40012,2,sourceId==7?600:1200)));
                if(timed) {
                    offers.add(timedPotion(40013,3,200));offers.add(timedPotion(40014,4,800));
                    offers.add(timedPotion(40016,5,600));offers.add(timedPotion(40068,6,1000));
                }
                if(ammo) { offers.add(arrow(40744,7,sourceId==7?20:40));offers.add(arrow(40746,8,sourceId==7?30:60)); }
                return Optional.of(new ModernShopCatalog.Snapshot(sourceId,expected,row,offers));
            };
            ModernGmCommandRepository gmRepository=(principal,sourceId,objectId)-> {
                if(!bots || sourceId!=7 || objectId!=3333 || !ModernApplicationTestBackend.SUN_ACCOUNT.equals(principal.accountForSource(sourceId)))throw new SecurityException();
                var row=fixture.characters.characters(principal,sourceId).stream().filter(c->c.objectId()==objectId).findFirst().orElseThrow();
                gmReads.incrementAndGet();
                return Optional.of(new ModernGmCommands.Snapshot(sourceId,principal.accountForSource(sourceId),row,200,false,false,
                        new ModernGmCommands.Definition("de",100,"L1SpawnDe",false)));
            };
            var skillReads = new AtomicInteger();
            var disintegrateReads=new AtomicInteger();
            ModernDisintegrateRepository disintegrateRepository=disintegrate?(principal,sourceId,objectId)->{
                // Same authenticated provider; simulated qualified skill row, never production source writes.
                physical.read(principal,sourceId,objectId);disintegrateReads.incrementAndGet();
                return Optional.of(new ModernDisintegrateCatalog.Snapshot(new TournamentPresence.CharacterKey(sourceId,objectId),
                        sourceId==7?ModernDisintegrateCatalog.Status.QUALIFIED:ModernDisintegrateCatalog.Status.UNLEARNED));
            }:null;
            ModernSkillsRepository skillRepository = (principal, sourceId, objectId) -> {
                var loaded = physical.read(principal, sourceId, objectId);
                skillReads.incrementAndGet();
                var learned = new java.util.ArrayList<>(honor ? List.of(new ModernSkills.Skill(1,"$1436",4,10,ModernSkills.Support.BASIC_HEAL),
                                new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),
                                new ModernSkills.Skill(5,"$1440",10,10,ModernSkills.Support.TELEPORT)) : sourceId != 7 && !regen ? List.of() : magic
                                ? List.of(new ModernSkills.Skill(4,"$1439",3,10,ModernSkills.Support.LIGHT_ARROW),
                                    new ModernSkills.Skill(5,"$1440",10,10,ModernSkills.Support.TELEPORT))
                                : List.of(new ModernSkills.Skill(5, "$1440", 10, 10, ModernSkills.Support.TELEPORT)));
                if(fire)learned.add(new ModernSkills.Skill(70,"$1975",48,2000,ModernSkills.Support.FIRE_STORM));
                if(disintegrate&&sourceId==7)learned.add(new ModernSkills.Skill(77,"究極光裂術",70,5000,ModernSkills.Support.UNIMPLEMENTED));
                if(spawn)learned.clear(); // No learned teleport can rescue a blocked spawn.
                return Optional.of(new ModernSkills.Snapshot(sourceId,loaded.source().input().equipment().source().character(),learned));
            };
            var randomCalls = new AtomicInteger();
            var weightTeleport = new java.util.concurrent.atomic.AtomicBoolean();
            var bowHundredDice = new AtomicInteger();
            var spawns = honor ? ModernHonorTestPeer.SPAWNS : map.serverChosenSpawns(
                    sharedMappings.isEmpty()?ModernWorldTestPeer.ANCHOR:sharedMappings.get(binding.key()).anchor(), 2);
            var destinations = honor?map.teleportCells().stream().filter(cell -> !spawns.contains(cell)).toList()
                    :ModernMapRotationTestPeer.arenaDestinations(map,spawns);
            var near = destinations.stream().filter(cell -> map.canStep(spawns.get(0), cell)).findFirst().orElseThrow();
            var rotationTeleportChoices=new java.util.HashMap<Long,Integer>();var rotationTeleportDraws=new AtomicInteger();
            for(var mapping:sharedMappings.values()) {
                var arenaMap=mapping.binding().map();var births=arenaMap.serverChosenSpawns(mapping.anchor(),2);
                var candidates=ModernMapRotationTestPeer.arenaDestinations(arenaMap,births);
                // These explicitly qualified two-map fixtures have different
                // candidate counts, all >100; light-arrow dice are <=100.
                if(candidates.size()<=100||rotationTeleportChoices.put((long)candidates.size(),
                        ModernMapRotationTestPeer.adjacentChoice(arenaMap,births,candidates))!=null)
                    throw new IllegalStateException("ambiguous rotation RNG fixture");
            }
            if (weight && destinations.size() <= 100) throw new IllegalStateException("weight fixture teleport/dice bounds overlap");
            // First teleport only has a deterministic, server-chosen adjacent RNG fixture.
            // The request never carries a destination; subsequent melee retains real server RNG.
            ModernPhysicalCombat.BoundedRandom random = bound -> {
                if (rotation) {
                    randomCalls.incrementAndGet();var choice=rotationTeleportChoices.get(bound);
                    if(choice!=null){rotationTeleportDraws.incrementAndGet();return choice;}
                    if(!sharedMappings.isEmpty()&&bound>100)throw new IllegalStateException("unexpected shared rotation RNG bound");
                    return bound-1;
                }
                if(regen) {
                    // Explicit ordinary dice fixture; natural CON dice, melee,
                    // and light-arrow each still request their actual bounds.
                    randomCalls.incrementAndGet();
                    return bound-1;
                }
                if(ammo) {
                    randomCalls.incrementAndGet();
                    // The first successful envelope draws first d100=1 and then
                    // ER d100=1, so the defender's positive retained ER evades.
                    // Later first d100=100 hits on equality without a second ER die.
                    // A low d20 alone is NOT an unconditional native fumble.
                    if(bound==100 && bowHundredDice.getAndIncrement()<2)return 0;
                    return bound-1;
                }
                if (weight) {
                    // Weight may deny teleport until a potion is consumed, so
                    // the first real RNG call can be healing rather than movement.
                    // The shared-map teleport population is distinct from this
                    // fixture's bounded melee/magic/healing dice. Only the first
                    // teleport is forced adjacent; authority still chooses/validates it.
                    randomCalls.incrementAndGet();
                    if (bound == destinations.size() && weightTeleport.compareAndSet(false,true))
                        return destinations.indexOf(near);
                    // Timed integration fixes dice, not a client supplied damage outcome.
                    if(timed && bound==10)return 9;
                    if(timed && bound==100)return 99;
                    return java.util.concurrent.ThreadLocalRandom.current().nextLong(bound);
                }
                if (spells && randomCalls.getAndIncrement() == 0) {
                    if (bound != destinations.size()) throw new IllegalStateException("unexpected fixture RNG order");
                    return destinations.indexOf(near);
                }
                return java.util.concurrent.ThreadLocalRandom.current().nextLong(bound);
            };
            // Explicit in-memory writer fixture. Real protocol/result path,
            // not evidence that any source or production SQL was changed.
            var resultCalls = new AtomicInteger();
            var recordReads = new AtomicInteger();
            var resultIds = new java.util.HashSet<java.util.UUID>();
            ModernCharacterRecordRepository recordRepository = sql == null ? ModernCharacterRecordRepository.unavailable()
                    : (principal, sourceId, objectId) -> {
                        var result = ModernCharacterRecordRepository.jdbc(sql::openConnection, ModernSettlementMysqlFixture.DATABASE)
                                .character(principal, sourceId, objectId);
                        recordReads.incrementAndGet(); return result;
                    };
            ModernMatchSettlement.Store resultStore = settlement ? (event, deadline) -> {
                if(spawn||descriptorLegacy)throw new IllegalStateException("movement-only fixture must never settle");
                if (event.mode() != (honor ? 5 : 1) || event.participants().size() != (fire&&bots?4:2)
                        || !event.participants().get(0).identity().equals(ModernParticipantId.human(7,3333))
                        || (bots&&!fire ? event.participants().get(1).identity().isHuman() || event.participants().get(1).identity().sourceId()!=7
                            : !event.participants().get(1).identity().equals(ModernParticipantId.human(42,3333)))
                        || event.participants().get(0).verdict() != ModernMatchSettlement.Verdict.WIN
                        || event.participants().get(1).verdict() != ModernMatchSettlement.Verdict.LOSS
                        || resultCalls.incrementAndGet() > (rotation ? 3 : 1)) throw new IllegalStateException("fixture result invalid");
                if(fire&&bots&&(event.participants().stream().filter(p->!p.identity().isHuman()).count()!=2
                        ||event.participants().stream().anyMatch(p->p.verdict()!=(p.team()==event.participants().get(0).team()?ModernMatchSettlement.Verdict.WIN:ModernMatchSettlement.Verdict.LOSS))))throw new IllegalStateException("fire bot result invalid");
                if (rotation && (event.mapId() != (resultCalls.get()==2 ? binding.key().logicalMapId() : secondBinding.key().logicalMapId())
                        || !resultIds.add(event.resultId()))) throw new IllegalStateException("rotation result map/UUID invalid");
                if (sql != null) {
                    sql.store().persist(event, deadline);
                    // Replay the exact UUID through genuine InnoDB. It must not
                    // award another win/loss, including the two equal object IDs.
                    sql.store().persist(event, deadline);
                }
            } : null;
            try (var outbox = sql == null ? null : new ModernMatchOutbox(
                        Files.createDirectory(sql.artifactDirectory().resolve("battle-outbox")), new byte[32]);
                 var coordinator = sql == null ? null : new OwnedCoordinator(new ModernSettlementCoordinator(outbox, resultStore, 3_000, null));
                 var worlds = new ModernWorldService(rooms,!sharedMappings.isEmpty()?sharedMappings: descriptor?Map.of(
                    binding.key(),new ModernWorldService.Mapping(binding,ModernHonorTestPeer.ANCHOR,200_000_000L,1_000_000_000L,700_000_000L,true,false,true,true,true,true,true,ModernHonorTestPeer.config()),
                    secondBinding.key(),new ModernWorldService.Mapping(secondBinding,ModernWorldTestPeer.ANCHOR,200_000_000L)) : rotation ? Map.of(
                    binding.key(), new ModernWorldService.Mapping(binding,ModernWorldTestPeer.ANCHOR,200_000_000L,1_000_000_000L,700_000_000L,true),
                    secondBinding.key(), new ModernWorldService.Mapping(secondBinding,ModernMapRotationTestPeer.ANCHOR509,200_000_000L,1_000_000_000L,700_000_000L,true)) : Map.of(binding.key(), new ModernWorldService.Mapping(binding,
                    honor ? ModernHonorTestPeer.ANCHOR : ModernWorldTestPeer.ANCHOR, 200_000_000L, 1_000_000_000L, spells ? 700_000_000L : 0, magic,bots,items,weight,timed,ammo,regen, honor ? ModernHonorTestPeer.config() : null,fire).withDisintegrate(disintegrate)),
                    new ModernWorldService.Limits(4, 2, 2, 3_000, 50,
                            // The v36 fixture loads BOTH the existing full preflight
                            // and two independent canonical workers. It still uses
                            // the identical ammo rules/RNG/SQL assertions below.
                            // Rotation keeps the original 15s LOAD / 45s ACTIVE
                            // rules even when its controller harness lives longer.
                            new ModernBattleEngine.Limits(canonicalAmmo||honor||fire||spawn||!rotation&&actionVersion>=39&&lifetime>60_000 ? 30_000 : 15_000, 3_000_000_000L, !canonicalAmmo&&!rotation&&actionVersion>=39&&!honor&&lifetime>60_000 ? 540_000 : canonicalAmmo ? 120_000 : honor||fire||spawn ? 160_000 : 45_000)),
                    random, coordinator == null ? resultStore : coordinator.value(),weight?Map.of(7,timed?100.0:1.0,42,timed?100.0:1.0):Map.of())) {
                ModernServer server = null;
                try {
                    server = new ModernServer(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127,0,0,1}), 0),
                            ModernTlsTestMaterial.serverContext(fixtures, false),
                            new ModernServer.Limits(4, 2, 600, 4, 2, 4, 2_000, 2_000, 2_000, 10_000),
                            fixture.verifier, fixture.characters, presence, (principal, sourceId, objectId) ->
                                Optional.of(physical.read(principal, sourceId, objectId).source().input().equipment().source()),
                            authority, store, rooms, worlds,actionVersion==41&&!honor?43:actionVersion>0?actionVersion:canonicalAmmo?36:worlds.largeMapsEnabled()?34:!sharedMappings.isEmpty()?19: disintegrate?27:descriptor?15:spawn?13:fire?14:honor?13:regen?12:ammo?11:timed?10:weight?9:items?8:bots?7:magic ? 6 : sql != null ? 5 : spells ? 4 : settlement ? 3 : 2,
                            spells ? skillRepository : ModernSkillsRepository.unavailable(), recordRepository, null,
                            bots?gmRepository:ModernGmCommandRepository.unavailable(),items?shopRepository:ModernShopCatalogRepository.unavailable(),items?20000:0,disintegrateRepository);
                    System.out.println("READY " + server.address().getPort()); System.out.flush();
                    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(lifetime);
                    for (;;) {
                        var state = server.snapshot();
                        if (state.accepted() == (bots&&!fire?1:2) && state.sessions() == 0) break;
                        if (state.accepted() > (bots&&!fire?1:2) || server.listenerFailed() || System.nanoTime() - deadline >= 0) throw new IllegalStateException();
                        Thread.sleep(10);
                    }
                    if (fixture.authenticationCalls.get() != (bots&&!fire?2:4) || physicalReads.get() < 2 || server.snapshot().failed() != 0) {
                        // Fixed categories and numeric counters only; preserve the
                        // original strict failure policy without source/SQL secrets.
                        var completed = server.snapshot();
                        System.err.println("MODERN_COMBAT_FLOW auth="+fixture.authenticationCalls.get()+" physical="+physicalReads.get()
                                +" failed="+completed.failed()+" accepted="+completed.accepted()+" sessions="+completed.sessions());
                        System.err.println("MODERN_COMBAT_FAILURE_COUNTS "+server.failureCounts());
                        throw new IllegalStateException("combat qualification not exercised");
                    }
                    if (resultCalls.get() != (spawn||descriptorLegacy?0:rotation ? 3 : settlement ? 1 : 0)) throw new IllegalStateException("fixture result not exercised");
                    if (rotation && (physicalReads.get() < 6 || recordReads.get() < 8)) throw new IllegalStateException("rotation source reread not exercised");
                    if(!sharedMappings.isEmpty()&&rotationTeleportDraws.get()!=3)throw new IllegalStateException("three actual arena teleports not exercised");
                    if (spells && !descriptorLegacy && (skillReads.get() < (bots?2:4) || !spawn&&randomCalls.get() < 1)) throw new IllegalStateException("spell source/cast not exercised");
                    if (sql != null && recordReads.get() < (bots||descriptorLegacy?2:5)) throw new IllegalStateException("central record not read by owners");
                    if(bots && gmReads.get()<1)throw new IllegalStateException("GM source policy not read");
                    if(items && shopReads.get()<(descriptorLegacy?2:spawn?1:10))throw new IllegalStateException("shop source qualification not exercised");
                    if(disintegrate&&disintegrateReads.get()!=4)throw new IllegalStateException("Disintegrate catalog/preparation capture count mismatch");
                } finally {
                    try {
                        if (server != null) {
                            server.close();
                            if (!server.awaitTermination(5_000)) throw new IllegalStateException("combat server not retired");
                        }
                    } finally {
                        worlds.close();
                        if (!worlds.awaitTermination(5_000) || !worlds.isQuiescent() || worlds.registrationCount() != 0
                                || presence.ownerCount() != 0 || presence.userCount() != 0 || rooms.ownerCount() != 0
                                || rooms.roomCount() != 0 || rooms.memberCount() != 0 || !store.isQuiescent())
                            throw new IllegalStateException("combat owners not retired");
                    }
                }
            }
            if ((spawn||descriptorLegacy) && sql != null && (!java.util.Arrays.equals(new long[]{0,0,0},sql.counts()) || sql.botCount()!=0
                    || sql.activeConnections()!=0 || sql.forcedTermination()))throw new IllegalStateException("spawn fixture created unexpected SQL state");
            if (!spawn && !descriptorLegacy && sql != null && (!java.util.Arrays.equals(rotation?new long[]{3,6,2}:fire&&bots?new long[]{1,2,2}:bots?new long[]{1,1,1}:new long[]{1,2,2}, sql.counts())
                    || !java.util.Arrays.equals(rotation?new long[]{3,0,3}:new long[]{1,0,1}, sql.total(7,3333))
                    || (bots&&sql.botCount()!=(fire?2:1))
                    || (!(bots&&!fire)&&!java.util.Arrays.equals(rotation?new long[]{0,3,3}:new long[]{0,1,1}, sql.total(42,3333)))
                    || sql.activeConnections() != 0 || sql.forcedTermination()))
                throw new IllegalStateException("isolated SQL totals/ownership mismatch");
        } finally {
            // Only this freshly-created flat temporary directory, after resource closure.
            try (var children = Files.list(directory)) {
                for (Path child : children.toList()) {
                    if (!child.toAbsolutePath().normalize().getParent().equals(directory)
                            || !Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS)) throw new IOException("unexpected fixture child");
                    Files.delete(child);
                }
            }
            Files.delete(directory);
        }
    }
    /** Synthetic, pure source facts for the TLS fixture, with no JUnit or source-DB dependency. */
    static ModernPhysicalCombat.Source weightSource(ModernPhysicalCombat.Source source) {
        var old = source.input().equipment(); var p = old.source().persisted();
        if (!old.source().inventory().isEmpty() || !source.input().weapons().isEmpty())
            throw new IllegalArgumentException("weight fixture is explicitly unarmed");
        var metadata = new ModernCharacterData.ItemTemplate(7001,ModernCharacterData.ItemKind.ARMOR,"$2",1,1,2_575_000);
        var item = new ModernCharacterData.InventoryItem(7000,metadata,1,false,0,0,true,0,false,0);
        var instance = new ModernEquipmentFacts.Instance(item,new ModernEquipmentFacts.Template(metadata,
                new EquipmentRules.ArmorFacts(127,new EquipmentRules.LevelRange(1,Long.MAX_VALUE),EquipmentRules.ArmorSlot.BODY),
                2,6,List.of(),ModernEquipmentFacts.Ability.zero(),ModernEquipmentFacts.TemplatePhysical.zero()),
                0,ModernEquipmentFacts.RuleValue.present(ModernEquipmentFacts.Ability.zero()),List.of(0L,0L,0L,0L,0L),ModernEquipmentFacts.Stone.none());
        // Explicit synthetic capacity: floor(.6*20+.4*20+1)*150=3150.
        // Positive original stats have no weight bonus for either fixture class.
        var values = new ModernCharacterData.PersistedValues(p.maxHp(),p.maxMp(),p.savedHp(),p.savedMp(),p.armorClass(),
                20,p.dexterity(),20,p.intelligence(),p.wisdom(),p.charisma(),p.experience(),p.lawful(),
                9,p.originalDexterity(),10,p.originalIntelligence(),p.originalWisdom(),p.food225());
        var snapshot = new ModernCharacterData.Snapshot(old.source().sourceId(),old.source().character(),values,List.of(item));
        var rules = new java.util.HashMap<ModernEquipmentFacts.RuleKey,ModernEquipmentFacts.RuleValue>();
        for (var key : ModernEquipmentFacts.requiredLookups(List.of(instance)))
            if (key.kind()!=ModernEquipmentFacts.RuleKind.VIP) rules.put(key,ModernEquipmentFacts.RuleValue.absent());
        var qualified = new ModernEquipmentFacts.Qualified(snapshot,Map.of(item.objectId(),instance),
                new ModernEquipmentFacts.Catalog(snapshot.sourceId(),old.catalog().resourceDatabase(),rules,Set.of(),List.of(),List.of(),
                        ModernEquipmentFacts.VipTableState.VERIFIED_ABSENT),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(qualified,true,Map.of()),source.growth(),
                Map.of(metadata.catalogId(),new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.ARMOR,"0",null)),
                Set.of(),source.magicGrowth());
    }

    /** Synthetic bow ownership; no source projectiles are imported. */
    static ModernPhysicalCombat.Source ammoSource(ModernPhysicalCombat.Source source,boolean bow) {
        if(!bow)return new ModernPhysicalCombat.Source(source.input(),source.growth(),source.templates(),source.procCatalogIds(),
                source.magicGrowth(),Map.of(),Map.of());
        var old=source.input().equipment();
        var seed=ModernPhysicalCombatTest.weapon(1,1,true,0,ModernEquipmentFacts.Ability.zero(),ModernEquipmentFacts.TemplatePhysical.zero());
        var t=seed.template();
        var weapon=new ModernEquipmentFacts.Instance(seed.source(),new ModernEquipmentFacts.Template(t.metadata(),t.rules(),4,
                t.safeEnchant(),t.setIds(),t.base(),t.physical()),seed.greater(),seed.dark(),seed.cards(),seed.stone());
        var inventory=new java.util.ArrayList<>(old.source().inventory());inventory.add(weapon.source());
        inventory.sort(java.util.Comparator.comparingLong(ModernCharacterData.InventoryItem::objectId));
        var snapshot=new ModernCharacterData.Snapshot(old.source().sourceId(),old.source().character(),old.source().persisted(),inventory);
        var instances=new java.util.HashMap<>(old.items());instances.put(1L,weapon);
        var rules=new java.util.HashMap<ModernEquipmentFacts.RuleKey,ModernEquipmentFacts.RuleValue>();
        for(var key:ModernEquipmentFacts.requiredLookups(List.copyOf(instances.values())))
            if(key.kind()!=ModernEquipmentFacts.RuleKind.VIP)rules.put(key,ModernEquipmentFacts.RuleValue.absent());
        var q=new ModernEquipmentFacts.Qualified(snapshot,instances,new ModernEquipmentFacts.Catalog(snapshot.sourceId(),
                old.catalog().resourceDatabase(),rules,Set.of(),List.of(),List.of(),ModernEquipmentFacts.VipTableState.VERIFIED_ABSENT),
                ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        var weaponFacts=new ModernCombatProfile.WeaponFacts(1,1,4,8,12,10,11,1,0,0,0,0,0);
        var policies=new java.util.HashMap<>(source.templates());
        policies.put(1L,new ModernPhysicalCombat.TemplatePolicy(ModernCharacterData.ItemKind.WEAPON,"0",new ModernPhysicalCombat.Rational(0,1)));
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,true,Map.of(1L,weaponFacts)),source.growth(),
                policies,Set.of(),source.magicGrowth(),Map.of(1L,0L),Map.of(1L,-1L));
    }
    /** Explicit source-qualified retained regen, independent of the real catalog. */
    static ModernPhysicalCombat.Source regenSource(ModernPhysicalCombat.Source source,boolean sun) {
        var old=source.input().equipment();
        var armor=old.items().get(7000L);var t=armor.template();
        var ability=t.base().with(ModernEquipmentFacts.Stat.HP_REGEN,sun?0:-4)
                .with(ModernEquipmentFacts.Stat.MP_REGEN,sun?5:-2)
                .with(ModernEquipmentFacts.Stat.MELEE_DAMAGE,sun?0:30);
        var a=armor.source();
        var equipped=new ModernCharacterData.InventoryItem(a.objectId(),a.template(),a.quantity(),true,a.enchantment(),a.durability(),
                a.identified(),a.blessing(),a.sealed(),a.chargeCount());
        var replacement=new ModernEquipmentFacts.Instance(equipped,new ModernEquipmentFacts.Template(t.metadata(),t.rules(),
                t.retainedType(),t.safeEnchant(),t.setIds(),ability,t.physical()),armor.greater(),armor.dark(),armor.cards(),armor.stone());
        var instances=new java.util.HashMap<>(old.items());instances.put(7000L,replacement);
        var snapshot=new ModernCharacterData.Snapshot(old.source().sourceId(),old.source().character(),old.source().persisted(),
                old.source().inventory().stream().map(item->item.objectId()==7000?equipped:item).toList());
        var q=new ModernEquipmentFacts.Qualified(snapshot,instances,old.catalog(),ModernItemLifetime.Observation.verifiedPermanent(snapshot));
        var rows=new java.util.ArrayList<ModernNaturalRegen.ConRow>();
        for(int con=1;con<=50;con++)rows.add(new ModernNaturalRegen.ConRow(con,ModernNaturalRegen.fallbackConHprMaximum(con)));
        var growth=new ModernNaturalRegen.Growth(old.source().sourceId(),old.catalog().resourceDatabase(),rows);
        return new ModernPhysicalCombat.Source(new ModernCombatProfile.Input(q,source.input().weaponsQualified(),source.input().weapons()),
                source.growth(),source.templates(),source.procCatalogIds(),source.magicGrowth(),source.magicBows(),source.weaponArrowSprites(),growth);
    }
    static ModernShopCatalog.Offer arrow(long id,int order,long price) {
        var kind=ModernAmmo.Kind.find(id);if(kind==null)throw new IllegalArgumentException("fixture arrow");
        var template=new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,id==40744?"$93":"$773",96,66,47,
                new ModernShopCatalog.Etc("0","arrow","arrow",kind.sourceMaterial(),1,0,0,0,0,0,0,0,0,0,0,0,127),
                new ModernShopCatalog.Arrow(id==40744?7:10,id==40744?6:9,-1,1));
        return ModernShopCatalog.Offer.fromSource(id,order,price,price,template);
    }
    /** The real retained item weight remains 3900/4900/5850, not a UI weight guess. */
    private static ModernShopCatalog.Offer potion(long id,int order,long price) {
        var kind=ModernItemProtocol.HealingKind.find(id);
        if(kind==null)throw new IllegalArgumentException("fixture potion");
        var template=new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,"$"+kind.description,kind.inventoryGraphic,
                id==40010?45:id==40011?43:46,id==40010?3900:id==40011?4900:5850,
                new ModernShopCatalog.Etc("hp.UserAddHp "+kind.minimum+" "+kind.maximum+" "+kind.effect+" true 0 0",
                        "potion","hpr","glass",1,0,0,0,0,0,0,2,kind.delayMillis,0,0,0,127));
        return ModernShopCatalog.Offer.fromSource(id,order,price,price,template);
    }
    private static ModernShopCatalog.Offer timedPotion(long id,int order,long price) {
        var kind=ModernTimedEffects.Kind.find(id);
        if(kind==null)throw new IllegalArgumentException("fixture timed potion");
        String executor=switch(kind) {
            case HASTE -> "brave.StatusHaste 300 191 true 40308 10";
            case BRAVE -> "brave.StatusBrave 300 751 true";
            case ELF_BRAVE -> "brave.StatusElfBrave 480 751 true";
            case WISDOM -> "brave.StatusWisdomPotion 300 750";
        };
        String name=switch(kind){case HASTE->"$234";case BRAVE->"$943";case ELF_BRAVE->"$110";case WISDOM->"$944";};
        int icon=switch(kind){case HASTE->77;case BRAVE->79;case ELF_BRAVE->130;case WISDOM->1529;};
        int ground=switch(kind){case HASTE->42;case BRAVE->44;case ELF_BRAVE->81;case WISDOM->3836;};
        var template=new ModernShopCatalog.Template(ModernCharacterData.ItemKind.ETC,name,icon,ground,
                kind==ModernTimedEffects.Kind.HASTE?3900:kind==ModernTimedEffects.Kind.ELF_BRAVE?5000:4500,
                new ModernShopCatalog.Etc(executor,"potion","power",kind==ModernTimedEffects.Kind.ELF_BRAVE?"vegetation":"glass",1,0,0,0,0,0,0,0,0,0,0,0,kind.classMask()));
        return ModernShopCatalog.Offer.fromSource(id,order,price,price,template);
    }
}
