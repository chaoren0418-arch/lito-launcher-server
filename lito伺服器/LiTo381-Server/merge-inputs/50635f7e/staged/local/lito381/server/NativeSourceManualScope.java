package local.lito381.server;
import java.util.concurrent.locks.ReentrantLock;
/** Original manual critical sections borrow the existing scope only for their original lock span. */
final class NativeSourceManualScope {
 static NativePlayerActionScope.Ticket scope(ClientSession pc){return NativeSourcePairedScope.enabled(pc)?NativePlayerActionScope.enter(pc):null;}
 static void finish(NativePlayerActionScope.Ticket ticket,Throwable failure){if(ticket==null)return;if(failure==null){ticket.close();return;}try{ticket.close();}catch(Throwable closing){if(closing!=failure)failure.addSuppressed(closing);}}
 static NativePlayerActionScope.Ticket enterState(ClientSession pc){var ticket=scope(pc);try{NativeLightAction.enterState(pc);return ticket;}catch(RuntimeException|Error failure){finish(ticket,failure);throw failure;}}
 static NativePlayerActionScope.Ticket enterRespawn(ClientSession pc){var ticket=scope(pc);try{NativeLightAction.enterRespawn(pc);return ticket;}catch(RuntimeException|Error failure){finish(ticket,failure);throw failure;}}
 static NativePlayerActionScope.Ticket lock(ReentrantLock lock,ClientSession pc){var ticket=scope(pc);try{lock.lock();return ticket;}catch(RuntimeException|Error failure){finish(ticket,failure);throw failure;}}
 static void exitState(ClientSession pc,NativePlayerActionScope.Ticket ticket,Throwable failure){Throwable primary=failure;try{NativeLightAction.exitState(pc);}catch(RuntimeException|Error error){primary=error;throw error;}finally{finish(ticket,primary);}}
 static void exitRespawn(ClientSession pc,NativePlayerActionScope.Ticket ticket,Throwable failure){Throwable primary=failure;try{NativeLightAction.exitRespawn(pc);}catch(RuntimeException|Error error){primary=error;throw error;}finally{finish(ticket,primary);}}
 static void unlock(ReentrantLock lock,ClientSession pc,NativePlayerActionScope.Ticket ticket,Throwable failure){Throwable primary=failure;try{lock.unlock();}catch(RuntimeException|Error error){primary=error;throw error;}finally{finish(ticket,primary);}}
 private NativeSourceManualScope(){}
}
