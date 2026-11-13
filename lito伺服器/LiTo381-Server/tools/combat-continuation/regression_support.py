"""Shared path/JDK plumbing for the historical combat regression drivers.

--resources is a read-only Server directory (useful for worktrees).  It never
changes the selected candidate.  A separate --out is required in this mode.
Build layers, behavioral assertions, expected case counts and timeouts are unchanged.
Isolated runs additionally measure the authorized current formal-file baseline.
"""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys

ROOT = Path(__file__).resolve().parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
import class_identity  # noqa: E402  (same directory; adapted drivers run elsewhere)

SERVER = ROOT.parents[1]
CURRENT_FORMAL_SHA256 = 'e1498871c7be6b287821c57ef5d95933db1740d9b4d4a80f6c3465f21d99d498'
HISTORICAL_FORMAL_SHA256 = '75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'


def read(path):
    return json.loads(Path(path).read_text('utf-8'))


def pin(path):
    path = Path(path).resolve()
    return dict(path=str(path), sha256=hashlib.sha256(path.read_bytes()).hexdigest())


def write(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def java_tool(name):
    home = os.environ.get('JAVA_HOME')
    if home:
        executable = Path(home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))
        if not executable.is_file():
            raise FileNotFoundError(f'JAVA_HOME does not contain {name}: {executable}')
        return str(executable)
    executable = shutil.which(name)
    if not executable:
        raise FileNotFoundError(f'{name} is required in JAVA_HOME/bin or PATH')
    return executable


class Options:
    def __init__(self, stage=False, reconcile=False):
        parser = argparse.ArgumentParser(description=__doc__)
        if stage:
            parser.add_argument('stage', nargs='?', default='cancellation-battle-runtime')
        if not stage:
            parser.add_argument('--stage', help=argparse.SUPPRESS)
        parser.add_argument('--jar', type=Path, help='Candidate JAR; defaults to the historical build receipt')
        parser.add_argument('--out', type=Path, help='Common output root; each suite has its own subdirectory')
        parser.add_argument('--resources', type=Path, default=SERVER,
                            help='Read-only Server directory containing historical fixtures/receipts')
        parser.add_argument('--config', type=Path,
                            help='Explicit isolated test configuration (probes require SQL resources)')
        parser.add_argument('--class-identity', choices=class_identity.MODES, default=class_identity.EXACT,
                            help='Historical class guards: exact bytes (default, patched candidates) or the '
                                 'receipted maven-normalized compatibility mode for a recompiled Maven --jar (#418)')
        if reconcile:
            parser.add_argument('--manifest', type=Path, help='Failed replay manifest inside --out')
        self.args = parser.parse_args()
        self.server = self.args.resources.resolve()
        self.root = self.server / 'tools/combat-continuation'
        self.out = self.args.out.resolve() if self.args.out else None
        self.config = self.args.config.resolve() if self.args.config else self.server / 'config/server.properties'
        if self.server != SERVER and not self.out:
            parser.error('--resources outside this worktree requires --out')
        if self.out:
            if self.server != SERVER and self.out.is_relative_to(self.server.parents[1]) and not self.out.is_relative_to(SERVER.parents[1]):
                parser.error('--out must not write into the read-only resource project')
            self.out.mkdir(parents=True, exist_ok=True)

    def build(self, stage):
        value = read(self.root / stage / 'build/build.json')
        receipt = value['runtime']
        if self.args.jar:
            value['runtime'] = pin(self.args.jar)
        else:
            assert pin(value['runtime']['path']) == value['runtime']
        if self.args.class_identity != class_identity.EXACT:
            # Explicit opt-in only; the patched candidate of record keeps byte identity.
            assert self.args.jar, '--class-identity ' + self.args.class_identity + ' requires an explicit --jar'
            assert value['runtime']['sha256'] != receipt.get('sha256'), \
                'The JAR equals the ' + stage + ' build receipt; candidates must use --class-identity exact'
            value['class_identity'] = dict(mode=self.args.class_identity, stage_receipt_runtime=receipt,
                                           rule=CLASS_IDENTITY_RULE)
        return value

    def destination(self, name, default):
        path = self.out / name if self.out else default
        path.mkdir(parents=True, exist_ok=True)
        return path

    def child_args(self):
        result = []
        for name in ('jar', 'out', 'resources', 'config'):
            value = getattr(self.args, name)
            if value is not None:
                result += ['--' + name, str(value.resolve())]
        if self.args.class_identity != class_identity.EXACT:
            result += ['--class-identity', self.args.class_identity]
        return result


