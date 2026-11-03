from pathlib import Path
import subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
jar=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else B/'candidate.jar'
cp=str(jar)+';'+str(S/'lib/*')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),str(S/'tools/dragon-combat/Regression.java'),str(S/'tools/empty-melee/EmptyMeleeRegression.java'),str(S/'tools/shared-database/SourceLoginRegression.java')],check=True)
log=S/'runtime/empty-melee-regression.log'
with log.open('w',encoding='utf-8') as stream:
 result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.EmptyMeleeRegression',str(S/'config/server.properties'),str(S/'runtime/empty-melee-regression')],cwd=S,stdout=stream,stderr=subprocess.STDOUT)
text=log.read_text('utf-8')
print('\n'.join(text.splitlines()[-5:]));result.check_returncode()

import json
frames=max((S/'runtime/empty-melee-regression/capture').glob('*/frames.jsonl'),key=lambda p:p.stat().st_mtime)
rows=[json.loads(line) for line in frames.read_text('utf8').splitlines()]
swings=[r for r in rows if r['note'].startswith('S_ATTACK empty-melee')]
assert len(swings)==9,len(swings)
for r in swings:
 b=bytes.fromhex(r['plain']);assert b[0]==0x23 and len(b)==16 and b[1]==1 and b[6:11]==bytes(5) and b[12:]==bytes(4),r
turns=[r for r in rows if r['note'].startswith('S_CHANGE_DIRECTION empty-melee')]
assert len(turns)==9
assert [bytes.fromhex(r['plain'])[11] for r in swings]==[bytes.fromhex(r['plain'])[5] for r in turns]
print('PASS: nine actual encrypted zero-target/zero-damage swings, exact native 16-byte S_ATTACK and matching direction packets')
