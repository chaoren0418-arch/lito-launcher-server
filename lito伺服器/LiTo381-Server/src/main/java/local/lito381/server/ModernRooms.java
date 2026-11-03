package local.lito381.server;

import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Authoritative modern WAITING rooms. Registration is a trusted navigation-adapter
 * operation following a qualified selection and channel claim, not a wire credential.
 * Native GameHub rooms remain a separate registry; only pure admission/rule policy
 * is shared. Bots are explicit server-owned participants, never fake sessions or
 * database characters. No countdown, world execution or network I/O exists here.
 */
final class ModernRooms {
    static final int MAX_ROOMS = 100;
    static final int PRIVATE = 4, ALLOW_UPPER_TIER = 16, EQUAL_SIDES = 32;
    enum Action { READ, CREATE, JOIN, LEAVE, TEAM, READY, CONFIGURE }
    enum Status { OK, DENIED, CONFLICT, FULL, UNAVAILABLE }
    enum Reason {
        NONE, INVALID_STATE, LEVEL_RESTRICTED, ROOM_NOT_FOUND, BAD_PASSWORD,
        NOT_MEMBER, NOT_OWNER, REVISION_MISMATCH, CAPACITY, INVALID_CONFIGURATION, REVISION_EXHAUSTED, GM_NOT_AUTHORIZED
    }
    record Scope(int sourceId, long objectId, long epoch, int channelId) {
        Scope {
            if (sourceId <= 0 || objectId <= 0 || epoch == 0 || channelId < 0 || channelId > 3)
                throw invalid();
        }
    }
    record Draft(String name, String password, int capacity, int mode, int mapId, int attributes) {
        Draft {
            text(name, 64, false); password = password == null ? "" : password; text(password, 64, true);
            validateConfig(capacity, mode, mapId, attributes, false);
        }
        Config publicConfig() {
            return new Config(name, capacity, mode, mapId, attributes | (password.isEmpty() ? 0 : PRIVATE));
        }
        @Override public String toString() { return "RoomDraft[redacted]"; }
    }
    record Command(Scope scope, Action action, long roomId, long expectedRevision,
                   int value, Draft draft, String password) {
        Command {
            if (scope == null || action == null) throw invalid();
            password = password == null ? "" : password; text(password, 64, true);
            if (action == Action.READ || action == Action.CREATE) {
                if (roomId != 0 || expectedRevision != 0) throw invalid();
            } else if (roomId <= 0 || expectedRevision < 1) throw invalid();
            if (action == Action.TEAM || action == Action.READY) {
                if (value != 0 && value != 1) throw invalid();
            } else if (value != 0) throw invalid();
            if ((action == Action.CREATE || action == Action.CONFIGURE) != (draft != null)) throw invalid();
            if (action != Action.JOIN && !password.isEmpty()) throw invalid();
        }
        @Override public String toString() { return "RoomCommand[redacted]"; }
    }
    record Config(String name, int capacity, int mode, int mapId, int attributes) {
        Config { text(name, 64, false); validateConfig(capacity, mode, mapId, attributes, true); }
    }
    record Summary(long id, long revision, Config config, int users, int ready, boolean readySatisfied) {
        Summary {
            Objects.requireNonNull(config);
            if (id <= 0 || revision < 1 || users < 1 || users > capacity(config)
                    || ready < 0 || ready > users || (readySatisfied && ready != users)) throw invalid();
        }
    }
    record Member(ModernParticipantId identity, String name, String sourceName,
                  long level, int characterClass, int sex, int team, boolean ready) {
        Member(TournamentPresence.CharacterKey key,String name,String sourceName,long level,int characterClass,int sex,int team,boolean ready) {
            this(ModernParticipantId.human(key),name,sourceName,level,characterClass,sex,team,ready);
        }
        Member {
            Objects.requireNonNull(identity); text(name, 256, false); text(sourceName, 256, false);
            if (level < 1 || characterClass < 0 || characterClass > 6
                    || sex < 0 || sex > 1 || team < 0 || team > 1) throw invalid();
        }
        /** Compatibility for human-only callers; never silently projects a bot into SQL identity. */
        TournamentPresence.CharacterKey key(){return identity.humanKey();}
    }
    private static final Comparator<TournamentPresence.CharacterKey> KEY_ORDER = Comparator
            .comparingInt(TournamentPresence.CharacterKey::sourceId)
            .thenComparingLong(TournamentPresence.CharacterKey::objectId);
    record RoomSnapshot(Summary summary, TournamentPresence.CharacterKey owner, List<Member> members) {
        RoomSnapshot {
            Objects.requireNonNull(summary); Objects.requireNonNull(owner); members = List.copyOf(members);
            if (members.size() != summary.users()) throw invalid();
            ModernParticipantId previous = null; int ready = 0, first = 0; boolean ownerFound = false;
            for (Member member : members) {
                if (previous != null && previous.compareTo(member.identity()) >= 0) throw invalid();
                previous = member.identity(); ownerFound |= member.identity().equals(ModernParticipantId.human(owner));
                if (member.ready()) ready++; if (member.team() == 0) first++;
            }
            if (!ownerFound || ready != summary.ready() || summary.readySatisfied() != readiness(
                    summary.config(), members.size(), ready, first)) throw invalid();
        }
    }
    record Option(int mode, int mapId, int minimumPlayers, int timeLimitSeconds,
                  boolean teamBased, String modeName, boolean worldAvailable, int maximumPlayers) {
        Option(int mode,int mapId,int minimumPlayers,int timeLimitSeconds,boolean teamBased,String modeName,boolean worldAvailable) {
            this(mode,mapId,minimumPlayers,timeLimitSeconds,teamBased,modeName,worldAvailable,100);
        }
        Option {
            BattleRules.RuleSet rules = TournamentRoomPolicy.rules(mode, mapId); text(modeName, 256, false);
            if (minimumPlayers < 1 || minimumPlayers > 100 || timeLimitSeconds < 1 || timeLimitSeconds > 86400
                    || minimumPlayers != rules.minimumPlayers() || timeLimitSeconds != rules.timeLimitSeconds()
                    || teamBased != rules.teamBased() || !modeName.equals(rules.mode().displayName())
                    ||maximumPlayers<Math.max(2,minimumPlayers)||maximumPlayers>100) throw invalid();
        }
    }
    record View(long listRevision, List<Summary> rooms, RoomSnapshot joined, List<Option> options) {
        View {
            rooms = List.copyOf(rooms); options = List.copyOf(options);
            if (listRevision < 1 || rooms.size() > MAX_ROOMS || options.size() != OPTIONS.size()) throw invalid();
            for (int i = 0; i < OPTIONS.size(); i++) {
                Option actual = options.get(i), expected = OPTIONS.get(i);
                if (actual.mode() != expected.mode() || actual.mapId() != expected.mapId()) throw invalid();
            }
            long previous = 0; boolean found = joined == null;
            for (Summary summary : rooms) {
                if (summary.id() <= previous) throw invalid(); previous = summary.id();
                if (joined != null && summary.id() == joined.summary().id()) {
                    if (!summary.equals(joined.summary())) throw invalid(); found = true;
                }
            }
            if (!found) throw invalid();
        }
    }
    record Result(Status status, Reason reason, View view) {
        Result {
            Objects.requireNonNull(status); Objects.requireNonNull(reason); Objects.requireNonNull(view);
            boolean valid = switch (status) {
                case OK -> reason == Reason.NONE;
                case CONFLICT -> reason == Reason.REVISION_MISMATCH;
                case FULL -> reason == Reason.CAPACITY;
                case UNAVAILABLE -> reason == Reason.REVISION_EXHAUSTED;
                case DENIED -> reason != Reason.NONE && reason != Reason.REVISION_MISMATCH
                        && reason != Reason.CAPACITY && reason != Reason.REVISION_EXHAUSTED;
            };
            if (!valid) throw invalid();
        }
    }
    record BotResult(int requested,int added,int removed,Result result) {
        BotResult {
            Objects.requireNonNull(result);
            if(requested<0 || requested>100 || added<0 || added>requested || removed<0 || removed>100
                    || requested==0 && added!=0 || requested!=0 && removed!=0
                    || result.status()!=Status.OK && (added!=0 || removed!=0))throw invalid();
        }
    }

