from pathlib import Path
import subprocess,time
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
cp=str(B/'candidate.jar')+';'+str(S/'lib/mysql-connector-j-8.0.33.jar')
sources=[R/'ResourceRegression.java',S/'tools/dragon-combat/Regression.java',S/'tools/shared-database/SourceLoginRegression.java',S/'tools/native-room-state/NativeDirectoryRegression.java',S/'tools/room-rules/RoomRulesRegression.java',S/'tools/durable-settlement/FlowRegression.java',S/'tools/native-room-state/MovementRegression.java',R/'MovementLockRegression.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],check=True)
for name in ['ResourceRegression','RoomRulesRegression','FlowRegression','NativeDirectoryRegression','MovementRegression','MovementLockRegression']:
 with (B/(name+'.log')).open('w',encoding='utf-8') as log:
  result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8',*(['-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(S.parent.parent/'lito登入器/LiTo381-Launcher/app/common_catalog.bin')] if name=='ResourceRegression' else []),'-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.'+name,str(S/'config/server.properties'),str(S/'runtime'/('resources-062-'+name+'-'+str(time.time_ns())))],cwd=S,stdout=log,stderr=subprocess.STDOUT,timeout=85)
 print(name, result.returncode, flush=True)
 if result.returncode:print((B/(name+'.log')).read_text(encoding='utf-8')[-6000:]);raise SystemExit(result.returncode)
