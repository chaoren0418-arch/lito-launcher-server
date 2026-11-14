"""External-server acceptance helper (T140 / Issue #404).

Connects the original native LiTo.bin flow to a LiTo Server that is ALREADY
running (for example the #284 experiment Server on 127.0.0.1:42083).  It never
starts, stops or owns a JVM and never rewrites the requested port.

The native client part reuses the reviewed formal helper development_startup.py
(same pins, same probe arguments, same shared-resource preflight).  Only the
Server Pair is replaced by a loopback endpoint check.  The formal standard flow
(development_startup.py, 127.0.0.1/127.0.0.2:32000) is not modified.

Pure profile functions sit at module level and import only the standard
library, so tests can import this file on any OS without touching Windows APIs.

T139 / Issue #412: this entry never writes into the client directory
(D:/lineage開發/0-客戶端).  litoddraw.dll and lito381-compat.ini are only read and
compared with what the run needs; any difference refuses the start and reports
it, nothing is copied or rewritten.  The run directory may be moved out of the
original project with --run-root (the launcher passes LITO_LAUNCHER_RUN_ROOT).
"""
from pathlib import Path
import argparse, hashlib, importlib.util, ipaddress, json, os, queue, socket, stat, subprocess, sys, threading, time, traceback

HERE = Path(__file__).resolve().parent
FORMAL_HELPER = HERE / 'development_startup.py'
# Same value as DevelopmentStartup.EntrySha: only the reviewed formal helper is reused.
FORMAL_HELPER_SHA = '56532A2FF06DFCF2C1569681D997E84410C8EA68CB4F65B4ED9BCA0D7CDA8189'
PROFILE_KIND = 'external'
LOCAL_CONFIG_PLACEHOLDER = 'external-server.properties'
# Same pin as development_startup.PINS[app/compat/litoddraw.dll]; checked against the formal table at run time.
CLIENT_DDRAW_SHA = 'E47BD6C6EC9979C8747A523E4457133802D19F673C8DC1E67707ABFA96216A8A'
CLIENT_DDRAW_NAME = 'litoddraw.dll'
CLIENT_INI_NAME = 'lito381-compat.ini'


class ExternalProfileError(ValueError):
    pass


class ClientCompatMismatch(RuntimeError):
    """The client directory does not already hold what this run needs; the external entry refuses instead of writing."""

    def __init__(self, message, differences):
        super().__init__(message)
        self.differences = differences


class RunRootError(ValueError):
    pass


def compat_ini_text(scale):
    """Exactly the ini content the formal flow writes for this scale (development_startup.py, LF form)."""
    return ('[Window]\nWidth=640\nHeight=480\nScalePercent=' + str(scale) +
            '\n[Diagnostics]\nLogging=1\n[NumericBreakthrough]\nEnabled=1\nSchema=4\n')


def _ini_scale(text):
    for line in text.splitlines():
        key, sep, value = line.partition('=')
        if sep and key.strip().casefold() == 'scalepercent':
            return value.strip()
    return None


