from pathlib import Path
import subprocess,hashlib,json
r=Path(__file__).resolve().parent;w=r.parent;s=Path(r'D:\lineage開發\3-lito開發\lito伺服器\LiTo381-Server');j=Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin');a=Path(r'C:\Users\CHAOREN\.m2\repository\org\ow2\asm');acp=';'.join(str(a/f'{x}/9.7/{x}-9.7.jar') for x in ['asm','asm-tree']);base=w/'server-buff/LiTo381-SharedResources-candidate.jar';cp=str(base)+';'+str(s/'runtime/formal-standard-20261011/lib/*')
def run(*args):subprocess.run(list(map(str,args)),check=True)
run(j/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',cp,'-d',r/'classes',r/'donor/ClientSession.java',r/'LiToBotEffects.java',r/'LiToItemDetails.java')
run(j/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',str(r/'classes')+';'+str(s/'target/classes')+';'+cp,'-d',r/'classes',r/'donor/GameHub.java')
run(j/'javac.exe','-cp',acp,'-d',r/'classes',r/'PatchSkills.java')
run(j/'java.exe','-cp',str(r/'classes')+';'+acp+';'+str(base),'PatchSkills',base,r/'classes/local/lito381/server/ClientSession.class',r/'candidate.jar')
run('python','-X','utf8',r/'build-details.py')
print('candidate sha256',hashlib.sha256((r/'candidate-details.jar').read_bytes()).hexdigest())
