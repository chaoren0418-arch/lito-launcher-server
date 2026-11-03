from pathlib import Path
import subprocess,zipfile,hashlib
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher';B=R/'build';B.mkdir(exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');asm=Path.home()/'.m2/repository/org/ow2/asm'
cp=';'.join(str(asm/f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree'])
assert hashlib.sha256((B/'base.jar').read_bytes()).hexdigest()=='e5f2e01b6294279dc350c96d38b27bd4dc53e3a1b92535b9be7abeb7283a9fdf', 'Retained historical input mismatch'

def run(*a):subprocess.run(list(map(str,a)),check=True)
run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',B/'base.jar','-d',B,*[S/'src/main/java/local/lito381/server'/f'{x}.java' for x in ['NativeActorTail','NativeRoomDirectoryRow','NativeMovement','NativeBattleVisibility']])
run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',B,R/'Patch.java')
run(J/'java.exe','-cp',str(B)+';'+cp,'Patch',B/'base.jar',B,B/'candidate.jar')
print(hashlib.sha256((B/'candidate.jar').read_bytes()).hexdigest())
