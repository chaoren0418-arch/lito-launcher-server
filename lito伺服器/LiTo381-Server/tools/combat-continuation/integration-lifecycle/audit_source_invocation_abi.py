"""Locate internal interface ABI call-site defects without initializing candidate/Source/JDBC."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,sys,shutil,os,csv
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
def read(p):return json.loads(Path(p).read_text('utf-8'))
def pin(p):
 p=Path(p).resolve();return dict(path=str(p),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
def checked(ref):assert pin(ref['path'])==ref;return Path(ref['path'])
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
index=read(D/((sys.argv[1]+'-current.json')if len(sys.argv)>1 else 'source-program-update-progress-current.json'));base=checked(index['runtime']);basis=checked(index['manifest']);m=read(basis);build_ref=index['manifest']if 'asm'in m else m['build'];build=read(checked(build_ref))
run=D/'source-invocation-ABI-census'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in [Path(__file__).name,'SourceInvocationAbiCensus.java']:shutil.copyfile(R/name,inputs/name)
asm=[checked(x)for x in build['asm']];cp=';'.join(map(str,[run/'tools',*asm]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8')
args=['-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',str(run/'tools'),str(inputs/'SourceInvocationAbiCensus.java')];argfile=run/'javac.args';argfile.write_text('\n'.join(json.dumps(x,ensure_ascii=False)for x in args),'utf-8')
with (run/'census.log').open('w',encoding='utf-8')as log:
 subprocess.run([str(J/'javac.exe'),'@'+str(argfile)],env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=30)
 subprocess.run([str(J/'java.exe'),'-Xverify:all','-cp',cp,'SourceInvocationAbiCensus',str(base),str(run/'mismatched-calls.tsv')],env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=60)
with (run/'mismatched-calls.tsv').open('r',encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
v=dict(status='inventoried-internal-interface-invocation-ABI-no-semantic-qualification',runtime=index['runtime'],basis=index['manifest'],candidate_build=build_ref,inputs=[pin(p)for p in inputs.iterdir()],asm=build['asm'],mismatched_call_sites=len(rows),affected_methods=len({(r['owner'],r['method'],r['descriptor'])for r in rows}),affected_owners=len({r['owner']for r in rows}),artifacts=[pin(run/'mismatched-calls.tsv'),pin(run/'census.log')],new_SQL_connections=0,real_backend_started=False,full_goal_achieved=False,formal_deployment_changed=False)
write(run/'manifest.json',v);write(D/'source-invocation-ABI-census-current.json',dict(status=v['status'],runtime=index['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],mismatched_call_sites=v['mismatched_call_sites'],affected_methods=v['affected_methods'],affected_owners=v['affected_owners'],manifest=pin(run/'manifest.json')),ensure_ascii=False))
