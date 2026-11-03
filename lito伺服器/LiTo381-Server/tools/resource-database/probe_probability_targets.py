from pathlib import Path
import subprocess,hashlib
R=Path(__file__).resolve().parent;B=R/'build';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
analysis=R/'inputs/asm-analysis-9.7.jar'
assert hashlib.sha256(analysis.read_bytes()).hexdigest()=='7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
asm=Path.home()/'.m2/repository/org/ow2/asm'
cp=';'.join([str(analysis),*[str(asm/f'{x}/9.7/{x}-9.7.jar') for x in ['asm','asm-tree']]])
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(B),str(R/'ProbeProbabilityTargets.java')],check=True)
subprocess.run([str(J/'java.exe'),'-Xmx1024m','-cp',str(B)+';'+cp,'ProbeProbabilityTargets',str(R/'inputs/base-0.6.61.jar'),str(B/'probability-target-provenance.tsv')],check=True,timeout=60)
