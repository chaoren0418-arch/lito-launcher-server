"""Fixture-only compatibility with the existing paired receipt/publication contract."""
def adapt(text):
 receipt='NativePlayerVitalCompletions.pending(life)==1'
 old='need(self.contains(HexFormat.of().formatHex(Protocol.manaPoint(1000-mpCost,1000))),"caster still receives committed MP after target retirement");'
 assert text.count(receipt)==text.count(old)==1
 replacement='''need(self.isEmpty()&&(int)get(pc,"playerMp")==1000-mpCost&&pc.nativeDeInventory(0).countCatalog(item)==3,"retired pair emits no old batch; original accepted MP/material cost retained once");
    try(var next=new SourceActionPairProbe.Wire(pc,peer)){var binding=NativePlayerCommit.Binding.capture(pc);NativeSourcePlayerActionLock.run(pc,()->need(NativePlayerCommit.debitMp(pc,binding,1)==1,"independent native MP commit after pair retirement"));need(collect(next).equals(List.of(HexFormat.of().formatHex(Protocol.manaPoint(999-mpCost,1000)))),"independent native MP completion drains exactly once after retired pair");}'''
 return text.replace(receipt,'NativePlayerVitalCompletions.pending(life)>0').replace(old,replacement)

def adapt_earth(text):
 text=adapt(text)
 lock='(mode==0||mode==1?peer:pc)'
 shutdown='else NativePlayerLifetime.shutdown(hub,false,0);'
 assert text.count(lock)==text.count(shutdown)==1
 return text.replace(lock,'(mode==0||mode==1||mode==4?peer:pc)').replace(shutdown,'else {need(Arrays.stream(worker.getStackTrace()).anyMatch(frame->frame.getClassName().equals("local.lito381.server.NativePairedCastSafety")&&frame.getMethodName().equals("run")),"STOPPED fixture waits after actual pair receipt reservation");NativePlayerLifetime.shutdown(hub,false,0);}')
