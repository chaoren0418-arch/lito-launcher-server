from pathlib import Path
import subprocess, hashlib, zipfile
r=Path(__file__).resolve().parent
s=r.parent.parent
l=s.parent.parent/'lito登入器/LiTo381-Launcher'
base=l/'app/server/LiTo381-SharedResources-candidate.jar'
j=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
asm=Path.home()/'.m2/repository/org/ow2/asm'
acp=';'.join(str(asm/f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree'])
classes=r/'build/classes';classes.mkdir(parents=True,exist_ok=True)
if hashlib.sha256(base.read_bytes()).hexdigest()!='3f31ef32c8ca1ca8f9507897f03b54ca37593326ad986b5c10fcff68f695ed5c':
 base=r/'build/base-0.6.35.jar'
 assert hashlib.sha256(base.read_bytes()).hexdigest()=='3f31ef32c8ca1ca8f9507897f03b54ca37593326ad986b5c10fcff68f695ed5c', 'Retained historical input mismatch'
cp=str(base)+';'+str(s/'lib/*')
def run(*args): subprocess.run(list(map(str,args)),check=True)
run(j/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',cp,'-d',classes,r/'SharedDatabase.java',r/'SourceLogin.java')
run(j/'javac.exe','-cp',acp,'-d',classes,r/'PatchShared.java')
run(j/'java.exe','-cp',str(classes)+';'+acp+';'+cp,'PatchShared',base,classes,r/'build/candidate.jar')
run(j/'javac.exe','-cp',acp,'-d',classes,r/'PatchSourceLogin.java')
run(j/'java.exe','-cp',str(classes)+';'+acp+';'+cp,'PatchSourceLogin',r/'build/candidate.jar',classes,r/'build/source-login.jar')
(r/'build/source-login.jar').replace(r/'build/candidate.jar')
print('Candidate SHA256:',hashlib.sha256((r/'build/candidate.jar').read_bytes()).hexdigest())
