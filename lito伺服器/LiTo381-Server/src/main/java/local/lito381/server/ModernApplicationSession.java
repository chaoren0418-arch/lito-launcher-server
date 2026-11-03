package local.lito381.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * One sequential, TLS-only AUTH/source/character session. This owns no listener,
 * database connection or GameHub. Backend implementations must have bounded calls.
 * PING is allowed before authentication, not a substitute for authentication.
 */
final class ModernApplicationSession {
    private final ModernTlsSessionTransport transport;
    private final ModernCharacterRepository characters;
    private final int readDeadlineMillis;
    private final int backendDeadlineMillis;
    private final ModernBackendExecutor.SessionGate backend;
    private final ModernNavigationSession navigation;
    private int lastRequestId;
    private boolean started;
    private boolean navigationNegotiated;
    private boolean characterDataNegotiated;
    private final ModernCharacterDataRepository characterData;
    private final ModernEquipmentAuthority equipment;
    private boolean equipmentNegotiated;
    private boolean roomsNegotiated;
    private boolean worldsNegotiated;
    private boolean combatNegotiated;
    private boolean npcNegotiated;
    private boolean skillsNegotiated;
    private boolean recordsNegotiated;
    private boolean gmNegotiated;
    private boolean shopNegotiated;

    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence) {
        this(transport, characters, executor, readDeadlineMillis, backendDeadlineMillis, presence,
                ModernCharacterDataRepository.unavailable());
    }

    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData) {
        this(transport, characters, executor, readDeadlineMillis, backendDeadlineMillis, presence, characterData, null);
    }

    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData,
                             ModernEquipmentAuthority equipment) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,null);
    }

    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData,
                             ModernEquipmentAuthority equipment, ModernRooms rooms) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,null);
    }

    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData,
                             ModernEquipmentAuthority equipment, ModernRooms rooms, ModernWorldService worlds) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,
                ModernSkillsRepository.unavailable());
    }
    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData,
                             ModernEquipmentAuthority equipment, ModernRooms rooms, ModernWorldService worlds,
                             ModernSkillsRepository skills) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,
                skills,ModernCharacterRecordRepository.unavailable());
    }
    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData,
                             ModernEquipmentAuthority equipment, ModernRooms rooms, ModernWorldService worlds,
                             ModernSkillsRepository skills, ModernCharacterRecordRepository records) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,
                rooms,worlds,skills,records,ModernGmCommandRepository.unavailable());
    }
    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData,
                             ModernEquipmentAuthority equipment, ModernRooms rooms, ModernWorldService worlds,
                             ModernSkillsRepository skills, ModernCharacterRecordRepository records,
                             ModernGmCommandRepository gmRepository) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,
                skills,records,gmRepository,ModernShopCatalogRepository.unavailable(),0);
    }
    ModernApplicationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                             ModernBackendExecutor executor, int readDeadlineMillis, int backendDeadlineMillis,
                             TournamentPresence presence, ModernCharacterDataRepository characterData,
                             ModernEquipmentAuthority equipment, ModernRooms rooms, ModernWorldService worlds,
                             ModernSkillsRepository skills, ModernCharacterRecordRepository records,
                             ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,skills,records,gmRepository,shopRepository,shopAllowance,null);
    }
    ModernApplicationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                             ModernBackendExecutor executor,int readDeadlineMillis,int backendDeadlineMillis,
                             TournamentPresence presence,ModernCharacterDataRepository characterData,ModernEquipmentAuthority equipment,
                             ModernRooms rooms,ModernWorldService worlds,ModernSkillsRepository skills,ModernCharacterRecordRepository records,
                             ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,skills,records,gmRepository,shopRepository,shopAllowance,disintegrateRepository,null);
    }
    ModernApplicationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                             ModernBackendExecutor executor,int readDeadlineMillis,int backendDeadlineMillis,
                             TournamentPresence presence,ModernCharacterDataRepository characterData,ModernEquipmentAuthority equipment,
                             ModernRooms rooms,ModernWorldService worlds,ModernSkillsRepository skills,ModernCharacterRecordRepository records,
                             ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                             ModernGreaterHasteRepository greaterHasteRepository) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,skills,records,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,null);
    }
    ModernApplicationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                             ModernBackendExecutor executor,int readDeadlineMillis,int backendDeadlineMillis,
                             TournamentPresence presence,ModernCharacterDataRepository characterData,ModernEquipmentAuthority equipment,
                             ModernRooms rooms,ModernWorldService worlds,ModernSkillsRepository skills,ModernCharacterRecordRepository records,
                             ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                             ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,skills,records,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,hasteRepository,null);
    }
    ModernApplicationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                             ModernBackendExecutor executor,int readDeadlineMillis,int backendDeadlineMillis,
                             TournamentPresence presence,ModernCharacterDataRepository characterData,ModernEquipmentAuthority equipment,
                             ModernRooms rooms,ModernWorldService worlds,ModernSkillsRepository skills,ModernCharacterRecordRepository records,
                             ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                             ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository,ModernSlowRepository slowRepository) {
        this(transport,characters,executor,readDeadlineMillis,backendDeadlineMillis,presence,characterData,equipment,rooms,worlds,skills,records,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,hasteRepository,slowRepository,null);
    }
    ModernApplicationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                             ModernBackendExecutor executor,int readDeadlineMillis,int backendDeadlineMillis,
                             TournamentPresence presence,ModernCharacterDataRepository characterData,ModernEquipmentAuthority equipment,
                             ModernRooms rooms,ModernWorldService worlds,ModernSkillsRepository skills,ModernCharacterRecordRepository records,
                             ModernGmCommandRepository gmRepository,ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                             ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository,ModernSlowRepository slowRepository,
                             ModernVipEquipmentAuthority vipEquipment) {
        this.transport = Objects.requireNonNull(transport);
        this.characters = Objects.requireNonNull(characters);
        this.characterData = Objects.requireNonNull(characterData);
        this.equipment = equipment;
        if (readDeadlineMillis < 50 || readDeadlineMillis > 30_000) {
            throw new IllegalArgumentException("application deadline must be 50..30000 ms");
        }
        this.readDeadlineMillis = readDeadlineMillis;
        if (backendDeadlineMillis < 50 || backendDeadlineMillis > 30_000) {
            throw new IllegalArgumentException("backend deadline must be 50..30000 ms");
        }
        this.backendDeadlineMillis = backendDeadlineMillis;
        backend = Objects.requireNonNull(executor).openSession();
        try {
            transport.attachBackend(backend, backendDeadlineMillis);
            navigation = new ModernNavigationSession(transport, characters, backend, backendDeadlineMillis, presence, rooms, worlds, equipment,skills,records,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,hasteRepository,slowRepository,vipEquipment);
        }
        catch (RuntimeException failure) { backend.close(); throw failure; }
    }

    /** Negotiates once, then runs until EOF, authentication rejection, deadline or protocol error. */
    void run() throws IOException {
        if (started) throw new IOException("modern application session is single use");
        started = true;
        try {
            var negotiated = transport.negotiate();
            lastRequestId = negotiated.requestId();
            navigationNegotiated = (negotiated.capabilities() & ModernSessionProtocol.CAP_TOURNAMENT_NAVIGATION) != 0;
            characterDataNegotiated = (negotiated.capabilities() & ModernSessionProtocol.CAP_CHARACTER_DATA) != 0;
            equipmentNegotiated = (negotiated.capabilities() & ModernSessionProtocol.CAP_EQUIPMENT) != 0;
            roomsNegotiated = (negotiated.capabilities() & ModernSessionProtocol.CAP_ROOMS) != 0;
            navigation.negotiateRooms(roomsNegotiated);
            worldsNegotiated=(negotiated.capabilities() & ModernSessionProtocol.CAP_WORLD)!=0;
            navigation.negotiateWorlds(worldsNegotiated);
            combatNegotiated=(negotiated.capabilities() & ModernSessionProtocol.CAP_COMBAT)!=0;
            npcNegotiated=negotiated.version()>=ModernNpcProtocol.RESOURCE_VERSION&&(negotiated.capabilities()&ModernSessionProtocol.CAP_NPC)!=0;
            navigation.negotiateCombat(combatNegotiated);
            navigation.negotiateResults(combatNegotiated && negotiated.version() >= 3);
            skillsNegotiated=combatNegotiated && negotiated.version()>=4;
            navigation.negotiateSkills(skillsNegotiated);
            navigation.negotiateDamageMagic(skillsNegotiated && negotiated.version() >= 6);
            recordsNegotiated = negotiated.version() >= 5;
            navigation.negotiateRecords(recordsNegotiated);
            gmNegotiated = negotiated.version() >= 7 && (negotiated.capabilities() & ModernSessionProtocol.CAP_GM_BOTS) != 0;
            navigation.negotiateGm(gmNegotiated,roomsNegotiated && negotiated.version() >= 7);
            shopNegotiated=negotiated.version()>=8 && (negotiated.capabilities()&ModernSessionProtocol.CAP_SHOP)!=0;
            navigation.negotiateShop(shopNegotiated);
            navigation.negotiateWeight(negotiated.version()>=9 && (negotiated.capabilities()&ModernSessionProtocol.CAP_WEIGHT)!=0);
            navigation.negotiateTimed(negotiated.version()>=10 && (negotiated.capabilities()&ModernSessionProtocol.CAP_TIMED)!=0);
            boolean arrowsNegotiated=negotiated.version()>=11 && (negotiated.capabilities()&ModernSessionProtocol.CAP_ARROW)!=0;
            navigation.negotiateArrow(arrowsNegotiated);
            navigation.negotiateRegen(negotiated.version()>=12&&(negotiated.capabilities()&ModernSessionProtocol.CAP_REGEN)!=0);
            navigation.negotiateHonor(negotiated.version()>=13&&(negotiated.capabilities()&ModernSessionProtocol.CAP_HONOR)!=0);
            navigation.negotiateFireStorm(negotiated.version()>=14&&(negotiated.capabilities()&ModernSessionProtocol.CAP_FIRE_STORM)!=0);
            navigation.negotiateMatchProtocol(negotiated);
            while (true) {
                try (var message = transport.readApplication(readDeadlineMillis)) {
                    if (message == null) return;
                    if (Integer.compareUnsigned(message.requestId(), lastRequestId) <= 0) {
                        throw new ModernApplicationProtocol.ProtocolException();
                    }
                    lastRequestId = message.requestId();
                    if(ModernBattleActionProtocol.negotiated(negotiated)&&ModernBattleActionProtocol.mutation(message))
                        throw new ModernApplicationProtocol.ProtocolException();
                    if (message instanceof ModernApplicationProtocol.Ping ping) {
                        transport.writeApplication(new ModernApplicationProtocol.Pong(ping.requestId()));
                    } else if (message instanceof ModernApplicationProtocol.Auth auth) {
                        if (!authenticate(auth)) return;
                    } else if (message instanceof ModernApplicationProtocol.CharacterRequest request) {
                        listCharacters(request);
                    } else if (message instanceof ModernApplicationProtocol.CharacterDataRequest request) {
                        if (!characterDataNegotiated) throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.characterData(request, characterData));
                    } else if (message instanceof ModernApplicationProtocol.EquipmentRequest request) {
                        equipment(request);
                    } else if(message instanceof ModernApplicationProtocol.RoomRequest request) {
                        if(!roomsNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.room(request));
                    } else if(message instanceof ModernApplicationProtocol.GreaterHasteRequest request) {
                        if(!ModernGreaterHasteProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.GreaterHasteResult(navigation.greaterHaste(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.HasteRequest request) {
                        if(!ModernHasteProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.HasteResult(navigation.haste(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.SlowRequest request) {
                        if(!ModernSlowProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.SlowResult(navigation.slow(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.PlayerHealingRequest request) {
                        if(!ModernSessionExtensions.allowsCommands(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.PlayerHealingResult(navigation.playerHeal(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.VipWorldRequest request) {
                        if(!ModernVipWorldProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.vipWorld(request));
                    } else if(message instanceof ModernApplicationProtocol.VipEquipmentRequest request) {
                        if(!ModernSessionExtensions.allowsVipEquipment(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.vipEquipment(request));
                    } else if(message instanceof ModernApplicationProtocol.BattleEquipmentRequest request) {
                        if(!ModernBattleEquipmentProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.battleEquipment(request));
                    } else if(message instanceof ModernApplicationProtocol.BattleRevivalRequest request) {
                        if(!ModernBattleRevivalProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.revival(request));
                    } else if(message instanceof ModernApplicationProtocol.BattleWorldRequest request) {
                        if(!ModernBattleWorldProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.battleWorld(request));
                    } else if(message instanceof ModernApplicationProtocol.BattleOperationRequest request) {
                        if(!ModernBattleOperationProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.operation(request));
                    } else if(message instanceof ModernApplicationProtocol.BattleActionRequest request) {
                        if(!ModernBattleActionProtocol.negotiated(negotiated)||ModernBattleOperationProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.action(request));
                    } else if(message instanceof ModernApplicationProtocol.BattleReadRequest request) {
                        if(!ModernBattleReadProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.BattleReadResult(request.requestId(),request.request(),navigation.readBattle(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NativeActorWorldRequest request) {
                        if(!worldsNegotiated||!ModernNativeActorWorldProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NativeActorWorldResult(navigation.nativeActorWorld(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NativeCatalogWorldRequest request) {
                        if(!worldsNegotiated||!ModernNativeCatalogWorldProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NativeCatalogWorldResult(navigation.nativeCatalogWorld(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NativeCanonicalWorldRequest request) {
                        if(!worldsNegotiated||!ModernNativeCanonicalWorldProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NativeCanonicalWorldResult(navigation.nativeCanonicalWorld(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NativeWorldRequest request) {
                        if(!worldsNegotiated||!ModernWorldResourceProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NativeWorldResult(navigation.nativeWorld(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.WorldRequest request) {
                        if(ModernBattleWorldProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        if(!worldsNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.WorldResult(request.requestId(),request.request(),navigation.world(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NpcRequest request) {
                        if(!npcNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        if(ModernBattleRevivalProtocol.negotiated(negotiated)&&request.request().action()==ModernNpcProtocol.Action.CONFIRM_DEATH)
                            throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NpcResult(request.requestId(),request.request(),navigation.npc(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NpcFireStormRequest request) {
                        if(!npcNegotiated||!skillsNegotiated||!ModernNpcFireStormProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NpcFireStormResult(request.requestId(),request.request(),navigation.fireStormNpc(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NpcDisintegrateRequest request) {
                        if(!npcNegotiated||!skillsNegotiated||!ModernNpcDisintegrateProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NpcDisintegrateResult(request.requestId(),request.request(),navigation.disintegrateNpc(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.NpcSpellRequest request) {
                        if(!npcNegotiated||!skillsNegotiated||negotiated.version()<24)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.NpcSpellResult(request.requestId(),request.request(),navigation.castNpc(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.SiegeRequest request) {
                        if(!combatNegotiated||negotiated.version()<26||(negotiated.capabilities()&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)==0)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.SiegeResult(request.requestId(),request.request(),navigation.siege(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.CombatRequest request) {
                        if(!combatNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.CombatResult(request.requestId(),request.request(),navigation.combat(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.SkillsRequest request) {
                        if(!skillsNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.skills(request));
                    } else if(message instanceof ModernApplicationProtocol.CastRequest request) {
                        if(!skillsNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.CastResult(request.requestId(),request.request(),navigation.cast(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.DisintegrateRequest request) {
                        if(!skillsNegotiated||!ModernDisintegrateProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.DisintegrateResult(request.requestId(),request.request(),navigation.disintegrate(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.RecordRequest request) {
                        if(!recordsNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.record(request));
                    } else if(message instanceof ModernApplicationProtocol.MatchmakingRequest request) {
                        if(!ModernMatchmakingProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.matchmaking(request));
                    } else if(message instanceof ModernApplicationProtocol.FilteredChatRequest request) {
                        if(!ModernFilteredChatProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.filteredChat(request));
                    } else if(message instanceof ModernApplicationProtocol.ModeratedDirectRequest request) {
                        if(!ModernFilteredChatProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.moderatedDirectChat(request));
                    } else if(message instanceof ModernApplicationProtocol.ChatRequest request) {
                        if(!ModernChatProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.chat(request));
                    } else if(message instanceof ModernApplicationProtocol.DirectChatRequest request) {
                        if(!ModernDirectChatProtocol.negotiated(negotiated))throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.directChat(request));
                    } else if(message instanceof ModernApplicationProtocol.GmRequest request) {
                        if(!gmNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.GmResult(request.requestId(),request.request(),navigation.gm(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.ShopRequest request) {
                        if(!shopNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.ShopResult(request.requestId(),request.request(),navigation.shop(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.ItemRequest request) {
                        if(!shopNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.ItemResult(request.requestId(),request.request(),navigation.item(request.request())));
                    } else if(message instanceof ModernApplicationProtocol.AmmoRequest request) {
                        if(!arrowsNegotiated)throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(new ModernApplicationProtocol.AmmoResult(request.requestId(),request.request(),navigation.ammo(request.request())));
                    } else if (message instanceof ModernApplicationProtocol.CharacterSelection
                            || message instanceof ModernApplicationProtocol.ChannelEnter
                            || message instanceof ModernApplicationProtocol.CharacterReturn
                            || message instanceof ModernApplicationProtocol.LobbyLeave) {
                        if (!navigationNegotiated) throw new ModernApplicationProtocol.ProtocolException();
                        transport.writeApplication(navigation.handle(message));
                    } else throw new ModernApplicationProtocol.ProtocolException();
                }
            }
        } catch (IOException | RuntimeException failure) {
            transport.noteApplicationFailure(failure);
            transport.failClosed();
            throw failure;
        } finally {
            // Transport observes the FIRST navigation cleanup failure, closes
            // the actual socket in all cases and preserves its diagnostic.
            try { transport.close(); } finally { backend.close(); }
        }
    }

    private boolean authenticate(ModernApplicationProtocol.Auth auth) throws IOException {
        if (transport.state() != ModernTlsSessionTransport.State.NEGOTIATED) {
            throw new ModernApplicationProtocol.ProtocolException();
        }
        try {
            var principal = transport.authenticateForApplication(auth.account(), auth.password());
            var sources = principal.sources().stream().sorted(Comparator.comparingInt(ModernAccountAuthentication.Source::id))
                    .map(source -> new ModernApplicationProtocol.Source(source.id(), source.name())).toList();
            transport.writeApplication(new ModernApplicationProtocol.AuthResult(auth.requestId(), 0, principal.account(), sources));
            return true;
        } catch (ModernAccountAuthentication.AuthenticationException failure) {
            if (failure.failure() == ModernAccountAuthentication.Failure.INVALID_STATE) throw new IOException("authentication connection closed");
            int status = failure.failure() == ModernAccountAuthentication.Failure.INVALID_CREDENTIALS ? 1 : 2;
            transport.writeApplication(new ModernApplicationProtocol.AuthResult(auth.requestId(), status, "", List.of()));
            return false;
        } finally { auth.close(); }
    }

    private void equipment(ModernApplicationProtocol.EquipmentRequest message) throws IOException {
        if (!equipmentNegotiated || equipment == null) throw new ModernApplicationProtocol.ProtocolException();
        var request = message.request();
        if(!navigation.prepareEquipmentMutation(request)) {
            transport.writeApplication(new ModernApplicationProtocol.EquipmentResult(message.requestId(),request,
                    ModernEquipmentAuthority.Result.unavailable(ModernEquipmentAuthority.BUSY)));
            return;
        }
        ModernEquipmentAuthority.Result result;
        try {
            result = navigation.equipment(request.sourceId(), request.objectId(), request.epoch(),
                    (principal, selected, operation) -> equipment.execute(principal, selected, operation, request,navigation.equipmentWeightRate(request.sourceId())));
        } catch (ModernBackendExecutor.BackendException failure) {
            if (failure.failure() != ModernBackendExecutor.Failure.BUSY
                    && failure.failure() != ModernBackendExecutor.Failure.OVERLOADED) {
                // The executor intentionally redacts worker exceptions, so an
                // unknown worker failure MAY follow a durable rename. Close;
                // never turn it into a rollback ACK or automatically retry SET.
                throw failure;
            }
            result = ModernEquipmentAuthority.Result.unavailable(ModernEquipmentAuthority.BUSY);
        }
        transport.writeApplication(new ModernApplicationProtocol.EquipmentResult(message.requestId(), request, result));
    }

    private void listCharacters(ModernApplicationProtocol.CharacterRequest request) throws IOException {
        var principal = transport.authenticatedPrincipal().orElseThrow(ModernApplicationProtocol.ProtocolException::new);
        navigation.beforeCharacterRequest();
        ModernApplicationProtocol.CharacterResult response;
        if (!principal.authorizesSource(request.sourceId())) {
            response = result(request, 1, List.of());
        } else {
            try {
                List<ModernApplicationProtocol.CharacterRow> rows = backend.call(backendDeadlineMillis,
                        scope -> characters.characters(principal, request.sourceId()));
                response = result(request, 0, rows);
                // Invalid backend rows become generic backend failure, never a partially valid list.
                byte[] validated = ModernApplicationProtocol.encode(response);
                Arrays.fill(validated, (byte) 0);
            } catch (ModernBackendExecutor.BackendException failure) {
                if (failure.failure() == ModernBackendExecutor.Failure.CANCELLED
                        || failure.failure() == ModernBackendExecutor.Failure.CLOSED
                        || failure.failure() == ModernBackendExecutor.Failure.DEADLINE) throw failure;
                response = result(request, 2, List.of());
            } catch (Exception ignored) {
                response = result(request, 2, List.of());
            }
        }
        transport.writeApplication(response);
        if (response.status() == 0) {
            navigation.recordRoster(request.sourceId(), request.selectionEpoch(), response.characters());
        }
    }

    private static ModernApplicationProtocol.CharacterResult result(ModernApplicationProtocol.CharacterRequest request,
                                                                    int status, List<ModernApplicationProtocol.CharacterRow> rows) {
        return new ModernApplicationProtocol.CharacterResult(request.requestId(), request.sourceId(),
                request.selectionEpoch(), status, rows);
    }
}
