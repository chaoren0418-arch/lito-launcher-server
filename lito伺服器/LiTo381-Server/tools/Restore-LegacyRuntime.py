"""Verify archived server rollback inputs; --apply restores missing .26 inputs only.

Existing files must match. This does not restore a database or start a service.
"""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile

S = Path(__file__).resolve().parents[1]
L = S.parent.parent / 'lito登入器/LiTo381-Launcher'

def digest(data):
    return hashlib.sha256(data).hexdigest()

def checked_path(relative):
    rel = Path(relative)
    if rel.is_absolute() or '..' in rel.parts or not rel.parts or rel.parts[0] != 'runtime':
        raise ValueError(f'Unexpected rollback path: {relative}')
    path = S / rel
    for parent in (path, *path.parents):
        if parent.exists() and parent.lstat().st_file_attributes & 0x400:
            raise ValueError(f'Reparse point refused: {parent}')
    if not path.resolve().is_relative_to(S):
        raise ValueError(f'Outside server project: {path}')
    return path

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    report = json.loads((S / 'docs/maintenance/layout-cleanup-20261012.json').read_text('utf-8'))
    archive = S / report['archive']
    if digest(archive.read_bytes()) != report['archive_sha256']:
        raise ValueError('Archive checksum mismatch')
    rollback = json.loads((L / 'backups/0.6.26-before-shared-resources-20261011/manifest.json').read_text('utf-8'))
    pending = []
    with zipfile.ZipFile(archive) as z:
        manifest = json.loads(z.read('MANIFEST.json'))
        for row in rollback['files']:
            original = Path(row['original'])
            if not original.is_relative_to(S):
                continue
            path = checked_path(original.relative_to(S))
            entry = 'server/' + path.relative_to(S).as_posix()
            data = z.read(entry)
            expected = row['sha256'].lower()
            if digest(data) != expected or manifest[entry]['sha256'] != expected:
                raise ValueError(f'Rollback input mismatch: {entry}')
            if path.exists() and digest(path.read_bytes()) != expected:
                raise ValueError(f'Existing file changed; refusing overwrite: {path}')
            pending.append((path, data))
        # Validate every input before making any change.
        restored = 0
        if args.apply:
            for path, data in pending:
                checked_path(path.relative_to(S))
                if not path.exists():
                    path.parent.mkdir(parents=True, exist_ok=True)
                    with path.open('xb') as out:
                        out.write(data)
                    restored += 1
    print(json.dumps({'verified': True, 'rollback_inputs': len(pending), 'restored_missing_files': restored}))

if __name__ == '__main__':
    main()
