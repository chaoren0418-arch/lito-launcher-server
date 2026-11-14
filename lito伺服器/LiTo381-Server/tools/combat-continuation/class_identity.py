"""Class identity guards for historical combat regressions (#418 / T142).

Historical drivers assert that selected classes are byte-for-byte identical to
the historical base the probes/fixtures were qualified against.  That stays the
rule for the patched candidates (mode ``exact``).

A Maven build recompiles every class, so bytes differ even when the compiled
program is the same (constant-pool order, debug attributes, javac layout).
Mode ``maven-normalized`` is an explicit, receipted compatibility mode: two
classes are accepted only if their *normalized* form is equal.  The normalized
form is a pure-Python decode of the class file (no JVM):

* every constant-pool index is replaced by the symbolic value it names
  (so pool order is irrelevant), ``ldc_w``/``goto_w``/``jsr_w`` are folded into
  ``ldc``/``goto``/``jsr`` and branch/switch/exception-table offsets become
  instruction indices (so wide/narrow encodings do not shift anything);
* only debug/verifier metadata is dropped: ``SourceFile``,
  ``SourceDebugExtension``, ``LineNumberTable``, ``LocalVariableTable``,
  ``LocalVariableTypeTable`` and ``StackMapTable`` (derived from the code,
  which is compared instruction by instruction; runs use ``-Xverify:all``);
* fields and methods are compared as sets keyed by name+descriptor; access
  flags, max_stack/max_locals, every instruction and operand, the exception
  table, signatures, inner-class tables, bootstrap methods and any other
  attribute (raw bytes if not decoded) stay in the comparison.

Any remaining difference fails and is reported per field/method.

One narrow exception (#428 / T146): when the *only* remaining difference is the
``InnerClasses`` table, a row that describes *another* class may differ in
ACC_FINAL alone, provided the runtime under test contains that class and it
declares exactly the compared flags.  Such a row is javac's copy of the other
class's declaration (the JVM does not use it for this class); ASM-patched
candidates keep the stale copy, recompiled Maven classes refresh it.  Same row
set, same visibility, and this class's own row must still match exactly.

Overlay checks (#435 / T149) may name *receiver hooks*: probe calls that an
observer inserted together with an ``aload_0`` directly before them.  Only that
one load is removed with the call; everything else still has to match.
"""
import hashlib
import json
import struct

EXACT = 'exact'
MAVEN = 'maven-normalized'
MODES = (EXACT, MAVEN)
DROPPED_ATTRIBUTES = frozenset({'SourceFile', 'SourceDebugExtension', 'LineNumberTable', 'LocalVariableTable',
                                'LocalVariableTypeTable', 'StackMapTable'})

_NAMES = ('nop aconst_null iconst_m1 iconst_0 iconst_1 iconst_2 iconst_3 iconst_4 iconst_5 lconst_0 lconst_1 '
          'fconst_0 fconst_1 fconst_2 dconst_0 dconst_1 bipush sipush ldc ldc_w ldc2_w iload lload fload dload aload '
          'iload_0 iload_1 iload_2 iload_3 lload_0 lload_1 lload_2 lload_3 fload_0 fload_1 fload_2 fload_3 '
          'dload_0 dload_1 dload_2 dload_3 aload_0 aload_1 aload_2 aload_3 iaload laload faload daload aaload '
          'baload caload saload istore lstore fstore dstore astore istore_0 istore_1 istore_2 istore_3 lstore_0 '
          'lstore_1 lstore_2 lstore_3 fstore_0 fstore_1 fstore_2 fstore_3 dstore_0 dstore_1 dstore_2 dstore_3 '
          'astore_0 astore_1 astore_2 astore_3 iastore lastore fastore dastore aastore bastore castore sastore '
          'pop pop2 dup dup_x1 dup_x2 dup2 dup2_x1 dup2_x2 swap iadd ladd fadd dadd isub lsub fsub dsub imul lmul '
          'fmul dmul idiv ldiv fdiv ddiv irem lrem frem drem ineg lneg fneg dneg ishl lshl ishr lshr iushr lushr '
          'iand land ior lor ixor lxor iinc i2l i2f i2d l2i l2f l2d f2i f2l f2d d2i d2l d2f i2b i2c i2s lcmp fcmpl '
          'fcmpg dcmpl dcmpg ifeq ifne iflt ifge ifgt ifle if_icmpeq if_icmpne if_icmplt if_icmpge if_icmpgt '
          'if_icmple if_acmpeq if_acmpne goto jsr ret tableswitch lookupswitch ireturn lreturn freturn dreturn '
          'areturn return getstatic putstatic getfield putfield invokevirtual invokespecial invokestatic '
          'invokeinterface invokedynamic new newarray anewarray arraylength athrow checkcast instanceof '
          'monitorenter monitorexit wide multianewarray ifnull ifnonnull goto_w jsr_w').split()
