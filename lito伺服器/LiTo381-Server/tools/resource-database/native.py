"""Single entry for the reproducible native development build and its qualification."""
from pathlib import Path
import argparse,subprocess,sys

def main():
 parser=argparse.ArgumentParser(description='LiTo native development build and verification')
 parser.add_argument('command',choices=['build','test','audit'],help='clean five-stage build, frozen native/application tests, or read-only evidence audit')
 parser.add_argument('--require-current-source',action='store_true',help='audit must also match the live external source corpus')
 args=parser.parse_args()
 if args.require_current_source and args.command!='audit':parser.error('--require-current-source applies to audit')
 root=Path(__file__).resolve().parent/'build'
 commands={'build':['build-native-integrated.py'],'test':['run-native-integrated-regressions.py','run-native-integrated-application.py'],'audit':['audit-native-integrated.py']}
 for name in commands[args.command]:
  result=subprocess.run([sys.executable,'-B','-X','utf8',str(root/name),*(['--require-current-source'] if args.require_current_source else [])])
  if result.returncode:return result.returncode
 return 0

if __name__=='__main__':raise SystemExit(main())
