from pathlib import Path
import subprocess, time
R=Path(__file__).resolve().parent;S=R.parents[1]
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
cp=';'.join(map(str,[R/'tests',R/'build/candidate.jar',S/'lib/*']))
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(R/'tests'),str(R/'SettlementSchema.java'),str(R/'SqlRegression.java')],check=True)
subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',cp,'local.lito381.server.SqlRegression',str(S/'config/server.properties'),str(R/'build'/('sql-test-'+str(time.time_ns()))),str(S.parents[2]/'1-伺服器/config/sql.properties')],check=True,timeout=90)