def verify_client_compat(client, scale, ddraw_sha=CLIENT_DDRAW_SHA):
    """Read-only check of client/litoddraw.dll and client/lito381-compat.ini.

    Returns a receipt when both already equal what this run needs.  Otherwise raises
    ClientCompatMismatch listing every difference; the files are never touched.
    The formal helper writes the ini in text mode, so CRLF and LF are treated alike.
    """
    client = Path(client)
    differences = []
    ddraw = client / CLIENT_DDRAW_NAME
    if not ddraw.is_file():
        differences.append({'file': str(ddraw), 'problem': 'missing', 'expected_sha256': ddraw_sha})
        ddraw_actual = None
    else:
        ddraw_actual = hashlib.sha256(ddraw.read_bytes()).hexdigest().upper()
        if ddraw_actual != ddraw_sha:
            differences.append({'file': str(ddraw), 'problem': 'sha256', 'actual_sha256': ddraw_actual, 'expected_sha256': ddraw_sha})
    ini = client / CLIENT_INI_NAME
    expected_ini = compat_ini_text(scale)
    if not ini.is_file():
        differences.append({'file': str(ini), 'problem': 'missing', 'expected_content': expected_ini})
        ini_sha = None
    else:
        raw = ini.read_bytes()
        ini_sha = hashlib.sha256(raw).hexdigest().upper()
        try:
            actual_ini = raw.decode('ascii').replace('\r\n', '\n')
        except UnicodeDecodeError:
            actual_ini = raw.decode('latin-1').replace('\r\n', '\n')
        if actual_ini != expected_ini:
            actual_scale = _ini_scale(actual_ini)
            if actual_scale is not None and actual_scale != str(scale):
                differences.append({'file': str(ini), 'problem': 'scale', 'actual_scale_percent': actual_scale,
                                    'expected_scale_percent': str(scale), 'actual_content': actual_ini,
                                    'expected_content': expected_ini})
            else:
                differences.append({'file': str(ini), 'problem': 'content', 'actual_content': actual_ini,
                                    'expected_content': expected_ini})
    if differences:
        lines = ['外部伺服器入口只讀取、不改 0-客戶端；以下檔案與本次需要的不同，已拒絕啟動（未覆寫任何檔案）：']
        for item in differences:
            if item['problem'] == 'missing':
                lines.append(f"- {item['file']} 不存在")
            elif item['problem'] == 'sha256':
                lines.append(f"- {item['file']} SHA-256 {item['actual_sha256']}，預期 {item['expected_sha256']}")
            elif item['problem'] == 'scale':
                lines.append(f"- {item['file']} 現有 ScalePercent={item['actual_scale_percent']}，本次選擇 "
                             f"{item['expected_scale_percent']}；請在登入器改選 {item['actual_scale_percent']}% 縮放後再試")
            else:
                lines.append(f"- {item['file']} 內容不同；現有 {item['actual_content']!r}，預期 {item['expected_content']!r}")
        raise ClientCompatMismatch('\n'.join(lines), differences)
    return {'client_directory': str(client), 'writes': 0,
            'litoddraw_dll': {'path': str(ddraw), 'sha256': ddraw_actual, 'matches_pin': True},
            'compat_ini': {'path': str(ini), 'sha256': ini_sha, 'scale_percent': scale, 'matches_expected': True}}


def _is_reparse(node):
    """True for symlinks, junctions and any other Windows reparse point."""
    node = Path(node)
    if node.is_symlink():
        return True
    is_junction = getattr(node, 'is_junction', None)
    if is_junction is not None and is_junction():
        return True
    try:
        attributes = getattr(os.lstat(node), 'st_file_attributes', 0)
    except OSError:
        return False
    return bool(attributes & getattr(stat, 'FILE_ATTRIBUTE_REPARSE_POINT', 0x400))


def _inside(child, parent):
    child = os.path.normcase(os.path.abspath(child))
    parent = os.path.normcase(os.path.abspath(parent)).rstrip('\\/')
    return child == parent or child.startswith(parent + os.sep) or child.startswith(parent + '/')


def resolve_run_root(value, client, is_reparse=_is_reparse):
    """Validate an explicit run root: absolute existing directory, no reparse point on its path, not inside the client."""
    if not isinstance(value, str) or not value.strip():
        raise RunRootError('run root must be a non-empty path')
    path = Path(value)
    if not path.is_absolute():
        raise RunRootError('run root must be an absolute path: ' + value)
    path = Path(os.path.abspath(path))
    for node in (path, *path.parents):
        if is_reparse(node):
            raise RunRootError('run root path contains a junction/reparse point: ' + str(node))
    if not path.is_dir():
        raise RunRootError('run root directory does not exist: ' + str(path))
    if _inside(path, client):
        raise RunRootError('run root must not be inside the client directory: ' + str(path))
    return path


def _identifier(value):
    return isinstance(value, str) and bool(value) and all(c.isascii() and (c.isalnum() or c == '_') for c in value)


