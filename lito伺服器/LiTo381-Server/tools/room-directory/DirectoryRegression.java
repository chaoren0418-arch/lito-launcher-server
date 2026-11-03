package local.lito381.server;
import java.nio.file.*;import java.net.*;import java.lang.reflect.*;import java.util.*;
public class DirectoryRegression {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static Object get(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object call(Object o,String n,Class<?>[] t,Object...a)throws Exception{var m=o.getClass().getDeclaredMethod(n,t);m.setAccessible(true);try{return m.invoke(o,a);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}}
 static void init(SourceLoginRegression.Fixture f,String name,int channel,String source)throws Exception {
  var m=Regression.class.getDeclaredMethod("character",String.class,int.class,long.class);m.setAccessible(true);var ch=(CharacterSummary)m.invoke(null,name,0,1000L+name.hashCode());
  m=Regression.class.getDeclaredMethod("initialize",ClientSession.class,CharacterSummary.class);m.setAccessible(true);m.invoke(null,f.session,ch);
  set(f.session,"selectedClassicServerName",source);set(f.session,"currentChannelId",channel);set(f.session,"hubChannelId",channel);
  Object phase=get(f.session,"phase");set(f.session,"phase",Enum.valueOf((Class)phase.getClass(),"CHANNEL"));f.hub.enterChannel(channel,f.session);f.client.setSoTimeout(300);
 }
 static List<byte[]> drain(SourceLoginRegression.Fixture f,Protocol.CipherState cipher)throws Exception {
  var rows=new ArrayList<byte[]>();try{while(true)rows.add(cipher.decrypt(Protocol.readFrame(f.client.getInputStream(),8192)));}catch(SocketTimeoutException e){}return rows;
 }
 static boolean room(List<byte[]> frames,long id,String name,int users)throws Exception {
  for(var b:frames){var r=new Protocol.Reader(b);if(r.c()!=0xcc)continue;if(r.c()!=0)continue;long key=r.d();String text=r.s();r.c();int count=r.h();if(key==id&&text.equals(name)&&count==users)return true;}return false;
 }
 static byte[] settings(int opcode,String name){return new Protocol.Writer().c(opcode).s(name).c(8).s("").h(8).bytes();}
 public static void main(String[] args)throws Exception {
  var config=ServerConfig.load(Path.of(args[0]),"standard");var root=Path.of(args[1]);Files.createDirectories(root);
  var state=new GameStateStore(root.resolve("unused"),false,true);var accounts=new MemoryAccountRepository("fixture","fixture","",0,1,40,52);
  try(var capture=new CaptureLog(root.resolve("capture"));var hub=new GameHub(config,state,accounts);
      var owner=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var peer=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,1,ClientSession.Endpoint.GAME);
      var waiting=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME);
      var outside=new SourceLoginRegression.Fixture(config,accounts,state,capture,hub,0,ClientSession.Endpoint.GAME)) {
   init(owner,"Owner",0,config.classicServerNames().get(0));init(peer,"Peer",0,config.classicServerNames().get(1));init(waiting,"Waiting",0,config.classicServerNames().get(0));init(outside,"Outside",1,config.classicServerNames().get(0));
   var a=new Protocol.CipherState(0x37110001);var b=new Protocol.CipherState(0x37110001);var c=new Protocol.CipherState(0x37110001);var d=new Protocol.CipherState(0x37110001);
   call(owner.session,"createRoom",new Class[]{byte[].class},settings(0xcc,"CrossSourceRoom"));long id=(long)get(owner.session,"currentRoomId");check(id!=0,"create failed");
   check(room(drain(peer,b),id,"CrossSourceRoom",1),"other-source lobby did not receive created room");check(room(drain(waiting,c),id,"CrossSourceRoom",1),"same-source lobby not refreshed");check(drain(outside,d).isEmpty(),"room leaked to different channel");drain(owner,a);
   call(owner.session,"editRoom",new Class[]{byte[].class},settings(0xdd,"RenamedRoom"));check(room(drain(peer,b),id,"RenamedRoom",1),"rename not broadcast");drain(waiting,c);drain(owner,a);
   call(peer.session,"joinRoom",new Class[]{byte[].class,String.class},new Protocol.Writer().c(0xcf).d(id).s("").bytes(),"regression");check(get(peer.session,"phase").toString().equals("ROOM"),"cross-source join failed");check(room(drain(waiting,c),id,"RenamedRoom",2),"join count not broadcast");drain(owner,a);drain(peer,b);
   call(peer.session,"exitRoom",new Class[]{});check(room(drain(waiting,c),id,"RenamedRoom",1),"leave count not broadcast");check(drain(outside,d).isEmpty(),"updates leaked channel");drain(owner,a);drain(peer,b);
   // Closing a listed recipient's server socket must not block others.
   peer.server.close();call(owner.session,"editRoom",new Class[]{byte[].class},settings(0xdd,"PeerClosed"));check(room(drain(waiting,c),id,"PeerClosed",1),"dead peer blocked following recipient");
   hub.enterChannel(0,outside.session);set(outside.session,"currentChannelId",0);set(outside.session,"hubChannelId",0);drain(owner,a);
   call(waiting.session,"createRoom",new Class[]{byte[].class},settings(0xcc,"SecondRoom"));long second=(long)get(waiting.session,"currentRoomId");check(second!=id,"second room reused occupied default");
   check(room(drain(outside,d),second,"SecondRoom",1),"new independent room not broadcast");check(drain(owner,a).stream().noneMatch(x->(x[0]&255)==0xcc),"lobby refresh sent to room UI");drain(waiting,c);
   call(waiting.session,"exitRoom",new Class[]{});var removed=drain(outside,d);check(removed.stream().anyMatch(x->{var r=new Protocol.Reader(x);return r.c()==0xcf&&r.d()==second;}),"room deletion not broadcast");check(room(removed,id,"PeerClosed",1),"remaining room not refreshed");
   System.out.println("PASS: real create/edit/join/leave dispatch; cross-source lobby updates; new/default room; counts; deletion; channel/phase isolation; disconnected recipient isolation; no SQL writes");
  }
 }
}
