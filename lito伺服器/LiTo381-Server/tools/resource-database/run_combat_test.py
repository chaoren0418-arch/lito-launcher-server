from pathlib import Path
import subprocess, sys

R = Path(__file__).resolve().parent
S = R.parents[1]
L = S.parent.parent / 'lito登入器/LiTo381-Launcher'
B = R / 'build'
J = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else B / 'candidate.jar'
cp = str(jar) + ';' + str(S / 'lib/*')
names=['CombatRegression','SourceSkillControlsRegression']
subprocess.run([str(J/'javac.exe'), '-encoding', 'UTF-8', '-implicit:none', '-cp', cp,
                '-d', str(B/'tests'), *[str(R/(name+'.java')) for name in names]], check=True)
for name in names:
    with (B/(name+'.log')).open('w', encoding='utf-8') as log:
        p = subprocess.run([str(J/'java.exe'), '-Dfile.encoding=UTF-8',
            '-Dlito.resourceProfile=tw13081901-v1', '-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),
            '-Xverify:all', '-cp', str(B/'tests')+';'+cp, 'local.lito381.server.'+name,
            str(S/'config/server.properties')], cwd=S, stdout=log, stderr=subprocess.STDOUT, timeout=60)
    print((B/(name+'.log')).read_text('utf-8')[-5000:])
    if p.returncode: raise SystemExit(p.returncode)