    /** Captured authority, not a client-supplied owner/key pair. */
    record Participant(TournamentPresence.Owner owner, Scope scope, Member member) {
        Participant { Objects.requireNonNull(owner);Objects.requireNonNull(scope);Objects.requireNonNull(member); }
    }
    /**
     * Minted only by this registry after actual readiness. Immutable roster data
     * survives cancellation for diagnostics; guard() grants only bounded in-memory
     * engine work. While holding a Guard never call Rooms, Navigation, JDBC or IO.
     * Lock order is Rooms -> ticket for invalidation, Engine -> ticket for gameplay;
     * neither ticket nor engine may call back into Rooms.
     */
    static final class FrozenPreparation {
        private final ModernRooms issuer;
        private final long matchId,generation;
        private final int channel;
        private final RoomSnapshot snapshot;
        private final List<Participant> participants;
        private final List<Member> bots;
        private final ReentrantLock gate=new ReentrantLock();
        private volatile boolean live=true;
        private boolean engineClaimed;
        private volatile boolean individualRaceExits;
        private volatile boolean individualTimeAttackExits;
        private final Set<TournamentPresence.Owner> withdrawnRaceOwners=new HashSet<>(); // Rooms monitor only.
        private FrozenPreparation(ModernRooms issuer,long matchId,long generation,int channel,
                                  RoomSnapshot snapshot,List<Participant> participants) {
            this.issuer=issuer;this.matchId=matchId;this.generation=generation;this.channel=channel;
            this.snapshot=snapshot;this.participants=List.copyOf(participants);
            this.bots=snapshot.members().stream().filter(member->!member.identity().isHuman()).toList();
        }
        long matchId(){return matchId;}
        long generation(){return generation;}
        RoomSnapshot snapshot(){return snapshot;}
        List<Participant> participants(){return participants;}
        List<Member> bots(){return bots;}
        boolean isLive(){return live;}
        Guard guard(){
            gate.lock();
            if(!live){gate.unlock();throw new IllegalStateException("world preparation is no longer live");}
            return new Guard();
        }
        /** Last constructor step, after validation: one authority per match, even after its close. */
        void claimEngine(){
            if(!gate.isHeldByCurrentThread() || !live || engineClaimed)
                throw new IllegalStateException("world preparation engine claim unavailable");
            engineClaimed=true;
        }
        void enableIndividualRaceExits(){
            if(!gate.isHeldByCurrentThread()||!live||!engineClaimed||snapshot.summary().config().mode()!=0)throw new IllegalStateException("race authority not active");
            individualRaceExits=true;
        }
        void enableIndividualTimeAttackExits(){
            if(!gate.isHeldByCurrentThread()||!live||!engineClaimed||snapshot.summary().config().mode()!=4)throw new IllegalStateException("cooperative authority not active");
            individualTimeAttackExits=true;
        }
        private boolean individualExits(){return individualRaceExits||individualTimeAttackExits;}
        private void invalidate(){gate.lock();try{live=false;}finally{gate.unlock();}}
        final class Guard implements AutoCloseable {
            private final Thread owner=Thread.currentThread();private boolean closed;
            private Guard(){}
            @Override public void close(){
                if(Thread.currentThread()!=owner)throw new IllegalStateException("world preparation guard owner mismatch");
                if(!closed){closed=true;gate.unlock();}
            }
        }
        @Override public String toString(){return "FrozenPreparation[redacted]";}
    }
    record PreparationResult(Result result,FrozenPreparation ticket) {
        PreparationResult {
            Objects.requireNonNull(result);
            if((result.status()==Status.OK)!=(ticket!=null))throw invalid();
        }
    }
    private static final List<Option> OPTIONS = options();
    private static List<Option> options() {
        List<Option> result = new ArrayList<>();
        for (BattleRules.Mode mode : BattleRules.Mode.values()) {
            if (!mode.isBattleScene()) continue;
            for (int map : mode.maps()) {
                BattleRules.RuleSet rules = TournamentRoomPolicy.rules(mode.wireId(), map);
                result.add(new Option(mode.wireId(), map, rules.minimumPlayers(), rules.timeLimitSeconds(),
                        rules.teamBased(), mode.displayName(), false));
            }
        }
        result.sort(Comparator.comparingInt(Option::mode).thenComparingInt(Option::mapId));
        return List.copyOf(result);
    }
    private static final class Binding {
        final TournamentPresence.Selection selection; final Scope scope; final boolean botProtocol; Room room;
        MatchWaiter matchmaking;
        ModernChat.Ledger chatLedger; ModernChat.Membership chatMembership;
        boolean directEnabled, filteredEnabled, largeMapsEnabled;
        Binding(TournamentPresence.Selection selected, long epoch, int channel) {
            this(selected,epoch,channel,false);
        }
        Binding(TournamentPresence.Selection selected,long epoch,int channel,boolean botProtocol) {
            selection = selected; scope = new Scope(selected.key().sourceId(), selected.key().objectId(), epoch, channel);
            this.botProtocol=botProtocol;
        }
        Member member(int team, boolean ready) {
            return new Member(selection.key(), selection.characterName(), selection.sourceName(),
                    selection.level(), selection.characterClass(), selection.sex(), team, ready);
        }
    }
    private static final class Seat {
        final Binding binding; int team; boolean ready;
        Seat(Binding binding, int team) { this.binding = binding; this.team = team; }
        Member snapshot() { return binding.member(team, ready); }
    }
    private static final class Room {
        final long id; long revision = 1; Draft draft; TournamentPresence.Owner owner;
        final ModernChat.Log chat = new ModernChat.Log();
        FrozenPreparation preparation;
        boolean speechSilenced;
        final LinkedHashMap<TournamentPresence.Owner, Seat> seats = new LinkedHashMap<>();
        final LinkedHashMap<ModernParticipantId,Member> bots=new LinkedHashMap<>();
        Room(long id, Draft draft, TournamentPresence.Owner owner, Binding binding) {
            this.id = id; this.draft = draft; this.owner = owner; seats.put(owner, new Seat(binding, 0));
        }
    }
    private final Map<TournamentPresence.Owner, Binding> bindings = new HashMap<>();
    private record MatchWaiter(TournamentPresence.Owner owner, Binding binding, long sequence,
                               ModernMatchmaking.Criteria criteria, ModernCharacterRecord.Snapshot standing) {}
    private final Map<TournamentPresence.Owner, MatchWaiter> matchWaiters = new HashMap<>();
    private long nextMatchmakingId = 1;
    private final ModernChat chat;
    private final ModernDirectChat directChat = new ModernDirectChat();
    private final ModernFilteredChat filteredChat = new ModernFilteredChat();
    private final ModernSpeechAuthority speech;
    private final List<ModernChat.Log> lobbyChats = List.of(new ModernChat.Log(),new ModernChat.Log(),new ModernChat.Log(),new ModernChat.Log());
    private final List<TreeMap<Long, Room>> channels = List.of(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>());
    private final long[] listRevisions = {1, 1, 1, 1};
    private final boolean[] exhaustedChannels = new boolean[4];
    private long nextRoomId = 1;
    private long nextMatchId = 1;
    private long nextBotId=1;
    private final List<Option> advertisedOptions;
    private Set<ModernWorldBinding.Key> largeWorlds=Set.of();
    private boolean largeWorldsConfigured;
    synchronized void configureLargeWorlds(Set<ModernWorldBinding.Key> worlds) {
        var copy=Set.copyOf(worlds);
        if(largeWorldsConfigured&&!largeWorlds.equals(copy))throw new IllegalStateException("world admission already configured");
        largeWorlds=copy;largeWorldsConfigured=true;
    }
    synchronized void enableLargeMaps(TournamentPresence.Owner owner,Scope scope) {
        var b=bindings.get(owner);
        if(b==null||!b.scope.equals(scope))throw new SecurityException("large map scope not registered");
        b.largeMapsEnabled=true;
    }
    private boolean mapAllowed(Binding binding,int mode,int map) {
        return binding.largeMapsEnabled||!largeWorlds.contains(new ModernWorldBinding.Key(mode,map));
    }
    private List<Option> optionsFor(Binding binding) {
        if(binding.largeMapsEnabled||largeWorlds.isEmpty())return advertisedOptions;
        return advertisedOptions.stream().map(o->mapAllowed(binding,o.mode(),o.mapId())?o:
                new Option(o.mode(),o.mapId(),o.minimumPlayers(),o.timeLimitSeconds(),o.teamBased(),o.modeName(),false,o.maximumPlayers())).toList();
    }
    private final Set<ModernWorldBinding.Key> botWorlds;
    private final Map<ModernWorldBinding.Key,Integer> playerCapacities;

