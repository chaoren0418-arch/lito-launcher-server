#!/usr/bin/env python3
"""隔離 Maven process-classes fixture；使用與 #134 相同的 AsRecord.main 入口。"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile
from xml.sax.saxutils import escape

sys.dont_write_bytecode = True
import as_record as tool


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--maven', required=True)
    p.add_argument('--maven-repo', required=True, type=Path)
    p.add_argument('--offline', action='store_true')
    args = p.parse_args()
    repo = tool.ROOT.parents[3]
    server = tool.ROOT.parents[1]
    module = repo / 'scratch/as-record/asm-final-maven'
    source = module / 'src'
    target = tool.TARGET / 'asm-final-maven'
    source.mkdir(parents=True, exist_ok=True)
    target.mkdir(parents=True, exist_ok=True)
    marker = source / 'local/lito381/server/AsmFinal.java'
    marker.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(server / 'src/main/java/local/lito381/server/AsmFinal.java', marker)
    (source / 'Parent.java').write_text('class Parent {}\n', encoding='utf-8')
    (source / 'PlainFinal.java').write_text('''import local.lito381.server.AsmFinal;
@AsmFinal({"value", "global"}) class PlainFinal extends Parent {
 public Object value; public static Object global;
 public PlainFinal() {} public PlainFinal(Object v) { value = v; }
}
''', encoding='utf-8')
    (source / 'AbstractFinal.java').write_text('''import local.lito381.server.AsmFinal;
@AsmFinal({"memo"}) abstract class AbstractFinal { Object memo; }
''', encoding='utf-8')
    path = lambda x: escape(x.as_posix())
    (module / 'pom.xml').write_text(f'''<project xmlns="http://maven.apache.org/POM/4.0.0">
 <modelVersion>4.0.0</modelVersion><groupId>local.lito381</groupId><artifactId>asm-final-fixture</artifactId><version>1</version>
 <properties><maven.compiler.release>17</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
 <build><directory>{path(target)}</directory><sourceDirectory>{path(source)}</sourceDirectory><plugins>
  <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version></plugin>
  <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-jar-plugin</artifactId><version>3.3.0</version></plugin>
  <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-antrun-plugin</artifactId><version>3.1.0</version>
   <dependencies>
    <dependency><groupId>org.ow2.asm</groupId><artifactId>asm</artifactId><version>9.7</version></dependency>
    <dependency><groupId>org.ow2.asm</groupId><artifactId>asm-tree</artifactId><version>9.7</version></dependency>
   </dependencies>
   <executions><execution><id>as-record-main</id><phase>process-classes</phase><goals><goal>run</goal></goals><configuration><target>
    <mkdir dir="${{project.build.directory}}/as-record-tool"/>
    <javac srcdir="{path(tool.ROOT / 'src/main/java')}" destdir="${{project.build.directory}}/as-record-tool" release="17" encoding="UTF-8" includeantruntime="false"><classpath refid="maven.plugin.classpath"/></javac>
    <java classname="local.lito381.asrecord.AsRecord" fork="true" failonerror="true">
     <jvmarg value="-Djava.io.tmpdir={path(module)}"/>
     <classpath><pathelement location="${{project.build.directory}}/as-record-tool"/><path refid="maven.plugin.classpath"/></classpath>
     <arg value="${{project.build.outputDirectory}}"/>
    </java>
   </target></configuration></execution></executions>
  </plugin>
 </plugins></build>
</project>
''', encoding='utf-8')
    settings = module / 'settings.xml'
    settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"/>', encoding='utf-8')
    env = os.environ.copy()
    env['TEMP'] = env['TMP'] = str(module)
    env['MAVEN_OPTS'] = '-Djava.io.tmpdir="' + str(module) + '"'
    command = [args.maven, '-B', '-s', str(settings), f'-Dmaven.repo.local={args.maven_repo.resolve()}', '-Dmaven.test.skip=true', 'package']
    if args.offline: command.append('-o')
    subprocess.run(command, cwd=module, env=env, check=True)
    product = target / 'asm-final-fixture-1.jar'
    with zipfile.ZipFile(product) as jar:
        assert not any(n.startswith(('org/objectweb/asm/', 'local/lito381/asrecord/', 'com/lineage/')) for n in jar.namelist())
    cp = tool.build()
    checker = target / 'checker'
    tool.compile_java([tool.ROOT / 'src/test/java/AsmFinalCheck.java'], checker, cp)
    tool.run([tool.jdk('java'), '-Xverify:all', '-cp', os.pathsep.join([str(checker), str(product), cp]),
              'AsmFinalCheck', target / 'classes'])
    print('ASM FINAL MAVEN SELFTEST PASS: process-classes -> package -> final fields, no runtime ASM')


if __name__ == '__main__':
    main()
