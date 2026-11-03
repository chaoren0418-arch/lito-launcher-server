from pathlib import Path
P=Path(__file__).resolve().parent;S=P.parents[2];B=S/'tools/resource-database/build'
source=B/'probe-native-retired-reuse-timer.py';text=source.read_text('utf-8')
old="S=Path(__file__).resolve().parents[3];B=Path(__file__).resolve().parent;D=B/'de-native-retired-reuse-timer-development'"
new="S=Path(__file__).resolve().parents[3];B=S/'tools/resource-database/build';D=Path(__file__).resolve().parent/'build'"
assert text.count(old)==1;text=text.replace(old,new)
(P/'test_timer.py').write_text(text,encoding='utf-8')
print('prepared exact32-case timer probe; only runtime/evidence routing changed; original assertions and180s JVM deadline retained')
