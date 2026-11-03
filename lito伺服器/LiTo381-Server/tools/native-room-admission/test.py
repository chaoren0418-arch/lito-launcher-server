from pathlib import Path
import subprocess,time,sys
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
cp=str(Path(sys.argv[1]).resolve() if len(sys.argv)>1 else B/'candidate.jar')+';'+str(S/'lib/*')
sources=[S/'tools/dragon-combat/Regression.java',S/'tools/shared-database/SourceLoginRegression.java',S/'tools/native-room-state/NativeDirectoryRegression.java',R/'AdmissionRegression.java']
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],check=True)
subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.AdmissionRegression',str(S/'config/server.properties'),str(S/'runtime'/('native-room-admission-'+str(time.time_ns())))],cwd=S,check=True,timeout=75)