# Historical boundary drivers build a GameStateStore observer overlay from the
# base JAR, then require that class to be byte-identical in the runtime JAR.
OVERLAY_SOURCE = "'ObserveDeItemTransaction',old,overlay)"
OVERLAY_ASSERT = re.compile(r"with zipfile\.ZipFile\(old\) as a,zipfile\.ZipFile\(jar\) as z:"
                            r"assert a\.read\('([^']+\.class)'\)==z\.read\('\1'\)")
PROBE_OWNERS = ('local/lito381/server/DeItemTransactionProbe',)
CLASS_IDENTITY_RULE = ('exact: bytes equal to the historical class (unchanged legacy guard). '
                       'maven-normalized: observer overlays are generated from the runtime under test and may '
                       'differ from its own class only by probe hook calls; other historical class guards compare '
                       'the constant-pool independent, debug-free normalized class and fail on any remaining difference.')


def adapt_class_identity(text):
    """Route the legacy GameStateStore overlay guard through assert_class_identity.

    In exact mode the helpers perform the original byte comparison and keep the
    base JAR as overlay source, so candidate behavior is unchanged; the original
    driver files are never modified.
    """
    overlay = OVERLAY_SOURCE in text
    if overlay:
        assert text.count(OVERLAY_SOURCE) == 1, 'Ambiguous observer overlay'
        text = text.replace(OVERLAY_SOURCE, "'ObserveDeItemTransaction',overlay_source(old,jar,build),overlay)")
    count = len(OVERLAY_ASSERT.findall(text))
    assert count <= 1, 'Ambiguous class identity guards'
    if count:
        text = OVERLAY_ASSERT.sub(
            lambda m: ('with zipfile.ZipFile(old) as a,zipfile.ZipFile(jar) as z:assert_class_identity(a,z,'
                       + repr(m[1]) + ",build,run/'class-identity.json'" + (',overlay' if overlay else '') + ')'),
            text)
    return text


def adapt_driver(text, config, runtime_dir=None):
    """Adapt only executable/config/runtime paths in directly invoked drivers."""
    text = adapt_class_identity(text)
    text = re.sub(r"Path\(r?['\"]C:[^'\"]*jdk-17\.0\.17\.10-hotspot[/\\]bin[/\\](java|javac)\.exe['\"]\)", lambda m: 'java_tool(' + repr(m[1]) + ')', text)
    text = re.sub(r"J\s*=\s*Path\(r?['\"]C:[^'\"]*jdk-17\.0\.17\.10-hotspot[^'\"]*['\"]\)", 'J=None', text)
    text = re.sub(r"J\s*/\s*['\"](java|javac)\.exe['\"]", lambda m: 'java_tool(' + repr(m[1]) + ')', text)
    text = re.sub(r"S\s*/\s*['\"]config/server.properties['\"]", lambda m: 'Path(' + repr(str(config)) + ')', text)
    if runtime_dir:
        text = re.sub(r"S\s*/\s*['\"]runtime/([^'\"]+)['\"]", lambda m: 'Path(' + repr(str(runtime_dir / m[1])) + ')', text)
    prelude = ('import sys\nfrom pathlib import Path\nsys.path.insert(0, ' + repr(str(ROOT)) + ')\n'
               'from regression_support import java_tool, portable_command, overlay_source, '
               'assert_class_identity, class_identity_changed\n')
    # Some legacy replays execute command arrays frozen in historical JSON.
    text = text.replace('subprocess.run(updated,', 'subprocess.run(portable_command(updated, ' + repr(str(config)) + '),')
    prelude += '# regression_support sha256=' + pin(Path(__file__))['sha256'] + '\n'
    return prelude + text


def class_identity_mode(build):
    mode = (build.get('class_identity') or {}).get('mode', class_identity.EXACT)
    assert mode in class_identity.MODES, mode
    return mode


def overlay_source(old, jar, build):
    """Exact: overlay from the historical base (legacy). Maven: from the runtime under test."""
    return jar if class_identity_mode(build) == class_identity.MAVEN else old


