"""Read-only current native baseline verification; old version-chain checks are archived."""
from pathlib import Path
import subprocess,sys
S=Path(__file__).resolve().parents[1]
L=S.parent.parent/'lito登入器/LiTo381-Launcher'
subprocess.run([sys.executable,str(L/'tools/baseline_066.py'),'verify'],check=True)
subprocess.run([sys.executable,str(L/'tools/verify_current.py')],check=True)
