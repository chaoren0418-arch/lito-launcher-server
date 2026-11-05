#!/usr/bin/env python3
"""可重跑的 CLI、JVM 行為、javac/ASM semantic-diff 驗收。"""
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile
sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent))
import as_record as tool

SERVER = tool.ROOT.parent.parent
REPO = SERVER.parent.parent
SCRATCH = REPO / 'scratch/as-record/selftest'
OUT = tool.TARGET / 'selftest'
ANNOTATION = SERVER / 'src/main/java/local/lito381/server/AsRecord.java'
FINAL_ANNOTATION = SERVER / 'src/main/java/local/lito381/server/AsmFinal.java'
MARKER = 'import local.lito381.server.AsRecord;\n'


def source(group, name, text):
    path = SCRATCH / group / (name + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')
    return path


def compile_group(group, texts, cp):
    output = OUT / group
    paths = [source(group, name, text) for name, text in texts.items()]
    tool.compile_java([ANNOTATION, FINAL_ANNOTATION, *paths], output, cp)
    return output


def snapshot(directory):
    return {p.relative_to(directory).as_posix(): p.read_bytes() for p in directory.rglob('*.class')}


def convert(directory, cp, error=None):
    result = subprocess.run([tool.jdk('java'), '-Xverify:all', '-cp', cp,
                             'local.lito381.asrecord.AsRecord', str(directory)],
                            capture_output=True, text=True, encoding='utf-8', errors='replace')
    print(result.stdout, end='')
    if error is None:
        assert result.returncode == 0, result.stderr
    else:
        assert result.returncode != 0 and error in result.stderr, result.stderr
        print('expected rejection: ' + result.stderr.strip())


def jar(directory, name):
    path = OUT / (name + '.jar')
    with zipfile.ZipFile(path, 'w') as archive:
        for entry in directory.rglob('*.class'):
            archive.write(entry, entry.relative_to(directory).as_posix())
    return path


def compare(left, right, classes, label, cp):
    report = SCRATCH / (label + '.json')
    tool.run([tool.jdk('java'), '-cp', cp, 'local.lito381.semanticdiff.SemanticDiff',
              '--left', left, '--right', right, '--classes', ','.join(classes), '--json', report])
    data = json.loads(report.read_text(encoding='utf-8'))
    assert data['counts']['SAME'] == len(classes), data
    assert all(data['counts'][key] == 0 for key in ['DIFF', 'ONLY_LEFT', 'ONLY_RIGHT']), data
    print(label + ': semantic-diff SAME')


def main():
    cp = tool.build()
    final_classes = compile_group('asm-final', {
        'Parent': 'class Parent {}',
        'PlainFinal': '''import local.lito381.server.AsmFinal;
@AsmFinal({"value", "global"}) class PlainFinal extends Parent {
 public Object value; public static Object global;
 public PlainFinal() {} public PlainFinal(Object v) { value = v; }
}''',
        'AbstractFinal': '''import local.lito381.server.AsmFinal;
@AsmFinal({"memo"}) abstract class AbstractFinal { Object memo; }''',
    }, cp)
    convert(final_classes, cp)
    checker = OUT / 'asm-final-check'
    tool.compile_java([tool.ROOT / 'src/test/java/AsmFinalCheck.java'], checker, cp)
    tool.run([tool.jdk('java'), '-Xverify:all', '-cp', os.pathsep.join([str(checker), str(final_classes), cp]),
              'AsmFinalCheck', final_classes])
    for label, fields, declaration, error in [
        ('missing', '{"wrong"}', 'Object value;', 'field missing'),
        ('duplicate', '{"value","value"}', 'Object value;', 'duplicate field'),
        ('volatile', '{"value"}', 'volatile Object value;', 'must not be volatile')]:
        bad = compile_group('asm-final-' + label, {
            'Bad': 'import local.lito381.server.AsmFinal; @AsmFinal(' + fields + ') class Bad { ' + declaration + ' }',
            'Good': 'import local.lito381.server.AsmFinal; @AsmFinal({"value"}) class Good { Object value; }',
        }, cp)
        before = snapshot(bad)
        convert(bad, cp, error)
        assert snapshot(bad) == before, 'invalid @AsmFinal partially rewrote batch'
    semantic = OUT / 'semantic-diff'
    tool.compile_java(sorted((SERVER / 'tools/semantic-diff/src/main/java').rglob('*.java')),
                      semantic, tool.dependencies())
    diff_cp = str(semantic) + os.pathsep + tool.dependencies()
    plain = compile_group('plain', {
        'Sample': 'public record Sample<T>(long id, T value, int[] tags, double score) implements java.io.Serializable {}',
        'Empty': 'public record Empty() {}',
        'Order': 'public record Order(String second, int first) {}',
        'Nest': 'public class Nest { public record Item(int n) {} }',
        'Frozen': 'public record Frozen(int value) {}',
    }, cp)
    converted = compile_group('converted', {
        'Sample': MARKER + '''@AsRecord(components={"id","value","tags","score"})
public final class Sample<T> implements java.io.Serializable {
 private final long id; private final T value; private final int[] tags; private final double score;
 public Sample(long id,T value,int[] tags,double score) {this.id=id;this.value=value;this.tags=tags;this.score=score;}
 public long id(){return id;} public T value(){return value;} public int[] tags(){return tags;} public double score(){return score;}
}''',
        'Empty': MARKER + '@AsRecord(components={}) public final class Empty { public Empty() {} }',
        'Order': MARKER + '''@AsRecord(components={"second","first"}) public final class Order {
 private final int first; private final String second;
 public Order(String second,int first){this.second=second;this.first=first;}
 public int first(){return first;} public String second(){return second;}
}''',
        'Nest': MARKER + '''public class Nest {
 @AsRecord(components={"n"}) public static final class Item {
  private final int n; public Item(int n){this.n=n;} public int n(){return n;}
 }
}''',
        'Frozen': MARKER + '''@AsRecord(components={"value"},finalFields={"memo"}) public final class Frozen {
 private final int value; private String memo;
 public Frozen(int value){this.value=value;this.memo="memo";}
 public Frozen(){this.value=0;}
 public int value(){return value;}
}''',
        'Extras': MARKER + '''@Deprecated @AsRecord(components={"id"}) public final class Extras {
 private final int id; public int extra; public Object publication;
 public Extras(int id){this.id=id;this.publication=new Object();}
 public Extras(){this(7);}
 public int id(){return id;}
 public Object allocate(){return new Object();}
}''',
        'Access': '''public final class Access {
 public static int exchange(Extras target, int value){int before=target.extra;target.extra=value;return before;}
 public static void main(String[] args) throws Exception {
  Extras a=new Extras(), b=new Extras(7); exchange(a,42); exchange(b,99);
  check(exchange(a,43)==42 && a.extra==43,"cross-class getfield/putfield");
  check(Extras.class.isRecord(),"isRecord");
  var components=Extras.class.getRecordComponents();
  check(components.length==1 && components[0].getName().equals("id") && components[0].getType()==int.class,"components");
  check(a.equals(b) && a.hashCode()==b.hashCode() && a.hashCode()==7,"object methods ignore extras");
  check(a.toString().equals("Extras[id=7]"),"toString");
  check(!a.equals(new Extras(8)) && !a.equals(null) && !a.equals("x"),"equals boundaries");
  check(a.publication.getClass()==Object.class && a.allocate().getClass()==Object.class,"new Object preserved");
  check(Extras.class.isAnnotationPresent(Deprecated.class),"other annotation preserved");
  check(java.lang.reflect.Modifier.isFinal(Frozen.class.getDeclaredField("memo").getModifiers()),"finalFields");
  check(new Frozen().value()==0 && new Frozen(3).value()==3,"both finalFields constructors");
  check(Sample.class.getGenericSuperclass()==Record.class,"generic superclass");
  check(Sample.class.getRecordComponents()[1].getGenericType().getTypeName().equals("T"),"generic component");
  check(Empty.class.isRecord() && new Empty().equals(new Empty()),"empty record");
  check(Nest.Item.class.isRecord(),"nested record");
  check(Order.class.getRecordComponents()[0].getName().equals("second"),"component order");
  System.out.println("runtime: isRecord/components/object methods/extra getfield-putfield/finalFields/generic/nested PASS");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}''',
    }, cp)
    before = snapshot(converted)
    convert(converted, cp)
    after = snapshot(converted)
    for name in ['Access.class', 'Nest.class', 'local/lito381/server/AsRecord.class']:
        assert before[name] == after[name], name
    assert b'java/lang/Record' in after['Extras.class']
    convert(converted, cp)
    assert after == snapshot(converted), 'idempotence'
    print('unannotated bytes unchanged; second pass bytes unchanged PASS')
    tool.run([tool.jdk('java'), '-Xverify:all', '-cp', converted, 'Access'])
    tool.compile_java([tool.ROOT / 'src/test/java/FixtureSupport.java'], OUT / 'support', cp)
    tool.run([tool.jdk('java'), '-cp', str(OUT / 'support') + os.pathsep + cp,
              'FixtureSupport', plain / 'Frozen.class'])
    compare(jar(converted, 'converted'), jar(plain, 'plain'),
            ['Sample', 'Empty', 'Order', 'Nest$Item'], 'javac-record', diff_cp)
    compare(OUT / 'converted.jar', OUT / 'plain.jar', ['Frozen'], 'final-fields-asm', diff_cp)
    negatives = {
        'missing-final': ('@AsRecord(components={},finalFields={"absent"}) final class Bad {}', 'finalFields field not found'),
        'missing-component': ('@AsRecord(components={"absent"}) final class Bad {}', 'component field not found'),
        'duplicate': ('@AsRecord(components={"x","x"}) final class Bad {}', 'duplicate field name'),
        'static-final': ('@AsRecord(components={},finalFields={"x"}) final class Bad { static int x; }', 'non-static'),
        'object-method': ('@AsRecord(components={}) final class Bad { public int hashCode(){return 1;} }', 'do not declare'),
        'superclass': ('@AsRecord(components={}) final class Bad extends java.util.ArrayList<String> {}', 'superclass must be'),
        'accessor': ('@AsRecord(components={"x"}) final class Bad { private final int x=1; }', 'missing public accessor'),
        'mutable-component': ('@AsRecord(components={"x"}) final class Bad { private int x; public int x(){return x;} }', 'private final'),
    }
    for label, (body, error) in negatives.items():
        invalid = compile_group('invalid-' + label, {
            'Bad': MARKER + body,
            'AAA': MARKER + '@AsRecord(components={}) final class AAA {}',
        }, cp)
        original = snapshot(invalid)
        convert(invalid, cp, error)
        assert original == snapshot(invalid), 'invalid batch wrote files: ' + label
    print('SELFTEST PASS')


if __name__ == '__main__':
    main()