def validate_profile(payload):
    """Validate an external profile dict; return a normalised copy.  Mirrors ExternalServerProfile.Validate (C#)."""
    if not isinstance(payload, dict):
        raise ExternalProfileError('external profile must be a JSON object')
    if payload.get('schemaVersion') != 1:
        raise ExternalProfileError('schemaVersion must be 1')
    if payload.get('kind') != PROFILE_KIND:
        raise ExternalProfileError('kind must be "external"')
    profile_id = payload.get('profileId')
    if not isinstance(profile_id, str) or not profile_id or any(
            not (c.isascii() and (c.isalnum() or c in '._-')) for c in profile_id):
        raise ExternalProfileError('profileId may contain only ASCII letters, digits, dot, dash, underscore')
    display = payload.get('displayName')
    if not isinstance(display, str) or not display.strip():
        raise ExternalProfileError('displayName must not be empty')
    try:
        address = ipaddress.IPv4Address(str(payload.get('host', '')).strip())
    except ipaddress.AddressValueError as error:
        raise ExternalProfileError('host must be an IPv4 address') from error
    if not address.is_loopback:
        raise ExternalProfileError('external server host must be loopback (127.0.0.0/8)')
    port = payload.get('port')
    if type(port) is not int or not 1024 <= port <= 65527:
        raise ExternalProfileError('port must be an integer between 1024 and 65527')
    static_database = payload.get('staticDatabase')
    if not _identifier(static_database):
        raise ExternalProfileError('staticDatabase must be an ASCII identifier')
    sources = payload.get('sources')
    if not isinstance(sources, list) or not 1 <= len(sources) <= 8:
        raise ExternalProfileError('sources must list 1 to 8 source servers')
    normalised = []
    for source in sources:
        if not isinstance(source, dict):
            raise ExternalProfileError('each source must be an object')
        source_id, name, database = source.get('id'), source.get('name'), source.get('database')
        if type(source_id) is not int or not 1 <= source_id <= 8:
            raise ExternalProfileError('source id must be an integer between 1 and 8')
        if not isinstance(name, str) or not name.strip() or ',' in name or any(ord(c) < 32 or ord(c) == 127 for c in name):
            raise ExternalProfileError('source name must be non-empty, without comma or control characters')
        try:
            encoded = name.encode('cp950')
        except UnicodeEncodeError as error:
            raise ExternalProfileError('source name must be encodable as cp950') from error
        if not 1 <= len(encoded) <= 63:
            raise ExternalProfileError('source name must be 1 to 63 cp950 bytes')
        if not _identifier(database):
            raise ExternalProfileError('source database must be an ASCII identifier')
        normalised.append({'id': source_id, 'name': name, 'database': database})
    ids = [s['id'] for s in normalised]
    if len(set(ids)) != len(ids):
        raise ExternalProfileError('source ids must be unique')
    databases = [s['database'].casefold() for s in normalised]
    if len(set(databases)) != len(databases) or static_database.casefold() in databases:
        raise ExternalProfileError('source databases must be unique and differ from staticDatabase')
    names = [s['name'].casefold() for s in normalised]
    if len(set(names)) != len(names):
        raise ExternalProfileError('source names must be unique')
    return {'schemaVersion': 1, 'kind': PROFILE_KIND, 'profileId': profile_id, 'displayName': display.strip(),
            'host': str(address), 'port': port, 'staticDatabase': static_database, 'sources': normalised}


def load_profile(path):
    return validate_profile(json.loads(Path(path).read_text(encoding='utf-8-sig')))


def expected_endpoints(profile):
    """Login endpoints the native flow needs: the base port plus base+id for every source (same rule as the formal flow)."""
    host, port = profile['host'], profile['port']
    return [(host, port)] + [(host, port + source['id']) for source in profile['sources']]


def effective_from_profile(profile):
    """Build the subset of UnifiedConfiguration settings that development_startup.select_source/source_catalog read."""
    sources = profile['sources']
    return {'bind': profile['host'],
            'login.source.port.base': str(profile['port']),
            'login.classic.server.ids': ','.join(str(s['id']) for s in sources),
            'login.classic.server.names': ','.join(s['name'] for s in sources),
            'database.dynamic.databases': ','.join(s['database'] for s in sources),
            'database.static.database': profile['staticDatabase']}


def native_profile(selected, profile):
    """Same field set as development_startup.main writes to run/server-profile.json."""
    sources = profile['sources']
    return dict(selected, localServerConfig=LOCAL_CONFIG_PLACEHOLDER,
                classicSourceNames=[s['name'] for s in sources],
                dynamicDatabases=[s['database'] for s in sources],
                nativeSourceIds=[s['id'] for s in sources],
                nativeSourcePortBase=profile['port'])