    ModernRooms() { this(Set.of()); }

    ModernRooms(Set<ModernWorldBinding.Key> availableWorlds) {
        this(availableWorlds,Set.of());
    }
    ModernRooms(Set<ModernWorldBinding.Key> availableWorlds,Set<ModernWorldBinding.Key> availableBotWorlds) {
        this(availableWorlds,availableBotWorlds,Map.of());
    }
    ModernRooms(Set<ModernWorldBinding.Key> availableWorlds,Set<ModernWorldBinding.Key> availableBotWorlds,
            Map<ModernWorldBinding.Key,Integer> playerCapacities) {
        this(availableWorlds,availableBotWorlds,playerCapacities,new ModernChat());
    }
    ModernRooms(Set<ModernWorldBinding.Key> availableWorlds,Set<ModernWorldBinding.Key> availableBotWorlds,
            Map<ModernWorldBinding.Key,Integer> playerCapacities,ModernChat chat) {
        this(availableWorlds,availableBotWorlds,playerCapacities,chat,new ModernSpeechAuthority(Map.of()));
    }
    ModernRooms(Set<ModernWorldBinding.Key> availableWorlds,Set<ModernWorldBinding.Key> availableBotWorlds,
            Map<ModernWorldBinding.Key,Integer> playerCapacities,ModernChat chat,ModernSpeechAuthority speech) {
        this.chat=Objects.requireNonNull(chat);
        this.speech=Objects.requireNonNull(speech);
        Set<ModernWorldBinding.Key> available = Set.copyOf(availableWorlds);
        this.playerCapacities=Map.copyOf(playerCapacities);
        if(!available.containsAll(this.playerCapacities.keySet()))throw invalid();
        this.playerCapacities.forEach((key,capacity)->{if(capacity<Math.max(2,TournamentRoomPolicy.rules(key.mode(),key.logicalMapId()).minimumPlayers())||capacity>100)throw invalid();});
        botWorlds=Set.copyOf(availableBotWorlds);
        if(!available.containsAll(botWorlds))throw invalid();
        advertisedOptions = OPTIONS.stream().map(option -> new Option(option.mode(), option.mapId(),
                option.minimumPlayers(), option.timeLimitSeconds(), option.teamBased(), option.modeName(),
                available.contains(new ModernWorldBinding.Key(option.mode(), option.mapId())),
                this.playerCapacities.getOrDefault(new ModernWorldBinding.Key(option.mode(),option.mapId()),100))).toList();
    }

    synchronized void enterLobby(TournamentPresence.Owner owner, TournamentPresence.Selection selection,
                                 long epoch, int channel) {
        enterLobby(owner,selection,epoch,channel,false);
    }
    synchronized void enterLobby(TournamentPresence.Owner owner,TournamentPresence.Selection selection,
                                 long epoch,int channel,boolean botProtocol) {
        Objects.requireNonNull(owner); Objects.requireNonNull(selection);
        Binding replacement = new Binding(selection, epoch, channel,botProtocol);
        replacement.member(0, false); // Eagerly validate trusted row text before it can become a wire snapshot.
        if (exhaustedChannels[channel]) throw new IllegalStateException("room revision exhausted");
        Binding prior = bindings.get(owner);
        if (prior != null && prior.room != null) throw new IllegalStateException("room membership active");
        for (var entry : bindings.entrySet()) {
            if (entry.getKey() != owner && entry.getValue().selection.key().equals(selection.key()))
                throw new SecurityException("room character already registered");
        }
        replacement.chatLedger=chat.register(owner);
        replacement.chatMembership=chat.join(replacement.chatLedger,lobbyChats.get(channel));
        directChat.leave(owner);
        filteredChat.leave(owner);
        if (prior != null) retireMatchmaking(owner, prior);
        bindings.put(owner, replacement);
    }