assert len(_NAMES) == 202
_FOLD = {'ldc_w': 'ldc', 'goto_w': 'goto', 'jsr_w': 'jsr'}
_CP1 = {0x12}
_CP2 = {0x13, 0x14, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1}
_LOCAL1 = set(range(0x15, 0x1a)) | set(range(0x36, 0x3b)) | {0xa9}
_BRANCH2 = set(range(0x99, 0xa9)) | {0xc6, 0xc7}
_BRANCH4 = {0xc8, 0xc9}


class ClassFormatError(ValueError):
    pass


class _Reader:
    def __init__(self, data, pos=0):
        self.data, self.pos = data, pos

    def take(self, n):
        if self.pos + n > len(self.data):
            raise ClassFormatError('truncated class file')
        value = self.data[self.pos:self.pos + n]
        self.pos += n
        return value

    def u1(self):
        return self.take(1)[0]

    def u2(self):
        return struct.unpack('>H', self.take(2))[0]

    def s2(self):
        return struct.unpack('>h', self.take(2))[0]

    def u4(self):
        return struct.unpack('>I', self.take(4))[0]

    def s4(self):
        return struct.unpack('>i', self.take(4))[0]


class _Pool:
    def __init__(self, reader):
        count = reader.u2()
        self.entries = [None] * count
        i = 1
        while i < count:
            tag = reader.u1()
            if tag == 1:
                raw = reader.take(reader.u2())
                self.entries[i] = ('Utf8', raw.decode('utf-8', errors='surrogateescape'))
            elif tag in (3, 4):
                self.entries[i] = ({3: 'Integer', 4: 'Float'}[tag], reader.take(4).hex())
            elif tag in (5, 6):
                self.entries[i] = ({5: 'Long', 6: 'Double'}[tag], reader.take(8).hex())
                i += 1
            elif tag in (7, 8, 16, 19, 20):
                self.entries[i] = ({7: 'Class', 8: 'String', 16: 'MethodType', 19: 'Module', 20: 'Package'}[tag], reader.u2())
            elif tag in (9, 10, 11, 12, 17, 18):
                name = {9: 'Field', 10: 'Method', 11: 'InterfaceMethod', 12: 'NameAndType', 17: 'Dynamic', 18: 'InvokeDynamic'}[tag]
                self.entries[i] = (name, reader.u2(), reader.u2())
            elif tag == 15:
                self.entries[i] = ('MethodHandle', reader.u1(), reader.u2())
            else:
                raise ClassFormatError('unknown constant-pool tag %d at %d' % (tag, i))
            i += 1
        self.bootstrap = []
        self.drop_owners = ()
        self.receiver_hooks = frozenset()
        self.dropped = []

    def utf8(self, index):
        entry = self.entries[index]
        if not entry or entry[0] != 'Utf8':
            raise ClassFormatError('constant %d is not Utf8' % index)
        return entry[1]

    def resolve(self, index):
        if index == 0:
            return None
        if index >= len(self.entries) or self.entries[index] is None:
            raise ClassFormatError('bad constant-pool index %d' % index)
        entry = self.entries[index]
        kind = entry[0]
        if kind == 'Utf8':
            return 'utf8:' + entry[1]
        if kind in ('Integer', 'Float', 'Long', 'Double'):
            return kind.lower() + ':' + entry[1]
        if kind in ('Class', 'Module', 'Package'):
            return kind.lower() + ':' + self.utf8(entry[1])
        if kind == 'String':
            return 'string:' + json.dumps(self.utf8(entry[1]), ensure_ascii=False)
        if kind == 'MethodType':
            return 'methodtype:' + self.utf8(entry[1])
        if kind == 'NameAndType':
            return self.utf8(entry[1]) + ':' + self.utf8(entry[2])
        if kind in ('Field', 'Method', 'InterfaceMethod'):
            return kind.lower() + ':' + self.resolve(entry[1]).removeprefix('class:') + '.' + self.resolve(entry[2])
        if kind == 'MethodHandle':
            return 'handle%d:%s' % (entry[1], self.resolve(entry[2]))
        if kind in ('Dynamic', 'InvokeDynamic'):
            return '%s:%s@%s' % (kind.lower(), self.resolve(entry[2]), self.bootstrap_method(entry[1]))
        raise ClassFormatError('unsupported constant ' + kind)

    def bootstrap_method(self, index):
        if index >= len(self.bootstrap):
            raise ClassFormatError('bootstrap method %d missing' % index)
        handle, args = self.bootstrap[index]
        return '[' + self.resolve(handle) + ' ' + ', '.join(self.resolve(a) for a in args) + ']'


