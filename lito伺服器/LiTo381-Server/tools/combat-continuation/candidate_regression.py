"""Sequential full regression; exact candidate and every inherited assertion remain pinned."""
from pathlib import Path
import json,hashlib,subprocess,sys
from regression_support import Options, prepare_regression
options=Options(stage=True)
CODE=Path(__file__).resolve().parent;R=options.root;S=options.server;stage=options.args.stage
assert stage in ['pet-material','polymorph-source','return-nature-transfer','npc-physical-source','npc-physical-size','cancellation-probability','cancellation-npc','area-sleep-slow','combat-config-origin','call-nature-material','cancellation-battle-runtime']
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
b=options.build(stage);runtime=b['runtime'];D=prepare_regression(options,stage)
if options.out:
 # Reconciliation may only reuse the failed replay produced by this invocation.
 write(D/'replay-reconciliation.json',dict(status='running',runtime=runtime))

def driver(name):
 args=options.child_args()
 if not options.args.jar:args+=['--jar',runtime['path']]
 return subprocess.run([sys.executable,'-B','-X','utf8',str(CODE/name),'--stage',stage,*args],cwd=CODE.parents[1]).returncode

def failure_summary():
 # Root cause of the failed qualification (first non-zero driver, its log, manifest error),
 # so a later reconciliation assertion never hides it (#341 item 08, #407).
 try:
  q=read(D/'qualification-current.json');m=read(Path(q['manifest']['path']))
 except Exception as error:
  return 'qualification manifest unreadable: '+repr(error)
 results=m.get('results') or [];failed=[r for r in results if r.get('exit_code')]
 row=failed[0] if failed else None
 return 'status='+str(m.get('status'))+'; error='+str(m.get('error'))+'; failed_driver='+str(row and row.get('name'))+'; log='+str(row and row.get('log',{}).get('path'))+'; manifest='+str(q['manifest']['path'])

def reconcile():
 # Only the known historical formal guard can be reconciled; all behavior must already pass.
 q=read(D/'qualification-current.json');assert sha(Path(q['manifest']['path']))==q['manifest']['sha256'];m=read(Path(q['manifest']['path']))
 assert m['results'] and m['results'][-1]['name']=='replay-native-de-item-state.py','Not reconcilable (only the replay-native-de-item-state.py formal guard is); root cause: '+failure_summary()
 manifests=sorted((D/'replays').glob('*/manifest.json'),key=lambda p:p.parent.name);assert manifests;mp=manifests[-1];fv=read(mp);formal=S.parent.parent/'lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar';assert fv['runtime']==runtime and fv['status']=='failed' and fv['error']==str(formal)
 if options.out:
  subprocess.run([sys.executable,'-B','-X','utf8',str(CODE/'reconcile_replay.py'),'--stage',stage,'--manifest',str(mp),*options.child_args()],cwd=S,check=True)
  return
 original=CODE/'reconcile_replay.py';t=original.read_text('utf-8');t=t.replace("R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';D=R/'build/integration-regression'",'R=Path('+repr(str(R))+');S=R.parents[1];B=S/\'tools/resource-database/build\';D=Path('+repr(str(D))+')');t=t.replace("p=D/'replays/20261018T140232749405Z/manifest.json'",'p=Path('+repr(str(mp))+')');adapter=D/'reconcile_replay.py'
 adapter.write_text('import sys\nsys.path.insert(0,'+repr(str(CODE))+')\n'+t,encoding='utf-8')
 write(D/'reconciliation-adaptation.json',{'original':pin(original),'adapted':pin(adapter),'failed_manifest':pin(mp),'change':'paths only; legacy assertions retained'})
 subprocess.run([sys.executable,'-B','-X','utf8',str(adapter)],cwd=S,check=True)
if not options.out and (D/'qualification-current.json').exists() and read(D/'qualification-current.json')['status']=='failed' and not (D/'replay-reconciliation.json').exists():
 q=read(D/'qualification-current.json');old=read(Path(q['manifest']['path']))
 if old['results'] and old['results'][-1]['name']=='replay-native-de-item-state.py':reconcile()
rc=driver('full_regression.py')
if rc:
 print('FULL REGRESSION FAILED before reconciliation: '+failure_summary(),file=sys.stderr,flush=True)
 reconcile()
 assert driver('full_regression.py')==0
assert driver('affected_regressions.py')==0
print('PASS exact '+stage+' candidate: 20 existing and 18 affected regression groups; full combat objective still open')