    /** Caller must negotiate chat transport before invoking this seam.
     * Rooms' monitor makes routing, admission and append indivisible. */
    synchronized ModernChat.Snapshot chatRead(TournamentPresence.Owner owner,Scope scope,long roomId,long membership,long after) {
        var binding=chatBinding(owner,scope,roomId);
        if(binding.filteredEnabled)throw new SecurityException("filtered conversation cannot read the legacy log");
        return chat.read(new ModernChat.Audience(scope.channelId(),roomId),binding.chatLedger,binding.chatMembership,membership,after);
    }
    synchronized ModernChat.Result chatSend(TournamentPresence.Owner owner,Scope scope,long roomId,long membership,long sequence,long after,String text) {
        var binding=chatBinding(owner,scope,roomId);
        if(binding.filteredEnabled)throw new SecurityException("filtered conversation cannot send through the legacy path");
        var result=chat.send(new ModernChat.Audience(scope.channelId(),roomId),binding.chatLedger,binding.chatMembership,
                membership,sequence,after,binding.selection,text);
        if(result.status()==ModernChat.Status.OK)
            filteredChat.publish(filteredToken(binding),ModernDirectChat.Peer.of(binding.selection),ModernFilteredChat.Kind.NORMAL,text,directChat);
        return result;
    }
    synchronized int chatOwnerCount() { return chat.ownerCount(); }
    /** Explicit v33 enrollment, requiring the private preference ledger. */
    synchronized void enableFilteredChat(TournamentPresence.Owner owner, Scope scope) {
        var binding=bindings.get(owner);
        if(binding==null||!binding.scope.equals(scope)||!binding.directEnabled)
            throw new SecurityException("filtered chat requires current direct enrollment");
        filteredChat.enable(owner,ModernDirectChat.Peer.of(binding.selection),filteredToken(binding));
        binding.filteredEnabled=true;
    }
    synchronized ModernFilteredChat.Result filteredChat(TournamentPresence.Owner owner,
            ModernFilteredChat.Request request,ModernFilteredChat.SpeechPolicy authority) {
        Objects.requireNonNull(request);
        var token=request.token();
        var binding=chatBinding(owner,token.scope(),token.roomId());
        if(!binding.filteredEnabled)throw new SecurityException("filtered chat not negotiated");
        return filteredChat.execute(owner,request,authority,chat,binding.chatLedger,
                binding.chatMembership.log(),binding.selection,directChat);
    }
    synchronized int filteredChatOwnerCount() { return filteredChat.ownerCount(); }
    /** Trusted negotiated adapter: policy is sampled under the SAME room
     * monitor as routing and delivery. No caller-supplied privilege booleans. */
    synchronized ModernFilteredChat.Result filteredChatAuthorized(TournamentPresence.Owner owner,
            ModernFilteredChat.Request request,ModernAccountAuthentication.Principal principal) {
        Objects.requireNonNull(request);
        var token=request.token();
        var binding=chatBinding(owner,token.scope(),token.roomId());
        if(!binding.filteredEnabled)throw new SecurityException("filtered chat not negotiated");
        var authority=speech.policy(owner,principal,binding.selection,filteredToken(binding),
                binding.room!=null&&binding.room.speechSilenced);
        return filteredChat.execute(owner,request,authority,chat,binding.chatLedger,
                binding.chatMembership.log(),binding.selection,directChat);
    }
    /** Internal server command/effect seam, NOT a room-owner or client action. */
    synchronized void silenceCharacter(TournamentPresence.Owner owner,ModernFilteredChat.Token token,int seconds,boolean timed) {
        var binding=exactSpeechBinding(owner,token);
        speech.silence(owner,binding.selection.key(),seconds,timed);
    }
    synchronized void silenceRoom(TournamentPresence.Owner owner,ModernFilteredChat.Token token,boolean silenced) {
        var binding=exactSpeechBinding(owner,token);
        if(binding.room==null)throw new SecurityException("room silence requires a current room");
        binding.room.speechSilenced=silenced;
    }
    private Binding exactSpeechBinding(TournamentPresence.Owner owner,ModernFilteredChat.Token token) {
        Objects.requireNonNull(token);
        var binding=chatBinding(owner,token.scope(),token.roomId());
        if(token.membership()<=0||!token.equals(filteredToken(binding)))throw new SecurityException("speech admission changed");
        return binding;
    }
    synchronized int speechOwnerCount(){return speech.ownerCount();}
    boolean supportsFilteredChat(){return speech.configured();}
    private static ModernFilteredChat.Token filteredToken(Binding binding) {
        return new ModernFilteredChat.Token(binding.scope,binding.room==null?0:binding.room.id,binding.chatMembership.generation());
    }
    private void moveFilteredChat(TournamentPresence.Owner owner,Binding binding) {
        if(!binding.filteredEnabled)return;
        if(binding.chatMembership.generation()==0)filteredChat.leave(owner);
        else filteredChat.move(owner,filteredToken(binding));
    }
    /** Trusted negotiated adapter enrollment, never an implicit capability for legacy clients. */
    synchronized void enableDirectChat(TournamentPresence.Owner owner, Scope scope) {
        var binding = bindings.get(owner);
        if (binding == null || !binding.scope.equals(scope)) throw new SecurityException("direct scope not registered");
        directChat.enable(owner, binding.selection, scope.epoch());
        binding.directEnabled = true;
    }
    synchronized ModernDirectChat.Result directChat(TournamentPresence.Owner owner, Scope scope,
            long roomId, long membership, ModernDirectChat.Request request) {
        var binding = chatBinding(owner, scope, roomId);
        if (binding.filteredEnabled || !binding.directEnabled || membership < 1 || membership != binding.chatMembership.generation())
            throw new SecurityException("direct audience changed or not negotiated");
        return directChat.execute(owner, request, chat, binding.chatLedger);
    }
    synchronized ModernDirectChat.Result moderatedDirectChat(TournamentPresence.Owner owner,
            ModernDirectChatProtocol.Request request, ModernAccountAuthentication.Principal principal) {
        var binding=exactSpeechBinding(owner,new ModernFilteredChat.Token(request.scope(),request.roomId(),request.membership()));
        if(!binding.filteredEnabled||!binding.directEnabled)throw new SecurityException("moderated direct chat not negotiated");
        var authority=speech.policy(owner,principal,binding.selection,filteredToken(binding),
                binding.room!=null&&binding.room.speechSilenced);
        return directChat.execute(owner,request.command(),chat,binding.chatLedger,authority.blocksPrivate());
    }
    synchronized int directChatOwnerCount() { return directChat.ownerCount(); }
    private Binding chatBinding(TournamentPresence.Owner owner,Scope scope,long roomId) {
        var binding=bindings.get(owner);
        if(binding==null||!binding.scope.equals(scope)||roomId<0
                ||roomId!=(binding.room==null?0:binding.room.id))throw new SecurityException("chat audience is not registered");
        return binding;
    }

    /** Trusted modern adapter seam, not yet a room wire opcode. Standing is a
     * confirmed source-qualified central record read OUTSIDE this monitor.
     * The adapter must negotiate matchmaking before exposing these methods. */
    synchronized ModernMatchmaking.Result startMatchmaking(TournamentPresence.Owner owner, Scope scope,
            ModernMatchmaking.Criteria criteria, ModernCharacterRecord.Snapshot standing) {
        Objects.requireNonNull(criteria); Objects.requireNonNull(standing);
        Binding binding = matchmakingBinding(owner, scope);
        if (!binding.selection.key().equals(standing.key())) throw new SecurityException("matchmaking record source changed");
        if (binding.room != null || binding.matchmaking != null)
            return matchmakingResult(binding, Status.DENIED, Reason.INVALID_STATE);
        if (!TournamentRoomPolicy.canCreate(scope.channelId(), binding.selection.level()))
            return matchmakingResult(binding, Status.DENIED, Reason.LEVEL_RESTRICTED);
        if (optionsFor(binding).stream().noneMatch(criteria::accepts))
            return matchmakingResult(binding, Status.DENIED, Reason.INVALID_CONFIGURATION);
        if (matchWaiters.size() >= ModernMatchmaking.MAX_WAITERS || channels.get(scope.channelId()).size() >= MAX_ROOMS)
            return matchmakingResult(binding, Status.FULL, Reason.CAPACITY);
        if (nextMatchmakingId == Long.MAX_VALUE || nextRoomId == Long.MAX_VALUE || !canBump(binding, null))
            return matchmakingResult(binding, Status.UNAVAILABLE, Reason.REVISION_EXHAUSTED);
        var ticket = new MatchWaiter(owner, binding, nextMatchmakingId++, criteria, standing);
        binding.matchmaking = ticket; matchWaiters.put(owner, ticket);
        // Enqueue ACK is distinct from match commit. The bounded subsequent
        // poll groups waiting players, allowing win-rate selection from a batch.
        return matchmakingResult(binding, Status.OK, Reason.NONE);
    }

