"""Keep the 887 original Proc assertions; adapt only the WORLD identity writer's actual lock observation."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'companion-proc-scene-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];source=B/'NativeProcHealingCompanionRegression.java';assert sha(source)=='d0e29157c5e0e2ee38d86d82ea2ad6d20ad08b07cfbe6a1d37023588801e3dc5';old=read(B/'proc-healing-companion-development/regressions-current.json');assert sha(Path(old['manifest']['path']))==old['manifest']['sha256'];basis=read(Path(old['manifest']['path']));vectors=Path(basis['source_vectors']['path']);assert sha(vectors)==basis['source_vectors']['sha256']
run=D/'companion-proc-legacy'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);shutil.copyfile(__file__,inputs/Path(__file__).name);probe=inputs/source.name;text=source.read_text('utf-8')
oldlock='Object lock=writerKind==2?get(master,"tamedCompanions"):CombatPoisonAtomic.status(writerKind==0?pc:master);'
newlock='Object lock=writerKind==2?get(master,"tamedCompanions"):a.world()?(writerKind==0?pc:master).nativePlayerActionLock():CombatPoisonAtomic.status(writerKind==0?pc:master);'
oldwait='writer.start();waitBlocked(writer,lock);'
newwait='writer.start();if(a.world()&&writerKind<2)waitPairedWriter(writer,(java.util.concurrent.locks.ReentrantLock)lock,worker);else waitBlocked(writer,lock);'
for a,z in [(oldlock,newlock),(oldwait,newwait)]:assert text.count(a)==1;text=text.replace(a,z)
marker='    static void innerLocks('
method='''    static void waitPairedWriter(Thread writer,java.util.concurrent.locks.ReentrantLock actual,Thread worker)throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);var bean=java.lang.management.ManagementFactory.getThreadMXBean();
        while(System.nanoTime()<until){var info=bean.getThreadInfo(writer.getId());if(actual.hasQueuedThread(writer)&&info!=null&&info.getLockOwnerId()==worker.getId()&&(info.getThreadState()==Thread.State.WAITING||info.getThreadState()==Thread.State.TIMED_WAITING))return;Thread.yield();}
        check(false,"WORLD identity writer genuinely waits on the same native consumer's actual Source action lock");
    }
'''
assert text.count(marker)==1;text=text.replace(marker,method+marker);probe.write_text(text,'utf-8');J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';N=B/'pc-native-accept-development';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,B/'pc-native-signed-hp-development/signed-probe-fixtures',N/'fixtures',F,F/'fixture-support.zip',b['runtime']['path'],S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'compile.log';v=dict(status='running',runtime=b['runtime'],basis=b['manifest'],original=pin(source),adapted=pin(probe),inputs=[pin(p) for p in inputs.iterdir()],source_reference=basis['source_reference'],source_vectors=basis['source_vectors'],fixture_dependencies=basis['fixture_dependencies'],support=basis['support'],adapter='Only WORLD identity-writer lock observation changes from the old status monitor to the exact Source action lock owned by the real blocked consumer. Custody and all BATTLE monitor observations, all HP/packet/cost/membership/queue/death/Source vector assertions remain.',results=[],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for p in v['fixture_dependencies']+[v['support']]:assert sha(Path(p['path']))==p['sha256']
 def invoke(args,out):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=600)
 with log.open('w',encoding='utf-8') as out:invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,probe],out)
 for mode in ['normal','drain-battle','drop-battle','drain-world','drop-world']:
  log=run/(mode+'.log');obs=run/(mode+'-observations.json');print('RUN '+mode,flush=True)
  with log.open('w',encoding='utf-8') as out:invoke([J/'java.exe','-Xverify:all','-Dlineage.damage.commitWorkers=2','-Dlineage.damage.maxTargetPending=512','-Dlineage.damage.maxGlobalPending=1024','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(S.parent.parent/'lito登入器/LiTo381-Launcher/app/common_catalog.bin'),'-cp',cp,'local.lito381.server.NativeProcHealingCompanionRegression',S/'config/server.properties',run/'runtime'/mode,obs,mode,vectors],out)
  result=read(obs);v['results'].append(dict(mode=mode,observations=pin(obs),log=pin(log),results=result));write(run/'manifest.json',v);print('PASS '+mode+' cases='+str(result['cases']),flush=True)
 count=sum(x['results']['cases'] for x in v['results']);assert count==887;v.update(status='passed-same-SHA-887-original-Proc-companion-actual-AI-and-queue-regressions',cases=count,checks=sum(x['results']['checks'] for x in v['results']))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2600:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-proc-legacy-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
