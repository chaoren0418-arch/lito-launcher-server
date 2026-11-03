"""Old cleanup entry retired; inspect the verified 0.6.60 plan instead."""
from pathlib import Path
import subprocess,sys
subprocess.run([sys.executable,str(Path(__file__).with_name('baseline_060.py')),'verify'],check=True)
