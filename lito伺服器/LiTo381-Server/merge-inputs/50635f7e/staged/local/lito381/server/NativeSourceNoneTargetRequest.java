package local.lito381.server;
/** Source target=none selects the original caster, never a packet-selected later actor or pose. */
final class NativeSourceNoneTargetRequest {
 static Protocol.AttackSpellRequest request(Protocol.AttackSpellRequest request,ClientSession pc,int skill){return request(NativeAreaMaterialAction.current.get(),pc,request,skill);}
 static Protocol.AttackSpellRequest request(NativeAreaMaterialAction.Context c,ClientSession pc,Protocol.AttackSpellRequest request,int skill){
  if(request==null||request.skillId()!=skill||!NativeSourcePlayerFactories.scoped(c)||c.pc!=pc||NativeSkillTargetMetadata.size()==0)return request;
  NativeSkillTargetMetadata.Row row;try{row=NativeSkillTargetMetadata.row(skill);}catch(IllegalStateException missing){return request;}
  if(!"none".equals(row.target()))return request;var source=NativeSourceSignedRecovery.member(pc,c.caster,c);if(source==null)return request;long id=c.caster.object();int x=source.x,y=source.y;
  return request.targetObjectId()==id&&request.x()==x&&request.y()==y?request:new Protocol.AttackSpellRequest(request.row(),request.column(),id,x,y);
 }
 private NativeSourceNoneTargetRequest(){}
}
