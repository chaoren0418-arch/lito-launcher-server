#!/usr/bin/env python3
"""以 CFR 反編譯指定候選 Target，於 scratch 改寫並驗證其語意。"""
import argparse
import json
import os
from pathlib import Path
import sys
import zipfile

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import as_record as tool


def main():
    server = tool.ROOT.parent.parent
    repo = server.parent.parent
    parser = argparse.ArgumentParser()
    parser.add_argument('--cfr', type=Path, required=True, help='CFR 0.152 JAR')
    parser.add_argument('--candidate', type=Path, default=server / 'reference/candidate-50635f7e.jar')
    args = parser.parse_args()
    entry = 'local/lito381/server/NativeReturnNatureMaterialAction$Target.class'
    output = tool.TARGET / 'candidate'
    scratch = repo / 'scratch/as-record/candidate'
    output.mkdir(parents=True, exist_ok=True)
    scratch.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.candidate) as archive:
        (output / 'Target.class').write_bytes(archive.read(entry))
    tool.run([tool.jdk('java'), '-jar', args.cfr, output / 'Target.class',
              '--extraclasspath', args.candidate, '--outputdir', scratch / 'cfr', '--silent', 'true'])
    decompiled = scratch / 'cfr/local/lito381/server/NativeReturnNatureMaterialAction$Target.java'
    text = decompiled.read_text(encoding='utf-8')
    text = text.replace('import java.lang.invoke.MethodHandle;\n', '')
    text = text.replace('import java.lang.runtime.ObjectMethods;\n', '')
    declaration = 'private static final class NativeReturnNatureMaterialAction.Target\nextends Record {'
    if declaration not in text:
        raise RuntimeError('unexpected CFR output: Target declaration')
    # 外層只提供 NestHost/InnerClasses 的編譯上下文，不列入比較或交付。
    text = text.replace(declaration, 'final class NativeReturnNatureMaterialAction {\n'
                        '@AsRecord(components={"caster","owner","pet","stamp"})\n'
                        'private static final class Target {')
    text = text.replace('private NativeReturnNatureMaterialAction.Target(', 'private Target(')
    start = text.index('    @Override\n    public final String toString()')
    end = text.index('    public ClientSession caster()', start)
    text = text[:start] + text[end:] + '\n}\n'
    sample = scratch / 'NativeReturnNatureMaterialAction.java'
    sample.write_text(text, encoding='utf-8')
    cp = tool.build()
    classes = output / 'classes'
    tool.compile_java([server / 'src/main/java/local/lito381/server/AsRecord.java', sample],
                      classes, str(args.candidate))
    tool.run([tool.jdk('java'), '-cp', cp, 'local.lito381.asrecord.AsRecord', classes])
    converted = output / 'converted.jar'
    with zipfile.ZipFile(converted, 'w') as archive:
        archive.write(classes / entry, entry)
    diff_classes = output / 'semantic-diff'
    tool.compile_java(sorted((server / 'tools/semantic-diff/src/main/java').rglob('*.java')),
                      diff_classes, tool.dependencies())
    report = scratch / 'semantic-diff.json'
    tool.run([tool.jdk('java'), '-cp', str(diff_classes) + os.pathsep + tool.dependencies(),
              'local.lito381.semanticdiff.SemanticDiff', '--left', converted, '--right', args.candidate,
              '--classes', entry, '--json', report])
    data = json.loads(report.read_text(encoding='utf-8'))
    if data['counts']['SAME'] != 1 or any(data['counts'][key] for key in ['DIFF', 'ONLY_LEFT', 'ONLY_RIGHT']):
        raise RuntimeError(f'candidate semantic-diff failed: {report}')
    if data['classSelection']['missingLeft'] or data['classSelection']['extraLeft']:
        raise RuntimeError('candidate output contains unexpected entries')
    print('CANDIDATE PASS: NativeReturnNatureMaterialAction$Target semantic-diff SAME')


if __name__ == '__main__':
    main()
