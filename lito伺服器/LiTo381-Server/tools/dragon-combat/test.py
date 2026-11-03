from pathlib import Path
import subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
jar=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else B/'candidate.jar'
cp=str(jar)+';'+str(S/'lib/*')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),str(R/'Regression.java'),str(S/'tools/shared-database/SourceLoginRegression.java')],check=True)
log=S/'runtime/dragon-combat-regression.log'
with log.open('w',encoding='utf-8') as stream:
 result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.Regression',str(S/'config/server.properties'),str(S/'runtime/dragon-combat-regression')],cwd=S,stdout=stream,stderr=subprocess.STDOUT)
text=log.read_text('utf-8')
print('\n'.join(text.splitlines()[-5:]));result.check_returncode()

import json
frames=sorted((S/'runtime/dragon-combat-regression/capture').glob('*/frames.jsonl'),key=lambda p:p.stat().st_mtime)[-1]
rows=[json.loads(line) for line in frames.read_text('utf-8').splitlines()]
assert any(r['plain']=='6D B0 04 B5' for r in rows)
assert any(r['plain']=='6D 00 00 B5' for r in rows)
assert any('chainsword-regression' in r.get('note','') and r['plain'].endswith('18') for r in rows)
assert any('S_EFFECT Foe Slayer target=1610612736 gfx=6509' in r.get('note','') for r in rows)
print('PASS: actual encrypted transport capture: skin 1200/0, chainsword wield24, own-summon effect6509')

attacks=[r for r in rows if r['opcode']=='0x23' and 'foe-slayer-strike=true' in r.get('note','')]
assert len(attacks)==3
assert all('target=1610612736' in r['note'] for r in attacks)
assert any('foe-slayer-final-strike=true' in r['note'] for r in attacks)
print('PASS: three actual S_ATTACK packets to own summon, sequence closes on third hit')
