from pathlib import Path
import json,hashlib,subprocess,os
R=Path(__file__).resolve().parent;ROOT=R.parent;S=ROOT.parents[1];B=S/'tools/resource-database/build';D=R/'build/curse-clock-boundary';D.mkdir(exist_ok=True);classes=D/'classes';classes.mkdir(exist_ok=True)
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
assert t.count(a)==1;t=t.replace(a,b);source=D/'CurseClockBoundaryProbe.java';source.write_text(t,encoding='utf8')
build=read(R/'build/build.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];F=B/'baseline-PC-ingress-efd4-v2'
deps=[classes,ROOT/'poison-publication/build/curse-consumer/classes',ROOT/'poison-publication/build/timers/classes',ROOT/'poison-publication/build/boundaries/classes',ROOT/'elemental-material/build/lifecycle/classes',ROOT/'elemental-material/build/boundaries/classes',ROOT/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
J=Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin');cp=';'.join(map(str,deps));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=build['runtime'],source=pin(p),probe=pin(source),driver=pin(Path(__file__)),original_frozen_suite_modified=False,formal_deployment_changed=False)
try:
 with (D/'probe.log').open('w',encoding='utf8')as out:
  subprocess.run([str(J/'javac.exe'),'-g:none','-implicit:none','-sourcepath','','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(source)],check=True,env=env,stdout=out,stderr=subprocess.STDOUT,timeout=90)
  subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'local.lito381.server.CurseClockBoundaryProbe',str(S/'config/server.properties'),str(D/'runtime'),str(D/'observations.json'),str(jar),'after'],cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=300)
 v['observations']=pin(D/'observations.json');v['counts']=read(D/'observations.json');assert v['counts']['cases']==24 and v['counts']['locked_writes']==0
 v['status']='passed-measured-paralysis-publication-clock-boundary'
except BaseException as e:v.update(status='failed',error=str(e));print((D/'probe.log').read_text('utf8',errors='replace')[-3500:]);raise
finally:v['log']=pin(D/'probe.log');(D/'receipt.json').write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(dict(status=v['status'],counts=v['counts']),ensure_ascii=False))
