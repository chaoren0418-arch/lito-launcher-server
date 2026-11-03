from pathlib import Path
import subprocess,hashlib
r=Path(__file__).resolve().parent;w=r.parent
j=Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin')
base=w/'repair-032/candidate-details.jar';lib=Path(r'D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server\runtime\formal-standard-20261011\lib')
asm=Path(r'C:\Users\CHAOREN\.m2\repository\org\ow2\asm');acp=';'.join(str(asm/f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree'])
def run(*args):subprocess.run(list(map(str,args)),check=True)
run(j/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',str(base)+';'+str(lib/'*'),'-d',r/'classes',r/'LiToItemDetails.java',r/'LiToItemTemplates.java')
run(j/'javac.exe','-cp',acp,'-d',r/'classes',r/'Patch.java')
run(j/'java.exe','-cp',str(r/'classes')+';'+acp+';'+str(base),'Patch',base,r/'classes',r/'candidate.jar')
print('candidate sha256',hashlib.sha256((r/'candidate.jar').read_bytes()).hexdigest())
