package local.lito381.server;
/** Source LOS type/through policy on original geometry operands; no discovery or retained state. */
final class NativeSourceTargetLos {
 static boolean requires(boolean proc,int type,boolean through){boolean attack=(type&64)==64;return attack||!proc&&!through&&type!=2&&type!=32;}
 static boolean enabled(NativeAreaMaterialAction.Context c,ClientSession pc){return NativeAreaMaterialAction.current.get()==c&&NativeSourcePlayerFactories.scoped(c)&&c.pc==pc&&NativeSourceSignedRecovery.member(pc,c.caster,c)!=null;}
 static boolean skips(NativeSkillTargetMetadata.Row row,NativeAreaMaterialAction.Context c){return enabled(c,c==null?null:c.pc)?!requires(false,row.type(),row.through()):row.through();}
 static boolean line(ArenaMapCatalog.Arena arena,int x,int y,int tx,int ty,ClientSession pc,int skill){var c=NativeAreaMaterialAction.current.get();if(enabled(c,pc)&&NativeSkillTargetMetadata.size()>0){NativeSkillTargetMetadata.Row row;try{row=NativeSkillTargetMetadata.row(skill);}catch(IllegalStateException missing){return BattleSpellCatalog.hasNativeLineOfSight(arena,x,y,tx,ty);}if(!requires(false,row.type(),row.through()))return true;}return BattleSpellCatalog.hasNativeLineOfSight(arena,x,y,tx,ty);}
 private NativeSourceTargetLos(){}
}