    /** Zero is observation-only bootstrap; a positive exact admission may
     * advance its still-waiting queue. A stale poll cannot advance a new ticket. */
    synchronized ModernMatchmaking.Result readMatchmaking(TournamentPresence.Owner owner, Scope scope, long ticketId) {
        if (ticketId < 0) throw invalid();
        Binding binding = matchmakingBinding(owner, scope);
        if (ticketId != 0 && (binding.matchmaking == null || binding.matchmaking.sequence() != ticketId))
            return matchmakingResult(binding, Status.CONFLICT, Reason.REVISION_MISMATCH);
        if (ticketId != 0 && binding.room == null) return tryMatchmaking(binding.matchmaking);
        return matchmakingResult(binding, Status.OK, Reason.NONE);
    }

    synchronized ModernMatchmaking.Result cancelMatchmaking(TournamentPresence.Owner owner, Scope scope, long ticketId) {
        if (ticketId <= 0) throw invalid();
        Binding binding = matchmakingBinding(owner, scope);
        if (binding.matchmaking == null) return matchmakingResult(binding, Status.OK, Reason.NONE);
        if (binding.matchmaking.sequence() != ticketId)
            return matchmakingResult(binding, Status.CONFLICT, Reason.REVISION_MISMATCH);
        // Once committed, cancelling a queue must NOT remove a room occupant.
        // Returning actual MATCHED membership makes the race explicit to UI.
        if (binding.room == null) retireMatchmaking(owner, binding);
        return matchmakingResult(binding, Status.OK, Reason.NONE);
    }

    synchronized int queuedMatchmakingCount() { return matchWaiters.size(); }

    private Binding matchmakingBinding(TournamentPresence.Owner owner, Scope scope) {
        Objects.requireNonNull(scope);
        Binding binding = bindings.get(owner);
        if (binding == null || !binding.scope.equals(scope)) throw new SecurityException("matchmaking scope is not registered");
        return binding;
    }

    private ModernMatchmaking.Result tryMatchmaking(MatchWaiter incoming) {
        Binding binding = incoming.binding();
        if (binding.matchmaking != incoming || matchWaiters.get(incoming.owner()) != incoming || binding.room != null)
            throw new IllegalStateException("matchmaking admission changed");
        if (nextRoomId == Long.MAX_VALUE || !canBump(binding, null))
            return matchmakingResult(binding, Status.UNAVAILABLE, Reason.REVISION_EXHAUSTED);
        var maps = optionsFor(binding).stream().filter(incoming.criteria()::accepts).toList();
        if (maps.isEmpty()) return matchmakingResult(binding, Status.DENIED, Reason.INVALID_CONFIGURATION);
        var compatible = matchWaiters.values().stream()
                .filter(candidate -> candidate.binding().scope.channelId() == binding.scope.channelId()
                        && candidate.criteria().equals(incoming.criteria())).toList();
        // Oldest compatible admission is the anchor, not whichever client polls
        // fastest. One poll can publish at most one complete cohort.
        var anchor = compatible.stream().min(Comparator.comparingLong(MatchWaiter::sequence)).orElseThrow();
        var candidates = compatible.stream().filter(candidate -> candidate != anchor)
                .sorted((left, right) -> ModernMatchmaking.compare(anchor.standing(), left.standing(), left.sequence(),
                        right.standing(), right.sequence())).limit(incoming.criteria().players() - 1L).toList();
        if (candidates.size() + 1 < incoming.criteria().players())
            return matchmakingResult(binding, Status.OK, Reason.NONE);
        var channel = channels.get(binding.scope.channelId());
        if (channel.size() >= MAX_ROOMS) return matchmakingResult(binding, Status.FULL, Reason.CAPACITY);
        var cohort = new ArrayList<MatchWaiter>(candidates); cohort.add(anchor);
        cohort.sort(Comparator.comparingLong(MatchWaiter::sequence));
        maps=maps.stream().filter(o->cohort.stream().allMatch(w->mapAllowed(w.binding(),o.mode(),o.mapId()))).toList();
        if(maps.isEmpty())return matchmakingResult(binding,Status.OK,Reason.NONE);
        // Explicit modern round-robin selection, not a claim about native RNG.
        var option = maps.get((int) ((nextRoomId - 1) % maps.size()));
        var draft = new Draft("自動配對", "", Math.max(2, cohort.size()), option.mode(), option.mapId(),
                option.teamBased() ? EQUAL_SIDES : 0);
        var first = cohort.get(0);
        var created = new Room(nextRoomId, draft, first.owner(), first.binding());
        for (int i = 1; i < cohort.size(); i++) {
            var waiter = cohort.get(i);
            created.seats.put(waiter.owner(), new Seat(waiter.binding(), option.teamBased() ? i % 2 : 0));
        }
        snapshot(created); // Complete valid roster before the single publication.
        nextRoomId++; channel.put(created.id, created); listRevisions[binding.scope.channelId()]++;
        for (var waiter : cohort) {
            var member = waiter.binding(); member.room = created; matchWaiters.remove(waiter.owner());
            member.chatMembership = chat.join(member.chatLedger, created.chat);
            moveFilteredChat(waiter.owner(), member);
        }
        // All seats remain unready: grouping is not a load/ready/world permit.
        return matchmakingResult(binding, Status.OK, Reason.NONE);
    }

    private ModernMatchmaking.Result matchmakingResult(Binding binding, Status status, Reason reason) {
        var admission = binding.matchmaking;
        return new ModernMatchmaking.Result(admission == null ? ModernMatchmaking.State.IDLE
                : binding.room == null ? ModernMatchmaking.State.WAITING : ModernMatchmaking.State.MATCHED,
                admission == null ? 0 : admission.sequence(), admission == null ? null : admission.criteria(), result(binding, status, reason));
    }

    private void retireMatchmaking(TournamentPresence.Owner owner, Binding binding) {
        matchWaiters.remove(owner); binding.matchmaking = null;
    }

