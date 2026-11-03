"""Replay frozen doll compiler/ASM inputs; no game, Hub or SQL initialization."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,sys
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def check(ref):assert sha(Path(ref['path']))==ref['sha256'],ref['path']
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
a=read(D/'source-poison-npc-build-current.json');check(a['manifest']);check(a['runtime']);m=read(Path(a['manifest']['path']));check(m['base'])
for ref in m['inputs']+m['asm']+m['Source_inputs']:check(ref)
for ref in m['Source_frozen_references']:check(ref['original']);check(ref['frozen'])
run=D/'source-poison-npc-reproduction'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True)
builder=Path(a['manifest']['path']).parent/'inputs/build_source_poison_npc.py'
with (run/'replay.log').open('w',encoding='utf-8')as log:subprocess.run([sys.executable,'-B','-X','utf8',str(builder),a['manifest']['path'],str(S),str(run/'replay')],stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
rebuilt=read(run/'replay/manifest.json');check(rebuilt['runtime']);assert rebuilt['runtime']['sha256']==a['runtime']['sha256']
v=dict(status='passed-frozen-byte-identical-poison-npc-original-method-inverse-JVM-no-initialization',runtime=a['runtime'],basis=a['manifest'],replay=pin(run/'replay/manifest.json'),inputs=[pin(Path(__file__))],log=pin(run/'replay.log'),new_game_runtime_cases=0,new_SQL_connections=0,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
write(run/'manifest.json',v);write(D/'source-poison-npc-reproduction-current.json',dict(status=v['status'],runtime=a['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],runtime=a['runtime'])))