def check_endpoints(profile, timeout=2.0, connect=socket.create_connection):
    """TCP connect only (no payload) to every expected endpoint; never binds or rewrites ports."""
    results = []
    for host, port in expected_endpoints(profile):
        try:
            with connect((host, port), timeout=timeout):
                ok, error = True, None
        except OSError as failure:
            ok, error = False, str(failure)
        results.append({'host': host, 'port': port, 'connect': ok, 'payload_bytes_sent': 0, 'error': error})
    return results


def emit(value): print(json.dumps(value, ensure_ascii=False), flush=True)


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _formal_helper():
    if hashlib.sha256(FORMAL_HELPER.read_bytes()).hexdigest().upper() != FORMAL_HELPER_SHA:
        raise ValueError('Reviewed formal helper changed: ' + str(FORMAL_HELPER))
    return _load('lito_formal_development_startup', FORMAL_HELPER)


def refuse_before_start(error, stage):
    """T152 / Issue #443: every refusal before anything is created ends with the same failed + finished events.

    Nothing was started or created (no run directory, no JVM, no native), so cleanup is
    trivially complete and the launcher must not report "清理尚未確認".
    """
    print(str(error), file=sys.stderr, flush=True)
    emit({'event': 'failed', 'reason': str(error), 'stage': stage, 'writes': 0,
          'differences': getattr(error, 'differences', [])})
    emit({'event': 'finished', 'exit_code': 1, 'accepted': False, 'cleanup_verified': True,
          'server_owned': False, 'result': None, 'product_complete': False})
    return 1


