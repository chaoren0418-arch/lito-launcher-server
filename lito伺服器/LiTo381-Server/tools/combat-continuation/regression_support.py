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
import subprocess
import sys
import zipfile

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
        parser.add_argument('--continue-per-case', action='store_true',
                            help='Run every case (driver/probe/test) of this entry even after a failure and record '
                                 'each result and first failure; assertions are unchanged and the receipt still fails '
                                 'unless every case passed (#435). Default: stop at the first failure.')
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
        if self.args.continue_per_case:
            result.append('--continue-per-case')
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
                       'the constant-pool independent, debug-free normalized class and fail on any remaining difference, '
                       'except: ' + class_identity.INNER_CLASS_FLAG_RULE + '.')


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
               'assert_class_identity, class_identity_changed, deferred_class_guard, runtime_overlay_jobs, '
               'runtime_overlay, assert_runtime_overlays, assert_normalized_class, regenerate_frozen_overlay\n')
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
    gating = [e['identical'] for e in value['entries'] if e['gating'] == 'assert']
    value['status'] = 'recorded' if not gating else 'passed' if all(gating) else 'failed'
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
    historical = class_identity.compare(name, base.read(name), runtime.read(name), mode, runtime=runtime)
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


def class_identity_changed(current, historical, expected_sha256, name, mode, receipt, runtime=None):
    """Legacy ``changed=sha256(current)!=row['sha256']`` with an explicit Maven mode.

    Maven: ``changed`` means the normalized class differs from the qualified
    historical class; the caller's original allowlist assertion still applies.
    ``runtime`` (the open runtime ZipFile) lets an InnerClasses flag-only
    difference be checked against the inner class the runtime really ships.
    """
    assert mode in class_identity.MODES, mode
    if mode == class_identity.EXACT:
        return hashlib.sha256(current).hexdigest() != expected_sha256
    assert hashlib.sha256(historical).hexdigest() == expected_sha256, name + ' qualified class is not the historical fixture'
    entry = class_identity.compare(name, historical, current, mode, runtime=runtime)
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
                        + repr(mode) + ',Path(' + repr(str(receipt)) + '),runtime=native)')


# --- #435 / T149: the remaining historical class guards (existing 13/15/16, affected 04/05/07/12/16) ---
#
# Static analysis (docs/reports/issue-435.md) shows that every guarded
# GameStateStore of these drivers differs from the Maven class by real methods
# (rankingPeriodKeyAt/validRankingPeriodKey), so a normalized comparison with
# the historical class can never pass.  The guards exist because an observer
# overlay produced from the historical class would replace the runtime's own
# class.  Maven mode therefore removes the swap instead of the guard: each
# runtime gets an overlay produced from its *own* class by the same observer,
# and each overlay must be exactly that class plus the same probe hooks.
# Exact mode never rewrites the drivers (candidates keep the original bytes check).

GAME_STATE_STORE = 'local/lito381/server/GameStateStore.class'
RUNTIME_INVENTORY = 'local/lito381/server/RuntimeInventory.class'
_PREFLIGHT_PROBE = 'local/lito381/server/NativeCastMaterialPreflightProbe'
_INVENTORY = 'Llocal/lito381/server/RuntimeInventory;'
# Observer tools (untracked, under --resources) -> probe owner and the classes they write.
OBSERVERS = {
    'ObserveDeItemTransaction': dict(owner='local/lito381/server/DeItemTransactionProbe', classes=(GAME_STATE_STORE,)),
    'ObservePlayerActionAuthority': dict(owner='local/lito381/server/PlayerActionAuthorityProbe',
                                         classes=(GAME_STATE_STORE,)),
    # afterCopy is inserted as ``aload_0; invokestatic`` before RuntimeInventory.copy()'s areturn.
    'ObserveNativeMaterialPreflight': dict(
        owner=_PREFLIGHT_PROBE, classes=(GAME_STATE_STORE, RUNTIME_INVENTORY),
        receiver_hooks=('method:' + _PREFLIGHT_PROBE + '.afterCopy:(' + _INVENTORY + _INVENTORY + ')' + _INVENTORY,)),
}
# The frozen transaction overlay (existing 16) was written by this observer source (stage receipt input).
OBSERVE_DE_ITEM_TRANSACTION_SHA256 = 'f50b3f6ef13792d78a8fe6ab1660487ecb8394f44077b12d90346d9fb3364578'

