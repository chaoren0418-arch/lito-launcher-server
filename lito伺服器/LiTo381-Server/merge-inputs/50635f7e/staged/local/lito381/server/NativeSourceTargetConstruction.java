package local.lito381.server;
/** Preserve Source makeTargetList bypasses before applying isTarget class-mask projection. */
final class NativeSourceTargetConstruction {
 static int capture(NativeSkillTargetMetadata.Row row,int skill){
  boolean only=row.targetTo()==0&&(row.type()&64)!=64&&skill!=68;
  boolean area=row.area()!=0&&skill!=49&&!row.target().equals("attack")&&row.type()!=64;
  // Source recognizes only the five low mask bits. Two otherwise unused bits carry immutable construction flags.
  return(row.targetTo()&31)|(only?32:0)|(area?64:0);
 }
 static boolean reject(NativeSourceNpcFactoryType.Stamp selected,int maximum,NativeSourceNpcFactoryType.Stamp primary,boolean player,boolean none,int type,int skill,Object node,Integer captured){
  var c=NativeAreaMaterialAction.current.get();
  if(captured!=null&&NativeSourcePlayerFactories.scoped(c)){
   if((captured&32)!=0)return node!=c.pc;
   if((captured&64)!=0&&node==c.pc)return false;
   captured=Integer.valueOf(captured&31);
  }
  return NativeSourceTargetMask.reject(selected,maximum,primary,player,none,type,skill,node,captured);
 }
 private NativeSourceTargetConstruction(){}
}
