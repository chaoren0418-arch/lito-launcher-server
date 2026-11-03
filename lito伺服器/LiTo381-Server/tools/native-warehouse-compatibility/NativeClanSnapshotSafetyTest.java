package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeClanSnapshotSafetyTest {
 @TempDir Path temporary;
 @Test void signedWriter127Accepts128RejectsAndEmptyClears(){
  var member=new GameHub.ClanMemberSnapshot("Member",Protocol.LitoClanRole.MEMBER,0,0,"",Protocol.LitoClanActivity.OFFLINE);
  assertEquals(127,count(Protocol.clanInfo(new GameHub.ClanData("Clan",Collections.nCopies(127,member),false))));
  assertThrows(IllegalArgumentException.class,()->Protocol.clanInfo(new GameHub.ClanData("Clan",Collections.nCopies(128,member),false)));
  assertEquals(0,count(Protocol.clanInfo(GameHub.ClanData.empty())));
 }
 @Test void imported128AuthorityRetainedAndViewPrioritizesActualOwnerMembersThenCandidates()throws Exception {
  ServerConfig c=ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
  try(GameHub h=new GameHub(c)){
   String owner=GameStateStore.sourceCharacter("Sun","Owner");var full=h.reconcileSqlClan(owner,snapshot("Owner",128));assertEquals(128,full.members().size());
   var view=GameHub.boundedNativeClanData(full);assertEquals(127,view.members().size());assertEquals("Owner",view.members().get(0).name());assertEquals(128,h.clanData(owner).members().size());
   assertNull(h.addClanCandidate(owner,"Extra"));assertEquals(128,h.clanData(owner).members().size());
   String moon=GameStateStore.sourceCharacter("Moon","Owner");h.reconcileSqlClan(moon,snapshot("Owner",1));assertEquals(1,h.clanData(moon).members().size());assertFalse(h.sameClan(owner,moon));assertEquals(128,h.clanData(owner).members().size());
  }
 }
 @Test void candidateBoundary126127ApprovalAndRoleRejectionsPreserveAuthority()throws Exception {
  ServerConfig c=ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
  try(GameHub h=new GameHub(c)){
   String owner=GameStateStore.sourceCharacter("Sun","Owner");h.reconcileSqlClan(owner,snapshot("Owner",126));
   assertNull(h.addClanCandidate(GameStateStore.sourceCharacter("Moon","Owner"),"Extra"));assertNull(h.addClanCandidate(GameStateStore.sourceCharacter("Sun","M000"),"Extra"));
   assertNotNull(h.addClanCandidate(owner,"Candidate"));assertEquals(127,h.clanData(owner).members().size());assertNull(h.addClanCandidate(owner,"Extra"));assertNull(h.addClanCandidate(owner,"Candidate"));
   assertNull(h.removeClanMember(owner,"Candidate")); // retained role-2 removal rejects candidate role3
   assertNotNull(h.admitClanCandidate(owner,"Candidate"));assertEquals(2,h.clanRole(GameStateStore.sourceCharacter("Sun","Candidate")));assertEquals(127,h.clanData(owner).members().size());
   assertNull(h.admitClanCandidate(owner,"Candidate"));assertNull(h.removeClanMember(owner,"Owner"));
  }
 }
 @Test void explicitlyForeignCandidateIdentityAndControlWireNameAreRejected()throws Exception {
  ServerConfig c=ServerConfig.load(Path.of("src/test/resources/server-config/config/server.selftest.properties"));
  try(GameHub h=new GameHub(c)){
   String owner=GameStateStore.sourceCharacter("Sun","Owner"),foreign=GameStateStore.sourceCharacter("Moon","Candidate");h.reconcileSqlClan(owner,snapshot("Owner",1));
   assertNull(h.addClanCandidate(owner,foreign));assertNull(h.admitClanCandidate(owner,foreign));assertNull(h.removeClanMember(owner,foreign));assertNull(h.changeClanRole(owner,foreign,2));assertEquals(1,h.clanData(owner).members().size());assertEquals(GameHub.ClanData.empty(),h.clanData(foreign));
  }
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;f.hub.reconcileSqlClan(f.session.sourceCharacterIdentity(),snapshot(f.session.playerName(),1));
   RuntimeException failure=assertThrows(RuntimeException.class,()->f.call("addClanCandidate",name(Protocol.C_LITO_CLAN_ADD,"Moon\u001FCandidate")));assertInstanceOf(IllegalArgumentException.class,failure.getCause());assertTrue(f.drain().isEmpty());assertEquals(1,f.hub.clanData(f.session.sourceCharacterIdentity()).members().size());
  }
 }
 @Test void actualEncryptedQueryAddApproveAndReadOnlyRejectStayWithin127()throws Exception {
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;String owner=f.session.sourceCharacterIdentity();f.hub.reconcileSqlClan(owner,snapshot(f.session.playerName(),126));
   f.call("sendClanInfo");assertEquals(126,count(f.drain().get(0)));
   f.call("addClanCandidate",name(Protocol.C_LITO_CLAN_ADD,"Candidate"));assertEquals(1,f.drain().size());
   f.call("sendClanInfo");assertEquals(127,count(f.drain().get(0)));
   f.call("addClanCandidate",name(Protocol.C_LITO_CLAN_ADD,"Extra"));assertTrue(f.drain().isEmpty());
   f.call("admitClanCandidate",name(Protocol.C_LITO_CLAN_ADMIT,"Candidate"));var approved=f.drain();assertEquals(1,approved.size());assertEquals(127,count(approved.get(0)));assertEquals(2,f.hub.clanRole(GameStateStore.sourceCharacter(p.source,"Candidate")));
   AccountRepository original=f.accounts;boolean[] called={false};AccountRepository proxy=(AccountRepository)Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),new Class<?>[]{AccountRepository.class},(x,m,a)->{
    if(m.getName().equals("repositoryForClassicSource"))return x;
    if(m.getName().equals("joinClan")){called[0]=true;return RepositoryClanMutation.readOnly(snapshot(f.session.playerName(),127));}
    return m.invoke(original,a);
   });NativeWarehouseCompatibilityTest.set(f.session,"accounts",proxy);
   var before=f.hub.clanData(owner);f.call("admitClanCandidate",name(Protocol.C_LITO_CLAN_ADMIT,"Rejected"));assertTrue(called[0]);assertEquals(before,f.hub.clanData(owner));assertTrue(f.drain().isEmpty());
  }
 }
 @Test void privateSnapshotRevokesOnMutationReselectSceneAndDisconnect()throws Exception {
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;f.hub.reconcileSqlClan(f.session.sourceCharacterIdentity(),snapshot(f.session.playerName(),1));
   var first=f.hub.nativeClanSnapshot(f.session);assertTrue(first.current(f.session));assertTrue(first.sameContext(f.session));assertFalse(first.current(p.other));
   assertNotNull(f.hub.addClanCandidate(f.session.sourceCharacterIdentity(),"Candidate"));assertFalse(first.current(f.session));assertFalse(first.sameContext(f.session));
   var reselect=f.hub.nativeClanSnapshot(f.session);assertTrue(f.hub.selectCharacter(f.session,1,p.source,f.accounts.characters("test").get(0)));assertFalse(reselect.current(f.session));assertFalse(reselect.sameContext(f.session));
   var scene=f.hub.nativeClanSnapshot(f.session);NativeWarehouseCompatibilityTest.set(f.session,"currentRoomId",p.room+1);assertFalse(scene.current(f.session));assertFalse(scene.sameContext(f.session));NativeWarehouseCompatibilityTest.set(f.session,"currentRoomId",p.room);
   var disconnect=f.hub.nativeClanSnapshot(f.session);f.hub.disconnect(f.session);assertFalse(disconnect.current(f.session));assertFalse(disconnect.sameContext(f.session));
  }
 }
 @Test void lateSQLSuccessAfterSameKeyReselectDoesNotInstallOrPublishOldContext()throws Exception {
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;String owner=f.session.sourceCharacterIdentity();f.hub.reconcileSqlClan(owner,snapshot(f.session.playerName(),1));assertNotNull(f.hub.addClanCandidate(owner,"Candidate"));
   AccountRepository original=f.accounts;boolean[] sourceCommitted={false};AccountRepository proxy=(AccountRepository)Proxy.newProxyInstance(AccountRepository.class.getClassLoader(),new Class<?>[]{AccountRepository.class},(x,m,a)->{
    if(m.getName().equals("repositoryForClassicSource"))return x;
    if(m.getName().equals("joinClan")){sourceCommitted[0]=true;assertTrue(f.hub.selectCharacter(f.session,1,p.source,f.accounts.characters("test").get(0)));return new RepositoryClanMutation(RepositoryClanMutationStatus.SUCCESS,snapshot(f.session.playerName(),2));}
    return m.invoke(original,a);
   });NativeWarehouseCompatibilityTest.set(f.session,"accounts",proxy);f.call("admitClanCandidate",name(Protocol.C_LITO_CLAN_ADMIT,"Candidate"));assertTrue(sourceCommitted[0]);assertTrue(f.drain().isEmpty());assertTrue(f.hub.clanData(owner).members().stream().anyMatch(r->r.name().equals("Candidate")&&r.role()==Protocol.LitoClanRole.CANDIDATE));
  }
 }
 @Test void staleIncrementalCandidateReplyCannotReachReselectedContext()throws Exception {
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;f.hub.reconcileSqlClan(f.session.sourceCharacterIdentity(),snapshot(f.session.playerName(),1));
   var candidate=f.hub.addClanCandidate(f.session.sourceCharacterIdentity(),"Candidate");var cap=f.hub.nativeClanSnapshot(f.session);assertTrue(cap.data().members().contains(candidate));
   assertTrue(f.hub.selectCharacter(f.session,1,p.source,f.accounts.characters("test").get(0)));f.call("sendNativeClanMemberAdded",cap,candidate);assertTrue(f.drain().isEmpty());
  }
 }
 static byte[] name(int op,String name){return new Protocol.Writer().c(op).s(name).bytes();}
 static int count(byte[] packet){assertEquals(0xF6,Byte.toUnsignedInt(packet[0]));int i=1;while(packet[i++]!=0){}return Byte.toUnsignedInt(packet[i]);}
 static RepositoryClanSnapshot snapshot(String owner,int count){var rows=new ArrayList<RepositoryClanMember>();rows.add(new RepositoryClanMember(10000,owner,4,10,"King",""));for(int i=0;i<count-1;i++)rows.add(new RepositoryClanMember(10001+i,String.format("M%03d",i),2,7,"Member",""));return new RepositoryClanSnapshot(true,40001,"Clan",10000,owner,0,0,7,false,"Welcome",0,false,rows);}
}
