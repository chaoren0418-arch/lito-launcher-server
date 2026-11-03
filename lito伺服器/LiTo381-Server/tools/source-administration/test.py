"""Verify source-account authorization and login routing without SQL writes."""
from pathlib import Path
import argparse
import subprocess
import sys

R = Path(__file__).resolve().parent
S = R.parents[1]
J = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--jar', type=Path, help='Test a published JAR instead of the candidate')
parser.add_argument('--sql', action='store_true', help='Also read-probe every live source account database')
args = parser.parse_args()
jar = args.jar or R / 'build/candidate.jar'
if not jar.is_file() and args.jar is None:
    subprocess.run([sys.executable, '-B', '-X', 'utf8', str(R / 'build.py')], check=True)
if not jar.is_file():
    parser.error(f'JAR does not exist: {jar}')
tests = R / 'build/tests'
tests.mkdir(parents=True, exist_ok=True)
output = S / 'runtime/source-administration-regression'
output.mkdir(parents=True, exist_ok=True)
cp = str(jar) + ';' + str(S / 'lib/*')
subprocess.run([str(J / 'javac.exe'), '-encoding', 'UTF-8', '-implicit:none',
                '-cp', cp, '-d', str(tests), str(R / 'Regression.java'),
                str(S / 'tools/shared-database/SourceLoginRegression.java'),
                str(R / 'CommandRoutingRegression.java')], check=True)
for name, log in [('Regression', 'authority.log'), ('SourceLoginRegression', 'source-login.log'),
                  ('CommandRoutingRegression', 'command-routing.log')]:
    command = [str(J / 'java.exe'), '-Dfile.encoding=UTF-8', '-Xverify:all',
               '-cp', str(tests) + ';' + cp, 'local.lito381.server.' + name,
               str(S / 'config/server.properties'), str(output / name)]
    if args.sql and name == 'Regression':
        command.append('--sql')
    with (output / log).open('w', encoding='utf-8') as stream:
        result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT,
                                encoding='utf-8')
    if result.returncode:
        print((output / log).read_text(encoding='utf-8'))
        result.check_returncode()
    print(f'PASS: {name}; log: {output / log}')