def _record_identity(receipt, mode, entry):
    receipt = Path(receipt)
    value = read(receipt) if receipt.exists() else dict(class_identity_mode=mode, rule=CLASS_IDENTITY_RULE, entries=[])
    assert value['class_identity_mode'] == mode, 'Class identity receipt belongs to another mode'
    value['entries'].append(entry)
    # Only assert_class_identity entries gate here; component entries are judged by the
    # driver's own allowlist assertion right after class_identity_changed returns.
    value['status'] = 'failed' if any(not e['identical'] for e in value['entries'] if e['gating'] == 'assert') else 'recorded'
    write(receipt, value)


def _overlay_files(overlay):
    overlay = Path(overlay)
    return sorted(p.relative_to(overlay).as_posix() for p in overlay.rglob('*') if p.is_file())


def assert_class_identity(base, runtime, name, build, receipt, overlay=None):
    """Legacy ``assert a.read(name)==z.read(name)`` with an explicit Maven mode.

    ``base``/``runtime`` are open ZipFiles (historical base, JAR under test).
    """
    mode = class_identity_mode(build)
    if mode == class_identity.EXACT:
        assert base.read(name) == runtime.read(name), name + ' differs from the historical base (exact bytes)'
        return
    historical = class_identity.compare(name, base.read(name), runtime.read(name), mode)
    if overlay is None:
        entry = historical
    else:
        # The overlay must replace exactly the expected class, and that class must be
        # the runtime's own class plus probe hooks; anything else is an unexpected swap.
        files = _overlay_files(overlay)
        data = (Path(overlay) / name).read_bytes() if name in files else b''
        entry = class_identity.compare_overlay(name, data, runtime.read(name), PROBE_OWNERS)
        entry['overlay_files'] = files
        if files != [name]:
            entry.update(identical=False, result='overlay-replaces-unexpected-classes')
        # Recorded, not gating: the overlay no longer carries the historical base
        # class, so its differences from the Maven class are evidence for review.
        entry['historical_base_comparison'] = historical
    entry.update(runtime_jar_sha256=build['runtime']['sha256'], gating='assert')
    _record_identity(receipt, mode, entry)
    assert entry['identical'], (name + ' class identity (' + mode + ') failed: ' + entry['result']
                                + ''.join('; ' + d for d in entry.get('differences', [])) + '; receipt ' + str(receipt))


def class_identity_changed(current, historical, expected_sha256, name, mode, receipt):
    """Legacy ``changed=sha256(current)!=row['sha256']`` with an explicit Maven mode.

    Maven: ``changed`` means the normalized class differs from the qualified
    historical class; the caller's original allowlist assertion still applies.
    """
    assert mode in class_identity.MODES, mode
    if mode == class_identity.EXACT:
        return hashlib.sha256(current).hexdigest() != expected_sha256
    assert hashlib.sha256(historical).hexdigest() == expected_sha256, name + ' qualified class is not the historical fixture'
    entry = class_identity.compare(name, historical, current, mode)
    entry.update(gating='caller-allowlist', changed=not entry['identical'])
    _record_identity(receipt, mode, entry)
    return not entry['identical']


COMPONENTS_DRIVER = 'replay-native-de-item-state-components.py'
COMPONENTS_GUARD = "current=hashlib.sha256(native.read(name)).hexdigest();changed=current!=row['sha256']"


def adapt_components_identity(text, mode, receipt):
    """Route the corpse/resurrection/vital/character fixture guard through class_identity_changed.

    The untracked driver under --resources is not modified; only the adapted
    copy changes.  Its allowlist assertion is kept verbatim.
    """
    assert mode in class_identity.MODES, mode
    assert text.count(COMPONENTS_GUARD) == 1, 'Unexpected ' + COMPONENTS_DRIVER + ' class guard'
    return text.replace(COMPONENTS_GUARD, "current=hashlib.sha256(native.read(name)).hexdigest();"
                        "changed=class_identity_changed(native.read(name),qualified.read(name),row['sha256'],name,"
                        + repr(mode) + ',Path(' + repr(str(receipt)) + '))')


def portable_command(command, config):
    command = list(map(str, command))
    name = Path(command[0]).name.lower().removesuffix('.exe')
    if name in ('java', 'javac'):
        command[0] = java_tool(name)
    for i, value in enumerate(command[1:], 1):
        if value.replace('\\', '/').endswith('/config/server.properties'):
            command[i] = config
    return command


