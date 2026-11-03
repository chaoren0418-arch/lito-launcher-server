"""Byte-preserving offline candidate index. No codec, runtime or resource binding.

Candidate must be an exact filename. Key queries use only explicit first-token
ASCII decimal declarations, never physical line number or candidate alignment.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

SCHEMA = 'LTmapstr-byte-candidate-index-v1'
GUARD = ('Candidate bytes only. File key, physical line, header count and token '
         'position do not qualify runtime table index, locale, label codec or '
         'map/resource ID. No cross-candidate alignment or title-column inference.')
POLICY = {
    'encoding': 'bytes authoritative; printable ASCII view only; no codec inference or non-ASCII decode',
    'physical_lines': 'CRLF, standalone CR, standalone LF only; retain terminator; terminal EOL creates no phantom row',
    'tokens': 'literal byte 23 (#), retain every empty/trailing token and whitespace; no escape grammar inferred',
    'header': 'first nonblank physical line, standalone unsigned ASCII decimal after SP/HT trim; observed count declaration only',
    'comment': 'first non-SP/HT byte is 3B (;); preserve whole content; other semicolons retained without inline truncation',
    'file_key': 'non-header non-comment hash row, first token SP/HT-trimmed signed ASCII decimal; arbitrary precision; no width coercion',
    'commented_key': 'separate inactive declaration after exactly one leading comment marker; never active file key',
    'placeholder': 'token SP/HT-trimmed equals exact lowercase ASCII no exist or no exsist; marker flag, not deleted',
    'label_metadata': 'all tokens returned with positions and bytes; no canonical title column or fallback key',
    'duplicate_keys': 'all matching physical rows in source order; no first/last winner',
}

def sha(data):
    return hashlib.sha256(data).hexdigest().upper()

def printable_ascii(data):
    return data.decode('ascii') if all(32 <= x <= 126 for x in data) else None

def decimal(data, signed=True):
    value = data.strip(b' \t')
    return int(value) if re.fullmatch(rb'[+-]?[0-9]+' if signed else rb'[0-9]+', value) else None

def parse_bytes(data, candidate, provenance):
    rows = []
    start = 0
    # A byte-level partition; bytes.splitlines() would also split at 0x85 etc.
    while start < len(data):
        end = start
        while end < len(data) and data[end] not in (10, 13):
            end += 1
        stop = end
        if stop < len(data):
            stop += 2 if data[end:end+2] == b'\r\n' else 1
        content, eol = data[start:end], data[end:stop]
        parts = content.split(b'#')
        tokens, position = [], start
        for n, part in enumerate(parts):
            tokens.append({'token_0based': n, 'byte_offset': position,
                           'byte_length': len(part), 'raw_hex': part.hex(),
                           'printable_ascii': printable_ascii(part)})
            position += len(part) + 1
        stripped = content.lstrip(b' \t')
        comment = stripped.startswith(b';')
        commented_key = decimal(stripped[1:].split(b'#', 1)[0]) if comment and b'#' in content else None
        markers = [{'token_0based': n, 'marker_ascii': part.strip(b' \t').decode('ascii')}
                   for n, part in enumerate(parts) if part.strip(b' \t') in (b'no exist', b'no exsist')]
        rows.append({
            'physical_line_1based': len(rows)+1, 'byte_offset': start,
            'content_byte_length': len(content), 'full_byte_length': stop-start,
            'raw_bytes_hex': data[start:stop].hex(), 'content_hex': content.hex(),
            'content_sha256': sha(content), 'eol_hex': eol.hex(),
            'eol_kind': {b'\r\n':'CRLF', b'\r':'CR', b'\n':'LF', b'':'NONE'}[eol],
            'kind': 'blank' if not content.strip(b' \t') else 'full_line_comment' if comment else 'hash_delimited' if b'#' in content else 'plain_bytes',
            'hash_delimiter_byte_offsets': [start+i for i, byte in enumerate(content) if byte == 35],
            'semicolon_byte_offsets': [start+i for i, byte in enumerate(content) if byte == 59],
            'tokens': tokens, 'full_line_comment': comment,
            'comment_marker_byte_offset': start+len(content)-len(stripped) if comment else None,
            'commented_file_key': commented_key,
            'file_declared_key': None if comment or b'#' not in content else decimal(parts[0]),
            'file_key_lexeme_hex': None if comment or b'#' not in content or decimal(parts[0]) is None else parts[0].hex(),
            'placeholder_markers': markers,
        })
        start = stop
    first = next((r for r in rows if r['kind'] != 'blank'), None)
    header = decimal(bytes.fromhex(first['content_hex']), signed=False) if first else None
    header_line = first['physical_line_1based'] if header is not None else None
    if header is not None:
        first['kind'] = 'header_count'
        first['file_declared_key'] = None
        first['file_key_lexeme_hex'] = None
    after = [r for r in rows if header_line is None or r['physical_line_1based'] > header_line]
    nonblank = [r for r in after if r['kind'] != 'blank']
    active = [r for r in nonblank if not r['full_line_comment']]
    counts = {
        'physical_rows': len(rows), 'header_rows': int(header is not None),
        'physical_rows_after_header': len(after), 'nonblank_rows_after_header': len(nonblank),
        'comment_rows_after_header': sum(r['full_line_comment'] for r in after),
        'noncomment_nonblank_rows_after_header': len(active),
        'hash_rows_after_header_including_comments': sum(bool(r['hash_delimiter_byte_offsets']) for r in after),
        'noncomment_hash_rows_after_header': sum(bool(r['hash_delimiter_byte_offsets']) for r in active),
        'noncomment_plain_rows_after_header': sum(r['kind'] == 'plain_bytes' for r in active),
        'explicit_numeric_key_rows': sum(r['file_declared_key'] is not None for r in rows),
        'commented_numeric_key_rows': sum(r['commented_file_key'] is not None for r in rows),
        'placeholder_rows_including_comments': sum(bool(r['placeholder_markers']) for r in after),
        'noncomment_placeholder_rows': sum(bool(r['placeholder_markers']) for r in active),
        'hash_delimiters': sum(len(r['hash_delimiter_byte_offsets']) for r in rows),
        'tokens': sum(len(r['tokens']) for r in rows),
    }
    by_key = {}
    for r in rows:
        if r['file_declared_key'] is not None:
            by_key.setdefault(str(r['file_declared_key']), []).append(r['physical_line_1based'])
    reconstructed = b''.join(bytes.fromhex(r['raw_bytes_hex']) for r in rows)
    assert reconstructed == data
    assert all(b'#'.join(bytes.fromhex(t['raw_hex']) for t in r['tokens']) == bytes.fromhex(r['content_hex']) for r in rows)
    return {
        'schema': SCHEMA, 'candidate': candidate, 'provenance': provenance,
        'policy': POLICY, 'authority_guard': GUARD,
        'source_byte_length': len(data), 'source_sha256': sha(data),
        'header_declared_count': header, 'header_physical_line_1based': header_line,
        'counts': counts,
        'count_minus_header': {k: counts[k]-header if header is not None else None for k in (
            'physical_rows_after_header','nonblank_rows_after_header',
            'noncomment_nonblank_rows_after_header','noncomment_hash_rows_after_header')},
        'terminal_eol': bool(rows and rows[-1]['eol_hex']),
        'eol_counts': {kind: sum(r['eol_kind'] == kind for r in rows) for kind in ('CRLF','CR','LF','NONE')},
        'duplicate_explicit_file_keys': {k: v for k, v in by_key.items() if len(v)>1},
        'coverage': {'reconstructed_source_sha256': sha(reconstructed), 'all_source_bytes_preserved': True,
                     'all_token_bytes_preserved': True, 'byte_interval': [0,len(data)]},
        'rows': rows,
    }

class CandidateIndex:
    """Portable JSON index reader. Never needs original archives or runtime."""
    def __init__(self, catalog=None):
        self.path = Path(catalog) if catalog else Path(__file__).with_name('map-candidate-index')/'catalog.json'
        self.catalog = json.loads(self.path.read_text(encoding='utf-8'))
        if self.catalog['schema'] != SCHEMA:
            raise ValueError('unsupported candidate catalog schema')
        self.entries = {e['candidate']: e for e in self.catalog['candidates']}
        if len(self.entries) != len(self.catalog['candidates']):
            raise ValueError('duplicate candidate filename')

    def list_candidates(self):
        return list(self.entries)

    def _load(self, candidate):
        entry = self.entries[candidate]  # Exact match only: no suffix/locale fallback.
        relative = Path(entry['relative_file'])
        if relative.is_absolute() or len(relative.parts) != 1:
            raise ValueError('index file must be a catalog sibling')
        raw = (self.path.parent/relative).read_bytes()
        if len(raw) != entry['parsed_pin']['bytes'] or sha(raw) != entry['parsed_pin']['sha256']:
            raise ValueError('parsed index pin mismatch')
        parsed = json.loads(raw)
        if (parsed['schema'] != SCHEMA or parsed['candidate'] != candidate or
            parsed['provenance'] != entry['provenance'] or
            parsed['source_sha256'] != entry['provenance']['decoded']['sha256'] or
            parsed['source_byte_length'] != entry['provenance']['decoded']['bytes']):
            raise ValueError('candidate provenance mismatch')
        return parsed

    def metadata(self, candidate):
        return {k: v for k, v in self._load(candidate).items() if k != 'rows'}

    def line(self, candidate, physical_line_1based):
        if type(physical_line_1based) is not int or physical_line_1based < 1:
            raise ValueError('physical line must be a positive integer')
        rows = self._load(candidate)['rows']
        if physical_line_1based > len(rows):
            raise IndexError('physical line outside candidate')
        return rows[physical_line_1based-1]

    def key(self, candidate, file_declared_key, include_comments=False):
        if type(file_declared_key) is not int:
            raise TypeError('file key must be an integer; no ordinal fallback')
        return [r for r in self._load(candidate)['rows'] if
                r['file_declared_key'] == file_declared_key or
                (include_comments and r['commented_file_key'] == file_declared_key)]

    def token(self, candidate, token_bytes, include_comments=False):
        if not isinstance(token_bytes, bytes):
            raise TypeError('token query requires exact bytes')
        needle = token_bytes.hex()
        return [r for r in self._load(candidate)['rows'] if
                (include_comments or not r['full_line_comment']) and
                any(t['raw_hex'] == needle for t in r['tokens'])]

    def label_metadata_for_key(self, candidate, file_declared_key, include_comments=False):
        return {'candidate': candidate, 'file_declared_key': file_declared_key,
                'authority_guard': GUARD, 'label_column_qualified': False,
                'matches': self.key(candidate, file_declared_key, include_comments)}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--catalog')
    parser.add_argument('--candidate', help='exact filename; required except list')
    parser.add_argument('--include-comments', action='store_true')
    parser.add_argument('operation', choices=['list','metadata','line','key','label-metadata','token-hex','token-ascii'])
    parser.add_argument('value', nargs='?')
    args = parser.parse_args()
    index = CandidateIndex(args.catalog)
    if args.operation != 'list' and not args.candidate:
        parser.error('--candidate is required')
    if args.operation not in ('list','metadata') and args.value is None:
        parser.error('value is required')
    if args.operation == 'list': result = index.list_candidates()
    elif args.operation == 'metadata': result = index.metadata(args.candidate)
    elif args.operation == 'line': result = index.line(args.candidate, int(args.value))
    elif args.operation == 'key': result = index.key(args.candidate, int(args.value), args.include_comments)
    elif args.operation == 'label-metadata': result = index.label_metadata_for_key(args.candidate, int(args.value), args.include_comments)
    else:
        query = bytes.fromhex(args.value) if args.operation == 'token-hex' else args.value.encode('ascii',errors='strict')
        result = index.token(args.candidate, query, args.include_comments)
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