def _code(pool, data):
    r = _Reader(data)
    max_stack, max_locals, length = r.u2(), r.u2(), r.u4()
    code = r.take(length)
    tail = r
    c = _Reader(code)
    raw = []  # (offset, name, operands with ('@', absolute offset) for branch targets)
    while c.pos < len(code):
        offset = c.pos
        op = c.u1()
        if op >= len(_NAMES):
            raise ClassFormatError('unknown opcode 0x%02x' % op)
        name = _NAMES[op]
        if op == 0xc4:  # wide
            inner = c.u1()
            operands = [_NAMES[inner], c.u2()] + ([c.s2()] if inner == 0x84 else [])
        elif op in _CP1:
            operands = [pool.resolve(c.u1())]
        elif op in _CP2:
            operands = [pool.resolve(c.u2())]
        elif op == 0xb9:
            operands = [pool.resolve(c.u2()), c.u1()]
            c.u1()
        elif op == 0xba:
            operands = [pool.resolve(c.u2())]
            c.u2()
        elif op == 0xc5:
            operands = [pool.resolve(c.u2()), c.u1()]
        elif op in _LOCAL1:
            operands = [c.u1()]
        elif op == 0x84:
            operands = [c.u1(), struct.unpack('>b', c.take(1))[0]]
        elif op == 0x10:
            operands = [struct.unpack('>b', c.take(1))[0]]
        elif op == 0x11:
            operands = [c.s2()]
        elif op == 0xbc:
            operands = [c.u1()]
        elif op in _BRANCH2:
            operands = [('@', offset + c.s2())]
        elif op in _BRANCH4:
            operands = [('@', offset + c.s4())]
        elif op in (0xaa, 0xab):
            c.take((4 - c.pos % 4) % 4)
            default = offset + c.s4()
            if op == 0xaa:
                low, high = c.s4(), c.s4()
                if high < low:
                    raise ClassFormatError('bad tableswitch')
                pairs = [(low + k, offset + c.s4()) for k in range(high - low + 1)]
            else:
                pairs = [(c.s4(), offset + c.s4()) for _ in range(c.u4())]
            operands = [('@', default)] + [x for key, target in pairs for x in (key, ('@', target))]
        else:
            operands = []
        raw.append((offset, _FOLD.get(name, name), operands))
    # Probe hooks (invokestatic into an explicitly named probe owner) are removed
    # before labels are assigned; a jump to a hook lands on the next instruction.
    # A hook listed in ``receiver_hooks`` was inserted together with the
    # ``aload_0`` right before it (#435: ObserveNativeMaterialPreflight passes
    # ``this`` to afterCopy); that load is part of the hook and goes with it.
    # Such a hook without ``aload_0`` directly before it is rejected.
    index, kept = {}, []
    for offset, name, operands in raw:
        index[offset] = len(kept)
        if name == 'invokestatic' and any(str(operands[0]).startswith('method:' + owner + '.') for owner in pool.drop_owners):
            if operands[0] in pool.receiver_hooks:
                if not kept or kept[-1][1:] != ('aload_0', []):
                    raise ClassFormatError('receiver hook without aload_0: ' + str(operands[0]))
                kept.pop()
                index[offset] = len(kept)
                pool.dropped.append('aload_0')
            pool.dropped.append(operands[0])
            continue
        kept.append((offset, name, operands))
    raw = kept
    index[len(code)] = len(raw)

    def label(target):
        if target not in index:
            raise ClassFormatError('branch into the middle of an instruction')
        return 'L%d' % index[target]

    instructions = []
    for _, name, operands in raw:
        parts = [label(o[1]) if isinstance(o, tuple) else str(o) for o in operands]
        instructions.append(' '.join([name, *parts]))
    handlers = []
    for _ in range(tail.u2()):
        start, end, handler, catch = tail.u2(), tail.u2(), tail.u2(), tail.u2()
        handlers.append('%s-%s -> %s %s' % (label(start), label(end), label(handler), pool.resolve(catch) or 'any'))
    attributes = _attributes(pool, tail)
    return dict(max_stack=max_stack, max_locals=max_locals, instructions=instructions, handlers=handlers,
                attributes=attributes)


