package local.lito381.server;
import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
public class MassTeleportAnswerProbe extends ElementalMaterialProbe {
 static final int[] SKILLS={69};
 static byte[] packet(int map,int x,int y){return new Protocol.Writer().c(Protocol.C_USE_SPELL).c(8).c(4).h(map).h(x).h(y).bytes();}
 static void reply(ClientSession peer,int question,int answer,String text,boolean nativeEntry)throws Exception{if(nativeEntry)call(peer,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_ANSWER).h(question).c(answer).s(text).bytes());else call(peer,"answerMassTeleport",new Class[]{Protocol.AskAnswer.class},new Protocol.AskAnswer(question,answer,text));}
 static Protocol.PortalDestination crossDestination(int excluded){for(int id=0;id<100;id++)if(id!=excluded&&ClassicWorldMapCatalog.supports(id)){var m=ClassicWorldMapCatalog.forMap(id);int cx=(m.startX()+m.endX())/2,cy=(m.startY()+m.endY())/2;for(int dx=-64;dx<=64;dx++)for(int dy=-64;dy<=64;dy++)if(m.standingPassable(cx+dx,cy+dy))return new Protocol.PortalDestination(id,cx+dx,cy+dy,true);}throw new AssertionError("known cross-map destination");}
 static void exercise(ClientSession pc,ClientSession peer,GameHub hub,GameStateStore state,boolean world,int skill,int mode)throws Exception{
  int change=mode/2;boolean nativeEntry=mode%2==0;
  CancellationMaterialProbe.setup(pc,peer,hub,world);set(pc,"learnedSkills",new HashSet<>(Set.of(2,69)));
  ((Map<?,?>)get(hub,"clanMembership")).clear();((Map<?,?>)get(hub,"clanProfiles")).clear();hub.registerOriginalClan(pc.sourceCharacterIdentity(),"TeleportClan");hub.registerOriginalClan(peer.sourceCharacterIdentity(),"TeleportClan");
  var row=BattleSpellCatalog.massTeleportSpell(69);int item=value(row,"itemConsumeId"),count=value(row,"itemConsumeCount");NativeDeItemPublication.grant(pc,item,count+3);
  set(pc,"pendingPortal",null);set(peer,"pendingPortal",null);set(peer,"pendingMassTeleport",null);
  int map=world?(int)get(pc,"classicWorldMapId"):hub.battleMapId((long)get(pc,"currentRoomId"),pc);byte[] cast=packet(map,0,0);
  if(world&&(change==10||change==11)){var dest=crossDestination(map);state.saveBookmark((String)call(pc,"stateCharacter",new Class[]{}),new GameStateStore.Bookmark("mass-fixture",dest.x(),dest.y(),dest.mapId()));cast=packet(dest.mapId(),dest.x(),dest.y());}
  try(var self=new SourceActionPairProbe.Wire(pc,peer);var other=new SourceActionPairProbe.Wire(peer,pc)){
   call(pc,"handle",new Class[]{byte[].class},cast);need(get(peer,"pendingMassTeleport")!=null&&peer.nativeMassTeleportAnswerLease!=null,"actual cast installs sole role-bound invitation");
   Object pending=get(peer,"pendingMassTeleport");var dest=(Protocol.PortalDestination)get(pending,"destination");int mp=(int)get(pc,"playerMp");long material=pc.nativeDeInventory(0).countCatalog(item);collect(self);collect(other);
   if(change==4){pending=copy(pending,Map.of("expiresAtNanos",System.nanoTime()-1));NativeMassTeleportAnswerLife.capture(peer,pending);set(peer,"pendingMassTeleport",pending);}
   if(change==5)NativePlayerCommit.selected(pc,pc.selectedCharacterName());
   if(change==6)NativePlayerCommit.selected(peer,peer.selectedCharacterName());
   if(change==7)set(pc,NativeDeItemApplication.DISCONNECTED,true);
   if(change==8)NativePlayerCommit.scene(peer,map+1,"classicWorldMapId");
   if(change==9)peer.nativeRejectMassTeleportQuestion();
   if(change==10){if(world){NativePlayerCommit.scene(pc,dest.mapId(),"classicWorldMapId");NativePlayerCommit.scene(pc,-1,"classicWorldInstanceId");}set(pc,"pendingPortal",null);}
   if(change==11){NativePlayerCommit.scene(pc,65530,"classicWorldMapId");}
   if(change==12)set(peer,"playerHp",0);
   if(change==13){((Map<?,?>)get(hub,"clanMembership")).clear();((Map<?,?>)get(hub,"clanProfiles")).clear();}
   if(change==15)set(peer,NativeDeItemApplication.DISCONNECTED,true);
   reply(peer,change==2?747:748,change==1?0:1,change==3?"bad":"",nativeEntry);
   boolean accepted=change==0||change==10||change==14;
   need((get(peer,"pendingPortal")!=null)==accepted,"answer acceptance world="+world+" change="+change+" native="+nativeEntry);
   if(accepted)need(get(peer,"pendingPortal").equals(dest),"invitation retains actual cast destination");
   need((int)get(pc,"playerMp")==mp&&pc.nativeDeInventory(0).countCatalog(item)==material,"reply never re-debits caster");
   if(change!=2&&change!=3)need(get(peer,"pendingMassTeleport")==null&&peer.nativeMassTeleportAnswerLease==null,"completion or retirement clears pending question and lease change="+change+" native="+nativeEntry);
   peer.nativeRejectMassTeleportQuestion();int framesBefore=other.raw.size();reply(peer,748,1,"",nativeEntry);need(other.raw.size()==framesBefore,"duplicate answer no duplicate portal or output");
   collect(self);collect(other);need(self.locked+other.locked==0,"answer publications after player unlock");
   need(NativePlayerVitalCompletions.pending(NativePlayerLifetime.of(pc))==0&&SourceActionPairProbe.state(pc).queue.isEmpty()&&SourceActionPairProbe.state(peer).queue.isEmpty(),"all answer queues drained");
  }cases++;
 }
 public static void main(String[]args)throws Exception{before=args[4].equals("before");var jar=Path.of(args[3]).toRealPath();for(var type:List.of(ClientSession.class,RuntimeInventory.class,NativeCastMaterial.class,NativeDeItemTransaction.class))need(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(jar),"actual native handlers and material authority from requested JAR");var base=ServerConfig.load(Path.of(args[0]),"standard");ResourceDatabase.load(base);var templates=new NativeDeItemTemplatesResources.Owner().reload(base);var definitions=new NativeDeItemDefinitionsResources.Owner().reload(base);var root=Path.of(args[1]);Throwable primary=null;
  try{for(boolean flag:new boolean[]{true})for(boolean world:new boolean[]{false,true})for(boolean app:new boolean[]{false,true})for(int skill:SKILLS)for(int mode=0;mode<32;mode++){var r=root.resolve(flag+"-"+world+"-"+app+"-"+skill+"-"+mode);Files.createDirectories(r);var cfg=copy(base,Map.of("battleNaturalHpRegenEnabled",false,"battleNaturalMpRegenEnabled",false,"statePersistenceEnabled",flag,"stateFile",r.resolve("must-not-write.properties")));var state=new GameStateStore(r.resolve("must-not-write.properties"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
   try(var capture=new CaptureLog(r.resolve("capture"));var hub=new GameHub(cfg,state,accounts);var f=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var p=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var m=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);var o=new SourceLoginRegression.Fixture(cfg,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)){var clock=(ScheduledExecutorService)get(hub,"battleClock");clock.shutdownNow();need(clock.awaitTermination(2,TimeUnit.SECONDS),"unrelated clock stopped");var fixtures=List.of(f,p,m,o);init(fixtures,cfg);FlowRegression.setup(hub,f.session,p.session,m.session,o.session);if(world)world(fixtures,hub);if(app)NativeDeItemApplication.install(hub,templates,definitions);exercise(p.session,m.session,hub,state,world,skill,mode);}need(!Files.exists(r.resolve("must-not-write.properties")),"both flags remain memory-only");
  }}catch(Throwable e){primary=e;throw e;}finally{try{NativePlayerCommit.closeFixture();}catch(Throwable e){if(primary!=null)primary.addSuppressed(e);else throw e;}}need(NativePlayerLifetime.allIdle(),"native lifetimes idle");var output=Path.of(args[2]);Files.writeString(output.resolveSibling("self-material-wire.txt"),String.join("\n",wires)+"\n");Files.writeString(output.resolveSibling("self-material-goldens.txt"),String.join("\n",goldens)+"\n");Files.writeString(output.resolveSibling("self-material-notes.txt"),String.join("\n",notes)+"\n");String result="{\"cases\":"+cases+",\"checks\":"+ownChecks+",\"self_skills\":1,\"material_skills\":1,\"Source_window_bypasses\":"+gaps+",\"actual_frames\":"+frames+",\"locked_write_calls\":"+lockedWrites+",\"production_PC_overlays\":0,\"full_goal_achieved\":false}";Files.writeString(output,result+"\n");System.out.println(result);}
}
