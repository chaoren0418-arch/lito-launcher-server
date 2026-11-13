import re, sys
from pathlib import Path
L = Path(sys.argv[1]).read_text(encoding='utf-8', errors='replace').splitlines()
for i, l in enumerate(L):
    if l.startswith('#138-THREAD-DUMP'):
        print('=====', l)
        j = i + 1
        cur = None; frames = []
        while j < len(L) and (L[j].startswith('THREAD ') or L[j].startswith('    at ') or L[j].startswith('LOCK ')):
            x = L[j]
            if x.startswith('THREAD '):
                if cur and any('lito381' in f for f in frames):
                    print(cur); [print('   ', f) for f in frames[:25]]
                cur = x; frames = []
            elif x.startswith('LOCK '):
                print(x)
            else:
                frames.append(x.strip())
            j += 1
        if cur and any('lito381' in f for f in frames):
            print(cur); [print('   ', f) for f in frames[:25]]
