"""Issue #420 / T144: offline guard for the G16 explicit-intent probe alignment.

#341 F1/F2/F4 (and F3 mode 1) failed because runtimes that carry
NativeSelectedPlayerIntent (candidate 50635f7e, Maven src/main) silently drop
a BATTLE spell aimed at another room player unless the packet carries the LiTo
client Ctrl extension.  The historical runtime 8d26c9d7 has no such gate.  The
probes below now add that extension only where the gate applies.

This test runs no JAR, SQL or probe.  It checks that
  * every ``need(`` assertion in the touched probes is byte-for-byte the one at
    the T144 base commit (no assertion removed, relaxed or re-worded);
  * the appended bytes are exactly what ``ForcedCombat`` in src/main unwraps;
  * the helper still mirrors ``NativeSelectedPlayerIntent.allowed``;
  * the curse-clock driver still generates one G16-aware ``cast`` whose packet
    layout equals the parent ``PoisonScopeMaterialProbe.cast``;
  * all touched Java sources (and the generated curse probe) parse, using the
    JDK parser when a JDK is on PATH.
"""
from pathlib import Path
import hashlib, re, shutil, subprocess, tempfile, unittest

HERE = Path(__file__).resolve().parent
SERVER = HERE.parents[1]
MAIN = SERVER / 'src/main/java/local/lito381/server'

# Fingerprint = SHA-256 of the ordered ``need(`` argument texts joined by "\n",
# computed on the T144 base commit (dfda9593).  Equal fingerprint = no
# assertion was deleted, added or changed.
NEED_FINGERPRINTS = {
    'call-nature-material/CallNatureMaterialProbe.java': (16, '6c4ea27dca4054a9ec5d0c313f4d5261227aa2a7e4bb7a093b1a73cfb34185e5'),
    'call-nature-material/CallNatureLifecycleProbe.java': (11, '8101ebe2a6762bbb6efa084a3296a5a4c6658a6e71e94cc018267f846a98e027'),
    'cancellation-probability/CancellationProbabilityProbe.java': (25, '575558b01fcf9d28da4c723af544a65915cdad7f33df75798f1e3f3da6086ea7'),
    'cancellation-probability/CancellationMaterialProbe.java': (12, '9eb5de5533fdbad952a651ba74262ea450f3f4a77e738714914fbc259a8375e8'),
    'cancellation-probability/CancellationLifecycleProbe.java': (21, '0e85790e7a6da6185cfffe539931aba6a772004791ee4a99fa3bb5e76b17c640'),
    'cancellation-probability/CancellationProbabilityLifecycleProbe.java': (23, 'a2ccfb661da5f9044809fa7f5b80a67e4c439f8b51cf587b8bf85489bb86e738'),
    'poison-publication/PoisonScopeCurseConsumerProbe.java': (16, '66f2108f709286a68cbceb1c8befa49852098543428ecac005da7c06ea967d0f'),
}
HELPER_OWNERS = [
    'call-nature-material/CallNatureMaterialProbe.java',
    'cancellation-probability/CancellationProbabilityProbe.java',
    'cancellation-probability/CancellationMaterialProbe.java',
]
TRAILER = '{0x4c,0x46,0x43,0x31,raw[0],1,0x67,(byte)0xa5}'
CURSE_DRIVER = HERE / 'cancellation-battle-runtime/test_curse_clock_boundary.py'


def read(path):
    return path.read_text('utf-8').replace('\r\n', '\n')


def need_arguments(text):
    """Ordered argument texts of every ``need(`` call (string literals aware)."""
    out = []
    for match in re.finditer(r'need\(', text):
        start = index = match.end()
        depth = 1
        while depth:
            char = text[index]
            if char == '"':
                index += 1
                while text[index] != '"':
                    index += 2 if text[index] == '\\' else 1
            elif char in '([{':
                depth += 1
            elif char in ')]}':
                depth -= 1
            index += 1
        out.append(text[start:index - 1])
    return out


def helper_block(text):
    start = text.index(' static final boolean G16_INTENT=')
    end = text.index('return out;\n }\n', start) + len('return out;\n }\n')
    return re.sub(r'\s*//[^\n]*', '', text[start:end])


def generated_curse_probe():
    """Run only the driver's text transformation (no Options, no JVM)."""
    driver = read(CURSE_DRIVER)
    start = driver.index("t=p.read_text('utf8')")
    end = driver.index(";source=D/'CurseClockBoundaryProbe.java'")
    namespace = {'p': HERE / 'poison-publication/PoisonScopeCurseConsumerProbe.java'}
    exec(compile(driver[start:end], str(CURSE_DRIVER), 'exec'), namespace)
    return namespace['t']


