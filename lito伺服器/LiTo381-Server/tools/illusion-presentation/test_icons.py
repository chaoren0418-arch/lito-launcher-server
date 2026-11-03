from pathlib import Path
import subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
jar=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else B/'candidate.jar'
cp=str(jar)+';'+str(S/'lib/*')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),str(S/'tools/dragon-combat/Regression.java'),str(R/'IllusionRegression.java'),str(S/'tools/shared-database/SourceLoginRegression.java')],check=True)
log=S/'runtime/illusion-status-regression.log'
with log.open('w',encoding='utf-8') as stream:
 result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.IllusionRegression',str(S/'config/server.properties'),str(S/'runtime/illusion-status-regression')],cwd=S,stdout=stream,stderr=subprocess.STDOUT)
text=log.read_text('utf-8')
print('\n'.join(text.splitlines()[-5:]));result.check_returncode()

import json
rows=[json.loads(x) for x in max((S/'runtime/illusion-status-regression/capture').glob('*/frames.jsonl'),key=lambda p:p.stat().st_mtime).read_text('utf-8').splitlines()]
icons=[r for r in rows if r['note'].startswith('S_ILLUSION_STATUS')]
assert len(icons)==21, len(icons) # two casts + nine refreshed + nine cleared + WORLD Patience
for r in icons:
 b=bytes.fromhex(r['plain']);assert len(b)==4 and b[0]==0x6d and b[3] in [201,204,206,209,211,214,216,217,219]
assert len([r for r in icons if bytes.fromhex(r['plain'])[1:3]==bytes(2)])==9
print('PASS: 21 exact native status packets, no duplicate timer refresh, all nine removals')
