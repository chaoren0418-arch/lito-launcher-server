import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from regression_support import formal_protection, Options, adapt_driver, write, java_tool
options=Options()
from pathlib import Path
import json,hashlib,subprocess,os
CODE=Path(__file__).resolve().parent;R=options.root/'cancellation-battle-runtime';ROOT=R.parent;S=ROOT.parents[1];B=S/'tools/resource-database/build';D=options.destination('curse-clock-boundary',CODE/'build/curse-clock-boundary');D.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));read=lambda p:json.loads(p.read_text('utf8'))
for old in [D/'probe.log',D/'receipt.json',D/'CurseClockBoundaryProbe.java']:
 if old.exists():
  frozen=D/('previous-'+sha(old)[:12]+'-'+old.name)
  if not frozen.exists():frozen.write_bytes(old.read_bytes())
p=ROOT/'poison-publication/PoisonScopeCurseConsumerProbe.java';assert sha(p)=='a1ae0b3a686515bb36d048019cabda70ac9e741cc9160ca607b4a312d0b9b787'
t=p.read_text('utf8').replace('PoisonScopeCurseConsumerProbe','CurseClockBoundaryProbe')
a='peer.serverExpireTimedStatuses(starts);need(status.paralysisActive(starts),"actual clock locks at exact deadline");'
b='var targetSocket=(Socket)get(peer,"socket");var targetDelayed=new AtomicLong();var targetFired=new AtomicBoolean();\n   set(peer,"socket",new Socket(){public OutputStream getOutputStream()throws IOException{return new FilterOutputStream(targetSocket.getOutputStream()){\n    public void write(byte[]b,int o,int n)throws IOException{if(targetFired.compareAndSet(false,true)){long begin=System.nanoTime();try{Thread.sleep(250);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}targetDelayed.set(System.nanoTime()-begin);}out.write(b,o,n);}\n    public void write(int b)throws IOException{write(new byte[]{(byte)b},0,1);}};}public void close(){}});\n   long tickStart=System.nanoTime(),tickEnd;try{peer.serverExpireTimedStatuses(starts);}finally{tickEnd=System.nanoTime();set(peer,"socket",targetSocket);}\n   need(status.paralysisActive(starts),"actual clock locks at exact deadline");\n   need(targetFired.get()&&targetDelayed.get()>=TimeUnit.MILLISECONDS.toNanos(250),"actual target lock publication delayed beyond legacy150ms heuristic");'
assert t.count(a)==1;t=t.replace(a,b)
a='try{cast(pc,33,row);}finally{set(pc,"socket",original);}'
assert t.count(a)==1;t=t.replace(a,'long castWallBegin=System.nanoTime();'+a)
a='need(expires>=starts+activeNs&&expires-starts-activeNs<TimeUnit.MILLISECONDS.toNanos(150),"configured active duration after lock packet");'
b='long delta=expires-starts-activeNs;System.out.println("CLOCK_BOUNDARY world="+world+" mode="+mode+" delayedNs="+targetDelayed.get()+" deltaNs="+delta+" measuredOperationNs="+(tickEnd-castWallBegin));need(delta>=targetDelayed.get()&&delta<=tickEnd-castWallBegin,"full configured duration after deliberately delayed lock packet; extension bounded by measured cast-and-clock interval");'
assert t.count(a)==1;t=t.replace(a,b)
# #420／G16：50635f7e／Maven 的 NativeSelectedPlayerIntent 會靜默丟棄 BATTLE 中未帶 Ctrl 意圖、指向同房間敵方玩家的施法（#341 F4 的根因）。
# 產生檔以同名 static cast 遮蔽父類別 cast（封包版面與 poison-publication/PoisonScopeMaterialProbe.java 的 cast 相同），
# 只在受測 runtime 含該閘、BATTLE、目標為同房間其他玩家時附加 LiTo 客戶端 Ctrl 擴充；歷史 runtime 與其他情況維持原始封包。不改任何 need()。
a='public class CurseClockBoundaryProbe extends PoisonScopeMaterialProbe {'
b=a+'\n static final boolean G16_INTENT=g16Present();\n static boolean g16Present(){try{Class.forName("local.lito381.server.NativeSelectedPlayerIntent",false,ClientSession.class.getClassLoader());return true;}catch(ClassNotFoundException e){return false;}}\n static byte[] g16Intent(ClientSession pc,long target,byte[] raw)throws Exception{\n  if(!G16_INTENT||target==pc.playerObjectId()||!String.valueOf(get(pc,"phase")).equals("BATTLE"))return raw;\n  boolean player=false;for(Object member:(Iterable<?>)call(get(pc,"hub"),"roomCombatants",new Class[]{long.class},((Number)get(pc,"currentRoomId")).longValue()))if(((ClientSession)member).playerObjectId()==target)player=true;\n  if(!player)return raw;\n  byte[] out=Arrays.copyOf(raw,raw.length+8);System.arraycopy(new byte[]{0x4c,0x46,0x43,0x31,raw[0],1,0x67,(byte)0xa5},0,out,raw.length,8);return out;\n }\n static void cast(ClientSession pc,int skill,Object row)throws Exception {\n  byte[] packet=new Protocol.Writer().c(Protocol.C_USE_SPELL).c((skill-1)>>3).c((skill-1)&7).d(targetId).h((int)get(pc,"positionX")+1).h((int)get(pc,"positionY")).bytes();\n  call(pc,"handle",new Class[]{byte[].class},g16Intent(pc,targetId,packet));\n }'
assert t.count(a)==1;t=t.replace(a,b);source=D/'CurseClockBoundaryProbe.java';source.write_text(t,encoding='utf8')
build=options.build('cancellation-battle-runtime');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];F=B/'baseline-PC-ingress-efd4-v2'
deps=[classes,ROOT/'poison-publication/build/curse-consumer/classes',ROOT/'poison-publication/build/timers/classes',ROOT/'poison-publication/build/boundaries/classes',ROOT/'elemental-material/build/lifecycle/classes',ROOT/'elemental-material/build/boundaries/classes',ROOT/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
cp=';'.join(map(str,deps));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=build['runtime'],source=pin(p),probe=pin(source),driver=pin(Path(__file__)),original_frozen_suite_modified=False,selected_player_intent='explicit LiTo Ctrl extension only when the runtime carries NativeSelectedPlayerIntent and the BATTLE target is another room player (#420)',formal_deployment_changed=False)
try:
 with formal_protection(options,v):
  with (D/'probe.log').open('w',encoding='utf8')as out:
   subprocess.run([str(java_tool('javac')),'-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(source)],check=True,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=90)
   subprocess.run([str(java_tool('java')),'-Xverify:all','-cp',cp,'local.lito381.server.CurseClockBoundaryProbe',str(options.config),str(D/'runtime'),str(D/'observations.json'),str(jar),'after'],cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=300)
  v['observations']=pin(D/'observations.json');v['counts']=read(D/'observations.json');assert v['counts']['cases']==24 and v['counts']['locked_writes']==0
  v['status']='passed-measured-paralysis-publication-clock-boundary'
except BaseException as e:
 v.update(status='failed',error=str(e))
 if (D/'probe.log').exists():print((D/'probe.log').read_text('utf8',errors='replace')[-3500:])
 raise
finally:
 if (D/'probe.log').exists():v['log']=pin(D/'probe.log')
 (D/'receipt.json').write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(dict(status=v['status'],counts=v['counts']),ensure_ascii=False))
