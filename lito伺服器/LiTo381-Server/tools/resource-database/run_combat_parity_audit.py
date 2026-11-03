from pathlib import Path
import subprocess,hashlib,json,argparse
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
parser=argparse.ArgumentParser(description='Read-only formal/candidate skill field audit')
parser.add_argument('--jar',type=Path)
parser.add_argument('--output',type=Path)
args=parser.parse_args()
formal=L/'app/server/LiTo381-SharedResources-candidate.jar';jar=args.jar.resolve() if args.jar else formal
if jar.resolve()==formal.resolve():
 expected=json.loads((S/'deployment/release-inputs.json').read_text(encoding='utf-8-sig'))['server_sha256']
 assert hashlib.sha256(jar.read_bytes()).hexdigest()==expected,'Formal release differs from pin'
else:
 expected=json.loads((R/'build/build-manifest.json').read_text(encoding='utf-8'))['candidate_sha256']
 assert hashlib.sha256(jar.read_bytes()).hexdigest()==expected,'Candidate differs from build manifest'
cp=str(jar)+';'+str(S/'lib/*');classes=R/'build/tests';classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(R/'CombatParityAudit.java')],check=True)
p=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-cp',str(classes)+';'+cp,'local.lito381.server.CombatParityAudit',str(S/'config/server.properties')],capture_output=True,encoding='utf-8')
output=args.output or (R/'build/combat-parity-candidate.txt' if args.jar else S/'docs/maintenance/combat-parity-0.6.68-evidence.txt')
output.write_text(p.stdout+p.stderr,encoding='utf-8')
print('Audited SHA256:',expected)
print('\n'.join(line for line in p.stdout.splitlines() if line.startswith(('FIELD_DIFFERENCE','FIELD_COMPARISON'))))
if p.returncode:print(p.stderr)
raise SystemExit(p.returncode)