RUNTIME_GUARD = re.compile(r"with zipfile\.ZipFile\(old\)as a,zipfile\.ZipFile\(jar\)as b:"
                           r"assert a\.read\('([^']+\.class)'\)==b\.read\('\1'\)")
OBSERVER_CALL = re.compile(r"invoke\(\[J/'java\.exe','-Xverify:all','-cp',tcp,'(Observe\w+)',old,overlay\]\)")
BRANCH_OVERLAY = "runtimecp=str(classes)+';'+str(overlay)+';'"
LIST_GUARD = re.compile(r"for name in (\[[^\]]+\]):assert old\.read\(name\)==new\.read\(name\)")
FROZEN_GUARD = re.compile(r"with zipfile\.ZipFile\(Path\(parent\['runtime'\]\['path'\]\)\)as prior:"
                          r"assert prior\.read\('([^']+\.class)'\)==z\.read\('\1'\)")
# Drivers that must be adapted in Maven mode (a silent no-op would hide a changed fixture).
MAVEN_GUARD_DRIVERS = frozenset({
    'probe-player-action-authority.py', 'replay-player-action-main-owned.py', 'replay-player-action-transaction.py',
    'timed-weapon-probe-native-cast-material-preflight.py', 'timed-weapon-probe-native-cast-material-authority.py',
    'timed-weapon-probe-native-cast-material-fault.py', 'timed-weapon-probe-native-cube-material-fault.py',
    'timed-probe-native-item-weapon-fault.py'})
RUNTIME_OVERLAY_RULE = ('maven-normalized observer overlays: every runtime (parent/before and the JAR under test) runs '
                        'with an overlay the same observer wrote from that runtime\'s own class; each overlay must '
                        'contain exactly the observer\'s classes, each equal to the runtime class plus probe hooks '
                        'only, and both overlays must carry the same probe hooks. The historical-base comparison '
                        'is recorded, not gating.')


def adapt_class_guards(name, text, mode, receipt):
    """Route the #433 class guards of one untracked driver copy through Maven-compatible checks.

    Exact mode returns ``text`` unchanged.  Maven mode rewrites only the
    guard, the observer invocation and the per-branch overlay choice; every
    probe, assertion and deadline stays verbatim.
    """
    assert mode in class_identity.MODES, mode
    if mode == class_identity.EXACT:
        return text
    receipt = 'Path(' + repr(str(receipt)) + ')'
    runtime, listed, frozen = (len(p.findall(text)) for p in (RUNTIME_GUARD, LIST_GUARD, FROZEN_GUARD))
    assert runtime <= 1 and listed <= 1 and frozen <= 1, 'Ambiguous class guards in ' + name
    if name in MAVEN_GUARD_DRIVERS:
        assert runtime + listed + frozen == 1, 'Expected one known class guard in ' + name
    if runtime:
        observers = OBSERVER_CALL.findall(text)
        assert len(observers) == 1 and observers[0] in OBSERVERS, 'Unknown observer overlay in ' + name
        observer = observers[0]
        assert RUNTIME_GUARD.search(text)[1] in OBSERVERS[observer]['classes'], 'Guarded class is not observed'
        assert text.count(BRANCH_OVERLAY) == 1, 'Expected one per-branch overlay classpath in ' + name
        text = RUNTIME_GUARD.sub(lambda m: 'deferred_class_guard(' + repr(m[1]) + ')', text)
        text = OBSERVER_CALL.sub(
            lambda m: ("[invoke([J/'java.exe','-Xverify:all','-cp',tcp," + repr(m[1]) + ",source,target]) "
                       "for source,target in runtime_overlay_jobs(old,jar,overlay)];"
                       "assert_runtime_overlays(old,jar,overlay," + repr(m[1]) + ',' + receipt + ')'), text)
        text = text.replace(BRANCH_OVERLAY, "runtimecp=str(classes)+';'+str(runtime_overlay(runtime,old,jar,overlay))+';'")
    if listed:
        text = LIST_GUARD.sub(lambda m: 'for name in ' + m[1] + ':assert_normalized_class(old,new,name,' + receipt + ')',
                              text)
    if frozen:
        text = FROZEN_GUARD.sub(
            lambda m: ("with zipfile.ZipFile(Path(parent['runtime']['path']))as prior:regenerate_frozen_overlay("
                       "prior,z,overlay,jar,'ObserveDeItemTransaction',B/'ObserveDeItemTransaction.java',"
                       "[x['path']for x in read(Path(build['manifest']['path']))['asm']],run/'observer-tools',"
                       + receipt + ',' + repr(OBSERVE_DE_ITEM_TRANSACTION_SHA256) + ',' + repr(m[1]) + ')'), text)
    return text