def prepare_regression(options, stage, default=None):
    build = options.build(stage)
    if default is None:
        default = ROOT / 'build' / (stage + '-regression')
        if stage in ('area-sleep-slow', 'combat-config-origin', 'call-nature-material', 'cancellation-battle-runtime'):
            default /= build['runtime']['sha256'][:12]
    dest = options.destination('regression', default)
    seed = read(options.root / 'build/integration-regression/preparation.json')
    preparation = dict(status=stage+'-candidate-under-test', runtime=build['runtime'],
                       parent_build=pin(options.root / stage / 'build/build.json'), asm=seed['asm'],
                       formal_deployment_changed=False, full_goal_achieved=False)
    if (dest / 'build-current.json').exists():
        assert read(dest / 'build-current.json')['runtime'] == build['runtime'], 'Output belongs to a different JAR'
    if options.args.config:
        config_ref = pin(options.config)
        config_receipt = dest / 'test-config.json'
        if config_receipt.exists():
            assert read(config_receipt) == config_ref, 'Output belongs to a different test configuration'
        write(config_receipt, config_ref)
    write(dest / 'preparation.json', preparation)
    current = dict(status='candidate-under-test', runtime=build['runtime'], manifest=pin(dest / 'preparation.json'))
    write(dest / 'build-current.json', current)
    return dest


class FormalProtection:
    """Read-only current-baseline guard; historical qualification remains immutable."""

    def __init__(self, server):
        self.formal = (server.parents[1] / 'lito登入器/LiTo381-Launcher/app/server/'
                       'LiTo381-SharedResources-candidate.jar').resolve()
        # The tracked reference belongs to this checkout; --resources supplies
        # untracked historical fixtures and the actual deployment location.
        self.reference = SERVER / 'reference/base-0.6.71.jar'
        self.record = dict(status='running', expected_sha256=CURRENT_FORMAL_SHA256,
                           historical_formal_at_candidate_qualification=HISTORICAL_FORMAL_SHA256)

    def start(self):
        self.record['started_utc'] = datetime.now(timezone.utc).isoformat()
        self.record['before'] = pin(self.formal)
        self.record['reference'] = pin(self.reference)
        assert self.record['reference']['sha256'] == CURRENT_FORMAL_SHA256, 'Current formal reference SHA256 mismatch'
        assert self.record['before']['sha256'] == CURRENT_FORMAL_SHA256, 'Current formal deployment SHA256 mismatch'

    def finish(self):
        self.record['after'] = pin(self.formal)
        self.record['finished_utc'] = datetime.now(timezone.utc).isoformat()
        assert self.record['before'] == self.record['after'], 'Formal deployment changed during regression'
        assert pin(self.reference) == self.record['reference'], 'Formal reference changed during regression'
        assert self.record['after']['sha256'] == CURRENT_FORMAL_SHA256, 'Current formal deployment SHA256 mismatch'
        self.record['status'] = 'passed'

    def is_formal(self, ref):
        return Path(ref['path']).resolve() == self.formal

    def check_historical(self, ref):
        assert self.is_formal(ref)
        assert ref['sha256'] == HISTORICAL_FORMAL_SHA256, 'Unexpected historical formal baseline'

    def check_historical_list(self, refs):
        assert sum(self.is_formal(ref) for ref in refs) == 1, 'Expected exactly one historical formal guard'


@contextmanager
def formal_protection(options, receipt):
    """Place before/after evidence in the receipt even when the body fails."""
    if not options.out:
        yield None
        return
    guard = FormalProtection(options.server)
    receipt['formal_protection'] = guard.record
    started = False
    try:
        guard.start()
        started = True
        yield guard
    finally:
        try:
            if started:
                guard.finish()
            else:
                guard.record['status'] = 'failed'
        except BaseException as error:
            guard.record.update(status='failed', error=str(error))
            raise
        finally:
            if 'before' in guard.record and 'after' in guard.record:
                receipt['formal_deployment_changed'] = guard.record['before'] != guard.record['after']


def verify_formal_protection(receipt, guard):
    """A seal must use evidence measured around each actual suite, not just itself."""
    proof = receipt['formal_protection']
    assert proof['status'] == 'passed'
    assert proof['expected_sha256'] == CURRENT_FORMAL_SHA256
    assert proof['historical_formal_at_candidate_qualification'] == HISTORICAL_FORMAL_SHA256
    assert proof['before'] == proof['after'] == pin(guard.formal)
    assert proof['before']['sha256'] == CURRENT_FORMAL_SHA256
    assert proof['reference'] == pin(guard.reference)
    assert proof['reference']['sha256'] == CURRENT_FORMAL_SHA256
    assert proof['started_utc'] <= proof['finished_utc']
