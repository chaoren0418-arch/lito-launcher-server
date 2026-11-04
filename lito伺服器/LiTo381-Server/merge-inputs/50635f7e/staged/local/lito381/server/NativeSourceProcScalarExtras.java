package local.lito381.server;
/** Additional Source scalar effects on the existing accepted-PC dispatcher and native state consumers. */
final class NativeSourceProcScalarExtras {
 static boolean supports(int id){return id==104||id==173||id==174||id==206;}
 static String expiry(int id){return switch(id){case 104->"venomResistExpiresAtNanos";case 173->"polluteWaterExpiresAtNanos";case 174->"strikerGaleExpiresAtNanos";case 206->"concentrationExpiresAtNanos";default->null;};}
 static boolean activate(BattleTimedStatus status,int id,long now,int seconds){switch(id){case 104->status.activateVenomResist(now,seconds);case 173->status.activatePolluteWater(now,seconds);case 174->status.activateStrikerGale(now,seconds);case 206->status.activateConcentration(now,seconds);default->{return false;}}return true;}
 private NativeSourceProcScalarExtras(){}
}
