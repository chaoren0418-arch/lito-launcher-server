from pathlib import Path
import json,subprocess,os,hashlib
R=Path(__file__).resolve().parent;D=R/'build/invisibility-audit';D.mkdir(exist_ok=True);m=json.loads((R/'build/integration-regression/preparation.json').read_text('utf-8'));jar=R/'solid-control/build/candidate-solid-control.jar';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=';'.join(map(str,[D,*[r['path'] for r in m['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx128m -XX:+UseSerialGC -XX:ActiveProcessorCount=2')
assert hashlib.sha256(jar.read_bytes()).hexdigest()=='c02e9d1e716b08bb27c21184d11a72c2fc8e66be7581165ec99c08b82b20d99c'
with (D/'build.log').open('w',encoding='utf-8') as log:
 subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(R/'InspectInvisibility.java')],env=env,stdout=log,stderr=subprocess.STDOUT,check=True,timeout=60)
 with (D/'calls.tsv').open('w',encoding='utf-8') as out:subprocess.run([str(J/'java.exe'),'-cp',cp,'InspectInvisibility',str(jar)],env=env,stdout=out,stderr=log,check=True,timeout=60)
print((D/'calls.tsv').read_text('utf-8'))
