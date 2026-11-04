#!/usr/bin/env python3
"""Scan class-file constants/instructions; generate compile-only legacy signatures.

Only Python's standard library is used. No legacy implementation is read.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import sys
import zipfile

PREFIX = 'com/lineage/'
HEADER = '// COMPILE-ONLY STUB for legacy com.lineage.*, never packaged'
THROW = 'throw new UnsupportedOperationException("compile-only stub");'
OPS = dict(zip(range(178, 186), ('getstatic', 'putstatic', 'getfield', 'putfield',
                               'invokevirtual', 'invokespecial', 'invokestatic', 'invokeinterface')))


class Reader:
    def __init__(self, data):
        self.data, self.pos = data, 0

    def take(self, size):
        end = self.pos + size
        if end > len(self.data):
            raise ValueError('truncated class file')
        result = self.data[self.pos:end]
        self.pos = end
        return result

    def u1(self):
        return self.take(1)[0]

    def u2(self):
        return struct.unpack('>H', self.take(2))[0]

    def u4(self):
        return struct.unpack('>I', self.take(4))[0]


def instructions(code):
    """Walk JVM instructions, including aligned switches and wide operands."""
    pc = 0
    while pc < len(code):
        start, op = pc, code[pc]
        pc += 1
        operand = None
        if op in (170, 171):
            pc += (-pc) % 4
            if op == 170:
                _, low, high = struct.unpack_from('>iii', code, pc)
                if high < low:
                    raise ValueError('invalid tableswitch')
                pc += 12 + 4 * (high - low + 1)
            else:
                _, count = struct.unpack_from('>ii', code, pc)
                if count < 0:
                    raise ValueError('invalid lookupswitch')
                pc += 8 + 8 * count
        elif op == 196:
            pc += 5 if code[pc] == 132 else 3
        else:
            size = 0
            if op in (16, 18, 188) or 21 <= op <= 25 or 54 <= op <= 58 or op == 169:
                size = 1
            elif op in (17, 19, 20, 132, 187, 189, 192, 193, 198, 199) or 153 <= op <= 168 or 178 <= op <= 184:
                size = 2
            elif op == 197:
                size = 3
            elif op in (185, 186, 200, 201):
                size = 4
            elif op > 201:
                raise ValueError(f'invalid opcode {op}')
            if op in OPS:
                operand = struct.unpack_from('>H', code, pc)[0]
            pc += size
        if pc > len(code):
            raise ValueError('truncated bytecode')
        yield start, op, operand


class ClassFile:
    def __init__(self, data):
        r = Reader(data)
        if r.u4() != 0xCAFEBABE:
            raise ValueError('not a class file')
        r.take(4)
        count = r.u2()
        self.cp = [None]
        while len(self.cp) < count:
            tag = r.u1()
            if tag == 1:
                # JVM modified UTF-8: preserve supplementary characters and NUL.
                value = r.take(r.u2()).replace(b'\xc0\x80', b'\0').decode('utf-8', 'surrogatepass')
                value = value.encode('utf-16', 'surrogatepass').decode('utf-16')
            elif tag in (7, 8, 16, 19, 20):
                value = r.u2()
            elif tag in (9, 10, 11, 12, 17, 18):
                value = (r.u2(), r.u2())
            elif tag == 15:
                value = (r.u1(), r.u2())
            elif tag in (3, 4):
                value = r.take(4)
            elif tag in (5, 6):
                value = r.take(8)
            else:
                raise ValueError(f'unknown constant tag {tag}')
            self.cp.append((tag, value))
            if tag in (5, 6):
                self.cp.append(None)
        self.access = r.u2()
        self.name = self.class_name(r.u2())
        r.u2()
        r.take(2 * r.u2())
        self.fields = self.members(r)
        self.methods = self.members(r)
        self.attributes(r)
        if r.pos != len(data):
            raise ValueError('trailing class data')

    def utf(self, index):
        return self.cp[index][1]

    def class_name(self, index):
        return self.utf(self.cp[index][1])

    def member_ref(self, index):
        tag, (owner, nt) = self.cp[index]
        name, desc = self.cp[nt][1]
        return tag, self.class_name(owner), self.utf(name), self.utf(desc)

    def attributes(self, r):
        return [(self.utf(r.u2()), r.take(r.u4())) for _ in range(r.u2())]

    def members(self, r):
        result = []
        for _ in range(r.u2()):
            flags, name, desc = r.u2(), self.utf(r.u2()), self.utf(r.u2())
            result.append((flags, name, desc, self.attributes(r)))
        return result


def descriptor_classes(desc):
    return re.findall(r'L([^;<>]+)', desc)


def scan(jars):
    classes, inputs = {}, []

    def ensure(name):
        if name not in classes:
            classes[name] = dict(name=name, kind='class', evidence=[], fields={}, methods={}, enum_constants=[])
        return classes[name]

    def evidence(row, item):
        if item not in row['evidence']:
            row['evidence'].append(item)

    def add_ref(owner, name, desc, tag, origin, opcode=None):
        if not owner.startswith(PREFIX):
            return
        row = ensure(owner)
        key = (name, desc)
        group = row['fields' if tag == 9 else 'methods']
        member = group.setdefault(key, dict(name=name, descriptor=desc, opcodes=[], evidence=[]))
        if opcode and opcode not in member['opcodes']:
            member['opcodes'].append(opcode)
        evidence(member, origin)
        if opcode == 'invokeinterface' or tag == 11:
            if row['kind'] == 'enum':
                raise ValueError(f'conflicting enum/interface evidence: {owner}')
            row['kind'] = 'interface'
        if opcode == 'getstatic' and desc == 'L' + owner + ';':
            # Self-typed getstatic is an enum candidate; corroborate below with
            # ACC_ENUM, values/ordinal usage or a switch map to avoid SINGLETON.
            member['enum_candidate'] = True

    for path in jars:
        inputs.append(dict(name=path.name, sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
        with zipfile.ZipFile(path) as z:
            for entry in sorted(z.namelist()):
                if not entry.endswith('.class'):
                    continue
                c = ClassFile(z.read(entry))
                source = dict(jar=path.name, entry=entry)
                for index, item in enumerate(c.cp):
                    if not item:
                        continue
                    tag, value = item
                    names = []
                    if tag == 7:
                        name = c.utf(value)
                        names = descriptor_classes(name) if name.startswith('[') else [name]
                    elif tag in (12, 16):
                        names = descriptor_classes(c.utf(value[1] if tag == 12 else value))
                    if tag in (9, 10, 11):
                        _, owner, name, desc = c.member_ref(index)
                        add_ref(owner, name, desc, tag, dict(**source, constant_pool=index))
                    elif tag == 15:
                        handle_kind, ref = value
                        ref_tag, owner, name, desc = c.member_ref(ref)
                        handle_op = {1: 'getfield', 2: 'getstatic', 3: 'putfield', 4: 'putstatic',
                                     5: 'invokevirtual', 6: 'invokestatic', 7: 'invokespecial',
                                     8: 'invokespecial', 9: 'invokeinterface'}[handle_kind]
                        add_ref(owner, name, desc, ref_tag,
                                dict(**source, constant_pool=index, reason='method-handle'), handle_op)
                    for name in names:
                        if name.startswith(PREFIX):
                            evidence(ensure(name), dict(**source, reason='constant-pool', constant_pool=index))
                for _, _, desc, _ in c.fields + c.methods:
                    for name in descriptor_classes(desc):
                        if name.startswith(PREFIX):
                            evidence(ensure(name), dict(**source, reason='descriptor', descriptor=desc))
                if c.name.startswith(PREFIX) and c.access & 0x4000:
                    ensure(c.name)['kind'] = 'enum'
                # Correlate values() -> putstatic SwitchMap; do not guess from
                # a UTF8 string alone (debug paths and arbitrary strings exist).
                for _, method, desc, attrs in c.methods:
                    for attr, raw in attrs:
                        if attr != 'Code':
                            continue
                        r = Reader(raw)
                        r.take(4)
                        code = r.take(r.u4())
                        refs = []
                        for offset, op, index in instructions(code):
                            if index is None:
                                continue
                            tag, owner, name, signature = c.member_ref(index)
                            origin = dict(**source, method=method + desc, offset=offset)
                            add_ref(owner, name, signature, tag, origin, OPS[op])
                            refs.append((op, owner, name, signature, origin))
                        aliases = {}
                        for i, (op, owner, name, signature, origin) in enumerate(refs):
                            if op != 179 or not name.startswith('$SwitchMap$com$lineage$') or signature != '[I' or i == 0:
                                continue
                            prev_op, runtime, prev_name, prev_desc, _ = refs[i - 1]
                            if prev_op != 184 or prev_name != 'values' or prev_desc != '()[L' + runtime + ';':
                                raise ValueError(f'unrecognized switch map initialization: {entry} {name}')
                            encoded = name[len('$SwitchMap$'):]
                            # The actual enum gives the nesting depth, so an uppercase
                            # package such as model/Instance is not mistaken for a type.
                            parts = encoded.split('$')
                            depth = runtime.rsplit('/', 1)[-1].count('$') + 1
                            legacy = '/'.join(parts[:-depth]) + '/' + '$'.join(parts[-depth:])
                            row = ensure(legacy)
                            row['kind'] = 'enum'
                            evidence(row, dict(**origin, reason='switch-map', field=name, runtime_enum=runtime))
                            aliases[runtime] = legacy
                        for op, owner, name, signature, origin in refs:
                            if owner in aliases:
                                legacy = aliases[owner]
                                mapped = signature.replace('L' + owner + ';', 'L' + legacy + ';')
                                add_ref(legacy, name, mapped, 9 if op <= 181 else 10,
                                        dict(**origin, reason='switch-map-alias', runtime_owner=owner), OPS[op])

    # Include enclosing declarations needed for Java's nested-type syntax.
    for name in list(classes):
        outer = name
        while '$' in outer:
            outer = outer.rsplit('$', 1)[0]
            evidence(ensure(outer), dict(reason='enclosing-type', nested=name))
    for name, row in sorted(classes.items()):
        methods = row['methods']
        enum_api = (('values', '()[L' + name + ';') in methods or ('ordinal', '()I') in methods)
        if enum_api and any(m.get('enum_candidate') for m in row['fields'].values()):
            row['kind'] = 'enum'
        for field in row['fields'].values():
            if field.pop('enum_candidate', False) and row['kind'] == 'enum':
                row['enum_constants'].append(field['name'])
        row['enum_constants'].sort()
        for group in ('fields', 'methods'):
            result = []
            for _, member in sorted(row[group].items()):
                uses = set(member['opcodes'])
                static = bool(uses & {'getstatic', 'putstatic', 'invokestatic'})
                instance = bool(uses & {'getfield', 'putfield', 'invokevirtual', 'invokespecial', 'invokeinterface'})
                if static and instance:
                    raise ValueError(f'inconsistent static/instance member: {name}.{member["name"]}')
                member['static'] = static if uses else None
                member['opcodes'].sort()
                result.append(member)
            row[group] = result
    return dict(schema_version=1, inputs=inputs, classes=[classes[n] for n in sorted(classes)])


def java_type(desc, pos=0):
    start = pos
    while desc[pos] == '[':
        pos += 1
    arrays = pos - start
    code = desc[pos]
    if code == 'L':
        end = desc.index(';', pos)
        name, pos = desc[pos + 1:end].replace('/', '.').replace('$', '.'), end + 1
    else:
        name = dict(B='byte', C='char', D='double', F='float', I='int', J='long', S='short', Z='boolean', V='void')[code]
        pos += 1
    return name + '[]' * arrays, pos


def generate(inventory):
    rows = {r['name']: r for r in inventory['classes']}

    def declaration(name, level):
        row = rows[name]
        simple = name.rsplit('/', 1)[-1].rsplit('$', 1)[-1]
        pad = '    ' * level
        kind = row['kind']
        modifier = 'public ' + ('static ' if level and kind == 'class' else '')
        lines = [pad + modifier + kind + ' ' + simple + ' {']
        body = pad + '    '
        if kind == 'enum':
            lines.append(body + ', '.join(row['enum_constants']) + ';')
        for field in row['fields']:
            if field['name'] in row['enum_constants']:
                continue
            if field['static'] is None:
                raise ValueError(f'unused field has unknown modifiers: {name}.{field["name"]}')
            if kind == 'interface':
                raise ValueError('interface fields need a non-constant throwing initializer; unsupported')
            typ, _ = java_type(field['descriptor'])
            lines.append(body + 'public ' + ('static ' if field['static'] else '') + typ + ' ' + field['name'] + ';')
        constructors = False
        for method in row['methods']:
            method_name, desc = method['name'], method['descriptor']
            if kind == 'enum' and (method_name, desc) in {
                ('values', '()[L' + name + ';'), ('valueOf', '(Ljava/lang/String;)L' + name + ';'), ('ordinal', '()I'),
                ('name', '()Ljava/lang/String;')}:
                continue  # javac-generated or final inherited java.lang.Enum API
            if method['static'] is None:
                raise ValueError(f'unused method has unknown modifiers: {name}.{method_name}')
            args, pos = [], 1
            while desc[pos] != ')':
                typ, pos = java_type(desc, pos)
                args.append(typ + ' arg' + str(len(args)))
            result, _ = java_type(desc, pos + 1)
            if method_name == '<init>':
                constructors = True
                signature = 'public ' + simple
            else:
                modifier = 'static ' if method['static'] else ('default ' if kind == 'interface' else '')
                signature = 'public ' + modifier + result + ' ' + method_name
            lines.append(body + signature + '(' + ', '.join(args) + ') { ' + THROW + ' }')
        if kind == 'class' and not constructors:
            # Suppress an accidental public, working implicit constructor.
            lines.append(body + 'private ' + simple + '() { ' + THROW + ' }')
        children = [n for n in rows if '$' in n and n.rsplit('$', 1)[0] == name]
        for child in sorted(children):
            lines.extend(declaration(child, level + 1))
        lines.append(pad + '}')
        return lines

    files = {}
    for name in sorted(rows):
        if '$' in name:
            continue
        package = name.rsplit('/', 1)[0].replace('/', '.')
        files[name + '.java'] = '\n'.join([HEADER, '// Generated by tools/legacy-stubs/scan.py; do not edit.',
                                        'package ' + package + ';', '', *declaration(name, 0), ''])
    return files


def main():
    server = Path(__file__).resolve().parents[2]
    repo = server.parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jars', nargs=2, type=Path, default=[server / 'reference/base-0.6.71.jar', server / 'reference/candidate-50635f7e.jar'])
    parser.add_argument('--output', type=Path, default=repo / 'docs/combat-merge/legacy-stubs.json')
    parser.add_argument('--stubs', type=Path, default=server / 'compile-stubs')
    parser.add_argument('--check', action='store_true', help='compare generated files without writing')
    args = parser.parse_args()
    inventory = scan(args.jars)
    generated = generate(inventory)
    expected = {args.output: json.dumps(inventory, ensure_ascii=False, indent=2) + '\n'}
    expected.update({args.stubs / name: text for name, text in generated.items()})
    extras = set(args.stubs.rglob('*.java')) - {args.stubs / name for name in generated}
    if extras:
        raise ValueError('stale/unmanaged Java files; review manually: ' + ', '.join(map(str, sorted(extras))))
    differences = []
    for path, text in expected.items():
        if args.check:
            if not path.is_file() or path.read_text(encoding='utf-8') != text:
                differences.append(str(path))
        else:
            if path.is_symlink() or (path.exists() and path.stat().st_nlink != 1):
                raise ValueError(f'refusing linked output: {path}')
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8', newline='\n')
    if differences:
        raise ValueError('generated output differs: ' + ', '.join(differences))
    print(f'{"CHECK PASS" if args.check else "GENERATED"}: {len(inventory["classes"])} types, {len(generated)} Java files, '
          f'{sum(len(r["enum_constants"]) for r in inventory["classes"])} enum constants')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        print(f'ERROR: {error}', file=sys.stderr)
        sys.exit(1)
