"""Launch only the sealed candidate; leave the published launcher/server untouched."""
from pathlib import Path
import argparse, ast, hashlib, json, subprocess, sys, uuid

ROOT = Path(__file__).resolve().parent
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
read = lambda p: json.loads(p.read_text('utf-8'))

def checked(ref):
    path = Path(ref['path'])
    if sha(path) != ref['sha256'].lower():
        raise ValueError('候選檔案驗證失敗：' + str(path))
    return path

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--verify-only', action='store_true')
    args = parser.parse_args()
    q = read(ROOT / 'build/qualification-current.json')
    if not q.get('same_sha_backend_regression_passed'):
        raise ValueError('候選尚未通過完整自動驗證，暫不開啟遊戲。')
    checked(q['runtime'])
    entry_ref = read(checked(q['prepared_native_entry']))
    entry = checked(entry_ref['adapted'])
    checked(entry_ref['server_entry'])
    checked(entry_ref['native_bin'])
    tree = ast.parse(entry.read_text('utf-8'))
    assignments = {n.targets[0].id: n.value for n in tree.body
                   if isinstance(n, ast.Assign) and isinstance(n.targets[0], ast.Name)}
    pins = assignments['PINS']
    for key, value in zip(pins.keys, pins.values):
        checked(dict(path=ast.literal_eval(key.args[0]), sha256=ast.literal_eval(value)))
    print('候選 ' + q['runtime']['sha256'][:12] + ' 驗證通過；正式版未替換。', flush=True)
    if args.verify_only:
        return 0
    client = Path(ast.literal_eval(assignments['CLIENT'].args[0]))
    delivery = Path(ast.literal_eval(assignments['DELIVERY'].args[0]))
    run_id = 'battle-npc-native-' + uuid.uuid4().hex
    log_path = ROOT / 'build/staging' / (run_id + '.log')
    command = [sys.executable, '-B', '-X', 'utf8', str(entry), '--serve',
               '--run-id', run_id, '--client-directory', str(client),
               '--scale', '200', '--service', 'StandardArena', '--source-id', '1']
    print('使用 200% 視窗。請從遊戲內選服並登入；正常關閉遊戲後會清理本次伺服器。', flush=True)
    print('若要中止，按 Ctrl+C；紀錄：' + str(log_path), flush=True)
    with log_path.open('wb') as log:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.PIPE,
                                   stdout=log, stderr=subprocess.STDOUT,
                                   creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait()
        except KeyboardInterrupt:
            if process.poll() is None:
                try:
                    process.stdin.write(b'{"command":"stop"}\n')
                    process.stdin.flush()
                except (BrokenPipeError, OSError):
                    pass
            code = process.wait()
        finally:
            process.stdin.close()
    terminal = delivery / 'runs' / run_id / 'terminal.json'
    result = read(terminal) if terminal.exists() else {}
    cleaned = result.get('cleanup_verified') is True
    print('測試程序已清理。' if cleaned else '請保留本次紀錄：尚未取得完整清理成功收據。', flush=True)
    if code:
        print('候選啟動或執行未完成，詳見紀錄：' + str(log_path), flush=True)
    return code if code else (0 if cleaned else 1)

if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