def _attribute(pool, name, data):
    r = _Reader(data)
    if name == 'Code':
        return _code(pool, data)
    if name in ('ConstantValue', 'Signature', 'NestHost', 'SourceFile', 'ModuleMainClass'):
        return pool.resolve(r.u2())
    if name in ('Exceptions', 'NestMembers', 'PermittedSubclasses', 'ModulePackages'):
        # Membership tables: javac emits them in hash/discovery order, the JVM treats them as sets.
        return sorted(pool.resolve(r.u2()) for _ in range(r.u2()))
    if name == 'InnerClasses':
        # Entries (class, outer, simple name, flags) are kept whole; only the table order is ignored.
        rows = [[pool.resolve(r.u2()), pool.resolve(r.u2()), pool.resolve(r.u2()), r.u2()] for _ in range(r.u2())]
        return sorted(rows, key=canonical)
    if name == 'EnclosingMethod':
        return [pool.resolve(r.u2()), pool.resolve(r.u2())]
    if name in ('Synthetic', 'Deprecated'):
        return True
    if name == 'MethodParameters':
        return [[pool.resolve(r.u2()), r.u2()] for _ in range(r.u1())]
    if name == 'BootstrapMethods':
        # Order-independent: invokedynamic operands already resolve their bootstrap inline.
        return sorted(pool.bootstrap_method(i) for i in range(len(pool.bootstrap)))
    return 'raw:' + data.hex()


def _attributes(pool, r):
    result = {}
    for _ in range(r.u2()):
        name = pool.utf8(r.u2())
        data = r.take(r.u4())
        if name in DROPPED_ATTRIBUTES:
            continue
        result.setdefault(name, []).append(_attribute(pool, name, data))
    return result


def _members(pool, r):
    members = {}
    for _ in range(r.u2()):
        flags, name, descriptor = r.u2(), pool.utf8(r.u2()), pool.utf8(r.u2())
        members[name + descriptor] = dict(access=flags, attributes=_attributes(pool, r))
    return members


def _load_bootstrap(pool, data, start):
    """BootstrapMethods is needed to resolve invokedynamic in Code, so read it first."""
    r = _Reader(data, start)
    for _ in range(r.u2()):
        name = pool.utf8(r.u2())
        payload = r.take(r.u4())
        if name == 'BootstrapMethods':
            b = _Reader(payload)
            pool.bootstrap = [(b.u2(), [b.u2() for _ in range(b.u2())]) for _ in range(b.u2())]


def _skip_members(r):
    for _ in range(r.u2()):
        r.take(6)
        for _ in range(r.u2()):
            r.take(2)
            r.take(r.u4())


