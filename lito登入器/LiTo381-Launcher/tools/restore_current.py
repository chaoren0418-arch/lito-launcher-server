"""Verify the 0.6.66 archive; --extract-to DIR produces a separate reviewable restore tree."""
from pathlib import Path
import argparse,json,hashlib
import baseline_066 as b
parser=argparse.ArgumentParser();parser.add_argument('--extract-to',type=Path);args=parser.parse_args()
r=json.loads(b.RECEIPT.read_text('utf-8'));assert b.sha(b.ARCHIVE)==r['baseline']['sha256']
m=b.verify_archive(b.ARCHIVE)
if args.extract_to:
 import zipfile
 target=args.extract_to.absolute()
 assert not target.exists(), 'Restore directory must not already exist'
 for parent in target.parents:assert not parent.is_symlink() and not parent.is_junction(), 'Reparse path refused'
 with zipfile.ZipFile(b.ARCHIVE) as z:
  for row in m['files']:
   rel=Path(row['path']);assert not rel.is_absolute() and '..' not in rel.parts and rel.parts[0] in b.ROOTS
   p=target/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(row['path']))
 print('Extracted verified launcher/ and server/ trees to',target)
else:print('PASS: sealed 0.6.66 restore contents verified; use --extract-to DIR for an isolated restore tree')
