package local.lito381.server;
/** Actual StartTimer_Skill bootstrap cadence, carried by the original Hub clock. */
final class NativeSourceCubeClock {
 static final long PERIOD=1_000_000_000L;
 record Phase(long next,long scan){}
 static String field(int type){return switch(type){case 0->"nativeSourceCubeBurnPhase";case 1->"nativeSourceCubeQuakePhase";case 2->"nativeSourceCubeShockPhase";case 3->"nativeSourceCubeHarmonyPhase";default->throw new IllegalArgumentException("invalid cube clock type");};}
 static int type(Object engine){return engine instanceof BattleCubeIgnitionEffects?0:engine instanceof BattleCubeQuakeEffects?1:engine instanceof BattleCubeShockEffects?2:engine instanceof BattleCubeHarmonyEffects?3:-1;}
 static Phase advance(Phase phase,long now){if(phase==null)return null;if(now<0)throw new IllegalArgumentException("invalid cube scan clock");if(now<phase.next())return new Phase(phase.next(),-1);long steps=(now-phase.next())/PERIOD+1;return new Phase(Math.addExact(phase.next(),Math.multiplyExact(steps,PERIOD)),now);}
 static boolean due(Phase phase,long now){return phase!=null&&phase.scan()==now;}
 /** Before the original ground clock task can be scheduled; no new worker/timer. */
 static void init(GameHub hub){long now=System.nanoTime();for(int i=0;i<4;i++)CombatAccess.set(hub,field(i),new Phase(now,-1));}
 /** Original synchronized per-type collector calls this once, before iterating any room. */
 static void begin(GameHub hub,long now,int type){if(!Thread.holdsLock(hub))throw new IllegalStateException("original Hub clock monitor required");if((boolean)CombatAccess.field(hub,"nativeStaticMotionClosed")||!NativePlayerLifetime.of(hub).admits())return;var name=field(type);CombatAccess.set(hub,name,advance((Phase)CombatAccess.field(hub,name),now));}
 /** Gate only the original new-effect scan; installed status callbacks/expiry still run every native tick. */
 static int compare(long now,long originalNext,Object engine){if(!CombatRules.loaded()||!(NativeSourceCubeOrigin.scene(engine) instanceof NativeSourceCubeOrigin.Scene scene))return Long.compare(now,originalNext);int type=type(engine);if(type<0||!Thread.holdsLock(scene.hub())||!Thread.holdsLock(engine)||(boolean)CombatAccess.field(scene.hub(),"nativeStaticMotionClosed")||!NativePlayerLifetime.of(scene.hub()).admits())return -1;return due((Phase)CombatAccess.field(scene.hub(),field(type)),now)?0:-1;}
 static void close(GameHub hub){for(int i=0;i<4;i++)CombatAccess.set(hub,field(i),null);}
 private NativeSourceCubeClock(){}
}
