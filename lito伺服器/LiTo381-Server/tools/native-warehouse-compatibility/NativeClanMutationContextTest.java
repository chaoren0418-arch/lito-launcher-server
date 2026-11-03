package local.lito381.server;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NativeClanMutationContextTest {
 @TempDir Path temporary;
 static byte[] rank(String name,int role){return new Protocol.Writer().c(Protocol.C_RANK_CONTROL).c(1).c(role).s(name).bytes();}
 static RepositoryClanSnapshot clan(NativePlayerTradeBoundaryTest.Pair p,int otherRole){
  var f=p.f;return new RepositoryClanSnapshot(true,40001,"Clan",f.session.playerObjectId(),f.session.playerName(),0,0,7,false,"Welcome",0,false,
   List.of(new RepositoryClanMember(f.session.playerObjectId(),f.session.playerName(),4,10,"King",""),new RepositoryClanMember(p.other.playerObjectId(),p.other.playerName(),otherRole,7,"Member","")));
 }
 interface Change {RepositoryClanMutation apply(Object[] args)throws Exception;}
 static void source(NativePlayerTradeBoundaryTest.Pair p,ClientSession session,Change change)throws Exception{
  AccountRepository repository=NativeLetterSqlFaultTest.proxy(AccountRepository.class,(proxy,m,a)->{
   if(m.getName().equals("repositoryForClassicSource"))return proxy;
   if(m.getName().equals("changeClanMember"))return change.apply(a);
   return m.invoke(p.f.accounts,a);
  });NativeWarehouseCompatibilityTest.set(session,"accounts",repository);
 }
 static void call(ClientSession session,String name,byte[] payload)throws Exception{
  Method m=ClientSession.class.getDeclaredMethod(name,byte[].class);m.setAccessible(true);m.invoke(session,(Object)payload);
 }
 void lateRole(boolean baseline)throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;String original=f.session.sourceCharacterIdentity();f.hub.reconcileSqlClan(original,clan(p,2));AtomicBoolean committed=new AtomicBoolean();
   source(p,f.session,a->{committed.set(true);assertTrue(f.hub.selectCharacter(f.session,2,"other-source",f.accounts.characters("test").get(0)));NativeWarehouseCompatibilityTest.set(f.session,"selectedClassicServerName","other-source");return new RepositoryClanMutation(RepositoryClanMutationStatus.SUCCESS,clan(p,1));});
   f.call("changeClanRole",rank(p.other.playerName(),1));assertTrue(committed.get());assertEquals(Protocol.LitoClanRole.MEMBER,f.hub.clanData(original).members().get(1).role());
   if(baseline){assertEquals(2,f.hub.clanData(f.session.sourceCharacterIdentity()).members().size());assertFalse(f.drain().isEmpty());}
   else{assertEquals(GameHub.ClanData.empty(),f.hub.clanData(f.session.sourceCharacterIdentity()));assertTrue(f.drain().isEmpty());assertTrue(p.drain().isEmpty());}
  }
 }
 @Test void baselineLateSqlRoleSuccessReconcilesWrongSourceAndPublishes()throws Exception{lateRole(true);}
 @Test void lateSqlRoleSuccessCannotReconcileOrPublishIntoChangedSource()throws Exception{lateRole(false);}
 void lateDelete(boolean baseline)throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;f.hub.reconcileSqlClan(f.session.sourceCharacterIdentity(),clan(p,2));
   source(p,f.session,a->{assertTrue(f.hub.selectCharacter(p.other,1,p.source,p.character));return RepositoryClanMutation.unavailable();});
   f.call("deleteClanMember",NativeClanSnapshotSafetyTest.name(Protocol.C_LITO_CLAN_DELETE,p.other.playerName()));
   assertEquals(1,f.hub.clanData(f.session.sourceCharacterIdentity()).members().size());assertEquals(1,f.drain().size());
   assertEquals(baseline?1:0,p.drain().size());
  }
 }
 @Test void baselineCachedDeleteRecipientReceivesAfterActualReselect()throws Exception{lateDelete(true);}
 @Test void deletedReplySkipsRecipientReselectedDuringMutation()throws Exception{lateDelete(false);}
 @Test void actualOwnerOnlyAndReadOnlyRejectionDoNotMutate()throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;String owner=f.session.sourceCharacterIdentity();var before=f.hub.reconcileSqlClan(owner,clan(p,2));AtomicInteger calls=new AtomicInteger();
   source(p,p.other,a->{calls.incrementAndGet();return RepositoryClanMutation.unavailable();});
   call(p.other,"changeClanRole",rank(f.session.playerName(),1));assertEquals(0,calls.get());assertEquals(before,f.hub.clanData(owner));assertTrue(p.drain().isEmpty());
   source(p,f.session,a->{calls.incrementAndGet();return RepositoryClanMutation.readOnly(clan(p,1));});f.call("changeClanRole",rank(p.other.playerName(),1));assertEquals(1,calls.get());assertEquals(before,f.hub.clanData(owner));assertTrue(f.drain().isEmpty());assertTrue(p.drain().isEmpty());
   f.hub.disconnect(f.session);f.call("changeClanRole",rank(p.other.playerName(),1));assertEquals(1,calls.get());assertEquals(before,f.hub.clanData(owner));
  }
 }
 @Test void normalRoleAndDeleteKeepExistingF6F9WireAndControlZeroNoReply()throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;String owner=f.session.sourceCharacterIdentity();f.hub.reconcileSqlClan(owner,clan(p,2));
   f.call("changeClanRole",new Protocol.Writer().c(Protocol.C_RANK_CONTROL).c(0).bytes());assertTrue(f.drain().isEmpty());assertTrue(p.drain().isEmpty());
   f.call("changeClanRole",rank(p.other.playerName(),1));var own=f.drain();var other=p.drain();assertEquals(1,own.size());assertEquals(1,other.size());assertEquals(0xF6,own.get(0)[0]&255);assertArrayEquals(own.get(0),other.get(0));
   assertEquals(1,f.hub.clanRole(p.other.sourceCharacterIdentity()));
   f.call("deleteClanMember",NativeClanSnapshotSafetyTest.name(Protocol.C_LITO_CLAN_DELETE,p.other.playerName()));assertEquals(2,f.hub.clanData(owner).members().size());assertTrue(f.drain().isEmpty()); // officer cannot be C_F7 removed
   f.call("changeClanRole",rank(p.other.playerName(),2));f.drain();p.drain();
   f.call("deleteClanMember",NativeClanSnapshotSafetyTest.name(Protocol.C_LITO_CLAN_DELETE,p.other.playerName()));own=f.drain();other=p.drain();assertEquals(1,own.size());assertEquals(1,other.size());assertArrayEquals(Protocol.clanMemberDeleted(p.other.playerName()),own.get(0));assertArrayEquals(own.get(0),other.get(0));assertEquals(1,f.hub.clanData(owner).members().size());
  }
 }
 @Test void stalePrivateDeletionAndRosterCapsDoNotPublish()throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;f.hub.reconcileSqlClan(f.session.sourceCharacterIdentity(),clan(p,2));var removed=f.hub.removeClanMember(f.session.sourceCharacterIdentity(),p.other.playerName());var cap=f.hub.nativeClanSnapshot(f.session);
   assertTrue(f.hub.selectCharacter(f.session,1,p.source,f.accounts.characters("test").get(0)));
   f.call("sendNativeClanMemberDeleted",cap,removed);f.call("sendClanInfo",cap);assertTrue(f.drain().isEmpty());
   cap=f.hub.nativeClanSnapshot(f.session);assertNotNull(f.hub.addClanCandidate(f.session.sourceCharacterIdentity(),"Candidate"));f.call("sendNativeClanMemberDeleted",cap,removed);f.call("sendClanInfo",cap);assertTrue(f.drain().isEmpty());
  }
 }
 @Test void recipientSourceChangeDuringSuccessfulRoleMutationGetsNoReplacementRoster()throws Exception{
  try(var p=new NativePlayerTradeBoundaryTest.Pair(temporary)){
   var f=p.f;f.hub.reconcileSqlClan(f.session.sourceCharacterIdentity(),clan(p,2));
   source(p,f.session,a->{var updated=clan(p,1);assertTrue(f.hub.selectCharacter(p.other,2,"other-source",p.character));NativeWarehouseCompatibilityTest.set(p.other,"selectedClassicServerName","other-source");return new RepositoryClanMutation(RepositoryClanMutationStatus.SUCCESS,updated);});
   f.call("changeClanRole",rank(p.other.playerName(),1));assertEquals(1,f.drain().size());assertTrue(p.drain().isEmpty());assertEquals(GameHub.ClanData.empty(),f.hub.clanData(p.other.sourceCharacterIdentity()));
  }
 }
}