def deferred_class_guard(name):
    """Placeholder left where the exact guard stood; the Maven check runs once the overlays exist."""
    return name


def parent_overlay(overlay):
    overlay = Path(overlay)
    return overlay.with_name(overlay.name + '-parent')


def runtime_overlay_jobs(old, jar, overlay):
    """(source runtime, overlay directory) pairs for the observer: the JAR under test and the parent."""
    return [(jar, overlay), (old, parent_overlay(overlay))]


def runtime_overlay(runtime, old, jar, overlay):
    """The overlay written from ``runtime`` itself."""
    runtime = Path(runtime).resolve()
    if runtime == Path(jar).resolve():
        return overlay
    if runtime == Path(old).resolve():
        return parent_overlay(overlay)
    raise AssertionError('No observer overlay was written from ' + str(runtime))


def observer_overlay_check(observer, overlay, runtime):
    """``overlay`` must hold exactly the observer's classes, each = ``runtime`` class + probe hooks."""
    spec = OBSERVERS[observer]
    files = _overlay_files(overlay) if Path(overlay).is_dir() else []
    entries = [class_identity.compare_overlay(n, (Path(overlay) / n).read_bytes() if n in files else b'',
                                              runtime.read(n), (spec['owner'],), spec.get('receiver_hooks', ()))
               for n in spec['classes']]
    return dict(overlay=str(overlay), files=files, classes=entries,
                identical=files == sorted(spec['classes']) and all(e['identical'] for e in entries),
                hooks={e['name']: sorted(e.get('removed_probe_calls', [])) for e in entries})


def _gate(receipt, entry, message):
    entry['gating'] = 'assert'
    _record_identity(receipt, class_identity.MAVEN, entry)
    assert entry['identical'], message + '; receipt ' + str(receipt)


def _overlay_failure(check):
    return '; '.join(e['name'] + ': ' + e['result'] + ''.join(' / ' + d for d in e.get('differences', []))
                     for e in check['classes'] if not e['identical']) or 'files ' + str(check['files'])


def assert_runtime_overlays(old, jar, overlay, observer, receipt):
    with zipfile.ZipFile(old) as a, zipfile.ZipFile(jar) as b:
        runtime = observer_overlay_check(observer, overlay, b)
        parent = observer_overlay_check(observer, parent_overlay(overlay), a)
        historical = [class_identity.compare(n, a.read(n), b.read(n), class_identity.MAVEN, runtime=b)
                      for n in OBSERVERS[observer]['classes']]
    same = runtime['hooks'] == parent['hooks']
    entry = dict(name=observer, mode='maven-runtime-overlays', rule=RUNTIME_OVERLAY_RULE, runtime_jar=pin(jar),
                 parent_jar=pin(old), runtime_overlay=runtime, parent_overlay=parent, same_probe_hooks=same,
                 historical_base_comparison=historical,
                 identical=runtime['identical'] and parent['identical'] and same)
    entry['result'] = ('runtime-overlays-hooks-only' if entry['identical'] else
                       'runtime-overlay: ' + _overlay_failure(runtime) if not runtime['identical'] else
                       'parent-overlay: ' + _overlay_failure(parent) if not parent['identical'] else
                       'probe hooks differ between overlays')
    _gate(receipt, entry, observer + ' overlay class identity (maven-normalized) failed: ' + entry['result'])


