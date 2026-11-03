from pathlib import Path
import subprocess, hashlib
R=Path(__file__).resolve().parent; S=R.parents[1]; L=S.parent.parent/'lito登入器/LiTo381-Launcher'
B=R/'build'; B.mkdir(exist_ok=True); C=B/'classes'; C.mkdir(exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
base=B/'base.jar'
if not base.exists(): base.write_bytes((L/'app/server/LiTo381-SharedResources-candidate.jar').read_bytes())
assert hashlib.sha256(base.read_bytes()).hexdigest()=='bbc59308c43df89ee5029d3b3c802be8ad55fee65c41faca99a76912dd5dce43'
asm=Path.home()/'.m2/repository/org/ow2/asm'
cp=';'.join(str(asm/f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree'])
def run(*a): subprocess.run(list(map(str,a)),check=True)
run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',base,'-d',C,*[S/'src/main/java/local/lito381/server'/f'{x}.java' for x in ['NativeRoomRules','DurableBattleSettlement']])
run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',B,R/'Patch.java')
run(J/'java.exe','-cp',str(B)+';'+str(base)+';'+cp,'Patch',base,C,B/'candidate.jar')
print(hashlib.sha256((B/'candidate.jar').read_bytes()).hexdigest())
