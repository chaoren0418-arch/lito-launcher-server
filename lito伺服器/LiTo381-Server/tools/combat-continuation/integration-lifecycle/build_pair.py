from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';C=R.parent;D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
parent=read(D/'build-current.json');jar=Path(parent['runtime']['path']);assert sha(jar)==parent['runtime']['sha256']=='1a67d8df84030dd457e8e602ce0c62ba7b61b02dffd7b9df7526a73c1dc1f475';seed=read(Path(parent['manifest']['path']))
run=D/'pair-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';original=inputs/'original';sources=inputs/'sources';original.mkdir(parents=True);sources.mkdir()
for p in [Path(__file__),R/'PairedLifecycleMethods.java',R/'NativePairedCastSafety.java']:shutil.copyfile(p,inputs/p.name)
for p,digest in [(C/'cancellation-material/build/inputs/sources/NativeLightAction.java','76d7cbc708c6a797c040791c914ad9ea90c55c26050fa06b27fdf42745f1eecf'),(C/'build/inputs/sources/NativeCastMaterial.java','2fa9bd4e6bdc57d8fccb54131d47c3d8c82dfa2c13d08b6dc532deccfcef9026')]:assert sha(p)==digest;shutil.copyfile(p,original/p.name)
def once(text,old,new):
 assert text.count(old)==1,old
 return text.replace(old,new)