def normalize(data, drop_owners=(), dropped=None, receiver_hooks=()):
    """Return the normalized, constant-pool independent form of one class file.

    ``drop_owners`` lists internal class names whose static calls are probe
    hooks to ignore (used only for overlay checks); removed calls are appended
    to ``dropped``.  ``receiver_hooks`` lists resolved hook methods
    (``method:owner.name:desc``) that were inserted with an ``aload_0``.
    """
    r = _Reader(bytes(data))
    if r.u4() != 0xCAFEBABE:
        raise ClassFormatError('not a class file')
    minor, major = r.u2(), r.u2()
    pool = _Pool(r)
    pool.drop_owners = tuple(drop_owners)
    pool.receiver_hooks = frozenset(receiver_hooks)
    if dropped is not None:
        pool.dropped = dropped
    header = r.pos
    r.take(6)
    r.take(2 * r.u2())
    _skip_members(r)
    _skip_members(r)
    _load_bootstrap(pool, r.data, r.pos)
    r.pos = header
    access, this, super_ = r.u2(), pool.resolve(r.u2()), pool.resolve(r.u2())
    interfaces = [pool.resolve(r.u2()) for _ in range(r.u2())]
    fields = _members(pool, r)
    methods = _members(pool, r)
    attributes = _attributes(pool, r)
    if r.pos != len(r.data):
        raise ClassFormatError('trailing bytes after class file')
    return dict(version='%d.%d' % (major, minor), access=access, this=this, super=super_, interfaces=interfaces,
                attributes=attributes, fields=fields, methods=methods)


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def normalized_sha256(form):
    return hashlib.sha256(canonical(form).encode('utf-8')).hexdigest()


def differences(left, right):
    """Human-readable list of what differs between two normalized forms."""
    result = []
    for key in ('version', 'access', 'this', 'super', 'interfaces'):
        if left[key] != right[key]:
            result.append('class ' + key)
    for key in sorted(set(left['attributes']) | set(right['attributes'])):
        if left['attributes'].get(key) != right['attributes'].get(key):
            result.append('class attribute ' + key)
    for kind in ('fields', 'methods'):
        for name in sorted(set(left[kind]) | set(right[kind])):
            if name not in left[kind]:
                result.append('%s %s only in compared class' % (kind[:-1], name))
            elif name not in right[kind]:
                result.append('%s %s only in reference class' % (kind[:-1], name))
            elif left[kind][name] != right[kind][name]:
                result.append('%s %s differs' % (kind[:-1], name))
    return result


ACC_FINAL = 0x0010
INNER_CLASS_FLAG_RULE = ('InnerClasses rows describing another class (inner != this) may differ only in ACC_FINAL, '
                         'with the same inner/outer/simple-name set, and the compared flags must equal the flags '
                         'that inner class declares for itself in the runtime under test')


def _single_table(form, attribute):
    tables = form['attributes'].get(attribute, [])
    if len(tables) > 1:
        raise ClassFormatError('repeated ' + attribute + ' attribute')
    return tables[0] if tables else []


def declared_inner_flags(runtime, inner):
    """Flags ``inner`` (``class:pkg/Outer$Inner``) declares in its own InnerClasses row, or None."""
    name = inner.removeprefix('class:') + '.class'
    try:
        rows = _single_table(normalize(runtime.read(name)), 'InnerClasses')
    except (KeyError, ClassFormatError):
        return None
    flags = [row[3] for row in rows if row[0] == inner]
    return flags[0] if len(flags) == 1 else None


def inner_class_flag_evidence(left, right, runtime):
    """Prove an InnerClasses-only difference is the javac/ASM representation of another class's flags.

    Returns ``(evidence, None)`` when every differing row is accepted, else
    ``(None, reason)``.  Accepted only if: both tables name the same set of
    (inner, outer, simple name) rows; each differing row describes a class
    other than this one; the flags differ in ACC_FINAL only (visibility,
    static, interface, abstract, synthetic, annotation and enum bits are
    identical); and the runtime under test contains that inner class declaring
    exactly the compared flags.  The class's own row is never tolerated.
    """
    try:
        rows = _single_table(left, 'InnerClasses'), _single_table(right, 'InnerClasses')
    except ClassFormatError as error:
        return None, str(error)
    keyed = []
    for table in rows:
        index = {canonical(row[:3]): row for row in table}
        if len(index) != len(table):
            return None, 'duplicate InnerClasses rows'
        keyed.append(index)
    reference, compared = keyed
    if set(reference) != set(compared):
        return None, 'inner class set differs'
    evidence = []
    for key in sorted(reference):
        inner, outer, simple, was = reference[key]
        now = compared[key][3]
        if was == now:
            continue
        if inner == left['this']:
            return None, 'own InnerClasses flags differ for ' + inner
        if (was ^ now) & ~ACC_FINAL:
            return None, 'flags of %s differ beyond ACC_FINAL (0x%04x vs 0x%04x)' % (inner, was, now)
        if runtime is None:
            return None, 'no runtime available to verify flags of ' + inner
        declared = declared_inner_flags(runtime, inner)
        if declared != now:
            return None, '%s declares %s in the runtime, compared row says 0x%04x' % (
                inner, 'nothing' if declared is None else '0x%04x' % declared, now)
        evidence.append(dict(inner_class=inner, outer_class=outer, simple_name=simple, reference_flags=was,
                             compared_flags=now, runtime_declared_flags=declared))
    if not evidence:
        return None, 'no differing InnerClasses row'
    return evidence, None


