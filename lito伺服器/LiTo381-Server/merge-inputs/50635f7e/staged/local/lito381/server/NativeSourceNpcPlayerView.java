package local.lito381.server;
import java.util.*;
/** Original PVE player discovery, carried only by the original invocation and existing tick Origin. */
final class NativeSourceNpcPlayerView {
 record Builder(GameHub hub,long room,IdentityHashMap<ClientSession,NativeSourceRawDamage.Member> members,Set<ClientSession> audience){}
 static Builder start(Object owner,Object room){if(!CombatRules.loaded())return null;var hub=(GameHub)owner;if(!Thread.holdsLock(hub))throw new IllegalStateException("NPC player discovery lost original Hub monitor");var b=new Builder(hub,(long)CombatAccess.field(room,"id"),new IdentityHashMap<>(),Collections.newSetFromMap(new IdentityHashMap<>()));for(var pc:(Set<ClientSession>)CombatAccess.field(room,"members")){b.audience().add(pc);remember(pc,b);}return b;}
 static void remember(ClientSession pc,Builder b){if(b==null||b.members().containsKey(pc))return;if(!Thread.holdsLock(b.hub()))throw new IllegalStateException("NPC player discovery outside original Hub monitor");b.members().put(pc,NativeSourceAcceptedPlayers.ingress(pc));}
 static Object finish(ClientSession pc,Builder b){if(pc==null||b==null)return pc;var kept=new IdentityHashMap<ClientSession,NativeSourceRawDamage.Member>();for(var member:b.members().entrySet())if(b.audience().contains(member.getKey())||member.getKey()==pc)kept.put(member.getKey(),member.getValue());return new NativeSourceNpcPlayer.Origin(b.hub(),b.room(),pc,b.members().get(pc),Collections.unmodifiableMap(kept));}
 static ClientSession target(Object choice){return choice instanceof NativeSourceNpcPlayer.Origin o?o.primary():choice instanceof ClientSession pc?pc:null;}
 static boolean current(Object choice){if(choice==null||choice instanceof ClientSession)return true;if(!(choice instanceof NativeSourceNpcPlayer.Origin o))return false;var m=o.original();return o.primary()!=null&&m!=null&&m.pc==o.primary()&&m.binding.phase().equals("BATTLE")&&m.binding.room()==o.room()&&CombatAccess.field(o.primary(),"hub")==o.hub()&&m.current(false);}
 static boolean entry(GameHub hub,ClientSession pc,Object room,Object choice){if(choice==null)return true;if(choice instanceof ClientSession old)return old==pc;return choice instanceof NativeSourceNpcPlayer.Origin o&&o.hub()==hub&&o.room()==(long)CombatAccess.field(room,"id")&&o.primary()==pc&&current(choice);}
 static boolean both(boolean original,Object choice){return original&&current(choice);}
 static boolean identity(Object current,Object original,Object choice){return current==original&&NativeSourceNpcPlayerView.current(choice);}
 static Object capture(GameHub.MonsterAiTick tick,Object choice){if(choice==null||choice instanceof ClientSession)return NativeSourceNpcPlayer.capture(tick);if(choice instanceof NativeSourceNpcPlayer.Origin o&&o.primary()==tick.target()&&o.room()==tick.roomId())return o;return new NativeSourceNpcPlayer.Origin(null,-1,null,null,Map.of());}
 static boolean dispatch(GameHub hub,List<GameHub.MonsterAiTick> out,Object room,BattleRules.Mode mode,long id,int force,int sx,int sy,ClientSession target,long now,int tx,int ty,String source,Object snapshot,Object choice){return NativeSourceNpcEscape.nativeSourceNpcPlayerFirstHandled(hub,out,room,mode,id,force,sx,sy,target,now,tx,ty,source,snapshot,choice);}
 private NativeSourceNpcPlayerView(){}
}
