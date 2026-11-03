from pathlib import Path
import subprocess,hashlib
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
asm=Path.home()/'.m2/repository/org/ow2/asm'
cp=';'.join(str(asm/f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree'])
def run(*a):subprocess.run(list(map(str,a)),check=True)
run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',B/'base.jar','-d',B,S/'src/main/java/local/lito381/server/EmptyMelee.java')
run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',B,R/'Patch.java')
run(J/'java.exe','-cp',str(B)+';'+cp+';'+str(B/'base.jar'),'Patch',B/'base.jar',B,B/'candidate.jar')
print(hashlib.sha256((B/'candidate.jar').read_bytes()).hexdigest())
