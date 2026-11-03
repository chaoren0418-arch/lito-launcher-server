from pathlib import Path
import subprocess,hashlib,shutil
r=Path(__file__).resolve().parent;l=r.parents[1];s=l.parent.parent/'lito伺服器/LiTo381-Server';j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');b=r/'build';b.mkdir(exist_ok=True)
base=l/'app/server/LiTo381-SharedResources-candidate.jar'
pinned=b/'base.jar'
if not pinned.exists():shutil.copyfile(base,pinned)
assert hashlib.sha256(pinned.read_bytes()).hexdigest()=='eeb0ba0b4c62e49cc73770b69ab67f877d3607a91ecce795a7a7b968e9c86409'
asm=Path.home()/'.m2/repository/org/ow2/asm';acp=';'.join(str(asm/f'{n}/9.7/{n}-9.7.jar') for n in ['asm','asm-tree'])
def run(*args):subprocess.run(list(map(str,args)),check=True)
run(j/'javac.exe','-encoding','UTF-8','-cp',pinned,'-d',b,s/'tools/shared-database/UnifiedConfiguration.java',s/'tools/shared-database/SharedDatabase.java')
run(j/'javac.exe','-encoding','UTF-8','-cp',acp,'-d',b,r/'Patch.java')
run(j/'java.exe','-cp',str(b)+';'+acp+';'+str(pinned),'Patch',pinned,b,b/'candidate.jar')
print(hashlib.sha256((b/'candidate.jar').read_bytes()).hexdigest())
