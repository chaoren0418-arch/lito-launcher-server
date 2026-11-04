"""Shared path/JDK plumbing for the historical combat regression drivers.

--resources is a read-only Server directory (useful for worktrees).  It never
changes the selected candidate.  A separate --out is required in this mode.
No build layer, assertion, expected case count or timeout is changed here.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys

ROOT = Path(__file__).resolve().parent
SERVER = ROOT.parents[1]


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
    def __init__(self, stage=False):
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
        if self.args.jar:
            value['runtime'] = pin(self.args.jar)
        else:
            assert pin(value['runtime']['path']) == value['runtime']
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
        return result


def adapt_driver(text, config, runtime_dir=None):
    """Adapt only executable/config/runtime paths in directly invoked drivers."""
    text = re.sub(r"Path\(r?['\"]C:[^'\"]*jdk-17\.0\.17\.10-hotspot[/\\]bin[/\\](java|javac)\.exe['\"]\)", lambda m: 'java_tool(' + repr(m[1]) + ')', text)
    text = re.sub(r"J\s*=\s*Path\(r?['\"]C:[^'\"]*jdk-17\.0\.17\.10-hotspot[^'\"]*['\"]\)", 'J=None', text)
    text = re.sub(r"J\s*/\s*['\"](java|javac)\.exe['\"]", lambda m: 'java_tool(' + repr(m[1]) + ')', text)
    text = re.sub(r"S\s*/\s*['\"]config/server.properties['\"]", lambda m: 'Path(' + repr(str(config)) + ')', text)
    if runtime_dir:
        text = re.sub(r"S\s*/\s*['\"]runtime/([^'\"]+)['\"]", lambda m: 'Path(' + repr(str(runtime_dir / m[1])) + ')', text)
    prelude = ('import sys\nfrom pathlib import Path\nsys.path.insert(0, ' + repr(str(ROOT)) + ')\n'
               'from regression_support import java_tool, portable_command\n')
    # Some legacy replays execute command arrays frozen in historical JSON.
    text = text.replace('subprocess.run(updated,', 'subprocess.run(portable_command(updated, ' + repr(str(config)) + '),')
    prelude += '# regression_support sha256=' + pin(Path(__file__))['sha256'] + '\n'
    return prelude + text


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
