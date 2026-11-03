from pathlib import Path
import subprocess,sys,json
S=Path(__file__).resolve().parents[2]
jar=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path(__file__).resolve().parent/'build/candidate.jar'
subprocess.run([sys.executable,'-B','-X','utf8',str(S/'tools/dragon-combat/test.py'),str(jar)],check=True)
p=max((S/'runtime/dragon-combat-regression/capture').glob('*/frames.jsonl'),key=lambda p:p.stat().st_mtime)
rows=[json.loads(line) for line in p.read_text('utf8').splitlines()]
attacks=[i for i,r in enumerate(rows) if 'foe-slayer-strike=true' in r['note']]
assert len(attacks)==3
assert not any(r['note'].startswith('S_ACTION Foe Slayer') for r in rows)
assert any(i>max(attacks) and 'S_EFFECT Foe Slayer caster=' in r['note'] and 'gfx=7020' in r['note'] for i,r in enumerate(rows))
print('PASS: three physical attacks, no extra casting action, acceleration7020 after all strikes')
