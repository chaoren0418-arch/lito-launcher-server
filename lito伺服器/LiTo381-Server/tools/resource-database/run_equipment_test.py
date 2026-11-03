from pathlib import Path
import subprocess,sys,time
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');L=S.parent.parent/'lito登入器/LiTo381-Launcher'
jar=Path(sys.argv[1]) if len(sys.argv)>1 else B/'candidate.jar'
cp=str(jar)+';'+str(S/'lib/*')
sources=[R/'EquipmentRegression.java',S/'tools/dragon-combat/Regression.java',S/'tools/shared-database/SourceLoginRegression.java',S/'tools/native-room-state/NativeDirectoryRegression.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'equipment-tests'),*map(str,sources)],check=True)
log=B/'equipment-toggle'/('test-'+str(time.time_ns())+'.log');log.parent.mkdir(exist_ok=True)
with log.open('w',encoding='utf-8') as f:
 result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Xverify:all','-cp',str(B/'equipment-tests')+';'+cp,'local.lito381.server.EquipmentRegression',str(S/'config/server.properties'),str(B/'equipment-toggle'/('run-'+str(time.time_ns())))],cwd=S,stdout=f,stderr=subprocess.STDOUT,timeout=100)
print(log);print(log.read_text(encoding='utf-8')[-7000:]);sys.exit(result.returncode)