class SelectedPlayerIntentAlignment(unittest.TestCase):
    def test_need_assertions_unchanged(self):
        for name, (count, digest) in NEED_FINGERPRINTS.items():
            with self.subTest(probe=name):
                arguments = need_arguments(read(HERE / name))
                self.assertEqual(len(arguments), count)
                self.assertEqual(hashlib.sha256('\n'.join(arguments).encode('utf-8')).hexdigest(), digest)

    def test_trailer_matches_forced_combat_unwrap(self):
        forced = read(MAIN / 'ForcedCombat.java')
        self.assertIn('MAGIC = {0x4c,0x46,0x43,0x31}', forced)
        self.assertIn('packet[end+4]!=packet[0] || packet[end+5]!=1', forced)
        self.assertIn('packet[end+6]!=0x67 || (packet[end+7]&255)!=0xa5', forced)
        self.assertIn('if (original != packet) CURRENT.set(new Intent(session,target(original)));', forced)

    def test_helper_mirrors_gate(self):
        gate = read(MAIN / 'NativeSelectedPlayerIntent.java')
        for fragment in ('equals("BATTLE")', 'hub.roomCombatants(room)', 'target.playerObjectId()==id',
                         'target==pc||ForcedCombat.nativeSelectedObjectIntent(pc,id)'):
            self.assertIn(fragment, gate)
        blocks = {name: helper_block(read(HERE / name)) for name in HELPER_OWNERS}
        blocks['generated CurseClockBoundaryProbe'] = helper_block(generated_curse_probe())
        reference = blocks[HELPER_OWNERS[0]]
        for name, block in blocks.items():
            with self.subTest(owner=name):
                self.assertEqual(block, reference)
                self.assertIn(TRAILER, block)
                self.assertIn('Class.forName("local.lito381.server.NativeSelectedPlayerIntent"', block)
                self.assertIn('!String.valueOf(get(pc,"phase")).equals("BATTLE")', block)
                self.assertIn('"roomCombatants"', block)

    def test_every_handle_call_in_touched_probes_is_routed(self):
        for name in ('call-nature-material/CallNatureMaterialProbe.java',
                     'cancellation-probability/CancellationProbabilityProbe.java',
                     'cancellation-probability/CancellationMaterialProbe.java',
                     'cancellation-probability/CancellationLifecycleProbe.java',
                     'cancellation-probability/CancellationProbabilityLifecycleProbe.java'):
            with self.subTest(probe=name):
                text = read(HERE / name)
                self.assertNotIn('call(pc,"handle",new Class[]{byte[].class},packet)', text)
                self.assertIn('call(pc,"handle",new Class[]{byte[].class},g16Intent(pc,', text)
        lifecycle = read(HERE / 'call-nature-material/CallNatureLifecycleProbe.java')
        self.assertIn('if(nativeEntry)call(pc,"handle",new Class[]{byte[].class},g16Intent(pc,id,packet));', lifecycle)
        self.assertIn('call(pc,"useCallOfNatureSpell",new Class[]{byte[].class,row.getClass()},packet,row)', lifecycle)

    def test_curse_generation_overrides_cast_with_parent_layout(self):
        source = HERE / 'poison-publication/PoisonScopeCurseConsumerProbe.java'
        pinned = re.search(r"assert sha\(p\)=='([0-9a-f]{64})'", read(CURSE_DRIVER)).group(1)
        self.assertEqual(hashlib.sha256(source.read_bytes()).hexdigest(), pinned)
        generated = generated_curse_probe()
        self.assertEqual(generated.count('static void cast('), 1)
        self.assertIn('call(pc,"handle",new Class[]{byte[].class},g16Intent(pc,targetId,packet));', generated)
        parent = read(HERE / 'poison-publication/PoisonScopeMaterialProbe.java')
        layout = re.search(r'static void cast\(ClientSession pc,int skill,Object row\)throws Exception \{\n  byte\[\] packet=(.*?);\n', parent).group(1)
        self.assertIn('byte[] packet=' + layout + ';', generated)
        self.assertEqual(need_arguments(generated)[:1], need_arguments(read(source))[:1])

    def test_java_sources_parse(self):
        java = shutil.which('java')
        if java is None:
            self.skipTest('no JDK on PATH')
        with tempfile.TemporaryDirectory() as temporary:
            temporary = Path(temporary)
            parser = temporary / 'ParseOnly.java'
            parser.write_text(
                'import javax.tools.*;import com.sun.source.util.JavacTask;import java.util.*;\n'
                'public class ParseOnly{public static void main(String[] a)throws Exception{'
                'var c=ToolProvider.getSystemJavaCompiler();var d=new DiagnosticCollector<JavaFileObject>();'
                'try(var fm=c.getStandardFileManager(d,null,java.nio.charset.StandardCharsets.UTF_8)){'
                '((JavacTask)c.getTask(null,fm,d,List.of("-proc:none"),null,fm.getJavaFileObjects(a))).parse();}'
                'int e=0;for(var x:d.getDiagnostics())if(x.getKind()==Diagnostic.Kind.ERROR){System.err.println(x);e++;}'
                'System.exit(e==0?0:1);}}\n', encoding='utf-8')
            generated = temporary / 'CurseClockBoundaryProbe.java'
            generated.write_text(generated_curse_probe(), encoding='utf-8')
            files = [str(HERE / name) for name in NEED_FINGERPRINTS] + [str(generated)]
            result = subprocess.run([java, str(parser), *files], capture_output=True, text=True, encoding='utf-8')
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)


if __name__ == '__main__':
    unittest.main()
