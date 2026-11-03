from pathlib import Path
import subprocess
r=Path(__file__).resolve().parent;j=Path(r'C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot\bin');a=Path(r'C:\Users\CHAOREN\.m2\repository\org\ow2\asm');cp=';'.join(str(a/f'{x}/9.7/{x}-9.7.jar') for x in ['asm','asm-tree'])
for args in [[j/'javac.exe','-cp',cp,'-d',r/'classes',r/'PatchDetails.java'],[j/'java.exe','-cp',str(r/'classes')+';'+cp+';'+str(r/'candidate.jar'),'PatchDetails',r/'candidate.jar',r/'classes/local/lito381/server/LiToItemDetails.class',r/'candidate-details.jar']]:subprocess.run(list(map(str,args)),check=True)