    synchronized Result execute(TournamentPresence.Owner owner, Command command) {
        Objects.requireNonNull(command);
        Binding binding = bindings.get(owner);
        if (binding == null || !binding.scope.equals(command.scope()))
            throw new SecurityException("room scope is not registered");
        if (exhaustedChannels[binding.scope.channelId()]) throw new IllegalStateException("room revision exhausted");
        if (command.action() == Action.READ) return result(binding, Status.OK, Reason.NONE);
        if (command.action() == Action.CREATE) return create(owner, binding, command.draft());
        Room target = channels.get(binding.scope.channelId()).get(command.roomId());
        if (target == null) return result(binding, Status.DENIED, Reason.ROOM_NOT_FOUND);
        if (command.action() == Action.JOIN) {
            if (binding.room != null) return result(binding, Status.DENIED, Reason.INVALID_STATE);
        } else if (binding.room != target || !target.seats.containsKey(owner)) {
            return result(binding, Status.DENIED, Reason.NOT_MEMBER);
        }
        if (target.revision != command.expectedRevision()) return result(binding, Status.CONFLICT, Reason.REVISION_MISMATCH);
        if(target.preparation!=null&&!(command.action()==Action.LEAVE&&target.preparation.individualExits()
                &&target.preparation.withdrawnRaceOwners.contains(owner)))return result(binding,Status.DENIED,Reason.INVALID_STATE);
        return switch (command.action()) {
            case JOIN -> join(owner, binding, target, command.password());
            case LEAVE -> {
                if (!canBump(binding, target)) yield exhausted(binding);
                remove(owner, binding, target); yield result(binding, Status.OK, Reason.NONE);
            }
            case TEAM -> team(owner, binding, target, command.value());
            case READY -> ready(owner, binding, target, command.value() == 1);
            case CONFIGURE -> configure(owner, binding, target, command.draft());
            default -> throw new IllegalStateException("unhandled room action");
        };
    }

    private Result create(TournamentPresence.Owner owner, Binding binding, Draft draft) {
        if (binding.room != null) return result(binding, Status.DENIED, Reason.INVALID_STATE);
        if(!mapAllowed(binding,draft.mode(),draft.mapId()))return result(binding,Status.DENIED,Reason.INVALID_CONFIGURATION);
        if(!capacityAllowed(draft))return result(binding,Status.DENIED,Reason.INVALID_CONFIGURATION);
        if (!TournamentRoomPolicy.canCreate(binding.scope.channelId(), binding.selection.level()))
            return result(binding, Status.DENIED, Reason.LEVEL_RESTRICTED);
        TreeMap<Long, Room> channel = channels.get(binding.scope.channelId());
        if (channel.size() >= MAX_ROOMS) return result(binding, Status.FULL, Reason.CAPACITY);
        if (nextRoomId == Long.MAX_VALUE || !canBump(binding, null)) return exhausted(binding);
        Room created = new Room(nextRoomId, draft, owner, binding);
        // Construction and all rejection checks precede the single monitor-protected publication.
        snapshot(created); nextRoomId++; channel.put(created.id, created); binding.room = created;
        retireMatchmaking(owner, binding);
        binding.chatMembership=chat.join(binding.chatLedger,created.chat);
        moveFilteredChat(owner,binding);
        listRevisions[binding.scope.channelId()]++;
        return result(binding, Status.OK, Reason.NONE);
    }
    private Result join(TournamentPresence.Owner owner, Binding binding, Room room, String password) {
        if(!mapAllowed(binding,room.draft.mode(),room.draft.mapId()))return result(binding,Status.DENIED,Reason.INVALID_CONFIGURATION);
        if(!room.bots.isEmpty() && !binding.botProtocol)return result(binding,Status.DENIED,Reason.INVALID_STATE);
        if (!room.draft.password().equals(password)) return result(binding, Status.DENIED, Reason.BAD_PASSWORD);
        if (!TournamentRoomPolicy.canJoin(binding.scope.channelId(), binding.selection.level(),
                (room.draft.attributes() & ALLOW_UPPER_TIER) != 0))
            return result(binding, Status.DENIED, Reason.LEVEL_RESTRICTED);
        if (users(room) >= capacity(room.draft.publicConfig())) return result(binding, Status.FULL, Reason.CAPACITY);
        if (!canBump(binding, room)) return exhausted(binding);
        int team = 0;
        if (TournamentRoomPolicy.rules(room.draft.mode(), room.draft.mapId()).teamBased()) {
            int first = firstTeam(room); team = first <= users(room) - first ? 0 : 1;
        }
        room.seats.put(owner, new Seat(binding, team)); binding.room = room; bump(binding, room);
        retireMatchmaking(owner, binding);
        binding.chatMembership=chat.join(binding.chatLedger,room.chat);
        moveFilteredChat(owner,binding);
        return result(binding, Status.OK, Reason.NONE);
    }
    /** Fresh source policy is consumed only with the current registered selection and exact room revision. */
    synchronized BotResult spawnBots(TournamentPresence.Owner owner,Scope scope,long roomId,long expectedRevision,
                                      int count,ModernGmCommands.Snapshot fresh) {
        new ModernGmCommands.SpawnRequest(count);
        if(scope==null || roomId<=0 || expectedRevision<1)throw invalid();
        Binding binding=bindings.get(owner);
        if(binding==null || !binding.scope.equals(scope))throw new SecurityException("GM room scope is not registered");
        if(exhaustedChannels[scope.channelId()])throw new IllegalStateException("room revision exhausted");
        Room room=channels.get(scope.channelId()).get(roomId);
        if(room==null)return botResult(binding,count,Status.DENIED,Reason.ROOM_NOT_FOUND);
        if(binding.room!=room || !room.seats.containsKey(owner))return botResult(binding,count,Status.DENIED,Reason.NOT_MEMBER);
        if(fresh==null || fresh.decision()!=ModernGmCommands.Decision.ALLOWED)
            return botResult(binding,count,Status.DENIED,Reason.GM_NOT_AUTHORIZED);
        var selected=binding.selection;var row=fresh.character();
        if(fresh.sourceId()!=scope.sourceId() || row.objectId()!=scope.objectId() || !row.name().equals(selected.characterName())
                || row.level()!=selected.level() || row.characterClass()!=selected.characterClass() || row.sex()!=selected.sex())
            throw new SecurityException("GM source selection changed");
        if(room.revision!=expectedRevision)return botResult(binding,count,Status.CONFLICT,Reason.REVISION_MISMATCH);
        if(room.preparation!=null || !botWorlds.contains(new ModernWorldBinding.Key(room.draft.mode(),room.draft.mapId()))
                || room.seats.values().stream().anyMatch(seat->!seat.binding.botProtocol))
            return botResult(binding,count,Status.DENIED,Reason.INVALID_STATE);
        int removed=count==0?room.bots.size():0;
        int added=count==0?0:Math.min(count,capacity(room.draft.publicConfig())-users(room));
        if(removed==0 && added==0)return new BotResult(count,0,0,result(binding,Status.OK,Reason.NONE));
        if(!canBump(binding,room) || added>0 && nextBotId>Long.MAX_VALUE-added)
            return botResult(binding,count,Status.UNAVAILABLE,Reason.REVISION_EXHAUSTED);
        // Validate all names/template metadata and allocate the complete batch
        // before one monitor-protected publication. No SQL/session/BIN identity.
        List<Member> batch=new ArrayList<>(added);int first=firstTeam(room),second=users(room)-first;
        boolean teams=TournamentRoomPolicy.rules(room.draft.mode(),room.draft.mapId()).teamBased();
        for(int index=0;index<added;index++) {
            long id=nextBotId+index;int type=(int)((id-1)%7),sex=(int)((id-1)%2);
            var template=ModernBotTemplate.roomTestDummy(type,sex);int team=teams && first>second?1:0;
            if(team==0)first++;else second++;
            batch.add(new Member(ModernParticipantId.bot(scope.sourceId(),id),"BOT"+id,selected.sourceName(),
                    template.level(),type,sex,team,true));
        }
        if(count==0)room.bots.clear();
        else {for(Member bot:batch)room.bots.put(bot.identity(),bot);nextBotId+=added;}
        bump(binding,room);
        return new BotResult(count,added,removed,result(binding,Status.OK,Reason.NONE));
    }
    private BotResult botResult(Binding binding,int count,Status status,Reason reason) {
        return new BotResult(count,0,0,result(binding,status,reason));
    }
    private Result team(TournamentPresence.Owner owner, Binding binding, Room room, int team) {
        BattleRules.RuleSet rules = TournamentRoomPolicy.rules(room.draft.mode(), room.draft.mapId());
        if (!rules.teamBased()) return result(binding, Status.DENIED, Reason.INVALID_CONFIGURATION);
        Seat seat = room.seats.get(owner);
        if (seat.team == team) return result(binding, Status.OK, Reason.NONE);
        int count = team == 0 ? firstTeam(room) : users(room) - firstTeam(room);
        if (rules.mode() == BattleRules.Mode.SIEGE_WAR && count >= BattleRules.SIEGE_MAXIMUM_PLAYERS_PER_FORCE)
            return result(binding, Status.FULL, Reason.CAPACITY);
        if (!canBump(binding, room)) return exhausted(binding);
        seat.team = team; seat.ready = false; bump(binding, room);
        return result(binding, Status.OK, Reason.NONE);
    }
    private Result ready(TournamentPresence.Owner owner, Binding binding, Room room, boolean desired) {
        Seat seat = room.seats.get(owner);
        if (seat.ready == desired) return result(binding, Status.OK, Reason.NONE);
        if (!canBump(binding, room)) return exhausted(binding);
        seat.ready = desired; bump(binding, room); return result(binding, Status.OK, Reason.NONE);
    }
    private Result configure(TournamentPresence.Owner owner, Binding binding, Room room, Draft draft) {
        if (room.owner != owner) return result(binding, Status.DENIED, Reason.NOT_OWNER);
        if(room.seats.values().stream().anyMatch(s->!mapAllowed(s.binding,draft.mode(),draft.mapId())))
            return result(binding,Status.DENIED,Reason.INVALID_CONFIGURATION);
        if(!capacityAllowed(draft))return result(binding,Status.DENIED,Reason.INVALID_CONFIGURATION);
        if (users(room) > capacity(draft.publicConfig())) return result(binding, Status.FULL, Reason.CAPACITY);
        if(!room.bots.isEmpty() && !botWorlds.contains(new ModernWorldBinding.Key(draft.mode(),draft.mapId())))
            return result(binding,Status.DENIED,Reason.INVALID_CONFIGURATION);
        if (room.draft.equals(draft)) return result(binding, Status.OK, Reason.NONE);
        if (!canBump(binding, room)) return exhausted(binding);
        boolean newMode = draft.mode() != room.draft.mode();
        boolean teams = TournamentRoomPolicy.rules(draft.mode(), draft.mapId()).teamBased();
        int index = 0;
        for (Seat seat : room.seats.values()) {
            seat.ready = false;
            if (!teams) seat.team = 0; else if (newMode) seat.team = index++ % 2;
        }
        for(var entry:room.bots.entrySet()) {
            var bot=entry.getValue();int team=!teams?0:newMode?index++%2:bot.team();
            entry.setValue(new Member(bot.identity(),bot.name(),bot.sourceName(),bot.level(),bot.characterClass(),bot.sex(),team,true));
        }
        room.draft = draft; bump(binding, room); return result(binding, Status.OK, Reason.NONE);
    }

