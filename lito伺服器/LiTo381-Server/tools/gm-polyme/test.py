from pathlib import Path
import subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher';B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=str(B/'candidate.jar')+';'+str(S/'lib/*')
files=[R/'PolyMeRegression.java',R/'Database.java',S/'tools/resource-database/TravelRegression.java',S/'tools/dragon-combat/Regression.java',S/'tools/shared-database/SourceLoginRegression.java',S/'tools/native-room-state/NativeDirectoryRegression.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,files)],check=True)
for name in ['PolyMeRegression','TravelRegression']:
 with (B/(name+'.log')).open('w',encoding='utf-8') as f:
  result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.'+name,str(S/'config/server.properties'),str(S/'runtime/gm-polyme-regression'/name)],cwd=S,stdout=f,stderr=subprocess.STDOUT,timeout=90)
 print((B/(name+'.log')).read_text('utf-8')[-4500:])
 if result.returncode:sys.exit(result.returncode)