def assert_normalized_class(base, runtime, name, receipt):
    """Legacy ``assert old.read(name)==new.read(name)`` in Maven mode (normalized, no overlay involved)."""
    entry = class_identity.compare(name, base.read(name), runtime.read(name), class_identity.MAVEN, runtime=runtime)
    _gate(receipt, entry, name + ' class identity (maven-normalized) failed: ' + entry['result']
          + ''.join('; ' + d for d in entry.get('differences', [])))


def write_observer_overlay(observer, source, asm, runtime_jar, target, work):
    """Compile the observer tool and let it write ``target`` from ``runtime_jar`` (as the drivers do)."""
    work = Path(work)
    work.mkdir(parents=True, exist_ok=True)
    tcp = os.pathsep.join(map(str, [work, *asm]))
    with (work / (observer + '.log')).open('a', encoding='utf-8') as out:
        for command in ([java_tool('javac'), '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '', '-cp', tcp,
                         '-d', work, source],
                        [java_tool('java'), '-Xverify:all', '-cp', tcp, observer, runtime_jar, target]):
            subprocess.run(list(map(str, command)), check=True, stdout=out, stderr=subprocess.STDOUT, timeout=180)


def regenerate_frozen_overlay(prior, runtime, overlay, runtime_jar, observer, source, asm, work, receipt,
                              source_sha256=None, name=GAME_STATE_STORE):
    """Maven replacement for ``assert prior.read(n)==z.read(n)`` guarding a *frozen* observer overlay.

    The frozen overlay must be the qualified parent class plus probe hooks; it
    is kept beside (``<overlay>-historical``) and ``overlay`` is rewritten by
    the same observer from the runtime under test, which must again be that
    runtime's class plus the same probe hooks.
    """
    assert name in OBSERVERS[observer]['classes'], name
    overlay = Path(overlay)
    frozen = observer_overlay_check(observer, overlay, prior)
    entry = dict(name=observer, mode='maven-frozen-overlay-regenerated', rule=RUNTIME_OVERLAY_RULE,
                 runtime_jar=pin(runtime_jar), observer_source=pin(source), frozen_overlay=frozen,
                 historical_base_comparison=[class_identity.compare(n, prior.read(n), runtime.read(n),
                                                                    class_identity.MAVEN, runtime=runtime)
                                             for n in OBSERVERS[observer]['classes']])
    if source_sha256 and entry['observer_source']['sha256'] != source_sha256:
        entry.update(identical=False, result='observer source is not the one that wrote the frozen overlay')
    elif not frozen['identical']:
        entry.update(identical=False, result='frozen overlay is not the parent class plus hooks: ' + _overlay_failure(frozen))
    else:
        historical = overlay.with_name(overlay.name + '-historical')
        assert not historical.exists(), 'Frozen overlay already moved: ' + str(historical)
        overlay.rename(historical)
        entry['frozen_overlay_kept_at'] = str(historical)
        write_observer_overlay(observer, source, asm, runtime_jar, overlay, work)
        regenerated = observer_overlay_check(observer, overlay, runtime)
        same = regenerated['hooks'] == frozen['hooks']
        entry.update(regenerated_overlay=regenerated, same_probe_hooks=same, identical=regenerated['identical'] and same,
                     result='regenerated-overlay-hooks-only' if regenerated['identical'] and same else
                     'regenerated overlay: ' + _overlay_failure(regenerated) if not regenerated['identical'] else
                     'probe hooks differ from the frozen overlay')
    _gate(receipt, entry, observer + ' frozen overlay class identity (maven-normalized) failed: ' + entry['result'])


# --- #435: --continue-per-case ---

class CaseFailures(SystemExit):
    """Every case ran; some failed.  Exit status 2 (an aborted entry exits 1 with a traceback)."""

    def __init__(self, summary):
        super().__init__(2)
        self.summary = summary

    def __str__(self):
        return 'case failures (entry completed): ' + json.dumps(self.summary, sort_keys=True)


class PrerequisiteFailed(AssertionError):
    """Every case ran, but an entry-level prerequisite recorded in the receipt did not hold."""


