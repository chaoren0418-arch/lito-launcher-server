from pathlib import Path
import subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
jar=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else B/'candidate.jar'
cp=str(jar)+';'+str(S/'lib/*')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),str(S/'tools/dragon-combat/Regression.java'),str(R/'TargetRegression.java'),str(S/'tools/shared-database/SourceLoginRegression.java')],check=True)
log=S/'runtime/illusion-target-regression.log'
with log.open('w',encoding='utf-8') as stream:
 result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.TargetRegression',str(S/'config/server.properties'),str(S/'runtime/illusion-target-regression')],cwd=S,stdout=stream,stderr=subprocess.STDOUT)
text=log.read_text('utf-8')
print('\n'.join(text.splitlines()[-5:]));result.check_returncode()