# Later area/NPC stages prepend these two exact capture ports with ASM. Preserve
# them in C0/C1; selected-method byte comparison proves this rebase is exact.
p=original/'NativeLightAction.java';shutil.copyfile(p,inputs/'upstream-NativeLightAction.java');t=p.read_text('utf-8');t=once(t,' static boolean deferSelfSend(ClientSession receiver,byte[] plain,String note){',' static boolean deferSelfSend(ClientSession receiver,byte[] plain,String note){\n  if(NativeAreaMaterialAction.captureOutput(receiver,plain,note))return true;if(NativeCancellationNpcMaterialAction.captureOutput(receiver,plain,note))return true;');p.write_text(t,encoding='utf-8')
t=(original/'NativeLightAction.java').read_text('utf-8')
t=once(t,'boolean captureSelf;ClientSession target;','boolean captureSelf;NativePlayerVitalCompletions.Receipt targetReceipt;ClientSession target;')
t=once(t,'  public void run(){boolean published=false;Throwable failure=null;','  public void run(){if(targetReceipt==null)emit();else targetReceipt.run(this::emit);}\n  private void emit(){boolean published=false;Throwable failure=null;')
t=once(t,'if(!NativeRolePublication.allowPublication(pc))return;','if(!accepted.binding.matches(pc)||!NativeRolePublication.allowPublication(pc)||target!=null&&!targetBinding.matches(target))return;')
t=once(t,'if(NativePetWorldLifetime.captureOutput(receiver,plain,note))return true;','var receiverBinding=NativePlayerCommit.Binding.capture(receiver);if(NativePetWorldLifetime.captureOutput(receiver,plain,note))return true;')
t=once(t,'batch.outputs.add(()->{if(batch.target!=null','batch.outputs.add(()->{if(!receiverBinding.matches(receiver)||(boolean)CombatAccess.field(receiver,NativeDeItemApplication.DISCONNECTED))return;if(batch.target!=null')
a=t.index(' static void commitPair(');e=t.index(' static void deferScoped(',a)
t=t[:a]+''' static void commitPair(ClientSession pc,ClientSession target,NativeSelfCastAction.Task body)throws IOException {
  var targetBinding=NativePlayerCommit.Binding.capture(target);
  var targetReceipt=target==pc?null:NativePlayerVitalCompletions.reserve(target);if(target!=pc&&targetReceipt==null)return;
  try{
   var parent=ingress.get();boolean owns=parent==null||parent.pc!=pc;var accepted=owns?reserve(pc):parent;if(accepted==null)return;
   ingress.remove();var packetContext=NativeCastTimeline.detach();
   try{try{NativePairedCastSafety.run(pc,target,()->{
    if(!accepted.binding.matches(pc)||!targetBinding.matches(target)||CombatAccess.field(target,"hub")!=accepted.hub
     ||(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)||(boolean)CombatAccess.field(target,NativeDeItemApplication.DISCONNECTED)
     ||CombatAccess.field(accepted.hub,NativeDeItemApplication.FIELD)!=accepted.application||accepted.application!=null&&accepted.application.closed)return;
    var batch=new Batch(pc,accepted.arrival,accepted);batch.captureSelf=true;batch.target=target;batch.targetBinding=targetBinding;batch.targetReceipt=targetReceipt;
    var previous=current.get();current.set(batch);
    try{body.run();}catch(IOException failure){throw new UncheckedIOException(failure);}
    finally{if(previous==null)current.remove();else current.set(previous);if(!batch.outputs.isEmpty()||!batch.completions.isEmpty()){NativePairedCastSafety.transfer(pc,target,targetReceipt);try{NativePlayerVitalCompletions.enqueueLocked(accepted.receipt,batch);}catch(RuntimeException|Error failure){NativePairedCastSafety.abandon(targetReceipt);throw failure;}}}
   });}catch(UncheckedIOException failure){throw failure.getCause();}}
   finally{NativeCastTimeline.restore(packetContext);if(parent==null)ingress.remove();else ingress.set(parent);if(owns)accepted.receipt.close();}
  }finally{if(targetReceipt!=null)targetReceipt.close();}
 }
'''+t[e:]
at=t.rfind('}');t=t[:at]+''' static boolean validTargets(ClientSession pc){
  var batch=current.get();if(batch==null||batch.pc!=pc||batch.target==null)return true;
  return batch.accepted.binding.matches(pc)&&batch.targetBinding.matches(batch.target)
   &&pc.nativePlayerActionLock().isHeldByCurrentThread()&&batch.target.nativePlayerActionLock().isHeldByCurrentThread()
   &&CombatAccess.field(batch.target,"hub")==batch.accepted.hub&&CombatAccess.field(batch.accepted.hub,NativeDeItemApplication.FIELD)==batch.accepted.application
   &&(batch.accepted.application==null||!batch.accepted.application.closed)
   &&!(boolean)CombatAccess.field(pc,NativeDeItemApplication.DISCONNECTED)&&!(boolean)CombatAccess.field(batch.target,NativeDeItemApplication.DISCONNECTED);
 }
'''+t[at:];(sources/'NativeLightAction.java').write_text(t,encoding='utf-8')
t=(original/'NativeCastMaterial.java').read_text('utf-8');t=once(t,'return cast!=null&&cast.transaction','return NativeLightAction.validTargets(pc)&&cast!=null&&cast.transaction');t=once(t,'  if(cast.resolved)return','  if(!NativeLightAction.validTargets(pc))return false;\n  if(cast.resolved)return');t=once(t,'   if(item==0||NativeCastMaterialPolicy.gm(pc)){','   boolean skip=item==0||NativeCastMaterialPolicy.gm(pc);if(!NativeLightAction.validTargets(pc))return false;\n   if(skip){');t=once(t,'      try{NativeDeItemApplication.replace(pc,next,0);}catch(RuntimeException|Error failure)','      try{if(!NativeLightAction.validTargets(pc))throw new IOException("material pair retired before install");NativeDeItemApplication.replace(pc,next,0);}catch(IOException|RuntimeException|Error failure)');(sources/'NativeCastMaterial.java').write_text(t,encoding='utf-8');shutil.copyfile(R/'NativePairedCastSafety.java',sources/'NativePairedCastSafety.java')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8');log=run/'build.log';v=dict(status='building',parent=pin(D/'build-current.json'),base=parent['runtime'],asm=seed['asm'],frozen_inputs=[pin(p) for p in inputs.rglob('*') if p.is_file()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  tools=run/'tools';tools.mkdir();tcp=';'.join(map(str,[tools,*[r['path'] for r in seed['asm']]]));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',tcp,'-d',tools,inputs/'PairedLifecycleMethods.java'])
  c0=run/'C0';c1=run/'C1';c0.mkdir();c1.mkdir();compiler=[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[jar,S/'lib/*']))]
  invoke([*compiler,'-d',c0,*sorted(original.glob('*.java'))]);invoke([*compiler,'-d',c1,*sorted(sources.glob('*.java'))])
  output=run/'classes';invoke([J/'java.exe','-Xverify:all','-cp',tcp,'PairedLifecycleMethods','splice',jar,output,c0,c1]);p=output/'local/lito381/server/NativePairedCastSafety.class';shutil.copyfile(c1/'local/lito381/server/NativePairedCastSafety.class',p)
  with zipfile.ZipFile(jar) as z:entries={n:z.read(n) for n in z.namelist()};before=dict(entries)
  for p in output.rglob('*.class'):entries[p.relative_to(output).as_posix()]=p.read_bytes()
  changed=sorted(n for n in before if before[n]!=entries[n]);fresh=sorted(set(entries)-set(before));prefix='local/lito381/server/';assert changed==sorted(prefix+n+'.class' for n in ['NativeLightAction','NativeLightAction$Batch','NativeCastMaterial']) and fresh==[prefix+'NativePairedCastSafety.class'],(changed,fresh)
  candidate=run/'candidate-integrated-pair-lifecycle-unqualified.jar'
  with zipfile.ZipFile(candidate,'w',zipfile.ZIP_DEFLATED) as z:
   for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
  invoke([J/'java.exe','-Xverify:all','-cp',tcp,'PairedLifecycleMethods','verify',jar,candidate,c0,c1])
 v.update(status='passed-integrated-pair-lifecycle-build-targeted-proof-pending',runtime=pin(candidate),changed=changed,fresh=fresh,production_PC_fields_added=0,new_workers_schedulers_ThreadLocals=0,complete_original_completion_cleanup_retained=True);print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-7500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'pair-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
