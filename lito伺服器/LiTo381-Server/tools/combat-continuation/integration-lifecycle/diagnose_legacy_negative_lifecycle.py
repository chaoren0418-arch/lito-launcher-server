"""Confirm the fixture's paired-publication contract on the unchanged parent and candidate."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,os,shutil
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];D=R/'build';B=S/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/'companion-negative-scene-build-current.json');build=read(Path(index['manifest']['path']));run=D/'negative-paired-publication-diagnostic'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'LegacyPairLifecycleAdapter.py',C/'negative-material/NegativeLifecycleProbe.java',C/'negative-material/NegativeMaterialProbe.java']:shutil.copyfile(p,inputs/p.name)
namespace={};exec(compile((inputs/'LegacyPairLifecycleAdapter.py').read_text('utf-8'),'LegacyPairLifecycleAdapter.py','exec'),namespace);p=inputs/'NegativeLifecycleProbe.java';p.write_text(namespace['adapt'](p.read_text('utf-8')).replace('new boolean[]{false,true}','new boolean[]{false}').replace('for(int skill:SKILLS)for(int mode=0;mode<7;mode++)','for(int skill:new int[]{29})for(int mode=3;mode<4;mode++)'),'utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');v=dict(status='running',inputs=[pin(p) for p in inputs.iterdir()],results=[],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False)
try:
 for name,runtime in [('parent',build['base']),('candidate',index['runtime'])]:
  assert sha(Path(runtime['path']))==runtime['sha256'];branch=run/name;branch.mkdir();cp=';'.join(map(str,[branch/'classes',C/'elemental-material/build/lifecycle/classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',runtime['path'],S/'lib/*']));log=branch/'test.log'
  with log.open('w',encoding='utf-8') as out:
   for args in [[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',branch/'classes',*inputs.glob('*.java')],[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.NegativeLifecycleProbe',S/'config/server.properties',branch/'runtime',branch/'observations.json',runtime['path'],'after']]:subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
  result=read(branch/'observations.json');assert result['cases']==1 and result['locked_write_calls']==0;v['results'].append(dict(runtime=runtime,observations=result,output=pin(branch/'observations.json'),log=pin(log)));print('PASS '+name+' original accepted cost, exact zero stale pair output and independent native MP completion',flush=True)
 v['status']='passed-parent-and-candidate-existing-paired-publication-and-independent-MP-contract'
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:write(run/'manifest.json',v);write(D/'negative-paired-publication-diagnostic-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json')))
