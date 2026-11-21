"""Verify the developer-only input observer independently of settings diagnostics."""
from pathlib import Path
import os, sys
from trace import run
root = Path(__file__).resolve().parents[2]
out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
run(root, out, Path(os.environ['MISTEP_JAVA_BIN']))
