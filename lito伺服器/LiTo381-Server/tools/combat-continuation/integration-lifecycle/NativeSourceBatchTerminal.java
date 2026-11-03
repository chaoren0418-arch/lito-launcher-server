package local.lito381.server;
import java.util.List;
/** Runs only after the actual original emit/rescue/completion span has terminated. */
final class NativeSourceBatchTerminal {
 static Throwable note(Throwable primary,Throwable first,Throwable next){if(primary!=null){if(primary!=next)primary.addSuppressed(next);return first;}if(first==null)return next;if(first!=next)first.addSuppressed(next);return first;}
 static Throwable list(Object batch,String field,Throwable primary,Throwable first){try{((List<?>)CombatAccess.field(batch,field)).clear();}catch(Throwable failure){return note(primary,first,failure);}return first;}
 static Throwable slot(Object batch,String field,Throwable primary,Throwable first){try{CombatAccess.set(batch,field,null);}catch(Throwable failure){return note(primary,first,failure);}return first;}
 static void finish(Object batch,Throwable primary)throws Throwable{Throwable failure=null;failure=list(batch,"outputs",primary,failure);failure=list(batch,"completions",primary,failure);failure=slot(batch,"target",primary,failure);failure=slot(batch,"targetBinding",primary,failure);failure=slot(batch,"material",primary,failure);failure=slot(batch,"targetReceipt",primary,failure);if(failure!=null)throw failure;}
 private NativeSourceBatchTerminal(){}
}
