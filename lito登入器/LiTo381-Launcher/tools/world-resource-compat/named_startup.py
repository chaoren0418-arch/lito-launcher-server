"""Candidate-only explicit-name loader. The formal release is not replaced."""
from pathlib import Path
import ast,hashlib,inspect,json,os,sys
import selection_startup
R=Path(__file__).resolve().parent
W=R/'build/named-ddraw'

def prepare_probe(original):
    W.mkdir(exist_ok=True)
    if hashlib.sha256(original.read_bytes()).hexdigest()!='a1097e0f7617d755448e279bad27f0470b4c643ccea85415f7089f524bdd6c15':
        raise ValueError('original probe changed')
    text=original.read_text('utf-8')
    for old,new,count in [
        ("m['name'].lower()=='ddraw.dll'","m['name'].lower()=='litoddraw.dll'",1),
        ("resolve_export(modules,'ddraw.dll','LiToSourceSession')","resolve_export(modules,'litoddraw.dll','LiToSourceSession')",1),
        ('m["name"].casefold() == "ddraw.dll"','m["name"].casefold() == "litoddraw.dll"',1),
        ('resolve_export([candidate_ddraw], "ddraw.dll", "LiToSharedResourceGate")',
         'resolve_export([candidate_ddraw], "litoddraw.dll", "LiToSharedResourceGate")',1),
        ('            if startup_gate_held and not pending_conditional_patches:\n',
         '            if startup_gate_held and not pending_conditional_patches:\n'
         '                from named_ddraw import install as install_named_ddraw\n'
         '                report["named_ddraw"] = install_named_ddraw(__import__("sys").modules[__name__], process.hProcess, root_pid, cwd)\n'
         '                if os.environ.get("LITO_NAMED_DDRAW_SMOKE") == "1":\n'
         '                    report["status"] = "named-loader-only-verified"\n'
         '                    break\n',1)]:
        if text.count(old)!=count:raise ValueError('named loader seam drift: '+old)
        text=text.replace(old,new)
    # Retain all original dependencies by absolute import roots.
    marker='import argparse\n'
    if text.count(marker)!=1:raise ValueError('probe import seam changed')
    text=text.replace(marker,'import sys\nsys.path[:0]='+repr([str(R),str(original.parent)])+'\n'+marker)
    probe=W/'lito_runtime_probe_named.py';probe.write_text(text,'utf-8')
    return probe

def configure():
    module,receipt=selection_startup.configure()
    probe=prepare_probe(module.PROBE)
    # Change only the destination of the existing startup copy, not its ownership flow.
    tree=ast.parse(inspect.getsource(module.main));changed=0
    for node in ast.walk(tree):
        if isinstance(node,ast.Constant) and node.value=='ddraw.dll':node.value='litoddraw.dll';changed+=1
    if changed!=1:raise ValueError('startup DLL destination seam changed')
    exec(compile(ast.fix_missing_locations(tree),str(Path(module.__file__)), 'exec'),module.__dict__)
    module.PROBE=probe
    original_native_args=module.native_args
    def named_native_args(service,run):
        args=original_native_args(service,run)
        if '--multi-instance-startup-gate' not in args:
            args.append('--multi-instance-startup-gate')
        return args
    module.native_args=named_native_args
    for p in (probe,R/'named_ddraw.py',R/'named_startup.py'):
        module.PINS[p]=hashlib.sha256(p.read_bytes()).hexdigest().upper()
    os.environ['LITO_NAMED_DDRAW_SHA256']=receipt['dll']['sha256']
    module.check_source(module.CLIENT)
    if (module.CLIENT/'ddraw.dll').exists():raise ValueError('client ddraw.dll has not been renamed')
    return module,receipt

def main():
    module,receipt=configure()
    if len(sys.argv)==1 or sys.argv[1]=='--check-only':
        print(json.dumps({'status':'named-loader-preflight','client_dll':str(module.CLIENT/'litoddraw.dll'),
            'formal_updated':False},ensure_ascii=False));return 0
    if sys.argv[1]!='--launch':raise ValueError('use --check-only or --launch')
    sys.argv=[str(module.__file__),*sys.argv[2:]]
    return module.main()
if __name__=='__main__':sys.exit(main())