    synchronized boolean hasRoom(TournamentPresence.Owner owner) {
        Binding binding = bindings.get(owner); return binding != null && binding.room != null;
    }
    synchronized int ownerCount() { return bindings.size(); }
    synchronized int roomCount() { return channels.stream().mapToInt(Map::size).sum(); }
    synchronized int memberCount() {
        return channels.stream().flatMap(channel -> channel.values().stream()).mapToInt(ModernRooms::users).sum();
    }
    synchronized void leaveLobby(TournamentPresence.Owner owner) {
        if (hasRoom(owner)) throw new IllegalStateException("room membership active");
        Binding binding = bindings.get(owner);
        if (binding != null) retireMatchmaking(owner, binding);
        directChat.leave(owner);
        filteredChat.leave(owner);
        bindings.remove(owner);
    }
    synchronized void disconnect(TournamentPresence.Owner owner) {
        matchWaiters.remove(owner);
        chat.disconnect(owner);
        directChat.disconnect(owner);
        filteredChat.disconnect(owner);
        speech.disconnect(owner);
        Binding binding = bindings.get(owner);
        if (binding == null) return;
        binding.matchmaking = null;
        if (binding.room != null) {
            Room room = binding.room;
            if(room.preparation==null||!room.preparation.individualExits()||!room.preparation.withdrawnRaceOwners.contains(owner))cancelPreparation(room);
            if (!canBump(binding, room)) {
                // Disconnect cannot leave ghost occupants. Never publish changed content
                // under a reused revision: this channel becomes fail-closed until restart.
                exhaustedChannels[binding.scope.channelId()] = true;
                removeSeat(owner, binding, room);
            } else remove(owner, binding, room);
        }
        bindings.remove(owner);
    }
    synchronized boolean unreadyForActivity(TournamentPresence.Owner owner) {
        Binding binding = bindings.get(owner);
        if (binding == null || binding.room == null) return true;
        Room room = binding.room; Seat seat = room.seats.get(owner);
        if(room.preparation!=null)return false;
        if (!seat.ready) return true;
        if (!canBump(binding, room)) throw new IllegalStateException("room revision exhausted");
        seat.ready = false; bump(binding, room);
        return true;
    }