_FAILURE_LINE = re.compile(r'(Error|Exception|failure=|FAILED)')


def log_failure(log, since=None):
    """First-failure text from a case log: its last error-looking line (else its last line)."""
    log = Path(log)
    if not log.is_file() or (since is not None and log.stat().st_mtime < since):
        return None
    lines = [line.strip() for line in log.read_text('utf-8', errors='replace').splitlines() if line.strip()]
    matches = [line for line in lines if _FAILURE_LINE.search(line)]
    return (matches or lines or [None])[-1]


def result_failure(result, since=None):
    """``error`` of a driver result.json written by this attempt."""
    result = Path(result)
    if not result.is_file() or (since is not None and result.stat().st_mtime < since):
        return None
    try:
        value = read(result)
    except ValueError:
        return None
    return value.get('error') if not str(value.get('status', '')).startswith('passed') else None


class CaseRunner:
    """Runs the cases of one entry.

    Disabled (default): ``run`` simply calls the case, so the first failure
    aborts the entry exactly as before.  Enabled (``--continue-per-case``):
    each case runs on its own; an ``Exception`` from it is recorded as that
    case's failure and the next case still runs.  KeyboardInterrupt/SystemExit
    and anything outside a case abort the entry.  Assertions are not touched.
    """

    def __init__(self, enabled, planned):
        self.enabled = bool(enabled)
        self.planned = list(planned)
        self.cases = []

    def status(self, case):
        return next((row['status'] for row in self.cases if row['case'] == case), None)

    def run(self, case, body, evidence=None):
        if not self.enabled:
            return body()
        started = datetime.now(timezone.utc)
        try:
            value = body()
        except Exception as error:
            message = None
            if evidence:
                try:
                    message = evidence(started.timestamp() - 1)
                except Exception as problem:  # evidence is best-effort; never hides the failure itself
                    message = 'evidence unreadable: ' + repr(problem)
            row = dict(case=case, status='failed', error_type=type(error).__name__,
                       first_failure=(message or (type(error).__name__ + ': ' + str(error)))[:4000],
                       started_utc=started.isoformat(), finished_utc=datetime.now(timezone.utc).isoformat())
            self.cases.append(row)
            print('CASE FAILED ' + case + ': ' + row['first_failure'], flush=True)
            return None
        self.cases.append(dict(case=case, status='passed', started_utc=started.isoformat(),
                               finished_utc=datetime.now(timezone.utc).isoformat()))
        return value

    def block(self, case, reason, hint):
        """A case whose prerequisite did not hold in this invocation; recorded, never counted as passed."""
        assert self.enabled
        self.cases.append(dict(case=case, status='blocked-prerequisite', first_failure=reason, hint=hint))
        print('CASE BLOCKED ' + case + ': ' + reason + ' -- ' + hint, flush=True)

    def summary(self):
        done = [row['case'] for row in self.cases]
        counts = dict(planned=len(self.planned), passed=0, failed=0, **{'blocked-prerequisite': 0})
        for row in self.cases:
            counts[row['status']] += 1
        counts['not_run'] = len([case for case in self.planned if case not in done])
        return counts

    def finish(self):
        """Inside the entry, after the last case: all passed, or CaseFailures (exit 2)."""
        if not self.enabled:
            return
        summary = self.summary()
        if summary['passed'] != summary['planned']:
            print('CASE SUMMARY ' + json.dumps(summary), flush=True)
            raise CaseFailures(summary)

    def fields(self, error=None):
        """Receipt fields: case results plus whether the entry completed or aborted."""
        if not self.enabled:
            return {}
        if error is None:
            kind = None
        elif isinstance(error, CaseFailures):
            kind = 'case-failures'
        elif isinstance(error, PrerequisiteFailed):
            kind = 'entry-prerequisite-failed'
        else:
            kind = 'entry-aborted'
        value = dict(case_mode='continue-per-case', cases=self.cases, case_summary=self.summary(),
                     entry_outcome='aborted' if kind == 'entry-aborted' else 'completed', failure_kind=kind)
        if kind == 'entry-aborted':
            value['entry_error'] = type(error).__name__ + ': ' + str(error)
        return value


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