def main(argv=None, formal_helper=None):
    parser = argparse.ArgumentParser(description='Connect the original LiTo.bin flow to an already running loopback LiTo Server.')
    parser.add_argument('--serve', action='store_true', required=True)
    parser.add_argument('--run-id', required=True)
    parser.add_argument('--client-directory', required=True)
    parser.add_argument('--scale', type=int, choices=(100, 125, 150, 200), required=True)
    parser.add_argument('--service', choices=('StandardArena',), required=True)
    parser.add_argument('--source-id', type=int, choices=range(1, 9), required=True)
    parser.add_argument('--external-profile', required=True)
    parser.add_argument('--run-root', help='Write runs/<run-id> under this directory instead of the delivery app directory.')
    args = parser.parse_args(argv)
    # Everything up to run.mkdir() is read-only: no run directory, JVM or native exists yet.
    # T152 / Issue #443: the formal resource check (check_source, same pins as the formal
    # standard flow) used to raise outside this guard, so the launcher saw no finished event
    # and reported "清理尚未確認".  Any refusal here now ends with failed + finished.
    stage = 'arguments'
    try:
        if not args.run_id.startswith('external-') or any(not (c.isascii() and (c.isalnum() or c == '-')) for c in args.run_id):
            raise ValueError('external run id must start with "external-"')
        stage = 'external_profile'
        profile = load_profile(args.external_profile)
        stage = 'formal_helper'
        dev = (formal_helper or _formal_helper)()
        if dev.PINS.get(dev.DLL) != CLIENT_DDRAW_SHA:
            raise ValueError('litoddraw.dll pin differs from the reviewed formal helper')
        # Exactly the formal standard flow's resource check (development_startup.main calls the same function).
        stage = 'check_source'
        dev.check_source(args.client_directory)
        # Read-only: refuse before anything is created if the client is not already prepared.
        stage = 'client_compat'
        client_compat = verify_client_compat(dev.CLIENT, args.scale)
        stage = 'run_root'
        run_root = resolve_run_root(args.run_root, dev.CLIENT) if args.run_root is not None else dev.DELIVERY
    except Exception as error:
        return refuse_before_start(error, stage)
    # Identity helpers only; importing Start-Shared-World.py does not start a JVM.
    module = _load('lito_shared_world_identity', dev.ENTRY)
    effective = effective_from_profile(profile)
    source_ids = [s['id'] for s in profile['sources']]
    selected = dev.select_source(effective, args.source_id)
    run = run_root / 'runs' / args.run_id
    run.mkdir(parents=True, exist_ok=False)
    commands = queue.Queue()

    def reader():
        for line in sys.stdin: commands.put(line)
        commands.put(None)
    threading.Thread(target=reader, daemon=True).start()

    def before_native():
        try: line = commands.get_nowait()
        except queue.Empty: return
        if line is None or json.loads(line).get('command') == 'stop': raise dev.StopSession()
        raise ValueError('Unknown owned control command')

    def server_alive():
        # The external Server is not owned; only re-check the selected login endpoint.
        host, port = selected['host'], selected['port']
        try:
            with socket.create_connection((host, port), timeout=2): pass
        except OSError as error:
            raise RuntimeError(f'External Server endpoint {host}:{port} is no longer reachable: {error}') from error

    probe = None; native_handle = None; native_streams = []; additional = []; additional_receipts = []; next_client = 2
    exit_code = 0
    report = {'run_id': args.run_id, 'external_server': True, 'server_owned': False, 'external_profile': profile,
              'external_profile_path': str(Path(args.external_profile).resolve()), 'game_window_verified': False,
              'runtime_client': str(dev.CLIENT / 'LiTo.bin'), 'bin_sha256': dev.BIN_SHA, 'service': args.service,
              'scale': args.scale, 'cleanup_verified': False, 'run_root': str(run_root),
              'client_writes_by_launcher': 0, 'client_compat': client_compat}
    try:
        endpoints = check_endpoints(profile)
        report['endpoint_checks'] = endpoints
        missing = [f"{e['host']}:{e['port']}" for e in endpoints if not e['connect']]
        if missing:
            raise RuntimeError('External Server is not listening on: ' + ', '.join(missing))
        ready = {'event': 'ready', 'external_server': True, 'server_owned': False, 'run': str(run),
                 'profile_id': profile['profileId'], 'host': profile['host'], 'port': profile['port'],
                 'endpoints': endpoints, 'selected_source_id': args.source_id, 'service_mode': 'standard',
                 'product_complete': False}
        dev.save(run / 'server-ready.json', ready); emit(ready)
        before_native()
        # T139: re-check (read only) right before the native start; no copy, no rewrite of the client directory.
        report['client_compat_before_native'] = verify_client_compat(dev.CLIENT, args.scale)
        render_log = dev.CLIENT / 'lito381-compat.log'
        report['render_log_initial_bytes'] = render_log.stat().st_size if render_log.exists() else 0
        native = native_profile(selected, profile)
        dev.save(run / 'server-profile.json', native)
        os.environ['LITO_NUMERIC_BREAKTHROUGH'] = '4'
        os.environ['LITO_EXTENDED_STATS_SCHEMA'] = '2'
        native_env = dict(os.environ, LITO_NATIVE_LOGIN_HOST=profile['host'],
                          LITO_NATIVE_SOURCES=dev.source_catalog(effective, source_ids))
        for key in ('LITO381_ACCOUNT', 'LITO381_PASSWORD', 'LITO381_CHARACTER', 'LITO381_CHARACTER_NAME'):
            native_env.pop(key, None)

        dev.check_source(args.client_directory); server_alive(); before_native()
        command = dev.native_args(args.service, run); report['native_command'] = command
        for name in ('native.stdout.log', 'native.stderr.log'): native_streams.append((run / name).open('wb'))
        probe = subprocess.Popen(command, cwd=dev.CLIENT, stdin=subprocess.DEVNULL, stdout=native_streams[0],
                                 stderr=native_streams[1], creationflags=subprocess.CREATE_NO_WINDOW, env=native_env)
        report['probe_lifetime_identity'] = module.lifetime_identity(probe)
        report['probe_identity'] = module.identity(probe)
        deadline = time.monotonic() + 40
        while time.monotonic() < deadline:
            if probe.poll() is not None: raise RuntimeError('Native launcher exited: ' + str(probe.returncode))
            child = dev.find_child(probe.pid, dev.CLIENT / 'LiTo.bin')
            if child:
                native_handle, native_id = child; report['native_identity'] = native_id
                dev.save(run / 'native-created.json', report)
                emit({'event': 'native_created', 'identity': native_id, 'run': str(run), 'window_verified': False}); break
            time.sleep(.1)
        else: raise TimeoutError('Actual LiTo.bin child was not observed within 40 seconds')
        deadline = time.monotonic() + 90
        resource_receipt = run / 'native-probe.shared-ready.json'
        while not resource_receipt.exists():
            before_native()
            if probe.poll() is not None: raise RuntimeError('Native exited before common resources became ready')
            if time.monotonic() >= deadline: raise TimeoutError('Common resources initialization exceeded 90 seconds')
            time.sleep(.1)
        resources = json.loads(resource_receipt.read_text(encoding='utf-8'))
        if resources != {'pid': native_id['pid'], 'profile': 'tw13081901-v1',
                         'base_count': 30000, 'total_count': 30285, 'native_cleanup_complete': True}:
            raise RuntimeError('Common resource readiness identity/count mismatch')
        report['shared_resources_ready'] = resources
        emit({'event': 'resources_ready', **resources})
        while probe.poll() is None or any(client.alive() for client in additional):
            for client in list(additional):
                if not client.alive():
                    client.cleanup(); additional_receipts.append(client.report); additional.remove(client)
            try: line = commands.get(timeout=.25)
            except queue.Empty: continue
            if line is None or json.loads(line).get('command') == 'stop': break
            if json.loads(line).get('command') == 'add_client':
                client = dev.AdditionalNative(module, run / ('client-' + str(next_client))); next_client += 1
                additional.append(client)
                try:
                    dev.check_source(args.client_directory)
                    def check_extra(): before_native()
                    pid = client.start(args.service, native, native_env, check_extra)
                    emit({'event': 'additional_client_result', 'success': True, 'pid': pid, 'run': str(client.run)})
                except dev.StopSession: break
                except Exception as error:
                    client.report['failure'] = str(error); client.cleanup()
                    additional_receipts.append(client.report); additional.remove(client)
                    emit({'event': 'additional_client_result', 'success': False, 'reason': str(error)})
                continue
            raise ValueError('Unknown owned control command')
        if probe.poll() is not None:
            report['native_probe_exit_code'] = probe.returncode
            if probe.returncode != 0: raise RuntimeError('Native launcher ended with failure: ' + str(probe.returncode))
    except dev.StopSession:
        pass
    except BaseException as error:
        exit_code = 1; report['failure'] = str(error); traceback.print_exc(file=sys.stderr)
        emit({'event': 'failed', 'reason': str(error)})
    finally:
        errors = []
        for client in reversed(additional):
            try: client.cleanup(); additional_receipts.append(client.report)
            except BaseException as error: errors.append('additional client cleanup: ' + str(error))
        report['additional_clients'] = additional_receipts
        try:
            if probe:
                if probe.poll() is None:
                    lifetime = module.lifetime_identity(probe)
                    if report.get('probe_lifetime_identity') and lifetime != report['probe_lifetime_identity']: raise RuntimeError('Probe lifetime identity drift')
                    if report.get('probe_identity') and not module.same_identity(module.identity(probe), report['probe_identity']): raise RuntimeError('Probe identity drift')
                    probe.terminate()  # Retained handle; its kill-on-close Job owns the native child.
                report['probe_exit_code'] = probe.wait(timeout=5)
                if native_handle:
                    if dev.kernel.WaitForSingleObject(native_handle, 5000) != 0: raise RuntimeError('Owned BIN did not exit')
                    dev.kernel.CloseHandle(native_handle); native_handle = None
                    report['native_exit_verified'] = True
                probe._handle.Close(); report['probe_handle_closed'] = True
        except BaseException as error: errors.append(str(error))
        for stream in native_streams:
            try: stream.close()
            except BaseException as error: errors.append('native stream close: ' + str(error))
        try:
            if 'render_log' in locals() and render_log.exists():
                with render_log.open('rb') as stream:
                    stream.seek(report['render_log_initial_bytes'])
                    (run / 'native-render.log').write_bytes(stream.read())
        except BaseException as error: report['render_capture_failure'] = str(error)
        # The external Server is never stopped here; it stays owned by whoever started it.
        report['cleanup_errors'] = errors
        report['cleanup_verified'] = not errors and native_handle is None
        if errors: exit_code = 1
        dev.save(run / 'terminal.json', report)
        emit({'event': 'finished', 'exit_code': exit_code, 'accepted': exit_code == 0, 'cleanup_verified': report['cleanup_verified'],
              'server_owned': False, 'result': str(run / 'terminal.json'), 'product_complete': False})
    return exit_code


if __name__ == '__main__': sys.exit(main())