    /** Trusted Navigation seam, not a new room wire action or permission to start combat. */
    synchronized PreparationResult prepareWorld(TournamentPresence.Owner owner,Scope scope,long roomId,long expectedRevision) {
        Objects.requireNonNull(scope);
        if(roomId<=0 || expectedRevision<1)throw invalid();
        Binding binding=bindings.get(owner);
        if(binding==null || !binding.scope.equals(scope))throw new SecurityException("world preparation scope is not registered");
        if(exhaustedChannels[scope.channelId()])throw new IllegalStateException("room revision exhausted");
        Room room=channels.get(scope.channelId()).get(roomId);
        if(room==null)return preparationDenied(binding,Status.DENIED,Reason.ROOM_NOT_FOUND);
        if(binding.room!=room || !room.seats.containsKey(owner))return preparationDenied(binding,Status.DENIED,Reason.NOT_MEMBER);
        if(room.owner!=owner)return preparationDenied(binding,Status.DENIED,Reason.NOT_OWNER);
        if(room.revision!=expectedRevision)return preparationDenied(binding,Status.CONFLICT,Reason.REVISION_MISMATCH);
        if(room.preparation!=null || !summary(room).readySatisfied())return preparationDenied(binding,Status.DENIED,Reason.INVALID_STATE);
        if(nextMatchId==Long.MAX_VALUE || !canBump(binding,room))return preparationDenied(binding,Status.UNAVAILABLE,Reason.REVISION_EXHAUSTED);
        // Construct and validate the complete capture before publishing its revision.
        RoomSnapshot original=snapshot(room);
        Summary after=new Summary(original.summary().id(),room.revision+1,original.summary().config(),
                original.summary().users(),original.summary().ready(),original.summary().readySatisfied());
        RoomSnapshot frozen=new RoomSnapshot(after,original.owner(),original.members());
        List<Participant> participants=room.seats.entrySet().stream().map(entry->new Participant(entry.getKey(),
                entry.getValue().binding.scope,entry.getValue().snapshot()))
                .sorted(Comparator.comparing(row->row.member().identity())).toList();
        FrozenPreparation ticket=new FrozenPreparation(this,nextMatchId,after.revision(),scope.channelId(),frozen,participants);
        bump(binding,room);nextMatchId++;room.preparation=ticket;
        return new PreparationResult(result(binding,Status.OK,Reason.NONE),ticket);
    }
    synchronized boolean isPreparing(TournamentPresence.Owner owner) {
        Binding binding=bindings.get(owner);return binding!=null && binding.room!=null && binding.room.preparation!=null;
    }
    /** Only after Engine has committed this racer's DNF/finished departure. No untrusted room packet can mint it. */
    synchronized void raceParticipantWithdrawn(TournamentPresence.Owner owner,FrozenPreparation ticket) {
        if(ticket.issuer!=this||!ticket.individualRaceExits||ticket.participants.stream().noneMatch(p->p.owner()==owner))
            throw new SecurityException("race retirement authority mismatch");
        ticket.withdrawnRaceOwners.add(owner);
    }
    /** Only the trusted world service, after cooperative Engine retirement. */
    synchronized void timeAttackParticipantWithdrawn(TournamentPresence.Owner owner,FrozenPreparation ticket) {
        if(ticket.issuer!=this||!ticket.individualTimeAttackExits||ticket.participants.stream().noneMatch(p->p.owner()==owner))
            throw new SecurityException("cooperative retirement authority mismatch");
        ticket.withdrawnRaceOwners.add(owner);
    }
    /** Idempotent for an already-cancelled ticket; a foreign registry ticket is never authority. */
    synchronized boolean releasePreparation(FrozenPreparation ticket) {
        Objects.requireNonNull(ticket);
        if(ticket.issuer!=this)throw new SecurityException("foreign world preparation");
        Room room=channels.get(ticket.channel).get(ticket.snapshot.summary().id());
        if(room==null || room.preparation!=ticket){if(ticket.individualExits())ticket.invalidate();return false;}
        Binding binding=room.seats.get(room.owner).binding;
        cancelPreparation(room);
        if(canBump(binding,room))bump(binding,room);
        else exhaustedChannels[ticket.channel]=true;
        return true;
    }
    private void cancelPreparation(Room room) {
        if(room.preparation==null)return;
        room.preparation.invalidate();room.preparation=null;
        for(Seat seat:room.seats.values())seat.ready=false;
    }
    private PreparationResult preparationDenied(Binding binding,Status status,Reason reason) {
        return new PreparationResult(result(binding,status,reason),null);
    }
    private void remove(TournamentPresence.Owner owner, Binding binding, Room room) {
        bump(binding, room); removeSeat(owner, binding, room);
    }
    private void removeSeat(TournamentPresence.Owner owner, Binding binding, Room room) {
        retireMatchmaking(owner, binding);
        room.seats.remove(owner); binding.room = null;
        binding.chatMembership=chat.join(binding.chatLedger,lobbyChats.get(binding.scope.channelId()));
        moveFilteredChat(owner,binding);
        if (room.seats.isEmpty()) {room.bots.clear();channels.get(binding.scope.channelId()).remove(room.id);}
        else if (room.owner == owner) room.owner = room.seats.keySet().iterator().next();
    }
    private boolean canBump(Binding binding, Room room) {
        return !exhaustedChannels[binding.scope.channelId()]
                && listRevisions[binding.scope.channelId()] < Long.MAX_VALUE
                && (room == null || room.revision < Long.MAX_VALUE);
    }
    private void bump(Binding binding, Room room) { listRevisions[binding.scope.channelId()]++; room.revision++; }
    private Result exhausted(Binding binding) { return result(binding, Status.UNAVAILABLE, Reason.REVISION_EXHAUSTED); }
    private Result result(Binding binding, Status status, Reason reason) {
        List<Summary> rooms = channels.get(binding.scope.channelId()).values().stream().map(this::summary).toList();
        return new Result(status, reason, new View(listRevisions[binding.scope.channelId()], rooms,
                binding.room == null ? null : snapshot(binding.room), optionsFor(binding)));
    }
    private static int users(Room room){return room.seats.size()+room.bots.size();}
    private int firstTeam(Room room) { return (int) room.seats.values().stream().filter(seat -> seat.team == 0).count()
            +(int)room.bots.values().stream().filter(bot->bot.team()==0).count(); }
    private Summary summary(Room room) {
        int ready = (int) room.seats.values().stream().filter(seat -> seat.ready).count()+room.bots.size();
        Config config = room.draft.publicConfig();
        return new Summary(room.id, room.revision, config, users(room), ready,
                readiness(config, users(room), ready, firstTeam(room)));
    }
    private RoomSnapshot snapshot(Room room) {
        List<Member> members = java.util.stream.Stream.concat(room.seats.values().stream().map(Seat::snapshot),room.bots.values().stream())
                .sorted(Comparator.comparing(Member::identity)).toList();
        return new RoomSnapshot(summary(room), room.seats.get(room.owner).binding.selection.key(), members);
    }
    private static boolean readiness(Config config, int users, int ready, int first) {
        return TournamentRoomPolicy.readySatisfied(TournamentRoomPolicy.rules(config.mode(), config.mapId()),
                users, ready, first, users - first, (config.attributes() & EQUAL_SIDES) != 0);
    }
    private static int capacity(Config config) {
        return TournamentRoomPolicy.combatantCapacity(config.capacity(), BattleRules.Mode.fromWire(config.mode()));
    }
    private boolean capacityAllowed(Draft draft) {
        return draft.capacity()<=playerCapacities.getOrDefault(new ModernWorldBinding.Key(draft.mode(),draft.mapId()),100);
    }
    private static void validateConfig(int capacity, int mode, int map, int attributes, boolean publicValue) {
        int allowed = ALLOW_UPPER_TIER | EQUAL_SIDES | (publicValue ? PRIVATE : 0);
        if (!TournamentRoomPolicy.nativeRoomCapacity(capacity) || (attributes & ~allowed) != 0) throw invalid();
        TournamentRoomPolicy.rules(mode, map);
    }
    static void text(String value, int maxBytes, boolean emptyAllowed) {
        if (value == null || (!emptyAllowed && value.codePoints().allMatch(
                code -> Character.isWhitespace(code) || Character.isSpaceChar(code)))
                || value.codePoints().anyMatch(Character::isISOControl)) throw invalid();
        try {
            int length = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value)).remaining();
            if (length > maxBytes) throw invalid();
        } catch (CharacterCodingException e) { throw invalid(); }
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("invalid modern room value"); }
}
