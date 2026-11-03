"""Pin remaining common actor Ref consumers without claiming they are behavior-qualified."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil
R=Path(__file__).resolve().parent;D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'companion-ref-scene-build-current.json');assert sha(Path(b['runtime']['path']))==b['runtime']['sha256'];seed=read(Path(read(D/'summon-retirement-build-current.json')['manifest']['path']))
run=D/'companion-ref-scene-consumer-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'CompanionRefConsumerCensus.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join([str(run/'tools'),*[x['path'] for x in seed['asm']]])
for ref in seed['asm']:assert sha(Path(ref['path']))==ref['sha256']
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(run/'tools'),str(inputs/'CompanionRefConsumerCensus.java')],check=True,timeout=60)
table=run/'consumers.tsv';subprocess.run([str(J/'java.exe'),'-Xms16m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-cp',cp,'CompanionRefConsumerCensus',b['runtime']['path'],str(table)],check=True,timeout=60)
rows=[dict(zip(['class','method','callee'],line.split('\t'))) for line in table.read_text('utf-8').splitlines()[1:]]
v=dict(status='located-readonly-post-Ref-scene-consumers-specialized-behavior-open',runtime=b['runtime'],basis=b['manifest'],inputs=[pin(p) for p in inputs.iterdir()],asm=seed['asm'],table=pin(table),consumer_ports=len(rows),consumer_calls=sum(not r['callee'].startswith('FIELD ') for r in rows),consumer_field_accesses=sum(r['callee'].startswith('FIELD ') for r in rows),consumer_classes=sorted({r['class'] for r in rows}),remaining=['Common WORLD Ref capture/live and one real accepted Joy feedback path have scoped tests only. Specialized ProcHealingCompanion.withTarget/find/registered/sameScene and NativeBattleTamedLifecycle captures, AI/hate/poison, debit/damage publication races remain open.','WORLD actor role continuity does not qualify every Ref consumer or convert BATTLE companion policy.','This is a bytecode census, not native behavior cases or full Source/control coverage.'],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'companion-ref-scene-consumers-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')));print(json.dumps(dict(status=v['status'],calls=len(rows),classes=len(v['consumer_classes']))))