def compare(name, reference, compared, mode, runtime=None):
    """Compare one class; returns a receipt entry with ``identical`` set.

    ``reference`` is the historical class (base JAR / qualified fixture),
    ``compared`` the class packaged in the runtime under test.  ``runtime``
    (an open ZipFile of the runtime under test) is only consulted when the
    single remaining difference is ``InnerClasses``; see
    ``inner_class_flag_evidence``.
    """
    if mode not in MODES:
        raise ValueError('unknown class identity mode: %r' % (mode,))
    entry = dict(name=name, mode=mode, reference_sha256=hashlib.sha256(reference).hexdigest(),
                 compared_sha256=hashlib.sha256(compared).hexdigest())
    if entry['reference_sha256'] == entry['compared_sha256']:
        entry.update(identical=True, result='byte-identical')
        return entry
    if mode == EXACT:
        entry.update(identical=False, result='bytes-differ')
        return entry
    try:
        left, right = normalize(reference), normalize(compared)
    except ClassFormatError as error:
        entry.update(identical=False, result='unparseable', error=str(error))
        return entry
    entry.update(reference_normalized_sha256=normalized_sha256(left), compared_normalized_sha256=normalized_sha256(right))
    diff = differences(left, right)
    if diff == ['class attribute InnerClasses']:
        evidence, reason = inner_class_flag_evidence(left, right, runtime)
        if evidence:
            entry.update(identical=True, result='normalized-identical-inner-class-flags', rule=INNER_CLASS_FLAG_RULE,
                         inner_class_flag_evidence=evidence, dropped_attributes=sorted(DROPPED_ATTRIBUTES))
            return entry
        entry['inner_class_flag_check'] = reason
    if diff:
        entry.update(identical=False, result='normalized-differ', differences=diff)
    else:
        entry.update(identical=True, result='normalized-identical', dropped_attributes=sorted(DROPPED_ATTRIBUTES))
    return entry


def _without_max_stack(form):
    form = json.loads(canonical(form))
    for member in form['methods'].values():
        for code in member['attributes'].get('Code', []):
            code.pop('max_stack', None)
    return form


def compare_overlay(name, overlay, runtime, probe_owners, receiver_hooks=()):
    """Overlay built from the runtime under test: only probe hooks may differ.

    The overlay class must normalize to the runtime's own class once static
    calls into ``probe_owners`` (and the ``aload_0`` of ``receiver_hooks``) are
    removed.  ``max_stack`` is excluded because the overlay writer (ASM
    COMPUTE_MAXS) recomputes it; ``max_locals``, every instruction, operand,
    branch target and handler still has to match.
    """
    entry = dict(name=name, mode='maven-overlay-from-runtime', overlay_sha256=hashlib.sha256(overlay).hexdigest(),
                 runtime_sha256=hashlib.sha256(runtime).hexdigest(), probe_owners=sorted(probe_owners))
    if receiver_hooks:
        entry['receiver_hooks'] = sorted(receiver_hooks)
    dropped = []
    try:
        left = _without_max_stack(normalize(overlay, probe_owners, dropped, receiver_hooks))
        right = _without_max_stack(normalize(runtime))
    except ClassFormatError as error:
        entry.update(identical=False, result='unparseable', error=str(error))
        return entry
    entry['removed_probe_calls'] = dropped
    diff = differences(right, left)
    if diff:
        entry.update(identical=False, result='overlay-differs-beyond-probe-hooks', differences=diff)
    elif not dropped:
        entry.update(identical=False, result='overlay-has-no-probe-hook')
    else:
        entry.update(identical=True, result='runtime-class-plus-probe-hooks-only')
    return entry