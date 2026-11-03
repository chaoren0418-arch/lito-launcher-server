package local.lito381.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Authorized modern character -> channels -> lobby adapter over shared presence. */
final class ModernNavigationSession implements AutoCloseable {
    private final ModernTlsSessionTransport transport;
    private final ModernCharacterRepository characters;
    private final ModernBackendExecutor.SessionGate backend;
    private final int backendMillis;
    private final TournamentPresence presence;
    private final TournamentPresence.Owner owner;
    private final ModernRooms rooms;
    private final ModernWorldService worlds;
    private final NativePlayerLoadoutAdapter nativePlayerLoadout;
    private final ModernEquipmentAuthority equipment;
    private final ModernVipEquipmentAuthority vipEquipment;
    private final ModernSkillsRepository skillsRepository;
    private final ModernDisintegrateRepository disintegrateRepository;
    private final ModernGreaterHasteRepository greaterHasteRepository;
    private final ModernHasteRepository hasteRepository;
    private final ModernSlowRepository slowRepository;
    private final ModernCharacterRecordRepository recordRepository;
    private final ModernGmCommandRepository gmRepository;
    private final ModernShopCatalogRepository shopRepository;
    private final long shopAllowance;
    private ModernShopAuthority shop;
    private boolean shopEnabled,shopNegotiationRecorded;
    private boolean weightEnabled,weightNegotiationRecorded;
    private boolean timedEnabled,timedNegotiationRecorded;
    private boolean bowEnabled,bowNegotiationRecorded;
    private boolean regenEnabled,regenNegotiationRecorded;
    private boolean honorEnabled,honorNegotiationRecorded;
    private boolean fireStormEnabled,fireStormNegotiationRecorded;
    private ModernSessionBoundary.Negotiated matchProtocol;
    private boolean roomsEnabled;
    private boolean roomsNegotiationRecorded;
    private boolean worldsEnabled;
    private boolean worldsNegotiationRecorded;
    private boolean combatEnabled;
    private boolean combatNegotiationRecorded;
    private boolean skillsEnabled, skillsNegotiationRecorded;
    private boolean damageMagicEnabled, damageMagicNegotiationRecorded;
    private boolean recordsEnabled, recordsNegotiationRecorded;
    private boolean gmEnabled, gmNegotiationRecorded;
    private boolean botProtocol;
    private TournamentPresence.Selection selectedIdentity;
    private final ModernEquipmentLease.Issuer equipmentIssuer = new ModernEquipmentLease.Issuer();
    private static final class EquipmentSelection {
        final ModernEquipmentLease lease;
        EquipmentSelection(ModernEquipmentLease lease) { this.lease = lease; }
    }
    private EquipmentSelection equipmentSelection;
    private boolean resultsNegotiationRecorded, resultsEnabled;
    private boolean closed;
    private volatile boolean cleanupFailed;
    private boolean selecting;
    private long generation;
    private int rosterSource;
    private long rosterEpoch;
    private Set<Long> rosterObjects = Set.of();
    private int phase;
    private int channel = 255;
    /** Opaque lobby admission: leaving and re-entering the same numeric scope
     * must still invalidate an in-flight matchmaking record read. */
    private Object matchmakingLobby;
    private int selectedSource;
    private long selectedEpoch;
    private ModernApplicationProtocol.CharacterRow selected;

    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence) {
        this(transport,characters,backend,backendMillis,presence,null);
    }

    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence, ModernRooms rooms) {
        this(transport,characters,backend,backendMillis,presence,rooms,null);
    }

    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence, ModernRooms rooms, ModernWorldService worlds) {
        this(transport, characters, backend, backendMillis, presence, rooms, worlds, null);
    }
    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence, ModernRooms rooms, ModernWorldService worlds,
                            ModernEquipmentAuthority equipment) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,ModernSkillsRepository.unavailable());
    }
    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence, ModernRooms rooms, ModernWorldService worlds,
                            ModernEquipmentAuthority equipment, ModernSkillsRepository skillsRepository) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,
                ModernCharacterRecordRepository.unavailable());
    }
    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence, ModernRooms rooms, ModernWorldService worlds,
                            ModernEquipmentAuthority equipment, ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,
                recordRepository,ModernGmCommandRepository.unavailable());
    }
    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence, ModernRooms rooms, ModernWorldService worlds,
                            ModernEquipmentAuthority equipment, ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository, ModernGmCommandRepository gmRepository) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,recordRepository,gmRepository,
                ModernShopCatalogRepository.unavailable(),0);
    }
    ModernNavigationSession(ModernTlsSessionTransport transport, ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend, int backendMillis,
                            TournamentPresence presence, ModernRooms rooms, ModernWorldService worlds,
                            ModernEquipmentAuthority equipment, ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository, ModernGmCommandRepository gmRepository,
                            ModernShopCatalogRepository shopRepository,long shopAllowance) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,recordRepository,gmRepository,shopRepository,shopAllowance,null);
    }
    ModernNavigationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend,int backendMillis,TournamentPresence presence,
                            ModernRooms rooms,ModernWorldService worlds,ModernEquipmentAuthority equipment,ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository,ModernGmCommandRepository gmRepository,
                            ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,recordRepository,gmRepository,shopRepository,shopAllowance,disintegrateRepository,null);
    }
    ModernNavigationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend,int backendMillis,TournamentPresence presence,
                            ModernRooms rooms,ModernWorldService worlds,ModernEquipmentAuthority equipment,ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository,ModernGmCommandRepository gmRepository,
                            ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                            ModernGreaterHasteRepository greaterHasteRepository) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,recordRepository,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,null);
    }
    ModernNavigationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend,int backendMillis,TournamentPresence presence,
                            ModernRooms rooms,ModernWorldService worlds,ModernEquipmentAuthority equipment,ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository,ModernGmCommandRepository gmRepository,
                            ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                            ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,recordRepository,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,hasteRepository,null);
    }
    ModernNavigationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend,int backendMillis,TournamentPresence presence,
                            ModernRooms rooms,ModernWorldService worlds,ModernEquipmentAuthority equipment,ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository,ModernGmCommandRepository gmRepository,
                            ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                            ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository,ModernSlowRepository slowRepository) {
        this(transport,characters,backend,backendMillis,presence,rooms,worlds,equipment,skillsRepository,recordRepository,gmRepository,shopRepository,shopAllowance,disintegrateRepository,greaterHasteRepository,hasteRepository,slowRepository,null);
    }
    ModernNavigationSession(ModernTlsSessionTransport transport,ModernCharacterRepository characters,
                            ModernBackendExecutor.SessionGate backend,int backendMillis,TournamentPresence presence,
                            ModernRooms rooms,ModernWorldService worlds,ModernEquipmentAuthority equipment,ModernSkillsRepository skillsRepository,
                            ModernCharacterRecordRepository recordRepository,ModernGmCommandRepository gmRepository,
                            ModernShopCatalogRepository shopRepository,long shopAllowance,ModernDisintegrateRepository disintegrateRepository,
                            ModernGreaterHasteRepository greaterHasteRepository,ModernHasteRepository hasteRepository,ModernSlowRepository slowRepository,
                            ModernVipEquipmentAuthority vipEquipment) {
        this.transport = Objects.requireNonNull(transport);
        this.characters = Objects.requireNonNull(characters);
        this.backend = Objects.requireNonNull(backend);
        if (backendMillis < 50 || backendMillis > 30_000) throw new IllegalArgumentException("invalid backend deadline");
        this.backendMillis = backendMillis;
        this.presence = Objects.requireNonNull(presence);
        this.rooms = rooms;
        this.worlds = worlds;
        this.nativePlayerLoadout = worlds == null ? null : worlds.nativePlayerLoadoutAdapter();
        this.equipment = equipment;
        this.vipEquipment = vipEquipment;
        if(vipEquipment!=null&&(equipment==null||worlds==null))throw new IllegalArgumentException("VIP world needs ordinary navigation and world services");
        this.skillsRepository = Objects.requireNonNull(skillsRepository);
        this.disintegrateRepository=disintegrateRepository;
        this.greaterHasteRepository=greaterHasteRepository;
        this.hasteRepository=hasteRepository;
        this.slowRepository=slowRepository;
        this.recordRepository = Objects.requireNonNull(recordRepository);
        this.gmRepository = Objects.requireNonNull(gmRepository);
        this.shopRepository=Objects.requireNonNull(shopRepository);
        if(shopAllowance<0 || shopAllowance>0 && (worlds==null || equipment==null))throw new IllegalArgumentException("shop needs combat services");
        this.shopAllowance=shopAllowance;
        if(worlds!=null && rooms==null)throw new IllegalArgumentException("world service needs rooms");
        owner = presence.connect();
        try { transport.attachNavigation(this); }
        catch (RuntimeException failure) { close(); throw failure; }
    }

    /** Exactly once after HELLO, before AUTH. Unselected optional features must not constrain legacy peers. */
    synchronized void negotiateRooms(boolean enabled) throws IOException {
        if(closed || roomsNegotiationRecorded || phase!=0 || selecting || enabled && rooms==null)
            throw new ModernApplicationProtocol.ProtocolException();
        roomsNegotiationRecorded=true;roomsEnabled=enabled;
    }

    synchronized void negotiateWorlds(boolean enabled) throws IOException {
        if(closed || worldsNegotiationRecorded || !roomsNegotiationRecorded || phase!=0 || selecting
                || enabled && (worlds==null || !roomsEnabled))throw new ModernApplicationProtocol.ProtocolException();
        worldsNegotiationRecorded=true;worldsEnabled=enabled;
    }
    synchronized void negotiateResults(boolean enabled) throws IOException {
        if (closed || resultsNegotiationRecorded || !worldsNegotiationRecorded || phase != 0 || selecting
                || enabled && !combatEnabled) throw new ModernApplicationProtocol.ProtocolException();
        resultsNegotiationRecorded = true; resultsEnabled = enabled;
    }
    synchronized void negotiateCombat(boolean enabled) throws IOException {
        if (closed || combatNegotiationRecorded || !worldsNegotiationRecorded || phase != 0 || selecting
                || enabled && (!worldsEnabled || equipment == null || !worlds.combatEnabled()))
            throw new ModernApplicationProtocol.ProtocolException();
        combatNegotiationRecorded = true; combatEnabled = enabled;
    }
    synchronized void negotiateSkills(boolean enabled) throws IOException {
        if (closed || skillsNegotiationRecorded || !combatNegotiationRecorded || phase != 0 || selecting
                || enabled && !combatEnabled) throw new ModernApplicationProtocol.ProtocolException();
        skillsNegotiationRecorded = true; skillsEnabled = enabled;
    }
    synchronized void negotiateRecords(boolean enabled) throws IOException {
        if (closed || recordsNegotiationRecorded || phase != 0 || selecting)
            throw new ModernApplicationProtocol.ProtocolException();
        recordsNegotiationRecorded = true; recordsEnabled = enabled;
    }
    synchronized void negotiateDamageMagic(boolean enabled) throws IOException {
        if (closed || damageMagicNegotiationRecorded || !skillsNegotiationRecorded || phase != 0 || selecting
                || enabled && !skillsEnabled) throw new ModernApplicationProtocol.ProtocolException();
        damageMagicNegotiationRecorded = true; damageMagicEnabled = enabled;
    }

    synchronized void negotiateGm(boolean enabled, boolean botProtocol) throws IOException {
        if (closed || gmNegotiationRecorded || !roomsNegotiationRecorded || phase != 0 || selecting
                || enabled && (!roomsEnabled || !botProtocol) || botProtocol && !roomsEnabled)
            throw new ModernApplicationProtocol.ProtocolException();
        gmNegotiationRecorded = true; gmEnabled = enabled; this.botProtocol = botProtocol;
    }
    synchronized void negotiateShop(boolean enabled) throws IOException {
        if(closed || shopNegotiationRecorded || !combatNegotiationRecorded || phase!=0 || selecting
                || enabled && (!combatEnabled || shopAllowance==0))throw new ModernApplicationProtocol.ProtocolException();
        shopNegotiationRecorded=true;shopEnabled=enabled;
    }
    synchronized void negotiateWeight(boolean enabled)throws IOException {
        if(closed || weightNegotiationRecorded || !shopNegotiationRecorded || phase!=0 || selecting
                || enabled&&(!shopEnabled || worlds==null || !worlds.weightEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        weightNegotiationRecorded=true;weightEnabled=enabled;
    }
    synchronized Double equipmentWeightRate(int sourceId)throws IOException {
        requireOpen();return weightEnabled?worlds.weightRate(sourceId):null;
    }
    synchronized void negotiateTimed(boolean enabled)throws ModernApplicationProtocol.ProtocolException {
        if(closed || timedNegotiationRecorded || !weightNegotiationRecorded || phase!=0 || selecting
                || enabled&&(!weightEnabled || worlds==null || !worlds.timedEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        timedNegotiationRecorded=true;timedEnabled=enabled;
    }
    synchronized void negotiateArrow(boolean enabled)throws ModernApplicationProtocol.ProtocolException {
        if(closed || bowNegotiationRecorded || !timedNegotiationRecorded || phase!=0 || selecting
                || enabled&&(!timedEnabled || worlds==null || !worlds.bowEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        bowNegotiationRecorded=true;bowEnabled=enabled;
    }
    synchronized void negotiateRegen(boolean enabled)throws ModernApplicationProtocol.ProtocolException {
        if(closed||regenNegotiationRecorded||!weightNegotiationRecorded||phase!=0||selecting
                ||enabled&&(!weightEnabled||worlds==null||!worlds.regenEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        regenNegotiationRecorded=true;regenEnabled=enabled;
    }
    synchronized void negotiateHonor(boolean enabled)throws ModernApplicationProtocol.ProtocolException {
        if(closed||honorNegotiationRecorded||!regenNegotiationRecorded||phase!=0||selecting
                ||enabled&&(!regenEnabled||worlds==null||!worlds.honorEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        honorNegotiationRecorded=true;honorEnabled=enabled;
    }
    synchronized void negotiateFireStorm(boolean enabled)throws ModernApplicationProtocol.ProtocolException {
        if(closed||fireStormNegotiationRecorded||!regenNegotiationRecorded||phase!=0||selecting
                ||enabled&&(!regenEnabled||!skillsEnabled||worlds==null||!worlds.fireStormEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        fireStormNegotiationRecorded=true;fireStormEnabled=enabled;
    }
    synchronized void negotiateMatchProtocol(ModernSessionBoundary.Negotiated protocol)throws ModernApplicationProtocol.ProtocolException {
        if(closed||phase!=0||selecting||matchProtocol!=null||protocol==null)throw new ModernApplicationProtocol.ProtocolException();
        ModernSessionExtensions.requireContext(protocol);
        if(ModernSessionExtensions.allowsArmor(protocol)&&(!worldsEnabled||!combatEnabled||!weightEnabled
                ||equipment==null||worlds==null||!worlds.armorProtectionEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        if(ModernSessionExtensions.allowsData(protocol)&&(!worldsEnabled||!combatEnabled||!skillsEnabled||!weightEnabled
                ||equipment==null||worlds==null||!worlds.playerHealingEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_GREATER_HASTE)!=0&&(!ModernGreaterHasteProtocol.negotiated(protocol)
                ||!timedEnabled||!skillsEnabled||greaterHasteRepository==null||worlds==null||!worlds.greaterHasteEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_HASTE)!=0&&(!ModernHasteProtocol.negotiated(protocol)
                ||!timedEnabled||!skillsEnabled||hasteRepository==null||worlds==null||!worlds.hasteEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_SLOW_DATA)!=0&&(!ModernSlowProtocol.negotiated(protocol)
                ||!timedEnabled||!skillsEnabled||slowRepository==null||worlds==null||!worlds.slowEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)!=0&&(protocol.version()<15||!worldsEnabled))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_BATTLE_DATA)!=0&&(!ModernBattleReadProtocol.negotiated(protocol)
                ||!worldsEnabled||worlds==null))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_MATCHMAKING)!=0&&(!ModernMatchmakingProtocol.negotiated(protocol)
                ||!roomsEnabled||!worldsEnabled||!recordsEnabled||recordRepository==null||equipment==null))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_DISINTEGRATE)!=0&&(!ModernDisintegrateProtocol.negotiated(protocol)
                ||!regenEnabled||!damageMagicEnabled||!skillsEnabled||disintegrateRepository==null||worlds==null||!worlds.disintegrateEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_NPC_DISINTEGRATE)!=0&&(!ModernNpcDisintegrateProtocol.negotiated(protocol)
                ||!regenEnabled||!skillsEnabled||disintegrateRepository==null||worlds==null||!worlds.npcDisintegrateEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        if((protocol.capabilities()&ModernSessionProtocol.CAP_NPC_FIRE_STORM)!=0&&(!ModernNpcFireStormProtocol.negotiated(protocol)
                ||!regenEnabled||!skillsEnabled||worlds==null||!worlds.npcFireStormEnabled()))throw new ModernApplicationProtocol.ProtocolException();
        matchProtocol=protocol;
    }

    /** Identity and membership checks are atomic; socket encoding stays outside this monitor. */
    synchronized ModernApplicationProtocol.ChatResult chat(ModernApplicationProtocol.ChatRequest message) throws IOException {
        requireOpen();
        var request=message.request(); var scope=request.scope();
        requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!ModernChatProtocol.negotiated(matchProtocol)||!roomsEnabled||phase!=2||channel!=scope.channelId())
            throw new ModernApplicationProtocol.ProtocolException();
        try {
            ModernChat.Result result;
            if(request.action()==ModernChatProtocol.Action.READ) {
                var snapshot=rooms.chatRead(owner,scope,request.roomId(),request.membership(),request.after());
                result=new ModernChat.Result(snapshot.available()?ModernChat.Status.OK:ModernChat.Status.UNAVAILABLE,snapshot);
            } else result=rooms.chatSend(owner,scope,request.roomId(),request.membership(),request.sequence(),request.after(),request.text());
            return new ModernApplicationProtocol.ChatResult(message.requestId(),request,result);
        } catch (IllegalArgumentException|SecurityException stale) {
            throw new ModernApplicationProtocol.ProtocolException();
        }
    }

    synchronized ModernApplicationProtocol.DirectChatResult directChat(ModernApplicationProtocol.DirectChatRequest message) throws IOException {
        requireOpen();var request=message.request();var scope=request.scope();
        requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!ModernDirectChatProtocol.negotiated(matchProtocol)||!roomsEnabled||phase!=2||channel!=scope.channelId())
            throw new ModernApplicationProtocol.ProtocolException();
        try {
            return new ModernApplicationProtocol.DirectChatResult(message.requestId(),request,
                    rooms.directChat(owner,scope,request.roomId(),request.membership(),request.command()));
        } catch(IllegalArgumentException|SecurityException stale){throw new ModernApplicationProtocol.ProtocolException();}
    }

    synchronized ModernApplicationProtocol.FilteredChatResult filteredChat(ModernApplicationProtocol.FilteredChatRequest message) throws IOException {
        var principal=requireOpen();var request=message.request();var scope=request.token().scope();
        requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!ModernFilteredChatProtocol.negotiated(matchProtocol)||!roomsEnabled||phase!=2||channel!=scope.channelId())
            throw new ModernApplicationProtocol.ProtocolException();
        try {
            return new ModernApplicationProtocol.FilteredChatResult(message.requestId(),request,
                    rooms.filteredChatAuthorized(owner,request,principal));
        } catch(IllegalArgumentException|SecurityException stale){throw new ModernApplicationProtocol.ProtocolException();}
    }
    synchronized ModernApplicationProtocol.ModeratedDirectResult moderatedDirectChat(ModernApplicationProtocol.ModeratedDirectRequest message) throws IOException {
        var principal=requireOpen();var request=message.request();var scope=request.scope();
        requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!ModernFilteredChatProtocol.negotiated(matchProtocol)||!roomsEnabled||phase!=2||channel!=scope.channelId())
            throw new ModernApplicationProtocol.ProtocolException();
        try {
            return new ModernApplicationProtocol.ModeratedDirectResult(message.requestId(),request,
                    rooms.moderatedDirectChat(owner,request,principal));
        } catch(IllegalArgumentException|SecurityException stale){throw new ModernApplicationProtocol.ProtocolException();}
    }

    /** Scores come from the selected account's central repository, never the request.
     * SQL is outside Nav/Rooms monitors; admission is fenced by the live selection
     * lease and deadline. READ/CANCEL need no SQL and cannot create an admission. */
    ModernApplicationProtocol.MatchmakingResult matchmaking(ModernApplicationProtocol.MatchmakingRequest message) throws IOException {
        var request=message.request();var context=request.scope();
        final EquipmentSelection attachment;final long expectedGeneration;final Object lobby;
        synchronized(this){
            requireMatchmakingScope(context);
            if(request.action()!=ModernMatchmakingProtocol.Action.START){
                var result=request.action()==ModernMatchmakingProtocol.Action.READ
                        ?rooms.readMatchmaking(owner,context,request.ticketId())
                        :rooms.cancelMatchmaking(owner,context,request.ticketId());
                return new ModernApplicationProtocol.MatchmakingResult(message.requestId(),request,ModernMatchmakingProtocol.Reply.result(result));
            }
            attachment=equipmentSelection;expectedGeneration=generation;lobby=matchmakingLobby;
        }
        ModernMatchmakingProtocol.Reply reply;
        try {
            reply=equipment(context.sourceId(),context.objectId(),context.epoch(),(principal,character,operation)->{
                final ModernCharacterRecord.Snapshot standing;
                try {
                    standing=Objects.requireNonNull(recordRepository.character(principal,context.sourceId(),context.objectId()));
                    if(!standing.key().equals(new TournamentPresence.CharacterKey(context.sourceId(),context.objectId())))
                        throw new IOException("matchmaking record qualification failed");
                } catch(Exception failure){
                    operation.scope().checkActive();
                    if(failure instanceof ModernBackendExecutor.BackendException backendFailure){
                        if(backendFailure.failure()==ModernBackendExecutor.Failure.CANCELLED
                                ||backendFailure.failure()==ModernBackendExecutor.Failure.CLOSED
                                ||backendFailure.failure()==ModernBackendExecutor.Failure.DEADLINE)throw backendFailure;
                        if(backendFailure.failure()==ModernBackendExecutor.Failure.BUSY
                                ||backendFailure.failure()==ModernBackendExecutor.Failure.OVERLOADED)return ModernMatchmakingProtocol.Reply.busy();
                    }
                    return ModernMatchmakingProtocol.Reply.unavailable();
                }
                operation.scope().checkActive();
                if(!operation.tryCommit())throw new ModernApplicationProtocol.ProtocolException();
                synchronized(ModernNavigationSession.this){
                    requireEquipmentAttachment(attachment,expectedGeneration,context.sourceId(),context.objectId(),context.epoch());
                    requireMatchmakingScope(context);
                    if(matchmakingLobby!=lobby)throw new ModernApplicationProtocol.ProtocolException();
                    if(selected!=character)throw new ModernApplicationProtocol.ProtocolException();
                    synchronized(rooms){
                        checkCommitClock(operation);
                        return ModernMatchmakingProtocol.Reply.result(rooms.startMatchmaking(owner,context,request.criteria(),standing));
                    }
                }
            });
        } catch(ModernBackendExecutor.BackendException failure){
            if(failure.failure()!=ModernBackendExecutor.Failure.BUSY&&failure.failure()!=ModernBackendExecutor.Failure.OVERLOADED)throw failure;
            reply=ModernMatchmakingProtocol.Reply.busy();
        }
        synchronized(this){
            requireEquipmentAttachment(attachment,expectedGeneration,context.sourceId(),context.objectId(),context.epoch());
            requireMatchmakingScope(context);
            if(matchmakingLobby!=lobby)throw new ModernApplicationProtocol.ProtocolException();
            return new ModernApplicationProtocol.MatchmakingResult(message.requestId(),request,reply);
        }
    }
    private void requireMatchmakingScope(ModernRooms.Scope scope)throws IOException {
        requireOpen();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!ModernMatchmakingProtocol.negotiated(matchProtocol)||!roomsEnabled||!worldsEnabled||!recordsEnabled
                ||phase!=2||channel!=scope.channelId()||matchmakingLobby==null)throw new ModernApplicationProtocol.ProtocolException();
    }

    /** Invalidate the previous page even if this new source query later fails. */
    synchronized void beforeCharacterRequest() throws IOException {
        requireOpen();
        if (phase != 0 || selecting) throw new ModernApplicationProtocol.ProtocolException();
        rosterSource = 0; rosterEpoch = 0; rosterObjects = Set.of(); generation++;
    }

    /** Called only AFTER a validated successful character result was written. */
    synchronized void recordRoster(int sourceId, long epoch, List<ModernApplicationProtocol.CharacterRow> rows)
            throws IOException {
        requireOpen();
        if (phase != 0 || selecting || sourceId <= 0 || epoch == 0) {
            throw new ModernApplicationProtocol.ProtocolException();
        }
        rosterSource = sourceId; rosterEpoch = epoch;
        rosterObjects = rows.stream().map(ModernApplicationProtocol.CharacterRow::objectId).collect(Collectors.toUnmodifiableSet());
    }

    ModernApplicationProtocol.NavigationResult handle(ModernApplicationProtocol.Message message) throws IOException {
        var result=handleNavigation(message);
        synchronizeWorldRegistration(result);
        return result;
    }

    private ModernApplicationProtocol.NavigationResult handleNavigation(ModernApplicationProtocol.Message message) throws IOException {
        if (message instanceof ModernApplicationProtocol.CharacterSelection request) return select(request);
        synchronized (this) {
            requireOpen();
            if (message instanceof ModernApplicationProtocol.ChannelEnter request) {
                requireSelection(request.sourceId(), request.objectId(), request.selectionEpoch());
                requireOutsideRoom();
                if (request.channelId() < 0 || request.channelId() >= TournamentPresence.TIERS.size()) {
                    throw new ModernApplicationProtocol.ProtocolException();
                }
                if (TournamentPresence.recommendedChannel(selected.level()) == null) {
                    return result(request.requestId(), 11, 1, request.sourceId(), request.objectId(), request.selectionEpoch());
                }
                var admitted = presence.enterChannel(owner, request.channelId());
                if (admitted == null) {
                    return result(request.requestId(), 11, 3, request.sourceId(), request.objectId(), request.selectionEpoch());
                }
                phase = 2; channel = admitted.id();
                if(roomsEnabled)rooms.enterLobby(owner,selectedIdentity,selectedEpoch,channel,botProtocol);
                if(roomsEnabled&&ModernLargeMaps.negotiated(matchProtocol))
                    rooms.enableLargeMaps(owner,new ModernRooms.Scope(selectedSource,selected.objectId(),selectedEpoch,channel));
                matchmakingLobby=new Object();
                if(roomsEnabled&&ModernDirectChatProtocol.negotiated(matchProtocol))
                    rooms.enableDirectChat(owner,new ModernRooms.Scope(selectedSource,selected.objectId(),selectedEpoch,channel));
                if(roomsEnabled&&ModernFilteredChatProtocol.negotiated(matchProtocol))
                    rooms.enableFilteredChat(owner,new ModernRooms.Scope(selectedSource,selected.objectId(),selectedEpoch,channel));
                return result(request.requestId(), 11, 0, request.sourceId(), request.objectId(), request.selectionEpoch());
            }
            if (message instanceof ModernApplicationProtocol.CharacterReturn request) {
                requireSelection(request.sourceId(), request.objectId(), request.selectionEpoch());
                requireOutsideRoom();
                // Atomic with commit admission. A durable operation cannot be
                // abandoned merely because the caller has requested another UI.
                final boolean canReturn;
                try { canReturn = equipmentSelection.lease.tryInvalidateIfNoPermit(); }
                catch (RuntimeException failure) { cleanupFailed = true; throw cleanupFailure(); }
                if (!canReturn) {
                    return result(request.requestId(), 12, 3, request.sourceId(), request.objectId(), request.selectionEpoch());
                }
                presence.returnToCharacters(owner);
                if(roomsEnabled)rooms.leaveLobby(owner);
                equipmentSelection = null;
                matchmakingLobby=null;
                if(shop!=null){shop.close();shop=null;}
                selectedIdentity = null;
                selected = null; selectedSource = 0; selectedEpoch = 0; phase = 0; channel = 255;
                rosterSource = 0; rosterEpoch = 0; rosterObjects = Set.of(); generation++;
                return result(request.requestId(), 12, 0, request.sourceId(), request.objectId(), request.selectionEpoch());
            }
            if (message instanceof ModernApplicationProtocol.LobbyLeave request) {
                requireSelection(request.sourceId(), request.objectId(), request.selectionEpoch());
                if (phase != 2) throw new ModernApplicationProtocol.ProtocolException();
                requireOutsideRoom();
                // Leaving a lobby changes membership, not the selected identity.
                // Keep the source/object/epoch and its role lock until BACK or
                // connection closure; this transition performs no roster read.
                presence.leaveChannel(owner);
                if(roomsEnabled)rooms.leaveLobby(owner);
                phase = 1; channel = 255;
                matchmakingLobby=null;
                return result(request.requestId(), 13, 0, request.sourceId(), request.objectId(), request.selectionEpoch());
            }
            throw new ModernApplicationProtocol.ProtocolException();
        }
    }

    /** No service/room callback is made while holding Navigation's monitor. */
    private void synchronizeWorldRegistration(ModernApplicationProtocol.NavigationResult result) throws IOException {
        final ModernRooms.Scope scope;
        final EquipmentSelection attachment;
        final long expectedGeneration;
        final boolean physical, qualifiedSkills, resultProtocol, damageMagicProtocol, weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol;
        final ModernShopAuthority shopState;
        final ModernSessionBoundary.Negotiated protocol;
        synchronized(this) {
            if(!worldsEnabled || result.status()!=0)return;
            requireOpen(); physical = combatEnabled; qualifiedSkills = skillsEnabled; resultProtocol = resultsEnabled;
            damageMagicProtocol = damageMagicEnabled;
            weightProtocol=weightEnabled;
            timedProtocol=timedEnabled;
            bowProtocol=bowEnabled;
            regenProtocol=regenEnabled;
            honorProtocol=honorEnabled;
            fireStormProtocol=fireStormEnabled;
            shopState=shopEnabled?shop:null;
            protocol=matchProtocol;
            if(phase!=2){scope=null;attachment=null;expectedGeneration=generation;}
            else {scope=new ModernRooms.Scope(selectedSource,selected.objectId(),selectedEpoch,channel);
                attachment=equipmentSelection;expectedGeneration=generation;}
        }
        if(scope==null){worlds.detach(owner);return;}
        boolean fullVip=physical&&ModernVipWorldProtocol.negotiated(protocol)&&vipEquipment!=null&&vipEquipment.sourceEnabled(scope.sourceId());
        worlds.register(owner,scope,deadline->freshWorldCharacter(scope,attachment,expectedGeneration,deadline),
                fullVip ? (ModernWorldService.FullVipReader)(deadline,armor)->freshPhysicalCharacter(scope,attachment,expectedGeneration,deadline,armor,true)
                : physical ? new ModernWorldService.PhysicalReader() {
                    public ModernPhysicalCombat.Fighter read(long deadline)throws Exception {return read(deadline,false);}
                    public ModernPhysicalCombat.Fighter read(long deadline,boolean armorProtection)throws Exception {
                        return freshPhysicalCharacter(scope,attachment,expectedGeneration,deadline,armorProtection);
                    }
                } : null, resultProtocol,
                qualifiedSkills ? deadline -> freshSkillsCharacter(scope,attachment,expectedGeneration,deadline) : null, damageMagicProtocol,
                shopState==null?null:new ModernWorldService.MatchItems() {
                    @Override public List<ModernShopAuthority.Stack> capture(long match,long matchGeneration,long deadline)throws Exception {
                        return captureShop(scope,attachment,expectedGeneration,shopState,match,matchGeneration,deadline,null);
                    }
                    @Override public List<ModernShopAuthority.Stack> capture(long match,long matchGeneration,long deadline,ModernInventoryWeight.Basis basis)throws Exception {
                        if((basis!=null)!=weightProtocol)throw new ModernApplicationProtocol.ProtocolException();
                        return captureShop(scope,attachment,expectedGeneration,shopState,match,matchGeneration,deadline,basis);
                    }
                    @Override public void terminal(long match,long matchGeneration){shopState.terminal(match,matchGeneration);}
                    @Override public List<ModernShopAuthority.Stack> capture(long match,long matchGeneration,long deadline,ModernInventoryWeight.Basis basis,ModernMatchDescriptor descriptor)throws Exception {
                        if(descriptor.has(ModernMatchDescriptor.WEIGHT)!=(basis!=null))throw new ModernApplicationProtocol.ProtocolException();
                        return captureShop(scope,attachment,expectedGeneration,shopState,match,matchGeneration,deadline,basis,descriptor);
                    }
                },weightProtocol,timedProtocol,bowProtocol,regenProtocol,honorProtocol,fireStormProtocol,protocol,
                (ModernDisintegrateProtocol.negotiated(protocol)||ModernNpcDisintegrateProtocol.negotiated(protocol))?deadline->freshDisintegrateCharacter(scope,attachment,expectedGeneration,deadline):null,
                ModernGreaterHasteProtocol.negotiated(protocol)?deadline->freshGreaterHasteCharacter(scope,attachment,expectedGeneration,deadline):null,
                ModernHasteProtocol.negotiated(protocol)?deadline->freshHasteCharacter(scope,attachment,expectedGeneration,deadline):null,
                ModernSlowProtocol.negotiated(protocol)?deadline->freshSlowCharacter(scope,attachment,expectedGeneration,deadline):null,
                ModernSessionExtensions.allowsData(protocol)&&(fullVip?vipEquipment.playerHealingSource(scope.sourceId()):equipment!=null&&equipment.playerHealingSource(scope.sourceId())),
                ModernSessionExtensions.allowsArmor(protocol)&&(fullVip?vipEquipment.armorProtectionSource(scope.sourceId()):equipment!=null&&equipment.armorProtectionSource(scope.sourceId())));
        boolean stale;
        synchronized(this) {
            stale=closed || phase!=2 || channel!=scope.channelId() || equipmentSelection!=attachment
                    || generation!=expectedGeneration;
        }
        if(stale)worlds.detach(owner);
    }

    /** A frozen service participant is qualified through its actual authenticated attachment and backend. */
    private ModernApplicationProtocol.CharacterRow freshWorldCharacter(ModernRooms.Scope scope,
            EquipmentSelection attachment,long expectedGeneration,long absoluteDeadlineNanos) throws IOException {
        final ModernAccountAuthentication.Principal principal;
        synchronized(this) {
            principal=requireOpen();
            requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
            if(!worldsEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        long remaining=absoluteDeadlineNanos-System.nanoTime();
        if(remaining<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50))throw new IOException("world preparation deadline");
        int millis=(int)Math.min(backendMillis,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var result=backend.call(millis,new ModernBackendExecutor.Work<ModernApplicationProtocol.CharacterRow>() {
            private ModernEquipmentLease.Operation operation;
            @Override public ModernApplicationProtocol.CharacterRow run(ModernBackendScope backendScope) throws Exception {
                synchronized(ModernNavigationSession.this) {
                    requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
                    if(phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
                }
                operation=attachment.lease.operation(backendScope);
                var rows=List.copyOf(characters.characters(principal,scope.sourceId()));
                byte[] checked=ModernApplicationProtocol.encode(new ModernApplicationProtocol.CharacterResult(1,
                        scope.sourceId(),scope.epoch(),0,rows));
                Arrays.fill(checked,(byte)0);
                return rows.stream().filter(row->row.objectId()==scope.objectId()).findFirst()
                        .orElseThrow(()->new IOException("world character unavailable"));
            }
            @Override public void close(){if(operation!=null)operation.close();}
        });
        synchronized(this) {
            requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
            if(phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        return result;
    }

    ModernBattleReadProtocol.Reply readBattle(ModernBattleReadProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var s=request.scope();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernBattleReadProtocol.negotiated(matchProtocol)||!worldsEnabled||worlds==null||phase!=2||channel!=s.channelId())
                throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.readBattle(owner,request);
    }

    ModernApplicationProtocol.VipWorldResult vipWorld(ModernApplicationProtocol.VipWorldRequest request)throws IOException {
        synchronized(this) {
            requireOpen();var s=request.request().scope();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernVipWorldProtocol.negotiated(matchProtocol)||!worldsEnabled||!combatEnabled||worlds==null||vipEquipment==null
                    ||phase!=2||channel!=s.channelId())throw new ModernApplicationProtocol.ProtocolException();
            // A configured ordinary source is not a forged owner. Explicitly
            // decline VIP worlds without loading it through a different DAO.
            if(!vipEquipment.sourceEnabled(s.sourceId()))return new ModernApplicationProtocol.VipWorldResult(
                    request.requestId(),request.request(),new ModernWorldService.VipWorldReply(
                        ModernWorldProtocol.Status.UNAVAILABLE,ModernWorldProtocol.Reason.NO_BINDING,null,null,null));
        }
        return new ModernApplicationProtocol.VipWorldResult(request.requestId(),request.request(),worlds.vipWorld(owner,request.request()));
    }
    ModernApplicationProtocol.VipEquipmentResult vipEquipment(ModernApplicationProtocol.VipEquipmentRequest request)throws IOException {
        var s=request.request().proof().life().player();
        synchronized(this) {
            requireOpen();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernVipWorldProtocol.negotiated(matchProtocol)||!ModernSessionExtensions.allowsVipEquipment(matchProtocol)
                    ||!worldsEnabled||!combatEnabled||worlds==null||vipEquipment==null||!vipEquipment.sourceEnabled(s.sourceId())
                    ||phase!=2||channel!=s.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        return equipment(s.sourceId(),s.objectId(),s.epoch(),(principal,selected,operation)->
                worlds.vipEquipment(owner,request,vipEquipment,principal,selected,operation));
    }
    ModernApplicationProtocol.BattleWorldResult battleWorld(ModernApplicationProtocol.BattleWorldRequest request)throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.request().scope();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!ModernBattleWorldProtocol.negotiated(matchProtocol)||!worldsEnabled||worlds==null||phase!=2||channel!=scope.channelId())
                throw new ModernApplicationProtocol.ProtocolException();
        }
        return new ModernApplicationProtocol.BattleWorldResult(request.requestId(),request.request(),worlds.battleWorld(owner,request.request()));
    }
    ModernWorldResourceProtocol.Result nativeWorld(ModernWorldResourceProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.world().scope();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!ModernWorldResourceProtocol.negotiated(matchProtocol)||!worldsEnabled||phase!=2||channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.nativeWorld(owner,request,matchProtocol);
    }
    ModernNativeCanonicalWorldProtocol.Result nativeCanonicalWorld(ModernNativeCanonicalWorldProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.world().scope();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!ModernNativeCanonicalWorldProtocol.negotiated(matchProtocol)||!worldsEnabled||worlds==null||phase!=2||channel!=scope.channelId())
                throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.nativeCanonicalWorld(owner,request,matchProtocol);
    }
    ModernNativeActorWorldProtocol.Result nativeActorWorld(ModernNativeActorWorldProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.catalogRequest().nativeRequest().world().scope();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!ModernNativeActorWorldProtocol.negotiated(matchProtocol)||!worldsEnabled||worlds==null||phase!=2||channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.nativeActorWorld(owner,request,matchProtocol);
    }
    ModernNativeCatalogWorldProtocol.Result nativeCatalogWorld(ModernNativeCatalogWorldProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.nativeRequest().world().scope();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!ModernNativeCatalogWorldProtocol.negotiated(matchProtocol)||!worldsEnabled||worlds==null||phase!=2||channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.nativeCatalogWorld(owner,request,matchProtocol);
    }
    ModernWorldProtocol.Reply world(ModernWorldProtocol.Request request) throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.scope();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!worldsEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.handle(owner,request);
    }

    /** Full fresh combat source and durable equipment overrides use the real selected owner's backend lease. */
    private ModernPhysicalCombat.Fighter freshPhysicalCharacter(ModernRooms.Scope scope, EquipmentSelection attachment,
            long expectedGeneration, long absoluteDeadlineNanos,boolean armorProtection) throws IOException {
        return freshPhysicalCharacter(scope,attachment,expectedGeneration,absoluteDeadlineNanos,armorProtection,false);
    }
    private ModernPhysicalCombat.Fighter freshPhysicalCharacter(ModernRooms.Scope scope, EquipmentSelection attachment,
            long expectedGeneration,long absoluteDeadlineNanos,boolean armorProtection,boolean fullVip)throws IOException {
        if (nativePlayerLoadout != null) {
            if (armorProtection || fullVip) throw new ModernApplicationProtocol.ProtocolException();
            return freshNativeMelee(scope, attachment, expectedGeneration, absoluteDeadlineNanos);
        }
        final ModernAccountAuthentication.Principal principal;
        final ModernApplicationProtocol.CharacterRow character;
        synchronized (this) {
            principal = requireOpen();
            requireEquipmentAttachment(attachment, expectedGeneration, scope.sourceId(), scope.objectId(), scope.epoch());
            if (!combatEnabled || phase != 2 || channel != scope.channelId()) throw new ModernApplicationProtocol.ProtocolException();
            if(fullVip&&(!ModernVipWorldProtocol.negotiated(matchProtocol)||vipEquipment==null||!vipEquipment.sourceEnabled(scope.sourceId())))throw new ModernApplicationProtocol.ProtocolException();
            character = selected;
        }
        long remaining = absoluteDeadlineNanos - System.nanoTime();
        if (remaining < java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50)) throw new IOException("combat preparation deadline");
        int millis = (int)Math.min(backendMillis, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var result = backend.call(millis, new ModernBackendExecutor.Work<ModernPhysicalCombat.Fighter>() {
            private ModernEquipmentLease.Operation operation;
            @Override public ModernPhysicalCombat.Fighter run(ModernBackendScope backendScope) throws Exception {
                synchronized (ModernNavigationSession.this) {
                    requireEquipmentAttachment(attachment, expectedGeneration, scope.sourceId(), scope.objectId(), scope.epoch());
                    if (!combatEnabled || phase != 2 || channel != scope.channelId()) throw new ModernApplicationProtocol.ProtocolException();
                }
                operation = attachment.lease.operation(backendScope);
                return fullVip?vipEquipment.prepareWorld(principal,character,operation,armorProtection,0)
                        :equipment.preparePhysicalForWorld(principal, character, operation,armorProtection);
            }
            @Override public void close() { if (operation != null) operation.close(); }
        });
        synchronized (this) {
            requireEquipmentAttachment(attachment, expectedGeneration, scope.sourceId(), scope.objectId(), scope.epoch());
            if (!combatEnabled || phase != 2 || channel != scope.channelId()) throw new ModernApplicationProtocol.ProtocolException();
        }
        return result;
    }
    /** Only an actual selected-owner registration can call this reader. Offline
     * LPL1 bytes are never accepted as session, selection or match authority. */
    private ModernPhysicalCombat.Fighter freshNativeMelee(ModernRooms.Scope scope, EquipmentSelection attachment,
            long expectedGeneration, long absoluteDeadlineNanos) throws IOException {
        final ModernAccountAuthentication.Principal principal;
        final ModernApplicationProtocol.CharacterRow character;
        synchronized (this) {
            principal = requireOpen();
            requireEquipmentAttachment(attachment, expectedGeneration, scope.sourceId(), scope.objectId(), scope.epoch());
            if (!combatEnabled || phase != 2 || channel != scope.channelId()) throw new ModernApplicationProtocol.ProtocolException();
            character = selected;
        }
        long remaining = absoluteDeadlineNanos - System.nanoTime();
        if (remaining < java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50)) throw new IOException("native melee preparation deadline");
        int millis = (int)Math.min(backendMillis, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var captured = backend.call(millis, new ModernBackendExecutor.Work<NativePlayerLoadoutAdapter.Captured>() {
            private ModernEquipmentLease.Operation operation;
            @Override public NativePlayerLoadoutAdapter.Captured run(ModernBackendScope backendScope) throws Exception {
                synchronized (ModernNavigationSession.this) {
                    requireEquipmentAttachment(attachment, expectedGeneration, scope.sourceId(), scope.objectId(), scope.epoch());
                    if (!combatEnabled || phase != 2 || channel != scope.channelId()) throw new ModernApplicationProtocol.ProtocolException();
                }
                operation = attachment.lease.operation(backendScope);
                var value = nativePlayerLoadout.capture(principal, character, scope, owner, operation);
                if (!value.fighter().supported()) throw new IOException("native melee source unsupported");
                return value;
            }
            @Override public void close() { if (operation != null) operation.close(); }
        });
        synchronized (this) {
            requireEquipmentAttachment(attachment, expectedGeneration, scope.sourceId(), scope.objectId(), scope.epoch());
            if (!combatEnabled || phase != 2 || channel != scope.channelId()
                    || System.nanoTime() - absoluteDeadlineNanos >= 0
                    || !captured.currentAt(System.currentTimeMillis())) throw new IOException("native melee publication retired");
            return captured.fighter();
        }
    }
    ModernApplicationProtocol.BattleActionResult action(ModernApplicationProtocol.BattleActionRequest request)throws IOException {
        requireBattleAction(request.request());return worlds.action(owner,request);
    }
    ModernApplicationProtocol.BattleRevivalResult revival(ModernApplicationProtocol.BattleRevivalRequest request)throws IOException {
        synchronized(this) {
            requireOpen();var s=request.request().proof().player();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernBattleRevivalProtocol.negotiated(matchProtocol)||!worldsEnabled||!combatEnabled
                    ||phase!=2||channel!=s.channelId()||worlds==null)throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.revival(owner,request);
    }
    ModernGreaterHasteProtocol.Reply greaterHaste(ModernGreaterHasteProtocol.Request request)throws IOException {
        var s=request.proof().player();
        synchronized(this) {
            requireOpen();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernGreaterHasteProtocol.negotiated(matchProtocol)||!worldsEnabled||!combatEnabled
                    ||phase!=2||channel!=s.channelId()||worlds==null||greaterHasteRepository==null)throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.greaterHaste(owner,request);
    }
    ModernHasteProtocol.Reply haste(ModernHasteProtocol.Request request)throws IOException {
        var s=request.proof().player();
        synchronized(this) {
            requireOpen();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernHasteProtocol.negotiated(matchProtocol)||!worldsEnabled||!combatEnabled
                    ||phase!=2||channel!=s.channelId()||worlds==null||hasteRepository==null)throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.haste(owner,request);
    }
    ModernSlowProtocol.Reply slow(ModernSlowProtocol.Request request)throws IOException {
        var s=request.proof().player();
        synchronized(this) {
            requireOpen();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernSlowProtocol.negotiated(matchProtocol)||!worldsEnabled||!combatEnabled
                    ||phase!=2||channel!=s.channelId()||worlds==null||slowRepository==null)throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.slow(owner,request);
    }
    ModernPlayerHealingProtocol.Reply playerHeal(ModernPlayerHealingProtocol.Request request)throws IOException {
        var s=request.proof().player();
        synchronized(this) {
            requireOpen();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernSessionExtensions.allowsCommands(matchProtocol)||!worldsEnabled||!combatEnabled
                    ||phase!=2||channel!=s.channelId()||worlds==null||equipment==null||!equipment.playerHealingSource(s.sourceId()))
                throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.playerHeal(owner,request);
    }
    ModernApplicationProtocol.BattleEquipmentResult battleEquipment(ModernApplicationProtocol.BattleEquipmentRequest request)throws IOException {
        var s=request.request().proof().player();
        synchronized(this) {
            requireOpen();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernBattleEquipmentProtocol.negotiated(matchProtocol)||!worldsEnabled||!combatEnabled
                    ||phase!=2||channel!=s.channelId()||worlds==null||equipment==null)
                throw new ModernApplicationProtocol.ProtocolException();
        }
        // Unknown backend failure may follow durable rename. Propagate/close;
        // never invent a rollback response or retry SET on this connection.
        return equipment(s.sourceId(),s.objectId(),s.epoch(),(principal,selected,operation)->
                worlds.equipment(owner,request,equipment,principal,selected,operation));
    }
    ModernApplicationProtocol.BattleOperationResult operation(ModernApplicationProtocol.BattleOperationRequest request)throws IOException {
        requireBattleAction(request.request());
        synchronized(this){if(!ModernBattleOperationProtocol.negotiated(matchProtocol))throw new ModernApplicationProtocol.ProtocolException();}
        return worlds.operation(owner,request);
    }
    private void requireBattleAction(ModernBattleActionProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var s=request.proof().player();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!ModernBattleActionProtocol.negotiated(matchProtocol)||!worldsEnabled||phase!=2||channel!=s.channelId()||worlds==null)
                throw new ModernApplicationProtocol.ProtocolException();
            var m=request.command();
            boolean spell=m instanceof ModernApplicationProtocol.CastRequest||m instanceof ModernApplicationProtocol.DisintegrateRequest
                    ||m instanceof ModernApplicationProtocol.NpcSpellRequest||m instanceof ModernApplicationProtocol.NpcDisintegrateRequest
                    ||m instanceof ModernApplicationProtocol.NpcFireStormRequest;
            if(!(m instanceof ModernApplicationProtocol.WorldRequest)&&!combatEnabled||spell&&!skillsEnabled)
                throw new ModernApplicationProtocol.ProtocolException();
            if(m instanceof ModernApplicationProtocol.ItemRequest||m instanceof ModernApplicationProtocol.AmmoRequest)requireShopScope(s);
            if(m instanceof ModernApplicationProtocol.AmmoRequest&&!bowEnabled)throw new ModernApplicationProtocol.ProtocolException();
            if(m instanceof ModernApplicationProtocol.CastRequest r&&(r.request().skillId()==1&&!honorEnabled||r.request().skillId()==70&&!fireStormEnabled))
                throw new ModernApplicationProtocol.ProtocolException();
            if((m instanceof ModernApplicationProtocol.DisintegrateRequest||m instanceof ModernApplicationProtocol.NpcDisintegrateRequest)&&disintegrateRepository==null)
                throw new ModernApplicationProtocol.ProtocolException();
            // Per-operation capability, catalog and match attachment checks
            // remain inside the shared canonical WorldService command path.
        }
    }
    ModernCombatProtocol.Reply combat(ModernCombatProtocol.Request request) throws IOException {
        synchronized (this) {
            requireOpen(); var scope = request.scope(); requireSelection(scope.sourceId(), scope.objectId(), scope.epoch());
            if (!combatEnabled || phase != 2 || channel != scope.channelId()) throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.combat(owner, request);
    }
    ModernNpcProtocol.Reply npc(ModernNpcProtocol.Request request) throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.player();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!combatEnabled||phase!=2||channel!=scope.channelId()||worlds==null||!worlds.npcEnabled()
                    ||matchProtocol==null||matchProtocol.version()<ModernNpcProtocol.RESOURCE_VERSION
                    ||(matchProtocol.capabilities()&ModernSessionProtocol.CAP_NPC)==0)throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.npc(owner,request);
    }
    ModernSiegeProtocol.Reply siege(ModernSiegeProtocol.Request request)throws IOException {
        synchronized(this){requireOpen();var scope=request.player();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!combatEnabled||phase!=2||channel!=scope.channelId()||worlds==null||matchProtocol==null||matchProtocol.version()<26
                    ||(matchProtocol.capabilities()&ModernSessionProtocol.CAP_MATCH_DESCRIPTOR)==0)throw new ModernApplicationProtocol.ProtocolException();}
        return worlds.siege(owner,request);
    }
    ModernNpcFireStormProtocol.Reply fireStormNpc(ModernNpcFireStormState.Request request)throws IOException {
        synchronized(this){
            requireOpen();var scope=request.player();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!combatEnabled||!skillsEnabled||phase!=2||channel!=scope.channelId()||worlds==null||!worlds.npcFireStormEnabled()
                    ||!ModernNpcFireStormProtocol.negotiated(matchProtocol))throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.fireStormNpc(owner,request);
    }
    ModernNpcDisintegrateProtocol.Reply disintegrateNpc(ModernNpcDisintegrateState.Request request)throws IOException {
        synchronized(this){
            requireOpen();var scope=request.player();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!combatEnabled||!skillsEnabled||phase!=2||channel!=scope.channelId()||worlds==null||!worlds.npcDisintegrateEnabled()
                    ||disintegrateRepository==null||!ModernNpcDisintegrateProtocol.negotiated(matchProtocol))throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.disintegrateNpc(owner,request);
    }
    ModernNpcSpellProtocol.Reply castNpc(ModernNpcSpellProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.player();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!combatEnabled||!skillsEnabled||phase!=2||channel!=scope.channelId()||worlds==null||!worlds.npcEnabled()
                    ||matchProtocol==null||matchProtocol.version()<24||(matchProtocol.capabilities()&ModernSessionProtocol.CAP_NPC)==0)
                throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.castNpc(owner,request);
    }

    /** Fresh immutable skill authority; same selected owner lease and absolute preparation deadline as the fighter. */
    private ModernDisintegrateCatalog.Snapshot freshDisintegrateCharacter(ModernRooms.Scope scope,EquipmentSelection attachment,
            long expectedGeneration,long absoluteDeadlineNanos)throws IOException {
        final ModernAccountAuthentication.Principal principal;
        synchronized(this){
            principal=requireOpen();requireDisintegrateAttachment(scope,attachment,expectedGeneration);
        }
        long remaining=absoluteDeadlineNanos-System.nanoTime();
        if(remaining<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50))throw new IOException("Disintegrate preparation deadline");
        int millis=(int)Math.min(backendMillis,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var result=backend.call(millis,new ModernBackendExecutor.Work<ModernDisintegrateCatalog.Snapshot>(){
            private ModernEquipmentLease.Operation operation;
            @Override public ModernDisintegrateCatalog.Snapshot run(ModernBackendScope backendScope)throws Exception {
                synchronized(ModernNavigationSession.this){requireDisintegrateAttachment(scope,attachment,expectedGeneration);}
                operation=attachment.lease.operation(backendScope);
                var value=disintegrateRepository.character(principal,scope.sourceId(),scope.objectId()).orElseThrow(()->new IOException("Disintegrate character unavailable"));
                if(!value.key().equals(new TournamentPresence.CharacterKey(scope.sourceId(),scope.objectId())))throw new IOException("Disintegrate source changed");
                return value;
            }
            @Override public void close(){if(operation!=null)operation.close();}
        });
        synchronized(this){requireDisintegrateAttachment(scope,attachment,expectedGeneration);}
        return result;
    }
    private void requireDisintegrateAttachment(ModernRooms.Scope scope,EquipmentSelection attachment,long expectedGeneration)throws IOException {
        requireOpen();requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
        if(!combatEnabled||!skillsEnabled||phase!=2||channel!=scope.channelId()||disintegrateRepository==null
                ||!(ModernDisintegrateProtocol.negotiated(matchProtocol)||ModernNpcDisintegrateProtocol.negotiated(matchProtocol)))throw new ModernApplicationProtocol.ProtocolException();
    }

    private ModernGreaterHasteCatalog.Snapshot freshGreaterHasteCharacter(ModernRooms.Scope scope,EquipmentSelection attachment,
            long expectedGeneration,long absoluteDeadlineNanos)throws IOException {
        final ModernAccountAuthentication.Principal principal;
        synchronized(this){principal=requireOpen();requireGreaterHasteAttachment(scope,attachment,expectedGeneration);}
        long remaining=absoluteDeadlineNanos-System.nanoTime();
        if(remaining<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50))throw new IOException("greater haste preparation deadline");
        int millis=(int)Math.min(backendMillis,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var result=backend.call(millis,new ModernBackendExecutor.Work<ModernGreaterHasteCatalog.Snapshot>(){
            private ModernEquipmentLease.Operation operation;
            @Override public ModernGreaterHasteCatalog.Snapshot run(ModernBackendScope backendScope)throws Exception {
                synchronized(ModernNavigationSession.this){requireGreaterHasteAttachment(scope,attachment,expectedGeneration);}
                operation=attachment.lease.operation(backendScope);
                var value=greaterHasteRepository.character(principal,scope.sourceId(),scope.objectId()).orElseThrow(()->new IOException("greater haste character unavailable"));
                if(!value.key().equals(new TournamentPresence.CharacterKey(scope.sourceId(),scope.objectId())))throw new IOException("greater haste source changed");
                return value;
            }
            @Override public void close(){if(operation!=null)operation.close();}
        });
        synchronized(this){requireGreaterHasteAttachment(scope,attachment,expectedGeneration);}
        return result;
    }
    private void requireGreaterHasteAttachment(ModernRooms.Scope scope,EquipmentSelection attachment,long expectedGeneration)throws IOException {
        requireOpen();requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
        if(!combatEnabled||!skillsEnabled||!timedEnabled||phase!=2||channel!=scope.channelId()||greaterHasteRepository==null
                ||!ModernGreaterHasteProtocol.negotiated(matchProtocol))throw new ModernApplicationProtocol.ProtocolException();
    }
    private ModernHasteCatalog.Snapshot freshHasteCharacter(ModernRooms.Scope scope,EquipmentSelection attachment,
            long expectedGeneration,long absoluteDeadlineNanos)throws IOException {
        final ModernAccountAuthentication.Principal principal;
        synchronized(this){principal=requireOpen();requireHasteAttachment(scope,attachment,expectedGeneration);}
        long remaining=absoluteDeadlineNanos-System.nanoTime();
        if(remaining<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50))throw new IOException("haste preparation deadline");
        int millis=(int)Math.min(backendMillis,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var result=backend.call(millis,new ModernBackendExecutor.Work<ModernHasteCatalog.Snapshot>(){
            private ModernEquipmentLease.Operation operation;
            @Override public ModernHasteCatalog.Snapshot run(ModernBackendScope backendScope)throws Exception {
                synchronized(ModernNavigationSession.this){requireHasteAttachment(scope,attachment,expectedGeneration);}
                operation=attachment.lease.operation(backendScope);
                var value=hasteRepository.character(principal,scope.sourceId(),scope.objectId()).orElseThrow(()->new IOException("haste character unavailable"));
                if(!value.key().equals(new TournamentPresence.CharacterKey(scope.sourceId(),scope.objectId())))throw new IOException("haste source changed");
                return value;
            }
            @Override public void close(){if(operation!=null)operation.close();}
        });
        synchronized(this){requireHasteAttachment(scope,attachment,expectedGeneration);}
        return result;
    }
    private void requireHasteAttachment(ModernRooms.Scope scope,EquipmentSelection attachment,long expectedGeneration)throws IOException {
        requireOpen();requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
        if(!combatEnabled||!skillsEnabled||!timedEnabled||phase!=2||channel!=scope.channelId()||hasteRepository==null
                ||!ModernHasteProtocol.negotiated(matchProtocol))throw new ModernApplicationProtocol.ProtocolException();
    }
    private ModernSlowCatalog.Snapshot freshSlowCharacter(ModernRooms.Scope scope,EquipmentSelection attachment,
            long expectedGeneration,long absoluteDeadlineNanos)throws IOException {
        final ModernAccountAuthentication.Principal principal;
        synchronized(this){principal=requireOpen();requireSlowAttachment(scope,attachment,expectedGeneration);}
        long remaining=absoluteDeadlineNanos-System.nanoTime();
        if(remaining<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50))throw new IOException("slow preparation deadline");
        int millis=(int)Math.min(backendMillis,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var result=backend.call(millis,new ModernBackendExecutor.Work<ModernSlowCatalog.Snapshot>(){
            private ModernEquipmentLease.Operation operation;
            @Override public ModernSlowCatalog.Snapshot run(ModernBackendScope backendScope)throws Exception {
                synchronized(ModernNavigationSession.this){requireSlowAttachment(scope,attachment,expectedGeneration);}
                operation=attachment.lease.operation(backendScope);
                var value=slowRepository.character(principal,scope.sourceId(),scope.objectId()).orElseThrow(()->new IOException("slow character unavailable"));
                if(!value.key().equals(new TournamentPresence.CharacterKey(scope.sourceId(),scope.objectId())))throw new IOException("slow source changed");
                return value;
            }
            @Override public void close(){if(operation!=null)operation.close();}
        });
        synchronized(this){requireSlowAttachment(scope,attachment,expectedGeneration);}
        return result;
    }
    private void requireSlowAttachment(ModernRooms.Scope scope,EquipmentSelection attachment,long expectedGeneration)throws IOException {
        requireOpen();requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
        if(!combatEnabled||!skillsEnabled||!timedEnabled||phase!=2||channel!=scope.channelId()||slowRepository==null
                ||!ModernSlowProtocol.negotiated(matchProtocol))throw new ModernApplicationProtocol.ProtocolException();
    }
    private ModernSkills.Snapshot freshSkillsCharacter(ModernRooms.Scope scope, EquipmentSelection attachment,
            long expectedGeneration, long absoluteDeadlineNanos) throws IOException {
        final ModernAccountAuthentication.Principal principal;
        final ModernApplicationProtocol.CharacterRow character;
        synchronized(this) {
            principal=requireOpen();requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
            if(!skillsEnabled || !combatEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
            character=selected;
        }
        long remaining=absoluteDeadlineNanos-System.nanoTime();
        if(remaining<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50))throw new IOException("skill preparation deadline");
        int millis=(int)Math.min(backendMillis,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        var result=backend.call(millis,new ModernBackendExecutor.Work<ModernSkills.Snapshot>() {
            private ModernEquipmentLease.Operation operation;
            @Override public ModernSkills.Snapshot run(ModernBackendScope backendScope)throws Exception {
                synchronized(ModernNavigationSession.this) {
                    requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
                    if(!skillsEnabled || !combatEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
                }
                operation=attachment.lease.operation(backendScope);
                var snapshot=skillsRepository.character(principal,scope.sourceId(),scope.objectId()).orElseThrow(()->new IOException("skill character unavailable"));
                validateSkillsSnapshot(snapshot,scope.sourceId(),character);
                return snapshot;
            }
            @Override public void close(){if(operation!=null)operation.close();}
        });
        synchronized(this) {
            requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());
            if(!skillsEnabled || !combatEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        }
        return result;
    }

    ModernApplicationProtocol.SkillsResult skills(ModernApplicationProtocol.SkillsRequest request)throws IOException {
        var context=request.context();
        final EquipmentSelection attachment;
        final long expectedGeneration;
        final boolean qualifyDisintegrate;
        final boolean qualifyGreaterHaste;
        final boolean qualifyHaste;
        final boolean qualifySlow;
        synchronized(this) {
            requireOpen();requireSelection(context.sourceId(),context.objectId(),context.epoch());
            if(!skillsEnabled)throw new ModernApplicationProtocol.ProtocolException();
            attachment=equipmentSelection;expectedGeneration=generation;
            qualifyDisintegrate=ModernDisintegrateProtocol.negotiated(matchProtocol)||ModernNpcDisintegrateProtocol.negotiated(matchProtocol);
            qualifyGreaterHaste=ModernGreaterHasteProtocol.negotiated(matchProtocol);
            qualifyHaste=ModernHasteProtocol.negotiated(matchProtocol);
            qualifySlow=ModernSlowProtocol.negotiated(matchProtocol);
        }
        ModernSpellProtocol.SkillsReply reply;
        try {
            reply=equipment(context.sourceId(),context.objectId(),context.epoch(),(principal,character,operation)-> {
                var snapshot=skillsRepository.character(principal,context.sourceId(),context.objectId());
                if(snapshot.isEmpty())return new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.DENIED,List.of());
                validateSkillsSnapshot(snapshot.orElseThrow(),context.sourceId(),character);
                var learned=snapshot.orElseThrow().skills();
                if(qualifyDisintegrate) {
                    checkCommitClock(operation);
                    var catalog=disintegrateRepository.character(principal,context.sourceId(),context.objectId()).orElseThrow(()->new IOException("Disintegrate catalog unavailable"));
                    checkCommitClock(operation);
                    learned=ModernDisintegrateSkills.qualify(snapshot.orElseThrow(),catalog);
                }
                if(qualifyGreaterHaste) {
                    checkCommitClock(operation);
                    var catalog=greaterHasteRepository.character(principal,context.sourceId(),context.objectId()).orElseThrow(()->new IOException("Greater Haste catalog unavailable"));
                    checkCommitClock(operation);
                    learned=ModernGreaterHasteSkills.qualify(new ModernSkills.Snapshot(context.sourceId(),character,learned),catalog);
                }
                if(qualifyHaste) {
                    checkCommitClock(operation);
                    var catalog=hasteRepository.character(principal,context.sourceId(),context.objectId()).orElseThrow(()->new IOException("Haste catalog unavailable"));
                    checkCommitClock(operation);
                    learned=ModernHasteSkills.qualify(new ModernSkills.Snapshot(context.sourceId(),character,learned),catalog);
                }
                if(qualifySlow) {
                    checkCommitClock(operation);
                    var catalog=slowRepository.character(principal,context.sourceId(),context.objectId()).orElseThrow(()->new IOException("Slow catalog unavailable"));
                    checkCommitClock(operation);
                    learned=ModernSlowSkills.qualify(new ModernSkills.Snapshot(context.sourceId(),character,learned),catalog);
                }
                var success=new ModernSpellProtocol.SkillsReply(ModernSpellProtocol.Status.OK,learned);
                byte[] checked=ModernApplicationProtocol.encode(new ModernApplicationProtocol.SkillsResult(request.requestId(),context,success),4);
                Arrays.fill(checked,(byte)0);return success;
            });
        }catch(ModernBackendExecutor.BackendException failure) {
            if(failure.failure()==ModernBackendExecutor.Failure.CANCELLED || failure.failure()==ModernBackendExecutor.Failure.CLOSED
                    || failure.failure()==ModernBackendExecutor.Failure.DEADLINE)throw failure;
            var status=failure.failure()==ModernBackendExecutor.Failure.BUSY || failure.failure()==ModernBackendExecutor.Failure.OVERLOADED
                    ?ModernSpellProtocol.Status.BUSY:ModernSpellProtocol.Status.UNAVAILABLE;
            reply=new ModernSpellProtocol.SkillsReply(status,List.of());
        }
        // Even generic backend replies may not be published into a newer selection.
        synchronized(this) {
            requireEquipmentAttachment(attachment,expectedGeneration,context.sourceId(),context.objectId(),context.epoch());
            if(!skillsEnabled)throw new ModernApplicationProtocol.ProtocolException();
        }
        return new ModernApplicationProtocol.SkillsResult(request.requestId(),context,reply);
    }
    ModernApplicationProtocol.RecordResult record(ModernApplicationProtocol.RecordRequest request) throws IOException {
        var context = request.context();
        final EquipmentSelection attachment;
        final long expectedGeneration;
        synchronized (this) {
            requireOpen(); requireSelection(context.sourceId(), context.objectId(), context.epoch());
            if (!recordsEnabled) throw new ModernApplicationProtocol.ProtocolException();
            attachment = equipmentSelection; expectedGeneration = generation;
        }
        ModernCharacterRecordProtocol.Reply reply;
        try {
            reply = equipment(context.sourceId(), context.objectId(), context.epoch(), (principal, character, operation) -> {
                var snapshot = recordRepository.character(principal, context.sourceId(), context.objectId());
                var value = new ModernCharacterRecordProtocol.Reply(context, ModernCharacterRecordProtocol.Status.OK, snapshot);
                byte[] checked = ModernApplicationProtocol.encode(new ModernApplicationProtocol.RecordResult(request.requestId(), value), 5);
                Arrays.fill(checked, (byte) 0);
                return value;
            });
        } catch (ModernBackendExecutor.BackendException failure) {
            if (failure.failure() == ModernBackendExecutor.Failure.CANCELLED
                    || failure.failure() == ModernBackendExecutor.Failure.CLOSED
                    || failure.failure() == ModernBackendExecutor.Failure.DEADLINE) throw failure;
            var status = failure.failure() == ModernBackendExecutor.Failure.BUSY
                    || failure.failure() == ModernBackendExecutor.Failure.OVERLOADED
                    ? ModernCharacterRecordProtocol.Status.BUSY : ModernCharacterRecordProtocol.Status.UNAVAILABLE;
            reply = new ModernCharacterRecordProtocol.Reply(context, status, null);
        }
        synchronized (this) {
            requireEquipmentAttachment(attachment, expectedGeneration, context.sourceId(), context.objectId(), context.epoch());
            if (!recordsEnabled) throw new ModernApplicationProtocol.ProtocolException();
        }
        return new ModernApplicationProtocol.RecordResult(request.requestId(), reply);
    }
    private static void validateSkillsSnapshot(ModernSkills.Snapshot snapshot,int source,
            ModernApplicationProtocol.CharacterRow character)throws IOException {
        if(snapshot==null || snapshot.sourceId()!=source || !snapshot.character().equals(character))throw new IOException("skill character changed");
    }
    ModernSpellProtocol.Reply cast(ModernSpellProtocol.Request request)throws IOException {
        synchronized(this) {
            requireOpen();var scope=request.scope();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
            if(!skillsEnabled || !combatEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
            if(request.skillId()==1&&request.targetEntityId()!=0&&!honorEnabled)throw new ModernApplicationProtocol.ProtocolException();
            if(request.skillId()==70&&!fireStormEnabled)throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.cast(owner,request);
    }
    ModernDisintegrateProtocol.Reply disintegrate(ModernDisintegrateState.Request request)throws IOException {
        synchronized(this){
            requireOpen();var s=request.scope();requireSelection(s.sourceId(),s.objectId(),s.epoch());
            if(!combatEnabled||!skillsEnabled||phase!=2||channel!=s.channelId()||worlds==null||disintegrateRepository==null
                    ||!ModernDisintegrateProtocol.negotiated(matchProtocol))throw new ModernApplicationProtocol.ProtocolException();
        }
        return worlds.disintegrate(owner,request);
    }

    private ModernApplicationProtocol.NavigationResult select(ModernApplicationProtocol.CharacterSelection request)
            throws IOException {
        final ModernAccountAuthentication.Principal principal;
        final long expectedGeneration;
        final TournamentPresence.SelectionTicket selectionTicket;
        synchronized (this) {
            principal = requireOpen();
            if (phase != 0 || selecting || request.sourceId() != rosterSource || request.selectionEpoch() != rosterEpoch) {
                throw new ModernApplicationProtocol.ProtocolException();
            }
            if (!principal.authorizesSource(request.sourceId()) || !rosterObjects.contains(request.objectId())) {
                return result(request.requestId(), 9, 1, request.sourceId(), request.objectId(), request.selectionEpoch());
            }
            selectionTicket = presence.selectionTicket(owner);
            if (selectionTicket == null) throw new ModernApplicationProtocol.ProtocolException();
            selecting = true; expectedGeneration = ++generation;
        }
        ModernApplicationProtocol.CharacterRow qualified = null;
        int status = 0;
        try {
            // One bounded operation, exact source-owned canonical account. An
            // old roster page is not authority to enter a reassigned/deleted row.
            var fresh = backend.call(backendMillis, scope -> {
                var rows = List.copyOf(characters.characters(principal, request.sourceId()));
                byte[] validated = ModernApplicationProtocol.encode(new ModernApplicationProtocol.CharacterResult(
                        request.requestId(), request.sourceId(), request.selectionEpoch(), 0, rows));
                Arrays.fill(validated, (byte) 0);
                return rows;
            });
            qualified = fresh.stream().filter(row -> row.objectId() == request.objectId()).findFirst().orElse(null);
            if (qualified == null) status = 1;
        } catch (ModernBackendExecutor.BackendException failure) {
            if (failure.failure() == ModernBackendExecutor.Failure.CANCELLED
                    || failure.failure() == ModernBackendExecutor.Failure.CLOSED
                    || failure.failure() == ModernBackendExecutor.Failure.DEADLINE) throw failure;
            status = failure.failure() == ModernBackendExecutor.Failure.OVERLOADED
                    || failure.failure() == ModernBackendExecutor.Failure.BUSY ? 3 : 2;
        } catch (Exception ignored) { status = 2; }
        synchronized (this) {
            requireOpen();
            if (generation != expectedGeneration || phase != 0 || !selecting) {
                throw new ModernApplicationProtocol.ProtocolException();
            }
            selecting = false;
            if (status == 0) {
                String sourceName = principal.sources().stream().filter(source -> source.id() == request.sourceId())
                        .findFirst().orElseThrow(ModernApplicationProtocol.ProtocolException::new).name();
                var choice = new TournamentPresence.Selection(
                        new TournamentPresence.CharacterKey(request.sourceId(), qualified.objectId()), sourceName,
                        qualified.name(), qualified.level(), qualified.characterClass(), qualified.sex());
                // A complete native deletion can fit between the fresh DB read
                // and this claim. The shared ticket covers both begin and end,
                // including a query that started before the deletion committed.
                var admitted = presence.select(owner, choice, selectionTicket);
                if (admitted == TournamentPresence.SelectStatus.CLOSED) throw new ModernApplicationProtocol.ProtocolException();
                if (admitted == TournamentPresence.SelectStatus.BUSY) status = 3;
                else {
                    var retained = presence.retainSelection(owner, choice.key());
                    if (retained == null) {
                        presence.returnToCharacters(owner);
                        throw new ModernApplicationProtocol.ProtocolException();
                    }
                    try {
                        equipmentSelection = new EquipmentSelection(equipmentIssuer.open(owner, choice.key(),
                                request.selectionEpoch(), retained::close));
                    } catch (RuntimeException failure) {
                        try { retained.close(); } finally { presence.returnToCharacters(owner); }
                        throw failure;
                    }
                    selected = qualified; selectedSource = request.sourceId(); selectedEpoch = request.selectionEpoch();
                    if(shopEnabled)shop=new ModernShopAuthority(shopAllowance,selectedSource,qualified,principal.accountForSource(selectedSource),timedEnabled,bowEnabled,ModernSlowProtocol.negotiated(matchProtocol));
                    selectedIdentity = choice;
                    phase = 1;
                }
            }
            return result(request.requestId(), 9, status, request.sourceId(), request.objectId(), request.selectionEpoch());
        }
    }

    private void requireSelection(int sourceId, long objectId, long epoch) throws IOException {
        if (phase == 0 || selected == null || selecting || sourceId != selectedSource
                || objectId != selected.objectId() || epoch != selectedEpoch || equipmentSelection == null
                || !equipmentIssuer.owns(equipmentSelection.lease)
                || !equipmentSelection.lease.matches(owner, new TournamentPresence.CharacterKey(sourceId, objectId), epoch)) {
            throw new ModernApplicationProtocol.ProtocolException();
        }
    }

    private void requireOutsideRoom() throws IOException {
        if(roomsEnabled && rooms.hasRoom(owner))throw new ModernApplicationProtocol.ProtocolException();
    }

    synchronized ModernApplicationProtocol.RoomResult room(ModernApplicationProtocol.RoomRequest request) throws IOException {
        requireOpen();var command=request.command();var scope=command.scope();
        requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!roomsEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        // The authority performs only bounded in-memory work. It never calls
        // back into navigation/presence, JDBC or a socket under this monitor.
        var result=rooms.execute(owner,command);
        if(worlds!=null&&command.action()!=ModernRooms.Action.READ&&result.status()==ModernRooms.Status.OK)worlds.retireNativeDiscovery(owner);
        return new ModernApplicationProtocol.RoomResult(request.requestId(),command,result);
    }

    /** Every invocation re-reads the selected source; the wire carries no grant or executor name. */
    ModernGmCommandProtocol.Reply gm(ModernGmCommandProtocol.Request request) throws IOException {
        synchronized (this) { requireGmScope(request.scope()); }
        ModernGmCommandProtocol.Reply reply;
        try {
            var context = request.scope();
            reply = equipment(context.sourceId(),context.objectId(),context.epoch(),(principal,character,operation) -> {
                ModernGmCommands.Snapshot fresh;
                try {
                    var found = Objects.requireNonNull(gmRepository.character(principal,context.sourceId(),context.objectId()));
                    fresh = found.orElse(null);
                    if (fresh != null && (fresh.sourceId() != context.sourceId()
                            || !fresh.canonicalAccount().equals(principal.accountForSource(context.sourceId()))
                            || !fresh.character().equals(character))) throw new IOException("GM source qualification failed");
                } catch (Exception failure) {
                    // This catch precedes any admission/mutation. Cancellation
                    // and deadline still win over a generic repository failure.
                    operation.scope().checkActive();
                    if (failure instanceof ModernBackendExecutor.BackendException backendFailure) {
                        if (backendFailure.failure() == ModernBackendExecutor.Failure.CANCELLED
                                || backendFailure.failure() == ModernBackendExecutor.Failure.CLOSED
                                || backendFailure.failure() == ModernBackendExecutor.Failure.DEADLINE) throw backendFailure;
                        if (backendFailure.failure() == ModernBackendExecutor.Failure.BUSY
                                || backendFailure.failure() == ModernBackendExecutor.Failure.OVERLOADED)
                            return ModernGmCommandProtocol.Reply.busy();
                    }
                    return ModernGmCommandProtocol.Reply.unavailable();
                }
                // checkActive/tryCommit may abort JDBC. They must remain OUTSIDE
                // navigation's monitor. The repository has already closed its
                // owned resources. Admission pins the selected key until Work.close.
                operation.scope().checkActive();
                if (!operation.tryCommit()) throw new ModernApplicationProtocol.ProtocolException();
                synchronized (ModernNavigationSession.this) {
                    requireGmScope(context);
                    if (selected != character) throw new ModernApplicationProtocol.ProtocolException();
                    // The room authority atomically rechecks actual membership,
                    // full scope, expected revision, waiting state and fresh grant.
                    // No database/socket callback is made under either monitor.
                    synchronized (rooms) {
                        // Pure reads only under Nav -> Rooms. A delayed lock
                        // acquisition cannot turn an expired database grant
                        // into a mutation; cancellation itself performs I/O elsewhere.
                        var cancelled = operation.scope().cancellation();
                        if (cancelled != null) throw new ModernBackendExecutor.BackendException(cancelled);
                        if (System.nanoTime() - operation.scope().deadlineNanos() >= 0)
                            throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.DEADLINE);
                        return ModernGmCommandProtocol.Reply.result(rooms.spawnBots(owner,context,request.roomId(),
                                request.expectedRevision(),request.count(),fresh));
                    }
                }
            });
        } catch (ModernBackendExecutor.BackendException failure) {
            if (failure.failure() != ModernBackendExecutor.Failure.BUSY
                    && failure.failure() != ModernBackendExecutor.Failure.OVERLOADED) throw failure;
            reply = ModernGmCommandProtocol.Reply.busy();
        }
        synchronized (this) { requireGmScope(request.scope()); return reply; }
    }

    private void requireGmScope(ModernRooms.Scope scope) throws IOException {
        requireOpen(); requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if (!gmEnabled || !roomsEnabled || phase != 2 || channel != scope.channelId() || !rooms.hasRoom(owner))
            throw new ModernApplicationProtocol.ProtocolException();
    }

    ModernItemProtocol.Reply item(ModernItemProtocol.Request request)throws IOException {
        synchronized(this){requireShopScope(request.scope());}
        return worlds.item(owner,request);
    }
    ModernAmmoProtocol.Reply ammo(ModernAmmoProtocol.Request request)throws IOException {
        synchronized(this){requireShopScope(request.scope());if(!bowEnabled)throw new ModernApplicationProtocol.ProtocolException();}
        return worlds.ammo(owner,request);
    }
    private void requireShopScope(ModernRooms.Scope scope)throws IOException {
        requireOpen();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!shopEnabled || shop==null || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
    }
    private ModernRooms.Result readRooms(ModernRooms.Scope scope) {
        return rooms.execute(owner,new ModernRooms.Command(scope,ModernRooms.Action.READ,0,0,0,null,""));
    }
    ModernShopProtocol.Reply shop(ModernShopProtocol.Request request)throws IOException {
        synchronized(this){requireShopScope(request.scope());}
        ModernShopProtocol.Reply reply;
        try {
            var scope=request.scope();
            reply=equipment(scope.sourceId(),scope.objectId(),scope.epoch(),(principal,character,operation)->{
                final ModernShopCatalog.Snapshot fresh;
                final ModernInventoryWeight.Basis basis;
                try {
                    basis=freshShopWeight(principal,character,operation,scope);
                    fresh=shopRepository.character(principal,scope.sourceId(),scope.objectId()).orElseThrow(()->new IOException("shop character unavailable"));
                    if(fresh.sourceId()!=scope.sourceId() || !fresh.character().equals(character)
                            || !fresh.canonicalAccount().equals(principal.accountForSource(scope.sourceId())))throw new IOException("shop source mismatch");
                } catch(Exception failure) {
                    operation.scope().checkActive();
                    if(failure instanceof ModernBackendExecutor.BackendException backendFailure) {
                        if(backendFailure.failure()==ModernBackendExecutor.Failure.BUSY || backendFailure.failure()==ModernBackendExecutor.Failure.OVERLOADED)
                            return ModernShopProtocol.Reply.exceptional(ModernShopAuthority.Status.BUSY,ModernShopAuthority.Reason.BUSY);
                        if(backendFailure.failure()==ModernBackendExecutor.Failure.CANCELLED || backendFailure.failure()==ModernBackendExecutor.Failure.CLOSED
                                || backendFailure.failure()==ModernBackendExecutor.Failure.DEADLINE)throw backendFailure;
                    }
                    return ModernShopProtocol.Reply.exceptional(ModernShopAuthority.Status.UNAVAILABLE,ModernShopAuthority.Reason.BACKEND);
                }
                operation.scope().checkActive();if(!operation.tryCommit())throw new ModernApplicationProtocol.ProtocolException();
                synchronized(ModernNavigationSession.this) {
                    requireShopScope(scope);if(selected!=character)throw new ModernApplicationProtocol.ProtocolException();
                    synchronized(rooms) {
                        checkCommitClock(operation);
                        if(rooms.isPreparing(owner) || shop.frozen())return ModernShopProtocol.Reply.exceptional(ModernShopAuthority.Status.FROZEN,ModernShopAuthority.Reason.FROZEN);
                        var current=readRooms(scope);var joined=current.view().joined();
                        if(current.status()!=ModernRooms.Status.OK)throw new ModernApplicationProtocol.ProtocolException();
                        long roomId=joined==null?0:joined.summary().id(),roomRev=joined==null?0:joined.summary().revision();
                        if(roomId!=request.roomId() || roomRev!=request.expectedRoomRevision())
                            return new ModernShopProtocol.Reply(shop.conflict(fresh,ModernShopAuthority.Reason.ROOM_CHANGED,basis),current);
                        if(!rooms.unreadyForActivity(owner))return ModernShopProtocol.Reply.exceptional(ModernShopAuthority.Status.FROZEN,ModernShopAuthority.Reason.FROZEN);
                        var result=shop.apply(request.action(),request.roundId(),request.expectedRevision(),request.token(),request.itemId(),request.quantity(),fresh,basis);
                        return new ModernShopProtocol.Reply(result,readRooms(scope));
                    }
                }
            });
        } catch(ModernBackendExecutor.BackendException failure) {
            if(failure.failure()!=ModernBackendExecutor.Failure.BUSY && failure.failure()!=ModernBackendExecutor.Failure.OVERLOADED)throw failure;
            reply=ModernShopProtocol.Reply.exceptional(ModernShopAuthority.Status.BUSY,ModernShopAuthority.Reason.BUSY);
        }
        synchronized(this){requireShopScope(request.scope());return reply;}
    }
    private List<ModernShopAuthority.Stack> captureShop(ModernRooms.Scope scope,EquipmentSelection attachment,long expectedGeneration,
            ModernShopAuthority expectedShop,long match,long matchGeneration,long deadline,ModernInventoryWeight.Basis expectedBasis)throws IOException {
        return captureShop(scope,attachment,expectedGeneration,expectedShop,match,matchGeneration,deadline,expectedBasis,null);
    }
    private List<ModernShopAuthority.Stack> captureShop(ModernRooms.Scope scope,EquipmentSelection attachment,long expectedGeneration,
            ModernShopAuthority expectedShop,long match,long matchGeneration,long deadline,ModernInventoryWeight.Basis expectedBasis,ModernMatchDescriptor descriptor)throws IOException {
        long remaining=deadline-System.nanoTime();
        if(remaining<java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(50))throw new IOException("shop preparation deadline");
        int millis=(int)Math.min(backendMillis,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
        return equipment(scope.sourceId(),scope.objectId(),scope.epoch(),millis,(principal,character,operation)->{
            synchronized(ModernNavigationSession.this){requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());requireShopScope(scope);if(shop!=expectedShop)throw new ModernApplicationProtocol.ProtocolException();}
            var basis=freshShopWeight(principal,character,operation,scope);
            if(descriptor==null || descriptor.has(ModernMatchDescriptor.WEIGHT)) {
                if((basis==null)!=(expectedBasis==null) || basis!=null&&!basis.sameFacts(expectedBasis))throw new IOException("world weight source changed");
            }
            var fresh=shopRepository.character(principal,scope.sourceId(),scope.objectId()).orElseThrow(()->new IOException("shop preparation unavailable"));
            operation.scope().checkActive();if(!operation.tryCommit())throw new ModernApplicationProtocol.ProtocolException();
            synchronized(ModernNavigationSession.this){
                requireEquipmentAttachment(attachment,expectedGeneration,scope.sourceId(),scope.objectId(),scope.epoch());requireShopScope(scope);
                if(shop!=expectedShop)throw new ModernApplicationProtocol.ProtocolException();
                synchronized(rooms){checkCommitClock(operation);if(!rooms.isPreparing(owner))throw new ModernApplicationProtocol.ProtocolException();
                    return expectedShop.capture(match,matchGeneration,deadline,fresh,basis,descriptor);}
            }
        });
    }
    private ModernInventoryWeight.Basis freshShopWeight(ModernAccountAuthentication.Principal principal,
            ModernApplicationProtocol.CharacterRow character,ModernEquipmentLease.Operation operation,ModernRooms.Scope scope)throws Exception {
        final Double rate;
        synchronized(this){requireShopScope(scope);rate=weightEnabled?worlds.weightRate(scope.sourceId()):null;}
        if(rate==null)return null;
        var result=equipment.execute(principal,character,operation,new ModernEquipmentAuthority.Request(scope.sourceId(),scope.objectId(),
                scope.epoch(),ModernEquipmentAuthority.READ,false,0,0,ModernEquipmentAuthority.ZERO),rate);
        if(result.status()!=ModernEquipmentAuthority.OK || result.snapshot()==null || !result.snapshot().source().character().equals(character))
            throw new IOException("shop equipment unavailable");
        var basis=ModernInventoryWeight.qualify(result.snapshot(),rate);
        operation.restrictCommit(result.snapshot().evaluatedAtMillis(),basis.validUntilMillis(),System::currentTimeMillis);
        return basis;
    }
    private static void checkCommitClock(ModernEquipmentLease.Operation operation)throws ModernBackendExecutor.BackendException {
        var cancelled=operation.scope().cancellation();if(cancelled!=null)throw new ModernBackendExecutor.BackendException(cancelled);
        if(System.nanoTime()-operation.scope().deadlineNanos()>=0)throw new ModernBackendExecutor.BackendException(ModernBackendExecutor.Failure.DEADLINE);
    }

    synchronized boolean prepareEquipmentMutation(ModernEquipmentAuthority.Request request) throws IOException {
        requireOpen();requireSelection(request.sourceId(),request.objectId(),request.epoch());
        return request.action()!=ModernEquipmentAuthority.SET || !roomsEnabled || rooms.unreadyForActivity(owner);
    }

    /** Server-owned preparation entry: never mints identity from a supplied key or bypasses the live lease. */
    synchronized ModernRooms.PreparationResult prepareWorld(ModernRooms.Scope scope,long roomId,long expectedRevision) throws IOException {
        requireOpen();requireSelection(scope.sourceId(),scope.objectId(),scope.epoch());
        if(!roomsEnabled || phase!=2 || channel!=scope.channelId())throw new ModernApplicationProtocol.ProtocolException();
        return rooms.prepareWorld(owner,scope,roomId,expectedRevision);
    }

    /** Trusted domain action, never supplied by the network or used to mint a selection. */
    @FunctionalInterface interface EquipmentAction<T> {
        T run(ModernAccountAuthentication.Principal principal, ModernApplicationProtocol.CharacterRow selected,
              ModernEquipmentLease.Operation operation) throws Exception;
    }

    /**
     * Equipment domain entry after authenticated SELECT. Captures the opaque
     * selection attachment, NOT just client-provided numeric key/epoch. This
     * method owns Operation until the actual BackendExecutor.Work.close; the
     * action may prepare/evaluate/store but must never close/retain Operation.
     * Its owned JDBC/file resources must actually close before run returns or
     * throws; never hand them to uncounted asynchronous work. Scope.finish is
     * an executor safety net, not permission for a domain action to leak them.
     * No file/JDBC IO occurs while holding navigation/presence/lease monitors.
     * This is not yet an equipment wire handler or a persisted-state authority.
     */
    <T> T equipment(int sourceId, long objectId, long epoch, EquipmentAction<T> action) throws IOException {
        return equipment(sourceId,objectId,epoch,backendMillis,action);
    }
    private <T> T equipment(int sourceId,long objectId,long epoch,int timeoutMillis,EquipmentAction<T> action)throws IOException {
        Objects.requireNonNull(action);
        final ModernAccountAuthentication.Principal principal;
        final ModernApplicationProtocol.CharacterRow character;
        final EquipmentSelection attachment;
        final long expectedGeneration;
        synchronized (this) {
            principal = requireOpen(); requireSelection(sourceId, objectId, epoch);
            attachment = equipmentSelection; character = selected; expectedGeneration = generation;
        }
        T value = backend.call(timeoutMillis, new ModernBackendExecutor.Work<T>() {
            private ModernEquipmentLease.Operation operation;
            @Override public T run(ModernBackendScope scope) throws Exception {
                synchronized (ModernNavigationSession.this) {
                    requireEquipmentAttachment(attachment, expectedGeneration, sourceId, objectId, epoch);
                }
                // scope.checkActive inside operation() can abort owned JDBC;
                // keep it outside navigation's monitor even before a query.
                operation = attachment.lease.operation(scope);
                return action.run(principal, character, operation);
            }
            @Override public void close() { if (operation != null) operation.close(); }
        });
        synchronized (this) {
            requireEquipmentAttachment(attachment, expectedGeneration, sourceId, objectId, epoch);
            return value;
        }
    }

    private void requireEquipmentAttachment(EquipmentSelection attachment, long expectedGeneration,
                                            int sourceId, long objectId, long epoch) throws IOException {
        requireOpen(); requireSelection(sourceId, objectId, epoch);
        if (generation != expectedGeneration || equipmentSelection != attachment) {
            throw new ModernApplicationProtocol.ProtocolException();
        }
    }

    ModernApplicationProtocol.CharacterDataResult characterData(ModernApplicationProtocol.CharacterDataRequest request,
            ModernCharacterDataRepository repository) throws IOException {
        final ModernAccountAuthentication.Principal principal;
        final long expectedGeneration;
        synchronized (this) {
            principal = requireOpen();
            requireSelection(request.sourceId(), request.objectId(), request.selectionEpoch());
            expectedGeneration = generation;
        }
        ModernApplicationProtocol.CharacterDataResult result;
        try {
            result = backend.call(backendMillis, scope -> {
                var snapshot = repository.character(principal, request.sourceId(), request.objectId());
                var response = new ModernApplicationProtocol.CharacterDataResult(request.requestId(), request.sourceId(),
                        request.objectId(), request.selectionEpoch(), snapshot.isPresent() ? 0 : 1, snapshot.orElse(null));
                byte[] validated = ModernApplicationProtocol.encode(response);
                Arrays.fill(validated, (byte) 0);
                return response;
            });
        } catch (ModernBackendExecutor.BackendException failure) {
            if (failure.failure() == ModernBackendExecutor.Failure.CANCELLED
                    || failure.failure() == ModernBackendExecutor.Failure.CLOSED
                    || failure.failure() == ModernBackendExecutor.Failure.DEADLINE) throw failure;
            int status = failure.failure() == ModernBackendExecutor.Failure.OVERLOADED
                    || failure.failure() == ModernBackendExecutor.Failure.BUSY ? 3 : 2;
            result = new ModernApplicationProtocol.CharacterDataResult(request.requestId(), request.sourceId(),
                    request.objectId(), request.selectionEpoch(), status, null);
        } catch (Exception ignored) {
            result = new ModernApplicationProtocol.CharacterDataResult(request.requestId(), request.sourceId(),
                    request.objectId(), request.selectionEpoch(), 2, null);
        }
        synchronized (this) {
            requireOpen();
            requireSelection(request.sourceId(), request.objectId(), request.selectionEpoch());
            if (generation != expectedGeneration) throw new ModernApplicationProtocol.ProtocolException();
            return result;
        }
    }
    private ModernAccountAuthentication.Principal requireOpen() throws IOException {
        if (closed) throw new ModernApplicationProtocol.ProtocolException();
        return transport.authenticatedPrincipal().orElseThrow(ModernApplicationProtocol.ProtocolException::new);
    }
    private ModernApplicationProtocol.NavigationResult result(int requestId, int action, int status,
                                                                int sourceId, long objectId, long epoch) {
        List<ModernApplicationProtocol.ChannelRow> channels = phase == 0 ? List.of()
                : presence.snapshots().stream().map(row -> new ModernApplicationProtocol.ChannelRow(
                        row.id(), row.name(), row.users(), row.capacity(), row.minimumLevel(), row.maximumLevel())).toList();
        return new ModernApplicationProtocol.NavigationResult(requestId, action, status, phase, channel,
                sourceId, objectId, epoch, selected, channels);
    }
    @Override public void close() {
        EquipmentSelection attachment;
        synchronized (this) {
            if (closed) { if (cleanupFailed) throw cleanupFailure(); return; }
            closed = true; generation++; selecting = false;
            matchmakingLobby=null;
            attachment = equipmentSelection; equipmentSelection = null;
            if(shop!=null){shop.close();shop=null;}
            selected = null; selectedSource = 0; selectedEpoch = 0; phase = 0; channel = 255;
            selectedIdentity = null;
            rosterObjects = Set.of(); rosterSource = 0; rosterEpoch = 0;
        }
        // A committed worker may still be on disk. Invalidation never waits;
        // Presence retains its key/alias until the Work.close callback retires.
        try {
            if (attachment != null) {
                attachment.lease.invalidate();
                if (attachment.lease.retirementFailed()) throw cleanupFailure();
            }
        } catch (RuntimeException failure) { cleanupFailed = true; throw cleanupFailure(); }
        finally {
            try { if(worlds!=null)worlds.detach(owner); }
            catch(RuntimeException failure) {cleanupFailed=true;throw cleanupFailure();}
            finally {
                try {if(rooms!=null)rooms.disconnect(owner);}
                catch(RuntimeException failure){cleanupFailed=true;throw cleanupFailure();}
                finally {presence.disconnect(owner);}
            }
        }
    }
    private static IllegalStateException cleanupFailure() {
        return new IllegalStateException("modern navigation owner cleanup failed");
    }
}
