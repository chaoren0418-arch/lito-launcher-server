package local.lito381.server;
/** Character none-target projection keeps Source's independent raw item selector unchanged. */
final class NativeSourceObjectTargetRequest {
 static boolean itemSelector(int skill){return skill==73||skill==100||skill==21||skill==12||skill==107;}
 static Protocol.ObjectSpellRequest request(Protocol.ObjectSpellRequest request,ClientSession pc,int skill){return request(NativeAreaMaterialAction.current.get(),pc,request,skill);}
 static Protocol.ObjectSpellRequest request(NativeAreaMaterialAction.Context c,ClientSession pc,Protocol.ObjectSpellRequest request,int skill){
  if(request==null||request.skillId()!=skill||itemSelector(skill)||!NativeSourcePlayerFactories.scoped(c)||c.pc!=pc||NativeSkillTargetMetadata.size()==0)return request;
  NativeSkillTargetMetadata.Row row;try{row=NativeSkillTargetMetadata.row(skill);}catch(IllegalStateException missing){return request;}
  if(!"none".equals(row.target())||NativeSourceSignedRecovery.member(pc,c.caster,c)==null)return request;long id=c.caster.object();return request.targetObjectId()==id?request:new Protocol.ObjectSpellRequest(request.row(),request.column(),id);
 }
 private NativeSourceObjectTargetRequest(){}
}
